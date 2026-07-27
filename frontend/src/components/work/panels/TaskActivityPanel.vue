<template>
  <TaskPanel panel-key="activity" title="Aktivite" :count="comments.length">
    <template #icon>
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
      </svg>
    </template>

    <template #actions>
      <button
        type="button"
        class="panel-action"
        :title="newestFirst ? 'En eskiden başlat' : 'En yeniden başlat'"
        @click="newestFirst = !newestFirst"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M3 4h13M3 8h9M3 12h5m5 8V8m0 12l-3-3m3 3l3-3"/>
        </svg>
        {{ newestFirst ? 'En yeni' : 'En eski' }}
      </button>
    </template>

    <!-- Tarihçe ile yorumlar aynı zaman çizgisinin iki yüzü; ayrı kartlarda
         dururken "bu görevde ne oldu" sorusu iki yerden okunuyordu -->
    <div class="tabs">
      <button type="button" class="tab" :class="{ 'tab--active': tab === 'comments' }" @click="tab = 'comments'">
        Yorumlar
        <span class="tab-count">{{ comments.length }}</span>
      </button>
      <button type="button" class="tab" :class="{ 'tab--active': tab === 'history' }" @click="tab = 'history'">
        Tarihçe
        <span class="tab-count">{{ history.length }}</span>
      </button>
      <button type="button" class="tab" :class="{ 'tab--active': tab === 'all' }" @click="tab = 'all'">
        Tümü
      </button>
    </div>

    <!-- Yorum yazma -->
    <div v-if="tab !== 'history'" class="flex items-start gap-2.5 mb-4">
      <div class="avatar avatar--me">{{ myInitials }}</div>
      <div class="flex-1 min-w-0">
        <div
          v-if="composerOpen"
          class="rounded-lg border border-gray-200 focus-within:border-blue-300 focus-within:ring-2 focus-within:ring-blue-100 transition-all overflow-hidden"
        >
          <TiptapEditor v-model="newComment" :uploadHandler="uploadHandler" placeholder="Yorumunuzu yazın…" :compact="true" />
        </div>
        <button
          v-else
          type="button"
          class="w-full text-left text-sm text-gray-400 border border-gray-200 rounded-lg px-3 py-2 hover:border-gray-300 hover:text-gray-500 transition-colors"
          @click="composerOpen = true"
        >
          Yorum ekle…
        </button>

        <div v-if="composerOpen" class="mt-2 flex justify-end gap-2">
          <button type="button" class="btn-ghost" @click="closeComposer">Vazgeç</button>
          <button
            type="button"
            class="btn-primary"
            :disabled="!hasCommentText || sending"
            @click="submitComment"
          >{{ sending ? 'Gönderiliyor…' : 'Yorum Yap' }}</button>
        </div>
      </div>
    </div>

    <!-- Zaman çizgisi -->
    <div v-if="loadingHistory && tab !== 'comments'" class="flex justify-center py-4">
      <svg class="animate-spin w-5 h-5 text-blue-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
      </svg>
    </div>

    <ul v-else-if="visibleFeed.length" class="space-y-3">
      <li v-for="item in visibleFeed" :key="item.key" class="flex items-start gap-2.5">
        <div v-if="item.kind === 'comment'" class="avatar avatar--user">{{ initials(item.author) }}</div>
        <div v-else class="avatar avatar--event">{{ fieldIcon(item.field) }}</div>

        <div class="flex-1 min-w-0">
          <!-- Yorum -->
          <template v-if="item.kind === 'comment'">
            <div class="flex flex-wrap items-baseline gap-x-2">
              <span class="text-sm font-semibold text-gray-800">{{ name(item.author) }}</span>
              <span class="text-[11px] text-gray-400">{{ relative(item.at) }}</span>
            </div>
            <div class="mt-1 rounded-lg bg-gray-50 border border-gray-100 px-3 py-2 text-sm text-gray-700">
              <RichContentViewer :content="item.text" />
            </div>
          </template>

          <!-- Alan değişikliği -->
          <template v-else>
            <p class="text-sm text-gray-600">
              <span class="font-semibold text-gray-800">{{ name(item.author) }}</span>
              <span class="text-gray-500"> {{ fieldLabel(item.field) }} güncelledi</span>
              <span class="text-[11px] text-gray-400"> · {{ relative(item.at) }}</span>
            </p>
            <p v-if="item.oldValue || item.newValue" class="mt-1 flex items-center gap-1.5 flex-wrap text-[11px]">
              <span v-if="item.oldValue" class="chip chip--old">{{ item.oldValue }}</span>
              <svg v-if="item.oldValue && item.newValue" class="w-3 h-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
              <span v-if="item.newValue" class="chip chip--new">{{ item.newValue }}</span>
            </p>
          </template>
        </div>
      </li>
    </ul>

    <p v-else class="text-sm text-gray-400 py-3">{{ emptyText }}</p>
  </TaskPanel>
</template>

<script setup>
/**
 * Aktivite paneli — yorumlar ve alan değişiklikleri tek zaman çizgisinde.
 *
 * Tarihçe sunucudan burada çekilir; görev güncellendikçe (updatedAt değişince)
 * tazelenir, böylece kullanıcı bir alanı değiştirdiğinde kaydı hemen görür.
 */
import { ref, computed, watch, onMounted } from 'vue'
import TaskPanel from '../TaskPanel.vue'
import TiptapEditor from '../../docs/TiptapEditor.vue'
import RichContentViewer from '../RichContentViewer.vue'
import { addComment, getHistory, uploadAttachment } from '../../../api/WorkApi.js'
import { useAuth } from '../../../composables/useAuth.js'
import { getInitials, displayName, formatRelativeTime } from '../../../utils/taskFormat.js'
import { taskPanelProps, taskPanelEmits } from './panelProps.js'

const props = defineProps(taskPanelProps)
const emit = defineEmits(taskPanelEmits)

const { user } = useAuth()

const tab = ref('comments')
const newestFirst = ref(true)
const composerOpen = ref(false)
const newComment = ref('')
const sending = ref(false)
const history = ref([])
const loadingHistory = ref(false)

const comments = computed(() => props.task?.comments || [])
const myInitials = computed(() => getInitials(user.value?.email || localStorage.getItem('user')))
const hasCommentText = computed(() => {
  const text = newComment.value.trim()
  return text !== '' && text !== '<p></p>'
})

const feed = computed(() => {
  const items = [
    ...comments.value.map((c, i) => ({
      key: `c-${c.id || i}`,
      kind: 'comment',
      at: c.createdAt || c.timestamp,
      author: c.author,
      text: c.text,
    })),
    ...history.value.map((h, i) => ({
      key: `h-${h.id || i}`,
      kind: 'history',
      at: h.changedAt,
      author: h.changedBy,
      field: h.field,
      oldValue: h.oldValue,
      newValue: h.newValue,
    })),
  ]
  const dir = newestFirst.value ? -1 : 1
  return items.sort((a, b) => (new Date(a.at || 0) - new Date(b.at || 0)) * dir)
})

const visibleFeed = computed(() => {
  if (tab.value === 'comments') return feed.value.filter(i => i.kind === 'comment')
  if (tab.value === 'history') return feed.value.filter(i => i.kind === 'history')
  return feed.value
})

const emptyText = computed(() => ({
  comments: 'Henüz yorum yok — ilk yorumu siz yazın.',
  history: 'Henüz değişiklik kaydı yok.',
  all: 'Bu görevde henüz bir hareket yok.',
}[tab.value]))

onMounted(loadHistory)
// Görev güncellendikçe tarihçe tazelenir; anahtar birleşik string olduğu için
// yalnız gerçekten değiştiğinde istek atılır
watch(() => `${props.task?.id}|${props.task?.updatedAt}`, loadHistory)

async function loadHistory() {
  if (!props.teamId || !props.task?.id) return
  loadingHistory.value = true
  try {
    history.value = await getHistory(props.teamId, props.task.id)
  } catch (e) {
    console.error('Tarihçe yüklenemedi:', e)
    history.value = []
  } finally {
    loadingHistory.value = false
  }
}

function closeComposer() {
  composerOpen.value = false
  newComment.value = ''
}

async function submitComment() {
  if (!hasCommentText.value || sending.value) return
  if (!props.teamId || !props.task?.id) return
  sending.value = true
  try {
    const updated = await addComment(props.teamId, props.task.id, newComment.value.trim())
    emit('patch', { comments: updated.comments || comments.value })
    closeComposer()
  } catch (e) {
    console.error('Yorum eklenemedi:', e)
  } finally {
    sending.value = false
  }
}

async function uploadHandler(file) {
  if (!props.teamId || !props.task?.id) return null
  const result = await uploadAttachment(props.teamId, props.task.id, file)
  return { downloadUrl: result.downloadUrl, fileName: result.fileName || file.name }
}

const fieldLabels = {
  status: 'Durumu',
  priority: 'Önceliği',
  assignee: 'Sorumluyu',
  title: 'Başlığı',
  description: 'Açıklamayı',
  sprint: 'Sprint\'i',
  branch: 'Branch\'i',
  ciDeploy: 'Deploy\'u',
  releaseId: 'Sürümü',
  storyPoints: 'Story point\'i',
}

const fieldIcons = {
  status: '🔄', priority: '⚡', assignee: '👤', title: '✏️', description: '📝',
  sprint: '🏃', branch: '🌿', ciDeploy: '🚀', releaseId: '📦', storyPoints: '🔢',
}

const fieldLabel = field => fieldLabels[field] || field
const fieldIcon = field => fieldIcons[field] || '📋'
const initials = getInitials
const name = displayName
const relative = formatRelativeTime
</script>

<style scoped>
.tabs {
  @apply flex items-center gap-1 border-b border-gray-100 mb-3 -mt-1;
}
.tab {
  @apply flex items-center gap-1.5 px-2.5 py-1.5 -mb-px border-b-2 border-transparent text-xs font-semibold text-gray-400 hover:text-gray-600 transition-colors;
}
.tab--active {
  @apply border-blue-500 text-blue-600;
}
.tab-count {
  @apply inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full bg-gray-100 text-[10px] font-semibold text-gray-500;
}
.tab--active .tab-count {
  @apply bg-blue-50 text-blue-600;
}

.avatar {
  @apply flex-shrink-0 w-7 h-7 rounded-full flex items-center justify-center text-[10px] font-bold;
}
.avatar--me { @apply bg-blue-600 text-white; }
.avatar--user { @apply bg-gray-500 text-white; }
.avatar--event { @apply bg-gray-100 text-xs; }

.chip {
  @apply px-1.5 py-0.5 rounded max-w-[220px] truncate;
}
.chip--old { @apply bg-gray-100 text-gray-500 line-through; }
.chip--new { @apply bg-green-50 text-green-700; }

.panel-action {
  @apply inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold text-gray-500 bg-gray-100 hover:bg-gray-200 hover:text-gray-700 transition-colors;
}
.btn-primary {
  @apply px-3 py-1.5 rounded-lg bg-blue-600 text-white text-xs font-semibold hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors;
}
.btn-ghost {
  @apply px-3 py-1.5 rounded-lg bg-gray-100 text-gray-600 text-xs font-semibold hover:bg-gray-200 transition-colors;
}
</style>
