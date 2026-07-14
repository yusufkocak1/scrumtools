package com.scrumtools.dto;

import java.util.UUID;

/** Repo–proje eşleme isteği. */
public record ScmRepoMappingRequest(UUID connectionId, String externalId) {}
