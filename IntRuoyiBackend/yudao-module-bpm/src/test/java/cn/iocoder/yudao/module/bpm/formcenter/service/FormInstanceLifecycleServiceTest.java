package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstancePermissionCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormSnapshot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormSnapshotType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormInstanceLifecycleServiceTest {

    @Test
    void createDraftAutomaticallyGrantsApplicantInstancePermissions() {
        FormInstanceLifecycleService service = new FormInstanceLifecycleService();

        FormActionInstance instance = service.createDraft(resolution(), context(), 501L, "idem-001");

        assertEquals(FormInstanceStatus.DRAFT, instance.getStatus());
        assertTrue(instance.hasInstancePermission(501L, FormInstancePermissionCode.VIEW));
        assertTrue(instance.hasInstancePermission(501L, FormInstancePermissionCode.EDIT_DRAFT));
        assertTrue(instance.hasInstancePermission(501L, FormInstancePermissionCode.SUBMIT));
        assertTrue(instance.hasInstancePermission(501L, FormInstancePermissionCode.ABANDON));
    }

    @Test
    void snapshotsAreImmutableAndCaptureBusinessContext() {
        FormInstanceLifecycleService service = new FormInstanceLifecycleService();
        FormActionInstance instance = service.createDraft(resolution(), context(), 501L, "idem-001");
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("reason", "before");

        FormSnapshot snapshot = service.saveDraft(instance, formData, List.of("ATT-1"));
        formData.put("reason", "after");

        assertEquals(FormSnapshotType.DRAFT, snapshot.getSnapshotType());
        assertEquals("before", snapshot.getFormData().get("reason"));
        assertEquals("FILE-1001", snapshot.getBusinessContext().getObjectId());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getFormData().put("other", "value"));
    }

    @Test
    void submitAndReworkSubmitCreateSeparateSnapshotsWithoutStartingNewBpm() {
        FormInstanceLifecycleService service = new FormInstanceLifecycleService();
        FormActionInstance instance = service.createDraft(resolution(), context(), 501L, "idem-001");

        FormSnapshot submitSnapshot = service.submit(instance, Map.of("reason", "submit"), "proc-001");
        service.markReworkRequired(instance, "task-001");
        FormSnapshot reworkSnapshot = service.reworkSubmit(instance, Map.of("reason", "rework"));

        assertEquals(FormInstanceStatus.IN_APPROVAL, instance.getStatus());
        assertEquals("proc-001", instance.getBpmBinding().getProcessInstanceId());
        assertEquals(FormSnapshotType.SUBMIT, submitSnapshot.getSnapshotType());
        assertEquals(FormSnapshotType.REWORK_SUBMIT, reworkSnapshot.getSnapshotType());
        assertEquals(2, instance.getSnapshots().size());
    }

    @Test
    void abandonedDraftCannotBeSubmitted() {
        FormInstanceLifecycleService service = new FormInstanceLifecycleService();
        FormActionInstance instance = service.createDraft(resolution(), context(), 501L, "idem-001");

        service.abandon(instance);
        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.submit(instance, Map.of("reason", "submit"), "proc-001"));

        assertEquals(FormInstanceStatus.ABANDONED, instance.getStatus());
        assertEquals(FormCenterErrorCode.FORM_INSTANCE_STATUS_INVALID, ex.getErrorCode());
    }

    private static FormActionResolution resolution() {
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
        return FormActionResolution.from(policy);
    }

    private static BusinessActionContext context() {
        return BusinessActionContext.builder()
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
    }
}
