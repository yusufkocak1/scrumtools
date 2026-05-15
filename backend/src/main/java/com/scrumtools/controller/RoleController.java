package com.scrumtools.controller;

import com.scrumtools.dto.RoleRequest;
import com.scrumtools.dto.RoleResponse;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.RoleScope;
import com.scrumtools.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getRoles(
            @RequestParam(required = false) RoleScope scope) {
        if (scope != null) {
            return ResponseEntity.ok(roleService.getRolesByScope(scope));
        }
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/defaults")
    public ResponseEntity<List<RoleResponse>> getDefaultRoles() {
        return ResponseEntity.ok(roleService.getDefaultRoles());
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(roleId, request));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }
}

