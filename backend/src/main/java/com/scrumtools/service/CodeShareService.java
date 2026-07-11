package com.scrumtools.service;

import com.scrumtools.dto.CodeShareResponse;
import com.scrumtools.entity.CodeShare;
import com.scrumtools.entity.Team;
import com.scrumtools.repository.CodeShareRepository;
import com.scrumtools.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeShareService {

    private final CodeShareRepository codeShareRepository;
    private final TeamRepository teamRepository;
    private final EntitlementService entitlementService;

    // ─── Save or Update ──────────────────────────────────────────────────────────

    @Transactional
    public CodeShareResponse saveOrUpdate(UUID teamId, String tag, String data) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));
        entitlementService.assertFeature(team.getOrganization(),
                com.scrumtools.entity.enums.PlanFeature.CODE_SHARE);

        CodeShare codeShare = codeShareRepository.findByTeamIdAndTag(teamId, tag)
                .orElseGet(() -> CodeShare.builder()
                        .team(team)
                        .tag(tag)
                        .build());

        codeShare.setData(data);
        codeShare = codeShareRepository.save(codeShare);

        log.info("CodeShare kaydedildi: teamId={}, tag={}", teamId, tag);
        return CodeShareResponse.from(codeShare);
    }

    // ─── Get By Tag ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CodeShareResponse getByTag(UUID teamId, String tag) {
        return codeShareRepository.findByTeamIdAndTag(teamId, tag)
                .map(CodeShareResponse::from)
                .orElse(null);
    }
}

