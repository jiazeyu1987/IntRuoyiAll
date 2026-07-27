package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MesProBatchRecordCellRuleSupport {

    public static final String CELL_RULE_KEY = "edhrCellRule";
    public static final String SIGNATURE_KEY = "edhrSignature";
    public static final String FILL_FORM_KEY = "fillForm";
    public static final String MANUAL_FILL_CELL_KEY = "edhrManualFillCell";

    private static final Set<String> SUPPORTED_VALUE_TYPES = Set.of(
            "STRING", "NUMBER", "DATE", "DATETIME", "BOOLEAN", "SIGNATURE");
    private static final Set<String> SIGNATURE_ACTION_TYPES = Set.of("FORM_REVIEW", "SUBMIT", "APPROVE");
    private static final Set<String> SINGLE_CHOICE_COMPONENT_FLAGS = Set.of("radio-group", "option-group", "single-choice", "select");
    private static final Pattern UNIT_PATTERN = Pattern.compile("[（(]([A-Za-z%℃°μΩ/\\u4e00-\\u9fa5]{1,12})[）)]");
    private static final List<String> KNOWN_UNITS = List.of(
            "℃", "°C", "kg", "g", "mg", "μg", "L", "mL", "ml", "mm", "cm", "m",
            "pcs", "支", "个", "件", "min", "h", "s", "%", "MPa", "kPa", "V", "A");

    private MesProBatchRecordCellRuleSupport() {
    }

    public static boolean isSupportedValueType(String valueType) {
        return SUPPORTED_VALUE_TYPES.contains(normalizeValueType(valueType));
    }

    public static String normalizeValueType(String valueType) {
        return StrUtil.blankToDefault(valueType, "").trim().toUpperCase(Locale.ROOT);
    }

    public static String defaultComponentFlag(String valueType, String existingComponentFlag) {
        return switch (normalizeValueType(valueType)) {
            case "NUMBER" -> "input-number";
            case "DATE" -> "date";
            case "DATETIME" -> "datetime";
            case "BOOLEAN" -> "checkbox";
            case "SIGNATURE" -> "signature";
            default -> StrUtil.blankToDefault(existingComponentFlag, "input-text");
        };
    }

    public static boolean isFillableCell(JSONObject cell) {
        JSONObject fillForm = cell == null ? null : cell.getJSONObject(FILL_FORM_KEY);
        return fillForm != null && StrUtil.isNotBlank(fillForm.getString("field"));
    }

    public static boolean isStaticCheckboxChoiceCell(JSONObject cell) {
        return cell != null && !isFillableCell(cell) && isCheckboxChoiceText(cellText(cell));
    }

    public static boolean isFillableCandidateCell(JSONObject cell) {
        return isFillableCell(cell) || isStaticCheckboxChoiceCell(cell);
    }

    public static boolean isCheckboxChoiceText(String text) {
        String normalized = StrUtil.blankToDefault(text, "").trim();
        if (normalized.isBlank() || !containsAny(normalized, "□", "☐")) {
            return false;
        }
        if (containsAny(normalized, "☑", "☒")) {
            return false;
        }
        int markerIndex = firstCheckboxMarkerIndex(normalized);
        if (markerIndex > 0 && !Character.isWhitespace(normalized.charAt(markerIndex - 1))) {
            return false;
        }
        String choiceLabel = normalizeCheckboxChoiceLabel(normalized);
        if (!containsCheckboxChoiceLabelToken(choiceLabel)) {
            return false;
        }
        if (isCompactUncheckedCheckboxChoiceGroup(normalized)) {
            return true;
        }
        if (isWrappedSingleUncheckedCheckboxChoice(normalized, choiceLabel)) {
            return true;
        }
        if (normalized.length() > 40 || normalized.contains("\n") || normalized.contains("\r")) {
            return false;
        }
        return true;
    }

    private static boolean containsCheckboxChoiceLabelToken(String label) {
        String normalized = StrUtil.blankToDefault(label, "").trim();
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.matches(".*[\\u4e00-\\u9fa5].*")
                || normalized.matches(".*[A-Za-z]+[0-9]+.*")
                || normalized.matches(".*[0-9]+[A-Za-z]+.*");
    }

    private static boolean isWrappedSingleUncheckedCheckboxChoice(String text, String label) {
        String canonical = StrUtil.blankToDefault(text, "")
                .replace('\r', '\n')
                .trim();
        List<String> lines = new ArrayList<>();
        for (String line : canonical.split("\\n+")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                lines.add(trimmed);
            }
        }
        if (lines.size() < 2 || lines.size() > 3 || !startsWithUncheckedCheckboxMarker(lines.get(0))
                || countUncheckedCheckboxMarkers(canonical) != 1
                || StrUtil.blankToDefault(label, "").length() > 60) {
            return false;
        }
        for (int index = 1; index < lines.size(); index++) {
            if (!isCompactCheckboxChoiceContinuation(lines.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCompactCheckboxChoiceContinuation(String text) {
        String trimmed = StrUtil.blankToDefault(text, "").trim();
        return !trimmed.isBlank()
                && trimmed.length() <= 24
                && trimmed.matches("[（(]?[A-Za-z0-9][A-Za-z0-9_./\\-]*[）)]?");
    }

    private static boolean isCompactUncheckedCheckboxChoiceGroup(String text) {
        String canonical = StrUtil.blankToDefault(text, "")
                .replace('\r', '\n')
                .trim();
        if (canonical.isBlank()) {
            return false;
        }
        List<String> lines = new ArrayList<>();
        for (String line : canonical.split("\\n+")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                lines.add(trimmed);
            }
        }
        if (lines.isEmpty() || lines.size() > 4) {
            return false;
        }
        int markerCount = 0;
        for (String line : lines) {
            if (!startsWithUncheckedCheckboxMarker(line)) {
                return false;
            }
            int lineMarkerCount = countUncheckedCheckboxMarkers(line);
            if (lineMarkerCount == 0 || lineMarkerCount > 3) {
                return false;
            }
            markerCount += lineMarkerCount;
        }
        String label = normalizeCheckboxChoiceLabel(canonical);
        return markerCount > 0 && (lines.size() > 1 || markerCount > 1) && label.length() <= 60;
    }

    private static boolean startsWithUncheckedCheckboxMarker(String text) {
        String trimmed = StrUtil.blankToDefault(text, "").trim();
        return !trimmed.isEmpty() && isUncheckedCheckboxMarker(trimmed.codePointAt(0));
    }

    private static int countUncheckedCheckboxMarkers(String text) {
        return countCheckboxMarkers(text, false);
    }

    static boolean containsAtLeastTwoCheckboxMarkers(String text) {
        return countCheckboxMarkers(text, true) >= 2;
    }

    private static int countCheckboxMarkers(String text, boolean includeChecked) {
        int count = 0;
        String normalized = StrUtil.blankToDefault(text, "");
        for (int offset = 0; offset < normalized.length(); ) {
            int codePoint = normalized.codePointAt(offset);
            if (isCheckboxMarker(codePoint, includeChecked)) {
                count++;
            }
            offset += Character.charCount(codePoint);
        }
        return count;
    }

    private static int firstCheckboxMarkerIndex(String text) {
        String normalized = StrUtil.blankToDefault(text, "");
        for (int offset = 0; offset < normalized.length(); ) {
            int codePoint = normalized.codePointAt(offset);
            if (isUncheckedCheckboxMarker(codePoint)) {
                return offset;
            }
            offset += Character.charCount(codePoint);
        }
        return -1;
    }

    private static boolean isUncheckedCheckboxMarker(int codePoint) {
        return codePoint == '□' || codePoint == '☐';
    }

    private static boolean isCheckboxMarker(int codePoint, boolean includeChecked) {
        if (isUncheckedCheckboxMarker(codePoint)) {
            return true;
        }
        return includeChecked && (codePoint == '☑' || codePoint == '☒');
    }

    public static String normalizeCheckboxChoiceLabel(String text) {
        String label = StrUtil.blankToDefault(text, "")
                .replace("□", " ")
                .replace("☐", " ")
                .replace("☑", " ")
                .replace("☒", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return StrUtil.blankToDefault(label, "勾选");
    }

    public static List<String> splitUncheckedCheckboxChoiceLabels(String text) {
        String normalized = StrUtil.blankToDefault(text, "")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank() || !startsWithUncheckedCheckboxMarker(normalized)) {
            return List.of();
        }
        List<Integer> markerIndexes = uncheckedCheckboxMarkerIndexes(normalized);
        if (markerIndexes.size() <= 1) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < markerIndexes.size(); index++) {
            int start = markerIndexes.get(index);
            int end = index + 1 < markerIndexes.size() ? markerIndexes.get(index + 1) : normalized.length();
            String label = normalizeCheckboxChoiceLabel(normalized.substring(start, end));
            label = stripChoiceLabelFillBlank(label);
            if (!label.isBlank()) {
                labels.add(label);
            }
        }
        return labels;
    }

    public static boolean hasMultipleUncheckedCheckboxChoiceLabels(String text) {
        return splitUncheckedCheckboxChoiceLabels(text).size() > 1;
    }

    static BatchRecordReportCellRuleVO buildAutoCheckboxRule(Integer rowIndex, Integer columnIndex, String text) {
        return buildAutoCheckboxRule(rowIndex, columnIndex, text, null);
    }

    private static BatchRecordReportCellRuleVO buildAutoCheckboxRule(Integer rowIndex, Integer columnIndex,
                                                                     String text, String groupLabel) {
        List<String> choiceLabels = splitUncheckedCheckboxChoiceLabels(text);
        if (choiceLabels.size() > 1) {
            String label = normalizeCheckboxChoiceGroupLabel(
                    StrUtil.blankToDefault(StrUtil.trim(groupLabel), normalizeCheckboxChoiceLabel(text)),
                    choiceLabels);
            BatchRecordReportCellRuleVO rule = baseSuggestion(rowIndex, columnIndex, "STRING", "radio-group",
                    label, null, 0.92, false);
            rule.setConstraints(singleChoiceConstraints(choiceLabels));
            return rule;
        }
        return baseSuggestion(rowIndex, columnIndex, "BOOLEAN", "checkbox",
                normalizeCheckboxChoiceLabel(text), null, 0.92, false);
    }

    static Object defaultFillValue(String valueType) {
        return "BOOLEAN".equals(normalizeValueType(valueType)) ? Boolean.FALSE : "";
    }

    private static List<Integer> uncheckedCheckboxMarkerIndexes(String text) {
        List<Integer> indexes = new ArrayList<>();
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (isUncheckedCheckboxMarker(codePoint)) {
                indexes.add(offset);
            }
            offset += Character.charCount(codePoint);
        }
        return indexes;
    }

    public static boolean isManualFillCell(JSONObject cell) {
        JSONObject fillForm = cell == null ? null : cell.getJSONObject(FILL_FORM_KEY);
        return fillForm != null && Boolean.TRUE.equals(fillForm.getBoolean(MANUAL_FILL_CELL_KEY));
    }

    public static boolean isReviewedRule(JSONObject cellRule) {
        return cellRule != null
                && Boolean.TRUE.equals(cellRule.getBoolean("reviewed"))
                && !"AUTO".equalsIgnoreCase(StrUtil.trim(cellRule.getString("source")))
                && isSupportedValueType(cellRule.getString("valueType"));
    }

    public static boolean hasValidSignatureMarker(JSONObject cell) {
        JSONObject signature = cell == null ? null : cell.getJSONObject(SIGNATURE_KEY);
        return signature != null
                && Boolean.TRUE.equals(signature.getBoolean("enabled"))
                && SIGNATURE_ACTION_TYPES.contains(signature.getString("actionType"));
    }

    public static List<BatchRecordReportCellRuleVO> extractReviewedRules(JSONObject root) {
        List<BatchRecordReportCellRuleVO> rules = new ArrayList<>();
        forEachCell(root, (rowIndex, columnIndex, cell) -> {
            JSONObject rule = cell.getJSONObject(CELL_RULE_KEY);
            if (isReviewedRule(rule)) {
                rules.add(toRuleVO(rowIndex, columnIndex, rule));
            }
        });
        rules.sort(ruleComparator());
        return rules;
    }

    public static List<BatchRecordReportCellRuleVO> buildSuggestions(JSONObject root) {
        List<BatchRecordReportCellRuleVO> suggestions = new ArrayList<>();
        JSONObject rows = root.getJSONObject("rows");
        forEachCell(root, (rowIndex, columnIndex, cell) -> {
            if (!isFillableCandidateCell(cell)) {
                return;
            }
            JSONObject existing = cell.getJSONObject(CELL_RULE_KEY);
            if (isReviewedRule(existing)) {
                suggestions.add(withFillFormPlaceholder(toRuleVO(rowIndex, columnIndex, existing), cell));
                return;
            }
            suggestions.add(withFillFormPlaceholder(suggestRule(rows, rowIndex, columnIndex, cell), cell));
        });
        suggestions.sort(ruleComparator());
        return suggestions;
    }

    public static int applyAutomaticSuggestions(JSONObject root, String reportCode) {
        Counter counter = new Counter();
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        forEachCell(root, (rowIndex, columnIndex, cell) -> {
            if (!isFillableCandidateCell(cell)) {
                return;
            }
            JSONObject existing = cell.getJSONObject(CELL_RULE_KEY);
            if (isReviewedRule(existing)) {
                return;
            }
            BatchRecordReportCellRuleVO suggestion = withFillFormPlaceholder(
                    suggestRule(rows, rowIndex, columnIndex, cell), cell);
            ensureManualFillForm(suggestion, cell, reportCode);
            cell.put(CELL_RULE_KEY, toRuleJson(suggestion));
            counter.value++;
        });
        return counter.value;
    }

    public static int refreshUnreviewedAutomaticSuggestions(JSONObject root, String reportCode) {
        Counter counter = new Counter();
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        forEachCell(root, (rowIndex, columnIndex, cell) -> {
            if (!isFillableCandidateCell(cell)) {
                return;
            }
            JSONObject existing = cell.getJSONObject(CELL_RULE_KEY);
            if (isReviewedRule(existing)) {
                return;
            }
            String beforeRule = existing == null ? "" : existing.toJSONString();
            JSONObject fillForm = cell.getJSONObject(FILL_FORM_KEY);
            String beforeFillForm = fillForm == null ? "" : fillForm.toJSONString();
            BatchRecordReportCellRuleVO suggestion = withFillFormPlaceholder(
                    suggestRule(rows, rowIndex, columnIndex, cell), cell);
            ensureManualFillForm(suggestion, cell, reportCode);
            JSONObject nextRule = toRuleJson(suggestion);
            cell.put(CELL_RULE_KEY, nextRule);
            JSONObject nextFillForm = cell.getJSONObject(FILL_FORM_KEY);
            String afterFillForm = nextFillForm == null ? "" : nextFillForm.toJSONString();
            if (!Objects.equals(beforeRule, nextRule.toJSONString())
                    || !Objects.equals(beforeFillForm, afterFillForm)) {
                counter.value++;
            }
        });
        return counter.value;
    }

    public static int materializeVersionApprovedCellRules(JSONObject root, String reportCode) {
        Counter counter = new Counter();
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        forEachCell(root, (rowIndex, columnIndex, cell) -> {
            if (!isFillableCandidateCell(cell) || hasValidSignatureMarker(cell)) {
                return;
            }
            JSONObject existing = cell.getJSONObject(CELL_RULE_KEY);
            if (isReviewedRule(existing)) {
                return;
            }
            if (existing == null) {
                BatchRecordReportCellRuleVO suggestion = withFillFormPlaceholder(
                        suggestRule(rows, rowIndex, columnIndex, cell), cell);
                ensureManualFillForm(suggestion, cell, reportCode);
                cell.put(CELL_RULE_KEY, toRuleJson(suggestion));
                existing = cell.getJSONObject(CELL_RULE_KEY);
            }
            if (existing != null) {
                existing.put("source", "VERSION_APPROVED");
                existing.put("reviewed", true);
                counter.value++;
            }
        });
        return counter.value;
    }

    public static int countUnreviewedFillableCells(JSONObject root) {
        Counter counter = new Counter();
        forEachCell(root, (rowIndex, columnIndex, cell) -> {
            if (isUnreviewedFillableCandidate(cell)) {
                counter.value++;
            }
        });
        return counter.value;
    }

    public static int normalizeAutomaticRulesAsUnreviewed(JSONObject root) {
        Counter counter = new Counter();
        forEachCell(root, (rowIndex, columnIndex, cell) -> {
            JSONObject rule = cell.getJSONObject(CELL_RULE_KEY);
            if (rule == null || !"AUTO".equalsIgnoreCase(StrUtil.trim(rule.getString("source")))
                    || !Boolean.TRUE.equals(rule.getBoolean("reviewed"))) {
                return;
            }
            rule.put("reviewed", false);
            counter.value++;
        });
        return counter.value;
    }

    public static JSONObject toRuleJson(BatchRecordReportCellRuleVO rule) {
        JSONObject json = new JSONObject(true);
        String valueType = normalizeValueType(rule.getValueType());
        json.put("rowIndex", rule.getRowIndex());
        json.put("columnIndex", rule.getColumnIndex());
        json.put("valueType", valueType);
        json.put("componentFlag", defaultComponentFlag(valueType, rule.getComponentFlag()));
        json.put("required", Boolean.TRUE.equals(rule.getRequired()));
        json.put("label", StrUtil.trim(rule.getLabel()));
        if (StrUtil.isNotBlank(rule.getPlaceholder())) {
            json.put("placeholder", StrUtil.trim(rule.getPlaceholder()));
        }
        if (StrUtil.isNotBlank(rule.getHelpText())) {
            json.put("helpText", StrUtil.trim(rule.getHelpText()));
        }
        json.put("constraints", mapToJson(rule.getConstraints()));
        JSONObject attachmentRule = mapToJson(rule.getAttachmentRule());
        if (!attachmentRule.isEmpty()) {
            json.put("attachmentRule", attachmentRule);
        }
        if (StrUtil.isNotBlank(rule.getUnit())) {
            json.put("unit", StrUtil.trim(rule.getUnit()));
        }
        boolean reviewed = Boolean.TRUE.equals(rule.getReviewed());
        json.put("source", normalizePersistedRuleSource(rule.getSource(), reviewed));
        json.put("confidence", rule.getConfidence() == null ? 1.0 : rule.getConfidence());
        json.put("reviewed", reviewed);
        return json;
    }

    private static String normalizePersistedRuleSource(String source, boolean reviewed) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(source), reviewed ? "MANUAL" : "AUTO");
        if (reviewed && "AUTO".equalsIgnoreCase(normalized)) {
            return "MANUAL";
        }
        return normalized;
    }

    public static BatchRecordReportCellRuleVO toRuleVO(Integer rowIndex, Integer columnIndex, JSONObject rule) {
        Map<String, Object> constraints = new LinkedHashMap<>();
        JSONObject constraintJson = rule.getJSONObject("constraints");
        if (constraintJson != null) {
            constraints.putAll(constraintJson);
        }
        Map<String, Object> attachmentRule = new LinkedHashMap<>();
        JSONObject attachmentRuleJson = rule.getJSONObject("attachmentRule");
        if (attachmentRuleJson != null) {
            attachmentRule.putAll(attachmentRuleJson);
        }
        return new BatchRecordReportCellRuleVO()
                .setRowIndex(rule.getInteger("rowIndex") == null ? rowIndex : rule.getInteger("rowIndex"))
                .setColumnIndex(rule.getInteger("columnIndex") == null ? columnIndex : rule.getInteger("columnIndex"))
                .setValueType(normalizeValueType(rule.getString("valueType")))
                .setComponentFlag(rule.getString("componentFlag"))
                .setRequired(rule.getBoolean("required"))
                .setLabel(rule.getString("label"))
                .setPlaceholder(rule.getString("placeholder"))
                .setHelpText(rule.getString("helpText"))
                .setConstraints(constraints)
                .setAttachmentRule(attachmentRule)
                .setUnit(rule.getString("unit"))
                .setSource(rule.getString("source"))
                .setConfidence(rule.getDouble("confidence"))
                .setReviewed(rule.getBoolean("reviewed"));
    }

    public static JSONObject requireCell(JSONObject root, Integer rowIndex, Integer columnIndex) {
        JSONObject rows = root.getJSONObject("rows");
        JSONObject row = rows == null ? null : rows.getJSONObject(String.valueOf(rowIndex));
        JSONObject cells = row == null ? null : row.getJSONObject("cells");
        return cells == null ? null : cells.getJSONObject(String.valueOf(columnIndex));
    }

    public static void validateRule(BatchRecordReportCellRuleVO rule, JSONObject cell) {
        if (rule == null || rule.getRowIndex() == null || rule.getColumnIndex() == null
                || rule.getRowIndex() < 0 || rule.getColumnIndex() < 0) {
            throw new IllegalArgumentException("rowIndex/columnIndex must be non-negative");
        }
        String valueType = normalizeValueType(rule.getValueType());
        if (!SUPPORTED_VALUE_TYPES.contains(valueType)) {
            throw new IllegalArgumentException("unsupported valueType " + rule.getValueType());
        }
        if ("SIGNATURE".equals(valueType) && !hasValidSignatureMarker(cell)) {
            throw new IllegalArgumentException("SIGNATURE rule requires enabled edhrSignature marker");
        }
        if (!"SIGNATURE".equals(valueType) && !isFillableCell(cell)) {
            throw new IllegalArgumentException("non-signature rule requires fillForm field");
        }
        validateConstraints(valueType, rule.getConstraints());
        validateAttachmentRule(rule.getAttachmentRule());
        if (rule.getConfidence() != null && (rule.getConfidence() < 0 || rule.getConfidence() > 1)) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
    }

    public static void ensureManualFillForm(BatchRecordReportCellRuleVO rule, JSONObject cell, String reportCode) {
        if (rule == null || cell == null || "SIGNATURE".equals(normalizeValueType(rule.getValueType()))) {
            return;
        }
        String valueType = normalizeValueType(rule.getValueType());
        JSONObject fillForm = cell.getJSONObject(FILL_FORM_KEY);
        if (fillForm == null || StrUtil.isBlank(fillForm.getString("field"))) {
            fillForm = new JSONObject(true);
            fillForm.put("component", "Input");
            fillForm.put("field", buildManualFieldName(reportCode, rule.getRowIndex(), rule.getColumnIndex()));
            Object defaultValue = defaultFillValue(valueType);
            fillForm.put("value", defaultValue);
            fillForm.put("defaultValue", defaultValue);
            fillForm.put("placeholder", "");
            fillForm.put("requiredTip", "不能为空~");
            fillForm.put("label", "");
            fillForm.put("pattern", "");
            fillForm.put("patternErrorTip", "");
            fillForm.put("requiredRelevanceCell", "");
            fillForm.put(MANUAL_FILL_CELL_KEY, true);
            cell.put(FILL_FORM_KEY, fillForm);
        }
        syncFillFormWithRule(rule, fillForm);
    }

    public static void removeManualFillForm(JSONObject cell) {
        if (isManualFillCell(cell)) {
            cell.remove(FILL_FORM_KEY);
        }
    }

    private static String buildManualFieldName(String reportCode, Integer rowIndex, Integer columnIndex) {
        String normalizedReportCode = StrUtil.blankToDefault(reportCode, "EBR").replaceAll("[^A-Za-z0-9_]", "_");
        return "ebr_" + normalizedReportCode + "_r" + rowIndex + "_c" + columnIndex;
    }

    private static void validateAttachmentRule(Map<String, Object> attachmentRule) {
        if (attachmentRule == null || attachmentRule.isEmpty()) {
            return;
        }
        Object required = attachmentRule.get("required");
        if (required != null && !(required instanceof Boolean)) {
            throw new IllegalArgumentException("attachmentRule.required must be boolean");
        }
        validatePositiveInteger(attachmentRule, "minCount");
        validatePositiveInteger(attachmentRule, "maxCount");
        Integer minCount = toInteger(attachmentRule.get("minCount"));
        Integer maxCount = toInteger(attachmentRule.get("maxCount"));
        if (minCount != null && maxCount != null && minCount > maxCount) {
            throw new IllegalArgumentException("attachmentRule.minCount must not exceed maxCount");
        }
        Object attachmentType = attachmentRule.get("attachmentType");
        if (attachmentType != null && StrUtil.isBlank(String.valueOf(attachmentType))) {
            throw new IllegalArgumentException("attachmentRule.attachmentType must not be blank");
        }
        Object groupKey = attachmentRule.get("groupKey");
        if (groupKey != null && StrUtil.isBlank(String.valueOf(groupKey))) {
            throw new IllegalArgumentException("attachmentRule.groupKey must not be blank");
        }
    }

    private static void validatePositiveInteger(Map<String, Object> attachmentRule, String key) {
        Integer value = toInteger(attachmentRule.get(key));
        if (value != null && value < 1) {
            throw new IllegalArgumentException("attachmentRule." + key + " must be positive integer");
        }
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("attachmentRule count must be numeric");
    }

    public static List<String> unreviewedFillableCoordinates(JSONObject root) {
        List<String> coordinates = new ArrayList<>();
        forEachCell(root, (rowIndex, columnIndex, cell) -> {
            if (isUnreviewedFillableCandidate(cell)) {
                coordinates.add("第 " + (rowIndex + 1) + " 行第 " + (columnIndex + 1) + " 列");
            }
        });
        return coordinates;
    }

    private static boolean isUnreviewedFillableCandidate(JSONObject cell) {
        return isFillableCandidateCell(cell)
                && !hasValidSignatureMarker(cell)
                && !isReviewedRule(cell.getJSONObject(CELL_RULE_KEY));
    }

    public static void forEachCell(JSONObject root, CellConsumer consumer) {
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        if (rows == null) {
            return;
        }
        numericKeys(rows).forEach(rowIndex -> {
            JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                return;
            }
            numericKeys(cells).forEach(columnIndex -> {
                JSONObject cell = cells.getJSONObject(String.valueOf(columnIndex));
                if (cell != null) {
                    consumer.accept(rowIndex, columnIndex, cell);
                }
            });
        });
    }

    private static BatchRecordReportCellRuleVO suggestRule(JSONObject rows, Integer rowIndex, Integer columnIndex,
                                                           JSONObject cell) {
        JSONObject fillForm = cell.getJSONObject("fillForm");
        String existingComponent = fillForm == null ? null : fillForm.getString("componentFlag");
        String ownText = cellText(cell);
        String label = resolveSuggestedLabel(rows, rowIndex, columnIndex, ownText);
        if (hasValidSignatureMarker(cell)) {
            return baseSuggestion(rowIndex, columnIndex, "SIGNATURE", "signature", label, null, 0.99, false);
        }
        String signatureDateHeader = resolveUpperSignatureDateLabel(rows, rowIndex, columnIndex);
        if (StrUtil.isNotBlank(signatureDateHeader)
                && (StrUtil.isBlank(compact(ownText)) || isCheckboxChoiceText(ownText))) {
            return baseSuggestion(rowIndex, columnIndex, "STRING", "input-text",
                    signatureDateHeader, null, 0.38, false);
        }
        if (isCheckboxChoiceText(ownText)) {
            return buildAutoCheckboxRule(rowIndex, columnIndex, ownText, label);
        }
        String compactLabel = compact(label);
        String unit = resolveUnit(label);
        if (isSignatureDateLabel(compactLabel)) {
            return baseSuggestion(rowIndex, columnIndex, "STRING", "input-text",
                    label, null, 0.35, false);
        }
        if (compactLabel.contains("□其他") || compactLabel.contains("☑其他")) {
            return baseSuggestion(rowIndex, columnIndex, "STRING",
                    defaultTextComponentFlag(existingComponent, compactLabel),
                    label, null, 0.88, false);
        }
        if (containsAny(compactLabel, "日期", "年月日")) {
            return baseSuggestion(rowIndex, columnIndex, "DATE", "date", label, null, 0.9, false);
        }
        if (containsIdentifierCue(compactLabel)) {
            return baseSuggestion(rowIndex, columnIndex, "STRING",
                    defaultTextComponentFlag(existingComponent, compactLabel),
                    label, null, 0.78, false);
        }
        if (hasBooleanCue(compactLabel)) {
            return baseSuggestion(rowIndex, columnIndex, "BOOLEAN", "checkbox", label, null, 0.86, false);
        }
        if (containsAny(compactLabel, "签名", "签字")) {
            return baseSuggestion(rowIndex, columnIndex, "STRING",
                    defaultTextComponentFlag(existingComponent, compactLabel),
                    label, null, 0.45, false);
        }
        if (isDateTimeLabel(compactLabel, unit)) {
            return baseSuggestion(rowIndex, columnIndex, "DATETIME", "datetime", label, null, 0.82, false);
        }
        if (unit != null || containsAny(compactLabel, "数量", "重量", "温度", "压力", "体积", "时长", "用时", "耗时", "持续", "长度", "宽度",
                "高度", "厚度", "速度", "电压", "电流", "批量", "含量", "浓度", "转速", "扭矩")) {
            return baseSuggestion(rowIndex, columnIndex, "NUMBER", "input-number", label, unit, 0.84, false);
        }
        return baseSuggestion(rowIndex, columnIndex, "STRING",
                defaultTextComponentFlag(existingComponent, compactLabel),
                label, null, 0.4, false);
    }

    private static String defaultTextComponentFlag(String existingComponentFlag, String compactLabel) {
        String normalized = StrUtil.blankToDefault(existingComponentFlag, "").trim().toLowerCase(Locale.ROOT);
        if ((normalized.contains("checkbox") || normalized.contains("boolean"))
                && !hasBooleanCue(compactLabel)) {
            return "input-text";
        }
        return defaultComponentFlag("STRING", existingComponentFlag);
    }

    private static String resolveSuggestedLabel(JSONObject rows, Integer rowIndex, Integer columnIndex, String ownText) {
        if (!isCheckboxChoiceText(ownText)) {
            return resolveNeighborLabel(rows, rowIndex, columnIndex);
        }
        if (!hasMultipleUncheckedCheckboxChoiceLabels(ownText)) {
            return normalizeCheckboxChoiceLabel(ownText);
        }
        String upperLabel = resolveUpperLabel(rows, rowIndex, columnIndex);
        if (StrUtil.isNotBlank(upperLabel) && !containsAny(upperLabel, "□", "☐", "☑", "☒")) {
            return upperLabel;
        }
        String leftLabel = resolveLeftLabel(rows, rowIndex, columnIndex);
        return StrUtil.isNotBlank(leftLabel) ? leftLabel : resolveNeighborLabel(rows, rowIndex, columnIndex);
    }

    private static BatchRecordReportCellRuleVO baseSuggestion(Integer rowIndex, Integer columnIndex, String valueType,
                                                               String componentFlag, String label, String unit,
                                                               double confidence, boolean reviewed) {
        String normalizedValueType = normalizeValueType(valueType);
        return new BatchRecordReportCellRuleVO()
                .setRowIndex(rowIndex)
                .setColumnIndex(columnIndex)
                .setValueType(normalizedValueType)
                .setComponentFlag(defaultComponentFlag(normalizedValueType, componentFlag))
                .setRequired(false)
                .setLabel(StrUtil.blankToDefault(StrUtil.trim(label), "第 " + (rowIndex + 1) + " 行第 "
                        + (columnIndex + 1) + " 列"))
                .setHelpText(defaultHelpText(normalizedValueType,
                        StrUtil.blankToDefault(StrUtil.trim(label), "第 " + (rowIndex + 1) + " 行第 "
                                + (columnIndex + 1) + " 列"), unit))
                .setConstraints(inferConstraints(normalizedValueType, label, unit))
                .setUnit(unit)
                .setSource("AUTO")
                .setConfidence(confidence)
                .setReviewed(reviewed);
    }

    private static BatchRecordReportCellRuleVO withFillFormPlaceholder(BatchRecordReportCellRuleVO rule, JSONObject cell) {
        JSONObject fillForm = cell == null ? null : cell.getJSONObject(FILL_FORM_KEY);
        if (rule != null && rule.getPlaceholder() == null && fillForm != null
                && StrUtil.isNotBlank(fillForm.getString("placeholder"))) {
            rule.setPlaceholder(StrUtil.trim(fillForm.getString("placeholder")));
        }
        if (rule != null && rule.getHelpText() == null && fillForm != null
                && StrUtil.isNotBlank(fillForm.getString("helpText"))) {
            rule.setHelpText(StrUtil.trim(fillForm.getString("helpText")));
        }
        return rule;
    }

    private static String defaultHelpText(String valueType, String label, String unit) {
        String normalizedLabel = StrUtil.blankToDefault(StrUtil.trim(label), "");
        if ("BOOLEAN".equals(normalizeValueType(valueType))) {
            return "\u6839\u636e\u5b9e\u9645\u60c5\u51b5\u52fe\u9009\u3010" + normalizedLabel + "\u3011";
        }
        if ("NUMBER".equals(normalizeValueType(valueType)) && StrUtil.isNotBlank(unit)) {
            return "\u586b\u5199\u3010" + normalizedLabel + "\u3011\uff0c\u5355\u4f4d\uff1a" + unit;
        }
        return "\u586b\u5199\u3010" + normalizedLabel + "\u3011";
    }

    private static void syncFillFormWithRule(BatchRecordReportCellRuleVO rule, JSONObject fillForm) {
        String valueType = normalizeValueType(rule.getValueType());
        String componentFlag = defaultComponentFlag(valueType, rule.getComponentFlag());
        fillForm.put("componentFlag", componentFlag);
        fillForm.put("required", Boolean.TRUE.equals(rule.getRequired()));
        String label = StrUtil.blankToDefault(StrUtil.trim(rule.getLabel()), "");
        if (StrUtil.isNotBlank(label)) {
            fillForm.put("labelText", label);
        }
        if (rule.getPlaceholder() != null) {
            fillForm.put("placeholder", StrUtil.blankToDefault(StrUtil.trim(rule.getPlaceholder()), ""));
        } else if (!fillForm.containsKey("placeholder")) {
            fillForm.put("placeholder", "");
        }
        if (rule.getHelpText() != null) {
            fillForm.put("helpText", StrUtil.blankToDefault(StrUtil.trim(rule.getHelpText()), ""));
        }
        if ("BOOLEAN".equals(valueType)) {
            if (fillForm.get("value") == null || "".equals(fillForm.get("value"))) {
                fillForm.put("value", Boolean.FALSE);
            }
            if (fillForm.get("defaultValue") == null || "".equals(fillForm.get("defaultValue"))) {
                fillForm.put("defaultValue", Boolean.FALSE);
            }
            return;
        }
        if (isSingleChoiceComponentFlag(componentFlag)) {
            Object options = rule.getConstraints() == null ? null : rule.getConstraints().get("options");
            if (options != null) {
                fillForm.put("options", options);
            }
            if (fillForm.get("value") == null || fillForm.get("value") instanceof Boolean) {
                fillForm.put("value", "");
            }
            if (fillForm.get("defaultValue") == null || fillForm.get("defaultValue") instanceof Boolean) {
                fillForm.put("defaultValue", "");
            }
            return;
        }
        if (fillForm.get("value") == null || fillForm.get("value") instanceof Boolean) {
            fillForm.put("value", "");
        }
        if (fillForm.get("defaultValue") == null || fillForm.get("defaultValue") instanceof Boolean) {
            fillForm.put("defaultValue", "");
        }
    }

    private static Map<String, Object> inferConstraints(String valueType, String label, String unit) {
        return switch (normalizeValueType(valueType)) {
            case "NUMBER" -> inferNumberConstraints(label, unit);
            case "STRING" -> inferStringConstraints(label);
            case "DATE" -> Map.of("format", "yyyy-MM-dd");
            case "DATETIME" -> Map.of("format", "yyyy-MM-dd HH:mm:ss");
            default -> Map.of();
        };
    }

    private static Map<String, Object> inferNumberConstraints(String label, String unit) {
        String compactLabel = compact(label);
        String normalizedUnit = StrUtil.blankToDefault(unit, "").trim().toLowerCase(Locale.ROOT);
        LinkedHashMap<String, Object> constraints = new LinkedHashMap<>();
        constraints.put("min", 0);
        constraints.put("scale", 2);
        constraints.put("precision", 12);
        if (containsAny(compactLabel, "温度") || unitMatches(normalizedUnit, "℃", "°c")) {
            constraints.put("min", -50);
            constraints.put("max", 200);
            constraints.put("scale", 1);
            constraints.put("precision", 6);
            return constraints;
        }
        if (containsAny(compactLabel, "压力") || unitMatches(normalizedUnit, "mpa", "kpa", "pa")) {
            constraints.put("max", 100);
            constraints.put("scale", 3);
            constraints.put("precision", 8);
            return constraints;
        }
        if (containsAny(compactLabel, "ph") || unitMatches(normalizedUnit, "ph")) {
            constraints.put("max", 14);
            constraints.put("scale", 2);
            constraints.put("precision", 4);
            return constraints;
        }
        if (containsAny(compactLabel, "百分比", "比例", "合格率", "不良率", "率")
                || unitMatches(normalizedUnit, "%")) {
            constraints.put("max", 100);
            constraints.put("scale", 2);
            constraints.put("precision", 5);
            return constraints;
        }
        if (containsAny(compactLabel, "数量", "批量", "件数", "个数", "支数", "次数")
                || unitMatches(normalizedUnit, "pcs", "支", "个", "件", "只", "次")) {
            constraints.put("scale", 0);
            constraints.put("precision", 10);
            return constraints;
        }
        if (containsAny(compactLabel, "重量", "质量", "称重") || unitMatches(normalizedUnit, "kg", "g", "mg", "μg")) {
            constraints.put("scale", 3);
            constraints.put("precision", 12);
            return constraints;
        }
        if (isDurationLabel(compactLabel, normalizedUnit)) {
            constraints.put("scale", 2);
            constraints.put("precision", 8);
            return constraints;
        }
        if (containsAny(compactLabel, "长度", "宽度", "高度", "厚度", "直径")
                || unitMatches(normalizedUnit, "mm", "cm", "m")) {
            constraints.put("scale", 2);
            constraints.put("precision", 10);
        }
        return constraints;
    }

    private static Map<String, Object> inferStringConstraints(String label) {
        String compactLabel = compact(label);
        if (StrUtil.isBlank(compactLabel)) {
            return Map.of();
        }
        if (containsAny(compactLabel, "描述", "原因", "说明", "备注", "处置", "处理", "纠正",
                "预防", "异常", "偏差", "措施")) {
            return Map.of("maxLength", 1000);
        }
        if (containsAny(compactLabel, "操作人", "复核人", "记录人", "检验人", "确认人", "审核人",
                "批准人", "人员", "姓名", "签名", "签字")) {
            return Map.of("maxLength", 64);
        }
        if (containsAny(compactLabel, "批号", "编号", "编码", "规格", "型号", "图号")) {
            return Map.of("maxLength", 128);
        }
        return Map.of();
    }

    private static String resolveNeighborLabel(JSONObject rows, Integer rowIndex, Integer columnIndex) {
        String leftLabel = resolveLeftLabel(rows, rowIndex, columnIndex);
        String upperLabel = resolveUpperLabel(rows, rowIndex, columnIndex);
        if (isSignatureDateLabel(compact(leftLabel))) {
            return leftLabel;
        }
        if (isCheckboxChoiceText(leftLabel) && isTypedTableColumnHeader(upperLabel)) {
            return upperLabel;
        }
        if (hasStrongTypeCue(leftLabel)) {
            return leftLabel;
        }
        if (hasStrongTypeCue(upperLabel)) {
            return upperLabel;
        }
        if (StrUtil.isNotBlank(leftLabel)) {
            return leftLabel;
        }
        return upperLabel;
    }

    private static boolean isTypedTableColumnHeader(String label) {
        return StrUtil.isNotBlank(label)
                && !isCheckboxChoiceText(label)
                && hasStrongTypeCue(label);
    }

    private static String resolveLeftLabel(JSONObject rows, Integer rowIndex, Integer columnIndex) {
        JSONObject row = rows == null ? null : rows.getJSONObject(String.valueOf(rowIndex));
        JSONObject cells = row == null ? null : row.getJSONObject("cells");
        if (cells != null) {
            for (int cursor = columnIndex - 1; cursor >= 0; cursor--) {
                JSONObject leftCell = cells.getJSONObject(String.valueOf(cursor));
                String text = cellText(leftCell);
                if (StrUtil.isNotBlank(text)) {
                    return text;
                }
            }
        }
        return "";
    }

    private static String resolveUpperLabel(JSONObject rows, Integer rowIndex, Integer columnIndex) {
        for (int cursor = rowIndex - 1; cursor >= 0; cursor--) {
            JSONObject upperRow = rows == null ? null : rows.getJSONObject(String.valueOf(cursor));
            JSONObject upperCells = upperRow == null ? null : upperRow.getJSONObject("cells");
            JSONObject upperCell = upperCells == null ? null : upperCells.getJSONObject(String.valueOf(columnIndex));
            String text = cellText(upperCell);
            if (StrUtil.isNotBlank(text)) {
                return text;
            }
        }
        return "";
    }

    private static String resolveUpperSignatureDateLabel(JSONObject rows, Integer rowIndex, Integer columnIndex) {
        for (int cursor = rowIndex - 1; cursor >= 0; cursor--) {
            JSONObject upperRow = rows == null ? null : rows.getJSONObject(String.valueOf(cursor));
            JSONObject upperCells = upperRow == null ? null : upperRow.getJSONObject("cells");
            if (upperCells == null) {
                continue;
            }
            for (Integer candidateColumnIndex : numericKeys(upperCells)) {
                JSONObject upperCell = upperCells.getJSONObject(String.valueOf(candidateColumnIndex));
                if (!coversColumn(upperCell, candidateColumnIndex, columnIndex)) {
                    continue;
                }
                String text = cellText(upperCell);
                if (StrUtil.isBlank(text)) {
                    continue;
                }
                if (isSignatureDateLabel(compact(text))) {
                    return text;
                }
            }
            String signatureDateTailLabel = resolveSignatureDateTailLabel(upperCells, columnIndex);
            if (StrUtil.isNotBlank(signatureDateTailLabel)) {
                return signatureDateTailLabel;
            }
        }
        return "";
    }

    private static String resolveSignatureDateTailLabel(JSONObject upperCells, Integer columnIndex) {
        int resultEndColumn = -1;
        int signatureStartColumn = Integer.MAX_VALUE;
        int signatureEndColumn = -1;
        String firstSignatureDateLabel = "";
        for (Integer candidateColumnIndex : numericKeys(upperCells)) {
            JSONObject upperCell = upperCells.getJSONObject(String.valueOf(candidateColumnIndex));
            String text = cellText(upperCell);
            String compactText = compact(text);
            if (StrUtil.isBlank(compactText)) {
                continue;
            }
            int candidateEndColumn = endColumn(upperCell, candidateColumnIndex);
            if (isChecklistResultHeaderText(compactText)) {
                resultEndColumn = Math.max(resultEndColumn, candidateEndColumn);
            }
            if (isSignatureDateLabel(compactText)) {
                if (candidateColumnIndex < signatureStartColumn) {
                    firstSignatureDateLabel = text;
                }
                signatureStartColumn = Math.min(signatureStartColumn, candidateColumnIndex);
                signatureEndColumn = Math.max(signatureEndColumn, candidateEndColumn);
            }
        }
        return resultEndColumn >= 0
                && signatureStartColumn != Integer.MAX_VALUE
                && resultEndColumn < signatureStartColumn
                && columnIndex >= signatureStartColumn
                && columnIndex <= signatureEndColumn + 1
                ? firstSignatureDateLabel
                : "";
    }

    private static boolean coversColumn(JSONObject cell, Integer startColumnIndex, Integer targetColumnIndex) {
        if (cell == null || startColumnIndex == null || targetColumnIndex == null) {
            return false;
        }
        return targetColumnIndex >= startColumnIndex
                && targetColumnIndex <= endColumn(cell, startColumnIndex);
    }

    private static int endColumn(JSONObject cell, Integer startColumnIndex) {
        int colSpan = 1;
        if (cell.getJSONArray("merge") != null && cell.getJSONArray("merge").size() > 1) {
            colSpan = Math.max(1, cell.getJSONArray("merge").getIntValue(1) + 1);
        }
        return startColumnIndex + colSpan - 1;
    }

    private static String cellText(JSONObject cell) {
        if (cell == null) {
            return "";
        }
        return firstNonBlank(cell.getString("text"), cell.getString("value"));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return "";
    }

    private static String resolveUnit(String label) {
        if (StrUtil.isBlank(label)) {
            return null;
        }
        Matcher matcher = UNIT_PATTERN.matcher(label);
        if (matcher.find()) {
            return matcher.group(1);
        }
        String compact = compact(label);
        for (String unit : KNOWN_UNITS) {
            if (compact.contains(unit.toLowerCase(Locale.ROOT))) {
                return unit;
            }
        }
        return null;
    }

    private static void validateConstraints(String valueType, Map<String, Object> constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return;
        }
        if ("NUMBER".equals(valueType)) {
            validateNumberConstraint(constraints, "min");
            validateNumberConstraint(constraints, "max");
            validateIntegerConstraint(constraints, "scale");
            validateIntegerConstraint(constraints, "precision");
            Number min = (Number) constraints.get("min");
            Number max = (Number) constraints.get("max");
            if (min != null && max != null && min.doubleValue() > max.doubleValue()) {
                throw new IllegalArgumentException("NUMBER min must not exceed max");
            }
            return;
        }
        if ("STRING".equals(valueType)) {
            validateIntegerConstraint(constraints, "minLength");
            validateIntegerConstraint(constraints, "maxLength");
            validateStringOptionGroupConstraints(constraints);
            return;
        }
        if ("DATE".equals(valueType) || "DATETIME".equals(valueType)) {
            return;
        }
        if (!constraints.isEmpty()) {
            throw new IllegalArgumentException(valueType + " constraints are not supported");
        }
    }

    private static void validateNumberConstraint(Map<String, Object> constraints, String key) {
        Object value = constraints.get(key);
        if (value != null && !(value instanceof Number)) {
            throw new IllegalArgumentException(key + " must be numeric");
        }
    }

    private static void validateIntegerConstraint(Map<String, Object> constraints, String key) {
        Object value = constraints.get(key);
        if (value == null) {
            return;
        }
        if (!(value instanceof Number) || ((Number) value).intValue() < 0) {
            throw new IllegalArgumentException(key + " must be non-negative integer");
        }
    }

    private static boolean isSingleChoiceComponentFlag(String componentFlag) {
        return SINGLE_CHOICE_COMPONENT_FLAGS.contains(StrUtil.blankToDefault(componentFlag, "").trim()
                .toLowerCase(Locale.ROOT));
    }

    private static String stripChoiceLabelFillBlank(String label) {
        return StrUtil.blankToDefault(label, "")
                .replaceAll("[_＿]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeCheckboxChoiceGroupLabel(String rawLabel, List<String> choiceLabels) {
        String label = StrUtil.blankToDefault(StrUtil.trim(rawLabel), "选项");
        if (!isGenericResultLabel(label) || !isInspectionResultChoicePair(choiceLabels)) {
            return label;
        }
        return "检测结果";
    }

    private static boolean isGenericResultLabel(String label) {
        String normalized = StrUtil.blankToDefault(label, "")
                .replaceAll("\\s+", "")
                .trim();
        return "结果".equals(normalized) || "检查结果".equals(normalized);
    }

    private static boolean isInspectionResultChoicePair(List<String> choiceLabels) {
        Set<String> normalizedLabels = choiceLabels.stream()
                .map(MesProBatchRecordCellRuleSupport::stripChoiceLabelFillBlank)
                .map(label -> label.replaceAll("\\s+", ""))
                .map(label -> label.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return containsChoicePair(normalizedLabels, "符合要求", "不符合要求")
                || containsChoicePair(normalizedLabels, "合格", "不合格")
                || containsChoicePair(normalizedLabels, "通过", "不通过")
                || containsChoicePair(normalizedLabels, "OK", "NG");
    }

    private static boolean containsChoicePair(Set<String> labels, String left, String right) {
        return labels.contains(left.toUpperCase(Locale.ROOT)) && labels.contains(right.toUpperCase(Locale.ROOT));
    }

    private static Map<String, Object> singleChoiceConstraints(List<String> choiceLabels) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (String choiceLabel : choiceLabels) {
            String label = stripChoiceLabelFillBlank(choiceLabel);
            if (StrUtil.isBlank(label) || options.stream().anyMatch(option -> label.equals(option.get("value")))) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("label", label);
            option.put("value", label);
            options.add(option);
        }
        Map<String, Object> constraints = new LinkedHashMap<>();
        constraints.put("selectionMode", "single");
        constraints.put("options", options);
        return constraints;
    }

    private static void validateStringOptionGroupConstraints(Map<String, Object> constraints) {
        Object selectionMode = constraints.get("selectionMode");
        Object rawOptions = constraints.get("options");
        if (selectionMode == null && rawOptions == null) {
            return;
        }
        if (!"single".equals(String.valueOf(selectionMode))) {
            throw new IllegalArgumentException("STRING option group selectionMode must be single");
        }
        if (!(rawOptions instanceof Iterable<?> options)) {
            throw new IllegalArgumentException("STRING option group options must be array");
        }
        int count = 0;
        for (Object option : options) {
            Object label = readOptionValue(option, "label");
            Object value = readOptionValue(option, "value");
            if (label == null || value == null || StrUtil.isBlank(String.valueOf(label))
                    || StrUtil.isBlank(String.valueOf(value))) {
                throw new IllegalArgumentException("STRING option group option label/value must not be blank");
            }
            count++;
        }
        if (count < 2) {
            throw new IllegalArgumentException("STRING option group options must contain at least two items");
        }
    }

    private static Object readOptionValue(Object option, String key) {
        if (option instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return option;
    }

    private static JSONObject mapToJson(Map<String, Object> map) {
        JSONObject json = new JSONObject(true);
        if (map != null) {
            json.putAll(map);
        }
        return json;
    }

    private static List<Integer> numericKeys(JSONObject object) {
        return object.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(Integer::valueOf)
                .sorted()
                .toList();
    }

    private static Comparator<BatchRecordReportCellRuleVO> ruleComparator() {
        return Comparator.comparing(BatchRecordReportCellRuleVO::getRowIndex)
                .thenComparing(BatchRecordReportCellRuleVO::getColumnIndex);
    }

    private static String compact(String text) {
        return StrUtil.blankToDefault(text, "")
                .replace(" ", "")
                .replace("\n", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String text, String... patterns) {
        return java.util.Arrays.stream(patterns)
                .filter(Objects::nonNull)
                .anyMatch(pattern -> text.contains(pattern.toLowerCase(Locale.ROOT)));
    }

    private static boolean unitMatches(String normalizedUnit, String... expectedUnits) {
        if (StrUtil.isBlank(normalizedUnit)) {
            return false;
        }
        return java.util.Arrays.stream(expectedUnits)
                .filter(Objects::nonNull)
                .map(unit -> unit.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedUnit::equals);
    }

    private static boolean hasStrongTypeCue(String label) {
        String compactLabel = compact(label);
        return StrUtil.isNotBlank(compactLabel)
                && (hasBooleanCue(compactLabel) || containsIdentifierCue(compactLabel)
                || containsAny(compactLabel, "日期", "年月日", "时间", "时长", "用时", "时间点", "时刻", "数量", "重量", "温度", "压力", "体积",
                "长度", "宽度", "高度", "厚度", "速度", "电压", "电流", "批量", "含量",
                "浓度", "转速", "扭矩", "操作人", "复核人", "记录人", "检验人", "确认人",
                "审核人", "批准人", "人员", "姓名", "签名", "签字")
                || resolveUnit(label) != null);
    }

    private static boolean containsIdentifierCue(String compactLabel) {
        return containsAny(compactLabel, "编号", "编码", "批号", "型号", "规格", "图号");
    }

    private static boolean isDateTimeLabel(String compactLabel, String unit) {
        String normalizedUnit = StrUtil.blankToDefault(unit, "").trim().toLowerCase(Locale.ROOT);
        if (isDurationLabel(compactLabel, normalizedUnit)) {
            return false;
        }
        return containsAny(compactLabel, "时间点", "时刻", "操作时间", "记录时间", "检验时间", "审核时间",
                "开始时间", "结束时间", "完成时间", "发生时间", "提交时间", "批准时间")
                || containsAny(compactLabel, "时间");
    }

    private static boolean isDurationLabel(String compactLabel, String normalizedUnit) {
        return containsAny(compactLabel, "时长", "用时", "耗时", "持续", "间隔")
                || unitMatches(normalizedUnit, "min", "h", "s");
    }

    private static boolean hasBooleanCue(String compactLabel) {
        if (containsAny(compactLabel, "□", "☑", "☐", "是否")) {
            return true;
        }
        if (containsAny(compactLabel, "数量", "件数", "个数", "支数", "批量")
                || containsIdentifierCue(compactLabel)) {
            return false;
        }
        return containsAny(compactLabel, "确认", "合格", "符合", "判定");
    }

    private static boolean isSignatureDateLabel(String compactLabel) {
        return containsAny(compactLabel, "日期", "/")
                && containsAny(compactLabel, "签名", "签字", "记录人", "操作人", "复核人", "审核人",
                "确认人", "批准人");
    }

    private static boolean isChecklistResultHeaderText(String compactLabel) {
        return StrUtil.isNotBlank(compactLabel)
                && compactLabel.contains("结果")
                && compactLabel.length() <= 10;
    }

    @FunctionalInterface
    public interface CellConsumer {
        void accept(Integer rowIndex, Integer columnIndex, JSONObject cell);
    }

    private static final class Counter {
        private int value;
    }
}
