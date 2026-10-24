package com.votesystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @Column(nullable = false, length = 50)
    private String firstname;

    @Column(nullable = false, length = 50)
    private String lastname;

    @Column(length = 255)
    private String photo;

    @Column(columnDefinition = "TEXT")
    private String platform;

    @Column(length = 255)
    private String symbol;

    @Column(name = "organization", length = 100)
    private String organization = "Infosys";

    public Candidate() {}

    public Candidate(Position position, String firstname, String lastname, String photo, String platform, String symbol) {
        this.position = position;
        this.firstname = firstname;
        this.lastname = lastname;
        this.photo = photo;
        this.platform = platform;
        this.symbol = symbol;
        this.organization = "Infosys";
    }

    public Candidate(Position position, String firstname, String lastname, String photo, String platform, String symbol, String organization) {
        this.position = position;
        this.firstname = firstname;
        this.lastname = lastname;
        this.photo = photo;
        this.platform = platform;
        this.symbol = symbol;
        this.organization = organization != null && !organization.isBlank() ? organization : "Infosys";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getOrganization() {
        return organization != null && !organization.isBlank() ? organization : "Infosys";
    }

    public void setOrganization(String organization) {
        this.organization = organization != null && !organization.isBlank() ? organization : "Infosys";
    }

    public String getOrganizationBadgeClass() {
        if ("Infosys".equalsIgnoreCase(organization)) return "bg-primary text-white";
        return "bg-secondary text-white";
    }

    public String getFullName() {
        return firstname + " " + lastname;
    }
}
