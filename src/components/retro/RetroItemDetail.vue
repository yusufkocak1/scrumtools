<template>
  <div class="relative flex flex-col bg-white rounded-2xl shadow-xl border border-gray-200 lg:w-[30%] mx-4 overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between p-4 border-b border-gray-200 bg-gray-50">
      <h3 class="font-semibold text-gray-900">Retro Item Details</h3>
      <button
        @click="$emit('close')"
        class="group p-2 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all focus:outline-none focus:ring-2 focus:ring-red-500"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- Main Item Display -->
    <div class="p-4 border-b border-gray-100">
      <div class="bg-white border border-gray-200 rounded-2xl p-4 shadow-sm">
        <!-- Header with Avatar and User Info -->
        <div class="flex items-start gap-3 mb-3">
          <!-- Avatar -->
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <span class="text-purple-600 font-bold text-sm">{{getAvatar()}}</span>
            </div>
          </div>

          <!-- Content Area -->
          <div class="flex-1 min-w-0">
            <!-- User Info -->
            <div class="flex items-center gap-2 mb-2">
              <span class="font-semibold text-gray-900 text-sm">{{ getItemOwnerName() }}</span>
            </div>

            <!-- Item Content -->
            <div class="mb-3">
              <div v-if="!edit"
                   @click="edit=true"
                   class="text-gray-900 text-sm leading-relaxed break-words cursor-pointer hover:text-blue-600 transition-colors">
                {{ item.value }}
              </div>
              <div v-else class="space-y-2">
                <textarea
                  v-model="item.value"
                  @blur="updateItem(); edit = false"
                  @keydown.esc="edit = false"
                  class="w-full min-h-[80px] p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none text-sm"
                  placeholder="Enter item content..."
                />
                <div class="flex gap-2">
                  <button
                    @click="updateItem(); edit = false"
                    class="px-3 py-1 bg-blue-600 text-white text-xs rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    Save
                  </button>
                  <button
                    @click="edit = false"
                    class="px-3 py-1 bg-gray-200 text-gray-700 text-xs rounded-lg hover:bg-gray-300 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Comments Section -->
    <div class="flex-1 p-4 overflow-hidden border-t ">
      <div class="space-y-3 h-full flex flex-col">
        <h4 class="font-medium text-gray-900 text-sm">Comments</h4>

        <!-- Comments List -->
        <div class="flex-1 space-y-3 overflow-y-auto">
          <RetroItemComment
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            :ownerName="members[comment.owner]?.displayName || 'Unknown'"
            @removeComment="removeComment"
          />
        </div>

        <!-- Add Comment -->
        <div class="flex gap-2 pt-3 border-t border-gray-200">
          <div class="flex-shrink-0">
            <div class="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
              <span class="text-purple-600 font-bold text-xs">{{ getCurrentUserInitials() }}</span>
            </div>
          </div>
          <div class="flex-1 flex gap-2">
            <input
              v-model="commentInput"
              @keydown.enter="addComment()"
              class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
              placeholder="Add comment..."
              type="text"
            />
            <button
              @click="addComment()"
              :disabled="!commentInput.trim()"
              class="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 transition-all disabled:opacity-50 disabled:cursor-not-allowed text-sm"
            >
              Post
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Status Actions -->
    <div class="p-4 border-t border-gray-200 bg-gray-50">
      <div class="flex items-center justify-end">
        <div class="flex gap-2">
          <button
            @click="setStatus('ACCEPTED')"
            class="flex items-center gap-1 px-3 py-1.5 bg-green-100 text-green-700 hover:bg-green-200 rounded-lg transition-colors text-xs font-medium"
            title="Accept"
          >
            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
            Accept
          </button>
          <button
            @click="setStatus('REJECTED')"
            class="flex items-center gap-1 px-3 py-1.5 bg-red-100 text-red-700 hover:bg-red-200 rounded-lg transition-colors text-xs font-medium"
            title="Reject"
          >
            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
            Reject
          </button>
          <button
            @click="setStatus('CONFLICT')"
            class="flex items-center gap-1 px-3 py-1.5 bg-amber-100 text-amber-700 hover:bg-amber-200 rounded-lg transition-colors text-xs font-medium"
            title="Conflict"
          >
            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
            </svg>
            Conflict
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  createRetroItemComment,
  getRetroItemComments,
  removeRetroItemComment, updateRetroItem,
  updateRetroItemStatus
} from "../../firebase/RetroBoardService.js";
import {createToast} from "mosha-vue-toastify";
import RetroItemComment from "./RetroItemComment.vue";

export default {
  name: "RetroItemDetail",
  components: {RetroItemComment},
  props: {
    item: Object,
    members: Array,
    boardId: String,
    teamId: String
  },
  data() {
    return {
      commentInput: "",
      comments: [],
      edit: false
    }
  },
  methods: {
    closeDetail() {
      this.$emit('closeDetail')
    },
    removeItem() {
      this.$emit('removeItem')
    },
    updateItem() {
      updateRetroItem(this.teamId, this.boardId, this.item.column, this.item.id, this.item.value)
    },
    addComment() {
      const newComment = {
        value: this.commentInput,
        owner: localStorage.getItem("user")
      }
      createRetroItemComment(this.teamId, this.boardId, this.item.column, this.item.id, newComment).then(() => {
        createToast('Comment added', {type: 'success', position: 'top-center'})
        this.getRetroItemComments()
        this.commentInput = ""
      })
    },
    removeComment(commentId) {
      removeRetroItemComment(this.teamId, this.boardId, this.item.column, this.item.id, commentId).then(() => {
        createToast('Comment removed', {type: 'success', position: 'top-center'})
        this.getRetroItemComments()
      })
    },
    getRetroItemComments() {
      getRetroItemComments(this.teamId, this.boardId, this.item.column, this.item.id, (comments) => {
        this.comments = comments
      })
    },
    setStatus(status) {
      updateRetroItemStatus(this.teamId, this.boardId, this.item.column, this.item.id, status)
    },
    getItemOwnerName() {
      return this.members[this.item.owner]?.displayName || 'Anonymous'
    },
    getAvatar(){
      let splitOwnerName = this.getItemOwnerName().split(" ")
      if(splitOwnerName.length > 1){
        return splitOwnerName[0].charAt(0).toUpperCase() + splitOwnerName[1].charAt(0).toUpperCase()
      }else {
        return splitOwnerName[0].charAt(0).toUpperCase()
      }
    },
    getCurrentUserInitials() {
      const currentUser = localStorage.getItem('user')
      const user = this.members[currentUser]
      if (!user?.displayName) return 'U'

      const words = user.displayName.trim().split(' ').filter(word => word.length > 0);
      if (words.length === 1) {
        return words[0].charAt(0).toUpperCase();
      } else if (words.length >= 2) {
        return (words[0].charAt(0) + words[words.length - 1].charAt(0)).toUpperCase();
      }
      return 'U';
    }
  },
  created() {
    this.getRetroItemComments()
  }
}
</script>
