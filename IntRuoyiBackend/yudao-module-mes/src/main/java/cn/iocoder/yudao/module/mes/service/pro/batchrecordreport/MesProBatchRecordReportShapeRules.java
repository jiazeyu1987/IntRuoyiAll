package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.util.ArrayList;
import java.util.List;

final class MesProBatchRecordReportShapeRules {

    static final int TARGET_RENDER_WIDTH_PX = resolveTargetRenderWidth(12);
    static final int SINGLE_PAGE_MAX_HEIGHT_PX = 650;
    static final int RELAXED_SINGLE_PAGE_MAX_HEIGHT_PX = 670;
    static final int DEFAULT_FONT_SIZE = 9;
    static final int MAX_RENDER_FONT_SIZE = 10;
    static final int MIN_RENDER_FONT_SIZE = 8;
    static final int DEFAULT_ROW_HEIGHT = 28;
    static final int MIN_ROW_HEIGHT_PX = 18;
    static final int MAX_ROW_HEIGHT_PX = 44;
    static final int MAX_PRESERVED_ROW_HEIGHT_PX = 96;
    static final int TITLE_ROW_HEIGHT_FLOOR_PX = 22;
    static final int HEADER_ROW_HEIGHT_FLOOR_PX = 20;
    static final int SUMMARY_ROW_HEIGHT_FLOOR_PX = 22;
    static final int FOOTER_ROW_HEIGHT_FLOOR_PX = 20;
    static final int MIN_COLUMN_WIDTH_PX = 18;
    static final int MAX_COLUMN_WIDTH_PX = 72;
    static final int ESTIMATED_CELL_HORIZONTAL_PADDING = 8;
    static final int NARRATIVE_CELL_HORIZONTAL_PADDING = 12;
    static final int ESTIMATED_LINE_PADDING = 6;
    static final int NARRATIVE_LINE_PADDING = 8;
    static final int NARRATIVE_MIN_ROW_HEIGHT_PX = 40;
    static final int NARRATIVE_EXTRA_LINE_HEIGHT_PX = 4;
    static final int NARRATIVE_MAX_CHAR_PER_LINE = 24;
    static final int SHARED_PAGE_WIDTH_NARROW_COLUMN_COUNT = 8;
    static final int SHARED_PAGE_WIDTH_MEDIUM_COLUMN_COUNT = 14;
    static final int SHARED_PAGE_WIDTH_NARROW_BUDGET_PX = 1120;
    static final int SHARED_PAGE_WIDTH_MEDIUM_BUDGET_PX = 1044;
    static final int SHARED_PAGE_WIDTH_DENSE_BUDGET_PX = 1044;
    static final int SHARED_PAGE_WIDTH_LANDSCAPE_BUDGET_PX =
            Math.round(SHARED_PAGE_WIDTH_DENSE_BUDGET_PX * 297f / 210f);
    static final int DENSE_TAIL_MIN_COLUMN_COUNT = 10;
    static final int DENSE_SEMANTIC_TAIL_MIN_COLUMN_COUNT = 9;
    static final int DENSE_TAIL_COLUMN_WIDTH_FLOOR_PX = 68;
    static final int DENSE_TAIL_UNIT_COLUMN_WIDTH_FLOOR_PX = 68;
    static final int DENSE_TAIL_COMPACT_LABEL_WIDTH_FLOOR_PX = 38;
    static final int CHECKLIST_CHOICE_COLUMN_WIDTH_FLOOR_PX = 36;
    static final int DEFAULT_ROWS_LEN = 100;
    static final int DEFAULT_COLS_LEN = 100;
    static final int DEFAULT_FILL_FORM_LAYOUT_WIDTH_PX = 120;
    static final int VISIBLE_FILL_FORM_LAYOUT_HEIGHT_PX = 36;
    static final int DEFAULT_FILL_FORM_LAYOUT_HEIGHT_PX = VISIBLE_FILL_FORM_LAYOUT_HEIGHT_PX;
    static final int DOC_HEADER_TOP_SPACER_HEIGHT_PX = 20;
    static final int CONTINUATION_PAGE_BREAK_SPACER_HEIGHT_PX = 56;
    static final int DOC_HEADER_PRINT_MARGIN_Y = 10;
    static final int COMPACT_FILL_FORM_MAX_WIDTH_PX = 96;
    static final int COMPACT_FILL_FORM_MIN_WIDTH_PX = 48;
    static final int COMPACT_FILL_FORM_MAX_HEIGHT_PX = 24;
    static final int COMPACT_FILL_FORM_MIN_HEIGHT_PX = 18;
    static final int COMPACT_FILL_CONTROL_ROW_BUFFER_PX = 0;
    static final int COMPACT_FILLABLE_MAX_WIDTH_PX = 200;
    static final int COMPACT_FILLABLE_MAX_HEIGHT_PX = 32;
    static final int DENSE_COMPACT_FILL_GRID_MIN_COUNT = 4;
    static final String DEFAULT_HORIZONTAL_ALIGN = "center";
    static final String DEFAULT_VERTICAL_ALIGN = "middle";
    static final String DEFAULT_SHEET_NAME = "\u9ed8\u8ba4Sheet";
    static final String EMPTY_CELL_TEXT = " ";
    static final String EDITABLE_PLACEHOLDER_TEXT = "\u8bf7\u586b\u5199";
    static final String INPUT_TYPE_INPUT = "Input";
    static final String INPUT_TYPE_TEXTAREA = "Textarea";
    static final String INPUT_TYPE_CHECKBOX = "Checkbox";

    private MesProBatchRecordReportShapeRules() {
    }

    static String normalizeRecognizedText(String text) {
        return text == null ? "" : text.trim();
    }

    static String normalizePaddingText() {
        return EMPTY_CELL_TEXT;
    }

    static int resolveTargetRenderWidth(int columnCount) {
        if (columnCount <= 0) {
            throw new IllegalArgumentException("columnCount must be positive");
        }
        int minimumWidth = columnCount * MIN_COLUMN_WIDTH_PX;
        return Math.max(minimumWidth, resolveSharedPageWidthBudget(columnCount));
    }

    static int resolveSharedPageWidthBudget(int columnCount) {
        if (columnCount <= 0) {
            throw new IllegalArgumentException("columnCount must be positive");
        }
        if (columnCount <= SHARED_PAGE_WIDTH_NARROW_COLUMN_COUNT) {
            return SHARED_PAGE_WIDTH_NARROW_BUDGET_PX;
        }
        if (columnCount <= SHARED_PAGE_WIDTH_MEDIUM_COLUMN_COUNT) {
            return SHARED_PAGE_WIDTH_MEDIUM_BUDGET_PX;
        }
        return SHARED_PAGE_WIDTH_DENSE_BUDGET_PX;
    }

    static boolean shouldPreserveFullPageWidthForLowColumnOverview(
            List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        if (columnCount <= 0) {
            throw new IllegalArgumentException("columnCount must be positive");
        }
        if (rows == null || rows.isEmpty()
                || columnCount <= 1
                || columnCount > SHARED_PAGE_WIDTH_NARROW_COLUMN_COUNT) {
            return false;
        }
        int sharedBudget = resolveSharedPageWidthBudget(columnCount);
        int fullPageWidthFloor = Math.min(1000, Math.round(sharedBudget * 0.9f));
        int maxSourceRowWidth = 0;
        int fullWidthRows = 0;
        int gridRows = 0;
        boolean overviewTitle = false;
        boolean processTitle = false;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            MesProBatchRecordSharedPageTitleRules.SharedPageTitleType titleType =
                    MesProBatchRecordSharedPageTitleRules.detectTitleType(row);
            if (titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD) {
                processTitle = true;
            } else if (titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY
                    || titleType == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE) {
                overviewTitle = true;
            }

            int rowColSpan = 0;
            int rowWidth = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                rowColSpan += Math.max(cell.getColSpan(), 1);
                rowWidth += Math.max(cell.getWidthPx(), 0);
            }
            maxSourceRowWidth = Math.max(maxSourceRowWidth, rowWidth);
            if (row.size() == 1 && rowColSpan >= columnCount) {
                fullWidthRows++;
            }
            if (row.size() >= 2 && rowColSpan >= columnCount) {
                gridRows++;
            }
        }
        return maxSourceRowWidth >= fullPageWidthFloor
                && (fullWidthRows >= 2 || gridRows >= 4)
                && (overviewTitle || rows.size() >= 20)
                && !processTitle;
    }

    static boolean shouldPreserveFullPageWidthForLowOrMediumProcessRecord(
            List<List<MesProBatchRecordParsedCell>> rows, int columnCount) {
        if (columnCount <= 0) {
            throw new IllegalArgumentException("columnCount must be positive");
        }
        if (rows == null || rows.isEmpty()
                || columnCount <= 1
                || columnCount > SHARED_PAGE_WIDTH_MEDIUM_COLUMN_COUNT
                || rows.size() < 17) {
            return false;
        }
        int sharedBudget = resolveSharedPageWidthBudget(columnCount);
        int fullPageWidthFloor = Math.min(1000, Math.round(sharedBudget * 0.9f));
        int maxSourceRowWidth = 0;
        boolean processTitle = false;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (MesProBatchRecordSharedPageTitleRules.detectTitleType(row)
                    == MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD) {
                processTitle = true;
            }
            int rowWidth = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                rowWidth += Math.max(cell.getWidthPx(), 0);
            }
            maxSourceRowWidth = Math.max(maxSourceRowWidth, rowWidth);
        }
        return processTitle && maxSourceRowWidth >= fullPageWidthFloor;
    }

    static int resolveDenseTailColumnWidthFloor(String text, int columnIndex, int columnCount) {
        String normalized = normalizeRecognizedText(text).replace('\n', ' ');
        if (normalized.isBlank()) {
            return 0;
        }
        String lowerCase = normalized.toLowerCase();
        boolean denseTailColumn = isDenseTailColumn(columnIndex, columnCount);
        boolean semanticTailColumn = isDenseSemanticTailColumn(columnIndex, columnCount);
        if (looksLikeChecklistChoiceText(normalized)) {
            if (!semanticTailColumn) {
                return 0;
            }
            return CHECKLIST_CHOICE_COLUMN_WIDTH_FLOOR_PX;
        }
        if (isNarrativeText(normalized)) {
            return 0;
        }
        if (normalized.contains("/") || lowerCase.contains("pcs")
                || normalized.contains("\u6570\u91cf") || lowerCase.contains("qty")
                || lowerCase.contains("quantity")) {
            if (!semanticTailColumn) {
                return 0;
            }
            return DENSE_TAIL_UNIT_COLUMN_WIDTH_FLOOR_PX;
        }
        if (normalized.contains("\u65e5\u671f") || lowerCase.contains("date")
                || normalized.endsWith("\u4eba") || lowerCase.contains("operator")
                || lowerCase.contains("reviewer")) {
            if (!semanticTailColumn) {
                return 0;
            }
            return DENSE_TAIL_COLUMN_WIDTH_FLOOR_PX;
        }
        if (denseTailColumn && normalized.length() <= 4) {
            return DENSE_TAIL_COMPACT_LABEL_WIDTH_FLOOR_PX;
        }
        return 0;
    }

    static int clampColumnWidth(int width) {
        return Math.max(MIN_COLUMN_WIDTH_PX, Math.min(MAX_COLUMN_WIDTH_PX, width));
    }

    static int clampFontSize(int fontSize, boolean bold) {
        int fallback = bold ? MAX_RENDER_FONT_SIZE : DEFAULT_FONT_SIZE;
        int resolved = fontSize <= 0 ? fallback : fontSize;
        int max = bold ? MAX_RENDER_FONT_SIZE : DEFAULT_FONT_SIZE;
        return Math.max(MIN_RENDER_FONT_SIZE, Math.min(max, resolved));
    }

    static int shrinkFontSize(int fontSize, boolean bold) {
        return clampFontSize(fontSize - 1, bold);
    }

    static int clampRowHeight(int height) {
        return Math.max(MIN_ROW_HEIGHT_PX, Math.min(MAX_ROW_HEIGHT_PX, height));
    }

    static int clampPreservedRowHeight(int height) {
        return Math.max(MIN_ROW_HEIGHT_PX, Math.min(MAX_PRESERVED_ROW_HEIGHT_PX, height));
    }

    static int estimateRowHeight(String text, int effectiveWidth, int fontSize) {
        return estimateRowHeight(text, effectiveWidth, fontSize, false);
    }

    static int estimatePreservedRowHeight(String text, int effectiveWidth, int fontSize) {
        return estimateRowHeight(text, effectiveWidth, fontSize, true);
    }

    private static int estimateRowHeight(String text, int effectiveWidth, int fontSize, boolean preserveHeight) {
        if (text == null || text.isBlank()) {
            return DEFAULT_ROW_HEIGHT;
        }
        boolean narrative = isNarrativeText(text);
        int horizontalPadding = resolveHorizontalPadding(narrative);
        int availableWidth = Math.max(effectiveWidth - horizontalPadding, MIN_COLUMN_WIDTH_PX);
        int effectiveFontSize = Math.max(1, fontSize);
        int charsPerLine = Math.max(1, availableWidth / effectiveFontSize);
        if (narrative) {
            charsPerLine = Math.min(charsPerLine, resolveNarrativeMaxCharsPerLine());
        }

        int visualLines = 0;
        for (String line : text.split("\\R", -1)) {
            int length = Math.max(line.trim().length(), 1);
            visualLines += Math.max(1, (int) Math.ceil(length / (double) charsPerLine));
        }

        int lineHeight = effectiveFontSize + resolveLinePadding(narrative);
        int estimatedHeight = Math.max(DEFAULT_ROW_HEIGHT, visualLines * lineHeight);
        if (narrative) {
            estimatedHeight = Math.max(estimatedHeight, resolveNarrativeRowHeightFloor(visualLines));
        }
        return preserveHeight ? clampPreservedRowHeight(estimatedHeight) : clampRowHeight(estimatedHeight);
    }

    static int resolveNarrativeRowHeightFloor(int visualLines) {
        if (visualLines <= 0) {
            throw new IllegalArgumentException("visualLines must be positive");
        }
        return NARRATIVE_MIN_ROW_HEIGHT_PX + Math.max(0, visualLines - 1) * NARRATIVE_EXTRA_LINE_HEIGHT_PX;
    }

    static int resolveRowHeightFloor(MesProBatchRecordSharedRowTypeRules.RowType rowType, int visualLines) {
        if (rowType == null) {
            return MIN_ROW_HEIGHT_PX;
        }
        return switch (rowType) {
            case TITLE -> TITLE_ROW_HEIGHT_FLOOR_PX;
            case FIELD, DETAIL_DATA -> MIN_ROW_HEIGHT_PX;
            case TABLE_HEADER -> HEADER_ROW_HEIGHT_FLOOR_PX;
            case SUMMARY -> SUMMARY_ROW_HEIGHT_FLOOR_PX;
            case FOOTER -> FOOTER_ROW_HEIGHT_FLOOR_PX;
            case LONG_DESCRIPTION -> Math.max(32, resolveNarrativeRowHeightFloor(Math.max(visualLines, 1)));
        };
    }

    static String resolveHorizontalAlign(MesProBatchRecordParsedCell cell) {
        if (cell != null && cell.isBorderless()) {
            return cell.getHorizontalAlign() == null || cell.getHorizontalAlign().isBlank()
                    ? "left"
                    : cell.getHorizontalAlign();
        }
        if (cell != null && ("center".equalsIgnoreCase(cell.getHorizontalAlign())
                || "right".equalsIgnoreCase(cell.getHorizontalAlign()))) {
            return cell.getHorizontalAlign();
        }
        String text = normalizeRecognizedText(cell == null ? null : cell.getText());
        if (isNarrativeText(text)) {
            return "left";
        }
        return DEFAULT_HORIZONTAL_ALIGN;
    }

    static String resolveVerticalAlign() {
        return DEFAULT_VERTICAL_ALIGN;
    }

    static boolean isFillable(MesProBatchRecordParsedCell cell) {
        if (cell == null) {
            return false;
        }
        if (cell.isVisualBlank()) {
            return false;
        }
        if (cell.isDiagonalSlash()) {
            return false;
        }
        return cell.isFillable() || cell.getText() == null || cell.getText().isBlank();
    }

    static String resolvePlaceholder(MesProBatchRecordParsedCell cell) {
        if (cell == null || cell.getPlaceholder() == null || cell.getPlaceholder().isBlank()) {
            return EDITABLE_PLACEHOLDER_TEXT;
        }
        return cell.getPlaceholder().trim();
    }

    static String resolveInputType(MesProBatchRecordParsedCell cell) {
        if (cell == null) {
            return INPUT_TYPE_INPUT;
        }
        if (INPUT_TYPE_TEXTAREA.equalsIgnoreCase(cell.getInputType())) {
            return INPUT_TYPE_TEXTAREA;
        }
        if (cell.getRowSpan() > 1 || cell.getHeightPx() >= 56) {
            return INPUT_TYPE_TEXTAREA;
        }
        return INPUT_TYPE_INPUT;
    }

    static boolean isCompactFillableCell(MesProBatchRecordParsedCell cell, int effectiveWidth) {
        if (!isFillable(cell) || effectiveWidth <= 0) {
            return false;
        }
        if (INPUT_TYPE_TEXTAREA.equals(resolveInputType(cell))) {
            return false;
        }
        if (cell.getColSpan() > 1 && effectiveWidth >= 180) {
            return false;
        }
        return effectiveWidth <= COMPACT_FILLABLE_MAX_WIDTH_PX
                || cell.getWidthPx() <= COMPACT_FILLABLE_MAX_WIDTH_PX
                || cell.getHeightPx() <= COMPACT_FILLABLE_MAX_HEIGHT_PX;
    }

    static boolean shouldHideCompactFillPlaceholder(MesProBatchRecordParsedCell cell, int effectiveWidth) {
        return isCompactFillableCell(cell, effectiveWidth)
                && normalizeRecognizedText(cell == null ? null : cell.getText()).isBlank();
    }

    static boolean shouldUseCompactFillLayout(int fillableCount, int compactCount) {
        return fillableCount > 0
                && compactCount >= DENSE_COMPACT_FILL_GRID_MIN_COUNT
                && compactCount * 2 >= fillableCount;
    }

    static int resolveCompactFillLayoutWidth(List<Integer> effectiveWidths) {
        if (effectiveWidths == null || effectiveWidths.isEmpty()) {
            return DEFAULT_FILL_FORM_LAYOUT_WIDTH_PX;
        }
        List<Integer> sortedWidths = new ArrayList<>(effectiveWidths);
        sortedWidths.sort(Integer::compareTo);
        int quartileIndex = Math.max(0, (sortedWidths.size() - 1) / 4);
        int referenceWidth = sortedWidths.get(quartileIndex) - 8;
        return Math.max(COMPACT_FILL_FORM_MIN_WIDTH_PX,
                Math.min(COMPACT_FILL_FORM_MAX_WIDTH_PX, referenceWidth));
    }

    static int resolveCompactFillLayoutHeight(List<Integer> rowHeights) {
        if (rowHeights == null || rowHeights.isEmpty()) {
            return DEFAULT_FILL_FORM_LAYOUT_HEIGHT_PX;
        }
        int referenceHeight = rowHeights.stream().min(Integer::compareTo).orElse(DEFAULT_ROW_HEIGHT) - 4;
        return Math.max(COMPACT_FILL_FORM_MIN_HEIGHT_PX,
                Math.min(COMPACT_FILL_FORM_MAX_HEIGHT_PX, referenceHeight));
    }

    static int resolveCompactFillRowHeightFloor(int fillFormLayoutHeight) {
        return clampRowHeight(fillFormLayoutHeight + COMPACT_FILL_CONTROL_ROW_BUFFER_PX);
    }

    static int resolveSinglePageTargetHeight(int rowCount, int columnCount,
                                             List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        if (isNarrativeHeavyStructuredProcessPage(rowCount, columnCount, rowTypes)
                || isLiveLikeDenseProcessPage(rowCount, columnCount, rowTypes)
                || isLiveLikeMediumProcessPage(rowCount, columnCount, rowTypes)
                || isLiveLikeLowColumnProcessPage(rowCount, columnCount, rowTypes)) {
            return RELAXED_SINGLE_PAGE_MAX_HEIGHT_PX;
        }
        return SINGLE_PAGE_MAX_HEIGHT_PX;
    }

    static boolean shouldUseRelaxedCalibratorViewport(int rowCount, int columnCount,
                                                      List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        return isNarrativeHeavyStructuredProcessPage(rowCount, columnCount, rowTypes);
    }

    static boolean isNarrativeText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.trim();
        if (looksLikeChecklistChoiceText(normalized)) {
            return false;
        }
        int lineCount = normalized.split("\\R", -1).length;
        if (lineCount >= 2) {
            return true;
        }
        if (isBulletNarrative(normalized)) {
            return true;
        }
        if (normalized.length() >= 18 && containsNarrativeDelimiter(normalized)) {
            return true;
        }
        return normalized.length() >= 48;
    }

    private static boolean containsNarrativeDelimiter(String text) {
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if ("\uFF0C\u3002\uFF1B\uFF1A\u3001,.!?;:()\uFF08\uFF09\u300A\u300B".indexOf(current) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBulletNarrative(String text) {
        return text.matches("^(?:\\d+|[\\u4e00-\\u5341]+)[\\u3001.\\uff0e].+");
    }

    private static boolean looksLikeChecklistChoiceText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replace('\r', ' ')
                .replace('\n', ' ')
                .replace("  ", " ")
                .trim();
        if (normalized.contains("□")) {
            return true;
        }
        return (normalized.contains("是") && normalized.contains("否"))
                || (normalized.contains("符合要求") && normalized.contains("不符合要求"));
    }

    private static int resolveHorizontalPadding(boolean narrative) {
        return narrative ? NARRATIVE_CELL_HORIZONTAL_PADDING : ESTIMATED_CELL_HORIZONTAL_PADDING;
    }

    private static int resolveLinePadding(boolean narrative) {
        return narrative ? NARRATIVE_LINE_PADDING : ESTIMATED_LINE_PADDING;
    }

    private static int resolveNarrativeMaxCharsPerLine() {
        return NARRATIVE_MAX_CHAR_PER_LINE;
    }

    private static boolean isNarrativeHeavyStructuredProcessPage(
            int rowCount, int columnCount, List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        if (rowTypes == null || columnCount < 9 || rowCount < 16) {
            return false;
        }
        long fieldRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FIELD);
        long longDescriptionRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION);
        long detailRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA);
        long summaryRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY);
        long footerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FOOTER);
        long headerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER);
        long structuredRows = headerRows + fieldRows + longDescriptionRows;
        return summaryRows >= 1
                && footerRows >= 1
                && fieldRows >= 4
                && longDescriptionRows >= 2
                && detailRows <= 3
                && structuredRows >= 10;
    }

    private static boolean isLiveLikeDenseProcessPage(
            int rowCount, int columnCount, List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
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

    private static boolean isLiveLikeMediumProcessPage(
            int rowCount, int columnCount, List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
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

    private static boolean isLiveLikeLowColumnProcessPage(
            int rowCount, int columnCount, List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes) {
        if (rowTypes == null
                || columnCount > SHARED_PAGE_WIDTH_NARROW_COLUMN_COUNT
                || rowCount < 20) {
            return false;
        }
        long titleRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.TITLE);
        long headerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER);
        long fieldRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FIELD);
        long longDescriptionRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION);
        long summaryRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY);
        long footerRows = countRowsOfType(rowTypes, MesProBatchRecordSharedRowTypeRules.RowType.FOOTER);
        return (titleRows >= 1 || headerRows >= 2)
                && summaryRows >= 1
                && footerRows >= 1
                && fieldRows >= 4
                && longDescriptionRows >= 1;
    }

    private static long countRowsOfType(List<MesProBatchRecordSharedRowTypeRules.RowType> rowTypes,
                                        MesProBatchRecordSharedRowTypeRules.RowType targetType) {
        if (rowTypes == null || targetType == null) {
            return 0;
        }
        return rowTypes.stream()
                .filter(targetType::equals)
                .count();
    }

    private static boolean isDenseTailColumn(int columnIndex, int columnCount) {
        if (columnCount < DENSE_TAIL_MIN_COLUMN_COUNT || columnIndex < 0) {
            return false;
        }
        int tailStart = columnCount - Math.max(5, (int) Math.ceil(columnCount * 0.35d));
        return columnIndex >= tailStart;
    }

    private static boolean isDenseSemanticTailColumn(int columnIndex, int columnCount) {
        if (columnCount < DENSE_SEMANTIC_TAIL_MIN_COLUMN_COUNT || columnIndex < 0) {
            return false;
        }
        int tailStart = Math.max(6, (int) Math.floor(columnCount / 3d));
        return columnIndex >= tailStart;
    }
}
