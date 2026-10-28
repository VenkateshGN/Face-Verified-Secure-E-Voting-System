package com.votesystem.controller;

import com.votesystem.model.*;
import com.votesystem.repository.*;
import com.votesystem.service.BlockchainService;
import com.votesystem.service.FileStorageService;
import com.votesystem.service.QrCodeService;
import com.votesystem.service.VoteIntegrityService;
import com.votesystem.service.VoteIntegrityService.VoteAuditReport;
import com.votesystem.util.OrgMatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminRepository adminRepo;
    private final PositionRepository positionRepo;
    private final CandidateRepository candidateRepo;
    private final VoterRepository voterRepo;
    private final VoteRepository voteRepo;
    private final AuditLogRepository auditRepo;
    private final TamperLogRepository tamperRepo;
    private final ElectionSettingRepository electionRepo;
    private final PasswordEncoder passwordEncoder;
    private final VoteIntegrityService integrityService;
    private final BlockchainService blockchainService;
    private final FileStorageService fileStorageService;
    private final QrCodeService qrCodeService;
    private final com.votesystem.service.ElectionScheduleService scheduleService;

    public AdminController(AdminRepository adminRepo, PositionRepository positionRepo,
                           CandidateRepository candidateRepo, VoterRepository voterRepo,
                           VoteRepository voteRepo, AuditLogRepository auditRepo,
                           TamperLogRepository tamperRepo, ElectionSettingRepository electionRepo,
                           PasswordEncoder passwordEncoder, VoteIntegrityService integrityService,
                           BlockchainService blockchainService, FileStorageService fileStorageService,
                           QrCodeService qrCodeService,
                           com.votesystem.service.ElectionScheduleService scheduleService) {
        this.adminRepo = adminRepo;
        this.positionRepo = positionRepo;
        this.candidateRepo = candidateRepo;
        this.voterRepo = voterRepo;
        this.voteRepo = voteRepo;
        this.auditRepo = auditRepo;
        this.tamperRepo = tamperRepo;
        this.electionRepo = electionRepo;
        this.passwordEncoder = passwordEncoder;
        this.integrityService = integrityService;
        this.blockchainService = blockchainService;
        this.fileStorageService = fileStorageService;
        this.qrCodeService = qrCodeService;
        this.scheduleService = scheduleService;
    }

    // ==================== AUTHENTICATION ====================

    @GetMapping("/login")
    public String showAdminLogin(HttpSession session) {
        if (session.getAttribute("adminId") != null) {
            return "redirect:/admin/dashboard";
        }
        return "admin/login";
    }

    @PostMapping("/login")
    public String handleAdminLogin(@RequestParam("username") String username,
                                   @RequestParam("password") String password,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        Optional<Admin> adminOpt = adminRepo.findByUsername(username.trim());
        if (adminOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Invalid admin credentials.");
            return "redirect:/admin/login";
        }

        Admin admin = adminOpt.get();
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            redirectAttrs.addFlashAttribute("error", "Incorrect password.");
            return "redirect:/admin/login";
        }

        // Clear voter attributes when admin logs in
        session.removeAttribute("voterId");
        session.removeAttribute("voterDbId");
        session.removeAttribute("voterName");
        session.removeAttribute("voterPhoto");
        session.removeAttribute("voterOrg");
        session.removeAttribute("step");
        session.removeAttribute("trackingToken");

        session.setAttribute("adminId", admin.getId());
        session.setAttribute("adminUsername", admin.getUsername());
        session.setAttribute("adminName", admin.getFullName());
        session.setAttribute("adminPhoto", admin.getPhoto());
        session.setAttribute("adminRole", admin.getRole());
        session.setAttribute("adminOrg", admin.getOrganization());
        session.setAttribute("adminTitle", admin.getOrganizerTitle());

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/register-organizer")
    public String showRegisterOrganizer(Model model, HttpSession session) {
        if (session.getAttribute("adminId") != null) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("electionSetting", getElectionSetting());
        return "admin/register";
    }

    @PostMapping("/register-organizer")
    @Transactional
    public String handleRegisterOrganizer(@RequestParam("organizationName") String organizationName,
                                          @RequestParam("firstname") String firstname,
                                          @RequestParam("lastname") String lastname,
                                          @RequestParam("organizerTitle") String organizerTitle,
                                          @RequestParam("username") String username,
                                          @RequestParam("password") String password,
                                          @RequestParam(value = "initialPosition", defaultValue = "Cultural Event Organizer") String initialPosition,
                                          HttpServletRequest request,
                                          HttpSession session,
                                          RedirectAttributes redirectAttrs) {
        try {
            if (organizationName == null || organizationName.isBlank()) {
                redirectAttrs.addFlashAttribute("error", "Organization name is required.");
                return "redirect:/admin/register-organizer";
            }
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                redirectAttrs.addFlashAttribute("error", "Username and password are required.");
                return "redirect:/admin/register-organizer";
            }
            if (adminRepo.existsByUsername(username.trim())) {
                redirectAttrs.addFlashAttribute("error", "Username '" + username.trim() + "' is already taken. Please pick another username.");
                return "redirect:/admin/register-organizer";
            }

            String org = organizationName.trim();
            Admin admin = new Admin(
                    username.trim(),
                    passwordEncoder.encode(password.trim()),
                    firstname != null ? firstname.trim() : "",
                    lastname != null ? lastname.trim() : "",
                    "admin.jpg",
                    "ORG_ADMIN",
                    org,
                    organizerTitle != null ? organizerTitle.trim() : "Event Organizer"
            );
            adminRepo.save(admin);

            // Automatically create initial starter position for this organization if not existing
            boolean hasPos = positionRepo.findAll().stream()
                    .anyMatch(p -> org.equalsIgnoreCase(p.getTargetOrganization()));
            if (!hasPos) {
                String posTitle = (initialPosition != null && !initialPosition.isBlank()) ? initialPosition.trim() : "Cultural Event Organizer";
                Position starterPos = new Position(posTitle, 1, 1, "OFFICE_ORGANIZATION", "ALL", org);
                positionRepo.save(starterPos);
            }

            String clientIp = request.getRemoteAddr();
            if (clientIp != null && clientIp.length() > 45) {
                clientIp = clientIp.substring(0, 45);
            }
            auditRepo.save(new AuditLog(
                    "Organizer Registered",
                    "ONBOARDING",
                    clientIp,
                    "Organization onboarded: " + org + " with Organizer Admin: " + admin.getUsername() + " (" + admin.getOrganizerTitle() + ")"
            ));

            // Auto login
            session.removeAttribute("voterId");
            session.removeAttribute("voterDbId");
            session.removeAttribute("voterName");
            session.removeAttribute("voterPhoto");
            session.removeAttribute("voterOrg");
            session.removeAttribute("step");
            session.removeAttribute("trackingToken");

            session.setAttribute("adminId", admin.getId());
            session.setAttribute("adminUsername", admin.getUsername());
            session.setAttribute("adminName", admin.getFullName());
            session.setAttribute("adminPhoto", admin.getPhoto());
            session.setAttribute("adminRole", admin.getRole());
            session.setAttribute("adminOrg", admin.getOrganization());
            session.setAttribute("adminTitle", admin.getOrganizerTitle());

            redirectAttrs.addFlashAttribute("success", "Welcome " + admin.getFullName() + "! Your organization portal for " + org + " is ready.");
            return "redirect:/admin/dashboard";
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.error("Data integrity conflict while registering organization: {}", ex.getMessage());
            redirectAttrs.addFlashAttribute("error", "Registration error: An account or organization with these details already exists.");
            return "redirect:/admin/register-organizer";
        } catch (Exception ex) {
            log.error("Unexpected error registering organization: ", ex);
            redirectAttrs.addFlashAttribute("error", "Failed to create organization: " + ex.getMessage());
            return "redirect:/admin/register-organizer";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("adminId");
        session.removeAttribute("adminUsername");
        session.removeAttribute("adminName");
        session.removeAttribute("adminPhoto");
        session.removeAttribute("adminRole");
        session.removeAttribute("adminOrg");
        session.removeAttribute("adminTitle");
        return "redirect:/admin/login";
    }

    private Admin getLoggedInAdmin(HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) return null;
        return adminRepo.findById(adminId).orElse(null);
    }

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "viewOrg", required = false) String viewOrgParam,
                            Model model, HttpServletRequest request, HttpSession session) {
        Admin currentAdmin = getLoggedInAdmin(session);
        if (currentAdmin == null) {
            return "redirect:/admin/login";
        }
        boolean isSuper = currentAdmin.isSuperAdmin();
        String adminOrg = (currentAdmin.getOrganization() != null) ? currentAdmin.getOrganization().trim() : "ALL";

        if (isSuper && viewOrgParam != null) {
            if ("ALL".equalsIgnoreCase(viewOrgParam.trim()) || "CLEAR".equalsIgnoreCase(viewOrgParam.trim())) {
                session.removeAttribute("superAdminViewOrg");
            } else {
                session.setAttribute("superAdminViewOrg", viewOrgParam.trim());
            }
        }

        String tempOrg = "ALL";
        if (!isSuper) {
            tempOrg = adminOrg;
        } else {
            String sessionViewOrg = (String) session.getAttribute("superAdminViewOrg");
            if (sessionViewOrg != null && !sessionViewOrg.isBlank() && !"ALL".equalsIgnoreCase(sessionViewOrg)) {
                tempOrg = sessionViewOrg.trim();
            }
        }
        final String effectiveOrg = tempOrg;
        boolean isViewingSpecificOrg = !"ALL".equalsIgnoreCase(effectiveOrg);

        List<Position> allPositions = positionRepo.findAllByOrderByPriorityAsc();
        List<Position> positions = (!isViewingSpecificOrg) ? allPositions : allPositions.stream()
                .filter(p -> OrgMatcher.matches(effectiveOrg, p.getTargetOrganization()))
                .toList();

        List<Candidate> allCandidates = candidateRepo.findAll();
        List<Candidate> candidates = (!isViewingSpecificOrg) ? allCandidates : allCandidates.stream()
                .filter(c -> OrgMatcher.matches(effectiveOrg, c.getOrganization())
                        || (c.getPosition() != null && OrgMatcher.matches(effectiveOrg, c.getPosition().getTargetOrganization())))
                .toList();

        List<Voter> allVoters = voterRepo.findAll();
        List<Voter> voters = (!isViewingSpecificOrg) ? allVoters : allVoters.stream()
                .filter(v -> OrgMatcher.matches(effectiveOrg, v.getOrganization()))
                .toList();

        long numPositions = positions.size();
        long numCandidates = candidates.size();
        long totalVoters = voters.size();
        long votersVoted = voters.stream().filter(Voter::isHasVoted).count();
        double turnout = totalVoters > 0 ? ((double) votersVoted / totalVoters) * 100.0 : 0.0;

        Map<Long, List<CandidateTally>> talliesByPosition = new HashMap<>();
        for (Position pos : positions) {
            List<Candidate> posCandidates = candidateRepo.findByPositionIdOrderByIdAsc(pos.getId()).stream()
                    .filter(c -> !isViewingSpecificOrg 
                            || OrgMatcher.matches(effectiveOrg, c.getOrganization())
                            || OrgMatcher.matches(pos.getTargetOrganization(), c.getOrganization())
                            || (c.getPosition() != null && OrgMatcher.matches(effectiveOrg, c.getPosition().getTargetOrganization())))
                    .toList();
            List<CandidateTally> tallies = new ArrayList<>();
            for (Candidate c : posCandidates) {
                long votes = voteRepo.countByCandidateId(c.getId());
                tallies.add(new CandidateTally(c.getId(), c.getFullName(), c.getPhoto(), c.getSymbol(), votes));
            }
            talliesByPosition.put(pos.getId(), tallies);
        }

        VoteAuditReport fullAuditReport = integrityService.auditAllVotes(request.getRemoteAddr(), request.getHeader("User-Agent"));
        VoteAuditReport auditReport;
        if (!isViewingSpecificOrg) {
            auditReport = fullAuditReport;
        } else {
            String filterOrg = effectiveOrg;
            List<VoteIntegrityService.VoteAuditItem> filteredItems = fullAuditReport.getItems().stream()
                    .filter(item -> filterOrg.equalsIgnoreCase(item.getVote().getOrganization()))
                    .toList();
            int total = filteredItems.size();
            int valid = (int) filteredItems.stream().filter(VoteIntegrityService.VoteAuditItem::isValid).count();
            int tampered = total - valid;
            auditReport = new VoteAuditReport(total, valid, tampered, filteredItems);
        }

        BlockchainService.BlockchainValidationResult chainResult = blockchainService.validateBlockchain();
        ElectionSetting electionSetting = getElectionSetting();
        List<String> availableOrganizations = getDistinctOrganizations();
        com.votesystem.service.ElectionScheduleService.ScheduleInfo scheduleInfo = scheduleService.getSchedule(effectiveOrg);

        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String portPart = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;
        String baseUrl = scheme + "://" + serverName + portPart;
        String voterRelativeUrl = (isViewingSpecificOrg && !effectiveOrg.equalsIgnoreCase("ALL"))
                ? "/vote?org=" + java.net.URLEncoder.encode(effectiveOrg, java.nio.charset.StandardCharsets.UTF_8)
                : "/vote";
        String voterDirectUrl = baseUrl + voterRelativeUrl;
        String voterQrCodeBase64 = qrCodeService.generateQrCodeBase64(voterDirectUrl, 260, 260);

        model.addAttribute("numPositions", numPositions);
        model.addAttribute("numCandidates", numCandidates);
        model.addAttribute("totalVoters", totalVoters);
        model.addAttribute("votersVoted", votersVoted);
        model.addAttribute("turnoutPercentage", String.format("%.1f", turnout));
        model.addAttribute("positions", positions);
        model.addAttribute("talliesByPosition", talliesByPosition);
        model.addAttribute("auditReport", auditReport);
        model.addAttribute("chainResult", chainResult);
        model.addAttribute("electionSetting", electionSetting);
        model.addAttribute("currentAdmin", currentAdmin);
        model.addAttribute("isSuperAdmin", isSuper);
        model.addAttribute("adminOrg", adminOrg);
        model.addAttribute("activeOrg", effectiveOrg);
        model.addAttribute("isViewingSpecificOrg", isViewingSpecificOrg);
        model.addAttribute("availableOrganizations", availableOrganizations);
        model.addAttribute("scheduleInfo", scheduleInfo);
        model.addAttribute("voterDirectUrl", voterDirectUrl);
        model.addAttribute("voterRelativeUrl", voterRelativeUrl);
        model.addAttribute("voterQrCodeBase64", voterQrCodeBase64);

        return "admin/dashboard";
    }

    // ==================== POSITIONS MANAGEMENT ====================

    @GetMapping("/positions")
    public String listPositions(Model model, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        List<Position> allPositions = positionRepo.findAllByOrderByPriorityAsc();
        List<Position> positions = isSuper ? allPositions : allPositions.stream()
                .filter(p -> OrgMatcher.matches(adminOrg, p.getTargetOrganization()))
                .toList();

        model.addAttribute("positions", positions);
        model.addAttribute("electionSetting", getElectionSetting());
        model.addAttribute("currentAdmin", admin);
        model.addAttribute("isSuperAdmin", isSuper);
        model.addAttribute("adminOrg", adminOrg);
        return "admin/positions";
    }

    @PostMapping("/positions/save")
    public String savePosition(@RequestParam(value = "id", required = false) Long id,
                               @RequestParam("description") String description,
                               @RequestParam("maxVote") Integer maxVote,
                               @RequestParam("priority") Integer priority,
                               @RequestParam(value = "electorateScope", defaultValue = "ALL") String electorateScope,
                               @RequestParam(value = "targetDepartment", defaultValue = "ALL") String targetDepartment,
                               @RequestParam(value = "targetOrganization", defaultValue = "ALL") String targetOrganization,
                               HttpSession session,
                               RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        Position position = (id != null) ? positionRepo.findById(id).orElse(new Position()) : new Position();

        if (id != null && !isSuper) {
            if (!OrgMatcher.matches(adminOrg, position.getTargetOrganization())) {
                redirectAttrs.addFlashAttribute("error", "Access Denied: You cannot modify positions belonging to other organizations.");
                return "redirect:/admin/positions";
            }
        }

        position.setDescription(description.trim());
        position.setMaxVote(maxVote != null ? maxVote : 1);
        position.setPriority(priority != null ? priority : 1);
        position.setElectorateScope(electorateScope != null && !electorateScope.isBlank() ? electorateScope.trim().toUpperCase() : "ALL");
        position.setTargetDepartment(targetDepartment != null && !targetDepartment.isBlank() ? targetDepartment.trim() : "ALL");

        if (!isSuper) {
            position.setTargetOrganization(adminOrg);
        } else {
            position.setTargetOrganization(targetOrganization != null && !targetOrganization.isBlank() ? targetOrganization.trim() : "ALL");
        }
        positionRepo.save(position);

        redirectAttrs.addFlashAttribute("success", "Position saved successfully.");
        return "redirect:/admin/positions";
    }

    @PostMapping("/positions/delete")
    public String deletePosition(@RequestParam("id") Long id, HttpSession session, RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        Position position = positionRepo.findById(id).orElse(null);
        if (position != null) {
            if (!isSuper && !OrgMatcher.matches(adminOrg, position.getTargetOrganization())) {
                redirectAttrs.addFlashAttribute("error", "Access Denied: You cannot delete positions from other organizations.");
                return "redirect:/admin/positions";
            }
            positionRepo.deleteById(id);
            redirectAttrs.addFlashAttribute("success", "Position deleted successfully.");
        }
        return "redirect:/admin/positions";
    }

    // ==================== CANDIDATES MANAGEMENT ====================

    @GetMapping("/candidates")
    public String listCandidates(Model model, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        List<Candidate> allCandidates = candidateRepo.findAll();
        List<Candidate> candidates = isSuper ? allCandidates : allCandidates.stream()
                .filter(c -> OrgMatcher.matches(adminOrg, c.getOrganization())
                        || (c.getPosition() != null && OrgMatcher.matches(adminOrg, c.getPosition().getTargetOrganization())))
                .toList();

        List<Position> allPositions = positionRepo.findAllByOrderByPriorityAsc();
        List<Position> positions = isSuper ? allPositions : allPositions.stream()
                .filter(p -> OrgMatcher.matches(adminOrg, p.getTargetOrganization()))
                .toList();

        model.addAttribute("candidates", candidates);
        model.addAttribute("positions", positions);
        model.addAttribute("electionSetting", getElectionSetting());
        model.addAttribute("currentAdmin", admin);
        model.addAttribute("isSuperAdmin", isSuper);
        model.addAttribute("adminOrg", adminOrg);
        return "admin/candidates";
    }

    @PostMapping("/candidates/save")
    public String saveCandidate(@RequestParam(value = "id", required = false) Long id,
                                @RequestParam("positionId") Long positionId,
                                @RequestParam("firstname") String firstname,
                                @RequestParam("lastname") String lastname,
                                @RequestParam("platform") String platform,
                                @RequestParam(value = "organization", defaultValue = "Infosys") String organization,
                                @RequestParam(value = "photo", required = false) MultipartFile photoFile,
                                @RequestParam(value = "symbol", required = false) MultipartFile symbolFile,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        Position position = positionRepo.findById(positionId).orElse(null);
        if (position == null) {
            redirectAttrs.addFlashAttribute("error", "Position not found.");
            return "redirect:/admin/candidates";
        }

        if (!isSuper && !OrgMatcher.matches(adminOrg, position.getTargetOrganization())) {
            redirectAttrs.addFlashAttribute("error", "Access Denied: You cannot assign candidates to positions outside your organization.");
            return "redirect:/admin/candidates";
        }

        Candidate candidate = (id != null) ? candidateRepo.findById(id).orElse(new Candidate()) : new Candidate();

        if (id != null && !isSuper) {
            if (!OrgMatcher.matches(adminOrg, candidate.getOrganization())) {
                redirectAttrs.addFlashAttribute("error", "Access Denied: You cannot modify candidates from other organizations.");
                return "redirect:/admin/candidates";
            }
        }

        candidate.setPosition(position);
        candidate.setFirstname(firstname.trim());
        candidate.setLastname(lastname.trim());
        candidate.setPlatform(platform);

        if (!isSuper) {
            candidate.setOrganization(adminOrg);
        } else {
            if (organization != null && !organization.isBlank() && !"Infosys".equalsIgnoreCase(organization)) {
                candidate.setOrganization(organization.trim());
            } else if (position.getTargetOrganization() != null && !position.getTargetOrganization().isBlank() && !"ALL".equalsIgnoreCase(position.getTargetOrganization())) {
                candidate.setOrganization(position.getTargetOrganization().trim());
            } else {
                candidate.setOrganization(organization != null && !organization.isBlank() ? organization.trim() : "Infosys");
            }
        }

        if (photoFile != null && !photoFile.isEmpty()) {
            String photoName = fileStorageService.storeFile(photoFile, "cand");
            if (photoName != null) candidate.setPhoto(photoName);
        }

        if (symbolFile != null && !symbolFile.isEmpty()) {
            String symbolName = fileStorageService.storeFile(symbolFile, "symbol");
            if (symbolName != null) candidate.setSymbol(symbolName);
        }

        candidateRepo.save(candidate);
        redirectAttrs.addFlashAttribute("success", "Candidate saved successfully.");
        return "redirect:/admin/candidates";
    }

    @PostMapping("/candidates/delete")
    public String deleteCandidate(@RequestParam("id") Long id, HttpSession session, RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        Candidate candidate = candidateRepo.findById(id).orElse(null);
        if (candidate != null) {
            if (!isSuper && !OrgMatcher.matches(adminOrg, candidate.getOrganization())) {
                redirectAttrs.addFlashAttribute("error", "Access Denied: You cannot delete candidates from other organizations.");
                return "redirect:/admin/candidates";
            }
            candidateRepo.deleteById(id);
            redirectAttrs.addFlashAttribute("success", "Candidate removed.");
        }
        return "redirect:/admin/candidates";
    }

    // ==================== VOTERS MANAGEMENT ====================

    @GetMapping("/voters")
    public String listVoters(@RequestParam(value = "org", required = false) String orgFilter,
                             Model model,
                             HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        List<Voter> allVoters = voterRepo.findAll();
        List<Voter> voters;
        String activeFilter;

        if (!isSuper) {
            // Organizer MUST ONLY see their own organization's voters! Other organize voters are excluded.
            voters = allVoters.stream()
                    .filter(v -> OrgMatcher.matches(adminOrg, v.getOrganization()))
                    .toList();
            activeFilter = adminOrg;
        } else {
            // Super Admin can filter by organization (e.g., ?org=Infosys to view Infosys voters only)
            if (orgFilter != null && !orgFilter.isBlank() && !"ALL".equalsIgnoreCase(orgFilter.trim())) {
                voters = allVoters.stream()
                        .filter(v -> OrgMatcher.matches(orgFilter, v.getOrganization()))
                        .toList();
                activeFilter = orgFilter.trim();
            } else {
                voters = allVoters;
                activeFilter = "ALL";
            }
        }

        Set<String> distinctOrganizations = allVoters.stream()
                .map(Voter::getOrganization)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));

        model.addAttribute("voters", voters);
        model.addAttribute("distinctOrganizations", distinctOrganizations);
        model.addAttribute("selectedOrg", activeFilter);
        model.addAttribute("electionSetting", getElectionSetting());
        model.addAttribute("currentAdmin", admin);
        model.addAttribute("isSuperAdmin", isSuper);
        model.addAttribute("adminOrg", adminOrg);
        return "admin/voters";
    }

    @PostMapping("/voters/save")
    public String saveVoter(@RequestParam(value = "id", required = false) Long id,
                            @RequestParam(value = "voterId", required = false) String voterId,
                            @RequestParam("firstname") String firstname,
                            @RequestParam("lastname") String lastname,
                            @RequestParam("email") String email,
                            @RequestParam(value = "voterCategory", defaultValue = "STUDENT") String voterCategory,
                            @RequestParam(value = "department", defaultValue = "General") String department,
                            @RequestParam(value = "organization", defaultValue = "Infosys") String organization,
                            @RequestParam(value = "password", required = false) String password,
                            @RequestParam(value = "photo", required = false) MultipartFile photoFile,
                            HttpSession session,
                            RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        Voter voter = (id != null) ? voterRepo.findById(id).orElse(new Voter()) : new Voter();

        if (id == null) {
            // New Voter
            if (voterId == null || voterId.isBlank()) {
                voterId = generateRandomVoterId();
            }
            if (voterRepo.existsByVoterId(voterId.trim())) {
                redirectAttrs.addFlashAttribute("error", "Voter ID already exists: " + voterId);
                return "redirect:/admin/voters";
            }
            voter.setVoterId(voterId.trim());
            voter.setPassword(passwordEncoder.encode(password != null && !password.isBlank() ? password : "password"));
        } else {
            if (!isSuper && !adminOrg.equalsIgnoreCase(voter.getOrganization())) {
                redirectAttrs.addFlashAttribute("error", "Access Denied: You cannot modify voters from other organizations.");
                return "redirect:/admin/voters";
            }
            if (password != null && !password.isBlank()) {
                voter.setPassword(passwordEncoder.encode(password));
            }
        }

        voter.setFirstname(firstname.trim());
        voter.setLastname(lastname.trim());
        voter.setEmail(email.trim());
        voter.setVoterCategory(voterCategory != null && !voterCategory.isBlank() ? voterCategory.trim().toUpperCase() : "STUDENT");
        voter.setDepartment(department != null && !department.isBlank() ? department.trim() : "General");

        if (!isSuper) {
            voter.setOrganization(adminOrg);
        } else {
            voter.setOrganization(organization != null && !organization.isBlank() ? organization.trim() : "Infosys");
        }

        if (photoFile != null && !photoFile.isEmpty()) {
            String photoName = fileStorageService.storeFile(photoFile, "voter");
            if (photoName != null) voter.setPhoto(photoName);
        }

        voterRepo.save(voter);
        redirectAttrs.addFlashAttribute("success", "Voter record saved successfully.");
        return "redirect:/admin/voters";
    }

    @PostMapping("/voters/update")
    public String updateVoter(@RequestParam("id") Long id,
                              @RequestParam("firstname") String firstname,
                              @RequestParam("lastname") String lastname,
                              @RequestParam("email") String email,
                              @RequestParam("voterCategory") String voterCategory,
                              @RequestParam(value = "department", defaultValue = "General") String department,
                              @RequestParam(value = "organization", defaultValue = "Infosys") String organization,
                              HttpSession session,
                              RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        Voter voter = voterRepo.findById(id).orElse(null);
        if (voter == null) {
            redirectAttrs.addFlashAttribute("error", "Voter not found.");
            return "redirect:/admin/voters";
        }
        if (!isSuper && !adminOrg.equalsIgnoreCase(voter.getOrganization())) {
            redirectAttrs.addFlashAttribute("error", "Access Denied: You cannot modify voters from other organizations.");
            return "redirect:/admin/voters";
        }

        voter.setFirstname(firstname.trim());
        voter.setLastname(lastname.trim());
        voter.setEmail(email.trim());
        voter.setVoterCategory(voterCategory != null && !voterCategory.isBlank() ? voterCategory.trim().toUpperCase() : "STUDENT");
        voter.setDepartment(department != null && !department.isBlank() ? department.trim() : "General");
        if (!isSuper) {
            voter.setOrganization(adminOrg);
        } else {
            voter.setOrganization(organization != null && !organization.isBlank() ? organization.trim() : "Infosys");
        }
        voterRepo.save(voter);
        redirectAttrs.addFlashAttribute("success", "Voter electorate profile updated successfully.");
        return "redirect:/admin/voters";
    }

    @PostMapping("/voters/delete")
    public String deleteVoter(@RequestParam("id") Long id, HttpSession session, RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        Voter voter = voterRepo.findById(id).orElse(null);
        if (voter != null) {
            if (!isSuper && !adminOrg.equalsIgnoreCase(voter.getOrganization())) {
                redirectAttrs.addFlashAttribute("error", "Access Denied: You cannot delete voters from other organizations.");
                return "redirect:/admin/voters";
            }
            voterRepo.deleteById(id);
            redirectAttrs.addFlashAttribute("success", "Voter record deleted.");
        }
        return "redirect:/admin/voters";
    }

    // ==================== VOTES & TAMPER TESTING ====================

    @GetMapping("/votes")
    public String listVotes(@RequestParam(value = "org", required = false) String orgParam,
                            Model model, HttpServletRequest request, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        String tempOrg = "ALL";
        if (!isSuper) {
            tempOrg = adminOrg;
        } else {
            if (orgParam != null && !orgParam.isBlank()) {
                tempOrg = orgParam.trim();
            } else {
                String sessionView = (String) session.getAttribute("superAdminViewOrg");
                if (sessionView != null && !sessionView.isBlank()) {
                    tempOrg = sessionView.trim();
                }
            }
        }
        final String effectiveOrg = tempOrg;
        boolean isViewingSpecificOrg = !"ALL".equalsIgnoreCase(effectiveOrg);

        VoteAuditReport fullAuditReport = integrityService.auditAllVotes(request.getRemoteAddr(), request.getHeader("User-Agent"));
        VoteAuditReport auditReport;
        if (!isViewingSpecificOrg) {
            auditReport = fullAuditReport;
        } else {
            String filterOrg = effectiveOrg;
            List<VoteIntegrityService.VoteAuditItem> filteredItems = fullAuditReport.getItems().stream()
                    .filter(item -> filterOrg.equalsIgnoreCase(item.getVote().getOrganization()))
                    .toList();
            int total = filteredItems.size();
            int valid = (int) filteredItems.stream().filter(VoteIntegrityService.VoteAuditItem::isValid).count();
            int tampered = total - valid;
            auditReport = new VoteAuditReport(total, valid, tampered, filteredItems);
        }

        List<Candidate> allCandidates = candidateRepo.findAll();
        List<Candidate> candidates = (!isViewingSpecificOrg) ? allCandidates : allCandidates.stream()
                .filter(c -> effectiveOrg.equalsIgnoreCase(c.getOrganization()))
                .toList();

        List<String> availableOrganizations = getDistinctOrganizations();

        model.addAttribute("auditReport", auditReport);
        model.addAttribute("allCandidates", candidates);
        model.addAttribute("currentAdmin", admin);
        model.addAttribute("isSuperAdmin", isSuper);
        model.addAttribute("adminOrg", adminOrg);
        model.addAttribute("activeOrg", effectiveOrg);
        model.addAttribute("isViewingSpecificOrg", isViewingSpecificOrg);
        model.addAttribute("availableOrganizations", availableOrganizations);
        return "admin/votes";
    }

    @PostMapping("/votes/reset")
    @Transactional
    public String resetVotes(@RequestParam(value = "organization", required = false) String orgParam,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        String targetOrg = isSuper ? (orgParam != null && !orgParam.isBlank() ? orgParam.trim() : "ALL") : adminOrg;

        if ("ALL".equalsIgnoreCase(targetOrg)) {
            if (!isSuper) {
                redirectAttrs.addFlashAttribute("error", "Access Denied: Only Platform Super Administrators can reset all organization votes.");
                return "redirect:/admin/votes";
            }
            voteRepo.deleteAll();
            List<Voter> voters = voterRepo.findAll();
            for (Voter v : voters) {
                v.setHasVoted(false);
                v.setVotedAt(null);
                v.setTrackingToken(null);
            }
            voterRepo.saveAll(voters);
            tamperRepo.deleteAll();

            auditRepo.save(new AuditLog("Votes Reset", "ALL", "ADMIN", "All ballot records reset by Super Admin."));
            redirectAttrs.addFlashAttribute("success", "All election votes across all organizations have been reset cleanly.");
        } else {
            List<Vote> votesToDelete = voteRepo.findAll().stream()
                    .filter(v -> targetOrg.equalsIgnoreCase(v.getOrganization()))
                    .toList();
            voteRepo.deleteAll(votesToDelete);

            List<Voter> voters = voterRepo.findAll().stream()
                    .filter(v -> targetOrg.equalsIgnoreCase(v.getOrganization()))
                    .toList();
            for (Voter v : voters) {
                v.setHasVoted(false);
                v.setVotedAt(null);
                v.setTrackingToken(null);
            }
            voterRepo.saveAll(voters);

            auditRepo.save(new AuditLog("Votes Reset", targetOrg, "ADMIN", "Ballot records for " + targetOrg + " reset by " + (admin != null ? admin.getUsername() : "admin")));
            redirectAttrs.addFlashAttribute("success", "All ballot records for " + targetOrg + " have been reset cleanly (" + votesToDelete.size() + " votes reset).");
        }

        return "redirect:/admin/votes";
    }

    @PostMapping("/votes/simulate-tamper")
    public String simulateTamper(@RequestParam("voteId") Long voteId,
                                 @RequestParam("newCandidateId") Long newCandidateId,
                                 RedirectAttributes redirectAttrs) {
        boolean ok = integrityService.simulateTamper(voteId, newCandidateId);
        if (ok) {
            redirectAttrs.addFlashAttribute("success", "Tampering simulated on Vote #" + voteId + "! Refresh the page to see cryptographic detection.");
        } else {
            redirectAttrs.addFlashAttribute("error", "Could not tamper with Vote #" + voteId);
        }
        return "redirect:/admin/votes";
    }

    // ==================== BLOCKCHAIN & AUDIT ====================

    @GetMapping("/blockchain")
    public String viewBlockchain(@RequestParam(value = "org", required = false) String orgParam,
                                 Model model, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        String tempOrg = "ALL";
        if (!isSuper) {
            tempOrg = adminOrg;
        } else {
            if (orgParam != null && !orgParam.isBlank()) {
                tempOrg = orgParam.trim();
            } else {
                String sessionView = (String) session.getAttribute("superAdminViewOrg");
                if (sessionView != null && !sessionView.isBlank()) {
                    tempOrg = sessionView.trim();
                }
            }
        }
        final String effectiveOrg = tempOrg;
        boolean isViewingSpecificOrg = !"ALL".equalsIgnoreCase(effectiveOrg);

        List<BlockchainBlock> allBlocks = blockchainService.getAllBlocks();
        BlockchainService.BlockchainValidationResult validation = blockchainService.validateBlockchain();

        List<BlockchainBlock> blocks;
        if (!isViewingSpecificOrg) {
            blocks = allBlocks;
        } else {
            String filter = effectiveOrg;
            blocks = allBlocks.stream()
                    .filter(b -> b.getBlockIndex() == 0 || filter.equalsIgnoreCase(b.getOrganization())
                            || (b.getDataPayload() != null && b.getDataPayload().toLowerCase().contains(filter.toLowerCase())))
                    .toList();
        }

        model.addAttribute("blocks", blocks);
        model.addAttribute("validation", validation);
        model.addAttribute("currentAdmin", admin);
        model.addAttribute("isSuperAdmin", isSuper);
        model.addAttribute("adminOrg", adminOrg);
        model.addAttribute("activeOrg", effectiveOrg);
        model.addAttribute("isViewingSpecificOrg", isViewingSpecificOrg);
        model.addAttribute("availableOrganizations", getDistinctOrganizations());
        return "admin/blockchain";
    }

    @GetMapping("/audit")
    public String viewAuditLogs(@RequestParam(value = "org", required = false) String orgParam,
                                Model model, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        boolean isSuper = admin.isSuperAdmin();
        String adminOrg = (admin.getOrganization() != null) ? admin.getOrganization().trim() : "ALL";

        String tempOrg = "ALL";
        if (!isSuper) {
            tempOrg = adminOrg;
        } else {
            if (orgParam != null && !orgParam.isBlank()) {
                tempOrg = orgParam.trim();
            } else {
                String sessionView = (String) session.getAttribute("superAdminViewOrg");
                if (sessionView != null && !sessionView.isBlank()) {
                    tempOrg = sessionView.trim();
                }
            }
        }
        final String effectiveOrg = tempOrg;
        boolean isViewingSpecificOrg = !"ALL".equalsIgnoreCase(effectiveOrg);

        List<AuditLog> allAuditLogs = auditRepo.findTop50ByOrderByTimestampDesc();
        List<TamperLog> allTamperLogs = tamperRepo.findAllByOrderByTimestampDesc();

        List<AuditLog> auditLogs;
        List<TamperLog> tamperLogs;

        if (!isViewingSpecificOrg) {
            auditLogs = allAuditLogs;
            tamperLogs = allTamperLogs;
        } else {
            String filter = effectiveOrg.toLowerCase();
            auditLogs = allAuditLogs.stream()
                    .filter(a -> (a.getDetails() != null && a.getDetails().toLowerCase().contains(filter))
                            || (a.getAction() != null && a.getAction().toLowerCase().contains(filter)))
                    .toList();

            tamperLogs = allTamperLogs.stream()
                    .filter(t -> {
                        if (t.getVoterId() != null && t.getVoterId().toLowerCase().contains(filter)) return true;
                        Optional<Voter> v = voterRepo.findByVoterId(t.getVoterId());
                        if (v.isPresent() && effectiveOrg.equalsIgnoreCase(v.get().getOrganization())) return true;
                        if (t.getVoteId() != null) {
                            Optional<Vote> vote = voteRepo.findById(t.getVoteId());
                            if (vote.isPresent() && effectiveOrg.equalsIgnoreCase(vote.get().getOrganization())) return true;
                        }
                        return t.getReason() != null && t.getReason().toLowerCase().contains(filter);
                    })
                    .toList();
        }

        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("tamperLogs", tamperLogs);
        model.addAttribute("currentAdmin", admin);
        model.addAttribute("isSuperAdmin", isSuper);
        model.addAttribute("adminOrg", adminOrg);
        model.addAttribute("activeOrg", effectiveOrg);
        model.addAttribute("isViewingSpecificOrg", isViewingSpecificOrg);
        model.addAttribute("availableOrganizations", getDistinctOrganizations());
        return "admin/audit";
    }

    // ==================== PRINTABLE RESULTS ====================

    @GetMapping("/print")
    public String printResults(Model model) {
        List<Position> positions = positionRepo.findAllByOrderByPriorityAsc();
        Map<Long, List<CandidateTally>> talliesByPosition = new HashMap<>();

        for (Position pos : positions) {
            List<Candidate> candidates = candidateRepo.findByPositionIdOrderByIdAsc(pos.getId());
            List<CandidateTally> tallies = new ArrayList<>();
            for (Candidate c : candidates) {
                long votes = voteRepo.countByCandidateId(c.getId());
                tallies.add(new CandidateTally(c.getId(), c.getFullName(), c.getPhoto(), c.getSymbol(), votes));
            }
            tallies.sort((a, b) -> Long.compare(b.getVoteCount(), a.getVoteCount()));
            talliesByPosition.put(pos.getId(), tallies);
        }

        ElectionSetting setting = getElectionSetting();
        model.addAttribute("positions", positions);
        model.addAttribute("talliesByPosition", talliesByPosition);
        model.addAttribute("totalVotes", voteRepo.count());
        model.addAttribute("votersVoted", voterRepo.countByHasVotedTrue());
        model.addAttribute("electionSetting", setting);

        return "admin/print";
    }

    // ==================== PROFILE SETTINGS ====================

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        Admin admin = adminRepo.findById(adminId).orElse(null);
        model.addAttribute("admin", admin);
        model.addAttribute("electionSetting", getElectionSetting());
        return "admin/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("firstname") String firstname,
                                @RequestParam("lastname") String lastname,
                                @RequestParam(value = "currPassword", required = false) String currPassword,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                @RequestParam(value = "photo", required = false) MultipartFile photoFile,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        Long adminId = (Long) session.getAttribute("adminId");
        Admin admin = adminRepo.findById(adminId).orElse(null);
        if (admin == null) return "redirect:/admin/login";

        admin.setFirstname(firstname.trim());
        admin.setLastname(lastname.trim());

        if (newPassword != null && !newPassword.isBlank()) {
            if (currPassword == null || !passwordEncoder.matches(currPassword, admin.getPassword())) {
                redirectAttrs.addFlashAttribute("error", "Current password does not match.");
                return "redirect:/admin/profile";
            }
            admin.setPassword(passwordEncoder.encode(newPassword));
        }

        if (photoFile != null && !photoFile.isEmpty()) {
            String photo = fileStorageService.storeFile(photoFile, "admin");
            if (photo != null) {
                admin.setPhoto(photo);
                session.setAttribute("adminPhoto", photo);
            }
        }

        adminRepo.save(admin);
        session.setAttribute("adminName", admin.getFullName());
        redirectAttrs.addFlashAttribute("success", "Admin profile updated successfully.");
        return "redirect:/admin/profile";
    }

    @ModelAttribute("electionSetting")
    public ElectionSetting populateElectionSetting() {
        return getElectionSetting();
    }

    @PostMapping("/election/status")
    public String updateElectionStatus(@RequestParam("status") String status, HttpSession session, RedirectAttributes redirectAttrs) {
        Admin currentAdmin = getLoggedInAdmin(session);
        if (currentAdmin == null || !currentAdmin.isSuperAdmin()) {
            return "redirect:/admin/login";
        }
        ElectionSetting s = getElectionSetting();
        s.setStatus(status.toUpperCase());
        electionRepo.save(s);
        redirectAttrs.addFlashAttribute("success", "Election status updated to " + s.getStatus());
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/election/schedule")
    public String updateElectionSchedule(@RequestParam(value = "startTime", required = false) String startTimeStr,
                                         @RequestParam(value = "endTime", required = false) String endTimeStr,
                                         @RequestParam(value = "timeLimitEnabled", defaultValue = "false") boolean timeLimitEnabled,
                                         @RequestParam(value = "presetDurationHours", required = false) Integer presetDurationHours,
                                         @RequestParam(value = "targetOrg", required = false) String targetOrg,
                                         HttpSession session,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttrs) {
        Admin currentAdmin = getLoggedInAdmin(session);
        if (currentAdmin == null) {
            return "redirect:/admin/login";
        }

        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        boolean enabled = timeLimitEnabled;

        if (presetDurationHours != null && presetDurationHours > 0) {
            start = java.time.LocalDateTime.now();
            end = start.plusHours(presetDurationHours);
            enabled = true;
        } else {
            if (startTimeStr != null && !startTimeStr.isBlank()) {
                try {
                    start = java.time.LocalDateTime.parse(startTimeStr.trim());
                } catch (Exception e) {
                    log.warn("Failed to parse start time: {}", startTimeStr);
                }
            }
            if (endTimeStr != null && !endTimeStr.isBlank()) {
                try {
                    end = java.time.LocalDateTime.parse(endTimeStr.trim());
                } catch (Exception e) {
                    log.warn("Failed to parse end time: {}", endTimeStr);
                }
            }
        }

        if (enabled && end != null && start != null && end.isBefore(start)) {
            redirectAttrs.addFlashAttribute("error", "End time must be after the start time.");
            return "redirect:/admin/dashboard";
        }

        String orgToSchedule = targetOrg;
        if (!currentAdmin.isSuperAdmin()) {
            orgToSchedule = currentAdmin.getOrganization();
        } else if (orgToSchedule == null || orgToSchedule.isBlank()) {
            String sessionViewOrg = (String) session.getAttribute("superAdminViewOrg");
            if (sessionViewOrg != null && !sessionViewOrg.isBlank() && !"ALL".equalsIgnoreCase(sessionViewOrg)) {
                orgToSchedule = sessionViewOrg;
            }
        }

        if (currentAdmin.isSuperAdmin() && (orgToSchedule == null || "ALL".equalsIgnoreCase(orgToSchedule.trim()))) {
            scheduleService.updateScheduleForOrganization("ALL", start, end, enabled);
            auditRepo.save(new AuditLog("Global Schedule Updated", "SCHEDULE", request.getRemoteAddr(),
                    "Super Admin updated global schedule: Start=" + start + ", End=" + end + ", Enabled=" + enabled));
        } else {
            scheduleService.updateScheduleForOrganization(orgToSchedule, start, end, enabled);
            auditRepo.save(new AuditLog("Organization Schedule Updated", "SCHEDULE", request.getRemoteAddr(),
                    "Schedule updated for " + orgToSchedule + ": Start=" + start + ", End=" + end + ", Enabled=" + enabled));
        }

        String statusMsg = enabled 
                ? "Election schedule successfully activated! Window: " + (start != null ? start.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a")) : "Immediate") + " to " + (end != null ? end.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a")) : "Unlimited") 
                : "Time limit disabled. Election is open without deadline.";
        redirectAttrs.addFlashAttribute("success", statusMsg);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/election/settings")
    public String updateElectionSettings(@RequestParam("organizationName") String organizationName,
                                         @RequestParam("organizationType") String organizationType,
                                         @RequestParam("electionTitle") String electionTitle,
                                         @RequestParam(value = "electionSubtitle", required = false) String electionSubtitle,
                                         @RequestParam(value = "voterIdLabel", required = false) String voterIdLabel,
                                         @RequestParam(value = "symbolLabel", required = false) String symbolLabel,
                                         @RequestParam(value = "emergencyKey", required = false) String emergencyKey,
                                         @RequestParam(value = "livenessCheckEnabled", defaultValue = "false") boolean livenessCheckEnabled,
                                         HttpSession session,
                                         RedirectAttributes redirectAttrs) {
        Admin currentAdmin = getLoggedInAdmin(session);
        if (currentAdmin == null || !currentAdmin.isSuperAdmin()) {
            redirectAttrs.addFlashAttribute("error", "Access Denied: Only Platform Super Administrators can update global election configurations.");
            return "redirect:/admin/login";
        }

        ElectionSetting s = getElectionSetting();
        s.setOrganizationName(organizationName.trim());
        s.setOrganizationType(organizationType.trim().toUpperCase());
        s.setElectionTitle(electionTitle.trim());
        if (electionSubtitle != null) s.setElectionSubtitle(electionSubtitle.trim());
        if (voterIdLabel != null && !voterIdLabel.isBlank()) s.setVoterIdLabel(voterIdLabel.trim());
        if (symbolLabel != null && !symbolLabel.isBlank()) s.setSymbolLabel(symbolLabel.trim());
        if (emergencyKey != null && !emergencyKey.isBlank()) s.setEmergencyKey(emergencyKey.trim());
        s.setLivenessCheckEnabled(livenessCheckEnabled);

        electionRepo.save(s);
        auditRepo.save(new AuditLog("Settings Updated", "ELECTION", "ADMIN", "Organization context updated: " + s.getOrganizationName() + " (" + s.getOrganizationTypeDisplayName() + ")"));
        redirectAttrs.addFlashAttribute("success", "Organization & Election configuration saved successfully!");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/election/apply-preset")
    @Transactional
    public String applyPreset(@RequestParam("presetType") String presetType,
                              @RequestParam(value = "populatePositions", defaultValue = "false") boolean populatePositions,
                              HttpSession session,
                              RedirectAttributes redirectAttrs) {
        Admin currentAdmin = getLoggedInAdmin(session);
        if (currentAdmin == null || !currentAdmin.isSuperAdmin()) {
            redirectAttrs.addFlashAttribute("error", "Access Denied: Only Platform Super Administrators can apply global election presets.");
            return "redirect:/admin/login";
        }

        ElectionSetting s = getElectionSetting();
        String preset = presetType.trim().toUpperCase();

        if ("SCHOOL_COLLEGE".equals(preset)) {
            s.setOrganizationName("Delhi Public School / St. Xavier's College");
            s.setOrganizationType("SCHOOL_COLLEGE");
            s.setElectionTitle("Annual Student Council Leadership Election 2026");
            s.setElectionSubtitle("Electing Head Boy, Head Girl, Sports Captain, Cultural Secretary, and House Representatives.");
            s.setVoterIdLabel("Student Roll Number / PRN");
            s.setSymbolLabel("House Badge / Club Emblem");

            if (populatePositions) {
                positionRepo.save(new Position("Student Council President / Head Boy", 1, 1, "STUDENT"));
                positionRepo.save(new Position("Student Council Vice President / Head Girl", 1, 2, "STUDENT"));
                positionRepo.save(new Position("Sports Captain", 1, 3, "STUDENT"));
                positionRepo.save(new Position("Cultural & Arts Secretary", 1, 4, "STUDENT"));
                positionRepo.save(new Position("Class / House Representatives", 2, 5, "STUDENT"));
                positionRepo.save(new Position("Faculty & Staff Advisory Senate", 1, 6, "OFFICE_ORGANIZATION"));
            }
        } else if ("CORPORATE".equals(preset)) {
            s.setOrganizationName("Apex Technologies Corp");
            s.setOrganizationType("CORPORATE");
            s.setElectionTitle("Corporate Board & Employee Advisory Council Election 2026");
            s.setElectionSubtitle("Electing Board of Directors and Employee Advisory Council members.");
            s.setVoterIdLabel("Employee ID");
            s.setSymbolLabel("Department / Team Logo");

            if (populatePositions) {
                positionRepo.save(new Position("Board of Directors", 3, 1, "OFFICE_ORGANIZATION"));
                positionRepo.save(new Position("Employee Welfare Council Representative", 1, 2, "OFFICE_ORGANIZATION"));
                positionRepo.save(new Position("Innovation & Tech Advisory Lead", 1, 3, "OFFICE_ORGANIZATION"));
                positionRepo.save(new Position("Workplace Safety & Compliance Officer", 1, 4, "OFFICE_ORGANIZATION"));
            }
        } else if ("COMMUNITY_NGO".equals(preset)) {
            s.setOrganizationName("Greenwood Community Welfare Association");
            s.setOrganizationType("COMMUNITY_NGO");
            s.setElectionTitle("Executive Committee General Election 2026");
            s.setElectionSubtitle("Democratically electing Trustees, Executive Committee, and Office Bearers.");
            s.setVoterIdLabel("Membership Card Number");
            s.setSymbolLabel("Society Emblem / Chapter Logo");

            if (populatePositions) {
                positionRepo.save(new Position("President / Chairperson", 1, 1, "OFFICE_ORGANIZATION"));
                positionRepo.save(new Position("General Secretary", 1, 2, "OFFICE_ORGANIZATION"));
                positionRepo.save(new Position("Treasurer & Finance Head", 1, 3, "OFFICE_ORGANIZATION"));
                positionRepo.save(new Position("Executive Committee Members", 3, 4, "ALL"));
            }
        } else if ("CORPORATE_INFOSYS".equals(preset) || "INFOSYS".equals(preset)) {
            s.setOrganizationName("Infosys");
            s.setOrganizationType("CORPORATE");
            s.setElectionTitle("Infosys Annual Cultural & Event Leadership Election 2026");
            s.setElectionSubtitle("Democratic election for Cultural Event Organizer and Employee Welfare Committee across Infosys units.");
            s.setVoterIdLabel("Infosys Employee ID");
            s.setSymbolLabel("Unit Emblem / Event Logo");

            if (populatePositions) {
                Position p1 = new Position("Cultural Event Organizer", 1, 1, "OFFICE_ORGANIZATION", "ALL", "Infosys");
                Position p2 = new Position("Employee Welfare Council Representative", 1, 2, "OFFICE_ORGANIZATION", "ALL", "Infosys");
                Position p3 = new Position("Sports & Recreation Coordinator", 1, 3, "OFFICE_ORGANIZATION", "ALL", "Infosys");
                positionRepo.save(p1);
                positionRepo.save(p2);
                positionRepo.save(p3);

                // Seed candidates specifically for Infosys Cultural Event Organizer
                candidateRepo.save(new Candidate(p1, "Ananya", "Rao", "Screenshot.png", "Fostering vibrant multicultural events, annual tech fests, and arts clubs across Infosys campuses.", "Screenshot.png", "Infosys"));
                candidateRepo.save(new Candidate(p1, "Rahul", "Deshmukh", "Screenshot.png", "Inclusive inter-unit competitions, celebration galas, and employee engagement fests.", "Screenshot.png", "Infosys"));

                // Seed Infosys employee voters
                if (!voterRepo.existsByVoterId("INFY_108291")) {
                    voterRepo.save(new Voter("INFY_108291", passwordEncoder.encode("password"), "Ananya", "Rao", "Screenshot.png", "ananya.rao@infosys.com", "EMPLOYEE", "Cultural Committee", "Infosys"));
                }
                if (!voterRepo.existsByVoterId("INFY_108292")) {
                    voterRepo.save(new Voter("INFY_108292", passwordEncoder.encode("password"), "Rahul", "Deshmukh", "Screenshot.png", "rahul.deshmukh@infosys.com", "EMPLOYEE", "Digital Systems", "Infosys"));
                }
                if (!voterRepo.existsByVoterId("INFY_108293")) {
                    voterRepo.save(new Voter("INFY_108293", passwordEncoder.encode("password"), "Priya", "Nair", "Screenshot.png", "priya.nair@infosys.com", "EMPLOYEE", "Human Resources", "Infosys"));
                }
            }
        } else if ("CIVIC_GOVERNMENT".equals(preset)) {
            s.setOrganizationName("Metropolitan Electoral Commission");
            s.setOrganizationType("CIVIC_GOVERNMENT");
            s.setElectionTitle("General Legislative Assembly Election 2026");
            s.setElectionSubtitle("Constituency legislative representatives and civic leadership selection.");
            s.setVoterIdLabel("Electoral Photo ID (EPIC / Voter ID)");
            s.setSymbolLabel("Allotted Party Symbol / Logo");

            if (populatePositions) {
                positionRepo.save(new Position("Member of Legislative Assembly (MLA)", 1, 1, "ALL"));
                positionRepo.save(new Position("Member of Parliament (MP)", 1, 2, "ALL"));
                positionRepo.save(new Position("City Mayor / Ward Councillor", 1, 3, "ALL"));
            }
        }

        electionRepo.save(s);
        auditRepo.save(new AuditLog("Preset Applied", "ELECTION", "ADMIN", "Applied preset template: " + s.getOrganizationTypeDisplayName()));
        redirectAttrs.addFlashAttribute("success", "Successfully applied preset: " + s.getOrganizationTypeDisplayName() + (populatePositions ? " (Standard positions added)" : ""));
        return "redirect:/admin/dashboard";
    }

    // ==================== ORGANIZERS MANAGEMENT (SUPER ADMIN) ====================

    @GetMapping("/organizers")
    public String listOrganizers(Model model, HttpSession session, RedirectAttributes redirectAttrs) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin/login";
        if (!admin.isSuperAdmin()) {
            redirectAttrs.addFlashAttribute("error", "Access Denied: Only Platform Super Administrators can manage organization organizers.");
            return "redirect:/admin/dashboard";
        }

        List<Admin> organizers = adminRepo.findAllByOrderByCreatedOnDesc();
        model.addAttribute("organizers", organizers);
        model.addAttribute("currentAdmin", admin);
        model.addAttribute("isSuperAdmin", true);
        model.addAttribute("electionSetting", getElectionSetting());
        return "admin/organizers";
    }

    @PostMapping("/organizers/save")
    @Transactional
    public String saveOrganizer(@RequestParam(value = "id", required = false) Long id,
                                @RequestParam("username") String username,
                                @RequestParam(value = "password", required = false) String password,
                                @RequestParam("firstname") String firstname,
                                @RequestParam("lastname") String lastname,
                                @RequestParam("organization") String organization,
                                @RequestParam(value = "organizerTitle", defaultValue = "Event Organizer") String organizerTitle,
                                @RequestParam(value = "role", defaultValue = "ORG_ADMIN") String role,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        Admin currentAdmin = getLoggedInAdmin(session);
        if (currentAdmin == null) return "redirect:/admin/login";
        if (!currentAdmin.isSuperAdmin()) {
            redirectAttrs.addFlashAttribute("error", "Access Denied: Only Platform Super Administrators can add or edit organizers.");
            return "redirect:/admin/dashboard";
        }

        try {
            if (id == null) {
                if (username == null || username.isBlank()) {
                    redirectAttrs.addFlashAttribute("error", "Login username is required.");
                    return "redirect:/admin/organizers";
                }
                if (organization == null || organization.isBlank()) {
                    redirectAttrs.addFlashAttribute("error", "Organization name is required.");
                    return "redirect:/admin/organizers";
                }
                if (adminRepo.existsByUsername(username.trim())) {
                    redirectAttrs.addFlashAttribute("error", "Username '" + username.trim() + "' is already in use.");
                    return "redirect:/admin/organizers";
                }
                if (password == null || password.isBlank()) {
                    redirectAttrs.addFlashAttribute("error", "Password is required for a new organizer account.");
                    return "redirect:/admin/organizers";
                }
                Admin newAdmin = new Admin(
                        username.trim(),
                        passwordEncoder.encode(password.trim()),
                        firstname != null ? firstname.trim() : "",
                        lastname != null ? lastname.trim() : "",
                        "admin.jpg",
                        role != null ? role.trim() : "ORG_ADMIN",
                        organization.trim(),
                        organizerTitle != null ? organizerTitle.trim() : "Event Organizer"
                );
                adminRepo.save(newAdmin);

                // Auto-create initial position if organization has no position yet
                String org = organization.trim();
                boolean hasPos = positionRepo.findAll().stream()
                        .anyMatch(p -> org.equalsIgnoreCase(p.getTargetOrganization()));
                if (!hasPos && !"ALL".equalsIgnoreCase(org)) {
                    Position starterPos = new Position("Cultural Event Organizer", 1, 1, "OFFICE_ORGANIZATION", "ALL", org);
                    positionRepo.save(starterPos);
                }

                auditRepo.save(new AuditLog("Organizer Created", "ADMIN_MGMT", "SUPER_ADMIN", "Created organizer: " + username.trim() + " for " + org));
                redirectAttrs.addFlashAttribute("success", "Organizer account created for " + org + " (" + username.trim() + ").");
            } else {
                Optional<Admin> existingOpt = adminRepo.findById(id);
                if (existingOpt.isEmpty()) {
                    redirectAttrs.addFlashAttribute("error", "Organizer not found.");
                    return "redirect:/admin/organizers";
                }
                Admin target = existingOpt.get();
                target.setFirstname(firstname != null ? firstname.trim() : "");
                target.setLastname(lastname != null ? lastname.trim() : "");
                target.setOrganization(organization != null ? organization.trim() : "ALL");
                target.setOrganizerTitle(organizerTitle != null ? organizerTitle.trim() : "Event Organizer");
                if (role != null && !role.isBlank()) {
                    target.setRole(role.trim());
                }
                if (password != null && !password.isBlank()) {
                    target.setPassword(passwordEncoder.encode(password.trim()));
                }
                adminRepo.save(target);
                auditRepo.save(new AuditLog("Organizer Updated", "ADMIN_MGMT", "SUPER_ADMIN", "Updated organizer: " + target.getUsername() + " (" + target.getOrganization() + ")"));
                redirectAttrs.addFlashAttribute("success", "Organizer details updated successfully.");
            }
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.error("Data integrity error saving organizer: {}", ex.getMessage());
            redirectAttrs.addFlashAttribute("error", "Conflict: An organizer with this username or organization already exists.");
        } catch (Exception ex) {
            log.error("Unexpected error saving organizer: ", ex);
            redirectAttrs.addFlashAttribute("error", "Error saving organizer: " + ex.getMessage());
        }

        return "redirect:/admin/organizers";
    }

    @PostMapping("/organizers/delete/{id}")
    @Transactional
    public String deleteOrganizer(@PathVariable("id") Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttrs) {
        Admin currentAdmin = getLoggedInAdmin(session);
        if (currentAdmin == null) return "redirect:/admin/login";
        if (!currentAdmin.isSuperAdmin()) {
            redirectAttrs.addFlashAttribute("error", "Access Denied: Only Platform Super Administrators can delete organizers.");
            return "redirect:/admin/dashboard";
        }

        if (currentAdmin.getId().equals(id)) {
            redirectAttrs.addFlashAttribute("error", "You cannot delete your own Super Administrator account.");
            return "redirect:/admin/organizers";
        }

        Optional<Admin> targetOpt = adminRepo.findById(id);
        if (targetOpt.isPresent()) {
            Admin target = targetOpt.get();
            adminRepo.delete(target);
            auditRepo.save(new AuditLog("Organizer Deleted", "ADMIN_MGMT", "SUPER_ADMIN", "Deleted organizer: " + target.getUsername() + " (" + target.getOrganization() + ")"));
            redirectAttrs.addFlashAttribute("success", "Organizer account '" + target.getUsername() + "' (" + target.getOrganization() + ") deleted.");
        } else {
            redirectAttrs.addFlashAttribute("error", "Organizer account not found.");
        }

        return "redirect:/admin/organizers";
    }

    @PostMapping("/organization/delete-election")
    @Transactional
    public String deleteOrganizationElection(@RequestParam("organizationName") String organizationName,
                                             HttpSession session,
                                             RedirectAttributes redirectAttrs) {
        Admin currentAdmin = getLoggedInAdmin(session);
        if (currentAdmin == null || !currentAdmin.isSuperAdmin()) {
            redirectAttrs.addFlashAttribute("error", "Access Denied: Only Platform Super Administrators can remove organization elections.");
            return "redirect:/admin/dashboard";
        }

        if (organizationName == null || organizationName.isBlank() || "ALL".equalsIgnoreCase(organizationName.trim())) {
            redirectAttrs.addFlashAttribute("error", "Invalid organization name specified for election removal.");
            return "redirect:/admin/dashboard";
        }

        String org = organizationName.trim();

        // 1. Find all positions of this organization
        List<Position> orgPositions = positionRepo.findAll().stream()
                .filter(p -> org.equalsIgnoreCase(p.getTargetOrganization()))
                .toList();

        // 2. Find all candidates of this organization or these positions
        List<Candidate> orgCandidates = candidateRepo.findAll().stream()
                .filter(c -> org.equalsIgnoreCase(c.getOrganization()) || (c.getPosition() != null && org.equalsIgnoreCase(c.getPosition().getTargetOrganization())))
                .toList();

        // 3. Find and delete all votes belonging to this organization (by candidate, position, or voter)
        List<Vote> allVotes = voteRepo.findAll();
        List<Vote> votesToDelete = allVotes.stream()
                .filter(v -> org.equalsIgnoreCase(v.getOrganization())
                        || (v.getPosition() != null && org.equalsIgnoreCase(v.getPosition().getTargetOrganization()))
                        || (v.getCandidate() != null && org.equalsIgnoreCase(v.getCandidate().getOrganization()))
                        || (v.getVoter() != null && org.equalsIgnoreCase(v.getVoter().getOrganization())))
                .toList();

        voteRepo.deleteAll(votesToDelete);

        // 4. Delete candidates
        candidateRepo.deleteAll(orgCandidates);

        // 5. Delete positions
        positionRepo.deleteAll(orgPositions);

        // 6. Reset voted status for voters in that organization
        List<Voter> orgVoters = voterRepo.findAll().stream()
                .filter(v -> org.equalsIgnoreCase(v.getOrganization()))
                .toList();
        for (Voter v : orgVoters) {
            v.setHasVoted(false);
            v.setVotedAt(null);
            v.setTrackingToken(null);
        }
        voterRepo.saveAll(orgVoters);

        // 7. Clear viewOrg if it matches deleted org
        if (org.equalsIgnoreCase((String) session.getAttribute("superAdminViewOrg"))) {
            session.removeAttribute("superAdminViewOrg");
        }

        // 8. Log action in AuditLog
        auditRepo.save(new AuditLog(
                "Organization Election Removed",
                "SUPER_ADMIN",
                "ADMIN_PORTAL",
                "Election for " + org + " removed by Super Admin " + currentAdmin.getUsername() + ". Purged " + votesToDelete.size() + " votes, " + orgCandidates.size() + " candidates, and " + orgPositions.size() + " positions."
        ));

        redirectAttrs.addFlashAttribute("success", "Successfully removed entire election for '" + org + "'! (" + votesToDelete.size() + " votes, " + orgCandidates.size() + " candidates, " + orgPositions.size() + " positions deleted).");
        return "redirect:/admin/organizers";
    }

    private List<String> getDistinctOrganizations() {
        Set<String> orgs = new LinkedHashSet<>();
        for (Admin a : adminRepo.findAll()) {
            if (a.getOrganization() != null && !a.getOrganization().isBlank() && !"ALL".equalsIgnoreCase(a.getOrganization())) {
                orgs.add(a.getOrganization().trim());
            }
        }
        for (Position p : positionRepo.findAll()) {
            if (p.getTargetOrganization() != null && !p.getTargetOrganization().isBlank() && !"ALL".equalsIgnoreCase(p.getTargetOrganization())) {
                orgs.add(p.getTargetOrganization().trim());
            }
        }
        for (Voter v : voterRepo.findAll()) {
            if (v.getOrganization() != null && !v.getOrganization().isBlank() && !"ALL".equalsIgnoreCase(v.getOrganization())) {
                orgs.add(v.getOrganization().trim());
            }
        }
        List<String> sorted = new ArrayList<>(orgs);
        Collections.sort(sorted);
        return sorted;
    }

    private ElectionSetting getElectionSetting() {
        return electionRepo.findAll().stream().findFirst().orElseGet(() -> {
            ElectionSetting s = new ElectionSetting("Apex Multi-Org Leadership Council", "GENERAL", "Executive Leadership Election 2026", "ACTIVE");
            return electionRepo.save(s);
        });
    }

    private String generateRandomVoterId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(15);
        for (int i = 0; i < 15; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static class CandidateTally {
        private final Long candidateId;
        private final String candidateName;
        private final String photo;
        private final String symbol;
        private final long voteCount;

        public CandidateTally(Long candidateId, String candidateName, String photo, String symbol, long voteCount) {
            this.candidateId = candidateId;
            this.candidateName = candidateName;
            this.photo = photo;
            this.symbol = symbol;
            this.voteCount = voteCount;
        }

        public Long getCandidateId() { return candidateId; }
        public String getCandidateName() { return candidateName; }
        public String getPhoto() { return photo; }
        public String getSymbol() { return symbol; }
        public long getVoteCount() { return voteCount; }
    }
}
