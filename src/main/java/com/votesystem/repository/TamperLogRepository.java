package com.votesystem.repository;

import com.votesystem.model.TamperLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TamperLogRepository extends JpaRepository<TamperLog, Long> {
    List<TamperLog> findAllByOrderByTimestampDesc();
    boolean existsByVoteId(Long voteId);
}
