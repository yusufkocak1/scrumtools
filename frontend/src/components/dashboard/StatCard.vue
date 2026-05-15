<template>
  <div class="bg-white rounded-xl border border-gray-100 shadow-sm px-3 py-4 flex flex-col items-center justify-center gap-1" :class="color">
    <span class="text-2xl font-bold">{{ value }}</span>
    <span class="text-xs font-medium opacity-80">{{ label }}</span>
  </div>
</template>

<script setup>
defineProps({
  label: String,
  value: { type: Number, default: 0 },
  color: { type: String, default: 'bg-gray-50 text-gray-700' }
})
</script>

