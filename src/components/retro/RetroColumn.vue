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
import { getItems, deleteItem, toggleVote } from "../../api/RetroBoardApi.js";
import { connect, subscribe, unsubscribe } from "../../api/websocket.js";
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
    async addVote(itemId, vote) {
      try {
        const voteValue = typeof vote === 'object' ? vote.value : vote;
        const updatedItem = await toggleVote(this.teamId, this.boardId, this.column, itemId, voteValue);
        if (updatedItem) {
          const idx = this.items.findIndex(i => i.id === itemId);
          if (idx !== -1) {
            this.items[idx] = { ...this.items[idx], votes: updatedItem.votes, voteScore: updatedItem.voteScore };
            this.items = [...this.items];
          }
        }
      } catch(e) {
        console.error('addVote error', e);
      }
    },
    async removeVote(itemId, voteValue) {
      try {
        const val = typeof voteValue === 'object' ? voteValue.value : voteValue;
        const updatedItem = await toggleVote(this.teamId, this.boardId, this.column, itemId, val);
        if (updatedItem) {
          const idx = this.items.findIndex(i => i.id === itemId);
          if (idx !== -1) {
            this.items[idx] = { ...this.items[idx], votes: updatedItem.votes, voteScore: updatedItem.voteScore };
            this.items = [...this.items];
          }
        }
      } catch(e) {
        console.error('removeVote error', e);
      }
    },
    removeItem(itemId) {
      deleteItem(this.teamId, this.boardId, this.column, itemId)
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
    unsubscribe(`/topic/retro/${this.teamId}/${this.boardId}`);
    this.unsubscribeListener = null;
  },
  created() {
    // İlk yükleme: REST ile kayıtları getir
    getItems(this.teamId, this.boardId, this.column).then(items => {
      if (!this.isDestroyed) this.items = items;
    });

    // WebSocket notification-only: değişiklik bildirimi gelince ilgili sütunu yenile
    const topic = `/topic/retro/${this.teamId}/${this.boardId}`;
    const doSubscribe = () => {
      subscribe(topic, (msg) => {
        if (this.isDestroyed) return;
        // Mesaj bu sütunla ilgiliyse veya sütun belirtilmemişse yenile
        if (!msg.columnName || msg.columnName === this.column) {
          getItems(this.teamId, this.boardId, this.column).then(items => {
            if (!this.isDestroyed) this.updateItems(items);
          });
        }
      });
    };

    connect(doSubscribe);
  }
};
</script>