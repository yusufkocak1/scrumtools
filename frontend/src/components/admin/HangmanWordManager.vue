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
      Her satıra bir kelime, ya da virgülle ayırarak yapıştırabilirsin. Sadece harf, boşluksuz.
    </p>

    <div class="space-y-2">
      <textarea
        v-model="input"
        rows="3"
        class="input-field"
        :placeholder="language === 'tr' ? 'örnek: retrospektif, teslimat, moral' : 'e.g. teamwork, delivery, morale'">
      </textarea>
      <button @click="addWords" :disabled="adding || !input.trim()" class="btn-primary text-sm">
        {{ adding ? 'Ekleniyor...' : '+ Kelimeleri Ekle' }}
      </button>
    </div>

    <div>
      <div v-if="loading" class="text-center py-8 text-gray-500">Yükleniyor...</div>

      <div v-else-if="!words.length" class="text-center py-8 text-sm text-gray-400">
        Bu dil için henüz eklenmiş kelime yok.
      </div>

      <div v-else class="flex flex-wrap gap-2">
        <span
          v-for="w in words"
          :key="w.id"
          class="inline-flex items-center gap-1.5 px-3 py-1 bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 rounded-full text-xs font-medium">
          {{ w.word }}
          <button @click="removeWord(w.id)" class="text-indigo-400 hover:text-red-500" title="Kaldır">×</button>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import AdminHangmanApi from '../../api/AdminHangmanApi.js'

const languageOptions = [
  { value: 'tr', label: 'Türkçe', flag: '🇹🇷' },
  { value: 'en', label: 'English', flag: '🇬🇧' }
]

const language = ref('tr')
const words = ref([])
const loading = ref(false)
const adding = ref(false)
const input = ref('')

async function load() {
  loading.value = true
  try {
    const res = await AdminHangmanApi.getWords(language.value)
    words.value = res.data
  } catch (e) {
    console.error('Kelimeler yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function addWords() {
  const parsed = input.value
    .split(/[\n,]+/)
    .map(w => w.trim())
    .filter(w => w.length > 0)
  if (!parsed.length) return

  adding.value = true
  try {
    const res = await AdminHangmanApi.addWords(language.value, parsed)
    words.value = res.data.words
    input.value = ''
    if (res.data.addedCount > 0) {
      createToast(`${res.data.addedCount} kelime eklendi`, { type: 'success', position: 'top-center' })
    }
    if (res.data.invalidWords?.length) {
      createToast(`Geçersiz kelimeler atlandı: ${res.data.invalidWords.join(', ')}`, { type: 'warning', position: 'top-center' })
    }
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

watch(language, load)
onMounted(load)
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500;
}
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
</style>
