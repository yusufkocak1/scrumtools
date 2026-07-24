package com.scrumtools.dto;

/**
 * Bağlantı testi sonucu — başarısızlıkta da 200 döner, ok=false ile.
 *
 * @param serverVersion  Jenkins sürümü (X-Jenkins başlığı)
 * @param user           kimliği doğrulanan kullanıcı
 * @param crumbRequired  kurulumda CSRF koruması açık mı (bağlantıya yazılır)
 */
public record CiConnectionTestResponse(
        boolean ok,
        String message,
        String serverVersion,
        String user,
        boolean crumbRequired
) {}
