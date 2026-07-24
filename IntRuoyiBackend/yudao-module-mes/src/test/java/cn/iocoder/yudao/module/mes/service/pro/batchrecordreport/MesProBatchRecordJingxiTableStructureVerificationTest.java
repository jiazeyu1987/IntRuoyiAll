package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordJingxiTableStructureVerificationTest {

    private static final Path REAL_DOC = Path.of(
            "C:\\Users\\BJB110\\Desktop\\2\\2\\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc");
    private static final String TABLE_TITLE = "精洗工序生产记录";
    private static final String ROUGH_WASH_TABLE_TITLE = "粗洗工序生产记录";
    private static final String WASH_TABLE_TITLE = "清洗工序生产记录";
    private static final String CLEAN_TABLE_TITLE = "清洁工序生产记录";
    private static final String ASSEMBLY_ONE_TABLE_TITLE = "组装Ⅰ工序生产记录";
    private static final String LIGHT_CURE_ONE_TABLE_TITLE = "光固Ⅰ工序生产记录";
    private static final String SILICONIZE_ONE_TABLE_TITLE = "硅化Ⅰ工序生产记录";
    private static final String SILICONIZE_TWO_TABLE_TITLE = "硅化Ⅱ工序生产记录";
    private static final String ASSEMBLY_TWO_TABLE_TITLE = "组装Ⅱ工序生产记录";
    private static final String INSPECTION_TABLE_TITLE = "检测工序生产记录";
    private static final String LIGHT_CURE_TWO_TABLE_TITLE = "光固Ⅱ工序生产记录";
    private static final String SINGLE_PACK_TABLE_TITLE = "单包装工序生产记录";
    private static final String MIDDLE_PACK_TABLE_TITLE = "中包装工序生产记录";
    private static final String OUTER_PACK_TABLE_TITLE = "大包装工序生产记录";
    private static final List<String> PROCESS_TABLE_TITLES = List.of(
            ROUGH_WASH_TABLE_TITLE,
            TABLE_TITLE,
            WASH_TABLE_TITLE,
            CLEAN_TABLE_TITLE,
            ASSEMBLY_ONE_TABLE_TITLE,
            LIGHT_CURE_ONE_TABLE_TITLE,
            SILICONIZE_ONE_TABLE_TITLE,
            SILICONIZE_TWO_TABLE_TITLE,
            ASSEMBLY_TWO_TABLE_TITLE,
            INSPECTION_TABLE_TITLE,
            LIGHT_CURE_TWO_TABLE_TITLE,
            SINGLE_PACK_TABLE_TITLE,
            MIDDLE_PACK_TABLE_TITLE,
            OUTER_PACK_TABLE_TITLE
    );
    private static final String REPORT_CODE = "EBR_JINGXI_VERIFY";

    private static byte[] cachedRealDocBytes;
    private static List<MesProBatchRecordParsedTable> cachedRouteATables;
    private static List<MesProBatchRecordParsedTable> cachedRouteBTables;
    private static final Map<String, WordTableShape> CACHED_WORD_SHAPES = new HashMap<>();

    private final MesProBatchRecordDocParser parser = new MesProBatchRecordDocParser();
    private final MesProBatchRecordReportLayoutCalibrator calibrator = new MesProBatchRecordReportLayoutCalibrator();
    private final MesProBatchRecordReportJsonBuilder builder = new MesProBatchRecordReportJsonBuilder();

    @Test
    void routeAJson_shouldMatchOriginalJingxiWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalRoughWashWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(ROUGH_WASH_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalWashWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(WASH_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalCleanWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(CLEAN_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalAssemblyOneWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(ASSEMBLY_ONE_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalLightCureOneWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(LIGHT_CURE_ONE_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalSiliconizeOneWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(SILICONIZE_ONE_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalSiliconizeTwoWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(SILICONIZE_TWO_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalAssemblyTwoWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(ASSEMBLY_TWO_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalInspectionWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(INSPECTION_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalLightCureTwoWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(LIGHT_CURE_TWO_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalSinglePackWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(SINGLE_PACK_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalMiddlePackWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(MIDDLE_PACK_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchOriginalOuterPackWordTableStructure() throws Exception {
        assertRouteAJsonShouldMatchOriginalWordTableStructure(OUTER_PACK_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalJingxiWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalRoughWashWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(ROUGH_WASH_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalWashWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(WASH_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalCleanWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(CLEAN_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalAssemblyOneWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(ASSEMBLY_ONE_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalLightCureOneWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(LIGHT_CURE_ONE_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalSiliconizeOneWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(SILICONIZE_ONE_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalSiliconizeTwoWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(SILICONIZE_TWO_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalAssemblyTwoWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(ASSEMBLY_TWO_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalInspectionWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(INSPECTION_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalLightCureTwoWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(LIGHT_CURE_TWO_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalSinglePackWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(SINGLE_PACK_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalMiddlePackWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(MIDDLE_PACK_TABLE_TITLE);
    }

    @Test
    void routeBJson_shouldMatchOriginalOuterPackWordTableStructure() throws Exception {
        assertRouteBJsonShouldMatchOriginalWordTableStructure(OUTER_PACK_TABLE_TITLE);
    }

    @Test
    void routeAJson_shouldMatchFullWordProcessVisualGroundTruthForEveryRenderedTable() throws Exception {
        assertRouteJsonShouldMatchFullWordProcessVisualGroundTruth(
                "routeA", routeATables(realDocBytes()));
    }

    @Test
    void routeBJson_shouldMatchFullWordProcessVisualGroundTruthForEveryRenderedTable() throws Exception {
        assertRouteJsonShouldMatchFullWordProcessVisualGroundTruth(
                "routeB", routeBTables(realDocBytes()));
    }

    @Test
    void routeBPackedMaterialMatrix_shouldRenderAsGridInsteadOfCollapsedWideCell() throws Exception {
        byte[] bytes = realDocBytes();
        for (String tableTitle : List.of(
                ASSEMBLY_ONE_TABLE_TITLE,
                LIGHT_CURE_ONE_TABLE_TITLE,
                SILICONIZE_ONE_TABLE_TITLE,
                SINGLE_PACK_TABLE_TITLE,
                MIDDLE_PACK_TABLE_TITLE,
                OUTER_PACK_TABLE_TITLE)) {
            MesProBatchRecordParsedTable parsed = routeBTables(bytes).stream()
                    .filter(table -> tableTitle.equals(table.getTableTitle()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("recognized table not found: " + tableTitle));
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsed);

            assertTrue(hasExpandedTopMaterialMatrix(calibrated),
                    () -> tableTitle + " top material matrix should be expanded into visible grid rows, actual="
                            + describeParsedRows(calibrated.getRows()));
            assertTrue(!hasCollapsedPackedMaterialMatrixCell(calibrated),
                    () -> tableTitle + " top material matrix collapsed into a wide text cell, actual="
                            + describeParsedRows(calibrated.getRows()));
        }
    }

    @Test
    void routeBPackedMaterialMatrixSideHeader_shouldStartAtMaterialMatrixRow() throws Exception {
        byte[] bytes = realDocBytes();
        for (String tableTitle : List.of(
                ASSEMBLY_ONE_TABLE_TITLE,
                LIGHT_CURE_ONE_TABLE_TITLE,
                SILICONIZE_ONE_TABLE_TITLE,
                SINGLE_PACK_TABLE_TITLE,
                MIDDLE_PACK_TABLE_TITLE,
                OUTER_PACK_TABLE_TITLE)) {
            MesProBatchRecordParsedTable parsed = routeBTables(bytes).stream()
                    .filter(table -> tableTitle.equals(table.getTableTitle()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("recognized table not found: " + tableTitle));
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsed);
            JSONObject actualJson = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

            assertTrue(topMaterialMatrixRowHasProcessSideHeader(calibrated),
                    () -> tableTitle + " top material matrix should be covered by the operation vertical side header, actual="
                            + describeParsedRows(calibrated.getRows()));
            assertTrue(topMaterialMatrixSideHeaderStartsAtVisualColumnZero(calibrated),
                    () -> tableTitle + " operation vertical side header should start at visual column 0 on the material matrix row, actual="
                            + describeVisualPlacement(calibrated));
            assertTrue(jsonTopMaterialMatrixSideHeaderStartsAtColumnZero(actualJson),
                    () -> tableTitle + " final report JSON should keep the operation side header at column 0, actual="
                            + describeJsonMaterialMatrixPlacement(actualJson));
            assertTrue(jsonLeftSectionColumnHasVisibleWidth(actualJson),
                    () -> tableTitle + " final report JSON should keep the left vertical section column visible, actual="
                            + describeJsonColumnWidths(actualJson));
            assertTrue(!topMaterialMatrixRowHasPreviousChecklistSideHeader(calibrated),
                    () -> tableTitle + " top material matrix was swallowed by the previous checklist vertical side header, actual="
                            + describeParsedRows(calibrated.getRows()));
        }
    }

    @Test
    void routeAPackedMaterialMatrixSideHeader_shouldStartAtMaterialMatrixRow() throws Exception {
        byte[] bytes = realDocBytes();
        for (String tableTitle : List.of(
                ASSEMBLY_ONE_TABLE_TITLE,
                LIGHT_CURE_ONE_TABLE_TITLE,
                SILICONIZE_ONE_TABLE_TITLE,
                SINGLE_PACK_TABLE_TITLE,
                MIDDLE_PACK_TABLE_TITLE,
                OUTER_PACK_TABLE_TITLE)) {
            MesProBatchRecordParsedTable parsed = routeATables(bytes).stream()
                    .filter(table -> tableTitle.equals(table.getTableTitle()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("recognized table not found: " + tableTitle));
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsed);
            JSONObject actualJson = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

            assertTrue(topMaterialMatrixRowHasProcessSideHeader(calibrated),
                    () -> tableTitle + " top material matrix should be covered by the operation vertical side header, actual="
                            + describeParsedRows(calibrated.getRows()));
            assertTrue(topMaterialMatrixSideHeaderStartsAtVisualColumnZero(calibrated),
                    () -> tableTitle + " operation vertical side header should start at visual column 0 on the material matrix row, actual="
                            + describeVisualPlacement(calibrated));
            assertTrue(jsonTopMaterialMatrixSideHeaderStartsAtColumnZero(actualJson),
                    () -> tableTitle + " final report JSON should keep the operation side header at column 0, actual="
                            + describeJsonMaterialMatrixPlacement(actualJson));
            assertTrue(jsonLeftSectionColumnHasVisibleWidth(actualJson),
                    () -> tableTitle + " final report JSON should keep the left vertical section column visible, actual="
                            + describeJsonColumnWidths(actualJson));
            assertTrue(!topMaterialMatrixRowHasPreviousChecklistSideHeader(calibrated),
                    () -> tableTitle + " top material matrix was swallowed by the previous checklist vertical side header, actual="
                            + describeParsedRows(calibrated.getRows()));
        }
    }

    @Test
    void routeBOperationRecordTail_shouldNotExposeSourceMicroGridColumns() throws Exception {
        byte[] bytes = realDocBytes();
        for (String tableTitle : List.of(
                ASSEMBLY_ONE_TABLE_TITLE,
                LIGHT_CURE_ONE_TABLE_TITLE,
                SILICONIZE_ONE_TABLE_TITLE,
                ASSEMBLY_TWO_TABLE_TITLE,
                INSPECTION_TABLE_TITLE,
                LIGHT_CURE_TWO_TABLE_TITLE,
                SINGLE_PACK_TABLE_TITLE,
                MIDDLE_PACK_TABLE_TITLE,
                OUTER_PACK_TABLE_TITLE)) {
            MesProBatchRecordParsedTable parsed = routeBTables(bytes).stream()
                    .filter(table -> tableTitle.equals(table.getTableTitle()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("recognized table not found: " + tableTitle));
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsed);
            JSONObject actualJson = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

            assertTrue(jsonOperationRecordTailHasStableVisualColumns(actualJson),
                    () -> tableTitle + " operation record tail should render semantic record columns instead of source micro-grid columns, actual="
                            + describeOperationRecordTail(actualJson));
        }
    }

    @Test
    void routeAOperationRecordTail_shouldNotExposeSourceMicroGridColumns() throws Exception {
        byte[] bytes = realDocBytes();
        for (String tableTitle : List.of(
                ASSEMBLY_ONE_TABLE_TITLE,
                LIGHT_CURE_ONE_TABLE_TITLE,
                SILICONIZE_ONE_TABLE_TITLE,
                ASSEMBLY_TWO_TABLE_TITLE,
                INSPECTION_TABLE_TITLE,
                LIGHT_CURE_TWO_TABLE_TITLE,
                SINGLE_PACK_TABLE_TITLE,
                MIDDLE_PACK_TABLE_TITLE,
                OUTER_PACK_TABLE_TITLE)) {
            MesProBatchRecordParsedTable parsed = routeATables(bytes).stream()
                    .filter(table -> tableTitle.equals(table.getTableTitle()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("recognized table not found: " + tableTitle));
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsed);
            JSONObject actualJson = JSON.parseObject(builder.build(calibrated, REPORT_CODE));

            assertTrue(jsonOperationRecordTailHasStableVisualColumns(actualJson),
                    () -> tableTitle + " operation record tail should render semantic record columns instead of source micro-grid columns, actual="
                            + describeOperationRecordTail(actualJson));
        }
    }

    private void assertRouteAJsonShouldMatchOriginalWordTableStructure(String tableTitle) throws Exception {
        byte[] bytes = realDocBytes();

        WordTableShape expected = cachedWordTableShape(bytes, tableTitle);
        MesProBatchRecordParsedTable parsed = routeATables(bytes).stream()
                .filter(table -> tableTitle.equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("recognized table not found: " + tableTitle));
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsed);
        JSONObject actualJson = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        JsonTableShape actual = extractJsonTableShape(actualJson).segmentFrom(tableTitle, expected.rowCount());

        List<String> diffs = diff(expected, actual);

        assertTrue(diffs.isEmpty(), () -> tableTitle + " structure mismatch:\n"
                + String.join("\n", diffs)
                + "\nexpected=" + expected.describe()
                + "\nactual=" + actual.describe());
    }

    private void assertRouteBJsonShouldMatchOriginalWordTableStructure(String tableTitle) throws Exception {
        byte[] bytes = realDocBytes();

        WordTableShape expected = cachedWordTableShape(bytes, tableTitle);
        MesProBatchRecordParsedTable parsed = routeBTables(bytes).stream()
                .filter(table -> tableTitle.equals(table.getTableTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("recognized table not found: " + tableTitle));
        MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsed);
        JSONObject actualJson = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
        JsonTableShape actual = extractJsonTableShape(actualJson).segmentFrom(tableTitle, expected.rowCount());

        List<String> diffs = diff(expected, actual);

        assertTrue(diffs.isEmpty(), () -> tableTitle + " routeB structure mismatch:\n"
                + String.join("\n", diffs)
                + "\nexpected=" + expected.describe()
                + "\nactual=" + actual.describe());
    }

    private void assertRouteJsonShouldMatchFullWordProcessVisualGroundTruth(
            String routeName, List<MesProBatchRecordParsedTable> parsedTables) throws Exception {
        byte[] bytes = realDocBytes();
        Map<String, MesProBatchRecordParsedTable> tableByTitle = new HashMap<>();
        for (MesProBatchRecordParsedTable table : parsedTables) {
            tableByTitle.put(table.getTableTitle(), table);
        }

        List<String> allDiffs = new ArrayList<>();
        List<String> matchedTitles = new ArrayList<>();
        for (String tableTitle : PROCESS_TABLE_TITLES) {
            MesProBatchRecordParsedTable parsed = tableByTitle.get(tableTitle);
            if (parsed == null) {
                allDiffs.add(routeName + " missing rendered table: " + tableTitle);
                continue;
            }
            WordTableShape expected = cachedWordTableShape(bytes, tableTitle);
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(parsed);
            JSONObject actualJson = JSON.parseObject(builder.build(calibrated, REPORT_CODE));
            JsonTableShape actual = extractJsonTableShape(actualJson).segmentFrom(tableTitle, expected.rowCount());
            List<String> diffs = diff(expected, actual);
            if (!diffs.isEmpty()) {
                allDiffs.add(routeName + " " + tableTitle + " visual ground truth mismatch:\n"
                        + String.join("\n", diffs)
                        + "\nexpected=" + expected.describe()
                        + "\nactual=" + actual.describe());
            } else {
                matchedTitles.add(tableTitle);
            }
        }

        assertTrue(allDiffs.isEmpty(),
                () -> routeName + " full Word process visual ground truth diff failed, matched="
                        + matchedTitles.size() + "/" + PROCESS_TABLE_TITLES.size()
                        + ", titles=" + matchedTitles
                        + "\n" + String.join("\n\n", allDiffs));
        assertTrue(PROCESS_TABLE_TITLES.equals(matchedTitles),
                () -> routeName + " full Word process coverage mismatch, expected="
                        + PROCESS_TABLE_TITLES + ", actual=" + matchedTitles);
    }

    private static byte[] realDocBytes() throws Exception {
        assertTrue(Files.exists(REAL_DOC), "required real DOC is missing: " + REAL_DOC);
        if (cachedRealDocBytes == null) {
            cachedRealDocBytes = Files.readAllBytes(REAL_DOC);
        }
        return cachedRealDocBytes;
    }

    private List<MesProBatchRecordParsedTable> routeATables(byte[] bytes) {
        if (cachedRouteATables == null) {
            cachedRouteATables = new MesProBatchRecordRouteARecognizer(parser)
                    .recognize(REAL_DOC, bytes, REAL_DOC.getFileName().toString());
        }
        return cachedRouteATables;
    }

    private static List<MesProBatchRecordParsedTable> routeBTables(byte[] bytes) {
        if (cachedRouteBTables == null) {
            cachedRouteBTables = new MesProBatchRecordRouteBRecognizer()
                    .recognize(REAL_DOC, bytes, REAL_DOC.getFileName().toString());
        }
        return cachedRouteBTables;
    }

    private static WordTableShape cachedWordTableShape(byte[] bytes, String title) throws Exception {
        WordTableShape cached = CACHED_WORD_SHAPES.get(title);
        if (cached == null) {
            cached = extractWordTableShape(bytes, title);
            CACHED_WORD_SHAPES.put(title, cached);
        }
        return cached;
    }

    private static WordTableShape extractWordTableShape(byte[] bytes, String title) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes))) {
            Range range = document.getRange();
            Constructor<TableIterator> constructor = TableIterator.class.getDeclaredConstructor(Range.class, int.class);
            constructor.setAccessible(true);
            TableIterator iterator = constructor.newInstance(range, 1);
            while (iterator.hasNext()) {
                Table table = iterator.next();
                if (!tableText(table).contains(title)) {
                    continue;
                }
                return toWordTableShape(table, title);
            }
        }
        throw new AssertionError("source Word table not found: " + title);
    }

    private static WordTableShape toWordTableShape(Table table, String title) {
        List<List<ShapeCell>> rows = new ArrayList<>();
        List<Integer> rowHeights = new ArrayList<>();
        List<List<ColumnSpanWidth>> rowColumnWidths = new ArrayList<>();
        int segmentStart = findSegmentStart(table, title);
        int segmentEnd = findNextProcessSegmentStart(table, segmentStart + 1);
        int logicalColumnCount = resolveLogicalVisualColumnCount(table, segmentStart, segmentEnd);
        List<Integer> boundaryColumnBoundaries = resolveBoundaryColumnBoundaries(table, 0, table.numRows());
        List<Integer> boundaryColumnWidths = resolveBoundaryColumnWidths(boundaryColumnBoundaries);
        boolean useBoundaryGrid = shouldUseBoundaryGrid(table, segmentStart, segmentEnd, boundaryColumnBoundaries,
                boundaryColumnWidths.size(), logicalColumnCount);
        int columnCount = useBoundaryGrid ? boundaryColumnWidths.size() : logicalColumnCount;
        List<Integer> visualColumnWidths = useBoundaryGrid
                ? boundaryColumnWidths
                : resolveLogicalVisualColumnWidths(table, segmentStart, segmentEnd, columnCount);
        int totalVisualWidth = visualColumnWidths.stream().mapToInt(Integer::intValue).sum();
        int[] occupiedUntilRowByColumn = new int[columnCount];
        for (int columnIndex = 0; columnIndex < occupiedUntilRowByColumn.length; columnIndex++) {
            occupiedUntilRowByColumn[columnIndex] = -1;
        }
        for (int sourceRowIndex = segmentStart; sourceRowIndex < segmentEnd; sourceRowIndex++) {
            int rowIndex = sourceRowIndex - segmentStart;
            TableRow row = table.getRow(sourceRowIndex);
            List<ShapeCell> shapeRow = new ArrayList<>();
            List<ColumnSpanWidth> widths = new ArrayList<>();
            int rowHeight = toPixels(row.getRowHeight(), 36);
            int columnIndex = 0;
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                while (!useBoundaryGrid && columnIndex < columnCount && occupiedUntilRowByColumn[columnIndex] >= rowIndex) {
                    columnIndex++;
                }
                int cellColumnIndex = useBoundaryGrid
                        ? resolveBoundaryStartColumnIndex(boundaryColumnBoundaries, cell)
                        : columnIndex;
                int colSpan = useBoundaryGrid
                        ? resolveBoundaryColSpan(boundaryColumnBoundaries, cell, cellColumnIndex)
                        : resolveVisualColSpan(cell, visualColumnWidths, columnIndex, totalVisualWidth,
                        resolveAvailableColSpan(row, cellIndex, columnCount, columnIndex,
                                occupiedUntilRowByColumn, rowIndex));
                int rowSpan = Math.min(resolveRowSpan(table, sourceRowIndex, cellIndex), segmentEnd - sourceRowIndex);
                int width = toWidthUnits(cell.getWidth());
                shapeRow.add(new ShapeCell(rowIndex, cellColumnIndex, rowSpan, colSpan,
                        normalizeCellText(cell.text()),
                        new BorderShape(resolveBorderStyle(cell.getBrcTop()),
                                resolveBorderStyle(cell.getBrcBottom()),
                                resolveBorderStyle(cell.getBrcLeft()),
                                resolveBorderStyle(cell.getBrcRight()))));
                widths.add(new ColumnSpanWidth(cellColumnIndex, colSpan, width));
                if (rowSpan > 1) {
                    for (int offset = 0; offset < colSpan && cellColumnIndex + offset < occupiedUntilRowByColumn.length; offset++) {
                        occupiedUntilRowByColumn[cellColumnIndex + offset] = rowIndex + rowSpan - 1;
                    }
                }
                columnIndex = cellColumnIndex + colSpan;
            }
            rows.add(shapeRow);
            rowHeights.add(rowHeight);
            rowColumnWidths.add(widths);
        }
        WordTableShape rawShape = new WordTableShape(rows.size(), columnCount,
                visualColumnWidths.stream().mapToInt(Integer::intValue).sum(),
                normalizeWidthVector(visualColumnWidths), rowHeights, rows);
        return normalizeImplicitVisualHeaderCoverage(expandPackedMaterialMatrixRows(rawShape));
    }

    private static WordTableShape normalizeImplicitVisualHeaderCoverage(WordTableShape source) {
        List<List<ShapeCell>> rows = copyShapeRows(source.rows());
        boolean changed = false;
        for (int rowIndex = 0; rowIndex + 1 < rows.size(); rowIndex++) {
            List<ShapeCell> headerRow = rows.get(rowIndex);
            List<ShapeCell> detailRow = rows.get(rowIndex + 1);
            if (!isMultiLevelSemanticHeaderShapeRow(headerRow, source.columnCount())) {
                continue;
            }
            List<ShapeCell> normalizedHeaderRow = new ArrayList<>();
            for (ShapeCell cell : headerRow) {
                if (isNarrowSemanticHeaderShapeCell(cell, source.columnCount())
                        && !hasShapeCellInRange(detailRow, cell.columnIndex(), cell.columnIndex() + cell.colSpan())) {
                    normalizedHeaderRow.add(new ShapeCell(cell.rowIndex(), cell.columnIndex(),
                            Math.max(2, cell.rowSpan()), cell.colSpan(), cell.text(), cell.border()));
                    changed = true;
                } else {
                    normalizedHeaderRow.add(cell);
                }
            }
            rows.set(rowIndex, normalizedHeaderRow);
        }
        if (!changed) {
            return source;
        }
        return new WordTableShape(source.rowCount(), source.columnCount(), source.tableWidth(),
                source.columnWidths(), source.rowHeights(), rows);
    }

    private static List<List<ShapeCell>> copyShapeRows(List<List<ShapeCell>> sourceRows) {
        List<List<ShapeCell>> rows = new ArrayList<>();
        for (List<ShapeCell> row : sourceRows) {
            rows.add(new ArrayList<>(row));
        }
        return rows;
    }

    private static boolean isMultiLevelSemanticHeaderShapeRow(List<ShapeCell> row, int columnCount) {
        if (row == null || row.size() < 6 || columnCount < 20) {
            return false;
        }
        long semanticHeaderCount = row.stream()
                .filter(cell -> isNarrowSemanticHeaderShapeCell(cell, columnCount))
                .count();
        return semanticHeaderCount >= 3;
    }

    private static boolean isNarrowSemanticHeaderShapeCell(ShapeCell cell, int columnCount) {
        if (cell == null || cell.rowSpan() > 1 || cell.colSpan() <= 1) {
            return false;
        }
        return MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                cell.text(), cell.columnIndex(), columnCount) > 0;
    }

    private static boolean hasShapeCellInRange(List<ShapeCell> row, int startColumn, int endColumn) {
        if (row == null || endColumn <= startColumn) {
            return false;
        }
        for (ShapeCell cell : row) {
            if (cell.columnIndex() < endColumn && cell.columnIndex() + cell.colSpan() > startColumn) {
                return true;
            }
        }
        return false;
    }

    private static int resolveLogicalVisualColumnCount(Table table, int segmentStart, int segmentEnd) {
        int columnCount = 0;
        for (int sourceRowIndex = segmentStart; sourceRowIndex < segmentEnd; sourceRowIndex++) {
            TableRow row = table.getRow(sourceRowIndex);
            columnCount = Math.max(columnCount, row.numCells());
        }
        return columnCount;
    }

    private static List<Integer> resolveLogicalVisualColumnWidths(Table table, int segmentStart, int segmentEnd, int columnCount) {
        TableRow baseRow = table.getRow(resolveDensestVisualRowIndex(table, segmentStart, segmentEnd));
        List<Integer> widths = new ArrayList<>();
        for (int cellIndex = 0; cellIndex < baseRow.numCells() && widths.size() < columnCount; cellIndex++) {
            widths.add(toWidthUnits(baseRow.getCell(cellIndex).getWidth()));
        }
        while (widths.size() < columnCount) {
            widths.add(120);
        }
        return widths;
    }

    private static List<Integer> resolveBoundaryColumnBoundaries(Table table, int segmentStart, int segmentEnd) {
        TreeSet<Integer> boundaries = new TreeSet<>();
        for (int sourceRowIndex = segmentStart; sourceRowIndex < segmentEnd; sourceRowIndex++) {
            TableRow row = table.getRow(sourceRowIndex);
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                int left = cell.getLeftEdge();
                int right = left + Math.max(1, cell.getWidth());
                if (left >= 0 && right > left) {
                    boundaries.add(left);
                    boundaries.add(right);
                }
            }
        }
        return new ArrayList<>(boundaries);
    }

    private static List<Integer> resolveBoundaryColumnWidths(List<Integer> boundaries) {
        if (boundaries == null || boundaries.size() < 2) {
            return List.of();
        }
        List<Integer> widths = new ArrayList<>();
        for (int index = 0; index < boundaries.size() - 1; index++) {
            int width = boundaries.get(index + 1) - boundaries.get(index);
            widths.add(toWidthUnits(width));
        }
        return widths;
    }

    private static boolean shouldUseBoundaryGrid(Table table, int segmentStart, int segmentEnd,
                                                 List<Integer> boundaries,
                                                 int boundaryColumnCount, int logicalColumnCount) {
        if (boundaryColumnCount < 60 || logicalColumnCount <= 0
                || boundaryColumnCount < logicalColumnCount * 3) {
            return false;
        }
        int densestRowCellCount = 0;
        boolean hasPackedInteriorGrid = false;
        boolean hasWideMergedVisualCell = false;
        for (int sourceRowIndex = segmentStart; sourceRowIndex < segmentEnd; sourceRowIndex++) {
            TableRow row = table.getRow(sourceRowIndex);
            densestRowCellCount = Math.max(densestRowCellCount, row.numCells());
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                int startColumnIndex = resolveBoundaryStartColumnIndex(boundaries, cell);
                int span = resolveBoundaryColSpan(boundaries, cell, startColumnIndex);
                if (span >= 20) {
                    hasWideMergedVisualCell = true;
                }
                if (span >= Math.max(20, Math.round(boundaryColumnCount * 0.35f))
                        && isPackedLabelGridText(cell.text())) {
                    hasPackedInteriorGrid = true;
                }
            }
        }
        boolean sparseRowsOnDenseGrid = densestRowCellCount > 0
                && boundaryColumnCount >= densestRowCellCount * 6;
        return hasPackedInteriorGrid && (hasWideMergedVisualCell || sparseRowsOnDenseGrid);
    }

    private static int resolveBoundaryStartColumnIndex(List<Integer> boundaries, TableCell cell) {
        if (boundaries == null || boundaries.size() < 2) {
            return 0;
        }
        int left = cell.getLeftEdge();
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < boundaries.size() - 1; index++) {
            int distance = Math.abs(boundaries.get(index) - left);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private static int resolveBoundaryColSpan(List<Integer> boundaries, TableCell cell, int startColumnIndex) {
        if (boundaries == null || boundaries.size() < 2) {
            return 1;
        }
        int right = cell.getLeftEdge() + Math.max(1, cell.getWidth());
        int bestIndex = Math.min(boundaries.size() - 1, Math.max(startColumnIndex + 1, startColumnIndex));
        int bestDistance = Integer.MAX_VALUE;
        for (int index = Math.max(1, startColumnIndex + 1); index < boundaries.size(); index++) {
            int distance = Math.abs(boundaries.get(index) - right);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return Math.max(1, bestIndex - startColumnIndex);
    }

    private static boolean isPackedLabelGridText(String text) {
        String normalized = normalizeCellText(text);
        if (normalized.isBlank()) {
            return false;
        }
        long slashSeparatorCount = normalized.chars()
                .filter(ch -> ch == '/' || ch == '／')
                .count();
        if (slashSeparatorCount < 5) {
            return false;
        }
        String[] tokens = normalized.split("[\\n/、，,；;：:\\s]+");
        int shortLabelCount = 0;
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 10) {
                shortLabelCount++;
            }
        }
        return shortLabelCount >= 6;
    }

    private static int resolveDensestVisualRowIndex(Table table, int segmentStart, int segmentEnd) {
        int bestRowIndex = segmentStart;
        int bestCellCount = -1;
        for (int sourceRowIndex = segmentStart; sourceRowIndex < segmentEnd; sourceRowIndex++) {
            TableRow row = table.getRow(sourceRowIndex);
            if (row.numCells() > bestCellCount) {
                bestCellCount = row.numCells();
                bestRowIndex = sourceRowIndex;
            }
        }
        return bestRowIndex;
    }

    private static int resolveAvailableColSpan(TableRow row, int cellIndex, int columnCount, int startColumnIndex,
                                               int[] occupiedUntilRowByColumn, int rowIndex) {
        int remainingCells = 0;
        for (int index = cellIndex; index < row.numCells(); index++) {
            if (!isMergedFollower(row.getCell(index))) {
                remainingCells++;
            }
        }
        int availableColumns = 0;
        for (int columnIndex = startColumnIndex; columnIndex < columnCount; columnIndex++) {
            if (occupiedUntilRowByColumn != null
                    && columnIndex < occupiedUntilRowByColumn.length
                    && occupiedUntilRowByColumn[columnIndex] >= rowIndex) {
                continue;
            }
            availableColumns++;
        }
        return Math.max(1, availableColumns - Math.max(0, remainingCells - 1));
    }

    private static int resolveVisualColSpan(TableCell cell, List<Integer> visualColumnWidths,
                                            int startColumnIndex, int totalVisualWidth, int maxSpan) {
        int width = toWidthUnits(cell.getWidth());
        if (startColumnIndex >= visualColumnWidths.size()) {
            return 1;
        }
        if (width >= Math.max(1, totalVisualWidth * 0.92f)) {
            return Math.min(maxSpan, visualColumnWidths.size() - startColumnIndex);
        }
        int accumulated = 0;
        int span = 0;
        for (int columnIndex = startColumnIndex;
             columnIndex < visualColumnWidths.size() && span < Math.max(1, maxSpan);
             columnIndex++) {
            accumulated += visualColumnWidths.get(columnIndex);
            span++;
            if (accumulated >= width * 0.85f) {
                break;
            }
        }
        return Math.max(1, Math.min(span, Math.min(maxSpan, visualColumnWidths.size() - startColumnIndex)));
    }

    private static int findSegmentStart(Table table, String title) {
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            if (rowText(table.getRow(rowIndex)).contains(title)) {
                return rowIndex;
            }
        }
        throw new AssertionError("source Word segment not found: " + title);
    }

    private static int findNextProcessSegmentStart(Table table, int startRowIndex) {
        for (int rowIndex = startRowIndex; rowIndex < table.numRows(); rowIndex++) {
            String text = rowText(table.getRow(rowIndex)).replace("\n", "");
            if (text.contains("工序生产记录")) {
                return rowIndex;
            }
        }
        return table.numRows();
    }

    private static JsonTableShape extractJsonTableShape(JSONObject root) {
        int columnCount = root.getJSONObject("cols").getIntValue("len");
        JSONArray styles = root.getJSONArray("styles");
        List<Integer> columnWidths = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            columnWidths.add(root.getJSONObject("cols")
                    .getJSONObject(String.valueOf(columnIndex))
                    .getIntValue("width"));
        }
        JSONObject rowsObject = root.getJSONObject("rows");
        List<String> rowKeys = rowsObject.keySet().stream()
                .filter(key -> key.chars().allMatch(Character::isDigit))
                .sorted(Comparator.comparingInt(Integer::parseInt))
                .toList();
        List<Integer> rowHeights = new ArrayList<>();
        List<List<ShapeCell>> rows = new ArrayList<>();
        for (String rowKey : rowKeys) {
            int rowIndex = Integer.parseInt(rowKey);
            JSONObject rowObject = rowsObject.getJSONObject(rowKey);
            rowHeights.add(rowObject.getIntValue("height"));
            JSONObject cellsObject = rowObject.getJSONObject("cells");
            List<String> cellKeys = cellsObject.keySet().stream()
                    .filter(key -> key.chars().allMatch(Character::isDigit))
                    .sorted(Comparator.comparingInt(Integer::parseInt))
                    .toList();
            List<ShapeCell> row = new ArrayList<>();
            for (String cellKey : cellKeys) {
                int columnIndex = Integer.parseInt(cellKey);
                JSONObject cellObject = cellsObject.getJSONObject(cellKey);
                int rowSpan = 1;
                int colSpan = 1;
                if (cellObject.getJSONArray("merge") != null) {
                    rowSpan = cellObject.getJSONArray("merge").getIntValue(0) + 1;
                    colSpan = cellObject.getJSONArray("merge").getIntValue(1) + 1;
                }
                row.add(new ShapeCell(rowIndex, columnIndex, rowSpan, colSpan,
                        normalizeCellText(cellObject.getString("text")),
                        extractBorderShape(styles, cellObject)));
            }
            rows.add(row);
        }
        int tableWidth = columnWidths.stream().mapToInt(Integer::intValue).sum();
        return new JsonTableShape(rows.size(), columnCount, tableWidth, columnWidths, rowHeights, rows);
    }

    private static List<String> diff(WordTableShape expected, JsonTableShape actual) {
        List<String> diffs = new ArrayList<>();
        if (expected.rowCount() != actual.rowCount()) {
            diffs.add("rowCount expected=" + expected.rowCount() + ", actual=" + actual.rowCount());
        }
        if (expected.columnCount() != actual.columnCount()) {
            diffs.add("columnCount expected=" + expected.columnCount() + ", actual=" + actual.columnCount());
        }
        List<Integer> expectedColumnWidths = normalizeExpectedRenderedColumnWidths(
                expected.columnWidths(), expected.rows(), actual.tableWidth());
        if (!isSameWidthRatio(expectedColumnWidths, actual.columnWidths())) {
            diffs.add("columnWidths expected=" + expectedColumnWidths + ", actual=" + actual.columnWidths());
        }
        if (!isSameScalarRatio(expected.tableWidth(), actual.tableWidth())) {
            diffs.add("tableWidth expected=" + expected.tableWidth() + ", actual=" + actual.tableWidth());
        }
        if (!isSameHeightRatio(expected.rowHeights(), actual.rowHeights())) {
            diffs.add("rowHeights expected=" + expected.rowHeights() + ", actual=" + actual.rowHeights());
        }
        List<String> overflowCells = findOverflowCells(actual);
        if (!overflowCells.isEmpty()) {
            diffs.add("cells exceed declared cols.len: " + overflowCells);
        }
        int comparableRows = Math.min(expected.rows().size(), actual.rows().size());
        for (int rowIndex = 0; rowIndex < comparableRows; rowIndex++) {
            List<ShapeCell> expectedRow = expected.rows().get(rowIndex);
            List<ShapeCell> actualRow = actual.rows().get(rowIndex);
            if (expectedRow.size() != actualRow.size()) {
                diffs.add("row " + rowIndex + " cellCount expected=" + expectedRow.size()
                        + ", actual=" + actualRow.size()
                        + ", expectedCells=" + compactCells(expectedRow)
                        + ", actualCells=" + compactCells(actualRow));
                continue;
            }
            for (int cellIndex = 0; cellIndex < expectedRow.size(); cellIndex++) {
                ShapeCell expectedCell = expectedRow.get(cellIndex);
                ShapeCell actualCell = actualRow.get(cellIndex);
                if (expectedCell.columnIndex() != actualCell.columnIndex()
                        || expectedCell.rowSpan() != actualCell.rowSpan()
                        || expectedCell.colSpan() != actualCell.colSpan()
                        || !expectedCell.text().equals(actualCell.text())) {
                    diffs.add("row " + rowIndex + " cell " + cellIndex
                            + " expected=" + expectedCell.compact()
                            + ", actual=" + actualCell.compact());
                }
                List<String> borderDiffs = diffBorder(expectedCell.border(), actualCell.border());
                if (!borderDiffs.isEmpty()) {
                    diffs.add("row " + rowIndex + " cell " + cellIndex
                            + " border expected=" + expectedCell.border().compact()
                            + ", actual=" + actualCell.border().compact()
                            + ", diffs=" + borderDiffs
                            + ", cell=" + expectedCell.compact());
                }
                if (!actualCell.border().isComplete()) {
                    diffs.add("row " + rowIndex + " cell " + cellIndex
                            + " rendered border incomplete actual=" + actualCell.border().compact()
                            + ", cell=" + actualCell.compact());
                }
            }
        }
        if (hasRightSidePhantomBlankColumn(actual)) {
            diffs.add("right-side phantom blank column detected at column " + (actual.columnCount() - 1));
        }
        return diffs;
    }

    private static List<String> findOverflowCells(JsonTableShape actual) {
        List<String> overflow = new ArrayList<>();
        for (List<ShapeCell> row : actual.rows()) {
            for (ShapeCell cell : row) {
                if (cell.columnIndex() + cell.colSpan() > actual.columnCount()) {
                    overflow.add("r" + cell.rowIndex() + "c" + cell.columnIndex()
                            + "[cs=" + cell.colSpan() + ", cols.len=" + actual.columnCount()
                            + ", text=" + cell.text().replace("\n", "/") + "]");
                }
            }
        }
        return overflow;
    }

    private static boolean hasRightSidePhantomBlankColumn(JsonTableShape actual) {
        if (actual.columnCount() <= 0) {
            return false;
        }
        int lastColumn = actual.columnCount() - 1;
        int touchedRows = 0;
        int blankRows = 0;
        for (List<ShapeCell> row : actual.rows()) {
            for (ShapeCell cell : row) {
                if (cell.columnIndex() <= lastColumn && cell.columnIndex() + cell.colSpan() > lastColumn) {
                    touchedRows++;
                    if (cell.text().isBlank()) {
                        blankRows++;
                    }
                    break;
                }
            }
        }
        return touchedRows >= Math.max(4, actual.rowCount() / 4) && blankRows == touchedRows;
    }

    private static BorderShape extractBorderShape(JSONArray styles, JSONObject cellObject) {
        if (cellObject == null || styles == null) {
            return BorderShape.empty();
        }
        int styleIndex = cellObject.getIntValue("style");
        if (styleIndex < 0 || styleIndex >= styles.size()) {
            return BorderShape.empty();
        }
        JSONObject style = styles.getJSONObject(styleIndex);
        if (style == null) {
            return BorderShape.empty();
        }
        JSONObject border = style.getJSONObject("border");
        if (border == null) {
            return BorderShape.empty();
        }
        return new BorderShape(borderSide(border, "top"), borderSide(border, "bottom"),
                borderSide(border, "left"), borderSide(border, "right"));
    }

    private static String borderSide(JSONObject border, String side) {
        JSONArray value = border.getJSONArray(side);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value.getString(0);
    }

    private static List<String> diffBorder(BorderShape expected, BorderShape actual) {
        List<String> diffs = new ArrayList<>();
        compareBorderSide("top", expected.top(), actual.top(), diffs);
        compareBorderSide("bottom", expected.bottom(), actual.bottom(), diffs);
        compareBorderSide("left", expected.left(), actual.left(), diffs);
        compareBorderSide("right", expected.right(), actual.right(), diffs);
        return diffs;
    }

    private static void compareBorderSide(String side, String expected, String actual, List<String> diffs) {
        if (expected == null) {
            return;
        }
        if (actual == null) {
            diffs.add(side + " missing");
            return;
        }
        if (!borderWeightAtLeast(actual, expected)) {
            diffs.add(side + " expectedAtLeast=" + expected + " actual=" + actual);
        }
    }

    private static boolean borderWeightAtLeast(String actual, String expected) {
        return borderWeightRank(actual) >= borderWeightRank(expected);
    }

    private static int borderWeightRank(String value) {
        if ("thick".equals(value)) {
            return 3;
        }
        if ("medium".equals(value)) {
            return 2;
        }
        if ("thin".equals(value)) {
            return 1;
        }
        return 0;
    }

    private static List<Integer> resolveColumnWidths(int columnCount, List<List<ColumnSpanWidth>> rows) {
        int[] widths = new int[columnCount];
        for (List<ColumnSpanWidth> row : rows) {
            for (ColumnSpanWidth spanWidth : row) {
                int perColumnWidth = Math.max(1, Math.round(spanWidth.widthPx() / (float) Math.max(1, spanWidth.colSpan())));
                for (int offset = 0; offset < spanWidth.colSpan() && spanWidth.columnIndex() + offset < widths.length; offset++) {
                    widths[spanWidth.columnIndex() + offset] = Math.max(
                            widths[spanWidth.columnIndex() + offset], perColumnWidth);
                }
            }
        }
        List<Integer> resolved = new ArrayList<>();
        for (int width : widths) {
            resolved.add(width);
        }
        return resolved;
    }

    private static List<Integer> normalizeWidthVector(List<Integer> widths) {
        int total = widths.stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return widths;
        }
        List<Integer> normalized = new ArrayList<>();
        for (Integer width : widths) {
            normalized.add(Math.round(width * 10000.0f / total));
        }
        return normalized;
    }

    private static boolean isSameWidthRatio(List<Integer> expected, List<Integer> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        List<Integer> normalizedExpected = normalizeWidthVector(expected);
        List<Integer> normalizedActual = normalizeWidthVector(actual);
        for (int index = 0; index < normalizedExpected.size(); index++) {
            if (Math.abs(normalizedExpected.get(index) - normalizedActual.get(index)) > 120) {
                return false;
            }
        }
        return true;
    }

    private static List<Integer> normalizeExpectedRenderedColumnWidths(List<Integer> sourceWidths,
                                                                       List<List<ShapeCell>> rows,
                                                                       int targetWidth) {
        if (sourceWidths == null || sourceWidths.isEmpty() || targetWidth <= 0) {
            return sourceWidths;
        }
        List<Integer> scaledWidths = stretchWidthsToTarget(sourceWidths, targetWidth);
        return normalizeVisibleVerticalSectionColumnWidth(scaledWidths, rows);
    }

    private static List<Integer> stretchWidthsToTarget(List<Integer> sourceWidths, int targetWidth) {
        int sourceTotal = sourceWidths.stream().mapToInt(Integer::intValue).sum();
        if (sourceTotal <= 0) {
            return sourceWidths;
        }
        List<Integer> scaled = new ArrayList<>(sourceWidths.size());
        int assignedWidth = 0;
        for (Integer sourceWidth : sourceWidths) {
            int width = Math.max(1, Math.round(Math.max(1, sourceWidth) * (targetWidth / (float) sourceTotal)));
            scaled.add(width);
            assignedWidth += width;
        }
        int remainder = targetWidth - assignedWidth;
        int cursor = 0;
        while (remainder != 0 && !scaled.isEmpty()) {
            int index = cursor % scaled.size();
            int current = scaled.get(index);
            if (remainder > 0) {
                scaled.set(index, current + 1);
                remainder--;
            } else if (current > 1) {
                scaled.set(index, current - 1);
                remainder++;
            }
            cursor++;
            if (cursor > scaled.size() * 4 && remainder < 0) {
                break;
            }
        }
        return scaled;
    }

    private static List<Integer> normalizeVisibleVerticalSectionColumnWidth(List<Integer> sourceWidths,
                                                                            List<List<ShapeCell>> rows) {
        if (sourceWidths == null || sourceWidths.isEmpty() || rows == null
                || !hasLeadingVerticalSectionColumn(rows)) {
            return sourceWidths;
        }
        List<Integer> widths = new ArrayList<>(sourceWidths);
        int visibleWidth = 24;
        int currentWidth = Math.max(0, widths.get(0));
        if (currentWidth >= visibleWidth) {
            return widths;
        }
        int deficit = visibleWidth - currentWidth;
        widths.set(0, visibleWidth);
        while (deficit > 0) {
            int donor = findBestWidthDonor(widths);
            if (donor <= 0) {
                break;
            }
            widths.set(donor, widths.get(donor) - 1);
            deficit--;
        }
        return widths;
    }

    private static boolean hasLeadingVerticalSectionColumn(List<List<ShapeCell>> rows) {
        for (List<ShapeCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            ShapeCell firstCell = row.get(0);
            if (firstCell == null || firstCell.columnIndex() != 0
                    || firstCell.colSpan() != 1 || firstCell.rowSpan() < 3) {
                continue;
            }
            if (isStructurallyNarrowLeadingSectionCell(firstCell)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStructurallyNarrowLeadingSectionCell(ShapeCell firstCell) {
        String text = normalizeCellText(firstCell.text()).replace("\n", "");
        return !text.isBlank();
    }

    private static int findBestWidthDonor(List<Integer> widths) {
        int bestColumn = -1;
        int bestSlack = 0;
        for (int columnIndex = 1; columnIndex < widths.size(); columnIndex++) {
            int slack = Math.max(0, widths.get(columnIndex) - 18);
            if (slack > bestSlack) {
                bestSlack = slack;
                bestColumn = columnIndex;
            }
        }
        return bestColumn;
    }

    private static boolean isSameScalarRatio(int expected, int actual) {
        if (expected <= 0 || actual <= 0) {
            return expected == actual;
        }
        float ratio = actual / (float) expected;
        return ratio >= 0.85f && ratio <= 1.25f;
    }

    private static boolean isSameHeightRatio(List<Integer> expected, List<Integer> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        for (int rowIndex = 0; rowIndex < expected.size(); rowIndex++) {
            int expectedHeight = expected.get(rowIndex);
            int actualHeight = actual.get(rowIndex);
            if (!isSameScalarRatio(expectedHeight, actualHeight)) {
                return false;
            }
        }
        return true;
    }

    private static String compactCells(List<ShapeCell> cells) {
        return cells.stream().map(ShapeCell::compact).toList().toString();
    }

    private static WordTableShape expandPackedMaterialMatrixRows(WordTableShape source) {
        List<List<ShapeCell>> expandedRows = new ArrayList<>();
        List<Integer> expandedHeights = new ArrayList<>();
        boolean expanded = false;
        for (int rowIndex = 0; rowIndex < source.rows().size(); rowIndex++) {
            List<ShapeCell> row = source.rows().get(rowIndex);
            PackedShapeMatrix packed = parsePackedMaterialMatrixRow(row, source.columnCount());
            if (packed == null) {
                expandedRows.add(row);
                expandedHeights.add(source.rowHeights().get(rowIndex));
                continue;
            }
            List<List<ShapeCell>> materialRows = buildPackedMaterialMatrixRows(
                    packed, source.columnCount(), rowIndex);
            expandedRows.addAll(materialRows);
            for (int index = 0; index < materialRows.size(); index++) {
                expandedHeights.add(24);
            }
            expanded = true;
        }
        if (!expanded) {
            return source;
        }
        return new WordTableShape(expandedRows.size(), source.columnCount(), source.tableWidth(),
                source.columnWidths(), expandedHeights, reindexRows(expandedRows));
    }

    private static PackedShapeMatrix parsePackedMaterialMatrixRow(List<ShapeCell> row, int columnCount) {
        if (row == null || row.size() != 2 || columnCount < 7) {
            return null;
        }
        ShapeCell sideCell = row.get(0);
        ShapeCell packedCell = row.get(1);
        if (sideCell == null || packedCell == null
                || sideCell.text().isBlank()
                || sideCell.rowSpan() < 4
                || sideCell.colSpan() > Math.max(4, columnCount / 20)
                || packedCell.colSpan() < columnCount - Math.max(8, sideCell.colSpan() + 2)) {
            return null;
        }
        List<String> lines = packedCell.text().lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.size() < 8) {
            return null;
        }
        List<String> headerTexts = new ArrayList<>(lines.subList(0, 6));
        if (!isRepeatedHeaderTextPattern(headerTexts)) {
            return null;
        }
        List<String> itemNames = new ArrayList<>();
        for (int index = 6; index < lines.size(); index++) {
            String line = lines.get(index);
            if (!"/".equals(line)) {
                itemNames.add(line);
            }
        }
        if (itemNames.size() < 2) {
            return null;
        }
        return new PackedShapeMatrix(sideCell, packedCell, headerTexts, itemNames);
    }

    private static List<List<ShapeCell>> buildPackedMaterialMatrixRows(PackedShapeMatrix packed,
                                                                       int columnCount,
                                                                       int rowIndex) {
        int sideColSpan = Math.max(1, packed.sideCell().colSpan());
        int availableColumns = Math.max(1, columnCount - sideColSpan);
        int[] colSpans = distributeAdaptiveColSpans(packed.headerTexts(), availableColumns);
        int detailRowCount = (int) Math.ceil(packed.itemNames().size() / 2.0d);
        List<List<ShapeCell>> rows = new ArrayList<>();
        ShapeCell sideCell = new ShapeCell(rowIndex, packed.sideCell().columnIndex(),
                packed.sideCell().rowSpan() + detailRowCount, sideColSpan,
                packed.sideCell().text(), packed.sideCell().border());

        List<ShapeCell> headerRow = new ArrayList<>();
        headerRow.add(sideCell);
        headerRow.addAll(buildPackedMaterialMatrixCells(rowIndex, sideColSpan,
                packed.headerTexts(), colSpans, packed.packedCell().border()));
        rows.add(headerRow);

        for (int pairIndex = 0; pairIndex < detailRowCount; pairIndex++) {
            int leftIndex = pairIndex * 2;
            String leftItem = packed.itemNames().get(leftIndex);
            String rightItem = leftIndex + 1 < packed.itemNames().size() ? packed.itemNames().get(leftIndex + 1) : "";
            List<String> detailTexts = List.of("/", leftItem, "", "/", rightItem, "");
            rows.add(buildPackedMaterialMatrixCells(rowIndex + pairIndex + 1, sideColSpan,
                    detailTexts, colSpans, packed.packedCell().border()));
        }
        return rows;
    }

    private static List<ShapeCell> buildPackedMaterialMatrixCells(int rowIndex,
                                                                  int startColumn,
                                                                  List<String> texts,
                                                                  int[] colSpans,
                                                                  BorderShape border) {
        List<ShapeCell> cells = new ArrayList<>();
        int columnIndex = startColumn;
        for (int index = 0; index < texts.size(); index++) {
            int colSpan = colSpans[index];
            cells.add(new ShapeCell(rowIndex, columnIndex, 1, colSpan, texts.get(index), border));
            columnIndex += colSpan;
        }
        return cells;
    }

    private static List<List<ShapeCell>> reindexRows(List<List<ShapeCell>> rows) {
        List<List<ShapeCell>> reindexed = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<ShapeCell> row = rows.get(rowIndex);
            List<ShapeCell> reindexedRow = new ArrayList<>();
            for (ShapeCell cell : row) {
                reindexedRow.add(new ShapeCell(rowIndex, cell.columnIndex(), cell.rowSpan(),
                        cell.colSpan(), cell.text(), cell.border()));
            }
            reindexed.add(reindexedRow);
        }
        return reindexed;
    }

    private static int[] distributeAdaptiveColSpans(List<String> texts, int totalColSpan) {
        int size = texts.size();
        int[] colSpans = new int[size];
        int[] weights = new int[size];
        int totalWeight = 0;
        for (int index = 0; index < size; index++) {
            weights[index] = estimateCellWeight(texts.get(index), index, totalColSpan);
            totalWeight += weights[index];
            colSpans[index] = 1;
        }
        int remaining = totalColSpan - size;
        while (remaining > 0) {
            int bestIndex = 0;
            int bestScore = Integer.MIN_VALUE;
            for (int index = 0; index < size; index++) {
                int score = weights[index] * 100 - colSpans[index] * totalWeight;
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = index;
                }
            }
            colSpans[bestIndex]++;
            remaining--;
        }
        return colSpans;
    }

    private static int estimateCellWeight(String text, int index, int totalColSpan) {
        String normalized = normalizeCellText(text).replaceAll("\\s+", "");
        if (normalized.isBlank()) {
            return 1;
        }
        int weight = Math.max(1, Math.min(6, normalized.length() / 3 + 1));
        if (normalized.endsWith("人") || normalized.endsWith("数量") || normalized.endsWith("日期")
                || normalized.contains("pcs")) {
            weight += totalColSpan >= 20 ? 2 : 1;
        }
        return Math.min(8, weight);
    }

    private static boolean hasExpandedTopMaterialMatrix(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return false;
        }
        int titleRowIndex = findParsedTitleRowIndex(table);
        int endRowIndex = Math.min(table.getRows().size(), Math.max(0, titleRowIndex) + 12);
        for (int rowIndex = Math.max(0, titleRowIndex); rowIndex < endRowIndex; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = table.getRows().get(rowIndex);
            if (row == null || row.size() < 7) {
                continue;
            }
            List<String> texts = row.stream()
                    .map(MesProBatchRecordJingxiTableStructureVerificationTest::parsedText)
                    .filter(text -> !text.isBlank())
                    .toList();
            if (texts.size() >= 6 && isRepeatedHeaderTextPattern(texts.subList(Math.max(0, texts.size() - 6), texts.size()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCollapsedPackedMaterialMatrixCell(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return false;
        }
        int titleRowIndex = findParsedTitleRowIndex(table);
        int endRowIndex = Math.min(table.getRows().size(), Math.max(0, titleRowIndex) + 12);
        int columnCount = Math.max(1, table.getColumnCount());
        for (int rowIndex = Math.max(0, titleRowIndex); rowIndex < endRowIndex; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = table.getRows().get(rowIndex);
            if (row == null || row.size() != 2) {
                continue;
            }
            MesProBatchRecordParsedCell sideCell = row.get(0);
            MesProBatchRecordParsedCell wideCell = row.get(1);
            String wideText = parsedText(wideCell);
            List<String> lines = wideText.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
            if (Math.max(1, sideCell.getRowSpan()) >= 4
                    && Math.max(1, wideCell.getColSpan()) >= columnCount - Math.max(8, columnCount / 20)
                    && lines.size() >= 10
                    && isRepeatedHeaderTextPattern(lines.subList(0, Math.min(6, lines.size())))) {
                return true;
            }
        }
        return false;
    }

    private static boolean topMaterialMatrixRowHasProcessSideHeader(MesProBatchRecordParsedTable table) {
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            if (!isExpandedMaterialHeaderRow(row)) {
                continue;
            }
            String firstText = normalizeParsedText(row.get(0).getText());
            return firstText.endsWith("生产操作及自检记录");
        }
        return false;
    }

    private static boolean topMaterialMatrixRowHasPreviousChecklistSideHeader(MesProBatchRecordParsedTable table) {
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            if (!isExpandedMaterialHeaderRow(row)) {
                continue;
            }
            String firstText = normalizeParsedText(row.get(0).getText());
            return firstText.contains("生产前检查记录");
        }
        return false;
    }

    private static boolean topMaterialMatrixSideHeaderStartsAtVisualColumnZero(MesProBatchRecordParsedTable table) {
        PlacedParsedCell cell = findFirstExpandedMaterialHeaderSideCell(table);
        return cell != null
                && cell.columnIndex() == 0
                && normalizeParsedText(cell.cell().getText()).endsWith("生产操作及自检记录");
    }

    private static PlacedParsedCell findFirstExpandedMaterialHeaderSideCell(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return null;
        }
        for (PlacedParsedCell placed : placeParsedCells(table)) {
            List<MesProBatchRecordParsedCell> row = table.getRows().get(placed.rowIndex());
            if (isExpandedMaterialHeaderRow(row) && row.get(0) == placed.cell()) {
                return placed;
            }
        }
        return null;
    }

    private static List<PlacedParsedCell> placeParsedCells(MesProBatchRecordParsedTable table) {
        int columnCount = Math.max(1, table.getColumnCount());
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        List<PlacedParsedCell> placedCells = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            int columnIndex = 0;
            for (MesProBatchRecordParsedCell cell : table.getRows().get(rowIndex)) {
                if (cell.getColumnIndex() != null) {
                    columnIndex = Math.max(0, cell.getColumnIndex());
                }
                while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                    columnIndex++;
                }
                int colSpan = Math.max(1, cell.getColSpan());
                placedCells.add(new PlacedParsedCell(rowIndex, columnIndex, cell));
                if (cell.getRowSpan() > 1) {
                    for (int offset = 0; offset < colSpan; offset++) {
                        blockedUntilRowByColumn.put(columnIndex + offset, rowIndex + cell.getRowSpan() - 1);
                    }
                }
                columnIndex += colSpan;
                if (columnIndex > columnCount * 2) {
                    break;
                }
            }
        }
        return placedCells;
    }

    private static boolean isExpandedMaterialHeaderRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 7) {
            return false;
        }
        List<String> texts = row.stream()
                .map(cell -> normalizeParsedText(cell.getText()))
                .toList();
        return texts.contains("物料编码")
                && texts.contains("物料名称")
                && texts.contains("批号")
                && texts.stream().filter("物料编码"::equals).count() >= 2
                && texts.stream().filter("物料名称"::equals).count() >= 2
                && texts.stream().filter("批号"::equals).count() >= 2;
    }

    private static String normalizeParsedText(String text) {
        return text == null ? "" : text.replace("\n", "").replace(" ", "").trim();
    }

    private static int findParsedTitleRowIndex(MesProBatchRecordParsedTable table) {
        String title = table.getTableTitle();
        if (title == null || title.isBlank() || table.getRows() == null) {
            return 0;
        }
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = table.getRows().get(rowIndex);
            if (row != null && row.stream().anyMatch(cell -> parsedText(cell).contains(title))) {
                return rowIndex;
            }
        }
        return 0;
    }

    private static String parsedText(MesProBatchRecordParsedCell cell) {
        return cell == null || cell.getText() == null ? "" : cell.getText().trim();
    }

    private static boolean isRepeatedHeaderTextPattern(List<String> texts) {
        if (texts == null || texts.size() < 6) {
            return false;
        }
        List<String> normalized = texts.stream()
                .limit(6)
                .map(text -> text == null ? "" : text.replaceAll("\\s+", ""))
                .toList();
        return !normalized.get(0).isBlank()
                && !normalized.get(1).isBlank()
                && !normalized.get(2).isBlank()
                && normalized.get(0).equals(normalized.get(3))
                && normalized.get(1).equals(normalized.get(4))
                && normalized.get(2).equals(normalized.get(5));
    }

    private static String describeParsedRows(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null) {
            return "[]";
        }
        int maxRows = Math.min(rows.size(), 14);
        List<String> descriptions = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < maxRows; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null) {
                descriptions.add("r" + rowIndex + "=[]");
                continue;
            }
            descriptions.add("r" + rowIndex + "=" + row.stream()
                    .map(cell -> "[rs=" + Math.max(1, cell.getRowSpan())
                            + ",cs=" + Math.max(1, cell.getColSpan())
                            + ",text=" + parsedText(cell).replace("\n", "/") + "]")
                    .toList());
        }
        return descriptions.toString();
    }

    private static String describeVisualPlacement(MesProBatchRecordParsedTable table) {
        if (table == null) {
            return "[]";
        }
        List<String> descriptions = new ArrayList<>();
        int endRowIndex = Math.min(table.getRows().size(), findParsedTitleRowIndex(table) + 14);
        for (PlacedParsedCell placed : placeParsedCells(table)) {
            if (placed.rowIndex() > endRowIndex) {
                break;
            }
            String text = parsedText(placed.cell()).replace("\n", "/");
            if (!text.contains("生产前检查记录")
                    && !text.contains("生产操作及自检记录")
                    && !text.contains("物料编码")
                    && !text.contains("物料名称")
                    && !text.contains("批号")) {
                continue;
            }
            descriptions.add("r" + placed.rowIndex()
                    + "c" + placed.columnIndex()
                    + "[rs=" + Math.max(1, placed.cell().getRowSpan())
                    + ",cs=" + Math.max(1, placed.cell().getColSpan())
                    + ",text=" + text + "]");
        }
        return descriptions.toString();
    }

    private static boolean jsonTopMaterialMatrixSideHeaderStartsAtColumnZero(JSONObject root) {
        JSONObject row = findJsonTopMaterialMatrixHeaderRow(root);
        if (row == null) {
            return false;
        }
        JSONObject cells = row.getJSONObject("cells");
        if (cells == null) {
            return false;
        }
        JSONObject firstCell = cells.getJSONObject("0");
        return firstCell != null
                && normalizeParsedText(firstCell.getString("text")).endsWith("生产操作及自检记录");
    }

    private static JSONObject findJsonTopMaterialMatrixHeaderRow(JSONObject root) {
        if (root == null) {
            return null;
        }
        JSONObject rowsObject = root.getJSONObject("rows");
        if (rowsObject == null) {
            return null;
        }
        List<Integer> rowIndexes = rowsObject.keySet().stream()
                .filter(key -> key != null && key.chars().allMatch(Character::isDigit))
                .map(Integer::parseInt)
                .sorted()
                .toList();
        for (Integer rowIndex : rowIndexes) {
            JSONObject rowObject = rowsObject.getJSONObject(String.valueOf(rowIndex));
            JSONObject cells = rowObject == null ? null : rowObject.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            List<String> texts = cells.keySet().stream()
                    .filter(key -> key != null && key.chars().allMatch(Character::isDigit))
                    .sorted(Comparator.comparingInt(Integer::parseInt))
                    .map(key -> normalizeParsedText(cells.getJSONObject(key).getString("text")))
                    .toList();
            if (texts.stream().filter("物料编码"::equals).count() >= 2
                    && texts.stream().filter("物料名称"::equals).count() >= 2
                    && texts.stream().filter("批号"::equals).count() >= 2) {
                return rowObject;
            }
        }
        return null;
    }

    private static String describeJsonMaterialMatrixPlacement(JSONObject root) {
        JSONObject row = findJsonTopMaterialMatrixHeaderRow(root);
        if (row == null) {
            return "material header row not found";
        }
        JSONObject cells = row.getJSONObject("cells");
        List<String> descriptions = new ArrayList<>();
        for (String key : cells.keySet().stream()
                .filter(item -> item != null && item.chars().allMatch(Character::isDigit))
                .sorted(Comparator.comparingInt(Integer::parseInt))
                .toList()) {
            JSONObject cell = cells.getJSONObject(key);
            String text = cell == null ? "" : cell.getString("text");
            if (text == null || text.isBlank()) {
                continue;
            }
            descriptions.add("c" + key + "[merge=" + (cell.getJSONArray("merge") == null
                    ? "[]"
                    : cell.getJSONArray("merge").toJSONString())
                    + ",text=" + text.replace("\n", "/") + "]");
        }
        return descriptions.toString();
    }

    private static boolean jsonOperationRecordTailHasStableVisualColumns(JSONObject root) {
        Integer headerRowIndex = findJsonOperationRecordTailHeaderRowIndex(root);
        if (headerRowIndex == null) {
            return false;
        }
        JSONObject rowsObject = root.getJSONObject("rows");
        JSONObject row = rowsObject.getJSONObject(String.valueOf(headerRowIndex));
        JSONObject bodyRow = rowsObject.getJSONObject(String.valueOf(headerRowIndex + 1));
        JSONObject cells = row.getJSONObject("cells");
        JSONObject bodyCells = bodyRow == null ? null : bodyRow.getJSONObject("cells");
        if (cells == null || bodyCells == null) {
            return false;
        }
        for (String key : cells.keySet().stream()
                .filter(item -> item != null && item.chars().allMatch(Character::isDigit))
                .sorted(Comparator.comparingInt(Integer::parseInt))
                .toList()) {
            JSONObject cell = cells.getJSONObject(key);
            String text = normalizeParsedText(cell == null ? "" : cell.getString("text"));
            if (!isOperationRecordTailHeader(text)) {
                continue;
            }
            int columnIndex = Integer.parseInt(key);
            int colSpan = jsonColSpan(cell);
            if (colSpan <= 1) {
                continue;
            }
            if (!headerOrBodyCoversOperationRecordTailRange(cell, bodyCells, columnIndex, colSpan)) {
                return false;
            }
        }
        return true;
    }

    private static JSONObject findJsonOperationRecordTailHeaderRow(JSONObject root) {
        Integer rowIndex = findJsonOperationRecordTailHeaderRowIndex(root);
        if (rowIndex == null || root == null || root.getJSONObject("rows") == null) {
            return null;
        }
        return root.getJSONObject("rows").getJSONObject(String.valueOf(rowIndex));
    }

    private static Integer findJsonOperationRecordTailHeaderRowIndex(JSONObject root) {
        if (root == null) {
            return null;
        }
        JSONObject rowsObject = root.getJSONObject("rows");
        if (rowsObject == null) {
            return null;
        }
        for (Integer rowIndex : rowsObject.keySet().stream()
                .filter(key -> key != null && key.chars().allMatch(Character::isDigit))
                .map(Integer::parseInt)
                .sorted()
                .toList()) {
            JSONObject rowObject = rowsObject.getJSONObject(String.valueOf(rowIndex));
            JSONObject cells = rowObject == null ? null : rowObject.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            List<String> texts = cells.keySet().stream()
                    .filter(key -> key != null && key.chars().allMatch(Character::isDigit))
                    .map(key -> normalizeParsedText(cells.getJSONObject(key).getString("text")))
                    .toList();
            long tailHeaders = texts.stream()
                    .filter(MesProBatchRecordJingxiTableStructureVerificationTest::isOperationRecordTailHeader)
                    .count();
            boolean hasRunningOrProduction = texts.stream().anyMatch(text -> text.contains("运行次数"))
                    || texts.stream().anyMatch(text -> text.contains("生产数量"));
            if (tailHeaders >= 4 && hasRunningOrProduction) {
                return rowIndex;
            }
        }
        return null;
    }

    private static boolean headerOrBodyCoversOperationRecordTailRange(JSONObject headerCell, JSONObject bodyCells,
                                                                      int startColumn, int colSpan) {
        return headerCellHasVerticalCoverage(headerCell)
                || bodyBlankRangeIsSingleMergedCell(bodyCells, startColumn, colSpan);
    }

    private static boolean headerCellHasVerticalCoverage(JSONObject headerCell) {
        if (headerCell == null || headerCell.getJSONArray("merge") == null) {
            return false;
        }
        return headerCell.getJSONArray("merge").getIntValue(0) >= 1;
    }

    private static boolean bodyBlankRangeIsSingleMergedCell(JSONObject bodyCells, int startColumn, int colSpan) {
        if (bodyRangeHasVisibleCell(bodyCells, startColumn, colSpan)) {
            return true;
        }
        JSONObject startCell = bodyCells.getJSONObject(String.valueOf(startColumn));
        if (startCell == null || !normalizeParsedText(startCell.getString("text")).isBlank()
                || jsonColSpan(startCell) != colSpan) {
            return false;
        }
        for (int offset = 1; offset < colSpan; offset++) {
            JSONObject internalCell = bodyCells.getJSONObject(String.valueOf(startColumn + offset));
            if (internalCell != null) {
                return false;
            }
        }
        return true;
    }

    private static boolean bodyRangeHasVisibleCell(JSONObject bodyCells, int startColumn, int colSpan) {
        for (int offset = 0; offset < colSpan; offset++) {
            JSONObject cell = bodyCells.getJSONObject(String.valueOf(startColumn + offset));
            if (cell != null && !normalizeParsedText(cell.getString("text")).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static int jsonColSpan(JSONObject cell) {
        if (cell == null || cell.getJSONArray("merge") == null) {
            return 1;
        }
        return Math.max(1, cell.getJSONArray("merge").getIntValue(1) + 1);
    }

    private static boolean isOperationRecordTailHeader(String text) {
        String normalized = normalizeParsedText(text);
        return normalized.contains("生产数量")
                || normalized.contains("自检合格数量")
                || normalized.contains("不合格数量")
                || normalized.equals("操作人")
                || normalized.equals("复核人")
                || normalized.contains("运行次数");
    }

    private static String describeOperationRecordTail(JSONObject root) {
        JSONObject row = findJsonOperationRecordTailHeaderRow(root);
        if (row == null) {
            return "operation record tail header row not found";
        }
        JSONObject cols = root == null ? null : root.getJSONObject("cols");
        JSONObject rowsObject = root == null ? null : root.getJSONObject("rows");
        Integer headerRowIndex = findJsonOperationRecordTailHeaderRowIndex(root);
        JSONObject bodyCells = rowsObject == null || headerRowIndex == null
                ? null
                : rowsObject.getJSONObject(String.valueOf(headerRowIndex + 1)).getJSONObject("cells");
        JSONObject cells = row.getJSONObject("cells");
        List<String> descriptions = new ArrayList<>();
        for (String key : cells.keySet().stream()
                .filter(item -> item != null && item.chars().allMatch(Character::isDigit))
                .sorted(Comparator.comparingInt(Integer::parseInt))
                .toList()) {
            JSONObject cell = cells.getJSONObject(key);
            String text = normalizeParsedText(cell == null ? "" : cell.getString("text"));
            if (!isOperationRecordTailHeader(text)) {
                continue;
            }
            int columnIndex = Integer.parseInt(key);
            int colSpan = jsonColSpan(cell);
            List<Integer> widths = new ArrayList<>();
            for (int offset = 0; offset < colSpan; offset++) {
                JSONObject col = cols == null ? null : cols.getJSONObject(String.valueOf(columnIndex + offset));
                widths.add(col == null ? 0 : col.getIntValue("width"));
            }
            JSONObject bodyCell = bodyCells == null ? null : bodyCells.getJSONObject(String.valueOf(columnIndex));
            descriptions.add("c" + key + "[cs=" + colSpan + ",widths=" + widths
                    + ",bodyCs=" + jsonColSpan(bodyCell)
                    + ",bodyText=" + (bodyCell == null ? "<missing>" : normalizeParsedText(bodyCell.getString("text")))
                    + ",text=" + text + "]");
        }
        return descriptions.toString();
    }

    private static boolean jsonLeftSectionColumnHasVisibleWidth(JSONObject root) {
        JSONObject cols = root == null ? null : root.getJSONObject("cols");
        JSONObject firstCol = cols == null ? null : cols.getJSONObject("0");
        return firstCol != null && firstCol.getIntValue("width") >= 24;
    }

    private static String describeJsonColumnWidths(JSONObject root) {
        JSONObject cols = root == null ? null : root.getJSONObject("cols");
        if (cols == null) {
            return "cols missing";
        }
        List<String> widths = new ArrayList<>();
        for (String key : cols.keySet().stream()
                .filter(item -> item != null && item.chars().allMatch(Character::isDigit))
                .sorted(Comparator.comparingInt(Integer::parseInt))
                .limit(12)
                .toList()) {
            JSONObject col = cols.getJSONObject(key);
            widths.add("c" + key + "=" + (col == null ? "null" : col.getIntValue("width")));
        }
        return widths.toString();
    }

    private static String tableText(Table table) {
        StringBuilder builder = new StringBuilder();
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            builder.append(rowText(table.getRow(rowIndex)));
        }
        return builder.toString();
    }

    private static String rowText(TableRow row) {
        StringBuilder builder = new StringBuilder();
        for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
            builder.append(normalizeCellText(row.getCell(cellIndex).text()));
        }
        return builder.toString();
    }

    private static boolean isMergedFollower(TableCell cell) {
        return (cell.isMerged() && !cell.isFirstMerged())
                || (cell.isVerticallyMerged() && !cell.isFirstVerticallyMerged());
    }

    private static int resolveColSpan(TableRow row, int startCellIndex) {
        TableCell cell = row.getCell(startCellIndex);
        if (!cell.isFirstMerged()) {
            return 1;
        }
        int span = 1;
        for (int index = startCellIndex + 1; index < row.numCells(); index++) {
            TableCell nextCell = row.getCell(index);
            if (nextCell.isMerged() && !nextCell.isFirstMerged()) {
                span++;
                continue;
            }
            break;
        }
        return span;
    }

    private static int resolveRowSpan(Table table, int rowIndex, int cellIndex) {
        TableCell cell = table.getRow(rowIndex).getCell(cellIndex);
        if (!cell.isFirstVerticallyMerged()) {
            return 1;
        }
        int span = 1;
        for (int nextRowIndex = rowIndex + 1; nextRowIndex < table.numRows(); nextRowIndex++) {
            TableRow nextRow = table.getRow(nextRowIndex);
            if (cellIndex >= nextRow.numCells()) {
                break;
            }
            TableCell nextCell = nextRow.getCell(cellIndex);
            if (nextCell.isVerticallyMerged() && !nextCell.isFirstVerticallyMerged()) {
                span++;
                continue;
            }
            break;
        }
        return span;
    }

    private static int toPixels(int twips, int defaultValue) {
        if (twips <= 0) {
            return defaultValue;
        }
        return Math.max(defaultValue, Math.round(twips / 15.0f));
    }

    private static int toWidthUnits(int twips) {
        if (twips <= 0) {
            return 1;
        }
        return Math.max(1, Math.round(twips / 15.0f));
    }

    private static String normalizeCellText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('\u0007', ' ')
                .replace('\u0008', ' ')
                .replace('\r', '\n')
                .replace('\u0000', ' ')
                .replaceAll("[\\n]{3,}", "\n\n")
                .trim();
    }

    private static String resolveBorderStyle(BorderCode borderCode) {
        if (borderCode == null || borderCode.isEmpty()) {
            return null;
        }
        int lineWidth = borderCode.getLineWidth();
        if (lineWidth >= 24) {
            return "thick";
        }
        if (lineWidth >= 12) {
            return "medium";
        }
        return "thin";
    }

    private record ColumnSpanWidth(int columnIndex, int colSpan, int widthPx) {
    }

    private record PlacedParsedCell(int rowIndex, int columnIndex, MesProBatchRecordParsedCell cell) {
    }

    private record PackedShapeMatrix(ShapeCell sideCell, ShapeCell packedCell, List<String> headerTexts,
                                     List<String> itemNames) {
    }

    private record ShapeCell(int rowIndex, int columnIndex, int rowSpan, int colSpan, String text,
                             BorderShape border) {

        private String compact() {
            return "r" + rowIndex + "c" + columnIndex + "[rs=" + rowSpan + ",cs=" + colSpan
                    + ",text=" + (text == null || text.isBlank() ? "" : text.replace("\n", "/")) + "]";
        }
    }

    private record BorderShape(String top, String bottom, String left, String right) {

        private static BorderShape empty() {
            return new BorderShape(null, null, null, null);
        }

        private String compact() {
            return "top=" + top + ",bottom=" + bottom + ",left=" + left + ",right=" + right;
        }

        private boolean isComplete() {
            return top != null && bottom != null && left != null && right != null;
        }
    }

    private record WordTableShape(int rowCount, int columnCount, int tableWidth, List<Integer> columnWidths,
                                  List<Integer> rowHeights, List<List<ShapeCell>> rows) {

        private String describe() {
            return "rows=" + rowCount + ", cols=" + columnCount + ", tableWidth=" + tableWidth
                    + ", widths=" + columnWidths
                    + ", rowCellCounts=" + rows.stream().map(List::size).toList()
                    + ", rowHeights=" + rowHeights;
        }
    }

    private record JsonTableShape(int rowCount, int columnCount, int tableWidth, List<Integer> columnWidths,
                                  List<Integer> rowHeights, List<List<ShapeCell>> rows) {

        private JsonTableShape segmentFrom(String title, int expectedRowCount) {
            int startIndex = -1;
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                if (rows.get(rowIndex).stream().anyMatch(cell -> cell.text() != null && cell.text().contains(title))) {
                    startIndex = rowIndex;
                    break;
                }
            }
            if (startIndex < 0) {
                throw new AssertionError("actual JSON segment not found: " + title);
            }
            int endIndex = Math.min(rows.size(), startIndex + expectedRowCount);
            List<List<ShapeCell>> segmentRows = new ArrayList<>();
            List<Integer> segmentRowHeights = new ArrayList<>();
            for (int rowIndex = startIndex; rowIndex < endIndex; rowIndex++) {
                int targetRowIndex = rowIndex - startIndex;
                List<ShapeCell> segmentRow = new ArrayList<>();
                for (ShapeCell cell : rows.get(rowIndex)) {
                    int clippedRowSpan = Math.min(cell.rowSpan(), endIndex - rowIndex);
                    segmentRow.add(new ShapeCell(targetRowIndex, cell.columnIndex(), clippedRowSpan,
                            cell.colSpan(), cell.text(), cell.border()));
                }
                segmentRows.add(segmentRow);
                segmentRowHeights.add(rowHeights.get(rowIndex));
            }
            return new JsonTableShape(segmentRows.size(), columnCount, tableWidth, columnWidths, segmentRowHeights, segmentRows);
        }

        private String describe() {
            return "rows=" + rowCount + ", cols=" + columnCount + ", tableWidth=" + tableWidth
                    + ", widths=" + columnWidths
                    + ", rowCellCounts=" + rows.stream().map(List::size).toList()
                    + ", rowHeights=" + rowHeights;
        }
    }
}
