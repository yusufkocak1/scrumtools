<template>
  <nav class="flex w-20 flex-col gap-2 py-4 bg-white border-r border-gray-200 shadow-sm">
    <!-- Retrospective -->
    <div @click="gotoRetrospective"
         class="group relative flex items-center justify-center p-3 mx-2 rounded-xl transition-all duration-200 hover:bg-blue-50 focus:bg-blue-50 active:bg-blue-100 cursor-pointer">
      <div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition-colors">
        <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
          <path d="M16 4c0-1.11.89-2 2-2s2 .89 2 2-.89 2-2 2-2-.89-2-2zm4 18v-6h2.5l-2.54-7.63A1.5 1.5 0 0 0 18.54 7H16c-.8 0-1.54.37-2 .95L12.58 9.7c-.35.47-.98.75-1.64.75H9.5a2 2 0 0 0-2 2v1a1 1 0 0 0 1 1H10v8h2v-8h.5c.3 0 .6-.1.85-.29L14.7 12H16l2.05 6H20z"/>
          <path d="M12.5 11.5c.83 0 1.5-.67 1.5-1.5s-.67-1.5-1.5-1.5S11 9.17 11 10s.67 1.5 1.5 1.5z"/>
          <path d="M5.5 6c1.11 0 2-.89 2-2s-.89-2-2-2-2 .89-2 2 .89 2 2 2zm1.5 1h-3c-.83 0-1.54.5-1.84 1.22L.66 12.08c-.11.26-.16.54-.16.82 0 1.11.89 2 2 2h2.5v7h3v-7h1l1.31-3.92C10.69 9.77 9.39 7 5.5 7z"/>
          <path d="M9 14l-2 2h6l-2-2H9z"/>
        </svg>
      </div>
      <!-- Tooltip -->
      <div class="absolute left-full ml-3 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
        Retrospective
      </div>
    </div>

    <!-- Scrum Poker -->
    <div @click="gotoScrumPoker"
         class="group relative flex items-center justify-center p-3 mx-2 rounded-xl transition-all duration-200 hover:bg-green-50 focus:bg-green-50 active:bg-green-100 cursor-pointer">
      <div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center group-hover:bg-green-200 transition-colors">
        <svg class="w-5 h-5 text-green-600" fill="currentColor" viewBox="0 0 512 512">
          <path d="M452.279,114.361L298.254,73.25V38.138C298.254,17.109,282.291,0,261.203,0H70.512C49.425,0,31.076,17.114,31.076,38.152 v334.007c0,21.037,18.307,38.151,39.345,38.151h66.145l-1.146,4.276c-2.641,9.855-1.275,20.145,3.845,28.977 c5.112,8.819,13.359,15.129,23.223,17.772l184.192,49.354c3.31,0.886,6.637,1.31,9.911,1.31c16.873,0,32.344-11.246,36.905-28.264 l86.149-322.626C485.088,140.79,472.648,119.819,452.279,114.361z M141.679,391.226H70.421c-10.514,0-20.261-8.553-20.261-19.067 V38.152c0-10.514,9.788-19.068,20.353-19.068h190.691c10.565,0,17.967,8.547,17.967,19.054v29.998l-11.084-3.13 c-20.359-5.448-41.071,6.633-46.518,26.952l-5.493,21.059L182.84,70.456c-1.808-2.311-4.543-3.661-7.477-3.661 s-5.688,1.35-7.496,3.661L81.997,180.19c-2.703,3.455-2.699,8.307,0.005,11.762l84.231,107.626L141.679,391.226z M210.075,135.966 L172.417,276.51l-70.779-90.439L175.4,91.819l33.99,43.432C209.598,135.517,209.846,135.728,210.075,135.966z M461.508,156.171 l-86.447,322.626c-2.72,10.157-13.236,16.192-23.443,13.458l-184.192-49.354c-4.952-1.327-9.089-4.49-11.652-8.91 c-2.557-4.41-3.239-9.548-1.921-14.467l86.447-322.627c2.721-10.155,13.239-16.191,23.443-13.457l184.193,49.354 C458.14,135.529,464.229,146.016,461.508,156.171z"/>
          <path d="M365.115,212.306c-13.618-3.652-28.135-1.539-40.214,5.448c-6.97-12.091-18.484-21.176-32.103-24.826 c-28.343-7.596-57.68,9.658-65.398,38.457c-4.46,16.647,9.303,46.354,21.635,68.343c6.121,10.914,27.125,46.802,40.406,50.361 c1.045,0.28,2.23,0.41,3.535,0.41c15.274,0,46.727-17.931,56.639-23.82c21.675-12.877,48.448-31.722,52.908-48.37 C410.24,249.51,393.46,219.901,365.115,212.306z M384.089,273.37c-1.558,5.817-15.315,19.398-41.834,35.47 c-23.175,14.044-42.023,21.591-47.803,22.552c-4.525-3.722-17.075-19.681-30.122-43.431 c-14.932-27.178-20.054-45.819-18.496-51.636c4.203-15.685,18.224-26.101,33.4-26.101c2.853,0,5.748,0.368,8.625,1.139 c11.634,3.117,20.551,12.044,23.85,23.88c0.906,3.251,3.462,5.781,6.722,6.655c3.255,0.876,6.737-0.04,9.148-2.402 c8.774-8.601,20.962-11.878,32.596-8.755C378.356,235.611,389.082,254.735,384.089,273.37z"/>
        </svg>
      </div>
      <!-- Tooltip -->
      <div class="absolute left-full ml-3 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
        Scrum Poker
      </div>
    </div>

    <!-- Work List -->
    <div @click="gotoWorkList"
         class="group relative flex items-center justify-center p-3 mx-2 rounded-xl transition-all duration-200 hover:bg-purple-50 focus:bg-purple-50 active:bg-purple-100 cursor-pointer">
      <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center group-hover:bg-purple-200 transition-colors">
        <svg class="w-5 h-5 text-purple-600" fill="currentColor" viewBox="0 0 20 20">
          <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"></path>
        </svg>
      </div>
      <!-- Tooltip -->
      <div class="absolute left-full ml-3 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
        Scrum Board
      </div>
    </div>

    <!-- Code Share -->
    <div @click="gotoShare"
         class="group relative flex items-center justify-center p-3 mx-2 rounded-xl transition-all duration-200 hover:bg-orange-50 focus:bg-orange-50 active:bg-orange-100 cursor-pointer">
      <div class="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center group-hover:bg-orange-200 transition-colors">
        <svg class="w-5 h-5 text-orange-600" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"></path>
        </svg>
      </div>
      <!-- Tooltip -->
      <div class="absolute left-full ml-3 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
        Code Share
      </div>
    </div>
  </nav>
</template>

<script>
import {createToast} from "mosha-vue-toastify";

export default {
  name: "SideBar",
  props: {
    teamId: String
  },
  methods: {
    gotoSettings() {
      this.$router.push(`/Settings`)
    },
    gotoScrumPoker() {
      this.$router.push(`/scrumPoker/${this.teamId}`)
    },
    gotoTeams() {
      this.$router.push(`/teams`)
    },
    gotoWorkList() {
      this.$router.push(`/workList/${this.teamId}`)
    },
    gotoShare() {
      this.$router.push(`/codeShare/${this.teamId}`)
    },
    gotoRetrospective() {
      this.$router.push(`/retrospective`)
    }
  }
}
</script>
