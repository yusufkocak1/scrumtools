<template>
  <div class="flex flex-col bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[85vh] overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between gap-2 px-4 py-3 border-b border-gray-200 bg-gray-50 flex-shrink-0">
      <div class="flex items-center gap-2 min-w-0">
        <h3 class="font-semibold text-gray-900 truncate">{{ item?.column || 'Retro Item' }}</h3>
        <span v-if="item?.status" :class="['px-2 py-0.5 rounded-full text-xs font-medium flex-shrink-0', statusBadgeClass]">
          {{ statusText }}
        </span>
      </div>
      <button
        @click="$emit('close')"
        class="p-2 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all focus:outline-none focus:ring-2 focus:ring-red-500 flex-shrink-0"
        title="Close (Esc)"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- Scrollable Body -->
    <div class="flex-1 overflow-y-auto">
      <!-- Item -->
      <div class="p-4 border-b border-gray-100">
        <div class="flex items-start gap-3">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <span class="text-purple-600 font-bold text-sm">{{ getAvatar() }}</span>
            </div>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="font-semibold text-gray-900 text-sm">{{ getItemOwnerName() }}</span>
              <span v-if="createdAtText" class="text-xs text-gray-400">{{ createdAtText }}</span>
            </div>

            <!-- Content -->
            <div class="mt-2">
              <div v-if="!edit" class="group flex items-start gap-2">
                <p class="text-gray-900 text-sm leading-relaxed break-words whitespace-pre-wrap flex-1">
                  {{ item.value }}
                </p>
                <button
                  v-if="canEdit"
                  @click="startEdit"
                  class="p-1.5 rounded-lg text-gray-300 hover:text-blue-500 hover:bg-blue-50 transition-colors flex-shrink-0"
                  title="Edit"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                  </svg>
                </button>
              </div>
              <div v-else class="space-y-2">
                <textarea
                  ref="editArea"
                  v-model="editValue"
                  @keydown.esc.stop="cancelEdit"
                  class="w-full min-h-[80px] p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none text-sm"
                  placeholder="Enter item content..."
                />
                <div class="flex gap-2">
                  <button
                    @click="saveEdit"
                    :disabled="!editValue.trim()"
                    class="px-3 py-1.5 bg-blue-600 text-white text-xs rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Save
                  </button>
                  <button
                    @click="cancelEdit"
                    class="px-3 py-1.5 bg-gray-200 text-gray-700 text-xs rounded-lg hover:bg-gray-300 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>

            <!-- Meta: vote score -->
            <div class="flex items-center gap-1.5 mt-3 text-xs text-gray-500">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M3.293 9.707a1 1 0 010-1.414l6-6a1 1 0 011.414 0l6 6a1 1 0 01-1.414 1.414L11 5.414V17a1 1 0 11-2 0V5.414L4.707 9.707a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
              </svg>
              <span>{{ voteScore }} {{ Math.abs(voteScore) === 1 ? 'vote' : 'votes' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Comments -->
      <div class="p-4">
        <h4 class="font-medium text-gray-900 text-sm mb-3">
          Comments <span class="text-gray-400 font-normal">({{ comments.length }})</span>
        </h4>

        <div v-if="comments.length" class="space-y-2">
          <RetroItemComment
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            :ownerName="resolveOwnerName(comment.owner)"
            :can-delete="canDeleteComment(comment)"
            @removeComment="removeComment"
          />
        </div>
        <p v-else class="text-sm text-gray-400 text-center py-6">
          No comments yet. Start the discussion!
        </p>
      </div>
    </div>

    <!-- Add Comment (pinned) -->
    <div class="flex gap-2 px-4 py-3 border-t border-gray-200 flex-shrink-0">
      <div class="flex-shrink-0">
        <div class="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
          <span class="text-purple-600 font-bold text-xs">{{ getCurrentUserInitials() }}</span>
        </div>
      </div>
      <div class="flex-1 flex gap-2">
        <input
          v-model="commentInput"
          @keydown.enter="addComment()"
          class="flex-1 min-w-0 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
          placeholder="Add comment..."
          type="text"
        />
        <button
          @click="addComment()"
          :disabled="!commentInput.trim()"
          class="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 transition-all disabled:opacity-50 disabled:cursor-not-allowed text-sm flex-shrink-0"
        >
          Post
        </button>
      </div>
    </div>

    <!-- Status Actions -->
    <div class="flex items-center justify-between gap-2 px-4 py-3 border-t border-gray-200 bg-gray-50 flex-shrink-0">
      <span class="text-xs text-gray-500 font-medium">Resolution</span>
      <div class="flex gap-2">
        <button
          @click="setStatus('ACCEPTED')"
          :class="['flex items-center gap-1 px-3 py-1.5 rounded-lg transition-colors text-xs font-medium',
                   item?.status === 'ACCEPTED'
                     ? 'bg-green-600 text-white'
                     : 'bg-green-100 text-green-700 hover:bg-green-200']"
          title="Accept"
        >
          <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
          </svg>
          Accept
        </button>
        <button
          @click="setStatus('REJECTED')"
          :class="['flex items-center gap-1 px-3 py-1.5 rounded-lg transition-colors text-xs font-medium',
                   item?.status === 'REJECTED'
                     ? 'bg-red-600 text-white'
                     : 'bg-red-100 text-red-700 hover:bg-red-200']"
          title="Reject"
        >
          <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
          </svg>
          Reject
        </button>
        <button
          @click="setStatus('CONFLICT')"
          :class="['flex items-center gap-1 px-3 py-1.5 rounded-lg transition-colors text-xs font-medium',
                   item?.status === 'CONFLICT'
                     ? 'bg-amber-500 text-white'
                     : 'bg-amber-100 text-amber-700 hover:bg-amber-200']"
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
</template>

<script>
import {
  addComment as apiAddComment,
  getComments,
  deleteComment,
  updateItem as apiUpdateItem,
  updateItemStatus
} from "../../api/RetroBoardApi.js";
import {createToast} from "mosha-vue-toastify";
import RetroItemComment from "./RetroItemComment.vue";

export default {
  name: "RetroItemDetail",
  components: {RetroItemComment},
  props: {
    item: Object,
    members: Array,
    boardId: String,
    teamId: String,
    isAdmin: {type: Boolean, default: false}
  },
  data() {
    return {
      commentInput: "",
      comments: [],
      edit: false,
      editValue: "",
      commentsLoadedForItem: ""
    }
  },
  computed: {
    currentUser() {
      return localStorage.getItem('user') || ''
    },
    canEdit() {
      return this.isAdmin || this.item?.owner === this.currentUser
    },
    voteScore() {
      const votes = this.item?.votes
      if (!Array.isArray(votes)) return 0
      return votes.reduce((sum, v) => sum + (v.value || 0), 0)
    },
    createdAtText() {
      if (!this.item?.createdAt) return ''
      const d = new Date(this.item.createdAt)
      if (isNaN(d.getTime())) return ''
      return d.toLocaleString(undefined, {day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'})
    },
    statusText() {
      const map = {ACCEPTED: 'Accepted', REJECTED: 'Rejected', CONFLICT: 'Conflict'}
      return map[this.item?.status] || ''
    },
    statusBadgeClass() {
      const map = {
        ACCEPTED: 'bg-green-100 text-green-700',
        REJECTED: 'bg-red-100 text-red-700',
        CONFLICT: 'bg-amber-100 text-amber-700'
      }
      return map[this.item?.status] || 'bg-gray-100 text-gray-600'
    }
  },
  methods: {
    startEdit() {
      this.editValue = this.item?.value || ""
      this.edit = true
      this.$nextTick(() => this.$refs.editArea?.focus())
    },
    cancelEdit() {
      this.edit = false
      this.editValue = ""
    },
    saveEdit() {
      const value = this.editValue.trim()
      if (!value) return
      if (value === this.item.value) {
        this.edit = false
        return
      }
      apiUpdateItem(this.teamId, this.boardId, this.item.column, this.item.id, value)
        .then(() => {
          this.item.value = value
          this.edit = false
          createToast('Item updated', {type: 'success', position: 'top-center'})
        })
        .catch((err) => {
          console.error('updateItem error', err)
          createToast('Failed to update item.', {type: 'danger', position: 'top-center'})
        })
    },
    addComment() {
      if (!this.commentInput.trim()) return;
      const value = this.commentInput.trim();
      this.commentInput = "";

      apiAddComment(this.teamId, this.boardId, this.item.column, this.item.id, value)
        .then((updatedItem) => {
          // addComment response'u güncel comments listesini içeriyor → anında güncelle
          if (updatedItem?.comments && Array.isArray(updatedItem.comments)) {
            this.comments = updatedItem.comments;
            this.commentsLoadedForItem = this.item.id;
          } else {
            // fallback: ayrı GET ile çek
            this.fetchComments(true);
          }
          createToast('Comment added', {type: 'success', position: 'top-center'});
        })
        .catch((err) => {
          console.error('addComment error', err);
          this.commentInput = value; // input'u geri koy
          createToast('Failed to add comment.', {type: 'danger', position: 'top-center'});
        });
    },
    canDeleteComment(comment) {
      return this.isAdmin || comment?.owner === this.currentUser
    },
    removeComment(commentId) {
      deleteComment(this.teamId, this.boardId, this.item.column, this.item.id, commentId)
        .then(() => {
          // Optimistik: listeden hemen kaldır
          this.comments = this.comments.filter(c => c.id !== commentId);
          createToast('Comment removed', {type: 'success', position: 'top-center'});
        })
        .catch((err) => {
          console.error('removeComment error', err);
          createToast('Failed to remove comment.', {type: 'danger', position: 'top-center'});
          this.fetchComments(true); // Hata durumunda DB'den taze çek
        });
    },
    /**
     * Yorumları yeniden çeker.
     * force=true: önbellek kontrolü yapmadan zorla yeniler.
     * RetroBoard.vue tarafından ref üzerinden public olarak çağrılabilir.
     */
    fetchComments(force = false) {
      if (!this.item?.id) return;
      if (!force && this.commentsLoadedForItem === this.item.id) return;
      getComments(this.teamId, this.boardId, this.item.column, this.item.id)
        .then(comments => {
          this.comments = comments;
          this.commentsLoadedForItem = this.item.id;
        })
        .catch(err => console.error('fetchComments error', err));
    },
    setStatus(status) {
      updateItemStatus(this.teamId, this.boardId, this.item.column, this.item.id, status)
        .then((updated) => {
          this.item.status = updated?.status || status
          createToast('Status updated', {type: 'success', position: 'top-center'})
        })
        .catch((err) => {
          console.error('updateItemStatus error', err)
          createToast('Failed to update status.', {type: 'danger', position: 'top-center'})
        })
    },
    getItemOwnerName() {
      return this.resolveOwnerName(this.item?.owner) || 'Anonymous'
    },
    resolveOwnerName(ownerKey) {
      if (!ownerKey) return 'Anonymous'
      if (!this.members) return ownerKey
      // members Array veya Object olabilir
      if (Array.isArray(this.members)) {
        const m = this.members.find(m => m.email === ownerKey)
        return m?.displayName || ownerKey
      }
      return this.members[ownerKey]?.displayName || ownerKey
    },
    getAvatar() {
      let splitOwnerName = this.getItemOwnerName().split(" ")
      if (splitOwnerName.length > 1) {
        return splitOwnerName[0].charAt(0).toUpperCase() + splitOwnerName[1].charAt(0).toUpperCase()
      } else {
        return splitOwnerName[0].charAt(0).toUpperCase()
      }
    },
    getCurrentUserInitials() {
      const currentUser = localStorage.getItem('user')
      if (!currentUser) return 'U'
      // email ise @ öncesini al
      const name = currentUser.includes('@') ? currentUser.split('@')[0] : currentUser
      const parts = name.replace(/[._-]/g, ' ').trim().split(' ')
      return parts.length >= 2
        ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
        : (parts[0][0] || 'U').toUpperCase()
    },
    handleKeydown(e) {
      if (e.key !== 'Escape') return
      if (this.edit) {
        this.cancelEdit()
      } else {
        this.$emit('close')
      }
    }
  },
  mounted() {
    document.addEventListener('keydown', this.handleKeydown)
  },
  beforeUnmount() {
    document.removeEventListener('keydown', this.handleKeydown)
  },
  watch: {
    'item.id': {
      handler(newVal, oldVal) {
        if (!newVal) return;
        if (oldVal && oldVal !== newVal) {
          this.comments = [];
          this.commentsLoadedForItem = null;
        }
        this.edit = false;
        this.fetchComments();
      },
      immediate: true
    }
  }
}
</script>
