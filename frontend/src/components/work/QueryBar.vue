<template>
  <div class="border-b border-gray-100 bg-white">
    <!-- Sekmeler + kayıtlı filtreler -->
    <div class="flex items-center gap-2 px-3 pt-2 sm:px-4">
      <div class="flex rounded-lg bg-gray-100 p-0.5">
        <button
          v-for="tab in TABS"
          :key="tab.id"
          class="rounded-md px-3 py-1 text-xs font-medium transition-all"
          :class="mode === tab.id
            ? 'bg-white text-gray-900 shadow-sm'
            : 'text-gray-500 hover:text-gray-700'"
          @click="mode = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <span
        v-if="mode === 'stql' && activeFilterCount"
        class="text-[11px] text-gray-400"
      >
        {{ activeFilterCount }} koşul
      </span>

      <div class="ml-auto flex items-center gap-2">
        <SavedFilterMenu
          :team-id="teamId"
          :project-id="projectId"
          :current-query="query"
          @select="onSavedFilterSelect"
        />
      </div>
    </div>

    <!-- STQL sekmesi -->
    <div v-if="mode === 'stql'" class="px-3 py-2.5 sm:px-4">
      <StqlInput
        v-model="query"
        :team-id="teamId"
        :project-id="projectId"
        :error="error"
        @run="onRun"
        @validate="$emit('validate', $event)"
      />
      <p class="mt-1.5 px-1 text-[11px] text-gray-400">
        Örnek:
        <button
          v-for="ex in EXAMPLES"
          :key="ex"
          class="mr-2 font-mono text-gray-500 underline decoration-dotted underline-offset-2 hover:text-blue-600"
          @click="applyExample(ex)"
        >{{ ex }}</button>
      </p>
    </div>

    <!-- Basit sekmesi -->
    <template v-else>
      <div
        v-if="!builderCompatible"
        class="mx-3 mt-2 flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 sm:mx-4"
      >
        <svg class="mt-px h-3.5 w-3.5 shrink-0 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <div class="text-[11px] leading-relaxed">
          <p class="text-amber-800">
            {{ builderIncompatibleReason || 'Bu sorgu görsel düzenleyiciye sığmıyor.' }}
          </p>
          <p class="mt-0.5 font-mono text-amber-700 break-all">{{ query }}</p>
          <button class="mt-1 font-sans text-amber-900 underline" @click="mode = 'stql'">
            STQL sekmesinde düzenle
          </button>
        </div>
      </div>

      <FilterBar
        :active-filters="filters"
        :statuses="statuses"
        :priorities="priorities"
        :issue-types="issueTypes"
        @add-filter="$emit('add-filter', $event)"
        @remove-filter="$emit('remove-filter', $event)"
        @clear-filters="$emit('clear-filters')"
        @open-builder="showBuilder = true"
      />
    </template>

    <FilterBuilder
      :is-open="showBuilder"
      :initial-filters="filters"
      :fields="catalogFields"
      @close="showBuilder = false"
      @apply="$emit('apply-filters', $event)"
    />
  </div>
</template>

<script setup>
/**
 * Görev sorgu çubuğu — "Basit" (görsel) ve "STQL" (sorgu dili) sekmelerini
 * tek kabuk altında toplar. İki sekme de aynı sorguyu besler; senkron
 * useTaskQuery composable'ında yapılır.
 *
 * Görsel düzenleyicinin alan listesi sunucudaki alan kataloğundan gelir, böylece
 * yeni sorgulanabilir bir alan eklendiğinde arayüz elle güncellenmek zorunda kalmaz.
 */
import { ref, watch, onMounted } from 'vue'
import FilterBar from './FilterBar.vue'
import FilterBuilder from './FilterBuilder.vue'
import StqlInput from './StqlInput.vue'
import SavedFilterMenu from './SavedFilterMenu.vue'
import { getQueryFields } from '../../api/QueryApi.js'

const props = defineProps({
  teamId:    { type: String, required: true },
  projectId: { type: String, default: null },

  /** STQL metni (v-model:query) */
  query:     { type: String, default: '' },
  /** Görsel koşullar: [{ field, operator, values }] */
  filters:   { type: Array, default: () => [] },

  builderCompatible:        { type: Boolean, default: true },
  builderIncompatibleReason: { type: String, default: null },
  activeFilterCount:        { type: Number, default: 0 },
  error:                    { type: Object, default: null },

  statuses:   { type: Array, default: () => ['To Do', 'In Progress', 'Done', 'Cancelled'] },
  priorities: { type: Array, default: () => ['Low', 'Medium', 'High', 'Critical'] },
  issueTypes: { type: Array, default: () => ['task', 'story', 'bug', 'epic'] },
})

const emit = defineEmits([
  'update:query', 'run', 'validate',
  'add-filter', 'remove-filter', 'clear-filters', 'apply-filters',
])

const TABS = [
  { id: 'builder', label: 'Basit' },
  { id: 'stql', label: 'STQL' },
]

const EXAMPLES = [
  'summary ~ "ödeme"',
  'assignee = currentUser() AND status != Done',
  'due <= 7d ORDER BY priority DESC',
]

const mode = ref('builder')
const showBuilder = ref(false)
const catalogFields = ref([])

const query = ref(props.query)

watch(() => props.query, (v) => { if (v !== query.value) query.value = v })
watch(query, (v) => emit('update:query', v))

// Görsel düzenleyiciye sığmayan bir sorgu geldiğinde (ör. kayıtlı filtre)
// kullanıcıyı sessizce yanlış görünümde bırakmamak için STQL sekmesine geçilir.
watch(() => props.builderCompatible, (compatible) => {
  if (!compatible) mode.value = 'stql'
})

onMounted(async () => {
  try {
    const catalog = await getQueryFields(props.teamId)
    catalogFields.value = catalog.fields || []
  } catch {
    // Katalog alınamazsa FilterBuilder kendi asgari listesine düşer.
  }
})

function onRun(text) {
  emit('update:query', text)
  emit('run', text)
}

function onSavedFilterSelect(filter) {
  mode.value = 'stql'
  query.value = filter.query || ''
  emit('update:query', query.value)
  emit('run', query.value)
}

function applyExample(example) {
  query.value = example
  emit('update:query', example)
  emit('run', example)
}
</script>
