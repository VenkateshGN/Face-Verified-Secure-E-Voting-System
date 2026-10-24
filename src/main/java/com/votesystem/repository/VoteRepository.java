package com.votesystem.repository;

import com.votesystem.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByVoterId(Long voterId);

    boolean existsByVoterId(Long voterId);

    long countByCandidateId(Long candidateId);

    @Query("SELECT COUNT(DISTINCT v.voter.id) FROM Vote v")
    long countDistinctVoters();

    @Query("SELECT v FROM Vote v JOIN FETCH v.position p JOIN FETCH v.candidate c JOIN FETCH v.voter vt ORDER BY p.priority ASC, v.id ASC")
    List<Vote> findAllWithDetails();
}
