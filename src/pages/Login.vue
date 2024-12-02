<template>
  <div class="relative flex w-full max-w-[36rem] flex-col rounded-xl  bg-clip-border text-gray-700 shadow-md">
    <div
        class="relative grid p-8 m-0 overflow-hidden border text-center  place-items-center rounded-xl bg-clip-border shadow-gray-900/20">
      <h5 class="flex font-sans text-xl antialiased font-semibold leading-snug tracking-normal ">
        Scrum Tools
      </h5>
    </div>
    <div class="p-6">
      <div class="relative right-0">
        <ul class="relative flex flex-wrap px-1.5 py-1.5 list-none rounded-md bg-slate-100" data-tabs="tabs"
            role="list">
          <li :class="['z-30 flex-auto text-center rounded py-2 ', mode==='login' ? 'bg-white' : '']"
              @click="()=>mode='login'">
            <span class=" flex items-center justify-center">
              Login
            </span>
          </li>
          <li :class="['z-30 flex-auto text-center rounded py-2', mode==='signup' ? 'bg-white' : '']"
              @click="()=>mode='signup'">
            <span class="flex items-center justify-center ">
              Signup
            </span>
          </li>
        </ul>
        <div class="p-5" data-tab-content="">
          <div id="login" role="tabpanel">
            <form class="flex flex-col gap-4 mt-12">
              <div  v-if="mode !== 'login'">
                <p>Your Name
                </p>
                <div class="relative h-10 w-full min-w-[200px]">
                  <input
                      class="peer h-full w-full rounded-[7px] border border-blue-gray-200 border-t-transparent !border-t-blue-gray-200 bg-transparent px-3 py-2.5 font-sans text-sm font-normal text-blue-gray-700 outline outline-0 transition-all placeholder-shown:border placeholder-shown:border-blue-gray-200 placeholder-shown:border-t-blue-gray-200 focus:border-2 focus:border-gray-900 focus:border-t-transparent focus:!border-t-gray-900 focus:outline-0 disabled:border-0 disabled:bg-blue-gray-50"
                      v-model="name"/>
                  <label
                      class="before:content[' '] after:content[' '] pointer-events-none absolute left-0 -top-1.5 flex h-full w-full select-none !overflow-visible truncate text-[11px] font-normal leading-tight text-gray-500 transition-all before:pointer-events-none before:mt-[6.5px] before:mr-1 before:box-border before:block before:h-1.5 before:w-2.5 before:rounded-tl-md before:border-t before:border-l before:border-blue-gray-200 before:transition-all before:content-none after:pointer-events-none after:mt-[6.5px] after:ml-1 after:box-border after:block after:h-1.5 after:w-2.5 after:flex-grow after:rounded-tr-md after:border-t after:border-r after:border-blue-gray-200 after:transition-all after:content-none peer-placeholder-shown:text-sm peer-placeholder-shown:leading-[3.75] peer-placeholder-shown:text-blue-gray-500 peer-placeholder-shown:before:border-transparent peer-placeholder-shown:after:border-transparent peer-focus:text-[11px] peer-focus:leading-tight peer-focus:text-gray-900 peer-focus:before:border-t-2 peer-focus:before:border-l-2 peer-focus:before:!border-gray-900 peer-focus:after:border-t-2 peer-focus:after:border-r-2 peer-focus:after:!border-gray-900 peer-disabled:text-transparent peer-disabled:before:border-transparent peer-disabled:after:border-transparent peer-disabled:peer-placeholder-shown:text-blue-gray-500"></label>
                </div>
              </div>
              <div>
                <p class="block mb-2 font-sans text-sm antialiased font-medium leading-normal text-blue-gray-900">
                  Your Email
                </p>
                <div class="relative h-10 w-full min-w-[200px]">
                  <input
                      class="peer h-full w-full rounded-[7px] border border-blue-gray-200 border-t-transparent !border-t-blue-gray-200 bg-transparent px-3 py-2.5 font-sans text-sm font-normal text-blue-gray-700 outline outline-0 transition-all placeholder-shown:border placeholder-shown:border-blue-gray-200 placeholder-shown:border-t-blue-gray-200 focus:border-2 focus:border-gray-900 focus:border-t-transparent focus:!border-t-gray-900 focus:outline-0 disabled:border-0 disabled:bg-blue-gray-50"
                      placeholder="name@mail.com"
                      v-model="email"
                      type="email"/>
                  <label
                      class="before:content[' '] after:content[' '] pointer-events-none absolute left-0 -top-1.5 flex h-full w-full select-none !overflow-visible truncate text-[11px] font-normal leading-tight text-gray-500 transition-all before:pointer-events-none before:mt-[6.5px] before:mr-1 before:box-border before:block before:h-1.5 before:w-2.5 before:rounded-tl-md before:border-t before:border-l before:border-blue-gray-200 before:transition-all before:content-none after:pointer-events-none after:mt-[6.5px] after:ml-1 after:box-border after:block after:h-1.5 after:w-2.5 after:flex-grow after:rounded-tr-md after:border-t after:border-r after:border-blue-gray-200 after:transition-all after:content-none peer-placeholder-shown:text-sm peer-placeholder-shown:leading-[3.75] peer-placeholder-shown:text-blue-gray-500 peer-placeholder-shown:before:border-transparent peer-placeholder-shown:after:border-transparent peer-focus:text-[11px] peer-focus:leading-tight peer-focus:text-gray-900 peer-focus:before:border-t-2 peer-focus:before:border-l-2 peer-focus:before:!border-gray-900 peer-focus:after:border-t-2 peer-focus:after:border-r-2 peer-focus:after:!border-gray-900 peer-disabled:text-transparent peer-disabled:before:border-transparent peer-disabled:after:border-transparent peer-disabled:peer-placeholder-shown:text-blue-gray-500"></label>
                </div>
              </div>
              <div>
                <p class="block mb-2 font-sans text-sm antialiased font-medium leading-normal text-blue-gray-900">
                  Password
                </p>
                <div class="relative h-10 w-full min-w-[200px]">
                  <input
                      class="peer h-full w-full rounded-[7px] border border-blue-gray-200 border-t-transparent !border-t-blue-gray-200 bg-transparent px-3 py-2.5 font-sans text-sm font-normal text-blue-gray-700 outline outline-0 transition-all placeholder-shown:border placeholder-shown:border-blue-gray-200 placeholder-shown:border-t-blue-gray-200 focus:border-2 focus:border-gray-900 focus:border-t-transparent focus:!border-t-gray-900 focus:outline-0 disabled:border-0 disabled:bg-blue-gray-50"
                      placeholder="************"
                      v-model="password"
                      type="password"/>
                  <label
                      class="before:content[' '] after:content[' '] pointer-events-none absolute left-0 -top-1.5 flex h-full w-full select-none !overflow-visible truncate text-[11px] font-normal leading-tight text-gray-500 transition-all before:pointer-events-none before:mt-[6.5px] before:mr-1 before:box-border before:block before:h-1.5 before:w-2.5 before:rounded-tl-md before:border-t before:border-l before:border-blue-gray-200 before:transition-all before:content-none after:pointer-events-none after:mt-[6.5px] after:ml-1 after:box-border after:block after:h-1.5 after:w-2.5 after:flex-grow after:rounded-tr-md after:border-t after:border-r after:border-blue-gray-200 after:transition-all after:content-none peer-placeholder-shown:text-sm peer-placeholder-shown:leading-[3.75] peer-placeholder-shown:text-blue-gray-500 peer-placeholder-shown:before:border-transparent peer-placeholder-shown:after:border-transparent peer-focus:text-[11px] peer-focus:leading-tight peer-focus:text-gray-900 peer-focus:before:border-t-2 peer-focus:before:border-l-2 peer-focus:before:!border-gray-900 peer-focus:after:border-t-2 peer-focus:after:border-r-2 peer-focus:after:!border-gray-900 peer-disabled:text-transparent peer-disabled:before:border-transparent peer-disabled:after:border-transparent peer-disabled:peer-placeholder-shown:text-blue-gray-500"></label>
                </div>
              </div>
                            <button
                  v-if="mode === 'login'"
                  class="select-none rounded-lg bg-green-700 py-3.5 px-7 text-center align-middle font-sans text-sm font-bold uppercase text-white shadow-md shadow-gray-900/10 transition-all hover:shadow-lg hover:shadow-gray-900/20 focus:opacity-[0.85] focus:shadow-none active:opacity-[0.85] active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                  type="button"
                  @click="()=>login()">
                login
              </button>
              <button v-else
                      class="select-none rounded-lg bg-amber-700 py-3.5 px-7 text-center align-middle font-sans text-sm font-bold uppercase text-white shadow-md shadow-gray-900/10 transition-all hover:shadow-lg hover:shadow-gray-900/20 focus:opacity-[0.85] focus:shadow-none active:opacity-[0.85] active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                      type="button"
                      @click="()=>signUp()">
                signup
              </button>
              <button
                  v-if="mode === 'login'"
                  class="select-none rounded-lg bg-red-700 py-3.5 px-7 text-center align-middle font-sans text-sm font-bold uppercase text-white shadow-md shadow-gray-900/10 transition-all hover:shadow-lg hover:shadow-gray-900/20 focus:opacity-[0.85] focus:shadow-none active:opacity-[0.85] active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                  type="button"
                  @click="()=>forgotPassword()">
                Forgot Password
              </button>
            </form>
          </div>
        </div>
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
  })
}
</script>
