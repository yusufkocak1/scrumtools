<template>
  <div class="relative flex flex-col   shadow-sm border border-slate-200 rounded-lg lg:w-[30%] bg-white p-1 mx-2">
    <div class="flex justify-end border-b py-1">
      <span class="w-full flex items-center ml-2 font-bold">Retro Item</span>
      <button
          class="rounded-2xl  font-extrabold text-md bg-slate-800 py-1 px-2  border border-transparent text-center  text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-red-800 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
          type="button"
          @click="$emit('close')">
        x
      </button>
    </div>
    <div class="p-5 mb-2  border border-slate-200">
      <div class="border-b-2 border-slate-200 my-2">
        <div class="pr-10">
          <div v-if="!edit"  @click="edit=true" class="text-sm text-white bg-slate-800 rounded-md p-6  border  mb-2 font-medium right-0 ">
            {{ item.value }}
          </div>
          <textarea
              v-else
              v-model="item.value"
              @blur="updateItem()"
              class="peer h-full min-h-[100px] w-full resize-none rounded-[7px] border border-blue-gray-200 border-t-transparent bg-transparent px-3 py-2.5 font-sans text-sm font-normal text-blue-gray-700 outline outline-0 transition-all placeholder-shown:border placeholder-shown:border-blue-gray-200 placeholder-shown:border-t-blue-gray-200 focus:border-2 focus:border-gray-900 focus:border-t-transparent focus:outline-0 disabled:resize-none disabled:border-0 disabled:bg-blue-gray-50"
              >
          </textarea>
          <label
              class="before:content[' '] after:content[' '] pointer-events-none absolute left-0 -top-1.5 flex h-full w-full select-none text-[11px] font-normal leading-tight text-blue-gray-400 transition-all before:pointer-events-none before:mt-[6.5px] before:mr-1 before:box-border before:block before:h-1.5 before:w-2.5 before:rounded-tl-md before:border-t before:border-l before:border-blue-gray-200 before:transition-all after:pointer-events-none after:mt-[6.5px] after:ml-1 after:box-border after:block after:h-1.5 after:w-2.5 after:flex-grow after:rounded-tr-md after:border-t after:border-r after:border-blue-gray-200 after:transition-all peer-placeholder-shown:text-sm peer-placeholder-shown:leading-[3.75] peer-placeholder-shown:text-blue-gray-500 peer-placeholder-shown:before:border-transparent peer-placeholder-shown:after:border-transparent peer-focus:text-[11px] peer-focus:leading-tight peer-focus:text-gray-900 peer-focus:before:border-t-2 peer-focus:before:border-l-2 peer-focus:before:border-gray-900 peer-focus:after:border-t-2 peer-focus:after:border-r-2 peer-focus:after:border-gray-900 peer-disabled:text-transparent peer-disabled:before:border-transparent peer-disabled:after:border-transparent peer-disabled:peer-placeholder-shown:text-blue-gray-500">
            Message
          </label>
        </div>
        <div class="pl-10">
          <RetroItemComment v-for="comment in comments" :key="comment.id" :comment="comment"
                            @removeComment="removeComment"></RetroItemComment>
        </div>
      </div>
      <div
          class="text-slate-800  flex w-full items-center rounded-md hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100 py-1 border-b px-2"
      ><input v-model="commentInput"
              class="border border-slate-200 rounded-md px-3 py-2 w-full transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow"
              placeholder="Add new comment"
              type="text"
              @keydown.enter="addComment()"/>
        <div class="ml-auto grid place-items-center justify-self-end">
          <button
              class="rounded-md border border-transparent  text-center text-xl px-2 font-bold transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
              type="button"
              @click="addComment()">
            +
          </button>
        </div>
      </div>
    </div>
    <div class="flex justify-end gap-2 p-2">
      <button class="w-6 h-6 bg-green-700 hover:border-2 rounded-3xl" @click="setStatus('ACCEPTED')"></button>
      <button class="w-6 h-6 bg-red-700 rounded-3xl hover:border-2" @click="setStatus('REJECTED')"></button>
      <button class="w-6 h-6 bg-amber-700 rounded-3xl hover:border-2" @click="setStatus('CONFLICT')"></button>
    </div>
  </div>

</template>
<script>

import {
  createRetroItemComment,
  getRetroItemComments,
  removeRetroItemComment, updateRetroItem,
  updateRetroItemStatus
} from "../../firebase/RetroBoardService.js";
import {createToast} from "mosha-vue-toastify";
import RetroItemComment from "./RetroItemComment.vue";

export default {
  name: "RetroItemDetail",
  components: {RetroItemComment},
  props: {
    item: Object,
    boardId: String,
    teamId: String
  },
  data() {
    return {
      commentInput: "",
      comments: [],
      edit: false
    }
  },
  methods: {

    closeDetail() {
      this.$emit('closeDetail')
    },
    removeItem() {
      this.$emit('removeItem')
    },
    updateItem() {
      updateRetroItem(this.teamId, this.boardId, this.item.column, this.item.id, this.item.value)
    },
    addComment() {
      const newComment = {
        value: this.commentInput,
        owner: localStorage.getItem("user")
      }
      createRetroItemComment(this.teamId, this.boardId, this.item.column, this.item.id, newComment).then(() => {
        createToast('comment added ', {type: 'success', position: 'top-center'})
        this.getRetroItemComments()
        this.commentInput = ""
      })
    },

    removeComment(commentId) {
      removeRetroItemComment(this.teamId, this.boardId, this.item.column, this.item.id, commentId).then(() => {
        createToast('comment removed ', {type: 'success', position: 'top-center'})
        this.getRetroItemComments()
      })
    },
    getRetroItemComments() {
      getRetroItemComments(this.teamId, this.boardId, this.item.column, this.item.id, (comments) => {
        this.comments = comments
      })
    },

    setStatus(status) {
      updateRetroItemStatus(this.teamId, this.boardId, this.item.column, this.item.id, status)
    }
  },
  created() {
    this.getRetroItemComments()
  }
}
</script>

