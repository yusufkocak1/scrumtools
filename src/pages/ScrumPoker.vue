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
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import PokerTable from "../components/poker/pokerTable.vue";
import PokerCard from "../components/poker/pokerCard.vue";
import SelectPokerCardType from "../components/poker/SelectPokerCardType.vue";
import {getTeamById} from "../firebase/TeamService.js";
import {
  joinScrumPoker, leaveScrumPoker, listenScrumPoker, listenVotesVisible, setVotesVisible,
  updateScrumPokerCardType,
  updateScrumPokerVote,
  cleanupListeners
} from "../firebase/ScrumPokerService.js";
import {auth} from "../firebase/Firebase.js";

export default {
  name: "ScrumPoker",
  components: {SelectPokerCardType, PokerCard, PokerTable},
  props: {
    teamId: String
  },
  setup(props) {
    const maintenance = ref(false)
    const selectedPokerCardType = ref({})
    const team = reactive({})
    const selectedPokerCardTypeName = ref("")
    const votes = ref(new Map()) // Map kullanarak O(1) erişim
    const isVotesVisible = ref(false)
    const selectedPokerCardNumber = ref(null)
    
    // Debounce için
    let updateTimeout = null
    let unsubscribeVotes = null
    let unsubscribeVisibility = null

    // Computed property for votes array (sadece gerektiğinde hesaplanır)
    const votesArray = computed(() => Array.from(votes.value.values()))

    const selectPokerCardType = (selectedType) => {
      updateScrumPokerCardType(props.teamId, selectedType.type)
      selectedPokerCardTypeName.value = selectedType.type
      selectedPokerCardType.value = selectedType
    }
    
    const selectPokerCard = (pokerCard) => {
      if(selectedPokerCardNumber.value === pokerCard) {
        pokerCard = "-"
      }
      
      // Debounce uygula
      if (updateTimeout) {
        clearTimeout(updateTimeout)
      }
      
      updateTimeout = setTimeout(() => {
        updateScrumPokerVote(props.teamId, auth.currentUser.email, pokerCard)
      }, 300) // 300ms debounce
      
      selectedPokerCardNumber.value = pokerCard
    }
    
    const newRound = async () => {
      if(isVotesVisible.value) {
        isVotesVisible.value = false
        // Batch update yapmak için Promise.all kullan
        const updates = Array.from(votes.value.values()).map((vote) =>
          updateScrumPokerVote(props.teamId, vote.email, "-")
        )
        await Promise.all(updates)
        selectedPokerCardNumber.value = null
      } else {
        isVotesVisible.value = true
      }
      await setVotesVisible(props.teamId, isVotesVisible.value)
    }
    
    onMounted(async () => {
      try {
        await getTeamById(props.teamId, (teamData) => {
          Object.assign(team, teamData)
          if (!teamData.pokerCardType) {
            updateScrumPokerCardType(teamData.id, "Fibonacci")
            selectedPokerCardTypeName.value = "Fibonacci"
          } else {
            selectedPokerCardTypeName.value = teamData.pokerCardType
          }
        })

        await joinScrumPoker(props.teamId, auth.currentUser.email)

        // Optimize listener - Map kullanarak veri yapısını iyileştir
        unsubscribeVotes = listenScrumPoker(props.teamId, (newVotes) => {
          const voteMap = new Map()
          newVotes.forEach(vote => {
            voteMap.set(vote.email, vote)
          })
          votes.value = voteMap
        })

        unsubscribeVisibility = listenVotesVisible(props.teamId, (visible) => {
          isVotesVisible.value = visible
        })
      } catch (error) {
        console.error("Error mounting ScrumPoker:", error)
      }
    })

    onUnmounted(() => {
      // Cleanup işlemleri
      if (updateTimeout) {
        clearTimeout(updateTimeout)
      }

      // Listener'ları temizle
      if (unsubscribeVotes) unsubscribeVotes()
      if (unsubscribeVisibility) unsubscribeVisibility()

      // Global cleanup
      cleanupListeners()

      // Firebase'den ayrıl
      leaveScrumPoker(props.teamId, auth.currentUser.email).catch(console.error)
    })

    return {
      maintenance,
      selectedPokerCardType,
      team,
      selectedPokerCardTypeName,
      votes: votesArray, // computed property döndür
      isVotesVisible,
      selectedPokerCardNumber,
      selectPokerCardType,
      selectPokerCard,
      newRound
    }
  }
}
</script>
