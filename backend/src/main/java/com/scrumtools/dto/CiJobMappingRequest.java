package com.scrumtools.dto;

import com.scrumtools.entity.enums.CiEnvironment;
import com.scrumtools.entity.enums.CiJobType;

import java.util.UUID;

/**
 * Job eşlemesi ekleme/güncelleme isteği.
 * autoTransitionOnSuccess yalnız RELEASE_PIPELINE tipinde anlamlıdır.
 */
public record CiJobMappingRequest(
        UUID connectionId,
        String jobFullName,
        String displayName,
        CiJobType jobType,
        CiEnvironment environment,
        String parameterTemplate,
        Boolean autoTransitionOnSuccess,
        Boolean enabled
) {}
