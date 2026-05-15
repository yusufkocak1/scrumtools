package com.scrumtools.entity.enums;

/**
 * Task'lar arasındaki ilişki tipleri.
 */
public enum LinkType {
    BLOCKS("blocks"),
    IS_BLOCKED_BY("is blocked by"),
    RELATES_TO("relates to"),
    DUPLICATES("duplicates"),
    IS_DUPLICATED_BY("is duplicated by"),
    CAUSES("causes"),
    IS_CAUSED_BY("is caused by"),
    CLONES("clones"),
    IS_CLONED_FROM("is cloned from");

    private final String label;

    LinkType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

