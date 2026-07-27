<template>
  <TaskPanel panel-key="relations" title="Alt Görevler ve İlişkiler" :count="totalCount">
    <template #icon>
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
      </svg>
    </template>

    <template #actions>
      <button type="button" class="panel-action" @click="startAdding">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        {{ tab === 'subtasks' ? 'Alt Görev' : 'İlişki' }}
      </button>
    </template>

    <!-- Eskiden iki ayrı karttı; ikisi de "bu iş neyle bağlı" sorusunu yanıtlıyor -->
    <div class="tabs">
      <button
        type="button"
        class="tab"
        :class="{ 'tab--active': tab === 'subtasks' }"
        @click="switchTab('subtasks')"
      >
        Alt Görevler
        <span class="tab-count">{{ subtasks.length }}</span>
      </button>
      <button
        type="button"
        class="tab"
        :class="{ 'tab--active': tab === 'links' }"
        @click="switchTab('links')"
      >
        İlişkili İşler
        <span class="tab-count">{{ links.length }}</span>
      </button>
    </div>

    <SubtaskList
      v-show="tab === 'subtasks'"
      v-model:adding="addingSubtask"
      :teamId="teamId"
      :taskId="task.id"
      :subtasks="subtasks"
      @update="$emit('refresh')"
      @open="sub => $emit('open-task', sub.customId)"
    />

    <TaskLinkList
      v-show="tab === 'links'"
      v-model:adding="addingLink"
      :teamId="teamId"
      :taskId="task.id"
      :links="links"
      @update="$emit('refresh-links')"
      @open="customId => $emit('open-task', customId)"
    />
  </TaskPanel>
</template>

<script setup>
/**
 * Alt görevler ile ilişkili işleri tek panelde sekmeler hâlinde toplar.
 * Ekleme butonu aktif sekmeye göre ilgili listenin formunu açar.
 */
import { ref, computed } from 'vue'
import TaskPanel from '../TaskPanel.vue'
import SubtaskList from '../SubtaskList.vue'
import TaskLinkList from '../TaskLinkList.vue'
import { taskPanelProps, taskPanelEmits } from './panelProps.js'

const props = defineProps(taskPanelProps)
defineEmits(taskPanelEmits)

const tab = ref('subtasks')
const addingSubtask = ref(false)
const addingLink = ref(false)

const totalCount = computed(() => props.subtasks.length + props.links.length)

function switchTab(next) {
  tab.value = next
  addingSubtask.value = false
  addingLink.value = false
}

function startAdding() {
  if (tab.value === 'subtasks') addingSubtask.value = true
  else addingLink.value = true
}
</script>

<style scoped>
.tabs {
  @apply flex items-center gap-1 border-b border-gray-100 mb-3 -mt-1;
}
.tab {
  @apply flex items-center gap-1.5 px-2.5 py-1.5 -mb-px border-b-2 border-transparent text-xs font-semibold text-gray-400 hover:text-gray-600 transition-colors;
}
.tab--active {
  @apply border-blue-500 text-blue-600;
}
.tab-count {
  @apply inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full bg-gray-100 text-[10px] font-semibold text-gray-500;
}
.tab--active .tab-count {
  @apply bg-blue-50 text-blue-600;
}
.panel-action {
  @apply inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold text-gray-500 bg-gray-100 hover:bg-gray-200 hover:text-gray-700 transition-colors;
}
</style>
