<template>
  <div
    class="rich-content-viewer prose prose-sm sm:prose max-w-none
           prose-headings:text-gray-900 prose-p:text-gray-700
           prose-a:text-blue-600 prose-a:underline
           prose-img:rounded-lg prose-img:max-w-full prose-img:shadow-sm
           prose-code:bg-gray-100 prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:text-sm
           prose-pre:bg-gray-900 prose-pre:text-gray-100
           prose-blockquote:border-l-blue-500
           prose-table:border prose-th:bg-gray-50"
    v-html="renderedHtml"
  ></div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

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

const renderedHtml = computed(() => {
  if (!props.content) return ''

  let html
  if (isHtml(props.content)) {
    // Already HTML (TipTap output) — sanitize and display
    html = props.content
  } else {
    // Legacy Markdown content — convert to HTML
    html = marked.parse(props.content, { breaks: true, gfm: true })
  }

  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['img', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
    ADD_ATTR: ['src', 'alt', 'title', 'target', 'rel', 'href', 'colspan', 'rowspan']
  })
})
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
.rich-content-viewer :deep(table) {
  border-collapse: collapse;
  width: 100%;
}
.rich-content-viewer :deep(th),
.rich-content-viewer :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 0.5rem 0.75rem;
}
.rich-content-viewer :deep(input[type="checkbox"]) {
  margin-right: 0.5rem;
}
</style>

