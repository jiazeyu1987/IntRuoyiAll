package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class MesProBatchRecordReportStyleEnhancer {

    private static final String SECTION_BAR_COLOR = "#d9d9d9";
    private static final String HEADER_GROUP_COLOR = "#ececec";
    private static final String HEADER_LABEL_COLOR = "#f7f7f7";
    private static final String BAND_BORDER_STYLE = "medium";
    private static final String PCS_TEXT_COLOR = "#c00000";

    public String enhance(String jsonStr, MesProBatchRecordParsedTable parsedTable) {
        JSONObject root = JSON.parseObject(jsonStr);
        JSONArray styles = root.getJSONArray("styles");
        JSONObject rowsObject = root.getJSONObject("rows");
        if (styles == null || rowsObject == null) {
            return jsonStr;
        }

        Map<String, Integer> styleCache = new TreeMap<>();
        Map<Integer, Map<Integer, MesProBatchRecordParsedCell>> parsedCellsByRowColumn =
                placeParsedCellsByRowColumn(parsedTable);
        for (int rowIndex = 0; rowIndex < parsedTable.getRows().size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> row = parsedTable.getRows().get(rowIndex);
            JSONObject rowObject = rowsObject.getJSONObject(String.valueOf(rowIndex));
            if (rowObject == null) {
                continue;
            }
            JSONObject cellsObject = rowObject.getJSONObject("cells");
            if (cellsObject == null || cellsObject.isEmpty()) {
                continue;
            }
            List<String> sortedColumns = new ArrayList<>(cellsObject.keySet());
            sortedColumns.sort(Comparator.comparingInt(Integer::parseInt));
            Map<Integer, MesProBatchRecordParsedCell> parsedCellsByColumn =
                    parsedCellsByRowColumn.getOrDefault(rowIndex, Map.of());
            for (String columnKey : sortedColumns) {
                JSONObject cellObject = cellsObject.getJSONObject(columnKey);
                if (cellObject == null) {
                    continue;
                }
                MesProBatchRecordParsedCell parsedCell = parsedCellsByColumn.get(Integer.parseInt(columnKey));
                if (parsedCell == null) {
                    continue;
                }
                CellDecoration decoration = resolveCellDecoration(row, parsedCell);
                String textColor = resolveCellTextColor(parsedCell.getText());
                Integer fontSizeOverride = resolveFontSizeOverride(row, parsedCell);
                String verticalAlignOverride = resolveVerticalAlignOverride(row, parsedCell);
                if (decoration == null && textColor == null
                        && fontSizeOverride == null && verticalAlignOverride == null) {
                    continue;
                }
                int originalStyleIndex = cellObject.getIntValue("style");
                int enhancedStyleIndex = resolveEnhancedStyleIndex(styles, styleCache, originalStyleIndex,
                        decoration, textColor, fontSizeOverride, verticalAlignOverride);
                cellObject.put("style", enhancedStyleIndex);
            }
        }
        return root.toJSONString();
    }

    private Map<Integer, Map<Integer, MesProBatchRecordParsedCell>> placeParsedCellsByRowColumn(
            MesProBatchRecordParsedTable parsedTable) {
        Map<Integer, Map<Integer, MesProBatchRecordParsedCell>> result = new HashMap<>();
        Map<Integer, Integer> blockedUntilRowByColumn = new HashMap<>();
        for (int rowIndex = 0; rowIndex < parsedTable.getRows().size(); rowIndex++) {
            int columnIndex = 0;
            for (MesProBatchRecordParsedCell cell : parsedTable.getRows().get(rowIndex)) {
                while (blockedUntilRowByColumn.getOrDefault(columnIndex, -1) >= rowIndex) {
                    columnIndex++;
                }
                result.computeIfAbsent(rowIndex, key -> new HashMap<>()).put(columnIndex, cell);
                if (cell.getRowSpan() > 1) {
                    for (int offset = 0; offset < Math.max(cell.getColSpan(), 1); offset++) {
                        blockedUntilRowByColumn.put(columnIndex + offset, rowIndex + cell.getRowSpan() - 1);
                    }
                }
                columnIndex += Math.max(1, cell.getColSpan());
            }
        }
        return result;
    }

    private CellDecoration resolveCellDecoration(List<MesProBatchRecordParsedCell> row,
                                                 MesProBatchRecordParsedCell cell) {
        if (cell == null) {
            return null;
        }
        String compactText = compactText(cell.getText());
        if (compactText.isBlank()) {
            return null;
        }
        if (isSectionRow(row, cell)) {
            return new CellDecoration(SECTION_BAR_COLOR, BorderDecoration.horizontalBand(BAND_BORDER_STYLE));
        }
        if (isHeaderCell(row, cell)) {
            if (isStrongHeaderSignal(cell)) {
                return new CellDecoration(HEADER_GROUP_COLOR, BorderDecoration.horizontalBand(BAND_BORDER_STYLE));
            }
            return new CellDecoration(HEADER_LABEL_COLOR, BorderDecoration.none());
        }
        if (isGenericLabelValueCell(row, cell)) {
            return new CellDecoration(HEADER_LABEL_COLOR, BorderDecoration.none());
        }
        return null;
    }

    private boolean isSectionRow(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell) {
        return row != null
                && row.size() == 1
                && cell != null
                && (cell.getColSpan() > 1 || cell.getWidthPx() >= MesProBatchRecordReportShapeRules.TARGET_RENDER_WIDTH_PX / 3);
    }

    private boolean isHeaderCell(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell) {
        if (row == null || row.size() <= 1 || cell == null || cell.isVisualBlank()) {
            return false;
        }
        String compactText = compactText(cell.getText());
        if (compactText.isBlank()) {
            return false;
        }
        if (isStrongHeaderSignal(cell)) {
            return true;
        }
        if (!isHeaderLikeRow(row)) {
            return false;
        }
        return isCompactLabelText(compactText);
    }

    private boolean isGenericLabelValueCell(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell) {
        if (!isLabelValueRow(row) || cell == null) {
            return false;
        }
        int index = indexOfCell(row, cell);
        if (index < 0 || index % 2 != 0) {
            return false;
        }
        return isCompactLabelText(compactText(cell.getText()));
    }

    private boolean isLabelValueRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 2 || row.size() % 2 != 0) {
            return false;
        }
        int labelCount = 0;
        for (int index = 0; index < row.size(); index += 2) {
            MesProBatchRecordParsedCell labelCell = row.get(index);
            MesProBatchRecordParsedCell valueCell = row.get(index + 1);
            String labelText = compactText(labelCell == null ? null : labelCell.getText());
            String valueText = compactText(valueCell == null ? null : valueCell.getText());
            if (labelText.isBlank() || !isCompactLabelText(labelText)) {
                return false;
            }
            if (!valueText.isBlank() && isCompactLabelText(valueText) && !looksLikeValueText(valueText)) {
                return false;
            }
            labelCount++;
        }
        return labelCount > 0;
    }

    private boolean isStrongHeaderSignal(MesProBatchRecordParsedCell cell) {
        return cell != null && (cell.isBold()
                || hasBackgroundColor(cell)
                || cell.getColSpan() > 1
                || cell.getRowSpan() > 1);
    }

    private String resolveCellTextColor(String text) {
        return compactText(text).contains("/pcs") ? PCS_TEXT_COLOR : null;
    }

    private Integer resolveFontSizeOverride(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell) {
        if (cell == null || cell.isVisualBlank()) {
            return null;
        }
        int baseFontSize = MesProBatchRecordReportShapeRules.clampFontSize(cell.getFontSize(), cell.isBold());
        if (shouldCompactNarrativeCell(row, cell)) {
            return Math.max(8, baseFontSize - 2);
        }
        if (shouldCompactDenseRepeatedDetailCell(row, cell)) {
            return Math.max(8, baseFontSize - 1);
        }
        return null;
    }

    private String resolveVerticalAlignOverride(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell) {
        return shouldCompactNarrativeCell(row, cell) ? "top" : null;
    }

    private boolean shouldCompactNarrativeCell(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell) {
        if (cell == null || cell.isVisualBlank() || cell.isBold()) {
            return false;
        }
        String normalizedText = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
        if (!MesProBatchRecordReportShapeRules.isNarrativeText(normalizedText)) {
            return false;
        }
        if (isSectionRow(row, cell) || isHeaderCell(row, cell)) {
            return false;
        }
        return cell.getColSpan() > 1
                || cell.getWidthPx() >= 220
                || (row != null && row.size() <= 3);
    }

    private boolean shouldCompactDenseRepeatedDetailCell(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell) {
        if (cell == null || cell.isVisualBlank() || cell.isBold()) {
            return false;
        }
        if (!isDenseRepeatedDetailRow(row) || isSectionRow(row, cell) || isHeaderCell(row, cell)) {
            return false;
        }
        String normalizedText = MesProBatchRecordReportShapeRules.normalizeRecognizedText(cell.getText());
        String compactText = compactText(normalizedText);
        if (compactText.isBlank() || MesProBatchRecordReportShapeRules.isNarrativeText(normalizedText)) {
            return false;
        }
        return cell.getWidthPx() <= 140 || compactText.length() >= 10;
    }

    private boolean isDenseRepeatedDetailRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 6) {
            return false;
        }
        int blankishCount = 0;
        int shortTextCount = 0;
        for (MesProBatchRecordParsedCell item : row) {
            String normalizedText = MesProBatchRecordReportShapeRules.normalizeRecognizedText(item == null ? null : item.getText());
            String compactText = compactText(normalizedText);
            if (compactText.isBlank() || item != null && (item.isVisualBlank() || item.isFillable())) {
                blankishCount++;
                continue;
            }
            if (!MesProBatchRecordReportShapeRules.isNarrativeText(normalizedText) && compactText.length() <= 18) {
                shortTextCount++;
            }
        }
        return blankishCount >= row.size() / 2 && shortTextCount >= 2;
    }

    private String compactText(String text) {
        return text == null ? "" : text.replace("\n", "").replace(" ", "").trim();
    }

    private boolean isHeaderLikeRow(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() <= 1) {
            return false;
        }
        long decoratedCount = row.stream().filter(this::hasDecorationSignal).count();
        if (decoratedCount == 0) {
            return false;
        }
        long compactCount = row.stream()
                .map(item -> compactText(item.getText()))
                .filter(text -> !text.isBlank())
                .filter(text -> text.length() <= 18)
                .count();
        return compactCount >= 2;
    }

    private boolean hasBackgroundColor(MesProBatchRecordParsedCell cell) {
        return cell != null && cell.getBackgroundColor() != null && !cell.getBackgroundColor().trim().isBlank();
    }

    private boolean hasDecorationSignal(MesProBatchRecordParsedCell cell) {
        return cell != null && (cell.isBold()
                || hasBackgroundColor(cell)
                || cell.getColSpan() > 1
                || cell.getRowSpan() > 1);
    }

    private boolean isCompactLabelText(String compactText) {
        if (compactText == null || compactText.isBlank()) {
            return false;
        }
        if (compactText.length() > 12) {
            return false;
        }
        if (MesProBatchRecordReportShapeRules.isNarrativeText(compactText)) {
            return false;
        }
        return !looksLikeValueText(compactText);
    }

    private boolean looksLikeValueText(String compactText) {
        if (compactText == null || compactText.isBlank()) {
            return false;
        }
        if (compactText.matches(".*\\d.*")) {
            return true;
        }
        String lowerCase = compactText.toLowerCase();
        if (lowerCase.contains("/pcs") || lowerCase.contains("/qty") || lowerCase.contains("/quantity")) {
            return false;
        }
        if (compactText.matches(".*[A-Za-z].*")) {
            return true;
        }
        for (int index = 0; index < compactText.length(); index++) {
            char current = compactText.charAt(index);
            if (current == '%'
                    || current == '\uFF05'
                    || current == '\u2103'
                    || current == '\u00B0'
                    || current == '\u25A1'
                    || current == ':'
                    || current == '\uFF1A') {
                return true;
            }
        }
        return false;
    }

    private int indexOfCell(List<MesProBatchRecordParsedCell> row, MesProBatchRecordParsedCell cell) {
        for (int index = 0; index < row.size(); index++) {
            if (row.get(index) == cell) {
                return index;
            }
        }
        return -1;
    }

    private int resolveEnhancedStyleIndex(JSONArray styles, Map<String, Integer> styleCache, int originalStyleIndex,
                                          CellDecoration decoration, String textColor,
                                          Integer fontSizeOverride, String verticalAlignOverride) {
        String backgroundColor = decoration == null ? "" : decoration.backgroundColor();
        String borderKey = decoration == null ? "" : decoration.borderDecoration().cacheKey();
        String cacheKey = originalStyleIndex + "|" + backgroundColor + "|" + borderKey
                + "|" + (textColor == null ? "" : textColor)
                + "|" + (fontSizeOverride == null ? "" : fontSizeOverride)
                + "|" + (verticalAlignOverride == null ? "" : verticalAlignOverride);
        Integer cached = styleCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        JSONObject original = styles.getJSONObject(originalStyleIndex);
        JSONObject enhanced = original == null
                ? new JSONObject(true)
                : JSON.parseObject(original.toJSONString(), JSONObject.class);
        if (!backgroundColor.isBlank()) {
            enhanced.put("bgcolor", backgroundColor);
        }
        applyBorderDecoration(enhanced, decoration == null ? null : decoration.borderDecoration());
        if (textColor != null && !textColor.isBlank()) {
            enhanced.put("color", textColor);
        }
        applyFontSizeOverride(enhanced, fontSizeOverride);
        if (verticalAlignOverride != null && !verticalAlignOverride.isBlank()) {
            enhanced.put("valign", verticalAlignOverride);
        }
        int newIndex = styles.size();
        styles.add(enhanced);
        styleCache.put(cacheKey, newIndex);
        return newIndex;
    }

    private void applyFontSizeOverride(JSONObject style, Integer fontSizeOverride) {
        if (fontSizeOverride == null || fontSizeOverride <= 0) {
            return;
        }
        JSONObject font = style.getJSONObject("font");
        if (font == null) {
            font = new JSONObject(true);
            style.put("font", font);
        }
        font.put("size", fontSizeOverride);
    }

    private void applyBorderDecoration(JSONObject style, BorderDecoration decoration) {
        if (decoration == null || decoration.isEmpty()) {
            return;
        }
        JSONObject border = style.getJSONObject("border");
        if (border == null) {
            border = new JSONObject(true);
            border.put("bottom", List.of("thin", "#000"));
            border.put("top", List.of("thin", "#000"));
            border.put("left", List.of("thin", "#000"));
            border.put("right", List.of("thin", "#000"));
            style.put("border", border);
        }
        updateBorderSide(border, "top", decoration.top());
        updateBorderSide(border, "bottom", decoration.bottom());
        updateBorderSide(border, "left", decoration.left());
        updateBorderSide(border, "right", decoration.right());
    }

    private void updateBorderSide(JSONObject border, String side, String borderStyle) {
        if (borderStyle == null || borderStyle.isBlank()) {
            return;
        }
        JSONArray current = border.getJSONArray(side);
        String color = current != null && current.size() > 1 && current.getString(1) != null
                ? current.getString(1)
                : "#000";
        String currentStyle = current != null && !current.isEmpty() ? current.getString(0) : "";
        border.put(side, List.of(strongerBorderStyle(currentStyle, borderStyle), color));
    }

    private String strongerBorderStyle(String currentStyle, String requestedStyle) {
        return borderRank(currentStyle) >= borderRank(requestedStyle) ? currentStyle : requestedStyle;
    }

    private int borderRank(String borderStyle) {
        if ("thick".equals(borderStyle)) {
            return 3;
        }
        if ("medium".equals(borderStyle)) {
            return 2;
        }
        if ("thin".equals(borderStyle)) {
            return 1;
        }
        return 0;
    }

    private record CellDecoration(String backgroundColor, BorderDecoration borderDecoration) {
    }

    private record BorderDecoration(String top, String bottom, String left, String right) {

        private static BorderDecoration horizontalBand(String borderStyle) {
            return new BorderDecoration(borderStyle, borderStyle, "", "");
        }

        private static BorderDecoration none() {
            return new BorderDecoration("", "", "", "");
        }

        private boolean isEmpty() {
            return top.isBlank() && bottom.isBlank() && left.isBlank() && right.isBlank();
        }

        private String cacheKey() {
            return top + ":" + bottom + ":" + left + ":" + right;
        }
    }
}
