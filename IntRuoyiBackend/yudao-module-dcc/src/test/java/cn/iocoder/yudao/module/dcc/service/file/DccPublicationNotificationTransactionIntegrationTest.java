package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateReasonMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityRuleSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityUserSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationDirectionSnapshotMapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactTaskDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@Import({DccPublicationNotificationTransactionServiceImpl.class, DccPublicationFollowupStatusService.class})
class DccPublicationNotificationTransactionIntegrationTest extends BaseDbUnitTest {

    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DccPublicationNotificationTransactionService transactionService;
    @Resource private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Resource private DccPublicationNotificationAuditMapper auditMapper;
    @Resource private DccPublicationFollowupStatusService statusService;
    @Resource private DccPublicationNotificationCandidateReasonMapper reasonMapper;
    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private DccPublicationImpactTaskMapper impactTaskMapper;
    @Resource private DccPublicationImpactAuditMapper impactTimelineAuditMapper;
    @Resource private DccPublicationNotificationCandidateMapper candidateMapper;
    @Resource private DccPublicationVisibilityRuleSnapshotMapper visibilityRuleMapper;
    @Resource private DccPublicationVisibilityUserSnapshotMapper visibilityUserMapper;
    @Resource private DccPublicationRelationDirectionSnapshotMapper directionMapper;

    @Test
    void beginAttempt_requiresNewCommitsEvenWhenCallingPublicationTransactionRollsBack() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO dcc_publication_notification_delivery
                  (id, batch_id, candidate_id, user_id, business_key, template_code, status,
                   attempt_count, row_version, creation_token, tenant_id, deleted)
                VALUES (10, 7, 11, 21, 'DCC_PUBLICATION:7:USER:21', 'dcc_publication_released',
                        'PENDING', 0, 0, 'tx-attempt-token', 1, 0)
                """);

        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(ignored -> {
                    transactionService.beginAttempt(1L, 10L, 0, null, "发布后自动发送", false);
                    throw new IllegalStateException("outer publication callback failed after attempt commit");
                }));

        assertEquals(1, jdbc.queryForObject(
                "SELECT attempt_count FROM dcc_publication_notification_delivery WHERE id=10", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT row_version FROM dcc_publication_notification_delivery WHERE id=10", Integer.class));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM dcc_publication_notification_audit WHERE delivery_id=10", Long.class));
    }

    @Test
    void sentDeliveryCannotBeReplayedOrMovedBackToFailed() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO dcc_publication_notification_delivery
                  (id, batch_id, candidate_id, user_id, business_key, template_code, status,
                   attempt_count, row_version, creation_token, tenant_id, deleted)
                VALUES (10, 7, 11, 21, 'DCC_PUBLICATION:7:USER:21', 'dcc_publication_released',
                        'SENT', 1, 2, 'sent-no-replay', 1, 0)
                """);

        assertThrows(ServiceException.class,
                () -> transactionService.beginAttempt(1L, 10L, 2, 9L, "错误重试", true));

        assertEquals("SENT", jdbc.queryForObject(
                "SELECT status FROM dcc_publication_notification_delivery WHERE id=10", String.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT attempt_count FROM dcc_publication_notification_delivery WHERE id=10", Integer.class));
    }

    @Test
    void platformCommitThenAckFailurePersistsFailedAndRetryUsesSameMessageIdentity() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO dcc_publication_followup_batch
                  (id, published_controlled_file_id, published_master_id, category_id, file_number_snapshot,
                   file_name_snapshot, version_no_snapshot, status, published_at, creation_token, tenant_id, deleted)
                VALUES (7, 70, 700, 20, 'DOC-70', '作业指导书', 'B/1', 'PROCESSING',
                        TIMESTAMP '2026-09-07 10:00:00', 'ack-loss-batch', 1, 0)
                """);
        jdbc.update("""
                INSERT INTO dcc_publication_notification_delivery
                  (id, batch_id, candidate_id, user_id, business_key, template_code, status,
                   attempt_count, row_version, creation_token, tenant_id, deleted)
                VALUES (10, 7, 11, 21, 'DCC_PUBLICATION:7:USER:21', 'dcc_publication_released',
                        'PENDING', 0, 0, 'ack-loss-delivery', 1, 0)
                """);
        jdbc.update("""
                INSERT INTO dcc_publication_notification_candidate_reason
                  (id, batch_id, candidate_id, reason_type, source_id, reason_summary, tenant_id, deleted)
                VALUES (31, 7, 11, 'FILE_OWNER', 70, '发布文件责任人', 1, 0)
                """);
        NotifyMessageSendApi platform = mock(NotifyMessageSendApi.class);
        when(platform.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(9001L);
        AtomicBoolean failFirstAck = new AtomicBoolean(true);
        DccPublicationNotificationTransactionService ackFailingWorker =
                new DccPublicationNotificationTransactionService() {
                    @Override
                    public DccPublicationNotificationDeliveryDO beginAttempt(
                            Long tenantId, Long deliveryId, Integer expectedVersion,
                            Long actorId, String reason, boolean retry) {
                        return transactionService.beginAttempt(
                                tenantId, deliveryId, expectedVersion, actorId, reason, retry);
                    }

                    @Override
                    public void markSent(Long tenantId, Long deliveryId, Integer expectedVersion,
                                         Long messageId, Long actorId, String reason) {
                        if (failFirstAck.getAndSet(false)) {
                            throw new IllegalStateException("injected DCC ACK failure");
                        }
                        transactionService.markSent(
                                tenantId, deliveryId, expectedVersion, messageId, actorId, reason);
                    }

                    @Override
                    public void markFailed(Long tenantId, Long deliveryId, Integer expectedVersion,
                                           Long actorId, String reason, String errorSummary) {
                        transactionService.markFailed(
                                tenantId, deliveryId, expectedVersion, actorId, reason, errorSummary);
                    }
                };
        DccPublicationNotificationDispatchOrchestrator orchestrator =
                new DccPublicationNotificationDispatchOrchestrator();
        ReflectionTestUtils.setField(orchestrator, "deliveryMapper", deliveryMapper);
        ReflectionTestUtils.setField(orchestrator, "reasonMapper", reasonMapper);
        ReflectionTestUtils.setField(orchestrator, "batchMapper", batchMapper);
        ReflectionTestUtils.setField(orchestrator, "notifyMessageSendApi", platform);
        ReflectionTestUtils.setField(orchestrator, "transactionService", ackFailingWorker);

        assertThrows(IllegalStateException.class,
                () -> orchestrator.retryDelivery(1L, 9L, 10L, 0, "人工重试"));
        assertEquals("FAILED", jdbc.queryForObject(
                "SELECT status FROM dcc_publication_notification_delivery WHERE id=10", String.class));
        assertEquals(2, jdbc.queryForObject(
                "SELECT row_version FROM dcc_publication_notification_delivery WHERE id=10", Integer.class));
        orchestrator.retryDelivery(1L, 9L, 10L, 2, "人工重试");

        assertEquals("SENT", jdbc.queryForObject(
                "SELECT status FROM dcc_publication_notification_delivery WHERE id=10", String.class));
        assertEquals(4, jdbc.queryForObject(
                "SELECT row_version FROM dcc_publication_notification_delivery WHERE id=10", Integer.class));
        assertEquals(9001L, jdbc.queryForObject(
                "SELECT system_message_id FROM dcc_publication_notification_delivery WHERE id=10", Long.class));
        ArgumentCaptor<NotifySendSingleToUserIdempotentReqDTO> requests =
                ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(platform, times(2)).sendSingleMessageIdempotentlyToAdmin(requests.capture());
        assertEquals(requests.getAllValues().get(0).getBusinessKey(),
                requests.getAllValues().get(1).getBusinessKey());
    }

    @Test
    void defaultMyWorkKeepsOutstandingRevisionTrackingAndExplicitCompletedFilterIsUnchanged() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        insertImpactTask(jdbc, 20L, 120L, "PENDING", null, "NOT_APPLICABLE");
        insertImpactTask(jdbc, 21L, 121L, "COMPLETED", "REVISION_REQUIRED", "NOT_STARTED");
        insertImpactTask(jdbc, 22L, 122L, "COMPLETED", "REVISION_REQUIRED", "REVISION_LINKED");
        insertImpactTask(jdbc, 23L, 123L, "COMPLETED", "NO_REVISION_REQUIRED", "NOT_APPLICABLE");
        insertImpactTask(jdbc, 24L, 124L, "COMPLETED", "REVISION_REQUIRED", "RESOLVED");

        Page<DccPublicationImpactTaskDO> defaultPage = impactTaskMapper.selectAssigneePage(
                new Page<>(1, 20), 1L, 99L, null, null);
        Page<DccPublicationImpactTaskDO> explicitCompleted = impactTaskMapper.selectAssigneePage(
                new Page<>(1, 20), 1L, 99L, "COMPLETED", null);

        assertEquals(java.util.Set.of(20L, 21L, 22L), defaultPage.getRecords().stream()
                .map(DccPublicationImpactTaskDO::getId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of(21L, 22L, 23L, 24L), explicitCompleted.getRecords().stream()
                .map(DccPublicationImpactTaskDO::getId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void timelineQueryUsesRealAuditMappersCurrentViewGuardAndStableCrossSourceOrdering() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO dcc_publication_followup_batch
                  (id, published_controlled_file_id, published_master_id, category_id, file_number_snapshot,
                   file_name_snapshot, version_no_snapshot, status, published_at, creation_token, tenant_id, deleted)
                VALUES (7, 70, 700, 20, 'DOC-70', '作业指导书', 'B/1', 'READY',
                        TIMESTAMP '2026-09-07 10:00:00', 'timeline-batch', 1, 0)
                """);
        jdbc.update("""
                INSERT INTO dcc_publication_notification_candidate
                  (id, batch_id, user_id, user_name_snapshot, dept_name_snapshot, resolution_status, tenant_id, deleted)
                VALUES (11, 7, 21, '收件人', '质量部', 'ACTIVE', 1, 0)
                """);
        jdbc.update("""
                INSERT INTO dcc_publication_notification_candidate_reason
                  (id, batch_id, candidate_id, reason_type, source_id, reason_summary, tenant_id, deleted)
                VALUES (31, 7, 11, 'FILE_OWNER', 70, '发布文件责任人', 1, 0)
                """);
        jdbc.update("""
                INSERT INTO dcc_publication_notification_delivery
                  (id, batch_id, candidate_id, user_id, business_key, template_code, status,
                   attempt_count, row_version, creation_token, tenant_id, deleted)
                VALUES (10, 7, 11, 21, 'DCC_PUBLICATION:7:USER:21', 'dcc_publication_released',
                        'SENT', 2, 4, 'timeline-delivery', 1, 0)
                """);
        insertImpactTask(jdbc, 20L, 120L, "COMPLETED", "REVISION_REQUIRED", "REVISION_LINKED");
        jdbc.update("""
                UPDATE dcc_publication_impact_task
                   SET assignee_user_id=9007199254740997,
                       linked_revision_controlled_file_id=9007199254740993,
                       linked_revision_version_snapshot='C/1'
                 WHERE id=20
                """);
        jdbc.update("""
                INSERT INTO dcc_publication_relation_direction_snapshot
                  (id, batch_id, relation_snapshot_id, direction, source_relation_id,
                   source_controlled_file_id, target_controlled_file_id, relation_source_snapshot,
                   tenant_id, deleted)
                VALUES (41, 7, 1020, 'FORWARD', 501, 70, 120, 'UPLOAD', 1, 0),
                       (42, 7, 1020, 'REVERSE', 502, 120, 70, 'UPLOAD', 1, 0)
                """);
        jdbc.update("""
                INSERT INTO dcc_publication_notification_audit
                  (id, delivery_id, batch_id, action_type, status_before, status_after, attempt_count,
                   error_summary, row_version_before, row_version_after, occurred_at, tenant_id, deleted)
                VALUES (60, 10, 7, 'MATERIALIZE', NULL, 'PENDING', 0,
                        NULL, 0, 0, TIMESTAMP '2026-09-07 10:00:30', 1, 0),
                       (61, 10, 7, 'FAILED', 'PENDING', 'FAILED', 1,
                        'IllegalStateException: publication notification delivery failed', 1, 2,
                        TIMESTAMP '2026-09-07 10:01:00', 1, 0),
                       (68, 10, 7, 'SENT', 'FAILED', 'SENT', 2, NULL, 3, 4,
                        TIMESTAMP '2026-09-07 10:06:00', 1, 0)
                """);
        jdbc.update("""
                INSERT INTO dcc_publication_impact_audit
                  (id, task_id, batch_id, action_type, actor_id, reason, status_before, status_after,
                   assignee_before, assignee_after, decision_snapshot, linked_revision_controlled_file_id,
                   row_version_before, row_version_after, occurred_at, tenant_id, deleted)
                VALUES (62, 20, 7, 'START', 99, NULL, 'PENDING', 'IN_REVIEW',
                        NULL, NULL, NULL, NULL, 0, 1,
                        TIMESTAMP '2026-09-07 10:01:00', 1, 0),
                       (63, 20, 7, 'REASSIGN', 99, '调整负责人', 'PENDING', 'PENDING',
                        9007199254740995, 9007199254740997, NULL, NULL, 1, 2,
                        TIMESTAMP '2026-09-07 10:02:00', 1, 0),
                       (64, 20, 7, 'DECIDE', 99, '需要同步', 'IN_REVIEW', 'COMPLETED',
                        NULL, NULL, 'REVISION_REQUIRED', NULL, 2, 3,
                        TIMESTAMP '2026-09-07 10:03:00', 1, 0),
                       (67, 20, 7, 'LINK_REVISION', 99, '历史关联版本', 'COMPLETED', 'COMPLETED',
                        NULL, NULL, 'REVISION_REQUIRED', 9007199254740991, 2, 3,
                        TIMESTAMP '2026-09-07 10:03:30', 1, 0),
                       (65, 20, 7, 'LINK_REVISION', 99, '关联升级版本', 'COMPLETED', 'COMPLETED',
                        NULL, NULL, 'REVISION_REQUIRED', 9007199254740993, 3, 4,
                        TIMESTAMP '2026-09-07 10:04:00', 1, 0),
                       (66, 20, 7, 'RESOLVE_REVISION', NULL, NULL, 'COMPLETED', 'COMPLETED',
                        NULL, NULL, 'REVISION_REQUIRED', 9007199254740993, 4, 5,
                        TIMESTAMP '2026-09-07 10:05:00', 1, 0)
                """);
        DccControlledFileQueryService access = mock(DccControlledFileQueryService.class);
        DccPublicationFollowupQueryServiceImpl query = new DccPublicationFollowupQueryServiceImpl();
        setQueryField(query, "controlledFileQueryService", access);
        setQueryField(query, "batchMapper", batchMapper);
        setQueryField(query, "deliveryMapper", deliveryMapper);
        setQueryField(query, "candidateMapper", candidateMapper);
        setQueryField(query, "reasonMapper", reasonMapper);
        setQueryField(query, "visibilityRuleMapper", visibilityRuleMapper);
        setQueryField(query, "visibilityUserMapper", visibilityUserMapper);
        setQueryField(query, "impactTaskMapper", impactTaskMapper);
        setQueryField(query, "directionMapper", directionMapper);
        setQueryField(query, "notificationAuditMapper", auditMapper);
        setQueryField(query, "impactAuditMapper", impactTimelineAuditMapper);
        setQueryField(query, "permissionApi", mock(PermissionApi.class));

        TenantContextHolder.setTenantId(1L);
        try {
            var result = query.getFileFollowup(99L, 70L);
            assertEquals(List.of("BATCH", "NOTIFICATION", "NOTIFICATION", "IMPACT", "IMPACT",
                            "IMPACT", "IMPACT", "IMPACT", "IMPACT", "NOTIFICATION"),
                    result.getTimeline().stream().map(event -> event.getSourceType()).toList());
            assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                    result.getTimeline().stream().map(event -> event.getSequenceNo()).toList());
            assertEquals("开始影响评估", result.getTimeline().get(3).getActionLabel());
            assertEquals(List.of("正向关联", "反向引用"),
                    result.getTimeline().get(3).getDirectionLabels());
            assertEquals("需要升版", result.getTimeline().get(5).getDecisionLabel());
            assertEquals("IllegalStateException: publication notification delivery failed",
                    result.getTimeline().get(2).getErrorSummary());
            assertNull(result.getTimeline().get(1).getAttemptCount());
            assertEquals(1, result.getTimeline().get(2).getAttemptCount());
            assertEquals(2, result.getTimeline().get(9).getAttemptCount());
            assertEquals("9007199254740995", result.getTimeline().get(4).getAssigneeBefore());
            assertEquals("9007199254740997", result.getTimeline().get(4).getAssigneeAfter());
            assertNull(result.getTimeline().get(3).getAssigneeBefore());
            assertNull(result.getTimeline().get(5).getAssigneeAfter());
            assertEquals("9007199254740991", result.getTimeline().get(6).getLinkedRevisionControlledFileId());
            assertNull(result.getTimeline().get(6).getLinkedRevisionVersion());
            assertEquals("9007199254740993", result.getTimeline().get(7).getLinkedRevisionControlledFileId());
            assertEquals("C/1", result.getTimeline().get(7).getLinkedRevisionVersion());
            assertEquals("9007199254740993", result.getTimeline().get(8).getLinkedRevisionControlledFileId());
            assertEquals("C/1", result.getTimeline().get(8).getLinkedRevisionVersion());
            assertNull(result.getTimeline().get(5).getLinkedRevisionControlledFileId());
            verify(access).getControlledFile(99L, 70L);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void setQueryField(DccPublicationFollowupQueryServiceImpl query, String name, Object value) {
        ReflectionTestUtils.setField(query, name, value);
    }

    private void insertImpactTask(JdbcTemplate jdbc, Long id, Long relatedMasterId,
                                  String taskStatus, String decision, String trackingStatus) {
        jdbc.update("""
                INSERT INTO dcc_publication_impact_task
                  (id, batch_id, publication_relation_snapshot_id, published_controlled_file_id,
                   related_master_id, related_active_controlled_file_id, assignee_user_id,
                   task_status, decision, revision_tracking_status, row_version, creation_token,
                   tenant_id, deleted)
                VALUES (?, 7, ?, 70, ?, ?, 99, ?, ?, ?, 0, ?, 1, 0)
                """, id, id + 1000, relatedMasterId, relatedMasterId + 1000, taskStatus,
                decision, trackingStatus, "work-predicate-" + id);
    }

    @Test
    void concurrentDeliveryCompletionSerializesOnBatchAndProducesFinalAuthoritativeStatus() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        seedBatchAndDeliveries(jdbc);
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> updateDeliveryAndRefresh(jdbc, start, 10L, "SENT"));
            Future<?> second = executor.submit(() -> updateDeliveryAndRefresh(jdbc, start, 11L, "SENT"));
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }
        assertEquals("COMPLETED", jdbc.queryForObject(
                "SELECT status FROM dcc_publication_followup_batch WHERE id=7", String.class));
    }

    @Test
    void concurrentDeliveryFailureSerializesOnBatchAndKeepsPartialFailedAuthoritative() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        seedBatchAndDeliveries(jdbc);
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> updateDeliveryAndRefresh(jdbc, start, 10L, "SENT"));
            Future<?> second = executor.submit(() -> updateDeliveryAndRefresh(jdbc, start, 11L, "FAILED"));
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }
        assertEquals("PARTIAL_FAILED", jdbc.queryForObject(
                "SELECT status FROM dcc_publication_followup_batch WHERE id=7", String.class));
    }

    private void seedBatchAndDeliveries(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO dcc_publication_followup_batch
                  (id, published_controlled_file_id, published_master_id, category_id, file_number_snapshot,
                   file_name_snapshot, version_no_snapshot, status, published_at, creation_token, tenant_id, deleted)
                VALUES (7, 70, 700, 20, 'DOC-70', '作业指导书', 'B/1', 'PROCESSING',
                        TIMESTAMP '2026-09-07 10:00:00', 'batch-lock-test', 1, 0)
                """);
        for (long id : new long[]{10L, 11L}) {
            jdbc.update("""
                    INSERT INTO dcc_publication_notification_delivery
                      (id, batch_id, candidate_id, user_id, business_key, template_code, status,
                       attempt_count, row_version, creation_token, tenant_id, deleted)
                    VALUES (?, 7, ?, ?, ?, 'dcc_publication_released', 'PENDING', 1, 1, ?, 1, 0)
                    """, id, id + 100, id + 200, "DCC_PUBLICATION:7:USER:" + (id + 200), "delivery-" + id);
        }
    }

    private void updateDeliveryAndRefresh(JdbcTemplate jdbc, CountDownLatch start, Long deliveryId,
                                          String status) {
        try {
            start.await();
            new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
                statusService.lockBatch(1L, 7L);
                jdbc.update("UPDATE dcc_publication_notification_delivery SET status=? WHERE id=?", status, deliveryId);
                statusService.refreshBatchStatus(1L, 7L);
            });
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("concurrent status test interrupted", ex);
        }
    }
}
