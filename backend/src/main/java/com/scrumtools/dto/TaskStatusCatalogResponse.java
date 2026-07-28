package com.scrumtools.dto;

import com.scrumtools.service.workflow.TaskStatusCatalog;

import java.util.List;
import java.util.UUID;

/**
 * Arayüzün durum kaydı (status registry) yanıtı.
 *
 * Frontend'deki her durum tüketicisi (board kolonları, görev formu, filtre
 * önerileri, gruplama sırası) bu tek listeden beslenir.
 *
 * @param workflowId   düzenleme ekranının hedef aldığı workflow
 * @param scope        "PROJECT" (proje override'ı) veya "TEAM"
 * @param statuses     sıralı durum listesi
 * @param unmapped     görevlerde geçip katalogda olmayan durum adları
 */
public record TaskStatusCatalogResponse(
        UUID workflowId,
        String workflowName,
        String scope,
        List<TaskStatusCatalog.StatusView> statuses,
        List<String> unmapped
) {
    public static TaskStatusCatalogResponse of(TaskStatusCatalog catalog, String scope, List<String> unmapped) {
        return new TaskStatusCatalogResponse(
                catalog.workflowId(), catalog.workflowName(), scope, catalog.statuses(), unmapped);
    }
}
