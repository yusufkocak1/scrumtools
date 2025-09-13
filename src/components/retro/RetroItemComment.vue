<template>
  <div class="bg-white border-2 border-gray-300 rounded-xl p-4 shadow-sm hover:shadow-md transition-shadow duration-200">
    <!-- Comment Content -->
    <div class="mb-3">
      <p class="text-gray-900 leading-relaxed">{{ comment.value }}</p>
    </div>

    <!-- Comment Footer -->
    <div class="flex items-center justify-between pt-3 border-t-2 border-gray-300">
      <!-- Owner Info -->
      <div class="flex items-center gap-2">
        <span class="text-sm font-medium text-gray-700">{{ ownerName }}</span>
      </div>

      <!-- Delete Button -->
      <button
        @click="$emit('removeComment', comment.id)"
        class="group relative p-1.5 rounded-lg transition-all duration-200 hover:bg-red-50 focus:bg-red-50 active:bg-red-100 cursor-pointer focus:outline-none focus:ring-2 focus:ring-red-500"
      >
        <svg class="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script>
import {getUserFromDB} from "../../firebase/AuthService.js";

export default {
  name: "RetroItemComment",
  props: {
    comment: Object,
    ownerName: String
  },
  data() {
  },
  created() {

  },
  computed:{
  }
}
</script>