<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="$emit('close')">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-2xl mx-4 max-h-[85vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
        <h2 class="text-lg font-semibold text-gray-900">Board Yönetimi</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 transition">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Board listesi -->
      <div class="flex-1 overflow-y-auto px-6 py-4 space-y-3">
        <div v-if="boards.length === 0" class="text-center py-10 text-gray-400 text-sm">
          Henüz board oluşturulmamış.
        </div>

        <div
          v-for="board in boards"
          :key="board.id"
          class="border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition"
        >
          <!-- Görüntüleme modu -->
          <div v-if="editingBoardId !== board.id">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900">{{ board.name }}</span>
                  <span
                    class="text-[10px] font-semibold px-2 py-0.5 rounded-full"
                    :class="board.boardType === 'SCRUM'
                      ? 'bg-blue-100 text-blue-700'
                      : 'bg-purple-100 text-purple-700'"
                  >
                    {{ board.boardType }}
                  </span>
                  <span v-if="board.isDefault" class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-green-100 text-green-700">
                    Varsayılan
                  </span>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <button
                  @click="startEditing(board)"
                  class="text-xs text-gray-500 hover:text-blue-600 px-2 py-1 rounded border border-gray-200 hover:border-blue-300 transition"
                >
                  Düzenle
                </button>
                <button
                  v-if="!board.isDefault"
                  @click="setDefault(board)"
                  class="text-xs text-gray-500 hover:text-green-600 px-2 py-1 rounded border border-gray-200 hover:border-green-300 transition"
                >
                  Varsayılan Yap
                </button>
                <button
                  @click="confirmDelete(board)"
                  class="text-xs text-red-500 hover:text-red-700 px-2 py-1 rounded border border-red-200 hover:border-red-400 transition"
                >
                  Sil
                </button>
              </div>
            </div>
            <div class="mt-2 text-xs text-gray-500">
              <span v-if="board.projectId" class="mr-3">📁 Proje bağlı</span>
              <span>{{ getColumnNames(board) }}</span>
            </div>
          </div>

          <!-- Düzenleme modu -->
          <div v-else class="space-y-3">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs font-medium text-gray-700 mb-1">Board Adı</label>
                <input
                  v-model="editForm.name"
                  class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-700 mb-1">Tür</label>
                <select
                  v-model="editForm.boardType"
                  class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  <option value="KANBAN">Kanban</option>
                  <option value="SCRUM">Scrum</option>
                </select>
              </div>
            </div>

            <!-- Sütun düzenleme -->
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Sütunlar</label>
              <div class="space-y-2">
                <div
                  v-for="(col, idx) in editForm.columns"
                  :key="idx"
                  class="flex items-center gap-2"
                >
                  <input
                    v-model="col.name"
                    class="flex-1 text-xs rounded border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-blue-500 focus:outline-none"
                    placeholder="Sütun adı"
                  />
                  <input
                    type="color"
                    v-model="col.color"
                    class="w-8 h-8 rounded border border-gray-300 cursor-pointer"
                  />
                  <input
                    type="number"
                    v-model.number="col.wipLimit"
                    min="0"
                    class="w-16 text-xs rounded border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-blue-500 focus:outline-none"
                    placeholder="WIP"
                    title="WIP Limit"
                  />
                  <button
                    @click="editForm.columns.splice(idx, 1)"
                    class="text-red-400 hover:text-red-600 p-1"
                    :disabled="editForm.columns.length <= 1"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </div>
              <button
                @click="editForm.columns.push({ name: '', color: '#6B7280', wipLimit: 0 })"
                class="mt-2 text-xs text-blue-600 hover:text-blue-800"
              >
                + Sütun Ekle
              </button>
            </div>

            <div class="flex justify-end gap-2 pt-2">
              <button
                @click="editingBoardId = null"
                class="px-3 py-1.5 text-xs text-gray-600 hover:text-gray-900 border border-gray-300 rounded-md"
              >
                İptal
              </button>
              <button
                @click="saveEdit(board)"
                :disabled="saving"
                class="px-3 py-1.5 text-xs font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {{ saving ? 'Kaydediliyor…' : 'Kaydet' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Silme onay dialogu -->
      <div v-if="deletingBoard" class="absolute inset-0 z-10 flex items-center justify-center bg-black/30 rounded-xl">
        <div class="bg-white rounded-lg shadow-xl p-6 max-w-sm mx-4">
          <h3 class="font-semibold text-gray-900 mb-2">Board'u Sil</h3>
          <p class="text-sm text-gray-600 mb-4">
            <strong>{{ deletingBoard.name }}</strong> board'unu silmek istediğinize emin misiniz? Bu işlem geri alınamaz.
          </p>
          <div class="flex justify-end gap-2">
            <button @click="deletingBoard = null" class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
              İptal
            </button>
            <button
              @click="doDelete"
              :disabled="saving"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50"
            >
              {{ saving ? 'Siliniyor…' : 'Sil' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { updateBoard, deleteBoard } from '../../api/BoardApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  boards: { type: Array, required: true }
})

const emit = defineEmits(['close', 'updated', 'deleted'])

const editingBoardId = ref(null)
const editForm = ref({ name: '', boardType: 'KANBAN', columns: [] })
const deletingBoard = ref(null)
const saving = ref(false)

function getColumnNames(board) {
  const cols = board.columnConfig?.columns
  if (!cols || cols.length === 0) return 'Sütun yok'
  return cols.map(c => c.name).join(' → ')
}

function startEditing(board) {
  editingBoardId.value = board.id
  editForm.value = {
    name: board.name,
    boardType: board.boardType,
    columns: (board.columnConfig?.columns || []).map(c => ({ ...c }))
  }
}

async function saveEdit(board) {
  saving.value = true
  try {
    const updated = await updateBoard(props.teamId, board.id, {
      name: editForm.value.name,
      boardType: editForm.value.boardType,
      columnConfig: { columns: editForm.value.columns }
    })
    editingBoardId.value = null
    emit('updated', updated)
  } catch (e) {
    console.error('Board güncellenemedi:', e)
  } finally {
    saving.value = false
  }
}

async function setDefault(board) {
  saving.value = true
  try {
    const updated = await updateBoard(props.teamId, board.id, { isDefault: true })
    emit('updated', updated)
  } catch (e) {
    console.error('Varsayılan yapılamadı:', e)
  } finally {
    saving.value = false
  }
}

function confirmDelete(board) {
  deletingBoard.value = board
}

async function doDelete() {
  if (!deletingBoard.value) return
  saving.value = true
  try {
    await deleteBoard(props.teamId, deletingBoard.value.id)
    emit('deleted', deletingBoard.value.id)
    deletingBoard.value = null
  } catch (e) {
    console.error('Board silinemedi:', e)
  } finally {
    saving.value = false
  }
}
</script>

