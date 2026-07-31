package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyFieldDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyFieldMappingDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyGenerateFromRulesReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyGenerateReqDTO;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_LIMIT_METADATA_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_SIGNATURE_DUPLICATE;

@Service
@Validated
public class MesProcessPoolReviewCopyServiceImpl implements MesProcessPoolReviewCopyService {

    @Resource
    private MesProProcessPoolEventMapper eventMapper;
    @Resource
    private MesProcessPoolReviewCopyMapper reviewCopyMapper;
    @Resource
    private MesProcessPoolReviewCopyFieldMapper reviewCopyFieldMapper;
    @Resource
    private MesProcessPoolReviewCopyRuleMapper reviewCopyRuleMapper;
    @Resource
    private MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    @Resource
    private MesProcessPoolFifoAllocationService fifoAllocationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateAndSubmitReviewCopy(MesProcessPoolReviewCopyGenerateReqDTO reqDTO) {
        validateRequest(reqDTO);
        MesProProcessPoolEventDO event = requireEvent(reqDTO.getEventId());
        JsonNode rawPayload = parseRawPayload(event);
        validateFieldMappings(reqDTO.getFieldMappings(), rawPayload);
        validateReviewerSignature(reqDTO);

        List<MesProcessPoolReviewCopyFieldDO> fields = buildFields(reqDTO.getFieldMappings(), event, rawPayload);
        MesProcessPoolReviewCopyDO reviewCopy = buildReviewCopy(reqDTO, event);
        reviewCopyMapper.insert(reviewCopy);
        fields.forEach(field -> field.setReviewCopyId(reviewCopy.getId()));
        if (!fields.isEmpty() && !Boolean.TRUE.equals(reviewCopyFieldMapper.insertBatch(fields))) {
            throw new IllegalStateException("Failed to insert MES process pool review copy fields");
        }
        return reviewCopy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateAndSubmitReviewCopyFromRules(MesProcessPoolReviewCopyGenerateFromRulesReqDTO reqDTO) {
        validateRuleRequest(reqDTO);
        MesProProcessPoolEventDO event = requireEvent(reqDTO.getEventId());
        List<MesProcessPoolReviewCopyRuleDO> rules = reviewCopyRuleMapper.selectEnabledListByContext(
                event.getProcessId(), event.getDeviceId(), event.getTemplateType());
        if (CollUtil.isEmpty(rules)) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED, "reviewCopyRules");
        }
        return generateAndSubmitReviewCopy(MesProcessPoolReviewCopyGenerateReqDTO.builder()
                .eventId(reqDTO.getEventId())
                .reviewerUserId(reqDTO.getReviewerUserId())
                .reviewerSignatureId(reqDTO.getReviewerSignatureId())
                .reviewerSignatureUserId(reqDTO.getReviewerSignatureUserId())
                .reviewerSignatureSnapshot(reqDTO.getReviewerSignatureSnapshot())
                .fieldMappings(toRuleMappings(event, rules))
                .build());
    }

    private void validateRequest(MesProcessPoolReviewCopyGenerateReqDTO reqDTO) {
        if (reqDTO == null || reqDTO.getEventId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "eventId");
        }
        if (reqDTO.getReviewerUserId() == null) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_REQUIRED, "reviewerUserId");
        }
        if (reqDTO.getReviewerSignatureId() == null || reqDTO.getReviewerSignatureUserId() == null
                || StrUtil.isBlank(reqDTO.getReviewerSignatureSnapshot())) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_REQUIRED, "reviewerSignature");
        }
    }

    private void validateRuleRequest(MesProcessPoolReviewCopyGenerateFromRulesReqDTO reqDTO) {
        if (reqDTO == null || reqDTO.getEventId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "eventId");
        }
        if (reqDTO.getReviewerUserId() == null) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_REQUIRED, "reviewerUserId");
        }
        if (reqDTO.getReviewerSignatureId() == null || reqDTO.getReviewerSignatureUserId() == null
                || StrUtil.isBlank(reqDTO.getReviewerSignatureSnapshot())) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_REQUIRED, "reviewerSignature");
        }
    }

    private List<MesProcessPoolReviewCopyFieldMappingDTO> toRuleMappings(
            MesProProcessPoolEventDO event, List<MesProcessPoolReviewCopyRuleDO> rules) {
        List<MesProProcessPoolQuantityFragmentDO> fragments =
                quantityFragmentMapper.selectListByEventId(event.getId());
        return rules.stream()
                .map(rule -> MesProcessPoolReviewCopyFieldMappingDTO.builder()
                        .fieldCode(rule.getFieldCode())
                        .fieldName(rule.getFieldName())
                        .lowerLimit(rule.getLowerLimit())
                        .upperLimit(rule.getUpperLimit())
                        .valueType(rule.getValueType())
                        .affectsAllocation(Boolean.TRUE.equals(rule.getAffectsAllocation()))
                        .allocationField(resolveAllocationField(rule))
                        .sourceQuantityFragmentId(resolveSourceQuantityFragmentId(rule, fragments))
                        .templateFieldMetadataJson(rule.getTemplateFieldMetadataJson())
                        .build())
                .toList();
    }

    private MesProcessPoolFragmentOriginalField resolveAllocationField(MesProcessPoolReviewCopyRuleDO rule) {
        if (!Boolean.TRUE.equals(rule.getAffectsAllocation())) {
            return null;
        }
        if (StrUtil.isBlank(rule.getAllocationField())) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED, rule.getFieldCode());
        }
        try {
            return MesProcessPoolFragmentOriginalField.valueOf(rule.getAllocationField());
        } catch (IllegalArgumentException ex) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED, rule.getFieldCode());
        }
    }

    private Long resolveSourceQuantityFragmentId(MesProcessPoolReviewCopyRuleDO rule,
                                                 List<MesProProcessPoolQuantityFragmentDO> fragments) {
        if (!Boolean.TRUE.equals(rule.getAffectsAllocation())) {
            return null;
        }
        if (StrUtil.isBlank(rule.getSourceQuantityType())) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED, rule.getFieldCode());
        }
        List<MesProProcessPoolQuantityFragmentDO> matched = fragments.stream()
                .filter(fragment -> Objects.equals(fragment.getSourceQuantityType(), rule.getSourceQuantityType()))
                .toList();
        if (matched.size() != 1) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED,
                    rule.getFieldCode() + ".sourceQuantityType=" + rule.getSourceQuantityType());
        }
        return matched.get(0).getId();
    }

    private MesProProcessPoolEventDO requireEvent(Long eventId) {
        MesProProcessPoolEventDO event = eventMapper.selectById(eventId);
        if (event == null || StrUtil.isBlank(event.getRawPayload())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "rawPayload");
        }
        return event;
    }

    private JsonNode parseRawPayload(MesProProcessPoolEventDO event) {
        try {
            return JsonUtils.getObjectMapper().readTree(event.getRawPayload());
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "rawPayload");
        }
    }

    private void validateFieldMappings(List<MesProcessPoolReviewCopyFieldMappingDTO> mappings, JsonNode rawPayload) {
        if (CollUtil.isEmpty(mappings)) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED, "fieldMappings");
        }
        for (MesProcessPoolReviewCopyFieldMappingDTO mapping : mappings) {
            if (mapping == null || StrUtil.isBlank(mapping.getFieldCode()) || StrUtil.isBlank(mapping.getFieldName())) {
                throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED, "fieldMappings");
            }
            if (!rawPayload.has(mapping.getFieldCode())) {
                throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED, mapping.getFieldCode());
            }
            if (mapping.getLowerLimit() == null || mapping.getUpperLimit() == null) {
                throw exception(PRO_PROCESS_POOL_REVIEW_COPY_LIMIT_METADATA_REQUIRED, mapping.getFieldCode());
            }
            if (mapping.getLowerLimit().compareTo(mapping.getUpperLimit()) > 0) {
                throw exception(PRO_PROCESS_POOL_REVIEW_COPY_LIMIT_METADATA_REQUIRED, mapping.getFieldCode());
            }
            if (Boolean.TRUE.equals(mapping.getAffectsAllocation())) {
                if (mapping.getSourceQuantityFragmentId() == null || mapping.getAllocationField() == null) {
                    throw exception(PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED, mapping.getFieldCode());
                }
                fifoAllocationService.validateOriginalFieldMutationAllowed(mapping.getSourceQuantityFragmentId(),
                        mapping.getAllocationField());
            }
        }
    }

    private void validateReviewerSignature(MesProcessPoolReviewCopyGenerateReqDTO reqDTO) {
        if (!Objects.equals(reqDTO.getReviewerUserId(), reqDTO.getReviewerSignatureUserId())) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_MISMATCH);
        }
        if (eventMapper.selectBySignatureId(reqDTO.getReviewerSignatureId()) != null
                || reviewCopyMapper.selectByReviewerSignatureId(reqDTO.getReviewerSignatureId()) != null) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_SIGNATURE_DUPLICATE, reqDTO.getReviewerSignatureId());
        }
    }

    private List<MesProcessPoolReviewCopyFieldDO> buildFields(
            List<MesProcessPoolReviewCopyFieldMappingDTO> mappings,
            MesProProcessPoolEventDO event,
            JsonNode rawPayload) {
        List<MesProcessPoolReviewCopyFieldDO> fields = new ArrayList<>();
        for (MesProcessPoolReviewCopyFieldMappingDTO mapping : mappings) {
            String rawValue = rawPayload.get(mapping.getFieldCode()).asText();
            BigDecimal rawNumber = parseNumber(rawValue, mapping.getFieldCode());
            CorrectedValue corrected = correct(rawNumber, mapping);
            fields.add(MesProcessPoolReviewCopyFieldDO.builder()
                    .eventId(event.getId())
                    .sourceQuantityFragmentId(mapping.getSourceQuantityFragmentId())
                    .fieldCode(mapping.getFieldCode())
                    .fieldName(mapping.getFieldName())
                    .rawValue(format(rawNumber))
                    .correctedValue(format(corrected.value()))
                    .ruleType(corrected.ruleType())
                    .lowerLimit(mapping.getLowerLimit())
                    .upperLimit(mapping.getUpperLimit())
                    .valueType(mapping.getValueType())
                    .affectsAllocation(Boolean.TRUE.equals(mapping.getAffectsAllocation()))
                    .feedbackSourceType(event.getFeedbackSourceType())
                    .feedbackSourceId(event.getFeedbackSourceId())
                    .recordbookSourceType(event.getRecordbookSourceType())
                    .recordbookSourceId(event.getRecordbookSourceId())
                    .templateFieldMetadataJson(mapping.getTemplateFieldMetadataJson())
                    .build());
        }
        return fields;
    }

    private BigDecimal parseNumber(String rawValue, String fieldCode) {
        try {
            return new BigDecimal(rawValue);
        } catch (NumberFormatException ex) {
            throw exception(PRO_PROCESS_POOL_REVIEW_COPY_LIMIT_METADATA_REQUIRED, fieldCode);
        }
    }

    private CorrectedValue correct(BigDecimal rawValue, MesProcessPoolReviewCopyFieldMappingDTO mapping) {
        if (rawValue.compareTo(mapping.getUpperLimit()) > 0) {
            return new CorrectedValue(mapping.getUpperLimit(), MesProcessPoolReviewCopyFieldDO.RULE_CLAMP_TO_MAX);
        }
        if (rawValue.compareTo(mapping.getLowerLimit()) < 0) {
            return new CorrectedValue(mapping.getLowerLimit(), MesProcessPoolReviewCopyFieldDO.RULE_CLAMP_TO_MIN);
        }
        return new CorrectedValue(rawValue, MesProcessPoolReviewCopyFieldDO.RULE_UNCHANGED_IN_RANGE);
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private MesProcessPoolReviewCopyDO buildReviewCopy(MesProcessPoolReviewCopyGenerateReqDTO reqDTO,
                                                       MesProProcessPoolEventDO event) {
        return MesProcessPoolReviewCopyDO.builder()
                .eventId(event.getId())
                .processPoolId(event.getPoolId())
                .workOrderId(event.getWorkOrderId())
                .routeId(event.getRouteId())
                .routeProcessId(event.getRouteProcessId())
                .processId(event.getProcessId())
                .feedbackSourceType(event.getFeedbackSourceType())
                .feedbackSourceId(event.getFeedbackSourceId())
                .recordbookSourceType(event.getRecordbookSourceType())
                .recordbookSourceId(event.getRecordbookSourceId())
                .rawPayloadSnapshot(event.getRawPayload())
                .reviewStatus(MesProcessPoolReviewCopyDO.STATUS_SUBMITTED)
                .reviewerUserId(reqDTO.getReviewerUserId())
                .reviewerSignatureId(reqDTO.getReviewerSignatureId())
                .reviewerSignatureUserId(reqDTO.getReviewerSignatureUserId())
                .reviewerSignatureSnapshot(reqDTO.getReviewerSignatureSnapshot())
                .reviewedAt(LocalDateTime.now())
                .build();
    }

    private record CorrectedValue(BigDecimal value, String ruleType) {
    }
}
