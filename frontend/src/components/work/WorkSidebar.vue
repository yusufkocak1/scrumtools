<template>
  <!-- Mobil drawer arkaplanı — masaüstünde sidebar zaten akışta olduğu için gizli -->
  <div
    v-if="open"
    class="fixed inset-0 z-[9998] bg-black/40 lg:hidden"
    @click="$emit('close')"
  ></div>

  <!-- Drawer, navbar'ın (z-9999) üstünde kalmalı; aksi halde üst kısmı header'ın
       altında kalıyor. lg'de z sıfırlanıp normal akışa döner. -->
  <aside
    :class="[
      'fixed inset-y-0 left-0 z-[9999] w-64 flex flex-col bg-white border-r border-gray-200 transition-transform duration-200 overflow-y-auto',
      'pt-safe pb-safe lg:pt-0 lg:pb-0',
      'lg:static lg:z-auto lg:w-60 lg:shrink-0 lg:translate-x-0 lg:transition-none',
      open ? 'translate-x-0' : '-translate-x-full'
    ]"
  >
    <!-- ── Proje context'i ─────────────────────────────────────────────────
         Görev/backlog/sürüm görünümlerinin tamamını daraltır. Sprintler takım
         bazlı kaldığı için bu seçim sprintleri gizlemez, içindeki görevleri
         filtreler. -->
    <div class="px-3 py-3 border-b border-gray-100">
      <div class="flex items-center justify-between mb-1.5">
        <span class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Proje</span>
        <!-- Takıma proje ekleme/çıkarma: projesiz takımın da buraya ulaşabilmesi
             gerekiyor, bu yüzden hasProjects'e bağlı değil. -->
        <button
          class="text-[11px] text-gray-500 hover:text-blue-600 transition"
          @click="$emit('manage-projects')"
          title="Takım projelerini yönet"
        >
          {{ hasProjects ? '+ Proje' : '+ Proje Bağla' }}
        </button>
      </div>

      <div v-if="hasProjects" class="flex items-center gap-1.5">
        <span
          v-if="activeProject"
          class="w-2 h-2 rounded-full shrink-0"
          :style="{ backgroundColor: activeProject.color || '#3B82F6' }"
        ></span>
        <select
          :value="projectId ?? allProjectsValue"
          @change="$emit('select-project', $event.target.value === allProjectsValue ? null : $event.target.value)"
          class="w-full min-w-0 text-xs font-medium rounded-md border border-gray-300 px-2 py-1.5 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
        >
          <option v-for="p in projects" :key="p.id" :value="p.id">
            {{ p.name }} ({{ p.key }})
          </option>
          <option v-if="projects.length > 1" :value="allProjectsValue">Tüm projeler</option>
        </select>
      </div>
      <p v-else class="text-xs text-gray-400">Takıma bağlı proje yok</p>
    </div>

    <!-- ── Görünümler ──────────────────────────────────────────────────── -->
    <nav class="p-2 space-y-0.5">
      <button
        v-for="v in views"
        :key="v.key"
        class="flex items-center gap-2.5 w-full px-2.5 py-2 text-sm font-medium rounded-lg transition-colors text-left"
        :class="modelValue === v.key
          ? 'bg-blue-50 text-blue-700'
          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'"
        @click="$emit('update:modelValue', v.key)"
      >
        <component :is="v.icon" class="w-4 h-4 shrink-0" />
        {{ v.label }}
      </button>

      <!-- Zengin filtreler ayrı bir sayfadır (görünüm sekmesi değil): görevleri
           renkli kategorilere ayıran tanımlar burada kurulur, dashboard grafikleri
           oradan beslenir. Çalışma alanının parçası olduğu için menüsü burada. -->
      <router-link
        to="/rich-filters"
        class="flex items-center gap-2.5 w-full px-2.5 py-2 text-sm font-medium rounded-lg transition-colors text-left"
        :class="$route.path.startsWith('/rich-filters')
          ? 'bg-blue-50 text-blue-700'
          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'"
      >
        <component :is="FilterIcon" class="w-4 h-4 shrink-0" />
        Zengin Filtreler
      </router-link>
    </nav>

    <!-- ── Board görünüm kontrolleri (sadece Board görünümünde) ──────────
         Burada yalnızca görünüm durumu var: hangi board açık, nasıl gruplanmış.
         Board'un yapılandırması (sütunlar, durum eşlemesi, yeni board) artık
         merkezi ayar sayfasında — ayarların görünümlerin içine dağılmaması için. -->
    <div v-if="modelValue === 'board'" class="px-3 py-3 border-t border-gray-100 space-y-2">
      <span class="block text-[11px] font-semibold uppercase tracking-wide text-gray-400">Board</span>

      <!-- Board seçici — yalnızca aktif projenin (ve takım geneli) board'ları -->
      <select
        v-if="boards.length > 1"
        :value="selectedBoardId"
        @change="$emit('update:selectedBoardId', $event.target.value)"
        class="w-full min-w-0 text-xs rounded-md border border-gray-300 px-2 py-1.5 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
      >
        <option v-for="b in boards" :key="b.id" :value="b.id">
          {{ b.name }} ({{ b.boardType === 'SCRUM' ? 'Scrum' : 'Kanban' }})
        </option>
      </select>

      <!-- Gruplama seçici -->
      <select
        :value="groupBy"
        @change="$emit('update:groupBy', $event.target.value)"
        class="w-full min-w-0 text-xs rounded-md border border-gray-300 px-2 py-1.5 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
      >
        <option value="status">Grupla: Status</option>
        <option value="assignee">Grupla: Kişi</option>
      </select>
    </div>

    <!-- ── Ayarlar ───────────────────────────────────────────────────────── -->
    <div class="mt-auto px-3 py-3 border-t border-gray-100">
      <router-link
        :to="settingsTo"
        class="flex items-center gap-2.5 w-full px-2.5 py-2 text-sm font-medium rounded-lg text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-colors"
      >
        <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
        </svg>
        Ayarlar
      </router-link>
    </div>
  </aside>
</template>

<script setup>
import { h, computed } from 'vue'

const props = defineProps({
  /** Mobil drawer açık mı — masaüstünde yok sayılır (sidebar her zaman görünür) */
  open: { type: Boolean, default: false },
  modelValue: { type: String, default: 'board' },

  /** Ayarlar bağlantısının hedeflediği takım */
  teamId: { type: String, default: null },

  projects: { type: Array, default: () => [] },
  projectId: { type: String, default: null },
  activeProject: { type: Object, default: null },
  hasProjects: { type: Boolean, default: false },
  allProjectsValue: { type: String, required: true },

  /** Aktif projede görünür board'lar */
  boards: { type: Array, default: () => [] },
  selectedBoardId: { type: String, default: null },
  groupBy: { type: String, default: 'status' },
})

defineEmits([
  'update:modelValue',
  'update:selectedBoardId',
  'update:groupBy',
  'select-project',
  'manage-projects',
  'close',
])

/** Board görünümündeyken doğrudan board ayarları bölümüne açılır. */
const settingsTo = computed(() => ({
  path: `/workspace-settings${props.teamId ? '/' + props.teamId : ''}`,
  query: { section: props.modelValue === 'board' ? 'boards' : 'statuses' },
}))

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

const FilterIcon = () => h('svg', {
  fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24',
  strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round'
}, [
  h('path', { d: 'M22 3H2l8 9.46V19l4 2v-8.54L22 3z' }),
])

const views = [
  { key: 'board',    label: 'Board',    icon: BoardIcon },
  { key: 'list',     label: 'Liste',    icon: ListIcon },
  { key: 'backlog',  label: 'Backlog',  icon: BacklogIcon },
  { key: 'releases', label: 'Sürümler', icon: ReleaseIcon },
  { key: 'activity', label: 'Aktivite', icon: ActivityIcon },
]
</script>
