package com.votesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voters")
public class Voter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voters_id", nullable = false, unique = true, length = 50)
    private String voterId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String firstname;

    @Column(nullable = false, length = 50)
    private String lastname;

    @Column(length = 255)
    private String photo;

    @Column(length = 100)
    private String email;

    @Column(length = 10)
    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "has_voted", nullable = false)
    private boolean hasVoted = false;

    @Column(name = "voted_at")
    private LocalDateTime votedAt;

    @Column(name = "tracking_token", length = 100)
    private String trackingToken;

    @Column(name = "voter_category", length = 30)
    private String voterCategory = "STUDENT"; // STUDENT, OFFICE_STAFF, EMPLOYEE, FACULTY, GENERAL

    @Column(name = "department", length = 100)
    private String department = "General"; // Field of Study / Branch

    @Column(name = "organization", length = 100)
    private String organization = "CRCE College"; // Company or organization (e.g., Infosys, Wipro, Apex, CRCE)

    public Voter() {}

    public Voter(String voterId, String password, String firstname, String lastname, String photo, String email) {
        this.voterId = voterId;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.photo = photo;
        this.email = email;
        this.hasVoted = false;
        this.voterCategory = "STUDENT";
        this.department = "General";
        this.organization = "CRCE College";
    }

    public Voter(String voterId, String password, String firstname, String lastname, String photo, String email, String voterCategory, String department) {
        this.voterId = voterId;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.photo = photo;
        this.email = email;
        this.hasVoted = false;
        this.voterCategory = voterCategory != null ? voterCategory : "STUDENT";
        this.department = department != null ? department : "General";
        this.organization = "CRCE College";
    }

    public Voter(String voterId, String password, String firstname, String lastname, String photo, String email, String voterCategory, String department, String organization) {
        this.voterId = voterId;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.photo = photo;
        this.email = email;
        this.hasVoted = false;
        this.voterCategory = voterCategory != null ? voterCategory : "STUDENT";
        this.department = department != null ? department : "General";
        this.organization = organization != null && !organization.isBlank() ? organization : "CRCE College";
    }

    // Dynamic Display Helpers for Voter Electorate Field
    public String getVoterCategoryDisplayName() {
        if ("STUDENT".equalsIgnoreCase(voterCategory)) return "Student Electorate";
        if ("OFFICE_STAFF".equalsIgnoreCase(voterCategory) || "EMPLOYEE".equalsIgnoreCase(voterCategory)) {
            return (organization != null && !organization.isBlank()) ? organization.trim() + " Employee" : "Corporate Employee";
        }
        if ("FACULTY".equalsIgnoreCase(voterCategory)) return "Faculty / Academic Staff";
        return "General Member Electorate";
    }

    public String getOrganizationDisplayName() {
        return organization != null && !organization.isBlank() ? organization.trim() : "General";
    }

    public String getOrganizationBadgeClass() {
        if ("Infosys".equalsIgnoreCase(organization)) return "bg-primary text-white";
        return "bg-secondary text-white";
    }

    public String getVoterCategoryBadgeClass() {
        if ("STUDENT".equalsIgnoreCase(voterCategory)) return "bg-primary text-white";
        if ("OFFICE_STAFF".equalsIgnoreCase(voterCategory) || "EMPLOYEE".equalsIgnoreCase(voterCategory)) return "bg-dark text-white";
        if ("FACULTY".equalsIgnoreCase(voterCategory)) return "bg-info text-dark";
        return "bg-success text-white";
    }

    public String getVoterCategoryIcon() {
        if ("STUDENT".equalsIgnoreCase(voterCategory)) return "fa-solid fa-graduation-cap";
        if ("OFFICE_STAFF".equalsIgnoreCase(voterCategory) || "EMPLOYEE".equalsIgnoreCase(voterCategory)) return "fa-solid fa-briefcase";
        if ("FACULTY".equalsIgnoreCase(voterCategory)) return "fa-solid fa-chalkboard-user";
        return "fa-solid fa-user-check";
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(LocalDateTime otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(LocalDateTime votedAt) {
        this.votedAt = votedAt;
    }

    public String getTrackingToken() {
        return trackingToken;
    }

    public void setTrackingToken(String trackingToken) {
        this.trackingToken = trackingToken;
    }

    public String getVoterCategory() {
        return voterCategory != null ? voterCategory : "STUDENT";
    }

    public void setVoterCategory(String voterCategory) {
        this.voterCategory = voterCategory;
    }

    public String getDepartment() {
        return department != null ? department : "General";
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getOrganization() {
        return organization != null && !organization.isBlank() ? organization : "Infosys";
    }

    public void setOrganization(String organization) {
        this.organization = organization != null && !organization.isBlank() ? organization : "Infosys";
    }

    public String getFullName() {
        return firstname + " " + lastname;
    }
}
