<template>
  <div class="w-full">
    <!-- Top Row: Task ID / Priority -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between mb-2 gap-2 sm:gap-0">
      <span class="text-[11px] sm:text-xs font-mono text-gray-500">{{ task.customId || task.id }}</span>
      <div class="flex items-center space-x-1 self-start sm:self-auto">
        <div class="w-4 h-4 rounded bg-blue-100 flex items-center justify-center">
          <svg class="w-3 h-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>
          </svg>
        </div>
        <span
          class="inline-flex items-center px-2 py-0.5 rounded text-[10px] sm:text-xs font-medium"
          :class="getPriorityClass(task.priority)"
        >
          {{ task.priority || 'Medium' }}
        </span>
      </div>
    </div>

    <!-- Title -->
    <h4 class="text-sm sm:text-base font-medium text-gray-900 mb-1 sm:mb-2 line-clamp-2">
      {{ task.title }}
    </h4>

    <!-- Description -->
    <p v-if="task.description" class="text-[11px] sm:text-xs text-gray-600 mb-2 sm:mb-3 line-clamp-2">
      {{ task.description }}
    </p>

    <!-- Labels -->
    <div v-if="task.labels && task.labels.length" class="flex flex-wrap gap-1 mb-2 sm:mb-3">
      <span
        v-for="(label,idx) in task.labels.slice(0, 2)"
        :key="label"
        class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] sm:text-xs font-medium bg-indigo-100 text-indigo-800"
        :class="{ 'hidden xs:inline-flex': idx === 1 }"
      >
        {{ label }}
      </span>
      <span v-if="task.labels.length > 2" class="text-[10px] sm:text-xs text-gray-400">
        +{{ task.labels.length - 2 }}
      </span>
    </div>

    <!-- Footer -->
    <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
      <!-- Assignee -->
      <div class="flex items-center space-x-2 min-w-0">
        <div v-if="task.assignee" class="flex items-center space-x-1 min-w-0">
          <div class="w-6 h-6 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-[10px] sm:text-xs font-medium">
            {{ getInitials(task.assignee) }}
          </div>
          <span class="text-[10px] sm:text-xs text-gray-600 truncate max-w-[80px]">{{ task.assignee }}</span>
        </div>
        <div v-else class="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center">
          <svg class="w-3 h-3 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
          </svg>
        </div>
      </div>

      <!-- Metrics -->
      <div class="flex items-center space-x-3 text-[10px] sm:text-xs">
        <div v-if="task.storyPoints" class="flex items-center space-x-1">
          <svg class="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
          </svg>
          <span class="text-gray-500">{{ task.storyPoints }}</span>
        </div>

        <div v-if="task.attachments && task.attachments.length" class="flex items-center space-x-1">
          <svg class="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/>
          </svg>
          <span class="text-gray-500">{{ task.attachments.length }}</span>
        </div>

        <div v-if="task.comments && task.comments.length" class="flex items-center space-x-1">
          <svg class="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
          </svg>
          <span class="text-gray-500">{{ task.comments.length }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TaskCard',
  props: {
    task: {
      type: Object,
      required: true
    }
  },
  methods: {
    getInitials(name) {
      if (!name) return '';
      return name.split(' ').map(word => word[0]).join('').toUpperCase().slice(0, 2);
    },

    getPriorityClass(priority) {
      const priorityMap = {
        'High': 'bg-red-100 text-red-800',
        'Medium': 'bg-yellow-100 text-yellow-800',
        'Low': 'bg-green-100 text-green-800',
        'Critical': 'bg-purple-100 text-purple-800'
      };
      return priorityMap[priority] || 'bg-gray-100 text-gray-800';
    }
  }
};
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
