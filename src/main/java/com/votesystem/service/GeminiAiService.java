package com.votesystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votesystem.dto.AiElectionPlan;
import com.votesystem.dto.AiExecutionResult;
import com.votesystem.model.*;
import com.votesystem.repository.*;
import com.votesystem.util.OrgMatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.1-flash-lite}")
    private String modelName;

    private final PositionRepository positionRepo;
    private final CandidateRepository candidateRepo;
    private final VoterRepository voterRepo;
    private final AdminRepository adminRepo;
    private final ElectionScheduleService electionScheduleService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditRepo;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiAiService(PositionRepository positionRepo,
                            CandidateRepository candidateRepo,
                            VoterRepository voterRepo,
                            AdminRepository adminRepo,
                            ElectionScheduleService electionScheduleService,
                            PasswordEncoder passwordEncoder,
                            AuditLogRepository auditRepo,
                            ObjectMapper objectMapper) {
        this.positionRepo = positionRepo;
        this.candidateRepo = candidateRepo;
        this.voterRepo = voterRepo;
        this.adminRepo = adminRepo;
        this.electionScheduleService = electionScheduleService;
        this.passwordEncoder = passwordEncoder;
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Sends the administrator's natural language prompt to Google Gemini API
     * and parses the returned JSON into an AiElectionPlan.
     */
    public AiElectionPlan generatePlanFromPrompt(String userPrompt, Admin currentAdmin) {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty.");
        }

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String adminOrg = (currentAdmin != null && currentAdmin.getOrganization() != null)
                ? currentAdmin.getOrganization().trim() : "ALL";
        boolean isSuper = (currentAdmin != null && currentAdmin.isSuperAdmin());

        String systemInstruction = buildSystemPrompt(currentTime, adminOrg, isSuper);

        try {
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            Map<String, Object> contentsMap = new LinkedHashMap<>();
            contentsMap.put("role", "user");

            Map<String, String> partMap = new LinkedHashMap<>();
            partMap.put("text", systemInstruction + "\n\nUser Request: " + userPrompt.trim());
            contentsMap.put("parts", List.of(partMap));

            payloadMap.put("contents", List.of(contentsMap));

            Map<String, Object> genConfig = new LinkedHashMap<>();
            genConfig.put("responseMimeType", "application/json");
            payloadMap.put("generationConfig", genConfig);

            String requestBody = objectMapper.writeValueAsString(payloadMap);

            String targetModel = (modelName != null && !modelName.isBlank()) ? modelName.trim() : "gemini-3.1-flash-lite";
            String key = resolveApiKey();
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":generateContent?key=" + key;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API error (Status {}): {}", response.statusCode(), response.body());
                throw new RuntimeException("Gemini AI API responded with status " + response.statusCode() + ": " + response.body());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode textNode = rootNode.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new RuntimeException("No response text returned by Gemini AI.");
            }

            String rawJson = textNode.asText().trim();
            // Clean markdown fence if any
            if (rawJson.startsWith("```")) {
                rawJson = rawJson.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
            }

            AiElectionPlan plan = objectMapper.readValue(rawJson, AiElectionPlan.class);

            // Enforce Multi-tenant isolation for non-superadmin
            if (!isSuper) {
                plan.setOrganization(adminOrg);
                if (plan.getPositions() != null) {
                    for (AiElectionPlan.PositionPlan pos : plan.getPositions()) {
                        pos.setTargetOrganization(adminOrg);
                    }
                }
                if (plan.getCandidates() != null) {
                    for (AiElectionPlan.CandidatePlan cand : plan.getCandidates()) {
                        cand.setOrganization(adminOrg);
                    }
                }
                if (plan.getVoters() != null) {
                    for (AiElectionPlan.VoterPlan voter : plan.getVoters()) {
                        voter.setOrganization(adminOrg);
                    }
                }
            }

            return plan;

        } catch (Exception e) {
            log.error("Failed to generate AI Election Plan from prompt: {}", e.getMessage(), e);
            throw new RuntimeException("AI Planning Failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the parsed AI Election Plan directly into the database.
     */
    @Transactional
    public AiExecutionResult executePlan(AiElectionPlan plan, Admin currentAdmin, HttpServletRequest request) {
        AiExecutionResult result = new AiExecutionResult();
        if (plan == null) {
            result.setSuccess(false);
            result.setMessage("Cannot execute null plan.");
            return result;
        }

        boolean isSuper = (currentAdmin != null && currentAdmin.isSuperAdmin());
        String adminOrg = (currentAdmin != null && currentAdmin.getOrganization() != null)
                ? currentAdmin.getOrganization().trim() : "ALL";

        String targetOrg = isSuper
                ? (plan.getOrganization() != null && !plan.getOrganization().isBlank() ? plan.getOrganization().trim() : "ALL")
                : adminOrg;

        result.setOrganization(targetOrg);
        result.setSummary(plan.getSummary());

        try {
            // 1. Process Election Schedule
            if (plan.getSchedule() != null && Boolean.TRUE.equals(plan.getSchedule().getEnabled())) {
                LocalDateTime start = null;
                LocalDateTime end = null;
                Integer duration = plan.getSchedule().getDurationHours();

                if (duration != null && duration > 0) {
                    start = LocalDateTime.now();
                    end = start.plusHours(duration);
                } else {
                    if (plan.getSchedule().getStartTime() != null && !plan.getSchedule().getStartTime().isBlank()) {
                        try {
                            start = LocalDateTime.parse(plan.getSchedule().getStartTime().trim());
                        } catch (Exception ex) {
                            start = LocalDateTime.now();
                        }
                    } else {
                        start = LocalDateTime.now();
                    }

                    if (plan.getSchedule().getEndTime() != null && !plan.getSchedule().getEndTime().isBlank()) {
                        try {
                            end = LocalDateTime.parse(plan.getSchedule().getEndTime().trim());
                        } catch (Exception ex) {
                            end = start.plusHours(3);
                        }
                    } else {
                        end = start.plusHours(3);
                    }
                }

                if (end.isAfter(start)) {
                    electionScheduleService.updateScheduleForOrganization(targetOrg, start, end, true);
                    result.setScheduleUpdated(true);
                    result.setFormattedStartTime(start.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
                    result.setFormattedEndTime(end.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
                }
            }

            // 2. Process Positions
            Map<String, Position> resolvedPositions = new HashMap<>();
            if (plan.getPositions() != null && !plan.getPositions().isEmpty()) {
                for (AiElectionPlan.PositionPlan posPlan : plan.getPositions()) {
                    if (posPlan.getDescription() == null || posPlan.getDescription().isBlank()) continue;
                    String desc = posPlan.getDescription().trim();

                    // Check if position already exists for this org
                    Position position = positionRepo.findAll().stream()
                            .filter(p -> p.getDescription().equalsIgnoreCase(desc) && OrgMatcher.matches(p.getTargetOrganization(), targetOrg))
                            .findFirst()
                            .orElse(null);

                    if (position == null) {
                        position = new Position();
                        position.setDescription(desc);
                        position.setMaxVote(posPlan.getMaxVote() != null ? posPlan.getMaxVote() : 1);
                        position.setPriority(posPlan.getPriority() != null ? posPlan.getPriority() : 1);
                        position.setElectorateScope(posPlan.getElectorateScope() != null ? posPlan.getElectorateScope().trim().toUpperCase() : "ALL");
                        position.setTargetDepartment(posPlan.getTargetDepartment() != null ? posPlan.getTargetDepartment().trim() : "ALL");
                        position.setTargetOrganization(targetOrg);
                        position = positionRepo.save(position);
                        result.getCreatedPositions().add(desc);
                        result.setPositionsCreated(result.getPositionsCreated() + 1);
                    }
                    resolvedPositions.put(desc.toLowerCase(), position);
                }
            }

            // 3. Process Candidates
            if (plan.getCandidates() != null && !plan.getCandidates().isEmpty()) {
                for (AiElectionPlan.CandidatePlan candPlan : plan.getCandidates()) {
                    if (candPlan.getFirstname() == null || candPlan.getFirstname().isBlank()) continue;

                    Position targetPos = null;
                    if (candPlan.getPositionName() != null && !candPlan.getPositionName().isBlank()) {
                        String posKey = candPlan.getPositionName().trim().toLowerCase();
                        targetPos = resolvedPositions.get(posKey);
                        if (targetPos == null) {
                            targetPos = positionRepo.findAll().stream()
                                    .filter(p -> p.getDescription().toLowerCase().contains(posKey) || posKey.contains(p.getDescription().toLowerCase()))
                                    .filter(p -> OrgMatcher.matches(p.getTargetOrganization(), targetOrg))
                                    .findFirst()
                                    .orElse(null);
                        }
                    }

                    if (targetPos == null && !resolvedPositions.isEmpty()) {
                        targetPos = resolvedPositions.values().iterator().next();
                    }

                    if (targetPos == null) {
                        // Create fallback default position
                        targetPos = new Position("Executive Officer", 1, 1, "ALL", "ALL");
                        targetPos.setTargetOrganization(targetOrg);
                        targetPos = positionRepo.save(targetPos);
                        resolvedPositions.put("executive officer", targetPos);
                        result.getCreatedPositions().add(targetPos.getDescription());
                        result.setPositionsCreated(result.getPositionsCreated() + 1);
                    }

                    Candidate cand = new Candidate();
                    cand.setPosition(targetPos);
                    cand.setFirstname(candPlan.getFirstname().trim());
                    cand.setLastname(candPlan.getLastname() != null ? candPlan.getLastname().trim() : "Candidate");
                    cand.setPlatform(candPlan.getPlatform() != null && !candPlan.getPlatform().isBlank()
                            ? candPlan.getPlatform().trim() : "Committed to excellence in leadership.");
                    cand.setOrganization(targetOrg);
                    cand.setPhoto("cand1.jpg");
                    cand.setSymbol("symbol.png");
                    candidateRepo.save(cand);

                    result.getCreatedCandidates().add(cand.getFirstname() + " " + cand.getLastname() + " (" + targetPos.getDescription() + ")");
                    result.setCandidatesCreated(result.getCandidatesCreated() + 1);
                }
            }

            // 4. Process Voters
            if (plan.getVoters() != null && !plan.getVoters().isEmpty()) {
                int defaultVoterCount = 1;
                for (AiElectionPlan.VoterPlan voterPlan : plan.getVoters()) {
                    int count = voterPlan.getGenerateCount() != null && voterPlan.getGenerateCount() > 0
                            ? voterPlan.getGenerateCount() : 1;

                    for (int i = 0; i < count; i++) {
                        String voterId = generateRandomVoterId();
                        while (voterRepo.existsByVoterId(voterId)) {
                            voterId = generateRandomVoterId();
                        }

                        String fn = voterPlan.getFirstname() != null && !voterPlan.getFirstname().isBlank()
                                ? voterPlan.getFirstname().trim() : "Voter";
                        if (count > 1 && (voterPlan.getFirstname() == null || voterPlan.getFirstname().isBlank())) {
                            fn = "Voter " + (result.getVotersCreated() + 1);
                        } else if (count > 1) {
                            fn = fn + " " + (i + 1);
                        }

                        String ln = voterPlan.getLastname() != null && !voterPlan.getLastname().isBlank()
                                ? voterPlan.getLastname().trim() : targetOrg;

                        String email = voterPlan.getEmail();
                        if (email == null || email.isBlank() || count > 1) {
                            email = fn.toLowerCase().replaceAll("[^a-z0-9]", "") + "."
                                    + ln.toLowerCase().replaceAll("[^a-z0-9]", "") + (i + 1)
                                    + "@" + targetOrg.toLowerCase().replaceAll("[^a-z0-9]", "") + ".org";
                        }

                        Voter voter = new Voter();
                        voter.setVoterId(voterId);
                        voter.setPassword(passwordEncoder.encode("password123"));
                        voter.setFirstname(fn);
                        voter.setLastname(ln);
                        voter.setEmail(email);
                        voter.setVoterCategory(voterPlan.getVoterCategory() != null ? voterPlan.getVoterCategory().toUpperCase() : "STUDENT");
                        voter.setDepartment(voterPlan.getDepartment() != null ? voterPlan.getDepartment().trim() : "General");
                        voter.setOrganization(targetOrg);
                        voter.setHasVoted(false);
                        voterRepo.save(voter);

                        result.getCreatedVoters().add(new AiExecutionResult.CreatedVoterInfo(
                                voterId,
                                fn + " " + ln,
                                email,
                                voter.getDepartment(),
                                targetOrg,
                                "password123"
                        ));
                        result.setVotersCreated(result.getVotersCreated() + 1);
                    }
                }
            }

            // 5. Process Organizers (Only allowed for Super Admins)
            if (isSuper && plan.getOrganizers() != null && !plan.getOrganizers().isEmpty()) {
                for (AiElectionPlan.OrganizerPlan orgPlan : plan.getOrganizers()) {
                    if (orgPlan.getFirstname() == null || orgPlan.getFirstname().isBlank()) continue;

                    String username = orgPlan.getUsername();
                    if (username == null || username.isBlank()) {
                        username = orgPlan.getFirstname().toLowerCase().replaceAll("[^a-z0-9]", "") + "_admin";
                    }
                    if (!adminRepo.existsByUsername(username)) {
                        Admin newAdmin = new Admin(
                                username,
                                passwordEncoder.encode("organizer123"),
                                orgPlan.getFirstname().trim(),
                                orgPlan.getLastname() != null ? orgPlan.getLastname().trim() : "Admin",
                                "Screenshot.png",
                                "ORG_ADMIN",
                                targetOrg,
                                orgPlan.getDesignation() != null ? orgPlan.getDesignation() : "Election Organizer"
                        );
                        adminRepo.save(newAdmin);
                        result.getCreatedOrganizers().add(username + " (" + targetOrg + ")");
                        result.setOrganizersCreated(result.getOrganizersCreated() + 1);
                    }
                }
            }

            // 6. Audit Log
            String clientIp = request != null ? request.getRemoteAddr() : "127.0.0.1";
            auditRepo.save(new AuditLog(
                    "GEMINI_AI_ELECTION_PROVISIONING",
                    UUID.randomUUID().toString(),
                    clientIp,
                    "Gemini AI automated configuration applied for organization: " + targetOrg
                            + ". ScheduleUpdated=" + result.isScheduleUpdated()
                            + ", Positions=" + result.getPositionsCreated()
                            + ", Candidates=" + result.getCandidatesCreated()
                            + ", Voters=" + result.getVotersCreated()
            ));

            result.setSuccess(true);
            result.setMessage("AI Election provisioning completed successfully!");
            return result;

        } catch (Exception e) {
            log.error("Execution of AI Election Plan failed: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Execution error: " + e.getMessage());
            return result;
        }
    }

    private String buildSystemPrompt(String currentTime, String adminOrg, boolean isSuper) {
        return """
        You are an intelligent AI Election Administrator for a high-security multi-tenant E-Voting platform.
        Current System Server Timestamp: %s (Asia/Kolkata).
        Admin Role: %s.
        Admin Organization Scope: %s.

        Analyze the natural language prompt and return a valid JSON object matching this EXACT schema:
        {
          "summary": "Short 1-2 sentence summary of what will be configured",
          "organization": "Target organization name (e.g. Infosys, Wipro, Google, Invation Hub, etc.)",
          "schedule": {
            "enabled": true or false,
            "startTime": "YYYY-MM-DDTHH:mm:ss" or null,
            "endTime": "YYYY-MM-DDTHH:mm:ss" or null,
            "durationHours": integer or null
          },
          "positions": [
            {
              "description": "Position Title (e.g. Cultural Coordinator, President)",
              "maxVote": 1,
              "priority": 1,
              "electorateScope": "ALL" or "STUDENT" or "EMPLOYEE",
              "targetDepartment": "ALL" or department name,
              "targetOrganization": "Organization name"
            }
          ],
          "candidates": [
            {
              "firstname": "First name",
              "lastname": "Last name",
              "positionName": "Matching position description",
              "platform": "Campaign slogan / manifesto",
              "organization": "Organization name"
            }
          ],
          "voters": [
            {
              "firstname": "First name or null",
              "lastname": "Last name or null",
              "email": "Email address or null",
              "voterCategory": "STUDENT" or "EMPLOYEE" or "FACULTY",
              "department": "Engineering / IT / General",
              "organization": "Organization name",
              "generateCount": 1
            }
          ],
          "organizers": [
            {
              "username": "unique username or null",
              "firstname": "First name",
              "lastname": "Last name",
              "organization": "Organization name",
              "designation": "Election Organizer"
            }
          ]
        }

        Rules:
        1. If user says 'Schedule an election for X starting now for 4 hours', set schedule.enabled=true, compute startTime as now, endTime as now + 4 hours (or set durationHours=4).
        2. If user requests 'create 5 voters' or 'register 10 voters', provide a voter entry with generateCount=5 (or generate 5 realistic voter items).
        3. Associate candidates with the appropriate positionName matching positions[].description.
        4. If not specified, default organization to %s.
        5. Return ONLY PURE RAW JSON. No markdown code blocks, no formatting.
        """.formatted(currentTime, isSuper ? "SUPER_ADMIN" : "ORG_ADMIN", adminOrg, adminOrg.equals("ALL") ? "Infosys" : adminOrg);
    }

    private String generateRandomVoterId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(15);
        for (int i = 0; i < 15; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        String sysProp = System.getProperty("gemini.api.key");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        try {
            java.io.File keyFile = new java.io.File("keys/gemini_api_key.txt");
            if (keyFile.exists()) {
                return java.nio.file.Files.readString(keyFile.toPath()).trim();
            }
        } catch (Exception ignored) {}
        throw new IllegalStateException("Google Gemini API Key is missing. Please set the GEMINI_API_KEY environment variable or application property.");
    }
}
