package com.votesystem.repository;

import com.votesystem.model.ElectionSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElectionSettingRepository extends JpaRepository<ElectionSetting, Long> {
}
