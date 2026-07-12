package com.scrumtools.dto;

import com.scrumtools.entity.SupportTicketMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SupportMessageResponse {

    private String id;
    private String authorEmail;
    private String authorName;
    private Boolean isAdminReply;
    private String content;
    private LocalDateTime createdAt;

    public static SupportMessageResponse from(SupportTicketMessage m) {
        return SupportMessageResponse.builder()
                .id(m.getId().toString())
                .authorEmail(m.getAuthorEmail())
                .authorName(m.getAuthorName())
                .isAdminReply(m.getIsAdminReply())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
