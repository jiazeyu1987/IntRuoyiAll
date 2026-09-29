package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/** Opt-in real MySQL fixture. No application context, business DB or service is started. */
@EnabledIfSystemProperty(named = "m9.mysql", matches = "true")
@ExtendWith(MockitoExtension.class)
class MesM9MysqlEvidenceProtectionTest {
    @InjectMocks
    MesTeamLeaderActiveOrderServiceImpl service;
    static DriverManagerDataSource dataSource;
    static SqlSessionFactory factory;
    static final long TENANT = 92820L;

    @Test
    void lockingReadSeesCommittedEvidenceAfterRepeatableReadSnapshot() throws Exception {
        try (SqlSession reader = factory.openSession(false); Connection writer = dataSource.getConnection()) {
            Connection c = reader.getConnection();
            try {
                c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM mes_pro_process_pool_order_process_completion WHERE tenant_id=92820")) {
                    assertTrue(rs.next()); assertEquals(0, rs.getInt(1));
                }
                insertEvidence(writer, "completion");
                assertEquals(List.of(403L), reader.getMapper(MesTeamLeaderDataCleanupMapper.class)
                        .selectHistoricalCompletionIdsForUpdate(TENANT, List.of(201L)));
            } finally {
                c.rollback();
                try (Statement st = writer.createStatement()) {
                    st.executeUpdate("DELETE FROM mes_pro_process_pool_order_process_completion WHERE tenant_id=92820 AND id=403");
                }
            }
        }
    }

    @Test
    void actualPhysicalDeleteRollsBackWhenLaterUniqueConstraintFails() throws Exception {
        try (Connection seed = dataSource.getConnection()) { insertEvidence(seed, "completion"); }
        try (SqlSession session = factory.openSession(false)) {
            Connection c = session.getConnection();
            try {
                assertEquals(1, session.getMapper(MesTeamLeaderDataCleanupMapper.class)
                        .deleteOrderProcessCompletions(TENANT, List.of(201L)));
                insertEvidence(c, "completion");
                assertThrows(SQLIntegrityConstraintViolationException.class, () -> insertEvidence(c, "completion"));
            } finally { c.rollback(); }
        }
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT aggregate_hash,deleted FROM mes_pro_process_pool_order_process_completion WHERE tenant_id=92820 AND id=403")) {
                assertTrue(rs.next()); assertEquals("a".repeat(64), rs.getString(1)); assertTrue(rs.getBoolean(2));
            } finally {
                st.executeUpdate("DELETE FROM mes_pro_process_pool_order_process_completion WHERE tenant_id=92820 AND id=403");
            }
        }
    }

    @Test
    void emptyRangeLockBlocksConcurrentEvidenceInsertUntilRollback() throws Exception {
        var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        try (SqlSession reader = factory.openSession(false)) {
            Connection c = reader.getConnection();
            assertEquals(List.of(), reader.getMapper(MesTeamLeaderDataCleanupMapper.class)
                    .selectHistoricalCompletionIdsForUpdate(TENANT, List.of(201L)));
            var started = new java.util.concurrent.CountDownLatch(1);
            var writer = pool.submit(() -> {
                try (Connection w = dataSource.getConnection(); Statement st = w.createStatement()) {
                    st.execute("SET SESSION innodb_lock_wait_timeout=10");
                    started.countDown();
                    insertEvidence(w, "completion");
                }
                return true;
            });
            try {
                assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                boolean waiting = false;
                try (Connection observer = dataSource.getConnection(); Statement st = observer.createStatement()) {
                    while (System.nanoTime() < deadline && !waiting) {
                        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM performance_schema.data_lock_waits")) {
                            assertTrue(rs.next()); waiting = rs.getInt(1) > 0;
                        }
                        Thread.yield();
                    }
                }
                assertTrue(waiting, "Must observe real InnoDB lock wait, not infer concurrency from elapsed time");
                assertFalse(writer.isDone());
                c.rollback();
                assertTrue(writer.get(10, java.util.concurrent.TimeUnit.SECONDS));
            } finally {
                c.rollback();
                pool.shutdown();
                assertTrue(pool.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS));
                try (Connection clean = dataSource.getConnection(); Statement st = clean.createStatement()) {
                    st.executeUpdate("DELETE FROM mes_pro_process_pool_order_process_completion WHERE tenant_id=92820 AND id=403");
                }
            }
        } finally { pool.shutdownNow(); }
    }

    @BeforeEach
    void dependencies() throws Exception {
        var ctor = MesTeamLeaderActiveOrderServiceImpl.class.getConstructors()[0];
        Object[] args = java.util.Arrays.stream(ctor.getParameterTypes()).map(type -> mock(type)).toArray();
        service = (MesTeamLeaderActiveOrderServiceImpl) ctor.newInstance(args);
    }

    @BeforeAll
    static void connectOwnedContainer() throws Exception {
        String name = "gxp-integration-mysql-round2-dalton-jdbc";
        assertEquals("gxp-integration-dalton-m9-round2", docker("inspect", "--format",
                "{{.Config.Labels.owner}}", name).trim());
        String port = docker("port", name, "3306/tcp").trim();
        assertTrue(port.matches("127\\.0\\.0\\.1:[0-9]+"));
        String password = docker("exec", name, "cat", "/run/m9/root-password").trim();
        dataSource = new DriverManagerDataSource("jdbc:mysql://" + port
                + "/gxp_m9_round2?useSSL=false&allowPublicKeyRetrieval=true", "root", password);
        Configuration config = new Configuration(new Environment("m9", new JdbcTransactionFactory(), dataSource));
        config.addMapper(MesTeamLeaderDataCleanupMapper.class);
        factory = new SqlSessionFactoryBuilder().build(config);
    }

    static String docker(String... args) throws Exception {
        var command = new java.util.ArrayList<String>();
        command.add("docker"); command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), "Task container command failed");
        return output;
    }

    @AfterEach
    void clearContext() { TenantContextHolder.clear(); }

    @ParameterizedTest
    @ValueSource(strings = {"piece", "aggregate", "completion"})
    void sharedRebuildAndRecoveryPreviewDetectsOrphanHistory(String kind) throws Exception {
        TenantContextHolder.setTenantId(TENANT);
        try (SqlSession session = factory.openSession(false)) {
          try {
            insertEvidence(session.getConnection(), kind);
            ReflectionTestUtils.setField(service, "dataCleanupMapper",
                    session.getMapper(MesTeamLeaderDataCleanupMapper.class));
            MesTeamLeaderActiveOrderRebuildPreview preview = ReflectionTestUtils.invokeMethod(service,
                    "buildRebuildPreview", MesProcessPoolActiveOrderDO.builder().id(101L).workOrderId(201L).build());
            assertTrue(preview.isHasHistoricalRuntimeData(), "Shared rebuild/recovery gate must detect orphan history");
          } finally {
            session.getConnection().rollback();
          }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"piece", "aggregate", "completion"})
    void ordinaryCleanupRejectsOrphanHistoricalEvidenceBeforeFirstWrite(String kind) throws Exception {
        TenantContextHolder.setTenantId(TENANT);
        try (SqlSession session = factory.openSession(false)) {
            Connection connection = session.getConnection();
          try {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            String table = insertEvidence(connection, kind);
            MesTeamLeaderDataCleanupMapper real = session.getMapper(MesTeamLeaderDataCleanupMapper.class);
            // Out-of-scope dependencies are isolated; scoped evidence reads use actual Mapper SQL.
            MesTeamLeaderDataCleanupMapper cleanup = mock(MesTeamLeaderDataCleanupMapper.class, call -> {
                String method = call.getMethod().getName();
                if (method.equals("selectPqcTaskIds") || method.startsWith("selectHistorical")) {
                    return call.getMethod().invoke(real, call.getArguments());
                }
                if (method.startsWith("delete") || method.startsWith("softRemove")) {
                    fail("First destructive statement reached before orphan evidence protection: " + method);
                }
                return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
            });
            service = spy(service);
            var scope = MesTeamLeaderDataCleanupPreview.builder().leaderUserId(3001L)
                    .orderIds(List.of(101L)).orderVersions(List.of(1)).workOrderIds(List.of(201L))
                    .batchExecutionIds(List.of()).activeOrderCount(1).reportEventCount(0)
                    .batchExecutionCount(0).releaseApplicationCount(0).releaseTransactionCount(0).build();
            doReturn(scope).when(service).previewDataCleanup(3001L);
            var orders = mock(MesProcessPoolActiveOrderMapper.class);
            when(orders.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                    .thenReturn(List.of(MesProcessPoolActiveOrderDO.builder().id(101L)
                            .workOrderId(201L).version(1).build()));
            ReflectionTestUtils.setField(service, "activeOrderMapper", orders);
            ReflectionTestUtils.setField(service, "dataCleanupMapper", cleanup);
            ReflectionTestUtils.setField(service, "releaseApplicationMapper", mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class));
            ReflectionTestUtils.setField(service, "gxpAuditService", mock(GxpAuditService.class));
            ServiceException error = assertThrows(ServiceException.class,
                    () -> service.executeDataCleanup(3001L, scope));
            assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_DATA_CLEANUP_BLOCKED.getCode(), error.getCode());
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(
                    "SELECT COUNT(*) FROM " + table + " WHERE tenant_id=" + TENANT)) {
                assertTrue(rows.next()); assertEquals(1, rows.getInt(1));
            }
          } finally {
            connection.rollback();
          }
        }
    }

    static String insertEvidence(Connection c, String kind) throws SQLException {
        try (Statement st = c.createStatement()) {
            if (kind.equals("piece")) {
                st.executeUpdate("INSERT INTO mes_pqc_inspection_task (id,active_order_id,work_order_id,route_id,route_version_id,route_process_id,process_id,regulation_version_id,inspection_type,inspection_rule_key,business_date,shift_code,round_no,planned_inspection_quantity,task_status,deleted,tenant_id) VALUES (301,101,201,1,1,1,1,1,'FIRST','FIRST','2026-09-29','DAY',1,1,'SUBMITTED',1,92820)");
                st.executeUpdate("INSERT INTO mes_pqc_inspection_piece_detail (id,task_id,sample_no,item_code,item_name,inspection_method,standard_text,result_type,judgement,deleted,tenant_id) VALUES (401,301,1,'M9','M9','M9','M9','TEXT','PASS',1,92820)");
                return "mes_pqc_inspection_piece_detail";
            }
            if (kind.equals("aggregate")) {
                st.executeUpdate("INSERT INTO mes_pqc_process_inspection_aggregate_detail (id,source_pqc_record_id,source_piece_detail_id,event_id,review_id,production_submit_event_id,pqc_task_id,active_order_id,work_order_id,route_id,route_version_id,route_process_id,process_id,regulation_version_id,inspection_type,business_date,shift_code,round_no,actual_inspection_quantity,sample_no,item_code,item_name,inspection_method,standard_text,result_type,measured_value,judgement,aggregated_at,deleted,tenant_id) VALUES (402,901,902,903,904,905,906,101,201,1,1,1,1,1,'FIRST','2026-09-29','DAY',1,1,1,'M9','M9','M9','M9','TEXT','PASS','PASS',NOW(),1,92820)");
                return "mes_pqc_process_inspection_aggregate_detail";
            }
            st.executeUpdate("INSERT INTO mes_pro_process_pool_order_process_completion (id,work_order_id,route_process_id,process_id,target_quantity,confirmed_quantity,completion_status,backfill_status,last_event_id,source_event_ids_json,source_allocation_ids_json,aggregate_hash,backfill_idempotency_key,deleted,tenant_id) VALUES (403,201,1,1,1,1,'COMPLETED','SUCCESS',999,'[999]','[998]',REPEAT('a',64),'m9',1,92820)");
            return "mes_pro_process_pool_order_process_completion";
        }
    }
}
