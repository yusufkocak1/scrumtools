/**
 * widgetExport.js
 *
 * Widget'tan CSV ve PNG indirme (bkz. RICH_FILTER_PLAN.md — Ö7).
 *
 * Tamamı istemcide: veri zaten ekranda duruyor, sunucuya ikinci bir tur atıp
 * aynı sorguyu yeniden çalıştırmak hem gereksiz hem de indirilen dosyanın
 * ekrandakinden farklı çıkma ihtimali demek olurdu. İndirilen şey, o an
 * <b>görülen</b> şeydir.
 */

/** Excel'in UTF-8'i doğru açması için BOM — Türkçe karakterler bozulmasın. */
const BOM = '﻿'

/**
 * Bir hücreyi CSV'ye kaçırır.
 *
 * Ayraç olarak noktalı virgül kullanılıyor: Türkçe yerelde Excel virgülü ondalık
 * ayracı sayar ve virgüllü CSV tek sütuna yapışır.
 */
function cell(value) {
    if (value === null || value === undefined) return ''
    const text = String(value)
    if (/[";\n\r]/.test(text)) return `"${text.replace(/"/g, '""')}"`
    return text
}

/**
 * Satırları CSV olarak indirir.
 * @param {string} filename — uzantısız ad
 * @param {string[]} headers
 * @param {Array<Array<*>>} rows
 */
export function downloadCsv(filename, headers, rows) {
    const lines = [headers.map(cell).join(';')]
    rows.forEach(row => lines.push(row.map(cell).join(';')))

    download(
        new Blob([BOM + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' }),
        `${safeName(filename)}.csv`
    )
}

/**
 * Grafiği PNG olarak indirir.
 *
 * @param {object} chartRef — vue-chartjs bileşen referansı
 * @param {string} filename
 *
 * Chart.js şeffaf zeminle çizer; PNG doğrudan alınırsa koyu temalı bir belgeye
 * yapıştırıldığında yazılar okunmaz olur. Bu yüzden beyaz zemin üzerine
 * kopyalanıp öyle indiriliyor.
 */
export function downloadChartPng(chartRef, filename) {
    const source = canvasOf(chartRef)
    if (!source) return false

    const target = document.createElement('canvas')
    target.width = source.width
    target.height = source.height

    const ctx = target.getContext('2d')
    ctx.fillStyle = '#FFFFFF'
    ctx.fillRect(0, 0, target.width, target.height)
    ctx.drawImage(source, 0, 0)

    target.toBlob(blob => {
        if (blob) download(blob, `${safeName(filename)}.png`)
    })
    return true
}

/** vue-chartjs referansından altındaki canvas'ı bulur. */
function canvasOf(chartRef) {
    const component = chartRef?.value ?? chartRef
    if (!component) return null
    if (component.chart?.canvas) return component.chart.canvas
    if (component.$el?.tagName === 'CANVAS') return component.$el
    return component.$el?.querySelector?.('canvas') ?? null
}

function download(blob, filename) {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    // Tarayıcı indirmeyi kuyruğa aldıktan sonra bırakılır; hemen iptal edilirse
    // büyük dosyalarda indirme yarıda kalabiliyor.
    setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** Dosya adında sorun çıkaran karakterleri temizler. */
function safeName(name) {
    return String(name || 'rapor').trim().replace(/[\\/:*?"<>|]+/g, '-').slice(0, 80) || 'rapor'
}
