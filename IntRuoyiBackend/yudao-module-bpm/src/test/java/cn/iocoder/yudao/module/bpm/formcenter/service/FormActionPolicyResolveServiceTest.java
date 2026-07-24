package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormActionPolicyResolveServiceTest {

    @Test
    void resolveFailsFastWhenContextMissesObjectVersion() {
        FormActionPolicyResolveService service = new FormActionPolicyResolveService(List.of());

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.resolve(baseContext().objectVersion(null).build()));

        assertEquals(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("objectVersion"));
    }

    @Test
    void resolveFailsFastWhenExplicitPolicyIsMissing() {
        FormActionPolicyResolveService service = new FormActionPolicyResolveService(List.of());

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.resolve(baseContext().build()));

        assertEquals(FormCenterErrorCode.FORM_POLICY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void resolveRequiresExplicitNonePolicyForNoFormAction() {
        FormActionPolicyResolveService service = new FormActionPolicyResolveService(List.of(
                basePolicy().policyType(FormPolicyType.NONE).slots(List.of()).build()));

        FormActionResolution resolution = service.resolve(baseContext().build());

        assertEquals(FormPolicyType.NONE, resolution.getPolicyType());
        assertFalse(resolution.requiresForm());
        assertTrue(resolution.getSlots().isEmpty());
    }

    @Test
    void resolveNonePolicyWithBpmApprovalStillRequiresBpm() {
        FormActionPolicyResolveService service = new FormActionPolicyResolveService(List.of(
                basePolicy().policyType(FormPolicyType.NONE)
                        .bpmProcessKey("mes-edhr-approval-v1")
                        .slots(List.of())
                        .build()));

        FormActionResolution resolution = service.resolve(baseContext().build());

        assertEquals(FormPolicyType.NONE, resolution.getPolicyType());
        assertFalse(resolution.requiresForm());
        assertTrue(resolution.requiresBpm());
        assertEquals("mes-edhr-approval-v1", resolution.getBpmProcessKey());
    }

    @Test
    void resolveBlocksPackageWhenSameSlotMatchesMoreThanOneTemplate() {
        FormActionPolicy policy = basePolicy()
                .policyType(FormPolicyType.PACKAGE)
                .slots(List.of(
                        FormPolicySlot.required("change-request",
                                FormTemplateVersionRef.of(1L, "CHANGE", "V1", "Change Form")),
                        FormPolicySlot.required("change-request",
                                FormTemplateVersionRef.of(2L, "CHANGE-EXTRA", "V1", "Extra Change Form"))))
                .build();
        FormActionPolicyResolveService service = new FormActionPolicyResolveService(List.of(policy));

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.resolve(baseContext().build()));

        assertEquals(FormCenterErrorCode.FORM_TEMPLATE_SLOT_CONFLICT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("change-request"));
    }

    @Test
    void resolveRequiredPolicyReturnsTemplateAndBpmRequirement() {
        FormActionPolicy policy = basePolicy()
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey("form-change-approval")
                .slots(List.of(FormPolicySlot.required("change-request",
                        FormTemplateVersionRef.of(1L, "CHANGE", "V1", "Change Form"))))
                .build();
        FormActionPolicyResolveService service = new FormActionPolicyResolveService(List.of(policy));

        FormActionResolution resolution = service.resolve(baseContext().build());

        assertEquals(FormPolicyType.REQUIRED, resolution.getPolicyType());
        assertEquals("form-change-approval", resolution.getBpmProcessKey());
        assertTrue(resolution.requiresForm());
        assertEquals("change-request", resolution.getSlots().get(0).getSlotCode());
    }

    private static BusinessActionContext.Builder baseContext() {
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
                .reason("initial upload");
    }

    private static FormActionPolicy.Builder basePolicy() {
        return FormActionPolicy.builder()
                .policyId(10L)
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .actionCode("UPLOAD")
                .objectState("DRAFT")
                .effectExecutorCode("DCC_UPLOAD")
                .status("PUBLISHED");
    }
}

