package com.scrumtools.controller;

import com.scrumtools.dto.CodeShareRequest;
import com.scrumtools.dto.CodeShareResponse;
import com.scrumtools.service.CodeShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/teams/{teamId}/code-shares")
@RequiredArgsConstructor
public class CodeShareController {

    private final CodeShareService codeShareService;

    /**
     * PUT /api/teams/{teamId}/code-shares/{tag} — Kaydet veya güncelle
     */
    @PutMapping("/{tag}")
    public ResponseEntity<CodeShareResponse> saveOrUpdate(
            @PathVariable UUID teamId,
            @PathVariable String tag,
            @Valid @RequestBody CodeShareRequest request
    ) {
        return ResponseEntity.ok(codeShareService.saveOrUpdate(teamId, tag, request.data()));
    }

    /**
     * GET /api/teams/{teamId}/code-shares/{tag} — Tag'e göre getir
     */
    @GetMapping("/{tag}")
    public ResponseEntity<CodeShareResponse> getByTag(
            @PathVariable UUID teamId,
            @PathVariable String tag
    ) {
        CodeShareResponse response = codeShareService.getByTag(teamId, tag);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}

