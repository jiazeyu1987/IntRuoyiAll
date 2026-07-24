package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyResolution;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class BusinessApprovalOrchestrator {

    private final BusinessApprovalPolicyResolveService policyResolveService;
    private final BusinessApprovalEffectExecutorRegistry executorRegistry;
    private final BusinessApprovalRequestStore requestStore;
    private final BusinessApprovalBpmStarter bpmStarter;
    private final AdminUserApi adminUserApi;
    private final ApprovalSignatureRecordService signatureRecordService;

    public BusinessApprovalOrchestrator(BusinessApprovalPolicyResolveService policyResolveService,
                                        BusinessApprovalEffectExecutorRegistry executorRegistry,
                                        BusinessApprovalRequestStore requestStore,
                                        BusinessApprovalBpmStarter bpmStarter,
                                        AdminUserApi adminUserApi,
                                        ApprovalSignatureRecordService signatureRecordService) {
        this.policyResolveService = policyResolveService;
        this.executorRegistry = executorRegistry;
        this.requestStore = requestStore;
        this.bpmStarter = bpmStarter;
        this.adminUserApi = adminUserApi;
        this.signatureRecordService = signatureRecordService;
    }

    public BusinessApprovalRequest submit(BusinessApprovalContext context) {
        return submit(context, null);
    }

    public BusinessApprovalRequest submit(BusinessApprovalContext context, String signaturePassword) {
        BusinessApprovalPolicyResolution resolution = policyResolveService.resolve(context);
        BusinessApprovalPolicy policy = resolution.getPolicy();
        BusinessApprovalEffectExecutor executor = executorRegistry.requireExecutor(resolution.getEffectExecutorCode());
        executor.precheck(context);
        if (resolution.getMode() == BusinessApprovalPolicyMode.DIRECT) {
            return executeDirect(context, policy, executor);
        }
        if (resolution.getMode() == BusinessApprovalPolicyMode.SIGNATURE_REQUIRED) {
            return executeSignatureRequired(context, policy, executor, signaturePassword);
        }
        if (resolution.getMode() == BusinessApprovalPolicyMode.BPM_REQUIRED) {
            return startBpmRequired(context, policy, executor);
        }
        throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID,
                "Unsupported business approval mode: " + resolution.getMode());
    }

    private BusinessApprovalRequest executeDirect(BusinessApprovalContext context,
                                                  BusinessApprovalPolicy policy,
                                                  BusinessApprovalEffectExecutor executor) {
        BusinessApprovalRequest request = requestStore.createDirectRequest(context, policy);
        BusinessApprovalEffectResult result = executor.executeDirect(context, request);
        return requestStore.update(request.withStatus(BusinessApprovalRequestStatus.DIRECT_EXECUTED, result));
    }

    private BusinessApprovalRequest executeSignatureRequired(BusinessApprovalContext context,
                                                            BusinessApprovalPolicy policy,
                                                            BusinessApprovalEffectExecutor executor,
                                                            String signaturePassword) {
        if (StrUtil.isBlank(signaturePassword)) {
            throw new BusinessApprovalException(
                    BusinessApprovalErrorCode.BUSINESS_APPROVAL_SIGNATURE_PASSWORD_REQUIRED,
                    "Electronic signature password is required for SIGNATURE_REQUIRED policy");
        }
        String trimmedPassword = StrUtil.trim(signaturePassword);
        BusinessApprovalRequest request = requestStore.createDirectRequest(context, policy);
        adminUserApi.validatePassword(context.getApplicantUserId(), trimmedPassword);
        Objects.requireNonNull(signatureRecordService.recordReviewSignature(ApprovalTaskReviewContext.of(
                context.getApplicantUserId(),
                ApprovalModuleCode.BPM,
                "BUSINESS_APPROVAL_" + context.getObjectType() + "_" + context.getActionCode(),
                String.valueOf(request.getRequestId()),
                context.getObjectType() + ":" + context.getObjectId() + ":" + context.getActionCode(),
                null,
                ApprovalTaskReviewResult.APPROVE,
                context.getReason(),
                trimmedPassword,
                false)), "BUSINESS_APPROVAL_SIGNATURE_RECORD_REQUIRED");
        BusinessApprovalEffectResult result = executor.executeDirect(context, request);
        return requestStore.update(request.withStatus(BusinessApprovalRequestStatus.DIRECT_EXECUTED, result));
    }

    private BusinessApprovalRequest startBpmRequired(BusinessApprovalContext context,
                                                    BusinessApprovalPolicy policy,
                                                    BusinessApprovalEffectExecutor executor) {
        if (StrUtil.isBlank(policy.getProcessDefinitionKey())) {
            throw new BusinessApprovalException(
                    BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_DEFINITION_MISSING,
                    "BPM process definition key is required for BPM_REQUIRED policy");
        }
        BusinessApprovalRequest request = requestStore.createPendingRequest(context, policy);
        String processInstanceId = bpmStarter.start(request, policy.getProcessDefinitionKey(),
                buildBpmVariables(request));
        if (StrUtil.isBlank(processInstanceId)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "BPM process instance was not started for request " + request.getRequestId());
        }
        BusinessApprovalRequest attached = requestStore.attachProcessInstance(request.getRequestId(),
                StrUtil.trim(processInstanceId));
        try {
            BusinessApprovalEffectResult result = executor.markPending(context, attached);
            return requestStore.update(attached.withStatus(BusinessApprovalRequestStatus.PENDING_BPM, result));
        } catch (RuntimeException ex) {
            cancelStartedProcess(attached, StrUtil.trim(processInstanceId), ex);
            throw ex;
        }
    }

    private void cancelStartedProcess(BusinessApprovalRequest request,
                                      String processInstanceId,
                                      RuntimeException pendingFailure) {
        try {
            bpmStarter.cancel(request, processInstanceId,
                    "business approval mark pending failed: requestId=" + request.getRequestId());
        } catch (RuntimeException cancelFailure) {
            cancelFailure.addSuppressed(pendingFailure);
            throw cancelFailure;
        }
    }

    private Map<String, Object> buildBpmVariables(BusinessApprovalRequest request) {
        BusinessApprovalContext context = request.getContext();
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("tenantId", context.getTenantId());
        variables.put("businessType", resolveBusinessType(context));
        variables.put("approvalRequestId", request.getRequestId());
        variables.put("dataDomain", context.getDataDomain());
        variables.put("systemCode", context.getSystemCode());
        variables.put("objectType", context.getObjectType());
        variables.put("objectId", context.getObjectId());
        variables.put("objectVersion", context.getObjectVersion());
        variables.put("actionCode", context.getActionCode());
        variables.put("objectState", context.getObjectState());
        variables.put("businessKey", context.getObjectType() + ":" + context.getObjectId() + ":" + context.getActionCode());
        variables.put("reason", context.getReason());
        if (context.getVariables() != null && !context.getVariables().isEmpty()) {
            context.getVariables().forEach(variables::putIfAbsent);
        }
        if (context.getStartUserSelectAssignees() != null && !context.getStartUserSelectAssignees().isEmpty()) {
            variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                    context.getStartUserSelectAssignees());
        }
        return variables;
    }

    private String resolveBusinessType(BusinessApprovalContext context) {
        Object businessType = context.getVariables() == null ? null : context.getVariables().get("businessType");
        String text = businessType == null ? null : String.valueOf(businessType);
        return StrUtil.isBlank(text) ? context.getSystemCode() + "_" + context.getObjectType() + "_"
                + context.getActionCode() : StrUtil.trim(text);
    }

}
