package com.scrumtools.query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * STQL fonksiyonlarının çözümü.
 *
 * Fonksiyonun dönüş tipi kullanıldığı alanın tipine göre yorumlanır:
 * tarih fonksiyonları LocalDate/LocalDateTime, currentUser() e-posta,
 * sprint fonksiyonları UUID listesi üretir.
 */
public final class QueryFunctions {

    public static final Set<String> USER_FUNCTIONS = Set.of("currentuser");
    public static final Set<String> DATE_FUNCTIONS = Set.of(
            "now", "today", "startofday", "endofday",
            "startofweek", "endofweek", "startofmonth", "endofmonth");
    public static final Set<String> SPRINT_FUNCTIONS = Set.of(
            "currentsprint", "opensprints", "closedsprints");

    /** Arayüzün fonksiyon listesi ucu için — ad ve kısa açıklama. */
    public static final List<String[]> CATALOG = List.of(
            new String[]{"currentUser()", "Oturumdaki kullanıcı"},
            new String[]{"now()", "Şu an"},
            new String[]{"today()", "Bugün"},
            new String[]{"startOfDay()", "Günün başı"},
            new String[]{"endOfDay()", "Günün sonu"},
            new String[]{"startOfWeek()", "Haftanın başı (Pazartesi)"},
            new String[]{"endOfWeek()", "Haftanın sonu (Pazar)"},
            new String[]{"startOfMonth()", "Ayın başı"},
            new String[]{"endOfMonth()", "Ayın sonu"},
            new String[]{"currentSprint()", "Aktif sprint(ler)"},
            new String[]{"openSprints()", "Açık sprintler"},
            new String[]{"closedSprints()", "Tamamlanmış sprintler"}
    );

    private QueryFunctions() {
    }

    public static boolean isKnown(String name) {
        String n = norm(name);
        return USER_FUNCTIONS.contains(n) || DATE_FUNCTIONS.contains(n) || SPRINT_FUNCTIONS.contains(n);
    }

    /** Kullanıcı fonksiyonu → e-posta. */
    public static String resolveUser(QueryNode.FunctionCall fn, QueryContext ctx) {
        if (!USER_FUNCTIONS.contains(norm(fn.name()))) {
            throw new QueryParseException(
                    "'" + fn.name() + "' bir kullanıcı fonksiyonu değil. Kullanılabilir: currentUser()",
                    fn.position(), fn.length());
        }
        if (ctx.currentUserEmail() == null) {
            throw new QueryParseException("currentUser() çözülemedi: oturum bilgisi yok.",
                    fn.position(), fn.length());
        }
        return ctx.currentUserEmail();
    }

    /** Sprint fonksiyonu → sprint id listesi. */
    public static List<java.util.UUID> resolveSprints(QueryNode.FunctionCall fn, QueryContext ctx) {
        return switch (norm(fn.name())) {
            case "currentsprint", "opensprints" -> ctx.sprintIds("open");
            case "closedsprints" -> ctx.sprintIds("done");
            default -> throw new QueryParseException(
                    "'" + fn.name() + "' bir sprint fonksiyonu değil. "
                            + "Kullanılabilir: currentSprint(), openSprints(), closedSprints()",
                    fn.position(), fn.length());
        };
    }

    /** Tarih fonksiyonu → zaman damgası. */
    public static LocalDateTime resolveDateTime(QueryNode.FunctionCall fn, QueryContext ctx) {
        LocalDateTime now = ctx.now();
        LocalDate today = now.toLocalDate();
        return switch (norm(fn.name())) {
            case "now" -> now;
            case "today", "startofday" -> today.atStartOfDay();
            case "endofday" -> today.atTime(23, 59, 59);
            case "startofweek" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case "endofweek" -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59);
            case "startofmonth" -> today.withDayOfMonth(1).atStartOfDay();
            case "endofmonth" -> today.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
            default -> throw new QueryParseException(
                    "'" + fn.name() + "' bir tarih fonksiyonu değil. "
                            + "Kullanılabilir: now(), today(), startOfWeek(), endOfMonth() ...",
                    fn.position(), fn.length());
        };
    }

    /**
     * Göreli tarih literal'i: -7d, 2w, -1M, 3h, 30m.
     * Birim: d=gün, w=hafta, M=ay, y=yıl, h=saat, m=dakika.
     */
    public static LocalDateTime resolveRelative(String literal, QueryContext ctx, int position, int length) {
        if (literal == null || literal.length() < 2) {
            throw new QueryParseException("Geçersiz göreli tarih: " + literal, position, length);
        }
        char unit = literal.charAt(literal.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(literal.substring(0, literal.length() - 1));
        } catch (NumberFormatException e) {
            throw new QueryParseException("Geçersiz göreli tarih: " + literal, position, length);
        }
        LocalDateTime now = ctx.now();
        return switch (unit) {
            case 'd' -> now.plusDays(amount);
            case 'w' -> now.plusWeeks(amount);
            case 'M' -> now.plusMonths(amount);
            case 'y' -> now.plusYears(amount);
            case 'h' -> now.plusHours(amount);
            case 'm' -> now.plusMinutes(amount);
            default -> throw new QueryParseException(
                    "Bilinmeyen zaman birimi '" + unit + "'. Kullanılabilir: d, w, M, y, h, m",
                    position, length);
        };
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
