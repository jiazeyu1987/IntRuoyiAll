package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormBpmBinding;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
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
import static org.mockito.Mockito.when;

class MesProEdhrBatchVoidFormEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private MesProEdhrRecordChangeService recordChangeService;

    @InjectMocks
    private MesProEdhrBatchVoidFormEffectExecutor executor;

    @Test
    void lifecyclePreflightValidatesBatchVoidBeforeBpmStarts() {
        FormBusinessEffectPrecheck precheck = executor.preflight(voidInstance());

        assertTrue(precheck.isPassed());
        verify(recordChangeService).precheckPlatformVoidBatchExecution(any());
        verify(recordChangeService, never()).requestPlatformVoidBatchExecution(any(), any());
    }

    @Test
    void lifecyclePreflightRejectsWrongContextAndMissingReason() {
        FormActionInstance wrongContext = instance("MES", "EDHR_BATCH_EXECUTION", "RELEASE", "CLOSED");
        wrongContext.setFormData(voidFormData());

        FormBusinessEffectPrecheck wrongContextResult = executor.preflight(wrongContext);

        assertFalse(wrongContextResult.isPassed());
        assertEquals("EDHR_BATCH_VOID lifecycle adapter only accepts MES EDHR_BATCH_EXECUTION VOID actions",
                wrongContextResult.getFailureReason());
        verify(recordChangeService, never()).precheckPlatformVoidBatchExecution(any());

        FormActionInstance missingReason = voidInstance();
        missingReason.setFormData(Map.of("batchExecutionId", 600L));

        FormBusinessEffectPrecheck missingReasonResult = executor.preflight(missingReason);

        assertFalse(missingReasonResult.isPassed());
        assertEquals("Missing eDHR batch void form field: reasonCategory", missingReasonResult.getFailureReason());
    }

    @Test
    void pendingStartedCreatesDomainVoidEventWithoutStartingPrivateBpm() {
        FormActionInstance instance = voidInstance();

        executor.onPendingApprovalStarted(instance);

        ArgumentCaptor<EdhrRecordChangeRequestReqVO> reqCaptor =
                ArgumentCaptor.forClass(EdhrRecordChangeRequestReqVO.class);
        verify(recordChangeService).requestPlatformVoidBatchExecution(reqCaptor.capture(), eq("PROC-EDHR-VOID-1"));
        assertEquals(600L, reqCaptor.getValue().getBatchExecutionId());
        assertEquals("QUALITY", reqCaptor.getValue().getReasonCategory());
        verify(recordChangeService, never()).requestVoidBatchExecution(any());
    }

    @Test
    void executeBatchVoidAppliesExistingDomainCallbackAfterBpmApprovalOnly() {
        when(recordChangeService.handleVoidBatchExecutionApprovalCallback(
                "PROC-EDHR-VOID-1", null, "APPROVED", null, 99L))
                .thenReturn(new EdhrRecordChangeRespVO().setId(800L));

        FormBusinessEffectResult result = executor.execute(voidInstance(), "IDEM-EDHR-VOID-1");

        assertTrue(result.isSuccess());
        assertEquals("800", result.getResultRef());
        verify(recordChangeService).handleVoidBatchExecutionApprovalCallback(
                "PROC-EDHR-VOID-1", null, "APPROVED", null, 99L);
    }

    @Test
    void executeBatchVoidUsesDomainCallbackResultWhenSnapshotHasNoChangeEventId() {
        FormActionInstance instance = voidInstance();
        Map<String, Object> formData = new LinkedHashMap<>(instance.getFormData());
        formData.remove("changeEventId");
        instance.setFormData(formData);
        when(recordChangeService.handleVoidBatchExecutionApprovalCallback(
                "PROC-EDHR-VOID-1", null, "APPROVED", null, 99L))
                .thenReturn(new EdhrRecordChangeRespVO().setId(801L));

        FormBusinessEffectResult result = executor.execute(instance, "IDEM-EDHR-VOID-1");

        assertTrue(result.isSuccess());
        assertEquals("801", result.getResultRef());
        verify(recordChangeService).handleVoidBatchExecutionApprovalCallback(
                "PROC-EDHR-VOID-1", null, "APPROVED", null, 99L);
    }

    @Test
    void executeBatchVoidDirectModeCreatesAndAppliesDomainChangeWithoutBpmProcess() {
        FormActionInstance instance = voidInstance();
        instance.setBpmBinding(null);
        when(recordChangeService.executeDirectPlatformVoidBatchExecution(any(), eq(99L)))
                .thenReturn(new EdhrRecordChangeRespVO().setId(802L));

        FormBusinessEffectResult result = executor.execute(instance, "IDEM-EDHR-VOID-1");

        assertTrue(result.isSuccess());
        assertEquals("802", result.getResultRef());
        ArgumentCaptor<EdhrRecordChangeRequestReqVO> reqCaptor =
                ArgumentCaptor.forClass(EdhrRecordChangeRequestReqVO.class);
        verify(recordChangeService).executeDirectPlatformVoidBatchExecution(reqCaptor.capture(), eq(99L));
        assertEquals(600L, reqCaptor.getValue().getBatchExecutionId());
        assertEquals("QUALITY", reqCaptor.getValue().getReasonCategory());
        verify(recordChangeService, never()).handleVoidBatchExecutionApprovalCallback(any(), any(), any(), any(), any());
    }

    @Test
    void businessApprovalContractRegistersBatchVoidExecutorAndProcessKey() {
        BusinessApprovalEffectExecutor businessExecutor = executor;

        assertEquals("EDHR_BATCH_VOID", businessExecutor.getExecutorCode());
        assertEquals(MesProEdhrRecordChangeServiceImpl.BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY,
                businessExecutor.getBpmProcessDefinitionKey());
    }

    @Test
    void businessApprovalDirectModeDelegatesToPlatformVoidService() {
        when(recordChangeService.executeDirectPlatformVoidBatchExecution(any(), eq(99L)))
                .thenReturn(new EdhrRecordChangeRespVO().setId(803L).setChangeStatus("EFFECTIVE"));

        BusinessApprovalEffectExecutor businessExecutor = executor;
        BusinessApprovalEffectResult result = businessExecutor.executeDirect(voidBusinessContext(),
                businessRequest(null));

        assertEquals("EFFECTIVE", result.getResultState());
        ArgumentCaptor<EdhrRecordChangeRequestReqVO> reqCaptor =
                ArgumentCaptor.forClass(EdhrRecordChangeRequestReqVO.class);
        verify(recordChangeService).executeDirectPlatformVoidBatchExecution(reqCaptor.capture(), eq(99L));
        assertEquals(600L, reqCaptor.getValue().getBatchExecutionId());
        assertEquals("QUALITY", reqCaptor.getValue().getReasonCategory());
        verify(recordChangeService, never()).requestPlatformVoidBatchExecution(any(), any());
    }

    @Test
    void businessApprovalBpmModeDelegatesPendingAndApprovedCallbacks() {
        when(recordChangeService.requestPlatformVoidBatchExecution(any(), eq("PROC-EDHR-VOID-1")))
                .thenReturn(new EdhrRecordChangeRespVO().setId(804L).setChangeStatus("SUBMITTED"));
        when(recordChangeService.handleVoidBatchExecutionApprovalCallback(
                "PROC-EDHR-VOID-1", null, "APPROVED", null, 88L))
                .thenReturn(new EdhrRecordChangeRespVO().setId(804L).setChangeStatus("EFFECTIVE"));

        BusinessApprovalEffectExecutor businessExecutor = executor;
        BusinessApprovalEffectResult pending = businessExecutor.markPending(voidBusinessContext(),
                businessRequest("PROC-EDHR-VOID-1"));
        BusinessApprovalEffectResult approved = businessExecutor.executeApproved(voidBusinessContext(),
                businessRequest("PROC-EDHR-VOID-1"), 88L);

        assertEquals("SUBMITTED", pending.getResultState());
        assertEquals("EFFECTIVE", approved.getResultState());
        verify(recordChangeService).requestPlatformVoidBatchExecution(any(), eq("PROC-EDHR-VOID-1"));
        verify(recordChangeService).handleVoidBatchExecutionApprovalCallback(
                "PROC-EDHR-VOID-1", null, "APPROVED", null, 88L);
    }

    @Test
    void executeBatchVoidDomainFailureBecomesQueryableEffectFailure() {
        doThrow(new IllegalStateException("void domain effect failed"))
                .when(recordChangeService).handleVoidBatchExecutionApprovalCallback(
                        "PROC-EDHR-VOID-1", null, "APPROVED", null, 99L);

        FormBusinessEffectResult result = executor.execute(voidInstance(), "IDEM-EDHR-VOID-1");

        assertFalse(result.isSuccess());
        assertEquals("void domain effect failed", result.getFailureReason());
    }

    @Test
    void pendingRejectedClosesBatchVoidWithoutApplyingApproval() {
        executor.onPendingApprovalClosed(voidInstance(), FormControlledActionApprovalOutcome.REJECTED, "reject");

        verify(recordChangeService).handleVoidBatchExecutionApprovalCallback(
                "PROC-EDHR-VOID-1", null, "REJECTED", "reject", 99L);
        verify(recordChangeService, never()).requestVoidBatchExecution(any());
    }

    @Test
    void pendingCancelledClosesBatchVoidWithoutApplyingApproval() {
        executor.onPendingApprovalClosed(voidInstance(), FormControlledActionApprovalOutcome.CANCELLED,
                "applicant cancelled");

        verify(recordChangeService).handleVoidBatchExecutionApprovalCallback(
                "PROC-EDHR-VOID-1", null, "CANCELLED", "applicant cancelled", 99L);
        verify(recordChangeService, never()).requestVoidBatchExecution(any());
    }

    private FormActionInstance voidInstance() {
        FormActionInstance instance = instance("MES", "EDHR_BATCH_EXECUTION", "VOID", "CLOSED");
        instance.setFormData(voidFormData());
        instance.setBpmBinding(new FormBpmBinding("PROC-EDHR-VOID-1", null));
        return instance;
    }

    private FormActionInstance instance(String systemCode, String objectType, String actionCode, String objectState) {
        FormActionPolicy policy = FormActionPolicy.builder()
                .policyId(41L)
                .tenantId(122L)
                .dataDomain(systemCode)
                .systemCode(systemCode)
                .objectType(objectType)
                .actionCode(actionCode)
                .objectState(objectState)
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey("mes-edhr-batch-void-approval")
                .effectExecutorCode("EDHR_BATCH_VOID")
                .status(FormActionPolicy.STATUS_PUBLISHED)
                .build();
        return new FormActionInstance("FCI-EDHR-VOID-1", FormActionResolution.from(policy),
                BusinessActionContext.builder()
                        .tenantId(122L)
                        .dataDomain(systemCode)
                        .systemCode(systemCode)
                        .objectType(objectType)
                        .objectId("600")
                        .objectVersion("BATCH-V1")
                        .actionCode(actionCode)
                        .objectState(objectState)
                        .reason("void batch")
                        .build(),
                99L, "IDEM-EDHR-VOID-1");
    }

    private Map<String, Object> voidFormData() {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("batchExecutionId", 600L);
        formData.put("changeEventId", 800L);
        formData.put("reasonCategory", "QUALITY");
        formData.put("reasonText", "void rejected");
        formData.put("password", "111111");
        formData.put("comment", "void rejected");
        return formData;
    }

    private BusinessApprovalContext voidBusinessContext() {
        return BusinessApprovalContext.builder()
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION")
                .objectId("600")
                .objectVersion("BATCH-V1")
                .actionCode("VOID")
                .objectState("CLOSED")
                .applicantUserId(99L)
                .reason("void batch")
                .variables(voidFormData())
                .build();
    }

    private BusinessApprovalRequest businessRequest(String processInstanceId) {
        return BusinessApprovalRequest.builder()
                .requestId(4101L)
                .processInstanceId(processInstanceId)
                .context(voidBusinessContext())
                .build();
    }
}
