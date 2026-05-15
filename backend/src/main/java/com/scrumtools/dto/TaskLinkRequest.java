package com.scrumtools.dto;

import com.scrumtools.entity.enums.LinkType;

import java.util.UUID;

public record TaskLinkRequest(
        UUID targetTaskId,
        LinkType linkType
) {}

