<template>
  <div>
    <div class="w-full max-w-sm min-w-[200px]">
      <div class="relative">
        <select
            v-model="selectedTeam"
            @change="handleTeamChange"
            class="w-full bg-transparent placeholder:text-slate-400 text-slate-700 text-sm border border-slate-200 rounded pl-3 pr-8 py-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-400 shadow-sm focus:shadow-md appearance-none cursor-pointer">

          <option v-for="team in teamList" :key="team.id" :value="team.id">{{ team.teamName }}</option>
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
    previousTeamId: "",
  }),
  created() {
    if (this.teamList.length > 0) {
      if(localStorage.getItem("selectedTeam")){
        this.selectedTeam = localStorage.getItem("selectedTeam");
      }else{
        this.selectedTeam = this.teamList[0].id
      }
      this.previousTeamId = this.selectedTeam;
    }
  },
  watch: {
    teamList() {
      if (this.teamList.length > 0) {
        if(localStorage.getItem("selectedTeam")){
          this.selectedTeam = localStorage.getItem("selectedTeam");
        }else{
          this.selectedTeam = this.teamList[0].id
        }
        this.previousTeamId = this.selectedTeam;
      }
    }
  },
  methods: {
    handleTeamChange() {
      // Eğer önceki seçim yoksa (ilk kez seçim) direkt emit et
      if (!this.previousTeamId || this.previousTeamId === this.selectedTeam) {
        this.previousTeamId = this.selectedTeam;
        this.$emit('select', this.selectedTeam);
        return;
      }

      // Değişiklik varsa App.vue'ya emit et (onay için)
      this.$emit('select', this.selectedTeam);
    },
    resetToSelected() {
      // Onay iptal edildiğinde eski değere geri dön
      this.selectedTeam = this.previousTeamId;
    }
  }
}
</script>
