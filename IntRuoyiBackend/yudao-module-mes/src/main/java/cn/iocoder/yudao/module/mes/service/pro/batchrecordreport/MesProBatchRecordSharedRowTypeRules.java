package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MesProBatchRecordSharedRowTypeRules {

    private static final List<String> NARRATIVE_PREFIXES = List.of("备注", "说明", "补充说明");
    private static final List<String> SUMMARY_KEYWORDS = List.of("汇总", "合计", "总计", "小计");
    private static final List<String> FOOTER_PREFIXES = List.of("生效日期", "打印日期");

    private MesProBatchRecordSharedRowTypeRules() {
    }

    enum RowType {
        TITLE,
        FIELD,
        LONG_DESCRIPTION,
        TABLE_HEADER,
        DETAIL_DATA,
        SUMMARY,
        FOOTER
    }

    static RowType classifyRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        return classifyRow(rows, rowIndex, countRowSignatures(rows));
    }

    static RowType classifyRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex,
                               Map<String, Integer> rowSignatureCounts) {
        if (rows == null || rowIndex < 0 || rowIndex >= rows.size()) {
            return RowType.FIELD;
        }
        List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
        if (row == null || row.isEmpty() || countNonEmptyCells(row) == 0) {
            return RowType.FIELD;
        }
        if (isTitleRow(rows, rowIndex, row)) {
            return RowType.TITLE;
        }
        if (isFooterRow(row)) {
            return RowType.FOOTER;
        }
        if (isSummaryRow(row)) {
            return RowType.SUMMARY;
        }
        if (isLongDescriptionRow(row)) {
            return RowType.LONG_DESCRIPTION;
        }
        if (isFieldRow(row)) {
            return RowType.FIELD;
        }
        if (isRepeatedSparseDetailRow(rows, rowIndex, row, rowSignatureCounts)) {
            return RowType.DETAIL_DATA;
        }
        if (isTableHeaderRow(row, rowSignatureCounts)) {
            return RowType.TABLE_HEADER;
        }
        if (isDetailDataRow(row)) {
            return RowType.DETAIL_DATA;
        }
        return resolveFallbackType(row);
    }

    static boolean isStructuredTemplateRow(RowType rowType) {
        return rowType == RowType.FIELD || rowType == RowType.TABLE_HEADER;
    }

    static boolean isCompactTableHeaderRow(List<MesProBatchRecordParsedCell> row) {
        return isValueHeaderRow(row);
    }

    static Map<String, Integer> countRowSignatures(List<List<MesProBatchRecordParsedCell>> rows) {
        Map<String, Integer> counts = new HashMap<>();
        if (rows == null) {
            return counts;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            counts.merge(rowStructureKey(row), 1, Integer::sum);
        }
        return counts;
    }

    static String rowStructureKey(List<MesProBatchRecordParsedCell> row) {
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

    private static RowType resolveFallbackType(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (countValueLikeCells(row) >= Math.max(1, nonEmptyCells / 2)) {
            return RowType.DETAIL_DATA;
        }
        if (nonEmptyCells == 1 && looksLikeParagraphText(firstMeaningfulText(row))) {
            return RowType.LONG_DESCRIPTION;
        }
        return RowType.FIELD;
    }

    private static boolean isTitleRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex,
                                      List<MesProBatchRecordParsedCell> row) {
        if (!isFirstMeaningfulRow(rows, rowIndex)) {
            return false;
        }
        if (MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row)) {
            return true;
        }
        return countNonEmptyCells(row) == 1 && normalizedTextLength(firstMeaningfulText(row)) >= 4;
    }

    private static boolean isFooterRow(List<MesProBatchRecordParsedCell> row) {
        if (countNonEmptyCells(row) != 1) {
            return false;
        }
        String text = firstMeaningfulText(row);
        for (String prefix : FOOTER_PREFIXES) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        return text.matches("^第\\s*\\d+\\s*页.*$");
    }

    private static boolean isSummaryRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (nonEmptyCells <= 0 || countLongTextCells(row) > 0) {
            return false;
        }
        if (nonEmptyCells == 1) {
            return containsSummaryKeyword(firstMeaningfulText(row));
        }
        return row.stream()
                .map(MesProBatchRecordParsedCell::getText)
                .map(MesProBatchRecordSharedRowTypeRules::normalizeStructureToken)
                .anyMatch(MesProBatchRecordSharedRowTypeRules::containsSummaryKeyword);
    }

    private static boolean isLongDescriptionRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (nonEmptyCells == 1) {
            String text = firstMeaningfulText(row);
            return startsWithAny(text, NARRATIVE_PREFIXES) || looksLikeParagraphText(text);
        }
        if (row.size() < 4 || nonEmptyCells < 2 || nonEmptyCells > 6) {
            return false;
        }
        return countLongTextCells(row) == 1
                && countShortLabelCells(row) >= 1
                && countValueLikeCells(row) <= Math.max(2, nonEmptyCells / 2);
    }

    private static boolean isFieldRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (nonEmptyCells < 3 || nonEmptyCells > 8) {
            return false;
        }
        if (countLongTextCells(row) > 0) {
            return false;
        }
        return countShortLabelCells(row) >= 2 && countValueLikeCells(row) <= 1 && countBlankCells(row) >= 1;
    }

    private static boolean isTableHeaderRow(List<MesProBatchRecordParsedCell> row,
                                            Map<String, Integer> rowSignatureCounts) {
        return isStructuredHeaderRow(row)
                || isValueHeaderRow(row)
                || rowSignatureCounts.getOrDefault(rowStructureKey(row), 0) >= 2 && countShortLabelCells(row) >= 2;
    }

    private static boolean isRepeatedSparseDetailRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex,
                                                     List<MesProBatchRecordParsedCell> row,
                                                     Map<String, Integer> rowSignatureCounts) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (rowSignatureCounts.getOrDefault(rowStructureKey(row), 0) < 2
                || nonEmptyCells < 2
                || nonEmptyCells > 4
                || countBlankCells(row) < Math.max(1, row.size() / 2)
                || countLongTextCells(row) > 0
                || countValueLikeCells(row) > 1
                || !containsSparseDetailMarker(row)) {
            return false;
        }
        return hasPriorStructuredHeader(rows, rowIndex);
    }

    private static boolean isStructuredHeaderRow(List<MesProBatchRecordParsedCell> row) {
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

    private static boolean isValueHeaderRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        if (nonEmptyCells < 4) {
            return false;
        }
        if (countLongTextCells(row) > 0 || countValueLikeCells(row) > 1) {
            return false;
        }
        int distinctTokens = (int) row.stream()
                .map(MesProBatchRecordParsedCell::getText)
                .map(MesProBatchRecordSharedRowTypeRules::normalizeStructureToken)
                .filter(text -> !text.isBlank())
                .distinct()
                .count();
        return distinctTokens <= Math.max(2, nonEmptyCells / 2);
    }

    private static boolean isDetailDataRow(List<MesProBatchRecordParsedCell> row) {
        int nonEmptyCells = countNonEmptyCells(row);
        return nonEmptyCells > 0
                && countValueLikeCells(row) >= Math.max(2, nonEmptyCells / 2)
                && countLongTextCells(row) == 0;
    }

    private static boolean hasPriorStructuredHeader(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        for (int index = 0; index < rowIndex; index++) {
            List<MesProBatchRecordParsedCell> candidate = rows.get(index);
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            if (isStructuredHeaderRow(candidate) || isValueHeaderRow(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFirstMeaningfulRow(List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        for (int index = 0; index < rowIndex; index++) {
            if (countNonEmptyCells(rows.get(index)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsSummaryKeyword(String text) {
        for (String keyword : SUMMARY_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithAny(String text, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static int countNonEmptyCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && cell.getText() != null && !cell.getText().isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static int countBlankCells(List<MesProBatchRecordParsedCell> row) {
        return Math.max(0, row.size() - countNonEmptyCells(row));
    }

    private static int countShortLabelCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (isShortLabelText(textOf(cell))) {
                count++;
            }
        }
        return count;
    }

    private static int countLongTextCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (looksLikeParagraphText(textOf(cell))) {
                count++;
            }
        }
        return count;
    }

    private static int countValueLikeCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (isValueLikeText(textOf(cell))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isShortLabelText(String text) {
        String normalized = normalizeStructureToken(text);
        return !normalized.isBlank() && normalized.length() <= 12 && !looksLikeParagraphText(normalized);
    }

    private static boolean looksLikeParagraphText(String text) {
        String normalized = normalizeStructureToken(text);
        if (looksLikeChecklistChoiceText(normalized)) {
            return false;
        }
        return normalized.length() >= 18 || normalized.contains("\n") || normalized.contains("。")
                || normalized.contains("，") || normalized.contains("；");
    }

    private static boolean isValueLikeText(String text) {
        String normalized = normalizeStructureToken(text);
        return !normalized.isBlank()
                && (normalized.matches("(?i)^[A-Z0-9./%:-]+$")
                || normalized.matches("^\\d{4}-\\d{2}-\\d{2}.*$")
                || normalized.matches("^\\d{1,2}:\\d{2}.*$")
                || normalized.matches(".*\\d{1,2}%$")
                || normalized.matches(".*\\d+min$"));
    }

    private static String normalizeStructureToken(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', ' ')
                .replace('\n', ' ')
                .replace("  ", " ")
                .trim();
    }

    private static int normalizedTextLength(String text) {
        return normalizeStructureToken(text).length();
    }

    private static String firstMeaningfulText(List<MesProBatchRecordParsedCell> row) {
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && cell.getText() != null && !cell.getText().isBlank()) {
                return textOf(cell);
            }
        }
        return "";
    }

    private static String textOf(MesProBatchRecordParsedCell cell) {
        return cell == null || cell.getText() == null ? "" : cell.getText().trim();
    }

    private static boolean looksLikeChecklistChoiceText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = normalizeStructureToken(text);
        if (normalized.contains("□")) {
            return true;
        }
        return (normalized.contains("是") && normalized.contains("否"))
                || (normalized.contains("符合要求") && normalized.contains("不符合要求"));
    }

    private static boolean containsSparseDetailMarker(List<MesProBatchRecordParsedCell> row) {
        for (MesProBatchRecordParsedCell cell : row) {
            String normalized = normalizeStructureToken(textOf(cell));
            if (normalized.isBlank()) {
                continue;
            }
            if (looksLikeChecklistChoiceText(normalized)
                    || "/".equals(normalized)
                    || "／".equals(normalized)
                    || normalized.matches(".*\\d+(%|℃|°C|mm|cm|ml|kg|g|min|pcs|atm|bar|kpa).*")) {
                return true;
            }
        }
        return false;
    }
}
