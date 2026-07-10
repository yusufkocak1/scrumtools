<template>
  <teleport to="body">
    <div class="fixed inset-0 z-[60] flex items-center justify-center bg-black/40" @click.self="$emit('cancel')">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
        <div class="px-6 py-4 border-b flex items-center justify-between">
          <h3 class="font-semibold text-gray-800">{{ title }}</h3>
          <button @click="$emit('cancel')" class="text-gray-400 hover:text-gray-600 text-xl">✕</button>
        </div>
        <div class="p-6">
          <label v-if="label" class="block text-sm font-medium text-gray-700 mb-1">
            {{ label }}
            <span v-if="optional" class="text-gray-400 font-normal">(isteğe bağlı)</span>
          </label>
          <input ref="inputEl" v-model="value" type="text" :placeholder="placeholder"
                 class="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-300 focus:border-indigo-400"
                 @keyup.enter="submit"/>
          <p v-if="hint" class="text-xs text-gray-400 mt-1.5">{{ hint }}</p>
        </div>
        <div class="px-6 py-4 bg-gray-50 flex justify-end gap-2">
          <button @click="$emit('cancel')"
                  class="px-4 py-2 text-sm text-gray-600 hover:bg-gray-200 rounded-lg transition">
            {{ cancelText }}
          </button>
          <button @click="submit" :disabled="!optional && !value.trim()"
                  class="px-4 py-2 text-sm bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-medium rounded-lg transition">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount} from 'vue'

const props = defineProps({
  title: {type: String, required: true},
  label: {type: String, default: ''},
  placeholder: {type: String, default: ''},
  initialValue: {type: String, default: ''},
  confirmText: {type: String, default: 'Tamam'},
  cancelText: {type: String, default: 'İptal'},
  hint: {type: String, default: ''},
  /** true ise boş değerle de onaylanabilir */
  optional: {type: Boolean, default: false}
})

const emit = defineEmits(['confirm', 'cancel'])

const value = ref(props.initialValue)
const inputEl = ref(null)

function submit() {
  if (!props.optional && !value.value.trim()) return
  emit('confirm', value.value.trim())
}

function onKeydown(e) {
  if (e.key === 'Escape') emit('cancel')
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  inputEl.value?.focus()
})
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>
