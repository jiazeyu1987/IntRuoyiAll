package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MesProEdhrBatchVoidFormEffectExecutor
        implements FormBusinessEffectExecutor, FormControlledActionLifecycleAdapter, BusinessApprovalEffectExecutor {

    public static final String EXECUTOR_CODE = "EDHR_BATCH_VOID";
    private static final String LIFECYCLE_CONTEXT_ERROR =
            "EDHR_BATCH_VOID lifecycle adapter only accepts MES EDHR_BATCH_EXECUTION VOID actions";

    private final MesProEdhrBatchVoidEffectService batchVoidEffectService;

    public MesProEdhrBatchVoidFormEffectExecutor(MesProEdhrBatchVoidEffectService batchVoidEffectService) {
        this.batchVoidEffectService = batchVoidEffectService;
    }

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    @Override
    public String getBpmProcessDefinitionKey() {
        return MesProEdhrRecordChangeServiceImpl.BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY;
    }

    @Override
    public void precheck(BusinessApprovalContext context) {
        requireBusinessVoidContext(context);
        batchVoidEffectService.precheckPlatformVoidBatchExecution(toRequest(context));
    }

    @Override
    public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context, BusinessApprovalRequest request) {
        requireBusinessVoidContext(context);
        EdhrRecordChangeRespVO event = batchVoidEffectService.executeDirectPlatformVoidBatchExecution(
                toRequest(context), requiredBusinessActorUserId(context.getApplicantUserId(), "applicantUserId"));
        return BusinessApprovalEffectResult.completed(resultState(event, "direct batch void"));
    }

    @Override
    public BusinessApprovalEffectResult markPending(BusinessApprovalContext context, BusinessApprovalRequest request) {
        requireBusinessVoidContext(context);
        EdhrRecordChangeRespVO event = batchVoidEffectService.requestPlatformVoidBatchExecution(
                toRequest(context), requiredProcessInstanceId(request));
        return BusinessApprovalEffectResult.pending(resultState(event, "pending batch void"));
    }

    @Override
    public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context, BusinessApprovalRequest request,
            Long actorUserId) {
        requireBusinessVoidContext(context);
        EdhrRecordChangeRespVO event = batchVoidEffectService.handleVoidBatchExecutionApprovalCallback(
                requiredProcessInstanceId(request), null, "APPROVED", null, actorUserId);
        return BusinessApprovalEffectResult.completed(resultState(event, "approved batch void"));
    }

    @Override
    public BusinessApprovalEffectResult reject(BusinessApprovalContext context, BusinessApprovalRequest request,
            Long actorUserId, String reason) {
        requireBusinessVoidContext(context);
        EdhrRecordChangeRespVO event = batchVoidEffectService.handleVoidBatchExecutionApprovalCallback(
                requiredProcessInstanceId(request), null, "REJECTED", reason, actorUserId);
        return BusinessApprovalEffectResult.rejected(resultState(event, "rejected batch void"));
    }

    @Override
    public BusinessApprovalEffectResult cancel(BusinessApprovalContext context, BusinessApprovalRequest request,
            Long actorUserId, String reason) {
        requireBusinessVoidContext(context);
        EdhrRecordChangeRespVO event = batchVoidEffectService.handleVoidBatchExecutionApprovalCallback(
                requiredProcessInstanceId(request), null, "CANCELLED",
                requiredCloseReason(reason, "Missing eDHR batch void cancellation reason"), actorUserId);
        return BusinessApprovalEffectResult.cancelled(resultState(event, "cancelled batch void"));
    }

    @Override
    public FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey) {
        if (!supports(instance)) {
            return FormBusinessEffectResult.failure(
                    "EDHR_BATCH_VOID executor only accepts MES EDHR_BATCH_EXECUTION VOID actions");
        }
        try {
            EdhrRecordChangeRespVO event;
            if (instance.getBpmBinding() == null || StrUtil.isBlank(instance.getBpmBinding().getProcessInstanceId())) {
                event = batchVoidEffectService.executeDirectPlatformVoidBatchExecution(toRequest(instance),
                        instance.getApplicantUserId());
            } else {
                event = batchVoidEffectService.handleVoidBatchExecutionApprovalCallback(requiredProcessInstanceId(instance),
                        null, "APPROVED", null, instance.getApplicantUserId());
            }
            if (event == null || event.getId() == null) {
                throw new IllegalStateException("Missing eDHR batch void domain event id");
            }
            return FormBusinessEffectResult.success(String.valueOf(event.getId()));
        } catch (RuntimeException ex) {
            return FormBusinessEffectResult.failure(ex.getMessage());
        }
    }

    @Override
    public boolean supports(FormActionInstance instance) {
        BusinessActionContext context = instance == null ? null : instance.getBusinessContext();
        return context != null
                && "MES".equals(context.getSystemCode())
                && "EDHR_BATCH_EXECUTION".equals(context.getObjectType())
                && "VOID".equals(context.getActionCode())
                && StrUtil.isNotBlank(context.getObjectState());
    }

    @Override
    public FormBusinessEffectPrecheck preflight(FormActionInstance instance) {
        if (!supports(instance)) {
            return FormBusinessEffectPrecheck.fail(LIFECYCLE_CONTEXT_ERROR);
        }
        try {
            batchVoidEffectService.precheckPlatformVoidBatchExecution(toRequest(instance));
            return FormBusinessEffectPrecheck.pass();
        } catch (RuntimeException ex) {
            return FormBusinessEffectPrecheck.fail(ex.getMessage());
        }
    }

    @Override
    public void onPendingApprovalStarted(FormActionInstance instance) {
        if (!supports(instance)) {
            throw new IllegalArgumentException(LIFECYCLE_CONTEXT_ERROR);
        }
        batchVoidEffectService.requestPlatformVoidBatchExecution(toRequest(instance), requiredProcessInstanceId(instance));
    }

    @Override
    public void onPendingApprovalClosed(FormActionInstance instance, FormControlledActionApprovalOutcome outcome,
            String reason) {
        if (outcome == null) {
            throw new IllegalArgumentException("EDHR_BATCH_VOID lifecycle outcome is required");
        }
        if (!supports(instance)) {
            throw new IllegalArgumentException(LIFECYCLE_CONTEXT_ERROR);
        }
        if (outcome == FormControlledActionApprovalOutcome.REJECTED) {
            String rejectReason = StrUtil.blankToDefault(reason, StrUtil.blankToDefault(
                    optionalString(instance.getFormData(), "comment"), requiredString(instance.getFormData(), "reasonText")));
            batchVoidEffectService.handleVoidBatchExecutionApprovalCallback(requiredProcessInstanceId(instance),
                    null, "REJECTED", rejectReason, instance.getApplicantUserId());
            return;
        }
        if (outcome == FormControlledActionApprovalOutcome.CANCELLED) {
            batchVoidEffectService.handleVoidBatchExecutionApprovalCallback(requiredProcessInstanceId(instance),
                    null, "CANCELLED",
                    requiredCloseReason(reason, "Missing eDHR batch void cancellation reason"),
                    instance.getApplicantUserId());
        }
    }

    private EdhrRecordChangeRequestReqVO toRequest(FormActionInstance instance) {
        return new EdhrRecordChangeRequestReqVO()
                .setBatchExecutionId(requiredBatchExecutionId(instance))
                .setReasonCategory(requiredString(instance.getFormData(), "reasonCategory"))
                .setReasonText(requiredString(instance.getFormData(), "reasonText"))
                .setPassword(requiredString(instance.getFormData(), "password"))
                .setComment(optionalString(instance.getFormData(), "comment"));
    }

    private EdhrRecordChangeRequestReqVO toRequest(BusinessApprovalContext context) {
        return new EdhrRecordChangeRequestReqVO()
                .setBatchExecutionId(requiredBatchExecutionId(context))
                .setReasonCategory(requiredBusinessVariableString(context, "reasonCategory"))
                .setReasonText(requiredBusinessVariableString(context, "reasonText"))
                .setPassword(requiredBusinessTransientVariableString(context, "password"))
                .setComment(optionalBusinessVariableString(context, "comment"));
    }

    private Long requiredBatchExecutionId(FormActionInstance instance) {
        Long contextBatchExecutionId = Long.valueOf(requiredObjectId(instance));
        Long formBatchExecutionId = requiredLong(instance.getFormData(), "batchExecutionId");
        if (!contextBatchExecutionId.equals(formBatchExecutionId)) {
            throw new IllegalArgumentException("EDHR batch void batchExecutionId must match context objectId");
        }
        return contextBatchExecutionId;
    }

    private Long requiredBatchExecutionId(BusinessApprovalContext context) {
        Long contextBatchExecutionId = Long.valueOf(requiredObjectId(context));
        Long variableBatchExecutionId = requiredBusinessVariableLong(context, "batchExecutionId");
        if (!contextBatchExecutionId.equals(variableBatchExecutionId)) {
            throw businessContextInvalid("EDHR batch void batchExecutionId must match context objectId");
        }
        return contextBatchExecutionId;
    }

    private Long requiredLong(Map<String, Object> formData, String key) {
        Object value = formData == null ? null : formData.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing eDHR batch void form field: " + key);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String requiredString(Map<String, Object> formData, String key) {
        Object value = formData == null ? null : formData.get(key);
        String text = value == null ? null : String.valueOf(value);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Missing eDHR batch void form field: " + key);
        }
        return text;
    }

    private String optionalString(Map<String, Object> formData, String key) {
        Object value = formData == null ? null : formData.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private String requiredCloseReason(String reason, String message) {
        String text = StrUtil.trimToNull(reason);
        if (text == null) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private void requireBusinessVoidContext(BusinessApprovalContext context) {
        if (context == null
                || !"MES".equals(context.getSystemCode())
                || !"EDHR_BATCH_EXECUTION".equals(context.getObjectType())
                || !"VOID".equals(context.getActionCode())
                || StrUtil.isBlank(context.getObjectState())) {
            throw businessContextInvalid(
                    "EDHR_BATCH_VOID business approval only accepts MES EDHR_BATCH_EXECUTION VOID actions");
        }
        requiredObjectId(context);
    }

    private String resultState(EdhrRecordChangeRespVO event, String operation) {
        if (event == null || event.getId() == null || StrUtil.isBlank(event.getChangeStatus())) {
            throw businessContextInvalid("Missing eDHR batch void domain event result: " + operation);
        }
        return event.getChangeStatus();
    }

    private Long requiredBusinessVariableLong(BusinessApprovalContext context, String key) {
        Object value = context.getVariables() == null ? null : context.getVariables().get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw businessContextInvalid("Missing eDHR batch void business approval variable: " + key);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (RuntimeException ex) {
            throw businessContextInvalid("Invalid eDHR batch void business approval variable: " + key);
        }
    }

    private String requiredBusinessVariableString(BusinessApprovalContext context, String key) {
        Object value = context.getVariables() == null ? null : context.getVariables().get(key);
        String text = value == null ? null : String.valueOf(value);
        if (text == null || text.isBlank()) {
            throw businessContextInvalid("Missing eDHR batch void business approval variable: " + key);
        }
        return text;
    }

    private String optionalBusinessVariableString(BusinessApprovalContext context, String key) {
        Object value = context.getVariables() == null ? null : context.getVariables().get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private String requiredBusinessTransientVariableString(BusinessApprovalContext context, String key) {
        Object value = context.getTransientVariables() == null ? null : context.getTransientVariables().get(key);
        String text = value == null ? null : String.valueOf(value);
        if (text == null || text.isBlank()) {
            throw businessContextInvalid("Missing eDHR batch void transient business approval variable: " + key);
        }
        return text;
    }

    private Long requiredBusinessActorUserId(Long actorUserId, String fieldName) {
        if (actorUserId == null) {
            throw businessContextInvalid("Missing eDHR batch void business approval actor: " + fieldName);
        }
        return actorUserId;
    }

    private String requiredObjectId(FormActionInstance instance) {
        String objectId = instance.getBusinessContext() == null ? null : instance.getBusinessContext().getObjectId();
        if (StrUtil.isBlank(objectId)) {
            throw new IllegalArgumentException("Missing eDHR batch void objectId");
        }
        return objectId;
    }

    private String requiredObjectId(BusinessApprovalContext context) {
        String objectId = context == null ? null : context.getObjectId();
        if (StrUtil.isBlank(objectId)) {
            throw businessContextInvalid("Missing eDHR batch void objectId");
        }
        try {
            Long.valueOf(objectId);
        } catch (RuntimeException ex) {
            throw businessContextInvalid("Invalid eDHR batch void objectId: " + objectId);
        }
        return objectId;
    }

    private String requiredProcessInstanceId(FormActionInstance instance) {
        if (instance.getBpmBinding() == null || StrUtil.isBlank(instance.getBpmBinding().getProcessInstanceId())) {
            throw new IllegalArgumentException("Missing eDHR batch void BPM processInstanceId");
        }
        return instance.getBpmBinding().getProcessInstanceId();
    }

    private String requiredProcessInstanceId(BusinessApprovalRequest request) {
        String processInstanceId = request == null ? null : request.getProcessInstanceId();
        if (StrUtil.isBlank(processInstanceId)) {
            throw businessContextInvalid("Missing eDHR batch void BPM processInstanceId");
        }
        return StrUtil.trim(processInstanceId);
    }

    private BusinessApprovalException businessContextInvalid(String message) {
        return new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, message);
    }
}
