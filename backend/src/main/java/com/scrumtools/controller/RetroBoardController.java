package com.scrumtools.controller;

import com.scrumtools.dto.*;
import com.scrumtools.service.RetroBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams/{teamId}/retro-boards")
@RequiredArgsConstructor
public class RetroBoardController {

    private final RetroBoardService retroBoardService;

    // ─── Boards ───────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<RetroBoardResponse>> getBoards(@PathVariable UUID teamId) {
        return ResponseEntity.ok(retroBoardService.getBoards(teamId));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<RetroBoardResponse> getBoard(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId) {
        return ResponseEntity.ok(retroBoardService.getBoard(teamId, boardId));
    }

    @PostMapping
    public ResponseEntity<RetroBoardResponse> createBoard(
            @PathVariable UUID teamId,
            @RequestBody CreateRetroBoardRequest req) {
        return ResponseEntity.ok(retroBoardService.createBoard(teamId, req));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<RetroBoardResponse> renameBoard(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(retroBoardService.renameBoard(teamId, boardId, body.get("retroBoardName")));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId) {
        retroBoardService.deleteBoard(teamId, boardId);
        return ResponseEntity.noContent().build();
    }

    // ─── Items ────────────────────────────────────────────────────────────────

    @GetMapping("/{boardId}/columns/{columnName}/items")
    public ResponseEntity<List<RetroItemResponse>> getItems(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName) {
        return ResponseEntity.ok(retroBoardService.getItems(teamId, boardId, columnName));
    }

    @PostMapping("/{boardId}/columns/{columnName}/items")
    public ResponseEntity<RetroItemResponse> createItem(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @RequestBody CreateRetroItemRequest req) {
        return ResponseEntity.ok(retroBoardService.createItem(teamId, boardId, columnName, req));
    }

    @PutMapping("/{boardId}/columns/{columnName}/items/{itemId}")
    public ResponseEntity<RetroItemResponse> updateItem(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(retroBoardService.updateItem(teamId, boardId, itemId, body.get("value")));
    }

    @PatchMapping("/{boardId}/columns/{columnName}/items/{itemId}/status")
    public ResponseEntity<RetroItemResponse> updateStatus(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(retroBoardService.updateStatus(teamId, boardId, itemId, body.get("status")));
    }

    @DeleteMapping("/{boardId}/columns/{columnName}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId) {
        retroBoardService.deleteItem(teamId, boardId, itemId);
        return ResponseEntity.noContent().build();
    }

    // ─── Discussion timer ─────────────────────────────────────────────────────

    @PostMapping("/{boardId}/columns/{columnName}/items/{itemId}/discussion")
    public ResponseEntity<RetroItemResponse> startDiscussion(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId,
            @RequestBody RetroDiscussionRequest req) {
        return ResponseEntity.ok(retroBoardService.startDiscussion(teamId, boardId, itemId, req.getDurationSeconds()));
    }

    @PatchMapping("/{boardId}/columns/{columnName}/items/{itemId}/discussion")
    public ResponseEntity<RetroItemResponse> extendDiscussion(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId,
            @RequestBody RetroDiscussionRequest req) {
        return ResponseEntity.ok(retroBoardService.extendDiscussion(teamId, boardId, itemId, req.getAdditionalSeconds()));
    }

    @DeleteMapping("/{boardId}/columns/{columnName}/items/{itemId}/discussion")
    public ResponseEntity<RetroItemResponse> stopDiscussion(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(retroBoardService.stopDiscussion(teamId, boardId, itemId));
    }

    // ─── Votes ────────────────────────────────────────────────────────────────

    @PostMapping("/{boardId}/columns/{columnName}/items/{itemId}/votes")
    public ResponseEntity<RetroItemResponse> toggleVote(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId,
            @RequestBody RetroVoteRequest req) {
        return ResponseEntity.ok(retroBoardService.toggleVote(teamId, boardId, itemId, req.getVoteValue()));
    }

    // ─── Comments ─────────────────────────────────────────────────────────────

    @GetMapping("/{boardId}/columns/{columnName}/items/{itemId}/comments")
    public ResponseEntity<List<RetroItemResponse.CommentDto>> getComments(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(retroBoardService.getComments(teamId, boardId, itemId));
    }

    @PostMapping("/{boardId}/columns/{columnName}/items/{itemId}/comments")
    public ResponseEntity<RetroItemResponse> addComment(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId,
            @RequestBody RetroCommentRequest req) {
        return ResponseEntity.ok(retroBoardService.addComment(teamId, boardId, itemId, req.getValue()));
    }

    @DeleteMapping("/{boardId}/columns/{columnName}/items/{itemId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @PathVariable String columnName,
            @PathVariable UUID itemId,
            @PathVariable UUID commentId) {
        retroBoardService.deleteComment(teamId, boardId, itemId, commentId);
        return ResponseEntity.noContent().build();
    }
}

