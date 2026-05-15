<template>
  <div class="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden transition-shadow duration-200 hover:shadow-md">
    <div class="h-1 w-full bg-gradient-to-r from-green-400 to-green-500"></div>
    <div class="p-5">
    <div class="flex items-center justify-between mb-3">
      <h3 class="text-base font-semibold text-gray-800 flex items-center gap-2">
        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
        Alt Görevler
        <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">{{ subtasks.length }}</span>
      </h3>
      <button
        @click="showAddForm = true"
        class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs font-medium text-green-600 bg-green-50 hover:bg-green-100 border border-green-200 transition-all"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Alt Görev Ekle
      </button>
    </div>

    <!-- Progress bar -->
    <div v-if="subtasks.length > 0" class="mb-3">
      <div class="flex justify-between text-xs text-gray-500 mb-1">
        <span>{{ completedCount }} / {{ subtasks.length }} tamamlandı</span>
        <span>{{ progressPercent }}%</span>
      </div>
      <div class="w-full bg-gray-200 rounded-full h-1.5">
        <div
          class="bg-green-500 h-1.5 rounded-full transition-all duration-300"
          :style="{ width: `${progressPercent}%` }"
        ></div>
      </div>
    </div>

    <!-- Subtask list -->
    <ul class="space-y-1.5">
      <li
        v-for="sub in subtasks"
        :key="sub.id"
        class="flex items-center gap-2 group"
      >
        <button
          @click="toggleSubtask(sub)"
          class="flex-shrink-0 w-4 h-4 rounded border-2 transition-colors"
          :class="isDone(sub.status)
            ? 'bg-green-500 border-green-500'
            : 'border-gray-300 hover:border-blue-400'"
        >
          <svg v-if="isDone(sub.status)" class="w-3 h-3 text-white mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
          </svg>
        </button>
        <span
          class="text-sm flex-1 cursor-pointer hover:text-blue-600 transition-colors"
          :class="{ 'line-through text-gray-400': isDone(sub.status) }"
          @click="$emit('open', sub)"
        >
          <span class="text-xs text-gray-400 font-mono mr-1">{{ sub.customId }}</span>
          {{ sub.title }}
        </span>
        <span class="text-xs">
          <StatusBadge :status="sub.status" />
        </span>
      </li>
    </ul>

    <p v-if="subtasks.length === 0" class="text-sm text-gray-400 text-center py-2">
      Henüz alt görev yok
    </p>

    <!-- Add form -->
    <div v-if="showAddForm" class="mt-3 flex gap-2">
      <input
        v-model="newTitle"
        @keydown.enter="addSubtask"
        @keydown.esc="showAddForm = false"
        placeholder="Alt görev başlığı..."
        class="flex-1 text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-blue-500"
        autofocus
      />
      <button
        @click="addSubtask"
        :disabled="!newTitle.trim() || loading"
        class="px-3 py-1.5 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50"
      >
        Ekle
      </button>
      <button
        @click="showAddForm = false"
        class="px-3 py-1.5 text-gray-500 hover:text-gray-700 text-sm"
      >
        İptal
      </button>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import StatusBadge from '@/components/workflow/StatusBadge.vue'
import { createSubtask, updateTaskStatus } from '@/api/WorkApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  taskId: { type: String, required: true },
  subtasks: { type: Array, default: () => [] },
})

const emit = defineEmits(['update', 'open'])

const showAddForm = ref(false)
const newTitle = ref('')
const loading = ref(false)

const completedCount = computed(() =>
  props.subtasks.filter(s => isDone(s.status)).length
)

const progressPercent = computed(() =>
  props.subtasks.length === 0
    ? 0
    : Math.round((completedCount.value / props.subtasks.length) * 100)
)

function isDone(status) {
  return ['Done', 'Closed', 'Fixed', 'Verified'].includes(status)
}

async function toggleSubtask(sub) {
  const newStatus = isDone(sub.status) ? 'To Do' : 'Done'
  try {
    await updateTaskStatus(props.teamId, sub.id, newStatus)
    emit('update')
  } catch (e) {
    console.error(e)
  }
}

async function addSubtask() {
  if (!newTitle.value.trim() || loading.value) return
  loading.value = true
  try {
    await createSubtask(props.teamId, props.taskId, { title: newTitle.value.trim() })
    newTitle.value = ''
    showAddForm.value = false
    emit('update')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

