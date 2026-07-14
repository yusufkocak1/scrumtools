package com.scrumtools.service.scm;

import java.util.regex.Pattern;

/**
 * Git branch adı doğrulaması (git check-ref-format kurallarının pratik alt kümesi).
 * Sağlayıcıya geçersiz ad göndermeden anlaşılır Türkçe hata üretmek için kullanılır.
 */
public final class ScmBranchNames {

    private static final int MAX_LENGTH = 200;

    /** İzinli karakterler: harf/rakam ve / . _ - */
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9./_-]+$");

    private ScmBranchNames() {}

    /** Geçersizse IllegalArgumentException fırlatır; geçerliyse kırpılmış adı döner. */
    public static String validate(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch adı zorunlu.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Branch adı en fazla " + MAX_LENGTH + " karakter olabilir.");
        }
        if (!ALLOWED.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Branch adı sadece harf, rakam ve / . _ - karakterlerini içerebilir (boşluk ve Türkçe karakter kullanılamaz).");
        }
        if (trimmed.startsWith("/") || trimmed.endsWith("/") || trimmed.contains("//")
                || trimmed.startsWith("-") || trimmed.startsWith(".")
                || trimmed.endsWith(".") || trimmed.contains("..")
                || trimmed.endsWith(".lock") || trimmed.contains("@{")) {
            throw new IllegalArgumentException("Geçersiz branch adı: " + trimmed);
        }
        return trimmed;
    }
}
