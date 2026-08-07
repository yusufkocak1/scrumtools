<template>
  <div class="flex items-center -space-x-2">
    <div
        v-for="person in visible"
        :key="person.email"
        class="relative w-8 h-8 rounded-full ring-2 ring-white flex items-center justify-center text-xs font-semibold text-white shadow-sm"
        :style="{ backgroundColor: person.color }"
        :title="person.self ? `${person.name} (siz)` : person.name">
      {{ initials(person.name) }}
      <span v-if="person.self"
            class="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-white"></span>
    </div>

    <div v-if="overflow > 0"
         class="w-8 h-8 rounded-full ring-2 ring-white bg-slate-200 text-slate-600 flex items-center justify-center text-xs font-semibold"
         :title="overflowNames">
      +{{ overflow }}
    </div>

    <span v-if="participants.length === 0" class="text-xs text-slate-400 pl-1">
      Bağlanılıyor…
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

/**
 * Dokümanda o an açık olan kişiler.
 *
 * Veri awareness'tan gelir, ayrı bir presence isteğinden değil: awareness zaten
 * imleçler için akıyor, aynı bilgiyi ikinci bir kanaldan çekmek boşuna trafik
 * ve iki listenin ayrışma riski demek.
 */
const props = defineProps({
  participants: { type: Array, default: () => [] },
  max: { type: Number, default: 5 }
})

const visible = computed(() => props.participants.slice(0, props.max))
const overflow = computed(() => Math.max(0, props.participants.length - props.max))
const overflowNames = computed(() =>
    props.participants.slice(props.max).map(p => p.name).join(', ')
)

function initials(name) {
  if (!name) return '?'
  return name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase()
}
</script>
