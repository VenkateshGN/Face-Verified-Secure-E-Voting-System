package com.votesystem.config;

import com.votesystem.model.*;
import com.votesystem.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminRepository adminRepo;
    private final PositionRepository positionRepo;
    private final CandidateRepository candidateRepo;
    private final VoterRepository voterRepo;
    private final ElectionSettingRepository electionRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AdminRepository adminRepo, PositionRepository positionRepo,
                           CandidateRepository candidateRepo, VoterRepository voterRepo,
                           ElectionSettingRepository electionRepo, PasswordEncoder passwordEncoder) {
        this.adminRepo = adminRepo;
        this.positionRepo = positionRepo;
        this.candidateRepo = candidateRepo;
        this.voterRepo = voterRepo;
        this.electionRepo = electionRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking database initialization...");

        // 1. Election Setting
        if (electionRepo.count() == 0) {
            ElectionSetting setting = new ElectionSetting("Apex Multi-Org Leadership Council", "GENERAL", "Annual Leadership Election 2026", "ACTIVE");
            setting.setElectionSubtitle("Universal leader election platform for schools, companies, and organizations.");
            setting.setVoterIdLabel("Voter / Student / Employee ID");
            setting.setSymbolLabel("Emblem / House Badge / Logo");
            setting.setEmergencyKey("VOTE2026");
            electionRepo.save(setting);
            log.info("Default multi-org election settings created.");
        } else {
            ElectionSetting s = electionRepo.findAll().get(0);
            boolean changed = false;
            if (s.getOrganizationName() == null || s.getOrganizationName().isBlank()) {
                s.setOrganizationName("Apex Multi-Org Leadership Council");
                changed = true;
            }
            if (s.getOrganizationType() == null || s.getOrganizationType().isBlank()) {
                s.setOrganizationType("GENERAL");
                changed = true;
            }
            if (s.getElectionSubtitle() == null) {
                s.setElectionSubtitle("Universal leader election platform for schools, companies, and organizations.");
                changed = true;
            }
            if (s.getVoterIdLabel() == null) {
                s.setVoterIdLabel("Voter / Student / Employee ID");
                changed = true;
            }
            if (s.getSymbolLabel() == null) {
                s.setSymbolLabel("Emblem / House Badge / Logo");
                changed = true;
            }
            if (changed) {
                electionRepo.save(s);
            }
        }

        // 2. Admin Users (Super Admin and Dedicated Organization Admins)
        if (!adminRepo.existsByUsername("Venkatesh")) {
            Admin admin = new Admin();
            admin.setUsername("Venkatesh");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setFirstname("Platform");
            admin.setLastname("Super Admin");
            admin.setPhoto("admin.jpg");
            admin.setRole("SUPER_ADMIN");
            admin.setOrganization("ALL");
            admin.setOrganizerTitle("System Super Administrator");
            admin.setCreatedOn(LocalDate.now());
            adminRepo.save(admin);
            log.info("Default super admin created: Username: Venkatesh | Password: password");
        }

        // Dedicated Infosys Organizer Admin is registered by Super Admin via /admin/organizers

        // Migrate any existing admins
        for (Admin a : adminRepo.findAll()) {
            boolean updated = false;
            if (a.getRole() == null || a.getRole().isBlank()) {
                if ("Venkatesh".equalsIgnoreCase(a.getUsername())) {
                    a.setRole("SUPER_ADMIN");
                    a.setOrganization("ALL");
                    a.setOrganizerTitle("System Super Administrator");
                } else {
                    a.setRole("ORG_ADMIN");
                    if (a.getOrganization() == null) a.setOrganization("Infosys");
                }
                updated = true;
            }
            if (updated) {
                adminRepo.save(a);
            }
        }

        // 3. Positions
        Position mla;
        Position mp;
        if (positionRepo.count() == 0) {
            mla = new Position("Student Council President", 1, 1, "STUDENT");
            mp = new Position("Sports & Cultural Secretary", 1, 2, "STUDENT");
            Position officePos = new Position("Executive Staff & Office Advisory Council", 1, 3, "OFFICE_ORGANIZATION");
            positionRepo.save(mla);
            positionRepo.save(mp);
            positionRepo.save(officePos);
            log.info("Default student and office positions created with scoped access.");
        } else {
            List<Position> posList = positionRepo.findAllByOrderByPriorityAsc();
            mla = posList.get(0);
            mp = posList.size() > 1 ? posList.get(1) : posList.get(0);

            // Migration check: ensure all existing positions have appropriate electorateScope
            boolean posChanged = false;
            for (Position p : posList) {
                if (p.getElectorateScope() == null || p.getElectorateScope().isBlank()) {
                    String d = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                    if (d.contains("student") || d.contains("head") || d.contains("captain") || d.contains("cultural") || d.contains("class") || d.contains("house")) {
                        p.setElectorateScope("STUDENT");
                    } else if (d.contains("board") || d.contains("employee") || d.contains("welfare") || d.contains("safety") || d.contains("office") || d.contains("trustee")) {
                        p.setElectorateScope("OFFICE_ORGANIZATION");
                    } else {
                        p.setElectorateScope("ALL");
                    }
                    positionRepo.save(p);
                    posChanged = true;
                }
            }
            if (posChanged) log.info("Migrated existing position scopes.");
        }

        // 4. Candidates and Voters are enrolled cleanly via Admin portal
        List<Voter> allVoters = voterRepo.findAll();
        for (Voter v : allVoters) {
                boolean updated = false;
                if (v.getVoterCategory() == null || v.getVoterCategory().isBlank()) {
                    v.setVoterCategory("STUDENT");
                    updated = true;
                }
                // Students and college voters MUST belong to CRCE College, NOT Infosys!
                if ("STUDENT".equalsIgnoreCase(v.getVoterCategory()) || 
                    (v.getVoterId() != null && (v.getVoterId().startsWith("STU_") || v.getVoterId().startsWith("STUDENT_") || v.getVoterId().equals("HrEn9AbXxtpo6jf")))) {
                    if (!"CRCE College".equalsIgnoreCase(v.getOrganization())) {
                        v.setOrganization("CRCE College");
                        updated = true;
                    }
                    if (!"STUDENT".equalsIgnoreCase(v.getVoterCategory())) {
                        v.setVoterCategory("STUDENT");
                        updated = true;
                    }
                }
                // Only actual Infosys employee voters belong to Infosys!
                if (v.getVoterId() != null && v.getVoterId().startsWith("INFY_")) {
                    if (!"Infosys".equalsIgnoreCase(v.getOrganization())) {
                        v.setOrganization("Infosys");
                        updated = true;
                    }
                    if (!"EMPLOYEE".equalsIgnoreCase(v.getVoterCategory())) {
                        v.setVoterCategory("EMPLOYEE");
                        updated = true;
                    }
                }
                if (v.getOrganization() == null || v.getOrganization().isBlank()) {
                    if ("OFFICE_STAFF".equalsIgnoreCase(v.getVoterCategory()) || "EMPLOYEE".equalsIgnoreCase(v.getVoterCategory())) {
                        v.setOrganization("Infosys");
                    } else {
                        v.setOrganization("CRCE College");
                    }
                    updated = true;
                }
                if (updated) {
                    voterRepo.save(v);
                }
            }

        // 6. Ensure Cultural Event Organizer position exists for Infosys
        boolean hasCulturalOrg = positionRepo.findAll().stream()
                .anyMatch(p -> p.getDescription() != null && p.getDescription().toLowerCase().contains("cultural event organizer"));
        if (!hasCulturalOrg) {
            Position culturalPos = new Position("Cultural Event Organizer", 1, 10, "OFFICE_ORGANIZATION", "ALL", "Infosys");
            positionRepo.save(culturalPos);

        }

        // 8. Migrate and partition all positions strictly by target organization
        for (Position p : positionRepo.findAll()) {
            String d = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
            if (d.contains("cultural event organizer") || d.contains("employee welfare") || d.contains("sports coordinator") || d.contains("infosys")) {
                p.setTargetOrganization("Infosys");
                p.setElectorateScope("OFFICE_ORGANIZATION");
            } else if (d.contains("student") || d.contains("president") || d.contains("captain") || d.contains("kho kho") || d.contains("secretary") || d.contains("representative") || d.contains("crce")) {
                p.setTargetOrganization("CRCE College");
                p.setElectorateScope("STUDENT");
            } else if (d.contains("mla") || d.contains("mp")) {
                p.setTargetOrganization("General Public");
                p.setElectorateScope("ALL");
            } else if (p.getTargetOrganization() == null || p.getTargetOrganization().isBlank() || "ALL".equalsIgnoreCase(p.getTargetOrganization())) {
                p.setTargetOrganization("CRCE College");
            }
            positionRepo.save(p);
        }

        // 9. Migrate candidate organization to strictly match their position target organization
        for (Candidate c : candidateRepo.findAll()) {
            if (c.getPosition() != null && c.getPosition().getTargetOrganization() != null) {
                c.setOrganization(c.getPosition().getTargetOrganization());
            } else if (c.getOrganization() == null || c.getOrganization().isBlank()) {
                c.setOrganization("Infosys");
            }
            candidateRepo.save(c);
        }

        log.info("System initialization complete.");
    }
}
