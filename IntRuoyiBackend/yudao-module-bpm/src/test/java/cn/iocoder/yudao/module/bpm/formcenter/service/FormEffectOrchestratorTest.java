package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormEffectExecution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormEffectStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormEffectOrchestratorTest {

    @Test
    void taskCompletedDoesNotTriggerBusinessEffect() {
        RecordingExecutor executor = new RecordingExecutor(true);
        FormEffectOrchestrator orchestrator = new FormEffectOrchestrator(
                FormBusinessEffectPrecheck.pass(), executor);
        FormActionInstance instance = instance();

        orchestrator.onTaskCompleted(instance, "task-001");

        assertEquals(FormInstanceStatus.DRAFT, instance.getStatus());
        assertEquals(0, executor.executeCount);
    }

    @Test
    void processApprovedTriggersEffectOnceWithSameIdempotencyKey() {
        RecordingExecutor executor = new RecordingExecutor(true);
        FormEffectOrchestrator orchestrator = new FormEffectOrchestrator(
                FormBusinessEffectPrecheck.pass(), executor);
        FormActionInstance instance = instance();
        instance.setStatus(FormInstanceStatus.IN_APPROVAL);

        FormEffectExecution first = orchestrator.onProcessApproved(instance);
        FormEffectExecution second = orchestrator.onProcessApproved(instance);

        assertEquals(FormEffectStatus.APPLIED, first.getStatus());
        assertEquals(FormEffectStatus.APPLIED, second.getStatus());
        assertEquals(first.getExecutionId(), second.getExecutionId());
        assertEquals(1, executor.executeCount);
    }

    @Test
    void processApprovedLeavesInstancePendingWhenObjectVersionChanged() {
        RecordingExecutor executor = new RecordingExecutor(true);
        FormEffectOrchestrator orchestrator = new FormEffectOrchestrator(
                FormBusinessEffectPrecheck.fail("OBJECT_VERSION_MISMATCH"), executor);
        FormActionInstance instance = instance();
        instance.setStatus(FormInstanceStatus.IN_APPROVAL);

        FormEffectExecution execution = orchestrator.onProcessApproved(instance);

        assertEquals(FormEffectStatus.FAILED_PENDING, execution.getStatus());
        assertEquals(FormInstanceStatus.EFFECT_FAILED_PENDING, instance.getStatus());
        assertEquals(0, executor.executeCount);
    }

    @Test
    void executorFailureNeverMarksInstanceApplied() {
        RecordingExecutor executor = new RecordingExecutor(false);
        FormEffectOrchestrator orchestrator = new FormEffectOrchestrator(
                FormBusinessEffectPrecheck.pass(), executor);
        FormActionInstance instance = instance();
        instance.setStatus(FormInstanceStatus.IN_APPROVAL);

        FormEffectExecution execution = orchestrator.onProcessApproved(instance);

        assertEquals(FormEffectStatus.FAILED_PENDING, execution.getStatus());
        assertEquals(FormInstanceStatus.EFFECT_FAILED_PENDING, instance.getStatus());
        assertEquals(1, executor.executeCount);
    }

    @Test
    void processApprovedRequiresApprovalInProgressStatus() {
        RecordingExecutor executor = new RecordingExecutor(true);
        FormEffectOrchestrator orchestrator = new FormEffectOrchestrator(
                FormBusinessEffectPrecheck.pass(), executor);
        FormActionInstance instance = instance();

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> orchestrator.onProcessApproved(instance));

        assertEquals(FormCenterErrorCode.FORM_INSTANCE_STATUS_INVALID, ex.getErrorCode());
        assertEquals(0, executor.executeCount);
    }

    private static FormActionInstance instance() {
        FormActionPolicy policy = FormActionPolicy.builder()
                .policyId(10L)
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .actionCode("UPLOAD")
                .objectState("DRAFT")
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey("form-change-approval")
                .effectExecutorCode("DCC_UPLOAD")
                .status("PUBLISHED")
                .slots(List.of(FormPolicySlot.required("change-request",
                        FormTemplateVersionRef.of(1L, "CHANGE", "V1", "Change Form"))))
                .build();
        BusinessActionContext context = BusinessActionContext.builder()
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .objectId("FILE-1001")
                .objectVersion("V1")
                .actionCode("UPLOAD")
                .objectState("DRAFT")
                .orgCode("ORG-QA")
                .deptCode("DCC")
                .roleCodes(List.of("doc_control"))
                .productCode("PTCA")
                .categoryCode("SOP")
                .reason("initial upload")
                .build();
        return new FormInstanceLifecycleService().createDraft(FormActionResolution.from(policy), context, 501L, "idem-001");
    }

    private static final class RecordingExecutor implements FormBusinessEffectExecutor {
        private final boolean success;
        private int executeCount;

        private RecordingExecutor(boolean success) {
            this.success = success;
        }

        @Override
        public FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey) {
            executeCount++;
            return success ? FormBusinessEffectResult.success("EFFECT-" + idempotencyKey)
                    : FormBusinessEffectResult.failure("BUSINESS_EXECUTOR_FAILED");
        }
    }
}
