package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseWithdrawReqVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MesProEdhrReleaseFormEffectExecutor
        implements FormBusinessEffectExecutor, FormControlledActionLifecycleAdapter {

    public static final String EXECUTOR_CODE = "EDHR_RELEASE";
    private static final String LIFECYCLE_CONTEXT_ERROR =
            "EDHR_RELEASE lifecycle adapter only accepts MES EDHR_BATCH_EXECUTION RELEASE actions";
    private static final String OWNER_SIGNATURE_ONLY_ERROR =
            "EDHR_RELEASE no longer supports BPM approval; use owner signature release submit";

    private final MesProEdhrReleaseService releaseService;

    public MesProEdhrReleaseFormEffectExecutor(MesProEdhrReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    @Override
    public FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey) {
        if (!supports(instance)) {
            return FormBusinessEffectResult.failure(
                    "EDHR_RELEASE executor only accepts MES EDHR_BATCH_EXECUTION RELEASE actions");
        }
        return FormBusinessEffectResult.failure(OWNER_SIGNATURE_ONLY_ERROR);
    }

    @Override
    public boolean supports(FormActionInstance instance) {
        BusinessActionContext context = instance == null ? null : instance.getBusinessContext();
        return context != null
                && "MES".equals(context.getSystemCode())
                && "EDHR_BATCH_EXECUTION".equals(context.getObjectType())
                && "RELEASE".equals(context.getActionCode())
                && "PRECHECK_PASSED".equals(context.getObjectState());
    }

    @Override
    public FormBusinessEffectPrecheck preflight(FormActionInstance instance) {
        if (!supports(instance)) {
            return FormBusinessEffectPrecheck.fail(LIFECYCLE_CONTEXT_ERROR);
        }
        return FormBusinessEffectPrecheck.fail(OWNER_SIGNATURE_ONLY_ERROR);
    }

    @Override
    public void onPendingApprovalStarted(FormActionInstance instance) {
        if (!supports(instance)) {
            throw new IllegalArgumentException(LIFECYCLE_CONTEXT_ERROR);
        }
        throw new IllegalStateException(OWNER_SIGNATURE_ONLY_ERROR);
    }

    @Override
    public void onPendingApprovalClosed(FormActionInstance instance, FormControlledActionApprovalOutcome outcome,
            String reason) {
        if (outcome == null) {
            throw new IllegalArgumentException("EDHR_RELEASE lifecycle outcome is required");
        }
        if (!supports(instance)) {
            throw new IllegalArgumentException(LIFECYCLE_CONTEXT_ERROR);
        }
        if (outcome == FormControlledActionApprovalOutcome.REJECTED) {
            releaseService.reject(new MesProEdhrReleaseRejectReqVO()
                    .setReleaseTransactionId(requiredReleaseTransactionId(instance))
                    .setIdempotencyKey(instance.getIdempotencyKey())
                    .setRejectReason(StrUtil.blankToDefault(reason,
                            StrUtil.blankToDefault(requiredOptionalString(instance.getFormData(), "rejectReason"), null))));
            return;
        }
        if (outcome == FormControlledActionApprovalOutcome.CANCELLED) {
            releaseService.withdraw(new MesProEdhrReleaseWithdrawReqVO()
                    .setReleaseTransactionId(requiredReleaseTransactionId(instance))
                    .setIdempotencyKey(instance.getIdempotencyKey())
                    .setWithdrawReason(requiredCloseReason(reason, "Missing eDHR release cancellation reason")));
        }
    }

    private Long requiredBatchExecutionId(FormActionInstance instance) {
        Long contextBatchExecutionId = Long.valueOf(requiredObjectId(instance));
        Long formBatchExecutionId = requiredLong(instance.getFormData(), "batchExecutionId");
        if (!contextBatchExecutionId.equals(formBatchExecutionId)) {
            throw new IllegalArgumentException("EDHR release batchExecutionId must match context objectId");
        }
        return contextBatchExecutionId;
    }

    private Long requiredReleaseTransactionId(FormActionInstance instance) {
        requiredBatchExecutionId(instance);
        return requiredLong(instance.getFormData(), "releaseTransactionId");
    }

    private Long requiredLong(Map<String, Object> formData, String key) {
        Object value = formData == null ? null : formData.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing eDHR release form field: " + key);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String requiredOptionalString(Map<String, Object> formData, String key) {
        Object value = formData == null ? null : formData.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String requiredCloseReason(String reason, String message) {
        String text = StrUtil.trimToNull(reason);
        if (text == null) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private String requiredObjectId(FormActionInstance instance) {
        String objectId = instance.getBusinessContext() == null ? null : instance.getBusinessContext().getObjectId();
        if (StrUtil.isBlank(objectId)) {
            throw new IllegalArgumentException("Missing eDHR release objectId");
        }
        return objectId;
    }
}
