<template>
  <div class="flex flex-col h-full">
    <div class="p-4 border-b flex items-center justify-between">
      <h3 class="font-semibold text-gray-800">📎 Dosyalar</h3>
      <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">✕</button>
    </div>

    <!-- Upload -->
    <div class="p-3 border-b">
      <label
          class="block w-full border-2 border-dashed border-gray-300 rounded-lg p-4 text-center cursor-pointer hover:border-indigo-400 hover:bg-indigo-50 transition">
        <input type="file" class="hidden" @change="onFileSelected" :disabled="uploading"/>
        <div class="text-sm text-gray-500">
          <span v-if="uploading">⏳ Yükleniyor...</span>
          <span v-else>📁 Dosya yüklemek için tıklayın</span>
        </div>
        <p class="text-xs text-gray-400 mt-1">Maks. 20MB</p>
      </label>
    </div>

    <!-- File List -->
    <div class="flex-1 overflow-y-auto">
      <div v-if="loading" class="p-4 text-gray-500 text-sm">Yükleniyor...</div>

      <div v-else-if="attachments.length === 0" class="p-4 text-gray-500 text-sm text-center">
        Henüz dosya yok
      </div>

      <div v-else class="divide-y">
        <div v-for="att in attachments" :key="att.id" class="p-3 flex items-center gap-3 group">
          <div class="w-8 h-8 bg-gray-100 rounded flex items-center justify-center text-sm">
            {{ getFileIcon(att.mimeType) }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm text-gray-800 truncate">{{ att.fileName }}</p>
            <p class="text-xs text-gray-400">{{ formatFileSize(att.fileSize) }}</p>
          </div>
          <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition">
            <button @click="insertIntoEditor(att)" title="Editöre ekle"
                    class="text-indigo-500 hover:text-indigo-700 p-1 rounded hover:bg-indigo-50">
              ⤵️
            </button>
            <a :href="att.downloadUrl" target="_blank" title="İndir"
               class="text-gray-500 hover:text-gray-700 p-1 rounded hover:bg-gray-100">
              ⬇️
            </a>
            <button @click="deleteTarget = att" title="Sil"
                    class="text-red-500 hover:text-red-700 p-1 rounded hover:bg-red-50">
              🗑️
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

function getFileIcon(mimeType) {
  if (!mimeType) return '📄'
  if (mimeType.startsWith('image/')) return '🖼️'
  if (mimeType.includes('pdf')) return '📕'
  if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return '📊'
  if (mimeType.includes('document') || mimeType.includes('word')) return '📝'
  if (mimeType.includes('zip') || mimeType.includes('archive')) return '📦'
  return '📄'
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

