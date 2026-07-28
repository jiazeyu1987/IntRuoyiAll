package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.util.ArrayList;
import java.util.List;

final class MesProBatchRecordPackedMaterialMatrixTextSupport {

    private MesProBatchRecordPackedMaterialMatrixTextSupport() {
    }

    static List<String> nonBlankLines(String text) {
        if (text == null) {
            return List.of();
        }
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    static List<String> extractItemNames(List<String> lines, int headerCount) {
        if (lines == null || headerCount < 0 || lines.size() <= headerCount) {
            return List.of();
        }
        List<String> itemNames = new ArrayList<>();
        for (int index = headerCount; index < lines.size(); index++) {
            String line = lines.get(index) == null ? "" : lines.get(index).trim();
            if (line.isBlank() || isPackedSeparatorLine(line)) {
                continue;
            }
            if (isContinuationLine(line) && !itemNames.isEmpty()) {
                int lastIndex = itemNames.size() - 1;
                itemNames.set(lastIndex, itemNames.get(lastIndex) + "\n" + line);
                continue;
            }
            itemNames.add(line);
        }
        return itemNames;
    }

    private static boolean isPackedSeparatorLine(String line) {
        String normalized = normalize(line);
        return "/".equals(normalized) || "／".equals(normalized);
    }

    static boolean isContinuationLine(String line) {
        String normalized = normalize(line);
        if (normalized.isBlank()
                || normalized.startsWith("□")
                || normalized.startsWith("☑")
                || normalized.startsWith("/")
                || normalized.startsWith("／")
                || isPackedHeaderToken(normalized)) {
            return false;
        }
        return isParentheticalLine(normalized);
    }

    private static boolean isPackedHeaderToken(String normalized) {
        return "物料编码".equals(normalized)
                || "物料名称".equals(normalized)
                || "批号".equals(normalized);
    }

    private static boolean isParentheticalLine(String normalized) {
        return (normalized.startsWith("（") && normalized.endsWith("）"))
                || (normalized.startsWith("(") && normalized.endsWith(")"))
                || (normalized.startsWith("【") && normalized.endsWith("】"))
                || (normalized.startsWith("[") && normalized.endsWith("]"));
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "").trim();
    }
}
