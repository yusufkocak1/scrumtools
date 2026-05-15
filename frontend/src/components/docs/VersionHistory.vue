<template>
  <div class="flex flex-col h-full">
    <div class="p-4 border-b flex items-center justify-between">
      <h3 class="font-semibold text-gray-800">🕒 Versiyon Geçmişi</h3>
      <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">✕</button>
    </div>

    <div class="flex-1 overflow-y-auto">
      <div v-if="loading" class="p-4 text-gray-500 text-sm">Yükleniyor...</div>

      <div v-else-if="versions.length === 0" class="p-4 text-gray-500 text-sm">Henüz versiyon yok</div>

      <div v-else class="divide-y">
        <div v-for="v in versions" :key="v.id"
             @click="previewVersion(v)"
             :class="[
               'p-3 cursor-pointer hover:bg-gray-50 transition',
               selectedVersion?.id === v.id ? 'bg-indigo-50 border-l-2 border-indigo-500' : ''
             ]">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium text-gray-800">v{{ v.versionNumber }}</span>
            <span class="text-xs text-gray-400">{{ formatDate(v.createdAt) }}</span>
          </div>
          <p v-if="v.changeSummary" class="text-xs text-gray-500 mt-1 truncate">{{ v.changeSummary }}</p>
          <p class="text-xs text-gray-400 mt-0.5">{{ v.createdByName }}</p>
        </div>
      </div>
    </div>

    <!-- Preview & Restore -->
    <div v-if="selectedVersion" class="border-t p-3 space-y-2">
      <div class="text-xs text-gray-500">
        <strong>v{{ selectedVersion.versionNumber }}</strong> önizlemesi
      </div>
      <div class="max-h-40 overflow-y-auto text-xs bg-gray-50 rounded p-2 prose prose-sm"
           v-html="previewHtml"></div>
      <button @click="restore"
              class="w-full bg-amber-500 hover:bg-amber-600 text-white text-sm px-3 py-2 rounded-lg transition">
        ↩️ Bu versiyona geri dön
      </button>
    </div>
  </div>
</template>

<script setup>
import {ref, watch, computed} from 'vue'
import {marked} from 'marked'
import DOMPurify from 'dompurify'
import DocApi from '../../api/DocApi.js'

const props = defineProps({
  projectId: {type: String, required: true},
  spaceId: {type: String, required: true},
  pageId: {type: String, default: null}
})

const emit = defineEmits(['restore', 'close'])

const versions = ref([])
const loading = ref(false)
const selectedVersion = ref(null)

const previewHtml = computed(() => {
  if (!selectedVersion.value) return ''
  return DOMPurify.sanitize(marked(selectedVersion.value.content || ''))
})

watch(() => props.pageId, loadVersions, {immediate: true})

async function loadVersions() {
  if (!props.pageId) return
  loading.value = true
  try {
    const res = await DocApi.getVersions(props.projectId, props.spaceId, props.pageId)
    versions.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function previewVersion(v) {
  selectedVersion.value = v
}

async function restore() {
  if (!selectedVersion.value) return
  if (!confirm(`v${selectedVersion.value.versionNumber} versiyonuna geri dönmek istediğinize emin misiniz?`)) return

  try {
    await DocApi.restoreVersion(props.projectId, props.spaceId, props.pageId, selectedVersion.value.versionNumber)
    emit('restore')
    selectedVersion.value = null
    await loadVersions()
  } catch (e) {
    console.error(e)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('tr-TR') + ' ' + d.toLocaleTimeString('tr-TR', {hour: '2-digit', minute: '2-digit'})
}
</script>

