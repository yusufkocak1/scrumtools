package com.scrumtools.repository;

import com.scrumtools.entity.Role;
import com.scrumtools.entity.enums.RoleScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByScope(RoleScope scope);

    List<Role> findByScopeAndScopeId(RoleScope scope, UUID scopeId);

    Optional<Role> findByNameAndScope(String name, RoleScope scope);

    List<Role> findByIsDefaultTrue();
}

