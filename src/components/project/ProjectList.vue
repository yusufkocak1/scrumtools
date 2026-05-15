<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
    <!-- Yeni Proje Kartı -->
    <button
      @click="$emit('create')"
      class="group border-2 border-dashed border-gray-300 rounded-xl p-6 flex flex-col items-center justify-center gap-3 hover:border-indigo-400 hover:bg-indigo-50 transition-all cursor-pointer min-h-[140px]"
    >
      <div class="w-12 h-12 rounded-full bg-indigo-100 flex items-center justify-center group-hover:scale-110 transition-transform">
        <svg class="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
      </div>
      <span class="text-sm text-gray-600 font-medium group-hover:text-indigo-600 transition-colors">Yeni Proje</span>
    </button>

    <!-- Proje Kartları -->
    <div
      v-for="project in projects"
      :key="project.id"
      @click="$emit('select', project)"
      class="bg-white rounded-xl border border-gray-200 p-5 cursor-pointer hover:shadow-lg hover:border-indigo-300 transition-all group"
    >
      <div class="flex items-start justify-between mb-3">
        <div class="flex items-center gap-3">
          <div
            class="w-10 h-10 rounded-lg flex items-center justify-center text-white font-bold text-sm flex-shrink-0"
            :style="{ backgroundColor: project.color || '#6366f1' }"
          >
            {{ project.key?.charAt(0) || '?' }}
          </div>
          <div class="min-w-0">
            <h3 class="font-semibold text-gray-900 text-sm group-hover:text-indigo-600 transition-colors truncate">
              {{ project.name }}
            </h3>
            <span class="text-xs text-gray-400 font-mono">{{ project.key }}</span>
          </div>
        </div>
        <span
          class="text-xs px-2 py-0.5 rounded-full flex-shrink-0 ml-2"
          :class="typeBadgeClass(project.projectType)"
        >{{ formatType(project.projectType) }}</span>
      </div>

      <p v-if="project.description" class="text-xs text-gray-500 line-clamp-2 mb-3">
        {{ project.description }}
      </p>

      <div class="flex items-center justify-between text-xs text-gray-400 mt-auto">
        <div class="flex items-center gap-1">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          {{ project.memberCount }} üye
        </div>
        <span
          class="px-1.5 py-0.5 rounded text-xs font-medium"
          :class="project.status === 'ACTIVE' ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-500'"
        >{{ project.status === 'ACTIVE' ? 'Aktif' : project.status }}</span>
      </div>
    </div>

    <div v-if="loading && projects.length === 0" class="col-span-full text-center py-12 text-gray-400">
      <div class="w-8 h-8 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin mx-auto mb-3"></div>
      Projeler yükleniyor...
    </div>
    <div v-else-if="!loading && projects.length === 0" class="col-span-full text-center py-12">
      <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-3">
        <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
        </svg>
      </div>
      <p class="text-gray-400 text-sm">Henüz proje yok. İlk projenizi oluşturun!</p>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  projects: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

defineEmits(['create', 'select'])

function formatType(type) {
  const map = { SCRUM: 'Scrum', KANBAN: 'Kanban', BUG_TRACKING: 'Bug', CUSTOM: 'Özel' }
  return map[type] || type
}

function typeBadgeClass(type) {
  if (type === 'SCRUM') return 'bg-blue-100 text-blue-700'
  if (type === 'KANBAN') return 'bg-purple-100 text-purple-700'
  if (type === 'BUG_TRACKING') return 'bg-red-100 text-red-700'
  return 'bg-gray-100 text-gray-600'
}
</script>
