package com.scrumtools.dto;

import com.scrumtools.entity.enums.ScmProvider;

/** Kişisel SCM hesabı bağlama isteği (PAT). baseUrl null = cloud. */
public record UserScmAccountRequest(
        ScmProvider provider,
        String baseUrl,
        String token
) {}
