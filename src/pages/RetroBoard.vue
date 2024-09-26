<template>
  <div class="w-screen">
    <div class="flex flex-wrap justify-around border-b m-2 py-2 ">
      <h1 class="font-bold text-2xl justify-start ">{{ board?.retroBoardName }}</h1>
      <div class="flex flex-wrap gap-2  justify-center items-center">
        <p class="mr-2">Anonymous Mode:</p>
        <div class="inline-flex items-center gap-2">
          <label class="text-slate-600 text-sm cursor-pointer" for="switch-component-on">Off</label>
          <div class="relative inline-block w-11 h-5">
            <input id="switch-component-on" v-model="anonymousMode" class="peer appearance-none w-11 h-5 bg-slate-100 rounded-full checked:bg-slate-800 cursor-pointer transition-colors duration-300"
                   type="checkbox"/>
            <label class="absolute top-0 left-0 w-5 h-5 bg-white rounded-full border border-slate-300 shadow-sm transition-transform duration-300 peer-checked:translate-x-6 peer-checked:border-slate-800 cursor-pointer"
                   for="switch-component-on">
            </label>
          </div>
          <label class="text-slate-600 text-sm cursor-pointer" for="switch-component-on">On</label>
        </div>
      </div>
    </div>
    <div class="flex flex-wrap gap-2 w-screen justify-center">
      <RetroColumn :isAdmin=isAdmin v-for="(column, index) in board?.columns" @addItem="addItem" :key="index" :column="column" :teamId="teamId" :boardId="boardId" @openDetail="openDetail"/>
      <div v-if="showItemDetail" class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60  backdrop-blur-sm transition-opacity duration-300">
      <RetroItemDetail class="absolute"  @close="showItemDetail = false" :item="RetroItemDetail" :board-id="boardId" :team-id="teamId"/>
      </div>
    </div>
  </div>
</template>

<script>
import {createRetroItem, getRetroBoard, getRetroItems} from "../firebase/RetroBoardService.js";
import {getTeamById} from "../firebase/TeamService.js";
import RetroColumn from "../components/retro/RetroColumn.vue";
import RetroItem from "../components/retro/RetroItem.vue";
import RetroItemDetail from "../components/retro/RetroItemDetail.vue";
import {createToast} from "mosha-vue-toastify";

export default {
  name: "RetroBoard",
  components: {RetroItemDetail, RetroItem, RetroColumn},
  props: {
    boardId: "",
    teamId: ""
  },
  data() {
    return {
      board: null,
      team: null,
      anonymousMode: false,
      item: Object,
      items: Array,
      showItemDetail: false,
      RetroItemDetail: {}
    }
  },
  created() {
    getRetroBoard(this.teamId, this.boardId, (board) => {
      this.board = board
    })
    getTeamById(this.teamId, (team) => {
      this.team = team
    })

  },
  methods: {
    getItems(column) {
      return this.items[column]
    },
    addItem(item) {
      const newItem = {
        ...item,
        owner: this.anonymousMode ? "Anonymous" : localStorage.getItem("user")
      }
      createRetroItem(this.teamId, this.boardId, newItem.column, newItem).then(()=>{
        createToast('Item created. ',{type:'success',position:'top-center'})
      })

    },
    openDetail(item) {
      this.RetroItemDetail = item
      this.showItemDetail = true
    }
  },

  computed: {
    columns() {
      return this.board?.columns
    },
    isAdmin() {
      return this.team?.adminEmail === localStorage.getItem('user')
    }
  }
}
</script>
