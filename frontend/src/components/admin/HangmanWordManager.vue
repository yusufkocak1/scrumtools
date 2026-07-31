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
      Buraya eklenen kelimeler tüm takımlar için Adam Asmaca oyununda dahili kelime havuzuna eklenir.
      Kelimeler bir kategoriye eklenir; oyuncular kategori seçerek ya da karışık oynayabilir.
      Her satıra bir kelime, ya da virgülle ayırarak yapıştırabilirsin. Sadece harf, boşluksuz.
    </p>

    <div class="space-y-2">
      <select v-model="category" class="input-field">
        <option :value="null">Tüm kategoriler (yalnızca listeleme)</option>
        <option v-for="opt in categoryOptions" :key="opt.code" :value="opt.code">
          {{ opt.emoji }} {{ opt.label }}
        </option>
      </select>

      <textarea
        v-model="input"
        rows="3"
        class="input-field"
        :disabled="!category"
        :placeholder="placeholder">
      </textarea>
      <button @click="addWords" :disabled="adding || !category || !input.trim()" class="btn-primary text-sm">
        {{ adding ? 'Ekleniyor...' : '+ Kelimeleri Ekle' }}
      </button>
      <p v-if="!category" class="text-xs text-amber-600 dark:text-amber-400">
        Kelime eklemek için önce bir kategori seç.
      </p>
    </div>

    <div>
      <div v-if="loading" class="text-center py-8 text-gray-500">Yükleniyor...</div>

      <div v-else-if="!words.length" class="text-center py-8 text-sm text-gray-400">
        {{ category ? 'Bu kategoride henüz eklenmiş kelime yok.' : 'Bu dil için henüz eklenmiş kelime yok.' }}
      </div>

      <div v-else class="space-y-4">
        <div v-for="group in groups" :key="group.code">
          <p class="text-xs font-semibold text-gray-500 dark:text-gray-400 mb-2">
            {{ group.label }}
            <span class="font-normal text-gray-400">({{ group.items.length }})</span>
          </p>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="w in group.items"
              :key="w.id"
              class="inline-flex items-center gap-1.5 px-3 py-1 bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 rounded-full text-xs font-medium">
              {{ w.word }}
              <button @click="removeWord(w.id)" class="text-indigo-400 hover:text-red-500" title="Kaldır">×</button>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import AdminHangmanApi from '../../api/AdminHangmanApi.js'
import { HANGMAN_CATEGORIES, hangmanCategoryOptions, hangmanCategoryLabel } from '../../data/hangmanWords.js'

const languageOptions = [
  { value: 'tr', label: 'Türkçe', flag: '🇹🇷' },
  { value: 'en', label: 'English', flag: '🇬🇧' }
]

const language = ref('tr')
/** null = tüm kategorileri listele (bu durumda ekleme kapalı) */
const category = ref(null)
const words = ref([])
const loading = ref(false)
const adding = ref(false)
const input = ref('')

const categoryOptions = computed(() => hangmanCategoryOptions(language.value))

const placeholder = computed(() => {
  if (!category.value) return 'Önce bir kategori seç'
  return language.value === 'tr' ? 'örnek: retrospektif, teslimat, moral' : 'e.g. teamwork, delivery, morale'
})

/**
 * Kelimeleri kategoriye göre gruplar. Kategori özelliğinden önce eklenmiş kayıtlar
 * (category = null) "Kategorisiz" grubunda en sonda gösterilir; oyunda yalnızca
 * karışık modda çıkarlar, temizlemek için buradan silinebilirler.
 */
const groups = computed(() => {
  const buckets = new Map()
  for (const w of words.value) {
    const code = w.category || 'UNCATEGORIZED'
    if (!buckets.has(code)) buckets.set(code, [])
    buckets.get(code).push(w)
  }

  const order = [...HANGMAN_CATEGORIES.map(c => c.code), 'UNCATEGORIZED']
  return order
    .filter(code => buckets.has(code))
    .map(code => ({
      code,
      label: code === 'UNCATEGORIZED'
        ? '❔ Kategorisiz (eski kayıtlar)'
        : hangmanCategoryLabel(code, language.value) || code,
      items: buckets.get(code)
    }))
})

async function load() {
  loading.value = true
  try {
    const res = await AdminHangmanApi.getWords(language.value, category.value)
    words.value = res.data
  } catch (e) {
    console.error('Kelimeler yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function addWords() {
  if (!category.value) return

  const parsed = input.value
    .split(/[\n,]+/)
    .map(w => w.trim())
    .filter(w => w.length > 0)
  if (!parsed.length) return

  adding.value = true
  try {
    const res = await AdminHangmanApi.addWords(language.value, category.value, parsed)
    input.value = ''
    if (res.data.addedCount > 0) {
      createToast(`${res.data.addedCount} kelime eklendi`, { type: 'success', position: 'top-center' })
    }
    if (res.data.duplicateCount > 0) {
      createToast(`${res.data.duplicateCount} kelime zaten havuzda vardı`, { type: 'info', position: 'top-center' })
    }
    if (res.data.invalidWords?.length) {
      createToast(`Geçersiz kelimeler atlandı: ${res.data.invalidWords.join(', ')}`, { type: 'warning', position: 'top-center' })
    }
    // Yanıt tüm dili döner; seçili kategori filtresine sadık kalmak için yeniden yükle.
    await load()
  } catch (e) {
    console.error('Kelimeler eklenemedi:', e)
  } finally {
    adding.value = false
  }
}

async function removeWord(wordId) {
  try {
    await AdminHangmanApi.deleteWord(wordId)
    words.value = words.value.filter(w => w.id !== wordId)
  } catch (e) {
    console.error('Kelime silinemedi:', e)
  }
}

watch([language, category], load)
onMounted(load)
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-60;
}
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
</style>
