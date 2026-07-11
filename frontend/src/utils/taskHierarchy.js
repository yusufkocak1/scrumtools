/**
 * Düz task listesinden parent → subtask hiyerarşisi kurar.
 *
 * Kural: parentTaskId dolu ama parent yüklü listede yoksa task top-level
 * gösterilir (orphan fallback — subtask asla görünmez kalmamalı).
 * Jira davranışı: subtask, kendi sprintId'sinden bağımsız olarak parent'ının
 * altında gösterilir; gruplama parent'ın konumuna göre yapılır.
 *
 * @param {Array} tasks - TaskResponse listesi (id, parentTaskId alanları kullanılır)
 * @returns {{ topLevel: Array, childrenOf: (id: string) => Array }}
 */
export function buildTaskTree(tasks) {
  const byId = new Map(tasks.map(t => [t.id, t]))
  const children = new Map()
  const topLevel = []
  for (const t of tasks) {
    if (t.parentTaskId && byId.has(t.parentTaskId)) {
      if (!children.has(t.parentTaskId)) children.set(t.parentTaskId, [])
      children.get(t.parentTaskId).push(t)
    } else {
      topLevel.push(t)
    }
  }
  return { topLevel, childrenOf: id => children.get(id) || [] }
}
