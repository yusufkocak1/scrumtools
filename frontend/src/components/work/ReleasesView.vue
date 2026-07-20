<template>
  <div class="h-full overflow-y-auto">
    <!-- Takım projeye bağlı değil -->
    <div v-if="!loading && !activeProjectId" class="max-w-lg mx-auto mt-16">
      <div class="bg-white rounded-xl border border-gray-200 p-8 text-center">
        <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-2xl flex items-center justify-center">
          <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-2">Sürüm yönetimi kullanılamıyor</h3>
        <p class="text-sm text-gray-500">
          Bu takım bir projeye bağlı değil. Sürümler proje seviyesinde yönetilir —
          sürüm yönetimini kullanmak için takımı bir projeye bağlayın.
        </p>

        <!-- Projeye bağlama -->
        <div v-if="orgProjects.length" class="mt-6 flex items-center justify-center gap-2">
          <select
            v-model="selectedProjectId"
            class="text-sm border border-gray-300 rounded-lg px-3 py-2 bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option :value="null" disabled>Proje seçin…</option>
            <option v-for="p in orgProjects" :key="p.id" :value="p.id">
              {{ p.name }} ({{ p.key }})
            </option>
          </select>
          <button
            @click="linkProject"
            :disabled="!selectedProjectId || linking"
            class="px-3 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ linking ? 'Bağlanıyor…' : 'Projeye Bağla' }}
          </button>
        </div>
        <p v-else-if="orgProjectsLoaded" class="mt-4 text-xs text-gray-400">
          Organizasyonda erişebildiğiniz bir proje bulunamadı. Önce bir proje oluşturun.
        </p>
        <p v-if="linkError" class="mt-3 text-xs text-red-500">{{ linkError }}</p>
      </div>
    </div>

    <div v-else class="max-w-5xl mx-auto">
      <!-- Başlık + Yeni Sürüm -->
      <div class="flex items-center justify-between mb-4">
        <div>
          <h2 class="text-base font-semibold text-gray-900">Sürümler</h2>
          <p v-if="projectName" class="text-xs text-gray-400">Proje: {{ projectName }}</p>
        </div>
        <button
          @click="openCreateForm"
          class="px-3 py-1.5 text-xs font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700"
        >
          + Sürüm
        </button>
      </div>

      <!-- Yükleniyor -->
      <div v-if="loading" class="space-y-3">
        <div v-for="i in 3" :key="i" class="bg-white rounded-xl border border-gray-200 p-5 animate-pulse">
          <div class="h-4 bg-gray-200 rounded w-40 mb-3"></div>
          <div class="h-3 bg-gray-100 rounded w-64"></div>
        </div>
      </div>

      <!-- Boş liste -->
      <div v-else-if="releases.length === 0" class="bg-white rounded-xl border border-gray-200 p-10 text-center">
        <p class="text-sm font-medium text-gray-500">Henüz sürüm yok</p>
        <p class="text-xs text-gray-400 mt-1">İlk sürümü oluşturup işleri pakete bağlayın.</p>
      </div>

      <!-- Release listesi -->
      <div v-else class="space-y-3">
        <div
          v-for="release in releases"
          :key="release.id"
          class="bg-white rounded-xl border border-gray-200 overflow-hidden"
        >
          <!-- Satır başlığı -->
          <div
            class="p-4 sm:p-5 cursor-pointer hover:bg-gray-50/50 transition-colors"
            @click="toggleExpand(release)"
          >
            <div class="flex flex-wrap items-center gap-3">
              <svg class="w-4 h-4 text-gray-400 transition-transform shrink-0"
                   :class="expandedId === release.id ? 'rotate-90' : ''"
                   fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
              <span class="text-sm font-semibold text-gray-900">{{ release.name }}</span>
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold"
                    :class="statusMeta(release.status).chip">
                {{ statusMeta(release.status).label }}
              </span>
              <span v-if="release.releaseManagerName || release.releaseManagerEmail"
                    class="text-xs text-gray-500">
                👤 {{ release.releaseManagerName || release.releaseManagerEmail }}
              </span>
              <div class="ml-auto flex items-center gap-4 text-xs text-gray-400">
                <span v-if="release.freezeDate" title="Paket kapanış tarihi">❄ {{ formatDate(release.freezeDate) }}</span>
                <span v-if="release.plannedReleaseDate && !release.actualReleaseDate" title="Planlanan yayın tarihi">📅 {{ formatDate(release.plannedReleaseDate) }}</span>
                <span v-if="release.actualReleaseDate" class="text-green-600 font-medium" title="Yayınlanma anı">🚀 {{ formatDateTime(release.actualReleaseDate) }}</span>
              </div>
            </div>

            <!-- İlerleme -->
            <div class="mt-3 flex items-center gap-3">
              <div class="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                <div class="h-full bg-green-500 rounded-full transition-all"
                     :style="{ width: progressPercent(release) + '%' }"></div>
              </div>
              <span class="text-[11px] text-gray-500 shrink-0">
                {{ release.doneTaskCount }}/{{ release.taskCount }} iş tamamlandı
              </span>
            </div>
          </div>

          <!-- Detay paneli -->
          <div v-if="expandedId === release.id" class="border-t border-gray-100 p-4 sm:p-5 bg-gray-50/40">
            <p v-if="release.description" class="text-sm text-gray-600 mb-4 whitespace-pre-line">{{ release.description }}</p>

            <!-- Aksiyonlar -->
            <div class="flex flex-wrap items-center gap-2 mb-5">
              <button
                v-for="t in availableTransitions(release)"
                :key="t.target"
                @click="transition(release, t)"
                class="px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors"
                :class="t.style"
              >
                {{ t.label }}
              </button>
              <div class="ml-auto flex items-center gap-2">
                <button
                  v-if="release.status !== 'RELEASED' && release.status !== 'CANCELLED'"
                  @click="openEditForm(release)"
                  class="px-3 py-1.5 text-xs text-gray-600 border border-gray-300 rounded-lg hover:bg-gray-100"
                >
                  Düzenle
                </button>
                <button
                  v-if="release.status !== 'RELEASED'"
                  @click="removeRelease(release)"
                  class="px-3 py-1.5 text-xs text-red-600 border border-red-200 rounded-lg hover:bg-red-50"
                >
                  Sil
                </button>
              </div>
            </div>

            <!-- Görev ekleme (yayınlanmamış sürümlerde) -->
            <div v-if="release.status !== 'RELEASED' && release.status !== 'CANCELLED'" class="mb-4 max-w-md">
              <label class="block text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-1.5">Sürüme İş Ekle</label>
              <TaskPickerInput
                :key="taskPickerKey"
                :team-id="teamId"
                v-model="pickTaskId"
                placeholder="Görev ara ve sürüme ekle..."
                @select="(t) => addTaskToRelease(release, t)"
              />
            </div>

            <!-- Sürüme bağlı görevler -->
            <div class="mb-2">
              <h4 class="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Bağlı İşler ({{ (releaseTasks[release.id] || []).length }})
              </h4>
              <div v-if="tasksLoading" class="text-xs text-gray-400 py-2">Yükleniyor...</div>
              <div v-else-if="!(releaseTasks[release.id] || []).length" class="text-xs text-gray-400 py-2">
                Bu sürüme bağlı iş yok.
              </div>
              <div v-else class="divide-y divide-gray-100 bg-white rounded-lg border border-gray-200">
                <div
                  v-for="t in releaseTasks[release.id]"
                  :key="t.id"
                  class="flex items-center gap-3 px-3 py-2"
                >
                  <span class="font-mono text-xs text-gray-500 shrink-0 cursor-pointer hover:text-blue-600"
                        @click="openTask(t)">{{ t.customId }}</span>
                  <span class="text-sm text-gray-800 truncate flex-1 cursor-pointer hover:text-blue-600"
                        @click="openTask(t)">{{ t.title }}</span>
                  <StatusBadge :status="t.status" />
                  <button
                    v-if="release.status !== 'RELEASED' && release.status !== 'CANCELLED'"
                    @click="removeTaskFromRelease(release, t)"
                    class="text-gray-300 hover:text-red-500 shrink-0"
                    title="Sürümden çıkar"
                  >✕</button>
                </div>
              </div>
            </div>

            <!-- Dağıtım tarihçesi (yayınlanmış sürümlerde) -->
            <div v-if="release.status === 'RELEASED'" class="mt-5">
              <h4 class="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Dağıtım Tarihçesi
              </h4>
              <div class="overflow-x-auto bg-white rounded-lg border border-gray-200">
                <table class="w-full text-sm">
                  <thead>
                    <tr class="text-left text-[11px] text-gray-400 uppercase tracking-wider border-b border-gray-100">
                      <th class="px-3 py-2 font-semibold">İş</th>
                      <th class="px-3 py-2 font-semibold">Başlık</th>
                      <th class="px-3 py-2 font-semibold">Yayın Anındaki Durum</th>
                      <th class="px-3 py-2 font-semibold">Dağıtım Tarihi</th>
                      <th class="px-3 py-2 font-semibold">Yayınlayan</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-50">
                    <tr v-for="d in deployments[release.id] || []" :key="d.id">
                      <td class="px-3 py-2 font-mono text-xs text-gray-500">{{ d.taskCustomId }}</td>
                      <td class="px-3 py-2 text-gray-800">{{ d.taskTitle }}</td>
                      <td class="px-3 py-2"><StatusBadge :status="d.taskStatusAtRelease" /></td>
                      <td class="px-3 py-2 text-gray-600">{{ formatDateTime(d.releasedAt) }}</td>
                      <td class="px-3 py-2 text-gray-600">{{ d.releasedBy }}</td>
                    </tr>
                    <tr v-if="!(deployments[release.id] || []).length">
                      <td colspan="5" class="px-3 py-3 text-xs text-gray-400 text-center">Dağıtım kaydı yok.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Oluştur / Düzenle Modal -->
    <ReleaseFormModal
      v-if="showForm && activeProjectId"
      :project-id="activeProjectId"
      :release="editingRelease"
      :team-members="teamMembers"
      @close="showForm = false"
      @saved="onReleaseSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import StatusBadge from '@/components/workflow/StatusBadge.vue'
import TaskPickerInput from './TaskPickerInput.vue'
import ReleaseFormModal from './ReleaseFormModal.vue'
import { getTeamById, linkTeamToProject } from '../../api/TeamApi.js'
import { ProjectApi } from '../../api/ProjectApi.js'
import { updateTask } from '../../api/WorkApi.js'
import {
  getProjectReleases,
  deleteRelease,
  updateReleaseStatus,
  getReleaseTasks,
  getReleaseDeployments
} from '../../api/ReleaseApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  /**
   * Aktif proje context'i. Sürümler proje seviyesinde olduğu için bu görünüm
   * doğrudan seçili projeyi gösterir; "Tüm projeler" seçiliyken (null) takımın
   * birincil projesine düşülür — sürüm listesi projeler arası birleştirilemez.
   */
  projectId: { type: String, default: null }
})

const router = useRouter()

const loading = ref(true)
const teamPrimaryProjectId = ref(null)
const teamProjects = ref([])

/** Gösterilecek proje: aktif context, yoksa takımın birincil projesi. */
const activeProjectId = computed(() => props.projectId || teamPrimaryProjectId.value)

const projectName = computed(() => {
  const match = teamProjects.value.find(p => p.id === activeProjectId.value)
  return match ? `${match.name} (${match.key})` : null
})
const teamMembers = ref([])
const releases = ref([])
const expandedId = ref(null)
const releaseTasks = ref({})
const deployments = ref({})
const tasksLoading = ref(false)
const showForm = ref(false)
const editingRelease = ref(null)
const pickTaskId = ref(null)
const taskPickerKey = ref(0)

// Projeye bağlama (takım projeye bağlı değilken)
const orgProjects = ref([])
const orgProjectsLoaded = ref(false)
const selectedProjectId = ref(null)
const linking = ref(false)
const linkError = ref(null)

// ─── Durum meta bilgileri ─────────────────────────────────────────────────────

const STATUS_META = {
  OPEN:        { label: 'Açık',          chip: 'bg-blue-100 text-blue-700' },
  CODE_FREEZE: { label: 'Paket Kapandı', chip: 'bg-amber-100 text-amber-700' },
  REGRESSION:  { label: 'Regresyon',     chip: 'bg-purple-100 text-purple-700' },
  APPROVED:    { label: 'Onaylandı',     chip: 'bg-teal-100 text-teal-700' },
  RELEASED:    { label: 'Yayınlandı',    chip: 'bg-green-100 text-green-700' },
  CANCELLED:   { label: 'İptal',         chip: 'bg-gray-100 text-gray-500' }
}

const PRIMARY = 'text-white bg-blue-600 border-blue-600 hover:bg-blue-700'
const SUCCESS = 'text-white bg-green-600 border-green-600 hover:bg-green-700'
const NEUTRAL = 'text-gray-600 bg-white border-gray-300 hover:bg-gray-100'
const DANGER  = 'text-red-600 bg-white border-red-200 hover:bg-red-50'

/** Backend'deki geçiş haritasıyla birebir aynı — buton üretimi için */
const TRANSITIONS = {
  OPEN: [
    { target: 'CODE_FREEZE', label: '❄ Paketi Kapat', style: PRIMARY },
    { target: 'CANCELLED', label: 'İptal Et', style: DANGER }
  ],
  CODE_FREEZE: [
    { target: 'REGRESSION', label: 'Regresyona Al', style: PRIMARY },
    { target: 'OPEN', label: 'Paketi Yeniden Aç', style: NEUTRAL },
    { target: 'CANCELLED', label: 'İptal Et', style: DANGER }
  ],
  REGRESSION: [
    { target: 'APPROVED', label: '✓ Onayla', style: PRIMARY },
    { target: 'CODE_FREEZE', label: 'Geri Al (Paket Kapandı)', style: NEUTRAL },
    { target: 'CANCELLED', label: 'İptal Et', style: DANGER }
  ],
  APPROVED: [
    { target: 'RELEASED', label: '🚀 Yayınla', style: SUCCESS },
    { target: 'REGRESSION', label: 'Geri Al (Regresyon)', style: NEUTRAL },
    { target: 'CANCELLED', label: 'İptal Et', style: DANGER }
  ],
  RELEASED: [],
  CANCELLED: [
    { target: 'OPEN', label: 'Yeniden Aç', style: NEUTRAL }
  ]
}

function statusMeta(status) {
  return STATUS_META[status] || { label: status, chip: 'bg-gray-100 text-gray-600' }
}

function availableTransitions(release) {
  return TRANSITIONS[release.status] || []
}

function progressPercent(release) {
  if (!release.taskCount) return 0
  return Math.round((release.doneTaskCount / release.taskCount) * 100)
}

// ─── Yükleme ──────────────────────────────────────────────────────────────────

async function load() {
  loading.value = true
  try {
    const team = await getTeamById(props.teamId)
    teamPrimaryProjectId.value = team?.projectId || null
    teamProjects.value = team?.projects || []
    teamMembers.value = Object.entries(team?.members || {}).map(([email, m]) => ({
      email,
      ...m,
      displayName: m.displayName || email
    }))
    if (activeProjectId.value) {
      releases.value = await getProjectReleases(activeProjectId.value)
    } else {
      releases.value = []
      await loadOrgProjects(team?.organizationId)
    }
  } catch (e) {
    console.error('Sürümler yüklenemedi:', e)
    releases.value = []
  } finally {
    loading.value = false
  }
}

// ─── Takımı projeye bağlama ───────────────────────────────────────────────────

async function loadOrgProjects(orgId) {
  orgProjectsLoaded.value = false
  orgProjects.value = []
  if (!orgId) { orgProjectsLoaded.value = true; return }
  try {
    const { data } = await ProjectApi.getByOrg(orgId)
    orgProjects.value = data || []
  } catch (e) {
    console.error('Projeler yüklenemedi:', e)
  } finally {
    orgProjectsLoaded.value = true
  }
}

async function linkProject() {
  if (!selectedProjectId.value || linking.value) return
  linking.value = true
  linkError.value = null
  try {
    await linkTeamToProject(props.teamId, selectedProjectId.value)
    await load()
  } catch (e) {
    linkError.value = e?.response?.data?.error || 'Takım projeye bağlanamadı.'
  } finally {
    linking.value = false
  }
}

async function toggleExpand(release) {
  if (expandedId.value === release.id) {
    expandedId.value = null
    return
  }
  expandedId.value = release.id
  await loadReleaseDetail(release)
}

async function loadReleaseDetail(release) {
  tasksLoading.value = true
  try {
    releaseTasks.value[release.id] = await getReleaseTasks(activeProjectId.value, release.id)
    if (release.status === 'RELEASED') {
      deployments.value[release.id] = await getReleaseDeployments(activeProjectId.value, release.id)
    }
  } catch (e) {
    console.error('Sürüm detayı yüklenemedi:', e)
  } finally {
    tasksLoading.value = false
  }
}

async function refreshRelease(release) {
  // Listeyi tazele (task sayıları güncellensin) + açık detayı yenile
  try {
    releases.value = await getProjectReleases(activeProjectId.value)
    const fresh = releases.value.find(r => r.id === release.id)
    if (fresh && expandedId.value === fresh.id) await loadReleaseDetail(fresh)
  } catch (e) {
    console.error(e)
  }
}

// ─── Görev ekle / çıkar ───────────────────────────────────────────────────────

async function addTaskToRelease(release, task) {
  try {
    await updateTask(props.teamId, task.id, { releaseId: release.id })
    await refreshRelease(release)
  } catch (e) {
    console.error('Görev sürüme eklenemedi:', e)
  } finally {
    // Picker'ı sıfırla (yeni arama için)
    pickTaskId.value = null
    taskPickerKey.value++
  }
}

async function removeTaskFromRelease(release, task) {
  try {
    await updateTask(props.teamId, task.id, { releaseId: '' })
    await refreshRelease(release)
  } catch (e) {
    console.error('Görev sürümden çıkarılamadı:', e)
  }
}

// ─── Durum geçişi / CRUD ──────────────────────────────────────────────────────

async function transition(release, t) {
  if (t.target === 'RELEASED') {
    // Done olmayan işleri kullanıcıya göster ve onay al
    let tasks = releaseTasks.value[release.id]
    if (!tasks) {
      try { tasks = await getReleaseTasks(activeProjectId.value, release.id) } catch { tasks = [] }
    }
    const notDone = (tasks || []).filter(x => x.status !== 'Done' && x.status !== 'Cancelled')
    let msg = `"${release.name}" sürümü yayınlanacak ve dağıtım tarihçesi kaydedilecek. Bu işlem geri alınamaz.`
    if (notDone.length > 0) {
      const list = notDone.slice(0, 10).map(x => `• ${x.customId} — ${x.title} (${x.status})`).join('\n')
      msg = `Dikkat: ${notDone.length} iş henüz tamamlanmadı:\n\n${list}${notDone.length > 10 ? '\n…' : ''}\n\n${msg}`
    }
    if (!confirm(msg)) return
  } else if (t.target === 'CANCELLED') {
    if (!confirm(`"${release.name}" sürümü iptal edilecek. Emin misiniz?`)) return
  }

  try {
    const updated = await updateReleaseStatus(activeProjectId.value, release.id, t.target)
    const idx = releases.value.findIndex(r => r.id === release.id)
    if (idx >= 0) releases.value[idx] = updated
    if (expandedId.value === release.id) await loadReleaseDetail(updated)
  } catch (e) {
    console.error('Durum geçişi başarısız:', e)
  }
}

async function removeRelease(release) {
  if (!confirm(`"${release.name}" sürümü silinecek. Bağlı işlerin sürüm bağı kaldırılır. Emin misiniz?`)) return
  try {
    await deleteRelease(activeProjectId.value, release.id)
    releases.value = releases.value.filter(r => r.id !== release.id)
    if (expandedId.value === release.id) expandedId.value = null
  } catch (e) {
    console.error('Sürüm silinemedi:', e)
  }
}

function openCreateForm() {
  editingRelease.value = null
  showForm.value = true
}

function openEditForm(release) {
  editingRelease.value = release
  showForm.value = true
}

async function onReleaseSaved() {
  showForm.value = false
  editingRelease.value = null
  await load()
}

function openTask(task) {
  router.push({ name: 'TaskDetail', params: { taskId: task.customId } })
}

// ─── Format ───────────────────────────────────────────────────────────────────

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('tr-TR')
}

function formatDateTime(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('tr-TR', { dateStyle: 'short', timeStyle: 'short' })
}

onMounted(load)
// Proje değişince o projenin sürümleri yüklenir (sürümler proje seviyesinde).
watch(() => [props.teamId, props.projectId], load)
</script>
