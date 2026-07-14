package com.scrumtools.controller;

import com.scrumtools.dto.UserScmAccountRequest;
import com.scrumtools.dto.UserScmAccountResponse;
import com.scrumtools.service.scm.UserScmAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Kullanıcının kendi SCM hesapları (kişisel PAT) — sadece hesap sahibi erişir. */
@RestController
@RequestMapping("/api/users/me/scm-accounts")
@RequiredArgsConstructor
public class UserScmAccountController {

    private final UserScmAccountService accountService;

    @GetMapping
    public ResponseEntity<List<UserScmAccountResponse>> list() {
        return ResponseEntity.ok(accountService.list(currentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<UserScmAccountResponse> connect(@RequestBody UserScmAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.connect(currentUserEmail(), request));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(@PathVariable UUID accountId) {
        accountService.delete(currentUserEmail(), accountId);
        return ResponseEntity.noContent().build();
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
