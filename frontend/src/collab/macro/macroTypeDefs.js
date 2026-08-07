/**
 * Makro editörüne yüklenen tip tanımları (COLLAB_WORKSPACE_PLAN.md Faz 4).
 *
 * Monaco'nun JS/TS diline "ekstra kütüphane" olarak veriliyor; böylece
 * `ScrumTools.` yazınca tamamlama çıkıyor. Kaynak burada düz metin olarak
 * duruyor çünkü asıl `.d.ts` dosyası derlemeye girmemeli — bu tanımlar
 * uygulamanın değil, kullanıcının yazdığı betiğin sözleşmesi.
 *
 * <b>§9.1'den sapma:</b> veri çağrıları `Promise` döndürür. Planın taslağı
 * senkron gösteriyordu ama K8 eşzamanlı RPC'yi açıkça reddediyor — çağrılar ana
 * iş parçacığına gidip kullanıcının oturumuyla yapılıyor.
 */
export const MACRO_TYPE_DEFS = `
declare namespace ScrumTools {
  interface Range {
    getA1Notation(): string
    getValues(): any[][]
    getValue(): any
    setValues(values: any[][]): Range
    setValue(value: any): Range
    setFormula(formula: string): Range
    setStyle(style: { bold?: boolean, italic?: boolean, bg?: string, color?: string }): Range
  }

  interface Sheet {
    getName(): string
    getSheetId(): string
    getLastRow(): number
    getLastColumn(): number
    /** getRange('A1:C10') veya getRange(row, column, numRows?, numColumns?) — 0 tabanlı. */
    getRange(a1: string): Range
    getRange(row: number, column: number, numRows?: number, numColumns?: number): Range
    insertRows(rowIndex: number, count?: number): void
    deleteRows(rowIndex: number, count?: number): void
    insertColumns(columnIndex: number, count?: number): void
    deleteColumns(columnIndex: number, count?: number): void
    sort(options: { column?: number, ascending?: boolean, range?: string }): void
  }

  interface Document {
    getType(): 'TEXT' | 'CODE' | 'SHEET'
    getTitle(): string
    getSheets(): Sheet[]
    getActiveSheet(): Sheet | null
    getSheetByName(name: string): Sheet | null
    /** TEXT ve CODE dokümanlarında içerik. */
    getText(): string
    setText(text: string): void
    replaceText(pattern: string | RegExp, replacement: string): string
  }

  function getActiveDocument(): Document

  namespace tasks {
    /** TASK_QUERY_LANGUAGE.md sözdizimi. Takım, dokümanın etiketinden gelir. */
    function query(tql: string, options?: {
      teamId?: string, projectId?: string, page?: number, size?: number
    }): Promise<{ content: any[], totalElements: number }>
  }

  namespace sprints {
    function current(teamId?: string): Promise<any | null>
  }

  namespace docs {
    function getPage(spaceId: string, pageId: string): Promise<any>
    function savePage(spaceId: string, title: string, html: string): Promise<any>
  }

  namespace http {
    /** Yalnızca sunucudaki izin listesindeki adresler; vekil üzerinden gider. */
    function fetch(url: string, options?: {
      method?: string, headers?: Record<string, string>, body?: string
    }): Promise<{ status: number, body: string }>
  }

  namespace ui {
    function toast(message: string, type?: 'info' | 'success' | 'warning' | 'danger'): Promise<void>
    function alert(message: string): Promise<void>
    function prompt(message: string, defaultValue?: string): Promise<string | null>
  }

  namespace utils {
    function formatDate(value: Date | string | number, pattern?: string): string
  }
}
`

/** Rıza diyaloğunda gösterilen okunabilir açıklamalar (§9.2). */
export const SCOPE_LABELS = {
  tasks: 'Görevlerinizi okuyacak',
  sprints: 'Sprint bilgilerini okuyacak',
  docs: 'Docs sayfalarını okuyacak',
  'docs:write': 'Docs\'a yeni sayfa yazacak',
  'document:write': 'Bu dokümanı değiştirecek',
  http: 'Dış bir adrese istek atacak (sunucu izin listesi üzerinden)',
  ui: 'Ekranda mesaj gösterecek',
  utils: 'Yardımcı işlevleri kullanacak'
}
