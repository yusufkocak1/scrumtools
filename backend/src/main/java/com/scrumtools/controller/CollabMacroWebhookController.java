package com.scrumtools.controller;

import com.scrumtools.entity.enums.MacroRunStatus;
import com.scrumtools.service.collab.macro.CollabServerMacroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Makro webhook tetikleyicisi (COLLAB_WORKSPACE_PLAN.md Faz 5).
 *
 * <p><b>Kimlik doğrulamasız bir uç, bilerek.</b> Çağıran yabancı bir sistem —
 * CI sunucusu, form aracı, ödeme sağlayıcı — ve ScrumTools oturumu yoktur.
 * Kimlik, gövdenin HMAC-SHA256'sıyla kanıtlanır ({@code X-ScrumTools-Signature}).
 * Bu yüzden yol {@code /api/webhooks/**} altında: {@code SecurityConfig} orayı
 * zaten JWT süzgecinin dışında tutuyor.
 *
 * <p><b>Neden her hata aynı cevabı veriyor:</b> "makro yok" ile "imza yanlış"
 * arasında ayrım yapmak, sırrı bilmeyen birine hangi kimliklerin var olduğunu
 * söylerdi. Uç noktanın dışarıya verdiği tek bilgi, isteğin kabul edilip
 * edilmediğidir.
 */
@RestController
@RequestMapping("/api/webhooks/collab-macros")
@RequiredArgsConstructor
@Slf4j
public class CollabMacroWebhookController {

    private final CollabServerMacroService serverMacroService;

    /**
     * @param rawBody imza <b>bu ham gövde</b> üzerinden doğrulanır; Spring'in
     *                nesneye çevirdiği bir DTO'dan yeniden üretilen JSON, alan
     *                sırası değiştiği için farklı bir imza verirdi
     */
    @PostMapping("/{macroId}")
    public ResponseEntity<Map<String, Object>> trigger(
            @PathVariable UUID macroId,
            @RequestHeader(value = "X-ScrumTools-Signature", required = false) String signature,
            @RequestBody(required = false) String rawBody) {

        try {
            MacroRunStatus status = serverMacroService.triggerByWebhook(macroId, signature, rawBody);
            // Makro çalıştı ama içeride hata verdiyse bu, çağıranın hatası değil:
            // 202 dönüp durumu gövdede bildiriyoruz. Webhook gönderen sistemler
            // 5xx'i yeniden dener; başarısız bir betiği tekrar tekrar koşturmak
            // dar sunucuda (D3) işleri kötüleştirmekten başka bir şey yapmaz.
            return ResponseEntity.accepted().body(Map.of(
                    "accepted", true,
                    "status", status.name()));

        } catch (SecurityException e) {
            log.warn("Makro webhook'u reddedildi: macroId={} — {}", macroId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("accepted", false, "error", "Reddedildi."));

        } catch (IllegalArgumentException e) {
            // Bilinmeyen makro da aynı cevabı alır (bkz. sınıf notu).
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("accepted", false, "error", "Reddedildi."));

        } catch (IllegalStateException e) {
            // Kuyruk dolu ya da zaman aşımı — burada 503 doğru cevap, çünkü
            // isteğin kendisinde bir sorun yok, sunucu şu an alamıyor.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("accepted", false, "error", e.getMessage()));
        }
    }
}
