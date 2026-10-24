package com.votesystem.service;

import com.votesystem.model.Candidate;
import com.votesystem.model.TamperLog;
import com.votesystem.model.Vote;
import com.votesystem.repository.CandidateRepository;
import com.votesystem.repository.TamperLogRepository;
import com.votesystem.repository.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VoteIntegrityService {

    private static final Logger log = LoggerFactory.getLogger(VoteIntegrityService.class);

    private final VoteRepository voteRepo;
    private final CandidateRepository candidateRepo;
    private final TamperLogRepository tamperLogRepo;
    private final CryptoService cryptoService;

    public VoteIntegrityService(VoteRepository voteRepo, CandidateRepository candidateRepo, TamperLogRepository tamperLogRepo, CryptoService cryptoService) {
        this.voteRepo = voteRepo;
        this.candidateRepo = candidateRepo;
        this.tamperLogRepo = tamperLogRepo;
        this.cryptoService = cryptoService;
    }

    public VoteAuditReport auditAllVotes(String clientIp, String userAgent) {
        List<Vote> votes = voteRepo.findAllWithDetails();
        List<VoteAuditItem> items = new ArrayList<>();
        int validCount = 0;
        int tamperedCount = 0;

        for (Vote v : votes) {
            VoteVerificationResult result = verifyVote(v);
            boolean isValid = result.isValid();

            if (isValid) {
                validCount++;
            } else {
                tamperedCount++;
                // Record to tamper_logs if not already logged
                if (!tamperLogRepo.existsByVoteId(v.getId())) {
                    TamperLog logEntry = new TamperLog(
                            v.getVoter() != null ? v.getVoter().getVoterId() : "Unknown",
                            v.getId(),
                            "Cryptographic Tampering Detected",
                            clientIp != null ? clientIp : "127.0.0.1",
                            userAgent != null ? userAgent : "Internal Audit Scanner",
                            result.getReasons()
                    );
                    tamperLogRepo.save(logEntry);
                    log.warn("Logged vote tampering for Vote ID #{}", v.getId());
                }
            }

            String originalCandidateName = "None";
            if (result.getDecryptedCandidateId() != null) {
                originalCandidateName = candidateRepo.findById(result.getDecryptedCandidateId())
                        .map(Candidate::getFullName)
                        .orElse("Unknown Candidate (ID: " + result.getDecryptedCandidateId() + ")");
            }

            items.add(new VoteAuditItem(v, isValid, result.getReasons(), result.getDetails(), originalCandidateName));
        }

        return new VoteAuditReport(votes.size(), validCount, tamperedCount, items);
    }

    public VoteVerificationResult verifyVote(Vote vote) {
        List<String> reasons = new ArrayList<>();
        List<String> details = new ArrayList<>();

        // 1. Recalculate SHA-256 vote hash
        String expectedHash = cryptoService.calculateVoteHash(
                vote.getVoter().getId(),
                vote.getCandidate().getId(),
                vote.getPosition().getId(),
                vote.getEncryptedVote(),
                vote.getDigitalSignature()
        );

        boolean hashOk = expectedHash.equalsIgnoreCase(vote.getVoteHash());
        if (hashOk) {
            details.add("SHA256 Hash verified");
        } else {
            reasons.add("Hash mismatch (Stored: " + shorten(vote.getVoteHash()) + " vs Calc: " + shorten(expectedHash) + ")");
            details.add("SHA256 Hash check failed");
        }

        // 2. Decrypt AES-256 ciphertext payload
        Long decryptedCandidateId = cryptoService.decryptVote(vote.getEncryptedVote());
        boolean decryptOk = false;
        if (decryptedCandidateId != null) {
            if (decryptedCandidateId.equals(vote.getCandidate().getId())) {
                decryptOk = true;
                details.add("Decrypted Candidate ID matches Database record (" + decryptedCandidateId + ")");
            } else {
                reasons.add("Candidate mismatch (DB Candidate: #" + vote.getCandidate().getId() + " vs Decrypted Payload: #" + decryptedCandidateId + ")");
                details.add("Database Candidate ID has been altered after ballot casting!");
            }
        } else {
            reasons.add("AES Decryption failed / Corrupted ciphertext");
            details.add("AES-256 payload could not be decrypted");
        }

        // 3. Verify RSA-256 digital signature
        boolean sigOk = cryptoService.verifySignature(vote.getEncryptedVote(), vote.getDigitalSignature());
        if (sigOk) {
            details.add("RSA-2048 Digital Signature verified with election public key");
        } else {
            reasons.add("RSA Signature verification failed (Invalid or forged signature)");
            details.add("Digital signature check failed");
        }

        boolean valid = hashOk && decryptOk && sigOk;
        return new VoteVerificationResult(valid, String.join(", ", reasons), String.join(". ", details) + ".", decryptedCandidateId);
    }

    @Transactional
    public boolean simulateTamper(Long voteId, Long newCandidateId) {
        return voteRepo.findById(voteId).map(vote -> {
            return candidateRepo.findById(newCandidateId).map(newCandidate -> {
                vote.setCandidate(newCandidate); // Alter candidate in DB without updating encrypted_vote or signature!
                voteRepo.save(vote);
                log.info("Tampering simulated on Vote #{}: Candidate set to #{}", voteId, newCandidateId);
                return true;
            }).orElse(false);
        }).orElse(false);
    }

    private String shorten(String s) {
        if (s == null) return "null";
        return s.length() > 8 ? s.substring(0, 8) + "..." : s;
    }

    // Helper result classes
    public static class VoteVerificationResult {
        private final boolean valid;
        private final String reasons;
        private final String details;
        private final Long decryptedCandidateId;

        public VoteVerificationResult(boolean valid, String reasons, String details, Long decryptedCandidateId) {
            this.valid = valid;
            this.reasons = reasons;
            this.details = details;
            this.decryptedCandidateId = decryptedCandidateId;
        }

        public boolean isValid() { return valid; }
        public String getReasons() { return reasons; }
        public String getDetails() { return details; }
        public Long getDecryptedCandidateId() { return decryptedCandidateId; }
    }

    public static class VoteAuditItem {
        private final Vote vote;
        private final boolean valid;
        private final String reasons;
        private final String details;
        private final String originalCandidateName;

        public VoteAuditItem(Vote vote, boolean valid, String reasons, String details, String originalCandidateName) {
            this.vote = vote;
            this.valid = valid;
            this.reasons = reasons;
            this.details = details;
            this.originalCandidateName = originalCandidateName;
        }

        public Vote getVote() { return vote; }
        public boolean isValid() { return valid; }
        public String getReasons() { return reasons; }
        public String getDetails() { return details; }
        public String getOriginalCandidateName() { return originalCandidateName; }
    }

    public static class VoteAuditReport {
        private final int totalVotes;
        private final int validVotes;
        private final int tamperedVotes;
        private final List<VoteAuditItem> items;

        public VoteAuditReport(int totalVotes, int validVotes, int tamperedVotes, List<VoteAuditItem> items) {
            this.totalVotes = totalVotes;
            this.validVotes = validVotes;
            this.tamperedVotes = tamperedVotes;
            this.items = items;
        }

        public int getTotalVotes() { return totalVotes; }
        public int getValidVotes() { return validVotes; }
        public int getTamperedVotes() { return tamperedVotes; }
        public List<VoteAuditItem> getItems() { return items; }
    }
}
