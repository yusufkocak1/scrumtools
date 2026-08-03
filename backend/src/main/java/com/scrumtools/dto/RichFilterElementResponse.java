package com.scrumtools.dto;

import com.scrumtools.entity.RichFilterElement;
import com.scrumtools.entity.enums.RichFilterElementKind;

import java.util.Map;
import java.util.UUID;

/** Zengin filtre öğesi yanıtı. */
public record RichFilterElementResponse(
        UUID id,
        RichFilterElementKind kind,
        String name,
        String query,
        String color,
        Integer position,
        Map<String, Object> config
) {

    public static RichFilterElementResponse from(RichFilterElement e) {
        return new RichFilterElementResponse(
                e.getId(),
                e.getKind(),
                e.getName(),
                e.getQuery(),
                e.getColor(),
                e.getPosition(),
                e.configOrEmpty());
    }
}
