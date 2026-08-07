<template>
  <div class="collab-embed my-4 rounded-xl border border-slate-200 overflow-hidden bg-white">
    <header class="px-3 py-2 bg-slate-50 border-b border-slate-200 flex items-center gap-2">
      <span class="px-1.5 py-0.5 rounded bg-emerald-50 text-emerald-600 text-[10px] font-semibold">
        {{ doc?.type === 'SHEET' ? 'TABLO' : 'ORTAK' }}
      </span>
      <span class="text-xs font-medium text-slate-700 truncate">
        {{ doc?.title || 'Ortak doküman' }}
      </span>
      <span v-if="live" class="text-[10px] text-emerald-600 flex items-center gap-1">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>canlı
      </span>
      <a :href="documentLink" target="_blank" rel="noopener"
         class="ml-auto text-[11px] px-2 py-1 rounded-lg text-indigo-600 hover:bg-indigo-50 transition shrink-0">
        Aç
      </a>
    </header>

    <div v-if="loading" class="p-4 text-xs text-slate-400">Yükleniyor…</div>
    <div v-else-if="error" class="p-4 text-xs text-rose-600">{{ error }}</div>

    <!-- Canlı gömme (Docs düzenleme modu). Kendi WS bağlantısını kurar. -->
    <div v-else-if="live" :style="{ height: height + 'px' }" class="min-h-0">
      <component :is="LiveSheet" v-if="LiveSheet"
                 :project-id="projectId"
                 :document-id="documentId"/>
    </div>

    <!-- Salt okunur önizleme (Docs okuma modu): anlık görüntüden üretiliyor,
         WS bağlantısı kurulmuyor. Bir sayfada beş gömülü tablo varsa beş canlı
         oturum açmak, okuyan herkes için beş bağlantı demekti. -->
    <div v-else class="overflow-auto" :style="{ maxHeight: height + 'px' }">
      <table v-if="preview.rows.length" class="w-full text-xs border-collapse">
        <tbody>
          <tr v-for="(row, r) in preview.rows" :key="r" class="border-b border-slate-100 last:border-0">
            <td v-for="(cell, c) in row" :key="c"
                class="px-2 py-1 border-r border-slate-100 last:border-0 text-slate-700 whitespace-nowrap">
              {{ cell }}
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="p-4 text-xs text-slate-400">Tablo henüz boş.</p>
      <p v-if="preview.truncated" class="px-3 py-1.5 text-[10px] text-slate-400 border-t border-slate-100">
        İlk {{ PREVIEW_ROWS }} satır gösteriliyor — tamamı için “Aç”.
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, shallowRef } from 'vue'
import CollabApi from '../../api/CollabApi.js'
import { parseCellKey } from '../../collab/UniverYjsBridge.js'

/**
 * Docs sayfasına gömülü ortak doküman (COLLAB_WORKSPACE_PLAN.md Y3).
 *
 * İki mod var ve farkları bilinçli:
 *
 * - **Okuma modu** (varsayılan): `snapshotText`'ten üretilmiş salt-okunur tablo.
 *   WS bağlantısı yok. Bir sayfada birkaç gömülü tablo olabilir; her biri canlı
 *   olsaydı sayfayı <i>okuyan</i> herkes o kadar eşzamanlı oturum açardı ve
 *   D3'ün bağlantı bütçesi okuma trafiğiyle tükenirdi.
 * - **Canlı mod** (`live`): gerçek Univer örneği, kendi bağlantısıyla. Yalnızca
 *   Docs düzenleme modunda, yani kullanıcının bilerek girdiği bir bağlamda.
 */
const props = defineProps({
  projectId: { type: String, required: true },
  documentId: { type: String, required: true },
  height: { type: Number, default: 420 },
  live: { type: Boolean, default: false }
})

const PREVIEW_ROWS = 50
const PREVIEW_COLS = 20

const doc = ref(null)
const loading = ref(true)
const error = ref('')
const LiveSheet = shallowRef(null)

const documentLink = computed(
    () => `/projects/${props.projectId}/collab/${props.documentId}`)

/**
 * Anlık görüntü JSON'undan tablo matrisi.
 *
 * Hesaplanmış formül değerleri anlık görüntüde zaten var (K5) — burada yeniden
 * hesaplanmıyor, ki zaten sunucu da istemci de bunu yapmıyor.
 */
const preview = computed(() => {
  const empty = { rows: [], truncated: false }
  if (!doc.value?.snapshotText) return empty

  let model
  try {
    model = JSON.parse(doc.value.snapshotText)
  } catch {
    return empty
  }

  const sheet = model?.sheets?.[0]
  if (!sheet?.cells) return empty

  let maxRow = -1
  let maxCol = -1
  const values = new Map()
  for (const [key, cell] of Object.entries(sheet.cells)) {
    const position = parseCellKey(key)
    if (!position) continue
    if (position.row >= PREVIEW_ROWS || position.col >= PREVIEW_COLS) continue
    const value = cell?.v ?? cell?.f ?? ''
    if (value === '' || value == null) continue
    values.set(`${position.row}:${position.col}`, value)
    if (position.row > maxRow) maxRow = position.row
    if (position.col > maxCol) maxCol = position.col
  }
  if (maxRow < 0) return empty

  const rows = []
  for (let r = 0; r <= maxRow; r++) {
    const row = []
    for (let c = 0; c <= maxCol; c++) row.push(values.get(`${r}:${c}`) ?? '')
    rows.push(row)
  }

  const totalRows = Object.keys(sheet.cells).length
  return { rows, truncated: totalRows > 0 && maxRow >= PREVIEW_ROWS - 1 }
})

onMounted(async () => {
  try {
    const { data } = await CollabApi.getDocument(props.projectId, props.documentId)
    doc.value = data
  } catch (e) {
    error.value = e?.response?.status === 403
        ? 'Bu dokümanı görme yetkiniz yok.'
        : 'Gömülü doküman açılamadı.'
    loading.value = false
    return
  }

  if (props.live) {
    // Univer birkaç MB ve React'i de getiriyor; okuma modunda hiç yüklenmiyor.
    const module = await import('./EmbeddedCollabSheet.vue')
    LiveSheet.value = module.default
  }
  loading.value = false
})
</script>
