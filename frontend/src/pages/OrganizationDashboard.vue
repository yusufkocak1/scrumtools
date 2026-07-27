<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto p-4 sm:p-6">

      <!-- Yükleniyor -->
      <div v-if="orgsLoading && !currentOrg" class="flex items-center justify-center py-24">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>

      <!-- Organizasyon seçilmemiş -->
      <div v-else-if="!currentOrg" class="bg-white rounded-2xl border border-gray-200 shadow-sm text-center py-20 px-6">
        <div class="w-20 h-20 bg-indigo-50 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-10 h-10 text-indigo-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z" clip-rule="evenodd"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-800 mb-2">Organizasyon Seçin</h3>
        <p class="text-gray-500 mb-6">Devam etmek için bir organizasyon seçin veya yeni oluşturun.</p>
        <button @click="showCreateOrgModal = true" class="btn-primary">Organizasyon Oluştur</button>
      </div>

      <!-- Sidebar + içerik -->
      <div v-else class="flex flex-col lg:flex-row gap-6">

        <!-- ─── Sidebar ─────────────────────────────────────────────────── -->
        <aside class="lg:w-64 lg:flex-shrink-0">
          <div class="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden lg:sticky lg:top-6">
            <!-- Org kimliği — switcher hem gösterim hem geçiş, ayrı bir başlık tekrarı yok -->
            <div class="p-4 border-b border-gray-100 space-y-2.5">
              <OrgSwitcher @create-org="showCreateOrgModal = true" />
              <div class="flex items-center gap-2 px-0.5">
                <span class="text-xs text-gray-400 truncate flex-1">{{ currentOrg?.slug }}</span>
                <span class="text-xs px-2 py-0.5 rounded-full font-medium flex-shrink-0" :class="roleBadgeClass()">
                  {{ roleLabel() }}
                </span>
              </div>
            </div>

            <!-- Gruplu menü — yetkisi olmayan bölüm hiç görünmez -->
            <nav class="p-2">
              <template v-for="group in visibleGroups" :key="group.title">
                <p class="px-3 pt-3 pb-1 text-[11px] font-semibold text-gray-400 uppercase tracking-wide">
                  {{ group.title }}
                </p>
                <button
                  v-for="item in group.items"
                  :key="item.id"
                  @click="activeSection = item.id"
                  :class="[
                    'w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors mb-0.5',
                    activeSection === item.id
                      ? 'bg-indigo-50 text-indigo-700'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  ]"
                >
                  <span v-html="item.icon" class="w-4 h-4 flex-shrink-0"></span>
                  <span class="flex-1 text-left">{{ item.label }}</span>
                  <span
                    v-if="item.count != null"
                    :class="[
                      'text-xs px-1.5 py-0.5 rounded-full',
                      activeSection === item.id ? 'bg-indigo-100 text-indigo-700' : 'bg-gray-100 text-gray-500'
                    ]"
                  >{{ item.count }}</span>
                </button>
              </template>
            </nav>
          </div>
        </aside>

        <!-- ─── İçerik ──────────────────────────────────────────────────── -->
        <main class="flex-1 min-w-0">
          <div class="bg-white rounded-2xl border border-gray-200 shadow-sm p-5 sm:p-6">

            <!-- Projeler -->
            <div v-if="activeSection === 'projects'">
              <div class="flex flex-wrap items-start justify-between gap-3 mb-5">
                <div>
                  <h2 class="text-lg font-semibold text-gray-900">Projeler</h2>
                  <p class="text-sm text-gray-500 mt-0.5">Organizasyonun aktif projeleri</p>
                </div>
                <button v-if="can.createProject" @click="showCreateProjectModal = true" class="btn-primary flex items-center gap-1.5">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                  </svg>
                  Proje Oluştur
                </button>
              </div>
              <ProjectList
                :projects="projects"
                :loading="projectsLoading"
                @create="showCreateProjectModal = true"
                @select="goToProject"
              />
            </div>

            <!-- Takımlar -->
            <OrgTeamsPanel v-else-if="activeSection === 'teams'" :orgId="currentOrg.id" />

            <!-- Üyeler + Davetler -->
            <OrgMembersPanel
              v-else-if="activeSection === 'members'"
              :orgId="currentOrg.id"
              @changed="loadProjects(currentOrg.id)"
            />

            <!-- Abonelik -->
            <BillingTab v-else-if="activeSection === 'billing'" :org="currentOrg" />

            <!-- Entegrasyonlar -->
            <div v-else-if="activeSection === 'integrations'">
              <div class="mb-5">
                <h2 class="text-lg font-semibold text-gray-900">Entegrasyonlar</h2>
                <p class="text-sm text-gray-500 mt-0.5">Kod deposu ve derleme sunucusu bağlantıları</p>
              </div>
              <div class="flex gap-1 mb-5 border-b border-gray-200">
                <button
                  v-for="sub in integrationSubTabs"
                  :key="sub.id"
                  @click="integrationSubTab = sub.id"
                  :class="[
                    'px-4 py-2.5 text-sm font-medium -mb-px border-b-2 transition-colors',
                    integrationSubTab === sub.id
                      ? 'border-indigo-600 text-indigo-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  ]"
                >{{ sub.label }}</button>
              </div>
              <ScmConnectionsTab v-if="integrationSubTab === 'scm'" :org-id="currentOrg.id" />
              <CiConnectionsTab v-else :org-id="currentOrg.id" />
            </div>

            <!-- Ayarlar -->
            <OrgSettings
              v-else-if="activeSection === 'settings'"
              :org="currentOrg"
              @updated="upsertOrganization($event)"
            />
          </div>
        </main>
      </div>
    </div>

    <!-- Yeni Organizasyon -->
    <div v-if="showCreateOrgModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl border border-gray-200">
        <h4 class="text-lg font-semibold text-gray-900 mb-4">Yeni Organizasyon</h4>
        <form @submit.prevent="createOrg" class="space-y-4">
          <div>
            <label class="label">Ad</label>
            <input v-model="newOrg.name" type="text" required class="input-field" placeholder="Organizasyon adı" />
          </div>
          <div>
            <label class="label">Slug</label>
            <input v-model="newOrg.slug" type="text" required class="input-field font-mono" placeholder="org-slug" />
          </div>
          <div>
            <label class="label">Açıklama</label>
            <textarea v-model="newOrg.description" rows="2" class="input-field" placeholder="Opsiyonel"></textarea>
          </div>
          <div class="flex gap-2 justify-end pt-1">
            <button type="button" @click="showCreateOrgModal = false" class="btn-secondary">İptal</button>
            <button type="submit" :disabled="creatingOrg" class="btn-primary">
              {{ creatingOrg ? 'Oluşturuluyor...' : 'Oluştur' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Yeni Proje -->
    <div v-if="showCreateProjectModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl border border-gray-200">
        <h4 class="text-lg font-semibold text-gray-900 mb-4">Yeni Proje</h4>
        <form @submit.prevent="createProject" class="space-y-4">
          <div>
            <label class="label">Proje Adı</label>
            <input v-model="newProject.name" type="text" required class="input-field" placeholder="Proje adı" />
          </div>
          <div>
            <label class="label">Anahtar (KEY)</label>
            <input
              v-model="newProject.key" type="text" required maxlength="10"
              @input="newProject.key = $event.target.value.toUpperCase()"
              class="input-field font-mono uppercase" placeholder="SCRM"
            />
          </div>
          <div>
            <label class="label">Tip</label>
            <select v-model="newProject.projectType" class="input-field">
              <option value="SCRUM">Scrum</option>
              <option value="KANBAN">Kanban</option>
              <option value="BUG_TRACKING">Bug Tracking</option>
              <option value="CUSTOM">Özel</option>
            </select>
          </div>
          <div class="flex gap-2 justify-end pt-1">
            <button type="button" @click="showCreateProjectModal = false" class="btn-secondary">İptal</button>
            <button type="submit" :disabled="creatingProject" class="btn-primary">
              {{ creatingProject ? 'Oluşturuluyor...' : 'Oluştur' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { createToast } from 'mosha-vue-toastify'
import OrgSwitcher from '../components/organization/OrgSwitcher.vue'
import OrgSettings from '../components/organization/OrgSettings.vue'
import OrgMembersPanel from '../components/organization/OrgMembersPanel.vue'
import OrgTeamsPanel from '../components/organization/OrgTeamsPanel.vue'
import BillingTab from '../components/billing/BillingTab.vue'
import ScmConnectionsTab from '../components/scm/ScmConnectionsTab.vue'
import CiConnectionsTab from '../components/ci/CiConnectionsTab.vue'
import ProjectList from '../components/project/ProjectList.vue'
import OrganizationApi from '../api/OrganizationApi.js'
import ProjectApi from '../api/ProjectApi.js'
import { getTeamsByOrg } from '../api/TeamApi.js'
import { useOrganizationContext } from '../composables/useOrganizationContext.js'
import { useOrgPermissions } from '../composables/useOrgPermissions.js'

const router = useRouter()

// Aktif organizasyon paylaşılan context'te; switcher ve ayarlar ekranı aynı
// seçimi yazar, seçim localStorage'da kalıcıdır.
const {
  activeOrg: currentOrg,
  loading: orgsLoading,
  loadOrganizations,
  selectOrg,
  upsertOrganization,
} = useOrganizationContext()

// Yetkiler org nesnesindeki myRole'den türer — ekran başına üye listesi çekilmez.
const { can, roleLabel, roleBadgeClass } = useOrgPermissions()

const ICONS = {
  projects: '<svg fill="currentColor" viewBox="0 0 20 20"><path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"/></svg>',
  teams: '<svg fill="currentColor" viewBox="0 0 20 20"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"/></svg>',
  members: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/></svg>',
  billing: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"/></svg>',
  integrations: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/></svg>',
  settings: '<svg fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/></svg>',
}

const activeSection = ref('projects')
const integrationSubTab = ref('scm')
const integrationSubTabs = [
  { id: 'scm', label: 'Git / SCM' },
  { id: 'ci', label: 'CI/CD' },
]

const projects = ref([])
const projectsLoading = ref(false)
const teamCount = ref(0)
const memberCount = ref(0)

const showCreateProjectModal = ref(false)
const newProject = ref({ name: '', key: '', projectType: 'SCRUM' })
const creatingProject = ref(false)

const showCreateOrgModal = ref(false)
const newOrg = ref({ name: '', slug: '', description: '' })
const creatingOrg = ref(false)

/**
 * Menü, yetkiye göre kurulur: erişilemeyen bölüm hiç render edilmez.
 * Kullanıcı görebildiği bir bağlantıya tıklayıp "yetkiniz yok" uyarısı almamalı —
 * yetki kontrolü backend'de de aynı şekilde uygulanır.
 */
const visibleGroups = computed(() => {
  const groups = [
    {
      title: 'Çalışma Alanı',
      items: [
        can.value.viewProjects && { id: 'projects', label: 'Projeler', icon: ICONS.projects, count: projects.value.length },
        can.value.viewTeams && { id: 'teams', label: 'Takımlar', icon: ICONS.teams, count: teamCount.value },
      ],
    },
    {
      title: 'İnsanlar',
      items: [
        can.value.viewMembers && { id: 'members', label: 'Üyeler', icon: ICONS.members, count: memberCount.value },
      ],
    },
    {
      title: 'Yönetim',
      items: [
        can.value.viewBilling && { id: 'billing', label: 'Abonelik', icon: ICONS.billing, count: null },
        can.value.viewIntegrations && { id: 'integrations', label: 'Entegrasyonlar', icon: ICONS.integrations, count: null },
        can.value.viewSettings && { id: 'settings', label: 'Ayarlar', icon: ICONS.settings, count: null },
      ],
    },
  ]
  return groups
    .map(g => ({ ...g, items: g.items.filter(Boolean) }))
    .filter(g => g.items.length > 0)
})

const allowedSections = computed(() => visibleGroups.value.flatMap(g => g.items.map(i => i.id)))

/**
 * Yetkisi olmayan bir bölüm seçili kalmasın: organizasyon değişince ya da rol
 * düşünce ilk erişilebilir bölüme dönülür.
 */
watch(allowedSections, (ids) => {
  if (ids.length && !ids.includes(activeSection.value)) activeSection.value = ids[0]
}, { immediate: true })

// Id'yi izliyoruz, nesneyi değil: ayarlardan yapılan bir ad/logo güncellemesi
// listedeki nesneyi tazeliyor ama aynı organizasyonda kalıyoruz.
watch(() => currentOrg.value?.id, async (orgId) => {
  if (!orgId) return
  memberCount.value = currentOrg.value?.memberCount ?? 0
  await Promise.all([loadProjects(orgId), loadCounts(orgId)])
}, { immediate: true })

// UpgradeModal "Paketleri İncele" → Abonelik bölümünü aç
function openBillingSection() {
  if (can.value.viewBilling) activeSection.value = 'billing'
}
onMounted(() => {
  loadOrganizations()
  window.addEventListener('scrumtools:open-billing-tab', openBillingSection)
})
onBeforeUnmount(() => window.removeEventListener('scrumtools:open-billing-tab', openBillingSection))

async function loadProjects(orgId) {
  projectsLoading.value = true
  try {
    const res = await ProjectApi.getByOrg(orgId)
    projects.value = res.data
  } catch (e) {
    console.error('Projeler yüklenemedi:', e)
  } finally {
    projectsLoading.value = false
  }
}

/** Sidebar rozetleri — hata durumunda rozet gizlenir, ekran açılmaya devam eder. */
async function loadCounts(orgId) {
  const [teamList, membersRes] = await Promise.all([
    getTeamsByOrg(orgId).catch(() => []),
    OrganizationApi.getMembers(orgId).catch(() => ({ data: [] })),
  ])
  teamCount.value = teamList.length
  memberCount.value = membersRes.data.length
}

async function createOrg() {
  creatingOrg.value = true
  try {
    const res = await OrganizationApi.create(newOrg.value)
    upsertOrganization(res.data)
    selectOrg(res.data)
    showCreateOrgModal.value = false
    newOrg.value = { name: '', slug: '', description: '' }
  } catch (e) {
    createToast(e?.response?.data?.message || 'Organizasyon oluşturulamadı.', { type: 'danger', position: 'top-center' })
  } finally {
    creatingOrg.value = false
  }
}

async function createProject() {
  if (!currentOrg.value) return
  creatingProject.value = true
  try {
    const res = await ProjectApi.create(currentOrg.value.id, newProject.value)
    projects.value.push(res.data)
    showCreateProjectModal.value = false
    newProject.value = { name: '', key: '', projectType: 'SCRUM' }
  } catch (e) {
    createToast(e?.response?.data?.message || 'Proje oluşturulamadı.', { type: 'danger', position: 'top-center' })
  } finally {
    creatingProject.value = false
  }
}

function goToProject(project) {
  router.push(`/projects/${project.id}`)
}
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium text-sm; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium text-sm; }
</style>
