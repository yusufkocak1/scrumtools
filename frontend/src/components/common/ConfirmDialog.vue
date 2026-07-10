<template>
  <teleport to="body">
    <div class="fixed inset-0 z-[60] flex items-center justify-center bg-black/40" @click.self="$emit('cancel')">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-sm mx-4 overflow-hidden">
        <div class="p-6">
          <div class="flex items-start gap-4">
            <div :class="['w-10 h-10 rounded-full flex items-center justify-center text-lg shrink-0', iconBgClass]">
              {{ icon }}
            </div>
            <div class="min-w-0">
              <h3 class="font-semibold text-gray-800">{{ title }}</h3>
              <p class="text-sm text-gray-500 mt-1 break-words">{{ message }}</p>
            </div>
          </div>
        </div>
        <div class="px-6 py-4 bg-gray-50 flex justify-end gap-2">
          <button @click="$emit('cancel')"
                  class="px-4 py-2 text-sm text-gray-600 hover:bg-gray-200 rounded-lg transition">
            {{ cancelText }}
          </button>
          <button ref="confirmBtn" @click="$emit('confirm')"
                  :class="['px-4 py-2 text-sm text-white font-medium rounded-lg transition', confirmBtnClass]">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import {ref, computed, onMounted, onBeforeUnmount} from 'vue'

const props = defineProps({
  title: {type: String, default: 'Emin misiniz?'},
  message: {type: String, default: ''},
  confirmText: {type: String, default: 'Onayla'},
  cancelText: {type: String, default: 'İptal'},
  /** 'danger' | 'warning' | 'primary' */
  variant: {type: String, default: 'danger'}
})

const emit = defineEmits(['confirm', 'cancel'])

const confirmBtn = ref(null)

const icon = computed(() => ({danger: '🗑️', warning: '⚠️', primary: '❔'}[props.variant] || '❔'))

const iconBgClass = computed(() => ({
  danger: 'bg-red-100',
  warning: 'bg-amber-100',
  primary: 'bg-indigo-100'
}[props.variant] || 'bg-indigo-100'))

const confirmBtnClass = computed(() => ({
  danger: 'bg-red-600 hover:bg-red-700',
  warning: 'bg-amber-500 hover:bg-amber-600',
  primary: 'bg-indigo-600 hover:bg-indigo-700'
}[props.variant] || 'bg-indigo-600 hover:bg-indigo-700'))

function onKeydown(e) {
  if (e.key === 'Escape') emit('cancel')
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  confirmBtn.value?.focus()
})
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>
