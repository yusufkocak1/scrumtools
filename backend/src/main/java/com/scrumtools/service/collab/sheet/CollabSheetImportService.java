package com.scrumtools.service.collab.sheet;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * {@code .xlsx} / {@code .csv} → {@link CollabSheetData} (plan §10).
 *
 * <p>Okuma <b>olay tabanlıdır</b> (XSSF + SAX): {@code XSSFWorkbook} tüm çalışma
 * kitabını DOM olarak kurar ve 200 bin hücrede yüzlerce MB heap ister — D3'ün
 * sunucusunda kullanılamaz. Burada satırlar akarken işlenir, bellekte yalnızca
 * üretilen model durur.
 *
 * <p>POI'nin hazır {@code XSSFSheetXMLHandler}'ı kullanılmadı: o sınıf ya
 * biçimlenmiş değeri ya da formülü verir, ikisini birden vermez. Hesap tablosu
 * modelimizde formül metni de hesaplanmış değer de gerekli (K5), bu yüzden
 * sayfa XML'i doğrudan ayrıştırılıyor.
 */
@Service
@Slf4j
public class CollabSheetImportService {

    /** Kullanıcıya gösterilen uyum raporu ile birlikte dönen sonuç. */
    public record Result(CollabSheetData data, List<String> warnings) {
    }

    public Result importFile(String fileName, byte[] content) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) {
            return importCsv(fileName, content);
        }
        if (lower.endsWith(".xlsx") || lower.endsWith(".xlsm")) {
            return importXlsx(fileName, content, lower.endsWith(".xlsm"));
        }
        throw new IllegalArgumentException("Yalnızca .xlsx, .xlsm ve .csv dosyaları yüklenebilir.");
    }

    // ─── XLSX ────────────────────────────────────────────────────────────────

    private Result importXlsx(String fileName, byte[] content, boolean macroEnabled) {
        List<String> warnings = new ArrayList<>();
        if (macroEnabled) {
            // K7: VBA içe aktarılmıyor ve bu sessizce yapılmıyor.
            warnings.add("Dosyadaki VBA makroları alınmadı — makro dili JavaScript'tir.");
        }

        List<CollabSheetData.Sheet> sheets = new ArrayList<>();
        Map<String, Map<String, Object>> styles = new LinkedHashMap<>();

        try (OPCPackage pkg = OPCPackage.open(new java.io.ByteArrayInputStream(content))) {
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable stylesTable = reader.getStylesTable();

            XSSFReader.SheetIterator iterator = (XSSFReader.SheetIterator) reader.getSheetsData();
            int index = 0;
            while (iterator.hasNext()) {
                try (InputStream sheetStream = iterator.next()) {
                    String name = iterator.getSheetName();
                    SheetHandler handler = new SheetHandler(strings, stylesTable, styles);

                    XMLReader parser = XMLHelper.newXMLReader();
                    parser.setContentHandler(handler);
                    parser.parse(new InputSource(sheetStream));

                    sheets.add(handler.toSheet("sheet-" + index, name));
                    index++;
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("xlsx ayrıştırılamadı: {}", fileName, e);
            throw new IllegalArgumentException("Excel dosyası okunamadı: " + e.getMessage());
        }

        if (sheets.isEmpty()) {
            throw new IllegalArgumentException("Dosyada okunabilir sayfa bulunamadı.");
        }
        warnings.add("Grafik, pivot tablo ve koşullu biçimlendirme bu sürümde aktarılmıyor.");

        return new Result(
                new CollabSheetData(CollabSheetData.CURRENT_VERSION, sheets, styles),
                warnings);
    }

    /**
     * Tek bir sayfanın XML'ini olay olay işler.
     *
     * <p>Aynı anda bellekte tutulan tek şey işlenmekte olan hücredir; satır
     * bittiğinde hücreler modele yazılır.
     */
    private static final class SheetHandler extends DefaultHandler {

        private final ReadOnlySharedStringsTable strings;
        private final StylesTable stylesTable;
        private final Map<String, Map<String, Object>> styles;

        private final Map<String, CollabSheetData.Cell> cells = new LinkedHashMap<>();
        private final Map<String, CollabSheetData.RowMeta> rows = new LinkedHashMap<>();
        private final Map<String, CollabSheetData.ColMeta> cols = new LinkedHashMap<>();
        private final List<CollabSheetData.Merge> merges = new ArrayList<>();

        private int maxRow = 0;
        private int maxCol = 0;

        private final StringBuilder text = new StringBuilder();
        private boolean captureValue;
        private boolean captureFormula;

        private int row;
        private int col;
        private String cellType;
        private int styleIndex = -1;
        private String value;
        private String formula;

        SheetHandler(ReadOnlySharedStringsTable strings, StylesTable stylesTable,
                     Map<String, Map<String, Object>> styles) {
            this.strings = strings;
            this.stylesTable = stylesTable;
            this.styles = styles;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            switch (qName) {
                case "row" -> {
                    String reference = attributes.getValue("r");
                    row = reference != null ? Integer.parseInt(reference) - 1 : row;
                    String height = attributes.getValue("ht");
                    boolean hidden = "1".equals(attributes.getValue("hidden"));
                    if (height != null || hidden) {
                        rows.put(String.valueOf(row), new CollabSheetData.RowMeta(
                                height != null ? (int) Math.round(Double.parseDouble(height) * 4 / 3) : null,
                                hidden ? Boolean.TRUE : null));
                    }
                }
                case "col" -> {
                    // <col min="1" max="3" width="12.5"/> — kapsadığı her sütuna yazılır.
                    String width = attributes.getValue("width");
                    boolean hidden = "1".equals(attributes.getValue("hidden"));
                    if (width == null && !hidden) return;
                    int min = parseInt(attributes.getValue("min"), 1);
                    int max = parseInt(attributes.getValue("max"), min);
                    // Aşırı geniş aralıklar (Excel bazen 16384'e kadar yazar) modeli
                    // şişirmesin diye sınırlanıyor.
                    max = Math.min(max, min + 255);
                    for (int c = min; c <= max; c++) {
                        cols.put(String.valueOf(c - 1), new CollabSheetData.ColMeta(
                                width != null ? (int) Math.round(Double.parseDouble(width) * 7) : null,
                                hidden ? Boolean.TRUE : null));
                    }
                }
                case "c" -> {
                    CellReference reference = new CellReference(attributes.getValue("r"));
                    row = reference.getRow();
                    col = reference.getCol();
                    cellType = attributes.getValue("t");
                    styleIndex = parseInt(attributes.getValue("s"), -1);
                    value = null;
                    formula = null;
                }
                case "v", "t" -> {
                    captureValue = true;
                    text.setLength(0);
                }
                case "f" -> {
                    captureFormula = true;
                    text.setLength(0);
                }
                case "mergeCell" -> {
                    String ref = attributes.getValue("ref");
                    if (ref != null && ref.contains(":")) {
                        String[] parts = ref.split(":");
                        CellReference from = new CellReference(parts[0]);
                        CellReference to = new CellReference(parts[1]);
                        merges.add(new CollabSheetData.Merge(
                                from.getRow(), from.getCol(),
                                to.getRow() - from.getRow() + 1,
                                to.getCol() - from.getCol() + 1));
                    }
                }
                default -> { /* diğer düğümler ilgi alanı dışında */ }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (captureValue || captureFormula) text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
                case "v", "t" -> {
                    if (captureValue) value = text.toString();
                    captureValue = false;
                }
                case "f" -> {
                    if (captureFormula) formula = text.toString();
                    captureFormula = false;
                }
                case "c" -> emitCell();
                default -> { /* yok */ }
            }
        }

        private void emitCell() {
            if (value == null && formula == null) return;

            Object cellValue = null;
            Integer type = null;
            if (value != null) {
                if ("s".equals(cellType)) {
                    cellValue = strings.getItemAt(Integer.parseInt(value)).getString();
                    type = 1;
                } else if ("b".equals(cellType)) {
                    cellValue = "1".equals(value);
                    type = 3;
                } else if ("e".equals(cellType)) {
                    cellValue = value;
                    type = 1;
                } else if ("str".equals(cellType) || "inlineStr".equals(cellType)) {
                    cellValue = value;
                    type = 1;
                } else {
                    cellValue = parseDouble(value);
                    type = cellValue == null ? null : 2;
                    if (cellValue == null) cellValue = value;
                }
            }

            String styleKey = resolveStyle(styleIndex);
            cells.put(CollabSheetData.cellKey(row, col), new CollabSheetData.Cell(
                    cellValue,
                    formula != null && !formula.isBlank() ? "=" + formula : null,
                    type,
                    styleKey));

            maxRow = Math.max(maxRow, row);
            maxCol = Math.max(maxCol, col);
        }

        /**
         * Sayı biçimini paylaşımlı stil sözlüğüne alır.
         *
         * <p>Yalnızca {@code numFmt} taşınıyor: yazı tipi/kenarlık bilgisi
         * {@code StylesTable} üzerinden hücre başına okunabilir ama içe aktarılan
         * her stil varyantını modele koymak hücre sayısı kadar stil üretebilir.
         * Uyum raporunda bu açıkça yazıyor.
         */
        private String resolveStyle(int index) {
            if (index < 0) return null;
            try {
                XSSFCellStyle style = stylesTable.getStyleAt(index);
                if (style == null) return null;
                String format = style.getDataFormatString();
                if (format == null || format.isBlank() || "General".equals(format)) return null;
                String key = "n" + index;
                styles.computeIfAbsent(key, k -> Map.of("numFmt", format));
                return key;
            } catch (RuntimeException e) {
                return null;
            }
        }

        CollabSheetData.Sheet toSheet(String id, String name) {
            return new CollabSheetData.Sheet(
                    id,
                    name != null && !name.isBlank() ? name : "Sayfa",
                    Math.max(maxRow + 1, 50),
                    Math.max(maxCol + 1, 20),
                    cells,
                    rows.isEmpty() ? null : rows,
                    cols.isEmpty() ? null : cols,
                    merges.isEmpty() ? null : merges);
        }
    }

    // ─── CSV ─────────────────────────────────────────────────────────────────

    /**
     * Basit CSV okuyucu: virgül ayraç, çift tırnak kaçış, gömülü satır sonu.
     *
     * <p>Ayrı bir CSV kütüphanesi eklenmedi — desteklenen ağırlık zaten
     * RFC 4180'in bu alt kümesi ve tek bağımlılık daha eklemek §12'nin dar
     * sunucusuna bir şey kazandırmıyor.
     */
    private Result importCsv(String fileName, byte[] content) {
        Map<String, CollabSheetData.Cell> cells = new LinkedHashMap<>();
        int maxRow = 0;
        int maxCol = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.ByteArrayInputStream(content), StandardCharsets.UTF_8))) {

            int row = 0;
            String line;
            StringBuilder pending = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                pending.append(line);
                // Tek sayıda tırnak varsa değer bir sonraki satıra taşıyor demektir.
                if (countQuotes(pending) % 2 != 0) {
                    pending.append('\n');
                    continue;
                }
                List<String> values = parseCsvLine(pending.toString());
                pending.setLength(0);

                for (int col = 0; col < values.size(); col++) {
                    String raw = values.get(col);
                    if (raw == null || raw.isEmpty()) continue;
                    Double number = parseDouble(raw);
                    cells.put(CollabSheetData.cellKey(row, col), number != null
                            ? new CollabSheetData.Cell(number, null, 2, null)
                            : new CollabSheetData.Cell(raw, null, 1, null));
                    maxCol = Math.max(maxCol, col);
                }
                maxRow = Math.max(maxRow, row);
                row++;
            }
        } catch (Exception e) {
            log.warn("csv ayrıştırılamadı: {}", fileName, e);
            throw new IllegalArgumentException("CSV dosyası okunamadı: " + e.getMessage());
        }

        CollabSheetData.Sheet sheet = new CollabSheetData.Sheet(
                "sheet-0", "Sayfa1",
                Math.max(maxRow + 1, 50), Math.max(maxCol + 1, 20),
                cells, null, null, null);

        return new Result(
                new CollabSheetData(CollabSheetData.CURRENT_VERSION, List.of(sheet), Map.of()),
                List.of("CSV'de biçim ve formül bilgisi yoktur; değerler düz olarak alındı."));
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static int countQuotes(CharSequence value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '"') count++;
        }
        return count;
    }

    private static Double parseDouble(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
