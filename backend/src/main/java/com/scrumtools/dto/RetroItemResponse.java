package com.scrumtools.dto;

import com.scrumtools.entity.RetroItem;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class RetroItemResponse {
    private String id;
    private String boardId;
    private String columnName;
    private String value;
    private String status;
    private String owner;
    private int voteScore;
    private List<VoteDto> votes;
    private List<CommentDto> comments;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class VoteDto {
        private String id;
        private String owner;
        private int voteValue;
    }

    @Data
    @Builder
    public static class CommentDto {
        private String id;
        private String owner;
        private String value;
        private LocalDateTime createdAt;
    }

    public static RetroItemResponse from(RetroItem item) {
        List<VoteDto> votes = item.getVotes() == null ? List.of() :
                item.getVotes().stream()
                        .map(v -> VoteDto.builder()
                                .id(v.getId().toString())
                                .owner(v.getOwner())
                                .voteValue(v.getVoteValue())
                                .build())
                        .collect(Collectors.toList());

        int score = item.getVotes() == null ? 0 :
                item.getVotes().stream().mapToInt(v -> v.getVoteValue()).sum();

        List<CommentDto> comments = item.getComments() == null ? List.of() :
                item.getComments().stream()
                        .map(c -> CommentDto.builder()
                                .id(c.getId().toString())
                                .owner(c.getOwner())
                                .value(c.getValue())
                                .createdAt(c.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());

        return RetroItemResponse.builder()
                .id(item.getId().toString())
                .boardId(item.getRetroBoard().getId().toString())
                .columnName(item.getColumnName())
                .value(item.getValue())
                .status(item.getStatus())
                .owner(item.getOwner())
                .voteScore(score)
                .votes(votes)
                .comments(comments)
                .createdAt(item.getCreatedAt())
                .build();
    }
}

