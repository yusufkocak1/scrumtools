package com.scrumtools.repository;

import com.scrumtools.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    List<TeamMember> findByTeamId(UUID teamId);

    Optional<TeamMember> findByTeamIdAndEmail(UUID teamId, String email);

    boolean existsByTeamIdAndEmail(UUID teamId, String email);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    List<TeamMember> findByEmail(String email);
}
