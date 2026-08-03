<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2">
        <div class="min-w-0">
          <h3 class="text-sm font-semibold text-gray-700 truncate">
            {{ definition?.name || 'Zengin filtre' }}
          </h3>
          <p class="text-[11px] text-gray-400 mt-0.5">
            Buradaki seçim, aynı filtreye bağlı tüm widget'ları daraltır.
          </p>
        </div>
        <button
          v-if="isFiltered"
          class="shrink-0 text-[11px] text-purple-600 hover:text-purple-700"
          @click="clear"
        >
          Temizle
        </button>
      </div>

      <!-- Arama -->
      <input
        :value="text"
        type="text"
        placeholder="Başlık veya görev no ara…"
        class="w-full rounded-lg border border-gray-200 px-3 py-1.5 text-xs focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
        @input="onSearchInput"
      />

      <!-- Akıllı filtre çipleri -->
      <div v-if="smartFilters.length" class="flex flex-wrap gap-1.5">
        <button
          v-for="element in smartFilters"
          :key="element.id"
          class="inline-flex items-center gap-1.5 px-2 py-1 rounded-full border text-[11px] transition-all"
          :class="selectedSmart.includes(element.id)
            ? 'border-transparent text-white shadow-sm'
            : 'border-gray-200 text-gray-600 hover:border-gray-300'"
          :style="selectedSmart.includes(element.id) ? { backgroundColor: element.color || '#6366F1' } : {}"
          @click="toggleSmart(element.id)"
        >
          <span
            v-if="!selectedSmart.includes(element.id)"
            class="w-2.5 h-2.5 rounded-sm"
            :style="{ backgroundColor: element.color || '#94A3B8' }"
          ></span>
          {{ element.name }}
        </button>
      </div>
      <p v-else-if="definition" class="text-[11px] text-gray-400">
        Bu filtrede akıllı filtre tanımlı değil.
        <router-link :to="`/rich-filters/${richFilterId}`" class="text-purple-600 hover:underline">
          Tanımla
        </router-link>
      </p>

      <!-- Toplam -->
      <div class="flex items-center justify-between pt-1 border-t border-gray-50 text-[11px]">
        <span class="text-gray-400">
          {{ loadingCount ? 'Hesaplanıyor…' : `${total} görev` }}
        </span>
        <button class="text-purple-600 hover:text-purple-700" @click="openInWorkList">
          Görevlerde aç
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
/**
 * Zengin filtre kontrolcüsü — seçimlerin görünen yüzü.
 *
 * Durumu kendisi tutmaz; `useRichFilterContext` üzerinden paylaşılan duruma
 * yazar. Bu yüzden bu widget dashboard'da olmasa da grafikten tıklayarak
 * filtreleme çalışmaya devam eder.
 */
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { countRichFilter, resolveRichFilter } from '../../api/RichFilterApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
})

const router = useRouter()
const {
  definition, error, smartFilters, selectedSmart, text, payload, signature, isFiltered,
  loadDefinition, toggleSmart, setText, clear,
} = useRichFilterContext(() => props.richFilterId)

const total = ref(0)
const loadingCount = ref(false)

/** Arama kutusu her tuşta sunucuya gitmesin; seçim durumu 400 ms sonra güncellenir. */
let searchTimer = null
function onSearchInput(event) {
  const value = event.target.value
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => setText(value), 400)
}

async function refreshCount() {
  if (!props.teamId || !props.richFilterId) return
  loadingCount.value = true
  try {
    total.value = await countRichFilter(props.teamId, props.richFilterId, payload.value)
  } catch {
    total.value = 0
  } finally {
    loadingCount.value = false
  }
}

/** Seçimin STQL karşılığını alıp mevcut görev listesi ekranında açar. */
async function openInWorkList() {
  try {
    const { stql } = await resolveRichFilter(props.teamId, props.richFilterId, payload.value)
    router.push({ path: `/workList/${props.teamId}`, query: stql ? { q: stql } : {} })
  } catch {
    router.push(`/workList/${props.teamId}`)
  }
}

onMounted(async () => {
  await loadDefinition(props.teamId)
  refreshCount()
})

watch(signature, refreshCount)
</script>
