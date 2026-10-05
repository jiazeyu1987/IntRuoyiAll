package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.config.YudaoTenantAutoConfiguration;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.spi.CompatibleHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.MybatisUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.h2.api.Trigger;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Bounded audit-envelope proof, not formal-source/signature authorization or full workflow proof.
 * Real parent services, aggregation/backfill writes, mappers and audit writer share a disposable H2 transaction.
 * Only formal-source discovery, progress, pick-list freeze, transfer trace and identity/signature are boundary doubles.
 */
class MesCompletionAggregationAuditTransactionTest {
    private static final String ORDER = "mes_pro_process_pool_active_order";
    private static final String COMPLETION = "mes_pro_process_pool_order_process_completion";
    private static final String TASK = "mes_pqc_inspection_task";
    private static final String RECORD = "mes_pro_process_pool_pqc_record";
    private static final String AGGREGATE = "mes_pqc_process_inspection_aggregate_detail";
    private static final String REVIEW = "mes_pro_process_pool_submission_review";
    private static final String RECEIPT = ORDER + "_completion_receipt";
    private static final String BACKFILL = ORDER + "_completion_backfill";
    private static final AtomicBoolean FAILURE_OBSERVED_WRITES = new AtomicBoolean();
    private static final List<String> BUSINESS_TABLES = List.of(ORDER, COMPLETION, TASK, RECORD,
            AGGREGATE, REVIEW, RECEIPT, BACKFILL, "mes_pro_process_pool_event", "mes_pqc_inspection_piece_detail");
    private JdbcTemplate jdbc;
    private MesTeamLeaderActiveOrderCompletionService completionService;
    private MesTeamLeaderSubmissionReviewService reviewService;
    private Object previousBeanFactory;
    private Object previousMybatisContext;
    private GenericApplicationContext fixtureContext;
    private MesReleaseAffectedStateCollector completionCollector;
    private DataSourceTransactionManager completionTransactionManager;
    private MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;

    @BeforeEach
    void fixture() throws Exception {
        previousBeanFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        previousMybatisContext = ReflectionTestUtils.getField(CompatibleHelper.getCompatibleSet().getClass(), "applicationContext");
        var source = new DriverManagerDataSource("jdbc:h2:mem:completion_aggregation_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        String jsonFunctions = "cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelinePqcGroupSqlTest";
        jdbc.execute("CREATE ALIAS JSON_EXTRACT FOR '" + jsonFunctions + ".jsonExtract'");
        jdbc.execute("CREATE ALIAS JSON_UNQUOTE FOR '" + jsonFunctions + ".jsonUnquote'");
        for (Class<?> row : List.of(MesProcessPoolActiveOrderDO.class,
                MesProcessPoolOrderProcessCompletionDO.class, MesProcessPoolActiveOrderCompletionReceiptDO.class,
                MesProcessPoolActiveOrderCompletionBackfillDO.class, MesProProcessPoolEventDO.class,
                MesProProcessPoolPqcRecordDO.class, MesPqcInspectionTaskDO.class,
                MesPqcInspectionPieceDetailDO.class, MesPqcProcessInspectionAggregateDetailDO.class,
                MesProcessPoolSubmissionReviewDO.class, MesProProcessPoolEventRevisionDO.class,
                GxpAuditEventDO.class, GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class,
                GxpAuditPolicyActivationDO.class, GxpAuditPolicyOperationDO.class)) createTable(row);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addInterceptor(new MesActiveOrderMaintenanceAuditTransactionTest.MysqlBitLiteralForH2());
        var tenantPlugin = new MybatisPlusInterceptor();
        new YudaoTenantAutoConfiguration().tenantLineInnerInterceptor(new TenantProperties(), tenantPlugin);
        config.addInterceptor(tenantPlugin);
        var factory = new MybatisSqlSessionFactoryBean();
        fixtureContext = new GenericApplicationContext();
        factory.setApplicationContext(fixtureContext);
        factory.setDataSource(source);
        factory.setConfiguration(config);
        factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO)));
        var sqlFactory = Objects.requireNonNull(factory.getObject());
        for (Class<?> mapper : List.of(MesProcessPoolActiveOrderMapper.class,
                MesProcessPoolOrderProcessCompletionMapper.class, MesProcessPoolActiveOrderCompletionReceiptMapper.class,
                MesProcessPoolActiveOrderCompletionBackfillMapper.class, MesProProcessPoolEventMapper.class,
                MesProProcessPoolPqcRecordMapper.class, MesPqcInspectionTaskMapper.class,
                MesPqcInspectionPieceDetailMapper.class, MesPqcProcessInspectionAggregateDetailMapper.class,
                MesProcessPoolSubmissionReviewMapper.class, MesProProcessPoolEventRevisionMapper.class,
                GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class)) config.addMapper(mapper);
        var sessions = new SqlSessionTemplate(sqlFactory);
        completionReceiptMapper = sessions.getMapper(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        var tx = new DataSourceTransactionManager(source);
        // BaseMapperX uses Hutool for DB type and MyBatis Plus's separate context for Db.saveBatch's mapper.
        // Both must point to this fixture; a previous Spring test's mapper must never supply batch writes.
        var beans = fixtureContext.getBeanFactory();
        beans.registerSingleton("dataSource", source);
        beans.registerSingleton("sqlSessionFactory", sqlFactory);
        beans.registerSingleton("aggregateMapper", sessions.getMapper(MesPqcProcessInspectionAggregateDetailMapper.class));
        fixtureContext.refresh();
        new SpringUtil().postProcessBeanFactory(beans);
        assertSame(source, GlobalConfigUtils.currentSessionFactory(MesPqcProcessInspectionAggregateDetailDO.class)
                .getConfiguration().getEnvironment().getDataSource(), "Global batch session factory must be fixture-owned");
        var batchMapper = SqlHelper.getMapper(MesPqcProcessInspectionAggregateDetailDO.class, sessions);
        assertSame(source, MybatisUtils.getSqlSessionFactory(MybatisUtils.getMybatisMapperProxy(batchMapper))
                .getConfiguration().getEnvironment().getDataSource(), "Db.saveBatch must not use another test's Spring mapper");
        var aggregateMapping = TableInfoHelper.getTableInfo(MesPqcProcessInspectionAggregateDetailDO.class);
        Set<String> mappedColumns = new TreeSet<>();
        mappedColumns.add(aggregateMapping.getKeyColumn());
        aggregateMapping.getFieldList().forEach(field -> mappedColumns.add(field.getColumn()));
        assertEquals(mappedColumns, new TreeSet<>(jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=?", String.class, AGGREGATE)),
                "Aggregate DDL must match the entire mapped DO hierarchy");
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
        var actor = new LoginUser();
        actor.setId(20L);
        actor.setTenantId(1L);
        actor.setInfo(Map.of("username", "completion-audit-test", LoginUser.INFO_KEY_NICKNAME, "completion-audit-test"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        var writer = new GxpAuditServiceImpl();
        inject(writer, "auditEventMapper", sessions.getMapper(GxpAuditEventMapper.class));
        inject(writer, "eventRelationMapper", sessions.getMapper(GxpAuditEventRelationMapper.class));
        inject(writer, "ledgerSequenceMapper", sessions.getMapper(GxpAuditLedgerSequenceMapper.class));
        inject(writer, "policyActivationMapper", sessions.getMapper(GxpAuditPolicyActivationMapper.class));
        inject(writer, "policyOperationMapper", sessions.getMapper(GxpAuditPolicyOperationMapper.class));
        var audit = (GxpAuditService) transactional(writer, tx);
        var aggregation = (MesPqcProcessInspectionAggregationService) transactional(
                new MesPqcProcessInspectionAggregationServiceImpl(
                        sessions.getMapper(MesProProcessPoolPqcRecordMapper.class),
                        sessions.getMapper(MesProProcessPoolEventMapper.class),
                        sessions.getMapper(MesPqcInspectionTaskMapper.class),
                        sessions.getMapper(MesPqcInspectionPieceDetailMapper.class),
                        sessions.getMapper(MesPqcProcessInspectionAggregateDetailMapper.class),
                        sessions.getMapper(MesProcessPoolSubmissionReviewMapper.class)), tx);
        var actualBackfill = new MesTeamLeaderActiveOrderCompletionBackfillPortImpl(null, null,
                sessions.getMapper(MesProcessPoolOrderProcessCompletionMapper.class), null, null, null, null,
                sessions.getMapper(MesProcessPoolActiveOrderCompletionBackfillMapper.class), null, null, null, null, null, null);
        var sourceBoundary = mock(MesTeamLeaderActiveOrderCompletionBackfillPort.class);
        when(sourceBoundary.prepare(anyLong(), any(), any())).thenAnswer(invocation -> draft());
        when(sourceBoundary.matchesReceiptSources(anyLong(), any(), any(), any())).thenReturn(true);
        doAnswer(invocation -> {
            actualBackfill.write(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(sourceBoundary).write(any(), anyLong());
        var progress = mock(MesTeamLeaderActiveOrderCompletionProgressPort.class);
        when(progress.read(anyLong(), any())).thenReturn(new MesTeamLeaderActiveOrderCompletionProgress()
                .setProductionProgressPercent(BigDecimal.valueOf(100)).setInspectionProgressPercent(BigDecimal.valueOf(100)));
        var completion = new MesTeamLeaderActiveOrderCompletionServiceImpl(
                sessions.getMapper(MesProcessPoolActiveOrderMapper.class),
                sessions.getMapper(MesProcessPoolActiveOrderCompletionReceiptMapper.class), progress, sourceBoundary,
                mock(MesTeamLeaderActiveOrderPickListCompletionSourceService.class),
                mock(MesActiveOrderTransferTraceService.class), aggregation);
        { org.springframework.test.util.ReflectionTestUtils.setField(completion, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        inject(completion, "gxpAuditService", audit);
        completionCollector = new MesReleaseAffectedStateCollector(source);
        completionTransactionManager = tx;
        inject(completion, "affectedStateCollector", completionCollector);
        completionService = (MesTeamLeaderActiveOrderCompletionService) transactional(completion, tx);
        var review = new MesTeamLeaderSubmissionReviewServiceImpl(mock(MesTeamLeaderScopeService.class),
                sessions.getMapper(MesProProcessPoolEventMapper.class),
                sessions.getMapper(MesProcessPoolSubmissionReviewMapper.class), aggregation);
        // Actual open freeze authority; persistence reads are boundaries for this signature/transaction fixture.
        var openFreeze = new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(openFreeze,"reviewMapper",org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper.class));
        var openOrders = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper.class);
        org.mockito.Mockito.lenient().when(openOrders.selectByIdForUpdate(org.mockito.ArgumentMatchers.anyLong())).thenAnswer(call ->
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder().id(call.getArgument(0)).temporaryFrozen(false).build());
        org.springframework.test.util.ReflectionTestUtils.setField(openFreeze,"workOrderMapper",openOrders);
        org.springframework.test.util.ReflectionTestUtils.setField(review,"nonconformanceReviewService",openFreeze);
        { org.springframework.test.util.ReflectionTestUtils.setField(review, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        { org.springframework.test.util.ReflectionTestUtils.setField(review, "returnCorrectionResolver", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver.class)); }
        var signatureBoundary = mock(MesProBatchRecordExecutionSignatureService.class);
        when(signatureBoundary.recordTeamLeaderReviewSignature(anyLong(), anyString(), anyString(),
                any(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesTeamLeaderReviewSignatureContext.class))).thenReturn(9101L);
        inject(review, "signatureService", signatureBoundary);
        inject(review, "revisionMapper", sessions.getMapper(MesProProcessPoolEventRevisionMapper.class));
        inject(review, "pqcTaskMapper", sessions.getMapper(MesPqcInspectionTaskMapper.class));
        inject(review, "gxpAuditService", audit);
        inject(review, "affectedStateCollector", new MesReleaseAffectedStateCollector(source));
        reviewService = (MesTeamLeaderSubmissionReviewService) transactional(review, tx);
        jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(1,1)");
        jdbc.update("INSERT INTO gxp_audit_policy_activation(id,tenant_id,policy_version) VALUES(1,1,'envelope-test')");
        for (String operation : List.of("mes.active-order.complete", "mes.pqc.review.approve", "mes.pqc.review.reject")) {
            jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,domain,"
                    + "subject_type,action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                    + " VALUES(1,'envelope-test',?,'MES','MES_ACTIVE_ORDER','UPDATE','SYSTEM_WITH_OPTIONAL_TEXT',?,"
                    + "'PRESENT_TO_PRESENT','GXP',1)", operation, operation.contains("review") ? "REQUIRED" : "NONE");
        }
        jdbc.update("INSERT INTO " + ORDER + "(id,tenant_id,leader_user_id,work_order_id,route_id,route_version_id,"
                + "active_status,business_status,version,simulated) VALUES(10,1,20,30,40,41,'ACTIVE','PRODUCING',2,0)");
        jdbc.update("INSERT INTO " + COMPLETION + "(id,tenant_id,work_order_id,route_process_id,process_id,"
                + "completion_status,backfill_status,backfill_error) VALUES(501,1,30,42,43,'COMPLETED','NOT_REQUIRED','prior-binding-message')");
        seedPendingPqc(1001, 5101, 6001, 6101);
        FAILURE_OBSERVED_WRITES.set(false);
    }

    @AfterEach
    void close() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", previousBeanFactory);
        CompatibleHelper.getCompatibleSet().setContext(previousMybatisContext);
        if (fixtureContext != null) fixtureContext.close();
        if (jdbc != null) jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) { statement.execute("SHUTDOWN"); }
            return null;
        });
    }

    @Test
    void approvalCapturesActualTaskRecordAndAggregateBeforeAfter() {
        var before = affectedRows(false);
        Long reviewId = reviewService.reviewSubmission(reviewRequest("APPROVED"));
        assertEquals("CONFIRMED", jdbc.queryForObject("SELECT task_status FROM " + TASK + " WHERE id=5101", String.class));
        assertEquals(reviewId, jdbc.queryForObject("SELECT process_inspection_review_id FROM " + RECORD + " WHERE id=6001", Long.class));
        assertEquals(1, count(AGGREGATE));
        assertAffectedState("before_state_json", before);
        assertAffectedState("after_state_json", affectedRows(false));
        assertEquals(1, count("gxp_audit_event"));
    }

    @Test
    void groupApprovalConfirmsBothTasksAndReplayPreservesAllRows() {
        seedReviewGroup();
        var displayedRequest = reviewRequest("APPROVED");
        Long reviewId = reviewService.reviewSubmission(displayedRequest);
        assertEquals(List.of("CONFIRMED", "CONFIRMED"), jdbc.queryForList(
                "SELECT task_status FROM " + TASK + " ORDER BY id", String.class));
        assertEquals(2, count(REVIEW));
        assertEquals(2, count(AGGREGATE));
        assertEquals(2, count("gxp_audit_event"));
        var completed = businessRows();
        var audit = jdbc.queryForList("SELECT * FROM gxp_audit_event ORDER BY id");
        assertEquals(reviewId, reviewService.reviewSubmission(displayedRequest));
        assertEquals(completed, businessRows());
        assertEquals(audit, jdbc.queryForList("SELECT * FROM gxp_audit_event ORDER BY id"));
    }

    @Test
    void secondGroupMembersAuditFailureRollsBackFirstAndSecondTogether() {
        seedReviewGroup();
        var before = businessRows();
        jdbc.execute("CREATE TRIGGER fail_second_group_audit BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + FailSecondGroupRelation.class.getName() + "'");
        assertThrows(RuntimeException.class, () -> reviewService.reviewSubmission(reviewRequest("APPROVED")));
        assertTrue(FAILURE_OBSERVED_WRITES.get(), "Failure must follow both real review/aggregation writes");
        assertEquals(before, businessRows());
        assertAuditRollback();
    }

    private void seedReviewGroup() {
        seedPendingPqc(1002, 5102, 6002, 6102);
        jdbc.update("UPDATE mes_pro_process_pool_event SET raw_payload=? WHERE id IN (1001,1002)",
                "{\"pqcSubmissionGroupId\":\"audit-group\"}");
    }

    @Test
    void completionCapturesBindingAndCatchUpWithoutChangingAlreadyAggregatedNeighbor() {
        seedCompletionReviewAndNeighbor();
        var before = affectedRows(true);
        var neighbor = jdbc.queryForList("SELECT * FROM " + AGGREGATE + " WHERE event_id=1002");
        completionService.complete(20L, completionCommand());
        assertEquals("SUCCESS", jdbc.queryForObject("SELECT backfill_status FROM " + COMPLETION + " WHERE id=501", String.class));
        assertEquals(jdbc.queryForObject("SELECT batch_record_id FROM " + RECEIPT, Long.class),
                jdbc.queryForObject("SELECT backfill_execution_id FROM " + COMPLETION + " WHERE id=501", Long.class));
        assertEquals(2, count(AGGREGATE));
        assertEquals(neighbor, jdbc.queryForList("SELECT * FROM " + AGGREGATE + " WHERE event_id=1002"));
        assertAffectedState("before_state_json", before);
        assertAffectedState("after_state_json", affectedRows(true));
        assertEquals(2, count("gxp_audit_event"));
    }

    @Test
    void largeCompletionPersistsEveryFullRowAndVerifiedManifestReceiptAndLedgerHashes() {
        seedCompletionReviewAndNeighbor();
        String body = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                Map.of("qaSnapshot", "完整标准\\\"生产路径".repeat(9000), "selectedSignedAt", "unchanged"));
        jdbc.update("UPDATE " + RECORD + " SET raw_payload=? WHERE event_id=1001", body);
        String before = fullCompletionRows();
        completionService.complete(20L, completionCommand());
        String after = fullCompletionRows();
        var parent = JSON.parseObject(jdbc.queryForObject("SELECT after_state_json FROM gxp_audit_event "
                + "WHERE idempotency_key NOT LIKE '%:ROWS:%'", String.class));
        var manifest = parent.getJSONObject("affectedRowAuditManifest");
        assertNotNull(manifest);
        assertTrue(manifest.getJSONArray("parts").size() > 1);
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(before)), manifest.getString("beforeHash"));
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(after)), manifest.getString("afterHash"));
        assertEquals(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(before),
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(reconstructCompletionRows("before_state_json")));
        assertEquals(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(after),
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(reconstructCompletionRows("after_state_json")));
        var receipt = completionReceiptMapper.selectById(jdbc.queryForObject("SELECT id FROM " + RECEIPT, Long.class));
        assertEquals(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt), receipt.getReceiptHash());
        assertEquals(receipt.getReceiptHash(), parent.getString("completionReceiptHash"));
        String previous = null;
        for (var event : jdbc.queryForList("SELECT * FROM gxp_audit_event ORDER BY ledger_sequence")) {
            String canonical = (String) event.get("canonical_event_json");
            String oldState = (String) event.get("before_state_json");
            String newState = (String) event.get("after_state_json");
            assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical), event.get("event_hash"));
            assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(oldState + "\u001f" + newState), event.get("state_payload_hash"));
            assertEquals(previous, event.get("previous_event_hash")); previous = (String) event.get("event_hash");
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event_relation WHERE event_id=? "
                            + "AND target_type='COMPLETION_RECEIPT' AND target_id=? AND target_hash=?", Integer.class,
                    event.get("id"), String.valueOf(receipt.getId()), receipt.getReceiptHash()));
        }
        for (Object part : manifest.getJSONArray("parts")) {
            var ref = (JSONObject) part;
            assertEquals(ref.getString("eventHash"), jdbc.queryForObject("SELECT event_hash FROM gxp_audit_event WHERE id=?",
                    String.class, ref.getLong("eventId")));
        }
        var allEvents = jdbc.queryForList("SELECT * FROM gxp_audit_event ORDER BY id");
        completionService.complete(20L, completionCommand());
        assertEquals(allEvents, jdbc.queryForList("SELECT * FROM gxp_audit_event ORDER BY id"));
    }

    @Test
    void laterFragmentFailureRollsBackEarlierFragmentsReceiptAndBusinessWrites() {
        seedCompletionReviewAndNeighbor();
        jdbc.update("UPDATE " + RECORD + " SET raw_payload=? WHERE event_id=1001",
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Map.of("raw", "完整标准".repeat(30000))));
        var before = businessRows();
        jdbc.execute("CREATE TRIGGER fail_later_fragment BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + FailLaterFragment.class.getName() + "'");
        assertThrows(RuntimeException.class, () -> completionService.complete(20L, completionCommand()));
        assertTrue(FAILURE_OBSERVED_WRITES.get(), "An earlier fragment and receipt must have been persisted before failure");
        assertEquals(before, businessRows()); assertAuditRollback();
    }

    private String fullCompletionRows() {
        return new org.springframework.transaction.support.TransactionTemplate(completionTransactionManager)
                .execute(status -> cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                        completionCollector.captureCompletion(10L, 30L)));
    }

    private String reconstructCompletionRows(String column) {
        Map<String, List<com.fasterxml.jackson.databind.JsonNode>> tables = new LinkedHashMap<>();
        for (String json : jdbc.queryForList("SELECT " + column + " FROM gxp_audit_event "
                + "WHERE idempotency_key LIKE '%:ROWS:%' ORDER BY ledger_sequence", String.class)) {
            cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json).path("affectedRows").fields().forEachRemaining(table -> {
                var rows = tables.computeIfAbsent(table.getKey(), key -> new ArrayList<>());
                table.getValue().forEach(rows::add);
            });
        }
        return cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(tables);
    }

    @Test
    void approvalAuditFailureRollsBackReviewTaskRecordAndRealAggregateInsert() {
        var before = businessRows();
        installFailureTrigger();
        assertThrows(RuntimeException.class, () -> reviewService.reviewSubmission(reviewRequest("APPROVED")));
        assertTrue(FAILURE_OBSERVED_WRITES.get(), "Failure must occur after real aggregate and parent writes");
        assertEquals(before, businessRows());
        assertAuditRollback();
    }

    @Test
    void completionAuditFailureRollsBackBindingsCatchUpAndReceiptTogether() {
        seedCompletionReviewAndNeighbor();
        var before = businessRows();
        installFailureTrigger();
        assertThrows(RuntimeException.class, () -> completionService.complete(20L, completionCommand()));
        assertTrue(FAILURE_OBSERVED_WRITES.get(), "Failure must occur after real binding, aggregate and receipt writes");
        assertEquals(before, businessRows());
        assertAuditRollback();
    }

    private void seedPendingPqc(long eventId, long taskId, long recordId, long pieceId) {
        jdbc.update("INSERT INTO mes_pro_process_pool_event(id,tenant_id,event_type,work_order_id,route_id,route_process_id,"
                + "process_id,qa_process_id,feedback_source_type,feedback_source_id,recordbook_source_type,recordbook_source_id,"
                + "actual_employee_id,signature_id,raw_payload) VALUES(?,1,'PQC_INSPECTION',30,40,42,43,44,"
                + "'MES_PQC_INSPECTION_TASK',?,'MES_PQC_INSPECTION_TASK',?,2001,9001,'{}')", eventId, taskId, taskId);
        jdbc.update("INSERT INTO " + TASK + "(id,tenant_id,active_order_id,work_order_id,route_id,route_version_id,"
                + "route_process_id,process_id,qa_process_id,regulation_version_id,inspection_type,business_date,shift_code,"
                + "round_no,actual_inspection_quantity,task_status,submitted_event_id)"
                + " VALUES(?,1,10,30,40,41,42,43,44,45,'PROCESS','2026-09-29','DAY',1,1,'SUBMITTED',?)", taskId, eventId);
        jdbc.update("INSERT INTO " + RECORD + "(id,tenant_id,event_id,work_order_id,route_id,route_process_id,process_id,"
                + "qa_process_id,process_inspection_aggregation_status) VALUES(?,1,?,30,40,42,43,44,'PENDING')", recordId, eventId);
        jdbc.update("INSERT INTO mes_pqc_inspection_piece_detail(id,tenant_id,task_id,sample_no,item_code,item_name,"
                + "inspection_method,standard_text,result_type,measured_value,judgement)"
                + " VALUES(?,1,?,1,'DIM','Dimension','GAUGE','10mm','NUMBER','10','PASS')", pieceId, taskId);
    }

    private void seedCompletionReviewAndNeighbor() {
        jdbc.update("INSERT INTO " + REVIEW + "(id,tenant_id,event_id,leader_user_id,leader_type,review_status)"
                + " VALUES(8001,1,1001,20,'PQC','APPROVED')");
        seedPendingPqc(1002, 5102, 6002, 6102);
        jdbc.update("UPDATE " + TASK + " SET task_status='CONFIRMED' WHERE id=5102");
        jdbc.update("UPDATE " + RECORD + " SET process_inspection_aggregation_status='AGGREGATED',"
                + "process_inspection_review_id=8002 WHERE id=6002");
        jdbc.update("INSERT INTO " + AGGREGATE + "(id,tenant_id,active_order_id,event_id,review_id,pqc_task_id,"
                + "source_pqc_record_id,source_piece_detail_id,item_code,measured_value,judgement)"
                + " VALUES(7002,1,10,1002,8002,5102,6002,6102,'NEIGHBOR','unchanged','PASS')");
    }

    private Map<String, List<Map<String, Object>>> affectedRows(boolean binding) {
        Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
        if (binding) rows.put(COMPLETION, jdbc.queryForList("SELECT id,tenant_id,work_order_id,completion_status,"
                + "backfill_status,backfill_execution_id,backfill_error FROM " + COMPLETION + " ORDER BY id"));
        rows.put(TASK, jdbc.queryForList("SELECT id,tenant_id,active_order_id,task_status,submitted_event_id FROM " + TASK + " ORDER BY id"));
        rows.put(RECORD, jdbc.queryForList("SELECT id,tenant_id,event_id,process_inspection_aggregation_status,"
                + "process_inspection_review_id FROM " + RECORD + " ORDER BY id"));
        rows.put(AGGREGATE, jdbc.queryForList("SELECT id,tenant_id,event_id,review_id,pqc_task_id,source_pqc_record_id,"
                + "source_piece_detail_id,item_code,measured_value,judgement FROM " + AGGREGATE + " ORDER BY id"));
        return rows;
    }

    private void assertAffectedState(String column, Map<String, List<Map<String, Object>>> expected) {
        JSONObject tables;
        if (count(RECEIPT) > 0) {
            tables = JSON.parseObject(reconstructCompletionRows(column));
        } else {
            var envelope = JSON.parseObject(jdbc.queryForObject("SELECT " + column + " FROM gxp_audit_event", String.class));
            tables = envelope.getJSONObject("affectedRows");
        }
        assertNotNull(tables, column + " must capture actual affected rows, not a literal AGGREGATED or receipt IDs");
        expected.forEach((table, rows) -> {
            var actual = tables.getJSONArray(table);
            assertNotNull(actual, column + ":" + table);
            Map<Long, JSONObject> byId = new LinkedHashMap<>();
            for (Object value : actual) {
                var row = (JSONObject) value;
                assertNull(byId.put(row.getLong("id"), row), "Duplicate affected identity");
            }
            assertEquals(rows.stream().map(row -> ((Number) row.get("id")).longValue()).collect(java.util.stream.Collectors.toSet()),
                    byId.keySet(), column + ":" + table + " identities");
            for (var row : rows) row.forEach((key, value) -> assertEquals(value == null ? null : value.toString(),
                    byId.get(((Number) row.get("id")).longValue()).getString(key), column + ":" + table + "." + key));
        });
    }

    private Map<String, List<Map<String, Object>>> businessRows() {
        Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
        BUSINESS_TABLES.forEach(table -> rows.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id")));
        return rows;
    }

    private MesTeamLeaderSubmissionReviewReqBO reviewRequest(String status) {
        // These fixtures contain one exact submitted PQC group, with no persisted review/revision yet.
        var displayed = jdbc.queryForList("SELECT id,raw_payload FROM mes_pro_process_pool_event "
                + "WHERE tenant_id=1 AND deleted=FALSE AND event_type='PQC_INSPECTION' ORDER BY id").stream()
                .map(member -> new MesSubmissionReviewExpectedContext()
                        .setEventId(((Number) member.get("id")).longValue())
                        .setPayloadHash(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher
                                .sha256((String) member.get("raw_payload")))
                        .setRevisionId(0L).setReviewId(0L).setReviewRound(0)).toList();
        return MesTeamLeaderSubmissionReviewReqBO.builder().eventId(1001L).leaderUserId(20L).leaderType("PQC")
                .expectedReviews(displayed)
                .reviewStatus(status).reviewRemark("Current submission review").signaturePassword("BOUNDARY-TEST-ONLY").build();
    }

    private MesTeamLeaderActiveOrderCompletionCommand completionCommand() {
        return new MesTeamLeaderActiveOrderCompletionCommand().setActiveOrderId(10L).setExpectedVersion(2)
                .setIdempotencyKey("current-completion-test").setConfirmNoReplenishmentInfo(true);
    }

    private MesTeamLeaderActiveOrderCompletionBackfillDraft draft() {
        return new MesTeamLeaderActiveOrderCompletionBackfillDraft().setTenantId(1L).setWorkOrderId(30L)
                .setBatchCode("BATCH30").setRouteId(40L).setRouteVersionId(41L).setSourceSnapshotHash("fixture-source")
                .setFormalSourceSnapshotJson("{\"fixtureSource\":true}").setSignatureSnapshotJson("{\"fixtureSignatureBoundary\":true}")
                .setBatchRecordSourceIdsJson("[1]").setProcessInspectionSourceIdsJson("[2]")
                .setBatchRecordStatus("SUCCESS").setProcessInspectionStatus("SUCCESS").setLossReportStatus("NOT_REQUIRED")
                .setHasActualLoss(false).setLossQuantity(BigDecimal.ZERO).setZeroLossConfirmationSnapshot("{\"confirmed\":true}")
                .setLossConditionFactsJson("[{\"processId\":43,\"status\":\"NO_LOSS\",\"hasActualLoss\":false,\"lossQuantity\":0,"
                        + "\"zeroLossConfirmationSnapshot\":\"{\\\"confirmed\\\":true}\",\"sourceHash\":\"loss-source\"}]");
    }

    private void installFailureTrigger() {
        jdbc.execute("CREATE TRIGGER fail_parent_relation BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + FailRelation.class.getName() + "'");
    }

    public static class FailRelation implements Trigger {
        @Override
        public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT (SELECT COUNT(*) FROM gxp_audit_event),"
                            + "(SELECT COUNT(*) FROM " + AGGREGATE + " WHERE event_id=1001),"
                            + "(SELECT task_status FROM " + TASK + " WHERE id=5101),"
                            + "(SELECT process_inspection_aggregation_status FROM " + RECORD + " WHERE id=6001),"
                            + "(SELECT COUNT(*) FROM " + REVIEW + " WHERE event_id=1001),"
                            + "(SELECT COUNT(*) FROM " + RECEIPT + "),"
                            + "(SELECT backfill_status FROM " + COMPLETION + " WHERE id=501)")) {
                rows.next();
                FAILURE_OBSERVED_WRITES.set(rows.getInt(1) == 1 && rows.getInt(2) == 1
                        && "CONFIRMED".equals(rows.getString(3)) && "AGGREGATED".equals(rows.getString(4))
                        && rows.getInt(5) == 1 && (rows.getInt(6) == 0 || "SUCCESS".equals(rows.getString(7))));
            }
            throw new SQLException("Injected relation failure after real parent and subordinate writes");
        }
    }

    public static class FailSecondGroupRelation implements Trigger {
        @Override
        public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT (SELECT COUNT(*) FROM gxp_audit_event),"
                            + "(SELECT COUNT(*) FROM " + AGGREGATE + "),"
                            + "(SELECT COUNT(*) FROM " + REVIEW + "),"
                            + "(SELECT COUNT(*) FROM " + TASK + " WHERE task_status='CONFIRMED')")) {
                rows.next();
                if (rows.getInt(1) < 2) return;
                FAILURE_OBSERVED_WRITES.set(rows.getInt(2) == 2 && rows.getInt(3) == 2 && rows.getInt(4) == 2);
            }
            throw new SQLException("Injected second group member audit failure");
        }
    }

    public static class FailLaterFragment implements Trigger {
        @Override
        public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT (SELECT COUNT(*) FROM gxp_audit_event),(SELECT COUNT(*) FROM " + RECEIPT + ")")) {
                rows.next(); if (rows.getInt(1) < 2) return;
                FAILURE_OBSERVED_WRITES.set(rows.getInt(1) >= 2 && rows.getInt(2) == 1);
            }
            throw new SQLException("Injected later fragment failure after earlier audit and receipt writes");
        }
    }

    private void assertAuditRollback() {
        assertEquals(0, count("gxp_audit_event"));
        assertEquals(0, count("gxp_audit_event_relation"));
        assertEquals(1L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=1", Long.class));
    }

    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    private static Object transactional(Object target, DataSourceTransactionManager manager) {
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }

    private static void inject(Object target, String field, Object value) { ReflectionTestUtils.setField(target, field, value); }

    private void createTable(Class<?> row) {
        List<String> columns = new ArrayList<>();
        for (Class<?> type = row; type != Object.class; type = type.getSuperclass()) for (var field : type.getDeclaredFields()) {
            var mapping = field.getAnnotation(TableField.class);
            if (Modifier.isStatic(field.getModifiers()) || (mapping != null && !mapping.exist())) continue;
            String name = mapping != null && !mapping.value().isBlank() ? mapping.value()
                    : field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            String sqlType = field.getType() == Long.class ? "BIGINT" : field.getType() == Integer.class ? "INT" :
                    field.getType() == Boolean.class ? "INT DEFAULT 0" : field.getType() == BigDecimal.class ? "DECIMAL(24,8)" :
                    field.getType() == LocalDateTime.class ? "TIMESTAMP" : field.getType() == LocalDate.class ? "DATE" : "VARCHAR(1000000)";
            columns.add("`" + name + "` " + (name.equals("id") ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : sqlType));
        }
        jdbc.execute("CREATE TABLE " + row.getAnnotation(TableName.class).value() + "(" + String.join(",", columns) + ")");
    }
}
