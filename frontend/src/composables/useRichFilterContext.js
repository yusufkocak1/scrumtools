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

function ensure(richFilterId) {
    if (!contexts[richFilterId]) {
        contexts[richFilterId] = {
            selection: { smart: [], text: '' },
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

    const smartFilters = computed(() =>
        (definition.value?.elements ?? []).filter(e => e.kind === 'SMART_FILTER')
    )

    const selectedSmart = computed(() => state.value?.selection.smart ?? [])
    const text = computed(() => state.value?.selection.text ?? '')

    /** Sunucuya gönderilecek seçim nesnesi — ham sorgu değil, id listesi. */
    const payload = computed(() => ({
        smart: [...selectedSmart.value],
        text: text.value || null,
    }))

    /**
     * Widget'ların izleyeceği imza. Seçim nesnesini derin izlemek yerine bunu
     * izlemek, aynı seçimle gereksiz yeniden çekmeyi önler.
     */
    const signature = computed(() => `${id.value}|${selectedSmart.value.join(',')}|${text.value}`)

    const isFiltered = computed(() => selectedSmart.value.length > 0 || !!text.value)

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

    function toggleSmart(smartId) {
        if (!id.value) return
        const ctx = ensure(id.value)
        const index = ctx.selection.smart.indexOf(smartId)
        if (index >= 0) ctx.selection.smart.splice(index, 1)
        else ctx.selection.smart.push(smartId)
    }

    function setSmart(ids) {
        if (!id.value) return
        ensure(id.value).selection.smart = [...(ids || [])]
    }

    function setText(value) {
        if (!id.value) return
        ensure(id.value).selection.text = value || ''
    }

    function clear() {
        if (!id.value) return
        const ctx = ensure(id.value)
        ctx.selection.smart = []
        ctx.selection.text = ''
    }

    return {
        id, definition, loading, error, smartFilters,
        selectedSmart, text, payload, signature, isFiltered,
        loadDefinition, toggleSmart, setSmart, setText, clear,
    }
}

// ─── URL senkronu ────────────────────────────────────────────────────────────
//
// Tek bir zengin filtrenin seçimi URL'ye yazılır: `?rf=<id>&smart=a,b&rfq=metin`.
// Dashboard'da birden çok zengin filtre bulunabilir; hepsini URL'ye sığdırmak
// yerine kullanıcının fiilen daralttığı filtre paylaşılır — pratikte bir panoda
// tek bir kontrolcü olur.

/** Daraltılmış ilk zengin filtrenin URL parametreleri; hiçbiri daraltılmamışsa boş. */
export function selectionToQuery() {
    for (const [richFilterId, ctx] of Object.entries(contexts)) {
        const { smart, text } = ctx.selection
        if (!smart.length && !text) continue
        return {
            rf: richFilterId,
            ...(smart.length ? { smart: smart.join(',') } : {}),
            ...(text ? { rfq: text } : {}),
        }
    }
    return {}
}

/** URL'deki seçimi geri yükler. Sayfa açılışında bir kez çağrılır. */
export function applySelectionFromQuery(query) {
    const richFilterId = query?.rf
    if (!richFilterId) return null

    const ctx = ensure(richFilterId)
    ctx.selection.smart = query.smart ? String(query.smart).split(',').filter(Boolean) : []
    ctx.selection.text = query.rfq ? String(query.rfq) : ''
    return richFilterId
}
