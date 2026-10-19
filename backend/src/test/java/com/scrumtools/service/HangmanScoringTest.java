package com.scrumtools.service;

import com.scrumtools.entity.HangmanMode;
import com.scrumtools.entity.HangmanPace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HangmanScoringTest {

    @Test
    void raceScoreRewardsPlaceAndRemainingLives() {
        // 1. sıra, hiç yanlış yok: 50 + 6 can × 5
        assertEquals(80, HangmanSessionService.raceSolveScore(1, 0));
        assertEquals(40 + 4 * 5, HangmanSessionService.raceSolveScore(2, 2));
        assertEquals(30 + 5, HangmanSessionService.raceSolveScore(3, 5));
        // Dördüncüden sonrası taban puan alır
        assertEquals(20 + 6 * 5, HangmanSessionService.raceSolveScore(4, 0));
        assertEquals(20 + 6 * 5, HangmanSessionService.raceSolveScore(12, 0));
    }

    @Test
    void fasterSolverAlwaysBeatsSlowerWithSameLives() {
        for (int wrong = 0; wrong < HangmanSessionService.MAX_WRONG; wrong++) {
            assertTrue(HangmanSessionService.raceSolveScore(1, wrong) > HangmanSessionService.raceSolveScore(2, wrong));
            assertTrue(HangmanSessionService.raceSolveScore(3, wrong) > HangmanSessionService.raceSolveScore(4, wrong));
        }
    }

    @Test
    void unknownOrMissingSettingsFallBackToDefaults() {
        assertEquals(HangmanMode.TURN, HangmanMode.parse(null));
        assertEquals(HangmanMode.TURN, HangmanMode.parse("bogus"));
        assertEquals(HangmanMode.RACE, HangmanMode.parse(" race "));
        assertEquals(HangmanMode.TURN, HangmanMode.of(null));

        assertEquals(HangmanPace.NORMAL, HangmanPace.parse(""));
        assertEquals(HangmanPace.FAST, HangmanPace.parse("fast"));
        assertEquals(HangmanPace.NORMAL, HangmanPace.of(null));
    }
}
