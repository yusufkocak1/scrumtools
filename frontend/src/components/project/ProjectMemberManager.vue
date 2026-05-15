<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
        Proje Üyeleri
        <span class="ml-2 text-sm font-normal text-gray-500">({{ members.length }})</span>
      </h3>
      <div class="flex gap-2">
        <button @click="showAddTeamModal = true" class="btn-secondary text-sm flex items-center gap-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          Takımı Ekle
        </button>
        <button @click="showAddModal = true" class="btn-primary text-sm flex items-center gap-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Üye Ekle
        </button>
      </div>
    </div>

    <div class="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 dark:bg-gray-700/50">
          <tr>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Kullanıcı</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Tür</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Rol</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Katılım</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
          <tr v-if="loading">
            <td colspan="5" class="text-center py-8 text-gray-500">Yükleniyor...</td>
          </tr>
          <tr v-else-if="members.length === 0">
            <td colspan="5" class="text-center py-8 text-gray-400">Henüz üye yok.</td>
          </tr>
          <tr
            v-for="member in members"
            :key="member.id"
            class="hover:bg-gray-50 dark:hover:bg-gray-700/30"
          >
            <td class="px-4 py-3">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-xs font-bold">
                  {{ member.userName?.charAt(0)?.toUpperCase() }}
                </div>
                <div>
                  <p class="font-medium text-gray-900 dark:text-white">{{ member.userName }}</p>
                  <p class="text-xs text-gray-500">{{ member.userEmail }}</p>
                </div>
              </div>
            </td>
            <td class="px-4 py-3">
              <span
                :class="member.memberType === 'OBSERVER'
                  ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
                  : 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400'"
                class="text-xs font-medium px-2 py-0.5 rounded-full"
              >
                {{ member.memberType === 'OBSERVER' ? 'Gözlemci' : 'Üye' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex flex-wrap gap-1 items-center">
                <span
                  v-for="role in member.roles"
                  :key="role.id"
                  class="inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400"
                >
                  <span class="w-1.5 h-1.5 rounded-full" :style="{ backgroundColor: role.color || '#6366f1' }"></span>
                  {{ role.name }}
                </span>
                <span v-if="!member.roles?.length" class="text-xs text-gray-400 italic">Rol yok</span>
                <button
                  @click="openRoleModal(member)"
                  class="ml-1 text-indigo-500 hover:text-indigo-700 text-xs"
                  title="Rolleri düzenle"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                  </svg>
                </button>
              </div>
            </td>
            <td class="px-4 py-3 text-gray-500 text-xs">
              {{ formatDate(member.joinedAt) }}
            </td>
            <td class="px-4 py-3 text-right">
              <button @click="removeMember(member)" class="text-red-500 hover:text-red-700 text-xs">Çıkar</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Üye Ekle Modal -->
    <div v-if="showAddModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl p-6 w-full max-w-md shadow-2xl">
        <h4 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Üye Ekle</h4>
        <div class="space-y-3">
          <div>
            <label class="label">E-posta</label>
            <input v-model="addEmail" type="email" class="input-field" placeholder="kullanici@example.com" />
          </div>
          <div>
            <label class="label">Üyelik Türü</label>
            <select v-model="addMemberType" class="input-field">
              <option value="MEMBER">Üye</option>
              <option value="OBSERVER">Gözlemci</option>
            </select>
          </div>
          <div>
            <label class="label">Roller</label>
            <div class="border border-gray-200 dark:border-gray-600 rounded-lg p-2 max-h-40 overflow-y-auto space-y-1">
              <label
                v-for="role in availableRoles"
                :key="role.id"
                class="flex items-center gap-2 cursor-pointer text-xs px-1 py-0.5 rounded hover:bg-gray-50 dark:hover:bg-gray-700/30"
              >
                <input type="checkbox" :value="role.id" v-model="addRoleIds" class="rounded text-indigo-600" />
                <span class="w-2 h-2 rounded-full" :style="{ backgroundColor: role.color || '#6B7280' }"></span>
                <span class="text-gray-700 dark:text-gray-300">{{ role.name }}</span>
              </label>
              <p v-if="availableRoles.length === 0" class="text-xs text-gray-400 text-center py-2">Rol bulunamadı</p>
            </div>
          </div>
        </div>
        <div class="flex gap-2 mt-4 justify-end">
          <button @click="showAddModal = false" class="btn-secondary">İptal</button>
          <button @click="addMember" :disabled="!addEmail" class="btn-primary">Ekle</button>
        </div>
      </div>
    </div>

    <!-- Takımı Toplu Ekle Modal -->
    <div v-if="showAddTeamModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl p-6 w-full max-w-md shadow-2xl">
        <h4 class="text-lg font-semibold mb-1 text-gray-900 dark:text-white">Takımı Projeye Ekle</h4>
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
          Seçilen takımın tüm üyeleri projeye eklenecektir. Zaten projede olan üyeler atlanır.
        </p>

        <div class="space-y-3">
          <!-- Takım Arama Autocomplete -->
          <div>
            <label class="label">Takım Ara</label>
            <div class="relative" ref="teamSearchRef">
              <input
                v-model="teamSearchQuery"
                @input="onTeamSearch"
                @focus="showTeamDropdown = true"
                type="text"
                class="input-field pr-8"
                :placeholder="selectedTeam ? selectedTeam.teamName : 'Takım adı yazın...'"
                autocomplete="off"
              />
              <button
                v-if="selectedTeam"
                @click="clearTeamSelection"
                class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>

              <!-- Dropdown -->
              <div
                v-if="showTeamDropdown && (teamSearchResults.length > 0 || teamsLoading)"
                class="absolute z-10 mt-1 w-full bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg max-h-52 overflow-y-auto"
              >
                <div v-if="teamsLoading" class="px-3 py-2 text-sm text-gray-500">Aranıyor...</div>
                <template v-else>
                  <button
                    v-for="team in teamSearchResults"
                    :key="team.id"
                    @mousedown.prevent="selectTeam(team)"
                    class="w-full text-left px-3 py-2 hover:bg-indigo-50 dark:hover:bg-indigo-900/30 text-sm text-gray-900 dark:text-white flex items-center gap-2"
                  >
                    <span class="w-7 h-7 rounded-lg bg-indigo-100 dark:bg-indigo-900/50 flex items-center justify-center text-indigo-700 dark:text-indigo-300 font-bold text-xs flex-shrink-0">
                      {{ team.teamName?.charAt(0)?.toUpperCase() }}
                    </span>
                    <span>
                      <span class="block font-medium">{{ team.teamName }}</span>
                      <span class="block text-xs text-gray-400">{{ team.teamCode }} · {{ Object.keys(team.members || {}).length }} üye</span>
                    </span>
                  </button>
                  <div v-if="teamSearchResults.length === 0 && teamSearchQuery" class="px-3 py-2 text-sm text-gray-400">
                    Sonuç bulunamadı.
                  </div>
                </template>
              </div>
            </div>

            <!-- Seçili Takım Özeti -->
            <div v-if="selectedTeam" class="mt-2 flex items-center gap-2 bg-indigo-50 dark:bg-indigo-900/20 border border-indigo-200 dark:border-indigo-700 rounded-lg px-3 py-2">
              <div class="w-6 h-6 rounded bg-indigo-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                {{ selectedTeam.teamName?.charAt(0)?.toUpperCase() }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-indigo-700 dark:text-indigo-300 truncate">{{ selectedTeam.teamName }}</p>
                <p class="text-xs text-gray-500">{{ Object.keys(selectedTeam.members || {}).length }} üye eklenecek</p>
              </div>
            </div>
          </div>

          <div>
            <label class="label">Üyelik Türü</label>
            <select v-model="teamMemberType" class="input-field">
              <option value="MEMBER">Üye</option>
              <option value="OBSERVER">Gözlemci</option>
            </select>
          </div>
          <div>
            <label class="label">Roller (opsiyonel)</label>
            <div class="border border-gray-200 dark:border-gray-600 rounded-lg p-2 max-h-40 overflow-y-auto space-y-1">
              <label
                v-for="role in availableRoles"
                :key="role.id"
                class="flex items-center gap-2 cursor-pointer text-xs px-1 py-0.5 rounded hover:bg-gray-50 dark:hover:bg-gray-700/30"
              >
                <input type="checkbox" :value="role.id" v-model="teamRoleIds" class="rounded text-indigo-600" />
                <span class="w-2 h-2 rounded-full" :style="{ backgroundColor: role.color || '#6B7280' }"></span>
                <span class="text-gray-700 dark:text-gray-300">{{ role.name }}</span>
              </label>
            </div>
          </div>
        </div>

        <div v-if="addTeamResult" class="mt-3 text-sm text-green-600 dark:text-green-400">
          {{ addTeamResult }}
        </div>
        <div class="flex gap-2 mt-4 justify-end">
          <button @click="closeTeamModal" class="btn-secondary">İptal</button>
          <button @click="addTeam" :disabled="!selectedTeam || addingTeam" class="btn-primary">
            <span v-if="addingTeam">Ekleniyor...</span>
            <span v-else>Takımı Ekle</span>
          </button>
        </div>
      </div>
    </div>
    <!-- Rol Düzenleme Modal -->
    <div v-if="showRoleModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl p-6 w-full max-w-md shadow-2xl">
        <h4 class="text-lg font-semibold mb-1 text-gray-900 dark:text-white">Rolleri Düzenle</h4>
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
          {{ editingMember?.userName }} için rolleri seçin
        </p>
        <div class="border border-gray-200 dark:border-gray-600 rounded-lg p-2 max-h-60 overflow-y-auto space-y-1">
          <label
            v-for="role in availableRoles"
            :key="role.id"
            class="flex items-center gap-2 cursor-pointer text-sm px-2 py-1.5 rounded hover:bg-gray-50 dark:hover:bg-gray-700/30"
          >
            <input type="checkbox" :value="role.id" v-model="editRoleIds" class="rounded text-indigo-600" />
            <span class="w-2.5 h-2.5 rounded-full flex-shrink-0" :style="{ backgroundColor: role.color || '#6B7280' }"></span>
            <span class="text-gray-700 dark:text-gray-300">{{ role.name }}</span>
          </label>
        </div>
        <div class="flex gap-2 mt-4 justify-end">
          <button @click="showRoleModal = false" class="btn-secondary">İptal</button>
          <button @click="saveRoles" :disabled="savingRoles" class="btn-primary">
            {{ savingRoles ? 'Kaydediliyor...' : 'Kaydet' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import ProjectApi from '../../api/ProjectApi.js'
import RoleApi from '../../api/RoleApi.js'
import { getTeamsByOrg } from '../../api/TeamApi.js'

const props = defineProps({
  projectId: { type: String, required: true },
  organizationId: { type: String, required: true }
})

const members = ref([])
const availableRoles = ref([])
const loading = ref(false)

// Üye ekleme
const showAddModal = ref(false)
const addEmail = ref('')
const addRoleIds = ref([])
const addMemberType = ref('MEMBER')

// Takım ekleme
const showAddTeamModal = ref(false)
const teamSearchQuery = ref('')
const teamSearchResults = ref([])
const teamsLoading = ref(false)
const showTeamDropdown = ref(false)
const selectedTeam = ref(null)
const teamSearchRef = ref(null)
const teamRoleIds = ref([])
const teamMemberType = ref('MEMBER')
const addingTeam = ref(false)
const addTeamResult = ref('')

// Rol düzenleme
const showRoleModal = ref(false)
const editingMember = ref(null)
const editRoleIds = ref([])
const savingRoles = ref(false)

let searchDebounce = null

async function loadData() {
  loading.value = true
  try {
    const [membersRes, rolesRes] = await Promise.all([
      ProjectApi.getMembers(props.projectId),
      RoleApi.getAll('PROJECT'),
    ])
    members.value = membersRes.data
    availableRoles.value = rolesRes.data
  } catch (e) {
    console.error('Veri yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function addMember() {
  try {
    await ProjectApi.addMember(props.projectId, addEmail.value, addRoleIds.value.length ? addRoleIds.value : null, addMemberType.value)
    showAddModal.value = false
    addEmail.value = ''
    addRoleIds.value = []
    addMemberType.value = 'MEMBER'
    await loadData()
  } catch (e) {
    console.error('Üye eklenemedi:', e)
  }
}

// --- Takım Autocomplete ---
function onTeamSearch() {
  selectedTeam.value = null
  showTeamDropdown.value = true
  clearTimeout(searchDebounce)
  searchDebounce = setTimeout(() => searchTeams(teamSearchQuery.value), 300)
}

async function searchTeams(query) {
  teamsLoading.value = true
  try {
    teamSearchResults.value = await getTeamsByOrg(props.organizationId, query)
  } catch (e) {
    console.error('Takım araması başarısız:', e)
    teamSearchResults.value = []
  } finally {
    teamsLoading.value = false
  }
}

function selectTeam(team) {
  selectedTeam.value = team
  teamSearchQuery.value = ''
  showTeamDropdown.value = false
}

function clearTeamSelection() {
  selectedTeam.value = null
  teamSearchQuery.value = ''
  teamSearchResults.value = []
}

function handleOutsideClick(e) {
  if (teamSearchRef.value && !teamSearchRef.value.contains(e.target)) {
    showTeamDropdown.value = false
  }
}

// Modal açıldığında ilk takımları yükle
watch(showAddTeamModal, async (val) => {
  if (val) {
    addTeamResult.value = ''
    await searchTeams('')
    document.addEventListener('click', handleOutsideClick)
  } else {
    document.removeEventListener('click', handleOutsideClick)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideClick)
})

async function addTeam() {
  if (!selectedTeam.value) return
  addingTeam.value = true
  addTeamResult.value = ''
  try {
    const res = await ProjectApi.addTeam(
      props.projectId,
      selectedTeam.value.id,
      teamRoleIds.value.length ? teamRoleIds.value : null,
      teamMemberType.value
    )
    const count = res.data?.length ?? 0
    addTeamResult.value = `${count} üye başarıyla projeye eklendi.`
    await loadData()
  } catch (e) {
    console.error('Takım eklenemedi:', e)
  } finally {
    addingTeam.value = false
  }
}

function closeTeamModal() {
  showAddTeamModal.value = false
  selectedTeam.value = null
  teamSearchQuery.value = ''
  teamSearchResults.value = []
  teamRoleIds.value = []
  teamMemberType.value = 'MEMBER'
  addTeamResult.value = ''
}

function openRoleModal(member) {
  editingMember.value = member
  editRoleIds.value = (member.roles || []).map(r => r.id)
  showRoleModal.value = true
}

async function saveRoles() {
  if (!editingMember.value) return
  savingRoles.value = true
  try {
    await ProjectApi.updateMemberRoles(props.projectId, editingMember.value.userId, editRoleIds.value)
    showRoleModal.value = false
    editingMember.value = null
    await loadData()
  } catch (e) {
    console.error('Roller güncellenemedi:', e)
  } finally {
    savingRoles.value = false
  }
}

async function removeMember(member) {
  if (!confirm(`${member.userName} üyeyi projeden çıkarmak istediğinize emin misiniz?`)) return
  try {
    await ProjectApi.removeMember(props.projectId, member.userId)
    await loadData()
  } catch (e) {
    console.error('Üye çıkarılamadı:', e)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR')
}

onMounted(loadData)
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500; }
.label { @apply block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 transition-colors; }
</style>

