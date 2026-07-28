package com.scrumtools.service;

import com.scrumtools.dto.BoardRequest;
import com.scrumtools.dto.BoardResponse;
import com.scrumtools.entity.Board;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.Team;
import com.scrumtools.entity.enums.BoardType;
import com.scrumtools.repository.BoardRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.TeamRepository;
import com.scrumtools.service.workflow.TaskStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final TaskStatusService taskStatusService;

    // ─── Listeleme ────────────────────────────────────────────────────────────

    public List<BoardResponse> getBoards(UUID teamId) {
        return boardRepository.findByTeamId(teamId)
                .stream()
                .map(BoardResponse::from)
                .collect(Collectors.toList());
    }

    public BoardResponse getBoard(UUID teamId, UUID boardId) {
        Board board = findBoard(teamId, boardId);
        return BoardResponse.from(board);
    }

    // ─── Oluşturma ────────────────────────────────────────────────────────────

    @Transactional
    public BoardResponse createBoard(UUID teamId, BoardRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));

        // İlk board ise varsayılan olarak işaretle
        boolean isFirst = boardRepository.findByTeamId(teamId).isEmpty();

        BoardType type = req.getBoardType() != null ? req.getBoardType() : BoardType.KANBAN;

        Board board = Board.builder()
                .team(team)
                .name(req.getName() != null ? req.getName() : "Board")
                .boardType(type)
                .columnConfig(req.getColumnConfig() != null ? req.getColumnConfig()
                        : defaultColumnConfig(teamId, req.getProjectId()))
                .swimlaneConfig(req.getSwimlaneConfig() != null ? req.getSwimlaneConfig() : defaultSwimlaneConfig())
                .cardConfig(req.getCardConfig() != null ? req.getCardConfig() : defaultCardConfig())
                .isDefault(req.getIsDefault() != null ? req.getIsDefault() : isFirst)
                .build();

        // Proje ilişkisi
        if (req.getProjectId() != null) {
            Project project = projectRepository.findById(req.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found: " + req.getProjectId()));
            board.setProject(project);
        }

        return BoardResponse.from(boardRepository.save(board));
    }

    // ─── Güncelleme ───────────────────────────────────────────────────────────

    @Transactional
    public BoardResponse updateBoard(UUID teamId, UUID boardId, BoardRequest req) {
        Board board = findBoard(teamId, boardId);

        if (req.getName() != null) board.setName(req.getName());
        if (req.getBoardType() != null) board.setBoardType(req.getBoardType());
        if (req.getColumnConfig() != null) board.setColumnConfig(req.getColumnConfig());
        if (req.getSwimlaneConfig() != null) board.setSwimlaneConfig(req.getSwimlaneConfig());
        if (req.getCardConfig() != null) board.setCardConfig(req.getCardConfig());
        if (req.getIsDefault() != null && req.getIsDefault()) {
            // Eski varsayılanı kaldır
            boardRepository.findByTeamIdAndIsDefaultTrue(teamId)
                    .ifPresent(old -> { old.setIsDefault(false); boardRepository.save(old); });
            board.setIsDefault(true);
        }

        return BoardResponse.from(boardRepository.save(board));
    }

    // ─── Silme ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteBoard(UUID teamId, UUID boardId) {
        Board board = findBoard(teamId, boardId);
        boardRepository.delete(board);
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private Board findBoard(UUID teamId, UUID boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found: " + boardId));
        if (!board.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Board does not belong to team");
        }
        return board;
    }

    /**
     * Yeni board'un başlangıç sütunları: takımın iş akışındaki her durum için bir
     * sütun. Sütunlar durum adına değil {@code statuses} listesine bağlıdır —
     * kullanıcı sonradan sütunları birleştirebilir (ör. "In Review" + "Testing"
     * tek "Doğrulama" sütununda) ya da bir durumu board dışında bırakabilir.
     *
     * İptal durumları varsayılan olarak sütuna alınmaz; board yürüyen işi
     * gösterir, vazgeçilen işi değil.
     */
    private Map<String, Object> defaultColumnConfig(UUID teamId, UUID projectId) {
        List<Map<String, Object>> columns = taskStatusService.getCatalog(teamId, projectId)
                .statuses().stream()
                .filter(s -> !Boolean.TRUE.equals(s.isCancellation()))
                .map(s -> Map.<String, Object>of(
                        "name", s.name(),
                        "color", s.color() != null ? s.color() : "#6B7280",
                        "wipLimit", 0,
                        "statuses", List.of(s.name())))
                .toList();

        if (columns.isEmpty()) {
            columns = List.of(
                    Map.<String, Object>of("name", "To Do", "color", "#6B7280", "wipLimit", 0,
                            "statuses", List.of("To Do")),
                    Map.<String, Object>of("name", "In Progress", "color", "#3B82F6", "wipLimit", 3,
                            "statuses", List.of("In Progress")),
                    Map.<String, Object>of("name", "Done", "color", "#10B981", "wipLimit", 0,
                            "statuses", List.of("Done"))
            );
        }
        return Map.of("columns", columns);
    }

    private Map<String, Object> defaultSwimlaneConfig() {
        return Map.of("enabled", false, "groupBy", "none");
    }

    private Map<String, Object> defaultCardConfig() {
        return Map.of(
                "showPriority",     true,
                "showAssignee",     true,
                "showLabels",       true,
                "showDueDate",      true,
                "showStoryPoints",  true,
                "showSubtaskCount", true
        );
    }
}
