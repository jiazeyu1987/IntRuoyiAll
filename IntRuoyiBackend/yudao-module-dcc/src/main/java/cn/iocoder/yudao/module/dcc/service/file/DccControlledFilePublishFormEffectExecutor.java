package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DccControlledFilePublishFormEffectExecutor
        implements FormBusinessEffectExecutor, FormControlledActionLifecycleAdapter {

    public static final String EXECUTOR_CODE = "DCC_PUBLISH";
    private static final String LIFECYCLE_CONTEXT_ERROR =
            "DCC_PUBLISH lifecycle adapter only accepts DCC CONTROLLED_FILE PUBLISH actions in READY_TO_PUBLISH state";

    private final DccControlledFileFinalizationService finalizationService;

    public DccControlledFilePublishFormEffectExecutor(DccControlledFileFinalizationService finalizationService) {
        this.finalizationService = finalizationService;
    }

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    @Override
    public FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey) {
        if (!supports(instance)) {
            return FormBusinessEffectResult.failure("DCC_PUBLISH executor only accepts DCC CONTROLLED_FILE PUBLISH actions");
        }
        try {
            Long controlledFileId = requiredControlledFileId(instance);
            finalizationService.applyApprovedPublishControlledFile(instance.getApplicantUserId(), controlledFileId,
                    idempotencyKey);
            return FormBusinessEffectResult.success(String.valueOf(controlledFileId));
        } catch (RuntimeException ex) {
            return FormBusinessEffectResult.failure(ex.getMessage());
        }
    }

    @Override
    public boolean supports(FormActionInstance instance) {
        return instance != null && isDccPublish(instance.getBusinessContext());
    }

    @Override
    public FormBusinessEffectPrecheck preflight(FormActionInstance instance) {
        if (!supports(instance)) {
            return FormBusinessEffectPrecheck.fail(LIFECYCLE_CONTEXT_ERROR);
        }
        try {
            Long controlledFileId = requiredControlledFileId(instance);
            finalizationService.precheckPublishControlledFile(instance.getApplicantUserId(), controlledFileId);
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
        requiredControlledFileId(instance);
    }

    @Override
    public void onPendingApprovalClosed(FormActionInstance instance, FormControlledActionApprovalOutcome outcome,
            String reason) {
        if (outcome == null) {
            throw new IllegalArgumentException("DCC_PUBLISH lifecycle outcome is required");
        }
        if (!supports(instance)) {
            throw new IllegalArgumentException(LIFECYCLE_CONTEXT_ERROR);
        }
        requiredControlledFileId(instance);
    }

    private boolean isDccPublish(BusinessActionContext context) {
        return context != null
                && "DCC".equals(context.getSystemCode())
                && "CONTROLLED_FILE".equals(context.getObjectType())
                && "PUBLISH".equals(context.getActionCode())
                && "READY_TO_PUBLISH".equals(context.getObjectState());
    }

    private Long requiredControlledFileId(FormActionInstance instance) {
        String objectId = instance.getBusinessContext() == null ? null : instance.getBusinessContext().getObjectId();
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException("Missing DCC publish objectId");
        }
        Long contextControlledFileId = Long.valueOf(objectId);
        Long formControlledFileId = requiredLong(instance.getFormData(), "controlledFileId");
        if (!contextControlledFileId.equals(formControlledFileId)) {
            throw new IllegalArgumentException("DCC publish controlledFileId must match context objectId");
        }
        return contextControlledFileId;
    }

    private Long requiredLong(Map<String, Object> formData, String key) {
        Object value = formData == null ? null : formData.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing DCC publish form field: " + key);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
