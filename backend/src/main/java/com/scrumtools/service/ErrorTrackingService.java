package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.ErrorEvent;
import com.scrumtools.entity.ErrorGroup;
import com.scrumtools.entity.enums.ErrorGroupStatus;
import com.scrumtools.entity.enums.ErrorSource;
import com.scrumtools.repository.ErrorEventRepository;
import com.scrumtools.repository.ErrorGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hata takip servisi — hataları fingerprint ile gruplar, her olaya kullanıcıya
 * gösterilen benzersiz bir takip numarası (ERR-XXXXXXXX) üretir.
 *
 * Kayıt metodlarında bilinçli olarak dış transaction YOKTUR: her repository
 * çağrısı kendi transaction'ında commit olur. Böylece eşzamanlı grup yaratma
 * yarışında (unique fingerprint ihlali) başarısız save tek başına rollback olur
 * ve grup yeniden okunabilir; ayrıca patlayan isteğin rollback'e giden
 * transaction'ından da etkilenilmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorTrackingService {

    private final ErrorGroupRepository groupRepository;
    private final ErrorEventRepository eventRepository;

    /** Takip kodu alfabesi — karışan karakterler yok (0/O, 1/I/L) */
    private static final String CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_STACK_LENGTH = 20000;

    /** Frontend raporları için kullanıcı başına dakikalık limit */
    private static final int REPORT_RATE_LIMIT_PER_MINUTE = 10;
    private final Map<String, RateWindow> rateWindows = new ConcurrentHashMap<>();

    // ─── Kayıt ────────────────────────────────────────────────────────────────

    /**
     * Backend (5xx) hatasını kaydeder ve takip numarasını döndürür.
     * GlobalExceptionHandler'dan çağrılır; asla exception fırlatmamalıdır
     * (çağıran taraf yine de try/catch ile sarar).
     */
    public String recordBackendError(Throwable t, String userEmail,
                                     String endpoint, String httpMethod, String userAgent) {
        Throwable root = rootCause(t);
        String exceptionType = root.getClass().getName();
        String location = firstAppFrame(root);
        String fingerprint = sha256("BACKEND|" + exceptionType + "|" + location);
        String message = truncate(root.getMessage(), MAX_MESSAGE_LENGTH);
        String stack = truncate(stackTraceOf(t), MAX_STACK_LENGTH);

        return recordEvent(ErrorSource.BACKEND, fingerprint, exceptionType, location,
                message, stack, userEmail, endpoint, httpMethod, userAgent);
    }

    /**
     * Frontend hata raporunu kaydeder. Rate limit aşımında null döner
     * (istemci sessizce yoksayar, retry döngüsü oluşmaz).
     */
    public String recordFrontendError(ErrorReportRequest req, String userEmail) {
        if (!allowReport(userEmail)) {
            log.debug("[ErrorTracking] Rate limit aşıldı: {}", userEmail);
            return null;
        }

        String errorName = req.getErrorName() != null ? req.getErrorName() : "Error";
        String normalizedMessage = normalizeMessage(req.getMessage());
        String topFrame = topStackFrame(req.getStack());
        String fingerprint = sha256("FRONTEND|" + errorName + "|" + normalizedMessage + "|" + topFrame);
        String message = truncate(req.getMessage(), MAX_MESSAGE_LENGTH);
        String stack = truncate(req.getStack(), MAX_STACK_LENGTH);
        String location = truncate(topFrame.isBlank() ? req.getComponentName() : topFrame, 512);

        return recordEvent(ErrorSource.FRONTEND, fingerprint, errorName, location,
                message, stack, userEmail, truncate(req.getUrl(), 1024), null,
                truncate(req.getUserAgent(), 512));
    }

    private String recordEvent(ErrorSource source, String fingerprint, String exceptionType,
                               String location, String message, String stack, String userEmail,
                               String endpoint, String httpMethod, String userAgent) {
        LocalDateTime now = LocalDateTime.now();
        ErrorGroup group = findOrCreateGroup(source, fingerprint, exceptionType, location, message, now);

        groupRepository.incrementOccurrence(group.getId(), now);

        // Çözülmüş grup tekrar oluştuysa yeniden açılır; IGNORED olduğu gibi kalır
        if (group.getStatus() == ErrorGroupStatus.RESOLVED) {
            group.setStatus(ErrorGroupStatus.REOPENED);
            groupRepository.save(group);
            log.warn("[ErrorTracking] Çözülmüş hata grubu yeniden açıldı: {} ({})",
                    group.getExceptionType(), group.getFingerprint());
        }

        ErrorEvent event = ErrorEvent.builder()
                .trackingCode(generateTrackingCode())
                .group(group)
                .userEmail(userEmail)
                .source(source)
                .message(message)
                .stackTrace(stack)
                .endpoint(endpoint)
                .httpMethod(httpMethod)
                .userAgent(userAgent)
                .build();
        event = eventRepository.save(event);

        log.info("[ErrorTracking] {} hatası kaydedildi: {} → grup {} ({})",
                source, event.getTrackingCode(), group.getId(), exceptionType);
        return event.getTrackingCode();
    }

    private ErrorGroup findOrCreateGroup(ErrorSource source, String fingerprint,
                                         String exceptionType, String location,
                                         String sampleMessage, LocalDateTime now) {
        return groupRepository.findByFingerprint(fingerprint).orElseGet(() -> {
            try {
                return groupRepository.saveAndFlush(ErrorGroup.builder()
                        .fingerprint(fingerprint)
                        .source(source)
                        .exceptionType(truncate(exceptionType, 512))
                        .location(truncate(location, 512))
                        .sampleMessage(sampleMessage)
                        .status(ErrorGroupStatus.NEW)
                        .occurrenceCount(0L)
                        .firstSeenAt(now)
                        .lastSeenAt(now)
                        .build());
            } catch (DataIntegrityViolationException race) {
                // Eşzamanlı istek aynı grubu bizden önce yarattı — unique constraint çözer
                return groupRepository.findByFingerprint(fingerprint)
                        .orElseThrow(() -> race);
            }
        });
    }

    // ─── Admin Okuma / Yönetim ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ErrorGroupResponse> getGroups(ErrorGroupStatus status, int page, int size) {
        Page<ErrorGroup> groups = (status != null)
                ? groupRepository.findByStatusOrderByLastSeenAtDesc(status, PageRequest.of(page, size))
                : groupRepository.findAllByOrderByLastSeenAtDesc(PageRequest.of(page, size));

        List<UUID> ids = groups.stream().map(ErrorGroup::getId).toList();
        Map<UUID, Long> userCounts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : eventRepository.countDistinctUsersByGroupIds(ids)) {
                userCounts.put((UUID) row[0], (Long) row[1]);
            }
        }
        return groups.map(g -> ErrorGroupResponse.from(g, userCounts.getOrDefault(g.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public ErrorGroupDetailResponse getGroupDetail(UUID groupId, int page, int size) {
        ErrorGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Hata grubu bulunamadı."));
        long userCount = eventRepository.countDistinctUsersByGroupId(groupId);
        Page<ErrorEventResponse> events = eventRepository
                .findByGroupIdOrderByOccurredAtDesc(groupId, PageRequest.of(page, size))
                .map(ErrorEventResponse::from);
        return new ErrorGroupDetailResponse(ErrorGroupResponse.from(group, userCount), events);
    }

    @Transactional
    public ErrorGroupResponse updateGroupStatus(UUID groupId, ErrorGroupStatus status, String adminEmail) {
        ErrorGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Hata grubu bulunamadı."));
        group.setStatus(status);
        if (status == ErrorGroupStatus.RESOLVED) {
            group.setResolvedAt(LocalDateTime.now());
            group.setResolvedBy(adminEmail);
        }
        group = groupRepository.save(group);
        long userCount = eventRepository.countDistinctUsersByGroupId(groupId);
        return ErrorGroupResponse.from(group, userCount);
    }

    @Transactional(readOnly = true)
    public ErrorEventResponse getEventByTrackingCode(String trackingCode) {
        return eventRepository.findByTrackingCode(trackingCode)
                .map(ErrorEventResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Bu takip numarasıyla kayıt bulunamadı."));
    }

    /** Takip koduna karşılık gelen grubu döndürür (destek talebi detayında bağ kurmak için). */
    @Transactional(readOnly = true)
    public ErrorGroupResponse findGroupByTrackingCode(String trackingCode) {
        return eventRepository.findByTrackingCode(trackingCode)
                .map(e -> {
                    ErrorGroup g = e.getGroup();
                    return ErrorGroupResponse.from(g, eventRepository.countDistinctUsersByGroupId(g.getId()));
                })
                .orElse(null);
    }

    // ─── Takip Kodu ───────────────────────────────────────────────────────────

    private String generateTrackingCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("ERR-");
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!eventRepository.existsByTrackingCode(code)) return code;
        }
        // 32^8 uzayda 5 ardışık çakışma pratikte imkansız; yine de benzersiz bir kod üret
        return "ERR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ─── Fingerprint Yardımcıları ─────────────────────────────────────────────

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    /**
     * Stack'teki ilk uygulama frame'i: "className.methodName".
     * Satır numarası bilinçli olarak alınmaz — kod değişikliklerinde fingerprint stabil kalır.
     */
    private static String firstAppFrame(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        for (StackTraceElement f : frames) {
            if (f.getClassName().startsWith("com.scrumtools")) {
                return f.getClassName() + "." + f.getMethodName();
            }
        }
        return frames.length > 0 ? frames[0].getClassName() + "." + frames[0].getMethodName() : "unknown";
    }

    private static String stackTraceOf(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    /** Mesajı normalize eder: 200 karaktere kırpar, rakamları # ile değiştirir (id/sayı farkları gruplamayı bozmasın). */
    private static String normalizeMessage(String message) {
        if (message == null) return "";
        String m = message.length() > 200 ? message.substring(0, 200) : message;
        return m.replaceAll("\\d+", "#");
    }

    /** Frontend stack'inin tepe frame'i: satır/kolon numaraları ve URL query/hash temizlenir. */
    private static String topStackFrame(String stack) {
        if (stack == null || stack.isBlank()) return "";
        for (String line : stack.split("\n")) {
            String trimmed = line.trim();
            // "at fn (http://...)" (Chrome) veya "fn@http://..." (Firefox) frame'lerini yakala
            if (trimmed.startsWith("at ") || trimmed.contains("@")) {
                return trimmed
                        .replaceAll(":\\d+:\\d+", "")   // :satır:kolon
                        .replaceAll(":\\d+", "")        // tek :satır
                        .replaceAll("[?#][^)\\s]*", ""); // URL query/hash
            }
        }
        return "";
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 bulunamadı", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ─── Rate Limit (in-memory, sabit 1 dakikalık pencere) ───────────────────

    private boolean allowReport(String userEmail) {
        String key = userEmail != null ? userEmail : "anonymous";
        long currentMinute = System.currentTimeMillis() / 60000;
        RateWindow window = rateWindows.compute(key, (k, w) ->
                (w == null || w.minute != currentMinute) ? new RateWindow(currentMinute) : w);
        return window.count.incrementAndGet() <= REPORT_RATE_LIMIT_PER_MINUTE;
    }

    private static class RateWindow {
        final long minute;
        final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
        RateWindow(long minute) { this.minute = minute; }
    }
}
