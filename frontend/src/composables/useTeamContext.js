/**
 * useTeamContext.js
 *
 * Aktif takım context'i — "şu an hangi takımla çalışıyoruz" sorusunun TEK kaynağı.
 * Board, Retrospective, Scrum Poker, GameBox, CodeShare ve Dashboard hepsi seçimi
 * buradan okur; sayfa içi takım seçicileri kaldırıldı, seçim Ayarlar'dan yönetilir.
 *
 * Organizasyonla ilişki: her takım bir organizasyona bağlıdır (TeamResponse.organizationId).
 * Aktif takım ile aktif organizasyon hep hizalı tutulur:
 *  - Takım seçilince organizasyonu takıma çekilir (derin link ile başka organizasyonun
 *    takımı açıldığında context bütün olarak o organizasyona geçer).
 *  - Ayarlar'dan organizasyon değiştirilince, aktif takım o organizasyonda değilse
 *    organizasyonun ilk takımına düşülür.
 *
 * Kalıcılık: localStorage 'selectedTeam' — eski yapının kullandığı anahtarla aynı,
 * böylece mevcut kullanıcıların seçimi göç sırasında kaybolmaz.
 */

import { ref, computed, watch } from 'vue'
import { getMyTeams } from '../api/TeamApi.js'
import { useOrganizationContext } from './useOrganizationContext.js'

const STORAGE_KEY = 'selectedTeam'

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

const teams = ref([])
const activeTeamId = ref(readPersisted())
const loading = ref(false)
const loaded = ref(false)

/** Aynı anda birden fazla component mount olduğunda tek istek atılır. */
let inFlight = null

// Organizasyon context'i modül seviyesinde paylaşılan state döndürür; burada
// component dışında çağırmak güvenlidir.
const orgContext = useOrganizationContext()

function setActive(id) {
  activeTeamId.value = id ?? null
  persist(activeTeamId.value)
}

function findTeam(id) {
  return teams.value.find(t => t.id === id) || null
}

/**
 * Organizasyonu takıma hizalar. Takımın organizasyonu kullanıcının organizasyon
 * listesinde yoksa (liste henüz yüklenmemiş olabilir) dokunulmaz — organizasyon
 * context'i kendi yüklemesinde doğrular.
 */
function alignOrgToTeam(team) {
  const orgId = team?.organizationId
  if (!orgId || orgId === orgContext.activeOrgId.value) return
  orgContext.selectOrg(orgId)
}

async function loadTeams({ force = false } = {}) {
  if (inFlight) return inFlight
  if (loaded.value && !force) return teams.value

  loading.value = true
  inFlight = (async () => {
    try {
      const res = await getMyTeams()
      teams.value = res ?? []

      // Kayıtlı takım hâlâ üyesi olduğumuz bir takımsa o kazanır ve organizasyon
      // ona hizalanır (organizasyon özelliğinden önce seçim yapmış kullanıcılar
      // kaldıkları takımda kalır). Değilse aktif organizasyonun ilk takımına düşülür.
      const stored = findTeam(activeTeamId.value)
      if (stored) {
        alignOrgToTeam(stored)
        setActive(stored.id)
      } else {
        const inOrg = teams.value.filter(
          t => !orgContext.activeOrgId.value || t.organizationId === orgContext.activeOrgId.value
        )
        setActive(inOrg[0]?.id ?? teams.value[0]?.id ?? null)
      }
      loaded.value = true
    } catch (e) {
      console.warn('[useTeamContext] takımlar yüklenemedi:', e)
      teams.value = []
      // Kayıtlı seçim korunur: geçici ağ hatası kullanıcının takımını değiştirmemeli.
    } finally {
      loading.value = false
      inFlight = null
    }
    return teams.value
  })()

  return inFlight
}

/** Takım nesnesi ya da id kabul eder — çağrı yerlerinde dönüşüm gerekmesin. */
function selectTeam(teamOrId) {
  const id = typeof teamOrId === 'string' ? teamOrId : teamOrId?.id ?? null
  if (!id) {
    setActive(null)
    return
  }
  setActive(id)
  const team = findTeam(id)
  if (team) alignOrgToTeam(team)
}

/**
 * URL'de :teamId taşıyan sayfalar (Board, Scrum Poker, CodeShare, GameBox, Retro
 * board) için: paylaşılan/derin linkteki takım context'e adopte edilir — URL her
 * zaman kazanır, sonraki gezinmeler aynı takımda devam eder. Takım listede yoksa
 * (üyelikten çıkarılmış link olabilir) context'e dokunulmaz.
 */
async function adoptTeam(teamId) {
  if (!teamId) return
  if (teamId === activeTeamId.value && loaded.value) return
  await loadTeams()
  if (findTeam(teamId)) selectTeam(teamId)
}

/** Logout'ta çağrılır — sonraki kullanıcı bir öncekinin seçimini devralmasın. */
export function clearTeamContext() {
  teams.value = []
  loaded.value = false
  inFlight = null
  setActive(null)
}

// Ayarlar'dan organizasyon değiştirilince: aktif takım yeni organizasyonda değilse
// o organizasyonun ilk takımına geçilir. İlk yükleme sırasında (loaded=false)
// tetiklenmez — kayıtlı seçim, liste gelmeden sıfırlanmamalı.
watch(
  () => orgContext.activeOrgId.value,
  orgId => {
    if (!loaded.value || !orgId) return
    const current = findTeam(activeTeamId.value)
    if (current && current.organizationId === orgId) return
    const inOrg = teams.value.filter(t => t.organizationId === orgId)
    setActive(inOrg[0]?.id ?? null)
  }
)

export function useTeamContext() {
  const activeTeam = computed(() => findTeam(activeTeamId.value))

  /** Ayarlar'daki takım seçici yalnızca aktif organizasyonun takımlarını listeler. */
  const teamsInActiveOrg = computed(() => {
    const orgId = orgContext.activeOrgId.value
    if (!orgId) return teams.value
    return teams.value.filter(t => t.organizationId === orgId)
  })

  const hasTeams = computed(() => teams.value.length > 0)

  return {
    teams,
    teamsInActiveOrg,
    activeTeamId,
    activeTeam,
    hasTeams,
    loading,
    loaded,
    loadTeams,
    selectTeam,
    adoptTeam,
    clearTeamContext,
  }
}

export default useTeamContext
