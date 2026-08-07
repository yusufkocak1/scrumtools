package com.scrumtools.dto;

import com.scrumtools.entity.enums.MacroRunStatus;
import jakarta.validation.constraints.NotNull;

/**
 * İstemcinin çalıştırma sonrası gönderdiği rapor (COLLAB_WORKSPACE_PLAN.md K8).
 *
 * <p>Yürütme tarayıcıda olduğu için sonucu sunucu gözlemleyemez; rapor
 * istemciden gelir. Bu, denetimin sınırını da belirler: kayıt "kullanıcının
 * tarayıcısı ne bildirdi"dir, "sunucu ne doğruladı" değil. Yine de değerlidir —
 * çalıştırma <b>izni</b> sunucuda verilir ve o karar her hâlükârda kaydedilir.
 */
public record CollabMacroRunReport(

        @NotNull
        MacroRunStatus status,

        Long durationMs,

        String log,

        String error,

        /** Dokümana yazdı mı — aktivite akışına düşüp düşmeyeceğini belirler. */
        Boolean wroteDocument
) {
}
