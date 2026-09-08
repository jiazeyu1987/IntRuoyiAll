package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HwpfWordTableVisualSchemaBuilder {

    private static final int DEFAULT_COLUMN_WIDTH = 160;
    private static final int DEFAULT_ROW_HEIGHT = 30;
    private static final Pattern CHECKBOX_OPTION_PATTERN =
            Pattern.compile("□\\s*([^□]+?)(?=□|$)");

    private HwpfWordTableVisualSchemaBuilder() {
    }

    static String build(HWPFDocument document) throws Exception {
        List<TableLayout> tableLayouts = collectTopLevelTables(document.getRange()).stream()
                .map(HwpfWordTableVisualSchemaBuilder::parseTable)
                .filter(table -> !table.rows().isEmpty())
                .toList();
        if (tableLayouts.isEmpty()) {
            throw new IllegalArgumentException("no recognizable Word table candidate");
        }
        int columnCount = tableLayouts.stream()
                .mapToInt(TableLayout::columnCount)
                .max()
                .orElse(1);
        List<Map<String, Object>> styles = new ArrayList<>();
        Map<String, Integer> styleIndexes = new HashMap<>();
        List<String> merges = new ArrayList<>();
        List<Map<String, Object>> cellRules = new ArrayList<>();
        Map<String, Object> rows = new LinkedHashMap<>();
        int rowIndex = 0;
        for (TableLayout tableLayout : tableLayouts) {
            for (RowLayout rowLayout : tableLayout.rows()) {
                Map<String, Object> cells = new LinkedHashMap<>();
                for (CellLayout cell : rowLayout.cells()) {
                    int styleIndex = resolveStyleIndex(cell, styles, styleIndexes);
                    cells.put(String.valueOf(cell.columnIndex()), layoutCell(cell.text(), rowIndex,
                            cell.columnIndex(), cell.rowSpan(), cell.colSpan(), styleIndex, merges));
                }
                rows.put(String.valueOf(rowIndex), Map.of(
                        "height", rowLayout.heightPx(),
                        "cells", cells));
                rowIndex++;
            }
            appendCellRules(tableLayout, rowIndex - tableLayout.rows().size(), columnCount, cellRules);
        }
        rows.put("len", rowIndex);

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("cols", buildColumns(tableLayouts, columnCount));
        layout.put("rows", rows);
        layout.put("styles", styles);
        layout.put("merges", merges);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("sheetLayoutJson", JsonUtils.toJsonString(layout));
        schema.put("cellRules", cellRules);
        schema.put("signatureCellMarkers", buildSignatureCellMarkers(cellRules));
        schema.put("assistRows", List.of());
        schema.put("fillAssignments", List.of());
        return JsonUtils.toJsonString(schema);
    }

    private static List<Table> collectTopLevelTables(Range range) throws Exception {
        Constructor<TableIterator> constructor = TableIterator.class.getDeclaredConstructor(Range.class, int.class);
        constructor.setAccessible(true);
        TableIterator iterator = constructor.newInstance(range, 1);
        List<Table> tables = new ArrayList<>();
        while (iterator.hasNext()) {
            tables.add(iterator.next());
        }
        return tables;
    }

    private static TableLayout parseTable(Table table) {
        List<Integer> visualColumnBoundaries = resolveVisualColumnBoundaries(table);
        int fallbackColumnCount = 0;
        List<RowLayout> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            int rowHeight = toPixels(row.getRowHeight(), DEFAULT_ROW_HEIGHT);
            List<CellLayout> cells = new ArrayList<>();
            int logicalColumnIndex = 0;
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                if (isMergedFollower(cell)) {
                    continue;
                }
                int columnIndex = visualColumnBoundaries.size() > 1
                        ? resolveStartColumnIndex(visualColumnBoundaries, cell) : logicalColumnIndex;
                int colSpan = visualColumnBoundaries.size() > 1
                        ? resolveVisualColSpan(visualColumnBoundaries, cell, columnIndex) : resolveColSpan(row, cellIndex);
                int rowSpan = resolveRowSpan(table, rowIndex, cellIndex);
                cells.add(new CellLayout(
                        normalizeCellText(cell.text()),
                        columnIndex,
                        colSpan,
                        rowSpan,
                        resolveHorizontalAlign(cell),
                        resolveVerticalAlign(cell),
                        resolveBold(cell),
                        resolveFontSize(cell),
                        resolveBorderStyle(cell.getBrcTop()),
                        resolveBorderStyle(cell.getBrcBottom()),
                        resolveBorderStyle(cell.getBrcLeft()),
                        resolveBorderStyle(cell.getBrcRight())));
                logicalColumnIndex += resolveColSpan(row, cellIndex);
                fallbackColumnCount = Math.max(fallbackColumnCount, columnIndex + colSpan);
            }
            rows.add(new RowLayout(rowHeight, cells));
        }
        int columnCount = visualColumnBoundaries.size() > 1 ? visualColumnBoundaries.size() - 1 : fallbackColumnCount;
        return new TableLayout(Math.max(1, columnCount), resolveVisualColumnWidths(visualColumnBoundaries), rows);
    }

    private static Map<String, Object> buildColumns(List<TableLayout> tableLayouts, int columnCount) {
        Map<String, Object> columns = new LinkedHashMap<>();
        List<Integer> widths = tableLayouts.stream()
                .map(TableLayout::columnWidths)
                .filter(columnWidths -> columnWidths.size() == columnCount)
                .findFirst()
                .orElse(List.of());
        for (int index = 0; index < columnCount; index++) {
            int width = index < widths.size() ? widths.get(index) : DEFAULT_COLUMN_WIDTH;
            columns.put(String.valueOf(index), Map.of("width", width));
        }
        columns.put("len", columnCount);
        return columns;
    }

    private static void appendCellRules(TableLayout tableLayout, int rowOffset, int columnCount,
                                        List<Map<String, Object>> rules) {
        Set<String> existing = new LinkedHashSet<>();
        for (Map<String, Object> rule : rules) {
            existing.add(rule.get("rowIndex") + ":" + rule.get("columnIndex"));
        }
        for (int rowIndex = 0; rowIndex < tableLayout.rows().size(); rowIndex++) {
            for (CellLayout cell : tableLayout.rows().get(rowIndex).cells()) {
                String text = normalizeText(cell.text());
                int absoluteRowIndex = rowOffset + rowIndex;
                String key = absoluteRowIndex + ":" + cell.columnIndex();
                if (countCheckboxMarkers(text) > 0 && existing.add(key)) {
                    rules.add(buildCheckboxRule(absoluteRowIndex, cell.columnIndex(), text));
                    continue;
                }
                if (!text.isBlank()) {
                    continue;
                }
                String label = resolveBlankCellLabel(tableLayout.rows(), rowIndex, cell.columnIndex(),
                        cell.colSpan(), columnCount);
                if (!label.isBlank() && existing.add(key)) {
                    String valueType = inferValueType(label);
                    rules.add(buildRule(absoluteRowIndex, cell.columnIndex(), valueType, componentFlag(valueType),
                            label, Map.of()));
                }
            }
        }
    }

    private static String resolveBlankCellLabel(List<RowLayout> rows, int rowIndex, int columnIndex,
                                                int colSpan, int columnCount) {
        CellLayout previous = previousCell(rows.get(rowIndex).cells(), columnIndex);
        if (previous != null) {
            String label = normalizeBlankRuleLabel(previous.text());
            if (isMeaningfulFieldLabel(label, previous.colSpan(), colSpan, columnCount)) {
                return label;
            }
        }
        for (int previousRowIndex = rowIndex - 1; previousRowIndex >= 0; previousRowIndex--) {
            CellLayout candidate = coveringCell(rows.get(previousRowIndex).cells(), columnIndex);
            if (candidate == null) {
                continue;
            }
            String label = normalizeBlankRuleLabel(candidate.text());
            if (isMeaningfulFieldLabel(label, candidate.colSpan(), colSpan, columnCount)) {
                return label;
            }
        }
        return "";
    }

    private static CellLayout previousCell(List<CellLayout> cells, int columnIndex) {
        CellLayout previous = null;
        for (CellLayout cell : cells) {
            if (cell.columnIndex() >= columnIndex) {
                break;
            }
            if (cell.columnIndex() + cell.colSpan() == columnIndex) {
                previous = cell;
            }
        }
        return previous;
    }

    private static CellLayout coveringCell(List<CellLayout> cells, int columnIndex) {
        for (CellLayout cell : cells) {
            if (cell.columnIndex() <= columnIndex && columnIndex < cell.columnIndex() + cell.colSpan()) {
                return cell;
            }
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

    private static List<Map<String, Object>> buildSignatureCellMarkers(List<Map<String, Object>> cellRules) {
        List<Map<String, Object>> markers = new ArrayList<>();
        Set<String> existing = new LinkedHashSet<>();
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

    private static Map<String, Object> layoutCell(String text, int rowIndex, int columnIndex,
                                                  int rowSpan, int columnSpan, int styleIndex,
                                                  List<String> merges) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("text", text);
        cell.put("style", styleIndex);
        if (rowSpan > 1 || columnSpan > 1) {
            cell.put("merge", List.of(rowSpan - 1, columnSpan - 1));
            merges.add(toMergeRange(rowIndex, columnIndex,
                    rowIndex + rowSpan - 1, columnIndex + columnSpan - 1));
        }
        return cell;
    }

    private static List<Integer> resolveVisualColumnBoundaries(Table table) {
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

    private static List<Integer> resolveVisualColumnWidths(List<Integer> visualColumnBoundaries) {
        if (visualColumnBoundaries == null || visualColumnBoundaries.size() < 2) {
            return List.of();
        }
        List<Integer> widths = new ArrayList<>();
        for (int index = 0; index < visualColumnBoundaries.size() - 1; index++) {
            widths.add(toWidthUnits(visualColumnBoundaries.get(index + 1) - visualColumnBoundaries.get(index)));
        }
        return widths;
    }

    private static int resolveStartColumnIndex(List<Integer> visualColumnBoundaries, TableCell cell) {
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

    private static int resolveVisualColSpan(List<Integer> visualColumnBoundaries, TableCell cell,
                                            int startColumnIndex) {
        int right = cell.getLeftEdge() + Math.max(1, cell.getWidth());
        int endBoundaryIndex = Math.min(visualColumnBoundaries.size() - 1,
                Math.max(startColumnIndex + 1, startColumnIndex));
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

    private static boolean isMergedFollower(TableCell cell) {
        return (cell.isMerged() && !cell.isFirstMerged())
                || (cell.isVerticallyMerged() && !cell.isFirstVerticallyMerged());
    }

    private static int resolveColSpan(TableRow row, int startCellIndex) {
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

    private static int resolveRowSpan(Table table, int rowIndex, int cellIndex) {
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

    private static int resolveStyleIndex(CellLayout cell, List<Map<String, Object>> styles,
                                         Map<String, Integer> styleIndexes) {
        String key = cell.horizontalAlign() + "|" + cell.verticalAlign() + "|" + cell.bold() + "|"
                + cell.fontSize() + "|" + cell.topBorderStyle() + "|" + cell.bottomBorderStyle()
                + "|" + cell.leftBorderStyle() + "|" + cell.rightBorderStyle();
        Integer existing = styleIndexes.get(key);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> style = new LinkedHashMap<>();
        style.put("align", cell.horizontalAlign());
        style.put("valign", cell.verticalAlign());
        style.put("textwrap", true);
        Map<String, Object> font = new LinkedHashMap<>();
        font.put("size", Math.max(8, Math.round(cell.fontSize() * 96F / 72F)));
        if (cell.bold()) {
            font.put("bold", true);
        }
        style.put("font", font);
        style.put("border", buildBorder(cell));
        int index = styles.size();
        styles.add(style);
        styleIndexes.put(key, index);
        return index;
    }

    private static Map<String, Object> buildBorder(CellLayout cell) {
        Map<String, Object> border = new LinkedHashMap<>();
        putBorder(border, "top", cell.topBorderStyle());
        putBorder(border, "bottom", cell.bottomBorderStyle());
        putBorder(border, "left", cell.leftBorderStyle());
        putBorder(border, "right", cell.rightBorderStyle());
        return border;
    }

    private static void putBorder(Map<String, Object> border, String side, String style) {
        if (style != null) {
            border.put(side, List.of(style, "#000"));
        }
    }

    private static boolean resolveBold(TableCell cell) {
        CharacterRun characterRun = firstCharacterRun(cell);
        return characterRun != null && characterRun.isBold();
    }

    private static int resolveFontSize(TableCell cell) {
        CharacterRun characterRun = firstCharacterRun(cell);
        if (characterRun == null) {
            return 10;
        }
        int raw = characterRun.getFontSize();
        return Math.max(10, raw > 20 ? raw / 2 : raw);
    }

    private static String resolveHorizontalAlign(TableCell cell) {
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

    private static String resolveVerticalAlign(TableCell cell) {
        return switch (cell.getVertAlign()) {
            case 1 -> "top";
            case 2 -> "middle";
            case 3 -> "bottom";
            default -> "middle";
        };
    }

    private static Paragraph firstParagraph(TableCell cell) {
        return cell.numParagraphs() <= 0 ? null : cell.getParagraph(0);
    }

    private static CharacterRun firstCharacterRun(TableCell cell) {
        Paragraph paragraph = firstParagraph(cell);
        return paragraph == null || paragraph.numCharacterRuns() <= 0 ? null : paragraph.getCharacterRun(0);
    }

    private static String resolveBorderStyle(BorderCode borderCode) {
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

    private static String componentFlag(String valueType) {
        return switch (valueType) {
            case "NUMBER" -> "input-number";
            case "DATE" -> "date";
            case "SIGNATURE" -> "signature";
            default -> "input-text";
        };
    }

    private static int countCheckboxMarkers(String text) {
        return (int) text.chars().filter(character -> character == '□').count();
    }

    private static String normalizeBlankRuleLabel(String text) {
        return normalizeText(text).replaceAll("[：:]$", "").trim();
    }

    private static String normalizeCellText(String text) {
        String normalized = normalizeText(text)
                .replace('\u0007', ' ')
                .replace('\u0008', ' ')
                .replace('\u0000', ' ')
                .replaceAll("[\\n]{3,}", "\n\n")
                .trim();
        return normalized.isBlank() ? "" : normalized;
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static int toPixels(int twips, int defaultValue) {
        if (twips <= 0) {
            return defaultValue;
        }
        return Math.max(defaultValue, Math.round(twips / 15.0f));
    }

    private static int toWidthUnits(int twips) {
        if (twips <= 0) {
            return 1;
        }
        return Math.max(1, Math.round(twips / 15.0f));
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

    private record TableLayout(int columnCount, List<Integer> columnWidths, List<RowLayout> rows) {
    }

    private record RowLayout(int heightPx, List<CellLayout> cells) {
    }

    private record CellLayout(String text, int columnIndex, int colSpan, int rowSpan,
                              String horizontalAlign, String verticalAlign, boolean bold, int fontSize,
                              String topBorderStyle, String bottomBorderStyle,
                              String leftBorderStyle, String rightBorderStyle) {
    }
}
