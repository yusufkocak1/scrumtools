<template>
  <div class="flex flex-col">
    <div
        class=" w-screen  px-4 py-2 mx-auto bg-white bg-opacity-90  top-3 shadow lg:px-8 lg:py-3 backdrop-blur-lg backdrop-saturate-150 z-[9999]">
      <div class=" flex flex-wrap items-center justify-between mx-auto text-slate-800 font-extrabold text-2xl">
        <RouterLink to="/">Home</RouterLink>
        <div class="">
          <ul class="flex flex-col gap-2 mt-2 mb-4 lg:mb-0 lg:mt-0 lg:flex-row lg:items-center lg:gap-6">
            <li class="flex items-center p-1 text-sm gap-x-2 text-slate-600"><span
                class="items-center p-1 text-2xl font-bold gap-x-2 text-gray-800 hidden lg:block" >{{name}}</span></li>
            <li class="flex justify-end items-end p-1 text-sm gap-x-2 text-slate-600">
              <div v-if="isLogged"
                   class="flex items-center p-1 text-2xl font-bold gap-x-2 text-gray-800">
                <button
                    class=" ml-auto h-6 max-h-[40px] w-6 max-w-[40px]  rounded-lg text-center text-xs font-medium uppercase text-inherit transition-all hover:bg-transparent "
                    type="button"
                    @click="logout"
                >
                  <svg fill="#000000" height="24px" width="24px" version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 512 512" xml:space="preserve"><g id="SVGRepo_bgCarrier" stroke-width="0"></g><g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g><g id="SVGRepo_iconCarrier"> <g> <g> <g> <path d="M320,320c-11.782,0-21.333,9.551-21.333,21.333v128h-256V42.667h256v128c0,11.782,9.551,21.333,21.333,21.333 s21.333-9.551,21.333-21.333V21.333C341.333,9.551,331.782,0,320,0H21.333C9.551,0,0,9.551,0,21.333v469.333 C0,502.449,9.551,512,21.333,512H320c11.782,0,21.333-9.551,21.333-21.333V341.333C341.333,329.551,331.782,320,320,320z"></path> <path d="M507.164,269.52c0.204-0.248,0.38-0.509,0.571-0.764c0.226-0.302,0.461-0.598,0.671-0.913 c0.204-0.304,0.38-0.62,0.566-0.932c0.17-0.285,0.349-0.564,0.506-0.857c0.17-0.318,0.315-0.646,0.468-0.971 c0.145-0.306,0.297-0.607,0.428-0.921c0.13-0.315,0.236-0.637,0.35-0.957c0.121-0.337,0.25-0.669,0.354-1.013 c0.097-0.32,0.168-0.646,0.249-0.969c0.089-0.351,0.187-0.698,0.258-1.055c0.074-0.375,0.118-0.753,0.173-1.13 c0.044-0.311,0.104-0.617,0.135-0.933c0.138-1.4,0.138-2.811,0-4.211c-0.031-0.315-0.09-0.621-0.135-0.933 c-0.054-0.377-0.098-0.756-0.173-1.13c-0.071-0.358-0.169-0.704-0.258-1.055c-0.081-0.324-0.152-0.649-0.249-0.969 c-0.104-0.344-0.233-0.677-0.354-1.013c-0.115-0.32-0.22-0.642-0.35-0.957c-0.13-0.314-0.283-0.615-0.428-0.921 c-0.153-0.325-0.297-0.653-0.468-0.971c-0.157-0.293-0.336-0.572-0.506-0.857c-0.186-0.312-0.363-0.628-0.566-0.932 c-0.211-0.315-0.445-0.611-0.671-0.913c-0.191-0.255-0.368-0.516-0.571-0.764c-0.439-0.535-0.903-1.05-1.392-1.54 c-0.007-0.008-0.014-0.016-0.021-0.023l-85.333-85.333c-8.331-8.331-21.839-8.331-30.17,0s-8.331,21.839,0,30.17l48.915,48.915 H256c-11.782,0-21.333,9.551-21.333,21.333c0,11.782,9.551,21.333,21.333,21.333h183.163l-48.915,48.915 c-8.331,8.331-8.331,21.839,0,30.17c8.331,8.331,21.839,8.331,30.17,0l85.333-85.333c0.008-0.008,0.014-0.016,0.021-0.023 C506.261,270.57,506.725,270.055,507.164,269.52z"></path> </g> </g> </g> </g></svg>
                </button>
              </div>
            </li>
          </ul>
        </div>

      </div>
    </div>
    <div class="flex justify-center">
      <RouterView/>
    </div>
  </div>
</template>
<script>
import {authService, getUserFromDB, logout} from "./firebase/AuthService.js";
import './scripts/collapse.js'

export default {
  name: "App",
  data: () => ({
    isLogged: false,
    name: "",
  }),
  methods: {
    logout() {
      logout()
      this.islogged = false
      this.name = ""
      this.$router.push('/login')
    },
    getUserName() {
      if (!authService.currentUser) return
      getUserFromDB(authService.currentUser.email, (user) => {
        this.name = user.name
      })
    },
  },
  created() {
    authService.onAuthStateChanged((user) => {
      this.isLogged = !!user;
      if (!this.isLogged){
        localStorage.removeItem("user")
        this.$router.push('/login')
      }else{
        this.getUserName()
      }
    })
    this.getUserName()
    if (!this.isLogged) {
      this.$router.push('/')
    }
    this.$router.push('/login')
  },
  watch: {
    isLogged() {
      if (!this.isLogged) {
        this.$router.push('/login')
      }
    }
  }
}
</script>