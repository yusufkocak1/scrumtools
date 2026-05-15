<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <h3 class="text-sm font-semibold text-gray-700">Vadesi Geçmiş Görevler</h3>

    <div v-if="loading" class="flex items-center justify-center py-8">
      <svg class="animate-spin w-6 h-6 text-purple-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
    </div>

    <div v-else-if="tasks.length === 0"
         class="text-center text-sm text-green-600 py-6 bg-green-50 rounded-lg">
      🎉 Vadesi geçmiş görev yok!
    </div>

    <div v-else class="overflow-auto max-h-64">
      <table class="w-full text-xs">
        <thead class="sticky top-0 bg-gray-50 text-gray-500">
          <tr>
            <th class="text-left py-2 px-2 font-medium">ID</th>
            <th class="text-left py-2 px-2 font-medium">Başlık</th>
            <th class="text-left py-2 px-2 font-medium">Öncelik</th>
            <th class="text-left py-2 px-2 font-medium">Atanan</th>
            <th class="text-left py-2 px-2 font-medium">Vade</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.id"
              class="border-t border-gray-100 hover:bg-red-50 cursor-pointer transition-colors"
              @click="emit('task-click', t)">
            <td class="py-2 px-2 text-purple-600 font-mono font-medium">{{ t.customId }}</td>
            <td class="py-2 px-2 text-gray-700 truncate max-w-[160px]">{{ t.title }}</td>
            <td class="py-2 px-2">
              <span class="px-1.5 py-0.5 rounded-full text-xs font-medium" :class="priorityColor(t.priority)">
                {{ t.priority }}
              </span>
            </td>
            <td class="py-2 px-2 text-gray-500 truncate max-w-[100px]">{{ t.assignee || '—' }}</td>
            <td class="py-2 px-2 text-red-600 font-medium">{{ t.dueDate }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOverdueTasks } from '../../api/ReportApi.js'

const props = defineProps({ teamId: { type: String, required: true } })
const emit = defineEmits(['task-click'])

const tasks = ref([])
const loading = ref(false)

onMounted(async () => {
  if (!props.teamId) return
  loading.value = true
  try { tasks.value = await getOverdueTasks(props.teamId) }
  catch (e) { console.error(e) }
  finally { loading.value = false }
})

const priorityColor = (p) => {
  const m = { Critical: 'bg-red-100 text-red-700', High: 'bg-orange-100 text-orange-700', Medium: 'bg-yellow-100 text-yellow-700', Low: 'bg-gray-100 text-gray-600' }
  return m[p] || 'bg-gray-100 text-gray-600'
}
</script>

