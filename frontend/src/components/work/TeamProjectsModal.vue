<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="$emit('close')">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-lg mx-4 max-h-[85vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">Takım Projeleri</h2>
          <p class="text-xs text-gray-400 mt-0.5">
            Takımın üzerinde çalıştığı projeler — görev ve board görünümleri bu listeden beslenir.
          </p>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 transition shrink-0 ml-4">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Proje listesi -->
      <div class="flex-1 overflow-y-auto px-6 py-4 space-y-2">
        <div v-if="loading" class="text-center py-10 text-gray-400 text-sm">
          Projeler yükleniyor…
        </div>

        <div v-else-if="orgProjects.length === 0" class="text-center py-10 text-gray-400 text-sm">
          Organizasyonda erişebildiğiniz bir proje bulunamadı. Önce bir proje oluşturun.
        </div>

        <div
          v-for="p in orgProjects"
          :key="p.id"
          class="flex items-center justify-between gap-3 border border-gray-200 rounded-lg px-4 py-3 hover:border-gray-300 transition"
        >
          <div class="flex items-center gap-3 min-w-0">
            <span
              class="w-2.5 h-2.5 rounded-full shrink-0"
              :style="{ backgroundColor: p.color || '#3B82F6' }"
            ></span>
            <div class="min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-sm font-medium text-gray-900 truncate">{{ p.name }}</span>
                <span
                  v-if="isPrimary(p.id)"
                  class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-green-100 text-green-700 shrink-0"
                >
                  Birincil
                </span>
              </div>
              <span class="text-xs text-gray-400">{{ p.key }}</span>
            </div>
          </div>

          <button
            v-if="isLinked(p.id)"
            @click="detach(p)"
            :disabled="busyId === p.id"
            class="shrink-0 text-xs text-red-500 hover:text-red-700 px-2 py-1 rounded border border-red-200 hover:border-red-400 transition disabled:opacity-50"
          >
            {{ busyId === p.id ? '…' : 'Çıkar' }}
          </button>
          <button
            v-else
            @click="attach(p)"
            :disabled="busyId === p.id"
            class="shrink-0 text-xs text-blue-600 hover:text-blue-800 px-2 py-1 rounded border border-blue-200 hover:border-blue-400 transition disabled:opacity-50"
          >
            {{ busyId === p.id ? '…' : 'Ekle' }}
          </button>
        </div>

        <p v-if="error" class="text-xs text-red-500 pt-1">{{ error }}</p>
      </div>

      <div class="px-6 py-4 border-t border-gray-200 flex justify-end">
        <button
          @click="$emit('close')"
          class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700"
        >
          Kapat
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Takımın çalıştığı projeleri yönetir. Backend baştan çoklu projeyi destekliyordu
 * (team_projects) ama arayüzde takıma ikinci bir proje ekleyecek hiçbir yer yoktu:
 * takım tek projesiyle kalıyor, proje seçici ile görev formu tek seçenek gösteriyordu.
 */
import { ref, onMounted } from 'vue'
import ProjectApi from '../../api/ProjectApi.js'
import { addTeamProject, removeTeamProject } from '../../api/TeamApi.js'

const props = defineProps({
  teamId:         { type: String, required: true },
  organizationId: { type: String, default: null },
  /** Takıma bağlı projeler (TeamResponse.projects) */
  teamProjects:   { type: Array, default: () => [] },
})

const emit = defineEmits(['close', 'changed'])

const orgProjects = ref([])
const linkedIds   = ref(new Set(props.teamProjects.map(p => p.id)))
const primaryId   = ref(props.teamProjects.find(p => p.primary)?.id ?? null)
const loading     = ref(false)
const busyId      = ref(null)
const error       = ref(null)

function isLinked(id)  { return linkedIds.value.has(id) }
function isPrimary(id) { return primaryId.value === id }

/** Takım kaydı değiştiğinde bağlı proje kümesini response'tan tazeler. */
function syncFromTeam(team) {
  const list = team?.projects ?? []
  linkedIds.value = new Set(list.map(p => p.id))
  primaryId.value = list.find(p => p.primary)?.id ?? null
  emit('changed', list)
}

async function loadOrgProjects() {
  if (!props.organizationId) return
  loading.value = true
  try {
    const { data } = await ProjectApi.getByOrg(props.organizationId)
    // ProjectResponse.id UUID olarak geliyor; TeamProjectResponse.id string —
    // karşılaştırmaların tutması için tek tipe indiriyoruz.
    orgProjects.value = (data || []).map(p => ({ ...p, id: String(p.id) }))
  } catch (e) {
    error.value = e?.response?.data?.error || 'Projeler yüklenemedi.'
  } finally {
    loading.value = false
  }
}

async function attach(project) {
  busyId.value = project.id
  error.value = null
  try {
    syncFromTeam(await addTeamProject(props.teamId, project.id))
  } catch (e) {
    error.value = e?.response?.data?.error || 'Proje takıma eklenemedi.'
  } finally {
    busyId.value = null
  }
}

async function detach(project) {
  busyId.value = project.id
  error.value = null
  try {
    // Backend o projede görevi olan takımı ayırmayı reddeder; mesajı olduğu gibi gösteriyoruz.
    syncFromTeam(await removeTeamProject(props.teamId, project.id))
  } catch (e) {
    error.value = e?.response?.data?.error || 'Proje takımdan çıkarılamadı.'
  } finally {
    busyId.value = null
  }
}

onMounted(loadOrgProjects)
</script>
