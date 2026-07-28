<template>
  <!-- Kart kabuğu yok: bu liste TaskRelationsPanel'in "Alt Görevler" sekmesinde yaşar -->
  <div>
    <!-- İlerleme -->
    <div v-if="subtasks.length" class="mb-3">
      <div class="flex items-center justify-between text-[11px] text-gray-500 mb-1.5">
        <span>{{ completedCount }} / {{ subtasks.length }} tamamlandı</span>
        <span class="font-semibold text-gray-600">{{ progressPercent }}%</span>
      </div>
      <div class="w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
        <div class="bg-green-500 h-full rounded-full transition-all duration-300" :style="{ width: `${progressPercent}%` }"></div>
      </div>
    </div>

    <ul v-if="subtasks.length" class="-mx-2">
      <li
        v-for="sub in subtasks"
        :key="sub.id"
        class="flex items-center gap-2.5 px-2 py-1.5 rounded-lg hover:bg-gray-50 group"
      >
        <button
          type="button"
          class="flex-shrink-0 w-4 h-4 rounded border-2 transition-colors"
          :class="isDone(sub.status) ? 'bg-green-500 border-green-500' : 'border-gray-300 hover:border-blue-400'"
          :title="isDone(sub.status) ? 'Yapılacak olarak işaretle' : 'Tamamlandı olarak işaretle'"
          @click="toggleSubtask(sub)"
        >
          <svg v-if="isDone(sub.status)" class="w-3 h-3 text-white mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
          </svg>
        </button>

        <button
          type="button"
          class="flex items-center gap-2 flex-1 min-w-0 text-left"
          @click="$emit('open', sub)"
        >
          <span class="font-mono text-[11px] text-gray-400 flex-shrink-0">{{ sub.customId }}</span>
          <span
            class="text-sm text-gray-700 truncate group-hover:text-blue-600 transition-colors"
            :class="{ 'line-through text-gray-400': isDone(sub.status) }"
          >{{ sub.title }}</span>
        </button>

        <StatusBadge :status="sub.status" class="flex-shrink-0" />
      </li>
    </ul>

    <p v-else-if="!adding" class="text-sm text-gray-400 py-3">
      Bu görev henüz alt görevlere bölünmemiş.
    </p>

    <!-- Ekleme satırı -->
    <div v-if="adding" class="mt-3 flex gap-2">
      <input
        ref="titleInput"
        v-model="newTitle"
        placeholder="Alt görev başlığı…"
        class="flex-1 text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
        @keydown.enter="addSubtask"
        @keydown.esc="close"
      />
      <button
        type="button"
        :disabled="!newTitle.trim() || loading"
        class="px-3 py-1.5 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-40 transition-colors"
        @click="addSubtask"
      >Ekle</button>
      <button type="button" class="px-2 py-1.5 text-gray-500 hover:text-gray-700 text-sm" @click="close">İptal</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import StatusBadge from '@/components/workflow/StatusBadge.vue'
import { createSubtask, updateTaskStatus } from '@/api/WorkApi.js'
import { useTaskStatuses } from '@/composables/useTaskStatuses.js'

const props = defineProps({
  teamId: { type: String, required: true },
  taskId: { type: String, required: true },
  subtasks: { type: Array, default: () => [] },
  /** Ekleme formu panel başlığındaki butondan açılır */
  adding: { type: Boolean, default: false },
})

const emit = defineEmits(['update', 'open', 'update:adding'])

const newTitle = ref('')
const loading = ref(false)
const titleInput = ref(null)

watch(() => props.adding, async open => {
  if (open) {
    await nextTick()
    titleInput.value?.focus()
  } else {
    newTitle.value = ''
  }
})

// "Bitti" tanımı ve işaretleme hedefleri iş akışından gelir; sabit ad listesi
// takım durumlarını yeniden adlandırdığında yanlış sonuç veriyordu.
const { isDone, statuses, initialStatus } = useTaskStatuses(() => props.teamId)

const completedCount = computed(() => props.subtasks.filter(s => isDone(s.status)).length)

const progressPercent = computed(() =>
  props.subtasks.length === 0 ? 0 : Math.round((completedCount.value / props.subtasks.length) * 100)
)

/** Onay kutusu işaretlendiğinde yazılacak durum — ilk "bitiş" durumu. */
const doneStatus = computed(() =>
  statuses.value.find(s => s.category === 'DONE' && !s.isCancellation)?.name ?? 'Done'
)

function close() {
  emit('update:adding', false)
}

async function toggleSubtask(sub) {
  const newStatus = isDone(sub.status) ? initialStatus.value : doneStatus.value
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
    close()
    emit('update')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>
