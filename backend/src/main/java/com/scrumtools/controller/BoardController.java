package com.scrumtools.controller;

import com.scrumtools.dto.BoardRequest;
import com.scrumtools.dto.BoardResponse;
import com.scrumtools.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Faz 4 — Board CRUD endpoint'leri
 * Base: /api/teams/{teamId}/boards
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/{teamId}/boards")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<List<BoardResponse>> getBoards(@PathVariable UUID teamId) {
        return ResponseEntity.ok(boardService.getBoards(teamId));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId
    ) {
        return ResponseEntity.ok(boardService.getBoard(teamId, boardId));
    }

    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(
            @PathVariable UUID teamId,
            @RequestBody BoardRequest req
    ) {
        return ResponseEntity.ok(boardService.createBoard(teamId, req));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<BoardResponse> updateBoard(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId,
            @RequestBody BoardRequest req
    ) {
        return ResponseEntity.ok(boardService.updateBoard(teamId, boardId, req));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable UUID teamId,
            @PathVariable UUID boardId
    ) {
        boardService.deleteBoard(teamId, boardId);
        return ResponseEntity.noContent().build();
    }
}

