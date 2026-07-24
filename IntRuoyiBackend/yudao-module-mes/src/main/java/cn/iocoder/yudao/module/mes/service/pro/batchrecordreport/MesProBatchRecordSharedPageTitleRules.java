package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.util.List;

final class MesProBatchRecordSharedPageTitleRules {

    private static final int MIN_SHORT_TITLE_LENGTH = 2;
    private static final int MAX_SHORT_TITLE_LENGTH = 18;
    private static final String PROCESS_RECORD_KEYWORD = "\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55";
    private static final String INFO_SUFFIX = "\u4fe1\u606f";
    private static final String RECORD_SUFFIX = "\u8bb0\u5f55";
    private static final String SUMMARY_KEYWORD = "\u6c47\u603b";
    private static final String SUMMARY_TABLE_SUFFIX = "\u8868";
    private static final List<String> NON_TITLE_KEYWORDS = List.of(
            "\u8981\u6c42",
            "\u7ed3\u679c",
            "\u65e5\u671f",
            "\u5907\u6ce8",
            "\u68c0\u67e5",
            "\u64cd\u4f5c\u4eba",
            "\u590d\u6838\u4eba"
    );

    private MesProBatchRecordSharedPageTitleRules() {
    }

    enum SharedPageTitleType {
        NONE,
        INFORMATION_SUMMARY,
        PROCESS_RECORD,
        OTHER_SHORT_TITLE
    }

    static boolean isSharedPageTitleRow(List<MesProBatchRecordParsedCell> row) {
        return detectTitleType(row) != SharedPageTitleType.NONE;
    }

    static SharedPageTitleType detectTitleType(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return SharedPageTitleType.NONE;
        }
        String normalized = normalizeTitle(firstNonBlankRowText(row));
        if (normalized.isBlank()) {
            return SharedPageTitleType.NONE;
        }
        if (normalized.contains(PROCESS_RECORD_KEYWORD)) {
            return SharedPageTitleType.PROCESS_RECORD;
        }
        if (countNonBlankCells(row) != 1) {
            return SharedPageTitleType.NONE;
        }
        if (normalized.length() < MIN_SHORT_TITLE_LENGTH || normalized.length() > MAX_SHORT_TITLE_LENGTH) {
            return SharedPageTitleType.NONE;
        }
        if (containsNonTitleKeyword(normalized)) {
            return SharedPageTitleType.NONE;
        }
        if (looksLikeInternalSectionTitle(normalized)) {
            return SharedPageTitleType.NONE;
        }
        if (normalized.contains(SUMMARY_KEYWORD)) {
            return SharedPageTitleType.INFORMATION_SUMMARY;
        }
        if (normalized.endsWith(INFO_SUFFIX) || normalized.endsWith(RECORD_SUFFIX)) {
            return SharedPageTitleType.OTHER_SHORT_TITLE;
        }
        return SharedPageTitleType.NONE;
    }

    static String resolveRepresentativeTitle(String fallbackTitle, List<List<MesProBatchRecordParsedCell>> rows) {
        String normalizedFallback = normalizeSharedTitle(fallbackTitle);
        if (rows == null || rows.isEmpty()) {
            return normalizedFallback;
        }
        int representativeStart = resolveRepresentativeSectionStart(rows);
        for (int index = representativeStart; index < rows.size(); index++) {
            List<MesProBatchRecordParsedCell> row = rows.get(index);
            if (detectTitleType(row) == SharedPageTitleType.NONE) {
                continue;
            }
            String title = normalizeSharedTitle(firstNonBlankRowText(row));
            if (!title.isBlank()) {
                return title;
            }
        }
        return normalizedFallback;
    }

    static List<List<MesProBatchRecordParsedCell>> resolveRepresentativeRows(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        int representativeStart = resolveRepresentativeSectionStart(rows);
        if (representativeStart <= 0) {
            return copyRows(rows, 0, rows.size());
        }
        return copyRows(rows, representativeStart, rows.size());
    }

    static String normalizeSharedTitle(String text) {
        String normalized = normalizeTitle(text);
        if (normalized.isBlank()) {
            return "";
        }
        int processRecordEnd = normalized.indexOf(PROCESS_RECORD_KEYWORD);
        if (processRecordEnd >= 0) {
            return normalized.substring(0, processRecordEnd + PROCESS_RECORD_KEYWORD.length()).trim();
        }
        return normalized;
    }

    static boolean shouldStartNewTemplate(SharedPageTitleType type, boolean hasMatchedHeader) {
        if (type == SharedPageTitleType.NONE) {
            return false;
        }
        if (type == SharedPageTitleType.PROCESS_RECORD) {
            return true;
        }
        return !hasMatchedHeader;
    }

    static String normalizeTitle(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\r', '\n').trim();
        if (normalized.isBlank()) {
            return "";
        }
        String firstLine = normalized.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(normalized);
        return firstLine.replaceAll("\\s+", " ").trim();
    }

    private static String firstNonBlankRowText(List<MesProBatchRecordParsedCell> row) {
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && cell.getText() != null && !cell.getText().isBlank()) {
                return cell.getText();
            }
        }
        return "";
    }

    private static int countNonBlankCells(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            if (cell != null && cell.getText() != null && !cell.getText().isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static boolean containsNonTitleKeyword(String text) {
        for (String keyword : NON_TITLE_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeInternalSectionTitle(String text) {
        if (text.contains(SUMMARY_KEYWORD) && !text.endsWith(SUMMARY_TABLE_SUFFIX)) {
            return true;
        }
        return text.endsWith("\u68c0\u67e5\u8bb0\u5f55")
                || text.endsWith("\u6e05\u573a\u8bb0\u5f55")
                || text.contains("\u64cd\u4f5c\u53ca\u81ea\u68c0\u8bb0\u5f55")
                || text.endsWith("\u653e\u884c\u4fe1\u606f")
                || text.contains("\u6279\u53f7\u4fe1\u606f");
    }

    private static int resolveRepresentativeSectionStart(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        int candidate = -1;
        int sharedTitleCount = 0;
        for (int index = 0; index < rows.size(); index++) {
            SharedPageTitleType type = detectTitleType(rows.get(index));
            if (type == SharedPageTitleType.PROCESS_RECORD) {
                return 0;
            }
            if (type == SharedPageTitleType.INFORMATION_SUMMARY || type == SharedPageTitleType.OTHER_SHORT_TITLE) {
                candidate = index;
                sharedTitleCount++;
            }
        }
        if (sharedTitleCount <= 1 || candidate <= 0) {
            return 0;
        }
        return candidate;
    }

    private static List<List<MesProBatchRecordParsedCell>> copyRows(List<List<MesProBatchRecordParsedCell>> rows,
                                                                    int startInclusive,
                                                                    int endExclusive) {
        List<List<MesProBatchRecordParsedCell>> copies = new java.util.ArrayList<>();
        for (int index = startInclusive; index < endExclusive; index++) {
            copies.add(new java.util.ArrayList<>(rows.get(index)));
        }
        return copies;
    }
}
