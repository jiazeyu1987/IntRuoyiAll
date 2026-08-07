package cn.iocoder.yudao.module.wordparser;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.HeaderStories;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefaultSharedWordDocumentParser implements SharedWordDocumentParser {

    private static final String PARSER_VERSION = "1";
    private static final Pattern LAST_INTEGER_PATTERN = Pattern.compile("\\d+");

    @Override
    public WordParseResult parse(WordParseCommand command) {
        if (command == null || command.source() == null || command.source().length == 0) {
            throw failure(WordParseFailureCode.EMPTY_SOURCE, command, 0, 0);
        }
        String extension = normalizeExtension(command.extension());
        if (!".doc".equals(extension) && !".docx".equals(extension)) {
            throw failure(WordParseFailureCode.UNSUPPORTED_SOURCE_TYPE, command, 0, 0);
        }
        if (command.profile() != WordParseProfile.STRUCTURAL_CANONICAL) {
            throw failure(WordParseFailureCode.UNSUPPORTED_SOURCE_TYPE, command, 0, 0);
        }

        try {
            ParsedDocument parsed = ".doc".equals(extension)
                    ? parseDoc(command.source()) : parseDocx(command.source());
            if (!parsed.hasContent()) {
                throw failure(WordParseFailureCode.NO_PARSEABLE_CONTENT, command, 0, 0);
            }
            WordParseDiagnostics diagnostics = diagnostics(command, parsed.paragraphs().size(),
                    parsed.tables().size(), null);
            return new WordParseResult(parsed.paragraphs(), parsed.documentFrame(), parsed.tables(), diagnostics);
        } catch (WordParseException ex) {
            throw ex;
        } catch (InvalidTableStructureException ex) {
            throw failure(WordParseFailureCode.INVALID_TABLE_STRUCTURE, command, 0, 0);
        } catch (Exception ex) {
            throw failure(WordParseFailureCode.CORRUPT_SOURCE, command, 0, 0);
        }
    }

    private ParsedDocument parseDoc(byte[] source) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(source))) {
            Range range = document.getRange();
            List<String> paragraphs = extractDocParagraphs(range);
            WordDocumentFrame frame = extractDocumentFrame(document);
            List<WordTable> tables = new ArrayList<>();
            List<Table> sourceTables = collectTopLevelTables(range);
            for (int index = 0; index < sourceTables.size(); index++) {
                tables.add(parseTable(sourceTables.get(index), index));
            }
            return new ParsedDocument(paragraphs, frame, tables);
        }
    }

    private ParsedDocument parseDocx(byte[] source) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source))) {
            DocxExtractionContext context = new DocxExtractionContext(resolveDocxTotalPageCount(document));
            List<String> paragraphs = document.getParagraphs().stream()
                    .map(paragraph -> normalizeDocxText(paragraph.getText(), paragraph.getCTP().xmlText(), context))
                    .filter(text -> !text.isBlank())
                    .toList();
            WordDocumentFrame frame = extractDocxDocumentFrame(document, context);
            List<WordTable> tables = new ArrayList<>();
            for (int index = 0; index < document.getTables().size(); index++) {
                tables.add(parseDocxTable(document.getTables().get(index), context, index));
            }
            return new ParsedDocument(paragraphs, frame, tables);
        }
    }

    private List<String> extractDocParagraphs(Range range) {
        List<String> paragraphs = new ArrayList<>();
        for (int index = 0; index < range.numParagraphs(); index++) {
            Paragraph paragraph = range.getParagraph(index);
            if (paragraph.isInTable()) {
                continue;
            }
            String text = normalizeText(paragraph.text());
            if (!text.isBlank()) {
                paragraphs.add(text);
            }
        }
        return List.copyOf(paragraphs);
    }

    private WordDocumentFrame extractDocumentFrame(HWPFDocument document) throws Exception {
        HeaderStories stories = new HeaderStories(document);
        return new WordDocumentFrame(
                firstNonEmptyStoryRows(stories.getFirstHeaderSubrange(), stories.getOddHeaderSubrange(),
                        stories.getEvenHeaderSubrange()),
                firstNonEmptyStoryRows(stories.getFirstFooterSubrange(), stories.getOddFooterSubrange(),
                        stories.getEvenFooterSubrange()));
    }

    private WordDocumentFrame extractDocxDocumentFrame(XWPFDocument document, DocxExtractionContext context) {
        return new WordDocumentFrame(
                firstNonEmptyDocxStoryRows(document.getHeaderList(), context),
                firstNonEmptyDocxStoryRows(document.getFooterList(), context));
    }

    private List<List<WordCell>> firstNonEmptyStoryRows(Range... ranges) throws Exception {
        for (Range range : ranges) {
            List<List<WordCell>> rows = storyRows(range);
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return List.of();
    }

    private List<List<WordCell>> storyRows(Range range) throws Exception {
        if (range == null || range.numParagraphs() == 0) {
            return List.of();
        }
        List<Table> tables = collectTopLevelTables(range);
        if (!tables.isEmpty()) {
            List<List<WordCell>> rows = parseTable(tables.get(0), 0).rows();
            if (hasMeaningfulText(rows)) {
                return rows;
            }
        }
        List<List<WordCell>> rows = new ArrayList<>();
        for (int index = 0; index < range.numParagraphs(); index++) {
            Paragraph paragraph = range.getParagraph(index);
            String text = normalizeText(paragraph.text());
            if (!text.isBlank()) {
                rows.add(List.of(paragraphCell(text, index == 0, index == 0 ? 10 : 8,
                        index == 0 ? "center" : "left", index == 0 ? 30 : 20)));
            }
        }
        return rows;
    }

    private List<List<WordCell>> firstNonEmptyDocxStoryRows(
            List<? extends XWPFHeaderFooter> stories, DocxExtractionContext context) {
        for (XWPFHeaderFooter story : stories) {
            List<List<WordCell>> rows = docxStoryRows(story, context);
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return List.of();
    }

    private List<List<WordCell>> docxStoryRows(XWPFHeaderFooter story, DocxExtractionContext context) {
        for (XWPFTable table : story.getTables()) {
            List<List<WordCell>> rows = parseDocxTable(table, context, 0).rows();
            if (hasMeaningfulText(rows)) {
                return rows;
            }
        }
        List<List<WordCell>> rows = new ArrayList<>();
        for (int index = 0; index < story.getParagraphs().size(); index++) {
            XWPFParagraph paragraph = story.getParagraphs().get(index);
            String text = normalizeDocxText(paragraph.getText(), paragraph.getCTP().xmlText(), context);
            if (!text.isBlank()) {
                rows.add(List.of(paragraphCell(text, resolveDocxParagraphBold(paragraph),
                        resolveDocxParagraphFontSize(paragraph, index == 0 ? 10 : 8),
                        resolveDocxParagraphHorizontalAlign(paragraph), index == 0 ? 30 : 20)));
            }
        }
        return rows;
    }

    private WordCell paragraphCell(String text, boolean bold, int fontSize, String align, int height) {
        return new WordCell(text, 1, 1, null, null, null, bold, fontSize, align, "center", 120, height,
                false, null, null, null, null, null);
    }

    private WordTable parseTable(Table table, int sourceIndex) {
        List<List<WordCell>> rows = new ArrayList<>();
        List<Integer> boundaries = resolveVisualColumnBoundaries(table);
        List<Integer> columnWidths = resolveVisualColumnWidths(boundaries);
        int columnCount = columnWidths.size();
        int[] blockedUntilRow = new int[Math.max(256, columnCount + 64)];
        java.util.Arrays.fill(blockedUntilRow, -1);
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            List<WordCell> cells = new ArrayList<>();
            int logicalColumnIndex = 0;
            int rowHeight = toPixels(row.getRowHeight(), 36);
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                while (logicalColumnIndex < blockedUntilRow.length
                        && blockedUntilRow[logicalColumnIndex] >= rowIndex) {
                    logicalColumnIndex++;
                }
                int columnIndex = resolveStartColumnIndex(boundaries, cell);
                int visualSpan = resolveVisualColSpan(boundaries, cell, columnIndex);
                int logicalSpan = resolveColSpan(row, cellIndex);
                int rowSpan = resolveRowSpan(table, rowIndex, cellIndex);
                WordCell parsed = new WordCell(normalizeText(cell.text()), rowSpan, visualSpan, columnIndex,
                        logicalColumnIndex, logicalSpan, resolveBold(cell), resolveFontSize(cell),
                        resolveHorizontalAlign(cell), resolveVerticalAlign(cell), toWidthUnits(cell.getWidth()),
                        rowHeight, false, resolveBorderStyle(cell.getBrcTop()),
                        resolveBorderStyle(cell.getBrcBottom()), resolveBorderStyle(cell.getBrcLeft()),
                        resolveBorderStyle(cell.getBrcRight()), null);
                cells.add(parsed);
                if (rowSpan > 1) {
                    for (int offset = 0; offset < logicalSpan
                            && logicalColumnIndex + offset < blockedUntilRow.length; offset++) {
                        blockedUntilRow[logicalColumnIndex + offset] = rowIndex + rowSpan - 1;
                    }
                }
                logicalColumnIndex += logicalSpan;
            }
            columnCount = Math.max(columnCount, resolveRowEndColumn(cells));
            rows.add(cells);
        }
        if (columnWidths.isEmpty() && columnCount > 0) {
            columnWidths = defaultWidths(columnCount);
        }
        return new WordTable(sourceIndex, rows.size(), columnCount, columnWidths, rows);
    }

    private WordTable parseDocxTable(XWPFTable table, DocxExtractionContext context, int sourceIndex) {
        validateDocxTable(table);
        List<List<WordCell>> rows = new ArrayList<>();
        int columnCount = 0;
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            List<WordCell> cells = new ArrayList<>();
            int logicalColumnIndex = 0;
            int rowHeight = resolveDocxRowHeightPx(row);
            for (int cellIndex = 0; cellIndex < row.getTableCells().size(); cellIndex++) {
                XWPFTableCell cell = row.getCell(cellIndex);
                int colSpan = resolveDocxColSpan(cell);
                int currentColumnIndex = logicalColumnIndex;
                logicalColumnIndex += colSpan;
                if (isDocxVerticalMergeFollower(cell)) {
                    continue;
                }
                int rowSpan = resolveDocxRowSpan(table, rowIndex, cellIndex);
                CTTcBorders borders = borders(cell);
                cells.add(new WordCell(
                        normalizeDocxText(resolveDocxCellText(cell), cell.getCTTc().xmlText(), context),
                        rowSpan, colSpan, currentColumnIndex, currentColumnIndex, colSpan,
                        resolveDocxBold(cell), resolveDocxFontSize(cell), resolveDocxHorizontalAlign(cell),
                        resolveDocxVerticalAlign(cell), resolveDocxWidthPx(cell, colSpan), rowHeight,
                        hasDocxDiagonalBorder(borders), borderStyle(borders == null ? null : borders.getTop()),
                        borderStyle(borders == null ? null : borders.getBottom()),
                        borderStyle(borders == null ? null : borders.getLeft()),
                        borderStyle(borders == null ? null : borders.getRight()), backgroundColor(cell)));
            }
            columnCount = Math.max(columnCount, logicalColumnIndex);
            rows.add(cells);
        }
        List<Integer> columnWidths = resolveSegmentColumnWidths(rows, columnCount);
        if (columnWidths.size() != columnCount) {
            columnWidths = defaultWidths(columnCount);
        }
        return new WordTable(sourceIndex, rows.size(), columnCount, columnWidths, rows);
    }

    private void validateDocxTable(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                CTTcPr properties = properties(cell);
                if (properties != null && properties.isSetGridSpan()
                        && toInt(properties.getGridSpan().getVal(), 0) <= 0) {
                    throw new InvalidTableStructureException();
                }
            }
        }
    }

    private List<Table> collectTopLevelTables(Range range) throws Exception {
        Constructor<TableIterator> constructor = TableIterator.class.getDeclaredConstructor(Range.class, int.class);
        constructor.setAccessible(true);
        TableIterator iterator = constructor.newInstance(range, 1);
        List<Table> tables = new ArrayList<>();
        while (iterator.hasNext()) {
            tables.add(iterator.next());
        }
        return tables;
    }

    private List<Integer> resolveVisualColumnBoundaries(Table table) {
        TreeSet<Integer> boundaries = new TreeSet<>();
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            TableRow row = table.getRow(rowIndex);
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

    private List<Integer> resolveVisualColumnWidths(List<Integer> boundaries) {
        if (boundaries.size() < 2) {
            return List.of();
        }
        List<Integer> widths = new ArrayList<>();
        for (int index = 0; index < boundaries.size() - 1; index++) {
            widths.add(toWidthUnits(boundaries.get(index + 1) - boundaries.get(index)));
        }
        return widths;
    }

    private int resolveStartColumnIndex(List<Integer> boundaries, TableCell cell) {
        if (boundaries.size() < 2) {
            return 0;
        }
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < boundaries.size() - 1; index++) {
            int distance = Math.abs(boundaries.get(index) - cell.getLeftEdge());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private int resolveVisualColSpan(List<Integer> boundaries, TableCell cell, int startColumnIndex) {
        if (boundaries.size() < 2) {
            return 1;
        }
        int right = cell.getLeftEdge() + Math.max(1, cell.getWidth());
        int endBoundaryIndex = Math.min(boundaries.size() - 1, startColumnIndex + 1);
        int bestDistance = Integer.MAX_VALUE;
        for (int index = startColumnIndex + 1; index < boundaries.size(); index++) {
            int distance = Math.abs(boundaries.get(index) - right);
            if (distance < bestDistance) {
                bestDistance = distance;
                endBoundaryIndex = index;
            }
        }
        return Math.max(1, endBoundaryIndex - startColumnIndex);
    }

    private int resolveRowEndColumn(List<WordCell> row) {
        int max = 0;
        for (WordCell cell : row) {
            max = Math.max(max, cell.columnIndex() + Math.max(1, cell.colSpan()));
        }
        return max;
    }

    private List<Integer> resolveSegmentColumnWidths(List<List<WordCell>> rows, int columnCount) {
        List<Integer> best = List.of();
        int bestScore = -1;
        for (List<WordCell> row : rows) {
            int rowColumns = row.stream().mapToInt(cell -> Math.max(1, cell.colSpan())).sum();
            if (rowColumns != columnCount) {
                continue;
            }
            int singleSpans = (int) row.stream().filter(cell -> cell.colSpan() == 1).count();
            int score = rowColumns * 100 + singleSpans;
            if (score > bestScore) {
                best = expandRowToColumnWidths(row);
                bestScore = score;
            }
        }
        return best;
    }

    private List<Integer> expandRowToColumnWidths(List<WordCell> row) {
        List<Integer> widths = new ArrayList<>();
        for (WordCell cell : row) {
            int span = Math.max(1, cell.colSpan());
            int base = Math.max(1, cell.widthPx() / span);
            int remainder = Math.max(0, cell.widthPx() - base * span);
            for (int offset = 0; offset < span; offset++) {
                widths.add(base + (offset < remainder ? 1 : 0));
            }
        }
        return widths;
    }

    private List<Integer> defaultWidths(int count) {
        List<Integer> widths = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            widths.add(120);
        }
        return widths;
    }

    private boolean isMergedFollower(TableCell cell) {
        return cell.isMerged() && !cell.isFirstMerged()
                || cell.isVerticallyMerged() && !cell.isFirstVerticallyMerged();
    }

    private int resolveColSpan(TableRow row, int startCellIndex) {
        TableCell cell = row.getCell(startCellIndex);
        if (!cell.isFirstMerged()) {
            return 1;
        }
        int span = 1;
        for (int index = startCellIndex + 1; index < row.numCells(); index++) {
            TableCell nextCell = row.getCell(index);
            if (nextCell.isMerged() && !nextCell.isFirstMerged()) {
                span++;
            } else {
                break;
            }
        }
        return span;
    }

    private int resolveRowSpan(Table table, int rowIndex, int cellIndex) {
        TableCell cell = table.getRow(rowIndex).getCell(cellIndex);
        if (!cell.isFirstVerticallyMerged()) {
            return 1;
        }
        int span = 1;
        for (int index = rowIndex + 1; index < table.numRows(); index++) {
            TableRow nextRow = table.getRow(index);
            if (cellIndex >= nextRow.numCells()) {
                break;
            }
            TableCell nextCell = nextRow.getCell(cellIndex);
            if (nextCell.isVerticallyMerged() && !nextCell.isFirstVerticallyMerged()) {
                span++;
            } else {
                break;
            }
        }
        return span;
    }

    private boolean resolveBold(TableCell cell) {
        CharacterRun run = firstCharacterRun(cell);
        return run != null && run.isBold();
    }

    private int resolveFontSize(TableCell cell) {
        CharacterRun run = firstCharacterRun(cell);
        if (run == null) {
            return 10;
        }
        int size = run.getFontSize();
        return Math.max(10, size > 20 ? size / 2 : size);
    }

    private String resolveHorizontalAlign(TableCell cell) {
        Paragraph paragraph = firstParagraph(cell);
        if (paragraph == null) {
            return "left";
        }
        return switch (paragraph.getJustification()) {
            case 1 -> "center";
            case 2 -> "right";
            default -> "left";
        };
    }

    private String resolveVerticalAlign(TableCell cell) {
        return switch (cell.getVertAlign()) {
            case 1 -> "top";
            case 3 -> "bottom";
            default -> "center";
        };
    }

    private Paragraph firstParagraph(TableCell cell) {
        return cell.numParagraphs() == 0 ? null : cell.getParagraph(0);
    }

    private CharacterRun firstCharacterRun(TableCell cell) {
        Paragraph paragraph = firstParagraph(cell);
        return paragraph == null || paragraph.numCharacterRuns() == 0 ? null : paragraph.getCharacterRun(0);
    }

    private boolean isDocxVerticalMergeFollower(XWPFTableCell cell) {
        CTTcPr properties = properties(cell);
        if (properties == null || !properties.isSetVMerge()) {
            return false;
        }
        Object value = properties.getVMerge().getVal();
        return value == null || STMerge.CONTINUE.equals(value);
    }

    private boolean isDocxVerticalMergeRestart(XWPFTableCell cell) {
        CTTcPr properties = properties(cell);
        return properties != null && properties.isSetVMerge()
                && STMerge.RESTART.equals(properties.getVMerge().getVal());
    }

    private int resolveDocxColSpan(XWPFTableCell cell) {
        CTTcPr properties = properties(cell);
        return properties == null || !properties.isSetGridSpan()
                ? 1 : toInt(properties.getGridSpan().getVal(), 1);
    }

    private int resolveDocxRowSpan(XWPFTable table, int rowIndex, int cellIndex) {
        if (!isDocxVerticalMergeRestart(table.getRow(rowIndex).getCell(cellIndex))) {
            return 1;
        }
        int span = 1;
        for (int index = rowIndex + 1; index < table.getRows().size(); index++) {
            XWPFTableRow row = table.getRow(index);
            if (cellIndex >= row.getTableCells().size() || !isDocxVerticalMergeFollower(row.getCell(cellIndex))) {
                break;
            }
            span++;
        }
        return span;
    }

    private boolean resolveDocxBold(XWPFTableCell cell) {
        XWPFRun run = firstDocxRun(cell);
        return run != null && run.isBold();
    }

    private int resolveDocxFontSize(XWPFTableCell cell) {
        XWPFRun run = firstDocxRun(cell);
        return run == null || run.getFontSize() <= 0 ? 10 : Math.max(8, run.getFontSize());
    }

    private String resolveDocxHorizontalAlign(XWPFTableCell cell) {
        return cell.getParagraphs().isEmpty() ? "left"
                : horizontalAlign(cell.getParagraphs().get(0).getAlignment());
    }

    private String resolveDocxVerticalAlign(XWPFTableCell cell) {
        if (cell.getVerticalAlignment() == null) {
            return "center";
        }
        return switch (cell.getVerticalAlignment()) {
            case TOP -> "top";
            case BOTTOM -> "bottom";
            default -> "center";
        };
    }

    private String horizontalAlign(ParagraphAlignment alignment) {
        if (alignment == ParagraphAlignment.CENTER) {
            return "center";
        }
        if (alignment == ParagraphAlignment.RIGHT) {
            return "right";
        }
        return "left";
    }

    private boolean resolveDocxParagraphBold(XWPFParagraph paragraph) {
        XWPFRun run = firstDocxRun(paragraph);
        return run != null && run.isBold();
    }

    private int resolveDocxParagraphFontSize(XWPFParagraph paragraph, int defaultValue) {
        XWPFRun run = firstDocxRun(paragraph);
        return run == null || run.getFontSize() <= 0 ? defaultValue : Math.max(8, run.getFontSize());
    }

    private String resolveDocxParagraphHorizontalAlign(XWPFParagraph paragraph) {
        return horizontalAlign(paragraph.getAlignment());
    }

    private int resolveDocxWidthPx(XWPFTableCell cell, int colSpan) {
        CTTcPr properties = properties(cell);
        if (properties == null || !properties.isSetTcW()) {
            return Math.max(1, colSpan) * 120;
        }
        return toWidthUnits(toInt(properties.getTcW().getW(), Math.max(1, colSpan) * 1800));
    }

    private int resolveDocxRowHeightPx(XWPFTableRow row) {
        return row.getHeight() <= 0 ? 36 : Math.max(1, Math.round(row.getHeight() / 15.0f));
    }

    private String resolveDocxCellText(XWPFTableCell cell) {
        StringBuilder builder = new StringBuilder();
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            if (paragraph.getRuns().isEmpty()) {
                builder.append(paragraph.getText());
            } else {
                paragraph.getRuns().forEach(run -> builder.append(resolveDocxRunText(run)));
            }
        }
        return builder.toString().isBlank() ? cell.getText() : builder.toString();
    }

    private String resolveDocxRunText(XWPFRun run) {
        String text = run.text();
        if (text == null || text.isEmpty() || run.getUnderline() == null
                || run.getUnderline() == UnderlinePatterns.NONE
                || !text.replace('\u00A0', ' ').trim().isEmpty()) {
            return text == null ? "" : text;
        }
        return "_".repeat(Math.max(3, text.length()));
    }

    private int resolveDocxTotalPageCount(XWPFDocument document) {
        if (document.getProperties() == null || document.getProperties().getExtendedProperties() == null
                || document.getProperties().getExtendedProperties().getUnderlyingProperties() == null) {
            return 0;
        }
        var properties = document.getProperties().getExtendedProperties().getUnderlyingProperties();
        return properties.isSetPages() ? Math.max(0, properties.getPages()) : 0;
    }

    private String normalizeDocxText(String text, String sourceXml, DocxExtractionContext context) {
        String normalized = normalizeText(text);
        if (normalized.isBlank() || context.totalPageCount() <= 0 || sourceXml == null
                || !sourceXml.contains("NUMPAGES")) {
            return normalized;
        }
        Matcher matcher = LAST_INTEGER_PATTERN.matcher(normalized);
        int start = -1;
        int end = -1;
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
        }
        return start < 0 ? normalized
                : normalized.substring(0, start) + context.totalPageCount() + normalized.substring(end);
    }

    private CTTcPr properties(XWPFTableCell cell) {
        CTTc ctTc = cell.getCTTc();
        return ctTc == null || !ctTc.isSetTcPr() ? null : ctTc.getTcPr();
    }

    private CTTcBorders borders(XWPFTableCell cell) {
        CTTcPr properties = properties(cell);
        return properties == null || !properties.isSetTcBorders() ? null : properties.getTcBorders();
    }

    private boolean hasDocxDiagonalBorder(CTTcBorders borders) {
        return borders != null && (visibleBorder(borders.getTl2Br()) || visibleBorder(borders.getTr2Bl()));
    }

    private boolean visibleBorder(CTBorder border) {
        if (border == null || border.getVal() == null) {
            return border != null;
        }
        String value = border.getVal().toString();
        return !"none".equalsIgnoreCase(value) && !"nil".equalsIgnoreCase(value);
    }

    private String borderStyle(CTBorder border) {
        return visibleBorder(border) ? border.getVal() == null ? "single" : border.getVal().toString() : null;
    }

    private String backgroundColor(XWPFTableCell cell) {
        CTTcPr properties = properties(cell);
        if (properties == null || !properties.isSetShd() || properties.getShd().getFill() == null) {
            return null;
        }
        Object value = properties.getShd().getFill();
        String fill = value instanceof byte[] bytes
                ? HexFormat.of().formatHex(bytes) : value.toString();
        return "auto".equalsIgnoreCase(fill) || fill.isBlank() ? null : fill.toUpperCase(Locale.ROOT);
    }

    private XWPFRun firstDocxRun(XWPFTableCell cell) {
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            XWPFRun run = firstDocxRun(paragraph);
            if (run != null) {
                return run;
            }
        }
        return null;
    }

    private XWPFRun firstDocxRun(XWPFParagraph paragraph) {
        return paragraph.getRuns().isEmpty() ? null : paragraph.getRuns().get(0);
    }

    private boolean hasMeaningfulText(List<List<WordCell>> rows) {
        return rows.stream().flatMap(List::stream).anyMatch(cell -> !cell.text().isBlank());
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof BigInteger integer) {
            return integer.intValue();
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigInteger(value.toString()).intValue();
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private int toPixels(int twips, int defaultValue) {
        return twips <= 0 ? defaultValue : Math.max(defaultValue, Math.round(twips / 15.0f));
    }

    private int toWidthUnits(int twips) {
        return twips <= 0 ? 1 : Math.max(1, Math.round(twips / 15.0f));
    }

    private String resolveBorderStyle(BorderCode border) {
        if (border == null || border.isEmpty()) {
            return null;
        }
        if (border.getLineWidth() >= 24) {
            return "thick";
        }
        return border.getLineWidth() >= 12 ? "medium" : "thin";
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u0007', ' ').replace('\u0008', ' ').replace('\r', '\n')
                .replace('\u0000', ' ').replaceAll("[\\n]{3,}", "\n\n").trim();
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "";
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized : "." + normalized;
    }

    private WordParseException failure(WordParseFailureCode code, WordParseCommand command,
                                       int paragraphCount, int tableCount) {
        return new WordParseException(code, diagnostics(command, paragraphCount, tableCount, code));
    }

    private WordParseDiagnostics diagnostics(WordParseCommand command, int paragraphCount, int tableCount,
                                             WordParseFailureCode failureCode) {
        byte[] source = command == null || command.source() == null ? new byte[0] : command.source();
        String extension = command == null ? "" : normalizeExtension(command.extension());
        String fileName = command == null || command.originalFileName() == null ? "" : command.originalFileName();
        return new WordParseDiagnostics(PARSER_VERSION, hash(source), extension,
                hash(fileName.getBytes(StandardCharsets.UTF_8)), paragraphCount, tableCount, List.of(), failureCode);
    }

    private String hash(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private record ParsedDocument(
            List<String> paragraphs,
            WordDocumentFrame documentFrame,
            List<WordTable> tables) {

        private boolean hasContent() {
            return !paragraphs.isEmpty() || !tables.isEmpty()
                    || documentFrame.headerRows().stream().flatMap(List::stream).anyMatch(cell -> !cell.text().isBlank())
                    || documentFrame.footerRows().stream().flatMap(List::stream).anyMatch(cell -> !cell.text().isBlank());
        }
    }

    private record DocxExtractionContext(int totalPageCount) {
    }

    private static final class InvalidTableStructureException extends RuntimeException {
    }
}
