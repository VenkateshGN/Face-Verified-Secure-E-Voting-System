package com.votesystem;

import com.votesystem.model.Admin;
import com.votesystem.model.Candidate;
import com.votesystem.model.Position;
import com.votesystem.model.Vote;
import com.votesystem.model.Voter;
import com.votesystem.repository.CandidateRepository;
import com.votesystem.repository.PositionRepository;
import com.votesystem.repository.VoteRepository;
import com.votesystem.repository.VoterRepository;
import com.votesystem.service.BlockchainService;
import com.votesystem.service.CryptoService;
import com.votesystem.service.VoteIntegrityService;
import com.votesystem.service.VoteIntegrityService.VoteAuditReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VoteSystemApplicationTests {

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private VoteIntegrityService integrityService;

    @Autowired
    private VoterRepository voterRepo;

    @Autowired
    private PositionRepository positionRepo;

    @Autowired
    private CandidateRepository candidateRepo;

    @Autowired
    private VoteRepository voteRepo;

    @Autowired
    private com.votesystem.repository.AdminRepository adminRepo;

    @Autowired
    private com.votesystem.controller.AdminController adminController;

    @Autowired
    private com.votesystem.controller.BallotController ballotController;

    @Autowired
    private com.votesystem.controller.VoterAuthController voterAuthController;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.votesystem.service.ElectionScheduleService scheduleService;

    @Autowired
    private com.votesystem.repository.ElectionSettingRepository electionRepo;

    @Autowired
    private com.votesystem.controller.PublicAuditController publicAuditController;

    @Test
    void contextLoads() {
        assertNotNull(cryptoService);
        assertNotNull(blockchainService);
        assertNotNull(integrityService);
        assertNotNull(ballotController);
    }

    @Test
    void testBlockchainContinuity() {
        BlockchainService.BlockchainValidationResult result = blockchainService.validateBlockchain();
        assertTrue(result.isValid(), "Initial blockchain chain should be valid");
        assertTrue(result.getTotalBlocks() >= 1, "Genesis block must exist");
    }

    @Test
    void testVoteCastingAndCryptographicTamperDetection() {
        Voter voter = new Voter("TEST_" + System.currentTimeMillis(), "password", "Test", "Voter", "Screenshot.png", "test@test.com");
        voter = voterRepo.save(voter);

        Position pos = positionRepo.findAll().stream().findFirst()
                .orElseGet(() -> positionRepo.save(new Position("Test President", 1, 1, "ALL")));
        Candidate cand1 = candidateRepo.findByPositionId(pos.getId()).stream().findFirst()
                .orElseGet(() -> candidateRepo.save(new Candidate(pos, "Candidate1", "Test", "photo.jpg", "Bio", "sym.png")));

        // Encrypt and sign vote
        String encrypted = cryptoService.encryptVote(cand1.getId());
        String sig = cryptoService.signVote(encrypted);
        String voteHash = cryptoService.calculateVoteHash(voter.getId(), cand1.getId(), pos.getId(), encrypted, sig);

        Vote vote = new Vote(voter, cand1, pos, encrypted, sig, voteHash);
        vote = voteRepo.save(vote);

        // Verify vote integrity
        VoteAuditReport initialReport = integrityService.auditAllVotes("127.0.0.1", "JUnit Test");
        assertTrue(initialReport.getValidVotes() >= 1);

        // Create second candidate for tamper simulation
        Candidate cand2 = candidateRepo.save(new Candidate(pos, "Candidate2", "Test", "photo.jpg", "Bio", "sym.png"));

        // Simulate Tamper: change DB candidate without altering encrypted payload or signature
        boolean tampered = integrityService.simulateTamper(vote.getId(), cand2.getId());
        assertTrue(tampered);

        VoteAuditReport postTamperReport = integrityService.auditAllVotes("127.0.0.1", "JUnit Test");
        assertTrue(postTamperReport.getTamperedVotes() >= 1, "Audit system must identify tampered vote");

        // Clean up
        voteRepo.delete(vote);
        candidateRepo.delete(cand2);
        candidateRepo.delete(cand1);
        voterRepo.delete(voter);
    }

    @Test
    void testStudentElectorateScopeIsolation() {
        Voter student = new Voter("STU_" + System.currentTimeMillis(), "password", "Alex", "Student", "Screenshot.png", "student@school.edu");
        student.setVoterCategory("STUDENT");
        student.setOrganization("CRCE College");
        student = voterRepo.save(student);

        Voter officeStaff = new Voter("STF_" + System.currentTimeMillis(), "password", "Sarah", "Manager", "admin.jpg", "staff@company.com");
        officeStaff.setVoterCategory("OFFICE_STAFF");
        officeStaff.setOrganization("CRCE College");
        officeStaff = voterRepo.save(officeStaff);

        Position studentPos = new Position("Student Council President", 1, 10, "STUDENT", "ALL", "CRCE College");
        studentPos = positionRepo.save(studentPos);

        Position officePos = new Position("Board of Directors Office Event", 1, 11, "OFFICE_ORGANIZATION", "ALL", "CRCE College");
        officePos = positionRepo.save(officePos);

        Position universalPos = new Position("Campus Health Committee", 1, 12, "ALL", "ALL", "CRCE College");
        universalPos = positionRepo.save(universalPos);

        // 1. Verify Student Voter Eligibility
        assertTrue(ballotController.isPositionEligibleForVoter(studentPos, student), "Students MUST be allowed to vote on Student positions");
        assertFalse(ballotController.isPositionEligibleForVoter(officePos, student), "Students MUST NOT be allowed to vote on Organization Office Events");
        assertTrue(ballotController.isPositionEligibleForVoter(universalPos, student), "Students can vote on ALL positions");

        // 2. Verify Office Staff Voter Eligibility
        assertTrue(ballotController.isPositionEligibleForVoter(officePos, officeStaff), "Office Staff MUST be allowed to vote on Office Events");
        assertFalse(ballotController.isPositionEligibleForVoter(studentPos, officeStaff), "Office Staff MUST NOT interfere in Student positions");
        assertTrue(ballotController.isPositionEligibleForVoter(universalPos, officeStaff), "Office Staff can vote on ALL positions");

        // Clean up
        positionRepo.delete(studentPos);
        positionRepo.delete(officePos);
        positionRepo.delete(universalPos);
        voterRepo.delete(student);
        voterRepo.delete(officeStaff);
    }

    @Test
    void testInfosysCompanyTenantIsolation() {
        // Infosys employee voter
        Voter infyEmployee = new Voter("INFY_TEST_1", "password", "Rohan", "Mehta", "Screenshot.png", "rohan@infosys.com", "EMPLOYEE", "Digital Tech", "Infosys");
        infyEmployee = voterRepo.save(infyEmployee);

        // Outside non-Infosys voter (e.g. Wipro or Student)
        Voter wiproEmployee = new Voter("WIPRO_TEST_1", "password", "Kavita", "Shah", "Screenshot.png", "kavita@wipro.com", "EMPLOYEE", "Cloud Solutions", "Wipro");
        wiproEmployee = voterRepo.save(wiproEmployee);

        Voter collegeStudent = new Voter("STU_TEST_1", "password", "Deepak", "Kumar", "Screenshot.png", "deepak@crce.edu", "STUDENT", "Computer Science", "CRCE College");
        collegeStudent = voterRepo.save(collegeStudent);

        // Position designated specifically for Infosys Cultural Event Organizer
        Position culturalOrganizer = new Position("Cultural Event Organizer", 1, 20, "OFFICE_ORGANIZATION", "ALL", "Infosys");
        culturalOrganizer = positionRepo.save(culturalOrganizer);

        // Position designated specifically for CRCE College Student Council President
        Position studentPresident = new Position("Student Council President", 1, 21, "STUDENT", "ALL", "CRCE College");
        studentPresident = positionRepo.save(studentPresident);

        // 1. Infosys Employee MUST be authorized to vote for Infosys position
        assertTrue(ballotController.isPositionEligibleForVoter(culturalOrganizer, infyEmployee), 
                "Infosys employee MUST be authorized to vote for Infosys Cultural Event Organizer");

        // 2. Infosys Employee MUST NOT be allowed to view or vote on CRCE College / Student positions
        assertFalse(ballotController.isPositionEligibleForVoter(studentPresident, infyEmployee),
                "Infosys employee MUST be strictly barred from other positions (e.g. Student Council President)");

        // 3. Non-Infosys Employee (Wipro) MUST be blocked from Infosys Cultural Event Organizer
        assertFalse(ballotController.isPositionEligibleForVoter(culturalOrganizer, wiproEmployee), 
                "Wipro employee MUST be blocked from voting for Infosys Cultural Event Organizer");

        // 4. Student (CRCE College) MUST be strictly blocked from Infosys Cultural Event Organizer
        assertFalse(ballotController.isPositionEligibleForVoter(culturalOrganizer, collegeStudent), 
                "Student MUST be strictly blocked from voting for Infosys Cultural Event Organizer");

        // 5. Student (CRCE College) MUST be authorized for CRCE College position
        assertTrue(ballotController.isPositionEligibleForVoter(studentPresident, collegeStudent),
                "CRCE College student MUST be authorized for CRCE College Student Council President");

        // Clean up
        positionRepo.delete(culturalOrganizer);
        positionRepo.delete(studentPresident);
        voterRepo.delete(infyEmployee);
        voterRepo.delete(wiproEmployee);
        voterRepo.delete(collegeStudent);
    }

    @Test
    void testInfosysVoterListExcludesOtherOrganizations() {
        // Mock session for infosys_admin
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        com.votesystem.model.Admin infyAdmin = adminRepo.findByUsername("infosys_admin").orElseGet(() -> {
            com.votesystem.model.Admin a = new com.votesystem.model.Admin("infosys_admin", "pwd", "Ananya", "Rao", "admin.jpg", "ORG_ADMIN", "Infosys", "Organizer");
            return adminRepo.save(a);
        });
        session.setAttribute("adminId", infyAdmin.getId());
        session.setAttribute("adminOrg", "Infosys");
        session.setAttribute("adminRole", "ORG_ADMIN");

        Voter testInfyVoter = voterRepo.save(new Voter("INFY_UNIT_TEST", "password", "Infy", "User", "Screenshot.png", "infy@test.com", "EMPLOYEE", "IT", "Infosys"));
        Voter otherVoter = voterRepo.save(new Voter("CRCE_UNIT_TEST", "password", "CRCE", "User", "Screenshot.png", "crce@test.com", "STUDENT", "CS", "CRCE College"));

        try {
            org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
            adminController.listVoters(null, model, session);

            @SuppressWarnings("unchecked")
            java.util.List<Voter> voters = (java.util.List<Voter>) model.get("voters");
            assertNotNull(voters);
            assertFalse(voters.isEmpty(), "Infosys must have employee voters");
            for (Voter v : voters) {
                assertEquals("Infosys", v.getOrganization(), "Infosys voters list must NOT display voters from other organizations!");
                assertNotEquals("CRCE College", v.getOrganization());
                assertNotEquals("STUDENT", v.getVoterCategory(), "Infosys voters list must not include student voters from colleges!");
            }
        } finally {
            voterRepo.delete(testInfyVoter);
            voterRepo.delete(otherVoter);
            adminRepo.delete(infyAdmin);
        }
    }

    @Test
    void testBallotRecordsTenantScoping() {
        Voter infyVoter = voterRepo.save(new Voter("INFY_TEMP_" + System.currentTimeMillis(), "password", "Test", "Employee", "Screenshot.png", "t@infy.com", "EMPLOYEE", "General", "Infosys"));

        Position infyPos = positionRepo.findAll().stream().filter(p -> "Infosys".equalsIgnoreCase(p.getTargetOrganization())).findFirst()
                .orElseGet(() -> positionRepo.save(new Position("Cultural Event Organizer", 1, 99, "OFFICE_ORGANIZATION", "ALL", "Infosys")));

        Candidate infyCand = candidateRepo.findByPositionId(infyPos.getId()).stream().findFirst()
                .orElseGet(() -> candidateRepo.save(new Candidate(infyPos, "Infy", "Candidate", "photo.jpg", "Bio", "sym.png", "Infosys")));

        String enc = cryptoService.encryptVote(infyCand.getId());
        String sig = cryptoService.signVote(enc);
        String hash = cryptoService.calculateVoteHash(infyVoter.getId(), infyCand.getId(), infyPos.getId(), enc, sig);
        Vote infyVote = voteRepo.save(new Vote(infyVoter, infyCand, infyPos, enc, sig, hash));

        // Mock session for infosys_admin
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        com.votesystem.model.Admin infyAdmin = adminRepo.findByUsername("infosys_admin").orElseGet(() -> {
            com.votesystem.model.Admin a = new com.votesystem.model.Admin("infosys_admin", "pwd", "Ananya", "Rao", "admin.jpg", "ORG_ADMIN", "Infosys", "Organizer");
            return adminRepo.save(a);
        });
        session.setAttribute("adminId", infyAdmin.getId());
        session.setAttribute("adminOrg", "Infosys");
        session.setAttribute("adminRole", "ORG_ADMIN");

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();

        adminController.listVotes(null, model, request, session);
        VoteAuditReport report = (VoteAuditReport) model.get("auditReport");
        assertNotNull(report);
        for (VoteIntegrityService.VoteAuditItem item : report.getItems()) {
            assertEquals("Infosys", item.getVote().getOrganization(), "Organizer must ONLY see their own organization's ballots!");
        }

        // Clean up
        voteRepo.delete(infyVote);
        candidateRepo.delete(infyCand);
        voterRepo.delete(infyVoter);
        adminRepo.delete(infyAdmin);
    }

    @Test
    void testSuperAdminDashboardImpersonationAndTenantSwitch() {
        org.springframework.mock.web.MockHttpSession superSession = new org.springframework.mock.web.MockHttpSession();
        com.votesystem.model.Admin superAdmin = adminRepo.findByUsername("Venkatesh").orElseThrow();
        superSession.setAttribute("adminId", superAdmin.getId());
        superSession.setAttribute("adminOrg", "ALL");
        superSession.setAttribute("adminRole", "SUPER_ADMIN");

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();

        // Switch to Infosys
        adminController.dashboard("Infosys", model, request, superSession);

        assertEquals("Infosys", model.get("activeOrg"));
        assertEquals(true, model.get("isViewingSpecificOrg"));
        @SuppressWarnings("unchecked")
        java.util.List<Position> scopedPositions = (java.util.List<Position>) model.get("positions");
        for (Position p : scopedPositions) {
            assertEquals("Infosys", p.getTargetOrganization(), "When switched to Infosys, positions must be scoped to Infosys!");
        }

        // Switch back to ALL
        org.springframework.ui.ExtendedModelMap allModel = new org.springframework.ui.ExtendedModelMap();
        adminController.dashboard("ALL", allModel, request, superSession);
        assertEquals("ALL", allModel.get("activeOrg"));
        assertEquals(false, allModel.get("isViewingSpecificOrg"));
    }

    @Test
    void testSuperAdminDeleteOrganizationElection() {
        // Create dummy organization
        String tempOrg = "TestCorp_" + System.currentTimeMillis();
        Position tempPos = positionRepo.save(new Position("Temp Role", 1, 99, "OFFICE_ORGANIZATION", "ALL", tempOrg));
        Candidate tempCand = candidateRepo.save(new Candidate(tempPos, "John", "Doe", "Screenshot.png", "Bio", "Screenshot.png", tempOrg));
        Voter tempVoter = voterRepo.save(new Voter("TEMPV_" + System.currentTimeMillis(), "pwd", "John", "Voter", "Screenshot.png", "j@tc.com", "EMPLOYEE", "General", tempOrg));

        String enc = cryptoService.encryptVote(tempCand.getId());
        String sig = cryptoService.signVote(enc);
        String hash = cryptoService.calculateVoteHash(tempVoter.getId(), tempCand.getId(), tempPos.getId(), enc, sig);
        Vote tempVote = voteRepo.save(new Vote(tempVoter, tempCand, tempPos, enc, sig, hash));
        tempVoter.setHasVoted(true);
        voterRepo.save(tempVoter);

        // Super Admin deletes election
        org.springframework.mock.web.MockHttpSession superSession = new org.springframework.mock.web.MockHttpSession();
        com.votesystem.model.Admin superAdmin = adminRepo.findByUsername("Venkatesh").orElseThrow();
        superSession.setAttribute("adminId", superAdmin.getId());
        superSession.setAttribute("adminRole", "SUPER_ADMIN");

        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttrs = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        adminController.deleteOrganizationElection(tempOrg, superSession, redirectAttrs);

        // Verify that positions, candidates, and votes for tempOrg are deleted
        assertTrue(positionRepo.findAll().stream().noneMatch(p -> tempOrg.equalsIgnoreCase(p.getTargetOrganization())));
        assertTrue(candidateRepo.findAll().stream().noneMatch(c -> tempOrg.equalsIgnoreCase(c.getOrganization())));
        assertTrue(voteRepo.findAll().stream().noneMatch(v -> tempOrg.equalsIgnoreCase(v.getOrganization())));

        // Voter still exists in registry but hasVoted is reset
        Voter updatedVoter = voterRepo.findById(tempVoter.getId()).orElseThrow();
        assertFalse(updatedVoter.isHasVoted(), "Voter voted status must be reset after election removal");

        // Clean up voter
        voterRepo.delete(updatedVoter);
    }

    @Test
    void testDirectVoterRouteAndOrgScoping() {
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/vote/Infosys");
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();

        String view = voterAuthController.showLogin("Infosys", null, model, session, request);
        assertEquals("voter/login", view);
        assertEquals("Infosys", model.get("effectiveOrg"));
        assertTrue((Boolean) model.get("isVoterPortalRoute"));
        assertNotNull(model.get("scheduleInfo"));
    }

    @Test
    void testElectionTimeLimitActiveWindow() {
        String testOrg = "ScheduleOrg_" + System.currentTimeMillis();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime start = now.minusHours(1);
        java.time.LocalDateTime end = now.plusHours(2);

        // Configure schedule
        scheduleService.updateScheduleForOrganization(testOrg, start, end, true);
        com.votesystem.service.ElectionScheduleService.ScheduleInfo info = scheduleService.getSchedule(testOrg);

        assertTrue(info.isTimeLimitActive(), "Time limit should be active");
        assertTrue(info.isVotingOpen(), "Voting should be open");
        assertFalse(info.isNotStarted(), "Should not be in not-started state");
        assertFalse(info.isExpired(), "Should not be expired");
        assertTrue(info.getRemainingSeconds() > 0, "Remaining seconds must be positive");
    }

    @Test
    void testElectionTimeLimitExpiredBlocksSubmission() {
        String expiredOrg = "ExpiredOrg_" + System.currentTimeMillis();
        java.time.LocalDateTime past = java.time.LocalDateTime.now().minusHours(3);
        java.time.LocalDateTime end = java.time.LocalDateTime.now().minusMinutes(10);

        // Set expired schedule
        scheduleService.updateScheduleForOrganization(expiredOrg, past, end, true);
        com.votesystem.service.ElectionScheduleService.ScheduleInfo info = scheduleService.getSchedule(expiredOrg);
        assertTrue(info.isExpired(), "Schedule must be expired");

        // Create voter for expired org
        Voter voter = voterRepo.save(new Voter("EXP_" + System.currentTimeMillis(), "password", "Exp", "Voter", "Screenshot.png", "exp@test.com", "EMPLOYEE", "Dept", expiredOrg));

        // Attempt ballot submit
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("voterId", voter.getVoterId());
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttrs = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String redirect = ballotController.submitBallot(java.util.Collections.emptyMap(), request, session, redirectAttrs);
        assertEquals("redirect:/login", redirect, "Expired election submission must redirect to login");
        assertTrue(redirectAttrs.getFlashAttributes().containsKey("error"), "Should contain error flash attribute");
        assertTrue(((String) redirectAttrs.getFlashAttributes().get("error")).contains("Voting deadline has expired"));

        // Clean up
        voterRepo.delete(voter);
    }

    @Test
    void testElectionNotStartedBlocksLogin() {
        String futureOrg = "FutureOrg_" + System.currentTimeMillis();
        java.time.LocalDateTime futureStart = java.time.LocalDateTime.now().plusDays(2);
        java.time.LocalDateTime futureEnd = java.time.LocalDateTime.now().plusDays(3);

        scheduleService.updateScheduleForOrganization(futureOrg, futureStart, futureEnd, true);
        com.votesystem.service.ElectionScheduleService.ScheduleInfo info = scheduleService.getSchedule(futureOrg);
        assertTrue(info.isNotStarted(), "Schedule must be in not-started state");

        // Create voter
        Voter voter = voterRepo.save(new Voter("FUT_" + System.currentTimeMillis(), passwordEncoder.encode("password"), "Fut", "Voter", "Screenshot.png", "fut@test.com", "EMPLOYEE", "Dept", futureOrg));

        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttrs = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String result = voterAuthController.handleLogin(voter.getVoterId(), "password", futureOrg, session, redirectAttrs);
        assertEquals("redirect:/login?org=" + futureOrg, result);
        assertTrue(redirectAttrs.getFlashAttributes().containsKey("error"));
        assertTrue(((String) redirectAttrs.getFlashAttributes().get("error")).contains("Voting has not commenced yet"));

        // Clean up
        voterRepo.delete(voter);
    }

    @Test
    void testElectionScheduleControllerAndPresetValidation() {
        Admin admin = adminRepo.findByUsername("Venkatesh").orElseGet(() -> {
            Admin a = new Admin("Venkatesh", passwordEncoder.encode("password"), "Venkatesh", "Admin", "admin.jpg");
            return adminRepo.save(a);
        });

        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("adminId", admin.getId());
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttrs = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        // 1. Validation test: End time before start time
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String startStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        String invalidEndStr = now.minusHours(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        String res1 = adminController.updateElectionSchedule(startStr, invalidEndStr, true, null, "ALL", session, request, redirectAttrs);
        assertEquals("redirect:/admin/dashboard", res1);
        assertTrue(redirectAttrs.getFlashAttributes().containsKey("error"), "Should trigger validation error for end before start");

        // 2. Preset test: 4 Hours preset
        redirectAttrs = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        String res2 = adminController.updateElectionSchedule(null, null, true, 4, "Infosys", session, request, redirectAttrs);
        assertEquals("redirect:/admin/dashboard", res2);
        assertTrue(redirectAttrs.getFlashAttributes().containsKey("success"));

        com.votesystem.service.ElectionScheduleService.ScheduleInfo infySchedule = scheduleService.getSchedule("Infosys");
        assertTrue(infySchedule.isTimeLimitActive(), "Infosys time limit should be active");
        assertTrue(infySchedule.isVotingOpen(), "Infosys voting should be open");
        assertTrue(infySchedule.getRemainingSeconds() > 14000, "Remaining seconds should be around 4 hours (14400s)");

        // 3. Multi-tenant independence: Another organization should NOT inherit Infosys schedule unless configured
        String otherOrg = "OtherOrg_" + System.currentTimeMillis();
        com.votesystem.service.ElectionScheduleService.ScheduleInfo otherSchedule = scheduleService.getSchedule(otherOrg);
        assertNotEquals("Infosys", otherSchedule.getScopeName(), "Other org should not have Infosys scope name");
    }

    @Test
    void testOrgMatcherFuzzyMatchingAndCandidateInheritance() {
        // 1. Fuzzy matching
        assertTrue(com.votesystem.util.OrgMatcher.matches("Inovation Hub", "Invation Hub"), "Inovation Hub and Invation Hub must match");
        assertTrue(com.votesystem.util.OrgMatcher.matches("Inovation Hub", "Innovation Hub"), "Inovation Hub and Innovation Hub must match");
        assertTrue(com.votesystem.util.OrgMatcher.matches("Invation Hub", "Inovation Hub"), "Invation Hub and Inovation Hub must match");
        assertFalse(com.votesystem.util.OrgMatcher.matches("Infosys", "Wipro"), "Infosys and Wipro must not match");
        assertFalse(com.votesystem.util.OrgMatcher.matches("CRCE College", "Infosys"), "CRCE College and Infosys must not match");

        // 2. Candidate organization inheritance from Position
        Position pos = new Position("Inovation Hub Leader", 1, 1, "OFFICE_ORGANIZATION", "ALL", "Invation Hub");
        pos = positionRepo.save(pos);

        Candidate cand = new Candidate();
        cand.setPosition(pos);
        cand.setFirstname("Venkatesha");
        cand.setLastname("GN");
        assertEquals("Invation Hub", cand.getOrganization(), "Candidate without explicit org must inherit from position targetOrganization");

        // 3. Voter with 'Inovation Hub' or 'Invation Hub' should see the position
        Voter v = new Voter("INV_" + System.currentTimeMillis(), "password", "Rolex", "Star", "photo.jpg", "rolex@test.com");
        v.setVoterCategory("EMPLOYEE");
        v.setOrganization("Inovation Hub");
        voterRepo.save(v);

        assertTrue(ballotController.isPositionEligibleForVoter(pos, v), "Voter with Inovation Hub must be eligible for Invation Hub position");
    }

    @Test
    void testUnauthenticatedUserCannotAccessAdminDashboard() {
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        // No adminId in session (e.g. voter session or anonymous visitor)
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.ui.ConcurrentModel model = new org.springframework.ui.ConcurrentModel();

        String view = adminController.dashboard(null, model, request, session);
        assertEquals("redirect:/admin/login", view, "Unauthenticated user must be redirected to /admin/login instead of seeing Super Admin dashboard");
    }

    @Test
    void testElectionClosingFeatureForSpecifiedTime() {
        String testOrg = "TestScheduleOrg_" + System.currentTimeMillis();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // 1. Active voting window: started 10 mins ago, closes in 10 mins
        java.time.LocalDateTime start = now.minusMinutes(10);
        java.time.LocalDateTime end = now.plusMinutes(10);
        scheduleService.updateScheduleForOrganization(testOrg, start, end, true);

        com.votesystem.service.ElectionScheduleService.ScheduleInfo activeSchedule = scheduleService.getSchedule(testOrg);
        assertTrue(activeSchedule.isTimeLimitActive(), "Time limit should be active");
        assertFalse(activeSchedule.isNotStarted(), "Polls should be started");
        assertFalse(activeSchedule.isExpired(), "Polls should not be expired yet");
        assertTrue(activeSchedule.isVotingOpen(), "Voting should be open");
        assertTrue(activeSchedule.getRemainingSeconds() > 0, "Remaining seconds should be positive");
        assertEquals("LIVE", activeSchedule.getScheduleStatus());

        // 2. Expired voting window: started 30 mins ago, closed 5 mins ago
        java.time.LocalDateTime expiredEnd = now.minusMinutes(5);
        scheduleService.updateScheduleForOrganization(testOrg, start, expiredEnd, true);

        com.votesystem.service.ElectionScheduleService.ScheduleInfo expiredSchedule = scheduleService.getSchedule(testOrg);
        assertTrue(expiredSchedule.isTimeLimitActive(), "Time limit should be active");
        assertTrue(expiredSchedule.isExpired(), "Polls should be expired");
        assertFalse(expiredSchedule.isVotingOpen(), "Voting should NOT be open after specified closing time");
        assertEquals(0, expiredSchedule.getRemainingSeconds(), "Remaining seconds should be 0 when expired");
        assertEquals("EXPIRED", expiredSchedule.getScheduleStatus());

        // 3. Future scheduled election: starts in 30 mins, ends in 60 mins
        java.time.LocalDateTime futureStart = now.plusMinutes(30);
        java.time.LocalDateTime futureEnd = now.plusMinutes(60);
        scheduleService.updateScheduleForOrganization(testOrg, futureStart, futureEnd, true);

        com.votesystem.service.ElectionScheduleService.ScheduleInfo futureSchedule = scheduleService.getSchedule(testOrg);
        assertTrue(futureSchedule.isTimeLimitActive());
        assertTrue(futureSchedule.isNotStarted(), "Polls should not be started yet");
        assertFalse(futureSchedule.isExpired());
        assertFalse(futureSchedule.isVotingOpen(), "Voting should NOT be open before start time");
        assertEquals("NOT_STARTED", futureSchedule.getScheduleStatus());

        // 4. Disable time limit: open indefinitely
        scheduleService.updateScheduleForOrganization(testOrg, null, null, false);
        com.votesystem.service.ElectionScheduleService.ScheduleInfo unlimitedSchedule = scheduleService.getSchedule(testOrg);
        assertFalse(unlimitedSchedule.isTimeLimitActive(), "Time limit should be inactive");
        assertFalse(unlimitedSchedule.isExpired());
        assertTrue(unlimitedSchedule.isVotingOpen(), "Voting should be open when time limit is disabled");
    }
}
