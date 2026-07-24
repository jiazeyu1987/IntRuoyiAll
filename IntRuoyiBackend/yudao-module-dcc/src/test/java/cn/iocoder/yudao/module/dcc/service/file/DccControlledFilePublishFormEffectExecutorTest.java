package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DccControlledFilePublishFormEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileFinalizationService finalizationService;

    @InjectMocks
    private DccControlledFilePublishFormEffectExecutor executor;

    @Test
    void executeDccPublish_runsApprovedFinalizationForReadyCandidate() {
        FormActionInstance instance = dccPublishInstance();

        FormBusinessEffectResult result = executor.execute(instance, "IDEM-PUBLISH-1");

        assertTrue(result.isSuccess());
        assertEquals("920", result.getResultRef());
        verify(finalizationService).applyApprovedPublishControlledFile(99L, 920L, "IDEM-PUBLISH-1");
    }

    @Test
    void lifecyclePreflightValidatesReadyCandidateBeforeBpmStarts() {
        FormBusinessEffectPrecheck precheck = executor.preflight(dccPublishInstance());

        assertTrue(precheck.isPassed());
        verify(finalizationService).precheckPublishControlledFile(99L, 920L);
    }

    @Test
    void lifecyclePreflightRejectsWrongContextAndDomainFailure() {
        FormActionInstance wrongState = instance("DCC", "CONTROLLED_FILE", "PUBLISH",
                "ACTIVE", "DCC_PUBLISH");
        wrongState.setFormData(publishFormData());

        FormBusinessEffectPrecheck wrongStateResult = executor.preflight(wrongState);

        assertFalse(wrongStateResult.isPassed());
        assertEquals("DCC_PUBLISH lifecycle adapter only accepts DCC CONTROLLED_FILE PUBLISH actions in READY_TO_PUBLISH state",
                wrongStateResult.getFailureReason());
        verify(finalizationService, never()).precheckPublishControlledFile(99L, 920L);

        FormActionInstance domainRejected = dccPublishInstance();
        doThrow(new IllegalArgumentException("candidate is not ready to publish"))
                .when(finalizationService).precheckPublishControlledFile(99L, 920L);

        FormBusinessEffectPrecheck domainResult = executor.preflight(domainRejected);

        assertFalse(domainResult.isPassed());
        assertEquals("candidate is not ready to publish", domainResult.getFailureReason());
    }

    @Test
    void pendingApprovalStartedOnlyValidatesPublishContextWithoutSelfLockPrecheck() {
        FormActionInstance instance = dccPublishInstance();

        assertDoesNotThrow(() -> executor.onPendingApprovalStarted(instance));
        verify(finalizationService, never()).precheckPublishControlledFile(99L, 920L);
    }

    @Test
    void pendingApprovalClosedOnlyValidatesContextAndOutcome() {
        FormActionInstance instance = dccPublishInstance();

        executor.onPendingApprovalClosed(instance, FormControlledActionApprovalOutcome.REJECTED, "reject");

        verify(finalizationService, never()).precheckPublishControlledFile(99L, 920L);
        verify(finalizationService, never()).applyApprovedPublishControlledFile(99L, 920L, "IDEM-PUBLISH-1");
    }

    private FormActionInstance dccPublishInstance() {
        FormActionInstance instance = instance("DCC", "CONTROLLED_FILE", "PUBLISH",
                "READY_TO_PUBLISH", "DCC_PUBLISH");
        instance.setFormData(publishFormData());
        return instance;
    }

    private FormActionInstance instance(String systemCode, String objectType, String actionCode,
                                        String objectState, String executorCode) {
        FormActionPolicy policy = FormActionPolicy.builder()
                .policyId(30L)
                .tenantId(122L)
                .dataDomain(systemCode)
                .systemCode(systemCode)
                .objectType(objectType)
                .actionCode(actionCode)
                .objectState(objectState)
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey("form-change-approval")
                .effectExecutorCode(executorCode)
                .status(FormActionPolicy.STATUS_PUBLISHED)
                .build();
        return new FormActionInstance("FCI-PUBLISH-1", FormActionResolution.from(policy),
                BusinessActionContext.builder()
                        .tenantId(122L)
                        .dataDomain(systemCode)
                        .systemCode(systemCode)
                        .objectType(objectType)
                        .objectId("920")
                        .objectVersion("V2.0")
                        .actionCode(actionCode)
                        .objectState(objectState)
                        .build(),
                99L, "IDEM-PUBLISH-1");
    }

    private Map<String, Object> publishFormData() {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("controlledFileId", 920L);
        formData.put("publishReason", "release approved revision");
        return formData;
    }
}
