package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordDocParserTest {

    private static final Path PILOT_SAMPLE = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
    private static final Path FIXED_SAMPLE = Path.of(
            "D:\\ProjectPackage\\Int\\IntRuoyi\\resource\\\u6279\u8bb0\u5f55\u6a21\u677f.doc");
    private static final Path PRESSURE_PUMP_SAMPLE = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
    private static final Path PROCESS_INSPECTION_DOCX_SAMPLE = Path.of(
            "C:\\Users\\BJB110\\Desktop\\\u6587\u6863\\\u8fc7\u7a0b\u68c0\u9a8c\u8bb0\u5f55.docx");

    private static final List<String> EXPECTED_TITLES = List.of(
            "\u4ea7\u54c1\u4fe1\u606f",
            "\u7c97\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7cbe\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u6e05\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5149\u56fa\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7845\u5316\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u7ec4\u88c5\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u68c0\u6d4b\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5149\u56fa\u2161\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5355\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u4e2d\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
            "\u5927\u5305\u88c5\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55"
    );

    private static final List<Integer> EXPECTED_ROW_COUNTS = List.of(
            46, 19, 19, 37, 21, 17, 17, 19, 18, 19, 19, 19, 23, 17, 17
    );

    private final MesProBatchRecordDocParser parser = new MesProBatchRecordDocParser();

    @Test
    void splitTemplates_usesGenericInfoHeaderRuleFromSharedPageTypeTitles() throws Exception {
        List<MesProBatchRecordParsedTable> templates = invokeSplitTemplates(MesProBatchRecordParsedTable.builder()
                .tableTitle("\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5\u751f\u4ea7\u8bb0\u5f55")
                .rowCount(3)
                .columnCount(3)
                .rows(List.of(
                        row("\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5\u751f\u4ea7\u8bb0\u5f55", "\u8bb0\u5f55\u7f16\u53f7", "RE-PP-ID-01"),
                        row("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f"),
                        row("\u5de5\u5e8f\u540d\u79f0", "\u64cd\u4f5c\u4eba\u5458", "\u88c5\u914d\u65e5\u671f")
                ))
                .build());

        assertEquals(1, templates.size());
        assertEquals("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f", templates.get(0).getTableTitle());
        assertEquals(3, templates.get(0).getRowCount());
    }

    @Test
    void splitTemplates_usesSharedSummaryHeaderRule() throws Exception {
        List<MesProBatchRecordParsedTable> templates = invokeSplitTemplates(MesProBatchRecordParsedTable.builder()
                .tableTitle("\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5\u751f\u4ea7\u8bb0\u5f55")
                .rowCount(3)
                .columnCount(3)
                .rows(List.of(
                        row("\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5\u751f\u4ea7\u8bb0\u5f55", "\u8bb0\u5f55\u7f16\u53f7", "RE-PP-ID-01"),
                        row("\u751f\u4ea7\u8bb0\u5f55\u6c47\u603b\u8868"),
                        row("\u9879\u76ee", "\u7ed3\u679c", "\u5907\u6ce8")
                ))
                .build());

        assertEquals(1, templates.size());
        assertEquals("\u751f\u4ea7\u8bb0\u5f55\u6c47\u603b\u8868", templates.get(0).getTableTitle());
        assertEquals(3, templates.get(0).getRowCount());
    }

    @Test
    void splitTemplates_preservesLeadingInfoBlocksWhenLaterSharedSectionsStayOnTheSameSourcePage() throws Exception {
        List<MesProBatchRecordParsedTable> templates = invokeSplitTemplates(MesProBatchRecordParsedTable.builder()
                .tableTitle("\u4ea7\u54c1\u4fe1\u606f")
                .rowCount(9)
                .columnCount(3)
                .rows(List.of(
                        row("\u4ea7\u54c1\u4fe1\u606f"),
                        row("\u4ea7\u54c1\u540d\u79f0", "\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5", "\u578b\u53f7\u89c4\u683c"),
                        row("\u914d\u4ef6\u8fdb\u8d27\u6279\u53f7\u4fe1\u606f"),
                        row("\u7269\u6599\u7f16\u7801", "\u7269\u6599\u540d\u79f0", "\u7269\u6599\u6279\u53f7"),
                        row("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f"),
                        row("\u5de5\u5e8f\u540d\u79f0", "\u64cd\u4f5c\u4eba\u5458", "\u88c5\u914d\u65e5\u671f"),
                        row("\u7c97\u6d17", "", ""),
                        row("\u8fc7\u7a0b\u653e\u884c\u4fe1\u606f"),
                        row("\u8fc7\u7a0b\u653e\u884c\u4eba/\u653e\u884c\u65e5\u671f\uff1a")
                ))
                .build());

        assertEquals(1, templates.size());
        assertEquals("\u4ea7\u54c1\u4fe1\u606f", templates.get(0).getTableTitle());
        assertEquals(9, templates.get(0).getRowCount());
        assertEquals("\u4ea7\u54c1\u4fe1\u606f", templates.get(0).getRows().get(0).get(0).getText());
        assertEquals("\u8fc7\u7a0b\u653e\u884c\u4eba/\u653e\u884c\u65e5\u671f\uff1a",
                templates.get(0).getRows().get(8).get(0).getText());
    }

    @Test
    void parseFixedSourceDoc_preservesLeadingSummaryAndProductInfoBlocks() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = parser.parse(bytes);

        assertEquals(15, tables.size());
        assertEquals("\u4ea7\u54c1\u4fe1\u606f", tables.get(0).getTableTitle());
        assertTrue(tables.get(0).getRowCount() > 14);
        assertContainsText(tables.get(0), "\u4ea7\u54c1\u4fe1\u606f");
        assertContainsText(tables.get(0), "\u914d\u4ef6\u8fdb\u8d27\u6279\u53f7\u4fe1\u606f");
        assertContainsText(tables.get(0), "\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f");
        assertContainsText(tables.get(0), "\u8fc7\u7a0b\u653e\u884c\u4fe1\u606f");
    }

    @Test
    void parseFixedSourceDoc_shouldCaptureExplicitCellBorderStyles() throws Exception {
        Assumptions.assumeTrue(Files.exists(FIXED_SAMPLE), "fixed sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(FIXED_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = parser.parse(bytes);

        boolean foundExplicitBorder = tables.stream()
                .flatMap(table -> table.getRows().stream())
                .flatMap(List::stream)
                .anyMatch(cell -> cell.getTopBorderStyle() != null
                        || cell.getBottomBorderStyle() != null
                        || cell.getLeftBorderStyle() != null
                        || cell.getRightBorderStyle() != null);

        assertTrue(foundExplicitBorder, "fixed source doc should expose at least one explicit cell border style");
    }

    @Test
    void parseDocx_readsGenericWordTablesWithMergedHeader() throws Exception {
        byte[] bytes = buildDocxProcessInspectionSample();

        List<MesProBatchRecordParsedTable> tables = parser.parseDocx(bytes);

        assertEquals(1, tables.size());
        MesProBatchRecordParsedTable table = tables.get(0);
        assertEquals("按压式球囊扩张压力泵组装过程检验记录", table.getTableTitle());
        assertEquals(3, table.getRowCount());
        assertTrue(table.getColumnCount() >= 3);
        assertEquals(3, table.getRows().get(0).get(0).getColSpan());
        assertContainsText(table, "检验项目");
        assertContainsText(table, "判定");
    }

    @Test
    void parseDocx_extractsGenericHeaderAndFooterFrameFromDocumentParts() throws Exception {
        byte[] bytes = buildDocxWithDocumentFrameSample();

        List<MesProBatchRecordParsedTable> tables = parser.parseDocx(bytes);

        assertEquals(1, tables.size());
        MesProBatchRecordDocumentFrame documentFrame = tables.get(0).getDocumentFrame();
        assertFalse(documentFrame.getHeaderRows().isEmpty(), "docx header table rows should be preserved");
        assertContainsText(documentFrame.getHeaderRows(), "文件标题");
        assertContainsText(documentFrame.getHeaderRows(), "记录编号");
        assertContainsText(documentFrame.getHeaderRows(), "DOC-001");
        assertContainsText(documentFrame.getFooterRows(), "生效日期：2026年02月02日");
    }

    @Test
    void parseDocx_preservesRealProcessInspectionDocumentHeaderFrame() throws Exception {
        Assumptions.assumeTrue(Files.exists(PROCESS_INSPECTION_DOCX_SAMPLE),
                "process inspection docx sample fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PROCESS_INSPECTION_DOCX_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = parser.parseDocx(bytes);

        assertFalse(tables.isEmpty());
        MesProBatchRecordDocumentFrame documentFrame = tables.get(0).getDocumentFrame();
        assertFalse(documentFrame.getHeaderRows().isEmpty(), "real process inspection docx header should be preserved");
        assertContainsText(documentFrame.getHeaderRows(), "过程检验记录");
        assertContainsText(documentFrame.getHeaderRows(), "记录编号");
        assertContainsText(documentFrame.getHeaderRows(), "RE-PQC-IDPR-001-01");
        assertContainsText(documentFrame.getHeaderRows(), "版本");
        assertContainsText(documentFrame.getHeaderRows(), "页码");
        assertContainsText(documentFrame.getHeaderRows(), "1 of 5");
        assertNotContainsText(documentFrame.getHeaderRows(), "1 of 8");

        MesProBatchRecordParsedTable calibrated =
                new MesProBatchRecordReportLayoutCalibrator().calibrate(tables.get(0));
        assertTrue(rowIndexOfText(calibrated.getRows(), "过程检验记录") >= 0,
                "calibrated recognition should render the docx header before the body table");
        assertTrue(rowIndexOfText(calibrated.getRows(), "生产批号")
                        > rowIndexOfText(calibrated.getRows(), "过程检验记录"),
                "body table should stay after the restored docx header");
    }

    @Test
    void parseDocx_preservesDeclaredTableRowHeights() throws Exception {
        byte[] bytes = buildDocxWithDeclaredRowHeights();

        List<MesProBatchRecordParsedTable> tables = parser.parseDocx(bytes);

        assertEquals(1, tables.size());
        assertEquals(24, rowHeight(tables.get(0).getRows().get(0)));
        assertEquals(48, rowHeight(tables.get(0).getRows().get(1)));
    }

    @Test
    void parseDocx_preservesUnderlinedBlankRunsAsFillableMarkers() throws Exception {
        byte[] bytes = buildDocxWithUnderlinedBlankRuns();

        List<MesProBatchRecordParsedTable> tables = parser.parseDocx(bytes);

        assertEquals(1, tables.size());
        String text = tables.get(0).getRows().get(0).get(0).getText();
        assertTrue(text.contains("合格数量：___"), "underlined blank runs after labels should survive parsing");
        assertTrue(text.contains("不合格数量：___"), "multiple underlined blank runs in one cell should be preserved");
        assertFalse(text.contains("合格数量：；"), "underlined blanks must not collapse into an empty label separator");
    }

    @Test
    void parseDocx_marksDiagonalBorderCellsAsForbidden() throws Exception {
        byte[] bytes = buildDocxDiagonalSlashSample();

        List<MesProBatchRecordParsedTable> tables = parser.parseDocx(bytes);

        MesProBatchRecordParsedCell forbiddenCell = tables.get(0).getRows().get(1).get(1);
        assertTrue(forbiddenCell.isDiagonalSlash(), "Word diagonal border cells must be preserved as forbidden cells");
        assertEquals("", forbiddenCell.getText());
    }

    @Test
    void parsePilotSample_returnsTemplatesFromPilotDoc() throws Exception {
        Assumptions.assumeTrue(Files.exists(PILOT_SAMPLE), "pilot sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = parser.parse(bytes);

        assertEquals(15, tables.size());
        assertEquals(1, tables.get(0).getSourceTableIndex());
        assertEquals(15, tables.get(14).getSourceTableIndex());
        assertFalse(tables.get(0).getTableTitle().isBlank());
        assertTrue(tables.stream().allMatch(table -> !table.getRows().isEmpty()));
        assertTrue(tables.stream().noneMatch(table -> "\u7269\u6599\u7f16\u7801".equals(table.getTableTitle())));
        assertIterableEquals(EXPECTED_TITLES,
                tables.stream().map(MesProBatchRecordParsedTable::getTableTitle).collect(Collectors.toList()));
        assertIterableEquals(EXPECTED_ROW_COUNTS,
                tables.stream().map(MesProBatchRecordParsedTable::getRowCount).collect(Collectors.toList()));
    }

    @Test
    void parsePilotSample_preservesComplexProcessVisualColumnGridFromWordCellBoundaries() throws Exception {
        Assumptions.assumeTrue(Files.exists(PILOT_SAMPLE), "pilot sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PILOT_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = parser.parse(bytes);

        MesProBatchRecordParsedTable cleanTable = tableByTitle(tables, "\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55");
        MesProBatchRecordParsedTable assemblyTable = tableByTitle(tables, "\u7ec4\u88c5\u2160\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55");

        assertEquals(10, cleanTable.getColumnCount(),
                "clean process table should use the target process segment grid instead of the full physical table grid");
        assertEquals(cleanTable.getColumnCount(), cleanTable.getColumnWidths().size());
        assertTrue(assemblyTable.getColumnCount() >= 60,
                "assembly process table should preserve Word visual grid columns instead of collapsing to logical cells");
        assertEquals(assemblyTable.getColumnCount(), assemblyTable.getColumnWidths().size());
        assertTrue(assemblyTable.getRows().stream()
                        .flatMap(List::stream)
                        .anyMatch(cell -> cell.getColSpan() > 20),
                "wide merged cells should span the visual grid rather than the small logical column count");
    }

    @Test
    void parsePressurePumpDoc_preservesEmptyCheckboxSymbolsInCleanProcessMaterialNames() throws Exception {
        Assumptions.assumeTrue(Files.exists(PRESSURE_PUMP_SAMPLE),
                "pressure pump sample doc fixture is not available on this machine");
        byte[] bytes = Files.readAllBytes(PRESSURE_PUMP_SAMPLE);

        List<MesProBatchRecordParsedTable> tables = parser.parse(bytes);

        MesProBatchRecordParsedTable cleanTable = tableByTitle(tables,
                "\u6e05\u6d01\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55");
        assertContainsText(cleanTable, "\u25a130atm\u538b\u529b\u8868");
        assertContainsText(cleanTable, "\u25a140atm\u538b\u529b\u8868");
    }

    @SuppressWarnings("unchecked")
    private List<MesProBatchRecordParsedTable> invokeSplitTemplates(MesProBatchRecordParsedTable parsedTable)
            throws Exception {
        Method method = MesProBatchRecordDocParser.class.getDeclaredMethod(
                "splitTemplates", MesProBatchRecordParsedTable.class);
        method.setAccessible(true);
        return (List<MesProBatchRecordParsedTable>) method.invoke(parser, parsedTable);
    }

    private static List<MesProBatchRecordParsedCell> row(String... texts) {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (String text : texts) {
            row.add(MesProBatchRecordParsedCell.builder()
                    .text(text)
                    .rowSpan(1)
                    .colSpan(1)
                    .bold(false)
                    .fontSize(10)
                    .horizontalAlign("center")
                    .verticalAlign("middle")
                    .widthPx(120)
                    .heightPx(24)
                    .build());
        }
        return row;
    }

    private static void assertContainsText(MesProBatchRecordParsedTable table, String expectedText) {
        assertContainsText(table.getRows(), expectedText);
    }

    private static void assertContainsText(List<List<MesProBatchRecordParsedCell>> rows, String expectedText) {
        boolean found = rows.stream()
                .flatMap(List::stream)
                .map(MesProBatchRecordParsedCell::getText)
                .filter(text -> text != null)
                .anyMatch(text -> text.contains(expectedText));
        assertTrue(found, "missing text: " + expectedText);
    }

    private static void assertNotContainsText(List<List<MesProBatchRecordParsedCell>> rows, String unexpectedText) {
        boolean found = rows.stream()
                .flatMap(List::stream)
                .map(MesProBatchRecordParsedCell::getText)
                .filter(text -> text != null)
                .anyMatch(text -> text.contains(unexpectedText));
        assertFalse(found, "unexpected text: " + unexpectedText);
    }

    private static int rowHeight(List<MesProBatchRecordParsedCell> row) {
        return row.stream()
                .mapToInt(MesProBatchRecordParsedCell::getHeightPx)
                .max()
                .orElse(0);
    }

    private static int rowIndexOfText(List<List<MesProBatchRecordParsedCell>> rows, String expectedText) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            for (MesProBatchRecordParsedCell cell : rows.get(rowIndex)) {
                String text = cell.getText();
                if (text != null && text.contains(expectedText)) {
                    return rowIndex;
                }
            }
        }
        return -1;
    }

    private static MesProBatchRecordParsedTable tableByTitle(List<MesProBatchRecordParsedTable> tables, String title) {
        return tables.stream()
                .filter(table -> title.equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing table: " + title));
    }

    private static byte[] buildDocxProcessInspectionSample() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(3, 3);
            XWPFTableRow titleRow = table.getRow(0);
            titleRow.getCell(0).setText("按压式球囊扩张压力泵组装过程检验记录");
            titleRow.getCell(0).getCTTc().addNewTcPr().addNewGridSpan().setVal(java.math.BigInteger.valueOf(3));
            titleRow.removeCell(2);
            titleRow.removeCell(1);
            table.getRow(1).getCell(0).setText("检验项目");
            table.getRow(1).getCell(1).setText("标准");
            table.getRow(1).getCell(2).setText("判定");
            table.getRow(2).getCell(0).setText("外观");
            table.getRow(2).getCell(1).setText("无明显缺陷");
            table.getRow(2).getCell(2).setText("合格");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildDocxWithDocumentFrameSample() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFTable headerTable = header.createTable(2, 3);
            headerTable.getRow(0).getCell(0).setText("文件标题");
            headerTable.getRow(0).getCell(1).setText("记录编号");
            headerTable.getRow(0).getCell(2).setText("DOC-001");
            headerTable.getRow(1).getCell(0).setText("版本");
            headerTable.getRow(1).getCell(1).setText("A/0");
            headerTable.getRow(1).getCell(2).setText("页码");
            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("生效日期：2026年02月02日");

            XWPFTable bodyTable = document.createTable(2, 2);
            bodyTable.getRow(0).getCell(0).setText("检验项目");
            bodyTable.getRow(0).getCell(1).setText("判定");
            bodyTable.getRow(1).getCell(0).setText("外观");
            bodyTable.getRow(1).getCell(1).setText("合格");

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildDocxWithDeclaredRowHeights() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(2, 1);
            table.getRow(0).setHeight(360);
            table.getRow(0).getCell(0).setText("标准高度行");
            table.getRow(1).setHeight(720);
            table.getRow(1).getCell(0).setText("较高明细行");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildDocxWithUnderlinedBlankRuns() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(1, 1);
            XWPFTableCell cell = table.getRow(0).getCell(0);
            cell.removeParagraph(0);
            XWPFParagraph paragraph = cell.addParagraph();
            paragraph.createRun().setText("合格数量：");
            underlinedBlankRun(paragraph, "        ");
            paragraph.createRun().setText("；不合格数量：");
            underlinedBlankRun(paragraph, "        ");
            paragraph.createRun().setText("；不合格评审报告编号（若有）：");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static void underlinedBlankRun(XWPFParagraph paragraph, String text) {
        XWPFRun run = paragraph.createRun();
        run.setUnderline(UnderlinePatterns.SINGLE);
        run.setText(text);
    }

    private static byte[] buildDocxDiagonalSlashSample() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("清洗功率");
            table.getRow(0).getCell(1).setText("清洗温度");
            table.getRow(1).getCell(0).setText("100%");
            CTTcBorders borders = table.getRow(1).getCell(1).getCTTc().addNewTcPr().addNewTcBorders();
            CTBorder diagonal = borders.addNewTl2Br();
            diagonal.setVal(STBorder.SINGLE);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
