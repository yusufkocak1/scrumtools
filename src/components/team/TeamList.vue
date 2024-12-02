<template>
  <div>
    <div class="w-full max-w-sm min-w-[200px]">
      <div class="relative">
        <select
            v-model="selectedTeam"
            class="w-full bg-transparent placeholder:text-slate-400 text-slate-700 text-sm border border-slate-200 rounded pl-3 pr-8 py-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-400 shadow-sm focus:shadow-md appearance-none cursor-pointer"
            @change="$emit('select',selectedTeam)">
          <option v-for="team in teamList" :key="team.id" :value=team.id>{{ team.teamName }}</option>
        </select>
        <svg class="h-5 w-5 ml-1 absolute top-2.5 right-2.5 text-slate-700" fill="none" stroke="currentColor" stroke-width="1.2" viewBox="0 0 24 24"
             xmlns="http://www.w3.org/2000/svg">
          <path d="M8.25 15 12 18.75 15.75 15m-7.5-6L12 5.25 15.75 9" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
    </div>
  </div>

</template>

<script>
export default {
  name: "TeamList",
  props: {
    teamList: Array,
  },
  data: () => ({
    selectedTeam: "",
  }),
  created() {
    if (this.teamList.length > 0) {
      this.selectedTeam = this.teamList[0].id
    }
  },
  watch: {
    teamList() {
      if (this.teamList.length > 0) {
        this.selectedTeam = this.teamList[0].id
      }
    }
  }
  ,
}
</script>
