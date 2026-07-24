package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_HEADERS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_REQUIRED_CELL_EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThirdPartyFeedbackExcelParserTest {

    private final ThirdPartyFeedbackExcelParser parser = new ThirdPartyFeedbackExcelParser();

    @Test
    void parseWorkbook_readsAllNonEmptySheets() throws Exception {
        byte[] workbookBytes = buildWorkbook();

        ThirdPartyFeedbackExcelParseResult result = parser.parse(new ByteArrayInputStream(workbookBytes));

        assertEquals(2, result.sheetCount());
        assertEquals(2, result.rows().size());
        assertEquals("棘突球囊报工", result.rows().get(0).sheetName());
        assertEquals(2, result.rows().get(0).rowNo());
        assertEquals("TASK-001", result.rows().get(0).taskCode());
        assertEquals(LocalDateTime.of(2026, 4, 9, 15, 27, 17), result.rows().get(0).feedbackTime());
        assertEquals("造影导管", result.rows().get(1).sheetName());
        assertEquals("TASK-002", result.rows().get(1).taskCode());
    }

    @Test
    void parseWorkbook_withInvalidHeaders_failsFast() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("棘突球囊报工");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("错误表头");
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse(new ByteArrayInputStream(workbookBytes)));
        assertEquals(PRO_FEEDBACK_IMPORT_HEADERS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void parseWorkbook_withRequiredCellEmpty_failsFast() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("棘突球囊报工");
            createHeader(sheet);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-04-09 15:27:17");
            row.createCell(1).setCellValue("A4040003");
            row.createCell(2).setCellValue("吴廷");
            row.createCell(3).setCellValue("潘金华");
            row.createCell(4).setCellValue("MO-001");
            row.createCell(5).setCellValue("包装工段");
            row.createCell(6).setCellValue("纸塑袋封口全检");
            row.createCell(8).setCellValue("ITEM-001");
            row.createCell(9).setCellValue("产品A");
            row.createCell(10).setCellValue("SPEC-A");
            row.createCell(12).setCellValue("PROC-001");
            row.createCell(13).setCellValue("纸塑袋封口全检");
            row.createCell(14).setCellValue("组装");
            row.createCell(15).setCellValue(234);
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse(new ByteArrayInputStream(workbookBytes)));
        assertEquals(PRO_FEEDBACK_IMPORT_REQUIRED_CELL_EMPTY.getCode(), exception.getCode());
    }

    @Test
    void parseLiPingDirectWorkReport_readsProductionRowsAndSkipsMiscRows() throws Exception {
        byte[] workbookBytes = buildLiPingWorkbook();

        DirectWorkReportExcelParseResult result = parser.parseLiPingDirectWorkReport(new ByteArrayInputStream(workbookBytes));

        assertEquals(1, result.sheetCount());
        assertEquals(1, result.skippedRows());
        assertEquals(1, result.rows().size());
        DirectWorkReportExcelRow row = result.rows().get(0);
        assertEquals("李萍报工单", row.sheetName());
        assertEquals(3, row.rowNo());
        assertEquals("881MO093613-1-11", row.taskCode());
        assertEquals("881MO093613", row.workOrderCode());
        assertEquals("3020110069", row.itemCode());
        assertEquals("Z2570", row.processCode());
        assertEquals("A2020002", row.feedbackUserCode());
        assertEquals("李萍", row.approverName());
        assertEquals(LocalDateTime.of(2026, 4, 9, 15, 27, 17), row.feedbackTime());
        assertEquals(0, new BigDecimal("213").compareTo(row.feedbackQuantity()));
    }

    @Test
    void parseLiPingDirectWorkReport_normalizesProductionOrderSuffixOnly() throws Exception {
        byte[] workbookBytes = buildLiPingWorkbook();

        DirectWorkReportExcelParseResult result = parser.parseLiPingDirectWorkReport(new ByteArrayInputStream(workbookBytes));

        DirectWorkReportExcelRow row = result.rows().get(0);
        assertEquals("881MO093613-1-11", row.taskCode());
        assertEquals("881MO093613", row.workOrderCode());
    }

    @Test
    void parseLiPingDirectWorkReport_fillsBlankTaskAndOrderFromPreviousProductionRow() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            createLiPingHeader(sheet);
            fillLiPingRow(sheet.createRow(1), "881MO093616-1", "881MO093616-1", "YXN.069.001.1005",
                    "冠状动脉棘突球囊扩张导管", "Z2976", "棘突丝切割", "组装", "A2020113",
                    "朱欣妮", "李萍临时工", "2026/7/5 16:32:40", 449);
            fillLiPingRow(sheet.createRow(2), "", "", "YXN.069.001.1005",
                    "冠状动脉棘突球囊扩张导管", "OEO00035", "杂务计时", "组装", "A2020114",
                    "张雅倩", "李萍临时工", "2026/7/5 16:44:38", 5);
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        DirectWorkReportExcelParseResult result = parser.parseLiPingDirectWorkReport(new ByteArrayInputStream(workbookBytes));

        assertEquals(1, result.sheetCount());
        assertEquals(0, result.skippedRows());
        assertEquals(2, result.rows().size());
        DirectWorkReportExcelRow inheritedRow = result.rows().get(1);
        assertEquals(3, inheritedRow.rowNo());
        assertEquals("881MO093616-1", inheritedRow.taskCode());
        assertEquals("881MO093616", inheritedRow.workOrderCode());
        assertEquals("OEO00035", inheritedRow.processCode());
        assertEquals(0, new BigDecimal("5").compareTo(inheritedRow.feedbackQuantity()));
    }

    @Test
    void parseLiPingDirectWorkReport_fillsBlankProductCodeFromPreviousProductionRow() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            createLiPingHeader(sheet);
            fillLiPingRow(sheet.createRow(1), "881MO093616-1", "881MO093616-1", "YXN.069.001.1005",
                    "冠状动脉棘突球囊扩张导管", "Z2976", "棘突丝切割", "组装", "A2020113",
                    "朱欣妮", "李萍临时工", "2026/7/5 16:32:40", 449);
            fillLiPingRow(sheet.createRow(2), "", "", "",
                    "", "Z2977", "棘突丝折弯", "组装", "A2020114",
                    "张雅倩", "李萍临时工", "2026/7/5 16:44:38", 5);
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        DirectWorkReportExcelParseResult result = parser.parseLiPingDirectWorkReport(new ByteArrayInputStream(workbookBytes));

        assertEquals(1, result.sheetCount());
        assertEquals(0, result.skippedRows());
        assertEquals(2, result.rows().size());
        DirectWorkReportExcelRow inheritedRow = result.rows().get(1);
        assertEquals(3, inheritedRow.rowNo());
        assertEquals("881MO093616-1", inheritedRow.taskCode());
        assertEquals("881MO093616", inheritedRow.workOrderCode());
        assertEquals("YXN.069.001.1005", inheritedRow.itemCode());
        assertEquals("冠状动脉棘突球囊扩张导管", inheritedRow.itemName());
        assertEquals("Z2977", inheritedRow.processCode());
    }

    @Test
    void parseLiPingDirectWorkReport_withOnlyMiscRows_returnsSkippedRows() throws Exception {
        byte[] workbookBytes = buildLiPingMiscOnlyWorkbook();

        DirectWorkReportExcelParseResult result = parser.parseLiPingDirectWorkReport(new ByteArrayInputStream(workbookBytes));

        assertEquals(1, result.sheetCount());
        assertEquals(2, result.skippedRows());
        assertEquals(0, result.rows().size());
    }

    @Test
    void parseLiPingDirectWorkReport_skipsBlankProductionOrderBeforeRequiringTaskCode() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            createLiPingHeader(sheet);
            fillLiPingRow(sheet.createRow(1), "", "", "3020110069", "外鞘管组件",
                    "Z2570", "外鞘管组件包装", "组装", "A2020002", "李萍", "李萍",
                    "2026/4/9 15:20:00", 10);
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        DirectWorkReportExcelParseResult result = parser.parseLiPingDirectWorkReport(new ByteArrayInputStream(workbookBytes));

        assertEquals(1, result.sheetCount());
        assertEquals(1, result.skippedRows());
        assertEquals(0, result.rows().size());
    }

    @Test
    void parseLiPingDirectWorkReport_skipsBlankTaskCodeBeforeRequiringOtherCells() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            createLiPingHeader(sheet);
            fillLiPingRow(sheet.createRow(1), "", "KDMO-309622-666472926", "YXN.037.011.1008", "冠状动脉棘突球囊扩张导管",
                    "Z2570", "快速交换口焊接", "组装", "A2020002", "李萍", "李萍",
                    "2026/7/5 15:24:22", 213);
            fillLiPingRow(sheet.createRow(2), "881MO093616-1-16", "MO000093759", "YXN.069.001.1005", "冠状动脉棘突球囊扩张导管",
                    "Z2570", "快速交换口焊接", "组装", "A2020002", "李萍", "李萍",
                    "2026/7/5 15:30:00", 185);
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        DirectWorkReportExcelParseResult result = parser.parseLiPingDirectWorkReport(new ByteArrayInputStream(workbookBytes));

        assertEquals(1, result.sheetCount());
        assertEquals(1, result.skippedRows());
        assertEquals(1, result.rows().size());
        DirectWorkReportExcelRow row = result.rows().get(0);
        assertEquals(3, row.rowNo());
        assertEquals("881MO093616-1-16", row.taskCode());
        assertEquals("MO000093759", row.workOrderCode());
    }

    private byte[] buildWorkbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var first = workbook.createSheet("棘突球囊报工");
            createHeader(first);
            var firstRow = first.createRow(1);
        fillRow(firstRow, LocalDateTime.of(2026, 4, 9, 15, 27, 17), "A4040003", "吴廷", "潘金华",
                "MO-001", "包装工段", "纸塑袋封口全检", "TASK-001", "ITEM-001", "产品A", "SPEC-A",
                "MOLD-1", "PROC-001", "纸塑袋封口全检", "组装", 234);

            var second = workbook.createSheet("造影导管");
            createHeader(second);
            var secondRow = second.createRow(1);
        fillRow(secondRow, LocalDateTime.of(2026, 4, 8, 21, 40, 37), "A4050006", "汤小芹", "刘青",
                "MO-002", "造影导管工段", "多功能造影导管包装", "TASK-002", "ITEM-002", "产品B", "SPEC-B",
                "", "PROC-002", "多功能造影导管包装", "组装", 250);

            workbook.createSheet("空白Sheet");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildLiPingWorkbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            createLiPingHeader(sheet);
            var miscRow = sheet.createRow(1);
            fillLiPingRow(miscRow, "杂务计时", "", "3020110069", "外鞘管组件", "ZW001",
                    "杂务", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:20:00", 0);
            var productionRow = sheet.createRow(2);
            fillLiPingRow(productionRow, "881MO093613-1-11", "881MO093613-1", "3020110069", "外鞘管组件",
                    "Z2570", "外鞘管组件包装", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:27:17", 213);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildLiPingMiscOnlyWorkbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("李萍报工单");
            createLiPingHeader(sheet);
            fillLiPingRow(sheet.createRow(1), "杂务计时", "", "3020110069", "外鞘管组件",
                    "ZW001", "杂务", "组装", "A2020002", "李萍", "李萍", "2026/4/9 15:20:00", 0);
            fillLiPingRow(sheet.createRow(2), "整理工时", "", "3020110069", "外鞘管组件",
                    "ZW002", "杂务", "组装", "A2020002", "李萍", "李萍", "2026/4/9 16:20:00", 0);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createHeader(org.apache.poi.ss.usermodel.Sheet sheet) {
        var header = sheet.createRow(0);
        List.of(
                "报工日期", "报工人编码", "报工人名称", "工段长", "生产订单号", "生产资源组", "生产资源", "派工单号",
                "产品编码", "产品名称", "规格", "模具编码", "工序编码", "工序名称", "所属部门", "报工数量", "支数",
                "公斤数", "实腔数", "全程时间", "生产定额", "工作时长", "注塑合模/组装公斤数", "注塑个数/组装个重", "操作"
        ).forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));
    }

    private void createLiPingHeader(org.apache.poi.ss.usermodel.Sheet sheet) {
        var header = sheet.createRow(0);
        List.of(
                "任务单", "生产订单", "产品代码", "产品名称", "工序编码", "工序名称", "部门", "人员工号",
                "人员名称", "工段长", "日期", "工序单价", "总产出", "总金额"
        ).forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));
    }

    private void fillRow(org.apache.poi.ss.usermodel.Row row, LocalDateTime feedbackTime, String feedbackUserCode,
                         String feedbackUserName, String approverName, String workOrderCode, String resourceGroup,
                         String resourceName, String taskCode, String itemCode, String itemName, String specification,
                         String moldCode, String processCode, String processName, String department, int quantity) {
        row.createCell(0).setCellValue(Date.from(feedbackTime.atZone(ZoneId.systemDefault()).toInstant()));
        row.createCell(1).setCellValue(feedbackUserCode);
        row.createCell(2).setCellValue(feedbackUserName);
        row.createCell(3).setCellValue(approverName);
        row.createCell(4).setCellValue(workOrderCode);
        row.createCell(5).setCellValue(resourceGroup);
        row.createCell(6).setCellValue(resourceName);
        row.createCell(7).setCellValue(taskCode);
        row.createCell(8).setCellValue(itemCode);
        row.createCell(9).setCellValue(itemName);
        row.createCell(10).setCellValue(specification);
        row.createCell(11).setCellValue(moldCode);
        row.createCell(12).setCellValue(processCode);
        row.createCell(13).setCellValue(processName);
        row.createCell(14).setCellValue(department);
        row.createCell(15).setCellValue(quantity);
        row.createCell(24).setCellValue("删除");
    }

    private void fillLiPingRow(org.apache.poi.ss.usermodel.Row row, String taskCode, String workOrderCode,
                               String itemCode, String itemName, String processCode, String processName,
                               String department, String feedbackUserCode, String feedbackUserName,
                               String approverName, String feedbackTime, int outputQuantity) {
        row.createCell(0).setCellValue(taskCode);
        row.createCell(1).setCellValue(workOrderCode);
        row.createCell(2).setCellValue(itemCode);
        row.createCell(3).setCellValue(itemName);
        row.createCell(4).setCellValue(processCode);
        row.createCell(5).setCellValue(processName);
        row.createCell(6).setCellValue(department);
        row.createCell(7).setCellValue(feedbackUserCode);
        row.createCell(8).setCellValue(feedbackUserName);
        row.createCell(9).setCellValue(approverName);
        row.createCell(10).setCellValue(feedbackTime);
        row.createCell(11).setCellValue(1.23);
        row.createCell(12).setCellValue(outputQuantity);
        row.createCell(13).setCellValue(outputQuantity * 1.23);
    }
}
