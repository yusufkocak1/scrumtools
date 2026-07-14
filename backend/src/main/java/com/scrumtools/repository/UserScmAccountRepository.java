package com.scrumtools.repository;

import com.scrumtools.entity.UserScmAccount;
import com.scrumtools.entity.enums.ScmProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserScmAccountRepository extends JpaRepository<UserScmAccount, UUID> {

    List<UserScmAccount> findByUserId(UUID userId);

    Optional<UserScmAccount> findByIdAndUserId(UUID id, UUID userId);

    List<UserScmAccount> findByUserEmail(String email);

    List<UserScmAccount> findByUserEmailAndProvider(String email, ScmProvider provider);

    /** Aynı provider + baseUrl (null'lar eş sayılır) için mevcut kayıt — duplicate kontrolü. */
    @Query("""
            SELECT a FROM UserScmAccount a
            WHERE a.user.id = :userId AND a.provider = :provider
              AND ((:baseUrl IS NULL AND a.baseUrl IS NULL) OR a.baseUrl = :baseUrl)
            """)
    Optional<UserScmAccount> findExisting(@Param("userId") UUID userId,
                                          @Param("provider") ScmProvider provider,
                                          @Param("baseUrl") String baseUrl);
}
