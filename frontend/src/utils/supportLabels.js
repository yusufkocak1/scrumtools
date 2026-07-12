// Destek talebi kategori/durum etiketleri ve rozet renkleri — Support sayfaları ile admin panelinde ortak

export const CATEGORY_LABELS = {
    PROBLEM: 'Sorun',
    SUGGESTION: 'Öneri',
    OTHER: 'Diğer',
}

export const STATUS_LABELS = {
    OPEN: 'Açık',
    IN_PROGRESS: 'İnceleniyor',
    WAITING_USER: 'Yanıt Bekleniyor',
    RESOLVED: 'Çözüldü',
    CLOSED: 'Kapatıldı',
}

export const STATUS_BADGE_CLASSES = {
    OPEN: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300',
    IN_PROGRESS: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/40 dark:text-indigo-300',
    WAITING_USER: 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300',
    RESOLVED: 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300',
    CLOSED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
}

export const CATEGORY_BADGE_CLASSES = {
    PROBLEM: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',
    SUGGESTION: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300',
    OTHER: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
}

export const ERROR_GROUP_STATUS_LABELS = {
    NEW: 'Yeni',
    RESOLVED: 'Çözüldü',
    REOPENED: 'Yeniden Açıldı',
    IGNORED: 'Yoksayıldı',
}

export const ERROR_GROUP_STATUS_BADGE_CLASSES = {
    NEW: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',
    RESOLVED: 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300',
    REOPENED: 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300',
    IGNORED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
}
