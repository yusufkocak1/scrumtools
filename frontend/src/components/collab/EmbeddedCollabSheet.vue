<template>
  <div class="h-full">
    <CollabSheetEditor
        v-if="ydoc"
        :ydoc="ydoc"
        :awareness="awareness"
        :document-id="documentId"
        :read-only="!canWrite"/>
    <div v-else class="p-4 text-xs text-slate-400">Bağlanıyor…</div>
  </div>
</template>

<script setup>
import CollabSheetEditor from './editors/CollabSheetEditor.vue'
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

const { ydoc, awareness, canWrite } = useCollabDoc(props.projectId, props.documentId)
</script>
