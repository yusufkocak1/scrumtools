<template>
  <div>
    <SelectPokerCardType  @selectPokerCardType="selectPokerCardType" :selectedPokerCardTypeName="selectedPokerCardTypeName" :team="team"></SelectPokerCardType>
    <div class="container flex flex-col justify-center  w-screen  mt-2">
      <PokerTable :isVotesVisible="isVotesVisible" :votes="votes" :members="team.members" @newRound="newRound"></PokerTable>
      <div class="py-16  border-4 flex justify-center flex-wrap gap-2 ">
      <pokerCard  v-for="pokerCard in selectedPokerCardType?.numbers" :number="pokerCard" :key="pokerCard" @selectPokerCard="selectPokerCard" :selectable="true" :newRound="newRound" :selectedCardNumber="selectedPokerCardNumber"></pokerCard>
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
  joinScrumPoker, leaveScrumPoker, listenScrumPoker, listenVotesVisible, setVotesVisible,
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
      isVotesVisible: false,
      selectedPokerCardNumber: null
    }
  },
  methods: {
    selectPokerCardType(selectedPokerCardType) {
      updateScrumPokerCardType(this.teamId, selectedPokerCardType.type)
      this.selectedPokerCardTypeName = selectedPokerCardType.type
      this.selectedPokerCardType = selectedPokerCardType
    },
    selectPokerCard(pokerCard) {
      if(this.selectedPokerCardNumber === pokerCard) {
        pokerCard = "-"
      }
      updateScrumPokerVote(this.teamId, auth.currentUser.email, pokerCard)
      this.selectedPokerCardNumber = pokerCard
    },
    newRound() {
      if(this.isVotesVisible) {
        this.isVotesVisible = false
        this.votes.forEach((vote) => {
          updateScrumPokerVote(this.teamId, vote.email, "-")
        })
      }else{
        this.isVotesVisible = true

      }
      setVotesVisible(this.teamId, this.isVotesVisible)

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
    listenVotesVisible(this.teamId,(isVotesVisible) => {
      this.isVotesVisible = isVotesVisible
    })
  },
}
</script>
