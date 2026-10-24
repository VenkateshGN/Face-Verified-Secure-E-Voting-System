package com.votesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "hash_value", length = 255)
    private String hashValue;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(String action, String hashValue, String ipAddress, String details) {
        this.action = action;
        this.hashValue = hashValue;
        this.ipAddress = ipAddress;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Transient
    public String getOrganization() {
        if (details != null) {
            if (details.startsWith("[")) {
                int end = details.indexOf(']');
                if (end > 1) {
                    return details.substring(1, end).trim();
                }
            }
            if (details.toLowerCase().contains("infosys")) return "Infosys";
            if (details.toLowerCase().contains("crce")) return "CRCE College";
            if (details.toLowerCase().contains("wipro")) return "Wipro";
        }
        if (action != null) {
            if (action.toLowerCase().contains("infosys")) return "Infosys";
            if (action.toLowerCase().contains("crce")) return "CRCE College";
            if (action.toLowerCase().contains("wipro")) return "Wipro";
        }
        return "ALL";
    }
}
