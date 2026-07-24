package com.scrumtools.repository;

import com.scrumtools.entity.CiJobMapping;
import com.scrumtools.entity.enums.CiJobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CiJobMappingRepository extends JpaRepository<CiJobMapping, UUID> {

    List<CiJobMapping> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    /** Task/release ekranlarının listeleri — yalnız kullanılabilir eşlemeler. */
    List<CiJobMapping> findByProjectIdAndJobTypeAndEnabledTrueOrderByDisplayNameAsc(
            UUID projectId, CiJobType jobType);

    List<CiJobMapping> findByConnectionId(UUID connectionId);

    long countByConnectionId(UUID connectionId);

    Optional<CiJobMapping> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByConnectionIdAndProjectIdAndJobFullName(
            UUID connectionId, UUID projectId, String jobFullName);

    /** Org genelinde eşleme sayısı — paket limiti kontrolü için. */
    @Query("SELECT COUNT(m) FROM CiJobMapping m WHERE m.connection.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") UUID orgId);
}
