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
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DccControlledFileObsoleteFormEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileObsoleteService obsoleteService;

    @InjectMocks
    private DccControlledFileObsoleteFormEffectExecutor executor;

    @Test
    void lifecyclePreflightValidatesDccObsoleteBeforeBpmStarts() {
        FormActionInstance instance = dccObsoleteInstance();

        FormBusinessEffectPrecheck precheck = executor.preflight(instance);

        assertTrue(precheck.isPassed());
        verify(obsoleteService).precheckObsoleteControlledFile(eq(99L), eq(900L), any());
        verify(obsoleteService, never()).applyApprovedObsoleteControlledFile(any(), any(), any());
    }

    @Test
    void lifecyclePreflightRejectsWrongContextAndMissingReason() {
        FormActionInstance wrongContext = instance("DCC", "CONTROLLED_FILE", "UPLOAD", "ACTIVE");
        wrongContext.setFormData(formData("no longer effective"));

        FormBusinessEffectPrecheck wrongContextResult = executor.preflight(wrongContext);

        assertFalse(wrongContextResult.isPassed());
        assertEquals("DCC_OBSOLETE lifecycle adapter only accepts DCC CONTROLLED_FILE OBSOLETE actions",
                wrongContextResult.getFailureReason());

        FormActionInstance missingReason = dccObsoleteInstance();
        missingReason.setFormData(Map.of("controlledFileId", 900L));

        FormBusinessEffectPrecheck missingReasonResult = executor.preflight(missingReason);

        assertFalse(missingReasonResult.isPassed());
        assertEquals("Missing DCC obsolete form field: reason", missingReasonResult.getFailureReason());
        verify(obsoleteService, never()).applyApprovedObsoleteControlledFile(any(), any(), any());
    }

    @Test
    void lifecyclePreflightRejectsMismatchedControlledFileIdentity() {
        FormActionInstance instance = dccObsoleteInstance();
        Map<String, Object> formData = new LinkedHashMap<>(instance.getFormData());
        formData.put("controlledFileId", 901L);
        instance.setFormData(formData);

        FormBusinessEffectPrecheck result = executor.preflight(instance);

        assertFalse(result.isPassed());
        assertEquals("DCC obsolete controlledFileId must match context objectId", result.getFailureReason());
        verify(obsoleteService, never()).precheckObsoleteControlledFile(any(), any(), any());
    }

    @Test
    void executeDccObsolete_appliesExistingDomainServiceAfterApprovalOnly() {
        FormActionInstance instance = dccObsoleteInstance();

        FormBusinessEffectResult result = executor.execute(instance, "IDEM-DCC-OBSOLETE-1");

        assertTrue(result.isSuccess());
        assertEquals("900", result.getResultRef());
        ArgumentCaptor<DccControlledFileObsoleteReqVO> reqCaptor =
                ArgumentCaptor.forClass(DccControlledFileObsoleteReqVO.class);
        verify(obsoleteService).applyApprovedObsoleteControlledFile(eq(99L), eq(900L), reqCaptor.capture());
        assertEquals("no longer effective", reqCaptor.getValue().getReason());
    }

    @Test
    void executeDccObsolete_domainFailureBecomesQueryableEffectFailure() {
        FormActionInstance instance = dccObsoleteInstance();
        doThrow(new IllegalStateException("obsolete domain effect failed"))
                .when(obsoleteService).applyApprovedObsoleteControlledFile(eq(99L), eq(900L), any());

        FormBusinessEffectResult result = executor.execute(instance, "IDEM-DCC-OBSOLETE-1");

        assertFalse(result.isSuccess());
        assertEquals("obsolete domain effect failed", result.getFailureReason());
    }

    @Test
    void pendingClosureRejectedOrWithdrawnDoesNotApplyDomainObsolete() {
        FormActionInstance instance = dccObsoleteInstance();

        executor.onPendingApprovalClosed(instance, FormControlledActionApprovalOutcome.REJECTED, "reject");

        verify(obsoleteService, never()).precheckObsoleteControlledFile(any(), any(), any());
        verify(obsoleteService, never()).applyApprovedObsoleteControlledFile(any(), any(), any());
    }

    @Test
    void pendingStartedValidatesFormIdentityWithoutRecheckingActiveDomainState() {
        FormActionInstance instance = dccObsoleteInstance();

        executor.onPendingApprovalStarted(instance);

        verify(obsoleteService, never()).precheckObsoleteControlledFile(any(), any(), any());
        verify(obsoleteService, never()).applyApprovedObsoleteControlledFile(any(), any(), any());
    }

    @Test
    void pendingClosureAfterApprovedEffectDoesNotRecheckActiveDomainState() {
        FormActionInstance instance = dccObsoleteInstance();

        executor.onPendingApprovalClosed(instance, FormControlledActionApprovalOutcome.EFFECTIVE, null);

        verify(obsoleteService, never()).precheckObsoleteControlledFile(any(), any(), any());
        verify(obsoleteService, never()).applyApprovedObsoleteControlledFile(any(), any(), any());
    }

    private FormActionInstance dccObsoleteInstance() {
        FormActionInstance instance = instance("DCC", "CONTROLLED_FILE", "OBSOLETE", "ACTIVE");
        instance.setFormData(formData("no longer effective"));
        return instance;
    }

    private FormActionInstance instance(String systemCode, String objectType, String actionCode, String objectState) {
        FormActionPolicy policy = FormActionPolicy.builder()
                .policyId(21L)
                .tenantId(122L)
                .dataDomain(systemCode)
                .systemCode(systemCode)
                .objectType(objectType)
                .actionCode(actionCode)
                .objectState(objectState)
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey("dcc-controlled-file-approval")
                .effectExecutorCode("DCC_OBSOLETE")
                .status(FormActionPolicy.STATUS_PUBLISHED)
                .build();
        return new FormActionInstance("FCI-OBSOLETE-1", FormActionResolution.from(policy),
                BusinessActionContext.builder()
                        .tenantId(122L)
                        .dataDomain(systemCode)
                        .systemCode(systemCode)
                        .objectType(objectType)
                        .objectId("900")
                        .objectVersion("V1.0")
                        .actionCode(actionCode)
                        .objectState(objectState)
                        .reason("obsolete request")
                        .build(),
                99L, "IDEM-DCC-OBSOLETE-1");
    }

    private Map<String, Object> formData(String reason) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("controlledFileId", 900L);
        formData.put("reason", reason);
        return formData;
    }
}
