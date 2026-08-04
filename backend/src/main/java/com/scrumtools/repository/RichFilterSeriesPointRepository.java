package com.scrumtools.repository;

import com.scrumtools.entity.RichFilterSeriesPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RichFilterSeriesPointRepository extends JpaRepository<RichFilterSeriesPoint, UUID> {

    Optional<RichFilterSeriesPoint> findByElementIdAndBucketDate(UUID elementId, LocalDate bucketDate);

    List<RichFilterSeriesPoint> findByElementIdAndBucketDateBetweenOrderByBucketDateAsc(
            UUID elementId, LocalDate from, LocalDate to);

    /** Kurgu öncesi tarih listesi — var olan noktaların üzerine yazılmaz. */
    @Query("SELECT p.bucketDate FROM RichFilterSeriesPoint p " +
           "WHERE p.element.id = :elementId AND p.bucketDate BETWEEN :from AND :to")
    List<LocalDate> findDates(@Param("elementId") UUID elementId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    long countByElementId(UUID elementId);

    /**
     * Kurgulanmış noktaları siler — yeniden kurgu, eskisini bırakmaz.
     * Ölçülmüş (SNAPSHOT) noktalara dokunulmaz: onlar gerçekten o gün sayıldı.
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RichFilterSeriesPoint p WHERE p.element.id = :elementId " +
           "AND p.source = com.scrumtools.entity.enums.SeriesPointSource.REPLAY")
    void deleteReplayedPoints(@Param("elementId") UUID elementId);

    void deleteByElementId(UUID elementId);

    /**
     * Zengin filtre silinirken noktaları da gider.
     * Öğeler {@code cascade = ALL} ile siliniyor ama noktalar öğeye dışarıdan
     * bağlı: temizlenmezse yabancı anahtar kısıtı silmeyi engellerdi.
     */
    void deleteByElementRichFilterId(UUID richFilterId);
}
