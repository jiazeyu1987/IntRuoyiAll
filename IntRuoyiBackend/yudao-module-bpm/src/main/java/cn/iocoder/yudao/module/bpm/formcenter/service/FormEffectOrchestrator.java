package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormEffectExecution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;

public class FormEffectOrchestrator {

    private static final String EXECUTION_PREFIX = "EFFECT-";

    private final FormBusinessEffectPrecheck precheck;
    private final FormBusinessEffectExecutor executor;

    public FormEffectOrchestrator(FormBusinessEffectPrecheck precheck, FormBusinessEffectExecutor executor) {
        this.precheck = precheck;
        this.executor = executor;
    }

    public void onTaskCompleted(FormActionInstance instance, String taskId) {
        // Business effects are deliberately bound to process completion, not task completion.
    }

    public FormEffectExecution onProcessApproved(FormActionInstance instance) {
        String idempotencyKey = instance.getIdempotencyKey();
        FormEffectExecution existingExecution = instance.findEffectExecution(idempotencyKey);
        if (existingExecution != null) {
            return existingExecution;
        }
        if (instance.getStatus() != FormInstanceStatus.IN_APPROVAL) {
            throw new FormCenterException(FormCenterErrorCode.FORM_INSTANCE_STATUS_INVALID,
                    "Process approved event requires approval-in-progress instance: " + instance.getStatus());
        }
        FormBusinessEffectPrecheck checked = precheck.check(instance);
        if (!checked.isPassed()) {
            FormEffectExecution execution = FormEffectExecution.failed(EXECUTION_PREFIX + idempotencyKey,
                    idempotencyKey, checked.getFailureReason());
            instance.addEffectExecution(execution);
            instance.setStatus(FormInstanceStatus.EFFECT_FAILED_PENDING);
            return execution;
        }
        FormBusinessEffectResult result = executor.execute(instance, idempotencyKey);
        if (!result.isSuccess()) {
            FormEffectExecution execution = FormEffectExecution.failed(EXECUTION_PREFIX + idempotencyKey,
                    idempotencyKey, result.getFailureReason());
            instance.addEffectExecution(execution);
            instance.setStatus(FormInstanceStatus.EFFECT_FAILED_PENDING);
            return execution;
        }
        FormEffectExecution execution = FormEffectExecution.applied(EXECUTION_PREFIX + idempotencyKey,
                idempotencyKey, result.getResultRef());
        instance.addEffectExecution(execution);
        instance.setStatus(FormInstanceStatus.EFFECTIVE);
        return execution;
    }

}
