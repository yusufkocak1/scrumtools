package com.scrumtools.service.collab;

import com.scrumtools.entity.CollabDocument;
import com.scrumtools.entity.User;
import com.scrumtools.entity.enums.Permission;
import com.scrumtools.entity.enums.SystemRole;
import com.scrumtools.repository.ProjectMemberRepository;
import com.scrumtools.service.DocPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Ortak çalışma dokümanlarının yetki kararları (COLLAB_WORKSPACE_PLAN.md §11).
 *
 * <p>Kapsam projedir (plan D4): kullanıcı projenin üyesi olmalı ve rolünde ilgili
 * {@code COLLAB_*} izni bulunmalıdır. Takım alanı yalnızca etikettir, yetkiye
 * etki etmez.
 *
 * <p><b>Faz 2 notu:</b> doküman bir {@code DocPage}'e bağlandığında karar
 * {@code DocPermissionService}'e devredilecek — aynı içerik için iki ayrı yetki
 * ağacı tutmak, er ya da geç birinin diğerinden sızmasıyla biter.
 */
@Service
@RequiredArgsConstructor
public class CollabPermissionService {

    private final ProjectMemberRepository projectMemberRepository;
    private final DocPermissionService docPermissionService;

    public boolean canRead(CollabDocument document, User user) {
        if (document.getDocPage() != null) {
            return docPermissionService.hasReadAccess(
                    document.getDocPage().getSpace(), document.getDocPage(), user);
        }
        return canReadInProject(document.getProject().getId(), user);
    }

    /**
     * Liste ekranı için: doküman bazlı değil proje bazlı okuma yetkisi.
     * Faz 1'de dokümanlar arasında yetki farkı yok — kapsam projedir.
     */
    public boolean canReadInProject(UUID projectId, User user) {
        return has(projectId, user, Permission.COLLAB_READ);
    }

    public boolean canWrite(CollabDocument document, User user) {
        if (document.isArchived()) return false;
        if (document.getDocPage() != null) {
            // Docs'a bağlı dokümanda tek yetki kaynağı Docs'tur (plan Y1): sayfayı
            // düzenleyemeyen biri, "ortak düzenleme" kapısından girip aynı sayfayı
            // değiştirebilseydi Docs'un izin ağacı anlamsızlaşırdı.
            return docPermissionService.hasWriteAccess(
                    document.getDocPage().getSpace(), document.getDocPage(), user);
        }
        return has(document.getProject().getId(), user, Permission.COLLAB_WRITE);
    }

    public void checkRead(CollabDocument document, User user) {
        if (!canRead(document, user)) {
            throw new SecurityException("Bu dokümanı görüntüleme yetkiniz yok");
        }
    }

    public void checkWrite(CollabDocument document, User user) {
        if (document.isArchived()) {
            throw new SecurityException("Arşivlenmiş doküman düzenlenemez");
        }
        if (!canWrite(document, user)) {
            throw new SecurityException("Bu dokümanı düzenleme yetkiniz yok");
        }
    }

    /** Oluşturma, arşivleme, Docs'a bağlama. */
    public void checkManage(UUID projectId, User user) {
        if (!has(projectId, user, Permission.COLLAB_MANAGE)) {
            throw new SecurityException("Doküman yönetimi için COLLAB_MANAGE yetkisine ihtiyacınız var");
        }
    }

    /** Makro çalıştırma / yönetme (§9.2). Doküman değil <b>proje</b> kapsamlıdır. */
    public boolean hasProjectPermission(UUID projectId, User user, Permission permission) {
        return has(projectId, user, permission);
    }

    private boolean has(UUID projectId, User user, Permission permission) {
        if (user == null) return false;
        if (user.getSystemRole() == SystemRole.SUPER_ADMIN) return true;
        return projectMemberRepository.findByProjectIdAndUserEmail(projectId, user.getEmail())
                .map(member -> member.hasPermission(permission))
                .orElse(false);
    }
}
