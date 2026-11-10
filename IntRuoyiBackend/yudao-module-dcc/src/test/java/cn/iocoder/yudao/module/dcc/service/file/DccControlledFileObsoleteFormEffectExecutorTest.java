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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DccControlledFileObsoleteFormEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileObsoleteService obsoleteService;
    @Mock private DccObsoleteFormExecutionPreparationService executionPreparation;

    @InjectMocks
    private DccControlledFileObsoleteFormEffectExecutor executor;
    @org.junit.jupiter.api.BeforeEach void wireActualPreparationPort(){org.springframework.test.util.ReflectionTestUtils.setField(executor,"executionPreparation",executionPreparation);}

    @Test
    void lifecyclePreflightValidatesDccObsoleteBeforeBpmStarts() {
        FormActionInstance instance = dccObsoleteInstance();

        FormBusinessEffectPrecheck precheck = executor.preflight(instance);

        assertTrue(precheck.isPassed());
        verify(obsoleteService).precheckObsoleteControlledFile(eq(99L), eq(900L), any());
        verify(obsoleteService, never()).applyApprovedObsoleteControlledFile(any(), any(), any());
    }

    @Test
    void futureControlledVersionCanUseTheSameObsoleteApprovalEffectBoundary() {
        var request=instance("DCC","CONTROLLED_FILE","OBSOLETE","CONTROLLED_PENDING_EFFECTIVE");
        request.setFormData(formData("未来受控版本需作废"));
        assertTrue(executor.supports(request));
        assertTrue(executor.preflight(request).isPassed());
        approvedEffectStage(request);
        assertTrue(executor.execute(request,"future-obsolete-effect").isSuccess());
        verify(obsoleteService).precheckObsoleteControlledFile(eq(99L),eq(900L),any());
        verify(obsoleteService).applyApprovedObsoleteControlledFile(eq(99L),eq(900L),any());
        var unsigned=instance("DCC","CONTROLLED_FILE","OBSOLETE","PENDING_DOC_CONTROL_REVIEW");
        assertFalse(executor.supports(unsigned));
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
        approvedEffectStage(instance);

        FormBusinessEffectResult result = executor.execute(instance, "IDEM-DCC-OBSOLETE-1");

        assertTrue(result.isSuccess());
        assertEquals("900", result.getResultRef());
        ArgumentCaptor<DccControlledFileObsoleteReqVO> reqCaptor =
                ArgumentCaptor.forClass(DccControlledFileObsoleteReqVO.class);
        verify(obsoleteService).applyApprovedObsoleteControlledFile(eq(99L), eq(900L), reqCaptor.capture());
        assertEquals("no longer effective", reqCaptor.getValue().getReason());
        assertEquals("obsolete-round",reqCaptor.getValue().getApprovalProcessInstanceId());
        assertEquals("V1.0",reqCaptor.getValue().getApprovedVersionNo());
    }

    @Test
    void executeDccObsolete_propagatesOriginalDomainFailure() {
        FormActionInstance instance = dccObsoleteInstance();
        approvedEffectStage(instance);
        var failure = new IllegalStateException("obsolete domain effect failed");
        doThrow(failure)
                .when(obsoleteService).applyApprovedObsoleteControlledFile(eq(99L), eq(900L), any());

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> executor.execute(instance, "IDEM-DCC-OBSOLETE-1")));
    }

    @Test
    void transactionalEffectFailureRollsBackApprovalAndKeepsBusinessCause() {
        var dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:obsolete_effect_rollback;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE approval_marker (id INT PRIMARY KEY)");
        var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
        var attributes = new org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource();
        attributes.setTransactionAttribute(new org.springframework.transaction.interceptor.DefaultTransactionAttribute());
        var proxyFactory = new org.springframework.aop.framework.ProxyFactory(obsoleteService);
        proxyFactory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager, attributes));
        var transactionalExecutor = new DccControlledFileObsoleteFormEffectExecutor(
                (DccControlledFileObsoleteService) proxyFactory.getProxy());
        var missingEvidence = cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        doThrow(missingEvidence).when(obsoleteService).applyApprovedObsoleteControlledFile(any(), any(), any());
        var instance = dccObsoleteInstance();
        approvedEffectStage(instance);

        var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(status -> {
                    jdbc.update("INSERT INTO approval_marker VALUES (1)");
                    transactionalExecutor.execute(instance, "obsolete-effect");
                }));

        assertSame(missingEvidence, failure);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM approval_marker", Integer.class));
    }

    @Test
    void executeWithoutActualBpmBindingCannotApplyAnApprovedObsoleteFact() {
        FormActionInstance instance=dccObsoleteInstance();
        instance.setStatus(cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus.PENDING_EFFECT);
        instance.setBpmBinding(null);
        assertThrows(IllegalArgumentException.class, () -> executor.execute(instance,"missing-approval-binding"));
        verify(obsoleteService,never()).applyApprovedObsoleteControlledFile(any(),any(),any());
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
        verify(executionPreparation).onPendingApprovalStarted(instance);

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

    private void approvedEffectStage(FormActionInstance instance) {
        instance.setBpmBinding(new cn.iocoder.yudao.module.bpm.formcenter.model.FormBpmBinding("obsolete-round",null));
        instance.setStatus(cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus.PENDING_EFFECT);
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
