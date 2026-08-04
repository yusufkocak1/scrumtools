<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="$emit('close')">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6">
      <h2 class="text-base font-semibold text-gray-800">{{ heading }}</h2>

      <label class="block text-xs font-medium text-gray-600 mt-4 mb-1.5">Pano adı</label>
      <input
        ref="nameInput"
        v-model="name"
        type="text"
        maxlength="60"
        placeholder="Sürüm takibi"
        class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
        @keyup.enter="submit"
      />

      <template v-if="mode !== 'duplicate'">
        <label class="block text-xs font-medium text-gray-600 mt-4 mb-1.5">Görünürlük</label>
        <div class="space-y-1.5">
          <label class="flex items-start gap-2 p-2 rounded-lg hover:bg-gray-50 cursor-pointer">
            <input type="radio" value="PRIVATE" v-model="visibility" class="mt-0.5 accent-purple-600" />
            <span>
              <span class="block text-sm text-gray-700">Yalnız ben</span>
              <span class="block text-[11px] text-gray-400">Takımdaki kimse bu panoyu görmez.</span>
            </span>
          </label>
          <label class="flex items-start gap-2 p-2 rounded-lg hover:bg-gray-50 cursor-pointer">
            <input type="radio" value="TEAM" v-model="visibility" class="mt-0.5 accent-purple-600" />
            <span>
              <span class="block text-sm text-gray-700">Takım</span>
              <span class="block text-[11px] text-gray-400">
                Takımın tümü görür; düzenlemeyi siz ve organizasyon yöneticileri yapabilir.
              </span>
            </span>
          </label>
        </div>
      </template>

      <p v-if="error" class="mt-3 text-xs text-red-600">{{ error }}</p>

      <button
        class="mt-5 w-full text-sm bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white px-4 py-2 rounded-lg transition-colors"
        :disabled="!name.trim() || saving"
        @click="submit"
      >
        {{ saving ? 'Kaydediliyor…' : submitLabel }}
      </button>
      <button class="mt-2 w-full text-sm text-gray-500 hover:text-gray-700" @click="$emit('close')">
        İptal
      </button>
    </div>
  </div>
</template>

<script setup>
/**
 * Pano oluşturma / yeniden adlandırma / kopyalama formu.
 *
 * Üç iş için tek form: alanları (ad, görünürlük) aynı, farklı olan yalnız
 * başlık ve düğme metni. Kopyalamada görünürlük sorulmaz — kopya her zaman
 * özel başlar; başkasının paylaştığı panoyu kopyalayıp aynı adla takıma geri
 * yaymak, sekme şeridinde iki özdeş isim demekti.
 */
import { ref, computed, onMounted, nextTick } from 'vue'

const props = defineProps({
  /** 'create' | 'rename' | 'duplicate' */
  mode: { type: String, default: 'create' },
  initialName: { type: String, default: '' },
  initialVisibility: { type: String, default: 'PRIVATE' },
  saving: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['save', 'close'])

const name = ref(props.initialName)
const visibility = ref(props.initialVisibility)
const nameInput = ref(null)

const heading = computed(() => ({
  create: 'Yeni pano',
  rename: 'Panoyu yeniden adlandır',
  duplicate: 'Panoyu kopyala',
}[props.mode] || 'Pano'))

const submitLabel = computed(() => ({
  create: 'Oluştur',
  rename: 'Kaydet',
  duplicate: 'Kopyala',
}[props.mode] || 'Kaydet'))

onMounted(async () => {
  await nextTick()
  nameInput.value?.focus()
  nameInput.value?.select()
})

function submit() {
  const trimmed = name.value.trim()
  if (!trimmed || props.saving) return
  emit('save', { name: trimmed, visibility: visibility.value })
}
</script>
