package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.springframework.dao.DuplicateKeyException;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

import static org.junit.jupiter.api.Assertions.*;

/** Dedicated MySQL fixture only; never uses application datasource configuration. */
class GxpAuditWriterMysqlSnapshotTest {

    private static final String CONTAINER = "gxp-integration-mysql-round2-dalton-jdbc";
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static GxpAuditService writer;
    private static final AtomicLong TENANTS = new AtomicLong(
            1_000_000_000_000L + (UUID.randomUUID().getMostSignificantBits() & 0x1ffffffffffffL));
    private static final ThreadLocal<Integer> WRITES = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Long> OTHER_CONNECTION = new ThreadLocal<>();

    @Test
    void oldSnapshotAppendsAfterTheCommittedTail() {
        long tenant = fixture();
        GxpAuditAppendResult first = tx(tenant, () -> writer.append(command("first")));
        GxpAuditAppendResult next = tx(tenant, () -> {
            long connection = connection();
            assertEquals(1L, count("gxp_audit_event", tenant));
            GxpAuditAppendResult winner = other(() -> tx(tenant, () -> {
                assertNotEquals(connection, connection());
                return writer.append(command("winner"));
            }));
            assertEquals(1L, count("gxp_audit_event", tenant), "B must retain its old RR snapshot");
            GxpAuditAppendResult appended = writer.append(command("next"));
            GxpAuditEventDO event = context.getBean(GxpAuditEventMapper.class).selectById(appended.eventId());
            assertEquals(winner.eventHash(), event.getPreviousEventHash());
            assertEquals(3L, appended.ledgerSequence());
            assertEquals(DigestUtil.sha256Hex(event.getCanonicalEventJson()), event.getEventHash());
            return appended;
        });
        assertNotEquals(first.eventId(), next.eventId());
        assertState(tenant, 4L, 3L, 3L);
    }

    @Test
    void oldSnapshotReplaysCommittedKeyWithoutAnyMapperWrites() {
        long tenant = fixture();
        tx(tenant, () -> {
            assertEquals(0L, count("gxp_audit_event", tenant));
            GxpAuditAppendResult winner = other(() -> tx(tenant, () -> writer.append(command("same"))));
            assertEquals(0L, count("gxp_audit_event", tenant));
            WRITES.set(0);
            GxpAuditCommand input = command("same");
            GxpAuditAppendResult replay = writer.append(input);
            assertTrue(replay.replayed());
            assertEquals(winner.eventId(), replay.eventId());
            assertEquals(winner.eventHash(), replay.eventHash());
            assertEquals(winner.ledgerSequence(), replay.ledgerSequence());
            assertEquals(0, WRITES.get());
            assertNull(input.getTransactionId(), "writer must not mutate the caller command");
            return null;
        });
        assertState(tenant, 2L, 1L, 1L);
    }

    @Test
    void oldSnapshotRejectsChangedPayloadWithFormalConflictAndZeroWrites() {
        long tenant = fixture();
        ServiceException failure = assertThrows(ServiceException.class, () -> tx(tenant, () -> {
            assertEquals(0L, count("gxp_audit_event", tenant));
            other(() -> tx(tenant, () -> writer.append(command("same"))));
            assertEquals(0L, count("gxp_audit_event", tenant));
            GxpAuditCommand changed = command("same");
            changed.setReason("different payload");
            WRITES.set(0);
            try {
                return writer.append(changed);
            } finally {
                assertEquals(0, WRITES.get());
            }
        }));
        assertEquals(GXP_AUDIT_IDEMPOTENCY_CONFLICT.getCode(), failure.getCode());
        assertState(tenant, 2L, 1L, 1L);
        assertEquals("test reason", jdbc.queryForObject(
                "SELECT reason FROM gxp_audit_event WHERE tenant_id = ?", String.class, tenant));
        assertStoredHash(tenant);
    }

    @Test
    void oldPolicySnapshotUsesNewCommittedVersionAndNeverFallsBack() {
        for (boolean hasOperation : List.of(true, false)) {
            long tenant = fixture();
            Supplier<GxpAuditAppendResult> attempt = () -> tx(tenant, () -> {
                assertEquals("v1", policyVersion(tenant));
                other(() -> tx(tenant, () -> {
                    writer.acquireLedgerLock();
                    policy(tenant, "v2", hasOperation, true, "GXP");
                    return null;
                }));
                assertEquals("v1", policyVersion(tenant), "ordinary read must still see v1");
                WRITES.set(0);
                try {
                    return writer.append(command("policy"));
                } catch (ServiceException ex) {
                    assertEquals(0, WRITES.get());
                    throw ex;
                }
            });
            if (hasOperation) {
                attempt.get();
                assertEquals("v2", jdbc.queryForObject(
                        "SELECT policy_version FROM gxp_audit_event WHERE tenant_id = ?", String.class, tenant));
                assertState(tenant, 2L, 1L, 1L);
            } else {
                assertEquals(GXP_AUDIT_POLICY_NOT_FOUND.getCode(),
                        assertThrows(ServiceException.class, attempt::get).getCode());
                assertState(tenant, 1L, 0L, 0L);
            }
        }
    }

    @Test
    void sameKeyInAnotherTenantCannotReplayOrJoinItsChain() {
        long a = fixture();
        long b = fixture();
        GxpAuditAppendResult first = tx(a, () -> writer.append(command("shared")));
        GxpAuditAppendResult second = tx(b, () -> writer.append(command("shared")));
        assertFalse(second.replayed());
        assertNotEquals(first.eventId(), second.eventId());
        assertEquals(1L, second.ledgerSequence());
        assertNull(jdbc.queryForObject("SELECT previous_event_hash FROM gxp_audit_event WHERE tenant_id = ?",
                String.class, b));
        assertState(a, 2L, 1L, 1L);
        assertState(b, 2L, 1L, 1L);
        assertStoredHash(a);
        assertStoredHash(b);
    }

    @Test
    void absentPolicyInactiveNonGxpAndMissingOrInvalidWatermarkFailWithoutWrites() {
        for (String kind : List.of("no-policy", "inactive", "non-gxp", "no-ledger", "invalid-ledger")) {
            long tenant = TENANTS.incrementAndGet();
            tx(tenant, () -> {
                if (!kind.equals("no-ledger")) {
                    ledger(tenant, kind.equals("invalid-ledger") ? 0L : 1L);
                }
                if (!kind.equals("no-policy")) {
                    policy(tenant, "v1", true, !kind.equals("inactive"),
                            kind.equals("non-gxp") ? "NOT_APPLICABLE" : "GXP");
                }
                return null;
            });
            ServiceException error = assertThrows(ServiceException.class, () -> tx(tenant, () -> {
                WRITES.set(0);
                try {
                    return writer.append(command("rejected"));
                } finally {
                    assertEquals(0, WRITES.get());
                }
            }));
            boolean badLedger = kind.endsWith("ledger");
            assertEquals((badLedger ? GXP_AUDIT_APPEND_FAILED : GXP_AUDIT_POLICY_NOT_FOUND).getCode(),
                    error.getCode(), kind);
            assertEquals(0L, count("gxp_audit_event", tenant));
            assertEquals(0L, count("gxp_audit_event_relation", tenant));
            if (kind.equals("no-ledger")) {
                assertEquals(0L, count("gxp_audit_ledger_sequence", tenant));
            } else {
                assertEquals(kind.equals("invalid-ledger") ? 0L : 1L, watermark(tenant));
            }
        }
    }

    @Test
    void writerWaitsOnTheExactLedgerHolderThenContinuesAfterCommit() throws Exception {
        long tenant = fixture();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch requested = new CountDownLatch(1);
        AtomicLong holderId = new AtomicLong();
        AtomicLong waiterId = new AtomicLong();
        try {
            Future<?> holder = pool.submit(() -> tx(tenant, () -> {
                holderId.set(connection());
                writer.acquireLedgerLock();
                held.countDown();
                await(release);
                return null;
            }));
            assertTrue(held.await(10, TimeUnit.SECONDS), "holder did not acquire ledger lock");
            Future<GxpAuditAppendResult> waiter = pool.submit(() -> tx(tenant, () -> {
                waiterId.set(connection());
                requested.countDown();
                return writer.append(command("waiter"));
            }));
            assertTrue(requested.await(10, TimeUnit.SECONDS));
            assertNotEquals(holderId.get(), waiterId.get());
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            boolean observed = false;
            while (System.nanoTime() < deadline && !waiter.isDone()) {
                Long waits = jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w "
                        + "JOIN performance_schema.threads r ON r.THREAD_ID = w.REQUESTING_THREAD_ID "
                        + "JOIN performance_schema.threads b ON b.THREAD_ID = w.BLOCKING_THREAD_ID "
                        + "JOIN performance_schema.data_locks l ON l.ENGINE_LOCK_ID = w.REQUESTING_ENGINE_LOCK_ID "
                        + "AND l.ENGINE = w.ENGINE "
                        + "WHERE r.PROCESSLIST_ID = ? AND b.PROCESSLIST_ID = ? "
                        + "AND l.OBJECT_SCHEMA = 'gxp_writer_snapshot' "
                        + "AND l.OBJECT_NAME = 'gxp_audit_ledger_sequence'", Long.class,
                        waiterId.get(), holderId.get());
                if (waits != null && waits > 0) {
                    observed = true;
                    break;
                }
                Thread.sleep(20); // Poll pacing only; success requires the exact database lock relation.
            }
            assertTrue(observed, "exact writer/holder ledger wait was not observed");
            assertFalse(waiter.isDone());
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
            assertEquals(1L, waiter.get(10, TimeUnit.SECONDS).ledgerSequence());
            assertState(tenant, 2L, 1L, 1L);
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        }
    }

    @Test
    void realRelationUniqueViolationRollsBackOuterSentinelEventAndWatermark() {
        long tenant = fixture();
        DuplicateKeyException failure = assertThrows(DuplicateKeyException.class, () -> tx(tenant, () -> {
            assertEquals(1, jdbc.update("UPDATE gxp_audit_ledger_sequence SET next_ledger_sequence = 99 "
                    + "WHERE tenant_id = ?", tenant));
            GxpAuditCommand input = command("rollback");
            input.setLinks(List.of(input.getLinks().get(0), input.getLinks().get(0)));
            WRITES.set(0);
            try {
                return writer.append(input);
            } finally {
                assertEquals(4, WRITES.get(), "watermark, event, first relation, duplicate relation");
            }
        }));
        assertNotNull(failure.getMostSpecificCause().getMessage());
        assertTrue(failure.getMostSpecificCause().getMessage().contains("uk_gxp_relation"));
        // The callback failed, so Spring completed rollback. These are new autocommit connections.
        assertState(tenant, 1L, 0L, 0L);
    }

    @Test
    void mandatoryRejectsWithZeroWritesButAnOuterTransactionCommits() {
        long tenant = fixture();
        WRITES.set(0);
        try {
            assertThrows(IllegalTransactionStateException.class, () -> writer.append(command("mandatory")));
            assertThrows(IllegalTransactionStateException.class, () -> writer.acquireLedgerLock());
            assertEquals(0, WRITES.get());
            assertState(tenant, 1L, 0L, 0L);
        } finally {
            WRITES.remove();
        }
        assertFalse(tx(tenant, () -> writer.append(command("mandatory"))).replayed());
        assertState(tenant, 2L, 1L, 1L);
    }

    private static long fixture() {
        long tenant = TENANTS.incrementAndGet();
        tx(tenant, () -> {
            ledger(tenant, 1L);
            policy(tenant, "v1", true, true, "GXP");
            return null;
        });
        return tenant;
    }

    private static void ledger(long tenant, long next) {
        GxpAuditLedgerSequenceDO row = new GxpAuditLedgerSequenceDO();
        row.setTenantId(tenant);
        row.setNextLedgerSequence(next);
        assertEquals(1, context.getBean(GxpAuditLedgerSequenceMapper.class).insert(row));
    }

    private static void policy(long tenant, String version, boolean operation, boolean active, String applicability) {
        // Test-only committed facts in the dedicated database; never a production activation call.
        GxpAuditPolicyActivationDO activation = new GxpAuditPolicyActivationDO();
        activation.setTenantId(tenant);
        activation.setPolicyVersion(version);
        activation.setPolicyHash(DigestUtil.sha256Hex(version));
        activation.setRequestId(UUID.randomUUID().toString());
        activation.setActorId(21L);
        activation.setApprovalReference("TEST-ONLY");
        activation.setActivatedAtUtc(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        activation.setEffectiveAfterSequence(0L);
        activation.setCanonicalActivationJson("{\"fixture\":true}");
        activation.setActivationHash(DigestUtil.sha256Hex(tenant + ":" + version));
        assertEquals(1, context.getBean(GxpAuditPolicyActivationMapper.class).insert(activation));
        if (!operation) {
            return;
        }
        GxpAuditPolicyOperationDO row = new GxpAuditPolicyOperationDO();
        row.setTenantId(tenant);
        row.setPolicyVersion(version);
        row.setOperationId("test.update");
        row.setSourceType("SERVICE_METHOD");
        row.setSourceLocator("test#update");
        row.setDomain("TEST");
        row.setSubjectType("TEST_OBJECT");
        row.setActionType("UPDATE");
        row.setReasonPolicy("REQUIRED_CATEGORY_AND_TEXT");
        row.setSignaturePolicy("NOT_REQUIRED");
        row.setStatePolicy("PRESENT_TO_PRESENT");
        row.setRetentionClass("GXP_MASTER_DATA");
        row.setTestIds("writer-mysql");
        row.setOwner("test-owner");
        row.setApplicability(applicability);
        row.setActive(active);
        row.setDeleted(false);
        assertEquals(1, context.getBean(GxpAuditPolicyOperationMapper.class).insert(row));
        Map<String, Object> persisted = jdbc.queryForMap(
                "SELECT create_time, update_time, creator, updater FROM gxp_audit_policy_operation "
                        + "WHERE tenant_id = ? AND policy_version = ? AND operation_id = ?",
                tenant, version, "test.update");
        assertNotNull(persisted.get("create_time"));
        assertNotNull(persisted.get("update_time"));
        assertEquals("21", persisted.get("creator"));
        assertEquals("21", persisted.get("updater"));
    }

    private static GxpAuditCommand command(String key) {
        return GxpAuditCommand.builder().operationId("test.update").subjectId("object-1")
                .subjectVersion("2").reason("test reason").idempotencyKey(key).requestId("request-1")
                .source("test#update")
                .beforeState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion("1")
                        .canonicalJson("{\"value\":1}").build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion("2")
                        .canonicalJson("{\"value\":2}").build())
                .links(List.of(new GxpAuditRelation("SOURCE", "TEST_OBJECT", "object-1", "2",
                        DigestUtil.sha256Hex("object-1")))).build();
    }

    private static <T> T tx(long tenant, Supplier<T> work) {
        LoginUser actor = new LoginUser();
        actor.setId(21L);
        actor.setTenantId(tenant);
        actor.setUserType(2);
        actor.setInfo(Map.of("username", "test-actor", LoginUser.INFO_KEY_NICKNAME, "test-actor"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, List.of()));
        TenantContextHolder.setTenantId(tenant);
        try {
            TransactionTemplate transaction = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
            transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            transaction.setTimeout(20);
            return transaction.execute(status -> {
                assertEquals("REPEATABLE-READ", jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
                if (OTHER_CONNECTION.get() != null) {
                    assertNotEquals(OTHER_CONNECTION.get().longValue(), connection(),
                            "concurrent commit must use another transaction-bound connection");
                }
                return work.get();
            });
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
            WRITES.remove();
        }
    }

    private static <T> T other(Supplier<T> work) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            long caller = connection();
            return executor.submit(() -> {
                OTHER_CONNECTION.set(caller);
                try {
                    return work.get();
                } finally {
                    OTHER_CONNECTION.remove();
                }
            }).get(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted concurrent fixture", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new AssertionError("concurrent fixture did not commit successfully", ex);
        } finally {
            executor.shutdownNow();
            try {
                assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(15, TimeUnit.SECONDS), "fixture latch timed out");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private static long connection() {
        return jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class);
    }

    private static long count(String table, long tenant) {
        assertTrue(List.of("gxp_audit_event", "gxp_audit_event_relation", "gxp_audit_ledger_sequence").contains(table));
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id = ?", Long.class, tenant);
    }

    private static long watermark(long tenant) {
        return jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id = ?",
                Long.class, tenant);
    }

    private static String policyVersion(long tenant) {
        return jdbc.queryForObject("SELECT policy_version FROM gxp_audit_policy_activation "
                + "WHERE tenant_id = ? ORDER BY id DESC LIMIT 1", String.class, tenant);
    }

    private static void assertState(long tenant, long watermark, long events, long relations) {
        assertEquals(watermark, watermark(tenant));
        assertEquals(events, count("gxp_audit_event", tenant));
        assertEquals(relations, count("gxp_audit_event_relation", tenant));
    }

    private static void assertStoredHash(long tenant) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT canonical_event_json, event_hash FROM gxp_audit_event WHERE tenant_id = ?", tenant);
        assertEquals(DigestUtil.sha256Hex((String) row.get("canonical_event_json")), row.get("event_hash"));
    }

    @Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
    public static class WriteObserver implements Interceptor {
        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            WRITES.set(WRITES.get() + 1);
            return invocation.proceed();
        }
    }

    @BeforeAll
    static void connectDedicatedFixture() throws Exception {
        assertEquals("gxp-integration-dalton-m9-round2",
                docker("inspect", "--format", "{{.Config.Labels.owner}}", CONTAINER));
        assertEquals("127.0.0.1:59241", docker("port", CONTAINER, "3306/tcp"));
        String password = docker("exec", CONTAINER, "cat", "/run/m9/root-password");
        DriverManagerDataSource datasource = new DriverManagerDataSource(
                "jdbc:mysql://127.0.0.1:59241/gxp_writer_snapshot"
                        + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000",
                "root", password);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> datasource);
        context.register(WriterConfiguration.class);
        context.refresh();
        jdbc = new JdbcTemplate(datasource);
        assertEquals("gxp_writer_snapshot", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertTrue(jdbc.queryForObject("SELECT VERSION()", String.class).startsWith("8."),
                "This fixture requires the authorized MySQL 8 container");
        for (String table : List.of("gxp_audit_ledger_sequence", "gxp_audit_event",
                "gxp_audit_event_relation", "gxp_audit_policy_activation", "gxp_audit_policy_operation")) {
            assertEquals("InnoDB", jdbc.queryForObject("SELECT ENGINE FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?", String.class, table));
        }
        assertEquals(12L, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TRIGGERS "
                + "WHERE TRIGGER_SCHEMA = DATABASE()", Long.class));
        assertEquals("tenant_id,event_id,relation_type,target_type,target_id", jdbc.queryForObject(
                "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') "
                        + "FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = 'gxp_audit_event_relation' AND INDEX_NAME = 'uk_gxp_relation' "
                        + "AND NON_UNIQUE = 0", String.class));
        writer = context.getBean(GxpAuditService.class);
        assertTrue(AopUtils.isAopProxy(writer), "MANDATORY must be applied by Spring");
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void mandatoryRejectsAppendAndLockBeforeAnyMapperExecution() {
        // Given the real Spring writer and no outer transaction, both entry points must reject.
        assertThrows(IllegalTransactionStateException.class,
                () -> writer.append(GxpAuditCommand.builder().build()));
        assertThrows(IllegalTransactionStateException.class, () -> writer.acquireLedgerLock());
    }

    @Test
    void realMappersParticipateInTheSameRepeatableReadSpringConnection() {
        TransactionTemplate transaction = new TransactionTemplate(
                context.getBean(DataSourceTransactionManager.class));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.executeWithoutResult(status -> {
            assertEquals("REPEATABLE-READ",
                    jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
            Long jdbcConnection = jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class);
            SqlSessionTemplate session = context.getBean(SqlSessionTemplate.class);
            try (var statement = session.getConnection().prepareStatement("SELECT CONNECTION_ID()");
                 var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(jdbcConnection.longValue(), result.getLong(1));
            } catch (java.sql.SQLException ex) {
                throw new IllegalStateException("Spring/MyBatis connection identity query failed", ex);
            }
            for (Class<?> mapper : new Class<?>[]{GxpAuditEventMapper.class,
                    GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                    GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class}) {
                assertNotNull(context.getBean(mapper));
                assertTrue(session.getConfiguration().hasStatement(mapper.getName() + ".insert"),
                        "Real MyBatis-Plus BaseMapper statements must be injected");
            }
        });
    }

    private static String docker(String... arguments) throws Exception {
        var command = new java.util.ArrayList<String>();
        command.add("docker");
        command.addAll(java.util.List.of(arguments));
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Dedicated fixture prerequisite command timed out");
        }
        // Never include output in assertion messages: one command returns a credential.
        assertEquals(0, process.exitValue(), "Dedicated fixture prerequisite command failed");
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class WriterConfiguration {
        @Bean
        DataSourceTransactionManager transactionManager(DataSource datasource) {
            return new DataSourceTransactionManager(datasource);
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource datasource) throws Exception {
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.addInterceptor(new WriteObserver());
            GlobalConfig global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
            global.setMetaObjectHandler(
                    new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler());
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(datasource);
            factory.setConfiguration(configuration);
            factory.setGlobalConfig(global);
            factory.setTransactionFactory(new SpringManagedTransactionFactory());
            SqlSessionFactory result = factory.getObject();
            assertNotNull(result);
            for (Class<?> mapper : new Class<?>[]{GxpAuditEventMapper.class,
                    GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                    GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class}) {
                result.getConfiguration().addMapper(mapper);
            }
            return result;
        }

        @Bean
        SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory factory) {
            return new SqlSessionTemplate(factory);
        }

        @Bean
        GxpAuditEventMapper auditEventMapper(SqlSessionTemplate session) {
            return session.getMapper(GxpAuditEventMapper.class);
        }

        @Bean
        GxpAuditEventRelationMapper eventRelationMapper(SqlSessionTemplate session) {
            return session.getMapper(GxpAuditEventRelationMapper.class);
        }

        @Bean
        GxpAuditLedgerSequenceMapper ledgerSequenceMapper(SqlSessionTemplate session) {
            return session.getMapper(GxpAuditLedgerSequenceMapper.class);
        }

        @Bean
        GxpAuditPolicyActivationMapper policyActivationMapper(SqlSessionTemplate session) {
            return session.getMapper(GxpAuditPolicyActivationMapper.class);
        }

        @Bean
        GxpAuditPolicyOperationMapper policyOperationMapper(SqlSessionTemplate session) {
            return session.getMapper(GxpAuditPolicyOperationMapper.class);
        }

        @Bean
        GxpAuditService writer() {
            return new GxpAuditServiceImpl();
        }
    }
}
