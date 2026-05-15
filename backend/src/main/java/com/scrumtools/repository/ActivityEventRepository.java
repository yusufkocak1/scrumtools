package com.scrumtools.repository;

import com.scrumtools.entity.ActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    Page<ActivityEvent> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    Page<ActivityEvent> findByActorEmailOrderByCreatedAtDesc(String email, Pageable pageable);
}

