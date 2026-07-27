package com.scrumtools.repository;

import com.scrumtools.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {

    Optional<EmailMessage> findByMessageId(String messageId);

    List<EmailMessage> findByOrganizationIdAndTemplateCodeOrderByCreatedAtDesc(
            UUID organizationId, String templateCode);
}
