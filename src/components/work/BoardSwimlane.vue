<template>
  <div class="flex flex-col gap-4">
    <!-- Gruplama başlık satırı (sütun header'ları) -->
    <div class="sticky top-0 z-10 bg-white/90 backdrop-blur-sm pb-2 border-b border-gray-100">
      <div class="flex gap-3 px-4 min-w-max">
        <!-- Soldaki boşluk (swimlane label genişliği) -->
        <div class="flex-shrink-0 w-44"></div>
        <!-- Sütun başlıkları -->
        <div
          v-for="col in columns"
          :key="col.name"
          class="flex-shrink-0 w-72 flex items-center gap-2 px-3 py-2.5"
        >
          <div class="w-2 h-2 rounded-full ring-2 ring-offset-1" :style="{ backgroundColor: col.color || '#6B7280', '--tw-ring-color': (col.color || '#6B7280') + '33' }"></div>
          <span class="font-bold text-[13px] text-gray-800 tracking-tight">{{ col.name }}</span>
        </div>
      </div>
    </div>

    <!-- Swimlane satırları -->
    <div
      v-for="lane in swimlanes"
      :key="lane.key"
      class="border border-gray-200/80 rounded-xl bg-white overflow-hidden shadow-sm hover:shadow-md transition-shadow"
    >
      <!-- Swimlane başlığı (collapse toggle) -->
      <button
        class="w-full flex items-center justify-between px-4 py-3 bg-gradient-to-r from-gray-50 to-white hover:from-gray-100 hover:to-gray-50 transition-all text-left border-b border-gray-100"
        @click="toggleLane(lane.key)"
      >
        <div class="flex items-center gap-3">
          <svg
            class="w-4 h-4 text-gray-400 transition-transform duration-200"
            :class="{ '-rotate-90': collapsedLanes[lane.key] }"
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
          </svg>

          <!-- Assignee avatar + isim -->
          <div class="flex items-center gap-2.5">
            <div
              v-if="lane.key !== '__unassigned__'"
              class="w-7 h-7 rounded-full bg-gradient-to-br from-blue-400 to-indigo-600 flex items-center justify-center text-white text-[11px] font-bold shadow-sm ring-2 ring-white"
            >
              {{ initials(lane.label) }}
            </div>
            <div
              v-else
              class="w-7 h-7 rounded-full bg-gray-100 border-2 border-dashed border-gray-300 flex items-center justify-center"
            >
              <svg class="w-3.5 h-3.5 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
              </svg>
            </div>
            <span class="text-sm font-semibold text-gray-800">{{ lane.label }}</span>
            <span class="text-[11px] text-gray-500 bg-gray-100 rounded-full px-2 py-0.5 font-medium">{{ lane.taskCount }}</span>
          </div>
        </div>

        <!-- Mini task distribution bar -->
        <div class="flex items-center gap-2 mr-2">
          <div class="flex h-2 w-20 rounded-full overflow-hidden bg-gray-100">
            <div
              v-for="col in columns"
              :key="col.name"
              class="h-full transition-all duration-300"
              :style="{
                backgroundColor: col.color || '#6B7280',
                width: lane.taskCount > 0 ? (getTasksForLaneColumn(lane.key, col.name).length / lane.taskCount * 100) + '%' : '0%'
              }"
            ></div>
          </div>
        </div>
      </button>

      <!-- Swimlane içeriği (sütunlar) -->
      <Transition name="swimlane-content">
        <div v-show="!collapsedLanes[lane.key]" class="overflow-x-auto p-3">
          <div class="flex gap-3 px-2 min-w-max">
            <BoardColumn
              v-for="col in columns"
              :key="col.name"
              :column="col"
              :tasks="getTasksForLaneColumn(lane.key, col.name)"
              :swimlane-key="lane.key"
              @task-click="$emit('task-click', $event)"
              @task-drop="handleSwimlanceDrop($event, lane.key)"
            />
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, Transition } from 'vue'
import BoardColumn from './BoardColumn.vue'

const props = defineProps({
  columns:  { type: Array, required: true },
  tasks:    { type: Array, default: () => [] },
  groupBy:  { type: String, default: 'assignee' }, // assignee | priority
})

const emit = defineEmits(['task-click', 'task-drop'])

// ─── Collapsed state ──────────────────────────────────────────────────────────
const collapsedLanes = ref({})

function toggleLane(key) {
  collapsedLanes.value[key] = !collapsedLanes.value[key]
}

// ─── Swimlane hesaplama ───────────────────────────────────────────────────────
const swimlanes = computed(() => {
  const map = {}

  for (const task of props.tasks) {
    let key, label

    if (props.groupBy === 'assignee') {
      key = task.assignee || '__unassigned__'
      label = task.assignee ? task.assignee.split('@')[0] : 'Atanmamış'
    } else if (props.groupBy === 'priority') {
      key = task.priority || 'Medium'
      label = task.priority || 'Medium'
    } else {
      key = '__all__'
      label = 'Tümü'
    }

    if (!map[key]) {
      map[key] = { key, label, taskCount: 0 }
    }
    map[key].taskCount++
  }

  // Sıralama: Atanmamış en sona
  const lanes = Object.values(map)
  lanes.sort((a, b) => {
    if (a.key === '__unassigned__') return 1
    if (b.key === '__unassigned__') return -1
    return a.label.localeCompare(b.label)
  })

  return lanes
})

// ─── Lane + Column → görev eşlemesi ──────────────────────────────────────────
function getTasksForLaneColumn(laneKey, colName) {
  return props.tasks.filter(task => {
    const taskLaneKey = props.groupBy === 'assignee'
      ? (task.assignee || '__unassigned__')
      : (task.priority || 'Medium')

    return taskLaneKey === laneKey && task.status === colName
  })
}

// ─── Drop handler (swimlane bilgisiyle) ───────────────────────────────────────
function handleSwimlanceDrop(dropData, laneKey) {
  emit('task-drop', { ...dropData, swimlaneKey: laneKey })
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function initials(name) {
  if (!name) return '?'
  const parts = name.split(/[._\-\s]/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return name.charAt(0).toUpperCase()
}
</script>

<style scoped>
.swimlane-content-enter-active {
  transition: all 0.25s ease-out;
  max-height: 1000px;
}
.swimlane-content-leave-active {
  transition: all 0.2s ease-in;
}
.swimlane-content-enter-from,
.swimlane-content-leave-to {
  opacity: 0;
  max-height: 0;
  padding: 0;
  overflow: hidden;
}
</style>

