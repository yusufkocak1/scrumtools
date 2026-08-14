<template>
  <div class="flex flex-row w-full bg-gray-50 min-h-screen">
    <div class="flex-1 min-w-0 p-4 sm:p-6">
      <div class="w-full max-w-6xl mx-auto flex flex-col gap-4 sm:gap-5">

        <!-- Sayfa başlığı — takım merkezi context'ten gelir (Ayarlar > Çalışma Alanı) -->
        <div class="bg-white rounded-2xl border border-gray-200 shadow-sm p-4 sm:p-5">
          <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-3 sm:gap-4">
            <div class="flex items-center gap-3 min-w-0">
              <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-700 flex items-center justify-center flex-shrink-0 shadow-sm text-xl sm:text-2xl">
                🃏
              </div>
              <div class="min-w-0">
                <h1 class="font-bold text-lg sm:text-2xl text-gray-900 truncate">Scrum Poker</h1>
                <p class="text-xs sm:text-sm text-gray-500 flex items-center gap-1.5 flex-wrap">
                  Planlama Pokeri
                  <span v-if="memberCount" class="inline-flex items-center gap-1 text-gray-400">
                    <span class="w-1 h-1 rounded-full bg-gray-300"></span>
                    {{ memberCount }} oyuncu
                  </span>
                </p>
              </div>
            </div>

            <div class="flex flex-wrap items-center gap-2">
              <!-- Davet linki — masaya oyuncu çağırmanın en hızlı yolu -->
              <button
                @click="copyInviteLink"
                class="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-gray-200 bg-white text-xs sm:text-sm font-medium text-gray-700 hover:bg-emerald-50 hover:border-emerald-300 hover:text-emerald-700 transition-colors"
                title="Masa linkini kopyala"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 010 5.656l-3 3a4 4 0 01-5.656-5.656l1.5-1.5M10.172 13.828a4 4 0 010-5.656l3-3a4 4 0 015.656 5.656l-1.5 1.5" />
                </svg>
                {{ linkCopied ? 'Kopyalandı' : 'Davet Et' }}
              </button>

              <router-link
                to="/settings"
                class="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-gray-200 bg-white text-xs sm:text-sm text-gray-700 hover:border-indigo-300 hover:text-indigo-700 transition-colors"
                title="Aktif takımı Ayarlar'dan değiştir"
              >
                <span class="w-2 h-2 rounded-full bg-green-500 flex-shrink-0"></span>
                <span class="truncate max-w-[10rem]">{{ team.teamName || 'Takım' }}</span>
                <span class="text-xs text-gray-400">Değiştir</span>
              </router-link>
            </div>
          </div>
        </div>

        <!-- Linked Task Banner (Work modülü entegrasyonu) -->
        <div v-if="activeTask" class="w-full bg-white border border-amber-200 rounded-2xl shadow-sm overflow-hidden">
          <div class="h-1 w-full bg-gradient-to-r from-amber-400 to-orange-500"></div>
          <div class="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4">
            <div class="flex items-center gap-3 min-w-0 flex-1">
              <div class="flex-shrink-0 w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center text-xl shadow-sm">
                🎯
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

        <!-- Masa: oyuncular, tur durumu ve turun TEK birincil aksiyonu burada -->
        <PokerTable
          :isVotesVisible="isVotesVisible"
          :votes="votes"
          :members="team.members"
          :currentUserEmail="userEmail"
          :incomingThrow="incomingThrow"
          :deck="deck"
          @newRound="newRound"
          @throw="throwAtPlayer"
        />

        <!-- Score Suggestion Panel — oylar açıldığında ve görev bağlıyken -->
        <div v-if="isVotesVisible && activeTask" class="w-full bg-white border border-green-200 rounded-2xl shadow-sm overflow-hidden">
          <div class="h-1 w-full bg-gradient-to-r from-green-400 to-green-600"></div>
          <div class="p-5 sm:p-6">
            <div class="text-center mb-5">
              <h3 class="text-lg font-bold text-gray-800">Puanı Göreve İşle</h3>
              <p class="text-sm text-gray-500 mt-1">
                <template v-if="average !== null">
                  <template v-if="deck.numeric">Ortalama</template>
                  <template v-else>Bedenlerin puan karşılığı ortalaması</template>
                  <span class="font-bold text-green-600">{{ average.toFixed(1) }}</span> —
                  tartışma sonrasında önerilen bir puanı seçin veya kendi değerinizi girin.
                </template>
                <template v-else>
                  Puanlanabilir oy bulunmuyor — puanı elle girebilirsiniz.
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
                  aria-label="Özel puan"
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

        <!-- Deste: kullanıcının kendi eli -->
        <div class="w-full bg-white border border-gray-200 rounded-2xl shadow-sm p-4 sm:p-6">
          <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 mb-5">
            <div class="min-w-0">
              <h3 class="text-base sm:text-lg font-bold text-gray-800">
                {{ isVotesVisible ? 'Deste kapandı' : 'Kartın' }}
              </h3>
              <p class="text-xs sm:text-sm text-gray-500 mt-0.5">
                <template v-if="isVotesVisible">
                  Bu tur tamamlandı — tekrar oylamak için masadan yeni tur başlat.
                </template>
                <template v-else-if="activeTask">
                  <span class="font-semibold text-gray-700">{{ activeTask.customId }}</span> için tahminini seç
                </template>
                <template v-else>
                  Efor tahminini en iyi anlatan kartı seç — istediğin zaman değiştirebilirsin
                </template>
              </p>
            </div>

            <div class="flex flex-wrap items-center gap-2 flex-shrink-0">
              <!-- Seçili kart özeti -->
              <div
                v-if="selectedPokerCardNumber && selectedPokerCardNumber !== '-'"
                class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-semibold"
              >
                <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                Seçimin: <span class="font-black text-sm">{{ selectedPokerCardNumber }}</span>
              </div>

              <!-- Deste seçimi — herkes için değişir, mevcut tur sıfırlanır -->
              <label class="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl border border-gray-200 bg-white text-xs text-gray-600 focus-within:border-emerald-300">
                <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v8a2 2 0 01-2 2H6a2 2 0 01-2-2V6z" />
                </svg>
                <span class="sr-only">Kart destesi</span>
                <select
                  :value="cardType"
                  @change="changeDeck($event)"
                  :disabled="changingDeck"
                  class="bg-transparent font-semibold text-gray-700 focus:outline-none cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <option v-for="d in decks" :key="d.id" :value="d.id">{{ d.label }}</option>
                </select>
              </label>
            </div>
          </div>

          <div class="flex justify-center flex-wrap gap-2.5 sm:gap-3 pt-3">
            <pokerCard
              v-for="card in deck.cards"
              :number="card"
              :key="card"
              @selectPokerCard="selectPokerCard"
              :disabled="isVotesVisible"
              :selectedCardNumber="selectedPokerCardNumber"
            ></pokerCard>
          </div>

          <p v-if="!isVotesVisible" class="text-center text-[11px] text-gray-400 mt-5">
            {{ deck.label }} destesi ({{ deck.hint }}) · Seçili karta tekrar dokunursan oyunu geri çekersin.
          </p>
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
import { getTeamById } from "../api/TeamApi.js";
import * as ScrumPokerApi from "../api/ScrumPokerApi.js";
import { connect, subscribe, unsubscribe } from "../api/websocket.js";
import { useTeamContext } from "../composables/useTeamContext.js";
import { useAuth } from "../composables/useAuth.js";
import { createToast } from "mosha-vue-toastify";
import { DECKS, DEFAULT_DECK_ID, cardPoints, getDeck } from "../components/poker/decks.js";

export default {
  name: "ScrumPoker",
  components: {PokerCard, PokerTable},
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
    const linkCopied = ref(false)

    // Aktif kart destesi — sunucuda tutulur, takımdaki herkes aynı desteyi görür
    const cardType = ref(DEFAULT_DECK_ID)
    const changingDeck = ref(false)
    const deck = computed(() => getDeck(cardType.value))

    // Masadaki "Sen" işaretlemesi için oturum sahibinin e-postası
    const { userEmail } = useAuth()

    // Work modülü entegrasyonu — oturuma bağlı görev + puan seçim durumu
    const activeTask = ref(null)
    const selectedSuggestion = ref(null)
    const customPoints = ref('')
    const applying = ref(false)

    // URL'deki takım merkezi context'e adopte edilir: paylaşılan poker linki
    // açıldığında sonraki gezinmeler (Board, Retro...) aynı takımda devam eder.
    const { adoptTeam } = useTeamContext()

    // Masada oyuncular arası fırlatma — kalıcı değil, yalnızca animasyon
    const incomingThrow = ref(null)
    let lastThrowSentAt = 0
    const THROW_COOLDOWN_MS = 600

    const topicsFor = (teamId) => [
      `/topic/poker/${teamId}/votes`,
      `/topic/poker/${teamId}/visibility`,
      `/topic/poker/${teamId}/task`,
      `/topic/poker/${teamId}/throws`,
      `/topic/poker/${teamId}/card-type`,
    ]

    let updateTimeout = null
    let copyTimeout = null

    const votesArray = computed(() => Array.from(votes.value.values()))

    const memberCount = computed(() => votesArray.value.length)

    // Turda en az bir oy var mı? Deste değişimi uyarısı bunu kullanır.
    const hasVotesInRound = computed(() => votesArray.value.some(v => v.vote && v.vote !== '-'))

    // Oyların story point karşılığı — beden destesinde points eşlemesi devreye girer (M → 3)
    const pointValues = computed(() =>
      votesArray.value
        .map(v => cardPoints(deck.value, v.vote))
        .filter(n => n !== null)
    )

    const average = computed(() => {
      if (!pointValues.value.length) return null
      return pointValues.value.reduce((acc, cur) => acc + cur, 0) / pointValues.value.length
    })

    // Ortalamanın alt ve üst komşusu, aktif destenin puan skalasından
    // (Fibonacci'de 9.2 → [8, 13]; bedende 4.0 → [3, 5]). Tam eşleşmede tek öneri.
    const suggestions = computed(() => {
      if (average.value === null) return []
      const scale = [...new Set(
        deck.value.cards.map(card => cardPoints(deck.value, card)).filter(n => n !== null)
      )].sort((a, b) => a - b)

      const floor = [...scale].reverse().find(n => n <= average.value)
      const ceil = scale.find(n => n >= average.value)
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

    // Masanın tek birincil aksiyonu: kartlar kapalıysa açar, açıksa yeni tur başlatır
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

    // Kart destesini değiştirir — sunucu turu sıfırlar ve herkese WS ile yayınlar.
    // Başarısızlıkta <select> DOM'da yeni değerde kalacağı için elle geri alınır.
    const changeDeck = async (event) => {
      const nextType = event.target.value
      if (!nextType || nextType === cardType.value || changingDeck.value) return

      // Turda oy varsa uyar — deste değişimi herkesin oyunu siler
      if (hasVotesInRound.value &&
          !window.confirm('Deste değişince bu turun oyları sıfırlanır. Devam edilsin mi?')) {
        event.target.value = cardType.value
        return
      }

      changingDeck.value = true
      try {
        await ScrumPokerApi.updateCardType(props.teamId, nextType)
        cardType.value = nextType
      } catch (error) {
        console.error("Kart destesi değiştirilemedi:", error)
        event.target.value = cardType.value
        createToast("Kart destesi değiştirilemedi.", { type: "error", position: "top-center" })
      } finally {
        changingDeck.value = false
      }
    }

    // Deste değişince (kendi isteğimizle ya da başka biri değiştirdiğinde) sunucu turu
    // sıfırlar; eski destenin kartı elimizde kalmasın.
    watch(cardType, () => {
      selectedPokerCardNumber.value = null
      resetScoreSelection()
    })

    // Masadaki başka bir oyuncuya obje fırlatır.
    // Sunucu hiçbir şey kaydetmez; herkese WS ile yayınlar ve masada animasyona dönüşür.
    const throwAtPlayer = ({ toEmail, item }) => {
      const now = Date.now()
      // İstemci tarafı cooldown — sunucu da ayrıca sınırlıyor
      if (now - lastThrowSentAt < THROW_COOLDOWN_MS) return
      lastThrowSentAt = now
      ScrumPokerApi.throwItem(props.teamId, toEmail, item).catch(console.error)
    }

    // Masa linkini panoya kopyalar — takım arkadaşlarını davet etmenin kısa yolu
    const copyInviteLink = async () => {
      try {
        await navigator.clipboard.writeText(window.location.href)
        linkCopied.value = true
        createToast("Masa linki kopyalandı — takımına gönderebilirsin.", { type: "success", position: "top-center" })
        if (copyTimeout) clearTimeout(copyTimeout)
        copyTimeout = setTimeout(() => { linkCopied.value = false }, 2000)
      } catch (error) {
        console.error("Link kopyalanamadı:", error)
        createToast("Link kopyalanamadı — adres çubuğundan kopyalayabilirsin.", { type: "warning", position: "top-center" })
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
        const target = router.resolve({ name: 'TaskDetail', params: { taskId: task.customId } })
        // Masaya bu görevin sayfasından gelindiyse yeni kayıt eklemek yerine geçmişte
        // geri dönüyoruz: aksi halde poker kaydı yığında kalıp görevdeki "Geri"
        // tuşunu puanlaması bitmiş masaya çeviriyor.
        if (router.options.history.state.back === target.fullPath) {
          router.go(-1)
        } else {
          router.push(target)
        }
      } catch (error) {
        console.error("Puan göreve işlenemedi:", error)
        createToast("Puan göreve işlenemedi. Lütfen tekrar deneyin.", { type: "error", position: "top-center" })
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

    // Takım oturumunu kurar: takım bilgisi + poker session + WS subscription'ları
    const joinTeam = async (teamId) => {
      try {
        // Takım bilgisini çek (eski takımın alanları kalmasın diye önce temizle)
        const teamData = await getTeamById(teamId)
        Object.keys(team).forEach(key => delete team[key])
        Object.assign(team, teamData)

        // Oturuma katıl + mevcut oyları ve varsa bağlı görevi al
        const session = await ScrumPokerApi.joinPoker(teamId)
        isVotesVisible.value = session.votesVisible
        applyVotes(session.votes)
        activeTask.value = session.task || null
        cardType.value = session.cardType || DEFAULT_DECK_ID

        // WebSocket bağlantısını kur ve topic'lere subscribe ol
        connect(() => {
          const [votesTopicKey, visibilityTopicKey, taskTopicKey, throwsTopicKey, cardTypeTopicKey] = topicsFor(teamId)

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

          // Throws topic — Data-Carrying: { fromEmail, fromName, toEmail, item, timestamp }
          // Her mesaj yeni bir nesne; referans değişimi masadaki animasyonu tetikler.
          subscribe(throwsTopicKey, (data) => {
            incomingThrow.value = data
          })

          // Card type topic — Data-Carrying: { cardType: "fibonacci" | "tshirt" | ... }
          // Deste değişimini backend zaten yeni turla birlikte yayınlar.
          subscribe(cardTypeTopicKey, (data) => {
            cardType.value = data.cardType || DEFAULT_DECK_ID
          })
        })
      } catch (error) {
        console.error("Error joining poker session:", error)
      }
    }

    // Takım oturumunu kapatır: WS subscription'ları + poker session
    const leaveTeam = (teamId) => {
      topicsFor(teamId).forEach(unsubscribe)
      ScrumPokerApi.leavePoker(teamId).catch(console.error)
    }

    const resetRoundState = () => {
      votes.value = new Map()
      isVotesVisible.value = false
      selectedPokerCardNumber.value = null
      activeTask.value = null
      incomingThrow.value = null
      cardType.value = DEFAULT_DECK_ID
      resetScoreSelection()
    }

    // Route param değişince (takım değişimi) eski oturumu kapat, yenisini kur
    watch(() => props.teamId, (newTeamId, oldTeamId) => {
      if (oldTeamId) leaveTeam(oldTeamId)
      resetRoundState()
      if (newTeamId) {
        adoptTeam(newTeamId)
        joinTeam(newTeamId)
      }
    })

    onMounted(() => {
      adoptTeam(props.teamId)
      joinTeam(props.teamId)
    })

    onUnmounted(() => {
      if (updateTimeout) clearTimeout(updateTimeout)
      if (copyTimeout) clearTimeout(copyTimeout)
      leaveTeam(props.teamId)
    })

    return {
      maintenance,
      team,
      votes: votesArray,
      memberCount,
      isVotesVisible,
      selectedPokerCardNumber,
      selectPokerCard,
      newRound: handleNewRound,
      decks: DECKS,
      deck,
      cardType,
      changingDeck,
      changeDeck,
      userEmail,
      linkCopied,
      copyInviteLink,
      incomingThrow,
      throwAtPlayer,
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
