package com.scrumtools.service;

import com.scrumtools.dto.*;
import com.scrumtools.entity.*;
import com.scrumtools.entity.enums.SupportCategory;
import com.scrumtools.entity.enums.SupportTicketStatus;
import com.scrumtools.entity.enums.SystemRole;
import com.scrumtools.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final SupportTicketAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final ErrorTrackingService errorTrackingService;

    /** Maksimum tek dosya boyutu: 20MB */
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

    /** Talep başına maksimum ek sayısı */
    private static final int MAX_ATTACHMENTS_PER_TICKET = 5;

    // ─── Kullanıcı Tarafı ─────────────────────────────────────────────────────

    @Transactional
    public SupportTicketResponse createTicket(String userEmail, SupportTicketCreateRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı."));

        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory())
                .status(SupportTicketStatus.OPEN)
                .errorTrackingCode(normalizeTrackingCode(request.getErrorTrackingCode()))
                .build();

        // Organizasyon snapshot'ı — admin panelinde filtreleme için
        organizationMemberRepository.findActiveByUserEmail(userEmail).stream()
                .findFirst()
                .ifPresent(om -> {
                    ticket.setOrganizationId(om.getOrganization().getId());
                    ticket.setOrganizationName(om.getOrganization().getName());
                });

        SupportTicket saved = ticketRepository.save(ticket);
        log.info("Destek talebi oluşturuldu: {} ({}) — {}", saved.getId(), saved.getCategory(), userEmail);

        notifyAdmins(userEmail, admins -> admins.forEach(admin ->
                notificationService.notifySupportTicketCreated(
                        admin.getEmail(), user.getName(), saved.getSubject(), saved.getId().toString())));

        return SupportTicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getMyTickets(String userEmail) {
        return ticketRepository.findByUserEmailOrderByUpdatedAtDesc(userEmail).stream()
                .map(SupportTicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportTicketDetailResponse getMyTicketDetail(String userEmail, UUID ticketId) {
        SupportTicket ticket = getOwnedTicket(userEmail, ticketId);
        return buildDetail(ticket, false);
    }

    @Transactional
    public SupportMessageResponse addUserMessage(String userEmail, UUID ticketId, SupportMessageRequest request) {
        SupportTicket ticket = getOwnedTicket(userEmail, ticketId);
        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new IllegalStateException("Kapalı talebe mesaj eklenemez.");
        }

        User user = ticket.getUser();
        SupportTicketMessage message = messageRepository.save(SupportTicketMessage.builder()
                .ticket(ticket)
                .authorEmail(userEmail)
                .authorName(user.getName())
                .isAdminReply(false)
                .content(request.getContent())
                .build());

        // Kullanıcı yanıt verdiyse talep tekrar destek ekibinin kuyruğuna düşer
        if (ticket.getStatus() == SupportTicketStatus.WAITING_USER) {
            ticket.setStatus(SupportTicketStatus.OPEN);
        }
        ticketRepository.save(ticket);

        notifyAdmins(userEmail, admins -> admins.forEach(admin ->
                notificationService.notifySupportTicketReplied(
                        admin.getEmail(), user.getName(), ticket.getSubject(),
                        ticket.getId().toString(), false)));

        return SupportMessageResponse.from(message);
    }

    // ─── Ekler ────────────────────────────────────────────────────────────────

    @Transactional
    public SupportAttachmentResponse uploadAttachment(String userEmail, UUID ticketId, MultipartFile file) {
        SupportTicket ticket = getOwnedTicket(userEmail, ticketId);
        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new IllegalStateException("Kapalı talebe dosya eklenemez.");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dosya boyutu 20MB'ı aşamaz");
        }
        if (attachmentRepository.countByTicketId(ticketId) >= MAX_ATTACHMENTS_PER_TICKET) {
            throw new IllegalArgumentException("Bir talebe en fazla " + MAX_ATTACHMENTS_PER_TICKET + " dosya eklenebilir");
        }

        String objectKey = storageService.upload("support/" + ticketId, file);

        SupportTicketAttachment attachment = attachmentRepository.save(SupportTicketAttachment.builder()
                .ticket(ticket)
                .fileName(file.getOriginalFilename())
                .objectKey(objectKey)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploadedBy(userEmail)
                .build());

        log.info("Destek talebine dosya eklendi: {} → talep {}", attachment.getFileName(), ticketId);
        return SupportAttachmentResponse.from(attachment, storageService.getPresignedUrl(objectKey, 60));
    }

    /** İndirme: talep sahibi veya SUPER_ADMIN erişebilir. */
    @Transactional(readOnly = true)
    public SupportTicketAttachment getAttachmentForDownload(String userEmail, UUID ticketId, UUID attachmentId) {
        SupportTicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NoSuchElementException("Dosya bulunamadı."));
        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new NoSuchElementException("Dosya bu talebe ait değil.");
        }
        if (!attachment.getTicket().getUser().getEmail().equals(userEmail) && !isSuperAdmin(userEmail)) {
            throw new SecurityException("Bu dosyaya erişim yetkiniz yok.");
        }
        return attachment;
    }

    public InputStream downloadAttachment(SupportTicketAttachment attachment) {
        return storageService.download(attachment.getObjectKey());
    }

    @Transactional
    public void deleteAttachment(String userEmail, UUID ticketId, UUID attachmentId) {
        SupportTicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NoSuchElementException("Dosya bulunamadı."));
        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new NoSuchElementException("Dosya bu talebe ait değil.");
        }
        if (!attachment.getUploadedBy().equals(userEmail)
                || !attachment.getTicket().getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Sadece kendi yüklediğiniz dosyaları silebilirsiniz.");
        }

        storageService.delete(attachment.getObjectKey());
        attachmentRepository.delete(attachment);
        log.info("Destek talebi dosyası silindi: {}", attachment.getFileName());
    }

    // ─── Admin Tarafı ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getTicketsForAdmin(SupportTicketStatus status, SupportCategory category,
                                                          UUID organizationId, int page, int size) {
        return ticketRepository.findFiltered(status, category, organizationId, PageRequest.of(page, size))
                .map(SupportTicketResponse::from);
    }

    @Transactional(readOnly = true)
    public SupportTicketDetailResponse getTicketDetailForAdmin(UUID ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Destek talebi bulunamadı."));
        return buildDetail(ticket, true);
    }

    @Transactional
    public SupportMessageResponse addAdminReply(String adminEmail, UUID ticketId,
                                                SupportMessageRequest request, boolean setWaitingUser) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Destek talebi bulunamadı."));
        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new IllegalStateException("Kapalı talebe mesaj eklenemez.");
        }

        String adminName = userRepository.findByEmail(adminEmail)
                .map(User::getName).orElse("Destek Ekibi");

        SupportTicketMessage message = messageRepository.save(SupportTicketMessage.builder()
                .ticket(ticket)
                .authorEmail(adminEmail)
                .authorName(adminName)
                .isAdminReply(true)
                .content(request.getContent())
                .build());

        if (setWaitingUser) {
            ticket.setStatus(SupportTicketStatus.WAITING_USER);
        }
        ticketRepository.save(ticket);

        notificationService.notifySupportTicketReplied(
                ticket.getUser().getEmail(), "Destek Ekibi", ticket.getSubject(),
                ticket.getId().toString(), true);

        return SupportMessageResponse.from(message);
    }

    @Transactional
    public SupportTicketResponse updateStatus(String adminEmail, UUID ticketId, SupportTicketStatus status) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Destek talebi bulunamadı."));
        if (ticket.getStatus() == status) {
            return SupportTicketResponse.from(ticket);
        }

        ticket.setStatus(status);
        SupportTicket saved = ticketRepository.save(ticket);
        log.info("Destek talebi durumu değişti: {} → {} ({})", ticketId, status, adminEmail);

        notificationService.notifySupportTicketStatusChanged(
                saved.getUser().getEmail(), saved.getSubject(),
                saved.getId().toString(), statusLabel(status));

        return SupportTicketResponse.from(saved);
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────

    private SupportTicket getOwnedTicket(String userEmail, UUID ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Destek talebi bulunamadı."));
        if (!ticket.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Bu talebe erişim yetkiniz yok.");
        }
        return ticket;
    }

    private SupportTicketDetailResponse buildDetail(SupportTicket ticket, boolean includeErrorGroup) {
        List<SupportMessageResponse> messages = messageRepository
                .findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                .map(SupportMessageResponse::from)
                .toList();

        List<SupportAttachmentResponse> attachments = attachmentRepository
                .findByTicketIdOrderByCreatedAtDesc(ticket.getId()).stream()
                .map(a -> SupportAttachmentResponse.from(a, storageService.getPresignedUrl(a.getObjectKey(), 60)))
                .toList();

        ErrorGroupResponse linkedErrorGroup = null;
        if (includeErrorGroup && ticket.getErrorTrackingCode() != null) {
            linkedErrorGroup = errorTrackingService.findGroupByTrackingCode(ticket.getErrorTrackingCode());
        }

        return SupportTicketDetailResponse.builder()
                .ticket(SupportTicketResponse.from(ticket))
                .description(ticket.getDescription())
                .messages(messages)
                .attachments(attachments)
                .linkedErrorGroup(linkedErrorGroup)
                .build();
    }

    private void notifyAdmins(String actorEmail, java.util.function.Consumer<List<User>> action) {
        List<User> admins = userRepository.findBySystemRole(SystemRole.SUPER_ADMIN).stream()
                .filter(admin -> !admin.getEmail().equals(actorEmail))
                .toList();
        if (!admins.isEmpty()) {
            action.accept(admins);
        }
    }

    private boolean isSuperAdmin(String email) {
        return userRepository.findByEmail(email)
                .map(u -> u.getSystemRole() == SystemRole.SUPER_ADMIN)
                .orElse(false);
    }

    private String normalizeTrackingCode(String code) {
        if (code == null || code.isBlank()) return null;
        return code.trim().toUpperCase();
    }

    private String statusLabel(SupportTicketStatus status) {
        return switch (status) {
            case OPEN -> "Açık";
            case IN_PROGRESS -> "İnceleniyor";
            case WAITING_USER -> "Yanıt Bekleniyor";
            case RESOLVED -> "Çözüldü";
            case CLOSED -> "Kapatıldı";
        };
    }
}
