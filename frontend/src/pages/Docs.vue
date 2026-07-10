<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header -->
    <div class="bg-white border-b px-4 sm:px-6 py-4 flex flex-wrap items-center justify-between gap-3">
      <div class="flex flex-wrap items-center gap-3 sm:gap-4">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">📚 Docs</h1>
          <p class="text-sm text-gray-500 mt-1">Proje dokümantasyonu ve bilgi yönetimi</p>
        </div>
        <!-- Project Selector -->
        <div v-if="selectedProject" class="relative ml-4">
          <button @click="showProjectDropdown = !showProjectDropdown"
                  class="flex items-center gap-2 border rounded-lg px-3 py-1.5 text-sm bg-gray-50 hover:bg-gray-100 transition">
            <span class="w-6 h-6 bg-indigo-100 rounded flex items-center justify-center text-indigo-600 text-xs font-bold">
              {{ selectedProject.name?.charAt(0).toUpperCase() }}
            </span>
            <span class="text-gray-700 font-medium max-w-[200px] truncate">{{ selectedProject.name }}</span>
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
            </svg>
          </button>
          <!-- Dropdown -->
          <div v-if="showProjectDropdown"
               class="absolute top-full left-0 mt-1 w-72 bg-white border rounded-xl shadow-xl z-50 py-1 max-h-80 overflow-y-auto">
            <div class="px-3 py-2 border-b">
              <input v-model="projectSearch" type="text" placeholder="Proje ara..."
                     class="w-full border rounded-lg px-2.5 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-300"/>
            </div>
            <div v-for="proj in filteredProjects" :key="proj.id"
                 @click="selectProject(proj)"
                 :class="['px-3 py-2 flex items-center gap-3 cursor-pointer transition text-sm',
                   proj.id === effectiveProjectId ? 'bg-indigo-50 text-indigo-700' : 'hover:bg-gray-50']">
              <span class="w-7 h-7 rounded flex items-center justify-center text-xs font-bold shrink-0"
                    :class="proj.id === effectiveProjectId ? 'bg-indigo-200 text-indigo-700' : 'bg-gray-100 text-gray-600'">
                {{ proj.name?.charAt(0).toUpperCase() }}
              </span>
              <div class="min-w-0">
                <p class="font-medium truncate">{{ proj.name }}</p>
                <p class="text-xs text-gray-400 truncate">{{ proj.orgName }}</p>
              </div>
              <span v-if="proj.id === effectiveProjectId" class="ml-auto text-indigo-500">✓</span>
            </div>
            <div v-if="filteredProjects.length === 0" class="px-3 py-4 text-center text-gray-400 text-sm">
              Proje bulunamadı
            </div>
          </div>
        </div>
      </div>
      <button
          v-if="canManageSpaces && effectiveProjectId"
          @click="showCreateSpace = true"
          class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition">
        <span>＋</span> Yeni Space
      </button>
    </div>

    <!-- Project Selection Screen (when no project selected yet) -->
    <div v-if="!effectiveProjectId" class="p-6">
      <div v-if="loadingProjects" class="text-center py-12 text-gray-500">Projeler yükleniyor...</div>
      <div v-else-if="allProjects.length === 0" class="text-center py-16">
        <div class="text-6xl mb-4">📂</div>
        <h3 class="text-lg font-medium text-gray-700">Henüz projeniz yok</h3>
        <p class="text-gray-500 mt-1">Docs kullanmak için önce bir proje oluşturmanız gerekiyor</p>
      </div>
      <div v-else>
        <h2 class="text-lg font-semibold text-gray-700 mb-4">Bir proje seçin</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div v-for="proj in allProjects" :key="proj.id"
               @click="selectProject(proj)"
               class="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md hover:border-indigo-300 cursor-pointer transition group">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 bg-indigo-100 rounded-lg flex items-center justify-center text-indigo-600 font-bold text-xl">
                {{ proj.name?.charAt(0).toUpperCase() }}
              </div>
              <div>
                <h3 class="font-semibold text-gray-800 group-hover:text-indigo-600 transition">{{ proj.name }}</h3>
                <p class="text-xs text-gray-500">{{ proj.orgName }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Space List (when project is selected) -->
    <div v-else class="p-6">
      <div v-if="loading" class="text-center py-12 text-gray-500">Yükleniyor...</div>

      <div v-else-if="spaces.length === 0" class="text-center py-12">
        <div class="text-6xl mb-4">📝</div>
        <h3 class="text-lg font-medium text-gray-700">Henüz space yok</h3>
        <p class="text-gray-500 mt-1">Proje dokümantasyonunuz için ilk space'i oluşturun</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
            v-for="space in spaces"
            :key="space.id"
            @click="openSpace(space)"
            class="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md hover:border-indigo-300 cursor-pointer transition group">
          <div class="flex items-start justify-between">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center text-indigo-600 font-bold text-lg">
                {{ space.name.charAt(0).toUpperCase() }}
              </div>
              <div>
                <h3 class="font-semibold text-gray-800 group-hover:text-indigo-600 transition">{{ space.name }}</h3>
                <p class="text-xs text-gray-500">{{ space.pageCount }} sayfa</p>
              </div>
            </div>
            <button
                v-if="canManageSpaces"
                @click.stop="editSpace(space)"
                class="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-gray-600 transition">
              ⚙️
            </button>
          </div>
          <p v-if="space.description" class="text-sm text-gray-600 mt-3 line-clamp-2">{{ space.description }}</p>
          <div class="mt-3 flex items-center gap-2 text-xs text-gray-400">
            <span>{{ space.createdByName }}</span>
            <span>•</span>
            <span>{{ formatDate(space.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Space Modal -->
    <div v-if="showCreateSpace || editingSpace" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-md">
        <h2 class="text-lg font-bold mb-4">{{ editingSpace ? 'Space Düzenle' : 'Yeni Space Oluştur' }}</h2>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Adı *</label>
            <input v-model="spaceForm.name" type="text" placeholder="Örn: API Docs, Test Verileri..."
                   class="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
            <textarea v-model="spaceForm.description" rows="3" placeholder="Space hakkında kısa bir açıklama..."
                      class="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"/>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="closeSpaceModal" class="px-4 py-2 text-gray-600 hover:text-gray-800 transition">İptal</button>
          <button @click="saveSpace" :disabled="!spaceForm.name"
                  class="bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-300 text-white px-4 py-2 rounded-lg transition">
            {{ editingSpace ? 'Güncelle' : 'Oluştur' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, computed, watch, onBeforeUnmount} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import DocApi from '../api/DocApi.js'
import OrganizationApi from '../api/OrganizationApi.js'
import ProjectApi from '../api/ProjectApi.js'

const router = useRouter()
const route = useRoute()

// projectId from route params (may be undefined if on /docs)
const routeProjectId = computed(() => route.params.projectId || null)

// All projects for selection
const allProjects = ref([])
const loadingProjects = ref(false)
const selectedProject = ref(null)
const showProjectDropdown = ref(false)
const projectSearch = ref('')

// Effective projectId: from route or from selection
const effectiveProjectId = computed(() => routeProjectId.value || selectedProject.value?.id || null)

const filteredProjects = computed(() => {
  if (!projectSearch.value) return allProjects.value
  const q = projectSearch.value.toLowerCase()
  return allProjects.value.filter(p =>
      p.name.toLowerCase().includes(q) || p.orgName?.toLowerCase().includes(q)
  )
})

const spaces = ref([])
const loading = ref(false)
const showCreateSpace = ref(false)
const editingSpace = ref(null)
const spaceForm = ref({name: '', description: ''})

// TODO: gerçek yetki kontrolü — şimdilik true
const canManageSpaces = ref(true)

onMounted(async () => {
  await loadAllProjects()

  // Eğer route'da projectId varsa, selectedProject'ı ayarla
  if (routeProjectId.value) {
    const found = allProjects.value.find(p => p.id === routeProjectId.value)
    if (found) {
      selectedProject.value = found
    } else {
      // allProjects'te bulunamadıysa, API'den çek
      try {
        const res = await ProjectApi.getById(routeProjectId.value)
        selectedProject.value = res.data
      } catch (e) {
        console.error('Proje bulunamadı:', e)
      }
    }
    localStorage.setItem('docs_last_project_id', routeProjectId.value)
    await loadSpaces()
  } else {
    // route'da projectId yok, localStorage'dan kontrol et
    const lastId = localStorage.getItem('docs_last_project_id')
    if (lastId) {
      const found = allProjects.value.find(p => p.id === lastId)
      if (found) {
        selectProject(found)
        return
      }
    }
  }
})

// Close dropdown on outside click
function onClickOutside(e) {
  if (showProjectDropdown.value && !e.target.closest('.relative')) {
    showProjectDropdown.value = false
  }
}
onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))

// Watch for route changes
watch(routeProjectId, async (newId) => {
  if (newId && newId !== selectedProject.value?.id) {
    const found = allProjects.value.find(p => p.id === newId)
    if (found) selectedProject.value = found
    localStorage.setItem('docs_last_project_id', newId)
    await loadSpaces()
  }
})

async function loadAllProjects() {
  loadingProjects.value = true
  allProjects.value = []
  try {
    const orgRes = await OrganizationApi.getMyOrganizations()
    for (const org of orgRes.data) {
      const projRes = await ProjectApi.getByOrg(org.id)
      for (const proj of projRes.data) {
        allProjects.value.push({...proj, orgName: org.name})
      }
    }
  } catch (e) {
    console.error('Projeler yüklenemedi:', e)
  } finally {
    loadingProjects.value = false
  }
}

function selectProject(proj) {
  selectedProject.value = proj
  showProjectDropdown.value = false
  projectSearch.value = ''
  localStorage.setItem('docs_last_project_id', proj.id)
  // URL'yi güncelle
  router.replace({name: 'Docs', params: {projectId: proj.id}})
  loadSpaces()
}

async function loadSpaces() {
  if (!effectiveProjectId.value) return
  loading.value = true
  try {
    const res = await DocApi.getSpaces(effectiveProjectId.value)
    spaces.value = res.data
  } catch (e) {
    console.error('Spaces yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function openSpace(space) {
  router.push({name: 'DocSpace', params: {projectId: effectiveProjectId.value, spaceId: space.id}})
}

function editSpace(space) {
  editingSpace.value = space
  spaceForm.value = {name: space.name, description: space.description || ''}
}

function closeSpaceModal() {
  showCreateSpace.value = false
  editingSpace.value = null
  spaceForm.value = {name: '', description: ''}
}

async function saveSpace() {
  try {
    if (editingSpace.value) {
      await DocApi.updateSpace(effectiveProjectId.value, editingSpace.value.id, spaceForm.value)
    } else {
      await DocApi.createSpace(effectiveProjectId.value, spaceForm.value)
    }
    closeSpaceModal()
    await loadSpaces()
  } catch (e) {
    console.error('Space kaydedilemedi:', e)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('tr-TR')
}
</script>

