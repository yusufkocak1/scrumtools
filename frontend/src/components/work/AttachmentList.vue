<template>
  <TaskPanel panel-key="attachments" title="Ekler" :count="attachments.length">
    <template #icon>
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/>
      </svg>
    </template>

    <template #actions>
      <label class="panel-action cursor-pointer">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Dosya
        <input type="file" class="hidden" multiple :disabled="uploading" @change="handleFileSelect" />
      </label>
    </template>

    <!-- Yükleme durumu -->
    <div v-if="uploading" class="mb-3">
      <p class="text-xs text-blue-600 mb-1">Yükleniyor… {{ uploadProgress }}%</p>
      <div class="w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
        <div class="bg-blue-600 h-full rounded-full transition-all duration-300" :style="{ width: uploadProgress + '%' }"></div>
      </div>
    </div>

    <div v-if="error" class="mb-3 flex items-start justify-between gap-2 p-2.5 bg-red-50 border border-red-100 rounded-lg text-xs text-red-700">
      <span>{{ error }}</span>
      <button type="button" class="text-red-400 hover:text-red-600" @click="error = ''">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <ul v-if="attachments.length" class="-mx-2">
      <li
        v-for="file in attachments"
        :key="file.id"
        class="flex items-center gap-2.5 px-2 py-1.5 rounded-lg hover:bg-gray-50 group"
      >
        <span class="flex-shrink-0 w-7 h-7 rounded-md flex items-center justify-center text-sm" :class="getFileIconClass(file.mimeType)">
          {{ getFileIcon(file.mimeType) }}
        </span>
        <div class="min-w-0 flex-1">
          <p class="text-sm text-gray-700 truncate">{{ file.fileName }}</p>
          <p class="text-[11px] text-gray-400 truncate">
            {{ formatFileSize(file.fileSize) }} · {{ displayName(file.uploadedBy) }} · {{ formatShortDate(file.createdAt) }}
          </p>
        </div>
        <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
          <a
            :href="getDownloadUrl(file)"
            target="_blank"
            class="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
            title="İndir"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
          </a>
          <button
            type="button"
            class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
            title="Sil"
            @click="deleteFile(file)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </button>
        </div>
      </li>
    </ul>

    <p v-else-if="!uploading" class="text-sm text-gray-400 py-3">
      Dosya eklenmemiş. Ekle butonuyla yükleyin (maks. 20MB).
    </p>
  </TaskPanel>
</template>

<script>
import TaskPanel from './TaskPanel.vue'
import { uploadAttachment, getAttachments, deleteAttachment, getAttachmentDownloadUrl } from '../../api/WorkApi.js'
import { displayName, formatShortDate } from '../../utils/taskFormat.js'
import { taskPanelProps, taskPanelEmits } from './panels/panelProps.js'

export default {
  name: 'AttachmentList',
  components: { TaskPanel },
  props: taskPanelProps,
  emits: taskPanelEmits,
  data() {
    return {
      attachments: [],
      uploading: false,
      uploadProgress: 0,
      error: ''
    }
  },
  watch: {
    'task.id'() { this.loadAttachments() },
  },
  async mounted() {
    await this.loadAttachments()
  },
  methods: {
    displayName,
    formatShortDate,

    async loadAttachments() {
      if (!this.teamId || !this.task?.id) return
      try {
        this.attachments = await getAttachments(this.teamId, this.task.id)
      } catch (e) {
        console.error('Attachments yüklenemedi:', e)
      }
    },

    async handleFileSelect(event) {
      const files = event.target.files
      if (!files?.length) return

      this.error = ''
      this.uploading = true

      for (const file of files) {
        if (file.size > 20 * 1024 * 1024) {
          this.error = `${file.name}: dosya boyutu 20MB'ı aşıyor`
          continue
        }
        try {
          this.uploadProgress = 0
          const result = await uploadAttachment(
            this.teamId, this.task.id, file,
            (progress) => { this.uploadProgress = progress }
          )
          this.attachments.unshift(result)
        } catch (e) {
          console.error('Upload hatası:', e)
          this.error = `${file.name} yüklenemedi: ${e.response?.data?.message || e.message}`
        }
      }

      this.uploading = false
      this.uploadProgress = 0
      event.target.value = '' // input'u temizle
    },

    async deleteFile(file) {
      if (!confirm(`"${file.fileName}" dosyası silinecek. Emin misiniz?`)) return

      try {
        await deleteAttachment(this.teamId, this.task.id, file.id)
        this.attachments = this.attachments.filter(a => a.id !== file.id)
      } catch (e) {
        console.error('Dosya silinemedi:', e)
        this.error = 'Dosya silinemedi'
      }
    },

    getDownloadUrl(file) {
      // Pre-signed URL varsa onu kullan (MinIO'dan direkt), yoksa backend proxy
      if (file.downloadUrl) return file.downloadUrl
      return getAttachmentDownloadUrl(this.teamId, this.task.id, file.id)
    },

    getFileIcon(mimeType) {
      if (!mimeType) return '📄'
      if (mimeType.startsWith('image/')) return '🖼️'
      if (mimeType.startsWith('video/')) return '🎬'
      if (mimeType.startsWith('audio/')) return '🎵'
      if (mimeType.includes('pdf')) return '📕'
      if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return '📊'
      if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return '📈'
      if (mimeType.includes('document') || mimeType.includes('word')) return '📝'
      if (mimeType.includes('zip') || mimeType.includes('archive') || mimeType.includes('tar')) return '📦'
      if (mimeType.includes('text') || mimeType.includes('json') || mimeType.includes('xml')) return '📃'
      return '📄'
    },

    getFileIconClass(mimeType) {
      if (!mimeType) return 'bg-gray-100'
      if (mimeType.startsWith('image/')) return 'bg-pink-50'
      if (mimeType.includes('pdf')) return 'bg-red-50'
      if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return 'bg-green-50'
      if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return 'bg-orange-50'
      if (mimeType.includes('document') || mimeType.includes('word')) return 'bg-blue-50'
      if (mimeType.includes('zip') || mimeType.includes('archive')) return 'bg-yellow-50'
      return 'bg-gray-100'
    },

    formatFileSize(bytes) {
      if (!bytes) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB']
      let i = 0
      let size = bytes
      while (size >= 1024 && i < units.length - 1) {
        size /= 1024
        i++
      }
      return `${size.toFixed(i > 0 ? 1 : 0)} ${units[i]}`
    },
  }
}
</script>

<style scoped>
.panel-action {
  @apply inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold text-gray-500 bg-gray-100 hover:bg-gray-200 hover:text-gray-700 transition-colors;
}
</style>
