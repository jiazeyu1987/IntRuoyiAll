package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.config.YudaoTenantAutoConfiguration;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.h2.api.Trigger;
import org.junit.jupiter.api.*;
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

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** Actual business mappers, GxP writer, specialized audit and one H2/Spring physical transaction.
 * The test-only SQL interceptor translates MySQL bit literals; this does not verify InnoDB locks.
 */
class MesActiveOrderMaintenanceAuditTransactionTest {
    private JdbcTemplate jdbc;
    private MesTeamLeaderActiveOrderService service;
    private MesProcessPoolActiveOrderMapper orders;
    private static final String ORDER_TABLE = "mes_pro_process_pool_active_order";
    private static final String SNAPSHOT_TABLE = ORDER_TABLE + "_process_snapshot";
    private static final String AUDIT_TABLE = "mes_pro_process_pool_team_maintenance_audit";
    private static final AtomicBoolean FAILURE_OBSERVED_REAL_WRITES = new AtomicBoolean();
    // Physical tenant columns in 20260512_mes_base_schema.sql / 20260624_mes_feedback_surplus_pool.sql,
    // not represented by these four legacy BaseDO mappings. No invented cross-table scope columns.
    private static final Set<Class<?>> PHYSICAL_TENANT_COLUMN_TYPES = Set.of(MesProFeedbackDO.class,
            MesProFeedbackImportRecordDO.class, MesProFeedbackSurplusPoolDO.class, MesProFeedbackSurplusAllocationDO.class);
    private static final List<Class<?>> CLEANUP_CHILD_TYPES = List.of(
            MesProcessPoolActiveOrderProcessSnapshotDO.class, MesPqcInspectionTaskDO.class,
            MesPqcInspectionPieceDetailDO.class, MesPqcProcessInspectionAggregateDetailDO.class,
            MesProcessPoolReportAllocationDO.class, MesProcessPoolReportAllocationAdjustmentAuditDO.class,
            MesProcessPoolOrderProcessCompletionDO.class, MesProcessPoolActiveOrderCompletionBackfillDO.class,
            MesProcessPoolActiveOrderCompletionReceiptDO.class, MesProcessPoolActiveOrderTransferTraceDO.class,
            MesProcessPoolActiveOrderPickListBindingDO.class, MesProcessPoolActiveOrderPickListBindingItemDO.class,
            MesProcessPoolActiveOrderVersionUpgradeRequestDO.class, MesProcessPoolWorkOrderAbnormalDO.class,
            MesProFeedbackDO.class, MesProFeedbackMaterialDO.class, MesProFeedbackImportRecordDO.class,
            MesProFeedbackSurplusPoolDO.class, MesProFeedbackSurplusAllocationDO.class,
            MesProcessPoolFifoAllocationLineDO.class, MesProProcessPoolDO.class,
            MesProProcessPoolEventDO.class, MesProEdhrBatchExecutionDO.class);

    // Each row matches only the named reachable arm; 99999 is outside the confirmed scope.
    private static final Map<String, Map<Long, Map<String, Long>>> COMPOSITE_SCOPE = Map.of(
            "mes_pro_feedback", Map.of(611L, Map.of("work_order_id", 1101L)),
            "mes_pro_feedback_import_record", Map.of(621L, Map.of("feedback_id", 611L)),
            "mes_pro_feedback_material", Map.of(
                    631L, Map.of("active_order_id", 101L, "feedback_id", 99999L),
                    632L, Map.of("active_order_id", 99999L, "feedback_id", 611L)),
            "mes_pro_feedback_surplus_pool", Map.of(
                    641L, Map.of("source_feedback_id", 611L, "source_import_record_id", 99999L),
                    642L, Map.of("source_feedback_id", 99999L, "source_import_record_id", 621L)),
            "mes_pro_feedback_surplus_allocation", Map.of(
                    651L, Map.of("pool_id", 641L, "import_record_id", 99999L),
                    652L, Map.of("pool_id", 642L, "import_record_id", 99999L),
                    653L, Map.of("pool_id", 99999L, "import_record_id", 621L)),
            ORDER_TABLE + "_version_upgrade_request", Map.of(
                    661L, Map.of("source_active_order_id", 101L, "target_active_order_id", 99999L,
                            "source_work_order_id", 99999L),
                    662L, Map.of("source_active_order_id", 99999L, "target_active_order_id", 102L,
                            "source_work_order_id", 99999L),
                    663L, Map.of("source_active_order_id", 99999L, "target_active_order_id", 99999L,
                            "source_work_order_id", 1101L)),
            "mes_pro_process_pool_fifo_allocation_line", Map.of(
                    671L, Map.of("target_work_order_id", 1101L, "source_event_id", 99999L)));

    @BeforeEach
    void fixture() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:maintenance_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        for (Class<?> row : List.of(MesProcessPoolActiveOrderDO.class,
                MesProcessPoolActiveOrderReleaseApplicationDO.class,
                MesProcessPoolTeamMaintenanceAuditDO.class, GxpAuditEventDO.class,
                GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class,
                GxpAuditPolicyActivationDO.class, GxpAuditPolicyOperationDO.class)) {
            createTable(row);
        }
        CLEANUP_CHILD_TYPES.forEach(this::createTable);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addInterceptor(new MysqlBitLiteralForH2());
        var tenantInterceptor = new MybatisPlusInterceptor();
        new YudaoTenantAutoConfiguration().tenantLineInnerInterceptor(new TenantProperties(), tenantInterceptor);
        config.addInterceptor(tenantInterceptor);
        List<Class<?>> mapperTypes = List.of(MesProcessPoolActiveOrderMapper.class,
                MesTeamLeaderDataCleanupMapper.class, MesProcessPoolActiveOrderReleaseApplicationMapper.class,
                MesProcessPoolTeamMaintenanceAuditMapper.class, GxpAuditEventMapper.class,
                GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class);
        mapperTypes.forEach(config::addMapper);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(config);
        var session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        var manager = new DataSourceTransactionManager(dataSource);
        orders = session.getMapper(MesProcessPoolActiveOrderMapper.class);
        var writer = new GxpAuditServiceImpl();
        inject(writer, "auditEventMapper", session.getMapper(GxpAuditEventMapper.class));
        inject(writer, "eventRelationMapper", session.getMapper(GxpAuditEventRelationMapper.class));
        inject(writer, "ledgerSequenceMapper", session.getMapper(GxpAuditLedgerSequenceMapper.class));
        inject(writer, "policyActivationMapper", session.getMapper(GxpAuditPolicyActivationMapper.class));
        inject(writer, "policyOperationMapper", session.getMapper(GxpAuditPolicyOperationMapper.class));
        var constructor = Arrays.stream(MesTeamLeaderActiveOrderServiceImpl.class.getConstructors())
                .max(Comparator.comparingInt(java.lang.reflect.Constructor::getParameterCount)).orElseThrow();
        var target = (MesTeamLeaderActiveOrderServiceImpl) constructor.newInstance(new Object[constructor.getParameterCount()]);
        inject(target, "activeOrderMapper", orders);
        inject(target, "releaseApplicationMapper", session.getMapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class));
        inject(target, "auditMapper", session.getMapper(MesProcessPoolTeamMaintenanceAuditMapper.class));
        inject(target, "dataCleanupMapper", session.getMapper(MesTeamLeaderDataCleanupMapper.class));
        inject(target, "gxpAuditService", transactional(writer, manager));
        // Also allows running these tests against the pre-fix class for an observable missing-event RED.
        for (var field : target.getClass().getDeclaredFields()) {
            if (javax.sql.DataSource.class.equals(field.getType())) {
                field.setAccessible(true);
                field.set(target, dataSource);
            }
        }
        service = (MesTeamLeaderActiveOrderService) transactional(target, manager);
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
        LoginUser actor = new LoginUser();
        actor.setId(3001L);
        actor.setTenantId(1L);
        actor.setInfo(Map.of("username", "maintenance-test", LoginUser.INFO_KEY_NICKNAME, "maintenance-test"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(1,1)");
        jdbc.update("INSERT INTO gxp_audit_policy_activation(id,tenant_id,policy_version) VALUES(1,1,'maintenance-test')");
        for (String operation : List.of("mes.active-order.reorder", "mes.active-order.data-cleanup")) {
            jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,"
                    + "domain,subject_type,action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                    + " VALUES(1,'maintenance-test',?,'MES',?,'UPDATE','SYSTEM_WITH_OPTIONAL_TEXT','NONE',"
                    + "'PRESENT_TO_PRESENT','GXP',1)", operation,
                    operation.endsWith("reorder") ? "MES_ACTIVE_ORDER" : "MES_RUNTIME_CLEANUP");
        }
        seedOrder(101, 3001, 1, 7, 1, "ACTIVE");
        seedOrder(102, 3001, 2, 9, 1, "ACTIVE");
        seedOrder(103, 3001, 3, 4, 1, "ACTIVE");
        FAILURE_OBSERVED_REAL_WRITES.set(false);
    }

    @AfterEach
    void close() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        if (jdbc != null) {
            jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
                try (var statement = connection.createStatement()) { statement.execute("SHUTDOWN"); }
                return null;
            });
        }
    }

    @Test
    void reorderPersistsBothOrdersAndRealAudit() {
        service.moveActiveOrder(move(102, "UP"));
        assertEquals(1L, orders.selectById(102L).getSortOrder());
        assertEquals(10, orders.selectById(102L).getVersion());
        assertEquals(2L, orders.selectById(101L).getSortOrder());
        assertEquals(8, orders.selectById(101L).getVersion());
        assertEquals(3L, orders.selectById(103L).getSortOrder());
        assertEquals(4, orders.selectById(103L).getVersion());
        assertEquals(1, count("gxp_audit_event"));
        JSONObject before = state("before_state_json");
        JSONObject after = state("after_state_json");
        assertEquals(102L, before.getJSONObject("target").getLong("id"));
        assertEquals(2L, before.getJSONObject("target").getLong("sortOrder"));
        assertEquals(9, before.getJSONObject("target").getInteger("version"));
        assertEquals(101L, before.getJSONObject("adjacent").getLong("id"));
        assertEquals(1L, before.getJSONObject("adjacent").getLong("sortOrder"));
        assertEquals(1L, after.getJSONObject("target").getLong("sortOrder"));
        assertEquals(10, after.getJSONObject("target").getInteger("version"));
        assertEquals(2L, after.getJSONObject("adjacent").getLong("sortOrder"));
        assertEquals(8, after.getJSONObject("adjacent").getInteger("version"));
        assertEquals(Set.of("101", "102"), new HashSet<>(jdbc.queryForList(
                "SELECT target_id FROM gxp_audit_event_relation WHERE target_type='ACTIVE_ORDER'", String.class)));
        assertEquals("SYSTEM", jdbc.queryForObject("SELECT reason_source FROM gxp_audit_event", String.class));
        assertEquals(1, count(AUDIT_TABLE));
        assertEquals(2L, watermark());
    }

    @Test
    void reorderAuditFailureRollsBackBothOrdersAndSpecializedAudit() {
        installFailureTrigger();
        assertThrows(RuntimeException.class, () -> service.moveActiveOrder(move(102, "UP")));
        assertTrue(FAILURE_OBSERVED_REAL_WRITES.get(), "Failure must follow actual business, specialized and unified writes");
        assertEquals(1L, orders.selectById(101L).getSortOrder());
        assertEquals(7, orders.selectById(101L).getVersion());
        assertEquals(2L, orders.selectById(102L).getSortOrder());
        assertEquals(9, orders.selectById(102L).getVersion());
        assertRollback();
    }

    @Test
    void cleanupPersistsExactScopeAndRealAudit() {
        seedCleanupChildren();
        var result = service.executeDataCleanup(3001L, service.previewDataCleanup(3001L));
        assertEquals(4, result.getActiveOrderCount());
        assertEquals(1, count("gxp_audit_event"));
        JSONObject before = state("before_state_json").getJSONObject("tables");
        JSONObject after = state("after_state_json").getJSONObject("tables");
        assertEquals(5, before.getJSONArray(ORDER_TABLE).size(), "Includes another leader and removed history");
        assertEquals(Set.of(201L, 202L), ids(before, SNAPSHOT_TABLE));
        assertEquals("frozen-before-cleanup", before.getJSONArray(SNAPSHOT_TABLE).getJSONObject(0)
                .getString("production_config_snapshot_json"));
        assertTrue(after.getJSONArray(SNAPSHOT_TABLE).isEmpty());
        assertEquals(Set.of(301L), ids(before, ORDER_TABLE + "_pick_list_binding_item"));
        assertTrue(after.getJSONArray(ORDER_TABLE + "_pick_list_binding_item").isEmpty());
        assertEquals(Set.of(401L), ids(before, "mes_pqc_inspection_task"));
        assertTrue(after.getJSONArray("mes_pqc_inspection_task").isEmpty());
        assertEquals(Set.of(501L), ids(before, "mes_pro_process_pool"));
        assertTrue(after.getJSONArray("mes_pro_process_pool").isEmpty());
        assertEquals("REMOVED", orders.selectById(104L).getActiveStatus());
        assertEquals(3, orders.selectById(104L).getVersion());
        assertNull(orders.selectById(901L), "Production tenant interceptor hides the foreign row");
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT active_status FROM " + ORDER_TABLE + " WHERE id=901", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM " + SNAPSHOT_TABLE + " WHERE tenant_id=2", Integer.class));
        assertEquals(1, count(AUDIT_TABLE));
        assertEquals(2L, watermark());
        assertNull(jdbc.queryForObject("SELECT before_object_version FROM gxp_audit_event", String.class));
        assertNull(jdbc.queryForObject("SELECT after_object_version FROM gxp_audit_event", String.class));
    }

    @Test
    void cleanupAuditFailureRollsBackDeletedChildrenAndSpecializedAudit() {
        seedCleanupChildren();
        var beforeOrders = jdbc.queryForList("SELECT * FROM " + ORDER_TABLE + " ORDER BY id");
        var beforeChildren = jdbc.queryForList("SELECT * FROM " + SNAPSHOT_TABLE + " ORDER BY id");
        installFailureTrigger();
        assertThrows(RuntimeException.class, () -> service.executeDataCleanup(3001L, service.previewDataCleanup(3001L)));
        assertTrue(FAILURE_OBSERVED_REAL_WRITES.get());
        assertEquals(beforeOrders, jdbc.queryForList("SELECT * FROM " + ORDER_TABLE + " ORDER BY id"));
        assertEquals(beforeChildren, jdbc.queryForList("SELECT * FROM " + SNAPSHOT_TABLE + " ORDER BY id"));
        assertEquals(1, count(ORDER_TABLE + "_pick_list_binding_item"));
        assertEquals(1, count("mes_pqc_inspection_task"));
        assertEquals(1, count("mes_pro_process_pool"));
        assertRollback();
    }

    @Test
    void missingActivatedCleanupPolicyRollsBackBusinessAndSpecializedWrites() {
        seedCleanupChildren();
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='mes.active-order.data-cleanup'");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.executeDataCleanup(3001L, service.previewDataCleanup(3001L)));
        assertEquals("ACTIVE", orders.selectById(101L).getActiveStatus());
        assertEquals(3, count(SNAPSHOT_TABLE));
        assertRollback();
    }

    @Test
    void retainedEventAndChangedScopeRejectBeforeMutation() {
        var scope = service.previewDataCleanup(3001L);
        jdbc.update("UPDATE " + ORDER_TABLE + " SET version=version+1 WHERE id=101");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.executeDataCleanup(3001L, scope));
        jdbc.update("INSERT INTO mes_pro_process_pool_event(id,tenant_id,event_type) VALUES(700,1,'PRODUCTION_SUBMIT')");
        var current = service.previewDataCleanup(3001L);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.executeDataCleanup(3001L, current));
        assertEquals("ACTIVE", orders.selectById(101L).getActiveStatus());
        assertRollback();
    }

    @Test
    void invalidDirectionAndBoundaryRemainRejected() {
        for (var request : List.of(move(101, "UP"), move(102, "SIDEWAYS"))) {
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.moveActiveOrder(request));
        }
        assertEquals(9, orders.selectById(102L).getVersion());
        assertRollback();
    }

    @Test
    void missingOrderRemainsRejected() {
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.moveActiveOrder(move(999, "DOWN")));
        assertRollback();
    }

    @Test
    void existingOtherLeaderOrderRemainsRejectedWithoutAnyWrites() {
        seedOrder(104, 4001, 1, 2, 1, "ACTIVE");
        var before = jdbc.queryForList("SELECT * FROM " + ORDER_TABLE + " ORDER BY id");
        assertEquals(4001L, orders.selectById(104L).getLeaderUserId(), "The refused order must really exist");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.moveActiveOrder(move(104, "DOWN")));
        assertEquals(before, jdbc.queryForList("SELECT * FROM " + ORDER_TABLE + " ORDER BY id"));
        assertRollback();
    }

    @Test
    void otherTenantRowsCannotEnterReorderAdjacency() {
        seedOrder(150, 3001, 1, 17, 2, "ACTIVE");
        seedOrder(250, 3001, 2, 19, 2, "ACTIVE");
        var foreignBefore = jdbc.queryForList("SELECT * FROM " + ORDER_TABLE + " WHERE tenant_id=2 ORDER BY id");
        assertEquals(List.of(101L, 102L, 103L), orders.selectActiveListByLeader(3001L).stream()
                .map(MesProcessPoolActiveOrderDO::getId).toList(),
                "Fixture must use the actual production tenant interceptor before claiming isolation");
        assertNull(orders.selectById(150L));
        service.moveActiveOrder(move(102, "UP"));
        assertEquals(101L, state("before_state_json").getJSONObject("adjacent").getLong("id"));
        assertEquals(Set.of("101", "102"), new HashSet<>(jdbc.queryForList(
                "SELECT target_id FROM gxp_audit_event_relation WHERE target_type='ACTIVE_ORDER'", String.class)));
        assertEquals(1L, orders.selectById(102L).getSortOrder());
        assertEquals(10, orders.selectById(102L).getVersion());
        assertEquals(2L, orders.selectById(101L).getSortOrder());
        assertEquals(8, orders.selectById(101L).getVersion());
        assertEquals(foreignBefore, jdbc.queryForList("SELECT * FROM " + ORDER_TABLE + " WHERE tenant_id=2 ORDER BY id"));
        assertEquals(1, count(AUDIT_TABLE));
        assertEquals(2L, watermark());
    }

    @Test
    void cleanupFixtureTablesHaveOnlyTheirActualMappedColumns() {
        assertAll(CLEANUP_CHILD_TYPES.stream().map(row -> (org.junit.jupiter.api.function.Executable) () -> {
            String table = row.getAnnotation(TableName.class).value();
            Set<String> expected = new TreeSet<>();
            if (PHYSICAL_TENANT_COLUMN_TYPES.contains(row)) expected.add("tenant_id");
            for (Class<?> type = row; type != Object.class; type = type.getSuperclass()) {
                for (var field : type.getDeclaredFields()) {
                    var mapping = field.getAnnotation(TableField.class);
                    if (Modifier.isStatic(field.getModifiers()) || (mapping != null && !mapping.exist())) continue;
                    expected.add(mapping != null && !mapping.value().isBlank() ? mapping.value()
                            : field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT));
                }
            }
            var actual = new TreeSet<>(jdbc.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=?", String.class, table));
            assertEquals(expected, actual, table + " must not accept another table's scope columns");
        }));
    }

    @Test
    void cleanupCompositeBranchesCaptureExactRowsAndPreserveControls() {
        seedCleanupChildren();
        assertCompositeFixturePresent();
        Map<String, List<Map<String, Object>>> controls = compositeControls();
        service.executeDataCleanup(3001L, service.previewDataCleanup(3001L));
        JSONObject before = state("before_state_json").getJSONObject("tables");
        JSONObject after = state("after_state_json").getJSONObject("tables");
        COMPOSITE_SCOPE.forEach((table, expected) -> {
            assertEquals(expected.keySet(), ids(before, table), table + " exact captured identities");
            for (Object value : before.getJSONArray(table)) {
                JSONObject row = (JSONObject) value;
                assertEquals(1L, row.getLong("tenant_id"));
                expected.get(row.getLong("id")).forEach((column, wanted) ->
                        assertEquals(wanted, row.getLong(column), table + "." + column));
            }
            assertTrue(after.getJSONArray(table).isEmpty(), table + " actual persisted after-state");
            for (Long id : expected.keySet()) {
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id=?", Integer.class, id));
            }
        });
        assertEquals(controls, compositeControls(), "Nonmatching and cross-tenant controls must survive unchanged");
        assertEquals(1, count(AUDIT_TABLE));
        assertEquals(2L, watermark());
    }

    @Test
    void cleanupCompositeBranchesRollBackAllPhysicalRowsAfterRealAuditFailure() {
        seedCleanupChildren();
        assertCompositeFixturePresent();
        Map<String, List<Map<String, Object>>> before = new LinkedHashMap<>();
        COMPOSITE_SCOPE.keySet().forEach(table -> before.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id")));
        var beforeOrders = jdbc.queryForList("SELECT * FROM " + ORDER_TABLE + " ORDER BY id");
        installFailureTrigger();
        assertThrows(RuntimeException.class, () -> service.executeDataCleanup(3001L, service.previewDataCleanup(3001L)));
        assertTrue(FAILURE_OBSERVED_REAL_WRITES.get(), "Failure must follow physical cleanup and both audit writes");
        before.forEach((table, rows) -> assertEquals(rows, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"), table));
        assertEquals(beforeOrders, jdbc.queryForList("SELECT * FROM " + ORDER_TABLE + " ORDER BY id"));
        assertRollback();
    }

    private void assertCompositeFixturePresent() {
        COMPOSITE_SCOPE.forEach((table, rows) -> {
            var actual = new HashSet<>(jdbc.queryForList("SELECT id FROM " + table + " WHERE tenant_id=1 AND id<1000", Long.class));
            assertEquals(rows.keySet(), actual, table + " requires nonempty independent OR-arm fixtures");
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=1 AND id>=1000", Integer.class),
                    table + " requires an unrelated current-tenant control");
            assertEquals(rows.size(), jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=2", Integer.class),
                    table + " requires matching-scope other-tenant controls for every arm");
        });
    }

    private Map<String, List<Map<String, Object>>> compositeControls() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        COMPOSITE_SCOPE.keySet().forEach(table -> result.put(table,
                jdbc.queryForList("SELECT * FROM " + table + " WHERE id>=1000 ORDER BY id")));
        return result;
    }

    private void seedCleanupChildren() {
        seedOrder(104, 4001, 1, 2, 1, "ACTIVE");
        seedOrder(105, 4001, 2, 5, 1, "REMOVED");
        seedOrder(901, 9001, 1, 1, 2, "ACTIVE");
        jdbc.update("INSERT INTO " + SNAPSHOT_TABLE
                + "(id,tenant_id,active_order_id,production_config_snapshot_json,deleted)"
                + " VALUES(201,1,101,'frozen-before-cleanup',0),(202,1,105,'removed-history',1),(203,2,901,'other-tenant',0)");
        jdbc.update("INSERT INTO " + ORDER_TABLE + "_pick_list_binding(id,tenant_id,active_order_id) VALUES(300,1,104)");
        jdbc.update("INSERT INTO " + ORDER_TABLE + "_pick_list_binding_item(id,tenant_id,binding_id) VALUES(301,1,300)");
        jdbc.update("INSERT INTO mes_pqc_inspection_task(id,tenant_id,active_order_id) VALUES(401,1,104)");
        jdbc.update("INSERT INTO mes_pro_process_pool(id,tenant_id,work_order_id) VALUES(501,1,9999)");
        COMPOSITE_SCOPE.forEach((table, rows) -> {
            rows.forEach((id, columns) -> {
                insertCompositeRow(table, id, 1L, columns);
                insertCompositeRow(table, id + 2000, 2L, columns);
            });
            Map<String, Long> unrelated = new LinkedHashMap<>();
            rows.values().forEach(columns -> columns.keySet().forEach(column -> unrelated.put(column, 99999L)));
            insertCompositeRow(table, Collections.min(rows.keySet()) + 1000, 1L, unrelated);
        });
    }

    private void insertCompositeRow(String table, long id, long tenantId, Map<String, Long> scope) {
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("tenant_id", tenantId);
        values.putAll(scope);
        jdbc.update("INSERT INTO " + table + "(" + String.join(",", values.keySet()) + ") VALUES("
                + String.join(",", Collections.nCopies(values.size(), "?")) + ")", values.values().toArray());
    }

    private void seedOrder(long id, long leader, long sort, int version, long tenant, String status) {
        jdbc.update("INSERT INTO " + ORDER_TABLE + "(id,leader_user_id,sort_order,version,tenant_id,"
                + "work_order_id,route_id,route_version_id,active_status,business_status,joined_at,simulated)"
                + " VALUES(?,?,?,?,?,?,10,11,?,?,?,0)", id, leader, sort, version, tenant, id + 1000,
                status, status, LocalDateTime.of(2026, 9, 29, 8, 0));
    }

    private void installFailureTrigger() {
        jdbc.execute("CREATE TRIGGER fail_relation BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + FailRelationAfterRealWrites.class.getName() + "'");
    }

    public static class FailRelationAfterRealWrites implements Trigger {
        @Override
        public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT (SELECT COUNT(*) FROM gxp_audit_event),"
                         + "(SELECT COUNT(*) FROM " + AUDIT_TABLE + "),"
                         + "(SELECT version FROM " + ORDER_TABLE + " WHERE id=102),"
                         + "(SELECT COUNT(*) FROM " + SNAPSHOT_TABLE + " WHERE tenant_id=1)")) {
                result.next();
                if (result.getInt(1) == 1 && result.getInt(2) == 1
                        && (result.getInt(3) == 10 || result.getInt(4) == 0)) {
                    FAILURE_OBSERVED_REAL_WRITES.set(true);
                }
            }
            // Observe each reachable OR arm after physical deletion, before the injected rollback.
            for (String table : COMPOSITE_SCOPE.keySet()) {
                try (var statement = connection.createStatement();
                     var remaining = statement.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=1 AND id<1000")) {
                    remaining.next();
                    if (remaining.getInt(1) != 0) FAILURE_OBSERVED_REAL_WRITES.set(false);
                }
            }
            throw new SQLException("Injected real relation constraint failure");
        }
    }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    public static class MysqlBitLiteralForH2 implements Interceptor {
        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            var statement = (StatementHandler) invocation.getTarget();
            var bound = statement.getBoundSql();
            ReflectionTestUtils.setField(bound, "sql", bound.getSql().replaceAll("(?i)\\bb'([01])'", "$1"));
            return invocation.proceed();
        }
    }

    private Object transactional(Object target, DataSourceTransactionManager manager) {
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }

    private static void inject(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }

    private MesTeamLeaderActiveOrderMoveReqBO move(long id, String direction) {
        return MesTeamLeaderActiveOrderMoveReqBO.builder().leaderUserId(3001L).activeOrderId(id).direction(direction).build();
    }

    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private long watermark() { return jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=1", Long.class); }
    private JSONObject state(String column) { return JSON.parseObject(jdbc.queryForObject("SELECT " + column + " FROM gxp_audit_event", String.class)); }

    private Set<Long> ids(JSONObject tables, String table) {
        Set<Long> result = new LinkedHashSet<>();
        for (Object row : tables.getJSONArray(table)) result.add(((JSONObject) row).getLong("id"));
        return result;
    }

    private void assertRollback() {
        assertEquals(0, count("gxp_audit_event"));
        assertEquals(0, count("gxp_audit_event_relation"));
        assertEquals(0, count(AUDIT_TABLE));
        assertEquals(1L, watermark());
    }

    private void createTable(Class<?> row) {
        String table = row.getAnnotation(TableName.class).value();
        List<String> columns = new ArrayList<>();
        if (PHYSICAL_TENANT_COLUMN_TYPES.contains(row)) columns.add("tenant_id BIGINT NOT NULL DEFAULT 0");
        for (Class<?> type = row; type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                var mapping = field.getAnnotation(TableField.class);
                if (Modifier.isStatic(field.getModifiers()) || (mapping != null && !mapping.exist())) continue;
                String name = mapping != null && !mapping.value().isBlank() ? mapping.value()
                        : field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
                String sqlType = field.getType() == Long.class ? "BIGINT" :
                        field.getType() == Integer.class ? "INT" : field.getType() == Boolean.class ? "INT DEFAULT 0" :
                        field.getType() == java.math.BigDecimal.class ? "DECIMAL(24,8)" :
                        field.getType() == LocalDateTime.class ? "TIMESTAMP" :
                        field.getType() == LocalDate.class ? "DATE" : "VARCHAR(1000000)";
                columns.add("`" + name + "` " + (name.equals("id") ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : sqlType));
            }
        }
        jdbc.execute("CREATE TABLE " + table + "(" + String.join(",", columns) + ")");
    }
}
