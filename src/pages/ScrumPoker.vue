<template>
  <div>
    <SelectPokerCardType  @selectPokerCardType="selectPokerCardType" :selectedPokerCardTypeName="selectedPokerCardTypeName" :team="team"></SelectPokerCardType>
    <div class="container flex flex-col justify-center  border w-screen  mt-2 gap-5">
      <PokerTable :isVotesVisible="isVotesVisible" :votes="votes" :members="team.members" @newRound="newRound"></PokerTable>
      <div class="p-2 flex flex-row justify-center lg:justify-start flex-wrap gap-2 absolute bottom-24">
      <pokerCard  v-for="pokerCard in selectedPokerCardType?.numbers" :number="pokerCard" :key="pokerCard" @selectPokerCard="selectPokerCard" :selectable="true" :newRound="newRound"></pokerCard>
      </div>
    </div>
  </div>
</template>
<script>
import PokerTable from "../components/poker/pokerTable.vue";
import PokerCard from "../components/poker/pokerCard.vue";
import SelectPokerCardType from "../components/poker/SelectPokerCardType.vue";
import {getTeamById} from "../firebase/TeamService.js";
import {
  joinScrumPoker, leaveScrumPoker, listenScrumPoker,
  updateScrumPokerCardType,
  updateScrumPokerVote
} from "../firebase/ScrumPokerService.js";
import {auth} from "../firebase/Firebase.js";

export default {
  name: "ScrumPoker",
  components: {SelectPokerCardType, PokerCard, PokerTable},
  props: {
    teamId: String
  },
  data() {
    return {
      maintenance: false,
      selectedPokerCardType: Object,
      team: {},
      selectedPokerCardTypeName: "",
      votes: [],
      isVotesVisible: false
    }
  },
  methods: {
    selectPokerCardType(selectedPokerCardType) {
      updateScrumPokerCardType(this.teamId, selectedPokerCardType.type)
      this.selectedPokerCardTypeName = selectedPokerCardType.type
      this.selectedPokerCardType = selectedPokerCardType
    },
    selectPokerCard(pokerCard) {
      updateScrumPokerVote(this.teamId, auth.currentUser.email, pokerCard)
    },
    newRound() {
      if(this.isVotesVisible) {
        this.votes.forEach((vote) => {
          updateScrumPokerVote(this.teamId, vote.email, "-").then(() => {
            this.isVotesVisible = false
          })
        })
      }else{
        this.isVotesVisible = true
      }
    }
  },
  unmounted() {
    leaveScrumPoker(this.teamId, auth.currentUser.email)
  },
  created() {
    getTeamById(this.teamId, (team) => {
      this.team = team
      if (!team.pokerCardType) {
        updateScrumPokerCardType(team.id, "Fibonacci")
        this.selectedPokerCardTypeName = "Fibonacci"
      }else{
        this.selectedPokerCardTypeName = this.team.pokerCardType
      }

      joinScrumPoker(this.teamId, auth.currentUser.email)
    })
    listenScrumPoker(this.teamId,(votes) => {
      this.votes = votes
    })
  }
}
</script>
