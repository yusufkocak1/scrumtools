<template>
  <div class="space-y-3">
    <div
      v-for="field in visibleFields"
      :key="field.id"
      class="flex flex-col gap-1"
    >
      <label class="text-xs font-medium text-gray-600">
        {{ field.name }}
        <span v-if="field.isRequired" class="text-red-500 ml-0.5">*</span>
      </label>

      <!-- TEXT -->
      <input
        v-if="field.fieldType === 'TEXT' || field.fieldType === 'URL'"
        :type="field.fieldType === 'URL' ? 'url' : 'text'"
        :value="modelValue[field.fieldKey]"
        @input="update(field.fieldKey, $event.target.value)"
        :placeholder="field.defaultValue || ''"
        class="text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-blue-500"
      />

      <!-- NUMBER -->
      <input
        v-else-if="field.fieldType === 'NUMBER'"
        type="number"
        :value="modelValue[field.fieldKey]"
        @input="update(field.fieldKey, +$event.target.value)"
        class="text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-blue-500"
      />

      <!-- DATE -->
      <input
        v-else-if="field.fieldType === 'DATE'"
        type="date"
        :value="modelValue[field.fieldKey]"
        @input="update(field.fieldKey, $event.target.value)"
        class="text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-blue-500"
      />

      <!-- CHECKBOX -->
      <label v-else-if="field.fieldType === 'CHECKBOX'" class="flex items-center gap-2 cursor-pointer">
        <input
          type="checkbox"
          :checked="modelValue[field.fieldKey]"
          @change="update(field.fieldKey, $event.target.checked)"
          class="w-4 h-4 rounded"
        />
        <span class="text-sm text-gray-700">{{ field.name }}</span>
      </label>

      <!-- SELECT -->
      <select
        v-else-if="field.fieldType === 'SELECT'"
        :value="modelValue[field.fieldKey]"
        @change="update(field.fieldKey, $event.target.value)"
        class="text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-blue-500"
      >
        <option value="">Seçiniz...</option>
        <option
          v-for="opt in getOptions(field)"
          :key="opt"
          :value="opt"
        >{{ opt }}</option>
      </select>

      <!-- MULTI_SELECT -->
      <div v-else-if="field.fieldType === 'MULTI_SELECT'" class="flex flex-wrap gap-1.5">
        <label
          v-for="opt in getOptions(field)"
          :key="opt"
          class="flex items-center gap-1.5 cursor-pointer"
        >
          <input
            type="checkbox"
            :checked="(modelValue[field.fieldKey] || []).includes(opt)"
            @change="toggleMultiSelect(field.fieldKey, opt, $event.target.checked)"
            class="w-3.5 h-3.5 rounded"
          />
          <span class="text-sm">{{ opt }}</span>
        </label>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  fields:     { type: Array, default: () => [] },
  modelValue: { type: Object, default: () => ({}) },
  issueType:  { type: String, default: null },
})

const emit = defineEmits(['update:modelValue'])

const visibleFields = computed(() =>
  props.fields.filter(f => {
    if (!f.isVisible) return false
    if (props.issueType && f.issueTypes?.length > 0) {
      return f.issueTypes.includes(props.issueType)
    }
    return true
  })
)

function update(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}

function toggleMultiSelect(key, opt, checked) {
  const current = [...(props.modelValue[key] || [])]
  if (checked) {
    if (!current.includes(opt)) current.push(opt)
  } else {
    const idx = current.indexOf(opt)
    if (idx !== -1) current.splice(idx, 1)
  }
  update(key, current)
}

function getOptions(field) {
  return field.options?.options || []
}
</script>

