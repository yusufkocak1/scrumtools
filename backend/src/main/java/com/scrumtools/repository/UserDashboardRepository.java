package com.scrumtools.repository;

import com.scrumtools.entity.UserDashboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserDashboardRepository extends JpaRepository<UserDashboard, UUID> {
    Optional<UserDashboard> findByUserId(UUID userId);
}

