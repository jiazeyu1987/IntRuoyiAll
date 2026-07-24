package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTaskPermissionCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormDerivedPermissionServiceTest {

    @Test
    void taskPermissionIsGrantedAndRevokedByActiveTaskId() {
        FormDerivedPermissionService service = new FormDerivedPermissionService();
        FormActionInstance instance = instance();

        service.grantForActiveTask(instance, "task-A", List.of(601L),
                Set.of(FormTaskPermissionCode.VIEW, FormTaskPermissionCode.APPROVE));
        service.grantForActiveTask(instance, "task-B", List.of(601L),
                Set.of(FormTaskPermissionCode.VIEW));
        service.revokeForTask(instance, "task-A");

        assertFalse(instance.hasTaskPermission("task-A", 601L, FormTaskPermissionCode.APPROVE));
        assertTrue(instance.hasTaskPermission("task-B", 601L, FormTaskPermissionCode.VIEW));
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
}

