package com.scrumtools.service.ci.client;

import com.scrumtools.entity.enums.CiBuildStatus;

/**
 * Çalışan/biten bir build'in sağlayıcıdaki durumu.
 *
 * @param status     sağlayıcı sonucundan map edilmiş durum
 * @param startedAt  build başlangıcı (epoch millis), bilinmiyorsa null
 * @param durationMs süre; build hâlâ çalışıyorsa 0/null gelebilir
 */
public record CiBuildInfo(CiBuildStatus status, Long startedAt, Long durationMs) {}
