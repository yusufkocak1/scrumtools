<template>
  <div class="space-y-5">
    <div class="flex items-start justify-between gap-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">Board'lar</h2>
        <p class="text-sm text-gray-500 mt-0.5">
          Sütunlar ve her sütunun hangi durumları topladığı. Bir sütun birden fazla
          durum içerebilir; her durumun sütunu olmak zorunda değildir.
        </p>
      </div>
      <button
        class="shrink-0 px-3 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700"
        @click="showCreate = true"
      >
        + Board
      </button>
    </div>

    <div v-if="loading" class="py-10 text-center text-sm text-gray-400">Yükleniyor…</div>

    <div v-else-if="boards.length === 0" class="py-10 text-center text-sm text-gray-400">
      Henüz board oluşturulmamış.
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="board in boards"
        :key="board.id"
        class="rounded-lg border border-gray-200 bg-white"
      >
        <!-- Özet -->
        <div v-if="editingId !== board.id" class="px-4 py-3">
          <div class="flex items-center justify-between gap-3">
            <div class="flex items-center gap-2 min-w-0">
              <span class="font-medium text-gray-900 truncate">{{ board.name }}</span>
              <span
                class="text-[10px] font-semibold px-2 py-0.5 rounded-full"
                :class="board.boardType === 'SCRUM' ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700'"
              >{{ board.boardType }}</span>
              <span v-if="board.isDefault" class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-green-100 text-green-700">
                Varsayılan
              </span>
              <span v-if="board.projectId" class="text-[10px] text-gray-500">📁 {{ projectName(board.projectId) }}</span>
            </div>
            <div class="flex items-center gap-2 shrink-0">
              <button
                class="px-2 py-1 text-xs text-gray-600 border border-gray-200 rounded hover:border-blue-300 hover:text-blue-600"
                @click="startEdit(board)"
              >Düzenle</button>
              <button
                v-if="!board.isDefault"
                class="px-2 py-1 text-xs text-gray-600 border border-gray-200 rounded hover:border-green-300 hover:text-green-600"
                :disabled="saving"
                @click="setDefault(board)"
              >Varsayılan Yap</button>
              <button
                class="px-2 py-1 text-xs text-red-500 border border-red-200 rounded hover:border-red-400 hover:text-red-700"
                @click="deletingBoard = board"
              >Sil</button>
            </div>
          </div>
          <div class="mt-2 flex flex-wrap gap-1.5">
            <span
              v-for="col in boardColumns(board)"
              :key="col.name"
              class="inline-flex items-center gap-1.5 text-[11px] rounded-full border border-gray-200 px-2 py-0.5 text-gray-600"
              :title="`Durumlar: ${columnStatuses(col).join(', ')}`"
            >
              <span class="w-1.5 h-1.5 rounded-full" :style="{ backgroundColor: col.color || '#6B7280' }"></span>
              {{ col.name }}
              <span v-if="columnStatuses(col).length > 1" class="text-gray-400">({{ columnStatuses(col).length }})</span>
            </span>
          </div>
        </div>

        <!-- Düzenleme -->
        <div v-else class="px-4 py-4 space-y-4 bg-gray-50/60">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Board Adı</label>
              <input
                v-model="form.name"
                class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Tür</label>
              <select
                v-model="form.boardType"
                class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="KANBAN">Kanban</option>
                <option value="SCRUM">Scrum</option>
              </select>
            </div>
          </div>

          <!-- Sütunlar + durum eşleme -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <label class="text-xs font-medium text-gray-700">Sütunlar ve Durum Eşlemesi</label>
              <button class="text-xs text-blue-600 hover:text-blue-800" @click="addColumn">+ Sütun Ekle</button>
            </div>

            <div class="space-y-2">
              <div
                v-for="(col, idx) in form.columns"
                :key="idx"
                class="rounded-lg border border-gray-200 bg-white p-3 space-y-2.5"
              >
                <div class="flex items-center gap-2">
                  <input
                    v-model="col.name"
                    class="flex-1 min-w-0 text-sm rounded border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-blue-500 focus:outline-none"
                    placeholder="Sütun adı"
                  />
                  <input type="color" v-model="col.color" class="w-8 h-8 rounded border border-gray-300 cursor-pointer shrink-0" />
                  <input
                    type="number"
                    v-model.number="col.wipLimit"
                    min="0"
                    class="w-16 text-xs rounded border border-gray-300 px-2 py-1.5 shrink-0"
                    placeholder="WIP"
                    title="WIP limiti (0 = sınırsız)"
                  />
                  <button
                    class="p-1 text-gray-400 hover:text-gray-700 disabled:opacity-30 shrink-0"
                    :disabled="idx === 0"
                    title="Sola taşı"
                    @click="moveColumn(idx, -1)"
                  >◀</button>
                  <button
                    class="p-1 text-gray-400 hover:text-gray-700 disabled:opacity-30 shrink-0"
                    :disabled="idx === form.columns.length - 1"
                    title="Sağa taşı"
                    @click="moveColumn(idx, 1)"
                  >▶</button>
                  <button
                    class="p-1 text-red-400 hover:text-red-600 disabled:opacity-30 shrink-0"
                    :disabled="form.columns.length <= 1"
                    title="Sütunu sil"
                    @click="form.columns.splice(idx, 1)"
                  >✕</button>
                </div>

                <!-- Durum seçimi -->
                <div class="flex flex-wrap gap-1.5">
                  <button
                    v-for="status in statuses"
                    :key="status.id"
                    type="button"
                    class="text-[11px] rounded-full border px-2 py-1 transition"
                    :class="isAssigned(col, status.name)
                      ? 'border-blue-400 bg-blue-50 text-blue-700 font-medium'
                      : ownerOf(status.name, idx)
                        ? 'border-gray-200 bg-gray-50 text-gray-300 cursor-not-allowed'
                        : 'border-gray-200 text-gray-500 hover:border-blue-300 hover:text-blue-600'"
                    :disabled="!isAssigned(col, status.name) && !!ownerOf(status.name, idx)"
                    :title="ownerOf(status.name, idx)
                      ? `Bu durum '${ownerOf(status.name, idx)}' sütununa atanmış`
                      : ''"
                    @click="toggleStatus(col, status.name)"
                  >
                    <span class="inline-block w-1.5 h-1.5 rounded-full mr-1 align-middle"
                          :style="{ backgroundColor: status.color || '#6B7280' }"></span>
                    {{ status.name }}
                  </button>
                </div>
                <p v-if="columnStatuses(col).length === 0" class="text-[11px] text-amber-700">
                  Durum seçilmedi — sütun boş kalır.
                </p>
              </div>
            </div>

            <!-- Board dışında kalan durumlar -->
            <p v-if="outsideBoard.length > 0" class="mt-2 text-[11px] text-gray-500">
              Board'da gösterilmeyen durumlar:
              <span class="font-medium text-gray-600">{{ outsideBoard.join(', ') }}</span>.
              Bu durumdaki görevler ilk sütunda toplanır.
            </p>
          </div>

          <p v-if="formError" class="text-xs text-red-600">{{ formError }}</p>

          <div class="flex justify-end gap-2">
            <button class="px-3 py-1.5 text-xs text-gray-600 border border-gray-300 rounded-md" @click="editingId = null">
              İptal
            </button>
            <button
              class="px-3 py-1.5 text-xs font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
              :disabled="saving"
              @click="saveEdit"
            >
              {{ saving ? 'Kaydediliyor…' : 'Kaydet' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Yeni board -->
    <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="showCreate = false">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">Yeni Board</h3>
        <div class="space-y-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Board Adı</label>
            <input
              v-model="newBoard.name"
              placeholder="ör. Sprint Board"
              class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Tür</label>
            <select
              v-model="newBoard.boardType"
              class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              <option value="KANBAN">Kanban</option>
              <option value="SCRUM">Scrum</option>
            </select>
            <p class="text-xs text-gray-400 mt-1">
              {{ newBoard.boardType === 'SCRUM'
                ? 'Scrum board aktif sprint üzerinden çalışır.'
                : 'Kanban board tüm görevleri sütunlarda gösterir.' }}
            </p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Proje (opsiyonel)</label>
            <select
              v-model="newBoard.projectId"
              class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              <option :value="null">— Takım geneli —</option>
              <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }} ({{ p.key }})</option>
            </select>
          </div>
          <p class="text-xs text-gray-500">
            Sütunlar iş akışındaki durumlardan otomatik oluşturulur; sonrasında birleştirebilirsiniz.
          </p>
        </div>
        <div class="flex justify-end gap-2 mt-5">
          <button class="px-4 py-2 text-sm text-gray-600" @click="showCreate = false">İptal</button>
          <button
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
            :disabled="!newBoard.name.trim() || saving"
            @click="handleCreate"
          >Oluştur</button>
        </div>
      </div>
    </div>

    <!-- Silme onayı -->
    <div v-if="deletingBoard" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="deletingBoard = null">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-sm mx-4 p-6">
        <h3 class="font-semibold text-gray-900 mb-2">Board'u Sil</h3>
        <p class="text-sm text-gray-600 mb-4">
          <strong>{{ deletingBoard.name }}</strong> silinecek. Görevler etkilenmez, yalnızca
          bu görünüm kaldırılır.
        </p>
        <div class="flex justify-end gap-2">
          <button class="px-4 py-2 text-sm text-gray-600" @click="deletingBoard = null">İptal</button>
          <button
            class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50"
            :disabled="saving"
            @click="handleDelete"
          >{{ saving ? 'Siliniyor…' : 'Sil' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Board yönetimi — sütunlar ve durum eşlemesi.
 *
 * Eskiden sütun adı doğrudan durum adı sayılıyordu; artık sütun bir durum kümesi
 * topluyor. Bir durum yalnızca tek sütuna atanabilir (aksi halde aynı görev iki
 * sütunda görünürdü), bu yüzden başka sütuna atanmış durumlar pasif gösterilir.
 */
import { ref, computed, onMounted, watch } from 'vue'
import { getBoards, createBoard, updateBoard, deleteBoard } from '../../api/BoardApi.js'
import { useTaskStatuses } from '../../composables/useTaskStatuses.js'
import { columnStatuses, statusesOutsideBoard } from '../../utils/boardColumns.js'

const props = defineProps({
  teamId: { type: String, required: true },
  projectId: { type: String, default: null },
  projects: { type: Array, default: () => [] },
})

const emit = defineEmits(['changed'])

const { statuses, statusNames } = useTaskStatuses(() => props.teamId, () => props.projectId)

const boards = ref([])
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formError = ref(null)
const showCreate = ref(false)
const deletingBoard = ref(null)

const form = ref({ name: '', boardType: 'KANBAN', columns: [] })
const newBoard = ref({ name: '', boardType: 'KANBAN', projectId: null })

function boardColumns(board) {
  return board?.columnConfig?.columns || []
}

function projectName(projectId) {
  return props.projects.find(p => p.id === projectId)?.name || 'Proje'
}

// ─── Durum ↔ sütun eşleme yardımcıları ───────────────────────────────────────

function isAssigned(col, statusName) {
  return columnStatuses(col).some(s => s.toLowerCase() === statusName.toLowerCase())
}

/** Durum başka bir sütuna atanmışsa o sütunun adı; değilse null. */
function ownerOf(statusName, exceptIndex) {
  const key = statusName.toLowerCase()
  const owner = form.value.columns.find((c, i) =>
    i !== exceptIndex && columnStatuses(c).some(s => s.toLowerCase() === key)
  )
  return owner?.name || null
}

function toggleStatus(col, statusName) {
  const list = Array.isArray(col.statuses) ? col.statuses : columnStatuses(col)
  const key = statusName.toLowerCase()
  const idx = list.findIndex(s => s.toLowerCase() === key)
  if (idx >= 0) list.splice(idx, 1)
  else list.push(statusName)
  col.statuses = list
}

const outsideBoard = computed(() =>
  statusesOutsideBoard(form.value.columns, statusNames.value)
)

// ─── CRUD ────────────────────────────────────────────────────────────────────

async function load() {
  if (!props.teamId) return
  loading.value = true
  try {
    boards.value = await getBoards(props.teamId)
  } catch (e) {
    console.error('Board listesi alınamadı:', e)
  } finally {
    loading.value = false
  }
}

function startEdit(board) {
  editingId.value = board.id
  formError.value = null
  form.value = {
    name: board.name,
    boardType: board.boardType,
    // statuses tanımsız eski board'lar sütun adıyla eşleşiyordu; düzenlemeye
    // açarken bu örtük eşlemeyi görünür hâle getiriyoruz.
    columns: boardColumns(board).map(c => ({
      name: c.name,
      color: c.color || '#6B7280',
      wipLimit: c.wipLimit || 0,
      statuses: [...columnStatuses(c)],
    })),
  }
}

function addColumn() {
  form.value.columns.push({ name: '', color: '#6B7280', wipLimit: 0, statuses: [] })
}

function moveColumn(index, delta) {
  const target = index + delta
  if (target < 0 || target >= form.value.columns.length) return
  const cols = form.value.columns
  ;[cols[index], cols[target]] = [cols[target], cols[index]]
}

async function saveEdit() {
  const columns = form.value.columns
  if (columns.some(c => !c.name.trim())) {
    formError.value = 'Sütun adı boş bırakılamaz.'
    return
  }
  const names = columns.map(c => c.name.trim().toLowerCase())
  if (new Set(names).size !== names.length) {
    formError.value = 'Aynı isimde iki sütun olamaz.'
    return
  }

  saving.value = true
  formError.value = null
  try {
    await updateBoard(props.teamId, editingId.value, {
      name: form.value.name,
      boardType: form.value.boardType,
      columnConfig: {
        columns: columns.map(c => ({
          name: c.name.trim(),
          color: c.color,
          wipLimit: c.wipLimit || 0,
          statuses: c.statuses || [],
        })),
      },
    })
    editingId.value = null
    await load()
    emit('changed')
  } catch (e) {
    formError.value = e.response?.data?.error || 'Board kaydedilemedi.'
  } finally {
    saving.value = false
  }
}

async function setDefault(board) {
  saving.value = true
  try {
    await updateBoard(props.teamId, board.id, { isDefault: true })
    await load()
    emit('changed')
  } catch (e) {
    console.error('Varsayılan yapılamadı:', e)
  } finally {
    saving.value = false
  }
}

async function handleCreate() {
  saving.value = true
  try {
    await createBoard(props.teamId, {
      name: newBoard.value.name.trim(),
      boardType: newBoard.value.boardType,
      projectId: newBoard.value.projectId || undefined,
    })
    showCreate.value = false
    newBoard.value = { name: '', boardType: 'KANBAN', projectId: props.projectId }
    await load()
    emit('changed')
  } catch (e) {
    console.error('Board oluşturulamadı:', e)
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!deletingBoard.value) return
  saving.value = true
  try {
    await deleteBoard(props.teamId, deletingBoard.value.id)
    deletingBoard.value = null
    await load()
    emit('changed')
  } catch (e) {
    console.error('Board silinemedi:', e)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  newBoard.value.projectId = props.projectId
  load()
})
watch(() => props.teamId, () => { editingId.value = null; load() })
</script>
