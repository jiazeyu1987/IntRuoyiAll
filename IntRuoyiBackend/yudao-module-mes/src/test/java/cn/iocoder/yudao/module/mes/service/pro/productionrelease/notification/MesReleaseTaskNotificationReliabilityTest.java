package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyTemplateService;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * True isolated H2 transactions, actual delivery/message Mappers and actual platform sending chain.
 * Only template directory, permissions, task read and unified audit kernel are boundary doubles.
 * Missing implementation types are reported as assertion RED, never compilation RED or a skip.
 * Reflection is confined to test bootstrapping; production calls use ordinary typed contracts.
 */
class MesReleaseTaskNotificationReliabilityTest {
    private static final String BASE = "cn.iocoder.yudao.module.mes.";
    private static final String PKG = BASE + "service.pro.productionrelease.notification.";
    private static final String PQC_TEMPLATE = "MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED";
    private static final String KEY = "MES_EDHR_RELEASE_TASK_ASSIGNED:900:USER:346";

    @AfterEach
    void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void allFrozenRecipientsPersistBeforeCommitAndMessagesStartAfterCommit() throws Exception {
        Fixture f = new Fixture();
        f.business.executeWithoutResult(tx -> {
            f.schedule(f.task);
            assertEquals(2, f.count("mes_pro_edhr_release_task_notify_delivery"));
            assertEquals(0, f.count("system_notify_message"), "business transaction must not send early");
        });
        assertEquals(2, f.count("system_notify_message"));
        assertEquals(List.of(346L, 347L), f.jdbc.queryForList(
                "SELECT user_id FROM mes_pro_edhr_release_task_notify_delivery ORDER BY user_id", Long.class));
        assertEquals(2, f.jdbc.queryForObject(
                "SELECT COUNT(*) FROM mes_pro_edhr_release_task_notify_delivery WHERE status='SENT'", Integer.class));
        assertEquals(341L, f.jdbc.queryForObject(
                "SELECT MIN(initiated_by) FROM mes_pro_edhr_release_task_notify_delivery", Long.class));
        assertEquals(1L, f.jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_notify_message WHERE user_id=347", Long.class),
                "assignee346 is not the only frozen recipient");
    }

    @Test
    void businessRollbackRemovesEveryDeliveryAndAuditAndSendsNothing() throws Exception {
        Fixture f = new Fixture();
        assertThrows(IllegalStateException.class, () -> f.business.executeWithoutResult(tx -> {
            f.schedule(f.task);
            assertEquals(2, f.count("mes_pro_edhr_release_task_notify_delivery"));
            assertTrue(f.count("notification_test_audit") > 0);
            throw new IllegalStateException("injected business failure after persisted intent");
        }));
        assertEquals(0, f.count("mes_pro_edhr_release_task_notify_delivery"));
        assertEquals(0, f.count("notification_test_audit"));
        assertEquals(0, f.count("system_notify_message"));
    }

    @Test
    void missingTransactionOrInvalidCandidateSnapshotFailsBeforeFirstWrite() throws Exception {
        Fixture f = new Fixture();
        assertThrows(RuntimeException.class, () -> f.schedule(f.task));
        f.task.setCandidateUserSnapshot("346,bad-id,347");
        assertThrows(RuntimeException.class, () -> f.business.executeWithoutResult(tx -> f.schedule(f.task)));
        assertEquals(0, f.count("mes_pro_edhr_release_task_notify_delivery"));
        assertEquals(0, f.count("system_notify_message"));
    }

    @Test
    void missingTemplateDoesNotRollBackBusinessAndPersistsVisibleFailureAfterCommit() throws Exception {
        Fixture f = new Fixture();
        when(f.templates.getNotifyTemplateByCodeFromCache(PQC_TEMPLATE)).thenReturn(null);
        assertDoesNotThrow(() -> f.business.executeWithoutResult(tx -> {
            f.schedule(f.task);
            assertEquals(2, f.count("mes_pro_edhr_release_task_notify_delivery"));
            assertEquals(0, f.count("system_notify_message"));
        }), "missing notification template cannot reverse the formal business commit");
        assertEquals(2, f.count("mes_pro_edhr_release_task_notify_delivery"));
        assertEquals(2, f.jdbc.queryForObject(
                "SELECT COUNT(*) FROM mes_pro_edhr_release_task_notify_delivery WHERE status='FAILED'", Integer.class));
        assertNotNull(f.jdbc.queryForObject(
                "SELECT last_error_summary FROM mes_pro_edhr_release_task_notify_delivery WHERE user_id=346", String.class));
        assertEquals(0, f.count("system_notify_message"));
        assertFalse(((List<?>) invoke(f.service, "listForTask", 1L, 346L, 900L)).isEmpty());
        Long id = f.jdbc.queryForObject(
                "SELECT id FROM mes_pro_edhr_release_task_notify_delivery WHERE user_id=346", Long.class);
        String frozenKey = f.jdbc.queryForObject(
                "SELECT business_key FROM mes_pro_edhr_release_task_notify_delivery WHERE id=?", String.class, id);
        when(f.templates.getNotifyTemplateByCodeFromCache(PQC_TEMPLATE)).thenAnswer(call -> f.template);
        f.retry(346L, id, f.version(id), "模板配置恢复后重试原投递");
        assertEquals("SENT", f.status(id));
        assertEquals(frozenKey, f.jdbc.queryForObject(
                "SELECT business_key FROM system_notify_message", String.class));
        assertEquals(1, f.count("system_notify_message"));
    }

    @Test
    void attemptReallyCommitsInIndependentTransactionBeforeCallerRollback() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        assertThrows(IllegalStateException.class, () -> f.business.executeWithoutResult(tx -> {
            f.begin(10L, 0, false);
            throw new IllegalStateException("outer caller rollback");
        }));
        assertEquals(1, f.version(10L));
        assertEquals(1, f.jdbc.queryForObject(
                "SELECT attempt_count FROM mes_pro_edhr_release_task_notify_delivery WHERE id=10", Integer.class));
        assertEquals(1, f.count("notification_test_audit"));
    }

    @Test
    void platformCommitSurvivesAckRollbackAndRetryReturnsExactlySameMessage() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        f.failSentAudit.set(true);
        assertThrows(RuntimeException.class, () -> f.retry(346L, 10L, 0, "首次人工投递"));
        assertEquals(1, f.count("system_notify_message"), "platform transaction must already be committed");
        Long original = f.jdbc.queryForObject("SELECT id FROM system_notify_message", Long.class);
        assertNotEquals("SENT", f.status(10L), "ACK failure must not claim a successful local receipt");
        f.retry(346L, 10L, f.version(10L), "恢复平台已提交而本地回执失败的投递");
        assertEquals("SENT", f.status(10L));
        assertEquals(1, f.count("system_notify_message"));
        assertEquals(original, f.jdbc.queryForObject(
                "SELECT system_message_id FROM mes_pro_edhr_release_task_notify_delivery WHERE id=10", Long.class));
        assertEquals(KEY, f.jdbc.queryForObject("SELECT business_key FROM system_notify_message", String.class));
    }

    @Test
    void disabledAfterPreparationStaysQueryableFailedAndRetryDoesNotChangeBinding() throws Exception {
        Fixture f = new Fixture();
        f.business.executeWithoutResult(tx -> {
            f.schedule(f.task);
            f.template.setStatus(1);
        });
        assertEquals(0, f.count("system_notify_message"));
        assertEquals(2, f.jdbc.queryForObject(
                "SELECT COUNT(*) FROM mes_pro_edhr_release_task_notify_delivery WHERE status='FAILED'", Integer.class));
        assertNotNull(f.jdbc.queryForObject(
                "SELECT last_error_summary FROM mes_pro_edhr_release_task_notify_delivery WHERE user_id=346", String.class));
        Object visible = invoke(f.service, "listForTask", 1L, 346L, 900L);
        assertFalse(((List<?>) visible).isEmpty(), "candidate can inspect persisted failed deliveries");
        Map<String, Object> frozen = f.jdbc.queryForMap(
                "SELECT id,business_key,template_code,template_params_json FROM mes_pro_edhr_release_task_notify_delivery WHERE user_id=346");
        Long id = ((Number) frozen.get("id")).longValue();
        f.template.setStatus(0);
        f.retry(346L, id, f.version(id), "模板恢复后重试");
        assertEquals("SENT", f.status(id));
        Map<String, Object> replayed = f.jdbc.queryForMap(
                "SELECT id,business_key,template_code,template_params_json FROM mes_pro_edhr_release_task_notify_delivery WHERE id=?", id);
        assertEquals(frozen, replayed);
        assertEquals(1, f.count("system_notify_message"));
    }

    @Test
    void unauthorisedActorTenantAndMissingPermissionCannotRetryOrQuery() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        assertThrows(RuntimeException.class, () -> f.retry(999L, 10L, 0, "非候选人重试"));
        assertThrows(RuntimeException.class, () -> invoke(f.service, "listForTask", 1L, 999L, 900L));
        assertThrows(RuntimeException.class, () -> invoke(f.dispatch, "retryDelivery", 2L, 346L, 10L, 0, "跨租户重试"));
        when(f.permissions.hasAnyPermissions(eq(346L), any(String[].class))).thenReturn(false);
        assertThrows(RuntimeException.class, () -> f.retry(346L, 10L, 0, "缺少正式权限"));
        assertThrows(RuntimeException.class, () -> invoke(f.service, "listForTask", 1L, 346L, 900L));
        assertEquals(0, f.version(10L));
        assertEquals(0, f.count("system_notify_message"));
    }

    @Test
    void staleVersionAndBlankRetryReasonCannotCreateAttempt() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        assertThrows(RuntimeException.class, () -> f.retry(346L, 10L, 9, "过期版本"));
        assertThrows(RuntimeException.class, () -> f.retry(346L, 10L, 0, " "));
        assertEquals(0, f.version(10L));
        assertEquals(0, f.count("notification_test_audit"));
        assertEquals(0, f.count("system_notify_message"));
    }

    @Test
    void oldFailureCannotOverwriteSentAndSentCannotBeRetried() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        f.retry(346L, 10L, 0, "首次合法发送");
        int sentVersion = f.version(10L);
        assertThrows(RuntimeException.class, () -> invoke(f.transactions, "recordFailed",
                1L, 10L, 1, 346L, "旧尝试", "OLD_ATTEMPT", true));
        assertThrows(RuntimeException.class, () -> f.retry(346L, 10L, sentVersion, "已成功不重复发送"));
        assertEquals("SENT", f.status(10L));
        assertEquals(sentVersion, f.version(10L));
        assertEquals(1, f.count("system_notify_message"));
    }

    @Test
    void auditFailureRollsBackAttemptAndSuccessfulAuditRowsTogether() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        f.failAnyAudit.set(true);
        assertThrows(RuntimeException.class, () -> f.begin(10L, 0, false));
        assertEquals(0, f.version(10L));
        assertEquals(0, f.jdbc.queryForObject(
                "SELECT attempt_count FROM mes_pro_edhr_release_task_notify_delivery WHERE id=10", Integer.class));
        assertEquals(0, f.count("notification_test_audit"));
    }

    @Test
    void samePlatformKeyWithChangedRecipientOrParamsIsRejectedAndTenantKeysAreIsolated() throws Exception {
        Fixture f = new Fixture();
        NotifySendSingleToUserIdempotentReqDTO original = f.request(346L, KEY, Map.of("workTaskId", "900"));
        Long id = (Long) invoke(f.sender, "send", original);
        assertThrows(RuntimeException.class, () -> invoke(f.sender, "send",
                f.request(347L, KEY, Map.of("workTaskId", "900"))));
        assertThrows(RuntimeException.class, () -> invoke(f.sender, "send",
                f.request(346L, KEY, Map.of("workTaskId", "901"))));
        TenantContextHolder.setTenantId(2L);
        Long otherTenantId = (Long) invoke(f.sender, "send", original);
        assertNotEquals(id, otherTenantId);
        assertEquals(2, f.count("system_notify_message"));
        assertEquals(2, f.jdbc.queryForObject(
                "SELECT COUNT(DISTINCT tenant_id) FROM system_notify_message", Integer.class));
    }

    @Test
    void concurrentPlatformSendsUseRealUniqueConstraintAndReturnOneMessageIdentity() throws Exception {
        Fixture f = new Fixture();
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var job = (java.util.concurrent.Callable<Long>) () -> {
                TenantContextHolder.setTenantId(1L);
                try {
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return (Long) invoke(f.sender, "send", f.request(346L, KEY, Map.of("workTaskId", "900")));
                } finally { TenantContextHolder.clear(); }
            };
            var first = executor.submit(job);
            var second = executor.submit(job);
            start.countDown();
            assertEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, f.count("system_notify_message"));
        } finally { executor.shutdownNow(); }
    }

    @Test
    void independentPlatformTransactionCommitsEvenWhenCallerRollsBack() throws Exception {
        Fixture f = new Fixture();
        assertThrows(IllegalStateException.class, () -> f.business.executeWithoutResult(tx -> {
            invoke(f.sender, "send", f.request(346L, KEY, Map.of("workTaskId", "900")));
            throw new IllegalStateException("caller failed after platform commit");
        }));
        assertEquals(1, f.count("system_notify_message"));
    }

    @Test
    void managerAssignmentFreezesPqcActorAndExactExistingMarketReleaseEntry() throws Exception {
        Fixture f = new Fixture();
        f.task.setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(449L).setBatchExecutionId(952L)
                .setProcessName("管理者代表最终放行")
                .setActionUrl("/mes/pro/feedback/edhr-batch-execution?batchExecutionId=952"
                        + "&releaseTransactionId=449&workTaskId=900&action=marketRelease");
        f.template.setCode("MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED");
        when(f.templates.getNotifyTemplateByCodeFromCache("MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED"))
                .thenAnswer(call -> f.template);
        f.business.executeWithoutResult(tx -> invoke(f.service, "scheduleAssigned", f.task, 346L));
        assertEquals(2, f.count("system_notify_message"));
        assertEquals(346L, f.jdbc.queryForObject(
                "SELECT MIN(initiated_by) FROM mes_pro_edhr_release_task_notify_delivery", Long.class));
        assertEquals(341L, f.task.getSourceUserId(), "task creator remains original application actor");
        String params = f.jdbc.queryForObject(
                "SELECT template_params_json FROM mes_pro_edhr_release_task_notify_delivery WHERE user_id=347", String.class);
        assertTrue(params.contains("marketRelease"));
        assertTrue(params.contains("batchExecutionId=952"));
        assertTrue(params.contains("releaseTransactionId=449"));
        assertTrue(params.contains("workTaskId=900"));
    }

    @Test
    void committedPlatformMessageStillRequiresEnabledTemplateOnReplayThenRecoversSameId() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        Long original = (Long) invoke(f.sender, "send", f.request(346L, KEY, Map.of("workTaskId", "900")));
        f.template.setStatus(1);
        assertThrows(RuntimeException.class, () -> f.retry(346L, 10L, 0, "模板停用期间重试"));
        assertEquals("FAILED", f.status(10L));
        assertEquals(1, f.count("system_notify_message"));
        assertNotNull(f.jdbc.queryForObject(
                "SELECT last_error_summary FROM mes_pro_edhr_release_task_notify_delivery WHERE id=10", String.class));
        f.template.setStatus(0);
        f.retry(346L, 10L, f.version(10L), "恢复已启用模板后继续同键投递");
        assertEquals(original, f.jdbc.queryForObject(
                "SELECT system_message_id FROM mes_pro_edhr_release_task_notify_delivery WHERE id=10", Long.class));
        assertEquals(1, f.count("system_notify_message"));
    }

    @Test
    void completedTaskKeepsFailedDeliveryVisibleAndRecipientCanRetrySameFrozenEvent() throws Exception {
        Fixture f = new Fixture();
        f.task.setStatus("DONE");
        f.pending(10L, 1L, 346L, KEY);
        f.jdbc.update("UPDATE mes_pro_edhr_release_task_notify_delivery SET status='FAILED',attempt_count=1,row_version=2 WHERE id=10");
        Map<String, Object> frozen = f.jdbc.queryForMap("""
                SELECT work_task_id,user_id,business_key,template_code,template_params_json,initiated_by
                  FROM mes_pro_edhr_release_task_notify_delivery WHERE id=10
                """);
        assertFalse(((List<?>) invoke(f.service, "listForTask", 1L, 346L, 900L)).isEmpty());
        assertThrows(RuntimeException.class, () -> f.retry(347L, 10L, 2, "其他候选不能替收件人重试"));
        assertThrows(RuntimeException.class, () -> invoke(f.dispatch, "retryDelivery", 2L, 346L, 10L, 2, "其他租户"));
        assertEquals(2, f.version(10L));
        f.retry(346L, 10L, 2, "已完成任务恢复原失败通知");
        assertEquals("DONE", f.task.getStatus(), "notification retry must not reopen or advance business task");
        assertEquals("SENT", f.status(10L));
        assertEquals(frozen, f.jdbc.queryForMap("""
                SELECT work_task_id,user_id,business_key,template_code,template_params_json,initiated_by
                  FROM mes_pro_edhr_release_task_notify_delivery WHERE id=10
                """));
        assertEquals(1, f.count("system_notify_message"));
        assertThrows(RuntimeException.class, () -> f.retry(346L, 10L, f.version(10L), "SENT不可重复投递"));
        assertEquals(1, f.count("system_notify_message"));
        verify(f.taskMapper, never()).updateById(any(MesProEdhrWorkTaskDO.class));
    }

    @Test
    void manualRetryWithAutomaticReasonTextStillCreatesUserSentAudit() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        String reason = "正式放行任务提交后自动投递站内信";
        f.retry(346L, 10L, 0, reason);
        assertEquals("SENT", f.status(10L), "actual transaction ACK must have committed");
        assertRetryAuditOrigin(f, "attempted", reason);
        assertRetryAuditOrigin(f, "sent", reason);
    }

    @Test
    void manualRetryWithAutomaticReasonTextStillCreatesUserFailedAudit() throws Exception {
        Fixture f = new Fixture();
        f.pending(10L, 1L, 346L, KEY);
        f.template.setStatus(1);
        String reason = "正式放行任务提交后自动投递站内信";
        assertThrows(RuntimeException.class, () -> f.retry(346L, 10L, 0, reason));
        assertEquals("FAILED", f.status(10L), "actual failure transaction ACK must have committed");
        assertRetryAuditOrigin(f, "attempted", reason);
        assertRetryAuditOrigin(f, "failed", reason);
    }

    @Test
    void afterCommitAutomaticDispatchKeepsSystemOriginForAttemptAndAck() throws Exception {
        Fixture f = new Fixture();
        f.business.executeWithoutResult(tx -> f.schedule(f.task));
        assertEquals(2, f.count("system_notify_message"));
        List<GxpAuditCommand> attemptsAndAcks = f.auditCommands.stream()
                .filter(command -> command.getOperationId().endsWith(".attempted")
                        || command.getOperationId().endsWith(".sent")).toList();
        assertEquals(4, attemptsAndAcks.size());
        for (GxpAuditCommand command : attemptsAndAcks) {
            assertEquals("SYSTEM", command.getReasonSource());
            assertEquals("RELEASE_NOTIFICATION_DISPATCH", command.getReasonCode());
        }
    }

    private static void assertRetryAuditOrigin(Fixture f, String action, String reason) {
        List<GxpAuditCommand> commands = f.auditCommands.stream()
                .filter(command -> command.getOperationId().equals("mes.release-task-notification." + action))
                .toList();
        assertEquals(1, commands.size(), "real Audit must append exactly one " + action + " command");
        GxpAuditCommand command = commands.get(0);
        assertAll("manual retry origin must survive transaction ACK regardless of user reason text",
                () -> assertEquals(reason, command.getReason()),
                () -> assertEquals("USER", command.getReasonSource()),
                () -> assertEquals("RELEASE_NOTIFICATION_RETRY", command.getReasonCode()));
    }

    private static final class Fixture {
        final JdbcTemplate jdbc;
        final TransactionTemplate business;
        final Object service, dispatch, transactions, sender;
        final MesProEdhrWorkTaskMapper taskMapper = mock(MesProEdhrWorkTaskMapper.class);
        final PermissionApi permissions = mock(PermissionApi.class);
        final NotifyTemplateService templates = mock(NotifyTemplateService.class);
        final GxpAuditService gxp = mock(GxpAuditService.class);
        final AtomicBoolean failSentAudit = new AtomicBoolean();
        final AtomicBoolean failAnyAudit = new AtomicBoolean();
        final List<GxpAuditCommand> auditCommands = new ArrayList<>();
        final MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO().setId(900L)
                .setTaskType("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(224L).setWorkOrderId(990274L).setWorkOrderCode("WO-NOTIFY")
                .setBatchCode("BATCH-NOTIFY").setProcessName("PQC生产放行").setStatus("TODO")
                .setCandidateUserSnapshot("346,347").setAssigneeUserId(346L).setSourceUserId(341L)
                .setOwnershipLocked(true).setActionUrl("/mes/production-release/pqc?applicationId=224&workTaskId=900");
        final NotifyTemplateDO template = new NotifyTemplateDO().setId(9001L).setCode(PQC_TEMPLATE)
                .setStatus(0).setType(1).setNickname("放行通知")
                .setContent("任务 {workTaskId}").setParams(List.of("workTaskId"));

        Fixture() throws Exception {
            // This first assertion is the deliberate RED while the required production component is absent.
            Class<?> deliveryMapperType = required(BASE + "dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper");
            TenantContextHolder.setTenantId(1L);
            var source = new DriverManagerDataSource("jdbc:h2:mem:release_notify_" + UUID.randomUUID()
                    + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
            jdbc = new JdbcTemplate(source);
            createSchema();
            var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(source);
            business = new TransactionTemplate(manager);
            var config = new MybatisConfiguration();
            config.setMapUnderscoreToCamelCase(true);
            var global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
            global.setMetaObjectHandler(new DefaultDBFieldHandler());
            GlobalConfigUtils.setGlobalConfig(config, global);
            config.addMapper(deliveryMapperType);
            config.addMapper(NotifyMessageMapper.class);
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setGlobalConfig(global); factory.setDataSource(source); factory.setConfiguration(config);
            var sessions = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
            Object deliveryMapper = sessions.getMapper(deliveryMapperType);

            when(templates.getNotifyTemplateByCodeFromCache(PQC_TEMPLATE)).thenAnswer(call -> template);
            when(templates.formatNotifyTemplateContent(anyString(), anyMap())).thenReturn("放行任务");
            when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenReturn(true);
            when(taskMapper.selectById(anyLong())).thenAnswer(call ->
                    Objects.equals(call.getArgument(0), task.getId()) ? task : null);
            when(gxp.append(any(GxpAuditCommand.class))).thenAnswer(call -> {
                GxpAuditCommand command = call.getArgument(0);
                auditCommands.add(command);
                jdbc.update("INSERT INTO notification_test_audit(operation_id) VALUES (?)", command.getOperationId());
                if (failAnyAudit.get() || (command.getOperationId().endsWith(".sent")
                        && failSentAudit.getAndSet(false))) {
                    throw new IllegalStateException("injected unified audit failure after actual SQL write");
                }
                return null;
            });
            Object audit = instance(PKG + "MesReleaseTaskNotificationAudit");
            field(audit, "gxpAuditService", gxp);
            Object transactionTarget = instance(PKG + "MesReleaseTaskNotificationTransactionService");
            field(transactionTarget, "deliveryMapper", deliveryMapper); field(transactionTarget, "audit", audit);
            transactions = proxy(transactionTarget, manager);

            NotifyMessageServiceImpl messages = new NotifyMessageServiceImpl();
            field(messages, "notifyMessageMapper", sessions.getMapper(NotifyMessageMapper.class));
            NotifySendServiceImpl sendTarget = new NotifySendServiceImpl();
            field(sendTarget, "notifyTemplateService", templates); field(sendTarget, "notifyMessageService", messages);
            NotifyMessageSendApiImpl api = new NotifyMessageSendApiImpl();
            field(api, "notifySendService", proxy(sendTarget, manager));
            Object senderTarget = instance(PKG + "MesReleaseTaskNotificationPlatformSender");
            field(senderTarget, "notifyMessageSendApi", api);
            sender = proxy(senderTarget, manager);
            dispatch = instance(PKG + "MesReleaseTaskNotificationDispatchService");
            field(dispatch, "deliveryMapper", deliveryMapper); field(dispatch, "workTaskMapper", taskMapper);
            field(dispatch, "transactionService", transactions); field(dispatch, "platformSender", sender);
            field(dispatch, "permissionApi", permissions);
            Object serviceTarget = instance(PKG + "MesReleaseTaskNotificationService");
            field(serviceTarget, "deliveryMapper", deliveryMapper); field(serviceTarget, "dispatchService", dispatch);
            field(serviceTarget, "audit", audit);
            field(serviceTarget, "permissionApi", permissions); field(serviceTarget, "workTaskMapper", taskMapper);
            service = proxy(serviceTarget, manager);
        }

        void createSchema() {
            jdbc.execute("""
                    CREATE TABLE mes_pro_edhr_release_task_notify_delivery (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL,
                      work_task_id BIGINT NOT NULL, event_type VARCHAR(32) NOT NULL, user_id BIGINT NOT NULL,
                      business_key VARCHAR(255) NOT NULL, template_code VARCHAR(128) NOT NULL,
                      template_params_json VARCHAR(12000) NOT NULL, initiated_by BIGINT NOT NULL,
                      status VARCHAR(16) NOT NULL, attempt_count INT DEFAULT 0 NOT NULL,
                      row_version INT DEFAULT 0 NOT NULL, last_attempt_at TIMESTAMP, sent_at TIMESTAMP,
                      system_message_id BIGINT, last_error_summary VARCHAR(512),
                      creator VARCHAR(64), updater VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted BOOLEAN DEFAULT FALSE,
                      UNIQUE(tenant_id,work_task_id,event_type,user_id), UNIQUE(tenant_id,business_key))
                    """);
            jdbc.execute("""
                    CREATE TABLE system_notify_message (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
                      user_type INT NOT NULL, business_key VARCHAR(255), template_id BIGINT, template_code VARCHAR(128),
                      template_type INT, template_nickname VARCHAR(255), template_content VARCHAR(12000),
                      template_params VARCHAR(12000), read_status BOOLEAN DEFAULT FALSE, read_time TIMESTAMP,
                      creator VARCHAR(64), updater VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted BOOLEAN DEFAULT FALSE,
                      UNIQUE(tenant_id,business_key))
                    """);
            jdbc.execute("CREATE TABLE notification_test_audit(id BIGINT AUTO_INCREMENT PRIMARY KEY,operation_id VARCHAR(128))");
        }

        void schedule(MesProEdhrWorkTaskDO assigned) { invoke(service, "scheduleAssigned", assigned, 341L); }
        void begin(Long id, Integer version, boolean retry) {
            invoke(transactions, "recordAttempt", 1L, id, version, 346L, "合法发送尝试", retry);
        }
        void retry(Long actor, Long id, Integer version, String reason) {
            invoke(dispatch, "retryDelivery", 1L, actor, id, version, reason);
        }
        int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
        int version(Long id) { return jdbc.queryForObject("SELECT row_version FROM mes_pro_edhr_release_task_notify_delivery WHERE id=?", Integer.class, id); }
        String status(Long id) { return jdbc.queryForObject("SELECT status FROM mes_pro_edhr_release_task_notify_delivery WHERE id=?", String.class, id); }
        void pending(Long id, Long tenant, Long user, String key) {
            jdbc.update("""
                    INSERT INTO mes_pro_edhr_release_task_notify_delivery
                      (id,tenant_id,work_task_id,event_type,user_id,business_key,template_code,template_params_json,
                       initiated_by,status,attempt_count,row_version)
                    VALUES (?, ?, 900, 'ASSIGNED', ?, ?, ?, '{"workTaskId":"900"}',341,'PENDING',0,0)
                    """, id, tenant, user, key, PQC_TEMPLATE);
        }
        NotifySendSingleToUserIdempotentReqDTO request(Long user, String key, Map<String, Object> params) {
            NotifySendSingleToUserIdempotentReqDTO request = new NotifySendSingleToUserIdempotentReqDTO();
            request.setUserId(user); request.setTemplateCode(PQC_TEMPLATE);
            request.setTemplateParams(params); request.setBusinessKey(key);
            return request;
        }
    }

    private static Class<?> required(String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException missing) {
            fail("required reliable notification component is absent: " + name);
            throw new AssertionError(missing);
        }
    }
    private static Object instance(String name) throws Exception { return required(name).getConstructor().newInstance(); }
    private static void field(Object target, String name, Object value) { ReflectionTestUtils.setField(target, name, value); }
    private static Object proxy(Object target, org.springframework.transaction.PlatformTransactionManager manager) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return factory.getProxy();
    }
    private static Object invoke(Object target, String method, Object... arguments) {
        Method found = java.util.Arrays.stream(target.getClass().getMethods())
                .filter(candidate -> candidate.getName().equals(method)
                        && candidate.getParameterCount() == arguments.length).findFirst()
                .orElseThrow(() -> new AssertionError("required notification operation missing: " + method));
        try { return found.invoke(target, arguments); }
        catch (InvocationTargetException invocation) {
            Throwable cause = invocation.getCause();
            if (cause instanceof RuntimeException runtime) { throw runtime; }
            if (cause instanceof Error error) { throw error; }
            throw new AssertionError("notification operation failed", cause);
        } catch (ReflectiveOperationException error) { throw new AssertionError("test contract invocation failed", error); }
    }
}
