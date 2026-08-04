package com.scrumtools.entity.enums;

/**
 * Pano görünürlüğü.
 *
 * {@link FilterVisibility} yeniden kullanılmadı: orada PROJECT değeri var ve pano
 * için karşılığı yok — takıma bağlı bir panonun "projeye açık" hâli, panodaki
 * widget'ların hepsi tek takımın verisini gösterdiği için anlamsız kalırdı.
 * Anlamsız bir enum değerini taşımak, arayüzde de serviste de her seferinde
 * "bu durumda ne olacak" sorusunu doğurur.
 */
public enum DashboardVisibility {

    /** Yalnız sahibi görür. */
    PRIVATE,

    /** Takımın bütün üyeleri görür; düzenleme yine sahibi ve organizasyon yöneticisindedir. */
    TEAM
}
