<template>
  <div class="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden transition-shadow duration-200 hover:shadow-md">
    <div class="h-1 w-full bg-gradient-to-r from-amber-400 to-orange-500"></div>
    <div class="p-5 sm:p-6">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-base font-semibold text-gray-800 flex items-center gap-2">
        <svg class="w-5 h-5 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/>
        </svg>
        Ekler
        <span v-if="attachments.length" class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-700">{{ attachments.length }}</span>
      </h2>
      <label
        class="inline-flex items-center px-3 py-1.5 border border-gray-200 rounded-lg text-xs sm:text-sm font-medium text-gray-600 bg-white hover:bg-gray-50 hover:border-gray-300 cursor-pointer transition-all shadow-sm"
      >
        <svg class="w-4 h-4 mr-1.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/>
        </svg>
        Ekle
        <input type="file" class="hidden" multiple @change="handleFileSelect" :disabled="uploading" />
      </label>
    </div>

    <!-- Upload Progress -->
    <div v-if="uploading" class="mb-4">
      <div class="flex items-center space-x-3 text-sm text-blue-600">
        <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
        </svg>
        <span>Uploading... {{ uploadProgress }}%</span>
      </div>
      <div class="mt-1 w-full bg-gray-200 rounded-full h-1.5">
        <div class="bg-blue-600 h-1.5 rounded-full transition-all duration-300" :style="{ width: uploadProgress + '%' }"></div>
      </div>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-center justify-between">
      <span>{{ error }}</span>
      <button @click="error = ''" class="text-red-500 hover:text-red-700">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- File List -->
    <div v-if="attachments.length" class="space-y-2">
      <div
        v-for="file in attachments"
        :key="file.id"
        class="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors group"
      >
        <div class="flex items-center space-x-3 min-w-0 flex-1">
          <!-- File Icon -->
          <div class="w-8 h-8 rounded flex items-center justify-center flex-shrink-0"
               :class="getFileIconClass(file.mimeType)">
            <span class="text-sm">{{ getFileIcon(file.mimeType) }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium text-gray-900 truncate">{{ file.fileName }}</p>
            <p class="text-[11px] text-gray-500">
              {{ formatFileSize(file.fileSize) }} · {{ file.uploadedBy }} · {{ formatDate(file.createdAt) }}
            </p>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex items-center space-x-1 ml-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <a
            :href="getDownloadUrl(file)"
            target="_blank"
            class="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
            title="Download"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
          </a>
          <button
            @click="deleteFile(file)"
            class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
            title="Delete"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!uploading" class="text-center py-8">
      <div class="w-14 h-14 mx-auto mb-3 bg-amber-50 rounded-2xl flex items-center justify-center">
        <svg class="w-7 h-7 text-amber-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/>
        </svg>
      </div>
      <p class="text-sm font-medium text-gray-500">Dosya eklenmemiş</p>
      <p class="text-xs text-gray-400 mt-1">Dosyaları sürükleyin veya Ekle butonuna tıklayın (maks. 20MB)</p>
    </div>
    </div>
  </div>
</template>

<script>
import { uploadAttachment, getAttachments, deleteAttachment, getAttachmentDownloadUrl } from '../../api/WorkApi.js'

export default {
  name: 'AttachmentList',
  props: {
    teamId: { type: String, required: true },
    taskId: { type: String, required: true }
  },
  data() {
    return {
      attachments: [],
      uploading: false,
      uploadProgress: 0,
      error: ''
    }
  },
  async mounted() {
    await this.loadAttachments()
  },
  methods: {
    async loadAttachments() {
      try {
        this.attachments = await getAttachments(this.teamId, this.taskId)
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
            this.teamId, this.taskId, file,
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
        await deleteAttachment(this.teamId, this.taskId, file.id)
        this.attachments = this.attachments.filter(a => a.id !== file.id)
      } catch (e) {
        console.error('Dosya silinemedi:', e)
        this.error = 'Dosya silinemedi'
      }
    },

    getDownloadUrl(file) {
      // Pre-signed URL varsa onu kullan (MinIO'dan direkt), yoksa backend proxy
      if (file.downloadUrl) return file.downloadUrl
      return getAttachmentDownloadUrl(this.teamId, this.taskId, file.id)
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
      if (mimeType.startsWith('image/')) return 'bg-pink-100'
      if (mimeType.includes('pdf')) return 'bg-red-100'
      if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return 'bg-green-100'
      if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return 'bg-orange-100'
      if (mimeType.includes('document') || mimeType.includes('word')) return 'bg-blue-100'
      if (mimeType.includes('zip') || mimeType.includes('archive')) return 'bg-yellow-100'
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

    formatDate(dateString) {
      if (!dateString) return ''
      return new Date(dateString).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' })
    }
  }
}
</script>

