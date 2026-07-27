/**
 * useTaskQuery.js
 *
 * Görev sorgusunun tek kaynağı. Board, liste ve backlog görünümleri filtreyi
 * kendi ref'lerinde tutmak yerine buradan okur.
 *
 * İki giriş yolu vardır ve ikisi de aynı sorguyu besler:
 *  - Görsel koşullar (FilterBar / FilterBuilder) → filters[]
 *  - STQL metni (StqlInput) → query
 *
 * Kaynak (`source`) hangisinin yazdığını belirler; diğeri ondan türetilir.
 * STQL görsel düzenleyiciye sığmıyorsa (OR, parantez, fonksiyon) görsel sekme
 * salt-okunur uyarıya düşer — Jira'nın davranışı.
 *
 * Sorgu URL'ye (?q=) yansıtılır: paylaşılan link aynı görünümü açar.
 */

import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { runQuery, validateQuery } from '../api/QueryApi.js'
import { filtersToStql, stqlToFilters } from '../utils/stql.js'

/**
 * @param {Object} options
 * @param {import('vue').Ref<string>|string} options.teamId
 * @param {import('vue').Ref<string|null>} [options.projectId] — aktif proje kapsamı
 * @param {number} [options.pageSize]
 * @param {boolean} [options.syncUrl] — sorguyu URL query param'ına yansıt
 */
export function useTaskQuery({ teamId, projectId = ref(null), pageSize = 50, syncUrl = true } = {}) {
    const route = useRoute()
    const router = useRouter()

    const teamIdRef = computed(() =>
        typeof teamId === 'function' ? teamId() : teamId?.value ?? teamId
    )
    const projectIdRef = computed(() => projectId?.value ?? projectId ?? null)

    // ─── Sorgu state'i ────────────────────────────────────────────────────────

    /** STQL metni — sorgunun kanonik hâli. */
    const query = ref('')
    /** Görsel koşullar: [{ field, operator, values }] */
    const filters = ref([])
    /** 'builder' | 'stql' — en son hangi taraf yazdı. */
    const source = ref('builder')
    /** Görsel düzenleyicinin sorguyu temsil edip edemediği. */
    const builderCompatible = ref(true)
    const builderIncompatibleReason = ref(null)

    // ─── Sonuç state'i ────────────────────────────────────────────────────────

    const tasks = ref([])
    const totalElements = ref(0)
    const totalPages = ref(0)
    const page = ref(0)
    const size = ref(pageSize)
    const isLoading = ref(false)
    /** { message, position, length } — sunucudan gelen sözdizimi hatası. */
    const error = ref(null)

    const hasQuery = computed(() => query.value.trim().length > 0)
    const activeFilterCount = computed(() => filters.value.length)

    // ─── Görsel ↔ STQL senkronu ───────────────────────────────────────────────

    /** Görsel koşullardan sorgu metnini türetir. */
    function syncFromFilters() {
        source.value = 'builder'
        query.value = filtersToStql(filters.value)
        builderCompatible.value = true
        builderIncompatibleReason.value = null
    }

    /** Sorgu metninden görsel koşulları türetmeyi dener. */
    function syncFromQuery() {
        source.value = 'stql'
        const result = stqlToFilters(query.value)
        builderCompatible.value = result.convertible
        builderIncompatibleReason.value = result.reason
        if (result.convertible) {
            filters.value = result.filters
        }
    }

    /**
     * Sorgu metni dışarıdan da değiştirilebilir (v-model, kayıtlı filtre, URL).
     * Metin görsel koşulların birebir karşılığıysa zaten senkrondur ve yeniden
     * çözümlenmesi gerekmez; değilse STQL tarafı yazmış demektir.
     */
    watch(query, (text) => {
        if (text === filtersToStql(filters.value)) return
        syncFromQuery()
    })

    // ─── Görsel koşul işlemleri ───────────────────────────────────────────────

    function addFilter(filter) {
        const idx = filters.value.findIndex(f => f.field === filter.field)
        if (idx >= 0) filters.value[idx] = { ...filter }
        else filters.value.push({ ...filter })
        syncFromFilters()
        page.value = 0
    }

    function removeFilter(field) {
        filters.value = filters.value.filter(f => f.field !== field)
        syncFromFilters()
        page.value = 0
    }

    function setFilters(list) {
        filters.value = (list || []).map(f => ({ ...f }))
        syncFromFilters()
        page.value = 0
    }

    function clearAll() {
        filters.value = []
        query.value = ''
        error.value = null
        builderCompatible.value = true
        builderIncompatibleReason.value = null
        page.value = 0
    }

    /** STQL metnini doğrudan ayarlar (editör veya kayıtlı filtre). */
    function setQuery(text) {
        query.value = text ?? ''
        syncFromQuery()
        page.value = 0
    }

    // ─── Çalıştırma ───────────────────────────────────────────────────────────

    async function run() {
        if (!teamIdRef.value) return
        isLoading.value = true
        error.value = null
        try {
            const result = await runQuery(teamIdRef.value, {
                query: query.value,
                projectId: projectIdRef.value,
                page: page.value,
                size: size.value,
            })
            tasks.value = result.content || []
            totalElements.value = result.totalElements || 0
            totalPages.value = result.totalPages || 0
        } catch (e) {
            const body = e?.response?.data
            error.value = body?.error
                ? { message: body.error, position: body.position ?? 0, length: body.length ?? 1 }
                : { message: 'Sorgu çalıştırılamadı.', position: 0, length: 1 }
            tasks.value = []
            totalElements.value = 0
            totalPages.value = 0
        } finally {
            isLoading.value = false
        }
    }

    /** Sorguyu çalıştırmadan doğrular — editör yazarken çağırır. */
    async function validate(text = query.value) {
        if (!teamIdRef.value) return { valid: true }
        try {
            const result = await validateQuery(teamIdRef.value, text, projectIdRef.value)
            error.value = result.valid ? null : result.error
            return result
        } catch {
            // Doğrulama ucu erişilemiyorsa editör kullanılabilir kalmalı.
            return { valid: true }
        }
    }

    function setPage(p) {
        page.value = p
        run()
    }

    // ─── URL senkronu ─────────────────────────────────────────────────────────

    function pushToUrl() {
        if (!syncUrl || !router) return
        const q = { ...route.query }
        if (query.value.trim()) q.q = query.value.trim()
        else delete q.q
        router.replace({ query: q }).catch(() => {})
    }

    /** Sayfa açılışında URL'deki sorguyu geri yükler. */
    function restoreFromUrl() {
        if (!syncUrl) return false
        const fromUrl = route.query?.q
        if (typeof fromUrl === 'string' && fromUrl.trim()) {
            setQuery(fromUrl)
            return true
        }
        return false
    }

    watch(query, pushToUrl)

    return {
        // state
        query, filters, source, builderCompatible, builderIncompatibleReason,
        tasks, totalElements, totalPages, page, size, isLoading, error,
        hasQuery, activeFilterCount,
        // görsel koşullar
        addFilter, removeFilter, setFilters, clearAll,
        // stql
        setQuery, validate,
        // çalıştırma
        run, setPage, restoreFromUrl,
    }
}
