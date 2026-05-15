<template>
  <div class="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden transition-shadow duration-200 hover:shadow-md">
    <div class="h-1 w-full bg-gradient-to-r from-light-blue-400 to-blue-500"></div>
    <div class="p-5">
    <div class="flex items-center justify-between mb-3">
      <h3 class="text-base font-semibold text-gray-800 flex items-center gap-2">
        <svg class="w-5 h-5 text-light-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
        </svg>
        İzleyenler
        <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-light-blue-100 text-light-blue-700">{{ watchers.length }}</span>
      </h3>
      <button
        @click="toggleWatch"
        class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all"
        :class="isWatching
          ? 'text-blue-700 bg-blue-50 hover:bg-blue-100 border border-blue-200'
          : 'text-gray-500 bg-gray-50 hover:bg-gray-100 border border-gray-200'"
      >
        {{ isWatching ? '👁️ İzliyorum' : '+ İzle' }}
      </button>
    </div>

    <div class="flex flex-wrap gap-1.5">
      <div
        v-for="email in watchers"
        :key="email"
        class="flex items-center gap-1.5 bg-gray-50 border border-gray-100 rounded-full px-2.5 py-1 group hover:border-gray-200 transition-colors"
      >
        <div class="w-5 h-5 rounded-full bg-gradient-to-br from-light-blue-400 to-blue-600 flex items-center justify-center text-white text-[10px] font-bold shadow-sm">
          {{ email[0].toUpperCase() }}
        </div>
        <span class="text-xs text-gray-600 max-w-[120px] truncate font-medium">{{ email }}</span>
        <button
          v-if="email === currentUser || canManage"
          @click="removeWatcher(email)"
          class="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500 ml-0.5"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <p v-if="watchers.length === 0" class="text-xs text-gray-400 italic py-1">
      Henüz izleyen yok
    </p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { addWatcher, removeWatcher as apiRemoveWatcher } from '@/api/WorkApi.js'
import { useAuth } from '@/composables/useAuth.js'

const props = defineProps({
  teamId:    { type: String, required: true },
  taskId:    { type: String, required: true },
  watchers:  { type: Array, default: () => [] },
  canManage: { type: Boolean, default: false },
})

const emit = defineEmits(['update'])
const { user } = useAuth()
const currentUser = computed(() => user.value?.email || '')

const isWatching = computed(() => props.watchers.includes(currentUser.value))

async function toggleWatch() {
  try {
    if (isWatching.value) {
      await apiRemoveWatcher(props.teamId, props.taskId, currentUser.value)
    } else {
      await addWatcher(props.teamId, props.taskId, currentUser.value)
    }
    emit('update')
  } catch (e) {
    console.error(e)
  }
}

async function removeWatcher(email) {
  try {
    await apiRemoveWatcher(props.teamId, props.taskId, email)
    emit('update')
  } catch (e) {
    console.error(e)
  }
}
</script>

