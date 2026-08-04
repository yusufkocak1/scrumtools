<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2">
        <div class="min-w-0">
          <h3 class="text-sm font-semibold text-gray-700 truncate">
            {{ title || definition?.name || 'Zengin filtre' }}
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
          Temizle ({{ activeCount }})
        </button>
      </div>

      <!-- Görünümler: kayıtlı seçim kombinasyonları -->
      <div v-if="views.length" class="flex items-center gap-1.5 flex-wrap">
        <button
          v-for="view in views"
          :key="view.id"
          class="px-2 py-1 rounded-md border text-[11px] transition-colors"
          :class="viewId === view.id
            ? 'border-purple-300 bg-purple-50 text-purple-700'
            : 'border-gray-200 text-gray-600 hover:border-gray-300'"
          @click="applyView(viewId === view.id ? null : view)"
        >
          {{ view.name }}
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

      <!-- Sabit filtreler: yazarın tanımladığı seçenekler -->
      <div v-for="element in staticFilters" :key="element.id" class="space-y-1">
        <span class="text-[10px] font-semibold uppercase tracking-wide text-gray-400">
          {{ element.name }}
        </span>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="option in optionsOf(element)"
            :key="option.id"
            class="px-2 py-1 rounded-md border text-[11px] transition-colors"
            :class="staticSelections[element.id] === option.id
              ? 'border-purple-300 bg-purple-50 text-purple-700'
              : 'border-gray-200 text-gray-600 hover:border-gray-300'"
            @click="setStatic(element.id, option.id)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <!-- Dinamik filtreler: seçenekler sonuç kümesinden gelir -->
      <div v-for="control in dynamicControls" :key="control.elementId" class="space-y-1">
        <div class="flex items-center justify-between">
          <span class="text-[10px] font-semibold uppercase tracking-wide text-gray-400">
            {{ control.name }}
          </span>
          <button
            v-if="dynamicSelections[control.elementId]?.length"
            class="text-[10px] text-gray-400 hover:text-purple-600"
            @click="clearDynamic(control.elementId)"
          >
            sıfırla
          </button>
        </div>

        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="option in control.options"
            :key="option.key || 'empty'"
            class="inline-flex items-center gap-1 px-2 py-1 rounded-md border text-[11px] transition-colors"
            :class="isDynamicSelected(control.elementId, option.key)
              ? 'border-purple-300 bg-purple-50 text-purple-700'
              : 'border-gray-200 text-gray-600 hover:border-gray-300'"
            @click="toggleDynamic(control.elementId, option.key)"
          >
            {{ option.label }}
            <span class="text-gray-400 tabular-nums">{{ option.value }}</span>
          </button>

          <span v-if="!control.options.length" class="text-[11px] text-gray-400">
            Bu seçimde değer yok.
          </span>
        </div>
      </div>

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

      <!-- Toplam + işlemler -->
      <div class="flex items-center justify-between pt-1 border-t border-gray-50 text-[11px]">
        <span class="text-gray-400">
          {{ loadingCount ? 'Hesaplanıyor…' : `${total} görev` }}
        </span>
        <div class="flex items-center gap-3">
          <button
            v-if="definition?.owned && isFiltered"
            class="text-gray-500 hover:text-purple-600"
            @click="saveAsView"
          >
            Görünüm kaydet
          </button>
          <button class="text-purple-600 hover:text-purple-700" @click="openInWorkList">
            Görevlerde aç
          </button>
        </div>
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
 *
 * Dinamik filtrelerin seçenekleri sunucudan gelir ve seçim değiştikçe yenilenir:
 * listede yalnız o an gerçekten var olan değerler görünür.
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import {
  countRichFilter, resolveRichFilter, optionsRichFilter, addRichFilterElement,
} from '../../api/RichFilterApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  refreshInterval: { type: Number, default: 0 },
})

const router = useRouter()
const {
  definition, error, smartFilters, staticFilters, dynamicFilters, views,
  selectedSmart, text, staticSelections, dynamicSelections, viewId,
  payload, signature, isFiltered, activeCount,
  loadDefinition, toggleSmart, setText, setStatic, toggleDynamic, clearDynamic,
  applyView, clear, currentSelectionSnapshot,
} = useRichFilterContext(() => props.richFilterId)

const total = ref(0)
const loadingCount = ref(false)
const dynamicControls = ref([])

const optionsOf = (element) => element.config?.options ?? []

const isDynamicSelected = computed(() => (elementId, value) =>
  (dynamicSelections.value[elementId] ?? []).includes(value)
)

/** Arama kutusu her tuşta sunucuya gitmesin; seçim durumu 400 ms sonra güncellenir. */
let searchTimer = null
function onSearchInput(event) {
  const value = event.target.value
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => setText(value), 400)
}

async function refresh() {
  if (!props.teamId || !props.richFilterId) return
  loadingCount.value = true
  try {
    total.value = await countRichFilter(props.teamId, props.richFilterId, payload.value)
  } catch {
    total.value = 0
  } finally {
    loadingCount.value = false
  }

  if (!dynamicFilters.value.length) {
    dynamicControls.value = []
    return
  }
  try {
    dynamicControls.value = await optionsRichFilter(props.teamId, props.richFilterId, payload.value)
  } catch {
    dynamicControls.value = []
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

/**
 * Mevcut seçimi görünüm olarak kaydeder. Yalnız filtrenin sahibine açıktır —
 * görünüm zengin filtrenin bir parçasıdır, herkesin ekleyebilmesi paylaşılan
 * tanımı kalabalıklaştırırdı.
 */
async function saveAsView() {
  const name = prompt('Görünüm adı:')
  if (!name?.trim()) return

  try {
    await addRichFilterElement(props.teamId, props.richFilterId, {
      kind: 'VIEW',
      name: name.trim(),
      config: { selection: currentSelectionSnapshot(), default: false },
    })
    await loadDefinition(props.teamId, { force: true })
  } catch (e) {
    console.error('Görünüm kaydedilemedi', e)
  }
}

onMounted(async () => {
  await loadDefinition(props.teamId)
  refresh()
})

watch(signature, refresh)
useAutoRefresh(refresh, () => props.refreshInterval)
</script>
