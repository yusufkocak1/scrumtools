package com.scrumtools.dto;

import com.scrumtools.entity.ErrorGroup;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorGroupResponse {

    private String id;
    private String source;
    private String exceptionType;
    private String location;
    private String sampleMessage;
    private String status;
    private Long occurrenceCount;
    private Long affectedUserCount;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;

    public static ErrorGroupResponse from(ErrorGroup g, long affectedUserCount) {
        return ErrorGroupResponse.builder()
                .id(g.getId().toString())
                .source(g.getSource().name())
                .exceptionType(g.getExceptionType())
                .location(g.getLocation())
                .sampleMessage(g.getSampleMessage())
                .status(g.getStatus().name())
                .occurrenceCount(g.getOccurrenceCount())
                .affectedUserCount(affectedUserCount)
                .firstSeenAt(g.getFirstSeenAt())
                .lastSeenAt(g.getLastSeenAt())
                .resolvedAt(g.getResolvedAt())
                .resolvedBy(g.getResolvedBy())
                .build();
    }
}
