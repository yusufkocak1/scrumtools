<template>
  <div class="relative">
    <!--
      İçerik araç çubuğu: sayfanın tamamını kopyala / seç.
      Yapışkan ve sağa yaslı — uzun sayfada aşağı inildiğinde de erişilebilir
      olması gerekiyor, asıl derdi olan mobilde en çok orada aranıyor.
    -->
    <div v-if="hasContent"
         class="sticky top-0 z-10 flex items-center justify-end gap-1.5 px-4 sm:px-8 pt-3 pb-2
                bg-gradient-to-b from-white via-white to-white/0">
      <span v-if="hint" class="text-[11px] text-slate-400 mr-1 truncate">{{ hint }}</span>
      <button type="button" @click="selectAll" :class="toolBtnClass(selected)">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round"
                d="M4.5 7.5V6a1.5 1.5 0 011.5-1.5h1.5m9 0H18A1.5 1.5 0 0119.5 6v1.5m0 9V18a1.5 1.5 0 01-1.5 1.5h-1.5m-9 0H6A1.5 1.5 0 014.5 18v-1.5M9 4.5h6m-10.5 6v3m15-3v3M9 19.5h6"/>
        </svg>
        <span>{{ selected ? 'Seçildi' : 'Tümünü Seç' }}</span>
      </button>
      <button type="button" @click="copyAll" :class="toolBtnClass(copyState === 'copied')">
        <svg v-if="copyState === 'copied'" class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.4" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/>
        </svg>
        <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round"
                d="M9 12.75V6.375c0-.621.504-1.125 1.125-1.125h8.25c.621 0 1.125.504 1.125 1.125v8.25c0 .621-.504 1.125-1.125 1.125H12"/>
          <path stroke-linecap="round" stroke-linejoin="round"
                d="M5.625 8.25h6.75c.621 0 1.125.504 1.125 1.125v8.25c0 .621-.504 1.125-1.125 1.125h-6.75A1.125 1.125 0 014.5 17.625v-8.25c0-.621.504-1.125 1.125-1.125z"/>
        </svg>
        <span>{{ copyLabel }}</span>
      </button>
    </div>

    <!--
      Genişlik düzenleyiciyle aynı (`max-w-none`). Okuma görünümü `max-w-3xl` ile
      kapalıydı; "Düzenle"ye basınca sayfa birden genişliyor, çıkınca daralıyordu.
      Aynı içeriğin iki modda iki farklı satır uzunluğuna sahip olması, gömülü
      tablo ve geniş kod bloklarında özellikle rahatsız edici — okurken kırpılan
      tablo düzenlemeye geçince sığıyordu.
    -->
    <article ref="root"
             class="doc-content prose prose-indigo max-w-none px-4 sm:px-8 pb-8 sm:pb-10"
             :class="hasContent ? 'pt-1' : 'pt-8 sm:pt-10'"
             v-html="html"></article>
  </div>
</template>

<script setup>
import { ref, computed, h, render, watch, onMounted, onBeforeUnmount } from 'vue'
import CollabEmbed from '../collab/CollabEmbed.vue'
import { wrapTables } from './table/tableView.js'
import { attachCopyButtons, copyElement, selectContent } from './contentCopy.js'

/**
 * Docs sayfasının okuma görünümü + gömülü ortak dokümanların canlandırılması
 * (COLLAB_WORKSPACE_PLAN.md Y3).
 *
 * Sayfa içeriği HTML olarak saklanıyor ve `v-html` ile basılıyor; gömme düğümü
 * orada boş bir `<div data-collab-embed>` olarak duruyor. Bu bileşen, basılan
 * HTML'i tarayıp o kaplara gerçek Vue bileşenini yerleştiriyor.
 *
 * <b>Neden içerik HTML'e gömülmüyor:</b> tablonun bir kopyasını sayfaya yazmak
 * bu adımı gereksiz kılardı ama kopya ilk düzenlemede eskirdi. Y3'ün tamamı
 * "canlı" olması üzerine kurulu, dolayısıyla HTML'de yalnızca kimlik durur ve
 * içerik <b>okuyanın kendi oturumuyla</b> çekilir — yetkisi olmayan sayfayı
 * okusa bile tabloyu göremez.
 */
const props = defineProps({
  html: { type: String, default: '' },
  projectId: { type: String, required: true }
})

const root = ref(null)
/** Yerleştirilen bileşenler; içerik değişince tek tek sökülmeleri gerekiyor. */
let mounted = []

const hasContent = computed(() => !!props.html?.trim())

function unmountAll() {
  // `render(null, el)` bileşeni söker ve onBeforeUnmount kancalarını çalıştırır.
  // Atlanırsa canlı gömmelerin WS bağlantıları sayfa değişince açık kalırdı.
  mounted.forEach((el) => {
    try { render(null, el) } catch { /* düğüm zaten DOM'dan çıkmış olabilir */ }
  })
  mounted = []
}

function hydrate() {
  unmountAll()
  resetFeedback()
  if (!root.value) return

  // Tabloları kaydırma sarmalayıcısına al: kaydedilen HTML'de sarmalayıcı yok,
  // editörde ise prosemirror-tables üretiyor (bkz. tableView.js).
  wrapTables(root.value)
  // Kod bloğu/tablo başına kopyala düğmesi — sarmalayıcılar hazır olduktan
  // sonra, çünkü tablo düğmesi sarmalayıcının dışına konuyor (contentCopy.js).
  attachCopyButtons(root.value)

  for (const holder of root.value.querySelectorAll('[data-collab-embed]')) {
    const documentId = holder.getAttribute('data-document-id')
    if (!documentId) continue

    holder.innerHTML = ''
    render(h(CollabEmbed, {
      projectId: props.projectId,
      documentId,
      height: Number(holder.getAttribute('data-height')) || 420,
      // Okuma modunda asla canlı değil: bir sayfadaki her gömme canlı olsaydı
      // sayfayı okuyan herkes o kadar WS oturumu açardı (D3).
      live: false
    }), holder)
    mounted.push(holder)
  }
}

// ─── Tüm içeriği kopyala / seç ───────────────────────────────────────────────

const copyState = ref('')
const selected = ref(false)
const hint = ref('')
let feedbackTimer = null

const copyLabel = computed(() => {
  if (copyState.value === 'copied') return 'Kopyalandı'
  if (copyState.value === 'failed') return 'Kopyalanamadı'
  return 'Kopyala'
})

function toolBtnClass(active) {
  return [
    'inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border text-[12px] font-medium transition shrink-0',
    active
        ? 'border-emerald-200 bg-emerald-50 text-emerald-600'
        : 'border-slate-200 bg-white text-slate-500 hover:text-indigo-600 hover:border-indigo-200 hover:bg-indigo-50/50'
  ]
}

function resetFeedback() {
  clearTimeout(feedbackTimer)
  copyState.value = ''
  selected.value = false
  hint.value = ''
}

function scheduleReset() {
  clearTimeout(feedbackTimer)
  feedbackTimer = setTimeout(resetFeedback, 2500)
}

async function copyAll() {
  const ok = await copyElement(root.value)
  copyState.value = ok ? 'copied' : 'failed'
  selected.value = false
  hint.value = ok ? '' : 'Panoya erişilemedi, "Tümünü Seç" ile elle kopyalayın'
  scheduleReset()
}

/**
 * Kopyalamak yerine <i>seçmek</i> de gerekiyor: kullanıcı içeriği kendi
 * menüsüyle paylaşmak/aramak isteyebilir, ayrıca pano izni verilmemiş
 * tarayıcılarda (ya da http üzerinden) kopyalamanın kalan tek yolu bu.
 */
function selectAll() {
  const ok = selectContent(root.value)
  selected.value = ok
  copyState.value = ''
  hint.value = ok ? 'Seçildi — dokunup "Kopyala"yı seçebilirsiniz' : ''
  scheduleReset()
}

// İlk çalıştırma `onMounted`'a bağlı, izleyiciye değil.
//
// <b>Buradaki tuzak:</b> daha önce tek bir `watch(..., { immediate: true,
// flush: 'post' })` vardı. `immediate` verildiğinde Vue ilk çağrıyı `flush`
// ayarına bakmadan <i>setup sırasında</i>, yani şablon daha basılmadan yapıyor;
// `root` o anda `null` olduğu için `hydrate` çıkışa gidiyordu. Sayfa okuma
// görünümüne geçtiğinde bileşen sıfırdan bağlanıyor ve `html` zaten dolu
// geliyor — yani izleyici bir daha hiç tetiklenmiyordu. Sonuç: kaydedilen
// sayfada gömülü tablolar <b>boş bir div</b> olarak kalıyor (ekranda hiç
// görünmüyor) ve tabloların kaydırma sarmalayıcısı hiç oluşmuyordu.
//
// `flush: 'post'` sonraki değişiklikler için hâlâ şart: varsayılan 'pre' ile
// izleyici DOM güncellenmeden önce koşar ve `v-html`'in yeni çıktısı henüz
// basılmamış olur — gömme kapları bulunamaz.
onMounted(hydrate)
watch(() => props.html, hydrate, { flush: 'post' })
onBeforeUnmount(() => {
  clearTimeout(feedbackTimer)
  unmountAll()
})
</script>
