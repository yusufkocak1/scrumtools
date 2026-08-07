package com.scrumtools.dto;

/**
 * Dokümanda o an açık olan bir katılımcı (COLLAB_WORKSPACE_PLAN.md §3).
 *
 * <p>Kalıcı değildir; yalnızca {@code /topic/collab/{id}/presence} yayınında
 * taşınır. İmleç/seçim konumu burada yoktur — o, ham WS üzerindeki awareness
 * kanalından akar ve saniyede onlarca kez değişir.
 */
public record CollabParticipantResponse(
        String email,
        String name,
        String color,
        boolean canWrite
) {
}
