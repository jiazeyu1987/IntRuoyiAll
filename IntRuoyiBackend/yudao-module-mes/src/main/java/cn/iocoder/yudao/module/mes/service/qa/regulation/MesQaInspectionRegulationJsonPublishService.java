package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationJsonPublishReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationJsonPublishRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;

@Service
public class MesQaInspectionRegulationJsonPublishService {

    private static final String OWNER_MODULE = MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA;
    private final ObjectMapper objectMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper versionMapper;
    private final MesQaInspectionRegulationService regulationService;

    public MesQaInspectionRegulationJsonPublishService(
            ObjectMapper objectMapper,
            MesQaInspectionRegulationMapper regulationMapper,
            MesQaInspectionRegulationVersionMapper versionMapper,
            MesQaInspectionRegulationService regulationService) {
        this.objectMapper = objectMapper;
        this.regulationMapper = regulationMapper;
        this.versionMapper = versionMapper;
        this.regulationService = regulationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public MesQaInspectionRegulationJsonPublishRespVO publishFormParserJson(
            MesQaInspectionRegulationJsonPublishReqVO reqVO) {
        Map<String, Object> json = reqVO.getRecognitionJson();
        ParsedJson parsed = readParsedJson(json);
        MesQaInspectionRegulationDO existing = regulationMapper.selectByDccProjectCodeId(
                reqVO.getDccProjectCodeId(), OWNER_MODULE);
        String versionNo = resolveVersionNo(parsed.versionNo(), existing);
        MesQaInspectionRegulationSaveReqVO saveReq =
                toSaveRequest(reqVO.getDccProjectCodeId(), parsed, existing, versionNo);
        if (existing != null) {
            mergeExistingQaOnlyFields(saveReq,
                    regulationService.getCurrent(reqVO.getDccProjectCodeId()));
        }
        MesQaInspectionRegulationPublishedVersionRespVO published = regulationService.publish(saveReq);
        int itemCount = saveReq.getProcesses().stream()
                .mapToInt(process -> process.getItems() == null ? 0 : process.getItems().size())
                .sum();
        return MesQaInspectionRegulationJsonPublishRespVO.builder()
                .dccProjectCodeId(reqVO.getDccProjectCodeId())
                .regulationId(published.getRegulationId())
                .publishedVersionId(published.getPublishedVersionId())
                .versionNo(published.getVersionNo())
                .route(existing == null ? "CREATE" : "UPDATE")
                .processCount(saveReq.getProcesses().size())
                .itemCount(itemCount)
                .build();
    }

    private MesQaInspectionRegulationSaveReqVO toSaveRequest(
            Long dccProjectCodeId, ParsedJson parsed, MesQaInspectionRegulationDO existing, String versionNo) {
        MesQaInspectionRegulationSaveReqVO request = new MesQaInspectionRegulationSaveReqVO();
        request.setRegulationId(existing == null ? null : existing.getId());
        request.setDccProjectCodeId(dccProjectCodeId);
        request.setOwnerModule(OWNER_MODULE);
        request.setRegulationCode(parsed.regulationCode());
        request.setRegulationName(parsed.regulationName());
        request.setVersionNo(versionNo);
        request.setEffectiveDate(parsed.effectiveDate());
        request.setFinalInspectionApplicable(false);
        request.setFinalInspectionNotApplicableReason("表单解析 JSON 未规定末检");
        request.setInspectionTypeRules(defaultInspectionTypeRules());
        request.setProcesses(parsed.processes());
        request.getProcesses().forEach(process -> process.getItems().forEach(item ->
                item.setCritical(Boolean.FALSE)));
        return request;
    }

    private void mergeExistingQaOnlyFields(
            MesQaInspectionRegulationSaveReqVO request,
            MesQaInspectionRegulationPublishedVersionRespVO current) {
        if (current == null || current.getProcesses() == null) {
            return;
        }
        Map<String, MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem> existingItems =
                new LinkedHashMap<>();
        current.getProcesses().forEach(process -> {
            if (process.getItems() != null) {
                process.getItems().forEach(item -> existingItems.put(item.getItemCode(), item));
            }
        });
        request.getProcesses().forEach(process -> process.getItems().forEach(item -> {
            // The binding identity is the DCC project selected by the user.
            // JSON does not carry the persisted QA-only critical flag, so use
            // the formal publish default instead of requiring a matching item
            // from the selected project's existing regulation.
            item.setCritical(Boolean.FALSE);
            MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem previous =
                    existingItems.get(item.getItemCode());
            if (previous == null) {
                return;
            }
            item.setStandardLowerLimit(previous.getStandardLowerLimit());
            item.setStandardUpperLimit(previous.getStandardUpperLimit());
            item.setStandardUnit(previous.getStandardUnit());
            item.setStandardPrecision(previous.getStandardPrecision());
            item.setCritical(previous.getCritical());
            item.setFailureRule(previous.getFailureRule());
            item.setSourceNote(previous.getSourceNote());
            item.setSourceOriginalPage(previous.getSourceOriginalPage());
            item.setSourceOriginalItem(previous.getSourceOriginalItem());
            item.setSourceOriginalExcerpt(previous.getSourceOriginalExcerpt());
            item.setSourceOriginalMethod(previous.getSourceOriginalMethod());
            item.setEquipmentOptions(previous.getEquipmentOptions() == null
                    ? List.of()
                    : previous.getEquipmentOptions().stream().map(equipment -> {
                        MesQaInspectionRegulationSaveReqVO.EquipmentOption option =
                                new MesQaInspectionRegulationSaveReqVO.EquipmentOption();
                        option.setEquipmentId(equipment.getEquipmentId());
                        option.setEquipmentCode(equipment.getEquipmentCode());
                        option.setEquipmentName(equipment.getEquipmentName());
                        option.setEquipmentNumber(equipment.getEquipmentNumber());
                        option.setDefaultFlag(equipment.getDefaultFlag());
                        option.setSort(equipment.getSort());
                        return option;
                    }).toList());
        }));
    }

    private String resolveVersionNo(String requestedVersionNo, MesQaInspectionRegulationDO existing) {
        String normalized = StrUtil.trim(requestedVersionNo);
        if (existing == null || versionMapper.selectByRegulationIdAndVersionNo(existing.getId(), normalized) == null) {
            return normalized;
        }
        String prefix = normalized;
        String numericPart = "";
        int separator = normalized.lastIndexOf('.');
        if (separator >= 0 && separator < normalized.length() - 1
                && normalized.substring(separator + 1).chars().allMatch(Character::isDigit)) {
            prefix = normalized.substring(0, separator + 1);
            numericPart = normalized.substring(separator + 1);
        } else {
            int split = normalized.length();
            while (split > 0 && Character.isDigit(normalized.charAt(split - 1))) {
                split--;
            }
            if (split < normalized.length()) {
                prefix = normalized.substring(0, split);
                numericPart = normalized.substring(split);
            }
        }
        int nextNumber = numericPart.isEmpty() ? 1 : Integer.parseInt(numericPart) + 1;
        String candidate;
        do {
            candidate = prefix + (numericPart.isEmpty()
                    ? Integer.toString(nextNumber)
                    : String.format("%0" + numericPart.length() + "d", nextNumber));
            nextNumber++;
        } while (versionMapper.selectByRegulationIdAndVersionNo(existing.getId(), candidate) != null);
        return candidate;
    }

    private ParsedJson readParsedJson(Map<String, Object> json) {
        try {
            ParsedJson parsed = objectMapper.convertValue(json, ParsedJson.class);
            if (parsed.schemaVersion() == null || parsed.schemaVersion() != 1
                    || StrUtil.isBlank(parsed.regulationCode())
                    || StrUtil.isBlank(parsed.regulationName())
                    || StrUtil.isBlank(parsed.versionNo())
                    || parsed.effectiveDate() == null
                    || parsed.processes() == null || parsed.processes().isEmpty()) {
                throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "QA JSON 顶层字段");
            }
            for (MesQaInspectionRegulationSaveReqVO.InspectionProcess process : parsed.processes()) {
                if (process == null || StrUtil.isBlank(process.getProcessCode())
                        || StrUtil.isBlank(process.getProcessName())
                        || process.getSort() == null || process.getItems() == null || process.getItems().isEmpty()) {
                    throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "QA JSON 工序");
                }
                for (MesQaInspectionRegulationSaveReqVO.InspectionItem item : process.getItems()) {
                    if (item == null || StrUtil.isBlank(item.getItemCode()) || StrUtil.isBlank(item.getItemName())
                            || StrUtil.isBlank(item.getInspectionMethod())
                            || StrUtil.isBlank(item.getInspectionTool())
                            || StrUtil.isBlank(item.getSamplingPlanText())
                            || StrUtil.isBlank(item.getStandardText())
                            || StrUtil.isBlank(item.getResultType())
                            || item.getItemSort() == null
                            || item.getApplicableInspectionTypes() == null
                            || item.getApplicableInspectionTypes().isEmpty()) {
                        throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "QA JSON 检验项目");
                    }
                }
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "QA JSON 结构");
        }
    }

    private static List<MesQaInspectionRegulationSaveReqVO.InspectionTypeRule> defaultInspectionTypeRules() {
        List<MesQaInspectionRegulationSaveReqVO.InspectionTypeRule> rules = new ArrayList<>();
        rules.add(rule("FIRST", "FIRST", "首检", "每个适用订单开始前", true));
        rules.add(rule("PATROL_AM", "PATROL", "上午巡检", "上午班次独立轮次", true));
        rules.add(rule("PATROL_PM", "PATROL", "下午巡检", "下午班次独立轮次", true));
        rules.add(rule("FINAL", "FINAL", "末检", "订单结束前", false));
        return rules;
    }

    private static MesQaInspectionRegulationSaveReqVO.InspectionTypeRule rule(
            String key, String type, String label, String roundLabel, boolean required) {
        MesQaInspectionRegulationSaveReqVO.InspectionTypeRule rule =
                new MesQaInspectionRegulationSaveReqVO.InspectionTypeRule();
        rule.setKey(key);
        rule.setInspectionType(type);
        rule.setLabel(label);
        rule.setRoundLabel(roundLabel);
        rule.setRequired(required);
        rule.setTaskRule("按发布规程生成检验任务");
        rule.setReleaseGate("检验规则必须完整");
        return rule;
    }

    private record ParsedJson(
            Integer schemaVersion,
            String sourceFileName,
            String regulationCode,
            String regulationName,
            String versionNo,
            LocalDate effectiveDate,
            List<MesQaInspectionRegulationSaveReqVO.InspectionProcess> processes) {
    }
}
