package com.scrumtools.repository;

import com.scrumtools.entity.DocPageVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocPageVersionRepository extends JpaRepository<DocPageVersion, UUID> {

    List<DocPageVersion> findByPageIdOrderByVersionNumberDesc(UUID pageId);

    Optional<DocPageVersion> findByPageIdAndVersionNumber(UUID pageId, int versionNumber);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM DocPageVersion v WHERE v.page.id = :pageId")
    int findMaxVersionNumber(UUID pageId);
}

