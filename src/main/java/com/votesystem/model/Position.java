package com.votesystem.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String description;

    @Column(name = "max_vote", nullable = false)
    private Integer maxVote = 1;

    @Column(nullable = false)
    private Integer priority = 1;

    @Column(name = "electorate_scope", length = 30)
    private String electorateScope = "ALL"; // STUDENT, OFFICE_ORGANIZATION, FACULTY_STAFF, ALL

    @Column(name = "target_department", length = 100)
    private String targetDepartment = "ALL";

    @Column(name = "target_organization", length = 100)
    private String targetOrganization = "ALL"; // e.g. "Infosys", "ALL"

    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<Candidate> candidates = new ArrayList<>();

    public Position() {}

    public Position(String description, Integer maxVote, Integer priority) {
        this.description = description;
        this.maxVote = maxVote;
        this.priority = priority;
        this.electorateScope = "ALL";
        this.targetOrganization = "ALL";
    }

    public Position(String description, Integer maxVote, Integer priority, String electorateScope) {
        this.description = description;
        this.maxVote = maxVote;
        this.priority = priority;
        this.electorateScope = electorateScope != null ? electorateScope : "ALL";
        this.targetOrganization = "ALL";
    }

    public Position(String description, Integer maxVote, Integer priority, String electorateScope, String targetDepartment) {
        this.description = description;
        this.maxVote = maxVote;
        this.priority = priority;
        this.electorateScope = electorateScope != null ? electorateScope : "ALL";
        this.targetDepartment = targetDepartment != null ? targetDepartment : "ALL";
        this.targetOrganization = "ALL";
    }

    public Position(String description, Integer maxVote, Integer priority, String electorateScope, String targetDepartment, String targetOrganization) {
        this.description = description;
        this.maxVote = maxVote;
        this.priority = priority;
        this.electorateScope = electorateScope != null ? electorateScope : "ALL";
        this.targetDepartment = targetDepartment != null ? targetDepartment : "ALL";
        this.targetOrganization = targetOrganization != null && !targetOrganization.isBlank() ? targetOrganization : "ALL";
    }

    // Dynamic Display Helpers for Scope and Organization
    public String getTargetOrganizationDisplayName() {
        if (targetOrganization == null || "ALL".equalsIgnoreCase(targetOrganization.trim()) || targetOrganization.isBlank()) {
            return "Open to All Organizations";
        }
        return targetOrganization.trim();
    }

    public boolean isOrganizationRestricted() {
        return targetOrganization != null && !"ALL".equalsIgnoreCase(targetOrganization.trim()) && !targetOrganization.isBlank();
    }

    public String getTargetOrganizationBadgeClass() {
        if (!isOrganizationRestricted()) return "bg-light text-dark border";
        if ("Infosys".equalsIgnoreCase(targetOrganization.trim())) return "bg-primary text-white";
        return "bg-dark text-white";
    }

    public String getElectorateScopeDisplayName() {
        if ("STUDENT".equalsIgnoreCase(electorateScope)) return "Student Body Only";
        if ("OFFICE_ORGANIZATION".equalsIgnoreCase(electorateScope)) return "Organization Office Event";
        if ("FACULTY_STAFF".equalsIgnoreCase(electorateScope)) return "Faculty / Academic Staff";
        return "Open to All Electorates";
    }

    public String getElectorateScopeBadgeClass() {
        if ("STUDENT".equalsIgnoreCase(electorateScope)) return "bg-primary text-white";
        if ("OFFICE_ORGANIZATION".equalsIgnoreCase(electorateScope)) return "bg-dark text-white";
        if ("FACULTY_STAFF".equalsIgnoreCase(electorateScope)) return "bg-info text-dark";
        return "bg-success text-white";
    }

    public String getElectorateScopeIcon() {
        if ("STUDENT".equalsIgnoreCase(electorateScope)) return "fa-solid fa-graduation-cap";
        if ("OFFICE_ORGANIZATION".equalsIgnoreCase(electorateScope)) return "fa-solid fa-building-user";
        if ("FACULTY_STAFF".equalsIgnoreCase(electorateScope)) return "fa-solid fa-chalkboard-user";
        return "fa-solid fa-globe";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxVote() {
        return maxVote;
    }

    public void setMaxVote(Integer maxVote) {
        this.maxVote = maxVote;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public String getElectorateScope() {
        return electorateScope != null ? electorateScope : "ALL";
    }

    public void setElectorateScope(String electorateScope) {
        this.electorateScope = electorateScope;
    }

    public String getTargetDepartment() {
        return targetDepartment != null ? targetDepartment : "ALL";
    }

    public void setTargetDepartment(String targetDepartment) {
        this.targetDepartment = targetDepartment;
    }

    public String getTargetOrganization() {
        return targetOrganization != null && !targetOrganization.isBlank() ? targetOrganization : "ALL";
    }

    public void setTargetOrganization(String targetOrganization) {
        this.targetOrganization = targetOrganization != null && !targetOrganization.isBlank() ? targetOrganization : "ALL";
    }

    public String getSlug() {
        if (description == null) return "";
        return description.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
