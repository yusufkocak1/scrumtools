package com.scrumtools.service.scm;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** SCM sağlayıcılarından gelen ISO-8601 tarihleri için ortak parser. */
public final class ScmDates {

    private ScmDates() {}

    /** Offset'li ISO-8601 tarihi sistem zone'unda LocalDateTime'a çevirir; bozuksa null. */
    public static LocalDateTime parse(Object isoDate) {
        if (isoDate == null) return null;
        try {
            return OffsetDateTime.parse(String.valueOf(isoDate))
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }
}
