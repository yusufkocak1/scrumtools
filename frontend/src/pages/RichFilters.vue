<template>
  <div class="w-full min-h-screen bg-gray-50">
    <!-- Üst bar -->
    <div class="bg-white border-b border-gray-200 px-6 py-3 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">Zengin Filtreler</h1>
        <p class="text-xs text-gray-500 mt-0.5">
          Bir temel sorgu + renkli akıllı filtreler = dashboard grafiklerinin ortak dili
        </p>
      </div>

      <div class="flex items-center gap-3 ml-auto">
        <router-link
          to="/settings"
          class="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-gray-200 bg-white text-sm text-gray-700 hover:border-purple-300 hover:text-purple-700 transition"
          title="Aktif takımı Ayarlar'dan değiştir"
        >
          <span class="w-2 h-2 rounded-full bg-green-500"></span>
          {{ activeTeam?.teamName || 'Takım seç' }}
        </router-link>

        <button
          class="text-sm bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white px-3 py-1.5 rounded-lg transition-colors"
          :disabled="!activeTeamId"
          @click="showCreate = true"
        >
          Zengin filtre oluştur
        </button>
      </div>
    </div>

    <div class="p-6">
      <div v-if="!activeTeamId" class="text-center py-20 text-sm text-gray-400">
        Zengin filtre oluşturmak için bir takım seçin.
      </div>

      <div v-else-if="loading" class="text-center py-20 text-sm text-gray-400">Yükleniyor…</div>

      <!-- Boş durum -->
      <div v-else-if="!filters.length" class="max-w-2xl mx-auto text-center py-16">
        <div class="text-4xl mb-4">◧</div>
        <h2 class="text-base font-semibold text-gray-800">Henüz zengin filtre yok</h2>
        <p class="text-sm text-gray-500 mt-2">
          Zengin filtre, bir görev kümesini adlandırılmış ve renkli kategorilere ayırır.
          "Analiz", "Geliştirme", "Test", "Tamamlandı" gibi kategoriler kurduğunuzda hem
          grafikler hem de <code class="font-mono text-xs">smart[…]</code> sorguları bu kategorileri kullanır.
        </p>
        <button
          class="mt-5 text-sm bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
          @click="showCreate = true"
        >
          İlk zengin filtreyi oluştur
        </button>
      </div>

      <!-- Liste -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <div
          v-for="filter in filters"
          :key="filter.id"
          class="bg-white rounded-xl border border-gray-100 p-5 hover:border-purple-200 hover:shadow-sm transition-all cursor-pointer flex flex-col gap-3"
          @click="open(filter)"
        >
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <h3 class="text-sm font-semibold text-gray-800 truncate">{{ filter.name }}</h3>
              <p v-if="filter.description" class="text-xs text-gray-500 mt-0.5 line-clamp-2">
                {{ filter.description }}
              </p>
            </div>
            <span class="shrink-0 text-[10px] px-1.5 py-0.5 rounded border" :class="visibilityClass(filter.visibility)">
              {{ visibilityLabel(filter.visibility) }}
            </span>
          </div>

          <!-- Akıllı filtre renkleri -->
          <div v-if="smartOf(filter).length" class="flex items-center gap-1 flex-wrap">
            <span
              v-for="element in smartOf(filter).slice(0, 8)"
              :key="element.id"
              class="w-4 h-4 rounded"
              :style="{ backgroundColor: element.color || '#94A3B8' }"
              :title="element.name"
            ></span>
            <span v-if="smartOf(filter).length > 8" class="text-[11px] text-gray-400">
              +{{ smartOf(filter).length - 8 }}
            </span>
          </div>
          <p v-else class="text-[11px] text-gray-400">Akıllı filtre eklenmemiş</p>

          <code v-if="filter.effectiveQuery" class="text-[11px] font-mono text-gray-500 truncate" :title="filter.effectiveQuery">
            {{ filter.effectiveQuery }}
          </code>

          <div class="flex items-center justify-between text-[11px] text-gray-400 pt-1 border-t border-gray-50">
            <span>{{ filter.ownerName || filter.ownerEmail }}</span>
            <span>{{ smartOf(filter).length }} akıllı filtre</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Oluşturma diyaloğu -->
    <div
      v-if="showCreate"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      @click.self="showCreate = false"
    >
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
        <div class="px-6 py-4 border-b border-gray-100">
          <h2 class="text-base font-semibold text-gray-800">Zengin filtre oluştur</h2>
        </div>

        <div class="px-6 py-5 space-y-4">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Ad</label>
            <input
              v-model="draft.name"
              type="text"
              maxlength="120"
              placeholder="Durum akışı"
              class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
            />
            <p class="mt-1 text-[11px] text-gray-400">
              Takım içinde benzersiz olmalı — sorgularda bu adla kullanılır.
            </p>
          </div>

          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Temel sorgu</label>
            <select
              v-model="draft.baseFilterId"
              class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400"
            >
              <option :value="null">Kayıtlı filtre kullanma — sorguyu burada yaz</option>
              <option v-for="sf in savedFilters" :key="sf.id" :value="sf.id">{{ sf.name }}</option>
            </select>

            <div v-if="!draft.baseFilterId" class="mt-3">
              <StqlInput
                v-model="draft.baseQuery"
                :team-id="activeTeamId"
                :project-id="projectId"
                placeholder="sprint = currentSprint()"
              />
              <p class="mt-1.5 text-[11px] text-gray-400">
                Boş bırakılırsa takımın tüm görevleri kapsanır.
              </p>
            </div>
          </div>

          <p v-if="createError" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
            {{ createError }}
          </p>
        </div>

        <div class="px-6 py-4 border-t border-gray-100 flex items-center justify-end gap-2">
          <button class="text-sm text-gray-500 hover:text-gray-700 px-3 py-1.5" @click="showCreate = false">İptal</button>
          <button
            class="text-sm bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white px-4 py-1.5 rounded-lg transition-colors"
            :disabled="!draft.name.trim() || creating"
            @click="create"
          >
            {{ creating ? 'Oluşturuluyor…' : 'Oluştur' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Zengin filtre listesi.
 *
 * Kartlarda akıllı filtrelerin renkleri gösterilir: kullanıcı listeye bakınca
 * hangi filtrenin hangi sınıflandırmayı taşıdığını adını okumadan anlar.
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import StqlInput from '../components/work/StqlInput.vue'
import { getRichFilters, createRichFilter } from '../api/RichFilterApi.js'
import { getSavedFilters } from '../api/SavedFilterApi.js'
import { useTeamContext } from '../composables/useTeamContext.js'
import { useProjectContext } from '../composables/useProjectContext.js'

const router = useRouter()
const { activeTeamId, activeTeam, loadTeams } = useTeamContext()
const { projectId } = useProjectContext(() => activeTeamId.value)

const filters = ref([])
const savedFilters = ref([])
const loading = ref(true)

const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const draft = ref({ name: '', baseFilterId: null, baseQuery: '' })

const VISIBILITY = {
  PRIVATE: { label: 'Özel', class: 'border-gray-200 text-gray-500' },
  TEAM: { label: 'Takım', class: 'border-blue-200 text-blue-600 bg-blue-50' },
  PROJECT: { label: 'Proje', class: 'border-purple-200 text-purple-600 bg-purple-50' },
}

const visibilityLabel = (v) => VISIBILITY[v]?.label ?? v
const visibilityClass = (v) => VISIBILITY[v]?.class ?? 'border-gray-200 text-gray-500'

const smartOf = (filter) => (filter.elements || []).filter(e => e.kind === 'SMART_FILTER')

function open(filter) {
  router.push(`/rich-filters/${filter.id}`)
}

async function load() {
  if (!activeTeamId.value) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    filters.value = await getRichFilters(activeTeamId.value, projectId.value)
    savedFilters.value = await getSavedFilters(activeTeamId.value, projectId.value)
  } catch (e) {
    console.error('Zengin filtreler yüklenemedi', e)
  } finally {
    loading.value = false
  }
}

async function create() {
  creating.value = true
  createError.value = ''
  try {
    const created = await createRichFilter(activeTeamId.value, {
      name: draft.value.name.trim(),
      baseFilterId: draft.value.baseFilterId,
      baseQuery: draft.value.baseFilterId ? null : draft.value.baseQuery,
      visibility: 'TEAM',
      projectId: null,
    })
    showCreate.value = false
    draft.value = { name: '', baseFilterId: null, baseQuery: '' }
    router.push(`/rich-filters/${created.id}`)
  } catch (e) {
    createError.value = e?.response?.data?.message || 'Zengin filtre oluşturulamadı.'
  } finally {
    creating.value = false
  }
}

onMounted(async () => {
  await loadTeams()
  await load()
})

watch(activeTeamId, load)
</script>
