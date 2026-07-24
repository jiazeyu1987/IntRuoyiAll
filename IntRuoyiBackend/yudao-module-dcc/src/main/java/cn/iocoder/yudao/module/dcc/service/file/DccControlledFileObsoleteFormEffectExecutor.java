package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DccControlledFileObsoleteFormEffectExecutor
        implements FormBusinessEffectExecutor, FormControlledActionLifecycleAdapter {

    public static final String EXECUTOR_CODE = "DCC_OBSOLETE";
    private static final String LIFECYCLE_CONTEXT_ERROR =
            "DCC_OBSOLETE lifecycle adapter only accepts DCC CONTROLLED_FILE OBSOLETE actions";

    private final DccControlledFileObsoleteService obsoleteService;

    public DccControlledFileObsoleteFormEffectExecutor(DccControlledFileObsoleteService obsoleteService) {
        this.obsoleteService = obsoleteService;
    }

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    @Override
    public FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey) {
        if (!supports(instance)) {
            return FormBusinessEffectResult.failure("DCC_OBSOLETE executor only accepts DCC CONTROLLED_FILE OBSOLETE actions");
        }
        try {
            Long controlledFileId = requiredControlledFileId(instance);
            obsoleteService.applyApprovedObsoleteControlledFile(instance.getApplicantUserId(), controlledFileId,
                    toObsoleteReqVO(instance.getFormData(), controlledFileId));
            return FormBusinessEffectResult.success(String.valueOf(controlledFileId));
        } catch (RuntimeException ex) {
            return FormBusinessEffectResult.failure(ex.getMessage());
        }
    }

    @Override
    public boolean supports(FormActionInstance instance) {
        return instance != null && isDccObsolete(instance.getBusinessContext());
    }

    @Override
    public FormBusinessEffectPrecheck preflight(FormActionInstance instance) {
        if (!supports(instance)) {
            return FormBusinessEffectPrecheck.fail(LIFECYCLE_CONTEXT_ERROR);
        }
        try {
            Long controlledFileId = requiredControlledFileId(instance);
            obsoleteService.precheckObsoleteControlledFile(instance.getApplicantUserId(), controlledFileId,
                    toObsoleteReqVO(instance.getFormData(), controlledFileId));
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
        Long controlledFileId = requiredControlledFileId(instance);
        toObsoleteReqVO(instance.getFormData(), controlledFileId);
    }

    @Override
    public void onPendingApprovalClosed(FormActionInstance instance, FormControlledActionApprovalOutcome outcome,
            String reason) {
        if (outcome == null) {
            throw new IllegalArgumentException("DCC_OBSOLETE lifecycle outcome is required");
        }
        if (!supports(instance)) {
            throw new IllegalArgumentException(LIFECYCLE_CONTEXT_ERROR);
        }
        Long controlledFileId = requiredControlledFileId(instance);
        toObsoleteReqVO(instance.getFormData(), controlledFileId);
    }

    private void requirePreflightPassed(FormActionInstance instance) {
        FormBusinessEffectPrecheck precheck = preflight(instance);
        if (!precheck.isPassed()) {
            throw new IllegalArgumentException(precheck.getFailureReason());
        }
    }

    private boolean isDccObsolete(BusinessActionContext context) {
        return context != null
                && "DCC".equals(context.getSystemCode())
                && "CONTROLLED_FILE".equals(context.getObjectType())
                && "OBSOLETE".equals(context.getActionCode())
                && "ACTIVE".equals(context.getObjectState());
    }

    private Long requiredControlledFileId(FormActionInstance instance) {
        String objectId = instance.getBusinessContext() == null ? null : instance.getBusinessContext().getObjectId();
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException("Missing DCC obsolete objectId");
        }
        return Long.valueOf(objectId);
    }

    private DccControlledFileObsoleteReqVO toObsoleteReqVO(Map<String, Object> formData, Long expectedControlledFileId) {
        String reason = requiredString(formData, "reason");
        Long formControlledFileId = requiredLong(formData, "controlledFileId");
        if (!formControlledFileId.equals(expectedControlledFileId)) {
            throw new IllegalArgumentException("DCC obsolete controlledFileId must match context objectId");
        }
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason(reason);
        return reqVO;
    }

    private Long requiredLong(Map<String, Object> formData, String key) {
        Object value = formData == null ? null : formData.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing DCC obsolete form field: " + key);
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
            throw new IllegalArgumentException("Missing DCC obsolete form field: " + key);
        }
        return text;
    }
}
