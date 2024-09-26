<template>
  <div class="relative flex flex-col   shadow-sm border border-slate-200 rounded-lg lg:w-96 bg-white p-1 mx-2">
    <div class="flex justify-end border-b py-1">
      <span class="w-full flex items-center ml-2 font-bold">Create Retro Board</span>
      <button class="rounded-2xl  font-extrabold text-md bg-slate-800 py-1 px-2  border border-transparent text-center  text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-red-800 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
              type="button"
              @click="$emit('close')">
        x
      </button>
    </div>
    <div class="p-5 my-3">
      <form class=" mb-2">
        <div class="mb-1 flex flex-col gap-4">

          <input class=" bg-transparent placeholder:text-slate-400 text-slate-700 text-sm border border-slate-200 rounded-md px-3 py-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow"
                 placeholder="Retro Board Name"
                 v-model="boardName"
                 type="text"/>
          <label class="border-b-2 pl-2">Column List</label>
          <div>
          <div v-for="(column,index) in columnList"
              class="text-slate-800  flex w-full items-center rounded-md hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100 py-1 border-b px-2"
          ><span>{{column}}</span>
            <div class="ml-auto grid place-items-center justify-self-end">
              <button @click="removeColumn(index)" class="rounded-md border border-transparent px-2 text-center text-sm transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none" type="button">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4">
                  <path fill-rule="evenodd" d="M16.5 4.478v.227a48.816 48.816 0 0 1 3.878.512.75.75 0 1 1-.256 1.478l-.209-.035-1.005 13.07a3 3 0 0 1-2.991 2.77H8.084a3 3 0 0 1-2.991-2.77L4.087 6.66l-.209.035a.75.75 0 0 1-.256-1.478A48.567 48.567 0 0 1 7.5 4.705v-.227c0-1.564 1.213-2.9 2.816-2.951a52.662 52.662 0 0 1 3.369 0c1.603.051 2.815 1.387 2.815 2.951Zm-6.136-1.452a51.196 51.196 0 0 1 3.273 0C14.39 3.05 15 3.684 15 4.478v.113a49.488 49.488 0 0 0-6 0v-.113c0-.794.609-1.428 1.364-1.452Zm-.355 5.945a.75.75 0 1 0-1.5.058l.347 9a.75.75 0 1 0 1.499-.058l-.346-9Zm5.48.058a.75.75 0 1 0-1.498-.058l-.347 9a.75.75 0 0 0 1.5.058l.345-9Z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
          </div>
            <div
                 class="text-slate-800  flex w-full items-center rounded-md hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100 py-1 border-b px-2"
            ><input v-model="column" placeholder="Add new column" class="border border-slate-200 rounded-md px-3 py-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow" type="text" />
              <div class="ml-auto grid place-items-center justify-self-end">
                <button  @click ="addColumn()" class="rounded-md border border-transparent  text-center text-xl px-2 font-bold transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none" type="button">
                  +
                </button>
              </div>
            </div>
          </div>
        </div>
        <button
            @click="createRetroBoard()"
            class="mt-4 w-full rounded-md bg-slate-800 py-2 px-4 border border-transparent text-center text-sm text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
            type="button">
          Create Retro Board
        </button>
      </form>
    </div>
  </div>


</template>

<script>

import {createRetroBoard} from "../../firebase/RetroBoardService.js";

export default {
  name: "CreateRetroBoard",
  props: {
    selectedTeam: String
  },
  data: () => ({
    boardName: "",
    columnList: ["Start","Stop","Continue"],
    column: ""
  }),

  methods: {
    createRetroBoard() {
      createRetroBoard(this.selectedTeam,this.boardName,this.columnList)
      this.$emit('close')
    },
    removeColumn(index) {
    this.columnList.splice(index, 1)
    },
    addColumn(){
      this.columnList.push(this.column)
      this.column = ""
    }
  }
}
</script>
