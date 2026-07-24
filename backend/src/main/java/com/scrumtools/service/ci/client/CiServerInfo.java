package com.scrumtools.service.ci.client;

/**
 * Bağlantı testinin sonucu.
 *
 * @param version      sunucu sürümü (Jenkins'te X-Jenkins başlığı), bilinmiyorsa null
 * @param user         kimliği doğrulanan kullanıcı adı
 * @param crumbRequired kurulum CSRF crumb istiyor mu — bağlantıya yazılır
 */
public record CiServerInfo(String version, String user, boolean crumbRequired) {}
