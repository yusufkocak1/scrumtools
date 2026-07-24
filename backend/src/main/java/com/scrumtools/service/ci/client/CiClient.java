package com.scrumtools.service.ci.client;

import java.util.List;
import java.util.Map;

/**
 * CI sağlayıcı soyutlaması — Jenkins ile başlar, GitLab CI / GitHub Actions
 * aynı arayüzü doldurarak eklenebilir. Instance'lar CiClientFactory tarafından
 * istek başına üretilir (kimlik bilgisi bağlantıdan çözülür), Spring bean'i değildir.
 */
public interface CiClient {

    /** Kimlik bilgilerini doğrular; sunucu sürümü ve crumb gereksinimini döner. */
    CiServerInfo validateConnection();

    /**
     * Tetiklenebilir job'ları listeler; folder'lar recursive taranır.
     * @param search boş/null değilse tam ad üzerinde büyük-küçük harf duyarsız filtre
     */
    List<CiJobInfo> listJobs(String search);

    /** Job'ın tanımlı build parametreleri — parametresiz job'da boş liste. */
    List<CiJobParameter> getJobParameters(String jobFullName);

    /**
     * Build'i tetikler.
     * @param parameters çözümlenmiş parametreler; boşsa parametresiz tetikleme yapılır
     * @return kuyruk kaydının URL'i (build numarası henüz yok)
     */
    String triggerBuild(String jobFullName, Map<String, String> parameters);

    /** Kuyruk kaydını sorgular — build numarası atandıysa döner. */
    CiQueueStatus getQueueStatus(String queueItemUrl);

    /** Çalışan/biten build'in durumunu sorgular. */
    CiBuildInfo getBuildInfo(String buildUrl);
}
