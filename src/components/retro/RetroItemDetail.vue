<template>
  <div class="relative flex flex-col bg-white rounded-2xl shadow-xl border-2 border-gray-300 lg:w-[30%] mx-4 overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between p-4 border-b-2 border-gray-300 bg-gray-50">
      <h3 class="font-semibold text-gray-900">Retro Item</h3>
      <button
        @click="$emit('close')"
        class="p-2 rounded-xl hover:bg-red-50 text-red-600 transition-colors focus:outline-none focus:ring-2 focus:ring-red-500"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- Content -->
    <div class="flex-1 p-6 space-y-4">
      <!-- Item Content -->
      <div>
        <div v-if="!edit"
             @click="edit=true"
             class="p-4 bg-gray-50 rounded-xl border-2 border-gray-300 cursor-pointer hover:bg-gray-100 transition-colors">
          <p class="text-gray-900">{{ item.value }}</p>
        </div>
        <div v-else class="space-y-2">
          <textarea
            v-model="item.value"
            @blur="updateItem(); edit = false"
            @keydown.esc="edit = false"
            class="w-full min-h-[100px] p-3 border-2 border-gray-400 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
            placeholder="Enter item content..."
          />
          <div class="flex gap-2">
            <button
              @click="updateItem(); edit = false"
              class="px-3 py-1 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors"
            >
              Save
            </button>
            <button
              @click="edit = false"
              class="px-3 py-1 bg-gray-200 text-gray-700 text-sm rounded-lg hover:bg-gray-300 transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>

      <!-- Comments -->
      <div class="space-y-3">
        <h4 class="font-medium text-gray-900">Comments</h4>
        <div class="space-y-2 max-h-48 overflow-y-auto">
          <RetroItemComment
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            :ownerName="members[comment.owner]?.displayName || 'Unknown'"
            @removeComment="removeComment"
          />
        </div>

        <!-- Add Comment -->
        <div class="flex gap-2 pt-2 border-t-2 border-gray-300">
          <input
            v-model="commentInput"
            @keydown.enter="addComment()"
            class="flex-1 px-3 py-2 border-2 border-gray-400 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Add comment..."
            type="text"
          />
          <button
            @click="addComment()"
            :disabled="!commentInput.trim()"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            +
          </button>
        </div>
      </div>
    </div>

    <!-- Status Actions -->
    <div class="p-4 border-t-2 border-gray-300 bg-gray-50">
      <div class="flex justify-end gap-3">
        <button
          @click="setStatus('ACCEPTED')"
          class="w-8 h-8 bg-green-600 hover:bg-green-700 rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-green-500"
          title="Accept"
        />
        <button
          @click="setStatus('REJECTED')"
          class="w-8 h-8 bg-red-600 hover:bg-red-700 rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-red-500"
          title="Reject"
        />
        <button
          @click="setStatus('CONFLICT')"
          class="w-8 h-8 bg-amber-600 hover:bg-amber-700 rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-amber-500"
          title="Conflict"
        />
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
        createToast('comment added ', {type: 'success', position: 'top-center'})
        this.getRetroItemComments()
        this.commentInput = ""
      })
    },

    removeComment(commentId) {
      removeRetroItemComment(this.teamId, this.boardId, this.item.column, this.item.id, commentId).then(() => {
        createToast('comment removed ', {type: 'success', position: 'top-center'})
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
    }
  },
  created() {
    this.getRetroItemComments()
  }
}
</script>
