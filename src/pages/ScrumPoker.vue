<template>
  <div class="flex flex-row w-full h-screen bg-white">
    <SideBar :team-id="teamId"></SideBar>
    <div class="flex-1 flex flex-col items-center min-h-screen p-6">
      <div class="w-full max-w-7xl mx-auto">
        <div class="flex flex-col items-center space-y-8">
          <PokerTable :isVotesVisible="isVotesVisible" :votes="votes" :members="team.members" @newRound="newRound"></PokerTable>

          <!-- Cards Selection Area -->
          <div class="w-full bg-gradient-to-br from-gray-50 to-gray-100 border-2 border-gray-200 rounded-2xl p-8 shadow-lg">
            <div class="text-center mb-6">
              <h3 class="text-xl font-bold text-gray-800 mb-2">Select Your Estimate</h3>
              <p class="text-gray-600 text-sm">Choose a Fibonacci number that represents your estimate</p>
            </div>
            <div class="flex justify-center flex-wrap gap-4">
              <pokerCard
                v-for="pokerCard in fibonacciNumbers"
                :number="pokerCard"
                :key="pokerCard"
                @selectPokerCard="selectPokerCard"
                :selectable="!isVotesVisible"
                :newRound="newRound"
                :selectedCardNumber="selectedPokerCardNumber"
                class="transform hover:rotate-1 transition-transform duration-300"
              ></pokerCard>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import PokerTable from "../components/poker/pokerTable.vue";
import PokerCard from "../components/poker/pokerCard.vue";
import SideBar from "../components/SideBar.vue";
import {getTeamById} from "../firebase/TeamService.js";
import {
  joinScrumPoker, leaveScrumPoker, listenScrumPoker, listenVotesVisible, setVotesVisible,
  updateScrumPokerVote,
  cleanupListeners
} from "../firebase/ScrumPokerService.js";
import {auth} from "../firebase/Firebase.js";

export default {
  name: "ScrumPoker",
  components: {PokerCard, PokerTable, SideBar},
  props: {
    teamId: String
  },
  setup(props) {
    const maintenance = ref(false)
    const team = reactive({})
    const votes = ref(new Map()) // Map kullanarak O(1) erişim
    const isVotesVisible = ref(false)
    const selectedPokerCardNumber = ref(null)
    const fibonacciNumbers = ["1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?"]

    // Debounce için
    let updateTimeout = null
    let unsubscribeVotes = null
    let unsubscribeVisibility = null

    // Computed property for votes array (sadece gerektiğinde hesaplanır)
    const votesArray = computed(() => Array.from(votes.value.values()))

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
      team,
      votes: votesArray, // computed property döndür
      isVotesVisible,
      selectedPokerCardNumber,
        fibonacciNumbers,
      selectPokerCard,
      newRound
    }
  }
}
</script>
