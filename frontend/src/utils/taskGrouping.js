/**
 * taskGrouping.js
 *
 * Backlog/liste görünümlerinde "group by <alan>" desteği. Şişen bir backlog'u
 * yönetilebilir kılmak için görevleri var olan alanlarına göre alt başlıklara
 * ayırır — yeni bir "kategori" alanı tanımlamaya gerek yoktur, gruplama tıpkı
 * bir filtre gibi görünüm seviyesinde kalır ve veriyi değiştirmez.
 *
 * Etiket (label) gruplamasında bir görev birden fazla etikete sahip olabilir;
 * Jira'daki gibi her etiketin altında ayrı ayrı görünür, toplam görev sayısı
 * grup sayılarının toplamından küçük olabilir.
 */

/** Değeri olmayan görevlerin toplandığı grup — sıralamada daima en sona gider. */
const EMPTY_KEY = '__none__'

/** Sabit sıraları olan alanlar; alfabetik sıralama bunlarda anlamsız olurdu. */
const PRIORITY_ORDER = ['Critical', 'High', 'Medium', 'Low']
const ISSUE_TYPE_ORDER = ['epic', 'story', 'task', 'bug']
const STATUS_ORDER = ['To Do', 'In Progress', 'Done', 'Cancelled']

const ISSUE_TYPE_LABELS = {
  epic: 'Epic',
  story: 'Story',
  task: 'Task',
  bug: 'Bug',
}

/**
 * Gruplama seçenekleri. `keysOf` bir görevin hangi grup(lar)a düştüğünü,
 * `labelOf` grup başlığını, `order` ise sabit sıralamayı belirler.
 */
export const GROUP_OPTIONS = [
  { value: 'none', label: 'Gruplama yok' },
  {
    value: 'label',
    label: 'Etiket',
    emptyLabel: 'Etiketsiz',
    keysOf: t => (t.labels?.length ? t.labels : [EMPTY_KEY]),
  },
  {
    value: 'assignee',
    label: 'Atanan kişi',
    emptyLabel: 'Atanmamış',
    keysOf: t => [t.assignee || EMPTY_KEY],
    labelOf: key => (key.includes('@') ? key.split('@')[0] : key),
  },
  {
    value: 'priority',
    label: 'Öncelik',
    emptyLabel: 'Önceliksiz',
    keysOf: t => [t.priority || EMPTY_KEY],
    order: PRIORITY_ORDER,
  },
  {
    value: 'issueType',
    label: 'İş tipi',
    emptyLabel: 'Tipsiz',
    keysOf: t => [t.issueType || EMPTY_KEY],
    labelOf: key => ISSUE_TYPE_LABELS[key] || key,
    order: ISSUE_TYPE_ORDER,
  },
  {
    value: 'status',
    label: 'Durum',
    emptyLabel: 'Durumsuz',
    keysOf: t => [t.status || EMPTY_KEY],
    order: STATUS_ORDER,
  },
  {
    value: 'project',
    label: 'Proje',
    emptyLabel: 'Projesiz',
    keysOf: t => [t.projectKey || EMPTY_KEY],
  },
  {
    value: 'release',
    label: 'Sürüm',
    emptyLabel: 'Sürüm atanmamış',
    keysOf: t => [t.releaseName || EMPTY_KEY],
  },
]

const OPTION_BY_VALUE = new Map(GROUP_OPTIONS.map(o => [o.value, o]))

export function isGroupingActive(groupBy) {
  return !!groupBy && groupBy !== 'none' && OPTION_BY_VALUE.has(groupBy)
}

export function groupOptionLabel(groupBy) {
  return OPTION_BY_VALUE.get(groupBy)?.label || 'Gruplama yok'
}

/**
 * Görevleri seçili alana göre gruplar.
 *
 * @param {Array} tasks — görev listesi (TaskResponse)
 * @param {string} groupBy — GROUP_OPTIONS içindeki bir `value`
 * @returns {Array<{key: string, label: string, tasks: Array, storyPoints: number}>}
 *          gruplama kapalıysa boş dizi döner (çağıran düz listeye düşer)
 */
export function groupTasks(tasks, groupBy) {
  if (!isGroupingActive(groupBy)) return []

  const option = OPTION_BY_VALUE.get(groupBy)
  const groups = new Map()

  for (const task of tasks) {
    for (const key of option.keysOf(task)) {
      if (!groups.has(key)) {
        groups.set(key, {
          key,
          label: key === EMPTY_KEY ? option.emptyLabel : (option.labelOf ? option.labelOf(key) : key),
          tasks: [],
          storyPoints: 0,
        })
      }
      const group = groups.get(key)
      group.tasks.push(task)
      group.storyPoints += task.storyPoints || 0
    }
  }

  return [...groups.values()].sort((a, b) => compareGroups(a, b, option.order))
}

/** Boş grup en sona; sabit sıralı alanlar kendi sırasına, diğerleri alfabetiğe göre. */
function compareGroups(a, b, order) {
  if (a.key === EMPTY_KEY) return 1
  if (b.key === EMPTY_KEY) return -1
  if (order) {
    const ai = order.indexOf(a.key)
    const bi = order.indexOf(b.key)
    // Sabit listede olmayan (özel workflow status'ü gibi) değerler sona alınır
    if (ai !== bi) return (ai < 0 ? order.length : ai) - (bi < 0 ? order.length : bi)
  }
  return a.label.localeCompare(b.label, 'tr')
}
