<template>
  <div class="flex flex-col h-full">
    <div class="px-4 py-3 border-b border-slate-200 flex items-center gap-2.5">
      <div class="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <h3 class="font-semibold text-slate-800 text-sm">Versiyon Geçmişi</h3>
      <button @click="$emit('close')"
              class="ml-auto p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <div class="flex-1 overflow-y-auto">
      <div v-if="loading" class="p-3 space-y-2">
        <div v-for="i in 4" :key="i" class="rounded-xl border border-slate-100 p-3 animate-pulse">
          <div class="h-3 bg-slate-100 rounded w-1/3"></div>
          <div class="h-2.5 bg-slate-100 rounded w-2/3 mt-2"></div>
        </div>
      </div>

      <div v-else-if="versions.length === 0" class="px-4 py-10 text-center">
        <p class="text-sm text-slate-400">Henüz versiyon yok</p>
      </div>

      <div v-else class="p-2 space-y-1">
        <div v-for="v in versions" :key="v.id"
             @click="previewVersion(v)"
             :class="[
               'px-3 py-2.5 rounded-xl cursor-pointer border transition',
               selectedVersion?.id === v.id
                 ? 'bg-indigo-50/70 border-indigo-200'
                 : 'border-transparent hover:bg-slate-50'
             ]">
          <div class="flex items-start gap-2.5">
            <span :class="[
                    'mt-0.5 w-9 shrink-0 text-center text-[11px] font-bold rounded-md px-1 py-0.5 transition',
                    selectedVersion?.id === v.id ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-500'
                  ]">
              v{{ v.versionNumber }}
            </span>
            <div class="min-w-0 flex-1">
              <p v-if="v.changeSummary" class="text-sm text-slate-700 truncate">{{ v.changeSummary }}</p>
              <p v-else class="text-sm text-slate-300 italic">Değişiklik özeti yok</p>
              <p class="text-[11px] text-slate-400 mt-0.5">{{ v.createdByName }} · {{ formatDate(v.createdAt) }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Preview & Restore -->
    <div v-if="selectedVersion" class="border-t border-slate-200 p-3 space-y-2 bg-slate-50/60">
      <div class="text-xs text-slate-500 font-medium">
        v{{ selectedVersion.versionNumber }} önizlemesi
      </div>
      <div class="max-h-40 overflow-y-auto text-xs bg-white border border-slate-200 rounded-lg p-2.5 prose prose-sm max-w-none"
           v-html="previewHtml"></div>
      <button @click="showRestoreConfirm = true"
              class="w-full inline-flex items-center justify-center gap-1.5 bg-amber-500 hover:bg-amber-400 text-white text-sm font-medium px-3 py-2 rounded-xl shadow-sm shadow-amber-200 transition">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 15L3 9m0 0l6-6M3 9h12a6 6 0 010 12h-3"/>
        </svg>
        Bu versiyona geri dön
      </button>
    </div>

    <ConfirmDialog
        v-if="showRestoreConfirm"
        title="Versiyona geri dön"
        :message="`Sayfa v${selectedVersion?.versionNumber} versiyonuna geri döndürülecek. Devam etmek istiyor musunuz?`"
        confirmText="Geri Dön"
        variant="warning"
        @confirm="restore"
        @cancel="showRestoreConfirm = false"
    />
  </div>
</template>

<script setup>
import {ref, watch, computed} from 'vue'
import {marked} from 'marked'
import DOMPurify from 'dompurify'
import DocApi from '../../api/DocApi.js'
import ConfirmDialog from '../common/ConfirmDialog.vue'

const props = defineProps({
  projectId: {type: String, required: true},
  spaceId: {type: String, required: true},
  pageId: {type: String, default: null}
})

const emit = defineEmits(['restore', 'close'])

const versions = ref([])
const loading = ref(false)
const selectedVersion = ref(null)
const showRestoreConfirm = ref(false)

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
  showRestoreConfirm.value = false
  if (!selectedVersion.value) return

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
