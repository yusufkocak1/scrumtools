<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-6xl mx-auto space-y-6">
      <!-- Yükleniyor -->
      <div v-if="loading" class="text-center py-20 text-gray-500">Proje yükleniyor...</div>

      <template v-else-if="project">
        <!-- Başlık -->
        <div class="flex items-start justify-between">
          <div class="flex items-center gap-4">
            <div
              class="w-12 h-12 rounded-xl flex items-center justify-center text-white font-bold text-lg"
              :style="{ backgroundColor: project.color || '#6366f1' }"
            >
              {{ project.key?.charAt(0) }}
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ project.name }}</h1>
                <span class="text-xs font-mono text-gray-500 bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded">{{ project.key }}</span>
              </div>
              <p class="text-gray-500 text-sm mt-0.5">{{ project.organizationName }}</p>
            </div>
          </div>
          <button @click="activeTab = 'settings'" class="btn-secondary text-sm">⚙️ Ayarlar</button>
        </div>

        <!-- Tabs -->
        <div class="flex gap-1 bg-gray-100 dark:bg-gray-800 rounded-xl p-1 w-fit">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            :class="[
              'px-4 py-2 text-sm rounded-lg transition-all font-medium',
              activeTab === tab.id
                ? 'bg-white dark:bg-gray-700 text-indigo-600 shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
            ]"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- Genel Bakış -->
        <div v-if="activeTab === 'overview'">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
              <p class="text-sm text-gray-500">Üye Sayısı</p>
              <p class="text-3xl font-bold text-indigo-600 mt-1">{{ project.memberCount }}</p>
            </div>
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
              <p class="text-sm text-gray-500">Proje Tipi</p>
              <p class="text-xl font-semibold text-gray-900 dark:text-white mt-1">{{ formatType(project.projectType) }}</p>
            </div>
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
              <p class="text-sm text-gray-500">Durum</p>
              <span class="mt-1 inline-block px-3 py-1 rounded-full text-sm"
                :class="project.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'">
                {{ project.status === 'ACTIVE' ? 'Aktif' : project.status }}
              </span>
            </div>
          </div>
          <div v-if="project.description" class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 mt-4">
            <h3 class="font-medium text-gray-900 dark:text-white mb-2">Açıklama</h3>
            <p class="text-gray-600 dark:text-gray-400 text-sm">{{ project.description }}</p>
          </div>

          <!-- Git / SCM: Bağlı Repolar -->
          <div v-if="project.organizationId" class="mt-4">
            <ScmRepoMappingPanel :project-id="project.id" :organization-id="project.organizationId" />
          </div>
        </div>

        <!-- Üyeler -->
        <div v-if="activeTab === 'members'">
          <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
            <ProjectMemberManager :project-id="project.id" :organization-id="project.organizationId" />
          </div>
        </div>

        <!-- Roller -->
        <div v-if="activeTab === 'roles'">
          <div class="space-y-6">
            <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-6">
              <RoleManager />
            </div>
            <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-6">
              <h3 class="font-semibold text-gray-900 dark:text-white mb-4">İzin Matrisi</h3>
              <PermissionMatrix :roles="defaultRoles" />
            </div>
          </div>
        </div>

        <!-- Ayarlar -->
        <div v-if="activeTab === 'settings'">
          <ProjectSettings
            :project="project"
            @updated="project = $event; activeTab = 'overview'"
            @cancel="activeTab = 'overview'"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ProjectSettings from '../components/project/ProjectSettings.vue'
import ProjectMemberManager from '../components/project/ProjectMemberManager.vue'
import ScmRepoMappingPanel from '../components/scm/ScmRepoMappingPanel.vue'
import RoleManager from '../components/roles/RoleManager.vue'
import PermissionMatrix from '../components/roles/PermissionMatrix.vue'
import ProjectApi from '../api/ProjectApi.js'
import RoleApi from '../api/RoleApi.js'

const route = useRoute()
const project = ref(null)
const defaultRoles = ref([])
const loading = ref(false)
const activeTab = ref('overview')

const tabs = [
  { id: 'overview', label: 'Genel Bakış' },
  { id: 'members', label: 'Üyeler' },
  { id: 'roles', label: 'Roller' },
  { id: 'settings', label: 'Ayarlar' },
]

async function loadProject() {
  loading.value = true
  try {
    const res = await ProjectApi.getById(route.params.id)
    project.value = res.data
  } catch (e) {
    console.error('Proje yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await RoleApi.getDefaults()
    defaultRoles.value = res.data
  } catch (e) {
    console.error('Roller yüklenemedi:', e)
  }
}

function formatType(type) {
  const map = { SCRUM: 'Scrum', KANBAN: 'Kanban', BUG_TRACKING: 'Bug Tracking', CUSTOM: 'Özel' }
  return map[type] || type
}

onMounted(() => {
  loadProject()
  loadRoles()
})
</script>

<style scoped>
.btn-secondary { @apply px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 transition-colors; }
</style>

