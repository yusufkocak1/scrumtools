<template>
  <div>
    <p v-if="builds.length === 0" class="text-sm text-gray-400 py-2">
      Henüz build tetiklenmemiş.
    </p>

    <ul v-else class="space-y-2">
      <li
        v-for="b in builds"
        :key="b.id"
        class="flex items-center gap-3 border border-gray-100 rounded-lg px-3 py-2.5 bg-white"
      >
        <!-- Durum rozeti -->
        <span
          :class="['inline-flex items-center gap-1.5 text-[11px] px-2 py-0.5 rounded-full font-medium flex-shrink-0', statusClass(b.status)]"
          :title="b.statusMessage || statusLabel(b.status)"
        >
          <svg v-if="b.status === 'RUNNING'" class="animate-spin w-3 h-3" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
          </svg>
          <span v-else class="w-1.5 h-1.5 rounded-full" :class="dotClass(b.status)"></span>
          {{ statusLabel(b.status) }}
        </span>

        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 text-sm text-gray-800">
            <span class="font-medium truncate">{{ b.jobDisplayName }}</span>
            <span v-if="b.buildNumber" class="font-mono text-xs text-gray-400">#{{ b.buildNumber }}</span>
          </div>
          <p class="text-[11px] text-gray-400 truncate">
            <span v-if="b.branch" class="font-mono">{{ b.branch }}</span>
            <span v-if="b.branch"> · </span>
            {{ b.triggeredBy }}
            <span v-if="b.durationMs"> · {{ formatDuration(b.durationMs) }}</span>
            <span> · {{ formatWhen(b.triggeredAt) }}</span>
          </p>
        </div>

        <!-- Manuel yenile (aktif build'lerde) -->
        <button
          v-if="!isTerminal(b.status)"
          @click="refresh(b)"
          :disabled="refreshingId === b.id"
          class="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors flex-shrink-0 disabled:opacity-50"
          title="Durumu yenile"
        >
          <svg class="w-4 h-4" :class="{ 'animate-spin': refreshingId === b.id }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
        </button>

        <!-- Jenkins linki -->
        <a
          v-if="b.buildUrl"
          :href="b.buildUrl"
          target="_blank"
          rel="noopener"
          class="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors flex-shrink-0"
          title="Jenkins'te aç"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
          </svg>
        </a>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { refreshBuild } from '../../api/CiApi.js'

const props = defineProps({
  builds: { type: Array, default: () => [] },
})
const emit = defineEmits(['updated'])

const refreshingId = ref(null)

const TERMINAL = ['SUCCESS', 'FAILURE', 'UNSTABLE', 'ABORTED', 'LOST']
function isTerminal(status) {
  return TERMINAL.includes(status)
}

function statusLabel(status) {
  return {
    QUEUED: 'Kuyrukta', RUNNING: 'Çalışıyor', SUCCESS: 'Başarılı',
    FAILURE: 'Başarısız', UNSTABLE: 'Kararsız', ABORTED: 'İptal', LOST: 'Kayıp',
  }[status] || status
}

function statusClass(status) {
  return {
    QUEUED: 'bg-gray-100 text-gray-600',
    RUNNING: 'bg-blue-100 text-blue-700',
    SUCCESS: 'bg-green-100 text-green-700',
    FAILURE: 'bg-red-100 text-red-700',
    UNSTABLE: 'bg-amber-100 text-amber-700',
    ABORTED: 'bg-gray-100 text-gray-500',
    LOST: 'bg-gray-200 text-gray-600',
  }[status] || 'bg-gray-100 text-gray-600'
}

function dotClass(status) {
  return {
    QUEUED: 'bg-gray-400',
    SUCCESS: 'bg-green-500',
    FAILURE: 'bg-red-500',
    UNSTABLE: 'bg-amber-500',
    ABORTED: 'bg-gray-400',
    LOST: 'bg-gray-500',
  }[status] || 'bg-gray-400'
}

function formatDuration(ms) {
  if (!ms || ms < 0) return ''
  const s = Math.round(ms / 1000)
  if (s < 60) return `${s}sn`
  const m = Math.floor(s / 60)
  const rem = s % 60
  return rem ? `${m}dk ${rem}sn` : `${m}dk`
}

function formatWhen(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('tr-TR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function refresh(build) {
  refreshingId.value = build.id
  try {
    const updated = await refreshBuild(build.id)
    emit('updated', updated)
  } catch (e) {
    console.error('Build yenilenemedi:', e)
  } finally {
    refreshingId.value = null
  }
}
</script>
