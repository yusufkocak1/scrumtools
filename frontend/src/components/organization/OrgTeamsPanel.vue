<template>
  <div class="space-y-5">
    <!-- Başlık -->
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">Takımlar</h2>
        <p class="text-sm text-gray-500 mt-0.5">
          Takım kurun, üyelerini ve çalıştığı projeleri yönetin
        </p>
      </div>
      <button v-if="can.manageTeams" @click="showCreateModal = true" class="btn-primary flex items-center gap-1.5">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Takım Oluştur
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
    </div>

    <div v-else-if="teams.length === 0" class="text-center py-16 border border-dashed border-gray-200 rounded-xl">
      <div class="w-14 h-14 bg-indigo-50 rounded-full flex items-center justify-center mx-auto mb-3">
        <svg class="w-7 h-7 text-indigo-400" fill="currentColor" viewBox="0 0 20 20">
          <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z" />
        </svg>
      </div>
      <h3 class="text-base font-semibold text-gray-700 mb-1">Henüz takım yok</h3>
      <p class="text-sm text-gray-500">
        <span v-if="can.manageTeams">Sağ üstten yeni bir takım oluşturun.</span>
        <span v-else>Bir yönetici tarafından takım oluşturulmasını bekleyin.</span>
      </p>
    </div>

    <!-- Takım listesi -->
    <div v-else class="space-y-3">
      <div
        v-for="team in teams"
        :key="team.id"
        class="border border-gray-200 rounded-xl overflow-hidden"
      >
        <!-- Satır başlığı -->
        <button
          @click="expanded = expanded === team.id ? null : team.id"
          class="w-full flex items-center gap-3 px-4 py-3.5 hover:bg-gray-50 transition-colors text-left"
        >
          <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-indigo-500 to-blue-500 flex items-center justify-center flex-shrink-0">
            <span class="text-white font-bold text-sm">{{ team.teamName?.charAt(0)?.toUpperCase() }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <p class="font-medium text-gray-900 truncate">{{ team.teamName }}</p>
              <span class="text-xs font-mono text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">{{ team.teamCode }}</span>
            </div>
            <p class="text-xs text-gray-500 mt-0.5">
              {{ team.memberEmails?.length || 0 }} üye
              <span v-if="team.projects?.length"> · {{ team.projects.length }} proje</span>
            </p>
          </div>
          <svg
            class="w-4 h-4 text-gray-400 transition-transform flex-shrink-0"
            :class="expanded === team.id ? 'rotate-90' : ''"
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>

        <!-- Detay -->
        <div v-if="expanded === team.id" class="border-t border-gray-100 p-4 bg-gray-50/50 space-y-5">
          <!-- Üyeler -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <h4 class="text-sm font-semibold text-gray-800">Üyeler</h4>
              <button
                v-if="canManage(team)"
                @click="openAddMember(team)"
                class="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
              >+ Üye Ekle</button>
            </div>
            <div class="bg-white rounded-lg border border-gray-200 divide-y divide-gray-100">
              <div
                v-for="(m, email) in team.members"
                :key="email"
                class="flex items-center gap-3 px-3 py-2"
              >
                <div class="w-7 h-7 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 text-xs font-bold flex-shrink-0">
                  {{ m.displayName?.charAt(0)?.toUpperCase() || '?' }}
                </div>
                <div class="min-w-0 flex-1">
                  <p class="text-sm text-gray-800 truncate">{{ m.displayName }}</p>
                  <p class="text-xs text-gray-400 truncate">{{ email }}</p>
                </div>
                <span
                  v-if="m.role === 'admin'"
                  class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full flex-shrink-0"
                >Takım Admini</span>
                <button
                  v-if="canManage(team) && email !== team.adminEmail"
                  @click="removeTeamMember(team, email)"
                  class="text-xs text-red-500 hover:text-red-700 flex-shrink-0"
                >Çıkar</button>
              </div>
              <p v-if="!team.memberEmails?.length" class="px-3 py-4 text-sm text-gray-400 text-center">
                Takımda üye yok.
              </p>
            </div>
            <p v-if="team.projects?.length" class="text-xs text-gray-500 mt-1.5">
              Takıma eklenen üyeler bağlı {{ team.projects.length }} projeye otomatik eklenir.
            </p>
          </div>

          <!-- Projeler -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <h4 class="text-sm font-semibold text-gray-800">Çalıştığı Projeler</h4>
              <select
                v-if="canManage(team)"
                :value="''"
                @change="addProject(team, $event.target.value); $event.target.value = ''"
                class="text-xs border border-gray-200 rounded-lg px-2 py-1 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="" disabled>+ Proje bağla</option>
                <option v-for="p in unlinkedProjects(team)" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select>
            </div>
            <div class="flex flex-wrap gap-2">
              <span
                v-for="p in team.projects || []"
                :key="p.id"
                class="inline-flex items-center gap-1.5 text-xs bg-white border border-gray-200 rounded-full pl-2.5 pr-1.5 py-1"
              >
                <span class="w-2 h-2 rounded-full" :style="{ backgroundColor: p.color || '#6366f1' }"></span>
                {{ p.name }}
                <span v-if="p.primary" class="text-[10px] text-indigo-600 font-medium">birincil</span>
                <button
                  v-if="canManage(team)"
                  @click="removeProject(team, p)"
                  class="text-gray-300 hover:text-red-500 ml-0.5"
                  title="Proje bağını kaldır"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </span>
              <span v-if="!team.projects?.length" class="text-xs text-gray-400 italic py-1">
                Takım henüz bir projeye bağlı değil.
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Yeni Takım -->
    <div v-if="showCreateModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl">
        <h4 class="text-lg font-semibold text-gray-900 mb-4">Yeni Takım</h4>
        <form @submit.prevent="createTeam" class="space-y-4">
          <div>
            <label class="label">Takım Adı</label>
            <input v-model="newTeam.teamName" @input="generateTeamCode" type="text" required class="input-field" placeholder="Takım adı" />
          </div>
          <div>
            <label class="label">Takım Kodu</label>
            <input
              v-model="newTeam.teamCode"
              @input="newTeam.teamCode = $event.target.value.toUpperCase()"
              type="text" required maxlength="4" minlength="2"
              class="input-field font-mono uppercase" placeholder="TKM"
            />
            <p class="text-xs text-gray-400 mt-1">2-4 karakter kısa kod</p>
          </div>
          <div class="flex gap-2 justify-end pt-1">
            <button type="button" @click="showCreateModal = false" class="btn-secondary">İptal</button>
            <button type="submit" :disabled="creating || newTeam.teamCode.length < 2" class="btn-primary">
              {{ creating ? 'Oluşturuluyor...' : 'Oluştur' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Takıma üye ekle -->
    <div v-if="addMemberTeam" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl">
        <h4 class="text-lg font-semibold text-gray-900">Üye Ekle</h4>
        <p class="text-sm text-gray-500 mb-4">{{ addMemberTeam.teamName }}</p>

        <div class="max-h-64 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100">
          <div
            v-for="member in availableMembers"
            :key="member.userEmail"
            @click="toggleSelection(member.userEmail)"
            class="flex items-center gap-3 px-4 py-2.5 hover:bg-gray-50 cursor-pointer transition-colors"
            :class="{ 'bg-indigo-50': selectedEmails.includes(member.userEmail) }"
          >
            <div class="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center flex-shrink-0">
              <span class="text-indigo-600 font-bold text-xs">{{ member.userName?.charAt(0)?.toUpperCase() || '?' }}</span>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-900 truncate">{{ member.userName }}</p>
              <p class="text-xs text-gray-400 truncate">{{ member.userEmail }}</p>
            </div>
            <svg v-if="selectedEmails.includes(member.userEmail)" class="w-5 h-5 text-indigo-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
            </svg>
          </div>
          <p v-if="availableMembers.length === 0" class="px-4 py-6 text-center text-sm text-gray-500">
            Tüm organizasyon üyeleri zaten bu takımda.
          </p>
        </div>

        <p v-if="addMemberTeam.projects?.length" class="text-xs text-gray-500 mt-2">
          Seçilenler takımın bağlı olduğu {{ addMemberTeam.projects.length }} projeye de eklenecek.
        </p>

        <div class="flex gap-2 justify-end pt-4">
          <button @click="closeAddMember" class="btn-secondary">İptal</button>
          <button @click="addSelectedMembers" :disabled="adding || selectedEmails.length === 0" class="btn-primary">
            {{ adding ? 'Ekleniyor...' : `Ekle (${selectedEmails.length})` }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import OrganizationApi from '../../api/OrganizationApi.js'
import ProjectApi from '../../api/ProjectApi.js'
import {
  getTeamsByOrg, createTeam as apiCreateTeam, addMemberToTeam,
  removeMember as apiRemoveMember, addTeamProject, removeTeamProject,
} from '../../api/TeamApi.js'
import { useAuth } from '../../composables/useAuth.js'
import { useOrgPermissions } from '../../composables/useOrgPermissions.js'
import { useTeamContext } from '../../composables/useTeamContext.js'

const props = defineProps({
  orgId: { type: String, required: true }
})

const auth = useAuth()
const { can } = useOrgPermissions()

const teams = ref([])
const projects = ref([])
const orgMembers = ref([])
const loading = ref(false)
const expanded = ref(null)

const showCreateModal = ref(false)
const newTeam = ref({ teamName: '', teamCode: '' })
const creating = ref(false)

const addMemberTeam = ref(null)
const selectedEmails = ref([])
const adding = ref(false)

/** Org yöneticisi her takımı, takım admini yalnızca kendi takımını yönetir. */
function canManage(team) {
  return can.value.manageTeams || team.adminEmail === auth.userEmail.value
}

const availableMembers = computed(() => {
  if (!addMemberTeam.value) return []
  const inTeam = new Set(addMemberTeam.value.memberEmails || [])
  return orgMembers.value.filter(m => !inTeam.has(m.userEmail))
})

function unlinkedProjects(team) {
  const linked = new Set((team.projects || []).map(p => p.id))
  return projects.value.filter(p => !linked.has(p.id))
}

async function loadAll() {
  if (!props.orgId) return
  loading.value = true
  try {
    const [teamList, projectsRes, membersRes] = await Promise.all([
      getTeamsByOrg(props.orgId),
      ProjectApi.getByOrg(props.orgId).catch(() => ({ data: [] })),
      OrganizationApi.getMembers(props.orgId).catch(() => ({ data: [] })),
    ])
    teams.value = teamList
    projects.value = projectsRes.data
    orgMembers.value = membersRes.data
  } catch (e) {
    console.error('Takımlar yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function replaceTeam(updated) {
  const idx = teams.value.findIndex(t => t.id === updated.id)
  if (idx !== -1) teams.value[idx] = updated
}

function generateTeamCode() {
  const name = newTeam.value.teamName.trim()
  if (!name) { newTeam.value.teamCode = ''; return }
  const words = name.split(/\s+/).filter(Boolean)
  newTeam.value.teamCode = words.length > 1
    ? words.map(w => w.charAt(0).toUpperCase()).join('').substring(0, 4)
    : name.substring(0, Math.min(4, Math.max(2, name.length))).toUpperCase()
}

async function createTeam() {
  if (newTeam.value.teamCode.length < 2) return
  creating.value = true
  try {
    const team = await apiCreateTeam(
      props.orgId,
      newTeam.value.teamName.trim(),
      newTeam.value.teamCode.trim().toUpperCase()
    )
    teams.value.push(team)
    showCreateModal.value = false
    newTeam.value = { teamName: '', teamCode: '' }
    // Merkezi takım context'i tazele: yeni takım modüllerde hemen görünsün
    useTeamContext().loadTeams({ force: true })
  } catch (e) {
    createToast(e?.response?.data?.message || 'Takım oluşturulamadı.', { type: 'danger', position: 'top-center' })
  } finally {
    creating.value = false
  }
}

function openAddMember(team) {
  addMemberTeam.value = team
  selectedEmails.value = []
}

function closeAddMember() {
  addMemberTeam.value = null
  selectedEmails.value = []
}

function toggleSelection(email) {
  const idx = selectedEmails.value.indexOf(email)
  if (idx === -1) selectedEmails.value.push(email)
  else selectedEmails.value.splice(idx, 1)
}

async function addSelectedMembers() {
  if (!addMemberTeam.value || selectedEmails.value.length === 0) return
  adding.value = true
  try {
    for (const email of selectedEmails.value) {
      replaceTeam(await addMemberToTeam(props.orgId, addMemberTeam.value.id, email))
    }
    closeAddMember()
  } catch (e) {
    createToast(e?.response?.data?.message || 'Üye eklenemedi.', { type: 'danger', position: 'top-center' })
  } finally {
    adding.value = false
  }
}

async function removeTeamMember(team, email) {
  const projectNote = team.projects?.length
    ? `\n\nKullanıcı takıma bağlı ${team.projects.length} projeden de çıkarılacak (başka bir bağlı takımda değilse).`
    : ''
  if (!confirm(`${email} takımdan çıkarılsın mı?${projectNote}`)) return
  try {
    replaceTeam(await apiRemoveMember(team.id, email))
  } catch (e) {
    createToast(e?.response?.data?.message || 'Üye çıkarılamadı.', { type: 'danger', position: 'top-center' })
  }
}

async function addProject(team, projectId) {
  if (!projectId) return
  try {
    replaceTeam(await addTeamProject(team.id, projectId))
    createToast('Proje bağlandı. Takım üyeleri projeye eklendi.', { type: 'success', position: 'top-center' })
  } catch (e) {
    createToast(e?.response?.data?.message || 'Proje bağlanamadı.', { type: 'danger', position: 'top-center' })
  }
}

async function removeProject(team, project) {
  if (!confirm(
    `${project.name} projesinin bağlantısı kaldırılsın mı?\n\n` +
    'Bu takım üzerinden projeye eklenmiş üyeler projeden çıkarılır.'
  )) return
  try {
    replaceTeam(await removeTeamProject(team.id, project.id))
  } catch (e) {
    createToast(e?.response?.data?.message || 'Proje bağlantısı kaldırılamadı.', { type: 'danger', position: 'top-center' })
  }
}

watch(() => props.orgId, loadAll)
onMounted(loadAll)
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200 transition-colors font-medium; }
</style>
