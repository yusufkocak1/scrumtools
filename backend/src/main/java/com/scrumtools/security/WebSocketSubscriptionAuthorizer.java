package com.scrumtools.security;

import com.scrumtools.repository.TeamMemberRepository;
import com.scrumtools.service.collab.CollabDocumentAccessResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * STOMP abonelik (SUBSCRIBE) yetkilendirmesi — COLLAB_WORKSPACE_PLAN.md E3.
 *
 * <p><b>Kapatılan açık:</b> {@code WebSocketConfig} yalnızca CONNECT'te JWT
 * doğruluyordu. Kimliği doğrulanmış herhangi bir kullanıcı {@code /topic/...}
 * altındaki <i>her</i> hedefe abone olabiliyordu — başka takımların retro
 * panoları, poker oyları ve başka kullanıcıların bildirimleri dahil. Broker
 * süreç-içi ve hedefler tahmin edilebilir ({@code /topic/poker/{teamId}/votes})
 * olduğu için sömürülmesi yalnızca bir takım UUID'si bilmeyi gerektiriyordu.
 *
 * <p><b>Politika: bilinmeyen hedef reddedilir.</b> Yeni bir yayın kanalı
 * eklendiğinde buraya da bir kural eklenmelidir; aksi hâlde abonelik sessizce
 * boş döner. Yanlış tarafa düşmek gerekiyorsa kapalı tarafa düşmesi tercih
 * edilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSubscriptionAuthorizer {

    private static final String TOPIC_PREFIX = "/topic/";

    private final TeamMemberRepository teamMemberRepository;
    private final CollabDocumentAccessResolver collabAccessResolver;

    /**
     * @param destination SUBSCRIBE çerçevesindeki hedef
     * @param email       CONNECT'te doğrulanmış JWT'den gelen kullanıcı (principal adı)
     * @return abonelik kurulabilir mi
     */
    public boolean isAuthorized(String destination, String email) {
        if (destination == null || email == null || email.isBlank()) {
            return false;
        }
        if (!destination.startsWith(TOPIC_PREFIX)) {
            // Broker yalnızca /topic ile yapılandırıldı; başka bir prefix
            // kullanılmışsa hedef zaten teslim edilemez.
            return false;
        }

        String[] parts = destination.substring(TOPIC_PREFIX.length()).split("/");
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return false;
        }
        String module = parts[0];
        String scopeId = parts[1];

        return switch (module) {
            // Takım kapsamlı yayınlar: /topic/retro/{teamId}/{boardId},
            // /topic/poker|quiz|hangman/{teamId}/..., /topic/team/{teamId}/activity
            case "retro", "poker", "quiz", "hangman", "team" -> isTeamMember(scopeId, email);

            // /topic/user/{email}/notifications — yalnızca sahibi
            case "user" -> email.equalsIgnoreCase(scopeId);

            // /topic/collab/{documentId}/... — doküman yetkisi (Faz 1)
            case "collab" -> canReadCollabDocument(scopeId, email);

            default -> false;
        };
    }

    private boolean isTeamMember(String teamId, String email) {
        UUID id = parseUuid(teamId);
        return id != null && teamMemberRepository.existsByTeamIdAndEmail(id, email);
    }

    private boolean canReadCollabDocument(String documentId, String email) {
        UUID id = parseUuid(documentId);
        return id != null && collabAccessResolver.canRead(id, email);
    }

    /** Hedefteki kimlik UUID değilse yetkilendirilecek bir kaynak da yoktur. */
    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
