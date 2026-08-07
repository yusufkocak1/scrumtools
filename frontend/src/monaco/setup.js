/**
 * Monaco kurulumu — paketten yükleme (COLLAB_WORKSPACE_PLAN.md R5).
 *
 * Monaco eskiden jsDelivr'dan AMD loader ile çekiliyordu. Faz 1'de gelen
 * `y-monaco` ise `monaco-editor/esm/vs/editor/editor.api.js`'i **statik** import
 * ediyor; yani paket zaten derlemeye giriyordu. CDN'i bırakmasaydık aynı anda
 * iki ayrı Monaco örneği çalışırdı: y-monaco'nun ürettiği `Range`/`Selection`
 * nesneleri CDN'deki editöre gider, sürümler de birbirini tutmazdı (0.53 / 0.52).
 *
 * Bu modül tek doğru örneği kurar ve `loader`'a verir. İçe aktarıldığı anda
 * çalışır; `VueMonacoEditor` bağlanmadan **önce** import edilmesi gerekir.
 *
 * Maliyet: Monaco birkaç MB'lık bir bağımlılık. Ana pakete girmemesi için yalnızca
 * tembel yüklenen rotalardan (CollabDocument) erişilen bileşenlerde kullanılmalı;
 * bugün tek tüketicisi `components/collab/editors/MonacoEditor.vue`.
 */
import * as monaco from 'monaco-editor'
import { loader } from '@guolao/vue-monaco-editor'

import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import CssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import HtmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

/**
 * Dil servisleri ayrı worker'larda koşar. Vite bunların her birini bağımsız bir
 * varlık olarak üretir ve ancak o dilde bir model açıldığında indirilir — beşini
 * birden tanımlamak ilk yükleme maliyetini artırmaz, CDN'deki davranışı korur.
 *
 * Tanımlanmazsa Monaco "You must define a function MonacoEnvironment.getWorker"
 * hatası atar ve otomatik tamamlama/doğrulama tamamen çalışmaz.
 */
self.MonacoEnvironment = {
  getWorker(_workerId, label) {
    switch (label) {
      case 'json':
        return new JsonWorker()
      case 'css':
      case 'scss':
      case 'less':
        return new CssWorker()
      case 'html':
      case 'handlebars':
      case 'razor':
        return new HtmlWorker()
      case 'typescript':
      case 'javascript':
        return new TsWorker()
      default:
        return new EditorWorker()
    }
  }
}

loader.config({ monaco })

export default monaco
