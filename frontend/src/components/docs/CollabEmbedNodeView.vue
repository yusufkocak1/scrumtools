<template>
  <NodeViewWrapper class="collab-embed-node" :class="{ 'ring-2 ring-indigo-300 rounded-xl': selected }">
    <CollabEmbed
        v-if="projectId && node.attrs.documentId"
        :project-id="projectId"
        :document-id="node.attrs.documentId"
        :height="node.attrs.height"
        :live="live"/>
    <div v-else class="my-4 p-4 rounded-xl border border-dashed border-slate-300 text-xs text-slate-400">
      Gömülü doküman tanımsız.
    </div>

    <div v-if="editor.isEditable" class="flex items-center gap-2 mt-1 mb-3">
      <button @click="live = !live"
              class="text-[11px] px-2 py-1 rounded-lg text-slate-600 hover:bg-slate-100 transition">
        {{ live ? 'Önizlemeye dön' : 'Canlı düzenle' }}
      </button>
      <button @click="deleteNode"
              class="text-[11px] px-2 py-1 rounded-lg text-rose-600 hover:bg-rose-50 transition">
        Gömmeyi kaldır
      </button>
    </div>
  </NodeViewWrapper>
</template>

<script setup>
import { ref, inject } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'
import CollabEmbed from '../collab/CollabEmbed.vue'

/**
 * Gömülü dokümanın düzenleyici içindeki görünümü (plan Y3).
 *
 * <b>Canlı mod varsayılan olarak kapalı.</b> Plan "Docs düzenleme modunda canlı
 * örnek" diyor; uygulamada bunu bir düğmenin arkasına aldım. Sebebi: sayfayı
 * düzenlemeye açan biri çoğu zaman metne dokunuyor, gömülü tabloya değil.
 * Her açılışta Univer'i indirip WS oturumu kurmak, tabloya hiç bakmayacak
 * kullanıcıya birkaç MB ve bir bağlantı maliyeti çıkarırdı (D3).
 */
defineProps({
  node: { type: Object, required: true },
  editor: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  deleteNode: { type: Function, required: true }
})

const live = ref(false)

// Düzenleyici projeyi prop olarak alıyor; düğüm görünümü ağacın içinde
// olduğundan provide/inject ile ulaşıyor.
const projectId = inject('collabEmbedProjectId', null)
</script>
