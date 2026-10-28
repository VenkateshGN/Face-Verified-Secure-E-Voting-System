package com.votesystem.controller;

import com.votesystem.model.Candidate;
import com.votesystem.model.Position;
import com.votesystem.repository.CandidateRepository;
import com.votesystem.repository.PositionRepository;
import com.votesystem.repository.VoteRepository;
import com.votesystem.service.BlockchainService;
import com.votesystem.service.VoteIntegrityService;
import com.votesystem.service.VoteIntegrityService.VoteAuditReport;
import com.votesystem.util.OrgMatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class PublicAuditController {

    private final VoteIntegrityService integrityService;
    private final BlockchainService blockchainService;
    private final PositionRepository positionRepo;
    private final CandidateRepository candidateRepo;
    private final VoteRepository voteRepo;
    private final com.votesystem.repository.VoterRepository voterRepo;
    private final com.votesystem.repository.ElectionSettingRepository electionRepo;

    public PublicAuditController(VoteIntegrityService integrityService, BlockchainService blockchainService,
                                 PositionRepository positionRepo, CandidateRepository candidateRepo,
                                 VoteRepository voteRepo, com.votesystem.repository.VoterRepository voterRepo,
                                 com.votesystem.repository.ElectionSettingRepository electionRepo) {
        this.integrityService = integrityService;
        this.blockchainService = blockchainService;
        this.positionRepo = positionRepo;
        this.candidateRepo = candidateRepo;
        this.voteRepo = voteRepo;
        this.voterRepo = voterRepo;
        this.electionRepo = electionRepo;
    }

    @org.springframework.web.bind.annotation.ModelAttribute("electionSetting")
    public com.votesystem.model.ElectionSetting populateElectionSetting() {
        return electionRepo.findAll().stream().findFirst().orElseGet(() -> {
            com.votesystem.model.ElectionSetting s = new com.votesystem.model.ElectionSetting("Apex Multi-Org Leadership Council", "GENERAL", "Executive Leadership Election 2026", "ACTIVE");
            return electionRepo.save(s);
        });
    }

    @GetMapping("/verify-votes")
    public String publicAudit(@org.springframework.web.bind.annotation.RequestParam(value = "org", required = false) String orgParam,
                              Model model, HttpServletRequest request) {
        VoteAuditReport fullReport = integrityService.auditAllVotes(request.getRemoteAddr(), request.getHeader("User-Agent"));
        BlockchainService.BlockchainValidationResult chainResult = blockchainService.validateBlockchain();

        String activeOrg = (orgParam != null && !orgParam.isBlank()) ? orgParam.trim() : "ALL";

        VoteAuditReport reportToDisplay;
        if ("ALL".equalsIgnoreCase(activeOrg)) {
            reportToDisplay = fullReport;
        } else {
            String filter = activeOrg;
            List<VoteIntegrityService.VoteAuditItem> filteredItems = fullReport.getItems().stream()
                    .filter(item -> OrgMatcher.matches(filter, item.getVote().getOrganization()))
                    .toList();
            int total = filteredItems.size();
            int valid = (int) filteredItems.stream().filter(VoteIntegrityService.VoteAuditItem::isValid).count();
            int tampered = total - valid;
            reportToDisplay = new VoteAuditReport(total, valid, tampered, filteredItems);
        }

        List<String> availableOrganizations = getDistinctOrganizations();

        model.addAttribute("auditReport", reportToDisplay);
        model.addAttribute("chainResult", chainResult);
        model.addAttribute("activeOrg", activeOrg);
        model.addAttribute("availableOrganizations", availableOrganizations);

        return "verify-votes";
    }

    private List<String> getDistinctOrganizations() {
        Set<String> orgs = new LinkedHashSet<>();
        for (Position p : positionRepo.findAll()) {
            if (p.getTargetOrganization() != null && !p.getTargetOrganization().isBlank() && !"ALL".equalsIgnoreCase(p.getTargetOrganization())) {
                orgs.add(p.getTargetOrganization().trim());
            }
        }
        for (Candidate c : candidateRepo.findAll()) {
            if (c.getOrganization() != null && !c.getOrganization().isBlank() && !"ALL".equalsIgnoreCase(c.getOrganization())) {
                orgs.add(c.getOrganization().trim());
            }
        }
        for (com.votesystem.model.Voter v : voterRepo.findAll()) {
            if (v.getOrganization() != null && !v.getOrganization().isBlank() && !"ALL".equalsIgnoreCase(v.getOrganization())) {
                orgs.add(v.getOrganization().trim());
            }
        }
        List<String> sorted = new ArrayList<>(orgs);
        Collections.sort(sorted);
        return sorted;
    }

    @GetMapping("/api/election-results")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLiveResults() {
        Map<String, Object> response = new HashMap<>();
        List<Position> positions = positionRepo.findAllByOrderByPriorityAsc();
        List<Map<String, Object>> positionsData = new ArrayList<>();

        for (Position pos : positions) {
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("positionId", pos.getId());
            pMap.put("description", pos.getDescription());

            List<Candidate> candidates = candidateRepo.findByPositionIdOrderByIdAsc(pos.getId());
            List<String> labels = new ArrayList<>();
            List<Long> counts = new ArrayList<>();

            for (Candidate c : candidates) {
                labels.add(c.getFullName());
                counts.add(voteRepo.countByCandidateId(c.getId()));
            }
            pMap.put("labels", labels);
            pMap.put("counts", counts);
            positionsData.add(pMap);
        }

        response.put("positions", positionsData);
        response.put("totalVotes", voteRepo.count());
        return ResponseEntity.ok(response);
    }
}
