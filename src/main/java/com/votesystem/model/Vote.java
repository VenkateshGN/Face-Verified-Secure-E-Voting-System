package com.votesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"voters_id", "position_id", "candidate_id"})
})
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "voters_id", nullable = false)
    private Voter voter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @Column(name = "encrypted_vote", columnDefinition = "TEXT")
    private String encryptedVote;

    @Column(name = "digital_signature", columnDefinition = "TEXT")
    private String digitalSignature;

    @Column(name = "vote_hash", length = 128)
    private String voteHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Vote() {
        this.createdAt = LocalDateTime.now();
    }

    public Vote(Voter voter, Candidate candidate, Position position, String encryptedVote, String digitalSignature, String voteHash) {
        this.voter = voter;
        this.candidate = candidate;
        this.position = position;
        this.encryptedVote = encryptedVote;
        this.digitalSignature = digitalSignature;
        this.voteHash = voteHash;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Voter getVoter() {
        return voter;
    }

    public void setVoter(Voter voter) {
        this.voter = voter;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getEncryptedVote() {
        return encryptedVote;
    }

    public void setEncryptedVote(String encryptedVote) {
        this.encryptedVote = encryptedVote;
    }

    public String getDigitalSignature() {
        return digitalSignature;
    }

    public void setDigitalSignature(String digitalSignature) {
        this.digitalSignature = digitalSignature;
    }

    public String getVoteHash() {
        return voteHash;
    }

    public void setVoteHash(String voteHash) {
        this.voteHash = voteHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Transient
    public String getOrganization() {
        if (position != null && position.getTargetOrganization() != null && !position.getTargetOrganization().isBlank() && !"ALL".equalsIgnoreCase(position.getTargetOrganization())) {
            return position.getTargetOrganization().trim();
        }
        if (candidate != null && candidate.getOrganization() != null && !candidate.getOrganization().isBlank() && !"ALL".equalsIgnoreCase(candidate.getOrganization())) {
            return candidate.getOrganization().trim();
        }
        if (voter != null && voter.getOrganization() != null && !voter.getOrganization().isBlank()) {
            return voter.getOrganization().trim();
        }
        return "ALL";
    }

    @Transient
    public String getOrganizationDisplayName() {
        String org = getOrganization();
        return "ALL".equalsIgnoreCase(org) ? "General / Public" : org;
    }
}
