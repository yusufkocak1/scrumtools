/**
 * CRDT hücre anahtarı sözlüğü — `"R{satır}C{sütun}"` (COLLAB_WORKSPACE_PLAN.md §5).
 *
 * <b>Neden ayrı bir dosya:</b> bu iki fonksiyon önce `UniverYjsBridge.js`
 * içindeydi ve oradan içe aktarılıyordu. Ama o modül
 * `@univerjs/preset-sheets-core`'u statik olarak import ediyor — yani tek bir
 * yardımcı fonksiyon için Univer'in tamamı (ve React) çağıranın paketine
 * giriyordu. Docs'un salt-okunur gömme önizlemesi (Y3) tam da Univer'i
 * yüklememek için yazılmıştı; oradan bir import, tasarımın kendisini iptal
 * ediyordu.
 *
 * Burada hiçbir bağımlılık yok, dolayısıyla worker'dan da, Docs'tan da,
 * köprüden de güvenle kullanılabilir.
 */

export function cellKey(row, col) {
  return `R${row}C${col}`
}

export function parseCellKey(key) {
  const separator = key.indexOf('C', 1)
  if (key[0] !== 'R' || separator < 0) return null
  const row = Number(key.slice(1, separator))
  const col = Number(key.slice(separator + 1))
  return Number.isFinite(row) && Number.isFinite(col) ? { row, col } : null
}
