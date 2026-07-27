<template>
  <!-- Kart kabuğu yok: bu liste TaskRelationsPanel'in "İlişkili İşler" sekmesinde yaşar -->
  <div>
    <!-- İlişki türüne göre gruplanır: "blocks" ve "relates to" satırları karışınca
         hangi işin neyi engellediği okunmuyordu -->
    <div v-if="links.length" class="space-y-3">
      <div v-for="group in groupedLinks" :key="group.label">
        <p class="text-[11px] font-semibold text-gray-400 uppercase tracking-wide mb-1">{{ group.label }}</p>
        <ul class="-mx-2">
          <li
            v-for="link in group.items"
            :key="link.id"
            class="flex items-center gap-2.5 px-2 py-1.5 rounded-lg hover:bg-gray-50 group"
          >
            <button
              type="button"
              class="flex items-center gap-2 flex-1 min-w-0 text-left"
              @click="$emit('open', link.customId)"
            >
              <span class="font-mono text-[11px] text-gray-400 flex-shrink-0">{{ link.customId }}</span>
              <span class="text-sm text-gray-700 truncate group-hover:text-blue-600 transition-colors">{{ link.title }}</span>
            </button>
            <StatusBadge v-if="link.status" :status="link.status" class="flex-shrink-0" />
            <button
              type="button"
              class="opacity-0 group-hover:opacity-100 text-gray-300 hover:text-red-500 transition-all"
              title="İlişkiyi kaldır"
              @click="removeLink(link.id)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </li>
        </ul>
      </div>
    </div>

    <p v-else-if="!adding" class="text-sm text-gray-400 py-3">
      Bu görev başka bir işle ilişkilendirilmemiş.
    </p>

    <!-- Ekleme formu -->
    <div v-if="adding" class="mt-3 space-y-2" :class="{ 'border-t border-gray-100 pt-3': links.length }">
      <div class="flex flex-col sm:flex-row gap-2">
        <select
          v-model="newLinkType"
          class="text-sm border border-gray-300 rounded-lg px-2 py-1.5 focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400"
        >
          <option v-for="(label, type) in forwardMap" :key="type" :value="type">{{ label }}</option>
        </select>
        <TaskPickerInput
          v-model="targetTaskId"
          :team-id="teamId"
          :exclude-task-id="taskId"
          placeholder="Hedef görevi ara…"
          class="flex-1"
        />
      </div>
      <div class="flex gap-2">
        <button
          type="button"
          :disabled="!targetTaskId || loading"
          class="px-3 py-1.5 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-40 transition-colors"
          @click="addLink"
        >Ekle</button>
        <button type="button" class="px-2 py-1.5 text-gray-500 hover:text-gray-700 text-sm" @click="close">İptal</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import TaskPickerInput from '@/components/work/TaskPickerInput.vue'
import StatusBadge from '@/components/workflow/StatusBadge.vue'
import { createLink, deleteLink } from '@/api/WorkApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  taskId: { type: String, required: true },
  links: { type: Array, default: () => [] },
  /** Ekleme formu panel başlığındaki butondan açılır */
  adding: { type: Boolean, default: false },
})

const emit = defineEmits(['update', 'open', 'update:adding'])

const newLinkType = ref('RELATES_TO')
const targetTaskId = ref(null)
const loading = ref(false)

watch(() => props.adding, open => { if (!open) targetTaskId.value = null })

/**
 * İlişki etiketleri iki yönlü tutulur: sunucu yalnız İngilizce ("is blocked by")
 * etiket döndürüyor, arayüz ise ilişkinin hangi ucundan bakıldığına göre Türkçe
 * karşılığını gösterir.
 */
const forwardMap = {
  BLOCKS: 'Engelliyor',
  IS_BLOCKED_BY: 'Engelleniyor',
  RELATES_TO: 'İlgili',
  DUPLICATES: 'Kopyası',
  IS_DUPLICATED_BY: 'Kopyalandığı',
  CAUSES: 'Sebep oluyor',
  IS_CAUSED_BY: 'Sebebi',
  CLONES: 'Klonu',
  IS_CLONED_FROM: 'Klonlandığı',
}

const inverseMap = {
  BLOCKS: 'Engelleniyor',
  IS_BLOCKED_BY: 'Engelliyor',
  RELATES_TO: 'İlgili',
  DUPLICATES: 'Kopyalandığı',
  IS_DUPLICATED_BY: 'Kopyası',
  CAUSES: 'Sebebi',
  IS_CAUSED_BY: 'Sebep oluyor',
  CLONES: 'Klonlandığı',
  IS_CLONED_FROM: 'Klonu',
}

function isSource(link) {
  return link.sourceTaskId === props.taskId
}

/** İlişkinin bu görevden bakınca gösterdiği karşı görev + okunur etiketi. */
const groupedLinks = computed(() => {
  const groups = new Map()
  for (const link of props.links) {
    const source = isSource(link)
    const label = (source ? forwardMap[link.linkType] : inverseMap[link.linkType]) || link.linkTypeLabel || link.linkType
    const item = {
      id: link.id,
      customId: source ? link.targetTaskCustomId : link.sourceTaskCustomId,
      title: source ? link.targetTaskTitle : link.sourceTaskTitle,
      status: source ? link.targetTaskStatus : link.sourceTaskStatus,
    }
    if (!groups.has(label)) groups.set(label, [])
    groups.get(label).push(item)
  }
  return [...groups.entries()].map(([label, items]) => ({ label, items }))
})

function close() {
  emit('update:adding', false)
}

async function addLink() {
  if (!targetTaskId.value || loading.value) return
  loading.value = true
  try {
    await createLink(props.teamId, props.taskId, targetTaskId.value, newLinkType.value)
    targetTaskId.value = null
    close()
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
