/**
 * useOrganizationContext.js
 *
 * Aktif organizasyon context'i — bir kullanıcı birden fazla organizasyonun üyesi
 * olabilir; projeler, takımlar, üyeler ve abonelik hepsi organizasyon bazlıdır.
 * Bu composable "şu an hangi organizasyondayız" sorusunun tek kaynağıdır.
 *
 * State modül seviyesinde tutulur (useAuth ile aynı kalıp): switcher'dan ya da
 * ayarlar ekranından yapılan seçim, aynı state'i okuyan her component'e anında
 * yansır — aradaki prop zincirinin senkron kalmasına bel bağlanmaz.
 *
 * Seçim localStorage'da saklanır; aksi halde her sayfa yüklemesinde listenin ilk
 * elemanına düşülür ve kullanıcı seçtiği organizasyonda kalamaz.
 */

import { ref, computed } from 'vue'
import OrganizationApi from '../api/OrganizationApi.js'

const STORAGE_KEY = 'activeOrgId'

function readPersisted() {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

function persist(id) {
  try {
    if (id) localStorage.setItem(STORAGE_KEY, id)
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Private mode / kota — kalıcılık best-effort, seçim yine de oturum içinde çalışır
  }
}

const organizations = ref([])
const activeOrgId = ref(readPersisted())
const loading = ref(false)
const loaded = ref(false)

/** Aynı anda birden fazla component mount olduğunda tek istek atılır. */
let inFlight = null

function setActive(id) {
  activeOrgId.value = id ?? null
  persist(activeOrgId.value)
}

/**
 * Seçim önceliği: kayıtlı seçim (hâlâ üyeysek) > listenin ilki.
 * Kayıtlı id artık listede yoksa — organizasyondan çıkarılmış ya da silinmiş
 * olabilir — sessizce ilk organizasyona düşülür; bozuk bir id yüzünden boş ekran
 * gösterilmez.
 */
function resolveActive() {
  const ids = organizations.value.map(o => o.id)
  if (activeOrgId.value && ids.includes(activeOrgId.value)) return activeOrgId.value
  return organizations.value[0]?.id ?? null
}

async function loadOrganizations({ force = false } = {}) {
  if (inFlight) return inFlight
  if (loaded.value && !force) return organizations.value

  loading.value = true
  inFlight = (async () => {
    try {
      const res = await OrganizationApi.getMyOrganizations()
      organizations.value = res.data ?? []
      setActive(resolveActive())
      loaded.value = true
    } catch (e) {
      console.warn('[useOrganizationContext] organizasyonlar yüklenemedi:', e)
      organizations.value = []
      // Kayıtlı seçim korunur: geçici bir ağ hatası kullanıcının organizasyonunu
      // değiştirmemeli, sonraki denemede aynı yere dönmeli.
    } finally {
      loading.value = false
      inFlight = null
    }
    return organizations.value
  })()

  return inFlight
}

/** Organizasyon nesnesi ya da id kabul eder — çağrı yerlerinde dönüşüm gerekmesin. */
function selectOrg(orgOrId) {
  const id = typeof orgOrId === 'string' ? orgOrId : orgOrId?.id ?? null
  if (id === activeOrgId.value) return
  setActive(id)
}

/**
 * Oluşturma/güncelleme sonrası listeyi yeniden çekmeden senkron tutar: yeni
 * organizasyon anında switcher'da görünür, değişen ad başlıkta hemen yenilenir.
 */
function upsertOrganization(org) {
  if (!org?.id) return
  const exists = organizations.value.some(o => o.id === org.id)
  organizations.value = exists
    ? organizations.value.map(o => (o.id === org.id ? { ...o, ...org } : o))
    : [...organizations.value, org]
}

/** Logout'ta çağrılır — sonraki kullanıcı bir öncekinin seçimini devralmasın. */
export function clearOrganizationContext() {
  organizations.value = []
  loaded.value = false
  inFlight = null
  setActive(null)
}

export function useOrganizationContext() {
  const activeOrg = computed(
    () => organizations.value.find(o => o.id === activeOrgId.value) || null
  )

  const hasOrganizations = computed(() => organizations.value.length > 0)

  /** Birden fazla organizasyon yoksa geçiş arayüzü gösterilmez. */
  const canSwitch = computed(() => organizations.value.length > 1)

  return {
    organizations,
    activeOrgId,
    activeOrg,
    hasOrganizations,
    canSwitch,
    loading,
    loaded,
    loadOrganizations,
    selectOrg,
    upsertOrganization,
    clearOrganizationContext,
  }
}

export default useOrganizationContext
