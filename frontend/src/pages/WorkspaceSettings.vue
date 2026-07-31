<template>
  <div class="flex flex-col lg:flex-row w-full min-h-screen bg-gray-50">
    <!-- Kategori navigasyonu -->
    <aside class="lg:w-64 lg:shrink-0 bg-white border-b lg:border-b-0 lg:border-r border-gray-200">
      <div class="px-4 py-4 border-b border-gray-100">
        <h1 class="text-sm font-semibold uppercase tracking-wide text-gray-400">Çalışma Ayarları</h1>
        <p class="mt-1 text-sm font-medium text-gray-900 truncate">{{ activeTeam?.teamName || 'Takım' }}</p>

        <!-- Proje kapsamı: durum seti ve board'lar projeye göre daralabilir -->
        <div v-if="hasProjects" class="mt-3">
          <label class="block text-[11px] font-semibold uppercase tracking-wide text-gray-400 mb-1">Kapsam</label>
          <select
            :value="projectId ?? ALL_PROJECTS"
            @change="selectProject($event.target.value === ALL_PROJECTS ? null : $event.target.value)"
            class="w-full text-xs rounded-md border border-gray-300 px-2 py-1.5 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
          >
            <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }} ({{ p.key }})</option>
            <option :value="ALL_PROJECTS">Takım geneli</option>
          </select>
        </div>
      </div>

      <nav class="p-2 space-y-4">
        <div v-for="group in navGroups" :key="group.label">
          <p class="px-2.5 pt-2 pb-1 text-[11px] font-semibold uppercase tracking-wide text-gray-400">
            {{ group.label }}
          </p>
          <button
            v-for="item in group.items"
            :key="item.key"
            class="flex items-center gap-2.5 w-full px-2.5 py-2 text-sm font-medium rounded-lg transition-colors text-left"
            :class="activeSection === item.key
              ? 'bg-blue-50 text-blue-700'
              : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'"
            @click="activeSection = item.key"
          >
            <span class="w-4 text-center shrink-0">{{ item.icon }}</span>
            <span class="truncate">{{ item.label }}</span>
          </button>
        </div>

        <!-- Bu modülün dışında yaşayan ayarlar — burada da tek listede görünsün -->
        <div>
          <p class="px-2.5 pt-2 pb-1 text-[11px] font-semibold uppercase tracking-wide text-gray-400">
            Diğer
          </p>
          <router-link
            v-for="link in externalLinks"
            :key="link.to"
            :to="link.to"
            class="flex items-center gap-2.5 w-full px-2.5 py-2 text-sm font-medium rounded-lg text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-colors"
          >
            <span class="w-4 text-center shrink-0">{{ link.icon }}</span>
            <span class="truncate">{{ link.label }}</span>
            <span class="ml-auto text-gray-300 text-xs">↗</span>
          </router-link>
        </div>
      </nav>
    </aside>

    <!-- İçerik -->
    <div class="flex-1 min-w-0 p-4 sm:p-6">
      <div class="max-w-4xl mx-auto">
        <div v-if="!resolvedTeamId" class="rounded-lg border border-gray-200 bg-white p-8 text-center text-sm text-gray-500">
          Önce bir takım seçin.
          <router-link to="/settings" class="text-blue-600 font-medium hover:underline">Çalışma alanı ayarları →</router-link>
        </div>

        <template v-else>
          <StatusManager
            v-if="activeSection === 'statuses'"
            :key="`status-${resolvedTeamId}-${projectId || 'team'}`"
            :team-id="resolvedTeamId"
            :project-id="projectId"
            @changed="bumpVersion"
          />

          <BoardManager
            v-else-if="activeSection === 'boards'"
            :key="`board-${resolvedTeamId}-${statusVersion}`"
            :team-id="resolvedTeamId"
            :project-id="projectId"
            :projects="projects"
            @changed="bumpVersion"
          />

          <div v-else-if="activeSection === 'projects'" class="space-y-4">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">Takım Projeleri</h2>
              <p class="text-sm text-gray-500 mt-0.5">
                Takımın üzerinde çalıştığı projeler — görev, board ve sürüm görünümleri bu listeden beslenir.
              </p>
            </div>
            <button
              class="px-3 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700"
              @click="showTeamProjects = true"
            >
              Projeleri Yönet
            </button>
            <ul v-if="projects.length" class="space-y-2">
              <li
                v-for="p in projects"
                :key="p.id"
                class="flex items-center gap-3 rounded-lg border border-gray-200 bg-white px-4 py-3"
              >
                <span class="w-2.5 h-2.5 rounded-full shrink-0" :style="{ backgroundColor: p.color || '#3B82F6' }"></span>
                <span class="text-sm font-medium text-gray-900 truncate">{{ p.name }}</span>
                <span class="text-xs text-gray-400">{{ p.key }}</span>
                <router-link
                  :to="`/projects/${p.id}`"
                  class="ml-auto text-xs text-blue-600 hover:underline shrink-0"
                >Proje ayarları →</router-link>
              </li>
            </ul>
            <p v-else class="text-sm text-gray-400">Takıma bağlı proje yok.</p>
          </div>
        </template>
      </div>
    </div>

    <TeamProjectsModal
      v-if="showTeamProjects"
      :team-id="resolvedTeamId"
      :organization-id="organizationId"
      :team-projects="projects"
      @close="showTeamProjects = false"
      @changed="handleProjectsChanged"
    />
  </div>
</template>

<script setup>
/**
 * Çalışma ayarlarının tek adresi.
 *
 * Board ayarları eskiden board görünümünün içindeki bir modaldaydı, durum
 * tanımlarının hiç arayüzü yoktu, proje bağlama başka bir modaldan yürüyordu.
 * Hepsi burada kategorilere ayrıldı; board görünümü yalnızca görünüm durumunu
 * (hangi board, nasıl gruplanmış) tutuyor.
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import StatusManager from '../components/workflow/StatusManager.vue'
import BoardManager from '../components/work/BoardManager.vue'
import TeamProjectsModal from '../components/work/TeamProjectsModal.vue'

import { useProjectContext } from '../composables/useProjectContext.js'
import { useTeamContext } from '../composables/useTeamContext.js'
import { invalidateStatusCache } from '../composables/useTaskStatuses.js'

const props = defineProps({ teamId: String })

const route = useRoute()
const router = useRouter()

const { adoptTeam, activeTeam, activeTeamId } = useTeamContext()

/**
 * URL'de takım yoksa merkezi context'teki aktif takıma düşülür.
 * Prop ile aynı adı taşımaması bilinçli: `<script setup>` içinde aynı isim
 * şablonda hangisinin kazandığını okuyan için belirsiz bırakırdı.
 */
const resolvedTeamId = computed(() => props.teamId || activeTeamId.value || null)

const {
  projects, projectId, organizationId, hasProjects,
  selectProject, loadProjects, ALL_PROJECTS,
} = useProjectContext(() => resolvedTeamId.value)

const validSections = ['statuses', 'boards', 'projects']
const activeSection = ref(
  validSections.includes(route.query.section) ? route.query.section : 'statuses'
)

const showTeamProjects = ref(false)

/**
 * Durumlar değiştiğinde board panelini yeniden kurmak için sürüm sayacı —
 * sütun eşleme arayüzü güncel durum listesiyle açılsın.
 */
const statusVersion = ref(0)
function bumpVersion() {
  invalidateStatusCache()
  statusVersion.value++
}

const navGroups = [
  {
    label: 'İş Akışı',
    items: [{ key: 'statuses', label: 'Görev Durumları', icon: '◉' }],
  },
  {
    label: 'Board',
    items: [{ key: 'boards', label: "Board'lar ve Sütunlar", icon: '▤' }],
  },
  {
    label: 'Kapsam',
    items: [{ key: 'projects', label: 'Takım Projeleri', icon: '📁' }],
  },
]

const externalLinks = computed(() => [
  { to: '/organizations', label: 'Organizasyon & Entegrasyonlar', icon: '🔌' },
  { to: '/settings', label: 'Profil ve Çalışma Alanı', icon: '⚙' },
])

async function handleProjectsChanged() {
  await loadProjects()
  bumpVersion()
}

// Seçili bölüm URL'de tutulur: paylaşılan link doğrudan o ayara açılır.
watch(activeSection, (section) => {
  router.replace({ query: { ...route.query, section } })
})

watch(resolvedTeamId, (id) => { if (id) adoptTeam(id) })

onMounted(() => {
  if (resolvedTeamId.value) adoptTeam(resolvedTeamId.value)
  loadProjects()
})
</script>
