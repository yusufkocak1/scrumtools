package com.scrumtools.dto;

import java.util.List;

/**
 * Takım modunda sırası gelen takımın bir harfe verdiği oylar.
 * Takım ne seçeceğini birlikte tartışsın diye açık yayınlanır.
 */
public record HangmanTeamVoteTally(
        String letter,
        int count,
        List<String> voters
) {
}
