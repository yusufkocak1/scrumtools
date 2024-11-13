<template>
  <div class="container">
    <!--profile-->
    <div class="py-16">
      <div class="flex flex-row border gap-2 bg-gray-100 rounded-xl p-6">
        <div class="flex flex-col col-span-6 border rounded-xl">
          <div class="flex flex-wrap">
            <div class="w-full max-w-sm min-w-[200px] relative mt-4">
              <label class="block mb-2 text-md  font-bold text-slate-600">
                Display Name
              </label>

              <div class="relative">
                <input type="text"
                       @change="changeDisplayName"
                       v-model="displayName"
                       class="w-full  placeholder:text-slate-400 text-slate-700 text-md border border-slate-200 rounded-md text-left py-2 px-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow" placeholder="Enter your text" />
                <button class="absolute right-1 top-1 bottom-1 rounded bg-slate-800 py-1 px-6  border border-transparent text-center text-sm text-white transition-all shadow-sm hover:shadow focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none" type="button">
                  Change
                </button>
              </div>
            </div>
            <div class="w-full max-w-sm min-w-[200px] relative mt-4">
              <label class="block mb-2 text-md  font-bold text-slate-600">
                Email
              </label>

              <div class="relative">
                <input type="text"
                       v-model="email"
                       class="w-full  placeholder:text-slate-400 text-slate-700 text-md border border-slate-200 rounded-md text-left py-2 px-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow" placeholder="Enter your text" />
                <button
                    @click="changeEmail"
                    class="absolute right-1 top-1 bottom-1 rounded bg-slate-800 py-1 px-6 border border-transparent text-center text-sm text-white transition-all shadow-sm hover:shadow focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none" type="button">
                  Change
                </button>
              </div>
            </div>
          </div>
        </div>

      </div>
      <div class="flex justify-center items-center">
      </div>
    </div>
  </div>
</template>
<script>
import {getAuth} from "firebase/auth";
import {changeEmail, updateDisplayName} from "../firebase/AuthService.js";
import {createToast} from "mosha-vue-toastify";
import {listenTeams, updateDisplayNameFromTeam} from "../firebase/TeamService.js";
import teams from "./Teams.vue";

export default {
  name: "Settings",
  methods: {

    changeDisplayName() {
      const self = this;
      updateDisplayName(self.displayName,(response)=>{
        createToast(response,{type:'success',position:'top-center'})
        self.teamList.forEach(team=>{
          updateDisplayNameFromTeam(team.id,self.displayName,getAuth().currentUser.email)
        })
      })
    },
    changeEmail() {
      createToast("Bu biraz riskli şuan değiştiremeyelim",{type:'warning',position:'top-center'})
/*
      changeEmail(this.email,(response)=>{
        createToast(response,{type:'success',position:'top-center'})
      })

 */
    },
    getAuth},

  created() {
    this.displayName = getAuth().currentUser.displayName
    this.email = getAuth().currentUser.email
    listenTeams((response) => {
      this.teamList = response;
    })
  },
  data: () => {
    return {
      displayName: "",
      email:"",
      teamList: [],
    }
  }
}
</script>
