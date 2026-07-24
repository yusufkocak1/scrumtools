package com.scrumtools.dto;

import java.util.List;
import java.util.UUID;

/**
 * Task detayı Deploy bölümünün tek çağrıda tüm verisi (ScmTaskDevService.getTaskScm deseni).
 *
 * @param featureEnabled  org'un paketinde CI_CD_INTEGRATION açık mı (kapalıysa upgrade yönlendirmesi)
 * @param projectId       task'ın projesi (null = projeye bağlı değil, bölüm gizlenir)
 * @param canDeploy       kullanıcı tetikleyebilir mi (proje üyesi + en az bir enabled TASK_DEPLOY eşlemesi)
 * @param mappings        seçilebilir TASK_DEPLOY eşlemeleri (yalnız enabled)
 * @param builds          bu task'ın build tarihçesi (yeni → eski)
 */
public record CiTaskDeployView(
        boolean featureEnabled,
        UUID projectId,
        boolean canDeploy,
        List<CiJobMappingResponse> mappings,
        List<CiBuildResponse> builds
) {}
