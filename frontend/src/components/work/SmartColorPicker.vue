<template>
  <div class="flex items-center gap-2 flex-wrap px-2 py-1.5">
    <label class="text-[11px] text-gray-400 shrink-0">Renklendirme</label>

    <select
      :value="modelValue"
      class="rounded-lg border border-gray-200 px-2 py-1 text-[11px] text-gray-600 focus:outline-none focus:border-purple-400"
      @change="emit('update:modelValue', $event.target.value)"
    >
      <option value="">Öncelik (varsayılan)</option>
      <option v-for="filter in richFilters" :key="filter.id" :value="filter.id">
        {{ filter.name }}
      </option>
    </select>

    <!-- Efsane: kural sırası burada da anlamlı, ilk eşleşen kazanır -->
    <div v-if="legend.length" class="flex items-center gap-x-3 gap-y-1 flex-wrap">
      <span v-for="element in legend" :key="element.id" class="inline-flex items-center gap-1.5 text-[11px]">
        <span class="w-2.5 h-2.5 rounded-sm" :style="{ backgroundColor: element.color || '#94A3B8' }"></span>
        <span class="text-gray-500">{{ element.name }}</span>
      </span>
      <span class="inline-flex items-center gap-1.5 text-[11px]">
        <span class="w-2.5 h-2.5 rounded-sm bg-gray-200"></span>
        <span class="text-gray-400">Sınıflandırılmamış</span>
      </span>
    </div>

    <span v-if="loading" class="text-[11px] text-gray-400">sınıflandırılıyor…</span>

    <router-link
      v-if="modelValue"
      :to="`/rich-filters/${modelValue}`"
      class="text-[11px] text-purple-600 hover:text-purple-700 ml-auto shrink-0"
    >
      Kuralları düzenle
    </router-link>
  </div>
</template>

<script setup>
/**
 * Board/liste renklendirme kaynağı seçici (Ö2).
 *
 * Varsayılan "öncelik": kartların bugünkü davranışı korunur, renklendirme
 * kullanıcının açık tercihiyle devreye girer. Sessizce zengin filtreye geçmek,
 * yıllardır önceliğe göre renk okuyan kullanıcıyı yanıltırdı.
 */
defineProps({
  modelValue: { type: String, default: '' },
  richFilters: { type: Array, default: () => [] },
  legend: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])
</script>
