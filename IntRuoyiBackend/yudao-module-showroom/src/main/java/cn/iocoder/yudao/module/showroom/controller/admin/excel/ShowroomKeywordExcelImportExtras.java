package cn.iocoder.yudao.module.showroom.controller.admin.excel;

import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomProductImportMode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ShowroomKeywordExcelImportExtras {

    public static final String SHEET_NAME = "关键词中英对照";
    public static final String NAME_ZH_HEADER = "中文关键词";
    public static final String NAME_EN_HEADER = "English Keyword";

    private ShowroomKeywordExcelImportExtras() {
    }

    public static List<ShowroomKeywordExcelImportRow> read(byte[] content,
                                                           ShowroomProductImportMode importMode) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalStateException("SHOWROOM_KEYWORD_IMPORT_EXCEL_EMPTY: 关键词导入文件内容不能为空");
        }
        ShowroomProductImportMode resolvedImportMode = importMode == null
                ? ShowroomProductImportMode.STANDARD
                : importMode;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                if (resolvedImportMode == ShowroomProductImportMode.BASE_WORKBOOK) {
                    return List.of();
                }
                throw new IllegalStateException("SHOWROOM_KEYWORD_IMPORT_SHEET_MISSING: 产品导入文件缺少 Sheet `"
                        + SHEET_NAME + "`");
            }
            DataFormatter formatter = new DataFormatter();
            int nameZhColumn = -1;
            int nameEnColumn = -1;
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalStateException("SHOWROOM_KEYWORD_IMPORT_HEADER_INVALID: 关键词页签表头不能为空");
            }
            for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                String value = normalizeCellText(headerRow.getCell(cellIndex), formatter);
                if (NAME_ZH_HEADER.equals(value)) {
                    nameZhColumn = cellIndex;
                } else if (NAME_EN_HEADER.equals(value)) {
                    nameEnColumn = cellIndex;
                }
            }
            if (nameZhColumn < 0 || nameEnColumn < 0) {
                throw new IllegalStateException("SHOWROOM_KEYWORD_IMPORT_HEADER_INVALID: 关键词页签必须包含 `"
                        + NAME_ZH_HEADER + "` 和 `" + NAME_EN_HEADER + "`");
            }
            List<ShowroomKeywordExcelImportRow> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int rowNo = rowIndex + 1;
                String nameZh = normalizeCellText(row.getCell(nameZhColumn), formatter);
                String nameEn = normalizeCellText(row.getCell(nameEnColumn), formatter);
                if (nameZh.isEmpty() && nameEn.isEmpty()) {
                    continue;
                }
                if (nameZh.isEmpty() || nameEn.isEmpty()) {
                    throw new IllegalStateException("SHOWROOM_KEYWORD_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                            + " 行关键词中英文必须同时填写");
                }
                rows.add(new ShowroomKeywordExcelImportRow(rowNo, nameZh, nameEn));
            }
            return List.copyOf(rows);
        }
    }

    private static String normalizeCellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }
}
