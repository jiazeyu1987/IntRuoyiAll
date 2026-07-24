package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common fillable-pattern rules shared by form profiles.
 */
public final class MesProBatchRecordFillablePatternSupport {

    private static final int MIN_COLUMN_WIDTH_PX = 72;
    private static final int FILLABLE_WIDTH_UNIT = 8;
    private static final Pattern INLINE_UNDERLINE_PATTERN = Pattern.compile("[_＿—\\-－─]{3,}");
    private static final Pattern TRAILING_UNDERLINE_PATTERN = Pattern.compile("[_＿—\\-－─]{3,}\\s*$");

    private MesProBatchRecordFillablePatternSupport() {
    }

    public static boolean hasTrailingUnderlineFillable(String text) {
        return TRAILING_UNDERLINE_PATTERN.matcher(StrUtil.blankToDefault(text, "")).find();
    }

    public static String removeTrailingUnderline(String text) {
        return TRAILING_UNDERLINE_PATTERN.matcher(StrUtil.blankToDefault(text, "")).replaceFirst("");
    }

    public static boolean hasInlineUnderlineFillable(String text) {
        return INLINE_UNDERLINE_PATTERN.matcher(StrUtil.blankToDefault(text, "")).find();
    }

    public static List<MesProBatchRecordParsedCell> splitTrailingUnderlineFillable(String text,
                                                                                  int totalWidthPx,
                                                                                  int heightPx,
                                                                                  String inputType) {
        return splitTrailingUnderlineFillable(text, totalWidthPx, heightPx, inputType, false);
    }

    static List<MesProBatchRecordParsedCell> splitTrailingUnderlineFillable(String text,
                                                                           int totalWidthPx,
                                                                           int heightPx,
                                                                           String inputType,
                                                                           boolean reviewedCheckboxChoices) {
        if (!hasTrailingUnderlineFillable(text)) {
            throw new IllegalArgumentException("text does not contain a trailing underline fillable area");
        }
        String labelText = removeTrailingUnderline(text);
        List<String> checkboxLabels = MesProBatchRecordCellRuleSupport.splitUncheckedCheckboxChoiceLabels(labelText);
        if (checkboxLabels.size() > 1) {
            int labelWidth = Math.max(MIN_COLUMN_WIDTH_PX, totalWidthPx / (checkboxLabels.size() + 1));
            int fillableWidth = Math.max(MIN_COLUMN_WIDTH_PX, totalWidthPx - labelWidth * checkboxLabels.size());
            List<MesProBatchRecordParsedCell> cells = new ArrayList<>();
            checkboxLabels.forEach(label -> cells.add(staticCheckboxChoiceCell(label, labelWidth, heightPx,
                    reviewedCheckboxChoices)));
            cells.add(fillableUnderlineCell(fillableWidth, heightPx, inputType));
            return cells;
        }
        int labelWidth = Math.max(MIN_COLUMN_WIDTH_PX, totalWidthPx / 2);
        int fillableWidth = Math.max(MIN_COLUMN_WIDTH_PX, totalWidthPx - labelWidth);
        return List.of(
                staticInlineLabelCell(labelText, labelWidth, heightPx),
                fillableUnderlineCell(fillableWidth, heightPx, inputType)
        );
    }

    public static List<MesProBatchRecordParsedCell> splitInlineUnderlineFillables(String text,
                                                                                  int totalWidthPx,
                                                                                  int heightPx,
                                                                                  String inputType) {
        String normalized = StrUtil.blankToDefault(text, "");
        Matcher matcher = INLINE_UNDERLINE_PATTERN.matcher(normalized);
        List<InlineFillablePart> parts = new ArrayList<>();
        int cursor = 0;
        while (matcher.find()) {
            addStaticPart(parts, normalized.substring(cursor, matcher.start()));
            parts.add(InlineFillablePart.input());
            cursor = matcher.end();
        }
        String tail = normalized.substring(cursor);
        addStaticPart(parts, tail);
        if (hasFillablePart(parts) && isTrailingBlankPrompt(tail)) {
            parts.add(InlineFillablePart.input());
        }
        if (!hasFillablePart(parts) || parts.size() <= 1) {
            return List.of();
        }
        return toCells(parts, totalWidthPx, heightPx, inputType);
    }

    private static void addStaticPart(List<InlineFillablePart> parts, String text) {
        String normalized = StrUtil.blankToDefault(text, "").replaceAll("\\s+", " ").trim();
        if (!normalized.isBlank()) {
            parts.add(InlineFillablePart.label(normalized));
        }
    }

    private static boolean hasFillablePart(List<InlineFillablePart> parts) {
        return parts.stream().anyMatch(InlineFillablePart::fillable);
    }

    private static boolean isTrailingBlankPrompt(String text) {
        String normalized = StrUtil.blankToDefault(text, "")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", "")
                .trim();
        return normalized.length() >= 2 && normalized.length() <= 80
                && (normalized.endsWith(":") || normalized.endsWith("："));
    }

    private static List<MesProBatchRecordParsedCell> toCells(List<InlineFillablePart> parts,
                                                            int totalWidthPx,
                                                            int heightPx,
                                                            String inputType) {
        int totalUnits = parts.stream().mapToInt(MesProBatchRecordFillablePatternSupport::widthUnits).sum();
        int normalizedTotalWidth = Math.max(totalWidthPx, MIN_COLUMN_WIDTH_PX * parts.size());
        List<MesProBatchRecordParsedCell> cells = new ArrayList<>();
        for (InlineFillablePart part : parts) {
            int width = Math.max(MIN_COLUMN_WIDTH_PX,
                    normalizedTotalWidth * widthUnits(part) / Math.max(totalUnits, 1));
            cells.add(part.fillable()
                    ? fillableUnderlineCell(width, heightPx, inputType)
                    : staticInlineLabelCell(part.text(), width, heightPx));
        }
        return cells;
    }

    private static int widthUnits(InlineFillablePart part) {
        if (part.fillable()) {
            return FILLABLE_WIDTH_UNIT;
        }
        return Math.max(4, StrUtil.blankToDefault(part.text(), "").length());
    }

    private static MesProBatchRecordParsedCell staticCheckboxChoiceCell(String text, int widthPx, int heightPx,
                                                                       boolean reviewedCellRule) {
        return MesProBatchRecordParsedCell.builder()
                .text(staticCheckboxChoiceText(text))
                .rowSpan(1)
                .colSpan(1)
                .widthPx(widthPx)
                .heightPx(heightPx)
                .bold(false)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .reviewedCellRule(reviewedCellRule)
                .cellRuleSource(reviewedCellRule ? "MANUAL" : null)
                .build();
    }

    private static String staticCheckboxChoiceText(String text) {
        String normalized = StrUtil.blankToDefault(text, "").trim();
        if (normalized.isBlank() || normalized.startsWith("□") || normalized.startsWith("☐")) {
            return normalized;
        }
        return "□" + normalized;
    }

    private static MesProBatchRecordParsedCell staticInlineLabelCell(String text, int widthPx, int heightPx) {
        return MesProBatchRecordParsedCell.builder()
                .text(StrUtil.blankToDefault(text, ""))
                .rowSpan(1)
                .colSpan(1)
                .widthPx(widthPx)
                .heightPx(heightPx)
                .bold(false)
                .horizontalAlign("right")
                .verticalAlign("middle")
                .build();
    }

    private static MesProBatchRecordParsedCell fillableUnderlineCell(int widthPx, int heightPx, String inputType) {
        return MesProBatchRecordParsedCell.builder()
                .text("")
                .rowSpan(1)
                .colSpan(1)
                .widthPx(widthPx)
                .heightPx(heightPx)
                .fillable(true)
                .placeholder("")
                .inputType(inputType)
                .horizontalAlign("left")
                .verticalAlign("middle")
                .build();
    }

    private record InlineFillablePart(String text, boolean fillable) {

        private static InlineFillablePart label(String text) {
            return new InlineFillablePart(text, false);
        }

        private static InlineFillablePart input() {
            return new InlineFillablePart("", true);
        }
    }
}
