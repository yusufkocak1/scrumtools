package com.scrumtools.repository;

import com.scrumtools.entity.SupportTicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportTicketAttachmentRepository extends JpaRepository<SupportTicketAttachment, UUID> {

    List<SupportTicketAttachment> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);

    long countByTicketId(UUID ticketId);
}
