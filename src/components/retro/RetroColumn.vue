<template>
  <div class="relative flex flex-col my-6 bg-white shadow-sm border border-slate-200 rounded-lg w-11/12 lg:w-[30%]">
    <div class="mx-3 mb-0 border-b border-slate-200 pt-3 pb-2 px-1">
    <span class="text-sm text-slate-600 font-medium">
      {{ column }}
    </span>
    </div>

    <div class="p-4">
      <RetroItem v-for="item in items" :key="item.id" :ownerName="members[item.owner]?.displayName"
                 :board-id="boardId" :column="column" :isAdmin="isAdmin" :item="item" :team-id="teamId"
                 @addVote="addVote" @openDetail="(itemDetail)=>$emit('openDetail', itemDetail, column)"
                 @removeItem="removeItem" @removeVote="removeVote"></RetroItem>
      <div
          class="text-slate-800  flex w-full items-center rounded-md hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100 py-1 border-b px-2"
      ><input v-model="item"
              class="border border-slate-200 rounded-md px-3 py-2 w-full transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow"
              placeholder="Add new item"
              type="text"
              @keydown.enter="addItem(column)"/>
        <div class="ml-auto grid place-items-center justify-self-end">
          <button
              class="rounded-md border border-transparent  text-center text-xl px-2 font-bold transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
              type="button"
              @click="addItem(column)">
            +
          </button>
        </div>
      </div>
    </div>
  </div>

</template>

<script>
import {listenRetroItemsChange, removeRetroBoardItem, removeVote, setVote} from "../../firebase/RetroBoardService.js";
import RetroItem from "./RetroItem.vue";
import {createToast} from "mosha-vue-toastify";

export default {
  name: "RetroColumn",
  components: {RetroItem},
  props: {
    column: String,
    boardId: String,
    teamId: String,
    isAdmin: Boolean,
    members: Array
  },
  data() {
    return {
      item: "",
      items: Array,
    }
  },
  methods: {
    getItems(column) {
      return []//this.item[column].items
    },
    addItem() {
      const newRetroItem = {
        value: this.item,
        column: this.column
      }
      this.$emit('addItem', newRetroItem)
      this.item = ""
    },
    addVote(itemId, vote) {
      setVote(this.teamId, this.boardId, this.column, itemId, vote)
    },
    removeVote(itemId) {
      removeVote(this.teamId, this.boardId, this.column, itemId, localStorage.getItem("user"))
    },
    removeItem(index) {
      removeRetroBoardItem(this.teamId, this.boardId, this.column, index).then(() => {
        createToast('Item removed. ', {type: 'success', position: 'top-center'})
      })
    },
  },
  unmounted() {
  },

  created() {
    listenRetroItemsChange(this.teamId, this.boardId, this.column, (items) => {
      this.items = items
    })
  }
}
</script>