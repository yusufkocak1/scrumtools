package com.scrumtools.entity;

import java.util.Locale;
import java.util.Optional;

/**
 * Adam Asmaca kelime kategorileri.
 *
 * Hem dahili havuz ({@link com.scrumtools.service.HangmanWordPool}) hem de DB'ye
 * eklenen kelimeler bir kategoriye bağlıdır. Oyuncu ister bir kategori seçer,
 * ister kategori seçmeden (null) tüm havuzdan rastgele oynar.
 *
 * NOT: Etiketlerin bir kopyası frontend'de {@code data/hangmanWords.js} içinde
 * emoji'leriyle birlikte durur — kelime havuzunda olduğu gibi burası ve orası
 * birlikte güncellenmelidir.
 */
public enum HangmanCategory {

    ANIMALS("Hayvanlar", "Animals"),
    PLANTS("Bitkiler ve Çiçekler", "Plants & Flowers"),
    FOOD("Yiyecek ve İçecek", "Food & Drink"),
    FRUITS_VEGETABLES("Meyve ve Sebze", "Fruits & Vegetables"),
    COUNTRIES("Ülkeler", "Countries"),
    CITIES("Şehirler", "Cities"),
    NATURE("Doğa ve Coğrafya", "Nature & Geography"),
    SPACE("Uzay", "Space"),
    WEATHER("Hava ve Mevsimler", "Weather & Seasons"),
    PROFESSIONS("Meslekler", "Professions"),
    SPORTS("Spor", "Sports"),
    MUSIC("Müzik", "Music"),
    CINEMA("Sinema ve Dizi", "Movies & TV"),
    LITERATURE("Edebiyat", "Literature"),
    ART("Sanat", "Art"),
    TECHNOLOGY("Teknoloji", "Technology"),
    SOFTWARE("Yazılım", "Software"),
    SCRUM("Scrum ve Çeviklik", "Scrum & Agile"),
    OFFICE("Ofis ve İş Dünyası", "Office & Business"),
    SCIENCE("Bilim", "Science"),
    HEALTH("Sağlık ve Tıp", "Health & Medicine"),
    BODY("İnsan Vücudu", "Human Body"),
    CLOTHING("Giyim", "Clothing"),
    HOME("Ev ve Eşyalar", "Home & Furniture"),
    TRANSPORT("Ulaşım ve Taşıtlar", "Transport & Vehicles"),
    SCHOOL("Okul ve Eğitim", "School & Education"),
    MYTHOLOGY("Mitoloji ve Masal", "Myth & Fantasy"),
    HOBBIES("Hobi ve Oyun", "Hobbies & Games");

    private final String trLabel;
    private final String enLabel;

    HangmanCategory(String trLabel, String enLabel) {
        this.trLabel = trLabel;
        this.enLabel = enLabel;
    }

    public String label(String language) {
        return "en".equals(language) ? enLabel : trLabel;
    }

    /** Boş/tanınmayan değerde boş döner — "kategori seçilmedi" anlamına gelir. */
    public static Optional<HangmanCategory> parse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ENGLISH)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Kategori zorunlu olan yerlerde (kelime ekleme) kullanılır. */
    public static HangmanCategory require(String raw) {
        return parse(raw).orElseThrow(
                () -> new IllegalArgumentException("Geçersiz kategori: " + raw));
    }
}
