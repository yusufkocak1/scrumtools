<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-gray-700">Takım Özeti</h3>
      <span class="text-xs text-gray-400">{{ teamId }}</span>
    </div>

    <div v-if="loading" class="flex-1 flex items-center justify-center py-6">
      <svg class="animate-spin w-6 h-6 text-purple-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
    </div>

    <template v-else-if="summary">
      <!-- Sayaç kartları -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <StatCard label="Toplam" :value="summary.totalTasks" color="bg-blue-50 text-blue-700" />
        <StatCard label="Açık" :value="summary.openTasks" color="bg-yellow-50 text-yellow-700" />
        <StatCard label="Süren" :value="summary.inProgressTasks" color="bg-purple-50 text-purple-700" />
        <StatCard label="Tamamlanan" :value="summary.doneTasks" color="bg-green-50 text-green-700" />
      </div>

      <!-- Overdue uyarısı -->
      <div v-if="summary.overdueTasks > 0"
           class="flex items-center gap-2 bg-red-50 text-red-700 rounded-lg px-3 py-2 text-sm">
        <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
        </svg>
        <span><strong>{{ summary.overdueTasks }}</strong> vadesi geçmiş görev var</span>
      </div>

      <!-- Sprint bilgisi -->
      <div class="flex items-center gap-4 text-sm text-gray-600 border-t border-gray-100 pt-3">
        <span>🏃 Toplam Sprint: <strong>{{ summary.totalSprints }}</strong></span>
        <span>✅ Aktif: <strong class="text-green-600">{{ summary.activeSprints }}</strong></span>
      </div>

      <!-- Tür dağılımı -->
      <div>
        <p class="text-xs text-gray-500 mb-2 font-medium uppercase tracking-wide">Tür Dağılımı</p>
        <div class="flex flex-wrap gap-2">
          <span v-for="item in summary.byType" :key="item.label"
                class="px-2 py-1 rounded-full text-xs font-medium"
                :class="typeColor(item.label)">
            {{ item.label }} ({{ item.count }})
          </span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTeamSummary } from '../../api/ReportApi.js'
import StatCard from './StatCard.vue'

const props = defineProps({ teamId: { type: String, required: true } })

const summary = ref(null)
const loading = ref(false)

onMounted(async () => {
  if (!props.teamId) return
  loading.value = true
  try { summary.value = await getTeamSummary(props.teamId) }
  catch (e) { console.error('Summary yüklenemedi', e) }
  finally { loading.value = false }
})

const typeColor = (t) => {
  const m = { task: 'bg-blue-100 text-blue-700', bug: 'bg-red-100 text-red-700', story: 'bg-green-100 text-green-700', epic: 'bg-purple-100 text-purple-700' }
  return m[t] || 'bg-gray-100 text-gray-600'
}
</script>

