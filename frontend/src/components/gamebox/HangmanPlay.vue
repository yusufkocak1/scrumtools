<template>
  <div class="max-w-6xl mx-auto">
    <!-- ─── Tur arası: kelime ilanı ───
         Tur bittiğinde oyun burada durur — kelime büyük harflerle ilan edilir ve
         sonraki kelimeye ancak moderatör geçirir. Sunucu da bu sürede tahmin kabul etmez. -->
    <transition name="fade">
      <div v-if="intermission"
           class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/70 backdrop-blur-sm">
        <div class="w-full max-w-2xl bg-white rounded-3xl shadow-2xl overflow-hidden max-h-full overflow-y-auto">
          <div :class="['px-6 py-6 text-center',
                        intermission.solved
                          ? 'bg-gradient-to-r from-green-500 to-emerald-600'
                          : 'bg-gradient-to-r from-rose-500 to-red-600']">
            <p class="text-5xl mb-2">{{ intermission.solved ? '🎉' : '💀' }}</p>
            <p class="text-white text-xl sm:text-2xl font-bold">
              {{ intermission.solved ? `${intermission.solvedByName} bildi!` : 'Kimse bilemedi!' }}
            </p>
            <p class="text-white/80 text-xs mt-1">
              Kelime {{ intermission.roundOrder + 1 }} / {{ session.totalRounds }}
            </p>
          </div>

          <div class="p-6 sm:p-8 text-center">
            <p class="text-xs font-semibold uppercase tracking-widest text-gray-400 mb-3">Kelime</p>
            <div class="flex flex-wrap justify-center gap-1.5 sm:gap-2">
              <span v-for="(ch, i) in revealedLetters" :key="i"
                    class="w-10 h-12 sm:w-12 sm:h-14 flex items-center justify-center rounded-xl
                           bg-indigo-50 border-2 border-indigo-200 text-indigo-700
                           font-mono font-bold text-2xl sm:text-3xl">
                {{ ch }}
              </span>
            </div>
            <p v-if="intermissionCategory" class="mt-4 text-sm text-gray-500">
              🏷️ {{ intermissionCategory }}
            </p>
            <p v-if="intermission.solved && intermission.score > 0" class="mt-3 text-sm text-gray-600">
              <strong class="text-green-600">+{{ intermission.score }}</strong> puan
            </p>
          </div>

          <div class="border-t border-gray-200 bg-gray-50 px-6 py-5 text-center">
            <button v-if="isHost" @click="$emit('next-round')"
                    class="w-full sm:w-auto px-8 py-3.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700
                           transition-colors font-semibold text-lg shadow-lg shadow-indigo-600/20">
              {{ isLastRound ? '🏁 Sonuçları Göster' : '⏭️ Sonraki Kelime' }}
            </button>
            <p v-else class="text-sm text-gray-500 flex items-center justify-center gap-2">
              <span class="w-2 h-2 rounded-full bg-indigo-500 animate-pulse"></span>
              {{ isLastRound
                  ? 'Moderatör sonuçları açıyor…'
                  : 'Moderatörün sonraki kelimeye geçmesi bekleniyor…' }}
            </p>
            <button v-if="isHost && !isLastRound" @click="$emit('finish')"
                    class="mt-3 text-xs text-gray-400 hover:text-red-600 transition-colors">
              ⏹️ Oyunu burada bitir
            </button>
          </div>
        </div>
      </div>
    </transition>

    <div class="grid lg:grid-cols-[1fr_320px] gap-6 items-start">
      <!-- ─── Oyun alanı ─── -->
      <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
        <!-- Sıra göstergesi -->
        <div :class="['relative p-4 sm:p-5 text-center transition-colors',
                      intermission
                        ? 'bg-gradient-to-r from-slate-600 to-slate-700'
                        : isMyTurn
                          ? 'bg-gradient-to-r from-green-500 to-emerald-600'
                          : 'bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500']">
          <!-- Sıra sesi — ofiste oynayan susturabilsin diye, tercih tarayıcıda kalıcı. -->
          <button v-if="!isSpectator" @click="toggleSound"
                  :title="soundEnabled ? 'Sıra sesini kapat' : 'Sıra sesini aç'"
                  class="absolute top-3 right-3 w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 transition-colors text-sm leading-none">
            {{ soundEnabled ? '🔔' : '🔕' }}
          </button>

          <p v-if="intermission" class="text-white font-bold text-lg">⏸️ Tur bitti</p>
          <p v-else-if="isMyTurn" class="text-white font-bold text-lg">🎯 Sıra sende!</p>
          <p v-else-if="isSpectator" class="text-white font-medium">
            👁️ İzliyorsun — sıra <strong>{{ session.currentTurnName }}</strong>'de
          </p>
          <p v-else class="text-white font-medium">
            ⏳ Sıra <strong>{{ session.currentTurnName }}</strong>'de
          </p>
          <p class="text-white/80 text-xs mt-1">
            Kelime {{ session.currentRoundIndex + 1 }} / {{ session.totalRounds }}
          </p>

          <!-- Bekleyen oyuncu ne kadar bekleyeceğini bilsin; belirsizlik sekme değiştirtiyor. -->
          <p v-if="!intermission && turnsUntilMine === 1"
             class="mt-2 inline-flex items-center gap-1.5 px-3 py-1 bg-yellow-300 text-yellow-900 rounded-full text-xs font-bold animate-pulse">
            👉 Sıradaki sensin — hazırlan!
          </p>
          <p v-else-if="!intermission && turnsUntilMine > 1" class="mt-2 text-white/75 text-xs">
            ⏳ {{ turnsUntilMine }} kişi sonra sıra sende
          </p>

          <!-- Sıra sayacı — sunucudan gelen kalan süreyle senkron, arada yerelde işler.
               Tur arasında sıra kimsede değildir: sayaç durur ve gizlenir. -->
          <div v-if="!intermission" class="mt-3 max-w-sm mx-auto">
            <div class="flex items-center justify-between gap-2 text-[11px] text-white/85 mb-1">
              <span class="font-semibold">⏱️ {{ secondsLeft }} sn</span>
              <span v-if="isSpectator"></span>
              <span v-else-if="isMyTurn && wordOpensIn > 0">
                🔒 Kelime tahmini {{ wordOpensIn }} sn daha sadece sende
              </span>
              <span v-else-if="isMyTurn">🔓 Kelime tahminine herkes girebilir</span>
              <span v-else-if="wordOpensIn > 0">
                🔒 Kelimeyi {{ wordOpensIn }} sn sonra sen de deneyebilirsin
              </span>
              <span v-else>🔓 Kelime tahmini açıldı — sen de deneyebilirsin</span>
            </div>
            <div class="h-1.5 bg-white/25 rounded-full overflow-hidden">
              <div :class="['h-full transition-all duration-1000 ease-linear',
                            secondsLeft <= 5 ? 'bg-red-300' : 'bg-white']"
                   :style="{ width: turnProgress + '%' }"></div>
            </div>
          </div>
          <!-- Kategori ipucu — moderatör açtıysa (sabit kategorili oyunda baştan açık). -->
          <p v-if="categoryText"
             class="mt-2 inline-flex items-center gap-1.5 px-3 py-1 bg-white/20 rounded-full backdrop-blur-sm text-white text-xs font-medium">
            🏷️ Kategori: {{ categoryText }}
          </p>
        </div>

        <div class="p-6 sm:p-8">
          <div class="grid sm:grid-cols-[auto_1fr] gap-6 items-start">
            <!-- Darağacı -->
            <div class="flex flex-col items-center">
              <svg viewBox="0 0 200 220" class="w-40 h-44 text-gray-700">
                <line x1="10" y1="210" x2="130" y2="210" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="40" y1="210" x2="40" y2="20" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="40" y1="20" x2="120" y2="20" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="120" y1="20" x2="120" y2="45" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>

                <circle v-if="wrongCount >= 1" cx="120" cy="60" r="15" stroke="#ef4444" stroke-width="4" fill="none"/>
                <line v-if="wrongCount >= 2" x1="120" y1="75" x2="120" y2="130" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="wrongCount >= 3" x1="120" y1="90" x2="100" y2="110" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="wrongCount >= 4" x1="120" y1="90" x2="140" y2="110" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="wrongCount >= 5" x1="120" y1="130" x2="100" y2="160" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="wrongCount >= 6" x1="120" y1="130" x2="140" y2="160" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
              </svg>
              <p class="text-sm font-medium" :class="remaining <= 2 ? 'text-red-600' : 'text-gray-500'">
                Kalan hak: {{ remaining }} / {{ maxWrong }}
              </p>
            </div>

            <!-- Kelime + klavye -->
            <div>
              <div class="text-center mb-6">
                <p class="text-3xl sm:text-4xl font-mono font-bold tracking-widest text-gray-900 break-all">
                  {{ displayWord }}
                </p>
                <p v-if="wrongLetters.length" class="mt-3 text-sm text-gray-500">
                  Yanlış harfler:
                  <span class="text-red-500 font-semibold">{{ wrongLetters.join(' ') }}</span>
                </p>
              </div>

              <div class="flex flex-wrap justify-center gap-1.5 mb-6">
                <button
                    v-for="letter in alphabet"
                    :key="letter"
                    :disabled="!canGuessLetter || isGuessed(letter) || busy"
                    @click="submitLetter(letter)"
                    :class="['w-9 h-9 sm:w-10 sm:h-10 rounded-lg text-sm font-semibold transition-colors border',
                             letterClass(letter)]">
                  {{ letter }}
                </button>
              </div>

              <!-- Kelime tahmini -->
              <div class="border-t border-gray-200 pt-5">
                <p class="text-sm font-semibold text-gray-700 mb-2">
                  💡 Kelimeyi biliyor musun?
                </p>
                <p class="text-xs text-gray-500 mb-3">
                  Doğru bilirsen <strong class="text-indigo-600">kalan tüm harflerin</strong> puanını
                  birden alırsın. Sıranın ilk {{ wordLockSeconds }} saniyesi sırası gelene aittir;
                  sonrasında herkes birer hak ile yarışa girer.
                </p>
                <div class="flex gap-2">
                  <input
                      v-model="wordGuess"
                      :disabled="!canGuessWord || busy"
                      @keyup.enter="submitWord"
                      type="text"
                      :placeholder="wordInputPlaceholder"
                      class="flex-1 px-4 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none disabled:bg-gray-50 disabled:text-gray-400" />
                  <button
                      @click="submitWord"
                      :disabled="!canGuessWord || busy || !wordGuess.trim()"
                      class="px-5 py-2.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap">
                    Tahmin Et
                  </button>
                </div>
                <p v-if="!isMyTurn && !isSpectator && canGuessWord" class="mt-2 text-xs text-amber-600">
                  ⚡ Sıra sende değil — bu sırada <strong>tek</strong> kelime tahmini hakkın var.
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Moderatör kontrolleri -->
        <div v-if="isHost" class="border-t border-gray-200 bg-gray-50 px-6 py-4 flex flex-wrap gap-3 justify-between items-center">
          <p class="text-xs text-gray-500">Moderatör kontrolleri</p>
          <div class="flex gap-2">
            <button v-if="canRevealCategory && !intermission" @click="$emit('reveal-category')"
                    title="Kelimenin kategorisini tüm oyunculara gösterir — geri alınamaz"
                    class="px-4 py-2 bg-white border border-indigo-300 text-indigo-700 rounded-lg hover:bg-indigo-50 transition-colors text-sm font-medium">
              🏷️ Kategoriyi Göster
            </button>
            <button v-if="!intermission" @click="$emit('skip')"
                    class="px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors text-sm font-medium">
              ⏭️ Sırayı Devret
            </button>
            <button @click="$emit('finish')"
                    class="px-4 py-2 bg-red-50 border border-red-200 text-red-700 rounded-lg hover:bg-red-100 transition-colors text-sm font-medium">
              ⏹️ Oyunu Bitir
            </button>
          </div>
        </div>
      </div>

      <!-- ─── Yan panel ─── -->
      <div class="space-y-4">
        <!-- Sıra kuyruğu — puan tablosu skora göre sıralı olduğu için sırayı göstermiyor. -->
        <div v-if="turnQueue.length > 1" class="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
          <div class="px-5 py-3 border-b border-gray-200 bg-gray-50">
            <h3 class="font-semibold text-gray-800">🔀 Sıra Kuyruğu</h3>
          </div>
          <div class="divide-y divide-gray-100">
            <div v-for="(p, i) in turnQueue" :key="p.email"
                 :class="['flex items-center gap-3 px-5 py-2.5',
                          i === 0 ? 'bg-green-50' : p.email === myEmail ? 'bg-yellow-50' : '']">
              <span :class="['w-6 h-6 rounded-full flex items-center justify-center text-[11px] font-bold shrink-0',
                             i === 0 ? 'bg-green-200 text-green-800' : 'bg-gray-100 text-gray-500']">
                {{ i === 0 ? '▶' : i }}
              </span>
              <p class="text-sm text-gray-800 truncate flex-1">
                {{ p.displayName }}
                <span v-if="p.email === myEmail" class="text-xs text-indigo-600 font-semibold">(sen)</span>
              </p>
              <span v-if="i === 0" class="text-xs font-semibold text-green-700 shrink-0">
                {{ intermission ? 'sırada' : 'şimdi' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Puan tablosu -->
        <div class="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
          <div class="px-5 py-3 border-b border-gray-200 bg-gray-50">
            <h3 class="font-semibold text-gray-800">🏆 Puan Sıralaması</h3>
          </div>
          <div class="divide-y divide-gray-100">
            <div v-for="entry in leaderboard" :key="entry.email"
                 :class="['flex items-center gap-3 px-5 py-3 transition-colors',
                          entry.email === session.currentTurnEmail ? 'bg-green-50' : '']">
              <span :class="['w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0',
                             rankClass(entry.rank)]">
                {{ entry.rank }}
              </span>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-gray-800 truncate">
                  {{ entry.displayName }}
                  <span v-if="entry.email === myEmail" class="text-xs text-indigo-600">(sen)</span>
                </p>
                <p class="text-xs text-gray-400">
                  {{ entry.wordsSolved }} kelime · {{ entry.correctLetterCount }}✓ {{ entry.wrongLetterCount }}✗
                </p>
              </div>
              <span class="font-bold text-indigo-600 shrink-0">{{ entry.totalScore }}</span>
            </div>
          </div>
        </div>

        <!-- Canlı akış -->
        <div class="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
          <div class="px-5 py-3 border-b border-gray-200 bg-gray-50">
            <h3 class="font-semibold text-gray-800">📜 Son Hamleler</h3>
          </div>
          <div v-if="!recentGuesses.length" class="px-5 py-6 text-center text-sm text-gray-400">
            Henüz hamle yok
          </div>
          <div v-else class="divide-y divide-gray-100 max-h-72 overflow-y-auto">
            <div v-for="(g, i) in recentGuesses" :key="i"
                 class="px-5 py-2.5 flex items-center gap-2 text-sm">
              <span class="text-gray-800 font-medium truncate flex-1">{{ g.displayName }}</span>
              <span :class="['px-2 py-0.5 rounded font-mono text-xs font-semibold',
                             g.correct ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600']">
                {{ upper(g.value) }}
              </span>
              <span :class="['text-xs font-bold w-10 text-right',
                             g.scoreDelta > 0 ? 'text-green-600' : g.scoreDelta < 0 ? 'text-red-500' : 'text-gray-400']">
                {{ g.scoreDelta > 0 ? '+' : '' }}{{ g.scoreDelta }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { guessHangmanLetter, guessHangmanWord } from '../../api/HangmanApi.js'
import { hangmanCategoryLabel } from '../../data/hangmanWords.js'
import { playTurnChime, flashTabTitle } from '../../utils/turnAlert.js'

const TR_ALPHABET = ['A', 'B', 'C', 'Ç', 'D', 'E', 'F', 'G', 'Ğ', 'H', 'I', 'İ', 'J', 'K', 'L', 'M',
  'N', 'O', 'Ö', 'P', 'R', 'S', 'Ş', 'T', 'U', 'Ü', 'V', 'Y', 'Z']
const EN_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

/** Sıra sesi tercihi tarayıcıda saklanır — her oyunda yeniden kapatmak gerekmesin. */
const SOUND_PREF_KEY = 'hangman.turnSound'

/** Sıra kuyruğunda gösterilecek oyuncu sayısı — daha uzunu panelde gürültü yapıyor. */
const QUEUE_PREVIEW = 5

export default {
  name: 'HangmanPlay',
  props: {
    session: Object,
    isHost: Boolean,
    teamId: String
  },
  emits: ['updated', 'skip', 'finish', 'reveal-category', 'next-round'],
  data: () => ({
    wordGuess: '',
    busy: false,
    /**
     * Sıra sayacı. Sunucudan gelen kalan süreyle her durum güncellemesinde senkronlanır,
     * arada yerelde saniye saniye işler — böylece istemci saati kaymış olsa da doğru kalır.
     * Sürenin dolmasıyla sıranın devri sunucudaki zamanlayıcının işi; burası sadece gösterim
     * ve erken kilitlemedir.
     */
    secondsLeft: 0,
    wordOpensIn: 0,
    tickTimer: null,
    /** Sırası olmayan oyuncunun bu sıra penceresindeki tek hakkını kullandı mı? */
    wordTriedThisTurn: false,
    /** Sıra sende uyarısının sesi. Tercih localStorage'dan okunur (bkz. created). */
    soundEnabled: true,
    /** flashTabTitle'ın döndürdüğü durdurucu; null = başlık flaşı çalışmıyor. */
    stopTitleFlash: null
  }),
  computed: {
    round() {
      return this.session?.round || null
    },
    /**
     * Tur arası — biten turun bilgileri. Sunucu tur bitince oyunu burada durdurur
     * (awaitingNextRound) ve sonraki kelimeye moderatörün geçmesini bekler.
     */
    intermission() {
      if (!this.session?.awaitingNextRound) return null
      const r = this.session.lastFinishedRound
      if (!r) return null
      return {
        solved: r.status === 'SOLVED',
        solvedByName: r.solvedByName,
        revealedWord: r.revealedWord,
        roundOrder: r.roundOrder,
        categoryLabel: r.categoryLabel,
        category: r.category,
        score: this.solvedScore(r)
      }
    },
    /** İlan panelindeki harf kutuları. */
    revealedLetters() {
      return this.upper(this.intermission?.revealedWord).split('')
    },
    intermissionCategory() {
      const r = this.intermission
      if (!r?.category && !r?.categoryLabel) return null
      return hangmanCategoryLabel(r.category, this.session?.language) || r.categoryLabel
    },
    /** Ara ekranındaki buton: son kelimeden sonra sıradaki adım sonuç ekranıdır. */
    isLastRound() {
      return (this.session?.currentRoundIndex ?? 0) + 1 >= (this.session?.totalRounds ?? 0)
    },
    locale() {
      return this.session?.language === 'tr' ? 'tr-TR' : 'en-US'
    },
    alphabet() {
      return this.session?.language === 'tr' ? TR_ALPHABET : EN_ALPHABET
    },
    myEmail() {
      return localStorage.getItem('user') || ''
    },
    isMyTurn() {
      return this.session?.currentTurnEmail === this.myEmail
    },
    isSpectator() {
      return (this.session?.participants || [])
          .some(p => p.email === this.myEmail && p.spectator)
    },
    /** Sıraya giren oyuncular, tur sırasına göre (izleyiciler yarışmaz). */
    playersInTurnOrder() {
      return (this.session?.participants || [])
          .filter(p => !p.spectator)
          .slice()
          .sort((a, b) => a.turnOrder - b.turnOrder)
    },
    /**
     * Kuyruğun başı: normalde sırası gelen oyuncu.
     *
     * Ara ekranında currentTurnEmail turu BİTİREN oyuncuda duruyor (sunucu sonraki turu
     * onun sonrasındakiyle başlatıyor), o yüzden orada bir kaydırıyoruz.
     */
    queueStartIndex() {
      const players = this.playersInTurnOrder
      const current = players.findIndex(p => p.email === this.session?.currentTurnEmail)
      if (current < 0) return 0
      return this.intermission ? (current + 1) % players.length : current
    },
    /** Kuyruğun başından itibaren sıradaki oyuncular. */
    turnQueue() {
      const players = this.playersInTurnOrder
      if (!players.length) return []
      const start = this.queueStartIndex
      return Array.from({ length: Math.min(players.length, QUEUE_PREVIEW) },
          (_, i) => players[(start + i) % players.length])
    },
    /**
     * Sıramın gelmesine kaç oyuncu kaldı? 0 = sıra bende, -1 = hesaplanamıyor
     * (izleyiciyim ya da oyunda değilim).
     */
    turnsUntilMine() {
      const players = this.playersInTurnOrder
      if (this.isSpectator || players.length < 2) return -1
      const mine = players.findIndex(p => p.email === this.myEmail)
      if (mine < 0 || !this.session?.currentTurnEmail) return -1
      return (mine - this.queueStartIndex + players.length) % players.length
    },
    turnSeconds() {
      return this.session?.turnSeconds || 30
    },
    wordLockSeconds() {
      return this.session?.wordLockSeconds ?? 10
    },
    turnProgress() {
      if (!this.turnSeconds) return 0
      return Math.max(0, Math.min(100, (this.secondsLeft / this.turnSeconds) * 100))
    },
    /** Harf tahmini her zaman yalnızca sırası gelen oyuncunun hakkı. */
    canGuessLetter() {
      return this.isMyTurn && !this.isSpectator && this.secondsLeft > 0
    },
    /**
     * Kelime tahmini: sırası gelen her an; diğerleri kilit açıldıktan sonra ve sıra başına bir kez.
     */
    canGuessWord() {
      if (this.isSpectator || this.secondsLeft <= 0) return false
      if (this.isMyTurn) return true
      return this.wordOpensIn <= 0 && !this.wordTriedThisTurn
    },
    wordInputPlaceholder() {
      if (this.isSpectator) return 'İzleyicisin'
      if (this.canGuessWord) return 'Tüm kelimeyi yaz...'
      if (this.wordTriedThisTurn) return 'Bu sıradaki hakkını kullandın'
      if (this.wordOpensIn > 0) return `${this.wordOpensIn} sn sonra açılıyor`
      return 'Sıranı bekle'
    },
    /**
     * Kategori sunucudan yalnızca açıldığında gelir; emoji'li etiketi yerelden alırız,
     * bilinmeyen kod olursa sunucunun etiketine düşeriz.
     */
    categoryText() {
      if (!this.round?.categoryRevealed) return null
      return hangmanCategoryLabel(this.round.category, this.session?.language)
          || this.round.categoryLabel
    },
    /** Moderatör için: açılabilecek bir kategori var ve henüz açılmadı. */
    canRevealCategory() {
      return !!this.round?.categoryAvailable && !this.round?.categoryRevealed
    },
    wrongCount() {
      return this.round?.wrongCount || 0
    },
    maxWrong() {
      return this.round?.maxWrong || 6
    },
    remaining() {
      return Math.max(0, this.maxWrong - this.wrongCount)
    },
    displayWord() {
      if (!this.round) return ''
      // maskedWord: bilinen harfler açık, bilinmeyenler '_'
      return this.round.maskedWord
          .split('')
          .map(ch => ch === '_' ? '_' : ch.toLocaleUpperCase(this.locale))
          .join(' ')
    },
    wrongLetters() {
      return (this.round?.wrongLetters || []).map(l => l.toLocaleUpperCase(this.locale))
    },
    guessedLetters() {
      return this.round?.guessedLetters || []
    },
    leaderboard() {
      return this.session?.leaderboard || []
    },
    recentGuesses() {
      return this.session?.recentGuesses || []
    }
  },
  watch: {
    // Sıra bize geçtiğinde eski tahmin metnini temizle.
    'session.currentTurnEmail'() {
      this.wordGuess = ''
      this.wordTriedThisTurn = false
    },
    // Ebeveyn her WS mesajında yeni bir oturum nesnesi verdiği için bu her güncellemede çalışır.
    session: {
      handler() { this.syncTimers() },
      immediate: true
    },
    /**
     * Sıra bana geçtiği AN uyar. immediate yok: bileşen zaten sıram bendeyken açıldığında
     * (sayfa yenileme) zil çalmasın — kullanıcı ekrana bakıyor demektir.
     */
    isMyTurn(mine, wasMine) {
      if (mine && !wasMine) this.alertMyTurn()
      else if (!mine) this.endTitleFlash()
    },
    // Tur arasına girildiğinde sıra kimsede değil; uyarı takılı kalmasın.
    intermission(active) {
      if (active) this.endTitleFlash()
    }
  },
  methods: {
    /** Sunucudan gelen kalan süreyi tek doğru kaynak kabul eder. */
    syncTimers() {
      this.secondsLeft = this.session?.turnSecondsLeft ?? 0
      const opensIn = this.session?.wordOpenInSeconds ?? 0
      // Kilit yeniden kapandıysa yeni bir sıra penceresi başlamıştır → hak tazelenir.
      if (opensIn > 0) this.wordTriedThisTurn = false
      this.wordOpensIn = opensIn
    },
    tick() {
      if (this.secondsLeft > 0) this.secondsLeft--
      if (this.wordOpensIn > 0) this.wordOpensIn--
    },
    /**
     * Sıra bana geçti: kısa zil + (sekme arkadaysa) başlık flaşı.
     *
     * Başlığı yalnızca sekme gizliyken flaşlatırız — ekrana bakan zaten yeşil başlığı
     * görüyor, flaş sadece dikkat dağıtır.
     */
    alertMyTurn() {
      if (this.soundEnabled) playTurnChime()
      if (document.hidden) {
        this.endTitleFlash()
        this.stopTitleFlash = flashTabTitle('🎯 SIRA SENDE!')
      }
    },
    endTitleFlash() {
      if (!this.stopTitleFlash) return
      this.stopTitleFlash()
      this.stopTitleFlash = null
    },
    /** Sekmeye dönüldüğünde flaşın işi bitmiştir. */
    handleVisibility() {
      if (!document.hidden) this.endTitleFlash()
    },
    toggleSound() {
      this.soundEnabled = !this.soundEnabled
      try {
        localStorage.setItem(SOUND_PREF_KEY, this.soundEnabled ? '1' : '0')
      } catch (e) {
        // Depolama kapalıysa tercih bu oturumluk kalır, sorun değil.
      }
      // Açarken bir kez çal: hem örnek olsun hem tarayıcının ses iznini bu tıklamayla al.
      if (this.soundEnabled) playTurnChime()
    },
    upper(v) {
      return (v || '').toLocaleUpperCase(this.locale)
    },
    /**
     * Turu bitiren tahminin kazandırdığı puan — canlı akıştaki kaydından okunur.
     * Akış son {@code RECENT_GUESS_LIMIT} hamleyle sınırlı olduğu için bulunamazsa 0 döner.
     */
    solvedScore(round) {
      if (round?.status !== 'SOLVED') return 0
      const winning = (this.session?.recentGuesses || [])
          .find(g => g.correct && g.userEmail === round.solvedByEmail)
      return winning?.scoreDelta || 0
    },
    isGuessed(letter) {
      return this.guessedLetters.includes(letter.toLocaleLowerCase(this.locale))
    },
    letterClass(letter) {
      if (!this.isGuessed(letter)) {
        return this.canGuessLetter
            ? 'bg-white border-gray-300 text-gray-700 hover:bg-indigo-50 hover:border-indigo-300'
            : 'bg-white border-gray-200 text-gray-300 cursor-not-allowed'
      }
      // round.wrongLetters sunucudan zaten küçük harf geliyor.
      const lower = letter.toLocaleLowerCase(this.locale)
      return (this.round?.wrongLetters || []).includes(lower)
          ? 'bg-red-100 border-red-300 text-red-500'
          : 'bg-green-100 border-green-300 text-green-700'
    },
    rankClass(rank) {
      if (rank === 1) return 'bg-yellow-100 text-yellow-700'
      if (rank === 2) return 'bg-gray-200 text-gray-700'
      if (rank === 3) return 'bg-orange-100 text-orange-700'
      return 'bg-gray-100 text-gray-500'
    },
    async submitLetter(letter) {
      if (!this.canGuessLetter || this.busy) return
      this.busy = true
      try {
        const updated = await guessHangmanLetter(this.teamId, this.session.id,
            letter.toLocaleLowerCase(this.locale))
        this.$emit('updated', updated)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.busy = false
    },
    async submitWord() {
      const guess = this.wordGuess.trim()
      if (!this.canGuessWord || this.busy || !guess) return
      // Sırası olmayanın sıra başına tek hakkı var; sunucu da doğrular, burası sadece UI kilidi.
      const spendsQuota = !this.isMyTurn
      this.busy = true
      try {
        const updated = await guessHangmanWord(this.teamId, this.session.id,
            guess.toLocaleLowerCase(this.locale))
        this.wordGuess = ''
        if (spendsQuota) this.wordTriedThisTurn = true
        this.$emit('updated', updated)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.busy = false
    },
    handleKeydown(e) {
      if (!this.canGuessLetter || this.busy) return
      // Kelime tahmini alanına yazarken klavye kısayolu devreye girmesin.
      if (e.target && ['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return
      if (e.key.length !== 1) return
      const upper = e.key.toLocaleUpperCase(this.locale)
      if (this.alphabet.includes(upper) && !this.isGuessed(upper)) {
        this.submitLetter(upper)
      }
    }
  },
  created() {
    try {
      this.soundEnabled = localStorage.getItem(SOUND_PREF_KEY) !== '0'
    } catch (e) {
      // Depolama okunamıyorsa varsayılan (açık) kalır.
    }
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeydown)
    document.addEventListener('visibilitychange', this.handleVisibility)
    this.syncTimers()
    this.tickTimer = setInterval(this.tick, 1000)
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown)
    document.removeEventListener('visibilitychange', this.handleVisibility)
    clearInterval(this.tickTimer)
    // Başlık flaşı sürerken çıkılırsa sekme adı takılı kalırdı.
    this.endTitleFlash()
  }
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
