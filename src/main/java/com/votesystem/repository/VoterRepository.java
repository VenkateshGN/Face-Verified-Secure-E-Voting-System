package com.votesystem.repository;

import com.votesystem.model.Voter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoterRepository extends JpaRepository<Voter, Long> {
    Optional<Voter> findByVoterId(String voterId);
    Optional<Voter> findByTrackingToken(String trackingToken);
    boolean existsByVoterId(String voterId);
    long countByHasVotedTrue();
}
