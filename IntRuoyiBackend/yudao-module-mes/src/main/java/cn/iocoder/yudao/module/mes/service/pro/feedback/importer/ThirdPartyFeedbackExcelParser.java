package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_FEEDBACK_TIME_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_HEADERS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_REQUIRED_CELL_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_WORKBOOK_EMPTY;

@Component
public class ThirdPartyFeedbackExcelParser {

    public enum ImportWorkbookTemplate {
        THIRD_PARTY,
        LI_PING_DIRECT_WORK_REPORT,
        UNKNOWN
    }

    private static final List<String> REQUIRED_HEADERS = List.of(
            "报工日期", "报工人编码", "报工人名称", "工段长", "生产订单号", "生产资源组", "生产资源", "派工单号",
            "产品编码", "产品名称", "规格", "模具编码", "工序编码", "工序名称", "所属部门", "报工数量", "支数",
            "公斤数", "实腔数", "全程时间", "生产定额", "工作时长", "注塑合模/组装公斤数", "注塑个数/组装个重", "操作"
    );
    private static final List<String> LI_PING_DIRECT_WORK_REPORT_HEADERS = List.of(
            "任务单", "生产订单", "产品代码", "产品名称", "工序编码", "工序名称", "部门", "人员工号",
            "人员名称", "工段长", "日期", "工序单价", "总产出", "总金额"
    );

    private static final List<DateTimeFormatter> FEEDBACK_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm")
    );

    public ImportWorkbookTemplate detectTemplate(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            ImportWorkbookTemplate detectedTemplate = ImportWorkbookTemplate.UNKNOWN;
            int nonEmptySheetCount = 0;
            int headerSize = Math.max(REQUIRED_HEADERS.size(), LI_PING_DIRECT_WORK_REPORT_HEADERS.size());
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (isBlankSheet(sheet, formatter, headerSize)) {
                    continue;
                }
                nonEmptySheetCount++;
                ImportWorkbookTemplate currentTemplate = detectSheetTemplate(sheet, formatter);
                if (currentTemplate == ImportWorkbookTemplate.UNKNOWN) {
                    return ImportWorkbookTemplate.UNKNOWN;
                }
                if (detectedTemplate == ImportWorkbookTemplate.UNKNOWN) {
                    detectedTemplate = currentTemplate;
                    continue;
                }
                if (detectedTemplate != currentTemplate) {
                    return ImportWorkbookTemplate.UNKNOWN;
                }
            }
            return nonEmptySheetCount == 0 ? ImportWorkbookTemplate.UNKNOWN : detectedTemplate;
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(PRO_FEEDBACK_IMPORT_HEADERS_INVALID, ex.getMessage());
        }
    }

    public ThirdPartyFeedbackExcelParseResult parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            List<ThirdPartyFeedbackExcelRow> rows = new ArrayList<>();
            int nonEmptySheetCount = 0;
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (isBlankSheet(sheet, formatter)) {
                    continue;
                }
                nonEmptySheetCount++;
                validateHeaders(sheet, formatter);
                rows.addAll(parseRows(sheet, formatter));
            }
            if (nonEmptySheetCount == 0 || rows.isEmpty()) {
                throw exception(PRO_FEEDBACK_IMPORT_WORKBOOK_EMPTY);
            }
            return new ThirdPartyFeedbackExcelParseResult(nonEmptySheetCount, rows);
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(PRO_FEEDBACK_IMPORT_HEADERS_INVALID, ex.getMessage());
        }
    }

    public DirectWorkReportExcelParseResult parseLiPingDirectWorkReport(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            List<DirectWorkReportExcelRow> rows = new ArrayList<>();
            int skippedRows = 0;
            int nonEmptySheetCount = 0;
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (isBlankSheet(sheet, formatter, LI_PING_DIRECT_WORK_REPORT_HEADERS.size())) {
                    continue;
                }
                nonEmptySheetCount++;
                validateLiPingDirectWorkReportHeaders(sheet, formatter);
                LiPingSheetRows sheetRows = parseLiPingDirectWorkReportRows(sheet, formatter);
                rows.addAll(sheetRows.rows());
                skippedRows += sheetRows.skippedRows();
            }
            if (nonEmptySheetCount == 0 || (rows.isEmpty() && skippedRows == 0)) {
                throw exception(PRO_FEEDBACK_IMPORT_WORKBOOK_EMPTY);
            }
            return new DirectWorkReportExcelParseResult(nonEmptySheetCount, skippedRows, rows);
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(PRO_FEEDBACK_IMPORT_HEADERS_INVALID, ex.getMessage());
        }
    }

    private boolean isBlankSheet(Sheet sheet, DataFormatter formatter) {
        return isBlankSheet(sheet, formatter, REQUIRED_HEADERS.size());
    }

    private boolean isBlankSheet(Sheet sheet, DataFormatter formatter, int headerSize) {
        if (sheet == null) {
            return true;
        }
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            short lastCellNum = row.getLastCellNum();
            for (int cellIndex = 0; cellIndex < Math.max(lastCellNum, headerSize); cellIndex++) {
                if (!normalize(formatter.formatCellValue(row.getCell(cellIndex))).isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void validateHeaders(Sheet sheet, DataFormatter formatter) {
        if (!matchesHeaders(sheet, formatter, REQUIRED_HEADERS)) {
            throw exception(PRO_FEEDBACK_IMPORT_HEADERS_INVALID, sheet.getSheetName());
        }
    }

    private void validateLiPingDirectWorkReportHeaders(Sheet sheet, DataFormatter formatter) {
        if (!matchesHeaders(sheet, formatter, LI_PING_DIRECT_WORK_REPORT_HEADERS)) {
            throw exception(PRO_FEEDBACK_IMPORT_HEADERS_INVALID, sheet.getSheetName());
        }
    }

    private ImportWorkbookTemplate detectSheetTemplate(Sheet sheet, DataFormatter formatter) {
        if (matchesHeaders(sheet, formatter, LI_PING_DIRECT_WORK_REPORT_HEADERS)) {
            return ImportWorkbookTemplate.LI_PING_DIRECT_WORK_REPORT;
        }
        if (matchesHeaders(sheet, formatter, REQUIRED_HEADERS)) {
            return ImportWorkbookTemplate.THIRD_PARTY;
        }
        return ImportWorkbookTemplate.UNKNOWN;
    }

    private boolean matchesHeaders(Sheet sheet, DataFormatter formatter, List<String> expectedHeaders) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return false;
        }
        List<String> actualHeaders = new ArrayList<>(expectedHeaders.size());
        for (int cellIndex = 0; cellIndex < expectedHeaders.size(); cellIndex++) {
            actualHeaders.add(normalize(formatter.formatCellValue(headerRow.getCell(cellIndex))));
        }
        return expectedHeaders.equals(actualHeaders);
    }

    private List<ThirdPartyFeedbackExcelRow> parseRows(Sheet sheet, DataFormatter formatter) {
        List<ThirdPartyFeedbackExcelRow> result = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isBlankRow(row, formatter)) {
                continue;
            }
            int rowNo = rowIndex + 1;
            String feedbackTimeText = requireCell(sheet.getSheetName(), rowNo, formatter, row, 0, "报工日期");
            String feedbackUserCode = requireCell(sheet.getSheetName(), rowNo, formatter, row, 1, "报工人编码");
            String feedbackUserName = normalize(formatter.formatCellValue(row.getCell(2)));
            String approverName = requireCell(sheet.getSheetName(), rowNo, formatter, row, 3, "工段长");
            String workOrderCode = requireCell(sheet.getSheetName(), rowNo, formatter, row, 4, "生产订单号");
            String resourceGroup = normalize(formatter.formatCellValue(row.getCell(5)));
            String resourceName = normalize(formatter.formatCellValue(row.getCell(6)));
            String taskCode = requireCell(sheet.getSheetName(), rowNo, formatter, row, 7, "派工单号");
            String itemCode = requireCell(sheet.getSheetName(), rowNo, formatter, row, 8, "产品编码");
            String itemName = normalize(formatter.formatCellValue(row.getCell(9)));
            String specification = normalize(formatter.formatCellValue(row.getCell(10)));
            String moldCode = normalize(formatter.formatCellValue(row.getCell(11)));
            String processCode = requireCell(sheet.getSheetName(), rowNo, formatter, row, 12, "工序编码");
            String processName = requireCell(sheet.getSheetName(), rowNo, formatter, row, 13, "工序名称");
            String department = normalize(formatter.formatCellValue(row.getCell(14)));
            String feedbackQuantityText = requireCell(sheet.getSheetName(), rowNo, formatter, row, 15, "报工数量");
            BigDecimal feedbackQuantity = parseDecimal(sheet.getSheetName(), rowNo, "报工数量", feedbackQuantityText);
            LocalDateTime feedbackTime = parseFeedbackTime(sheet.getSheetName(), rowNo, row.getCell(0), feedbackTimeText);
            result.add(new ThirdPartyFeedbackExcelRow(
                    sheet.getSheetName(), rowNo, feedbackTime, feedbackUserCode, feedbackUserName, approverName,
                    workOrderCode, resourceGroup, resourceName, taskCode, itemCode, itemName, specification,
                    moldCode, processCode, processName, department, feedbackQuantity
            ));
        }
        return result;
    }

    private LiPingSheetRows parseLiPingDirectWorkReportRows(Sheet sheet, DataFormatter formatter) {
        List<DirectWorkReportExcelRow> result = new ArrayList<>();
        int skippedRows = 0;
        String lastProductionTaskCode = "";
        String lastProductionWorkOrderCode = "";
        String lastProductionItemCode = "";
        String lastProductionItemName = "";
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isBlankRow(row, formatter, LI_PING_DIRECT_WORK_REPORT_HEADERS.size())) {
                continue;
            }
            int rowNo = rowIndex + 1;
            String taskCode = normalize(formatter.formatCellValue(row.getCell(0)));
            String workOrderCode = normalizeLiPingWorkOrderCode(formatter.formatCellValue(row.getCell(1)));
            if (taskCode.isBlank()) {
                taskCode = lastProductionTaskCode;
            }
            if (workOrderCode.isBlank()) {
                workOrderCode = lastProductionWorkOrderCode;
            }
            if (taskCode.isBlank() || workOrderCode.isBlank()) {
                skippedRows++;
                continue;
            }
            String itemCode = normalize(formatter.formatCellValue(row.getCell(2)));
            String itemName = normalize(formatter.formatCellValue(row.getCell(3)));
            if (itemCode.isBlank()) {
                itemCode = lastProductionItemCode;
            }
            if (itemName.isBlank()) {
                itemName = lastProductionItemName;
            }
            if (itemCode.isBlank()) {
                throw exception(PRO_FEEDBACK_IMPORT_REQUIRED_CELL_EMPTY, sheet.getSheetName(), rowNo, "产品代码");
            }
            String processCode = requireCell(sheet.getSheetName(), rowNo, formatter, row, 4, "工序编码");
            String processName = requireCell(sheet.getSheetName(), rowNo, formatter, row, 5, "工序名称");
            String department = normalize(formatter.formatCellValue(row.getCell(6)));
            String feedbackUserCode = requireCell(sheet.getSheetName(), rowNo, formatter, row, 7, "人员工号");
            String feedbackUserName = normalize(formatter.formatCellValue(row.getCell(8)));
            String approverName = requireCell(sheet.getSheetName(), rowNo, formatter, row, 9, "工段长");
            String feedbackTimeText = requireCell(sheet.getSheetName(), rowNo, formatter, row, 10, "日期");
            String feedbackQuantityText = requireCell(sheet.getSheetName(), rowNo, formatter, row, 12, "总产出");
            BigDecimal feedbackQuantity = parseDecimal(sheet.getSheetName(), rowNo, "总产出", feedbackQuantityText);
            LocalDateTime feedbackTime = parseFeedbackTime(sheet.getSheetName(), rowNo, row.getCell(10), feedbackTimeText);
            result.add(new DirectWorkReportExcelRow(sheet.getSheetName(), rowNo, taskCode, workOrderCode,
                    itemCode, itemName, processCode, processName, department, feedbackUserCode, feedbackUserName,
                    approverName, feedbackTime, feedbackQuantity));
            lastProductionTaskCode = taskCode;
            lastProductionWorkOrderCode = workOrderCode;
            lastProductionItemCode = itemCode;
            lastProductionItemName = itemName;
        }
        return new LiPingSheetRows(skippedRows, result);
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        return isBlankRow(row, formatter, REQUIRED_HEADERS.size());
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, int headerSize) {
        short lastCellNum = row.getLastCellNum();
        for (int cellIndex = 0; cellIndex < Math.max(lastCellNum, headerSize); cellIndex++) {
            if (!normalize(formatter.formatCellValue(row.getCell(cellIndex))).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String requireCell(String sheetName, int rowNo, DataFormatter formatter, Row row, int cellIndex, String headerName) {
        String value = normalize(formatter.formatCellValue(row.getCell(cellIndex)));
        if (value.isBlank()) {
            throw exception(PRO_FEEDBACK_IMPORT_REQUIRED_CELL_EMPTY, sheetName, rowNo, headerName);
        }
        return value;
    }

    private BigDecimal parseDecimal(String sheetName, int rowNo, String headerName, String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            throw exception(PRO_FEEDBACK_IMPORT_REQUIRED_CELL_EMPTY, sheetName, rowNo, headerName);
        }
    }

    private LocalDateTime parseFeedbackTime(String sheetName, int rowNo, Cell cell, String text) {
        try {
            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                double numericValue = cell.getNumericCellValue();
                if (DateUtil.isCellDateFormatted(cell) || DateUtil.isValidExcelDate(numericValue)) {
                    return LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(DateUtil.getJavaDate(numericValue).getTime()),
                            ZoneId.systemDefault()
                    );
                }
            }
            for (DateTimeFormatter formatter : FEEDBACK_TIME_FORMATTERS) {
                try {
                    return LocalDateTime.parse(text, formatter);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        throw exception(PRO_FEEDBACK_IMPORT_FEEDBACK_TIME_INVALID, sheetName, rowNo, text);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeLiPingWorkOrderCode(String value) {
        String normalized = normalize(value);
        if (normalized.matches(".+-\\d+")) {
            return normalized.substring(0, normalized.lastIndexOf('-'));
        }
        return normalized;
    }

    private record LiPingSheetRows(int skippedRows, List<DirectWorkReportExcelRow> rows) {
    }
}
