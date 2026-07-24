package com.scrumtools.dto;

import com.scrumtools.entity.CiJobMapping;

import java.time.LocalDateTime;
import java.util.UUID;

/** Job eşlemesi yanıtı — bağlantı adı/durumu da gömülür (liste ekranında gösterilir). */
public record CiJobMappingResponse(
        UUID id,
        UUID connectionId,
        String connectionName,
        String connectionStatus,
        UUID projectId,
        String jobFullName,
        String displayName,
        String jobType,
        String environment,
        String parameterTemplate,
        boolean autoTransitionOnSuccess,
        boolean enabled,
        String createdBy,
        LocalDateTime createdAt
) {
    public static CiJobMappingResponse from(CiJobMapping m) {
        return new CiJobMappingResponse(
                m.getId(),
                m.getConnection().getId(),
                m.getConnection().getName(),
                m.getConnection().getStatus().name(),
                m.getProject().getId(),
                m.getJobFullName(),
                m.getDisplayName(),
                m.getJobType().name(),
                m.getEnvironment().name(),
                m.getParameterTemplate(),
                Boolean.TRUE.equals(m.getAutoTransitionOnSuccess()),
                Boolean.TRUE.equals(m.getEnabled()),
                m.getCreatedBy(),
                m.getCreatedAt()
        );
    }
}
