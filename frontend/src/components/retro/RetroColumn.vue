<template>
  <div class="relative flex flex-col my-6 bg-white shadow-sm border border-slate-200 rounded-lg w-11/12 lg:w-[32%]">
    <div class="mx-3 mb-0 border-b border-slate-200 pt-3 pb-2 px-1">
      <span class="text-sm text-slate-600 font-medium">{{ column }}</span>
    </div>

    <div class="p-4">
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
import { deleteItem, toggleVote } from "../../api/RetroBoardApi.js";
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
    members: { type: [Array, Object], default: () => [] },
    items: { type: Array, default: () => [] }
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