package com.scrumtools.service.scm;

import com.scrumtools.entity.ScmRepository;
import com.scrumtools.entity.Task;
import com.scrumtools.entity.Team;
import com.scrumtools.repository.TaskRepository;
import com.scrumtools.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Commit mesajı / branch adındaki task anahtarlarını (TEAMCODE-N) ayrıştırır
 * ve reponun eşlendiği projenin takımları KAPSAMINDA task'a çözer.
 * Kapsam sınırlaması, farklı org'larda aynı takım kodu bulunması durumunda
 * yanlış eşlemeyi engeller.
 */
@Component
@RequiredArgsConstructor
public class ScmCommitLinker {

    /**
     * TEAMCODE-N formatı; kelime sınırları şart (SCRM-12 ✓, XSCRM-12 ✗, scrm-12 ✗).
     * Takım kodu: büyük harfle başlar, 2-10 karakter [A-Z0-9]; numara: 1-6 hane.
     */
    public static final Pattern TASK_KEY =
            Pattern.compile("(?<![A-Z0-9])([A-Z][A-Z0-9]{1,9}-\\d{1,6})(?!\\d)");

    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;

    /** Metindeki tüm task anahtarlarını sırayı koruyarak, tekrarsız döner. */
    public static Set<String> extractTaskKeys(String text) {
        Set<String> keys = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return keys;
        Matcher matcher = TASK_KEY.matcher(text);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    /** Metindeki anahtarları reponun projesindeki task'lara çözer (eşleşmeyenler atlanır). */
    public List<Task> resolveTasks(ScmRepository repo, String text) {
        Set<String> keys = extractTaskKeys(text);
        if (keys.isEmpty()) return List.of();

        List<UUID> teamIds = teamRepository.findByProjectId(repo.getProject().getId()).stream()
                .map(Team::getId)
                .toList();
        if (teamIds.isEmpty()) return List.of();

        List<Task> tasks = new ArrayList<>();
        for (String key : keys) {
            taskRepository.findByCustomIdInTeams(key, teamIds).ifPresent(tasks::add);
        }
        return tasks;
    }
}
