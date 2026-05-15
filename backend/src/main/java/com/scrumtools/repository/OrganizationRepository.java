package com.scrumtools.repository;

import com.scrumtools.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Organization> findByOwnerId(UUID ownerId);

    @Query("SELECT o FROM Organization o JOIN OrganizationMember om ON om.organization = o WHERE om.user.email = :email")
    List<Organization> findAllByMemberEmail(@Param("email") String email);
}

