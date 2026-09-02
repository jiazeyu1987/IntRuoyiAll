package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

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
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
public class MesProBatchRecordDocParser {

    private static final Pattern LAST_INTEGER_PATTERN = Pattern.compile("\\d+");

    public List<MesProBatchRecordParsedTable> parseWord(byte[] bytes, String sourceFileName) {
        return isDocxSource(bytes, sourceFileName) ? parseDocx(bytes) : parse(bytes);
    }

    public MesProBatchRecordDocumentFrame extractWordDocumentFrame(byte[] bytes, String sourceFileName) {
        return isDocxSource(bytes, sourceFileName) ? extractDocxDocumentFrame(bytes) : extractDocumentFrame(bytes);
    }

    public List<MesProBatchRecordParsedTable> parse(byte[] bytes) {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes))) {
            Range range = document.getRange();
            MesProBatchRecordDocumentFrame documentFrame = extractDocumentFrame(document);
            List<MesProBatchRecordParsedTable> tables = new ArrayList<>();
            int sourceTopLevelTableIndex = 1;
            for (Table table : collectTopLevelTables(range)) {
                List<MesProBatchRecordParsedTable> splitTables = splitTemplates(parseTable(table));
                for (int splitIndex = 0; splitIndex < splitTables.size(); splitIndex++) {
                    splitTables.get(splitIndex).setSourceTopLevelTableIndex(sourceTopLevelTableIndex);
                    splitTables.get(splitIndex).setSourceSplitIndex(splitIndex + 1);
                }
                tables.addAll(splitTables);
                sourceTopLevelTableIndex++;
            }
            for (int index = 0; index < tables.size(); index++) {
                tables.get(index).setSourceTableIndex(index + 1);
                tables.get(index).setDocumentFrame(documentFrame);
            }
            return tables;
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    ex.getMessage());
        }
    }

    public MesProBatchRecordDocumentFrame extractDocumentFrame(byte[] bytes) {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes))) {
            return extractDocumentFrame(document);
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    ex.getMessage());
        }
    }

    public List<MesProBatchRecordParsedTable> parseDocx(byte[] bytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            DocxExtractionContext extractionContext = resolveDocxExtractionContext(document);
            MesProBatchRecordDocumentFrame documentFrame = extractDocxDocumentFrame(document, extractionContext);
            List<MesProBatchRecordParsedTable> tables = new ArrayList<>();
            int sourceTopLevelTableIndex = 1;
            for (XWPFTable table : document.getTables()) {
                List<MesProBatchRecordParsedTable> splitTables = splitTemplates(parseDocxTable(table, extractionContext));
                for (int splitIndex = 0; splitIndex < splitTables.size(); splitIndex++) {
                    splitTables.get(splitIndex).setSourceTopLevelTableIndex(sourceTopLevelTableIndex);
                    splitTables.get(splitIndex).setSourceSplitIndex(splitIndex + 1);
                }
                tables.addAll(splitTables);
                sourceTopLevelTableIndex++;
            }
            for (int index = 0; index < tables.size(); index++) {
                tables.get(index).setSourceTableIndex(index + 1);
                tables.get(index).setDocumentFrame(documentFrame);
            }
            return tables;
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    ex.getMessage());
        }
    }

    private boolean isDocxSource(byte[] bytes, String sourceFileName) {
        String lowerFileName = Objects.toString(sourceFileName, "").trim().toLowerCase(Locale.ROOT);
        if (lowerFileName.endsWith(".docx")) {
            return true;
        }
        if (lowerFileName.endsWith(".doc")) {
            return false;
        }
        return hasZipPackageHeader(bytes);
    }

    private boolean hasZipPackageHeader(byte[] bytes) {
        return bytes != null
                && bytes.length >= 4
                && bytes[0] == 0x50
                && bytes[1] == 0x4B
                && bytes[2] == 0x03
                && bytes[3] == 0x04;
    }

    public MesProBatchRecordDocumentFrame extractDocxDocumentFrame(byte[] bytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return extractDocxDocumentFrame(document, resolveDocxExtractionContext(document));
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    ex.getMessage());
        }
    }

    private MesProBatchRecordDocumentFrame extractDocumentFrame(HWPFDocument document) throws Exception {
        HeaderStories stories = new HeaderStories(document);
        return MesProBatchRecordDocumentFrame.builder()
                .headerRows(firstNonEmptyStoryRows(
                        stories.getFirstHeaderSubrange(),
                        stories.getOddHeaderSubrange(),
                        stories.getEvenHeaderSubrange()))
                .footerRows(firstNonEmptyStoryRows(
                        stories.getFirstFooterSubrange(),
                        stories.getOddFooterSubrange(),
                        stories.getEvenFooterSubrange()))
                .build();
    }

    private MesProBatchRecordDocumentFrame extractDocxDocumentFrame(XWPFDocument document,
                                                                   DocxExtractionContext extractionContext) {
        return MesProBatchRecordDocumentFrame.builder()
                .headerRows(firstNonEmptyDocxStoryRows(document.getHeaderList(), extractionContext))
                .footerRows(firstNonEmptyDocxStoryRows(document.getFooterList(), extractionContext))
                .build();
    }

    private List<List<MesProBatchRecordParsedCell>> firstNonEmptyDocxStoryRows(
            List<? extends XWPFHeaderFooter> stories, DocxExtractionContext extractionContext) {
        if (stories == null || stories.isEmpty()) {
            return List.of();
        }
        for (XWPFHeaderFooter story : stories) {
            List<List<MesProBatchRecordParsedCell>> rows = docxStoryRows(story, extractionContext);
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return List.of();
    }

    private List<List<MesProBatchRecordParsedCell>> docxStoryRows(XWPFHeaderFooter story,
                                                                  DocxExtractionContext extractionContext) {
        if (story == null) {
            return List.of();
        }
        for (XWPFTable table : story.getTables()) {
            MesProBatchRecordParsedTable parsed = parseDocxTable(table, extractionContext);
            if (hasMeaningfulText(parsed.getRows())) {
                return parsed.getRows();
            }
        }
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        List<XWPFParagraph> paragraphs = story.getParagraphs();
        for (int index = 0; index < paragraphs.size(); index++) {
            XWPFParagraph paragraph = paragraphs.get(index);
            String text = normalizeDocxText(paragraph.getText(), paragraph.getCTP().xmlText(), extractionContext);
            if (text.isBlank()) {
                continue;
            }
            rows.add(List.of(MesProBatchRecordParsedCell.builder()
                    .text(text)
                    .rowSpan(1)
                    .colSpan(1)
                    .bold(resolveDocxParagraphBold(paragraph))
                    .fontSize(resolveDocxParagraphFontSize(paragraph, index == 0 ? 10 : 8))
                    .horizontalAlign(resolveDocxParagraphHorizontalAlign(paragraph))
                    .verticalAlign("middle")
                    .heightPx(index == 0 ? 30 : 20)
                    .build()));
        }
        return rows;
    }

    private boolean hasMeaningfulText(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        return rows.stream()
                .flatMap(List::stream)
                .map(MesProBatchRecordParsedCell::getText)
                .anyMatch(text -> text != null && !text.isBlank());
    }

    private List<List<MesProBatchRecordParsedCell>> firstNonEmptyStoryRows(Range... ranges) throws Exception {
        for (Range range : ranges) {
            List<List<MesProBatchRecordParsedCell>> rows = storyRows(range);
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return List.of();
    }

    private List<List<MesProBatchRecordParsedCell>> storyRows(Range range) throws Exception {
        if (range == null || range.numParagraphs() == 0) {
            return List.of();
        }
        for (Table table : collectTopLevelTables(range)) {
            MesProBatchRecordParsedTable parsed = parseTable(table);
            if (parsed.getRows() != null && !parsed.getRows().isEmpty()) {
                return parsed.getRows();
            }
        }
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        for (int index = 0; index < range.numParagraphs(); index++) {
            String text = normalizeCellText(range.getParagraph(index).text());
            if (text.isBlank()) {
                continue;
            }
            rows.add(List.of(MesProBatchRecordParsedCell.builder()
                    .text(text)
                    .rowSpan(1)
                    .colSpan(1)
                    .bold(index == 0)
                    .fontSize(index == 0 ? 10 : 8)
                    .horizontalAlign(index == 0 ? "center" : "left")
                    .verticalAlign("middle")
                    .heightPx(index == 0 ? 30 : 20)
                    .build()));
        }
        return rows;
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

    private MesProBatchRecordParsedTable parseTable(Table table) {
        List<List<MesProBatchRecordParsedCell>> parsedRows = new ArrayList<>();
        String tableTitle = "";
        List<Integer> visualColumnBoundaries = resolveVisualColumnBoundaries(table);
        List<Integer> visualColumnWidths = resolveVisualColumnWidths(visualColumnBoundaries);
        int maxColumnCount = visualColumnWidths.size();
        int[] logicalBlockedUntilRowByColumn = new int[Math.max(256, maxColumnCount + 64)];
        for (int index = 0; index < logicalBlockedUntilRowByColumn.length; index++) {
            logicalBlockedUntilRowByColumn[index] = -1;
        }
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            List<MesProBatchRecordParsedCell> parsedCells = new ArrayList<>();
            int rowHeightPx = toPixels(row.getRowHeight(), 36);
            int logicalColumnIndex = 0;
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                String text = normalizeCellText(cell.text());
                int columnIndex = resolveStartColumnIndex(visualColumnBoundaries, cell);
                int visualColSpan = resolveVisualColSpan(visualColumnBoundaries, cell, columnIndex);
                while (logicalColumnIndex < logicalBlockedUntilRowByColumn.length
                        && logicalBlockedUntilRowByColumn[logicalColumnIndex] >= rowIndex) {
                    logicalColumnIndex++;
                }
                int logicalColSpan = resolveColSpan(row, cellIndex);
                int rowSpan = resolveRowSpan(table, rowIndex, cellIndex);
                MesProBatchRecordParsedCell parsedCell = MesProBatchRecordParsedCell.builder()
                        .text(text)
                        .rowSpan(rowSpan)
                        .colSpan(visualColSpan)
                        .columnIndex(columnIndex)
                        .logicalColumnIndex(logicalColumnIndex)
                        .logicalColSpan(logicalColSpan)
                        .bold(resolveBold(cell))
                        .fontSize(resolveFontSize(cell))
                        .horizontalAlign(resolveHorizontalAlign(cell))
                        .verticalAlign(resolveVerticalAlign(cell))
                        .widthPx(toWidthUnits(cell.getWidth()))
                        .heightPx(rowHeightPx)
                        .topBorderStyle(resolveBorderStyle(cell.getBrcTop()))
                        .bottomBorderStyle(resolveBorderStyle(cell.getBrcBottom()))
                        .leftBorderStyle(resolveBorderStyle(cell.getBrcLeft()))
                        .rightBorderStyle(resolveBorderStyle(cell.getBrcRight()))
                        .build();
                parsedCells.add(parsedCell);
                if (rowSpan > 1) {
                    for (int offset = 0; offset < logicalColSpan
                            && logicalColumnIndex + offset < logicalBlockedUntilRowByColumn.length; offset++) {
                        logicalBlockedUntilRowByColumn[logicalColumnIndex + offset] = rowIndex + rowSpan - 1;
                    }
                }
                logicalColumnIndex += logicalColSpan;
                if (tableTitle.isBlank() && !text.isBlank()) {
                    tableTitle = extractTemplateTitle(text);
                }
            }
            maxColumnCount = Math.max(maxColumnCount, resolveRowEndColumn(parsedCells));
            parsedRows.add(parsedCells);
        }
        tableTitle = MesProBatchRecordSharedPageTitleRules.resolveRepresentativeTitle(tableTitle, parsedRows);
        if (tableTitle.isBlank()) {
            tableTitle = "\u8868";
        }
        return MesProBatchRecordParsedTable.builder()
                .tableTitle(tableTitle)
                .rowCount(parsedRows.size())
                .columnCount(maxColumnCount)
                .columnWidths(visualColumnWidths)
                .rows(parsedRows)
                .build();
    }

    private MesProBatchRecordParsedTable parseDocxTable(XWPFTable table) {
        return parseDocxTable(table, DocxExtractionContext.EMPTY);
    }

    private MesProBatchRecordParsedTable parseDocxTable(XWPFTable table, DocxExtractionContext extractionContext) {
        List<List<MesProBatchRecordParsedCell>> parsedRows = new ArrayList<>();
        String tableTitle = "";
        int maxColumnCount = 0;
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            List<MesProBatchRecordParsedCell> parsedCells = new ArrayList<>();
            int logicalColumnIndex = 0;
            int rowHeightPx = resolveDocxRowHeightPx(row);
            for (int cellIndex = 0; cellIndex < row.getTableCells().size(); cellIndex++) {
                XWPFTableCell cell = row.getCell(cellIndex);
                if (isDocxVerticalMergeFollower(cell)) {
                    continue;
                }
                int colSpan = resolveDocxColSpan(cell);
                int rowSpan = resolveDocxRowSpan(table, rowIndex, cellIndex);
                String text = normalizeDocxText(resolveDocxCellText(cell), cell.getCTTc().xmlText(), extractionContext);
                MesProBatchRecordParsedCell parsedCell = MesProBatchRecordParsedCell.builder()
                        .text(text)
                        .rowSpan(rowSpan)
                        .colSpan(colSpan)
                        .columnIndex(logicalColumnIndex)
                        .logicalColumnIndex(logicalColumnIndex)
                        .logicalColSpan(colSpan)
                        .bold(resolveDocxBold(cell))
                        .fontSize(resolveDocxFontSize(cell))
                        .horizontalAlign(resolveDocxHorizontalAlign(cell))
                        .verticalAlign("middle")
                        .widthPx(resolveDocxWidthPx(cell, colSpan))
                        .heightPx(rowHeightPx)
                        .diagonalSlash(hasDocxDiagonalBorder(cell))
                        .topBorderStyle("solid")
                        .bottomBorderStyle("solid")
                        .leftBorderStyle("solid")
                        .rightBorderStyle("solid")
                        .build();
                parsedCells.add(parsedCell);
                logicalColumnIndex += colSpan;
                if (tableTitle.isBlank() && !text.isBlank()) {
                    tableTitle = extractTemplateTitle(text);
                }
            }
            maxColumnCount = Math.max(maxColumnCount, logicalColumnIndex);
            parsedRows.add(parsedCells);
        }
        tableTitle = MesProBatchRecordSharedPageTitleRules.resolveRepresentativeTitle(tableTitle, parsedRows);
        if (tableTitle.isBlank()) {
            tableTitle = "\u8868";
        }
        return MesProBatchRecordParsedTable.builder()
                .tableTitle(tableTitle)
                .rowCount(parsedRows.size())
                .columnCount(maxColumnCount)
                .columnWidths(resolveDocxColumnWidths(parsedRows, maxColumnCount))
                .rows(parsedRows)
                .build();
    }

    private DocxExtractionContext resolveDocxExtractionContext(XWPFDocument document) {
        return new DocxExtractionContext(resolveDocxTotalPageCount(document));
    }

    private int resolveDocxTotalPageCount(XWPFDocument document) {
        if (document == null || document.getProperties() == null
                || document.getProperties().getExtendedProperties() == null
                || document.getProperties().getExtendedProperties().getUnderlyingProperties() == null) {
            return 0;
        }
        var appProperties = document.getProperties().getExtendedProperties().getUnderlyingProperties();
        if (!appProperties.isSetPages()) {
            return 0;
        }
        return Math.max(0, appProperties.getPages());
    }

    private String normalizeDocxText(String text, String sourceXml, DocxExtractionContext extractionContext) {
        String normalized = normalizeCellText(text);
        if (normalized.isBlank()
                || extractionContext == null
                || extractionContext.totalPageCount() <= 0
                || sourceXml == null
                || !sourceXml.contains("NUMPAGES")) {
            return normalized;
        }
        return replaceCachedTotalPageCount(normalized, extractionContext.totalPageCount());
    }

    private String resolveDocxCellText(XWPFTableCell cell) {
        if (cell == null || cell.getParagraphs() == null || cell.getParagraphs().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            if (paragraph == null || paragraph.getRuns().isEmpty()) {
                builder.append(paragraph == null ? "" : paragraph.getText());
                continue;
            }
            for (XWPFRun run : paragraph.getRuns()) {
                builder.append(resolveDocxRunText(run));
            }
        }
        String text = builder.toString();
        String nestedTableText = resolveDocxNestedTableText(cell);
        if (!nestedTableText.isBlank()) {
            text = text.isBlank() ? nestedTableText : text + "\n" + nestedTableText;
        }
        return text.isBlank() ? cell.getText() : text;
    }

    private String resolveDocxNestedTableText(XWPFTableCell cell) {
        if (cell == null || cell.getTables() == null || cell.getTables().isEmpty()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (XWPFTable nestedTable : cell.getTables()) {
            if (nestedTable == null || nestedTable.getRows() == null) {
                continue;
            }
            for (XWPFTableRow nestedRow : nestedTable.getRows()) {
                List<String> values = new ArrayList<>();
                for (XWPFTableCell nestedCell : nestedRow.getTableCells()) {
                    values.add(resolveDocxOwnCellText(nestedCell));
                }
                String line = String.join("\t", values).trim();
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        return String.join("\n", lines);
    }

    private String resolveDocxOwnCellText(XWPFTableCell cell) {
        if (cell == null || cell.getParagraphs() == null || cell.getParagraphs().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            if (paragraph == null || paragraph.getRuns().isEmpty()) {
                builder.append(paragraph == null ? "" : paragraph.getText());
                continue;
            }
            for (XWPFRun run : paragraph.getRuns()) {
                builder.append(resolveDocxRunText(run));
            }
        }
        return builder.toString().trim();
    }

    private String resolveDocxRunText(XWPFRun run) {
        if (run == null) {
            return "";
        }
        String text = run.text();
        if (!isUnderlinedBlankRun(run, text)) {
            return text;
        }
        return "_".repeat(Math.max(3, text.length()));
    }

    private boolean isUnderlinedBlankRun(XWPFRun run, String text) {
        if (run == null || text == null || text.isEmpty()) {
            return false;
        }
        UnderlinePatterns underline = run.getUnderline();
        if (underline == null || underline == UnderlinePatterns.NONE) {
            return false;
        }
        return text.replace('\u00A0', ' ').trim().isEmpty();
    }

    private String replaceCachedTotalPageCount(String text, int totalPageCount) {
        Matcher matcher = LAST_INTEGER_PATTERN.matcher(text);
        int start = -1;
        int end = -1;
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
        }
        if (start < 0) {
            return text;
        }
        return text.substring(0, start) + totalPageCount + text.substring(end);
    }

    private int resolveDocxRowHeightPx(XWPFTableRow row) {
        int twips = row == null ? 0 : row.getHeight();
        if (twips <= 0) {
            return 36;
        }
        return Math.max(1, Math.round(twips / 15.0f));
    }

    private boolean hasDocxDiagonalBorder(XWPFTableCell cell) {
        if (cell == null) {
            return false;
        }
        CTTc ctTc = cell.getCTTc();
        if (ctTc == null || !ctTc.isSetTcPr()) {
            return false;
        }
        CTTcPr tcPr = ctTc.getTcPr();
        if (tcPr == null || !tcPr.isSetTcBorders()) {
            return false;
        }
        CTTcBorders borders = tcPr.getTcBorders();
        return isVisibleDocxBorder(borders.getTl2Br()) || isVisibleDocxBorder(borders.getTr2Bl());
    }

    private boolean isVisibleDocxBorder(CTBorder border) {
        if (border == null) {
            return false;
        }
        STBorder.Enum value = border.getVal();
        if (value == null) {
            return true;
        }
        String normalized = value.toString();
        return !"none".equalsIgnoreCase(normalized) && !"nil".equalsIgnoreCase(normalized);
    }

    private List<MesProBatchRecordParsedTable> splitTemplates(MesProBatchRecordParsedTable parsedTable) {
        List<Integer> headerIndexes = findTemplateHeaderIndexes(parsedTable.getRows());
        if (headerIndexes.isEmpty() || headerIndexes.size() == 1 && headerIndexes.get(0) == 0) {
            if (shouldPreserveFullLeadingShortTitleTable(parsedTable.getRows())) {
                String leadingTitle = resolveLeadingSharedTitle(parsedTable.getTableTitle(), parsedTable.getRows());
                return List.of(buildParsedTable(leadingTitle,
                        copyRows(parsedTable.getRows(), 0, parsedTable.getRows().size()),
                        parsedTable.getColumnWidths()));
            }
            List<List<MesProBatchRecordParsedCell>> representativeRows =
                    MesProBatchRecordSharedPageTitleRules.resolveRepresentativeRows(parsedTable.getRows());
            String representativeTitle = MesProBatchRecordSharedPageTitleRules.resolveRepresentativeTitle(
                    parsedTable.getTableTitle(), representativeRows);
            return List.of(buildParsedTable(representativeTitle, representativeRows, parsedTable.getColumnWidths()));
        }

        List<MesProBatchRecordParsedTable> templates = new ArrayList<>();
        for (int index = 0; index < headerIndexes.size(); index++) {
            int segmentStart = index == 0 ? 0 : headerIndexes.get(index);
            int titleRowIndex = headerIndexes.get(index);
            int segmentEnd = index + 1 < headerIndexes.size() ? headerIndexes.get(index + 1) : parsedTable.getRows().size();
            List<List<MesProBatchRecordParsedCell>> segmentRows = copyRows(parsedTable.getRows(), segmentStart, segmentEnd);
            String title = extractTemplateTitle(firstNonBlankRowText(parsedTable.getRows().get(titleRowIndex)));
            templates.add(buildParsedTable(title, segmentRows, parsedTable.getColumnWidths()));
        }
        return templates;
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
        int visualMaxColumnCount = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            visualMaxColumnCount = Math.max(visualMaxColumnCount, resolveRowEndColumn(row));
        }
        List<Integer> logicalColumnWidths = resolveLogicalColumnWidths(rows);
        List<List<MesProBatchRecordParsedCell>> logicalRows = normalizeRowsToLogicalGrid(rows, logicalColumnWidths);
        int logicalMaxColumnCount = 0;
        for (List<MesProBatchRecordParsedCell> row : logicalRows) {
            logicalMaxColumnCount = Math.max(logicalMaxColumnCount, resolveRowEndColumn(row));
        }
        int effectiveVisualColumnCount = resolveEffectiveVisualColumnCount(rows, visualMaxColumnCount);
        boolean useSourceVisualGrid = shouldUseSourceVisualColumnWidths(rows, sourceColumnWidths,
                effectiveVisualColumnCount, logicalMaxColumnCount);
        if (useSourceVisualGrid && !canProjectRowsToVisualGrid(rows, visualMaxColumnCount, effectiveVisualColumnCount)) {
            useSourceVisualGrid = false;
        }
        List<List<MesProBatchRecordParsedCell>> outputRows = useSourceVisualGrid
                ? normalizeRowsToVisualGrid(rows, effectiveVisualColumnCount)
                : logicalRows;
        int maxColumnCount = useSourceVisualGrid ? effectiveVisualColumnCount : logicalMaxColumnCount;
        List<Integer> columnWidths = useSourceVisualGrid
                ? cropColumnWidths(sourceColumnWidths, effectiveVisualColumnCount)
                : logicalColumnWidths;
        if (!useSourceVisualGrid && (columnWidths == null || columnWidths.size() != maxColumnCount)) {
            columnWidths = resolveSegmentColumnWidths(outputRows, maxColumnCount);
        }
        if (!columnWidths.isEmpty()) {
            maxColumnCount = Math.max(maxColumnCount, columnWidths.size());
        }
        return MesProBatchRecordParsedTable.builder()
                .tableTitle(title)
                .rowCount(outputRows.size())
                .columnCount(maxColumnCount)
                .columnWidths(columnWidths == null ? List.of() : columnWidths)
                .rows(outputRows)
                .build();
    }

    private int resolveEffectiveVisualColumnCount(List<List<MesProBatchRecordParsedCell>> rows, int visualMaxColumnCount) {
        if (rows == null || rows.isEmpty()) {
            return visualMaxColumnCount;
        }
        int effectiveColumnCount = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            int rowEndColumn = resolveRowEndColumn(row);
            if (rowEndColumn <= 0 || isFullWidthDecorativeRow(row, visualMaxColumnCount)) {
                continue;
            }
            effectiveColumnCount = Math.max(effectiveColumnCount, rowEndColumn);
        }
        return effectiveColumnCount > 0 ? effectiveColumnCount : visualMaxColumnCount;
    }

    private boolean canProjectRowsToVisualGrid(List<List<MesProBatchRecordParsedCell>> rows,
                                               int visualMaxColumnCount,
                                               int effectiveVisualColumnCount) {
        if (rows == null || rows.isEmpty() || effectiveVisualColumnCount <= 0) {
            return false;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            int rowEndColumn = resolveRowEndColumn(row);
            if (rowEndColumn <= effectiveVisualColumnCount) {
                continue;
            }
            if (isFullWidthDecorativeRow(row, visualMaxColumnCount)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean isFullWidthDecorativeRow(List<MesProBatchRecordParsedCell> row, int visualMaxColumnCount) {
        if (row == null || row.size() != 1 || visualMaxColumnCount <= 0) {
            return false;
        }
        MesProBatchRecordParsedCell cell = row.get(0);
        if (cell == null || cell.getText() == null || cell.getText().isBlank()) {
            return false;
        }
        int startColumn = cell.getColumnIndex() == null ? 0 : cell.getColumnIndex();
        int endColumn = startColumn + Math.max(1, cell.getColSpan());
        return startColumn == 0 && endColumn >= Math.max(1, visualMaxColumnCount - 1);
    }

    private List<List<MesProBatchRecordParsedCell>> normalizeRowsToVisualGrid(
            List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<List<MesProBatchRecordParsedCell>> normalizedRows = new ArrayList<>();
        for (List<MesProBatchRecordParsedCell> row : rows) {
            List<MesProBatchRecordParsedCell> normalizedRow = new ArrayList<>();
            int runningColumn = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null) {
                    continue;
                }
                int startColumn = cell.getColumnIndex() == null ? runningColumn : cell.getColumnIndex();
                startColumn = Math.min(Math.max(0, startColumn), Math.max(0, columnCount - 1));
                int colSpan = Math.max(1, cell.getColSpan());
                colSpan = Math.min(colSpan, Math.max(1, columnCount - startColumn));
                normalizedRow.add(copyCellForGrid(cell, startColumn, colSpan));
                runningColumn = startColumn + colSpan;
            }
            normalizedRows.add(normalizedRow);
        }
        return normalizedRows;
    }

    private List<Integer> cropColumnWidths(List<Integer> sourceColumnWidths, int columnCount) {
        if (sourceColumnWidths == null || sourceColumnWidths.isEmpty() || columnCount <= 0) {
            return List.of();
        }
        return new ArrayList<>(sourceColumnWidths.subList(0, Math.min(sourceColumnWidths.size(), columnCount)));
    }

    private boolean shouldUseSourceVisualColumnWidths(List<List<MesProBatchRecordParsedCell>> rows,
                                                      List<Integer> sourceColumnWidths,
                                                      int visualMaxColumnCount,
                                                      int logicalMaxColumnCount) {
        if (sourceColumnWidths == null || sourceColumnWidths.isEmpty()
                || sourceColumnWidths.size() < visualMaxColumnCount
                || logicalMaxColumnCount <= 0) {
            return false;
        }
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        int densestVisualRowCellCount = rows.stream()
                .filter(row -> row != null)
                .mapToInt(List::size)
                .max()
                .orElse(0);
        boolean highDensityBoundaryGrid = sourceColumnWidths.size() >= 60
                && visualMaxColumnCount >= 60
                && visualMaxColumnCount >= logicalMaxColumnCount * 3;
        boolean hasWideMergedVisualCells = rows.stream()
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null && Math.max(1, cell.getColSpan()) >= 20);
        boolean sparseRowsOnDenseGrid = densestVisualRowCellCount > 0
                && visualMaxColumnCount >= densestVisualRowCellCount * 6;
        boolean hasPackedInteriorGrid = rows.stream()
                .anyMatch(row -> hasPackedInteriorGridRow(row, visualMaxColumnCount));
        return highDensityBoundaryGrid && hasPackedInteriorGrid
                && (hasWideMergedVisualCells || sparseRowsOnDenseGrid);
    }

    private boolean hasPackedInteriorGridRow(List<MesProBatchRecordParsedCell> row, int visualMaxColumnCount) {
        if (row == null || row.size() < 2 || visualMaxColumnCount < 60) {
            return false;
        }
        int wideCellSpanThreshold = Math.max(20, Math.round(visualMaxColumnCount * 0.35f));
        for (int index = 1; index < row.size(); index++) {
            MesProBatchRecordParsedCell cell = row.get(index);
            if (cell == null || Math.max(1, cell.getColSpan()) < wideCellSpanThreshold) {
                continue;
            }
            if (isPackedLabelGridText(cell.getText())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPackedLabelGridText(String text) {
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

    private List<List<MesProBatchRecordParsedCell>> normalizeRowsToLogicalGrid(List<List<MesProBatchRecordParsedCell>> rows,
                                                                               List<Integer> logicalColumnWidths) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<List<MesProBatchRecordParsedCell>> normalizedRows = new ArrayList<>();
        int maxSourceColumnCount = rows.stream()
                .filter(row -> row != null)
                .mapToInt(List::size)
                .max()
                .orElse(1);
        int[] blockedUntilRowByColumn = new int[Math.max(256, maxSourceColumnCount * 4 + 64)];
        for (int index = 0; index < blockedUntilRowByColumn.length; index++) {
            blockedUntilRowByColumn[index] = -1;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            List<MesProBatchRecordParsedCell> normalizedRow = new ArrayList<>();
            int runningColumn = 0;
            if (row != null) {
                for (MesProBatchRecordParsedCell cell : row) {
                    if (cell == null) {
                        continue;
                    }
                    while (runningColumn < blockedUntilRowByColumn.length
                            && blockedUntilRowByColumn[runningColumn] >= rowIndex) {
                        runningColumn++;
                    }
                    int colSpan = resolveLogicalWidthMappedSpan(cell, logicalColumnWidths, runningColumn,
                            blockedUntilRowByColumn, rowIndex, row);
                    int columnIndex = runningColumn;
                    normalizedRow.add(copyCellForGrid(cell, columnIndex, colSpan));
                    if (cell.getRowSpan() > 1) {
                        for (int offset = 0; offset < colSpan
                                && columnIndex + offset < blockedUntilRowByColumn.length; offset++) {
                            blockedUntilRowByColumn[columnIndex + offset] = rowIndex + cell.getRowSpan() - 1;
                        }
                    }
                    runningColumn = columnIndex + colSpan;
                }
            }
            normalizedRows.add(normalizedRow);
        }
        return normalizedRows;
    }

    private List<Integer> resolveLogicalColumnWidths(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<MesProBatchRecordParsedCell> bestRow = List.of();
        int bestScore = -1;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            int logicalEndColumn = 0;
            int singleSpanCount = 0;
            int textCount = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null) {
                    continue;
                }
                int startColumn = cell.getLogicalColumnIndex() == null ? logicalEndColumn : cell.getLogicalColumnIndex();
                int colSpan = Math.max(1, cell.getLogicalColSpan() == null ? cell.getColSpan() : cell.getLogicalColSpan());
                logicalEndColumn = Math.max(logicalEndColumn, startColumn + colSpan);
                if (colSpan == 1) {
                    singleSpanCount++;
                }
                if (cell.getText() != null && !cell.getText().isBlank()) {
                    textCount++;
                }
            }
            int score = logicalEndColumn * 100 + singleSpanCount * 10 + textCount;
            if (score > bestScore) {
                bestScore = score;
                bestRow = row;
            }
        }
        if (bestRow.isEmpty()) {
            return List.of();
        }
        int logicalColumnCount = bestRow.stream()
                .mapToInt(cell -> {
                    int startColumn = cell.getLogicalColumnIndex() == null ? 0 : cell.getLogicalColumnIndex();
                    int colSpan = Math.max(1, cell.getLogicalColSpan() == null ? cell.getColSpan() : cell.getLogicalColSpan());
                    return startColumn + colSpan;
                })
                .max()
                .orElse(0);
        if (logicalColumnCount <= 0) {
            return List.of();
        }
        int[] widths = new int[logicalColumnCount];
        int runningColumn = 0;
        for (MesProBatchRecordParsedCell cell : bestRow) {
            if (cell == null) {
                continue;
            }
            int startColumn = cell.getLogicalColumnIndex() == null ? runningColumn : cell.getLogicalColumnIndex();
            int colSpan = Math.max(1, cell.getLogicalColSpan() == null ? cell.getColSpan() : cell.getLogicalColSpan());
            int width = Math.max(1, cell.getWidthPx());
            int baseWidth = Math.max(1, width / colSpan);
            int remainder = Math.max(0, width - baseWidth * colSpan);
            for (int offset = 0; offset < colSpan && startColumn + offset < widths.length; offset++) {
                widths[startColumn + offset] = Math.max(widths[startColumn + offset],
                        baseWidth + (offset < remainder ? 1 : 0));
            }
            runningColumn = startColumn + colSpan;
        }
        fillMissingLogicalWidthsFromVerticalSpans(rows, widths);
        int fallbackWidth = resolveFallbackColumnWidth(widths);
        List<Integer> resolved = new ArrayList<>(widths.length);
        for (int width : widths) {
            resolved.add(width > 0 ? width : fallbackWidth);
        }
        return resolved;
    }

    private void fillMissingLogicalWidthsFromVerticalSpans(List<List<MesProBatchRecordParsedCell>> rows, int[] widths) {
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null || Math.max(1, cell.getRowSpan()) <= 1) {
                    continue;
                }
                int startColumn = cell.getLogicalColumnIndex() == null ? -1 : cell.getLogicalColumnIndex();
                int colSpan = Math.max(1, cell.getLogicalColSpan() == null ? cell.getColSpan() : cell.getLogicalColSpan());
                if (startColumn < 0 || colSpan != 1 || startColumn >= widths.length || widths[startColumn] > 0) {
                    continue;
                }
                widths[startColumn] = Math.max(1, cell.getWidthPx());
            }
        }
    }

    private int resolveFallbackColumnWidth(int[] widths) {
        int total = 0;
        int count = 0;
        for (int width : widths) {
            if (width > 0) {
                total += width;
                count++;
            }
        }
        return count == 0 ? 120 : Math.max(1, Math.round(total / (float) count));
    }

    private int resolveLogicalWidthMappedSpan(MesProBatchRecordParsedCell cell,
                                              List<Integer> logicalColumnWidths,
                                              int startColumn,
                                              int[] blockedUntilRowByColumn,
                                              int rowIndex,
                                              List<MesProBatchRecordParsedCell> row) {
        int fallbackSpan = Math.max(1, cell.getLogicalColSpan() == null ? cell.getColSpan() : cell.getLogicalColSpan());
        if (logicalColumnWidths == null || logicalColumnWidths.isEmpty()
                || startColumn < 0 || startColumn >= logicalColumnWidths.size()) {
            return fallbackSpan;
        }
        int remainingCells = countRemainingCells(row, cell);
        int availableColumns = countAvailableLogicalColumns(startColumn, logicalColumnWidths.size(),
                blockedUntilRowByColumn, rowIndex);
        if (availableColumns <= 0) {
            return fallbackSpan;
        }
        int maxSpan = Math.max(1, availableColumns - Math.max(0, remainingCells - 1));
        int sourceWidth = Math.max(1, cell.getWidthPx());
        int totalWidth = logicalColumnWidths.stream().mapToInt(width -> Math.max(1, width)).sum();
        if (remainingCells == 1 && sourceWidth >= totalWidth * 0.92f) {
            return maxSpan;
        }
        int accumulated = 0;
        int span = 0;
        for (int columnIndex = startColumn; columnIndex < logicalColumnWidths.size() && span < maxSpan; columnIndex++) {
            if (blockedUntilRowByColumn != null
                    && columnIndex < blockedUntilRowByColumn.length
                    && blockedUntilRowByColumn[columnIndex] >= rowIndex) {
                continue;
            }
            accumulated += Math.max(1, logicalColumnWidths.get(columnIndex));
            span++;
            if (accumulated >= sourceWidth * 0.85f) {
                break;
            }
        }
        return Math.max(1, span);
    }

    private int countRemainingCells(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell currentCell) {
        if (row == null || row.isEmpty()) {
            return 1;
        }
        int index = row.indexOf(currentCell);
        return index < 0 ? 1 : Math.max(1, row.size() - index);
    }

    private int countAvailableLogicalColumns(int startColumn,
                                             int maxColumn,
                                             int[] blockedUntilRowByColumn,
                                             int rowIndex) {
        int count = 0;
        for (int columnIndex = startColumn; columnIndex < maxColumn; columnIndex++) {
            if (blockedUntilRowByColumn != null
                    && columnIndex < blockedUntilRowByColumn.length
                    && blockedUntilRowByColumn[columnIndex] >= rowIndex) {
                continue;
            }
            count++;
        }
        return count;
    }

    private MesProBatchRecordParsedCell copyCellForGrid(MesProBatchRecordParsedCell source, int columnIndex, int colSpan) {
        return MesProBatchRecordParsedCell.builder()
                .text(source.getText())
                .rowSpan(Math.max(1, source.getRowSpan()))
                .colSpan(Math.max(1, colSpan))
                .columnIndex(Math.max(0, columnIndex))
                .logicalColumnIndex(source.getLogicalColumnIndex())
                .logicalColSpan(source.getLogicalColSpan())
                .bold(source.isBold())
                .fontSize(source.getFontSize())
                .horizontalAlign(source.getHorizontalAlign())
                .verticalAlign(source.getVerticalAlign())
                .widthPx(source.getWidthPx())
                .heightPx(source.getHeightPx())
                .fillable(source.isFillable())
                .visualBlank(source.isVisualBlank())
                .borderless(source.isBorderless())
                .diagonalSlash(source.isDiagonalSlash())
                .topBorderStyle(source.getTopBorderStyle())
                .bottomBorderStyle(source.getBottomBorderStyle())
                .leftBorderStyle(source.getLeftBorderStyle())
                .rightBorderStyle(source.getRightBorderStyle())
                .backgroundColor(source.getBackgroundColor())
                .documentFrameRole(source.getDocumentFrameRole())
                .placeholder(source.getPlaceholder())
                .inputType(source.getInputType())
                .build();
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

    private String rowText(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && cell.getText() != null) {
                builder.append(cell.getText());
            }
        }
        return builder.toString();
    }

    private List<Integer> resolveSegmentColumnWidths(List<List<MesProBatchRecordParsedCell>> rows, int maxColumnCount) {
        if (rows == null || rows.isEmpty() || maxColumnCount <= 0) {
            return List.of();
        }
        List<Integer> bestWidths = List.of();
        int bestScore = -1;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            int rowColumnCount = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
            if (rowColumnCount <= 0 || rowColumnCount > maxColumnCount) {
                continue;
            }
            int singleSpanCount = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                if (Math.max(1, cell.getColSpan()) == 1) {
                    singleSpanCount++;
                }
            }
            int score = rowColumnCount * 100 + singleSpanCount;
            if (score > bestScore) {
                bestScore = score;
                bestWidths = expandRowToColumnWidths(row);
            }
        }
        return bestWidths;
    }

    private List<Integer> expandRowToColumnWidths(List<MesProBatchRecordParsedCell> row) {
        List<Integer> widths = new ArrayList<>();
        for (MesProBatchRecordParsedCell cell : row) {
            int colSpan = Math.max(1, cell.getColSpan());
            int width = Math.max(1, cell.getWidthPx());
            int baseWidth = Math.max(1, width / colSpan);
            int remainder = Math.max(0, width - baseWidth * colSpan);
            for (int offset = 0; offset < colSpan; offset++) {
                widths.add(baseWidth + (offset < remainder ? 1 : 0));
            }
        }
        return widths;
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

    private List<Integer> resolveVisualColumnWidths(List<Integer> visualColumnBoundaries) {
        if (visualColumnBoundaries == null || visualColumnBoundaries.size() < 2) {
            return List.of();
        }
        List<Integer> widths = new ArrayList<>();
        for (int index = 0; index < visualColumnBoundaries.size() - 1; index++) {
            int width = visualColumnBoundaries.get(index + 1) - visualColumnBoundaries.get(index);
            widths.add(toWidthUnits(width));
        }
        return widths;
    }

    private int resolveStartColumnIndex(List<Integer> visualColumnBoundaries, TableCell cell) {
        if (visualColumnBoundaries == null || visualColumnBoundaries.size() < 2) {
            return 0;
        }
        int left = cell.getLeftEdge();
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < visualColumnBoundaries.size() - 1; index++) {
            int distance = Math.abs(visualColumnBoundaries.get(index) - left);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private int resolveVisualColSpan(List<Integer> visualColumnBoundaries, TableCell cell, int startColumnIndex) {
        if (visualColumnBoundaries == null || visualColumnBoundaries.size() < 2) {
            return 1;
        }
        int right = cell.getLeftEdge() + Math.max(1, cell.getWidth());
        int endBoundaryIndex = Math.min(visualColumnBoundaries.size() - 1, Math.max(startColumnIndex + 1, startColumnIndex));
        int bestDistance = Integer.MAX_VALUE;
        for (int index = Math.max(1, startColumnIndex + 1); index < visualColumnBoundaries.size(); index++) {
            int distance = Math.abs(visualColumnBoundaries.get(index) - right);
            if (distance < bestDistance) {
                bestDistance = distance;
                endBoundaryIndex = index;
            }
        }
        return Math.max(1, endBoundaryIndex - startColumnIndex);
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

    private boolean isMergedFollower(TableCell cell) {
        return (cell.isMerged() && !cell.isFirstMerged())
                || (cell.isVerticallyMerged() && !cell.isFirstVerticallyMerged());
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
                continue;
            }
            break;
        }
        return span;
    }

    private int resolveRowSpan(Table table, int rowIndex, int cellIndex) {
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

    private boolean resolveBold(TableCell cell) {
        CharacterRun characterRun = firstCharacterRun(cell);
        return characterRun != null && characterRun.isBold();
    }

    private int resolveFontSize(TableCell cell) {
        CharacterRun characterRun = firstCharacterRun(cell);
        if (characterRun == null) {
            return 10;
        }
        int raw = characterRun.getFontSize();
        return Math.max(10, raw > 20 ? raw / 2 : raw);
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
            case 2 -> "middle";
            case 3 -> "bottom";
            default -> "middle";
        };
    }

    private Paragraph firstParagraph(TableCell cell) {
        if (cell.numParagraphs() <= 0) {
            return null;
        }
        return cell.getParagraph(0);
    }

    private CharacterRun firstCharacterRun(TableCell cell) {
        Paragraph paragraph = firstParagraph(cell);
        if (paragraph == null || paragraph.numCharacterRuns() <= 0) {
            return null;
        }
        return paragraph.getCharacterRun(0);
    }

    private boolean isDocxVerticalMergeFollower(XWPFTableCell cell) {
        CTTcPr tcPr = tcPr(cell);
        if (tcPr == null || !tcPr.isSetVMerge()) {
            return false;
        }
        Object mergeValue = tcPr.getVMerge().getVal();
        return mergeValue == null || STMerge.CONTINUE.equals(mergeValue);
    }

    private boolean isDocxVerticalMergeRestart(XWPFTableCell cell) {
        CTTcPr tcPr = tcPr(cell);
        return tcPr != null && tcPr.isSetVMerge() && STMerge.RESTART.equals(tcPr.getVMerge().getVal());
    }

    private int resolveDocxColSpan(XWPFTableCell cell) {
        CTTcPr tcPr = tcPr(cell);
        if (tcPr == null || !tcPr.isSetGridSpan()) {
            return 1;
        }
        return Math.max(1, toInt(tcPr.getGridSpan().getVal(), 1));
    }

    private int resolveDocxRowSpan(XWPFTable table, int rowIndex, int cellIndex) {
        XWPFTableCell cell = table.getRow(rowIndex).getCell(cellIndex);
        if (!isDocxVerticalMergeRestart(cell)) {
            return 1;
        }
        int span = 1;
        for (int nextRowIndex = rowIndex + 1; nextRowIndex < table.getRows().size(); nextRowIndex++) {
            XWPFTableRow nextRow = table.getRow(nextRowIndex);
            if (cellIndex >= nextRow.getTableCells().size()) {
                break;
            }
            XWPFTableCell nextCell = nextRow.getCell(cellIndex);
            if (isDocxVerticalMergeFollower(nextCell)) {
                span++;
                continue;
            }
            break;
        }
        return span;
    }

    private boolean resolveDocxBold(XWPFTableCell cell) {
        XWPFRun run = firstDocxRun(cell);
        return run != null && run.isBold();
    }

    private int resolveDocxFontSize(XWPFTableCell cell) {
        XWPFRun run = firstDocxRun(cell);
        if (run == null || run.getFontSize() <= 0) {
            return 10;
        }
        return Math.max(8, run.getFontSize());
    }

    private String resolveDocxHorizontalAlign(XWPFTableCell cell) {
        if (cell.getParagraphs().isEmpty()) {
            return "left";
        }
        ParagraphAlignment alignment = cell.getParagraphs().get(0).getAlignment();
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
        if (run == null || run.getFontSize() <= 0) {
            return defaultValue;
        }
        return Math.max(8, run.getFontSize());
    }

    private String resolveDocxParagraphHorizontalAlign(XWPFParagraph paragraph) {
        ParagraphAlignment alignment = paragraph.getAlignment();
        if (alignment == ParagraphAlignment.CENTER) {
            return "center";
        }
        if (alignment == ParagraphAlignment.RIGHT) {
            return "right";
        }
        return "left";
    }

    private int resolveDocxWidthPx(XWPFTableCell cell, int colSpan) {
        CTTcPr tcPr = tcPr(cell);
        if (tcPr == null || !tcPr.isSetTcW()) {
            return Math.max(1, colSpan) * 120;
        }
        return toWidthUnits(toInt(tcPr.getTcW().getW(), Math.max(1, colSpan) * 1800));
    }

    private List<Integer> resolveDocxColumnWidths(List<List<MesProBatchRecordParsedCell>> rows, int maxColumnCount) {
        List<Integer> widths = resolveSegmentColumnWidths(rows, maxColumnCount);
        if (!widths.isEmpty()) {
            return widths;
        }
        List<Integer> defaults = new ArrayList<>();
        for (int index = 0; index < maxColumnCount; index++) {
            defaults.add(120);
        }
        return defaults;
    }

    private XWPFRun firstDocxRun(XWPFTableCell cell) {
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            if (!paragraph.getRuns().isEmpty()) {
                return paragraph.getRuns().get(0);
            }
        }
        return null;
    }

    private XWPFRun firstDocxRun(XWPFParagraph paragraph) {
        if (paragraph == null || paragraph.getRuns().isEmpty()) {
            return null;
        }
        return paragraph.getRuns().get(0);
    }

    private CTTcPr tcPr(XWPFTableCell cell) {
        CTTc ctTc = cell.getCTTc();
        return ctTc == null || !ctTc.isSetTcPr() ? null : ctTc.getTcPr();
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.intValue();
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
        if (twips <= 0) {
            return defaultValue;
        }
        return Math.max(defaultValue, Math.round(twips / 15.0f));
    }

    private int toWidthUnits(int twips) {
        if (twips <= 0) {
            return 1;
        }
        return Math.max(1, Math.round(twips / 15.0f));
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

    private String resolveBorderStyle(BorderCode borderCode) {
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

    private record DocxExtractionContext(int totalPageCount) {

        private static final DocxExtractionContext EMPTY = new DocxExtractionContext(0);
    }
}
