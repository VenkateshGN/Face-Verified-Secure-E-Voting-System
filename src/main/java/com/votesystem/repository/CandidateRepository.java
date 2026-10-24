package com.votesystem.repository;

import com.votesystem.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByPositionId(Long positionId);
    List<Candidate> findByPositionIdOrderByIdAsc(Long positionId);
}
