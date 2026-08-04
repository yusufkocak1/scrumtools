package com.scrumtools.entity.enums;

/**
 * Bir zaman serisi noktasının nereden geldiği.
 *
 * Ayrım arayüze taşınır: kurgulanmış geçmiş kesikli çizgiyle gösterilir. Ölçülen
 * değerle tahmin edilen değeri aynı çizgide birleştirmek, grafiğe bakanın
 * güvenilirlik farkını görmesini engellerdi (bkz. RICH_FILTER_PLAN.md — K13).
 */
public enum SeriesPointSource {

    /** O gün fiilen ölçüldü — gecelik iş sorguyu çalıştırıp yazdı. */
    SNAPSHOT,

    /** Geriye dönük kurgulandı — {@code task_history} değişiklikleri ters uygulanarak. */
    REPLAY
}
