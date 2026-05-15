package com.scrumtools.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO (S3-compatible) dosya depolama servisi.
 *
 * Dosya yolu formatı: teams/{teamId}/tasks/{taskId}/{uuid}_{originalFileName}
 * Bu yapı ileride bucket policy, lifecycle rules ve CDN entegrasyonu için uygundur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /**
     * Dosya yükle — MinIO'ya kaydet ve object key döndür.
     *
     * @return objectKey (MinIO'daki tam yol)
     */
    public String upload(UUID teamId, UUID taskId, MultipartFile file) {
        String pathPrefix = String.format("teams/%s/tasks/%s", teamId, taskId);
        return upload(pathPrefix, file);
    }

    /**
     * Genel amaçlı dosya yükleme — verilen prefix altına kaydeder.
     *
     * @param pathPrefix MinIO object key prefix'i (örn. "docs/spaces/{spaceId}/pages/{pageId}")
     * @return objectKey (MinIO'daki tam yol)
     */
    public String upload(String pathPrefix, MultipartFile file) {
        try {
            String originalName = sanitizeFileName(file.getOriginalFilename());
            String objectKey = String.format("%s/%s_%s",
                    pathPrefix, UUID.randomUUID().toString().substring(0, 8), originalName);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("Dosya yüklendi: {} ({} bytes)", objectKey, file.getSize());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Dosya yüklenemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Dosyayı MinIO'dan sil.
     */
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("Dosya silindi: {}", objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Dosya silinemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Dosya stream'i döndür (indirme için).
     */
    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Dosya indirilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Pre-signed URL üret — dosya tarayıcıdan doğrudan indirilebilir (geçici link).
     *
     * @param expiryMinutes URL geçerlilik süresi (dakika)
     */
    public String getPresignedUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Pre-signed URL oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Dosya adındaki zararlı karakterleri temizle.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "unnamed";
        // Sadece alfanumerik, nokta, tire ve alt çizgiye izin ver
        return fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}

