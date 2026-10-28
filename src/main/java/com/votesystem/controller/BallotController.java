package com.votesystem.controller;

import com.votesystem.model.*;
import com.votesystem.repository.*;
import com.votesystem.service.BlockchainService;
import com.votesystem.service.CryptoService;
import com.votesystem.service.QrCodeService;
import com.votesystem.util.OrgMatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Controller
public class BallotController {

    private static final Logger log = LoggerFactory.getLogger(BallotController.class);

    private final PositionRepository positionRepo;
    private final CandidateRepository candidateRepo;
    private final VoterRepository voterRepo;
    private final VoteRepository voteRepo;
    private final AuditLogRepository auditRepo;
    private final CryptoService cryptoService;
    private final BlockchainService blockchainService;
    private final QrCodeService qrCodeService;
    private final ElectionSettingRepository electionRepo;
    private final com.votesystem.service.ElectionScheduleService scheduleService;

    public BallotController(PositionRepository positionRepo, CandidateRepository candidateRepo,
                            VoterRepository voterRepo, VoteRepository voteRepo,
                            AuditLogRepository auditRepo, CryptoService cryptoService,
                            BlockchainService blockchainService, QrCodeService qrCodeService,
                            ElectionSettingRepository electionRepo,
                            com.votesystem.service.ElectionScheduleService scheduleService) {
        this.positionRepo = positionRepo;
        this.candidateRepo = candidateRepo;
        this.voterRepo = voterRepo;
        this.voteRepo = voteRepo;
        this.auditRepo = auditRepo;
        this.cryptoService = cryptoService;
        this.blockchainService = blockchainService;
        this.qrCodeService = qrCodeService;
        this.electionRepo = electionRepo;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/ballot")
    public String showBallot(Model model, HttpSession session, RedirectAttributes redirectAttrs) {
        String voterId = (String) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/login";

        Voter voter = voterRepo.findByVoterId(voterId).orElse(null);
        if (voter == null) return "redirect:/login";

        if (voter.isHasVoted()) {
            return "redirect:/receipt";
        }

        // Time Limit & Schedule Check for Voter's Organization
        com.votesystem.service.ElectionScheduleService.ScheduleInfo schedule = scheduleService.getSchedule(voter.getOrganization());
        if (schedule.isNotStarted()) {
            redirectAttrs.addFlashAttribute("error", "Voting has not commenced yet. Polls for " 
                    + voter.getOrganizationDisplayName() + " will open on " + schedule.getFormattedStartTime() + ".");
            return "redirect:/login";
        }
        if (schedule.isExpired()) {
            redirectAttrs.addFlashAttribute("error", "The voting window for " + voter.getOrganizationDisplayName() 
                    + " has concluded. Polls closed at " + schedule.getFormattedEndTime() + ". Ballots are sealed.");
            return "redirect:/login";
        }

        List<Position> allPositions = positionRepo.findAllByOrderByPriorityAsc();
        List<Position> eligiblePositions = allPositions.stream()
                .filter(p -> isPositionEligibleForVoter(p, voter))
                .toList();

        // Ensure candidates shown only include candidates belonging to this voter's authorized company/organization
        for (Position p : eligiblePositions) {
            String voterOrg = (voter.getOrganization() != null) ? voter.getOrganization().trim() : "";
            List<Candidate> filteredCandidates = p.getCandidates().stream()
                    .filter(c -> {
                        if (!voterOrg.isBlank() && !"ALL".equalsIgnoreCase(voterOrg)) {
                            return OrgMatcher.matches(voterOrg, c.getOrganization())
                                    || OrgMatcher.matches(voterOrg, p.getTargetOrganization());
                        }
                        return true;
                    })
                    .toList();
            p.setCandidates(new ArrayList<>(filteredCandidates));
        }

        int excludedCount = allPositions.size() - eligiblePositions.size();
        ElectionSetting setting = electionRepo.findAll().stream().findFirst().orElse(new ElectionSetting());

        model.addAttribute("voter", voter);
        model.addAttribute("positions", eligiblePositions);
        model.addAttribute("allPositionsCount", allPositions.size());
        model.addAttribute("excludedCount", excludedCount);
        model.addAttribute("electionTitle", setting.getElectionTitle());
        model.addAttribute("electionSetting", setting);
        model.addAttribute("voterOrganization", voter.getOrganizationDisplayName());

        // Election Time Limit & Live Countdown attributes
        model.addAttribute("timeLimitActive", schedule.isTimeLimitActive());
        model.addAttribute("remainingSeconds", schedule.getRemainingSeconds());
        model.addAttribute("formattedEndTime", schedule.getFormattedEndTime());
        model.addAttribute("formattedStartTime", schedule.getFormattedStartTime());
        model.addAttribute("scheduleStatus", schedule.getScheduleStatus());

        return "voter/ballot";
    }

    @PostMapping("/ballot/submit")
    @Transactional
    public String submitBallot(@RequestParam Map<String, String> allParams,
                               HttpServletRequest request,
                               HttpSession session,
                               RedirectAttributes redirectAttrs) {
        String voterId = (String) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/login";

        Voter voter = voterRepo.findByVoterId(voterId).orElse(null);
        if (voter == null) return "redirect:/login";

        // Double voting prevention
        if (voter.isHasVoted() || voteRepo.existsByVoterId(voter.getId())) {
            redirectAttrs.addFlashAttribute("error", "You have already cast your ballot. Multiple votes are prohibited.");
            return "redirect:/receipt";
        }

        // Server-Side Time Limit Enforcement
        com.votesystem.service.ElectionScheduleService.ScheduleInfo schedule = scheduleService.getSchedule(voter.getOrganization());
        if (schedule.isNotStarted()) {
            auditRepo.save(new AuditLog(
                    "Early Ballot Blocked",
                    "SECURITY_VIOLATION",
                    request.getRemoteAddr(),
                    "Voter " + voter.getVoterId() + " (Org: " + voter.getOrganization() + ") attempted ballot submission before scheduled start: " + schedule.getFormattedStartTime()
            ));
            redirectAttrs.addFlashAttribute("error", "Voting has not commenced yet for " + voter.getOrganizationDisplayName() + ".");
            return "redirect:/login";
        }
        if (schedule.isExpired()) {
            auditRepo.save(new AuditLog(
                    "Late Ballot Blocked",
                    "SECURITY_VIOLATION",
                    request.getRemoteAddr(),
                    "Voter " + voter.getVoterId() + " (Org: " + voter.getOrganization() + ") attempted ballot submission after deadline expired at " + schedule.getFormattedEndTime()
            ));
            redirectAttrs.addFlashAttribute("error", "Voting deadline has expired! Your ballot was received after the " + schedule.getFormattedEndTime() + " cutoff and could not be counted.");
            return "redirect:/login";
        }

        List<Position> positions = positionRepo.findAllByOrderByPriorityAsc();
        List<Vote> votesToSave = new ArrayList<>();
        List<String> summarySelections = new ArrayList<>();

        for (Position pos : positions) {
            String paramName = "pos_" + pos.getId();
            String[] selectedCandidates = request.getParameterValues(paramName);

            if (selectedCandidates == null || selectedCandidates.length == 0) {
                // Check if slug parameter was sent
                selectedCandidates = request.getParameterValues(pos.getSlug());
            }

            if (selectedCandidates != null && selectedCandidates.length > 0) {
                // Strict Multi-Tenant & Electorate Authorization Check
                if (!isPositionEligibleForVoter(pos, voter)) {
                    auditRepo.save(new AuditLog(
                            "Unauthorized Vote Blocked",
                            "SECURITY_VIOLATION",
                            request.getRemoteAddr(),
                            "Voter " + voter.getVoterId() + " (Org: " + voter.getOrganization() + ", " + voter.getVoterCategory() + ") attempted unauthorized ballot submission on restricted position: " + pos.getDescription() + " (Target Org: " + pos.getTargetOrganization() + ", Scope: " + pos.getElectorateScope() + ")"
                    ));
                    redirectAttrs.addFlashAttribute("error", "Access Denied: As a member of " + voter.getOrganizationDisplayName() + " (" + voter.getVoterCategoryDisplayName() + "), you are not permitted to vote on " + pos.getDescription() + " (" + pos.getTargetOrganizationDisplayName() + " - " + pos.getElectorateScopeDisplayName() + ").");
                    return "redirect:/ballot";
                }
                if (selectedCandidates.length > pos.getMaxVote()) {
                    redirectAttrs.addFlashAttribute("error", "Maximum votes exceeded for position: " + pos.getDescription());
                    return "redirect:/ballot";
                }

                for (String candStr : selectedCandidates) {
                    try {
                        Long candidateId = Long.parseLong(candStr);
                        Candidate candidate = candidateRepo.findById(candidateId).orElse(null);
                        if (candidate == null || !candidate.getPosition().getId().equals(pos.getId())) {
                            redirectAttrs.addFlashAttribute("error", "Invalid candidate selected for position: " + pos.getDescription());
                            return "redirect:/ballot";
                        }

                        // Strict Multi-Tenant Candidate Validation
                        if (candidate.getOrganization() != null && !candidate.getOrganization().isBlank()
                                && voter.getOrganization() != null && !voter.getOrganization().isBlank()
                                && !"ALL".equalsIgnoreCase(voter.getOrganization())) {
                            if (!OrgMatcher.matches(candidate.getOrganization(), voter.getOrganization())
                                    && !OrgMatcher.matches(pos.getTargetOrganization(), voter.getOrganization())) {
                                auditRepo.save(new AuditLog(
                                        "Unauthorized Candidate Vote Blocked",
                                        "SECURITY_VIOLATION",
                                        request.getRemoteAddr(),
                                        "Voter " + voter.getVoterId() + " (Org: " + voter.getOrganization() + ") attempted unauthorized vote for Candidate " + candidate.getFullName() + " (Org: " + candidate.getOrganization() + ")"
                                ));
                                redirectAttrs.addFlashAttribute("error", "Access Denied: Candidate " + candidate.getFullName() + " does not belong to your authorized organization (" + voter.getOrganizationDisplayName() + ").");
                                return "redirect:/ballot";
                            }
                        }

                        // 1. AES-256 Encrypt vote
                        String encryptedVote = cryptoService.encryptVote(candidateId);

                        // 2. RSA Digital Signature
                        String digitalSignature = cryptoService.signVote(encryptedVote);

                        // 3. SHA-256 Vote Hash
                        String voteHash = cryptoService.calculateVoteHash(voter.getId(), candidateId, pos.getId(), encryptedVote, digitalSignature);

                        Vote vote = new Vote(voter, candidate, pos, encryptedVote, digitalSignature, voteHash);
                        votesToSave.add(vote);
                        summarySelections.add(pos.getDescription() + " -> " + candidate.getFullName());

                    } catch (NumberFormatException e) {
                        redirectAttrs.addFlashAttribute("error", "Invalid candidate format");
                        return "redirect:/ballot";
                    }
                }
            }
        }

        if (votesToSave.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Please select at least one candidate before submitting your ballot.");
            return "redirect:/ballot";
        }

        // Persist all votes atomically
        voteRepo.saveAll(votesToSave);

        // Append to Micro-Blockchain Ledger
        String maskedVoter = maskVoterId(voter.getVoterId());
        String orgTag = voter.getOrganization() != null && !voter.getOrganization().isBlank() ? voter.getOrganization().trim() : "ALL";
        String transactionSummary = "[" + orgTag + "] " + String.join("; ", summarySelections);
        String lastVoteHash = votesToSave.get(votesToSave.size() - 1).getVoteHash();
        BlockchainBlock block = blockchainService.addVoteBlock(lastVoteHash, maskedVoter, transactionSummary);

        // Record Audit Log
        AuditLog audit = new AuditLog(
                "Ballot Submitted",
                block.getBlockHash(),
                request.getRemoteAddr(),
                "[" + orgTag + "] Voter " + maskedVoter + " submitted " + votesToSave.size() + " votes in Block #" + block.getBlockIndex()
        );
        auditRepo.save(audit);

        // Mark voter as voted and generate unique Tracking Token
        String trackingToken = UUID.randomUUID().toString();
        voter.setHasVoted(true);
        voter.setVotedAt(LocalDateTime.now());
        voter.setTrackingToken(trackingToken);
        voterRepo.save(voter);

        session.setAttribute("step", "DONE");
        session.setAttribute("trackingToken", trackingToken);
        session.setAttribute("blockIndex", block.getBlockIndex());
        session.setAttribute("blockHash", block.getBlockHash());

        redirectAttrs.addFlashAttribute("success", "Your ballot has been cryptographically signed, encrypted, and recorded to the blockchain ledger!");
        return "redirect:/receipt";
    }

    @GetMapping("/receipt")
    public String showReceipt(Model model, HttpSession session, HttpServletRequest request) {
        String voterId = (String) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/login";

        Voter voter = voterRepo.findByVoterId(voterId).orElse(null);
        if (voter == null) return "redirect:/login";

        if (!voter.isHasVoted()) {
            return "redirect:/ballot";
        }

        String trackingToken = voter.getTrackingToken();
        if (trackingToken == null) {
            trackingToken = UUID.randomUUID().toString();
            voter.setTrackingToken(trackingToken);
            voterRepo.save(voter);
        }

        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String verificationUrl = baseUrl + "/verify-receipt?token=" + trackingToken;

        String qrCodeDataUrl = qrCodeService.generateQrCodeBase64(verificationUrl, 250, 250);

        ElectionSetting setting = electionRepo.findAll().stream().findFirst().orElse(new ElectionSetting());

        model.addAttribute("voter", voter);
        model.addAttribute("maskedVoterId", maskVoterId(voter.getVoterId()));
        model.addAttribute("trackingToken", trackingToken);
        model.addAttribute("verificationUrl", verificationUrl);
        model.addAttribute("qrCodeDataUrl", qrCodeDataUrl);
        model.addAttribute("electionTitle", setting.getElectionTitle());

        return "voter/receipt";
    }

    @GetMapping("/verify-receipt")
    public String verifyReceipt(@RequestParam(value = "token", required = false) String token, Model model) {
        if (token != null && !token.isBlank()) {
            Optional<Voter> voterOpt = voterRepo.findByTrackingToken(token.trim());
            if (voterOpt.isPresent()) {
                Voter voter = voterOpt.get();
                model.addAttribute("found", true);
                model.addAttribute("maskedVoterId", maskVoterId(voter.getVoterId()));
                model.addAttribute("voterOrganization", voter.getOrganizationDisplayName());
                model.addAttribute("votedAt", voter.getVotedAt());
                model.addAttribute("token", token);
                model.addAttribute("message", "Ballot successfully verified on the Immutable Ledger! Timestamp: " + voter.getVotedAt());
            } else {
                model.addAttribute("found", false);
                model.addAttribute("token", token);
                model.addAttribute("message", "No ballot record matching tracking token: " + token);
            }
        }
        return "voter/verify-receipt";
    }

    @ModelAttribute("electionSetting")
    public ElectionSetting populateElectionSetting() {
        return electionRepo.findAll().stream().findFirst().orElseGet(() -> {
            ElectionSetting s = new ElectionSetting("Apex Multi-Org Leadership Council", "GENERAL", "Executive Leadership Election 2026", "ACTIVE");
            return electionRepo.save(s);
        });
    }

    public boolean isPositionEligibleForVoter(Position pos, Voter voter) {
        if (pos == null || voter == null) return false;

        String voterOrg = (voter.getOrganization() != null) ? voter.getOrganization().trim() : "";
        String targetOrg = (pos.getTargetOrganization() != null) ? pos.getTargetOrganization().trim() : "ALL";

        // 1. Strict Multi-Tenant Organization Isolation:
        // A voter must ONLY be allowed to view and vote for positions belonging to their specific organization.
        // For example, an Infosys employee MUST ONLY see Infosys positions (like Cultural Event Organizer).
        // Positions from other organizations, schools, or public elections MUST NOT be displayed or voted on!
        if (!voterOrg.isBlank() && !"ALL".equalsIgnoreCase(voterOrg) && !"PUBLIC".equalsIgnoreCase(voterOrg)) {
            // Corporate / School / Organization voter: MUST strictly match the position's target organization
            if (!OrgMatcher.matches(targetOrg, voterOrg)) {
                return false; // Strictly isolate: do not display or allow other positions!
            }
        } else {
            // General / Unassigned voters can only vote on positions open to ALL
            if (pos.isOrganizationRestricted()) {
                return false;
            }
        }

        // 2. Electorate Scope Isolation (Students vs Office/Corporate Employees vs Faculty)
        String scope = pos.getElectorateScope();
        String voterCat = voter.getVoterCategory();
        if (voterCat == null) voterCat = "STUDENT";

        if ("STUDENT".equalsIgnoreCase(voterCat)) {
            // Students can ONLY vote on student body positions or universal public elections
            if (!"STUDENT".equalsIgnoreCase(scope) && !"ALL".equalsIgnoreCase(scope)) {
                return false;
            }
        } else if ("OFFICE_STAFF".equalsIgnoreCase(voterCat) || "EMPLOYEE".equalsIgnoreCase(voterCat)) {
            // Corporate / Office employees cannot vote in student-only elections or faculty-only governance
            if ("STUDENT".equalsIgnoreCase(scope) || "FACULTY_STAFF".equalsIgnoreCase(scope)) {
                return false;
            }
        } else if ("FACULTY".equalsIgnoreCase(voterCat)) {
            if ("STUDENT".equalsIgnoreCase(scope) || "OFFICE_ORGANIZATION".equalsIgnoreCase(scope)) {
                return false;
            }
        }

        // 3. Department / Division check (if configured)
        String targetDept = pos.getTargetDepartment();
        if (targetDept != null && !"ALL".equalsIgnoreCase(targetDept.trim()) && !targetDept.isBlank()) {
            if (voter.getDepartment() == null || !targetDept.trim().equalsIgnoreCase(voter.getDepartment().trim())) {
                return false;
            }
        }

        return true;
    }

    private String maskVoterId(String id) {
        if (id == null || id.length() <= 4) return "VOTER-***";
        return id.substring(0, 3) + "***" + id.substring(id.length() - 3);
    }
}
