package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetroBoardService {

    private final RetroBoardRepository boardRepository;
    private final RetroItemRepository itemRepository;
    private final RetroItemCommentRepository commentRepository;
    private final RetroItemVoteRepository voteRepository;
    private final TeamRepository teamRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EntitlementService entitlementService;

    // ─── Board ────────────────────────────────────────────────────────────────

    public List<RetroBoardResponse> getBoards(UUID teamId) {
        return boardRepository.findByTeamIdOrderByCreatedAtAsc(teamId)
                .stream().map(RetroBoardResponse::from).collect(Collectors.toList());
    }

    public RetroBoardResponse getBoard(UUID teamId, UUID boardId) {
        RetroBoard board = findBoard(boardId, teamId);
        return RetroBoardResponse.from(board);
    }

    @Transactional
    public RetroBoardResponse createBoard(UUID teamId, CreateRetroBoardRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        entitlementService.assertFeature(team.getOrganization(),
                com.scrumtools.entity.enums.PlanFeature.RETRO);
        RetroBoard board = RetroBoard.builder()
                .team(team)
                .retroBoardName(req.getRetroBoardName())
                .columns(req.getColumns() != null ? req.getColumns() : List.of("Start", "Stop", "Continue"))
                .build();
        return RetroBoardResponse.from(boardRepository.save(board));
    }

    /**
     * Board adını ve kolon yapısını günceller.
     *  - columnRenames: eski ad → yeni ad; item'ların columnName alanı taşınır.
     *  - columns listesinde olmayan kolonların item'ları silinir.
     * Sonunda BOARD_UPDATED WS eventi yayınlanır (columnName boş → istemci tüm kolonları yeniler).
     */
    @Transactional
    public RetroBoardResponse updateBoard(UUID teamId, UUID boardId, UpdateRetroBoardRequest req) {
        RetroBoard board = findBoard(boardId, teamId);

        if (req.getRetroBoardName() != null && !req.getRetroBoardName().isBlank()) {
            board.setRetroBoardName(req.getRetroBoardName().trim());
        }

        if (req.getColumns() != null) {
            List<String> newColumns = req.getColumns().stream()
                    .filter(c -> c != null && !c.isBlank())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            if (newColumns.isEmpty()) {
                throw new RuntimeException("Board must have at least one column");
            }

            List<RetroItem> items = itemRepository.findByRetroBoardId(boardId);

            // 1) Yeniden adlandırma: item'ları yeni kolon adına taşı
            Map<String, String> renames = req.getColumnRenames() != null ? req.getColumnRenames() : Map.of();
            for (RetroItem item : items) {
                String renamed = renames.get(item.getColumnName());
                if (renamed != null && !renamed.isBlank()) {
                    item.setColumnName(renamed.trim());
                }
            }

            // 2) Silinen kolonların item'larını kaldır, kalanları kaydet
            List<RetroItem> orphaned = items.stream()
                    .filter(i -> !newColumns.contains(i.getColumnName()))
                    .collect(Collectors.toList());
            if (!orphaned.isEmpty()) {
                board.getItems().removeAll(orphaned);
                itemRepository.deleteAll(orphaned);
            }
            items.removeAll(orphaned);
            itemRepository.saveAll(items);

            board.setColumns(newColumns);
        }

        RetroBoardResponse response = RetroBoardResponse.from(boardRepository.save(board));
        notify(teamId, boardId, "BOARD_UPDATED", null, null);
        return response;
    }

    @Transactional
    public void deleteBoard(UUID teamId, UUID boardId) {
        RetroBoard board = findBoard(boardId, teamId);
        boardRepository.delete(board);
    }

    // ─── Items ────────────────────────────────────────────────────────────────

    public List<RetroItemResponse> getItems(UUID teamId, UUID boardId, String columnName) {
        findBoard(boardId, teamId); // auth check
        return itemRepository.findByRetroBoardIdAndColumnNameOrderByCreatedAtAsc(boardId, columnName)
                .stream().map(RetroItemResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public RetroItemResponse createItem(UUID teamId, UUID boardId, String columnName, CreateRetroItemRequest req) {
        RetroBoard board = findBoard(boardId, teamId);
        String owner = req.getOwner() != null ? req.getOwner() :
                SecurityContextHolder.getContext().getAuthentication().getName();

        RetroItem item = RetroItem.builder()
                .retroBoard(board)
                .columnName(columnName)
                .value(req.getValue())
                .owner(owner)
                .build();
        RetroItem saved = itemRepository.save(item);

        notify(teamId, boardId, "ITEM_ADDED", columnName, saved.getId().toString());
        return RetroItemResponse.from(saved);
    }

    @Transactional
    public RetroItemResponse updateItem(UUID teamId, UUID boardId, UUID itemId, String newValue) {
        RetroItem item = findItem(itemId, boardId);
        item.setValue(newValue);
        RetroItem saved = itemRepository.save(item);
        notify(teamId, boardId, "ITEM_UPDATED", saved.getColumnName(), itemId.toString());
        return RetroItemResponse.from(saved);
    }

    @Transactional
    public RetroItemResponse updateStatus(UUID teamId, UUID boardId, UUID itemId, String status) {
        RetroItem item = findItem(itemId, boardId);
        item.setStatus(status);
        // Karar verildiğinde aktif tartışma zamanlayıcısı da kapanır
        boolean hadDiscussion = item.getDiscussionEndsAt() != null;
        item.setDiscussionEndsAt(null);
        item.setDiscussionDurationSeconds(null);
        RetroItem saved = itemRepository.save(item);
        if (hadDiscussion) {
            notifyDiscussion(teamId, boardId, "DISCUSSION_STOPPED", saved);
        }
        notify(teamId, boardId, "ITEM_UPDATED", saved.getColumnName(), itemId.toString());
        return RetroItemResponse.from(saved);
    }

    // ─── Discussion timer ─────────────────────────────────────────────────────

    private static final int MIN_DISCUSSION_SECONDS = 10;
    private static final int MAX_DISCUSSION_SECONDS = 2 * 60 * 60; // 2 saat

    @Transactional
    public RetroItemResponse startDiscussion(UUID teamId, UUID boardId, UUID itemId, Integer durationSeconds) {
        RetroItem item = findItem(itemId, boardId);
        int duration = clampDuration(durationSeconds);
        item.setDiscussionEndsAt(Instant.now().plusSeconds(duration));
        item.setDiscussionDurationSeconds(duration);
        RetroItem saved = itemRepository.save(item);
        notifyDiscussion(teamId, boardId, "DISCUSSION_STARTED", saved);
        return RetroItemResponse.from(saved);
    }

    @Transactional
    public RetroItemResponse extendDiscussion(UUID teamId, UUID boardId, UUID itemId, Integer additionalSeconds) {
        RetroItem item = findItem(itemId, boardId);
        int extra = clampDuration(additionalSeconds);
        Instant now = Instant.now();
        Instant endsAt = item.getDiscussionEndsAt();

        if (endsAt == null || !endsAt.isAfter(now)) {
            // Süre dolmuş (veya hiç yok) → ek süre yeni bir tur gibi baştan sayılır
            item.setDiscussionEndsAt(now.plusSeconds(extra));
            item.setDiscussionDurationSeconds(extra);
        } else {
            // Hâlâ çalışıyor → mevcut sürenin üzerine eklenir
            item.setDiscussionEndsAt(endsAt.plusSeconds(extra));
            int total = item.getDiscussionDurationSeconds() != null ? item.getDiscussionDurationSeconds() : 0;
            item.setDiscussionDurationSeconds(total + extra);
        }
        RetroItem saved = itemRepository.save(item);
        notifyDiscussion(teamId, boardId, "DISCUSSION_EXTENDED", saved);
        return RetroItemResponse.from(saved);
    }

    @Transactional
    public RetroItemResponse stopDiscussion(UUID teamId, UUID boardId, UUID itemId) {
        RetroItem item = findItem(itemId, boardId);
        item.setDiscussionEndsAt(null);
        item.setDiscussionDurationSeconds(null);
        RetroItem saved = itemRepository.save(item);
        notifyDiscussion(teamId, boardId, "DISCUSSION_STOPPED", saved);
        return RetroItemResponse.from(saved);
    }

    private int clampDuration(Integer seconds) {
        if (seconds == null) throw new RuntimeException("durationSeconds is required");
        return Math.max(MIN_DISCUSSION_SECONDS, Math.min(MAX_DISCUSSION_SECONDS, seconds));
    }

    @Transactional
    public void deleteItem(UUID teamId, UUID boardId, UUID itemId) {
        RetroItem item = findItem(itemId, boardId);
        String columnName = item.getColumnName();
        itemRepository.delete(item);
        notify(teamId, boardId, "ITEM_DELETED", columnName, itemId.toString());
    }

    // ─── Votes (toggle) ───────────────────────────────────────────────────────

    @Transactional
    public RetroItemResponse toggleVote(UUID teamId, UUID boardId, UUID itemId, int voteValue) {
        RetroItem item = findItem(itemId, boardId);
        String owner = SecurityContextHolder.getContext().getAuthentication().getName();

        voteRepository.findByRetroItemIdAndOwner(itemId, owner).ifPresentOrElse(existing -> {
            if (existing.getVoteValue() == voteValue) {
                // Same vote → toggle off (remove)
                voteRepository.delete(existing);
                item.getVotes().remove(existing);
            } else {
                // Different direction → update
                existing.setVoteValue(voteValue);
                voteRepository.save(existing);
            }
        }, () -> {
            // No existing vote → create
            RetroItemVote vote = RetroItemVote.builder()
                    .retroItem(item)
                    .owner(owner)
                    .voteValue(voteValue)
                    .build();
            voteRepository.save(vote);
            item.getVotes().add(vote);
        });

        RetroItem refreshed = itemRepository.findById(itemId).orElse(item);
        notify(teamId, boardId, "VOTE_CHANGED", refreshed.getColumnName(), itemId.toString());
        return RetroItemResponse.from(refreshed);
    }

    // ─── Comments ─────────────────────────────────────────────────────────────

    public List<RetroItemResponse.CommentDto> getComments(UUID teamId, UUID boardId, UUID itemId) {
        RetroItem item = findItem(itemId, boardId);
        return item.getComments().stream()
                .map(c -> RetroItemResponse.CommentDto.builder()
                        .id(c.getId().toString())
                        .owner(c.getOwner())
                        .value(c.getValue())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public RetroItemResponse addComment(UUID teamId, UUID boardId, UUID itemId, String value) {
        RetroItem item = findItem(itemId, boardId);
        String owner = SecurityContextHolder.getContext().getAuthentication().getName();

        RetroItemComment comment = RetroItemComment.builder()
                .retroItem(item)
                .owner(owner)
                .value(value)
                .build();
        commentRepository.save(comment);
        item.getComments().add(comment);

        notify(teamId, boardId, "COMMENT_ADDED", item.getColumnName(), itemId.toString());
        // item'ı DB'den taze çek; lazy koleksiyonları güvenli şekilde yükle
        RetroItem refreshed = itemRepository.findById(itemId).orElse(item);
        return RetroItemResponse.from(refreshed);
    }

    @Transactional
    public void deleteComment(UUID teamId, UUID boardId, UUID itemId, UUID commentId) {
        RetroItemComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        RetroItem item = comment.getRetroItem();
        commentRepository.delete(comment);
        notify(teamId, boardId, "COMMENT_DELETED", item.getColumnName(), itemId.toString());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private RetroBoard findBoard(UUID boardId, UUID teamId) {
        RetroBoard board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("RetroBoard not found"));
        if (!board.getTeam().getId().equals(teamId))
            throw new RuntimeException("Board does not belong to team");
        return board;
    }

    private RetroItem findItem(UUID itemId, UUID boardId) {
        RetroItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("RetroItem not found"));
        if (!item.getRetroBoard().getId().equals(boardId))
            throw new RuntimeException("Item does not belong to board");
        return item;
    }

    /**
     * Notification-only WebSocket mesajı gönderir.
     * Mesaj içeriği: { event, columnName, itemId, triggeredBy, timestamp }
     * Client bu mesajı alınca debounce havuzunda biriktirir ve belirli süre sonra flush eder.
     *
     * Topic: /topic/retro/{teamId}/{boardId}
     */
    private void notify(UUID teamId, UUID boardId, String event, String columnName, String itemId) {
        notify(teamId, boardId, event, columnName, itemId, Map.of());
    }

    private void notify(UUID teamId, UUID boardId, String event, String columnName, String itemId,
                        Map<String, String> extras) {
        String topic = "/topic/retro/" + teamId + "/" + boardId;
        String triggeredBy = "";
        try {
            triggeredBy = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {}

        Map<String, String> payload = new java.util.HashMap<>(extras);
        payload.put("event",       event);
        payload.put("columnName",  columnName  != null ? columnName  : "");
        payload.put("itemId",      itemId      != null ? itemId      : "");
        payload.put("triggeredBy", triggeredBy);
        payload.put("timestamp",   Instant.now().toString());
        try {
            messagingTemplate.convertAndSend(topic, payload);
        } catch (Exception e) {
            log.warn("[WS] RetroBoard notification gönderilemedi: topic={}, error={}", topic, e.getMessage());
        }
    }

    /**
     * Tartışma zamanlayıcısı olayları veri taşır (notification-only istisnası):
     * istemciler debounce/refetch beklemeden sayaç durumunu anında senkronlar.
     */
    private void notifyDiscussion(UUID teamId, UUID boardId, String event, RetroItem item) {
        Map<String, String> extras = Map.of(
                "discussionEndsAt",
                item.getDiscussionEndsAt() != null ? item.getDiscussionEndsAt().toString() : "",
                "discussionDurationSeconds",
                item.getDiscussionDurationSeconds() != null ? item.getDiscussionDurationSeconds().toString() : ""
        );
        notify(teamId, boardId, event, item.getColumnName(), item.getId().toString(), extras);
    }
}

