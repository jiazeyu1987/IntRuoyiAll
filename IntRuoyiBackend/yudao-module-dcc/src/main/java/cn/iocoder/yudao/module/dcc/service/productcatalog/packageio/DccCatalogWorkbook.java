package cn.iocoder.yudao.module.dcc.service.productcatalog.packageio;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;

/** Text cells preserve identifiers, dates and leading zeroes exactly. */
public final class DccCatalogWorkbook {
    private DccCatalogWorkbook() {}
    public record SheetData(String name, List<String> columns, List<Map<String, String>> rows) {}
    public static byte[] write(List<SheetData> sheets) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            for (var data : sheets) {
                var sheet = workbook.createSheet(data.name());
                var header = sheet.createRow(0);
                for (int c = 0; c < data.columns().size(); c++) {
                    header.createCell(c).setCellValue(data.columns().get(c));
                    sheet.setColumnWidth(c, 24 * 256);
                }
                for (var values : data.rows()) {
                    var row = sheet.createRow(sheet.getLastRowNum() + 1);
                    for (int c = 0; c < data.columns().size(); c++) {
                        String value = values.get(data.columns().get(c));
                        if (value != null) {
                            if (value.length() > 32767) throw new IllegalArgumentException(data.name() + "：单元格超过 Excel 长度限制");
                            row.createCell(c, CellType.STRING).setCellValue(value);
                        }
                    }
                }
                sheet.createFreezePane(0, 1);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
    public static List<SheetData> read(byte[] bytes, List<SheetData> schema) throws IOException {
        if (bytes.length == 0 || bytes.length > 20 * 1024 * 1024) throw new IllegalArgumentException("请选择 20MB 以内的 Excel 工作簿");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() != schema.size()) throw new IllegalArgumentException("工作表数量不符，请使用产品目录导出的完整工作簿");
            List<SheetData> result = new ArrayList<>();
            int total = 0;
            for (var expected : schema) {
                var sheet = workbook.getSheet(expected.name());
                if (sheet == null || sheet.getRow(0) == null) throw new IllegalArgumentException("缺少工作表：" + expected.name());
                var header = sheet.getRow(0);
                if (header.getLastCellNum() != expected.columns().size()) throw new IllegalArgumentException(expected.name() + "：列数不符");
                for (int c = 0; c < expected.columns().size(); c++) {
                    if (!expected.columns().get(c).equals(value(header.getCell(c)))) throw new IllegalArgumentException(expected.name() + "：第 " + (c + 1) + " 列表头不符");
                }
                List<Map<String, String>> rows = new ArrayList<>();
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    var row = sheet.getRow(r);
                    if (row == null) continue;
                    if (++total > 100000) throw new IllegalArgumentException("工作簿超过 100000 行");
                    if (row.getLastCellNum() > expected.columns().size()) throw new IllegalArgumentException(expected.name() + "：第 " + (r + 1) + " 行存在多余列");
                    Map<String, String> values = new LinkedHashMap<>();
                    for (int c = 0; c < expected.columns().size(); c++) {
                        try { values.put(expected.columns().get(c), value(row.getCell(c))); }
                        catch (IllegalArgumentException ex) { throw new IllegalArgumentException(expected.name() + " 第 " + (r + 1) + " 行：" + ex.getMessage(), ex); }
                    }
                    if (values.values().stream().anyMatch(Objects::nonNull)) rows.add(values);
                }
                if ("导入说明".equals(expected.name()) && (rows.size() != 1
                    || !Objects.equals(expected.rows().get(0).get("format"), rows.get(0).get("format"))))
                    throw new IllegalArgumentException("工作簿版本不符，请在当前系统重新导出完整工作簿后再导入");
                result.add(new SheetData(expected.name(), expected.columns(), rows));
            }
            return result;
        }
    }
    private static String value(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() != CellType.STRING) throw new IllegalArgumentException("数据必须为文本，禁止公式及数值单元格；请保留导出格式");
        return cell.getStringCellValue();
    }
}
