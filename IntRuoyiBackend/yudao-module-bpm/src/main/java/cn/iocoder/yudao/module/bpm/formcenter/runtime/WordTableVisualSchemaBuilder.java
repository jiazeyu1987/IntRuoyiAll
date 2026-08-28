package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        int columnCount = resolveColumnCount(table);
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("cols", buildColumns(table, columnCount));
        layout.put("rows", buildRows(table, columnCount));
        layout.put("merges", List.of());

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("sheetLayoutJson", JsonUtils.toJsonString(layout));
        schema.put("cellRules", buildCellRules(table, columnCount));
        schema.put("signatureCellMarkers", buildSignatureCellMarkers(table, columnCount));
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

    private static Map<String, Object> buildRows(XWPFTable table, int columnCount) {
        Map<String, Object> rows = new LinkedHashMap<>();
        List<XWPFTableRow> tableRows = table.getRows();
        for (int rowIndex = 0; rowIndex < tableRows.size(); rowIndex++) {
            XWPFTableRow row = tableRows.get(rowIndex);
            Map<String, Object> cells = new LinkedHashMap<>();
            int columnIndex = 0;
            List<XWPFTableCell> rowCells = row.getTableCells();
            for (int physicalCellIndex = 0; physicalCellIndex < rowCells.size(); physicalCellIndex++) {
                XWPFTableCell cell = rowCells.get(physicalCellIndex);
                int columnSpan = resolveColumnSpan(cell);
                if (isVerticalMergeFollower(cell)) {
                    columnIndex += columnSpan;
                    continue;
                }
                int rowSpan = isVerticalMergeRestart(cell)
                        ? resolveVerticalSpan(table, rowIndex, physicalCellIndex) : 1;
                List<InlineInputSegment> segmentedInputs = parseSegmentedInputCell(cell.getText());
                if (!segmentedInputs.isEmpty()) {
                    appendSegmentedInputLayout(cells, columnIndex, rowSpan, columnSpan, segmentedInputs);
                    columnIndex += columnSpan;
                    continue;
                }
                InlineTextInput inlineInput = parseInlineTextInput(cell.getText());
                if (inlineInput == null) {
                    cells.put(String.valueOf(columnIndex), layoutCell(normalizeText(cell.getText()), rowSpan, columnSpan));
                    columnIndex += columnSpan;
                    continue;
                }

                int visualSpan = Math.max(columnSpan, 2);
                cells.put(String.valueOf(columnIndex), layoutCell(inlineInput.label(), rowSpan, 1));
                cells.put(String.valueOf(columnIndex + 1), layoutCell("", rowSpan, visualSpan - 1));
                columnIndex += visualSpan;
            }
            rows.put(String.valueOf(rowIndex), Map.of(
                    "height", resolveRowHeight(row),
                    "cells", cells));
        }
        rows.put("len", tableRows.size());
        return rows;
    }

    private static List<Map<String, Object>> buildCellRules(XWPFTable table, int columnCount) {
        List<Map<String, Object>> rules = new ArrayList<>();
        List<XWPFTableRow> tableRows = table.getRows();
        buildHeaderRules(tableRows.isEmpty() ? null : tableRows.get(0), rules);
        Map<Integer, String> headerLabels = resolveInspectionHeaderLabels(table, columnCount);
        int headerRowIndex = resolveInspectionHeaderRowIndex(table);
        for (int rowIndex = 0; rowIndex < tableRows.size(); rowIndex++) {
            if (rowIndex <= headerRowIndex) {
                continue;
            }
            int columnIndex = 0;
            for (XWPFTableCell cell : tableRows.get(rowIndex).getTableCells()) {
                int columnSpan = resolveColumnSpan(cell);
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
        return rules;
    }

    private static void buildHeaderRules(XWPFTableRow row, List<Map<String, Object>> rules) {
        if (row == null) {
            return;
        }
        int columnIndex = 0;
        String pendingLabel = null;
        for (XWPFTableCell cell : row.getTableCells()) {
            int columnSpan = resolveColumnSpan(cell);
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

    private static void appendSegmentedInputLayout(Map<String, Object> cells, int startColumn, int rowSpan,
                                                   int columnSpan, List<InlineInputSegment> segments) {
        for (InlineInputPlacement placement : resolveSegmentedInputPlacements(startColumn, columnSpan, segments)) {
            cells.put(String.valueOf(placement.labelColumn()),
                    layoutCell(placement.segment().displayLabel(), rowSpan, placement.labelSpan()));
            cells.put(String.valueOf(placement.inputColumn()),
                    layoutCell("", rowSpan, placement.inputSpan()));
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

    private static List<Map<String, Object>> buildSignatureCellMarkers(XWPFTable table, int columnCount) {
        List<Map<String, Object>> markers = new ArrayList<>();
        Map<Integer, String> headerLabels = resolveInspectionHeaderLabels(table, columnCount);
        int headerRowIndex = resolveInspectionHeaderRowIndex(table);
        Integer signatureColumn = headerLabels.entrySet().stream()
                .filter(entry -> entry.getValue().contains("复核人"))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        if (signatureColumn == null) {
            return markers;
        }
        for (int rowIndex = headerRowIndex + 1; rowIndex < table.getRows().size(); rowIndex++) {
            if (!isFooterRow(rowText(table.getRow(rowIndex)))) {
                markers.add(Map.of(
                        "rowIndex", rowIndex,
                        "columnIndex", signatureColumn,
                        "enabled", true,
                        "signatureCellKey", rowIndex + ":" + signatureColumn,
                        "actionType", "FORM_REVIEW",
                        "label", "复核人/日期"));
            }
        }
        return markers;
    }

    private static Map<Integer, String> resolveInspectionHeaderLabels(XWPFTable table, int columnCount) {
        int headerRowIndex = resolveInspectionHeaderRowIndex(table);
        if (headerRowIndex < 0) {
            return Map.of();
        }
        Map<Integer, String> labels = new LinkedHashMap<>();
        int columnIndex = 0;
        for (XWPFTableCell cell : table.getRow(headerRowIndex).getTableCells()) {
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
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            String text = rowText(table.getRow(rowIndex));
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

    private static Map<String, Object> layoutCell(String text, int rowSpan, int columnSpan) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("text", text);
        if (rowSpan > 1 || columnSpan > 1) {
            cell.put("merge", List.of(rowSpan - 1, columnSpan - 1));
        }
        return cell;
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

    private static int resolveVerticalSpan(XWPFTable table, int rowIndex, int cellIndex) {
        int span = 1;
        for (int nextRowIndex = rowIndex + 1; nextRowIndex < table.getRows().size(); nextRowIndex++) {
            List<XWPFTableCell> nextCells = table.getRow(nextRowIndex).getTableCells();
            if (cellIndex >= nextCells.size() || !isVerticalMergeFollower(nextCells.get(cellIndex))) {
                break;
            }
            span++;
        }
        return span;
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

    private record InlineTextInput(String label, String underline) {
    }

    private record InlineInputSegment(String ruleLabel, String displayLabel, String valueType,
                                      String componentFlag, int preferredLabelSpan) {
    }

    private record InlineInputPlacement(InlineInputSegment segment, int labelColumn, int labelSpan,
                                        int inputColumn, int inputSpan) {
    }

}
