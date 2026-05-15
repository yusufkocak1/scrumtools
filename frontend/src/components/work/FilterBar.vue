<template>
  <div class="flex flex-wrap items-center gap-2.5 px-4 py-2.5 bg-white border-b border-gray-100">
    <!-- Hızlı filtreler -->
    <div class="flex flex-wrap gap-2">
      <!-- Status quick filter -->
      <div class="relative">
        <select
          v-model="quickStatus"
          class="appearance-none text-xs rounded-lg border border-gray-200 pl-7 pr-7 py-1.5 bg-gray-50 hover:bg-white hover:border-gray-300 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 focus:outline-none transition-all cursor-pointer font-medium text-gray-700"
          @change="applyQuickFilter('status', quickStatus)"
        >
          <option value="">Tüm Durumlar</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
        </select>
        <svg class="absolute left-2 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <svg class="absolute right-2 top-1/2 -translate-y-1/2 w-3 h-3 text-gray-400 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
        </svg>
      </div>

      <!-- Priority quick filter -->
      <div class="relative">
        <select
          v-model="quickPriority"
          class="appearance-none text-xs rounded-lg border border-gray-200 pl-7 pr-7 py-1.5 bg-gray-50 hover:bg-white hover:border-gray-300 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 focus:outline-none transition-all cursor-pointer font-medium text-gray-700"
          @change="applyQuickFilter('priority', quickPriority)"
        >
          <option value="">Tüm Öncelikler</option>
          <option v-for="p in priorities" :key="p" :value="p">{{ p }}</option>
        </select>
        <svg class="absolute left-2 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4h13M3 8h9m-9 4h6m4 0l4-4m0 0l4 4m-4-4v12"/>
        </svg>
        <svg class="absolute right-2 top-1/2 -translate-y-1/2 w-3 h-3 text-gray-400 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
        </svg>
      </div>

      <!-- Type quick filter -->
      <div class="relative">
        <select
          v-model="quickType"
          class="appearance-none text-xs rounded-lg border border-gray-200 pl-7 pr-7 py-1.5 bg-gray-50 hover:bg-white hover:border-gray-300 focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 focus:outline-none transition-all cursor-pointer font-medium text-gray-700"
          @change="applyQuickFilter('issueType', quickType)"
        >
          <option value="">Tüm Türler</option>
          <option v-for="t in issueTypes" :key="t" :value="t">{{ t }}</option>
        </select>
        <svg class="absolute left-2 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/>
        </svg>
        <svg class="absolute right-2 top-1/2 -translate-y-1/2 w-3 h-3 text-gray-400 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
        </svg>
      </div>
    </div>

    <!-- Ayırıcı -->
    <div v-if="activeFilters.length" class="w-px h-5 bg-gray-200"></div>

    <!-- Aktif filtre chip'leri -->
    <TransitionGroup name="chip" tag="div" class="flex flex-wrap gap-1.5">
      <FilterChip
        v-for="f in activeFilters"
        :key="f.field"
        :filter="f"
        @remove="$emit('remove-filter', f.field)"
      />
    </TransitionGroup>

    <!-- Gelişmiş filtre butonu -->
    <button
      class="inline-flex items-center gap-1.5 text-xs text-gray-600 hover:text-blue-700 px-3 py-1.5 rounded-lg border border-gray-200 hover:border-blue-300 hover:bg-blue-50/50 bg-white transition-all shadow-sm"
      @click="$emit('open-builder')"
    >
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z"/>
      </svg>
      Filtre
      <span v-if="activeFilters.length" class="ml-0.5 bg-blue-600 text-white rounded-full w-4 h-4 flex items-center justify-center text-[10px] font-bold">
        {{ activeFilters.length }}
      </span>
    </button>

    <!-- Temizle -->
    <button
      v-if="activeFilters.length"
      class="inline-flex items-center gap-1 text-xs text-red-500 hover:text-red-700 hover:bg-red-50 px-2 py-1 rounded-md transition-colors"
      @click="$emit('clear-filters')"
    >
      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
      </svg>
      Temizle
    </button>
  </div>
</template>

<script setup>
import { ref, watch, TransitionGroup } from 'vue'
import FilterChip from './FilterChip.vue'

const props = defineProps({
  activeFilters: { type: Array, default: () => [] },
  statuses:      { type: Array, default: () => ['To Do', 'In Progress', 'Done', 'Cancelled'] },
  priorities:    { type: Array, default: () => ['Low', 'Medium', 'High', 'Critical'] },
  issueTypes:    { type: Array, default: () => ['task', 'story', 'bug', 'epic'] },
})

const emit = defineEmits(['add-filter', 'remove-filter', 'clear-filters', 'open-builder'])

const quickStatus   = ref('')
const quickPriority = ref('')
const quickType     = ref('')

function applyQuickFilter(field, value) {
  if (!value) {
    emit('remove-filter', field)
  } else {
    emit('add-filter', { field, operator: 'eq', values: [value] })
  }
}

// Dışarıdan filtre temizlenince dropdown'ları sıfırla
watch(() => props.activeFilters, (filters) => {
  if (!filters.find(f => f.field === 'status'))    quickStatus.value   = ''
  if (!filters.find(f => f.field === 'priority'))  quickPriority.value = ''
  if (!filters.find(f => f.field === 'issueType')) quickType.value     = ''
})
</script>

<style>
.chip-enter-active {
  transition: all 0.2s ease-out;
}
.chip-leave-active {
  transition: all 0.15s ease-in;
}
.chip-enter-from {
  opacity: 0;
  transform: scale(0.8) translateY(-4px);
}
.chip-leave-to {
  opacity: 0;
  transform: scale(0.8);
}
</style>

