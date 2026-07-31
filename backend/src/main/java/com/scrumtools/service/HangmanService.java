package com.scrumtools.service;

import com.scrumtools.dto.HangmanCategoryResponse;
import com.scrumtools.dto.HangmanWordBulkRequest;
import com.scrumtools.dto.HangmanWordBulkResponse;
import com.scrumtools.dto.HangmanWordPageResponse;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Adam Asmaca kelime havuzunu yönetir. Kelimeler global'dir (takım bazlı değil) ve
 * bir kategoriye bağlıdır; eklenmesi/silinmesi SUPER_ADMIN'e özeldir
 * (bkz. AdminHangmanController), okunması ise oyunu oynayan herkese açıktır.
 *
 * Oyunda kullanılan havuz = dahili havuz ({@link HangmanWordPool}) + DB kayıtları.
 * Admin listesi de bu ikisini birleştirir; dahili kelimelerin DB kaydı olmadığı için
 * silinemezler (yanıtta id = null, source = BUILT_IN).
 */
@Service
@RequiredArgsConstructor
public class HangmanService {

    // Adam asmaca boşluksuz tek kelime olmalı; her dilin kendi alfabesiyle sınırlı.
    private static final Pattern TR_WORD = Pattern.compile("^[a-zçğıöşü]{2,30}$");
    private static final Pattern EN_WORD = Pattern.compile("^[a-z]{2,30}$");
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr");

    private static final int MAX_PAGE_SIZE = 200;

    private final HangmanWordRepository wordRepository;

    /** Oyun içi kullanım: yalnızca DB'ye eklenmiş kelimeler (dahili havuz istemcide zaten var). */
    public List<HangmanWordResponse> getWords(String language, String category) {
        String lang = normalizeLanguage(language);
        List<HangmanWord> words = HangmanCategory.parse(category)
                .map(c -> wordRepository.findByLanguageAndCategoryOrderByCreatedAtDesc(lang, c))
                .orElseGet(() -> wordRepository.findByLanguageOrderByCreatedAtDesc(lang));
        return words.stream().map(HangmanWordResponse::from).toList();
    }

    /**
     * Admin paneli listesi: dahili havuz + DB kayıtları, kategoriye/arama metnine göre
     * süzülüp sayfalanır. Havuz birkaç bin kelime olduğu için sıralama/sayfalama bellekte yapılır.
     *
     * @param category null/boş ise tüm kategoriler (kategorisiz eski kayıtlar en sonda)
     * @param search   kelime içinde geçen metin; null/boş ise süzme yok
     */
    public HangmanWordPageResponse getWordPage(String language, String category, String search, int page, int size) {
        String lang = normalizeLanguage(language);
        Optional<HangmanCategory> filter = HangmanCategory.parse(category);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageIndex = Math.max(page, 0);

        List<HangmanCategory> categories = filter.isPresent()
                ? List.of(filter.get())
                : Arrays.asList(HangmanCategory.values());

        List<HangmanWordResponse> all = new ArrayList<>();
        for (HangmanCategory c : categories) {
            for (String word : HangmanWordPool.forCategory(lang, c)) {
                all.add(HangmanWordResponse.builtIn(word, lang, c));
            }
        }
        List<HangmanWord> dbWords = filter.isPresent()
                ? wordRepository.findByLanguageAndCategoryOrderByCreatedAtDesc(lang, filter.get())
                : wordRepository.findByLanguageOrderByCreatedAtDesc(lang);
        dbWords.forEach(w -> all.add(HangmanWordResponse.from(w)));

        String query = search == null ? "" : search.trim().toLowerCase(localeOf(lang));
        if (!query.isEmpty()) {
            all.removeIf(w -> !w.word().contains(query));
        }

        // Kategori sırasına, kategori içinde alfabetik; kategorisizler en sonda.
        all.sort(Comparator
                .comparingInt((HangmanWordResponse w) -> w.category() == null
                        ? Integer.MAX_VALUE
                        : HangmanCategory.valueOf(w.category()).ordinal())
                .thenComparing(HangmanWordResponse::word));

        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int from = Math.min(pageIndex * pageSize, total);
        int to = Math.min(from + pageSize, total);

        return new HangmanWordPageResponse(List.copyOf(all.subList(from, to)),
                pageIndex, pageSize, total, totalPages);
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

        Locale locale = localeOf(lang);
        Pattern pattern = "tr".equals(lang) ? TR_WORD : EN_WORD;
        // Dahili havuzda zaten olan kelimeyi tekrar eklemek listede çift kayıt yaratır.
        Set<String> builtIn = new HashSet<>(HangmanWordPool.forLanguage(lang));

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
            if (builtIn.contains(normalized)
                    || wordRepository.existsByLanguageAndWordIgnoreCase(lang, normalized)) {
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

        return new HangmanWordBulkResponse(added, duplicate, invalid);
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

    private Locale localeOf(String language) {
        return "tr".equals(language) ? TR_LOCALE : Locale.ENGLISH;
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
