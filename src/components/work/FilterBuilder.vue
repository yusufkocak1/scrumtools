<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-lg mx-4 p-6">
      <!-- Başlık -->
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-900">Gelişmiş Filtre</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd"
              d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
              clip-rule="evenodd"/>
          </svg>
        </button>
      </div>

      <!-- Koşullar listesi -->
      <div class="space-y-3 mb-4 max-h-72 overflow-y-auto">
        <div
          v-for="(cond, idx) in conditions"
          :key="idx"
          class="flex items-center gap-2"
        >
          <!-- Alan -->
          <select v-model="cond.field" class="flex-1 text-sm rounded-md border border-gray-300 px-2 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none">
            <option v-for="f in FIELDS" :key="f.value" :value="f.value">{{ f.label }}</option>
          </select>
          <!-- Operatör -->
          <select v-model="cond.operator" class="w-28 text-sm rounded-md border border-gray-300 px-2 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none">
            <option v-for="o in opsForField(cond.field)" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
          <!-- Değer -->
          <input
            v-if="!['is_null','is_not_null'].includes(cond.operator)"
            v-model="cond.rawValue"
            placeholder="değer"
            class="flex-1 text-sm rounded-md border border-gray-300 px-2 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
          <!-- Sil -->
          <button @click="removeCondition(idx)" class="text-gray-400 hover:text-red-500">
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd"
                d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
                clip-rule="evenodd"/>
            </svg>
          </button>
        </div>

        <div v-if="conditions.length === 0" class="text-sm text-gray-400 text-center py-4">
          Henüz filtre koşulu eklenmedi
        </div>
      </div>

      <!-- Koşul Ekle -->
      <button
        @click="addCondition"
        class="text-sm text-blue-600 hover:text-blue-800 flex items-center gap-1 mb-5"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
        </svg>
        Koşul ekle
      </button>

      <!-- Butonlar -->
      <div class="flex justify-end gap-3">
        <button @click="clearAll" class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
          Temizle
        </button>
        <button
          @click="applyFilters"
          class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700"
        >
          Uygula
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  isOpen:         { type: Boolean, default: false },
  initialFilters: { type: Array, default: () => [] }
})
const emit = defineEmits(['close', 'apply'])

const FIELDS = [
  { value: 'status',    label: 'Durum' },
  { value: 'priority',  label: 'Öncelik' },
  { value: 'issueType', label: 'Tür' },
  { value: 'assignee',  label: 'Atanan' },
  { value: 'reporter',  label: 'Açan' },
  { value: 'labels',    label: 'Etiket' },
  { value: 'dueDate',   label: 'Son Tarih' },
  { value: 'startDate', label: 'Başl. Tarihi' },
  { value: 'sprintId',  label: 'Sprint ID' },
]

const STR_OPS  = [
  { value: 'eq',         label: 'Eşit' },
  { value: 'neq',        label: 'Eşit Değil' },
  { value: 'in',         label: 'Birine Eşit' },
  { value: 'contains',   label: 'İçeriyor' },
  { value: 'is_null',    label: 'Boş' },
  { value: 'is_not_null',label: 'Dolu' },
]
const DATE_OPS = [
  { value: 'eq',         label: 'Eşit' },
  { value: 'gt',         label: 'Sonra' },
  { value: 'lt',         label: 'Önce' },
  { value: 'is_null',    label: 'Boş' },
  { value: 'is_not_null',label: 'Dolu' },
]
const DATE_FIELDS = ['dueDate', 'startDate']

function opsForField(field) {
  return DATE_FIELDS.includes(field) ? DATE_OPS : STR_OPS
}

const conditions = ref([])

watch(() => props.isOpen, (open) => {
  if (open) {
    conditions.value = props.initialFilters.map(f => ({
      field:    f.field,
      operator: f.operator,
      rawValue: (f.values || []).join(', ')
    }))
  }
})

function addCondition() {
  conditions.value.push({ field: 'status', operator: 'eq', rawValue: '' })
}

function removeCondition(idx) {
  conditions.value.splice(idx, 1)
}

function clearAll() {
  conditions.value = []
}

function applyFilters() {
  const filters = conditions.value
    .filter(c => c.field && c.operator)
    .map(c => ({
      field:    c.field,
      operator: c.operator,
      values:   ['is_null','is_not_null'].includes(c.operator)
                  ? []
                  : (c.rawValue || '').split(',').map(s => s.trim()).filter(Boolean)
    }))
  emit('apply', filters)
  emit('close')
}
</script>

