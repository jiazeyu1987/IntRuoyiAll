package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.h2.api.Trigger;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real projection mapper, audit writer and one physical H2 transaction; directory lookups are doubles. */
class MesSignatureProjectionAuditTransactionTest {
    private static final String TABLE = "mes_pro_batch_record_execution_signature";
    private static final String EMPLOYEE_CREATE = "mes.employee-production-signature.create";
    private static final String DRAFT_CREATE = "edhr.field-save-evidence.create";
    private static final String BIND = "edhr.field-save-evidence.bind";
    private static final AtomicBoolean FAILURE_SAW_WRITES = new AtomicBoolean();
    private JdbcTemplate jdbc;
    private MesProBatchRecordExecutionSignatureService service;
    private MesProBatchRecordExecutionSignatureMapper signatures;
    private DataSourceTransactionManager transactionManager;

    @BeforeEach
    void fixture() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:signature_audit_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        for (Class<?> row : List.of(MesProBatchRecordExecutionSignatureDO.class, GxpAuditEventDO.class,
                GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class,
                GxpAuditPolicyActivationDO.class, GxpAuditPolicyOperationDO.class)) createTable(row);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        List.of(MesProBatchRecordExecutionSignatureMapper.class, GxpAuditEventMapper.class,
                GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class).forEach(config::addMapper);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(config);
        var session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        transactionManager = new DataSourceTransactionManager(dataSource);
        signatures = session.getMapper(MesProBatchRecordExecutionSignatureMapper.class);
        var writer = new GxpAuditServiceImpl();
        ReflectionTestUtils.setField(writer, "auditEventMapper", session.getMapper(GxpAuditEventMapper.class));
        ReflectionTestUtils.setField(writer, "eventRelationMapper", session.getMapper(GxpAuditEventRelationMapper.class));
        ReflectionTestUtils.setField(writer, "ledgerSequenceMapper", session.getMapper(GxpAuditLedgerSequenceMapper.class));
        ReflectionTestUtils.setField(writer, "policyActivationMapper", session.getMapper(GxpAuditPolicyActivationMapper.class));
        ReflectionTestUtils.setField(writer, "policyOperationMapper", session.getMapper(GxpAuditPolicyOperationMapper.class));
        var target = new MesProBatchRecordExecutionSignatureService();
        ReflectionTestUtils.setField(target, "signatureMapper", signatures);
        var directory = mock(AdminUserService.class);
        when(directory.getUser(99L)).thenReturn(AdminUserDO.builder().id(99L)
                .username("device-user").nickname("Device operator").build());
        ReflectionTestUtils.setField(target, "adminUserService", directory);
        ReflectionTestUtils.setField(target, "deptService", mock(DeptService.class));
        ReflectionTestUtils.setField(target, "postService", mock(PostService.class));
        ReflectionTestUtils.setField(target, "roleService", mock(RoleService.class));
        ReflectionTestUtils.setField(target, "permissionService", mock(PermissionService.class));
        var encoder = new BCryptPasswordEncoder(4);
        var profiles = mock(MesProcessPoolTeamEmployeeProfileMapper.class);
        when(profiles.selectById(8801L)).thenReturn(MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(8801L).employeeCode("TMP-8801").employeeName("Employee A").displayName("Employee A")
                .enabled(true).signaturePasswordHash(encoder.encode("fixture-signature-password")).build());
        ReflectionTestUtils.setField(target, "employeeProfileMapper", profiles);
        ReflectionTestUtils.setField(target, "passwordEncoder", encoder);
        // Allows the same test to run against the pre-fix class and fail on missing observable audit.
        for (var field : target.getClass().getDeclaredFields()) {
            if (GxpAuditService.class.equals(field.getType())) {
                field.setAccessible(true);
                field.set(target, transactional(writer));
            }
        }
        service = (MesProBatchRecordExecutionSignatureService) transactional(target);
        TenantContextHolder.setTenantId(1L);
        LoginUser actor = new LoginUser();
        actor.setId(99L);
        actor.setTenantId(1L);
        actor.setInfo(Map.of("username", "device-user", LoginUser.INFO_KEY_NICKNAME, "Device operator"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(1,1)");
        jdbc.update("INSERT INTO gxp_audit_policy_activation(id,tenant_id,policy_version) VALUES(1,1,'signature-fixture')");
        for (String operation : List.of(EMPLOYEE_CREATE, DRAFT_CREATE, BIND)) {
            jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,domain,"
                    + "subject_type,action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                    + " VALUES(1,'signature-fixture',?,'MES','MES_SIGNATURE_PROJECTION',?,"
                    + "'SYSTEM_WITH_OPTIONAL_TEXT','NONE',?,'GXP',1)", operation,
                    BIND.equals(operation) ? "UPDATE" : "CREATE",
                    BIND.equals(operation) ? "PRESENT_TO_PRESENT" : "ABSENT_TO_PRESENT");
        }
        FAILURE_SAW_WRITES.set(false);
    }

    @AfterEach
    void close() throws SQLException {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        if (jdbc != null) {
            // SHUTDOWN closes H2 before JdbcTemplate can inspect statement warnings.
            try (Connection connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection();
                 var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @Test
    void employeeCreateCapturesPersistedProjectionWithoutInventingSystemSigner() {
        Long id = service.recordProductionSubmitSignature(8801L, "fixture-signature-password",
                "Production submit", productionContext());
        assertEquals(1, count("gxp_audit_event"));
        JSONObject event = event(EMPLOYEE_CREATE);
        assertEquals("ABSENT", event.getString("before_state"));
        assertEquals("PRESENT", event.getString("after_state"));
        assertEquals(snapshot(id), JSON.parseObject(event.getString("after_state_json")));
        assertEquals(99L, event.getLong("actor_id"));
        JSONObject performer = JSON.parseObject(event.getString("performed_by_json"));
        assertEquals("MES_EMPLOYEE_PROFILE", performer.getString("identityDomain"));
        assertEquals(8801L, performer.getLong("id"));
        assertNull(event.getString("signature_record_id"), "MES projection ID is not a unified signature ID");
        assertEquals("PASSWORD", signatures.selectById(id).getSignatureMode());
        assertTrue(signatures.selectById(id).getPasswordVerified());
        assertFalse(event.toJSONString().contains("fixture-signature-password"));
        assertFalse(event.toJSONString().contains("signaturePasswordHash"));
    }

    @Test
    void draftCreateCapturesActualUnverifiedSessionEvidence() {
        Long id = service.recordFieldChangeDraftSave(draft()).getSignatureId();
        assertEquals(1, count("gxp_audit_event"));
        JSONObject event = event(DRAFT_CREATE);
        assertEquals("ABSENT", event.getString("before_state"));
        assertEquals(snapshot(id), JSON.parseObject(event.getString("after_state_json")));
        assertFalse(signatures.selectById(id).getPasswordVerified());
        assertEquals("DRAFT_SESSION", signatures.selectById(id).getSignatureMode());
        assertNull(event.getString("signature_record_id"));
    }

    @Test
    void bindingCapturesExactPersistedBeforeAndAfter() {
        Long id = seedDraft();
        JSONObject before = snapshot(id);
        service.attachFieldChangeSignature(binding(id));
        assertEquals(1, count("gxp_audit_event"));
        JSONObject event = event(BIND);
        assertEquals(before, JSON.parseObject(event.getString("before_state_json")));
        assertEquals(snapshot(id), JSON.parseObject(event.getString("after_state_json")));
        assertEquals(7001L, signatures.selectById(id).getAuditBatchId());
        assertEquals("head-after", signatures.selectById(id).getFieldAuditHeadHash());
        assertEquals("challenge-after", signatures.selectById(id).getSignatureChallengeHash());
        assertFalse(signatures.selectById(id).getPasswordVerified());
    }

    @Test
    void employeeAuditFailureRollsBackProjectionAndLedger() {
        installFailureTrigger();
        assertThrows(RuntimeException.class, () -> service.recordProductionSubmitSignature(
                8801L, "fixture-signature-password", "Production submit", productionContext()));
        assertTrue(FAILURE_SAW_WRITES.get(), "Relation failure must see real projection and real event");
        assertEquals(0, count(TABLE));
        assertAuditRollback();
    }

    @Test
    void draftAuditFailureRollsBackProjectionAndLedger() {
        installFailureTrigger();
        assertThrows(RuntimeException.class, () -> service.recordFieldChangeDraftSave(draft()));
        assertTrue(FAILURE_SAW_WRITES.get());
        assertEquals(0, count(TABLE));
        assertAuditRollback();
    }

    @Test
    void bindingAuditFailureRestoresPriorPersistedBindings() {
        Long id = seedDraft();
        JSONObject before = snapshot(id);
        installFailureTrigger();
        assertThrows(RuntimeException.class, () -> service.attachFieldChangeSignature(binding(id)));
        assertTrue(FAILURE_SAW_WRITES.get());
        assertEquals(before, snapshot(id));
        assertAuditRollback();
    }

    @Test
    void missingActivatedOperationRejectsProjectionCommit() {
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id=?", EMPLOYEE_CREATE);
        assertThrows(RuntimeException.class, () -> service.recordProductionSubmitSignature(
                8801L, "fixture-signature-password", "Production submit", productionContext()));
        assertEquals(0, count(TABLE));
        assertAuditRollback();
    }

    @Test
    void parentFailureRollsBackBothProjectionEventsAndBinding() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    Long id = service.recordFieldChangeDraftSave(draft()).getSignatureId();
                    service.attachFieldChangeSignature(binding(id));
                    assertEquals(2, count("gxp_audit_event"));
                    assertEquals(7001L, signatures.selectById(id).getAuditBatchId());
                    throw new IllegalStateException("parent field-save failure");
                }));
        assertEquals(0, count(TABLE));
        assertAuditRollback();
    }

    @Test
    void invalidPasswordCannotWriteProjectionOrAudit() {
        assertThrows(RuntimeException.class, () -> service.recordProductionSubmitSignature(
                8801L, "wrong", "submit", productionContext()));
        assertEquals(0, count(TABLE));
        assertAuditRollback();
    }

    @Test
    void mismatchingExecutionCannotRebindProjection() {
        Long id = seedDraft();
        JSONObject before = snapshot(id);
        assertThrows(RuntimeException.class, () -> service.attachFieldChangeSignature(binding(id).setExecutionId(222L)));
        assertEquals(before, snapshot(id));
        assertAuditRollback();
    }

    private MesProductionSubmitSignatureContext productionContext() {
        return new MesProductionSubmitSignatureContext(409L, 520L, 985L, "production-submit-1");
    }

    private Long seedDraft() {
        var row = MesProBatchRecordExecutionSignatureDO.builder().executionId(101L).actorId(99L)
                .actorName("Device operator").actionType("FIELD_CHANGE").signatureMode("DRAFT_SESSION")
                .passwordVerified(false).signatureChallengeHash("challenge-before")
                .signedAt(LocalDateTime.of(2026, 9, 29, 1, 2, 3)).comment("existing reason").build();
        signatures.insert(row);
        return row.getId();
    }

    private MesProBatchRecordExecutionFieldAuditSignatureCommand draft() {
        return new MesProBatchRecordExecutionFieldAuditSignatureCommand().setExecutionId(101L)
                .setReasonCategory("CORRECTION").setReasonText("Observed correction")
                .setSignatureChallengeHash("challenge-before");
    }

    private MesProBatchRecordExecutionFieldAuditSignatureAttachCommand binding(Long id) {
        return new MesProBatchRecordExecutionFieldAuditSignatureAttachCommand().setSignatureId(id)
                .setExecutionId(101L).setAuditBatchId(7001L).setSignatureChallengeHash("challenge-after")
                .setFieldAuditRevision(2L).setFieldAuditHeadHash("head-after").setCellValuesHash("cells-after");
    }

    private JSONObject snapshot(Long id) { return JSON.parseObject(JsonUtils.toJsonString(signatures.selectById(id))); }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private JSONObject event(String operation) {
        return new JSONObject(jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE operation_id=?", operation));
    }
    private void assertAuditRollback() {
        assertEquals(0, count("gxp_audit_event"));
        assertEquals(0, count("gxp_audit_event_relation"));
        assertEquals(1L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence", Long.class));
    }
    private void installFailureTrigger() {
        jdbc.execute("CREATE TRIGGER fail_signature_relation BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + FailRelation.class.getName() + "'");
    }
    public static class FailRelation implements Trigger {
        @Override
        public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT (SELECT COUNT(*) FROM " + TABLE + "),(SELECT COUNT(*) FROM gxp_audit_event),"
                            + "(SELECT COUNT(*) FROM gxp_audit_event WHERE operation_id='" + BIND + "'),"
                            + "(SELECT COUNT(*) FROM " + TABLE + " WHERE audit_batch_id=7001"
                            + " AND signature_challenge_hash='challenge-after' AND field_audit_revision=2"
                            + " AND field_audit_head_hash='head-after' AND cell_values_hash='cells-after')")) {
                rows.next();
                FAILURE_SAW_WRITES.set(rows.getInt(1) > 0 && rows.getInt(2) > 0
                        && (rows.getInt(3) == 0 || rows.getInt(4) == 1));
            }
            throw new SQLException("Injected projection audit relation failure");
        }
    }
    private Object transactional(Object target) {
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }
    private void createTable(Class<?> row) {
        List<String> columns = new ArrayList<>();
        for (Class<?> type = row; type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                var mapping = field.getAnnotation(TableField.class);
                if (Modifier.isStatic(field.getModifiers()) || (mapping != null && !mapping.exist())) continue;
                String name = mapping != null && !mapping.value().isBlank() ? mapping.value()
                        : field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
                String sqlType = field.getType() == Long.class ? "BIGINT" : field.getType() == Integer.class ? "INT"
                        : field.getType() == Boolean.class ? "INT DEFAULT 0"
                        : field.getType() == LocalDateTime.class ? "TIMESTAMP" : "VARCHAR(1000000)";
                columns.add("`" + name + "` " + (name.equals("id") ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : sqlType));
            }
        }
        jdbc.execute("CREATE TABLE " + row.getAnnotation(TableName.class).value() + "(" + String.join(",", columns) + ")");
    }
}
