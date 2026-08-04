/**
 * useRichFilterContext.js
 *
 * Aynı zengin filtreye bağlı widget'ların paylaştığı seçim durumu — dashboard'daki
 * çapraz filtrelemenin kalbi. Bir grafikte "Test" dilimine tıklamak, sayfadaki
 * bütün widget'ları o dilime daraltır.
 *
 * Jira'da bu, dashboard'a "controller" gadget'ının eklenmiş olmasını şart koşar;
 * unutulursa widget'lar sessizce statik kalır. Burada durum modül seviyesinde
 * tutulur: kontrolcü widget'ı yalnızca bu durumun **görünen yüzü**dür, olmasa da
 * grafikten tıklayarak filtreleme çalışır (bkz. RICH_FILTER_PLAN.md — K10).
 *
 * Seçim URL'ye de yansır (K11): daraltılmış bir dashboard olduğu gibi paylaşılabilir.
 */

import { reactive, computed, unref, watchEffect } from 'vue'
import { getRichFilter } from '../api/RichFilterApi.js'

/** richFilterId → { selection, definition, loading, error } */
const contexts = reactive({})

const emptySelection = () => ({
    /** Seçili akıllı filtre id'leri — aralarında OR. */
    smart: [],
    /** Arama kutusu. */
    text: '',
    /** Sabit filtreler: öğe id → seçenek id (tek seçim). */
    static: {},
    /** Dinamik filtreler: öğe id → değerler (kontrol içinde OR). */
    dynamic: {},
    /** Uygulanmış görünüm — yalnız arayüzde "hangi görünümdeyiz" göstergesi. */
    viewId: null,
})

function ensure(richFilterId) {
    if (!contexts[richFilterId]) {
        contexts[richFilterId] = {
            selection: emptySelection(),
            definition: null,
            loading: false,
            error: null,
        }
    }
    return contexts[richFilterId]
}

/**
 * @param {import('vue').Ref<string>|(() => string)|string} richFilterId
 */
export function useRichFilterContext(richFilterId) {
    const id = computed(() =>
        typeof richFilterId === 'function' ? richFilterId() : unref(richFilterId)
    )

    // Kayıt oluşturmak bir yan etkidir; computed içinde yapılırsa Vue'nun
    // bağımlılık takibi kendi kendini tetikler. Bu yüzden hazırlama burada,
    // okuma aşağıdaki computed'de.
    watchEffect(() => {
        if (id.value) ensure(id.value)
    })

    const state = computed(() => (id.value ? contexts[id.value] ?? null : null))
    const definition = computed(() => state.value?.definition ?? null)
    const loading = computed(() => state.value?.loading ?? false)
    const error = computed(() => state.value?.error ?? null)

    const elementsOf = (kind) =>
        computed(() => (definition.value?.elements ?? []).filter(e => e.kind === kind))

    const smartFilters = elementsOf('SMART_FILTER')
    const staticFilters = elementsOf('STATIC_FILTER')
    const dynamicFilters = elementsOf('DYNAMIC_FILTER')
    const views = elementsOf('VIEW')

    const selection = computed(() => state.value?.selection ?? emptySelection())
    const selectedSmart = computed(() => selection.value.smart)
    const text = computed(() => selection.value.text)
    const staticSelections = computed(() => selection.value.static)
    const dynamicSelections = computed(() => selection.value.dynamic)
    const viewId = computed(() => selection.value.viewId)

    /** Sunucuya gönderilecek seçim nesnesi — ham sorgu değil, id ve değer listeleri. */
    const payload = computed(() => ({
        smart: [...selectedSmart.value],
        text: text.value || null,
        staticSelections: { ...staticSelections.value },
        dynamic: { ...dynamicSelections.value },
    }))

    /**
     * Widget'ların izleyeceği imza. Seçim nesnesini derin izlemek yerine bunu
     * izlemek, aynı seçimle gereksiz yeniden çekmeyi önler.
     */
    const signature = computed(() => JSON.stringify([
        id.value, selectedSmart.value, text.value, staticSelections.value, dynamicSelections.value,
    ]))

    const activeCount = computed(() =>
        selectedSmart.value.length
        + (text.value ? 1 : 0)
        + Object.values(staticSelections.value).filter(Boolean).length
        + Object.values(dynamicSelections.value).filter(v => v?.length).length
    )

    const isFiltered = computed(() => activeCount.value > 0)

    /** Zengin filtre tanımını bir kez yükler; aynı id'yi kullanan widget'lar paylaşır. */
    async function loadDefinition(teamId, { force = false } = {}) {
        if (!id.value || !teamId) return null
        const ctx = ensure(id.value)
        if (ctx.definition && !force) return ctx.definition
        if (ctx.loading) return ctx.definition

        ctx.loading = true
        ctx.error = null
        try {
            ctx.definition = await getRichFilter(teamId, id.value)
            applyDefaultView(ctx)
        } catch (e) {
            // 404/403: filtre silinmiş ya da paylaşımı kaldırılmış. Widget bunu
            // "yetim" kartına dönüşerek gösterir, sessizce boş kalmaz (K15).
            ctx.error = e?.response?.status === 404 || e?.response?.status === 403
                ? 'missing'
                : 'error'
        } finally {
            ctx.loading = false
        }
        return ctx.definition
    }

    /**
     * Varsayılan görünüm yalnız kullanıcı hiçbir şey seçmemişken uygulanır:
     * paylaşılan linkteki seçimi ezmek, linki anlamsız kılardı.
     */
    function applyDefaultView(ctx) {
        const selected = ctx.selection
        const untouched = !selected.smart.length && !selected.text
            && !Object.keys(selected.static).length && !Object.keys(selected.dynamic).length
        if (!untouched) return

        const fallback = (ctx.definition?.elements ?? [])
            .find(e => e.kind === 'VIEW' && e.config?.default)
        if (fallback) applySelection(ctx, fallback.config?.selection, fallback.id)
    }

    // ─── Seçim işlemleri ──────────────────────────────────────────────────────

    function toggleSmart(smartId) {
        const ctx = current()
        if (!ctx) return
        const index = ctx.selection.smart.indexOf(smartId)
        if (index >= 0) ctx.selection.smart.splice(index, 1)
        else ctx.selection.smart.push(smartId)
        ctx.selection.viewId = null
    }

    function setSmart(ids) {
        const ctx = current()
        if (!ctx) return
        ctx.selection.smart = [...(ids || [])]
        ctx.selection.viewId = null
    }

    function setText(value) {
        const ctx = current()
        if (!ctx) return
        ctx.selection.text = value || ''
        ctx.selection.viewId = null
    }

    /** Sabit filtre: tek seçim. Aynı seçeneğe tekrar tıklamak seçimi kaldırır. */
    function setStatic(elementId, optionId) {
        const ctx = current()
        if (!ctx) return
        if (!optionId || ctx.selection.static[elementId] === optionId) {
            delete ctx.selection.static[elementId]
        } else {
            ctx.selection.static[elementId] = optionId
        }
        ctx.selection.viewId = null
    }

    function toggleDynamic(elementId, value) {
        const ctx = current()
        if (!ctx) return
        const values = ctx.selection.dynamic[elementId] ?? []
        const index = values.indexOf(value)
        const next = index >= 0
            ? values.filter(v => v !== value)
            : [...values, value]

        if (next.length) ctx.selection.dynamic[elementId] = next
        else delete ctx.selection.dynamic[elementId]
        ctx.selection.viewId = null
    }

    function clearDynamic(elementId) {
        const ctx = current()
        if (!ctx) return
        delete ctx.selection.dynamic[elementId]
        ctx.selection.viewId = null
    }

    /** Görünümü uygular — kayıtlı seçimin tamamı yerine geçer. */
    function applyView(view) {
        const ctx = current()
        if (!ctx) return
        if (!view) {
            clear()
            return
        }
        applySelection(ctx, view.config?.selection, view.id)
    }

    function clear() {
        const ctx = current()
        if (!ctx) return
        ctx.selection = emptySelection()
    }

    /** Görünüm olarak kaydedilecek seçim — sunucuya `config.selection` olarak gider. */
    function currentSelectionSnapshot() {
        return {
            smart: [...selectedSmart.value],
            text: text.value || '',
            static: { ...staticSelections.value },
            dynamic: { ...dynamicSelections.value },
        }
    }

    function current() {
        return id.value ? ensure(id.value) : null
    }

    return {
        id, definition, loading, error,
        smartFilters, staticFilters, dynamicFilters, views,
        selection, selectedSmart, text, staticSelections, dynamicSelections, viewId,
        payload, signature, isFiltered, activeCount,
        loadDefinition,
        toggleSmart, setSmart, setText, setStatic, toggleDynamic, clearDynamic,
        applyView, clear, currentSelectionSnapshot,
    }
}

/** Kayıtlı bir seçimi bağlama yazar. */
function applySelection(ctx, saved, viewId = null) {
    const next = emptySelection()
    if (saved) {
        next.smart = Array.isArray(saved.smart) ? [...saved.smart] : []
        next.text = saved.text || ''
        next.static = { ...(saved.static || {}) }
        next.dynamic = { ...(saved.dynamic || {}) }
    }
    next.viewId = viewId
    ctx.selection = next
}

// ─── URL senkronu ────────────────────────────────────────────────────────────
//
// Seçim tek bir parametreye kodlanır: `?rf=<id>&rfsel=<base64url JSON>`.
// Faz 2'de okunabilir `smart=`/`rfq=` kullanılıyordu; dinamik filtre değerleri
// (e-posta, etiket, sürüm adı) ayraçlarla çakıştığı için tek ve kaçışsız bir
// kodlamaya geçildi. Dashboard'da birden çok zengin filtre bulunabilir; hepsini
// URL'ye sığdırmak yerine kullanıcının fiilen daralttığı filtre paylaşılır —
// pratikte bir panoda tek kontrolcü olur.

function encode(value) {
    const json = JSON.stringify(value)
    return btoa(String.fromCharCode(...new TextEncoder().encode(json)))
        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function decode(encoded) {
    const base64 = String(encoded).replace(/-/g, '+').replace(/_/g, '/')
    const binary = atob(base64)
    const bytes = Uint8Array.from(binary, c => c.charCodeAt(0))
    return JSON.parse(new TextDecoder().decode(bytes))
}

/** Daraltılmış ilk zengin filtrenin URL parametreleri; hiçbiri daraltılmamışsa boş. */
export function selectionToQuery() {
    for (const [richFilterId, ctx] of Object.entries(contexts)) {
        const { smart, text, static: statics, dynamic } = ctx.selection
        const empty = !smart.length && !text
            && !Object.keys(statics).length && !Object.keys(dynamic).length
        if (empty) continue

        return {
            rf: richFilterId,
            rfsel: encode({ smart, text, static: statics, dynamic }),
        }
    }
    return {}
}

/** URL'deki seçimi geri yükler. Sayfa açılışında bir kez çağrılır. */
export function applySelectionFromQuery(query) {
    const richFilterId = query?.rf
    if (!richFilterId) return null

    const ctx = ensure(richFilterId)
    if (!query.rfsel) return richFilterId

    try {
        applySelection(ctx, decode(query.rfsel))
    } catch {
        // Bozuk/eski bir link seçimsiz açılır; hata göstermek kullanıcıya
        // yapabileceği bir şey sunmaz.
    }
    return richFilterId
}
