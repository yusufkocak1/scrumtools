<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
    </div>

    <!-- Boş Liste -->
    <div v-else-if="templates.length === 0" class="text-center py-16">
      <div class="w-24 h-24 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg class="w-12 h-12 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
      </div>
      <h3 class="text-xl font-semibold text-gray-900 mb-2">Henüz şablon yok</h3>
      <p class="text-gray-500">İlk quiz şablonunuzu oluşturarak başlayın</p>
    </div>

    <!-- Şablon Kartları -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="t in templates" :key="t.id"
           class="bg-white rounded-xl shadow-md border border-gray-200 hover:shadow-lg transition-all duration-300 overflow-hidden">
        <div class="p-6">
          <div class="flex items-start justify-between mb-3">
            <h3 class="text-lg font-bold text-gray-900 truncate flex-1">{{ t.title }}</h3>
            <span class="ml-2 px-2 py-1 bg-indigo-100 text-indigo-700 text-xs font-medium rounded-full whitespace-nowrap">
              {{ t.questionCount }} soru
            </span>
          </div>
          <p v-if="t.description" class="text-gray-500 text-sm mb-4 line-clamp-2">{{ t.description }}</p>
          <div class="flex items-center text-xs text-gray-400 mb-4">
            <span>{{ t.createdByName }}</span>
            <span class="mx-2">•</span>
            <span>{{ formatDate(t.createdAt) }}</span>
          </div>
          <div class="flex gap-2">
            <button @click="$emit('start', t.id)"
                    class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors text-sm font-medium">
              🚀 Başlat
            </button>
            <button @click="$emit('edit', t.id)"
                    class="px-3 py-2 bg-gray-100 text-gray-600 rounded-lg hover:bg-gray-200 transition-colors text-sm">
              ✏️
            </button>
            <button @click="confirmDelete(t.id, t.title)"
                    class="px-3 py-2 bg-red-50 text-red-500 rounded-lg hover:bg-red-100 transition-colors text-sm">
              🗑️
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'QuizTemplateList',
  props: {
    teamId: String,
    templates: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false }
  },
  emits: ['edit', 'delete', 'start', 'refresh'],
  methods: {
    formatDate(dateStr) {
      if (!dateStr) return ''
      return new Date(dateStr).toLocaleDateString('tr-TR', {
        year: 'numeric', month: 'short', day: 'numeric'
      })
    },
    confirmDelete(id, title) {
      if (confirm(`"${title}" şablonunu silmek istediğinize emin misiniz?`)) {
        this.$emit('delete', id)
      }
    }
  }
}
</script>

