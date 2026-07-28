package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MesProBatchRecordReportLayoutCalibrator {

    private static final String DOCUMENT_FRAME_HEADER_ROLE = "DOCUMENT_HEADER";
    private static final String DOCUMENT_FRAME_FOOTER_ROLE = "DOCUMENT_FOOTER";
    private static final int FOOTER_SPACER_ROW_COUNT = 0;
    private static final int PROCESS_TEMPLATE_TOTAL_COL_SPAN = 20;
    private static final int PROCESS_BODY_FONT_SIZE = 9;
    private static final int PROCESS_HEADER_FONT_SIZE = 10;
    private static final int PACKED_MATERIAL_MATRIX_HEADER_COUNT = 6;
    private static final int OVERVIEW_SECTION_CONTINUATION_MIN_GAP = 12;
    private static final int CHECKLIST_SIDE_HEADER_COL_SPAN = 1;
    private static final int[] CHECKLIST_OUTCOME_FIXED_TAIL_COL_SPANS = {3, 3, 3};
    private static final int[] CHECKLIST_HEADER_FIXED_TAIL_COL_SPANS = {3, 3, 3};
    private static final int[] CHECKLIST_BODY_FIXED_TAIL_COL_SPANS = {3, 3, 3};
    private static final List<String> CHECKLIST_HEADER_LABELS = List.of("检查要求", "结果", "操作人/日期", "复核人/日期");
    private static final int[] METADATA_LABEL_VALUE_PAIR_SPANS = {3, 4};
    private static final int OPERATION_BAND_LABEL_COL_SPAN = 3;
    private static final int OPERATION_BAND_SELF_INSPECTION_COL_SPAN = 3;
    private static final int OPERATION_BAND_TAIL_COL_SPAN = 2;
    private static final int OPERATION_SELF_INSPECTION_TAIL_WIDTH_FLOOR_PX = 68;

    public MesProBatchRecordParsedTable calibrate(MesProBatchRecordParsedTable parsedTable) {
        int columnCount = Math.max(parsedTable.getColumnCount(), 1);
        int measuredSourceWidth = measureTableWidth(parsedTable.getRows(), columnCount);
        int sourceWidth = Math.max(measuredSourceWidth,
                MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(columnCount));
        Map<String, Integer> rowSignatureCounts = MesProBatchRecordSharedRowTypeRules.countRowSignatures(parsedTable.getRows());
        boolean processTemplate = isSharedProcessTemplate(parsedTable)
                || isMultiSegmentProcessPage(parsedTable.getRows(), rowSignatureCounts);
        boolean sharedOverviewTemplate = isSharedOverviewTemplate(parsedTable);
        List<Integer> sourceColumnWidths = parsedTable.getColumnWidths();
        boolean preserveSourceGrid = shouldPreserveSourceGrid(parsedTable.getRows(), sourceColumnWidths, columnCount);
        boolean keepCompleteSourceGrid = !preserveSourceGrid
                && shouldKeepCompleteSourceGrid(parsedTable, sourceColumnWidths, columnCount);
        boolean preserveOverviewSourceColumnWidths = !preserveSourceGrid && !keepCompleteSourceGrid
                && shouldPreserveOverviewSourceColumnWidths(sharedOverviewTemplate, parsedTable.getRows(),
                sourceColumnWidths, columnCount);
        boolean preserveOverviewSourceShape = !preserveSourceGrid && !keepCompleteSourceGrid
                && shouldPreserveOverviewSourceShape(sharedOverviewTemplate, parsedTable.getRows(),
                sourceColumnWidths, columnCount);
        boolean preserveExactSourceShape = preserveSourceGrid || keepCompleteSourceGrid || preserveOverviewSourceShape;
        int renderWidth = resolveRenderWidth(sourceWidth, measuredSourceWidth, columnCount,
                processTemplate || sharedOverviewTemplate,
                preserveExactSourceShape,
                parsedTable.getRows());
        if ((preserveSourceGrid || preserveOverviewSourceShape)
                && hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)) {
            renderWidth = sumPositiveWidths(sourceColumnWidths);
            sourceWidth = renderWidth;
        }
        if (sharedOverviewTemplate && !preserveOverviewSourceShape) {
            renderWidth = resolveSharedOverviewRenderWidth(renderWidth, columnCount);
        }
        List<List<MesProBatchRecordParsedCell>> normalizedRows = preserveExactSourceShape
                ? cloneRows(parsedTable.getRows(), preserveSourceGrid || preserveOverviewSourceShape)
                : normalizeImplicitMergedRows(
                parsedTable.getRows(), columnCount, sourceWidth, renderWidth, processTemplate,
                preserveSourceGrid, (preserveSourceGrid || preserveOverviewSourceColumnWidths) ? sourceColumnWidths : List.of());
        if (processTemplate) {
            int effectiveColumnCount = Math.max(columnCount, resolveLogicalMaxColumnCount(normalizedRows, columnCount));
            if (effectiveColumnCount != columnCount) {
                columnCount = effectiveColumnCount;
                sourceColumnWidths = parsedTable.getColumnWidths();
                preserveSourceGrid = shouldPreserveSourceGrid(parsedTable.getRows(), sourceColumnWidths, columnCount);
                preserveOverviewSourceColumnWidths = !preserveSourceGrid
                        && shouldPreserveOverviewSourceColumnWidths(sharedOverviewTemplate, parsedTable.getRows(),
                        sourceColumnWidths, columnCount);
                renderWidth = resolveRenderWidth(sourceWidth, measuredSourceWidth, columnCount, true,
                        preserveSourceGrid,
                        parsedTable.getRows());
                if (preserveSourceGrid && hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)) {
                    renderWidth = sumPositiveWidths(sourceColumnWidths);
                    sourceWidth = renderWidth;
                }
                if (sharedOverviewTemplate && !preserveSourceGrid) {
                    renderWidth = resolveSharedOverviewRenderWidth(renderWidth, columnCount);
                }
                normalizedRows = preserveSourceGrid
                        ? cloneRows(parsedTable.getRows(), true)
                        : normalizeImplicitMergedRows(
                        parsedTable.getRows(), columnCount, sourceWidth, renderWidth, true,
                        preserveSourceGrid, (preserveSourceGrid || preserveOverviewSourceColumnWidths) ? sourceColumnWidths : List.of());
            }
        }

        MesProBatchRecordDocumentFrame documentFrame = parsedTable.getDocumentFrame();
        List<List<MesProBatchRecordParsedCell>> documentHeaderRows =
                buildDocumentFrameRows(documentFrame == null ? List.of() : documentFrame.getHeaderRows(),
                        columnCount, renderWidth, DOCUMENT_FRAME_HEADER_ROLE);
        List<List<MesProBatchRecordParsedCell>> documentFooterRows =
                buildDocumentFrameRows(documentFrame == null ? List.of() : documentFrame.getFooterRows(),
                        columnCount, renderWidth, DOCUMENT_FRAME_FOOTER_ROLE);

        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        rows.addAll(documentHeaderRows);
        rows.addAll(normalizedRows);
        if (!preserveExactSourceShape) {
            normalizeChecklistNarrativeBands(rows, columnCount, renderWidth, processTemplate);
        }
        if (!preserveExactSourceShape) {
            normalizeExplicitMaterialMatrixHeaderRows(rows, columnCount, renderWidth, processTemplate);
            normalizeOperationInstructionBands(rows, columnCount, renderWidth, processTemplate);
        }
        restoreOperationInstructionVerticalMainCells(rows);
        expandPackedMaterialMatrixRows(rows, columnCount, renderWidth, processTemplate);
        if (preserveSourceGrid) {
            insertContinuationHeadersForLongRepeatedEquipmentMatrix(rows, columnCount, renderWidth, processTemplate,
                    documentHeaderRows);
            extendSemanticHeadersOverMissingSourceDetailRows(rows, columnCount);
        } else if (!keepCompleteSourceGrid && !preserveOverviewSourceShape) {
            normalizeChecklistNarrativeBands(rows, columnCount, renderWidth, processTemplate);
            insertContinuationHeadersForLongRepeatedOperationSegments(rows, columnCount, renderWidth, processTemplate,
                    documentHeaderRows);
            insertContinuationHeadersForLongOverviewSections(rows, columnCount, renderWidth, sharedOverviewTemplate,
                    documentHeaderRows);
        }
        if (preserveExactSourceShape) {
            restoreSourceIndexedRepeatedOperationTailBlankBlocks(rows, columnCount);
        }
        int repeatedBlockColumnCount = Math.max(columnCount, resolveLogicalMaxColumnCount(rows, columnCount));
        if (!preserveExactSourceShape) {
            mergeTrailingBlankColumnsAcrossRepeatedOperationBlocks(
                    rows, repeatedBlockColumnCount, processTemplate || containsRepeatedOperationBlockWithDrying(rows));
            enforceDeclaredColumnBudget(rows, columnCount, renderWidth, sourceColumnWidths);
        }
        int semanticPostProcessColumnCount = Math.max(columnCount, resolveMaxColumnCount(rows));
        if (!preserveExactSourceShape
                && semanticPostProcessColumnCount > PROCESS_TEMPLATE_TOTAL_COL_SPAN) {
            normalizeChecklistNarrativeBands(rows, semanticPostProcessColumnCount, renderWidth, true);
            mergeTrailingBlankColumnsAcrossRepeatedOperationBlocks(
                    rows, semanticPostProcessColumnCount, containsRepeatedOperationBlockWithDrying(rows));
        }
        boolean routeBSource = Boolean.TRUE.equals(parsedTable.getRouteBSource());
        for (int index = 0; index < FOOTER_SPACER_ROW_COUNT; index++) {
            rows.add(List.of());
        }
        rows.addAll(documentFooterRows);
        closeSourceIndexedRightEdgeGaps(rows, columnCount);
        balanceChecklistNarrativeTrailingBlankSpans(rows);

        List<Integer> fixedColumnWidths = resolveFixedColumnWidths(parsedTable, rows, columnCount, renderWidth,
                sharedOverviewTemplate, preserveExactSourceShape, preserveOverviewSourceColumnWidths);
        if (!preserveExactSourceShape) {
            applySinglePageCompression(rows);
        }
        applyResolvedColumnWidths(rows, toIntArray(fixedColumnWidths), false, preserveExactSourceShape);

        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(parsedTable.getSourceTableIndex())
                .tableTitle(parsedTable.getTableTitle())
                .rowCount(rows.size())
                .columnCount(columnCount)
                .columnWidths(fixedColumnWidths)
                .preserveSourceGrid(preserveExactSourceShape)
                .routeBSource(parsedTable.getRouteBSource())
                .documentFrame(documentFrame)
                .rows(rows)
                .build();
    }

    private List<List<MesProBatchRecordParsedCell>> cloneRows(List<List<MesProBatchRecordParsedCell>> sourceRows) {
        return cloneRows(sourceRows, false);
    }

    private List<List<MesProBatchRecordParsedCell>> cloneRows(List<List<MesProBatchRecordParsedCell>> sourceRows,
                                                              boolean preserveExactSourceHeight) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        if (sourceRows == null) {
            return rows;
        }
        for (List<MesProBatchRecordParsedCell> row : sourceRows) {
            List<MesProBatchRecordParsedCell> clonedRow = new ArrayList<>();
            for (MesProBatchRecordParsedCell cell : row) {
                clonedRow.add(cloneCell(cell, 1.0f, preserveExactSourceHeight));
            }
            rows.add(clonedRow);
        }
        return rows;
    }

    private void extendSemanticHeadersOverMissingSourceDetailRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                                  int columnCount) {
        if (rows == null || rows.size() < 2 || columnCount <= 0) {
            return;
        }
        for (int rowIndex = 0; rowIndex + 1 < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> headerRow = rows.get(rowIndex);
            List<MesProBatchRecordParsedCell> detailRow = rows.get(rowIndex + 1);
            if (!isMultiLevelSemanticHeaderRow(headerRow, columnCount) || detailRow == null || detailRow.isEmpty()) {
                continue;
            }
            for (CellPlacement headerCell : placeRowCells(headerRow)) {
                MesProBatchRecordParsedCell cell = headerCell.cell();
                if (!isNarrowSemanticHeaderCell(cell, headerCell.startColumn(), columnCount)
                        || hasAnyDetailCellInRange(detailRow, headerCell.startColumn(), headerCell.endColumn())) {
                    continue;
                }
                cell.setRowSpan(Math.max(2, Math.max(1, cell.getRowSpan())));
            }
        }
    }

    private boolean isMultiLevelSemanticHeaderRow(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (row == null || row.size() < 6 || columnCount < 20) {
            return false;
        }
        int semanticHeaderCount = 0;
        for (CellPlacement placement : placeRowCells(row)) {
            if (isNarrowSemanticHeaderCell(placement.cell(), placement.startColumn(), columnCount)) {
                semanticHeaderCount++;
            }
        }
        return semanticHeaderCount >= 3;
    }

    private boolean isNarrowSemanticHeaderCell(MesProBatchRecordParsedCell cell, int startColumn, int columnCount) {
        if (cell == null || Math.max(1, cell.getRowSpan()) > 1 || Math.max(1, cell.getColSpan()) <= 1) {
            return false;
        }
        return MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                textOf(cell), startColumn, columnCount) > 0;
    }

    private boolean hasAnyDetailCellInRange(List<MesProBatchRecordParsedCell> row, int startColumn, int endColumn) {
        if (row == null || endColumn <= startColumn) {
            return false;
        }
        for (CellPlacement placement : placeRowCells(row)) {
            if (placement.startColumn() < endColumn && placement.endColumn() > startColumn) {
                return true;
            }
        }
        return false;
    }

    private List<CellPlacement> placeRowCells(List<MesProBatchRecordParsedCell> row) {
        List<CellPlacement> placements = new ArrayList<>();
        if (row == null || row.isEmpty()) {
            return placements;
        }
        int cursor = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null) {
                continue;
            }
            int startColumn = cell.getColumnIndex() == null ? cursor : Math.max(0, cell.getColumnIndex());
            int endColumn = startColumn + Math.max(1, cell.getColSpan());
            placements.add(new CellPlacement(cell, startColumn, endColumn));
            cursor = Math.max(cursor, endColumn);
        }
        return placements;
    }

    private boolean shouldCompressPreservedSourceGridHeights(List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        if (rows == null || rows.isEmpty() || columnCount < 10) {
            return false;
        }
        List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes = classifyRowTypes(rows);
        int targetHeight = MesProBatchRecordReportShapeRules.resolveSinglePageTargetHeight(
                rows.size(), columnCount, rowTypes);
        return sumHeights(resolveRowHeights(rows, rowTypes)) > targetHeight;
    }

    private boolean shouldKeepCompleteSourceGrid(MesProBatchRecordParsedTable parsedTable,
                                                 List<Integer> sourceColumnWidths,
                                                 int columnCount) {
        if (parsedTable == null || !hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)
                || columnCount < 5 || columnCount > 40) {
            return false;
        }
        List<List<MesProBatchRecordParsedCell>> rows = parsedTable.getRows();
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        boolean hasSourceColumnIndexes = rows.stream()
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null && cell.getColumnIndex() != null);
        if (!hasSourceColumnIndexes) {
            return false;
        }
        int maxRowEndColumn = rows.stream()
                .filter(row -> row != null && !row.isEmpty())
                .mapToInt(this::resolveRowEndColumn)
                .max()
                .orElse(0);
        if (maxRowEndColumn > columnCount) {
            return false;
        }
        boolean hasFullWidthTitle = rows.stream()
                .filter(row -> row != null && row.size() == 1)
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null && Math.max(1, cell.getColSpan()) == columnCount
                        && !textOf(cell).isBlank());
        boolean hasDeclaredWidthSkeleton = rows.stream()
                .filter(row -> row != null
                        && row.size() >= Math.min(columnCount, Math.max(5, columnCount - 2)))
                .anyMatch(row -> resolveRowEndColumn(row) == columnCount);
        boolean hasOpenVerticalMerge = rows.stream()
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null && Math.max(1, cell.getRowSpan()) >= 2);
        boolean hasProcessContent = rows.stream()
                .anyMatch(row -> rowText(row).contains("生产")
                        || rowText(row).contains("操作")
                        || rowText(row).contains("检查"));
        return hasFullWidthTitle && hasDeclaredWidthSkeleton && hasOpenVerticalMerge && hasProcessContent;
    }

    private boolean shouldPreserveOverviewSourceShape(boolean sharedOverviewTemplate,
                                                      List<List<MesProBatchRecordParsedCell>> rows,
                                                      List<Integer> sourceColumnWidths,
                                                      int columnCount) {
        if (!sharedOverviewTemplate || !hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)
                || rows == null || rows.isEmpty()) {
            return false;
        }
        boolean hasSourceColumnIndexes = rows.stream()
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null && cell.getColumnIndex() != null);
        if (!hasSourceColumnIndexes) {
            return false;
        }
        int maxRowEndColumn = rows.stream()
                .filter(row -> row != null && !row.isEmpty())
                .mapToInt(this::resolveRowEndColumn)
                .max()
                .orElse(0);
        if (maxRowEndColumn > columnCount) {
            return false;
        }
        boolean hasFullWidthSectionRows = rows.stream()
                .filter(row -> row != null && row.size() == 1)
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null
                        && Math.max(1, cell.getColSpan()) == columnCount
                        && !textOf(cell).isBlank());
        boolean hasDenseVisualSkeleton = rows.stream()
                .filter(row -> row != null)
                .anyMatch(row -> row.size() >= Math.min(columnCount, Math.max(4, columnCount - 1))
                        && resolveRowEndColumn(row) == columnCount);
        return hasFullWidthSectionRows && hasDenseVisualSkeleton;
    }

    private boolean shouldPreserveOverviewSourceColumnWidths(boolean sharedOverviewTemplate,
                                                             List<List<MesProBatchRecordParsedCell>> rows,
                                                             List<Integer> sourceColumnWidths,
                                                             int columnCount) {
        if (!sharedOverviewTemplate || !hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)
                || rows == null || rows.isEmpty()) {
            return false;
        }
        long sourceIndexedFullWidthRows = rows.stream()
                .filter(row -> isSourceIndexedSparseOverviewRow(row, columnCount))
                .count();
        return sourceIndexedFullWidthRows >= 2;
    }

    private boolean isSourceIndexedSparseOverviewRow(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (row == null || row.size() < 4 || row.size() > Math.max(8, columnCount)
                || resolveRowEndColumn(row) != columnCount
                || countNonEmptyCells(row) < 2) {
            return false;
        }
        boolean hasMergedSourceCell = false;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null || cell.getColumnIndex() == null) {
                return false;
            }
            int startColumn = Math.max(0, cell.getColumnIndex());
            int colSpan = Math.max(1, cell.getColSpan());
            if (startColumn + colSpan > columnCount) {
                return false;
            }
            hasMergedSourceCell = hasMergedSourceCell || colSpan > 1;
        }
        return hasMergedSourceCell;
    }

    private MesProBatchRecordParsedTable keepCompleteSourceGrid(MesProBatchRecordParsedTable parsedTable,
                                                                List<Integer> sourceColumnWidths,
                                                                int columnCount) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        for (List<MesProBatchRecordParsedCell> row : parsedTable.getRows()) {
            List<MesProBatchRecordParsedCell> clonedRow = new ArrayList<>();
            for (MesProBatchRecordParsedCell cell : row) {
                MesProBatchRecordParsedCell cloned = cloneCell(cell, 1.0f);
                clonedRow.add(cloned);
            }
            rows.add(clonedRow);
        }
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(parsedTable.getSourceTableIndex())
                .tableTitle(parsedTable.getTableTitle())
                .rowCount(rows.size())
                .columnCount(columnCount)
                .columnWidths(sourceColumnWidths)
                .preserveSourceGrid(false)
                .rows(rows)
                .build();
    }

    private String vertical(String text) {
        StringBuilder builder = new StringBuilder();
        for (int offset = 0; offset < text.length(); ) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            int codePoint = text.codePointAt(offset);
            builder.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        return builder.toString();
    }

    private List<List<MesProBatchRecordParsedCell>> buildDocumentFrameRows(
            List<List<MesProBatchRecordParsedCell>> sourceRows,
            int columnCount,
            int tableWidth,
            String role) {
        if (sourceRows == null || sourceRows.isEmpty()) {
            return List.of();
        }
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        int sourceColumnCount = Math.max(1, resolveMaxColumnCount(sourceRows));
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        int frameRowIndex = 0;
        for (List<MesProBatchRecordParsedCell> sourceRow : sourceRows) {
            if (sourceRow == null || sourceRow.isEmpty() || rowText(sourceRow).isBlank()) {
                continue;
            }
            List<MesProBatchRecordParsedCell> row = new ArrayList<>();
            int availableColumnCount = countAvailableDocumentFrameColumns(
                    frameRowIndex, columnCount, blockedUntilRowByColumn);
            if (availableColumnCount <= 0) {
                continue;
            }
            List<Integer> targetSpans = resolveDocumentFrameTargetSpans(sourceRow, availableColumnCount);
            int targetCursor = nextAvailableDocumentFrameColumn(
                    0, frameRowIndex, columnCount, blockedUntilRowByColumn);
            for (int cellIndex = 0; cellIndex < sourceRow.size(); cellIndex++) {
                MesProBatchRecordParsedCell sourceCell = sourceRow.get(cellIndex);
                if (sourceCell == null) {
                    continue;
                }
                int targetStart = nextAvailableDocumentFrameColumn(
                        targetCursor, frameRowIndex, columnCount, blockedUntilRowByColumn);
                if (targetStart >= columnCount) {
                    break;
                }
                int targetSpan = Math.max(1, cellIndex < targetSpans.size()
                        ? targetSpans.get(cellIndex)
                        : Math.round(Math.max(1, sourceCell.getColSpan())
                        * (availableColumnCount / (float) sourceColumnCount)));
                int targetEnd = resolveDocumentFrameCellEnd(
                        targetStart, targetSpan, frameRowIndex, columnCount, blockedUntilRowByColumn);
                MesProBatchRecordParsedCell cell = cloneCell(sourceCell, 1.0f, true);
                cell.setColumnIndex(targetStart);
                cell.setColSpan(Math.max(1, targetEnd - targetStart));
                cell.setWidthPx(sourceCell.getWidthPx() > 0
                        ? sourceCell.getWidthPx()
                        : Math.round(tableWidth * (cell.getColSpan() / (float) Math.max(1, columnCount))));
                cell.setHeightPx(DOCUMENT_FRAME_FOOTER_ROLE.equals(role)
                        ? 20
                        : MesProBatchRecordReportShapeRules.clampRowHeight(sourceCell.getHeightPx()));
                cell.setFillable(false);
                cell.setDocumentFrameRole(role);
                if (DOCUMENT_FRAME_FOOTER_ROLE.equals(role)) {
                    cell.setBorderless(true);
                }
                row.add(cell);
                targetCursor = targetEnd;
            }
            if (!row.isEmpty()) {
                rows.add(row);
                markDocumentFrameBlockedColumns(row, frameRowIndex, blockedUntilRowByColumn);
                frameRowIndex++;
            }
        }
        return rows;
    }

    private int countAvailableDocumentFrameColumns(int rowIndex,
                                                   int columnCount,
                                                   Map<Integer, Integer> blockedUntilRowByColumn) {
        int count = 0;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            if (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) < rowIndex) {
                count++;
            }
        }
        return count;
    }

    private int nextAvailableDocumentFrameColumn(int startColumn,
                                                 int rowIndex,
                                                 int columnCount,
                                                 Map<Integer, Integer> blockedUntilRowByColumn) {
        int columnIndex = Math.max(0, startColumn);
        while (columnIndex < columnCount
                && blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
            columnIndex++;
        }
        return columnIndex;
    }

    private int resolveDocumentFrameCellEnd(int targetStart,
                                            int targetSpan,
                                            int rowIndex,
                                            int columnCount,
                                            Map<Integer, Integer> blockedUntilRowByColumn) {
        int columnIndex = targetStart;
        int consumedColumns = 0;
        while (columnIndex < columnCount && consumedColumns < Math.max(1, targetSpan)) {
            if (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                break;
            }
            columnIndex++;
            consumedColumns++;
        }
        return Math.max(targetStart + 1, columnIndex);
    }

    private void markDocumentFrameBlockedColumns(List<MesProBatchRecordParsedCell> row,
                                                 int rowIndex,
                                                 Map<Integer, Integer> blockedUntilRowByColumn) {
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null || Math.max(1, cell.getRowSpan()) <= 1) {
                continue;
            }
            int startColumn = cell.getColumnIndex() == null ? 0 : Math.max(0, cell.getColumnIndex());
            int endColumn = startColumn + Math.max(1, cell.getColSpan());
            for (int columnIndex = startColumn; columnIndex < endColumn; columnIndex++) {
                blockedUntilRowByColumn.put(columnIndex, rowIndex + Math.max(1, cell.getRowSpan()) - 1);
            }
        }
    }

    private List<Integer> resolveDocumentFrameTargetSpans(List<MesProBatchRecordParsedCell> sourceRow, int columnCount) {
        if (sourceRow == null || sourceRow.isEmpty()) {
            return List.of();
        }
        int positiveWidthSum = sourceRow.stream()
                .mapToInt(cell -> Math.max(0, cell == null ? 0 : cell.getWidthPx()))
                .sum();
        if (positiveWidthSum <= 0) {
            return distributeSpanByExistingGrid(sourceRow, columnCount);
        }
        int[] spans = new int[sourceRow.size()];
        int assigned = 0;
        for (int index = 0; index < sourceRow.size(); index++) {
            int width = Math.max(0, sourceRow.get(index) == null ? 0 : sourceRow.get(index).getWidthPx());
            spans[index] = Math.max(1, Math.round(width * columnCount / (float) positiveWidthSum));
            assigned += spans[index];
        }
        if (spans.length == 3 && columnCount >= 10) {
            int middleFloor = Math.max(1, Math.round(columnCount / 5.0f));
            if (spans[1] < middleFloor) {
                assigned += middleFloor - spans[1];
                spans[1] = middleFloor;
            }
        }
        while (assigned > columnCount) {
            int widestIndex = widestSpanIndex(spans);
            if (spans[widestIndex] <= 1) {
                break;
            }
            spans[widestIndex]--;
            assigned--;
        }
        while (assigned < columnCount) {
            spans[widestSpanIndex(spans)]++;
            assigned++;
        }
        List<Integer> result = new ArrayList<>(spans.length);
        for (int span : spans) {
            result.add(Math.max(1, span));
        }
        return result;
    }

    private List<Integer> distributeSpanByExistingGrid(List<MesProBatchRecordParsedCell> sourceRow, int columnCount) {
        int sourceSpanSum = sourceRow.stream()
                .mapToInt(cell -> Math.max(1, cell == null ? 1 : cell.getColSpan()))
                .sum();
        List<Integer> spans = new ArrayList<>(sourceRow.size());
        int assigned = 0;
        for (MesProBatchRecordParsedCell cell : sourceRow) {
            int span = Math.max(1, Math.round(Math.max(1, cell == null ? 1 : cell.getColSpan())
                    * columnCount / (float) Math.max(1, sourceSpanSum)));
            spans.add(span);
            assigned += span;
        }
        for (int index = spans.size() - 1; index >= 0 && assigned != columnCount; index--) {
            int delta = columnCount - assigned;
            int adjusted = Math.max(1, spans.get(index) + delta);
            assigned += adjusted - spans.get(index);
            spans.set(index, adjusted);
        }
        return spans;
    }

    private int widestSpanIndex(int[] spans) {
        int widestIndex = 0;
        for (int index = 1; index < spans.length; index++) {
            if (spans[index] > spans[widestIndex]) {
                widestIndex = index;
            }
        }
        return widestIndex;
    }

    private List<List<MesProBatchRecordParsedCell>> cloneDocumentFrameRows(
            List<List<MesProBatchRecordParsedCell>> sourceRows) {
        if (sourceRows == null || sourceRows.isEmpty()) {
            return List.of();
        }
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        for (List<MesProBatchRecordParsedCell> sourceRow : sourceRows) {
            List<MesProBatchRecordParsedCell> row = new ArrayList<>();
            if (sourceRow != null) {
                for (MesProBatchRecordParsedCell sourceCell : sourceRow) {
                    if (sourceCell != null) {
                        row.add(cloneCell(sourceCell, 1.0f, true));
                    }
                }
            }
            rows.add(row);
        }
        return rows;
    }

    private List<List<MesProBatchRecordParsedCell>> normalizeImplicitMergedRows(
            List<List<MesProBatchRecordParsedCell>> originalRows, int columnCount, int sourceWidth, int renderWidth,
            boolean processTemplate, boolean preserveSourceGrid, List<Integer> sourceColumnWidths) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        float scale = renderWidth / (float) sourceWidth;
        Map<String, Integer> rowSignatureCounts = MesProBatchRecordSharedRowTypeRules.countRowSignatures(originalRows);
        Map<String, RowShapeTemplate> repeatedRowTemplates = new HashMap<>();
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();

        for (int rowIndex = 0; rowIndex < originalRows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> originalRow = originalRows.get(rowIndex);
            String rowSignature = rowStructureKey(originalRow);
            if (processTemplate) {
                List<MesProBatchRecordParsedCell> specializedRow = normalizeProcessTemplateRow(
                        originalRows, rowIndex, originalRow, columnCount, renderWidth, scale, rowSignatureCounts,
                        preserveSourceGrid, sourceColumnWidths, blockedUntilRowByColumn);
                if (specializedRow != null) {
                    rows.add(specializedRow);
                    markBlockedColumns(specializedRow, rowIndex, blockedUntilRowByColumn);
                    if (shouldCacheRepeatedRowTemplate(originalRows, rowIndex, rowSignatureCounts)) {
                        repeatedRowTemplates.putIfAbsent(rowSignature, RowShapeTemplate.from(specializedRow));
                    }
                    continue;
                }
                RowShapeTemplate repeatedTemplate = repeatedRowTemplates.get(rowSignature);
                if (repeatedTemplate != null) {
                    List<MesProBatchRecordParsedCell> templatedRow = applyRowShapeTemplate(
                            originalRow, scale, renderWidth, columnCount, repeatedTemplate);
                    rows.add(templatedRow);
                    markBlockedColumns(templatedRow, rowIndex, blockedUntilRowByColumn);
                    continue;
                }
            }

            List<MesProBatchRecordParsedCell> clonedRow = new ArrayList<>();
            for (MesProBatchRecordParsedCell originalCell : originalRow) {
                MesProBatchRecordParsedCell clonedCell = cloneCell(originalCell, scale);
                if (shouldExpandToFullWidth(originalRow, originalCell, columnCount, sourceWidth)) {
                    clonedCell.setColSpan(columnCount);
                    clonedCell.setWidthPx(renderWidth);
                    clonedCell.setHorizontalAlign("center");
                    clonedCell.setBold(true);
                    clonedCell.setFontSize(10);
                }
                clonedRow.add(clonedCell);
            }
            rows.add(clonedRow);
            markBlockedColumns(clonedRow, rowIndex, blockedUntilRowByColumn);
        }
        return rows;
    }

    private void insertContinuationHeadersForLongRepeatedOperationSegments(List<List<MesProBatchRecordParsedCell>> rows,
                                                                           int columnCount,
                                                                           int renderWidth,
                                                                           boolean processTemplate,
                                                                           List<List<MesProBatchRecordParsedCell>> documentHeaderRows) {
        if (!processTemplate || countDocumentHeaderRows(rows) > 1) {
            return;
        }
        if (documentHeaderRows == null || documentHeaderRows.isEmpty()) {
            return;
        }
        int continuationIndex = resolveRepeatedOperationContinuationIndex(rows);
        if (continuationIndex < 0) {
            return;
        }
        rows.addAll(continuationIndex, cloneDocumentFrameRows(documentHeaderRows));
    }

    private void insertContinuationHeadersForLongRepeatedEquipmentMatrix(List<List<MesProBatchRecordParsedCell>> rows,
                                                                         int columnCount,
                                                                         int renderWidth,
                                                                         boolean processTemplate,
                                                                         List<List<MesProBatchRecordParsedCell>> documentHeaderRows) {
        if (!processTemplate || countDocumentHeaderRows(rows) > 1) {
            return;
        }
        if (documentHeaderRows == null || documentHeaderRows.isEmpty()) {
            return;
        }
        List<Integer> equipmentMatrixRows = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            if (isActualRepeatedEquipmentMatrixRow(rows.get(index))) {
                equipmentMatrixRows.add(index);
            }
        }
        int continuationIndex = resolveSourceConsistentRepeatedEquipmentMatrixContinuationIndex(
                rows, equipmentMatrixRows, columnCount);
        if (continuationIndex < 0) {
            return;
        }
        closeOpenVerticalMergesBeforeInsertion(rows, continuationIndex);
        rows.addAll(continuationIndex, cloneDocumentFrameRows(documentHeaderRows));
    }

    private void materializeSourceIndexedRowsToAvailableColumnCount(List<List<MesProBatchRecordParsedCell>> rows,
                                                                    int columnCount,
                                                                    int renderWidth) {
        if (rows == null || rows.isEmpty() || columnCount <= 0) {
            return;
        }
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            if (row.stream().noneMatch(cell -> cell != null && cell.getColumnIndex() != null)) {
                markBlockedColumns(row, rowIndex, blockedUntilRowByColumn);
                continue;
            }
            List<MesProBatchRecordParsedCell> materialized = new ArrayList<>();
            int cursor = 0;
            int rowEnd = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null) {
                    continue;
                }
                while (blockedUntilRowByColumn.getOrDefault(cursor, -1) >= rowIndex) {
                    cursor++;
                }
                int startColumn = cell.getColumnIndex() == null ? cursor : Math.max(0, cell.getColumnIndex());
                while (cursor < startColumn && cursor < columnCount) {
                    if (blockedUntilRowByColumn.getOrDefault(cursor, -1) < rowIndex) {
                        if (!materialized.isEmpty() || hasOpenBlockedColumnBefore(blockedUntilRowByColumn, rowIndex, startColumn)) {
                            materialized.add(buildVisualBlankCell(1, renderWidth, columnCount));
                        }
                    }
                    cursor++;
                }
                MesProBatchRecordParsedCell cloned = cloneCell(cell, 1.0f, true);
                cloned.setColumnIndex(null);
                int colSpan = Math.max(1, cloned.getColSpan());
                if (cursor + colSpan > columnCount) {
                    colSpan = Math.max(1, columnCount - cursor);
                    cloned.setColSpan(colSpan);
                }
                materialized.add(cloned);
                if (Math.max(1, cloned.getRowSpan()) > 1) {
                    for (int offset = 0; offset < colSpan && cursor + offset < columnCount; offset++) {
                        blockedUntilRowByColumn.put(cursor + offset, rowIndex + Math.max(1, cloned.getRowSpan()) - 1);
                    }
                }
                cursor += colSpan;
                rowEnd = Math.max(rowEnd, cursor);
            }
            boolean shouldFillTrailingSourceBlanks = shouldFillTrailingSourceBlanks(row);
            while (cursor < columnCount) {
                if (shouldFillTrailingSourceBlanks && blockedUntilRowByColumn.getOrDefault(cursor, -1) < rowIndex) {
                    materialized.add(buildVisualBlankCell(1, renderWidth, columnCount));
                    rowEnd = Math.max(rowEnd, cursor + 1);
                }
                cursor++;
            }
            if (rowEnd > columnCount) {
                materialized = normalizeRowToAvailableColumns(materialized, rowIndex, columnCount, renderWidth,
                        blockedUntilRowByColumn, List.of());
            }
            rows.set(rowIndex, materialized);
        }
    }

    private void materializeSourceIndexedParameterRowsToAvailableColumnCount(
            List<List<MesProBatchRecordParsedCell>> rows,
            int columnCount,
            int renderWidth) {
        if (rows == null || rows.isEmpty() || columnCount <= 0) {
            return;
        }
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            if (shouldMaterializeSourceIndexedRow(rows, rowIndex)) {
                rows.set(rowIndex, materializeSourceIndexedRow(row, rowIndex, columnCount, renderWidth,
                        blockedUntilRowByColumn));
            }
            markBlockedColumns(rows.get(rowIndex), rowIndex, blockedUntilRowByColumn);
        }
    }

    private boolean shouldMaterializeSourceIndexedRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
        return row != null
                && row.stream().anyMatch(cell -> cell != null && cell.getColumnIndex() != null)
                && isWithinSourceIndexedParameterBand(rows, rowIndex);
    }

    private boolean isWithinSourceIndexedParameterBand(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        int startIndex = Math.max(0, rowIndex - 2);
        int endIndex = Math.min(rows.size() - 1, rowIndex + 1);
        for (int index = startIndex; index <= endIndex; index++) {
            if (shouldFillTrailingSourceBlanks(rows.get(index))) {
                return true;
            }
        }
        return false;
    }

    private List<MesProBatchRecordParsedCell> materializeSourceIndexedRow(
            List<MesProBatchRecordParsedCell> row,
            int rowIndex,
            int columnCount,
            int renderWidth,
            Map<Integer, Integer> blockedUntilRowByColumn) {
        List<MesProBatchRecordParsedCell> materialized = new ArrayList<>();
        int cursor = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null) {
                continue;
            }
            while (blockedUntilRowByColumn.getOrDefault(cursor, -1) >= rowIndex) {
                cursor++;
            }
            int startColumn = cell.getColumnIndex() == null ? cursor : Math.max(0, cell.getColumnIndex());
            while (cursor < startColumn && cursor < columnCount) {
                if (blockedUntilRowByColumn.getOrDefault(cursor, -1) < rowIndex
                        && (!materialized.isEmpty()
                        || hasOpenBlockedColumnBefore(blockedUntilRowByColumn, rowIndex, startColumn))) {
                    materialized.add(buildVisualBlankCell(1, renderWidth, columnCount));
                }
                cursor++;
            }
            MesProBatchRecordParsedCell cloned = cloneCell(cell, 1.0f, true);
            cloned.setColumnIndex(null);
            int colSpan = Math.max(1, cloned.getColSpan());
            if (cursor + colSpan > columnCount) {
                colSpan = Math.max(1, columnCount - cursor);
                cloned.setColSpan(colSpan);
            }
            materialized.add(cloned);
            cursor += colSpan;
        }
        while (cursor < columnCount) {
            if (blockedUntilRowByColumn.getOrDefault(cursor, -1) < rowIndex) {
                materialized.add(buildVisualBlankCell(1, renderWidth, columnCount));
            }
            cursor++;
        }
        return materialized;
    }

    private MesProBatchRecordParsedCell buildVisualBlankCell(int colSpan, int renderWidth, int columnCount) {
        return MesProBatchRecordParsedCell.builder()
                .text("")
                .rowSpan(1)
                .colSpan(Math.max(1, colSpan))
                .bold(false)
                .fontSize(PROCESS_BODY_FONT_SIZE)
                .horizontalAlign("center")
                .verticalAlign(MesProBatchRecordReportShapeRules.DEFAULT_VERTICAL_ALIGN)
                .widthPx(resolveSpanWidth(Math.max(1, Math.round(renderWidth / (float) Math.max(1, columnCount)))
                        * Math.max(1, colSpan), Math.max(1, colSpan)))
                .heightPx(MesProBatchRecordReportShapeRules.MIN_ROW_HEIGHT_PX)
                .fillable(false)
                .visualBlank(true)
                .build();
    }

    private boolean shouldFillTrailingSourceBlanks(List<MesProBatchRecordParsedCell> row) {
        return hasSourceColumnGap(row)
                && (isRepeatedOperationParameterRow(row)
                || isRepeatedOperationDetailRow(row));
    }

    private boolean hasSourceColumnGap(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        int cursor = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null) {
                continue;
            }
            Integer columnIndex = cell.getColumnIndex();
            if (columnIndex != null && columnIndex > cursor) {
                return true;
            }
            cursor = Math.max(cursor, (columnIndex == null ? cursor : columnIndex) + Math.max(1, cell.getColSpan()));
        }
        return false;
    }

    private void closeOpenVerticalMergesBeforeInsertion(List<List<MesProBatchRecordParsedCell>> rows,
                                                        int insertionIndex) {
        for (int rowIndex = 0; rowIndex < insertionIndex; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            for (MesProBatchRecordParsedCell cell : row) {
                int rowSpan = Math.max(1, cell.getRowSpan());
                int allowedRowSpan = insertionIndex - rowIndex;
                if (rowSpan > allowedRowSpan) {
                    cell.setRowSpan(Math.max(1, allowedRowSpan));
                }
            }
        }
    }

    private int resolveSourceConsistentRepeatedEquipmentMatrixContinuationIndex(
            List<List<MesProBatchRecordParsedCell>> rows,
            List<Integer> equipmentMatrixRows,
            int columnCount) {
        if (equipmentMatrixRows.size() < 8 || columnCount <= 0) {
            return -1;
        }
        long fullWidthEquipmentRows = equipmentMatrixRows.stream()
                .filter(index -> resolveRowEndColumn(rows.get(index)) >= columnCount)
                .count();
        if (fullWidthEquipmentRows < Math.max(8, Math.round(equipmentMatrixRows.size() * 0.8f))) {
            return -1;
        }
        return equipmentMatrixRows.get(equipmentMatrixRows.size() / 2);
    }

    private int countDocumentHeaderRows(List<List<MesProBatchRecordParsedCell>> rows) {
        int count = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (isDocumentHeaderLayoutRow(row)) {
                count++;
            }
        }
        return count;
    }

    private int countDocumentHeaderGroups(List<List<MesProBatchRecordParsedCell>> rows) {
        int count = 0;
        boolean previousRowWasHeader = false;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            boolean currentRowIsHeader = isDocumentHeaderLayoutRow(row);
            if (currentRowIsHeader && !previousRowWasHeader) {
                count++;
            }
            previousRowWasHeader = currentRowIsHeader;
        }
        return count;
    }

    private int resolveRepeatedOperationContinuationIndex(List<List<MesProBatchRecordParsedCell>> rows) {
        List<Integer> operationParameterRows = new ArrayList<>();
        List<Integer> equipmentMatrixRows = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            if (isRepeatedOperationParameterRow(rows.get(index))) {
                operationParameterRows.add(index);
            }
            if (isRepeatedEquipmentMatrixRow(rows.get(index))) {
                equipmentMatrixRows.add(index);
            }
        }
        if (operationParameterRows.size() < 6) {
            return resolveRepeatedEquipmentMatrixContinuationIndex(rows, equipmentMatrixRows);
        }
        return operationParameterRows.get(3);
    }

    private int resolveRepeatedEquipmentMatrixContinuationIndex(List<List<MesProBatchRecordParsedCell>> rows,
                                                               List<Integer> equipmentMatrixRows) {
        if (equipmentMatrixRows.size() < 8) {
            return -1;
        }
        int continuationIndex = equipmentMatrixRows.get(equipmentMatrixRows.size() / 2);
        if (isInsideOpenVerticalMergeBand(rows, continuationIndex)) {
            return -1;
        }
        return continuationIndex;
    }

    private boolean isInsideOpenVerticalMergeBand(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        for (int index = 0; index < rowIndex; index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            if (row == null || row.isEmpty()) {
                continue;
            }
            for (MesProBatchRecordParsedCell cell : row) {
                if (Math.max(1, cell.getRowSpan()) > rowIndex - index) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRepeatedOperationParameterRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        return row.size() >= 5
                && countNonBlankCells(row) >= 3
                && countShortLabelCells(row) >= 3
                && countSlashCells(row) <= 1
                && sumColSpans(row) >= 10;
    }

    private boolean isRepeatedEquipmentMatrixRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        return row.size() >= 5
                && countChecklistChoiceCells(row) >= 2
                && countNonBlankCells(row) >= 3
                && sumColSpans(row) >= 10;
    }

    private boolean isActualRepeatedEquipmentMatrixRow(List<MesProBatchRecordParsedCell> row) {
        return isRepeatedEquipmentMatrixRow(row);
    }

    private void normalizeChecklistNarrativeBands(List<List<MesProBatchRecordParsedCell>> rows,
                                                  int columnCount,
                                                  int renderWidth,
                                                  boolean processTemplate) {
        if (columnCount < 6 || (!processTemplate && !containsChecklistNarrativeBandCandidate(rows)
                && !containsChecklistNarrativeBodyBehindOpenSideHeaderCandidate(rows))) {
            return;
        }
        for (int rowIndex = resolveLeadingDocumentHeaderRowCount(rows); rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (isDenseVisualChecklistNarrativeBandHeaderRow(row)) {
                int bandEndIndex = Math.min(rows.size() - 1, rowIndex + Math.max(1, row.get(0).getRowSpan()) - 1);
                if (!containsChecklistNarrativeBandRows(rows, rowIndex + 1, bandEndIndex)) {
                    continue;
                }
                int semanticColumnCount = resolveChecklistNarrativeSemanticColumnCount(row, columnCount);
                int semanticRenderWidth = resolveSemanticRenderWidth(renderWidth, columnCount, semanticColumnCount);
                rows.set(rowIndex, normalizeChecklistNarrativeBandHeaderRow(row, semanticColumnCount, semanticRenderWidth));
                int sideHeaderColumns = Math.max(1, rows.get(rowIndex).get(0).getColSpan());
                normalizeChecklistNarrativeBodyRowsInSpan(rows, rowIndex, bandEndIndex,
                        sideHeaderColumns, semanticColumnCount, semanticRenderWidth);
                continue;
            }
            if (isChecklistOutcomeAlignmentHeaderRow(row)
                    && shouldNormalizeChecklistOutcomeAlignmentRow(row, columnCount)) {
                int bodyEndIndex = resolveChecklistOutcomeBodyEndIndex(rows, rowIndex + 1);
                rows.set(rowIndex, normalizeChecklistOutcomeAlignmentHeaderRow(
                        row, columnCount, renderWidth, Math.max(0, bodyEndIndex - rowIndex)));
                int blockedColumns = Math.max(1, rows.get(rowIndex).get(0).getColSpan());
                for (int bodyRowIndex = rowIndex + 1; bodyRowIndex <= bodyEndIndex; bodyRowIndex++) {
                    List<MesProBatchRecordParsedCell> bodyRow = rows.get(bodyRowIndex);
                    if (!isChecklistOutcomeBodyRow(bodyRow)) {
                        continue;
                    }
                    rows.set(bodyRowIndex, normalizeChecklistOutcomeBodyRow(
                            bodyRow, blockedColumns, columnCount, renderWidth));
                }
                rowIndex = Math.max(rowIndex, bodyEndIndex);
                continue;
            }
            if (!isChecklistNarrativeBandHeaderRow(row)) {
                if (columnCount > PROCESS_TEMPLATE_TOTAL_COL_SPAN) {
                    normalizeChecklistNarrativeBodyBehindOpenSideHeader(rows, rowIndex, columnCount, renderWidth);
                }
                continue;
            }
            int bandEndIndex = Math.min(rows.size() - 1, rowIndex + Math.max(1, row.get(0).getRowSpan()) - 1);
            if (!containsChecklistNarrativeBandRows(rows, rowIndex + 1, bandEndIndex)) {
                continue;
            }
            int semanticColumnCount = resolveChecklistNarrativeSemanticColumnCount(row, columnCount);
            int semanticRenderWidth = resolveSemanticRenderWidth(renderWidth, columnCount, semanticColumnCount);
            rows.set(rowIndex, normalizeChecklistNarrativeBandHeaderRow(row, semanticColumnCount, semanticRenderWidth));
            int sideHeaderColumns = Math.max(1, rows.get(rowIndex).get(0).getColSpan());
            normalizeChecklistNarrativeBodyRowsInSpan(rows, rowIndex, bandEndIndex,
                    sideHeaderColumns, semanticColumnCount, semanticRenderWidth);
        }
    }

    private void normalizeChecklistNarrativeBodyRowsInSpan(List<List<MesProBatchRecordParsedCell>> rows,
                                                           int headerRowIndex,
                                                           int bandEndIndex,
                                                           int sideHeaderColumns,
                                                           int semanticColumnCount,
                                                           int semanticRenderWidth) {
        for (int bodyRowIndex = headerRowIndex + 1; bodyRowIndex <= bandEndIndex; bodyRowIndex++) {
            List<MesProBatchRecordParsedCell> bodyRow = rows.get(bodyRowIndex);
            if (!isChecklistNarrativeBandRow(bodyRow)) {
                continue;
            }
            rows.set(bodyRowIndex,
                    normalizeChecklistNarrativeBandRow(bodyRow, Math.max(1, semanticColumnCount - sideHeaderColumns),
                            semanticRenderWidth, semanticColumnCount));
        }
    }

    private void normalizeChecklistNarrativeBodyBehindOpenSideHeader(List<List<MesProBatchRecordParsedCell>> rows,
                                                                     int rowIndex,
                                                                     int columnCount,
                                                                     int renderWidth) {
        if (rowIndex <= 0 || rowIndex >= rows.size()) {
            return;
        }
        List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
        if (!isChecklistNarrativeBandRow(row)) {
            return;
        }
        MesProBatchRecordParsedCell sideHeaderCell = findOpenSideHeaderAbove(rows, rowIndex);
        if (sideHeaderCell == null || !isShortLabelText(compactStructureToken(textOf(sideHeaderCell)))) {
            return;
        }
        int semanticColumnCount = resolveChecklistNarrativeSemanticColumnCount(rows.get(rowIndex - 1), columnCount);
        int semanticRenderWidth = resolveSemanticRenderWidth(renderWidth, columnCount, semanticColumnCount);
        int sideHeaderColumns = Math.max(1,
                Math.min(Math.max(1, sideHeaderCell.getColSpan()), Math.max(1, semanticColumnCount - 1)));
        rows.set(rowIndex,
                normalizeChecklistNarrativeBandRow(row, Math.max(1, semanticColumnCount - sideHeaderColumns),
                        semanticRenderWidth, semanticColumnCount));
    }

    private MesProBatchRecordParsedCell findOpenSideHeaderAbove(List<List<MesProBatchRecordParsedCell>> rows,
                                                                int targetRowIndex) {
        for (int rowIndex = targetRowIndex - 1; rowIndex >= 0; rowIndex--) {
            int cursor = 0;
            for (MesProBatchRecordParsedCell cell : rows.get(rowIndex)) {
                int rowSpan = Math.max(1, cell.getRowSpan());
                int colSpan = Math.max(1, cell.getColSpan());
                if (cursor == 0 && rowIndex + rowSpan > targetRowIndex && colSpan > 1) {
                    return cell;
                }
                cursor += colSpan;
            }
        }
        return null;
    }

    private boolean containsChecklistNarrativeBandCandidate(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        for (int rowIndex = 0; rowIndex < rows.size() - 1; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (!isChecklistNarrativeBandHeaderRow(row)) {
                continue;
            }
            int bandEndIndex = Math.min(rows.size() - 1, rowIndex + Math.max(1, row.get(0).getRowSpan()) - 1);
            if (containsChecklistNarrativeBandRows(rows, rowIndex + 1, bandEndIndex)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsChecklistNarrativeBodyBehindOpenSideHeaderCandidate(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.size() < 2) {
            return false;
        }
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            if (isChecklistNarrativeBandRow(rows.get(rowIndex))
                    && findOpenSideHeaderAbove(rows, rowIndex) != null) {
                return true;
            }
        }
        return false;
    }

    private int resolveChecklistNarrativeSemanticColumnCount(List<MesProBatchRecordParsedCell> headerRow,
                                                             int columnCount) {
        if (columnCount <= PROCESS_TEMPLATE_TOTAL_COL_SPAN) {
            return columnCount;
        }
        int sourceSpan = headerRow == null
                ? 0
                : headerRow.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
        if (hasChecklistNarrativeHeaderTail(headerRow)) {
            return PROCESS_TEMPLATE_TOTAL_COL_SPAN;
        }
        if (sourceSpan >= PROCESS_TEMPLATE_TOTAL_COL_SPAN * 2
                || columnCount >= PROCESS_TEMPLATE_TOTAL_COL_SPAN * 2) {
            return PROCESS_TEMPLATE_TOTAL_COL_SPAN;
        }
        return columnCount;
    }

    private int resolveSemanticRenderWidth(int renderWidth, int columnCount, int semanticColumnCount) {
        if (semanticColumnCount >= columnCount) {
            return renderWidth;
        }
        return MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(semanticColumnCount);
    }

    private boolean isDenseVisualChecklistNarrativeBandHeaderRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 2) {
            return false;
        }
        int sourceSpan = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
        return sourceSpan > PROCESS_TEMPLATE_TOTAL_COL_SPAN * 2
                && isChecklistNarrativeBandHeaderRow(row);
    }

    private boolean isChecklistNarrativeBandHeaderRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 2) {
            return false;
        }
        MesProBatchRecordParsedCell firstCell = row.get(0);
        int rowSpan = Math.max(1, firstCell.getRowSpan());
        int colSpan = Math.max(1, firstCell.getColSpan());
        boolean shortSideHeader = isShortLabelText(compactStructureToken(textOf(firstCell)));
        if (rowSpan >= 2 && colSpan > 1 && shortSideHeader) {
            return true;
        }
        int sourceSpan = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
        return rowSpan >= 3
                && colSpan == 1
                && shortSideHeader
                && sourceSpan > PROCESS_TEMPLATE_TOTAL_COL_SPAN * 2
                && hasChecklistNarrativeHeaderTail(row);
    }

    private boolean hasChecklistNarrativeHeaderTail(List<MesProBatchRecordParsedCell> row) {
        String text = rowText(row);
        return text.contains("项目")
                && (text.contains("要求") || text.contains("结果"))
                && (text.contains("操作人") || text.contains("复核人"));
    }

    private boolean containsChecklistNarrativeBandRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                       int startRowIndex,
                                                       int endRowIndex) {
        for (int rowIndex = startRowIndex; rowIndex <= endRowIndex; rowIndex++) {
            if (isChecklistNarrativeBandRow(rows.get(rowIndex))) {
                return true;
            }
        }
        return false;
    }

    private boolean isChecklistNarrativeBandRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 4 || row.size() > 6) {
            return false;
        }
        int narrativeIndex = findNarrativeCellIndex(row);
        if (narrativeIndex != 1 || !isShortLabelText(textOf(row.get(0)))) {
            return false;
        }
        boolean hasTailOutcome = false;
        boolean hasTailBlank = false;
        for (int index = narrativeIndex + 1; index < row.size(); index++) {
            String text = textOf(row.get(index));
            if (containsChecklistChoice(text) || isShortLabelText(text)) {
                hasTailOutcome = true;
            }
            if (text.isBlank() || row.get(index).isVisualBlank()) {
                hasTailBlank = true;
            }
        }
        return hasTailOutcome && hasTailBlank;
    }

    private int findNarrativeCellIndex(List<MesProBatchRecordParsedCell> row) {
        int narrativeIndex = -1;
        for (int index = 0; index < row.size(); index++) {
            if (!looksLikeParagraphText(textOf(row.get(index)))) {
                continue;
            }
            if (narrativeIndex >= 0) {
                return -1;
            }
            narrativeIndex = index;
        }
        return narrativeIndex;
    }

    private boolean containsChecklistChoice(String text) {
        return normalizeStructureToken(text).contains("□");
    }

    private void normalizeExplicitMaterialMatrixHeaderRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                           int columnCount,
                                                           int renderWidth,
                                                           boolean processTemplate) {
        if (!processTemplate || columnCount < PACKED_MATERIAL_MATRIX_HEADER_COUNT + 1) {
            return;
        }
        for (int rowIndex = resolveLeadingDocumentHeaderRowCount(rows); rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (!isExplicitMaterialMatrixHeaderRow(row)) {
                continue;
            }
            rows.set(rowIndex, normalizeExplicitMaterialMatrixHeaderRow(rows, rowIndex, columnCount, renderWidth));
        }
    }

    private boolean isExplicitMaterialMatrixHeaderRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() != PACKED_MATERIAL_MATRIX_HEADER_COUNT + 1) {
            return false;
        }
        MesProBatchRecordParsedCell sideHeaderCell = row.get(0);
        if (sideHeaderCell == null || Math.max(1, sideHeaderCell.getColSpan()) != 1) {
            return false;
        }
        String sideHeaderText = textOf(sideHeaderCell).replace("\n", "").trim();
        if (sideHeaderText.isBlank()) {
            return false;
        }
        return isRepeatedHeaderCellPattern(row.subList(1, row.size()));
    }

    private boolean isStandaloneMaterialMatrixHeaderRow(List<MesProBatchRecordParsedCell> row) {
        return row != null
                && row.size() == PACKED_MATERIAL_MATRIX_HEADER_COUNT
                && isRepeatedHeaderCellPattern(row);
    }

    private List<MesProBatchRecordParsedCell> normalizeExplicitMaterialMatrixHeaderRow(
            List<List<MesProBatchRecordParsedCell>> rows, int rowIndex, int columnCount, int renderWidth) {
        List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
        MesProBatchRecordParsedCell sideHeaderCell = cloneCell(row.get(0), 1.0f);
        sideHeaderCell.setColSpan(1);
        sideHeaderCell.setRowSpan(1);
        int availableColumns = Math.max(1, columnCount - 1);
        List<MesProBatchRecordParsedCell> headerCells = new ArrayList<>();
        for (int index = 1; index < row.size(); index++) {
            headerCells.add(cloneCell(row.get(index), 1.0f));
        }
        int[] colSpans = resolvePackedMaterialHeaderColSpans(headerCells, availableColumns);
        List<MesProBatchRecordParsedCell> normalized = new ArrayList<>();
        normalized.add(sideHeaderCell);
        normalized.addAll(cloneRowWithDistributedSpans(headerCells, 1.0f, renderWidth, availableColumns, colSpans, null));
        return tuneProcessRow(normalized, resolveStructuredFontSize(row), resolveStructuredHeight(row), true);
    }

    private List<MesProBatchRecordParsedCell> normalizeStandaloneMaterialMatrixHeaderRow(
            List<MesProBatchRecordParsedCell> row, int columnCount, int renderWidth) {
        if (columnCount < PACKED_MATERIAL_MATRIX_HEADER_COUNT) {
            return tuneProcessRow(cloneRowWithAdaptiveSpans(row, 1.0f, renderWidth, row.size()),
                    resolveStructuredFontSize(row), resolveStructuredHeight(row), true);
        }
        int[] colSpans = resolveStandaloneMaterialHeaderColSpans(columnCount);
        return tuneProcessRow(cloneRowWithDistributedSpans(row, 1.0f, renderWidth, columnCount, colSpans, null),
                resolveStructuredFontSize(row), resolveStructuredHeight(row), true);
    }

    private int[] resolvePackedMaterialHeaderColSpans(List<MesProBatchRecordParsedCell> headerCells, int availableColumns) {
        if (headerCells == null || headerCells.size() != PACKED_MATERIAL_MATRIX_HEADER_COUNT) {
            return distributeAdaptiveColSpans(headerCells, availableColumns);
        }
        int[] preferred = {5, 5, 1, 4, 4, 1};
        int preferredSum = sum(preferred);
        if (preferredSum == availableColumns) {
            return preferred;
        }
        int[] scaled = new int[preferred.length];
        int assigned = 0;
        for (int index = 0; index < preferred.length; index++) {
            scaled[index] = Math.max(1, Math.round(preferred[index] * availableColumns / (float) preferredSum));
            assigned += scaled[index];
        }
        scaled[scaled.length - 1] = Math.max(1, scaled[scaled.length - 1] + (availableColumns - assigned));
        return sum(scaled) == availableColumns ? scaled : distributeAdaptiveColSpans(headerCells, availableColumns);
    }

    private int[] resolveStandaloneMaterialHeaderColSpans(int columnCount) {
        if (columnCount == 10) {
            return new int[]{1, 2, 2, 1, 2, 2};
        }
        int[] spans = new int[PACKED_MATERIAL_MATRIX_HEADER_COUNT];
        int baseSpan = Math.max(1, columnCount / PACKED_MATERIAL_MATRIX_HEADER_COUNT);
        int remainder = Math.max(0, columnCount - baseSpan * PACKED_MATERIAL_MATRIX_HEADER_COUNT);
        for (int index = 0; index < spans.length; index++) {
            spans[index] = baseSpan + (index < remainder ? 1 : 0);
        }
        return spans;
    }

    private void normalizeOperationInstructionBands(List<List<MesProBatchRecordParsedCell>> rows,
                                                    int columnCount,
                                                    int renderWidth,
                                                    boolean processTemplate) {
        if (!processTemplate || columnCount < 12) {
            return;
        }
        for (int rowIndex = resolveLeadingDocumentHeaderRowCount(rows); rowIndex < rows.size() - 1; rowIndex++) {
            List<MesProBatchRecordParsedCell> equipmentRow = rows.get(rowIndex);
            List<MesProBatchRecordParsedCell> bodyRow = rows.get(rowIndex + 1);
            if (!isOperationInstructionEquipmentRow(equipmentRow) || !isOperationInstructionBodyRow(bodyRow)) {
                continue;
            }
            MesProBatchRecordParsedCell sharedSideHeader = resolveOrRestoreSharedOperationBandSideHeader(
                    rows, rowIndex, columnCount);
            int availableColumns = resolveOperationBandAvailableColumns(columnCount, sharedSideHeader);
            if (availableColumns < 10) {
                continue;
            }
            List<MesProBatchRecordParsedCell> normalizedEquipmentRow = normalizeOperationInstructionEquipmentRow(
                    equipmentRow, availableColumns, renderWidth, sharedSideHeader != null);
            List<MesProBatchRecordParsedCell> normalizedBodyRow = normalizeOperationInstructionBodyRow(
                    bodyRow, availableColumns, renderWidth, sharedSideHeader != null);
            rows.set(rowIndex, normalizedEquipmentRow);
            rows.set(rowIndex + 1, normalizedBodyRow);
            restoreOperationInstructionDetailRows(rows, rowIndex + 1, normalizedBodyRow, sharedSideHeader != null);
        }
    }

    private void restoreOperationInstructionVerticalMainCells(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.size() < 2) {
            return;
        }
        for (int rowIndex = resolveLeadingDocumentHeaderRowCount(rows); rowIndex < rows.size() - 1; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (!isOperationInstructionBodyRow(row)) {
                continue;
            }
            int continuationCount = countOperationInstructionBlankDetailRows(rows, rowIndex + 1);
            if (continuationCount <= 0) {
                continue;
            }
            int rowSpan = continuationCount + 1;
            setRowSpanForCellContaining(row, "生产自检", rowSpan);
            setRowSpanForCellContaining(row, "合格标准", rowSpan);
        }
    }

    private int countOperationInstructionBlankDetailRows(List<List<MesProBatchRecordParsedCell>> rows, int startRowIndex) {
        int continuationCount = 0;
        for (int rowIndex = startRowIndex; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (rowText(row).contains("生产批量汇总")) {
                break;
            }
            if (!isOperationInstructionDetailBlankRow(row)) {
                break;
            }
            continuationCount++;
        }
        return continuationCount;
    }

    private void setRowSpanForCellContaining(List<MesProBatchRecordParsedCell> row, String fragment, int rowSpan) {
        MesProBatchRecordParsedCell cell = findCellContaining(row, fragment);
        if (cell != null) {
            cell.setRowSpan(Math.max(Math.max(1, cell.getRowSpan()), rowSpan));
        }
    }

    private boolean isOperationInstructionEquipmentRow(List<MesProBatchRecordParsedCell> row) {
        String text = rowText(row);
        return text.contains("设备编码") && text.contains("是否在计量效期内");
    }

    private boolean isOperationInstructionBodyRow(List<MesProBatchRecordParsedCell> row) {
        String text = rowText(row);
        return text.contains("操作日期")
                && text.contains("生产自检")
                && text.contains("合格标准")
                && text.contains("生产数量/pcs");
    }

    private List<MesProBatchRecordParsedCell> normalizeOperationInstructionEquipmentRow(
            List<MesProBatchRecordParsedCell> row, int availableColumns, int renderWidth, boolean sharedSideHeaderPresent) {
        MesProBatchRecordParsedCell equipmentLabel = findCellContaining(row, "设备编码");
        MesProBatchRecordParsedCell equipmentValue = findFirstNonEmptyCellAfter(row, equipmentLabel, "是否在计量效期内");
        MesProBatchRecordParsedCell validityLabel = findCellContaining(row, "是否在计量效期内");
        MesProBatchRecordParsedCell validityValue = findCellContaining(row, "□");
        if (equipmentLabel == null || equipmentValue == null || validityLabel == null || validityValue == null) {
            return row;
        }
        int effectiveAvailableColumns = resolveOperationInstructionStructuredColumnCount(row, availableColumns);
        int labelSpan = sharedSideHeaderPresent ? OPERATION_BAND_LABEL_COL_SPAN : OPERATION_BAND_LABEL_COL_SPAN;
        int validityLabelSpan = 6;
        int validityValueSpan = 2;
        int valueSpan = effectiveAvailableColumns - labelSpan - validityLabelSpan - validityValueSpan;
        if (valueSpan < 6) {
            return row;
        }
        List<MesProBatchRecordParsedCell> normalized = new ArrayList<>();
        MesProBatchRecordParsedCell clonedLabel = cloneCell(equipmentLabel, 1.0f);
        clonedLabel.setColSpan(labelSpan);
        normalized.add(clonedLabel);

        MesProBatchRecordParsedCell clonedValue = cloneCell(equipmentValue, 1.0f);
        clonedValue.setColSpan(valueSpan);
        normalized.add(clonedValue);

        MesProBatchRecordParsedCell clonedValidityLabel = cloneCell(validityLabel, 1.0f);
        clonedValidityLabel.setColSpan(validityLabelSpan);
        normalized.add(clonedValidityLabel);

        MesProBatchRecordParsedCell clonedValidityValue = cloneCell(validityValue, 1.0f);
        clonedValidityValue.setColSpan(validityValueSpan);
        normalized.add(clonedValidityValue);
        return tuneProcessRow(normalized, resolveStructuredFontSize(row),
                sharedSideHeaderPresent ? Math.max(52, resolveStructuredHeight(row)) : Math.max(32, resolveStructuredHeight(row)),
                true);
    }

    private List<MesProBatchRecordParsedCell> normalizeOperationInstructionBodyRow(
            List<MesProBatchRecordParsedCell> row, int availableColumns, int renderWidth, boolean sharedSideHeaderPresent) {
        MesProBatchRecordParsedCell dateCell = findCellContaining(row, "操作日期");
        MesProBatchRecordParsedCell selfInspectionCell = findCellContaining(row, "生产自检");
        MesProBatchRecordParsedCell narrativeCell = findCellContaining(row, "合格标准");
        if (dateCell == null || selfInspectionCell == null || narrativeCell == null) {
            return row;
        }
        List<MesProBatchRecordParsedCell> tailCells = new ArrayList<>();
        for (MesProBatchRecordParsedCell cell : row) {
            String text = textOf(cell);
            if (text.contains("生产数量/pcs")
                    || text.contains("自检合格数量/pcs")
                    || text.contains("不合格数量/pcs")
                    || "操作人".equals(text)
                    || "复核人".equals(text)) {
                tailCells.add(cell);
            }
        }
        if (tailCells.size() < 5) {
            return row;
        }
        int effectiveAvailableColumns = resolveOperationInstructionStructuredColumnCount(row, availableColumns);
        int dateSpan = sharedSideHeaderPresent ? 1 : 1;
        int selfInspectionSpan = sharedSideHeaderPresent ? 3 : OPERATION_BAND_SELF_INSPECTION_COL_SPAN;
        int[] tailColSpans = resolveOperationInstructionTailColSpans(tailCells, sharedSideHeaderPresent);
        int fixedTailWidth = 0;
        for (int span : tailColSpans) {
            fixedTailWidth += span;
        }
        int narrativeSpan = effectiveAvailableColumns - dateSpan - selfInspectionSpan - fixedTailWidth;
        if (narrativeSpan < 8) {
            return row;
        }
        List<MesProBatchRecordParsedCell> normalized = new ArrayList<>();
        MesProBatchRecordParsedCell clonedDateCell = cloneCell(dateCell, 1.0f);
        clonedDateCell.setColSpan(dateSpan);
        normalized.add(clonedDateCell);

        MesProBatchRecordParsedCell clonedSelfInspectionCell = cloneCell(selfInspectionCell, 1.0f);
        clonedSelfInspectionCell.setColSpan(selfInspectionSpan);
        normalized.add(clonedSelfInspectionCell);

        MesProBatchRecordParsedCell clonedNarrativeCell = cloneCell(narrativeCell, 1.0f);
        clonedNarrativeCell.setColSpan(narrativeSpan);
        alignCellLeft(clonedNarrativeCell);
        normalized.add(clonedNarrativeCell);

        for (int index = 0; index < tailCells.size(); index++) {
            MesProBatchRecordParsedCell tailCell = tailCells.get(index);
            if (tailColSpans[index] <= 0) {
                continue;
            }
            MesProBatchRecordParsedCell clonedTailCell = cloneCell(tailCell, 1.0f);
            clonedTailCell.setColSpan(tailColSpans[index]);
            normalized.add(clonedTailCell);
        }
        int resolvedHeight = resolveChecklistNarrativeRowHeight(row, normalized);
        if (sharedSideHeaderPresent && textOf(narrativeCell).contains("合格标准")) {
            resolvedHeight = Math.max(resolvedHeight, 84);
        }
        return tuneProcessRow(normalized, PROCESS_BODY_FONT_SIZE, resolvedHeight, false);
    }

    private int[] resolveOperationInstructionTailColSpans(List<MesProBatchRecordParsedCell> tailCells,
                                                          boolean sharedSideHeaderPresent) {
        int[] colSpans = new int[tailCells.size()];
        for (int index = 0; index < colSpans.length; index++) {
            colSpans[index] = sharedSideHeaderPresent ? 1 : OPERATION_BAND_TAIL_COL_SPAN;
        }
        if (sharedSideHeaderPresent && colSpans.length >= 5) {
            return colSpans;
        }
        if (colSpans.length < 5) {
            return colSpans;
        }
        for (int index = colSpans.length - 1; index >= 0; index--) {
            String text = textOf(tailCells.get(index));
            if ("操作人".equals(text) || "复核人".equals(text)) {
                colSpans[index] = 1;
                return colSpans;
            }
        }
        return colSpans;
    }

    private int resolveOperationInstructionStructuredColumnCount(List<MesProBatchRecordParsedCell> row, int availableColumns) {
        int sourceSpan = row.stream()
                .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                .sum();
        if (sourceSpan >= 10) {
            return Math.max(10, Math.min(availableColumns, sourceSpan));
        }
        return Math.max(10, availableColumns);
    }

    private void restoreOperationInstructionDetailRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                       int bodyRowIndex,
                                                       List<MesProBatchRecordParsedCell> bodyTemplateRow,
                                                       boolean sharedSideHeaderPresent) {
        if (rows == null || bodyTemplateRow == null || bodyTemplateRow.isEmpty()) {
            return;
        }
        List<Integer> detailRowIndexes = new ArrayList<>();
        for (int rowIndex = bodyRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            if (rowText(row).contains("生产批量汇总")) {
                mergeOperationInstructionNarrativeBand(rows, bodyRowIndex, detailRowIndexes);
                rows.set(rowIndex, normalizeOperationInstructionSummaryRow(row, bodyTemplateRow, sharedSideHeaderPresent));
                break;
            }
            if (!isOperationInstructionDetailBlankRow(row)) {
                break;
            }
            rows.set(rowIndex, buildOperationInstructionDetailRowFromTemplate(row, bodyTemplateRow));
            detailRowIndexes.add(rowIndex);
        }
    }

    private boolean isOperationInstructionDetailBlankRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        return row.stream().allMatch(cell -> textOf(cell).isBlank() || cell.isVisualBlank());
    }

    private List<MesProBatchRecordParsedCell> buildOperationInstructionDetailRowFromTemplate(
            List<MesProBatchRecordParsedCell> sourceRow,
            List<MesProBatchRecordParsedCell> bodyTemplateRow) {
        List<MesProBatchRecordParsedCell> normalized = new ArrayList<>(bodyTemplateRow.size());
        int rowHeight = resolveStructuredHeight(sourceRow);
        for (MesProBatchRecordParsedCell templateCell : bodyTemplateRow) {
            MesProBatchRecordParsedCell cloned = cloneCell(templateCell, 1.0f);
            cloned.setText("");
            cloned.setBold(false);
            cloned.setRowSpan(1);
            cloned.setHeightPx(Math.max(18, rowHeight));
            normalized.add(cloned);
        }
        return tuneProcessRow(normalized, PROCESS_BODY_FONT_SIZE, Math.max(18, rowHeight), false);
    }

    private List<MesProBatchRecordParsedCell> normalizeOperationInstructionSummaryRow(
            List<MesProBatchRecordParsedCell> sourceRow,
            List<MesProBatchRecordParsedCell> bodyTemplateRow,
            boolean sharedSideHeaderPresent) {
        if (sourceRow == null || sourceRow.isEmpty() || bodyTemplateRow == null || bodyTemplateRow.size() < 4) {
            return sourceRow;
        }
        MesProBatchRecordParsedCell summaryCell = sourceRow.get(0);
        if (!textOf(summaryCell).contains("生产批量汇总")) {
            return sourceRow;
        }
        int leadingSpan = Math.max(1, bodyTemplateRow.get(0).getColSpan())
                + Math.max(1, bodyTemplateRow.get(1).getColSpan())
                + Math.max(1, bodyTemplateRow.get(2).getColSpan());
        List<MesProBatchRecordParsedCell> normalized = new ArrayList<>();
        MesProBatchRecordParsedCell clonedSummary = cloneCell(summaryCell, 1.0f);
        clonedSummary.setColSpan(leadingSpan);
        normalized.add(clonedSummary);
        for (int index = 3; index < bodyTemplateRow.size(); index++) {
            MesProBatchRecordParsedCell blank = cloneCell(bodyTemplateRow.get(index), 1.0f);
            blank.setText("");
            blank.setBold(false);
            blank.setRowSpan(1);
            normalized.add(blank);
        }
        if (!sharedSideHeaderPresent) {
            return normalized;
        }
        return normalized;
    }

    private void mergeOperationInstructionNarrativeBand(List<List<MesProBatchRecordParsedCell>> rows,
                                                        int bodyRowIndex,
                                                        List<Integer> detailRowIndexes) {
        if (rows == null || detailRowIndexes == null || detailRowIndexes.isEmpty()) {
            return;
        }
        List<MesProBatchRecordParsedCell> bodyRow = rows.get(bodyRowIndex);
        if (bodyRow == null || bodyRow.size() < 3) {
            return;
        }
        int mergedRowSpan = detailRowIndexes.size() + 1;
        // Remove continuation cells from right to left so later deletions do not shift
        // the remaining column skeleton and accidentally swallow neighboring blank cells.
        mergeOperationInstructionColumnGroup(bodyRow, detailRowIndexes, 2, mergedRowSpan, rows);
        mergeOperationInstructionColumnGroup(bodyRow, detailRowIndexes, 1, mergedRowSpan, rows);
    }

    private void mergeOperationInstructionColumnGroup(List<MesProBatchRecordParsedCell> bodyRow,
                                                      List<Integer> detailRowIndexes,
                                                      int bodyCellIndex,
                                                      int mergedRowSpan,
                                                      List<List<MesProBatchRecordParsedCell>> rows) {
        if (bodyCellIndex < 0 || bodyCellIndex >= bodyRow.size()) {
            return;
        }
        MesProBatchRecordParsedCell anchorCell = bodyRow.get(bodyCellIndex);
        anchorCell.setRowSpan(mergedRowSpan);
        for (Integer detailRowIndex : detailRowIndexes) {
            List<MesProBatchRecordParsedCell> detailRow = rows.get(detailRowIndex);
            if (detailRow == null || bodyCellIndex >= detailRow.size()) {
                continue;
            }
            detailRow.remove(bodyCellIndex);
        }
    }

    private MesProBatchRecordParsedCell resolveOrRestoreSharedOperationBandSideHeader(
            List<List<MesProBatchRecordParsedCell>> rows, int equipmentRowIndex, int columnCount) {
        MesProBatchRecordParsedCell existing = findSharedOperationBandSideHeader(rows, equipmentRowIndex);
        if (existing != null) {
            return existing;
        }
        int anchorRowIndex = findOperationBandAnchorRowIndex(rows, equipmentRowIndex, columnCount);
        if (anchorRowIndex < 0) {
            return null;
        }
        MesProBatchRecordParsedCell anchorCell = rows.get(anchorRowIndex).get(0);
        int continuationCount = countOperationBandContinuationRows(rows, anchorRowIndex, columnCount);
        if (continuationCount < 2) {
            return null;
        }
        anchorCell.setRowSpan(Math.max(Math.max(1, anchorCell.getRowSpan()), 1 + continuationCount));
        return anchorCell;
    }

    private MesProBatchRecordParsedCell findSharedOperationBandSideHeader(List<List<MesProBatchRecordParsedCell>> rows,
                                                                          int targetRowIndex) {
        if (rows == null || targetRowIndex <= 0) {
            return null;
        }
        for (int rowIndex = targetRowIndex - 1; rowIndex >= 0; rowIndex--) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            MesProBatchRecordParsedCell firstCell = row.get(0);
            if (Math.max(1, firstCell.getColSpan()) != 1 || Math.max(1, firstCell.getRowSpan()) < 2) {
                continue;
            }
            if (rowIndex + Math.max(1, firstCell.getRowSpan()) - 1 >= targetRowIndex) {
                return firstCell;
            }
        }
        return null;
    }

    private int findOperationBandAnchorRowIndex(List<List<MesProBatchRecordParsedCell>> rows,
                                                int equipmentRowIndex,
                                                int columnCount) {
        if (rows == null || equipmentRowIndex <= 0) {
            return -1;
        }
        for (int rowIndex = equipmentRowIndex - 1; rowIndex >= 0; rowIndex--) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (looksLikeOperationBandAnchorRow(row, columnCount)) {
                return rowIndex;
            }
            if (looksLikeIndependentOperationSectionBoundary(row)) {
                break;
            }
        }
        return -1;
    }

    private int countOperationBandContinuationRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                   int anchorRowIndex,
                                                   int columnCount) {
        int continuationCount = 0;
        for (int rowIndex = anchorRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (!looksLikeOperationBandContinuationRow(row, columnCount)) {
                break;
            }
            continuationCount++;
        }
        return continuationCount;
    }

    private boolean looksLikeOperationBandAnchorRow(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (row == null || row.size() < 2) {
            return false;
        }
        MesProBatchRecordParsedCell firstCell = row.get(0);
        if (Math.max(1, firstCell.getColSpan()) != 1 || firstCell.getWidthPx() > 180) {
            return false;
        }
        String firstText = textOf(firstCell).replace("\n", "").trim();
        if (firstText.isBlank() || firstText.length() > 20) {
            return false;
        }
        int totalSpan = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
        if (totalSpan < columnCount - 1) {
            return false;
        }
        int repeatedHeaderCount = 0;
        boolean hasMultiLineBody = false;
        List<String> seen = new ArrayList<>();
        for (int index = 1; index < row.size(); index++) {
            String text = textOf(row.get(index)).trim();
            if (text.contains("\n")) {
                hasMultiLineBody = true;
            }
            if (text.isBlank() || text.length() > 10) {
                continue;
            }
            if (seen.contains(text)) {
                repeatedHeaderCount++;
            } else {
                seen.add(text);
            }
        }
        return hasMultiLineBody || repeatedHeaderCount >= 1;
    }

    private boolean looksLikeOperationBandContinuationRow(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        if (looksLikeIndependentOperationSectionBoundary(row)) {
            return false;
        }
        if (isOperationInstructionEquipmentRow(row)
                || isOperationInstructionBodyRow(row)
                || rowText(row).contains("生产批量汇总")) {
            return true;
        }
        int totalSpan = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
        return totalSpan >= Math.max(3, columnCount - 5);
    }

    private boolean looksLikeIndependentOperationSectionBoundary(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        MesProBatchRecordParsedCell firstCell = row.get(0);
        String firstText = textOf(firstCell).replace("\n", "").trim();
        if (firstText.isBlank()) {
            return false;
        }
        if (Math.max(1, firstCell.getColSpan()) != 1 || firstCell.getWidthPx() > 180) {
            return false;
        }
        return firstText.endsWith("记录")
                || firstText.endsWith("信息")
                || firstText.contains("工序");
    }

    private int resolveOperationBandAvailableColumns(int columnCount, MesProBatchRecordParsedCell sharedSideHeader) {
        if (sharedSideHeader == null) {
            return Math.max(1, columnCount);
        }
        int blockedColumns = Math.max(1, sharedSideHeader.getColSpan());
        return Math.max(1, columnCount - blockedColumns);
    }

    private MesProBatchRecordParsedCell findCellContaining(List<MesProBatchRecordParsedCell> row, String fragment) {
        if (row == null || fragment == null) {
            return null;
        }
        for (MesProBatchRecordParsedCell cell : row) {
            if (textOf(cell).contains(fragment)) {
                return cell;
            }
        }
        return null;
    }

    private MesProBatchRecordParsedCell findFirstNonEmptyCellAfter(List<MesProBatchRecordParsedCell> row,
                                                                   MesProBatchRecordParsedCell anchor,
                                                                   String excludedFragment) {
        if (row == null || anchor == null) {
            return null;
        }
        boolean found = false;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == anchor) {
                found = true;
                continue;
            }
            if (!found) {
                continue;
            }
            String text = textOf(cell);
            if (!text.isBlank() && (excludedFragment == null || !text.contains(excludedFragment)) && !containsChecklistChoice(text)) {
                return cell;
            }
        }
        return null;
    }

    private boolean isChecklistOutcomeAlignmentHeaderRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 4) {
            return false;
        }
        String firstText = textOf(row.get(0)).replace("\n", "");
        if (!firstText.contains("生产前检查记录") && !firstText.contains("生产后清场记录")) {
            return false;
        }
        int matched = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            String text = textOf(cell);
            for (String label : CHECKLIST_HEADER_LABELS) {
                if (label.equals(text)) {
                    matched++;
                    break;
                }
            }
        }
        return matched >= 3;
    }

    private boolean shouldNormalizeChecklistOutcomeAlignmentRow(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (columnCount > PROCESS_TEMPLATE_TOTAL_COL_SPAN) {
            return true;
        }
        int sourceSpan = row.stream()
                .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                .sum();
        return sourceSpan > columnCount;
    }

    private List<MesProBatchRecordParsedCell> normalizeChecklistOutcomeAlignmentHeaderRow(
            List<MesProBatchRecordParsedCell> row, int columnCount, int renderWidth, int bodyRowCount) {
        if (row.size() < 4 || columnCount < 5) {
            return row;
        }
        List<MesProBatchRecordParsedCell> normalized = new ArrayList<>();
        MesProBatchRecordParsedCell sideHeaderCell = cloneCell(row.get(0), 1.0f);
        sideHeaderCell.setColSpan(resolveChecklistHeaderSideSpan(row, columnCount));
        sideHeaderCell.setRowSpan(Math.max(Math.max(1, sideHeaderCell.getRowSpan()), 1 + bodyRowCount));
        sideHeaderCell.setHorizontalAlign("center");
        String normalizedText = textOf(sideHeaderCell).replace("\n", "");
        if (!normalizedText.isBlank()) {
            sideHeaderCell.setText(vertical(normalizedText));
        }
        normalized.add(sideHeaderCell);

        int availableColumns = Math.max(1, columnCount - sideHeaderCell.getColSpan());
        int[] tailColSpans = resolveChecklistOutcomeHeaderTailColSpans(availableColumns, row.size() - 2,
                rowFitsDeclaredColumnBudget(row, columnCount));
        int tailCellCount = tailColSpans.length;
        int narrativeSpan = Math.max(1,
                columnCount - sideHeaderCell.getColSpan() - sum(tailColSpans));
        MesProBatchRecordParsedCell checkCell = cloneCell(resolveChecklistHeaderCheckCell(row), 1.0f);
        checkCell.setColSpan(narrativeSpan);
        normalized.add(checkCell);

        for (int offset = 0; offset < tailCellCount; offset++) {
            int index = Math.max(2, row.size() - tailCellCount) + offset;
            MesProBatchRecordParsedCell cloned = cloneCell(row.get(index), 1.0f);
            cloned.setColSpan(tailColSpans[offset]);
            normalized.add(cloned);
        }
        return tuneProcessRow(normalized, resolveStructuredFontSize(row), resolveStructuredHeight(row), true);
    }

    private int resolveChecklistHeaderSideSpan(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (columnCount < 5) {
            return 1;
        }
        if (textOf(row.get(0)).contains("\n") && Math.max(1, row.get(0).getColSpan()) > 1) {
            return Math.min(Math.max(1, row.get(0).getColSpan()), Math.max(1, columnCount - 4));
        }
        int sourceSpan = row.stream()
                .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                .sum();
        if (sourceSpan > columnCount) {
            return Math.min(Math.max(1, row.get(0).getColSpan()), Math.max(1, columnCount - 4));
        }
        return Math.min(CHECKLIST_SIDE_HEADER_COL_SPAN, Math.max(1, columnCount - 4));
    }

    private MesProBatchRecordParsedCell resolveChecklistHeaderCheckCell(List<MesProBatchRecordParsedCell> row) {
        for (MesProBatchRecordParsedCell cell : row) {
            if ("检查要求".equals(textOf(cell))) {
                return cell;
            }
        }
        return row.get(1);
    }

    private int resolveChecklistOutcomeBodyEndIndex(List<List<MesProBatchRecordParsedCell>> rows, int startRowIndex) {
        int bodyEndIndex = startRowIndex - 1;
        for (int rowIndex = startRowIndex; rowIndex < rows.size(); rowIndex++) {
            if (!isChecklistOutcomeBodyRow(rows.get(rowIndex))) {
                break;
            }
            bodyEndIndex = rowIndex;
        }
        return bodyEndIndex;
    }

    private boolean isChecklistOutcomeBodyRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 2 || row.size() > 4) {
            return false;
        }
        if (textOf(row.get(0)).startsWith("备注")) {
            return false;
        }
        if (!looksLikeParagraphText(textOf(row.get(0)))) {
            return false;
        }
        boolean hasOutcomeChoice = false;
        for (int index = 1; index < row.size(); index++) {
            String text = textOf(row.get(index));
            if (containsChecklistChoice(text) || isShortLabelText(text)) {
                hasOutcomeChoice = true;
                break;
            }
        }
        return hasOutcomeChoice;
    }

    private List<MesProBatchRecordParsedCell> normalizeChecklistOutcomeBodyRow(
            List<MesProBatchRecordParsedCell> row, int blockedColumns, int columnCount, int renderWidth) {
        int availableColumns = Math.max(1, columnCount - Math.max(1, blockedColumns));
        int[] tailColSpans = Math.max(1, blockedColumns) > 1
                ? resolveCompactChecklistTailColSpans(row.size() - 1)
                : resolveChecklistOutcomeTailColSpans(availableColumns, row.size() - 1);
        if (availableColumns <= tailColSpans.length) {
            return row;
        }
        MesProBatchRecordParsedCell narrativeCell = cloneCell(row.get(0), 1.0f);
        int narrativeSpan = availableColumns - sum(tailColSpans);
        narrativeCell.setColSpan(narrativeSpan);
        alignCellLeft(narrativeCell);

        List<MesProBatchRecordParsedCell> normalized = new ArrayList<>();
        normalized.add(narrativeCell);
        int tailStartIndex = Math.max(1, row.size() - tailColSpans.length);
        for (int offset = 0; offset < tailColSpans.length; offset++) {
            int sourceIndex = tailStartIndex + offset;
            MesProBatchRecordParsedCell tailCell = cloneCell(row.get(sourceIndex), 1.0f);
            tailCell.setColSpan(tailColSpans[offset]);
            normalized.add(tailCell);
        }
        return tuneProcessRow(normalized, PROCESS_BODY_FONT_SIZE,
                resolveChecklistNarrativeRowHeight(row, normalized), false);
    }

    private boolean rowFitsDeclaredColumnBudget(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (row == null || columnCount <= 0) {
            return false;
        }
        int sourceSpan = row.stream()
                .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                .sum();
        return sourceSpan <= columnCount;
    }

    private int[] resolveChecklistOutcomeHeaderTailColSpans(int availableColumns, int tailCellCount,
                                                            boolean sourceFitsDeclaredGrid) {
        int resolvedTailCellCount = Math.max(0, Math.min(CHECKLIST_OUTCOME_FIXED_TAIL_COL_SPANS.length, tailCellCount));
        if (resolvedTailCellCount <= 0) {
            return new int[0];
        }
        if (availableColumns <= 20) {
            if (!sourceFitsDeclaredGrid) {
                return resolveCompactChecklistTailColSpans(resolvedTailCellCount);
            }
            int[] fixedTailSpans = resolveChecklistOutcomeTailColSpans(availableColumns, tailCellCount);
            return fixedTailSpans.length == resolvedTailCellCount
                    ? fixedTailSpans
                    : resolveCompactChecklistTailColSpans(resolvedTailCellCount);
        }
        return resolveChecklistOutcomeTailColSpans(availableColumns, tailCellCount);
    }

    private int[] resolveCompactChecklistTailColSpans(int tailCellCount) {
        int resolvedTailCellCount = Math.max(0, Math.min(CHECKLIST_OUTCOME_FIXED_TAIL_COL_SPANS.length, tailCellCount));
        int[] compact = new int[resolvedTailCellCount];
        for (int index = 0; index < resolvedTailCellCount; index++) {
            compact[index] = 1;
        }
        return compact;
    }

    private int[] resolveShiftedChecklistTailColSpans(int tailCellCount) {
        int[] compact = resolveCompactChecklistTailColSpans(tailCellCount);
        if (compact.length > 0) {
            compact[0] = Math.max(1, compact[0] + 1);
        }
        return compact;
    }

    private int[] resolveChecklistOutcomeTailColSpans(int availableColumns, int tailCellCount) {
        int resolvedTailCellCount = Math.max(0, Math.min(CHECKLIST_OUTCOME_FIXED_TAIL_COL_SPANS.length, tailCellCount));
        if (resolvedTailCellCount <= 0) {
            return new int[0];
        }
        int[] colSpans = new int[resolvedTailCellCount];
        int tailWidth = 0;
        for (int index = 0; index < resolvedTailCellCount; index++) {
            colSpans[index] = CHECKLIST_OUTCOME_FIXED_TAIL_COL_SPANS[index];
            tailWidth += colSpans[index];
        }
        if (tailWidth >= availableColumns) {
            for (int index = 0; index < resolvedTailCellCount; index++) {
                colSpans[index] = 1;
            }
        }
        return colSpans;
    }

    private int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    private List<Integer> buildChecklistOutcomeHeaderWidthHints(List<MesProBatchRecordParsedCell> row, int tailCellCount) {
        if (row == null || row.isEmpty()) {
            return List.of();
        }
        List<Integer> hints = new ArrayList<>(tailCellCount + 2);
        hints.add(resolveSourceCellWidth(row.get(0)));
        hints.add(resolveSourceCellWidth(resolveChecklistHeaderCheckCell(row)));
        int tailStart = Math.max(2, row.size() - tailCellCount);
        for (int index = tailStart; index < row.size(); index++) {
            hints.add(resolveSourceCellWidth(row.get(index)));
        }
        return hints;
    }

    private List<Integer> buildChecklistOutcomeBodyWidthHints(List<MesProBatchRecordParsedCell> row, int tailCellCount) {
        if (row == null || row.isEmpty()) {
            return List.of();
        }
        List<Integer> hints = new ArrayList<>(tailCellCount + 1);
        hints.add(resolveSourceCellWidth(row.get(0)));
        int tailStart = Math.max(1, row.size() - tailCellCount);
        for (int index = tailStart; index < row.size(); index++) {
            hints.add(resolveSourceCellWidth(row.get(index)));
        }
        return hints;
    }

    private List<Integer> buildWidthHints(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return List.of();
        }
        List<Integer> hints = new ArrayList<>(row.size());
        for (MesProBatchRecordParsedCell cell : row) {
            hints.add(resolveSourceCellWidth(cell));
        }
        return hints;
    }

    private int resolveSourceCellWidth(MesProBatchRecordParsedCell cell) {
        if (cell == null) {
            return MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX;
        }
        return Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX, cell.getWidthPx());
    }

    private void applySemanticGroupWidths(List<MesProBatchRecordParsedCell> normalizedRow,
                                          List<Integer> sourceWidthHints,
                                          int targetWidth,
                                          int fixedTailCellCount) {
        if (normalizedRow == null || normalizedRow.isEmpty()
                || sourceWidthHints == null || sourceWidthHints.size() != normalizedRow.size()
                || targetWidth <= 0) {
            return;
        }
        int[] weights = new int[sourceWidthHints.size()];
        int totalWeight = 0;
        for (int index = 0; index < sourceWidthHints.size(); index++) {
            weights[index] = Math.max(1, sourceWidthHints.get(index));
            totalWeight += weights[index];
        }
        if (fixedTailCellCount > 1 && weights.length >= fixedTailCellCount + 1) {
            int tailStart = weights.length - fixedTailCellCount;
            int tailTotal = 0;
            for (int index = tailStart; index < weights.length; index++) {
                tailTotal += weights[index];
            }
            int averageTailWidth = Math.max(1, Math.round(tailTotal / (float) fixedTailCellCount));
            for (int index = tailStart; index < weights.length; index++) {
                totalWeight -= weights[index];
                weights[index] = averageTailWidth;
                totalWeight += weights[index];
            }
        }

        int[] resolvedWidths = new int[normalizedRow.size()];
        double[] fractions = new double[normalizedRow.size()];
        int assignedWidth = 0;
        for (int index = 0; index < normalizedRow.size(); index++) {
            MesProBatchRecordParsedCell cell = normalizedRow.get(index);
            int minWidth = MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX * Math.max(1, cell.getColSpan());
            double exactWidth = targetWidth * (weights[index] / (double) Math.max(totalWeight, 1));
            int resolvedWidth = Math.max(minWidth, (int) Math.floor(exactWidth));
            resolvedWidths[index] = resolvedWidth;
            fractions[index] = exactWidth - Math.floor(exactWidth);
            assignedWidth += resolvedWidth;
        }
        while (assignedWidth < targetWidth) {
            int receiverIndex = findBestWidthReceiver(fractions);
            resolvedWidths[receiverIndex]++;
            fractions[receiverIndex] = 0;
            assignedWidth++;
        }
        while (assignedWidth > targetWidth) {
            int donorIndex = findBestSemanticWidthDonor(normalizedRow, resolvedWidths);
            if (donorIndex < 0) {
                break;
            }
            resolvedWidths[donorIndex]--;
            assignedWidth--;
        }
        for (int index = 0; index < normalizedRow.size(); index++) {
            normalizedRow.get(index).setWidthPx(resolvedWidths[index]);
        }
    }

    private int findBestSemanticWidthDonor(List<MesProBatchRecordParsedCell> normalizedRow, int[] resolvedWidths) {
        int bestIndex = -1;
        int bestSurplus = 0;
        for (int index = 0; index < normalizedRow.size(); index++) {
            int minWidth = MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX
                    * Math.max(1, normalizedRow.get(index).getColSpan());
            int surplus = resolvedWidths[index] - minWidth;
            if (surplus > bestSurplus) {
                bestSurplus = surplus;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private List<MesProBatchRecordParsedCell> normalizeChecklistNarrativeBandHeaderRow(
            List<MesProBatchRecordParsedCell> row, int columnCount, int renderWidth) {
        MesProBatchRecordParsedCell sideHeaderCell = cloneCell(row.get(0), 1.0f);
        sideHeaderCell.setColSpan(1);
        sideHeaderCell.setHorizontalAlign("center");
        String normalizedText = textOf(sideHeaderCell).replace("\n", "");
        if (!normalizedText.isBlank()) {
            sideHeaderCell.setText(vertical(normalizedText));
        }
        int availableColumns = Math.max(1, columnCount - 1);
        List<MesProBatchRecordParsedCell> normalizedRow = new ArrayList<>();
        normalizedRow.add(sideHeaderCell);
        if (row.size() == 1) {
            return normalizedRow;
        }
        List<MesProBatchRecordParsedCell> tailCells = row.subList(1, row.size());
        int[] tailRowSpans = tailCells.stream()
                .mapToInt(cell -> Math.max(1, cell.getRowSpan()))
                .toArray();
        int[] tailColSpans = distributeChecklistHeaderColSpans(tailCells, availableColumns);
        int normalizedWidth = Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX * availableColumns,
                Math.round(renderWidth * (availableColumns / (float) Math.max(columnCount, 1))));
        normalizedRow.addAll(cloneRowWithDistributedSpans(
                tailCells, 1.0f, normalizedWidth, availableColumns, tailColSpans, tailRowSpans));
        return tuneProcessRow(normalizedRow, resolveStructuredFontSize(row), resolveStructuredHeight(row), true);
    }

    private List<MesProBatchRecordParsedCell> normalizeChecklistNarrativeBandRow(
            List<MesProBatchRecordParsedCell> row, int availableColumns, int renderWidth, int columnCount) {
        int[] rowSpans = row.stream()
                .mapToInt(cell -> Math.max(1, cell.getRowSpan()))
                .toArray();
        int[] colSpans = distributeChecklistNarrativeColSpans(row, availableColumns);
        if (colSpans == null) {
            return row;
        }
        int normalizedWidth = Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX * availableColumns,
                Math.round(renderWidth * (availableColumns / (float) Math.max(columnCount, 1))));
        List<MesProBatchRecordParsedCell> normalizedRow = cloneRowWithDistributedSpans(
                row, 1.0f, normalizedWidth, availableColumns, colSpans, rowSpans);
        balanceChecklistNarrativeTrailingBlankSpans(normalizedRow, availableColumns);
        alignFirstNarrativeCellLeft(normalizedRow);
        return tuneProcessRow(normalizedRow, PROCESS_BODY_FONT_SIZE,
                resolveChecklistNarrativeRowHeight(row, normalizedRow), false);
    }

    private void balanceChecklistNarrativeTrailingBlankSpans(List<MesProBatchRecordParsedCell> row, int availableColumns) {
        if (availableColumns <= PROCESS_TEMPLATE_TOTAL_COL_SPAN * 2 || row == null || row.size() != 5) {
            return;
        }
        if (!isShortLabelText(textOf(row.get(0)))
                || !looksLikeParagraphText(textOf(row.get(1)))
                || !containsChecklistChoice(textOf(row.get(2)))
                || !isBlankCell(row.get(3))
                || !isBlankCell(row.get(4))) {
            return;
        }
        MesProBatchRecordParsedCell penultimateBlank = row.get(3);
        MesProBatchRecordParsedCell lastBlank = row.get(4);
        int penultimateSpan = Math.max(1, penultimateBlank.getColSpan());
        int lastSpan = Math.max(1, lastBlank.getColSpan());
        if (lastSpan <= penultimateSpan + 4) {
            return;
        }
        lastBlank.setColSpan(lastSpan - 1);
    }

    private void balanceChecklistNarrativeTrailingBlankSpans(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            balanceChecklistNarrativeTrailingBlankSpans(row, sumColSpans(row));
        }
    }

    private int resolveChecklistNarrativeRowHeight(List<MesProBatchRecordParsedCell> sourceRow,
                                                   List<MesProBatchRecordParsedCell> normalizedRow) {
        int sourceHeight = sourceRow == null
                ? 0
                : sourceRow.stream().mapToInt(MesProBatchRecordParsedCell::getHeightPx).max().orElse(0);
        int estimatedHeight = MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT;
        if (normalizedRow != null) {
            for (MesProBatchRecordParsedCell cell : normalizedRow) {
                int fontSize = MesProBatchRecordReportShapeRules.clampFontSize(cell.getFontSize(), cell.isBold());
                String visibleText = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
                estimatedHeight = Math.max(estimatedHeight,
                        MesProBatchRecordReportShapeRules.estimatePreservedRowHeight(
                                visibleText,
                                Math.max(cell.getWidthPx(), MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX),
                                fontSize));
            }
        }
        return MesProBatchRecordReportShapeRules.clampPreservedRowHeight(Math.max(sourceHeight, estimatedHeight));
    }

    private void alignCellLeft(MesProBatchRecordParsedCell cell) {
        if (cell != null) {
            cell.setHorizontalAlign("left");
        }
    }

    private int[] distributeChecklistNarrativeColSpans(List<MesProBatchRecordParsedCell> row, int totalColSpan) {
        if (!isChecklistNarrativeBandRow(row) || totalColSpan < row.size()) {
            return null;
        }
        int[] compactNarrativeTemplate = tryBuildCompactChecklistNarrativeTemplate(row, totalColSpan);
        if (compactNarrativeTemplate != null) {
            return compactNarrativeTemplate;
        }
        int[] fixedTailTemplate = tryBuildChecklistFixedTailTemplate(row, totalColSpan, CHECKLIST_BODY_FIXED_TAIL_COL_SPANS);
        if (fixedTailTemplate != null) {
            return fixedTailTemplate;
        }
        int[] colSpans = new int[row.size()];
        for (int index = 0; index < row.size(); index++) {
            colSpans[index] = 1;
        }
        colSpans[0] = 3;
        for (int index = 2; index < row.size(); index++) {
            String text = textOf(row.get(index));
            colSpans[index] = text.isBlank() || row.get(index).isVisualBlank() ? 2 : 3;
        }
        int fixedColSpan = 0;
        for (int index = 0; index < row.size(); index++) {
            if (index == 1) {
                continue;
            }
            fixedColSpan += colSpans[index];
        }
        if (fixedColSpan >= totalColSpan) {
            return distributeAdaptiveColSpans(row, totalColSpan);
        }
        colSpans[1] = totalColSpan - fixedColSpan;
        int narrativeMaxColSpan = Math.max(5, (int) Math.ceil(totalColSpan * 0.5));
        while (colSpans[1] > narrativeMaxColSpan) {
            int receiverIndex = findChecklistNarrativeSpanReceiver(row, colSpans, totalColSpan);
            if (receiverIndex < 0) {
                break;
            }
            colSpans[1]--;
            colSpans[receiverIndex]++;
        }
        return colSpans;
    }

    private int[] tryBuildCompactChecklistNarrativeTemplate(List<MesProBatchRecordParsedCell> row, int totalColSpan) {
        if (row == null || row.size() != 5 || totalColSpan < PROCESS_TEMPLATE_TOTAL_COL_SPAN - 1) {
            return null;
        }
        int sourceSpan = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
        if (sourceSpan <= PROCESS_TEMPLATE_TOTAL_COL_SPAN * 2) {
            return null;
        }
        if (!isShortLabelText(textOf(row.get(0)))
                || !looksLikeParagraphText(textOf(row.get(1)))
                || !containsChecklistChoice(textOf(row.get(2)))
                || !isBlankCell(row.get(3))
                || !isBlankCell(row.get(4))) {
            return null;
        }
        int[] colSpans = {3, 8, 3, 2, 3};
        int consumed = sum(colSpans);
        if (consumed != totalColSpan) {
            colSpans[1] = Math.max(5, colSpans[1] + totalColSpan - consumed);
        }
        return sum(colSpans) == totalColSpan ? colSpans : null;
    }

    private int findChecklistNarrativeSpanReceiver(List<MesProBatchRecordParsedCell> row,
                                                   int[] colSpans,
                                                   int totalColSpan) {
        for (int index = 2; index < row.size(); index++) {
            if (!textOf(row.get(index)).isBlank()
                    && colSpans[index] < resolveChecklistNarrativeTailSpanCap(row, index, totalColSpan)) {
                return index;
            }
        }
        if (colSpans[0] < 4) {
            return 0;
        }
        for (int index = row.size() - 1; index >= 2; index--) {
            if ((textOf(row.get(index)).isBlank() || row.get(index).isVisualBlank())
                    && colSpans[index] < resolveChecklistNarrativeTailSpanCap(row, index, totalColSpan)) {
                return index;
            }
        }
        return -1;
    }

    private int resolveChecklistNarrativeTailSpanCap(List<MesProBatchRecordParsedCell> row,
                                                     int index,
                                                     int totalColSpan) {
        boolean compactTailBand = row.size() <= 4;
        boolean blankTailCell = textOf(row.get(index)).isBlank() || row.get(index).isVisualBlank();
        if (compactTailBand) {
            return blankTailCell
                    ? Math.max(3, (int) Math.ceil(totalColSpan * 0.2))
                    : Math.max(4, (int) Math.ceil(totalColSpan * 0.18));
        }
        return blankTailCell ? 3 : 4;
    }

    private int[] distributeChecklistHeaderColSpans(List<MesProBatchRecordParsedCell> row, int totalColSpan) {
        int[] fixedTailTemplate = tryBuildChecklistFixedTailTemplate(row, totalColSpan, CHECKLIST_HEADER_FIXED_TAIL_COL_SPANS);
        if (fixedTailTemplate != null) {
            return fixedTailTemplate;
        }
        return distributeAdaptiveColSpans(row, totalColSpan);
    }

    private int[] tryBuildChecklistFixedTailTemplate(List<MesProBatchRecordParsedCell> row,
                                                     int totalColSpan,
                                                     int[] fixedTailColSpans) {
        if (row == null || row.size() < fixedTailColSpans.length + 1) {
            return null;
        }
        int[] colSpans = new int[row.size()];
        int tailStart = row.size() - fixedTailColSpans.length;
        int tailTotal = 0;
        for (int index = 0; index < fixedTailColSpans.length; index++) {
            int span = fixedTailColSpans[index];
            colSpans[tailStart + index] = span;
            tailTotal += span;
        }
        int headReserved = Math.max(3, Math.min(4, totalColSpan / 6));
        colSpans[0] = headReserved;
        int narrativeSpan = totalColSpan - headReserved - tailTotal;
        if (narrativeSpan < 4) {
            return null;
        }
        for (int index = 1; index < tailStart; index++) {
            colSpans[index] = index == 1 ? narrativeSpan : 1;
        }
        if (tailStart > 2) {
            int consumed = 0;
            for (int span : colSpans) {
                consumed += span;
            }
            if (consumed != totalColSpan) {
                colSpans[1] += totalColSpan - consumed;
            }
        }
        int sum = 0;
        for (int span : colSpans) {
            sum += span;
        }
        return sum == totalColSpan ? colSpans : null;
    }

    private void insertContinuationHeadersForLongOverviewSections(List<List<MesProBatchRecordParsedCell>> rows,
                                                                  int columnCount,
                                                                  int renderWidth,
                                                                  boolean sharedOverviewTemplate,
                                                                  List<List<MesProBatchRecordParsedCell>> documentHeaderRows) {
        if (!sharedOverviewTemplate || countDocumentHeaderGroups(rows) > 1) {
            return;
        }
        if (documentHeaderRows == null || documentHeaderRows.isEmpty()) {
            return;
        }
        List<Integer> continuationIndexes = resolveOverviewSectionContinuationIndexes(rows);
        for (int index = continuationIndexes.size() - 1; index >= 0; index--) {
            rows.addAll(continuationIndexes.get(index), cloneDocumentFrameRows(documentHeaderRows));
        }
    }

    private List<Integer> resolveOverviewSectionContinuationIndexes(List<List<MesProBatchRecordParsedCell>> rows) {
        List<Integer> continuationIndexes = new ArrayList<>();
        int lastContinuationIndex = 0;
        for (int index = 0; index < rows.size(); index++) {
            if (!isOverviewSectionTitleRow(rows.get(index))) {
                continue;
            }
            if (index - lastContinuationIndex >= OVERVIEW_SECTION_CONTINUATION_MIN_GAP) {
                continuationIndexes.add(index);
                lastContinuationIndex = index;
            }
        }
        return continuationIndexes;
    }

    private boolean isOverviewSectionTitleRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty() || countNonEmptyCells(row) != 1) {
            return false;
        }
        String text = textOf(firstMeaningfulCell(row));
        return text.endsWith("信息") && text.length() <= 16;
    }

    private List<MesProBatchRecordParsedCell> normalizeProcessTemplateRow(List<List<MesProBatchRecordParsedCell>> originalRows,
                                                                           int rowIndex,
                                                                           List<MesProBatchRecordParsedCell> originalRow,
                                                                           int columnCount,
                                                                           int renderWidth,
                                                                           float scale,
                                                                           Map<String, Integer> rowSignatureCounts,
                                                                           boolean preserveSourceGrid,
                                                                           List<Integer> sourceColumnWidths,
                                                                           Map<Integer, Integer> blockedUntilRowByColumn) {
        if (originalRow == null || originalRow.isEmpty()) {
            return null;
        }
        boolean sourceWidthMappedRow = preserveSourceGrid || isSourceIndexedSparseOverviewRow(originalRow, columnCount);
        List<MesProBatchRecordParsedCell> sourceMappedRow = sourceWidthMappedRow
                ? cloneRowWithSourceWidthSpans(originalRow, scale, renderWidth, columnCount,
                sourceColumnWidths, rowIndex, blockedUntilRowByColumn)
                : null;
        if (isChecklistOutcomeAlignmentHeaderRow(originalRow)
                && originalRow.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum() > columnCount) {
            int bodyEndIndex = resolveChecklistOutcomeBodyEndIndex(originalRows, rowIndex + 1);
            return normalizeChecklistOutcomeAlignmentHeaderRow(
                    originalRow, columnCount, renderWidth, Math.max(0, bodyEndIndex - rowIndex));
        }
        if (isExplicitMaterialMatrixHeaderRow(originalRow)) {
            if (sourceMappedRow != null) {
                return tuneProcessRowKeepingHeight(sourceMappedRow, resolveStructuredFontSize(originalRow), true);
            }
            return normalizeExplicitMaterialMatrixHeaderRow(originalRows, rowIndex, columnCount, renderWidth);
        }
        if (isStandaloneMaterialMatrixHeaderRow(originalRow)) {
            return normalizeStandaloneMaterialMatrixHeaderRow(originalRow, columnCount, renderWidth);
        }
        MesProBatchRecordSharedRowTypeRules.RowType rowType =
                MesProBatchRecordSharedRowTypeRules.classifyRow(originalRows, rowIndex, rowSignatureCounts);
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.TITLE) {
            MesProBatchRecordParsedCell titleCell = cloneCell(firstMeaningfulCell(originalRow), scale);
            titleCell.setColSpan(columnCount);
            titleCell.setWidthPx(renderWidth);
            titleCell.setHorizontalAlign("center");
            titleCell.setBold(true);
            titleCell.setFontSize(10);
            titleCell.setHeightPx(sourceMappedRow != null ? resolveOriginalRowHeight(originalRow) : 30);
            return List.of(titleCell);
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.FOOTER) {
            MesProBatchRecordParsedCell footerCell = cloneCell(firstMeaningfulCell(originalRow), scale);
            footerCell.setColSpan(columnCount);
            footerCell.setWidthPx(renderWidth);
            footerCell.setHorizontalAlign("left");
            footerCell.setFontSize(8);
            footerCell.setHeightPx(20);
            footerCell.setBorderless(true);
            return List.of(footerCell);
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION
                && countNonEmptyCells(originalRow) == 1) {
            MesProBatchRecordParsedCell noteCell = cloneCell(firstMeaningfulCell(originalRow), scale);
            noteCell.setColSpan(columnCount);
            noteCell.setWidthPx(renderWidth);
            noteCell.setHorizontalAlign("left");
            noteCell.setFontSize(PROCESS_BODY_FONT_SIZE);
            noteCell.setHeightPx(sourceMappedRow != null
                    ? resolveOriginalRowHeight(originalRow)
                    : resolveNarrativeHeight(originalRow));
            return List.of(noteCell);
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION) {
            if (sourceMappedRow != null) {
                alignFirstNarrativeCellLeft(sourceMappedRow);
                return tuneProcessRowKeepingHeight(sourceMappedRow, PROCESS_BODY_FONT_SIZE, false);
            }
            List<MesProBatchRecordParsedCell> mixedRow =
                    cloneRowWithAdaptiveSpans(originalRow, scale, renderWidth, columnCount);
            alignFirstNarrativeCellLeft(mixedRow);
            return tuneProcessRow(mixedRow, PROCESS_BODY_FONT_SIZE, 36, false);
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.FIELD) {
            if (sourceMappedRow != null) {
                sourceMappedRow = normalizeSparseOverviewLabelValueSymmetry(sourceMappedRow, columnCount, renderWidth);
                return tuneProcessRowKeepingHeight(sourceMappedRow, PROCESS_BODY_FONT_SIZE, false);
            }
            List<MesProBatchRecordParsedCell> metadataRow =
                    tryNormalizeMetadataLabelValuePairs(originalRow, scale, renderWidth, columnCount);
            if (metadataRow != null) {
                return tuneProcessRow(metadataRow, PROCESS_BODY_FONT_SIZE, 26, false);
            }
            return tuneProcessRow(cloneRowWithAdaptiveSpans(originalRow, scale, renderWidth, columnCount),
                    PROCESS_BODY_FONT_SIZE, 26, false);
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY) {
            if (sourceMappedRow != null) {
                return tuneProcessRowKeepingHeight(sourceMappedRow,
                        resolveStructuredFontSize(originalRow), resolveStructuredBold(originalRow));
            }
            List<MesProBatchRecordParsedCell> verticalSideHeaderRow =
                    tryNormalizeLeadingOperationSideHeaderRow(originalRow, scale, renderWidth, columnCount);
            if (verticalSideHeaderRow != null) {
                return tuneProcessRow(verticalSideHeaderRow,
                        resolveStructuredFontSize(originalRow), resolveStructuredHeight(originalRow),
                        resolveStructuredBold(originalRow));
            }
            return tuneProcessRow(cloneRowWithAdaptiveSpans(originalRow, scale, renderWidth, columnCount),
                    resolveStructuredFontSize(originalRow), resolveStructuredHeight(originalRow), resolveStructuredBold(originalRow));
        }
        return null;
    }

    private List<MesProBatchRecordParsedCell> normalizeSparseOverviewLabelValueSymmetry(
            List<MesProBatchRecordParsedCell> row, int columnCount, int renderWidth) {
        if (row == null || row.size() != 4 || columnCount <= 0 || columnCount % 2 != 0
                || !isShortLabelText(textOf(row.get(0)))
                || !isShortLabelText(textOf(row.get(2)))
                || looksLikeParagraphText(textOf(row.get(1)))
                || looksLikeParagraphText(textOf(row.get(3)))) {
            return row;
        }
        int leftLabelSpan = Math.max(1, row.get(0).getColSpan());
        int rightLabelSpan = Math.max(1, row.get(2).getColSpan());
        if (leftLabelSpan == rightLabelSpan) {
            return row;
        }
        int halfColumns = columnCount / 2;
        int labelSpan = Math.min(Math.min(leftLabelSpan, rightLabelSpan), Math.max(1, halfColumns - 1));
        int[] colSpans = {labelSpan, halfColumns - labelSpan, labelSpan, halfColumns - labelSpan};
        int[] rowSpans = row.stream()
                .mapToInt(cell -> Math.max(1, cell.getRowSpan()))
                .toArray();
        return cloneRowWithDistributedSpans(row, 1.0f, renderWidth, columnCount, colSpans, rowSpans);
    }

    private List<MesProBatchRecordParsedCell> tryNormalizeLeadingOperationSideHeaderRow(
            List<MesProBatchRecordParsedCell> originalRow, float scale, int renderWidth, int columnCount) {
        if (originalRow == null || originalRow.size() < 2 || columnCount < 3) {
            return null;
        }
        MesProBatchRecordParsedCell sideHeader = originalRow.get(0);
        if (sideHeader == null || Math.max(1, sideHeader.getRowSpan()) < 2) {
            return null;
        }
        if (!looksLikeOperationSideHeaderText(textOf(sideHeader))) {
            return null;
        }

        List<MesProBatchRecordParsedCell> normalized = new ArrayList<>();
        MesProBatchRecordParsedCell normalizedSideHeader = cloneCell(sideHeader, scale);
        normalizedSideHeader.setColSpan(1);
        normalizedSideHeader.setWidthPx(Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX,
                Math.round(renderWidth / (float) Math.max(columnCount, 1))));
        normalized.add(normalizedSideHeader);

        List<MesProBatchRecordParsedCell> remainingCells = new ArrayList<>(originalRow.subList(1, originalRow.size()));
        int availableColumns = Math.max(remainingCells.size(), columnCount - 1);
        int[] colSpans = distributeAdaptiveColSpans(remainingCells, availableColumns);
        normalized.addAll(cloneRowWithDistributedSpans(remainingCells, scale,
                Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX * availableColumns,
                        renderWidth - normalizedSideHeader.getWidthPx()),
                availableColumns, colSpans, null));
        return normalized;
    }

    private boolean looksLikeOperationSideHeaderText(String text) {
        String normalized = normalizeStructureToken(text).replace(" ", "");
        return normalized.length() <= 24
                && normalized.contains("操作")
                && normalized.contains("自检")
                && normalized.endsWith("记录");
    }

    private boolean isMultiSegmentProcessPage(List<List<MesProBatchRecordParsedCell>> rows,
                                              Map<String, Integer> rowSignatureCounts) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        boolean hasTitle = false;
        boolean hasMetadataOrNarrative = false;
        boolean hasStructuredSection = false;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            MesProBatchRecordSharedRowTypeRules.RowType rowType =
                    MesProBatchRecordSharedRowTypeRules.classifyRow(rows, rowIndex, rowSignatureCounts);
            if (!hasTitle && rowType == MesProBatchRecordSharedRowTypeRules.RowType.TITLE) {
                hasTitle = true;
            }
            if (!hasMetadataOrNarrative && (rowType == MesProBatchRecordSharedRowTypeRules.RowType.FIELD
                    || rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION)) {
                hasMetadataOrNarrative = true;
            }
            if (!hasStructuredSection && (rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER
                    || rowType == MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA
                    || rowType == MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY)) {
                hasStructuredSection = true;
            }
        }
        return hasTitle && hasStructuredSection && hasMetadataOrNarrative;
    }

    private Map<String, Integer> countRowSignatures(List<List<MesProBatchRecordParsedCell>> rows) {
        Map<String, Integer> counts = new HashMap<>();
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            counts.merge(rowStructureKey(row), 1, Integer::sum);
        }
        return counts;
    }

    private boolean shouldCacheRepeatedRowTemplate(List<List<MesProBatchRecordParsedCell>> rows,
                                                   int rowIndex,
                                                   Map<String, Integer> rowSignatureCounts) {
        if (rows == null || rowIndex < 0 || rowIndex >= rows.size()) {
            return false;
        }
        List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
        if (row == null || row.isEmpty()) {
            return false;
        }
        MesProBatchRecordSharedRowTypeRules.RowType rowType =
                MesProBatchRecordSharedRowTypeRules.classifyRow(rows, rowIndex, rowSignatureCounts);
        return MesProBatchRecordSharedRowTypeRules.isStructuredTemplateRow(rowType)
                && rowSignatureCounts.getOrDefault(rowStructureKey(row), 0) >= 2;
    }

    private boolean isStructuredProcessRow(List<MesProBatchRecordParsedCell> row) {
        return isMetadataRow(row) || isStructuredHeaderRow(row) || isValueHeaderRow(row);
    }

    private boolean isRepeatedStructuredRow(List<MesProBatchRecordParsedCell> row,
                                            Map<String, Integer> rowSignatureCounts) {
        return isStructuredProcessRow(row) && rowSignatureCounts.getOrDefault(rowStructureKey(row), 0) >= 2;
    }

    private boolean isTitleLikeRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex,
                                   List<MesProBatchRecordParsedCell> row) {
        return isFirstMeaningfulRow(rows, rowIndex)
                && countNonEmptyCells(row) == 1
                && normalizedTextLength(firstMeaningfulText(row)) >= 4;
    }

    private boolean isFooterDateRow(List<MesProBatchRecordParsedCell> row) {
        return isDocumentFooterLayoutRow(row);
    }

    private boolean isNarrativeRow(List<MesProBatchRecordParsedCell> row) {
        if (countNonEmptyCells(row) != 1) {
            return false;
        }
        String text = firstMeaningfulText(row);
        return text.startsWith("备注") || text.startsWith("说明") || text.startsWith("补充说明")
                || normalizedTextLength(text) >= 18;
    }

    private boolean isMetadataRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (nonEmptyCells < 3 || nonEmptyCells > 8) {
            return false;
        }
        if (countLongTextCells(row) > 0) {
            return false;
        }
        return countShortLabelCells(row) >= 2 && countValueLikeCells(row) <= 1 && countBlankCells(row) >= 1;
    }

    private List<MesProBatchRecordParsedCell> tryNormalizeMetadataLabelValuePairs(
            List<MesProBatchRecordParsedCell> originalRow, float scale, int renderWidth, int columnCount) {
        if (!isMetadataLabelValuePairsRow(originalRow, columnCount)) {
            return null;
        }
        int pairCount = originalRow.size() / 2;
        int[] colSpans = new int[originalRow.size()];
        for (int pairIndex = 0; pairIndex < pairCount; pairIndex++) {
            int baseIndex = pairIndex * 2;
            colSpans[baseIndex] = METADATA_LABEL_VALUE_PAIR_SPANS[0];
            colSpans[baseIndex + 1] = METADATA_LABEL_VALUE_PAIR_SPANS[1];
        }
        return cloneRowWithDistributedSpans(originalRow, scale, renderWidth, columnCount, colSpans, null);
    }

    private boolean isMetadataLabelValuePairsRow(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (row == null || row.isEmpty() || row.size() % 2 != 0) {
            return false;
        }
        int pairCount = row.size() / 2;
        if (pairCount < 2) {
            return false;
        }
        int expectedTotalSpan = pairCount * (METADATA_LABEL_VALUE_PAIR_SPANS[0] + METADATA_LABEL_VALUE_PAIR_SPANS[1]);
        if (expectedTotalSpan != columnCount) {
            return false;
        }
        for (int pairIndex = 0; pairIndex < pairCount; pairIndex++) {
            String labelText = textOf(row.get(pairIndex * 2));
            String valueText = textOf(row.get(pairIndex * 2 + 1));
            if (!isShortLabelText(labelText)) {
                return false;
            }
            if (looksLikeParagraphText(valueText)) {
                return false;
            }
        }
        return true;
    }

    private boolean isMixedNarrativeStructuredRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (row == null || row.size() < 4 || nonEmptyCells < 2 || nonEmptyCells > 6) {
            return false;
        }
        return countLongTextCells(row) == 1
                && countShortLabelCells(row) >= 1
                && countValueLikeCells(row) <= Math.max(2, nonEmptyCells / 2);
    }

    private boolean isStructuredHeaderRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (nonEmptyCells < 3 || nonEmptyCells > 16) {
            return false;
        }
        if (countLongTextCells(row) > 0) {
            return false;
        }
        return countShortLabelCells(row) >= Math.max(3, (int) Math.ceil(nonEmptyCells * 0.6))
                && countValueLikeCells(row) <= 1;
    }

    private boolean isValueHeaderRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (nonEmptyCells < 4) {
            return false;
        }
        if (countLongTextCells(row) > 0 || countValueLikeCells(row) > 1) {
            return false;
        }
        int distinctTokens = (int) row.stream()
                .map(MesProBatchRecordParsedCell::getText)
                .map(this::normalizeStructureToken)
                .filter(text -> text != null && !text.isBlank())
                .distinct()
                .count();
        return distinctTokens <= Math.max(2, nonEmptyCells / 2);
    }

    private boolean isFirstMeaningfulRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        for (int index = 0; index < rowIndex; index++) {
            if (countNonEmptyCells(rows.get(index)) > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean appearsBeforeFirstRepeatedSection(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex,
                                                      Map<String, Integer> rowSignatureCounts) {
        int firstRepeatedSectionIndex = findFirstRepeatedStructuredIndex(rows, rowSignatureCounts);
        return firstRepeatedSectionIndex < 0 || rowIndex < firstRepeatedSectionIndex;
    }

    private boolean appearsBeforeFirstDataBlock(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex,
                                                Map<String, Integer> rowSignatureCounts) {
        int firstDataIndex = findFirstDataBlockIndex(rows, rowSignatureCounts);
        return firstDataIndex < 0 || rowIndex < firstDataIndex;
    }

    private int findFirstRepeatedStructuredIndex(List<List<MesProBatchRecordParsedCell>> rows,
                                                 Map<String, Integer> rowSignatureCounts) {
        for (int index = 0; index < rows.size(); index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            if (isRepeatedStructuredRow(row, rowSignatureCounts)) {
                return index;
            }
        }
        return -1;
    }

    private int findFirstDataBlockIndex(List<List<MesProBatchRecordParsedCell>> rows,
                                        Map<String, Integer> rowSignatureCounts) {
        for (int index = 0; index < rows.size(); index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            if (row == null || row.isEmpty()) {
                continue;
            }
            if (!isStructuredProcessRow(row)
                    && countValueLikeCells(row) >= Math.max(2, countNonEmptyCells(row) / 2)
                    && !isNarrativeRow(row)) {
                return index;
            }
        }
        return -1;
    }

    private int resolveStructuredFontSize(List<MesProBatchRecordParsedCell> row) {
        return MesProBatchRecordSharedRowTypeRules.isCompactTableHeaderRow(row)
                ? PROCESS_BODY_FONT_SIZE
                : PROCESS_HEADER_FONT_SIZE;
    }

    private int resolveStructuredHeight(List<MesProBatchRecordParsedCell> row) {
        return MesProBatchRecordSharedRowTypeRules.isCompactTableHeaderRow(row) ? 24 : 28;
    }

    private boolean resolveStructuredBold(List<MesProBatchRecordParsedCell> row) {
        return !MesProBatchRecordSharedRowTypeRules.isCompactTableHeaderRow(row);
    }

    private int resolveNarrativeHeight(List<MesProBatchRecordParsedCell> row) {
        return Math.max(24, Math.min(36, 18 + countNonEmptyCells(row) * 2));
    }

    private int resolveOriginalRowHeight(List<MesProBatchRecordParsedCell> row) {
        int rowHeight = 0;
        if (row != null) {
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell != null) {
                    rowHeight = Math.max(rowHeight, cell.getHeightPx());
                }
            }
        }
        return MesProBatchRecordReportShapeRules.clampPreservedRowHeight(rowHeight);
    }

    private MesProBatchRecordParsedCell firstMeaningfulCell(List<MesProBatchRecordParsedCell> row) {
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && cell.getText() != null && !cell.getText().isBlank()) {
                return cell;
            }
        }
        return row.get(0);
    }

    private String firstMeaningfulText(List<MesProBatchRecordParsedCell> row) {
        MesProBatchRecordParsedCell cell = firstMeaningfulCell(row);
        return cell == null ? "" : textOf(cell);
    }

    private List<MesProBatchRecordParsedCell> cloneRowWithAdaptiveSpans(List<MesProBatchRecordParsedCell> originalRow,
                                                                        float scale,
                                                                        int renderWidth,
                                                                        int totalColSpan) {
        int safeTotalColSpan = Math.max(totalColSpan, originalRow.size());
        int[] colSpans = distributeAdaptiveColSpans(originalRow, safeTotalColSpan);
        return cloneRowWithDistributedSpans(originalRow, scale, renderWidth, safeTotalColSpan, colSpans, null);
    }

    private List<MesProBatchRecordParsedCell> cloneRowWithSourceWidthSpans(List<MesProBatchRecordParsedCell> originalRow,
                                                                          float scale,
                                                                          int renderWidth,
                                                                          int columnCount,
                                                                          List<Integer> sourceColumnWidths,
                                                                          int rowIndex,
                                                                          Map<Integer, Integer> blockedUntilRowByColumn) {
        if (originalRow == null || originalRow.isEmpty()
                || !hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)) {
            return null;
        }
        int[] colSpans = resolveSourceWidthColSpans(
                originalRow, sourceColumnWidths, columnCount, rowIndex, blockedUntilRowByColumn);
        if (colSpans == null) {
            return null;
        }
        int[] rowSpans = originalRow.stream()
                .mapToInt(cell -> Math.max(1, cell.getRowSpan()))
                .toArray();
        return cloneRowWithDistributedSpans(originalRow, scale, renderWidth, columnCount, colSpans, rowSpans);
    }

    private int[] resolveSourceWidthColSpans(List<MesProBatchRecordParsedCell> row,
                                             List<Integer> sourceColumnWidths,
                                             int startColumn,
                                             int totalColumns) {
        if (row == null || sourceColumnWidths == null || startColumn < 0
                || startColumn >= sourceColumnWidths.size()) {
            return null;
        }
        int[] colSpans = new int[row.size()];
        int cursor = startColumn;
        int maxColumn = Math.min(sourceColumnWidths.size(), startColumn + Math.max(1, totalColumns));
        for (int index = 0; index < row.size(); index++) {
            MesProBatchRecordParsedCell cell = row.get(index);
            if (cursor >= maxColumn) {
                return null;
            }
            int remainingCells = row.size() - index;
            int remainingColumns = maxColumn - cursor;
            if (remainingColumns < remainingCells) {
                return null;
            }
            int span = resolveWidthMappedSpan(cell, sourceColumnWidths, cursor, maxColumn, remainingCells);
            colSpans[index] = Math.max(1, span);
            cursor += colSpans[index];
        }
        return cursor <= maxColumn ? colSpans : null;
    }

    private int[] resolveSourceWidthColSpans(List<MesProBatchRecordParsedCell> row,
                                             List<Integer> sourceColumnWidths,
                                             int columnCount,
                                             int rowIndex,
                                             Map<Integer, Integer> blockedUntilRowByColumn) {
        if (row == null || sourceColumnWidths == null || columnCount <= 0) {
            return null;
        }
        int[] colSpans = new int[row.size()];
        int cursor = 0;
        int maxColumn = Math.min(sourceColumnWidths.size(), columnCount);
        for (int index = 0; index < row.size(); index++) {
            while (blockedUntilRowByColumn != null
                    && blockedUntilRowByColumn.getOrDefault(cursor, -1) >= rowIndex) {
                cursor++;
            }
            if (cursor >= maxColumn) {
                return null;
            }
            int remainingCells = row.size() - index;
            if (countAvailableColumns(cursor, maxColumn, rowIndex, blockedUntilRowByColumn) < remainingCells) {
                return null;
            }
            MesProBatchRecordParsedCell cell = row.get(index);
            int span = resolveWidthMappedSpan(cell, sourceColumnWidths, cursor, maxColumn, remainingCells);
            colSpans[index] = Math.max(1, span);
            cursor += colSpans[index];
        }
        return colSpans;
    }

    private int countAvailableColumns(int startColumn,
                                      int maxColumn,
                                      int rowIndex,
                                      Map<Integer, Integer> blockedUntilRowByColumn) {
        int count = 0;
        for (int columnIndex = startColumn; columnIndex < maxColumn; columnIndex++) {
            if (blockedUntilRowByColumn != null
                    && blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                continue;
            }
            count++;
        }
        return count;
    }

    private int resolveWidthMappedSpan(MesProBatchRecordParsedCell cell,
                                       List<Integer> sourceColumnWidths,
                                       int startColumn,
                                       int maxColumn,
                                       int remainingCells) {
        int sourceWidth = Math.max(1, cell == null ? 0 : cell.getWidthPx());
        int totalSourceWidth = sourceColumnWidths.stream().mapToInt(width -> Math.max(1, width)).sum();
        if (remainingCells == 1 && sourceWidth >= totalSourceWidth * 0.92f) {
            return Math.max(1, maxColumn - startColumn);
        }
        if (sourceWidth <= sourceColumnWidths.get(startColumn) * 1.25f) {
            return 1;
        }
        int accumulatedWidth = 0;
        int maxSpan = Math.max(1, maxColumn - startColumn - Math.max(0, remainingCells - 1));
        for (int span = 1; span <= maxSpan; span++) {
            accumulatedWidth += Math.max(1, sourceColumnWidths.get(startColumn + span - 1));
            if (accumulatedWidth >= sourceWidth * 0.85f) {
                return span;
            }
        }
        return maxSpan;
    }

    private int[] distributeAdaptiveColSpans(List<MesProBatchRecordParsedCell> originalRow, int totalColSpan) {
        int size = originalRow.size();
        int[] colSpans = new int[size];
        int totalWeight = 0;
        int[] weights = new int[size];
        for (int index = 0; index < size; index++) {
            weights[index] = estimateCellWeight(originalRow, index, totalColSpan);
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


    private int estimateCellWeight(List<MesProBatchRecordParsedCell> row, int index, int totalColSpan) {
        MesProBatchRecordParsedCell cell = row.get(index);
        String text = normalizeStructureToken(cell == null ? null : cell.getText());
        if (text.isBlank()) {
            return 1;
        }
        int widthFloor = MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(text, index, totalColSpan);
        if (looksLikeParagraphText(text)) {
            int base = Math.max(6, Math.min(14, text.length() / 5 + 2));
            if (index == 0 || index == 1) {
                base += 2;
            }
            return Math.min(16, base);
        }
        int weight = Math.max(1, Math.min(6, text.length() / 3 + 1));
        if (widthFloor >= MesProBatchRecordReportShapeRules.DENSE_TAIL_UNIT_COLUMN_WIDTH_FLOOR_PX) {
            weight += 3;
        } else if (widthFloor >= MesProBatchRecordReportShapeRules.DENSE_TAIL_COLUMN_WIDTH_FLOOR_PX) {
            weight += 2;
        } else if (widthFloor > 0) {
            weight += 1;
        }
        return Math.min(8, weight);
    }

    private void alignFirstNarrativeCellLeft(List<MesProBatchRecordParsedCell> row) {
        if (row == null) {
            return;
        }
        for (MesProBatchRecordParsedCell cell : row) {
            if (looksLikeParagraphText(textOf(cell))) {
                cell.setHorizontalAlign("left");
                return;
            }
        }
    }

    private int countNonEmptyCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && cell.getText() != null && !cell.getText().isBlank()) {
                count++;
            }
        }
        return count;
    }

    private int countBlankCells(List<MesProBatchRecordParsedCell> row) {
        return Math.max(0, row.size() - countNonEmptyCells(row));
    }

    private int countNonBlankCells(List<MesProBatchRecordParsedCell> row) {
        return countNonEmptyCells(row);
    }

    private int countShortLabelCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (isShortLabelText(textOf(cell))) {
                count++;
            }
        }
        return count;
    }

    private int countLongTextCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (looksLikeParagraphText(textOf(cell))) {
                count++;
            }
        }
        return count;
    }

    private int countValueLikeCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (isValueLikeText(textOf(cell))) {
                count++;
            }
        }
        return count;
    }

    private boolean isShortLabelText(String text) {
        String normalized = normalizeStructureToken(text);
        return !normalized.isBlank() && normalized.length() <= 12 && !looksLikeParagraphText(normalized);
    }

    private boolean looksLikeParagraphText(String text) {
        String normalized = normalizeStructureToken(text);
        return normalized.length() >= 18 || normalized.contains("\n") || normalized.contains("。")
                || normalized.contains("，") || normalized.contains("；");
    }

    private boolean isValueLikeText(String text) {
        String normalized = normalizeStructureToken(text);
        return !normalized.isBlank()
                && (normalized.matches("(?i)^[A-Z0-9./%:-]+$")
                || normalized.matches("^\\d{4}-\\d{2}-\\d{2}.*$")
                || normalized.matches("^\\d{1,2}:\\d{2}.*$")
                || normalized.matches(".*\\d{1,2}%$")
                || normalized.matches(".*\\d+min$"));
    }

    private boolean isRepeatedHeaderCellPattern(List<MesProBatchRecordParsedCell> cells) {
        if (cells == null || cells.size() != PACKED_MATERIAL_MATRIX_HEADER_COUNT) {
            return false;
        }
        List<String> texts = cells.stream()
                .map(this::textOf)
                .toList();
        return isRepeatedHeaderTextPattern(texts);
    }

    private boolean isRepeatedHeaderTextPattern(List<String> texts) {
        if (texts == null || texts.size() != PACKED_MATERIAL_MATRIX_HEADER_COUNT) {
            return false;
        }
        int half = PACKED_MATERIAL_MATRIX_HEADER_COUNT / 2;
        for (int index = 0; index < half; index++) {
            String left = normalizeStructureToken(texts.get(index));
            String right = normalizeStructureToken(texts.get(index + half));
            if (left.isBlank() || !left.equals(right) || !isShortLabelText(left)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRepeatedOperationDetailRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        return hasSourceColumnGap(row)
                && row.size() >= 3
                && countShortLabelCells(row) >= 2
                && countBlankCells(row) >= 1
                && sumColSpans(row) >= 10
                && countLongTextCells(row) == 0;
    }

    private boolean isSectionBoundaryRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        int nonEmptyCells = countNonEmptyCells(row);
        return (nonEmptyCells == 1
                && countSlashCells(row) == 0
                && normalizedTextLength(firstMeaningfulText(row)) >= 4)
                || isPostRepeatedOperationNarrativeBoundaryRow(row, nonEmptyCells);
    }

    private boolean isPostRepeatedOperationNarrativeBoundaryRow(List<MesProBatchRecordParsedCell> row,
                                                                int nonEmptyCells) {
        if (nonEmptyCells != 2 || countSlashCells(row) > 0 || sumColSpans(row) < 10) {
            return false;
        }
        boolean hasShortSectionLabel = false;
        boolean hasWideNarrative = false;
        int cursor = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            String text = textOf(cell);
            int colSpan = Math.max(1, cell.getColSpan());
            if (!text.isBlank()) {
                if (cursor <= 3 && isShortLabelText(text) && normalizedTextLength(text) >= 4) {
                    hasShortSectionLabel = true;
                }
                if (colSpan >= 6 && looksLikeParagraphText(text)) {
                    hasWideNarrative = true;
                }
            }
            cursor += colSpan;
        }
        return hasShortSectionLabel && hasWideNarrative;
    }

    private String rowStructureKey(List<MesProBatchRecordParsedCell> row) {
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row) {
            String token = normalizeStructureToken(textOf(cell));
            if (token.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(token);
        }
        if (builder.isEmpty()) {
            return "empty:" + row.size();
        }
        return builder.toString();
    }

    private String normalizeStructureToken(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', ' ')
                .replace('\n', ' ')
                .replace("  ", " ")
                .trim();
    }

    private String compactStructureToken(String text) {
        return normalizeStructureToken(text).replace(" ", "");
    }

    private int normalizedTextLength(String text) {
        return normalizeStructureToken(text).length();
    }

    private List<MesProBatchRecordParsedCell> applyRowShapeTemplate(List<MesProBatchRecordParsedCell> sourceRow,
                                                                    float scale,
                                                                    int renderWidth,
                                                                    int columnCount,
                                                                    RowShapeTemplate template) {
        if (sourceRow.size() != template.colSpans.size()) {
            List<MesProBatchRecordParsedCell> adaptiveRow = cloneRowWithAdaptiveSpans(
                    sourceRow, scale, renderWidth, Math.max(columnCount, sourceRow.size()));
            return tuneProcessRow(adaptiveRow, template.primaryFontSize(), template.primaryHeight(), template.isBoldRow());
        }
        List<MesProBatchRecordParsedCell> row = new ArrayList<>(sourceRow.size());
        for (int index = 0; index < sourceRow.size(); index++) {
            MesProBatchRecordParsedCell cloned = cloneCell(sourceRow.get(index), scale);
            cloned.setColumnIndex(null);
            cloned.setColSpan(template.colSpans.get(index));
            cloned.setRowSpan(template.rowSpans.get(index));
            cloned.setWidthPx(template.widths.get(index));
            cloned.setHeightPx(template.heights.get(index));
            cloned.setBold(template.bolds.get(index));
            cloned.setFontSize(template.fontSizes.get(index));
            cloned.setHorizontalAlign(template.horizontalAligns.get(index));
            row.add(cloned);
        }
        return row;
    }

    private List<MesProBatchRecordParsedCell> cloneRowWithDistributedSpans(List<MesProBatchRecordParsedCell> originalRow,
                                                                            float scale,
                                                                            int renderWidth,
                                                                            int totalColSpan,
                                                                            int[] colSpans,
                                                                            int[] rowSpans) {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        int totalSourceWidth = originalRow.stream()
                .mapToInt(cell -> Math.max(cell == null ? 0 : cell.getWidthPx(),
                        MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX))
                .sum();
        for (int index = 0; index < originalRow.size(); index++) {
            MesProBatchRecordParsedCell cloned = cloneCell(originalRow.get(index), scale);
            cloned.setColumnIndex(null);
            cloned.setColSpan(colSpans[index]);
            int sourceWidth = Math.max(originalRow.get(index) == null ? 0 : originalRow.get(index).getWidthPx(),
                    MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
            int proportionalWidth = totalSourceWidth <= 0
                    ? Math.round(renderWidth * (colSpans[index] / (float) totalColSpan))
                    : Math.round(renderWidth * (sourceWidth / (float) totalSourceWidth));
            cloned.setWidthPx(Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX,
                    proportionalWidth));
            if (rowSpans != null) {
                cloned.setRowSpan(rowSpans[index]);
            }
            row.add(cloned);
        }
        return row;
    }

    private List<MesProBatchRecordParsedCell> tuneProcessRow(List<MesProBatchRecordParsedCell> row,
                                                             int fontSize,
                                                             int heightPx,
                                                             boolean bold) {
        for (MesProBatchRecordParsedCell cell : row) {
            cell.setFontSize(fontSize);
            cell.setHeightPx(heightPx);
            if (bold) {
                cell.setBold(true);
            }
        }
        return row;
    }

    private List<MesProBatchRecordParsedCell> tuneProcessRowKeepingHeight(List<MesProBatchRecordParsedCell> row,
                                                                         int fontSize,
                                                                         boolean bold) {
        for (MesProBatchRecordParsedCell cell : row) {
            cell.setFontSize(fontSize);
            if (bold) {
                cell.setBold(true);
            }
        }
        return row;
    }

    private boolean shouldExpandToFullWidth(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell,
                                            int columnCount, int tableWidth) {
        return row.size() == 1
                && columnCount > 1
                && cell.getColSpan() == 1
                && cell.getWidthPx() >= Math.round(tableWidth * 0.85f);
    }

    private int measureTableWidth(List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        int width = columnCount * MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            int rowWidth = row.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum();
            width = Math.max(width, rowWidth);
        }
        return width;
    }

    private int resolveRenderWidth(int sourceWidth,
                                   int measuredSourceWidth,
                                   int columnCount,
                                   boolean processTemplate,
                                   boolean preserveSourceGrid,
                                   List<List<MesProBatchRecordParsedCell>> rows) {
        int minRequiredWidth = columnCount * MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX;
        int targetWidth = MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(columnCount);
        int maxWidthBudget = targetWidth;
        int candidate = processTemplate ? targetWidth : Math.min(sourceWidth, targetWidth);
        if (preserveSourceGrid) {
            int sourceGridWidth = Math.max(minRequiredWidth, measuredSourceWidth);
            if (processTemplate && shouldUseLandscapeProcessBudget(columnCount, measuredSourceWidth, rows)) {
                return Math.max(sourceGridWidth, MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_LANDSCAPE_BUDGET_PX);
            }
            return sourceGridWidth;
        }
        if (processTemplate && shouldUseLandscapeProcessBudget(columnCount, measuredSourceWidth, rows)) {
            candidate = Math.max(candidate, MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_LANDSCAPE_BUDGET_PX);
            maxWidthBudget = Math.max(maxWidthBudget, MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_LANDSCAPE_BUDGET_PX);
        }
        return Math.min(maxWidthBudget,
                Math.max(minRequiredWidth, candidate));
    }

    private int resolveSharedOverviewRenderWidth(int renderWidth, int columnCount) {
        if (columnCount <= MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_MEDIUM_COLUMN_COUNT) {
            return Math.max(renderWidth, MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_NARROW_BUDGET_PX);
        }
        return renderWidth;
    }

    private boolean shouldUseLandscapeProcessBudget(int columnCount,
                                                    int measuredSourceWidth,
                                                    List<List<MesProBatchRecordParsedCell>> rows) {
        return measuredSourceWidth >= MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_DENSE_BUDGET_PX
                || (columnCount >= MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_MEDIUM_COLUMN_COUNT
                && hasWideOperationMatrix(rows));
    }

    private boolean hasWideOperationMatrix(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        boolean hasEquipmentRow = false;
        boolean hasBodyRow = false;
        boolean hasRepeatedProcessDetails = false;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            String text = rowText(row);
            hasEquipmentRow = hasEquipmentRow || text.contains("设备编码") && text.contains("是否在计量效期内");
            hasBodyRow = hasBodyRow || text.contains("操作日期")
                    && text.contains("生产数量/pcs")
                    && text.contains("复核人");
            hasRepeatedProcessDetails = hasRepeatedProcessDetails
                    || text.contains("烘干温度")
                    || text.contains("烘干时间")
                    || text.contains("热合");
        }
        return hasEquipmentRow && hasBodyRow && hasRepeatedProcessDetails;
    }

    private boolean isSharedOverviewTemplate(MesProBatchRecordParsedTable parsedTable) {
        if (parsedTable == null) {
            return false;
        }
        if (parsedTable.getSourceTableIndex() != null && parsedTable.getSourceTableIndex() == 1) {
            return true;
        }
        MesProBatchRecordSharedPageTitleRules.SharedPageTitleType titleType = detectSharedPageTitleType(parsedTable);
        return titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY
                || titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE;
    }

    private boolean isSharedProcessTemplate(MesProBatchRecordParsedTable parsedTable) {
        return detectSharedPageTitleType(parsedTable)
                == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD;
    }

    private MesProBatchRecordSharedPageTitleRules.SharedPageTitleType detectSharedPageTitleType(
            MesProBatchRecordParsedTable parsedTable) {
        if (parsedTable == null) {
            return MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE;
        }
        List<MesProBatchRecordParsedCell> probeRow = List.of(MesProBatchRecordParsedCell.builder()
                .text(parsedTable.getTableTitle())
                .rowSpan(1)
                .colSpan(1)
                .widthPx(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX)
                .heightPx(MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT)
                .build());
        return MesProBatchRecordSharedPageTitleRules.detectTitleType(probeRow);
    }

    private List<Integer> buildUniformOverviewWidths(int columnCount, int renderWidth) {
        int[] columnWidths = new int[Math.max(columnCount, 1)];
        int baseWidth = Math.max(1, renderWidth / Math.max(columnCount, 1));
        int remainder = Math.max(0, renderWidth - baseWidth * columnWidths.length);
        for (int index = 0; index < columnWidths.length; index++) {
            columnWidths[index] = baseWidth + (index < remainder ? 1 : 0);
        }
        List<Integer> widths = new ArrayList<>(columnWidths.length);
        for (int width : columnWidths) {
            widths.add(width);
        }
        return widths;
    }

    private List<Integer> resolveFixedColumnWidths(MesProBatchRecordParsedTable parsedTable,
                                                   List<List<MesProBatchRecordParsedCell>> rows,
                                                   int columnCount,
                                                   int renderWidth,
                                                   boolean sharedOverviewTemplate,
                                                   boolean preserveSourceGrid,
                                                   boolean preserveOverviewSourceColumnWidths) {
        List<Integer> sourceColumnWidths = parsedTable == null ? List.of() : parsedTable.getColumnWidths();
        if (sharedOverviewTemplate && preserveSourceGrid && hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)) {
            return new ArrayList<>(sourceColumnWidths);
        }
        if (sharedOverviewTemplate && preserveOverviewSourceColumnWidths
                && hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)) {
            List<Integer> scaledColumnWidths = scaleColumnWidthsToBudget(sourceColumnWidths, renderWidth);
            return rebalanceStandaloneMaterialMatrixColumnsIfPresent(rows, scaledColumnWidths, columnCount);
        }
        if (sharedOverviewTemplate) {
            return buildUniformOverviewWidths(columnCount, renderWidth);
        }
        if (preserveSourceGrid) {
            if (hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)) {
                return new ArrayList<>(sourceColumnWidths);
            }
            return scaleColumnWidthsToBudget(sourceColumnWidths, renderWidth);
        }
        return resolveFullWidthColumnBudget(rows, columnCount, renderWidth);
    }

    private List<Integer> rebalanceStandaloneMaterialMatrixColumnsIfPresent(
            List<List<MesProBatchRecordParsedCell>> rows, List<Integer> columnWidths, int columnCount) {
        if (rows == null || columnWidths == null || columnWidths.size() != columnCount
                || rows.stream().noneMatch(this::isStandaloneMaterialMatrixHeaderRow)) {
            return columnWidths;
        }
        int[] spans = resolveStandaloneMaterialHeaderColSpans(columnCount);
        List<int[]> columnGroups = new ArrayList<>();
        int cursor = 0;
        for (int span : spans) {
            columnGroups.add(new int[]{cursor, span});
            cursor += span;
        }
        if (cursor != columnCount) {
            return columnWidths;
        }
        int[] rebalanced = toIntArray(columnWidths);
        rebalanceColumnGroupsToNearEqualTotals(rebalanced, columnGroups);
        return toIntegerList(rebalanced);
    }

    private int sumPositiveWidths(List<Integer> columnWidths) {
        if (columnWidths == null || columnWidths.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Integer columnWidth : columnWidths) {
            if (columnWidth != null && columnWidth > 0) {
                total += columnWidth;
            }
        }
        return total;
    }

    private boolean shouldPreserveSourceGrid(List<List<MesProBatchRecordParsedCell>> rows,
                                             List<Integer> sourceColumnWidths,
                                             int columnCount) {
        if (!hasUsableSourceColumnWidths(sourceColumnWidths, columnCount)) {
            return false;
        }
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        int maxRowEndColumn = rows.stream()
                .filter(row -> row != null && !row.isEmpty())
                .mapToInt(this::resolveRowEndColumn)
                .max()
                .orElse(0);
        int maxRowCellCount = rows.stream()
                .filter(row -> row != null)
                .mapToInt(List::size)
                .max()
                .orElse(0);
        boolean highDensityVisualGrid = columnCount >= 60
                && maxRowEndColumn >= columnCount
                && maxRowCellCount <= Math.max(16, columnCount / 3);
        boolean hasWideMergedVisualCell = rows.stream()
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null
                        && Math.max(1, cell.getColSpan()) >= Math.max(20, columnCount / 4));
        boolean hasSparseRowsOnDenseGrid = rows.stream()
                .filter(row -> row != null && !row.isEmpty())
                .anyMatch(row -> row.size() <= Math.max(8, columnCount / 8)
                        && resolveRowEndColumn(row) >= Math.max(40, columnCount / 2));
        boolean hasTallVisualSideHeader = rows.stream()
                .flatMap(List::stream)
                .anyMatch(cell -> cell != null
                        && Math.max(1, cell.getRowSpan()) >= 3
                        && Math.max(1, cell.getColSpan()) == 1
                        && normalizeStructureToken(textOf(cell)).length() >= 4);
        boolean hasProcessChecklistBand = rows.stream()
                .anyMatch(row -> rowText(row).contains("检查要求")
                        && rowText(row).contains("操作人/日期")
                        && rowText(row).contains("复核人/日期"));
        boolean hasPostClearanceBand = rows.stream()
                .anyMatch(row -> rowText(row).contains("生产后清场记录"));
        return highDensityVisualGrid
                && hasWideMergedVisualCell
                && hasSparseRowsOnDenseGrid
                && (hasTallVisualSideHeader || hasProcessChecklistBand || hasPostClearanceBand);
    }

    private boolean hasUsableSourceColumnWidths(List<Integer> sourceColumnWidths, int columnCount) {
        if (sourceColumnWidths == null || sourceColumnWidths.size() != columnCount || columnCount <= 0) {
            return false;
        }
        for (Integer sourceColumnWidth : sourceColumnWidths) {
            if (sourceColumnWidth == null || sourceColumnWidth <= 0) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> scaleColumnWidthsToBudget(List<Integer> sourceColumnWidths, int renderWidth) {
        int columnCount = sourceColumnWidths.size();
        int targetWidth = Math.max(columnCount, renderWidth);
        int sourceTotalWidth = 0;
        for (Integer sourceColumnWidth : sourceColumnWidths) {
            sourceTotalWidth += Math.max(1, sourceColumnWidth == null ? 0 : sourceColumnWidth);
        }
        if (sourceTotalWidth <= 0) {
            return buildUniformOverviewWidths(columnCount, targetWidth);
        }

        int[] scaledWidths = new int[columnCount];
        double[] fractions = new double[columnCount];
        int assignedWidth = 0;
        for (int index = 0; index < columnCount; index++) {
            int sourceWidth = Math.max(1, sourceColumnWidths.get(index));
            double exactWidth = targetWidth * (sourceWidth / (double) sourceTotalWidth);
            int scaledWidth = Math.max(1, (int) Math.floor(exactWidth));
            scaledWidths[index] = scaledWidth;
            fractions[index] = exactWidth - Math.floor(exactWidth);
            assignedWidth += scaledWidth;
        }
        while (assignedWidth < targetWidth) {
            int receiver = findBestWidthReceiver(fractions);
            scaledWidths[receiver]++;
            fractions[receiver] = 0;
            assignedWidth++;
        }
        while (assignedWidth > targetWidth) {
            int donor = findLargestShrinkableWidth(scaledWidths);
            if (donor < 0) {
                break;
            }
            scaledWidths[donor]--;
            assignedWidth--;
        }
        return toIntegerList(scaledWidths);
    }

    private int findLargestShrinkableWidth(int[] widths) {
        int bestIndex = -1;
        int bestWidth = 1;
        for (int index = 0; index < widths.length; index++) {
            if (widths[index] > bestWidth) {
                bestWidth = widths[index];
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private List<Integer> resolveFullWidthColumnBudget(List<List<MesProBatchRecordParsedCell>> rows,
                                                       int columnCount,
                                                       int renderWidth) {
        int[] resolvedColumnWidths = resolveColumnWidths(rows, columnCount);
        fitColumnWidthsToBudget(resolvedColumnWidths, renderWidth);
        if (columnCount > 1) {
            int[] floors = resolveDenseTailColumnFloors(rows, columnCount);
            mergeSharedVerticalSideHeaderFloors(rows, floors);
            mergeOperationNarrativeFloors(rows, floors);
            applyColumnFloors(resolvedColumnWidths, floors);
        }
        harmonizeChecklistOutcomeTailColumns(rows, resolvedColumnWidths);
        stretchColumnWidthsToBudget(resolvedColumnWidths, renderWidth);
        harmonizeChecklistOutcomeTailColumns(rows, resolvedColumnWidths);
        return toIntegerList(resolvedColumnWidths);
    }

    private int[] toIntArray(List<Integer> widths) {
        int[] values = new int[widths == null ? 0 : widths.size()];
        if (widths == null) {
            return values;
        }
        for (int index = 0; index < widths.size(); index++) {
            values[index] = widths.get(index) == null ? 0 : widths.get(index);
        }
        return values;
    }

    private List<Integer> toIntegerList(int[] widths) {
        List<Integer> values = new ArrayList<>(widths == null ? 0 : widths.length);
        if (widths == null) {
            return values;
        }
        for (int width : widths) {
            values.add(width);
        }
        return values;
    }

    private List<MesProBatchRecordParsedCell> buildFullWidthRow(String text, int columnCount, int tableWidth,
                                                                boolean bold, int fontSize, int heightPx) {
        return buildFullWidthRow(text, columnCount, tableWidth, bold, fontSize, heightPx, "center");
    }

    private List<MesProBatchRecordParsedCell> buildFullWidthRow(String text, int columnCount, int tableWidth,
                                                                boolean bold, int fontSize, int heightPx,
                                                                String horizontalAlign) {
        return List.of(buildCell(text, 1, columnCount, bold, fontSize, horizontalAlign, tableWidth, columnCount, heightPx));
    }

    private MesProBatchRecordParsedCell buildCell(String text, int rowSpan, int colSpan, boolean bold, int fontSize,
                                                  String horizontalAlign, int tableWidth, int totalColSpan, int heightPx) {
        int width = Math.round(tableWidth * (colSpan / (float) Math.max(totalColSpan, 1)));
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(rowSpan)
                .colSpan(colSpan)
                .bold(bold)
                .fontSize(MesProBatchRecordReportShapeRules.clampFontSize(fontSize, bold))
                .horizontalAlign(horizontalAlign)
                .verticalAlign("middle")
                .widthPx(resolveSpanWidth(width, colSpan))
                .heightPx(MesProBatchRecordReportShapeRules.clampRowHeight(heightPx))
                .fillable(false)
                .borderless(false)
                .build();
    }

    private MesProBatchRecordParsedCell cloneCell(MesProBatchRecordParsedCell source, float scale) {
        return cloneCell(source, scale, false);
    }

    private MesProBatchRecordParsedCell cloneCell(MesProBatchRecordParsedCell source, float scale,
                                                  boolean preserveExactSourceHeight) {
        String text = source.getText();
        boolean fillable = MesProBatchRecordReportShapeRules.isFillable(source);
        String placeholder = MesProBatchRecordReportShapeRules.resolvePlaceholder(source);
        String inputType = MesProBatchRecordReportShapeRules.resolveInputType(source);
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(source.getRowSpan())
                .colSpan(source.getColSpan())
                .columnIndex(source.getColumnIndex())
                .bold(source.isBold())
                .fontSize(MesProBatchRecordReportShapeRules.clampFontSize(source.getFontSize(), source.isBold()))
                .horizontalAlign(source.getHorizontalAlign())
                .verticalAlign(source.getVerticalAlign())
                .widthPx(resolveSpanWidth(Math.round(source.getWidthPx() * scale), Math.max(source.getColSpan(), 1)))
                .heightPx(preserveExactSourceHeight
                        ? Math.max(MesProBatchRecordReportShapeRules.MIN_ROW_HEIGHT_PX, source.getHeightPx())
                        : MesProBatchRecordReportShapeRules.clampPreservedRowHeight(source.getHeightPx()))
                .fillable(fillable)
                .visualBlank(source.isVisualBlank())
                .borderless(source.isBorderless())
                .diagonalSlash(source.isDiagonalSlash())
                .topBorderStyle(source.getTopBorderStyle())
                .bottomBorderStyle(source.getBottomBorderStyle())
                .leftBorderStyle(source.getLeftBorderStyle())
                .rightBorderStyle(source.getRightBorderStyle())
                .backgroundColor(source.getBackgroundColor())
                .documentFrameRole(source.getDocumentFrameRole())
                .placeholder(placeholder)
                .inputType(inputType)
                .build();
    }

    private void applySinglePageCompression(List<List<MesProBatchRecordParsedCell>> rows) {
        applySinglePageCompression(rows, List.of());
    }

    private void applySinglePageCompression(List<List<MesProBatchRecordParsedCell>> rows, List<Integer> declaredColumnWidths) {
        normalizeCellWidths(rows);
        if (declaredColumnWidths == null || declaredColumnWidths.isEmpty()) {
            rebalanceDenseTailColumns(rows);
        } else {
            applyResolvedColumnWidths(rows, toIntArray(declaredColumnWidths), true);
        }
        List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes = classifyRowTypes(rows);
        int targetHeight = MesProBatchRecordReportShapeRules.shouldUseRelaxedCalibratorViewport(
                rows.size(), resolveMaxColumnCount(rows), rowTypes)
                ? MesProBatchRecordReportShapeRules.RELAXED_SINGLE_PAGE_MAX_HEIGHT_PX
                : MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX;
        List<Integer> rowHeights = resolveRowHeights(rows, rowTypes);
        while (sumHeights(rowHeights) > targetHeight
                && shrinkFonts(rows)) {
            rowHeights = resolveRowHeights(rows, rowTypes);
        }
        if (sumHeights(rowHeights) > targetHeight) {
            rowHeights = shrinkRowHeights(rows, rowHeights, rowTypes, targetHeight);
        }
        int preferredViewportBudget = resolvePreferredViewportHeightBudget(rowTypes, targetHeight);
        if (preferredViewportBudget < targetHeight
                && sumHeights(rowHeights) > preferredViewportBudget) {
            rowHeights = shrinkRowHeights(rows, rowHeights, rowTypes, preferredViewportBudget);
        }
        int minimumViewportBudget = resolveMinimumStructuredViewportHeightBudget(rowTypes, targetHeight);
        if (minimumViewportBudget > 0 && sumHeights(rowHeights) < minimumViewportBudget) {
            rowHeights = stretchRowHeights(rowHeights, rowTypes, minimumViewportBudget);
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int height = rowHeights.get(rowIndex);
            for (MesProBatchRecordParsedCell cell : rows.get(rowIndex)) {
                cell.setHeightPx(height);
            }
        }
        if (declaredColumnWidths != null && !declaredColumnWidths.isEmpty()) {
            applyResolvedColumnWidths(rows, toIntArray(declaredColumnWidths), true);
        }
    }

    private void normalizeCellWidths(List<List<MesProBatchRecordParsedCell>> rows) {
        for (List<MesProBatchRecordParsedCell> row : rows) {
            for (MesProBatchRecordParsedCell cell : row) {
                cell.setWidthPx(resolveSpanWidth(cell.getWidthPx(), Math.max(cell.getColSpan(), 1)));
            }
        }
    }

    private void rebalanceDenseTailColumns(List<List<MesProBatchRecordParsedCell>> rows) {
        int columnCount = resolveMaxColumnCount(rows);
        if (columnCount < 12) {
            return;
        }
        int targetWidth = rows.stream()
                .mapToInt(row -> row.stream().mapToInt(MesProBatchRecordParsedCell::getWidthPx).sum())
                .max()
                .orElse(MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(columnCount));
        int[] columnWidths = resolveColumnWidths(rows, columnCount);
        fitColumnWidthsToBudget(columnWidths, targetWidth);
        int[] floors = resolveDenseTailColumnFloors(rows, columnCount);
        if (!applyColumnFloors(columnWidths, floors)) {
            return;
        }
        applyResolvedColumnWidths(rows, columnWidths);
    }

    private List<Integer> resolveRowHeights(List<List<MesProBatchRecordParsedCell>> rows,
                                            List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        List<Integer> rowHeights = new ArrayList<>(rows.size());
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, rowIndex);
            if (row.isEmpty()) {
                rowHeights.add(MesProBatchRecordReportShapeRules.resolveRowHeightFloor(rowType, 1));
                continue;
            }
            if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.FOOTER) {
                rowHeights.add(MesProBatchRecordReportShapeRules.resolveRowHeightFloor(rowType, 1));
                continue;
            }
            int rowHeight = MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT;
            boolean preserveRowHeight = shouldPreserveResolvedRowHeight(row);
            int maxVisualLines = 1;
            for (MesProBatchRecordParsedCell cell : row) {
                int fontSize = MesProBatchRecordReportShapeRules.clampFontSize(cell.getFontSize(), cell.isBold());
                String visibleText = cell.isFillable()
                        ? MesProBatchRecordReportShapeRules.resolvePlaceholder(cell)
                        : MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
                int estimatedHeight = preserveRowHeight
                        ? MesProBatchRecordReportShapeRules.estimatePreservedRowHeight(
                        visibleText, Math.max(cell.getWidthPx(), MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX), fontSize)
                        : MesProBatchRecordReportShapeRules.estimateRowHeight(
                        visibleText, Math.max(cell.getWidthPx(), MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX), fontSize);
                maxVisualLines = Math.max(maxVisualLines, estimateVisualLines(visibleText, cell.getWidthPx(), fontSize));
                rowHeight = Math.max(rowHeight, Math.max(cell.getHeightPx(), estimatedHeight));
            }
            rowHeight = Math.max(rowHeight,
                    MesProBatchRecordReportShapeRules.resolveRowHeightFloor(rowType, maxVisualLines));
            rowHeights.add(preserveRowHeight
                    ? MesProBatchRecordReportShapeRules.clampPreservedRowHeight(rowHeight)
                    : MesProBatchRecordReportShapeRules.clampRowHeight(rowHeight));
        }
        return rowHeights;
    }

    private boolean shouldPreserveResolvedRowHeight(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        return row.stream()
                .filter(cell -> cell != null)
                .anyMatch(cell -> cell.getHeightPx() > MesProBatchRecordReportShapeRules.MAX_ROW_HEIGHT_PX);
    }

    private boolean shrinkFonts(List<List<MesProBatchRecordParsedCell>> rows) {
        boolean changed = false;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            for (MesProBatchRecordParsedCell cell : row) {
                int current = MesProBatchRecordReportShapeRules.clampFontSize(cell.getFontSize(), cell.isBold());
                int next = MesProBatchRecordReportShapeRules.shrinkFontSize(current, cell.isBold());
                if (next < current) {
                    cell.setFontSize(next);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private List<Integer> shrinkRowHeights(List<Integer> rowHeights,
                                           List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        return shrinkRowHeights(List.of(), rowHeights, rowTypes, MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX);
    }

    private List<Integer> shrinkRowHeights(List<List<MesProBatchRecordParsedCell>> rows,
                                           List<Integer> rowHeights,
                                           List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                           int targetHeight) {
        List<Integer> heights = new ArrayList<>(rowHeights);
        int totalHeight = sumHeights(heights);
        while (totalHeight > targetHeight) {
            boolean changed = false;
            for (int priority = 0; priority <= 6
                    && totalHeight > targetHeight; priority++) {
                for (int index = 0; index < heights.size()
                        && totalHeight > targetHeight; index++) {
                    MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, index);
                    if (resolveCompressionPriority(rowType) != priority) {
                        continue;
                    }
                    int current = heights.get(index);
                    int floor = resolveCompressionFloor(rows, rowHeights, rowTypes, index, current);
                    if (current > floor) {
                        heights.set(index, current - 1);
                        totalHeight--;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return heights;
    }

    private int resolvePreferredViewportHeightBudget(
            List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes, int targetHeight) {
        if (targetHeight > MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX) {
            return targetHeight;
        }
        long headerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER);
        long fieldRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FIELD);
        long longDescriptionRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION);
        long detailRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA);
        long summaryRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY);
        long footerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FOOTER);
        long structuredRows = headerRows + fieldRows + longDescriptionRows;
        boolean structuredTailAfterSummary = hasStructuredTailAfterSummary(rowTypes);
        if (rowTypes == null || rowTypes.size() < 18 || structuredRows < 8
                || summaryRows == 0 || footerRows == 0) {
            return targetHeight;
        }
        int budget = MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX
                - MesProBatchRecordReportShapeRules.HEADER_ROW_HEIGHT_FLOOR_PX
                - MesProBatchRecordReportShapeRules.FOOTER_ROW_HEIGHT_FLOOR_PX;
        if (detailRows >= 8) {
            budget -= structuredTailAfterSummary ? 12 : 36;
        } else if (detailRows >= 6) {
            budget -= structuredTailAfterSummary ? 0 : 24;
        }
        if (structuredTailAfterSummary) {
            budget += 12;
        }
        return Math.max(520, Math.min(MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX - 1, budget));
    }

    private int resolveMinimumStructuredViewportHeightBudget(
            List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes, int targetHeight) {
        if (rowTypes == null || rowTypes.isEmpty()) {
            return 0;
        }
        long headerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER);
        long fieldRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FIELD);
        long detailRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA);
        long summaryRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY);
        long footerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FOOTER);
        if (headerRows == 0 || fieldRows == 0 || detailRows < 4 || summaryRows == 0 || footerRows == 0) {
            return 0;
        }
        int minimumHeight = 0;
        for (MesProBatchRecordSharedRowTypeRules.RowType rowType : rowTypes) {
            minimumHeight += resolveViewportPresentationHeight(rowType);
        }
        return Math.min(targetHeight, minimumHeight);
    }

    private int resolveViewportPresentationHeight(MesProBatchRecordSharedRowTypeRules.RowType rowType) {
        if (rowType == null) {
            return MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT;
        }
        return switch (rowType) {
            case TITLE, FIELD, TABLE_HEADER, SUMMARY -> 52;
            case LONG_DESCRIPTION -> 40;
            case DETAIL_DATA -> 30;
            case FOOTER -> MesProBatchRecordReportShapeRules.FOOTER_ROW_HEIGHT_FLOOR_PX;
        };
    }

    private List<Integer> stretchRowHeights(List<Integer> rowHeights,
                                            List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                            int targetHeight) {
        List<Integer> heights = new ArrayList<>(rowHeights);
        int totalHeight = sumHeights(heights);
        while (totalHeight < targetHeight) {
            boolean changed = false;
            for (int index = 0; index < heights.size() && totalHeight < targetHeight; index++) {
                MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, index);
                int current = heights.get(index);
                int ceiling = resolveStretchCeiling(rowType, current);
                if (current < ceiling) {
                    heights.set(index, current + 1);
                    totalHeight++;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return heights;
    }

    private int resolveStretchCeiling(MesProBatchRecordSharedRowTypeRules.RowType rowType, int currentHeight) {
        if (rowType == null) {
            return currentHeight;
        }
        return switch (rowType) {
            case TITLE, FIELD, TABLE_HEADER, SUMMARY -> Math.max(currentHeight, 52);
            case LONG_DESCRIPTION -> Math.max(currentHeight, 40);
            case FOOTER -> Math.max(currentHeight, MesProBatchRecordReportShapeRules.FOOTER_ROW_HEIGHT_FLOOR_PX);
            case DETAIL_DATA -> currentHeight;
        };
    }

    private boolean hasStructuredTailAfterSummary(List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        if (rowTypes == null || rowTypes.isEmpty()) {
            return false;
        }
        int lastSummaryIndex = -1;
        for (int index = 0; index < rowTypes.size(); index++) {
            if (rowTypes.get(index) == MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY) {
                lastSummaryIndex = index;
            }
        }
        if (lastSummaryIndex < 0) {
            return false;
        }
        int footerIndex = -1;
        for (int index = lastSummaryIndex + 1; index < rowTypes.size(); index++) {
            if (rowTypes.get(index) == MesProBatchRecordSharedRowTypeRules.RowType.FOOTER) {
                footerIndex = index;
                break;
            }
        }
        if (footerIndex <= lastSummaryIndex + 1) {
            return false;
        }
        int tailRows = 0;
        int tailHeaderRows = 0;
        int tailNarrativeRows = 0;
        for (int index = lastSummaryIndex + 1; index < footerIndex; index++) {
            MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypes.get(index);
            tailRows++;
            if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER) {
                tailHeaderRows++;
            } else if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION) {
                tailNarrativeRows++;
            }
        }
        return tailRows >= 3 && tailHeaderRows >= 1 && tailNarrativeRows >= 1;
    }

    private long countRowsOfType(List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                 MesProBatchRecordSharedRowTypeRules.RowType targetType) {
        if (rowTypes == null || targetType == null) {
            return 0;
        }
        return rowTypes.stream()
                .filter(targetType::equals)
                .count();
    }

    private int sumHeights(List<Integer> rowHeights) {
        return rowHeights.stream().reduce(0, Integer::sum);
    }

    private List<MesProBatchRecordSharedRowTypeRules.RowType> classifyRowTypes(List<List<MesProBatchRecordParsedCell>> rows) {
        Map<String, Integer> rowSignatureCounts = MesProBatchRecordSharedRowTypeRules.countRowSignatures(rows);
        List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes = new ArrayList<>(rows.size());
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            rowTypes.add(MesProBatchRecordSharedRowTypeRules.classifyRow(rows, rowIndex, rowSignatureCounts));
        }
        return rowTypes;
    }

    private MesProBatchRecordSharedRowTypeRules.RowType rowTypeAt(
            List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes, int rowIndex) {
        if (rowTypes == null || rowIndex < 0 || rowIndex >= rowTypes.size()) {
            return MesProBatchRecordSharedRowTypeRules.RowType.FIELD;
        }
        return rowTypes.get(rowIndex);
    }

    private int estimateVisualLines(String text, int effectiveWidth, int fontSize) {
        if (text == null || text.isBlank()) {
            return 1;
        }
        boolean narrative = MesProBatchRecordReportShapeRules.isNarrativeText(text);
        int availableWidth = Math.max(effectiveWidth
                        - (narrative ? MesProBatchRecordReportShapeRules.NARRATIVE_CELL_HORIZONTAL_PADDING
                        : MesProBatchRecordReportShapeRules.ESTIMATED_CELL_HORIZONTAL_PADDING),
                MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
        int charsPerLine = Math.max(1, availableWidth / Math.max(fontSize, 1));
        if (narrative) {
            charsPerLine = Math.min(charsPerLine, MesProBatchRecordReportShapeRules.NARRATIVE_MAX_CHAR_PER_LINE);
        }
        int visualLines = 0;
        for (String line : text.split("\\R", -1)) {
            int length = Math.max(line.trim().length(), 1);
            visualLines += Math.max(1, (int) Math.ceil(length / (double) charsPerLine));
        }
        return Math.max(1, visualLines);
    }

    private int resolveCompressionPriority(MesProBatchRecordSharedRowTypeRules.RowType rowType) {
        if (rowType == null) {
            return 0;
        }
        return switch (rowType) {
            case FIELD -> 0;
            case DETAIL_DATA -> 1;
            case TITLE -> 2;
            case TABLE_HEADER -> 3;
            case FOOTER -> 4;
            case SUMMARY -> 5;
            case LONG_DESCRIPTION -> 6;
        };
    }

    private int resolveCompressionFloor(List<List<MesProBatchRecordParsedCell>> rows,
                                        List<Integer> originalRowHeights,
                                        List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                        int rowIndex,
                                        int currentHeight) {
        MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, rowIndex);
        int baseFloor = MesProBatchRecordReportShapeRules.resolveRowHeightFloor(rowType, 1);
        if (isProtectedOperationBandEquipmentRow(rowIndex, rowTypes)) {
            return Math.max(baseFloor, 52);
        }
        if (isProtectedOperationBandBodyRow(rowIndex, rowTypes)) {
            return Math.max(baseFloor, 84);
        }
        if (isProtectedOperationBandSummaryRow(rowIndex, rowTypes)) {
            return Math.max(baseFloor, 24);
        }
        int originalHeight = rowIndex >= 0 && rowIndex < originalRowHeights.size()
                ? originalRowHeights.get(rowIndex)
                : currentHeight;
        if (isSingleColumnCompressionRow(rows, rowIndex)) {
            return baseFloor;
        }
        if (originalHeight <= MesProBatchRecordReportShapeRules.MAX_ROW_HEIGHT_PX) {
            return baseFloor;
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION) {
            return Math.max(MesProBatchRecordReportShapeRules.MIN_ROW_HEIGHT_PX, Math.min(originalHeight, 36));
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.FIELD
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER) {
            return Math.max(baseFloor, Math.min(originalHeight, 52));
        }
        return baseFloor;
    }

    private boolean isSingleColumnCompressionRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        if (rows == null || rowIndex < 0 || rowIndex >= rows.size()) {
            return false;
        }
        List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
        long singleCellRows = rows.stream()
                .filter(candidate -> candidate != null && candidate.size() == 1)
                .count();
        return row != null
                && row.size() == 1
                && rows.size() >= 20
                && singleCellRows >= rows.size() - 3L;
    }

    private boolean isProtectedOperationBandEquipmentRow(int rowIndex,
                                                         List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        return rowTypeAt(rowTypes, rowIndex) == MesProBatchRecordSharedRowTypeRules.RowType.FIELD
                && rowIndex >= 0
                && rowIndex + 1 < rowTypes.size()
                && rowTypeAt(rowTypes, rowIndex + 1) == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION;
    }

    private boolean isProtectedOperationBandBodyRow(int rowIndex,
                                                    List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        return rowTypeAt(rowTypes, rowIndex) == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION
                && rowIndex > 0
                && rowTypeAt(rowTypes, rowIndex - 1) == MesProBatchRecordSharedRowTypeRules.RowType.FIELD;
    }

    private boolean isProtectedOperationBandSummaryRow(int rowIndex,
                                                       List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        return rowTypeAt(rowTypes, rowIndex) == MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY;
    }

    private int resolveSpanWidth(int widthPx, int colSpan) {
        int safeSpan = Math.max(colSpan, 1);
        int perColumnWidth = MesProBatchRecordReportShapeRules.clampColumnWidth(
                Math.max(1, Math.round(widthPx / (float) safeSpan)));
        return perColumnWidth * safeSpan;
    }

    private int resolveMaxColumnCount(List<List<MesProBatchRecordParsedCell>> rows) {
        int columnCount = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            int rowColSpan = row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum();
            columnCount = Math.max(columnCount, rowColSpan);
        }
        return columnCount;
    }

    private int resolveRowEndColumn(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return 0;
        }
        int cursor = 0;
        int rowEndColumn = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null) {
                continue;
            }
            int startColumn = cell.getColumnIndex() == null ? cursor : Math.max(0, cell.getColumnIndex());
            int colSpan = Math.max(1, cell.getColSpan());
            rowEndColumn = Math.max(rowEndColumn, startColumn + colSpan);
            cursor = startColumn + colSpan;
        }
        return rowEndColumn;
    }

    private int resolveLogicalMaxColumnCount(List<List<MesProBatchRecordParsedCell>> rows, int declaredColumnCount) {
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        int logicalMaxColumnCount = 0;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int columnIndex = 0;
            int leadingBlockedColumns = 0;
            while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                columnIndex++;
                leadingBlockedColumns++;
            }
            int rowColSpan = rows.get(rowIndex).stream()
                    .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                    .sum();
            if (declaredColumnCount > 0
                    && leadingBlockedColumns > 0
                    && rowColSpan <= declaredColumnCount
                    && leadingBlockedColumns + rowColSpan > declaredColumnCount) {
                logicalMaxColumnCount = Math.max(logicalMaxColumnCount, declaredColumnCount);
            }
            for (MesProBatchRecordParsedCell cell : rows.get(rowIndex)) {
                while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                    columnIndex++;
                }
                int colSpan = Math.max(1, cell.getColSpan());
                boolean rowHasOpenBlockedColumn = declaredColumnCount > 0
                        && hasOpenBlockedColumnBefore(blockedUntilRowByColumn, rowIndex,
                        Math.max(columnIndex, declaredColumnCount));
                if (rowHasOpenBlockedColumn && columnIndex >= declaredColumnCount) {
                    logicalMaxColumnCount = Math.max(logicalMaxColumnCount, declaredColumnCount);
                } else if (declaredColumnCount > 0
                        && columnIndex < declaredColumnCount
                        && columnIndex + colSpan > declaredColumnCount
                        && rowHasOpenBlockedColumn) {
                    logicalMaxColumnCount = Math.max(logicalMaxColumnCount, declaredColumnCount);
                } else {
                    logicalMaxColumnCount = Math.max(logicalMaxColumnCount, columnIndex + colSpan);
                }
                if (cell.getRowSpan() > 1) {
                    for (int offset = 0; offset < colSpan; offset++) {
                        blockedUntilRowByColumn.put(columnIndex + offset, rowIndex + cell.getRowSpan() - 1);
                    }
                }
                columnIndex += colSpan;
            }
        }
        return Math.max(logicalMaxColumnCount, resolveMaxColumnCount(rows));
    }

    private boolean hasOpenBlockedColumnBefore(Map<Integer, Integer> blockedUntilRowByColumn,
                                               int rowIndex,
                                               int columnIndex) {
        for (int blockedColumnIndex = 0; blockedColumnIndex < columnIndex; blockedColumnIndex++) {
            if (blockedUntilRowByColumn.getOrDefault(blockedColumnIndex, -1) >= rowIndex) {
                return true;
            }
        }
        return false;
    }

    private int[] resolveColumnWidths(List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        int[] columnWidths = new int[columnCount];
        for (List<MesProBatchRecordParsedCell> row : rows) {
            int cursor = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                int colSpan = Math.max(1, cell.getColSpan());
                int width = Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX,
                        Math.round(cell.getWidthPx() / (float) colSpan));
                for (int index = 0; index < colSpan && cursor + index < columnCount; index++) {
                    columnWidths[cursor + index] = Math.max(columnWidths[cursor + index], width);
                }
                cursor += colSpan;
            }
        }
        for (int index = 0; index < columnWidths.length; index++) {
            if (columnWidths[index] <= 0) {
                columnWidths[index] = MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX;
            }
        }
        return columnWidths;
    }

    private int[] resolveDenseTailColumnFloors(List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        int[] floors = new int[columnCount];
        for (List<MesProBatchRecordParsedCell> row : rows) {
            int cursor = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                int colSpan = Math.max(1, cell.getColSpan());
                if (colSpan == 1) {
                    floors[cursor] = Math.max(floors[cursor],
                            MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                                    cell.getText(), cursor, columnCount));
                } else {
                    mergeShortSemanticSpanFloor(floors, cursor, colSpan, cell);
                }
                cursor += colSpan;
            }
        }
        return floors;
    }

    private void mergeShortSemanticSpanFloor(int[] floors,
                                             int startColumn,
                                             int colSpan,
                                             MesProBatchRecordParsedCell cell) {
        if (floors == null || cell == null || colSpan <= 1) {
            return;
        }
        String text = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText()).replace('\n', ' ').trim();
        if (text.isBlank() || text.length() > 12 || text.contains("：") || text.contains(":")) {
            return;
        }
        int minWidth = 120;
        int perColumnFloor = (int) Math.ceil(minWidth / (double) colSpan);
        for (int index = 0; index < colSpan && startColumn + index < floors.length; index++) {
            floors[startColumn + index] = Math.max(floors[startColumn + index], perColumnFloor);
        }
    }

    private void mergeSharedVerticalSideHeaderFloors(List<List<MesProBatchRecordParsedCell>> rows, int[] floors) {
        if (rows == null || floors == null || floors.length == 0) {
            return;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            MesProBatchRecordParsedCell firstCell = row.get(0);
            if (firstCell == null) {
                continue;
            }
            if (Math.max(1, firstCell.getColSpan()) != 1 || Math.max(1, firstCell.getRowSpan()) < 4) {
                continue;
            }
            String text = textOf(firstCell).replace("\n", "").trim();
            if (text.isBlank()) {
                continue;
            }
            floors[0] = Math.max(floors[0], 120);
        }
    }

    private void mergeOperationNarrativeFloors(List<List<MesProBatchRecordParsedCell>> rows, int[] floors) {
        if (rows == null || floors == null || floors.length == 0) {
            return;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            boolean operationInstructionBodyRow = isOperationInstructionBodyRow(row);
            int cursor = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                int colSpan = Math.max(1, cell.getColSpan());
                String text = textOf(cell);
                if (text.contains("合格标准")) {
                    int perColumnFloor = (int) Math.ceil(520D / colSpan);
                    for (int index = 0; index < colSpan && cursor + index < floors.length; index++) {
                        floors[cursor + index] = Math.max(floors[cursor + index], perColumnFloor);
                    }
                }
                if (operationInstructionBodyRow && isOperationSelfInspectionTailLabel(text)) {
                    int perColumnFloor = (int) Math.ceil(
                            OPERATION_SELF_INSPECTION_TAIL_WIDTH_FLOOR_PX / (double) colSpan);
                    for (int index = 0; index < colSpan && cursor + index < floors.length; index++) {
                        floors[cursor + index] = Math.max(floors[cursor + index], perColumnFloor);
                    }
                }
                cursor += colSpan;
            }
        }
    }

    private boolean isOperationSelfInspectionTailLabel(String text) {
        return text.contains("生产数量/pcs")
                || text.contains("自检合格数量/pcs")
                || text.contains("不合格数量/pcs")
                || "操作人".equals(text)
                || "复核人".equals(text);
    }

    private boolean applyColumnFloors(int[] columnWidths, int[] floors) {
        int deficit = 0;
        for (int index = 0; index < columnWidths.length; index++) {
            if (floors[index] > columnWidths[index]) {
                deficit += floors[index] - columnWidths[index];
            }
        }
        if (deficit <= 0) {
            return false;
        }
        for (int index = 0; index < floors.length; index++) {
            if (floors[index] > columnWidths[index]) {
                columnWidths[index] = floors[index];
            }
        }
        while (deficit > 0) {
            int donor = findBestWidthDonor(columnWidths, floors);
            if (donor < 0) {
                break;
            }
            columnWidths[donor]--;
            deficit--;
        }
        return true;
    }

    private void fitColumnWidthsToBudget(int[] columnWidths, int targetWidth) {
        int totalWidth = 0;
        for (int columnWidth : columnWidths) {
            totalWidth += columnWidth;
        }
        if (totalWidth <= targetWidth) {
            return;
        }
        int[] floors = new int[columnWidths.length];
        int shrinkableWidth = 0;
        for (int index = 0; index < columnWidths.length; index++) {
            floors[index] = MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX;
            shrinkableWidth += Math.max(0, columnWidths[index] - floors[index]);
        }
        int overflow = totalWidth - targetWidth;
        if (overflow <= 0) {
            return;
        }
        if (shrinkableWidth <= 0) {
            return;
        }
        if (overflow >= shrinkableWidth) {
            for (int index = 0; index < columnWidths.length; index++) {
                columnWidths[index] = floors[index];
            }
            return;
        }

        double[] fractions = new double[columnWidths.length];
        int reduced = 0;
        for (int index = 0; index < columnWidths.length; index++) {
            int capacity = Math.max(0, columnWidths[index] - floors[index]);
            if (capacity <= 0) {
                fractions[index] = -1;
                continue;
            }
            double exactReduction = overflow * (capacity / (double) shrinkableWidth);
            int appliedReduction = Math.min(capacity, (int) Math.floor(exactReduction));
            columnWidths[index] -= appliedReduction;
            fractions[index] = capacity - appliedReduction <= 0 ? -1 : exactReduction - Math.floor(exactReduction);
            reduced += appliedReduction;
        }
        int remaining = overflow - reduced;
        while (remaining > 0) {
            int donor = findBestShrinkReceiver(fractions, columnWidths, floors);
            if (donor < 0) {
                donor = findBestWidthDonor(columnWidths, floors);
                if (donor < 0) {
                    break;
                }
            }
            columnWidths[donor]--;
            if (columnWidths[donor] <= floors[donor]) {
                fractions[donor] = -1;
            } else {
                fractions[donor] = 0;
            }
            remaining--;
        }
    }

    private int findBestShrinkReceiver(double[] fractions, int[] columnWidths, int[] floors) {
        int bestIndex = -1;
        double bestFraction = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < fractions.length; index++) {
            if (columnWidths[index] <= floors[index]) {
                continue;
            }
            if (fractions[index] > bestFraction) {
                bestFraction = fractions[index];
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private void stretchColumnWidthsToBudget(int[] columnWidths, int targetWidth) {
        int totalWidth = 0;
        for (int columnWidth : columnWidths) {
            totalWidth += columnWidth;
        }
        if (totalWidth <= 0 || totalWidth >= targetWidth) {
            return;
        }
        int extraWidth = targetWidth - totalWidth;
        double[] fractions = new double[columnWidths.length];
        int distributed = 0;
        for (int index = 0; index < columnWidths.length; index++) {
            double exactShare = extraWidth * (columnWidths[index] / (double) totalWidth);
            int share = (int) Math.floor(exactShare);
            columnWidths[index] += share;
            fractions[index] = exactShare - share;
            distributed += share;
        }
        int remaining = extraWidth - distributed;
        while (remaining > 0) {
            int receiver = findBestWidthReceiver(fractions);
            columnWidths[receiver]++;
            fractions[receiver] = 0;
            remaining--;
        }
    }

    private int findBestWidthDonor(int[] columnWidths, int[] floors) {
        int bestIndex = -1;
        int bestSlack = 0;
        for (int index = 0; index < columnWidths.length; index++) {
            int floor = floors[index] > 0
                    ? floors[index]
                    : MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX;
            int slack = columnWidths[index] - floor;
            if (slack > bestSlack) {
                bestSlack = slack;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private int findBestWidthReceiver(double[] fractions) {
        int bestIndex = 0;
        double bestFraction = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < fractions.length; index++) {
            if (fractions[index] > bestFraction) {
                bestFraction = fractions[index];
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private void harmonizeChecklistOutcomeTailColumns(List<List<MesProBatchRecordParsedCell>> rows, int[] columnWidths) {
        if (rows == null || columnWidths == null || columnWidths.length == 0) {
            return;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (!isChecklistOutcomeAlignmentHeaderRow(row) || row.size() < 5) {
                continue;
            }
            int cursor = 0;
            List<int[]> tailRanges = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                MesProBatchRecordParsedCell cell = row.get(cellIndex);
                int span = Math.max(1, cell.getColSpan());
                int startColumn = cell.getColumnIndex() == null ? cursor : Math.max(0, cell.getColumnIndex());
                if (cellIndex >= row.size() - CHECKLIST_OUTCOME_FIXED_TAIL_COL_SPANS.length) {
                    tailRanges.add(new int[]{startColumn, span});
                }
                cursor = startColumn + span;
            }
            if (tailRanges.size() != CHECKLIST_OUTCOME_FIXED_TAIL_COL_SPANS.length) {
                continue;
            }
            rebalanceColumnGroupsToNearEqualTotals(columnWidths, tailRanges);
        }
    }

    private void rebalanceColumnGroupsToNearEqualTotals(int[] columnWidths, List<int[]> columnGroups) {
        if (columnGroups == null || columnGroups.isEmpty()) {
            return;
        }
        int totalWidth = 0;
        for (int[] group : columnGroups) {
            totalWidth += sumColumns(columnWidths, group[0], group[1]);
        }
        if (totalWidth <= 0) {
            return;
        }
        int groupCount = columnGroups.size();
        int baseGroupWidth = totalWidth / groupCount;
        int remainder = totalWidth % groupCount;
        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            int targetGroupWidth = baseGroupWidth + (groupIndex < remainder ? 1 : 0);
            distributeWidthAcrossColumns(columnWidths,
                    columnGroups.get(groupIndex)[0],
                    columnGroups.get(groupIndex)[1],
                    targetGroupWidth);
        }
    }

    private int sumColumns(int[] columnWidths, int start, int length) {
        int total = 0;
        for (int offset = 0; offset < length && start + offset < columnWidths.length; offset++) {
            total += columnWidths[start + offset];
        }
        return total;
    }

    private void distributeWidthAcrossColumns(int[] columnWidths, int start, int length, int totalWidth) {
        if (length <= 0 || start < 0 || start >= columnWidths.length) {
            return;
        }
        int normalizedLength = Math.min(length, columnWidths.length - start);
        int baseWidth = Math.max(1, totalWidth / normalizedLength);
        int remainder = Math.max(0, totalWidth - baseWidth * normalizedLength);
        for (int offset = 0; offset < normalizedLength; offset++) {
            columnWidths[start + offset] = baseWidth + (offset < remainder ? 1 : 0);
        }
    }

    private void applyResolvedColumnWidths(List<List<MesProBatchRecordParsedCell>> rows, int[] columnWidths) {
        applyResolvedColumnWidths(rows, columnWidths, false);
    }

    private void applyResolvedColumnWidths(List<List<MesProBatchRecordParsedCell>> rows, int[] columnWidths,
                                           boolean skipDocumentHeaderRows) {
        applyResolvedColumnWidths(rows, columnWidths, skipDocumentHeaderRows, false);
    }

    private void applyResolvedColumnWidths(List<List<MesProBatchRecordParsedCell>> rows, int[] columnWidths,
                                           boolean skipDocumentHeaderRows, boolean honorSourceColumnIndex) {
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (skipDocumentHeaderRows && isDocumentHeaderLayoutRow(row)) {
                continue;
            }
            int cursor = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                int colSpan = Math.max(1, cell.getColSpan());
                int width = 0;
                int startColumn = honorSourceColumnIndex && cell.getColumnIndex() != null
                        ? Math.max(0, cell.getColumnIndex())
                        : cursor;
                for (int index = 0; index < colSpan && startColumn + index < columnWidths.length; index++) {
                    width += columnWidths[startColumn + index];
                }
            cell.setWidthPx(width);
            cursor = startColumn + colSpan;
        }
        }
    }

    private boolean isDocumentHeaderLayoutRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        return row.stream()
                .anyMatch(cell -> cell != null
                        && DOCUMENT_FRAME_HEADER_ROLE.equals(cell.getDocumentFrameRole()));
    }

    private boolean isDocumentFooterLayoutRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        return row.stream()
                .anyMatch(cell -> cell != null
                        && DOCUMENT_FRAME_FOOTER_ROLE.equals(cell.getDocumentFrameRole()));
    }

    private String textOf(MesProBatchRecordParsedCell cell) {
        return cell == null || cell.getText() == null ? "" : cell.getText().trim();
    }

    private boolean isBlankCell(MesProBatchRecordParsedCell cell) {
        return textOf(cell).isBlank() || cell.isVisualBlank();
    }

    private void restoreSourceIndexedRepeatedOperationTailBlankBlocks(List<List<MesProBatchRecordParsedCell>> rows,
                                                                       int columnCount) {
        if (rows == null || rows.isEmpty() || columnCount < 10) {
            return;
        }
        int scanIndex = 0;
        while (scanIndex < rows.size()) {
            List<MesProBatchRecordParsedCell> row = rows.get(scanIndex);
            if (!isRepeatedOperationBlockStartRow(row) || !hasSourceColumnIndexes(row)) {
                scanIndex++;
                continue;
            }

            List<int[]> sectionBlocks = new ArrayList<>();
            int sectionEndIndex = scanIndex;
            while (sectionEndIndex < rows.size()
                    && isRepeatedOperationBlockStartRow(rows.get(sectionEndIndex))
                    && hasSourceColumnIndexes(rows.get(sectionEndIndex))) {
                int blockEndIndex = sectionEndIndex + 1;
                while (blockEndIndex < rows.size()
                        && !isRepeatedOperationBlockStartRow(rows.get(blockEndIndex))
                        && !isRepeatedOperationBlockBoundaryRow(rows.get(blockEndIndex))) {
                    blockEndIndex++;
                }
                sectionBlocks.add(new int[]{sectionEndIndex, blockEndIndex});
                sectionEndIndex = blockEndIndex;
            }

            boolean dryingRepeatedSection = containsDryingSubsection(rows, scanIndex, sectionEndIndex);
            int perBlockTailEndColumn = dryingRepeatedSection
                    ? Math.min(columnCount - 3, columnCount - 1)
                    : columnCount - 1;
            for (int[] block : sectionBlocks) {
                int blockStartIndex = block[0];
                int blockEndIndex = block[1];
                if (blockEndIndex - blockStartIndex < 3) {
                    continue;
                }
                int tailStartColumn = resolveSourceIndexedTailStartColumn(rows, blockStartIndex, blockEndIndex, columnCount);
                if (tailStartColumn <= perBlockTailEndColumn) {
                    mergeSourceIndexedTailBlankColumns(rows, blockStartIndex, blockEndIndex - 1,
                            tailStartColumn, perBlockTailEndColumn);
                }
            }
            if (dryingRepeatedSection && sectionEndIndex - scanIndex >= 3) {
                int signatureStartColumn = Math.max(0, columnCount - 2);
                mergeSourceIndexedTailBlankColumns(rows, scanIndex, sectionEndIndex - 1,
                        signatureStartColumn, columnCount - 1);
            }
            scanIndex = Math.max(sectionEndIndex, scanIndex + 1);
        }
    }

    private void closeSourceIndexedRightEdgeGaps(List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        if (rows == null || rows.isEmpty() || columnCount <= 0) {
            return;
        }
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            int columnIndex = 0;
            int rowEndColumn = 0;
            MesProBatchRecordParsedCell rightmostCell = null;
            int rightmostCellStart = 0;
            int rightmostCellEnd = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null) {
                    continue;
                }
                if (cell.getColumnIndex() != null) {
                    columnIndex = Math.max(0, cell.getColumnIndex());
                }
                while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                    columnIndex++;
                }
                int colSpan = Math.max(1, cell.getColSpan());
                int cellEnd = columnIndex + colSpan;
                if (cellEnd > rowEndColumn) {
                    rowEndColumn = cellEnd;
                    rightmostCell = cell;
                    rightmostCellStart = columnIndex;
                    rightmostCellEnd = cellEnd;
                }
                if (Math.max(1, cell.getRowSpan()) > 1) {
                    for (int offset = 0; offset < colSpan; offset++) {
                        blockedUntilRowByColumn.put(columnIndex + offset,
                                rowIndex + Math.max(1, cell.getRowSpan()) - 1);
                    }
                }
                columnIndex = cellEnd;
            }
            if (!hasSourceColumnIndexes(row)
                    || rightmostCell == null
                    || rightmostCellEnd >= columnCount
                    || rightmostCellStart >= columnCount
                    || isDocumentFooterLayoutRow(row)
                    || allColumnsBlocked(blockedUntilRowByColumn, rowIndex, rowEndColumn, columnCount)) {
                continue;
            }
            int extension = columnCount - rightmostCellEnd;
            rightmostCell.setColSpan(Math.max(1, rightmostCell.getColSpan()) + extension);
            if (Math.max(1, rightmostCell.getRowSpan()) > 1) {
                for (int column = rightmostCellEnd; column < columnCount; column++) {
                    blockedUntilRowByColumn.put(column, rowIndex + Math.max(1, rightmostCell.getRowSpan()) - 1);
                }
            }
        }
    }

    private boolean allColumnsBlocked(Map<Integer, Integer> blockedUntilRowByColumn,
                                      int rowIndex,
                                      int startColumn,
                                      int endColumn) {
        int start = Math.max(0, startColumn);
        int end = Math.max(start, endColumn);
        for (int column = start; column < end; column++) {
            if (blockedUntilRowByColumn.getOrDefault(column, -1) < rowIndex) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSourceColumnIndexes(List<MesProBatchRecordParsedCell> row) {
        if (row == null) {
            return false;
        }
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && cell.getColumnIndex() != null) {
                return true;
            }
        }
        return false;
    }

    private int resolveSourceIndexedTailStartColumn(List<List<MesProBatchRecordParsedCell>> rows,
                                                    int startRowIndex,
                                                    int endExclusive,
                                                    int columnCount) {
        int defaultTailStart = Math.max(0, columnCount - 5);
        int firstBlankTailColumn = columnCount;
        for (int rowIndex = startRowIndex; rowIndex < endExclusive; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty() || !isRepeatedOperationBlockStartRow(row)) {
                continue;
            }
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null || !isBlankCell(cell) || cell.isDiagonalSlash()) {
                    continue;
                }
                int startColumn = sourceStartColumn(row, cell, -1);
                if (startColumn >= defaultTailStart) {
                    firstBlankTailColumn = Math.min(firstBlankTailColumn, startColumn);
                }
            }
        }
        return firstBlankTailColumn == columnCount ? defaultTailStart : firstBlankTailColumn;
    }

    private int sourceStartColumn(List<MesProBatchRecordParsedCell> row,
                                  MesProBatchRecordParsedCell target,
                                  int defaultValue) {
        if (row == null || target == null) {
            return defaultValue;
        }
        int cursor = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            int startColumn = cell.getColumnIndex() == null ? cursor : Math.max(0, cell.getColumnIndex());
            if (cell == target) {
                return startColumn;
            }
            cursor = Math.max(cursor, startColumn + Math.max(1, cell.getColSpan()));
        }
        return defaultValue;
    }

    private void mergeSourceIndexedTailBlankColumns(List<List<MesProBatchRecordParsedCell>> rows,
                                                    int startRowIndex,
                                                    int endRowIndex,
                                                    int startColumnIndex,
                                                    int endColumnIndex) {
        int rowSpan = Math.max(1, endRowIndex - startRowIndex + 1);
        for (int columnIndex = endColumnIndex; columnIndex >= startColumnIndex; columnIndex--) {
            if (!isSourceIndexedTailColumnBlank(rows, startRowIndex, endRowIndex, columnIndex)) {
                continue;
            }
            List<MesProBatchRecordParsedCell> firstRow = rows.get(startRowIndex);
            CellPosition anchor = findSourceCellPositionAtColumn(firstRow, columnIndex);
            MesProBatchRecordParsedCell anchorCell;
            if (anchor == null) {
                anchorCell = buildRecoveredTailBlankCell(columnIndex);
                insertSourceIndexedCell(firstRow, anchorCell);
            } else {
                anchorCell = anchor.cell();
                anchorCell.setText("");
            }
            anchorCell.setColumnIndex(columnIndex);
            anchorCell.setColSpan(1);
            anchorCell.setRowSpan(rowSpan);
            anchorCell.setFillable(true);
            anchorCell.setVisualBlank(false);
            anchorCell.setDiagonalSlash(false);
            for (int rowIndex = endRowIndex; rowIndex > startRowIndex; rowIndex--) {
                removeSourceIndexedBlankCellAtColumn(rows.get(rowIndex), columnIndex);
            }
        }
    }

    private boolean isSourceIndexedTailColumnBlank(List<List<MesProBatchRecordParsedCell>> rows,
                                                   int startRowIndex,
                                                   int endRowIndex,
                                                   int columnIndex) {
        for (int rowIndex = startRowIndex; rowIndex <= endRowIndex; rowIndex++) {
            CellPosition position = findSourceCellPositionAtColumn(rows.get(rowIndex), columnIndex);
            if (position == null) {
                continue;
            }
            MesProBatchRecordParsedCell cell = position.cell();
            if (cell == null || !isBlankCell(cell) || cell.isDiagonalSlash()) {
                return false;
            }
        }
        return true;
    }

    private MesProBatchRecordParsedCell buildRecoveredTailBlankCell(int columnIndex) {
        return MesProBatchRecordParsedCell.builder()
                .text("")
                .rowSpan(1)
                .colSpan(1)
                .columnIndex(columnIndex)
                .bold(false)
                .fontSize(PROCESS_BODY_FONT_SIZE)
                .horizontalAlign("center")
                .verticalAlign(MesProBatchRecordReportShapeRules.DEFAULT_VERTICAL_ALIGN)
                .widthPx(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX)
                .heightPx(MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT)
                .fillable(true)
                .visualBlank(false)
                .build();
    }

    private void insertSourceIndexedCell(List<MesProBatchRecordParsedCell> row,
                                         MesProBatchRecordParsedCell cell) {
        int targetColumn = Math.max(0, cell.getColumnIndex() == null ? 0 : cell.getColumnIndex());
        int insertIndex = row.size();
        int cursor = 0;
        for (int index = 0; index < row.size(); index++) {
            MesProBatchRecordParsedCell existing = row.get(index);
            int existingColumn = existing.getColumnIndex() == null ? cursor : Math.max(0, existing.getColumnIndex());
            if (existingColumn > targetColumn) {
                insertIndex = index;
                break;
            }
            cursor = Math.max(cursor, existingColumn + Math.max(1, existing.getColSpan()));
        }
        row.add(insertIndex, cell);
    }

    private void removeSourceIndexedBlankCellAtColumn(List<MesProBatchRecordParsedCell> row,
                                                      int columnIndex) {
        CellPosition position = findSourceCellPositionAtColumn(row, columnIndex);
        if (position == null || position.cell() == null) {
            return;
        }
        MesProBatchRecordParsedCell cell = position.cell();
        if (!isBlankCell(cell) || cell.isDiagonalSlash()) {
            return;
        }
        row.remove(position.cellIndex());
    }

    private CellPosition findSourceCellPositionAtColumn(List<MesProBatchRecordParsedCell> row, int targetColumnIndex) {
        if (row == null) {
            return null;
        }
        int cursor = 0;
        for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
            MesProBatchRecordParsedCell cell = row.get(cellIndex);
            int colSpan = Math.max(cell.getColSpan(), 1);
            int columnIndex = cell.getColumnIndex() == null ? cursor : Math.max(0, cell.getColumnIndex());
            if (targetColumnIndex >= columnIndex && targetColumnIndex < columnIndex + colSpan) {
                return new CellPosition(cellIndex, cell);
            }
            cursor = Math.max(cursor, columnIndex + colSpan);
        }
        return null;
    }

    private void mergeTrailingBlankColumnsAcrossRepeatedOperationBlocks(List<List<MesProBatchRecordParsedCell>> rows,
                                                                        int columnCount,
                                                                        boolean processTemplate) {
        if (columnCount < 10 || (!processTemplate && !containsRepeatedOperationBlockWithDrying(rows))) {
            return;
        }
        int scanIndex = 0;
        while (scanIndex < rows.size()) {
            if (!isRepeatedOperationBlockStartRow(rows.get(scanIndex))) {
                scanIndex++;
                continue;
            }
            int blockEndIndex = scanIndex + 1;
            while (blockEndIndex < rows.size()
                    && !isRepeatedOperationBlockStartRow(rows.get(blockEndIndex))
                    && !isRepeatedOperationBlockBoundaryRow(rows.get(blockEndIndex))) {
                blockEndIndex++;
            }
            if (blockEndIndex - scanIndex >= 3 && containsDryingSubsection(rows, scanIndex, blockEndIndex)) {
                alignRepeatedOperationTailBlankStart(rows, scanIndex, blockEndIndex, columnCount);
                mergeTrailingBlankColumns(rows, scanIndex, blockEndIndex - 1,
                        resolveRepeatedBlockTailMergeStartColumn(rows, scanIndex, blockEndIndex, columnCount),
                        columnCount - 1);
            }
            scanIndex = blockEndIndex;
        }
    }

    private boolean containsRepeatedOperationBlockWithDrying(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            if (!isRepeatedOperationBlockStartRow(rows.get(rowIndex))) {
                continue;
            }
            int endExclusive = rowIndex + 1;
            while (endExclusive < rows.size()
                    && !isRepeatedOperationBlockStartRow(rows.get(endExclusive))
                    && !isRepeatedOperationBlockBoundaryRow(rows.get(endExclusive))) {
                endExclusive++;
            }
            if (endExclusive - rowIndex >= 3 && containsDryingSubsection(rows, rowIndex, endExclusive)) {
                return true;
            }
        }
        return false;
    }

    private void alignRepeatedOperationTailBlankStart(List<List<MesProBatchRecordParsedCell>> rows,
                                                      int startRowIndex,
                                                      int endExclusive,
                                                      int columnCount) {
        if (columnCount <= PROCESS_TEMPLATE_TOTAL_COL_SPAN || rows == null) {
            return;
        }
        int targetTailStartColumn = Math.min(columnCount - 2, PROCESS_TEMPLATE_TOTAL_COL_SPAN - 2);
        int blockRowSpan = Math.max(1, endExclusive - startRowIndex);
        for (int rowIndex = startRowIndex; rowIndex < endExclusive; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (!isRepeatedOperationBlockStartRow(row)) {
                continue;
            }
            int tailCellIndex = findRepeatedOperationTailBlankCellIndex(row, targetTailStartColumn);
            if (tailCellIndex < 0) {
                continue;
            }
            compactPrefixBeforeCell(row, tailCellIndex, targetTailStartColumn);
            MesProBatchRecordParsedCell tailCell = row.get(tailCellIndex);
            tailCell.setText("");
            tailCell.setVisualBlank(true);
            tailCell.setRowSpan(blockRowSpan);
            truncateContinuationCellsBeforeTail(rows, rowIndex + 1, endExclusive, targetTailStartColumn);
        }
    }

    private void truncateContinuationCellsBeforeTail(List<List<MesProBatchRecordParsedCell>> rows,
                                                     int startRowIndex,
                                                     int endExclusive,
                                                     int targetTailStartColumn) {
        for (int rowIndex = startRowIndex; rowIndex < endExclusive; rowIndex++) {
            truncateCellSpanningColumn(rows.get(rowIndex), targetTailStartColumn);
        }
    }

    private void truncateCellSpanningColumn(List<MesProBatchRecordParsedCell> row, int targetColumnIndex) {
        if (row == null || row.isEmpty() || targetColumnIndex <= 0) {
            return;
        }
        int cursor = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            int colSpan = Math.max(1, cell.getColSpan());
            if (cursor < targetColumnIndex && cursor + colSpan > targetColumnIndex) {
                cell.setColSpan(Math.max(1, targetColumnIndex - cursor));
                return;
            }
            cursor += colSpan;
        }
    }

    private int findRepeatedOperationTailBlankCellIndex(List<MesProBatchRecordParsedCell> row,
                                                        int targetTailStartColumn) {
        if (row == null || row.isEmpty()) {
            return -1;
        }
        int cursor = 0;
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < row.size(); index++) {
            MesProBatchRecordParsedCell cell = row.get(index);
            int colSpan = Math.max(1, cell.getColSpan());
            if (cursor + colSpan > targetTailStartColumn
                    && isBlankCell(cell)
                    && Math.max(1, cell.getRowSpan()) >= 2) {
                int distance = Math.abs(cursor - targetTailStartColumn);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = index;
                }
            }
            cursor += colSpan;
        }
        return bestIndex;
    }

    private void compactPrefixBeforeCell(List<MesProBatchRecordParsedCell> row, int cellIndex, int targetPrefixSpan) {
        int prefixSpan = 0;
        for (int index = 0; index < cellIndex; index++) {
            prefixSpan += Math.max(1, row.get(index).getColSpan());
        }
        int excess = prefixSpan - targetPrefixSpan;
        for (int index = cellIndex - 1; index >= 0 && excess > 0; index--) {
            MesProBatchRecordParsedCell cell = row.get(index);
            int colSpan = Math.max(1, cell.getColSpan());
            int reducible = colSpan - 1;
            if (reducible <= 0) {
                continue;
            }
            int reduction = Math.min(excess, reducible);
            cell.setColSpan(colSpan - reduction);
            excess -= reduction;
        }
    }

    private int resolveRepeatedBlockTailMergeStartColumn(List<List<MesProBatchRecordParsedCell>> rows,
                                                         int startRowIndex,
                                                         int endExclusive,
                                                         int columnCount) {
        if (rows == null || columnCount <= 0) {
            return 0;
        }
        int firstStart = Math.max(0, columnCount - 5);
        for (int rowIndex = startRowIndex; rowIndex < endExclusive; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty() || !isRepeatedOperationBlockStartRow(row)) {
                continue;
            }
            int cursor = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                int colSpan = Math.max(1, cell.getColSpan());
                if (isBlankCell(cell) && Math.max(1, cell.getRowSpan()) >= 2) {
                    firstStart = Math.min(firstStart, cursor);
                }
                cursor += colSpan;
            }
        }
        return Math.max(0, Math.min(firstStart, columnCount - 1));
    }

    private void expandPackedMaterialMatrixRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                int columnCount,
                                                int renderWidth,
                                                boolean processTemplate) {
        if (columnCount < PACKED_MATERIAL_MATRIX_HEADER_COUNT + 1) {
            return;
        }
        for (int rowIndex = resolveLeadingDocumentHeaderRowCount(rows); rowIndex < rows.size(); rowIndex++) {
            PackedMaterialMatrix packed = parsePackedMaterialMatrixRow(rows.get(rowIndex), columnCount);
            if (packed == null) {
                continue;
            }
            List<List<MesProBatchRecordParsedCell>> expandedRows = buildPackedMaterialMatrixRows(
                    packed, columnCount, renderWidth);
            if (expandedRows.isEmpty()) {
                continue;
            }
            rows.remove(rowIndex);
            rows.addAll(rowIndex, expandedRows);
            rowIndex += expandedRows.size() - 1;
        }
    }

    private PackedMaterialMatrix parsePackedMaterialMatrixRow(List<MesProBatchRecordParsedCell> row, int columnCount) {
        if (row == null || row.size() != 2) {
            return null;
        }
        MesProBatchRecordParsedCell sideHeaderCell = row.get(0);
        MesProBatchRecordParsedCell packedCell = row.get(1);
        if (sideHeaderCell == null || packedCell == null
                || !isNarrowPackedMatrixSideHeader(sideHeaderCell, columnCount)
                || Math.max(1, packedCell.getColSpan()) < columnCount - Math.max(8, Math.max(1, sideHeaderCell.getColSpan()) + 2)) {
            return null;
        }
        List<String> lines = MesProBatchRecordPackedMaterialMatrixTextSupport.nonBlankLines(packedCell.getText());
        if (lines.size() < PACKED_MATERIAL_MATRIX_HEADER_COUNT + 2) {
            return null;
        }
        List<String> headerTexts = new ArrayList<>(lines.subList(0, PACKED_MATERIAL_MATRIX_HEADER_COUNT));
        if (!isRepeatedHeaderTextPattern(headerTexts)) {
            return null;
        }
        boolean explicitMergedSideHeader = Math.max(1, sideHeaderCell.getRowSpan()) >= 4;
        boolean collapsedMatrixShape = lines.size() >= PACKED_MATERIAL_MATRIX_HEADER_COUNT + 4
                && Math.max(1, packedCell.getColSpan()) >= Math.max(1,
                columnCount - Math.max(1, sideHeaderCell.getColSpan()));
        if (!explicitMergedSideHeader && !collapsedMatrixShape) {
            return null;
        }
        List<String> itemNames = MesProBatchRecordPackedMaterialMatrixTextSupport.extractItemNames(
                lines, PACKED_MATERIAL_MATRIX_HEADER_COUNT);
        if (itemNames.size() < 2) {
            return null;
        }
        return new PackedMaterialMatrix(sideHeaderCell, packedCell, headerTexts, itemNames);
    }

    private boolean isNarrowPackedMatrixSideHeader(MesProBatchRecordParsedCell sideHeaderCell, int columnCount) {
        if (sideHeaderCell == null || columnCount <= 0) {
            return false;
        }
        int sideColSpan = Math.max(1, sideHeaderCell.getColSpan());
        int sideWidth = Math.max(0, sideHeaderCell.getWidthPx());
        String text = textOf(sideHeaderCell).replace("\n", "").trim();
        return !text.isBlank()
                && sideColSpan <= Math.max(4, Math.max(1, columnCount / 20))
                && (sideWidth <= 220 || sideColSpan <= 3);
    }

    private List<List<MesProBatchRecordParsedCell>> buildPackedMaterialMatrixRows(PackedMaterialMatrix packed,
                                                                                  int columnCount,
                                                                                  int renderWidth) {
        int availableColumns = Math.max(1, columnCount - Math.max(1, packed.sideHeaderCell().getColSpan()));
        if (availableColumns < PACKED_MATERIAL_MATRIX_HEADER_COUNT) {
            return List.of();
        }
        List<MesProBatchRecordParsedCell> headerCells = new ArrayList<>();
        for (String text : packed.headerTexts()) {
            headerCells.add(buildPackedMaterialMatrixCell(text, 1, renderWidth, columnCount, true, 9, 24, false));
        }
        int[] colSpans = distributeAdaptiveColSpans(headerCells, availableColumns);
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        MesProBatchRecordParsedCell sideHeaderCell = cloneCell(packed.sideHeaderCell(), 1.0f);
        int detailRowCount = (int) Math.ceil(packed.itemNames().size() / 2.0d);
        sideHeaderCell.setRowSpan(Math.max(1, sideHeaderCell.getRowSpan()) + detailRowCount);
        sideHeaderCell.setColSpan(Math.max(1, packed.sideHeaderCell().getColSpan()));

        List<MesProBatchRecordParsedCell> firstRow = new ArrayList<>();
        firstRow.add(sideHeaderCell);
        firstRow.addAll(cloneRowWithDistributedSpans(headerCells, 1.0f, renderWidth, availableColumns, colSpans, null));
        rows.add(tuneProcessRow(firstRow, PROCESS_BODY_FONT_SIZE, 24, true));

        for (int pairIndex = 0; pairIndex < detailRowCount; pairIndex++) {
            int leftIndex = pairIndex * 2;
            String leftItem = packed.itemNames().get(leftIndex);
            String rightItem = leftIndex + 1 < packed.itemNames().size() ? packed.itemNames().get(leftIndex + 1) : "";
            List<MesProBatchRecordParsedCell> detailCells = List.of(
                    buildPackedMaterialMatrixCell("/", 1, renderWidth, availableColumns, false, 9, 24, false),
                    buildPackedMaterialMatrixCell(leftItem, 1, renderWidth, availableColumns, false, 9, 24, false),
                    buildPackedMaterialMatrixCell("", 1, renderWidth, availableColumns, false, 9, 24, true),
                    buildPackedMaterialMatrixCell("/", 1, renderWidth, availableColumns, false, 9, 24, false),
                    buildPackedMaterialMatrixCell(rightItem, 1, renderWidth, availableColumns, false, 9, 24, true),
                    buildPackedMaterialMatrixCell("", 1, renderWidth, availableColumns, false, 9, 24, true)
            );
            rows.add(tuneProcessRow(
                    cloneRowWithDistributedSpans(detailCells, 1.0f, renderWidth, availableColumns, colSpans, null),
                    PROCESS_BODY_FONT_SIZE, 24, false));
        }
        return rows;
    }

    private MesProBatchRecordParsedCell buildPackedMaterialMatrixCell(String text,
                                                                      int colSpan,
                                                                      int renderWidth,
                                                                      int columnCount,
                                                                      boolean bold,
                                                                      int fontSize,
                                                                      int heightPx,
                                                                      boolean fillableBlank) {
        boolean blank = text == null || text.isBlank();
        boolean fillable = fillableBlank && blank;
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(1)
                .colSpan(colSpan)
                .bold(bold)
                .fontSize(MesProBatchRecordReportShapeRules.clampFontSize(fontSize, bold))
                .horizontalAlign("center")
                .verticalAlign(MesProBatchRecordReportShapeRules.DEFAULT_VERTICAL_ALIGN)
                .widthPx(Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX,
                        Math.round(renderWidth / (float) Math.max(columnCount, 1))))
                .heightPx(MesProBatchRecordReportShapeRules.clampRowHeight(heightPx))
                .fillable(fillable)
                .visualBlank(blank && !fillable)
                .placeholder(fillable ? "" : MesProBatchRecordReportShapeRules.EDITABLE_PLACEHOLDER_TEXT)
                .inputType(MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT)
                .build();
    }

    private boolean isRepeatedOperationBlockStartRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        boolean hasSlashMarker = false;
        boolean hasMaterialToken = false;
        for (int index = 0; index < row.size(); index++) {
            String text = textOf(row.get(index));
            if (index <= 2 && "/".equals(text)) {
                hasSlashMarker = true;
            }
            if (!text.isBlank()
                    && !"/".equals(text)
                    && index >= 2) {
                hasMaterialToken = true;
            }
        }
        return hasSlashMarker && hasMaterialToken && hasMergedBlankTail(row);
    }

    private boolean hasMergedBlankTail(List<MesProBatchRecordParsedCell> row) {
        for (int index = 2; index < row.size(); index++) {
            MesProBatchRecordParsedCell cell = row.get(index);
            if (cell != null && isBlankCell(cell) && Math.max(1, cell.getRowSpan()) >= 2) {
                return true;
            }
        }
        return false;
    }

    private boolean isRepeatedOperationBlockBoundaryRow(List<MesProBatchRecordParsedCell> row) {
        return isDocumentHeaderLayoutRow(row)
                || isDocumentFooterLayoutRow(row)
                || isSectionBoundaryRow(row);
    }

    private boolean containsDryingSubsection(List<List<MesProBatchRecordParsedCell>> rows, int startIndex, int endExclusive) {
        for (int index = startIndex; index < endExclusive; index++) {
            if (isRepeatedOperationDetailRow(rows.get(index))) {
                return true;
            }
        }
        return false;
    }

    private void mergeTrailingBlankColumns(List<List<MesProBatchRecordParsedCell>> rows,
                                           int startRowIndex,
                                           int endRowIndex,
                                           int startColumnIndex,
                                           int endColumnIndex) {
        for (int columnIndex = endColumnIndex; columnIndex >= startColumnIndex; columnIndex--) {
            List<CellPosition> positions = new ArrayList<>();
            boolean allBlank = true;
            for (int rowIndex = startRowIndex; rowIndex <= endRowIndex; rowIndex++) {
                CellPosition position = findCellPositionAtColumn(rows.get(rowIndex), columnIndex);
                if (position == null || !textOf(position.cell()).isBlank()) {
                    allBlank = false;
                    break;
                }
                positions.add(position);
            }
            if (!allBlank || positions.size() < 2) {
                continue;
            }
            CellPosition first = positions.get(0);
            first.cell().setRowSpan(positions.size());
            for (int index = positions.size() - 1; index >= 1; index--) {
                CellPosition position = positions.get(index);
                rows.get(startRowIndex + index).remove(position.cellIndex());
            }
        }
    }

    private void enforceDeclaredColumnBudget(List<List<MesProBatchRecordParsedCell>> rows,
                                             int columnCount,
                                             int renderWidth,
                                             List<Integer> sourceColumnWidths) {
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        for (int rowIndex = resolveLeadingDocumentHeaderRowCount(rows); rowIndex < rows.size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            if (rowExceedsDeclaredColumnBudget(row, rowIndex, columnCount, blockedUntilRowByColumn)) {
                row = normalizeRowToAvailableColumns(row, rowIndex, columnCount, renderWidth, blockedUntilRowByColumn,
                        sourceColumnWidths);
                rows.set(rowIndex, row);
            }
            markBlockedColumns(row, rowIndex, blockedUntilRowByColumn);
        }
    }

    private int resolveLeadingDocumentHeaderRowCount(List<List<MesProBatchRecordParsedCell>> rows) {
        int count = 0;
        while (count < rows.size() && isDocumentHeaderLayoutRow(rows.get(count))) {
            count++;
        }
        return count;
    }

    private int countSlashCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if ("/".equals(normalizeStructureToken(textOf(cell)))) {
                count++;
            }
        }
        return count;
    }

    private int countChecklistChoiceCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (containsChecklistChoice(textOf(cell))) {
                count++;
            }
        }
        return count;
    }

    private int sumColSpans(List<MesProBatchRecordParsedCell> row) {
        int total = 0;
        if (row == null) {
            return total;
        }
        for (MesProBatchRecordParsedCell cell : row) {
            total += Math.max(1, cell == null ? 1 : cell.getColSpan());
        }
        return total;
    }

    private boolean rowExceedsDeclaredColumnBudget(List<MesProBatchRecordParsedCell> row,
                                                   int rowIndex,
                                                   int columnCount,
                                                   Map<Integer, Integer> blockedUntilRowByColumn) {
        int columnIndex = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                columnIndex++;
            }
            int colSpan = Math.max(1, cell.getColSpan());
            if (columnIndex + colSpan > columnCount) {
                return true;
            }
            columnIndex += colSpan;
        }
        return false;
    }

    private List<MesProBatchRecordParsedCell> normalizeRowToAvailableColumns(List<MesProBatchRecordParsedCell> row,
                                                                              int rowIndex,
                                                                              int columnCount,
                                                                              int renderWidth,
                                                                              Map<Integer, Integer> blockedUntilRowByColumn,
                                                                              List<Integer> sourceColumnWidths) {
        int availableColumns = resolveAvailableColumnSlots(row, rowIndex, columnCount, blockedUntilRowByColumn);
        List<MesProBatchRecordParsedCell> trimmedRow = trimTrailingBlankCellsToFit(row, availableColumns);
        if (trimmedRow.size() > availableColumns) {
            trimmedRow = mergeTrailingCellsToFit(trimmedRow, availableColumns);
        }
        if (trimmedRow.size() > availableColumns) {
            throw new IllegalStateException("row_shape_exceeds_available_columns rowIndex=" + rowIndex
                    + " size=" + trimmedRow.size() + " availableColumns=" + availableColumns
                    + " texts=" + trimmedRow.stream().map(this::textOf).toList());
        }
        int[] rowSpans = trimmedRow.stream()
                .mapToInt(cell -> Math.max(1, cell.getRowSpan()))
                .toArray();
        int[] colSpans = resolveSourceWidthColSpans(trimmedRow, sourceColumnWidths, 0, availableColumns);
        if (colSpans == null) {
            colSpans = distributeAdaptiveColSpans(trimmedRow, Math.max(availableColumns, trimmedRow.size()));
        }
        int normalizedWidth = Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX * availableColumns,
                Math.round(renderWidth * (availableColumns / (float) Math.max(columnCount, 1))));
        return cloneRowWithDistributedSpans(trimmedRow, 1.0f, normalizedWidth,
                Math.max(availableColumns, trimmedRow.size()), colSpans, rowSpans);
    }

    private int resolveAvailableColumnSlots(List<MesProBatchRecordParsedCell> row,
                                            int rowIndex,
                                            int columnCount,
                                            Map<Integer, Integer> blockedUntilRowByColumn) {
        int minimalRequiredColumns = row.stream()
                .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                .sum();
        int availableColumns = 0;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            if (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                continue;
            }
            availableColumns++;
        }
        int requiredWithinDeclaredColumns = Math.min(columnCount, minimalRequiredColumns);
        if (availableColumns < columnCount) {
            return Math.max(1, Math.min(availableColumns, requiredWithinDeclaredColumns));
        }
        return Math.max(1, requiredWithinDeclaredColumns);
    }

    private List<MesProBatchRecordParsedCell> trimTrailingBlankCellsToFit(List<MesProBatchRecordParsedCell> row,
                                                                          int availableColumns) {
        List<MesProBatchRecordParsedCell> trimmed = new ArrayList<>(row);
        while (trimmed.size() > availableColumns) {
            int removableIndex = findTrailingBlankCellIndex(trimmed);
            if (removableIndex < 0) {
                break;
            }
            trimmed.remove(removableIndex);
        }
        return trimmed;
    }

    private List<MesProBatchRecordParsedCell> mergeTrailingCellsToFit(List<MesProBatchRecordParsedCell> row,
                                                                       int availableColumns) {
        List<MesProBatchRecordParsedCell> merged = new ArrayList<>(row);
        while (merged.size() > availableColumns && merged.size() >= 2) {
            int rightIndex = merged.size() - 1;
            int leftIndex = rightIndex - 1;
            MesProBatchRecordParsedCell left = cloneCell(merged.get(leftIndex), 1.0f);
            MesProBatchRecordParsedCell right = merged.get(rightIndex);
            String joinedText = joinCellText(left.getText(), right == null ? "" : right.getText());
            left.setText(joinedText);
            left.setColSpan(Math.max(1, left.getColSpan()) + Math.max(1, right == null ? 1 : right.getColSpan()));
            left.setWidthPx(Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX,
                    Math.max(0, left.getWidthPx()) + Math.max(0, right == null ? 0 : right.getWidthPx())));
            left.setRowSpan(Math.max(Math.max(1, left.getRowSpan()), Math.max(1, right == null ? 1 : right.getRowSpan())));
            merged.set(leftIndex, left);
            merged.remove(rightIndex);
        }
        return merged;
    }

    private String joinCellText(String left, String right) {
        String leftText = left == null ? "" : left.trim();
        String rightText = right == null ? "" : right.trim();
        if (leftText.isBlank()) {
            return rightText;
        }
        if (rightText.isBlank()) {
            return leftText;
        }
        return leftText + "\n" + rightText;
    }

    private int findTrailingBlankCellIndex(List<MesProBatchRecordParsedCell> row) {
        for (int index = row.size() - 1; index >= 0; index--) {
            MesProBatchRecordParsedCell cell = row.get(index);
            if (cell == null) {
                return index;
            }
            if (textOf(cell).isBlank() || cell.isVisualBlank()) {
                return index;
            }
        }
        return -1;
    }

    private void markBlockedColumns(List<MesProBatchRecordParsedCell> row,
                                    int rowIndex,
                                    Map<Integer, Integer> blockedUntilRowByColumn) {
        int columnIndex = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                columnIndex++;
            }
            int colSpan = Math.max(1, cell.getColSpan());
            int rowSpan = Math.max(1, cell.getRowSpan());
            if (rowSpan > 1) {
                for (int offset = 0; offset < colSpan; offset++) {
                    blockedUntilRowByColumn.put(columnIndex + offset, rowIndex + rowSpan - 1);
                }
            }
            columnIndex += colSpan;
        }
    }

    private CellPosition findCellPositionAtColumn(List<MesProBatchRecordParsedCell> row, int targetColumnIndex) {
        if (row == null) {
            return null;
        }
        int columnIndex = 0;
        for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
            MesProBatchRecordParsedCell cell = row.get(cellIndex);
            int colSpan = Math.max(cell.getColSpan(), 1);
            if (targetColumnIndex >= columnIndex && targetColumnIndex < columnIndex + colSpan) {
                return new CellPosition(cellIndex, cell);
            }
            columnIndex += colSpan;
        }
        return null;
    }

    private String rowText(List<MesProBatchRecordParsedCell> row) {
        StringBuilder builder = new StringBuilder();
        if (row == null) {
            return "";
        }
        for (MesProBatchRecordParsedCell cell : row) {
            builder.append(textOf(cell));
        }
        return builder.toString();
    }

    private String rowSignature(List<MesProBatchRecordParsedCell> row) {
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(textOf(cell));
        }
        return builder.toString();
    }

    private static final class RowShapeTemplate {
        private final List<Integer> colSpans;
        private final List<Integer> rowSpans;
        private final List<Integer> widths;
        private final List<Integer> heights;
        private final List<Boolean> bolds;
        private final List<Integer> fontSizes;
        private final List<String> horizontalAligns;

        private RowShapeTemplate(List<Integer> colSpans, List<Integer> rowSpans, List<Integer> widths,
                                 List<Integer> heights, List<Boolean> bolds, List<Integer> fontSizes,
                                 List<String> horizontalAligns) {
            this.colSpans = colSpans;
            this.rowSpans = rowSpans;
            this.widths = widths;
            this.heights = heights;
            this.bolds = bolds;
            this.fontSizes = fontSizes;
            this.horizontalAligns = horizontalAligns;
        }

        private static RowShapeTemplate from(List<MesProBatchRecordParsedCell> row) {
            List<Integer> colSpans = new ArrayList<>(row.size());
            List<Integer> rowSpans = new ArrayList<>(row.size());
            List<Integer> widths = new ArrayList<>(row.size());
            List<Integer> heights = new ArrayList<>(row.size());
            List<Boolean> bolds = new ArrayList<>(row.size());
            List<Integer> fontSizes = new ArrayList<>(row.size());
            List<String> horizontalAligns = new ArrayList<>(row.size());
            for (MesProBatchRecordParsedCell cell : row) {
                colSpans.add(cell.getColSpan());
                rowSpans.add(cell.getRowSpan());
                widths.add(cell.getWidthPx());
                heights.add(cell.getHeightPx());
                bolds.add(cell.isBold());
                fontSizes.add(cell.getFontSize());
                horizontalAligns.add(cell.getHorizontalAlign());
            }
            return new RowShapeTemplate(colSpans, rowSpans, widths, heights, bolds, fontSizes, horizontalAligns);
        }

        private int primaryFontSize() {
            return fontSizes.stream().findFirst().orElse(PROCESS_BODY_FONT_SIZE);
        }

        private int primaryHeight() {
            return heights.stream().findFirst().orElse(MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT);
        }

        private boolean isBoldRow() {
            return bolds.stream().anyMatch(Boolean::booleanValue);
        }
    }

    private record PackedMaterialMatrix(MesProBatchRecordParsedCell sideHeaderCell,
                                        MesProBatchRecordParsedCell packedCell,
                                        List<String> headerTexts,
                                        List<String> itemNames) {
    }

    private record CellPosition(int cellIndex, MesProBatchRecordParsedCell cell) {
    }

    private record CellPlacement(MesProBatchRecordParsedCell cell, int startColumn, int endColumn) {
    }
}
