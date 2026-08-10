package com.scrumtools.service.collab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CollabHtmlSanitizer} testleri — DOCS_TABLE_PLAN.md §6.
 *
 * <p><b>Neden özellikle bu sınıf test ediliyor:</b> tablo öznitelikleri dört
 * ayrı sanitize noktasından geçiyor ve bu, zincirin <i>sunucudaki</i> halkası.
 * Buradaki bir eksik, kullanıcıya "hata" olarak değil <b>sessiz veri kaybı</b>
 * olarak döner: sayfa "Ortak Düzenle" ile açılıp Docs'a geri yazıldığında
 * sütun tipleri, renkler ve toplam satırı kaybolur. Böyle bir kayıp, ancak
 * kullanıcı fark edip bildirdiğinde görülür — testin değeri tam olarak burada.
 */
class CollabHtmlSanitizerTest {

    private final CollabHtmlSanitizer sanitizer = new CollabHtmlSanitizer();

    @Test
    void tableAttributesSurvive() {
        String html = """
                <table data-table-layout="striped freeze-first" data-sort-col="1" data-sort-dir="asc">
                  <colgroup><col style="width: 120px"></colgroup>
                  <tbody>
                    <tr>
                      <th data-col-type="currency" data-col-format="decimals=2;symbol=TL"\
                 data-col-agg="sum" data-sorted="asc" colwidth="120">Tutar</th>
                    </tr>
                    <tr>
                      <td data-col-type="currency" data-v="1500.5" data-align="right"\
                 data-bg="amber" colspan="1" rowspan="1">1.500,50 TL</td>
                    </tr>
                    <tr data-total-row=""><td data-col-type="currency">1.500,50 TL</td></tr>
                  </tbody>
                </table>
                """;

        String cleaned = sanitizer.sanitize(html);

        for (String attribute : new String[]{
                "data-table-layout", "data-sort-col", "data-sort-dir",
                "data-col-type", "data-col-format", "data-col-agg", "data-sorted",
                "data-v", "data-align", "data-bg", "data-total-row",
                "colwidth", "colspan", "rowspan"}) {
            assertTrue(cleaned.contains(attribute),
                    "Öznitelik sanitize sırasında düştü: " + attribute + "\n" + cleaned);
        }
        assertTrue(cleaned.contains("width: 120px"), "Sütun genişliği düştü:\n" + cleaned);
    }

    @Test
    void collabEmbedContainerSurvives() {
        String cleaned = sanitizer.sanitize(
                "<div data-collab-embed data-document-id=\"abc-123\" data-type=\"SHEET\" data-height=\"420\"></div>");

        assertTrue(cleaned.contains("data-collab-embed"));
        assertTrue(cleaned.contains("abc-123"));
        assertTrue(cleaned.contains("data-height"));
    }

    @Test
    void scriptAndEventHandlersRemoved() {
        String cleaned = sanitizer.sanitize("""
                <p onclick="steal()">merhaba</p>
                <script>alert(1)</script>
                <iframe src="https://evil.example"></iframe>
                <td data-col-type="number" onmouseover="x()">5</td>
                <a href="javascript:alert(1)">tıkla</a>
                """);

        assertFalse(cleaned.contains("<script"));
        assertFalse(cleaned.contains("<iframe"));
        assertFalse(cleaned.contains("onclick"));
        assertFalse(cleaned.contains("onmouseover"));
        assertFalse(cleaned.contains("javascript:"));
        assertTrue(cleaned.contains("merhaba"));
    }

    @Test
    void blankInputPassesThrough() {
        assertTrue(sanitizer.sanitize(null) == null);
        assertTrue(sanitizer.sanitize("   ").isBlank());
    }
}
