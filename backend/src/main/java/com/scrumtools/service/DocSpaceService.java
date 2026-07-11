package com.scrumtools.service;

import com.scrumtools.dto.DocSpaceRequest;
import com.scrumtools.dto.DocSpaceResponse;
import com.scrumtools.entity.DocSpace;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.User;
import com.scrumtools.repository.DocPageRepository;
import com.scrumtools.repository.DocSpaceRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocSpaceService {

    private final DocSpaceRepository spaceRepository;
    private final DocPageRepository pageRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DocPermissionService permissionService;
    private final EntitlementService entitlementService;

    @Transactional
    public DocSpaceResponse createSpace(UUID projectId, DocSpaceRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proje bulunamadı"));
        entitlementService.assertFeature(project.getOrganization(),
                com.scrumtools.entity.enums.PlanFeature.DOCS);

        User user = getCurrentUser();
        permissionService.checkSpaceManageAccess(projectId, user);

        if (spaceRepository.existsByProjectIdAndNameIgnoreCase(projectId, request.name())) {
            throw new IllegalArgumentException("Bu isimde bir space zaten mevcut");
        }

        DocSpace space = DocSpace.builder()
                .project(project)
                .name(request.name())
                .description(request.description())
                .createdBy(user)
                .build();

        space = spaceRepository.save(space);
        log.info("Doc space oluşturuldu: {} (proje: {})", space.getName(), projectId);

        return DocSpaceResponse.from(space, 0);
    }

    public List<DocSpaceResponse> getSpaces(UUID projectId) {
        User user = getCurrentUser();
        List<DocSpace> spaces = spaceRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        return spaces.stream()
                .filter(space -> permissionService.hasSpaceAccess(space, user))
                .map(space -> {
                    int pageCount = pageRepository.countBySpaceId(space.getId());
                    return DocSpaceResponse.from(space, pageCount);
                })
                .collect(Collectors.toList());
    }

    public DocSpaceResponse getSpace(UUID spaceId) {
        DocSpace space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkReadAccess(space, null, user);

        int pageCount = pageRepository.countBySpaceId(spaceId);
        return DocSpaceResponse.from(space, pageCount);
    }

    @Transactional
    public DocSpaceResponse updateSpace(UUID spaceId, DocSpaceRequest request) {
        DocSpace space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkSpaceManageAccess(space.getProject().getId(), user);

        space.setName(request.name());
        space.setDescription(request.description());
        space = spaceRepository.save(space);

        int pageCount = pageRepository.countBySpaceId(spaceId);
        return DocSpaceResponse.from(space, pageCount);
    }

    @Transactional
    public void deleteSpace(UUID spaceId) {
        DocSpace space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space bulunamadı"));

        User user = getCurrentUser();
        permissionService.checkSpaceManageAccess(space.getProject().getId(), user);

        spaceRepository.delete(space);
        log.info("Doc space silindi: {}", spaceId);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}

