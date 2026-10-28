package com.votesystem.controller;

import com.votesystem.model.ElectionSetting;
import com.votesystem.model.Voter;
import com.votesystem.repository.ElectionSettingRepository;
import com.votesystem.repository.VoterRepository;
import com.votesystem.service.ElectionScheduleService;
import com.votesystem.service.FaceVerificationService;
import com.votesystem.service.FaceVerificationService.FaceVerificationResult;
import com.votesystem.service.FaceVerificationService.LivenessChallenge;
import com.votesystem.service.OtpService;
import com.votesystem.service.OtpService.OtpValidationResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class VoterAuthController {

    private static final Logger log = LoggerFactory.getLogger(VoterAuthController.class);

    private final VoterRepository voterRepo;
    private final ElectionSettingRepository electionRepo;
    private final PasswordEncoder passwordEncoder;
    private final FaceVerificationService faceService;
    private final OtpService otpService;
    private final ElectionScheduleService scheduleService;

    public VoterAuthController(VoterRepository voterRepo, ElectionSettingRepository electionRepo,
                               PasswordEncoder passwordEncoder, FaceVerificationService faceService,
                               OtpService otpService, ElectionScheduleService scheduleService) {
        this.voterRepo = voterRepo;
        this.electionRepo = electionRepo;
        this.passwordEncoder = passwordEncoder;
        this.faceService = faceService;
        this.otpService = otpService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("voterId") != null) {
            String step = (String) session.getAttribute("step");
            if ("STEP_FACE".equals(step)) return "redirect:/verify-face";
            if ("STEP_OTP".equals(step)) return "redirect:/verify-otp";
            if ("STEP_BALLOT".equals(step)) return "redirect:/ballot";
            if ("DONE".equals(step)) return "redirect:/receipt";
        }
        return "redirect:/login";
    }

    @GetMapping({"/login", "/vote", "/vote/{org}"})
    public String showLogin(@PathVariable(value = "org", required = false) String pathOrg,
                            @RequestParam(value = "org", required = false) String queryOrg,
                            Model model,
                            HttpSession session,
                            HttpServletRequest request) {
        if (session.getAttribute("voterId") != null) {
            return "redirect:/";
        }

        String effectiveOrg = (pathOrg != null && !pathOrg.isBlank()) ? pathOrg.trim() : (queryOrg != null ? queryOrg.trim() : null);
        if (effectiveOrg != null && !effectiveOrg.isBlank()) {
            session.setAttribute("targetOrg", effectiveOrg);
        } else if (session.getAttribute("targetOrg") != null) {
            effectiveOrg = (String) session.getAttribute("targetOrg");
        }

        boolean isVoterPortalRoute = request.getRequestURI().contains("/vote");
        ElectionSetting setting = getElectionSetting();
        ElectionScheduleService.ScheduleInfo schedule = scheduleService.getSchedule(effectiveOrg);

        model.addAttribute("electionTitle", setting.getElectionTitle());
        model.addAttribute("electionStatus", setting.getStatus());
        model.addAttribute("scheduleInfo", schedule);
        model.addAttribute("effectiveOrg", effectiveOrg);
        model.addAttribute("isVoterPortalRoute", isVoterPortalRoute);
        return "voter/login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("voter") String voterId,
                              @RequestParam("password") String password,
                              @RequestParam(value = "org", required = false) String orgParam,
                              HttpSession session,
                              RedirectAttributes redirectAttrs) {
        ElectionSetting setting = getElectionSetting();
        String orgQuerySuffix = (orgParam != null && !orgParam.isBlank()) ? "?org=" + orgParam : "";

        if ("PAUSED".equalsIgnoreCase(setting.getStatus())) {
            redirectAttrs.addFlashAttribute("error", "Voting is temporarily paused by the election commission.");
            return "redirect:/login" + orgQuerySuffix;
        }
        if ("CONCLUDED".equalsIgnoreCase(setting.getStatus())) {
            redirectAttrs.addFlashAttribute("error", "The election has concluded. Ballots are sealed.");
            return "redirect:/login" + orgQuerySuffix;
        }

        Optional<Voter> voterOpt = voterRepo.findByVoterId(voterId.trim());
        if (voterOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Invalid Voter ID.");
            return "redirect:/login" + orgQuerySuffix;
        }

        Voter voter = voterOpt.get();
        if (!passwordEncoder.matches(password, voter.getPassword())) {
            redirectAttrs.addFlashAttribute("error", "Incorrect Password.");
            return "redirect:/login" + orgQuerySuffix;
        }

        // Time Limit Enforcement for Voter's Organization
        ElectionScheduleService.ScheduleInfo schedule = scheduleService.getSchedule(voter.getOrganization());
        if (schedule.isNotStarted()) {
            redirectAttrs.addFlashAttribute("error", "Voting has not commenced yet. Polls for " 
                    + voter.getOrganizationDisplayName() + " will open on " + schedule.getFormattedStartTime() + ".");
            return "redirect:/login" + orgQuerySuffix;
        }
        if (schedule.isExpired()) {
            redirectAttrs.addFlashAttribute("error", "The election voting window for " + voter.getOrganizationDisplayName() 
                    + " has concluded. Polls closed on " + schedule.getFormattedEndTime() + ". Ballots are sealed.");
            return "redirect:/login" + orgQuerySuffix;
        }

        // Strip any residual administrative session attributes to isolate voter context
        session.removeAttribute("adminId");
        session.removeAttribute("adminUsername");
        session.removeAttribute("adminName");
        session.removeAttribute("adminPhoto");
        session.removeAttribute("adminRole");
        session.removeAttribute("adminOrg");
        session.removeAttribute("adminTitle");
        session.removeAttribute("superAdminViewOrg");

        // Check if voter has already voted
        if (voter.isHasVoted()) {
            session.setAttribute("voterId", voter.getVoterId());
            session.setAttribute("voterName", voter.getFullName());
            session.setAttribute("step", "DONE");
            return "redirect:/receipt";
        }

        // Credentials valid -> Proceed to Face Verification step
        session.setAttribute("voterDbId", voter.getId());
        session.setAttribute("voterId", voter.getVoterId());
        session.setAttribute("voterName", voter.getFullName());
        session.setAttribute("voterPhoto", voter.getPhoto());
        session.setAttribute("voterOrg", voter.getOrganization());
        session.setAttribute("step", "STEP_FACE");

        return "redirect:/verify-face";
    }

    @GetMapping("/verify-face")
    public String showFaceVerification(Model model, HttpSession session) {
        String voterId = (String) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/login";

        Voter voter = voterRepo.findByVoterId(voterId).orElse(null);
        if (voter == null) return "redirect:/login";

        ElectionSetting setting = getElectionSetting();
        LivenessChallenge challenge = faceService.generateLivenessChallenge();
        session.setAttribute("livenessChallengeToken", challenge.getChallengeToken());

        model.addAttribute("voter", voter);
        model.addAttribute("livenessInstruction", challenge.getInstruction());
        model.addAttribute("livenessToken", challenge.getChallengeToken());
        model.addAttribute("emergencyKeyEnabled", true);

        return "voter/face";
    }

    @PostMapping("/verify-face")
    public String handleFaceVerification(@RequestParam(value = "image", required = false) String capturedImageBase64,
                                         @RequestParam(value = "emergencyKey", required = false) String emergencyKey,
                                         HttpSession session,
                                         RedirectAttributes redirectAttrs) {
        String voterId = (String) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/login";

        Voter voter = voterRepo.findByVoterId(voterId).orElse(null);
        if (voter == null) return "redirect:/login";

        ElectionSetting setting = getElectionSetting();
        FaceVerificationResult result = faceService.verifyFace(
                voter.getVoterId(),
                voter.getPhoto(),
                capturedImageBase64,
                emergencyKey,
                setting.getEmergencyKey()
        );

        if (result.isSuccess()) {
            log.info("Face verification passed for voter {}", voterId);
            session.setAttribute("step", "STEP_OTP");

            // Generate and dispatch OTP
            String otpCode = otpService.generateAndSendOtp(voter);
            session.setAttribute("simulatedOtp", otpCode); // For convenient local test notifications

            return "redirect:/verify-otp";
        } else {
            redirectAttrs.addFlashAttribute("error", result.getMessage());
            return "redirect:/verify-face";
        }
    }

    @GetMapping("/verify-otp")
    public String showOtpVerification(Model model, HttpSession session) {
        String voterId = (String) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/login";

        Voter voter = voterRepo.findByVoterId(voterId).orElse(null);
        if (voter == null) return "redirect:/login";

        model.addAttribute("voter", voter);
        model.addAttribute("maskedEmail", maskEmail(voter.getEmail()));
        model.addAttribute("simulatedOtp", session.getAttribute("simulatedOtp"));

        return "voter/otp";
    }

    @PostMapping("/verify-otp")
    public String handleOtpVerification(@RequestParam("otp") String otpInput,
                                        HttpSession session,
                                        RedirectAttributes redirectAttrs) {
        String voterId = (String) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/login";

        Voter voter = voterRepo.findByVoterId(voterId).orElse(null);
        if (voter == null) return "redirect:/login";

        OtpValidationResult validation = otpService.validateOtp(voter, otpInput);
        if (validation.isValid()) {
            session.setAttribute("step", "STEP_BALLOT");
            session.removeAttribute("simulatedOtp");
            return "redirect:/ballot";
        } else {
            redirectAttrs.addFlashAttribute("error", validation.getMessage());
            return "redirect:/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttrs) {
        String voterId = (String) session.getAttribute("voterId");
        if (voterId == null) return "redirect:/login";

        Voter voter = voterRepo.findByVoterId(voterId).orElse(null);
        if (voter == null) return "redirect:/login";

        String otp = otpService.generateAndSendOtp(voter);
        session.setAttribute("simulatedOtp", otp);
        redirectAttrs.addFlashAttribute("success", "A new OTP code has been dispatched to your email.");
        return "redirect:/verify-otp";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @ModelAttribute("electionSetting")
    public ElectionSetting populateElectionSetting() {
        return getElectionSetting();
    }

    private ElectionSetting getElectionSetting() {
        return electionRepo.findAll().stream().findFirst().orElseGet(() -> {
            ElectionSetting s = new ElectionSetting("Apex Multi-Org Leadership Council", "GENERAL", "Executive Leadership Election 2026", "ACTIVE");
            return electionRepo.save(s);
        });
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "registered email";
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) return name + "***@" + domain;
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }
}
