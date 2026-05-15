<template>
  <div class="space-y-1">
    <div v-for="page in pages" :key="page.id">
      <div
          @click="$emit('select', page)"
          :class="[
            'flex items-center gap-2 px-3 py-1.5 rounded-lg cursor-pointer text-sm transition',
            page.id === selectedPageId
              ? 'bg-indigo-100 text-indigo-700 font-medium'
              : 'text-gray-700 hover:bg-gray-100'
          ]"
          :style="{ paddingLeft: (depth * 16 + 12) + 'px' }">
        <span v-if="page.children && page.children.length" @click.stop="toggle(page.id)" class="cursor-pointer">
          {{ expanded[page.id] ? '▾' : '▸' }}
        </span>
        <span v-else class="w-3"></span>
        <span class="truncate">{{ page.title }}</span>
      </div>
      <!-- Children -->
      <PageTree
          v-if="page.children && page.children.length && expanded[page.id]"
          :pages="page.children"
          :selectedPageId="selectedPageId"
          :depth="depth + 1"
          @select="(p) => $emit('select', p)"
      />
    </div>
    <div v-if="pages.length === 0 && depth === 0" class="text-sm text-gray-400 px-3 py-2">
      Henüz sayfa yok
    </div>
  </div>
</template>

<script setup>
import {reactive} from 'vue'

const props = defineProps({
  pages: {type: Array, default: () => []},
  selectedPageId: {type: String, default: null},
  depth: {type: Number, default: 0}
})

defineEmits(['select'])

const expanded = reactive({})

// Başlangıçta hepsini aç
props.pages.forEach(p => {
  if (p.children && p.children.length) {
    expanded[p.id] = true
  }
})

function toggle(id) {
  expanded[id] = !expanded[id]
}
</script>

