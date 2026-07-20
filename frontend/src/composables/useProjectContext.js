/**
 * useProjectContext.js
 *
 * Aktif proje context'i — bir takım birden fazla projede çalışabilir, görevler ve
 * sürümler proje bazlıdır, sprintler ise takım bazlı kalır (takımın zaman kutusu).
 * Bu composable "şu an hangi projeye bakıyoruz" sorusunun tek kaynağıdır; board,
 * backlog, liste ve sürüm görünümleri seçimi buradan okur.
 *
 * Kalıcılık iki katmanlı:
 *  - URL query (?projectId=) — link paylaşıldığında/sayfa yenilendiğinde korunur
 *  - localStorage (takım bazlı) — takıma geri dönüldüğünde son seçim hatırlanır
 *
 * projectId null ise "Tüm projeler" görünümü aktiftir: takımın toplam yükünü görmek
 * isteyen scrum master için sprint planlamada gereklidir.
 */

import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTeamById } from '../api/TeamApi.js'

const STORAGE_PREFIX = 'worklist_project_'

/** "Tüm projeler" seçimini temsil eden sentinel — localStorage'da null saklanamaz. */
const ALL_PROJECTS = '__all__'

export function useProjectContext(teamIdRef) {
  const route = useRoute()
  const router = useRouter()

  const projects = ref([])
  const loading = ref(false)
  /** null = Tüm projeler */
  const projectId = ref(null)

  const teamId = computed(() =>
    typeof teamIdRef === 'function' ? teamIdRef() : teamIdRef?.value ?? teamIdRef
  )

  const activeProject = computed(() =>
    projects.value.find(p => p.id === projectId.value) || null
  )

  const isAllProjects = computed(() => projectId.value === null)

  /** Takımın hiç projesi yoksa proje seçici gizlenir, kurulum yönlendirmesi gösterilir. */
  const hasProjects = computed(() => projects.value.length > 0)

  function storageKey() {
    return teamId.value ? `${STORAGE_PREFIX}${teamId.value}` : null
  }

  function persist(value) {
    const key = storageKey()
    if (!key) return
    try {
      localStorage.setItem(key, value ?? ALL_PROJECTS)
    } catch {
      // Private mode / kota — kalıcılık best-effort, seçim yine de oturum içinde çalışır
    }
  }

  function readPersisted() {
    const key = storageKey()
    if (!key) return undefined
    try {
      const raw = localStorage.getItem(key)
      if (raw === null) return undefined
      return raw === ALL_PROJECTS ? null : raw
    } catch {
      return undefined
    }
  }

  /**
   * Seçim önceliği: URL query > localStorage > takımın birincil projesi > ilk proje.
   * Kayıtlı seçim artık takımın projesi değilse (proje kaldırılmış/arşivlenmiş olabilir)
   * sessizce varsayılana düşülür — bozuk bir ID yüzünden boş ekran gösterilmez.
   */
  function resolveInitialSelection() {
    const ids = projects.value.map(p => p.id)

    const fromUrl = route.query.projectId
    if (fromUrl === ALL_PROJECTS) return null
    if (fromUrl && ids.includes(fromUrl)) return fromUrl

    const stored = readPersisted()
    if (stored === null) return null
    if (stored && ids.includes(stored)) return stored

    const primary = projects.value.find(p => p.primary)
    return primary?.id ?? projects.value[0]?.id ?? null
  }

  function syncUrl(value) {
    const next = { ...route.query }
    if (value) next.projectId = value
    else next.projectId = ALL_PROJECTS
    // replace: proje değiştirmek geri tuşuyla dolaşılacak bir gezinme değil
    router.replace({ query: next }).catch(() => {})
  }

  function selectProject(value) {
    projectId.value = value ?? null
    persist(projectId.value)
    syncUrl(projectId.value)
  }

  async function loadProjects() {
    if (!teamId.value) return
    loading.value = true
    try {
      const team = await getTeamById(teamId.value)
      projects.value = team?.projects ?? []
      projectId.value = resolveInitialSelection()
      persist(projectId.value)
      // İlk yüklemede URL'i sessizce hizala ki paylaşılan link doğru context'i taşısın
      if (route.query.projectId !== (projectId.value ?? ALL_PROJECTS)) {
        syncUrl(projectId.value)
      }
    } catch (e) {
      console.warn('[useProjectContext] takım projeleri yüklenemedi:', e)
      projects.value = []
      projectId.value = null
    } finally {
      loading.value = false
    }
  }

  // Takım değişince proje listesi ve seçim baştan çözümlenir — WorkList aynı component
  // instance'ını farklı takımlar için yeniden kullanıyor (route: /workList/:teamId).
  watch(teamId, () => { loadProjects() })

  return {
    projects,
    projectId,
    activeProject,
    isAllProjects,
    hasProjects,
    loading,
    selectProject,
    loadProjects,
    ALL_PROJECTS,
  }
}
