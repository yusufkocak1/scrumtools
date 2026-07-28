<template>
  <span
    :class="[
      'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap',
      categoryClass
    ]"
    :style="colorStyle"
  >
    <span v-if="statusObj.icon" class="text-xs">{{ statusObj.icon }}</span>
    {{ statusObj.name }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { guessCategory, CATEGORY_CLASSES } from '../../composables/useTaskStatuses.js'

const props = defineProps({
  /** { name, category, color, icon } veya sadece string status adı */
  status: {
    type: [Object, String],
    required: true,
  },
})

const statusObj = computed(() =>
  typeof props.status === 'string'
    ? { name: props.status, category: guessCategory(props.status), color: null, icon: null }
    : props.status
)

const categoryClass = computed(() => {
  const cat = statusObj.value?.category || 'TO_DO'
  return CATEGORY_CLASSES[cat] ?? CATEGORY_CLASSES.TO_DO
})

const colorStyle = computed(() => {
  const color = statusObj.value?.color
  if (!color) return {}
  return {
    backgroundColor: `${color}20`,
    color: color,
  }
})

// Kategori tahmini tek yerde: composable ile backend aynı sözcük kümesini
// kullanıyor, burada ayrı bir kopya tutmak üçüncü bir doğruluk kaynağı üretirdi.
</script>

