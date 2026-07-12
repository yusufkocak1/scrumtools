package com.scrumtools.repository;

import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findBySystemRole(SystemRole systemRole);
}

