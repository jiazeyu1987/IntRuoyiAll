package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.config.YudaoTenantAutoConfiguration;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** M1 checkpoint: real MySQL persistence/transactions, not yet the concurrent matrix. No DDL. */
class PermissionReceiptMysqlConcurrencyTest {
    enum Operation { ROLE_MENU, USER_ROLE, DATA_SCOPE }
    private static final String SCHEMA = "gxp_permission_receipt_round3";
    private static final String URL = "jdbc:mysql://127.0.0.1:59241/" + SCHEMA
            + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000";
    private static final AtomicLong IDS = new AtomicLong(
            1_000_000_000_000L + (UUID.randomUUID().getMostSignificantBits() & 0x1ffffffffffffL));
    private static final ObjectMapper JSON = new ObjectMapper();
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate db;
    private static PermissionServiceImpl service;
    private static final ThreadLocal<Observation> OBSERVATION = new ThreadLocal<>();
    private long tenant;
    private long subject;

    @BeforeAll
    static void connect() {
        assertEquals(SCHEMA, requiredEnv("GXP_PERMISSION_MYSQL_SCHEMA"));
        DriverManagerDataSource source = new DriverManagerDataSource(URL,
                requiredEnv("GXP_PERMISSION_MYSQL_USER"), requiredEnv("GXP_PERMISSION_MYSQL_PASSWORD"));
        db = new JdbcTemplate(source);
        assertEquals(SCHEMA, db.queryForObject("SELECT DATABASE()", String.class));
        assertTrue(db.queryForObject("SELECT VERSION()", String.class).startsWith("8."));
        for (String table : List.of("system_role", "system_users", "system_role_menu", "system_user_role",
                "system_gxp_command_receipt", "gxp_audit_event", "gxp_audit_event_relation",
                "gxp_audit_ledger_sequence", "gxp_audit_policy_activation", "gxp_audit_policy_operation")) {
            assertEquals("InnoDB", db.queryForObject("SELECT ENGINE FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA=? AND TABLE_NAME=?", String.class, SCHEMA, table));
        }
        for (String column : List.of("identity_json", "source_json", "result_json")) {
            assertEquals("json", db.queryForObject("SELECT DATA_TYPE FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA=? AND TABLE_NAME='system_gxp_command_receipt' AND COLUMN_NAME=?",
                    String.class, SCHEMA, column));
        }
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> source);
        context.register(Configuration.class);
        context.refresh();
        service = context.getBean(PermissionServiceImpl.class);
        assertTrue(AopUtils.isAopProxy(service));
        assertTrue(AopUtils.isAopProxy(context.getBean(GxpAuditService.class)));
        assertEquals(1, context.getBean(MybatisPlusInterceptor.class).getInterceptors().size());
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        assertNotNull(value, "Missing dedicated fixture input: " + name);
        assertFalse(value.isBlank(), "Blank dedicated fixture input: " + name);
        return value;
    }

    @BeforeEach
    void fixture() {
        tenant = IDS.incrementAndGet(); subject = IDS.incrementAndGet();
        LoginUser actor = new LoginUser(); actor.setId(1001L); actor.setTenantId(tenant);
        actor.setInfo(Map.of("username", "permission.mysql.fixture", LoginUser.INFO_KEY_NICKNAME, "Fixture"));
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        new TransactionTemplate(context.getBean(DataSourceTransactionManager.class)).executeWithoutResult(status -> {
            db.update("INSERT INTO system_role(id,name,code,sort,status,type,data_scope,tenant_id) "
                    + "VALUES(?, 'fixture', ?,1,0,2,1,?)", subject, "fixture-" + subject, tenant);
            db.update("INSERT INTO system_users(id,username,canonical_username,nickname,tenant_id) "
                    + "VALUES(?,?,?,'Fixture',?)", subject, "fixture-" + subject, "fixture-" + subject, tenant);
            GxpAuditLedgerSequenceDO watermark = new GxpAuditLedgerSequenceDO();
            watermark.setTenantId(tenant); watermark.setNextLedgerSequence(1L);
            assertEquals(1, context.getBean(GxpAuditLedgerSequenceMapper.class).insert(watermark));
            GxpAuditPolicyActivationDO active = new GxpAuditPolicyActivationDO();
            active.setTenantId(tenant); active.setPolicyVersion("permission-mysql-test-only");
            active.setPolicyHash("a".repeat(64)); active.setPreviousActivationHash("0".repeat(64));
            active.setRequestId(UUID.randomUUID().toString()); active.setActorId(1001L);
            active.setApprovalReference("ISOLATED-FIXTURE-NOT-APPROVAL");
            active.setActivatedAtUtc(LocalDateTime.of(2026, 1, 1, 0, 0)); active.setEffectiveAfterSequence(0L);
            active.setCanonicalActivationJson("{\"fixture\":true}"); active.setActivationHash("b".repeat(64));
            assertEquals(1, context.getBean(GxpAuditPolicyActivationMapper.class).insert(active));
            for (Operation op : Operation.values()) {
                GxpAuditPolicyOperationDO policy = new GxpAuditPolicyOperationDO();
                policy.setTenantId(tenant); policy.setPolicyVersion(active.getPolicyVersion());
                policy.setOperationId(operationId(op)); policy.setSourceType("SERVICE_METHOD");
                policy.setSourceLocator("PermissionServiceImpl"); policy.setDomain("SYSTEM");
                policy.setSubjectType(op == Operation.USER_ROLE ? "SYSTEM_USER" : "SYSTEM_ROLE");
                policy.setActionType("UPDATE"); policy.setReasonPolicy("USER_REQUIRED");
                policy.setSignaturePolicy("NONE"); policy.setStatePolicy("PRESENT_TO_PRESENT");
                policy.setRetentionClass("TEST_ONLY"); policy.setTestIds("PermissionReceiptMysqlConcurrencyTest");
                policy.setOwner("TEST_ONLY"); policy.setApplicability("GXP"); policy.setActive(true);
                assertEquals(1, context.getBean(GxpAuditPolicyOperationMapper.class).insert(policy));
            }
        });
    }

    @AfterEach
    void clearThread() {
        OBSERVATION.remove(); TenantContextHolder.clear(); SecurityContextHolder.clearContext();
        // Keep committed dedicated fixture facts for runner inspection; never delete audit history.
    }

    @AfterAll
    static void close() { if (context != null) context.close(); }

    private static String operationId(Operation op) {
        return "system.permission." + switch (op) {
            case ROLE_MENU -> "role-menu.assign";
            case USER_ROLE -> "user-role.assign";
            case DATA_SCOPE -> "role-data-scope.assign";
        };
    }

    private void invoke(Operation op) {
        switch (op) {
            case ROLE_MENU -> service.assignRoleMenu(subject, Set.of(2L), "reason", "mysql-owned-command");
            case USER_ROLE -> service.assignUserRole(subject, Set.of(2L), "reason", "mysql-owned-command");
            case DATA_SCOPE -> service.assignRoleDataScope(subject, 2, Set.of(2L), "reason", "mysql-owned-command");
        }
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void realJsonReceiptCommitsAndReplaysWithoutWrites(Operation op) throws Exception {
        Observation first = new Observation(); OBSERVATION.set(first);
        invoke(op);
        assertEquals(1, first.receiptInserts);
        assertEquals(1L, count("gxp_audit_event")); assertEquals(1L, count("system_gxp_command_receipt"));
        var receipt = db.queryForMap("SELECT * FROM system_gxp_command_receipt WHERE tenant_id=?", tenant);
        assertArrayEquals("mysql-owned-command".getBytes(StandardCharsets.UTF_8), (byte[]) receipt.get("source_key"));
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest((byte[]) receipt.get("source_key")),
                (byte[]) receipt.get("source_key_sha256"));
        var source = JSON.readTree(receipt.get("source_json").toString());
        assertEquals("mysql-owned-command", source.get("sourceKey").textValue());
        assertEquals("1001", source.get("actorId").textValue());
        assertEquals(JSON.readTree("{\"schemaVersion\":\"system.permission.result.v1\",\"kind\":\"VOID\"}"),
                JSON.readTree(receipt.get("result_json").toString()));
        String before = snapshot();
        Observation replay = new Observation(); OBSERVATION.set(replay);
        assertDoesNotThrow(() -> invoke(op));
        assertEquals(0, replay.writes); assertEquals(before, snapshot());
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void receiptBoundaryFaultRollsBackRealBusinessEventAndLedger(Operation op) throws Exception {
        String before = snapshot();
        RuntimeException injected = new IllegalStateException("MYSQL-M1-receipt-boundary-fault");
        Observation observation = new Observation(); observation.failure = injected;
        observation.atReceipt = () -> {
            assertEquals(1L, count("gxp_audit_event"));
            assertEquals(0L, count("system_gxp_command_receipt"));
            assertEquals(2L, db.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=?",
                    Long.class, tenant));
            if (op == Operation.DATA_SCOPE) {
                assertEquals(2, db.queryForObject("SELECT data_scope FROM system_role WHERE id=?", Integer.class, subject));
            } else assertEquals(1L, count(op == Operation.ROLE_MENU ? "system_role_menu" : "system_user_role"));
        };
        OBSERVATION.set(observation);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> invoke(op));
        boolean found = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) if (cause == injected) found = true;
        assertTrue(found, "must propagate the exact receipt-boundary fault");
        assertEquals(1, observation.receiptInserts);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(before, snapshot());
    }

    private long count(String table) {
        return db.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=?", Long.class, tenant);
    }

    private String snapshot() throws Exception {
        List<Object> tables = new ArrayList<>();
        for (String table : List.of("system_role", "system_users", "system_role_menu", "system_user_role",
                "system_gxp_command_receipt", "gxp_audit_event", "gxp_audit_event_relation", "gxp_audit_ledger_sequence")) {
            tables.add(db.query("SELECT * FROM " + table + " WHERE tenant_id=? ORDER BY "
                    + (table.equals("gxp_audit_ledger_sequence") ? "tenant_id" : "id"), (rs, row) -> {
                List<String> values = new ArrayList<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    Object value = rs.getObject(i);
                    values.add(value instanceof byte[] bytes ? HexFormat.of().formatHex(bytes) : rs.getString(i));
                }
                return values;
            }, tenant));
        }
        return JSON.writeValueAsString(tables);
    }

    static class Observation {
        int writes; int receiptInserts; Runnable atReceipt; RuntimeException failure;
    }

    @Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
    static class WriteObserver implements Interceptor {
        @Override public Object intercept(Invocation invocation) throws Throwable {
            Observation observed = OBSERVATION.get();
            if (observed != null) {
                observed.writes++;
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                var connection = ((Executor) invocation.getTarget()).getTransaction().getConnection();
                try (var statement = connection.prepareStatement("SELECT CONNECTION_ID()"); var result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(db.queryForObject("SELECT CONNECTION_ID()", Long.class).longValue(), result.getLong(1));
                }
                if (((MappedStatement) invocation.getArgs()[0]).getId().equals(PermissionCommandReceiptMapper.class.getName() + ".insert")) {
                    observed.receiptInserts++;
                    if (observed.atReceipt != null) observed.atReceipt.run();
                    if (observed.failure != null) throw observed.failure;
                }
            }
            return invocation.proceed();
        }
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    @Import({PermissionServiceImpl.class, PermissionCommandProtocol.class, GxpAuditServiceImpl.class})
    static class Configuration {
        @Bean DataSourceTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean MybatisPlusInterceptor tenantPlugin() {
            MybatisPlusInterceptor plugin = new MybatisPlusInterceptor();
            new YudaoTenantAutoConfiguration().tenantLineInnerInterceptor(new TenantProperties(), plugin);
            return plugin;
        }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source, MybatisPlusInterceptor plugin) throws Exception {
            MybatisConfiguration config = new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true);
            config.addInterceptor(plugin); config.addInterceptor(new WriteObserver());
            GlobalConfig global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
            global.setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler());
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source); factory.setConfiguration(config); factory.setGlobalConfig(global);
            factory.setTransactionFactory(new SpringManagedTransactionFactory());
            SqlSessionFactory result = factory.getObject(); assertNotNull(result);
            for (Class<?> mapper : List.of(GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class,
                    GxpAuditLedgerSequenceMapper.class, GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class,
                    PermissionCommandReceiptMapper.class, RoleMapper.class, RoleMenuMapper.class, UserRoleMapper.class, AdminUserMapper.class)) {
                result.getConfiguration().addMapper(mapper);
            }
            return result;
        }
        @Bean SqlSessionTemplate session(SqlSessionFactory factory) { return new SqlSessionTemplate(factory); }
        @Bean GxpAuditEventMapper auditEventMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditEventMapper.class); }
        @Bean GxpAuditEventRelationMapper eventRelationMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditEventRelationMapper.class); }
        @Bean GxpAuditLedgerSequenceMapper ledgerSequenceMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditLedgerSequenceMapper.class); }
        @Bean GxpAuditPolicyActivationMapper policyActivationMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditPolicyActivationMapper.class); }
        @Bean GxpAuditPolicyOperationMapper policyOperationMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditPolicyOperationMapper.class); }
        @Bean PermissionCommandReceiptMapper receipts(SqlSessionTemplate s) { return s.getMapper(PermissionCommandReceiptMapper.class); }
        @Bean RoleMapper roleMapper(SqlSessionTemplate s) { return s.getMapper(RoleMapper.class); }
        @Bean RoleMenuMapper roleMenuMapper(SqlSessionTemplate s) { return s.getMapper(RoleMenuMapper.class); }
        @Bean UserRoleMapper userRoleMapper(SqlSessionTemplate s) { return s.getMapper(UserRoleMapper.class); }
        @Bean AdminUserMapper adminUserMapper(SqlSessionTemplate s) { return s.getMapper(AdminUserMapper.class); }
        @Bean RoleService roleService(RoleMapper mapper) {
            RoleService roles = mock(RoleService.class);
            when(roles.getRole(anyLong())).thenAnswer(i -> mapper.selectById((Long) i.getArgument(0)));
            doAnswer(i -> {
                var role = mapper.selectById((Long) i.getArgument(0));
                role.setDataScope(i.getArgument(1)); role.setDataScopeDeptIds(i.getArgument(2));
                assertEquals(1, mapper.updateById(role)); return null;
            }).when(roles).updateRoleDataScope(anyLong(), anyInt(), anySet());
            return roles;
        }
        @Bean MenuService menuService() { return mock(MenuService.class); }
        @Bean DeptService deptService() { return mock(DeptService.class); }
        @Bean AdminUserService userService() { return mock(AdminUserService.class); }
        @Bean SystemEntitlementService systemEntitlementService() { return mock(SystemEntitlementService.class); }
        @Bean TemporaryRoleGrantService temporaryRoleGrantService() { return mock(TemporaryRoleGrantService.class); }
    }
}
