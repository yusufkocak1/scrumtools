package com.scrumtools.dto;

import lombok.Data;

@Data
public class RetroVoteRequest {
    /** +1 or -1 */
    private int voteValue;
}

