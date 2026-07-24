package com.scrumtools.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Build tetikleme isteği (task deploy veya release pipeline ortak).
 *
 * @param mappingId  tetiklenecek job eşlemesi
 * @param branch     yalnız task deploy'da anlamlı — deploy edilecek branch (serbest metin)
 * @param overrides  şablon dışı ek/üzerine yazan parametreler (parametre adı → değer); opsiyonel
 */
public record CiTriggerRequest(
        UUID mappingId,
        String branch,
        Map<String, String> overrides
) {}
