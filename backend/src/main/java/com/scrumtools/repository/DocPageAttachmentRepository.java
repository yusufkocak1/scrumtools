package com.scrumtools.repository;

import com.scrumtools.entity.DocPageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocPageAttachmentRepository extends JpaRepository<DocPageAttachment, UUID> {

    List<DocPageAttachment> findByPageIdOrderByCreatedAtDesc(UUID pageId);

    int countByPageId(UUID pageId);
}

