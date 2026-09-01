package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WordTableVisualSchemaBuilder {

    private static final int DEFAULT_COLUMN_WIDTH = 160;
    private static final int DEFAULT_ROW_HEIGHT = 30;
    private static final Pattern INLINE_TEXT_INPUT_PATTERN =
            Pattern.compile("^(.+?[：:])([＿_]{2,})$");
    private static final Pattern CHECKBOX_OPTION_PATTERN =
            Pattern.compile("□\\s*([^□]+?)(?=□|$)");

    private WordTableVisualSchemaBuilder() {
    }

    static String build(XWPFTable table) {
        return build(table, 0, table.getRows().size());
    }

    static String build(XWPFTable table, int startRowInclusive, int endRowExclusive) {
        if (startRowInclusive < 0 || endRowExclusive > table.getRows().size()
                || startRowInclusive >= endRowExclusive) {
            throw new IllegalArgumentException("invalid Word table row range");
        }
        int columnCount = resolveColumnCount(table);
        RowExpansionPlan rowPlan = buildRowExpansionPlan(table, startRowInclusive, endRowExclusive);
        List<Map<String, Object>> styles = new ArrayList<>();
        Map<String, Integer> styleIndexes = new HashMap<>();
        List<String> merges = new ArrayList<>();
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("cols", buildColumns(table, columnCount));
        layout.put("rows", buildRows(table, columnCount, startRowInclusive, endRowExclusive,
                rowPlan, styles, styleIndexes, merges));
        layout.put("styles", styles);
        layout.put("merges", merges);

        List<Map<String, Object>> cellRules = buildCellRules(table, columnCount,
                startRowInclusive, endRowExclusive, rowPlan);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("sheetLayoutJson", JsonUtils.toJsonString(layout));
        schema.put("cellRules", cellRules);
        schema.put("signatureCellMarkers", buildSignatureCellMarkers(table, columnCount,
                startRowInclusive, endRowExclusive, rowPlan, cellRules));
        schema.put("assistRows", List.of());
        schema.put("fillAssignments", List.of());
        return JsonUtils.toJsonString(schema);
    }

    private static Map<String, Object> buildColumns(XWPFTable table, int columnCount) {
        Map<String, Object> columns = new LinkedHashMap<>();
        List<CTTblGridCol> gridColumns = resolveGridColumns(table);
        for (int index = 0; index < columnCount; index++) {
            int width = index < gridColumns.size()
                    ? toPixels(gridColumns.get(index).getW()) : DEFAULT_COLUMN_WIDTH;
            columns.put(String.valueOf(index), Map.of("width", width));
        }
        columns.put("len", columnCount);
        return columns;
    }

    private static Map<String, Object> buildRows(XWPFTable table, int columnCount,
                                                  int startRowInclusive, int endRowExclusive,
                                                  RowExpansionPlan rowPlan,
                                                  List<Map<String, Object>> styles,
                                                  Map<String, Integer> styleIndexes,
                                                  List<String> merges) {
        Map<String, Object> rows = new LinkedHashMap<>();
        List<XWPFTableRow> tableRows = table.getRows();
        for (int sourceRowIndex = startRowInclusive; sourceRowIndex < endRowExclusive; sourceRowIndex++) {
            int rowIndex = rowPlan.visualStart(sourceRowIndex);
            XWPFTableRow row = tableRows.get(sourceRowIndex);
            int expansion = rowPlan.expansion(sourceRowIndex);
            List<Map<String, Object>> expandedCells = new ArrayList<>();
            List<Integer> expandedHeights = new ArrayList<>();
            for (int offset = 0; offset < expansion; offset++) {
                expandedCells.add(new LinkedHashMap<>());
                expandedHeights.add(resolveRowHeight(row));
            }
            int columnIndex = 0;
            List<XWPFTableCell> rowCells = row.getTableCells();
            for (int physicalCellIndex = 0; physicalCellIndex < rowCells.size(); physicalCellIndex++) {
                XWPFTableCell cell = rowCells.get(physicalCellIndex);
                int columnSpan = resolveColumnSpan(cell);
                if (isVerticalMergeFollower(cell)) {
                    columnIndex += columnSpan;
                    continue;
                }
                if (hasNestedTable(cell)) {
                    appendNestedTableLayout(table, cell.getTables().get(0), rowIndex, columnIndex, columnSpan,
                            expandedCells, expandedHeights, styles, styleIndexes, merges);
                    columnIndex += columnSpan;
                    continue;
                }
                int sourceRowSpan = isVerticalMergeRestart(cell)
                        ? resolveVerticalSpan(table, sourceRowIndex, columnIndex, columnSpan, endRowExclusive) : 1;
                int rowSpan = isVerticalMergeRestart(cell)
                        ? rowPlan.visualSpan(sourceRowIndex, sourceRowSpan) : expansion;
                int styleIndex = resolveStyleIndex(cell, styles, styleIndexes);
                List<InlineInputSegment> segmentedInputs = parseSegmentedInputCell(cell.getText());
                if (!segmentedInputs.isEmpty()) {
                    appendSegmentedInputLayout(expandedCells.get(0), rowIndex, columnIndex, rowSpan, columnSpan,
                            segmentedInputs, styleIndex, merges);
                    columnIndex += columnSpan;
                    continue;
                }
                InlineTextInput inlineInput = parseInlineTextInput(cell.getText());
                if (inlineInput == null) {
                    expandedCells.get(0).put(String.valueOf(columnIndex), layoutCell(resolveCellText(cell),
                            rowIndex, columnIndex, rowSpan, columnSpan, styleIndex,
                            resolveDiagonalDirection(cell), merges));
                    columnIndex += columnSpan;
                    continue;
                }

                int visualSpan = Math.max(columnSpan, 2);
                expandedCells.get(0).put(String.valueOf(columnIndex), layoutCell(inlineInput.label(),
                        rowIndex, columnIndex, rowSpan, 1, styleIndex, null, merges));
                expandedCells.get(0).put(String.valueOf(columnIndex + 1), layoutCell("",
                        rowIndex, columnIndex + 1, rowSpan, visualSpan - 1, styleIndex, null, merges));
                columnIndex += visualSpan;
            }
            for (int offset = 0; offset < expansion; offset++) {
                rows.put(String.valueOf(rowIndex + offset), Map.of(
                        "height", expandedHeights.get(offset),
                        "cells", expandedCells.get(offset)));
            }
        }
        rows.put("len", rowPlan.totalRows());
        return rows;
    }

    private static List<Map<String, Object>> buildCellRules(XWPFTable table, int columnCount,
                                                             int startRowInclusive, int endRowExclusive,
                                                             RowExpansionPlan rowPlan) {
        List<Map<String, Object>> sourceRules = buildSourceCellRules(table, columnCount,
                startRowInclusive, endRowExclusive);
        List<Map<String, Object>> rules = new ArrayList<>();
        for (Map<String, Object> sourceRule : sourceRules) {
            int sourceRowIndex = startRowInclusive + ((Number) sourceRule.get("rowIndex")).intValue();
            rules.add(remapRule(sourceRule, rowPlan.visualStart(sourceRowIndex),
                    ((Number) sourceRule.get("columnIndex")).intValue()));
        }
        appendNestedTableRules(table, startRowInclusive, endRowExclusive, rowPlan, rules);
        return rules;
    }

    private static List<Map<String, Object>> buildSourceCellRules(XWPFTable table, int columnCount,
                                                                   int startRowInclusive, int endRowExclusive) {
        List<Map<String, Object>> rules = new ArrayList<>();
        List<XWPFTableRow> tableRows = table.getRows().subList(startRowInclusive, endRowExclusive);
        buildHeaderRules(tableRows.isEmpty() ? null : tableRows.get(0), rules);
        Map<Integer, String> headerLabels = resolveInspectionHeaderLabels(tableRows, columnCount);
        int headerRowIndex = resolveInspectionHeaderRowIndex(tableRows);
        for (int rowIndex = 0; rowIndex < tableRows.size(); rowIndex++) {
            if (rowIndex <= headerRowIndex) {
                continue;
            }
            int columnIndex = 0;
            for (XWPFTableCell cell : tableRows.get(rowIndex).getTableCells()) {
                int columnSpan = resolveColumnSpan(cell);
                if (hasNestedTable(cell)) {
                    columnIndex += columnSpan;
                    continue;
                }
                if (isVerticalMergeFollower(cell)) {
                    columnIndex += columnSpan;
                    continue;
                }
                List<InlineInputSegment> segmentedInputs = parseSegmentedInputCell(cell.getText());
                if (!segmentedInputs.isEmpty()) {
                    appendSegmentedInputRules(rules, rowIndex, columnIndex, columnSpan, segmentedInputs);
                    columnIndex += columnSpan;
                    continue;
                }
                if (isFooterRow(rowText(tableRows.get(rowIndex)))) {
                    columnIndex += columnSpan;
                    continue;
                }
                InlineTextInput inlineInput = parseInlineTextInput(cell.getText());
                if (inlineInput != null) {
                    rules.add(buildRule(rowIndex, columnIndex + 1, "STRING", "input-text",
                            inlineInput.label(), Map.of()));
                    columnIndex += Math.max(columnSpan, 2);
                    continue;
                }
                String text = normalizeText(cell.getText());
                if (isFullWidthFormTitle(rowIndex, columnSpan, columnCount, text)) {
                    columnIndex += columnSpan;
                    continue;
                }
                if (countCheckboxMarkers(text) > 0) {
                    rules.add(buildCheckboxRule(rowIndex, columnIndex, text));
                    columnIndex += columnSpan;
                    continue;
                }
                String headerLabel = headerLabels.get(columnIndex);
                if (text.isBlank() && headerLabel != null) {
                    if (headerLabel.contains("复核人")) {
                        rules.add(buildRule(rowIndex, columnIndex, "SIGNATURE", "signature",
                                headerLabel, Map.of()));
                    } else if (headerLabel.contains("检验日期") || headerLabel.contains("检验人/日期")) {
                        rules.add(buildRule(rowIndex, columnIndex, "DATE", "date",
                                headerLabel, Map.of()));
                    } else if (headerLabel.contains("序号") || headerLabel.contains("检测数量")) {
                        rules.add(buildRule(rowIndex, columnIndex, "NUMBER", "input-number",
                                headerLabel, Map.of()));
                    }
                }
                columnIndex += columnSpan;
            }
        }
        buildGenericBlankCellRules(tableRows, columnCount, rules);
        return rules;
    }

    private static boolean isFullWidthFormTitle(int rowIndex, int columnSpan, int columnCount, String text) {
        return rowIndex == 0 && columnSpan >= columnCount
                && (text.contains("记录") || text.contains("表单"));
    }

    private static void buildHeaderRules(XWPFTableRow row, List<Map<String, Object>> rules) {
        if (row == null) {
            return;
        }
        int columnIndex = 0;
        String pendingLabel = null;
        for (XWPFTableCell cell : row.getTableCells()) {
            int columnSpan = resolveColumnSpan(cell);
            if (hasNestedTable(cell)) {
                pendingLabel = null;
                columnIndex += columnSpan;
                continue;
            }
            String text = normalizeText(cell.getText());
            if (!text.isBlank()) {
                pendingLabel = text;
            } else if (pendingLabel != null) {
                String valueType = pendingLabel.contains("批数量") ? "NUMBER" : "STRING";
                String componentFlag = "NUMBER".equals(valueType) ? "input-number" : "input-text";
                rules.add(buildRule(0, columnIndex, valueType, componentFlag, pendingLabel, Map.of()));
                pendingLabel = null;
            }
            columnIndex += columnSpan;
        }
    }

    private static void appendSegmentedInputLayout(Map<String, Object> cells, int rowIndex, int startColumn,
                                                   int rowSpan, int columnSpan,
                                                   List<InlineInputSegment> segments, int styleIndex,
                                                   List<String> merges) {
        for (InlineInputPlacement placement : resolveSegmentedInputPlacements(startColumn, columnSpan, segments)) {
            cells.put(String.valueOf(placement.labelColumn()),
                    layoutCell(placement.segment().displayLabel(), rowIndex, placement.labelColumn(),
                            rowSpan, placement.labelSpan(), styleIndex, null, merges));
            cells.put(String.valueOf(placement.inputColumn()),
                    layoutCell("", rowIndex, placement.inputColumn(), rowSpan,
                            placement.inputSpan(), styleIndex, null, merges));
        }
    }

    private static void appendSegmentedInputRules(List<Map<String, Object>> rules, int rowIndex, int startColumn,
                                                  int columnSpan, List<InlineInputSegment> segments) {
        for (InlineInputPlacement placement : resolveSegmentedInputPlacements(startColumn, columnSpan, segments)) {
            InlineInputSegment segment = placement.segment();
            rules.add(buildRule(rowIndex, placement.inputColumn(), segment.valueType(),
                    segment.componentFlag(), segment.ruleLabel(), Map.of()));
        }
    }

    private static List<InlineInputPlacement> resolveSegmentedInputPlacements(int startColumn, int columnSpan,
            List<InlineInputSegment> segments) {
        List<InlineInputPlacement> placements = new ArrayList<>();
        int remainingSpan = Math.max(columnSpan, segments.size() * 2);
        int columnIndex = startColumn;
        for (int index = 0; index < segments.size(); index++) {
            InlineInputSegment segment = segments.get(index);
            int remainingSegments = segments.size() - index - 1;
            int requiredSpanAfterCurrentInput = remainingSegments * 2;
            int labelSpan = Math.min(segment.preferredLabelSpan(),
                    Math.max(1, remainingSpan - 1 - requiredSpanAfterCurrentInput));
            int labelColumn = columnIndex;
            columnIndex += labelSpan;
            remainingSpan -= labelSpan;
            int inputSpan = index == segments.size() - 1 ? Math.max(1, remainingSpan) : 1;
            int inputColumn = columnIndex;
            columnIndex += inputSpan;
            remainingSpan -= inputSpan;
            placements.add(new InlineInputPlacement(segment, labelColumn, labelSpan, inputColumn, inputSpan));
        }
        return placements;
    }

    private static List<Map<String, Object>> buildSignatureCellMarkers(XWPFTable table, int columnCount,
                                                                        int startRowInclusive,
                                                                        int endRowExclusive,
                                                                        RowExpansionPlan rowPlan,
                                                                        List<Map<String, Object>> cellRules) {
        List<Map<String, Object>> markers = new ArrayList<>();
        List<XWPFTableRow> tableRows = table.getRows().subList(startRowInclusive, endRowExclusive);
        Map<Integer, String> headerLabels = resolveInspectionHeaderLabels(tableRows, columnCount);
        int headerRowIndex = resolveInspectionHeaderRowIndex(tableRows);
        Integer signatureColumn = headerLabels.entrySet().stream()
                .filter(entry -> entry.getValue().contains("复核人"))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        for (int rowIndex = headerRowIndex + 1; signatureColumn != null && rowIndex < tableRows.size(); rowIndex++) {
            if (!isFooterRow(rowText(tableRows.get(rowIndex)))) {
                int visualRowIndex = rowPlan.visualStart(startRowInclusive + rowIndex);
                markers.add(Map.of(
                        "rowIndex", visualRowIndex,
                        "columnIndex", signatureColumn,
                        "enabled", true,
                        "signatureCellKey", visualRowIndex + ":" + signatureColumn,
                        "actionType", "FORM_REVIEW",
                        "label", "复核人/日期"));
            }
        }
        Set<String> existing = markers.stream()
                .map(marker -> marker.get("rowIndex") + ":" + marker.get("columnIndex"))
                .collect(java.util.stream.Collectors.toSet());
        for (Map<String, Object> rule : cellRules) {
            if (!"SIGNATURE".equals(rule.get("valueType"))) {
                continue;
            }
            int rowIndex = ((Number) rule.get("rowIndex")).intValue();
            int columnIndex = ((Number) rule.get("columnIndex")).intValue();
            String key = rowIndex + ":" + columnIndex;
            if (existing.add(key)) {
                markers.add(Map.of(
                        "rowIndex", rowIndex,
                        "columnIndex", columnIndex,
                        "enabled", true,
                        "signatureCellKey", key,
                        "actionType", "FORM_REVIEW",
                        "label", String.valueOf(rule.get("label"))));
            }
        }
        return markers;
    }

    private static Map<Integer, String> resolveInspectionHeaderLabels(XWPFTable table, int columnCount) {
        return resolveInspectionHeaderLabels(table.getRows(), columnCount);
    }

    private static Map<Integer, String> resolveInspectionHeaderLabels(List<XWPFTableRow> rows, int columnCount) {
        int headerRowIndex = resolveInspectionHeaderRowIndex(rows);
        if (headerRowIndex < 0) {
            return Map.of();
        }
        Map<Integer, String> labels = new LinkedHashMap<>();
        int columnIndex = 0;
        for (XWPFTableCell cell : rows.get(headerRowIndex).getTableCells()) {
            int columnSpan = resolveColumnSpan(cell);
            String text = normalizeText(cell.getText());
            for (int offset = 0; offset < columnSpan && columnIndex + offset < columnCount; offset++) {
                labels.put(columnIndex + offset, text);
            }
            columnIndex += columnSpan;
        }
        return labels;
    }

    private static int resolveInspectionHeaderRowIndex(XWPFTable table) {
        return resolveInspectionHeaderRowIndex(table.getRows());
    }

    private static int resolveInspectionHeaderRowIndex(List<XWPFTableRow> rows) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            String text = rowText(rows.get(rowIndex));
            if (text.contains("序号") && text.contains("检验日期") && text.contains("检测数量")) {
                return rowIndex;
            }
        }
        return -1;
    }

    private static boolean isFooterRow(String text) {
        String normalized = normalizeText(text);
        return normalized.contains("合格数量") || normalized.startsWith("备注");
    }

    private static String rowText(XWPFTableRow row) {
        return row.getTableCells().stream()
                .map(XWPFTableCell::getText)
                .map(WordTableVisualSchemaBuilder::normalizeText)
                .filter(text -> !text.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private static Map<String, Object> buildRule(int rowIndex, int columnIndex, String valueType,
                                                  String componentFlag, String label,
                                                  Map<String, Object> constraints) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rowIndex", rowIndex);
        rule.put("columnIndex", columnIndex);
        rule.put("valueType", valueType);
        rule.put("componentFlag", componentFlag);
        rule.put("required", false);
        rule.put("label", label);
        rule.put("placeholder", "");
        rule.put("constraints", constraints);
        rule.put("source", "AUTO");
        rule.put("reviewed", false);
        rule.put("confidence", 1);
        return rule;
    }

    private static Map<String, Object> buildCheckboxRule(int rowIndex, int columnIndex, String text) {
        List<Map<String, String>> options = new ArrayList<>();
        Matcher matcher = CHECKBOX_OPTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String label = matcher.group(1).replaceAll("[_＿]+$", "").trim();
            if (!label.isBlank()) {
                options.add(Map.of("label", label, "value", label));
            }
        }
        if (options.size() >= 2) {
            Map<String, Object> constraints = new LinkedHashMap<>();
            constraints.put("selectionMode", "single");
            constraints.put("options", options);
            return buildRule(rowIndex, columnIndex, "STRING", "radio-group", "检测结果", constraints);
        }
        return buildRule(rowIndex, columnIndex, "BOOLEAN", "checkbox", text, Map.of());
    }

    private static int countCheckboxMarkers(String text) {
        return (int) text.chars().filter(character -> character == '□').count();
    }

    private static Map<String, Object> layoutCell(String text, int rowIndex, int columnIndex,
                                                   int rowSpan, int columnSpan, int styleIndex,
                                                   String diagonalDirection, List<String> merges) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("text", text);
        cell.put("style", styleIndex);
        if (diagonalDirection != null) {
            cell.put("edhrDiagonalSlash", true);
            cell.put("edhrDiagonalSlashDirection", diagonalDirection);
        }
        if (rowSpan > 1 || columnSpan > 1) {
            cell.put("merge", List.of(rowSpan - 1, columnSpan - 1));
            merges.add(toMergeRange(rowIndex, columnIndex,
                    rowIndex + rowSpan - 1, columnIndex + columnSpan - 1));
        }
        return cell;
    }

    private static RowExpansionPlan buildRowExpansionPlan(XWPFTable table, int startRowInclusive,
                                                           int endRowExclusive) {
        List<Integer> visualStarts = new ArrayList<>();
        List<Integer> expansions = new ArrayList<>();
        int visualRowIndex = 0;
        for (int sourceRowIndex = startRowInclusive; sourceRowIndex < endRowExclusive; sourceRowIndex++) {
            visualStarts.add(visualRowIndex);
            int expansion = 1;
            for (XWPFTableCell cell : table.getRow(sourceRowIndex).getTableCells()) {
                if (cell.getTables().size() > 1) {
                    throw new IllegalArgumentException("multiple nested Word tables in one cell are not supported");
                }
                if (!hasNestedTable(cell)) {
                    continue;
                }
                if (!resolveCellText(cell).isBlank()) {
                    throw new IllegalArgumentException("nested Word table host cell also contains direct text");
                }
                XWPFTable nestedTable = cell.getTables().get(0);
                if (nestedTable.getRows().isEmpty()) {
                    throw new IllegalArgumentException("nested Word table has no rows");
                }
                validateNestedTableDepth(nestedTable);
                expansion = Math.max(expansion, nestedTable.getRows().size());
            }
            expansions.add(expansion);
            visualRowIndex += expansion;
        }
        return new RowExpansionPlan(startRowInclusive, visualStarts, expansions, visualRowIndex);
    }

    private static void validateNestedTableDepth(XWPFTable nestedTable) {
        boolean containsNestedTable = nestedTable.getRows().stream()
                .flatMap(row -> row.getTableCells().stream())
                .anyMatch(WordTableVisualSchemaBuilder::hasNestedTable);
        if (containsNestedTable) {
            throw new IllegalArgumentException("nested Word tables deeper than one level are not supported");
        }
    }

    private static void appendNestedTableLayout(XWPFTable parentTable, XWPFTable nestedTable,
                                                 int visualStartRow, int hostStartColumn, int hostColumnSpan,
                                                 List<Map<String, Object>> expandedCells,
                                                 List<Integer> expandedHeights,
                                                 List<Map<String, Object>> styles,
                                                 Map<String, Integer> styleIndexes,
                                                 List<String> merges) {
        int nestedColumnCount = resolveColumnCount(nestedTable);
        int[] boundaries = resolveNestedColumnBoundaries(parentTable, hostStartColumn, hostColumnSpan,
                nestedTable, nestedColumnCount);
        for (int nestedRowIndex = 0; nestedRowIndex < nestedTable.getRows().size(); nestedRowIndex++) {
            XWPFTableRow nestedRow = nestedTable.getRow(nestedRowIndex);
            Map<String, Object> targetCells = expandedCells.get(nestedRowIndex);
            expandedHeights.set(nestedRowIndex,
                    Math.max(expandedHeights.get(nestedRowIndex), resolveRowHeight(nestedRow)));
            int nestedColumnIndex = 0;
            for (XWPFTableCell nestedCell : nestedRow.getTableCells()) {
                int nestedColumnSpan = resolveColumnSpan(nestedCell);
                if (nestedColumnIndex + nestedColumnSpan > nestedColumnCount) {
                    throw new IllegalArgumentException("nested Word table row exceeds its declared column grid");
                }
                if (isVerticalMergeFollower(nestedCell)) {
                    nestedColumnIndex += nestedColumnSpan;
                    continue;
                }
                int mappedColumn = hostStartColumn + boundaries[nestedColumnIndex];
                int mappedEndColumn = hostStartColumn + boundaries[nestedColumnIndex + nestedColumnSpan];
                int mappedColumnSpan = mappedEndColumn - mappedColumn;
                int rowSpan = isVerticalMergeRestart(nestedCell)
                        ? resolveVerticalSpan(nestedTable, nestedRowIndex, nestedColumnIndex,
                        nestedColumnSpan, nestedTable.getRows().size()) : 1;
                int styleIndex = resolveStyleIndex(nestedCell, styles, styleIndexes);
                List<InlineInputSegment> segmentedInputs = parseSegmentedInputCell(nestedCell.getText());
                if (!segmentedInputs.isEmpty()) {
                    appendSegmentedInputLayout(targetCells, visualStartRow + nestedRowIndex,
                            mappedColumn, rowSpan, mappedColumnSpan, segmentedInputs, styleIndex, merges);
                    nestedColumnIndex += nestedColumnSpan;
                    continue;
                }
                InlineTextInput inlineInput = parseInlineTextInput(nestedCell.getText());
                if (inlineInput == null) {
                    targetCells.put(String.valueOf(mappedColumn), layoutCell(resolveCellText(nestedCell),
                            visualStartRow + nestedRowIndex, mappedColumn, rowSpan, mappedColumnSpan,
                            styleIndex, resolveDiagonalDirection(nestedCell), merges));
                } else {
                    int visualSpan = Math.max(mappedColumnSpan, 2);
                    targetCells.put(String.valueOf(mappedColumn), layoutCell(inlineInput.label(),
                            visualStartRow + nestedRowIndex, mappedColumn, rowSpan, 1,
                            styleIndex, null, merges));
                    targetCells.put(String.valueOf(mappedColumn + 1), layoutCell("",
                            visualStartRow + nestedRowIndex, mappedColumn + 1, rowSpan,
                            visualSpan - 1, styleIndex, null, merges));
                }
                nestedColumnIndex += nestedColumnSpan;
            }
        }
    }

    private static void appendNestedTableRules(XWPFTable parentTable, int startRowInclusive,
                                                int endRowExclusive, RowExpansionPlan rowPlan,
                                                List<Map<String, Object>> rules) {
        for (int sourceRowIndex = startRowInclusive; sourceRowIndex < endRowExclusive; sourceRowIndex++) {
            int hostStartColumn = 0;
            for (XWPFTableCell cell : parentTable.getRow(sourceRowIndex).getTableCells()) {
                int hostColumnSpan = resolveColumnSpan(cell);
                if (!hasNestedTable(cell)) {
                    hostStartColumn += hostColumnSpan;
                    continue;
                }
                XWPFTable nestedTable = cell.getTables().get(0);
                int nestedColumnCount = resolveColumnCount(nestedTable);
                int[] boundaries = resolveNestedColumnBoundaries(parentTable, hostStartColumn, hostColumnSpan,
                        nestedTable, nestedColumnCount);
                List<Map<String, Object>> nestedRules = buildSourceCellRules(nestedTable, nestedColumnCount,
                        0, nestedTable.getRows().size());
                for (Map<String, Object> nestedRule : nestedRules) {
                    int nestedRowIndex = ((Number) nestedRule.get("rowIndex")).intValue();
                    int nestedColumnIndex = ((Number) nestedRule.get("columnIndex")).intValue();
                    if (nestedColumnIndex < 0 || nestedColumnIndex >= nestedColumnCount) {
                        throw new IllegalArgumentException("nested Word table rule exceeds its declared column grid");
                    }
                    rules.add(remapRule(nestedRule,
                            rowPlan.visualStart(sourceRowIndex) + nestedRowIndex,
                            hostStartColumn + boundaries[nestedColumnIndex]));
                }
                hostStartColumn += hostColumnSpan;
            }
        }
    }

    private static Map<String, Object> remapRule(Map<String, Object> sourceRule,
                                                  int rowIndex, int columnIndex) {
        Map<String, Object> rule = new LinkedHashMap<>(sourceRule);
        rule.put("rowIndex", rowIndex);
        rule.put("columnIndex", columnIndex);
        return rule;
    }

    private static int[] resolveNestedColumnBoundaries(XWPFTable parentTable, int hostStartColumn,
                                                        int hostColumnSpan, XWPFTable nestedTable,
                                                        int nestedColumnCount) {
        if (hostColumnSpan < nestedColumnCount) {
            throw new IllegalArgumentException("nested Word table has more columns than its parent cell grid span");
        }
        double[] parentCumulative = cumulativeWidths(resolveGridColumns(parentTable),
                hostStartColumn, hostColumnSpan);
        double[] nestedCumulative = cumulativeWidths(resolveGridColumns(nestedTable), 0, nestedColumnCount);
        double parentTotal = parentCumulative[hostColumnSpan];
        double nestedTotal = nestedCumulative[nestedColumnCount];
        int[] boundaries = new int[nestedColumnCount + 1];
        boundaries[0] = 0;
        boundaries[nestedColumnCount] = hostColumnSpan;
        for (int nestedBoundary = 1; nestedBoundary < nestedColumnCount; nestedBoundary++) {
            double target = parentTotal * nestedCumulative[nestedBoundary] / nestedTotal;
            int minimum = boundaries[nestedBoundary - 1] + 1;
            int maximum = hostColumnSpan - (nestedColumnCount - nestedBoundary);
            int selected = minimum;
            double selectedDistance = Double.MAX_VALUE;
            for (int candidate = minimum; candidate <= maximum; candidate++) {
                double distance = Math.abs(parentCumulative[candidate] - target);
                if (distance < selectedDistance) {
                    selected = candidate;
                    selectedDistance = distance;
                }
            }
            boundaries[nestedBoundary] = selected;
        }
        return boundaries;
    }

    private static double[] cumulativeWidths(List<CTTblGridCol> gridColumns, int startColumn, int columnCount) {
        double[] cumulative = new double[columnCount + 1];
        for (int offset = 0; offset < columnCount; offset++) {
            int gridIndex = startColumn + offset;
            Object width = gridIndex < gridColumns.size() ? gridColumns.get(gridIndex).getW() : null;
            double value = width instanceof Number number && number.doubleValue() > 0
                    ? number.doubleValue() : 1D;
            cumulative[offset + 1] = cumulative[offset] + value;
        }
        return cumulative;
    }

    private static boolean hasNestedTable(XWPFTableCell cell) {
        return cell != null && !cell.getTables().isEmpty();
    }

    private static int resolveColumnCount(XWPFTable table) {
        int gridColumnCount = resolveGridColumns(table).size();
        if (gridColumnCount > 0) {
            return gridColumnCount;
        }
        return table.getRows().stream()
                .mapToInt(row -> row.getTableCells().stream().mapToInt(WordTableVisualSchemaBuilder::resolveColumnSpan).sum())
                .max()
                .orElse(1);
    }

    private static List<CTTblGridCol> resolveGridColumns(XWPFTable table) {
        CTTblGrid grid = table.getCTTbl().getTblGrid();
        return grid == null ? List.of() : grid.getGridColList();
    }

    private static int resolveColumnSpan(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().getTcPr();
        if (properties == null || !properties.isSetGridSpan() || properties.getGridSpan().getVal() == null) {
            return 1;
        }
        return Math.max(1, properties.getGridSpan().getVal().intValue());
    }

    private static boolean isVerticalMergeRestart(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().getTcPr();
        return properties != null && properties.isSetVMerge()
                && STMerge.RESTART.equals(properties.getVMerge().getVal());
    }

    private static boolean isVerticalMergeFollower(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().getTcPr();
        if (properties == null || !properties.isSetVMerge()) {
            return false;
        }
        Object value = properties.getVMerge().getVal();
        return value == null || STMerge.CONTINUE.equals(value);
    }

    private static int resolveVerticalSpan(XWPFTable table, int rowIndex, int startColumn,
                                            int columnSpan, int endRowExclusive) {
        int span = 1;
        for (int nextRowIndex = rowIndex + 1; nextRowIndex < endRowExclusive; nextRowIndex++) {
            PositionedCell nextCell = findCellAtColumn(table.getRow(nextRowIndex), startColumn);
            if (nextCell == null || nextCell.columnSpan() != columnSpan
                    || !isVerticalMergeFollower(nextCell.cell())) {
                break;
            }
            span++;
        }
        return span;
    }

    private static PositionedCell findCellAtColumn(XWPFTableRow row, int targetColumn) {
        int columnIndex = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            int columnSpan = resolveColumnSpan(cell);
            if (columnIndex == targetColumn) {
                return new PositionedCell(cell, columnIndex, columnSpan);
            }
            columnIndex += columnSpan;
        }
        return null;
    }

    private static void buildGenericBlankCellRules(List<XWPFTableRow> rows, int columnCount,
                                                    List<Map<String, Object>> rules) {
        Set<String> existing = new HashSet<>();
        for (Map<String, Object> rule : rules) {
            existing.add(rule.get("rowIndex") + ":" + rule.get("columnIndex"));
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int columnIndex = 0;
            for (XWPFTableCell cell : rows.get(rowIndex).getTableCells()) {
                int columnSpan = resolveColumnSpan(cell);
                String key = rowIndex + ":" + columnIndex;
                if (!hasNestedTable(cell) && !isVerticalMergeFollower(cell) && normalizeText(cell.getText()).isBlank()
                        && !hasDiagonalBorder(cell) && !existing.contains(key)) {
                    String label = resolveBlankCellLabel(rows, rowIndex, columnIndex, columnSpan, columnCount);
                    if (!label.isBlank()) {
                        String valueType = inferValueType(label);
                        String componentFlag = switch (valueType) {
                            case "NUMBER" -> "input-number";
                            case "DATE" -> "date";
                            case "SIGNATURE" -> "signature";
                            default -> "input-text";
                        };
                        rules.add(buildRule(rowIndex, columnIndex, valueType, componentFlag, label, Map.of()));
                        existing.add(key);
                    }
                }
                columnIndex += columnSpan;
            }
        }
    }

    private static String resolveBlankCellLabel(List<XWPFTableRow> rows, int rowIndex, int columnIndex,
                                                int columnSpan, int columnCount) {
        for (int previousRowIndex = rowIndex - 1; previousRowIndex >= 0; previousRowIndex--) {
            PositionedCell candidate = findCoveringCell(rows.get(previousRowIndex), columnIndex);
            if (candidate == null || isVerticalMergeFollower(candidate.cell()) || hasDiagonalBorder(candidate.cell())) {
                continue;
            }
            String text = normalizeBlankRuleLabel(candidate.cell().getText());
            if (isMeaningfulFieldLabel(text, candidate.columnSpan(), columnSpan, columnCount)) {
                return text;
            }
        }
        PositionedCell previous = null;
        int currentColumn = 0;
        for (XWPFTableCell cell : rows.get(rowIndex).getTableCells()) {
            int currentSpan = resolveColumnSpan(cell);
            if (currentColumn == columnIndex) {
                break;
            }
            previous = new PositionedCell(cell, currentColumn, currentSpan);
            currentColumn += currentSpan;
        }
        if (previous == null || previous.columnIndex() + previous.columnSpan() != columnIndex) {
            return "";
        }
        String text = normalizeBlankRuleLabel(previous.cell().getText());
        return isMeaningfulFieldLabel(text, previous.columnSpan(), columnSpan, columnCount) ? text : "";
    }

    private static PositionedCell findCoveringCell(XWPFTableRow row, int targetColumn) {
        int columnIndex = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            int columnSpan = resolveColumnSpan(cell);
            if (columnIndex <= targetColumn && targetColumn < columnIndex + columnSpan) {
                return new PositionedCell(cell, columnIndex, columnSpan);
            }
            columnIndex += columnSpan;
        }
        return null;
    }

    private static boolean isMeaningfulFieldLabel(String text, int labelSpan, int valueSpan, int columnCount) {
        if (text.isBlank() || text.contains("□") || text.startsWith("备注")) {
            return false;
        }
        String compact = text.replaceAll("\\s+", "");
        if (Set.of("参考值", "实际", "结果", "检查要求", "要求", "项目").contains(compact)) {
            return false;
        }
        return labelSpan < Math.max(columnCount / 2, valueSpan * 4);
    }

    private static String normalizeBlankRuleLabel(String text) {
        return normalizeText(text).replaceAll("[：:]$", "").trim();
    }

    private static String inferValueType(String label) {
        String normalized = normalizeText(label);
        if (normalized.contains("复核人") || normalized.contains("操作人") || normalized.contains("记录人")) {
            return "SIGNATURE";
        }
        if (normalized.contains("日期")) {
            return "DATE";
        }
        if (normalized.contains("数量") || normalized.contains("次数") || normalized.contains("序号")
                || normalized.toLowerCase(Locale.ROOT).contains("pcs")) {
            return "NUMBER";
        }
        return "STRING";
    }

    private static boolean hasDiagonalBorder(XWPFTableCell cell) {
        return resolveDiagonalDirection(cell) != null;
    }

    private static String resolveDiagonalDirection(XWPFTableCell cell) {
        CTTcPr properties = cell == null ? null : cell.getCTTc().getTcPr();
        if (properties == null || !properties.isSetTcBorders()) {
            return null;
        }
        CTTcBorders borders = properties.getTcBorders();
        boolean topLeftToBottomRight = isVisibleBorder(borders.getTl2Br());
        boolean topRightToBottomLeft = isVisibleBorder(borders.getTr2Bl());
        if (topLeftToBottomRight && topRightToBottomLeft) {
            return "BOTH";
        }
        if (topRightToBottomLeft) {
            return "TR2BL";
        }
        return topLeftToBottomRight ? "TL2BR" : null;
    }

    private static boolean isVisibleBorder(CTBorder border) {
        if (border == null) {
            return false;
        }
        STBorder.Enum value = border.getVal();
        return value == null || !("none".equalsIgnoreCase(value.toString())
                || "nil".equalsIgnoreCase(value.toString()));
    }

    private static int resolveStyleIndex(XWPFTableCell cell, List<Map<String, Object>> styles,
                                         Map<String, Integer> styleIndexes) {
        String align = "left";
        if (!cell.getParagraphs().isEmpty()) {
            align = switch (cell.getParagraphs().get(0).getAlignment()) {
                case CENTER -> "center";
                case RIGHT -> "right";
                default -> "left";
            };
        }
        boolean bold = cell.getParagraphs().stream()
                .flatMap(paragraph -> paragraph.getRuns().stream())
                .anyMatch(run -> run.isBold());
        int fontPointSize = cell.getParagraphs().stream()
                .flatMap(paragraph -> paragraph.getRuns().stream())
                .mapToInt(run -> run.getFontSize() > 0 ? run.getFontSize() : 0)
                .filter(size -> size > 0)
                .findFirst().orElse(10);
        int fontSize = Math.max(8, Math.round(fontPointSize * 96F / 72F));
        String key = align + "|" + bold + "|" + fontSize;
        Integer existing = styleIndexes.get(key);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> style = new LinkedHashMap<>();
        style.put("align", align);
        style.put("valign", "middle");
        style.put("textwrap", true);
        Map<String, Object> font = new LinkedHashMap<>();
        font.put("size", fontSize);
        if (bold) {
            font.put("bold", true);
        }
        style.put("font", font);
        style.put("border", Map.of(
                "bottom", List.of("thin", "#000"),
                "top", List.of("thin", "#000"),
                "left", List.of("thin", "#000"),
                "right", List.of("thin", "#000")));
        int index = styles.size();
        styles.add(style);
        styleIndexes.put(key, index);
        return index;
    }

    private static String toMergeRange(int startRow, int startColumn, int endRow, int endColumn) {
        return toCellReference(startRow, startColumn) + ":" + toCellReference(endRow, endColumn);
    }

    private static String toCellReference(int rowIndex, int columnIndex) {
        int value = columnIndex + 1;
        StringBuilder column = new StringBuilder();
        while (value > 0) {
            int remainder = (value - 1) % 26;
            column.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return column + String.valueOf(rowIndex + 1);
    }

    private static int resolveRowHeight(XWPFTableRow row) {
        if (row.getCtRow().getTrPr() == null || row.getCtRow().getTrPr().getTrHeightList().isEmpty()
                || row.getCtRow().getTrPr().getTrHeightList().get(0).getVal() == null) {
            return DEFAULT_ROW_HEIGHT;
        }
        Object height = row.getCtRow().getTrPr().getTrHeightList().get(0).getVal();
        if (!(height instanceof Number number)) {
            return DEFAULT_ROW_HEIGHT;
        }
        return Math.max(24, Math.round(number.floatValue() / 15F));
    }

    private static int toPixels(Object width) {
        if (!(width instanceof Number number) || number.doubleValue() <= 0) {
            return DEFAULT_COLUMN_WIDTH;
        }
        return Math.max(4, Math.round(number.floatValue() * 96F / 1440F));
    }

    private static InlineTextInput parseInlineTextInput(String text) {
        Matcher matcher = INLINE_TEXT_INPUT_PATTERN.matcher(normalizeText(text));
        return matcher.matches() ? new InlineTextInput(matcher.group(1), matcher.group(2)) : null;
    }

    private static List<InlineInputSegment> parseSegmentedInputCell(String text) {
        String normalized = normalizeText(text)
                .replace('\u00A0', ' ')
                .replace('\u3000', ' ');
        String compact = normalized.replaceAll("\\s+", "");
        if (compact.contains("合格数量") && compact.contains("不合格数量")
                && compact.contains("不合格评审报告编号")) {
            List<InlineInputSegment> segments = new ArrayList<>();
            Matcher matcher = Pattern.compile("([^；;：:\\n]+?)[：:]([^；;\\n]*)[；;]?").matcher(normalized);
            while (matcher.find()) {
                String label = normalizeRuleLabel(matcher.group(1));
                if ("合格数量".equals(label) || "不合格数量".equals(label)) {
                    segments.add(new InlineInputSegment(label, label + "：", "NUMBER", "input-number", 1));
                } else if (label.contains("不合格评审报告编号")) {
                    segments.add(new InlineInputSegment("不合格评审报告编号（若有）",
                            "不合格评审报告编号（若有）：", "STRING", "input-text", 3));
                }
            }
            return segments;
        }
        if (compact.startsWith("备注：特殊内容") || compact.startsWith("备注:特殊内容")) {
            return List.of(new InlineInputSegment("备注", normalized, "STRING", "textarea", 4));
        }
        return List.of();
    }

    private static String normalizeRuleLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.replaceAll("^[；;\\s]+", "")
                .replaceAll("[：:]$", "")
                .trim();
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static String resolveCellText(XWPFTableCell cell) {
        String paragraphText = cell.getParagraphs().stream()
                .map(paragraph -> normalizeText(paragraph.getText()))
                .filter(text -> !text.isBlank())
                .collect(java.util.stream.Collectors.joining("\n"));
        return paragraphText.isBlank() ? normalizeText(cell.getText()) : paragraphText;
    }

    private record InlineTextInput(String label, String underline) {
    }

    private record InlineInputSegment(String ruleLabel, String displayLabel, String valueType,
                                      String componentFlag, int preferredLabelSpan) {
    }

    private record InlineInputPlacement(InlineInputSegment segment, int labelColumn, int labelSpan,
                                        int inputColumn, int inputSpan) {
    }

    private record PositionedCell(XWPFTableCell cell, int columnIndex, int columnSpan) {
    }

    private record RowExpansionPlan(int sourceStartRow, List<Integer> visualStarts,
                                    List<Integer> expansions, int totalRows) {

        private int visualStart(int sourceRowIndex) {
            return visualStarts.get(sourceRowIndex - sourceStartRow);
        }

        private int expansion(int sourceRowIndex) {
            return expansions.get(sourceRowIndex - sourceStartRow);
        }

        private int visualSpan(int sourceRowIndex, int sourceRowSpan) {
            int span = 0;
            int startOffset = sourceRowIndex - sourceStartRow;
            for (int offset = 0; offset < sourceRowSpan; offset++) {
                span += expansions.get(startOffset + offset);
            }
            return span;
        }
    }

}
