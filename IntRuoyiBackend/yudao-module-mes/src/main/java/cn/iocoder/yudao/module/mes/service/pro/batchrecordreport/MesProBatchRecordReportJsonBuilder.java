package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MesProBatchRecordReportJsonBuilder {
    private static final int A4_PORTRAIT_WIDTH_MM = 210;
    private static final int A4_PORTRAIT_HEIGHT_MM = 297;
    private static final int A4_LANDSCAPE_WIDTH_MM = 297;
    private static final int A4_LANDSCAPE_HEIGHT_MM = 210;
    private static final int LANDSCAPE_TABLE_WIDTH_THRESHOLD_PX = 1000;
    private static final int DEFAULT_PARSED_CELL_HEIGHT_PX = 36;
    private static final String DEFAULT_SIGNATURE_DISPLAY_FORMAT = "ACTOR_SIGNED_AT";

    public String build(MesProBatchRecordParsedTable parsedTable) {
        return build(parsedTable, "EBR");
    }

    public String build(MesProBatchRecordParsedTable parsedTable, String reportCode) {
        parsedTable = expandInlineCheckboxChoiceCells(parsedTable);
        int effectiveColumnCount = resolveEffectiveColumnCount(parsedTable);
        List<Integer> fixedColumnWidths = resolveFixedColumnWidths(parsedTable);
        if (!fixedColumnWidths.isEmpty()) {
            effectiveColumnCount = Math.max(effectiveColumnCount, fixedColumnWidths.size());
        }
        PageDecorationPlan decorationPlan = resolvePageDecorationPlan(parsedTable);

        JSONObject root = new JSONObject(true);
        root.put("loopBlockList", new JSONArray());

        JSONObject querySetting = new JSONObject(true);
        querySetting.put("izOpenQueryBar", false);
        querySetting.put("izDefaultQuery", true);
        root.put("querySetting", querySetting);

        JSONObject recordSubTableOrCollection = new JSONObject(true);
        recordSubTableOrCollection.put("record", new JSONArray());
        recordSubTableOrCollection.put("range", new JSONArray());
        recordSubTableOrCollection.put("group", new JSONArray());
        root.put("recordSubTableOrCollection", recordSubTableOrCollection);

        JSONObject printConfig = new JSONObject(true);
        printConfig.put("paper", "A4");
        printConfig.put("width", A4_PORTRAIT_WIDTH_MM);
        printConfig.put("height", A4_PORTRAIT_HEIGHT_MM);
        printConfig.put("definition", 1);
        printConfig.put("isBackend", false);
        printConfig.put("marginX", 6);
        printConfig.put("marginY", decorationPlan.hasDocumentHeader()
                ? MesProBatchRecordReportShapeRules.DOC_HEADER_PRINT_MARGIN_Y
                : 6);
        printConfig.put("layout", "portrait");
        printConfig.put("printCallBackUrl", "");
        root.put("printConfig", printConfig);

        JSONObject hidden = new JSONObject(true);
        hidden.put("rows", new JSONArray());
        hidden.put("cols", new JSONArray());
        JSONObject hiddenConditions = new JSONObject(true);
        hiddenConditions.put("rows", new JSONObject(true));
        hiddenConditions.put("cols", new JSONObject(true));
        hidden.put("conditions", hiddenConditions);
        root.put("hidden", hidden);
        root.put("dbexps", new JSONArray());
        root.put("dicts", new JSONArray());

        JSONObject queryFormSetting = new JSONObject(true);
        queryFormSetting.put("idField", "");
        queryFormSetting.put("useQueryForm", false);
        queryFormSetting.put("dbKey", "");
        root.put("queryFormSetting", queryFormSetting);

        root.put("freeze", "A1");
        root.put("autofilter", new JSONObject(true));
        root.put("validations", new JSONArray());

        JSONObject area = new JSONObject(true);
        area.put("sri", 0);
        area.put("sci", 0);
        area.put("eri", 0);
        area.put("eci", 0);
        area.put("width", MesProBatchRecordReportShapeRules.TARGET_RENDER_WIDTH_PX);
        area.put("height", MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT);
        root.put("area", area);

        root.put("pyGroupEngine", false);
        root.put("submitHandlers", new JSONArray());
        root.put("hiddenCells", new JSONArray());
        root.put("zonedEditionList", new JSONArray());
        root.put("isViewContentHorizontalCenter", true);

        Map<Integer, Integer> columnWidthMap = new HashMap<>();
        JSONObject rowsObject = new JSONObject(true);
        JSONArray styles = new JSONArray();
        Map<String, Integer> styleIndexes = new HashMap<>();
        List<String> merges = new ArrayList<>();
        Map<Integer, Integer> rowHeightFloors = new HashMap<>();

        List<PlacedCell> placedCells = placeCells(parsedTable, effectiveColumnCount);
        Map<Integer, List<PlacedCell>> cellsByRow = new HashMap<>();
        Map<Integer, boolean[]> occupiedColumnsByRow = buildOccupiedColumns(placedCells, effectiveColumnCount);
        List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes = classifyRowTypes(parsedTable.getRows());
        if (!fixedColumnWidths.isEmpty()) {
            for (int columnIndex = 0; columnIndex < fixedColumnWidths.size(); columnIndex++) {
                columnWidthMap.put(columnIndex, fixedColumnWidths.get(columnIndex));
            }
        }
        for (PlacedCell cell : placedCells) {
            cellsByRow.computeIfAbsent(cell.rowIndex(), key -> new ArrayList<>()).add(cell);
            if (fixedColumnWidths.isEmpty()) {
                int perColumnWidth = Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX,
                        cell.cell().getWidthPx() / Math.max(1, cell.cell().getColSpan()));
                for (int offset = 0; offset < cell.cell().getColSpan(); offset++) {
                    columnWidthMap.merge(cell.columnIndex() + offset, perColumnWidth, Math::max);
                }
            }
        }
        Map<Integer, Integer> renderedColumnWidthMap = resolveRenderedColumnWidths(columnWidthMap, parsedTable, effectiveColumnCount);
        int renderedColumnCount = resolveRenderedColumnCount(renderedColumnWidthMap, effectiveColumnCount);
        int dataRectWidth = resolveRenderedTableWidth(renderedColumnWidthMap, renderedColumnCount);
        FillFormLayoutSpec fillFormLayoutSpec = resolveFillFormLayoutSpec(placedCells, renderedColumnWidthMap);
        SequenceNumberColumnPlan sequenceNumberColumnPlan = resolveSequenceNumberColumnPlan(placedCells, cellsByRow,
                occupiedColumnsByRow, rowTypes, renderedColumnCount, parsedTable.getRowCount());
        SignatureDateCheckboxFragmentPlan signatureDateCheckboxFragmentPlan =
                resolveSignatureDateCheckboxFragments(cellsByRow);
        Set<CellRef> signatureDateCheckboxFragments = signatureDateCheckboxFragmentPlan.fragments();
        Map<Integer, List<CoveredBlankSpan>> coveredBlankSpansByRow =
                resolveCoveredBlankSpansByRow(parsedTable, cellsByRow, rowTypes, renderedColumnCount);
        boolean authoritativeSourceGrid = Boolean.TRUE.equals(parsedTable.getPreserveSourceGrid())
                && hasAuthoritativeSourceColumnWidths(parsedTable, columnWidthMap,
                sumWidths(columnWidthMap), effectiveColumnCount);
        boolean sourceBackedColumnWidths = !fixedColumnWidths.isEmpty()
                && fixedColumnWidths.size() == effectiveColumnCount;
        boolean sharedOverviewLikeTable = isSharedOverviewLikeTable(parsedTable, effectiveColumnCount);
        area.put("width", dataRectWidth);
        applyPrintPageLayout(printConfig, dataRectWidth, effectiveColumnCount);
        boolean preserveCompactedOneLineRows = hasStructuredTailAfterSummary(rowTypes);

        for (int rowIndex = 0; rowIndex < parsedTable.getRowCount(); rowIndex++) {
            JSONObject rowObject = new JSONObject(true);
            JSONObject cellsObject = new JSONObject(true);
            List<MesProBatchRecordParsedCell> sourceRow = rowIndex < parsedTable.getRows().size()
                    ? parsedTable.getRows().get(rowIndex)
                    : List.of();
            MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, rowIndex);
            boolean sectionRow = isSectionLikeRow(sourceRow);
            boolean preferSourceRowHeight = shouldPreferSourceRowHeight(sourceRow, authoritativeSourceGrid,
                    sourceBackedColumnWidths, effectiveColumnCount);
            int sourceRowHeight = resolveSourceRowHeight(sourceRow);
            boolean preserveExactSourceRowHeight = authoritativeSourceGrid && preferSourceRowHeight;
            int rowHeight = preserveExactSourceRowHeight
                    ? sourceRowHeight
                    : preferSourceRowHeight
                    ? MesProBatchRecordReportShapeRules.clampPreservedRowHeight(sourceRowHeight)
                    : preserveCompactedOneLineRows
                    ? MesProBatchRecordReportShapeRules.resolveRowHeightFloor(rowType, 1)
                    : MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT;
            int compactRowHeightFloor = 0;
            int multiLineRowHeightFloor = 0;
            int visibleFillControlRowHeightFloor = 0;
            int maxVisualLines = 1;
            for (PlacedCell cell : cellsByRow.getOrDefault(rowIndex, List.of())) {
                int effectiveWidth = resolveEffectiveCellWidth(renderedColumnWidthMap, cell.columnIndex(), cell.cell().getColSpan());
                int fontSize = MesProBatchRecordReportShapeRules.clampFontSize(cell.cell().getFontSize(), cell.cell().isBold());
                String visibleText = resolveVisibleText(cell.cell());
                int estimatedHeight = preferSourceRowHeight
                        ? MesProBatchRecordReportShapeRules.estimatePreservedRowHeight(visibleText, effectiveWidth, fontSize)
                        : MesProBatchRecordReportShapeRules.estimateRowHeight(visibleText, effectiveWidth, fontSize);
                estimatedHeight = reserveWhitespaceForWideBlankArea(cell.cell(), effectiveWidth, estimatedHeight);
                int visualLines = estimateVisualLines(visibleText, effectiveWidth, fontSize);
                maxVisualLines = Math.max(maxVisualLines, visualLines);
                int candidateHeight = cell.cell().getHeightPx();
                if (!preferSourceRowHeight
                        && (!preserveCompactedOneLineRows || shouldAllowJsonHeightGrowth(rowType, visibleText))) {
                    candidateHeight = Math.max(candidateHeight, estimatedHeight);
                }
                if (visibleText != null && visibleText.contains("\n")) {
                    multiLineRowHeightFloor = Math.max(multiLineRowHeightFloor, estimatedHeight);
                }
                rowHeight = Math.max(rowHeight,
                        preserveExactSourceRowHeight
                                ? candidateHeight
                                : preferSourceRowHeight
                                ? MesProBatchRecordReportShapeRules.clampPreservedRowHeight(candidateHeight)
                                : MesProBatchRecordReportShapeRules.clampRowHeight(candidateHeight));
                if (fillFormLayoutSpec.compact()
                        && MesProBatchRecordReportShapeRules.isCompactFillableCell(cell.cell(), effectiveWidth)) {
                    compactRowHeightFloor = Math.max(compactRowHeightFloor,
                            MesProBatchRecordReportShapeRules.resolveCompactFillRowHeightFloor(fillFormLayoutSpec.height()));
                }
                boolean staticSequenceNumberCell =
                        sequenceNumberColumnPlan.placedCellText(rowIndex, cell.columnIndex()) != null;
                boolean signatureDateCheckboxFragment =
                        signatureDateCheckboxFragments.contains(new CellRef(rowIndex, cell.columnIndex()));
                if ((!staticSequenceNumberCell && shouldUseVisibleFillFormControl(cell.cell(), rowType, sourceRow, effectiveWidth,
                        fillFormLayoutSpec.compact(), signatureDateCheckboxFragment))
                        || (sharedOverviewLikeTable && preferSourceRowHeight
                        && !isRecognizedCheckboxChoiceCell(cell.cell())
                        && !staticSequenceNumberCell
                        && shouldRenderFillForm(cell.cell(), rowType, sourceRow, effectiveWidth))) {
                    visibleFillControlRowHeightFloor = Math.max(visibleFillControlRowHeightFloor,
                            fillFormLayoutSpec.height());
                }

                JSONObject cellObject = buildCellObject(cell.cell(), styles, styleIndexes, reportCode,
                        rowIndex, cell.columnIndex(), parsedTable.getRowCount(), effectiveColumnCount,
                        sourceRow, sectionRow, effectiveWidth, fillFormLayoutSpec.compact(), rowType,
                        sequenceNumberColumnPlan, signatureDateCheckboxFragment,
                        signatureDateCheckboxFragmentPlan.appendedChoicesByAnchor()
                                .getOrDefault(new CellRef(rowIndex, cell.columnIndex()), List.of()));
                if (cell.cell().getRowSpan() > 1 || cell.cell().getColSpan() > 1) {
                    cellObject.put("merge", List.of(cell.cell().getRowSpan() - 1, cell.cell().getColSpan() - 1));
                    merges.add(toMergeRange(rowIndex, cell.columnIndex(),
                            rowIndex + cell.cell().getRowSpan() - 1,
                            cell.columnIndex() + cell.cell().getColSpan() - 1));
                }
                cellsObject.put(String.valueOf(cell.columnIndex()), cellObject);
            }
            fillBlankCells(parsedTable, rowIndex, renderedColumnCount, occupiedColumnsByRow, cellsObject, styles, styleIndexes,
                    reportCode, sectionRow, renderedColumnWidthMap, fillFormLayoutSpec.compact(), rowType,
                    coveredBlankSpansByRow.getOrDefault(rowIndex, List.of()), sequenceNumberColumnPlan);
            applyDefaultSignatureMarkers(cellsObject);
            rowObject.put("cells", cellsObject);
            int rowHeightFloor = Math.max(compactRowHeightFloor,
                    MesProBatchRecordReportShapeRules.resolveRowHeightFloor(rowType, maxVisualLines));
            rowHeightFloor = Math.max(rowHeightFloor, visibleFillControlRowHeightFloor);
            rowHeightFloor = Math.max(rowHeightFloor, resolveProtectedOperationBandRowHeightFloor(rowIndex, rowTypes));
            rowHeightFloor = Math.max(rowHeightFloor, multiLineRowHeightFloor);
            if (preferSourceRowHeight && visibleFillControlRowHeightFloor <= rowHeight) {
                rowHeightFloor = Math.min(rowHeightFloor, rowHeight);
            }
            if (compactRowHeightFloor > 0) {
                rowHeightFloors.put(rowIndex, compactRowHeightFloor);
            }
            if (visibleFillControlRowHeightFloor > 0) {
                rowHeightFloors.put(rowIndex, Math.max(rowHeightFloors.getOrDefault(rowIndex, 0),
                        visibleFillControlRowHeightFloor));
            }
            if (multiLineRowHeightFloor > 0) {
                rowHeightFloors.put(rowIndex, Math.max(rowHeightFloors.getOrDefault(rowIndex, 0), multiLineRowHeightFloor));
            }
            if (preferSourceRowHeight) {
                rowHeightFloors.put(rowIndex, Math.max(rowHeightFloors.getOrDefault(rowIndex, 0),
                        preserveExactSourceRowHeight
                                ? sourceRowHeight
                                : MesProBatchRecordReportShapeRules.clampPreservedRowHeight(sourceRowHeight)));
            }
            rowObject.put("height", preserveExactSourceRowHeight
                    ? Math.max(rowHeight, rowHeightFloor)
                    : preferSourceRowHeight
                    ? MesProBatchRecordReportShapeRules.clampPreservedRowHeight(Math.max(rowHeight, rowHeightFloor))
                    : MesProBatchRecordReportShapeRules.clampRowHeight(Math.max(rowHeight, rowHeightFloor)));
            rowsObject.put(String.valueOf(rowIndex), rowObject);
        }
        boolean sourceHeightLockedGrid = authoritativeSourceGrid
                || (Boolean.TRUE.equals(parsedTable.getPreserveSourceGrid()) && sourceBackedColumnWidths);
        if (!sourceHeightLockedGrid) {
            shrinkRowsToSinglePage(rowsObject, parsedTable.getRowCount(), effectiveColumnCount, rowTypes, rowHeightFloors);
            expandRowsToSinglePageMinimum(rowsObject, parsedTable.getRowCount(), effectiveColumnCount, rowTypes,
                    rowHeightFloors);
        }
        DecoratedRowsResult decoratedRows = applyPageDecorations(rowsObject, merges, parsedTable, styles,
                styleIndexes, reportCode, dataRectWidth, decorationPlan, rowTypes, rowHeightFloors);
        rowsObject = decoratedRows.rowsObject();
        merges = decoratedRows.merges();
        rowsObject.put("len", Math.max(decoratedRows.rowCount(), MesProBatchRecordReportShapeRules.DEFAULT_ROWS_LEN));
        root.put("rows", rowsObject);

        JSONObject colsObject = new JSONObject(true);
        for (int columnIndex = 0; columnIndex < renderedColumnCount; columnIndex++) {
            JSONObject colObject = new JSONObject(true);
            colObject.put("width",
                    renderedColumnWidthMap.getOrDefault(columnIndex, MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX));
            colsObject.put(String.valueOf(columnIndex), colObject);
        }
        colsObject.put("len", renderedColumnCount);
        root.put("cols", colsObject);

        JSONObject rpBar = new JSONObject(true);
        rpBar.put("show", false);
        rpBar.put("pageSize", "");
        rpBar.put("btnList", new JSONArray());
        root.put("rpbar", rpBar);
        root.put("fixedPrintHeadRows", decoratedRows.fixedPrintHeadRows());
        root.put("fixedPrintTailRows", decoratedRows.fixedPrintTailRows());
        root.put("displayConfig", new JSONObject(true));
        root.put("background", false);
        root.put("name", MesProBatchRecordReportShapeRules.DEFAULT_SHEET_NAME);

        JSONObject fillFormToolbar = new JSONObject(true);
        fillFormToolbar.put("show", false);
        fillFormToolbar.put("btnList", JSONArray.parseArray(
                "[\"save\",\"subTable_add\",\"verify\",\"subTable_del\",\"print\",\"close\",\"first\",\"prev\",\"next\",\"paging\",\"total\",\"last\",\"exportPDF\",\"exportExcel\",\"exportWord\"]"));
        root.put("fillFormToolbar", fillFormToolbar);

        JSONObject fillFormInfo = new JSONObject(true);
        JSONObject fillFormLayout = new JSONObject(true);
        fillFormLayout.put("direction", "horizontal");
        fillFormLayout.put("width", fillFormLayoutSpec.width());
        fillFormLayout.put("height", fillFormLayoutSpec.height());
        fillFormInfo.put("layout", fillFormLayout);
        root.put("fillFormInfo", fillFormInfo);

        root.put("styles", styles);
        root.put("freezeLineColor", "rgb(185, 185, 185)");
        root.put("merges", merges);
        root.put("dataRectWidth", dataRectWidth);
        return JSON.toJSONString(root);
    }

    private MesProBatchRecordParsedTable expandInlineCheckboxChoiceCells(MesProBatchRecordParsedTable parsedTable) {
        if (parsedTable == null || parsedTable.getRows() == null || parsedTable.getRows().isEmpty()) {
            return parsedTable;
        }
        boolean changed = false;
        int maxColumnCount = Math.max(parsedTable.getColumnCount(), 1);
        List<List<MesProBatchRecordParsedCell>> expandedRows = new ArrayList<>();
        for (List<MesProBatchRecordParsedCell> row : parsedTable.getRows()) {
            List<MesProBatchRecordParsedCell> expandedRow = new ArrayList<>();
            int rowColumnCount = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell == null) {
                    continue;
                }
                List<MesProBatchRecordParsedCell> expandedCells = expandInlineCheckboxChoiceCell(cell);
                if (expandedCells.size() > 1) {
                    changed = true;
                    expandedRow.addAll(expandedCells);
                    rowColumnCount += expandedCells.stream().mapToInt(item -> Math.max(item.getColSpan(), 1)).sum();
                } else {
                    expandedRow.add(cell);
                    rowColumnCount += Math.max(cell.getColSpan(), 1);
                }
            }
            maxColumnCount = Math.max(maxColumnCount, rowColumnCount);
            expandedRows.add(expandedRow);
        }
        if (!changed) {
            return parsedTable;
        }
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(parsedTable.getSourceTableIndex())
                .sourceTopLevelTableIndex(parsedTable.getSourceTopLevelTableIndex())
                .sourceSplitIndex(parsedTable.getSourceSplitIndex())
                .tableTitle(parsedTable.getTableTitle())
                .rowCount(parsedTable.getRowCount())
                .columnCount(maxColumnCount)
                .columnWidths(parsedTable.getColumnWidths())
                .preserveSourceGrid(parsedTable.getPreserveSourceGrid())
                .routeBSource(parsedTable.getRouteBSource())
                .documentFrame(parsedTable.getDocumentFrame())
                .rows(expandedRows)
                .build();
    }

    private List<MesProBatchRecordParsedCell> expandInlineCheckboxChoiceCell(MesProBatchRecordParsedCell cell) {
        if (cell == null) {
            return List.of();
        }
        if (cell.isFillable() || cell.isVisualBlank() || cell.isDiagonalSlash()) {
            return List.of(cell);
        }
        String text = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
        if (text.isBlank()) {
            return List.of(cell);
        }
        List<MesProBatchRecordParsedCell> parts = splitInlineCheckboxChoiceParts(cell, text);
        if (parts.size() <= 1) {
            return List.of(cell);
        }
        int originalColSpan = Math.max(cell.getColSpan(), 1);
        if (originalColSpan < parts.size()) {
            return List.of(cell);
        }
        int[] colSpans = distributeColSpans(originalColSpan, parts.size());
        List<MesProBatchRecordParsedCell> expandedCells = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            expandedCells.add(copyInlineCheckboxPart(cell, parts.get(index), colSpans[index], index == 0));
        }
        return expandedCells;
    }

    private List<MesProBatchRecordParsedCell> splitInlineCheckboxChoiceParts(MesProBatchRecordParsedCell sourceCell,
                                                                            String text) {
        if (MesProBatchRecordFillablePatternSupport.hasTrailingUnderlineFillable(text)) {
            String labelText = MesProBatchRecordFillablePatternSupport.removeTrailingUnderline(text);
            if (MesProBatchRecordCellRuleSupport.hasMultipleUncheckedCheckboxChoiceLabels(labelText)) {
                return MesProBatchRecordFillablePatternSupport.splitTrailingUnderlineFillable(
                        text,
                        Math.max(sourceCell.getWidthPx(), MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX),
                        sourceCell.getHeightPx(),
                        MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT);
            }
        }
        if (MesProBatchRecordFillablePatternSupport.hasInlineUnderlineFillable(text)) {
            return MesProBatchRecordFillablePatternSupport.splitInlineUnderlineFillables(
                    text,
                    Math.max(sourceCell.getWidthPx(), MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX),
                    sourceCell.getHeightPx(),
                    MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT);
        }
        return List.of();
    }

    private int[] distributeColSpans(int totalColSpan, int partCount) {
        int[] colSpans = new int[partCount];
        for (int index = 0; index < partCount; index++) {
            colSpans[index] = 1;
        }
        int remaining = totalColSpan - partCount;
        for (int index = partCount - 1; remaining > 0; index = Math.max(0, index - 1)) {
            colSpans[index]++;
            remaining--;
        }
        return colSpans;
    }

    private MesProBatchRecordParsedCell copyInlineCheckboxPart(MesProBatchRecordParsedCell sourceCell,
                                                              MesProBatchRecordParsedCell part,
                                                              int colSpan,
                                                              boolean firstPart) {
        return MesProBatchRecordParsedCell.builder()
                .text(part.getText())
                .rowSpan(Math.max(sourceCell.getRowSpan(), 1))
                .colSpan(colSpan)
                .columnIndex(firstPart ? sourceCell.getColumnIndex() : null)
                .logicalColumnIndex(firstPart ? sourceCell.getLogicalColumnIndex() : null)
                .logicalColSpan(null)
                .bold(sourceCell.isBold())
                .fontSize(sourceCell.getFontSize())
                .horizontalAlign(part.getHorizontalAlign())
                .verticalAlign(part.getVerticalAlign())
                .widthPx(part.getWidthPx())
                .heightPx(sourceCell.getHeightPx())
                .fillable(part.isFillable())
                .visualBlank(false)
                .borderless(sourceCell.isBorderless())
                .diagonalSlash(false)
                .topBorderStyle(sourceCell.getTopBorderStyle())
                .bottomBorderStyle(sourceCell.getBottomBorderStyle())
                .leftBorderStyle(sourceCell.getLeftBorderStyle())
                .rightBorderStyle(sourceCell.getRightBorderStyle())
                .backgroundColor(sourceCell.getBackgroundColor())
                .documentFrameRole(sourceCell.getDocumentFrameRole())
                .placeholder(part.getPlaceholder())
                .inputType(part.getInputType())
                .build();
    }

    private List<Integer> resolveFixedColumnWidths(MesProBatchRecordParsedTable parsedTable) {
        if (parsedTable.getColumnWidths() == null || parsedTable.getColumnWidths().isEmpty()) {
            return List.of();
        }
        if (parsedTable.getColumnWidths().size() != parsedTable.getColumnCount()) {
            throw new IllegalArgumentException("Parsed table fixed column width count must equal columnCount");
        }
        List<Integer> widths = new ArrayList<>(parsedTable.getColumnWidths().size());
        for (Integer width : parsedTable.getColumnWidths()) {
            if (width == null || width <= 0) {
                throw new IllegalArgumentException("Parsed table fixed column widths must be positive");
            }
            widths.add(width);
        }
        return widths;
    }

    private int sumWidths(List<Integer> widths) {
        return widths.stream().reduce(0, Integer::sum);
    }

    private int sumWidths(Map<Integer, Integer> widthsByColumn) {
        return widthsByColumn.values().stream().mapToInt(width -> {
            if (width == null || width <= 0) {
                throw new IllegalArgumentException("Column width must be positive");
            }
            return width;
        }).sum();
    }

    private void applyPrintPageLayout(JSONObject printConfig, int dataRectWidth, int columnCount) {
        if (shouldUseLandscapePrintLayout(dataRectWidth, columnCount)) {
            printConfig.put("layout", "landscape");
            printConfig.put("width", A4_LANDSCAPE_WIDTH_MM);
            printConfig.put("height", A4_LANDSCAPE_HEIGHT_MM);
            return;
        }
        printConfig.put("layout", "portrait");
        printConfig.put("width", A4_PORTRAIT_WIDTH_MM);
        printConfig.put("height", A4_PORTRAIT_HEIGHT_MM);
    }

    private boolean shouldUseLandscapePrintLayout(int dataRectWidth, int columnCount) {
        return dataRectWidth >= LANDSCAPE_TABLE_WIDTH_THRESHOLD_PX;
    }

    private Map<Integer, Integer> resolveRenderedColumnWidths(Map<Integer, Integer> rawColumnWidths,
                                                              MesProBatchRecordParsedTable parsedTable,
                                                              int effectiveColumnCount) {
        Map<Integer, Integer> renderedColumnWidths = new HashMap<>();
        int columnCount = effectiveColumnCount;
        if (columnCount <= 0) {
            return renderedColumnWidths;
        }
        int sharedBudget = MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(columnCount);
        int rawTotalWidth = sumWidths(rawColumnWidths);
        if (hasDeclaredFixedColumnWidths(parsedTable, rawColumnWidths, effectiveColumnCount)) {
            for (int columnIndex = 0; columnIndex < effectiveColumnCount; columnIndex++) {
                renderedColumnWidths.put(columnIndex, rawColumnWidths.get(columnIndex));
            }
            normalizeVisibleVerticalSectionColumnWidth(renderedColumnWidths, parsedTable, effectiveColumnCount);
            return renderedColumnWidths;
        }
        if (hasAuthoritativeSourceColumnWidths(parsedTable, rawColumnWidths, rawTotalWidth, effectiveColumnCount)) {
            for (int columnIndex = 0; columnIndex < parsedTable.getColumnWidths().size(); columnIndex++) {
                renderedColumnWidths.put(columnIndex, rawColumnWidths.get(columnIndex));
            }
            normalizeVisibleVerticalSectionColumnWidth(renderedColumnWidths, parsedTable, effectiveColumnCount);
            return renderedColumnWidths;
        }
        int sourceAnchoredWidth = resolveImageRecognizedSourceAnchoredFullRowWidth(parsedTable, columnCount);
        if (sourceAnchoredWidth > 0) {
            return distributeWidthAcrossColumns(sourceAnchoredWidth, columnCount);
        }
        int tolerance = Math.max(12, columnCount * 2);
        boolean preserveBudgetSizedWidths = rawColumnWidths.size() >= columnCount
                && Math.abs(rawTotalWidth - sharedBudget) <= tolerance;
        boolean stretchNarrowPageWidths = rawColumnWidths.size() >= columnCount
                && columnCount > 1
                && columnCount <= MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_NARROW_COLUMN_COUNT
                && rawTotalWidth > 0
                && rawTotalWidth < sharedBudget;
        boolean fitFullPageLowColumnOverviewWidths = rawColumnWidths.size() >= columnCount
                && rawTotalWidth > sharedBudget
                && MesProBatchRecordReportShapeRules.shouldPreserveFullPageWidthForLowColumnOverview(
                        parsedTable.getRows(), columnCount);
        boolean fitFullPageLowOrMediumProcessWidths = rawColumnWidths.size() >= columnCount
                && rawTotalWidth > sharedBudget
                && MesProBatchRecordReportShapeRules.shouldPreserveFullPageWidthForLowOrMediumProcessRecord(
                        parsedTable.getRows(), columnCount);
        if (stretchNarrowPageWidths || fitFullPageLowColumnOverviewWidths || fitFullPageLowOrMediumProcessWidths) {
            return stretchWidthsToBudget(rawColumnWidths, columnCount, sharedBudget);
        }
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int rawWidth = rawColumnWidths.getOrDefault(columnIndex, MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
            renderedColumnWidths.put(columnIndex, preserveBudgetSizedWidths
                    ? rawWidth
                    : MesProBatchRecordReportShapeRules.clampColumnWidth(rawWidth));
        }
        return renderedColumnWidths;
    }

    private void normalizeVisibleVerticalSectionColumnWidth(Map<Integer, Integer> renderedColumnWidths,
                                                            MesProBatchRecordParsedTable parsedTable,
                                                            int effectiveColumnCount) {
        if (renderedColumnWidths == null || parsedTable == null || parsedTable.getRows() == null
                || effectiveColumnCount <= 1 || !hasLeadingVerticalSectionColumn(parsedTable)) {
            return;
        }
        int currentWidth = Math.max(0, renderedColumnWidths.getOrDefault(0, 0));
        int visibleWidth = Math.max(24, MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
        if (currentWidth >= visibleWidth) {
            return;
        }
        int deficit = visibleWidth - currentWidth;
        renderedColumnWidths.put(0, visibleWidth);
        while (deficit > 0) {
            int donor = findBestRenderedColumnWidthDonor(renderedColumnWidths, effectiveColumnCount);
            if (donor <= 0) {
                break;
            }
            int donorWidth = renderedColumnWidths.getOrDefault(donor, 0);
            renderedColumnWidths.put(donor, donorWidth - 1);
            deficit--;
        }
    }

    private boolean hasLeadingVerticalSectionColumn(MesProBatchRecordParsedTable parsedTable) {
        for (List<MesProBatchRecordParsedCell> row : parsedTable.getRows()) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            MesProBatchRecordParsedCell firstCell = row.get(0);
            if (firstCell == null || Math.max(1, firstCell.getColSpan()) != 1
                    || Math.max(1, firstCell.getRowSpan()) < 3) {
                continue;
            }
            if (isStructurallyNarrowLeadingSectionCell(firstCell)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStructurallyNarrowLeadingSectionCell(MesProBatchRecordParsedCell firstCell) {
        String text = compactText(firstCell.getText()).replace("\n", "");
        if (text.isBlank()) {
            return false;
        }
        int width = Math.max(0, firstCell.getWidthPx());
        int height = Math.max(0, firstCell.getHeightPx()) * Math.max(1, firstCell.getRowSpan());
        if (width <= 0 || height <= 0) {
            return true;
        }
        return height >= width * 2;
    }

    private int findBestRenderedColumnWidthDonor(Map<Integer, Integer> renderedColumnWidths, int effectiveColumnCount) {
        int bestColumn = -1;
        int bestSlack = 0;
        for (int columnIndex = 1; columnIndex < effectiveColumnCount; columnIndex++) {
            int width = renderedColumnWidths.getOrDefault(columnIndex, 0);
            int floor = MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX;
            int slack = width - floor;
            if (slack > bestSlack) {
                bestSlack = slack;
                bestColumn = columnIndex;
            }
        }
        return bestColumn;
    }

    private int resolveImageRecognizedSourceAnchoredFullRowWidth(MesProBatchRecordParsedTable parsedTable, int columnCount) {
        if (parsedTable.getRows() == null || parsedTable.getRows().isEmpty() || columnCount <= 0) {
            return 0;
        }
        if (parsedTable.getColumnWidths() != null && !parsedTable.getColumnWidths().isEmpty()) {
            return 0;
        }
        if (columnCount < MesProBatchRecordReportShapeRules.DENSE_TAIL_MIN_COLUMN_COUNT) {
            return 0;
        }
        int anchoredWidth = 0;
        for (List<MesProBatchRecordParsedCell> row : parsedTable.getRows()) {
            int rowColSpan = 0;
            int rowWidth = 0;
            boolean hasMergedCell = false;
            for (MesProBatchRecordParsedCell cell : row) {
                int colSpan = Math.max(cell.getColSpan(), 1);
                rowColSpan += colSpan;
                rowWidth += Math.max(cell.getWidthPx(), 0);
                hasMergedCell = hasMergedCell || colSpan > 1;
            }
            if (rowColSpan == columnCount && hasMergedCell && rowWidth > 0) {
                anchoredWidth = Math.max(anchoredWidth, rowWidth);
            }
        }
        return anchoredWidth;
    }

    private Map<Integer, Integer> distributeWidthAcrossColumns(int totalWidth, int columnCount) {
        Map<Integer, Integer> distributedWidths = new HashMap<>();
        if (totalWidth <= 0 || columnCount <= 0) {
            return distributedWidths;
        }
        int baseWidth = Math.max(1, totalWidth / columnCount);
        int remainder = totalWidth - baseWidth * columnCount;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int width = baseWidth + (columnIndex < remainder ? 1 : 0);
            distributedWidths.put(columnIndex, Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX, width));
        }
        return distributedWidths;
    }

    private boolean hasAuthoritativeSourceColumnWidths(MesProBatchRecordParsedTable parsedTable,
                                                       Map<Integer, Integer> rawColumnWidths,
                                                       int rawTotalWidth,
                                                       int effectiveColumnCount) {
        List<Integer> sourceColumnWidths = parsedTable.getColumnWidths();
        if (!Boolean.TRUE.equals(parsedTable.getPreserveSourceGrid())) {
            return false;
        }
        if (sourceColumnWidths == null || sourceColumnWidths.isEmpty()) {
            return false;
        }
        if (rawColumnWidths.size() < sourceColumnWidths.size()) {
            return false;
        }
        int sourceTotalWidth = 0;
        for (int columnIndex = 0; columnIndex < sourceColumnWidths.size(); columnIndex++) {
            Integer sourceWidth = sourceColumnWidths.get(columnIndex);
            if (sourceWidth == null || sourceWidth <= 0) {
                return false;
            }
            if (!sourceWidth.equals(rawColumnWidths.get(columnIndex))) {
                return false;
            }
            sourceTotalWidth += sourceWidth;
        }
        return sourceTotalWidth == rawTotalWidth;
    }

    private boolean hasDeclaredFixedColumnWidths(MesProBatchRecordParsedTable parsedTable,
                                                  Map<Integer, Integer> rawColumnWidths,
                                                  int effectiveColumnCount) {
        if (shouldStretchSharedOverviewDeclaredWidths(parsedTable, rawColumnWidths, effectiveColumnCount)) {
            return false;
        }
        if (!Boolean.TRUE.equals(parsedTable.getPreserveSourceGrid())
                && effectiveColumnCount <= MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_MEDIUM_COLUMN_COUNT
                && !isSharedOverviewLikeTable(parsedTable, effectiveColumnCount)) {
            return false;
        }
        return hasMatchedDeclaredColumnWidths(parsedTable, rawColumnWidths, effectiveColumnCount);
    }

    private boolean shouldStretchSharedOverviewDeclaredWidths(MesProBatchRecordParsedTable parsedTable,
                                                              Map<Integer, Integer> rawColumnWidths,
                                                              int effectiveColumnCount) {
        if (!isSharedOverviewLikeTable(parsedTable, effectiveColumnCount)
                || effectiveColumnCount > MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_NARROW_COLUMN_COUNT
                || rawColumnWidths.size() < effectiveColumnCount) {
            return false;
        }
        int rawTotalWidth = sumWidths(rawColumnWidths);
        return rawTotalWidth > 0
                && rawTotalWidth < MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(effectiveColumnCount);
    }

    private boolean isSharedOverviewLikeTable(MesProBatchRecordParsedTable parsedTable, int effectiveColumnCount) {
        if (parsedTable == null || parsedTable.getRows() == null
                || effectiveColumnCount > MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_MEDIUM_COLUMN_COUNT) {
            return false;
        }
        String tableTitle = MesProBatchRecordReportShapeRules.normalizeRecognizedText(parsedTable.getTableTitle());
        if (tableTitle.contains("工序") || tableTitle.contains("操作") || tableTitle.contains("自检")) {
            return false;
        }
        boolean overviewTitle = false;
        boolean processTitle = false;
        for (List<MesProBatchRecordParsedCell> row : parsedTable.getRows()) {
            MesProBatchRecordSharedPageTitleRules.SharedPageTitleType titleType =
                    MesProBatchRecordSharedPageTitleRules.detectTitleType(row);
            if (titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD) {
                processTitle = true;
            } else if (titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY
                    || titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE) {
                overviewTitle = true;
            }
        }
        return overviewTitle && !processTitle;
    }

    private boolean hasMatchedDeclaredColumnWidths(MesProBatchRecordParsedTable parsedTable,
                                                   Map<Integer, Integer> rawColumnWidths,
                                                   int effectiveColumnCount) {
        List<Integer> sourceColumnWidths = parsedTable.getColumnWidths();
        if (sourceColumnWidths == null || sourceColumnWidths.size() != effectiveColumnCount) {
            return false;
        }
        for (int columnIndex = 0; columnIndex < effectiveColumnCount; columnIndex++) {
            Integer sourceWidth = sourceColumnWidths.get(columnIndex);
            if (sourceWidth == null || sourceWidth <= 0 || !sourceWidth.equals(rawColumnWidths.get(columnIndex))) {
                return false;
            }
        }
        return true;
    }

    private Map<Integer, Integer> stretchWidthsToBudget(Map<Integer, Integer> rawColumnWidths, int columnCount, int targetBudget) {
        Map<Integer, Integer> stretchedWidths = new HashMap<>();
        int rawTotalWidth = sumWidths(rawColumnWidths);
        if (columnCount <= 0 || rawTotalWidth <= 0) {
            return stretchedWidths;
        }
        int assignedWidth = 0;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int rawWidth = rawColumnWidths.getOrDefault(columnIndex, MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
            int stretched = Math.max(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX,
                    Math.round(rawWidth * (targetBudget / (float) rawTotalWidth)));
            stretchedWidths.put(columnIndex, stretched);
            assignedWidth += stretched;
        }
        int remainder = targetBudget - assignedWidth;
        int cursor = 0;
        while (remainder != 0 && columnCount > 0) {
            int columnIndex = cursor % columnCount;
            int currentWidth = stretchedWidths.getOrDefault(columnIndex, MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
            if (remainder > 0) {
                stretchedWidths.put(columnIndex, currentWidth + 1);
                remainder--;
            } else if (currentWidth > MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX) {
                stretchedWidths.put(columnIndex, currentWidth - 1);
                remainder++;
            }
            cursor++;
            if (cursor > columnCount * 4 && remainder < 0) {
                break;
            }
        }
        return stretchedWidths;
    }

    private int resolveRenderedTableWidth(Map<Integer, Integer> renderedColumnWidths, int columnCount) {
        if (!renderedColumnWidths.isEmpty()) {
            return sumWidths(renderedColumnWidths);
        }
        return MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(Math.max(columnCount, 1));
    }

    private int resolveRenderedColumnCount(Map<Integer, Integer> renderedColumnWidths, int fallbackColumnCount) {
        if (renderedColumnWidths.isEmpty()) {
            return Math.max(fallbackColumnCount, 1);
        }
        return renderedColumnWidths.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private FillFormLayoutSpec resolveFillFormLayoutSpec(List<PlacedCell> placedCells,
                                                         Map<Integer, Integer> renderedColumnWidthMap) {
        List<Integer> compactFillWidths = new ArrayList<>();
        List<Integer> compactFillHeights = new ArrayList<>();
        int fillableCount = 0;
        for (PlacedCell placedCell : placedCells) {
            if (!MesProBatchRecordReportShapeRules.isFillable(placedCell.cell())) {
                continue;
            }
            fillableCount++;
            int effectiveWidth = resolveEffectiveCellWidth(renderedColumnWidthMap,
                    placedCell.columnIndex(), placedCell.cell().getColSpan());
            if (!MesProBatchRecordReportShapeRules.isCompactFillableCell(placedCell.cell(), effectiveWidth)) {
                continue;
            }
            compactFillWidths.add(effectiveWidth);
            compactFillHeights.add(MesProBatchRecordReportShapeRules.clampRowHeight(
                    Math.max(placedCell.cell().getHeightPx(), MesProBatchRecordReportShapeRules.MIN_ROW_HEIGHT_PX)));
        }
        if (!MesProBatchRecordReportShapeRules.shouldUseCompactFillLayout(fillableCount, compactFillWidths.size())) {
            return new FillFormLayoutSpec(
                    MesProBatchRecordReportShapeRules.DEFAULT_FILL_FORM_LAYOUT_WIDTH_PX,
                    MesProBatchRecordReportShapeRules.DEFAULT_FILL_FORM_LAYOUT_HEIGHT_PX,
                    false);
        }
        return new FillFormLayoutSpec(
                MesProBatchRecordReportShapeRules.resolveCompactFillLayoutWidth(compactFillWidths),
                MesProBatchRecordReportShapeRules.resolveCompactFillLayoutHeight(compactFillHeights),
                true);
    }

    private List<PlacedCell> placeCells(MesProBatchRecordParsedTable parsedTable, int effectiveColumnCount) {
        List<PlacedCell> placedCells = new ArrayList<>();
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        int columnCount = Math.max(effectiveColumnCount, 1);
        for (int rowIndex = 0; rowIndex < parsedTable.getRows().size(); rowIndex++) {
            int columnIndex = 0;
            for (MesProBatchRecordParsedCell cell : parsedTable.getRows().get(rowIndex)) {
                if (cell.getColumnIndex() != null) {
                    columnIndex = Math.max(0, cell.getColumnIndex());
                }
                while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                    columnIndex++;
                }
                int colSpan = Math.max(1, cell.getColSpan());
                if (columnIndex + colSpan > columnCount) {
                    if (canSkipOverflowBlankCell(cell, columnIndex, columnCount)) {
                        continue;
                    }
                    throw new IllegalArgumentException("Parsed table cell exceeds declared columnCount at row "
                            + rowIndex + ", column " + columnIndex + ", colSpan " + colSpan
                            + ", columnCount " + columnCount + ", text=" + compactText(cell.getText()));
                }
                placedCells.add(new PlacedCell(rowIndex, columnIndex, cell));
                if (cell.getRowSpan() > 1) {
                    for (int offset = 0; offset < colSpan; offset++) {
                        blockedUntilRowByColumn.put(columnIndex + offset, rowIndex + cell.getRowSpan() - 1);
                    }
                }
                columnIndex += colSpan;
            }
        }
        return placedCells;
    }

    private boolean canSkipOverflowBlankCell(MesProBatchRecordParsedCell cell, int columnIndex, int columnCount) {
        if (columnIndex < columnCount || cell == null) {
            return false;
        }
        String text = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
        return text.isBlank() || cell.isVisualBlank();
    }

    private Map<Integer, boolean[]> buildOccupiedColumns(List<PlacedCell> placedCells, int effectiveColumnCount) {
        Map<Integer, boolean[]> occupiedByRow = new HashMap<>();
        int columnCount = Math.max(effectiveColumnCount, 1);
        for (PlacedCell cell : placedCells) {
            for (int rowOffset = 0; rowOffset < Math.max(cell.cell().getRowSpan(), 1); rowOffset++) {
                boolean[] occupied = occupiedByRow.computeIfAbsent(cell.rowIndex() + rowOffset, key -> new boolean[columnCount]);
                for (int colOffset = 0; colOffset < Math.max(cell.cell().getColSpan(), 1); colOffset++) {
                    int columnIndex = cell.columnIndex() + colOffset;
                    if (columnIndex >= 0 && columnIndex < columnCount) {
                        occupied[columnIndex] = true;
                    }
                }
            }
        }
        return occupiedByRow;
    }

    private SequenceNumberColumnPlan resolveSequenceNumberColumnPlan(List<PlacedCell> placedCells,
                                                                     Map<Integer, List<PlacedCell>> cellsByRow,
                                                                     Map<Integer, boolean[]> occupiedColumnsByRow,
                                                                     List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                                                     int columnCount,
                                                                     int rowCount) {
        Map<Integer, Integer> headerRowsByColumn = new HashMap<>();
        for (PlacedCell cell : placedCells) {
            if (!isSequenceNumberHeaderCell(cell.cell())) {
                continue;
            }
            for (int offset = 0; offset < Math.max(1, cell.cell().getColSpan()); offset++) {
                int columnIndex = cell.columnIndex() + offset;
                if (columnIndex >= 0 && columnIndex < columnCount) {
                    headerRowsByColumn.merge(columnIndex, cell.rowIndex(), Math::max);
                }
            }
        }
        if (headerRowsByColumn.isEmpty()) {
            return SequenceNumberColumnPlan.none();
        }

        List<Integer> sequenceColumns = new ArrayList<>(headerRowsByColumn.keySet());
        sequenceColumns.sort(Integer::compareTo);
        Set<Integer> sequenceColumnSet = new HashSet<>(sequenceColumns);
        Map<CellRef, String> placedCellValues = new HashMap<>();
        Map<CellRef, String> syntheticCellValues = new HashMap<>();
        Map<Integer, Integer> nextValueByColumn = new HashMap<>();
        for (Integer columnIndex : sequenceColumns) {
            nextValueByColumn.put(columnIndex, 1);
        }

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, rowIndex);
            for (Integer columnIndex : sequenceColumns) {
                int headerRowIndex = headerRowsByColumn.get(columnIndex);
                if (rowIndex <= headerRowIndex || !isSequenceNumberDataRow(rowType)) {
                    continue;
                }
                List<PlacedCell> rowCells = cellsByRow.getOrDefault(rowIndex, List.of());
                if (!hasSequenceNumberRowContext(rowCells, sequenceColumnSet, columnIndex)) {
                    continue;
                }
                PlacedCell sequenceCell = findPlacedCellStartingAt(rowCells, columnIndex);
                if (sequenceCell != null) {
                    if (!isSequenceNumberValueCell(sequenceCell.cell())) {
                        continue;
                    }
                    String explicitValue = normalizeSequenceNumberValue(sequenceCell.cell().getText());
                    int nextValue = nextValueByColumn.getOrDefault(columnIndex, 1);
                    String displayValue = explicitValue.isBlank() ? String.valueOf(nextValue) : explicitValue;
                    placedCellValues.put(new CellRef(rowIndex, columnIndex), displayValue);
                    nextValueByColumn.put(columnIndex, resolveNextSequenceNumber(nextValue, explicitValue));
                    continue;
                }
                if (!isColumnOccupied(occupiedColumnsByRow, rowIndex, columnIndex)) {
                    int nextValue = nextValueByColumn.getOrDefault(columnIndex, 1);
                    syntheticCellValues.put(new CellRef(rowIndex, columnIndex), String.valueOf(nextValue));
                    nextValueByColumn.put(columnIndex, nextValue + 1);
                }
            }
        }
        return new SequenceNumberColumnPlan(sequenceColumnSet, placedCellValues, syntheticCellValues);
    }

    private boolean isSequenceNumberHeaderCell(MesProBatchRecordParsedCell cell) {
        return cell != null && "序号".equals(compactText(cell.getText()));
    }

    private boolean isSequenceNumberDataRow(MesProBatchRecordSharedRowTypeRules.RowType rowType) {
        return rowType == null
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.FIELD
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER;
    }

    private boolean hasSequenceNumberRowContext(List<PlacedCell> rowCells,
                                                Set<Integer> sequenceColumns,
                                                int sequenceColumnIndex) {
        for (PlacedCell cell : rowCells) {
            if (cell == null || cell.cell() == null) {
                continue;
            }
            if (spansAnyColumn(cell, sequenceColumns) || cell.columnIndex() == sequenceColumnIndex) {
                continue;
            }
            String text = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.cell().getText());
            if (!text.isBlank() && !isSequenceNumberHeaderCell(cell.cell())) {
                return true;
            }
        }
        return false;
    }

    private boolean spansAnyColumn(PlacedCell cell, Set<Integer> columns) {
        int startColumn = cell.columnIndex();
        int endColumn = startColumn + Math.max(1, cell.cell().getColSpan()) - 1;
        for (Integer columnIndex : columns) {
            if (columnIndex >= startColumn && columnIndex <= endColumn) {
                return true;
            }
        }
        return false;
    }

    private PlacedCell findPlacedCellStartingAt(List<PlacedCell> rowCells, int columnIndex) {
        for (PlacedCell cell : rowCells) {
            if (cell.columnIndex() == columnIndex) {
                return cell;
            }
        }
        return null;
    }

    private boolean isSequenceNumberValueCell(MesProBatchRecordParsedCell cell) {
        if (cell == null || cell.isDiagonalSlash() || cell.isVisualBlank()) {
            return false;
        }
        String compact = compactText(cell.getText());
        return compact.isBlank()
                || compact.matches("\\d+[、.．)）]?")
                || compact.matches("[一二三四五六七八九十]+[、.．)）]?");
    }

    private String normalizeSequenceNumberValue(String text) {
        String compact = compactText(text);
        if (!isSequenceNumberText(compact)) {
            return "";
        }
        return compact;
    }

    private boolean isSequenceNumberText(String compactText) {
        return compactText != null
                && (compactText.matches("\\d+[、.．)）]?")
                || compactText.matches("[一二三四五六七八九十]+[、.．)）]?"));
    }

    private int resolveNextSequenceNumber(int currentValue, String explicitValue) {
        if (explicitValue != null && explicitValue.matches("\\d+[、.．)）]?")) {
            String digits = explicitValue.replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                return Math.max(currentValue + 1, Integer.parseInt(digits) + 1);
            }
        }
        return currentValue + 1;
    }

    private boolean isColumnOccupied(Map<Integer, boolean[]> occupiedColumnsByRow, int rowIndex, int columnIndex) {
        boolean[] occupiedColumns = occupiedColumnsByRow.get(rowIndex);
        return occupiedColumns != null
                && columnIndex >= 0
                && columnIndex < occupiedColumns.length
                && occupiedColumns[columnIndex];
    }

    private JSONObject buildCellObject(MesProBatchRecordParsedCell cell, JSONArray styles,
                                       Map<String, Integer> styleIndexes, String reportCode,
                                       int rowIndex, int columnIndex, int rowCount, int columnCount,
                                       List<MesProBatchRecordParsedCell> sourceRow, boolean sectionRow,
                                       int effectiveWidth, boolean compactFillLayout,
                                       MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                       SequenceNumberColumnPlan sequenceNumberColumnPlan,
                                       boolean signatureDateCheckboxFragment,
                                       List<String> appendedCheckboxChoices) {
        JSONObject cellObject = new JSONObject(true);
        String sequenceText = sequenceNumberColumnPlan.placedCellText(rowIndex, columnIndex);
        String visibleText = sequenceText == null ? resolveVisibleText(cell) : sequenceText;
        if (signatureDateCheckboxFragment) {
            visibleText = "";
        }
        cellObject.put("style", resolveStyleIndex(cell, styles, styleIndexes, rowIndex, columnIndex, rowCount, columnCount,
                sourceRow, sectionRow, resolveTextColor(visibleText)));
        if (cell.isDiagonalSlash()) {
            cellObject.put("edhrDiagonalSlash", true);
        }
        if (sequenceText != null) {
            cellObject.put("text", normalizeRenderableText(sequenceText));
            return cellObject;
        }
        boolean renderFillForm = shouldRenderFillForm(cell, rowType, sourceRow, effectiveWidth,
                signatureDateCheckboxFragment);
        if (renderFillForm) {
            boolean visibleSingleLineBlankEntry = isVisibleSingleLineBlankEntryCell(cell, sourceRow);
            boolean sameRowSignatureDateBlankFillCell = isSameRowSignatureDateBlankFillCell(cell, sourceRow);
            boolean visuallyQuietFillCell = signatureDateCheckboxFragment
                    || (!visibleSingleLineBlankEntry && shouldUseVisuallyQuietBlankFillForm(cell, rowType, sourceRow));
            cellObject.put("text", visibleText);
            cellObject.put("fillForm", buildFillForm(cell, reportCode, rowIndex, columnIndex, effectiveWidth,
                    compactFillLayout,
                    visuallyQuietFillCell,
                    visibleSingleLineBlankEntry,
                    signatureDateCheckboxFragment,
                    sameRowSignatureDateBlankFillCell,
                    appendedCheckboxChoices));
            if (!signatureDateCheckboxFragment && isRecognizedCheckboxChoiceCell(cell)) {
                cellObject.put(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY,
                        MesProBatchRecordCellRuleSupport.toRuleJson(buildCheckboxCellRule(cell, rowIndex, columnIndex,
                                appendedCheckboxChoices)));
            }
        } else {
            cellObject.put("text", normalizeRenderableText(visibleText));
        }
        return cellObject;
    }

    private BatchRecordReportCellRuleVO buildCheckboxCellRule(MesProBatchRecordParsedCell cell,
                                                              int rowIndex,
                                                              int columnIndex,
                                                              List<String> appendedCheckboxChoices) {
        BatchRecordReportCellRuleVO rule =
                MesProBatchRecordCellRuleSupport.buildAutoCheckboxRule(rowIndex, columnIndex,
                        appendCheckboxChoiceText(cell.getText(), appendedCheckboxChoices));
        if (cell.isReviewedCellRule()) {
            String source = cell.getCellRuleSource();
            rule.setSource(source == null || source.isBlank() ? "MANUAL" : source);
            rule.setReviewed(true);
            rule.setConfidence(1.0);
        }
        return rule;
    }

    private boolean shouldUseVisibleFillFormControl(MesProBatchRecordParsedCell cell,
                                                    MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                                    List<MesProBatchRecordParsedCell> sourceRow,
                                                    int effectiveWidth,
                                                    boolean compactFillLayout,
                                                    boolean signatureDateCheckboxFragment) {
        if (!shouldRenderFillForm(cell, rowType, sourceRow, effectiveWidth, signatureDateCheckboxFragment)) {
            return false;
        }
        if (signatureDateCheckboxFragment) {
            return false;
        }
        if (shouldUseVisuallyQuietBlankFillForm(cell, rowType, sourceRow)) {
            return false;
        }
        return !resolveFillPlaceholder(cell, effectiveWidth, compactFillLayout, false,
                isVisibleSingleLineBlankEntryCell(cell, sourceRow)).isBlank();
    }

    private void fillBlankCells(MesProBatchRecordParsedTable parsedTable, int rowIndex,
                                int effectiveColumnCount,
                                Map<Integer, boolean[]> occupiedColumnsByRow,
                                JSONObject cellsObject, JSONArray styles, Map<String, Integer> styleIndexes,
                                String reportCode, boolean sectionRow, Map<Integer, Integer> columnWidthMap,
                                boolean compactFillLayout,
                                MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                List<CoveredBlankSpan> coveredBlankSpans,
                                SequenceNumberColumnPlan sequenceNumberColumnPlan) {
        int columnCount = effectiveColumnCount;
        boolean[] occupied = occupiedColumnsByRow.getOrDefault(rowIndex, new boolean[columnCount]);
        boolean populatedRow = hasVisibleContentRow(occupied, cellsObject);
        int[] contentBounds = resolveContentColumnBounds(occupied, cellsObject, columnCount);
        int firstContentColumn = contentBounds[0];
        int lastContentColumn = contentBounds[1];
        List<MesProBatchRecordParsedCell> sourceRow = rowIndex < parsedTable.getRows().size()
                ? parsedTable.getRows().get(rowIndex)
                : List.of();
        boolean allowAutoFillBlanks = populatedRow && shouldAutoFillBlankCells(rowType, sourceRow);
        MesProBatchRecordParsedCell blankCell = populatedRow
                ? MesProBatchRecordParsedCell.builder()
                .text("")
                .bold(false)
                .fontSize(MesProBatchRecordReportShapeRules.DEFAULT_FONT_SIZE)
                .horizontalAlign("left")
                .verticalAlign(MesProBatchRecordReportShapeRules.DEFAULT_VERTICAL_ALIGN)
                .widthPx(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX)
                .heightPx(MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT)
                .fillable(true)
                .placeholder("")
                .inputType(MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT)
                .build()
                : MesProBatchRecordParsedCell.builder()
                .text(MesProBatchRecordReportShapeRules.normalizePaddingText())
                .bold(false)
                .fontSize(MesProBatchRecordReportShapeRules.DEFAULT_FONT_SIZE)
                .horizontalAlign(MesProBatchRecordReportShapeRules.DEFAULT_HORIZONTAL_ALIGN)
                .verticalAlign(MesProBatchRecordReportShapeRules.DEFAULT_VERTICAL_ALIGN)
                .widthPx(MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX)
                .heightPx(MesProBatchRecordReportShapeRules.DEFAULT_ROW_HEIGHT)
                .fillable(false)
                .build();
        markCoveredBlankSpans(occupied, cellsObject, coveredBlankSpans);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            if (occupied[columnIndex] || cellsObject.containsKey(String.valueOf(columnIndex))) {
                continue;
            }
            if (populatedRow && (columnIndex < firstContentColumn || columnIndex > lastContentColumn)) {
                continue;
            }
            int blankStyle = resolveStyleIndex(blankCell, styles, styleIndexes,
                    rowIndex, columnIndex, parsedTable.getRowCount(), effectiveColumnCount,
                    sourceRow,
                    sectionRow, null);
            JSONObject blankObject = new JSONObject(true);
            String sequenceText = sequenceNumberColumnPlan.syntheticCellText(rowIndex, columnIndex);
            blankObject.put("text", sequenceText != null
                    ? sequenceText
                    : populatedRow
                    ? ""
                    : MesProBatchRecordReportShapeRules.normalizePaddingText());
            blankObject.put("style", blankStyle);
            if (sequenceText == null
                    && allowAutoFillBlanks && columnIndex >= firstContentColumn && columnIndex <= lastContentColumn) {
                int effectiveWidth = resolveEffectiveCellWidth(columnWidthMap, columnIndex, 1);
                blankObject.put("fillForm",
                        buildFillForm(blankCell, reportCode, rowIndex, columnIndex, effectiveWidth,
                                compactFillLayout, shouldUseVisuallyQuietBlankFillForm(blankCell, rowType, sourceRow),
                                false, false, false, List.of()));
            }
            cellsObject.put(String.valueOf(columnIndex), blankObject);
        }
    }

    private void markCoveredBlankSpans(boolean[] occupied,
                                       JSONObject cellsObject,
                                       List<CoveredBlankSpan> coveredBlankSpans) {
        if (coveredBlankSpans == null || coveredBlankSpans.isEmpty()) {
            return;
        }
        for (CoveredBlankSpan span : coveredBlankSpans) {
            int startColumn = Math.max(0, span.startColumn());
            int colSpan = Math.max(1, Math.min(span.colSpan(), occupied.length - startColumn));
            if (colSpan <= 1 || startColumn >= occupied.length
                    || hasOccupiedOrRenderedCellInRange(occupied, cellsObject, startColumn, colSpan)) {
                continue;
            }
            for (int offset = 0; offset < colSpan && startColumn + offset < occupied.length; offset++) {
                occupied[startColumn + offset] = true;
            }
        }
    }

    private boolean hasOccupiedOrRenderedCellInRange(boolean[] occupied,
                                                     JSONObject cellsObject,
                                                     int startColumn,
                                                     int colSpan) {
        for (int offset = 0; offset < colSpan; offset++) {
            int columnIndex = startColumn + offset;
            if (columnIndex < occupied.length && occupied[columnIndex]) {
                return true;
            }
            if (cellsObject.containsKey(String.valueOf(columnIndex))) {
                return true;
            }
        }
        return false;
    }

    private Map<Integer, List<CoveredBlankSpan>> resolveCoveredBlankSpansByRow(
            MesProBatchRecordParsedTable parsedTable,
            Map<Integer, List<PlacedCell>> cellsByRow,
            List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
            int columnCount) {
        Map<Integer, List<CoveredBlankSpan>> spansByRow = new HashMap<>();
        if (parsedTable == null || parsedTable.getRows() == null || cellsByRow == null || columnCount <= 1) {
            return spansByRow;
        }
        for (int rowIndex = 0; rowIndex + 1 < parsedTable.getRowCount(); rowIndex++) {
            List<PlacedCell> rowCells = cellsByRow.getOrDefault(rowIndex, List.of());
            if (rowCells.isEmpty() || !isStructuredHeaderLikeRow(rowTypes, rowIndex)) {
                continue;
            }
            List<CoveredBlankSpan> candidates = rowCells.stream()
                    .filter(cell -> shouldCoverBlankSpan(cell, columnCount))
                    .map(cell -> new CoveredBlankSpan(cell.columnIndex(), Math.max(1, cell.cell().getColSpan())))
                    .toList();
            if (candidates.size() < 3) {
                continue;
            }
            List<PlacedCell> nextRowCells = cellsByRow.getOrDefault(rowIndex + 1, List.of());
            List<CoveredBlankSpan> inherited = new ArrayList<>();
            for (CoveredBlankSpan span : candidates) {
                if (!hasVisiblePlacedCellInRange(nextRowCells, span.startColumn(), span.colSpan())) {
                    inherited.add(span);
                }
            }
            if (!inherited.isEmpty()) {
                spansByRow.put(rowIndex + 1, inherited);
            }
        }
        return spansByRow;
    }

    private boolean isStructuredHeaderLikeRow(List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes, int rowIndex) {
        MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, rowIndex);
        return rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.FIELD;
    }

    private boolean shouldCoverBlankSpan(PlacedCell cell, int columnCount) {
        if (cell == null || cell.cell() == null || Math.max(1, cell.cell().getColSpan()) <= 1) {
            return false;
        }
        String text = resolveVisibleText(cell.cell());
        int floor = MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                text, cell.columnIndex(), columnCount);
        return floor > 0;
    }

    private boolean hasVisiblePlacedCellInRange(List<PlacedCell> rowCells, int startColumn, int colSpan) {
        int endColumn = startColumn + Math.max(1, colSpan);
        for (PlacedCell cell : rowCells) {
            int cellStart = cell.columnIndex();
            int cellEnd = cellStart + Math.max(1, cell.cell().getColSpan());
            if (cellStart >= endColumn || cellEnd <= startColumn) {
                continue;
            }
            String text = MesProBatchRecordReportShapeRules.normalizeRecognizedText(resolveVisibleText(cell.cell()));
            if (!text.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private int[] resolveContentColumnBounds(boolean[] occupied, JSONObject cellsObject, int columnCount) {
        int first = columnCount;
        int last = -1;
        if (cellsObject != null && !cellsObject.isEmpty()) {
            for (String key : cellsObject.keySet()) {
                if (key == null || !key.chars().allMatch(Character::isDigit)) {
                    continue;
                }
                int columnIndex = Integer.parseInt(key);
                JSONObject cellObject = cellsObject.getJSONObject(key);
                int colSpan = 1;
                if (cellObject != null && cellObject.getJSONArray("merge") != null) {
                    colSpan = Math.max(1, cellObject.getJSONArray("merge").getIntValue(1) + 1);
                }
                first = Math.min(first, columnIndex);
                last = Math.max(last, columnIndex + colSpan - 1);
            }
        } else {
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                if (!occupied[columnIndex]) {
                    continue;
                }
                first = Math.min(first, columnIndex);
                last = Math.max(last, columnIndex);
            }
        }
        if (last < 0) {
            return new int[]{0, -1};
        }
        return new int[]{first, last};
    }

    private boolean hasVisibleContentRow(boolean[] occupied, JSONObject cellsObject) {
        if (cellsObject != null && !cellsObject.isEmpty()) {
            return true;
        }
        for (boolean value : occupied) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    private void applyDefaultSignatureMarkers(JSONObject cellsObject) {
        String activeLabel = null;
        String activeActionType = null;
        boolean markerAssigned = false;
        for (Integer columnIndex : numericKeys(cellsObject)) {
            JSONObject cell = cellsObject.getJSONObject(String.valueOf(columnIndex));
            if (cell == null) {
                continue;
            }
            String text = compactText(cell.getString("text"));
            String actionType = resolveDefaultSignatureActionType(text);
            if (actionType != null) {
                activeLabel = cell.getString("text");
                activeActionType = actionType;
                markerAssigned = false;
                continue;
            }
            if (!text.isEmpty()) {
                activeLabel = null;
                activeActionType = null;
                markerAssigned = false;
                continue;
            }
            if (activeActionType == null || markerAssigned || cell.getJSONObject("fillForm") == null
                    || cell.containsKey("edhrSignature")) {
                continue;
            }
            cell.put("edhrSignature", defaultSignatureMarker(activeActionType, activeLabel));
            markerAssigned = true;
        }
    }

    private List<Integer> numericKeys(JSONObject object) {
        if (object == null || object.isEmpty()) {
            return List.of();
        }
        return object.keySet().stream()
                .filter(key -> key.chars().allMatch(Character::isDigit))
                .map(Integer::valueOf)
                .sorted()
                .toList();
    }

    private JSONObject defaultSignatureMarker(String actionType, String label) {
        JSONObject signature = new JSONObject(true);
        signature.put("enabled", true);
        signature.put("actionType", actionType);
        signature.put("label", label);
        signature.put("displayFormat", DEFAULT_SIGNATURE_DISPLAY_FORMAT);
        return signature;
    }

    private String resolveDefaultSignatureActionType(String compactText) {
        if (compactText == null || compactText.isEmpty() || !compactText.contains("日期")) {
            return null;
        }
        if (compactText.contains("复核人") || compactText.contains("审核人")
                || compactText.contains("确认人") || compactText.contains("批准人")) {
            return "APPROVE";
        }
        if (compactText.contains("记录人") || compactText.contains("操作人")
                || compactText.contains("签名") || compactText.contains("签字")) {
            return "SUBMIT";
        }
        return null;
    }

    private boolean shouldAutoFillBlankCells(MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                             List<MesProBatchRecordParsedCell> sourceRow) {
        if (containsSummaryKeyword(sourceRow)) {
            return false;
        }
        if (rowType == null) {
            return true;
        }
        return switch (rowType) {
            case FIELD, DETAIL_DATA -> true;
            case TABLE_HEADER -> hasStructuredBlankEntryCues(sourceRow);
            case TITLE, LONG_DESCRIPTION, SUMMARY, FOOTER -> false;
        };
    }

    private boolean shouldRenderFillForm(MesProBatchRecordParsedCell cell,
                                         MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                         List<MesProBatchRecordParsedCell> sourceRow,
                                         int effectiveWidth) {
        return shouldRenderFillForm(cell, rowType, sourceRow, effectiveWidth, false);
    }

    private boolean shouldRenderFillForm(MesProBatchRecordParsedCell cell,
                                         MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                         List<MesProBatchRecordParsedCell> sourceRow,
                                         int effectiveWidth,
                                         boolean signatureDateCheckboxFragment) {
        if (cell != null && cell.isDiagonalSlash()) {
            return false;
        }
        if (signatureDateCheckboxFragment) {
            return isEditableRowType(rowType);
        }
        if (isRecognizedCheckboxChoiceCell(cell)) {
            if (isRepeatedEquipmentMatrixRow(sourceRow) && !isStandaloneCheckboxChoiceCell(cell, sourceRow)) {
                return false;
            }
            return isEditableRowType(rowType);
        }
        if (isNarrativePromptBlankArea(cell, effectiveWidth)) {
            return isEditableRowType(rowType);
        }
        if (!MesProBatchRecordReportShapeRules.isFillable(cell)) {
            return false;
        }
        if (isStructuredSummaryBlankEntryCell(cell, rowType, sourceRow)) {
            return true;
        }
        if (cell.isFillable()) {
            return isEditableRowType(rowType);
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION
                && isWideBlankNarrativeArea(cell, effectiveWidth)) {
            return true;
        }
        return shouldAutoFillBlankCells(rowType, sourceRow);
    }

    private boolean isStructuredSummaryBlankEntryCell(MesProBatchRecordParsedCell cell,
                                                      MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                                      List<MesProBatchRecordParsedCell> sourceRow) {
        if (rowType != MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY
                || cell == null
                || sourceRow == null
                || sourceRow.size() < 2
                || cell.isVisualBlank()
                || cell.isDiagonalSlash()
                || !MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText()).isBlank()) {
            return false;
        }
        boolean hasSummaryLabel = false;
        int blankCells = 0;
        for (MesProBatchRecordParsedCell candidate : sourceRow) {
            if (candidate == null) {
                continue;
            }
            String text = compactText(candidate.getText());
            if (containsSummaryKeyword(List.of(candidate))) {
                hasSummaryLabel = true;
            }
            if (text.isBlank() && !candidate.isVisualBlank() && !candidate.isDiagonalSlash()) {
                blankCells++;
            }
        }
        return hasSummaryLabel && blankCells >= 3;
    }

    private boolean isEditableRowType(MesProBatchRecordSharedRowTypeRules.RowType rowType) {
        return rowType != MesProBatchRecordSharedRowTypeRules.RowType.TITLE
                && rowType != MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY
                && rowType != MesProBatchRecordSharedRowTypeRules.RowType.FOOTER;
    }

    private boolean isRecognizedCheckboxChoiceCell(MesProBatchRecordParsedCell cell) {
        return cell != null
                && !cell.isVisualBlank()
                && MesProBatchRecordCellRuleSupport.isCheckboxChoiceText(cell.getText());
    }

    private SignatureDateCheckboxFragmentPlan resolveSignatureDateCheckboxFragments(Map<Integer, List<PlacedCell>> cellsByRow) {
        Set<CellRef> fragments = new HashSet<>();
        Map<CellRef, List<String>> appendedChoicesByAnchor = new HashMap<>();
        if (cellsByRow == null || cellsByRow.isEmpty()) {
            return new SignatureDateCheckboxFragmentPlan(fragments, appendedChoicesByAnchor);
        }
        for (Map.Entry<Integer, List<PlacedCell>> entry : cellsByRow.entrySet()) {
            int rowIndex = entry.getKey();
            for (PlacedCell cell : entry.getValue()) {
                if (cell == null || !isRecognizedCheckboxChoiceCell(cell.cell())) {
                    continue;
                }
                if (isUnderSignatureDateHeader(cellsByRow, rowIndex, cell.columnIndex())) {
                    fragments.add(new CellRef(rowIndex, cell.columnIndex()));
                    CellRef anchor = findLeftCheckboxChoiceAnchor(cellsByRow, rowIndex, cell.columnIndex());
                    if (anchor != null) {
                        appendUniqueCheckboxChoice(appendedChoicesByAnchor
                                .computeIfAbsent(anchor, ignored -> new ArrayList<>()), cell.cell().getText());
                    }
                }
            }
        }
        return new SignatureDateCheckboxFragmentPlan(fragments, appendedChoicesByAnchor);
    }

    private CellRef findLeftCheckboxChoiceAnchor(Map<Integer, List<PlacedCell>> cellsByRow,
                                                 int rowIndex,
                                                 int columnIndex) {
        PlacedCell best = null;
        for (PlacedCell candidate : cellsByRow.getOrDefault(rowIndex, List.of())) {
            if (candidate == null || candidate.columnIndex() >= columnIndex
                    || !isRecognizedCheckboxChoiceCell(candidate.cell())
                    || isUnderSignatureDateHeader(cellsByRow, rowIndex, candidate.columnIndex())) {
                continue;
            }
            if (best == null || candidate.columnIndex() > best.columnIndex()) {
                best = candidate;
            }
        }
        return best == null ? null : new CellRef(rowIndex, best.columnIndex());
    }

    private void appendUniqueCheckboxChoice(List<String> choices, String rawChoiceText) {
        String normalized = MesProBatchRecordCellRuleSupport.normalizeCheckboxChoiceLabel(rawChoiceText);
        if (normalized.isBlank()) {
            return;
        }
        for (String existing : choices) {
            if (normalized.equals(MesProBatchRecordCellRuleSupport.normalizeCheckboxChoiceLabel(existing))) {
                return;
            }
        }
        choices.add(rawChoiceText);
    }

    private boolean isUnderSignatureDateHeader(Map<Integer, List<PlacedCell>> cellsByRow,
                                               int rowIndex,
                                               int columnIndex) {
        for (int cursor = rowIndex - 1; cursor >= 0; cursor--) {
            List<PlacedCell> upperRow = cellsByRow.getOrDefault(cursor, List.of());
            PlacedCell coveringHeader = findCoveringNonBlankHeaderCell(upperRow, columnIndex);
            if (coveringHeader != null) {
                return isSignatureDateHeaderText(compactText(coveringHeader.cell().getText()));
            }
            if (isInsideSignatureDateHeaderTail(upperRow, columnIndex)) {
                return true;
            }
        }
        return false;
    }

    private PlacedCell findCoveringNonBlankHeaderCell(List<PlacedCell> upperRow, int columnIndex) {
        for (PlacedCell candidate : upperRow) {
            if (candidate == null || !coversColumn(candidate, columnIndex)) {
                continue;
            }
            String text = compactText(candidate.cell().getText());
            if (!text.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isInsideSignatureDateHeaderTail(List<PlacedCell> upperRow, int columnIndex) {
        int resultEndColumn = -1;
        int signatureStartColumn = Integer.MAX_VALUE;
        int signatureEndColumn = -1;
        for (PlacedCell candidate : upperRow) {
            if (candidate == null) {
                continue;
            }
            String text = compactText(candidate.cell().getText());
            if (text.isBlank()) {
                continue;
            }
            int candidateEndColumn = endColumn(candidate);
            if (isChecklistResultHeaderText(text)) {
                resultEndColumn = Math.max(resultEndColumn, candidateEndColumn);
            }
            if (isSignatureDateHeaderText(text)) {
                signatureStartColumn = Math.min(signatureStartColumn, candidate.columnIndex());
                signatureEndColumn = Math.max(signatureEndColumn, candidateEndColumn);
            }
        }
        return resultEndColumn >= 0
                && signatureStartColumn != Integer.MAX_VALUE
                && resultEndColumn < signatureStartColumn
                && columnIndex >= signatureStartColumn
                && columnIndex <= signatureEndColumn + 1;
    }

    private boolean isChecklistResultHeaderText(String compactText) {
        return compactText != null
                && compactText.contains("结果")
                && compactText.length() <= 10;
    }

    private boolean coversColumn(PlacedCell cell, int columnIndex) {
        int startColumn = cell.columnIndex();
        return columnIndex >= startColumn && columnIndex <= endColumn(cell);
    }

    private int endColumn(PlacedCell cell) {
        return cell.columnIndex() + Math.max(1, cell.cell().getColSpan()) - 1;
    }

    private boolean isSignatureDateHeaderText(String compactText) {
        return compactText != null
                && compactText.contains("日期")
                && (compactText.contains("/")
                || compactText.contains("操作人")
                || compactText.contains("复核人")
                || compactText.contains("记录人")
                || compactText.contains("审核人")
                || compactText.contains("确认人")
                || compactText.contains("批准人")
                || compactText.contains("检验人")
                || compactText.contains("生产人员")
                || compactText.contains("签名")
                || compactText.contains("签字"));
    }

    private boolean isStandaloneCheckboxChoiceCell(MesProBatchRecordParsedCell cell,
                                                   List<MesProBatchRecordParsedCell> sourceRow) {
        if (cell == null || sourceRow == null || sourceRow.isEmpty()) {
            return false;
        }
        String ownText = compactText(cell.getText());
        if (ownText.isBlank()) {
            return false;
        }
        for (MesProBatchRecordParsedCell candidate : sourceRow) {
            if (candidate == null || candidate == cell) {
                continue;
            }
            String text = compactText(candidate.getText());
            if (text.isBlank() || text.equals(ownText)) {
                continue;
            }
            if (text.contains(ownText) && text.length() > ownText.length()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldUseVisuallyQuietBlankFillForm(MesProBatchRecordParsedCell cell,
                                                        MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                                        List<MesProBatchRecordParsedCell> sourceRow) {
        if (cell == null || !MesProBatchRecordReportShapeRules.isFillable(cell)
                || !MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText()).isBlank()) {
            return false;
        }
        return rowType == MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA
                || isRepeatedEquipmentMatrixRow(sourceRow);
    }

    private boolean isVisibleSingleLineBlankEntryCell(MesProBatchRecordParsedCell cell,
                                                      List<MesProBatchRecordParsedCell> sourceRow) {
        if (cell == null || sourceRow == null || sourceRow.size() < 3
                || !MesProBatchRecordReportShapeRules.isFillable(cell)
                || !MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText()).isBlank()
                || MesProBatchRecordReportShapeRules.INPUT_TYPE_TEXTAREA.equals(
                MesProBatchRecordReportShapeRules.resolveInputType(cell))) {
            return false;
        }
        int cellIndex = indexOfCellByIdentity(sourceRow, cell);
        if (cellIndex < 2) {
            return false;
        }
        String previousText = compactText(sourceRow.get(cellIndex - 1).getText());
        String previousPreviousText = compactText(sourceRow.get(cellIndex - 2).getText());
        if (!"/".equals(previousPreviousText) || !isMaterialMatrixItemText(previousText)) {
            return false;
        }
        int materialTripletCount = 0;
        for (int index = 2; index < sourceRow.size(); index++) {
            MesProBatchRecordParsedCell candidate = sourceRow.get(index);
            if (candidate == null
                    || !MesProBatchRecordReportShapeRules.isFillable(candidate)
                    || !MesProBatchRecordReportShapeRules.normalizeRecognizedText(candidate.getText()).isBlank()) {
                continue;
            }
            String itemText = compactText(sourceRow.get(index - 1).getText());
            String slashText = compactText(sourceRow.get(index - 2).getText());
            if ("/".equals(slashText) && isMaterialMatrixItemText(itemText)) {
                materialTripletCount++;
            }
        }
        return materialTripletCount >= 1;
    }

    private boolean isSameRowSignatureDateBlankFillCell(MesProBatchRecordParsedCell cell,
                                                        List<MesProBatchRecordParsedCell> sourceRow) {
        if (cell == null || sourceRow == null
                || !MesProBatchRecordReportShapeRules.isFillable(cell)
                || !MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText()).isBlank()) {
            return false;
        }
        int cellIndex = indexOfCellByIdentity(sourceRow, cell);
        if (cellIndex <= 0) {
            return false;
        }
        for (int index = cellIndex - 1; index >= 0; index--) {
            String previousText = compactText(sourceRow.get(index).getText());
            if (previousText.isBlank()) {
                continue;
            }
            return isSignatureDateHeaderText(previousText);
        }
        return false;
    }

    private int indexOfCellByIdentity(List<MesProBatchRecordParsedCell> sourceRow, MesProBatchRecordParsedCell cell) {
        if (sourceRow == null || cell == null) {
            return -1;
        }
        for (int index = 0; index < sourceRow.size(); index++) {
            if (sourceRow.get(index) == cell) {
                return index;
            }
        }
        return -1;
    }

    private boolean isMaterialMatrixItemText(String text) {
        if (text == null || text.isBlank() || "/".equals(text)) {
            return false;
        }
        if (MesProBatchRecordCellRuleSupport.isCheckboxChoiceText(text)) {
            return false;
        }
        return text.length() <= 32 && !MesProBatchRecordReportShapeRules.isNarrativeText(text);
    }

    private int resolveEffectiveColumnCount(MesProBatchRecordParsedTable parsedTable) {
        int declaredColumnCount = Math.max(parsedTable.getColumnCount(), 1);
        if (hasCompleteFixedColumnWidthVector(parsedTable)) {
            return declaredColumnCount;
        }
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        int logicalMaxColumnCount = 0;
        for (int rowIndex = 0; rowIndex < parsedTable.getRows().size(); rowIndex++) {
            int columnIndex = 0;
            int leadingBlockedColumns = 0;
            while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                columnIndex++;
                leadingBlockedColumns++;
            }
            int rowColSpan = parsedTable.getRows().get(rowIndex).stream()
                    .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                    .sum();
            if (leadingBlockedColumns > 0
                    && rowColSpan <= declaredColumnCount
                    && leadingBlockedColumns + rowColSpan > declaredColumnCount) {
                logicalMaxColumnCount = Math.max(logicalMaxColumnCount, declaredColumnCount);
            }
            for (MesProBatchRecordParsedCell cell : parsedTable.getRows().get(rowIndex)) {
                if (cell.getColumnIndex() != null) {
                    columnIndex = Math.max(0, cell.getColumnIndex());
                }
                while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                    columnIndex++;
                }
                int colSpan = Math.max(1, cell.getColSpan());
                logicalMaxColumnCount = Math.max(logicalMaxColumnCount, columnIndex + colSpan);
                if (cell.getRowSpan() > 1) {
                    for (int offset = 0; offset < colSpan; offset++) {
                        blockedUntilRowByColumn.put(columnIndex + offset, rowIndex + cell.getRowSpan() - 1);
                    }
                }
                columnIndex += colSpan;
            }
        }
        return Math.max(declaredColumnCount, logicalMaxColumnCount);
    }

    private boolean hasCompleteFixedColumnWidthVector(MesProBatchRecordParsedTable parsedTable) {
        return parsedTable != null
                && parsedTable.getColumnWidths() != null
                && parsedTable.getColumnWidths().size() == Math.max(parsedTable.getColumnCount(), 1);
    }

    private boolean containsSummaryKeyword(List<MesProBatchRecordParsedCell> sourceRow) {
        if (sourceRow == null) {
            return false;
        }
        for (MesProBatchRecordParsedCell cell : sourceRow) {
            String text = compactText(cell == null ? null : cell.getText());
            if (text.contains("汇总") || text.contains("合计") || text.contains("总计") || text.contains("小计")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStructuredBlankEntryCues(List<MesProBatchRecordParsedCell> sourceRow) {
        if (sourceRow == null || sourceRow.isEmpty()) {
            return false;
        }
        int blankCells = 0;
        int nonBlankCells = 0;
        int slashOnlyCells = 0;
        int shortLabelCells = 0;
        int compactCodeCells = 0;
        for (MesProBatchRecordParsedCell cell : sourceRow) {
            String text = compactText(cell == null ? null : cell.getText());
            if (text.isEmpty()) {
                blankCells++;
                continue;
            }
            if ("/".equals(text)) {
                slashOnlyCells++;
                continue;
            }
            nonBlankCells++;
            if (looksLikeShortStructuredToken(text)) {
                shortLabelCells++;
            }
            if (looksLikeCompactStructuredCode(text)) {
                compactCodeCells++;
            }
        }
        if (blankCells == 0 || nonBlankCells == 0) {
            return false;
        }
        return shortLabelCells >= Math.max(2, nonBlankCells - compactCodeCells)
                && nonBlankCells > slashOnlyCells;
    }

    private boolean looksLikeShortStructuredToken(String text) {
        return text != null
                && !text.isBlank()
                && text.length() <= 12
                && !MesProBatchRecordReportShapeRules.isNarrativeText(text);
    }

    private boolean looksLikeCompactStructuredCode(String text) {
        if (text == null || text.isBlank() || text.length() > 20) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            hasLetter = hasLetter || Character.isLetter(codePoint);
            hasDigit = hasDigit || Character.isDigit(codePoint);
            offset += Character.charCount(codePoint);
        }
        return hasLetter && hasDigit;
    }

    private PageDecorationPlan resolvePageDecorationPlan(MesProBatchRecordParsedTable parsedTable) {
        if (parsedTable == null || parsedTable.getRows() == null || parsedTable.getRows().size() < 2) {
            return PageDecorationPlan.none();
        }
        List<List<MesProBatchRecordParsedCell>> rows = parsedTable.getRows();
        int columnCount = Math.max(parsedTable.getColumnCount(), 1);
        if (!isDocumentHeaderBlockStart(rows, 0, columnCount)) {
            return PageDecorationPlan.none();
        }
        return new PageDecorationPlan(true, List.of(), List.of());
    }

    private List<Integer> resolveRepeatedEquipmentMatrixBreakRows(List<List<MesProBatchRecordParsedCell>> rows) {
        List<Integer> equipmentMatrixRows = new ArrayList<>();
        for (int rowIndex = 2; rowIndex < rows.size(); rowIndex++) {
            if (isRepeatedEquipmentMatrixRow(rows.get(rowIndex))) {
                equipmentMatrixRows.add(rowIndex);
            }
        }
        if (equipmentMatrixRows.size() < 8) {
            return List.of();
        }
        int lastMatrixRowIndex = equipmentMatrixRows.get(equipmentMatrixRows.size() - 1);
        if (shouldKeepCompactStructuredTailWithRepeatedMatrix(rows, lastMatrixRowIndex + 1)) {
            int matrixBreakStart = resolveBreakStartInsideDominantEquipmentMatrixBand(equipmentMatrixRows);
            return matrixBreakStart >= 0 ? List.of(matrixBreakStart) : List.of();
        }
        int midpointRowIndex = equipmentMatrixRows.get(equipmentMatrixRows.size() / 2);
        int preferredBreakStart = resolveBreakStartAfterDominantEquipmentMatrixBand(rows, equipmentMatrixRows, midpointRowIndex);
        if (preferredBreakStart >= 0) {
            return List.of(preferredBreakStart);
        }
        return List.of(midpointRowIndex);
    }

    private int resolveBreakStartInsideDominantEquipmentMatrixBand(List<Integer> equipmentMatrixRows) {
        int midpointRowIndex = equipmentMatrixRows.get(equipmentMatrixRows.size() / 2);
        RowBand dominantBand = resolveRowBandContaining(equipmentMatrixRows, midpointRowIndex);
        if (dominantBand == null || dominantBand.rowCount() < 8) {
            return -1;
        }
        int breakStart = dominantBand.startRowIndex() + dominantBand.rowCount() / 2;
        int rowsBeforeBreak = breakStart - dominantBand.startRowIndex();
        int rowsAfterBreak = dominantBand.endRowIndex() - breakStart + 1;
        if (rowsBeforeBreak < 3 || rowsAfterBreak < 3) {
            return -1;
        }
        return breakStart;
    }

    private int resolveBreakStartAfterDominantEquipmentMatrixBand(List<List<MesProBatchRecordParsedCell>> rows,
                                                                  List<Integer> equipmentMatrixRows,
                                                                  int midpointRowIndex) {
        RowBand dominantBand = resolveRowBandContaining(equipmentMatrixRows, midpointRowIndex);
        if (dominantBand == null) {
            return -1;
        }
        if (dominantBand.rowCount() < Math.max(4, (equipmentMatrixRows.size() + 1) / 2)) {
            return -1;
        }
        int breakStart = dominantBand.endRowIndex() + 1;
        if (breakStart >= rows.size()) {
            return -1;
        }
        if (shouldKeepCompactStructuredTailWithRepeatedMatrix(rows, breakStart)) {
            return -1;
        }
        return breakStart;
    }

    private boolean shouldKeepCompactStructuredTailWithRepeatedMatrix(List<List<MesProBatchRecordParsedCell>> rows,
                                                                      int breakStart) {
        int remainingRowCount = rows.size() - breakStart;
        if (remainingRowCount <= 0 || remainingRowCount > 14) {
            return false;
        }
        for (int rowIndex = breakStart; rowIndex < rows.size(); rowIndex++) {
            if (containsSummaryKeyword(rows.get(rowIndex))) {
                return true;
            }
        }
        return false;
    }

    private RowBand resolveRowBandContaining(List<Integer> rowIndexes, int targetRowIndex) {
        if (rowIndexes == null || rowIndexes.isEmpty()) {
            return null;
        }
        int bandStart = rowIndexes.get(0);
        int bandEnd = bandStart;
        int bandCount = 1;
        for (int index = 1; index < rowIndexes.size(); index++) {
            int currentRowIndex = rowIndexes.get(index);
            if (currentRowIndex == bandEnd + 1) {
                bandEnd = currentRowIndex;
                bandCount++;
            } else {
                if (targetRowIndex >= bandStart && targetRowIndex <= bandEnd) {
                    return new RowBand(bandStart, bandEnd, bandCount);
                }
                bandStart = currentRowIndex;
                bandEnd = currentRowIndex;
                bandCount = 1;
            }
        }
        if (targetRowIndex >= bandStart && targetRowIndex <= bandEnd) {
            return new RowBand(bandStart, bandEnd, bandCount);
        }
        return null;
    }

    private boolean isRepeatedEquipmentMatrixRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        int nonBlankCells = 0;
        int blankCells = 0;
        int checklistChoiceCells = 0;
        int shortStructuredCells = 0;
        int longStructuredCells = 0;
        int totalColSpan = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null) {
                continue;
            }
            totalColSpan += Math.max(1, cell.getColSpan());
            String text = compactText(cell.getText());
            if (text.isBlank() || cell.isVisualBlank()) {
                blankCells++;
                continue;
            }
            nonBlankCells++;
            if (containsBinaryChoiceGlyphPair(text)) {
                checklistChoiceCells++;
            }
            if (looksLikeShortStructuredToken(text)) {
                shortStructuredCells++;
            } else if (!MesProBatchRecordReportShapeRules.isNarrativeText(text)) {
                longStructuredCells++;
            }
        }
        return row.size() >= 5
                && totalColSpan >= Math.max(10, row.size())
                && nonBlankCells >= 3
                && blankCells >= 1
                && checklistChoiceCells >= 1
                && shortStructuredCells + longStructuredCells >= 3;
    }

    private boolean containsBinaryChoiceGlyphPair(String text) {
        return MesProBatchRecordCellRuleSupport.containsAtLeastTwoCheckboxMarkers(text);
    }

    private boolean isDocumentHeaderBlockStart(List<List<MesProBatchRecordParsedCell>> rows, int startRowIndex,
                                               int columnCount) {
        if (rows == null || startRowIndex < 0 || startRowIndex + 1 >= rows.size()) {
            return false;
        }
        List<MesProBatchRecordParsedCell> firstRow = rows.get(startRowIndex);
        List<MesProBatchRecordParsedCell> secondRow = rows.get(startRowIndex + 1);
        if (firstRow == null || secondRow == null || firstRow.size() != 3 || secondRow.size() != 2) {
            return false;
        }
        MesProBatchRecordParsedCell firstCell = firstRow.get(0);
        if (firstCell == null || firstCell.getRowSpan() < 2 || !firstCell.isBold()) {
            return false;
        }
        int firstRowColSpan = firstRow.stream().mapToInt(cell -> Math.max(cell.getColSpan(), 1)).sum();
        int secondRowColSpan = secondRow.stream().mapToInt(cell -> Math.max(cell.getColSpan(), 1)).sum();
        return firstRowColSpan == columnCount
                && secondRowColSpan > 0
                && secondRowColSpan == firstRowColSpan - Math.max(firstCell.getColSpan(), 1)
                && firstRow.stream().allMatch(cell -> cell != null && cell.isBold())
                && secondRow.stream().allMatch(cell -> cell != null && cell.isBold());
    }

    private DecoratedRowsResult applyPageDecorations(JSONObject rowsObject,
                                                     List<String> merges,
                                                     MesProBatchRecordParsedTable parsedTable,
                                                     JSONArray styles,
                                                     Map<String, Integer> styleIndexes,
                                                     String reportCode,
                                                     int dataRectWidth,
                                                     PageDecorationPlan decorationPlan,
                                                     List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                                     Map<Integer, Integer> rowHeightFloors) {
        int originalRowCount = Math.max(parsedTable.getRowCount(), parsedTable.getRows().size());
        if (!decorationPlan.hasDocumentHeader()
                && decorationPlan.continuationHeaderStarts().isEmpty()
                && decorationPlan.continuationBreakStarts().isEmpty()) {
            return new DecoratedRowsResult(rowsObject, merges, originalRowCount, new JSONArray(), new JSONArray());
        }
        JSONObject decoratedRows = new JSONObject(true);
        Map<Integer, Integer> renderedRowIndexes = new HashMap<>();
        int renderedRowIndex = 0;
        JSONArray fixedPrintHeadRows = new JSONArray();
        JSONArray fixedPrintTailRows = new JSONArray();
        List<String> decoratedMerges = new ArrayList<>();
        Set<Integer> continuationTableHeaderRows = new HashSet<>();
        if (decorationPlan.hasDocumentHeader()) {
            fixedPrintHeadRows.add(buildRangeDescriptor(
                    renderedRowIndex, 0, renderedRowIndex + 1, parsedTable.getColumnCount() - 1));
        }
        for (int sourceRowIndex = 0; sourceRowIndex < originalRowCount; sourceRowIndex++) {
            if (decorationPlan.continuationBreakStarts().contains(sourceRowIndex)) {
                int spacerRowIndex = renderedRowIndex;
                appendSpacerRow(decoratedRows, renderedRowIndex++, parsedTable.getColumnCount(), dataRectWidth,
                        MesProBatchRecordReportShapeRules.CONTINUATION_PAGE_BREAK_SPACER_HEIGHT_PX, styles,
                        styleIndexes, reportCode);
                JSONObject spacerRow = decoratedRows.getJSONObject(String.valueOf(spacerRowIndex));
                if (spacerRow != null) {
                    spacerRow.put("pagingRow", true);
                }
                if (parsedTable.getColumnCount() > 1) {
                    decoratedMerges.add(toMergeRange(spacerRowIndex, 0, spacerRowIndex, parsedTable.getColumnCount() - 1));
                }
                int repeatedHeaderSourceRowIndex = resolveContinuationTableHeaderSourceRowIndex(
                        parsedTable.getRows(), rowTypes, sourceRowIndex);
                if (repeatedHeaderSourceRowIndex >= 0) {
                    JSONObject repeatedHeaderRow = cloneRow(rowsObject.getJSONObject(String.valueOf(repeatedHeaderSourceRowIndex)));
                    if (repeatedHeaderRow != null) {
                        continuationTableHeaderRows.add(renderedRowIndex);
                        decoratedRows.put(String.valueOf(renderedRowIndex++), repeatedHeaderRow);
                    }
                }
            }
            if (decorationPlan.continuationHeaderStarts().contains(sourceRowIndex)) {
                int spacerRowIndex = renderedRowIndex;
                appendSpacerRow(decoratedRows, renderedRowIndex++, parsedTable.getColumnCount(), dataRectWidth,
                        MesProBatchRecordReportShapeRules.CONTINUATION_PAGE_BREAK_SPACER_HEIGHT_PX, styles,
                        styleIndexes, reportCode);
                JSONObject spacerRow = decoratedRows.getJSONObject(String.valueOf(spacerRowIndex));
                if (spacerRow != null) {
                    spacerRow.put("pagingRow", true);
                }
                if (parsedTable.getColumnCount() > 1) {
                    decoratedMerges.add(toMergeRange(spacerRowIndex, 0, spacerRowIndex, parsedTable.getColumnCount() - 1));
                }
            }
            JSONObject existingRow = rowsObject.getJSONObject(String.valueOf(sourceRowIndex));
            if (existingRow == null) {
                continue;
            }
            renderedRowIndexes.put(sourceRowIndex, renderedRowIndex);
            decoratedRows.put(String.valueOf(renderedRowIndex), existingRow);
            renderedRowIndex++;
        }
        if (decorationPlan.hasDocumentHeader()) {
            fixedPrintTailRows = buildFixedPrintTailRows(rowTypes, renderedRowIndexes, parsedTable.getColumnCount());
        }
        decoratedMerges.addAll(shiftMergeRows(merges, renderedRowIndexes));
        reclaimDocumentHeaderTopSpacerBudget(decoratedRows, renderedRowIndexes, rowTypes, rowHeightFloors);
        splitVerticalMergesAcrossPagingRows(decoratedRows, continuationTableHeaderRows);
        return new DecoratedRowsResult(decoratedRows, collectMergeRangesFromRows(decoratedRows), renderedRowIndex,
                fixedPrintHeadRows, fixedPrintTailRows);
    }

    private JSONArray buildFixedPrintTailRows(List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                              Map<Integer, Integer> renderedRowIndexes,
                                              int columnCount) {
        JSONArray fixedPrintTailRows = new JSONArray();
        if (rowTypes == null || rowTypes.isEmpty() || renderedRowIndexes == null || renderedRowIndexes.isEmpty()) {
            return fixedPrintTailRows;
        }
        for (int sourceRowIndex = 0; sourceRowIndex < rowTypes.size(); sourceRowIndex++) {
            if (rowTypeAt(rowTypes, sourceRowIndex) != MesProBatchRecordSharedRowTypeRules.RowType.FOOTER) {
                continue;
            }
            Integer renderedRowIndex = renderedRowIndexes.get(sourceRowIndex);
            if (renderedRowIndex == null) {
                continue;
            }
            fixedPrintTailRows.add(buildRangeDescriptor(renderedRowIndex, 0,
                    renderedRowIndex, Math.max(columnCount, 1) - 1));
        }
        return fixedPrintTailRows;
    }

    private int resolveContinuationTableHeaderSourceRowIndex(List<List<MesProBatchRecordParsedCell>> rows,
                                                             List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                                             int continuationSourceRowIndex) {
        if (!shouldContinueDetailBandAfterPaging(rows, rowTypes, continuationSourceRowIndex)) {
            return -1;
        }
        int rowIndex = continuationSourceRowIndex - 1;
        while (isContinuationDetailBandRow(rows, rowTypes, rowIndex)) {
            rowIndex--;
        }
        if (rowIndex < 0 || rowTypeAt(rowTypes, rowIndex) != MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER) {
            return -1;
        }
        return isReusableContinuationTableHeader(rows.get(rowIndex)) ? rowIndex : -1;
    }

    private boolean shouldContinueDetailBandAfterPaging(List<List<MesProBatchRecordParsedCell>> rows,
                                                        List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                                        int continuationSourceRowIndex) {
        if (rows == null || continuationSourceRowIndex < 0 || continuationSourceRowIndex >= rows.size()) {
            return false;
        }
        MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, continuationSourceRowIndex);
        return rowType == MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA
                || isRepeatedEquipmentMatrixRow(rows.get(continuationSourceRowIndex));
    }

    private boolean isContinuationDetailBandRow(List<List<MesProBatchRecordParsedCell>> rows,
                                                List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                                int rowIndex) {
        if (rows == null || rowIndex < 0 || rowIndex >= rows.size()) {
            return false;
        }
        return rowTypeAt(rowTypes, rowIndex) == MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA
                || isRepeatedEquipmentMatrixRow(rows.get(rowIndex));
    }

    private boolean isReusableContinuationTableHeader(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty() || isRepeatedEquipmentMatrixRow(row)) {
            return false;
        }
        int nonBlankCells = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell == null) {
                continue;
            }
            if (Math.max(cell.getRowSpan(), 1) > 1 || Math.max(cell.getColSpan(), 1) > 1) {
                return false;
            }
            String text = compactText(cell.getText());
            if (!text.isBlank()) {
                nonBlankCells++;
            }
        }
        return nonBlankCells >= 3;
    }

    private JSONObject cloneRow(JSONObject row) {
        return row == null ? null : JSON.parseObject(row.toJSONString(), JSONObject.class);
    }

    private int resolveDocumentHeaderTopSpacerHeight() {
        return MesProBatchRecordReportShapeRules.DOC_HEADER_TOP_SPACER_HEIGHT_PX;
    }

    private boolean shouldPreferSourceRowHeight(List<MesProBatchRecordParsedCell> sourceRow,
                                                boolean authoritativeSourceGrid,
                                                boolean sourceBackedColumnWidths,
                                                int effectiveColumnCount) {
        int sourceRowHeight = resolveSourceRowHeight(sourceRow);
        if (sourceRow.isEmpty()
                || sourceRowHeight < MesProBatchRecordReportShapeRules.MIN_ROW_HEIGHT_PX) {
            return false;
        }
        if (authoritativeSourceGrid) {
            return true;
        }
        if (sourceRowHeight > MesProBatchRecordReportShapeRules.MAX_PRESERVED_ROW_HEIGHT_PX) {
            return false;
        }
        if (sourceBackedColumnWidths
                && effectiveColumnCount > MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_MEDIUM_COLUMN_COUNT) {
            return true;
        }
        if (containsChecklistNarrativeBandShape(sourceRow)) {
            return true;
        }
        if (!sourceBackedColumnWidths) {
            return false;
        }
        return hasCredibleSingleLineSourceHeight(sourceRow, sourceRowHeight);
    }

    private boolean hasCredibleSingleLineSourceHeight(List<MesProBatchRecordParsedCell> sourceRow, int sourceRowHeight) {
        if (sourceRow == null
                || sourceRowHeight < MesProBatchRecordReportShapeRules.MIN_ROW_HEIGHT_PX
                || sourceRowHeight > MesProBatchRecordReportShapeRules.MAX_ROW_HEIGHT_PX) {
            return false;
        }
        boolean hasText = false;
        int totalTextLength = 0;
        int narrativeCellCount = 0;
        for (MesProBatchRecordParsedCell cell : sourceRow) {
            if (cell == null || cell.isFillable()) {
                continue;
            }
            String text = compactText(cell.getText());
            if (cell.getText() != null && cell.getText().contains("\n")
                    && !isShortWrappedSourceCell(cell.getText())) {
                return false;
            }
            if (!text.isBlank()) {
                hasText = true;
                totalTextLength += text.length();
                if (MesProBatchRecordReportShapeRules.isNarrativeText(text)) {
                    narrativeCellCount++;
                }
            }
        }
        return hasText && totalTextLength <= 180 && narrativeCellCount <= 1;
    }

    private boolean isShortWrappedSourceCell(String text) {
        String compact = compactText(text);
        if (compact.isBlank() || compact.length() > 12) {
            return false;
        }
        return !compact.contains("。")
                && !compact.contains("，")
                && !compact.contains("；")
                && !compact.contains("：");
    }

    private boolean containsChecklistNarrativeBandShape(List<MesProBatchRecordParsedCell> sourceRow) {
        if (sourceRow == null || sourceRow.isEmpty()) {
            return false;
        }
        int narrativeCellCount = 0;
        int narrativeSpan = 0;
        boolean hasChecklistTail = false;
        for (MesProBatchRecordParsedCell cell : sourceRow) {
            if (cell == null) {
                continue;
            }
            String text = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
            if (MesProBatchRecordReportShapeRules.isNarrativeText(text)) {
                narrativeCellCount++;
                narrativeSpan += Math.max(1, cell.getColSpan());
            }
            if (text.contains("□符合要求") || text.contains("□不符合要求")
                    || text.contains("操作人/日期") || text.contains("复核人/日期")
                    || text.contains("结果")) {
                hasChecklistTail = true;
            }
        }
        return hasChecklistTail && (narrativeSpan >= 3 || narrativeCellCount >= 2);
    }

    private int resolveSourceRowHeight(List<MesProBatchRecordParsedCell> sourceRow) {
        return sourceRow.stream()
                .mapToInt(MesProBatchRecordParsedCell::getHeightPx)
                .max()
                .orElse(0);
    }

    private void reclaimDocumentHeaderTopSpacerBudget(
            JSONObject decoratedRows,
            Map<Integer, Integer> renderedRowIndexes,
            List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
            Map<Integer, Integer> rowHeightFloors) {
        int remaining = resolveDocumentHeaderTopSpacerHeight()
                - MesProBatchRecordReportShapeRules.DOC_HEADER_TOP_SPACER_HEIGHT_PX;
        if (remaining <= 0 || renderedRowIndexes.isEmpty()) {
            return;
        }
        while (remaining > 0) {
            boolean changed = false;
            for (int priority = 0; priority <= 6 && remaining > 0; priority++) {
                for (int sourceRowIndex = 0; sourceRowIndex < renderedRowIndexes.size() && remaining > 0; sourceRowIndex++) {
                    if (remaining <= 0) {
                        break;
                    }
                    MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, sourceRowIndex);
                    if (resolveCompressionPriority(rowType) != priority) {
                        continue;
                    }
                    Integer renderedRowIndex = renderedRowIndexes.get(sourceRowIndex);
                    if (renderedRowIndex == null) {
                        continue;
                    }
                    JSONObject rowObject = decoratedRows.getJSONObject(String.valueOf(renderedRowIndex));
                    if (rowObject == null) {
                        continue;
                    }
                    int currentHeight = rowObject.getIntValue("height");
                    int floor = Math.max(
                            resolveCompressionFloor(rowObject.getIntValue("height"), rowType, sourceRowIndex, rowTypes),
                            rowHeightFloors.getOrDefault(sourceRowIndex, 0));
                    if (currentHeight > floor) {
                        rowObject.put("height", currentHeight - 1);
                        remaining--;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                return;
            }
        }
    }

    private void appendSpacerRow(JSONObject rowsObject,
                                 int rowIndex,
                                 int columnCount,
                                 int dataRectWidth,
                                 int rowHeight,
                                 JSONArray styles,
                                 Map<String, Integer> styleIndexes,
                                 String reportCode) {
        JSONObject rowObject = new JSONObject(true);
        JSONObject cellsObject = new JSONObject(true);
        MesProBatchRecordParsedCell spacerCell = MesProBatchRecordParsedCell.builder()
                .text(MesProBatchRecordReportShapeRules.normalizePaddingText())
                .colSpan(Math.max(columnCount, 1))
                .widthPx(Math.max(dataRectWidth, MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(Math.max(columnCount, 1))))
                .heightPx(rowHeight)
                .horizontalAlign("left")
                .verticalAlign(MesProBatchRecordReportShapeRules.DEFAULT_VERTICAL_ALIGN)
                .borderless(true)
                .visualBlank(true)
                .build();
        JSONObject spacerCellObject = buildCellObject(spacerCell, styles, styleIndexes, reportCode,
                0, 0, 1, Math.max(columnCount, 1), List.of(), false, spacerCell.getWidthPx(), false,
                MesProBatchRecordSharedRowTypeRules.RowType.FOOTER, SequenceNumberColumnPlan.none(), false, List.of());
        if (columnCount > 1) {
            spacerCellObject.put("merge", List.of(0, columnCount - 1));
        }
        cellsObject.put("0", spacerCellObject);
        rowObject.put("cells", cellsObject);
        rowObject.put("height", rowHeight);
        rowsObject.put(String.valueOf(rowIndex), rowObject);
    }

    private void splitVerticalMergesAcrossPagingRows(JSONObject rowsObject, Set<Integer> continuationTableHeaderRows) {
        if (rowsObject == null || rowsObject.isEmpty()) {
            return;
        }
        List<Integer> rowIndexes = rowsObject.keySet().stream()
                .filter(key -> key.chars().allMatch(Character::isDigit))
                .map(Integer::parseInt)
                .sorted()
                .toList();
        for (Integer pagingRowIndex : rowIndexes) {
            JSONObject pagingRow = rowsObject.getJSONObject(String.valueOf(pagingRowIndex));
            if (pagingRow == null || !pagingRow.getBooleanValue("pagingRow")) {
                continue;
            }
            int continuationRowIndex = resolveMergeContinuationRowIndex(rowsObject, pagingRowIndex + 1,
                    continuationTableHeaderRows);
            JSONObject continuationRow = rowsObject.getJSONObject(String.valueOf(continuationRowIndex));
            if (continuationRow == null) {
                continue;
            }
            JSONObject continuationCells = continuationRow.getJSONObject("cells");
            if (continuationCells == null) {
                continuationCells = new JSONObject(true);
                continuationRow.put("cells", continuationCells);
            }
            for (Integer startRowIndex : rowIndexes) {
                if (startRowIndex >= pagingRowIndex) {
                    break;
                }
                JSONObject row = rowsObject.getJSONObject(String.valueOf(startRowIndex));
                if (row == null) {
                    continue;
                }
                JSONObject cells = row.getJSONObject("cells");
                if (cells == null) {
                    continue;
                }
                for (String cellKey : new ArrayList<>(cells.keySet())) {
                    JSONObject cell = cells.getJSONObject(cellKey);
                    if (cell == null || !cell.containsKey("merge")) {
                        continue;
                    }
                    JSONArray merge = cell.getJSONArray("merge");
                    int rowSpan = merge.getIntValue(0) + 1;
                    int colSpan = merge.getIntValue(1) + 1;
                    int endRowIndex = startRowIndex + rowSpan - 1;
                    if (!(startRowIndex < pagingRowIndex && endRowIndex >= pagingRowIndex)) {
                        continue;
                    }
                    int firstSegmentRowSpan = pagingRowIndex - startRowIndex;
                    int secondSegmentRowSpan = rowSpan - firstSegmentRowSpan;
                    if (firstSegmentRowSpan <= 0 || secondSegmentRowSpan <= 0) {
                        continue;
                    }
                    setMerge(cell, firstSegmentRowSpan, colSpan);
                    JSONObject continuationCell = JSON.parseObject(cell.toJSONString(), JSONObject.class);
                    setMerge(continuationCell, secondSegmentRowSpan, colSpan);
                    continuationCells.putIfAbsent(cellKey, continuationCell);
                }
            }
        }
    }

    private int resolveMergeContinuationRowIndex(JSONObject rowsObject,
                                                 int startRowIndex,
                                                 Set<Integer> continuationTableHeaderRows) {
        int rowIndex = startRowIndex;
        while (continuationTableHeaderRows != null && continuationTableHeaderRows.contains(rowIndex)
                && rowsObject.getJSONObject(String.valueOf(rowIndex + 1)) != null) {
            rowIndex++;
        }
        return rowIndex;
    }

    private void setMerge(JSONObject cell, int rowSpan, int colSpan) {
        if (rowSpan <= 1 && colSpan <= 1) {
            cell.remove("merge");
            return;
        }
        cell.put("merge", List.of(Math.max(0, rowSpan - 1), Math.max(0, colSpan - 1)));
    }

    private List<String> collectMergeRangesFromRows(JSONObject rowsObject) {
        List<String> merges = new ArrayList<>();
        if (rowsObject == null) {
            return merges;
        }
        List<Integer> rowIndexes = rowsObject.keySet().stream()
                .filter(key -> key.chars().allMatch(Character::isDigit))
                .map(Integer::parseInt)
                .sorted()
                .toList();
        for (Integer rowIndex : rowIndexes) {
            JSONObject row = rowsObject.getJSONObject(String.valueOf(rowIndex));
            if (row == null) {
                continue;
            }
            JSONObject cells = row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String cellKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(cellKey);
                if (cell == null || !cell.containsKey("merge")) {
                    continue;
                }
                JSONArray merge = cell.getJSONArray("merge");
                int columnIndex = Integer.parseInt(cellKey);
                int rowSpan = merge.getIntValue(0) + 1;
                int colSpan = merge.getIntValue(1) + 1;
                merges.add(toMergeRange(rowIndex, columnIndex,
                        rowIndex + rowSpan - 1,
                        columnIndex + colSpan - 1));
            }
        }
        return merges;
    }

    private JSONObject buildRangeDescriptor(int sri, int sci, int eri, int eci) {
        JSONObject range = new JSONObject(true);
        range.put("sri", sri);
        range.put("sci", sci);
        range.put("eri", eri);
        range.put("eci", eci);
        return range;
    }

    private List<String> shiftMergeRows(List<String> merges,
                                        Map<Integer, Integer> renderedRowIndexes) {
        List<String> shiftedMerges = new ArrayList<>();
        for (String merge : merges) {
            MergeRange range = parseMergeRange(merge);
            Integer renderedStartRow = renderedRowIndexes.get(range.startRow());
            Integer renderedEndRow = renderedRowIndexes.get(range.endRow());
            if (renderedStartRow == null || renderedEndRow == null) {
                continue;
            }
            shiftedMerges.add(toMergeRange(renderedStartRow, range.startColumn(), renderedEndRow, range.endColumn()));
        }
        return shiftedMerges;
    }

    private MergeRange parseMergeRange(String mergeRange) {
        String[] refs = mergeRange.split(":");
        if (refs.length != 2) {
            throw new IllegalArgumentException("Invalid merge range: " + mergeRange);
        }
        CellRef start = parseCellRef(refs[0]);
        CellRef end = parseCellRef(refs[1]);
        return new MergeRange(start.rowIndex(), start.columnIndex(), end.rowIndex(), end.columnIndex());
    }

    private CellRef parseCellRef(String ref) {
        int splitIndex = 0;
        while (splitIndex < ref.length() && Character.isLetter(ref.charAt(splitIndex))) {
            splitIndex++;
        }
        String columnName = ref.substring(0, splitIndex).toUpperCase();
        String rowPart = ref.substring(splitIndex);
        int columnIndex = 0;
        for (int index = 0; index < columnName.length(); index++) {
            columnIndex = columnIndex * 26 + (columnName.charAt(index) - 'A' + 1);
        }
        return new CellRef(Integer.parseInt(rowPart) - 1, columnIndex - 1);
    }

    private void shrinkRowsToSinglePage(JSONObject rowsObject, int rowCount, int columnCount,
                                        List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                        Map<Integer, Integer> rowHeightFloors) {
        int targetHeight = resolveTargetSinglePageHeight(rowCount, columnCount, rowTypes);
        int totalHeight = sumRowHeights(rowsObject, rowCount);
        while (totalHeight > targetHeight) {
            boolean changed = false;
            for (int priority = 0; priority <= 6
                    && totalHeight > targetHeight; priority++) {
                for (int rowIndex = 0; rowIndex < rowCount
                        && totalHeight > targetHeight; rowIndex++) {
                    JSONObject rowObject = rowsObject.getJSONObject(String.valueOf(rowIndex));
                    if (rowObject == null) {
                        continue;
                    }
                    MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, rowIndex);
                    if (resolveCompressionPriority(rowType) != priority) {
                        continue;
                    }
                    int currentHeight = rowObject.getIntValue("height");
                    int floor = Math.max(
                            resolveCompressionFloor(currentHeight, rowType, rowIndex, rowTypes),
                            rowHeightFloors.getOrDefault(rowIndex, 0));
                    if (currentHeight > floor) {
                        rowObject.put("height", currentHeight - 1);
                        totalHeight--;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                return;
            }
        }
    }

    private void expandRowsToSinglePageMinimum(JSONObject rowsObject, int rowCount, int columnCount,
                                               List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                               Map<Integer, Integer> rowHeightFloors) {
        int targetHeight = resolveTargetSinglePageHeight(rowCount, columnCount, rowTypes);
        if (targetHeight <= MesProBatchRecordReportShapeRules.SINGLE_PAGE_MAX_HEIGHT_PX) {
            return;
        }
        if (columnCount > MesProBatchRecordReportShapeRules.SHARED_PAGE_WIDTH_NARROW_COLUMN_COUNT) {
            return;
        }
        int totalHeight = sumRowHeights(rowsObject, rowCount);
        while (totalHeight < targetHeight) {
            boolean changed = false;
            for (int rowIndex = 0; rowIndex < rowCount && totalHeight < targetHeight; rowIndex++) {
                JSONObject rowObject = rowsObject.getJSONObject(String.valueOf(rowIndex));
                if (rowObject == null) {
                    continue;
                }
                if (isLockedAtSourceHeight(rowObject, rowIndex, rowHeightFloors)) {
                    continue;
                }
                MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypeAt(rowTypes, rowIndex);
                if (!shouldGrowForSinglePageMinimum(rowType, rowIndex, rowTypes)) {
                    continue;
                }
                int currentHeight = rowObject.getIntValue("height");
                if (currentHeight >= MesProBatchRecordReportShapeRules.MAX_PRESERVED_ROW_HEIGHT_PX) {
                    continue;
                }
                rowObject.put("height", currentHeight + 1);
                totalHeight++;
                changed = true;
            }
            if (!changed) {
                return;
            }
        }
    }

    private boolean shouldGrowForSinglePageMinimum(MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                                   int rowIndex,
                                                   List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        if (isProtectedOperationBandBodyRow(rowIndex, rowTypes)) {
            return true;
        }
        return rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.FIELD;
    }

    private boolean isLockedAtSourceHeight(JSONObject rowObject,
                                           int rowIndex,
                                           Map<Integer, Integer> rowHeightFloors) {
        Integer floor = rowHeightFloors.get(rowIndex);
        return floor != null && rowObject.getIntValue("height") <= floor;
    }

    private int resolveTargetSinglePageHeight(int rowCount, int columnCount,
                                              List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        return MesProBatchRecordReportShapeRules.resolveSinglePageTargetHeight(rowCount, columnCount, rowTypes);
    }

    private boolean isLiveLikeDenseProcessPage(int rowCount, int columnCount,
                                               List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        if (rowTypes == null || columnCount < 15 || rowCount < 20) {
            return false;
        }
        long titleRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.TITLE);
        long fieldRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FIELD);
        long longDescriptionRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION);
        long summaryRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY);
        long footerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FOOTER);
        return summaryRows >= 1
                && footerRows >= 1
                && fieldRows >= 2
                && (longDescriptionRows >= 1 || titleRows >= 2);
    }

    private boolean isLiveLikeMediumProcessPage(int rowCount, int columnCount,
                                                List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        if (rowTypes == null || columnCount < 9 || columnCount > 14 || rowCount < 22) {
            return false;
        }
        long titleRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.TITLE);
        long fieldRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FIELD);
        long longDescriptionRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION);
        long summaryRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY);
        long footerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FOOTER);
        return summaryRows >= 1
                && footerRows >= 1
                && fieldRows >= 2
                && (longDescriptionRows >= 1 || titleRows >= 1);
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

    private int sumRowHeights(JSONObject rowsObject, int rowCount) {
        int totalHeight = 0;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            JSONObject rowObject = rowsObject.getJSONObject(String.valueOf(rowIndex));
            if (rowObject != null) {
                totalHeight += rowObject.getIntValue("height");
            }
        }
        return totalHeight;
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
        int horizontalPadding = narrative
                ? MesProBatchRecordReportShapeRules.NARRATIVE_CELL_HORIZONTAL_PADDING
                : MesProBatchRecordReportShapeRules.ESTIMATED_CELL_HORIZONTAL_PADDING;
        int availableWidth = Math.max(effectiveWidth - horizontalPadding, MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
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

    private boolean shouldAllowJsonHeightGrowth(MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                                 String visibleText) {
        if (visibleText != null && visibleText.contains("\n")) {
            return true;
        }
        return rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION
                || MesProBatchRecordReportShapeRules.isNarrativeText(visibleText);
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
        int structuredTailRows = 0;
        int tailFieldRows = 0;
        int tailNarrativeRows = 0;
        for (int index = lastSummaryIndex + 1; index < footerIndex; index++) {
            MesProBatchRecordSharedRowTypeRules.RowType rowType = rowTypes.get(index);
            tailRows++;
            if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER) {
                structuredTailRows++;
            } else if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.FIELD) {
                structuredTailRows++;
                tailFieldRows++;
            } else if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION) {
                structuredTailRows++;
                tailNarrativeRows++;
            }
        }
        return tailRows >= 3
                && structuredTailRows >= 3
                && (tailFieldRows + tailNarrativeRows) >= 1;
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

    private int resolveCompressionFloor(int currentHeight,
                                        MesProBatchRecordSharedRowTypeRules.RowType rowType,
                                        int rowIndex,
                                        List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        int baseFloor = MesProBatchRecordReportShapeRules.resolveRowHeightFloor(rowType, 1);
        int protectedOperationBandFloor = resolveProtectedOperationBandRowHeightFloor(rowIndex, rowTypes);
        if (protectedOperationBandFloor > 0) {
            return Math.max(baseFloor, protectedOperationBandFloor);
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY) {
            return Math.max(baseFloor, 24);
        }
        if (currentHeight <= MesProBatchRecordReportShapeRules.MAX_ROW_HEIGHT_PX) {
            return baseFloor;
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION) {
            return Math.max(baseFloor, Math.min(currentHeight, 84));
        }
        if (rowType == MesProBatchRecordSharedRowTypeRules.RowType.FIELD
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY
                || rowType == MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER) {
            return Math.max(baseFloor, Math.min(currentHeight, 52));
        }
        return baseFloor;
    }

    private int resolveProtectedOperationBandRowHeightFloor(int rowIndex,
                                                            List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        if (isProtectedOperationBandEquipmentRow(rowIndex, rowTypes)) {
            return 52;
        }
        if (isProtectedOperationBandBodyRow(rowIndex, rowTypes)) {
            return 84;
        }
        return 0;
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

    private JSONObject buildFillForm(MesProBatchRecordParsedCell cell, String reportCode,
                                     int rowIndex, int columnIndex, int effectiveWidth,
                                     boolean compactFillLayout, boolean visuallyQuietFillCell,
                                     boolean visibleSingleLineBlankEntry,
                                     boolean forcePlainTextInput,
                                     boolean signatureFillCell,
                                     List<String> appendedCheckboxChoices) {
        JSONObject fillForm = new JSONObject(true);
        String inputType = (forcePlainTextInput || signatureFillCell)
                ? MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT
                : resolveFillInputType(cell, effectiveWidth);
        boolean compactFillCell = compactFillLayout
                && MesProBatchRecordReportShapeRules.isCompactFillableCell(cell, effectiveWidth);
        boolean checkboxFillCell = !forcePlainTextInput
                && !signatureFillCell
                && MesProBatchRecordReportShapeRules.INPUT_TYPE_CHECKBOX.equals(inputType);
        Object defaultValue = MesProBatchRecordCellRuleSupport.defaultFillValue(checkboxFillCell ? "BOOLEAN" : "STRING");
        fillForm.put("component", "Input");
        fillForm.put("field", buildFieldName(reportCode, rowIndex, columnIndex));
        fillForm.put("componentFlag", signatureFillCell
                ? MesProBatchRecordCellRuleSupport.defaultComponentFlag("SIGNATURE", null)
                : checkboxFillCell
                ? "checkbox"
                : MesProBatchRecordReportShapeRules.INPUT_TYPE_TEXTAREA.equals(inputType)
                ? "input-textarea"
                : "input-text");
        fillForm.put("value", defaultValue);
        fillForm.put("defaultValue", defaultValue);
        fillForm.put("placeholder", checkboxFillCell
                ? ""
                : signatureFillCell
                ? ""
                : forcePlainTextInput
                ? ""
                : resolveFillPlaceholder(cell, effectiveWidth, compactFillLayout, visuallyQuietFillCell,
                visibleSingleLineBlankEntry));
        fillForm.put("required", false);
        fillForm.put("requiredTip", "\u4e0d\u80fd\u4e3a\u7a7a~");
        fillForm.put("label", "");
        fillForm.put("labelText", checkboxFillCell
                ? MesProBatchRecordCellRuleSupport.normalizeCheckboxChoiceLabel(
                appendCheckboxChoiceText(cell.getText(), appendedCheckboxChoices))
                : "");
        if (checkboxFillCell) {
            JSONArray options = resolveCheckboxChoiceOptions(cell, appendedCheckboxChoices);
            if (!options.isEmpty()) {
                fillForm.put("options", options);
            }
        }
        fillForm.put("pattern", "");
        fillForm.put("patternErrorTip", "");
        fillForm.put("requiredRelevanceCell", "");
        if (compactFillCell || visuallyQuietFillCell) {
            JSONObject props = new JSONObject(true);
            props.put("border", false);
            props.put("size", "small");
            fillForm.put("props", props);
        }
        return fillForm;
    }

    private JSONArray resolveCheckboxChoiceOptions(MesProBatchRecordParsedCell cell, List<String> appendedCheckboxChoices) {
        JSONArray options = new JSONArray();
        List<String> labels = MesProBatchRecordCellRuleSupport.splitUncheckedCheckboxChoiceLabels(
                appendCheckboxChoiceText(cell == null ? null : cell.getText(), appendedCheckboxChoices));
        if (labels.size() <= 1) {
            return options;
        }
        for (String rawLabel : labels) {
            String label = MesProBatchRecordCellRuleSupport.normalizeCheckboxChoiceLabel(rawLabel);
            if (label.isBlank()) {
                continue;
            }
            JSONObject option = new JSONObject(true);
            option.put("label", label);
            option.put("value", label);
            options.add(option);
        }
        return options;
    }

    private String appendCheckboxChoiceText(String baseText, List<String> appendedCheckboxChoices) {
        String result = MesProBatchRecordReportShapeRules.normalizeRecognizedText(baseText);
        if (appendedCheckboxChoices == null || appendedCheckboxChoices.isEmpty()) {
            return result;
        }
        List<String> existingLabels = new ArrayList<>();
        List<String> splitLabels = MesProBatchRecordCellRuleSupport.splitUncheckedCheckboxChoiceLabels(result);
        if (splitLabels.isEmpty() && MesProBatchRecordCellRuleSupport.isCheckboxChoiceText(result)) {
            existingLabels.add(MesProBatchRecordCellRuleSupport.normalizeCheckboxChoiceLabel(result));
        } else {
            for (String splitLabel : splitLabels) {
                existingLabels.add(MesProBatchRecordCellRuleSupport.normalizeCheckboxChoiceLabel(splitLabel));
            }
        }
        StringBuilder builder = new StringBuilder(result);
        for (String rawChoice : appendedCheckboxChoices) {
            String choiceText = MesProBatchRecordReportShapeRules.normalizeRecognizedText(rawChoice);
            String normalizedLabel = MesProBatchRecordCellRuleSupport.normalizeCheckboxChoiceLabel(choiceText);
            if (normalizedLabel.isBlank() || existingLabels.contains(normalizedLabel)) {
                continue;
            }
            if (!choiceText.startsWith("□") && !choiceText.startsWith("☐")) {
                choiceText = "□" + normalizedLabel;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(choiceText);
            existingLabels.add(normalizedLabel);
        }
        return builder.toString();
    }

    private String resolveFillInputType(MesProBatchRecordParsedCell cell, int effectiveWidth) {
        if (isRecognizedCheckboxChoiceCell(cell)) {
            return MesProBatchRecordReportShapeRules.INPUT_TYPE_CHECKBOX;
        }
        if (isNarrativePromptBlankArea(cell, effectiveWidth)) {
            return MesProBatchRecordReportShapeRules.INPUT_TYPE_TEXTAREA;
        }
        if (isWideBlankNarrativeArea(cell, effectiveWidth)) {
            return MesProBatchRecordReportShapeRules.INPUT_TYPE_TEXTAREA;
        }
        return MesProBatchRecordReportShapeRules.resolveInputType(cell);
    }

    private String resolveFillPlaceholder(MesProBatchRecordParsedCell cell, int effectiveWidth,
                                          boolean compactFillLayout, boolean visuallyQuietFillCell,
                                          boolean visibleSingleLineBlankEntry) {
        if (isRecognizedCheckboxChoiceCell(cell)) {
            return "";
        }
        if (visibleSingleLineBlankEntry
                && MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell == null ? null : cell.getText()).isBlank()) {
            return MesProBatchRecordReportShapeRules.resolvePlaceholder(cell);
        }
        if (visuallyQuietFillCell) {
            return "";
        }
        if (cell != null && cell.getPlaceholder() != null && cell.getPlaceholder().isEmpty()) {
            return "";
        }
        if (compactFillLayout
                && MesProBatchRecordReportShapeRules.shouldHideCompactFillPlaceholder(cell, effectiveWidth)) {
            return "";
        }
        return MesProBatchRecordReportShapeRules.resolvePlaceholder(cell);
    }

    private String buildFieldName(String reportCode, int rowIndex, int columnIndex) {
        String normalizedReportCode = reportCode == null ? "EBR" : reportCode.replaceAll("[^A-Za-z0-9_]", "_");
        return "ebr_" + normalizedReportCode + "_r" + rowIndex + "_c" + columnIndex;
    }

    private String resolveVisibleText(MesProBatchRecordParsedCell cell) {
        if (MesProBatchRecordReportShapeRules.isFillable(cell)) {
            String normalizedText = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
            return normalizedText.isBlank()
                    ? ""
                    : normalizedText;
        }
        String normalizedText = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
        return normalizedText.isBlank()
                ? MesProBatchRecordReportShapeRules.normalizePaddingText()
                : normalizedText;
    }

    private int resolveStyleIndex(MesProBatchRecordParsedCell cell, JSONArray styles, Map<String, Integer> styleIndexes,
                                  int rowIndex, int columnIndex, int rowCount, int columnCount,
                                  List<MesProBatchRecordParsedCell> sourceRow, boolean sectionRow, String textColor) {
        String align = MesProBatchRecordReportShapeRules.resolveHorizontalAlign(cell);
        String valign = MesProBatchRecordReportShapeRules.resolveVerticalAlign();
        int fontSize = MesProBatchRecordReportShapeRules.clampFontSize(cell.getFontSize(), cell.isBold());
        String backgroundColor = cell.getBackgroundColor() == null ? "" : cell.getBackgroundColor().trim();
        BorderSpec borderSpec = resolveBorderSpec(cell, rowIndex, columnIndex, rowCount, columnCount, sourceRow, sectionRow);
        String key = cell.isBold() + "|" + fontSize + "|" + align + "|" + valign + "|" + cell.isBorderless()
                + "|" + backgroundColor + "|" + borderSpec.key() + "|" + (textColor == null ? "" : textColor);
        Integer existing = styleIndexes.get(key);
        if (existing != null) {
            return existing;
        }
        JSONObject style = new JSONObject(true);
        style.put("align", align);
        style.put("valign", valign);
        style.put("textwrap", true);
        JSONObject font = new JSONObject(true);
        font.put("size", fontSize);
        if (cell.isBold()) {
            font.put("bold", true);
        }
        style.put("font", font);
        if (!backgroundColor.isBlank()) {
            style.put("bgcolor", backgroundColor);
        }
        if (textColor != null && !textColor.isBlank()) {
            style.put("color", textColor);
        }
        if (!borderSpec.isEmpty()) {
            JSONObject border = new JSONObject(true);
            border.put("bottom", List.of(borderSpec.bottom(), "#000"));
            border.put("top", List.of(borderSpec.top(), "#000"));
            border.put("left", List.of(borderSpec.left(), "#000"));
            border.put("right", List.of(borderSpec.right(), "#000"));
            style.put("border", border);
        }
        int index = styles.size();
        styles.add(style);
        styleIndexes.put(key, index);
        return index;
    }

    private String resolveTextColor(String text) {
        return compactText(text).contains("/pcs") ? "#c00000" : null;
    }

    private String normalizeRenderableText(String text) {
        return text == null || text.isBlank()
                ? MesProBatchRecordReportShapeRules.normalizePaddingText()
                : text;
    }

    private int reserveWhitespaceForWideBlankArea(MesProBatchRecordParsedCell cell, int effectiveWidth, int estimatedHeight) {
        if (!isWideBlankNarrativeArea(cell, effectiveWidth)) {
            return estimatedHeight;
        }
        return Math.max(estimatedHeight, MesProBatchRecordReportShapeRules.resolveNarrativeRowHeightFloor(1));
    }

    private BorderSpec resolveBorderSpec(MesProBatchRecordParsedCell cell, int rowIndex, int columnIndex,
                                         int rowCount, int columnCount, List<MesProBatchRecordParsedCell> sourceRow,
                                         boolean sectionRow) {
        if (cell.isBorderless()) {
            return BorderSpec.empty();
        }
        if (hasExplicitSourceBorderStyles(cell)) {
            return new BorderSpec(
                    resolveExplicitOrFallbackBorderStyle(cell.getTopBorderStyle(),
                            resolveBorderWeight("top", rowIndex, columnIndex, cell, rowCount, columnCount, sourceRow, sectionRow)),
                    resolveExplicitOrFallbackBorderStyle(cell.getBottomBorderStyle(),
                            resolveBorderWeight("bottom", rowIndex, columnIndex, cell, rowCount, columnCount, sourceRow, sectionRow)),
                    resolveExplicitOrFallbackBorderStyle(cell.getLeftBorderStyle(),
                            resolveBorderWeight("left", rowIndex, columnIndex, cell, rowCount, columnCount, sourceRow, sectionRow)),
                    resolveExplicitOrFallbackBorderStyle(cell.getRightBorderStyle(),
                            resolveBorderWeight("right", rowIndex, columnIndex, cell, rowCount, columnCount, sourceRow, sectionRow))
            );
        }
        return new BorderSpec(
                resolveBorderWeight("top", rowIndex, columnIndex, cell, rowCount, columnCount, sourceRow, sectionRow),
                resolveBorderWeight("bottom", rowIndex, columnIndex, cell, rowCount, columnCount, sourceRow, sectionRow),
                resolveBorderWeight("left", rowIndex, columnIndex, cell, rowCount, columnCount, sourceRow, sectionRow),
                resolveBorderWeight("right", rowIndex, columnIndex, cell, rowCount, columnCount, sourceRow, sectionRow)
        );
    }

    private boolean hasExplicitSourceBorderStyles(MesProBatchRecordParsedCell cell) {
        return cell.getTopBorderStyle() != null
                || cell.getBottomBorderStyle() != null
                || cell.getLeftBorderStyle() != null
                || cell.getRightBorderStyle() != null;
    }

    private String resolveExplicitOrFallbackBorderStyle(String explicitStyle, String fallbackStyle) {
        return explicitStyle != null ? explicitStyle : fallbackStyle;
    }

    private String resolveBorderWeight(String side, int rowIndex, int columnIndex, MesProBatchRecordParsedCell cell,
                                       int rowCount, int columnCount, List<MesProBatchRecordParsedCell> sourceRow,
                                       boolean sectionRow) {
        if (isOuterBorderSide(side, rowIndex, columnIndex, cell, rowCount, columnCount)) {
            return "thick";
        }
        if (sectionRow && ("top".equals(side) || "bottom".equals(side))) {
            return "medium";
        }
        if (("top".equals(side) || "bottom".equals(side)) && isSectionLikeCell(cell, sourceRow)) {
            return "medium";
        }
        return "thin";
    }

    private boolean isOuterBorderSide(String side, int rowIndex, int columnIndex, MesProBatchRecordParsedCell cell,
                                      int rowCount, int columnCount) {
        int rowSpan = Math.max(cell.getRowSpan(), 1);
        int colSpan = Math.max(cell.getColSpan(), 1);
        return switch (side) {
            case "top" -> rowIndex == 0;
            case "bottom" -> rowIndex + rowSpan >= rowCount;
            case "left" -> columnIndex == 0;
            case "right" -> columnIndex + colSpan >= columnCount;
            default -> false;
        };
    }

    private boolean isSectionLikeRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        if (row.size() == 1) {
            return isSectionLikeCell(row.get(0), row);
        }
        long decoratedCount = row.stream().filter(cell -> cell.isBold()
                || hasBackgroundColor(cell)
                || cell.getColSpan() > 1
                || cell.getRowSpan() > 1).count();
        long fillableCount = row.stream().filter(MesProBatchRecordReportShapeRules::isFillable).count();
        return decoratedCount > 0 && decoratedCount >= fillableCount;
    }

    private boolean isSectionLikeCell(MesProBatchRecordParsedCell cell, List<MesProBatchRecordParsedCell> row) {
        if (cell == null) {
            return false;
        }
        String compactText = compactText(cell.getText());
        if (compactText.isBlank()) {
            return false;
        }
        boolean headerShape = cell.isBold()
                || hasBackgroundColor(cell)
                || cell.getColSpan() > 1
                || cell.getRowSpan() > 1;
        boolean compactShape = compactText.length() <= 24
                || "center".equalsIgnoreCase(MesProBatchRecordReportShapeRules.resolveHorizontalAlign(cell));
        if (!headerShape || !compactShape) {
            return false;
        }
        if (row == null || row.size() <= 1) {
            return true;
        }
        long decoratedCount = row.stream().filter(item -> item.isBold()
                || hasBackgroundColor(item)
                || item.getColSpan() > 1
                || item.getRowSpan() > 1).count();
        long fillableCount = row.stream().filter(MesProBatchRecordReportShapeRules::isFillable).count();
        return decoratedCount > 0 && decoratedCount >= fillableCount;
    }

    private boolean hasBackgroundColor(MesProBatchRecordParsedCell cell) {
        return cell != null && cell.getBackgroundColor() != null && !cell.getBackgroundColor().trim().isBlank();
    }

    private String compactText(String text) {
        return text == null ? "" : text.replace("\n", "").replace(" ", "").trim();
    }

    private boolean isWideBlankNarrativeArea(MesProBatchRecordParsedCell cell, int effectiveWidth) {
        if (cell == null || !MesProBatchRecordReportShapeRules.isFillable(cell)) {
            return false;
        }
        if (!MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText()).isBlank()) {
            return false;
        }
        return effectiveWidth >= 180 && (cell.getColSpan() > 1 || cell.getWidthPx() >= 180);
    }

    private boolean isNarrativePromptBlankArea(MesProBatchRecordParsedCell cell, int effectiveWidth) {
        if (cell == null || cell.isVisualBlank() || cell.isDiagonalSlash() || cell.isFillable()) {
            return false;
        }
        if (effectiveWidth < 240 && cell.getWidthPx() < 240) {
            return false;
        }
        String text = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
        if (text.isBlank()) {
            return false;
        }
        String compact = compactText(text);
        boolean narrativePrefix = compact.startsWith("备注")
                || compact.startsWith("说明")
                || compact.startsWith("补充说明");
        return narrativePrefix && compact.contains("填写") && compact.contains("空白");
    }

    private int resolveEffectiveCellWidth(Map<Integer, Integer> columnWidthMap, int columnIndex, int colSpan) {
        int width = 0;
        for (int offset = 0; offset < Math.max(colSpan, 1); offset++) {
            width += columnWidthMap.getOrDefault(columnIndex + offset, MesProBatchRecordReportShapeRules.MIN_COLUMN_WIDTH_PX);
        }
        return width;
    }

    private String toMergeRange(int startRow, int startColumn, int endRow, int endColumn) {
        return toCellRef(startRow, startColumn) + ":" + toCellRef(endRow, endColumn);
    }

    private String toCellRef(int rowIndex, int columnIndex) {
        return toColumnName(columnIndex) + (rowIndex + 1);
    }

    private String toColumnName(int columnIndex) {
        StringBuilder builder = new StringBuilder();
        int current = columnIndex;
        while (current >= 0) {
            builder.insert(0, (char) ('A' + (current % 26)));
            current = current / 26 - 1;
        }
        return builder.toString();
    }

    private record BorderSpec(String top, String bottom, String left, String right) {
        private static BorderSpec empty() {
            return new BorderSpec("", "", "", "");
        }

        private boolean isEmpty() {
            return top.isBlank() && bottom.isBlank() && left.isBlank() && right.isBlank();
        }

        private String key() {
            return top + "|" + bottom + "|" + left + "|" + right;
        }
    }

    private record FillFormLayoutSpec(int width, int height, boolean compact) {
    }

    private record PageDecorationPlan(boolean hasDocumentHeader,
                                      List<Integer> continuationHeaderStarts,
                                      List<Integer> continuationBreakStarts) {
        private static PageDecorationPlan none() {
            return new PageDecorationPlan(false, List.of(), List.of());
        }
    }

    private record DecoratedRowsResult(JSONObject rowsObject, List<String> merges, int rowCount,
                                       JSONArray fixedPrintHeadRows, JSONArray fixedPrintTailRows) {
    }

    private record MergeRange(int startRow, int startColumn, int endRow, int endColumn) {
    }

    private record RowBand(int startRowIndex, int endRowIndex, int rowCount) {
    }

    private record CellRef(int rowIndex, int columnIndex) {
    }

    private record SignatureDateCheckboxFragmentPlan(Set<CellRef> fragments,
                                                     Map<CellRef, List<String>> appendedChoicesByAnchor) {
    }

    private record SequenceNumberColumnPlan(Set<Integer> columns,
                                            Map<CellRef, String> placedCellValues,
                                            Map<CellRef, String> syntheticCellValues) {
        private static SequenceNumberColumnPlan none() {
            return new SequenceNumberColumnPlan(Set.of(), Map.of(), Map.of());
        }

        private String placedCellText(int rowIndex, int columnIndex) {
            return placedCellValues.get(new CellRef(rowIndex, columnIndex));
        }

        private String syntheticCellText(int rowIndex, int columnIndex) {
            return syntheticCellValues.get(new CellRef(rowIndex, columnIndex));
        }
    }

    private record PlacedCell(int rowIndex, int columnIndex, MesProBatchRecordParsedCell cell) {
    }

    private record CoveredBlankSpan(int startColumn, int colSpan) {
    }
}
