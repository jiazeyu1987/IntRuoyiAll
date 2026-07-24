package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteERecognizerTest {

    private static final String SOURCE_DOC_FILE_NAME = "source.doc";

    private static final Path PILOT_SAMPLE = Path.of(
            "C:\\Users\\BJB110\\Desktop\\2\\2\\RE-PP-ID-01\uFF08A 1\uFF09\u7403\u56CA\u6269\u5F20\u538B\u529B\u6CF5\u751F\u4EA7\u8BB0\u5F55(1).doc");
    private static final Path LOSS_REPORT_SAMPLE = Path.of(
            "C:\\Users\\BJB110\\Desktop\\文档\\损耗单.doc");
    private static final List<String> EXPECTED_TITLES = List.of(
            "template-01",
            "template-02",
            "template-03",
            "template-04",
            "template-05",
            "template-06",
            "template-07",
            "template-08",
            "template-09",
            "template-10",
            "template-11",
            "template-12",
            "template-13",
            "template-14",
            "template-15"
    );

    @Test
    void recognize_whenSourceWordProfileMatches_usesProfileBeforeImageRoute() {
        RecordingImageParser imageParser = new RecordingImageParser();
        MesProBatchRecordParsedTable sourceTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("source-profile-table")
                .rowCount(1)
                .columnCount(1)
                .rows(List.of(List.of(MesProBatchRecordParsedCell.builder()
                        .text("PROFILE_SOURCE")
                        .build())))
                .build();
        MesProBatchRecordRouteERecognizer recognizer = new MesProBatchRecordRouteERecognizer(
                new StubDocParser(List.of(sourceTable)), imageParser,
                new MesProBatchRecordFormProfileRegistry(List.of(new StubSourceWordProfile())));

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                Path.of(SOURCE_DOC_FILE_NAME), new byte[]{1}, SOURCE_DOC_FILE_NAME);

        assertEquals(1, tables.size());
        assertEquals("profile-normalized", tables.get(0).getTableTitle());
        assertEquals("profile-fillable", tables.get(0).getRows().get(0).get(0).getText());
        assertTrue(tables.get(0).getRows().get(0).get(0).isFillable());
        assertTrue(imageParser.calls().isEmpty(), "profile-matched source Word tables must bypass image recognition");
    }

    @Test
    void recognize_batchesFifteenTemplatesThroughImageParser() {
        RecordingImageParser imageParser = new RecordingImageParser();
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new StubDocParser(createSourceTables(15, 12, 6)), imageParser);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                Path.of(SOURCE_DOC_FILE_NAME), new byte[]{1}, SOURCE_DOC_FILE_NAME);

        assertEquals(EXPECTED_TITLES.size(), tables.size());
        assertEquals(3, imageParser.calls().size());
        assertEquals(EXPECTED_TITLES, tables.stream().map(MesProBatchRecordParsedTable::getTableTitle).toList());
        assertEquals(1, tables.get(0).getSourceTableIndex());
        assertEquals(15, tables.get(14).getSourceTableIndex());
        assertTrue(imageParser.calls().stream().allMatch(call -> call.fileName().endsWith(".png")));
        assertTrue(imageParser.calls().stream().allMatch(call -> call.bytes().length > 8));
        assertTrue(imageParser.calls().stream().allMatch(call -> isPng(call.bytes())));
    }

    @Test
    void recognize_whenImageParserBatchCountDoesNotMatch_failFast() throws Exception {
        MesProBatchRecordImageParser imageParser = new MismatchBatchImageParser();
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new StubDocParser(createSourceTables(15, 12, 6)), imageParser);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recognizer.recognize(Path.of(SOURCE_DOC_FILE_NAME), new byte[]{1}, SOURCE_DOC_FILE_NAME));

        assertEquals(PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID.getCode(), exception.getCode());
    }

    @Test
    void recognize_whenWordBytesAreEmpty_failFast() {
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new MesProBatchRecordDocParser(), (originalFileName, imageBytes) -> List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recognizer.recognize(PILOT_SAMPLE, new byte[0], SOURCE_DOC_FILE_NAME));

        assertEquals(PRO_BATCH_RECORD_REPORT_PARSE_FAILED.getCode(), exception.getCode());
    }

    @Test
    void recognize_whenImageParserCollapsesSourceStructure_failFast() {
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new StubDocParser(createSourceTables(15, 12, 6)),
                        new CollapsedStructureImageParser());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recognizer.recognize(PILOT_SAMPLE, new byte[]{1}, "route-e-structure.doc"));

        assertEquals(PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID.getCode(), exception.getCode());
    }

    @Test
    void recognize_whenImageParserMergesLossReportEntryCells_restoresSourceFillableBlanks() {
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new StubDocParser(List.of(createLossReportSourceTable())),
                        new LossReportMergedEntryImageParser());

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                Path.of(SOURCE_DOC_FILE_NAME), new byte[]{1}, SOURCE_DOC_FILE_NAME);

        List<MesProBatchRecordParsedCell> metadataRow = tables.get(0).getRows().get(0);
        assertEquals(8, metadataRow.size());
        assertEquals("", metadataRow.get(1).getText());
        assertEquals("", metadataRow.get(3).getText());
        assertEquals("", metadataRow.get(5).getText());
        assertEquals("", metadataRow.get(7).getText());
    }

    @Test
    void recognize_whenSourceWordIsLossReport_usesSourceStructureWithoutImageParser() {
        FailingImageParser imageParser = new FailingImageParser();
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new StubDocParser(List.of(createLossReportSourceTable())), imageParser);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                Path.of(SOURCE_DOC_FILE_NAME), new byte[]{1}, "8.3-09（E 1）生产过程损耗报告单.doc");

        assertEquals(1, tables.size());
        assertEquals(0, imageParser.callCount());
        List<MesProBatchRecordParsedCell> metadataRow = tables.get(0).getRows().get(0);
        assertEquals(8, metadataRow.size());
        assertEquals("产品名称", metadataRow.get(0).getText());
        assertEquals("", metadataRow.get(1).getText());
        assertEquals("型号规格", metadataRow.get(2).getText());
        assertEquals("", metadataRow.get(3).getText());
        assertEquals("批号", metadataRow.get(4).getText());
        assertEquals("", metadataRow.get(5).getText());
        assertEquals("生产数量", metadataRow.get(6).getText());
        assertEquals("", metadataRow.get(7).getText());
    }

    @Test
    void recognize_whenSourceWordIsLossReport_preservesHorizontalDetailTableLayout() {
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new StubDocParser(List.of(createLossReportSourceTable())),
                        new FailingImageParser());

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                Path.of(SOURCE_DOC_FILE_NAME), new byte[]{1}, "8.3-09（E 1）生产过程损耗报告单.doc");

        MesProBatchRecordParsedTable table = tables.get(0);
        assertLossReportHorizontalLayout(table);
        long fillableCount = table.getRows().stream()
                .flatMap(List::stream)
                .filter(MesProBatchRecordParsedCell::isFillable)
                .count();
        assertEquals(47, fillableCount,
                "top fields, eight horizontal detail rows, merged personnel fields and approver must be fillable");

        JSONObject root = JSON.parseObject(new MesProBatchRecordReportJsonBuilder().build(table, "EBR_TEST_LOSS"));
        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);
        assertEquals(63, suggestions.size(), "horizontal loss report fields must become JSON fillForm suggestions");
        assertTrue(suggestions.stream().anyMatch(rule -> "不合格日期".equals(rule.getLabel())));
        assertTrue(suggestions.stream().anyMatch(rule -> "不合格原因".equals(rule.getLabel())));
        assertEquals(8, suggestions.stream()
                .filter(rule -> "报废".equals(rule.getLabel()) && "BOOLEAN".equals(rule.getValueType())).count());
        assertTrue(suggestions.stream()
                .filter(rule -> "报废".equals(rule.getLabel()) && "BOOLEAN".equals(rule.getValueType()))
                .allMatch(rule -> "checkbox".equals(rule.getComponentFlag())));
        assertEquals(8, suggestions.stream()
                .filter(rule -> "其他：".equals(rule.getLabel()) && "BOOLEAN".equals(rule.getValueType())).count());
        assertTrue(suggestions.stream()
                .filter(rule -> "其他：".equals(rule.getLabel()) && "BOOLEAN".equals(rule.getValueType()))
                .allMatch(rule -> "checkbox".equals(rule.getComponentFlag())));
        assertTrue(suggestions.stream().anyMatch(rule -> rule.getLabel() != null
                && rule.getLabel().startsWith("批准人/日期")));
    }

    @Test
    void recognize_whenRealLossReportDoc_expandsMergedBodyFillableFields() throws Exception {
        Assumptions.assumeTrue(Files.exists(LOSS_REPORT_SAMPLE), "real loss report sample is required");
        FailingImageParser imageParser = new FailingImageParser();
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new MesProBatchRecordDocParser(), imageParser);

        List<MesProBatchRecordParsedTable> tables = recognizer.recognize(
                LOSS_REPORT_SAMPLE, Files.readAllBytes(LOSS_REPORT_SAMPLE), LOSS_REPORT_SAMPLE.getFileName().toString());

        assertEquals(1, tables.size());
        assertEquals(0, imageParser.callCount());
        MesProBatchRecordParsedTable table = tables.get(0);
        assertLossReportHorizontalLayout(table);
        JSONObject root = JSON.parseObject(new MesProBatchRecordReportJsonBuilder().build(table, "EBR_REAL_LOSS"));
        List<BatchRecordReportCellRuleVO> suggestions = MesProBatchRecordCellRuleSupport.buildSuggestions(root);
        assertEquals(63, suggestions.size(), "real loss report JSON must expose every horizontal fillable field");
        assertTrue(suggestions.stream().anyMatch(rule -> "产品名称".equals(rule.getLabel())));
        assertTrue(suggestions.stream().anyMatch(rule -> "不合格日期".equals(rule.getLabel())));
        assertTrue(suggestions.stream().anyMatch(rule -> "工序名称".equals(rule.getLabel())));
        assertTrue(suggestions.stream().anyMatch(rule -> "不合格数量".equals(rule.getLabel())));
        assertTrue(suggestions.stream().anyMatch(rule -> "不合格原因".equals(rule.getLabel())));
        assertEquals(8, suggestions.stream()
                .filter(rule -> "报废".equals(rule.getLabel()) && "BOOLEAN".equals(rule.getValueType())).count());
        assertTrue(suggestions.stream()
                .filter(rule -> "报废".equals(rule.getLabel()) && "BOOLEAN".equals(rule.getValueType()))
                .allMatch(rule -> "checkbox".equals(rule.getComponentFlag())));
        assertEquals(8, suggestions.stream()
                .filter(rule -> "其他：".equals(rule.getLabel()) && "BOOLEAN".equals(rule.getValueType())).count());
        assertTrue(suggestions.stream()
                .filter(rule -> "其他：".equals(rule.getLabel()) && "BOOLEAN".equals(rule.getValueType()))
                .allMatch(rule -> "checkbox".equals(rule.getComponentFlag())));
        assertTrue(suggestions.stream().anyMatch(rule -> rule.getLabel() != null
                && rule.getLabel().startsWith("批准人/日期")));
    }

    private static void assertLossReportHorizontalLayout(MesProBatchRecordParsedTable table) {
        assertEquals(12, table.getRows().size(),
                "loss report must keep top row, description row, horizontal header, eight detail rows and approval");
        assertEquals("损耗描述：", table.getRows().get(1).get(0).getText());
        assertEquals(9, table.getRows().get(1).get(0).getColSpan());
        assertEquals(List.of("不合格日期", "工序名称", "不合格数量", "不合格原因", "处置方式", "生产人员/日期", "检验人员\n确认/日期"),
                table.getRows().get(2).stream().map(MesProBatchRecordParsedCell::getText).toList());
        assertEquals(3, table.getRows().get(2).get(4).getColSpan());

        List<MesProBatchRecordParsedCell> firstDetailRow = table.getRows().get(3);
        assertEquals(9, firstDetailRow.size());
        assertEquals("", firstDetailRow.get(0).getText());
        assertTrue(firstDetailRow.get(0).isFillable());
        assertEquals("", firstDetailRow.get(3).getText());
        assertTrue(firstDetailRow.get(3).isFillable());
        assertEquals("□报废", firstDetailRow.get(4).getText());
        assertFalse(firstDetailRow.get(4).isFillable());
        assertEquals(1, firstDetailRow.get(4).getColSpan());
        assertEquals("□其他：", firstDetailRow.get(5).getText());
        assertFalse(firstDetailRow.get(5).isFillable());
        assertEquals(1, firstDetailRow.get(5).getColSpan());
        assertEquals("", firstDetailRow.get(6).getText());
        assertTrue(firstDetailRow.get(6).isFillable());
        assertEquals(1, firstDetailRow.get(6).getColSpan());
        assertEquals(8, firstDetailRow.get(7).getRowSpan());
        assertTrue(firstDetailRow.get(7).isFillable());
        assertEquals(8, firstDetailRow.get(8).getRowSpan());
        assertTrue(firstDetailRow.get(8).isFillable());

        List<MesProBatchRecordParsedCell> secondDetailRow = table.getRows().get(4);
        assertEquals(7, secondDetailRow.size(),
                "merged personnel columns must only appear on the first detail row");
        assertEquals("□报废", secondDetailRow.get(4).getText());
        assertFalse(secondDetailRow.get(4).isFillable());
        assertEquals("□其他：", secondDetailRow.get(5).getText());
        assertFalse(secondDetailRow.get(5).isFillable());
        assertEquals("", secondDetailRow.get(6).getText());
        assertTrue(secondDetailRow.get(6).isFillable());

        assertEquals("批准人/日期：", table.getRows().get(11).get(0).getText());
        assertTrue(table.getRows().get(11).get(1).isFillable());
    }

    @Test
    void renderTemplatePng_whenTemplateHasManyRows_preservesSourceRowHeight() throws Exception {
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new MesProBatchRecordDocParser(), (originalFileName, imageBytes) -> List.of());

        byte[] pngBytes = recognizer.renderTemplatePng(createLargeSourceTable(), 1);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));

        assertTrue(isPng(pngBytes));
        assertTrue(image.getHeight() >= 380,
                () -> "expected route-E image to preserve row height, actual=" + image.getHeight());
    }

    @Test
    void renderTemplatePng_whenTemplateHasMultipleColumns_preservesSourceColumnWidth() throws Exception {
        MesProBatchRecordRouteERecognizer recognizer =
                recognizer(new MesProBatchRecordDocParser(), (originalFileName, imageBytes) -> List.of());

        byte[] pngBytes = recognizer.renderTemplatePng(createWideSourceTable(), 1);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));

        assertTrue(isPng(pngBytes));
        assertTrue(image.getWidth() >= 900,
                () -> "expected route-E image to preserve multi-column width, actual=" + image.getWidth());
    }

    private static boolean isPng(byte[] bytes) {
        return bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private static MesProBatchRecordRouteERecognizer recognizer(MesProBatchRecordDocParser docParser,
                                                                MesProBatchRecordImageParser imageParser) {
        return new MesProBatchRecordRouteERecognizer(docParser, imageParser,
                new MesProBatchRecordFormProfileRegistry(List.of(new MesProBatchRecordLossReportNormalizer())));
    }

    private static final class StubSourceWordProfile implements MesProBatchRecordFormProfile {

        @Override
        public String formSlotType() {
            return "STUB_PROFILE";
        }

        @Override
        public boolean supportsSourceTable(MesProBatchRecordParsedTable table) {
            return table != null && table.getRows() != null && table.getRows().stream()
                    .flatMap(List::stream)
                    .anyMatch(cell -> "PROFILE_SOURCE".equals(cell.getText()));
        }

        @Override
        public MesProBatchRecordParsedTable normalizeSourceTable(int templateIndex,
                                                                 MesProBatchRecordParsedTable sourceTable) {
            return MesProBatchRecordParsedTable.builder()
                    .sourceTableIndex(templateIndex)
                    .tableTitle("profile-normalized")
                    .rowCount(1)
                    .columnCount(1)
                    .rows(List.of(List.of(MesProBatchRecordParsedCell.builder()
                            .text("profile-fillable")
                            .fillable(true)
                            .build())))
                    .build();
        }
    }

    private static MesProBatchRecordParsedTable createLargeSourceTable() {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            rows.add(List.of(MesProBatchRecordParsedCell.builder()
                    .text("line " + (index + 1) + " summary content with a long process note repeated repeated repeated")
                    .rowSpan(1)
                    .colSpan(1)
                    .build()));
        }
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("large-template")
                .rowCount(rows.size())
                .columnCount(1)
                .rows(rows)
                .build();
    }

    private static MesProBatchRecordParsedTable createWideSourceTable() {
        List<MesProBatchRecordParsedCell> row = List.of(
                MesProBatchRecordParsedCell.builder().text("col-1").widthPx(220).heightPx(36).build(),
                MesProBatchRecordParsedCell.builder().text("col-2").widthPx(220).heightPx(36).build(),
                MesProBatchRecordParsedCell.builder().text("col-3").widthPx(220).heightPx(36).build(),
                MesProBatchRecordParsedCell.builder().text("col-4").widthPx(220).heightPx(36).build()
        );
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("wide-template")
                .rowCount(1)
                .columnCount(4)
                .rows(List.of(row))
                .build();
    }

    private static MesProBatchRecordParsedTable createLossReportSourceTable() {
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("loss-report")
                .rowCount(3)
                .columnCount(8)
                .rows(List.of(
                        List.of(
                                lossReportCell("产品名称"), lossReportCell(""),
                                lossReportCell("型号规格"), lossReportCell(""),
                                lossReportCell("批号"), lossReportCell(""),
                                lossReportCell("生产数量"), lossReportCell("")),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("""
                                        损耗描述：
                                        不合格日期
                                        工序名称
                                        不合格数量
                                        不合格原因
                                        处置方式
                                        生产人员/日期
                                        检验人员
                                        确认/日期

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________""")
                                .colSpan(8)
                                .rowSpan(1)
                                .widthPx(960)
                                .heightPx(220)
                                .build()),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("批准人/日期：")
                                .colSpan(8)
                                .rowSpan(1)
                                .widthPx(960)
                                .heightPx(48)
                                .build())
                ))
                .build();
    }

    private static MesProBatchRecordParsedCell lossReportCell(String text) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(1)
                .colSpan(1)
                .widthPx(120)
                .heightPx(29)
                .build();
    }

    private static List<MesProBatchRecordParsedTable> createSourceTables(int tableCount, int rowCount, int columnCount) {
        List<MesProBatchRecordParsedTable> tables = new ArrayList<>();
        for (int tableIndex = 0; tableIndex < tableCount; tableIndex++) {
            tables.add(createStructuredTable(tableIndex + 1, "source-" + (tableIndex + 1), rowCount, columnCount));
        }
        return tables;
    }

    private static MesProBatchRecordParsedTable createStructuredTable(int sourceTableIndex, String title, int rowCount,
                                                                      int columnCount) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<MesProBatchRecordParsedCell> cells = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                cells.add(MesProBatchRecordParsedCell.builder()
                        .text(title + "-r" + rowIndex + "-c" + columnIndex)
                        .colSpan(1)
                        .rowSpan(1)
                        .widthPx(180)
                        .heightPx(32)
                        .build());
            }
            rows.add(cells);
        }
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(sourceTableIndex)
                .tableTitle(title)
                .rowCount(rows.size())
                .columnCount(columnCount)
                .rows(rows)
                .build();
    }

    private static final class RecordingImageParser implements MesProBatchRecordImageParser {

        private final List<ImageCall> calls = new ArrayList<>();

        @Override
        public List<MesProBatchRecordParsedTable> parse(String originalFileName, byte[] bytes) {
            int batchIndex = calls.size();
            calls.add(new ImageCall(originalFileName, bytes));
            int startIndex = batchIndex * 5;
            List<MesProBatchRecordParsedTable> tables = new ArrayList<>();
            for (int index = startIndex; index < Math.min(startIndex + 5, EXPECTED_TITLES.size()); index++) {
                tables.add(createStructuredTable(index + 1, EXPECTED_TITLES.get(index), 12, 6));
            }
            return tables;
        }

        List<ImageCall> calls() {
            return calls;
        }
    }

    private static final class MismatchBatchImageParser implements MesProBatchRecordImageParser {

        private int callCount;

        @Override
        public List<MesProBatchRecordParsedTable> parse(String originalFileName, byte[] bytes) {
            callCount++;
            int returnedCount = callCount == 1 ? 0 : 1;
            List<MesProBatchRecordParsedTable> tables = new ArrayList<>();
            for (int index = 0; index < returnedCount; index++) {
                tables.add(TestBatchRecordFixtures.parsedTable(index + 1, "batch-" + callCount + "-" + index));
            }
            return tables;
        }
    }

    private static final class StubDocParser extends MesProBatchRecordDocParser {

        private final List<MesProBatchRecordParsedTable> tables;

        private StubDocParser(List<MesProBatchRecordParsedTable> tables) {
            this.tables = tables;
        }

        @Override
        public List<MesProBatchRecordParsedTable> parse(byte[] bytes) {
            return tables;
        }
    }

    private static final class CollapsedStructureImageParser implements MesProBatchRecordImageParser {

        private int callCount;

        @Override
        public List<MesProBatchRecordParsedTable> parse(String originalFileName, byte[] bytes) {
            callCount++;
            List<MesProBatchRecordParsedTable> tables = new ArrayList<>();
            int startIndex = (callCount - 1) * 5;
            for (int index = 0; index < 5; index++) {
                int tableIndex = startIndex + index + 1;
                tables.add(createStructuredTable(tableIndex, "collapsed-" + tableIndex, 4, 3));
            }
            return tables;
        }
    }

    private static final class LossReportMergedEntryImageParser implements MesProBatchRecordImageParser {

        @Override
        public List<MesProBatchRecordParsedTable> parse(String originalFileName, byte[] bytes) {
            return List.of(MesProBatchRecordParsedTable.builder()
                    .sourceTableIndex(1)
                    .tableTitle("loss-report")
                    .rowCount(3)
                    .columnCount(8)
                    .rows(List.of(
                            List.of(
                                    mergedLossReportCell("产品名称"),
                                    mergedLossReportCell("型号规格"),
                                    mergedLossReportCell("批号"),
                                    mergedLossReportCell("生产数量")),
                            List.of(MesProBatchRecordParsedCell.builder()
                                    .text("损耗描述：")
                                    .colSpan(8)
                                    .rowSpan(1)
                                    .widthPx(960)
                                    .heightPx(220)
                                    .build()),
                            List.of(MesProBatchRecordParsedCell.builder()
                                    .text("批准人/日期：")
                                    .colSpan(8)
                                    .rowSpan(1)
                                    .widthPx(960)
                                    .heightPx(48)
                                    .build())
                    ))
                    .build());
        }

        private MesProBatchRecordParsedCell mergedLossReportCell(String text) {
            return MesProBatchRecordParsedCell.builder()
                    .text(text)
                    .rowSpan(1)
                    .colSpan(2)
                    .widthPx(240)
                    .heightPx(29)
                    .build();
        }
    }

    private static final class FailingImageParser implements MesProBatchRecordImageParser {

        private int callCount;

        @Override
        public List<MesProBatchRecordParsedTable> parse(String originalFileName, byte[] bytes) {
            callCount++;
            throw new AssertionError("loss report should be recognized from source Word table");
        }

        private int callCount() {
            return callCount;
        }
    }

    private record ImageCall(String fileName, byte[] bytes) {
    }
}
