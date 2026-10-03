package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyTemplateService;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.h2.api.Trigger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Real disposable H2, actual notification/platform chain and actual GxP writer with its five Mappers.
 * Only template directory, permission and formal task reads are explicit boundary doubles.
 * H2 policy rows are isolated TEST_ONLY fixtures, never runtime approval or activation evidence.
 * No manual detachment/reset of the kernel's transaction resource is permitted in this fixture.
 */
@Execution(ExecutionMode.SAME_THREAD)
class MesReleaseTaskNotificationKernelTransactionTest {
    private static final String PREFIX = "mes.release-task-notification.";
    private static final String DELIVERY = "mes_pro_edhr_release_task_notify_delivery";
    private static final String EVENT = "gxp_audit_event";
    private static final String RELATION = "gxp_audit_event_relation";
    private static final String TEMPLATE = "MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED";
    private static final String POLICY = "TEST_ONLY-notification-kernel-fixture";
    private static final String KEY = "MES_EDHR_RELEASE_TASK_ASSIGNED:900:USER:346";
    private static final List<FailureObservation> FAILURES = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean FAILURE_CONSUMED = new AtomicBoolean();
    private static String failAction;
    private Fixture fixture;

    @BeforeEach
    void resetFailureInjection() {
        failAction = null;
        FAILURES.clear();
        FAILURE_CONSUMED.set(false);
    }

    @AfterEach
    void closeFixture() throws SQLException {
        // As in the existing real-kernel fixture, avoid JdbcTemplate warning inspection after H2 closes itself.
        if (fixture != null) {
            try (var connection = fixture.source.getConnection(); var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        // Do not unbind the GxP resource here: only the actual production synchronization may do so.
    }

    @Test
    void fourOperationsPersistRealActorRelationsSnapshotsAndHashChain() throws Exception {
        Fixture f = open();
        f.task.setCandidateUserSnapshot("346,347");
        f.templateStatus = call -> call == 1 ? 0 : 1;
        LocalDateTime earliestUtc = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        f.schedule();
        assertEquals(2, f.count(DELIVERY));
        assertEquals(1, f.count("system_notify_message"));
        assertEquals(Set.of(PREFIX + "created", PREFIX + "attempted", PREFIX + "sent", PREFIX + "failed"),
                Set.copyOf(f.events().stream().map(GxpAuditEventDO::getOperationId).toList()));
        assertEquals(6, f.count(EVENT));
        assertEquals(12, f.count(RELATION));
        assertKernelLedger(f, 341L, earliestUtc);
        for (GxpAuditEventDO event : f.events()) {
            assertEquals("SYSTEM", event.getReasonSource());
            assertEquals("RELEASE_NOTIFICATION_DISPATCH", event.getReasonCode());
        }
        assertEquals("SENT", f.status(f.idFor(346L)));
        assertEquals("FAILED", f.status(f.idFor(347L)));
        assertNotNull(f.jdbc.queryForObject("SELECT last_error_summary FROM " + DELIVERY + " WHERE user_id=347", String.class));
    }

    @Test
    void doneManualRetryPersistsActualUserOriginEvenWithAutomaticWords() throws Exception {
        Fixture f = open();
        f.templateStatus = call -> 1;
        f.schedule();
        Long id = f.idFor(346L);
        assertEquals("FAILED", f.status(id));
        f.task.setStatus("DONE");
        Map<String, Object> frozen = f.frozen(id);
        f.actor(1L, 346L);
        f.templateStatus = call -> 0;
        int priorEvents = f.count(EVENT);
        f.retry(id, f.version(id), MesReleaseTaskNotificationAudit.AUTOMATIC_REASON);
        assertEquals("SENT", f.status(id));
        assertEquals("DONE", f.task.getStatus());
        assertEquals(frozen, f.frozen(id));
        assertEquals(1, f.count("system_notify_message"));
        List<GxpAuditEventDO> newEvents = f.events().subList(priorEvents, f.count(EVENT));
        assertEquals(2, newEvents.size());
        for (GxpAuditEventDO event : newEvents) {
            assertEquals(346L, event.getActorId());
            assertEquals("kernel-notify-346", event.getActorUsername());
            assertEquals("USER", event.getReasonSource());
            assertEquals("RELEASE_NOTIFICATION_RETRY", event.getReasonCode());
            assertEquals(MesReleaseTaskNotificationAudit.AUTOMATIC_REASON, event.getReason());
            assertEquals("346", String.valueOf(JsonUtils.parseObject(event.getAuthenticatedActorJson(), Map.class).get("actorId")));
        }
        int events = f.count(EVENT);
        assertThrows(RuntimeException.class, () -> f.retry(id, f.version(id), "已成功不得重放"));
        assertEquals(1, f.count("system_notify_message"));
        assertEquals(events, f.count(EVENT));
        verify(f.taskMapper, never()).updateById(any(MesProEdhrWorkTaskDO.class));
    }

    @Test
    void sourceRollbackRemovesRealCreatedLedgerAndNeverRunsDispatch() throws Exception {
        Fixture f = open();
        f.task.setCandidateUserSnapshot("346,347");
        assertThrows(IllegalStateException.class, () -> f.business.executeWithoutResult(tx -> {
            f.jdbc.update("INSERT INTO parent_marker(id) VALUES(1)");
            f.scheduler.scheduleAssigned(f.task, 341L);
            assertEquals(2, f.count(DELIVERY));
            assertEquals(2, f.count(EVENT));
            assertEquals(4, f.count(RELATION));
            assertEquals(3L, f.watermark());
            assertEquals(0, f.count("system_notify_message"));
            throw new IllegalStateException("injected source failure after actual created ledger SQL");
        }));
        assertEquals(0, f.count("parent_marker"));
        assertEquals(0, f.count(DELIVERY));
        assertEquals(0, f.count(EVENT));
        assertEquals(0, f.count(RELATION));
        assertEquals(1L, f.watermark());
        assertEquals(0, f.count("system_notify_message"));
    }

    @Test
    void createdRelationFailureRollsBackParentIntentLedgerAndDispatch() throws Exception {
        Fixture f = open();
        f.failRelation("created");
        assertThrows(RuntimeException.class, f::schedule);
        assertPhysicalFailure("created", "PENDING", 0, 0);
        assertEquals(1, FAILURES.get(0).parentRows());
        assertEquals(0, f.count("parent_marker"));
        assertEquals(0, f.count(DELIVERY));
        assertEquals(0, f.count(EVENT));
        assertEquals(0, f.count(RELATION));
        assertEquals(1L, f.watermark());
        assertEquals(0, f.count("system_notify_message"));
    }

    @Test
    void attemptRelationFailureRollsBackReceiptAndLedgerBeforePlatformSend() throws Exception {
        Fixture f = open();
        f.seedPending();
        f.actor(1L, 346L);
        f.failRelation("attempted");
        assertThrows(RuntimeException.class, () -> f.retry(10L, 0, "人工首次尝试"));
        assertPhysicalFailure("attempted", "PENDING", 1, 0);
        assertEquals("PENDING", f.status(10L));
        assertEquals(0, f.version(10L));
        assertEquals(0, f.jdbc.queryForObject("SELECT attempt_count FROM " + DELIVERY, Integer.class));
        assertEquals(0, f.count(EVENT));
        assertEquals(0, f.count(RELATION));
        assertEquals(1L, f.watermark());
        assertEquals(0, f.count("system_notify_message"));
    }

    @Test
    void sentRelationFailurePreservesPlatformCommitAndStableRecovery() throws Exception {
        Fixture f = open();
        f.seedPending();
        f.actor(1L, 346L);
        Map<String, Object> frozen = f.frozen(10L);
        f.failRelation("sent");
        assertThrows(RuntimeException.class, () -> f.retry(10L, 0, "首次投递但本地成功审计失败"));
        assertPhysicalFailure("sent", "SENT", 2, 1);
        assertEquals(1, f.count("system_notify_message"));
        Long originalMessage = f.jdbc.queryForObject("SELECT id FROM system_notify_message", Long.class);
        assertEquals("FAILED", f.status(10L), "independent failure ACK must leave an observable recoverable receipt");
        assertEquals(2, f.version(10L));
        assertEquals(List.of(PREFIX + "attempted", PREFIX + "failed"), f.events().stream().map(GxpAuditEventDO::getOperationId).toList());
        assertEquals(3L, f.watermark());
        assertEquals(4, f.count(RELATION));
        f.retry(10L, f.version(10L), "恢复已经提交的平台消息回执");
        assertEquals("SENT", f.status(10L));
        assertEquals(frozen, f.frozen(10L));
        assertEquals(1, f.count("system_notify_message"));
        assertEquals(originalMessage, f.jdbc.queryForObject("SELECT system_message_id FROM " + DELIVERY, Long.class));
        assertEquals(KEY, f.jdbc.queryForObject("SELECT business_key FROM system_notify_message", String.class));
        assertEquals(1, f.events().stream().filter(event -> event.getOperationId().equals(PREFIX + "sent")).count());
        assertKernelLedger(f, 346L, f.startedUtc.minusSeconds(1));
    }

    @Test
    void failedRelationFailureKeepsCommittedAttemptWithoutFalseAck() throws Exception {
        Fixture f = open();
        f.seedPending();
        f.actor(1L, 346L);
        f.templateStatus = call -> 1;
        f.failRelation("failed");
        assertThrows(RuntimeException.class, () -> f.retry(10L, 0, "停用模板失败审计也失败"));
        assertPhysicalFailure("failed", "FAILED", 2, 0);
        assertEquals("PENDING", f.status(10L));
        assertEquals(1, f.version(10L));
        assertEquals(1, f.count(EVENT));
        assertEquals(PREFIX + "attempted", f.events().get(0).getOperationId());
        assertEquals(2, f.count(RELATION));
        assertEquals(2L, f.watermark());
        assertEquals(0, f.count("system_notify_message"));
        f.templateStatus = call -> 0;
        f.retry(10L, f.version(10L), "恢复配置及原投递");
        assertEquals("SENT", f.status(10L));
        assertEquals(1, f.count("system_notify_message"));
        assertEquals(0, f.events().stream().filter(event -> event.getOperationId().equals(PREFIX + "failed")).count());
        assertKernelLedger(f, 346L, f.startedUtc.minusSeconds(1));
    }

    @ParameterizedTest
    @CsvSource({"created,false", "created,true", "attempted,false", "attempted,true",
            "sent,false", "sent,true", "failed,false", "failed,true"})
    void missingOperationRejectsMutationWithoutErasingEarlierIndependentCommits(String action, boolean inactive) throws Exception {
        Fixture f = open();
        if (inactive) {
            f.jdbc.update("UPDATE gxp_audit_policy_operation SET active=FALSE WHERE tenant_id=1 AND operation_id=?", PREFIX + action);
        } else {
            f.jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id=?", PREFIX + action);
        }
        if ("created".equals(action)) {
            assertThrows(RuntimeException.class, f::schedule);
            assertEquals(0, f.count("parent_marker"));
            assertEquals(0, f.count(DELIVERY));
        } else {
            f.seedPending();
            f.actor(1L, 346L);
            if ("failed".equals(action)) { f.templateStatus = call -> 1; }
            assertThrows(RuntimeException.class, () -> f.retry(10L, 0, "核验缺失或停用的正式操作"));
            assertEquals(1, f.count(DELIVERY));
            assertEquals("sent".equals(action) ? "FAILED" : "PENDING", f.status(10L));
            assertEquals("attempted".equals(action) ? 0 : "sent".equals(action) ? 2 : 1, f.version(10L));
        }
        int expectedEvents = "sent".equals(action) ? 2 : "failed".equals(action) ? 1 : 0;
        assertEquals(expectedEvents, f.count(EVENT));
        assertEquals(expectedEvents * 2, f.count(RELATION));
        assertEquals(expectedEvents + 1L, f.watermark());
        assertEquals("sent".equals(action) ? 1 : 0, f.count("system_notify_message"));
        assertEquals(0, f.events().stream().filter(event -> event.getOperationId().equals(PREFIX + action)).count());
    }

    @Test
    void afterCommitIndependentWritesDoNotInheritParentTransactionId() throws Exception {
        Fixture f = open();
        f.schedule();
        assertEquals(4, f.manager.commits, "business, attempt, platform and ACK must physically commit independently");
        assertEquals(3, f.count(EVENT));
        assertEquals("SENT", f.status(f.idFor(346L)));
        List<GxpAuditEventDO> events = f.events();
        assertEquals(List.of(PREFIX + "created", PREFIX + "attempted", PREFIX + "sent"),
                events.stream().map(GxpAuditEventDO::getOperationId).toList());
        assertAll("REQUIRES_NEW ledger identity must not inherit afterCommit parent's bound GxP resource",
                () -> assertNotEquals(events.get(0).getTransactionId(), events.get(1).getTransactionId()),
                () -> assertNotEquals(events.get(0).getTransactionId(), events.get(2).getTransactionId()),
                () -> assertNotEquals(events.get(1).getTransactionId(), events.get(2).getTransactionId()));
    }

    @Test
    void tenantIsolationAndAuthenticatedActorRemainExact() throws Exception {
        Fixture f = open();
        f.seedPending();
        f.actor(2L, 346L);
        assertThrows(RuntimeException.class, () -> f.dispatcher.retryDelivery(2L, 346L, 10L, 0, "其他租户不能发送此回执"));
        assertThrows(RuntimeException.class, () -> f.scheduler.listForTask(2L, 346L, 900L));
        assertEquals(0, f.count(EVENT));
        assertEquals(0, f.count("system_notify_message"));
        assertEquals(0, f.version(10L));
        f.actor(1L, 347L);
        assertThrows(RuntimeException.class, () -> f.retry(10L, 0, "不是原收件人"));
        assertEquals(0, f.count(EVENT));
        f.actor(1L, 346L);
        f.retry(10L, 0, "正式本人投递");
        assertEquals(1, f.count("system_notify_message"));
        assertEquals(2, f.count(EVENT));
        assertKernelLedger(f, 346L, f.startedUtc.minusSeconds(1));
        for (GxpAuditEventDO event : f.events()) {
            assertEquals(1L, event.getTenantId());
            assertEquals("346", String.valueOf(JsonUtils.parseObject(event.getPerformedByJson(), Map.class).get("actorId")));
            assertEquals("1", String.valueOf(JsonUtils.parseObject(event.getAuthenticatedActorJson(), Map.class).get("tenantId")));
        }
        assertEquals(1L, f.jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=2", Long.class));
    }

    private Fixture open() throws Exception {
        fixture = new Fixture();
        return fixture;
    }

    private static void assertPhysicalFailure(String action, String status, int version, int platformRows) {
        assertEquals(1, FAILURES.size(), "one actual intended relation SQL failure must occur");
        FailureObservation seen = FAILURES.get(0);
        assertEquals(PREFIX + action, seen.operation());
        assertTrue(seen.deliveryRows() > 0, "actual receipt must have been written before injected failure");
        assertTrue(seen.eventRows() > 0, "actual unified event must already exist before relation failure");
        assertTrue(seen.watermark() > seen.eventSequence(), "actual ledger watermark must already have advanced");
        assertEquals(status, seen.status());
        assertEquals(version, seen.version());
        assertEquals(platformRows, seen.platformRows());
    }

    private static void assertKernelLedger(Fixture f, Long actor, LocalDateTime earliestUtc) {
        String priorHash = null;
        long sequence = 0;
        for (GxpAuditEventDO event : f.events()) {
            assertEquals(++sequence, event.getLedgerSequence());
            assertEquals(priorHash, event.getPreviousEventHash());
            assertEquals(DigestUtil.sha256Hex(event.getCanonicalEventJson()), event.getEventHash());
            assertEquals(DigestUtil.sha256Hex(event.getBeforeStateJson() + "\u001f" + event.getAfterStateJson()), event.getStatePayloadHash());
            assertEquals("SHA-256", event.getAlgorithm());
            assertEquals(POLICY, event.getPolicyVersion());
            assertEquals("MES_RELEASE_NOTIFY_DELIVERY", event.getSubjectType());
            assertEquals(actor, event.getActorId());
            assertEquals("kernel-notify-" + actor, event.getActorUsername());
            assertFalse(event.getServerOccurredAt().isBefore(earliestUtc));
            assertFalse(event.getServerOccurredAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1)));
            assertNotNull(event.getTransactionId());
            assertFalse(event.getTransactionId().isBlank());
            assertNull(event.getSignatureRecordId());
            assertEquals("SUCCESS", event.getResultStatus(), "receipt state mutation SUCCESS is not a claim that FAILED delivery sent");
            String action = event.getOperationId().substring(PREFIX.length());
            assertEquals(sourceLocator(action), event.getSourceLocator());
            Map<?, ?> after = JsonUtils.parseObject(event.getAfterStateJson(), Map.class);
            assertEquals(event.getSubjectId(), String.valueOf(after.get("id")));
            assertEquals(event.getSubjectVersion(), String.valueOf(after.get("rowVersion")));
            assertEquals("900", String.valueOf(after.get("workTaskId")));
            assertEquals(TEMPLATE, after.get("templateCode"));
            assertTrue(after.get("businessKey").toString().startsWith("MES_EDHR_RELEASE_TASK_ASSIGNED:900:USER:"));
            assertEquals("MES_NOTIFY:" + event.getSubjectId() + ":" + action + ":" + event.getSubjectVersion(), event.getIdempotencyKey());
            assertEquals(1, f.jdbc.queryForObject("SELECT COUNT(*) FROM " + RELATION
                    + " WHERE event_id=? AND relation_type='SUBJECT' AND target_type='MES_RELEASE_NOTIFY_DELIVERY'"
                    + " AND target_id=? AND target_version=?", Integer.class, event.getId(), event.getSubjectId(), event.getSubjectVersion()));
            assertEquals(1, f.jdbc.queryForObject("SELECT COUNT(*) FROM " + RELATION
                    + " WHERE event_id=? AND relation_type='SOURCE' AND target_type='MES_WORK_TASK' AND target_id='900'",
                    Integer.class, event.getId()));
            if ("created".equals(action)) {
                assertEquals("ABSENT", event.getBeforeState());
                assertTrue(JsonUtils.parseObject(event.getBeforeStateJson(), Map.class).isEmpty());
            } else {
                Map<?, ?> before = JsonUtils.parseObject(event.getBeforeStateJson(), Map.class);
                assertEquals(event.getSubjectId(), String.valueOf(before.get("id")));
                assertEquals(((Number) before.get("rowVersion")).intValue() + 1,
                        ((Number) after.get("rowVersion")).intValue());
                assertEquals(before.get("businessKey"), after.get("businessKey"));
                assertEquals(before.get("templateParamsJson"), after.get("templateParamsJson"));
                assertEquals(before.get("userId"), after.get("userId"));
            }
            priorHash = event.getEventHash();
        }
        assertEquals(sequence + 1, f.watermark());
    }

    private static String sourceLocator(String action) {
        return "created".equals(action) ? MesReleaseTaskNotificationService.class.getName() + "#scheduleAssigned"
                : MesReleaseTaskNotificationTransactionService.class.getName() + "#"
                + switch (action) { case "attempted" -> "beginAttempt"; case "sent" -> "markSent"; case "failed" -> "markFailed";
                    default -> throw new AssertionError("unexpected action " + action); };
    }

    /** The trigger uses the same real connection, so it observes writes that the actual failing transaction later rolls back. */
    public static final class OperationRelationFailure implements Trigger {
        @Override public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var statement = connection.prepareStatement("SELECT operation_id,subject_id,ledger_sequence FROM gxp_audit_event WHERE id=?")) {
                statement.setObject(1, newRow[2]);
                try (var rows = statement.executeQuery()) {
                    if (!rows.next() || !Objects.equals(PREFIX + failAction, rows.getString(1))
                            || !FAILURE_CONSUMED.compareAndSet(false, true)) { return; }
                    String operation = rows.getString(1);
                    String subject = rows.getString(2);
                    long eventSequence = rows.getLong(3);
                    try (var delivery = connection.prepareStatement("SELECT status,row_version FROM " + DELIVERY + " WHERE id=?")) {
                        delivery.setString(1, subject);
                        try (var actual = delivery.executeQuery()) {
                            if (!actual.next()) { throw new SQLException("injected failure did not reach an actual receipt"); }
                            FAILURES.add(new FailureObservation(operation, count(connection, DELIVERY), count(connection, EVENT),
                                    scalar(connection, "SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=1"),
                                    eventSequence, actual.getString(1), actual.getInt(2), count(connection, "system_notify_message"),
                                    count(connection, "parent_marker")));
                        }
                    }
                    throw new SQLException("injected real relation failure after receipt/event/watermark SQL", "45000");
                }
            }
        }
        private static int count(Connection connection, String table) throws SQLException {
            return (int) scalar(connection, "SELECT COUNT(*) FROM " + table);
        }
        private static long scalar(Connection connection, String sql) throws SQLException {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
                rows.next(); return rows.getLong(1);
            }
        }
    }

    private record FailureObservation(String operation, int deliveryRows, int eventRows, long watermark,
                                      long eventSequence, String status, int version, int platformRows, int parentRows) { }

    private static final class Fixture {
        final LocalDateTime startedUtc = LocalDateTime.now(ZoneOffset.UTC);
        final DriverManagerDataSource source;
        final JdbcTemplate jdbc;
        final CountingTransactions manager;
        final TransactionTemplate business;
        final SqlSessionTemplate sessions;
        final MesReleaseTaskNotificationService scheduler;
        final MesReleaseTaskNotificationDispatchService dispatcher;
        final MesProEdhrWorkTaskMapper taskMapper = mock(MesProEdhrWorkTaskMapper.class);
        final MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO().setId(900L)
                .setTaskType("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(224L).setWorkOrderId(990274L).setWorkOrderCode("WO-KERNEL-FIXTURE")
                .setBatchCode("BATCH-KERNEL-FIXTURE").setProcessName("PQC生产放行").setStatus("TODO")
                .setCandidateUserSnapshot("346").setAssigneeUserId(346L).setSourceUserId(341L)
                .setOwnershipLocked(true).setActionUrl("/mes/production-release/pqc?applicationId=224&workTaskId=900");
        IntUnaryOperator templateStatus = call -> 0;

        Fixture() throws Exception {
            source = new DriverManagerDataSource("jdbc:h2:mem:notification_kernel_" + UUID.randomUUID()
                    + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
            jdbc = new JdbcTemplate(source);
            createSchemas();
            manager = new CountingTransactions(source);
            business = new TransactionTemplate(manager);
            var configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            var global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
            global.setMetaObjectHandler(new DefaultDBFieldHandler());
            GlobalConfigUtils.setGlobalConfig(configuration, global);
            for (Class<?> mapper : List.of(MesReleaseTaskNotifyDeliveryMapper.class, NotifyMessageMapper.class,
                    GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                    GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class)) { configuration.addMapper(mapper); }
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setGlobalConfig(global); factory.setDataSource(source); factory.setConfiguration(configuration);
            sessions = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
            var writer = new GxpAuditServiceImpl();
            field(writer, "auditEventMapper", mapper(GxpAuditEventMapper.class));
            field(writer, "eventRelationMapper", mapper(GxpAuditEventRelationMapper.class));
            field(writer, "ledgerSequenceMapper", mapper(GxpAuditLedgerSequenceMapper.class));
            field(writer, "policyActivationMapper", mapper(GxpAuditPolicyActivationMapper.class));
            field(writer, "policyOperationMapper", mapper(GxpAuditPolicyOperationMapper.class));
            GxpAuditService kernel = (GxpAuditService) proxy(writer, manager);
            var audit = new MesReleaseTaskNotificationAudit();
            field(audit, "gxpAuditService", kernel);
            var transactionTarget = new MesReleaseTaskNotificationTransactionService();
            field(transactionTarget, "deliveryMapper", mapper(MesReleaseTaskNotifyDeliveryMapper.class));
            field(transactionTarget, "audit", audit);
            var transactions = (MesReleaseTaskNotificationTransactionService) proxy(transactionTarget, manager);
            var templates = mock(NotifyTemplateService.class);
            var reads = new AtomicInteger();
            when(templates.getNotifyTemplateByCodeFromCache(TEMPLATE)).thenAnswer(invocation ->
                    new NotifyTemplateDO().setId(9001L).setCode(TEMPLATE).setStatus(templateStatus.applyAsInt(reads.incrementAndGet()))
                            .setType(2).setNickname("TEST_ONLY kernel notification")
                            .setContent("任务{workTaskId}").setParams(List.of("workTaskId")));
            when(templates.formatNotifyTemplateContent(anyString(), anyMap())).thenReturn("TEST_ONLY notification task900");
            var messages = new NotifyMessageServiceImpl();
            field(messages, "notifyMessageMapper", mapper(NotifyMessageMapper.class));
            var sending = new NotifySendServiceImpl();
            field(sending, "notifyMessageService", messages); field(sending, "notifyTemplateService", templates);
            var api = new NotifyMessageSendApiImpl();
            field(api, "notifySendService", proxy(sending, manager));
            var senderTarget = new MesReleaseTaskNotificationPlatformSender();
            field(senderTarget, "notifyMessageSendApi", api);
            var sender = (MesReleaseTaskNotificationPlatformSender) proxy(senderTarget, manager);
            PermissionApi permissions = mock(PermissionApi.class);
            when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenReturn(true);
            when(taskMapper.selectById(anyLong())).thenAnswer(invocation ->
                    Objects.equals(invocation.getArgument(0), task.getId())
                            && Objects.equals(TenantContextHolder.getTenantId(), 1L) ? task : null);
            dispatcher = new MesReleaseTaskNotificationDispatchService();
            field(dispatcher, "deliveryMapper", mapper(MesReleaseTaskNotifyDeliveryMapper.class));
            field(dispatcher, "workTaskMapper", taskMapper); field(dispatcher, "transactionService", transactions);
            field(dispatcher, "platformSender", sender); field(dispatcher, "permissionApi", permissions);
            var schedulerTarget = new MesReleaseTaskNotificationService();
            field(schedulerTarget, "deliveryMapper", mapper(MesReleaseTaskNotifyDeliveryMapper.class));
            field(schedulerTarget, "dispatchService", dispatcher); field(schedulerTarget, "audit", audit);
            field(schedulerTarget, "permissionApi", permissions); field(schedulerTarget, "workTaskMapper", taskMapper);
            scheduler = (MesReleaseTaskNotificationService) proxy(schedulerTarget, manager);
            seedTestOnlyPolicy();
            actor(1L, 341L);
        }

        void schedule() {
            business.executeWithoutResult(tx -> {
                jdbc.update("INSERT INTO parent_marker(id) VALUES(1)");
                scheduler.scheduleAssigned(task, 341L);
                assertEquals(0, count("system_notify_message"), "actual platform chain must wait for source commit");
            });
        }
        void retry(Long id, Integer version, String reason) {
            dispatcher.retryDelivery(1L, SecurityFrameworkUtils.getLoginUserId(), id, version, reason);
        }
        void actor(Long tenant, Long actorId) {
            TenantContextHolder.setTenantId(tenant);
            var user = new LoginUser().setId(actorId).setTenantId(tenant).setUserType(2)
                    .setInfo(Map.of("username", "kernel-notify-" + actorId, LoginUser.INFO_KEY_NICKNAME, "kernel-notify-" + actorId));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        }
        void failRelation(String action) {
            failAction = action;
            jdbc.execute("CREATE TRIGGER fail_notification_relation BEFORE INSERT ON " + RELATION
                    + " FOR EACH ROW CALL '" + OperationRelationFailure.class.getName() + "'");
        }
        int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
        Long watermark() { return jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=1", Long.class); }
        Long idFor(Long user) { return jdbc.queryForObject("SELECT id FROM " + DELIVERY + " WHERE user_id=?", Long.class, user); }
        int version(Long id) { return jdbc.queryForObject("SELECT row_version FROM " + DELIVERY + " WHERE id=?", Integer.class, id); }
        String status(Long id) { return jdbc.queryForObject("SELECT status FROM " + DELIVERY + " WHERE id=?", String.class, id); }
        Map<String, Object> frozen(Long id) {
            return jdbc.queryForMap("SELECT work_task_id,event_type,user_id,business_key,template_code,template_params_json,initiated_by FROM " + DELIVERY + " WHERE id=?", id);
        }
        List<GxpAuditEventDO> events() {
            return jdbc.queryForList("SELECT id FROM " + EVENT + " ORDER BY ledger_sequence", Long.class)
                    .stream().map(id -> mapper(GxpAuditEventMapper.class).selectByTenantIdAndId(1L, id)).toList();
        }
        void seedPending() {
            // Explicit isolated preexisting receipt, not an invented runtime created audit or historical backfill.
            jdbc.update("INSERT INTO " + DELIVERY + " (id,tenant_id,work_task_id,event_type,user_id,business_key,"
                    + "template_code,template_params_json,initiated_by,status,attempt_count,row_version)"
                    + " VALUES(10,1,900,'ASSIGNED',346,?,?, '{\"workTaskId\":\"900\"}',341,'PENDING',0,0)", KEY, TEMPLATE);
        }
        <T> T mapper(Class<T> type) { return sessions.getMapper(type); }

        private void seedTestOnlyPolicy() {
            for (Long tenant : List.of(1L, 2L)) {
                jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(?,1)", tenant);
                jdbc.update("INSERT INTO gxp_audit_policy_activation(tenant_id,policy_version,policy_hash,request_id,actor_id,"
                        + "approval_reference,activated_at_utc,effective_after_sequence,canonical_activation_json,activation_hash)"
                        + " VALUES(?,?,'TEST_ONLY-H2','TEST_ONLY-H2',341,'TEST_ONLY-NOT-RUNTIME-APPROVAL',CURRENT_TIMESTAMP,0,'{}','TEST_ONLY-H2')", tenant, POLICY);
                for (String action : List.of("created", "attempted", "sent", "failed")) {
                    jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,"
                            + "source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,"
                            + "retention_class,test_ids,owner,applicability,active) VALUES(?,?,?,'SERVICE_METHOD',?,'MES',"
                            + "'MES_RELEASE_NOTIFY_DELIVERY',?,?,'NONE',?,'GXP_MES_WORK_TASK','TEST_ONLY-KERNEL',"
                            + "'TEST_ONLY-owner','GXP',TRUE)", tenant, POLICY, PREFIX + action, sourceLocator(action),
                            "created".equals(action) ? "ADD" : "UPDATE", "created".equals(action) ? "SYSTEM" : "SYSTEM_WITH_OPTIONAL_TEXT",
                            "created".equals(action) ? "ABSENT_TO_PRESENT" : "PRESENT_TO_PRESENT");
                }
            }
        }

        private void createSchemas() throws Exception {
            String systemSchema = Files.readString(backendRoot().resolve("yudao-module-system/src/test/resources/sql/create_tables.sql"), StandardCharsets.UTF_8);
            for (String table : List.of(EVENT, RELATION, "gxp_audit_ledger_sequence", "gxp_audit_policy_activation", "gxp_audit_policy_operation")) {
                var ddl = Pattern.compile("(?is)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+\"" + table
                        + "\"\\s*\\(.*?\\)\\s*COMMENT\\s*'[^']*'\\s*;").matcher(systemSchema);
                assertTrue(ddl.find(), "complete existing physical H2 audit DDL is required for " + table);
                jdbc.execute(ddl.group());
            }
            jdbc.execute("""
                    CREATE TABLE mes_pro_edhr_release_task_notify_delivery (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,work_task_id BIGINT NOT NULL,
                      event_type VARCHAR(32) NOT NULL,user_id BIGINT NOT NULL,business_key VARCHAR(255) NOT NULL,
                      template_code VARCHAR(128) NOT NULL,template_params_json LONGTEXT NOT NULL,initiated_by BIGINT NOT NULL,
                      status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,attempt_count INT DEFAULT 0 NOT NULL,
                      row_version INT DEFAULT 0 NOT NULL,last_attempt_at TIMESTAMP,sent_at TIMESTAMP,
                      system_message_id BIGINT,last_error_summary VARCHAR(512),creator VARCHAR(64),updater VARCHAR(64),
                      create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      deleted BOOLEAN DEFAULT FALSE NOT NULL,UNIQUE(tenant_id,work_task_id,event_type,user_id),UNIQUE(tenant_id,business_key))
                    """);
            jdbc.execute("""
                    CREATE TABLE system_notify_message (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,user_id BIGINT NOT NULL,user_type INT NOT NULL,
                      business_key VARCHAR(255),template_id BIGINT,template_code VARCHAR(128),template_type INT,
                      template_nickname VARCHAR(255),template_content LONGTEXT,template_params LONGTEXT,
                      read_status BOOLEAN DEFAULT FALSE,read_time TIMESTAMP,creator VARCHAR(64),updater VARCHAR(64),
                      create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      deleted BOOLEAN DEFAULT FALSE,UNIQUE(tenant_id,business_key))
                    """);
            jdbc.execute("CREATE TABLE parent_marker(id BIGINT PRIMARY KEY)");
        }
    }

    private static final class CountingTransactions extends DataSourceTransactionManager {
        int commits;
        CountingTransactions(DriverManagerDataSource source) { super(source); }
        @Override protected void doCommit(DefaultTransactionStatus status) { super.doCommit(status); commits++; }
    }

    private static Object proxy(Object target, DataSourceTransactionManager manager) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }
    private static void field(Object target, String name, Object value) { ReflectionTestUtils.setField(target, name, value); }
    private static Path backendRoot() {
        Path cursor = Path.of("").toAbsolutePath();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve("yudao-module-system/src/test/resources/sql"))) { return cursor; }
            if (Files.isDirectory(cursor.resolve("IntRuoyiBackend/yudao-module-system/src/test/resources/sql"))) { return cursor.resolve("IntRuoyiBackend"); }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("the complete existing backend test schema is a mandatory kernel test prerequisite");
    }
}
