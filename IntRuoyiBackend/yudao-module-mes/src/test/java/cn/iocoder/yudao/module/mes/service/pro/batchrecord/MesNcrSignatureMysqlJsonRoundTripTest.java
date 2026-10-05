package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.*;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Narrow JSON persistence contract, not an end-to-end dispose/credential authorization test. */
class MesNcrSignatureMysqlJsonRoundTripTest {
    private static final String CONTAINER = "gxp-integration-mysql-round2-dalton-jdbc";
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static final long ACTOR = 21L;
    private static final long REVIEW = 970049L;
    private static final String REASON = "让步处置 JSON 往返";
    private static final String AGGREGATE = DigestUtil.sha256Hex("ncr-json-roundtrip-business-payload");

    @BeforeAll
    static void openDedicatedFixture() throws Exception {
        assertEquals("gxp-integration-dalton-m9-round2",
                docker("inspect", "--format", "{{.Config.Labels.owner}}", CONTAINER));
        String fixtureEndpoint = docker("port", CONTAINER, "3306/tcp");
        assertTrue(fixtureEndpoint.matches("127\\.0\\.0\\.1:[1-9][0-9]{3,4}"),
                "The verified fixture container must expose exactly one loopback IPv4 endpoint");
        String credential = docker("exec", CONTAINER, "cat", "/run/m9/root-password");
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:mysql://" + fixtureEndpoint + "/gxp_writer_snapshot"
                        + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000",
                "root", credential);
        jdbc = new JdbcTemplate(dataSource);
        assertEquals("gxp_writer_snapshot", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertTrue(jdbc.queryForObject("SELECT VERSION()", String.class).startsWith("8."));
        for (String table : List.of("system_electronic_signature", "gxp_audit_event", "gxp_audit_event_relation",
                "gxp_audit_ledger_sequence", "gxp_audit_policy_operation", "gxp_audit_policy_activation")) {
            assertEquals("InnoDB", jdbc.queryForObject("SELECT ENGINE FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?", String.class, table),
                    "Missing formal InnoDB schema: " + table);
        }
        assertEquals("json", jdbc.queryForObject("SELECT DATA_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_electronic_signature' "
                + "AND COLUMN_NAME='canonical_content_json'", String.class));
        assertEquals(2048L, jdbc.queryForObject("SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_electronic_signature' "
                + "AND COLUMN_NAME='subject_id'", Long.class));
        assertEquals(12L, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TRIGGERS "
                + "WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE LIKE 'gxp_audit_%'", Long.class));
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> dataSource);
        context.register(FixtureConfiguration.class);
        context.refresh();
        assertTrue(AopUtils.isAopProxy(context.getBean(ElectronicSignatureService.class)));
        assertTrue(AopUtils.isAopProxy(context.getBean(GxpAuditService.class)));
    }

    @AfterAll
    static void closeFixture() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void realSignatureAndQueryVerifierSurviveNativeJsonRoundTrip() {
        exercise(false);
    }

    @Test
    void ncrAcceptsTheSameValidSignatureAfterNativeJsonRoundTrip() {
        exercise(true);
    }

    private void exercise(boolean checkNcrConsumer) {
        long tenant = 2_000_000_000_000L + (UUID.randomUUID().getMostSignificantBits() & 0x1ffffffffffffL);
        LoginUser actor = new LoginUser();
        actor.setId(ACTOR);
        actor.setTenantId(tenant);
        actor.setUserType(2);
        actor.setInfo(Map.of("username", "ncr-json-fixture", LoginUser.INFO_KEY_NICKNAME, "NCR JSON fixture"));
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, List.of()));
        AdminUserApi authentication = context.getBean(AdminUserApi.class);
        reset(authentication);
        // Explicit boundary: this test verifies persisted signature evidence, not a real user's password.
        doAnswer(call -> {
            assertEquals(ACTOR, (Long) call.getArgument(0));
            assertEquals("TEST-ONLY-NOT-A-REAL-PASSWORD", call.getArgument(1));
            return null;
        }).when(authentication).reauthenticateForSignature(ACTOR, "TEST-ONLY-NOT-A-REAL-PASSWORD");
        try {
            TransactionTemplate tx = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
            tx.executeWithoutResult(status -> {
                status.setRollbackOnly(); // Every fixture row and event is task-owned and rolled back, even on PASS.
                seedAuditFacts(tenant);
                String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "QA_DISPOSITION",
                        null, null, null, null, null, null, null, "EDHR_NONCONFORMANCE_REVIEW", REVIEW,
                        "eDHR不合格评审处置", "QA_DISPOSITION", null, null, AGGREGATE, null);
                var result = context.getBean(ElectronicSignatureService.class).sign(new ElectronicSignatureCommand(
                        "MES", "QA_DISPOSITION", "MES_BATCH_RECORD", subject,
                        MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject),
                        "TEST-ONLY-NOT-A-REAL-PASSWORD", REASON, "ncr-json:" + UUID.randomUUID(), null, null));
                verify(authentication).reauthenticateForSignature(ACTOR, "TEST-ONLY-NOT-A-REAL-PASSWORD");
                // Clear MyBatis local cache: the evidence must come from MySQL, not the inserted Java object.
                context.getBean(SqlSessionTemplate.class).clearCache();
                var mapper = context.getBean(ElectronicSignatureRecordMapper.class);
                ElectronicSignatureRecordDO persisted = mapper.selectById(result.signatureId());
                assertNotNull(persisted);
                assertEquals(tenant, persisted.getTenantId());
                assertEquals(result.contentHash(), persisted.getContentHash());
                assertNotNull(persisted.getCreateTime());
                assertEquals("21", persisted.getCreator());
                String databaseJson = jdbc.queryForObject("SELECT canonical_content_json "
                        + "FROM system_electronic_signature WHERE id=? AND tenant_id=?",
                        String.class, result.signatureId(), tenant);
                assertEquals(databaseJson, persisted.getCanonicalContentJson());
                var query = context.getBean(ElectronicSignatureQueryService.class);
                assertEquals(result.contentHash(), query.getById(result.signatureId()).contentHash());
                var verified = query.verifyEvidence(result.signatureId());
                assertEquals("VALID", verified.verificationStatus());
                assertEquals(verified.storedContentHash(), verified.calculatedContentHash());
                assertEquals(verified.storedEvidenceHash(), verified.calculatedEvidenceHash());
                assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event "
                        + "WHERE tenant_id=? AND operation_id='signature.record.create'", Long.class, tenant));
                if (checkNcrConsumer) {
                    // Invoke the frozen production boundary itself; do not duplicate its hash algorithm in a test.
                    var ncr = new MesProEdhrNonconformanceReviewServiceImpl();
                    { org.springframework.test.util.ReflectionTestUtils.setField(ncr, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
                    ReflectionTestUtils.setField(ncr, "signatureRecordMapper", mapper);
                    ReflectionTestUtils.setField(ncr, "signatureQueryService", query);
                    ElectronicSignatureRecordDO accepted = assertDoesNotThrow(() ->
                            ReflectionTestUtils.invokeMethod(ncr, "requireDispositionAuditSignature",
                                    result.signatureId(), ACTOR, REVIEW, REASON, AGGREGATE),
                            "A formally valid signature must remain acceptable after native MySQL JSON storage");
                    assertNotNull(accepted);
                    assertEquals(result.signatureId(), accepted.getId());
                    assertEquals(result.contentHash(), accepted.getContentHash());
                }
            });
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
            // New connections after transaction completion verify this test leaves no committed fixture facts.
            for (String table : List.of("system_electronic_signature", "gxp_audit_event",
                    "gxp_audit_event_relation", "gxp_audit_policy_activation", "gxp_audit_policy_operation",
                    "gxp_audit_ledger_sequence")) {
                assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=?",
                        Long.class, tenant), "Unexpected committed fixture data: " + table);
            }
        }
    }

    private static void seedAuditFacts(long tenant) {
        GxpAuditLedgerSequenceDO ledger = new GxpAuditLedgerSequenceDO();
        ledger.setTenantId(tenant);
        ledger.setNextLedgerSequence(1L);
        assertEquals(1, context.getBean(GxpAuditLedgerSequenceMapper.class).insert(ledger));
        GxpAuditPolicyActivationDO activation = new GxpAuditPolicyActivationDO();
        activation.setTenantId(tenant);
        activation.setPolicyVersion("ncr-json-test-v1");
        activation.setPolicyHash(DigestUtil.sha256Hex("ncr-json-test-v1"));
        activation.setRequestId(UUID.randomUUID().toString());
        activation.setActorId(ACTOR);
        activation.setApprovalReference("TEST-ONLY-NOT-PRODUCTION-APPROVAL");
        activation.setActivatedAtUtc(LocalDateTime.now(java.time.ZoneOffset.UTC));
        activation.setEffectiveAfterSequence(0L);
        activation.setCanonicalActivationJson("{\"fixture\":true}");
        activation.setActivationHash(DigestUtil.sha256Hex("ncr-json:" + tenant));
        assertEquals(1, context.getBean(GxpAuditPolicyActivationMapper.class).insert(activation));
        GxpAuditPolicyOperationDO policy = new GxpAuditPolicyOperationDO();
        policy.setTenantId(tenant);
        policy.setPolicyVersion("ncr-json-test-v1");
        policy.setOperationId("signature.record.create");
        policy.setSourceType("SERVICE_METHOD");
        policy.setSourceLocator("ElectronicSignatureServiceImpl.sign");
        policy.setDomain("TEST");
        policy.setSubjectType("MES_BATCH_RECORD");
        policy.setActionType("CREATE");
        policy.setReasonPolicy("REQUIRED_CATEGORY_AND_TEXT");
        policy.setSignaturePolicy("REQUIRED");
        policy.setStatePolicy("ABSENT_TO_PRESENT");
        policy.setRetentionClass("GXP_MASTER_DATA");
        policy.setTestIds("MesNcrSignatureMysqlJsonRoundTripTest");
        policy.setOwner("Rawls-test-fixture");
        policy.setApplicability("GXP");
        policy.setActive(true);
        policy.setDeleted(false);
        assertEquals(1, context.getBean(GxpAuditPolicyOperationMapper.class).insert(policy));
    }

    private static String docker(String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Dedicated fixture prerequisite timed out");
        }
        assertEquals(0, process.exitValue(), "Dedicated fixture prerequisite failed; output is withheld");
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class FixtureConfiguration {
        @Bean DataSourceTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean AdminUserApi adminUserApi() { return mock(AdminUserApi.class); }
        @Bean ElectronicSignatureSubjectAdapter subjectAdapter() { return new MesBatchRecordSignatureSubjectAdapter(); }
        @Bean ElectronicSignatureService signatureService() { return new ElectronicSignatureServiceImpl(); }
        @Bean ElectronicSignatureQueryService signatureQueryService() { return new ElectronicSignatureQueryServiceImpl(); }
        @Bean GxpAuditService gxpAuditService() { return new GxpAuditServiceImpl(); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception {
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            GlobalConfig global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
            global.setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler());
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(ds);
            factory.setConfiguration(configuration);
            factory.setGlobalConfig(global);
            factory.setTransactionFactory(new SpringManagedTransactionFactory());
            SqlSessionFactory result = Objects.requireNonNull(factory.getObject());
            for (Class<?> mapper : List.of(ElectronicSignatureRecordMapper.class, GxpAuditEventMapper.class,
                    GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                    GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class)) {
                result.getConfiguration().addMapper(mapper);
            }
            return result;
        }
        @Bean SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory factory) { return new SqlSessionTemplate(factory); }
        @Bean ElectronicSignatureRecordMapper signatureRecordMapper(SqlSessionTemplate s) { return s.getMapper(ElectronicSignatureRecordMapper.class); }
        @Bean GxpAuditEventMapper auditEventMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditEventMapper.class); }
        @Bean GxpAuditEventRelationMapper eventRelationMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditEventRelationMapper.class); }
        @Bean GxpAuditLedgerSequenceMapper ledgerSequenceMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditLedgerSequenceMapper.class); }
        @Bean GxpAuditPolicyActivationMapper policyActivationMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditPolicyActivationMapper.class); }
        @Bean GxpAuditPolicyOperationMapper policyOperationMapper(SqlSessionTemplate s) { return s.getMapper(GxpAuditPolicyOperationMapper.class); }
    }
}
