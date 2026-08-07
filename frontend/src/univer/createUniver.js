import { Univer, LogLevel, LocaleType, merge } from '@univerjs/core'
import { FUniver } from '@univerjs/core/facade'
import { UniverSheetsCorePreset } from '@univerjs/preset-sheets-core'
import sheetsCoreEnUS from '@univerjs/preset-sheets-core/locales/en-US'
import '@univerjs/preset-sheets-core/lib/index.css'

/**
 * Univer örneği kurar (COLLAB_WORKSPACE_PLAN.md K4 / Faz 3).
 *
 * <p>Resmî yol `@univerjs/presets` paketindeki `createUniver`'dır ama o paket
 * yirmiden fazla preset'i bağımlılık olarak çeker (belge iş birliği, pivot,
 * koşullu biçim…). Bize yalnızca `preset-sheets-core` gerekiyor ve Jenkins
 * ajanı dar bir makine (D3) — bu yüzden yalnız ihtiyacımız olan preset kuruluyor
 * ve otuz satırlık bootstrap burada tutuluyor. Kullanılan her şey belgelenmiş
 * genel API: `new Univer()`, `registerPlugin()`, `FUniver.newAPI()`.
 *
 * Sürümler `package.json`'da **kesin** sabitlenmiştir (R1): Univer'in mutation
 * sözleşmesi ara sürümlerde değişebilir ve köprü ona bağlıdır.
 */
export function createUniver({ container, locale = LocaleType.EN_US, presetOptions = {} }) {
  const preset = UniverSheetsCorePreset({ container, ...presetOptions })

  const univer = new Univer({
    logLevel: LogLevel.WARN,
    locale,
    locales: { [locale]: merge({}, sheetsCoreEnUS) }
  })

  // Preset eklentileri sırayla kaydedilir; aynı eklenti iki kez gelirse
  // sonuncusu geçerli olur (üstteki createUniver'ın davranışı).
  const registered = new Map()
  for (const entry of preset.plugins) {
    const [plugin, options] = Array.isArray(entry) ? entry : [entry, undefined]
    registered.set(plugin.pluginName, { plugin, options })
  }
  registered.forEach(({ plugin, options }) => univer.registerPlugin(plugin, options))

  return { univer, univerAPI: FUniver.newAPI(univer) }
}
