package com.scrumtools.repository;

import com.scrumtools.entity.SupportTicket;
import com.scrumtools.entity.enums.SupportCategory;
import com.scrumtools.entity.enums.SupportTicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    List<SupportTicket> findByUserEmailOrderByUpdatedAtDesc(String email);

    /** Admin listesi — null gelen filtreler yoksayılır. */
    @Query("""
            SELECT t FROM SupportTicket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:category IS NULL OR t.category = :category)
              AND (:organizationId IS NULL OR t.organizationId = :organizationId)
            ORDER BY t.updatedAt DESC
            """)
    Page<SupportTicket> findFiltered(@Param("status") SupportTicketStatus status,
                                     @Param("category") SupportCategory category,
                                     @Param("organizationId") UUID organizationId,
                                     Pageable pageable);
}
