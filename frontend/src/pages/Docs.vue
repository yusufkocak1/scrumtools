<template>
  <div class="min-h-screen bg-slate-50">
    <!-- Header -->
    <div class="sticky top-0 z-20 bg-white/95 backdrop-blur border-b border-slate-200">
      <div class="px-4 sm:px-8 py-4 flex flex-wrap items-center gap-3">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-white flex items-center justify-center shadow-sm shadow-indigo-200">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25"/>
            </svg>
          </div>
          <div>
            <h1 class="text-lg font-bold text-slate-800 leading-tight">Docs</h1>
            <p class="text-xs text-slate-500">Proje dokümantasyonu ve bilgi yönetimi</p>
          </div>
        </div>

        <!-- Project Selector -->
        <div v-if="selectedProject" class="project-picker relative sm:ml-3">
          <button @click="showProjectDropdown = !showProjectDropdown"
                  class="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-sm hover:border-indigo-200 hover:bg-indigo-50/50 transition">
            <span :class="['w-6 h-6 rounded-lg bg-gradient-to-br text-white text-xs font-bold flex items-center justify-center', avatarGradient(selectedProject.name)]">
              {{ selectedProject.name?.charAt(0).toUpperCase() }}
            </span>
            <span class="text-slate-700 font-medium max-w-[180px] truncate">{{ selectedProject.name }}</span>
            <svg class="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5"/>
            </svg>
          </button>
          <!-- Dropdown -->
          <div v-if="showProjectDropdown"
               class="absolute top-full left-0 mt-1.5 w-72 bg-white border border-slate-200 rounded-xl shadow-xl shadow-slate-200/60 z-50 py-1 max-h-80 overflow-y-auto">
            <div class="px-2.5 py-2 border-b border-slate-100">
              <div class="relative">
                <svg class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/>
                </svg>
                <input v-model="projectSearch" type="text" placeholder="Proje ara..."
                       class="w-full border border-slate-200 rounded-lg pl-8 pr-2.5 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition"/>
              </div>
            </div>
            <div v-for="proj in filteredProjects" :key="proj.id"
                 @click="selectProject(proj)"
                 :class="['mx-1 my-0.5 px-2 py-2 flex items-center gap-2.5 cursor-pointer transition text-sm rounded-lg',
                   proj.id === effectiveProjectId ? 'bg-indigo-50 text-indigo-700' : 'hover:bg-slate-50']">
              <span :class="['w-7 h-7 rounded-lg bg-gradient-to-br text-white flex items-center justify-center text-xs font-bold shrink-0', avatarGradient(proj.name)]">
                {{ proj.name?.charAt(0).toUpperCase() }}
              </span>
              <div class="min-w-0">
                <p class="font-medium truncate">{{ proj.name }}</p>
                <p class="text-xs text-slate-400 truncate">{{ proj.orgName }}</p>
              </div>
              <svg v-if="proj.id === effectiveProjectId" class="ml-auto w-4 h-4 text-indigo-500 shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/>
              </svg>
            </div>
            <div v-if="filteredProjects.length === 0" class="px-3 py-5 text-center text-slate-400 text-sm">
              Proje bulunamadı
            </div>
          </div>
        </div>

        <button
            v-if="canManageSpaces && effectiveProjectId"
            @click="showCreateSpace = true"
            class="ml-auto inline-flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-500 active:bg-indigo-700 text-white text-sm font-medium px-3.5 py-2 rounded-xl shadow-sm shadow-indigo-200 transition">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
          </svg>
          Yeni Space
        </button>
      </div>
    </div>

    <!-- Project Selection Screen (when no project selected yet) -->
    <div v-if="!effectiveProjectId" class="p-4 sm:p-8">
      <div v-if="loadingProjects" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="i in 6" :key="i" class="bg-white rounded-2xl border border-slate-200 p-5 animate-pulse">
          <div class="flex items-center gap-3">
            <div class="w-11 h-11 bg-slate-100 rounded-xl"></div>
            <div class="flex-1 space-y-2">
              <div class="h-3.5 bg-slate-100 rounded w-2/3"></div>
              <div class="h-2.5 bg-slate-100 rounded w-1/3"></div>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="allProjects.length === 0" class="text-center py-20">
        <div class="w-16 h-16 mx-auto rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mb-4">
          <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-slate-700">Henüz projeniz yok</h3>
        <p class="text-slate-500 mt-1 text-sm">Docs kullanmak için önce bir proje oluşturmanız gerekiyor</p>
      </div>
      <div v-else>
        <h2 class="text-base font-semibold text-slate-700 mb-4">Bir proje seçin</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div v-for="proj in allProjects" :key="proj.id"
               @click="selectProject(proj)"
               class="group bg-white rounded-2xl border border-slate-200 p-5 cursor-pointer transition-all duration-200 hover:border-indigo-200 hover:shadow-lg hover:shadow-indigo-500/5 hover:-translate-y-0.5">
            <div class="flex items-center gap-3">
              <div :class="['w-11 h-11 rounded-xl bg-gradient-to-br text-white flex items-center justify-center font-bold text-lg shadow-sm', avatarGradient(proj.name)]">
                {{ proj.name?.charAt(0).toUpperCase() }}
              </div>
              <div class="min-w-0">
                <h3 class="font-semibold text-slate-800 group-hover:text-indigo-600 transition truncate">{{ proj.name }}</h3>
                <p class="text-xs text-slate-400 truncate">{{ proj.orgName }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Space List (when project is selected) -->
    <div v-else class="p-4 sm:p-8">
      <!-- Toolbar -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div class="flex items-center gap-2">
          <h2 class="text-base font-semibold text-slate-700">Spaces</h2>
          <span v-if="!loading" class="text-xs font-medium bg-slate-200/70 text-slate-500 px-2 py-0.5 rounded-full">{{ filteredSpaces.length }}</span>
        </div>
        <div v-if="spaces.length > 0" class="relative w-full sm:w-64">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/>
          </svg>
          <input v-model="spaceSearch" type="text" placeholder="Space ara..."
                 class="w-full bg-white border border-slate-200 rounded-xl pl-9 pr-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition placeholder:text-slate-400"/>
        </div>
      </div>

      <!-- Skeleton -->
      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="i in 6" :key="i" class="bg-white rounded-2xl border border-slate-200 p-5 animate-pulse">
          <div class="w-11 h-11 bg-slate-100 rounded-xl"></div>
          <div class="h-3.5 bg-slate-100 rounded w-1/2 mt-4"></div>
          <div class="h-2.5 bg-slate-100 rounded w-full mt-3"></div>
          <div class="h-2.5 bg-slate-100 rounded w-2/3 mt-2"></div>
        </div>
      </div>

      <!-- Empty: no spaces at all -->
      <div v-else-if="spaces.length === 0" class="text-center py-20">
        <div class="w-16 h-16 mx-auto rounded-2xl bg-indigo-50 text-indigo-400 flex items-center justify-center mb-4">
          <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-slate-700">Henüz space yok</h3>
        <p class="text-slate-500 mt-1 text-sm">Proje dokümantasyonunuz için ilk space'i oluşturun</p>
        <button v-if="canManageSpaces" @click="showCreateSpace = true"
                class="mt-5 inline-flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium px-4 py-2 rounded-xl shadow-sm shadow-indigo-200 transition">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
          </svg>
          İlk Space'i Oluştur
        </button>
      </div>

      <!-- Empty: search returned nothing -->
      <div v-else-if="filteredSpaces.length === 0" class="text-center py-16">
        <p class="text-slate-500 text-sm">"<span class="font-medium text-slate-700">{{ spaceSearch }}</span>" ile eşleşen space bulunamadı</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
            v-for="space in filteredSpaces"
            :key="space.id"
            @click="openSpace(space)"
            class="group bg-white rounded-2xl border border-slate-200 p-5 cursor-pointer transition-all duration-200 hover:border-indigo-200 hover:shadow-lg hover:shadow-indigo-500/5 hover:-translate-y-0.5">
          <div class="flex items-start justify-between gap-3">
            <div :class="['w-11 h-11 rounded-xl bg-gradient-to-br text-white flex items-center justify-center font-bold text-lg shadow-sm', avatarGradient(space.name)]">
              {{ space.name.charAt(0).toUpperCase() }}
            </div>
            <button
                v-if="canManageSpaces"
                @click.stop="editSpace(space)"
                title="Space ayarları"
                class="opacity-0 group-hover:opacity-100 p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z"/>
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
              </svg>
            </button>
          </div>
          <h3 class="mt-3 font-semibold text-slate-800 group-hover:text-indigo-600 transition truncate">{{ space.name }}</h3>
          <p :class="['mt-1 text-sm line-clamp-2 min-h-[2.5rem]', space.description ? 'text-slate-500' : 'text-slate-300 italic']">
            {{ space.description || 'Açıklama eklenmemiş' }}
          </p>
          <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between gap-2 text-xs text-slate-400">
            <span class="inline-flex items-center gap-1.5 shrink-0">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"/>
              </svg>
              {{ space.pageCount }} sayfa
            </span>
            <span class="truncate">{{ space.createdByName }} · {{ formatDate(space.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Space Modal -->
    <div v-if="showCreateSpace || editingSpace"
         class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4"
         @click.self="closeSpaceModal">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-100 flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25"/>
            </svg>
          </div>
          <h2 class="font-semibold text-slate-800">{{ editingSpace ? 'Space Düzenle' : 'Yeni Space Oluştur' }}</h2>
          <button @click="closeSpaceModal" class="ml-auto p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1.5">Adı <span class="text-rose-500">*</span></label>
            <input v-model="spaceForm.name" type="text" placeholder="Örn: API Docs, Test Verileri..."
                   class="w-full border border-slate-200 rounded-xl px-3.5 py-2.5 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition placeholder:text-slate-400"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1.5">Açıklama</label>
            <textarea v-model="spaceForm.description" rows="3" placeholder="Space hakkında kısa bir açıklama..."
                      class="w-full border border-slate-200 rounded-xl px-3.5 py-2.5 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition placeholder:text-slate-400 resize-none"/>
          </div>
        </div>
        <div class="px-6 py-4 bg-slate-50 flex justify-end gap-2">
          <button @click="closeSpaceModal"
                  class="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-200/60 rounded-xl transition">İptal</button>
          <button @click="saveSpace" :disabled="!spaceForm.name"
                  class="bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-300 disabled:cursor-not-allowed text-white text-sm font-medium px-4 py-2 rounded-xl shadow-sm shadow-indigo-200 disabled:shadow-none transition">
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
const spaceSearch = ref('')
const showCreateSpace = ref(false)
const editingSpace = ref(null)
const spaceForm = ref({name: '', description: ''})

const filteredSpaces = computed(() => {
  if (!spaceSearch.value.trim()) return spaces.value
  const q = spaceSearch.value.toLowerCase()
  return spaces.value.filter(s =>
      s.name.toLowerCase().includes(q) || s.description?.toLowerCase().includes(q)
  )
})

// TODO: gerçek yetki kontrolü — şimdilik true
const canManageSpaces = ref(true)

// İsimden deterministik gradyan seçimi (space/proje avatarları için)
const avatarGradients = [
  'from-indigo-500 to-violet-600',
  'from-sky-500 to-blue-600',
  'from-emerald-500 to-teal-600',
  'from-amber-500 to-orange-600',
  'from-rose-500 to-pink-600',
  'from-fuchsia-500 to-purple-600'
]

function avatarGradient(name) {
  let h = 0
  for (const ch of (name || '')) h = (h + ch.charCodeAt(0)) % 997
  return avatarGradients[h % avatarGradients.length]
}

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
  if (showProjectDropdown.value && !e.target.closest('.project-picker')) {
    showProjectDropdown.value = false
  }
}
onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))

// Watch for route changes — space yüklemesinin tek noktası burası:
// selectProject route'u değiştirir, spaces bu watcher üzerinden yüklenir
watch(routeProjectId, async (newId, oldId) => {
  if (!newId || newId === oldId) return
  const found = allProjects.value.find(p => p.id === newId)
  if (found) selectedProject.value = found
  localStorage.setItem('docs_last_project_id', newId)
  await loadSpaces()
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
  // URL'yi güncelle — spaces, routeProjectId watcher'ı üzerinden yüklenir.
  // Route zaten bu projedeyse (ör. sayfa /docs iken localStorage seçimi) watcher
  // tetiklenmeyeceği için loadSpaces'i doğrudan çağırıyoruz.
  if (routeProjectId.value === proj.id) {
    loadSpaces()
  } else {
    router.replace({name: 'Docs', params: {projectId: proj.id}})
  }
}

// Hızlı proje geçişlerinde geç gelen eski yanıtın güncel listeyi ezmemesi için istek sayacı
let spacesRequestSeq = 0

async function loadSpaces() {
  if (!effectiveProjectId.value) return
  const reqId = ++spacesRequestSeq
  loading.value = true
  try {
    const res = await DocApi.getSpaces(effectiveProjectId.value)
    if (reqId !== spacesRequestSeq) return
    spaces.value = res.data
  } catch (e) {
    console.error('Spaces yüklenemedi:', e)
  } finally {
    if (reqId === spacesRequestSeq) loading.value = false
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
