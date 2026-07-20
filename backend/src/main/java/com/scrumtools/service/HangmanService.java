package com.scrumtools.service;

import com.scrumtools.dto.HangmanWordBulkRequest;
import com.scrumtools.dto.HangmanWordBulkResponse;
import com.scrumtools.dto.HangmanWordResponse;
import com.scrumtools.entity.HangmanWord;
import com.scrumtools.repository.HangmanWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Adam Asmaca kelime havuzunu yönetir. Kelimeler global'dir (takım bazlı değil);
 * eklenmesi/silinmesi SUPER_ADMIN'e özeldir (bkz. AdminHangmanController),
 * okunması ise oyunu oynayan herkese açıktır.
 */
@Service
@RequiredArgsConstructor
public class HangmanService {

    // Adam asmaca boşluksuz tek kelime olmalı; her dilin kendi alfabesiyle sınırlı.
    private static final Pattern TR_WORD = Pattern.compile("^[a-zçğıöşü]{2,30}$");
    private static final Pattern EN_WORD = Pattern.compile("^[a-z]{2,30}$");
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr");

    private final HangmanWordRepository wordRepository;

    public List<HangmanWordResponse> getWords(String language) {
        String lang = normalizeLanguage(language);
        return wordRepository.findByLanguageOrderByCreatedAtDesc(lang)
                .stream().map(HangmanWordResponse::from).toList();
    }

    @Transactional
    public HangmanWordBulkResponse addWords(HangmanWordBulkRequest request) {
        String lang = normalizeLanguage(request.language());
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
            if (wordRepository.existsByLanguageAndWordIgnoreCase(lang, normalized)) {
                duplicate++;
                continue;
            }

            wordRepository.save(HangmanWord.builder()
                    .language(lang)
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
