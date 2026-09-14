package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceDraftReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSnapshotRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionPublishProjectionServiceImpl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED;

@Service
public class MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImpl
        implements MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort {

    private static final Long TEMPLATE_ID = 25L;
    private static final String TEMPLATE_STATUS_PUBLISHED = "PUBLISHED";
    private static final String INSTANCE_STATUS_DRAFT = "DRAFT";
    private static final String INSTANCE_STATUS_REWORKING = "REWORKING";
    private static final String INSTANCE_STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String AUDIT_KEY = "_lossReportReleaseAudit";

    private final FormTemplateVersionMapper templateVersionMapper;
    private final FormActionInstanceMapper instanceMapper;
    private final FormCenterRuntimeService runtimeService;

    public MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImpl(
            FormTemplateVersionMapper templateVersionMapper,
            FormActionInstanceMapper instanceMapper,
            FormCenterRuntimeService runtimeService) {
        this.templateVersionMapper = templateVersionMapper;
        this.instanceMapper = instanceMapper;
        this.runtimeService = runtimeService;
    }

    @Override
    public TargetResolution resolveTarget(MesProRouteFlowProcessBatchRecordDO binding,
                                          List<MesProBatchRecordCellLinkRuleDO> rules) {
        if (binding == null || !TEMPLATE_ID.equals(binding.getFormTemplateId())
                || binding.getLastPublishedTemplateVersionId() == null
                || StrUtil.isBlank(binding.getLastPublishedTemplateVersionNo())) {
            return blocked("LOSS_REPORT_DYNAMIC_FORM_TEMPLATE_REQUIRED",
                    "损耗单动态绑定缺少 template 25 已发布版本身份");
        }
        FormTemplateVersionDO template = templateVersionMapper.selectById(binding.getLastPublishedTemplateVersionId());
        if (template == null || template.getId() == null || !TEMPLATE_ID.equals(template.getTemplateId())
                || !Objects.equals(binding.getLastPublishedTemplateVersionId(), template.getId())
                || !Objects.equals(binding.getLastPublishedTemplateVersionNo(), template.getVersionNo())
                || !TEMPLATE_STATUS_PUBLISHED.equals(template.getStatus())
                || StrUtil.isBlank(template.getRecognizedSchemaJson())) {
            return blocked("LOSS_REPORT_DYNAMIC_FORM_TEMPLATE_REQUIRED",
                    "损耗单动态绑定未命中精确 PUBLISHED template 25 版本");
        }
        JSONArray recognizedFields;
        try {
            recognizedFields = JSON.parseArray(template.getRecognizedSchemaJson());
        } catch (RuntimeException ex) {
            return blocked("LOSS_REPORT_DYNAMIC_FORM_TEMPLATE_REQUIRED",
                    "损耗单动态模板识别字段快照无效");
        }
        if (recognizedFields == null || recognizedFields.isEmpty() || rules == null || rules.isEmpty()) {
            return blocked("LOSS_REPORT_MAPPING_REQUIRED", "损耗单动态模板缺少精确字段映射");
        }
        Map<Long, String> targetFieldCodes = new LinkedHashMap<>();
        Set<String> uniqueFieldCodes = new LinkedHashSet<>();
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            String fieldCode = resolveFieldCode(recognizedFields, rule);
            if (!isExactRule(template, rule, fieldCode) || !uniqueFieldCodes.add(fieldCode)
                    || targetFieldCodes.putIfAbsent(rule.getId(), fieldCode) != null) {
                return blocked("LOSS_REPORT_MAPPING_REQUIRED",
                        "损耗单动态模板存在缺失、重复或类型不一致的稳定 fieldCode 映射");
            }
        }
        return new TargetResolution()
                .setTemplateVersionId(template.getId())
                .setTemplateSnapshotHash(hash("FORM_TEMPLATE_VERSION_V1", template.getId(), template.getTemplateId(),
                        template.getVersionNo(), template.getRecognizedSchemaJson(), template.getJimuSchemaJson()))
                .setTargetFieldCodes(Map.copyOf(targetFieldCodes));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WriteResult write(WriteCommand command) {
        validateWriteCommand(command);
        MesProEdhrBatchExecutionTaskDO task = command.getBatchTask();
        FormActionInstanceDO instance = instanceMapper.selectById(task.getFormCenterInstanceId());
        validateInstance(command, instance);
        String auditHeadHash = auditHeadHash(command);
        Map<String, Object> formData = parseFormData(instance.getFormDataJson());
        if (INSTANCE_STATUS_EFFECTIVE.equals(instance.getStatus())) {
            validateReplay(formData, command, auditHeadHash);
            return resultFromSubmitSnapshot(instance.getId(), auditHeadHash, INSTANCE_STATUS_EFFECTIVE);
        }
        if (!INSTANCE_STATUS_DRAFT.equals(instance.getStatus())
                && !INSTANCE_STATUS_REWORKING.equals(instance.getStatus())) {
            throw sourceRequired("损耗单 FormCenter instance 状态不允许自动写入，instanceId=" + instance.getId());
        }
        for (FieldWrite field : command.getFields()) {
            Object existing = formData.get(field.getTargetFieldCode());
            if (hasValue(existing) && !Objects.equals(String.valueOf(existing), String.valueOf(field.getValue()))) {
                throw sourceRequired("损耗单 FormCenter 目标 fieldCode 已存在冲突值：" + field.getTargetFieldCode());
            }
            formData.put(field.getTargetFieldCode(), field.getValue());
        }
        validateRequiredFields(command.getTarget().getTemplateVersionId(), formData);
        formData.put(AUDIT_KEY, auditMetadata(command, auditHeadHash));
        FormInstanceDraftReqVO draft = new FormInstanceDraftReqVO();
        draft.setFormData(formData);
        runtimeService.saveDraft(instance.getId(), draft, instance.getApplicantUserId());
        FormInstanceSubmitReqVO submit = new FormInstanceSubmitReqVO();
        submit.setFormData(formData);
        FormInstanceRespVO submitted = runtimeService.submitInstance(instance.getId(), submit,
                instance.getApplicantUserId());
        if (submitted == null || !Objects.equals(instance.getId(), submitted.getId())
                || !INSTANCE_STATUS_EFFECTIVE.equals(submitted.getStatus())) {
            throw sourceRequired("损耗单 FormCenter instance 未进入 EFFECTIVE，instanceId=" + instance.getId());
        }
        return resultFromSubmitSnapshot(instance.getId(), auditHeadHash, submitted.getStatus());
    }

    private void validateWriteCommand(WriteCommand command) {
        if (command == null || command.getTenantId() == null || command.getBatchExecutionId() == null
                || command.getBatchTask() == null || command.getBinding() == null
                || command.getTarget() == null || !command.getTarget().isValid()
                || command.getFields() == null || command.getFields().isEmpty()
                || StrUtil.isBlank(command.getSourceSnapshotHash()) || StrUtil.isBlank(command.getEvidenceHash())
                || command.getSignatureEvidence() == null || command.getSignatureEvidence().isEmpty()) {
            throw sourceRequired("损耗单动态 FormCenter 写入命令不完整");
        }
        MesProEdhrBatchExecutionTaskDO task = command.getBatchTask();
        MesProRouteFlowProcessBatchRecordDO binding = command.getBinding();
        if (!Objects.equals(command.getBatchExecutionId(), task.getBatchExecutionId())
                || !Objects.equals(binding.getId(), task.getRouteBindingId())
                || !Objects.equals(binding.getFormBindingKey(), task.getFormBindingKey())
                || !Objects.equals(binding.getFormTemplateId(), task.getFormTemplateId())
                || !Objects.equals(binding.getLastPublishedTemplateVersionId(), task.getFormTemplateVersionId())
                || !Objects.equals(binding.getLastPublishedTemplateVersionNo(), task.getFormTemplateVersionNo())
                || !Objects.equals(command.getTarget().getTemplateVersionId(), task.getFormTemplateVersionId())
                || task.getFormCenterInstanceId() == null || StrUtil.isBlank(task.getRouteBindingSnapshotHash())
                || StrUtil.isBlank(binding.getRecordCategorySnapshotHash())
                || !Objects.equals(binding.getRecordCategorySnapshotHash(), task.getRouteBindingSnapshotHash())
                || StrUtil.isBlank(binding.getSlotConfigSnapshotHash())
                || StrUtil.isBlank(task.getSlotConfigSnapshotHash())
                || !Objects.equals(binding.getSlotConfigSnapshotHash(), task.getSlotConfigSnapshotHash())) {
            throw sourceRequired("损耗单动态 FormCenter task/binding/instance 身份不一致");
        }
        Set<Long> fieldRuleIds = new LinkedHashSet<>();
        Set<String> targetFields = new LinkedHashSet<>();
        for (FieldWrite field : command.getFields()) {
            if (field == null || field.getRuleId() == null || field.getRuleVersion() == null
                    || StrUtil.isBlank(field.getSourceCellKey()) || StrUtil.isBlank(field.getSourceFieldCode())
                    || StrUtil.isBlank(field.getTargetFieldCode()) || field.getValue() == null
                    || StrUtil.isBlank(field.getSourceValueHash())
                    || !Objects.equals(command.getTarget().getTargetFieldCodes().get(field.getRuleId()),
                    field.getTargetFieldCode())
                    || !fieldRuleIds.add(field.getRuleId()) || !targetFields.add(field.getTargetFieldCode())) {
                throw sourceRequired("损耗单动态 FormCenter 逐字段写入证据不完整或重复");
            }
        }
    }

    private void validateInstance(WriteCommand command, FormActionInstanceDO instance) {
        MesProEdhrBatchExecutionTaskDO task = command.getBatchTask();
        if (instance == null || instance.getId() == null || instance.getApplicantUserId() == null
                || !Objects.equals(task.getFormCenterInstanceId(), instance.getId())
                || !Objects.equals(command.getTenantId(), instance.getTenantId())
                || !"MES".equals(instance.getSystemCode()) || !"EDHR_ROUTE_FORM".equals(instance.getObjectType())
                || !Objects.equals(String.valueOf(task.getId()), instance.getObjectId())) {
            throw sourceRequired("损耗单 FormCenter instance 不属于当前 batch task");
        }
        BusinessActionContextReqVO context;
        try {
            context = JsonUtils.parseObject(instance.getBusinessContextJson(), BusinessActionContextReqVO.class);
        } catch (RuntimeException ex) {
            throw sourceRequired("损耗单 FormCenter instance 业务上下文无效");
        }
        String expectedActionCode = MesProRouteVersionPublishProjectionServiceImpl.routeFormActionCode(
                Long.valueOf(instance.getObjectVersion()), task.getFormBindingKey());
        if (context == null || !Objects.equals(command.getTenantId(), context.getTenantId())
                || !Objects.equals(instance.getSystemCode(), context.getSystemCode())
                || !Objects.equals(instance.getObjectType(), context.getObjectType())
                || !Objects.equals(instance.getObjectId(), context.getObjectId())
                || !Objects.equals(instance.getObjectVersion(), context.getObjectVersion())
                || !Objects.equals(expectedActionCode, instance.getActionCode())
                || !Objects.equals(expectedActionCode, context.getActionCode())) {
            throw sourceRequired("损耗单 FormCenter instance 业务动作上下文不匹配");
        }
    }

    private void validateRequiredFields(Long templateVersionId, Map<String, Object> formData) {
        FormTemplateVersionDO template = templateVersionMapper.selectById(templateVersionId);
        JSONArray fields = template == null ? null : JSON.parseArray(template.getRecognizedSchemaJson());
        if (fields == null || fields.isEmpty()) {
            throw sourceRequired("损耗单动态模板识别字段快照无效");
        }
        for (Object raw : fields) {
            JSONObject field = JSON.parseObject(JSON.toJSONString(raw));
            String fieldCode = StrUtil.trim(field.getString("fieldCode"));
            if (Boolean.TRUE.equals(field.getBoolean("required")) && !hasValue(formData.get(fieldCode))) {
                throw sourceRequired("损耗单动态模板必填 fieldCode 未写入：" + fieldCode);
            }
        }
    }

    private void validateReplay(Map<String, Object> formData, WriteCommand command, String auditHeadHash) {
        Object rawAudit = formData.get(AUDIT_KEY);
        JSONObject audit = rawAudit == null ? null : JSON.parseObject(JSON.toJSONString(rawAudit));
        if (audit == null || !Objects.equals(auditHeadHash, audit.getString("fieldAuditHeadHash"))
                || !Objects.equals(command.getSourceSnapshotHash(), audit.getString("sourceSnapshotHash"))) {
            throw sourceRequired("已生效损耗单 FormCenter instance 与本次正式来源不一致");
        }
        for (FieldWrite field : command.getFields()) {
            if (!Objects.equals(String.valueOf(field.getValue()),
                    String.valueOf(formData.get(field.getTargetFieldCode())))) {
                throw sourceRequired("已生效损耗单 FormCenter fieldCode 与本次正式来源不一致："
                        + field.getTargetFieldCode());
            }
        }
    }

    private WriteResult resultFromSubmitSnapshot(Long instanceId, String auditHeadHash, String status) {
        FormInstanceSnapshotRespVO snapshot = runtimeService.getInstanceSnapshots(instanceId).stream()
                .filter(item -> item != null && item.getId() != null && "SUBMIT".equals(item.getSnapshotType()))
                .filter(item -> snapshotHasAuditHead(item, auditHeadHash))
                .max(Comparator.comparing(FormInstanceSnapshotRespVO::getSnapshotVersion)
                        .thenComparing(FormInstanceSnapshotRespVO::getId))
                .orElseThrow(() -> sourceRequired("损耗单 FormCenter 提交缺少匹配的正式审计快照"));
        return new WriteResult().setFormCenterInstanceId(instanceId).setFieldAuditSnapshotId(snapshot.getId())
                .setFieldAuditHeadHash(auditHeadHash).setEffectiveStatus(status);
    }

    private boolean snapshotHasAuditHead(FormInstanceSnapshotRespVO snapshot, String auditHeadHash) {
        Object rawAudit = snapshot.getFormData() == null ? null : snapshot.getFormData().get(AUDIT_KEY);
        JSONObject audit = rawAudit == null ? null : JSON.parseObject(JSON.toJSONString(rawAudit));
        return audit != null && Objects.equals(auditHeadHash, audit.getString("fieldAuditHeadHash"));
    }

    private Map<String, Object> auditMetadata(WriteCommand command, String auditHeadHash) {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("fieldAuditHeadHash", auditHeadHash);
        audit.put("sourceSnapshotHash", command.getSourceSnapshotHash());
        audit.put("evidenceHash", command.getEvidenceHash());
        audit.put("batchExecutionId", command.getBatchExecutionId());
        audit.put("batchTaskId", command.getBatchTask().getId());
        audit.put("formTemplateId", command.getBinding().getFormTemplateId());
        audit.put("formTemplateVersionId", command.getTarget().getTemplateVersionId());
        audit.put("templateSnapshotHash", command.getTarget().getTemplateSnapshotHash());
        audit.put("fields", command.getFields().stream().map(this::fieldAuditMetadata).toList());
        audit.put("signatures", command.getSignatureEvidence().stream().map(this::signatureMetadata).toList());
        return audit;
    }

    private Map<String, Object> fieldAuditMetadata(FieldWrite field) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("ruleId", field.getRuleId());
        value.put("ruleVersion", field.getRuleVersion());
        value.put("sourceType", "PRODUCTION_LOSS");
        value.put("sourceCellKey", field.getSourceCellKey());
        value.put("sourceFieldCode", field.getSourceFieldCode());
        value.put("sourceValueHash", field.getSourceValueHash());
        value.put("targetFieldCode", field.getTargetFieldCode());
        value.put("valueDisplay", field.getDisplayValue());
        return value;
    }

    private Map<String, Object> signatureMetadata(MesTeamLeaderActiveOrderReleaseSignatureEvidence signature) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("role", signature.getRole());
        value.put("sourceType", signature.getSourceType());
        value.put("sourceId", signature.getSourceId());
        value.put("signatureId", signature.getSignatureId());
        value.put("userId", signature.getUserId());
        value.put("signedAt", signature.getSignedAt());
        value.put("evidenceHash", signature.getEvidenceHash());
        return value;
    }

    private String auditHeadHash(WriteCommand command) {
        List<String> parts = new ArrayList<>();
        parts.add("PRODUCTION_LOSS_FORMCENTER_AUDIT_V1");
        parts.add(String.valueOf(command.getTenantId()));
        parts.add(String.valueOf(command.getBatchExecutionId()));
        parts.add(String.valueOf(command.getBatchTask().getId()));
        parts.add(String.valueOf(command.getBatchTask().getFormCenterInstanceId()));
        parts.add(command.getSourceSnapshotHash());
        parts.add(command.getEvidenceHash());
        parts.add(command.getTarget().getTemplateSnapshotHash());
        command.getFields().stream().sorted(Comparator.comparing(FieldWrite::getRuleId)).forEach(field -> {
            parts.add(String.valueOf(field.getRuleId()));
            parts.add(String.valueOf(field.getRuleVersion()));
            parts.add(field.getSourceCellKey());
            parts.add(field.getSourceFieldCode());
            parts.add(field.getSourceValueHash());
            parts.add(field.getTargetFieldCode());
        });
        command.getSignatureEvidence().stream()
                .sorted(Comparator.comparing(MesTeamLeaderActiveOrderReleaseSignatureEvidence::getSignatureId))
                .forEach(signature -> {
                    parts.add(String.valueOf(signature.getSignatureId()));
                    parts.add(signature.getEvidenceHash());
                });
        return sha256(String.join("|", parts));
    }

    private String resolveFieldCode(JSONArray fields, MesProBatchRecordCellLinkRuleDO rule) {
        if (rule == null || rule.getTargetRowIndex() == null || rule.getTargetColumnIndex() == null
                || rule.getTargetRowIndex() < 3) {
            return null;
        }
        int index;
        if (rule.getTargetColumnIndex() == 1) {
            index = (rule.getTargetRowIndex() - 3) * 2;
        } else if (rule.getTargetColumnIndex() == 3) {
            index = (rule.getTargetRowIndex() - 3) * 2 + 1;
        } else {
            return null;
        }
        if (index < 0 || index >= fields.size()) {
            return null;
        }
        JSONObject field = JSON.parseObject(JSON.toJSONString(fields.get(index)));
        return StrUtil.trim(field.getString("fieldCode"));
    }

    private boolean isExactRule(FormTemplateVersionDO template, MesProBatchRecordCellLinkRuleDO rule,
                                String fieldCode) {
        if (rule == null || rule.getId() == null || rule.getRuleVersion() == null
                || !Boolean.TRUE.equals(rule.getEnabled()) || StrUtil.isBlank(fieldCode)
                || !"FORM_TEMPLATE_VERSION".equals(rule.getScopeType())
                || !Objects.equals(template.getId(), rule.getScopeId())
                || !"PRODUCTION_LOSS".equals(StrUtil.trim(rule.getSourceType()))
                || StrUtil.isBlank(rule.getSourceCellKey()) || StrUtil.isBlank(rule.getSourceFieldCode())
                || !Objects.equals("FORMTPL:" + template.getId(), rule.getTargetReportId())
                || !Objects.equals(rule.getTargetRowIndex() + ":" + rule.getTargetColumnIndex(),
                rule.getTargetCellKey()) || StrUtil.isBlank(rule.getTargetValueType())
                || StrUtil.isBlank(rule.getTemplateSnapshotHash())) {
            return false;
        }
        JSONArray fields = JSON.parseArray(template.getRecognizedSchemaJson());
        int index = rule.getTargetColumnIndex() == 1 ? (rule.getTargetRowIndex() - 3) * 2
                : (rule.getTargetRowIndex() - 3) * 2 + 1;
        JSONObject field = JSON.parseObject(JSON.toJSONString(fields.get(index)));
        return Objects.equals(normalizeFieldType(field.getString("fieldType")), rule.getTargetValueType());
    }

    private String normalizeFieldType(String fieldType) {
        return switch (StrUtil.trimToEmpty(fieldType).toLowerCase()) {
            case "number" -> "NUMBER";
            case "date" -> "DATE";
            case "datetime" -> "DATETIME";
            case "checkbox" -> "BOOLEAN";
            default -> "STRING";
        };
    }

    private Map<String, Object> parseFormData(String formDataJson) {
        if (StrUtil.isBlank(formDataJson)) {
            return new LinkedHashMap<>();
        }
        JSONObject parsed;
        try {
            parsed = JSON.parseObject(formDataJson);
        } catch (RuntimeException ex) {
            throw sourceRequired("损耗单 FormCenter instance 表单数据无效");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (parsed != null) {
            result.putAll(parsed);
        }
        return result;
    }

    private boolean hasValue(Object value) {
        return value != null && (!(value instanceof String text) || StrUtil.isNotBlank(text));
    }

    private TargetResolution blocked(String blockerType, String message) {
        return new TargetResolution().setBlockerType(blockerType).setBlockerMessage(message);
    }

    private RuntimeException sourceRequired(String message) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_SOURCE_REQUIRED, message);
    }

    private String hash(Object... values) {
        List<String> parts = new ArrayList<>();
        for (Object value : values) {
            parts.add(value == null ? "<null>" : String.valueOf(value));
        }
        return sha256(String.join("|", parts));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
