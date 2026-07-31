<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="font-semibold text-gray-900 dark:text-white">Adam Asmaca Kelime Havuzu</h3>
      <div class="flex items-center gap-1 bg-gray-100 dark:bg-gray-700 rounded-lg p-1">
        <button
          v-for="opt in languageOptions"
          :key="opt.value"
          @click="language = opt.value"
          :class="[
            'px-3 py-1.5 rounded-md text-sm font-medium transition-colors',
            language === opt.value
              ? 'bg-white dark:bg-gray-600 text-indigo-600 dark:text-indigo-300 shadow-sm'
              : 'text-gray-500 dark:text-gray-400'
          ]">
          {{ opt.flag }} {{ opt.label }}
        </button>
      </div>
    </div>

    <p class="text-xs text-gray-500 dark:text-gray-400">
      Liste, uygulamayla gelen <strong>dahili</strong> kelimeleri ve buradan <strong>eklenen</strong>
      kelimeleri birlikte gösterir; oyunda ikisi de kullanılır. Dahili kelimeler kodda tanımlıdır,
      silinemez. Kelimeler bir kategoriye eklenir; oyuncular kategori seçerek ya da karışık oynar.
    </p>

    <!-- Ekleme -->
    <div class="space-y-2 bg-gray-50 dark:bg-gray-800/50 rounded-lg p-3">
      <select v-model="addCategory" class="input-field w-full">
        <option :value="null">Kategori seç…</option>
        <option v-for="opt in categoryOptions" :key="opt.code" :value="opt.code">
          {{ opt.emoji }} {{ opt.label }}
        </option>
      </select>

      <textarea
        v-model="input"
        rows="2"
        class="input-field w-full"
        :disabled="!addCategory"
        :placeholder="placeholder">
      </textarea>
      <button @click="addWords" :disabled="adding || !addCategory || !input.trim()" class="btn-primary text-sm">
        {{ adding ? 'Ekleniyor...' : '+ Kelimeleri Ekle' }}
      </button>
      <p class="text-xs text-gray-500 dark:text-gray-400">
        Her satıra bir kelime, ya da virgülle ayırarak yapıştır. Sadece harf, boşluksuz, 2-30 karakter.
      </p>
    </div>

    <!-- Süzgeçler -->
    <div class="flex flex-wrap items-center gap-2">
      <select v-model="category" class="input-field flex-1 min-w-[200px]">
        <option :value="null">Tüm kategoriler</option>
        <option v-for="opt in categoryOptions" :key="opt.code" :value="opt.code">
          {{ opt.emoji }} {{ opt.label }}
        </option>
      </select>
      <input
        v-model="search"
        type="search"
        placeholder="Kelime ara..."
        class="input-field flex-1 min-w-[160px]" />
      <select v-model.number="size" class="input-field">
        <option :value="25">25</option>
        <option :value="50">50</option>
        <option :value="100">100</option>
      </select>
    </div>

    <!-- Tablo -->
    <div>
      <div v-if="loading" class="text-center py-8 text-gray-500">Yükleniyor...</div>

      <div v-else-if="!items.length" class="text-center py-8 text-sm text-gray-400">
        Bu süzgeçlerle eşleşen kelime yok.
      </div>

      <div v-else class="overflow-x-auto border border-gray-200 dark:border-gray-700 rounded-lg">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 dark:bg-gray-700/50 text-gray-500 dark:text-gray-400">
            <tr>
              <th class="text-left font-medium px-3 py-2">Kelime</th>
              <th class="text-left font-medium px-3 py-2">Kategori</th>
              <th class="text-left font-medium px-3 py-2">Kaynak</th>
              <th class="text-left font-medium px-3 py-2 hidden sm:table-cell">Ekleyen</th>
              <th class="px-3 py-2 w-10"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
            <tr v-for="w in items" :key="w.id || `${w.category}-${w.word}`"
                class="text-gray-800 dark:text-gray-200">
              <td class="px-3 py-2 font-medium">{{ w.word }}</td>
              <td class="px-3 py-2 text-gray-500 dark:text-gray-400">
                {{ categoryText(w.category) }}
              </td>
              <td class="px-3 py-2">
                <span
                  :class="[
                    'px-2 py-0.5 rounded-full text-xs font-medium',
                    w.source === 'BUILT_IN'
                      ? 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300'
                      : 'bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-300'
                  ]">
                  {{ w.source === 'BUILT_IN' ? 'Dahili' : 'Eklenen' }}
                </span>
              </td>
              <td class="px-3 py-2 text-gray-500 dark:text-gray-400 hidden sm:table-cell">
                {{ w.createdByEmail || '—' }}
              </td>
              <td class="px-3 py-2 text-right">
                <button
                  v-if="w.id"
                  @click="removeWord(w)"
                  class="text-gray-400 hover:text-red-500"
                  title="Kaldır">×</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Sayfalama -->
      <div v-if="!loading && totalElements > 0"
           class="flex flex-wrap items-center justify-between gap-2 mt-3 text-sm text-gray-500 dark:text-gray-400">
        <span>
          {{ totalElements }} kelime — sayfa {{ page + 1 }} / {{ totalPages }}
        </span>
        <div class="flex items-center gap-2">
          <button @click="page--" :disabled="page === 0" class="btn-page">← Önceki</button>
          <button @click="page++" :disabled="page >= totalPages - 1" class="btn-page">Sonraki →</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import AdminHangmanApi from '../../api/AdminHangmanApi.js'
import { hangmanCategoryOptions, hangmanCategoryLabel } from '../../data/hangmanWords.js'

const languageOptions = [
  { value: 'tr', label: 'Türkçe', flag: '🇹🇷' },
  { value: 'en', label: 'English', flag: '🇬🇧' }
]

const language = ref('tr')
/** Listeyi süzen kategori — null = tümü */
const category = ref(null)
/** Yeni kelimelerin ekleneceği kategori (süzgeçten bağımsız, zorunlu) */
const addCategory = ref(null)
const search = ref('')
const page = ref(0)
const size = ref(50)

const items = ref([])
const totalElements = ref(0)
const totalPages = ref(1)
const loading = ref(false)
const adding = ref(false)
const input = ref('')

const categoryOptions = computed(() => hangmanCategoryOptions(language.value))

const placeholder = computed(() => {
  if (!addCategory.value) return 'Önce bir kategori seç'
  return language.value === 'tr' ? 'örnek: retrospektif, teslimat, moral' : 'e.g. teamwork, delivery, morale'
})

/** Kategori özelliğinden önce eklenmiş kayıtlarda kategori boştur. */
function categoryText(code) {
  return hangmanCategoryLabel(code, language.value) || '❔ Kategorisiz'
}

async function load() {
  loading.value = true
  try {
    const { data } = await AdminHangmanApi.getWords({
      language: language.value,
      category: category.value,
      search: search.value.trim(),
      page: page.value,
      size: size.value
    })
    items.value = data.items
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
    // Süzgeç daraldığında son sayfada kalınmışsa geri sar.
    if (page.value > 0 && page.value >= data.totalPages) page.value = data.totalPages - 1
  } catch (e) {
    console.error('Kelimeler yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function addWords() {
  if (!addCategory.value) return

  const parsed = input.value
    .split(/[\n,]+/)
    .map(w => w.trim())
    .filter(w => w.length > 0)
  if (!parsed.length) return

  adding.value = true
  try {
    const { data } = await AdminHangmanApi.addWords(language.value, addCategory.value, parsed)
    input.value = ''
    if (data.addedCount > 0) {
      createToast(`${data.addedCount} kelime eklendi`, { type: 'success', position: 'top-center' })
    }
    if (data.duplicateCount > 0) {
      createToast(`${data.duplicateCount} kelime havuzda zaten vardı`, { type: 'info', position: 'top-center' })
    }
    if (data.invalidWords?.length) {
      createToast(`Geçersiz kelimeler atlandı: ${data.invalidWords.join(', ')}`, { type: 'warning', position: 'top-center' })
    }
    await load()
  } catch (e) {
    console.error('Kelimeler eklenemedi:', e)
  } finally {
    adding.value = false
  }
}

async function removeWord(word) {
  try {
    await AdminHangmanApi.deleteWord(word.id)
    await load()
  } catch (e) {
    console.error('Kelime silinemedi:', e)
  }
}

// Süzgeç değişince ilk sayfaya dön; arama için yazmayı bırakmayı bekle.
let searchTimer = null

/** Sayfa değişimi zaten load'u tetiklediği için çift istek atmamaya dikkat. */
function resetAndLoad() {
  if (page.value !== 0) page.value = 0
  else load()
}

watch([language, category, size], resetAndLoad)
watch(search, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(resetAndLoad, 300)
})
watch(page, load)
onMounted(load)
</script>

<style scoped>
.input-field {
  @apply px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-60;
}
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
.btn-page {
  @apply px-3 py-1.5 border border-gray-300 dark:border-gray-600 rounded-lg text-sm text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors;
}
</style>
