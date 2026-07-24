package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormDuplicateDecision;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormDuplicateDecisionType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormDuplicateStrategy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormDuplicateRoutingServiceTest {

    @Test
    void sameApplicantDraftReturnsExistingDraft() {
        FormDuplicateRoutingService service = new FormDuplicateRoutingService(new FormInstanceLifecycleService(),
                new MemoryActionInstanceStore());
        FormDuplicateDecision first = service.resolveOrCreate(resolution(), context(), 501L, "idem-001",
                FormDuplicateStrategy.BLOCK_ACTIVE);

        FormDuplicateDecision second = service.resolveOrCreate(resolution(), context(), 501L, "idem-002",
                FormDuplicateStrategy.BLOCK_ACTIVE);

        assertEquals(FormDuplicateDecisionType.CREATED, first.getType());
        assertEquals(FormDuplicateDecisionType.RETURN_EXISTING_DRAFT, second.getType());
        assertEquals(first.getInstance().getInstanceCode(), second.getInstance().getInstanceCode());
    }

    @Test
    void activeApprovalBlocksDuplicateApplication() {
        FormDuplicateRoutingService service = new FormDuplicateRoutingService(new FormInstanceLifecycleService(),
                new MemoryActionInstanceStore());
        FormActionInstance first = service.resolveOrCreate(resolution(), context(), 501L, "idem-001",
                FormDuplicateStrategy.BLOCK_ACTIVE).getInstance();
        first.setStatus(FormInstanceStatus.IN_APPROVAL);

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.resolveOrCreate(resolution(), context(), 601L, "idem-002",
                        FormDuplicateStrategy.BLOCK_ACTIVE));

        assertEquals(FormCenterErrorCode.DUPLICATE_APPLICATION_ACTIVE, ex.getErrorCode());
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

    private static final class MemoryActionInstanceStore implements FormActionInstanceStore {

        private final List<FormActionInstance> instances = new ArrayList<>();

        @Override
        public void insert(FormActionInstance instance) {
            instances.add(instance);
        }

        @Override
        public void update(FormActionInstance instance) {
        }

        @Override
        public List<FormActionInstance> findSameBusinessAction(BusinessActionContext context) {
            return instances.stream()
                    .filter(instance -> sameBusinessAction(instance.getBusinessContext(), context))
                    .toList();
        }

        private boolean sameBusinessAction(BusinessActionContext left, BusinessActionContext right) {
            return Objects.equals(left.getTenantId(), right.getTenantId())
                    && Objects.equals(left.getSystemCode(), right.getSystemCode())
                    && Objects.equals(left.getObjectType(), right.getObjectType())
                    && Objects.equals(left.getObjectId(), right.getObjectId())
                    && Objects.equals(left.getObjectVersion(), right.getObjectVersion())
                    && Objects.equals(left.getActionCode(), right.getActionCode());
        }

    }

}
