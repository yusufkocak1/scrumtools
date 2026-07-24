<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
    <div class="flex items-center justify-between mb-4">
      <div>
        <h3 class="font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          <svg class="w-5 h-5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          CI/CD Job'ları
        </h3>
        <p class="text-sm text-gray-500 mt-0.5">
          Jenkins job'larını bu projeye eşleyin; task'lardan deploy, release'lerden pipeline tetiklenir
        </p>
      </div>
      <button
        @click="openCreate"
        class="flex items-center gap-2 px-3 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Job Eşle
      </button>
    </div>

    <div v-if="loading" class="text-center py-8 text-gray-400 text-sm">Yükleniyor...</div>

    <div v-else-if="mappings.length === 0" class="text-center py-8">
      <p class="text-sm text-gray-500">
        Henüz job eşlenmemiş. Önce organizasyon ayarlarındaki
        <span class="font-medium">Entegrasyonlar → CI/CD</span> sekmesinden bir Jenkins bağlantısı ekleyin,
        sonra buradan job eşleyin.
      </p>
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="m in mappings"
        :key="m.id"
        class="flex items-center gap-3 border border-gray-100 dark:border-gray-700 rounded-lg px-4 py-3"
        :class="{ 'opacity-60': !m.enabled }"
      >
        <span
          :class="[
            'text-[11px] px-2 py-0.5 rounded-full font-medium flex-shrink-0',
            m.jobType === 'RELEASE_PIPELINE' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'
          ]"
        >
          {{ m.jobType === 'RELEASE_PIPELINE' ? 'Release' : 'Deploy' }}
        </span>

        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium text-gray-900 dark:text-white truncate">{{ m.displayName }}</p>
          <p class="text-xs text-gray-400 truncate font-mono">
            {{ m.jobFullName }} · {{ m.connectionName }} · {{ envLabel(m.environment) }}
          </p>
        </div>

        <span v-if="m.jobType === 'RELEASE_PIPELINE' && m.autoTransitionOnSuccess"
          class="text-[11px] text-amber-600 flex-shrink-0" title="Build başarısında release otomatik RELEASED'a geçer">
          oto-geçiş
        </span>
        <span v-if="!m.enabled" class="text-[11px] text-gray-400 flex-shrink-0">pasif</span>
        <span v-if="m.connectionStatus !== 'ACTIVE'" class="text-[11px] text-red-500 flex-shrink-0"
          title="Bağlantı aktif değil">bağlantı: {{ m.connectionStatus }}</span>

        <button @click="openEdit(m)"
          class="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors flex-shrink-0"
          title="Düzenle">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
          </svg>
        </button>
        <button @click="remove(m)"
          class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors flex-shrink-0"
          title="Eşlemeyi kaldır">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <CiJobMappingModal
      v-if="showModal"
      :project-id="projectId"
      :organization-id="organizationId"
      :mapping="editingMapping"
      @close="showModal = false"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import CiJobMappingModal from './CiJobMappingModal.vue'
import { getJobMappings, deleteJobMapping } from '../../api/CiApi.js'

const props = defineProps({
  projectId: { type: String, required: true },
  organizationId: { type: String, required: true },
})

const mappings = ref([])
const loading = ref(false)
const showModal = ref(false)
const editingMapping = ref(null)

function envLabel(env) {
  return { TEST: 'Test', STAGING: 'Staging', PROD: 'Prod' }[env] || env
}

async function load() {
  loading.value = true
  try {
    mappings.value = await getJobMappings(props.projectId)
  } catch (e) {
    console.error('Job eşlemeleri yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingMapping.value = null
  showModal.value = true
}

function openEdit(m) {
  editingMapping.value = m
  showModal.value = true
}

function onSaved() {
  load()
  createToast('Eşleme kaydedildi.', { type: 'success', position: 'top-center', timeout: 3000 })
}

async function remove(m) {
  const confirmed = window.confirm(
    `"${m.displayName}" eşlemesi kaldırılsın mı?\n\nBu job'a ait build tarihçesi de silinir.`)
  if (!confirmed) return
  await deleteJobMapping(props.projectId, m.id)
  mappings.value = mappings.value.filter(x => x.id !== m.id)
  createToast('Eşleme kaldırıldı.', { type: 'success', position: 'top-center', timeout: 3000 })
}

load()
</script>
