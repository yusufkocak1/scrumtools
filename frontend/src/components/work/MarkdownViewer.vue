<template>
  <div
    class="markdown-viewer prose prose-sm sm:prose max-w-none
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

<script>
import { marked } from 'marked'
import DOMPurify from 'dompurify'

export default {
  name: 'MarkdownViewer',
  props: {
    content: { type: String, default: '' }
  },
  computed: {
    renderedHtml() {
      if (!this.content) return ''
      const rawHtml = marked.parse(this.content, {
        breaks: true,
        gfm: true
      })
      return DOMPurify.sanitize(rawHtml, {
        ADD_TAGS: ['img'],
        ADD_ATTR: ['src', 'alt', 'title', 'target', 'rel']
      })
    }
  }
}
</script>

<style scoped>
.markdown-viewer :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: transform 0.15s ease;
}
.markdown-viewer :deep(img:hover) {
  transform: scale(1.01);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.markdown-viewer :deep(table) {
  border-collapse: collapse;
  width: 100%;
}
.markdown-viewer :deep(th),
.markdown-viewer :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 0.5rem 0.75rem;
}
.markdown-viewer :deep(input[type="checkbox"]) {
  margin-right: 0.5rem;
}
</style>

