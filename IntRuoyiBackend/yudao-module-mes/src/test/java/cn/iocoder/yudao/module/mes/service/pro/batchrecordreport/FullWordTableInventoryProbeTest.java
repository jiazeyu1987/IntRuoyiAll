
package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullWordTableInventoryProbeTest {

    private static final Path REAL_DOC = Path.of("C:\\Users\\BJB110\\Desktop\\2\\2\\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc");
    private static final Path OUTPUT = Path.of("D:\\ProjectPackage\\Int\\IntRuoyi\\doc\\tasks\\20260704-full-word-batch-record-table-consistency\\full-word-inventory-probe.txt");
    private static final List<String> EXPECTED_RENDER_TABLE_TITLES = List.of(
            "产品信息",
            "粗洗工序生产记录",
            "精洗工序生产记录",
            "清洗工序生产记录",
            "清洁工序生产记录",
            "组装Ⅰ工序生产记录",
            "光固Ⅰ工序生产记录",
            "硅化Ⅰ工序生产记录",
            "硅化Ⅱ工序生产记录",
            "组装Ⅱ工序生产记录",
            "检测工序生产记录",
            "光固Ⅱ工序生产记录",
            "单包装工序生产记录",
            "中包装工序生产记录",
            "大包装工序生产记录"
    );

    @Test
    void dumpFullWordInventoryProbe() throws Exception {
        byte[] bytes = Files.readAllBytes(REAL_DOC);
        List<String> lines = new ArrayList<>();
        List<TableSummary> wordTables = wordTables(bytes);
        List<MesProBatchRecordParsedTable> parserTables = new MesProBatchRecordDocParser().parse(bytes);
        lines.add("word_table_count=" + wordTables.size());
        lines.add("parser_table_count=" + parserTables.size());
        lines.add("--- WORD TABLES ---");
        for (TableSummary table : wordTables) {
            lines.add(table.toLine());
        }
        lines.add("--- PARSER TABLES ---");
        for (int i = 0; i < parserTables.size(); i++) {
            MesProBatchRecordParsedTable table = parserTables.get(i);
            int cellCount = table.getRows() == null ? 0 : table.getRows().stream().mapToInt(List::size).sum();
            int maxEnd = table.getRows() == null ? 0 : table.getRows().stream()
                    .flatMap(List::stream)
                    .mapToInt(cell -> (cell.getColumnIndex() == null ? 0 : cell.getColumnIndex()) + Math.max(1, cell.getColSpan()))
                    .max().orElse(0);
            lines.add(String.format("#%02d sourceIndex=%s title=%s rows=%s cols=%s widths=%d cells=%d maxEnd=%d first=%s",
                    i + 1,
                    table.getSourceTableIndex(), compact(table.getTableTitle()), table.getRowCount(), table.getColumnCount(),
                    table.getColumnWidths() == null ? 0 : table.getColumnWidths().size(), cellCount, maxEnd,
                    compact(firstText(table))));
        }
        Files.writeString(OUTPUT, String.join(System.lineSeparator(), lines), StandardCharsets.UTF_8);
        System.out.println(String.join(System.lineSeparator(), lines));
    }

    @Test
    void parseFullWordDoc_shouldPreserveTopLevelWordTableInventoryAcrossSplitTemplates() throws Exception {
        byte[] bytes = Files.readAllBytes(REAL_DOC);
        List<TableSummary> wordTables = wordTables(bytes);
        List<MesProBatchRecordParsedTable> parserTables = new MesProBatchRecordDocParser().parse(bytes);

        List<Integer> distinctTopLevelSourceIndexes = parserTables.stream()
                .map(MesProBatchRecordParsedTable::getSourceTopLevelTableIndex)
                .distinct()
                .toList();

        assertEquals(wordTables.size(), distinctTopLevelSourceIndexes.size(),
                "split templates must still be traceable to every original Word top-level table");
        assertEquals(List.of(1, 2, 3), distinctTopLevelSourceIndexes,
                "sourceTopLevelTableIndex must preserve original Word table order after parser splits templates");
        assertEquals(EXPECTED_RENDER_TABLE_TITLES, parserTables.stream()
                        .map(MesProBatchRecordParsedTable::getTableTitle)
                        .toList(),
                "full-document render table inventory must keep the expected order");

        assertSourceTableRange(parserTables, 1, 1, 1);
        assertSourceTableRange(parserTables, 2, 2, 4);
        assertSourceTableRange(parserTables, 3, 5, 15);
    }

    @Test
    void parseFullWordDoc_shouldExposeStableRenderedTableStructureInventory() throws Exception {
        byte[] bytes = Files.readAllBytes(REAL_DOC);
        List<MesProBatchRecordParsedTable> parserTables = new MesProBatchRecordDocParser().parse(bytes);

        assertEquals(EXPECTED_RENDER_TABLE_TITLES.size(), parserTables.size(),
                "the system render inventory must cover all split Word table segments");
        for (MesProBatchRecordParsedTable table : parserTables) {
            assertTrue(table.getRowCount() != null && table.getRowCount() > 0,
                    () -> table.getTableTitle() + " must preserve row count");
            assertTrue(table.getColumnCount() != null && table.getColumnCount() > 0,
                    () -> table.getTableTitle() + " must preserve logical or visual column count");
            assertEquals(table.getColumnCount().intValue(),
                    table.getColumnWidths() == null ? 0 : table.getColumnWidths().size(),
                    () -> table.getTableTitle() + " column width vector must align with declared column count");
            assertTrue(table.getRows() != null && !table.getRows().isEmpty(),
                    () -> table.getTableTitle() + " rows must not be empty");
        }
    }

    @Test
    void parseFullWordDoc_shouldPreserveProductInfoTopLevelTableCellsAndText() throws Exception {
        byte[] bytes = Files.readAllBytes(REAL_DOC);
        Table productInfoWordTable = topLevelWordTables(bytes).get(0);
        MesProBatchRecordParsedTable productInfo = new MesProBatchRecordDocParser().parse(bytes).get(0);

        assertEquals("产品信息", productInfo.getTableTitle());
        assertEquals(1, productInfo.getSourceTopLevelTableIndex().intValue());
        assertEquals(productInfoWordTable.numRows(), productInfo.getRowCount().intValue(),
                "product info rendered table must preserve the original Word row count");
        assertEquals(maxRowCellCount(productInfoWordTable), productInfo.getColumnCount().intValue(),
                "product info visual column count must follow the densest Word row, not internal source boundaries");
        assertEquals(productInfo.getColumnCount().intValue(), productInfo.getColumnWidths().size(),
                "product info width vector must align with visual column count");
        assertEquals(wordCellCount(productInfoWordTable), parsedCellCount(productInfo),
                "product info must preserve every Word cell as a rendered logical cell");

        for (int rowIndex = 0; rowIndex < productInfoWordTable.numRows(); rowIndex++) {
            final int currentRowIndex = rowIndex;
            TableRow wordRow = productInfoWordTable.getRow(rowIndex);
            List<MesProBatchRecordParsedCell> parsedRow = productInfo.getRows().get(rowIndex);
            assertEquals(wordRow.numCells(), parsedRow.size(),
                    () -> "product info row " + currentRowIndex + " must preserve Word cell count");
            for (int cellIndex = 0; cellIndex < wordRow.numCells(); cellIndex++) {
                final int currentCellIndex = cellIndex;
                assertEquals(normalize(wordRow.getCell(cellIndex).text()), normalize(parsedRow.get(cellIndex).getText()),
                        () -> "product info row " + currentRowIndex + " cell " + currentCellIndex
                                + " text placement mismatch");
            }
        }
    }

    @Test
    void renderProductInfoJson_shouldMatchOriginalWordVisualShape() throws Exception {
        byte[] bytes = Files.readAllBytes(REAL_DOC);
        Table productInfoWordTable = topLevelWordTables(bytes).get(0);
        MesProBatchRecordParsedTable productInfo = new MesProBatchRecordDocParser().parse(bytes).get(0);
        MesProBatchRecordParsedTable calibrated = new MesProBatchRecordReportLayoutCalibrator().calibrate(productInfo);
        JSONObject renderedJson = JSON.parseObject(new MesProBatchRecordReportJsonBuilder()
                .build(calibrated, "EBR_PRODUCT_INFO_VERIFY"));

        TableSegmentShape expected = toProductInfoVisualShape(productInfoWordTable);
        TableSegmentShape actual = extractProductInfoDataShape(renderedJson, expected);

        List<String> diffs = diffSegment("产品信息", expected, actual);
        assertTrue(diffs.isEmpty(), () -> "product info JSON visual shape mismatch:\n"
                + String.join("\n", diffs)
                + "\nexpected=" + describe(expected)
                + "\nactual=" + describe(actual));
    }

    private static void assertSourceTableRange(List<MesProBatchRecordParsedTable> parserTables,
                                               int sourceTopLevelTableIndex,
                                               int firstOutputTableIndex,
                                               int lastOutputTableIndex) {
        for (int index = firstOutputTableIndex; index <= lastOutputTableIndex; index++) {
            MesProBatchRecordParsedTable table = parserTables.get(index - 1);
            assertEquals(sourceTopLevelTableIndex, table.getSourceTopLevelTableIndex(),
                    "output table #" + index + " must keep original Word top-level source table");
            assertEquals(index, table.getSourceTableIndex(),
                    "sourceTableIndex remains the system render table order");
            assertEquals(index - firstOutputTableIndex + 1, table.getSourceSplitIndex(),
                    "sourceSplitIndex must be stable within the original Word table");
        }
    }

    private static List<TableSummary> wordTables(byte[] bytes) throws Exception {
        List<Table> tables = topLevelWordTables(bytes);
        List<TableSummary> result = new ArrayList<>();
        int index = 1;
        for (Table table : tables) {
            List<Integer> boundaries = boundaries(table);
            int cellCount = 0;
            int mergedFollowers = 0;
            int maxRowCells = 0;
            int nonBlank = 0;
            for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
                TableRow row = table.getRow(rowIndex);
                maxRowCells = Math.max(maxRowCells, row.numCells());
                for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                    TableCell cell = row.getCell(cellIndex);
                    cellCount++;
                    if (cell.isFirstMerged() || cell.isMerged()) {
                        mergedFollowers++;
                    }
                    if (!normalize(cell.text()).isBlank()) {
                        nonBlank++;
                    }
                }
            }
            result.add(new TableSummary(index++, table.numRows(), boundaries.size() > 1 ? boundaries.size() - 1 : maxRowCells,
                    maxRowCells, cellCount, nonBlank, mergedFollowers, firstText(table), tableTextHead(table)));
        }
        return result;
    }

    private static List<Table> topLevelWordTables(byte[] bytes) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes))) {
            Range range = document.getRange();
            Constructor<TableIterator> constructor = TableIterator.class.getDeclaredConstructor(Range.class, int.class);
            constructor.setAccessible(true);
            TableIterator iterator = constructor.newInstance(range, 1);
            List<Table> result = new ArrayList<>();
            while (iterator.hasNext()) {
                result.add(iterator.next());
            }
            return result;
        }
    }

    private static int maxRowCellCount(Table table) {
        int max = 0;
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            max = Math.max(max, table.getRow(rowIndex).numCells());
        }
        return max;
    }

    private static int wordCellCount(Table table) {
        int count = 0;
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            count += table.getRow(rowIndex).numCells();
        }
        return count;
    }

    private static int parsedCellCount(MesProBatchRecordParsedTable table) {
        return table.getRows() == null ? 0 : table.getRows().stream().mapToInt(List::size).sum();
    }

    private static TableSegmentShape toSegmentShape(Table table, String title) {
        int start = findSegmentStart(table, title);
        int end = findSegmentEnd(table, start + 1);
        return toTableShape(table, start, end);
    }

    private static TableSegmentShape toWholeTableShape(Table table) {
        return toTableShape(table, 0, table.numRows());
    }

    private static TableSegmentShape toProductInfoVisualShape(Table table) {
        int columnCount = maxRowCellCount(table);
        List<Integer> columnWidths = logicalColumnWidths(table, 0, table.numRows(), columnCount);
        List<List<CellShape>> rows = new ArrayList<>();
        List<Integer> rowHeights = new ArrayList<>();
        for (int sourceRowIndex = 0; sourceRowIndex < table.numRows(); sourceRowIndex++) {
            TableRow sourceRow = table.getRow(sourceRowIndex);
            List<CellShape> row = new ArrayList<>();
            int runningColumn = 0;
            for (int cellIndex = 0; cellIndex < sourceRow.numCells(); cellIndex++) {
                TableCell cell = sourceRow.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                int remainingCells = sourceRow.numCells() - cellIndex;
                int availableColumns = Math.max(1, columnCount - runningColumn);
                int colSpan = resolveProductInfoColSpan(cell, columnWidths, runningColumn,
                        Math.max(1, availableColumns - Math.max(0, remainingCells - 1)));
                row.add(new CellShape(sourceRowIndex, runningColumn, 1, colSpan, normalize(cell.text())));
                runningColumn += colSpan;
            }
            rows.add(row);
            rowHeights.add(toPixels(sourceRow.getRowHeight(), 36));
        }
        return new TableSegmentShape(rows.size(), columnCount, normalizeWidthVector(columnWidths), rowHeights, rows);
    }

    private static int resolveProductInfoColSpan(TableCell cell, List<Integer> columnWidths,
                                                 int startColumn, int maxSpan) {
        if (columnWidths == null || columnWidths.isEmpty() || startColumn >= columnWidths.size()) {
            return 1;
        }
        int sourceWidth = toWidthUnits(cell.getWidth());
        int accumulated = 0;
        int span = 0;
        for (int columnIndex = startColumn; columnIndex < columnWidths.size() && span < maxSpan; columnIndex++) {
            accumulated += Math.max(1, columnWidths.get(columnIndex));
            span++;
            if (accumulated >= sourceWidth * 0.85f) {
                break;
            }
        }
        return Math.max(1, span);
    }

    private static TableSegmentShape toTableShape(Table table, int start, int end) {
        List<Integer> boundaries = shouldUseBoundaryGrid(table, start, end)
                ? boundaries(table, start, end)
                : List.of();
        boolean boundaryGrid = !boundaries.isEmpty();
        List<Integer> visualColumnWidths = boundaryGrid
                ? boundaryWidths(boundaries)
                : logicalColumnWidths(table, start, end, resolveLogicalColumnCount(table, start, end));
        int totalVisualWidth = visualColumnWidths.stream().mapToInt(Integer::intValue).sum();
        int[] blockedUntilRowByColumn = new int[4096];
        for (int index = 0; index < blockedUntilRowByColumn.length; index++) {
            blockedUntilRowByColumn[index] = -1;
        }
        List<List<CellShape>> rows = new ArrayList<>();
        List<Integer> rowHeights = new ArrayList<>();
        int maxColumn = 0;
        for (int sourceRowIndex = start; sourceRowIndex < end; sourceRowIndex++) {
            TableRow row = table.getRow(sourceRowIndex);
            int outputRowIndex = sourceRowIndex - start;
            List<CellShape> outputRow = new ArrayList<>();
            int runningColumn = 0;
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                while (!boundaryGrid && runningColumn < blockedUntilRowByColumn.length
                        && blockedUntilRowByColumn[runningColumn] >= outputRowIndex) {
                    runningColumn++;
                }
                int columnIndex = boundaryGrid ? boundaryStart(boundaries, cell) : runningColumn;
                int colSpan = boundaryGrid
                        ? boundarySpan(boundaries, cell, columnIndex)
                        : visualColSpan(cell, visualColumnWidths, columnIndex, totalVisualWidth);
                int rowSpan = Math.min(resolveRowSpan(table, sourceRowIndex, cellIndex), end - sourceRowIndex);
                outputRow.add(new CellShape(outputRowIndex, columnIndex, rowSpan, colSpan, normalize(cell.text())));
                if (rowSpan > 1) {
                    for (int offset = 0; offset < colSpan && columnIndex + offset < blockedUntilRowByColumn.length; offset++) {
                        blockedUntilRowByColumn[columnIndex + offset] = outputRowIndex + rowSpan - 1;
                    }
                }
                runningColumn = columnIndex + colSpan;
                maxColumn = Math.max(maxColumn, columnIndex + colSpan);
            }
            rows.add(outputRow);
            rowHeights.add(toPixels(row.getRowHeight(), 36));
        }
        return new TableSegmentShape(rows.size(), Math.max(maxColumn, visualColumnWidths.size()), normalizeWidthVector(visualColumnWidths),
                rowHeights, rows);
    }

    private static TableSegmentShape toJsonShape(JSONObject root) {
        JSONObject rowsObject = root.getJSONObject("rows");
        JSONObject colsObject = root.getJSONObject("cols");
        List<Integer> columnWidths = new ArrayList<>();
        if (colsObject != null) {
            for (String key : colsObject.keySet().stream()
                    .filter(item -> item != null && item.chars().allMatch(Character::isDigit))
                    .sorted(Comparator.comparingInt(Integer::parseInt))
                    .toList()) {
                columnWidths.add(colsObject.getJSONObject(key).getIntValue("width"));
            }
        }
        List<List<CellShape>> rows = new ArrayList<>();
        List<Integer> rowHeights = new ArrayList<>();
        int maxColumn = 0;
        if (rowsObject != null) {
            for (String rowKey : rowsObject.keySet().stream()
                    .filter(item -> item != null && item.chars().allMatch(Character::isDigit))
                    .sorted(Comparator.comparingInt(Integer::parseInt))
                    .toList()) {
                int rowIndex = Integer.parseInt(rowKey);
                JSONObject rowObject = rowsObject.getJSONObject(rowKey);
                rowHeights.add(rowObject == null ? 36 : rowObject.getIntValue("height"));
                JSONObject cellsObject = rowObject == null ? null : rowObject.getJSONObject("cells");
                List<CellShape> row = new ArrayList<>();
                if (cellsObject != null) {
                    for (String cellKey : cellsObject.keySet().stream()
                            .filter(item -> item != null && item.chars().allMatch(Character::isDigit))
                            .sorted(Comparator.comparingInt(Integer::parseInt))
                            .toList()) {
                        int columnIndex = Integer.parseInt(cellKey);
                        JSONObject cellObject = cellsObject.getJSONObject(cellKey);
                        int rowSpan = 1;
                        int colSpan = 1;
                        if (cellObject != null && cellObject.getJSONArray("merge") != null) {
                            rowSpan = Math.max(1, cellObject.getJSONArray("merge").getIntValue(0) + 1);
                            colSpan = Math.max(1, cellObject.getJSONArray("merge").getIntValue(1) + 1);
                        }
                        row.add(new CellShape(rowIndex, columnIndex, rowSpan, colSpan,
                                normalize(cellObject == null ? "" : cellObject.getString("text"))));
                        maxColumn = Math.max(maxColumn, columnIndex + colSpan);
                    }
                }
                rows.add(row);
            }
        }
        return new TableSegmentShape(rows.size(), Math.max(maxColumn, columnWidths.size()),
                normalizeWidthVector(columnWidths), rowHeights, rows);
    }

    private static TableSegmentShape extractProductInfoDataShape(JSONObject root, TableSegmentShape expected) {
        TableSegmentShape rendered = toJsonShape(root);
        List<List<CellShape>> dataRows = new ArrayList<>();
        List<Integer> dataHeights = new ArrayList<>();
        int expectedRowIndex = 0;
        for (int rowIndex = 0; rowIndex < rendered.rows().size() && expectedRowIndex < expected.rows().size(); rowIndex++) {
            List<CellShape> row = rendered.rows().get(rowIndex);
            if (isDocumentHeaderDecorationRow(row) || isPagingSpacerRow(row)) {
                continue;
            }
            List<CellShape> expectedRow = expected.rows().get(expectedRowIndex);
            if (!matchesRowTextSignature(expectedRow, row)) {
                continue;
            }
            dataRows.add(reindexRow(row, expectedRowIndex));
            dataHeights.add(rendered.rowHeights().get(rowIndex));
            expectedRowIndex++;
        }
        assertEquals(expected.rows().size(), dataRows.size(),
                () -> "product info JSON data rows must be recoverable after removing print decorations, actualRows="
                        + dataRows.stream().map(FullWordTableInventoryProbeTest::compactCells).toList());
        return new TableSegmentShape(dataRows.size(), rendered.columnCount(), rendered.columnWidths(), dataHeights, dataRows);
    }

    private static boolean matchesRowTextSignature(List<CellShape> expectedRow, List<CellShape> actualRow) {
        List<String> expectedTexts = expectedRow.stream()
                .map(CellShape::text)
                .map(FullWordTableInventoryProbeTest::normalize)
                .toList();
        List<String> actualTexts = actualRow.stream()
                .map(CellShape::text)
                .map(FullWordTableInventoryProbeTest::normalize)
                .toList();
        return expectedTexts.equals(actualTexts);
    }

    private static boolean isDocumentHeaderDecorationRow(List<CellShape> row) {
        String text = normalize(row == null ? "" : row.stream().map(CellShape::text).reduce("", (left, right) -> left + " " + right));
        return text.contains("球囊扩张压力泵生产记录")
                || text.contains("记录编号 RE-PP-ID-01")
                || text.contains("版本 A/1");
    }

    private static boolean isPagingSpacerRow(List<CellShape> row) {
        return row != null && row.size() == 1 && normalize(row.get(0).text()).isBlank()
                && row.get(0).colSpan() >= 2;
    }

    private static List<CellShape> reindexRow(List<CellShape> row, int rowIndex) {
        List<CellShape> result = new ArrayList<>();
        for (CellShape cell : row) {
            result.add(new CellShape(rowIndex, cell.columnIndex(), cell.rowSpan(), cell.colSpan(), cell.text()));
        }
        return result;
    }

    private static TableSegmentShape toSegmentShape(MesProBatchRecordParsedTable table) {
        List<List<CellShape>> rows = new ArrayList<>();
        List<Integer> rowHeights = new ArrayList<>();
        int maxColumn = 0;
        if (table.getRows() != null) {
            for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
                List<MesProBatchRecordParsedCell> sourceRow = table.getRows().get(rowIndex);
                List<CellShape> row = new ArrayList<>();
                int rowHeight = 36;
                for (MesProBatchRecordParsedCell cell : sourceRow) {
                    int columnIndex = cell.getColumnIndex() == null ? maxColumn : cell.getColumnIndex();
                    int rowSpan = Math.max(1, cell.getRowSpan());
                    int colSpan = Math.max(1, cell.getColSpan());
                    row.add(new CellShape(rowIndex, columnIndex, rowSpan, colSpan, normalize(cell.getText())));
                    maxColumn = Math.max(maxColumn, columnIndex + colSpan);
                    rowHeight = Math.max(rowHeight, cell.getHeightPx());
                }
                rows.add(row);
                rowHeights.add(rowHeight);
            }
        }
        return new TableSegmentShape(rows.size(), Math.max(maxColumn, table.getColumnCount() == null ? 0 : table.getColumnCount()),
                normalizeWidthVector(table.getColumnWidths() == null ? List.of() : table.getColumnWidths()), rowHeights, rows);
    }

    private static List<String> diffSegment(String title, TableSegmentShape expected, TableSegmentShape actual) {
        List<String> diffs = new ArrayList<>();
        String prefix = title + ": ";
        if (expected.rowCount() != actual.rowCount()) {
            diffs.add(prefix + "rowCount expected=" + expected.rowCount() + ", actual=" + actual.rowCount());
        }
        if (expected.columnCount() != actual.columnCount()) {
            diffs.add(prefix + "columnCount expected=" + expected.columnCount() + ", actual=" + actual.columnCount());
        }
        if (!sameWidthVector(expected.columnWidths(), actual.columnWidths())) {
            diffs.add(prefix + "columnWidths expected=" + expected.columnWidths() + ", actual=" + actual.columnWidths());
        }
        if (!sameHeightVector(expected.rowHeights(), actual.rowHeights())) {
            diffs.add(prefix + "rowHeights expected=" + expected.rowHeights() + ", actual=" + actual.rowHeights());
        }
        int comparableRows = Math.min(expected.rows().size(), actual.rows().size());
        for (int rowIndex = 0; rowIndex < comparableRows; rowIndex++) {
            List<CellShape> expectedRow = expected.rows().get(rowIndex);
            List<CellShape> actualRow = actual.rows().get(rowIndex);
            if (expectedRow.size() != actualRow.size()) {
                diffs.add(prefix + "row " + rowIndex + " cellCount expected=" + expectedRow.size()
                        + ", actual=" + actualRow.size()
                        + ", expectedCells=" + compactCells(expectedRow)
                        + ", actualCells=" + compactCells(actualRow));
                continue;
            }
            for (int cellIndex = 0; cellIndex < expectedRow.size(); cellIndex++) {
                CellShape expectedCell = expectedRow.get(cellIndex);
                CellShape actualCell = actualRow.get(cellIndex);
                if (expectedCell.columnIndex() != actualCell.columnIndex()
                        || expectedCell.rowSpan() != actualCell.rowSpan()
                        || expectedCell.colSpan() != actualCell.colSpan()
                        || !expectedCell.text().equals(actualCell.text())) {
                    diffs.add(prefix + "row " + rowIndex + " cell " + cellIndex
                            + " expected=" + expectedCell.compact()
                            + ", actual=" + actualCell.compact());
                }
            }
        }
        return diffs;
    }

    private static int findSegmentStart(Table table, String title) {
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            if (rowText(table.getRow(rowIndex)).contains(title)) {
                return rowIndex;
            }
        }
        throw new AssertionError("source Word segment not found: " + title);
    }

    private static int findSegmentEnd(Table table, int startRowIndex) {
        for (int rowIndex = startRowIndex; rowIndex < table.numRows(); rowIndex++) {
            String text = rowText(table.getRow(rowIndex)).replace("\n", "");
            if (text.contains("工序生产记录")) {
                return rowIndex;
            }
        }
        return table.numRows();
    }

    private static boolean shouldUseBoundaryGrid(Table table, int start, int end) {
        List<Integer> boundaryList = boundaries(table, start, end);
        int boundaryColumns = Math.max(0, boundaryList.size() - 1);
        int maxRowCells = 0;
        boolean hasPackedInteriorGrid = false;
        boolean hasWideMergedVisualCell = false;
        for (int rowIndex = start; rowIndex < end; rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            maxRowCells = Math.max(maxRowCells, row.numCells());
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                int columnIndex = boundaryStart(boundaryList, cell);
                int span = boundarySpan(boundaryList, cell, columnIndex);
                if (span >= 20) {
                    hasWideMergedVisualCell = true;
                }
                if (span >= Math.max(20, Math.round(boundaryColumns * 0.35f)) && isPackedLabelGridText(cell.text())) {
                    hasPackedInteriorGrid = true;
                }
            }
        }
        return boundaryColumns >= 60
                && maxRowCells > 0
                && boundaryColumns >= maxRowCells * 3
                && hasPackedInteriorGrid
                && (hasWideMergedVisualCell || boundaryColumns >= maxRowCells * 6);
    }

    private static List<Integer> boundaries(Table table, int start, int end) {
        TreeSet<Integer> values = new TreeSet<>();
        for (int rowIndex = start; rowIndex < end; rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                values.add(cell.getLeftEdge());
                values.add(cell.getLeftEdge() + Math.max(1, cell.getWidth()));
            }
        }
        return values.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private static int boundaryStart(List<Integer> boundaries, TableCell cell) {
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < Math.max(0, boundaries.size() - 1); index++) {
            int distance = Math.abs(boundaries.get(index) - cell.getLeftEdge());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private static int boundarySpan(List<Integer> boundaries, TableCell cell, int startColumnIndex) {
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

    private static List<Integer> boundaryWidths(List<Integer> boundaries) {
        return IntStream.range(0, Math.max(0, boundaries.size() - 1))
                .mapToObj(index -> toWidthUnits(boundaries.get(index + 1) - boundaries.get(index)))
                .toList();
    }

    private static List<Integer> logicalColumnWidths(Table table, int start, int end, int columnCount) {
        int bestRow = start;
        int bestCells = -1;
        for (int rowIndex = start; rowIndex < end; rowIndex++) {
            int cells = table.getRow(rowIndex).numCells();
            if (cells > bestCells) {
                bestCells = cells;
                bestRow = rowIndex;
            }
        }
        TableRow row = table.getRow(bestRow);
        List<Integer> widths = new ArrayList<>();
        for (int cellIndex = 0; cellIndex < row.numCells() && widths.size() < columnCount; cellIndex++) {
            TableCell cell = row.getCell(cellIndex);
            if (!isMergedFollower(cell)) {
                widths.add(toWidthUnits(cell.getWidth()));
            }
        }
        while (widths.size() < columnCount) {
            widths.add(120);
        }
        return widths;
    }

    private static int resolveLogicalColumnCount(Table table, int start, int end) {
        int columnCount = 0;
        for (int rowIndex = start; rowIndex < end; rowIndex++) {
            columnCount = Math.max(columnCount, table.getRow(rowIndex).numCells());
        }
        return columnCount;
    }

    private static int visualColSpan(TableCell cell, List<Integer> visualColumnWidths,
                                     int startColumnIndex, int totalVisualWidth) {
        int width = toWidthUnits(cell.getWidth());
        if (startColumnIndex >= visualColumnWidths.size()) {
            return 1;
        }
        if (width >= Math.max(1, totalVisualWidth * 0.92f)) {
            return visualColumnWidths.size() - startColumnIndex;
        }
        int accumulated = 0;
        int span = 0;
        for (int columnIndex = startColumnIndex; columnIndex < visualColumnWidths.size(); columnIndex++) {
            accumulated += visualColumnWidths.get(columnIndex);
            span++;
            if (accumulated >= width * 0.85f) {
                break;
            }
        }
        return Math.max(1, Math.min(span, visualColumnWidths.size() - startColumnIndex));
    }

    private static boolean isPackedLabelGridText(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        long slashCount = normalized.chars().filter(ch -> ch == '/' || ch == '／').count();
        if (slashCount < 5) {
            return false;
        }
        int shortLabels = 0;
        for (String token : normalized.split("[\\n/、，,；;：:\\s]+")) {
            String trimmed = token.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 10) {
                shortLabels++;
            }
        }
        return shortLabels >= 6;
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

    private static boolean sameWidthVector(List<Integer> expected, List<Integer> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        for (int index = 0; index < expected.size(); index++) {
            if (Math.abs(expected.get(index) - actual.get(index)) > 120) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameHeightVector(List<Integer> expected, List<Integer> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        for (int index = 0; index < expected.size(); index++) {
            int e = expected.get(index);
            int a = actual.get(index);
            if (e <= 0 || a <= 0) {
                if (e != a) {
                    return false;
                }
                continue;
            }
            float ratio = a / (float) e;
            if (ratio < 0.85f || ratio > 1.25f) {
                return false;
            }
        }
        return true;
    }

    private static String rowText(TableRow row) {
        StringBuilder builder = new StringBuilder();
        for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
            builder.append(normalize(row.getCell(cellIndex).text()));
        }
        return builder.toString();
    }

    private static String compactCells(List<CellShape> cells) {
        return cells.stream().map(CellShape::compact).toList().toString();
    }

    private static String describe(TableSegmentShape shape) {
        return "rows=" + shape.rowCount()
                + ", cols=" + shape.columnCount()
                + ", widths=" + shape.columnWidths()
                + ", rowHeights=" + shape.rowHeights()
                + ", rowCellCounts=" + shape.rows().stream().map(List::size).toList();
    }

    private static List<Integer> boundaries(Table table) {
        TreeSet<Integer> values = new TreeSet<>();
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                values.add(cell.getLeftEdge());
                values.add(cell.getLeftEdge() + Math.max(1, cell.getWidth()));
            }
        }
        return values.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private static String firstText(Table table) {
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                String text = normalize(row.getCell(cellIndex).text());
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static String tableTextHead(Table table) {
        StringBuilder sb = new StringBuilder();
        for (int rowIndex = 0; rowIndex < Math.min(4, table.numRows()); rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                sb.append(' ').append(normalize(row.getCell(cellIndex).text()));
            }
        }
        return sb.toString();
    }

    private static String firstText(MesProBatchRecordParsedTable table) {
        if (table.getRows() == null) {
            return "";
        }
        return table.getRows().stream().flatMap(List::stream)
                .map(MesProBatchRecordParsedCell::getText)
                .filter(text -> text != null && !text.isBlank())
                .findFirst().orElse("");
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u0007', ' ').replace('\u0008', ' ').replace('\r', '\n').replace('\u0000', ' ')
                .replaceAll("[\\n]{3,}", "\n\n").trim();
    }

    private static String compact(String text) {
        String normalized = normalize(text).replace('\n', '/').replaceAll("\\s+", " ");
        return normalized.length() > 80 ? normalized.substring(0, 80) + "..." : normalized;
    }

    private record CellShape(int rowIndex, int columnIndex, int rowSpan, int colSpan, String text) {
        private String compact() {
            return "r" + rowIndex + "c" + columnIndex + "[rs=" + rowSpan + ",cs=" + colSpan
                    + ",text=" + FullWordTableInventoryProbeTest.compact(text) + "]";
        }
    }

    private record TableSegmentShape(int rowCount, int columnCount, List<Integer> columnWidths,
                                     List<Integer> rowHeights, List<List<CellShape>> rows) {
    }

    private record TableSummary(int index, int rows, int boundaryCols, int maxRowCells, int cells,
                                int nonBlankCells, int mergedMarkers, String firstText, String headText) {
        private String toLine() {
            return String.format("#%02d rows=%d boundaryCols=%d maxRowCells=%d cells=%d nonBlank=%d mergedMarkers=%d first=%s head=%s",
                    index, rows, boundaryCols, maxRowCells, cells, nonBlankCells, mergedMarkers, compact(firstText), compact(headText));
        }
    }
}
