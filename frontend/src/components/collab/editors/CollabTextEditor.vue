<template>
  <div class="collab-text-editor h-full flex flex-col">
    <div v-if="!readOnly"
         class="bg-white border-b border-slate-200 px-3 py-1.5 flex flex-wrap items-center gap-0.5">
      <button @click="editor?.chain().focus().toggleBold().run()"
              :class="btn(editor?.isActive('bold'))" title="Kalın"><strong>B</strong></button>
      <button @click="editor?.chain().focus().toggleItalic().run()"
              :class="btn(editor?.isActive('italic'))" title="İtalik"><em>I</em></button>
      <button @click="editor?.chain().focus().toggleStrike().run()"
              :class="btn(editor?.isActive('strike'))" title="Üstü çizili"><s>S</s></button>
      <span class="w-px h-5 bg-slate-200 mx-1.5"></span>
      <button @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()"
              :class="btn(editor?.isActive('heading', { level: 1 }))">H1</button>
      <button @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
              :class="btn(editor?.isActive('heading', { level: 2 }))">H2</button>
      <button @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
              :class="btn(editor?.isActive('heading', { level: 3 }))">H3</button>
      <span class="w-px h-5 bg-slate-200 mx-1.5"></span>
      <button @click="editor?.chain().focus().toggleBulletList().run()"
              :class="btn(editor?.isActive('bulletList'))" title="Madde listesi">•</button>
      <button @click="editor?.chain().focus().toggleOrderedList().run()"
              :class="btn(editor?.isActive('orderedList'))" title="Numaralı liste">1.</button>
      <button @click="editor?.chain().focus().toggleBlockquote().run()"
              :class="btn(editor?.isActive('blockquote'))" title="Alıntı">❝</button>
      <button @click="editor?.chain().focus().toggleCodeBlock().run()"
              :class="btn(editor?.isActive('codeBlock'))" title="Kod bloğu">&lt;/&gt;</button>
      <button @click="editor?.chain().focus().toggleHighlight().run()"
              :class="btn(editor?.isActive('highlight'))" title="Vurgula">▨</button>
      <span class="w-px h-5 bg-slate-200 mx-1.5"></span>
      <button @click="insertTable" :class="btn(false)" title="Tablo ekle">▦</button>
    </div>

    <div class="flex-1 overflow-y-auto">
      <editor-content :editor="editor" class="prose prose-indigo max-w-none p-6 min-h-full"/>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Highlight from '@tiptap/extension-highlight'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import CollabEmbed from '../../docs/collabEmbedExtension.js'

/**
 * Eş zamanlı zengin metin editörü — TipTap + @tiptap/y-tiptap (plan K1).
 *
 * Bağlamayı y-prosemirror değil, TipTap'in kendi çatalı olan `@tiptap/y-tiptap`
 * yapıyor: Collaboration eklentisinin akran (peer) bağımlılığı odur, ikisi bir
 * arada kurulursa iki ayrı ProseMirror eklenti anahtarı oluşur.
 *
 * Docs modülüyle aynı içerik biçimini (HTML) üretir; Faz 2'de bir Docs sayfası
 * bu editörle ortak düzenlenebilecek.
 */
const props = defineProps({
  ydoc: { type: Object, required: true },
  awareness: { type: Object, required: true },
  readOnly: { type: Boolean, default: false }
})

const emit = defineEmits(['snapshotText'])

const editor = useEditor({
  editable: !props.readOnly,
  extensions: [
    // Geri alma yığınını StarterKit değil Collaboration yönetmeli: yerel geçmiş
    // uzaktan gelen değişiklikleri de geri alır ve başkasının yazdığını siler.
    StarterKit.configure({ undoRedo: false }),
    Highlight,
    Link.configure({ openOnClick: false }),
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader,
    Placeholder.configure({ placeholder: 'Yazmaya başlayın — ekibiniz anlık olarak görecek…' }),
    // Y3 gömme düğümü burada da tanımlı olmak zorunda. Eksik olsaydı, gömme
    // içeren bir Docs sayfası "Ortak Düzenle" ile açıldığında TipTap tanımadığı
    // düğümü ayrıştırırken atardı — ve sayfa Docs'a geri yazıldığında gömme
    // sessizce kaybolurdu.
    CollabEmbed,
    Collaboration.configure({ document: props.ydoc, field: 'prosemirror' }),
    CollaborationCaret.configure({ provider: { awareness: props.awareness } })
  ]
})

// Anlık görüntü metni HTML olarak üretilir; sunucu bunu sanitize edip saklar
// ve Faz 2'de Docs sayfasına yazar (plan K6/Y1).
emit('snapshotText', () => editor.value?.getHTML() ?? '')

watch(() => props.readOnly, (value) => {
  editor.value?.setEditable(!value)
})

function insertTable() {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

/**
 * Boş bir Y.Doc'a Docs sayfasının HTML'ini aktarır (plan Y1 adım 3).
 *
 * `prosemirrorToYDoc` yerine düz `setContent` kullanılıyor: Collaboration
 * eklentisi zaten her ProseMirror transaction'ını Y.Doc'a yazıyor, yani içerik
 * normal yazma yoluyla girer ve aynı CRDT güncellemesi olarak röle edilir.
 * Ayrı bir dönüştürme yolu, şema uyuşmazlığı riskini boşuna eklerdi.
 *
 * Çağıran taraf tohumlama hakkını sunucudan almış olmalı — bu fonksiyonun
 * kendisi ikilenmeye karşı koruma sağlamaz.
 */
function seedContent(html) {
  if (!editor.value || !html) return
  editor.value.commands.setContent(html, { emitUpdate: true })
}

defineExpose({ seedContent })

function btn(isActive) {
  return [
    'h-8 min-w-[2rem] px-1.5 inline-flex items-center justify-center rounded-lg text-sm transition',
    isActive ? 'bg-indigo-100 text-indigo-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-800'
  ]
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style>
.collab-text-editor .ProseMirror {
  outline: none;
  min-height: 100%;
}

.collab-text-editor .ProseMirror p.is-editor-empty:first-child::before {
  color: #adb5bd;
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}

/* Uzaktaki imleç ve sahibinin adı — CollaborationCaret'in ürettiği yapı */
.collab-text-editor .collaboration-carets__caret {
  border-left: 1px solid #0d0d0d;
  border-right: 1px solid #0d0d0d;
  margin-left: -1px;
  margin-right: -1px;
  pointer-events: none;
  position: relative;
  word-break: normal;
}

.collab-text-editor .collaboration-carets__label {
  border-radius: 3px 3px 3px 0;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
  left: -1px;
  line-height: normal;
  padding: 0.1rem 0.3rem;
  position: absolute;
  top: -1.4em;
  user-select: none;
  white-space: nowrap;
}

.collab-text-editor .ProseMirror table {
  border-collapse: collapse;
  margin: 1rem 0;
  width: 100%;
  table-layout: fixed;
}

.collab-text-editor .ProseMirror th,
.collab-text-editor .ProseMirror td {
  border: 2px solid #d1d5db;
  padding: 0.5rem 0.75rem;
  min-width: 80px;
  vertical-align: top;
}

.collab-text-editor .ProseMirror th {
  background-color: #f3f4f6;
  font-weight: 600;
}
</style>
