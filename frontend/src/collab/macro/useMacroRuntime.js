import { ref } from 'vue'
import axios from '../../api/axios'
import CollabMacroApi from '../../api/CollabMacroApi.js'

/**
 * Makro yürütme host'u — worker'ın ana iş parçacığındaki karşılığı
 * (COLLAB_WORKSPACE_PLAN.md K8 / §9).
 *
 * Sorumlulukları:
 * 1. Sunucudan çalıştırma **izni** almak (onay/yetki kararı orada verilir).
 * 2. Dokümanın okunabilir kopyasını çıkarıp worker'a vermek.
 * 3. Worker'ın veri çağrılarını **kullanıcının kendi oturumuyla** mevcut REST
 *    uçlarına çevirmek — makronun yetkisi kullanıcının yetkisidir, ayrı bir
 *    yetki yolu yoktur.
 * 4. Zaman aşımında worker'ı sonlandırmak. Sonsuz döngüyü içeriden durdurmak
 *    mümkün değil; sayaç burada olmak zorunda.
 * 5. Dönen işlem listesini tek transaction'da uygulamak ve sonucu raporlamak.
 */
export function useMacroRuntime({ projectId, documentId, getTeamId, getSnapshot, applyOps }) {
  const running = ref(false)
  const lastResult = ref(null)

  async function runMacro(macro, trigger = 'MANUAL') {
    if (running.value) return null
    running.value = true

    let worker = null
    let timer = null
    let ticket = null

    try {
      const { data } = await CollabMacroApi.run(projectId, macro.id, trigger)
      ticket = data

      const result = await new Promise((resolve) => {
        worker = new Worker(new URL('./macroWorker.js', import.meta.url), { type: 'module' })

        timer = setTimeout(() => {
          worker.terminate()
          resolve({
            status: 'TIMEOUT',
            ops: [],
            error: `Makro ${Math.round(ticket.timeoutMs / 1000)} saniyede tamamlanmadı ve durduruldu.`
          })
        }, ticket.timeoutMs)

        worker.onmessage = async (event) => {
          const message = event.data
          if (message.type === 'call') {
            try {
              const value = await hostCall(message.scope, message.method, message.args)
              worker.postMessage({ type: 'callResult', id: message.id, value })
            } catch (error) {
              worker.postMessage({
                type: 'callResult', id: message.id,
                error: error?.response?.data?.error || error?.message || 'Çağrı başarısız'
              })
            }
            return
          }
          if (message.type === 'result') {
            clearTimeout(timer)
            worker.terminate()
            resolve(message)
          }
        }

        worker.onerror = (event) => {
          clearTimeout(timer)
          worker.terminate()
          resolve({ status: 'FAILED', ops: [], error: event.message || 'Worker hatası' })
        }

        worker.postMessage({
          type: 'run',
          source: ticket.source,
          snapshot: getSnapshot()
        })
      })

      let wroteDocument = false
      if (result.status === 'SUCCESS' && result.ops?.length) {
        applyOps(result.ops)
        wroteDocument = true
      }

      await CollabMacroApi.completeRun(projectId, ticket.runId, {
        status: result.status,
        durationMs: result.durationMs,
        log: result.log,
        error: result.error,
        wroteDocument
      })

      lastResult.value = result
      return result
    } finally {
      clearTimeout(timer)
      worker?.terminate()
      running.value = false
    }
  }

  /**
   * Worker'ın `ScrumTools.*` çağrılarını mevcut REST uçlarına bağlar.
   *
   * Makroya özel sunucu uçları **açılmadı**: aynı veriyi ikinci bir yoldan
   * sunmak, ikinci bir yetki kontrolü demek ve iki kontrolün zamanla
   * ayrışmaması için bir sebep yok.
   */
  async function hostCall(scope, method, args = []) {
    if (scope === 'tasks' && method === 'query') {
      const [tql, options = {}] = args
      const team = options.teamId || getTeamId?.()
      if (!team) {
        throw new Error('ScrumTools.tasks.query için takım gerekli: '
          + 'dokümana takım etiketi ekleyin ya da { teamId } geçin.')
      }
      const { data } = await axios.post(`/api/teams/${team}/tasks/query`, {
        query: tql,
        projectId: options.projectId || projectId,
        page: options.page || 0,
        size: options.size || 100
      })
      return data
    }

    if (scope === 'sprints' && method === 'current') {
      const team = args[0] || getTeamId?.()
      if (!team) throw new Error('ScrumTools.sprints.current için takım kimliği gerekli.')
      const { data } = await axios.get(`/api/teams/${team}/sprints`)
      // Ayrı bir "aktif sprint" ucu yok; liste üzerinden seçiliyor.
      return (Array.isArray(data) ? data : []).find((sprint) => sprint.status === 'ACTIVE') || null
    }

    if (scope === 'docs' && method === 'getPage') {
      const [spaceId, pageId] = args
      const { data } = await axios.get(
        `/api/projects/${projectId}/docs/spaces/${spaceId}/pages/${pageId}`)
      return data
    }

    if (scope === 'docs' && method === 'savePage') {
      const [spaceId, title, html] = args
      const { data } = await axios.post(
        `/api/projects/${projectId}/docs/spaces/${spaceId}/pages`,
        { title, content: html })
      return data
    }

    if (scope === 'http' && method === 'fetch') {
      const [url, options = {}] = args
      const { data } = await axios.post(
        `/api/projects/${projectId}/collab/macros/http`,
        { url, method: options.method, headers: options.headers, body: options.body })
      return data
    }

    if (scope === 'ui') {
      if (method === 'alert') return window.alert(String(args[0] ?? ''))
      if (method === 'prompt') return window.prompt(String(args[0] ?? ''), args[1] ?? '')
      if (method === 'toast') {
        const { createToast } = await import('mosha-vue-toastify')
        createToast(String(args[0] ?? ''), {
          type: args[1] || 'info', position: 'bottom-right', timeout: 4000
        })
        return null
      }
    }

    throw new Error(`Bilinmeyen API çağrısı: ScrumTools.${scope}.${method}`)
  }

  return { running, lastResult, runMacro, documentId }
}
