package com.scrumtools.dto;

/**
 * Oyun sırasında gönderilen anlık emoji tepkisi. Saklanmaz, yalnızca yayınlanır:
 *   /topic/hangman/{teamId}/reactions
 */
public record HangmanReactionEvent(
        String sessionId,
        String email,
        String displayName,
        String emoji
) {
}
