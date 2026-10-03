package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseApplicationPersistenceService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadiness;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseFormalFactSnapshots;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * First RED gate for the two actual producers. Mapper doubles delimit this unit test:
 * callback registration is asserted here; real commit/rollback and delivery durability
 * are intentionally reserved for the separate transaction contract, not claimed by this test.
 */
class MesReleaseTaskNotificationHandoffTest {

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications =
            mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
    private final MesReleaseFlowAuditRecorder audit = mock(MesReleaseFlowAuditRecorder.class);

    @BeforeEach
    void beginBusinessTransactionContext() {
        TenantContextHolder.setTenantId(1L);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void clearTransactionContext() {
        TransactionSynchronizationManager.clear();
        TenantContextHolder.clear();
    }

    @Test
    void newPqcReleaseTaskSchedulesNotificationOnlyAfterBusinessCommit() {
        MesProcessPoolActiveOrderReleaseApplicationDO application = newApplication();
        when(applications.insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class)))
                .thenAnswer(invocation -> {
                    ((MesProcessPoolActiveOrderReleaseApplicationDO) invocation.getArgument(0)).setId(224L);
                    return 1;
                });
        when(applications.updateById(any(MesProcessPoolActiveOrderReleaseApplicationDO.class))).thenReturn(1);
        assignWorkTaskId(2734L);

        persistence()
                .persistPending(application, new MesProductionReleaseRoleCandidates(
                        910494L, MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER,
                        List.of(1L, 1300L, 2022L, 9908090335L, 9908090346L), "pqc-candidates"));

        ArgumentCaptor<MesProEdhrWorkTaskDO> task = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(tasks).insert(task.capture());
        assertEquals("1,1300,2022,9908090335,9908090346", task.getValue().getCandidateUserSnapshot());
        assertEquals(2734L, application.getPqcReleaseWorkTaskId());
        assertFalse(TransactionSynchronizationManager.getSynchronizations().isEmpty(),
                "a committed PQC task must trigger its frozen recipients' durable delivery after commit");
    }

    @Test
    void formalPqcApprovalSchedulesManagerTaskNotificationAfterBusinessCommit() {
        MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
        MesProEdhrReleaseTransactionMapper releases = mock(MesProEdhrReleaseTransactionMapper.class);
        MesProductionReleaseRequiredCandidateResolver candidates =
                mock(MesProductionReleaseRequiredCandidateResolver.class);
        MesProductionReleaseBusinessReadinessService readiness =
                mock(MesProductionReleaseBusinessReadinessService.class);
        MesProcessPoolActiveOrderReleaseApplicationDO application = newApplication()
                .setId(224L).setBatchExecutionId(952L).setPqcReleaseWorkTaskId(2734L)
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING).setVersion(5)
                .setPqcDecision("APPROVE").setPqcDecidedBy(9908090346L)
                .setPqcDecidedAt(LocalDateTime.of(2026, 10, 3, 12, 0));
        String hash = MesProductionReleaseFormalFactSnapshots.recomputeActiveOrderFactsSnapshot(application);
        application.setReportSnapshotHash(hash);
        when(applications.selectById(224L)).thenReturn(application);
        when(batches.selectById(952L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(952L).setBatchExecutionCode("BE-952").setWorkOrderId(990274L)
                .setWorkOrderCode("WO-NOTIFY").setBatchCode("BATCH-NOTIFY").setRouteId(400L));
        when(candidates.resolveRequiredCandidates(1L, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(910495L,
                        MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(1L, 9908090347L), "manager-candidates"));
        when(readiness.resolveActiveOrderFormalFactsReadiness(any(MesProEdhrBatchExecutionDO.class),
                any(MesProcessPoolActiveOrderReleaseApplicationDO.class)))
                .thenReturn(new MesProductionReleaseBusinessReadiness("PASS", "PASS", "NOT_APPLICABLE",
                        "NOT_APPLICABLE", "NOT_APPLICABLE", "PASS", 6, 0, 0,
                        "{\"items\":[]}", "readiness-hash", List.of()));
        when(releases.insert(any(MesProEdhrReleaseTransactionDO.class))).thenAnswer(invocation -> {
            ((MesProEdhrReleaseTransactionDO) invocation.getArgument(0)).setId(449L);
            return 1;
        });
        assignWorkTaskId(2735L);

        var initializer = new MesProductionReleaseManagerStageInitializerImpl(applications, batches, releases, tasks,
                candidates, readiness);
        ReflectionTestUtils.setField(initializer, "notificationService", scheduler());
        initializer.initializeManagerReleaseStage(
                new MesProductionReleaseManagerStageInitializationCommand().setApplicationId(224L)
                        .setBatchExecutionId(952L).setExpectedApplicationVersion(5)
                        .setReportSnapshotHash(hash).setActiveOrderFormalFacts(true));

        ArgumentCaptor<MesProEdhrWorkTaskDO> task = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(tasks).insert(task.capture());
        assertEquals("1,9908090347", task.getValue().getCandidateUserSnapshot());
        assertEquals("RELEASE_APPROVE", task.getValue().getTaskType());
        assertFalse(TransactionSynchronizationManager.getSynchronizations().isEmpty(),
                "the formal PQC approval handoff must schedule manager notification after commit");
    }

    @Test
    void applicationReplayDoesNotCreateOrScheduleAnotherAssignedEvent() {
        MesProcessPoolActiveOrderReleaseApplicationDO incoming = newApplication();
        MesProcessPoolActiveOrderReleaseApplicationDO stored = newApplication().setId(224L)
                .setPqcReleaseWorkTaskId(2734L);
        when(applications.insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class)))
                .thenThrow(new DuplicateKeyException("same formal request"));
        when(applications.selectByRequestIdempotencyKey(1009200409L, "NOTIFY-REQUEST"))
                .thenReturn(stored);

        var result = persistence()
                .persistPending(incoming, new MesProductionReleaseRoleCandidates(910494L,
                        MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER, List.of(9908090346L), "candidates"));

        assertEquals(2734L, result.getPqcReleaseWorkTaskId());
        verifyNoInteractions(tasks, audit);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void failedPqcTaskBindingDoesNotScheduleAnyNotification() {
        when(applications.insert(any(MesProcessPoolActiveOrderReleaseApplicationDO.class)))
                .thenAnswer(invocation -> {
                    ((MesProcessPoolActiveOrderReleaseApplicationDO) invocation.getArgument(0)).setId(224L);
                    return 1;
                });
        when(applications.updateById(any(MesProcessPoolActiveOrderReleaseApplicationDO.class))).thenReturn(0);
        assignWorkTaskId(2734L);

        assertThrows(IllegalStateException.class,
                () -> persistence()
                        .persistPending(newApplication(), new MesProductionReleaseRoleCandidates(910494L,
                                MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER,
                                List.of(9908090346L), "candidates")));
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verifyNoInteractions(audit);
    }

    private void assignWorkTaskId(long id) {
        when(tasks.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(invocation -> {
            ((MesProEdhrWorkTaskDO) invocation.getArgument(0)).setId(id);
            return 1;
        });
        when(tasks.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);
    }

    private MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistence() {
        var service = new MesTeamLeaderActiveOrderReleaseApplicationPersistenceService(applications, tasks, audit);
        ReflectionTestUtils.setField(service, "notificationService", scheduler());
        return service;
    }

    /** Real scheduler; explicit persistence/audit/dispatch doubles retain this test's documented unit boundary. */
    private MesReleaseTaskNotificationService scheduler() {
        var scheduler = new MesReleaseTaskNotificationService();
        var deliveries = mock(MesReleaseTaskNotifyDeliveryMapper.class);
        var ids = new java.util.concurrent.atomic.AtomicLong(9000);
        when(deliveries.insert(any(MesReleaseTaskNotifyDeliveryDO.class))).thenAnswer(call -> {
            ((MesReleaseTaskNotifyDeliveryDO) call.getArgument(0)).setId(ids.incrementAndGet());
            return 1;
        });
        ReflectionTestUtils.setField(scheduler, "deliveryMapper", deliveries);
        ReflectionTestUtils.setField(scheduler, "audit", mock(MesReleaseTaskNotificationAudit.class));
        ReflectionTestUtils.setField(scheduler, "dispatchService", mock(MesReleaseTaskNotificationDispatchService.class));
        return scheduler;
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO newApplication() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO().setActiveOrderId(1009200409L)
                .setWorkOrderId(990274L).setWorkOrderCode("WO-NOTIFY").setBatchCode("BATCH-NOTIFY")
                .setRouteId(400L).setRouteVersionId(401L).setVersion(1)
                .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setRequestIdempotencyKey("NOTIFY-REQUEST").setBusinessIdempotencyKey("NOTIFY-BUSINESS")
                .setSourceSnapshotHash("formal-source-hash").setAppliedBy(9908090341L)
                .setAppliedAt(LocalDateTime.of(2026, 10, 3, 11, 0));
    }
}
