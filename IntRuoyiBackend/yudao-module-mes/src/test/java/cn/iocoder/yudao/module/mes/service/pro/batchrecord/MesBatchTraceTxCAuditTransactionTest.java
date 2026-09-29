package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.h2.api.Trigger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Current Tx-C only: real H2, MyBatis, trace graph, GxP writer and Spring transactions.
 * The upstream read-only completion-receipt port is a named boundary double.
 * No historical test fixture, configured database, authentication or external messaging.
 */
@org.junit.jupiter.api.parallel.Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
class MesBatchTraceTxCAuditTransactionTest {
    private static final String BATCH = "mes_pro_edhr_batch_execution";
    private static final String PROVISION = "mes_pro_edhr_batch_provisioning_record";
    private static final String ORIGIN = "mes_pro_edhr_batch_execution_origin";
    private static final String LINK = "mes_pro_edhr_batch_execution_trace_link";
    private static final String MANIFEST = "mes_pro_edhr_batch_execution_trace_manifest";
    private static final String OUTBOX = "mes_pro_edhr_batch_trace_outbox_event";
    private static final String AUDIT = "gxp_audit_event";
    private static final String SUCCESS = "mes.batch-trace.provision";
    private static final String FAILURE = "mes.batch-trace.provision-failure";
    private static final AtomicBoolean AUDIT_FAILURE_SAW_WRITES = new AtomicBoolean();
    private static final AtomicBoolean TRACE_FAILURE_SAW_WRITES = new AtomicBoolean();
    private static boolean expectedFailureState;
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private CountingTransactions transactions;
    private SqlSessionTemplate session;
    private AnnotationConfigApplicationContext context;
    private MesProEdhrBatchTraceTxCProducer producer;
    private final List<String> tables = new ArrayList<>();
    private final List<String> statements = new ArrayList<>();
    private MappingObserver observer;
    private String sourceHash;
    private String bundleHash;

    @BeforeEach
    void fixture() throws Exception {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:txc_audit_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        for (Class<?> row : List.of(MesProEdhrBatchExecutionDO.class,
                MesProEdhrBatchProvisioningRecordDO.class, MesProEdhrOperationAuditEventDO.class,
                MesProcessPoolActiveOrderPickListBindingDO.class, MesProcessPoolActiveOrderPickListBindingItemDO.class,
                MesProEdhrBatchExecutionOriginDO.class, MesProEdhrBatchExecutionTraceLinkDO.class,
                MesProEdhrBatchExecutionTraceManifestDO.class, MesProEdhrBatchTraceOutboxEventDO.class,
                GxpAuditEventDO.class, GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class,
                GxpAuditPolicyActivationDO.class, GxpAuditPolicyOperationDO.class)) createTable(row);
        jdbc.execute("CREATE TABLE parent_marker(id BIGINT PRIMARY KEY)");
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addInterceptor(new ObservedSql(statements));
        List<Class<?>> mappers = List.of(MesProEdhrBatchExecutionMapper.class,
                MesProEdhrBatchProvisioningRecordMapper.class, MesProEdhrOperationAuditEventMapper.class,
                MesProcessPoolActiveOrderPickListBindingMapper.class, MesProcessPoolActiveOrderPickListBindingItemMapper.class,
                MesProEdhrBatchExecutionOriginMapper.class, MesProEdhrBatchExecutionTraceLinkMapper.class,
                MesProEdhrBatchExecutionTraceManifestMapper.class, MesProEdhrBatchTraceOutboxEventMapper.class,
                GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class);
        mappers.forEach(config::addMapper);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(config);
        session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        transactions = new CountingTransactions(dataSource);
        var writer = new GxpAuditServiceImpl();
        ReflectionTestUtils.setField(writer, "auditEventMapper", mapper(GxpAuditEventMapper.class));
        ReflectionTestUtils.setField(writer, "eventRelationMapper", mapper(GxpAuditEventRelationMapper.class));
        ReflectionTestUtils.setField(writer, "ledgerSequenceMapper", mapper(GxpAuditLedgerSequenceMapper.class));
        ReflectionTestUtils.setField(writer, "policyActivationMapper", mapper(GxpAuditPolicyActivationMapper.class));
        ReflectionTestUtils.setField(writer, "policyOperationMapper", mapper(GxpAuditPolicyOperationMapper.class));
        var trace = new MesProEdhrBatchTraceabilityServiceImpl(mapper(MesProEdhrBatchExecutionMapper.class),
                mapper(MesProEdhrBatchExecutionOriginMapper.class), mapper(MesProEdhrBatchExecutionTraceLinkMapper.class),
                mapper(MesProEdhrBatchExecutionTraceManifestMapper.class));
        var receiptPort = mock(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class);
        seedCurrentWitnesses(receiptPort);
        context = new AnnotationConfigApplicationContext();
        context.registerBean("transactionalEventListenerFactory", TransactionalEventListenerFactory.class);
        for (Class<?> type : mappers) registerMapper(type);
        context.registerBean("unifiedAudit", GxpAuditService.class, () -> (GxpAuditService) tx(writer));
        observer = new MappingObserver();
        context.registerBean(MappingObserver.class, () -> observer);
        producer = new MesProEdhrBatchTraceTxCProducer(mapper(MesProEdhrBatchExecutionMapper.class),
                mapper(MesProEdhrOperationAuditEventMapper.class),
                mapper(MesProcessPoolActiveOrderPickListBindingMapper.class),
                mapper(MesProcessPoolActiveOrderPickListBindingItemMapper.class),
                mapper(MesProEdhrBatchTraceOutboxEventMapper.class), (MesProEdhrBatchTraceabilityService) tx(trace),
                context, transactions, receiptPort, mock(MesIndependentBatchPrerequisiteReceiptPort.class),
                mapper(MesProEdhrBatchProvisioningRecordMapper.class));
        // Normal Spring resource injection supplies future mandatory audit/mapper fields;
        // the unchanged pre-RED producer has no such fields and still executes its real behavior.
        context.registerBean(MesProEdhrBatchTraceTxCProducer.class, () -> producer);
        context.registerBean(MesProEdhrBatchTraceTxCApplicationService.class);
        context.refresh();
        for (long tenant : List.of(1L, 2L)) {
            jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(?,1)", tenant);
            jdbc.update("INSERT INTO gxp_audit_policy_activation(id,tenant_id,policy_version) VALUES(?,?,'txc-current-test')", tenant, tenant);
            for (String operation : List.of(SUCCESS, FAILURE)) {
                jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,domain,"
                        + "subject_type,action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                        + " VALUES(?,'txc-current-test',?,'MES','MES_BATCH_TRACE_PROVISIONING','UPDATE','SYSTEM','NONE',"
                        + "'PRESENT_TO_PRESENT','GXP',1)", tenant, operation);
            }
        }
        actor(1L, true);
        AUDIT_FAILURE_SAW_WRITES.set(false);
        TRACE_FAILURE_SAW_WRITES.set(false);
        expectedFailureState = false;
        statements.clear();
    }

    @AfterEach
    void close() {
        if (context != null) context.close();
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        if (jdbc != null) jdbc.execute("SHUTDOWN");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void afterCommitUsesIndependentCommitAndActualBeforeAfterAndCurrentActor(boolean authenticated) {
        actor(1L, authenticated);
        JsonNode before = domainState();
        var parentId = new AtomicReference<String>();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            parentId.set(parentTransactionId());
            jdbc.update("INSERT INTO parent_marker(id) VALUES(1)");
            context.publishEvent(event());
            assertEquals(0, count(OUTBOX), "Transactional listener must not run before parent commit");
        });
        assertEquals(1, count("parent_marker"), "Parent Tx-B stays committed");
        assertEquals("BATCH_READY", status(PROVISION, "status"));
        assertEquals(1, count(ORIGIN));
        assertTrue(count(LINK) >= 17, "Real active-order graph links persisted");
        assertEquals(1, count(MANIFEST));
        assertEquals(1, observer.events.size(), "Tx-C publication occurs after its own commit");
        assertEquals(2, transactions.commits, "Parent and Tx-C are distinct real commits");
        var audit = audit(SUCCESS);
        assertEnvelope(audit, before, domainState());
        assertNotEquals(parentId.get(), audit.getTransactionId());
        assertEquals(authenticated ? 71L : -1L, audit.getActorId());
        assertEquals(authenticated ? "txc-current-fixture" : "SYSTEM_ACTOR", audit.getActorUsername());
        assertEquals(1L, audit.getTenantId());
        assertEquals("SYSTEM", audit.getReasonSource());
        assertEquals("SERVICE_METHOD", audit.getSourceType());
        assertEquals(MesProEdhrBatchTraceTxCProducer.class.getName() + "#produce", audit.getSourceLocator());
        assertNull(audit.getSignatureRecordId());
        int ledgerLock = firstSql("gxp_audit_ledger_sequence", "for update");
        int businessLock = firstSql(BATCH, "for update");
        assertTrue(ledgerLock >= 0 && businessLock > ledgerLock,
                "Ledger lock must precede the first batch lock");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void failureRetainsExistingRetryableOrBlockedStateInOwnAuditedTransaction(boolean retryable) {
        JsonNode before = domainState();
        MesProEdhrBatchTraceTxCCommand command = command();
        if (retryable) command.setExpectedSourceSnapshotHash("different-current-witness");
        else installTraceFailure();
        MesProEdhrBatchTraceTxCResult result = producer.produce(command);
        String expected = retryable ? "BATCH_PROVISIONING_RETRYABLE" : "BATCH_PROVISIONING_BLOCKED";
        assertEquals(expected, status(PROVISION, "status"));
        assertEquals(expected, status(BATCH, "provisioning_status"));
        assertEquals(retryable ? "SOURCE_SNAPSHOT_HASH_MISMATCH" : "TRACE_SERVICE_FAILURE",
                status(PROVISION, "error_code"));
        assertEquals(1, jdbc.queryForObject("SELECT attempt_count FROM " + PROVISION, Integer.class));
        assertEquals(0, count(ORIGIN));
        assertEquals(0, count(LINK));
        assertEquals(0, count(MANIFEST));
        assertEquals("TRACE_MAPPING_BLOCKED", result.getStatus());
        assertEquals(1, transactions.rollbacks);
        assertEquals(1, transactions.commits);
        if (!retryable) assertTrue(TRACE_FAILURE_SAW_WRITES.get(), "Trace transaction really wrote before failure");
        var audit = audit(FAILURE);
        assertEnvelope(audit, before, domainState());
        assertEquals("FAILED", audit.getResultStatus());
        assertNotNull(audit.getErrorCode());
        assertEquals(SUCCESS, audit.getAttemptedOperationId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void auditRelationFailureRollsBackAndEscapesWithoutBusinessFailureRewrite(boolean businessFailure) {
        expectedFailureState = businessFailure;
        jdbc.execute("CREATE TRIGGER fail_txc_audit BEFORE INSERT ON gxp_audit_event_relation"
                + " FOR EACH ROW CALL '" + AuditRelationFailure.class.getName() + "'");
        var before = allRows();
        var command = command();
        if (businessFailure) command.setExpectedSourceSnapshotHash("different-current-witness");
        assertThrows(RuntimeException.class, () -> producer.produce(command),
                "Audit failure must propagate rather than return TRACE_SERVICE_FAILURE");
        assertTrue(AUDIT_FAILURE_SAW_WRITES.get(), "Injected failure must observe actual domain/outbox/audit writes");
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(before, allRows(), "All business/graph/outbox/audit/watermark rows restore");
        assertEquals(0, observer.events.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sameRequestReplayHasZeroDuplicateAppendOrStateChange(boolean businessFailure) {
        var command = command();
        if (businessFailure) command.setExpectedSourceSnapshotHash("different-current-witness");
        producer.produce(command);
        assertEquals(1, count(AUDIT), "First result must include its audit");
        var before = allRows();
        int publications = observer.events.size();
        var replay = producer.produce(command);
        assertTrue(replay.getIdempotent());
        assertEquals(before, allRows());
        assertEquals(publications, observer.events.size());
    }

    @Test
    void actorTenantMismatchRejectsBeforeBusinessWrite() {
        actor(2L, true);
        TenantContextHolder.setTenantId(1L);
        var before = allRows();
        assertThrows(RuntimeException.class, () -> producer.produce(command()));
        assertEquals(before, allRows());
        assertEquals(0, transactions.begins, "Reject inconsistent authenticated tenant before transaction/locks");
    }

    @Test
    void eventTenantMismatchRejectsBeforeBusinessWrite() {
        actor(2L, true);
        var before = allRows();
        assertThrows(RuntimeException.class, () ->
                context.getBean(MesProEdhrBatchTraceTxCApplicationService.class).handle(event()));
        assertEquals(before, allRows());
        assertEquals(2L, TenantContextHolder.getRequiredTenantId(), "Listener restores caller context");
    }

    @Test
    void foreignTenantCannotReplayAnotherTenantsMatchingOutbox() {
        producer.produce(command());
        assertEquals(1, count(OUTBOX));
        actor(2L, true);
        var before = allRows();
        assertThrows(RuntimeException.class, () -> producer.produce(command()));
        assertEquals(before, allRows());
    }

    @Test
    void failureCommitAndTransactionIdentitySurviveParentRollback() {
        JsonNode before = domainState();
        var parentId = new AtomicReference<String>();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            parentId.set(parentTransactionId());
            jdbc.update("INSERT INTO parent_marker(id) VALUES(2)");
            producer.produce(command().setExpectedSourceSnapshotHash("different-current-witness"));
            assertEquals(parentId.get(), parentTransactionId(), "Inner Tx-C must not replace outer audit identity");
            status.setRollbackOnly();
        });
        assertEquals(0, count("parent_marker"));
        assertEquals("BATCH_PROVISIONING_RETRYABLE", status(PROVISION, "status"));
        var audit = audit(FAILURE);
        assertEnvelope(audit, before, domainState());
        assertNotEquals(parentId.get(), audit.getTransactionId());
        assertEquals(1, transactions.commits);
        assertEquals(2, transactions.rollbacks, "Failed success attempt and parent both roll back");
    }

    private void seedCurrentWitnesses(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort receiptPort) {
        List<MesProEdhrBatchTraceSource> sources = new ArrayList<>();
        List<String> types = new ArrayList<>(MesProEdhrBatchTraceabilityValidator.requiredLinkTypesFor("ACTIVE_ORDER_COMPLETION"));
        types.add("NO_LOSS_CONFIRMED");
        Collections.sort(types);
        for (String type : types) {
            long id = switch (type) {
                case "ACTIVE_ORDER" -> 8101;
                case "WORK_ORDER" -> 3001;
                case "MATERIAL_ISSUE" -> 4101;
                case "MATERIAL_ISSUE_LINE" -> 4201;
                case "COMPLETION_BACKFILL_RECEIPT" -> 5001;
                case "BATCH_PROVISION_RECEIPT" -> 7001;
                default -> 10000 + types.indexOf(type);
            };
            String snapshot = JSON.toJSONString(Map.of("sourceType", type, "id", id, "observed", "current-fixture"));
            String hash = MesProEdhrBatchTraceSourceHash.calculate(type, snapshot);
            if (type.equals("MATERIAL_ISSUE") || type.equals("MATERIAL_ISSUE_LINE")) {
                snapshot = "{\"bindingId\":4201,\"pickListId\":4101}";
                hash = MesProEdhrBatchTraceSourceHash.calculate(type, snapshot);
            }
            sources.add(new MesProEdhrBatchTraceSource().setLinkType(type).setSourceObjectType(type)
                    .setSourceObjectId(id).setSourceIdentityKey(type + ":" + type + ":" + id + "::")
                    .setSnapshotJson(snapshot).setSnapshotHash(hash)
                    .setRelationStatus(type.equals("NO_LOSS_CONFIRMED") ? "NO_LOSS_CONFIRMED" : "BOUND"));
        }
        sourceHash = source(sources, "BATCH_PROVISION_RECEIPT").getSnapshotHash();
        bundleHash = new MesProEdhrBatchTraceabilityValidator().calculateSourceBundleHash(sources);
        String receiptHash = source(sources, "COMPLETION_BACKFILL_RECEIPT").getSnapshotHash();
        String bindingHash = source(sources, "MATERIAL_ISSUE").getSnapshotHash();
        JSONArray evidence = new JSONArray();
        for (var item : sources) {
            JSONObject json = new JSONObject(true);
            json.put("sourceType", item.getLinkType());
            json.put("sourceObjectType", item.getSourceObjectType());
            json.put("sourceObjectId", item.getSourceObjectId());
            json.put("sourceIdentityKey", item.getSourceIdentityKey());
            json.put("snapshotJson", item.getSnapshotJson());
            json.put("sourceSnapshotHash", item.getSnapshotHash());
            json.put("relationStatus", item.getRelationStatus());
            evidence.add(json);
        }
        JSONObject metadata = new JSONObject(true);
        metadata.put("entryType", "ACTIVE_ORDER_COMPLETION");
        metadata.put("originKey", "CURRENT:8101:1");
        metadata.put("activeOrderId", 8101L);
        metadata.put("workOrderId", 3001L);
        metadata.put("sourceSnapshotHash", sourceHash);
        metadata.put("sourceBundleHash", bundleHash);
        metadata.put("sourceVersion", "1");
        metadata.put("sourceCredentialId", "5001");
        metadata.put("completionTransactionId", "current-completion");
        metadata.put("completionVersion", 1);
        metadata.put("completionBackfillReceiptId", 5001L);
        metadata.put("completionBackfillReceiptHash", receiptHash);
        metadata.put("batchProvisionReceiptId", 7001L);
        metadata.put("batchProvisionStatus", "BATCH_PROVISIONING");
        metadata.put("hasActualLoss", false);
        metadata.put("pickListSources", List.of(Map.of("pickListBindingId", 4201L, "pickListId", 4101L,
                "bindingVersion", 1, "sourceSnapshotHash", bindingHash)));
        metadata.put("sourceEvidence", evidence);
        jdbc.update("INSERT INTO " + BATCH + "(id,tenant_id,provisioning_status,status) VALUES(9001,1,'BATCH_PROVISIONING',10)");
        jdbc.update("INSERT INTO " + PROVISION + "(id,tenant_id,batch_execution_id,status,source_snapshot_hash,"
                + "source_bundle_hash,attempt_count) VALUES(7001,1,9001,'BATCH_PROVISIONING',?,?,0)", sourceHash, bundleHash);
        jdbc.update("INSERT INTO mes_pro_edhr_operation_audit_event(id,batch_execution_id,operation_type,result_status,"
                + "metadata_json,audit_hash) VALUES(6001,9001,'OPEN','SUCCESS',?,'current-parent-fact')", metadata.toJSONString());
        jdbc.update("INSERT INTO mes_pro_process_pool_active_order_pick_list_binding(id,tenant_id,active_order_id,"
                + "work_order_id,pick_list_id,binding_status,binding_version,source_snapshot_hash)"
                + " VALUES(4201,1,8101,3001,4101,'BOUND',1,?)", bindingHash);
        jdbc.update("INSERT INTO mes_pro_process_pool_active_order_pick_list_binding_item(id,tenant_id,binding_id,"
                + "item_snapshot_hash) VALUES(4301,1,4201,'current-line')");
        when(receiptPort.getByReceiptId(5001L, 1L)).thenReturn(new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(5001L).setTenantId(1L).setWorkOrderId(3001L).setActiveOrderId(8101L)
                .setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setSourceSnapshotHash(sourceHash).setReceiptHash(receiptHash).setHasActualLoss(false));
    }

    private MesProEdhrBatchTraceSource source(List<MesProEdhrBatchTraceSource> sources, String type) {
        return sources.stream().filter(s -> type.equals(s.getLinkType())).findFirst().orElseThrow();
    }

    private MesProEdhrBatchTraceTxCCommand command() {
        return new MesProEdhrBatchTraceTxCCommand().setBatchExecutionId(9001L).setProvisioningReceiptId(7001L)
                .setEventId("current-txc-event").setIdempotencyKey("current-txc-key")
                .setExpectedSourceSnapshotHash(sourceHash).setExpectedSourceBundleHash(bundleHash)
                .setExpectedSourceVersion("1").setCapturedBy(0L);
    }

    private MesProEdhrBatchProvisionedEvent event() {
        return new MesProEdhrBatchProvisionedEvent().setTenantId(1L).setBatchExecutionId(9001L)
                .setProvisioningReceiptId(7001L).setEventId("current-txc-event").setIdempotencyKey("current-txc-key")
                .setExpectedSourceSnapshotHash(sourceHash).setExpectedSourceBundleHash(bundleHash)
                .setExpectedSourceVersion("1").setCapturedBy(0L);
    }

    private void actor(Long tenant, boolean authenticated) {
        TenantContextHolder.setTenantId(tenant);
        SecurityContextHolder.clearContext();
        if (authenticated) {
            // Synthetic test principal, not a production actor fallback.
            LoginUser user = new LoginUser();
            user.setId(71L);
            user.setTenantId(tenant);
            user.setUserType(2);
            user.setInfo(Map.of("username", "txc-current-fixture", LoginUser.INFO_KEY_NICKNAME, "Tx-C fixture"));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, List.of()));
        }
    }

    private GxpAuditEventDO audit(String operation) {
        assertEquals(1, count(AUDIT), "Exactly one unified Tx-C fact, never parent success reused");
        var audit = mapper(GxpAuditEventMapper.class).selectList().get(0);
        assertEquals(operation, audit.getOperationId());
        assertEquals("MES_BATCH_TRACE_PROVISIONING", audit.getSubjectType());
        assertEquals("9001", audit.getSubjectId());
        assertEquals("PRESENT", audit.getBeforeState());
        assertEquals("PRESENT", audit.getAfterState());
        assertNotNull(audit.getTransactionId());
        return audit;
    }

    private JsonNode domainState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("batch", mapper(MesProEdhrBatchExecutionMapper.class).selectById(9001L));
        state.put("provisioning", mapper(MesProEdhrBatchProvisioningRecordMapper.class).selectByIdAndTenantId(1L, 7001L));
        state.put("origins", mapper(MesProEdhrBatchExecutionOriginMapper.class).selectListByBatchExecutionId(9001L));
        state.put("traceLinks", mapper(MesProEdhrBatchExecutionTraceLinkMapper.class).selectListByBatchExecutionId(9001L));
        state.put("manifests", mapper(MesProEdhrBatchExecutionTraceManifestMapper.class).selectListByBatchExecutionId(9001L));
        state.put("outbox", mapper(MesProEdhrBatchTraceOutboxEventMapper.class).selectListByBatchExecutionId(9001L));
        return JsonUtils.parseTree(JsonUtils.toJsonString(state));
    }

    private void assertEnvelope(GxpAuditEventDO audit, JsonNode before, JsonNode after) {
        assertEquals(before, JsonUtils.parseTree(audit.getBeforeStateJson()), "Before must be actual pre-write rows");
        assertEquals(after, JsonUtils.parseTree(audit.getAfterStateJson()), "After must include actual graph/outbox rows");
    }

    private Map<String, List<Map<String, Object>>> allRows() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        Map<String, List<Map<String, Object>>> snapshot = new TreeMap<>();
        tables.forEach(table -> snapshot.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1")));
        return snapshot;
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private String status(String table, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM " + table, String.class);
    }

    private int firstSql(String table, String text) {
        for (int i = 0; i < statements.size(); i++)
            if (statements.get(i).contains(table) && statements.get(i).contains(text)) return i;
        return -1;
    }

    private String parentTransactionId() {
        try {
            return ReflectionTestUtils.invokeMethod(Class.forName(
                    "cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditTransactionContext"), "requireTransactionId");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private void installTraceFailure() {
        jdbc.execute("CREATE TRIGGER fail_txc_trace BEFORE INSERT ON " + MANIFEST
                + " FOR EACH ROW CALL '" + TraceManifestFailure.class.getName() + "'");
    }

    public static class TraceManifestFailure implements Trigger {
        @Override public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            TRACE_FAILURE_SAW_WRITES.set(scalar(connection, "SELECT COUNT(*) FROM " + ORIGIN) == 1
                    && scalar(connection, "SELECT COUNT(*) FROM " + LINK) >= 17);
            throw new SQLException("Injected trace manifest storage failure after real graph writes");
        }
    }

    public static class AuditRelationFailure implements Trigger {
        @Override public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            // Permit one relation so rollback checks also cover an already-inserted relation.
            if (scalar(connection, "SELECT COUNT(*) FROM gxp_audit_event_relation") == 0) return;
            String expected = expectedFailureState ? "BATCH_PROVISIONING_RETRYABLE" : "BATCH_READY";
            AUDIT_FAILURE_SAW_WRITES.set(
                    scalar(connection, "SELECT COUNT(*) FROM " + BATCH + " WHERE provisioning_status='" + expected + "'") == 1
                    && scalar(connection, "SELECT COUNT(*) FROM " + PROVISION + " WHERE status='" + expected + "'") == 1
                    && scalar(connection, "SELECT COUNT(*) FROM " + OUTBOX) == 1
                    && scalar(connection, "SELECT COUNT(*) FROM " + ORIGIN) == (expectedFailureState ? 0 : 1)
                    && scalar(connection, "SELECT COUNT(*) FROM " + MANIFEST) == (expectedFailureState ? 0 : 1)
                    && scalar(connection, "SELECT COUNT(*) FROM " + AUDIT) == 1
                    && scalar(connection, "SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=1") == 2);
            throw new SQLException("Injected GxP relation failure after real Tx-C business and audit writes");
        }
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            if (!rows.next()) throw new SQLException("Required current fixture row absent");
            return rows.getLong(1);
        }
    }

    static class MappingObserver {
        final List<MesProEdhrBatchTraceMappingEvent> events = new ArrayList<>();
        @EventListener public void mapping(MesProEdhrBatchTraceMappingEvent event) { events.add(event); }
    }

    static class CountingTransactions extends DataSourceTransactionManager {
        int begins, commits, rollbacks;
        CountingTransactions(DriverManagerDataSource dataSource) { super(dataSource); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            super.doBegin(transaction, definition);
            begins++;
        }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            super.doCommit(status);
            commits++;
        }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            super.doRollback(status);
            rollbacks++;
        }
    }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    public static class ObservedSql implements Interceptor {
        private final List<String> statements;
        ObservedSql(List<String> statements) { this.statements = statements; }
        @Override public Object intercept(Invocation invocation) throws Throwable {
            var bound = ((StatementHandler) invocation.getTarget()).getBoundSql();
            String sql = bound.getSql().replace("b'0'", "0").replace("b'1'", "1");
            ReflectionTestUtils.setField(bound, "sql", sql);
            statements.add(sql.toLowerCase(Locale.ROOT));
            return invocation.proceed();
        }
    }

    private <T> T mapper(Class<T> type) { return session.getMapper(type); }
    private <T> void registerMapper(Class<T> type) { context.registerBean(type, () -> mapper(type)); }
    private Object tx(Object target) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
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
                        : field.getType() == Boolean.class ? "INT DEFAULT 0" : field.getType() == BigDecimal.class ? "DECIMAL(24,8)"
                        : field.getType() == LocalDateTime.class ? "TIMESTAMP" : field.getType() == LocalDate.class ? "DATE"
                        : "VARCHAR(1000000)";
                if (name.equals("id")) sqlType = "BIGINT AUTO_INCREMENT PRIMARY KEY";
                else if (field.isAnnotationPresent(TableId.class)) sqlType += " PRIMARY KEY";
                columns.add("`" + name + "` " + sqlType);
            }
        }
        String table = row.getAnnotation(TableName.class).value();
        jdbc.execute("CREATE TABLE " + table + "(" + String.join(",", columns) + ")");
        tables.add(table);
    }
}
