package com.scrumtools.dto;

import com.scrumtools.entity.PokerVote;

/**
 * Frontend'in beklediği oy verisi: { email, displayName, vote, timestamp }
 */
public record PokerVoteResponse(
        String email,
        String displayName,
        String vote,
        Long timestamp
) {
    public static PokerVoteResponse from(PokerVote v) {
        return new PokerVoteResponse(
                v.getUserEmail(),
                v.getDisplayName(),
                v.getVote(),
                v.getTimestamp()
        );
    }
}

