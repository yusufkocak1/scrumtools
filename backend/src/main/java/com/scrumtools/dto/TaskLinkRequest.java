package com.scrumtools.dto;

import com.scrumtools.entity.enums.LinkType;

public record TaskLinkRequest(
        // UUID veya customId (örn. TEAM-5) kabul edilir
        String targetTaskId,
        LinkType linkType
) {}

