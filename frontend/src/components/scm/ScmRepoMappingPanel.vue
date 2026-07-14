<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-center justify-between mb-4">
      <div>
        <h3 class="font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          <svg class="w-5 h-5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
          </svg>
          Bağlı Repolar
        </h3>
        <p class="text-sm text-gray-500 mt-0.5">
          Eşlenen repolardaki commit'ler task anahtarıyla (örn. SCRM-12) otomatik bağlanır
        </p>
      </div>
      <button
        v-if="!showAddPanel"
        @click="openAddPanel"
        class="flex items-center gap-2 px-3 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Repo Ekle
      </button>
    </div>

    <!-- Eşlenmiş repo listesi -->
    <div v-if="loading" class="text-center py-8 text-gray-400 text-sm">Yükleniyor...</div>

    <div v-else-if="repos.length === 0 && !showAddPanel" class="text-center py-8">
      <p class="text-sm text-gray-500">
        Henüz repo eşlenmemiş. Önce organizasyon ayarlarındaki
        <span class="font-medium">Entegrasyonlar</span> sekmesinden bir SCM bağlantısı ekleyin,
        sonra buradan repo eşleyin.
      </p>
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="repo in repos"
        :key="repo.id"
        class="flex items-center gap-3 border border-gray-100 dark:border-gray-700 rounded-lg px-4 py-3"
      >
        <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="currentColor" viewBox="0 0 16 16">
          <path d="M2 2.5A2.5 2.5 0 014.5 0h8.75a.75.75 0 01.75.75v12.5a.75.75 0 01-.75.75h-2.5a.75.75 0 010-1.5h1.75v-2h-8a1 1 0 00-.714 1.7.75.75 0 01-1.072 1.05A2.495 2.495 0 012 11.5v-9z"/>
        </svg>
        <div class="flex-1 min-w-0">
          <a :href="repo.webUrl" target="_blank" rel="noopener"
            class="text-sm font-medium text-gray-900 dark:text-white hover:text-indigo-600 truncate block">
            {{ repo.fullName }} ↗
          </a>
          <p class="text-xs text-gray-400">
            {{ repo.provider }} · {{ repo.connectionName }} · varsayılan: {{ repo.defaultBranch }}
          </p>
        </div>

        <!-- Webhook durumu -->
        <span
          :class="[
            'text-xs px-2 py-0.5 rounded-full font-medium flex-shrink-0',
            repo.webhookStatus === 'ACTIVE' ? 'bg-green-100 text-green-700'
              : repo.webhookStatus === 'FAILED' ? 'bg-red-100 text-red-700'
              : 'bg-amber-100 text-amber-700'
          ]"
          :title="webhookTitle(repo.webhookStatus)"
        >
          webhook: {{ webhookLabel(repo.webhookStatus) }}
        </span>

        <button
          v-if="repo.webhookStatus !== 'ACTIVE'"
          @click="retryWebhook(repo)"
          class="text-xs text-indigo-600 hover:underline flex-shrink-0"
          title="Webhook'u yeniden kurmayı dene"
        >
          Yeniden kur
        </button>

        <button
          @click="removeRepo(repo)"
          class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors flex-shrink-0"
          title="Eşlemeyi kaldır"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Repo ekleme paneli -->
    <div v-if="showAddPanel" class="mt-4 border border-indigo-100 dark:border-indigo-900 bg-indigo-50/50 dark:bg-indigo-950/30 rounded-lg p-4 space-y-3">
      <div class="flex items-center justify-between">
        <h4 class="text-sm font-semibold text-gray-800 dark:text-gray-200">Repo Eşle</h4>
        <button @click="showAddPanel = false" class="text-gray-400 hover:text-gray-600">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <p v-if="connectionsError" class="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
        {{ connectionsError }}
      </p>

      <template v-else>
        <!-- Bağlantı seçimi -->
        <div>
          <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">SCM Bağlantısı</label>
          <select v-model="selectedConnectionId" @change="searchRepos" class="input-field">
            <option value="" disabled>Bağlantı seçin</option>
            <option v-for="c in connections" :key="c.id" :value="c.id">
              {{ c.name }} ({{ c.provider }})
            </option>
          </select>
        </div>

        <!-- Canlı repo arama -->
        <div v-if="selectedConnectionId">
          <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Repo Ara</label>
          <input
            v-model="repoSearch"
            @input="debouncedSearch"
            type="text"
            class="input-field"
            placeholder="repo adı yazın..."
          />

          <div v-if="searching" class="text-center py-4 text-xs text-gray-400">Aranıyor...</div>
          <div v-else-if="remoteRepos.length" class="mt-2 max-h-56 overflow-y-auto border border-gray-200 dark:border-gray-700 rounded-lg divide-y divide-gray-100 dark:divide-gray-700 bg-white dark:bg-gray-800">
            <div
              v-for="r in remoteRepos"
              :key="r.externalId"
              class="flex items-center gap-3 px-3 py-2.5"
            >
              <div class="flex-1 min-w-0">
                <p class="text-sm text-gray-800 dark:text-gray-200 truncate font-mono">{{ r.fullName }}</p>
                <p class="text-[11px] text-gray-400">{{ r.privateRepo ? '🔒 private' : '🌐 public' }} · {{ r.defaultBranch }}</p>
              </div>
              <span v-if="r.mapped" class="text-xs text-gray-400 flex-shrink-0">eşlenmiş</span>
              <button
                v-else
                @click="mapRemoteRepo(r)"
                :disabled="mappingId === r.externalId"
                class="px-2.5 py-1 text-xs font-medium bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50 flex-shrink-0"
              >
                {{ mappingId === r.externalId ? 'Eşleniyor...' : 'Eşle' }}
              </button>
            </div>
          </div>
          <p v-else-if="searched" class="text-xs text-gray-400 mt-2 text-center py-2">Sonuç bulunamadı.</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import {
  getProjectRepos, mapRepo, unmapRepo, rewebhookRepo,
  getConnections, getRemoteRepos,
} from '../../api/ScmApi.js'

const props = defineProps({
  projectId: { type: String, required: true },
  organizationId: { type: String, required: true },
})

const repos = ref([])
const loading = ref(false)

const showAddPanel = ref(false)
const connections = ref([])
const connectionsError = ref('')
const selectedConnectionId = ref('')
const repoSearch = ref('')
const remoteRepos = ref([])
const searching = ref(false)
const searched = ref(false)
const mappingId = ref(null)
let searchTimer = null

function webhookLabel(status) {
  return { ACTIVE: 'aktif', FAILED: 'kurulamadı', NONE: 'yok' }[status] || status
}

function webhookTitle(status) {
  if (status === 'ACTIVE') return 'Push event\'leri anlık işleniyor'
  if (status === 'FAILED') return 'Webhook kurulamadı — token\'ın hook yetkisi olmayabilir; commit\'ler periyodik taramayla gelir'
  return 'Webhook kurulmadı (public URL yapılandırılmamış olabilir); commit\'ler periyodik taramayla gelir'
}

async function load() {
  loading.value = true
  try {
    repos.value = await getProjectRepos(props.projectId)
  } catch (e) {
    console.error('Bağlı repolar yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function openAddPanel() {
  showAddPanel.value = true
  connectionsError.value = ''
  try {
    connections.value = await getConnections(props.organizationId, { _skipErrorToast: true })
    if (connections.value.length === 0) {
      connectionsError.value =
        'Henüz SCM bağlantısı yok. Organizasyon ayarlarındaki Entegrasyonlar sekmesinden bağlantı ekleyin.'
    } else if (connections.value.length === 1) {
      selectedConnectionId.value = connections.value[0].id
      searchRepos()
    }
  } catch (e) {
    connectionsError.value = e?.response?.status === 403
      ? 'Bağlantıları listelemek için organizasyon yöneticisi yetkisi gerekli.'
      : 'Bağlantılar yüklenemedi.'
  }
}

function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(searchRepos, 400)
}

async function searchRepos() {
  if (!selectedConnectionId.value) return
  searching.value = true
  try {
    remoteRepos.value = await getRemoteRepos(
      props.organizationId, selectedConnectionId.value, repoSearch.value)
    searched.value = true
  } catch (e) {
    console.error('Repo araması başarısız:', e)
    remoteRepos.value = []
  } finally {
    searching.value = false
  }
}

async function mapRemoteRepo(remoteRepo) {
  mappingId.value = remoteRepo.externalId
  try {
    const mapped = await mapRepo(props.projectId, selectedConnectionId.value, remoteRepo.externalId)
    repos.value.push(mapped)
    remoteRepo.mapped = true
    createToast(
      mapped.webhookStatus === 'ACTIVE'
        ? 'Repo eşlendi, webhook kuruldu.'
        : 'Repo eşlendi. Webhook kurulamadı — commit\'ler periyodik taramayla bağlanacak.',
      { type: 'success', position: 'top-center', timeout: 4000 }
    )
  } finally {
    mappingId.value = null
  }
}

async function removeRepo(repo) {
  const confirmed = window.confirm(
    `"${repo.fullName}" eşlemesi kaldırılsın mı?\n\nBu repoya ait commit/branch kayıtları da silinir.`)
  if (!confirmed) return
  await unmapRepo(props.projectId, repo.id)
  repos.value = repos.value.filter(r => r.id !== repo.id)
}

async function retryWebhook(repo) {
  const updated = await rewebhookRepo(props.projectId, repo.id)
  const idx = repos.value.findIndex(r => r.id === repo.id)
  if (idx !== -1) repos.value[idx] = updated
  createToast(
    updated.webhookStatus === 'ACTIVE' ? 'Webhook kuruldu.' : 'Webhook yine kurulamadı.',
    { type: updated.webhookStatus === 'ACTIVE' ? 'success' : 'danger', position: 'top-center', timeout: 4000 }
  )
}

load()
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
</style>
