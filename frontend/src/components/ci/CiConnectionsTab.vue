<template>
  <div>
    <!-- Başlık -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">CI/CD Entegrasyonu</h2>
        <p class="text-sm text-gray-500 mt-0.5">
          Jenkins sunucunuzu bağlayın, job'ları projelerle eşleyin; task ve release'lerden tek tıkla tetikleyin
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
      <h3 class="text-base font-semibold text-gray-700 mb-1">CI/CD entegrasyonu PRO paketiyle birlikte</h3>
      <p class="text-sm text-gray-500 mb-4">
        Task'lardan test ortamına deploy ve release pipeline'larını uygulama içinden tetiklemek için paketinizi yükseltin.
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
          <span v-html="JENKINS_ICON" class="w-8 h-8 text-indigo-400"></span>
        </div>
        <h3 class="text-base font-semibold text-gray-700 mb-1">Henüz CI/CD bağlantısı yok</h3>
        <p class="text-sm text-gray-500">
          Jenkins sunucunuzu kullanıcı adı ve API token ile bağlayın;
          ardından proje ayarlarından job'ları eşleyin.
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
            <span v-html="JENKINS_ICON" class="w-5 h-5"></span>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h3 class="font-semibold text-gray-900 truncate">{{ conn.name }}</h3>
              <span
                :class="[
                  'text-xs px-2 py-0.5 rounded-full font-medium',
                  conn.status === 'ACTIVE' ? 'bg-green-100 text-green-700'
                    : conn.status === 'INVALID' ? 'bg-red-100 text-red-700'
                    : 'bg-gray-100 text-gray-500'
                ]"
              >
                {{ statusLabel(conn.status) }}
              </span>
            </div>
            <p class="text-xs text-gray-400 mt-0.5 truncate">
              {{ conn.baseUrl }} · {{ conn.username }} · {{ conn.tokenHint }} · {{ conn.mappingCount }} job eşlendi
              <span v-if="conn.serverVersion"> · Jenkins {{ conn.serverVersion }}</span>
            </p>
          </div>

          <div class="flex items-center gap-1 flex-shrink-0">
            <button
              @click="test(conn)"
              :disabled="testingId === conn.id"
              class="px-3 py-1.5 text-xs font-medium text-gray-600 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors disabled:opacity-50"
              title="Bağlantıyı canlı doğrula"
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
          💡 Job'ları projelerle eşlemek için ilgili projenin sayfasındaki
          <span class="font-medium">"CI/CD Job'ları"</span> bölümünü kullanın.
        </p>
      </div>
    </template>

    <!-- Ekle / Düzenle modalı -->
    <CiConnectionModal
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
import CiConnectionModal from './CiConnectionModal.vue'
import { getConnections, deleteConnection, testConnection } from '../../api/CiApi.js'
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

const JENKINS_ICON = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1.2 4.3c.5-.2 1.1.1 1.2.6.1.4-.1.8-.5 1-.3.5-.2 1.1.2 1.5.1-.5.6-.9 1.1-.8.5 0 .9.5.9 1 0 .3-.2.6-.4.8.4.3.6.8.5 1.3l-.1.3c.5.1.9.5.9 1.1 0 .4-.2.7-.5.9.2.2.3.5.3.8 0 .6-.5 1.1-1.1 1.1-.2 0-.4-.1-.6-.2-.2.3-.5.5-.9.5-.3 0-.6-.1-.8-.4-.2.2-.5.4-.8.4-.5 0-1-.4-1-1 0-.1 0-.2.1-.3-.6-.1-1-.6-1-1.2 0-.3.1-.6.4-.8-.4-.2-.7-.7-.6-1.2.1-.5.5-.8 1-.9-.2-.3-.3-.7-.1-1.1.2-.5.7-.8 1.2-.7-.1-.4 0-.9.4-1.2.1-.1.2-.1.3-.2z"/></svg>'

function statusLabel(status) {
  return { ACTIVE: 'Aktif', INVALID: 'Geçersiz', DISABLED: 'Devre Dışı' }[status] || status
}

async function load() {
  if (!props.orgId) return
  loading.value = true
  try {
    const ent = await refresh(props.orgId)
    featureEnabled.value = ent?.features?.includes('CI_CD_INTEGRATION') ?? false
    if (featureEnabled.value) {
      connections.value = await getConnections(props.orgId)
    }
  } catch (e) {
    console.error('CI/CD bağlantıları yüklenemedi:', e)
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
    load()
  } finally {
    testingId.value = null
  }
}

async function remove(conn) {
  const confirmed = window.confirm(
    `"${conn.name}" bağlantısı silinsin mi?\n\n` +
    `Bu bağlantıyla eşlenmiş ${conn.mappingCount} job eşlemesi ve bağlı build tarihçesi de silinir.`
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
