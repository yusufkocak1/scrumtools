<template>
  <div class="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden transition-shadow duration-200 hover:shadow-md">
    <div class="h-1 w-full bg-gradient-to-r from-gray-400 to-gray-500"></div>
    <div class="p-5">
    <h3 class="text-base font-semibold text-gray-800 flex items-center gap-2 mb-3">
      <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      Tarihçe
    </h3>

    <div v-if="loading" class="flex justify-center py-4">
      <svg class="animate-spin w-5 h-5 text-blue-500" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
      </svg>
    </div>

    <ul v-else-if="history.length > 0" class="space-y-2">
      <li
        v-for="item in history"
        :key="item.id"
        class="flex gap-3 text-sm"
      >
        <!-- Icon -->
        <div class="flex-shrink-0 w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center">
          <span class="text-xs">{{ fieldIcon(item.field) }}</span>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-gray-700">
            <span class="font-medium">{{ item.changedBy }}</span>
            {{ fieldLabel(item.field) }} güncelledi
          </p>
          <p class="text-xs text-gray-500 flex items-center gap-1.5 flex-wrap mt-0.5">
            <span v-if="item.oldValue" class="bg-red-50 text-red-600 px-1.5 py-0.5 rounded line-through">
              {{ item.oldValue }}
            </span>
            <svg v-if="item.oldValue && item.newValue" class="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
            <span v-if="item.newValue" class="bg-green-50 text-green-600 px-1.5 py-0.5 rounded">
              {{ item.newValue }}
            </span>
            <span class="text-gray-400 ml-1">• {{ formatDate(item.changedAt) }}</span>
          </p>
        </div>
      </li>
    </ul>

    <p v-else class="text-sm text-gray-400 text-center py-2">
      Henüz değişiklik kaydı yok
    </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHistory } from '@/api/WorkApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  taskId: { type: String, required: true },
})

const history = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    history.value = await getHistory(props.teamId, props.taskId)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

const fieldLabels = {
  status:   'Durum',
  priority: 'Öncelik',
  assignee: 'Sorumlu',
  title:    'Başlık',
  description: 'Açıklama',
  sprint:   'Sprint',
}

const fieldIcons = {
  status:   '🔄',
  priority: '⚡',
  assignee: '👤',
  title:    '✏️',
  description: '📝',
  sprint:   '🏃',
}

function fieldLabel(field) {
  return fieldLabels[field] || field
}

function fieldIcon(field) {
  return fieldIcons[field] || '📋'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('tr-TR', { dateStyle: 'short', timeStyle: 'short' })
}
</script>

