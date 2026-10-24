package com.votesystem.service;

import com.votesystem.model.Voter;
import com.votesystem.repository.VoterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_VALIDITY_MINUTES = 5;

    private final VoterRepository voterRepo;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public OtpService(VoterRepository voterRepo) {
        this.voterRepo = voterRepo;
    }

    /**
     * Generates and stores a new 6-digit OTP for the given voter.
     */
    public String generateAndSendOtp(Voter voter) {
        int code = 100000 + secureRandom.nextInt(900000);
        String otpString = String.valueOf(code);

        voter.setOtp(otpString);
        voter.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        voterRepo.save(voter);

        log.info(">>>>> [SECURE 2FA DISPATCH] Voter: {} ({}) | Generated OTP: {} | Valid until: {} <<<<<",
                voter.getVoterId(), voter.getEmail(), otpString, voter.getOtpExpiry());

        // Dispatch real email if mailSender configured, or log simulated dispatch
        sendOtpEmail(voter.getEmail(), otpString, voter.getFirstname());

        return otpString;
    }

    /**
     * Validates the provided OTP.
     */
    public OtpValidationResult validateOtp(Voter voter, String enteredOtp) {
        if (voter == null) {
            return new OtpValidationResult(false, "Voter session not found");
        }

        if (voter.getOtp() == null || voter.getOtpExpiry() == null) {
            return new OtpValidationResult(false, "No active OTP generated. Please request a new code.");
        }

        if (LocalDateTime.now().isAfter(voter.getOtpExpiry())) {
            return new OtpValidationResult(false, "OTP has expired. Please request a new one.");
        }

        if (!voter.getOtp().trim().equals(enteredOtp != null ? enteredOtp.trim() : "")) {
            return new OtpValidationResult(false, "Invalid OTP code entered. Please try again.");
        }

        // OTP verified successfully - invalidate it to prevent replay attacks
        voter.setOtp(null);
        voter.setOtpExpiry(null);
        voterRepo.save(voter);

        return new OtpValidationResult(true, "OTP verified successfully");
    }

    private void sendOtpEmail(String recipientEmail, String otp, String voterName) {
        if (mailSender != null && recipientEmail != null && recipientEmail.contains("@")) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipientEmail);
                message.setSubject("Your Secure E-Voting System OTP Code");
                message.setText("Dear " + voterName + ",\n\n"
                        + "Your One-Time Password (OTP) for casting your ballot is: " + otp + "\n\n"
                        + "This code is valid for 5 minutes. Do not share this OTP with anyone.\n\n"
                        + "Secure E-Voting Commission");
                mailSender.send(message);
                log.info("Real email dispatched to {}", recipientEmail);
            } catch (Exception e) {
                log.warn("Could not dispatch real email (fallback to simulated mode): {}", e.getMessage());
            }
        }
    }

    public static class OtpValidationResult {
        private final boolean valid;
        private final String message;

        public OtpValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}
