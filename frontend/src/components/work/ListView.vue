<template>
  <div class="flex flex-col h-full">
    <!-- Filtre çubuğu -->
    <FilterBar
      :active-filters="activeFilters"
      @add-filter="addFilter"
      @remove-filter="removeFilter"
      @clear-filters="clearFilters"
      @open-builder="showBuilder = true"
    />

    <!-- Tablo -->
    <div class="flex-1 overflow-auto">
      <div v-if="isLoading" class="flex items-center justify-center py-16 text-gray-400 text-sm">
        Yükleniyor…
      </div>

      <table v-else class="w-full text-sm border-collapse">
        <!-- Başlık -->
        <thead class="bg-gray-50 sticky top-0 z-10">
          <tr class="border-b border-gray-200">
            <th
              v-for="col in visibleColumns"
              :key="col.key"
              class="text-left px-3 py-2.5 text-xs font-semibold text-gray-500 uppercase tracking-wide whitespace-nowrap select-none cursor-pointer hover:bg-gray-100"
              @click="toggleSort(col.key)"
            >
              <div class="flex items-center gap-1">
                {{ col.label }}
                <span v-if="sortBy === col.key" class="text-blue-500">
                  {{ sortDir === 'asc' ? '↑' : '↓' }}
                </span>
              </div>
            </th>
          </tr>
        </thead>

        <!-- Gövde -->
        <tbody>
          <tr
            v-for="{ task, depth, hasChildren } in displayRows"
            :key="task.id"
            class="border-b border-gray-100 hover:bg-blue-50 cursor-pointer transition-colors"
            @click="openTask(task)"
          >
            <!-- ID -->
            <td class="px-3 py-2 font-mono text-xs text-gray-400 whitespace-nowrap">
              <div class="flex items-center gap-1" :class="depth === 1 ? 'pl-6' : ''">
                <button
                  v-if="hasChildren"
                  class="w-4 text-gray-400 hover:text-gray-700"
                  title="Alt görevleri aç/kapat"
                  @click.stop="toggleExpand(task.id)"
                >{{ isExpanded(task.id) ? '▾' : '▸' }}</button>
                <span v-else-if="depth === 1" class="text-gray-300">↳</span>
                <span v-else class="w-4"></span>
                <span>{{ task.customId }}</span>
                <span
                  v-if="hasChildren"
                  class="ml-1 px-1.5 py-0.5 rounded-full bg-gray-100 text-gray-500 text-[10px] font-sans"
                >{{ task.subtaskCount }}</span>
              </div>
            </td>
            <!-- Başlık -->
            <td class="px-3 py-2 font-medium text-gray-900 max-w-xs truncate">{{ task.title }}</td>
            <!-- Tür -->
            <td class="px-3 py-2 whitespace-nowrap">
              <span class="text-xs px-1.5 py-0.5 rounded bg-gray-100 text-gray-600 capitalize">{{ task.issueType }}</span>
            </td>
            <!-- Durum -->
            <td class="px-3 py-2 whitespace-nowrap">
              <span class="text-xs px-2 py-0.5 rounded-full" :class="statusClass(task.status)">{{ task.status }}</span>
            </td>
            <!-- Öncelik -->
            <td class="px-3 py-2 whitespace-nowrap">
              <span class="text-xs px-1.5 py-0.5 rounded" :class="priorityClass(task.priority)">{{ task.priority }}</span>
            </td>
            <!-- Atanan -->
            <td class="px-3 py-2 text-xs text-gray-600 whitespace-nowrap max-w-[120px] truncate">
              {{ task.assignee || '—' }}
            </td>
            <!-- Son Tarih -->
            <td class="px-3 py-2 text-xs whitespace-nowrap" :class="overdueClass(task.dueDate)">
              {{ task.dueDate || '—' }}
            </td>
            <!-- SP -->
            <td class="px-3 py-2 text-xs text-center text-gray-600">{{ task.storyPoints || '—' }}</td>
          </tr>

          <!-- Sonuç yok -->
          <tr v-if="displayRows.length === 0">
            <td :colspan="visibleColumns.length" class="text-center py-12 text-gray-400 text-sm">
              Görev bulunamadı
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Sayfalama -->
    <div v-if="totalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200 bg-white">
      <span class="text-xs text-gray-500">
        {{ totalElements }} sonuç · Sayfa {{ page + 1 }}/{{ totalPages }}
      </span>
      <div class="flex gap-2">
        <button
          :disabled="page === 0"
          class="px-3 py-1 text-xs rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50"
          @click="changePage(page - 1)"
        >← Önceki</button>
        <button
          :disabled="page >= totalPages - 1"
          class="px-3 py-1 text-xs rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50"
          @click="changePage(page + 1)"
        >Sonraki →</button>
      </div>
    </div>

    <!-- Filter builder modal -->
    <FilterBuilder
      :is-open="showBuilder"
      :initial-filters="activeFilters"
      @close="showBuilder = false"
      @apply="applyBuilderFilters"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import FilterBar     from './FilterBar.vue'
import FilterBuilder from './FilterBuilder.vue'
import { getTasks, filterTasks } from '../../api/WorkApi.js'
import { buildTaskTree } from '../../utils/taskHierarchy.js'

const props = defineProps({
  teamId: { type: String, required: true },
  /** Aktif proje context'i — null ise takımın tüm projelerindeki görevler listelenir. */
  projectId: { type: String, default: null }
})

const router = useRouter()

// ─── State ────────────────────────────────────────────────────────────────────
const allTasks      = ref([])
const isLoading     = ref(false)
const showBuilder   = ref(false)
const activeFilters = ref([])
const sortBy        = ref('createdAt')
const sortDir       = ref('desc')
const page          = ref(0)
const size          = ref(50)
const totalElements = ref(0)
const totalPages    = ref(1)

const visibleColumns = [
  { key: 'customId',   label: 'ID' },
  { key: 'title',      label: 'Başlık' },
  { key: 'issueType',  label: 'Tür' },
  { key: 'status',     label: 'Durum' },
  { key: 'priority',   label: 'Öncelik' },
  { key: 'assignee',   label: 'Atanan' },
  { key: 'dueDate',    label: 'Son Tarih' },
  { key: 'storyPoints',label: 'SP' },
]

// ─── Yükleme ──────────────────────────────────────────────────────────────────
async function loadTasks() {
  isLoading.value = true
  try {
    if (activeFilters.value.length > 0) {
      const result = await filterTasks(props.teamId, {
        filters:   activeFilters.value,
        projectId: props.projectId || undefined,
        sortBy:  sortBy.value,
        sortDir: sortDir.value,
        page:    page.value,
        size:    size.value
      })
      allTasks.value      = result.content
      totalElements.value = result.totalElements
      totalPages.value    = result.totalPages
    } else {
      const data = await getTasks(props.teamId, false, props.projectId)
      allTasks.value      = data
      totalElements.value = data.length
      totalPages.value    = 1
    }
  } catch (e) {
    console.error('ListView yükleme hatası:', e)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadTasks)
watch(() => [props.teamId, props.projectId], () => { page.value = 0; loadTasks() })
watch(activeFilters, () => { page.value = 0; loadTasks() }, { deep: true })

// ─── Hiyerarşi: aç/kapa (varsayılan açık — kapatılanlar takip edilir) ─────────
const collapsed = ref(new Set())

function isExpanded(id) {
  return !collapsed.value.has(id)
}

function toggleExpand(id) {
  const next = new Set(collapsed.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  collapsed.value = next
}

// ─── Sıralı + Sayfalı satırlar (filtre yoksa client-side hiyerarşi) ───────────
const displayRows = computed(() => {
  // Server-side filtre modunda düz liste: filtrelenmiş alt kümede iç içe
  // gösterim yanıltıcı olur (Jira'da da JQL sonuçları düzdür)
  if (activeFilters.value.length > 0) {
    return allTasks.value.map(t => ({ task: t, depth: 0, hasChildren: false }))
  }
  const tree = buildTaskTree(allTasks.value)
  // Top-level sıralanır; subtask'lar parent'ının altında customId sırasıyla gelir
  const sorted = [...tree.topLevel].sort((a, b) => {
    const av = a[sortBy.value], bv = b[sortBy.value]
    if (av == null) return 1
    if (bv == null) return -1
    const cmp = av < bv ? -1 : av > bv ? 1 : 0
    return sortDir.value === 'asc' ? cmp : -cmp
  })
  const start = page.value * size.value
  const rows = []
  for (const task of sorted.slice(start, start + size.value)) {
    const children = tree.childrenOf(task.id)
    rows.push({ task, depth: 0, hasChildren: children.length > 0 })
    if (children.length > 0 && isExpanded(task.id)) {
      const subs = [...children].sort((a, b) =>
        (a.customId || '').localeCompare(b.customId || '', undefined, { numeric: true }))
      for (const sub of subs) rows.push({ task: sub, depth: 1, hasChildren: false })
    }
  }
  return rows
})

function toggleSort(key) {
  if (sortBy.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value  = key
    sortDir.value = 'asc'
  }
  if (activeFilters.value.length > 0) loadTasks()
}

function changePage(p) {
  page.value = p
  loadTasks()
}

// ─── Task detay ───────────────────────────────────────────────────────────────
function openTask(task) {
  router.push({ name: 'TaskDetail', params: { taskId: task.customId || task.id } })
}

// ─── Filtreler ────────────────────────────────────────────────────────────────
function addFilter(filter) {
  const idx = activeFilters.value.findIndex(f => f.field === filter.field)
  if (idx >= 0) activeFilters.value[idx] = { ...filter }
  else activeFilters.value.push({ ...filter })
}

function removeFilter(field) {
  activeFilters.value = activeFilters.value.filter(f => f.field !== field)
}

function clearFilters() {
  activeFilters.value = []
}

function applyBuilderFilters(filters) {
  activeFilters.value = filters
}

// ─── Stil yardımcıları ────────────────────────────────────────────────────────
function statusClass(s) {
  return {
    'To Do':       'bg-gray-100 text-gray-700',
    'In Progress': 'bg-blue-100 text-blue-700',
    'Done':        'bg-green-100 text-green-700',
    'Cancelled':   'bg-red-100 text-red-600',
  }[s] || 'bg-gray-100 text-gray-600'
}

function priorityClass(p) {
  return {
    Critical: 'bg-red-100 text-red-700',
    High:     'bg-orange-100 text-orange-700',
    Medium:   'bg-yellow-100 text-yellow-700',
    Low:      'bg-green-100 text-green-700',
  }[p] || 'bg-gray-100 text-gray-600'
}

function overdueClass(dateStr) {
  if (!dateStr) return 'text-gray-400'
  return new Date(dateStr) < new Date() ? 'text-red-600 font-semibold' : 'text-gray-600'
}
</script>

