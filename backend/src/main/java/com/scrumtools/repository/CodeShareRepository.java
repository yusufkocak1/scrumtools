package com.scrumtools.repository;

import com.scrumtools.entity.CodeShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CodeShareRepository extends JpaRepository<CodeShare, UUID> {

    Optional<CodeShare> findByTeamIdAndTag(UUID teamId, String tag);
}

