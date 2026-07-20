package com.scrumtools.service;

import com.scrumtools.dto.HangmanWordBulkRequest;
import com.scrumtools.dto.HangmanWordBulkResponse;
import com.scrumtools.dto.HangmanWordResponse;
import com.scrumtools.entity.HangmanWord;
import com.scrumtools.entity.Team;
import com.scrumtools.repository.HangmanWordRepository;
import com.scrumtools.repository.TeamRepository;
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
 * Takımların Adam Asmaca oyunu için kendi kelimelerini eklemesini sağlar.
 * Eklenen kelimeler dahili (built-in) TR/EN kelime havuzuna ek olarak
 * frontend tarafında rastgele seçime dahil edilir.
 */
@Service
@RequiredArgsConstructor
public class HangmanService {

    // Adam asmaca boşluksuz tek kelime olmalı; her dilin kendi alfabesiyle sınırlı.
    private static final Pattern TR_WORD = Pattern.compile("^[a-zçğıöşü]{2,30}$");
    private static final Pattern EN_WORD = Pattern.compile("^[a-z]{2,30}$");
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr");

    private final HangmanWordRepository wordRepository;
    private final TeamRepository teamRepository;

    public List<HangmanWordResponse> getWords(UUID teamId, String language) {
        String lang = normalizeLanguage(language);
        return wordRepository.findByTeamIdAndLanguageOrderByCreatedAtDesc(teamId, lang)
                .stream().map(HangmanWordResponse::from).toList();
    }

    @Transactional
    public HangmanWordBulkResponse addWords(UUID teamId, HangmanWordBulkRequest request) {
        String lang = normalizeLanguage(request.language());
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Takım bulunamadı"));
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
            if (wordRepository.existsByTeamIdAndLanguageAndWordIgnoreCase(teamId, lang, normalized)) {
                duplicate++;
                continue;
            }

            wordRepository.save(HangmanWord.builder()
                    .team(team)
                    .language(lang)
                    .word(normalized)
                    .createdByEmail(email)
                    .build());
            added++;
        }

        return new HangmanWordBulkResponse(getWords(teamId, lang), added, duplicate, invalid);
    }

    @Transactional
    public void deleteWord(UUID teamId, UUID wordId) {
        HangmanWord word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("Kelime bulunamadı"));
        if (!word.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Bu kelime bu takıma ait değil");
        }
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
