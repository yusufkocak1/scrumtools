<template>
  <div class="relative" ref="rootEl">
    <!-- Tetikleyici -->
    <button
      class="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-xs text-gray-600 shadow-sm transition-all hover:border-blue-300 hover:bg-blue-50/50 hover:text-blue-700 whitespace-nowrap"
      @click="toggleMenu"
    >
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.196-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/>
      </svg>
      <span>{{ activeFilterName || 'Kayıtlı Filtreler' }}</span>
      <svg class="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
      </svg>
    </button>

    <!-- Liste -->
    <div
      v-if="isOpen"
      class="absolute right-0 z-40 mt-1 w-72 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg"
    >
      <div class="max-h-72 overflow-y-auto">
        <p v-if="isLoading" class="px-3 py-4 text-center text-xs text-gray-400">Yükleniyor…</p>

        <p v-else-if="!filters.length" class="px-3 py-4 text-center text-xs text-gray-400">
          Henüz kayıtlı filtre yok.<br>
          <span class="text-[11px]">Bir sorgu kurup "Kaydet" ile saklayabilirsiniz.</span>
        </p>

        <div v-else>
          <div
            v-for="f in filters"
            :key="f.id"
            class="group flex items-center gap-1.5 border-b border-gray-50 px-2 py-2 last:border-0 hover:bg-gray-50"
          >
            <button
              class="shrink-0 p-0.5 transition-colors"
              :class="f.favorite ? 'text-amber-400 hover:text-amber-500' : 'text-gray-300 hover:text-amber-400'"
              :title="f.favorite ? 'Favorilerden çıkar' : 'Favorilere ekle'"
              @click.stop="toggleFavorite(f)"
            >
              <svg class="w-3.5 h-3.5" :fill="f.favorite ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.196-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/>
              </svg>
            </button>

            <button class="min-w-0 flex-1 text-left" @click="select(f)">
              <p class="truncate text-xs font-medium text-gray-800">{{ f.name }}</p>
              <p class="truncate font-mono text-[10px] text-gray-400">{{ f.query || 'tüm görevler' }}</p>
            </button>

            <span
              v-if="f.visibility !== 'PRIVATE'"
              class="shrink-0 rounded px-1.5 py-0.5 text-[9px] font-medium"
              :class="f.visibility === 'TEAM' ? 'bg-blue-50 text-blue-600' : 'bg-purple-50 text-purple-600'"
              :title="f.visibility === 'TEAM' ? 'Takımla paylaşıldı' : 'Projeyle paylaşıldı'"
            >
              {{ f.visibility === 'TEAM' ? 'Takım' : 'Proje' }}
            </span>

            <button
              v-if="f.owned"
              class="shrink-0 p-0.5 text-gray-300 opacity-0 transition-opacity hover:text-red-500 group-hover:opacity-100"
              title="Sil"
              @click.stop="remove(f)"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Kaydet -->
      <div class="border-t border-gray-100 bg-gray-50/50 p-2">
        <button
          class="w-full rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
          :disabled="!currentQuery?.trim()"
          :title="!currentQuery?.trim() ? 'Kaydetmek için önce bir sorgu kurun' : ''"
          @click="openSaveDialog"
        >
          Bu sorguyu kaydet
        </button>
        <button
          v-if="activeFilter?.owned"
          class="mt-1.5 w-full rounded-md border border-gray-200 bg-white px-3 py-1.5 text-xs text-gray-600 transition-colors hover:bg-gray-50"
          @click="updateActive"
        >
          "{{ activeFilter.name }}" filtresini güncelle
        </button>
      </div>
    </div>

    <!-- Kaydetme kutusu -->
    <div v-if="showSaveDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
        <h3 class="mb-4 text-base font-semibold text-gray-900">Filtreyi Kaydet</h3>

        <label class="mb-1 block text-xs font-medium text-gray-600">Ad</label>
        <input
          v-model="form.name"
          placeholder="Örn: Bana atanmış açık işler"
          class="mb-3 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          @keydown.enter="save"
        />

        <label class="mb-1 block text-xs font-medium text-gray-600">Açıklama (opsiyonel)</label>
        <input
          v-model="form.description"
          class="mb-3 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
        />

        <label class="mb-1 block text-xs font-medium text-gray-600">Görünürlük</label>
        <select
          v-model="form.visibility"
          class="mb-3 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
        >
          <option value="PRIVATE">Yalnız ben</option>
          <option value="TEAM">Takımım görsün</option>
          <option value="PROJECT" :disabled="!projectId">
            Projedeki herkes görsün{{ projectId ? '' : ' (önce proje seçin)' }}
          </option>
        </select>

        <p class="mb-4 rounded-md bg-gray-50 px-3 py-2 font-mono text-[11px] text-gray-500 break-all">
          {{ currentQuery || 'tüm görevler' }}
        </p>

        <p v-if="saveError" class="mb-3 text-xs text-red-600">{{ saveError }}</p>

        <div class="flex justify-end gap-2">
          <button class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900" @click="showSaveDialog = false">
            Vazgeç
          </button>
          <button
            class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:bg-gray-300"
            :disabled="!form.name.trim() || isSaving"
            @click="save"
          >
            {{ isSaving ? 'Kaydediliyor…' : 'Kaydet' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Kayıtlı filtre menüsü.
 *
 * Bir filtre seçildiğinde sorgu metni yukarı iletilir; çalıştırma sorumluluğu
 * üst bileşendedir (aynı sorgu hem board hem liste görünümünü besleyebilsin diye).
 */
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import {
  getSavedFilters, createSavedFilter, updateSavedFilter,
  deleteSavedFilter, setSavedFilterFavorite
} from '../../api/SavedFilterApi.js'

const props = defineProps({
  teamId:       { type: String, required: true },
  projectId:    { type: String, default: null },
  currentQuery: { type: String, default: '' },
})

const emit = defineEmits(['select'])

const rootEl = ref(null)
const isOpen = ref(false)
const isLoading = ref(false)
const filters = ref([])

const activeFilterId = ref(null)
const activeFilter = computed(() => filters.value.find(f => f.id === activeFilterId.value) || null)
const activeFilterName = computed(() => activeFilter.value?.name || null)

const showSaveDialog = ref(false)
const isSaving = ref(false)
const saveError = ref(null)
const form = ref({ name: '', description: '', visibility: 'PRIVATE' })

// Sorgu elle değiştirilirse artık kayıtlı filtre "aktif" sayılmaz.
watch(() => props.currentQuery, (q) => {
  if (activeFilter.value && activeFilter.value.query !== q) {
    activeFilterId.value = null
  }
})

watch(() => [props.teamId, props.projectId], () => {
  if (isOpen.value) load()
})

onMounted(() => document.addEventListener('click', onDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick))

function onDocumentClick(e) {
  if (isOpen.value && rootEl.value && !rootEl.value.contains(e.target)) {
    isOpen.value = false
  }
}

function toggleMenu() {
  isOpen.value = !isOpen.value
  if (isOpen.value) load()
}

async function load() {
  isLoading.value = true
  try {
    filters.value = await getSavedFilters(props.teamId, props.projectId)
  } catch (e) {
    console.error('Kayıtlı filtreler yüklenemedi:', e)
    filters.value = []
  } finally {
    isLoading.value = false
  }
}

function select(filter) {
  activeFilterId.value = filter.id
  isOpen.value = false
  emit('select', filter)
}

async function toggleFavorite(filter) {
  try {
    const updated = await setSavedFilterFavorite(props.teamId, filter.id, !filter.favorite)
    const idx = filters.value.findIndex(f => f.id === filter.id)
    if (idx >= 0) filters.value[idx] = updated
  } catch (e) {
    console.error('Favori güncellenemedi:', e)
  }
}

async function remove(filter) {
  if (!confirm(`"${filter.name}" filtresi silinsin mi?`)) return
  try {
    await deleteSavedFilter(props.teamId, filter.id)
    filters.value = filters.value.filter(f => f.id !== filter.id)
    if (activeFilterId.value === filter.id) activeFilterId.value = null
  } catch (e) {
    console.error('Filtre silinemedi:', e)
  }
}

function openSaveDialog() {
  form.value = { name: '', description: '', visibility: 'PRIVATE' }
  saveError.value = null
  showSaveDialog.value = true
  isOpen.value = false
}

async function save() {
  if (!form.value.name.trim()) return
  isSaving.value = true
  saveError.value = null
  try {
    const created = await createSavedFilter(props.teamId, {
      name: form.value.name.trim(),
      description: form.value.description || null,
      query: props.currentQuery || '',
      visibility: form.value.visibility,
      projectId: props.projectId,
    })
    filters.value = [created, ...filters.value]
    activeFilterId.value = created.id
    showSaveDialog.value = false
  } catch (e) {
    saveError.value = e?.response?.data?.error || 'Filtre kaydedilemedi.'
  } finally {
    isSaving.value = false
  }
}

async function updateActive() {
  if (!activeFilter.value) return
  try {
    const updated = await updateSavedFilter(props.teamId, activeFilter.value.id, {
      name: activeFilter.value.name,
      description: activeFilter.value.description,
      query: props.currentQuery || '',
      visibility: activeFilter.value.visibility,
      projectId: activeFilter.value.projectId || props.projectId,
    })
    const idx = filters.value.findIndex(f => f.id === updated.id)
    if (idx >= 0) filters.value[idx] = updated
    isOpen.value = false
  } catch (e) {
    console.error('Filtre güncellenemedi:', e)
  }
}
</script>
