<template>
  <div :class="['bg-white border border-gray-200 rounded-2xl p-4 shadow-sm hover:shadow-lg transition-all duration-200 hover:border-blue-200 mb-3', getBorderColorByStatus]">
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
        <!-- User Info -->
        <div class="flex items-center gap-2 mb-2">
          <span class="font-semibold text-gray-900 text-sm">{{ ownerName || 'Anonymous' }}</span>
        </div>

        <!-- Item Content -->
        <div class="mb-3">
          <p @click="openDetail" class="text-gray-900 text-sm leading-relaxed break-words cursor-pointer hover:text-blue-600 transition-colors">
            {{ item.value }}
          </p>
        </div>

        <!-- Interaction Bar -->
        <div class="flex items-center justify-between pt-2 border-t border-gray-100">
          <!-- Voting Section -->
          <div class="flex items-center gap-2">
            <button
              @click="addVote(1)"
              :class="['flex items-center gap-1 px-2 py-1 rounded-lg text-xs transition-colors',
                       isThereUpVote ? 'bg-green-100 text-green-700' : 'text-gray-500 hover:bg-green-50 hover:text-green-600']"
            >
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M3.293 9.707a1 1 0 010-1.414l6-6a1 1 0 011.414 0l6 6a1 1 0 01-1.414 1.414L11 5.414V17a1 1 0 11-2 0V5.414L4.707 9.707a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
              </svg>
            </button>

            <span class="text-sm font-medium text-gray-700 min-w-[20px] text-center">
              {{ getVoteCount }}
            </span>

            <button
              @click="addVote(-1)"
              :class="['flex items-center gap-1 px-2 py-1 rounded-lg text-xs transition-colors',
                       isThereDownVote ? 'bg-red-100 text-red-700' : 'text-gray-500 hover:bg-red-50 hover:text-red-600']"
            >
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 10.293a1 1 0 010 1.414l-6 6a1 1 0 01-1.414 0l-6-6a1 1 0 111.414-1.414L9 14.586V3a1 1 0 012 0v11.586l4.293-4.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
              </svg>
            </button>
          </div>

          <!-- Actions -->
          <div class="flex items-center gap-1">
            <button
              @click="openDetail"
              class="flex items-center gap-1 text-gray-500 hover:text-blue-500 transition-colors text-xs px-2 py-1 rounded-lg hover:bg-blue-50"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-3.582 8-8 8a8.955 8.955 0 01-4.126-.98L3 20l1.98-5.126A8.955 8.955 0 013 12c0-4.418 3.582-8 8-8s8 3.582 8 8z"/>
              </svg>
              <span>Comment</span>
            </button>

            <button
              v-if="isOwner"
              @click="$emit('removeItem', item.id)"
              class="group p-1.5 rounded-full transition-all duration-200 hover:bg-red-50 focus:bg-red-50 active:bg-red-100 cursor-pointer focus:outline-none focus:ring-2 focus:ring-red-300"
            >
              <svg class="w-3.5 h-3.5 text-gray-400 group-hover:text-red-500 transition-colors" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {getUserFromDB} from "../../firebase/AuthService.js";
import {getVotes, removeVote} from "../../firebase/RetroBoardService.js";

export default {
  name: "RetroItem",
  data() {
    return {
      owner: "",
      votes: [],
      isDestroyed: false,
      lastVoteTime: 0
    }
  },
  props: {
    item: Object,
    isAdmin: false,
    teamId: String,
    boardId: String,
    column: String,
    ownerName: String
  },
  watch: {
    // Watch for item changes to update votes efficiently
    'item.votes': {
      handler(newVotes) {
        if (newVotes && Array.isArray(newVotes)) {
          this.votes = newVotes
        }
      },
      immediate: true
    }
  },
  methods: {
    openDetail() {
      // Throttle detail opening
      const now = Date.now()
      if (now - this.lastVoteTime < 200) {
        return
      }
      this.lastVoteTime = now

      this.$emit('openDetail', this.item)
    },
    getAvatar(){
      if (!this.ownerName) return 'A'

      let splitOwnerName = this.ownerName.split(" ")
      if(splitOwnerName.length > 1){
        return splitOwnerName[0].charAt(0).toUpperCase() + splitOwnerName[1].charAt(0).toUpperCase()
      }else {
        return splitOwnerName[0].charAt(0).toUpperCase()
      }
    },
    addVote(voteValue) {
      // Prevent rapid fire voting
      const now = Date.now()
      if (now - this.lastVoteTime < 300) {
        return
      }
      this.lastVoteTime = now

      const vote = {
        value: voteValue,
        owner: localStorage.getItem('user')
      }

      if (voteValue === 1 && this.isThereUpVote) {
        removeVote(this.teamId, this.boardId, this.column, this.item.id, localStorage.getItem('user'))
      } else if (voteValue === -1 && this.isThereDownVote) {
        removeVote(this.teamId, this.boardId, this.column, this.item.id, localStorage.getItem('user'))
      } else {
        this.$emit('addVote', this.item.id, vote)
      }
    }
  },
  computed: {
    isOwner() {
      return this.item.owner === localStorage.getItem('user') || this.isAdmin
    },
    getVoteCount() {
      if (!this.votes || !Array.isArray(this.votes)) return 0
      return this.votes.map(vote => vote.value).reduce((a, b) => a + b, 0)
    },
    isThereUpVote() {
      if (!this.votes || !Array.isArray(this.votes)) return false
      return this.votes.some(vote => vote.value === 1 && vote.owner === localStorage.getItem('user'))
    },
    isThereDownVote() {
      if (!this.votes || !Array.isArray(this.votes)) return false
      return this.votes.some(vote => vote.value === -1 && vote.owner === localStorage.getItem('user'))
    },
    getStatusColor() {
      if (this.item.status === "ACCEPTED") {
        return "bg-green-100 text-green-700"
      } else if (this.item.status === "REJECTED") {
        return "bg-red-100 text-red-700"
      } else if (this.item.status === "CONFLICT") {
        return "bg-amber-100 text-amber-700"
      }
      return ""
    },
    getStatusText() {
      if (this.item.status === "ACCEPTED") {
        return "Kabul Edildi"
      } else if (this.item.status === "REJECTED") {
        return "Reddedildi"
      } else if (this.item.status === "CONFLICT") {
        return "Tartışmalı"
      }
      return ""
    },
    getBorderColorByStatus() {
      if (!this.item.status) {
        return "border-slate-200"
      } else if (this.item.status === "ACCEPTED") {
        return " border-l-4 border-l-green-700"
      } else if (this.item.status === "REJECTED") {
        return "border-l-4 border-l-red-700"
      } else if (this.item.status === "CONFLICT") {
        return "border-l-4 border-l-amber-700"
      } else {
        return "border-slate-200"
      }
    }
  },
  created() {
    if (this.item.owner === "Anonymous") {
      this.owner = "Anonymous"
    }
    // Votes are now handled via props/watch instead of individual listeners
    // This eliminates the major performance bottleneck
  },
  beforeUnmount() {
    this.isDestroyed = true
    // No more individual listeners to clean up
  }
}
</script>
