<template>
  <div
    ref="root"
    class="rich-content-viewer doc-content prose prose-sm sm:prose max-w-none
           prose-headings:text-gray-900 prose-p:text-gray-700
           prose-a:text-blue-600 prose-a:underline
           prose-img:rounded-lg prose-img:max-w-full prose-img:shadow-sm
           prose-code:bg-gray-100 prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:text-sm
           prose-pre:bg-gray-900 prose-pre:text-gray-100
           prose-blockquote:border-l-blue-500"
    v-html="renderedHtml"
  ></div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { RICH_CONTENT_SANITIZE_CONFIG } from '../docs/table/tableSchema.js'
import { wrapTables } from '../docs/table/tableView.js'

const root = ref(null)

const props = defineProps({
  content: { type: String, default: '' }
})

/**
 * Detect whether the content is already HTML (from TipTap) or legacy Markdown.
 * Simple heuristic: if trimmed content starts with a '<' tag, treat as HTML.
 */
function isHtml(text) {
  if (!text) return false
  const trimmed = text.trim()
  return trimmed.startsWith('<')
}

/**
 * İzin listesi `docs/table/tableSchema.js`'ten geliyor (DOCS_TABLE_PLAN.md §6).
 *
 * Buradaki liste daha önce yalnızca `colspan`/`rowspan` içeriyordu; `colwidth`
 * bile yoktu, yani görev açıklamasındaki bir tablonun sütun genişlikleri
 * kaydediliyor ama <b>gösterilmiyordu</b>. Tam olarak planın uyardığı sessiz
 * kayıp türü — düzeltmenin yolu listeyi tek kaynağa bağlamaktan geçiyor.
 */
const renderedHtml = computed(() => {
  if (!props.content) return ''

  const html = isHtml(props.content)
      ? props.content
      : marked.parse(props.content, { breaks: true, gfm: true })

  return DOMPurify.sanitize(html, RICH_CONTENT_SANITIZE_CONFIG)
})

// İlk sarmalama `onMounted` ile: `immediate: true` verilseydi Vue ilk çağrıyı
// `flush` ayarına bakmadan setup sırasında yapardı ve `root` henüz `null`
// olduğu için sarmalayıcı hiç oluşmazdı (DocRenderedContent.vue'daki aynı tuzak).
// Sonraki değişiklikler için `flush: 'post'` şart: varsayılan 'pre' ile izleyici
// DOM güncellenmeden önce koşar ve `v-html`'in yeni çıktısı henüz basılmamış olur.
onMounted(() => wrapTables(root.value))
watch(renderedHtml, () => wrapTables(root.value), { flush: 'post' })
</script>

<style scoped>
.rich-content-viewer :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: transform 0.15s ease;
}
.rich-content-viewer :deep(img:hover) {
  transform: scale(1.01);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
/* Tablo stilleri `assets/doc-table.css` içinde, `.doc-content` kapsamında —
   burada tekrar tanımlanırsa aynı tablo Docs'ta ve görevde farklı görünür. */
.rich-content-viewer :deep(input[type="checkbox"]) {
  margin-right: 0.5rem;
}
</style>

