package com.votesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tamper_logs")
public class TamperLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voter_id", length = 50)
    private String voterId;

    @Column(name = "vote_id")
    private Long voteId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public TamperLog() {
        this.timestamp = LocalDateTime.now();
    }

    public TamperLog(String voterId, Long voteId, String action, String ipAddress, String userAgent, String reason) {
        this.voterId = voterId;
        this.voteId = voteId;
        this.action = action;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVoterId() {
        return voterId;
    }

    public void setVoterId(String voterId) {
        this.voterId = voterId;
    }

    public Long getVoteId() {
        return voteId;
    }

    public void setVoteId(Long voteId) {
        this.voteId = voteId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Transient
    public String getOrganization() {
        if (voterId != null) {
            if (voterId.toUpperCase().startsWith("INFY")) return "Infosys";
            if (voterId.toUpperCase().startsWith("WIPRO")) return "Wipro";
            if (voterId.toUpperCase().startsWith("STU")) return "CRCE College";
        }
        if (reason != null) {
            if (reason.toLowerCase().contains("infosys")) return "Infosys";
            if (reason.toLowerCase().contains("crce")) return "CRCE College";
            if (reason.toLowerCase().contains("wipro")) return "Wipro";
        }
        return "ALL";
    }
}
