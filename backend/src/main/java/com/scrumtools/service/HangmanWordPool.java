package com.scrumtools.service;

import java.util.List;

/**
 * Sunucu tarafı dahili kelime havuzu.
 *
 * Takım oyununda kelime SUNUCUDA seçilir ve istemciye açık gönderilmez; bu yüzden
 * frontend'deki {@code hangmanWords.js} havuzunun bir kopyası burada da bulunur.
 * (Tek kişilik oyun hâlâ frontend havuzunu kullanır.)
 *
 * DB'deki {@code hangman_words} tablosundaki kelimeler bu havuza EK olarak kullanılır;
 * DB boş olsa bile oyun başlatılabilsin diye buradaki liste güvenli bir taban sağlar.
 */
public final class HangmanWordPool {

    private HangmanWordPool() {
    }

    public static final List<String> TR = List.of(
            // Genel
            "bilgisayar", "klavye", "yazılım", "donanım", "geliştirici", "takım", "oyun", "kahve",
            "dağ", "deniz", "orman", "çöl", "ada", "nehir", "köprü", "kale", "bahçe", "mutfak",
            "yatak", "kütüphane", "hastane", "havalimanı", "istasyon", "restoran", "bisiklet",
            "uçak", "yelkenli", "fil", "zürafa", "penguen", "yunus", "kelebek", "sincap", "kanguru",
            "çikolata", "ananas", "çilek", "sandviç", "şemsiye", "teleskop", "mikroskop", "kimya",
            "coğrafya", "tarih", "matematik", "bilim", "resim", "heykel", "orkestra", "senfoni",
            "macera", "hazine", "gizem", "ejderha", "büyücü", "krallık", "özgürlük", "cesaret",
            "bilgelik", "uyum", "güneş", "gökkuşağı", "yıldırım", "kartopu", "şelale", "yıldız",
            "gemi", "roket", "volkan", "buzul", "çiftlik", "değirmen", "fener", "liman",
            // Scrum & yazılım temalı
            "sprint", "koşu", "pano", "öncelik", "hedef", "planlama", "tahmin", "sürüm",
            "hata", "özellik", "test", "sunucu", "tarayıcı", "fonksiyon", "değişken", "algoritma",
            "terminal", "ekran", "yazıcı", "şifre", "kullanıcı", "takvim", "toplantı", "geribildirim"
    );

    public static final List<String> EN = List.of(
            // General
            "computer", "keyboard", "developer", "teamwork", "coffee", "mountain", "ocean", "forest",
            "desert", "island", "river", "bridge", "castle", "garden", "kitchen", "bedroom", "library",
            "hospital", "airport", "station", "restaurant", "bicycle", "airplane", "sailboat",
            "elephant", "giraffe", "penguin", "dolphin", "butterfly", "squirrel", "kangaroo",
            "chocolate", "pineapple", "strawberry", "sandwich", "umbrella", "telescope", "microscope",
            "chemistry", "geography", "history", "mathematics", "science", "painting", "sculpture",
            "orchestra", "symphony", "adventure", "treasure", "mystery", "dragon", "wizard", "kingdom",
            "freedom", "courage", "wisdom", "harmony", "sunshine", "thunder", "lightning", "snowflake",
            "waterfall", "rocket", "volcano", "glacier", "lighthouse", "harbor", "windmill", "rainbow",
            // Scrum & software themed
            "sprint", "backlog", "kanban", "meeting", "deadline", "feedback", "strategy", "priority",
            "velocity", "standup", "planning", "estimate", "release", "version", "feature", "testing",
            "deploy", "server", "browser", "function", "variable", "algorithm", "framework", "terminal"
    );

    public static List<String> forLanguage(String language) {
        return "en".equals(language) ? EN : TR;
    }
}
