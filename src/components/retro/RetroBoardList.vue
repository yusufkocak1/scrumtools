<template>
  <ul class="p-2 flex justify-center lg:justify-start flex-wrap gap-2 ">

    <li v-for="(board,index) in boardList"
        class=" flex flex-col my-6 justify-center items-center bg-white shadow-sm border border-slate-200 rounded-lg w-64 h-64">
      <div
          class="flex flex-col  my-2 justify-center items-center bg-white shadow-sm border border-slate-200 rounded-lg h-64">
        <div class="relative h-56 m-2.5 overflow-hidden text-white rounded-md"
             @click="openRetroBoard(board.id)">
          <img
              :src="getImageUrl(cardImageURLs[index%7]) "
              alt="card-image"/>
        </div>
        <div class="px-2  flex flex-row justify-center items-center w-full">
          <h6 class="mb-2 text-slate-800 text-xl font-semibold w-full justify-center items-center"
              @click="$emit('OpenRetroBoard',board.id)">
            {{ board.retroBoardName }}
          </h6>
          <div class=" relative flex flex-col justify-end items-end">
            <button
                @click="showEdit[index] = !showEdit[index]"
            >
              <svg class="bi bi-three-dots-vertical" fill="#000000" height="24" viewBox="0 0 16 16" width="24"
                   xmlns="http://www.w3.org/2000/svg">
                <path d="M9.5 13a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm0-5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm0-5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0z"/>
              </svg>
            </button>
            <ul v-if="showEdit[index]"
                class="absolute left-8  mt-2 w-56 origin-top-right shadow-lg bg-white ring-1 ring-black ring-opacity-5 divide-y divide-gray-100 focus:outline-none">
              <li class="cursor-pointer text-slate-800 flex w-full text-sm items-center rounded-md  transition-all hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100"
                  role="menuitem"
              >Edit</li>
              <li class="cursor-pointer text-slate-800 flex w-full text-sm items-center rounded-md  transition-all hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100"
                  role="menuitem"
                  @click="deleteBoard(board.id)">Delete</li>
            </ul>
          </div>
        </div>
      </div>
    </li>
    <li class=" flex flex-col my-6 justify-center items-center bg-white shadow-sm border border-slate-200 rounded-lg w-64 h-64">
      <div @click="$emit('createBoard')">
        <div class="p-4 flex flex-col justify-center items-center space-y-2">
          <span
              class=" w-8 h-8 text-3xl font-extrabold border-4 border-slate-800 flex items-center justify-center rounded-3xl">
            +
          </span>
          <span class="flex items-center justify-center text-xl font-semibold">New Board</span>
        </div>
      </div>
    </li>
  </ul>

</template>

<script>
import CardBG1 from "../../assets/images/CardBG-1.png";
import CardBG2 from "../../assets/images/CardBG-2.png";
import CardBG3 from "../../assets/images/CardBG-3.png";
import CardBG4 from "../../assets/images/CardBG-4.png";
import CardBG5 from "../../assets/images/CardBG-5.png";
import CardBG6 from "../../assets/images/CardBG-6.png";
import CardBG7 from "../../assets/images/CardBG-7.png";

export default {
  name: "RetroBoardList",
  data: () => ({
    cardImageURLs: [CardBG1, CardBG2, CardBG3, CardBG4, CardBG5, CardBG6, CardBG7],
    showEdit: []
  }),
  props: {
    boardList: Array,
    teamId: String
  },
  methods: {
    getImageUrl(url) {
      return new URL(url, import.meta.url).href
    },
    deleteBoard(boardId) {
      this.$emit('deleteBoard', boardId)
    },
    openRetroBoard(boardId) {
      this.$router.push(`/retroBoard/${this.teamId}/${boardId}`)
    },
  },
  created() {
    this.showEdit = Array(this.boardList.length).fill(false)
  }
}
</script>
