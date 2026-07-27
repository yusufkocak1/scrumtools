<template>
  <!-- Görev hiç yayınlanmadıysa panel gizlenir -->
  <TaskPanel
    panel-key="deployments"
    title="Dağıtım Geçmişi"
    :count="deployments.length"
    :available="deployments.length > 0"
  >
    <template #icon>
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
      </svg>
    </template>

    <ul class="space-y-2">
      <li v-for="d in deployments" :key="d.id" class="flex items-start gap-2.5">
        <span class="flex-shrink-0 w-6 h-6 rounded-full bg-green-50 text-green-600 flex items-center justify-center text-[11px]">🚀</span>
        <div class="min-w-0 flex-1">
          <p class="text-sm font-medium text-gray-800 truncate">{{ d.releaseName }}</p>
          <p class="text-[11px] text-gray-400 truncate">
            {{ name(d.releasedBy) }} · {{ shortDate(d.releasedAt) }}
          </p>
        </div>
      </li>
    </ul>
  </TaskPanel>
</template>

<script setup>
import TaskPanel from '../TaskPanel.vue'
import { displayName, formatShortDate } from '../../../utils/taskFormat.js'
import { taskPanelProps, taskPanelEmits } from './panelProps.js'

defineProps(taskPanelProps)
defineEmits(taskPanelEmits)

const name = displayName
const shortDate = formatShortDate
</script>
