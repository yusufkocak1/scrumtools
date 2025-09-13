<template>
  <div class="bg-white border border-gray-200 rounded-2xl p-4 shadow-sm hover:shadow-lg transition-all duration-200 hover:border-blue-200">
    <!-- Header with Avatar and User Info -->
    <div class="flex items-start gap-3">
      <!-- Avatar -->
      <div class="flex-shrink-0">
        <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
          <span class="text-purple-600 font-bold text-sm">{{ getAvatar() }}</span>
        </div>
      </div>

      <!-- Content Area -->
      <div class="flex-1 min-w-0">
        <!-- User Info and Actions -->
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-2">
            <span class="font-semibold text-gray-900 text-sm">{{ ownerName }}</span>
          </div>

        </div>

        <!-- Comment Content -->
        <div class="">
          <p class="text-gray-900 text-sm leading-relaxed break-words">{{ comment.value }}</p>
        </div>

        <!-- Interaction Bar -->
        <div class="flex items-center justify-end gap-2 pt-1">
          <button
              @click="$emit('removeComment', comment.id)"
              class="group p-1.5 rounded-full transition-all duration-200 hover:bg-red-50 focus:bg-red-50 active:bg-red-100 cursor-pointer focus:outline-none focus:ring-2 focus:ring-red-300"
          >
            <svg class="w-3.5 h-3.5 text-gray-400 group-hover:text-red-500 transition-colors" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
            </svg>
          </button></div>
      </div>
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
    return {}
  },
  created() {

  },
  methods: {
    getAvatar(){
      let splitOwnerName = this.ownerName.split(" ")
      if(splitOwnerName.length > 1){
        return splitOwnerName[0].charAt(0).toUpperCase() + splitOwnerName[1].charAt(0).toUpperCase()
      }else {
        return splitOwnerName[0].charAt(0).toUpperCase()
      }
    }
  },
  computed:{

  }
}
</script>