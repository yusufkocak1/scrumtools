<template>
  <div class="relative">
    <div
      class="flex items-start gap-2 rounded-lg border bg-white transition-all"
      :class="props.error
        ? 'border-red-300 ring-2 ring-red-500/10'
        : 'border-gray-200 focus-within:border-blue-400 focus-within:ring-2 focus-within:ring-blue-500/20'"
    >
      <svg class="w-4 h-4 text-gray-400 mt-2.5 ml-3 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
      </svg>

      <textarea
        ref="inputEl"
        v-model="text"
        rows="1"
        spellcheck="false"
        :placeholder="placeholder"
        class="flex-1 resize-none bg-transparent py-2 pr-2 text-xs font-mono text-gray-800 placeholder:text-gray-400 placeholder:font-sans focus:outline-none"
        @input="onInput"
        @keydown="onKeydown"
        @blur="onBlur"
        @click="updateSuggestions"
      ></textarea>

      <div class="flex items-center gap-1.5 py-1.5 pr-2 shrink-0">
        <span v-if="isLoading" class="text-[11px] text-gray-400">…</span>
        <span
          v-else-if="resultCount !== null && !error"
          class="text-[11px] font-medium text-gray-500 whitespace-nowrap"
        >
          {{ resultCount }} sonuç
        </span>
        <button
          v-if="text"
          class="text-gray-400 hover:text-gray-600 p-0.5"
          title="Temizle"
          @click="clear"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
        <button
          class="rounded-md bg-blue-600 px-2.5 py-1 text-[11px] font-medium text-white hover:bg-blue-700 transition-colors whitespace-nowrap"
          @click="emitRun"
        >
          Çalıştır
        </button>
      </div>
    </div>

    <!--
      Hata gösterimi iki seviyeli:
        · Çalıştırma hatası (props.error) kırmızı — kullanıcı bilerek çalıştırdı.
        · Yazım anındaki uyarı (liveError) amber — sorgu henüz yarım olabilir,
          kırmızı göstermek yazarken sürekli "hata yaptın" demek olurdu.
    -->
    <div v-if="displayedError" class="mt-1.5 flex items-start gap-1.5 px-1">
      <svg
        class="w-3.5 h-3.5 mt-px shrink-0"
        :class="props.error ? 'text-red-500' : 'text-amber-500'"
        fill="none" stroke="currentColor" viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01M5 19h14a2 2 0 001.84-2.75L13.74 4a2 2 0 00-3.5 0l-7.1 12.25A2 2 0 004.99 19z"/>
      </svg>
      <div class="text-[11px] leading-relaxed">
        <p :class="props.error ? 'text-red-600' : 'text-amber-700'">{{ displayedError.message }}</p>
        <p v-if="marker" class="mt-0.5 font-mono text-gray-500 whitespace-pre-wrap break-all">
          <span>{{ marker.before }}</span><span
            class="rounded-sm underline decoration-wavy"
            :class="props.error
              ? 'bg-red-100 text-red-700 decoration-red-400'
              : 'bg-amber-100 text-amber-800 decoration-amber-400'"
          >{{ marker.error }}</span><span>{{ marker.after }}</span>
        </p>
      </div>
    </div>

    <!-- Otomatik tamamlama -->
    <div
      v-if="suggestions.length && showSuggestions"
      class="absolute z-40 mt-1 w-full max-w-md max-h-64 overflow-y-auto rounded-lg border border-gray-200 bg-white shadow-lg"
    >
      <p class="px-3 pt-2 pb-1 text-[10px] font-semibold uppercase tracking-wide text-gray-400">
        {{ suggestionTitle }}
      </p>
      <button
        v-for="(s, idx) in suggestions"
        :key="s.value + idx"
        class="w-full flex items-baseline gap-2 px-3 py-1.5 text-left text-xs transition-colors"
        :class="idx === highlighted ? 'bg-blue-50 text-blue-800' : 'text-gray-700 hover:bg-gray-50'"
        @mousedown.prevent="applySuggestion(s)"
        @mouseenter="highlighted = idx"
      >
        <span class="font-mono">{{ s.value }}</span>
        <span v-if="s.label && s.label !== s.value" class="text-[11px] text-gray-400 truncate">
          {{ s.label }}
        </span>
      </button>
    </div>
  </div>
</template>

<script setup>
/**
 * STQL sorgu editörü.
 *
 * Otomatik tamamlama bağlama duyarlıdır: imlecin bulunduğu yere göre alan,
 * operatör veya değer önerir. Değer önerileri sunucudan gelir (takımın gerçek
 * verisi), alan ve operatörler ise alan kataloğundan.
 *
 * Doğrulama sunucuda yapılır — dilin tek yorumcusu backend'dir. İstemci yalnız
 * hatayı konumuyla gösterir.
 */
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { getQueryFields, suggestValues, validateQuery } from '../../api/QueryApi.js'
import { highlightError, quote } from '../../utils/stql.js'

/**
 * Kullanıcı yazmayı bıraktıktan sonra sunucuya gidilene kadar beklenen süre.
 * Her tuş vuruşunda sorgulamak hem gereksiz yük hem de yarım sorgular yüzünden
 * sürekli hata gösterimi demek olurdu.
 */
const VALIDATE_DEBOUNCE_MS = 700
/** Öneri listesi yereldeki katalogdan da beslendiği için daha kısa tutulabilir. */
const SUGGEST_DEBOUNCE_MS = 250

const props = defineProps({
  modelValue: { type: String, default: '' },
  teamId:     { type: String, required: true },
  projectId:  { type: String, default: null },
  error:      { type: Object, default: null },
  placeholder: {
    type: String,
    default: 'summary ~ "ödeme" AND assignee = currentUser() ORDER BY priority DESC'
  },
})

const emit = defineEmits(['update:modelValue', 'run', 'validated'])

const inputEl = ref(null)
const text = ref(props.modelValue)

const catalog = ref({ fields: [], functions: [], keywords: [] })
const suggestions = ref([])
const suggestionTitle = ref('Alanlar')
const showSuggestions = ref(false)
const highlighted = ref(0)
const resultCount = ref(null)
const isLoading = ref(false)

/** Yazarken yapılan doğrulamadan gelen uyarı — çalıştırma hatasından ayrı tutulur. */
const liveError = ref(null)

/** Öneri uygulanırken değiştirilecek metin aralığı. */
const replaceRange = ref({ start: 0, end: 0 })

/** Çalıştırma hatası varsa o önceliklidir; yoksa yazım anındaki uyarı gösterilir. */
const displayedError = computed(() => props.error || liveError.value)

const marker = computed(() => highlightError(text.value, displayedError.value))

watch(() => props.modelValue, (v) => {
  if (v !== text.value) {
    text.value = v ?? ''
    scheduleCheck(0)
  }
})

// Takım veya proje değişince önbellekteki değerler artık geçerli değil.
watch(() => [props.teamId, props.projectId], () => valueCache.clear())

onMounted(async () => {
  try {
    catalog.value = await getQueryFields(props.teamId)
  } catch {
    // Katalog alınamazsa editör yazılabilir kalır, sadece öneri gösterilmez.
  }
  scheduleCheck(0)
})

onBeforeUnmount(() => {
  clearTimeout(checkTimer)
  clearTimeout(suggestTimer)
})

// ─── Giriş ────────────────────────────────────────────────────────────────────

let checkTimer = null

function onInput() {
  emit('update:modelValue', text.value)
  autoGrow()
  updateSuggestions()
  // Yazarken sorgu çalıştırılmaz; yalnızca duraklamada doğrulanır ve sayılır.
  scheduleCheck(VALIDATE_DEBOUNCE_MS)
}

function autoGrow() {
  nextTick(() => {
    const el = inputEl.value
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 120) + 'px'
  })
}

function onKeydown(e) {
  if (showSuggestions.value && suggestions.value.length) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      highlighted.value = (highlighted.value + 1) % suggestions.value.length
      return
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      highlighted.value = (highlighted.value - 1 + suggestions.value.length) % suggestions.value.length
      return
    }
    if (e.key === 'Tab' || (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey)) {
      e.preventDefault()
      applySuggestion(suggestions.value[highlighted.value])
      return
    }
    if (e.key === 'Escape') {
      showSuggestions.value = false
      return
    }
  }

  // Enter çalıştırır; satır sonu gerekiyorsa Shift+Enter.
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    emitRun()
  }
}

function onBlur() {
  // Öneriye tıklama blur'dan sonra gelmesin diye kısa gecikme.
  setTimeout(() => { showSuggestions.value = false }, 120)
}

function emitRun() {
  // Bekleyen doğrulama isteği gereksiz: çalıştırma zaten sunucuya gidiyor.
  clearTimeout(checkTimer)
  showSuggestions.value = false
  emit('update:modelValue', text.value)
  emit('run', text.value)
}

function clear() {
  clearTimeout(checkTimer)
  text.value = ''
  resultCount.value = null
  liveError.value = null
  emit('update:modelValue', '')
  emit('validated', { valid: true })
  emit('run', '')
}

// ─── Doğrulama + canlı sonuç sayacı (tek istek) ───────────────────────────────

/** Aynı anda birden çok yanıt dönerse yalnız en sonuncusu uygulanır. */
let checkSeq = 0

function scheduleCheck(delay = VALIDATE_DEBOUNCE_MS) {
  clearTimeout(checkTimer)
  checkTimer = setTimeout(runCheck, delay)
}

async function runCheck() {
  if (!props.teamId) return

  // Boş sorgu için sunucuya gitmeye gerek yok — sayaç da anlamsız olurdu.
  if (!text.value.trim()) {
    checkSeq++
    liveError.value = null
    resultCount.value = null
    isLoading.value = false
    emit('validated', { valid: true })
    return
  }

  const seq = ++checkSeq
  const snapshot = text.value
  isLoading.value = true
  try {
    const result = await validateQuery(props.teamId, snapshot, props.projectId)
    // Kullanıcı bu arada yazmaya devam ettiyse eski yanıtı uygulama.
    if (seq !== checkSeq) return

    liveError.value = result.valid ? null : result.error
    resultCount.value = result.valid ? (result.count ?? null) : null
    emit('validated', result)
  } catch {
    if (seq !== checkSeq) return
    // Doğrulama ucuna ulaşılamıyorsa editör kullanılabilir kalmalı.
    liveError.value = null
    resultCount.value = null
  } finally {
    if (seq === checkSeq) isLoading.value = false
  }
}

// ─── Otomatik tamamlama ───────────────────────────────────────────────────────

/**
 * İmlecin bulunduğu bağlamı çözer: yeni bir koşulun neresindeyiz?
 * @returns {{kind: 'field'|'operator'|'value', prefix: string, field: string|null, start: number, end: number}}
 */
function detectContext() {
  const el = inputEl.value
  const caret = el ? el.selectionStart : text.value.length
  const before = text.value.slice(0, caret)

  // İmlecin hemen solundaki yazılmakta olan kelime.
  const wordMatch = before.match(/([\p{L}\p{N}_[\]".@-]*)$/u)
  const prefix = wordMatch ? wordMatch[1] : ''
  const start = caret - prefix.length

  // Kelimeden önceki kısmın son anlamlı parçaları.
  const head = before.slice(0, start).trim()
  if (!head) return { kind: 'field', prefix, field: null, start, end: caret }

  // Son bağlaçtan sonrasını al — her koşul kendi başına değerlendirilir.
  const segment = head.split(/\s+(?:AND|OR|and|or)\s+/).pop().trim()
  if (!segment) return { kind: 'field', prefix, field: null, start, end: caret }

  const operatorMatch = segment.match(/(!=|!~|>=|<=|<>|=|~|>|<)\s*$/)
  if (operatorMatch) {
    const field = segment.slice(0, operatorMatch.index).trim().split(/\s+/).pop()
    return { kind: 'value', prefix, field, start, end: caret }
  }
  if (/\b(IN|in)\s*\($/.test(segment) || /,\s*$/.test(segment)) {
    const field = segment.split(/\s+/)[0]
    return { kind: 'value', prefix, field, start, end: caret }
  }
  // Segment tek kelimeyse o kelime alandır ve şimdi operatör bekleniyor.
  if (/^[\p{L}\p{N}_[\].]+$/u.test(segment)) {
    return { kind: 'operator', prefix, field: segment, start, end: caret }
  }
  return { kind: 'field', prefix, field: null, start, end: caret }
}

let suggestTimer = null

function updateSuggestions() {
  clearTimeout(suggestTimer)
  suggestTimer = setTimeout(async () => {
    const ctx = detectContext()
    replaceRange.value = { start: ctx.start, end: ctx.end }
    highlighted.value = 0

    if (ctx.kind === 'field') {
      suggestionTitle.value = 'Alanlar ve fonksiyonlar'
      const q = ctx.prefix.toLowerCase()
      const fields = (catalog.value.fields || [])
        .filter(f => !q || f.name.toLowerCase().includes(q)
          || (f.label || '').toLowerCase().includes(q)
          || (f.aliases || []).some(a => a.toLowerCase().includes(q)))
        .map(f => ({ value: f.name, label: f.label }))
      const keywords = (catalog.value.keywords || [])
        .filter(k => q && k.toLowerCase().startsWith(q))
        .map(k => ({ value: k, label: '' }))
      suggestions.value = [...fields, ...keywords].slice(0, 30)
    } else if (ctx.kind === 'operator') {
      const field = findField(ctx.field)
      suggestionTitle.value = field ? `${field.label} — operatörler` : 'Operatörler'
      suggestions.value = (field?.operators || ['=', '!=', '~', 'IN'])
        .map(op => ({ value: op, label: operatorLabel(op) }))
    } else {
      const field = findField(ctx.field)
      if (!field) { suggestions.value = []; showSuggestions.value = false; return }

      suggestionTitle.value = `${field.label} — değerler`
      if (field.type === 'USER') {
        suggestions.value = [
          { value: 'currentUser()', label: 'Ben' },
          ...(await fetchValues(field.name, ctx.prefix)),
        ]
      } else if (field.type === 'DATE' || field.type === 'DATETIME') {
        suggestions.value = (catalog.value.functions || [])
          .filter(f => !f.name.includes('current') && !f.name.includes('Sprint'))
          .map(f => ({ value: f.name, label: f.description }))
      } else {
        suggestions.value = await fetchValues(field.name, ctx.prefix)
      }
    }

    showSuggestions.value = suggestions.value.length > 0
  }, SUGGEST_DEBOUNCE_MS)
}

/**
 * Alan başına değer listesi önbelleği.
 *
 * Sunucu en fazla 50 öneri döndürür. Liste bu sınıra ulaşmadıysa alanın tüm
 * değerleri elimizde demektir ve ön ek süzmesi yerelde yapılabilir — her tuş
 * vuruşunda sunucuya gitmeye gerek kalmaz. Sınıra ulaşıldıysa (büyük takımlar,
 * çok etiket) süzmeyi sunucu yapmalıdır.
 */
const SUGGEST_PAGE_LIMIT = 50
const valueCache = new Map()

async function fetchValues(fieldName, prefix) {
  const field = findField(fieldName)
  if (!field?.hasSuggestions) return []

  const cached = valueCache.get(field.name)
  if (cached && cached.complete) {
    const q = prefix.toLowerCase()
    return cached.items.filter(item =>
      !q || item.value.toLowerCase().includes(q) || (item.label || '').toLowerCase().includes(q))
  }

  try {
    const items = await suggestValues(props.teamId, field.name, prefix, props.projectId)
    if (!prefix) {
      valueCache.set(field.name, { items, complete: items.length < SUGGEST_PAGE_LIMIT })
    }
    return items
  } catch {
    return []
  }
}

function findField(name) {
  if (!name) return null
  const n = name.toLowerCase()
  return (catalog.value.fields || []).find(f =>
    f.name.toLowerCase() === n || (f.aliases || []).some(a => a.toLowerCase() === n)) || null
}

function applySuggestion(suggestion) {
  if (!suggestion) return

  const ctx = detectContext()
  // Değer önerileri tırnaklanır; alan/operatör/fonksiyon adları olduğu gibi girer.
  const isValue = suggestionTitle.value.includes('değerler')
  const insert = isValue && !suggestion.value.endsWith('()')
    ? quote(suggestion.value)
    : suggestion.value

  const before = text.value.slice(0, ctx.start)
  const after = text.value.slice(ctx.end)
  const needsSpace = !after.startsWith(' ') && !insert.endsWith('(')
  text.value = before + insert + (needsSpace ? ' ' : '') + after

  emit('update:modelValue', text.value)
  showSuggestions.value = false
  autoGrow()

  nextTick(() => {
    const caret = (before + insert).length + (needsSpace ? 1 : 0)
    inputEl.value?.focus()
    inputEl.value?.setSelectionRange(caret, caret)
    updateSuggestions()
  })
}

function operatorLabel(op) {
  return {
    '=': 'eşittir',
    '!=': 'eşit değil',
    '~': 'içeriyor',
    '!~': 'içermiyor',
    '>': 'büyüktür',
    '>=': 'büyük veya eşit',
    '<': 'küçüktür',
    '<=': 'küçük veya eşit',
    'IN': 'listeden biri',
    'NOT IN': 'listede yok',
    'IS EMPTY': 'boş',
    'IS NOT EMPTY': 'dolu',
  }[op] || ''
}
</script>
