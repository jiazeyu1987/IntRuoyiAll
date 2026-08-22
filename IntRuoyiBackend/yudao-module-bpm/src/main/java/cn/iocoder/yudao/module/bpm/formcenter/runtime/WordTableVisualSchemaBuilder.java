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
        schema.put("signatureCellMarkers", List.of());
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
        for (int rowIndex = 0; rowIndex < tableRows.size(); rowIndex++) {
            int columnIndex = 0;
            List<XWPFTableCell> rowCells = tableRows.get(rowIndex).getTableCells();
            for (int physicalCellIndex = 0; physicalCellIndex < rowCells.size(); physicalCellIndex++) {
                XWPFTableCell cell = rowCells.get(physicalCellIndex);
                int columnSpan = resolveColumnSpan(cell);
                if (isVerticalMergeFollower(cell)) {
                    columnIndex += columnSpan;
                    continue;
                }
                InlineTextInput inlineInput = parseInlineTextInput(cell.getText());
                if (inlineInput == null) {
                    columnIndex += columnSpan;
                    continue;
                }
                int inputColumnIndex = columnIndex + 1;
                if (inputColumnIndex >= columnCount) {
                    throw new IllegalStateException("inline text input exceeds source table columns");
                }
                rules.add(Map.of(
                        "rowIndex", rowIndex,
                        "columnIndex", inputColumnIndex,
                        "valueType", "STRING",
                        "componentFlag", "input-text",
                        "required", false,
                        "label", inlineInput.label(),
                        "placeholder", "",
                        "source", "AUTO",
                        "reviewed", false,
                        "confidence", 1));
                columnIndex += Math.max(columnSpan, 2);
            }
        }
        return rules;
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

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private record InlineTextInput(String label, String underline) {
    }

}
