<template>
  <div class="flex flex-col h-full bg-white">
    <header class="px-4 py-2.5 border-b border-slate-200 flex items-center gap-3 shrink-0">
      <button @click="$emit('back')"
              class="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
              title="Listeye dön">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>

      <span :class="['px-2 py-0.5 rounded-md text-[11px] font-semibold tracking-wide', typeBadge.class]">
        {{ typeBadge.label }}
      </span>

      <input
          v-if="canWrite"
          :value="title"
          @change="$emit('rename', $event.target.value)"
          class="flex-1 min-w-0 text-lg font-semibold text-slate-800 bg-transparent border border-transparent rounded-lg px-2 py-1 -mx-2 outline-none hover:border-slate-200 focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100 transition"
          maxlength="500"/>
      <h1 v-else class="flex-1 min-w-0 text-lg font-semibold text-slate-800 truncate">{{ title }}</h1>

      <select v-if="type === 'CODE' && canWrite"
              :value="language"
              @change="$emit('language', $event.target.value)"
              class="text-xs border border-slate-200 rounded-lg px-2 py-1.5 text-slate-600 outline-none focus:border-indigo-300 transition">
        <option v-for="lang in LANGUAGES" :key="lang" :value="lang">{{ lang }}</option>
      </select>

      <span v-if="!canWrite"
            class="px-2 py-1 rounded-lg bg-slate-100 text-slate-500 text-xs font-medium whitespace-nowrap"
            title="Bu dokümanda yazma yetkiniz yok">
        Salt okunur
      </span>

      <span class="text-xs text-slate-400 whitespace-nowrap min-w-[7rem] text-right">{{ saveLabel }}</span>

      <!-- Elle kaydetme (REST). Bağlantı durumundan bağımsız çalışır: otomatik
           kaydetme WS üzerinden seçilen "yazar"a bağlı ve bağlantı yokken hiç
           tetiklenmiyor. -->
      <button v-if="canWrite" @click="$emit('save')" :disabled="saving"
              :class="['px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition shadow-sm',
                       needsSave
                         ? 'bg-indigo-600 text-white hover:bg-indigo-500 shadow-indigo-200'
                         : 'bg-slate-100 text-slate-600 hover:bg-slate-200',
                       saving ? 'opacity-60 cursor-wait' : '']"
              :title="status === 'synced' ? 'Kaydet (Ctrl+S)' : 'Bağlantı olmadan da kaydeder (Ctrl+S)'">
        {{ saving ? 'Kaydediliyor…' : 'Kaydet' }}
      </button>

      <PresenceBar :participants="participants"/>

      <a v-if="docPageLink" :href="docPageLink"
         class="px-2 py-1 rounded-lg bg-amber-50 text-amber-700 text-xs font-medium whitespace-nowrap hover:bg-amber-100 transition"
         title="Bu doküman bir Docs sayfasıyla aynalanıyor">
        Docs'a bağlı
      </a>
      <button v-else-if="canWrite && type === 'TEXT'" @click="$emit('publish')"
              class="px-2.5 py-1.5 rounded-lg text-slate-600 hover:bg-slate-100 text-xs font-medium whitespace-nowrap transition">
        Docs'a Kaydet
      </button>

      <!-- Excel/CSV indirme (plan §10). Yazma yetkisi aranmıyor: dosyayı
           indirmek okuma yetkisiyle yapılabilecek bir iştir. -->
      <div v-if="type === 'SHEET'" class="relative">
        <button @click="exportOpen = !exportOpen"
                class="px-2.5 py-1.5 rounded-lg text-slate-600 hover:bg-slate-100 text-xs font-medium whitespace-nowrap transition">
          İndir
        </button>
        <div v-if="exportOpen"
             class="absolute right-0 top-full mt-1 z-20 w-32 bg-white border border-slate-200 rounded-xl shadow-lg py-1">
          <button v-for="format in ['xlsx', 'csv']" :key="format"
                  @click="emitExport(format)"
                  class="w-full text-left px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50 transition">
            .{{ format }} olarak
          </button>
        </div>
      </div>

      <button @click="$emit('toggle-macros')"
              class="px-2.5 py-1.5 rounded-lg text-slate-600 hover:bg-slate-100 text-xs font-medium whitespace-nowrap transition"
              title="Makrolar — JavaScript betikleri">
        Makrolar
      </button>

      <button @click="$emit('toggle-history')"
              :class="['p-1.5 rounded-lg transition', historyOpen
                        ? 'bg-indigo-50 text-indigo-600'
                        : 'text-slate-400 hover:text-slate-700 hover:bg-slate-100']"
              title="Geçmiş">
        <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round"
                d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </button>
    </header>

    <ConnectionBanner :status="status" :detail="connectionError" @retry="$emit('retry')"/>

    <div class="flex-1 min-h-0">
      <slot/>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import PresenceBar from './PresenceBar.vue'
import ConnectionBanner from './ConnectionBanner.vue'

/**
 * Doküman kabuğu: başlık, katılımcılar, kaydetme durumu ve bağlantı şeridi.
 * Editörün kendisi slot'tan gelir — üç içerik tipi de aynı kabuğu paylaşır.
 */
const props = defineProps({
  title: { type: String, default: '' },
  type: { type: String, default: 'TEXT' },
  language: { type: String, default: 'javascript' },
  status: { type: String, default: 'connecting' },
  canWrite: { type: Boolean, default: false },
  isWriter: { type: Boolean, default: false },
  pendingChanges: { type: Boolean, default: false },
  lastSavedAt: { type: [Date, null], default: null },
  participants: { type: Array, default: () => [] },
  /** Docs'a bağlıysa sayfanın adresi; boşsa "Docs'a Kaydet" düğmesi gösterilir. */
  docPageLink: { type: String, default: '' },
  historyOpen: { type: Boolean, default: false },
  /** Elle kaydetme sürüyor mu. */
  saving: { type: Boolean, default: false },
  /** Bağlantı arızasının teknik açıklaması. */
  connectionError: { type: String, default: '' }
})

const emit = defineEmits(['back', 'rename', 'language', 'retry', 'publish', 'export',
  'save', 'toggle-macros', 'toggle-history'])

const exportOpen = ref(false)

function emitExport(format) {
  exportOpen.value = false
  emit('export', format)
}

const LANGUAGES = [
  'javascript', 'typescript', 'java', 'python', 'csharp', 'go', 'rust', 'kotlin',
  'php', 'ruby', 'sql', 'html', 'css', 'json', 'yaml', 'xml', 'markdown', 'shell', 'plaintext'
]

const TYPE_BADGES = {
  TEXT: { label: 'METİN', class: 'bg-indigo-50 text-indigo-600' },
  CODE: { label: 'KOD', class: 'bg-slate-800 text-white' },
  SHEET: { label: 'TABLO', class: 'bg-emerald-50 text-emerald-600' }
}
const typeBadge = computed(() => TYPE_BADGES[props.type] || TYPE_BADGES.TEXT)

/** Kaydet düğmesi vurgulansın mı — bekleyen değişiklik varsa. */
const needsSave = computed(() => props.pendingChanges || props.status !== 'synced')

/**
 * Kaydetme göstergesi.
 *
 * Bağlıyken "kaydedilmedi" yerine "kaydediliyor" deniyor, çünkü CRDT'de
 * değişiklik zaten sunucudaki append log'una yazıldı — bekleyen tek şey
 * okunabilir anlık görüntü (plan K6).
 *
 * <b>Bağlantı yokken bu doğru değil</b> ve etiket eskiden bu durumda tamamen
 * boşalıyordu: kullanıcı ne "kaydedildi" ne "kaydedilmedi" görüyor, hiçbir şey
 * görmüyordu. Kopukken değişiklikler gerçekten yalnızca sekmede duruyor, bu
 * yüzden açıkça söyleniyor.
 */
const saveLabel = computed(() => {
  if (props.status !== 'synced') {
    return props.pendingChanges ? 'Kaydedilmedi' : ''
  }
  if (props.pendingChanges) return 'Kaydediliyor…'
  if (props.lastSavedAt) {
    return `Kaydedildi ${props.lastSavedAt.toLocaleTimeString('tr-TR', {
      hour: '2-digit', minute: '2-digit'
    })}`
  }
  return 'Değişiklik yok'
})
</script>
