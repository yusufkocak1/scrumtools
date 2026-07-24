package com.scrumtools.service.ci.client;

/**
 * Kuyruk kaydının o anki durumu. Build henüz başlamadıysa buildNumber null'dır
 * ve poller beklemeye devam eder.
 *
 * @param buildNumber çözümlenen build numarası, henüz yoksa null
 * @param buildUrl    çözümlenen build linki, henüz yoksa null
 * @param cancelled   kuyruktayken iptal edildi mi
 * @param reason      neden beklediği ("Waiting for next available executor") — UI'da gösterilir
 */
public record CiQueueStatus(Integer buildNumber, String buildUrl, boolean cancelled, String reason) {

    public boolean isResolved() {
        return buildNumber != null;
    }
}
