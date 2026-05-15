package com.scrumtools.service;

import com.scrumtools.dto.CustomFieldDefinitionRequest;
import com.scrumtools.dto.CustomFieldDefinitionResponse;
import com.scrumtools.entity.CustomFieldDefinition;
import com.scrumtools.entity.Project;
import com.scrumtools.entity.Team;
import com.scrumtools.repository.CustomFieldDefinitionRepository;
import com.scrumtools.repository.ProjectRepository;
import com.scrumtools.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomFieldService {

    private final CustomFieldDefinitionRepository fieldRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<CustomFieldDefinitionResponse> getFieldsByTeam(UUID teamId) {
        return fieldRepository.findByTeamIdOrderByPositionAsc(teamId).stream()
                .map(CustomFieldDefinitionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomFieldDefinitionResponse> getFieldsByProject(UUID projectId) {
        return fieldRepository.findByProjectIdOrderByPositionAsc(projectId).stream()
                .map(CustomFieldDefinitionResponse::from).toList();
    }

    @Transactional
    public CustomFieldDefinitionResponse createForTeam(UUID teamId, CustomFieldDefinitionRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));

        CustomFieldDefinition def = buildDefinition(request);
        def.setTeam(team);
        def = fieldRepository.save(def);
        log.info("CustomField oluşturuldu: {} (team={})", def.getName(), teamId);
        return CustomFieldDefinitionResponse.from(def);
    }

    @Transactional
    public CustomFieldDefinitionResponse createForProject(UUID projectId, CustomFieldDefinitionRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + projectId));

        CustomFieldDefinition def = buildDefinition(request);
        def.setProject(project);
        def = fieldRepository.save(def);
        log.info("CustomField oluşturuldu: {} (project={})", def.getName(), projectId);
        return CustomFieldDefinitionResponse.from(def);
    }

    @Transactional
    public CustomFieldDefinitionResponse update(UUID fieldId, CustomFieldDefinitionRequest request) {
        CustomFieldDefinition def = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Alan bulunamadı: " + fieldId));

        if (request.name() != null) def.setName(request.name());
        if (request.fieldType() != null) def.setFieldType(request.fieldType());
        if (request.options() != null) def.setOptions(request.options());
        if (request.isRequired() != null) def.setIsRequired(request.isRequired());
        if (request.isVisible() != null) def.setIsVisible(request.isVisible());
        if (request.issueTypes() != null) def.setIssueTypes(request.issueTypes());
        if (request.defaultValue() != null) def.setDefaultValue(request.defaultValue());
        if (request.position() != null) def.setPosition(request.position());

        def = fieldRepository.save(def);
        return CustomFieldDefinitionResponse.from(def);
    }

    @Transactional
    public void delete(UUID fieldId) {
        CustomFieldDefinition def = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Alan bulunamadı: " + fieldId));
        fieldRepository.delete(def);
        log.info("CustomField silindi: {}", fieldId);
    }

    private CustomFieldDefinition buildDefinition(CustomFieldDefinitionRequest request) {
        return CustomFieldDefinition.builder()
                .name(request.name())
                .fieldKey(request.fieldKey())
                .fieldType(request.fieldType())
                .options(request.options())
                .isRequired(request.isRequired() != null ? request.isRequired() : false)
                .isVisible(request.isVisible() != null ? request.isVisible() : true)
                .issueTypes(request.issueTypes() != null ? request.issueTypes() : new ArrayList<>())
                .defaultValue(request.defaultValue())
                .position(request.position() != null ? request.position() : 0)
                .build();
    }
}

