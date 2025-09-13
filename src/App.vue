<template>
  <div class="flex flex-col">
    <!-- Loading spinner -->
    <div v-if="loading" class="fixed inset-0 bg-white bg-opacity-75 flex items-center justify-center z-[10000]">
      <div class="animate-spin rounded-full h-32 w-32 border-b-2 border-gray-900"></div>
    </div>

    <Navbar
      :isLogged="isLogged"
      :name="name"
      @logout="logout"
    />

    <div class="flex justify-center">
      <RouterView/>
    </div>
  </div>
</template>
<script>
import {authService, getUserFromDB, logout} from "./firebase/AuthService.js";
import Navbar from "./components/Navbar.vue";
import './scripts/collapse.js'

export default {
  name: "App",
  components: {
    Navbar
  },
  data: () => ({
    isLogged: false,
    name: "",
    loading: true,
  }),
  methods: {
    logout() {
      logout()
      this.isLogged = false
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
      // Loading'i kapat
      this.loading = false;
    })
    this.getUserName()
    if (!this.isLogged) {
      this.$router.push('/')
    }
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