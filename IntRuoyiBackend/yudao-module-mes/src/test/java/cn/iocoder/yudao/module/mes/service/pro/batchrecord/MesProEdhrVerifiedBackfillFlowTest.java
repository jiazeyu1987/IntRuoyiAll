package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.formcenter.model.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real effect executor and work-task service; persistence is isolated from business data. */
class MesProEdhrVerifiedBackfillFlowTest extends BaseMockitoUnitTest {
    @Mock private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private MesProEdhrOperationAuditService operationAuditService;
    @Mock private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissionApi;
    @InjectMocks private MesProEdhrWorkTaskServiceImpl workTaskService;

    @ParameterizedTest
    @ValueSource(strings = {"PROCESS_INSPECTION", "LOSS_REPORT"})
    void downstreamEvidenceBecomesApprovedWithoutManualTodo(String slot) {
        pendingRelease(slot, 77L);
        var result = executor().execute(instance(true, 77L), "automatic");
        assertTrue(result.isSuccess(), result.getFailureReason());
        verify(batchTaskMapper).updateById(argThat((MesProEdhrBatchExecutionTaskDO task) ->
                task.getId().equals(700L) && task.getStatus().equals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                        && task.getOpenedBy().equals(77L) && task.getApprovedAt() != null));
        verify(operationAuditService).recordInCallerTransaction(argThat(command ->
                command.getActorUserId().equals(77L) && command.getAfterSummaryHash().equals("a".repeat(64))
                        && command.getWorkTaskId().equals(601L)));
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
        verify(batchTaskMapper, never()).selectListByBatchExecutionId(any());
    }

    @Test
    void firstProcessClosesProductionOwnedCompanionAsAutomaticEvidence() {
        pendingRelease("LOSS_REPORT", 77L);
        when(workTaskMapper.selectActiveByBatchTaskAndType(700L, "FILL"))
                .thenReturn(new MesProEdhrWorkTaskDO().setId(602L).setBatchTaskId(700L).setTaskType("FILL")
                        .setStatus("TODO").setAssigneeUserId(88L).setCandidateUserSnapshot("88"));
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(122L);
        try {
            var result = executor().execute(instance(true, 77L), "first-process");
            assertTrue(result.isSuccess(), result.getFailureReason());
            verify(workTaskMapper).updateById(argThat((MesProEdhrWorkTaskDO task) ->
                    task.getId().equals(602L) && "DONE".equals(task.getStatus())
                            && task.getReason().startsWith("AUTOMATIC_BACKFILL:")
                            && task.getAssigneeUserId() == null));
            verify(permissionApi).revokeEntitlementSource(any());
        } finally {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @Test
    void automaticEvidenceRejectsAnActorOutsideTheFrozenPqcCandidates() {
        pendingRelease("LOSS_REPORT", 77L);
        var result = executor().execute(instance(true, 99L), "unauthorized");
        assertFalse(result.isSuccess());
        verify(batchTaskMapper, never()).updateById(any(MesProEdhrBatchExecutionTaskDO.class));
        verifyNoInteractions(operationAuditService);
    }

    @Test
    void automaticContextCannotCompleteAManualMainForm() {
        when(batchTaskMapper.selectByIdForUpdate(700L)).thenReturn(task("MAIN"));
        var result = executor().execute(instance(true, 77L), "wrong-slot");
        assertFalse(result.isSuccess());
        verifyNoInteractions(workTaskMapper, batchExecutionMapper, operationAuditService);
    }

    @Test
    void manualSubmissionChecksActualActorEvenWhenCreatorOwnsTheTodo() {
        when(batchTaskMapper.selectByIdForUpdate(700L)).thenReturn(task("MAIN"));
        when(workTaskMapper.selectActiveByBatchTaskAndType(700L, "FILL"))
                .thenReturn(new MesProEdhrWorkTaskDO().setId(602L).setStatus("TODO")
                        .setAssigneeUserId(99L).setCandidateUserSnapshot("99"));
        var result = executor().execute(instance(false, 77L), "manual");
        assertFalse(result.isSuccess());
        verify(batchTaskMapper, never()).updateById(any(MesProEdhrBatchExecutionTaskDO.class));
        verify(workTaskMapper, never()).updateById(any(MesProEdhrWorkTaskDO.class));
    }

    private void pendingRelease(String slot, Long candidate) {
        when(batchTaskMapper.selectByIdForUpdate(700L)).thenReturn(task(slot));
        when(batchExecutionMapper.selectById(900L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(900L).setTenantId(122L).setActiveContextKey("PQC_RELEASE:501")
                .setWorkOrderId(100L).setRouteId(200L).setRouteVersionId(300L));
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(501L)
                .setWorkOrderId(100L).setRouteId(200L).setRouteVersionId(300L)
                .setApplicationStatus("PQC_RELEASE_PENDING").setPqcReleaseWorkTaskId(601L);
        application.setTenantId(122L);
        when(releaseApplicationMapper.selectByIdForUpdate(501L)).thenReturn(application);
        when(workTaskMapper.selectById(601L)).thenReturn(new MesProEdhrWorkTaskDO().setId(601L)
                .setTaskType("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(501L).setStatus("TODO").setCandidateUserSnapshot(candidate.toString()));
    }

    private MesProEdhrBatchExecutionTaskDO task(String slot) {
        return new MesProEdhrBatchExecutionTaskDO().setId(700L).setBatchExecutionId(900L)
                .setNodeType("ROUTE_FORM").setFormSlotType(slot).setRouteProcessId(202L)
                .setFormCenterInstanceId(4001L).setFormTemplateId(2001L).setFormTemplateVersionId(3002L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
    }

    private MesProEdhrRouteFormFillEffectExecutor executor() {
        return new MesProEdhrRouteFormFillEffectExecutor(batchTaskMapper, workTaskService);
    }

    private FormActionInstance instance(boolean automatic, Long actor) {
        var policy = FormActionPolicy.builder().policyId(40L).tenantId(122L).dataDomain("MES")
                .systemCode("MES").objectType("EDHR_ROUTE_FORM").actionCode("EDHR_RF_100_BINDING")
                .objectState("ACTIVE").policyType(FormPolicyType.REQUIRED).approvalMode(FormApprovalMode.DIRECT)
                .effectExecutorCode(MesProEdhrRouteFormFillEffectExecutor.EXECUTOR_CODE)
                .status(FormActionPolicy.STATUS_PUBLISHED).build();
        var instance = new FormActionInstance("4001", FormActionResolution.from(policy),
                BusinessActionContext.builder().tenantId(122L).dataDomain("MES").systemCode("MES")
                        .objectType("EDHR_ROUTE_FORM").objectId("700").objectVersion("100")
                        .actionCode("EDHR_RF_100_BINDING").objectState("ACTIVE").reason("fill").build(), 99L, "idem");
        instance.setExecutionContext(automatic
                ? FormActionExecutionContext.verifiedBackfill(actor, MesProEdhrRouteFormFillEffectExecutor.EXECUTOR_CODE, "a".repeat(64))
                : FormActionExecutionContext.manual(actor));
        return instance;
    }
}
