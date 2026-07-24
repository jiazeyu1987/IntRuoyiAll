package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
public class MesProBatchRecordRouteBRecognizer implements MesProBatchRecordRouteRecognizer {

    public static final String ROUTE_KEY = MesProBatchRecordRecognitionRouteKeys.B;

    private static final long DEFAULT_TIMEOUT_MS = 600_000L;
    private static final String PYTHON_SCRIPT = """
            import json
            import pathlib
            import sys

            try:
                import pythoncom
                import win32com.client
            except Exception as ex:
                print(f"route_b_prerequisite_missing:{ex}", file=sys.stderr)
                raise SystemExit(12)

            POINT_TO_PX = 96.0 / 72.0

            def normalize_text(value):
                if value is None:
                    return ""
                normalized = (
                    value.replace("\\x07", " ")
                    .replace("\\x08", " ")
                    .replace("\\r", "\\n")
                    .replace("\\x00", " ")
                    .strip()
                )
                while "\\n\\n\\n" in normalized:
                    normalized = normalized.replace("\\n\\n\\n", "\\n\\n")
                return normalized

            def first_line(value):
                if not value:
                    return ""
                for line in value.splitlines():
                    line = line.strip()
                    if line:
                        return " ".join(line.split())
                return " ".join(value.strip().split())

            def to_pixels(points_value, default_value):
                try:
                    numeric = float(points_value)
                except Exception:
                    numeric = 0.0
                if numeric <= 0:
                    return default_value
                return max(default_value, int(round(numeric * POINT_TO_PX)))

            def safe_float(value, default_value=0.0):
                try:
                    numeric = float(value)
                except Exception:
                    numeric = default_value
                return numeric

            def normalize_boundary(value):
                return int(round(safe_float(value, 0.0) * 20.0))

            def coalesce_boundaries(boundaries):
                if not boundaries:
                    return [0, 1]
                merged = []
                for boundary in sorted(boundaries):
                    if not merged or boundary != merged[-1]:
                        merged.append(boundary)
                if len(merged) < 2:
                    merged.append(merged[0] + 1)
                return merged

            def resolve_nearest_boundary_index(boundaries, value, default_index):
                if not boundaries:
                    return default_index
                best_index = default_index
                best_distance = None
                for index, boundary in enumerate(boundaries):
                    distance = abs(boundary - value)
                    if best_distance is None or distance < best_distance:
                        best_index = index
                        best_distance = distance
                return best_index

            def has_diagonal_border(cell):
                for border_type in (-7, -8):
                    try:
                        border = cell.Borders(border_type)
                        if int(border.LineStyle) != 0:
                            return True
                    except Exception:
                        continue
                return False

            def parse_table(table):
                rows_by_index = {}
                visual_boundaries = set()
                for cell_index in range(1, table.Range.Cells.Count + 1):
                    cell = table.Range.Cells(cell_index)
                    row_index = int(cell.RowIndex)
                    width_points = safe_float(cell.Width, 0.0)
                    width = max(1, normalize_boundary(width_points))
                    rows_by_index.setdefault(row_index, []).append({
                        "text": normalize_text(cell.Range.Text),
                        "sourceColumnIndex": int(cell.ColumnIndex),
                        "sourceCellIndex": cell_index,
                        "rowSpan": 1,
                        "width": width,
                        "widthPx": to_pixels(width_points, 60),
                        "heightPx": to_pixels(cell.Height, 24),
                        "diagonalSlash": has_diagonal_border(cell),
                    })

                for row in rows_by_index.values():
                    cursor = 0
                    for cell in sorted(row, key=lambda item: (item.get("sourceColumnIndex", 0), item.get("sourceCellIndex", 0))):
                        cell["left"] = cursor
                        cursor += max(1, int(cell.get("width", 1)))
                        cell["right"] = cursor
                        visual_boundaries.add(cell["left"])
                        visual_boundaries.add(cell["right"])

                ordered_boundaries = coalesce_boundaries(visual_boundaries)
                column_widths = []
                for index in range(0, len(ordered_boundaries) - 1):
                    column_widths.append(to_pixels((ordered_boundaries[index + 1] - ordered_boundaries[index]) / 20.0, 1))

                for row in rows_by_index.values():
                    for cell in row:
                        start_index = resolve_nearest_boundary_index(ordered_boundaries, cell["left"], 0)
                        end_index = resolve_nearest_boundary_index(ordered_boundaries, cell["right"], start_index + 1)
                        if end_index <= start_index:
                            end_index = min(len(ordered_boundaries) - 1, start_index + 1)
                        cell["columnIndex"] = start_index
                        cell["colSpan"] = max(1, end_index - start_index)
                        del cell["left"]
                        del cell["right"]
                        del cell["width"]
                        del cell["sourceCellIndex"]

                ordered_rows = []
                for index in sorted(rows_by_index.keys()):
                    ordered_rows.append(sorted(rows_by_index[index], key=lambda item: item.get("columnIndex", 0)))
                title = ""
                for row in ordered_rows:
                    for cell in row:
                        if cell["text"].strip():
                            title = first_line(cell["text"])
                            break
                    if title:
                        break
                if not title:
                    title = "Table"
                return {"title": title, "rows": ordered_rows, "columnWidths": column_widths}

            def main():
                source = pathlib.Path(sys.argv[1])
                output_path = pathlib.Path(sys.argv[2])
                if not source.exists():
                    print(f"route_b_source_missing:{source}", file=sys.stderr)
                    raise SystemExit(11)
                pythoncom.CoInitialize()
                word = None
                document = None
                try:
                    word = win32com.client.DispatchEx("Word.Application")
                    word.Visible = False
                    word.DisplayAlerts = 0
                    document = word.Documents.Open(str(source), ReadOnly=True, AddToRecentFiles=False)
                    payload = {"tables": [parse_table(document.Tables(index)) for index in range(1, document.Tables.Count + 1)]}
                    output_path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
                    print(f"route_b_output_written:{output_path}")
                finally:
                    if document is not None:
                        try:
                            document.Close(False)
                        except Exception:
                            pass
                    if word is not None:
                        try:
                            word.Quit()
                        except Exception:
                            pass
                    pythoncom.CoUninitialize()

            if __name__ == "__main__":
                main()
            """;

    @Value("${yudao.mes.batch-record-report.route-b.python-command:python}")
    private String pythonCommand = "python";
    @Value("${yudao.mes.batch-record-report.route-b.python-working-directory:#{systemProperties['user.dir']}}")
    private String pythonWorkingDirectory = System.getProperty("user.dir");
    @Value("${yudao.mes.batch-record-report.route-b.timeout-ms:" + DEFAULT_TIMEOUT_MS + "}")
    private long timeoutMs = DEFAULT_TIMEOUT_MS;

    public MesProBatchRecordRouteBRecognizer() {
    }

    MesProBatchRecordRouteBRecognizer(String pythonCommand, Path pythonWorkingDirectory, long timeoutMs) {
        this.pythonCommand = pythonCommand;
        this.pythonWorkingDirectory = pythonWorkingDirectory == null ? null : pythonWorkingDirectory.toString();
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String routeKey() {
        return ROUTE_KEY;
    }

    @Override
    public List<MesProBatchRecordParsedTable> recognize(Path sourcePath, byte[] sourceBytes, String originalFileName) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("mes-batch-record-route-b-");
            Path resolvedSourcePath = resolveSourcePath(sourcePath, sourceBytes, originalFileName, tempDir);
            Path scriptPath = tempDir.resolve("route-b-word-com.py");
            Path outputPath = tempDir.resolve("route-b-output.json");
            Files.writeString(scriptPath, PYTHON_SCRIPT, StandardCharsets.UTF_8);

            ProcessBuilder builder = new ProcessBuilder(
                    pythonCommand,
                    scriptPath.toString(),
                    resolvedSourcePath.toString(),
                    outputPath.toString()
            );
            builder.redirectErrorStream(true);
            if (StrUtil.isNotBlank(pythonWorkingDirectory)) {
                Path workingDirectory = Path.of(pythonWorkingDirectory);
                if (Files.exists(workingDirectory)) {
                    builder.directory(workingDirectory.toFile());
                }
            }
            builder.environment().put("PYTHONIOENCODING", "utf-8");

            Process process;
            try {
                process = builder.start();
            } catch (IOException ex) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        "route_b_python_process_start_failed:" + ex.getMessage());
            }

            boolean finished;
            try {
                finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        "route_b_python_process_interrupted");
            }
            if (!finished) {
                process.destroyForcibly();
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        "route_b_python_process_timeout");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        "route_b_python_process_failed:" + output);
            }
            if (!Files.exists(outputPath) || Files.size(outputPath) == 0) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        "route_b_python_output_file_missing:" + output);
            }

            RouteBResponse response = JsonUtils.parseObject(Files.readString(outputPath, StandardCharsets.UTF_8),
                    RouteBResponse.class);
            if (response == null || response.getTables() == null || response.getTables().isEmpty()) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        "route_b_python_output_invalid");
            }

            List<MesProBatchRecordParsedTable> parsedTables = new ArrayList<>();
            for (RawTablePayload rawTable : response.getTables()) {
                parsedTables.addAll(splitTemplates(toParsedTable(rawTable)));
            }
            parsedTables = alignWithDocParserVisualGrids(parsedTables, sourceBytes);
            for (int index = 0; index < parsedTables.size(); index++) {
                parsedTables.get(index).setSourceTableIndex(index + 1);
                parsedTables.get(index).setRouteBSource(true);
            }
            return parsedTables;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IOException ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_b_io_failed:" + ex.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private Path resolveSourcePath(Path sourcePath, byte[] sourceBytes, String originalFileName, Path tempDir) throws IOException {
        if (sourcePath != null && Files.exists(sourcePath)) {
            return sourcePath;
        }
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_b_source_bytes_empty");
        }
        String fileName = normalizeFileName(originalFileName);
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            fileName = "source.doc";
        }
        Path tempSourcePath = tempDir.resolve(fileName);
        Files.write(tempSourcePath, sourceBytes);
        return tempSourcePath;
    }

    private List<MesProBatchRecordParsedTable> alignWithDocParserVisualGrids(
            List<MesProBatchRecordParsedTable> routeTables, byte[] sourceBytes) {
        if (routeTables == null || routeTables.isEmpty() || sourceBytes == null || sourceBytes.length == 0) {
            return routeTables;
        }
        List<MesProBatchRecordParsedTable> docTables = new MesProBatchRecordDocParser().parse(sourceBytes);
        MesProBatchRecordDocumentFrame documentFrame = resolveDocumentFrame(docTables);
        boolean[] matched = new boolean[docTables.size()];
        List<MesProBatchRecordParsedTable> alignedTables = new ArrayList<>();
        for (MesProBatchRecordParsedTable routeTable : routeTables) {
            int matchIndex = findDocParserMatch(routeTable, docTables, matched);
            MesProBatchRecordParsedTable alignedTable;
            if (matchIndex >= 0) {
                matched[matchIndex] = true;
                alignedTable = applyRouteCellSemantics(docTables.get(matchIndex), routeTable);
            } else {
                alignedTable = routeTable;
            }
            if (alignedTable.getDocumentFrame() == null && hasDocumentFrame(documentFrame)) {
                alignedTable.setDocumentFrame(documentFrame);
            }
            alignedTables.add(alignedTable);
        }
        return alignedTables;
    }

    private MesProBatchRecordParsedTable applyRouteCellSemantics(MesProBatchRecordParsedTable docTable,
                                                                  MesProBatchRecordParsedTable routeTable) {
        if (docTable == null || routeTable == null || !hasDiagonalSlashCell(routeTable)
                || docTable.getRows() == null || routeTable.getRows() == null) {
            return docTable;
        }
        int rowCount = Math.min(docTable.getRows().size(), routeTable.getRows().size());
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<MesProBatchRecordParsedCell> docRow = docTable.getRows().get(rowIndex);
            List<MesProBatchRecordParsedCell> routeRow = routeTable.getRows().get(rowIndex);
            if (docRow == null || routeRow == null) {
                continue;
            }
            if (docRow.size() == routeRow.size()) {
                for (int cellIndex = 0; cellIndex < routeRow.size(); cellIndex++) {
                    MesProBatchRecordParsedCell routeCell = routeRow.get(cellIndex);
                    if (routeCell != null && routeCell.isDiagonalSlash() && cellIndex < docRow.size()) {
                        docRow.get(cellIndex).setDiagonalSlash(true);
                    }
                }
                continue;
            }
            for (MesProBatchRecordParsedCell routeCell : routeRow) {
                if (routeCell == null || !routeCell.isDiagonalSlash()) {
                    continue;
                }
                MesProBatchRecordParsedCell matchedCell = findNearestCompatibleCell(docRow, routeCell);
                if (matchedCell != null) {
                    matchedCell.setDiagonalSlash(true);
                }
            }
        }
        return docTable;
    }

    private boolean hasDiagonalSlashCell(MesProBatchRecordParsedTable table) {
        return table != null && table.getRows() != null && table.getRows().stream()
                .filter(row -> row != null)
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null && cell.isDiagonalSlash());
    }

    private MesProBatchRecordParsedCell findNearestCompatibleCell(List<MesProBatchRecordParsedCell> candidates,
                                                                  MesProBatchRecordParsedCell routeCell) {
        MesProBatchRecordParsedCell best = null;
        int bestDistance = Integer.MAX_VALUE;
        String routeText = normalizeCellText(routeCell);
        int routeColumn = routeCell.getColumnIndex() == null ? -1 : routeCell.getColumnIndex();
        for (MesProBatchRecordParsedCell candidate : candidates) {
            if (candidate == null || candidate.isDiagonalSlash()) {
                continue;
            }
            String candidateText = normalizeCellText(candidate);
            if (!routeText.equals(candidateText)) {
                continue;
            }
            int candidateColumn = candidate.getColumnIndex() == null ? -1 : candidate.getColumnIndex();
            int distance = routeColumn < 0 || candidateColumn < 0 ? 0 : Math.abs(routeColumn - candidateColumn);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private String normalizeCellText(MesProBatchRecordParsedCell cell) {
        return cell == null || cell.getText() == null ? "" : cell.getText().replaceAll("\\s+", "");
    }

    private MesProBatchRecordDocumentFrame resolveDocumentFrame(List<MesProBatchRecordParsedTable> docTables) {
        if (docTables == null) {
            return null;
        }
        for (MesProBatchRecordParsedTable docTable : docTables) {
            if (docTable != null && hasDocumentFrame(docTable.getDocumentFrame())) {
                return docTable.getDocumentFrame();
            }
        }
        return null;
    }

    private boolean hasDocumentFrame(MesProBatchRecordDocumentFrame documentFrame) {
        return documentFrame != null
                && ((documentFrame.getHeaderRows() != null && !documentFrame.getHeaderRows().isEmpty())
                || (documentFrame.getFooterRows() != null && !documentFrame.getFooterRows().isEmpty()));
    }

    private int findDocParserMatch(MesProBatchRecordParsedTable routeTable,
                                   List<MesProBatchRecordParsedTable> docTables,
                                   boolean[] matched) {
        if (routeTable == null || docTables == null || docTables.isEmpty()) {
            return -1;
        }
        String routeTitle = normalizeMatchTitle(routeTable.getTableTitle());
        int routeCellCount = countCells(routeTable);
        String routeFingerprint = textFingerprint(routeTable);
        String routeTextDigest = textDigest(routeTable);
        for (int index = 0; index < docTables.size(); index++) {
            if (matched[index]) {
                continue;
            }
            MesProBatchRecordParsedTable docTable = docTables.get(index);
            if (!routeTitle.equals(normalizeMatchTitle(docTable.getTableTitle()))) {
                continue;
            }
            if (Math.max(0, routeTable.getRowCount()) != Math.max(0, docTable.getRowCount())) {
                continue;
            }
            boolean sameCellCount = routeCellCount == countCells(docTable);
            boolean sameFingerprint = routeFingerprint.equals(textFingerprint(docTable));
            boolean sameTextDigest = isCompatibleTextDigest(routeTextDigest, textDigest(docTable));
            if (!sameTextDigest || (!sameFingerprint && !sameCellCount)) {
                continue;
            }
            if (docTable.getColumnWidths() == null
                    || docTable.getColumnWidths().size() != Math.max(1, docTable.getColumnCount())) {
                continue;
            }
            if (!shouldAlignToDocParserVisualGrid(routeTable, docTable)
                    && !shouldAlignToDocParserLogicalGrid(routeTable, docTable)) {
                continue;
            }
            return index;
        }
        return -1;
    }

    private boolean shouldAlignToDocParserVisualGrid(MesProBatchRecordParsedTable routeTable,
                                                     MesProBatchRecordParsedTable docTable) {
        int routeColumnCount = Math.max(1, routeTable.getColumnCount());
        int docColumnCount = Math.max(1, docTable.getColumnCount());
        if (docColumnCount < 120 || routeColumnCount >= docColumnCount) {
            return false;
        }
        int missingColumnCount = docColumnCount - routeColumnCount;
        if (missingColumnCount < Math.max(20, Math.round(docColumnCount * 0.15f))) {
            return false;
        }
        int docMaxRowEnd = maxRowEndColumn(docTable);
        if (docMaxRowEnd < docColumnCount) {
            return false;
        }
        return hasPackedInteriorGridRow(docTable, docColumnCount);
    }

    private boolean shouldAlignToDocParserLogicalGrid(MesProBatchRecordParsedTable routeTable,
                                                      MesProBatchRecordParsedTable docTable) {
        int routeColumnCount = Math.max(1, routeTable.getColumnCount());
        int docColumnCount = Math.max(1, docTable.getColumnCount());
        if (docColumnCount > 40 || routeColumnCount <= docColumnCount) {
            return false;
        }
        if (routeColumnCount < Math.max(docColumnCount * 2, docColumnCount + 6)) {
            return false;
        }
        int routeMaxRowEnd = maxRowEndColumn(routeTable);
        int docMaxRowEnd = maxRowEndColumn(docTable);
        if (routeMaxRowEnd < Math.max(docColumnCount + 1, routeColumnCount / 2) || docMaxRowEnd > docColumnCount) {
            return false;
        }
        return !hasPackedInteriorGridRow(docTable, docColumnCount)
                && !hasPackedInteriorGridRow(routeTable, routeColumnCount);
    }

    private int maxRowEndColumn(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return 0;
        }
        int maxColumn = 0;
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            maxColumn = Math.max(maxColumn, resolveRowEndColumn(row));
        }
        return maxColumn;
    }

    private boolean hasPackedInteriorGridRow(MesProBatchRecordParsedTable table, int columnCount) {
        if (table == null || table.getRows() == null || columnCount < 60) {
            return false;
        }
        int wideCellSpanThreshold = Math.max(20, Math.round(columnCount * 0.35f));
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            if (row == null || row.size() < 2) {
                continue;
            }
            for (int index = 1; index < row.size(); index++) {
                MesProBatchRecordParsedCell cell = row.get(index);
                if (cell == null || Math.max(1, cell.getColSpan()) < wideCellSpanThreshold) {
                    continue;
                }
                if (isPackedLabelGridText(cell.getText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPackedLabelGridText(String text) {
        String normalized = text == null ? "" : text.replace('\r', '\n').trim();
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

    private String normalizeMatchTitle(String title) {
        return MesProBatchRecordSharedPageTitleRules.normalizeSharedTitle(StrUtil.blankToDefault(title, ""))
                .replaceAll("\\s+", "");
    }

    private int countCells(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return 0;
        }
        return table.getRows().stream()
                .mapToInt(row -> row == null ? 0 : row.size())
                .sum();
    }

    private String textFingerprint(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            if (row == null) {
                continue;
            }
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null || cell.getText() == null) {
                    continue;
                }
                String text = cell.getText().replaceAll("\\s+", "");
                if (!text.isBlank()) {
                    builder.append(text).append('|');
                }
            }
        }
        return builder.toString();
    }

    private String textDigest(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            if (row == null) {
                continue;
            }
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null || cell.getText() == null) {
                    continue;
                }
                appendDigestText(builder, cell.getText());
            }
        }
        return builder.toString();
    }

    private void appendDigestText(StringBuilder builder, String text) {
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint) || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                builder.appendCodePoint(Character.toLowerCase(codePoint));
            }
            offset += Character.charCount(codePoint);
        }
    }

    private boolean isCompatibleTextDigest(String routeTextDigest, String docTextDigest) {
        if (routeTextDigest == null || docTextDigest == null
                || routeTextDigest.isBlank() || docTextDigest.isBlank()) {
            return false;
        }
        return routeTextDigest.equals(docTextDigest)
                || routeTextDigest.contains(docTextDigest)
                || docTextDigest.contains(routeTextDigest);
    }

    private MesProBatchRecordParsedTable toParsedTable(RawTablePayload rawTable) {
        if (rawTable == null || rawTable.getRows() == null || rawTable.getRows().isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_b_table_rows_empty");
        }
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        int maxColumnCount = 0;
        for (List<RawCellPayload> rawRow : rawTable.getRows()) {
            List<MesProBatchRecordParsedCell> row = new ArrayList<>();
            int rowColumnCount = 0;
            if (rawRow != null) {
                for (RawCellPayload rawCell : rawRow) {
                    if (rawCell == null) {
                        continue;
                    }
                    int colSpan = Math.max(1, rawCell.getColSpan());
                    Integer columnIndex = rawCell.getColumnIndex();
                    row.add(MesProBatchRecordParsedCell.builder()
                            .text(StrUtil.blankToDefault(rawCell.getText(), ""))
                            .rowSpan(Math.max(1, rawCell.getRowSpan()))
                            .colSpan(colSpan)
                            .columnIndex(columnIndex)
                            .bold(rawCell.isBold())
                            .fontSize(Math.max(10, rawCell.getFontSize()))
                            .horizontalAlign(StrUtil.blankToDefault(rawCell.getHorizontalAlign(), "left"))
                            .verticalAlign(StrUtil.blankToDefault(rawCell.getVerticalAlign(), "middle"))
                            .widthPx(Math.max(60, rawCell.getWidthPx()))
                            .heightPx(Math.max(24, rawCell.getHeightPx()))
                            .diagonalSlash(rawCell.isDiagonalSlash())
                            .build());
                    int columnEnd = columnIndex == null ? rowColumnCount + colSpan : columnIndex + colSpan;
                    rowColumnCount = Math.max(rowColumnCount + colSpan, columnEnd);
                }
            }
            rows.add(row);
            maxColumnCount = Math.max(maxColumnCount, rowColumnCount);
        }
        normalizeRepeatedHorizontalShortHeaderRows(rows);
        maxColumnCount = resolveMaxColumnCount(rows);
        restoreSharedVerticalSideHeaders(rows, Math.max(1, maxColumnCount));
        maxColumnCount = resolveMaxColumnCount(rows);
        String tableTitle = resolveRepresentativeTitle(
                extractTemplateTitle(StrUtil.blankToDefault(rawTable.getTitle(), "")), rows);
        if (tableTitle.isBlank()) {
            tableTitle = "Table";
        }
        return MesProBatchRecordParsedTable.builder()
                .tableTitle(tableTitle)
                .rowCount(rows.size())
                .columnCount(Math.max(1, Math.max(maxColumnCount, rawTable.getColumnWidths() == null ? 0 : rawTable.getColumnWidths().size())))
                .columnWidths(rawTable.getColumnWidths() == null ? List.of() : rawTable.getColumnWidths())
                .rows(rows)
                .build();
    }

    private void restoreSharedVerticalSideHeaders(List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        if (rows == null || rows.size() < 3 || columnCount < 6) {
            return;
        }
        for (int rowIndex = 0; rowIndex < rows.size() - 1; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (!looksLikeSharedVerticalBandAnchor(row, columnCount)) {
                continue;
            }
            MesProBatchRecordParsedCell sideHeader = row.get(0);
            String sideHeaderText = compactCellText(sideHeader);
            int continuationCount = countSharedVerticalBandContinuations(rows, rowIndex + 1, columnCount, sideHeaderText);
            if (continuationCount < 2) {
                continue;
            }
            sideHeader.setRowSpan(Math.max(Math.max(1, sideHeader.getRowSpan()), 1 + continuationCount));
        }
    }

    private void normalizeRepeatedHorizontalShortHeaderRows(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null) {
            return;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (!looksLikeRepeatedHorizontalShortHeaderRow(row)) {
                continue;
            }
            int columnIndex = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                cell.setColumnIndex(columnIndex++);
                cell.setColSpan(1);
            }
        }
    }

    private boolean looksLikeRepeatedHorizontalShortHeaderRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 6) {
            return false;
        }
        int sourceSpan = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
        if (sourceSpan <= row.size()) {
            return false;
        }
        int repeatedShortLabelCount = 0;
        List<String> tokens = new ArrayList<>();
        for (MesProBatchRecordParsedCell cell : row) {
            String token = compactCellText(cell);
            if (token.isBlank() || token.length() > 12 || token.contains("\n")) {
                return false;
            }
            if (tokens.contains(token)) {
                repeatedShortLabelCount++;
            } else {
                tokens.add(token);
            }
        }
        return repeatedShortLabelCount >= 1;
    }

    private boolean looksLikeSharedVerticalBandAnchor(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (row == null || row.size() < 2) {
            return false;
        }
        if (looksLikeRepeatedHorizontalShortHeaderRow(row)) {
            return false;
        }
        MesProBatchRecordParsedCell sideHeader = row.get(0);
        String sideHeaderText = compactCellText(sideHeader);
        if (sideHeaderText.isBlank() || Math.max(1, sideHeader.getColSpan()) != 1) {
            return false;
        }
        int occupiedEndColumn = resolveRowEndColumn(row);
        if (occupiedEndColumn < Math.max(3, columnCount - 3)) {
            return false;
        }
        int sideWidth = Math.max(1, sideHeader.getWidthPx());
        int rowWidth = row.stream().mapToInt(cell -> Math.max(1, cell.getWidthPx())).sum();
        if (sideWidth > 180 || sideWidth * 4 > Math.max(rowWidth, sideWidth)) {
            return false;
        }
        if (row.size() == 2) {
            String bodyText = compactCellText(row.get(1));
            return bodyText.contains("\n") || bodyText.length() >= 16;
        }
        int repeatedShortLabelCount = 0;
        List<String> tokens = new ArrayList<>();
        for (int index = 1; index < row.size(); index++) {
            String token = compactCellText(row.get(index));
            if (token.isBlank() || token.length() > 10) {
                continue;
            }
            if (tokens.contains(token)) {
                repeatedShortLabelCount++;
            } else {
                tokens.add(token);
            }
        }
        return repeatedShortLabelCount >= 1;
    }

    private int countSharedVerticalBandContinuations(List<List<MesProBatchRecordParsedCell>> rows,
                                                     int startRowIndex,
                                                     int columnCount,
                                                     String anchorText) {
        int continuationCount = 0;
        for (int rowIndex = startRowIndex; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (!looksLikeSharedVerticalBandContinuation(row, columnCount, anchorText)) {
                break;
            }
            continuationCount++;
        }
        return continuationCount;
    }

    private boolean looksLikeSharedVerticalBandContinuation(List<MesProBatchRecordParsedCell> row,
                                                           int columnCount,
                                                           String anchorText) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        MesProBatchRecordParsedCell firstCell = row.get(0);
        String firstText = compactCellText(firstCell);
        if (looksLikeSharedVerticalBandAnchor(row, columnCount)) {
            return firstText.isBlank() || firstText.equals(anchorText);
        }
        if (looksLikeIndependentSideHeader(firstCell, firstText)) {
            return false;
        }
        int totalSpan = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
        int occupiedEndColumn = Math.max(totalSpan, resolveRowEndColumn(row));
        int wideBandThreshold = Math.max(3, (int) Math.ceil(columnCount * 0.45D));
        if (occupiedEndColumn < wideBandThreshold && !looksLikeSparseContinuationDataRow(row, columnCount, occupiedEndColumn)) {
            return false;
        }
        return true;
    }

    private boolean looksLikeSparseContinuationDataRow(List<MesProBatchRecordParsedCell> row,
                                                       int columnCount,
                                                       int occupiedEndColumn) {
        if (row == null || row.size() < 4 || columnCount < 10) {
            return false;
        }
        int sparseThreshold = Math.max(3, (int) Math.ceil(columnCount * 0.35D));
        if (occupiedEndColumn < sparseThreshold) {
            return false;
        }
        int blankCount = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (compactCellText(cell).isBlank()) {
                blankCount++;
            }
        }
        return blankCount == row.size();
    }

    private int resolveRowEndColumn(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return 0;
        }
        int runningColumn = 0;
        int maxColumn = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null) {
                continue;
            }
            int colSpan = Math.max(1, cell.getColSpan());
            int columnEnd = cell.getColumnIndex() == null ? runningColumn + colSpan : cell.getColumnIndex() + colSpan;
            maxColumn = Math.max(maxColumn, columnEnd);
            runningColumn += colSpan;
        }
        return maxColumn;
    }

    private int resolveMaxColumnCount(List<List<MesProBatchRecordParsedCell>> rows) {
        int maxColumnCount = 0;
        if (rows == null) {
            return maxColumnCount;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            maxColumnCount = Math.max(maxColumnCount, resolveRowEndColumn(row));
        }
        return maxColumnCount;
    }

    private boolean looksLikeIndependentSideHeader(MesProBatchRecordParsedCell firstCell, String firstText) {
        if (firstCell == null || firstText.isBlank()) {
            return false;
        }
        if (Math.max(1, firstCell.getColSpan()) != 1) {
            return false;
        }
        if (firstCell.getWidthPx() > 180) {
            return false;
        }
        return firstText.endsWith("记录")
                || firstText.endsWith("信息")
                || firstText.endsWith("汇总")
                || firstText.contains("工序");
    }

    private String compactCellText(MesProBatchRecordParsedCell cell) {
        if (cell == null || cell.getText() == null) {
            return "";
        }
        return cell.getText().replace('\r', '\n').trim();
    }

    private List<MesProBatchRecordParsedTable> splitTemplates(MesProBatchRecordParsedTable parsedTable) {
        List<Integer> headerIndexes = findTemplateHeaderIndexes(parsedTable.getRows());
        if (headerIndexes.isEmpty() || (headerIndexes.size() == 1 && headerIndexes.get(0) == 0)) {
            if (shouldPreserveFullLeadingShortTitleTable(parsedTable.getRows())) {
                String leadingTitle = resolveLeadingSharedTitle(parsedTable.getTableTitle(), parsedTable.getRows());
                return List.of(buildParsedTable(leadingTitle,
                        copyRows(parsedTable.getRows(), 0, parsedTable.getRows().size()), parsedTable.getColumnWidths()));
            }
            List<List<MesProBatchRecordParsedCell>> representativeRows =
                    MesProBatchRecordSharedPageTitleRules.resolveRepresentativeRows(parsedTable.getRows());
            String representativeTitle = MesProBatchRecordSharedPageTitleRules.resolveRepresentativeTitle(
                    parsedTable.getTableTitle(), representativeRows);
            return List.of(buildParsedTable(representativeTitle, representativeRows, parsedTable.getColumnWidths()));
        }
        List<MesProBatchRecordParsedTable> templates = new ArrayList<>();
        for (int index = 0; index < headerIndexes.size(); index++) {
            int titleRowIndex = headerIndexes.get(index);
            int segmentStart = index == 0 ? 0 : titleRowIndex;
            int segmentEnd = index + 1 < headerIndexes.size() ? headerIndexes.get(index + 1) : parsedTable.getRows().size();
            List<List<MesProBatchRecordParsedCell>> segmentRows = copyRows(parsedTable.getRows(), segmentStart, segmentEnd);
            String title = MesProBatchRecordSharedPageTitleRules.normalizeSharedTitle(
                    firstNonBlankRowText(parsedTable.getRows().get(titleRowIndex)));
            templates.add(buildParsedTable(title, segmentRows, parsedTable.getColumnWidths()));
        }
        return templates;
    }

    private List<Integer> findTemplateHeaderIndexes(List<List<MesProBatchRecordParsedCell>> rows) {
        List<Integer> headerIndexes = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            MesProBatchRecordSharedPageTitleRules.SharedPageTitleType titleType =
                    MesProBatchRecordSharedPageTitleRules.detectTitleType(rows.get(index));
            if (MesProBatchRecordSharedPageTitleRules.shouldStartNewTemplate(titleType, !headerIndexes.isEmpty())) {
                headerIndexes.add(index);
            }
        }
        return headerIndexes;
    }

    boolean isGenericTemplateHeaderRow(List<MesProBatchRecordParsedCell> row) {
        return MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row);
    }

    private boolean shouldPreserveFullLeadingShortTitleTable(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        MesProBatchRecordSharedPageTitleRules.SharedPageTitleType firstType =
                MesProBatchRecordSharedPageTitleRules.detectTitleType(rows.get(0));
        if (firstType != MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE
                && firstType != MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY) {
            return false;
        }
        int laterShortTitleCount = 0;
        for (int index = 1; index < rows.size(); index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            MesProBatchRecordSharedPageTitleRules.SharedPageTitleType titleType =
                    MesProBatchRecordSharedPageTitleRules.detectTitleType(row);
            if (titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD) {
                return false;
            }
            if (titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY
                    || titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE) {
                laterShortTitleCount++;
            }
        }
        return laterShortTitleCount >= 1;
    }

    private String resolveLeadingSharedTitle(String fallbackTitle, List<List<MesProBatchRecordParsedCell>> rows) {
        for (List<MesProBatchRecordParsedCell> row : rows) {
            MesProBatchRecordSharedPageTitleRules.SharedPageTitleType titleType =
                    MesProBatchRecordSharedPageTitleRules.detectTitleType(row);
            if (titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY
                    || titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE) {
                String title = MesProBatchRecordSharedPageTitleRules.normalizeSharedTitle(firstNonBlankRowText(row));
                if (!title.isBlank()) {
                    return title;
                }
            }
        }
        return fallbackTitle;
    }

    private String firstNonBlankRowText(List<MesProBatchRecordParsedCell> row) {
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell.getText() != null && !cell.getText().isBlank()) {
                return cell.getText();
            }
        }
        return "";
    }

    private List<List<MesProBatchRecordParsedCell>> copyRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                             int startInclusive, int endExclusive) {
        List<List<MesProBatchRecordParsedCell>> copies = new ArrayList<>();
        for (int index = startInclusive; index < endExclusive; index++) {
            copies.add(new ArrayList<>(rows.get(index)));
        }
        return copies;
    }

    private MesProBatchRecordParsedTable buildParsedTable(String title,
                                                          List<List<MesProBatchRecordParsedCell>> rows,
                                                          List<Integer> sourceColumnWidths) {
        int maxColumnCount = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            maxColumnCount = Math.max(maxColumnCount, resolveRowEndColumn(row));
        }
        if (sourceColumnWidths != null && !sourceColumnWidths.isEmpty()) {
            maxColumnCount = Math.max(maxColumnCount, sourceColumnWidths.size());
        }
        List<Integer> columnWidths = sourceColumnWidths == null || sourceColumnWidths.isEmpty()
                ? List.of()
                : new ArrayList<>(sourceColumnWidths.subList(0,
                        Math.min(sourceColumnWidths.size(), Math.max(1, maxColumnCount))));
        if (!columnWidths.isEmpty() && columnWidths.size() < maxColumnCount) {
            while (columnWidths.size() < maxColumnCount) {
                columnWidths.add(1);
            }
        }
        return MesProBatchRecordParsedTable.builder()
                .tableTitle(title)
                .rowCount(rows.size())
                .columnCount(Math.max(1, maxColumnCount))
                .columnWidths(columnWidths)
                .rows(rows)
                .build();
    }

    private String extractTemplateTitle(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\r', '\n').trim();
        if (normalized.isBlank()) {
            return "";
        }
        String firstLine = normalized.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(normalized);
        return MesProBatchRecordSharedPageTitleRules.normalizeSharedTitle(
                firstLine.replaceAll("\\s+", " ").trim());
    }

    String resolveRepresentativeTitle(String fallbackTitle, List<List<MesProBatchRecordParsedCell>> rows) {
        return MesProBatchRecordSharedPageTitleRules.resolveRepresentativeTitle(
                extractTemplateTitle(fallbackTitle), rows);
    }

    private String normalizeFileName(String originalFileName) {
        String fileName = StrUtil.blankToDefault(originalFileName, "");
        if (StrUtil.isBlank(fileName)) {
            return "source.doc";
        }
        try {
            return Path.of(fileName).getFileName().toString();
        } catch (InvalidPathException ignored) {
            return fileName;
        }
    }

    private void cleanupTempDir(Path tempDir) {
        if (tempDir == null || Files.notExists(tempDir)) {
            return;
        }
        try (var stream = Files.walk(tempDir)) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // keep primary failure visible
                        }
                    });
        } catch (IOException ignored) {
            // keep primary failure visible
        }
    }

    @Data
    static class RouteBResponse {
        private List<RawTablePayload> tables;
    }

    @Data
    static class RawTablePayload {
        private String title;
        private List<Integer> columnWidths;
        private List<List<RawCellPayload>> rows;
    }

    @Data
    static class RawCellPayload {
        private String text;
        private Integer columnIndex;
        private int rowSpan;
        private int colSpan;
        private boolean bold;
        private int fontSize;
        private String horizontalAlign;
        private String verticalAlign;
        private int widthPx;
        private int heightPx;
        private boolean diagonalSlash;
    }
}
