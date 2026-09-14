package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MesProBatchRecordProcessMaterialExtractor {

    private static final Pattern PROCESS_TITLE_PATTERN = Pattern.compile("(.+?工序)生产记录");
    private static final Pattern MATERIAL_CODE_PATTERN = Pattern.compile("A\\d{3}(?:\\.\\d+){2,}");

    public List<ProcessMaterialMapping> extract(List<MesProBatchRecordParsedTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return List.of();
        }
        List<ProcessMaterialMapping> mappings = new ArrayList<>();
        for (MesProBatchRecordParsedTable table : tables) {
            String processName = resolveProcessName(table);
            if (processName.isBlank()) {
                continue;
            }
            List<ProcessMaterial> inputMaterials = extractInputMaterials(table, processName);
            List<ProcessMaterial> outputMaterials = extractOutputMaterials(table);
            mappings.add(new ProcessMaterialMapping(processName, inputMaterials, outputMaterials));
        }
        return mappings;
    }

    private List<ProcessMaterial> extractInputMaterials(MesProBatchRecordParsedTable table, String processName) {
        Map<String, ProcessMaterial> materials = new LinkedHashMap<>();
        List<List<MesProBatchRecordParsedCell>> rows = safeRows(table);
        int firstOperationHeaderIndex = firstRowIndexContaining(rows, "操作日期");
        for (List<MesProBatchRecordParsedCell> row : rows) {
            for (MesProBatchRecordParsedCell cell : row) {
                String text = normalized(cell.getText());
                if (!isPackedMaterialMatrix(text)) {
                    continue;
                }
                addAll(materials, parsePackedMaterials(text, processName));
            }
        }
        if (!materials.isEmpty() || firstOperationHeaderIndex <= 0) {
            return List.copyOf(materials.values());
        }
        for (int rowIndex = 0; rowIndex < firstOperationHeaderIndex; rowIndex++) {
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            String rowText = rowText(row);
            if (!hasMaterialHeader(rowText) || rowText.contains("操作日期")) {
                continue;
            }
            for (int dataRowIndex = rowIndex + 1; dataRowIndex < firstOperationHeaderIndex; dataRowIndex++) {
                String dataRowText = rowText(rows.get(dataRowIndex));
                if (dataRowText.contains("设备编码") || dataRowText.contains("生产自检")) {
                    break;
                }
                addAll(materials, parseAdjacentMaterialCells(rows.get(dataRowIndex), true));
            }
        }
        return List.copyOf(materials.values());
    }

    private List<ProcessMaterial> extractOutputMaterials(MesProBatchRecordParsedTable table) {
        Map<String, ProcessMaterial> materials = new LinkedHashMap<>();
        boolean insideOutputGrid = false;
        for (List<MesProBatchRecordParsedCell> row : safeRows(table)) {
            String rowText = rowText(row);
            if (!insideOutputGrid && isOperationMaterialOutputHeader(rowText)) {
                insideOutputGrid = true;
                continue;
            }
            if (!insideOutputGrid) {
                continue;
            }
            if (isOutputGridTerminator(rowText)) {
                break;
            }
            addAll(materials, parseAdjacentMaterialCells(row, false));
        }
        return List.copyOf(materials.values());
    }

    private List<ProcessMaterial> parseAdjacentMaterialCells(List<MesProBatchRecordParsedCell> row,
                                                             boolean preserveCheckboxMarker) {
        List<String> texts = row == null ? List.of() : row.stream()
                .map(MesProBatchRecordParsedCell::getText)
                .map(this::normalized)
                .filter(text -> !text.isBlank())
                .toList();
        if (texts.isEmpty()) {
            return List.of();
        }
        List<ProcessMaterial> materials = new ArrayList<>();
        for (int index = 0; index < texts.size(); index++) {
            String text = texts.get(index);
            Matcher matcher = MATERIAL_CODE_PATTERN.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            String code = matcher.group();
            String inlineName = compact(text.substring(matcher.end()));
            String name = inlineName.isBlank() ? nextMaterialName(texts, index + 1) : inlineName;
            if (!preserveCheckboxMarker) {
                name = stripLeadingCheckboxMarker(name);
            }
            if (!name.isBlank()) {
                materials.add(new ProcessMaterial(code, name));
            }
        }
        return materials;
    }

    private List<ProcessMaterial> parsePackedMaterials(String text, String processName) {
        String compact = compact(text);
        int firstDataIndex = firstMaterialDataIndex(compact);
        if (firstDataIndex < 0) {
            return List.of();
        }
        compact = compact.substring(firstDataIndex);
        List<ProcessMaterial> codedMaterials = new ArrayList<>();
        List<ProcessMaterial> noCodeMaterials = new ArrayList<>();
        int cursor = 0;
        while (cursor < compact.length()) {
            int nextCodeStart = nextCodeStart(compact, cursor);
            int nextSlashStart = nextNoCodeMarker(compact, cursor, nextCodeStart);
            if (nextSlashStart >= 0 && (nextCodeStart < 0 || nextSlashStart < nextCodeStart)) {
                int end = nextCodeStart(compact, nextSlashStart + 1);
                if (end < 0) {
                    end = compact.length();
                }
                addNoCodeMaterial(noCodeMaterials, processName, compact.substring(nextSlashStart + 1, end));
                cursor = end;
                continue;
            }
            if (nextCodeStart < 0) {
                break;
            }
            Matcher matcher = MATERIAL_CODE_PATTERN.matcher(compact);
            matcher.region(nextCodeStart, compact.length());
            if (!matcher.find() || matcher.start() != nextCodeStart) {
                cursor = nextCodeStart + 1;
                continue;
            }
            int codeEnd = matcher.end();
            int followingCodeStart = nextCodeStart(compact, codeEnd);
            int segmentEnd = followingCodeStart < 0 ? compact.length() : followingCodeStart;
            String segment = compact.substring(codeEnd, segmentEnd);
            int noCodeSeparator = noCodeSeparator(segment);
            String name = noCodeSeparator < 0 ? segment : segment.substring(0, noCodeSeparator);
            addMaterial(codedMaterials, matcher.group(), name);
            if (noCodeSeparator >= 0) {
                addNoCodeMaterial(noCodeMaterials, processName, segment.substring(noCodeSeparator + 1));
            }
            cursor = segmentEnd;
        }
        List<ProcessMaterial> materials = new ArrayList<>(codedMaterials);
        materials.addAll(noCodeMaterials);
        return materials;
    }

    private int firstMaterialDataIndex(String text) {
        Matcher matcher = MATERIAL_CODE_PATTERN.matcher(text);
        int codeStart = matcher.find() ? matcher.start() : -1;
        int slashStart = nextNoCodeMarker(text, 0, codeStart);
        if (codeStart < 0) {
            return slashStart;
        }
        if (slashStart < 0) {
            return codeStart;
        }
        return Math.min(codeStart, slashStart);
    }

    private int nextCodeStart(String text, int start) {
        Matcher matcher = MATERIAL_CODE_PATTERN.matcher(text);
        if (!matcher.find(Math.max(0, start))) {
            return -1;
        }
        return matcher.start();
    }

    private int nextNoCodeMarker(String text, int start, int beforeIndex) {
        int limit = beforeIndex < 0 ? text.length() : beforeIndex;
        for (int index = Math.max(0, start); index < limit; index++) {
            char ch = text.charAt(index);
            if ((ch == '/' || ch == '／') && index + 1 < text.length() && isNoCodeMaterialStart(text.substring(index + 1))) {
                return index;
            }
        }
        return -1;
    }

    private int noCodeSeparator(String text) {
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch == '/' || ch == '／') && index > 0 && isNoCodeMaterialStart(text.substring(index + 1))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isNoCodeMaterialStart(String text) {
        return text.startsWith("KC-6")
                || text.startsWith("9180")
                || text.startsWith("环氧树脂")
                || text.startsWith("固化剂")
                || text.startsWith("增韧剂");
    }

    private void addNoCodeMaterial(List<ProcessMaterial> materials, String processName, String name) {
        String normalizedName = compact(name);
        if (normalizedName.isBlank()) {
            return;
        }
        materials.add(new ProcessMaterial(resolveNoCodeLabel(processName), normalizedName));
    }

    private String resolveNoCodeLabel(String processName) {
        return "硅化Ⅱ工序".equals(processName) ? "无编码" : "无编号";
    }

    private void addMaterial(List<ProcessMaterial> materials, String code, String name) {
        String normalizedCode = compact(code);
        String normalizedName = compact(name);
        if (normalizedCode.isBlank() || normalizedName.isBlank()) {
            return;
        }
        materials.add(new ProcessMaterial(normalizedCode, normalizedName));
    }

    private void addAll(Map<String, ProcessMaterial> target, List<ProcessMaterial> materials) {
        for (ProcessMaterial material : materials) {
            target.putIfAbsent(material.code() + "\u0000" + material.name(), material);
        }
    }

    private String nextMaterialName(List<String> texts, int startIndex) {
        for (int index = startIndex; index < texts.size(); index++) {
            String candidate = compact(texts.get(index));
            if (candidate.isBlank()
                    || hasMaterialHeader(candidate)
                    || MATERIAL_CODE_PATTERN.matcher(candidate).find()
                    || isOperationValue(candidate)) {
                continue;
            }
            return candidate;
        }
        return "";
    }

    private boolean isOperationValue(String text) {
        String normalized = compact(text);
        return normalized.matches("\\d+(\\.\\d+)?")
                || normalized.matches("\\d+[-~]\\d+%?")
                || normalized.matches("\\d+±\\d+")
                || normalized.endsWith("min")
                || normalized.endsWith("h")
                || normalized.endsWith("%")
                || normalized.equals("室温")
                || normalized.equals("纯化水")
                || normalized.equals("注射用水")
                || normalized.equals("参考值")
                || normalized.equals("实际");
    }

    private boolean isPackedMaterialMatrix(String text) {
        String compact = compact(text);
        return compact.contains("物料编码")
                && compact.contains("物料名称")
                && compact.contains("批号")
                && firstMaterialDataIndex(compact) >= 0;
    }

    private boolean isOperationMaterialOutputHeader(String rowText) {
        String compact = compact(rowText);
        return compact.contains("操作日期")
                && compact.contains("物料编码")
                && compact.contains("物料名称")
                && compact.contains("批号");
    }

    private boolean isOutputGridTerminator(String rowText) {
        String compact = compact(rowText);
        return compact.contains("生产自检")
                || compact.contains("生产批量汇总")
                || compact.contains("生产后清场记录")
                || compact.contains("工序生产记录");
    }

    private boolean hasMaterialHeader(String text) {
        String compact = compact(text);
        return compact.contains("物料编码") || compact.contains("物料名称") || compact.contains("批号");
    }

    private int firstRowIndexContaining(List<List<MesProBatchRecordParsedCell>> rows, String expected) {
        for (int index = 0; index < rows.size(); index++) {
            if (compact(rowText(rows.get(index))).contains(compact(expected))) {
                return index;
            }
        }
        return -1;
    }

    private String resolveProcessName(MesProBatchRecordParsedTable table) {
        String title = normalized(table == null ? null : table.getTableTitle());
        Matcher matcher = PROCESS_TITLE_PATTERN.matcher(title);
        if (matcher.find()) {
            return compact(matcher.group(1));
        }
        for (List<MesProBatchRecordParsedCell> row : safeRows(table)) {
            matcher = PROCESS_TITLE_PATTERN.matcher(rowText(row));
            if (matcher.find()) {
                return compact(matcher.group(1));
            }
        }
        return "";
    }

    private List<List<MesProBatchRecordParsedCell>> safeRows(MesProBatchRecordParsedTable table) {
        if (table == null || table.getRows() == null) {
            return List.of();
        }
        return table.getRows();
    }

    private String rowText(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row) {
            String text = normalized(cell.getText());
            if (!text.isBlank()) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private String normalized(String value) {
        return value == null ? "" : value.replace('\u0007', ' ')
                .replace('\u0008', ' ')
                .replace('\u0000', ' ')
                .trim();
    }

    private String compact(String value) {
        return normalized(value).replaceAll("\\s+", "").trim();
    }

    private String stripLeadingCheckboxMarker(String value) {
        String text = compact(value);
        while (text.startsWith("□") || text.startsWith("☑")) {
            text = text.substring(1);
        }
        return text;
    }

    public record ProcessMaterialMapping(String processName,
                                         List<ProcessMaterial> inputMaterials,
                                         List<ProcessMaterial> outputMaterials) {
        public ProcessMaterialMapping {
            inputMaterials = inputMaterials == null ? List.of() : List.copyOf(inputMaterials);
            outputMaterials = outputMaterials == null ? List.of() : List.copyOf(outputMaterials);
        }
    }

    public record ProcessMaterial(String code, String name) {
    }
}
