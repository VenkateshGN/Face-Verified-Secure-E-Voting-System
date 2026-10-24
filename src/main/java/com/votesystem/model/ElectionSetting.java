package com.votesystem.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "election_settings")
public class ElectionSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_name")
    private String organizationName = "Apex Leadership Council";

    @Column(name = "organization_type", length = 30)
    private String organizationType = "GENERAL"; // SCHOOL_COLLEGE, CORPORATE, COMMUNITY_NGO, CIVIC_GOVERNMENT, GENERAL

    @Column(name = "election_title", nullable = false)
    private String electionTitle = "Executive Leadership Election 2026";

    @Column(name = "election_subtitle", columnDefinition = "TEXT")
    private String electionSubtitle = "Universal multi-organization biometric voting platform for schools, companies, and organizations.";

    @Column(name = "voter_id_label", length = 60)
    private String voterIdLabel = "Voter ID / Member ID / Roll No";

    @Column(name = "symbol_label", length = 60)
    private String symbolLabel = "Emblem / House Badge / Logo";

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, PAUSED, CONCLUDED, DRAFT

    @Column(name = "liveness_check_enabled", nullable = false)
    private boolean livenessCheckEnabled = true;

    @Column(name = "face_match_threshold", nullable = false)
    private double faceMatchThreshold = 0.70;

    @Column(name = "emergency_key", length = 50)
    private String emergencyKey = "VOTE2026";

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "time_limit_enabled", nullable = false)
    private boolean timeLimitEnabled = false;

    public ElectionSetting() {}

    public ElectionSetting(String electionTitle, String status) {
        this.electionTitle = electionTitle;
        this.status = status;
    }

    public ElectionSetting(String organizationName, String organizationType, String electionTitle, String status) {
        this.organizationName = organizationName;
        this.organizationType = organizationType;
        this.electionTitle = electionTitle;
        this.status = status;
    }

    // Dynamic Display Helpers
    public String getOrganizationTypeDisplayName() {
        if ("SCHOOL_COLLEGE".equalsIgnoreCase(organizationType)) return "School / College / University";
        if ("CORPORATE".equalsIgnoreCase(organizationType)) return "Company / Corporate Enterprise";
        if ("COMMUNITY_NGO".equalsIgnoreCase(organizationType)) return "Organization / NGO / Society";
        if ("CIVIC_GOVERNMENT".equalsIgnoreCase(organizationType)) return "Civic / Public Democratic Body";
        return "Universal Organization";
    }

    public String getOrganizationTypeIcon() {
        if ("SCHOOL_COLLEGE".equalsIgnoreCase(organizationType)) return "fa-solid fa-graduation-cap";
        if ("CORPORATE".equalsIgnoreCase(organizationType)) return "fa-solid fa-building";
        if ("COMMUNITY_NGO".equalsIgnoreCase(organizationType)) return "fa-solid fa-users-line";
        if ("CIVIC_GOVERNMENT".equalsIgnoreCase(organizationType)) return "fa-solid fa-landmark";
        return "fa-solid fa-sitemap";
    }

    public String getOrganizationTypeBadgeClass() {
        if ("SCHOOL_COLLEGE".equalsIgnoreCase(organizationType)) return "bg-primary text-white";
        if ("CORPORATE".equalsIgnoreCase(organizationType)) return "bg-dark text-white";
        if ("COMMUNITY_NGO".equalsIgnoreCase(organizationType)) return "bg-warning text-dark";
        if ("CIVIC_GOVERNMENT".equalsIgnoreCase(organizationType)) return "bg-info text-white";
        return "bg-success text-white";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrganizationName() {
        return organizationName != null && !organizationName.isBlank() ? organizationName : "Apex Leadership Council";
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationType() {
        return organizationType != null ? organizationType : "GENERAL";
    }

    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    public String getElectionTitle() {
        return electionTitle;
    }

    public void setElectionTitle(String electionTitle) {
        this.electionTitle = electionTitle;
    }

    public String getElectionSubtitle() {
        return electionSubtitle != null ? electionSubtitle : "Universal multi-organization voting platform";
    }

    public void setElectionSubtitle(String electionSubtitle) {
        this.electionSubtitle = electionSubtitle;
    }

    public String getVoterIdLabel() {
        return voterIdLabel != null && !voterIdLabel.isBlank() ? voterIdLabel : "Voter ID / Member ID / Roll No";
    }

    public void setVoterIdLabel(String voterIdLabel) {
        this.voterIdLabel = voterIdLabel;
    }

    public String getSymbolLabel() {
        return symbolLabel != null && !symbolLabel.isBlank() ? symbolLabel : "Emblem / House Badge / Logo";
    }

    public void setSymbolLabel(String symbolLabel) {
        this.symbolLabel = symbolLabel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isLivenessCheckEnabled() {
        return livenessCheckEnabled;
    }

    public void setLivenessCheckEnabled(boolean livenessCheckEnabled) {
        this.livenessCheckEnabled = livenessCheckEnabled;
    }

    public double getFaceMatchThreshold() {
        return faceMatchThreshold;
    }

    public void setFaceMatchThreshold(double faceMatchThreshold) {
        this.faceMatchThreshold = faceMatchThreshold;
    }

    public String getEmergencyKey() {
        return emergencyKey;
    }

    public void setEmergencyKey(String emergencyKey) {
        this.emergencyKey = emergencyKey;
    }

    // Schedule & Time Limit Getters and Helpers
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isTimeLimitEnabled() {
        return timeLimitEnabled;
    }

    public void setTimeLimitEnabled(boolean timeLimitEnabled) {
        this.timeLimitEnabled = timeLimitEnabled;
    }

    public boolean isTimeLimitActive() {
        return timeLimitEnabled && endTime != null;
    }

    public boolean isNotStartedYet() {
        if (!timeLimitEnabled || startTime == null) return false;
        return LocalDateTime.now().isBefore(startTime);
    }

    public boolean isExpired() {
        if (!timeLimitEnabled || endTime == null) return false;
        return LocalDateTime.now().isAfter(endTime);
    }

    public boolean isVotingOpen() {
        if ("PAUSED".equalsIgnoreCase(status) || "CONCLUDED".equalsIgnoreCase(status)) return false;
        if (isNotStartedYet()) return false;
        if (isExpired()) return false;
        return true;
    }

    public long getRemainingSeconds() {
        if (!isTimeLimitActive() || endTime == null) return 0;
        long secs = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        return Math.max(0, secs);
    }

    public String getFormattedStartTime() {
        if (startTime == null) return "Immediate";
        return startTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    public String getFormattedEndTime() {
        if (endTime == null) return "No Expiry";
        return endTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    public String getStartTimeHtmlInput() {
        if (startTime == null) return "";
        return startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    public String getEndTimeHtmlInput() {
        if (endTime == null) return "";
        return endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    public String getScheduleStatus() {
        if (!timeLimitEnabled) return "NO_LIMIT";
        if (isNotStartedYet()) return "NOT_STARTED";
        if (isExpired()) return "EXPIRED";
        return "LIVE";
    }

    public String getScheduleStatusBadgeClass() {
        String status = getScheduleStatus();
        if ("NOT_STARTED".equals(status)) return "bg-warning text-dark";
        if ("LIVE".equals(status)) return "bg-success text-white";
        if ("EXPIRED".equals(status)) return "bg-danger text-white";
        return "bg-secondary text-white";
    }

    public String getScheduleStatusLabel() {
        String status = getScheduleStatus();
        if ("NOT_STARTED".equals(status)) return "Scheduled (Starts: " + getFormattedStartTime() + ")";
        if ("LIVE".equals(status)) return "Live Polls (Closes: " + getFormattedEndTime() + ")";
        if ("EXPIRED".equals(status)) return "Voting Concluded (Ended: " + getFormattedEndTime() + ")";
        return "No Time Limit (Always Open)";
    }
}
