<template>
  <!-- overflow-hidden yok: "sütuna ekle" menüsü panelin dışına taşabilmeli -->
  <div class="rounded-lg border border-gray-200 bg-white">
    <!-- Başlık -->
    <div class="flex items-start justify-between gap-3 px-4 py-3 border-b border-gray-100">
      <div class="min-w-0">
        <h3 class="text-sm font-semibold text-gray-900">Sütun Eşlemesi</h3>
        <p class="text-xs text-gray-500 mt-0.5">
          Durumları sürükleyerek sütunlara dağıtın. Bir sütun birden fazla durum
          toplayabilir; sütunda ilk sıradaki durum <strong>birincildir</strong> —
          kart bu sütuna bırakıldığında görev o duruma geçer.
        </p>
      </div>
      <button
        type="button"
        class="shrink-0 px-2.5 py-1.5 text-xs font-medium text-blue-600 border border-blue-200 rounded-md hover:bg-blue-50"
        @click="addColumn"
      >
        + Sütun Ekle
      </button>
    </div>

    <div class="flex items-stretch">
      <!-- ── Sütun şeridi ─────────────────────────────────────────────────── -->
      <div class="flex-1 min-w-0 overflow-x-auto p-3">
        <div v-if="columns.length === 0" class="py-8 text-center text-sm text-gray-400">
          Sütun yok — board boş görünür.
        </div>

        <div v-else class="flex items-stretch">
          <template v-for="(col, idx) in columns" :key="idx">
            <!-- sütun taşıma göstergesi -->
            <div
              class="w-1 shrink-0 mx-0.5 rounded-full transition-colors"
              :class="columnDropAt === idx ? 'bg-blue-500' : 'bg-transparent'"
            ></div>

            <div
              class="w-56 shrink-0 flex flex-col rounded-lg border transition-colors"
              :class="statusDrop?.col === idx
                ? 'border-blue-400 bg-blue-50/60'
                : 'border-gray-200 bg-gray-50/70'"
              @dragover="onColumnDragOver($event, idx)"
              @drop="onCardDrop($event, idx)"
            >
              <!-- üst çubuk: tutamaç + sil -->
              <div class="flex items-center gap-1 px-2 pt-2">
                <span
                  class="cursor-grab select-none px-1 text-gray-300 hover:text-gray-500 leading-none"
                  draggable="true"
                  title="Sütunu taşımak için sürükleyin"
                  @dragstart="startColumnDrag($event, idx)"
                  @dragend="resetDrag"
                >⠿</span>
                <button
                  type="button"
                  class="p-0.5 text-gray-300 hover:text-gray-600 disabled:opacity-30"
                  :disabled="idx === 0"
                  title="Sola taşı"
                  @click="moveColumn(idx, -1)"
                >◀</button>
                <button
                  type="button"
                  class="p-0.5 text-gray-300 hover:text-gray-600 disabled:opacity-30"
                  :disabled="idx === columns.length - 1"
                  title="Sağa taşı"
                  @click="moveColumn(idx, 1)"
                >▶</button>
                <button
                  type="button"
                  class="ml-auto p-0.5 text-gray-300 hover:text-red-500 disabled:opacity-30"
                  :disabled="columns.length <= 1"
                  :title="columns.length <= 1 ? 'Son sütun silinemez' : 'Sütunu sil'"
                  @click="removeColumn(idx)"
                >🗑</button>
              </div>

              <!-- sütun adı -->
              <div class="px-2 pt-1">
                <input
                  :value="col.name"
                  placeholder="Sütun adı"
                  class="w-full bg-transparent text-sm font-semibold text-gray-900 px-1 py-1 rounded border border-transparent hover:border-gray-200 focus:bg-white focus:border-blue-400 focus:outline-none placeholder:font-normal placeholder:text-gray-400"
                  @input="updateColumn(idx, { name: $event.target.value })"
                />
              </div>

              <!-- renk şeridi: tıklayınca renk seçici -->
              <label class="block mx-3 mt-1 h-1.5 rounded-full cursor-pointer" title="Sütun rengi"
                     :style="{ backgroundColor: col.color || '#6B7280' }">
                <input
                  type="color"
                  class="sr-only"
                  :value="col.color || '#6B7280'"
                  @input="updateColumn(idx, { color: $event.target.value })"
                />
              </label>

              <!-- WIP limiti -->
              <div class="flex items-center gap-1.5 px-3 pt-2 text-[11px] text-gray-500">
                <span>WIP limiti</span>
                <input
                  type="number"
                  min="0"
                  :value="col.wipLimit || 0"
                  class="w-14 text-[11px] rounded border border-gray-200 bg-white px-1.5 py-0.5 focus:border-blue-400 focus:outline-none"
                  title="0 = sınırsız"
                  @input="updateColumn(idx, { wipLimit: Number($event.target.value) || 0 })"
                />
              </div>

              <!-- durum kartları -->
              <div
                class="flex flex-col gap-1 flex-1 px-2 py-2 mt-1"
                @dragover="onListDragOver($event, idx)"
              >
                <template v-for="(name, sIdx) in statusesOf(col)" :key="name">
                  <div
                    class="h-0.5 rounded-full transition-colors"
                    :class="isHint(idx, sIdx) ? 'bg-blue-500' : 'bg-transparent'"
                  ></div>
                  <div
                    class="group rounded-md border bg-white px-2 py-1.5 cursor-grab"
                    :class="isKnown(name)
                      ? 'border-gray-200 hover:border-gray-300'
                      : 'border-dashed border-amber-300'"
                    draggable="true"
                    @dragstart="startStatusDrag($event, name)"
                    @dragend="resetDrag"
                    @dragover="onCardDragOver($event, idx, sIdx)"
                  >
                    <div class="flex items-start gap-1">
                      <StatusBadge :status="statusOf(name)" class="max-w-[9rem] truncate" />
                      <button
                        type="button"
                        class="ml-auto shrink-0 text-gray-300 opacity-0 group-hover:opacity-100 hover:text-red-500 text-xs leading-none"
                        title="Sütundan çıkar"
                        @click="unmapStatus(name)"
                      >✕</button>
                    </div>
                    <p class="mt-1 text-[11px]" :class="countOf(name) > 0 ? 'text-gray-600' : 'text-gray-400'">
                      {{ countLabel(name) }}
                    </p>
                    <p v-if="sIdx === 0 && statusesOf(col).length > 1" class="text-[10px] text-blue-600 mt-0.5">
                      birincil
                    </p>
                    <p v-if="!isKnown(name)" class="text-[10px] text-amber-700 mt-0.5">
                      iş akışında tanımlı değil
                    </p>
                  </div>
                </template>

                <!-- sona bırakma göstergesi -->
                <div
                  class="h-0.5 rounded-full transition-colors"
                  :class="isHint(idx, statusesOf(col).length) ? 'bg-blue-500' : 'bg-transparent'"
                ></div>

                <p
                  v-if="statusesOf(col).length === 0"
                  class="flex-1 flex items-center justify-center rounded-md border border-dashed border-gray-300 px-2 py-4 text-[11px] text-center text-gray-400"
                >
                  Buraya durum sürükleyin
                </p>
              </div>
            </div>
          </template>

          <div
            class="w-1 shrink-0 mx-0.5 rounded-full transition-colors"
            :class="columnDropAt === columns.length ? 'bg-blue-500' : 'bg-transparent'"
          ></div>
        </div>
      </div>

      <!-- ── Eşlenmemiş durumlar ──────────────────────────────────────────── -->
      <aside
        class="w-56 shrink-0 border-l p-3 rounded-r-lg transition-colors"
        :class="statusDrop?.col === UNMAPPED ? 'border-blue-400 bg-blue-50/60' : 'border-gray-200 bg-gray-50'"
        @dragover="onUnmappedDragOver"
        @drop="onUnmappedDrop"
      >
        <h4 class="text-xs font-semibold text-gray-700">Eşlenmemiş Durumlar</h4>
        <p class="mt-1 text-[11px] text-gray-500 leading-snug">
          Hiçbir sütuna atanmamış durumlar. Bu durumdaki görevler board'da
          <strong>ilk sütunda</strong> toplanır.
        </p>

        <div class="mt-3 flex flex-col gap-1.5">
          <div
            v-for="status in unmappedStatuses"
            :key="status.id || status.name"
            class="group rounded-md border border-gray-200 bg-white px-2 py-1.5 cursor-grab"
            draggable="true"
            @dragstart="startStatusDrag($event, status.name)"
            @dragend="resetDrag"
          >
            <div class="flex items-start gap-1">
              <StatusBadge :status="status" class="max-w-[9rem] truncate" />
              <div class="relative ml-auto shrink-0" data-assign-menu>
                <button
                  type="button"
                  class="text-gray-300 opacity-0 group-hover:opacity-100 hover:text-blue-600 text-xs leading-none"
                  title="Bir sütuna ekle"
                  @click="assignMenuFor = assignMenuFor === status.name ? null : status.name"
                >＋</button>
                <div
                  v-if="assignMenuFor === status.name"
                  class="absolute right-0 top-5 z-10 w-36 max-h-48 overflow-y-auto rounded-md border border-gray-200 bg-white shadow-lg py-1"
                >
                  <button
                    v-for="(col, idx) in columns"
                    :key="idx"
                    type="button"
                    class="block w-full text-left px-2.5 py-1.5 text-[11px] text-gray-700 hover:bg-blue-50 truncate"
                    @click="assignToColumn(status.name, idx)"
                  >
                    {{ col.name || `Sütun ${idx + 1}` }}
                  </button>
                </div>
              </div>
            </div>
            <p class="mt-1 text-[11px]" :class="countOf(status.name) > 0 ? 'text-amber-700' : 'text-gray-400'">
              {{ countLabel(status.name) }}
            </p>
          </div>

          <p v-if="unmappedStatuses.length === 0" class="text-[11px] text-gray-400 py-2">
            Tüm durumlar bir sütuna atanmış.
          </p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
/**
 * Sütun ↔ durum haritası.
 *
 * Eşleme eskiden sütun başına açılıp kapanan durum rozetleriyle yapılıyordu;
 * hangi durumun nerede olduğunu görmek için her sütunu tek tek okumak
 * gerekiyordu. Burada eşleme board'un kendisi gibi görünüyor: durumlar sütunlar
 * arasında sürüklenir, hiçbir sütuna girmeyenler sağdaki panelde birikir.
 *
 * Sütun içindeki sıra anlamlı: `targetStatusFor` ilk durumu birincil kabul eder,
 * bu yüzden sıralama da sürüklenebilir.
 *
 * Sürükleme codebase'in geri kalanıyla aynı yerli HTML5 DnD ile yapılıyor.
 * `dataTransfer` içeriği dragover sırasında okunamadığı için taşınan şey ayrıca
 * `drag` ref'inde tutulur; `dataTransfer` yalnızca tarayıcının sürüklemeyi
 * başlatması için doldurulur.
 */
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import StatusBadge from '../workflow/StatusBadge.vue'
import { columnStatuses } from '../../utils/boardColumns.js'

/** Eşlenmemiş panelini sütun indeksleriyle aynı alanda temsil eden anahtar. */
const UNMAPPED = 'unmapped'

const props = defineProps({
  /** [{ name, color, wipLimit, statuses[] }] */
  modelValue: { type: Array, required: true },
  /** İş akışındaki durumlar: [{ id, name, category, color, icon }] */
  statuses: { type: Array, default: () => [] },
  /**
   * Durum adı (küçük harf) → görev sayısı. `null` "henüz yüklenmedi" demek;
   * yüklenmiş bir haritada eksik anahtar 0 görev anlamına gelir (GROUP BY boş
   * durumları hiç döndürmez).
   */
  counts: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue'])

const lower = (v) => String(v ?? '').toLowerCase()

const columns = computed(() => props.modelValue)

// ─── Durum bilgisi ───────────────────────────────────────────────────────────

const statusByName = computed(() => {
  const map = new Map()
  for (const s of props.statuses) map.set(lower(s.name), s)
  return map
})

/** Katalogda yoksa yalnızca adı olan bir nesne — rozet yine de çizilsin. */
function statusOf(name) {
  return statusByName.value.get(lower(name)) || { name }
}

function isKnown(name) {
  return statusByName.value.has(lower(name))
}

/**
 * Sütunun durumları — aynı ad iki kez varsa ilki kalır.
 * Eşleme bu ekranda tekrar üretemez ama elle yazılmış eski yapılandırmalarda
 * olabilir; tekrarlanan ad hem listede iki kart çizerdi hem de kaydedince
 * kalıcılaşırdı.
 */
function statusesOf(column) {
  const seen = new Set()
  return columnStatuses(column).filter(name => {
    const key = lower(name)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

/** Sayım yüklenmediyse -1 döner; "0 görev" ile "bilinmiyor" karışmasın. */
function countOf(name) {
  if (!props.counts) return -1
  const value = props.counts[lower(name)]
  return typeof value === 'number' ? value : 0
}

function countLabel(name) {
  const count = countOf(name)
  if (count < 0) return '—'
  return count === 0 ? 'Görev yok' : `${count} görev`
}

const assignedNames = computed(() => {
  const set = new Set()
  for (const col of columns.value) {
    for (const name of statusesOf(col)) set.add(lower(name))
  }
  return set
})

const unmappedStatuses = computed(() =>
  props.statuses.filter(s => !assignedNames.value.has(lower(s.name)))
)

// ─── Sütun düzenleme ─────────────────────────────────────────────────────────

/**
 * Sütunlar prop olduğu için yerinde değiştirilmez; her işlem yeni bir dizi
 * yayınlar. `statuses` de kopyalanır — aksi halde iki sütun aynı diziyi
 * paylaşabilirdi.
 */
function commit(next) {
  emit('update:modelValue', next)
}

function cloneColumns() {
  return columns.value.map(c => ({ ...c, statuses: [...statusesOf(c)] }))
}

function updateColumn(idx, patch) {
  const next = cloneColumns()
  next[idx] = { ...next[idx], ...patch }
  commit(next)
}

function addColumn() {
  commit([...cloneColumns(), { name: '', color: '#6B7280', wipLimit: 0, statuses: [] }])
}

/**
 * Sütun silinince içindeki durumlar eşlenmemişe düşer — sessizce başka bir
 * sütuna dağıtmak, kullanıcının görmediği bir eşleme üretirdi.
 */
function removeColumn(idx) {
  if (columns.value.length <= 1) return
  const next = cloneColumns()
  next.splice(idx, 1)
  commit(next)
}

function moveColumn(idx, delta) {
  const target = idx + delta
  if (target < 0 || target >= columns.value.length) return
  const next = cloneColumns()
  ;[next[idx], next[target]] = [next[target], next[idx]]
  commit(next)
}

// ─── Durum taşıma ────────────────────────────────────────────────────────────

/**
 * Durumu hedef sütunun verilen sırasına taşır. Önce tüm sütunlardan çıkarılır:
 * aynı durum iki sütunda olsaydı görev iki kez sayılırdı.
 *
 * @param {number|typeof UNMAPPED} toColumn hedef sütun; UNMAPPED ise eşleme kaldırılır
 */
function moveStatus(name, toColumn, at = Number.MAX_SAFE_INTEGER) {
  const next = cloneColumns()
  const key = lower(name)

  // Hedef sütunda kaldırılan öğe sayısı kadar ekleme indeksi kayar.
  let shift = 0
  for (let i = 0; i < next.length; i++) {
    const list = next[i].statuses
    const pos = list.findIndex(s => lower(s) === key)
    if (pos < 0) continue
    list.splice(pos, 1)
    if (i === toColumn && pos < at) shift++
  }

  if (toColumn !== UNMAPPED) {
    const list = next[toColumn].statuses
    const index = Math.max(0, Math.min(list.length, at - shift))
    list.splice(index, 0, name)
  }

  commit(next)
}

function unmapStatus(name) {
  moveStatus(name, UNMAPPED)
}

function assignToColumn(name, idx) {
  assignMenuFor.value = null
  moveStatus(name, idx)
}

// ─── Sürükle-bırak ───────────────────────────────────────────────────────────

/** { type: 'status', name } | { type: 'column', index } */
const drag = ref(null)
/** Durum bırakma hedefi: { col, index } */
const statusDrop = ref(null)
/** Sütun bırakma hedefi — kaçıncı sıraya ekleneceği. */
const columnDropAt = ref(null)

const assignMenuFor = ref(null)

function resetDrag() {
  drag.value = null
  statusDrop.value = null
  columnDropAt.value = null
}

function startStatusDrag(event, name) {
  drag.value = { type: 'status', name }
  assignMenuFor.value = null
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', name)
}

function startColumnDrag(event, index) {
  drag.value = { type: 'column', index }
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', columns.value[index]?.name || '')
}

function isHint(colIdx, index) {
  return statusDrop.value?.col === colIdx && statusDrop.value?.index === index
}

/** Kart üzerindeyken: imleç kartın üst yarısındaysa öncesine, altındaysa sonrasına. */
function onCardDragOver(event, colIdx, sIdx) {
  if (drag.value?.type !== 'status') return
  event.preventDefault()
  event.stopPropagation()
  const rect = event.currentTarget.getBoundingClientRect()
  const after = event.clientY > rect.top + rect.height / 2
  statusDrop.value = { col: colIdx, index: after ? sIdx + 1 : sIdx }
}

/** Kartların dışındaki boşluk: sona ekle. */
function onListDragOver(event, colIdx) {
  if (drag.value?.type !== 'status') return
  event.preventDefault()
  statusDrop.value = { col: colIdx, index: statusesOf(columns.value[colIdx]).length }
}

/**
 * Sütun kartının tamamı bırakma hedefi.
 * Sütun sürüklemesinde kartın hangi yanında olduğumuz ekleme sırasını belirler;
 * durum sürüklemesinde başlık/renk gibi liste dışı alanlar da kabul edilir —
 * yoksa kartın üst yarısına bırakmak sessizce hiçbir şey yapmazdı.
 */
function onColumnDragOver(event, colIdx) {
  if (drag.value?.type === 'column') {
    event.preventDefault()
    const rect = event.currentTarget.getBoundingClientRect()
    columnDropAt.value = event.clientX < rect.left + rect.width / 2 ? colIdx : colIdx + 1
  } else if (drag.value?.type === 'status') {
    event.preventDefault()
    if (statusDrop.value?.col !== colIdx) {
      statusDrop.value = { col: colIdx, index: statusesOf(columns.value[colIdx]).length }
    }
  }
}

/** Sütun kartına bırakma — hem durum hem sütun sürüklemesi buraya düşer. */
function onCardDrop(event, colIdx) {
  event.preventDefault()
  const payload = drag.value
  const target = statusDrop.value
  const insertAt = columnDropAt.value
  resetDrag()

  if (payload?.type === 'status') {
    moveStatus(payload.name, colIdx, target?.col === colIdx ? target.index : Number.MAX_SAFE_INTEGER)
  } else if (payload?.type === 'column' && insertAt !== null) {
    reorderColumn(payload.index, insertAt)
  }
}

function reorderColumn(from, to) {
  if (from === to || from + 1 === to) return
  const next = cloneColumns()
  const [moved] = next.splice(from, 1)
  next.splice(from < to ? to - 1 : to, 0, moved)
  commit(next)
}

function onUnmappedDragOver(event) {
  if (drag.value?.type !== 'status') return
  event.preventDefault()
  statusDrop.value = { col: UNMAPPED, index: 0 }
}

function onUnmappedDrop(event) {
  event.preventDefault()
  const payload = drag.value
  resetDrag()
  if (payload?.type === 'status') moveStatus(payload.name, UNMAPPED)
}

// "Sütuna ekle" menüsü dışarı tıklayınca kapansın — açık kalırsa altındaki
// durum kartlarını örter.
function closeAssignMenu(event) {
  if (!event.target.closest?.('[data-assign-menu]')) assignMenuFor.value = null
}
onMounted(() => document.addEventListener('click', closeAssignMenu))
onBeforeUnmount(() => document.removeEventListener('click', closeAssignMenu))
</script>
