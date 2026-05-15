<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-gray-700">Oluşturulan vs Çözülen</h3>
      <select v-model="days" @change="load"
              class="text-xs border border-gray-200 rounded-lg px-2 py-1 focus:outline-none">
        <option :value="7">Son 7 gün</option>
        <option :value="14">Son 14 gün</option>
        <option :value="30">Son 30 gün</option>
        <option :value="90">Son 90 gün</option>
      </select>
    </div>

    <div v-if="loading" class="flex-1 flex items-center justify-center py-8">
      <svg class="animate-spin w-6 h-6 text-purple-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
    </div>

    <div v-else-if="chartData" class="relative h-52">
      <Line :data="chartData" :options="chartOptions" />
    </div>

    <div v-else class="text-center text-xs text-gray-400 py-8">Veri yok.</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement,
  LineElement, Title, Tooltip, Legend, Filler
} from 'chart.js'
import { getCreatedVsResolved } from '../../api/ReportApi.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

const props = defineProps({ teamId: { type: String, required: true } })

const data = ref([])
const days = ref(30)
const loading = ref(false)

onMounted(load)

async function load() {
  if (!props.teamId) return
  loading.value = true
  try { data.value = await getCreatedVsResolved(props.teamId, days.value) }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}

const chartData = computed(() => {
  if (!data.value?.length) return null
  return {
    labels: data.value.map(d => d.date),
    datasets: [
      {
        label: 'Oluşturulan',
        data: data.value.map(d => d.created),
        borderColor: '#f59e0b',
        backgroundColor: 'rgba(245,158,11,0.08)',
        tension: 0.3,
        fill: true,
        pointRadius: 3,
      },
      {
        label: 'Çözülen',
        data: data.value.map(d => d.resolved),
        borderColor: '#22c55e',
        backgroundColor: 'rgba(34,197,94,0.08)',
        tension: 0.3,
        fill: true,
        pointRadius: 3,
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
    y: { beginAtZero: true, ticks: { stepSize: 1, font: { size: 10 } } }
  }
}
</script>

