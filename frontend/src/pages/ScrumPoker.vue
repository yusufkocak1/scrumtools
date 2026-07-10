<template>
  <div class="flex flex-row w-full pb-20 lg:pb-0">
    <SideBar :team-id="teamId"></SideBar>
    <div class="flex-1 min-w-0 p-4">
      <div class="flex flex-col items-center min-h-screen">
        <div class="w-full max-w-7xl mx-auto">
          <div class="flex flex-col items-center space-y-8">
            <PokerTable :isVotesVisible="isVotesVisible" :votes="votes" :members="team.members" @newRound="newRound"></PokerTable>

            <!-- Cards Selection Area -->
            <div class="w-full bg-gradient-to-br from-gray-50 to-gray-100 border-2 border-gray-200 rounded-2xl p-4 sm:p-8 shadow-lg">
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
  </div>
</template>
<script>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import PokerTable from "../components/poker/pokerTable.vue";
import PokerCard from "../components/poker/pokerCard.vue";
import SideBar from "../components/SideBar.vue";
import { getTeamById } from "../api/TeamApi.js";
import * as ScrumPokerApi from "../api/ScrumPokerApi.js";
import { connect, subscribe, unsubscribe } from "../api/websocket.js";

export default {
  name: "ScrumPoker",
  components: {PokerCard, PokerTable, SideBar},
  props: {
    teamId: String
  },
  setup(props) {
    const maintenance = ref(false)
    const team = reactive({})
    const votes = ref(new Map())
    const isVotesVisible = ref(false)
    const selectedPokerCardNumber = ref(null)
    const fibonacciNumbers = ["1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?"]

    const votesTopicKey = `/topic/poker/${props.teamId}/votes`
    const visibilityTopicKey = `/topic/poker/${props.teamId}/visibility`

    let updateTimeout = null

    const votesArray = computed(() => Array.from(votes.value.values()))

    const applyVotes = (newVotes) => {
      const voteMap = new Map()
      newVotes.forEach(v => voteMap.set(v.email, v))
      votes.value = voteMap
    }

    const selectPokerCard = (pokerCard) => {
      if (selectedPokerCardNumber.value === pokerCard) {
        pokerCard = "-"
      }

      if (updateTimeout) clearTimeout(updateTimeout)

      updateTimeout = setTimeout(() => {
        ScrumPokerApi.vote(props.teamId, pokerCard).catch(console.error)
      }, 300)

      selectedPokerCardNumber.value = pokerCard
    }

    const handleNewRound = async () => {
      if (isVotesVisible.value) {
        // Yeni tur: backend tüm oyları sıfırlar + visibility=false yapar + broadcast eder
        await ScrumPokerApi.newRound(props.teamId)
        selectedPokerCardNumber.value = null
      } else {
        // Oyları göster
        await ScrumPokerApi.setVotesVisible(props.teamId, true)
      }
    }

    onMounted(async () => {
      try {
        // Takım bilgisini çek
        const teamData = await getTeamById(props.teamId)
        Object.assign(team, teamData)

        // Oturuma katıl + mevcut oyları al
        const session = await ScrumPokerApi.joinPoker(props.teamId)
        isVotesVisible.value = session.votesVisible
        applyVotes(session.votes)

        // WebSocket bağlantısını kur ve topic'lere subscribe ol
        connect(() => {
          // Votes topic — Data-Carrying: gelen mesaj doğrudan oy listesi
          subscribe(votesTopicKey, (data) => {
            applyVotes(data)
          })

          // Visibility topic — Data-Carrying: { votesVisible: bool }
          subscribe(visibilityTopicKey, (data) => {
            isVotesVisible.value = data.votesVisible
          })
        })
      } catch (error) {
        console.error("Error mounting ScrumPoker:", error)
      }
    })

    onUnmounted(() => {
      if (updateTimeout) clearTimeout(updateTimeout)

      // WebSocket subscription'larını temizle
      unsubscribe(votesTopicKey)
      unsubscribe(visibilityTopicKey)

      // Oturumdan ayrıl
      ScrumPokerApi.leavePoker(props.teamId).catch(console.error)
    })

    return {
      maintenance,
      team,
      votes: votesArray,
      isVotesVisible,
      selectedPokerCardNumber,
      fibonacciNumbers,
      selectPokerCard,
      newRound: handleNewRound
    }
  }
}
</script>
