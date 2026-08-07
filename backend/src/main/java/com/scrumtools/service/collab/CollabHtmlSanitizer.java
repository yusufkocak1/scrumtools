package com.scrumtools.service.collab;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Anlık görüntü HTML'inin sunucu tarafı temizliği (COLLAB_WORKSPACE_PLAN.md K6).
 *
 * <p><b>Neden gerekli:</b> okunabilir çıktıyı sunucu değil <i>istemci</i> üretir
 * — sunucuda Yjs yok (K2). Yani {@code snapshot_text} kullanıcıdan gelen ham
 * HTML'dir. Bu içerik liste ekranında ve Faz 2'den itibaren Docs sayfalarında
 * gösterilecek; istemcinin DOMPurify'ına güvenmek, saldırganın kendi istemcisini
 * yazabildiği bir dünyada hiçbir şeye güvenmemek demektir.
 *
 * <p>İzin listesi TipTap'in ürettiği etiketlerle hizalıdır (başlık, liste, tablo,
 * kod bloğu, görsel, link). {@code <script>}, {@code <iframe>}, {@code on*}
 * öznitelikleri ve {@code javascript:} URL'leri jsoup tarafından atılır.
 */
@Component
public class CollabHtmlSanitizer {

    private final Safelist safelist = buildSafelist();

    public String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        return Jsoup.clean(html, safelist);
    }

    private Safelist buildSafelist() {
        return Safelist.relaxed()
                // TipTap kod bloğu highlight'ı <pre><code class="language-x"> üretir
                .addAttributes("code", "class")
                .addAttributes("pre", "class")
                .addAttributes("span", "class")
                // Vurgulama ve görev listeleri
                .addTags("mark", "u", "s")
                .addAttributes("li", "data-checked")
                .addAttributes("ul", "data-type")
                // Tablo hizalama/birleştirme
                .addAttributes("td", "colspan", "rowspan", "colwidth")
                .addAttributes("th", "colspan", "rowspan", "colwidth")
                // Uygulama içi kalıcı medya bağlantıları /api/media üzerinden gelir
                .addProtocols("img", "src", "http", "https", "data")
                // target="_blank" korunur; jsoup rel=nofollow ekler
                .addAttributes("a", "target")
                // Y3 — gömülü ortak doküman kabı. Süzülürse gömme kimliğini
                // kaybeder ve sayfa ortak düzenlemeden geçtiğinde sessizce
                // boş bir div'e döner. Öznitelikler yalnızca bir UUID ve bir
                // sayı taşıyor; içerik yine okuyanın yetkisiyle çekiliyor.
                .addAttributes("div", "data-collab-embed", "data-document-id",
                        "data-type", "data-height");
    }
}
