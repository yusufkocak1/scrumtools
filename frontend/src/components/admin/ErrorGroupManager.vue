<template>
  <div class="space-y-4">
    <!-- Filtre -->
    <div class="flex flex-wrap items-center gap-2">
      <h3 class="font-semibold text-gray-900 dark:text-white mr-auto">Hata Kayıtları</h3>
      <select
        v-model="statusFilter"
        @change="loadGroups(0)"
        class="text-xs border border-gray-200 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-200"
      >
        <option value="">Tüm Durumlar</option>
        <option v-for="(label, key) in ERROR_GROUP_STATUS_LABELS" :key="key" :value="key">{{ label }}</option>
      </select>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-500">Yükleniyor...</div>

    <div v-else-if="groups.length === 0" class="text-center py-12 text-gray-500">
      Kayıtlı hata yok. 🎉
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="group in groups"
        :key="group.id"
        class="border border-gray-100 dark:border-gray-700 rounded-lg overflow-hidden"
      >
        <!-- Grup satırı -->
        <div
          @click="toggleExpand(group)"
          class="p-3 flex items-start justify-between gap-3 hover:bg-gray-50 dark:hover:bg-gray-700/30 cursor-pointer"
        >
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', ERROR_GROUP_STATUS_BADGE_CLASSES[group.status]]">
                {{ ERROR_GROUP_STATUS_LABELS[group.status] }}
              </span>
              <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300">
                {{ group.source === 'BACKEND' ? 'Sunucu' : 'Tarayıcı' }}
              </span>
              <span class="text-xs text-gray-500 font-medium">
                {{ group.occurrenceCount }} kez / {{ group.affectedUserCount }} kullanıcı
              </span>
            </div>
            <p class="text-sm font-mono text-gray-900 dark:text-white mt-1 break-all">{{ group.exceptionType }}</p>
            <p v-if="group.location" class="text-xs font-mono text-gray-500 break-all">{{ group.location }}</p>
            <p v-if="group.sampleMessage" class="text-xs text-gray-500 mt-1 line-clamp-2">{{ group.sampleMessage }}</p>
            <p class="text-xs text-gray-400 mt-1">
              İlk: {{ formatDate(group.firstSeenAt) }} · Son: {{ formatDate(group.lastSeenAt) }}
              <template v-if="group.resolvedBy"> · Çözen: {{ group.resolvedBy }}</template>
            </p>
          </div>

          <!-- Aksiyonlar -->
          <div class="flex flex-col gap-1 whitespace-nowrap" @click.stop>
            <button
              v-if="group.status !== 'RESOLVED'"
              @click="changeStatus(group, 'RESOLVED')"
              class="text-xs px-2 py-1 rounded bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300 hover:opacity-80"
            >
              Çözüldü
            </button>
            <button
              v-if="group.status === 'RESOLVED' || group.status === 'IGNORED'"
              @click="changeStatus(group, 'REOPENED')"
              class="text-xs px-2 py-1 rounded bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300 hover:opacity-80"
            >
              Yeniden Aç
            </button>
            <button
              v-if="group.status !== 'IGNORED'"
              @click="changeStatus(group, 'IGNORED')"
              class="text-xs px-2 py-1 rounded bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300 hover:opacity-80"
            >
              Yoksay
            </button>
          </div>
        </div>

        <!-- Genişletilmiş: olay listesi -->
        <div v-if="expanded[group.id]" class="border-t border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-900/40 p-3 space-y-2">
          <div v-if="expanded[group.id].loading" class="text-sm text-gray-500">Yükleniyor...</div>
          <template v-else>
            <div
              v-for="event in expanded[group.id].events"
              :key="event.id"
              class="bg-white dark:bg-gray-800 rounded-lg p-2 text-xs"
            >
              <div class="flex flex-wrap items-center gap-2">
                <span class="font-mono font-medium text-indigo-600 dark:text-indigo-400">{{ event.trackingCode }}</span>
                <span class="text-gray-500">{{ event.userEmail || 'anonim' }}</span>
                <span v-if="event.endpoint" class="text-gray-400 font-mono truncate max-w-[16rem]">
                  {{ event.httpMethod ? event.httpMethod + ' ' : '' }}{{ event.endpoint }}
                </span>
                <span class="text-gray-400 ml-auto">{{ formatDate(event.occurredAt) }}</span>
              </div>
              <details v-if="event.stackTrace" class="mt-1">
                <summary class="cursor-pointer text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">Stack trace</summary>
                <pre class="mt-1 p-2 bg-gray-100 dark:bg-gray-900 rounded overflow-x-auto text-[10px] leading-tight text-gray-600 dark:text-gray-400">{{ event.stackTrace }}</pre>
              </details>
            </div>

            <button
              v-if="expanded[group.id].hasMore"
              @click="loadMoreEvents(group)"
              class="text-xs text-indigo-600 dark:text-indigo-400 hover:underline"
            >
              Daha fazla göster
            </button>
          </template>
        </div>
      </div>
    </div>

    <!-- Sayfalama -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 text-sm">
      <button
        :disabled="page === 0"
        @click="loadGroups(page - 1)"
        class="px-3 py-1 rounded border border-gray-200 dark:border-gray-600 disabled:opacity-40 text-gray-600 dark:text-gray-300"
      >
        ‹
      </button>
      <span class="text-gray-500">{{ page + 1 }} / {{ totalPages }}</span>
      <button
        :disabled="page >= totalPages - 1"
        @click="loadGroups(page + 1)"
        class="px-3 py-1 rounded border border-gray-200 dark:border-gray-600 disabled:opacity-40 text-gray-600 dark:text-gray-300"
      >
        ›
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import AdminSupportApi from '../../api/AdminSupportApi.js'
import {
  ERROR_GROUP_STATUS_LABELS, ERROR_GROUP_STATUS_BADGE_CLASSES,
} from '../../utils/supportLabels.js'

const groups = ref([])
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const statusFilter = ref('')

// groupId → { loading, events, page, hasMore }
const expanded = ref({})

async function loadGroups(targetPage = 0) {
  loading.value = true
  try {
    const result = await AdminSupportApi.getErrorGroups({
      status: statusFilter.value,
      page: targetPage,
    })
    groups.value = result.content
    page.value = result.number
    totalPages.value = result.totalPages
    expanded.value = {}
  } finally {
    loading.value = false
  }
}

async function toggleExpand(group) {
  if (expanded.value[group.id]) {
    delete expanded.value[group.id]
    return
  }
  expanded.value[group.id] = { loading: true, events: [], page: 0, hasMore: false }
  const detail = await AdminSupportApi.getErrorGroupDetail(group.id, 0)
  expanded.value[group.id] = {
    loading: false,
    events: detail.events.content,
    page: 0,
    hasMore: !detail.events.last,
  }
}

async function loadMoreEvents(group) {
  const state = expanded.value[group.id]
  const nextPage = state.page + 1
  const detail = await AdminSupportApi.getErrorGroupDetail(group.id, nextPage)
  state.events.push(...detail.events.content)
  state.page = nextPage
  state.hasMore = !detail.events.last
}

const STATUS_CONFIRM_MESSAGES = {
  RESOLVED: 'Bu hata grubu "Çözüldü" olarak işaretlensin mi? Hata tekrar oluşursa otomatik olarak "Yeniden Açıldı" durumuna geçer.',
  IGNORED: 'Bu hata grubu yoksayılsın mı? Tekrarlar sayılmaya devam eder ama durum değişmez.',
  REOPENED: 'Bu hata grubu yeniden açılsın mı?',
}

async function changeStatus(group, status) {
  if (!confirm(STATUS_CONFIRM_MESSAGES[status])) return
  const updated = await AdminSupportApi.updateErrorGroupStatus(group.id, status)
  const idx = groups.value.findIndex(g => g.id === group.id)
  if (idx !== -1) groups.value[idx] = updated
  createToast('Hata grubu güncellendi.', { type: 'success', position: 'top-center' })
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

onMounted(() => loadGroups(0))
</script>
