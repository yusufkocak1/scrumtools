package com.scrumtools.controller;

import com.scrumtools.service.collab.macro.MacroHttpProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * {@code ScrumTools.http.fetch} vekili (COLLAB_WORKSPACE_PLAN.md §9.1 / §9.2).
 *
 * <p>Makro doğrudan ağa çıkamaz — worker'da {@code fetch} silinmiştir. Dış
 * çağrılar buradan geçer ki alan adı allowlist'i ve SSRF koruması tek bir yerde
 * uygulansın; tarayıcıdan serbest {@code fetch} bırakmak, kullanıcının oturum
 * çerezleriyle iç ağa istek atan bir makro demekti.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/collab/macros/http")
@RequiredArgsConstructor
public class CollabMacroHttpProxyController {

    private final MacroHttpProxyService proxyService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> fetch(
            @PathVariable UUID projectId,
            @RequestBody MacroHttpProxyService.ProxyRequest request) {
        return ResponseEntity.ok(proxyService.fetch(projectId, request));
    }
}
