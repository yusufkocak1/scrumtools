# 🚀 ScrumTools — Jira Alternatifi Geliştirme Planı

> **Tarih:** 2026-05-14  
> **Versiyon:** 2.1  
> **Durum:** Aktif Geliştirme

---

## 📋 İçindekiler

1. [Mevcut Durum Analizi](#1-mevcut-durum-analizi)
2. [Faz 1 — Gelişmiş Kullanıcı & Rol Yönetimi](#2-faz-1--gelişmiş-kullanıcı--rol-yönetimi)
3. [Faz 2 — Workflow Engine](#3-faz-2--workflow-engine)
4. [Faz 3 — Gelişmiş Task Yönetimi](#4-faz-3--gelişmiş-task-yönetimi)
5. [Faz 4 — Board & Görünüm](#5-faz-4--board--görünüm)
6. [Faz 5 — Bildirim & Aktivite](#6-faz-5--bildirim--aktivite)
7. [Faz 6 — Raporlama & Dashboard](#7-faz-6--raporlama--dashboard)
8. [Faz 7-8 — Webhook, Zaman, SLA](#8-faz-7-8--webhook-zaman-sla)
9. [API Endpoint Planı](#9-api-endpoint-planı)
10. [Teknik Mimari](#10-teknik-mimari)
11. [Uygulama Yol Haritası](#11-uygulama-yol-haritası)

---

## 1. Mevcut Durum Analizi

### ✅ Tamamlanan (2026-05-14 itibarıyla)

#### Backend
| Katman | Durum |
|--------|-------|
| `User` entity (avatarUrl, systemRole, status, phone, timezone, locale) | ✅ Tamamlandı |
| `Organization`, `OrganizationMember` entity | ✅ Tamamlandı |
| `Project`, `ProjectMember` entity | ✅ Tamamlandı |
| `Role`, `Permission` enum, `PermissionService` | ✅ Tamamlandı |
| `Invitation` entity & servisi | ✅ Tamamlandı |
| `ProjectSecurityEvaluator` | ✅ Tamamlandı |
| `DataInitializer` (varsayılan roller + Scrum Workflow) | ✅ Tamamlandı |
| `Team.organization` → NOT NULL (zorunlu bağlantı) | ✅ Tamamlandı |
| `TeamMember.user` FK (nullable, backward compat) | ✅ Tamamlandı |
| `TaskAttachment` + MinIO entegrasyonu | ✅ Tamamlandı |
| `Workflow`, `WorkflowStatus`, `WorkflowTransition` entity | ✅ Tamamlandı |
| `WorkflowService`, `WorkflowEngine`, `WorkflowController` | ✅ Tamamlandı |
| `Task` - parentTask (subtask), reporter, dueDate, startDate, estimatedHours, loggedHours, watchers, environment, resolution, position | ✅ Tamamlandı |
| `TaskLink` entity + `TaskLinkService` + `TaskLinkController` | ✅ Tamamlandı |
| `TaskHistory` entity + `AuditService` | ✅ Tamamlandı |
| `CustomFieldDefinition` entity + `CustomFieldService` + controller | ✅ Tamamlandı |
| `TaskService` - AuditService entegrasyonu, subtask & watcher desteği | ✅ Tamamlandı |
| Flyway V3 migration (team_members.user_id, task yeni alanlar, workflow, task_history, task_links, custom_fields) | ✅ Tamamlandı |
| `Board` entity + `BoardService` + `BoardController` | ✅ Tamamlandı |
| `TaskFilterService` (JPA Criteria API) | ✅ Tamamlandı |
| Flyway V4 migration (`boards` tablosu) | ✅ Tamamlandı |
| `Notification` entity + `NotificationService` + `NotificationController` | ✅ Tamamlandı |
| `ActivityEvent` entity + `ActivityService` | ✅ Tamamlandı |
| `NotificationType`, `ActivityAction` enum | ✅ Tamamlandı |
| `TaskService` → bildirim & aktivite tetikleyicileri | ✅ Tamamlandı |
| `@EnableAsync` (ScrumToolsApplication) | ✅ Tamamlandı |
| Flyway V5 migration (`notifications`, `activity_events` tabloları) | ✅ Tamamlandı |

#### Frontend
| Bileşen | Durum |
|---------|-------|
| `OrganizationApi.js`, `ProjectApi.js`, `RoleApi.js`, `InvitationApi.js`, `UserApi.js` | ✅ Tamamlandı |
| `WorkflowApi.js` | ✅ Tamamlandı |
| `WorkApi.js` - subtask, link, history, watcher, custom field metodları | ✅ Tamamlandı |
| `useAuth.js`, `usePermissions.js` | ✅ Tamamlandı |
| `v-permission.js` directive | ✅ Tamamlandı |
| `organization/` bileşenleri (OrgSwitcher, OrgMemberList, OrgSettings) | ✅ Tamamlandı |
| `project/` bileşenleri (ProjectList, ProjectMemberManager, ProjectSettings) | ✅ Tamamlandı |
| `roles/` bileşenleri (RoleManager, RoleEditor, PermissionMatrix) | ✅ Tamamlandı |
| `invitation/` bileşenleri (InviteModal, PendingInvitations) | ✅ Tamamlandı |
| `StatusBadge.vue` (workflow kategori renk desteği) | ✅ Tamamlandı |
| `SubtaskList.vue` (ekleme, ilerleme çubuğu) | ✅ Tamamlandı |
| `TaskLinkList.vue` (ilişki ekleme/silme) | ✅ Tamamlandı |
| `TaskHistory.vue` (audit log görüntüleme) | ✅ Tamamlandı |
| `WatcherList.vue` (izle/bırak) | ✅ Tamamlandı |
| `CustomFieldRenderer.vue` (TEXT, NUMBER, DATE, SELECT, MULTI_SELECT, CHECKBOX, URL) | ✅ Tamamlandı |
| `TaskDetail.vue` - yeni bileşenlerle entegrasyon | ✅ Tamamlandı |
| `AttachmentList.vue` | ✅ Tamamlandı |
| Pages: OrganizationDashboard, ProjectDashboard, UserProfile, AdminPanel | ✅ Tamamlandı |
| `BoardApi.js`, `useTaskFilter.js` | ✅ Tamamlandı |
| `BoardView.vue`, `BoardColumn.vue`, `ListView.vue`, `Backlog.vue` | ✅ Tamamlandı |
| `FilterBar.vue`, `FilterBuilder.vue`, `FilterChip.vue` | ✅ Tamamlandı |
| `ViewSwitcher.vue` (Board / Liste / Backlog / **Aktivite**) | ✅ Tamamlandı |
| `WorkList.vue` — çoklu görünüm + board seçici + aktivite akışı | ✅ Tamamlandı |
| `NotificationApi.js` | ✅ Tamamlandı |
| `useNotifications.js` composable (WebSocket STOMP + REST) | ✅ Tamamlandı |
| `NotificationBell.vue` (navbar badge) | ✅ Tamamlandı |
| `NotificationPanel.vue` (açılır bildirim listesi) | ✅ Tamamlandı |
| `ActivityFeed.vue` (takım aktivite akışı) | ✅ Tamamlandı |
| `Navbar.vue` — NotificationBell entegrasyonu | ✅ Tamamlandı |
| `AddTaskForm.vue` — dueDate, startDate, estimatedHours, environment, reporter, parentTaskId | ✅ Tamamlandı |

---

## 2. Faz 1 — Gelişmiş Kullanıcı & Rol Yönetimi

**Durum: ✅ TAMAMLANDI**

Tüm entity'ler, servisler, controller'lar ve frontend bileşenleri implement edildi.

**Kalan ince borçlar:**
- [x] ~~`TeamApi.js` prefix tutarsızlığı~~ → ✅ Düzeltildi
- [ ] Email doğrulama akışı (emailVerified flag var, email gönderimi yok)

---

## 3. Faz 2 — Workflow Engine

**Durum: ✅ TAMAMLANDI (Temel)**

### Yapılanlar
- `Workflow`, `WorkflowStatus`, `WorkflowTransition` entity'leri
- `WorkflowService` (CRUD + status/transition yönetimi)
- `WorkflowEngine` (geçiş validasyonu + aksiyon yürütme)
- `WorkflowController` (REST API)
- Scrum Workflow şablonu `DataInitializer`'da seed edildi
- `StatusBadge.vue` bileşeni

### Kalan Çalışmalar
- [ ] `WorkflowDesigner.vue` — görsel workflow editörü (drag-drop)
- [ ] `TransitionDialog.vue` — geçiş sırasında açılan dialog (yorum/koşul kontrol)
- [ ] `WorkflowPreview.vue` — salt okunur önizleme
- [ ] Task status değişikliğini WorkflowEngine üzerinden zorunlu geçirme (şu an bypass edilebilir)
- [ ] Kanban ve Bug Tracking varsayılan workflow şablonları

---

## 4. Faz 3 — Gelişmiş Task Yönetimi

**Durum: ✅ TAMAMLANDI (Temel)**

### Yapılanlar
- `Task` entity'ye: `parentTask`, `reporter`, `dueDate`, `startDate`, `estimatedHours`, `loggedHours`, `environment`, `resolution`, `resolvedAt`, `position`, `watchers`
- `TaskLink` entity + `LinkType` enum + `TaskLinkService` + `TaskLinkController`
- `TaskHistory` entity + `AuditService` (status, priority, assignee, title audit)
- `CustomFieldDefinition` entity + `CustomFieldType` enum + `CustomFieldService` + `CustomFieldController`
- `TaskService`: subtask, watcher API, AuditService entegrasyonu, reporter otomatik atama
- Frontend: `SubtaskList.vue`, `TaskLinkList.vue`, `TaskHistory.vue`, `WatcherList.vue`, `CustomFieldRenderer.vue`
- `TaskDetail.vue` entegrasyonu
- `AddTaskForm.vue` → dueDate, startDate, estimatedHours, environment, reporter, parentTaskId eklendi

### Kalan Çalışmalar
- [ ] `CustomFieldManager.vue` — admin panel'de alan tanımları yönetimi
- [ ] `CustomFieldEditor.vue` — alan değeri düzenleme paneli
- [ ] Gelişmiş yorum: `parentComment` (reply), mention (@user), reactions
- [ ] Epic metadata (color, startDate, endDate, targetVersion, progress computed)
- [x] ~~`AddTaskForm.vue`'ya dueDate, estimatedHours, parentTask seçimi eklenmesi~~ → ✅ Tamamlandı

---

## 5. Faz 4 — Board & Görünüm Katmanı

**Durum: ✅ TAMAMLANDI (Temel)**  
**Öncelik:** 🟠 Yüksek | **Bağımlılık:** Faz 1, 2, 3

### Yapılanlar

#### Backend
| Görev | Durum |
|-------|-------|
| `Board` entity (boardType, columnConfig JSONB, swimlaneConfig JSONB, cardConfig JSONB) | ✅ |
| `BoardType` enum (SCRUM / KANBAN) | ✅ |
| `BoardRepository`, `BoardService`, `BoardController` | ✅ |
| `TaskFilterCriteria`, `TaskFilterRequest` DTO | ✅ |
| `TaskFilterService` (JPA Criteria API, field/operator/values) | ✅ |
| `POST /api/teams/{id}/tasks/filter` endpoint | ✅ |
| Flyway V4 migration (`boards` tablosu) | ✅ |

#### Frontend
| Görev | Durum |
|-------|-------|
| `BoardApi.js` (Board CRUD + filterTasks) | ✅ |
| `WorkApi.js` — filterTasks metodu eklendi | ✅ |
| `useTaskFilter.js` composable | ✅ |
| `FilterChip.vue` | ✅ |
| `FilterBuilder.vue` (modal, alan/operatör/değer) | ✅ |
| `FilterBar.vue` (hızlı filtreler + chip'ler) | ✅ |
| `BoardColumn.vue` (WIP limit, HTML5 DnD) | ✅ |
| `BoardView.vue` (Kanban board, swimlane-ready) | ✅ |
| `ListView.vue` (tablo görünümü, client+server sort/page) | ✅ |
| `ViewSwitcher.vue` (Board / Liste / Backlog / Aktivite) | ✅ |
| `WorkList.vue` — çoklu görünüm + board seçici + board oluşturma | ✅ |

### Kalan Çalışmalar
- [ ] `BoardSettingsPanel.vue` — sütun adı/renk/WIP limit düzenleme
- [ ] Swimlane görünümü (groupBy: assignee/priority)
- [ ] Sprint bazlı board filtresi (aktif sprint'i otomatik seç)
- [ ] Kanban + Bug Tracking varsayılan workflow seed'leri (DataInitializer)

---

## 6. Faz 5 — Bildirim & Aktivite Sistemi

**Durum: ✅ TAMAMLANDI (Temel)**  
**Öncelik:** 🟡 Orta | **Bağımlılık:** Faz 1, 3

### Yapılanlar

#### Backend
| Görev | Durum |
|-------|-------|
| `Notification` entity (`recipientEmail`, `type` enum, `title`, `message`, `entityType`, `entityId`, `data` JSONB, `isRead`) | ✅ |
| `ActivityEvent` entity (`actorEmail`, `action` enum, `entityType`, `entityId`, `team` FK, `details` JSONB) | ✅ |
| `NotificationType` enum (TASK_ASSIGNED, TASK_COMMENTED, TASK_STATUS_CHANGED, WATCHED_TASK_UPDATED, …) | ✅ |
| `ActivityAction` enum (TASK_CREATED/UPDATED/COMMENTED, SPRINT_*, MEMBER_*, BOARD_*) | ✅ |
| `NotificationRepository`, `ActivityEventRepository` | ✅ |
| `NotificationService` (@Async push + REST CRUD) | ✅ |
| `ActivityService` (@Async record + WebSocket push) | ✅ |
| `NotificationController` (`/api/notifications/**`, `/api/teams/{id}/activity`) | ✅ |
| `TaskService` → bildirim tetikleyicileri (atama, yorum, status, watcher) | ✅ |
| `TaskService` → aktivite kaydı (create, update, comment) | ✅ |
| `@EnableAsync` — ScrumToolsApplication | ✅ |
| Flyway V5 migration (`notifications`, `activity_events` tabloları) | ✅ |

#### Frontend
| Bileşen | Durum |
|---------|-------|
| `NotificationApi.js` (getNotifications, getUnreadCount, markRead, markAllRead, getTeamActivity) | ✅ |
| `useNotifications.js` composable (REST + WebSocket STOMP, unread count, mark read) | ✅ |
| `NotificationBell.vue` (navbar badge, panel toggle) | ✅ |
| `NotificationPanel.vue` (liste, okundu işareti, yönlendirme) | ✅ |
| `ActivityFeed.vue` (takım aktivite akışı, emoji ikonlar, timeAgo) | ✅ |
| `Navbar.vue` — NotificationBell entegrasyonu | ✅ |
| `ViewSwitcher.vue` — "Aktivite" sekmesi eklendi | ✅ |
| `WorkList.vue` — aktivite görünümü + ActivityFeed entegrasyonu | ✅ |
| `AddTaskForm.vue` — dueDate, startDate, estimatedHours, environment, reporter, parentTaskId alanları | ✅ |

### Kalan Çalışmalar
- [ ] `INVITATION_RECEIVED` bildirimi — InvitationService'e entegrasyon
- [ ] Sprint başlatma/tamamlama aktivite kaydı
- [ ] Bildirim tercihleri (kullanıcı bazlı disable)

---

## 7. Faz 6 — Raporlama & Dashboard

**Durum: ✅ TAMAMLANDI (Temel)**  
**Öncelik:** 🟡 Orta | **Bağımlılık:** Faz 1, 2, 3

### Yapılanlar

#### Backend
| Görev | Durum |
|-------|-------|
| `BurndownReportDto`, `SprintVelocityDto`, `WorkloadDto`, `DistributionItemDto`, `TeamSummaryDto`, `CreatedVsResolvedDto` DTO'ları | ✅ |
| `TaskRepository` rapor sorguları (countByStatus/Priority/Type, findOverdue, findBySprintId, workloadByAssignee, createdPerDay, resolvedPerDay) | ✅ |
| `UserDashboard` entity (user FK, layout JSONB) | ✅ |
| `UserDashboardRepository` | ✅ |
| `ReportService` (summary, burndown, velocity, workload, createdVsResolved, overdue) | ✅ |
| `DashboardService` (getLayout, saveLayout, upsertWidget, removeWidget) | ✅ |
| `ReportController` (`/api/teams/{id}/reports/**`) | ✅ |
| `DashboardController` (`/api/dashboards/**`) | ✅ |
| Flyway V6 migration (`user_dashboards` tablosu) | ✅ |

#### Frontend
| Bileşen | Durum |
|---------|-------|
| `chart.js` + `vue-chartjs` kurulumu | ✅ |
| `ReportApi.js` (summary, burndown, velocity, workload, createdVsResolved, overdue) | ✅ |
| `DashboardApi.js` (getLayout, saveLayout, upsertWidget, removeWidget) | ✅ |
| `StatCard.vue` (özet kart) | ✅ |
| `SummaryWidget.vue` (takım özeti, dağılım, overdue uyarısı) | ✅ |
| `BurndownWidget.vue` (Line chart, sprint seçici) | ✅ |
| `VelocityWidget.vue` (Bar chart, son 10 sprint) | ✅ |
| `WorkloadWidget.vue` (horizontal Bar chart) | ✅ |
| `CreatedVsResolvedWidget.vue` (Line chart, gün filtresi) | ✅ |
| `OverdueWidget.vue` (tablo) | ✅ |
| `Dashboard.vue` sayfası (takım seçici, widget grid, add/remove, layout kaydet) | ✅ |
| `router.js` — `/dashboard` rotası | ✅ |
| `SideBar.vue` — Dashboard butonu | ✅ |

### Kalan Çalışmalar
- [ ] `vue-grid-layout` ile drag-drop yeniden boyutlandırma (şu an statik grid)
- [ ] Dashboard widget boyutu (w/h) ayarı
- [ ] Birden fazla takımı aynı dashboard'da karşılaştırma

---

## 8. Faz 7-8 — Webhook, Zaman, SLA

**Durum: ⬜ PLANLANMADI**  
**Öncelik:** 🟢 Düşük | **Tahmin:** 5-6 hafta | **Bağımlılık:** Faz 3+

- **Faz 7:** `Webhook` entity, event yayını, HMAC imzalama
- **Faz 8:** `TimeLog` entity, `SlaRule` entity, time tracking UI

---

## 9. API Endpoint Planı

### Mevcut Endpoint'ler

| Method | Path | Faz | Durum |
|--------|------|-----|-------|
| `POST/GET` | `/api/auth/register`, `/login`, `/me` | - | ✅ |
| `GET/PUT` | `/api/users/profile` | Faz 1 | ✅ |
| `*` | `/api/organizations/**` | Faz 1 | ✅ |
| `*` | `/api/projects/**` | Faz 1 | ✅ |
| `*` | `/api/roles/**` | Faz 1 | ✅ |
| `*` | `/api/invitations/**` | Faz 1 | ✅ |
| `*` | `/api/teams/**` | - | ✅ |
| `*` | `/api/teams/{id}/tasks/**` | - | ✅ |
| `GET/POST` | `/api/teams/{id}/tasks/{tid}/subtasks` | Faz 3 | ✅ |
| `*` | `/api/teams/{id}/tasks/{tid}/links` | Faz 3 | ✅ |
| `GET` | `/api/teams/{id}/tasks/{tid}/history` | Faz 3 | ✅ |
| `*` | `/api/teams/{id}/tasks/{tid}/watchers` | Faz 3 | ✅ |
| `*` | `/api/teams/{id}/tasks/{tid}/attachments` | Faz 3 | ✅ |
| `*` | `/api/teams/{id}/workflows` | Faz 2 | ✅ |
| `*` | `/api/projects/{id}/workflows` | Faz 2 | ✅ |
| `*` | `/api/workflows/{id}/**` | Faz 2 | ✅ |
| `*` | `/api/teams/{id}/custom-fields` | Faz 3 | ✅ |
| `*` | `/api/custom-fields/{id}` | Faz 3 | ✅ |
| `POST` | `/api/teams/{id}/tasks/filter` | Faz 4 | ✅ |
| `*` | `/api/teams/{id}/boards` | Faz 4 | ✅ |
| `GET/PATCH/POST` | `/api/notifications/**` | Faz 5 | ✅ |
| `GET` | `/api/notifications/unread-count` | Faz 5 | ✅ |
| `GET` | `/api/teams/{id}/activity` | Faz 5 | ✅ |

### Planlanan Endpoint'ler

| Method | Path | Faz |
|--------|------|-----|
| `*` | `/api/teams/{id}/reports/**` | Faz 6 |
| `*` | `/api/dashboards/**` | Faz 6 |
| `*` | `/api/webhooks/**` | Faz 7 |
| `*` | `/api/teams/{id}/tasks/{tid}/time-log` | Faz 8 |

---

## 10. Teknik Mimari

### Backend
| Karar | Seçim |
|-------|-------|
| Framework | Spring Boot 3.5 |
| DB | PostgreSQL + JSONB |
| ORM | Hibernate/JPA |
| Auth | JWT + @PreAuthorize |
| WebSocket | STOMP over SockJS |
| File Storage | MinIO (entegre) |
| Migration | Flyway (V1–V5) |
| Async | @EnableAsync + @Async |
| Java | 25 |

### Frontend
| Karar | Seçim |
|-------|-------|
| Framework | Vue 3 |
| Styling | Tailwind CSS |
| HTTP | Axios |
| Router | Vue Router |
| Drag & Drop | Mevcut HTML5 |
| Real-time | STOMP over SockJS |
| Charts | Faz 6'da eklenecek (vue-chartjs) |

---

## 11. Uygulama Yol Haritası

```
┌──────────────────────────────────────────────────────────────────┐
│                     GÜNCEL YOL HARİTASI (2026-05-14)             │
├──────────┬─────────────────────────────────────────┬─────────────┤
│ Zaman    │ Faz                                     │ Durum       │
├──────────┼─────────────────────────────────────────┼─────────────┤
│ Hf 1-4   │ Faz 1: Kullanıcı & Rol Yönetimi         │ ✅ Tamamlandı│
├──────────┼─────────────────────────────────────────┼─────────────┤
│ Hf 5-8   │ Faz 2: Workflow Engine (Temel)           │ ✅ Tamamlandı│
│          │  ├─ WorkflowDesigner.vue                 │ ⬜ Bekliyor  │
│          │  └─ TransitionDialog.vue                 │ ⬜ Bekliyor  │
├──────────┼─────────────────────────────────────────┼─────────────┤
│ Hf 9-13  │ Faz 3: Gelişmiş Task Yönetimi (Temel)   │ ✅ Tamamlandı│
│          │  ├─ CustomFieldManager.vue               │ ⬜ Bekliyor  │
│          │  ├─ Gelişmiş yorum (reply/mention)       │ ⬜ Bekliyor  │
│          │  └─ AddTaskForm güncellemesi              │ ✅ Tamamlandı│
├──────────┼─────────────────────────────────────────┼─────────────┤
│ Hf 14-17 │ Faz 4: Board & Görünüm                  │ ✅ Tamamlandı│
│          │  ├─ Board entity + API                   │ ✅          │
│          │  ├─ Filter/Search engine                 │ ✅          │
│          │  ├─ BoardView.vue / ListView.vue         │ ✅          │
│          │  └─ FilterBuilder.vue                    │ ✅          │
├──────────┼─────────────────────────────────────────┼─────────────┤
│ Hf 18-20 │ Faz 5: Bildirim & Aktivite              │ ✅ Tamamlandı│
│          │  ├─ Notification entity + API            │ ✅          │
│          │  ├─ ActivityEvent entity + API           │ ✅          │
│          │  ├─ WebSocket push (bildirim + aktivite) │ ✅          │
│          │  ├─ NotificationBell.vue + Panel         │ ✅          │
│          │  └─ ActivityFeed.vue                     │ ✅          │
├──────────┼─────────────────────────────────────────┼─────────────┤
│ Hf 21-24 │ Faz 6: Raporlama & Dashboard            │ ⬜ Planlandı │
├──────────┼─────────────────────────────────────────┼─────────────┤
│ Hf 25-30 │ Faz 7-8: Webhook, Zaman, SLA            │ ⬜ Planlandı │
└──────────┴─────────────────────────────────────────┴─────────────┘
```

---

## ⚠️ Önemli Notlar

### Açık Borçlar

| # | Borç | Öncelik |
|---|------|---------|
| 1 | `WorkflowDesigner.vue` — görsel drag-drop editörü | 🟡 Orta |
| 2 | `CustomFieldManager.vue` — admin alan tanımı yönetimi | 🟡 Orta |
| 3 | `TransitionDialog.vue` — workflow geçiş koşul dialogu | 🟡 Orta |
| 4 | Kanban + Bug Tracking varsayılan workflow seed'i | 🟡 Orta |
| 5 | `INVITATION_RECEIVED` bildirimi — InvitationService entegrasyonu | 🟢 Düşük |
| 6 | Sprint başlatma/tamamlama aktivite kaydı | 🟢 Düşük |
| 7 | Bildirim tercihleri (kullanıcı bazlı disable) | 🟢 Düşük |
| 8 | Email doğrulama akışı (emailVerified flag var, gönderim yok) | 🟢 Düşük |
| 9 | `BoardSettingsPanel.vue` — sütun adı/renk/WIP limit düzenleme | 🟢 Düşük |

### ✅ Kapatılan Borçlar
- ~~`AddTaskForm.vue` — dueDate, estimatedHours, parentTask, reporter UI'a ekle~~ → **Tamamlandı**
- ~~`TeamApi.js` prefix tutarsızlığı~~ → **Tamamlandı**
- ~~Flyway V5 migration~~ → **Tamamlandı**

### Migration Durumu
| Versiyon | İçerik | Durum |
|----------|--------|-------|
| V1 | Baseline schema | ✅ |
| V2 | member_type eklenmesi | ✅ |
| V3 | team_members.user_id, task yeni alanlar, workflow, task_history, task_links, custom_fields | ✅ |
| V4 | boards tablosu | ✅ |
| V5 | notifications + activity_events tabloları | ✅ |

> **Bu doküman yaşayan bir dokümandır.**  
> Son güncelleme: 2026-05-14
