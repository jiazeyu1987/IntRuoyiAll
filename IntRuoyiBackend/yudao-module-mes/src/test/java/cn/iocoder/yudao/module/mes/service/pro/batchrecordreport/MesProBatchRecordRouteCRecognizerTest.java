package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteCRecognizerTest {

    private static final String SOURCE_DOC_FILE_NAME = "source.doc";
    private static final int EXPECTED_FIXTURE_TEMPLATE_COUNT = 15;

    @Test
    void recognize_normalizedDocx_returnsFifteenBusinessTemplates() throws Exception {
        AtomicReference<String> fileNameRef = new AtomicReference<>();
        MesProBatchRecordRouteCRecognizer recognizer = new MesProBatchRecordRouteCRecognizer((originalFileName, bytes) -> {
            fileNameRef.set(originalFileName);
            return new MesProBatchRecordRouteCRecognizer.NormalizedDocument("docx", fifteenTemplateDocx());
        });

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                SOURCE_DOC_FILE_NAME,
                "fake-doc".getBytes(StandardCharsets.UTF_8));

        assertEquals(EXPECTED_FIXTURE_TEMPLATE_COUNT, tables.size());
        assertEquals(SOURCE_DOC_FILE_NAME, fileNameRef.get());
        assertEquals(1, tables.get(0).getSourceTableIndex());
        assertEquals(EXPECTED_FIXTURE_TEMPLATE_COUNT, tables.get(14).getSourceTableIndex());
        assertEquals("Product Information", tables.get(0).getTableTitle());
        assertEquals("Outer Packaging Operation Record", tables.get(14).getTableTitle());
        assertTrue(tables.stream().allMatch(table -> !table.getRows().isEmpty()));
        assertFalse(tables.stream().anyMatch(table -> table.getTableTitle().isBlank()));
    }

    @Test
    void recognize_whenNormalizationOutputMissing_failFast() {
        MesProBatchRecordRouteCRecognizer recognizer = new MesProBatchRecordRouteCRecognizer((originalFileName, bytes) ->
                new MesProBatchRecordRouteCRecognizer.NormalizedDocument("docx", new byte[0]));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recognizer.recognize("pilot.doc", "fake-doc".getBytes(StandardCharsets.UTF_8)));

        assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
    }

    @Test
    void recognize_whenNormalizationOutputIsInvalidDocx_failFast() {
        MesProBatchRecordRouteCRecognizer recognizer = new MesProBatchRecordRouteCRecognizer((originalFileName, bytes) ->
                new MesProBatchRecordRouteCRecognizer.NormalizedDocument("docx",
                        "not-a-docx".getBytes(StandardCharsets.UTF_8)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recognizer.recognize("pilot.doc", "fake-doc".getBytes(StandardCharsets.UTF_8)));

        assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
    }

    @Test
    void recognize_whenConfiguredPythonCommandCannotStart_failFastWithToolchainMessage() throws Exception {
        MesProBatchRecordRouteCRecognizer recognizer = new MesProBatchRecordRouteCRecognizer();
        setField(recognizer, "pythonCommand", Path.of("missing-route-c-python.cmd").toString());
        setField(recognizer, "pythonWorkingDirectory", System.getProperty("user.dir"));
        setField(recognizer, "normalizeTimeoutMs", 1000L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recognizer.recognize("pilot.doc", "fake-doc".getBytes(StandardCharsets.UTF_8)));

        assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("route_c_python_process_start_failed"));
    }

    @Test
    void recognize_whenHeaderContainsChecklistSuffix_keepsHeaderVisibleAfterLayoutCalibration() throws Exception {
        MesProBatchRecordRouteCRecognizer recognizer = new MesProBatchRecordRouteCRecognizer((originalFileName, bytes) ->
                new MesProBatchRecordRouteCRecognizer.NormalizedDocument("docx",
                        fifteenTemplateDocxWithCombinedChecklistHeadersAndWideGrid()));

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                SOURCE_DOC_FILE_NAME,
                "fake-doc".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordParsedTable firstOperationTable = tables.get(1);
        MesProBatchRecordParsedTable calibrated = new MesProBatchRecordReportLayoutCalibrator().calibrate(firstOperationTable);
        String expectedHeader = "\u7c97\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55\n\u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f";
        assertEquals("\u7c97\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55", firstOperationTable.getTableTitle());
        assertEquals(expectedHeader, firstOperationTable.getRows().get(0).get(0).getText());
        assertEquals("\u751f\u4ea7\u6279\u53f7", firstOperationTable.getRows().get(1).get(0).getText());
        int headerRowIndex = findFirstRowIndex(calibrated, expectedHeader);
        int metadataRowIndex = findFirstRowIndex(calibrated, "\u751f\u4ea7\u6279\u53f7");
        assertEquals(expectedHeader, calibrated.getRows().get(headerRowIndex).get(0).getText());
        assertEquals(19, calibrated.getRows().get(headerRowIndex).get(0).getColSpan());
        assertEquals(6, calibrated.getRows().get(metadataRowIndex).size());
        assertEquals(19, calibrated.getRows().get(metadataRowIndex).stream()
                .mapToInt(MesProBatchRecordParsedCell::getColSpan)
                .sum());
        assertTrue(calibrated.getRows().get(metadataRowIndex).stream().limit(5)
                        .allMatch(cell -> cell.getColSpan() == 1),
                "leading metadata fields must stay individually addressable, spans="
                        + calibrated.getRows().get(metadataRowIndex).stream().map(MesProBatchRecordParsedCell::getColSpan).toList());
        assertEquals(14, calibrated.getRows().get(metadataRowIndex).get(5).getColSpan(),
                "the final value cell may absorb the remaining visual grid width without collapsing preceding fields");
    }

    private static int findFirstRowIndex(MesProBatchRecordParsedTable table, String firstCellText) {
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = table.getRows().get(rowIndex);
            if (!row.isEmpty() && firstCellText.equals(row.get(0).getText())) {
                return rowIndex;
            }
        }
        throw new AssertionError("Missing row with first cell text: " + firstCellText);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> type = target.getClass();
        java.lang.reflect.Field field = null;
        while (type != null && field == null) {
            try {
                field = type.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(fieldName);
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    private static byte[] fifteenTemplateDocx() {
        String[][] groupedTitles = new String[][]{
                {"Product Information"},
                {"Rough Wash Operation Record"},
                {"Fine Wash Operation Record", "Cleaning Operation Record"},
                {"Purification Operation Record"},
                {"Assembly I Operation Record", "Light Cure I Operation Record"},
                {"Siliconization I Operation Record"},
                {"Siliconization II Operation Record"},
                {"Assembly II Operation Record", "Inspection Operation Record"},
                {"Light Cure II Operation Record"},
                {"Inner Packaging Operation Record", "Middle Packaging Operation Record", "Outer Packaging Operation Record"}
        };
        try {
            try (XWPFDocument document = new XWPFDocument();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                for (int tableIndex = 0; tableIndex < groupedTitles.length; tableIndex++) {
                    int rowCount = groupedTitles[tableIndex].length * 2;
                    XWPFTable table = document.createTable(rowCount, 2);
                    int rowIndex = 0;
                    for (String title : groupedTitles[tableIndex]) {
                        table.getRow(rowIndex).getCell(0).setText(title);
                        table.getRow(rowIndex).getCell(1).setText("Pressure Pump");
                        table.getRow(rowIndex + 1).getCell(0).setText("Operator");
                        table.getRow(rowIndex + 1).getCell(1).setText("Alice");
                        rowIndex += 2;
                    }
                    if (tableIndex < groupedTitles.length - 1) {
                        document.createParagraph();
                    }
                }
                document.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to build route-c docx fixture", ex);
        }
    }

    private static byte[] fifteenTemplateDocxWithCombinedChecklistHeaders() {
        String[] titles = new String[]{
                "\u4ea7\u54c1\u4fe1\u606f",
                "\u7c97\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7cbe\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u6e05\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7ec4\u88c5\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u5149\u56fa\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7845\u5316\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7845\u5316\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7ec4\u88c5\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u68c0\u6d4b\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u5149\u56fa\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u5355\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u4e2d\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u5927\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f"
        };
        try {
            try (XWPFDocument document = new XWPFDocument();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                for (String title : titles) {
                    XWPFTable table = document.createTable(2, 1);
                    table.getRow(0).getCell(0).setText(title);
                    table.getRow(1).getCell(0).setText("Batch No.");
                    document.createParagraph();
                }
                document.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to build route-c checklist fixture", ex);
        }
    }

    private static byte[] fifteenTemplateDocxWithCombinedChecklistHeadersAndWideGrid() {
        String[] titles = new String[]{
                "\u4ea7\u54c1\u4fe1\u606f",
                "\u7c97\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7cbe\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u6e05\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7ec4\u88c5\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u5149\u56fa\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7845\u5316\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7845\u5316\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u7ec4\u88c5\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u68c0\u6d4b\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u5149\u56fa\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u5355\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u2611\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u25a1\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u4e2d\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f",
                "\u5927\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f"
        };
        try {
            try (XWPFDocument document = new XWPFDocument();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                document.createTable(1, 1).getRow(0).getCell(0).setText(titles[0]);
                document.createParagraph();
                for (int index = 1; index < titles.length; index++) {
                    XWPFTable table = document.createTable(3, 1);
                    setGridSpan(table.getRow(0).getCell(0), 44);
                    table.getRow(0).getCell(0).setText(titles[index]);
                    XWPFTableRow batchRow = table.getRow(1);
                    populateGridRow(batchRow,
                            new String[]{"\u751f\u4ea7\u6279\u53f7", "", "\u4ea7\u54c1\u89c4\u683c", "", "\u751f\u4ea7\u4f9d\u636e", "PP-ID-1-04"},
                            new int[]{6, 7, 12, 8, 3, 8});
                    XWPFTableRow headerRow = table.getRow(2);
                    populateGridRow(headerRow,
                            new String[]{"\u64cd\u4f5c\u65e5\u671f", "\u7269\u6599\u7f16\u7801", "\u7269\u6599\u540d\u79f0", "\u6279\u53f7", "\u6e05\u6d17\u6b21\u6570", "\u6e05\u6d17\u529f\u7387"},
                            new int[]{3, 3, 3, 3, 3, 4});
                    document.createParagraph();
                }
                document.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to build route-c wide-grid checklist fixture", ex);
        }
    }

    private static void populateGridRow(XWPFTableRow row, String[] texts, int[] spans) {
        for (int index = 1; index < texts.length; index++) {
            row.addNewTableCell();
        }
        for (int index = 0; index < texts.length; index++) {
            XWPFTableCell cell = row.getCell(index);
            cell.setText(texts[index]);
            setGridSpan(cell, spans[index]);
        }
    }

    private static void setGridSpan(XWPFTableCell cell, int span) {
        if (cell.getCTTc().getTcPr() == null) {
            cell.getCTTc().addNewTcPr();
        }
        cell.getCTTc().getTcPr().addNewGridSpan().setVal(BigInteger.valueOf(span));
    }
}
