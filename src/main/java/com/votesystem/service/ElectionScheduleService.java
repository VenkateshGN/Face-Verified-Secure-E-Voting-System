package com.votesystem.service;

import com.votesystem.model.Admin;
import com.votesystem.model.ElectionSetting;
import com.votesystem.repository.AdminRepository;
import com.votesystem.repository.ElectionSettingRepository;
import com.votesystem.util.OrgMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ElectionScheduleService {

    private final AdminRepository adminRepo;
    private final ElectionSettingRepository electionRepo;

    public ElectionScheduleService(AdminRepository adminRepo, ElectionSettingRepository electionRepo) {
        this.adminRepo = adminRepo;
        this.electionRepo = electionRepo;
    }

    public ScheduleInfo getSchedule(String organization) {
        if (organization != null && !organization.isBlank() && !"ALL".equalsIgnoreCase(organization.trim())) {
            List<Admin> admins = adminRepo.findAll().stream()
                    .filter(a -> OrgMatcher.matches(organization, a.getOrganization()))
                    .toList();
            for (Admin admin : admins) {
                if (Boolean.TRUE.equals(admin.getTimeLimitEnabled()) || admin.getElectionEndTime() != null) {
                    return new ScheduleInfo(
                            admin.isTimeLimitActive(),
                            admin.isNotStartedYet(),
                            admin.isExpired(),
                            admin.isVotingOpen(),
                            admin.getRemainingSeconds(),
                            admin.getFormattedStartTime(),
                            admin.getFormattedEndTime(),
                            admin.getStartTimeHtmlInput(),
                            admin.getEndTimeHtmlInput(),
                            admin.getScheduleStatus(),
                            admin.getScheduleStatusBadgeClass(),
                            admin.getScheduleStatusLabel(),
                            Boolean.TRUE.equals(admin.getTimeLimitEnabled()),
                            organization.trim()
                    );
                }
            }
        }

        // Fallback to Global Election Setting
        ElectionSetting setting = electionRepo.findAll().stream().findFirst().orElseGet(() -> {
            ElectionSetting s = new ElectionSetting("Apex Leadership Council", "GENERAL", "Executive Leadership Election 2026", "ACTIVE");
            return electionRepo.save(s);
        });

        return new ScheduleInfo(
                setting.isTimeLimitActive(),
                setting.isNotStartedYet(),
                setting.isExpired(),
                setting.isVotingOpen(),
                setting.getRemainingSeconds(),
                setting.getFormattedStartTime(),
                setting.getFormattedEndTime(),
                setting.getStartTimeHtmlInput(),
                setting.getEndTimeHtmlInput(),
                setting.getScheduleStatus(),
                setting.getScheduleStatusBadgeClass(),
                setting.getScheduleStatusLabel(),
                setting.isTimeLimitEnabled(),
                "Global / " + setting.getOrganizationName()
        );
    }

    @Transactional
    public void updateScheduleForAdmin(Admin admin, LocalDateTime start, LocalDateTime end, boolean enabled) {
        if (admin.isSuperAdmin()) {
            ElectionSetting setting = electionRepo.findAll().stream().findFirst().orElseGet(() -> new ElectionSetting());
            setting.setStartTime(start);
            setting.setEndTime(end);
            setting.setTimeLimitEnabled(enabled);
            electionRepo.save(setting);
        } else {
            admin.setElectionStartTime(start);
            admin.setElectionEndTime(end);
            admin.setTimeLimitEnabled(enabled);
            adminRepo.save(admin);
        }
    }

    @Transactional
    public void updateScheduleForOrganization(String organization, LocalDateTime start, LocalDateTime end, boolean enabled) {
        if (organization == null || organization.isBlank() || "ALL".equalsIgnoreCase(organization.trim())) {
            ElectionSetting setting = electionRepo.findAll().stream().findFirst().orElseGet(ElectionSetting::new);
            setting.setStartTime(start);
            setting.setEndTime(end);
            setting.setTimeLimitEnabled(enabled);
            electionRepo.save(setting);
        } else {
            List<Admin> admins = adminRepo.findAll().stream()
                    .filter(a -> OrgMatcher.matches(organization, a.getOrganization()))
                    .toList();
            if (admins.isEmpty()) {
                Admin newAdmin = new Admin(
                        organization.trim().toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_") + "_admin",
                        "$2a$10$w85o3wZk2O9rZcE018gYI.U2eHn3kRzBqPZ3J8lQ/WJpB2mG9qN2.",
                        organization.trim(),
                        "Organizer",
                        "Screenshot.png",
                        "ORG_ADMIN",
                        organization.trim(),
                        organization.trim() + " Election Organizer"
                );
                newAdmin.setElectionStartTime(start);
                newAdmin.setElectionEndTime(end);
                newAdmin.setTimeLimitEnabled(enabled);
                adminRepo.save(newAdmin);
            } else {
                for (Admin a : admins) {
                    a.setElectionStartTime(start);
                    a.setElectionEndTime(end);
                    a.setTimeLimitEnabled(enabled);
                    adminRepo.save(a);
                }
            }
        }
    }

    public static class ScheduleInfo {
        private final boolean timeLimitActive;
        private final boolean notStarted;
        private final boolean expired;
        private final boolean votingOpen;
        private final long remainingSeconds;
        private final String formattedStartTime;
        private final String formattedEndTime;
        private final String startTimeHtml;
        private final String endTimeHtml;
        private final String scheduleStatus;
        private final String scheduleStatusBadgeClass;
        private final String scheduleStatusLabel;
        private final boolean timeLimitEnabled;
        private final String scopeName;

        public ScheduleInfo(boolean timeLimitActive, boolean notStarted, boolean expired, boolean votingOpen,
                            long remainingSeconds, String formattedStartTime, String formattedEndTime,
                            String startTimeHtml, String endTimeHtml, String scheduleStatus,
                            String scheduleStatusBadgeClass, String scheduleStatusLabel,
                            boolean timeLimitEnabled, String scopeName) {
            this.timeLimitActive = timeLimitActive;
            this.notStarted = notStarted;
            this.expired = expired;
            this.votingOpen = votingOpen;
            this.remainingSeconds = remainingSeconds;
            this.formattedStartTime = formattedStartTime;
            this.formattedEndTime = formattedEndTime;
            this.startTimeHtml = startTimeHtml;
            this.endTimeHtml = endTimeHtml;
            this.scheduleStatus = scheduleStatus;
            this.scheduleStatusBadgeClass = scheduleStatusBadgeClass;
            this.scheduleStatusLabel = scheduleStatusLabel;
            this.timeLimitEnabled = timeLimitEnabled;
            this.scopeName = scopeName;
        }

        public boolean isTimeLimitActive() { return timeLimitActive; }
        public boolean isNotStarted() { return notStarted; }
        public boolean isExpired() { return expired; }
        public boolean isVotingOpen() { return votingOpen; }
        public long getRemainingSeconds() { return remainingSeconds; }
        public String getFormattedStartTime() { return formattedStartTime; }
        public String getFormattedEndTime() { return formattedEndTime; }
        public String getStartTimeHtml() { return startTimeHtml; }
        public String getEndTimeHtml() { return endTimeHtml; }
        public String getScheduleStatus() { return scheduleStatus; }
        public String getScheduleStatusBadgeClass() { return scheduleStatusBadgeClass; }
        public String getScheduleStatusLabel() { return scheduleStatusLabel; }
        public boolean isTimeLimitEnabled() { return timeLimitEnabled; }
        public String getScopeName() { return scopeName; }
    }
}
