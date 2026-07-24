package com.scrumtools.config;

import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.PlanFeature;
import com.scrumtools.entity.enums.RoleScope;
import com.scrumtools.entity.enums.StatusCategory;
import com.scrumtools.repository.*;
import com.scrumtools.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final RoleService roleService;
    private final TeamRepository teamRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final RoleRepository roleRepository;
    private final PlanRepository planRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Varsayılan roller kontrol ediliyor...");
        roleService.seedDefaultRoles();
        log.info("Varsayılan roller hazır.");

        // Organizasyona bağlı olmayan (orphan) takımları temizle
        var orphans = teamRepository.findByOrganizationIsNull();
        if (!orphans.isEmpty()) {
            teamRepository.deleteAll(orphans);
            log.info("{} adet organizasyonsuz (orphan) takım silindi.", orphans.size());
        }

        log.info("Sistem workflow şablonları kontrol ediliyor...");
        seedSystemWorkflowTemplates();
        log.info("Workflow şablonları hazır.");

        backfillScmGrants();
    }

    /**
     * Git entegrasyonu sonradan eklendiği için mevcut kurulumlardaki seed'li
     * kayıtlara yeni hakları işler (seed metodları varlık kontrolüyle atladığından
     * burada backfill gerekir). Idempotent — her boot'ta güvenle çalışır.
     */
    private void backfillScmGrants() {
        for (String code : List.of("PRO", "MAX")) {
            planRepository.findByCode(code).ifPresent(plan -> {
                boolean changed = false;
                for (PlanFeature feature : List.of(PlanFeature.GIT_INTEGRATION, PlanFeature.CI_CD_INTEGRATION)) {
                    if (!plan.getFeatures().contains(feature)) {
                        plan.getFeatures().add(feature);
                        changed = true;
                        log.info("{} planına {} özelliği eklendi.", code, feature);
                    }
                }
                if (changed) planRepository.save(plan);
            });
        }
        for (String roleName : List.of("Project Admin", "Developer")) {
            roleRepository.findByNameAndScope(roleName, RoleScope.PROJECT)
                    .filter(Role::getIsDefault)
                    .ifPresent(role -> {
                        if (!role.getPermissions().contains(Permission.SCM_CREATE_BRANCH)) {
                            role.getPermissions().add(Permission.SCM_CREATE_BRANCH);
                            roleRepository.save(role);
                            log.info("'{}' rolüne SCM_CREATE_BRANCH izni eklendi.", roleName);
                        }
                    });
        }
    }

    /**
     * Proje oluşturulduğunda kopyalanacak sistem genelinde workflow şablonları.
     * Takım/proje bağlantısı olmayan (null) bu kayıtlar şablon olarak kullanılır.
     */
    private void seedSystemWorkflowTemplates() {
        // Scrum Workflow şablonu daha önce oluşturulmuş mu kontrol et
        boolean scrumExists = workflowRepository.findAll().stream()
                .anyMatch(w -> "Scrum Workflow".equals(w.getName())
                        && w.getTeam() == null && w.getProject() == null);
        if (!scrumExists) {
            createScrumWorkflowTemplate();
        }
    }

    private void createScrumWorkflowTemplate() {
        Workflow workflow = Workflow.builder()
                .name("Scrum Workflow")
                .description("Standart Scrum iş akışı: Open → In Progress → In Review → Testing → Done")
                .isDefault(true)
                .issueTypes(List.of("task", "story", "bug"))
                .build();
        workflow = workflowRepository.save(workflow);

        WorkflowStatus open = saveStatus(workflow, "Open", StatusCategory.TO_DO, "#6B7280", true, false, 0);
        WorkflowStatus inProgress = saveStatus(workflow, "In Progress", StatusCategory.IN_PROGRESS, "#3B82F6", false, false, 1);
        WorkflowStatus inReview = saveStatus(workflow, "In Review", StatusCategory.IN_PROGRESS, "#8B5CF6", false, false, 2);
        WorkflowStatus testing = saveStatus(workflow, "Testing", StatusCategory.IN_PROGRESS, "#F59E0B", false, false, 3);
        WorkflowStatus done = saveStatus(workflow, "Done", StatusCategory.DONE, "#10B981", false, true, 4);
        WorkflowStatus cancelled = saveStatus(workflow, "Cancelled", StatusCategory.DONE, "#EF4444", false, true, 5);

        saveTransition(workflow, "Start Progress", open, inProgress, 0);
        saveTransition(workflow, "Submit for Review", inProgress, inReview, 1);
        saveTransition(workflow, "Back to In Progress", inReview, inProgress, 2);
        saveTransition(workflow, "Send to Testing", inReview, testing, 3);
        saveTransition(workflow, "Reject - Back to Progress", testing, inProgress, 4);
        saveTransition(workflow, "Complete", testing, done, 5);
        saveTransition(workflow, "Cancel", null, cancelled, 6);

        log.info("Scrum Workflow şablonu oluşturuldu.");
    }

    private WorkflowStatus saveStatus(Workflow wf, String name, StatusCategory cat,
                                       String color, boolean isInitial, boolean isFinal, int pos) {
        return workflowStatusRepository.save(WorkflowStatus.builder()
                .workflow(wf)
                .name(name)
                .category(cat)
                .color(color)
                .isInitial(isInitial)
                .isFinal(isFinal)
                .position(pos)
                .build());
    }

    private void saveTransition(Workflow wf, String name, WorkflowStatus from,
                                 WorkflowStatus to, int pos) {
        workflowTransitionRepository.save(WorkflowTransition.builder()
                .workflow(wf)
                .name(name)
                .fromStatus(from)
                .toStatus(to)
                .position(pos)
                .build());
    }
}
