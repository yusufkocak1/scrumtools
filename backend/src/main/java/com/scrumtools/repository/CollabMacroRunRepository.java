package com.scrumtools.repository;

import com.scrumtools.entity.CollabMacroRun;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollabMacroRunRepository extends JpaRepository<CollabMacroRun, UUID> {

    List<CollabMacroRun> findByMacroIdOrderByStartedAtDesc(UUID macroId, Limit limit);
}
