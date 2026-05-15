<template>
  <span
    class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 border border-blue-200"
  >
    <span class="font-semibold capitalize">{{ fieldLabel }}</span>
    <span class="text-blue-500">{{ operatorSymbol }}</span>
    <span>{{ valueLabel }}</span>
    <button
      class="ml-0.5 text-blue-400 hover:text-blue-700 focus:outline-none"
      @click="$emit('remove')"
      aria-label="Filtreyi kaldır"
    >
      <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd"
          d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
          clip-rule="evenodd"/>
      </svg>
    </button>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  filter: { type: Object, required: true }
})
defineEmits(['remove'])

const FIELD_LABELS = {
  status: 'Durum', priority: 'Öncelik', issueType: 'Tür',
  assignee: 'Kişi', reporter: 'Açan', labels: 'Etiket',
  dueDate: 'Son Tarih', startDate: 'Başl. Tarihi', sprintId: 'Sprint'
}

const OP_SYMBOLS = {
  eq: '=', neq: '≠', in: '∈', contains: '~',
  gt: '>', lt: '<', is_null: '= boş', is_not_null: '≠ boş'
}

const fieldLabel    = computed(() => FIELD_LABELS[props.filter.field] || props.filter.field)
const operatorSymbol = computed(() => OP_SYMBOLS[props.filter.operator] || props.filter.operator)
const valueLabel    = computed(() => {
  const v = props.filter.values
  if (!v || v.length === 0) return ''
  return v.length > 2 ? `${v.slice(0, 2).join(', ')} +${v.length - 2}` : v.join(', ')
})
</script>

