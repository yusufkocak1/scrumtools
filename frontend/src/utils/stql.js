/**
 * stql.js — STQL metni ile görsel filtre koşulları arasında çeviri.
 *
 * Arayüzün "Basit" ve "STQL" sekmeleri arasındaki geçişi bu dosya sağlar:
 *  - filtersToStql: her zaman çalışır (koşullar düz AND zinciridir)
 *  - stqlToFilters: yalnız düz AND zinciri + görsel düzenleyicinin desteklediği
 *    operatörler için çalışır. OR, NOT, parantez veya fonksiyon içeren sorgular
 *    görsel düzenleyiciye sığmaz; bu durumda sekme salt-okunur uyarıya düşer.
 *
 * Not: sorgunun tek geçerli yorumcusu backend'dir. Buradaki çözümleme yalnızca
 * arayüz sekmelerini eşitlemek içindir; doğrulama her zaman sunucudan gelir.
 */

const RESERVED = ['AND', 'OR', 'NOT', 'IN', 'IS', 'EMPTY', 'NULL', 'ORDER', 'BY', 'ASC', 'DESC']

/** Görsel düzenleyicinin desteklediği operatörler — STQL sembolü ↔ eski ad. */
const OPERATOR_TO_SYMBOL = {
    eq: '=',
    neq: '!=',
    contains: '~',
    in: 'IN',
    gt: '>',
    lt: '<',
    is_null: 'IS EMPTY',
    is_not_null: 'IS NOT EMPTY',
}

const SYMBOL_TO_OPERATOR = {
    '=': 'eq',
    '!=': 'neq',
    '<>': 'neq',
    '~': 'contains',
    'IN': 'in',
    '>': 'gt',
    '<': 'lt',
    'IS EMPTY': 'is_null',
    'IS NOT EMPTY': 'is_not_null',
}

const UNARY = ['is_null', 'is_not_null']

// ─── filters[] → STQL ─────────────────────────────────────────────────────────

/**
 * Görsel koşulları STQL metnine çevirir.
 * @param {Array<{field: string, operator: string, values: string[]}>} filters
 * @returns {string}
 */
export function filtersToStql(filters) {
    if (!filters?.length) return ''

    return filters
        .map(f => {
            const symbol = OPERATOR_TO_SYMBOL[f.operator]
            if (!symbol) return null

            if (UNARY.includes(f.operator)) return `${f.field} ${symbol}`

            const values = (f.values || []).filter(v => v !== null && v !== undefined && v !== '')
            if (!values.length) return null

            if (f.operator === 'in') {
                return `${f.field} IN (${values.map(quote).join(', ')})`
            }
            return `${f.field} ${symbol} ${quote(values[0])}`
        })
        .filter(Boolean)
        .join(' AND ')
}

/** Basit değerler tırnaksız kalır; boşluklu, özel karakterli veya anahtar kelime olanlar tırnaklanır. */
export function quote(raw) {
    const value = String(raw ?? '')
    const simple = value.length > 0 && /^[\p{L}\p{N}_-]+$/u.test(value)
    const reserved = RESERVED.includes(value.toUpperCase())
    if (simple && !reserved) return value
    return `"${value.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`
}

// ─── STQL → filters[] ─────────────────────────────────────────────────────────

/**
 * STQL metnini görsel koşullara çevirmeyi dener.
 * @param {string} stql
 * @returns {{convertible: boolean, filters: Array, reason: string|null}}
 */
export function stqlToFilters(stql) {
    const text = (stql || '').trim()
    if (!text) return { convertible: true, filters: [], reason: null }

    let tokens
    try {
        tokens = tokenize(text)
    } catch {
        return fail('Sorgu çözümlenemedi')
    }

    // ORDER BY görsel düzenleyicide karşılığı olmadığı için ayrıştırılıp atılır.
    const orderIdx = tokens.findIndex((t, i) =>
        t.type === 'word' && t.value.toUpperCase() === 'ORDER'
        && tokens[i + 1]?.type === 'word' && tokens[i + 1].value.toUpperCase() === 'BY')
    if (orderIdx >= 0) tokens = tokens.slice(0, orderIdx)

    // OR, NOT ve gruplama parantezi ayrıca elenmez: readCondition ve aşağıdaki
    // bağlaç kontrolü bunları zaten reddeder. Böylece "IS NOT EMPTY" gibi içinde
    // NOT geçen ama görsel modda karşılığı olan koşullar yanlışlıkla elenmez.
    const filters = []
    let i = 0
    while (i < tokens.length) {
        const parsed = readCondition(tokens, i)
        if (!parsed) return fail('Görsel düzenleyicinin desteklemediği bir koşul var')

        filters.push(parsed.filter)
        i = parsed.next

        if (i >= tokens.length) break
        const conjunction = tokens[i]
        if (conjunction.type !== 'word' || conjunction.value.toUpperCase() !== 'AND') {
            return fail('Yalnızca AND ile bağlanan koşullar görsel düzenleyicide gösterilebilir')
        }
        i++
    }

    // Görsel düzenleyici alan başına tek koşul tutar; aynı alan tekrarlanıyorsa sığmaz.
    const seen = new Set()
    for (const f of filters) {
        if (seen.has(f.field)) {
            return fail('Aynı alan için birden fazla koşul görsel düzenleyicide gösterilemiyor')
        }
        seen.add(f.field)
    }

    return { convertible: true, filters, reason: null }
}

function fail(reason) {
    return { convertible: false, filters: [], reason }
}

/** Tek bir "alan operatör değer" üçlüsünü okur. */
function readCondition(tokens, start) {
    const fieldToken = tokens[start]
    if (!fieldToken || fieldToken.type !== 'word') return null

    // cf[...] özel alanı görsel düzenleyicide temsil edilemiyor.
    if (fieldToken.value.includes('[')) return null

    let i = start + 1
    const opToken = tokens[i]
    if (!opToken) return null

    let operator = null

    if (opToken.type === 'op') {
        operator = SYMBOL_TO_OPERATOR[opToken.value]
        i++
    } else if (opToken.type === 'word' && opToken.value.toUpperCase() === 'IN') {
        operator = 'in'
        i++
    } else if (opToken.type === 'word' && opToken.value.toUpperCase() === 'IS') {
        i++
        let negated = false
        if (tokens[i]?.type === 'word' && tokens[i].value.toUpperCase() === 'NOT') {
            negated = true
            i++
        }
        const kw = tokens[i]?.value?.toUpperCase()
        if (kw !== 'EMPTY' && kw !== 'NULL') return null
        i++
        return { filter: { field: fieldToken.value, operator: negated ? 'is_not_null' : 'is_null', values: [] }, next: i }
    }

    if (!operator) return null

    if (operator === 'in') {
        if (tokens[i]?.value !== '(') return null
        i++
        const values = []
        while (tokens[i] && tokens[i].value !== ')') {
            if (tokens[i].value === ',') { i++; continue }
            if (tokens[i].type === 'op') return null
            values.push(tokens[i].value)
            i++
        }
        if (tokens[i]?.value !== ')') return null
        i++
        if (!values.length) return null
        return { filter: { field: fieldToken.value, operator, values }, next: i }
    }

    const valueToken = tokens[i]
    if (!valueToken || valueToken.type === 'op') return null
    // Fonksiyon çağrısı — görsel düzenleyicide karşılığı yok.
    if (tokens[i + 1]?.value === '(') return null

    return { filter: { field: fieldToken.value, operator, values: [valueToken.value] }, next: i + 1 }
}

/**
 * Küçük sözcüksel çözümleyici — backend lexer'ının arayüz karşılığı.
 * Amaç tam bir dil desteği değil, sekme senkronu için yeterli ayrıştırma.
 */
function tokenize(input) {
    const tokens = []
    let i = 0

    while (i < input.length) {
        const ch = input[i]

        if (/\s/.test(ch)) { i++; continue }

        if (ch === '"' || ch === "'") {
            const quoteChar = ch
            let value = ''
            i++
            while (i < input.length && input[i] !== quoteChar) {
                if (input[i] === '\\' && i + 1 < input.length) {
                    i++
                    value += input[i] === 'n' ? '\n' : input[i]
                } else {
                    value += input[i]
                }
                i++
            }
            if (i >= input.length) throw new Error('Kapatılmamış tırnak')
            i++
            tokens.push({ type: 'string', value })
            continue
        }

        if ('()[],'.includes(ch)) {
            tokens.push({ type: 'punct', value: ch })
            i++
            continue
        }

        const twoChar = input.slice(i, i + 2)
        if (['!=', '!~', '>=', '<=', '<>'].includes(twoChar)) {
            tokens.push({ type: 'op', value: twoChar })
            i += 2
            continue
        }
        if ('=~<>'.includes(ch)) {
            tokens.push({ type: 'op', value: ch })
            i++
            continue
        }

        // Tırnaksız kelime/sayı — alan adı, anahtar kelime veya basit değer.
        let word = ''
        while (i < input.length && !/[\s()[\],=~<>!]/.test(input[i])) {
            word += input[i]
            i++
        }
        if (!word) throw new Error(`Beklenmeyen karakter: ${ch}`)
        tokens.push({ type: 'word', value: word })
    }

    return tokens
}

// ─── Yardımcılar ──────────────────────────────────────────────────────────────

/**
 * Sorgunun ORDER BY bölümünü değiştirir veya ekler.
 * Tablo sütun başlığına tıklandığında sıralamayı sorguya yansıtmak için kullanılır —
 * böylece sıralama da paylaşılabilir linkin parçası olur.
 *
 * @param {string} query
 * @param {string} field — STQL alan adı
 * @param {'asc'|'desc'} dir
 */
export function withOrderBy(query, field, dir = 'asc') {
    const base = stripOrderBy(query)
    const clause = `ORDER BY ${field}${dir === 'desc' ? ' DESC' : ''}`
    return base ? `${base} ${clause}` : clause
}

/** Sorgudan ORDER BY bölümünü çıkarır. */
export function stripOrderBy(query) {
    return (query || '').replace(/\s*\bORDER\s+BY\b[\s\S]*$/i, '').trim()
}

/** Sorgudaki ORDER BY'ın ilk maddesini okur; yoksa null. */
export function readOrderBy(query) {
    const match = (query || '').match(/\bORDER\s+BY\s+([\p{L}\p{N}_[\].]+)(\s+(ASC|DESC))?/iu)
    if (!match) return null
    return { field: match[1], dir: (match[3] || 'asc').toLowerCase() }
}

/** Hata konumunu metin içinde işaretleyen okunabilir gösterim üretir. */
export function highlightError(query, error) {
    if (!error || typeof error.position !== 'number') return null
    const start = Math.max(0, Math.min(error.position, query.length))
    const end = Math.min(query.length, start + (error.length || 1))
    return {
        before: query.slice(0, start),
        error: query.slice(start, end) || ' ',
        after: query.slice(end),
    }
}
