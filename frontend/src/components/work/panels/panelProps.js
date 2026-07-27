/**
 * panelProps.js
 *
 * Görev detayındaki panellerin ortak prop sözleşmesi. Paneller sayfada
 * `<component :is>` ile sıralandığı için hepsi aynı props'u alır; panel
 * kendisine gerekmeyeni yok sayar.
 *
 * Ortak event'ler:
 *   refresh        — görevi (ve alt görevleri) sunucudan tazele
 *   refresh-links  — yalnız ilişkileri tazele
 *   open-task      — başka bir göreve git (customId)
 *   field          — (alan, değer) alanı sunucuya yaz (kaydı sayfa yapar)
 *   patch          — panel kaydı zaten yaptı, yerel görevi güncelle
 */

export const taskPanelProps = {
  task: { type: Object, required: true },
  teamId: { type: String, default: null },
  subtasks: { type: Array, default: () => [] },
  links: { type: Array, default: () => [] },
  releases: { type: Array, default: () => [] },
  deployments: { type: Array, default: () => [] },
}

export const taskPanelEmits = ['refresh', 'refresh-links', 'open-task', 'field', 'patch']
