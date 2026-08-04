package com.scrumtools.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Bir sorgunun çalıştırıldığı bağlam — {@code currentUser()}, {@code currentSprint()}
 * gibi fonksiyonlar bu bilgilerden çözülür.
 *
 * @param teamId           kapsam kısıtı: sonuçlar her zaman bu takıma daraltılır
 * @param projectId        aktif proje kapsamı; null ise takımın tüm projeleri
 * @param currentUserEmail oturumdaki kullanıcı — currentUser() bunu döndürür
 * @param sprintResolver   sprint durumundan UUID listesi çözen kaynak (repository köprüsü)
 * @param smartResolver    zengin filtre adından akıllı filtre kurallarını çözen kaynak
 * @param now              "şimdi" — testlerde sabitlenebilsin diye dışarıdan verilir
 */
public record QueryContext(
        UUID teamId,
        UUID projectId,
        String currentUserEmail,
        SprintResolver sprintResolver,
        SmartFilterResolver smartResolver,
        LocalDateTime now
) {

    /** Sprint durumuna göre id listesi döndüren kaynak. Durum: "open" | "done" | "backlog". */
    @FunctionalInterface
    public interface SprintResolver {
        List<UUID> sprintIdsByStatus(UUID teamId, String status);
    }

    /**
     * {@code smart["ad"]} yazımının çözüm kaynağı — {@link SprintResolver} ile aynı
     * köprü deseni: sorgu paketi repository'lere doğrudan bağlanmaz.
     *
     * Zengin filtre yoksa ya da kullanıcının erişimi yoksa {@code null} döner; ikisi
     * ayırt edilmez, "var ama senin değil" bilgisi kendi başına bir sızıntıdır.
     * Zengin filtre var ama akıllı filtresi yoksa boş liste döner.
     */
    @FunctionalInterface
    public interface SmartFilterResolver {
        List<SmartClause> clausesOf(UUID teamId, String richFilterName);
    }

    public LocalDate today() {
        return now.toLocalDate();
    }

    public List<UUID> sprintIds(String status) {
        return sprintResolver == null ? List.of() : sprintResolver.sprintIdsByStatus(teamId, status);
    }

    /** Bir zengin filtrenin sıralı akıllı filtreleri; çözülemezse null. */
    public List<SmartClause> smartClauses(String richFilterName) {
        return smartResolver == null ? null : smartResolver.clausesOf(teamId, richFilterName);
    }
}
