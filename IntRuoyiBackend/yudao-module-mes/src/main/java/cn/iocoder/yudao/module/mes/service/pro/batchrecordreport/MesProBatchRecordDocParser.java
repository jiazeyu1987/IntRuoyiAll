package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.wordparser.SharedWordDocumentParser;
import cn.iocoder.yudao.module.wordparser.WordCell;
import cn.iocoder.yudao.module.wordparser.WordDocumentFrame;
import cn.iocoder.yudao.module.wordparser.WordParseCommand;
import cn.iocoder.yudao.module.wordparser.WordParseException;
import cn.iocoder.yudao.module.wordparser.WordParseProfile;
import cn.iocoder.yudao.module.wordparser.WordParseResult;
import cn.iocoder.yudao.module.wordparser.WordTable;

import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

public class MesProBatchRecordDocParser {

    private final SharedWordDocumentParser sharedParser;

    public MesProBatchRecordDocParser(SharedWordDocumentParser sharedParser) {
        this.sharedParser = sharedParser;
    }

    public List<MesProBatchRecordParsedTable> parse(byte[] bytes) {
        return parse(bytes, ".doc");
    }

    public MesProBatchRecordDocumentFrame extractDocumentFrame(byte[] bytes) {
        return parseShared(bytes, ".doc").documentFrame();
    }

    public List<MesProBatchRecordParsedTable> parseDocx(byte[] bytes) {
        return parse(bytes, ".docx");
    }

    public MesProBatchRecordDocumentFrame extractDocxDocumentFrame(byte[] bytes) {
        return parseShared(bytes, ".docx").documentFrame();
    }

    private List<MesProBatchRecordParsedTable> parse(byte[] bytes, String extension) {
        MappedSharedDocument mapped = parseShared(bytes, extension);
        List<MesProBatchRecordParsedTable> tables = new ArrayList<>();
        for (WordTable sourceTable : mapped.result().tables()) {
            MesProBatchRecordParsedTable rawTable = toMesRawTable(sourceTable, ".docx".equals(extension));
            List<MesProBatchRecordParsedTable> splitTables = splitTemplates(rawTable);
            for (int splitIndex = 0; splitIndex < splitTables.size(); splitIndex++) {
                MesProBatchRecordParsedTable splitTable = splitTables.get(splitIndex);
                splitTable.setSourceTopLevelTableIndex(sourceTable.sourceTopLevelTableIndex() + 1);
                splitTable.setSourceSplitIndex(splitIndex + 1);
                splitTable.setDocumentFrame(mapped.documentFrame());
                tables.add(splitTable);
            }
        }
        for (int index = 0; index < tables.size(); index++) {
            tables.get(index).setSourceTableIndex(index + 1);
        }
        return tables;
    }

    private MappedSharedDocument parseShared(byte[] bytes, String extension) {
        try {
            WordParseResult result = sharedParser.parse(new WordParseCommand(
                    bytes, extension, "mes-word-source" + extension, WordParseProfile.STRUCTURAL_CANONICAL));
            return new MappedSharedDocument(result, toMesDocumentFrame(result.documentFrame(), extension));
        } catch (WordParseException ex) {
            throw switch (ex.code()) {
                case EMPTY_SOURCE -> exception(
                        MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EMPTY);
                case UNSUPPORTED_SOURCE_TYPE -> exception(
                        MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID);
                case NO_PARSEABLE_CONTENT -> exception(
                        MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID, 0);
                case CORRUPT_SOURCE, INVALID_TABLE_STRUCTURE -> exception(
                        MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                        ex.code().name());
            };
        }
    }

    private MesProBatchRecordParsedTable toMesRawTable(WordTable source, boolean docx) {
        List<List<MesProBatchRecordParsedCell>> rows = toMesRows(source.rows(), docx);
        String title = "";
        for (List<MesProBatchRecordParsedCell> row : rows) {
            for (MesProBatchRecordParsedCell cell : row) {
                if (!cell.getText().isBlank()) {
                    title = extractTemplateTitle(cell.getText());
                    break;
                }
            }
            if (!title.isBlank()) {
                break;
            }
        }
        title = MesProBatchRecordSharedPageTitleRules.resolveRepresentativeTitle(title, rows);
        if (title.isBlank()) {
            title = "\u8868";
        }
        return MesProBatchRecordParsedTable.builder()
                .sourceTopLevelTableIndex(source.sourceTopLevelTableIndex() + 1)
                .tableTitle(title)
                .rowCount(source.rowCount())
                .columnCount(source.columnCount())
                .columnWidths(source.columnWidths())
                .rows(rows)
                .build();
    }

    private MesProBatchRecordDocumentFrame toMesDocumentFrame(WordDocumentFrame source, String extension) {
        boolean docx = ".docx".equals(extension);
        return MesProBatchRecordDocumentFrame.builder()
                .headerRows(toMesRows(source.headerRows(), docx))
                .footerRows(toMesRows(source.footerRows(), docx))
                .build();
    }

    private List<List<MesProBatchRecordParsedCell>> toMesRows(List<List<WordCell>> sourceRows, boolean docx) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        for (List<WordCell> sourceRow : sourceRows) {
            List<MesProBatchRecordParsedCell> row = new ArrayList<>();
            int legacyDocxColumnIndex = 0;
            for (WordCell sourceCell : sourceRow) {
                Integer mappedDocxColumnIndex = docx && sourceCell.columnIndex() != null
                        ? legacyDocxColumnIndex : null;
                row.add(toMesCell(sourceCell, docx, mappedDocxColumnIndex));
                if (mappedDocxColumnIndex != null) {
                    legacyDocxColumnIndex += sourceCell.logicalColSpan();
                }
            }
            rows.add(row);
        }
        return rows;
    }

    private MesProBatchRecordParsedCell toMesCell(
            WordCell source, boolean docx, Integer mappedDocxColumnIndex) {
        boolean legacyDocxTableCell = docx && source.columnIndex() != null;
        return MesProBatchRecordParsedCell.builder()
                .text(source.text())
                .rowSpan(source.rowSpan())
                .colSpan(source.colSpan())
                .columnIndex(mappedDocxColumnIndex == null ? source.columnIndex() : mappedDocxColumnIndex)
                .logicalColumnIndex(mappedDocxColumnIndex == null
                        ? source.logicalColumnIndex() : mappedDocxColumnIndex)
                .logicalColSpan(source.logicalColSpan())
                .bold(source.bold())
                .fontSize(source.fontSize())
                .horizontalAlign(source.horizontalAlign())
                .verticalAlign(docx || "center".equals(source.verticalAlign()) ? "middle" : source.verticalAlign())
                .widthPx(source.widthPx())
                .heightPx(source.heightPx())
                .diagonalSlash(source.diagonalSlash())
                .topBorderStyle(legacyDocxTableCell ? "solid" : source.topBorderStyle())
                .bottomBorderStyle(legacyDocxTableCell ? "solid" : source.bottomBorderStyle())
                .leftBorderStyle(legacyDocxTableCell ? "solid" : source.leftBorderStyle())
                .rightBorderStyle(legacyDocxTableCell ? "solid" : source.rightBorderStyle())
                .build();
    }

    private record MappedSharedDocument(
            WordParseResult result,
            MesProBatchRecordDocumentFrame documentFrame) {
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

}
