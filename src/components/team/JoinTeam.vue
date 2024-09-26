
<template>
  <div class="relative flex flex-col  shadow-sm border border-slate-200 rounded-lg mx-2 lg:w-96 bg-white p-1">
      <div class="flex justify-end border-b py-1">
        <span class="w-full flex items-center ml-2 font-bold">Join Team</span>
      <button @click="$emit('close')" class="rounded-2xl  font-extrabold text-md bg-slate-800 py-1 px-2  border border-transparent text-center  text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-red-800 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none" type="button">
        x
      </button>
      </div>
        <div class="p-5 my-3">
          <div class="w-full max-w-sm min-w-[200px]">
            <div class="relative">
              <input type="text" v-model="teamId" class="w-full bg-transparent placeholder:text-slate-400 text-slate-700 text-sm border border-slate-200 rounded-md pl-3 pr-16 py-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow" placeholder="Team Id" />
              <button
                  class="absolute right-1 top-1 rounded bg-green-700 py-1 px-2.5 border border-transparent text-center text-sm text-white transition-all shadow-sm hover:shadow focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                  type="button"
                  @click="joinTeam()"
              >
                Join
              </button>
            </div>
          </div>      </div>
    </div>


</template>

<script>
import {addUserToTeam} from "../../firebase/TeamService.js";

export default {
  name: "JoinTeam",
  data: () => ({
    teamId: "",
  }),

  methods: {
    joinTeam() {
      const email = localStorage.getItem("user")
      addUserToTeam(email,this.teamId)
      this.$emit('close')
    }
  }
}
</script>
