package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseWithdrawReqVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MesProEdhrReleaseFormEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private MesProEdhrReleaseService releaseService;

    @InjectMocks
    private MesProEdhrReleaseFormEffectExecutor executor;

    @Test
    void lifecyclePreflightRejectsRetiredBpmReleaseBeforeBpmStarts() {
        FormBusinessEffectPrecheck precheck = executor.preflight(releaseInstance());

        assertFalse(precheck.isPassed());
        assertEquals("EDHR_RELEASE no longer supports BPM approval; use owner signature release submit",
                precheck.getFailureReason());
        verify(releaseService, never()).submit(any());
        verify(releaseService, never()).approve(any());
    }

    @Test
    void lifecyclePreflightRejectsWrongContextWithoutCallingDomainRelease() {
        FormActionInstance wrongContext = instance("MES", "EDHR_BATCH_EXECUTION", "VOID", "CLOSED");
        wrongContext.setFormData(releaseFormData());

        FormBusinessEffectPrecheck wrongContextResult = executor.preflight(wrongContext);

        assertFalse(wrongContextResult.isPassed());
        assertEquals("EDHR_RELEASE lifecycle adapter only accepts MES EDHR_BATCH_EXECUTION RELEASE actions",
                wrongContextResult.getFailureReason());
        verify(releaseService, never()).submit(any());
        verify(releaseService, never()).approve(any());
    }

    @Test
    void pendingStartedFailsFastWithoutMovingReleaseTransactionToApproval() {
        FormActionInstance instance = releaseInstance();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> executor.onPendingApprovalStarted(instance));

        assertEquals("EDHR_RELEASE no longer supports BPM approval; use owner signature release submit",
                ex.getMessage());
        verify(releaseService, never()).submit(any());
        verify(releaseService, never()).approve(any());
    }

    @Test
    void executeReleaseFailsFastWithoutApplyingBpmApproval() {
        FormBusinessEffectResult result = executor.execute(releaseInstance(), "IDEM-EDHR-RELEASE-1");

        assertFalse(result.isSuccess());
        assertEquals("EDHR_RELEASE no longer supports BPM approval; use owner signature release submit",
                result.getFailureReason());
        verify(releaseService, never()).approve(any());
    }

    @Test
    void pendingRejectedClosesReleaseWithoutApplyingApproval() {
        executor.onPendingApprovalClosed(releaseInstance(), FormControlledActionApprovalOutcome.REJECTED, "reject");

        ArgumentCaptor<MesProEdhrReleaseRejectReqVO> reqCaptor =
                ArgumentCaptor.forClass(MesProEdhrReleaseRejectReqVO.class);
        verify(releaseService).reject(reqCaptor.capture());
        assertEquals(700L, reqCaptor.getValue().getReleaseTransactionId());
        assertEquals("reject", reqCaptor.getValue().getRejectReason());
        verify(releaseService, never()).approve(any());
    }

    @Test
    void pendingCancelledWithdrawsReleaseWithoutApplyingApproval() {
        executor.onPendingApprovalClosed(releaseInstance(), FormControlledActionApprovalOutcome.CANCELLED,
                "applicant cancelled");

        ArgumentCaptor<MesProEdhrReleaseWithdrawReqVO> reqCaptor =
                ArgumentCaptor.forClass(MesProEdhrReleaseWithdrawReqVO.class);
        verify(releaseService).withdraw(reqCaptor.capture());
        assertEquals(700L, reqCaptor.getValue().getReleaseTransactionId());
        assertEquals("applicant cancelled", reqCaptor.getValue().getWithdrawReason());
        verify(releaseService, never()).approve(any());
    }

    private FormActionInstance releaseInstance() {
        FormActionInstance instance = instance("MES", "EDHR_BATCH_EXECUTION", "RELEASE", "PRECHECK_PASSED");
        instance.setFormData(releaseFormData());
        return instance;
    }

    private FormActionInstance instance(String systemCode, String objectType, String actionCode, String objectState) {
        FormActionPolicy policy = FormActionPolicy.builder()
                .policyId(40L)
                .tenantId(122L)
                .dataDomain(systemCode)
                .systemCode(systemCode)
                .objectType(objectType)
                .actionCode(actionCode)
                .objectState(objectState)
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey("mes-edhr-release-approval")
                .effectExecutorCode("EDHR_RELEASE")
                .status(FormActionPolicy.STATUS_PUBLISHED)
                .build();
        return new FormActionInstance("FCI-EDHR-RELEASE-1", FormActionResolution.from(policy),
                BusinessActionContext.builder()
                        .tenantId(122L)
                        .dataDomain(systemCode)
                        .systemCode(systemCode)
                        .objectType(objectType)
                        .objectId("600")
                        .objectVersion("BATCH-V1")
                        .actionCode(actionCode)
                        .objectState(objectState)
                        .reason("release batch")
                        .build(),
                99L, "IDEM-EDHR-RELEASE-1");
    }

    private Map<String, Object> releaseFormData() {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("batchExecutionId", 600L);
        formData.put("releaseTransactionId", 700L);
        formData.put("submitReason", "release batch");
        formData.put("approvalOpinion", "approved");
        formData.put("rejectReason", "needs rework");
        formData.put("signoffEvidenceHash", "SIGNOFF-HASH-1");
        return formData;
    }

}
