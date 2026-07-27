package com.scrumtools.entity.enums;

/** Kayıtlı filtrenin kimlerce görülebileceği. */
public enum FilterVisibility {

    /** Yalnızca sahibi görür. */
    PRIVATE,

    /** Filtrenin ait olduğu takımın tüm üyeleri görür. */
    TEAM,

    /** Filtrenin projesinde çalışan tüm takımlar görür. */
    PROJECT
}
