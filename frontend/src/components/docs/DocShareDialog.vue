<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4"
       @click.self="$emit('close')">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-xl max-h-[85vh] flex flex-col">
      <!-- Başlık -->
      <div class="px-5 py-4 border-b border-slate-100 flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M7.217 10.907a2.25 2.25 0 100 2.186m0-2.186c.18.324.283.696.283 1.093s-.103.77-.283 1.093m0-2.186l9.566-5.314m-9.566 7.5l9.566 5.314m0 0a2.25 2.25 0 103.935 2.186 2.25 2.25 0 00-3.935-2.186zm0-12.814a2.25 2.25 0 103.933-2.185 2.25 2.25 0 00-3.933 2.185z"/>
          </svg>
        </div>
        <div class="min-w-0">
          <h2 class="font-semibold text-slate-800 leading-tight">Paylaş</h2>
          <p class="text-xs text-slate-400 truncate">{{ title || 'Doküman' }}</p>
        </div>
        <button @click="$emit('close')"
                class="ml-auto p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="flex-1 overflow-y-auto p-5 space-y-5">
        <!-- ─── Link ───────────────────────────────────────────────────────── -->
        <section>
          <label class="block text-sm font-medium text-slate-700 mb-1.5">Bağlantı</label>
          <div class="flex gap-2">
            <input :value="shareUrl" readonly @focus="$event.target.select()"
                   class="flex-1 min-w-0 border border-slate-200 bg-slate-50 rounded-xl px-3 py-2 text-sm text-slate-600 outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition"/>
            <button @click="copyLink"
                    class="shrink-0 inline-flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-xl text-sm font-medium shadow-sm shadow-indigo-200 transition">
              <svg v-if="!copied" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 01-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 011.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 00-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 01-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 00-3.375-3.375h-1.5a1.125 1.125 0 01-1.125-1.125v-1.5a3.375 3.375 0 00-3.375-3.375H9.75"/>
              </svg>
              <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/>
              </svg>
              {{ copied ? 'Kopyalandı' : 'Kopyala' }}
            </button>
          </div>
          <!--
            Bu uyarı süs değil: linkin tek başına erişim vermediğini söylemezsek
            kullanıcı linki gönderip "paylaştım" sanıyor, karşı taraf 403 alıyor
            ve arıza "link çalışmıyor" diye geri geliyor.
          -->
          <p class="mt-2 text-xs text-slate-500 flex items-start gap-1.5">
            <svg class="w-3.5 h-3.5 mt-0.5 shrink-0 text-slate-400" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z"/>
            </svg>
            <span>Bağlantı yalnızca erişim yetkisi olan kişilerde açılır. Aşağıdan kişi ya da takım ekleyin.</span>
          </p>
        </section>

        <!-- ─── Davet ──────────────────────────────────────────────────────── -->
        <section class="border-t border-slate-100 pt-5">
          <label class="block text-sm font-medium text-slate-700 mb-2">Kişi veya takım ekle</label>

          <div class="flex flex-wrap gap-2 mb-2">
            <select v-model="form.targetType"
                    class="border border-slate-200 rounded-xl px-2.5 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition">
              <option v-for="(label, key) in targetTypeLabels" :key="key" :value="key">{{ label }}</option>
            </select>
            <select v-model="form.accessLevel"
                    class="border border-slate-200 rounded-xl px-2.5 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition">
              <option value="READ">Okuyabilir</option>
              <option value="WRITE">Düzenleyebilir</option>
              <option value="ADMIN">Yönetebilir</option>
            </select>
            <select v-if="pageId" v-model="form.scope"
                    class="border border-slate-200 rounded-xl px-2.5 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition">
              <option value="page">Sadece bu sayfa</option>
              <option value="space">Tüm alan</option>
            </select>
          </div>

          <!-- Seçili hedef -->
          <div v-if="selectedTarget"
               class="flex items-center justify-between border border-indigo-200 bg-indigo-50 rounded-xl px-3 py-2">
            <div class="min-w-0">
              <span class="text-sm font-medium text-slate-800">{{ selectedTarget.name }}</span>
              <span v-if="selectedTarget.detail" class="text-xs text-slate-500 ml-2">{{ selectedTarget.detail }}</span>
            </div>
            <button @click="clearTarget"
                    class="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-indigo-100 ml-2 shrink-0 transition">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <!-- Arama -->
          <div v-else class="relative">
            <input v-model="targetSearch" type="text" :placeholder="searchPlaceholder"
                   @focus="onSearchFocus" @blur="showResults = false"
                   class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition placeholder:text-slate-400"/>
            <div v-if="showResults"
                 class="absolute z-10 mt-1 w-full bg-white border border-slate-200 rounded-xl shadow-lg shadow-slate-200/60 max-h-56 overflow-y-auto py-1">
              <div v-if="searching" class="px-3 py-2 text-sm text-slate-400">Aranıyor…</div>
              <div v-else-if="targetResults.length === 0" class="px-3 py-2 text-sm text-slate-400">Sonuç bulunamadı</div>
              <button v-else v-for="result in targetResults" :key="result.id"
                      @mousedown.prevent="selectTarget(result)"
                      class="w-full text-left px-3 py-2 hover:bg-indigo-50 flex items-center justify-between gap-2 transition">
                <span class="text-sm text-slate-800 truncate">{{ result.name }}</span>
                <span v-if="result.detail" class="text-xs text-slate-400 truncate">{{ result.detail }}</span>
              </button>
            </div>
          </div>

          <div class="flex items-center justify-between gap-3 mt-3">
            <label class="flex items-center gap-2 text-xs text-slate-500 cursor-pointer">
              <input type="checkbox" v-model="form.canDelegate" class="rounded border-slate-300 text-indigo-600"/>
              Bu kişi de paylaşabilsin
            </label>
            <button @click="share" :disabled="!form.targetId || sharing"
                    class="bg-slate-800 hover:bg-slate-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white px-4 py-2 rounded-xl text-sm font-medium transition">
              {{ sharing ? 'Ekleniyor…' : 'Paylaş' }}
            </button>
          </div>
          <p v-if="shareError" class="mt-2 text-xs text-rose-600">{{ shareError }}</p>
        </section>

        <!-- ─── Erişimi olanlar ────────────────────────────────────────────── -->
        <section class="border-t border-slate-100 pt-5">
          <div class="flex items-center justify-between mb-2">
            <label class="text-sm font-medium text-slate-700">Erişimi olanlar</label>
            <span class="text-xs text-slate-400">{{ scopeLabel }}</span>
          </div>

          <div v-if="loading" class="text-sm text-slate-400 py-4">Yükleniyor…</div>
          <div v-else-if="permissions.length === 0" class="text-sm text-slate-400 py-4">
            Bu {{ pageId ? 'sayfa' : 'alan' }} için özel bir paylaşım yok — proje yöneticileri erişebilir.
          </div>
          <div v-else class="space-y-2">
            <div v-for="perm in permissions" :key="perm.id"
                 class="flex items-center justify-between gap-3 border border-slate-200 rounded-xl px-3 py-2.5 hover:border-slate-300 transition">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-1.5">
                  <span class="text-sm font-medium text-slate-800">{{ perm.targetName }}</span>
                  <span class="text-xs bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full">
                    {{ targetTypeLabels[perm.targetType] }}
                  </span>
                  <span :class="accessBadgeClass(perm.accessLevel)">{{ accessLabels[perm.accessLevel] }}</span>
                  <span v-if="perm.canDelegate" class="text-xs bg-amber-50 text-amber-600 px-2 py-0.5 rounded-full">
                    Paylaşabilir
                  </span>
                </div>
                <p class="text-xs text-slate-400 mt-0.5">{{ perm.grantedByName }} ekledi</p>
              </div>
              <button @click="revokeTarget = perm"
                      class="shrink-0 text-sm font-medium text-rose-500 hover:text-rose-600 hover:bg-rose-50 px-2.5 py-1.5 rounded-lg transition">
                Kaldır
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>

    <ConfirmDialog
        v-if="revokeTarget"
        title="Erişimi kaldır"
        :message="`${revokeTarget.targetName} artık bu içeriğe erişemeyecek.`"
        confirmText="Kaldır"
        variant="danger"
        @confirm="revoke"
        @cancel="revokeTarget = null"/>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import DocApi from '../../api/DocApi.js'
import ConfirmDialog from '../common/ConfirmDialog.vue'

/**
 * Doküman paylaşımı — bağlantı + kişi/takım daveti.
 *
 * <b>Paylaşım modeli:</b> bağlantı tek başına erişim <i>vermez</i>. Token'lı
 * herkese açık link bilinçli olarak yok: Docs sayfaları proje içi bilgi taşıyor
 * ve tahmin edilemez bir URL, kimlik doğrulamanın yerine geçmez. Erişim her
 * zaman {@code DocPermission} üzerinden veriliyor, bağlantı yalnızca adresi
 * taşıyor. Arayüz bunu açıkça söylüyor (yukarıdaki uyarı).
 *
 * <b>Neden hep `delegatePermission`:</b> sunucudaki devir yolu, yetki
 * yönetiminin üst kümesi — süperadmin ve DOCS_MANAGE_SPACES sahipleri de oradan
 * geçiyor. Tek uç nokta kullanmak, kullanıcıya anlamsız bir "devretme modu"
 * seçeneği sormayı gereksiz kılıyor.
 */
const props = defineProps({
  projectId: { type: String, required: true },
  spaceId: { type: String, required: true },
  pageId: { type: String, default: null },
  title: { type: String, default: '' }
})

defineEmits(['close'])

const targetTypeLabels = {
  USER: 'Kullanıcı',
  TEAM: 'Takım',
  ORGANIZATION: 'Organizasyon',
  PROJECT_MEMBERS: 'Proje üyeleri'
}

const accessLabels = {
  READ: 'Okuyabilir',
  WRITE: 'Düzenleyebilir',
  ADMIN: 'Yönetebilir'
}

const searchPlaceholders = {
  USER: 'İsim veya e-posta ile ara…',
  TEAM: 'Takım adı ile ara…',
  ORGANIZATION: 'Organizasyon seç…',
  PROJECT_MEMBERS: 'Proje seç…'
}

const permissions = ref([])
const loading = ref(true)
const revokeTarget = ref(null)
const sharing = ref(false)
const shareError = ref('')
const copied = ref(false)

const form = ref({
  scope: props.pageId ? 'page' : 'space',
  targetType: 'USER',
  targetId: '',
  accessLevel: 'READ',
  canDelegate: false
})

// ─── Bağlantı ────────────────────────────────────────────────────────────────

/**
 * Mutlak adres: kullanıcı bunu kopyalayıp başka bir yere yapıştıracak, göreli
 * yol orada işe yaramaz.
 */
const shareUrl = computed(() => {
  const path = props.pageId
      ? `/projects/${props.projectId}/docs/${props.spaceId}/pages/${props.pageId}`
      : `/projects/${props.projectId}/docs/${props.spaceId}`
  return `${window.location.origin}${path}`
})

const scopeLabel = computed(() => (props.pageId ? 'Bu sayfa' : 'Tüm alan'))

async function copyLink() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
  } catch {
    // `navigator.clipboard` güvenli olmayan bağlamda (http) ve bazı tarayıcı
    // ayarlarında yok; sessizce başarısız olmak yerine eski yola düşülüyor.
    const field = document.createElement('textarea')
    field.value = shareUrl.value
    field.style.position = 'fixed'
    field.style.opacity = '0'
    document.body.appendChild(field)
    field.select()
    try { document.execCommand('copy') } catch { /* kullanıcı elle kopyalar */ }
    document.body.removeChild(field)
  }
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

// ─── Hedef arama ─────────────────────────────────────────────────────────────

const targetSearch = ref('')
const targetResults = ref([])
const selectedTarget = ref(null)
const searching = ref(false)
const showResults = ref(false)
let searchTimer = null

const searchPlaceholder = computed(() => searchPlaceholders[form.value.targetType])

watch(() => form.value.targetType, clearTarget)

watch(targetSearch, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(searchTargets, 300)
})

async function searchTargets() {
  searching.value = true
  showResults.value = true
  try {
    const res = await DocApi.searchPermissionTargets(
        props.projectId, form.value.targetType, targetSearch.value)
    targetResults.value = res.data
  } catch {
    targetResults.value = []
  } finally {
    searching.value = false
  }
}

function onSearchFocus() {
  searchTargets()
}

function selectTarget(target) {
  selectedTarget.value = target
  form.value.targetId = target.id
  showResults.value = false
}

function clearTarget() {
  selectedTarget.value = null
  form.value.targetId = ''
  targetSearch.value = ''
  targetResults.value = []
  showResults.value = false
}

// ─── Yetkiler ────────────────────────────────────────────────────────────────

onMounted(loadPermissions)
onUnmounted(() => clearTimeout(searchTimer))

async function loadPermissions() {
  loading.value = true
  try {
    const params = props.pageId ? { pageId: props.pageId } : { spaceId: props.spaceId }
    const res = await DocApi.getPermissions(props.projectId, params)
    permissions.value = res.data
  } catch {
    permissions.value = []
  } finally {
    loading.value = false
  }
}

async function share() {
  if (!form.value.targetId || sharing.value) return
  sharing.value = true
  shareError.value = ''
  try {
    await DocApi.delegatePermission(props.projectId, {
      spaceId: form.value.scope === 'space' ? props.spaceId : null,
      pageId: form.value.scope === 'page' ? props.pageId : null,
      targetType: form.value.targetType,
      targetId: form.value.targetId,
      accessLevel: form.value.accessLevel,
      canDelegate: form.value.canDelegate
    }, { _skipErrorToast: true })
    // Liste yeniden çekiliyor, gelen tek kaydı eklemek yerine: aynı hedefe
    // ikinci kez paylaşım sunucuda güncelleme yapıyor, yerel ekleme yinelenmiş
    // satır gösterirdi.
    await loadPermissions()
    clearTarget()
    form.value.canDelegate = false
  } catch (e) {
    shareError.value = e?.response?.data?.error
        || 'Paylaşım eklenemedi. Bu içerikte paylaşma yetkiniz olmayabilir.'
  } finally {
    sharing.value = false
  }
}

async function revoke() {
  const perm = revokeTarget.value
  revokeTarget.value = null
  if (!perm) return
  try {
    await DocApi.revokePermission(props.projectId, perm.id)
    permissions.value = permissions.value.filter((p) => p.id !== perm.id)
  } catch { /* axios interceptor toast gösteriyor */ }
}

function accessBadgeClass(level) {
  const base = 'text-xs px-2 py-0.5 rounded-full'
  switch (level) {
    case 'READ': return `${base} bg-emerald-50 text-emerald-600`
    case 'WRITE': return `${base} bg-amber-50 text-amber-600`
    case 'ADMIN': return `${base} bg-rose-50 text-rose-600`
    default: return `${base} bg-slate-100 text-slate-600`
  }
}
</script>
