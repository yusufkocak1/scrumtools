<template>
  <div
    class="flex-shrink-0 w-72 flex flex-col rounded-xl transition-all duration-200"
    :class="[
      isDragOver
        ? 'bg-blue-50/80 ring-2 ring-blue-400/60 shadow-lg shadow-blue-100'
        : 'bg-gray-50/80 hover:bg-gray-100/60',
      wipExceeded ? 'ring-1 ring-red-300' : ''
    ]"
    @dragover.prevent="isDragOver = true"
    @dragleave="isDragOver = false"
    @drop.prevent="onDrop"
  >
    <!-- Sütun üst renk şeridi -->
    <div class="h-1 rounded-t-xl" :style="{ backgroundColor: column.color || '#6B7280' }"></div>

    <!-- Sütun Başlığı -->
    <div class="flex items-center justify-between px-3 pt-3 pb-2">
      <div class="flex items-center gap-2 min-w-0">
        <div class="w-2 h-2 rounded-full flex-shrink-0 ring-2 ring-offset-1" :style="{ backgroundColor: column.color || '#6B7280', '--tw-ring-color': (column.color || '#6B7280') + '33' }"></div>
        <span class="font-bold text-[13px] text-gray-800 truncate tracking-tight">{{ column.name }}</span>
        <span class="text-[11px] text-gray-500 bg-white border border-gray-200 rounded-full px-2 py-0.5 font-medium shadow-sm">{{ tasks.length }}</span>
      </div>
      <!-- WIP limiti -->
      <span
        v-if="column.wipLimit > 0"
        class="text-[11px] px-2 py-0.5 rounded-full font-semibold transition-all"
        :class="wipExceeded ? 'bg-red-100 text-red-700 animate-pulse ring-1 ring-red-300' : 'bg-gray-100 text-gray-600'"
        :title="`WIP Limit: ${column.wipLimit}`"
      >
        {{ tasks.length }}/{{ column.wipLimit }}
      </span>
    </div>

    <!-- Görev Kartları -->
    <div class="flex-1 overflow-y-auto px-2 pb-3 space-y-2.5 min-h-[100px]">
      <div
        v-for="task in tasks"
        :key="task.id"
        draggable="true"
        class="group bg-white rounded-xl p-3.5 shadow-sm border border-gray-100 cursor-grab active:cursor-grabbing
               hover:shadow-md hover:-translate-y-0.5 hover:border-gray-200
               active:opacity-70 active:rotate-[0.5deg] active:scale-[0.98]
               transition-all duration-150 select-none relative overflow-hidden"
        @dragstart="onDragStart(task)"
        @click="$emit('task-click', task)"
      >
        <!-- Sol kenar priority renk çubuğu -->
        <div class="absolute left-0 top-0 bottom-0 w-[3px] rounded-l-xl" :class="priorityBarClass(task.priority)"></div>

        <!-- customId + issueType icon + priority -->
        <div class="flex items-center justify-between mb-2 pl-1.5">
          <div class="flex items-center gap-1.5">
            <!-- Issue Type Icon -->
            <span class="flex items-center justify-center w-4 h-4 rounded" :class="issueTypeIconClass(task.issueType)">
              <svg v-if="task.issueType === 'bug'" class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M6.56 1.14a.75.75 0 01.177 1.045 3.989 3.989 0 00-.464.86c.185.17.382.329.59.473A3.993 3.993 0 0110 2.5c1.195 0 2.273.523 3.008 1.352.13-.07.258-.147.382-.229a3.99 3.99 0 00-.821-1.297.75.75 0 111.133-.984 5.49 5.49 0 011.046 1.724.75.75 0 01-.318.96 5.47 5.47 0 01-1.013.504A4 4 0 0114 6.5h.25a.75.75 0 010 1.5H14v.5c0 .058-.002.115-.005.172l1.83 1.83a.75.75 0 01-1.06 1.06l-1.453-1.452A4.002 4.002 0 0110 13.5a4.002 4.002 0 01-3.312-2.89L5.235 12.06a.75.75 0 01-1.06-1.06l1.83-1.83A4.025 4.025 0 016 8.5V8h-.25a.75.75 0 010-1.5H6A4 4 0 016.583 4.76a5.467 5.467 0 01-1.013-.504.75.75 0 01-.318-.96A5.488 5.488 0 016.38.572a.75.75 0 011.045-.177z" clip-rule="evenodd"/>
              </svg>
              <svg v-else-if="task.issueType === 'story'" class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                <path d="M5 3a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2V5a2 2 0 00-2-2H5zm0 2h10v7h-2l-1 2H8l-1-2H5V5z"/>
              </svg>
              <svg v-else-if="task.issueType === 'epic'" class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z" clip-rule="evenodd"/>
              </svg>
              <svg v-else class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V7.414A2 2 0 0015.414 6L12 2.586A2 2 0 0010.586 2H6zm5 6a1 1 0 10-2 0v2H7a1 1 0 100 2h2v2a1 1 0 102 0v-2h2a1 1 0 100-2h-2V8z" clip-rule="evenodd"/>
              </svg>
            </span>
            <span class="text-[11px] font-mono text-gray-400 font-medium">{{ task.customId }}</span>
          </div>
          <span
            class="text-[10px] px-2 py-0.5 rounded-full font-semibold tracking-wide uppercase"
            :class="priorityClass(task.priority)"
          >{{ task.priority }}</span>
        </div>

        <!-- Başlık -->
        <p class="text-[13px] font-semibold text-gray-900 line-clamp-2 mb-2 leading-snug pl-1.5 group-hover:text-blue-900 transition-colors">{{ task.title }}</p>

        <!-- Subtask progress bar -->
        <div v-if="task.subtaskCount > 0" class="mb-2 pl-1.5">
          <div class="flex items-center gap-2">
            <div class="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all duration-300"
                :class="subtaskProgress(task) === 100 ? 'bg-green-500' : 'bg-blue-500'"
                :style="{ width: subtaskProgress(task) + '%' }"
              ></div>
            </div>
            <span class="text-[10px] text-gray-400 font-medium">{{ task.subtaskDone || 0 }}/{{ task.subtaskCount }}</span>
          </div>
        </div>

        <!-- Etiketler -->
        <div v-if="task.labels?.length" class="flex flex-wrap gap-1 mb-2 pl-1.5">
          <span
            v-for="label in task.labels.slice(0,2)"
            :key="label"
            class="text-[10px] bg-indigo-50 text-indigo-700 rounded-full px-2 py-0.5 font-medium border border-indigo-100"
          >{{ label }}</span>
          <span v-if="task.labels.length > 2" class="text-[10px] text-gray-400 font-medium">+{{ task.labels.length - 2 }}</span>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-between pt-2 border-t border-gray-50 pl-1.5">
          <!-- Assignee -->
          <div v-if="task.assignee" class="flex items-center gap-1.5">
            <div class="w-6 h-6 rounded-full bg-gradient-to-br from-blue-400 to-indigo-600 flex items-center justify-center text-white text-[10px] font-bold shadow-sm ring-2 ring-white">
              {{ initials(task.assignee) }}
            </div>
            <span class="text-[11px] text-gray-600 truncate max-w-[70px] font-medium">{{ task.assignee.split('@')[0] }}</span>
          </div>
          <div v-else class="flex items-center gap-1.5">
            <div class="w-6 h-6 rounded-full bg-gray-100 border-2 border-dashed border-gray-300 flex items-center justify-center">
              <svg class="w-3 h-3 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
              </svg>
            </div>
            <span class="text-[10px] text-gray-400 italic">Atanmadı</span>
          </div>

          <!-- Metrics -->
          <div class="flex items-center gap-2">
            <span
              v-if="task.storyPoints"
              class="inline-flex items-center justify-center text-[10px] bg-blue-50 text-blue-700 rounded-md px-1.5 py-0.5 font-bold border border-blue-100"
            >{{ task.storyPoints }}<span class="text-blue-400 ml-0.5">SP</span></span>
            <!-- Due date -->
            <span
              v-if="task.dueDate"
              class="inline-flex items-center gap-0.5 text-[10px] px-1.5 py-0.5 rounded-md"
              :class="isOverdue(task.dueDate) ? 'text-red-700 bg-red-50 font-semibold border border-red-100' : 'text-gray-500 bg-gray-50'"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
              </svg>
              {{ formatDate(task.dueDate) }}
            </span>
          </div>
        </div>
      </div>

      <!-- Boş durum -->
      <div
        v-if="tasks.length === 0"
        class="h-24 rounded-xl border-2 border-dashed flex flex-col items-center justify-center gap-1.5 transition-all duration-200"
        :class="isDragOver ? 'border-blue-400 bg-blue-50/50 scale-[1.02]' : 'border-gray-200 bg-white/50'"
      >
        <svg class="w-5 h-5 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
        </svg>
        <span class="text-[11px] text-gray-400 font-medium">Görev sürükleyin</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  column: { type: Object, required: true },  // { name, color, wipLimit }
  tasks:  { type: Array,  default: () => [] },
  swimlaneKey: { type: String, default: null },
})

const emit = defineEmits(['task-click', 'task-drop'])

const isDragOver  = ref(false)
let draggingTask  = null

const wipExceeded = computed(() =>
  props.column.wipLimit > 0 && props.tasks.length > props.column.wipLimit
)

function onDragStart(task) {
  draggingTask = task
  // DataTransfer'e de yazıyoruz, global state yetersiz olursa kullanılır
  event.dataTransfer.setData('taskId',    task.id)
  event.dataTransfer.setData('fromStatus', task.status)
}

function onDrop(e) {
  isDragOver.value = false
  const taskId     = e.dataTransfer.getData('taskId')
  const fromStatus = e.dataTransfer.getData('fromStatus')
  if (taskId && fromStatus !== props.column.name) {
    emit('task-drop', { taskId, toStatus: props.column.name, fromStatus })
  }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function priorityClass(p) {
  return {
    Critical: 'bg-red-50 text-red-700 border border-red-200',
    High:     'bg-orange-50 text-orange-700 border border-orange-200',
    Medium:   'bg-amber-50 text-amber-700 border border-amber-200',
    Low:      'bg-emerald-50 text-emerald-700 border border-emerald-200',
  }[p] || 'bg-gray-50 text-gray-600 border border-gray-200'
}

function priorityBarClass(p) {
  return {
    Critical: 'bg-red-500',
    High:     'bg-orange-500',
    Medium:   'bg-amber-400',
    Low:      'bg-emerald-400',
  }[p] || 'bg-gray-300'
}

function issueTypeIconClass(type) {
  return {
    bug:   'bg-red-100 text-red-600',
    story: 'bg-green-100 text-green-600',
    epic:  'bg-purple-100 text-purple-600',
    task:  'bg-blue-100 text-blue-600',
  }[type] || 'bg-blue-100 text-blue-600'
}

function subtaskProgress(task) {
  if (!task.subtaskCount || task.subtaskCount === 0) return 0
  return Math.round(((task.subtaskDone || 0) / task.subtaskCount) * 100)
}

function initials(email) {
  if (!email) return '?'
  const name = email.split('@')[0]
  const parts = name.split(/[._-]/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return name.charAt(0).toUpperCase()
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const months = ['Oca','Şub','Mar','Nis','May','Haz','Tem','Ağu','Eyl','Eki','Kas','Ara']
  return `${d.getDate()} ${months[d.getMonth()]}`
}

function isOverdue(dateStr) {
  if (!dateStr) return false
  return new Date(dateStr) < new Date()
}
</script>

