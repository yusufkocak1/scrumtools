<template>
  <div class="space-y-0.5">
    <div v-for="page in pages" :key="page.id">
      <div
          @click="$emit('select', page)"
          :class="[
            'group flex items-center gap-1 pr-1.5 py-[5px] rounded-lg cursor-pointer text-sm transition select-none',
            page.id === selectedPageId
              ? 'bg-indigo-100/70 text-indigo-700 font-medium'
              : 'text-slate-600 hover:bg-slate-200/60 hover:text-slate-800'
          ]"
          :style="{ paddingLeft: (depth * 14 + 6) + 'px' }">
        <button v-if="page.children && page.children.length"
                @click.stop="toggle(page.id)"
                class="w-5 h-5 shrink-0 flex items-center justify-center rounded text-slate-400 hover:bg-slate-300/50 hover:text-slate-600 transition"
                :title="isOpen(page.id) ? 'Daralt' : 'Genişlet'">
          <svg :class="['w-3 h-3 transition-transform duration-150', isOpen(page.id) ? 'rotate-90' : '']"
               fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5"/>
          </svg>
        </button>
        <span v-else class="w-5 shrink-0"></span>
        <svg :class="['w-4 h-4 shrink-0', page.id === selectedPageId ? 'text-indigo-500' : 'text-slate-400']"
             fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round"
                d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"/>
        </svg>
        <span class="truncate flex-1">{{ page.title }}</span>
        <button @click.stop="$emit('add-child', page)"
                title="Alt sayfa ekle"
                class="w-5 h-5 shrink-0 hidden group-hover:flex items-center justify-center rounded text-slate-400 hover:text-indigo-600 hover:bg-indigo-100 transition">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
          </svg>
        </button>
      </div>
      <!-- Children -->
      <PageTree
          v-if="page.children && page.children.length && isOpen(page.id)"
          :pages="page.children"
          :selectedPageId="selectedPageId"
          :depth="depth + 1"
          :forceExpand="forceExpand"
          @select="(p) => $emit('select', p)"
          @add-child="(p) => $emit('add-child', p)"
      />
    </div>
    <div v-if="pages.length === 0 && depth === 0" class="px-3 py-6 text-center">
      <p class="text-sm text-slate-400">Henüz sayfa yok</p>
    </div>
  </div>
</template>

<script setup>
import {reactive, watch} from 'vue'

const props = defineProps({
  pages: {type: Array, default: () => []},
  selectedPageId: {type: String, default: null},
  depth: {type: Number, default: 0},
  /** Arama sırasında tüm dalları açık tutar */
  forceExpand: {type: Boolean, default: false}
})

defineEmits(['select', 'add-child'])

const expanded = reactive({})

// Yeni gelen üst sayfalar varsayılan olarak açık; kullanıcının kapattıkları korunur
watch(() => props.pages, (pages) => {
  pages.forEach(p => {
    if (p.children && p.children.length && !(p.id in expanded)) {
      expanded[p.id] = true
    }
  })
}, {immediate: true, deep: true})

function isOpen(id) {
  return props.forceExpand || expanded[id]
}

function toggle(id) {
  expanded[id] = !expanded[id]
}
</script>
