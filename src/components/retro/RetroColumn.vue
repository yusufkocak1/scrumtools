<template>
  <div class="relative flex flex-col my-6 bg-white shadow-sm border border-slate-200 rounded-lg w-11/12 lg:w-[32%]">
    <div class="mx-3 mb-0 border-b border-slate-200 pt-3 pb-2 px-1">
      <span class="text-sm text-slate-600 font-medium">{{ column }}</span>
    </div>

    <div class="p-4">
      <RetroItem
        v-for="item in items"
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
      <div class="text-slate-800 flex w-full items-center rounded-md hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100 py-1 border-b px-2">
        <input
          v-model="item"
          class="border border-slate-200 rounded-md px-3 py-2 w-full transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow"
          placeholder="Add new item"
          type="text"
          @keydown.enter="addItem()"
        />
        <div class="ml-auto grid place-items-center justify-self-end">
          <button
            class="rounded-md border border-transparent text-center text-xl px-2 font-bold transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
            type="button"
            @click="addItem()"
          >
            +
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { listenRetroItemsChange, removeRetroBoardItem, removeVote as fbRemoveVote, setVote } from "../../firebase/RetroBoardService.js";
import RetroItem from "./RetroItem.vue";
import { createToast } from "mosha-vue-toastify";

const ADD_ITEM_THROTTLE_MS = 500;

export default {
  name: "RetroColumn",
  components: { RetroItem },
  props: {
    column: { type: String, required: true },
    boardId: { type: String, required: true },
    teamId: { type: String, required: true },
    isAdmin: { type: Boolean, default: false },
    // members dizisi (veya obje) dışarıdan geliyor, index erişimi güvenli işleniyor
    members: { type: [Array, Object], default: () => [] }
  },
  data() {
    return {
      item: "",
      items: [],
      unsubscribeListener: null,
      lastUpdateTime: 0,
      isDestroyed: false
    };
  },
  methods: {
    resolveOwnerName(ownerKey) {
      if (!ownerKey || !this.members) return "";
      // Eğer members bir object ise direkt eriş, array ise uygun member'ı bul
      if (Array.isArray(this.members)) {
        const found = this.members.find(m => m.email === ownerKey || m.id === ownerKey || m.owner === ownerKey);
        return found?.displayName || found?.name || ownerKey;
      }
      return this.members[ownerKey]?.displayName || this.members[ownerKey]?.name || ownerKey || "";
    },
    addItem() {
      const now = Date.now();
      if (now - this.lastUpdateTime < ADD_ITEM_THROTTLE_MS) return; // throttle
      this.lastUpdateTime = now;

      const trimmed = this.item.trim();
      if (!trimmed) return;

      this.$emit("addItem", { value: trimmed, column: this.column });
      this.item = "";
    },
    addVote(itemId, vote) {
      const idx = this.items.findIndex(i => i.id === itemId);
      if (idx !== -1) {
        const item = this.items[idx];
        if (!item.votes) item.votes = [];
        const existingIdx = item.votes.findIndex(v => v.owner === vote.owner && v.value === vote.value);
        if (existingIdx !== -1) {
          item.votes.splice(existingIdx, 1); // toggle remove
        } else {
          // remove opposite vote first
            const oppositeIdx = item.votes.findIndex(v => v.owner === vote.owner && v.value === -vote.value);
          if (oppositeIdx !== -1) item.votes.splice(oppositeIdx, 1);
          item.votes.push(vote);
        }
        this.items = [...this.items]; // force reactivity
      }
      setVote(this.teamId, this.boardId, this.column, itemId, vote);
    },
    removeVote(itemId, voteValue) {
      const user = localStorage.getItem("user");
      const idx = this.items.findIndex(i => i.id === itemId);
      if (idx !== -1) {
        const item = this.items[idx];
        if (item.votes) {
          item.votes = item.votes.filter(v => !(v.owner === user && v.value === voteValue));
          this.items = [...this.items];
        }
      }
      fbRemoveVote(this.teamId, this.boardId, this.column, itemId, user);
    },
    removeItem(itemId) {
      removeRetroBoardItem(this.teamId, this.boardId, this.column, itemId)
        .then(() => createToast("Item removed.", { type: "success", position: "top-center" }))
        .catch(() => createToast("Failed to remove item.", { type: "danger", position: "top-center" }));
    },
    updateItems(items) {
      if (this.isDestroyed) return;
      // requestAnimationFrame ile batch
      requestAnimationFrame(() => {
        if (!this.isDestroyed) {
          this.items = items;
        }
      });
    },
    openItemDetail(item) {
      this.$emit("openDetail", item, this.column);
    }
  },
  beforeUnmount() {
    this.isDestroyed = true;
    if (this.unsubscribeListener && typeof this.unsubscribeListener === "function") {
      this.unsubscribeListener();
      this.unsubscribeListener = null;
    }
  },
  created() {
    this.unsubscribeListener = listenRetroItemsChange(
      this.teamId,
      this.boardId,
      this.column,
      this.updateItems
    );
  }
};
</script>