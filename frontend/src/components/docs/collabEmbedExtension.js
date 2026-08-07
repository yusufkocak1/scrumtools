import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import CollabEmbedNodeView from './CollabEmbedNodeView.vue'

/**
 * Docs sayfasına ortak doküman gömme düğümü (COLLAB_WORKSPACE_PLAN.md Y3).
 *
 * Kaydedilen HTML, planın §8'de tarif ettiği biçim:
 * ```html
 * <div data-collab-embed data-document-id="…" data-type="SHEET" data-height="420"></div>
 * ```
 *
 * <b>Düğüm boş bir kap, içerik değil.</b> Tablonun bir kopyasını sayfaya
 * gömmek kolay olurdu ama o kopya ilk düzenlemede eskir; Y3'ün bütün amacı
 * "canlı" olması. Bu yüzden HTML'de yalnızca <i>kimlik</i> duruyor, içerik her
 * görüntülemede dokümandan geliyor. Yan etkisi: yetkisi olmayan biri sayfayı
 * okusa da tabloyu göremez, çünkü içeriği getiren istek onun oturumuyla gider.
 */
export const CollabEmbed = Node.create({
  name: 'collabEmbed',
  group: 'block',
  atom: true,
  draggable: true,

  addAttributes() {
    return {
      documentId: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-document-id'),
        renderHTML: (attributes) => ({ 'data-document-id': attributes.documentId })
      },
      docType: {
        default: 'SHEET',
        parseHTML: (element) => element.getAttribute('data-type') || 'SHEET',
        renderHTML: (attributes) => ({ 'data-type': attributes.docType })
      },
      height: {
        default: 420,
        parseHTML: (element) => Number(element.getAttribute('data-height')) || 420,
        renderHTML: (attributes) => ({ 'data-height': String(attributes.height) })
      }
    }
  },

  parseHTML() {
    return [{ tag: 'div[data-collab-embed]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-collab-embed': '' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(CollabEmbedNodeView)
  },

  addCommands() {
    return {
      insertCollabEmbed: (attributes) => ({ commands }) =>
        commands.insertContent({ type: this.name, attrs: attributes })
    }
  }
})

export default CollabEmbed
