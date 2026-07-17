<template>
  <div class="flex flex-col bg-white w-full sm:max-w-xl rounded-t-3xl sm:rounded-2xl shadow-2xl max-h-[92vh] sm:max-h-[85vh] overflow-hidden">
    <!-- Mobile drag handle -->
    <div class="sm:hidden flex justify-center pt-2 pb-1 flex-shrink-0">
      <div class="w-10 h-1 rounded-full bg-gray-300"></div>
    </div>

    <!-- Header -->
    <div class="flex items-center justify-between gap-2 px-4 py-3 border-b border-gray-200 flex-shrink-0">
      <div class="flex items-center gap-2 min-w-0">
        <span class="w-2 h-2 rounded-full bg-indigo-500 flex-shrink-0"></span>
        <h3 class="font-semibold text-gray-900 truncate">{{ item?.column || 'Retro Item' }}</h3>
        <span v-if="item?.status" :class="['px-2 py-0.5 rounded-full text-xs font-medium flex-shrink-0', statusBadgeClass]">
          {{ statusText }}
        </span>
      </div>
      <button
        @click="$emit('close')"
        class="p-2 -mr-1 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all focus:outline-none focus:ring-2 focus:ring-red-500 flex-shrink-0"
        title="Close (Esc)"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- ─── Discussion engine ─────────────────────────────────────────── -->

    <!-- Idle: optional timer, unobtrusive -->
    <div v-if="timerState === 'idle'" class="px-4 py-2.5 border-b border-gray-100 bg-gray-50/70 flex-shrink-0">
      <div class="flex items-center gap-2">
        <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <span class="text-xs font-medium text-gray-500 flex-shrink-0">Discussion timer</span>
        <div class="flex items-center gap-1.5 overflow-x-auto ml-auto pl-2">
          <template v-if="!showCustom">
            <button
              v-for="preset in startPresets"
              :key="preset.seconds"
              :disabled="timerBusy"
              @click="startTimer(preset.seconds)"
              class="px-2.5 py-1 rounded-full text-xs font-medium bg-white border border-gray-200 text-gray-600 hover:border-indigo-300 hover:text-indigo-600 hover:bg-indigo-50 transition-colors flex-shrink-0 disabled:opacity-50"
            >
              {{ preset.label }}
            </button>
            <button
              @click="showCustom = true"
              class="px-2.5 py-1 rounded-full text-xs font-medium bg-white border border-dashed border-gray-300 text-gray-500 hover:border-indigo-300 hover:text-indigo-600 transition-colors flex-shrink-0"
            >
              Custom
            </button>
          </template>
          <template v-else>
            <input
              ref="customInput"
              v-model.number="customMinutes"
              @keydown.enter="startCustom"
              @keydown.esc.stop="showCustom = false"
              type="number" min="1" max="120" placeholder="min"
              class="w-16 px-2 py-1 text-xs border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
            <button
              :disabled="!customMinutes || customMinutes < 1 || timerBusy"
              @click="startCustom"
              class="px-2.5 py-1 rounded-full text-xs font-medium bg-indigo-600 text-white hover:bg-indigo-700 transition-colors disabled:opacity-50 flex-shrink-0"
            >
              Start
            </button>
            <button
              @click="showCustom = false"
              class="p-1 rounded-full text-gray-400 hover:text-gray-600 flex-shrink-0"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </template>
        </div>
      </div>
    </div>

    <!-- Running: live countdown -->
    <div v-else-if="timerState === 'running'" :class="['px-4 py-3 border-b flex-shrink-0 transition-colors', runningPanelClass]">
      <div class="flex items-center gap-3">
        <div class="relative flex-shrink-0">
          <span :class="['absolute inline-flex h-full w-full rounded-full opacity-60 animate-ping', urgency === 'critical' ? 'bg-red-400' : urgency === 'warn' ? 'bg-amber-400' : 'bg-indigo-400']"></span>
          <span :class="['relative inline-flex w-2.5 h-2.5 rounded-full', urgency === 'critical' ? 'bg-red-500' : urgency === 'warn' ? 'bg-amber-500' : 'bg-indigo-500']"></span>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-baseline gap-2">
            <span :class="['text-2xl font-bold tabular-nums leading-none', urgency === 'critical' ? 'text-red-600' : urgency === 'warn' ? 'text-amber-600' : 'text-indigo-700']">
              {{ countdownText }}
            </span>
            <span class="text-xs text-gray-500 truncate">discussion in progress</span>
          </div>
          <div class="mt-2 h-1.5 rounded-full bg-white/80 overflow-hidden">
            <div
              :class="['h-full rounded-full transition-all duration-500 ease-linear', urgency === 'critical' ? 'bg-red-500' : urgency === 'warn' ? 'bg-amber-500' : 'bg-indigo-500']"
              :style="{ width: (progress * 100) + '%' }"
            ></div>
          </div>
        </div>
        <div class="flex items-center gap-1.5 flex-shrink-0">
          <button
            :disabled="timerBusy"
            @click="extendTimer(60)"
            class="px-2.5 py-1.5 rounded-lg text-xs font-medium bg-white/90 border border-gray-200 text-gray-600 hover:text-indigo-600 hover:border-indigo-300 transition-colors disabled:opacity-50"
            title="Add 1 minute"
          >
            +1m
          </button>
          <button
            :disabled="timerBusy"
            @click="stopTimer"
            class="p-1.5 rounded-lg bg-white/90 border border-gray-200 text-gray-400 hover:text-red-500 hover:border-red-300 transition-colors disabled:opacity-50"
            title="Cancel timer"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 9h6v6H9z"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Expired: decision panel -->
    <div v-else-if="timerState === 'expired'" class="px-4 py-3 border-b border-amber-200 bg-gradient-to-br from-amber-50 to-orange-50 flex-shrink-0">
      <div class="flex items-center gap-2 mb-2.5">
        <svg class="w-5 h-5 text-amber-500 flex-shrink-0 animate-pulse" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"/>
        </svg>
        <div class="min-w-0">
          <p class="text-sm font-semibold text-amber-800 leading-tight">Time's up!</p>
          <p class="text-xs text-amber-600">Add extra time or resolve this item.</p>
        </div>
      </div>
      <div class="flex items-center gap-1.5 mb-2 overflow-x-auto">
        <span class="text-xs font-medium text-amber-700 flex-shrink-0">Extra time:</span>
        <button
          v-for="preset in extendPresets"
          :key="preset.seconds"
          :disabled="timerBusy"
          @click="extendTimer(preset.seconds)"
          class="px-2.5 py-1 rounded-full text-xs font-medium bg-white border border-amber-200 text-amber-700 hover:bg-amber-100 hover:border-amber-300 transition-colors flex-shrink-0 disabled:opacity-50"
        >
          {{ preset.label }}
        </button>
      </div>
      <div class="grid grid-cols-3 gap-2">
        <button
          @click="setStatus('ACCEPTED')"
          class="flex items-center justify-center gap-1 px-2 py-2 rounded-lg bg-green-600 text-white text-xs font-semibold hover:bg-green-700 transition-colors"
        >
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
          </svg>
          Accept
        </button>
        <button
          @click="setStatus('REJECTED')"
          class="flex items-center justify-center gap-1 px-2 py-2 rounded-lg bg-red-600 text-white text-xs font-semibold hover:bg-red-700 transition-colors"
        >
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
          </svg>
          Reject
        </button>
        <button
          @click="setStatus('CONFLICT')"
          class="flex items-center justify-center gap-1 px-2 py-2 rounded-lg bg-amber-500 text-white text-xs font-semibold hover:bg-amber-600 transition-colors"
        >
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
          </svg>
          Conflict
        </button>
      </div>
    </div>

    <!-- ─── Scrollable body ───────────────────────────────────────────── -->
    <div class="flex-1 overflow-y-auto overscroll-contain">
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
              <span class="flex items-center gap-1 ml-auto px-2 py-0.5 rounded-full bg-gray-100 text-xs text-gray-500">
                <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M3.293 9.707a1 1 0 010-1.414l6-6a1 1 0 011.414 0l6 6a1 1 0 01-1.414 1.414L11 5.414V17a1 1 0 11-2 0V5.414L4.707 9.707a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                </svg>
                {{ voteScore }}
              </span>
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
          </div>
        </div>
      </div>

      <!-- Comments -->
      <div class="p-4">
        <h4 class="font-medium text-gray-900 text-sm mb-3">
          Discussion <span class="text-gray-400 font-normal">({{ comments.length }})</span>
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
      <div class="hidden sm:block flex-shrink-0">
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
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Resolution footer (hidden while the expired decision panel is shown) -->
    <div
      v-if="timerState !== 'expired'"
      class="px-4 py-3 border-t border-gray-200 bg-gray-50 flex-shrink-0 pb-[max(0.75rem,env(safe-area-inset-bottom))]"
    >
      <div class="grid grid-cols-3 gap-2">
        <button
          @click="setStatus('ACCEPTED')"
          :class="['flex items-center justify-center gap-1 px-2 py-2 rounded-lg transition-colors text-xs font-medium',
                   item?.status === 'ACCEPTED'
                     ? 'bg-green-600 text-white'
                     : 'bg-green-100 text-green-700 hover:bg-green-200']"
          title="Accept"
        >
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
          </svg>
          Accept
        </button>
        <button
          @click="setStatus('REJECTED')"
          :class="['flex items-center justify-center gap-1 px-2 py-2 rounded-lg transition-colors text-xs font-medium',
                   item?.status === 'REJECTED'
                     ? 'bg-red-600 text-white'
                     : 'bg-red-100 text-red-700 hover:bg-red-200']"
          title="Reject"
        >
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
          </svg>
          Reject
        </button>
        <button
          @click="setStatus('CONFLICT')"
          :class="['flex items-center justify-center gap-1 px-2 py-2 rounded-lg transition-colors text-xs font-medium',
                   item?.status === 'CONFLICT'
                     ? 'bg-amber-500 text-white'
                     : 'bg-amber-100 text-amber-700 hover:bg-amber-200']"
          title="Conflict"
        >
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
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
  updateItemStatus,
  startDiscussion,
  extendDiscussion,
  stopDiscussion
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
      commentsLoadedForItem: "",
      // Discussion timer state
      nowTs: Date.now(),
      tickInterval: null,
      timerBusy: false,
      showCustom: false,
      customMinutes: null,
      expiryNotifiedFor: null,
      startPresets: [
        {label: '1m', seconds: 60},
        {label: '3m', seconds: 180},
        {label: '5m', seconds: 300},
        {label: '10m', seconds: 600}
      ],
      extendPresets: [
        {label: '+1m', seconds: 60},
        {label: '+3m', seconds: 180},
        {label: '+5m', seconds: 300}
      ]
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
    },

    // ─── Discussion timer ────────────────────────────────────────────────
    endsAtTs() {
      if (!this.item?.discussionEndsAt) return null
      const t = new Date(this.item.discussionEndsAt).getTime()
      return isNaN(t) ? null : t
    },
    totalDurationSeconds() {
      return this.item?.discussionDurationSeconds || 0
    },
    remainingSeconds() {
      if (!this.endsAtTs) return 0
      return Math.max(0, Math.ceil((this.endsAtTs - this.nowTs) / 1000))
    },
    /**
     * Tartışma motorunun durum makinesi:
     *  hidden  → karar verilmiş, zamanlayıcı alanı gösterilmez
     *  idle    → zamanlayıcı yok; başlatmak tamamen opsiyonel
     *  running → geri sayım aktif
     *  expired → süre doldu, karar paneli (ek süre / accept / reject / conflict)
     */
    timerState() {
      if (this.item?.status) return 'hidden'
      if (!this.endsAtTs) return 'idle'
      return this.remainingSeconds > 0 ? 'running' : 'expired'
    },
    progress() {
      if (!this.totalDurationSeconds) return 0
      return Math.min(1, this.remainingSeconds / this.totalDurationSeconds)
    },
    urgency() {
      const r = this.remainingSeconds
      if (r <= 15 || (this.totalDurationSeconds && r / this.totalDurationSeconds <= 0.1)) return 'critical'
      if (r <= 60 || (this.totalDurationSeconds && r / this.totalDurationSeconds <= 0.25)) return 'warn'
      return 'normal'
    },
    runningPanelClass() {
      return {
        normal: 'bg-indigo-50 border-indigo-100',
        warn: 'bg-amber-50 border-amber-100',
        critical: 'bg-red-50 border-red-100'
      }[this.urgency]
    },
    countdownText() {
      const r = this.remainingSeconds
      const h = Math.floor(r / 3600)
      const m = Math.floor((r % 3600) / 60)
      const s = r % 60
      const pad = n => String(n).padStart(2, '0')
      return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`
    }
  },
  methods: {
    // ─── Discussion timer ────────────────────────────────────────────────
    startTimer(seconds) {
      this.timerBusy = true
      startDiscussion(this.teamId, this.boardId, this.item.column, this.item.id, seconds)
        .then(updated => {
          this.applyDiscussionFields(updated)
          this.showCustom = false
          this.customMinutes = null
        })
        .catch(err => {
          console.error('startDiscussion error', err)
          createToast('Failed to start timer.', {type: 'danger', position: 'top-center'})
        })
        .finally(() => { this.timerBusy = false })
    },
    startCustom() {
      const minutes = Number(this.customMinutes)
      if (!minutes || minutes < 1) return
      this.startTimer(Math.round(Math.min(120, minutes) * 60))
    },
    extendTimer(seconds) {
      this.timerBusy = true
      extendDiscussion(this.teamId, this.boardId, this.item.column, this.item.id, seconds)
        .then(updated => this.applyDiscussionFields(updated))
        .catch(err => {
          console.error('extendDiscussion error', err)
          createToast('Failed to extend timer.', {type: 'danger', position: 'top-center'})
        })
        .finally(() => { this.timerBusy = false })
    },
    stopTimer() {
      this.timerBusy = true
      stopDiscussion(this.teamId, this.boardId, this.item.column, this.item.id)
        .then(updated => this.applyDiscussionFields(updated))
        .catch(err => {
          console.error('stopDiscussion error', err)
          createToast('Failed to stop timer.', {type: 'danger', position: 'top-center'})
        })
        .finally(() => { this.timerBusy = false })
    },
    /**
     * API/WS'den gelen güncel tartışma alanlarını item üzerine yazar.
     * item referansı board'daki kartla paylaşıldığı için kart rozeti de güncellenir.
     */
    applyDiscussionFields(updated) {
      if (!updated) return
      this.item.discussionEndsAt = updated.discussionEndsAt || null
      this.item.discussionDurationSeconds = updated.discussionDurationSeconds || null
      this.nowTs = Date.now()
    },
    syncTicker() {
      const needsTick = this.timerState === 'running'
      if (needsTick && !this.tickInterval) {
        this.tickInterval = setInterval(() => { this.nowTs = Date.now() }, 250)
      } else if (!needsTick && this.tickInterval) {
        clearInterval(this.tickInterval)
        this.tickInterval = null
      }
    },
    notifyExpiry() {
      // Aynı süre dolumu için bir kez uyar (extend sonrası yeni endsAt yeni uyarı üretir)
      if (this.expiryNotifiedFor === this.endsAtTs) return
      this.expiryNotifiedFor = this.endsAtTs
      try {
        if (navigator.vibrate) navigator.vibrate([180, 90, 180])
      } catch (e) { /* desteklenmiyor */ }
      try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)()
        const beep = (freq, start, dur) => {
          const osc = ctx.createOscillator()
          const gain = ctx.createGain()
          osc.connect(gain)
          gain.connect(ctx.destination)
          osc.frequency.value = freq
          gain.gain.setValueAtTime(0.15, ctx.currentTime + start)
          gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + start + dur)
          osc.start(ctx.currentTime + start)
          osc.stop(ctx.currentTime + start + dur)
        }
        beep(880, 0, 0.18)
        beep(660, 0.25, 0.28)
        setTimeout(() => ctx.close(), 1200)
      } catch (e) { /* ses çalınamadı, kritik değil */ }
    },

    // ─── Item / comments ─────────────────────────────────────────────────
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
          // Karar zamanlayıcıyı da kapatır (backend temizler, response yansıtır)
          this.applyDiscussionFields(updated || {})
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
      } else if (this.showCustom) {
        this.showCustom = false
      } else {
        this.$emit('close')
      }
    }
  },
  mounted() {
    document.addEventListener('keydown', this.handleKeydown)
    this.syncTicker()
  },
  beforeUnmount() {
    document.removeEventListener('keydown', this.handleKeydown)
    if (this.tickInterval) {
      clearInterval(this.tickInterval)
      this.tickInterval = null
    }
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
        this.showCustom = false;
        this.customMinutes = null;
        this.expiryNotifiedFor = null;
        this.nowTs = Date.now();
        this.fetchComments();
      },
      immediate: true
    },
    timerState(newState, oldState) {
      this.syncTicker()
      if (newState === 'expired' && oldState === 'running') {
        this.notifyExpiry()
      }
    }
  }
}
</script>
