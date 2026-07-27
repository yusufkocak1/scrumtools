<template>
  <section
    v-if="available"
    class="panel"
    :class="{
      'panel--editing': editing,
      'panel--dragging': isDragging,
      'panel--collapsed': collapsed,
    }"
    :draggable="editing"
    @dragstart="onDragStart"
    @dragenter="onDragEnter"
    @dragover.prevent
    @drop.prevent="layout.endDrag()"
    @dragend="layout.endDrag()"
  >
    <header class="panel-head" @click="onHeadClick">
      <button
        type="button"
        class="panel-chevron"
        :aria-expanded="String(!collapsed)"
        :title="collapsed ? 'Genişlet' : 'Daralt'"
        @click.stop="layout.toggleCollapsed(panelKey)"
      >
        <svg class="w-3.5 h-3.5 transition-transform" :class="{ '-rotate-90': collapsed }"
             fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 9l-7 7-7-7"/>
        </svg>
      </button>

      <span v-if="$slots.icon" class="panel-icon"><slot name="icon" /></span>

      <h3 class="panel-title">{{ title }}</h3>

      <span v-if="count !== null && count !== undefined" class="panel-count">{{ count }}</span>

      <div class="ml-auto flex items-center gap-1" @click.stop>
        <!-- Düzen modunda panel eylemleri yerini taşıma kontrollerine bırakır:
             dokunmatik cihazda sürükleme çalışmadığı için buton alternatifi şart -->
        <template v-if="editing">
          <button type="button" class="panel-move" title="Yukarı taşı" @click="layout.moveBy(panelKey, -1)">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"/>
            </svg>
          </button>
          <button type="button" class="panel-move" title="Aşağı taşı" @click="layout.moveBy(panelKey, 1)">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
            </svg>
          </button>
          <button
            type="button"
            class="panel-move"
            :title="column === 'main' ? 'Yan kolona taşı' : 'Ana kolona taşı'"
            @click="layout.moveToOtherColumn(panelKey)"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M8 7h12m0 0l-4-4m4 4l-4 4M16 17H4m0 0l4 4m-4-4l4-4"/>
            </svg>
          </button>
          <span class="panel-grip" title="Sürükleyerek taşı">
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <circle cx="7" cy="5" r="1.5"/><circle cx="13" cy="5" r="1.5"/>
              <circle cx="7" cy="10" r="1.5"/><circle cx="13" cy="10" r="1.5"/>
              <circle cx="7" cy="15" r="1.5"/><circle cx="13" cy="15" r="1.5"/>
            </svg>
          </span>
        </template>
        <slot v-else name="actions" />
      </div>
    </header>

    <div v-show="!collapsed" class="panel-body">
      <slot />
    </div>
  </section>
</template>

<script setup>
/**
 * TaskPanel — görev detayındaki tüm bölümlerin ortak kabuğu.
 *
 * Kart görünümünü tek yerde tutar (eski tasarımda her bölüm kendi rengini ve
 * başlık düzenini taşıdığı için sayfa kalabalık görünüyordu), katlama ve
 * sürükle-bırak davranışını sayfadan `taskLayout` üzerinden alır.
 *
 * Görev detayı dışında (provider yokken) de sorunsuz render olur: bu durumda
 * panel sabit, açık ve sürüklenemez bir karta dönüşür.
 */
import { computed, inject } from 'vue'

const props = defineProps({
  /** useTaskLayout içindeki panel anahtarı */
  panelKey: { type: String, required: true },
  title: { type: String, required: true },
  /** Başlıktaki sayaç rozeti — null ise gösterilmez */
  count: { type: [Number, String], default: null },
  /** Panelin gösterilecek verisi/yetkisi yoksa false — kart tamamen gizlenir */
  available: { type: Boolean, default: true },
})

const fallback = {
  editing: { value: false },
  draggingKey: { value: null },
  isCollapsed: () => false,
  toggleCollapsed: () => {},
  columnOf: () => 'main',
  moveBy: () => {},
  moveToOtherColumn: () => {},
  startDrag: () => {},
  endDrag: () => {},
  dragOverPanel: () => {},
}

const layout = inject('taskLayout', fallback)

const editing = computed(() => layout.editing.value)
const collapsed = computed(() => layout.isCollapsed(props.panelKey))
const column = computed(() => layout.columnOf(props.panelKey))
const isDragging = computed(() => layout.draggingKey.value === props.panelKey)

function onHeadClick() {
  if (!editing.value) layout.toggleCollapsed(props.panelKey)
}

function onDragStart(e) {
  if (!editing.value) return
  e.dataTransfer.effectAllowed = 'move'
  // Firefox sürüklemeyi ancak veri set edilirse başlatır
  e.dataTransfer.setData('text/plain', props.panelKey)
  layout.startDrag(props.panelKey)
}

function onDragEnter() {
  // Yer değiştirme dragover yerine dragenter'da yapılır: dragover saniyede
  // onlarca kez tetiklenip listeyi titretir
  if (editing.value) layout.dragOverPanel(props.panelKey)
}
</script>

<style scoped>
.panel {
  @apply bg-white rounded-xl border border-gray-200 transition-colors duration-150;
}
.panel--editing {
  @apply border-dashed border-blue-300 bg-blue-50/20 cursor-grab select-none;
}
.panel--dragging {
  @apply opacity-40 cursor-grabbing;
}

.panel-head {
  @apply flex items-center gap-2 px-4 py-3 cursor-pointer;
}
.panel--collapsed .panel-head {
  @apply py-2.5;
}

.panel-chevron {
  @apply flex-shrink-0 w-5 h-5 -ml-1 flex items-center justify-center rounded text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors;
}
.panel-icon {
  @apply flex-shrink-0 text-gray-400;
}
.panel-title {
  @apply text-sm font-semibold text-gray-800 truncate;
}
.panel-count {
  @apply inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full bg-gray-100 text-[11px] font-semibold text-gray-500;
}

.panel-move {
  @apply w-6 h-6 flex items-center justify-center rounded-md text-blue-500 bg-white border border-blue-200 hover:bg-blue-50 transition-colors;
}
.panel-grip {
  @apply w-6 h-6 flex items-center justify-center text-blue-300;
}

.panel-body {
  @apply px-4 pb-4;
}
</style>
