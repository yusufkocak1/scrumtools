package com.scrumtools.dto;

import com.scrumtools.entity.PokerSession;

import java.util.List;

/**
 * Poker oturumu bilgisi + mevcut oylar + varsa bağlı görev.
 */
public record PokerSessionResponse(
        String teamId,
        Boolean votesVisible,
        String cardType,
        List<PokerVoteResponse> votes,
        PokerTaskInfo task
) {
    public static PokerSessionResponse from(PokerSession session, List<PokerVoteResponse> votes, PokerTaskInfo task) {
        return new PokerSessionResponse(
                session.getTeam().getId().toString(),
                session.getVotesVisible(),
                session.getCardType(),
                votes,
                task
        );
    }
}

