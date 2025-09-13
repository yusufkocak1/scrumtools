<template>
  <button
    @click="copyTeamId"
    class="group relative flex items-center justify-center p-2 rounded-xl transition-all duration-200 hover:bg-purple-50 focus:bg-purple-50 active:bg-purple-100 cursor-pointer border border-purple-200 hover:border-purple-300 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
  >
    <div class="w-5 h-5 bg-purple-100 rounded-md flex items-center justify-center group-hover:bg-purple-200 transition-colors">
      <svg class="w-3 h-3 text-purple-600" fill="currentColor" viewBox="0 0 20 20">
        <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z"/>
      </svg>
    </div>

    <!-- Tooltip -->
    <div class="absolute bottom-full mb-2 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap">
      Invite to Team
    </div>
  </button>
</template>

<script>
import {createToast} from "mosha-vue-toastify";

export default {
  name: "InviteToTheTeam",
  props: {
    teamId: String
  },
  methods: {
    copyTeamId() {
      navigator.clipboard.writeText(this.teamId)
      createToast("Team ID copied to clipboard", {
        type: "success",
        position: "top-center"
      })
    },
  }
}
</script>
