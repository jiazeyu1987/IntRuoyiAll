package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordPressurePumpCellDiffReportTest {

    private static final Path REAL_DOC = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
    private static final String REPORT_RELATIVE =
            "doc/tasks/20260723-batch-assembly1-lightcure1-recognition/pressure-pump-assembly-lightcure-cell-diff-report.md";
    private static final List<String> TARGET_TITLES = List.of("组装Ⅰ工序生产记录", "光固Ⅰ工序生产记录");

    private final MesProBatchRecordRouteBRecognizer recognizer =
            new MesProBatchRecordRouteBRecognizer(TestBatchRecordFixtures.wordParser(),
                    "python", Path.of(System.getProperty("user.dir")), 600_000L);
    private final MesProBatchRecordReportLayoutCalibrator calibrator = new MesProBatchRecordReportLayoutCalibrator();
    private final MesProBatchRecordReportJsonBuilder jsonBuilder = new MesProBatchRecordReportJsonBuilder();

    @Test
    void pressurePumpAssemblyOneAndLightCureOne_shouldMatchOriginalWordCells() throws Exception {
        assertTrue(Files.exists(REAL_DOC), "required real DOC is missing: " + REAL_DOC);
        byte[] bytes = Files.readAllBytes(REAL_DOC);
        List<MesProBatchRecordParsedTable> recognizedTables = recognizer.recognize(
                REAL_DOC, bytes, REAL_DOC.getFileName().toString());

        List<TableDiffReport> reports = new ArrayList<>();
        for (String title : TARGET_TITLES) {
            TableShape expected = toWordSegmentShape(bytes, title);
            MesProBatchRecordParsedTable recognized = recognizedTables.stream()
                    .filter(table -> title.equals(table.getTableTitle()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("recognized table not found: " + title));
            MesProBatchRecordParsedTable calibrated = calibrator.calibrate(recognized);
            JSONObject json = JSON.parseObject(jsonBuilder.build(calibrated, "EBR_PRESSURE_DIFF_" + reports.size()));
            TableShape actual = extractRenderedSegmentShape(json, title, expected.rowCount());
            reports.add(diff(title, expected, actual, recognized, calibrated));
        }

        writeReport(reports);
        int maxScore = reports.stream()
                .flatMap(report -> report.diffs().stream())
                .mapToInt(CellDiff::score)
                .max()
                .orElse(0);
        assertTrue(maxScore == 0,
                () -> "pressure pump assembly/light-cure cell diff report has remaining differences, maxScore="
                        + maxScore + ", report=" + reportPath().toAbsolutePath());
    }

    private static TableShape toWordSegmentShape(byte[] bytes, String title) throws Exception {
        Method method = MesProBatchRecordJingxiTableStructureVerificationTest.class
                .getDeclaredMethod("extractWordTableShape", byte[].class, String.class);
        method.setAccessible(true);
        Object wordShape = method.invoke(null, bytes, title);
        return fromLegacyWordShape(wordShape);
    }

    private static TableShape fromLegacyWordShape(Object wordShape) throws Exception {
        int rowCount = (Integer) invoke(wordShape, "rowCount");
        int columnCount = (Integer) invoke(wordShape, "columnCount");
        @SuppressWarnings("unchecked")
        List<Integer> columnWidths = (List<Integer>) invoke(wordShape, "columnWidths");
        @SuppressWarnings("unchecked")
        List<Integer> rowHeights = (List<Integer>) invoke(wordShape, "rowHeights");
        @SuppressWarnings("unchecked")
        List<List<Object>> legacyRows = (List<List<Object>>) invoke(wordShape, "rows");
        List<List<CellShape>> rows = new ArrayList<>();
        for (List<Object> legacyRow : legacyRows) {
            List<CellShape> row = new ArrayList<>();
            for (Object legacyCell : legacyRow) {
                String text = normalize((String) invoke(legacyCell, "text"));
                row.add(new CellShape(
                        (Integer) invoke(legacyCell, "rowIndex"),
                        (Integer) invoke(legacyCell, "columnIndex"),
                        (Integer) invoke(legacyCell, "rowSpan"),
                        (Integer) invoke(legacyCell, "colSpan"),
                        text,
                        expectedControlKind(text),
                        false));
            }
            rows.add(row);
        }
        return new TableShape(rowCount, columnCount, columnWidths, rowHeights, rows);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static TableShape extractRenderedSegmentShape(JSONObject root, String title, int expectedRows) {
        TableShape rendered = toJsonShape(root);
        int startRow = findRenderedTitleRow(rendered, title);
        List<List<CellShape>> rows = new ArrayList<>();
        List<Integer> rowHeights = new ArrayList<>();
        int maxColumn = 0;
        int endRow = Math.min(rendered.rows().size(), startRow + expectedRows);
        for (int rowIndex = startRow; rowIndex < endRow; rowIndex++) {
            List<CellShape> sourceRow = rendered.rows().get(rowIndex);
            List<CellShape> reindexedRow = new ArrayList<>();
            for (CellShape cell : sourceRow) {
                CellShape reindexed = new CellShape(rowIndex - startRow, cell.columnIndex(), cell.rowSpan(),
                        cell.colSpan(), cell.text(), cell.controlKind(), cell.diagonalSlash());
                reindexedRow.add(reindexed);
                maxColumn = Math.max(maxColumn, reindexed.columnIndex() + reindexed.colSpan());
            }
            rows.add(reindexedRow);
            rowHeights.add(rendered.rowHeights().get(rowIndex));
        }
        return new TableShape(rows.size(), Math.max(maxColumn, rendered.columnCount()),
                rendered.columnWidths(), rowHeights, rows);
    }

    private static int findRenderedTitleRow(TableShape shape, String title) {
        for (int rowIndex = 0; rowIndex < shape.rows().size(); rowIndex++) {
            for (CellShape cell : shape.rows().get(rowIndex)) {
                if (normalize(cell.text()).contains(title)) {
                    return rowIndex;
                }
            }
        }
        throw new AssertionError("rendered JSON segment title not found: " + title);
    }

    private static TableShape toJsonShape(JSONObject root) {
        JSONObject rowsObject = root.getJSONObject("rows");
        JSONObject colsObject = root.getJSONObject("cols");
        List<Integer> columnWidths = new ArrayList<>();
        if (colsObject != null) {
            for (String key : sortedNumericKeys(colsObject)) {
                columnWidths.add(colsObject.getJSONObject(key).getIntValue("width"));
            }
        }
        List<List<CellShape>> rows = new ArrayList<>();
        List<Integer> rowHeights = new ArrayList<>();
        int maxColumn = 0;
        if (rowsObject != null) {
            for (String rowKey : sortedNumericKeys(rowsObject)) {
                int rowIndex = Integer.parseInt(rowKey);
                JSONObject rowObject = rowsObject.getJSONObject(rowKey);
                rowHeights.add(rowObject == null ? 36 : rowObject.getIntValue("height"));
                JSONObject cellsObject = rowObject == null ? null : rowObject.getJSONObject("cells");
                List<CellShape> row = new ArrayList<>();
                if (cellsObject != null) {
                    for (String cellKey : sortedNumericKeys(cellsObject)) {
                        int columnIndex = Integer.parseInt(cellKey);
                        JSONObject cellObject = cellsObject.getJSONObject(cellKey);
                        int rowSpan = 1;
                        int colSpan = 1;
                        if (cellObject != null && cellObject.getJSONArray("merge") != null) {
                            rowSpan = Math.max(1, cellObject.getJSONArray("merge").getIntValue(0) + 1);
                            colSpan = Math.max(1, cellObject.getJSONArray("merge").getIntValue(1) + 1);
                        }
                        row.add(new CellShape(rowIndex, columnIndex, rowSpan, colSpan,
                                normalize(cellObject == null ? "" : cellObject.getString("text")),
                                actualControlKind(cellObject),
                                cellObject != null && Boolean.TRUE.equals(cellObject.getBoolean("edhrDiagonalSlash"))));
                        maxColumn = Math.max(maxColumn, columnIndex + colSpan);
                    }
                }
                rows.add(row);
            }
        }
        return new TableShape(rows.size(), Math.max(maxColumn, columnWidths.size()),
                normalizeWidthVector(columnWidths), rowHeights, rows);
    }

    private static List<String> sortedNumericKeys(JSONObject object) {
        return object.keySet().stream()
                .filter(item -> item != null && item.chars().allMatch(Character::isDigit))
                .sorted(Comparator.comparingInt(Integer::parseInt))
                .toList();
    }

    private static TableDiffReport diff(String title, TableShape expected, TableShape actual,
                                         MesProBatchRecordParsedTable recognized,
                                         MesProBatchRecordParsedTable calibrated) {
        List<CellDiff> diffs = new ArrayList<>();
        int ignoredRightEdgeClosureCount = 0;
        if (expected.rowCount() != actual.rowCount()) {
            diffs.add(CellDiff.tableLevel(title, 100, "rowCount",
                    String.valueOf(expected.rowCount()), String.valueOf(actual.rowCount())));
        }
        if (expected.columnCount() != actual.columnCount()) {
            diffs.add(CellDiff.tableLevel(title, 80, "columnCount",
                    String.valueOf(expected.columnCount()), String.valueOf(actual.columnCount())));
        }
        if (!sameWidthVector(expected.columnWidths(), actual.columnWidths(), expected.rows())) {
            diffs.add(CellDiff.tableLevel(title, 40, "columnWidths",
                    expected.columnWidths().toString(), actual.columnWidths().toString()));
        }
        if (!sameHeightVector(expected.rowHeights(), actual.rowHeights())) {
            diffs.add(CellDiff.tableLevel(title, 20, "rowHeights",
                    expected.rowHeights().toString(), actual.rowHeights().toString()));
        }

        int comparableRows = Math.min(expected.rows().size(), actual.rows().size());
        for (int rowIndex = 0; rowIndex < comparableRows; rowIndex++) {
            List<CellShape> expectedRow = expected.rows().get(rowIndex);
            List<CellShape> actualRow = actual.rows().get(rowIndex);
            if (expectedRow.size() != actualRow.size()) {
                diffs.add(new CellDiff(title, rowIndex, -1, 60, "cellCount",
                        compactCells(expectedRow), compactCells(actualRow)));
            }
            int comparableCells = Math.min(expectedRow.size(), actualRow.size());
            for (int cellIndex = 0; cellIndex < comparableCells; cellIndex++) {
                CellShape expectedCell = expectedRow.get(cellIndex);
                CellShape actualCell = actualRow.get(cellIndex);
                int score = 0;
                List<String> reasons = new ArrayList<>();
                if (expectedCell.columnIndex() != actualCell.columnIndex()) {
                    score += 20;
                    reasons.add("columnIndex");
                }
                if (expectedCell.rowSpan() != actualCell.rowSpan()) {
                    score += 15;
                    reasons.add("rowSpan");
                }
                if (expectedCell.colSpan() != actualCell.colSpan()) {
                    if (compatibleRightEdgeClosure(expected, actual, expectedCell, actualCell)) {
                        ignoredRightEdgeClosureCount++;
                    } else {
                        score += 15;
                        reasons.add("colSpan");
                    }
                }
                if (!normalizeVisualText(expectedCell.text()).equals(normalizeVisualText(actualCell.text()))) {
                    score += 30;
                    reasons.add("text");
                }
                if (!compatibleControlKind(expectedCell.controlKind(), actualCell.controlKind())) {
                    score += 20;
                    reasons.add("control");
                }
                if (expectedCell.diagonalSlash() != actualCell.diagonalSlash()) {
                    score += 10;
                    reasons.add("diagonalSlash");
                }
                if (score > 0) {
                    diffs.add(new CellDiff(title, rowIndex, cellIndex, score, String.join("+", reasons),
                            expectedCell.compact(), actualCell.compact()));
                }
            }
        }
        diffs.sort(Comparator.comparingInt(CellDiff::score).reversed());
        return new TableDiffReport(title, expected, actual, recognized.getColumnCount(), calibrated.getColumnCount(),
                Boolean.TRUE.equals(calibrated.getPreserveSourceGrid()), diffs, ignoredRightEdgeClosureCount);
    }

    private static boolean compatibleRightEdgeClosure(TableShape expected,
                                                      TableShape actual,
                                                      CellShape expectedCell,
                                                      CellShape actualCell) {
        return expected.columnCount() == actual.columnCount()
                && expectedCell.columnIndex() == actualCell.columnIndex()
                && expectedCell.rowSpan() == actualCell.rowSpan()
                && expectedCell.colSpan() + 1 == actualCell.colSpan()
                && expectedCell.columnIndex() + expectedCell.colSpan() == expected.columnCount() - 1
                && actualCell.columnIndex() + actualCell.colSpan() == actual.columnCount()
                && normalizeVisualText(expectedCell.text()).equals(normalizeVisualText(actualCell.text()))
                && compatibleControlKind(expectedCell.controlKind(), actualCell.controlKind())
                && expectedCell.diagonalSlash() == actualCell.diagonalSlash();
    }

    private static boolean compatibleControlKind(String expected, String actual) {
        if (expected.equals(actual)) {
            return true;
        }
        if ("blank-input".equals(expected) && actual.startsWith("input")) {
            return true;
        }
        return "checkbox".equals(expected)
                && (actual.startsWith("checkbox") || actual.startsWith("radio-group"));
    }

    private static String expectedControlKind(String text) {
        String normalized = normalize(text);
        String compact = normalized.replaceAll("\\s+", "");
        if (compact.contains("工序生产记录")
                || compact.contains("关键/特殊工序")
                || compact.contains("非关键/特殊工序")) {
            return "static";
        }
        if (normalized.contains("□") || normalized.contains("☑")) {
            return "checkbox";
        }
        if (normalized.isBlank()) {
            return "blank-input";
        }
        return "static";
    }

    private static String actualControlKind(JSONObject cellObject) {
        if (cellObject == null) {
            return "missing";
        }
        JSONObject fillForm = cellObject.getJSONObject("fillForm");
        if (fillForm == null) {
            return Boolean.TRUE.equals(cellObject.getBoolean("edhrDiagonalSlash")) ? "slash" : "static";
        }
        String componentFlag = fillForm.getString("componentFlag");
        if (componentFlag == null || componentFlag.isBlank()) {
            componentFlag = "input";
        }
        JSONArray options = fillForm.getJSONArray("options");
        if (options == null || options.isEmpty()) {
            return componentFlag;
        }
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            JSONObject option = options.getJSONObject(index);
            labels.add(option == null ? "" : option.getString("label"));
        }
        return componentFlag + labels;
    }

    private static String normalizeVisualText(String text) {
        return normalize(text).replaceAll("\\s+", "");
    }

    private static void writeReport(List<TableDiffReport> reports) throws Exception {
        Path outputReport = reportPath();
        Files.createDirectories(outputReport.getParent());
        StringBuilder builder = new StringBuilder();
        builder.append("# 压力泵组装Ⅰ/光固Ⅰ单元格差异报告\n\n");
        builder.append("- source: `").append(REAL_DOC).append("`\n");
        builder.append("- generatedAt: `").append(LocalDateTime.now()).append("`\n");
        builder.append("- recognizer: `RouteB + LayoutCalibrator + ReportJsonBuilder`\n\n");
        for (TableDiffReport tableReport : reports) {
            builder.append("## ").append(tableReport.title()).append("\n\n");
            builder.append("- wordRows=").append(tableReport.expected().rowCount())
                    .append(", jsonRows=").append(tableReport.actual().rowCount())
                    .append(", wordCols=").append(tableReport.expected().columnCount())
                    .append(", jsonCols=").append(tableReport.actual().columnCount())
                    .append(", recognizedCols=").append(tableReport.recognizedColumnCount())
                    .append(", calibratedCols=").append(tableReport.calibratedColumnCount())
                    .append(", preserveSourceGrid=").append(tableReport.preserveSourceGrid())
                    .append("\n");
            builder.append("- diffCount=").append(tableReport.diffs().size())
                    .append(", maxScore=").append(tableReport.diffs().stream().mapToInt(CellDiff::score).max().orElse(0))
                    .append(", ignoredRightEdgeClosureCount=").append(tableReport.ignoredRightEdgeClosureCount())
                    .append("\n\n");
            builder.append("| rank | score | row | cell | type | expected | actual |\n");
            builder.append("|---:|---:|---:|---:|---|---|---|\n");
            List<CellDiff> topDiffs = tableReport.diffs().stream().limit(60).toList();
            for (int index = 0; index < topDiffs.size(); index++) {
                CellDiff diff = topDiffs.get(index);
                builder.append('|').append(index + 1)
                        .append('|').append(diff.score())
                        .append('|').append(diff.rowIndex() + 1)
                        .append('|').append(diff.cellIndex() + 1)
                        .append('|').append(escapeTable(diff.type()))
                        .append('|').append(escapeTable(diff.expected()))
                        .append('|').append(escapeTable(diff.actual()))
                        .append("|\n");
            }
            builder.append("\n");
        }
        Files.writeString(outputReport, builder.toString(), StandardCharsets.UTF_8);
    }

    private static Path reportPath() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path repoRoot = userDir.getFileName() != null && "yudao-module-mes".equals(userDir.getFileName().toString())
                ? userDir.getParent()
                : userDir;
        return repoRoot.resolve(REPORT_RELATIVE);
    }

    private static String compactCells(List<CellShape> cells) {
        return cells.stream().map(CellShape::compact).toList().toString();
    }

    private static String escapeTable(String value) {
        return compact(value).replace("|", "\\|");
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

    private static boolean sameWidthVector(List<Integer> expected, List<Integer> actual, List<List<CellShape>> rows) {
        if (expected.size() != actual.size()) {
            return false;
        }
        List<Integer> normalizedExpected = normalizeVisibleLeadingSectionColumnWidth(expected, rows);
        List<Integer> normalizedActual = normalizeWidthVector(actual);
        for (int index = 0; index < normalizedExpected.size(); index++) {
            if (Math.abs(normalizedExpected.get(index) - normalizedActual.get(index)) > 120) {
                return false;
            }
        }
        return true;
    }

    private static List<Integer> normalizeVisibleLeadingSectionColumnWidth(List<Integer> sourceWidths,
                                                                           List<List<CellShape>> rows) {
        List<Integer> widths = normalizeWidthVector(sourceWidths);
        if (widths == null || widths.isEmpty() || !hasLeadingVerticalSectionColumn(rows)) {
            return widths;
        }
        List<Integer> adjusted = new ArrayList<>(widths);
        int visibleWidth = Math.round(24 * 10000.0f / MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_DENSE_BUDGET_PX);
        int currentWidth = Math.max(0, adjusted.get(0));
        if (currentWidth >= visibleWidth) {
            return adjusted;
        }
        int deficit = visibleWidth - currentWidth;
        adjusted.set(0, visibleWidth);
        while (deficit > 0) {
            int donor = findBestWidthDonor(adjusted);
            if (donor <= 0) {
                break;
            }
            adjusted.set(donor, adjusted.get(donor) - 1);
            deficit--;
        }
        return adjusted;
    }

    private static boolean hasLeadingVerticalSectionColumn(List<List<CellShape>> rows) {
        if (rows == null) {
            return false;
        }
        for (List<CellShape> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            CellShape firstCell = row.get(0);
            if (firstCell != null
                    && firstCell.columnIndex() == 0
                    && firstCell.colSpan() == 1
                    && firstCell.rowSpan() >= 3
                    && !normalize(firstCell.text()).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static int findBestWidthDonor(List<Integer> widths) {
        int floor = Math.round(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX * 10000.0f
                / MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_DENSE_BUDGET_PX);
        int bestColumn = -1;
        int bestSlack = 0;
        for (int columnIndex = 1; columnIndex < widths.size(); columnIndex++) {
            int slack = Math.max(0, widths.get(columnIndex) - floor);
            if (slack > bestSlack) {
                bestSlack = slack;
                bestColumn = columnIndex;
            }
        }
        return bestColumn;
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

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u0007', ' ')
                .replace('\u0008', ' ')
                .replace('\r', '\n')
                .replace('\u0000', ' ')
                .replaceAll("[\\n]{3,}", "\n\n")
                .trim();
    }

    private static String compact(String text) {
        String normalized = normalize(text).replace('\n', '/').replaceAll("\\s+", " ");
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private record CellShape(int rowIndex, int columnIndex, int rowSpan, int colSpan, String text,
                             String controlKind, boolean diagonalSlash) {
        private String compact() {
            return "r" + (rowIndex + 1) + "c" + (columnIndex + 1)
                    + "[rs=" + rowSpan
                    + ",cs=" + colSpan
                    + ",ctrl=" + controlKind
                    + ",slash=" + diagonalSlash
                    + ",text=" + MesProBatchRecordPressurePumpCellDiffReportTest.compact(text) + "]";
        }
    }

    private record TableShape(int rowCount, int columnCount, List<Integer> columnWidths,
                              List<Integer> rowHeights, List<List<CellShape>> rows) {
    }

    private record CellDiff(String title, int rowIndex, int cellIndex, int score, String type,
                            String expected, String actual) {
        private static CellDiff tableLevel(String title, int score, String type, String expected, String actual) {
            return new CellDiff(title, -1, -1, score, type, expected, actual);
        }
    }

    private record TableDiffReport(String title, TableShape expected, TableShape actual,
                                   int recognizedColumnCount, int calibratedColumnCount,
                                   boolean preserveSourceGrid, List<CellDiff> diffs,
                                   int ignoredRightEdgeClosureCount) {
    }
}
