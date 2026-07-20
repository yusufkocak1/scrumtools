package com.scrumtools.dto;

import java.util.List;

public record HangmanWordBulkResponse(
        List<HangmanWordResponse> words,
        int addedCount,
        int duplicateCount,
        List<String> invalidWords
) {}
