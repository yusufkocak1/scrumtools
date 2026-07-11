<template>
  <div class="inline-flex bg-gray-100/80 rounded-lg p-1 gap-0.5 border border-gray-200/60">
    <button
      v-for="v in views"
      :key="v.key"
      class="flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded-md transition-all duration-150"
      :class="modelValue === v.key
        ? 'bg-white text-gray-900 shadow-sm ring-1 ring-gray-200/80'
        : 'text-gray-500 hover:text-gray-700 hover:bg-white/50'"
      @click="$emit('update:modelValue', v.key)"
    >
      <component :is="v.icon" class="w-3.5 h-3.5" />
      {{ v.label }}
    </button>
  </div>
</template>

<script setup>
import { h } from 'vue'

defineProps({
  modelValue: { type: String, default: 'board' }
})
defineEmits(['update:modelValue'])

// SVG icon bileşenleri inline olarak tanımlandı
const BoardIcon = () => h('svg', {
  fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24',
  strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round'
}, [
  h('rect', { x: 3, y: 3, width: 7, height: 18, rx: 1 }),
  h('rect', { x: 14, y: 3, width: 7, height: 10, rx: 1 }),
  h('rect', { x: 14, y: 17, width: 7, height: 4, rx: 1 }),
])

const ListIcon = () => h('svg', {
  fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24',
  strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round'
}, [
  h('line', { x1: 8, y1: 6, x2: 21, y2: 6 }),
  h('line', { x1: 8, y1: 12, x2: 21, y2: 12 }),
  h('line', { x1: 8, y1: 18, x2: 21, y2: 18 }),
  h('line', { x1: 3, y1: 6, x2: 3.01, y2: 6 }),
  h('line', { x1: 3, y1: 12, x2: 3.01, y2: 12 }),
  h('line', { x1: 3, y1: 18, x2: 3.01, y2: 18 }),
])

const BacklogIcon = () => h('svg', {
  fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24',
  strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round'
}, [
  h('path', { d: 'M9 11l3 3L22 4' }),
  h('path', { d: 'M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11' }),
])

const ActivityIcon = () => h('svg', {
  fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24',
  strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round'
}, [
  h('path', { d: 'M22 12h-4l-3 9L9 3l-3 9H2' }),
])

const ReleaseIcon = () => h('svg', {
  fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24',
  strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round'
}, [
  h('path', { d: 'M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z' }),
  h('line', { x1: 7, y1: 7, x2: 7.01, y2: 7 }),
])

const views = [
  { key: 'board',    label: 'Board',    icon: BoardIcon },
  { key: 'list',     label: 'Liste',    icon: ListIcon },
  { key: 'backlog',  label: 'Backlog',  icon: BacklogIcon },
  { key: 'releases', label: 'Sürümler', icon: ReleaseIcon },
  { key: 'activity', label: 'Aktivite', icon: ActivityIcon },
]
</script>

