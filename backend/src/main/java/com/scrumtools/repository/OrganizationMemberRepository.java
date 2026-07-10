package com.scrumtools.repository;

import com.scrumtools.entity.OrganizationMember;
import com.scrumtools.entity.enums.OrgRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByOrganizationId(UUID organizationId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserEmail(UUID organizationId, String email);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndUserEmailAndOrgRoleIn(UUID organizationId, String email, List<OrgRole> roles);

    @Query("""
            SELECT om FROM OrganizationMember om
            WHERE om.organization.id = :orgId
              AND (LOWER(om.user.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(om.user.email) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY om.user.name
            """)
    List<OrganizationMember> searchByOrganizationIdAndUserNameOrEmail(@Param("orgId") UUID orgId, @Param("query") String query);
}

