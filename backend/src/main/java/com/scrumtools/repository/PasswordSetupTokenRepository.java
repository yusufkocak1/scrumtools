package com.scrumtools.repository;

import com.scrumtools.entity.PasswordSetupToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, UUID> {

    Optional<PasswordSetupToken> findByTokenHash(String tokenHash);

    List<PasswordSetupToken> findByUserIdAndUsedAtIsNull(UUID userId);
}
