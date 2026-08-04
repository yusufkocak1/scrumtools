/**
 * Masada oyuncuların birbirine fırlatabildiği objeler.
 *
 * id → backend'in beyaz listesindeki ASCII token (ScrumPokerService.ALLOWED_THROWS).
 * Emoji yalnızca burada yaşar; ağdan/DB'den hiç geçmez, böylece kodlama sorunu olmaz.
 * Yeni bir obje eklerken backend beyaz listesine de eklemek gerekir.
 */
export const THROWABLES = [
    { id: 'heart',  emoji: '❤️', label: 'Kalp' },
    { id: 'clap',   emoji: '👏', label: 'Alkış' },
    { id: 'fire',   emoji: '🔥', label: 'Ateş' },
    { id: 'party',  emoji: '🎉', label: 'Konfeti' },
    { id: 'arrow',  emoji: '🏹', label: 'Ok' },
    { id: 'paper',  emoji: '📄', label: 'Kağıt' },
    { id: 'tomato', emoji: '🍅', label: 'Domates' },
    { id: 'coffee', emoji: '☕', label: 'Kahve' }
]

const BY_ID = new Map(THROWABLES.map(t => [t.id, t]))

/** Bilinmeyen token gelirse animasyon yine de çalışsın diye nötr bir emoji döner. */
export const throwableEmoji = (id) => BY_ID.get(id)?.emoji || '✨'

export const throwableLabel = (id) => BY_ID.get(id)?.label || 'Obje'
