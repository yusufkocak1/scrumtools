package com.scrumtools.entity.enums;

/**
 * Zengin filtrenin alt öğe türü.
 *
 * Sekiz kavramın ortak yanı çoktur (ad, sıra, renk, sorgu, sahiplik); tür başına
 * ayrı entity açmak sekiz tablo ve sekiz CRUD yolu demekti. Ortak sütunlar
 * {@link com.scrumtools.entity.RichFilterElement} üzerinde tipli durur, türe özel
 * alanlar {@code config} (JSONB) içinde taşınır — yeni bir tür eklemek migration
 * gerektirmez.
 *
 * Bkz. RICH_FILTER_PLAN.md — K7.
 */
public enum RichFilterElementKind {

    /** Adlandırılmış + renkli STQL parçası. Filtrelemez, sınıflandırır. */
    SMART_FILTER,

    /** Yazarın elle tanımladığı seçenekli açılır liste — config.options[]. */
    STATIC_FILTER,

    /** Bir alandan sonuç kümesine göre üretilen açılır liste — config.field. */
    DYNAMIC_FILTER,

    /** Seçimlerin adlandırılmış hâli — config.selection. */
    VIEW,

    /**
     * Akıllı filtrelerden oluşan sayaçlı liste paneli.
     *
     * Kullanılmıyor: kuyruk, tanımın değil <b>panonun</b> bir kararı çıktı ve
     * {@code RF_QUEUE} widget'ının yapılandırmasında taşınıyor (K21). Aynı
     * sınıflandırmayı iki panoda farklı kuyruk düzeniyle göstermek, tanımı
     * değiştirmeyi gerektirmesin diye.
     */
    QUEUE,

    /** Görev başına türetilmiş sayı (yaş, çevrim süresi) — config.kind. Henüz kullanılmıyor. */
    CUSTOM_VALUE,

    /** İki ölçünün oranı. Kuyrukla aynı gerekçeyle widget yapılandırmasına taşındı (K21). */
    RATIO,

    /** Bir ölçünün zaman içindeki seyri — config.interval / config.window. */
    TIME_SERIES
}
