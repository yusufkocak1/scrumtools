package com.scrumtools.service.ci.client;

/**
 * Keşifte bulunan bir job.
 *
 * @param fullName    folder dahil tam yol ("scrumtools/deploy-test") — eşlemede saklanan kimlik
 * @param displayName sağlayıcıdaki görünen ad
 * @param url         tarayıcı linki
 * @param buildable   tetiklenebilir mi (devre dışı job'lar false)
 */
public record CiJobInfo(String fullName, String displayName, String url, boolean buildable) {}
