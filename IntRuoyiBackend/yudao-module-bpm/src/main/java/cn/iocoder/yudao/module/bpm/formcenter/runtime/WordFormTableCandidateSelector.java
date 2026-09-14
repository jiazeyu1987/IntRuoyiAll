package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.hutool.core.util.StrUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class WordFormTableCandidateSelector {

    private static final String PROCESS_FORM_TITLE_SUFFIX = "工序生产记录";

    private WordFormTableCandidateSelector() {
    }

    static Selection select(XWPFDocument document, String templateName) {
        List<Candidate> candidates = collectCandidates(document);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("no recognizable Word table candidate");
        }
        if (candidates.size() == 1) {
            return new Selection(candidates.get(0), 1);
        }

        List<ScoredCandidate> matches = candidates.stream()
                .map(candidate -> new ScoredCandidate(candidate, matchScore(candidate.title(), templateName)))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
                .toList();
        if (!matches.isEmpty()) {
            int highestScore = matches.get(0).score();
            List<ScoredCandidate> highestMatches = matches.stream()
                    .filter(candidate -> candidate.score() == highestScore)
                    .toList();
            if (highestMatches.size() == 1) {
                return new Selection(highestMatches.get(0).candidate(), candidates.size());
            }
        }

        String titles = candidates.stream()
                .map(Candidate::title)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("、"));
        throw new IllegalArgumentException("multiple Word form candidates found; template name must uniquely match: "
                + titles);
    }

    private static List<Candidate> collectCandidates(XWPFDocument document) {
        List<Candidate> candidates = new ArrayList<>();
        for (int tableIndex = 0; tableIndex < document.getTables().size(); tableIndex++) {
            XWPFTable table = document.getTables().get(tableIndex);
            List<Integer> processTitleRows = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
                if (extractProcessFormTitle(table.getRow(rowIndex)) != null) {
                    processTitleRows.add(rowIndex);
                }
            }
            if (processTitleRows.isEmpty()) {
                candidates.add(new Candidate(table, 0, table.getRows().size(),
                        firstNonBlankTitle(table), tableIndex));
                continue;
            }
            for (int splitIndex = 0; splitIndex < processTitleRows.size(); splitIndex++) {
                int startRow = processTitleRows.get(splitIndex);
                int endRow = splitIndex + 1 < processTitleRows.size()
                        ? processTitleRows.get(splitIndex + 1) : table.getRows().size();
                candidates.add(new Candidate(table, startRow, endRow,
                        extractProcessFormTitle(table.getRow(startRow)), tableIndex));
            }
        }
        return candidates;
    }

    private static String extractProcessFormTitle(XWPFTableRow row) {
        if (row == null || row.getTableCells().size() != 1) {
            return null;
        }
        String text = compact(row.getCell(0).getText());
        int suffixIndex = text.indexOf(PROCESS_FORM_TITLE_SUFFIX);
        if (suffixIndex < 0) {
            return null;
        }
        return text.substring(0, suffixIndex + PROCESS_FORM_TITLE_SUFFIX.length());
    }

    private static String firstNonBlankTitle(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = normalizeText(cell.getText());
                if (!text.isBlank()) {
                    return text.length() > 80 ? text.substring(0, 80) : text;
                }
            }
        }
        return "表格";
    }

    private static int matchScore(String candidateTitle, String templateName) {
        String title = compact(candidateTitle).toLowerCase(Locale.ROOT);
        String requested = compact(templateName).toLowerCase(Locale.ROOT);
        if (title.isBlank() || requested.isBlank()) {
            return 0;
        }
        if (title.equals(requested)) {
            return 100;
        }
        String titleKey = semanticKey(title);
        String requestedKey = semanticKey(requested);
        if (!titleKey.isBlank() && titleKey.equals(requestedKey)) {
            return 90;
        }
        if (requested.length() >= 2 && title.contains(requested)) {
            return 80;
        }
        if (title.length() >= 2 && requested.contains(title)) {
            return 70;
        }
        if (titleKey.length() >= 2 && requestedKey.length() >= 2
                && (titleKey.contains(requestedKey) || requestedKey.contains(titleKey))) {
            return 60;
        }
        return 0;
    }

    private static String semanticKey(String value) {
        return compact(value)
                .replace(PROCESS_FORM_TITLE_SUFFIX, "")
                .replace("生产记录", "")
                .replace("工序", "")
                .replace("表单", "")
                .replace("模板", "")
                .replace("记录", "");
    }

    private static String compact(String text) {
        return normalizeText(text).replaceAll("[\\p{P}\\p{S}\\s]+", "");
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    record Candidate(XWPFTable table, int startRowInclusive, int endRowExclusive,
                     String title, int sourceTableIndex) {
    }

    record Selection(Candidate candidate, int candidateCount) {
    }

    private record ScoredCandidate(Candidate candidate, int score) {
    }
}
