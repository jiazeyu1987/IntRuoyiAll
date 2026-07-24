package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MesProBatchRecordRouteDRecognizer implements MesProBatchRecordRouteRecognizer {

    public static final String ROUTE_KEY = MesProBatchRecordRecognitionRouteKeys.D;

    private static final long DEFAULT_TIMEOUT_MS = 600_000L;
    private static final int PDF_EXPORT_FORMAT = 17;
    private static final int DEFAULT_CELL_WIDTH_PX = 120;
    private static final int DEFAULT_CELL_HEIGHT_PX = 28;
    private static final String PDF_EXPORT_SCRIPT = String.join("\n",
            "import sys",
            "import win32com.client",
            "source = sys.argv[1]",
            "pdf_target = sys.argv[2]",
            "word = None",
            "source_doc = None",
            "try:",
            "    word = win32com.client.DispatchEx('Word.Application')",
            "    word.Visible = False",
            "    word.DisplayAlerts = 0",
            "    source_doc = word.Documents.Open(source, ReadOnly=True, AddToRecentFiles=False)",
            "    source_doc.ExportAsFixedFormat(pdf_target, " + PDF_EXPORT_FORMAT + ")",
            "finally:",
            "    if source_doc is not None:",
            "        source_doc.Close(False)",
            "    if word is not None:",
            "        word.Quit()");
    private static final String PDF_TABLE_PARSE_SCRIPT = String.join("\n",
            "import json",
            "import sys",
            "import pdfplumber",
            "",
            "path = sys.argv[1]",
            "output_path = sys.argv[2]",
            "settings = {",
            "    'vertical_strategy': 'lines',",
            "    'horizontal_strategy': 'lines',",
            "    'intersection_tolerance': 5,",
            "    'snap_tolerance': 3,",
            "    'join_tolerance': 3,",
            "    'edge_min_length': 3,",
            "    'text_tolerance': 3,",
            "}",
            "",
            "def normalize(value):",
            "    if value is None:",
            "        return ''",
            "    return str(value).replace('\\r', '\\n').replace('\\x00', ' ').strip()",
            "",
            "tables = []",
            "with pdfplumber.open(path) as pdf:",
            "    for page in pdf.pages:",
            "        for table in page.extract_tables(settings) or []:",
            "            rows = []",
            "            for row in table:",
            "                if not row:",
            "                    continue",
            "                cells = []",
            "                col_index = 0",
            "                while col_index < len(row):",
            "                    text = normalize(row[col_index])",
            "                    if not text:",
            "                        col_index += 1",
            "                        continue",
            "                    span = 1",
            "                    probe = col_index + 1",
            "                    while probe < len(row) and not normalize(row[probe]):",
            "                        span += 1",
            "                        probe += 1",
            "                    cells.append({'text': text, 'rowSpan': 1, 'colSpan': span})",
            "                    col_index = probe",
            "                if cells:",
            "                    rows.append(cells)",
            "            if rows:",
            "                tables.append({'rows': rows})",
            "with open(output_path, 'w', encoding='utf-8') as fp:",
            "    json.dump({'tables': tables}, fp, ensure_ascii=False)");
    @Value("${yudao.mes.batch-record-report.route-d.python-command:}")
    private String pythonCommand;
    @Value("${yudao.mes.batch-record-report.route-d.python-working-directory:#{systemProperties['user.dir']}}")
    private String pythonWorkingDirectory;
    @Value("${yudao.mes.batch-record-report.route-d.timeout-ms:" + DEFAULT_TIMEOUT_MS + "}")
    private long timeoutMs;
    private String pdfExportScript = PDF_EXPORT_SCRIPT;
    private String pdfTableParseScript = PDF_TABLE_PARSE_SCRIPT;

    @Override
    public String routeKey() {
        return ROUTE_KEY;
    }

    @Override
    public List<MesProBatchRecordParsedTable> recognize(Path sourcePath, byte[] content, String originalFileName) {
        return recognize(originalFileName, content);
    }

    public List<MesProBatchRecordParsedTable> recognize(String originalFileName, byte[] content) {
        String fileName = StrUtil.blankToDefault(normalizeFileName(originalFileName), "batch-record.doc");
        validateDocInput(fileName, content);
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("mes-batch-record-route-d-");
            Path sourcePath = tempDir.resolve("source.doc");
            Path pdfPath = tempDir.resolve("source.route-d.pdf");
            Files.write(sourcePath, content);

            runPdfExport(sourcePath, pdfPath);
            ensureNonEmptyFile(pdfPath, "route_d_pdf_missing");
            List<RawPdfTable> pdfTables = extractPdfTables(pdfPath);
            List<MesProBatchRecordParsedTable> parsedTables = parsePdfTables(pdfTables);
            if (parsedTables.isEmpty()) {
                throw new IOException("route_d_no_tables_recognized");
            }
            for (int index = 0; index < parsedTables.size(); index++) {
                parsedTables.get(index).setSourceTableIndex(index + 1);
            }
            return parsedTables;
        } catch (IOException ex) {
            log.warn("Route D PDF recognition failed, fileName={}, message={}", fileName, ex.getMessage(), ex);
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    ex.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private void validateDocInput(String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EMPTY);
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID);
        }
    }

    private void runPdfExport(Path sourcePath, Path pdfPath) throws IOException {
        List<String> command = List.of(
                resolvePythonCommand(),
                "-c",
                pdfExportScript,
                sourcePath.toString(),
                pdfPath.toString()
        );
        String stdout = runPythonCommand(command, "route_d_pdf_export");
        if (StrUtil.isBlank(stdout) && Files.notExists(pdfPath)) {
            throw new IOException("route_d_pdf_missing");
        }
    }

    protected List<RawPdfTable> extractPdfTables(Path pdfPath) throws IOException {
        Path outputPath = pdfPath.getParent().resolve("route-d-pdf-tables.json");
        List<String> command = List.of(
                resolvePythonCommand(),
                "-c",
                pdfTableParseScript,
                pdfPath.toString(),
                outputPath.toString()
        );
        runPythonCommand(command, "route_d_pdf_table_parse");
        ensureNonEmptyFile(outputPath, "route_d_pdf_tables_output_missing");
        RawPdfResponse response = JsonUtils.parseObject(Files.readString(outputPath, StandardCharsets.UTF_8), RawPdfResponse.class);
        if (response == null || response.getTables() == null || response.getTables().isEmpty()) {
            throw new IOException("route_d_pdf_tables_empty");
        }
        return response.getTables();
    }

    private String runPythonCommand(List<String> command, String stage) throws IOException {
        MesProBatchRecordExternalToolchainPreflight.requirePositiveTimeout(timeoutMs,
                stage + "_timeout_invalid");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Path workingDirectory = MesProBatchRecordExternalToolchainPreflight.resolveWorkingDirectory(
                pythonWorkingDirectory, stage + "_working_directory_invalid");
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException ex) {
            throw new IOException(stage + "_start_failed:" + ex.getMessage(), ex);
        }
        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(stage + "_interrupted", ex);
        }
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(stage + "_timed_out");
        }
        if (process.exitValue() != 0) {
            throw new IOException(stage + "_failed:" + trimForError(stdout));
        }
        return stdout;
    }

    private List<MesProBatchRecordParsedTable> parsePdfTables(List<RawPdfTable> pdfTables) throws IOException {
        List<List<MesProBatchRecordParsedCell>> flattenedRows = new ArrayList<>();
        for (RawPdfTable pdfTable : pdfTables) {
            if (isDocumentHeaderTable(pdfTable)) {
                continue;
            }
            List<List<MesProBatchRecordParsedCell>> rows = mapRows(pdfTable);
            flattenedRows.addAll(normalizeTemplateHeaderRows(rows));
        }
        if (flattenedRows.isEmpty()) {
            throw new IOException("route_d_pdf_tables_empty");
        }
        MesProBatchRecordParsedTable flatPdfTable = buildParsedTable(resolveTemplateTitle(firstNonBlankRowText(flattenedRows.get(0))), flattenedRows);
        List<MesProBatchRecordParsedTable> templates = splitTemplates(flatPdfTable);
        if (templates.isEmpty()) {
            throw new IOException("route_d_pdf_contains_no_templates");
        }
        return templates;
    }

    private boolean isDocumentHeaderTable(RawPdfTable table) {
        if (table == null || table.getRows() == null || table.getRows().isEmpty()) {
            return true;
        }
        List<List<MesProBatchRecordParsedCell>> rows = mapRows(table);
        return isDocumentMetadataTable(rows);
    }

    private List<List<MesProBatchRecordParsedCell>> mapRows(RawPdfTable table) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        int inferredColumnCount = inferColumnCount(table);
        for (List<RawPdfCell> rawRow : table.getRows()) {
            List<MesProBatchRecordParsedCell> row = new ArrayList<>();
            if (rawRow != null) {
                for (RawPdfCell rawCell : rawRow) {
                    if (rawCell == null) {
                        continue;
                    }
                    int colSpan = Math.max(1, rawCell.getColSpan());
                    row.add(MesProBatchRecordParsedCell.builder()
                            .text(normalizeCellText(rawCell.getText()))
                            .rowSpan(Math.max(1, rawCell.getRowSpan()))
                            .colSpan(colSpan)
                            .widthPx(DEFAULT_CELL_WIDTH_PX * colSpan)
                            .heightPx(DEFAULT_CELL_HEIGHT_PX)
                            .horizontalAlign("left")
                            .verticalAlign("middle")
                            .build());
                }
            }
            if (row.size() == 1 && inferredColumnCount > 1) {
                MesProBatchRecordParsedCell cell = row.get(0);
                row.set(0, MesProBatchRecordParsedCell.builder()
                        .text(cell.getText())
                        .rowSpan(cell.getRowSpan())
                        .colSpan(inferredColumnCount)
                        .widthPx(DEFAULT_CELL_WIDTH_PX * inferredColumnCount)
                        .heightPx(cell.getHeightPx())
                        .horizontalAlign(cell.getHorizontalAlign())
                        .verticalAlign(cell.getVerticalAlign())
                        .build());
            }
            rows.add(row);
        }
        return rows;
    }

    private int inferColumnCount(RawPdfTable table) {
        int maxColumns = 1;
        for (List<RawPdfCell> row : table.getRows()) {
            int columns = 0;
            if (row != null) {
                for (RawPdfCell cell : row) {
                    if (cell != null) {
                        columns += Math.max(1, cell.getColSpan());
                    }
                }
            }
            maxColumns = Math.max(maxColumns, columns);
        }
        return maxColumns;
    }

    private List<List<MesProBatchRecordParsedCell>> normalizeTemplateHeaderRows(List<List<MesProBatchRecordParsedCell>> rows) {
        List<List<MesProBatchRecordParsedCell>> normalizedRows = new ArrayList<>();
        for (List<MesProBatchRecordParsedCell> row : rows) {
            TitleChecklistSplit split = splitHeaderChecklist(row);
            if (split == null) {
                normalizedRows.add(row);
                continue;
            }
            normalizedRows.add(List.of(split.titleCell()));
            normalizedRows.add(List.of(split.checklistCell()));
        }
        return normalizedRows;
    }

    private TitleChecklistSplit splitHeaderChecklist(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() != 1) {
            return null;
        }
        MesProBatchRecordParsedCell cell = row.get(0);
        String text = normalizeCellText(cell.getText());
        String title = resolveTemplateTitle(text);
        if (title.isBlank() || title.equals(text) || !text.contains("关键/特殊工序")) {
            return null;
        }
        String checklist = text.substring(text.indexOf(title) + title.length()).trim();
        if (checklist.isBlank()) {
            return null;
        }
        MesProBatchRecordParsedCell titleCell = MesProBatchRecordParsedCell.builder()
                .text(title)
                .rowSpan(1)
                .colSpan(cell.getColSpan())
                .widthPx(cell.getWidthPx())
                .heightPx(cell.getHeightPx())
                .horizontalAlign(cell.getHorizontalAlign())
                .verticalAlign(cell.getVerticalAlign())
                .build();
        MesProBatchRecordParsedCell checklistCell = MesProBatchRecordParsedCell.builder()
                .text(checklist)
                .rowSpan(1)
                .colSpan(cell.getColSpan())
                .widthPx(cell.getWidthPx())
                .heightPx(cell.getHeightPx())
                .horizontalAlign(cell.getHorizontalAlign())
                .verticalAlign(cell.getVerticalAlign())
                .build();
        return new TitleChecklistSplit(titleCell, checklistCell);
    }

    private List<MesProBatchRecordParsedTable> splitTemplates(MesProBatchRecordParsedTable parsedTable) {
        List<Integer> headerIndexes = findTemplateHeaderIndexes(parsedTable.getRows());
        if (headerIndexes.isEmpty() || (headerIndexes.size() == 1 && headerIndexes.get(0) == 0)) {
            parsedTable.setTableTitle(resolveTemplateTitle(parsedTable.getTableTitle()));
            return List.of(parsedTable);
        }
        List<MesProBatchRecordParsedTable> templates = new ArrayList<>();
        for (int index = 0; index < headerIndexes.size(); index++) {
            int titleRowIndex = headerIndexes.get(index);
            int segmentStart = index == 0 ? 0 : titleRowIndex;
            int segmentEnd = index + 1 < headerIndexes.size() ? headerIndexes.get(index + 1) : parsedTable.getRows().size();
            List<List<MesProBatchRecordParsedCell>> segmentRows = copyRows(parsedTable.getRows(), segmentStart, segmentEnd);
            String title = resolveTemplateTitle(firstNonBlankRowText(parsedTable.getRows().get(titleRowIndex)));
            templates.add(buildParsedTable(title, segmentRows));
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

    private boolean isTemplateHeader(List<MesProBatchRecordParsedCell> row) {
        String normalized = normalizeCellText(firstNonBlankRowText(row));
        if (normalized.isBlank()) {
            return false;
        }
        return MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row);
    }

    private String resolveTemplateTitle(String rawText) {
        String normalized = normalizeCellText(rawText);
        if (MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(buildHeaderProbeRow(normalized))) {
            return MesProBatchRecordSharedPageTitleRules.normalizeSharedTitle(normalized);
        }
        return normalized;
    }

    private boolean isDocumentMetadataTable(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return true;
        }
        int rowsToInspect = Math.min(3, rows.size());
        boolean hasTitleLikeCell = false;
        boolean hasTemplateHeader = false;
        boolean hasMultiCellMetadataRows = false;
        for (int index = 0; index < rowsToInspect; index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            String rowText = firstNonBlankRowText(row);
            if (MesProBatchRecordSharedPageTitleRules.detectTitleType(row)
                    != MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE) {
                hasTemplateHeader = true;
            }
            if (rowText.length() >= 8 && !MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row)) {
                hasTitleLikeCell = true;
            }
            if (row != null && row.size() >= 2 && countNonBlankCells(row) >= 2) {
                hasMultiCellMetadataRows = true;
            }
        }
        return !hasTemplateHeader && hasTitleLikeCell && hasMultiCellMetadataRows;
    }

    private List<MesProBatchRecordParsedCell> buildHeaderProbeRow(String text) {
        return List.of(MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(1)
                .colSpan(1)
                .widthPx(DEFAULT_CELL_WIDTH_PX)
                .heightPx(DEFAULT_CELL_HEIGHT_PX)
                .horizontalAlign("left")
                .verticalAlign("middle")
                .build());
    }

    private String firstNonBlankRowText(List<MesProBatchRecordParsedCell> row) {
        for (MesProBatchRecordParsedCell cell : row) {
            if (StrUtil.isNotBlank(cell.getText())) {
                return normalizeCellText(cell.getText());
            }
        }
        return "";
    }

    private int countNonBlankCells(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && StrUtil.isNotBlank(cell.getText())) {
                count++;
            }
        }
        return count;
    }

    private List<List<MesProBatchRecordParsedCell>> copyRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                             int startInclusive,
                                                             int endExclusive) {
        List<List<MesProBatchRecordParsedCell>> copies = new ArrayList<>();
        for (int index = startInclusive; index < endExclusive; index++) {
            copies.add(new ArrayList<>(rows.get(index)));
        }
        return copies;
    }

    private MesProBatchRecordParsedTable buildParsedTable(String title, List<List<MesProBatchRecordParsedCell>> rows) {
        int columnCount = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            int currentColumns = row.stream().mapToInt(MesProBatchRecordParsedCell::getColSpan).sum();
            columnCount = Math.max(columnCount, currentColumns);
        }
        return MesProBatchRecordParsedTable.builder()
                .tableTitle(title)
                .rowCount(rows.size())
                .columnCount(columnCount)
                .rows(rows)
                .build();
    }

    private void ensureNonEmptyFile(Path path, String errorKey) throws IOException {
        if (!Files.exists(path) || Files.size(path) <= 0) {
            throw new IOException(errorKey);
        }
    }

    private String resolvePythonCommand() throws IOException {
        return MesProBatchRecordExternalToolchainPreflight.requireCommand(
                pythonCommand, "route_d_python_command_missing");
    }

    private String normalizeFileName(String originalFileName) {
        if (StrUtil.isBlank(originalFileName)) {
            return "";
        }
        try {
            return Path.of(originalFileName).getFileName().toString();
        } catch (InvalidPathException ignored) {
            return originalFileName;
        }
    }

    private String normalizeCellText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', '\n')
                .replace('\u0007', ' ')
                .replace('\u0008', ' ')
                .replace('\u0000', ' ')
                .replaceAll("[\\n]{3,}", "\n\n")
                .trim();
    }

    private String trimForError(String output) {
        String normalized = normalizeCellText(output).replaceAll("\\s+", " ");
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
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
                            // best-effort cleanup only
                        }
                    });
        } catch (IOException ignored) {
            // best-effort cleanup only
        }
    }

    @Data
    static class RawPdfResponse {
        private List<RawPdfTable> tables;
    }

    @Data
    static class RawPdfTable {
        private List<List<RawPdfCell>> rows;
    }

    @Data
    static class RawPdfCell {
        private String text;
        private Integer rowSpan;
        private Integer colSpan;
    }

    private record TitleChecklistSplit(MesProBatchRecordParsedCell titleCell,
                                       MesProBatchRecordParsedCell checklistCell) {
    }
}
