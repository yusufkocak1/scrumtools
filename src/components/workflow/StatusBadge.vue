<template>
  <span
    :class="[
      'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap',
      categoryClass
    ]"
    :style="colorStyle"
  >
    <span v-if="status.icon" class="text-xs">{{ status.icon }}</span>
    {{ status.name }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** { name, category, color, icon } veya sadece string status adı */
  status: {
    type: [Object, String],
    required: true,
  },
})

const statusObj = computed(() =>
  typeof props.status === 'string'
    ? { name: props.status, category: guessCategoryFromName(props.status), color: null, icon: null }
    : props.status
)

const categoryClass = computed(() => {
  const cat = statusObj.value?.category || 'TO_DO'
  return {
    TO_DO: 'bg-gray-100 text-gray-700',
    IN_PROGRESS: 'bg-blue-100 text-blue-700',
    DONE: 'bg-green-100 text-green-700',
  }[cat] ?? 'bg-gray-100 text-gray-700'
})

const colorStyle = computed(() => {
  const color = statusObj.value?.color
  if (!color) return {}
  return {
    backgroundColor: `${color}20`,
    color: color,
  }
})

function guessCategoryFromName(name) {
  const n = name?.toLowerCase() || ''
  if (['done', 'closed', 'fixed', 'verified', 'cancelled', 'won\'t fix'].some(s => n.includes(s)))
    return 'DONE'
  if (['progress', 'review', 'testing', 'selected'].some(s => n.includes(s)))
    return 'IN_PROGRESS'
  return 'TO_DO'
}
</script>

