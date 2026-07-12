<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 p-4 sm:p-6">
    <div class="max-w-4xl mx-auto space-y-6">
      <div v-if="loading" class="text-center py-12 text-gray-500">Yükleniyor...</div>

      <template v-else-if="detail">
        <!-- Başlık -->
        <div>
          <button
            @click="$router.push('/support')"
            class="text-sm text-indigo-600 dark:text-indigo-400 hover:underline mb-2"
          >
            ← Destek Taleplerim
          </button>
          <div class="flex flex-wrap items-center gap-2">
            <h1 class="text-xl font-bold text-gray-900 dark:text-white">{{ detail.ticket.subject }}</h1>
          </div>
          <div class="flex flex-wrap items-center gap-2 mt-2">
            <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', CATEGORY_BADGE_CLASSES[detail.ticket.category]]">
              {{ CATEGORY_LABELS[detail.ticket.category] }}
            </span>
            <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', STATUS_BADGE_CLASSES[detail.ticket.status]]">
              {{ STATUS_LABELS[detail.ticket.status] }}
            </span>
            <span
              v-if="detail.ticket.errorTrackingCode"
              class="text-xs px-2 py-0.5 rounded-full font-mono bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300"
            >
              Takip No: {{ detail.ticket.errorTrackingCode }}
            </span>
            <span class="text-xs text-gray-400">{{ formatDate(detail.ticket.createdAt) }}</span>
          </div>
        </div>

        <!-- Açıklama -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-4">
          <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ detail.description }}</p>
        </div>

        <!-- Ekler -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-4 space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="font-semibold text-gray-900 dark:text-white text-sm">Ekler ({{ detail.attachments.length }}/5)</h3>
            <label
              v-if="!isClosed && detail.attachments.length < 5"
              class="text-sm text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer"
            >
              + Dosya Ekle
              <input type="file" class="hidden" @change="uploadFile" accept="image/*,.pdf,.txt,.log" />
            </label>
          </div>

          <div v-if="uploading" class="text-sm text-gray-500">Yükleniyor...</div>

          <p v-if="detail.attachments.length === 0" class="text-sm text-gray-400">Dosya eklenmemiş.</p>
          <ul v-else class="space-y-1">
            <li
              v-for="att in detail.attachments"
              :key="att.id"
              class="flex items-center justify-between text-sm bg-gray-50 dark:bg-gray-700/50 rounded-lg px-3 py-2"
            >
              <a
                :href="att.downloadUrl || SupportApi.getAttachmentDownloadUrl(ticketId, att.id)"
                target="_blank"
                rel="noopener"
                class="text-indigo-600 dark:text-indigo-400 hover:underline truncate"
              >
                {{ att.fileName }}
              </a>
              <div class="flex items-center gap-3 ml-2 whitespace-nowrap">
                <span class="text-xs text-gray-400">{{ formatSize(att.fileSize) }}</span>
                <button
                  v-if="!isClosed"
                  @click="deleteFile(att)"
                  class="text-xs text-gray-400 hover:text-red-500"
                >
                  Sil
                </button>
              </div>
            </li>
          </ul>
        </div>

        <!-- Mesajlar -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-4 space-y-4">
          <h3 class="font-semibold text-gray-900 dark:text-white text-sm">Mesajlar</h3>

          <p v-if="detail.messages.length === 0" class="text-sm text-gray-400">Henüz mesaj yok.</p>

          <div
            v-for="msg in detail.messages"
            :key="msg.id"
            :class="['rounded-lg p-3', msg.isAdminReply
              ? 'bg-indigo-50 dark:bg-indigo-900/20 border border-indigo-100 dark:border-indigo-800'
              : 'bg-gray-50 dark:bg-gray-700/50']"
          >
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs font-medium" :class="msg.isAdminReply ? 'text-indigo-600 dark:text-indigo-400' : 'text-gray-600 dark:text-gray-300'">
                {{ msg.isAdminReply ? 'Destek Ekibi' : (msg.authorName || msg.authorEmail) }}
              </span>
              <span class="text-xs text-gray-400">{{ formatDate(msg.createdAt) }}</span>
            </div>
            <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ msg.content }}</p>
          </div>

          <!-- Yanıt kutusu -->
          <div v-if="isClosed" class="text-sm text-gray-400 border-t border-gray-100 dark:border-gray-700 pt-3">
            Bu talep kapatılmıştır.
          </div>
          <form v-else @submit.prevent="sendMessage" class="border-t border-gray-100 dark:border-gray-700 pt-3 space-y-2">
            <textarea
              v-model="newMessage"
              rows="3"
              required
              placeholder="Yanıtınızı yazın..."
              class="w-full text-sm border border-gray-200 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            ></textarea>
            <div class="flex justify-end">
              <button
                type="submit"
                :disabled="sending || !newMessage.trim()"
                class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium rounded-lg"
              >
                {{ sending ? 'Gönderiliyor...' : 'Gönder' }}
              </button>
            </div>
          </form>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import SupportApi from '../api/SupportApi.js'
import {
  CATEGORY_LABELS, STATUS_LABELS,
  CATEGORY_BADGE_CLASSES, STATUS_BADGE_CLASSES,
} from '../utils/supportLabels.js'

const props = defineProps({
  ticketId: { type: String, required: true },
})

const detail = ref(null)
const loading = ref(false)
const newMessage = ref('')
const sending = ref(false)
const uploading = ref(false)

const isClosed = computed(() => detail.value?.ticket?.status === 'CLOSED')

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await SupportApi.getTicketDetail(props.ticketId)
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  sending.value = true
  try {
    await SupportApi.addMessage(props.ticketId, newMessage.value.trim())
    newMessage.value = ''
    await loadDetail()
  } finally {
    sending.value = false
  }
}

async function uploadFile(e) {
  const file = e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (file.size > 20 * 1024 * 1024) {
    createToast('Dosya boyutu 20MB\'ı aşamaz.', { type: 'warning', position: 'top-center' })
    return
  }
  uploading.value = true
  try {
    await SupportApi.uploadAttachment(props.ticketId, file)
    await loadDetail()
  } finally {
    uploading.value = false
  }
}

async function deleteFile(att) {
  if (!confirm(`"${att.fileName}" silinsin mi?`)) return
  await SupportApi.deleteAttachment(props.ticketId, att.id)
  await loadDetail()
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

onMounted(loadDetail)
</script>
