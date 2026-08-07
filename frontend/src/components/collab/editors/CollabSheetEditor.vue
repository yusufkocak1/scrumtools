<template>
  <div class="h-full flex flex-col">
    <div v-if="mobileReadOnly"
         class="px-4 py-2 text-xs bg-amber-50 text-amber-800 border-b border-amber-200 flex items-center gap-2">
      <span class="font-medium">Salt okunur</span>
      <span>— hesap tablosu düzenleme masaüstünde yapılabilir.</span>
    </div>
    <div ref="host" class="flex-1 min-h-0 univer-host"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { UniverYjsBridge } from '../../../collab/UniverYjsBridge.js'

/**
 * Eş zamanlı hesap tablosu — Univer + UniverYjsBridge (plan K4, Faz 3).
 *
 * Univer motoru **tembel yükleniyor**: paket birkaç MB ve React'i de beraberinde
 * getiriyor. Statik import edilseydi metin/kod dokümanı açan kullanıcı da bu
 * bedeli öderdi.
 */
const props = defineProps({
  ydoc: { type: Object, required: true },
  awareness: { type: Object, required: true },
  documentId: { type: String, required: true },
  readOnly: { type: Boolean, default: false }
})

const emit = defineEmits(['snapshotText', 'ready'])

const host = ref(null)
let univer = null
let univerAPI = null
let bridge = null
let awarenessHandler = null

/**
 * R8 — mobilde salt okunur.
 *
 * Dokunmatik ızgara deneyimi v1'de yeterli değil; yazmaya izin vermek
 * kullanıcının yanlışlıkla hücre bozmasına yol açıyor. Ölçüm bir kez yapılıyor:
 * ekran döndürüldüğünde modun değişmesi düzenlemenin ortasında kilitlenmek olurdu.
 */
const mobileReadOnly = ref(false)
const effectiveReadOnly = computed(() => props.readOnly || mobileReadOnly.value)

onMounted(async () => {
  mobileReadOnly.value = window.matchMedia('(max-width: 767px)').matches

  const { createUniver } = await import('../../../univer/createUniver.js')
  if (!host.value) return

  const instance = createUniver({
    container: host.value,
    presetOptions: {
      // Salt okunur modda araç çubuğu yanıltıcı olurdu: düğmeler görünür ama
      // her biri sessizce reddedilirdi.
      toolbar: !effectiveReadOnly.value,
      footer: true
    }
  })
  univer = instance.univer
  univerAPI = instance.univerAPI

  bridge = new UniverYjsBridge({
    ydoc: props.ydoc,
    univer,
    univerAPI,
    awareness: props.awareness,
    unitId: props.documentId
  })

  const workbookData = bridge.buildWorkbookData()
  const workbook = univerAPI.createWorkbook(workbookData)
  bridge.start(workbook)

  if (effectiveReadOnly.value) {
    // Univer'in kendi izin motorunu kurmak yerine giriş katmanı kapatılıyor:
    // yazma yetkisi olmayanın paketleri sunucuda zaten atılıyor (plan §6), bu
    // yalnızca kullanıcıyı boşuna uğraştırmamak için.
    host.value.classList.add('univer-read-only')
  }

  // Anlık görüntü metnini köprü üretir: hesaplanmış formül değerleri yalnızca
  // Univer'de var, CRDT'de yok (K5).
  emit('snapshotText', () => bridge.toSnapshotJson())
  emit('ready')

  awarenessHandler = () => {
    if (!bridge) return
    const states = new Map()
    props.awareness.getStates().forEach((state, clientId) => {
      if (clientId === props.awareness.clientID) return
      states.set(clientId, state)
    })
    bridge.renderRemoteCursors(states)
  }
  props.awareness.on('change', awarenessHandler)
})

watch(() => props.readOnly, (value) => {
  host.value?.classList.toggle('univer-read-only', value || mobileReadOnly.value)
})

/** İçe aktarma tohumlaması ve geçmişten geri yükleme için (plan §10 / §6). */
function seedContent(json) {
  if (!bridge || !json) return
  try {
    bridge.seedFromModel(typeof json === 'string' ? JSON.parse(json) : json)
  } catch (error) {
    console.warn('[collab] tablo içeriği uygulanamadı', error)
  }
}

/** Makro anlık görüntüsü: worker'a giden okunabilir kopya (plan K8). */
function getSnapshotModel() {
  if (!bridge) return { sheets: [] }
  try {
    return JSON.parse(bridge.toSnapshotJson())
  } catch {
    return { sheets: [] }
  }
}

/** Makronun ürettiği işlem listesi — tek transaction'da uygulanır. */
function applyMacroOps(ops) {
  bridge?.applyMacroOps(ops)
}

defineExpose({ seedContent, getSnapshotModel, applyMacroOps })

onBeforeUnmount(() => {
  if (awarenessHandler) props.awareness.off('change', awarenessHandler)
  bridge?.destroy()
  bridge = null
  try {
    univer?.dispose?.()
  } catch { /* kapanışta Univer bazen zaten yok edilmiş olur */ }
  univer = null
  univerAPI = null
})
</script>

<style scoped>
.univer-host {
  position: relative;
  width: 100%;
}

/* Salt okunur: ızgara görünür ve kaydırılabilir kalır, giriş engellenir. */
.univer-host.univer-read-only :deep(.univer-sheet-container) {
  pointer-events: none;
}
</style>
