<template>
  <div class="bg-white rounded-2xl w-full shadow-sm border border-gray-200 p-4 sm:p-5 mb-4 sm:mb-6">
    <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-3 sm:gap-4">
      <!-- Board Title and Info -->
      <div class="flex items-center gap-3 min-w-0">
        <div class="w-10 h-10 sm:w-12 sm:h-12 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center flex-shrink-0 shadow-sm">
          <svg class="w-5 h-5 sm:w-6 sm:h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
            <path d="M16 4c0-1.11.89-2 2-2s2 .89 2 2-.89 2-2 2-2-.89-2-2zm4 18v-6h2.5l-2.54-7.63A1.5 1.5 0 0 0 18.54 7H16c-.8 0-1.54.37-2 .95L12.58 9.7c-.35.47-.98.75-1.64.75H9.5a2 2 0 0 0-2 2v1a1 1 0 0 0 1 1H10v8h2v-8h.5c.3 0 .6-.1.85-.29L14.7 12H16l2.05 6H20z"/>
            <path d="M12.5 11.5c.83 0 1.5-.67 1.5-1.5s-.67-1.5-1.5-1.5S11 9.17 11 10s.67 1.5 1.5 1.5z"/>
          </svg>
        </div>
        <div class="min-w-0">
          <h1 class="font-bold text-lg sm:text-2xl text-gray-900 truncate">{{ boardName }}</h1>
          <p class="text-xs sm:text-sm text-gray-500 flex items-center gap-1.5">
            Retrospective Board
            <span v-if="memberCount > 0" class="inline-flex items-center gap-1 text-gray-400">
              <span class="w-1 h-1 rounded-full bg-gray-300"></span>
              {{ memberCount }} member{{ memberCount === 1 ? '' : 's' }}
            </span>
          </p>
        </div>
      </div>

      <!-- Controls -->
      <div class="flex flex-wrap items-center gap-2 sm:gap-3">
        <!-- Anonymous Mode Toggle -->
        <label
          :for="switchId"
          class="flex items-center gap-2.5 px-3 py-2 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded-xl cursor-pointer select-none transition-colors"
        >
          <svg class="w-4 h-4 flex-shrink-0 transition-colors" :class="anonymousMode ? 'text-red-500' : 'text-gray-500'" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-6-3a2 2 0 11-4 0 2 2 0 014 0zm-2 4a5 5 0 00-4.546 2.916A5.986 5.986 0 0010 16a5.986 5.986 0 004.546-2.084A5 5 0 0010 11z" clip-rule="evenodd"></path>
          </svg>
          <span class="text-xs sm:text-sm font-medium text-gray-700 whitespace-nowrap">Anonymous</span>
          <input
            :id="switchId"
            :checked="anonymousMode"
            @change="$emit('toggle-anonymous', $event.target.checked)"
            class="sr-only"
            type="checkbox"
          />
          <span
            class="relative block w-10 h-6 rounded-full transition-colors duration-300 flex-shrink-0"
            :class="anonymousMode ? 'bg-red-500' : 'bg-gray-300'"
          >
            <span
              class="absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-300 ease-in-out"
              :class="anonymousMode ? 'translate-x-4' : 'translate-x-0'"
            ></span>
          </span>
        </label>

        <!-- Action Buttons -->
        <div class="flex items-center gap-2 ml-auto lg:ml-0">
          <!-- Export Button -->
          <button
            @click="$emit('export-results')"
            title="Export to JSON"
            class="group relative flex items-center gap-2 px-3 py-2 rounded-xl transition-all duration-200 bg-white hover:bg-emerald-50 active:bg-emerald-100 cursor-pointer border border-gray-200 hover:border-emerald-300">
            <svg class="w-4 h-4 text-gray-500 group-hover:text-emerald-600 transition-colors" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"></path>
            </svg>
            <span class="hidden sm:inline text-xs font-medium text-gray-600 group-hover:text-emerald-700 transition-colors">Export</span>
          </button>

          <!-- Settings Button -->
          <button
            @click="$emit('open-settings')"
            title="Board Settings"
            class="group relative flex items-center gap-2 px-3 py-2 rounded-xl transition-all duration-200 bg-white hover:bg-indigo-50 active:bg-indigo-100 cursor-pointer border border-gray-200 hover:border-indigo-300">
            <svg class="w-4 h-4 text-gray-500 group-hover:text-indigo-600 transition-colors" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"></path>
            </svg>
            <span class="hidden sm:inline text-xs font-medium text-gray-600 group-hover:text-indigo-700 transition-colors">Settings</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RetroBoardHeader',
  props: {
    boardName: {
      type: String,
      default: ''
    },
    memberCount: {
      type: Number,
      default: 0
    },
    anonymousMode: {
      type: Boolean,
      default: false
    }
  },
  emits: ['toggle-anonymous', 'export-results', 'open-settings'],
  computed: {
    switchId() {
      return `anonymous-switch-${Math.random().toString(36).substr(2, 9)}`;
    }
  }
}
</script>
