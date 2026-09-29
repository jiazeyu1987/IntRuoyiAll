package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationServiceImpl;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApiImpl;
import cn.iocoder.yudao.module.system.dal.mysql.dept.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.dept.*;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Actual CONCESSION dispose transaction. Formal precommitted fixtures are owned/provisioned by Dalton.
 * No DDL, business mocks, fallback datasource, outer test rollback, or assertion-skipping assumptions.
 * REWORK/manager cancellation are deliberately separate checkpoints, not covered by these two tests.
 */
@org.junit.jupiter.api.parallel.Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
class MesNcrDispositionMysqlTransactionTest {
    private static final String CONTAINER = "gxp-integration-mysql-round2-dalton-jdbc";
    private static final String OPERATION = "mes.nonconformance.concession";
    private static DataSource dataSource;
    private static JdbcTemplate jdbc;
    private static SqlSessionTemplate session;
    private static DataSourceTransactionManager transactions;
    private static MesProEdhrNonconformanceReviewServiceImpl service;
    private static ElectronicSignatureQueryServiceImpl signatures;
    private static boolean failAtTail;
    private static boolean observedRealTailWrites;
    private static Fixture current;
    private static final TailFailure TAIL_FAILURE = new TailFailure();
    private static final List<Class<?>> MAPPERS = List.of(
            MesProEdhrNonconformanceReviewMapper.class, MesProWorkOrderMapper.class,
            MesProcessPoolActiveOrderMapper.class, MesProcessPoolActiveOrderReleaseApplicationMapper.class,
            MesProEdhrOperationAuditEventMapper.class, FileMapper.class,
            ElectronicSignatureRecordMapper.class, AdminUserMapper.class,
            DeptMapper.class, PostMapper.class, RoleMapper.class, UserRoleMapper.class,
            DccElectronicSignatureAuthorizationMapper.class,
            GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
            GxpAuditPolicyOperationMapper.class, GxpAuditPolicyActivationMapper.class);
    private static final List<String> TENANT_TABLES = List.of(
            "mes_pro_edhr_nonconformance_review", "mes_pro_work_order", "mes_pro_process_pool_active_order",
            "mes_pro_process_pool_active_order_release_application", "mes_pro_edhr_operation_audit_event",
            "system_electronic_signature", "gxp_audit_event", "gxp_audit_event_relation",
            "gxp_audit_ledger_sequence", "gxp_audit_policy_operation", "gxp_audit_policy_activation");

    @BeforeAll
    static void assembleRealServices() throws Exception {
        assertEquals("gxp-integration-dalton-m9-round2", docker("inspect", "--format", "{{.Config.Labels.owner}}", CONTAINER));
        assertEquals("127.0.0.1:59241", docker("port", CONTAINER, "3306/tcp"));
        dataSource = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:59241/gxp_writer_snapshot"
                + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000",
                "root", docker("exec", CONTAINER, "cat", "/run/m9/root-password"));
        jdbc = new JdbcTemplate(dataSource);
        assertEquals("gxp_writer_snapshot", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertTrue(jdbc.queryForObject("SELECT VERSION()", String.class).startsWith("8."));
        transactions = new DataSourceTransactionManager(dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var plugins = new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        plugins.addInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(
                new cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor(
                        new cn.iocoder.yudao.framework.tenant.config.TenantProperties())));
        configuration.addInterceptor(plugins);
        GlobalConfig global = new GlobalConfig();
        global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
        global.setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler());
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(global);
        factory.setTransactionFactory(new SpringManagedTransactionFactory());
        SqlSessionFactory sql = Objects.requireNonNull(factory.getObject());
        for (Class<?> mapper : MAPPERS) sql.getConfiguration().addMapper(mapper);
        session = new SqlSessionTemplate(sql);
        preflightSchema();

        var writer = new GxpAuditServiceImpl();
        inject(writer, "auditEventMapper", mapper(GxpAuditEventMapper.class));
        inject(writer, "eventRelationMapper", mapper(GxpAuditEventRelationMapper.class));
        inject(writer, "ledgerSequenceMapper", mapper(GxpAuditLedgerSequenceMapper.class));
        inject(writer, "policyOperationMapper", mapper(GxpAuditPolicyOperationMapper.class));
        inject(writer, "policyActivationMapper", mapper(GxpAuditPolicyActivationMapper.class));
        GxpAuditService transactionalWriter = proxy(writer);
        ProxyFactory faultBoundary = new ProxyFactory(transactionalWriter);
        faultBoundary.addAdvice((MethodInterceptor) call -> {
            Object result = call.proceed(); // Always execute the actual writer first, including event/relation/ledger.
            if (call.getMethod().getName().equals("append")
                    && OPERATION.equals(((GxpAuditCommand) call.getArguments()[0]).getOperationId())) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertWrittenInsideTransaction(current);
                observedRealTailWrites = true;
                if (failAtTail) throw TAIL_FAILURE;
            }
            return result;
        });
        GxpAuditService audit = (GxpAuditService) faultBoundary.getProxy();

        var users = new AdminUserServiceImpl();
        inject(users, "userMapper", mapper(AdminUserMapper.class));
        inject(users, "passwordEncoder", new BCryptPasswordEncoder());
        var userApi = new AdminUserApiImpl();
        inject(userApi, "userService", proxy(users));
        var dept = new DeptServiceImpl(); inject(dept, "deptMapper", mapper(DeptMapper.class));
        var posts = new PostServiceImpl(); inject(posts, "postMapper", mapper(PostMapper.class));
        var roles = new RoleServiceImpl(); inject(roles, "roleMapper", mapper(RoleMapper.class));
        var permissions = new PermissionServiceImpl(); inject(permissions, "userRoleMapper", mapper(UserRoleMapper.class));
        var authorization = new DccElectronicSignatureAuthorizationServiceImpl();
        inject(authorization, "authorizationMapper", mapper(DccElectronicSignatureAuthorizationMapper.class));
        var adapter = new MesBatchRecordSignatureSubjectAdapter();
        var sign = new ElectronicSignatureServiceImpl();
        inject(sign, "adminUserApi", userApi);
        inject(sign, "signatureRecordMapper", mapper(ElectronicSignatureRecordMapper.class));
        inject(sign, "gxpAuditService", audit);
        inject(sign, "subjectAdapters", List.of(adapter));
        signatures = new ElectronicSignatureQueryServiceImpl();
        inject(signatures, "signatureRecordMapper", mapper(ElectronicSignatureRecordMapper.class));
        inject(signatures, "subjectAdapters", List.of(adapter));
        var mesSign = new MesProBatchRecordExecutionSignatureService();
        inject(mesSign, "adminUserService", proxy(users));
        inject(mesSign, "adminUserApi", userApi);
        inject(mesSign, "authorizationService", authorization);
        inject(mesSign, "deptService", dept);
        inject(mesSign, "postService", posts);
        inject(mesSign, "roleService", roles);
        inject(mesSign, "permissionService", permissions);
        inject(mesSign, "electronicSignatureService", proxy(sign));
        var specialized = new MesProEdhrOperationAuditServiceImpl();
        inject(specialized, "auditEventMapper", mapper(MesProEdhrOperationAuditEventMapper.class));
        var ncr = new MesProEdhrNonconformanceReviewServiceImpl();
        inject(ncr, "reviewMapper", mapper(MesProEdhrNonconformanceReviewMapper.class));
        inject(ncr, "workOrderMapper", mapper(MesProWorkOrderMapper.class));
        inject(ncr, "activeOrderMapper", mapper(MesProcessPoolActiveOrderMapper.class));
        inject(ncr, "releaseApplicationMapper", mapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class));
        inject(ncr, "fileMapper", mapper(FileMapper.class));
        inject(ncr, "signatureService", proxy(mesSign));
        inject(ncr, "signatureRecordMapper", mapper(ElectronicSignatureRecordMapper.class));
        inject(ncr, "signatureQueryService", signatures);
        inject(ncr, "operationAuditService", proxy(specialized));
        inject(ncr, "unifiedAudit", audit);
        inject(ncr, "auditReceiptMapper", mapper(GxpAuditEventMapper.class));
        service = proxy(ncr);
        assertTrue(AopUtils.isAopProxy(service));
        assertTrue(AopUtils.isAopProxy(transactionalWriter));
    }

    @Test
    void legalConcessionCommitsRealBusinessSignatureSpecializedAndUnifiedAudit() {
        exercise("commit", false);
    }

    @Test
    void failureAfterRealFinalAppendRestoresAllRowsRelationsAndLedgerHead() {
        exercise("rollback", true);
    }

    private void exercise(String caseName, boolean failure) {
        current = Fixture.read(caseName);
        assertNotEquals(Fixture.read("commit").tenant(), Fixture.read("rollback").tenant(),
                "PRECONDITION: commit and rollback require different task-owned tenants");
        failAtTail = failure;
        observedRealTailWrites = false;
        LoginUser actor = new LoginUser();
        actor.setId(current.actor()); actor.setTenantId(current.tenant()); actor.setUserType(2);
        actor.setInfo(Map.of("username", "rawls-ncr-r03-" + caseName, LoginUser.INFO_KEY_NICKNAME, "NCR R03"));
        TenantContextHolder.setTenantId(current.tenant());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
        try {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "No enclosing rollback test transaction");
            preflightFacts(current);
            Map<String, List<Map<String, Object>>> before = snapshot(current);
            var file = mapper(FileMapper.class).selectById(current.file());
            String password = System.getenv("NCR_R03_SIGNATURE_PASSWORD");
            assertNotNull(password, "PRECONDITION: NCR_R03_SIGNATURE_PASSWORD environment variable required (never print)");
            assertFalse(password.isBlank(), "PRECONDITION: signature credential missing");
            var request = new MesProEdhrNonconformanceReviewDisposeReqVO().setId(current.review())
                    .setDisposition("concession_release").setReviewOpinion("NCR R03 dedicated fixture " + caseName)
                    .setSignaturePassword(password).setReviewMaterials(List.of(
                            new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO()
                                    .setUrl("/admin-api/infra/file/" + file.getConfigId() + "/get/" + file.getPath())
                                    .setFileName(file.getName()).setSortNo(1)));
            if (failure) {
                assertSame(TAIL_FAILURE, assertThrows(TailFailure.class, () -> service.dispose(request)),
                        "Only the injected post-write failure is transaction evidence; earlier setup/signature failures are not");
                assertTrue(observedRealTailWrites, "Must observe real writes before the injected failure");
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                assertEquals(before, snapshot(current), "Fresh connections must see exact pre-transaction rows, relations and ledger");
            } else {
                assertEquals(current.review(), service.dispose(request).getId());
                assertTrue(observedRealTailWrites);
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                assertWrittenInsideTransaction(current); // Outside transaction: reads use new connections after actual commit.
                assertNotEquals(before, snapshot(current));
            }
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
            current = null;
            failAtTail = false;
        }
    }

    private static void preflightFacts(Fixture f) {
        var review = mapper(MesProEdhrNonconformanceReviewMapper.class).selectById(f.review());
        assertNotNull(review, "PRECONDITION: precommitted task-owned NCR missing");
        assertEquals("pending_review", review.getReviewStatus());
        assertEquals("ACTIVE_ORDER", review.getSourceType());
        assertEquals(f.activeOrder(), review.getActiveOrderId());
        assertEquals(f.workOrder(), review.getWorkOrderId());
        assertNull(review.getBatchExecutionId(), "This checkpoint is explicitly the legal no-batch CONCESSION branch");
        assertEquals(Boolean.FALSE, review.getPreviousWorkOrderTemporaryFrozen());
        assertNotNull(review.getFrozenAt());
        assertEquals(1L, count("mes_pro_edhr_nonconformance_review", f.tenant()), "Isolated tenant: exactly one review");
        assertEquals(0L, count("mes_pro_process_pool_active_order_release_application", f.tenant()),
                "Application/manager/REWORK branches are not this checkpoint");
        assertEquals(Boolean.TRUE, mapper(MesProWorkOrderMapper.class).selectById(f.workOrder()).getTemporaryFrozen());
        var order = mapper(MesProcessPoolActiveOrderMapper.class).selectById(f.activeOrder());
        assertNotNull(order); assertEquals(f.workOrder(), order.getWorkOrderId());
        var actor = mapper(AdminUserMapper.class).selectById(f.actor());
        assertNotNull(actor); assertEquals(f.tenant(), actor.getTenantId());
        assertNotNull(actor.getDeptId(), "PRECONDITION: formal organization fixture required");
        assertNotNull(mapper(DeptMapper.class).selectById(actor.getDeptId()), "PRECONDITION: actor department missing");
        assertNotNull(actor.getPostIds()); assertFalse(actor.getPostIds().isEmpty());
        for (Long postId : actor.getPostIds()) {
            assertNotNull(mapper(PostMapper.class).selectById(postId), "PRECONDITION: actor post missing");
        }
        var assignments = mapper(UserRoleMapper.class).selectListByUserId(f.actor());
        assertFalse(assignments.isEmpty());
        for (var assignment : assignments) {
            assertNotNull(mapper(RoleMapper.class).selectById(assignment.getRoleId()), "PRECONDITION: actor role missing");
        }
        assertNotNull(mapper(FileMapper.class).selectById(f.file()), "PRECONDITION: formal material metadata required");
        for (String table : List.of("system_electronic_signature", "mes_pro_edhr_operation_audit_event",
                "gxp_audit_event", "gxp_audit_event_relation")) assertEquals(0L, count(table, f.tenant()), table);
        assertEquals(1L, count("gxp_audit_ledger_sequence", f.tenant()));
        assertEquals(1L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=?",
                Long.class, f.tenant()));
        assertEquals(1L, count("gxp_audit_policy_activation", f.tenant()));
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=? "
                + "AND operation_id IN ('signature.record.create','mes.nonconformance.concession') AND active=1 AND deleted=0",
                Long.class, f.tenant()), "PRECONDITION: both real writer operation policies required");
    }

    private static void assertWrittenInsideTransaction(Fixture f) {
        assertEquals("closed", jdbc.queryForObject("SELECT review_status FROM mes_pro_edhr_nonconformance_review WHERE tenant_id=? AND id=?",
                String.class, f.tenant(), f.review()));
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT temporary_frozen FROM mes_pro_work_order WHERE tenant_id=? AND id=?",
                Boolean.class, f.tenant(), f.workOrder()));
        assertEquals(1L, count("system_electronic_signature", f.tenant()));
        assertEquals(1L, count("mes_pro_edhr_operation_audit_event", f.tenant()));
        assertEquals(2L, count("gxp_audit_event", f.tenant()), "Signature and NCR events are distinct");
        var events = jdbc.queryForList("SELECT id,operation_id,signature_record_id,event_hash,previous_event_hash,ledger_sequence "
                + "FROM gxp_audit_event WHERE tenant_id=? ORDER BY ledger_sequence", f.tenant());
        assertEquals("signature.record.create", events.get(0).get("operation_id"));
        assertEquals(OPERATION, events.get(1).get("operation_id"));
        assertEquals(events.get(0).get("event_hash"), events.get(1).get("previous_event_hash"));
        var canonicalEvents = jdbc.queryForList("SELECT canonical_event_json,event_hash FROM gxp_audit_event "
                + "WHERE tenant_id=? ORDER BY ledger_sequence", f.tenant());
        for (var event : canonicalEvents) {
            assertEquals(event.get("event_hash"), cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                    event.get("canonical_event_json").toString()));
        }
        assertEquals(3L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=?",
                Long.class, f.tenant()));
        assertTrue(count("gxp_audit_event_relation", f.tenant()) > 0);
        Long signatureId = jdbc.queryForObject("SELECT id FROM system_electronic_signature WHERE tenant_id=?", Long.class, f.tenant());
        assertEquals(signatureId.toString(), events.get(1).get("signature_record_id"));
        assertEquals("VALID", signatures.verifyEvidence(signatureId).verificationStatus());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event_relation WHERE tenant_id=? "
                + "AND event_id=? AND relation_type='SUBJECT' AND target_type='NONCONFORMANCE_REVIEW' AND target_id=?", Long.class,
                f.tenant(), events.get(1).get("id"), f.review().toString()));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event_relation WHERE tenant_id=? "
                + "AND event_id=? AND relation_type='SIGNATURE' AND target_type='ELECTRONIC_SIGNATURE' AND target_id=?", Long.class,
                f.tenant(), events.get(1).get("id"), signatureId.toString()));
    }

    private static Map<String, List<Map<String, Object>>> snapshot(Fixture f) {
        Map<String, List<Map<String, Object>>> result = new TreeMap<>();
        for (String table : TENANT_TABLES) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=? ORDER BY "
                    + (table.equals("gxp_audit_ledger_sequence") ? "tenant_id" : "id"), f.tenant()));
        }
        // Never include credential/password columns in assertion diagnostics.
        result.put("actor-login-state", jdbc.queryForList("SELECT id,login_failure_count,login_failure_window_start_time,"
                + "login_locked,login_locked_time,update_time FROM system_users WHERE tenant_id=? AND id=?", f.tenant(), f.actor()));
        return result;
    }

    private static void preflightSchema() {
        for (Class<?> type : MAPPERS) {
            // MyBatis has registered the real DO TableInfo; validate every mapped column, not only asserted columns.
            Class<?> entity = org.springframework.core.ResolvableType.forClass(type)
                    .as(com.baomidou.mybatisplus.core.mapper.BaseMapper.class).getGeneric(0).resolve();
            assertNotNull(entity, "PRECONDITION: mapper entity unresolved: " + type.getName());
            var info = TableInfoHelper.getTableInfo(entity);
            assertNotNull(info, "PRECONDITION: TableInfo missing: " + entity.getName());
            assertEquals("InnoDB", jdbc.queryForObject("SELECT ENGINE FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?", String.class, info.getTableName()),
                    "PRECONDITION: missing formal InnoDB table " + info.getTableName());
            Set<String> actual = new HashSet<>(jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?", String.class, info.getTableName()));
            List<String> required = new ArrayList<>(); required.add(info.getKeyColumn());
            info.getFieldList().forEach(field -> required.add(field.getColumn()));
            for (String column : required) assertTrue(actual.contains(column.replace("`", "")),
                    "PRECONDITION: formal DDL missing mapped column " + info.getTableName() + "." + column);
        }
        assertEquals(12L, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TRIGGERS "
                + "WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE LIKE 'gxp_audit_%'", Long.class));
        assertEquals("json", jdbc.queryForObject("SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='system_electronic_signature' AND COLUMN_NAME='canonical_content_json'", String.class));
    }

    private static long count(String table, long tenant) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=?", Long.class, tenant));
    }
    private static <T> T mapper(Class<T> type) { return session.getMapper(type); }
    private static void inject(Object target, String field, Object value) { ReflectionTestUtils.setField(target, field, value); }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(T target) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }
    private record Fixture(Long tenant, Long actor, Long review, Long workOrder, Long activeOrder, Long file) {
        static Fixture read(String name) {
            return new Fixture(id(name, "tenant"), id(name, "actor"), id(name, "review"),
                    id(name, "workOrder"), id(name, "activeOrder"), id(name, "file"));
        }
        private static Long id(String name, String field) {
            String key = "ncr.r03." + name + "." + field;
            String value = System.getProperty(key);
            assertNotNull(value, "PRECONDITION: Dalton-owned precommitted fixture property required: " + key);
            assertTrue(value.matches("[1-9][0-9]*"), "PRECONDITION: positive fixture identity required: " + key);
            return Long.valueOf(value);
        }
    }
    private static class TailFailure extends RuntimeException {
        TailFailure() { super("NCR_R03_INJECTED_AFTER_REAL_APPEND"); }
    }
    private static String docker(String... args) throws Exception {
        List<String> command = new ArrayList<>(); command.add("docker"); command.addAll(List.of(args));
        Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!p.waitFor(10, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new IllegalStateException("Fixture preflight timed out"); }
        assertEquals(0, p.exitValue(), "Fixture container preflight failed; credential output withheld");
        return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
    }
}
