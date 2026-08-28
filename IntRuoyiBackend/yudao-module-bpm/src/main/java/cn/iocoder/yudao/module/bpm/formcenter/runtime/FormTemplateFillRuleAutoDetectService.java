package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormTemplateFillRuleAutoDetectRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormTemplateFillRuleCandidateVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FormTemplateFillRuleAutoDetectService {

    private static final String DRAFT_STATUS = FormTemplateStatus.DRAFT.name();
    private static final String SIGNATURE_COMPONENT_FLAG = "signature";
    private static final String BOOLEAN_COMPONENT_FLAG = "checkbox";
    private static final String RADIO_GROUP_COMPONENT_FLAG = "radio-group";
    private static final String OPTION_GROUP_COMPONENT_FLAG = "option-group";
    private static final String SELECT_COMPONENT_FLAG = "select";
    private static final String TEXTAREA_COMPONENT_FLAG = "textarea";
    private static final String INPUT_TEXT_COMPONENT_FLAG = "input-text";
    private static final String INPUT_NUMBER_COMPONENT_FLAG = "input-number";
    private static final String DATE_COMPONENT_FLAG = "date";
    private static final String DATETIME_COMPONENT_FLAG = "datetime";
    private static final Pattern AUTO_VERSION_PATTERN = Pattern.compile("^([Vv]?)(\\d+)((?:\\.\\d+)*)$");
    private static final Pattern UNIT_PATTERN = Pattern.compile("[（(]([A-Za-z%℃°μΩ/\\u4e00-\\u9fa5]{1,12})[）)]");
    private static final List<String> KNOWN_UNITS = List.of(
            "℃", "°C", "kg", "g", "mg", "μg", "L", "mL", "ml", "mm", "cm", "m",
            "pcs", "支", "个", "件", "min", "h", "s", "%", "MPa", "kPa", "V", "A");

    @Resource
    private FormTemplateVersionMapper templateVersionMapper;

    @Transactional(rollbackFor = Exception.class)
    public FormTemplateFillRuleAutoDetectRespVO detect(Long templateId, String versionNo) {
        FormTemplateVersionDO sourceVersion = requireCurrentTenantTemplateVersion(templateId, versionNo);
        FormTemplateVersionDO editableVersion = sourceVersion;
        boolean draftCreated = false;
        if (!DRAFT_STATUS.equals(sourceVersion.getStatus())) {
            FormTemplateVersionDO existingDraft = templateVersionMapper.selectDraftByTemplateId(
                    TenantContextHolder.getRequiredTenantId(), templateId);
            if (existingDraft == null) {
                String draftVersionNo = resolveNextDraftVersionNo(sourceVersion);
                editableVersion = cloneAsDraft(sourceVersion, draftVersionNo);
                templateVersionMapper.insert(editableVersion);
                draftCreated = true;
            } else {
                editableVersion = existingDraft;
            }
        }

        List<FormTemplateFillRuleCandidateVO> candidates = buildCandidates(editableVersion);
        FormTemplateFillRuleAutoDetectRespVO respVO = new FormTemplateFillRuleAutoDetectRespVO();
        respVO.setTemplateId(sourceVersion.getTemplateId());
        respVO.setTemplateName(sourceVersion.getTemplateName());
        respVO.setSourceVersionNo(sourceVersion.getVersionNo());
        respVO.setVersionNo(editableVersion.getVersionNo());
        respVO.setTargetStatus(DRAFT_STATUS);
        respVO.setDraftCreated(draftCreated);
        respVO.setCandidateCount(candidates.size());
        respVO.setCandidates(candidates);
        return respVO;
    }

    private FormTemplateVersionDO requireCurrentTenantTemplateVersion(Long templateId, String versionNo) {
        FormTemplateVersionDO version = templateVersionMapper.selectByTemplateIdAndVersionNo(templateId, versionNo);
        if (version == null || !Objects.equals(version.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template version not found: " + templateId + "/" + versionNo);
        }
        return version;
    }

    private FormTemplateVersionDO cloneAsDraft(FormTemplateVersionDO sourceVersion, String versionNo) {
        return FormTemplateVersionDO.builder()
                .templateId(sourceVersion.getTemplateId())
                .tenantId(sourceVersion.getTenantId())
                .templateName(sourceVersion.getTemplateName())
                .versionNo(versionNo)
                .status(DRAFT_STATUS)
                .sourceFileName(sourceVersion.getSourceFileName())
                .sourceFileContent(sourceVersion.getSourceFileContent())
                .recognizedSchemaJson(sourceVersion.getRecognizedSchemaJson())
                .jimuSchemaJson(sourceVersion.getJimuSchemaJson())
                .remark(sourceVersion.getRemark())
                .build();
    }

    private String resolveNextDraftVersionNo(FormTemplateVersionDO sourceVersion) {
        String nextVersionNo = nextVersionNo(sourceVersion.getVersionNo());
        while (templateVersionMapper.selectByTemplateIdAndVersionNo(sourceVersion.getTemplateId(), nextVersionNo) != null) {
            nextVersionNo = nextVersionNo(nextVersionNo);
        }
        return nextVersionNo;
    }

    private String nextVersionNo(String latestVersionNo) {
        if (latestVersionNo == null || latestVersionNo.trim().isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Latest template version number is blank");
        }
        Matcher matcher = AUTO_VERSION_PATTERN.matcher(latestVersionNo.trim());
        if (!matcher.matches()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template version number cannot be auto-incremented: " + latestVersionNo);
        }
        long current;
        try {
            current = Long.parseLong(matcher.group(2));
        } catch (NumberFormatException ex) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template version number cannot be auto-incremented: " + latestVersionNo);
        }
        return matcher.group(1) + (current + 1) + matcher.group(3);
    }

    private List<FormTemplateFillRuleCandidateVO> buildCandidates(FormTemplateVersionDO version) {
        Map<String, Object> root = parseSchemaRoot(version);
        Map<String, Object> layout = resolveLayoutRoot(root);
        Map<String, Object> rows = asMap(layout.get("rows"));
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<FormTemplateFillRuleCandidateVO> candidates = buildCandidatesFromExistingRules(root);
        Set<String> existingCellKeys = new HashSet<>();
        for (FormTemplateFillRuleCandidateVO candidate : candidates) {
            existingCellKeys.add(cellKey(candidate.getRowIndex(), candidate.getColumnIndex()));
        }
        for (Integer rowIndex : numericKeys(rows)) {
            Map<String, Object> row = asMap(rows.get(String.valueOf(rowIndex)));
            Map<String, Object> cells = row == null ? null : asMap(row.get("cells"));
            if (cells == null || cells.isEmpty()) {
                continue;
            }
            for (Integer columnIndex : numericKeys(cells)) {
                if (existingCellKeys.contains(cellKey(rowIndex, columnIndex))) {
                    continue;
                }
                Map<String, Object> cell = asMap(cells.get(String.valueOf(columnIndex)));
                FormTemplateFillRuleCandidateVO candidate = buildCandidate(layout, rowIndex, columnIndex, cell);
                if (candidate != null) {
                    candidates.add(candidate);
                    existingCellKeys.add(cellKey(rowIndex, columnIndex));
                }
            }
        }
        candidates.sort(Comparator.comparing(FormTemplateFillRuleCandidateVO::getRowIndex)
                .thenComparing(FormTemplateFillRuleCandidateVO::getColumnIndex));
        return candidates;
    }

    private List<FormTemplateFillRuleCandidateVO> buildCandidatesFromExistingRules(Map<String, Object> root) {
        Object rawRules = root.get("cellRules");
        if (!(rawRules instanceof List<?> rules) || rules.isEmpty()) {
            return new ArrayList<>();
        }
        List<FormTemplateFillRuleCandidateVO> candidates = new ArrayList<>();
        for (Object rawRule : rules) {
            Map<String, Object> rule = asMap(rawRule);
            if (rule == null) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                        "Template cell rule must be an object");
            }
            Integer rowIndex = asInteger(rule.get("rowIndex"));
            Integer columnIndex = asInteger(rule.get("columnIndex"));
            String valueType = normalizeValueType(getString(rule, "valueType"));
            String componentFlag = normalizeComponentFlag(getString(rule, "componentFlag"));
            if (rowIndex == null || columnIndex == null || StrUtil.isBlank(valueType)
                    || StrUtil.isBlank(componentFlag)) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                        "Template cell rule is invalid");
            }
            FormTemplateFillRuleCandidateVO candidate = new FormTemplateFillRuleCandidateVO();
            candidate.setRowIndex(rowIndex);
            candidate.setColumnIndex(columnIndex);
            candidate.setLabel(firstNonBlank(getString(rule, "label"),
                    getString(rule, "labelText"), defaultLabel(rowIndex, columnIndex)));
            candidate.setValueType(valueType);
            candidate.setComponentFlag(componentFlag);
            candidate.setRequired(asBoolean(rule.get("required"), false));
            candidate.setConstraints(asMap(rule.get("constraints")));
            candidate.setUnit(blankToNull(getString(rule, "unit")));
            candidate.setPlaceholder(blankToNull(firstNonBlank(getString(rule, "placeholder"),
                    resolvePlaceholder(valueType, componentFlag, candidate.getLabel(), ""))));
            candidate.setHelpText(blankToNull(firstNonBlank(getString(rule, "helpText"),
                    resolveHelpText(valueType, candidate.getLabel(), candidate.getUnit(), "",
                            List.of(), ""))));
            candidate.setConfidence(asDouble(rule.get("confidence"), 1d));
            candidate.setReason(firstNonBlank(getString(rule, "reason"), "使用导入时已识别的填写规则"));
            candidates.add(candidate);
        }
        return candidates;
    }

    private FormTemplateFillRuleCandidateVO buildCandidate(Map<String, Object> layout, Integer rowIndex,
            Integer columnIndex, Map<String, Object> cell) {
        if (cell == null) {
            return null;
        }
        Map<String, Object> fillForm = asMap(cell.get("fillForm"));
        Map<String, Object> signature = asMap(cell.get("edhrSignature"));
        String cellText = cellText(cell);
        if (!isFillableCandidate(fillForm, signature, cellText)) {
            return null;
        }

        String leftLabel = resolveLeftLabel(layout, rowIndex, columnIndex);
        String upperLabel = resolveUpperLabel(layout, rowIndex, columnIndex);
        List<String> checkboxChoiceLabels = splitUncheckedCheckboxChoiceLabels(cellText);
        String label = resolveLabel(fillForm, signature, cellText, leftLabel, upperLabel, checkboxChoiceLabels);
        if (StrUtil.isBlank(label)) {
            label = defaultLabel(rowIndex, columnIndex);
        }
        String unit = resolveUnit(label);
        String existingComponentFlag = normalizeComponentFlag(firstNonBlank(
                getString(fillForm, "componentFlag"),
                getString(fillForm, "component"),
                getString(fillForm, "type")));
        String valueType = resolveValueType(existingComponentFlag, signature, cellText, label, unit,
                !checkboxChoiceLabels.isEmpty() && checkboxChoiceLabels.size() > 1);
        String componentFlag = resolveComponentFlag(valueType, existingComponentFlag, checkboxChoiceLabels);
        if (BOOLEAN_COMPONENT_FLAG.equals(componentFlag) && !containsBooleanCue(compact(label)) && !isStaticCheckboxChoiceText(cellText)) {
            valueType = "STRING".equals(valueType) ? "BOOLEAN" : valueType;
        }

        Map<String, Object> constraints = buildConstraints(valueType, componentFlag, label, unit, checkboxChoiceLabels);
        String placeholder = resolvePlaceholder(valueType, componentFlag, label, getString(fillForm, "placeholder"));
        String helpText = resolveHelpText(valueType, label, unit, cellText, checkboxChoiceLabels, getString(fillForm, "helpText"));
        Double confidence = resolveConfidence(valueType, componentFlag, checkboxChoiceLabels, fillForm);
        String reason = resolveReason(valueType, label, unit, cellText, checkboxChoiceLabels);
        Boolean required = resolveRequired(fillForm, label);

        FormTemplateFillRuleCandidateVO candidate = new FormTemplateFillRuleCandidateVO();
        candidate.setRowIndex(rowIndex);
        candidate.setColumnIndex(columnIndex);
        candidate.setLabel(label);
        candidate.setValueType(valueType);
        candidate.setComponentFlag(componentFlag);
        candidate.setRequired(required);
        candidate.setConstraints(constraints.isEmpty() ? null : constraints);
        candidate.setUnit(blankToNull(unit));
        candidate.setPlaceholder(blankToNull(placeholder));
        candidate.setHelpText(blankToNull(helpText));
        candidate.setConfidence(confidence);
        candidate.setReason(reason);
        return candidate;
    }

    private boolean isFillableCandidate(Map<String, Object> fillForm, Map<String, Object> signature, String cellText) {
        if (fillForm != null && StrUtil.isNotBlank(getString(fillForm, "field"))) {
            return true;
        }
        if (signature != null && Boolean.TRUE.equals(signature.get("enabled"))) {
            return true;
        }
        return isStaticCheckboxChoiceText(cellText);
    }

    private String resolveLabel(Map<String, Object> fillForm, Map<String, Object> signature, String cellText,
            String leftLabel, String upperLabel, List<String> checkboxChoiceLabels) {
        String fillLabel = firstNonBlank(getString(fillForm, "labelText"), getString(fillForm, "label"));
        if (StrUtil.isBlank(fillLabel) && signature != null) {
            fillLabel = getString(signature, "label");
        }
        if (!checkboxChoiceLabels.isEmpty()) {
            String rawLabel = firstNonBlank(fillLabel, leftLabel, upperLabel, cellText);
            return normalizeCheckboxChoiceGroupLabel(rawLabel, checkboxChoiceLabels);
        }
        if (isStaticCheckboxChoiceText(cellText)) {
            return firstNonBlank(fillLabel, normalizeCheckboxChoiceLabel(cellText), leftLabel, upperLabel);
        }
        String label = firstNonBlank(fillLabel, cellText, leftLabel, upperLabel);
        return label;
    }

    private String resolveValueType(String existingComponentFlag, Map<String, Object> signature, String cellText,
            String label, String unit, boolean hasMultipleCheckboxOptions) {
        if (isSignatureCandidate(existingComponentFlag, signature)) {
            return "SIGNATURE";
        }
        String compactLabel = compact(label);
        if (hasMultipleCheckboxOptions) {
            return "STRING";
        }
        if (isStaticCheckboxChoiceText(cellText) || containsBooleanCue(compactLabel)) {
            return "BOOLEAN";
        }
        if (isDateTimeLabel(compactLabel)) {
            return DATETIME_COMPONENT_FLAG.equals(existingComponentFlag) ? "DATETIME" : "DATETIME";
        }
        if (isDateLabel(compactLabel)) {
            return "DATE";
        }
        if (isDurationLabel(compactLabel, unit) || isNumberLabel(compactLabel, unit)) {
            return "NUMBER";
        }
        if (isNumberComponentFlag(existingComponentFlag)) {
            return "NUMBER";
        }
        if (DATE_COMPONENT_FLAG.equals(existingComponentFlag)) {
            return "DATE";
        }
        if (DATETIME_COMPONENT_FLAG.equals(existingComponentFlag)) {
            return "DATETIME";
        }
        if (BOOLEAN_COMPONENT_FLAG.equals(existingComponentFlag)) {
            return "BOOLEAN";
        }
        if (isRadioComponentFlag(existingComponentFlag)) {
            return "STRING";
        }
        if (TEXTAREA_COMPONENT_FLAG.equals(existingComponentFlag)) {
            return "STRING";
        }
        if (isUploadComponentFlag(existingComponentFlag)) {
            return "STRING";
        }
        return "STRING";
    }

    private String resolveComponentFlag(String valueType, String existingComponentFlag,
            List<String> checkboxChoiceLabels) {
        String normalizedValueType = StrUtil.blankToDefault(valueType, "").trim().toUpperCase(Locale.ROOT);
        if (!checkboxChoiceLabels.isEmpty() && checkboxChoiceLabels.size() > 1) {
            return isRadioComponentFlag(existingComponentFlag) ? existingComponentFlag : RADIO_GROUP_COMPONENT_FLAG;
        }
        if ("SIGNATURE".equals(normalizedValueType)) {
            return SIGNATURE_COMPONENT_FLAG;
        }
        if ("DATE".equals(normalizedValueType)) {
            return DATE_COMPONENT_FLAG;
        }
        if ("DATETIME".equals(normalizedValueType)) {
            return DATETIME_COMPONENT_FLAG;
        }
        if ("NUMBER".equals(normalizedValueType)) {
            return isNumberComponentFlag(existingComponentFlag) ? existingComponentFlag : INPUT_NUMBER_COMPONENT_FLAG;
        }
        if ("BOOLEAN".equals(normalizedValueType)) {
            return BOOLEAN_COMPONENT_FLAG;
        }
        if (isRadioComponentFlag(existingComponentFlag)) {
            return existingComponentFlag;
        }
        if (TEXTAREA_COMPONENT_FLAG.equals(existingComponentFlag)) {
            return TEXTAREA_COMPONENT_FLAG;
        }
        if (isUploadComponentFlag(existingComponentFlag)) {
            return existingComponentFlag;
        }
        return inferStringComponentFlag(existingComponentFlag, valueType);
    }

    private String inferStringComponentFlag(String existingComponentFlag, String valueType) {
        if (TEXTAREA_COMPONENT_FLAG.equals(existingComponentFlag)) {
            return TEXTAREA_COMPONENT_FLAG;
        }
        if (SELECT_COMPONENT_FLAG.equals(existingComponentFlag) || OPTION_GROUP_COMPONENT_FLAG.equals(existingComponentFlag)) {
            return existingComponentFlag;
        }
        return INPUT_TEXT_COMPONENT_FLAG;
    }

    private Map<String, Object> buildConstraints(String valueType, String componentFlag, String label, String unit,
            List<String> checkboxChoiceLabels) {
        if (checkboxChoiceLabels.size() > 1) {
            return singleChoiceConstraints(checkboxChoiceLabels, label);
        }
        if ("NUMBER".equals(valueType)) {
            return inferNumberConstraints(label, unit);
        }
        if ("DATE".equals(valueType)) {
            return Map.of("format", "yyyy-MM-dd");
        }
        if ("DATETIME".equals(valueType)) {
            return Map.of("format", "yyyy-MM-dd HH:mm:ss");
        }
        if (TEXTAREA_COMPONENT_FLAG.equals(componentFlag) || containsAny(compact(label), "描述", "原因", "说明", "备注", "处置", "处理", "纠正", "预防", "异常", "偏差", "措施")) {
            return Map.of("maxLength", 1000);
        }
        Map<String, Object> constraints = inferStringConstraints(label);
        return constraints.isEmpty() ? Map.of() : constraints;
    }

    private Map<String, Object> inferNumberConstraints(String label, String unit) {
        String compactLabel = compact(label);
        String normalizedUnit = normalizeUnit(unit);
        Map<String, Object> constraints = new LinkedHashMap<>();
        if (containsAny(compactLabel, "温度") || unitMatches(normalizedUnit, "℃", "°C")) {
            constraints.put("min", -50);
            constraints.put("max", 200);
            constraints.put("scale", 1);
            constraints.put("precision", 6);
            return constraints;
        }
        if (containsAny(compactLabel, "压力") || unitMatches(normalizedUnit, "MPa", "kPa")) {
            constraints.put("min", 0);
            constraints.put("max", 100);
            constraints.put("scale", 3);
            constraints.put("precision", 12);
            return constraints;
        }
        if (containsAny(compactLabel, "pH")) {
            constraints.put("min", 0);
            constraints.put("max", 14);
            constraints.put("scale", 2);
            constraints.put("precision", 8);
            return constraints;
        }
        if (containsAny(compactLabel, "数量", "件数", "个数", "支数", "批量") || unitMatches(normalizedUnit, "pcs", "支", "个", "件")) {
            constraints.put("min", 0);
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
        if (containsAny(compactLabel, "长度", "宽度", "高度", "厚度", "直径") || unitMatches(normalizedUnit, "mm", "cm", "m")) {
            constraints.put("scale", 2);
            constraints.put("precision", 10);
            return constraints;
        }
        if (unitMatches(normalizedUnit, "%")) {
            constraints.put("min", 0);
            constraints.put("max", 100);
            constraints.put("scale", 2);
            constraints.put("precision", 8);
            return constraints;
        }
        return constraints;
    }

    private Map<String, Object> inferStringConstraints(String label) {
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

    private Map<String, Object> singleChoiceConstraints(List<String> choiceLabels, String label) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (String choiceLabel : choiceLabels) {
            String normalized = stripChoiceLabelFillBlank(choiceLabel);
            if (StrUtil.isBlank(normalized) || options.stream().anyMatch(option -> normalized.equals(option.get("value")))) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("label", normalized);
            option.put("value", normalized);
            options.add(option);
        }
        Map<String, Object> constraints = new LinkedHashMap<>();
        constraints.put("selectionMode", "single");
        constraints.put("options", options);
        if (options.size() > 1 && isGenericResultLabel(label) && isInspectionResultChoicePair(choiceLabels)) {
            constraints.put("groupLabel", "检测结果");
        }
        return constraints;
    }

    private String resolvePlaceholder(String valueType, String componentFlag, String label, String existingPlaceholder) {
        if (StrUtil.isNotBlank(existingPlaceholder)) {
            return existingPlaceholder.trim();
        }
        if (SIGNATURE_COMPONENT_FLAG.equals(componentFlag)) {
            return "请签名";
        }
        if (BOOLEAN_COMPONENT_FLAG.equals(componentFlag)) {
            return "";
        }
        if (DATE_COMPONENT_FLAG.equals(componentFlag) || DATETIME_COMPONENT_FLAG.equals(componentFlag)
                || SELECT_COMPONENT_FLAG.equals(componentFlag) || OPTION_GROUP_COMPONENT_FLAG.equals(componentFlag)
                || RADIO_GROUP_COMPONENT_FLAG.equals(componentFlag)) {
            return "请选择" + label;
        }
        if ("NUMBER".equals(valueType) || INPUT_TEXT_COMPONENT_FLAG.equals(componentFlag)
                || TEXTAREA_COMPONENT_FLAG.equals(componentFlag) || isUploadComponentFlag(componentFlag)) {
            return "请输入" + label;
        }
        return "";
    }

    private String resolveHelpText(String valueType, String label, String unit, String cellText,
            List<String> checkboxChoiceLabels, String existingHelpText) {
        if (StrUtil.isNotBlank(existingHelpText)) {
            return existingHelpText.trim();
        }
        if (checkboxChoiceLabels.size() > 1) {
            return "根据“" + firstNonBlank(cellText, label) + "”识别为单选组选项字段";
        }
        return switch (valueType) {
            case "NUMBER" -> "根据“" + firstNonBlank(label, cellText) + "”识别为数字字段"
                    + (StrUtil.isNotBlank(unit) ? "，单位 " + unit : "");
            case "DATE" -> "根据“" + firstNonBlank(label, cellText) + "”识别为日期字段";
            case "DATETIME" -> "根据“" + firstNonBlank(label, cellText) + "”识别为日期时间字段";
            case "BOOLEAN" -> "根据“" + firstNonBlank(label, cellText) + "”识别为勾选字段";
            case "SIGNATURE" -> "根据“" + firstNonBlank(label, cellText) + "”识别为签名字段";
            default -> "根据“" + firstNonBlank(label, cellText) + "”识别为文本字段";
        };
    }

    private Double resolveConfidence(String valueType, String componentFlag, List<String> checkboxChoiceLabels,
            Map<String, Object> fillForm) {
        if (checkboxChoiceLabels.size() > 1) {
            return 0.92d;
        }
        if ("SIGNATURE".equals(valueType) || SIGNATURE_COMPONENT_FLAG.equals(componentFlag)) {
            return 0.98d;
        }
        if ("NUMBER".equals(valueType) || DATE_COMPONENT_FLAG.equals(valueType) || DATETIME_COMPONENT_FLAG.equals(valueType)
                || BOOLEAN_COMPONENT_FLAG.equals(componentFlag)) {
            return 0.9d;
        }
        if (TEXTAREA_COMPONENT_FLAG.equals(componentFlag) || isUploadComponentFlag(componentFlag)) {
            return 0.85d;
        }
        if (fillForm != null && StrUtil.isNotBlank(getString(fillForm, "componentFlag"))
                && !INPUT_TEXT_COMPONENT_FLAG.equals(normalizeComponentFlag(getString(fillForm, "componentFlag")))) {
            return 0.9d;
        }
        return 0.82d;
    }

    private String resolveReason(String valueType, String label, String unit, String cellText,
            List<String> checkboxChoiceLabels) {
        String evidence = firstNonBlank(cellText, label);
        if (checkboxChoiceLabels.size() > 1) {
            return "根据“" + evidence + "”识别为单选组选项字段";
        }
        return switch (valueType) {
            case "NUMBER" -> "根据“" + evidence + "”识别为数字字段" + (StrUtil.isNotBlank(unit) ? "，单位 " + unit : "");
            case "DATE" -> "根据“" + evidence + "”识别为日期字段";
            case "DATETIME" -> "根据“" + evidence + "”识别为日期时间字段";
            case "BOOLEAN" -> "根据“" + evidence + "”识别为勾选字段";
            case "SIGNATURE" -> "根据“" + evidence + "”识别为签名字段";
            default -> "根据“" + evidence + "”识别为文本字段";
        };
    }

    private Boolean resolveRequired(Map<String, Object> fillForm, String label) {
        Object required = fillForm == null ? null : fillForm.get("required");
        if (required instanceof Boolean boolRequired) {
            return boolRequired;
        }
        return containsAny(compact(label), "必填");
    }

    private Map<String, Object> parseSchemaRoot(FormTemplateVersionDO version) {
        if (version.getJimuSchemaJson() == null || version.getJimuSchemaJson().isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template schema is missing: " + version.getTemplateId() + "/" + version.getVersionNo());
        }
        try {
            Map<String, Object> root = JsonUtils.parseObject(version.getJimuSchemaJson(),
                    new TypeReference<Map<String, Object>>() {});
            if (root == null || root.isEmpty()) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                        "Template schema is missing: " + version.getTemplateId() + "/" + version.getVersionNo());
            }
            return root;
        } catch (RuntimeException ex) {
            if (ex instanceof FormCenterException) {
                throw ex;
            }
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template schema parse failed: " + version.getTemplateId() + "/" + version.getVersionNo());
        }
    }

    private Map<String, Object> resolveLayoutRoot(Map<String, Object> root) {
        Map<String, Object> rows = asMap(root.get("rows"));
        if (rows != null) {
            return root;
        }
        Map<String, Object> layout = asMap(root.get("layout"));
        if (layout != null && asMap(layout.get("rows")) != null) {
            return layout;
        }
        layout = parseJsonMap(root.get("sheetLayoutJson"), "Template sheet layout parse failed");
        if (layout != null && asMap(layout.get("rows")) != null) {
            return layout;
        }
        layout = parseJsonMap(root.get("layout"), "Template layout parse failed");
        if (layout != null && asMap(layout.get("rows")) != null) {
            return layout;
        }
        throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                "Template schema rows are missing");
    }

    private Map<String, Object> parseJsonMap(Object value, String failureMessage) {
        if (!(value instanceof String json) || json.isBlank()) {
            return null;
        }
        try {
            return JsonUtils.parseObject(json, new TypeReference<Map<String, Object>>() {});
        } catch (RuntimeException ex) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID, failureMessage);
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) map;
            return casted;
        }
        return null;
    }

    private String resolveLeftLabel(Map<String, Object> layout, Integer rowIndex, Integer columnIndex) {
        Map<String, Object> rows = asMap(layout.get("rows"));
        Map<String, Object> row = rows == null ? null : asMap(rows.get(String.valueOf(rowIndex)));
        Map<String, Object> cells = row == null ? null : asMap(row.get("cells"));
        if (cells == null) {
            return "";
        }
        for (int cursor = columnIndex - 1; cursor >= 0; cursor--) {
            Map<String, Object> candidateCell = asMap(cells.get(String.valueOf(cursor)));
            String text = cellText(candidateCell);
            if (StrUtil.isNotBlank(text)) {
                return text;
            }
        }
        return "";
    }

    private String resolveUpperLabel(Map<String, Object> layout, Integer rowIndex, Integer columnIndex) {
        Map<String, Object> rows = asMap(layout.get("rows"));
        for (int cursor = rowIndex - 1; cursor >= 0; cursor--) {
            Map<String, Object> upperRow = rows == null ? null : asMap(rows.get(String.valueOf(cursor)));
            Map<String, Object> upperCells = upperRow == null ? null : asMap(upperRow.get("cells"));
            Map<String, Object> upperCell = upperCells == null ? null : asMap(upperCells.get(String.valueOf(columnIndex)));
            String text = cellText(upperCell);
            if (StrUtil.isNotBlank(text)) {
                return text;
            }
        }
        return "";
    }

    private List<Integer> numericKeys(Map<String, Object> object) {
        return object.keySet().stream()
                .filter(key -> key != null && key.toString().matches("\\d+"))
                .map(key -> Integer.valueOf(key.toString()))
                .sorted()
                .toList();
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && stringValue.matches("\\d+")) {
            return Integer.valueOf(stringValue);
        }
        return null;
    }

    private Boolean asBoolean(Object value, boolean defaultValue) {
        return value instanceof Boolean boolValue ? boolValue : defaultValue;
    }

    private Double asDouble(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue && StrUtil.isNotBlank(stringValue)) {
            try {
                return Double.valueOf(stringValue);
            } catch (NumberFormatException ex) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                        "Template cell rule confidence is invalid");
            }
        }
        return defaultValue;
    }

    private String normalizeValueType(String valueType) {
        return StrUtil.blankToDefault(valueType, "").trim().toUpperCase(Locale.ROOT);
    }

    private String cellKey(Integer rowIndex, Integer columnIndex) {
        return rowIndex + ":" + columnIndex;
    }

    private String cellText(Map<String, Object> cell) {
        if (cell == null) {
            return "";
        }
        Map<String, Object> fillForm = asMap(cell.get("fillForm"));
        return firstNonBlank(getString(cell, "text"), getString(cell, "value"), getString(fillForm, "labelText"),
                getString(fillForm, "label"), getString(fillForm, "helpText"));
    }

    private boolean isStaticCheckboxChoiceText(String text) {
        String normalized = StrUtil.blankToDefault(text, "").trim();
        if (normalized.isBlank() || !containsAny(normalized, "□", "☐")) {
            return false;
        }
        if (containsAny(normalized, "☑", "☒")) {
            return false;
        }
        return !normalizeCheckboxChoiceLabel(normalized).isBlank();
    }

    private List<String> splitUncheckedCheckboxChoiceLabels(String text) {
        String normalized = StrUtil.blankToDefault(text, "")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank() || !containsAny(normalized, "□", "☐") || containsAny(normalized, "☑", "☒")) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        String[] parts = normalized.split("(?=[□☐])");
        for (String part : parts) {
            String trimmed = stripChoiceLabelFillBlank(part);
            if (StrUtil.isNotBlank(trimmed)) {
                labels.add(trimmed);
            }
        }
        return labels.size() > 1 ? labels : List.of();
    }

    private String normalizeCheckboxChoiceLabel(String text) {
        String label = StrUtil.blankToDefault(text, "")
                .replace("□", " ")
                .replace("☐", " ")
                .replace("☑", " ")
                .replace("☒", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return StrUtil.blankToDefault(label, "勾选");
    }

    private String normalizeCheckboxChoiceGroupLabel(String rawLabel, List<String> choiceLabels) {
        String label = StrUtil.blankToDefault(StrUtil.trim(rawLabel), "选项");
        if (containsAny(label, "□", "☐", "☑", "☒")) {
            label = "选项";
        }
        if (!isGenericResultLabel(label) || !isInspectionResultChoicePair(choiceLabels)) {
            return label;
        }
        return "检测结果";
    }

    private boolean isGenericResultLabel(String label) {
        String normalized = StrUtil.blankToDefault(label, "").replaceAll("\\s+", "").trim();
        return "结果".equals(normalized) || "检查结果".equals(normalized);
    }

    private boolean isInspectionResultChoicePair(List<String> choiceLabels) {
        List<String> normalizedLabels = choiceLabels.stream()
                .map(this::stripChoiceLabelFillBlank)
                .map(label -> label.replaceAll("\\s+", ""))
                .map(label -> label.toUpperCase(Locale.ROOT))
                .toList();
        return containsChoicePair(normalizedLabels, "符合要求", "不符合要求")
                || containsChoicePair(normalizedLabels, "合格", "不合格")
                || containsChoicePair(normalizedLabels, "通过", "不通过")
                || containsChoicePair(normalizedLabels, "OK", "NG");
    }

    private boolean containsChoicePair(List<String> labels, String left, String right) {
        return labels.contains(left.toUpperCase(Locale.ROOT)) && labels.contains(right.toUpperCase(Locale.ROOT));
    }

    private String stripChoiceLabelFillBlank(String label) {
        return StrUtil.blankToDefault(label, "")
                .replaceAll("[_＿]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isSignatureCandidate(String existingComponentFlag, Map<String, Object> signature) {
        if (SIGNATURE_COMPONENT_FLAG.equals(existingComponentFlag)) {
            return true;
        }
        return signature != null && Boolean.TRUE.equals(signature.get("enabled"));
    }

    private boolean isDateLabel(String compactLabel) {
        return containsAny(compactLabel, "日期") && !isDateTimeLabel(compactLabel);
    }

    private boolean isDateTimeLabel(String compactLabel) {
        return containsAny(compactLabel, "时间点", "时刻", "操作时间", "记录时间", "检验时间", "审核时间",
                "开始时间", "结束时间", "完成时间", "发生时间", "提交时间", "批准时间")
                || (containsAny(compactLabel, "时间") && !isDurationLabel(compactLabel, null));
    }

    private boolean isDurationLabel(String compactLabel, String unit) {
        return containsAny(compactLabel, "时长", "用时", "耗时", "持续", "间隔")
                || unitMatches(normalizeUnit(unit), "min", "h", "s");
    }

    private boolean isNumberLabel(String compactLabel, String unit) {
        if (containsAny(compactLabel, "编号", "编码", "批号", "型号", "规格", "图号")) {
            return false;
        }
        return containsAny(compactLabel, "数量", "重量", "质量", "温度", "压力", "体积",
                "长度", "宽度", "高度", "厚度", "速度", "电压", "电流", "批量",
                "含量", "浓度", "转速", "扭矩", "时间", "分贝", "百分比")
                || StrUtil.isNotBlank(normalizeUnit(unit));
    }

    private boolean containsBooleanCue(String compactLabel) {
        if (containsAny(compactLabel, "□", "☑", "☐", "是否")) {
            return true;
        }
        if (containsAny(compactLabel, "数量", "件数", "个数", "支数", "批量")
                || containsAny(compactLabel, "编号", "编码", "批号", "型号", "规格", "图号")) {
            return false;
        }
        return containsAny(compactLabel, "确认", "合格", "符合", "判定");
    }

    private String resolveUnit(String label) {
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

    private String normalizeUnit(String unit) {
        return StrUtil.blankToDefault(unit, "").trim();
    }

    private boolean unitMatches(String normalizedUnit, String... expectedUnits) {
        if (StrUtil.isBlank(normalizedUnit)) {
            return false;
        }
        for (String expectedUnit : expectedUnits) {
            if (expectedUnit != null && normalizedUnit.equalsIgnoreCase(expectedUnit)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeComponentFlag(String componentFlag) {
        return StrUtil.blankToDefault(componentFlag, "").trim().toLowerCase(Locale.ROOT);
    }

    private boolean isNumberComponentFlag(String componentFlag) {
        return INPUT_NUMBER_COMPONENT_FLAG.equals(componentFlag);
    }

    private boolean isRadioComponentFlag(String componentFlag) {
        return RADIO_GROUP_COMPONENT_FLAG.equals(componentFlag)
                || OPTION_GROUP_COMPONENT_FLAG.equals(componentFlag)
                || SELECT_COMPONENT_FLAG.equals(componentFlag);
    }

    private boolean isUploadComponentFlag(String componentFlag) {
        return componentFlag != null && componentFlag.startsWith("upload-");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return "";
    }

    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return "";
        }
        Object value = map.get(key);
        return value == null ? "" : StrUtil.trim(String.valueOf(value));
    }

    private String compact(String text) {
        return StrUtil.blankToDefault(text, "")
                .replace(" ", "")
                .replace("\n", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... patterns) {
        if (text == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String pattern : patterns) {
            if (pattern != null && normalized.contains(pattern.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String defaultLabel(Integer rowIndex, Integer columnIndex) {
        return "第 " + (rowIndex + 1) + " 行第 " + (columnIndex + 1) + " 列";
    }

}
