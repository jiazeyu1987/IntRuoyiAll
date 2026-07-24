package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
public class MesProBatchRecordRouteCRecognizer implements MesProBatchRecordRouteRecognizer {

    public static final String ROUTE_KEY = MesProBatchRecordRecognitionRouteKeys.C;

    private static final String DOCX_FORMAT = "docx";
    private static final String CN_OPERATION_RECORD = "\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55";
    private static final String EN_OPERATION_RECORD = "operation record";
    private static final String CN_CHECKLIST_SUFFIX = "\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f";
    private static final int LARGE_GRID_COLUMN_THRESHOLD = 35;
    private static final int LARGE_GRID_TARGET_COLUMNS = 19;
    private static final int MEDIUM_GRID_COLUMN_THRESHOLD = 20;
    private static final int MEDIUM_GRID_TARGET_COLUMNS = 14;
    private static final int MIN_COMPRESSED_CELL_WIDTH_PX = 24;
    private static final long DEFAULT_NORMALIZE_TIMEOUT_MS = 120_000L;
    private static final int DOCX_FILE_FORMAT = 16;
    private static final String PYTHON_SCRIPT = String.join("\n",
            "import sys",
            "import win32com.client",
            "source = sys.argv[1]",
            "target = sys.argv[2]",
            "word = None",
            "doc = None",
            "try:",
            "    word = win32com.client.DispatchEx('Word.Application')",
            "    word.Visible = False",
            "    word.DisplayAlerts = 0",
            "    doc = word.Documents.Open(source, ReadOnly=True)",
            "    doc.SaveAs2(target, FileFormat=" + DOCX_FILE_FORMAT + ")",
            "finally:",
            "    if doc is not None:",
            "        doc.Close(False)",
            "    if word is not None:",
            "        word.Quit()");

    private final RouteCNormalizer normalizer;
    @Value("${yudao.mes.batch-record-report.route-c.python-command:}")
    private String pythonCommand;
    @Value("${yudao.mes.batch-record-report.route-c.python-working-directory:#{systemProperties['user.dir']}}")
    private String pythonWorkingDirectory = System.getProperty("user.dir");
    @Value("${yudao.mes.batch-record-report.route-c.timeout-ms:" + DEFAULT_NORMALIZE_TIMEOUT_MS + "}")
    private long normalizeTimeoutMs = DEFAULT_NORMALIZE_TIMEOUT_MS;

    public MesProBatchRecordRouteCRecognizer() {
        this(null);
    }

    MesProBatchRecordRouteCRecognizer(RouteCNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    @Override
    public String routeKey() {
        return ROUTE_KEY;
    }

    public List<MesProBatchRecordParsedTable> recognize(byte[] bytes) {
        return recognize("source.doc", bytes);
    }

    public List<MesProBatchRecordParsedTable> recognize(String originalFileName, byte[] bytes) {
        return recognize(null, bytes, originalFileName);
    }

    @Override
    public List<MesProBatchRecordParsedTable> recognize(Path sourcePath, byte[] sourceBytes, String originalFileName) {
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_c_source_bytes_empty");
        }
        String normalizedFileName = normalizeFileName(originalFileName, sourcePath);
        try {
            NormalizedDocument normalizedDocument = normalizeDocument(normalizedFileName, sourceBytes);
            validateNormalizedDocument(normalizedDocument);
            List<MesProBatchRecordParsedTable> tables = parseNormalizedDocx(normalizedDocument.bytes());
            if (tables.isEmpty()) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        "route_c_no_tables_recognized");
            }
            return tables;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            String message = StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    message.startsWith("route_c_") ? message : "route_c_normalize_parse_failed:" + message);
        }
    }

    private NormalizedDocument normalizeDocument(String normalizedFileName, byte[] sourceBytes) throws IOException {
        if (normalizer != null) {
            return normalizer.normalize(normalizedFileName, sourceBytes);
        }
        return new WordComDocxNormalizer(pythonCommand, pythonWorkingDirectory, normalizeTimeoutMs)
                .normalize(normalizedFileName, sourceBytes);
    }

    private void validateNormalizedDocument(NormalizedDocument normalizedDocument) {
        if (normalizedDocument == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_c_normalized_document_missing");
        }
        String format = StrUtil.blankToDefault(normalizedDocument.format(), "").trim().toLowerCase(Locale.ROOT);
        if (!DOCX_FORMAT.equals(format)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_c_normalized_format_invalid:" + normalizedDocument.format());
        }
        if (normalizedDocument.bytes() == null || normalizedDocument.bytes().length == 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_c_normalized_output_empty");
        }
    }

    private List<MesProBatchRecordParsedTable> parseNormalizedDocx(byte[] bytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<MesProBatchRecordParsedTable> parsedTables = new ArrayList<>();
            for (XWPFTable table : document.getTables()) {
                parsedTables.addAll(splitTemplates(parseTable(table)));
            }
            for (int index = 0; index < parsedTables.size(); index++) {
                parsedTables.get(index).setSourceTableIndex(index + 1);
            }
            return parsedTables;
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_c_normalized_docx_invalid:" + ex.getMessage());
        }
    }

    private MesProBatchRecordParsedTable parseTable(XWPFTable table) {
        List<List<MesProBatchRecordParsedCell>> parsedRows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<MesProBatchRecordParsedCell> parsedCells = new ArrayList<>();
            int rowHeightPx = toPixels(row.getHeight(), 36);
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = normalizeCellText(cell.getText());
                MesProBatchRecordParsedCell parsedCell = MesProBatchRecordParsedCell.builder()
                        .text(text)
                        .rowSpan(1)
                        .colSpan(resolveColSpan(cell))
                        .bold(resolveBold(cell))
                        .fontSize(resolveFontSize(cell))
                        .horizontalAlign(resolveHorizontalAlign(cell))
                        .verticalAlign(resolveVerticalAlign(cell))
                        .widthPx(resolveWidthPx(cell))
                        .heightPx(rowHeightPx)
                        .build();
                parsedCells.add(parsedCell);
            }
            parsedRows.add(parsedCells);
        }
        List<List<MesProBatchRecordParsedCell>> normalizedRows = normalizeTemplateHeaderRows(parsedRows);
        int maxColumnCount = resolveMaxColumnCount(normalizedRows);
        List<List<MesProBatchRecordParsedCell>> displayRows = compressWideGridColumns(normalizedRows, maxColumnCount);
        String tableTitle = resolveTableTitle(displayRows);
        maxColumnCount = resolveMaxColumnCount(displayRows);
        if (tableTitle.isBlank()) {
            tableTitle = "Table";
        }
        return MesProBatchRecordParsedTable.builder()
                .tableTitle(tableTitle)
                .rowCount(displayRows.size())
                .columnCount(maxColumnCount)
                .rows(displayRows)
                .build();
    }

    private List<MesProBatchRecordParsedTable> splitTemplates(MesProBatchRecordParsedTable parsedTable) {
        List<Integer> headerIndexes = findTemplateHeaderIndexes(parsedTable.getRows());
        if (headerIndexes.isEmpty() || (headerIndexes.size() == 1 && headerIndexes.get(0) == 0)) {
            parsedTable.setTableTitle(extractTemplateTitle(parsedTable.getTableTitle()));
            return List.of(parsedTable);
        }
        List<MesProBatchRecordParsedTable> templates = new ArrayList<>();
        for (int index = 0; index < headerIndexes.size(); index++) {
            int segmentStart = index == 0 ? 0 : headerIndexes.get(index);
            int titleRowIndex = headerIndexes.get(index);
            int segmentEnd = index + 1 < headerIndexes.size() ? headerIndexes.get(index + 1) : parsedTable.getRows().size();
            List<List<MesProBatchRecordParsedCell>> segmentRows = copyRows(parsedTable.getRows(), segmentStart, segmentEnd);
            String title = extractTemplateTitle(firstNonBlankRowText(parsedTable.getRows().get(titleRowIndex)));
            templates.add(buildParsedTable(title, segmentRows));
        }
        return templates;
    }

    private List<Integer> findTemplateHeaderIndexes(List<List<MesProBatchRecordParsedCell>> rows) {
        List<Integer> headerIndexes = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            String rowText = extractTemplateTitle(firstNonBlankRowText(rows.get(index)));
            if (isTemplateHeader(rowText)) {
                headerIndexes.add(index);
            }
        }
        return headerIndexes;
    }

    private boolean isTemplateHeader(String rowText) {
        if (rowText == null || rowText.isBlank()) {
            return false;
        }
        String normalized = rowText.replaceAll("\\s+", " ").trim();
        return "\u4ea7\u54c1\u4fe1\u606f".equals(normalized)
                || normalized.contains("\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55")
                || "Product Information".equalsIgnoreCase(normalized)
                || normalized.toLowerCase(Locale.ROOT).contains("operation record");
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
                                                             int startInclusive,
                                                             int endExclusive) {
        List<List<MesProBatchRecordParsedCell>> copies = new ArrayList<>();
        for (int index = startInclusive; index < endExclusive; index++) {
            copies.add(new ArrayList<>(rows.get(index)));
        }
        return copies;
    }

    private MesProBatchRecordParsedTable buildParsedTable(String title, List<List<MesProBatchRecordParsedCell>> rows) {
        int maxColumnCount = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            int rowColumnCount = row.stream().mapToInt(MesProBatchRecordParsedCell::getColSpan).sum();
            maxColumnCount = Math.max(maxColumnCount, rowColumnCount);
        }
        return MesProBatchRecordParsedTable.builder()
                .tableTitle(title)
                .rowCount(rows.size())
                .columnCount(maxColumnCount)
                .rows(rows)
                .build();
    }

    private List<List<MesProBatchRecordParsedCell>> normalizeTemplateHeaderRows(List<List<MesProBatchRecordParsedCell>> rows) {
        List<List<MesProBatchRecordParsedCell>> normalizedRows = new ArrayList<>();
        for (List<MesProBatchRecordParsedCell> row : rows) {
            HeaderCellRewrite rewrite = rewriteTemplateHeaderRow(row);
            normalizedRows.add(rewrite == null ? row
                    : copyRowWithReplacement(row, rewrite.cellIndex(), rewrite.displayText(), rewrite.rowHeightPx()));
        }
        return normalizedRows;
    }

    private String resolveTableTitle(List<List<MesProBatchRecordParsedCell>> rows) {
        for (List<MesProBatchRecordParsedCell> row : rows) {
            String rowText = firstNonBlankRowText(row);
            if (!rowText.isBlank()) {
                return extractTemplateTitle(rowText);
            }
        }
        return "";
    }

    private int resolveMaxColumnCount(List<List<MesProBatchRecordParsedCell>> rows) {
        return rows.stream()
                .mapToInt(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getColSpan).sum())
                .max()
                .orElse(0);
    }

    private List<List<MesProBatchRecordParsedCell>> compressWideGridColumns(List<List<MesProBatchRecordParsedCell>> rows,
                                                                            int maxColumnCount) {
        int targetColumnCount = resolveTargetColumnCount(maxColumnCount);
        if (targetColumnCount >= maxColumnCount) {
            return rows;
        }
        List<List<MesProBatchRecordParsedCell>> compressedRows = new ArrayList<>(rows.size());
        for (List<MesProBatchRecordParsedCell> row : rows) {
            compressedRows.add(compressRowColumns(row, targetColumnCount));
        }
        return compressedRows;
    }

    private int resolveTargetColumnCount(int maxColumnCount) {
        if (maxColumnCount >= LARGE_GRID_COLUMN_THRESHOLD) {
            return LARGE_GRID_TARGET_COLUMNS;
        }
        if (maxColumnCount >= MEDIUM_GRID_COLUMN_THRESHOLD) {
            return MEDIUM_GRID_TARGET_COLUMNS;
        }
        return maxColumnCount;
    }

    private List<MesProBatchRecordParsedCell> compressRowColumns(List<MesProBatchRecordParsedCell> row, int targetColumnCount) {
        int rowColumnCount = row.stream().mapToInt(MesProBatchRecordParsedCell::getColSpan).sum();
        if (rowColumnCount <= targetColumnCount || row.isEmpty()) {
            return row;
        }
        if (row.size() == 1) {
            MesProBatchRecordParsedCell source = row.get(0);
            return List.of(copyCell(source, source.getText(), targetColumnCount,
                    Math.max(MIN_COMPRESSED_CELL_WIDTH_PX,
                            Math.round(source.getWidthPx() * (targetColumnCount / (float) Math.max(source.getColSpan(), 1)))),
                    source.getHeightPx()));
        }
        List<ScaledSpan> scaledSpans = new ArrayList<>(row.size());
        int assigned = 0;
        for (int index = 0; index < row.size(); index++) {
            MesProBatchRecordParsedCell cell = row.get(index);
            double exact = cell.getColSpan() * targetColumnCount / (double) rowColumnCount;
            int scaled = Math.max(1, (int) Math.floor(exact));
            assigned += scaled;
            scaledSpans.add(new ScaledSpan(index, scaled, exact - scaled));
        }
        while (assigned < targetColumnCount) {
            scaledSpans.stream()
                    .max(Comparator.comparingDouble(ScaledSpan::fractionalPart)
                            .thenComparingInt(ScaledSpan::index))
                    .ifPresent(span -> {
                        span.scaledSpan++;
                    });
            assigned++;
        }
        while (assigned > targetColumnCount) {
            scaledSpans.stream()
                    .filter(span -> span.scaledSpan > 1)
                    .min(Comparator.comparingDouble(ScaledSpan::fractionalPart)
                            .thenComparingInt(ScaledSpan::index))
                    .ifPresent(span -> {
                        span.scaledSpan--;
                    });
            assigned--;
        }
        List<MesProBatchRecordParsedCell> compressedRow = new ArrayList<>(row.size());
        for (ScaledSpan span : scaledSpans) {
            MesProBatchRecordParsedCell source = row.get(span.index);
            int widthPx = Math.max(MIN_COMPRESSED_CELL_WIDTH_PX,
                    Math.round(source.getWidthPx() * (span.scaledSpan / (float) Math.max(source.getColSpan(), 1))));
            compressedRow.add(copyCell(source, source.getText(), span.scaledSpan, widthPx, source.getHeightPx()));
        }
        return compressedRow;
    }

    private HeaderCellRewrite rewriteTemplateHeaderRow(List<MesProBatchRecordParsedCell> row) {
        int nonBlankCellIndex = -1;
        int nonBlankCellCount = 0;
        for (int index = 0; index < row.size(); index++) {
            MesProBatchRecordParsedCell cell = row.get(index);
            if (cell.getText() == null || cell.getText().isBlank()) {
                continue;
            }
            nonBlankCellIndex = index;
            nonBlankCellCount++;
            if (nonBlankCellCount > 1) {
                return null;
            }
        }
        if (nonBlankCellCount != 1) {
            return null;
        }
        MesProBatchRecordParsedCell cell = row.get(nonBlankCellIndex);
        HeaderChecklistSplit split = splitHeaderChecklist(cell.getText());
        if (split == null) {
            return null;
        }
        int rowHeightPx = Math.max(48, cell.getHeightPx() * 2);
        return new HeaderCellRewrite(nonBlankCellIndex, split.title() + "\n" + split.checklist(), rowHeightPx);
    }

    private List<MesProBatchRecordParsedCell> copyRowWithReplacement(List<MesProBatchRecordParsedCell> row,
                                                                     int replacementIndex,
                                                                     String replacementText,
                                                                     int rowHeightPx) {
        List<MesProBatchRecordParsedCell> copiedRow = new ArrayList<>(row.size());
        for (int index = 0; index < row.size(); index++) {
            MesProBatchRecordParsedCell source = row.get(index);
            copiedRow.add(copyCell(source,
                    index == replacementIndex ? replacementText : source.getText(),
                    source.getColSpan(),
                    source.getWidthPx(),
                    rowHeightPx));
        }
        return copiedRow;
    }

    private MesProBatchRecordParsedCell copyCell(MesProBatchRecordParsedCell source,
                                                 String text,
                                                 int colSpan,
                                                 int widthPx,
                                                 int heightPx) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(source.getRowSpan())
                .colSpan(colSpan)
                .bold(source.isBold())
                .fontSize(source.getFontSize())
                .horizontalAlign(source.getHorizontalAlign())
                .verticalAlign(source.getVerticalAlign())
                .widthPx(widthPx)
                .heightPx(heightPx)
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
        HeaderChecklistSplit split = splitHeaderChecklist(firstLine);
        String title = split == null ? firstLine : split.title();
        return title.replaceAll("\\s+", " ").trim();
    }

    private HeaderChecklistSplit splitHeaderChecklist(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replace('\r', '\n').trim();
        if (normalized.isBlank()) {
            return null;
        }
        String flattened = normalized.replaceAll("\\s+", " ").trim();
        int headerEnd = findOperationRecordEndIndex(flattened);
        if (headerEnd < 0 || headerEnd >= flattened.length()) {
            return null;
        }
        String title = flattened.substring(0, headerEnd).trim();
        String checklist = flattened.substring(headerEnd).trim();
        if (!isTemplateHeader(title) || checklist.isBlank() || !checklist.contains(CN_CHECKLIST_SUFFIX)) {
            return null;
        }
        return new HeaderChecklistSplit(title, checklist);
    }

    private int findOperationRecordEndIndex(String text) {
        int cnIndex = text.indexOf(CN_OPERATION_RECORD);
        if (cnIndex >= 0) {
            return cnIndex + CN_OPERATION_RECORD.length();
        }
        String lowerCaseText = text.toLowerCase(Locale.ROOT);
        int enIndex = lowerCaseText.indexOf(EN_OPERATION_RECORD);
        if (enIndex >= 0) {
            return enIndex + EN_OPERATION_RECORD.length();
        }
        return -1;
    }

    private int resolveColSpan(XWPFTableCell cell) {
        if (cell.getCTTc().getTcPr() == null || !cell.getCTTc().getTcPr().isSetGridSpan()) {
            return 1;
        }
        return Math.max(1, cell.getCTTc().getTcPr().getGridSpan().getVal().intValue());
    }

    private boolean resolveBold(XWPFTableCell cell) {
        XWPFRun firstRun = firstRun(cell);
        return firstRun != null && firstRun.isBold();
    }

    private int resolveFontSize(XWPFTableCell cell) {
        XWPFRun firstRun = firstRun(cell);
        if (firstRun == null || firstRun.getFontSize() <= 0) {
            return 10;
        }
        return Math.max(10, firstRun.getFontSize());
    }

    private String resolveHorizontalAlign(XWPFTableCell cell) {
        XWPFParagraph paragraph = firstParagraph(cell);
        if (paragraph == null) {
            return "left";
        }
        ParagraphAlignment alignment = paragraph.getAlignment();
        if (alignment == ParagraphAlignment.CENTER) {
            return "center";
        }
        if (alignment == ParagraphAlignment.RIGHT) {
            return "right";
        }
        return "left";
    }

    private String resolveVerticalAlign(XWPFTableCell cell) {
        if (cell.getVerticalAlignment() == XWPFTableCell.XWPFVertAlign.TOP) {
            return "top";
        }
        if (cell.getVerticalAlignment() == XWPFTableCell.XWPFVertAlign.BOTTOM) {
            return "bottom";
        }
        TextAlignment alignment = firstParagraph(cell) == null ? null : firstParagraph(cell).getVerticalAlignment();
        if (alignment == TextAlignment.TOP) {
            return "top";
        }
        if (alignment == TextAlignment.BOTTOM) {
            return "bottom";
        }
        return "middle";
    }

    private int resolveWidthPx(XWPFTableCell cell) {
        try {
            return toPixels((int) Math.round(cell.getWidthDecimal()), 120);
        } catch (Exception ignored) {
            return 120;
        }
    }

    private XWPFParagraph firstParagraph(XWPFTableCell cell) {
        if (cell.getParagraphs().isEmpty()) {
            return null;
        }
        return cell.getParagraphs().get(0);
    }

    private XWPFRun firstRun(XWPFTableCell cell) {
        XWPFParagraph paragraph = firstParagraph(cell);
        if (paragraph == null || paragraph.getRuns().isEmpty()) {
            return null;
        }
        return paragraph.getRuns().get(0);
    }

    private int toPixels(int twips, int defaultValue) {
        if (twips <= 0) {
            return defaultValue;
        }
        return Math.max(defaultValue, Math.round(twips / 15.0f));
    }

    private String normalizeCellText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
                .replace('\u0007', ' ')
                .replace('\u0008', ' ')
                .replace('\r', '\n')
                .replace('\u0000', ' ')
                .replaceAll("[\\n]{3,}", "\n\n")
                .trim();
        return normalized.isBlank() ? "" : normalized;
    }

    private String normalizeFileName(String originalFileName, Path sourcePath) {
        String fileName = StrUtil.blankToDefault(originalFileName, "");
        if (StrUtil.isBlank(fileName) && sourcePath != null) {
            fileName = sourcePath.getFileName().toString();
        }
        if (StrUtil.isBlank(fileName)) {
            return "source.doc";
        }
        try {
            return Path.of(fileName).getFileName().toString();
        } catch (InvalidPathException ignored) {
            return fileName;
        }
    }

    @FunctionalInterface
    interface RouteCNormalizer {

        NormalizedDocument normalize(String originalFileName, byte[] sourceBytes) throws IOException;
    }

    public record NormalizedDocument(String format, byte[] bytes) {
    }

    static final class WordComDocxNormalizer implements RouteCNormalizer {

        private final String pythonCommand;
        private final String pythonWorkingDirectory;
        private final long timeoutMs;

        WordComDocxNormalizer() {
            this(null, System.getProperty("user.dir"), DEFAULT_NORMALIZE_TIMEOUT_MS);
        }

        WordComDocxNormalizer(String pythonCommand, Path pythonWorkingDirectory, long timeoutMs) {
            this(pythonCommand, pythonWorkingDirectory == null ? null : pythonWorkingDirectory.toString(), timeoutMs);
        }

        WordComDocxNormalizer(String pythonCommand, String pythonWorkingDirectory, long timeoutMs) {
            this.pythonCommand = pythonCommand;
            this.pythonWorkingDirectory = pythonWorkingDirectory;
            this.timeoutMs = timeoutMs;
        }

        @Override
        public NormalizedDocument normalize(String originalFileName, byte[] sourceBytes) throws IOException {
            String fileName = StrUtil.blankToDefault(originalFileName, "batch-record.doc");
            if (!fileName.toLowerCase(Locale.ROOT).endsWith(".doc")) {
                throw new IOException("route_c_only_supports_doc_input");
            }
            Path tempDir = Files.createTempDirectory("mes-batch-record-route-c-");
            Path sourcePath = tempDir.resolve(fileName);
            Path targetPath = tempDir.resolve(fileName.substring(0, fileName.length() - 4) + ".route-c.docx");
            try {
                Files.write(sourcePath, sourceBytes);
                runWordConversion(sourcePath, targetPath);
                if (!Files.exists(targetPath) || Files.size(targetPath) == 0) {
                    throw new IOException("route_c_normalized_docx_missing");
                }
                return new NormalizedDocument(DOCX_FORMAT, Files.readAllBytes(targetPath));
            } finally {
                try {
                    Files.deleteIfExists(sourcePath);
                    Files.deleteIfExists(targetPath);
                    Files.deleteIfExists(tempDir);
                } catch (IOException ignored) {
                    // Keep the primary normalization failure visible.
                }
            }
        }

        private void runWordConversion(Path sourcePath, Path targetPath) throws IOException {
            String resolvedPythonCommand = MesProBatchRecordExternalToolchainPreflight.requireCommand(
                    pythonCommand, "route_c_python_command_missing");
            MesProBatchRecordExternalToolchainPreflight.requirePositiveTimeout(
                    timeoutMs, "route_c_python_timeout_invalid");
            Path workingDirectory = MesProBatchRecordExternalToolchainPreflight.resolveWorkingDirectory(
                    pythonWorkingDirectory, "route_c_python_working_directory_invalid");
            ProcessBuilder builder = new ProcessBuilder(
                    resolvedPythonCommand,
                    "-c",
                    PYTHON_SCRIPT,
                    sourcePath.toString(),
                    targetPath.toString()
            );
            builder.redirectErrorStream(true);
            builder.environment().put("PYTHONIOENCODING", "utf-8");
            if (workingDirectory != null) {
                builder.directory(workingDirectory.toFile());
            }
            Process process;
            try {
                process = builder.start();
            } catch (IOException ex) {
                throw new IOException("route_c_python_process_start_failed:" + ex.getMessage(), ex);
            }
            boolean finished;
            try {
                finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("route_c_normalization_interrupted", ex);
            }
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("route_c_normalization_timed_out");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IOException("route_c_normalization_failed:" + StrUtil.maxLength(output, 240));
            }
        }
    }

    private record HeaderChecklistSplit(String title, String checklist) {
    }

    private record HeaderCellRewrite(int cellIndex, String displayText, int rowHeightPx) {
    }

    private static final class ScaledSpan {

        private final int index;
        private final double fractionalPart;
        private int scaledSpan;

        private ScaledSpan(int index, int scaledSpan, double fractionalPart) {
            this.index = index;
            this.scaledSpan = scaledSpan;
            this.fractionalPart = fractionalPart;
        }

        private int index() {
            return index;
        }

        private double fractionalPart() {
            return fractionalPart;
        }
    }
}
