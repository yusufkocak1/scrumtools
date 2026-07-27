package com.scrumtools.controller;

import com.scrumtools.service.ErrorTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Yalnızca uygulamanın kendi controller'larını kapsar.
 * <p>
 * basePackages ŞART: kapsamsız bırakılırsa Spring Boot'un {@code BasicErrorController}'ı
 * da sarılır. SockJS transport istekleri ({@code /ws/.../htmlfile}, {@code xhr_streaming})
 * yanıtı {@code application/javascript} olarak açar; bağlantı koptuğunda Tomcat hata
 * sayfasına dispatch eder ve {@code BasicErrorController} Map gövdesini bu preset
 * content-type ile yazamaz. O hata buradaki RuntimeException handler'ına düşünce
 * handler da aynı sebeple yazamıyor ve "Failure in @ExceptionHandler" zinciri oluşuyordu.
 */
@RestControllerAdvice(basePackages = "com.scrumtools.controller")
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorTrackingService errorTrackingService;

    // ─── Kimlik Doğrulama ─────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }

    // ─── Yetki / Güvenlik ─────────────────────────────────────────────────────

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException e) {
        log.warn("Yetki hatası: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(org.springframework.security.access.AccessDeniedException e) {
        log.warn("Erişim engellendi: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Bu işlem için yetkiniz yok."));
    }

    // ─── Kaynak Bulunamadı ────────────────────────────────────────────────────

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UsernameNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Kaynak bulunamadı."));
    }

    // ─── Geçersiz İstek / Validasyon ──────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", errors));
    }

    // ─── Gövde Dönüştürme ─────────────────────────────────────────────────────

    /** İstemcinin gönderdiği gövde okunamadı (bozuk JSON, tip uyumsuzluğu). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("İstek gövdesi okunamadı: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "İstek gövdesi geçersiz."));
    }

    /**
     * Yanıt serileştirilemedi — bu istemcinin değil bizim hatamız. Ayrıca yakalanmazsa
     * RuntimeException handler'ına düşüp 400 dönüyor ve iç hata metni istemciye sızıyordu.
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Map<String, String>> handleNotWritable(HttpMessageNotWritableException e,
                                                                 HttpServletRequest request) {
        return tracked500(e, request);
    }

    // ─── Paket Limiti (402 → frontend upgrade akışını tetikler) ───────────────

    @ExceptionHandler(com.scrumtools.exception.PlanLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handlePlanLimit(com.scrumtools.exception.PlanLimitExceededException e) {
        log.info("Paket limiti aşıldı ({}): {}", e.getLimitType(), e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of(
                        "code", "PLAN_LIMIT",
                        "limitType", e.getLimitType(),
                        "error", e.getMessage()
                ));
    }

    // ─── RuntimeException (Servis katmanından gelen iş kuralı hataları) ───────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        log.warn("İş kuralı hatası: {}", e.getMessage());
        String message = e.getMessage() != null ? e.getMessage() : "Beklenmeyen bir hata oluştu.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    // ─── Programlama Hataları (izlenen 500) ───────────────────────────────────

    /**
     * NPE ve benzeri programlama hataları RuntimeException handler'ına (400)
     * düşmesin diye ayrıca yakalanır — Spring "en spesifik kazanır" kuralıyla
     * bu handler önceliklidir. Bunlar gerçek çökmelerdir, izlenen 500 döner.
     */
    @ExceptionHandler({
            NullPointerException.class,
            ClassCastException.class,
            IndexOutOfBoundsException.class,
            UnsupportedOperationException.class,
            ConcurrentModificationException.class,
            ArithmeticException.class
    })
    public ResponseEntity<Map<String, String>> handleProgrammingError(RuntimeException e, HttpServletRequest request) {
        return tracked500(e, request);
    }

    // ─── Genel (Yakalanmayan tüm hatalar) ─────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e, HttpServletRequest request) {
        return tracked500(e, request);
    }

    /**
     * İzlenen 500 yanıtı: hata loglanır, ErrorTrackingService ile fingerprint
     * bazlı gruplanıp takip numarası üretilir ve yanıtta döndürülür.
     * Kayıt başarısız olsa bile yanıt bozulmaz — trackingCode sadece atlanır.
     */
    private ResponseEntity<Map<String, String>> tracked500(Exception e, HttpServletRequest request) {
        log.error("Beklenmeyen hata: ", e);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "Sunucu hatası. Lütfen tekrar deneyin.");
        try {
            String trackingCode = errorTrackingService.recordBackendError(
                    e,
                    currentUserEmailOrNull(),
                    request != null ? request.getRequestURI() : null,
                    request != null ? request.getMethod() : null,
                    request != null ? request.getHeader("User-Agent") : null);
            if (trackingCode != null) {
                body.put("trackingCode", trackingCode);
            }
        } catch (Exception trackingFailure) {
            log.error("Hata kaydı oluşturulamadı: {}", trackingFailure.getMessage(), trackingFailure);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String currentUserEmailOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        return "anonymousUser".equals(name) ? null : name;
    }
}

