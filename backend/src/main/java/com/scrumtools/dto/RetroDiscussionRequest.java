package com.scrumtools.dto;

import lombok.Data;

@Data
public class RetroDiscussionRequest {
    /** Start: planned discussion length in seconds */
    private Integer durationSeconds;
    /** Extend: extra time in seconds */
    private Integer additionalSeconds;
}
