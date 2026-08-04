package com.scrumtools.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Zengin filtre çalıştırma isteği: <b>seçimler</b> + çalıştırma parametreleri.
 *
 * İstemci hiçbir zaman ham sorgu göndermez; yalnız hangi öğelerin seçildiğini
 * bildirir, sunucu bunları kendi kayıtlarından çözer. Böylece paylaşılan bir
 * dashboard linki kapsam aşamaz ve seçim durumu URL'ye sığacak kadar küçük kalır
 * (bkz. RICH_FILTER_PLAN.md — K2).
 */
@Data
public class RichFilterRuntimeRequest {

    /** Kapsam projesi; verilmezse zengin filtrenin kendi projesi geçerlidir. */
    private UUID projectId;

    /** Seçili akıllı filtre id'leri — aralarında OR. */
    private List<UUID> smart;

    /** Arama kutusundaki metin: başlıkta veya görev numarasında aranır. */
    private String text;

    /** Sabit filtre seçimleri: öğe id → seçenek id. (Faz 4) */
    private Map<UUID, String> staticSelections;

    /** Dinamik filtre seçimleri: öğe id → değerler. (Faz 4) */
    private Map<UUID, List<String>> dynamic;

    // ─── Çalıştırma parametreleri ────────────────────────────────────────────

    private Integer page;
    private Integer size;

    /** Gruplama alanı; verilmezse zengin filtrenin kendi akıllı filtreleri. */
    private String groupBy;

    /** İkinci gruplama ekseni — yalnız ısı haritasında, zorunlu (Faz 7). */
    private String splitBy;

    private String metric;
    private Integer limit;

    /** Sınıflandırılacak görev id'leri — board renklendirmesi (Faz 7). */
    private List<UUID> taskIds;
}
