<template>
  <div class="flex flex-row w-full pb-20 lg:pb-0">
    <SideBar :team-id="teamId"></SideBar>
    <div class="flex-1 min-w-0 p-4">
      <div class="flex flex-col items-center min-h-screen">
        <div class="w-full max-w-7xl mx-auto">
          <div class="flex flex-col items-center space-y-8">

            <!-- Linked Task Banner (Work modülü entegrasyonu) -->
            <div v-if="activeTask" class="w-full bg-white border-2 border-amber-200/70 rounded-2xl shadow-lg overflow-hidden">
              <div class="h-1 w-full bg-gradient-to-r from-amber-400 to-orange-500"></div>
              <div class="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4">
                <div class="flex items-center gap-3 min-w-0 flex-1">
                  <div class="flex-shrink-0 w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center text-xl shadow-sm">
                    🃏
                  </div>
                  <div class="min-w-0">
                    <div class="flex items-center gap-2 flex-wrap">
                      <span class="text-[10px] uppercase tracking-wider font-semibold text-amber-600">Puanlanan Görev</span>
                      <span class="text-xs font-mono text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">{{ activeTask.customId }}</span>
                      <span v-if="activeTask.issueType" class="text-xs text-gray-500 capitalize">{{ activeTask.issueType }}</span>
                      <span
                        v-if="activeTask.storyPoints"
                        class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200"
                      >
                        Mevcut: {{ activeTask.storyPoints }} SP
                      </span>
                    </div>
                    <h2 class="text-base sm:text-lg font-bold text-gray-900 truncate mt-0.5">{{ activeTask.title }}</h2>
                  </div>
                </div>
                <div class="flex items-center gap-2 flex-shrink-0">
                  <router-link
                    :to="`/task/${activeTask.customId}`"
                    class="inline-flex items-center px-3 py-1.5 rounded-lg border border-gray-200 text-xs font-medium text-gray-600 bg-white hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm"
                  >
                    Görevi Aç
                  </router-link>
                  <button
                    @click="detachTask"
                    class="inline-flex items-center px-3 py-1.5 rounded-lg border border-red-100 text-xs font-medium text-red-500 bg-red-50 hover:bg-red-100 hover:border-red-200 transition-all"
                    title="Görev bağını kaldır — puan işlenmez"
                  >
                    Bağı Kaldır
                  </button>
                </div>
              </div>
            </div>

            <PokerTable :isVotesVisible="isVotesVisible" :votes="votes" :members="team.members" @newRound="newRound"></PokerTable>

            <!-- Score Suggestion Panel — oylar açıldığında ve görev bağlıyken -->
            <div v-if="isVotesVisible && activeTask" class="w-full bg-white border-2 border-green-200/70 rounded-2xl shadow-lg overflow-hidden">
              <div class="h-1 w-full bg-gradient-to-r from-green-400 to-green-600"></div>
              <div class="p-5 sm:p-6">
                <div class="text-center mb-5">
                  <h3 class="text-lg font-bold text-gray-800">Puanı Göreve İşle</h3>
                  <p class="text-sm text-gray-500 mt-1">
                    <template v-if="average !== null">
                      Ortalama <span class="font-bold text-green-600">{{ average.toFixed(1) }}</span> —
                      tartışma sonrasında önerilen bir puanı seçin veya kendi değerinizi girin.
                    </template>
                    <template v-else>
                      Sayısal oy bulunmuyor — puanı elle girebilirsiniz.
                    </template>
                  </p>
                </div>

                <div class="flex flex-wrap items-center justify-center gap-3">
                  <!-- Önerilen Fibonacci değerleri (ortalamanın alt/üst komşusu) -->
                  <button
                    v-for="s in suggestions"
                    :key="'suggestion-' + s"
                    @click="selectSuggestion(s)"
                    class="w-16 h-20 rounded-xl border-2 font-black text-2xl transition-all duration-200 shadow-sm hover:shadow-md hover:scale-105 active:scale-95"
                    :class="selectedSuggestion === s
                      ? 'border-green-500 bg-gradient-to-br from-green-500 to-green-600 text-white shadow-green-500/30'
                      : 'border-green-200 bg-green-50 text-green-700 hover:border-green-400'"
                  >
                    {{ s }}
                  </button>

                  <!-- Custom giriş -->
                  <div class="flex flex-col items-center gap-1">
                    <input
                      v-model="customPoints"
                      @input="selectedSuggestion = null"
                      type="number"
                      min="0"
                      placeholder="Özel"
                      class="w-20 h-20 rounded-xl border-2 text-center font-black text-2xl transition-all focus:outline-none focus:ring-2 focus:ring-green-300"
                      :class="customPoints !== '' && selectedSuggestion === null
                        ? 'border-green-500 bg-green-50 text-green-700'
                        : 'border-gray-200 bg-white text-gray-700'"
                    />
                  </div>

                  <button
                    @click="applyChosenScore"
                    :disabled="chosenPoints === null || applying"
                    class="select-none rounded-full bg-gradient-to-r from-green-600 to-green-700 hover:from-green-700 hover:to-green-800 py-3 px-8 text-sm font-semibold text-white shadow-lg shadow-green-500/20 transition-all duration-300 hover:shadow-xl hover:scale-105 active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100 flex items-center gap-2"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/>
                    </svg>
                    {{ applying ? 'Kaydediliyor...' : (chosenPoints !== null ? `${chosenPoints} SP Kaydet ve Göreve Dön` : 'Kaydet ve Göreve Dön') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Cards Selection Area -->
            <div class="w-full bg-gradient-to-br from-gray-50 to-gray-100 border-2 border-gray-200 rounded-2xl p-4 sm:p-8 shadow-lg">
              <div class="text-center mb-6">
                <h3 class="text-xl font-bold text-gray-800 mb-2">
                  {{ isVotesVisible ? 'Votes Revealed' : 'Select Your Estimate' }}
                </h3>
                <p v-if="!isVotesVisible" class="text-gray-600 text-sm">
                  <template v-if="activeTask">
                    <span class="font-semibold">{{ activeTask.customId }}</span> için tahmininizi seçin
                  </template>
                  <template v-else>
                    Choose a Fibonacci number that represents your estimate
                  </template>
                </p>
                <div v-else class="flex flex-col items-center gap-3">
                  <p class="text-gray-600 text-sm">Voting is closed for this round</p>
                  <button
                    @click="newRound"
                    class="select-none rounded-full bg-gradient-to-r from-green-600 to-green-700 hover:from-green-700 hover:to-green-800 py-2 px-6 text-sm font-semibold text-white shadow-lg shadow-green-500/20 transition-all duration-300 hover:shadow-xl hover:scale-105 active:scale-95 flex items-center gap-2"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    Start New Round
                  </button>
                </div>
              </div>
              <div class="flex justify-center flex-wrap gap-4">
                <pokerCard
                  v-for="pokerCard in fibonacciNumbers"
                  :number="pokerCard"
                  :key="pokerCard"
                  @selectPokerCard="selectPokerCard"
                  :disabled="isVotesVisible"
                  :selectedCardNumber="selectedPokerCardNumber"
                  class="transform transition-transform duration-300"
                  :class="isVotesVisible ? '' : 'hover:rotate-1'"
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
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
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
    const router = useRouter()
    const maintenance = ref(false)
    const team = reactive({})
    const votes = ref(new Map())
    const isVotesVisible = ref(false)
    const selectedPokerCardNumber = ref(null)
    const fibonacciNumbers = ["1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?"]

    // Work modülü entegrasyonu — oturuma bağlı görev + puan seçim durumu
    const activeTask = ref(null)
    const selectedSuggestion = ref(null)
    const customPoints = ref('')
    const applying = ref(false)

    const votesTopicKey = `/topic/poker/${props.teamId}/votes`
    const visibilityTopicKey = `/topic/poker/${props.teamId}/visibility`
    const taskTopicKey = `/topic/poker/${props.teamId}/task`

    let updateTimeout = null

    const votesArray = computed(() => Array.from(votes.value.values()))

    const average = computed(() => {
      const valid = votesArray.value.filter(v => !isNaN(v.vote) && v.vote !== "-" && v.vote !== '?')
      if (valid.length === 0) return null
      return valid.reduce((acc, cur) => acc + parseFloat(cur.vote), 0) / valid.length
    })

    // Ortalamanın alt ve üst Fibonacci komşuları (örn. 9.2 → [8, 13]; tam eşleşmede tek öneri)
    const suggestions = computed(() => {
      if (average.value === null) return []
      const deck = fibonacciNumbers.filter(n => !isNaN(n)).map(Number)
      const floor = [...deck].reverse().find(n => n <= average.value)
      const ceil = deck.find(n => n >= average.value)
      return [...new Set([floor, ceil].filter(n => n !== undefined))]
    })

    const chosenPoints = computed(() => {
      if (selectedSuggestion.value !== null) return selectedSuggestion.value
      const custom = parseInt(customPoints.value, 10)
      return !isNaN(custom) && custom >= 0 ? custom : null
    })

    const resetScoreSelection = () => {
      selectedSuggestion.value = null
      customPoints.value = ''
    }

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

    const selectSuggestion = (points) => {
      selectedSuggestion.value = points
      customPoints.value = ''
    }

    // Seçilen puanı göreve işler ve görev sayfasına geri döner.
    // Backend görev bağını kaldırıp yeni tur başlatır; diğer üyeler WS ile güncellenir.
    const applyChosenScore = async () => {
      if (chosenPoints.value === null || applying.value) return
      applying.value = true
      try {
        const task = await ScrumPokerApi.applyScore(props.teamId, chosenPoints.value)
        router.push({ name: 'TaskDetail', params: { taskId: task.customId } })
      } catch (error) {
        console.error("Puan göreve işlenemedi:", error)
        alert('Puan göreve işlenemedi. Lütfen tekrar deneyin.')
      } finally {
        applying.value = false
      }
    }

    // Görev bağını kaldırır — puan işlenmez, masa bağımsız moda döner
    const detachTask = async () => {
      try {
        await ScrumPokerApi.clearPokerTask(props.teamId)
      } catch (error) {
        console.error("Görev bağı kaldırılamadı:", error)
      }
    }

    // Yeni tur başladığında (görünürlük kapanınca) puan seçimini sıfırla
    watch(isVotesVisible, (visible) => {
      if (!visible) {
        resetScoreSelection()
        selectedPokerCardNumber.value = null
      }
    })

    onMounted(async () => {
      try {
        // Takım bilgisini çek
        const teamData = await getTeamById(props.teamId)
        Object.assign(team, teamData)

        // Oturuma katıl + mevcut oyları ve varsa bağlı görevi al
        const session = await ScrumPokerApi.joinPoker(props.teamId)
        isVotesVisible.value = session.votesVisible
        applyVotes(session.votes)
        activeTask.value = session.task || null

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

          // Task topic — Data-Carrying: { task: PokerTaskInfo | null }
          subscribe(taskTopicKey, (data) => {
            activeTask.value = data.task || null
            resetScoreSelection()
            // Yeni görev bağlandıysa backend turu sıfırladı — kart seçimi de temizlenmeli.
            // (task=null durumunda tur korunur; mevcut seçim geçerli kalır)
            if (data.task) selectedPokerCardNumber.value = null
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
      unsubscribe(taskTopicKey)

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
      newRound: handleNewRound,
      // Work modülü entegrasyonu
      activeTask,
      average,
      suggestions,
      selectedSuggestion,
      customPoints,
      chosenPoints,
      applying,
      selectSuggestion,
      applyChosenScore,
      detachTask
    }
  }
}
</script>
