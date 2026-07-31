package com.scrumtools.service;

import com.scrumtools.dto.HangmanCategoryResponse;
import com.scrumtools.dto.HangmanWordBulkRequest;
import com.scrumtools.dto.HangmanWordBulkResponse;
import com.scrumtools.dto.HangmanWordResponse;
import com.scrumtools.entity.HangmanCategory;
import com.scrumtools.entity.HangmanWord;
import com.scrumtools.repository.HangmanWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Adam Asmaca kelime havuzunu yönetir. Kelimeler global'dir (takım bazlı değil) ve
 * bir kategoriye bağlıdır; eklenmesi/silinmesi SUPER_ADMIN'e özeldir
 * (bkz. AdminHangmanController), okunması ise oyunu oynayan herkese açıktır.
 *
 * DB'deki kelimeler dahili havuza ({@link HangmanWordPool}) EK olarak kullanılır.
 */
@Service
@RequiredArgsConstructor
public class HangmanService {

    // Adam asmaca boşluksuz tek kelime olmalı; her dilin kendi alfabesiyle sınırlı.
    private static final Pattern TR_WORD = Pattern.compile("^[a-zçğıöşü]{2,30}$");
    private static final Pattern EN_WORD = Pattern.compile("^[a-z]{2,30}$");
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr");

    private final HangmanWordRepository wordRepository;

    /** Kategori null ise tüm kelimeler (kategorisiz eski kayıtlar dâhil). */
    public List<HangmanWordResponse> getWords(String language, String category) {
        String lang = normalizeLanguage(language);
        List<HangmanWord> words = HangmanCategory.parse(category)
                .map(c -> wordRepository.findByLanguageAndCategoryOrderByCreatedAtDesc(lang, c))
                .orElseGet(() -> wordRepository.findByLanguageOrderByCreatedAtDesc(lang));
        return words.stream().map(HangmanWordResponse::from).toList();
    }

    public List<HangmanWordResponse> getWords(String language) {
        return getWords(language, null);
    }

    /**
     * Kategori listesi ve her kategorinin toplam kelime sayısı (dahili havuz + DB, tekrarsız).
     */
    public List<HangmanCategoryResponse> getCategories(String language) {
        String lang = normalizeLanguage(language);

        Map<HangmanCategory, Set<String>> fromDb = new EnumMap<>(HangmanCategory.class);
        for (HangmanWord w : wordRepository.findByLanguageOrderByCreatedAtDesc(lang)) {
            if (w.getCategory() != null) {
                fromDb.computeIfAbsent(w.getCategory(), c -> new HashSet<>()).add(w.getWord());
            }
        }

        return Arrays.stream(HangmanCategory.values())
                .map(category -> {
                    Set<String> all = new HashSet<>(HangmanWordPool.forCategory(lang, category));
                    all.addAll(fromDb.getOrDefault(category, Set.of()));
                    return new HangmanCategoryResponse(category.name(), category.label(lang), all.size());
                })
                .toList();
    }

    @Transactional
    public HangmanWordBulkResponse addWords(HangmanWordBulkRequest request) {
        String lang = normalizeLanguage(request.language());
        HangmanCategory category = HangmanCategory.require(request.category());
        String email = currentEmail();

        Locale locale = "tr".equals(lang) ? TR_LOCALE : Locale.ENGLISH;
        Pattern pattern = "tr".equals(lang) ? TR_WORD : EN_WORD;

        int added = 0;
        int duplicate = 0;
        List<String> invalid = new ArrayList<>();

        for (String raw : request.words()) {
            if (raw == null) continue;
            String normalized = raw.trim().toLowerCase(locale);
            if (normalized.isEmpty()) continue;

            if (!pattern.matcher(normalized).matches()) {
                invalid.add(raw.trim());
                continue;
            }
            // Aynı kelime birden fazla kategoriye girmesin: aynı oyunda iki kez çıkmasını önler.
            if (wordRepository.existsByLanguageAndWordIgnoreCase(lang, normalized)) {
                duplicate++;
                continue;
            }

            wordRepository.save(HangmanWord.builder()
                    .language(lang)
                    .category(category)
                    .word(normalized)
                    .createdByEmail(email)
                    .build());
            added++;
        }

        return new HangmanWordBulkResponse(getWords(lang), added, duplicate, invalid);
    }

    @Transactional
    public void deleteWord(UUID wordId) {
        HangmanWord word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("Kelime bulunamadı"));
        wordRepository.delete(word);
    }

    private String normalizeLanguage(String language) {
        String lang = language == null ? "" : language.trim().toLowerCase(Locale.ENGLISH);
        if (!lang.equals("tr") && !lang.equals("en")) {
            throw new IllegalArgumentException("Desteklenmeyen dil: " + language);
        }
        return lang;
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
