import { ref, computed } from 'vue'
import { CommandType } from '@univerjs/core'
import { COMMAND_MAP, UNSUPPORTED_LABELS } from './recorderMapping.js'

/**
 * Makro kaydedici (COLLAB_WORKSPACE_PLAN.md §9.3).
 *
 * <b>Neden köprünün üstüne "ince bir katman" değil:</b> `UniverYjsBridge` aynı
 * `onCommandExecuted` akışını dinliyor ama `CommandType.MUTATION` dışındakini
 * eliyor — ve bunu bilerek yapıyor, CRDT'ye oturmuş düşük seviye değişiklik
 * yazılmalı. Kaydedici için mutation yanlış seviye: tek bir kullanıcı işlemi
 * birkaç mutation üretir, parametreler dönüşüm sonrasıdır ve "kalın yap" niyet
 * olarak değil tam stil nesnesi olarak gelir. Buradaki filtre bu yüzden
 * `CommandType.COMMAND`.
 *
 * Yalnızca `SHEET` için: TEXT/CODE'da kayıt, son içeriği yazan tek bir
 * `setText` üretirdi — bu makro değil, dokümanın kopyasıdır.
 */
export function useMacroRecorder() {
  const recording = ref(false)
  const steps = ref([])
  const skipped = ref([])

  let disposer = null
  let lastSelection = null

  const stepCount = computed(() => steps.value.length)
  const skippedCount = computed(() => skipped.value.length)

  /**
   * @param univerAPI  Univer Facade
   * @param isApplyingRemote  köprünün "şu an uzak değişiklik uyguluyorum" bayrağı
   */
  function start(univerAPI, isApplyingRemote) {
    if (recording.value || !univerAPI) return
    recording.value = true
    steps.value = []
    skipped.value = []

    const subscription = univerAPI.onCommandExecuted((command) => {
      if (!recording.value) return

      // Uzak değişiklikler köprünün `applyLocally`'si üzerinden geliyor ve o
      // sırada bu bayrak açık. Kaydedilirlerse yanınızda çalışan birinin
      // düzenlemeleri sizin makronuza yazılırdı — en sinsi hata bu olurdu.
      //
      // Aynı bayrak **makro yazımlarını da** kapsıyor: `applyMacroOps` CRDT'ye
      // `'macro'` origin'iyle yazıyor, gözlemciler tetikleniyor ve değişiklik
      // Univer'e yine `applyLocally` üzerinden giriyor. Yani "çalışan makronun
      // yanında kayıt" durumu için ayrı bir kontrol gerekmiyor.
      if (isApplyingRemote?.()) return

      if (command.id === 'univer.command.undo') {
        onUndo()
        return
      }
      if (command.id === 'univer.command.redo') return

      // Seçim bir operation, komut değil; ama aralığı bilmeyen eşleyiciler için
      // son seçim bağlam olarak lazım.
      if (command.id === 'sheet.operation.set-selections') {
        lastSelection = command.params?.selections?.[0]?.range || lastSelection
        return
      }

      if (command.type !== CommandType.COMMAND) return
      record(command)
    })

    disposer = () => subscription?.dispose?.()
  }

  function record(command) {
    const mapper = COMMAND_MAP[command.id]
    if (!mapper) {
      // Sessizce atlamıyoruz (§9.3): kısalmış bir betik, kullanıcının
      // şüphelenmesi için hiçbir sebep bırakmaz.
      const label = UNSUPPORTED_LABELS[command.id] || command.id
      skipped.value.push(label)
      steps.value.push({ comment: `// kaydedilemedi: ${label}` })
      return
    }

    try {
      const result = mapper(command.params, { selection: lastSelection })
      if (!result) {
        const label = UNSUPPORTED_LABELS[command.id] || command.id
        skipped.value.push(label)
        steps.value.push({ comment: `// kaydedilemedi: ${label}` })
        return
      }
      steps.value.push(result)
    } catch (error) {
      // Univer'in parametre biçimi sürümle değişebilir (R1). Kaydedicide bu
      // ölümcül değil: adım düşer, kullanıcı görür.
      console.warn('[makro-kaydedici] komut çevrilemedi', command.id, error)
      skipped.value.push(UNSUPPORTED_LABELS[command.id] || command.id)
      steps.value.push({ comment: `// kaydedilemedi: ${command.id}` })
    }
  }

  /**
   * Geri alma, kaydedicinin tuzağı: kullanıcı bir şey yapar, beğenmez, geri
   * alır. İkisini birden yazarsak betik hatayı da tekrarlar.
   *
   * Ölçüt listenin kendisi, ayrı bir sayaç değil: kayıt her zaman boş listeyle
   * başlıyor, dolayısıyla "liste boşken gelen geri alma" ile "kayıttan önceki
   * bir işlemi geri alıyor" aynı şey. Ayrı sayaç tutmak, `// kaydedilemedi:`
   * satırları sayılmadığı için listeden kayardı ve yanlış adımı sildirirdi.
   */
  function onUndo() {
    if (steps.value.length === 0) {
      stop()
      return
    }
    steps.value.pop()
  }

  function stop() {
    recording.value = false
    disposer?.()
    disposer = null
  }

  /** Kaydı çalıştırılabilir bir makro kaynağına çevirir. */
  function toSource(name = 'Kayıt') {
    const body = steps.value.length
      ? steps.value.map((step) => '  ' + (step.code || step.comment)).join('\n')
      : '  // kaydedilen işlem yok'

    return `// ${name} — makro kaydedicinin ürettiği başlangıç betiği.
// Aralıklar ve değerler sabit yazıldı; tekrar kullanmak için düzenleyin.
const doc = ScrumTools.getActiveDocument();
const sheet = doc.getActiveSheet();

${body}
`
  }

  function reset() {
    stop()
    steps.value = []
    skipped.value = []
  }

  return { recording, steps, skipped, stepCount, skippedCount, start, stop, toSource, reset }
}
