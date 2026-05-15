/**
 * useTaskFilter.js — Faz 4
 * Filtre state yönetimi: aktif filtreler, sıralama, sayfalama
 */

import { ref, computed } from 'vue'
import { filterTasks } from '../api/WorkApi.js'

export function useTaskFilter(teamId) {
    // Aktif filtre listesi: [{ field, operator, values }]
    const activeFilters = ref([])

    const sortBy  = ref('createdAt')
    const sortDir = ref('desc')
    const page    = ref(0)
    const size    = ref(50)

    const isLoading    = ref(false)
    const tasks        = ref([])
    const totalElements = ref(0)
    const totalPages   = ref(0)

    const hasFilters = computed(() => activeFilters.value.length > 0)

    // ─── Filtre Ekleme / Çıkarma ──────────────────────────────────────────────

    function addFilter(filter) {
        // Aynı field varsa güncelle
        const idx = activeFilters.value.findIndex(f => f.field === filter.field)
        if (idx >= 0) {
            activeFilters.value[idx] = filter
        } else {
            activeFilters.value.push(filter)
        }
    }

    function removeFilter(field) {
        activeFilters.value = activeFilters.value.filter(f => f.field !== field)
    }

    function clearFilters() {
        activeFilters.value = []
        page.value = 0
    }

    // ─── Yükleme ──────────────────────────────────────────────────────────────

    async function fetchFiltered() {
        if (!teamId) return
        isLoading.value = true
        try {
            const result = await filterTasks(teamId, {
                filters: activeFilters.value,
                sortBy: sortBy.value,
                sortDir: sortDir.value,
                page: page.value,
                size: size.value
            })
            tasks.value        = result.content
            totalElements.value = result.totalElements
            totalPages.value   = result.totalPages
        } catch (e) {
            console.error('Task filter hatası:', e)
        } finally {
            isLoading.value = false
        }
    }

    function setSort(field, dir = 'asc') {
        sortBy.value  = field
        sortDir.value = dir
        page.value    = 0
        fetchFiltered()
    }

    function setPage(p) {
        page.value = p
        fetchFiltered()
    }

    return {
        activeFilters,
        sortBy,
        sortDir,
        page,
        size,
        isLoading,
        tasks,
        totalElements,
        totalPages,
        hasFilters,
        addFilter,
        removeFilter,
        clearFilters,
        fetchFiltered,
        setSort,
        setPage,
    }
}

