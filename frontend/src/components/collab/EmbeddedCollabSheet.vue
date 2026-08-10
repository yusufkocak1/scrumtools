<template>
  <div class="h-full">
    <CollabSheetEditor
        ref="sheetEditor"
        :ydoc="ydoc"
        :awareness="awareness"
        :document-id="documentId"
        :read-only="!canWrite"/>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import CollabSheetEditor from './editors/CollabSheetEditor.vue'
import CollabApi from '../../api/CollabApi.js'
import { useCollabDoc } from '../../composables/useCollabDoc.js'

/**
 * Docs içine gömülü <b>canlı</b> hesap tablosu (plan Y3).
 *
 * Ayrı bir dosya olmasının sebebi tembel yükleme: `CollabEmbed` bunu yalnızca
 * canlı modda import ediyor, böylece Docs okuyan kullanıcı Univer'i indirmiyor.
 *
 * Oturum tamamen bağımsız: kendi `/ws/collab` bağlantısı, kendi yazar seçimi
 * (K6). Gömüldüğü sayfayla ortak hiçbir durumu yok — Docs sayfası kaydedilirken
 * bu dokümanın içeriği kopyalanmaz, yalnızca kimliği gömülüdür.
 */
const props = defineProps({
  projectId: { type: String, required: true },
  documentId: { type: String, required: true }
})

const { ydoc, awareness, canWrite, status } = useCollabDoc(props.projectId, props.documentId)

const sheetEditor = ref(null)
let seedAttempted = false

/**
 * Tohumlama — `CollabDocument.vue` ile aynı sözleşme (plan Y1 adım 3 / §10).
 *
 * <b>Neden burada da gerekli:</b> Excel'den içe aktarılmış bir tablonun modeli
 * `snapshot_text`'te bekliyor ve CRDT'ye ilk açan istemci yazıyor. Gömülü
 * düzenleyici bunu yapmadığı için, tabloyu ilk kez Docs içinden açan kullanıcı
 * <b>boş bir ızgara</b> görüyordu — ve orada bir şey yazdığında içe aktarılan
 * veri hiç yüklenmemiş olarak kalıyordu.
 */
watch([status, canWrite], async ([currentStatus, writable]) => {
  if (seedAttempted || currentStatus !== 'synced' || !writable) return
  seedAttempted = true
  try {
    const { data } = await CollabApi.claimSeed(props.projectId, props.documentId)
    // granted=false: başka bir sekme aktarımı üstlendi — dokunmuyoruz.
    if (data?.granted && data.content) sheetEditor.value?.seedContent(data.content)
  } catch {
    seedAttempted = false
  }
}, { immediate: true })
</script>
