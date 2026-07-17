<template>
  <div class="relative flex flex-col w-full bg-white shadow-sm hover:shadow-md transition-shadow duration-200 border border-gray-200 rounded-2xl overflow-hidden">
    <!-- Renkli vurgu şeridi -->
    <div :class="['h-1 w-full', theme.bar]"></div>

    <!-- Kolon başlığı -->
    <div class="flex items-center gap-2 px-4 pt-3 pb-2.5 border-b border-gray-100">
      <span :class="['w-2 h-2 rounded-full flex-shrink-0', theme.dot]"></span>
      <h2 class="text-sm font-semibold text-gray-800 truncate">{{ column }}</h2>
      <span :class="['ml-auto px-2 py-0.5 rounded-full text-xs font-medium tabular-nums flex-shrink-0', theme.badge]">
        {{ localItems.length }}
      </span>
    </div>

    <!-- Item listesi -->
    <div class="flex-1 p-3 space-y-3">
      <RetroItem
        v-for="item in localItems"
        :key="item.id"
        :ownerName="resolveOwnerName(item.owner)"
        :board-id="boardId"
        :column="column"
        :isAdmin="isAdmin"
        :item="item"
        :team-id="teamId"
        @addVote="addVote"
        @openDetail="openItemDetail(item)"
        @removeItem="removeItem"
        @removeVote="removeVote"
      />

      <!-- Boş durum -->
      <div v-if="localItems.length === 0" class="flex flex-col items-center justify-center py-6 text-center">
        <div :class="['w-9 h-9 rounded-xl flex items-center justify-center mb-2', theme.badge]">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
        </div>
        <p class="text-xs text-gray-400">No items yet — add the first one</p>
      </div>
    </div>

    <!-- Yeni item ekleme -->
    <div class="p-3 pt-0">
      <div :class="['flex items-center gap-1 bg-gray-50 border border-gray-200 rounded-xl pl-3 pr-1.5 py-1 transition-all focus-within:bg-white focus-within:ring-2 focus-within:border-transparent', theme.ring]">
        <input
          v-model="item"
          class="flex-1 min-w-0 bg-transparent text-sm text-gray-800 placeholder:text-gray-400 py-1.5 focus:outline-none"
          placeholder="Add new item"
          type="text"
          @keydown.enter="addItem()"
        />
        <button
          :disabled="!item.trim()"
          :class="['p-1.5 rounded-lg text-white transition-colors flex-shrink-0 disabled:opacity-30 disabled:pointer-events-none', theme.button]"
          type="button"
          title="Add item"
          @click="addItem()"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { deleteItem, toggleVote } from "../../api/RetroBoardApi.js";
import RetroItem from "./RetroItem.vue";
import { createToast } from "mosha-vue-toastify";

const ADD_ITEM_THROTTLE_MS = 500;

// Kolon sırasına göre döngüsel renk temaları (Tailwind JIT için tam class isimleri)
const ACCENT_THEMES = [
  { bar: "bg-emerald-500", dot: "bg-emerald-500", badge: "bg-emerald-50 text-emerald-700", button: "bg-emerald-600 hover:bg-emerald-700", ring: "focus-within:ring-emerald-500/60" },
  { bar: "bg-rose-500",    dot: "bg-rose-500",    badge: "bg-rose-50 text-rose-700",       button: "bg-rose-600 hover:bg-rose-700",       ring: "focus-within:ring-rose-500/60" },
  { bar: "bg-sky-500",     dot: "bg-sky-500",     badge: "bg-sky-50 text-sky-700",         button: "bg-sky-600 hover:bg-sky-700",         ring: "focus-within:ring-sky-500/60" },
  { bar: "bg-amber-500",   dot: "bg-amber-500",   badge: "bg-amber-50 text-amber-700",     button: "bg-amber-600 hover:bg-amber-700",     ring: "focus-within:ring-amber-500/60" },
  { bar: "bg-violet-500",  dot: "bg-violet-500",  badge: "bg-violet-50 text-violet-700",   button: "bg-violet-600 hover:bg-violet-700",   ring: "focus-within:ring-violet-500/60" }
];

export default {
  name: "RetroColumn",
  components: { RetroItem },
  props: {
    column: { type: String, required: true },
    boardId: { type: String, required: true },
    teamId: { type: String, required: true },
    isAdmin: { type: Boolean, default: false },
    members: { type: [Array, Object], default: () => [] },
    items: { type: Array, default: () => [] },
    accentIndex: { type: Number, default: 0 }
  },
  computed: {
    theme() {
      return ACCENT_THEMES[this.accentIndex % ACCENT_THEMES.length];
    }
  },
  data() {
    return {
      item: "",
      localItems: [],
      lastAddTime: 0
    };
  },
  watch: {
    items: {
      handler(newItems) {
        this.localItems = newItems ? [...newItems] : [];
      },
      immediate: true,
      deep: false
    }
  },
  methods: {
    resolveOwnerName(ownerKey) {
      if (!ownerKey || !this.members) return "";
      if (Array.isArray(this.members)) {
        const found = this.members.find(m => m.email === ownerKey || m.id === ownerKey || m.owner === ownerKey);
        return found?.displayName || found?.name || ownerKey;
      }
      return this.members[ownerKey]?.displayName || this.members[ownerKey]?.name || ownerKey || "";
    },
    addItem() {
      const now = Date.now();
      if (now - this.lastAddTime < ADD_ITEM_THROTTLE_MS) return;
      this.lastAddTime = now;

      const trimmed = this.item.trim();
      if (!trimmed) return;

      this.$emit("addItem", { value: trimmed, column: this.column });
      this.item = "";
    },
    async addVote(itemId, vote) {
      try {
        const voteValue = typeof vote === 'object' ? vote.value : vote;
        this._applyOptimisticVote(itemId, voteValue, vote.owner || localStorage.getItem('user'));

        const updatedItem = await toggleVote(this.teamId, this.boardId, this.column, itemId, voteValue);
        if (updatedItem) {
          const idx = this.localItems.findIndex(i => i.id === itemId);
          if (idx !== -1) {
            this.localItems[idx] = { ...this.localItems[idx], votes: updatedItem.votes, voteScore: updatedItem.voteScore };
            this.localItems = [...this.localItems];
          }
        }
      } catch(e) {
        console.error('addVote error', e);
        this.localItems = this.items ? [...this.items] : [];
      }
    },
    async removeVote(itemId, voteValue) {
      try {
        const val = typeof voteValue === 'object' ? voteValue.value : voteValue;
        this._applyOptimisticVote(itemId, val, localStorage.getItem('user'), true);

        const updatedItem = await toggleVote(this.teamId, this.boardId, this.column, itemId, val);
        if (updatedItem) {
          const idx = this.localItems.findIndex(i => i.id === itemId);
          if (idx !== -1) {
            this.localItems[idx] = { ...this.localItems[idx], votes: updatedItem.votes, voteScore: updatedItem.voteScore };
            this.localItems = [...this.localItems];
          }
        }
      } catch(e) {
        console.error('removeVote error', e);
        this.localItems = this.items ? [...this.items] : [];
      }
    },
    _applyOptimisticVote(itemId, voteValue, owner, isRemove = false) {
      const idx = this.localItems.findIndex(i => i.id === itemId);
      if (idx === -1) return;
      const item = { ...this.localItems[idx] };
      let votes = Array.isArray(item.votes) ? [...item.votes] : [];

      if (isRemove) {
        votes = votes.filter(v => !(v.owner === owner && v.value === voteValue));
      } else {
        const existingIdx = votes.findIndex(v => v.owner === owner);
        if (existingIdx !== -1) {
          if (votes[existingIdx].value === voteValue) {
            votes.splice(existingIdx, 1);
          } else {
            votes[existingIdx] = { ...votes[existingIdx], value: voteValue };
          }
        } else {
          votes.push({ owner, value: voteValue });
        }
      }

      item.votes = votes;
      this.localItems[idx] = item;
      this.localItems = [...this.localItems];
    },
    removeItem(itemId) {
      this.localItems = this.localItems.filter(i => i.id !== itemId);
      deleteItem(this.teamId, this.boardId, this.column, itemId)
        .then(() => createToast("Item removed.", { type: "success", position: "top-center" }))
        .catch(() => {
          createToast("Failed to remove item.", { type: "danger", position: "top-center" });
          this.localItems = this.items ? [...this.items] : [];
        });
    },
    openItemDetail(item) {
      this.$emit("openDetail", item, this.column);
    }
  }
};
</script>