/**
 * useSmartColoring.js
 *
 * Board ve liste kartlarını bir zengin filtrenin akıllı filtrelerine göre
 * renklendirir (bkz. RICH_FILTER_PLAN.md — Ö2).
 *
 * Panoda tanımlanan kategoriler günlük çalışma ekranlarına taşınır: "hangi işler
 * takılı kaldı" sorusunun cevabı dashboard'a gitmeden, board'un üzerinde görünür.
 *
 * Burada zengin filtre <b>filtrelemez</b>, yalnız sınıflandırır. Board kendi
 * görevlerini kendi sorgusuyla getirir; sunucudan sadece "bu görevler hangi
 * kategoriye düşüyor" sorulur. Filtreleme de yapsaydı, board'un sorgu çubuğu ile
 * renklendirme seçimi birbiriyle yarışan iki filtre olurdu.
 *
 * Sınıflandırma sunucuda, panodakiyle <b>aynı</b> {@code CASE WHEN} ifadesiyle
 * yapılır: karttaki renk ile grafikteki dilim aynı kuraldan gelir.
 */

import { ref, computed, watch } from 'vue'
import { getRichFilters, classifyTasks } from '../api/RichFilterApi.js'

/** Seçim takım bazında hatırlanır: her açılışta yeniden seçmek angarya olurdu. */
const storageKey = (teamId) => `scrumtools.smartColoring.${teamId}`

/**
 * @param {import('vue').Ref<string>} teamId
 * @param {import('vue').Ref<Array>} tasks — o an ekranda olan görevler
 * @param {import('vue').Ref<string|null>} [projectId]
 */
export function useSmartColoring(teamId, tasks, projectId = ref(null)) {
    const richFilters = ref([])
    const selectedId = ref('')
    const tags = ref({})
    const loading = ref(false)

    const selected = computed(() =>
        richFilters.value.find(f => f.id === selectedId.value) || null
    )

    /** Efsane: seçili filtrenin akıllı filtreleri, sıra korunarak. */
    const legend = computed(() =>
        (selected.value?.elements || []).filter(e => e.kind === 'SMART_FILTER')
    )

    const isActive = computed(() => !!selectedId.value && !!selected.value)

    /** Görev id → { id, name, color } | null */
    const tagOf = (task) => (task ? tags.value[task.id] ?? null : null)

    async function loadFilters() {
        if (!teamId.value) return
        try {
            richFilters.value = await getRichFilters(teamId.value, projectId.value || null)
        } catch {
            richFilters.value = []
        }
        restoreSelection()
    }

    function restoreSelection() {
        const saved = safeRead(storageKey(teamId.value))
        // Kayıtlı filtre silinmiş ya da paylaşımdan çıkmış olabilir; sessizce kapanır.
        selectedId.value = richFilters.value.some(f => f.id === saved) ? saved : ''
    }

    function select(id) {
        selectedId.value = id || ''
        safeWrite(storageKey(teamId.value), selectedId.value)
        if (!selectedId.value) tags.value = {}
        else refresh()
    }

    async function refresh() {
        if (!isActive.value || !teamId.value) {
            tags.value = {}
            return
        }
        const ids = (tasks.value || []).map(t => t.id).filter(Boolean)
        if (!ids.length) {
            tags.value = {}
            return
        }

        loading.value = true
        try {
            tags.value = await classifyTasks(teamId.value, selectedId.value, ids, projectId.value || null)
        } catch {
            // Sınıflandırma başarısızsa kartlar renksiz kalır; board çalışmaya devam eder.
            tags.value = {}
        } finally {
            loading.value = false
        }
    }

    // Görev listesi değiştikçe yeniden sınıflandırılır. İzlenen şey id dizisi:
    // aynı görevler yeniden yüklendiğinde gereksiz istek gitmesin.
    watch(
        () => (tasks.value || []).map(t => t.id).join(','),
        () => { if (isActive.value) refresh() }
    )

    watch(teamId, () => {
        tags.value = {}
        loadFilters()
    })

    return { richFilters, selectedId, selected, legend, tags, tagOf, isActive, loading, loadFilters, select, refresh }
}

function safeRead(key) {
    try {
        return localStorage.getItem(key) || ''
    } catch {
        return ''
    }
}

function safeWrite(key, value) {
    try {
        if (value) localStorage.setItem(key, value)
        else localStorage.removeItem(key)
    } catch {
        // Gizli sekmede localStorage yazılamayabilir; seçim o oturumda yaşar.
    }
}
