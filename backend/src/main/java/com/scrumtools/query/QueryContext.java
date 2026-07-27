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
 * @param now              "şimdi" — testlerde sabitlenebilsin diye dışarıdan verilir
 */
public record QueryContext(
        UUID teamId,
        UUID projectId,
        String currentUserEmail,
        SprintResolver sprintResolver,
        LocalDateTime now
) {

    /** Sprint durumuna göre id listesi döndüren kaynak. Durum: "open" | "done" | "backlog". */
    @FunctionalInterface
    public interface SprintResolver {
        List<UUID> sprintIdsByStatus(UUID teamId, String status);
    }

    public LocalDate today() {
        return now.toLocalDate();
    }

    public List<UUID> sprintIds(String status) {
        return sprintResolver == null ? List.of() : sprintResolver.sprintIdsByStatus(teamId, status);
    }
}
