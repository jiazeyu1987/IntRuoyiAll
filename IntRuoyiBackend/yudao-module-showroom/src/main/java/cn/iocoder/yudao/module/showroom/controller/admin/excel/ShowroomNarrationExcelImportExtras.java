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

public final class ShowroomNarrationExcelImportExtras {

    public static final String SHEET_NAME = "讲解音频";
    public static final String TARGET_TYPE_HEADER = "目标类型";
    public static final String TARGET_CODE_HEADER = "目标编码";
    public static final String TARGET_NAME_HEADER = "目标名称";
    public static final String LANGUAGE_HEADER = "语言";
    public static final String SCRIPT_TEXT_HEADER = "讲解稿";
    public static final String AUDIO_FILE_ID_HEADER = "音频文件ID";
    public static final String AUDIO_URL_HEADER = "音频地址";
    public static final String AUDIO_DURATION_HEADER = "音频时长(秒)";
    public static final String VOICE_HEADER = "音色";

    private ShowroomNarrationExcelImportExtras() {
    }

    public static List<ShowroomNarrationExcelImportRow> read(byte[] content,
                                                             ShowroomProductImportMode importMode) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_EXCEL_EMPTY: 讲解音频导入文件内容不能为空");
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
                throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_SHEET_MISSING: 产品导入文件缺少 Sheet `"
                        + SHEET_NAME + "`");
            }
            DataFormatter formatter = new DataFormatter();
            HeaderColumns headers = resolveHeaders(sheet, formatter);
            List<ShowroomNarrationExcelImportRow> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int rowNo = rowIndex + 1;
                String targetType = normalizeCellText(row.getCell(headers.targetTypeColumn()), formatter);
                String targetCode = normalizeCellText(row.getCell(headers.targetCodeColumn()), formatter);
                String targetName = normalizeCellText(row.getCell(headers.targetNameColumn()), formatter);
                String language = normalizeCellText(row.getCell(headers.languageColumn()), formatter);
                String scriptText = normalizeCellText(row.getCell(headers.scriptTextColumn()), formatter);
                String audioFileIdText = normalizeCellText(row.getCell(headers.audioFileIdColumn()), formatter);
                String audioUrl = normalizeCellText(row.getCell(headers.audioUrlColumn()), formatter);
                String audioDurationText = normalizeCellText(row.getCell(headers.audioDurationColumn()), formatter);
                String voice = normalizeCellText(row.getCell(headers.voiceColumn()), formatter);
                if (targetType.isEmpty() && targetCode.isEmpty() && targetName.isEmpty()
                        && language.isEmpty() && scriptText.isEmpty() && audioFileIdText.isEmpty()
                        && audioUrl.isEmpty() && audioDurationText.isEmpty() && voice.isEmpty()) {
                    continue;
                }
                if (targetType.isEmpty() || targetCode.isEmpty() || language.isEmpty() || scriptText.isEmpty()) {
                    throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                            + " 行目标类型、目标编码、语言和讲解稿不能为空");
                }
                Long audioFileId = audioFileIdText.isEmpty() ? null : parseLong(audioFileIdText, rowNo, "音频文件ID");
                Integer audioDuration = audioDurationText.isEmpty()
                        ? null
                        : parseInt(audioDurationText, rowNo, "音频时长(秒)");
                if (audioDuration == null || audioDuration <= 0) {
                    throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                            + " 行音频时长(秒)必须为正整数");
                }
                if (audioFileId == null && audioUrl.isEmpty()) {
                    throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                            + " 行音频文件ID和音频地址不能同时为空");
                }
                rows.add(new ShowroomNarrationExcelImportRow(rowNo, targetType, targetCode, targetName, language,
                        scriptText, audioFileId, audioUrl, audioDuration, voice));
            }
            return List.copyOf(rows);
        }
    }

    private static HeaderColumns resolveHeaders(Sheet sheet, DataFormatter formatter) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_HEADER_INVALID: 讲解音频页签表头不能为空");
        }
        int targetTypeColumn = -1;
        int targetCodeColumn = -1;
        int targetNameColumn = -1;
        int languageColumn = -1;
        int scriptTextColumn = -1;
        int audioFileIdColumn = -1;
        int audioUrlColumn = -1;
        int audioDurationColumn = -1;
        int voiceColumn = -1;
        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            String value = normalizeCellText(headerRow.getCell(cellIndex), formatter);
            switch (value) {
                case TARGET_TYPE_HEADER -> targetTypeColumn = cellIndex;
                case TARGET_CODE_HEADER -> targetCodeColumn = cellIndex;
                case TARGET_NAME_HEADER -> targetNameColumn = cellIndex;
                case LANGUAGE_HEADER -> languageColumn = cellIndex;
                case SCRIPT_TEXT_HEADER -> scriptTextColumn = cellIndex;
                case AUDIO_FILE_ID_HEADER -> audioFileIdColumn = cellIndex;
                case AUDIO_URL_HEADER -> audioUrlColumn = cellIndex;
                case AUDIO_DURATION_HEADER -> audioDurationColumn = cellIndex;
                case VOICE_HEADER -> voiceColumn = cellIndex;
                default -> {
                }
            }
        }
        if (targetTypeColumn < 0 || targetCodeColumn < 0 || targetNameColumn < 0
                || languageColumn < 0 || scriptTextColumn < 0 || audioFileIdColumn < 0
                || audioUrlColumn < 0 || audioDurationColumn < 0 || voiceColumn < 0) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_HEADER_INVALID: 讲解音频页签缺少必需表头");
        }
        return new HeaderColumns(targetTypeColumn, targetCodeColumn, targetNameColumn, languageColumn,
                scriptTextColumn, audioFileIdColumn, audioUrlColumn, audioDurationColumn, voiceColumn);
    }

    private static Long parseLong(String value, int rowNo, String fieldLabel) {
        try {
            return Long.parseLong(value.replaceAll("\\.0+$", "").trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                    + " 行" + fieldLabel + "不是有效数字");
        }
    }

    private static Integer parseInt(String value, int rowNo, String fieldLabel) {
        try {
            return Integer.parseInt(value.replaceAll("\\.0+$", "").trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                    + " 行" + fieldLabel + "不是有效数字");
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

    private record HeaderColumns(int targetTypeColumn,
                                 int targetCodeColumn,
                                 int targetNameColumn,
                                 int languageColumn,
                                 int scriptTextColumn,
                                 int audioFileIdColumn,
                                 int audioUrlColumn,
                                 int audioDurationColumn,
                                 int voiceColumn) {
    }
}
