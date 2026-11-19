package com.votesystem.dto;

import java.util.ArrayList;
import java.util.List;

public class AiExecutionResult {

    private boolean success;
    private String message;
    private String summary;
    private String organization;
    private boolean scheduleUpdated = false;
    private String formattedStartTime;
    private String formattedEndTime;
    private int positionsCreated = 0;
    private List<String> createdPositions = new ArrayList<>();
    private int candidatesCreated = 0;
    private List<String> createdCandidates = new ArrayList<>();
    private int votersCreated = 0;
    private List<CreatedVoterInfo> createdVoters = new ArrayList<>();
    private int organizersCreated = 0;
    private List<String> createdOrganizers = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public AiExecutionResult() {}

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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

    public boolean isScheduleUpdated() {
        return scheduleUpdated;
    }

    public void setScheduleUpdated(boolean scheduleUpdated) {
        this.scheduleUpdated = scheduleUpdated;
    }

    public String getFormattedStartTime() {
        return formattedStartTime;
    }

    public void setFormattedStartTime(String formattedStartTime) {
        this.formattedStartTime = formattedStartTime;
    }

    public String getFormattedEndTime() {
        return formattedEndTime;
    }

    public void setFormattedEndTime(String formattedEndTime) {
        this.formattedEndTime = formattedEndTime;
    }

    public int getPositionsCreated() {
        return positionsCreated;
    }

    public void setPositionsCreated(int positionsCreated) {
        this.positionsCreated = positionsCreated;
    }

    public List<String> getCreatedPositions() {
        return createdPositions != null ? createdPositions : new ArrayList<>();
    }

    public void setCreatedPositions(List<String> createdPositions) {
        this.createdPositions = createdPositions;
    }

    public int getCandidatesCreated() {
        return candidatesCreated;
    }

    public void setCandidatesCreated(int candidatesCreated) {
        this.candidatesCreated = candidatesCreated;
    }

    public List<String> getCreatedCandidates() {
        return createdCandidates != null ? createdCandidates : new ArrayList<>();
    }

    public void setCreatedCandidates(List<String> createdCandidates) {
        this.createdCandidates = createdCandidates;
    }

    public int getVotersCreated() {
        return votersCreated;
    }

    public void setVotersCreated(int votersCreated) {
        this.votersCreated = votersCreated;
    }

    public List<CreatedVoterInfo> getCreatedVoters() {
        return createdVoters != null ? createdVoters : new ArrayList<>();
    }

    public void setCreatedVoters(List<CreatedVoterInfo> createdVoters) {
        this.createdVoters = createdVoters;
    }

    public int getOrganizersCreated() {
        return organizersCreated;
    }

    public void setOrganizersCreated(int organizersCreated) {
        this.organizersCreated = organizersCreated;
    }

    public List<String> getCreatedOrganizers() {
        return createdOrganizers != null ? createdOrganizers : new ArrayList<>();
    }

    public void setCreatedOrganizers(List<String> createdOrganizers) {
        this.createdOrganizers = createdOrganizers;
    }

    public List<String> getWarnings() {
        return warnings != null ? warnings : new ArrayList<>();
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public static class CreatedVoterInfo {
        private String voterId;
        private String name;
        private String email;
        private String department;
        private String organization;
        private String initialPassword;

        public CreatedVoterInfo() {}

        public CreatedVoterInfo(String voterId, String name, String email, String department, String organization, String initialPassword) {
            this.voterId = voterId;
            this.name = name;
            this.email = email;
            this.department = department;
            this.organization = organization;
            this.initialPassword = initialPassword;
        }

        public String getVoterId() {
            return voterId;
        }

        public void setVoterId(String voterId) {
            this.voterId = voterId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDepartment() {
            return department;
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

        public String getInitialPassword() {
            return initialPassword;
        }

        public void setInitialPassword(String initialPassword) {
            this.initialPassword = initialPassword;
        }
    }
}
