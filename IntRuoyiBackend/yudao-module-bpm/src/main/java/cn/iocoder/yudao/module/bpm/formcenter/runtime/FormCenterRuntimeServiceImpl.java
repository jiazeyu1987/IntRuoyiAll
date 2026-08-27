package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalRequestStore;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionSnapshotDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormEffectExecutionDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTaskPermissionDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionSnapshotMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormEffectExecutionMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTaskPermissionMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.formcenter.model.*;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormActionPolicyResolveService;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FormCenterRuntimeServiceImpl implements FormCenterRuntimeService {

    private static final String BUSINESS_KEY_PREFIX = "FORM_ACTION:";
    private static final String TEMPLATE_IMPORT_ACTION_CREATE = "CREATE";
    private static final String TEMPLATE_IMPORT_ACTION_UPGRADE = "UPGRADE";
    private static final String TEMPLATE_ACTION_OBSOLETE = "OBSOLETE";
    private static final String FORM_TEMPLATE_APPROVAL_DATA_DOMAIN = "FORM_CENTER";
    private static final String FORM_TEMPLATE_APPROVAL_SYSTEM_CODE = "FORM_CENTER";
    private static final String FORM_TEMPLATE_APPROVAL_OBJECT_TYPE = "FORM_TEMPLATE";
    private static final Pattern AUTO_VERSION_PATTERN = Pattern.compile("^([Vv]?)(\\d+)((?:\\.\\d+)*)$");

    @Resource
    private FormTemplateVersionMapper templateVersionMapper;
    @Resource
    private FormActionPolicyMapper actionPolicyMapper;
    @Resource
    private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;
    @Resource
    private FormActionInstanceMapper actionInstanceMapper;
    @Resource
    private FormActionSnapshotMapper actionSnapshotMapper;
    @Resource
    private FormTaskPermissionMapper taskPermissionMapper;
    @Resource
    private FormEffectExecutionMapper effectExecutionMapper;
    @Resource
    private FormTemplateRecognizer templateRecognizer;
    @Resource
    private FormTemplateFillRuleAutoDetectService formTemplateFillRuleAutoDetectService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private TaskService flowableTaskService;
    @Autowired(required = false)
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @Autowired(required = false)
    private BusinessApprovalRequestStore businessApprovalRequestStore;
    @Autowired(required = false)
    private List<FormControlledActionLifecycleAdapter> lifecycleAdapters = List.of();
    @Autowired(required = false)
    private List<FormBusinessEffectExecutor> effectExecutors = List.of();

    @Override
    public PageResult<FormCenterTemplateRespVO> getTemplatePool(FormCenterTemplatePoolPageReqVO reqVO) {
        reqVO.setTenantId(resolveTenantId(reqVO.getTenantId()));
        PageResult<FormTemplateVersionDO> pageResult = templateVersionMapper.selectPage(reqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::toTemplateResp).toList(),
                pageResult.getTotal());
    }

    @Override
    public FormCenterTemplateRespVO getTemplateVersion(Long templateId, String versionNo) {
        return toTemplateResp(requireCurrentTenantTemplateVersion(templateId, versionNo));
    }

    @Override
    public PageResult<FormPolicyRespVO> getPolicyPage(FormPolicyPageReqVO reqVO) {
        reqVO.setTenantId(resolveTenantId(reqVO.getTenantId()));
        PageResult<FormActionPolicyDO> pageResult = actionPolicyMapper.selectPage(reqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::toPolicyResp).toList(),
                pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormCenterTemplateImportRespVO importDoc(FormCenterTemplateImportReqVO reqVO, Long applicantUserId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String templateName = normalizeTemplateName(reqVO.getTemplateName());
        FormTemplateVersionDO latestExisting = resolveImportTargetTemplate(tenantId, templateName,
                reqVO.getSelectedTemplateId());
        boolean upgradeImport = latestExisting != null;
        String versionNo = upgradeImport ? nextVersionNo(latestExisting.getVersionNo()) : "V1.0";
        byte[] sourceBytes = readSourceBytes(reqVO);
        FormTemplateImportCommand command = FormTemplateImportCommand.of(templateName, versionNo,
                reqVO.getFile().getOriginalFilename(), sourceBytes, reqVO.getRemark());
        validateDocSource(command);
        FormTemplateRecognition recognition = templateRecognizer.recognize(command);
        if (!recognition.isSuccess() || recognition.getFields().isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_RECOGNITION_FAILED,
                    "Template recognition failed: " + recognition.getFailureReason());
        }
        FormTemplateVersionDO insertObj = FormTemplateVersionDO.builder()
                .tenantId(tenantId)
                .templateId(upgradeImport ? latestExisting.getTemplateId() : null)
                .templateName(templateName)
                .versionNo(versionNo)
                .status(FormTemplateStatus.DRAFT.name())
                .sourceFileName(command.getSourceFileName())
                .sourceFileContent(Base64.getEncoder().encodeToString(sourceBytes))
                .recognizedSchemaJson(JsonUtils.toJsonString(recognition.getFields()))
                .jimuSchemaJson(recognition.getJimuSchemaJson())
                .remark(reqVO.getRemark())
                .build();
        templateVersionMapper.insert(insertObj);
        if (!upgradeImport) {
            insertObj.setTemplateId(insertObj.getId());
            templateVersionMapper.updateById(insertObj);
        }

        FormCenterTemplateImportRespVO respVO = new FormCenterTemplateImportRespVO();
        respVO.setTemplateId(insertObj.getTemplateId());
        respVO.setVersionNo(insertObj.getVersionNo());
        respVO.setStatus(insertObj.getStatus());
        respVO.setImportAction(upgradeImport ? TEMPLATE_IMPORT_ACTION_UPGRADE : TEMPLATE_IMPORT_ACTION_CREATE);
        respVO.setSourceTemplateId(upgradeImport ? latestExisting.getTemplateId() : null);
        if (upgradeImport) {
            BusinessApprovalRequest approvalRequest = submitTemplateUpgradeApproval(tenantId, insertObj,
                    applicantUserId, reqVO.getRemark());
            respVO.setApprovalRequestId(approvalRequest.getRequestId());
            respVO.setApprovalProcessInstanceId(approvalRequest.getProcessInstanceId());
            if (approvalRequest.getResultState() != null && !approvalRequest.getResultState().isBlank()) {
                respVO.setStatus(approvalRequest.getResultState());
            }
        }
        respVO.setRecognizedFields(recognition.getFields());
        respVO.setWarnings(List.of());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormPolicyRespVO savePolicy(FormPolicySaveReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        FormPolicyType policyType = parsePolicyType(reqVO.getPolicyType());
        List<FormPolicySlot> slots = reqVO.getSlots().stream().map(this::toPolicySlot).toList();
        validatePolicySlots(policyType, slots);

        FormActionPolicyDO insertObj = FormActionPolicyDO.builder()
                .tenantId(tenantId)
                .dataDomain(reqVO.getDataDomain())
                .systemCode(reqVO.getSystemCode())
                .objectType(reqVO.getObjectType())
                .actionCode(reqVO.getActionCode())
                .objectState(reqVO.getObjectState())
                .policyType(policyType.name())
                .approvalMode(parseApprovalModeOrDefault(reqVO.getApprovalMode()).name())
                .bpmProcessKey(reqVO.getBpmProcessKey())
                .effectExecutorCode(reqVO.getEffectExecutorCode())
                .status("DRAFT")
                .slotsJson(JsonUtils.toJsonString(slots))
                .remark(reqVO.getRemark())
                .build();
        actionPolicyMapper.insert(insertObj);
        return toPolicyResp(insertObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishPolicy(Long policyId) {
        FormActionPolicyDO policy = requirePolicy(policyId);
        FormApprovalMode approvalMode = parseApprovalModeOrDefault(policy.getApprovalMode());
        if (approvalMode == FormApprovalMode.BPM_REQUIRED
                && (policy.getBpmProcessKey() == null || policy.getBpmProcessKey().isBlank())) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "BPM process key is required before publishing form policy");
        }
        if (policy.getEffectExecutorCode() == null || policy.getEffectExecutorCode().isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.EFFECT_EXECUTOR_MISSING,
                    "Effect executor code is required before publishing form policy");
        }
        List<FormPolicySlot> slots = parsePolicySlots(policy.getSlotsJson());
        validatePolicySlots(parsePolicyType(policy.getPolicyType()), slots);
        for (FormPolicySlot slot : slots) {
            FormTemplateVersionDO version = requireLatestPublishedTemplateVersion(policy.getTenantId(), slot);
            if (!FormTemplateStatus.PUBLISHED.name().equals(version.getStatus())) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                        "Only published template versions can be bound to published form policy: "
                                + version.getTemplateId() + "/" + version.getVersionNo());
            }
        }
        policy.setStatus(FormActionPolicy.STATUS_PUBLISHED);
        actionPolicyMapper.updateById(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormPolicyRespVO switchPolicyApprovalMode(Long policyId, FormPolicySwitchApprovalModeReqVO reqVO) {
        FormActionPolicyDO sourcePolicy = requirePolicy(policyId);
        if (!FormActionPolicy.STATUS_PUBLISHED.equals(sourcePolicy.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Only published form policy can switch approval mode: " + policyId);
        }
        FormApprovalMode targetMode = parseApprovalMode(reqVO.getApprovalMode());
        String targetProcessKey = resolveSwitchBpmProcessKey(sourcePolicy, targetMode, reqVO.getBpmProcessKey());
        if (sourcePolicy.getEffectExecutorCode() == null || sourcePolicy.getEffectExecutorCode().isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.EFFECT_EXECUTOR_MISSING,
                    "Effect executor code is required before switching form policy approval mode");
        }
        List<FormPolicySlot> slots = parsePolicySlots(sourcePolicy.getSlotsJson());
        validatePolicySlots(parsePolicyType(sourcePolicy.getPolicyType()), slots);

        sourcePolicy.setStatus("DISABLED");
        actionPolicyMapper.updateById(sourcePolicy);

        FormActionPolicyDO targetPolicy = FormActionPolicyDO.builder()
                .tenantId(sourcePolicy.getTenantId())
                .dataDomain(sourcePolicy.getDataDomain())
                .systemCode(sourcePolicy.getSystemCode())
                .objectType(sourcePolicy.getObjectType())
                .actionCode(sourcePolicy.getActionCode())
                .objectState(sourcePolicy.getObjectState())
                .policyType(sourcePolicy.getPolicyType())
                .approvalMode(targetMode.name())
                .bpmProcessKey(targetProcessKey)
                .effectExecutorCode(sourcePolicy.getEffectExecutorCode())
                .status(FormActionPolicy.STATUS_PUBLISHED)
                .slotsJson(sourcePolicy.getSlotsJson())
                .remark(sourcePolicy.getRemark())
                .build();
        actionPolicyMapper.insert(targetPolicy);
        return toPolicyResp(targetPolicy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveJimuSchema(Long templateId, String versionNo, FormCenterTemplateJimuSchemaReqVO reqVO) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        if (!FormTemplateStatus.DRAFT.name().equals(version.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Only draft template versions can be adjusted: " + templateId + "/" + versionNo);
        }
        version.setJimuSchemaJson(reqVO.getJimuSchema());
        templateVersionMapper.updateById(version);
    }

    @Override
    public FormTemplateFillRuleAutoDetectRespVO autoDetectTemplateFillRules(Long templateId, String versionNo) {
        return formTemplateFillRuleAutoDetectService.detect(templateId, versionNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishTemplate(Long templateId, String versionNo) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        if (FormTemplateStatus.OBSOLETE.name().equals(version.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Obsolete template versions cannot be published: " + templateId + "/" + versionNo);
        }
        if (FormTemplateStatus.PENDING_APPROVAL.name().equals(version.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Template versions pending approval cannot be published directly: " + templateId + "/" + versionNo);
        }
        version.setStatus(FormTemplateStatus.PUBLISHED.name());
        templateVersionMapper.updateById(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableTemplate(Long templateId, String versionNo) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        if (FormTemplateStatus.OBSOLETE.name().equals(version.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Obsolete template versions cannot be disabled: " + templateId + "/" + versionNo);
        }
        if (FormTemplateStatus.PENDING_APPROVAL.name().equals(version.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Template versions pending approval cannot be disabled directly: " + templateId + "/" + versionNo);
        }
        version.setStatus(FormTemplateStatus.DISABLED.name());
        templateVersionMapper.updateById(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableTemplate(Long templateId, String versionNo) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        if (!FormTemplateStatus.DISABLED.name().equals(version.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Only disabled template versions can be enabled: " + templateId + "/" + versionNo);
        }
        version.setStatus(FormTemplateStatus.PUBLISHED.name());
        templateVersionMapper.updateById(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void obsoleteTemplate(Long templateId, String versionNo) {
        requireCurrentTenantTemplateVersion(templateId, versionNo);
        throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                "Template obsolete requires BPM approval request: " + templateId + "/" + versionNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormTemplateObsoleteRespVO submitTemplateObsoleteRequest(Long templateId, String versionNo,
            FormTemplateObsoleteReqVO reqVO, Long applicantUserId) {
        FormTemplateVersionDO version = requireCurrentTenantTemplateVersion(templateId, versionNo);
        if (FormTemplateStatus.PENDING_APPROVAL.name().equals(version.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Template versions pending approval cannot submit duplicate obsolete request: "
                            + templateId + "/" + versionNo);
        }
        if (FormTemplateStatus.OBSOLETE.name().equals(version.getStatus())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Obsolete template versions cannot submit obsolete request: " + templateId + "/" + versionNo);
        }
        BusinessApprovalRequest approvalRequest = submitTemplateObsoleteApproval(version, applicantUserId,
                normalizeObsoleteReason(reqVO.getReason()), reqVO.getStartUserSelectAssignees());
        FormTemplateObsoleteRespVO respVO = new FormTemplateObsoleteRespVO();
        respVO.setApprovalRequestId(approvalRequest.getRequestId());
        respVO.setApprovalProcessInstanceId(approvalRequest.getProcessInstanceId());
        respVO.setStatus(approvalRequest.getResultState() == null || approvalRequest.getResultState().isBlank()
                ? FormTemplateStatus.PENDING_APPROVAL.name() : approvalRequest.getResultState());
        return respVO;
    }

    @Override
    public FormTemplateObsoletePendingRespVO findTemplateObsoletePendingRequest(Long templateId, String versionNo,
            Long currentUserId) {
        FormTemplateVersionDO version = requireCurrentTenantTemplateVersion(templateId, versionNo);
        return findTemplateObsoletePendingRequest(version, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawTemplateObsoleteRequest(Long templateId, String versionNo, String reason, Long currentUserId) {
        FormTemplateVersionDO version = requireCurrentTenantTemplateVersion(templateId, versionNo);
        BusinessApprovalRequest request = requireTemplateObsoletePendingRequest(version);
        if (!Objects.equals(request.getContext().getApplicantUserId(), currentUserId)) {
            throw new FormCenterException(FormCenterErrorCode.BPM_TASK_PERMISSION_MISSING,
                    "Only obsolete request applicant can withdraw: " + request.getRequestId());
        }
        if (request.getProcessInstanceId() == null || request.getProcessInstanceId().isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "Obsolete request process instance is missing: " + request.getRequestId());
        }
        processInstanceApi.cancelProcessInstance(currentUserId, request.getProcessInstanceId(),
                normalizeWithdrawReason(reason));
    }

    @Override
    public byte[] getTemplateSourceFile(Long templateId, String versionNo) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        if (version.getSourceFileContent() == null) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template source file not found: " + templateId + "/" + versionNo);
        }
        return Base64.getDecoder().decode(version.getSourceFileContent());
    }

    @Override
    public FormActionResolutionRespVO resolveAction(BusinessActionContextReqVO reqVO) {
        reqVO.setTenantId(resolveTenantId(reqVO.getTenantId()));
        BusinessActionContext context = toContext(reqVO);
        List<FormActionPolicy> policies = businessApprovalPolicyMapper.selectPublishedByAction(reqVO.getTenantId(),
                        reqVO.getDataDomain(), reqVO.getSystemCode(), reqVO.getObjectType(), reqVO.getActionCode(),
                        reqVO.getObjectState())
                .stream().map(this::toPolicy).toList();
        FormActionResolution resolution = new FormActionPolicyResolveService(policies).resolve(context);
        return toResolutionResp(resolution);
    }

    @Override
    public FormInstanceRespVO findActiveBusinessAction(BusinessActionContextReqVO reqVO) {
        reqVO.setTenantId(resolveTenantId(reqVO.getTenantId()));
        FormActionInstanceDO active = actionInstanceMapper.selectActiveByBusinessObject(reqVO.getTenantId(),
                reqVO.getSystemCode(), reqVO.getObjectType(), reqVO.getObjectId());
        return active == null ? null : toInstanceResp(active);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormInstanceRespVO createInstance(FormInstanceCreateReqVO reqVO, Long userId) {
        reqVO.getContext().setTenantId(resolveTenantId(reqVO.getContext().getTenantId()));
        FormActionResolutionRespVO resolution = resolveAction(reqVO.getContext());
        List<FormActionInstanceDO> sameInstances = actionInstanceMapper.selectSameBusinessAction(
                reqVO.getContext().getTenantId(), reqVO.getContext().getSystemCode(), reqVO.getContext().getObjectType(),
                reqVO.getContext().getObjectId(), reqVO.getContext().getObjectVersion(), reqVO.getContext().getActionCode());
        for (FormActionInstanceDO sameInstance : sameInstances) {
            if (sameInstance.getApplicantUserId().equals(userId) && FormInstanceStatus.DRAFT.name().equals(sameInstance.getStatus())) {
                return toInstanceResp(sameInstance);
            }
            if (FormInstanceStatus.IN_APPROVAL.name().equals(sameInstance.getStatus())
                    || FormInstanceStatus.REWORKING.name().equals(sameInstance.getStatus())) {
                throw new FormCenterException(FormCenterErrorCode.DUPLICATE_APPLICATION_ACTIVE,
                        "Active duplicate form action instance exists: " + sameInstance.getInstanceCode());
            }
        }
        FormActionInstanceDO insertObj = FormActionInstanceDO.builder()
                .instanceCode("FCI-" + reqVO.getContext().getTenantId() + "-" + System.currentTimeMillis())
                .tenantId(reqVO.getContext().getTenantId())
                .policyId(resolution.getPolicyId())
                .applicantUserId(userId)
                .status(FormInstanceStatus.DRAFT.name())
                .dataDomain(reqVO.getContext().getDataDomain())
                .systemCode(reqVO.getContext().getSystemCode())
                .objectType(reqVO.getContext().getObjectType())
                .objectId(reqVO.getContext().getObjectId())
                .objectVersion(reqVO.getContext().getObjectVersion())
                .actionCode(reqVO.getContext().getActionCode())
                .objectState(reqVO.getContext().getObjectState())
                .idempotencyKey(reqVO.getIdempotencyKey())
                .businessContextJson(JsonUtils.toJsonString(reqVO.getContext()))
                .formDataJson(JsonUtils.toJsonString(reqVO.getFormData()))
                .build();
        actionInstanceMapper.insert(insertObj);
        recordSnapshot(insertObj, FormSnapshotType.DRAFT, insertObj.getFormDataJson());
        return toInstanceResp(insertObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(Long instanceId, FormInstanceDraftReqVO reqVO, Long userId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING);
        instance.setFormDataJson(JsonUtils.toJsonString(reqVO.getFormData()));
        actionInstanceMapper.updateById(instance);
        recordSnapshot(instance, FormSnapshotType.DRAFT, instance.getFormDataJson());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormInstanceRespVO submitInstance(Long instanceId, FormInstanceSubmitReqVO reqVO, Long userId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING);
        BusinessApprovalPolicyDO policy = requireBusinessApprovalPolicy(instance.getPolicyId());
        FormActionPolicy resolvedPolicy = toPolicy(policy);
        FormApprovalMode approvalMode = resolvedPolicy.getApprovalMode();
        if (approvalMode == FormApprovalMode.BPM_REQUIRED
                && (resolvedPolicy.getBpmProcessKey() == null || resolvedPolicy.getBpmProcessKey().isBlank())) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "BPM process key is required before form action submit");
        }
        FormActionInstance domainInstance = toDomainInstance(instance, resolvedPolicy);
        domainInstance.setFormData(reqVO.getFormData());
        FormControlledActionLifecycleAdapter lifecycleAdapter = runLifecyclePreflight(domainInstance);
        if (approvalMode == FormApprovalMode.DIRECT) {
            instance.setFormDataJson(JsonUtils.toJsonString(reqVO.getFormData()));
            actionInstanceMapper.updateById(instance);
            recordSnapshot(instance, FormSnapshotType.SUBMIT, instance.getFormDataJson());
            FormEffectExecutionRespVO response = applyBusinessEffect(instance, resolvedPolicy);
            FormControlledActionApprovalOutcome outcome = FormEffectStatus.APPLIED.name().equals(response.getStatus())
                    ? FormControlledActionApprovalOutcome.EFFECTIVE
                    : FormControlledActionApprovalOutcome.EFFECT_FAILED_PENDING;
            notifyPendingApprovalClosed(instance, outcome, null, lifecycleAdapter);
            return toInstanceResp(instance);
        }
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                buildBpmRequest(instance, resolvedPolicy, reqVO.getStartUserSelectAssignees()));
        try {
            instance.setStatus(FormInstanceStatus.IN_APPROVAL.name());
            instance.setFormDataJson(JsonUtils.toJsonString(reqVO.getFormData()));
            instance.setBpmProcessInstanceId(processInstanceId);
            actionInstanceMapper.updateById(instance);
            recordSnapshot(instance, FormSnapshotType.SUBMIT, instance.getFormDataJson());
            persistCurrentActiveTaskPermissions(instance);
            domainInstance.setStatus(FormInstanceStatus.IN_APPROVAL);
            domainInstance.setBpmBinding(new FormBpmBinding(processInstanceId, null));
            lifecycleAdapter.onPendingApprovalStarted(domainInstance);
        } catch (RuntimeException submitFailure) {
            cancelCreatedProcessInstance(userId, processInstanceId, instanceId, submitFailure);
            throw submitFailure;
        }
        return toInstanceResp(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reworkSubmitInstance(Long instanceId, FormInstanceSubmitReqVO reqVO, Long userId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireStatus(instance, FormInstanceStatus.REWORKING);
        if (instance.getBpmProcessInstanceId() == null || instance.getBpmProcessInstanceId().isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "Existing BPM process binding is required for rework submit");
        }
        instance.setStatus(FormInstanceStatus.IN_APPROVAL.name());
        instance.setFormDataJson(JsonUtils.toJsonString(reqVO.getFormData()));
        actionInstanceMapper.updateById(instance);
        recordSnapshot(instance, FormSnapshotType.REWORK_SUBMIT, instance.getFormDataJson());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandonInstance(Long instanceId, Long userId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING, FormInstanceStatus.REJECTED);
        instance.setStatus(FormInstanceStatus.ABANDONED.name());
        actionInstanceMapper.updateById(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmTaskCreated(FormBpmTaskCreatedReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcessInstanceId(reqVO.getProcessInstanceId());
        requireStatus(instance, FormInstanceStatus.IN_APPROVAL);
        if (reqVO.getHandlerUserIds() == null || reqVO.getHandlerUserIds().isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.BPM_ACTIVE_TASK_ASSIGNEE_MISSING,
                    "BPM active task assignees are required for derived form permissions: " + reqVO.getTaskId());
        }
        for (Long userId : reqVO.getHandlerUserIds()) {
            FormTaskPermissionDO existing = taskPermissionMapper.selectByTaskIdAndUserId(instance.getTenantId(),
                    instance.getId(), reqVO.getTaskId(), userId);
            if (existing == null) {
                taskPermissionMapper.insert(FormTaskPermissionDO.builder()
                        .tenantId(instance.getTenantId())
                        .instanceId(instance.getId())
                        .bpmProcessInstanceId(instance.getBpmProcessInstanceId())
                        .taskId(reqVO.getTaskId())
                        .userId(userId)
                        .permissionCodesJson(JsonUtils.toJsonString(EnumSet.allOf(FormTaskPermissionCode.class)))
                        .status(FormTaskPermissionDO.STATUS_ACTIVE)
                        .build());
            } else {
                existing.setPermissionCodesJson(JsonUtils.toJsonString(EnumSet.allOf(FormTaskPermissionCode.class)));
                existing.setStatus(FormTaskPermissionDO.STATUS_ACTIVE);
                taskPermissionMapper.updateById(existing);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmTaskCompleted(FormBpmTaskCompletedReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcessInstanceId(reqVO.getProcessInstanceId());
        revokeTaskPermissions(instance, reqVO.getTaskId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmReworkRequired(FormBpmReworkRequiredReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcessInstanceId(reqVO.getProcessInstanceId());
        requireStatus(instance, FormInstanceStatus.IN_APPROVAL);
        revokeAllActiveTaskPermissions(instance);
        instance.setStatus(FormInstanceStatus.REWORKING.name());
        actionInstanceMapper.updateById(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmProcessRejected(FormBpmProcessRejectedReqVO reqVO) {
        closePendingApproval(reqVO.getProcessInstanceId(), FormInstanceStatus.REJECTED,
                FormControlledActionApprovalOutcome.REJECTED, reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onBpmProcessCancelled(FormBpmProcessCancelledReqVO reqVO) {
        closePendingApproval(reqVO.getProcessInstanceId(), FormInstanceStatus.ABANDONED,
                FormControlledActionApprovalOutcome.CANCELLED, reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormEffectExecutionRespVO onBpmProcessApproved(FormBpmProcessApprovedReqVO reqVO) {
        FormActionInstanceDO instance = requireInstanceByProcessInstanceId(reqVO.getProcessInstanceId());
        requireStatus(instance, FormInstanceStatus.IN_APPROVAL);
        FormActionPolicy policy = toPolicy(requireBusinessApprovalPolicy(instance.getPolicyId()));
        FormControlledActionLifecycleAdapter lifecycleAdapter = requireLifecycleAdapter(toDomainInstance(instance, policy));
        revokeAllActiveTaskPermissions(instance);
        instance.setStatus(FormInstanceStatus.PENDING_EFFECT.name());
        actionInstanceMapper.updateById(instance);
        FormEffectExecutionRespVO response = applyBusinessEffect(instance, policy);
        FormControlledActionApprovalOutcome outcome = FormEffectStatus.APPLIED.name().equals(response.getStatus())
                ? FormControlledActionApprovalOutcome.EFFECTIVE
                : FormControlledActionApprovalOutcome.EFFECT_FAILED_PENDING;
        notifyPendingApprovalClosed(instance, outcome, null, lifecycleAdapter);
        return response;
    }

    @Override
    public List<FormInstanceSnapshotRespVO> getInstanceSnapshots(Long instanceId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        return actionSnapshotMapper.selectByInstanceId(instance.getTenantId(), instance.getId()).stream()
                .map(this::toSnapshotResp)
                .toList();
    }

    @Override
    public PageResult<FormEffectExecutionRespVO> getPendingEffects(FormEffectPendingPageReqVO reqVO) {
        reqVO.setTenantId(resolveTenantId(reqVO.getTenantId()));
        PageResult<FormEffectExecutionDO> pageResult = effectExecutionMapper.selectPendingPage(reqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::toEffectResp).toList(),
                pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormEffectExecutionRespVO retryEffect(Long instanceId) {
        FormActionInstanceDO instance = requireInstance(instanceId);
        requireStatus(instance, FormInstanceStatus.EFFECT_FAILED_PENDING);
        FormActionPolicy policy = toPolicy(requireBusinessApprovalPolicy(instance.getPolicyId()));
        FormControlledActionLifecycleAdapter lifecycleAdapter = requireLifecycleAdapter(toDomainInstance(instance, policy));
        instance.setStatus(FormInstanceStatus.PENDING_EFFECT.name());
        actionInstanceMapper.updateById(instance);
        FormEffectExecutionRespVO response = applyBusinessEffect(instance, policy);
        FormControlledActionApprovalOutcome outcome = FormEffectStatus.APPLIED.name().equals(response.getStatus())
                ? FormControlledActionApprovalOutcome.EFFECTIVE
                : FormControlledActionApprovalOutcome.EFFECT_FAILED_PENDING;
        notifyPendingApprovalClosed(instance, outcome, null, lifecycleAdapter);
        return response;
    }

    private byte[] readSourceBytes(FormCenterTemplateImportReqVO reqVO) {
        try {
            return reqVO.getFile().getBytes();
        } catch (IOException ex) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template source file cannot be read: " + ex.getMessage());
        }
    }

    private void validateDocSource(FormTemplateImportCommand command) {
        String fileName = command.getSourceFileName();
        if (fileName == null || !(fileName.endsWith(".doc") || fileName.endsWith(".docx"))) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_TYPE_UNSUPPORTED,
                    "Only doc/docx template source files are supported: " + fileName);
        }
        if (command.getSourceBytes().length == 0) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template source file is empty: " + fileName);
        }
    }

    private String normalizeTemplateName(String templateName) {
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template name cannot be blank");
        }
        return templateName.trim();
    }

    private FormTemplateVersionDO resolveImportTargetTemplate(Long tenantId, String templateName,
            Long selectedTemplateId) {
        if (selectedTemplateId != null) {
            FormTemplateVersionDO selected = templateVersionMapper.selectLatestByTemplateId(tenantId, selectedTemplateId);
            if (selected == null) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                        "Selected template not found: " + selectedTemplateId);
            }
            if (!Objects.equals(templateName, selected.getTemplateName())) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                        "Selected template name mismatched: " + selectedTemplateId + "/" + templateName);
            }
            return selected;
        }
        return templateVersionMapper.selectLatestByTemplateName(tenantId, templateName);
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

    private BusinessApprovalRequest submitTemplateUpgradeApproval(Long tenantId, FormTemplateVersionDO version,
            Long applicantUserId, String reason) {
        if (businessApprovalOrchestrator == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Form template upgrade approval orchestrator is not available");
        }
        BusinessApprovalContext context = BusinessApprovalContext.builder()
                .tenantId(tenantId)
                .dataDomain(FORM_TEMPLATE_APPROVAL_DATA_DOMAIN)
                .systemCode(FORM_TEMPLATE_APPROVAL_SYSTEM_CODE)
                .objectType(FORM_TEMPLATE_APPROVAL_OBJECT_TYPE)
                .objectId(String.valueOf(version.getId()))
                .objectVersion(version.getVersionNo())
                .actionCode(TEMPLATE_IMPORT_ACTION_UPGRADE)
                .objectState(FormTemplateStatus.DRAFT.name())
                .applicantUserId(applicantUserId)
                .reason(reason)
                .build();
        try {
            return businessApprovalOrchestrator.submit(context);
        } catch (BusinessApprovalException ex) {
            FormCenterErrorCode errorCode = ex.getErrorCode() == BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_NOT_FOUND
                    ? FormCenterErrorCode.FORM_POLICY_NOT_FOUND : FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID;
            throw new FormCenterException(errorCode,
                    "Form template upgrade approval cannot be started: " + ex.getMessage());
        }
    }

    private BusinessApprovalRequest submitTemplateObsoleteApproval(FormTemplateVersionDO version,
            Long applicantUserId, String reason, Map<String, List<Long>> startUserSelectAssignees) {
        if (businessApprovalOrchestrator == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Form template obsolete approval orchestrator is not available");
        }
        BusinessApprovalContext context = buildTemplateObsoleteContext(version, applicantUserId, reason,
                startUserSelectAssignees);
        try {
            return businessApprovalOrchestrator.submit(context);
        } catch (BusinessApprovalException ex) {
            FormCenterErrorCode errorCode = ex.getErrorCode() == BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_NOT_FOUND
                    ? FormCenterErrorCode.FORM_POLICY_NOT_FOUND : FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID;
            throw new FormCenterException(errorCode,
                    "Form template obsolete approval cannot be started: " + ex.getMessage());
        }
    }

    private BusinessApprovalContext buildTemplateObsoleteContext(FormTemplateVersionDO version, Long applicantUserId,
            String reason, Map<String, List<Long>> startUserSelectAssignees) {
        return BusinessApprovalContext.builder()
                .tenantId(version.getTenantId())
                .dataDomain(FORM_TEMPLATE_APPROVAL_DATA_DOMAIN)
                .systemCode(FORM_TEMPLATE_APPROVAL_SYSTEM_CODE)
                .objectType(FORM_TEMPLATE_APPROVAL_OBJECT_TYPE)
                .objectId(String.valueOf(version.getId()))
                .objectVersion(version.getVersionNo())
                .actionCode(TEMPLATE_ACTION_OBSOLETE)
                .objectState(version.getStatus())
                .applicantUserId(applicantUserId)
                .reason(reason)
                .startUserSelectAssignees(startUserSelectAssignees)
                .build();
    }

    private FormTemplateObsoletePendingRespVO findTemplateObsoletePendingRequest(FormTemplateVersionDO version,
            Long currentUserId) {
        if (businessApprovalRequestStore == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Form template obsolete approval request store is not available");
        }
        return businessApprovalRequestStore.findPendingByBusinessAction(
                        buildTemplateObsoleteContext(version, currentUserId, "query pending obsolete request", null))
                .map(request -> toTemplateObsoletePendingResp(request, currentUserId))
                .orElse(null);
    }

    private BusinessApprovalRequest requireTemplateObsoletePendingRequest(FormTemplateVersionDO version) {
        if (businessApprovalRequestStore == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Form template obsolete approval request store is not available");
        }
        return businessApprovalRequestStore.findPendingByBusinessAction(
                        buildTemplateObsoleteContext(version, null, "query pending obsolete request", null))
                .orElseThrow(() -> new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                        "Pending obsolete approval request not found: " + version.getTemplateId()
                                + "/" + version.getVersionNo()));
    }

    private FormTemplateObsoletePendingRespVO toTemplateObsoletePendingResp(BusinessApprovalRequest request,
            Long currentUserId) {
        FormTemplateObsoletePendingRespVO respVO = new FormTemplateObsoletePendingRespVO();
        respVO.setApprovalRequestId(request.getRequestId());
        respVO.setApprovalProcessInstanceId(request.getProcessInstanceId());
        respVO.setApplicantUserId(request.getContext().getApplicantUserId());
        respVO.setCanWithdraw(Objects.equals(request.getContext().getApplicantUserId(), currentUserId));
        respVO.setObjectState(request.getContext().getObjectState());
        respVO.setStatus(request.getStatus().name());
        respVO.setReason(request.getContext().getReason());
        return respVO;
    }

    private String normalizeObsoleteReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                    "Template obsolete reason cannot be blank");
        }
        return reason.trim();
    }

    private String normalizeWithdrawReason(String reason) {
        return reason == null || reason.trim().isEmpty() ? "申请人撤回表单模板作废申请" : reason.trim();
    }

    private BpmProcessInstanceCreateReqDTO buildBpmRequest(FormActionInstanceDO instance, FormActionPolicy policy,
            Map<String, List<Long>> startUserSelectAssignees) {
        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO();
        reqDTO.setProcessDefinitionKey(policy.getBpmProcessKey());
        reqDTO.setBusinessKey(BUSINESS_KEY_PREFIX + instance.getInstanceCode());
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("formInstanceId", instance.getId());
        variables.put("objectId", instance.getObjectId());
        variables.put("objectVersion", instance.getObjectVersion());
        variables.put("actionCode", instance.getActionCode());
        Map<String, Object> businessContext = JsonUtils.parseObject(instance.getBusinessContextJson(),
                new TypeReference<Map<String, Object>>() {});
        variables.put("businessContext", businessContext);
        variables.put("tenantId", businessContext.get("tenantId"));
        variables.put("dataDomain", businessContext.get("dataDomain"));
        variables.put("systemCode", businessContext.get("systemCode"));
        variables.put("objectType", businessContext.get("objectType"));
        variables.put("objectState", businessContext.get("objectState"));
        variables.put("orgCode", businessContext.get("orgCode"));
        variables.put("deptCode", businessContext.get("deptCode"));
        variables.put("roleCodes", businessContext.get("roleCodes"));
        variables.put("productCode", businessContext.get("productCode"));
        variables.put("categoryCode", businessContext.get("categoryCode"));
        variables.put("reason", businessContext.get("reason"));
        if (startUserSelectAssignees != null && !startUserSelectAssignees.isEmpty()) {
            variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                    startUserSelectAssignees);
            reqDTO.setStartUserSelectAssignees(startUserSelectAssignees);
        }
        reqDTO.setVariables(variables);
        return reqDTO;
    }

    private FormActionInstanceDO requireInstance(Long instanceId) {
        FormActionInstanceDO instance = actionInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                    "Form action instance not found: " + instanceId);
        }
        return instance;
    }

    private FormActionPolicyDO requirePolicy(Long policyId) {
        FormActionPolicyDO policy = actionPolicyMapper.selectById(policyId);
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (policy == null || !Objects.equals(policy.getTenantId(), currentTenantId)) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Form policy not found: " + policyId);
        }
        return policy;
    }

    private BusinessApprovalPolicyDO requireBusinessApprovalPolicy(Long policyId) {
        BusinessApprovalPolicyDO policy = businessApprovalPolicyMapper.selectById(policyId);
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (policy == null || !Objects.equals(policy.getTenantId(), currentTenantId)) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "Business approval policy not found: " + policyId);
        }
        return policy;
    }

    private FormTemplateVersionDO requireTemplateVersion(Long templateId, String versionNo) {
        FormTemplateVersionDO version = templateVersionMapper.selectByTemplateIdAndVersionNo(templateId, versionNo);
        if (version == null) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template version not found: " + templateId + "/" + versionNo);
        }
        return version;
    }

    private FormTemplateVersionDO requireCurrentTenantTemplateVersion(Long templateId, String versionNo) {
        FormTemplateVersionDO version = requireTemplateVersion(templateId, versionNo);
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(version.getTenantId(), currentTenantId)) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template version not found: " + templateId + "/" + versionNo);
        }
        return version;
    }

    private FormTemplateVersionDO requireTenantTemplateVersion(Long versionId) {
        FormTemplateVersionDO version = templateVersionMapper.selectById(versionId);
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (version == null || !Objects.equals(version.getTenantId(), currentTenantId)) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template version not found: " + versionId);
        }
        return version;
    }

    private FormTemplateVersionDO requireLatestPublishedTemplateVersion(Long tenantId, FormPolicySlot slot) {
        Long templateId = parseTemplateId(slot, slot.getTemplateVersionRef());
        FormTemplateVersionDO latestVersion = templateVersionMapper.selectLatestPublishedByTemplateId(tenantId, templateId);
        if (latestVersion == null) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Latest published template version not found: " + templateId);
        }
        return latestVersion;
    }

    private void requireStatus(FormActionInstanceDO instance, FormInstanceStatus... allowedStatuses) {
        for (FormInstanceStatus allowedStatus : allowedStatuses) {
            if (allowedStatus.name().equals(instance.getStatus())) {
                return;
            }
        }
        throw new FormCenterException(FormCenterErrorCode.FORM_INSTANCE_STATUS_INVALID,
                "Form instance status invalid: " + instance.getStatus());
    }

    private Long resolveTenantId(Long requestedTenantId) {
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (requestedTenantId != null && !Objects.equals(requestedTenantId, currentTenantId)) {
            throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                    "Request tenant does not match current tenant context: " + requestedTenantId);
        }
        return currentTenantId;
    }

    private FormPolicyType parsePolicyType(String policyType) {
        try {
            return FormPolicyType.valueOf(policyType);
        } catch (IllegalArgumentException ex) {
            throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                    "Unsupported form policy type: " + policyType);
        }
    }

    private FormApprovalMode parseApprovalModeOrDefault(String approvalMode) {
        if (approvalMode == null || approvalMode.isBlank()) {
            return FormApprovalMode.BPM_REQUIRED;
        }
        return parseApprovalMode(approvalMode);
    }

    private FormApprovalMode parseApprovalMode(String approvalMode) {
        try {
            return FormApprovalMode.valueOf(approvalMode);
        } catch (IllegalArgumentException ex) {
            throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                    "Unsupported form approval mode: " + approvalMode);
        }
    }

    private String resolveSwitchBpmProcessKey(FormActionPolicyDO sourcePolicy, FormApprovalMode targetMode,
            String requestedProcessKey) {
        if (targetMode == FormApprovalMode.DIRECT) {
            return requestedProcessKey == null || requestedProcessKey.isBlank()
                    ? sourcePolicy.getBpmProcessKey() : requestedProcessKey;
        }
        String processKey = requestedProcessKey == null || requestedProcessKey.isBlank()
                ? sourcePolicy.getBpmProcessKey() : requestedProcessKey;
        if (processKey == null || processKey.isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "BPM process key is required before switching form policy to BPM_REQUIRED");
        }
        return processKey;
    }

    private FormPolicySlot toPolicySlot(FormPolicySlotReqVO slotReqVO) {
        FormTemplateVersionDO version = templateVersionMapper.selectLatestPublishedByTemplateId(
                TenantContextHolder.getRequiredTenantId(), slotReqVO.getTemplateId());
        if (version == null) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Latest published template version not found: " + slotReqVO.getTemplateId());
        }
        return new FormPolicySlot(slotReqVO.getSlotCode(), Boolean.TRUE.equals(slotReqVO.getRequired()),
                FormTemplateVersionRef.of(version.getId(), String.valueOf(version.getTemplateId()),
                        version.getVersionNo(), version.getTemplateName()));
    }

    private List<FormPolicySlot> parsePolicySlots(String slotsJson) {
        if (slotsJson == null || slotsJson.isBlank()) {
            return List.of();
        }
        return JsonUtils.parseArray(slotsJson, FormPolicySlot.class);
    }

    private void validatePolicySlots(FormPolicyType policyType, List<FormPolicySlot> slots) {
        if (policyType == FormPolicyType.NONE && slots.isEmpty()) {
            return;
        }
        if (slots.isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.FORM_TEMPLATE_SLOT_CONFLICT,
                    "Form policy must bind at least one template slot");
        }
        Set<String> slotCodes = new HashSet<>();
        for (FormPolicySlot slot : slots) {
            if (slot.getTemplateVersionRef() == null || slot.getTemplateVersionRef().getVersionId() == null) {
                throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                        "Form policy slot must bind a template version: " + slot.getSlotCode());
            }
            if (!slotCodes.add(slot.getSlotCode())) {
                throw new FormCenterException(FormCenterErrorCode.FORM_TEMPLATE_SLOT_CONFLICT,
                        "Form policy slot matched more than one template: " + slot.getSlotCode());
            }
        }
    }

    private BusinessActionContext toContext(BusinessActionContextReqVO reqVO) {
        return BusinessActionContext.builder()
                .tenantId(reqVO.getTenantId())
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

    private FormActionPolicy toPolicy(FormActionPolicyDO policyDO) {
        return FormActionPolicy.builder()
                .policyId(policyDO.getId())
                .tenantId(policyDO.getTenantId())
                .dataDomain(policyDO.getDataDomain())
                .systemCode(policyDO.getSystemCode())
                .objectType(policyDO.getObjectType())
                .actionCode(policyDO.getActionCode())
                .objectState(policyDO.getObjectState())
                .policyType(FormPolicyType.valueOf(policyDO.getPolicyType()))
                .approvalMode(parseApprovalModeOrDefault(policyDO.getApprovalMode()))
                .bpmProcessKey(policyDO.getBpmProcessKey())
                .effectExecutorCode(policyDO.getEffectExecutorCode())
                .status(policyDO.getStatus())
                .slots(resolveLatestPublishedSlots(policyDO.getTenantId(),
                        parsePolicySlots(policyDO.getSlotsJson())))
                .build();
    }

    private FormActionPolicy toPolicy(BusinessApprovalPolicyDO policyDO) {
        BusinessApprovalPolicyMode policyMode = parseBusinessApprovalPolicyMode(policyDO.getPolicyMode());
        return FormActionPolicy.builder()
                .policyId(policyDO.getId())
                .tenantId(policyDO.getTenantId())
                .dataDomain(policyDO.getDataDomain())
                .systemCode(policyDO.getSystemCode())
                .objectType(policyDO.getObjectType())
                .actionCode(policyDO.getActionCode())
                .objectState(policyDO.getObjectState())
                .policyType(FormPolicyType.NONE)
                .approvalMode(toFormApprovalMode(policyMode, policyDO.getId()))
                .bpmProcessKey(policyDO.getProcessDefinitionKey())
                .effectExecutorCode(policyDO.getEffectExecutorCode())
                .status(policyDO.getStatus())
                .slots(List.of())
                .build();
    }

    private FormActionPolicy toStoredPolicy(FormActionPolicyDO policyDO) {
        return FormActionPolicy.builder()
                .policyId(policyDO.getId())
                .tenantId(policyDO.getTenantId())
                .dataDomain(policyDO.getDataDomain())
                .systemCode(policyDO.getSystemCode())
                .objectType(policyDO.getObjectType())
                .actionCode(policyDO.getActionCode())
                .objectState(policyDO.getObjectState())
                .policyType(FormPolicyType.valueOf(policyDO.getPolicyType()))
                .approvalMode(parseApprovalModeOrDefault(policyDO.getApprovalMode()))
                .bpmProcessKey(policyDO.getBpmProcessKey())
                .effectExecutorCode(policyDO.getEffectExecutorCode())
                .status(policyDO.getStatus())
                .slots(parsePolicySlots(policyDO.getSlotsJson()))
                .build();
    }

    private List<FormPolicySlot> resolveLatestPublishedSlots(Long tenantId, List<FormPolicySlot> slots) {
        return slots.stream()
                .map(slot -> resolveLatestPublishedSlot(tenantId, slot))
                .toList();
    }

    private FormPolicySlot resolveLatestPublishedSlot(Long tenantId, FormPolicySlot slot) {
        FormTemplateVersionDO latestVersion = requireLatestPublishedTemplateVersion(tenantId, slot);
        return new FormPolicySlot(slot.getSlotCode(), slot.isRequired(),
                FormTemplateVersionRef.of(latestVersion.getId(), String.valueOf(latestVersion.getTemplateId()),
                        latestVersion.getVersionNo(), latestVersion.getTemplateName()));
    }

    private Long parseTemplateId(FormPolicySlot slot, FormTemplateVersionRef ref) {
        if (ref == null || ref.getTemplateCode() == null || ref.getTemplateCode().isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Form policy slot must carry template identity: " + slot.getSlotCode());
        }
        try {
            return Long.valueOf(ref.getTemplateCode());
        } catch (NumberFormatException ex) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Form policy slot template identity is invalid: " + slot.getSlotCode());
        }
    }

    private void recordSnapshot(FormActionInstanceDO instance, FormSnapshotType snapshotType, String formDataJson) {
        Long count = actionSnapshotMapper.selectCountByInstanceId(instance.getTenantId(), instance.getId());
        actionSnapshotMapper.insert(FormActionSnapshotDO.builder()
                .tenantId(instance.getTenantId())
                .instanceId(instance.getId())
                .snapshotType(snapshotType.name())
                .snapshotVersion(count.intValue() + 1)
                .formDataJson(formDataJson)
                .businessContextJson(instance.getBusinessContextJson())
                .attachmentIdsJson(JsonUtils.toJsonString(List.of()))
                .build());
    }

    private FormActionInstanceDO requireInstanceByProcessInstanceId(String processInstanceId) {
        FormActionInstanceDO instance = actionInstanceMapper.selectByProcessInstanceId(
                TenantContextHolder.getRequiredTenantId(), processInstanceId);
        if (instance == null) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "Form action instance not found by BPM process instance: " + processInstanceId);
        }
        if (!Objects.equals(processInstanceId, instance.getBpmProcessInstanceId())) {
            throw new FormCenterException(FormCenterErrorCode.BPM_CALLBACK_STALE,
                    "BPM callback process instance is stale: " + processInstanceId);
        }
        return instance;
    }

    private void revokeTaskPermissions(FormActionInstanceDO instance, String taskId) {
        List<FormTaskPermissionDO> activePermissions = taskPermissionMapper.selectActiveByTaskId(instance.getTenantId(),
                instance.getId(), taskId);
        if (activePermissions.isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.BPM_TASK_PERMISSION_MISSING,
                    "Active form task permission not found for BPM task: " + taskId);
        }
        for (FormTaskPermissionDO permission : activePermissions) {
            permission.setStatus(FormTaskPermissionDO.STATUS_REVOKED);
            taskPermissionMapper.updateById(permission);
        }
    }

    private void revokeAllActiveTaskPermissions(FormActionInstanceDO instance) {
        for (FormTaskPermissionDO permission : taskPermissionMapper.selectActiveByProcessInstanceId(
                instance.getTenantId(), instance.getBpmProcessInstanceId())) {
            permission.setStatus(FormTaskPermissionDO.STATUS_REVOKED);
            taskPermissionMapper.updateById(permission);
        }
    }

    private void persistCurrentActiveTaskPermissions(FormActionInstanceDO instance) {
        List<Task> activeTasks = flowableTaskService.createTaskQuery()
                .processInstanceId(instance.getBpmProcessInstanceId())
                .active()
                .list();
        if (activeTasks.isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.BPM_ACTIVE_TASK_ASSIGNEE_MISSING,
                    "BPM active task is required after form action submit: " + instance.getBpmProcessInstanceId());
        }
        for (Task activeTask : activeTasks) {
            List<Long> handlerUserIds = resolveTaskHandlerUserIds(activeTask);
            if (handlerUserIds.isEmpty()) {
                throw new FormCenterException(FormCenterErrorCode.BPM_ACTIVE_TASK_ASSIGNEE_MISSING,
                        "BPM active task assignees are required for derived form permissions: " + activeTask.getId());
            }
            FormBpmTaskCreatedReqVO reqVO = new FormBpmTaskCreatedReqVO();
            reqVO.setProcessInstanceId(instance.getBpmProcessInstanceId());
            reqVO.setTaskId(activeTask.getId());
            reqVO.setHandlerUserIds(handlerUserIds);
            onBpmTaskCreated(reqVO);
        }
    }

    private List<Long> resolveTaskHandlerUserIds(Task task) {
        Set<Long> userIds = new HashSet<>();
        addTaskHandlerUserId(userIds, task.getAssignee());
        addTaskHandlerUserId(userIds, task.getOwner());
        return userIds.stream().toList();
    }

    private void addTaskHandlerUserId(Set<Long> userIds, String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            return;
        }
        try {
            userIds.add(Long.valueOf(rawUserId));
        } catch (NumberFormatException ex) {
            throw new FormCenterException(FormCenterErrorCode.BPM_ACTIVE_TASK_ASSIGNEE_MISSING,
                    "BPM active task handler is not a numeric user id: " + rawUserId);
        }
    }

    private FormEffectExecutionRespVO applyBusinessEffect(FormActionInstanceDO instance) {
        return applyBusinessEffect(instance, toPolicy(requireBusinessApprovalPolicy(instance.getPolicyId())));
    }

    private FormEffectExecutionRespVO applyBusinessEffect(FormActionInstanceDO instance, FormActionPolicy policy) {
        FormEffectExecutionDO existingExecution = effectExecutionMapper.selectByInstanceIdAndIdempotencyKey(
                instance.getTenantId(), instance.getId(), instance.getIdempotencyKey());
        if (existingExecution != null && FormEffectStatus.APPLIED.name().equals(existingExecution.getStatus())) {
            instance.setStatus(FormInstanceStatus.EFFECTIVE.name());
            actionInstanceMapper.updateById(instance);
            return toEffectResp(existingExecution);
        }

        FormBusinessEffectExecutor executor = requireEffectExecutor(policy.getEffectExecutorCode());
        FormBusinessEffectResult result = executor.execute(toDomainInstance(instance, policy), instance.getIdempotencyKey());

        FormEffectExecutionDO execution = existingExecution == null ? FormEffectExecutionDO.builder()
                .tenantId(instance.getTenantId())
                .instanceId(instance.getId())
                .executionCode("EFFECT-" + instance.getIdempotencyKey())
                .idempotencyKey(instance.getIdempotencyKey())
                .build() : existingExecution;
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
        if (execution.getId() == null) {
            effectExecutionMapper.insert(execution);
        } else {
            effectExecutionMapper.updateById(execution);
        }
        actionInstanceMapper.updateById(instance);
        return toEffectResp(execution);
    }

    private FormBusinessEffectExecutor requireEffectExecutor(String executorCode) {
        if (executorCode == null || executorCode.isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.EFFECT_EXECUTOR_MISSING,
                    "Effect executor code is required before applying form action");
        }
        return effectExecutors.stream()
                .filter(executor -> executorCode.equals(executor.getExecutorCode()))
                .findFirst()
                .orElseThrow(() -> new FormCenterException(FormCenterErrorCode.EFFECT_EXECUTOR_MISSING,
                        "Effect executor is not registered: " + executorCode));
    }

    private FormControlledActionLifecycleAdapter runLifecyclePreflight(FormActionInstance instance) {
        FormControlledActionLifecycleAdapter adapter = requireLifecycleAdapter(instance);
        FormBusinessEffectPrecheck precheck = adapter.preflight(instance);
        if (precheck == null || !precheck.isPassed()) {
            String reason = precheck == null ? "Lifecycle preflight did not return a result"
                    : precheck.getFailureReason();
            throw new FormCenterException(FormCenterErrorCode.CONTROLLED_ACTION_PREFLIGHT_FAILED,
                    "Controlled action lifecycle preflight failed: " + reason);
        }
        return adapter;
    }

    private void cancelCreatedProcessInstance(Long userId, String processInstanceId, Long instanceId,
            RuntimeException submitFailure) {
        try {
            processInstanceApi.cancelProcessInstance(userId, processInstanceId,
                    "form action submit compensation: instanceId=" + instanceId);
        } catch (RuntimeException compensationFailure) {
            compensationFailure.addSuppressed(submitFailure);
            throw compensationFailure;
        }
    }

    private void closePendingApproval(String processInstanceId, FormInstanceStatus targetStatus,
            FormControlledActionApprovalOutcome outcome, String reason) {
        FormActionInstanceDO instance = requireInstanceByProcessInstanceId(processInstanceId);
        requireStatus(instance, FormInstanceStatus.IN_APPROVAL, FormInstanceStatus.REWORKING);
        revokeAllActiveTaskPermissions(instance);
        instance.setStatus(targetStatus.name());
        actionInstanceMapper.updateById(instance);
        notifyPendingApprovalClosed(instance, outcome, reason);
    }

    private void notifyPendingApprovalClosed(FormActionInstanceDO instance,
            FormControlledActionApprovalOutcome outcome, String reason) {
        notifyPendingApprovalClosed(instance, outcome, reason, null);
    }

    private void notifyPendingApprovalClosed(FormActionInstanceDO instance,
            FormControlledActionApprovalOutcome outcome, String reason, FormControlledActionLifecycleAdapter adapter) {
        FormActionPolicy policy = toPolicy(requireBusinessApprovalPolicy(instance.getPolicyId()));
        FormActionInstance domainInstance = toDomainInstance(instance, policy);
        FormControlledActionLifecycleAdapter lifecycleAdapter = adapter == null ? requireLifecycleAdapter(domainInstance) : adapter;
        lifecycleAdapter.onPendingApprovalClosed(domainInstance, outcome, reason);
    }

    private FormControlledActionLifecycleAdapter requireLifecycleAdapter(FormActionInstance instance) {
        return lifecycleAdapters.stream()
                .filter(candidate -> candidate.supports(instance))
                .findFirst()
                .orElseThrow(() -> new FormCenterException(FormCenterErrorCode.CONTROLLED_ACTION_ADAPTER_MISSING,
                        "Controlled action lifecycle adapter is not registered: "
                                + instance.getBusinessContext().getSystemCode() + "/"
                                + instance.getBusinessContext().getObjectType() + "/"
                                + instance.getBusinessContext().getActionCode()));
    }

    private FormActionInstance toDomainInstance(FormActionInstanceDO instance, FormActionPolicyDO policy) {
        return toDomainInstance(instance, toPolicy(policy));
    }

    private BusinessApprovalPolicyMode parseBusinessApprovalPolicyMode(String policyMode) {
        try {
            return BusinessApprovalPolicyMode.valueOf(policyMode);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                    "Unsupported business approval policy mode: " + policyMode);
        }
    }

    private FormApprovalMode toFormApprovalMode(BusinessApprovalPolicyMode policyMode, Long policyId) {
        if (policyMode == BusinessApprovalPolicyMode.BPM_REQUIRED) {
            return FormApprovalMode.BPM_REQUIRED;
        }
        if (policyMode == BusinessApprovalPolicyMode.DIRECT) {
            return FormApprovalMode.DIRECT;
        }
        throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                "Form Center runtime does not support SIGNATURE_REQUIRED business approval policy: " + policyId);
    }

    private FormActionInstance toDomainInstance(FormActionInstanceDO instance, FormActionPolicy policy) {
        BusinessActionContextReqVO contextReqVO = JsonUtils.parseObject(instance.getBusinessContextJson(),
                BusinessActionContextReqVO.class);
        FormActionInstance domain = new FormActionInstance(instance.getInstanceCode(), FormActionResolution.from(policy),
                toContext(contextReqVO),
                instance.getApplicantUserId(), instance.getIdempotencyKey());
        domain.setStatus(FormInstanceStatus.valueOf(instance.getStatus()));
        if (instance.getBpmProcessInstanceId() != null) {
            domain.setBpmBinding(new FormBpmBinding(instance.getBpmProcessInstanceId(), null));
        }
        domain.setFormData(parseFormData(instance.getFormDataJson()));
        return domain;
    }

    private FormActionInstance toDomainInstanceSnapshot(FormActionInstanceDO instance, FormActionPolicyDO policy) {
        BusinessActionContextReqVO contextReqVO = JsonUtils.parseObject(instance.getBusinessContextJson(),
                BusinessActionContextReqVO.class);
        FormActionPolicy snapshotPolicy = FormActionPolicy.builder()
                .policyId(policy.getId())
                .tenantId(policy.getTenantId())
                .dataDomain(policy.getDataDomain())
                .systemCode(policy.getSystemCode())
                .objectType(policy.getObjectType())
                .actionCode(policy.getActionCode())
                .objectState(policy.getObjectState())
                .policyType(FormPolicyType.valueOf(policy.getPolicyType()))
                .approvalMode(parseApprovalModeOrDefault(policy.getApprovalMode()))
                .bpmProcessKey(policy.getBpmProcessKey())
                .effectExecutorCode(policy.getEffectExecutorCode())
                .status(policy.getStatus())
                .slots(parsePolicySlots(policy.getSlotsJson()))
                .build();
        FormActionInstance domain = new FormActionInstance(instance.getInstanceCode(),
                FormActionResolution.from(snapshotPolicy), toContext(contextReqVO), instance.getApplicantUserId(),
                instance.getIdempotencyKey());
        domain.setStatus(FormInstanceStatus.valueOf(instance.getStatus()));
        if (instance.getBpmProcessInstanceId() != null) {
            domain.setBpmBinding(new FormBpmBinding(instance.getBpmProcessInstanceId(), null));
        }
        domain.setFormData(parseFormData(instance.getFormDataJson()));
        return domain;
    }

    private FormCenterTemplateRespVO toTemplateResp(FormTemplateVersionDO version) {
        FormCenterTemplateRespVO respVO = new FormCenterTemplateRespVO();
        respVO.setTemplateId(version.getTemplateId());
        respVO.setTemplateName(version.getTemplateName());
        respVO.setVersionNo(version.getVersionNo());
        respVO.setStatus(version.getStatus());
        respVO.setUpdatedTime(version.getUpdateTime());
        respVO.setRemark(version.getRemark());
        respVO.setRecognizedFields(parseRecognizedFields(version.getRecognizedSchemaJson()));
        respVO.setJimuSchemaJson(version.getJimuSchemaJson());
        respVO.setSourceFileName(version.getSourceFileName());
        return respVO;
    }

    private List<FormRecognizedField> parseRecognizedFields(String recognizedSchemaJson) {
        if (recognizedSchemaJson == null || recognizedSchemaJson.isBlank()) {
            return List.of();
        }
        return JsonUtils.parseArray(recognizedSchemaJson, FormRecognizedField.class);
    }

    private FormActionResolutionRespVO toResolutionResp(FormActionResolution resolution) {
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

    private FormPolicyRespVO toPolicyResp(FormActionPolicyDO policy) {
        FormPolicyRespVO respVO = new FormPolicyRespVO();
        respVO.setId(policy.getId());
        respVO.setDataDomain(policy.getDataDomain());
        respVO.setSystemCode(policy.getSystemCode());
        respVO.setObjectType(policy.getObjectType());
        respVO.setActionCode(policy.getActionCode());
        respVO.setObjectState(policy.getObjectState());
        respVO.setPolicyType(policy.getPolicyType());
        respVO.setApprovalMode(parseApprovalModeOrDefault(policy.getApprovalMode()).name());
        respVO.setBpmProcessKey(policy.getBpmProcessKey());
        respVO.setEffectExecutorCode(policy.getEffectExecutorCode());
        respVO.setStatus(policy.getStatus());
        respVO.setSlots(parsePolicySlots(policy.getSlotsJson()));
        respVO.setRemark(policy.getRemark());
        respVO.setUpdatedTime(policy.getUpdateTime());
        return respVO;
    }

    private FormInstanceRespVO toInstanceResp(FormActionInstanceDO instance) {
        FormInstanceRespVO respVO = new FormInstanceRespVO();
        respVO.setId(instance.getId());
        respVO.setInstanceCode(instance.getInstanceCode());
        respVO.setStatus(instance.getStatus());
        respVO.setBpmProcessInstanceId(instance.getBpmProcessInstanceId());
        respVO.setContext(JsonUtils.parseObject(instance.getBusinessContextJson(), BusinessActionContextReqVO.class));
        return respVO;
    }

    private FormInstanceSnapshotRespVO toSnapshotResp(FormActionSnapshotDO snapshot) {
        FormInstanceSnapshotRespVO respVO = new FormInstanceSnapshotRespVO();
        respVO.setId(snapshot.getId());
        respVO.setInstanceId(snapshot.getInstanceId());
        respVO.setSnapshotType(snapshot.getSnapshotType());
        respVO.setSnapshotVersion(snapshot.getSnapshotVersion());
        respVO.setFormData(parseFormData(snapshot.getFormDataJson()));
        respVO.setContext(JsonUtils.parseObject(snapshot.getBusinessContextJson(), BusinessActionContextReqVO.class));
        respVO.setAttachmentIds(parseAttachmentIds(snapshot.getAttachmentIdsJson()));
        respVO.setCreatedTime(snapshot.getCreateTime());
        return respVO;
    }

    private List<String> parseAttachmentIds(String attachmentIdsJson) {
        if (attachmentIdsJson == null || attachmentIdsJson.isBlank()) {
            return List.of();
        }
        return JsonUtils.parseArray(attachmentIdsJson, String.class);
    }

    private Map<String, Object> parseFormData(String formDataJson) {
        if (formDataJson == null || formDataJson.isBlank()) {
            return Map.of();
        }
        return JsonUtils.parseObject(formDataJson, new TypeReference<Map<String, Object>>() {});
    }

    private FormEffectExecutionRespVO toEffectResp(FormEffectExecutionDO execution) {
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

}
