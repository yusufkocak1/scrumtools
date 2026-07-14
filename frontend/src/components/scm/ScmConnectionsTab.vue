<template>
  <div>
    <!-- Başlık -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">Git Entegrasyonları</h2>
        <p class="text-sm text-gray-500 mt-0.5">
          GitHub / GitLab hesaplarını bağlayın, repoları projelerle eşleyin
        </p>
      </div>
      <button
        v-if="featureEnabled"
        @click="openCreateModal"
        class="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Bağlantı Ekle
      </button>
    </div>

    <!-- Paket uyarısı -->
    <div v-if="!featureEnabled" class="text-center py-16">
      <div class="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
        </svg>
      </div>
      <h3 class="text-base font-semibold text-gray-700 mb-1">Git entegrasyonu PRO paketiyle birlikte</h3>
      <p class="text-sm text-gray-500 mb-4">
        Task'larınıza commit ve branch'leri otomatik bağlamak için paketinizi yükseltin.
      </p>
      <button @click="openBilling" class="px-5 py-2.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium text-sm">
        Paketleri İncele
      </button>
    </div>

    <template v-else>
      <!-- Yükleniyor -->
      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>

      <!-- Boş durum -->
      <div v-else-if="connections.length === 0" class="text-center py-16">
        <div class="w-16 h-16 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
          </svg>
        </div>
        <h3 class="text-base font-semibold text-gray-700 mb-1">Henüz SCM bağlantısı yok</h3>
        <p class="text-sm text-gray-500">
          GitHub veya GitLab hesabınızı Personal Access Token ile bağlayın;
          ardından proje ayarlarından repoları eşleyin.
        </p>
      </div>

      <!-- Bağlantı listesi -->
      <div v-else class="space-y-3">
        <div
          v-for="conn in connections"
          :key="conn.id"
          class="flex items-center gap-4 bg-white border border-gray-200 rounded-xl p-4 hover:shadow-sm transition-shadow"
        >
          <div class="w-10 h-10 bg-gray-900 rounded-lg flex items-center justify-center flex-shrink-0 text-white">
            <span v-html="providerIcon(conn.provider)" class="w-5 h-5"></span>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h3 class="font-semibold text-gray-900 truncate">{{ conn.name }}</h3>
              <span
                :class="[
                  'text-xs px-2 py-0.5 rounded-full font-medium',
                  conn.status === 'ACTIVE' ? 'bg-green-100 text-green-700'
                    : conn.status === 'TOKEN_INVALID' ? 'bg-red-100 text-red-700'
                    : 'bg-gray-100 text-gray-500'
                ]"
              >
                {{ statusLabel(conn.status) }}
              </span>
            </div>
            <p class="text-xs text-gray-400 mt-0.5 truncate">
              {{ conn.provider }}<span v-if="conn.baseUrl"> · {{ conn.baseUrl }}</span>
              · {{ conn.tokenHint }} · {{ conn.repoCount }} repo eşlendi
            </p>
          </div>

          <div class="flex items-center gap-1 flex-shrink-0">
            <button
              @click="test(conn)"
              :disabled="testingId === conn.id"
              class="px-3 py-1.5 text-xs font-medium text-gray-600 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors disabled:opacity-50"
              title="Token'ı canlı doğrula"
            >
              {{ testingId === conn.id ? 'Test ediliyor...' : 'Test Et' }}
            </button>
            <button
              @click="openEditModal(conn)"
              class="p-2 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
              title="Düzenle"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
            </button>
            <button
              @click="remove(conn)"
              class="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
              title="Bağlantıyı sil"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
            </button>
          </div>
        </div>

        <p class="text-xs text-gray-400 pt-2">
          💡 Repoları projelerle eşlemek için ilgili projenin sayfasındaki
          <span class="font-medium">"Bağlı Repolar"</span> bölümünü kullanın.
        </p>
      </div>
    </template>

    <!-- Ekle / Düzenle modalı -->
    <ScmConnectionModal
      v-if="showModal"
      :org-id="orgId"
      :connection="editingConnection"
      @close="showModal = false"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import ScmConnectionModal from './ScmConnectionModal.vue'
import { getConnections, deleteConnection, testConnection } from '../../api/ScmApi.js'
import { useEntitlements } from '../../composables/useEntitlements.js'

const props = defineProps({
  orgId: { type: String, required: true },
})

const { refresh } = useEntitlements()
const featureEnabled = ref(true)

const connections = ref([])
const loading = ref(false)
const showModal = ref(false)
const editingConnection = ref(null)
const testingId = ref(null)

const GITHUB_ICON = '<svg viewBox="0 0 16 16" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0016 8c0-4.42-3.58-8-8-8z"/></svg>'
const GITLAB_ICON = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M23.6 9.6l-1.2-3.7L20 .9c-.1-.4-.7-.4-.8 0l-2.4 5H7.2L4.8.9c-.1-.4-.7-.4-.8 0L1.6 5.9.4 9.6c-.1.3 0 .7.3.9l11.3 8.2 11.3-8.2c.3-.2.4-.6.3-.9z"/></svg>'

function providerIcon(provider) {
  return provider === 'GITLAB' ? GITLAB_ICON : GITHUB_ICON
}

function statusLabel(status) {
  return { ACTIVE: 'Aktif', TOKEN_INVALID: 'Token Geçersiz', DISABLED: 'Devre Dışı' }[status] || status
}

async function load() {
  if (!props.orgId) return
  loading.value = true
  try {
    const ent = await refresh(props.orgId)
    featureEnabled.value = ent?.features?.includes('GIT_INTEGRATION') ?? false
    if (featureEnabled.value) {
      connections.value = await getConnections(props.orgId)
    }
  } catch (e) {
    console.error('SCM bağlantıları yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  editingConnection.value = null
  showModal.value = true
}

function openEditModal(conn) {
  editingConnection.value = conn
  showModal.value = true
}

function onSaved() {
  load()
  createToast('Bağlantı kaydedildi.', { type: 'success', position: 'top-center', timeout: 3000 })
}

async function test(conn) {
  testingId.value = conn.id
  try {
    const result = await testConnection(props.orgId, conn.id)
    createToast(result.message, {
      type: result.ok ? 'success' : 'danger',
      position: 'top-center',
      timeout: 4000,
    })
    if (!result.ok) load()
  } finally {
    testingId.value = null
  }
}

async function remove(conn) {
  const confirmed = window.confirm(
    `"${conn.name}" bağlantısı silinsin mi?\n\n` +
    `Bu bağlantıyla eşlenmiş ${conn.repoCount} repo eşlemesi ve bağlı commit kayıtları da silinir.`
  )
  if (!confirmed) return
  await deleteConnection(props.orgId, conn.id)
  connections.value = connections.value.filter(c => c.id !== conn.id)
  createToast('Bağlantı silindi.', { type: 'success', position: 'top-center', timeout: 3000 })
}

function openBilling() {
  window.dispatchEvent(new CustomEvent('scrumtools:open-billing-tab'))
}

watch(() => props.orgId, load)
onMounted(load)
</script>
