<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-gray-700">Sprint Burndown</h3>
      <!-- Sprint seçici -->
      <select v-model="selectedSprintId" @change="load"
              class="text-xs border border-gray-200 rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-purple-300">
        <option v-for="s in sprints" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>
    </div>

    <div v-if="loading" class="flex-1 flex items-center justify-center py-8">
      <svg class="animate-spin w-6 h-6 text-purple-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
    </div>

    <div v-else-if="!selectedSprintId" class="text-center text-xs text-gray-400 py-8">
      Henüz sprint bulunmuyor.
    </div>

    <div v-else-if="chartData" class="relative h-52">
      <Line :data="chartData" :options="chartOptions" />
    </div>

    <div v-if="report" class="text-xs text-gray-500 text-right">
      Toplam: <strong>{{ report.totalPoints }}</strong> SP
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement,
  LineElement, Title, Tooltip, Legend, Filler
} from 'chart.js'
import { getBurndown } from '../../api/ReportApi.js'
import { getSprints } from '../../api/WorkApi.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

const props = defineProps({ teamId: { type: String, required: true } })

const sprints = ref([])
const selectedSprintId = ref(null)
const report = ref(null)
const loading = ref(false)

onMounted(async () => {
  if (!props.teamId) return
  try {
    sprints.value = await getSprints(props.teamId)
    // Aktif sprint varsa onu seç, yoksa son sprint
    const active = sprints.value.find(s => s.status === 'open')
    selectedSprintId.value = active?.id || sprints.value[sprints.value.length - 1]?.id
    if (selectedSprintId.value) await load()
  } catch (e) { console.error(e) }
})

async function load() {
  if (!selectedSprintId.value) return
  loading.value = true
  try { report.value = await getBurndown(props.teamId, selectedSprintId.value) }
  catch (e) { console.error('Burndown yüklenemedi', e) }
  finally { loading.value = false }
}

const chartData = computed(() => {
  if (!report.value?.points) return null
  const pts = report.value.points
  return {
    labels: pts.map(p => p.date),
    datasets: [
      {
        label: 'İdeal',
        data: pts.map(p => p.ideal),
        borderColor: '#94a3b8',
        borderDash: [5, 5],
        tension: 0.3,
        fill: false,
        pointRadius: 0,
      },
      {
        label: 'Gerçek',
        data: pts.map(p => p.actual >= 0 ? p.actual : null),
        borderColor: '#8b5cf6',
        backgroundColor: 'rgba(139,92,246,0.08)',
        tension: 0.3,
        fill: true,
        pointRadius: 3,
        spanGaps: false,
      }
    ]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { position: 'bottom', labels: { font: { size: 11 } } } },
  scales: {
    x: { ticks: { font: { size: 10 }, maxRotation: 45 } },
    y: { beginAtZero: true, ticks: { font: { size: 10 } } }
  }
}
</script>

