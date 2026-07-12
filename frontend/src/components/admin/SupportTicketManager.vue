<template>
  <div class="space-y-4">
    <!-- Filtreler -->
    <div class="flex flex-wrap items-center gap-2">
      <h3 class="font-semibold text-gray-900 dark:text-white mr-auto">Destek Talepleri</h3>
      <select
        v-model="filters.status"
        @change="loadTickets(0)"
        class="text-xs border border-gray-200 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-200"
      >
        <option value="">Tüm Durumlar</option>
        <option v-for="(label, key) in STATUS_LABELS" :key="key" :value="key">{{ label }}</option>
      </select>
      <select
        v-model="filters.category"
        @change="loadTickets(0)"
        class="text-xs border border-gray-200 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-200"
      >
        <option value="">Tüm Kategoriler</option>
        <option v-for="(label, key) in CATEGORY_LABELS" :key="key" :value="key">{{ label }}</option>
      </select>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-500">Yükleniyor...</div>

    <div v-else-if="tickets.length === 0" class="text-center py-12 text-gray-500">
      Filtreye uyan destek talebi yok.
    </div>

    <!-- Liste -->
    <div v-else class="overflow-x-auto">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 dark:bg-gray-700/50">
          <tr>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Kullanıcı</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Konu</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Kategori</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Durum</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Güncelleme</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
          <tr
            v-for="ticket in tickets"
            :key="ticket.id"
            @click="openDetail(ticket.id)"
            class="hover:bg-gray-50 dark:hover:bg-gray-700/30 cursor-pointer"
          >
            <td class="px-4 py-3">
              <p class="font-medium text-gray-900 dark:text-white">{{ ticket.userName }}</p>
              <p class="text-xs text-gray-500">{{ ticket.userEmail }}</p>
              <p v-if="ticket.organizationName" class="text-xs text-gray-400">{{ ticket.organizationName }}</p>
            </td>
            <td class="px-4 py-3 text-gray-700 dark:text-gray-300 max-w-xs">
              <p class="truncate">{{ ticket.subject }}</p>
              <p v-if="ticket.errorTrackingCode" class="text-xs font-mono text-gray-400">{{ ticket.errorTrackingCode }}</p>
            </td>
            <td class="px-4 py-3">
              <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', CATEGORY_BADGE_CLASSES[ticket.category]]">
                {{ CATEGORY_LABELS[ticket.category] }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', STATUS_BADGE_CLASSES[ticket.status]]">
                {{ STATUS_LABELS[ticket.status] }}
              </span>
            </td>
            <td class="px-4 py-3 text-xs text-gray-500">{{ formatDate(ticket.updatedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Sayfalama -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 text-sm">
      <button
        :disabled="page === 0"
        @click="loadTickets(page - 1)"
        class="px-3 py-1 rounded border border-gray-200 dark:border-gray-600 disabled:opacity-40 text-gray-600 dark:text-gray-300"
      >
        ‹
      </button>
      <span class="text-gray-500">{{ page + 1 }} / {{ totalPages }}</span>
      <button
        :disabled="page >= totalPages - 1"
        @click="loadTickets(page + 1)"
        class="px-3 py-1 rounded border border-gray-200 dark:border-gray-600 disabled:opacity-40 text-gray-600 dark:text-gray-300"
      >
        ›
      </button>
    </div>

    <!-- Detay Modalı -->
    <div
      v-if="detail"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      @click.self="detail = null"
    >
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div class="p-4 border-b border-gray-100 dark:border-gray-700 flex items-start justify-between gap-3">
          <div>
            <h3 class="font-semibold text-gray-900 dark:text-white">{{ detail.ticket.subject }}</h3>
            <div class="flex flex-wrap items-center gap-2 mt-1">
              <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', CATEGORY_BADGE_CLASSES[detail.ticket.category]]">
                {{ CATEGORY_LABELS[detail.ticket.category] }}
              </span>
              <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', STATUS_BADGE_CLASSES[detail.ticket.status]]">
                {{ STATUS_LABELS[detail.ticket.status] }}
              </span>
              <span class="text-xs text-gray-500">
                {{ detail.ticket.userName }} ({{ detail.ticket.userEmail }})
                <template v-if="detail.ticket.organizationName"> · {{ detail.ticket.organizationName }}</template>
              </span>
            </div>
          </div>
          <button @click="detail = null" class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">✕</button>
        </div>

        <div class="p-4 space-y-4">
          <!-- Durum değiştir -->
          <div class="flex items-center gap-2">
            <label class="text-sm text-gray-600 dark:text-gray-300">Durum:</label>
            <select
              :value="detail.ticket.status"
              @change="changeStatus($event.target.value)"
              class="text-xs border border-gray-200 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-200"
            >
              <option v-for="(label, key) in STATUS_LABELS" :key="key" :value="key">{{ label }}</option>
            </select>
          </div>

          <!-- Açıklama -->
          <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
            <p class="text-xs text-gray-400 mb-1">Açıklama — {{ formatDate(detail.ticket.createdAt) }}</p>
            <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ detail.description }}</p>
          </div>

          <!-- Bağlı hata grubu -->
          <div
            v-if="detail.linkedErrorGroup"
            class="bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-800 rounded-lg p-3"
          >
            <p class="text-xs font-medium text-red-600 dark:text-red-400 mb-1">
              Bağlı Hata Kaydı ({{ detail.ticket.errorTrackingCode }})
            </p>
            <p class="text-sm font-mono text-gray-700 dark:text-gray-300 break-all">{{ detail.linkedErrorGroup.exceptionType }}</p>
            <p v-if="detail.linkedErrorGroup.location" class="text-xs font-mono text-gray-500 break-all">{{ detail.linkedErrorGroup.location }}</p>
            <p class="text-xs text-gray-600 dark:text-gray-400 mt-1">
              Bu hata {{ detail.linkedErrorGroup.occurrenceCount }} kez, {{ detail.linkedErrorGroup.affectedUserCount }} kullanıcıda görüldü.
              Durum: {{ ERROR_GROUP_STATUS_LABELS[detail.linkedErrorGroup.status] }}
            </p>
          </div>

          <!-- Ekler -->
          <div v-if="detail.attachments.length">
            <p class="text-xs text-gray-400 mb-1">Ekler</p>
            <ul class="space-y-1">
              <li v-for="att in detail.attachments" :key="att.id">
                <a
                  :href="att.downloadUrl"
                  target="_blank"
                  rel="noopener"
                  class="text-sm text-indigo-600 dark:text-indigo-400 hover:underline"
                >
                  {{ att.fileName }}
                </a>
              </li>
            </ul>
          </div>

          <!-- Mesajlar -->
          <div class="space-y-2">
            <p class="text-xs text-gray-400">Mesajlar</p>
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
                  {{ msg.isAdminReply ? (msg.authorName || 'Destek Ekibi') : (msg.authorName || msg.authorEmail) }}
                </span>
                <span class="text-xs text-gray-400">{{ formatDate(msg.createdAt) }}</span>
              </div>
              <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ msg.content }}</p>
            </div>
          </div>

          <!-- Yanıt kutusu -->
          <div v-if="detail.ticket.status === 'CLOSED'" class="text-sm text-gray-400">
            Bu talep kapatılmıştır.
          </div>
          <form v-else @submit.prevent="sendReply" class="space-y-2">
            <textarea
              v-model="replyText"
              rows="3"
              required
              placeholder="Yanıtınızı yazın..."
              class="w-full text-sm border border-gray-200 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            ></textarea>
            <div class="flex items-center justify-between">
              <label class="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-300 cursor-pointer">
                <input type="checkbox" v-model="setWaitingUser" class="rounded" />
                Durumu "Yanıt Bekleniyor" yap
              </label>
              <button
                type="submit"
                :disabled="replying || !replyText.trim()"
                class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium rounded-lg"
              >
                {{ replying ? 'Gönderiliyor...' : 'Yanıtla' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import AdminSupportApi from '../../api/AdminSupportApi.js'
import {
  CATEGORY_LABELS, STATUS_LABELS,
  CATEGORY_BADGE_CLASSES, STATUS_BADGE_CLASSES,
  ERROR_GROUP_STATUS_LABELS,
} from '../../utils/supportLabels.js'

const tickets = ref([])
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const filters = ref({ status: '', category: '' })

const detail = ref(null)
const replyText = ref('')
const setWaitingUser = ref(true)
const replying = ref(false)

async function loadTickets(targetPage = 0) {
  loading.value = true
  try {
    const result = await AdminSupportApi.getTickets({
      status: filters.value.status,
      category: filters.value.category,
      page: targetPage,
    })
    tickets.value = result.content
    page.value = result.number
    totalPages.value = result.totalPages
  } finally {
    loading.value = false
  }
}

async function openDetail(ticketId) {
  detail.value = await AdminSupportApi.getTicketDetail(ticketId)
  replyText.value = ''
  setWaitingUser.value = true
}

async function sendReply() {
  replying.value = true
  try {
    await AdminSupportApi.reply(detail.value.ticket.id, replyText.value.trim(), setWaitingUser.value)
    createToast('Yanıt gönderildi.', { type: 'success', position: 'top-center' })
    await openDetail(detail.value.ticket.id)
    await loadTickets(page.value)
  } finally {
    replying.value = false
  }
}

async function changeStatus(status) {
  await AdminSupportApi.updateTicketStatus(detail.value.ticket.id, status)
  createToast('Durum güncellendi.', { type: 'success', position: 'top-center' })
  await openDetail(detail.value.ticket.id)
  await loadTickets(page.value)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

onMounted(() => loadTickets(0))
</script>
