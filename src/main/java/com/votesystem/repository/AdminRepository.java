package com.votesystem.repository;

import com.votesystem.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
    boolean existsByUsername(String username);
    java.util.List<Admin> findByOrganization(String organization);
    java.util.List<Admin> findAllByOrderByCreatedOnDesc();
}
