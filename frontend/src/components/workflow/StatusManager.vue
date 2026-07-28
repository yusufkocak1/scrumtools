<template>
  <div class="space-y-5">
    <!-- Başlık + kapsam -->
    <div class="flex items-start justify-between gap-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">Görev Durumları</h2>
        <p class="text-sm text-gray-500 mt-0.5">
          Görevlerin alabileceği durumlar. Board sütunları bu durumlara eşlenir —
          birebir aynı olmaları gerekmez.
        </p>
      </div>
      <button
        class="shrink-0 px-3 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
        :disabled="loading || isFallback"
        @click="startCreate"
      >
        + Durum Ekle
      </button>
    </div>

    <!-- Sunucudan liste alınamadı: yerel varsayılan gösteriliyor, düzenleme kapalı -->
    <div
      v-if="isFallback && !loading"
      class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
    >
      <p class="font-medium">Durum listesi sunucudan alınamadı</p>
      <p class="mt-0.5 text-red-700">
        Geçici olarak varsayılan durumlar gösteriliyor. Veriyi bozmamak için düzenleme
        devre dışı — bağlantı düzeldiğinde sayfayı yenileyin.
      </p>
    </div>

    <!-- Kapsam bilgisi -->
    <div
      v-if="projectId"
      class="flex items-start gap-3 rounded-lg border px-4 py-3 text-sm"
      :class="scope === 'PROJECT'
        ? 'border-indigo-200 bg-indigo-50 text-indigo-800'
        : 'border-gray-200 bg-gray-50 text-gray-600'"
    >
      <span class="mt-0.5">{{ scope === 'PROJECT' ? '📁' : '👥' }}</span>
      <div class="flex-1 min-w-0">
        <p v-if="scope === 'PROJECT'">
          Bu proje kendi durum setini kullanıyor. Değişiklikler yalnızca bu projeyi etkiler.
        </p>
        <template v-else>
          <p>Bu proje takımın ortak durum setini kullanıyor; düzenleme tüm takımı etkiler.</p>
          <button
            class="mt-1.5 text-xs font-medium text-indigo-600 hover:text-indigo-800 underline"
            :disabled="saving"
            @click="detachFromTeam"
          >
            Bu projeye özel durum seti oluştur
          </button>
        </template>
      </div>
    </div>

    <!-- Eşlenmemiş durum uyarısı -->
    <div
      v-if="unmapped.length > 0"
      class="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
    >
      <p class="font-medium">Tanımsız durumlar</p>
      <p class="mt-0.5 text-amber-700">
        Görevlerde kullanılan ama listede olmayan durumlar var:
        <span class="font-mono">{{ unmapped.join(', ') }}</span>.
        Bunları ekleyin ya da görevleri tanımlı bir duruma taşıyın.
      </p>
      <button
        class="mt-2 text-xs font-medium text-amber-900 underline hover:no-underline"
        @click="adoptUnmapped"
      >
        Hepsini durum listesine ekle
      </button>
    </div>

    <div v-if="loading" class="py-10 text-center text-sm text-gray-400">Yükleniyor…</div>

    <!-- Durum listesi -->
    <ul v-else class="space-y-2">
      <li
        v-for="(status, idx) in statuses"
        :key="status.id"
        class="rounded-lg border border-gray-200 bg-white transition hover:border-gray-300"
      >
        <!-- Görüntüleme -->
        <div v-if="editingId !== status.id" class="flex items-center gap-3 px-4 py-3">
          <span class="w-3 h-3 rounded-full shrink-0" :style="{ backgroundColor: status.color || '#6B7280' }"></span>

          <span class="font-medium text-gray-900 truncate">
            <span v-if="status.icon" class="mr-1">{{ status.icon }}</span>{{ status.name }}
          </span>

          <span class="text-[11px] font-medium px-2 py-0.5 rounded-full" :class="categoryClass(status.category)">
            {{ CATEGORY_LABELS[status.category] || status.category }}
          </span>

          <span v-if="status.isInitial" class="text-[11px] px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-100">
            Başlangıç
          </span>
          <span v-if="status.isCancellation" class="text-[11px] px-2 py-0.5 rounded-full bg-red-50 text-red-600 border border-red-100">
            İptal
          </span>
          <span v-else-if="status.isFinal" class="text-[11px] px-2 py-0.5 rounded-full bg-green-50 text-green-700 border border-green-100">
            Bitiş
          </span>

          <div class="ml-auto flex items-center gap-1 shrink-0">
            <button
              class="p-1.5 text-gray-400 hover:text-gray-700 disabled:opacity-30"
              :disabled="idx === 0 || saving || isFallback"
              title="Yukarı taşı"
              @click="move(idx, -1)"
            >▲</button>
            <button
              class="p-1.5 text-gray-400 hover:text-gray-700 disabled:opacity-30"
              :disabled="idx === statuses.length - 1 || saving || isFallback"
              title="Aşağı taşı"
              @click="move(idx, 1)"
            >▼</button>
            <button
              class="ml-1 px-2 py-1 text-xs text-gray-600 border border-gray-200 rounded hover:border-blue-300 hover:text-blue-600 disabled:opacity-40"
              :disabled="isFallback"
              @click="startEdit(status)"
            >Düzenle</button>
            <button
              class="px-2 py-1 text-xs text-red-500 border border-red-200 rounded hover:border-red-400 hover:text-red-700 disabled:opacity-40"
              :disabled="isFallback || status.isInitial || statuses.length <= 1"
              :title="status.isInitial ? 'Başlangıç durumu silinemez' : 'Sil'"
              @click="askDelete(status)"
            >Sil</button>
          </div>
        </div>

        <!-- Düzenleme -->
        <div v-else class="px-4 py-4 space-y-3 bg-gray-50/60">
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div class="sm:col-span-2">
              <label class="block text-xs font-medium text-gray-700 mb-1">Durum Adı</label>
              <input
                v-model="form.name"
                class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                placeholder="ör. Kod İncelemesi"
              />
              <p v-if="editingId !== NEW_ID && form.name !== originalName" class="mt-1 text-[11px] text-amber-700">
                Ad değişikliği bu durumdaki tüm görevlere uygulanır.
              </p>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Kategori</label>
              <select
                v-model="form.category"
                class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="TO_DO">Yapılacak</option>
                <option value="IN_PROGRESS">Devam ediyor</option>
                <option value="DONE">Tamamlandı</option>
              </select>
              <p class="mt-1 text-[11px] text-gray-500">Burndown ve velocity bu kategoriye bakar.</p>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-4">
            <label class="flex items-center gap-2 text-xs text-gray-700">
              <span>Renk</span>
              <input type="color" v-model="form.color" class="w-9 h-8 rounded border border-gray-300 cursor-pointer" />
            </label>
            <label class="flex items-center gap-2 text-xs text-gray-700">
              <span>İkon</span>
              <input
                v-model="form.icon"
                maxlength="2"
                class="w-14 text-sm text-center rounded-md border border-gray-300 px-2 py-1.5"
                placeholder="🚀"
              />
            </label>
            <label class="flex items-center gap-2 text-xs text-gray-700">
              <input type="checkbox" v-model="form.isInitial" class="rounded border-gray-300" />
              Yeni görevlerin başlangıcı
            </label>
            <label class="flex items-center gap-2 text-xs text-gray-700">
              <input type="checkbox" v-model="form.isFinal" class="rounded border-gray-300" />
              İş bitti sayılır
            </label>
            <label class="flex items-center gap-2 text-xs text-gray-700">
              <input type="checkbox" v-model="form.isCancellation" class="rounded border-gray-300" />
              İptal durumu
            </label>
          </div>
          <p class="text-[11px] text-gray-500">
            İptal durumundaki görevler listelerde varsayılan olarak gizlenir ve sprint
            kapanışında ne tamamlanmış ne yarım kalmış sayılır.
          </p>

          <p v-if="formError" class="text-xs text-red-600">{{ formError }}</p>

          <div class="flex justify-end gap-2 pt-1">
            <button class="px-3 py-1.5 text-xs text-gray-600 border border-gray-300 rounded-md" @click="cancelEdit">
              İptal
            </button>
            <button
              class="px-3 py-1.5 text-xs font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
              :disabled="saving || !form.name.trim()"
              @click="save"
            >
              {{ saving ? 'Kaydediliyor…' : 'Kaydet' }}
            </button>
          </div>
        </div>
      </li>
    </ul>

    <!-- Silme + taşıma dialogu -->
    <div v-if="deleting" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="deleting = null">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 p-6">
        <h3 class="font-semibold text-gray-900 mb-2">Durumu Sil</h3>
        <p class="text-sm text-gray-600">
          <strong>{{ deleting.name }}</strong> durumu silinecek.
        </p>
        <div class="mt-4">
          <label class="block text-xs font-medium text-gray-700 mb-1">
            Bu durumdaki görevler nereye taşınsın?
          </label>
          <select
            v-model="migrateToId"
            class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
          >
            <option :value="null">— Seçilmedi —</option>
            <option v-for="s in statuses.filter(s => s.id !== deleting.id)" :key="s.id" :value="s.id">
              {{ s.name }}
            </option>
          </select>
          <p class="mt-1 text-[11px] text-gray-500">
            Durumda görev yoksa boş bırakabilirsiniz.
          </p>
        </div>
        <p v-if="deleteError" class="mt-3 text-xs text-red-600">{{ deleteError }}</p>
        <div class="flex justify-end gap-2 mt-5">
          <button class="px-4 py-2 text-sm text-gray-600" @click="deleting = null">İptal</button>
          <button
            class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50"
            :disabled="saving"
            @click="confirmDelete"
          >
            {{ saving ? 'Siliniyor…' : 'Sil' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Takımın (veya projenin) görev durumlarını yöneten panel.
 *
 * Durum adı değiştiğinde sunucu o durumdaki görevleri de günceller; silmede ise
 * görev varsa hedef durum zorunludur. Bu yüzden ekran her iki işlemde de
 * kullanıcıyı bilgilendiriyor — sessiz veri kaybı olmasın.
 */
import { ref, computed, watch } from 'vue'
import WorkflowApi from '../../api/WorkflowApi.js'
import { useTaskStatuses, CATEGORY_LABELS, CATEGORY_CLASSES } from '../../composables/useTaskStatuses.js'

const props = defineProps({
  teamId: { type: String, required: true },
  projectId: { type: String, default: null },
})

const emit = defineEmits(['changed'])

const NEW_ID = '__new__'

const {
  statuses, unmapped, workflowId, scope, isFallback, loading, refresh,
} = useTaskStatuses(() => props.teamId, () => props.projectId)

const editingId = ref(null)
const originalName = ref('')
const saving = ref(false)
const formError = ref(null)
const deleting = ref(null)
const migrateToId = ref(null)
const deleteError = ref(null)

const emptyForm = () => ({
  name: '', category: 'TO_DO', color: '#6B7280', icon: '',
  isInitial: false, isFinal: false, isCancellation: false,
})
const form = ref(emptyForm())

function categoryClass(category) {
  return CATEGORY_CLASSES[category] || CATEGORY_CLASSES.TO_DO
}

function startCreate() {
  editingId.value = NEW_ID
  originalName.value = ''
  formError.value = null
  form.value = emptyForm()
}

function startEdit(status) {
  editingId.value = status.id
  originalName.value = status.name
  formError.value = null
  form.value = {
    name: status.name,
    category: status.category || 'TO_DO',
    color: status.color || '#6B7280',
    icon: status.icon || '',
    isInitial: !!status.isInitial,
    isFinal: !!status.isFinal,
    isCancellation: !!status.isCancellation,
  }
}

function cancelEdit() {
  editingId.value = null
  formError.value = null
}

/** Aynı ada sahip başka durum var mı — sunucuya gitmeden yakala. */
function isDuplicateName(name) {
  const key = name.trim().toLowerCase()
  return statuses.value.some(s => s.id !== editingId.value && s.name.toLowerCase() === key)
}

async function save() {
  const name = form.value.name.trim()
  if (!name) return
  if (isDuplicateName(name)) {
    formError.value = 'Bu isimde bir durum zaten var.'
    return
  }
  if (!workflowId.value) {
    formError.value = 'İş akışı bulunamadı.'
    return
  }

  saving.value = true
  formError.value = null
  try {
    const payload = { ...form.value, name }
    if (editingId.value === NEW_ID) {
      payload.position = statuses.value.length
      await WorkflowApi.addStatus(workflowId.value, payload)
    } else {
      await WorkflowApi.updateStatus(workflowId.value, editingId.value, payload)
    }
    editingId.value = null
    await reload()
  } catch (e) {
    formError.value = e.response?.data?.error || e.response?.data?.message || 'Durum kaydedilemedi.'
  } finally {
    saving.value = false
  }
}

function askDelete(status) {
  deleting.value = status
  migrateToId.value = null
  deleteError.value = null
}

async function confirmDelete() {
  if (!deleting.value) return
  saving.value = true
  deleteError.value = null
  try {
    await WorkflowApi.deleteStatus(workflowId.value, deleting.value.id, migrateToId.value)
    deleting.value = null
    await reload()
  } catch (e) {
    deleteError.value = e.response?.data?.error || e.response?.data?.message || 'Durum silinemedi.'
  } finally {
    saving.value = false
  }
}

/** Sıralama board sütunu önerisini ve gruplama sırasını belirler. */
async function move(index, delta) {
  const target = index + delta
  if (target < 0 || target >= statuses.value.length) return
  const a = statuses.value[index]
  const b = statuses.value[target]
  saving.value = true
  try {
    await WorkflowApi.updateStatus(workflowId.value, a.id, { position: target })
    await WorkflowApi.updateStatus(workflowId.value, b.id, { position: index })
    await reload()
  } catch (e) {
    console.error('Sıra değiştirilemedi:', e)
  } finally {
    saving.value = false
  }
}

/** Görevlerde bulunan tanımsız durumları listeye ekler. */
async function adoptUnmapped() {
  if (!workflowId.value || unmapped.value.length === 0) return
  saving.value = true
  try {
    let position = statuses.value.length
    for (const name of unmapped.value) {
      await WorkflowApi.addStatus(workflowId.value, {
        name, category: 'TO_DO', color: '#6B7280', position: position++,
      })
    }
    await reload()
  } catch (e) {
    console.error('Durumlar eklenemedi:', e)
  } finally {
    saving.value = false
  }
}

/** Projeyi takım setinden ayırıp kendi kopyasını verir. */
async function detachFromTeam() {
  if (!props.projectId) return
  saving.value = true
  try {
    await WorkflowApi.provisionForProject(props.projectId)
    await reload()
  } catch (e) {
    console.error('Projeye özel durum seti oluşturulamadı:', e)
  } finally {
    saving.value = false
  }
}

async function reload() {
  await refresh()
  emit('changed')
}

// Kapsam değişince açık düzenleme formu geçersiz kalır.
watch(() => [props.teamId, props.projectId], () => { editingId.value = null })
</script>
