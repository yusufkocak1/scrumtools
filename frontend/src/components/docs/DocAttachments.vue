<template>
  <div class="flex flex-col h-full">
    <div class="px-4 py-3 border-b border-slate-200 flex items-center gap-2.5">
      <div class="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round"
                d="M18.375 12.739l-7.693 7.693a4.5 4.5 0 01-6.364-6.364l10.94-10.94A3 3 0 1119.5 7.372L8.552 18.32m.009-.01l-.01.01m5.699-9.941l-7.81 7.81a1.5 1.5 0 002.112 2.13"/>
        </svg>
      </div>
      <h3 class="font-semibold text-slate-800 text-sm">Dosyalar</h3>
      <button @click="$emit('close')"
              class="ml-auto p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- Upload -->
    <div class="p-3 border-b border-slate-200">
      <label
          class="group block w-full border-2 border-dashed border-slate-200 rounded-xl p-4 text-center cursor-pointer hover:border-indigo-300 hover:bg-indigo-50/50 transition">
        <input type="file" class="hidden" @change="onFileSelected" :disabled="uploading"/>
        <div v-if="uploading" class="text-indigo-600">
          <div class="animate-spin inline-block w-5 h-5 border-2 border-indigo-500 border-t-transparent rounded-full"></div>
          <p class="text-sm mt-1.5">Yükleniyor...</p>
        </div>
        <div v-else>
          <svg class="w-6 h-6 mx-auto text-slate-400 group-hover:text-indigo-500 transition" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"/>
          </svg>
          <p class="text-sm text-slate-500 mt-1.5">Dosya yüklemek için tıklayın</p>
          <p class="text-xs text-slate-400 mt-0.5">Maks. 20MB</p>
        </div>
      </label>
    </div>

    <!-- File List -->
    <div class="flex-1 overflow-y-auto">
      <div v-if="loading" class="p-3 space-y-2">
        <div v-for="i in 3" :key="i" class="flex items-center gap-3 p-2 animate-pulse">
          <div class="w-9 h-9 bg-slate-100 rounded-lg shrink-0"></div>
          <div class="flex-1 space-y-1.5">
            <div class="h-3 bg-slate-100 rounded w-2/3"></div>
            <div class="h-2.5 bg-slate-100 rounded w-1/4"></div>
          </div>
        </div>
      </div>

      <div v-else-if="attachments.length === 0" class="px-4 py-10 text-center">
        <p class="text-sm text-slate-400">Henüz dosya yok</p>
      </div>

      <div v-else class="p-2 space-y-0.5">
        <div v-for="att in attachments" :key="att.id"
             class="px-2 py-2 rounded-xl flex items-center gap-3 group hover:bg-slate-50 transition">
          <div :class="['w-9 h-9 rounded-lg flex items-center justify-center shrink-0', fileVisual(att.mimeType).tile]">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.6" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" :d="fileVisual(att.mimeType).path"/>
            </svg>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm text-slate-700 truncate">{{ att.fileName }}</p>
            <p class="text-xs text-slate-400">{{ formatFileSize(att.fileSize) }}</p>
          </div>
          <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition">
            <button @click="insertIntoEditor(att)" title="Editöre ekle"
                    class="p-1.5 rounded-lg text-indigo-500 hover:text-indigo-700 hover:bg-indigo-50 transition">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
              </svg>
            </button>
            <a :href="att.downloadUrl" target="_blank" title="İndir"
               class="p-1.5 rounded-lg text-slate-500 hover:text-slate-700 hover:bg-slate-100 transition">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3"/>
              </svg>
            </a>
            <button @click="deleteTarget = att" title="Sil"
                    class="p-1.5 rounded-lg text-rose-500 hover:text-rose-700 hover:bg-rose-50 transition">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <ConfirmDialog
        v-if="deleteTarget"
        title="Dosyayı sil"
        :message="`&quot;${deleteTarget.fileName}&quot; dosyası kalıcı olarak silinecek.`"
        confirmText="Sil"
        variant="danger"
        @confirm="deleteAttachment"
        @cancel="deleteTarget = null"
    />
  </div>
</template>

<script setup>
import {ref, watch} from 'vue'
import DocApi from '../../api/DocApi.js'
import ConfirmDialog from '../common/ConfirmDialog.vue'

const props = defineProps({
  projectId: {type: String, required: true},
  spaceId: {type: String, required: true},
  pageId: {type: String, default: null}
})

const emit = defineEmits(['inserted', 'close'])

const attachments = ref([])
const loading = ref(false)
const uploading = ref(false)
const deleteTarget = ref(null)

watch(() => props.pageId, loadAttachments, {immediate: true})

async function loadAttachments() {
  if (!props.pageId) return
  loading.value = true
  try {
    const res = await DocApi.getAttachments(props.projectId, props.spaceId, props.pageId)
    attachments.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function onFileSelected(event) {
  const file = event.target.files[0]
  if (!file || !props.pageId) return

  uploading.value = true
  try {
    const res = await DocApi.uploadAttachment(props.projectId, props.spaceId, props.pageId, file)
    attachments.value.unshift(res.data)
  } catch (e) {
    console.error(e)
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

function insertIntoEditor(att) {
  emit('inserted', att.downloadUrl)
}

async function deleteAttachment() {
  const att = deleteTarget.value
  deleteTarget.value = null
  if (!att) return
  try {
    await DocApi.deleteAttachment(props.projectId, att.id)
    attachments.value = attachments.value.filter(a => a.id !== att.id)
  } catch (e) {
    console.error(e)
  }
}

// MIME türüne göre ikon rengi ve path'i
const FILE_VISUALS = {
  image: {
    tile: 'bg-sky-50 text-sky-500',
    path: 'M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 001.5-1.5V6a1.5 1.5 0 00-1.5-1.5H3.75A1.5 1.5 0 002.25 6v12a1.5 1.5 0 001.5 1.5zm10.5-11.25h.008v.008h-.008V8.25z'
  },
  pdf: {
    tile: 'bg-rose-50 text-rose-500',
    path: 'M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z'
  },
  sheet: {
    tile: 'bg-emerald-50 text-emerald-500',
    path: 'M3.75 6A2.25 2.25 0 016 3.75h12A2.25 2.25 0 0120.25 6v12A2.25 2.25 0 0118 20.25H6A2.25 2.25 0 013.75 18V6zM3.75 9.75h16.5M3.75 14.25h16.5M9.75 3.75v16.5M14.25 3.75v16.5'
  },
  doc: {
    tile: 'bg-blue-50 text-blue-500',
    path: 'M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z'
  },
  archive: {
    tile: 'bg-amber-50 text-amber-500',
    path: 'M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5m8.25 3v6.75m0 0l-3-3m3 3l3-3M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z'
  },
  default: {
    tile: 'bg-slate-100 text-slate-500',
    path: 'M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z'
  }
}

function fileVisual(mimeType) {
  if (!mimeType) return FILE_VISUALS.default
  if (mimeType.startsWith('image/')) return FILE_VISUALS.image
  if (mimeType.includes('pdf')) return FILE_VISUALS.pdf
  if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return FILE_VISUALS.sheet
  if (mimeType.includes('document') || mimeType.includes('word')) return FILE_VISUALS.doc
  if (mimeType.includes('zip') || mimeType.includes('archive')) return FILE_VISUALS.archive
  return FILE_VISUALS.default
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>
