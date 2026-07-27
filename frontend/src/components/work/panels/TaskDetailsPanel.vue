<template>
  <TaskPanel panel-key="details" title="Detaylar">
    <template #icon>
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
      </svg>
    </template>

    <!-- Eskiden durum / ekip / etiket / zaman ayrı kartlardaydı; hepsi görevin
         alan listesi olduğu için tek panelde toplandı -->
    <dl class="fields">
      <dt>Durum</dt>
      <dd>
        <select class="field-select" :class="statusClass" :value="task.status" @change="emitField('status', $event.target.value)">
          <option v-if="!knownStatuses.includes(task.status)" :value="task.status">{{ task.status }}</option>
          <option v-for="s in knownStatuses" :key="s" :value="s">{{ s }}</option>
        </select>
      </dd>

      <dt>Öncelik</dt>
      <dd>
        <select class="field-select" :class="priorityClass" :value="task.priority" @change="emitField('priority', $event.target.value)">
          <option value="Low">🟢 Düşük</option>
          <option value="Medium">🟡 Orta</option>
          <option value="High">🟠 Yüksek</option>
          <option value="Critical">🔴 Kritik</option>
        </select>
      </dd>

      <dt>Sorumlu</dt>
      <dd>
        <div v-if="task.assignee" class="person">
          <span class="person-avatar bg-blue-600">{{ initials(task.assignee) }}</span>
          <span class="person-name">{{ name(task.assignee) }}</span>
        </div>
        <span v-else class="field-empty">Atanmadı</span>
      </dd>

      <template v-if="task.issueType">
        <dt>Tür</dt>
        <dd class="flex items-center gap-1.5 text-sm text-gray-700 capitalize">
          <span>{{ typeIcon }}</span>{{ task.issueType }}
        </dd>
      </template>

      <template v-if="task.storyPoints">
        <dt>Puan</dt>
        <dd>
          <span class="inline-flex items-center justify-center min-w-[24px] h-6 px-1.5 rounded-md bg-amber-50 text-amber-700 text-xs font-bold border border-amber-100">
            {{ task.storyPoints }}
          </span>
        </dd>
      </template>

      <template v-if="releases.length || task.releaseId">
        <dt>Sürüm</dt>
        <dd>
          <select class="field-select" :value="task.releaseId || ''" @change="emitField('releaseId', $event.target.value)">
            <option value="">— Sürüm yok —</option>
            <option v-for="r in releases" :key="r.id" :value="r.id">
              {{ r.name }} ({{ releaseStatusLabel(r.status) }})
            </option>
          </select>
        </dd>
      </template>

      <template v-for="role in filledRoles" :key="role.key">
        <dt>{{ role.label }}</dt>
        <dd>
          <div class="person">
            <span class="person-avatar" :class="role.color">{{ initials(role.value) }}</span>
            <span class="person-name">{{ name(role.value) }}</span>
          </div>
        </dd>
      </template>

      <template v-if="task.labels?.length">
        <dt>Etiketler</dt>
        <dd class="flex flex-wrap gap-1">
          <span v-for="label in task.labels" :key="label" class="label-chip">{{ label }}</span>
        </dd>
      </template>
    </dl>

    <div class="mt-4 pt-3 border-t border-gray-100 space-y-1.5">
      <div class="flex items-center justify-between text-[11px]">
        <span class="text-gray-400">Oluşturuldu</span>
        <span class="text-gray-500">{{ shortDate(task.createdAt) }}</span>
      </div>
      <div v-if="task.updatedAt" class="flex items-center justify-between text-[11px]">
        <span class="text-gray-400">Güncellendi</span>
        <span class="text-gray-500">{{ relative(task.updatedAt) }}</span>
      </div>
    </div>
  </TaskPanel>
</template>

<script setup>
/**
 * Görevin alan listesi — Jira'daki "Details" paneli gibi tek yerde toplanmış
 * durum, öncelik, kişiler, etiketler ve zaman bilgisi.
 */
import { computed } from 'vue'
import TaskPanel from '../TaskPanel.vue'
import { getInitials, displayName, formatShortDate, formatRelativeTime } from '../../../utils/taskFormat.js'
import { taskPanelProps, taskPanelEmits } from './panelProps.js'

const props = defineProps(taskPanelProps)
const emit = defineEmits(taskPanelEmits)

const knownStatuses = ['To Do', 'In Progress', 'Done']

const typeIcons = { bug: '🐛', story: '📖', epic: '⚡', task: '✅', subtask: '📌' }

const typeIcon = computed(() => typeIcons[(props.task?.issueType || 'task').toLowerCase()] || '✅')

const statusClass = computed(() => {
  const status = (props.task?.status || '').toLowerCase()
  if (status === 'done') return 'border-green-200 bg-green-50 text-green-700'
  if (status === 'in progress') return 'border-blue-200 bg-blue-50 text-blue-700'
  if (status === 'cancelled') return 'border-red-200 bg-red-50 text-red-700'
  return 'border-gray-200 bg-white text-gray-700'
})

const priorityClass = computed(() => {
  const p = (props.task?.priority || '').toLowerCase()
  if (p === 'critical') return 'border-red-200 bg-red-50 text-red-700'
  if (p === 'high') return 'border-orange-200 bg-orange-50 text-orange-700'
  if (p === 'medium') return 'border-yellow-200 bg-yellow-50 text-yellow-700'
  return 'border-gray-200 bg-white text-gray-700'
})

const filledRoles = computed(() => [
  { key: 'developer', label: 'Geliştirici', value: props.task?.developer, color: 'bg-green-600' },
  { key: 'analyst', label: 'Analist', value: props.task?.analyst, color: 'bg-indigo-600' },
  { key: 'tester', label: 'Test', value: props.task?.tester, color: 'bg-purple-600' },
].filter(r => r.value))

function emitField(field, value) {
  emit('field', field, value)
}

function releaseStatusLabel(status) {
  return {
    OPEN: 'Açık', CODE_FREEZE: 'Paket Kapandı', REGRESSION: 'Regresyon',
    APPROVED: 'Onaylandı', RELEASED: 'Yayınlandı', CANCELLED: 'İptal',
  }[status] || status
}

const initials = getInitials
const name = displayName
const shortDate = formatShortDate
const relative = formatRelativeTime
</script>

<style scoped>
.fields {
  @apply grid items-center gap-x-3 gap-y-2.5;
  grid-template-columns: 84px minmax(0, 1fr);
}
.fields dt {
  @apply text-[11px] font-semibold text-gray-400 uppercase tracking-wide;
}
.fields dd {
  @apply min-w-0;
}

.field-select {
  @apply w-full px-2 py-1.5 rounded-lg border text-xs font-semibold cursor-pointer transition-colors focus:outline-none focus:ring-2 focus:ring-blue-100;
}
.field-empty {
  @apply text-sm text-gray-400;
}

.person {
  @apply flex items-center gap-2 min-w-0;
}
.person-avatar {
  @apply flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-white text-[10px] font-bold;
}
.person-name {
  @apply text-sm text-gray-700 truncate;
}

.label-chip {
  @apply inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-medium bg-gray-100 text-gray-600;
}
</style>
