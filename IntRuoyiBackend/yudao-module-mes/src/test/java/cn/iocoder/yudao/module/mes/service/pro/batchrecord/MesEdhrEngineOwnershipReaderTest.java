package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.EngineApprovalIdentity;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.NativeApprovalOwnership;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.flowable.task.api.Task;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrEngineOwnershipMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesEdhrEngineOwnershipReaderTest {
    private final MesEdhrEngineOwnershipMapper mapper = mock(MesEdhrEngineOwnershipMapper.class);
    private final BpmTaskService engineTasks = mock(BpmTaskService.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final MesEdhrEngineOwnershipReader reader = new MesEdhrEngineOwnershipReader(mapper, engineTasks, permissions);
    private Task engineTask;
    private final EngineApprovalIdentity identity = new EngineApprovalIdentity(122L, "engine-1", "process-1",
            "definition-1", "mes-edhr-approval-v1", "BUSINESS_APPROVAL:19", "reviewNode");
    private BusinessApprovalRequestDO request;
    private MesEdhrEngineOwnershipMapper.ExecutionFact execution;
    private MesEdhrEngineOwnershipMapper.SnapshotFact snapshot;
    private MesEdhrEngineOwnershipMapper.WorkTaskFact task;

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(122L);
        engineTask = mock(Task.class);
        when(engineTask.getId()).thenReturn("engine-1"); when(engineTask.getTenantId()).thenReturn("122");
        when(engineTask.getProcessInstanceId()).thenReturn("process-1");
        when(engineTask.getProcessDefinitionId()).thenReturn("definition-1");
        when(engineTask.getTaskDefinitionKey()).thenReturn("reviewNode");
        when(engineTask.getAssignee()).thenReturn("7");
        when(engineTasks.getTask("engine-1")).thenReturn(engineTask);
        when(permissions.hasAnyPermissions(7L, "mes:pro-batch-record-execution:approve")).thenReturn(true);
        when(permissions.hasAnyPermissions(7L, "mes:pro-batch-record-execution:track")).thenReturn(true);
        when(permissions.hasAnyPermissions(7L, "mes:pro-batch-record-execution:signature-query")).thenReturn(true);
        request = BusinessApprovalRequestDO.builder().tenantId(122L).processInstanceId("process-1")
                .objectId("301").objectVersion("2")
                .effectExecutorCode(MesProBatchRecordExecutionBusinessApprovalEffectExecutor.EXECUTOR_CODE).build();
        execution = new MesEdhrEngineOwnershipMapper.ExecutionFact();
        execution.setId(301L); execution.setTenantId(122L); execution.setRevisionNo(2); execution.setStatus(1);
        execution.setProcessInstanceId("process-1"); execution.setProcessDefinitionKey("mes-edhr-approval-v1");
        snapshot = new MesEdhrEngineOwnershipMapper.SnapshotFact();
        snapshot.setId(20L); snapshot.setTenantId(122L); snapshot.setExecutionId(301L);
        snapshot.setProcessInstanceId("process-1"); snapshot.setProcessDefinitionKey("mes-edhr-approval-v1");
        snapshot.setCurrentBpmTaskId("engine-1"); snapshot.setCurrentTaskDefinitionKey("reviewNode");
        snapshot.setApprovalStatus("SUBMITTED"); // actual writer leaves processDefinitionId absent
        task = task(401L, "REVIEW");
        when(mapper.selectExecutions(122L, 301L)).thenReturn(List.of(execution));
        when(mapper.selectSnapshots(122L, 301L)).thenReturn(List.of(snapshot));
        when(mapper.selectWorkTasks(122L, 301L, "engine-1")).thenReturn(List.of(task));
        when(mapper.selectPhaseWorkTasks(122L, 301L)).thenAnswer(call -> mapper.selectWorkTasks(122L, 301L, "engine-1"));
    }
    @AfterEach void clean() { TenantContextHolder.clear(); }

    @ParameterizedTest @ValueSource(strings = {"REVIEW", "APPROVE"})
    void uniqueActualReviewOrApproveUsesDomainTaskNotSubmitFillTask(String type) {
        task.setTaskType(type);
        var result = reader.read(request, identity, 7L);
        assertEquals(NativeApprovalOwnership.Owner.EDHR_EXECUTION, result.owner());
        assertEquals("/mes/pro/feedback/edhr-approval/detail", result.decisionRoute());
        assertEquals(Map.of("id", "301", "workTaskId", "401", "bpmTaskId", "engine-1"), result.decisionQuery());
        assertNull(result.navigationBlocker());
    }

    @Test void candidateAuthorizationUsesExistingDomainAuthorizationContract() {
        task.setAssigneeUserId(8L); task.setCandidateUserSnapshot("6,7");
        assertEquals("401", reader.read(request, identity, 7L).decisionQuery().get("workTaskId"));
    }

    @Test void multipleLegalSignatureCellsNeverChooseFirst() {
        when(mapper.selectWorkTasks(122L, 301L, "engine-1")).thenReturn(List.of(task, task(402L, "REVIEW")));
        var result = reader.read(request, identity, 7L);
        assertEquals("EDHR_ENGINE_WORK_TASK_CONTEXT_AMBIGUOUS", result.navigationBlocker());
        assertNull(result.decisionRoute()); assertEquals(Map.of(), result.decisionQuery());
        assertTrue(result.requiresDomainReview());
    }

    @ParameterizedTest @ValueSource(strings = {"notAssigned", "completed", "doing", "empty"})
    void missingEligibleDomainTaskDoesNotGrantNavigationOrGenericMutation(String mode) {
        if (mode.equals("notAssigned")) task.setAssigneeUserId(8L);
        if (mode.equals("completed")) task.setStatus("DONE");
        if (mode.equals("doing")) task.setStatus("DOING");
        if (mode.equals("empty")) when(mapper.selectWorkTasks(122L, 301L, "engine-1")).thenReturn(List.of());
        var result = reader.read(request, identity, 7L);
        assertEquals("EDHR_ENGINE_WORK_TASK_CONTEXT_MISSING", result.navigationBlocker());
        assertNull(result.decisionRoute()); assertTrue(result.requiresDomainReview());
    }

    @Test void exactWorkTaskCannotGrantMissingOfficialDomainAuthority() {
        when(permissions.hasAnyPermissions(7L, "mes:pro-batch-record-execution:approve")).thenReturn(false);
        var result = reader.read(request, identity, 7L);
        assertEquals("EDHR_ENGINE_DOMAIN_AUTHORITY_MISSING", result.navigationBlocker());
        assertNull(result.decisionRoute()); assertTrue(result.requiresDomainReview());
    }

    @Test void frozenCandidateCanNavigateAndReadExplicitTaskWithoutEngineMutation() {
        when(engineTask.getAssignee()).thenReturn("8");
        task.setAssigneeUserId(8L); task.setCandidateUserSnapshot("6,7");
        assertNull(reader.read(request, identity, 7L).navigationBlocker());
        var proof = reader.readDomainAssignment(request, identity, 401L, 7L);
        assertEquals(java.util.Set.of(6L, 7L, 8L), proof.eligibleActorIds());
        assertEquals(401L, proof.workTaskId()); assertEquals("REVIEW", proof.phase());
        verify(engineTasks, never()).validateTask(any(), any());
        verify(engineTasks, never()).approveTask(any(), any());
        verify(engineTasks, never()).transferTask(any(), any());
    }

    @Test void explicitReviewProofSupportsSharedActivityWithoutChoosingFirst() {
        var second = task(402L, "REVIEW");
        when(mapper.selectWorkTasks(122L, 301L, "engine-1")).thenReturn(List.of(task, second));
        assertEquals("EDHR_ENGINE_WORK_TASK_CONTEXT_AMBIGUOUS", reader.read(request, identity, 7L).navigationBlocker());
        assertEquals(402L, reader.readDomainAssignment(request, identity, 402L, 7L).workTaskId());
    }

    @ParameterizedTest @ValueSource(strings = {"wrongActor", "wrongTask", "completed", "reviewWithApprove",
            "approveWithActiveReview", "duplicateApprove", "permission", "invalidCandidate"})
    void assignmentProofRejectsWrongActorTaskOrPhase(String mode) {
        long actor = mode.equals("wrongActor") ? 9L : 7L;
        long selectedId = mode.equals("wrongTask") ? 999L : 401L;
        if (mode.equals("completed")) task.setStatus("DONE");
        if (mode.equals("reviewWithApprove")) {
            when(mapper.selectWorkTasks(122L, 301L, "engine-1")).thenReturn(List.of(task, task(402L, "APPROVE")));
        }
        if (mode.equals("approveWithActiveReview") || mode.equals("duplicateApprove")) {
            task.setTaskType("APPROVE");
            when(mapper.selectWorkTasks(122L, 301L, "engine-1")).thenReturn(List.of(task,
                    task(402L, mode.equals("duplicateApprove") ? "APPROVE" : "REVIEW")));
        }
        if (mode.equals("permission")) when(permissions.hasAnyPermissions(7L, "mes:pro-batch-record-execution:track")).thenReturn(false);
        if (mode.equals("invalidCandidate")) task.setCandidateUserSnapshot("-1,7");
        assertThrows(IllegalStateException.class, () -> reader.readDomainAssignment(request, identity, selectedId, actor));
    }

    @Test void uniqueApproveRetainsEveryFrozenCandidateIncludingNonFirst() {
        task.setTaskType("APPROVE"); task.setAssigneeUserId(6L); task.setCandidateUserSnapshot("6,7");
        var reviewed = task(402L, "REVIEW"); reviewed.setStatus("DONE");
        when(mapper.selectWorkTasks(122L, 301L, "engine-1")).thenReturn(List.of(reviewed, task));
        assertEquals(java.util.Set.of(6L, 7L), reader.readDomainAssignment(request, identity, 401L, 7L).eligibleActorIds());
    }

    @Test void anotherEngineActivityStillCountsAsUnfinishedReviewForFinalPhase() {
        task.setTaskType("APPROVE");
        var otherActivityReview = task(402L, "REVIEW"); otherActivityReview.setBpmTaskId("other-engine-task");
        when(mapper.selectPhaseWorkTasks(122L, 301L)).thenReturn(List.of(task, otherActivityReview));
        assertThrows(IllegalStateException.class, () -> reader.readDomainAssignment(request, identity, 401L, 7L));
    }

    @Test void actionConsumesLockedCurrentFactsEvenWhenOrdinaryReadStillShowsOldWritableTask() {
        var ledger = mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(reader, "gxpAuditService", ledger);
        when(mapper.selectExecutionsForUpdate(122L, 301L)).thenReturn(List.of(execution));
        when(mapper.selectSnapshotsForUpdate(122L, 301L)).thenReturn(List.of(snapshot));
        var current = task(401L, "REVIEW"); current.setStatus("DONE");
        when(mapper.selectPhaseWorkTasksForUpdate(122L, 301L)).thenReturn(List.of(current));
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertEquals(401L, reader.readDomainAssignment(request, identity, 401L, 7L).workTaskId());
            clearInvocations(mapper);
            assertThrows(IllegalStateException.class, () -> reader.acquireDomainAssignment(request, identity, 401L, 7L));
            var order = inOrder(ledger, mapper);
            order.verify(ledger).acquireLedgerLock();
            order.verify(mapper).selectExecutionsForUpdate(122L, 301L);
            order.verify(mapper).selectSnapshotsForUpdate(122L, 301L);
            order.verify(mapper).selectPhaseWorkTasksForUpdate(122L, 301L);
            verify(mapper, never()).selectExecutions(any(), any());
            verify(mapper, never()).selectSnapshots(any(), any());
            verify(mapper, never()).selectWorkTasks(any(), any(), any());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test void refetchedEngineTaskCannotChangeTenantOrProcess() {
        when(engineTask.getTenantId()).thenReturn("123");
        assertTrue(assertThrows(IllegalStateException.class, () -> reader.read(request, identity, 7L))
                .getMessage().contains("ENGINE_REFETCH_IDENTITY_MISMATCH"));
    }

    @ParameterizedTest @ValueSource(strings = {"approve", "track", "signature-query"})
    void everyMandatoryPagePermissionIsRequiredAndNamed(String suffix) {
        String permission = "mes:pro-batch-record-execution:" + suffix;
        when(permissions.hasAnyPermissions(7L, permission)).thenReturn(false);
        var result = reader.read(request, identity, 7L);
        assertNull(result.decisionRoute());
        assertEquals("EDHR_ENGINE_DOMAIN_AUTHORITY_MISSING", result.navigationBlocker());
        assertEquals(Map.of("requiredPermissionsMissing", permission), result.decisionQuery());
        assertTrue(result.requiresDomainReview());
    }

    @Test void navigationKeepsLongIdentityAsExactStrings() {
        task.setId(9007199254740993L);
        var result = reader.read(request, identity, 7L);
        assertEquals("9007199254740993", result.decisionQuery().get("workTaskId"));
        assertEquals("301", result.decisionQuery().get("id"));
        assertEquals("engine-1", result.decisionQuery().get("bpmTaskId"));
    }

    @ParameterizedTest @ValueSource(strings = {"requestTenant", "executor", "executionTenant", "executionProcess", "executionDefinition",
            "revision", "executionStatus", "snapshotTenant", "snapshotExecution", "snapshotProcess", "snapshotDefinition",
            "snapshotDefinitionId", "snapshotTask", "snapshotNode", "snapshotStatus", "taskTenant", "taskExecution",
            "taskBpm", "fillTask", "deletedTask", "duplicateExecution", "duplicateSnapshot", "duplicateTask"})
    void relationConflictFailsWithEvidenceInsteadOfGuessing(String mode) {
        switch (mode) {
            case "requestTenant" -> request.setTenantId(123L);
            case "executor" -> request.setEffectExecutorCode("OTHER");
            case "executionTenant" -> execution.setTenantId(123L);
            case "executionProcess" -> execution.setProcessInstanceId("another");
            case "executionDefinition" -> execution.setProcessDefinitionKey("another");
            case "revision" -> execution.setRevisionNo(3);
            case "executionStatus" -> execution.setStatus(3);
            case "snapshotTenant" -> snapshot.setTenantId(123L);
            case "snapshotExecution" -> snapshot.setExecutionId(302L);
            case "snapshotProcess" -> snapshot.setProcessInstanceId("another");
            case "snapshotDefinition" -> snapshot.setProcessDefinitionKey("another");
            case "snapshotDefinitionId" -> snapshot.setProcessDefinitionId("another");
            case "snapshotTask" -> snapshot.setCurrentBpmTaskId("another");
            case "snapshotNode" -> snapshot.setCurrentTaskDefinitionKey("another");
            case "snapshotStatus" -> snapshot.setApprovalStatus("APPROVED");
            case "taskTenant" -> task.setTenantId(123L);
            case "taskExecution" -> task.setExecutionId(302L);
            case "taskBpm" -> task.setBpmTaskId("another");
            case "fillTask" -> task.setTaskType("FILL");
            case "deletedTask" -> task.setDeleted(true);
            case "duplicateExecution" -> when(mapper.selectExecutions(122L, 301L)).thenReturn(List.of(execution, execution));
            case "duplicateSnapshot" -> when(mapper.selectSnapshots(122L, 301L)).thenReturn(List.of(snapshot, snapshot));
            case "duplicateTask" -> when(mapper.selectWorkTasks(122L, 301L, "engine-1")).thenReturn(List.of(task, task));
            default -> throw new AssertionError(mode);
        }
        assertThrows(IllegalStateException.class, () -> reader.read(request, identity, 7L));
    }

    @Test void missingContextNeverUsesRequestTenantAsFallback() {
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> reader.read(request, identity, 7L));
        verifyNoInteractions(mapper);
    }

    private MesEdhrEngineOwnershipMapper.WorkTaskFact task(Long id, String type) {
        var result = new MesEdhrEngineOwnershipMapper.WorkTaskFact();
        result.setId(id); result.setTenantId(122L); result.setExecutionId(301L); result.setBpmTaskId("engine-1");
        result.setTaskType(type); result.setStatus("TODO"); result.setAssigneeUserId(7L);
        return result;
    }
}
