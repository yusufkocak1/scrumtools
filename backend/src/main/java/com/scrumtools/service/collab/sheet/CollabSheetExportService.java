package com.scrumtools.service.collab.sheet;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * {@link CollabSheetData} → {@code .xlsx} / {@code .csv} (plan §10).
 *
 * <p>Yazma {@code SXSSFWorkbook} ile akışlıdır: bellekte yalnızca son N satır
 * tutulur, gerisi geçici dosyaya iner. Klasik {@code XSSFWorkbook} 200 bin
 * hücrede yüzlerce MB yer ve D3'ün sunucusunu düşürür.
 *
 * <p>Hem formül hem hesaplanmış değer yazılır: değerler istemciden gelen anlık
 * görüntüden okunur (K5 — sunucu formül hesaplamaz), formüller de yazıldığı için
 * Excel'de dosya açıldığında hücreler canlı kalır.
 */
@Service
@Slf4j
public class CollabSheetExportService {

    /** Bellekte tutulan satır penceresi; gerisi diske akıtılır. */
    private static final int ROW_ACCESS_WINDOW = 200;

    public byte[] toXlsx(CollabSheetData data) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Map<String, CellStyle> styleCache = new HashMap<>();

            for (CollabSheetData.Sheet sheet : safeSheets(data)) {
                SXSSFSheet target = workbook.createSheet(safeSheetName(workbook, sheet.name()));
                writeSheet(workbook, target, sheet, data.styles(), styleCache);
            }

            if (workbook.getNumberOfSheets() == 0) {
                workbook.createSheet("Sayfa1");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("xlsx üretilemedi", e);
            throw new IllegalStateException("Excel dosyası oluşturulamadı: " + e.getMessage());
        }
    }

    private void writeSheet(SXSSFWorkbook workbook, SXSSFSheet target,
                            CollabSheetData.Sheet source,
                            Map<String, Map<String, Object>> styles,
                            Map<String, CellStyle> styleCache) {

        // Hücreler satır satır gruplanıyor: SXSSF yalnızca ileri doğru yazmaya
        // izin verir, penceresi kaymış bir satıra dönmek hata verir.
        Map<Integer, List<Map.Entry<Integer, CollabSheetData.Cell>>> byRow = new TreeMap<>();
        if (source.cells() != null) {
            source.cells().forEach((key, cell) -> {
                int[] position = parseKey(key);
                if (position == null) return;
                byRow.computeIfAbsent(position[0], k -> new ArrayList<>())
                        .add(Map.entry(position[1], cell));
            });
        }

        byRow.forEach((rowIndex, entries) -> {
            Row row = target.createRow(rowIndex);
            entries.sort(Map.Entry.comparingByKey());
            for (Map.Entry<Integer, CollabSheetData.Cell> entry : entries) {
                writeCell(workbook, row, entry.getKey(), entry.getValue(), styles, styleCache);
            }
        });

        if (source.cols() != null) {
            source.cols().forEach((key, meta) -> {
                if (meta.w() == null) return;
                try {
                    // Excel sütun genişliği 1/256 karakter biriminde; piksel ≈ 7 karakter.
                    target.setColumnWidth(Integer.parseInt(key), Math.min(255 * 256, meta.w() * 256 / 7));
                } catch (NumberFormatException ignored) {
                    // bozuk anahtar sessizce atlanır, dosyanın tamamı kaybedilmez
                }
            });
        }

        if (source.merges() != null) {
            for (CollabSheetData.Merge merge : source.merges()) {
                if (merge.rs() <= 1 && merge.cs() <= 1) continue;
                target.addMergedRegion(new CellRangeAddress(
                        merge.r(), merge.r() + merge.rs() - 1,
                        merge.c(), merge.c() + merge.cs() - 1));
            }
        }
    }

    private void writeCell(SXSSFWorkbook workbook, Row row, int column,
                           CollabSheetData.Cell source,
                           Map<String, Map<String, Object>> styles,
                           Map<String, CellStyle> styleCache) {
        Cell cell = row.createCell(column);

        if (source.f() != null && !source.f().isBlank()) {
            String formula = source.f().startsWith("=") ? source.f().substring(1) : source.f();
            try {
                cell.setCellFormula(formula);
            } catch (RuntimeException e) {
                // POI'nin ayrıştıramadığı formül (Univer'e özgü fonksiyon olabilir)
                // metin olarak yazılır — hücreyi tamamen kaybetmekten iyidir.
                cell.setCellValue(source.f());
            }
        } else if (source.v() instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (source.v() instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (source.v() != null) {
            cell.setCellValue(String.valueOf(source.v()));
        }

        String numFmt = numberFormat(styles, source.s());
        if (numFmt != null) {
            cell.setCellStyle(styleCache.computeIfAbsent(numFmt, format -> {
                CellStyle style = workbook.createCellStyle();
                style.setDataFormat(workbook.createDataFormat().getFormat(format));
                return style;
            }));
        }
    }

    // ─── CSV ─────────────────────────────────────────────────────────────────

    /** Yalnızca ilk sayfa dışa aktarılır — CSV'nin sayfa kavramı yoktur. */
    public byte[] toCsv(CollabSheetData data) {
        List<CollabSheetData.Sheet> sheets = safeSheets(data);
        if (sheets.isEmpty()) return new byte[0];
        CollabSheetData.Sheet sheet = sheets.getFirst();

        Map<Integer, Map<Integer, CollabSheetData.Cell>> grid = new TreeMap<>();
        int maxCol = 0;
        if (sheet.cells() != null) {
            for (Map.Entry<String, CollabSheetData.Cell> entry : sheet.cells().entrySet()) {
                int[] position = parseKey(entry.getKey());
                if (position == null) continue;
                grid.computeIfAbsent(position[0], k -> new TreeMap<>()).put(position[1], entry.getValue());
                maxCol = Math.max(maxCol, position[1]);
            }
        }

        StringBuilder out = new StringBuilder();
        // BOM: Excel UTF-8 CSV'yi ancak bununla doğru açıyor.
        out.append('﻿');
        for (Map.Entry<Integer, Map<Integer, CollabSheetData.Cell>> rowEntry : grid.entrySet()) {
            Map<Integer, CollabSheetData.Cell> cells = rowEntry.getValue();
            for (int col = 0; col <= maxCol; col++) {
                if (col > 0) out.append(',');
                CollabSheetData.Cell cell = cells.get(col);
                if (cell != null && cell.v() != null) {
                    out.append(escapeCsv(String.valueOf(cell.v())));
                }
            }
            out.append('\n');
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escapeCsv(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    // ─── Yardımcılar ─────────────────────────────────────────────────────────

    private static List<CollabSheetData.Sheet> safeSheets(CollabSheetData data) {
        return data == null || data.sheets() == null ? List.of() : data.sheets();
    }

    private static String numberFormat(Map<String, Map<String, Object>> styles, String key) {
        if (key == null || styles == null) return null;
        Map<String, Object> style = styles.get(key);
        if (style == null) return null;
        Object format = style.get("numFmt");
        return format instanceof String text && !text.isBlank() ? text : null;
    }

    /** Excel sayfa adı kuralları: 31 karakter, bazı işaretler yasak, tekrar edemez. */
    private static String safeSheetName(SXSSFWorkbook workbook, String name) {
        String base = (name == null || name.isBlank()) ? "Sayfa" : name;
        base = base.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (base.length() > 28) base = base.substring(0, 28);
        String candidate = base;
        int suffix = 2;
        while (workbook.getSheet(candidate) != null) {
            candidate = base + " " + suffix++;
        }
        return candidate;
    }

    /** {@code "R12C3"} → {@code [12, 3]}; tanınmayan anahtar için {@code null}. */
    private static int[] parseKey(String key) {
        if (key == null || key.length() < 4 || key.charAt(0) != 'R') return null;
        int separator = key.indexOf('C', 1);
        if (separator < 0) return null;
        try {
            return new int[]{
                    Integer.parseInt(key.substring(1, separator)),
                    Integer.parseInt(key.substring(separator + 1))
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
