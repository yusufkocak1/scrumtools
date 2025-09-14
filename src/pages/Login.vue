<template>
  <div class="min-h-screen w-full bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-4">
    <div class="w-full max-w-xl">
      <!-- Logo/Header Section -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-blue-100 rounded-xl flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
            <!-- Development Tools Icon -->
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
            <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
            <circle cx="17" cy="14" r="1.5"/>
            <path d="M6 15l2-2 2 2"/>
          </svg>
        </div>
        <h1 class="text-3xl font-bold text-gray-900 mb-2">Scrum Tools</h1>
        <p class="text-gray-600">Agile team management platform</p>
      </div>

      <!-- Login/Signup Card -->
      <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
        <!-- Tab Navigation -->
        <div class="bg-gray-50 p-6 border-b border-gray-200">
          <div class="flex bg-white rounded-xl p-1 shadow-sm">
            <button
              @click="mode = 'login'"
              :class="[
                'flex-1 py-3 px-4 text-sm font-medium rounded-lg transition-all duration-200',
                mode === 'login'
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              ]">
              Sign In
            </button>
            <button
              @click="mode = 'signup'"
              :class="[
                'flex-1 py-3 px-4 text-sm font-medium rounded-lg transition-all duration-200',
                mode === 'signup'
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              ]">
              Sign Up
            </button>
          </div>
        </div>

        <!-- Form Content -->
        <div class="p-8">
          <form class="space-y-6" @submit.prevent="mode === 'login' ? login() : signUp()">
            <!-- Name Field (only for signup) -->
            <div v-if="mode === 'signup'" class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Full Name
              </label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg class="w-5 h-5 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"></path>
                  </svg>
                </div>
                <input
                  v-model="name"
                  type="text"
                  required
                  class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 placeholder-gray-400"
                  placeholder="Enter your full name">
              </div>
            </div>

            <!-- Email Field -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Email Address
              </label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg class="w-5 h-5 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z"></path>
                    <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z"></path>
                  </svg>
                </div>
                <input
                  v-model="email"
                  type="email"
                  required
                  class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 placeholder-gray-400"
                  placeholder="Enter your email">
              </div>
            </div>

            <!-- Password Field -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Password
              </label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg class="w-5 h-5 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd"></path>
                  </svg>
                </div>
                <input
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  required
                  class="w-full pl-10 pr-12 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 placeholder-gray-400"
                  placeholder="Enter your password">
                <button
                  type="button"
                  @click="showPassword = !showPassword"
                  class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 transition-colors duration-200">
                  <svg v-if="!showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                  </svg>
                  <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L5.636 5.636m4.242 4.242L14.12 14.12m0 0l4.243 4.243M14.12 14.12L18.364 18.364"></path>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Action Buttons -->
            <div class="space-y-4">
              <!-- Primary Action Button -->
              <button
                type="submit"
                class="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-xl transition-all duration-200 transform hover:scale-105 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 shadow-lg hover:shadow-xl">
                <span v-if="mode === 'login'">Sign In</span>
                <span v-else>Create Account</span>
              </button>

              <!-- Forgot Password (only for login) -->
              <button
                v-if="mode === 'login'"
                type="button"
                @click="forgotPassword"
                class="w-full bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium py-3 px-4 rounded-xl transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2">
                Forgot Password?
              </button>
            </div>
          </form>
        </div>

        <!-- Footer -->
        <div class="bg-gray-50 px-8 py-4 border-t border-gray-200 text-center">
          <p class="text-sm text-gray-600">
            <span v-if="mode === 'login'">Don't have an account?</span>
            <span v-else>Already have an account?</span>
            <button
              @click="mode = mode === 'login' ? 'signup' : 'login'"
              class="text-blue-600 hover:text-blue-700 font-medium ml-1 transition-colors duration-200">
              <span v-if="mode === 'login'">Sign up</span>
              <span v-else>Sign in</span>
            </button>
          </p>
        </div>
      </div>

      <!-- Additional Info -->
      <div class="mt-8 text-center">
        <p class="text-sm text-gray-500">
          Secure agile project management for your team
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import {login, resetPassword, signUp} from "../firebase/AuthService.js";
import {createToast} from "mosha-vue-toastify";
export default {
  name: "Login",
  methods: {
    login() {
      login(this.email, this.password,(isLoggingSuccess)=>{
        if(isLoggingSuccess){
          this.isLogged = true
          this.$router.push('/')
        }else {
          this.isLogged = true
          this.$router.push('/')
          createToast('Username or password is incorrect.',{type:'danger',position:'top-center'})
        }
      })
    },
    signUp() {
      signUp(this.email, this.password,this.name,()=>{
        createToast("Sign up successfull",{type:'success',position:'top-center'})
        this.mode = "login"
      })
    },
    forgotPassword() {
      if(this.email){
        resetPassword(this.email)
      }
    }
  },
  mounted() {

    if (localStorage.getItem("user")) {
      this.isLogged = true
      this.$router.push('/')
    }
  },
  data: () => ({
    mode: "login",
    email: "",
    password: "",
    name:"",
    showPassword: false,
  })
}
</script>

<style scoped>
/* Add any additional styles here if needed */
</style>
