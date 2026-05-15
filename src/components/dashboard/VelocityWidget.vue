<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <h3 class="text-sm font-semibold text-gray-700">Sprint Velocity</h3>

    <div v-if="loading" class="flex-1 flex items-center justify-center py-8">
      <svg class="animate-spin w-6 h-6 text-purple-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
    </div>

    <div v-else-if="chartData" class="relative h-52">
      <Bar :data="chartData" :options="chartOptions" />
    </div>

    <div v-else class="text-center text-xs text-gray-400 py-8">Veri yok.</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS, CategoryScale, LinearScale, BarElement,
  Title, Tooltip, Legend
} from 'chart.js'
import { getVelocity } from '../../api/ReportApi.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

const props = defineProps({ teamId: { type: String, required: true } })

const data = ref([])
const loading = ref(false)

onMounted(async () => {
  if (!props.teamId) return
  loading.value = true
  try { data.value = await getVelocity(props.teamId) }
  catch (e) { console.error(e) }
  finally { loading.value = false }
})

const chartData = computed(() => {
  if (!data.value?.length) return null
  // Son 10 sprint'i göster
  const items = data.value.slice(-10)
  return {
    labels: items.map(s => s.sprintName),
    datasets: [
      {
        label: 'Taahhüt',
        data: items.map(s => s.committed),
        backgroundColor: 'rgba(148,163,184,0.7)',
        borderRadius: 4,
      },
      {
        label: 'Tamamlanan',
        data: items.map(s => s.completed),
        backgroundColor: 'rgba(139,92,246,0.8)',
        borderRadius: 4,
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

