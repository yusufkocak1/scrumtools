/**
 * useTaskStatuses.js
 *
 * Görev durumlarının frontend'deki tek kaynağı.
 *
 * Durumlar artık kodda sabit bir dizi değil, takımın (varsa aktif projenin) iş
 * akışından geliyor. Görev formu, board kolonları, filtre önerileri, rozet
 * renkleri ve gruplama sırası hep bu listeden beslenir — böylece takım bir durumu
 * yeniden adlandırdığında ekranın bir köşesi eski adı göstermeye devam etmez.
 *
 * Aynı takım/proje için sonuç modül düzeyinde paylaşılır: board, backlog ve liste
 * görünümleri aynı anda açıldığında tek istek atılır.
 */

import { ref, computed, watch, unref } from 'vue'
import WorkflowApi from '../api/WorkflowApi.js'

/**
 * Sunucu durum listesini veremediğinde kullanılan set.
 *
 * Uygulamanın eski sabit durumlarıyla aynı: durum kaydı devreye girmeden önce
 * yazılmış görevler ve board sütunları bu adlarla eşleşiyor. Geçiş sırasında
 * (ör. frontend güncellenip backend henüz güncellenmemişken) durum seçicilerin
 * ve board sütunlarının boş kalmaması için gerekli.
 */
const FALLBACK_STATUSES = [
    { id: 'fallback-todo', name: 'To Do', category: 'TO_DO', color: '#6B7280', isInitial: true },
    { id: 'fallback-progress', name: 'In Progress', category: 'IN_PROGRESS', color: '#3B82F6' },
    { id: 'fallback-review', name: 'In Review', category: 'IN_PROGRESS', color: '#8B5CF6' },
    { id: 'fallback-done', name: 'Done', category: 'DONE', color: '#10B981', isFinal: true },
    { id: 'fallback-cancelled', name: 'Cancelled', category: 'DONE', color: '#EF4444', isFinal: true, isCancellation: true },
]

const fallbackCatalog = () => ({
    workflowId: null,
    workflowName: null,
    scope: 'TEAM',
    statuses: FALLBACK_STATUSES,
    unmapped: [],
    /** Ayarlar ekranı düzenlemeye izin vermemeli — bu liste sunucuda yok. */
    isFallback: true,
})

/** key → promise — aynı kapsam için tekrar istek atılmasını önler. */
const cache = new Map()

const cacheKey = (teamId, projectId) => `${teamId || '-'}:${projectId || '-'}`

/**
 * Kapsamın durum kataloğunu getirir. Aynı anahtar için uçuşta olan istek varsa
 * ona bağlanır.
 */
async function fetchCatalog(teamId, projectId, { force = false } = {}) {
    const key = cacheKey(teamId, projectId)
    if (!force && cache.has(key)) return cache.get(key)

    const promise = WorkflowApi.getStatuses(teamId, projectId)
        .then(res => {
            const data = res.data
            // Sunucu boş liste dönerse (workflow henüz üretilmemiş) ekranlar
            // durumsuz kalmasın.
            if (!data?.statuses?.length) return fallbackCatalog()
            return data
        })
        .catch(err => {
            // Hata kalıcı olmasın: sonraki deneme yeniden istek atabilsin.
            cache.delete(key)
            console.warn('[useTaskStatuses] durum listesi alınamadı, varsayılana düşülüyor:', err)
            return fallbackCatalog()
        })

    cache.set(key, promise)
    return promise
}

/** Durum listesi değiştiğinde (ayarlar ekranı) önbelleği düşür. */
export function invalidateStatusCache() {
    cache.clear()
}

/**
 * Kategorilerin görsel karşılığı. Durumun kendi rengi varsa o kullanılır;
 * kategori sınıfı yalnızca renk tanımlı değilken devreye girer.
 */
export const CATEGORY_CLASSES = {
    TO_DO: 'bg-gray-100 text-gray-700',
    IN_PROGRESS: 'bg-blue-100 text-blue-700',
    DONE: 'bg-green-100 text-green-700',
}

export const CATEGORY_LABELS = {
    TO_DO: 'Yapılacak',
    IN_PROGRESS: 'Devam ediyor',
    DONE: 'Tamamlandı',
}

/**
 * Katalogda bulunmayan durum adları için kategori tahmini.
 * Backend'deki TaskStatusCatalog.guessCategory ile aynı sözcük kümesini kullanır —
 * iki taraf aynı görevi farklı kategoriye koymasın diye.
 */
export function guessCategory(name) {
    const n = (name || '').toLowerCase()
    if (!n) return 'TO_DO'
    if (['done', 'closed', 'fixed', 'verified', 'cancelled', 'canceled', "won't fix",
        'wont fix', 'resolved', 'tamamland', 'iptal', 'kapand'].some(t => n.includes(t))) {
        return 'DONE'
    }
    if (['progress', 'review', 'testing', 'test', 'selected', 'devam',
        'incelem', 'geliştir', 'gelistir'].some(t => n.includes(t))) {
        return 'IN_PROGRESS'
    }
    return 'TO_DO'
}

/**
 * @param {import('vue').Ref<string>|string|Function} teamId
 * @param {import('vue').Ref<string|null>|string|null} [projectId] — aktif proje kapsamı
 */
export function useTaskStatuses(teamId, projectId = null) {
    const teamIdRef = computed(() =>
        typeof teamId === 'function' ? teamId() : unref(teamId) ?? null
    )
    const projectIdRef = computed(() =>
        typeof projectId === 'function' ? projectId() : unref(projectId) ?? null
    )

    const catalog = ref(null)
    const loading = ref(false)
    const error = ref(null)

    /** Sıralı durum nesneleri: { id, name, category, color, icon, isInitial, isFinal, isCancellation } */
    const statuses = computed(() => catalog.value?.statuses ?? [])
    const statusNames = computed(() => statuses.value.map(s => s.name))

    /** Görevlerde geçip iş akışında tanımlı olmayan adlar — ayarlar ekranı uyarısı. */
    const unmapped = computed(() => catalog.value?.unmapped ?? [])

    /** Düzenlenecek workflow ve kapsamı ('TEAM' | 'PROJECT'). */
    const workflowId = computed(() => catalog.value?.workflowId ?? null)
    const scope = computed(() => catalog.value?.scope ?? 'TEAM')
    /** Liste sunucudan değil yerel varsayılandan geliyorsa düzenleme yapılamaz. */
    const isFallback = computed(() => catalog.value?.isFallback === true)

    const byName = computed(() => {
        const map = new Map()
        for (const s of statuses.value) map.set(s.name.toLowerCase(), s)
        return map
    })

    function findStatus(name) {
        if (!name) return null
        return byName.value.get(String(name).toLowerCase()) || null
    }

    function categoryOf(name) {
        return findStatus(name)?.category ?? guessCategory(name)
    }

    function isDone(name) {
        const s = findStatus(name)
        if (s) return s.category === 'DONE' || s.isFinal === true
        return guessCategory(name) === 'DONE'
    }

    function isCancelled(name) {
        const s = findStatus(name)
        if (s) return s.isCancellation === true
        const n = (name || '').toLowerCase()
        return n.includes('cancel') || n.includes('iptal')
    }

    /** Yeni görevlerin varsayılan durumu. */
    const initialStatus = computed(() =>
        statuses.value.find(s => s.isInitial)?.name ?? statuses.value[0]?.name ?? 'To Do'
    )

    /** Rozet/etiket için stil — durumun kendi rengi öncelikli. */
    function styleOf(name) {
        const color = findStatus(name)?.color
        if (!color) return {}
        return { backgroundColor: `${color}1A`, color }
    }

    function classOf(name) {
        if (findStatus(name)?.color) return ''
        return CATEGORY_CLASSES[categoryOf(name)] ?? CATEGORY_CLASSES.TO_DO
    }

    /**
     * Gruplama/sıralama için durum sırası. İş akışındaki pozisyon esas alınır;
     * tanımsız durumlar sona düşer.
     */
    function orderOf(name) {
        const idx = statusNames.value.findIndex(n => n.toLowerCase() === String(name || '').toLowerCase())
        return idx < 0 ? Number.MAX_SAFE_INTEGER : idx
    }

    async function load({ force = false } = {}) {
        if (!teamIdRef.value) return
        loading.value = true
        error.value = null
        try {
            catalog.value = await fetchCatalog(teamIdRef.value, projectIdRef.value, { force })
        } catch (e) {
            error.value = e
            console.warn('[useTaskStatuses] durumlar yüklenemedi:', e)
        } finally {
            loading.value = false
        }
    }

    /** Ayarlar ekranı durumları değiştirdiğinde çağrılır. */
    async function refresh() {
        invalidateStatusCache()
        await load({ force: true })
    }

    watch([teamIdRef, projectIdRef], () => load(), { immediate: true })

    return {
        catalog, statuses, statusNames, unmapped, workflowId, scope, isFallback,
        loading, error,
        initialStatus,
        findStatus, categoryOf, isDone, isCancelled, styleOf, classOf, orderOf,
        load, refresh,
    }
}
