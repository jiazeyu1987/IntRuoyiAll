package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormPolicyPublishServiceTest {

    @Test
    void policyPublishFailsFastWhenEffectExecutorIsMissing() {
        FormPolicyPublishService service = new FormPolicyPublishService(Set.of("DCC_UPLOAD"));
        FormActionPolicy policy = policy(null);

        FormCenterException ex = assertThrows(FormCenterException.class, () -> service.publish(policy));

        assertEquals(FormCenterErrorCode.EFFECT_EXECUTOR_MISSING, ex.getErrorCode());
    }

    @Test
    void policyPublishFailsFastWhenExecutorIsNotRegistered() {
        FormPolicyPublishService service = new FormPolicyPublishService(Set.of("DCC_UPLOAD"));
        FormActionPolicy policy = policy("UNKNOWN_EXECUTOR");

        FormCenterException ex = assertThrows(FormCenterException.class, () -> service.publish(policy));

        assertEquals(FormCenterErrorCode.EFFECT_EXECUTOR_MISSING, ex.getErrorCode());
    }

    @Test
    void policyPublishReturnsPublishedPolicyForResolver() {
        FormPolicyPublishService service = new FormPolicyPublishService(Set.of("DCC_UPLOAD"));
        FormActionPolicy published = service.publish(policy("DCC_UPLOAD"));

        assertEquals(FormActionPolicy.STATUS_PUBLISHED, published.getStatus());
    }

    private static FormActionPolicy policy(String executorCode) {
        return FormActionPolicy.builder()
                .policyId(10L)
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .actionCode("UPLOAD")
                .objectState("DRAFT")
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey("form-change-approval")
                .effectExecutorCode(executorCode)
                .status("DRAFT")
                .slots(List.of(FormPolicySlot.required("change-request",
                        FormTemplateVersionRef.of(1L, "CHANGE", "V1", "Change Form"))))
                .build();
    }

}
