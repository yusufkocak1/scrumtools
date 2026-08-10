package com.scrumtools.dto;

/**
 * Yüklenen quiz görselinin kimliği ve soruya gömülecek kalıcı bağlantısı.
 */
public record QuizImageResponse(
        String id,
        String url,
        String fileName
) {
}
