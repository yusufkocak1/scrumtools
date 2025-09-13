<template>
  <div class="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 mb-6">
    <div class="flex flex-wrap justify-between items-center">
      <!-- Board Title and Info -->
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center">
          <svg class="w-6 h-6 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
            <path d="M16 4c0-1.11.89-2 2-2s2 .89 2 2-.89 2-2 2-2-.89-2-2zm4 18v-6h2.5l-2.54-7.63A1.5 1.5 0 0 0 18.54 7H16c-.8 0-1.54.37-2 .95L12.58 9.7c-.35.47-.98.75-1.64.75H9.5a2 2 0 0 0-2 2v1a1 1 0 0 0 1 1H10v8h2v-8h.5c.3 0 .6-.1.85-.29L14.7 12H16l2.05 6H20z"/>
            <path d="M12.5 11.5c.83 0 1.5-.67 1.5-1.5s-.67-1.5-1.5-1.5S11 9.17 11 10s.67 1.5 1.5 1.5z"/>
          </svg>
        </div>
        <div>
          <h1 class="font-bold text-2xl text-gray-900 mb-1">{{ boardName }}</h1>
          <p class="text-sm text-gray-500">Retrospective Board Session</p>
        </div>
      </div>

      <!-- Controls -->
      <div class="flex items-center gap-4">
        <!-- Anonymous Mode Toggle -->
        <div class="flex items-center gap-3 px-4 py-3 bg-gray-50 rounded-xl">
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gray-600" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-6-3a2 2 0 11-4 0 2 2 0 014 0zm-2 4a5 5 0 00-4.546 2.916A5.986 5.986 0 0010 16a5.986 5.986 0 004.546-2.084A5 5 0 0010 11z" clip-rule="evenodd"></path>
            </svg>
            <span class="text-sm font-medium text-gray-700">Anonymous Mode</span>
          </div>

          <div class="inline-flex items-center gap-3">
            <label class="text-xs text-gray-500 cursor-pointer select-none" :for="switchId">Off</label>
            <div class="relative inline-block">
              <input
                :id="switchId"
                :checked="anonymousMode"
                @change="$emit('toggle-anonymous', $event.target.checked)"
                class="sr-only"
                type="checkbox"
              />
              <label
                :for="switchId"
                class="block w-12 h-6 bg-gray-200 rounded-full cursor-pointer transition-colors duration-300"
                :class="anonymousMode ? 'bg-red-600' : 'bg-gray-200'"
              >
                <div
                  class="absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-300 ease-in-out"
                  :class="anonymousMode ? 'translate-x-6' : 'translate-x-0'"
                ></div>
              </label>
            </div>
            <label class="text-xs text-gray-500 cursor-pointer select-none" :for="switchId">On</label>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex items-center gap-2">
          <!-- Export Button -->
          <button
            @click="$emit('export-results')"
            class="group relative flex items-center justify-center p-3 rounded-xl transition-all duration-200 hover:bg-green-50 focus:bg-green-50 active:bg-green-100 cursor-pointer border border-green-200 hover:border-green-300">
            <div class="w-5 h-5 bg-green-100 rounded-md flex items-center justify-center group-hover:bg-green-200 transition-colors">
              <svg class="w-3 h-3 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"></path>
              </svg>
            </div>
            <!-- Tooltip -->
            <div class="absolute bottom-full mb-2 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap">
              Export to JSON
            </div>
          </button>

          <!-- Settings Button -->
          <button
            @click="$emit('open-settings')"
            class="group relative flex items-center justify-center p-3 rounded-xl transition-all duration-200 hover:bg-orange-50 focus:bg-orange-50 active:bg-orange-100 cursor-pointer border border-orange-200 hover:border-orange-300">
            <div class="w-5 h-5 bg-orange-100 rounded-md flex items-center justify-center group-hover:bg-orange-200 transition-colors">
              <svg class="w-3 h-3 text-orange-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"></path>
              </svg>
            </div>
            <!-- Tooltip -->
            <div class="absolute bottom-full mb-2 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap">
              Board Settings
            </div>
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
