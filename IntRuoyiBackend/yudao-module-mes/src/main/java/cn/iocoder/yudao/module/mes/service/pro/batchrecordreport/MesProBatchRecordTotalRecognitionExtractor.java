package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MesProBatchRecordTotalRecognitionExtractor {

    private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile("RE-PP-([A-Z]+-\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PROCESS_TITLE_PATTERN = Pattern.compile("(.+?工序)生产记录");
    private static final Pattern EQUIPMENT_CODE_PATTERN = Pattern.compile("[A-Z]\\d{5}");
    private static final Pattern NAMED_EQUIPMENT_PATTERN = Pattern.compile("([^：:\\n]+)[：:]\\s*[□☑]?([A-Z]\\d{5})");
    private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile(
            "^(清洗|烘干|紫外灯|能量范围|传送带速度|运行次数|硅油|硅化|热合).*");
    private static final String ACTUAL_VALUE_PLACEHOLDER = "待填写";

    private final MesProBatchRecordProcessMaterialExtractor materialExtractor =
            new MesProBatchRecordProcessMaterialExtractor();

    public RecognitionResult extract(String sourceFileName, List<MesProBatchRecordParsedTable> tables) {
        if (sourceFileName == null || sourceFileName.isBlank()) {
            throw new IllegalArgumentException("sourceFileName is required");
        }
        if (tables == null || tables.isEmpty()) {
            throw new IllegalArgumentException("parsed Word tables are required");
        }
        Product product = new Product(extractProductName(tables), extractProductCode(sourceFileName));
        Map<String, MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping> materialMappings =
                new LinkedHashMap<>();
        for (MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping mapping
                : materialExtractor.extract(tables)) {
            materialMappings.put(mapping.processName(), mapping);
        }

        List<ProcessRecognition> processes = new ArrayList<>();
        for (MesProBatchRecordParsedTable table : tables) {
            String processName = resolveProcessName(table);
            if (processName.isBlank()) {
                continue;
            }
            MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping materialMapping =
                    materialMappings.get(processName);
            if (materialMapping == null) {
                throw new IllegalStateException("material mapping is missing for process: " + processName);
            }
            processes.add(new ProcessRecognition(processName,
                    convertMaterials(materialMapping.inputMaterials()),
                    convertMaterials(materialMapping.outputMaterials()),
                    extractEquipmentGroups(processName, table)));
        }
        if (processes.isEmpty()) {
            throw new IllegalStateException("no process production record table was recognized");
        }
        return new RecognitionResult(product, 2, processes);
    }

    private String extractProductName(List<MesProBatchRecordParsedTable> tables) {
        for (MesProBatchRecordParsedTable table : tables) {
            for (List<MesProBatchRecordParsedCell> row : safeRows(table)) {
                for (int index = 0; index < row.size(); index++) {
                    MesProBatchRecordParsedCell cell = row.get(index);
                    if (!"产品名称".equals(compact(cell.getText()))) {
                        continue;
                    }
                    int labelEnd = endColumn(cell);
                    for (MesProBatchRecordParsedCell candidate : row) {
                        String value = compact(candidate.getText());
                        if (startColumn(candidate) >= labelEnd && !value.isBlank() && !"型号规格".equals(value)) {
                            return value;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("product name was not found in parsed Word tables");
    }

    private String extractProductCode(String sourceFileName) {
        Matcher matcher = PRODUCT_CODE_PATTERN.matcher(sourceFileName.toUpperCase(Locale.ROOT));
        if (!matcher.find()) {
            throw new IllegalStateException("product code was not found in source file name: " + sourceFileName);
        }
        return matcher.group(1);
    }

    private List<Material> convertMaterials(
            List<MesProBatchRecordProcessMaterialExtractor.ProcessMaterial> sourceMaterials) {
        List<Material> materials = new ArrayList<>();
        for (MesProBatchRecordProcessMaterialExtractor.ProcessMaterial source : sourceMaterials) {
            if ("无编号".equals(source.code()) || "无编码".equals(source.code())) {
                materials.add(new Material(null, source.name(), source.code()));
            } else {
                materials.add(new Material(source.code(), source.name(), null));
            }
        }
        return List.copyOf(materials);
    }

    private List<EquipmentGroup> extractEquipmentGroups(String processName, MesProBatchRecordParsedTable table) {
        List<EquipmentOption> equipment = extractEquipment(table);
        if (equipment.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, List<String>> parameters = extractParameters(table);
        if (parameters.isEmpty()) {
            return equipment.stream()
                    .map(option -> new EquipmentGroup(List.of(option), List.of(), selectionMode(processName)))
                    .toList();
        }

        int valueGroupCount = parameters.values().stream().mapToInt(List::size).max().orElse(0);
        if (valueGroupCount > 1) {
            return groupByParameterValueVectors(processName, equipment, parameters, valueGroupCount);
        }
        return groupByEquipmentParameterDomain(processName, equipment, parameters);
    }

    private List<EquipmentOption> extractEquipment(MesProBatchRecordParsedTable table) {
        LinkedHashMap<String, EquipmentOption> equipment = new LinkedHashMap<>();
        List<List<MesProBatchRecordParsedCell>> rows = safeRows(table);
        for (List<MesProBatchRecordParsedCell> row : rows) {
            for (MesProBatchRecordParsedCell cell : row) {
                String text = normalized(cell.getText());
                Matcher matcher = NAMED_EQUIPMENT_PATTERN.matcher(text);
                while (matcher.find()) {
                    String name = compact(matcher.group(1));
                    String code = matcher.group(2);
                    equipment.putIfAbsent(code, new EquipmentOption(code, stripCheckbox(name)));
                }
            }
        }
        if (!equipment.isEmpty()) {
            return List.copyOf(equipment.values());
        }

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            for (MesProBatchRecordParsedCell header : rows.get(rowIndex)) {
                String headerText = compact(header.getText());
                if (!headerText.endsWith("设备编码") || "设备编码".equals(headerText)) {
                    continue;
                }
                String equipmentName = headerText.substring(0, headerText.length() - "设备编码".length());
                for (int dataRowIndex = rowIndex + 1; dataRowIndex < rows.size(); dataRowIndex++) {
                    String dataRowText = rowText(rows.get(dataRowIndex));
                    if (compact(dataRowText).contains("生产自检")) {
                        break;
                    }
                    for (MesProBatchRecordParsedCell valueCell : rows.get(dataRowIndex)) {
                        if (!overlaps(header, valueCell)) {
                            continue;
                        }
                        Matcher codeMatcher = EQUIPMENT_CODE_PATTERN.matcher(normalized(valueCell.getText()));
                        while (codeMatcher.find()) {
                            String code = codeMatcher.group();
                            equipment.putIfAbsent(code, new EquipmentOption(code, equipmentName));
                        }
                    }
                }
            }
        }
        return List.copyOf(equipment.values());
    }

    private LinkedHashMap<String, List<String>> extractParameters(MesProBatchRecordParsedTable table) {
        List<List<MesProBatchRecordParsedCell>> rows = safeRows(table);
        int operationHeaderIndex = firstRowContaining(rows, "操作日期");
        if (operationHeaderIndex < 0) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, LinkedHashMap<String, Integer>> counts = new LinkedHashMap<>();
        for (int rowIndex = operationHeaderIndex; rowIndex + 1 < rows.size(); rowIndex++) {
            if (compact(rowText(rows.get(rowIndex))).contains("生产自检")) {
                break;
            }
            List<MesProBatchRecordParsedCell> headerRow = rows.get(rowIndex);
            List<MesProBatchRecordParsedCell> followingRow = rows.get(rowIndex + 1);
            for (MesProBatchRecordParsedCell header : headerRow) {
                String parameterName = compact(header.getText());
                if (!isPotentialParameterName(parameterName)) {
                    continue;
                }
                MesProBatchRecordParsedCell referenceMarker = followingRow.stream()
                        .filter(cell -> overlaps(header, cell))
                        .filter(cell -> isReferenceMarker(compact(cell.getText())))
                        .findFirst()
                        .orElse(null);
                int valueRowIndex = referenceMarker == null ? rowIndex + 1 : rowIndex + 2;
                if (valueRowIndex >= rows.size()) {
                    continue;
                }
                MesProBatchRecordParsedCell valueRange = referenceMarker == null ? header : referenceMarker;
                List<String> values = referenceValues(rows.get(valueRowIndex), valueRange);
                for (String value : values) {
                    counts.computeIfAbsent(parameterName, ignored -> new LinkedHashMap<>())
                            .merge(value, 1, Integer::sum);
                }
            }
        }

        LinkedHashMap<String, List<String>> parameters = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, Integer>> entry : counts.entrySet()) {
            int strongestEvidence = entry.getValue().values().stream().mapToInt(Integer::intValue).max().orElse(0);
            List<String> values = entry.getValue().entrySet().stream()
                    .filter(value -> value.getValue() == strongestEvidence)
                    .map(Map.Entry::getKey)
                    .toList();
            if (!values.isEmpty()) {
                parameters.put(entry.getKey(), values);
            }
        }
        return parameters;
    }

    private List<String> referenceValues(List<MesProBatchRecordParsedCell> row,
                                         MesProBatchRecordParsedCell range) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (MesProBatchRecordParsedCell cell : row) {
            if (!overlaps(range, cell)) {
                continue;
            }
            for (String line : normalized(cell.getText()).split("\\R")) {
                String value = compact(line);
                if (isReferenceValue(value)) {
                    values.add(value);
                }
            }
        }
        return List.copyOf(values);
    }

    private List<EquipmentGroup> groupByParameterValueVectors(String processName,
                                                               List<EquipmentOption> equipment,
                                                               LinkedHashMap<String, List<String>> parameters,
                                                               int valueGroupCount) {
        List<List<EquipmentOption>> equipmentGroups = groupConsecutiveEquipmentByName(equipment);
        if (equipmentGroups.size() != valueGroupCount) {
            throw new IllegalStateException("equipment groups cannot be uniquely aligned with parameter value groups"
                    + " for process " + processName + ": " + equipmentGroups + " / " + parameters);
        }
        List<EquipmentGroup> result = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < equipmentGroups.size(); groupIndex++) {
            List<Parameter> groupParameters = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                List<String> values = entry.getValue();
                if (values.size() != 1 && values.size() != valueGroupCount) {
                    throw new IllegalStateException("parameter values cannot be uniquely aligned: " + entry.getKey());
                }
                String referenceValue = values.size() == 1 ? values.get(0) : values.get(groupIndex);
                groupParameters.add(toParameter(processName, entry.getKey(), referenceValue));
            }
            result.add(new EquipmentGroup(equipmentGroups.get(groupIndex), groupParameters,
                    selectionMode(processName)));
        }
        return List.copyOf(result);
    }

    private List<EquipmentGroup> groupByEquipmentParameterDomain(String processName, List<EquipmentOption> equipment,
                                                                  LinkedHashMap<String, List<String>> parameters) {
        LinkedHashMap<String, List<EquipmentOption>> equipmentByDomain = new LinkedHashMap<>();
        for (EquipmentOption option : equipment) {
            equipmentByDomain.computeIfAbsent(equipmentDomain(option.name()), ignored -> new ArrayList<>()).add(option);
        }
        LinkedHashMap<String, List<Parameter>> parametersByDomain = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String domain = parameterDomain(entry.getKey());
            parametersByDomain.computeIfAbsent(domain, ignored -> new ArrayList<>())
                    .add(toParameter(processName, entry.getKey(), entry.getValue().get(0)));
        }

        if (equipmentByDomain.size() == 1) {
            List<Parameter> allParameters = parametersByDomain.values().stream().flatMap(List::stream).toList();
            return List.of(new EquipmentGroup(equipment, allParameters, selectionMode(processName)));
        }
        List<EquipmentGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<EquipmentOption>> entry : equipmentByDomain.entrySet()) {
            List<Parameter> groupParameters = parametersByDomain.getOrDefault(entry.getKey(), List.of());
            groups.add(new EquipmentGroup(entry.getValue(), groupParameters, selectionMode(processName)));
        }
        Set<String> unmatchedDomains = new LinkedHashSet<>(parametersByDomain.keySet());
        unmatchedDomains.removeAll(equipmentByDomain.keySet());
        if (!unmatchedDomains.isEmpty()) {
            throw new IllegalStateException("parameter domain has no matching equipment: " + unmatchedDomains);
        }
        return List.copyOf(groups);
    }

    private List<List<EquipmentOption>> groupConsecutiveEquipmentByName(List<EquipmentOption> equipment) {
        List<List<EquipmentOption>> groups = new ArrayList<>();
        for (EquipmentOption option : equipment) {
            if (groups.isEmpty() || !groups.get(groups.size() - 1).get(0).name().equals(option.name())) {
                groups.add(new ArrayList<>());
            }
            groups.get(groups.size() - 1).add(option);
        }
        return groups.stream().map(List::copyOf).toList();
    }

    private String equipmentDomain(String equipmentName) {
        if (equipmentName.contains("清洗")) {
            return "清洗";
        }
        if (equipmentName.contains("干燥")) {
            return "烘干";
        }
        if (equipmentName.contains("硅油")) {
            return "硅油";
        }
        return equipmentName;
    }

    private String parameterDomain(String parameterName) {
        if (parameterName.startsWith("清洗")) {
            return "清洗";
        }
        if (parameterName.startsWith("烘干")) {
            return "烘干";
        }
        if (parameterName.startsWith("硅油") || parameterName.startsWith("硅化")) {
            return "硅油";
        }
        return parameterName;
    }

    private String selectionMode(String processName) {
        return "清洗工序".equals(processName) ? "MULTIPLE" : "SINGLE";
    }

    private boolean isPotentialParameterName(String text) {
        if (text.isBlank() || text.length() > 30 || !PARAMETER_NAME_PATTERN.matcher(text).matches()) {
            return false;
        }
        return !text.contains("操作日期")
                && !text.contains("物料编码")
                && !text.contains("物料名称")
                && !text.equals("批号")
                && !text.contains("设备编码")
                && !text.contains("计量效期")
                && !text.contains("生产数量")
                && !text.contains("合格数量")
                && !text.contains("不合格数量")
                && !text.equals("操作人")
                && !text.equals("复核人");
    }

    private boolean isReferenceMarker(String text) {
        return "参考值".equals(text) || "参数".equals(text);
    }

    private boolean isReferenceValue(String text) {
        if (text.isBlank() || isReferenceMarker(text) || "实际".equals(text)
                || text.contains("□") || text.contains("☑")
                || text.contains("生产自检") || text.contains("生产批量汇总")) {
            return false;
        }
        return !text.matches("A\\d{3}(?:\\.\\d+){2,}")
                && !EQUIPMENT_CODE_PATTERN.matcher(text).matches();
    }

    private String resolveProcessName(MesProBatchRecordParsedTable table) {
        Matcher matcher = PROCESS_TITLE_PATTERN.matcher(normalized(table == null ? null : table.getTableTitle()));
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

    private int firstRowContaining(List<List<MesProBatchRecordParsedCell>> rows, String expected) {
        String compactExpected = compact(expected);
        for (int index = 0; index < rows.size(); index++) {
            if (compact(rowText(rows.get(index))).contains(compactExpected)) {
                return index;
            }
        }
        return -1;
    }

    private List<List<MesProBatchRecordParsedCell>> safeRows(MesProBatchRecordParsedTable table) {
        return table == null || table.getRows() == null ? List.of() : table.getRows();
    }

    private String rowText(List<MesProBatchRecordParsedCell> row) {
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row == null ? List.<MesProBatchRecordParsedCell>of() : row) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(normalized(cell.getText()));
        }
        return builder.toString();
    }

    private boolean overlaps(MesProBatchRecordParsedCell left, MesProBatchRecordParsedCell right) {
        return startColumn(left) < endColumn(right) && startColumn(right) < endColumn(left);
    }

    private int startColumn(MesProBatchRecordParsedCell cell) {
        return cell.getColumnIndex() == null ? 0 : cell.getColumnIndex();
    }

    private int endColumn(MesProBatchRecordParsedCell cell) {
        int span = Math.max(1, cell.getColSpan());
        return startColumn(cell) + span;
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

    private String stripCheckbox(String value) {
        String text = compact(value);
        while (text.startsWith("□") || text.startsWith("☑")) {
            text = text.substring(1);
        }
        return text;
    }

    public record RecognitionResult(Product product, int schemaVersion, List<ProcessRecognition> processes) {
        public RecognitionResult {
            processes = List.copyOf(processes);
        }
    }

    public record Product(String name, String code) {
    }

    public record ProcessRecognition(String name, List<Material> inputs, List<Material> outputs,
                                     List<EquipmentGroup> equipmentGroups) {
        public ProcessRecognition {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            equipmentGroups = List.copyOf(equipmentGroups);
        }
    }

    public record Material(String code, String name,
                           @JsonInclude(JsonInclude.Include.NON_NULL) String sourceCodeLabel) {
    }

    public record EquipmentGroup(List<EquipmentOption> equipmentOptions, List<Parameter> parameters,
                                 String selectionMode) {
        public EquipmentGroup {
            equipmentOptions = List.copyOf(equipmentOptions);
            parameters = List.copyOf(parameters);
        }
    }

    public record EquipmentOption(String code, String name) {
    }

    private Parameter toParameter(String processName, String name, String referenceValue) {
        String effectiveReference = referenceValue;
        if ("粗洗工序".equals(processName) && "清洗介质".equals(name)) {
            effectiveReference = "自来水";
        }
        if ("清洗温度".equals(name) && "室温".equals(referenceValue)) {
            effectiveReference = "20-30℃";
        }
        Ui ui = resolveUi(processName, name, effectiveReference);
        return new Parameter(name, effectiveReference, ACTUAL_VALUE_PLACEHOLDER, ui);
    }

    private Ui resolveUi(String processName, String name, String referenceValue) {
        if ("清洗介质".equals(name)) {
            return new Ui("select", referenceValue, null, null, null, null, null,
                    "自来水".equals(referenceValue) ? List.of("自来水", "纯化水") : List.of(referenceValue));
        }
        if ("清洗温度".equals(name) && "20-30℃".equals(referenceValue)) {
            return new Ui("number", BigDecimal.valueOf(26), BigDecimal.ONE, BigDecimal.valueOf(20), BigDecimal.valueOf(30), "℃", null, null);
        }
        String unit = unit(name, referenceValue);
        Matcher plusMinus = Pattern.compile("(-?\\d+(?:\\.\\d+)?)±(\\d+(?:\\.\\d+)?)").matcher(referenceValue);
        if (plusMinus.matches()) {
            BigDecimal center = new BigDecimal(plusMinus.group(1));
            BigDecimal delta = new BigDecimal(plusMinus.group(2));
            return numeric(center, center.subtract(delta), center.add(delta), unit);
        }
        Matcher range = Pattern.compile("(-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)(?:[%℃])?").matcher(referenceValue);
        if (range.matches()) {
            BigDecimal min = new BigDecimal(range.group(1));
            BigDecimal max = new BigDecimal(range.group(2));
            return numeric(min.add(max).divide(BigDecimal.valueOf(2)), min, max, unit);
        }
        Matcher exact = Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:[A-Za-z%℃]+)?").matcher(referenceValue);
        if (exact.matches()) {
            BigDecimal value = new BigDecimal(exact.group(1));
            return numeric(value, name.contains("次数") || name.endsWith("数量") ? BigDecimal.ONE : null, null, unit);
        }
        return new Ui("text", referenceValue, null, null, null, null, null, null);
    }

    private Ui numeric(BigDecimal value, BigDecimal min, BigDecimal max, String unit) {
        int scale = Math.max(value.scale(), Math.max(min == null ? 0 : min.scale(), max == null ? 0 : max.scale()));
        BigDecimal step = "h".equals(unit) || "s".equals(unit) ? BigDecimal.valueOf(0.1)
                : scale <= 0 ? BigDecimal.ONE : BigDecimal.ONE.movePointLeft(scale);
        return new Ui("number", plain(value), step, plain(min), plain(max), unit, null, null);
    }

    private BigDecimal plain(BigDecimal value) {
        if (value == null) return null;
        return new BigDecimal(value.setScale(Math.max(0, value.scale()), RoundingMode.UNNECESSARY).toPlainString());
    }

    private String unit(String name, String referenceValue) {
        if (name.contains("℃") || referenceValue.endsWith("℃")) return "℃";
        if (name.contains("kPa")) return "kPa";
        if (name.contains("MPa")) return "MPa";
        if (name.contains("ms")) return "ms";
        if (name.endsWith("s")) return "s";
        if (referenceValue.endsWith("min")) return "min";
        if (referenceValue.endsWith("h")) return "h";
        if (referenceValue.endsWith("%")) return "%";
        if (name.contains("次数")) return "次";
        if (name.contains("数量")) return "个";
        if (name.contains("mJ/cm2")) return "mJ/cm2";
        return null;
    }

    public record Parameter(String name, String referenceValue, String actualValue, Ui ui) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Ui(String control, Object defaultValue, BigDecimal step, BigDecimal min, BigDecimal max,
                     String unit, String displayName, List<String> options) {
    }
}
