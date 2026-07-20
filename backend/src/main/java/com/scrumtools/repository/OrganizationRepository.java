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

    // ORDER BY olmadan sıra DB'nin insafına kalıyor ve istemcideki "ilk organizasyon"
    // varsayılanı sayfa yüklemeleri arasında değişebiliyordu.
    @Query("SELECT o FROM Organization o JOIN OrganizationMember om ON om.organization = o WHERE om.user.email = :email ORDER BY o.name ASC")
    List<Organization> findAllByMemberEmail(@Param("email") String email);
}

