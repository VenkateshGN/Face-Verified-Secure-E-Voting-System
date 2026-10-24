package com.votesystem.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String firstname;

    @Column(nullable = false, length = 50)
    private String lastname;

    @Column(length = 150)
    private String photo;

    @Column(name = "role", length = 30)
    private String role = "ORG_ADMIN"; // SUPER_ADMIN or ORG_ADMIN

    @Column(name = "organization", length = 100)
    private String organization = "ALL"; // e.g. "Infosys", "CRCE College", "ALL"

    @Column(name = "organizer_title", length = 100)
    private String organizerTitle = "Election Organizer";

    @Column(name = "election_start_time")
    private LocalDateTime electionStartTime;

    @Column(name = "election_end_time")
    private LocalDateTime electionEndTime;

    @Column(name = "time_limit_enabled")
    private Boolean timeLimitEnabled = false;

    @Column(name = "created_on")
    private LocalDate createdOn = LocalDate.now();

    public Admin() {
        this.createdOn = LocalDate.now();
    }

    public Admin(String username, String password, String firstname, String lastname, String photo) {
        this.username = username;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.photo = photo;
        this.role = "SUPER_ADMIN";
        this.organization = "ALL";
        this.organizerTitle = "System Administrator";
        this.createdOn = LocalDate.now();
    }

    public Admin(String username, String password, String firstname, String lastname, String photo, String role, String organization, String organizerTitle) {
        this.username = username;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.photo = photo;
        this.role = role != null ? role : "ORG_ADMIN";
        this.organization = organization != null ? organization : "ALL";
        this.organizerTitle = organizerTitle != null ? organizerTitle : "Election Organizer";
        this.createdOn = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDate createdOn) {
        this.createdOn = createdOn;
    }

    public String getFullName() {
        return firstname + " " + lastname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganizerTitle() {
        return organizerTitle;
    }

    public void setOrganizerTitle(String organizerTitle) {
        this.organizerTitle = organizerTitle;
    }

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equalsIgnoreCase(role) || organization == null || "ALL".equalsIgnoreCase(organization.trim());
    }

    public String getRoleDisplayName() {
        if (isSuperAdmin()) return "Platform Super Administrator";
        return (organization != null && !organization.isBlank() ? organization : "") + " Election Organizer";
    }

    public String getRoleBadgeClass() {
        if (isSuperAdmin()) return "bg-dark text-white";
        return "bg-primary text-white";
    }

    // Schedule & Time Limit Getters and Helpers
    public LocalDateTime getElectionStartTime() {
        return electionStartTime;
    }

    public void setElectionStartTime(LocalDateTime electionStartTime) {
        this.electionStartTime = electionStartTime;
    }

    public LocalDateTime getElectionEndTime() {
        return electionEndTime;
    }

    public void setElectionEndTime(LocalDateTime electionEndTime) {
        this.electionEndTime = electionEndTime;
    }

    public Boolean getTimeLimitEnabled() {
        return timeLimitEnabled;
    }

    public void setTimeLimitEnabled(Boolean timeLimitEnabled) {
        this.timeLimitEnabled = timeLimitEnabled;
    }

    public boolean isTimeLimitActive() {
        return Boolean.TRUE.equals(timeLimitEnabled) && electionEndTime != null;
    }

    public boolean isNotStartedYet() {
        if (!Boolean.TRUE.equals(timeLimitEnabled) || electionStartTime == null) return false;
        return LocalDateTime.now().isBefore(electionStartTime);
    }

    public boolean isExpired() {
        if (!Boolean.TRUE.equals(timeLimitEnabled) || electionEndTime == null) return false;
        return LocalDateTime.now().isAfter(electionEndTime);
    }

    public boolean isVotingOpen() {
        if (isNotStartedYet()) return false;
        if (isExpired()) return false;
        return true;
    }

    public long getRemainingSeconds() {
        if (!isTimeLimitActive() || electionEndTime == null) return 0;
        long secs = Duration.between(LocalDateTime.now(), electionEndTime).getSeconds();
        return Math.max(0, secs);
    }

    public String getFormattedStartTime() {
        if (electionStartTime == null) return "Immediate";
        return electionStartTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    public String getFormattedEndTime() {
        if (electionEndTime == null) return "No Expiry";
        return electionEndTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    public String getStartTimeHtmlInput() {
        if (electionStartTime == null) return "";
        return electionStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    public String getEndTimeHtmlInput() {
        if (electionEndTime == null) return "";
        return electionEndTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    public String getScheduleStatus() {
        if (!Boolean.TRUE.equals(timeLimitEnabled)) return "NO_LIMIT";
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
