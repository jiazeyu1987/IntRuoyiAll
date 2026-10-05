package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.signature.api.*;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.JsonNode;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_APPEND_FAILED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Formal MySQL transaction contract. Requires Dalton-owned, already prepared formal fixtures.
 * Does not create tables, activate policy, start a database, or fake a business/signature/writer write.
 * Password reauthentication and external user/authorization directory are explicit boundary doubles;
 * this class is NOT evidence of real password, organization or DCC authorization verification.
 */
class MesPqcCorrectionMysqlTransactionTest {
    private static final String CONTAINER = "gxp-integration-mysql-round2-dalton-jdbc";
    private static final String PASSWORD = "CORRECTION-TRANSACTION-TEST-ONLY";
    private static final String OPERATION = "mes.pqc-inspection.correct";
    private static final List<Class<?>> MAPPERS = List.of(
            MesProProcessPoolEventMapper.class, MesProProcessPoolPqcRecordMapper.class,
            MesPqcInspectionTaskMapper.class, MesPqcInspectionPieceDetailMapper.class,
            MesProProcessPoolEventRevisionMapper.class, MesProProcessPoolEventRevisionDiffMapper.class,
            MesProcessPoolSubmissionReviewMapper.class, MesProcessPoolActiveOrderMapper.class,
            MesPqcProcessInspectionAggregateDetailMapper.class, MesProcessPoolTeamLeaderScopeMapper.class,
            MesProcessPoolActiveOrderReleaseApplicationMapper.class, MesProEdhrReleaseTransactionMapper.class,
            MesProEdhrNonconformanceReviewMapper.class, MesProWorkOrderMapper.class,
            MesProcessPoolFifoAllocationLineMapper.class, ElectronicSignatureRecordMapper.class,
            GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
            GxpAuditPolicyOperationMapper.class, GxpAuditPolicyActivationMapper.class);
    private static final List<String> WRITE_TABLES = List.of(
            "mes_pro_process_pool_event", "mes_pro_process_pool_event_revision",
            "mes_pro_process_pool_event_revision_diff", "mes_pro_process_pool_pqc_record",
            "mes_pqc_inspection_task", "mes_pqc_inspection_piece_detail",
            "mes_pro_process_pool_submission_review", "mes_pqc_process_inspection_aggregate_detail",
            "system_electronic_signature", "gxp_audit_event", "gxp_audit_event_relation",
            "gxp_audit_ledger_sequence");
    private static JdbcTemplate jdbc;
    private static SqlSessionTemplate sessions;
    private static DataSourceTransactionManager transactions;
    private static JsonNode fixtures;

    @BeforeAll
    static void openExistingDedicatedDatabase() throws Exception {
        // No assumptions/disabled tests: missing prerequisites are hard setup failures, never behavior RED.
        assertEquals("gxp-integration-dalton-m9-round2",
                docker("inspect", "--format", "{{.Config.Labels.owner}}", CONTAINER));
        assertEquals("127.0.0.1:59241", docker("port", CONTAINER, "3306/tcp"));
        String credential = docker("exec", CONTAINER, "cat", "/run/m9/root-password");
        DataSource source = new DriverManagerDataSource(
                "jdbc:mysql://127.0.0.1:59241/gxp_writer_snapshot"
                        + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000",
                "root", credential);
        jdbc = new JdbcTemplate(source);
        assertEquals("gxp_writer_snapshot", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertTrue(Objects.requireNonNull(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8."));
        transactions = new DataSourceTransactionManager(source);
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        GlobalConfig global = new GlobalConfig();
        global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
        global.setMetaObjectHandler(new DefaultDBFieldHandler());
        MybatisPlusInterceptor plugin = new MybatisPlusInterceptor();
        plugin.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties())));
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(config);
        factory.setGlobalConfig(global);
        factory.setPlugins(plugin);
        factory.setTransactionFactory(new SpringManagedTransactionFactory());
        SqlSessionFactory sqlFactory = Objects.requireNonNull(factory.getObject());
        MAPPERS.forEach(sqlFactory.getConfiguration()::addMapper);
        sessions = new SqlSessionTemplate(sqlFactory);
        preflightSchema();
        String fixturePath = System.getProperty("correction.mysql.fixture");
        assertNotNull(fixturePath, "PREFLIGHT: correction.mysql.fixture must identify Dalton's six owned formal cases");
        fixtures = JsonUtils.parseTree(Files.readString(Path.of(fixturePath), StandardCharsets.UTF_8));
        assertEquals("gxp-integration-ampere-correction", fixtures.path("owner").asText());
        assertEquals(6, fixtures.path("cases").size(), "Exactly six isolated cases required");
        Set<Long> tenants = new HashSet<>();
        for (JsonNode row : fixtures.path("cases")) {
            assertTrue(row.path("tenantId").asLong() > 1, "Dedicated non-baseline tenant required");
            assertTrue(tenants.add(row.path("tenantId").asLong()), "Each case must own a different tenant");
        }
    }

    private static void preflightSchema() {
        List<String> missing = new ArrayList<>();
        int checkedMappings = 0;
        // Metadata is used only to check the existing official schema, never to generate substitute DDL.
        for (var table : TableInfoHelper.getTableInfos()) {
            if (!MAPPERS.stream().anyMatch(type -> type.getGenericInterfaces().length > 0
                    && Arrays.toString(type.getGenericInterfaces()).contains(table.getEntityType().getName()))) continue;
            checkedMappings++;
            List<String> engines = jdbc.queryForList("SELECT ENGINE FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?", String.class, table.getTableName());
            if (!engines.equals(List.of("InnoDB"))) {
                missing.add(table.getTableName() + ": missing formal InnoDB table");
                continue;
            }
            Set<String> columns = new HashSet<>(jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?", String.class, table.getTableName()));
            List<String> expected = new ArrayList<>();
            expected.add(table.getKeyColumn());
            table.getFieldList().forEach(field -> expected.add(field.getColumn()));
            expected.stream().filter(column -> column != null && !columns.contains(column.replace("`", "")))
                    .forEach(column -> missing.add(table.getTableName() + "." + column));
        }
        assertEquals(MAPPERS.size(), checkedMappings, "PREFLIGHT must inspect every registered formal Mapper");
        assertTrue(missing.isEmpty(), "PREFLIGHT missing formal columns (no compatibility ALTER permitted): " + missing);
        for (String table : List.of("gxp_audit_event", "gxp_audit_event_relation", "gxp_audit_ledger_sequence",
                "gxp_audit_policy_version", "gxp_audit_policy_operation", "gxp_audit_policy_activation",
                "gxp_audit_coverage_report", "gxp_audit_daily_manifest", "gxp_audit_seal_watermark",
                "gxp_audit_legacy_fact_baseline")) {
            assertEquals(List.of("InnoDB"), jdbc.queryForList("SELECT ENGINE FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?", String.class, table),
                    "PREFLIGHT: full formal audit schema required: " + table);
        }
        assertEquals("json", jdbc.queryForObject("SELECT DATA_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_electronic_signature' "
                + "AND COLUMN_NAME='canonical_content_json'", String.class));
        assertEquals(2048L, jdbc.queryForObject("SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_electronic_signature' "
                + "AND COLUMN_NAME='subject_id'", Long.class));
        String generated = jdbc.queryForObject("SELECT GENERATION_EXPRESSION FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_inspection_piece_detail' "
                + "AND COLUMN_NAME='current_sample_no'", String.class);
        assertNotNull(generated, "PREFLIGHT: formal correction-history migration required");
        assertEquals("casewhendeleted=0thensample_noelsenullend",
                generated.toLowerCase(Locale.ROOT).replace("`", "").replace(" ", "").replace("(", "").replace(")", ""));
        assertEquals("tenant_id,task_id,current_sample_no,item_code", jdbc.queryForObject(
                "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pqc_inspection_piece_detail' "
                        + "AND INDEX_NAME='uk_mes_pqc_piece_current' AND NON_UNIQUE=0 AND SUB_PART IS NULL", String.class));
        assertEquals(12L, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TRIGGERS "
                + "WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE LIKE 'gxp_audit_%'", Long.class));
    }

    @ParameterizedTest(name = "aggregated={0}, failure={1}")
    @CsvSource({"false,NONE", "true,NONE", "false,AUDIT", "true,AUDIT", "false,RECEIPT", "true,RECEIPT"})
    void formalWritesCommitOrAutomaticallyRollback(boolean aggregated, String failure) {
        JsonNode fixture = null;
        for (JsonNode candidate : fixtures.path("cases")) {
            if (candidate.path("aggregated").asBoolean() == aggregated
                    && failure.equals(candidate.path("failure").asText())) {
                assertNull(fixture, "Duplicate case");
                fixture = candidate;
            }
        }
        assertNotNull(fixture, "Missing exact case, no alternative fixture selected");
        long tenant = fixture.path("tenantId").asLong();
        long actorId = fixture.path("actorId").asLong();
        assertTrue(actorId > 0);
        MesProcessPoolPqcInspectionCorrectionCommand command = JsonUtils.parseObject(
                fixture.path("command").toString(), MesProcessPoolPqcInspectionCorrectionCommand.class);
        assertNotNull(command);
        assertEquals(actorId, command.getActorUserId());
        assertTrue(command.getEventId() != null && command.getEventId() > 0);
        command.setSignaturePassword(PASSWORD);
        LoginUser login = new LoginUser();
        login.setId(actorId);
        login.setTenantId(tenant);
        login.setUserType(2);
        login.setInfo(Map.of("username", "correction-local-transaction-fixture",
                LoginUser.INFO_KEY_NICKNAME, "Correction local fixture"));
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, List.of()));
        try {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            Probe probe = new Probe(tenant, command, aggregated, failure);
            probe.preflightFacts();
            Map<String, String> before = snapshot(tenant);
            MesProcessPoolPqcInspectionCorrectionService service = assemble(actorId, probe);
            assertTrue(AopUtils.isAopProxy(service));
            if ("NONE".equals(failure)) {
                Long revision = service.correct(command);
                assertNotNull(revision);
                assertTrue(probe.auditReached && probe.receiptReached);
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                sessions.clearCache();
                probe.assertWritten(true);
                assertEquals(revision, jdbc.queryForObject("SELECT id FROM mes_pro_process_pool_event_revision "
                        + "WHERE tenant_id=? AND event_id=?", Long.class, tenant, command.getEventId()));
            } else {
                ServiceException error = assertThrows(ServiceException.class, () -> service.correct(command));
                assertEquals(GXP_AUDIT_APPEND_FAILED.getCode(), error.getCode());
                assertTrue(probe.auditReached, "Must reach the injected fault AFTER real persisted writes");
                assertEquals("RECEIPT".equals(failure), probe.receiptReached);
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                sessions.clearCache();
                assertEquals(before, snapshot(tenant), "Every formal row and ledger must recover exactly");
            }
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }
    }

    private static MesProcessPoolPqcInspectionCorrectionService assemble(long actor, Probe probe) {
        GxpAuditServiceImpl writerTarget = new GxpAuditServiceImpl();
        inject(writerTarget, "auditEventMapper", mapper(GxpAuditEventMapper.class));
        inject(writerTarget, "ledgerSequenceMapper", mapper(GxpAuditLedgerSequenceMapper.class));
        inject(writerTarget, "policyOperationMapper", mapper(GxpAuditPolicyOperationMapper.class));
        inject(writerTarget, "policyActivationMapper", mapper(GxpAuditPolicyActivationMapper.class));
        inject(writerTarget, "eventRelationMapper", mapper(GxpAuditEventRelationMapper.class));
        GxpAuditService writer = transactional(writerTarget, null);
        GxpAuditService audited = new GxpAuditService() {
            public void acquireLedgerLock() { writer.acquireLedgerLock(); }
            public GxpAuditAppendResult append(GxpAuditCommand input) {
                GxpAuditAppendResult result = writer.append(input);
                if (OPERATION.equals(input.getOperationId())) {
                    probe.assertWritten(true);
                    probe.auditReached = true;
                    if ("AUDIT".equals(probe.failure)) throw exception(GXP_AUDIT_APPEND_FAILED, "correction-test-after-audit-write");
                }
                return result;
            }
        };
        // Only external identity ports are doubled. No core service/Mapper write is mocked.
        AdminUserApi reauthentication = mock(AdminUserApi.class, invocation -> {
            assertEquals("reauthenticateForSignature", invocation.getMethod().getName());
            assertEquals(actor, (Long) invocation.getArgument(0));
            assertEquals(PASSWORD, invocation.getArgument(1));
            return null;
        });
        AdminUserDO user = new AdminUserDO();
        user.setId(actor);
        user.setUsername("correction-local-transaction-fixture");
        user.setNickname("Correction local fixture");
        AdminUserService users = mock(AdminUserService.class);
        when(users.getUser(actor)).thenReturn(user);
        PermissionService permissions = mock(PermissionService.class);
        when(permissions.getUserRoleIdListByUserId(actor)).thenReturn(Set.of());
        DccElectronicSignatureAuthorizationService authorization = mock(DccElectronicSignatureAuthorizationService.class);
        when(authorization.isElectronicSignatureEnabled(actor)).thenReturn(true);
        var adapter = new MesBatchRecordSignatureSubjectAdapter();
        ElectronicSignatureServiceImpl signatureTarget = new ElectronicSignatureServiceImpl();
        inject(signatureTarget, "adminUserApi", reauthentication);
        inject(signatureTarget, "signatureRecordMapper", mapper(ElectronicSignatureRecordMapper.class));
        inject(signatureTarget, "subjectAdapters", List.of(adapter));
        inject(signatureTarget, "gxpAuditService", audited);
        ElectronicSignatureQueryServiceImpl query = new ElectronicSignatureQueryServiceImpl();
        inject(query, "signatureRecordMapper", mapper(ElectronicSignatureRecordMapper.class));
        inject(query, "subjectAdapters", List.of(adapter));
        probe.query = query;
        MesProBatchRecordExecutionSignatureService mesSignature = new MesProBatchRecordExecutionSignatureService();
        inject(mesSignature, "adminUserService", users);
        inject(mesSignature, "permissionService", permissions);
        inject(mesSignature, "authorizationService", authorization);
        inject(mesSignature, "adminUserApi", reauthentication);
        inject(mesSignature, "electronicSignatureService", transactional(signatureTarget, null));
        mesSignature = transactional(mesSignature, null);
        var event = mapper(MesProProcessPoolEventMapper.class);
        var record = mapper(MesProProcessPoolPqcRecordMapper.class);
        var task = mapper(MesPqcInspectionTaskMapper.class);
        var piece = mapper(MesPqcInspectionPieceDetailMapper.class);
        var review = mapper(MesProcessPoolSubmissionReviewMapper.class);
        var aggregate = mapper(MesPqcProcessInspectionAggregateDetailMapper.class);
        var revisionTarget = new MesProcessPoolEventRevisionServiceImpl(
                event, mapper(MesProProcessPoolEventRevisionMapper.class), mapper(MesProProcessPoolEventRevisionDiffMapper.class),
                new MesProcessPoolFifoAllocationService(mapper(MesProcessPoolFifoAllocationLineMapper.class)),
                review, mesSignature);
        org.springframework.test.util.ReflectionTestUtils.setField(revisionTarget, "nonconformanceReviewService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class));
        inject(revisionTarget, "pqcTaskMapper", task);
        MesProcessPoolEventRevisionService revision = transactional(revisionTarget, null);
        MesProEdhrNonconformanceReviewServiceImpl freezeGuard = new MesProEdhrNonconformanceReviewServiceImpl();
        inject(freezeGuard, "reviewMapper", mapper(MesProEdhrNonconformanceReviewMapper.class));
        inject(freezeGuard, "workOrderMapper", mapper(MesProWorkOrderMapper.class));
        MesPqcProcessInspectionAggregationService aggregation = transactional(
                new MesPqcProcessInspectionAggregationServiceImpl(record, event, task, piece, aggregate, review), null);
        var correction = new MesProcessPoolPqcInspectionCorrectionService(event, record, task, piece,
                revision, mesSignature, new MesTeamLeaderScopeServiceImpl(mapper(MesProcessPoolTeamLeaderScopeMapper.class)),
                new MesReportAllocationReleaseStateService(mapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class),
                        mapper(MesProEdhrReleaseTransactionMapper.class)), aggregation, freezeGuard);
        inject(correction, "reviewMapper", review);
        inject(correction, "activeOrderMapper", mapper(MesProcessPoolActiveOrderMapper.class));
        inject(correction, "aggregateDetailMapper", aggregate);
        inject(correction, "gxpAuditService", audited);
        inject(correction, "electronicSignatureQueryService", query);
        return transactional(correction, invocation -> {
            Object result = invocation.proceed();
            if ("correct".equals(invocation.getMethod().getName())) {
                assertNotNull(result);
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                probe.assertWritten(true);
                probe.receiptReached = true;
                if ("RECEIPT".equals(probe.failure)) throw exception(GXP_AUDIT_APPEND_FAILED, "correction-test-receipt-boundary");
            }
            return result;
        });
    }

    private static final class Probe {
        final long tenant;
        final MesProcessPoolPqcInspectionCorrectionCommand command;
        final boolean aggregated;
        final String failure;
        long taskId;
        long recordId;
        long previousReview;
        String oldPayload;
        String oldRecordPayload;
        String oldReview;
        final Map<Long, Map<String, Object>> oldAggregateSources = new LinkedHashMap<>();
        boolean auditReached;
        boolean receiptReached;
        ElectronicSignatureQueryService query;
        Probe(long tenant, MesProcessPoolPqcInspectionCorrectionCommand command, boolean aggregated, String failure) {
            this.tenant = tenant; this.command = command; this.aggregated = aggregated; this.failure = failure;
        }
        void preflightFacts() {
            var event = mapper(MesProProcessPoolEventMapper.class).selectById(command.getEventId());
            assertNotNull(event, "PREFLIGHT: required owned event absent");
            assertEquals(tenant, event.getTenantId());
            oldPayload = event.getRawPayload();
            taskId = event.getFeedbackSourceId();
            var task = mapper(MesPqcInspectionTaskMapper.class).selectById(taskId);
            assertNotNull(task);
            assertNotEquals(task.getActualInspectionQuantity(), command.getActualInspectionQuantity(),
                    "PREFLIGHT: choose a legal quantity change so task UPDATE is observable");
            var record = mapper(MesProProcessPoolPqcRecordMapper.class).selectByEventId(event.getId());
            assertNotNull(record);
            recordId = record.getId();
            oldRecordPayload = record.getRawPayload();
            assertEquals(aggregated, "AGGREGATED".equals(record.getProcessInspectionAggregationStatus()));
            previousReview = record.getProcessInspectionReviewId() == null ? 0 : record.getProcessInspectionReviewId();
            assertEquals(aggregated, previousReview > 0);
            // MS-C01 Given: a real approved review and nonempty aggregates bound to the old current pieces.
            if (aggregated) {
                var review = mapper(MesProcessPoolSubmissionReviewMapper.class).selectById(previousReview);
                assertNotNull(review, "PREFLIGHT: old review must exist");
                assertEquals(tenant, review.getTenantId());
                assertEquals(command.getEventId(), review.getEventId());
                assertEquals("PQC", review.getLeaderType());
                assertEquals("APPROVED", review.getReviewStatus());
                oldReview = JsonUtils.toJsonString(review);
                var oldPieces = mapper(MesPqcInspectionPieceDetailMapper.class).selectListByTaskId(taskId);
                assertFalse(oldPieces.isEmpty(), "PREFLIGHT: current pieces required");
                var oldRows = mapper(MesPqcProcessInspectionAggregateDetailMapper.class).selectListByEventId(event.getId());
                assertFalse(oldRows.isEmpty(), "PREFLIGHT: existing aggregation required");
                assertEquals(oldPieces.size(), oldRows.size());
                Set<Long> representedPieces = new HashSet<>();
                for (var row : oldRows) {
                    assertEquals(tenant, row.getTenantId());
                    assertEquals(event.getId(), row.getEventId());
                    assertEquals(taskId, row.getPqcTaskId());
                    assertEquals(recordId, row.getSourcePqcRecordId());
                    assertEquals(previousReview, row.getReviewId());
                    assertEquals(record.getProductionSubmitEventId(), row.getProductionSubmitEventId());
                    var source = oldPieces.stream().filter(piece -> piece.getId().equals(row.getSourcePieceDetailId()))
                            .findFirst().orElseThrow(() -> new AssertionError("PREFLIGHT: aggregate must bind a current piece"));
                    assertEquals(tenant, source.getTenantId());
                    assertEquals(taskId, source.getTaskId());
                    assertEquals(source.getMeasuredValue(), row.getMeasuredValue());
                    assertEquals(source.getSampleNo(), row.getSampleNo());
                    assertEquals(source.getItemCode(), row.getItemCode());
                    assertTrue(representedPieces.add(source.getId()), "PREFLIGHT: duplicate aggregate piece binding");
                    oldAggregateSources.put(row.getId(), jdbc.queryForMap(
                            "SELECT tenant_id,event_id,pqc_task_id,source_pqc_record_id,source_piece_detail_id,"
                                    + "review_id,production_submit_event_id FROM mes_pqc_process_inspection_aggregate_detail "
                                    + "WHERE tenant_id=? AND id=? AND deleted=FALSE", tenant, row.getId()));
                }
            }
            for (String table : List.of("system_electronic_signature", "mes_pro_process_pool_event_revision",
                    "mes_pro_process_pool_event_revision_diff", "gxp_audit_event", "gxp_audit_event_relation")) {
                assertEquals(0L, count(table, tenant), "PREFLIGHT: fresh isolated case required: " + table);
            }
            assertEquals(1L, count("gxp_audit_ledger_sequence", tenant));
            assertEquals(1L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence "
                    + "WHERE tenant_id=?", Long.class, tenant));
            assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=? "
                    + "AND operation_id IN ('signature.record.create','mes.pqc-inspection.correct') "
                    + "AND active=TRUE AND deleted=FALSE", Long.class, tenant), "PREFLIGHT: both test policies required");
        }
        void assertWritten(boolean auditWritten) {
            sessions.clearCache();
            assertEquals(1L, count("system_electronic_signature", tenant));
            assertEquals(1L, count("mes_pro_process_pool_event_revision", tenant));
            assertTrue(count("mes_pro_process_pool_event_revision_diff", tenant) > 0);
            var event = mapper(MesProProcessPoolEventMapper.class).selectById(command.getEventId());
            assertNotEquals(JsonUtils.parseTree(oldPayload), JsonUtils.parseTree(event.getRawPayload()));
            var task = mapper(MesPqcInspectionTaskMapper.class).selectById(taskId);
            assertEquals(command.getActualInspectionQuantity(), task.getActualInspectionQuantity());
            var record = mapper(MesProProcessPoolPqcRecordMapper.class).selectByEventId(command.getEventId());
            assertEquals(recordId, record.getId());
            assertNotEquals(JsonUtils.parseTree(oldRecordPayload), JsonUtils.parseTree(record.getRawPayload()));
            assertEquals(JsonUtils.parseTree(event.getRawPayload()), JsonUtils.parseTree(record.getRawPayload()));
            var revision = mapper(MesProProcessPoolEventRevisionMapper.class).selectListByEventId(command.getEventId()).get(0);
            assertEquals(JsonUtils.parseTree(event.getRawPayload()), JsonUtils.parseTree(revision.getAfterPayload()));
            var formal = query.getById(revision.getRevisionSignatureId());
            assertNotNull(formal);
            assertEquals("VALID", query.verifyEvidence(revision.getRevisionSignatureId()).verificationStatus());
            assertEquals(tenant, mapper(ElectronicSignatureRecordMapper.class)
                    .selectById(formal.id()).getTenantId());
            assertEquals(command.getActorUserId(), formal.actorId());
            assertEquals("FIELD_CHANGE", formal.actionCode());
            var pieces = mapper(MesPqcInspectionPieceDetailMapper.class).selectListByTaskId(taskId);
            for (var item : command.getItemResults()) {
                for (int index = 0; index < item.getSampleValues().size(); index++) {
                    int sample = index + 1;
                    var detail = pieces.stream().filter(p -> p.getSampleNo() == sample && item.getItemCode().equals(p.getItemCode()))
                            .findFirst().orElseThrow();
                    assertEquals(item.getSampleValues().get(index), detail.getMeasuredValue());
                    assertEquals(tenant, detail.getTenantId());
                }
            }
            assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM mes_pqc_inspection_piece_detail "
                    + "WHERE tenant_id=? AND task_id=? AND deleted=TRUE", Long.class, tenant, taskId) > 0);
            if (aggregated) {
                // MS-C01 Then: verify persisted new review, signed supersession, and replacement BEFORE a fault.
                // This same probe is also executed after successful commit; rollback still compares all raw rows.
                assertNotEquals(previousReview, record.getProcessInspectionReviewId());
                assertEquals(oldReview, JsonUtils.toJsonString(
                        mapper(MesProcessPoolSubmissionReviewMapper.class).selectById(previousReview)), "Old review must remain unchanged");
                var newReview = mapper(MesProcessPoolSubmissionReviewMapper.class)
                        .selectById(record.getProcessInspectionReviewId());
                assertNotNull(newReview, "New review must be an actual persisted row");
                assertEquals(record.getProcessInspectionReviewId(), newReview.getId());
                assertEquals(tenant, newReview.getTenantId());
                assertEquals(command.getEventId(), newReview.getEventId());
                assertEquals("APPROVED", newReview.getReviewStatus());
                assertEquals("PQC", newReview.getLeaderType());
                assertEquals(formal.actorId(), newReview.getLeaderUserId());
                assertEquals(formal.actorId(), newReview.getReviewSignatureUserId());
                assertEquals(formal.id(), newReview.getReviewSignatureId());
                assertEquals(command.getChangeReason().trim(), newReview.getReviewRemark());
                assertNotNull(newReview.getReviewSignatureSnapshotJson());
                JsonNode evidence = JsonUtils.parseTree(newReview.getReviewSignatureSnapshotJson());
                assertEquals("PQC_INSPECTION_CORRECTION", evidence.path("actionType").asText());
                assertEquals(revision.getId().longValue(), evidence.path("revisionId").longValue());
                assertEquals(previousReview, evidence.path("supersededReviewId").longValue());
                assertEquals(command.getEventId().longValue(), evidence.path("processPoolEventId").longValue());
                assertEquals(formal.id().longValue(), evidence.path("signatureId").longValue());
                assertEquals(formal.actorId().longValue(), evidence.path("actorId").longValue());
                assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                        evidence.path("payloadHash").asText());
                assertEquals(formal.id().longValue(), evidence.path("signature").path("signatureId").longValue());
                assertEquals(formal.actorId().longValue(), evidence.path("signature").path("actorId").longValue());
                assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_submission_review "
                        + "WHERE tenant_id=? AND event_id=? AND id IN (?,?) AND deleted=FALSE", Long.class,
                        tenant, command.getEventId(), previousReview, newReview.getId()));
                var rows = mapper(MesPqcProcessInspectionAggregateDetailMapper.class).selectListByEventId(command.getEventId());
                assertEquals(pieces.size(), rows.size());
                for (var old : oldAggregateSources.entrySet()) {
                    assertTrue(rows.stream().noneMatch(row -> row.getId().equals(old.getKey())),
                            "Old aggregate cannot remain in the current projection");
                    assertEquals(old.getValue(), jdbc.queryForMap(
                            "SELECT tenant_id,event_id,pqc_task_id,source_pqc_record_id,source_piece_detail_id,"
                                    + "review_id,production_submit_event_id FROM mes_pqc_process_inspection_aggregate_detail "
                                    + "WHERE tenant_id=? AND id=? AND deleted=TRUE", tenant, old.getKey()),
                            "Old aggregate must be soft-deleted with historical source bindings preserved");
                }
                Set<Long> representedPieces = new HashSet<>();
                for (var row : rows) {
                    assertEquals(command.getEventId(), row.getEventId());
                    assertEquals(taskId, row.getPqcTaskId());
                    assertEquals(recordId, row.getSourcePqcRecordId());
                    assertEquals(record.getProcessInspectionReviewId(), row.getReviewId());
                    var sourcePiece = pieces.stream().filter(piece -> piece.getId().equals(row.getSourcePieceDetailId()))
                            .findFirst().orElseThrow();
                    assertEquals(sourcePiece.getMeasuredValue(), row.getMeasuredValue());
                    assertEquals(sourcePiece.getSampleNo(), row.getSampleNo());
                    assertEquals(sourcePiece.getItemCode(), row.getItemCode());
                    assertEquals(tenant, row.getTenantId());
                    assertTrue(representedPieces.add(sourcePiece.getId()), "Every current piece is bound exactly once");
                }
            } else {
                assertEquals(0L, count("mes_pro_process_pool_submission_review", tenant));
                assertEquals(0L, count("mes_pqc_process_inspection_aggregate_detail", tenant));
            }
            if (auditWritten) {
                assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE tenant_id=? "
                        + "AND operation_id=?", Long.class, tenant, OPERATION));
                assertEquals(formal.contentHash(), jdbc.queryForObject("SELECT signature_content_hash FROM gxp_audit_event "
                        + "WHERE tenant_id=? AND operation_id=?", String.class, tenant, OPERATION));
                assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE tenant_id=? "
                        + "AND operation_id='signature.record.create'", Long.class, tenant));
                assertTrue(count("gxp_audit_event_relation", tenant) > 0);
                assertEquals(3L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence "
                        + "WHERE tenant_id=?", Long.class, tenant));
            }
        }
    }

    private static Map<String, String> snapshot(long tenant) {
        Map<String, String> result = new TreeMap<>();
        for (String table : WRITE_TABLES) {
            String order = "gxp_audit_ledger_sequence".equals(table) ? "tenant_id" : "id";
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=? ORDER BY " + order, tenant);
            for (Map<String, Object> row : rows) row.replaceAll((key, value) ->
                    value instanceof byte[] bytes ? HexFormat.of().formatHex(bytes) : value);
            result.put(table, JsonUtils.toJsonString(rows));
        }
        return result;
    }
    private static long count(String table, long tenant) {
        assertTrue(WRITE_TABLES.contains(table), "Table must be explicitly owned");
        return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_id=?", Long.class, tenant));
    }
    private static <T> T mapper(Class<T> type) { return sessions.getMapper(type); }
    private static void inject(Object target, String field, Object value) { ReflectionTestUtils.setField(target, field, value); }
    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target, MethodInterceptor insideTransaction) {
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        if (insideTransaction != null) proxy.addAdvice(insideTransaction);
        return (T) proxy.getProxy();
    }
    private static String docker(String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("docker"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("PREFLIGHT: dedicated fixture inspection timed out");
        }
        assertEquals(0, process.exitValue(), "PREFLIGHT: dedicated fixture unavailable; credential output withheld");
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
    }
}
