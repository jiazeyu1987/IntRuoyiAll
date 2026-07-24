package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormBpmStartRequest;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormBpmBindingServiceTest {

    @Test
    void submitFailsFastWhenPolicyRequiresBpmButProcessKeyIsMissing() {
        RecordingBpmStarter starter = new RecordingBpmStarter("proc-001");
        FormBpmBindingService service = new FormBpmBindingService(starter);
        FormActionInstance instance = instance(policy(null));

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.startNewProcess(instance, 501L));

        assertEquals(FormCenterErrorCode.BPM_BINDING_MISSING, ex.getErrorCode());
        assertEquals(0, starter.startCount);
    }

    @Test
    void submitStartsBpmWithStableBusinessKeyAndContextVariables() {
        RecordingBpmStarter starter = new RecordingBpmStarter("proc-001");
        FormBpmBindingService service = new FormBpmBindingService(starter);
        FormActionInstance instance = instance(policy("form-change-approval"));

        String processInstanceId = service.startNewProcess(instance, 501L);

        assertEquals("proc-001", processInstanceId);
        assertEquals("proc-001", instance.getBpmBinding().getProcessInstanceId());
        assertEquals("form-change-approval", starter.lastRequest.getProcessDefinitionKey());
        assertEquals("FORM_ACTION:" + instance.getInstanceCode(), starter.lastRequest.getBusinessKey());
        assertEquals("FILE-1001", starter.lastRequest.getVariables().get("objectId"));
        assertEquals("UPLOAD", starter.lastRequest.getVariables().get("actionCode"));
    }

    @Test
    void reworkSubmitReusesExistingBpmProcess() {
        RecordingBpmStarter starter = new RecordingBpmStarter("proc-001");
        FormBpmBindingService service = new FormBpmBindingService(starter);
        FormActionInstance instance = instance(policy("form-change-approval"));
        service.startNewProcess(instance, 501L);

        String processInstanceId = service.reuseProcessForRework(instance, 501L, "task-001");

        assertEquals("proc-001", processInstanceId);
        assertEquals(1, starter.startCount);
    }

    private static FormActionInstance instance(FormActionPolicy policy) {
        return new FormInstanceLifecycleService().createDraft(FormActionResolution.from(policy), context(), 501L, "idem-001");
    }

    private static FormActionPolicy policy(String bpmKey) {
        return FormActionPolicy.builder()
                .policyId(10L)
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .actionCode("UPLOAD")
                .objectState("DRAFT")
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey(bpmKey)
                .effectExecutorCode("DCC_UPLOAD")
                .status("PUBLISHED")
                .slots(List.of(FormPolicySlot.required("change-request",
                        FormTemplateVersionRef.of(1L, "CHANGE", "V1", "Change Form"))))
                .build();
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

    private static final class RecordingBpmStarter implements FormBpmStarter {
        private final String processInstanceId;
        private int startCount;
        private FormBpmStartRequest lastRequest;

        private RecordingBpmStarter(String processInstanceId) {
            this.processInstanceId = processInstanceId;
        }

        @Override
        public String start(Long userId, FormBpmStartRequest request) {
            this.startCount++;
            this.lastRequest = request;
            return processInstanceId;
        }
    }
}

