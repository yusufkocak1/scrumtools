package com.scrumtools.dto;

import com.scrumtools.entity.Dashboard;
import com.scrumtools.entity.enums.DashboardVisibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pano yanıtı — düzenle birlikte gelir.
 *
 * Düzen ayrı bir uçta tutulmadı: pano listesi zaten kullanıcının kendi panolarıyla
 * sınırlı (onlarca değil, birkaç tane) ve her sekme değişiminde ikinci bir istek
 * atmak, sekmeler arasında gezinmeyi gözle görülür biçimde yavaşlatırdı.
 *
 * {@code canEdit} sunucunun kararıdır; arayüz rol yorumunu tekrar etmez
 * (bkz. useOrgPermissions — bayraklar yalnız görünürlük içindir, yetki burada).
 */
public record DashboardResponse(
        UUID id,
        UUID teamId,
        String name,
        DashboardVisibility visibility,
        int position,
        List<Map<String, Object>> layout,
        UUID ownerId,
        String ownerName,
        boolean owned,
        boolean canEdit,
        LocalDateTime updatedAt
) {

    public static DashboardResponse from(Dashboard dashboard, UUID viewerId, boolean canEdit) {
        boolean owned = dashboard.getOwner().getId().equals(viewerId);
        return new DashboardResponse(
                dashboard.getId(),
                dashboard.getTeam().getId(),
                dashboard.getName(),
                dashboard.getVisibility(),
                dashboard.getPosition(),
                dashboard.getLayout() == null ? List.of() : dashboard.getLayout(),
                dashboard.getOwner().getId(),
                dashboard.getOwner().getName(),
                owned,
                canEdit,
                dashboard.getUpdatedAt()
        );
    }
}
