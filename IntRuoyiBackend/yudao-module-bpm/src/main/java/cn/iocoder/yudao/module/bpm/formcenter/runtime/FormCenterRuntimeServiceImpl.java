package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.*;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.*;
import cn.iocoder.yudao.module.bpm.formcenter.model.*;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FormCenterRuntimeServiceImpl implements FormCenterRuntimeService {

    private static final String BUSINESS_KEY_PREFIX = "FORM_ACTION:";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final TypeReference<List<FormPolicySlot>> POLICY_SLOT_LIST =
            new TypeReference<>() {
            };
    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {
            };

    @Resource
    private FormTemplateVersionMapper templateVersionMapper;
    @Resource
    private FormActionPolicyMapper policyMapper;
    @Resource
    private FormActionInstanceMapper instanceMapper;
    @Resource
    private FormActionSnapshotMapper snapshotMapper;
    @Resource
    private FormTaskPermissionMapper taskPermissionMapper;
    @Resource
    private FormEffectExecutionMapper effectExecutionMapper;
    @Resource
    @Lazy
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private ObjectProvider<FormBusinessEffectExecutor> effectExecutorProvider;
    @Resource
    private ObjectProvider<FormControlledActionLifecycleAdapter> lifecycleAdapterProvider;

    @Override
    public PageResult<FormCenterTemplateRespVO> getTemplatePool(FormCenterTemplatePoolPageReqVO reqVO) {
        reqVO.setTenantId(currentTenantIdIfAbsent(reqVO.getTenantId()));
        PageResult<FormTemplateVersionDO> page = templateVersionMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toTemplateRespVO).toList(), page.getTotal());
    }

    @Override
    public PageResult<FormPolicyRespVO> getPolicyPage(FormPolicyPageReqVO reqVO) {
        reqVO.setTenantId(currentTenantIdIfAbsent(reqVO.getTenantId()));
        PageResult<FormActionPolicyDO> page = policyMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toPolicyRespVO).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormPolicyRespVO savePolicy(FormPolicySaveReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<FormPolicySlot> slots = toPolicySlots(tenantId, reqVO.getSlots());
        FormActionPolicyDO policy = FormActionPolicyDO.builder()
                .tenantId(tenantId)
                .dataDomain(reqVO.getDataDomain())
                .systemCode(reqVO.getSystemCode())
                .objectType(reqVO.getObjectType())
                .actionCode(reqVO.getActionCode())
                .objectState(reqVO.getObjectState())
                .policyType(reqVO.getPolicyType())
                .approvalMode(resolveApprovalMode(reqVO.getApprovalMode()).name())
                .bpmProcessKey(reqVO.getBpmProcessKey())
                .effectExecutorCode(reqVO.getEffectExecutorCode())
                .status(STATUS_DRAFT)
                .slotsJson(JsonUtils.toJsonString(slots))
                .remark(reqVO.getRemark())
                .build();
        policyMapper.insert(policy);
        return toPolicyRespVO(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishPolicy(Long policyId) {
        FormActionPolicyDO policy = requirePolicy(policyId);
        requireEffectExecutor(policy.getEffectExecutorCode());
        if (FormApprovalMode.BPM_REQUIRED.name().equals(policy.getApprovalMode())
                && isBlank(policy.getBpmProcessKey())) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "BPM process key is required before publishing policy: " + policyId);
        }
        policy.setStatus(STATUS_PUBLISHED);
        policyMapper.updateById(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormPolicyRespVO switchPolicyApprovalMode(Long policyId, FormPolicySwitchApprovalModeReqVO reqVO) {
        FormActionPolicyDO policy = requirePolicy(policyId);
        FormApprovalMode approvalMode = resolveApprovalMode(reqVO.getApprovalMode());
        policy.setApprovalMode(approvalMode.name());
        if (!isBlank(reqVO.getBpmProcessKey())) {
            policy.setBpmProcessKey(reqVO.getBpmProcessKey());
        }
        if (approvalMode == FormApprovalMode.BPM_REQUIRED && isBlank(policy.getBpmProcessKey())) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "BPM process key is required for BPM_REQUIRED policy: " + policyId);
        }
        policyMapper.updateById(policy);
        return toPolicyRespVO(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormCenterTemplateImportRespVO importDoc(FormCenterTemplateImportReqVO reqVO, Long userId) {
        MultipartFile file = reqVO.getFile();
        if (file == null || file.isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template source file is required");
        }
        String fileName = Objects.requireNonNullElse(file.getOriginalFilename(), "");
        if (!fileName.endsWith(".doc") && !fileName.endsWith(".docx")) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_TYPE_UNSUPPORTED,
                    "Only doc/docx template source is supported: " + fileName);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long templateId = resolveImportTemplateId(tenantId, reqVO);
        String versionNo = nextVersionNo(tenantId, templateId, reqVO.getTemplateName());
        FormTemplateVersionDO version = FormTemplateVersionDO.builder()
                .templateId(templateId)
                .tenantId(tenantId)
                .templateName(reqVO.getTemplateName())
                .versionNo(versionNo)
                .status(FormTemplateStatus.DRAFT.name())
                .sourceFileName(fileName)
                .sourceFileContent(readFileContent(file))
                .recognizedSchemaJson(JsonUtils.toJsonString(List.of()))
                .remark(reqVO.getRemark())
                .build();
        templateVersionMapper.insert(version);
        if (version.getTemplateId() == null) {
            version.setTemplateId(version.getId());
            templateVersionMapper.updateById(version);
        }
        FormCenterTemplateImportRespVO respVO = new FormCenterTemplateImportRespVO();
        respVO.setTemplateId(version.getTemplateId());
        respVO.setVersionNo(version.getVersionNo());
        respVO.setStatus(version.getStatus());
        respVO.setImportAction(reqVO.getSelectedTemplateId() == null ? "CREATE" : "UPGRADE");
        respVO.setSourceTemplateId(reqVO.getSelectedTemplateId());
        respVO.setRecognizedFields(List.of());
        respVO.setWarnings(List.of());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveJimuSchema(Long templateId, String versionNo, FormCenterTemplateJimuSchemaReqVO reqVO) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        requireStatus(version, FormTemplateStatus.DRAFT, FormTemplateStatus.READY);
        version.setJimuSchemaJson(reqVO.getJimuSchema());
        templateVersionMapper.updateById(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishTemplate(Long templateId, String versionNo) {
        updateTemplateStatus(templateId, versionNo, FormTemplateStatus.PUBLISHED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableTemplate(Long templateId, String versionNo) {
        updateTemplateStatus(templateId, versionNo, FormTemplateStatus.DISABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableTemplate(Long templateId, String versionNo) {
        updateTemplateStatus(templateId, versionNo, FormTemplateStatus.PUBLISHED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void obsoleteTemplate(Long templateId, String versionNo) {
        updateTemplateStatus(templateId, versionNo, FormTemplateStatus.OBSOLETE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormTemplateObsoleteRespVO submitTemplateObsoleteRequest(Long templateId, String versionNo,
            FormTemplateObsoleteReqVO reqVO, Long userId) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        BusinessActionContextReqVO context = buildTemplateContext(version, "OBSOLETE", reqVO.getReason());
        FormInstanceCreateReqVO createReqVO = new FormInstanceCreateReqVO();
        createReqVO.setContext(context);
        createReqVO.setIdempotencyKey("TPL-OBSOLETE-" + version.getId());
        createReqVO.setFormData(Map.of("reason", reqVO.getReason(), "templateVersionId", version.getId()));
        FormInstanceRespVO draft = createInstance(createReqVO, userId);
        FormInstanceSubmitReqVO submitReqVO = new FormInstanceSubmitReqVO();
        submitReqVO.setFormData(createReqVO.getFormData());
        submitReqVO.setStartUserSelectAssignees(reqVO.getStartUserSelectAssignees());
        FormInstanceRespVO submitted = submitInstance(draft.getId(), submitReqVO, userId);
        FormTemplateObsoleteRespVO respVO = new FormTemplateObsoleteRespVO();
        respVO.setApprovalRequestId(submitted.getId());
        respVO.setApprovalProcessInstanceId(submitted.getBpmProcessInstanceId());
        respVO.setStatus(submitted.getStatus());
        return respVO;
    }

    @Override
    public FormTemplateObsoletePendingRespVO findTemplateObsoletePendingRequest(Long templateId, String versionNo,
            Long userId) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        BusinessActionContextReqVO context = buildTemplateContext(version, "OBSOLETE", null);
        FormInstanceRespVO active = findActiveBusinessAction(context);
        if (active == null) {
            return null;
        }
        FormActionInstanceDO instance = requireInstance(active.getId());
        FormTemplateObsoletePendingRespVO respVO = new FormTemplateObsoletePendingRespVO();
        respVO.setApprovalRequestId(instance.getId());
        respVO.setApprovalProcessInstanceId(instance.getBpmProcessInstanceId());
        respVO.setApplicantUserId(instance.getApplicantUserId());
        respVO.setCanWithdraw(Objects.equals(instance.getApplicantUserId(), userId));
        respVO.setObjectState(instance.getObjectState());
        respVO.setStatus(instance.getStatus());
        respVO.setReason(context.getReason());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawTemplateObsoleteRequest(Long templateId, String versionNo, String reason, Long userId) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        BusinessActionContextReqVO context = buildTemplateContext(version, "OBSOLETE", reason);
        FormInstanceRespVO active = findActiveBusinessAction(context);
        if (active == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_INSTANCE_STATUS_INVALID,
                    "No active obsolete request for template: " + templateId + "/" + versionNo);
        }
        abandonInstance(active.getId(), userId);
    }

    @Override
    public byte[] getTemplateSourceFile(Long templateId, String versionNo) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        if (version.getSourceFileContent() == null) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(version.getSourceFileContent());
    }

    @Override
    public FormActionResolutionRespVO resolveAction(BusinessActionContextReqVO reqVO) {
        BusinessActionContext context = toContext(reqVO);
        FormActionPolicyDO policy = requirePublishedPolicy(context);
        return toResolutionRespVO(toResolution(policy));
    }

    @Override
    public FormInstanceRespVO findActiveBusinessAction(BusinessActionContextReqVO reqVO) {
        BusinessActionContext context = toContext(reqVO);
        List<FormActionInstanceDO> instances = instanceMapper.selectByBusinessActionAndStatuses(context.getTenantId(),
                context.getSystemCode(), context.getObjectType(), context.getObjectId(), context.getActionCode(),
                List.of(FormInstanceStatus.IN_APPROVAL.name(), FormInstanceStatus.REWORKING.name()));
        return instances.isEmpty() ? null : toInstanceRespVO(instances.get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormInstanceRespVO createInstance(FormInstanceCreateReqVO reqVO, Long userId) {
        BusinessActionContext context = toContext(reqVO.getContext());
        FormActionPolicyDO policy = requirePublishedPolicy(context);
        FormActionInstanceDO instance = FormActionInstanceDO.builder()
                .instanceCode("FCI-" + context.getTenantId() + "-" + UUID.randomUUID())
                .tenantId(context.getTenantId())
                .policyId(policy.getId())
                .applicantUserId(userId)
                .status(FormInstanceStatus.DRAFT.name())
                .dataDomain(context.getDataDomain())
                .systemCode(context.getSystemCode())
                .objectType(context.getObjectType())
                .objectId(context.getObjectId())
                .objectVersion(context.getObjectVersion())
                .actionCode(context.getActionCode())
                .objectState(context.getObjectState())
                .idempotencyKey(reqVO.getIdempotencyKey())
                .businessContextJson(JsonUtils.toJsonString(context))
                .formDataJson(toJson(reqVO.getFormData()))
                .build();
        instanceMapper.insert(instance);
        if (reqVO.getFormData() != null && !reqVO.getFormData().isEmpty()) {
            recordSnapshot(instance, FormSnapshotType.DRAFT, reqVO.getFormData(), List.of());
        }
        return toInstanceRespVO(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(Long instanceId, FormInstanceDraftReqVO reqVO, Long userId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireOwner(instance, userId);
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING);
        instance.setFormDataJson(toJson(reqVO.getFormData()));
        instanceMapper.updateById(instance);
        recordSnapshot(instance, FormSnapshotType.DRAFT, reqVO.getFormData(), parseAttachmentIds(reqVO.getAttachmentIds()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormInstanceRespVO submitInstance(Long instanceId, FormInstanceSubmitReqVO reqVO, Long userId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireOwner(instance, userId);
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING);
        FormActionPolicyDO policy = requirePolicy(instance.getPolicyId());
        instance.setFormDataJson(toJson(reqVO.getFormData()));
        recordSnapshot(instance, FormSnapshotType.SUBMIT, reqVO.getFormData(), List.of());
        if (FormApprovalMode.DIRECT.name().equals(policy.getApprovalMode())) {
            return applyDirectEffect(instance);
        }
        FormActionInstance runtimeInstance = toRuntimeInstance(instance, policy);
        preflightLifecycle(runtimeInstance);
        BpmProcessInstanceCreateReqDTO reqDTO = buildBpmRequest(instance, policy, reqVO.getStartUserSelectAssignees());
        String processInstanceId = processInstanceService.createProcessInstance(userId, reqDTO);
        instance.setBpmProcessInstanceId(processInstanceId);
        instance.setStatus(FormInstanceStatus.IN_APPROVAL.name());
        instanceMapper.updateById(instance);
        notifyPendingApprovalStarted(toRuntimeInstance(instance, policy));
        return toInstanceRespVO(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reworkSubmitInstance(Long instanceId, FormInstanceSubmitReqVO reqVO, Long userId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireOwner(instance, userId);
        requireStatus(instance, FormInstanceStatus.REWORKING);
        instance.setFormDataJson(toJson(reqVO.getFormData()));
        instance.setStatus(FormInstanceStatus.IN_APPROVAL.name());
        instanceMapper.updateById(instance);
        recordSnapshot(instance, FormSnapshotType.REWORK_SUBMIT, reqVO.getFormData(), List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandonInstance(Long instanceId, Long userId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireOwner(instance, userId);
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING, FormInstanceStatus.REJECTED);
        instance.setStatus(FormInstanceStatus.ABANDONED.name());
        instanceMapper.updateById(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmTaskCreated(FormBpmTaskCreatedReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcess(reqVO.getProcessInstanceId());
        for (Long userId : reqVO.getHandlerUserIds()) {
            FormTaskPermissionDO permission = taskPermissionMapper.selectByTaskIdAndUserId(instance.getTenantId(),
                    instance.getId(), reqVO.getTaskId(), userId);
            if (permission == null) {
                permission = FormTaskPermissionDO.builder()
                        .tenantId(instance.getTenantId())
                        .instanceId(instance.getId())
                        .bpmProcessInstanceId(reqVO.getProcessInstanceId())
                        .taskId(reqVO.getTaskId())
                        .userId(userId)
                        .permissionCodesJson(JsonUtils.toJsonString(List.of(FormTaskPermissionCode.VIEW.name(),
                                FormTaskPermissionCode.APPROVE.name(), FormTaskPermissionCode.REJECT.name(),
                                FormTaskPermissionCode.REWORK.name())))
                        .status(FormTaskPermissionDO.STATUS_ACTIVE)
                        .build();
                taskPermissionMapper.insert(permission);
            } else {
                permission.setStatus(FormTaskPermissionDO.STATUS_ACTIVE);
                taskPermissionMapper.updateById(permission);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmTaskCompleted(FormBpmTaskCompletedReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcess(reqVO.getProcessInstanceId());
        List<FormTaskPermissionDO> permissions = taskPermissionMapper.selectActiveByTaskId(instance.getTenantId(),
                instance.getId(), reqVO.getTaskId());
        for (FormTaskPermissionDO permission : permissions) {
            permission.setStatus(FormTaskPermissionDO.STATUS_REVOKED);
            taskPermissionMapper.updateById(permission);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmReworkRequired(FormBpmReworkRequiredReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcess(reqVO.getProcessInstanceId());
        instance.setStatus(FormInstanceStatus.REWORKING.name());
        instanceMapper.updateById(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmProcessRejected(FormBpmProcessRejectedReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcess(reqVO.getProcessInstanceId());
        instance.setStatus(FormInstanceStatus.REJECTED.name());
        instanceMapper.updateById(instance);
        notifyPendingApprovalClosed(instance, FormControlledActionApprovalOutcome.REJECTED, reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormEffectExecutionRespVO onBpmProcessApproved(FormBpmProcessApprovedReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcess(reqVO.getProcessInstanceId());
        requireStatus(instance, FormInstanceStatus.IN_APPROVAL);
        FormEffectExecutionDO execution = executeEffect(instance, false);
        notifyPendingApprovalClosed(instance, FormControlledActionApprovalOutcome.valueOf(instance.getStatus()), null);
        return toEffectRespVO(execution);
    }

    @Override
    public List<FormInstanceSnapshotRespVO> getInstanceSnapshots(Long instanceId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        return snapshotMapper.selectByInstanceId(instance.getTenantId(), instanceId)
                .stream().map(this::toSnapshotRespVO).toList();
    }

    @Override
    public PageResult<FormEffectExecutionRespVO> getPendingEffects(FormEffectPendingPageReqVO reqVO) {
        reqVO.setTenantId(currentTenantIdIfAbsent(reqVO.getTenantId()));
        PageResult<FormEffectExecutionDO> page = effectExecutionMapper.selectPendingPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toEffectRespVO).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormEffectExecutionRespVO retryEffect(Long instanceId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireStatus(instance, FormInstanceStatus.EFFECT_FAILED_PENDING);
        return toEffectRespVO(executeEffect(instance, true));
    }

    private FormPolicySlot toPolicySlot(Long tenantId, FormPolicySlotReqVO slotReqVO) {
        FormTemplateVersionDO version = templateVersionMapper.selectLatestPublishedByTemplateId(tenantId,
                slotReqVO.getTemplateId());
        if (version == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Published template version not found for template: " + slotReqVO.getTemplateId());
        }
        return new FormPolicySlot(slotReqVO.getSlotCode(), Boolean.TRUE.equals(slotReqVO.getRequired()),
                FormTemplateVersionRef.of(version.getId(), String.valueOf(version.getTemplateId()),
                        version.getVersionNo(), version.getTemplateName()));
    }

    private List<FormPolicySlot> toPolicySlots(Long tenantId, List<FormPolicySlotReqVO> reqVOS) {
        return reqVOS == null ? List.of() : reqVOS.stream().map(reqVO -> toPolicySlot(tenantId, reqVO)).toList();
    }

    private FormTemplateVersionDO requireLatestPublishedTemplateVersion(Long tenantId, FormPolicySlot slot) {
        Long templateId = parseTemplateId(slot, slot.getTemplateVersionRef());
        FormTemplateVersionDO version = templateVersionMapper.selectLatestPublishedByTemplateId(tenantId, templateId);
        if (version == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Published template version not found for template: " + templateId);
        }
        return version;
    }

    private void requireStatus(FormTemplateVersionDO version, FormTemplateStatus... statuses) {
        for (FormTemplateStatus status : statuses) {
            if (status.name().equals(version.getStatus())) {
                return;
            }
        }
        throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                "Template version status invalid: " + version.getStatus());
    }

    private Long parseTemplateId(FormPolicySlot slot, FormTemplateVersionRef ref) {
        if (ref == null || isBlank(ref.getTemplateCode())) {
            throw new FormCenterException(FormCenterErrorCode.FORM_TEMPLATE_SLOT_CONFLICT,
                    "Template identity is required for slot: " + slot.getSlotCode());
        }
        return Long.valueOf(ref.getTemplateCode());
    }

    private BpmProcessInstanceCreateReqDTO buildBpmRequest(FormActionInstanceDO instance, FormActionPolicyDO policy,
            Map<String, List<Long>> startUserSelectAssignees) {
        Map<String, Object> variables = buildBpmVariables(toContext(instance));
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                startUserSelectAssignees);
        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO();
        reqDTO.setProcessDefinitionKey(policy.getBpmProcessKey());
        reqDTO.setBusinessKey(BUSINESS_KEY_PREFIX + instance.getInstanceCode());
        reqDTO.setVariables(variables);
        reqDTO.setStartUserSelectAssignees(startUserSelectAssignees);
        return reqDTO;
    }

    private Map<String, Object> buildBpmVariables(BusinessActionContext context) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("tenantId", context.getTenantId());
        variables.put("dataDomain", context.getDataDomain());
        variables.put("systemCode", context.getSystemCode());
        variables.put("objectType", context.getObjectType());
        variables.put("objectId", context.getObjectId());
        variables.put("objectVersion", context.getObjectVersion());
        variables.put("actionCode", context.getActionCode());
        variables.put("objectState", context.getObjectState());
        variables.put("deptCode", context.getDeptCode());
        variables.put("orgCode", context.getOrgCode());
        variables.put("roleCodes", context.getRoleCodes());
        variables.put("productCode", context.getProductCode());
        variables.put("categoryCode", context.getCategoryCode());
        variables.put("reason", context.getReason());
        return variables;
    }

    private void recordSnapshot(FormActionInstanceDO instance, FormSnapshotType type, Map<String, Object> formData,
            List<String> attachmentIds) {
        Long count = snapshotMapper.selectCountByInstanceId(instance.getTenantId(), instance.getId());
        snapshotMapper.insert(FormActionSnapshotDO.builder()
                .tenantId(instance.getTenantId())
                .instanceId(instance.getId())
                .snapshotType(type.name())
                .snapshotVersion(count.intValue() + 1)
                .formDataJson(toJson(formData))
                .businessContextJson(instance.getBusinessContextJson())
                .attachmentIdsJson(toJson(attachmentIds))
                .build());
    }

    private FormInstanceRespVO applyDirectEffect(FormActionInstanceDO instance) {
        instance.setStatus(FormInstanceStatus.IN_APPROVAL.name());
        instanceMapper.updateById(instance);
        return toInstanceRespVO(executeEffect(instance, false), instance);
    }

    private FormEffectExecutionDO executeEffect(FormActionInstanceDO instance, boolean retry) {
        FormEffectExecutionDO existing = effectExecutionMapper.selectByInstanceIdAndIdempotencyKey(instance.getTenantId(),
                instance.getId(), instance.getIdempotencyKey());
        if (existing != null && FormEffectStatus.APPLIED.name().equals(existing.getStatus())) {
            return existing;
        }
        if (existing != null && !retry) {
            return existing;
        }
        FormActionPolicyDO policy = requirePolicy(instance.getPolicyId());
        FormActionInstance runtimeInstance = toRuntimeInstance(instance, policy);
        FormBusinessEffectPrecheck precheck = preflightLifecycle(runtimeInstance);
        FormBusinessEffectResult result = precheck.isPassed()
                ? requireEffectExecutor(policy.getEffectExecutorCode()).execute(runtimeInstance, instance.getIdempotencyKey())
                : FormBusinessEffectResult.failure(precheck.getFailureReason());
        FormEffectExecutionDO execution = existing == null ? FormEffectExecutionDO.builder()
                .tenantId(instance.getTenantId())
                .instanceId(instance.getId())
                .executionCode("EFFECT-" + instance.getIdempotencyKey())
                .idempotencyKey(instance.getIdempotencyKey())
                .build() : existing;
        if (result.isSuccess()) {
            execution.setStatus(FormEffectStatus.APPLIED.name());
            execution.setResultRef(result.getResultRef());
            execution.setFailureReason(null);
            instance.setStatus(FormInstanceStatus.EFFECTIVE.name());
        } else {
            execution.setStatus(FormEffectStatus.FAILED_PENDING.name());
            execution.setResultRef(null);
            execution.setFailureReason(result.getFailureReason());
            instance.setStatus(FormInstanceStatus.EFFECT_FAILED_PENDING.name());
        }
        if (existing == null) {
            effectExecutionMapper.insert(execution);
        } else {
            effectExecutionMapper.updateById(execution);
        }
        instanceMapper.updateById(instance);
        return execution;
    }

    private FormBusinessEffectPrecheck preflightLifecycle(FormActionInstance instance) {
        for (FormControlledActionLifecycleAdapter adapter : lifecycleAdapters()) {
            if (adapter.supports(instance)) {
                return adapter.preflight(instance);
            }
        }
        return FormBusinessEffectPrecheck.pass();
    }

    private void notifyPendingApprovalStarted(FormActionInstance instance) {
        for (FormControlledActionLifecycleAdapter adapter : lifecycleAdapters()) {
            if (adapter.supports(instance)) {
                adapter.onPendingApprovalStarted(instance);
            }
        }
    }

    private void notifyPendingApprovalClosed(FormActionInstanceDO instance, FormControlledActionApprovalOutcome outcome,
            String reason) {
        FormActionPolicyDO policy = requirePolicy(instance.getPolicyId());
        FormActionInstance runtimeInstance = toRuntimeInstance(instance, policy);
        for (FormControlledActionLifecycleAdapter adapter : lifecycleAdapters()) {
            if (adapter.supports(runtimeInstance)) {
                adapter.onPendingApprovalClosed(runtimeInstance, outcome, reason);
            }
        }
    }


    private List<FormControlledActionLifecycleAdapter> lifecycleAdapters() {
        return lifecycleAdapterProvider == null ? List.of() : lifecycleAdapterProvider.orderedStream().toList();
    }
    private FormBusinessEffectExecutor requireEffectExecutor(String executorCode) {
        return effectExecutorProvider.orderedStream()
                .filter(executor -> Objects.equals(executor.getExecutorCode(), executorCode))
                .findFirst()
                .orElseThrow(() -> new FormCenterException(FormCenterErrorCode.EFFECT_EXECUTOR_MISSING,
                        "Form effect executor missing: " + executorCode));
    }

    private FormActionPolicyDO requirePublishedPolicy(BusinessActionContext context) {
        List<FormActionPolicyDO> policies = policyMapper.selectPublishedByAction(context.getTenantId(),
                context.getDataDomain(), context.getSystemCode(), context.getObjectType(), context.getActionCode(),
                context.getObjectState());
        if (policies.isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "No published form policy matched action " + context.getActionCode());
        }
        if (policies.size() > 1) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_CONFLICT,
                    "More than one published form policy matched action " + context.getActionCode());
        }
        FormActionPolicyDO policy = policies.get(0);
        for (FormPolicySlot slot : parsePolicySlots(policy.getSlotsJson())) {
            requireLatestPublishedTemplateVersion(context.getTenantId(), slot);
        }
        return policy;
    }

    private FormActionResolution toResolution(FormActionPolicyDO policy) {
        return FormActionResolution.from(FormActionPolicy.builder()
                .policyId(policy.getId())
                .tenantId(policy.getTenantId())
                .dataDomain(policy.getDataDomain())
                .systemCode(policy.getSystemCode())
                .objectType(policy.getObjectType())
                .actionCode(policy.getActionCode())
                .objectState(policy.getObjectState())
                .policyType(FormPolicyType.valueOf(policy.getPolicyType()))
                .approvalMode(resolveApprovalMode(policy.getApprovalMode()))
                .bpmProcessKey(policy.getBpmProcessKey())
                .effectExecutorCode(policy.getEffectExecutorCode())
                .status(policy.getStatus())
                .slots(parsePolicySlots(policy.getSlotsJson()))
                .build());
    }

    private FormActionInstance toRuntimeInstance(FormActionInstanceDO instance, FormActionPolicyDO policy) {
        FormActionInstance runtime = new FormActionInstance(instance.getInstanceCode(), toResolution(policy),
                toContext(instance), instance.getApplicantUserId(), instance.getIdempotencyKey());
        runtime.setStatus(FormInstanceStatus.valueOf(instance.getStatus()));
        runtime.setFormData(parseMap(instance.getFormDataJson()));
        if (!isBlank(instance.getBpmProcessInstanceId())) {
            runtime.setBpmBinding(new FormBpmBinding(instance.getBpmProcessInstanceId(), null));
        }
        return runtime;
    }

    private BusinessActionContext toContext(BusinessActionContextReqVO reqVO) {
        Long tenantId = currentTenantIdIfAbsent(reqVO.getTenantId());
        return BusinessActionContext.builder()
                .tenantId(tenantId)
                .dataDomain(reqVO.getDataDomain())
                .systemCode(reqVO.getSystemCode())
                .objectType(reqVO.getObjectType())
                .objectId(reqVO.getObjectId())
                .objectVersion(reqVO.getObjectVersion())
                .actionCode(reqVO.getActionCode())
                .objectState(reqVO.getObjectState())
                .orgCode(reqVO.getOrgCode())
                .deptCode(reqVO.getDeptCode())
                .roleCodes(reqVO.getRoleCodes())
                .productCode(reqVO.getProductCode())
                .categoryCode(reqVO.getCategoryCode())
                .reason(reqVO.getReason())
                .build();
    }

    private BusinessActionContext toContext(FormActionInstanceDO instance) {
        BusinessActionContext context = JsonUtils.parseObject(instance.getBusinessContextJson(),
                BusinessActionContext.class);
        if (context != null) {
            return context;
        }
        return BusinessActionContext.builder()
                .tenantId(instance.getTenantId())
                .dataDomain(instance.getDataDomain())
                .systemCode(instance.getSystemCode())
                .objectType(instance.getObjectType())
                .objectId(instance.getObjectId())
                .objectVersion(instance.getObjectVersion())
                .actionCode(instance.getActionCode())
                .objectState(instance.getObjectState())
                .build();
    }

    private BusinessActionContextReqVO toContextReqVO(BusinessActionContext context) {
        BusinessActionContextReqVO reqVO = new BusinessActionContextReqVO();
        reqVO.setTenantId(context.getTenantId());
        reqVO.setDataDomain(context.getDataDomain());
        reqVO.setSystemCode(context.getSystemCode());
        reqVO.setObjectType(context.getObjectType());
        reqVO.setObjectId(context.getObjectId());
        reqVO.setObjectVersion(context.getObjectVersion());
        reqVO.setActionCode(context.getActionCode());
        reqVO.setObjectState(context.getObjectState());
        reqVO.setOrgCode(context.getOrgCode());
        reqVO.setDeptCode(context.getDeptCode());
        reqVO.setRoleCodes(context.getRoleCodes());
        reqVO.setProductCode(context.getProductCode());
        reqVO.setCategoryCode(context.getCategoryCode());
        reqVO.setReason(context.getReason());
        return reqVO;
    }

    private FormActionResolutionRespVO toResolutionRespVO(FormActionResolution resolution) {
        FormActionResolutionRespVO respVO = new FormActionResolutionRespVO();
        respVO.setPolicyId(resolution.getPolicyId());
        respVO.setPolicyType(resolution.getPolicyType().name());
        respVO.setApprovalMode(resolution.getApprovalMode().name());
        respVO.setRequiresForm(resolution.requiresForm());
        respVO.setRequiresBpm(resolution.requiresBpm());
        respVO.setBpmProcessKey(resolution.getBpmProcessKey());
        respVO.setSlots(resolution.getSlots());
        return respVO;
    }

    private FormPolicyRespVO toPolicyRespVO(FormActionPolicyDO policy) {
        FormPolicyRespVO respVO = new FormPolicyRespVO();
        respVO.setId(policy.getId());
        respVO.setDataDomain(policy.getDataDomain());
        respVO.setSystemCode(policy.getSystemCode());
        respVO.setObjectType(policy.getObjectType());
        respVO.setActionCode(policy.getActionCode());
        respVO.setObjectState(policy.getObjectState());
        respVO.setPolicyType(policy.getPolicyType());
        respVO.setApprovalMode(policy.getApprovalMode());
        respVO.setBpmProcessKey(policy.getBpmProcessKey());
        respVO.setEffectExecutorCode(policy.getEffectExecutorCode());
        respVO.setStatus(policy.getStatus());
        respVO.setSlots(parsePolicySlots(policy.getSlotsJson()));
        respVO.setRemark(policy.getRemark());
        respVO.setUpdatedTime(policy.getUpdateTime());
        return respVO;
    }

    private FormCenterTemplateRespVO toTemplateRespVO(FormTemplateVersionDO version) {
        FormCenterTemplateRespVO respVO = new FormCenterTemplateRespVO();
        respVO.setTemplateId(version.getTemplateId());
        respVO.setTemplateName(version.getTemplateName());
        respVO.setVersionNo(version.getVersionNo());
        respVO.setStatus(version.getStatus());
        respVO.setUpdatedTime(version.getUpdateTime());
        respVO.setRemark(version.getRemark());
        respVO.setRecognizedFields(JsonUtils.parseArray(version.getRecognizedSchemaJson(), FormRecognizedField.class));
        respVO.setJimuSchemaJson(version.getJimuSchemaJson());
        respVO.setSourceFileName(version.getSourceFileName());
        return respVO;
    }

    private FormInstanceRespVO toInstanceRespVO(FormActionInstanceDO instance) {
        FormInstanceRespVO respVO = new FormInstanceRespVO();
        respVO.setId(instance.getId());
        respVO.setInstanceCode(instance.getInstanceCode());
        respVO.setStatus(instance.getStatus());
        respVO.setBpmProcessInstanceId(instance.getBpmProcessInstanceId());
        respVO.setContext(toContextReqVO(toContext(instance)));
        return respVO;
    }

    private FormInstanceRespVO toInstanceRespVO(FormEffectExecutionDO execution, FormActionInstanceDO instance) {
        return toInstanceRespVO(instance);
    }

    private FormInstanceSnapshotRespVO toSnapshotRespVO(FormActionSnapshotDO snapshot) {
        FormInstanceSnapshotRespVO respVO = new FormInstanceSnapshotRespVO();
        respVO.setId(snapshot.getId());
        respVO.setInstanceId(snapshot.getInstanceId());
        respVO.setSnapshotType(snapshot.getSnapshotType());
        respVO.setSnapshotVersion(snapshot.getSnapshotVersion());
        respVO.setFormData(parseMap(snapshot.getFormDataJson()));
        respVO.setContext(toContextReqVO(JsonUtils.parseObject(snapshot.getBusinessContextJson(),
                BusinessActionContext.class)));
        respVO.setAttachmentIds(parseStringList(snapshot.getAttachmentIdsJson()));
        respVO.setCreatedTime(snapshot.getCreateTime());
        return respVO;
    }

    private FormEffectExecutionRespVO toEffectRespVO(FormEffectExecutionDO execution) {
        FormEffectExecutionRespVO respVO = new FormEffectExecutionRespVO();
        respVO.setId(execution.getId());
        respVO.setInstanceId(execution.getInstanceId());
        respVO.setExecutionCode(execution.getExecutionCode());
        respVO.setIdempotencyKey(execution.getIdempotencyKey());
        respVO.setStatus(execution.getStatus());
        respVO.setResultRef(execution.getResultRef());
        respVO.setFailureReason(execution.getFailureReason());
        return respVO;
    }

    private FormActionPolicyDO requirePolicy(Long policyId) {
        FormActionPolicyDO policy = policyMapper.selectById(policyId);
        if (policy == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Form policy not found: " + policyId);
        }
        return policy;
    }

    private FormTemplateVersionDO requireTemplateVersion(Long templateId, String versionNo) {
        FormTemplateVersionDO version = templateVersionMapper.selectByTemplateIdAndVersionNo(templateId, versionNo);
        if (version == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Template version not found: " + templateId + "/" + versionNo);
        }
        return version;
    }

    private FormActionInstanceDO requireInstance(Long instanceId) {
        FormActionInstanceDO instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_INSTANCE_STATUS_INVALID,
                    "Form instance not found: " + instanceId);
        }
        return instance;
    }

    private FormActionInstanceDO requireInstanceByProcess(String processInstanceId) {
        FormActionInstanceDO instance = instanceMapper.selectByProcessInstanceId(TenantContextHolder.getRequiredTenantId(),
                processInstanceId);
        if (instance == null) {
            throw new FormCenterException(FormCenterErrorCode.BPM_CALLBACK_STALE,
                    "Form instance not found for BPM process: " + processInstanceId);
        }
        return instance;
    }

    private void requireOwner(FormActionInstanceDO instance, Long userId) {
        if (!Objects.equals(instance.getApplicantUserId(), userId)) {
            throw new FormCenterException(FormCenterErrorCode.BPM_TASK_PERMISSION_MISSING,
                    "Current user cannot modify form instance: " + instance.getId());
        }
    }

    private void requireStatus(FormActionInstanceDO instance, FormInstanceStatus... statuses) {
        for (FormInstanceStatus status : statuses) {
            if (status.name().equals(instance.getStatus())) {
                return;
            }
        }
        throw new FormCenterException(FormCenterErrorCode.FORM_INSTANCE_STATUS_INVALID,
                "Form instance status invalid: " + instance.getStatus());
    }

    private void updateTemplateStatus(Long templateId, String versionNo, FormTemplateStatus status) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        version.setStatus(status.name());
        templateVersionMapper.updateById(version);
    }

    private Long resolveImportTemplateId(Long tenantId, FormCenterTemplateImportReqVO reqVO) {
        if (reqVO.getSelectedTemplateId() != null) {
            return reqVO.getSelectedTemplateId();
        }
        FormTemplateVersionDO latest = templateVersionMapper.selectLatestByTemplateName(tenantId, reqVO.getTemplateName());
        return latest == null ? null : latest.getTemplateId();
    }

    private String nextVersionNo(Long tenantId, Long templateId, String templateName) {
        FormTemplateVersionDO latest = templateId == null
                ? templateVersionMapper.selectLatestByTemplateName(tenantId, templateName)
                : templateVersionMapper.selectLatestByTemplateId(tenantId, templateId);
        if (latest == null || isBlank(latest.getVersionNo())) {
            return "V1";
        }
        String versionNo = latest.getVersionNo().replace("v", "V");
        if (versionNo.startsWith("V")) {
            return "V" + (Long.parseLong(versionNo.substring(1)) + 1);
        }
        return versionNo + "-NEXT";
    }

    private String readFileContent(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template source file cannot be read: " + file.getOriginalFilename());
        }
    }

    private BusinessActionContextReqVO buildTemplateContext(FormTemplateVersionDO version, String actionCode,
            String reason) {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setTenantId(version.getTenantId());
        context.setDataDomain("BPM");
        context.setSystemCode("BPM");
        context.setObjectType("FORM_TEMPLATE");
        context.setObjectId(String.valueOf(version.getTemplateId()));
        context.setObjectVersion(version.getVersionNo());
        context.setActionCode(actionCode);
        context.setObjectState(version.getStatus());
        context.setReason(reason);
        return context;
    }

    private FormApprovalMode resolveApprovalMode(String approvalMode) {
        return isBlank(approvalMode) ? FormApprovalMode.BPM_REQUIRED : FormApprovalMode.valueOf(approvalMode);
    }

    private Long currentTenantIdIfAbsent(Long tenantId) {
        return tenantId == null ? TenantContextHolder.getRequiredTenantId() : tenantId;
    }

    private List<FormPolicySlot> parsePolicySlots(String json) {
        List<FormPolicySlot> slots = JsonUtils.parseObject(json, POLICY_SLOT_LIST);
        return slots == null ? List.of() : slots;
    }

    private Map<String, Object> parseMap(String json) {
        Map<String, Object> map = JsonUtils.parseObject(json, MAP_TYPE);
        return map == null ? Map.of() : map;
    }

    private List<String> parseStringList(String json) {
        List<String> values = JsonUtils.parseObject(json, STRING_LIST);
        return values == null ? List.of() : values;
    }

    private List<String> parseAttachmentIds(String attachmentIds) {
        if (isBlank(attachmentIds)) {
            return List.of();
        }
        if (attachmentIds.trim().startsWith("[")) {
            return parseStringList(attachmentIds);
        }
        return Arrays.stream(attachmentIds.split(",")).map(String::trim).filter(text -> !text.isEmpty()).toList();
    }

    private String toJson(Object value) {
        return JsonUtils.toJsonString(value == null ? Map.of() : value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
