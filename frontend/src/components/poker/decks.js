/**
 * Scrum Poker kart desteleri.
 *
 * Deste id'leri backend'in beyaz listesiyle eşleşmeli
 * (ScrumPokerService.ALLOWED_CARD_TYPES). Kartların kendisi yalnızca burada tanımlıdır;
 * backend sadece hangi destenin seçili olduğunu tutar.
 *
 * numeric  : kart değeri doğrudan sayı mı? (ortalama gösterilip gösterilmeyeceğini belirler)
 * points   : sayısal olmayan destelerde kartın story point karşılığı — görev puanlaması için
 * unsure   : "bilmiyorum" kartı; istatistiklere katılmaz
 */

export const UNSURE_CARD = '?'

export const DECKS = [
    {
        id: 'fibonacci',
        label: 'Fibonacci',
        hint: '1 · 2 · 3 · 5 · 8',
        numeric: true,
        cards: ['1', '2', '3', '5', '8', '13', '21', '34', '55', '89', UNSURE_CARD]
    },
    {
        id: 'tshirt',
        label: 'Tişört Bedeni',
        hint: 'XS · S · M · L · XL',
        numeric: false,
        cards: ['XS', 'S', 'M', 'L', 'XL', 'XXL', UNSURE_CARD],
        // Beden → story point karşılığı; "Puanı Göreve İşle" panelinin önerileri buradan çıkar
        points: { XS: 1, S: 2, M: 3, L: 5, XL: 8, XXL: 13 }
    },
    {
        id: 'powers',
        label: "2'nin Katları",
        hint: '1 · 2 · 4 · 8 · 16',
        numeric: true,
        cards: ['1', '2', '4', '8', '16', '32', '64', UNSURE_CARD]
    }
]

const BY_ID = new Map(DECKS.map(d => [d.id, d]))

export const DEFAULT_DECK_ID = 'fibonacci'

/** Bilinmeyen/boş id gelirse varsayılan desteye düşer — masa asla boş kalmaz. */
export const getDeck = (id) => BY_ID.get(id) || BY_ID.get(DEFAULT_DECK_ID)

/**
 * Kartın sayısal karşılığı: sayısal destelerde değerin kendisi,
 * beden destesinde points eşlemesi. Oy yoksa / "?" ise null.
 */
export const cardPoints = (deck, card) => {
    if (card === null || card === undefined) return null
    const value = String(card).trim()
    if (value === '' || value === '-' || value === UNSURE_CARD) return null

    if (deck?.points && Object.prototype.hasOwnProperty.call(deck.points, value)) {
        return deck.points[value]
    }

    const parsed = parseFloat(value)
    return isNaN(parsed) ? null : parsed
}

/**
 * Kartın destedeki sırası — "S – XL" gibi aralıkları doğru sıralamak için.
 * Destede olmayan kart (deste değişimi sırasında kalmış oy) sona atılır.
 */
export const cardOrder = (deck, card) => {
    const index = deck?.cards?.indexOf(String(card))
    return index === undefined || index < 0 ? Number.MAX_SAFE_INTEGER : index
}
