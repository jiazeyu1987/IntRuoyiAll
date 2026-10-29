package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.service.pro.MesSa36Sa38MapperFixture;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real task/application/file persistence; notifications, formal reapply and audit are explicit boundaries. */
class MesPqcReleaseCorrectionServiceTest {
    private MesSa36Sa38MapperFixture db;
    private MesProEdhrWorkTaskMapper tasks;
    private MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private MesProcessPoolActiveOrderMapper activeOrders;
    private MesProcessPoolActiveOrderDossierFileMapper files;
    private MesTeamLeaderActiveOrderReleaseApplicationService release;
    private GxpAuditService audit;
    private MesPqcReleaseCorrectionService service;
    private MesProcessPoolActiveOrderReleaseApplicationDO source;
    private NotifyMessageSendApi notifyApi;

    @BeforeEach
    void setUp() throws Exception {
        TenantContextHolder.setTenantId(1L);
        db = new MesSa36Sa38MapperFixture();
        for (Class<?> type : List.of(MesProEdhrWorkTaskDO.class, MesProcessPoolActiveOrderDO.class,
                MesProcessPoolActiveOrderReleaseApplicationDO.class, MesProcessPoolActiveOrderDossierFileDO.class,
                MesProEdhrBatchExecutionDO.class, MesProEdhrReleaseTransactionDO.class, MesProEdhrNonconformanceReviewDO.class, MesProEdhrRecordChangeEventDO.class)) {
            db.table(type, type.getAnnotation(TableName.class).value());
        }
        db.jdbc.execute("ALTER TABLE mes_pro_edhr_work_task ADD COLUMN tenant_id BIGINT");
        db.jdbc.execute("ALTER TABLE mes_pro_edhr_record_change_event ADD COLUMN tenant_id BIGINT");
        db.register(MesProEdhrWorkTaskMapper.class, MesProcessPoolActiveOrderMapper.class,
                MesProcessPoolActiveOrderReleaseApplicationMapper.class, MesProcessPoolActiveOrderDossierFileMapper.class,
                MesProEdhrBatchExecutionMapper.class, MesProEdhrReleaseTransactionMapper.class, MesProEdhrNonconformanceReviewMapper.class,
                MesProEdhrRecordChangeEventMapper.class);
        tasks = db.mapper(MesProEdhrWorkTaskMapper.class);
        applications = db.mapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        activeOrders = db.mapper(MesProcessPoolActiveOrderMapper.class);
        files = db.mapper(MesProcessPoolActiveOrderDossierFileMapper.class);
        var batches = db.mapper(MesProEdhrBatchExecutionMapper.class);
        var permissions = mock(PermissionApi.class);
        when(permissions.hasAnyPermissions(eq(10L), any(String[].class))).thenReturn(true);
        release = mock(MesTeamLeaderActiveOrderReleaseApplicationService.class);
        audit = mock(GxpAuditService.class);
        var workOrders = mock(MesProWorkOrderMapper.class);
        when(workOrders.selectByIdForUpdate(30L)).thenReturn(new MesProWorkOrderDO().setId(30L));
        service = new MesPqcReleaseCorrectionService(tasks, applications, activeOrders, files, batches, workOrders, permissions,
                mock(MesEdhrBatchLifecycleGuard.class), mock(MesProEdhrNonconformanceReviewService.class), release, audit);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "deadlineService", mock(MesEdhrTaskDeadlineService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "releaseTransactions", db.mapper(MesProEdhrReleaseTransactionMapper.class));
        var formalWorkOrder = new MesProWorkOrderDO().setId(30L); formalWorkOrder.setTenantId(1L);
        when(workOrders.selectById(30L)).thenReturn(formalWorkOrder);
        notifyApi = mock(NotifyMessageSendApi.class);
        when(notifyApi.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(99L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "notifyApi", notifyApi);
        var auxiliary = mock(MesWorkTaskAuxiliaryAudit.class);
        doAnswer(call -> { call.<java.util.function.Supplier<List<Long>>>getArgument(1).get(); return null; })
                .when(auxiliary).notifications(any(), any());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "auxiliaryAudit", auxiliary);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "reviewMapper", db.mapper(MesProEdhrNonconformanceReviewMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "changeMapper", db.mapper(MesProEdhrRecordChangeEventMapper.class));
        var order = new MesProcessPoolActiveOrderDO().setId(20L).setLeaderUserId(10L)
                .setWorkOrderId(30L).setActiveStatus("ACTIVE").setDossierRevision(0L);
        order.setTenantId(1L); activeOrders.insert(order);
        batches.insert(new MesProEdhrBatchExecutionDO().setId(40L).setTenantId(1L).setWorkOrderId(30L).setStatus(20));
        source = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(50L).setActiveOrderId(20L)
                .setWorkOrderId(30L).setWorkOrderCode("WO-CORRECTION").setBatchCode("B-CORRECTION")
                .setBatchExecutionId(40L).setRouteId(60L).setPqcReleaseWorkTaskId(70L).setAppliedBy(10L).setVersion(3)
                .setApplicationStatus("PQC_RELEASE_REJECTED").setPqcDecision("REJECT").setPqcRejectReason("补齐成品检文件")
                .setSourceSnapshotHash("formal-source");
        source.setTenantId(1L);
        applications.insert(source);
        when(release.applyCorrection(eq(10L), any(), eq(50L), eq(40L))).thenAnswer(invocation -> {
            var next = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(51L)
                    .setActiveOrderId(20L).setWorkOrderId(30L).setBatchExecutionId(40L).setAppliedBy(10L)
                    .setApplicationStatus("PQC_RELEASE_PENDING").setPqcReleaseWorkTaskId(71L).setVersion(2);
            next.setTenantId(1L); applications.insert(next);
            return new MesTeamLeaderActiveOrderReleaseApplicationResult().setApplicationId(51L)
                    .setPqcReleaseWorkTaskId(71L).setVersion(2).setStatus("PQC_RELEASE_PENDING").setSourceSnapshotHash("new-source");
        });
    }

    @AfterEach void clear() { TenantContextHolder.clear(); }
    private Long create() { return db.transaction.execute(status -> service.create(source, 11L, "补齐成品检文件")); }
    private MesPqcReleaseCorrectionService.Resubmit command(Long id) {
        return new MesPqcReleaseCorrectionService.Resubmit(id, 3, "correction-resubmit-1", "已补齐成品检文件");
    }
    private void upload() {
        var file = new MesProcessPoolActiveOrderDossierFileDO().setId(81L)
                .setActiveOrderId(20L).setApplicationId(50L).setCategoryKey("FINISHED_PRODUCT_FILE").setOperatorId(10L);
        file.setTenantId(1L); files.insert(file);
        activeOrders.advanceDossierRevision(20L, 0L);
    }

    @Test void rejectionCreatesTaskForOriginalApplicantAndPreventsDuplicates() {
        Long id = create();
        var task = tasks.selectById(id);
        assertEquals("PQC_RELEASE_CORRECTION", task.getTaskType());
        assertEquals(10L, task.getAssigneeUserId());
        assertEquals("TODO", task.getStatus());
        assertEquals("/user/profile?tab=workbench&releaseCorrectionTaskId=" + id, task.getActionUrl());
        assertEquals(50L, service.get(10L, id).applicationId());
        assertThrows(RuntimeException.class, this::create);
        assertEquals(1L, db.jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_work_task", Long.class));
    }

    @Test void correctionBelongsOnlyToOriginalApplicant() {
        Long id = create();
        assertThrows(RuntimeException.class, () -> service.get(12L, id));
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.resubmit(12L, command(id))));
        verifyNoInteractions(release);
    }

    @Test void requiresActualNewFileAndAllAssignedDossierTasksCompleted() {
        Long id = create();
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.resubmit(10L, command(id))));
        activeOrders.advanceDossierRevision(20L, 0L);
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.resubmit(10L, command(id))));
        var file = new MesProcessPoolActiveOrderDossierFileDO().setId(81L).setActiveOrderId(20L).setApplicationId(50L);
        file.setTenantId(1L); files.insert(file);
        tasks.insert(new MesProEdhrWorkTaskDO().setId(91L).setTaskType("DOSSIER_COMPLETE")
                .setBusinessScopeType("ACTIVE_ORDER_DOSSIER").setBusinessScopeId(20L).setStatus("TODO"));
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.resubmit(10L, command(id))));
        verifyNoInteractions(release);
    }

    @Test void resubmitCreatesNewRoundClosesTaskAndReplaysWithoutRewritingRejection() {
        Long id = create(); upload();
        var result = db.transaction.execute(status -> service.resubmit(10L, command(id)));
        assertEquals(51L, result.applicationId());
        assertEquals("DONE", tasks.selectById(id).getStatus());
        assertEquals("PQC_RELEASE_REJECTED", applications.selectById(50L).getApplicationStatus());
        assertEquals(3, applications.selectById(50L).getVersion());
        assertEquals("补齐成品检文件", applications.selectById(50L).getPqcRejectReason());
        assertEquals(result, db.transaction.execute(status -> service.resubmit(10L, command(id))));
        verify(release, times(1)).applyCorrection(eq(10L), any(), eq(50L), eq(40L));
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.resubmit(10L,
                new MesPqcReleaseCorrectionService.Resubmit(id, 3, "other-key", "已补齐成品检文件"))));
    }

    @Test void auditFailureRollsBackNewRoundAndTaskCompletion() {
        Long id = create(); upload();
        doThrow(new IllegalStateException("audit unavailable")).when(audit).append(any(GxpAuditCommand.class));
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.resubmit(10L, command(id))));
        assertEquals("TODO", tasks.selectById(id).getStatus());
        assertNull(applications.selectById(51L));
        assertEquals("PQC_RELEASE_REJECTED", applications.selectById(50L).getApplicationStatus());
    }

    @Test void simultaneousResubmitsReturnOneNewRoundAndOneCompletedTask() throws Exception {
        Long id = create(); upload();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<MesPqcReleaseCorrectionService.Receipt> submit = () -> {
            TenantContextHolder.setTenantId(1L);
            try {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return db.transaction.execute(status -> service.resubmit(10L, command(id)));
            } finally { TenantContextHolder.clear(); }
        };
        try {
            var first = pool.submit(submit);
            var second = pool.submit(submit);
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(2L, db.jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_active_order_release_application", Long.class));
            assertEquals("DONE", tasks.selectById(id).getStatus());
            verify(release, times(1)).applyCorrection(eq(10L), any(), eq(50L), eq(40L));
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void latestBatchLookupReturnsNewRoundAndKeepsRejectedHistory() {
        var next = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(51L)
                .setActiveOrderId(20L).setWorkOrderId(30L).setBatchExecutionId(40L)
                .setApplicationStatus("PQC_RELEASE_PENDING").setPqcReleaseWorkTaskId(71L);
        next.setTenantId(1L); applications.insert(next);
        assertEquals(51L, applications.selectByBatchExecutionId(40L).getId());
        assertEquals("REJECT", applications.selectById(50L).getPqcDecision());
        assertEquals("补齐成品检文件", applications.selectById(50L).getPqcRejectReason());
    }

    @Test void originalApplicantExplicitlyReceivesMissingHistoricalCorrectionOnlyOnce() {
        Long id = db.transaction.execute(status -> service.receive(10L, 50L));
        assertEquals(id, db.transaction.execute(status -> service.receive(10L, 50L)));
        verify(notifyApi, times(1)).sendSingleMessageIdempotentlyToAdmin(argThat(notice ->
                notice.getUserId().equals(10L) && notice.getTemplateCode().equals("MES_EDHR_PQC_CORRECTION_ASSIGNED")
                        && notice.getTemplateParams().get("reason").equals("补齐成品检文件")));
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.receive(12L, 50L)));
    }

    @Test void notificationFailureRollsBackCreationAndLeavesHistoricalRejectionIntact() {
        when(notifyApi.sendSingleMessageIdempotentlyToAdmin(any())).thenThrow(new IllegalStateException("template missing"));
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.receive(10L, 50L)));
        assertEquals(0L, db.jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_work_task", Long.class));
        assertEquals("PQC_RELEASE_REJECTED", applications.selectById(50L).getApplicationStatus());
    }
    private void withdraw() throws Exception {
        applications.updateById(new MesProcessPoolActiveOrderReleaseApplicationDO().setId(50L)
                .setApplicationStatus("MARKET_RELEASE_WITHDRAWN").setPqcDecision("APPROVE")
                .setReleaseTransactionId(90L).setReleaseApprovalWorkTaskId(91L));
        var tx = new MesProEdhrReleaseTransactionDO().setId(90L).setBatchExecutionId(40L)
                .setReleaseStatus("WITHDRAWN").setWithdrawnBy(10L).setWithdrawnAt(java.time.LocalDateTime.now())
                .setWithdrawReason("补齐成品检文件");
        tx.setTenantId(1L); db.mapper(MesProEdhrReleaseTransactionMapper.class).insert(tx);
    }

    @Test void marketWithdrawalRereadsPersistedDecisionAndCreatesOriginalApplicantCorrection() throws Exception {
        withdraw(); // The supplied application remains the pre-decision snapshot; DB is authoritative.
        Long id = create();
        var task = tasks.selectById(id);
        assertEquals("MARKET_WITHDRAWAL", task.getReviewSourceType());
        assertEquals(90L, task.getReviewSourceId());
        assertEquals("上市放行补正", task.getProcessName());
        assertEquals("APPROVE", applications.selectById(50L).getPqcDecision());
        assertEquals(70L, applications.selectById(50L).getPqcReleaseWorkTaskId());
        assertTrue(service.get(10L, id).writable());
        assertTrue(service.requireMarketCorrectionMutation(10L, 20L, 50L));
        assertThrows(RuntimeException.class, () -> service.requireMarketCorrectionMutation(12L, 20L, 50L));
        assertThrows(RuntimeException.class, () -> service.requireMarketCorrectionMutation(10L, 20L, 99L));
    }

    @Test void marketCorrectionCreatesNewPqcRoundAndKeepsWithdrawnAndApprovedFacts() throws Exception {
        withdraw(); Long id = create(); upload();
        var result = db.transaction.execute(status -> service.resubmit(10L, command(id)));
        assertEquals(51L, result.applicationId());
        var old = applications.selectById(50L);
        assertEquals("MARKET_RELEASE_WITHDRAWN", old.getApplicationStatus());
        assertEquals("APPROVE", old.getPqcDecision());
        assertEquals(70L, old.getPqcReleaseWorkTaskId());
        assertEquals(90L, old.getReleaseTransactionId());
        assertEquals("WITHDRAWN", db.mapper(MesProEdhrReleaseTransactionMapper.class).selectById(90L).getReleaseStatus());
        assertEquals("DONE", tasks.selectById(id).getStatus());
        assertEquals(result, db.transaction.execute(status -> service.resubmit(10L, command(id))));
    }

    @Test void marketCorrectionRejectsUnwithdrawnTransactionAndForgedReason() throws Exception {
        withdraw();
        db.mapper(MesProEdhrReleaseTransactionMapper.class).updateById(new MesProEdhrReleaseTransactionDO()
                .setId(90L).setReleaseStatus("PENDING_APPROVAL"));
        assertThrows(RuntimeException.class, this::create);
        assertEquals(0L, db.jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_work_task", Long.class));
        db.mapper(MesProEdhrReleaseTransactionMapper.class).updateById(new MesProEdhrReleaseTransactionDO()
                .setId(90L).setReleaseStatus("WITHDRAWN"));
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.create(source, 11L, "伪造原因")));
    }

    @Test void rejectedMarketReleaseRequiresItsFormalQaConcessionBeforeExplicitReceive() throws Exception {
        withdraw();
        applications.updateById(new MesProcessPoolActiveOrderReleaseApplicationDO().setId(50L).setApplicationStatus("MARKET_RELEASE_REJECTED"));
        var rejectedAt = java.time.LocalDateTime.now().minusMinutes(2);
        db.mapper(MesProEdhrReleaseTransactionMapper.class).updateById(new MesProEdhrReleaseTransactionDO().setId(90L)
                .setReleaseStatus("REJECTED").setRejectedBy(11L).setRejectedAt(rejectedAt).setRejectReason("补齐成品检文件"));
        assertThrows(RuntimeException.class, () -> db.transaction.execute(status -> service.receive(10L, 50L)));
        var review = new MesProEdhrNonconformanceReviewDO().setId(100L).setTenantId(1L).setSourceType("ACTIVE_ORDER")
                .setSourceId(20L).setActiveOrderId(20L).setBatchExecutionId(40L).setWorkOrderId(30L)
                .setReviewStatus("closed").setDisposition("concession_release").setQaUserId(12L)
                .setFrozenAt(rejectedAt.plusSeconds(1)).setClosedAt(rejectedAt.plusMinutes(1))
                .setRemark("上市放行不予批准；原放行事务:90");
        db.mapper(MesProEdhrNonconformanceReviewMapper.class).insert(review);
        Long id = db.transaction.execute(status -> service.receive(10L, 50L));
        assertEquals("MARKET_REJECTION", tasks.selectById(id).getReviewSourceType());
        upload();
        var result = db.transaction.execute(status -> service.resubmit(10L, command(id)));
        assertEquals(51L, result.applicationId());
        assertEquals("APPROVE", applications.selectById(50L).getPqcDecision());
        assertEquals("MARKET_RELEASE_REJECTED", applications.selectById(50L).getApplicationStatus());
        assertEquals("REJECTED", db.mapper(MesProEdhrReleaseTransactionMapper.class).selectById(90L).getReleaseStatus());
    }

}
