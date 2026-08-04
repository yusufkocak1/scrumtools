package com.scrumtools.dto;

/**
 * Masadaki bir oyuncunun diğerine fırlattığı eğlence objesi.
 *
 * Kalıcı değildir — DB'ye yazılmaz, yalnızca WebSocket üzerinden anlık yayınlanır:
 *   /topic/poker/{teamId}/throws
 *
 * item alanı ASCII bir token'dır ("heart", "arrow"...); hangi emoji ile gösterileceğine
 * frontend karar verir. Böylece emoji karakteri backend/DB kodlamasına hiç girmez.
 */
public record PokerThrowResponse(
        String fromEmail,
        String fromName,
        String toEmail,
        String item,
        Long timestamp
) {
}
