<template>
  <TaskPanel panel-key="watchers" title="İzleyenler" :count="watchers.length">
    <template #icon>
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
      </svg>
    </template>

    <template #actions>
      <button
        type="button"
        class="panel-action"
        :class="isWatching ? 'is-watching' : ''"
        @click="toggleWatch"
      >
        {{ isWatching ? '👁️ İzliyorum' : '+ İzle' }}
      </button>
    </template>

    <div v-if="watchers.length" class="flex flex-wrap gap-1.5">
      <div
        v-for="email in watchers"
        :key="email"
        class="flex items-center gap-1.5 bg-gray-50 border border-gray-100 rounded-full pl-1 pr-2 py-0.5 group hover:border-gray-200 transition-colors"
        :title="email"
      >
        <span class="w-5 h-5 rounded-full bg-blue-600 flex items-center justify-center text-white text-[9px] font-bold">
          {{ initials(email) }}
        </span>
        <span class="text-[11px] text-gray-600 max-w-[120px] truncate">{{ name(email) }}</span>
        <button
          v-if="email === currentUser"
          type="button"
          class="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500 transition-opacity"
          title="Takibi bırak"
          @click="removeWatcher(email)"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <p v-else class="text-sm text-gray-400 py-1">
      Henüz izleyen yok — değişikliklerden haberdar olmak için “İzle”ye basın.
    </p>
  </TaskPanel>
</template>

<script setup>
import { computed } from 'vue'
import TaskPanel from './TaskPanel.vue'
import { addWatcher, removeWatcher as apiRemoveWatcher } from '@/api/WorkApi.js'
import { useAuth } from '@/composables/useAuth.js'
import { getInitials, displayName } from '@/utils/taskFormat.js'
import { taskPanelProps, taskPanelEmits } from './panels/panelProps.js'

const props = defineProps(taskPanelProps)
const emit = defineEmits(taskPanelEmits)

const { user } = useAuth()
const currentUser = computed(() => user.value?.email || '')
const watchers = computed(() => props.task?.watchers || [])
const isWatching = computed(() => watchers.value.includes(currentUser.value))

async function toggleWatch() {
  if (!props.teamId || !props.task?.id) return
  try {
    if (isWatching.value) {
      await apiRemoveWatcher(props.teamId, props.task.id, currentUser.value)
    } else {
      await addWatcher(props.teamId, props.task.id, currentUser.value)
    }
    emit('refresh')
  } catch (e) {
    console.error(e)
  }
}

async function removeWatcher(email) {
  try {
    await apiRemoveWatcher(props.teamId, props.task.id, email)
    emit('refresh')
  } catch (e) {
    console.error(e)
  }
}

const initials = getInitials
const name = displayName
</script>

<style scoped>
.panel-action {
  @apply inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold text-gray-500 bg-gray-100 hover:bg-gray-200 hover:text-gray-700 transition-colors;
}
.panel-action.is-watching {
  @apply text-blue-600 bg-blue-50 hover:bg-blue-100;
}
</style>
