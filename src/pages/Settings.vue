<template>
  <div class="container">
    <!--profile-->
    <div class="py-16">
      <div class="flex flex-row border gap-2 bg-gray-100 rounded-xl p-6">
        <div class="flex flex-col w-full col-span-12 border p-4 rounded-xl">
          <div class="w-full max-w-sm min-w-[200px] flex  mt-4">
            <label class="block mb-2 text-md  font-bold text-slate-600">
              Email: {{ email.toUpperCase() }}
            </label>
          </div>
          <div class="w-full  flex items-center mt-4">
            <label class="  w-1/8 flex-wrap block mb-2 text-md  font-bold text-slate-600">
              Display Name:
            </label>

              <input v-model="displayName"
                     class=" mx-2 placeholder:text-slate-400 text-slate-700 text-md border border-slate-200 rounded-md text-left py-2 px-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow"
                     placeholder="Enter your text"
                     type="text"
                     @change="changeDisplayName"/>
            <button
                class="mx-2 rounded bg-slate-800 py-1 px-6  border border-transparent text-center text-sm text-white transition-all shadow-sm hover:shadow focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                type="button">
              Change
            </button>
          </div>
          <div class="w-full  flex items-center mt-4">
            <label class="w-1/8 flex-wrap block mb-2 text-md  font-bold text-slate-600">
              New Password:
            </label>
             <input v-model="password"
                     class=" mx-2  placeholder:text-slate-400 text-slate-700 text-md border border-slate-200 rounded-md text-left py-2 px-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow"
                     placeholder="Enter New Password"
                     type="password"
                     @change="changePassword"/>

            <button
                class=" mx-2 rounded bg-slate-800 py-1 px-6  border border-transparent text-center text-sm text-white transition-all shadow-sm hover:shadow focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                type="button">
              Change
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import {getAuth} from "firebase/auth";
import {changeEmail, changePassword, updateDisplayName} from "../firebase/AuthService.js";
import {createToast} from "mosha-vue-toastify";
import {listenTeams, updateDisplayNameFromTeam} from "../firebase/TeamService.js";
import teams from "./Teams.vue";

export default {
  name: "Settings",
  methods: {

    changeDisplayName() {
      const self = this;
      updateDisplayName(self.displayName, (response) => {
        createToast(response, {type: 'success', position: 'top-center'})
        self.teamList.forEach(team => {
          updateDisplayNameFromTeam(team.id, self.displayName, getAuth().currentUser.email)
        })
      })
    },
    changeEmail() {
      createToast("Bu biraz riskli şuan değiştiremeyelim", {type: 'warning', position: 'top-center'})
      /*
            changeEmail(this.email,(response)=>{
              createToast(response,{type:'success',position:'top-center'})
            })

       */
    },
    changePassword() {
      changePassword(this.password, (response) => {
        createToast(response, {type: 'success', position: 'top-center'})
      })
    },
    getAuth
  },

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
      email: "",
      teamList: [],
    }
  }
}
</script>
