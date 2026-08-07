package com.scrumtools.config;

import com.scrumtools.service.MediaLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gömülü görsel migrasyonu: açıklama/yorum/doküman içeriğine geçmişte MinIO presigned
 * URL'i (60 dakikalık, imzalı) gömülüyordu; süre dolunca görseller kırılıyordu.
 *
 * Bu runner içerikteki presigned bağlantıları, aynı dosyayı gösteren kalıcı
 * /api/media/... bağlantısına çevirir. Object key eşleşmesiyle çalışır: eşleşen ek
 * kaydı bulunamayan (silinmiş dosya) bağlantılara dokunulmaz.
 *
 * Idempotent — sorgu yalnızca hâlâ presigned imza içeren satırları getirir, dönüşüm
 * sonrası bu satırlar tekrar seçilmez; her boot'ta güvenle çalışır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaUrlBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final MediaLinkService mediaLinkService;

    /** İçerikteki mutlak URL'ler — HTML attribute'ı ya da markdown link parantezi sınır olur. */
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>()\\\\]+");

    /** Presigned URL'i ayırt eden imza parametresi. */
    private static final String PRESIGNED_MARKER = "X-Amz-Signature";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, String> objectKeyToMediaUrl = loadAttachmentUrls();
        if (objectKeyToMediaUrl.isEmpty()) return;

        int updated = 0;
        updated += rewriteColumn("tasks", "description", objectKeyToMediaUrl);
        updated += rewriteColumn("task_comments", "text", objectKeyToMediaUrl);
        updated += rewriteColumn("doc_pages", "content", objectKeyToMediaUrl);
        updated += rewriteColumn("doc_page_versions", "content", objectKeyToMediaUrl);

        if (updated > 0) {
            log.info("Medya URL migrasyonu: {} kayıttaki süresi dolan presigned bağlantı kalıcı /api/media linkine çevrildi.", updated);
        }
    }

    /** Tüm ek kayıtları için object key → kalıcı medya URL'i eşlemesi. */
    private Map<String, String> loadAttachmentUrls() {
        Map<String, String> map = new HashMap<>();
        collectInto(map, "task_attachments", MediaLinkService.TASK_ATTACHMENT);
        collectInto(map, "doc_page_attachments", MediaLinkService.DOC_ATTACHMENT);
        return map;
    }

    private void collectInto(Map<String, String> map, String table, String mediaType) {
        jdbcTemplate.query("SELECT id, object_key FROM " + table, rs -> {
            String objectKey = rs.getString("object_key");
            if (objectKey != null && !objectKey.isBlank()) {
                map.put(objectKey, mediaLinkService.urlFor(mediaType, UUID.fromString(rs.getString("id"))));
            }
        });
    }

    private int rewriteColumn(String table, String column, Map<String, String> objectKeyToMediaUrl) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, " + column + " AS content FROM " + table +
                        " WHERE " + column + " LIKE '%" + PRESIGNED_MARKER + "%'");

        int updated = 0;
        for (Map<String, Object> row : rows) {
            String content = (String) row.get("content");
            if (content == null) continue;

            String rewritten = replacePresignedUrls(content, objectKeyToMediaUrl);
            if (!rewritten.equals(content)) {
                jdbcTemplate.update(
                        "UPDATE " + table + " SET " + column + " = ? WHERE id = ?",
                        rewritten, row.get("id"));
                updated++;
            }
        }
        return updated;
    }

    private String replacePresignedUrls(String content, Map<String, String> objectKeyToMediaUrl) {
        Matcher matcher = URL_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String url = matcher.group();
            String replacement = url;
            if (url.contains(PRESIGNED_MARKER)) {
                for (Map.Entry<String, String> entry : objectKeyToMediaUrl.entrySet()) {
                    if (url.contains(entry.getKey())) {
                        replacement = entry.getValue();
                        break;
                    }
                }
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
