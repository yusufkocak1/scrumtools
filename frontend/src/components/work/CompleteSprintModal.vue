<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" @click.self="$emit('close')">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-md max-h-[90vh] overflow-y-auto p-6">
      <h2 class="text-lg font-semibold text-gray-900">Sprinti Kapat</h2>
      <p class="text-sm text-gray-500 mt-1 mb-4 truncate">{{ sprint.name }}</p>

      <!-- Kapanış özeti -->
      <div class="rounded-lg border border-gray-200 bg-gray-50 p-3 mb-5">
        <div class="flex items-center justify-between text-sm">
          <span class="text-gray-600">Tamamlanan</span>
          <span class="font-semibold text-green-700">{{ completedTasks.length }} iş</span>
        </div>
        <div class="flex items-center justify-between text-sm mt-1">
          <span class="text-gray-600">Tamamlanmayan</span>
          <span class="font-semibold" :class="incompleteTasks.length ? 'text-amber-700' : 'text-gray-400'">
            {{ incompleteTasks.length }} iş
          </span>
        </div>
        <div class="mt-2.5 h-1.5 w-full rounded-full bg-gray-200 overflow-hidden">
          <div class="h-full rounded-full bg-green-500 transition-all" :style="{ width: completionPercent + '%' }"></div>
        </div>
        <!-- Sprint takımın zaman kutusu; içinde başka projelerin işleri de olabilir.
             Ekranda proje filtresi açıkken sayılar o projeye ait olanları gösterir. -->
        <p v-if="projectScoped" class="text-[11px] text-gray-400 mt-2">
          Sayılar aktif proje filtresine göre. Seçtiğiniz işlem sprintteki diğer
          projelerin işlerine de uygulanır.
        </p>
      </div>

      <!-- Yarım kalan işlerin akıbeti -->
      <div v-if="incompleteTasks.length" class="space-y-2 mb-5">
        <p class="text-sm font-medium text-gray-700">
          Tamamlanmayan {{ incompleteTasks.length }} iş ne olsun?
        </p>

        <label
          v-for="opt in actionOptions"
          :key="opt.value"
          class="flex items-start gap-3 rounded-lg border p-3 cursor-pointer transition-colors"
          :class="action === opt.value ? 'border-blue-400 bg-blue-50/60' : 'border-gray-200 hover:bg-gray-50'"
        >
          <input v-model="action" type="radio" :value="opt.value" class="mt-0.5 text-blue-600 focus:ring-blue-500" />
          <span class="min-w-0">
            <span class="block text-sm font-medium text-gray-800">{{ opt.label }}</span>
            <span class="block text-xs text-gray-500 mt-0.5">{{ opt.hint }}</span>
          </span>
        </label>

        <!-- Hedef sprint seçimi (yalnızca taşıma seçildiğinde) -->
        <div v-if="action === 'MOVE'" class="pl-3">
          <select
            v-model="targetSprintId"
            class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
          >
            <option value="">Sprint seçin…</option>
            <option v-for="s in targetSprints" :key="s.id" :value="s.id">
              {{ s.name }}{{ s.status === 'open' ? ' (aktif)' : '' }}
            </option>
          </select>
          <p v-if="!targetSprints.length" class="text-xs text-amber-600 mt-1">
            Taşınabilecek açık bir sprint yok — önce yeni bir sprint oluşturun.
          </p>
        </div>
      </div>

      <p v-else class="text-sm text-gray-600 mb-5">
        {{ relevantTasks.length
          ? 'Sprintteki tüm işler tamamlanmış. Sprint kapatıldığında backlog ekranından kaldırılacak.'
          : 'Sprintte iş yok. Kapatıldığında backlog ekranından kaldırılacak.' }}
      </p>

      <p v-if="error" class="text-sm text-red-600 mb-3">{{ error }}</p>

      <div class="flex justify-end gap-3">
        <button class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900" @click="$emit('close')">
          İptal
        </button>
        <button
          class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
          :disabled="submitting || !canSubmit"
          @click="submit"
        >
          {{ submitting ? 'Kapatılıyor…' : 'Sprinti Kapat' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { completeSprint } from '../../api/WorkApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  sprint: { type: Object, required: true },
  /** Sprintteki görevler (alt görevler dahil) — özet sayıları buradan hesaplanır */
  tasks: { type: Array, default: () => [] },
  /** Takımın tüm sprintleri — taşıma hedefi listesi buradan süzülür */
  sprints: { type: Array, default: () => [] },
  /** Çağıran ekranda proje filtresi açık mı — özet sayıları o kadarını kapsar */
  projectScoped: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'completed'])

// Backend'deki TaskService.isDoneStatus ile aynı tanım — özet sayıların
// sunucunun uyguladığı ayrımla birebir örtüşmesi için.
const DONE_STATUSES = ['done', 'closed', 'fixed', 'verified']

const action = ref('BACKLOG')
const targetSprintId = ref('')
const submitting = ref(false)
const error = ref('')

const actionOptions = [
  { value: 'BACKLOG', label: 'Backlog\'a geri gönder', hint: 'Sprint bağı kaldırılır, işler backlog\'un başında bekler.' },
  { value: 'MOVE', label: 'Başka bir sprinte taşı', hint: 'Bir sonraki sprintin kapsamına eklenir.' },
  { value: 'COMPLETE', label: 'Tümünü tamamlandı say', hint: 'Yarım kalan işler "Done" olarak işaretlenir.' },
  { value: 'KEEP', label: 'Bu sprintte kalsın', hint: 'İşler kapanan sprintte kalır; backlog ekranında görünmez.' },
]

// İptal edilen işler ne tamamlanmış ne yarım sayılır — backend de onları dışarıda bırakır.
const relevantTasks = computed(() =>
  props.tasks.filter(t => (t.status || '').toLowerCase() !== 'cancelled')
)

const completedTasks = computed(() =>
  relevantTasks.value.filter(t => DONE_STATUSES.includes((t.status || '').toLowerCase()))
)

const incompleteTasks = computed(() =>
  relevantTasks.value.filter(t => !DONE_STATUSES.includes((t.status || '').toLowerCase()))
)

const completionPercent = computed(() => {
  if (!relevantTasks.value.length) return 0
  return Math.round((completedTasks.value.length / relevantTasks.value.length) * 100)
})

/** Kapanmış sprintler ve sprintin kendisi hedef olamaz. */
const targetSprints = computed(() =>
  props.sprints.filter(s => s.id !== props.sprint.id && s.status !== 'done')
)

const canSubmit = computed(() =>
  !(incompleteTasks.value.length && action.value === 'MOVE' && !targetSprintId.value)
)

async function submit() {
  if (submitting.value || !canSubmit.value) return
  submitting.value = true
  error.value = ''
  try {
    const result = await completeSprint(props.teamId, props.sprint.id, {
      // Yarım iş yoksa aksiyonun bir karşılığı yok; sunucuya da anlamsız bir
      // taşıma göndermemek için sprinti olduğu gibi kapatıyoruz.
      incompleteAction: incompleteTasks.value.length ? action.value : 'KEEP',
      targetSprintId: action.value === 'MOVE' ? targetSprintId.value : null,
    })
    emit('completed', result)
    emit('close')
  } catch (e) {
    error.value = e?.response?.data?.error || 'Sprint kapatılamadı.'
  } finally {
    submitting.value = false
  }
}
</script>
