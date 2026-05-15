package com.scrumtools.repository;

import com.scrumtools.entity.Invitation;
import com.scrumtools.entity.enums.InvitationStatus;
import com.scrumtools.entity.enums.InvitationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByEmailAndStatus(String email, InvitationStatus status);

    List<Invitation> findByTargetIdAndTypeAndStatus(UUID targetId, InvitationType type, InvitationStatus status);

    boolean existsByEmailAndTargetIdAndTypeAndStatus(String email, UUID targetId, InvitationType type, InvitationStatus status);
}

