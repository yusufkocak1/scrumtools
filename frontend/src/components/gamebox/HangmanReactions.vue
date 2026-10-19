<template>
  <div>
    <!-- Uçan tepkiler — ekranın altından yükselip kaybolur, oyuna dokunmaz. -->
    <div class="pointer-events-none fixed bottom-20 right-4 sm:right-8 z-40 w-40 h-72 overflow-hidden">
      <div v-for="r in reactions" :key="r.id"
           class="reaction-float absolute bottom-0 flex flex-col items-center"
           :style="{ left: r.offset + '%' }">
        <span class="text-3xl leading-none">{{ r.emoji }}</span>
        <span class="mt-0.5 px-1.5 py-0.5 rounded bg-gray-900/60 text-white text-[10px] whitespace-nowrap max-w-[7rem] truncate">
          {{ r.displayName }}
        </span>
      </div>
    </div>

    <!-- Tepki çubuğu -->
    <div class="bg-white rounded-2xl shadow-lg border border-gray-200 px-3 py-2 flex flex-wrap justify-center gap-1">
      <button v-for="emoji in emojis" :key="emoji" @click="send(emoji)"
              :title="`${emoji} gönder`"
              class="w-10 h-10 rounded-xl text-xl hover:bg-gray-100 active:scale-90 transition-transform">
        {{ emoji }}
      </button>
    </div>
  </div>
</template>

<script>
import { sendHangmanReaction } from '../../api/HangmanApi.js'

/** Sunucudaki HangmanSessionService.REACTION_EMOJIS ile aynı olmalı. */
const EMOJIS = ['👏', '🔥', '😂', '😱', '🤔', '💀', '🎉', '👀']

export default {
  name: 'HangmanReactions',
  props: {
    session: Object,
    teamId: String,
    /** GameBox'ın WS'ten topladığı son tepkiler: [{ id, emoji, displayName, offset }] */
    reactions: { type: Array, default: () => [] }
  },
  data: () => ({
    emojis: EMOJIS
  }),
  methods: {
    async send(emoji) {
      try {
        await sendHangmanReaction(this.teamId, this.session.id, emoji)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    }
  }
}
</script>

<style scoped>
.reaction-float {
  animation: reaction-rise 2.8s ease-out forwards;
}
@keyframes reaction-rise {
  0%   { transform: translateY(0) scale(0.6); opacity: 0; }
  10%  { transform: translateY(-20px) scale(1.1); opacity: 1; }
  80%  { opacity: 1; }
  100% { transform: translateY(-240px) scale(1); opacity: 0; }
}
</style>
