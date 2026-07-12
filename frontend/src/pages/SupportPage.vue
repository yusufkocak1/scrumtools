<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-4xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Destek Taleplerim</h1>
        <button
          @click="openCreateModal"
          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors"
        >
          + Yeni Talep
        </button>
      </div>

      <!-- Liste -->
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow overflow-hidden">
        <div v-if="loading" class="text-center py-12 text-gray-500">Yükleniyor...</div>

        <div v-else-if="tickets.length === 0" class="text-center py-12 text-gray-500">
          Henüz destek talebiniz yok. Sorun ya da önerileriniz için yeni talep oluşturabilirsiniz.
        </div>

        <ul v-else class="divide-y divide-gray-100 dark:divide-gray-700">
          <li
            v-for="ticket in tickets"
            :key="ticket.id"
            @click="$router.push(`/support/${ticket.id}`)"
            class="p-4 hover:bg-gray-50 dark:hover:bg-gray-700/30 cursor-pointer"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="font-medium text-gray-900 dark:text-white truncate">{{ ticket.subject }}</p>
                <div class="flex flex-wrap items-center gap-2 mt-1">
                  <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', CATEGORY_BADGE_CLASSES[ticket.category]]">
                    {{ CATEGORY_LABELS[ticket.category] }}
                  </span>
                  <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', STATUS_BADGE_CLASSES[ticket.status]]">
                    {{ STATUS_LABELS[ticket.status] }}
                  </span>
                  <span
                    v-if="ticket.errorTrackingCode"
                    class="text-xs px-2 py-0.5 rounded-full font-mono bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300"
                  >
                    {{ ticket.errorTrackingCode }}
                  </span>
                </div>
              </div>
              <span class="text-xs text-gray-400 whitespace-nowrap">{{ formatDate(ticket.updatedAt) }}</span>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <!-- Yeni Talep Modalı -->
    <div
      v-if="showCreateModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      @click.self="showCreateModal = false"
    >
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div class="p-4 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
          <h3 class="font-semibold text-gray-900 dark:text-white">Yeni Destek Talebi</h3>
          <button @click="showCreateModal = false" class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">✕</button>
        </div>

        <form @submit.prevent="createTicket" class="p-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Kategori</label>
            <select
              v-model="form.category"
              class="w-full text-sm border border-gray-200 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            >
              <option value="PROBLEM">Sorun</option>
              <option value="SUGGESTION">Öneri</option>
              <option value="OTHER">Diğer</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Konu</label>
            <input
              v-model="form.subject"
              type="text"
              maxlength="200"
              required
              placeholder="Kısaca özetleyin"
              class="w-full text-sm border border-gray-200 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Açıklama</label>
            <textarea
              v-model="form.description"
              rows="5"
              required
              placeholder="Sorunu ya da önerinizi ayrıntılı anlatın. Sorunsa: ne yaptınız, ne bekliyordunuz, ne oldu?"
              class="w-full text-sm border border-gray-200 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            ></textarea>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Hata Takip No <span class="text-gray-400 font-normal">(varsa)</span>
            </label>
            <input
              v-model="form.errorTrackingCode"
              type="text"
              maxlength="16"
              placeholder="ERR-XXXXXXXX"
              class="w-full text-sm font-mono border border-gray-200 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>

          <!-- Dosya ekleri -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Ekler <span class="text-gray-400 font-normal">(ekran görüntüsü vb. — en fazla {{ MAX_FILES }} dosya, 20MB)</span>
            </label>
            <input
              ref="fileInput"
              type="file"
              multiple
              accept="image/*,.pdf,.txt,.log"
              @change="onFilesSelected"
              class="w-full text-sm text-gray-600 dark:text-gray-300 file:mr-3 file:px-3 file:py-1.5 file:rounded-lg file:border-0 file:bg-indigo-50 file:text-indigo-600 dark:file:bg-gray-700 dark:file:text-indigo-300 file:text-sm file:font-medium file:cursor-pointer"
            />
            <ul v-if="selectedFiles.length" class="mt-2 space-y-1">
              <li
                v-for="(file, i) in selectedFiles"
                :key="i"
                class="flex items-center justify-between text-xs text-gray-600 dark:text-gray-300 bg-gray-50 dark:bg-gray-700/50 rounded px-2 py-1"
              >
                <span class="truncate">{{ file.name }} ({{ formatSize(file.size) }})</span>
                <button type="button" @click="selectedFiles.splice(i, 1)" class="text-gray-400 hover:text-red-500 ml-2">✕</button>
              </li>
            </ul>
          </div>

          <div v-if="uploadStatus" class="text-sm text-gray-500">{{ uploadStatus }}</div>

          <div class="flex justify-end gap-2 pt-2">
            <button
              type="button"
              @click="showCreateModal = false"
              class="px-4 py-2 text-sm text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
            >
              Vazgeç
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium rounded-lg"
            >
              {{ submitting ? 'Gönderiliyor...' : 'Talep Oluştur' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createToast } from 'mosha-vue-toastify'
import SupportApi from '../api/SupportApi.js'
import {
  CATEGORY_LABELS, STATUS_LABELS,
  CATEGORY_BADGE_CLASSES, STATUS_BADGE_CLASSES,
} from '../utils/supportLabels.js'

const MAX_FILES = 5
const MAX_FILE_SIZE = 20 * 1024 * 1024

const route = useRoute()
const router = useRouter()

const tickets = ref([])
const loading = ref(false)

const showCreateModal = ref(false)
const submitting = ref(false)
const uploadStatus = ref('')
const selectedFiles = ref([])
const fileInput = ref(null)

const form = ref({
  subject: '',
  description: '',
  category: 'PROBLEM',
  errorTrackingCode: '',
})

async function loadTickets() {
  loading.value = true
  try {
    tickets.value = await SupportApi.getMyTickets()
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  form.value = {
    subject: '',
    description: '',
    category: 'PROBLEM',
    errorTrackingCode: route.query.trackingCode || '',
  }
  selectedFiles.value = []
  uploadStatus.value = ''
  showCreateModal.value = true
}

function onFilesSelected(e) {
  for (const file of e.target.files) {
    if (selectedFiles.value.length >= MAX_FILES) {
      createToast(`En fazla ${MAX_FILES} dosya ekleyebilirsiniz.`, { type: 'warning', position: 'top-center' })
      break
    }
    if (file.size > MAX_FILE_SIZE) {
      createToast(`"${file.name}" 20MB sınırını aşıyor.`, { type: 'warning', position: 'top-center' })
      continue
    }
    selectedFiles.value.push(file)
  }
  if (fileInput.value) fileInput.value.value = ''
}

async function createTicket() {
  submitting.value = true
  try {
    const ticket = await SupportApi.createTicket(form.value)

    // Ekleri sırayla yükle — biri başarısız olsa da talep oluşmuş olur
    for (let i = 0; i < selectedFiles.value.length; i++) {
      uploadStatus.value = `Dosya yükleniyor (${i + 1}/${selectedFiles.value.length})...`
      try {
        await SupportApi.uploadAttachment(ticket.id, selectedFiles.value[i])
      } catch {
        createToast(`"${selectedFiles.value[i].name}" yüklenemedi.`, { type: 'warning', position: 'top-center' })
      }
    }

    createToast('Destek talebiniz oluşturuldu.', { type: 'success', position: 'top-center' })
    showCreateModal.value = false
    router.push(`/support/${ticket.id}`)
  } finally {
    submitting.value = false
    uploadStatus.value = ''
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(() => {
  loadTickets()
  // Hata toast'ından "?trackingCode=ERR-..." ile gelindiyse modalı otomatik aç
  if (route.query.trackingCode) {
    openCreateModal()
  }
})
</script>
