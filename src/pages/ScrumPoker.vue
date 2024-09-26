<template>
  <div>
    <SelectPokerCardType @selectPokerCardType="selectPokerCardType" :selectedPokerCardTypeName="selectedPokerCardTypeName" :team="team"></SelectPokerCardType>
    <div class="container flex flex-row justify-center  border w-screen  mt-2">
      <PokerTable></PokerTable>
      <pokerCard v-for="pokerCard in selectedPokerCardType?.numbers" :key="pokerCard"></pokerCard>
      <span class="p-4 font-extrabold text-4xl flex justify-center items-center text-center">Bir Müslüman kardeşimiz yardım etmek isterse eksiklerini tamamlayıp bu özelliği de kullanabiliriz.</span>
    </div>
  </div>
</template>
<script>
import PokerTable from "../components/poker/pokerTable.vue";
import PokerCard from "../components/poker/pokerCard.vue";
import SelectPokerCardType from "../components/poker/SelectPokerCardType.vue";
import {getTeamById} from "../firebase/TeamService.js";
import {createScrumPoker, getScrumPokerFromTeam, updateScrumPokerCardType} from "../firebase/ScrumPokerService.js";

export default {
  name: "ScrumPoker",
  components: {SelectPokerCardType, PokerCard, PokerTable},
  props: {
    teamId: String
  },
  data() {
    return {
      selectedPokerCardType: Object,
      team: {},
      poker: {},
      selectedPokerCardTypeName: ""
    }
  },
  methods: {
    selectPokerCardType(selectedPokerCardType) {
      updateScrumPokerCardType(this.teamId,this.poker.id, selectedPokerCardType.type)
      this.selectedPokerCardTypeName = selectedPokerCardType.type
      this.selectedPokerCardType = selectedPokerCardType
    }
  },
  created() {
    getTeamById(this.teamId, (team) => {
      this.team = team
    })
    getScrumPokerFromTeam(this.teamId, (scrumPoker) => {
      if(!scrumPoker){
        createScrumPoker(this.teamId, "Fibonacci")
      }else{
        console.log(scrumPoker)
        this.poker = scrumPoker[0]
        this.selectedPokerCardTypeName = this.poker.cardType
      }
    })
  }
}
</script>
