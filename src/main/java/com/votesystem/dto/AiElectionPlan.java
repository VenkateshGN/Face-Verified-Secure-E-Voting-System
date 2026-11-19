package com.votesystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiElectionPlan {

    private String summary;
    private String organization;
    private SchedulePlan schedule;
    private List<PositionPlan> positions = new ArrayList<>();
    private List<CandidatePlan> candidates = new ArrayList<>();
    private List<VoterPlan> voters = new ArrayList<>();
    private List<OrganizerPlan> organizers = new ArrayList<>();

    public AiElectionPlan() {}

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public SchedulePlan getSchedule() {
        return schedule;
    }

    public void setSchedule(SchedulePlan schedule) {
        this.schedule = schedule;
    }

    public List<PositionPlan> getPositions() {
        return positions != null ? positions : new ArrayList<>();
    }

    public void setPositions(List<PositionPlan> positions) {
        this.positions = positions;
    }

    public List<CandidatePlan> getCandidates() {
        return candidates != null ? candidates : new ArrayList<>();
    }

    public void setCandidates(List<CandidatePlan> candidates) {
        this.candidates = candidates;
    }

    public List<VoterPlan> getVoters() {
        return voters != null ? voters : new ArrayList<>();
    }

    public void setVoters(List<VoterPlan> voters) {
        this.voters = voters;
    }

    public List<OrganizerPlan> getOrganizers() {
        return organizers != null ? organizers : new ArrayList<>();
    }

    public void setOrganizers(List<OrganizerPlan> organizers) {
        this.organizers = organizers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchedulePlan {
        private Boolean enabled = false;
        private String startTime;
        private String endTime;
        private Integer durationHours;

        public SchedulePlan() {}

        public Boolean getEnabled() {
            return enabled != null ? enabled : false;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public Integer getDurationHours() {
            return durationHours;
        }

        public void setDurationHours(Integer durationHours) {
            this.durationHours = durationHours;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PositionPlan {
        private String description;
        private Integer maxVote = 1;
        private Integer priority = 1;
        private String electorateScope = "ALL";
        private String targetDepartment = "ALL";
        private String targetOrganization;

        public PositionPlan() {}

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getMaxVote() {
            return maxVote != null ? maxVote : 1;
        }

        public void setMaxVote(Integer maxVote) {
            this.maxVote = maxVote;
        }

        public Integer getPriority() {
            return priority != null ? priority : 1;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
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
            return targetOrganization;
        }

        public void setTargetOrganization(String targetOrganization) {
            this.targetOrganization = targetOrganization;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CandidatePlan {
        private String firstname;
        private String lastname;
        private String positionName;
        private String platform;
        private String organization;

        public CandidatePlan() {}

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

        public String getPositionName() {
            return positionName;
        }

        public void setPositionName(String positionName) {
            this.positionName = positionName;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VoterPlan {
        private String firstname;
        private String lastname;
        private String email;
        private String voterCategory = "STUDENT";
        private String department = "General";
        private String organization;
        private Integer generateCount = 1;

        public VoterPlan() {}

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
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
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public Integer getGenerateCount() {
            return generateCount != null && generateCount > 0 ? generateCount : 1;
        }

        public void setGenerateCount(Integer generateCount) {
            this.generateCount = generateCount;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrganizerPlan {
        private String username;
        private String firstname;
        private String lastname;
        private String organization;
        private String designation = "Election Organizer";

        public OrganizerPlan() {}

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
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

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public String getDesignation() {
            return designation != null ? designation : "Election Organizer";
        }

        public void setDesignation(String designation) {
            this.designation = designation;
        }
    }
}
