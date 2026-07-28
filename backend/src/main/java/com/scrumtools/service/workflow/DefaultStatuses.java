package com.scrumtools.service.workflow;

import com.scrumtools.entity.enums.StatusCategory;

import java.util.List;

/**
 * Bir takıma workflow ilk kez üretilirken kullanılan durum seti.
 *
 * Bilinçli olarak uygulamanın eski sabit durumlarıyla ({@code To Do / In Progress
 * / Done / Cancelled}) aynı adları taşır: provizyon anında var olan görevlerin
 * durumları kataloğun dışında kalmasın, board kolonları eşleşmeye devam etsin.
 * Takım bu seti ayarlar ekranından yeniden adlandırabilir ya da genişletebilir.
 */
public final class DefaultStatuses {

    /** Workflow hiç durum içermiyorsa kullanılacak adlar (eski sabit değerler). */
    public static final String FALLBACK_INITIAL = "To Do";
    public static final String FALLBACK_DONE = "Done";
    public static final String FALLBACK_CANCELLED = "Cancelled";

    public record Seed(String name, StatusCategory category, String color,
                       boolean isInitial, boolean isFinal, boolean isCancellation) {}

    public static final List<Seed> SEEDS = List.of(
            new Seed("To Do",       StatusCategory.TO_DO,       "#6B7280", true,  false, false),
            new Seed("In Progress", StatusCategory.IN_PROGRESS, "#3B82F6", false, false, false),
            new Seed("In Review",   StatusCategory.IN_PROGRESS, "#8B5CF6", false, false, false),
            new Seed("Done",        StatusCategory.DONE,        "#10B981", false, true,  false),
            new Seed("Cancelled",   StatusCategory.DONE,        "#EF4444", false, true,  true)
    );

    private DefaultStatuses() {}
}
