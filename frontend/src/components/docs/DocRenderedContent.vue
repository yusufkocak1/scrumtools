<template>
  <article ref="root"
           class="prose prose-indigo max-w-3xl mx-auto px-4 sm:px-8 py-8 sm:py-10 overflow-x-auto"
           v-html="html"></article>
</template>

<script setup>
import { ref, h, render, watch, onBeforeUnmount } from 'vue'
import CollabEmbed from '../collab/CollabEmbed.vue'

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
  if (!root.value) return

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

// `flush: 'post'` şart: varsayılan 'pre' ile izleyici DOM güncellenmeden önce
// koşar ve `v-html`'in yeni çıktısı henüz basılmamış olur — gömme kapları
// bulunamaz. `nextTick` ile beklemek de işe yarardı ama ilk çalıştırmada
// (`immediate`) bileşen henüz bağlanmadığı için `root` boş kalırdı.
watch(() => props.html, hydrate, { immediate: true, flush: 'post' })
onBeforeUnmount(unmountAll)
</script>
