package com.scrumtools.dto;

import com.scrumtools.service.ci.client.CiJobInfo;

/**
 * Keşif ekranındaki job satırı.
 *
 * @param mapped bu job hâlihazırda bir projeye eşlenmiş mi (dropdown'da işaretlenir)
 */
public record CiJobInfoResponse(
        String fullName,
        String displayName,
        String url,
        boolean buildable,
        boolean mapped
) {
    public static CiJobInfoResponse from(CiJobInfo info, boolean mapped) {
        return new CiJobInfoResponse(
                info.fullName(), info.displayName(), info.url(), info.buildable(), mapped);
    }
}
