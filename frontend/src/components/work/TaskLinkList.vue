<template>
  <div class="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden transition-shadow duration-200 hover:shadow-md">
    <div class="h-1 w-full bg-gradient-to-r from-indigo-400 to-purple-500"></div>
    <div class="p-5">
    <div class="flex items-center justify-between mb-3">
      <h3 class="text-base font-semibold text-gray-800 flex items-center gap-2">
        <svg class="w-5 h-5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
        </svg>
        İlişkili İşler
        <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-indigo-100 text-indigo-700">{{ links.length }}</span>
      </h3>
      <button
        @click="showAddForm = !showAddForm"
        class="inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs font-medium text-indigo-600 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 transition-all"
      >
        <svg class="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        İlişki Ekle
      </button>
    </div>

    <ul class="space-y-2">
      <li
        v-for="link in links"
        :key="link.id"
        class="flex items-center gap-2 group"
      >
        <span class="text-xs text-gray-400 italic min-w-[110px]">
          {{ isSource(link) ? link.linkTypeLabel : getInverseLabel(link.linkType) }}
        </span>
        <span
          class="text-sm text-blue-600 hover:underline cursor-pointer flex-1"
          @click="$emit('open', isSource(link) ? link.targetTaskId : link.sourceTaskId)"
        >
          <span class="font-mono text-xs text-gray-500 mr-1">
            {{ isSource(link) ? link.targetTaskCustomId : link.sourceTaskCustomId }}
          </span>
          {{ isSource(link) ? link.targetTaskTitle : link.sourceTaskTitle }}
        </span>
        <button
          @click="removeLink(link.id)"
          class="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500 transition-all"
          title="İlişkiyi kaldır"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </li>
    </ul>

    <p v-if="links.length === 0" class="text-sm text-gray-400 text-center py-2">
      Henüz ilişkilendirilmiş iş yok
    </p>

    <!-- Add form -->
    <div v-if="showAddForm" class="mt-3 space-y-2 border-t border-gray-100 pt-3">
      <div class="flex gap-2">
        <select
          v-model="newLinkType"
          class="text-sm border border-gray-300 rounded-lg px-2 py-1.5 focus:outline-none focus:border-blue-500"
        >
          <option value="BLOCKS">blocks</option>
          <option value="IS_BLOCKED_BY">is blocked by</option>
          <option value="RELATES_TO">relates to</option>
          <option value="DUPLICATES">duplicates</option>
          <option value="IS_DUPLICATED_BY">is duplicated by</option>
          <option value="CLONES">clones</option>
          <option value="CAUSES">causes</option>
        </select>
        <input
          v-model="targetTaskId"
          placeholder="Hedef Task ID..."
          class="flex-1 text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-blue-500"
        />
      </div>
      <div class="flex gap-2">
        <button
          @click="addLink"
          :disabled="!targetTaskId.trim() || loading"
          class="px-3 py-1.5 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          Ekle
        </button>
        <button
          @click="showAddForm = false"
          class="px-3 py-1.5 text-gray-500 hover:text-gray-700 text-sm"
        >
          İptal
        </button>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { createLink, deleteLink } from '@/api/WorkApi.js'

const props = defineProps({
  teamId:  { type: String, required: true },
  taskId:  { type: String, required: true },
  links:   { type: Array, default: () => [] },
})

const emit = defineEmits(['update', 'open'])

const showAddForm = ref(false)
const newLinkType = ref('RELATES_TO')
const targetTaskId = ref('')
const loading = ref(false)

function isSource(link) {
  return link.sourceTaskId === props.taskId
}

const inverseMap = {
  BLOCKS: 'is blocked by',
  IS_BLOCKED_BY: 'blocks',
  RELATES_TO: 'relates to',
  DUPLICATES: 'is duplicated by',
  IS_DUPLICATED_BY: 'duplicates',
  CAUSES: 'is caused by',
  IS_CAUSED_BY: 'causes',
  CLONES: 'is cloned from',
  IS_CLONED_FROM: 'clones',
}

function getInverseLabel(linkType) {
  return inverseMap[linkType] || linkType
}

async function addLink() {
  if (!targetTaskId.value.trim() || loading.value) return
  loading.value = true
  try {
    await createLink(props.teamId, props.taskId, targetTaskId.value.trim(), newLinkType.value)
    targetTaskId.value = ''
    showAddForm.value = false
    emit('update')
  } catch (e) {
    // Hata interceptor tarafından otomatik gösterilir
  } finally {
    loading.value = false
  }
}

async function removeLink(linkId) {
  if (!confirm('Bu ilişki kaldırılsın mı?')) return
  try {
    await deleteLink(props.teamId, props.taskId, linkId)
    emit('update')
  } catch (e) {
    console.error(e)
  }
}
</script>

