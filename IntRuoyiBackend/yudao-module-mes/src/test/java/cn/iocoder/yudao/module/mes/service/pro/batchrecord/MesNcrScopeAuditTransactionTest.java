package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderReworkCycleService;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementClaimDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementGrantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.h2.api.Trigger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Current NCR paths only. Real MyBatis business writes, rework/task services, specialized ledger,
 * independent signature verification and GxpAuditService share one physical H2/Spring transaction.
 * Signature issuance/password authentication and the external entitlement API are explicit doubles.
 * Test-only MySQL bit translation is not production compatibility or an InnoDB isolation proof.
 */
@org.junit.jupiter.api.parallel.Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
class MesNcrScopeAuditTransactionTest {
    private static final String REVIEW = "mes_pro_edhr_nonconformance_review";
    private static final String COUNTER = REVIEW + "_counter";
    private static final String WORK = "mes_pro_work_order";
    private static final String BATCH = "mes_pro_edhr_batch_execution";
    private static final String ACTIVE = "mes_pro_process_pool_active_order";
    private static final String APPLICATION = ACTIVE + "_release_application";
    private static final String SNAPSHOT = ACTIVE + "_process_snapshot";
    private static final String PQC = "mes_pqc_inspection_task";
    private static final String TASK = "mes_pro_edhr_work_task";
    private static final String RELEASE = "mes_pro_edhr_release_transaction";
    private static final String SPECIALIZED = "mes_pro_edhr_operation_audit_event";
    private static final String SIGNATURE = "system_electronic_signature";
    private static final String CREATE = "mes.nonconformance.active-order.create";
    private static final AtomicBoolean FAILURE_SAW_WRITES = new AtomicBoolean();
    private static String expectedDisposition;
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactions;
    private MesProEdhrNonconformanceReviewServiceImpl service;
    private ElectronicSignatureRecordMapper signatures;
    private ElectronicSignatureQueryServiceImpl signatureQuery;
    private MesProBatchRecordExecutionSignatureService signer;
    private PermissionApi permissionBoundary;
    private final List<String> tables = new ArrayList<>();

    @BeforeEach
    void fixture() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:ncr_scope_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        for (Class<?> row : List.of(MesProEdhrNonconformanceReviewDO.class,
                MesProEdhrNonconformanceReviewCounterDO.class, MesProWorkOrderDO.class,
                MesProEdhrBatchExecutionDO.class, MesProEdhrBatchExecutionOriginDO.class,
                MesProcessPoolActiveOrderDO.class, MesProcessPoolActiveOrderReleaseApplicationDO.class,
                MesProcessPoolActiveOrderProcessSnapshotDO.class, MesPqcInspectionTaskDO.class,
                MesProEdhrWorkTaskDO.class, MesProEdhrReleaseTransactionDO.class,
                SystemEntitlementClaimDO.class, SystemEntitlementGrantDO.class, SystemEntitlementAuditEventDO.class,
                MesProEdhrOperationAuditEventDO.class, FileDO.class, ElectronicSignatureRecordDO.class,
                GxpAuditEventDO.class, GxpAuditEventRelationDO.class, GxpAuditLedgerSequenceDO.class,
                GxpAuditPolicyActivationDO.class, GxpAuditPolicyOperationDO.class)) createTable(row);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addInterceptor(new MysqlBitsForH2());
        List.of(MesProEdhrNonconformanceReviewMapper.class, MesProEdhrNonconformanceReviewCounterMapper.class,
                MesProWorkOrderMapper.class, MesProEdhrBatchExecutionMapper.class, MesProEdhrBatchExecutionOriginMapper.class,
                MesProcessPoolActiveOrderMapper.class, MesProcessPoolActiveOrderReleaseApplicationMapper.class,
                MesProcessPoolActiveOrderProcessSnapshotMapper.class, MesPqcInspectionTaskMapper.class,
                MesProEdhrWorkTaskMapper.class, MesProEdhrReleaseTransactionMapper.class,
                MesProEdhrOperationAuditEventMapper.class, FileMapper.class, ElectronicSignatureRecordMapper.class,
                GxpAuditEventMapper.class, GxpAuditEventRelationMapper.class, GxpAuditLedgerSequenceMapper.class,
                GxpAuditPolicyActivationMapper.class, GxpAuditPolicyOperationMapper.class).forEach(config::addMapper);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(config);
        var session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        transactions = new DataSourceTransactionManager(dataSource);
        var writer = new GxpAuditServiceImpl();
        inject(writer, "auditEventMapper", session.getMapper(GxpAuditEventMapper.class));
        inject(writer, "eventRelationMapper", session.getMapper(GxpAuditEventRelationMapper.class));
        inject(writer, "ledgerSequenceMapper", session.getMapper(GxpAuditLedgerSequenceMapper.class));
        inject(writer, "policyActivationMapper", session.getMapper(GxpAuditPolicyActivationMapper.class));
        inject(writer, "policyOperationMapper", session.getMapper(GxpAuditPolicyOperationMapper.class));
        var specialized = new MesProEdhrOperationAuditServiceImpl();
        inject(specialized, "auditEventMapper", session.getMapper(MesProEdhrOperationAuditEventMapper.class));
        var tasks = new MesProEdhrWorkTaskServiceImpl();
        inject(tasks, "workTaskMapper", session.getMapper(MesProEdhrWorkTaskMapper.class));
        permissionBoundary = mock(PermissionApi.class);
        inject(tasks, "permissionApi", permissionBoundary);
        // Existing explicit no-write PermissionApi seam remains; use the real mandatory audit boundary.
        inject(tasks, "auxiliaryAudit", new MesWorkTaskAuxiliaryAudit(dataSource, (GxpAuditService) tx(writer)));
        signatures = session.getMapper(ElectronicSignatureRecordMapper.class);
        signatureQuery = new ElectronicSignatureQueryServiceImpl();
        inject(signatureQuery, "signatureRecordMapper", signatures);
        inject(signatureQuery, "subjectAdapters", List.of(new MesBatchRecordSignatureSubjectAdapter()));
        signer = mock(MesProBatchRecordExecutionSignatureService.class);
        when(signer.recordQaDispositionSignature(any(), any(), any(), any(), any())).thenAnswer(call ->
                insertSignature(call.getArgument(0), call.getArgument(1), call.getArgument(3), call.getArgument(4)));
        var target = new MesProEdhrNonconformanceReviewServiceImpl();
        Map<String, Class<?>> dependencies = Map.ofEntries(
                Map.entry("reviewMapper", MesProEdhrNonconformanceReviewMapper.class),
                Map.entry("reviewCounterMapper", MesProEdhrNonconformanceReviewCounterMapper.class),
                Map.entry("workOrderMapper", MesProWorkOrderMapper.class),
                Map.entry("batchExecutionMapper", MesProEdhrBatchExecutionMapper.class),
                Map.entry("batchExecutionOriginMapper", MesProEdhrBatchExecutionOriginMapper.class),
                Map.entry("activeOrderMapper", MesProcessPoolActiveOrderMapper.class),
                Map.entry("releaseApplicationMapper", MesProcessPoolActiveOrderReleaseApplicationMapper.class),
                Map.entry("processSnapshotMapper", MesProcessPoolActiveOrderProcessSnapshotMapper.class),
                Map.entry("pqcInspectionTaskMapper", MesPqcInspectionTaskMapper.class),
                Map.entry("workTaskMapper", MesProEdhrWorkTaskMapper.class),
                Map.entry("releaseTransactionMapper", MesProEdhrReleaseTransactionMapper.class),
                Map.entry("fileMapper", FileMapper.class), Map.entry("auditReceiptMapper", GxpAuditEventMapper.class));
        dependencies.forEach((name, type) -> inject(target, name, session.getMapper(type)));
        inject(target, "signatureService", signer);
        inject(target, "signatureRecordMapper", signatures);
        inject(target, "signatureQueryService", signatureQuery);
        inject(target, "operationAuditService", tx(specialized));
        inject(target, "workTaskService", tx(tasks));
        inject(target, "reworkCycleService", tx(new MesActiveOrderReworkCycleService(
                session.getMapper(MesProcessPoolActiveOrderMapper.class),
                session.getMapper(MesProcessPoolActiveOrderProcessSnapshotMapper.class),
                session.getMapper(MesPqcInspectionTaskMapper.class))));
        inject(target, "unifiedAudit", tx(writer));
        service = (MesProEdhrNonconformanceReviewServiceImpl) tx(target);
        TenantContextHolder.setTenantId(1L);
        var actor = new LoginUser();
        actor.setId(21L);
        actor.setTenantId(1L);
        actor.setUserType(2);
        actor.setInfo(Map.of("username", "ncr-transaction-fixture", LoginUser.INFO_KEY_NICKNAME, "NCR fixture"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
        jdbc.update("INSERT INTO gxp_audit_ledger_sequence(tenant_id,next_ledger_sequence) VALUES(1,1)");
        jdbc.update("INSERT INTO gxp_audit_policy_activation(id,tenant_id,policy_version) VALUES(1,1,'ncr-scope-fixture')");
        for (String operation : List.of(CREATE, "mes.nonconformance.rework", "mes.nonconformance.void")) {
            jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,domain,"
                    + "subject_type,action_type,reason_policy,signature_policy,state_policy,applicability,active)"
                    + " VALUES(1,'ncr-scope-fixture',?,'MES','NONCONFORMANCE_REVIEW',?,'USER_REQUIRED',?,?,'GXP',1)",
                    operation, CREATE.equals(operation) ? "CREATE" : "UPDATE",
                    CREATE.equals(operation) ? "NONE" : "REQUIRED",
                    CREATE.equals(operation) ? "ABSENT_TO_PRESENT" : "PRESENT_TO_PRESENT");
        }
        jdbc.update("INSERT INTO " + WORK + "(id,tenant_id,code,batch_code,temporary_frozen)"
                + " VALUES(3001,1,'WO-NCR','BATCH-NCR',0)");
        jdbc.update("INSERT INTO " + ACTIVE + "(id,tenant_id,work_order_id,leader_user_id,route_id,route_version_id,"
                + "qa_regulation_version_id,erp_fixed_quantity_snapshot,active_status,business_status,version)"
                + " VALUES(8101,1,3001,3002,4001,4002,5001,10,'ACTIVE','ACTIVE',1)");
        jdbc.update("INSERT INTO " + BATCH + "(id,tenant_id,work_order_id,batch_execution_code,work_order_code,batch_code,status)"
                + " VALUES(9001,1,3001,'EXEC-NCR','WO-NCR','BATCH-NCR',10)");
        FAILURE_SAW_WRITES.set(false);
        expectedDisposition = null;
    }

    @AfterEach
    void close() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        if (jdbc != null) jdbc.execute("SHUTDOWN");
    }

    @Test
    void unsignedCreateRelationFailureRollsBackReviewCounterFreezeAndBothAudits() {
        jdbc.update("INSERT INTO mes_pro_edhr_batch_execution_origin(id,tenant_id,batch_execution_id,"
                + "active_order_id,work_order_id,entry_type) VALUES(9101,1,9001,8101,3001,'ACTIVE_ORDER_COMPLETION')");
        Map<String, List<Map<String, Object>>> before = snapshot();
        installFailureTrigger();
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertThrows(RuntimeException.class, () -> service.create(new MesProEdhrNonconformanceReviewCreateReqVO()
                .setActiveOrderId(8101L).setNonconformanceReason("Observed nonconformance")));
        assertTrue(FAILURE_SAW_WRITES.get(), "Failure must observe real review/counter/freeze/specialized/event/relation writes");
        assertRestored(before);
        verifyNoInteractions(signer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rework", "void"})
    void dispositionRelationFailureRollsBackReleaseTasksSignatureCycleAndBothAudits(String disposition) {
        seedDisposition();
        expectedDisposition = disposition;
        Map<String, List<Map<String, Object>>> before = snapshot();
        installFailureTrigger();
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertThrows(RuntimeException.class, () -> service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L).setDisposition(disposition).setReviewOpinion("Observed disposition")
                .setSignaturePassword("test-only-issuance-boundary")
                .setReviewMaterials(List.of(new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO()
                        .setFileId(9102L).setUrl("https://fixture.invalid/review.pdf").setFileName("review.pdf").setSortNo(1)))));
        assertTrue(FAILURE_SAW_WRITES.get(), "Failure must follow actual downstream and audit writes, never setup failure");
        assertRestored(before);
        verify(permissionBoundary).revokeEntitlementSource(any());
    }

    private void seedDisposition() {
        jdbc.update("UPDATE " + WORK + " SET temporary_frozen=1 WHERE id=3001");
        jdbc.update("UPDATE " + BATCH + " SET status=15 WHERE id=9001");
        jdbc.update("UPDATE " + ACTIVE + " SET business_status='COMPLETED' WHERE id=8101");
        jdbc.update("INSERT INTO " + REVIEW + "(id,tenant_id,source_type,source_id,active_order_id,"
                + "work_order_id,batch_execution_id,review_status,previous_batch_status,previous_work_order_temporary_frozen,"
                + "frozen_at,nonconformance_reason) VALUES(1001,1,'ACTIVE_ORDER',8101,8101,3001,9001,"
                + "'pending_review',10,0,'2026-09-29 08:00:00','Observed nonconformance')");
        jdbc.update("INSERT INTO " + APPLICATION + "(id,tenant_id,active_order_id,work_order_id,application_status,"
                + "version,pqc_release_work_task_id,release_approval_work_task_id,release_transaction_id)"
                + " VALUES(7001,1,8101,3001,'MANAGER_RELEASE_PENDING',1,8001,8002,9301)");
        jdbc.update("INSERT INTO " + TASK + "(id,tenant_id,task_type,business_scope_type,business_scope_id,status)"
                + " VALUES(8001,1,'PQC_PRODUCTION_RELEASE','RELEASE_APPLICATION',7001,'TODO')");
        jdbc.update("INSERT INTO " + TASK + "(id,tenant_id,task_type,business_scope_type,business_scope_id,status,candidate_user_snapshot)"
                + " VALUES(8002,1,'RELEASE_APPROVE','RELEASE_TRANSACTION',9301,'TODO','[21]')");
        jdbc.update("INSERT INTO " + RELEASE + "(id,tenant_id,release_status,version)"
                + " VALUES(9301,1,'PENDING_APPROVAL',4)");
        jdbc.update("INSERT INTO infra_file(id,config_id,name,path,url,size,type)"
                + " VALUES(9102,10,'review.pdf','mes/edhr-ncr/reviews/1001/upload-1/review.pdf',"
                + "'https://fixture.invalid/review.pdf',10,'application/pdf')");
        for (long i = 1; i <= 2; i++) {
            jdbc.update("INSERT INTO " + SNAPSHOT + "(id,tenant_id,active_order_id,work_order_id,route_id,route_version_id,"
                    + "route_process_id,production_config_snapshot_json) VALUES(?,1,8101,3001,4001,4002,?,?)",
                    4100 + i, 5100 + i, "{\"frozen\":" + i + "}");
            jdbc.update("INSERT INTO " + PQC + "(id,tenant_id,active_order_id,work_order_id,route_id,route_version_id,"
                    + "route_process_id,task_status,actual_inspection_quantity,submitted_event_id,submitted_content_hash)"
                    + " VALUES(?,1,8101,3001,4001,4002,?,'CONFIRMED',10,?,'prior-inspection')",
                    6100 + i, 5100 + i, 7100 + i);
        }
    }

    private Long insertSignature(Long actor, Long review, String reason, String aggregateHash) {
        String action = "QA_DISPOSITION";
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action,
                null, null, null, null, null, null, null, "EDHR_NONCONFORMANCE_REVIEW", review,
                "eDHR不合格评审处置", action, null, null, aggregateHash, null);
        String version = MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject);
        var adapter = new MesBatchRecordSignatureSubjectAdapter();
        var snapshot = adapter.loadAndAuthorize(new SignatureSubjectCommand(actor, "MES", action,
                "MES_BATCH_RECORD", subject, version, reason));
        String canonical = JsonUtils.toJsonString(sorted(JsonUtils.parseTree(snapshot.canonicalContentJson())));
        var definition = adapter.supportedActions().stream().filter(a -> action.equals(a.actionCode())).findFirst().orElseThrow();
        var time = LocalDateTime.of(2026, 9, 29, 9, 0);
        var record = ElectronicSignatureRecordDO.builder().id(9101L).moduleCode("MES").actionCode(action)
                .subjectType("MES_BATCH_RECORD").subjectId(subject).subjectVersion(version).actorId(actor)
                .meaningCode(definition.meaningCode()).meaningLabel(definition.meaningLabel()).reason(reason)
                .signedAt(time).timeEvidenceId("SERVER_CLOCK:" + time).authenticationMethod("SESSION_PLUS_PASSWORD")
                .canonicalContentJson(canonical).contentHash(DigestUtil.sha256Hex(canonical)).algorithm("SHA-256")
                .keyVersion("system-local-v1").policyVersion(definition.policyVersion()).verificationStatus("VALID").build();
        record.setTenantId(1L);
        record.setEvidenceHash(DigestUtil.sha256Hex(String.join("|", "1", actor.toString(), "MES", action,
                "MES_BATCH_RECORD", subject, version, record.getMeaningCode(), record.getMeaningLabel(), reason,
                time.toString(), record.getTimeEvidenceId(), "SESSION_PLUS_PASSWORD", record.getContentHash(),
                "", "", "", "", "", "SHA-256", "system-local-v1", record.getPolicyVersion(), "VALID")));
        assertEquals(1, signatures.insert(record));
        assertEquals("VALID", signatureQuery.verifyEvidence(9101L).verificationStatus());
        return record.getId();
    }

    private void installFailureTrigger() {
        jdbc.execute("CREATE TRIGGER fail_ncr_relation BEFORE INSERT ON gxp_audit_event_relation FOR EACH ROW CALL '"
                + FailAfterWrites.class.getName() + "'");
    }

    public static class FailAfterWrites implements Trigger {
        @Override public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            // Let the first real relation persist; fail the second so relation rollback is also observed.
            if (scalar(connection, "SELECT COUNT(*) FROM gxp_audit_event_relation") < 1) return;
            boolean common = scalar(connection, "SELECT COUNT(*) FROM gxp_audit_event") == 1
                    && scalar(connection, "SELECT COUNT(*) FROM " + SPECIALIZED) == 1
                    && scalar(connection, "SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence") == 2;
            boolean changed;
            if (expectedDisposition == null) {
                changed = scalar(connection, "SELECT COUNT(*) FROM " + REVIEW + " WHERE review_status='pending_review'") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + COUNTER + " WHERE current_serial=0") == 1
                        && scalar(connection, "SELECT temporary_frozen FROM " + WORK + " WHERE id=3001") == 1
                        && scalar(connection, "SELECT status FROM " + BATCH + " WHERE id=9001") == 15
                        && scalar(connection, "SELECT COUNT(*) FROM " + SIGNATURE) == 0;
            } else {
                boolean rework = "rework".equals(expectedDisposition);
                changed = scalar(connection, "SELECT COUNT(*) FROM " + REVIEW + " WHERE id=1001 AND review_status='closed'") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + SIGNATURE) == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + APPLICATION
                            + " WHERE id=7001 AND application_status='PQC_RELEASE_REJECTED' AND version=2") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + TASK + " WHERE id=8001 AND status='DONE'") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + TASK + " WHERE id=8002 AND status='CANCELED'") == 1
                        && scalar(connection, "SELECT COUNT(*) FROM " + RELEASE
                            + " WHERE id=9301 AND release_status='REJECTED' AND version=5") == 1
                        && scalar(connection, "SELECT status FROM " + BATCH + " WHERE id=9001") == (rework ? 50 : 60)
                        && scalar(connection, "SELECT temporary_frozen FROM " + WORK + " WHERE id=3001") == (rework ? 0 : 1)
                        && scalar(connection, "SELECT COUNT(*) FROM " + ACTIVE + " WHERE rework_review_id=1001") == (rework ? 1 : 0)
                        && scalar(connection, "SELECT COUNT(*) FROM " + SNAPSHOT + " WHERE active_order_id<>8101") == (rework ? 2 : 0)
                        && scalar(connection, "SELECT COUNT(*) FROM " + PQC + " WHERE active_order_id<>8101"
                            + " AND task_status='PENDING' AND actual_inspection_quantity=0") == (rework ? 2 : 0)
                        && scalar(connection, "SELECT COUNT(*) FROM " + ACTIVE
                            + " WHERE id=8101 AND active_status='REMOVED' AND business_status='REWORKED' AND version=2") == (rework ? 1 : 0);
            }
            FAILURE_SAW_WRITES.set(common && changed);
            throw new SQLException("Injected NCR relation failure after actual domain and ledger writes");
        }
        private static long scalar(Connection connection, String query) throws SQLException {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(query)) {
                if (!rows.next()) throw new SQLException("Required transaction evidence row absent");
                return rows.getLong(1);
            }
        }
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> result = new TreeMap<>();
        tables.forEach(table -> result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1")));
        return result;
    }

    private void assertRestored(Map<String, List<Map<String, Object>>> before) {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "Read back outside the service transaction");
        assertEquals(before, snapshot(), "Every actual business, signature, specialized/unified audit row and watermark must restore");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event_relation", Integer.class));
        assertEquals(1L, jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence", Long.class));
    }

    private static JsonNode sorted(JsonNode node) {
        if (!node.isObject()) return node;
        ObjectNode result = JsonUtils.getObjectMapper().createObjectNode();
        TreeSet<String> keys = new TreeSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        keys.forEach(key -> result.set(key, sorted(node.get(key))));
        return result;
    }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    public static class MysqlBitsForH2 implements Interceptor {
        @Override public Object intercept(Invocation invocation) throws Throwable {
            var bound = ((StatementHandler) invocation.getTarget()).getBoundSql();
            ReflectionTestUtils.setField(bound, "sql", bound.getSql().replace("b'0'", "0").replace("b'1'", "1"));
            return invocation.proceed();
        }
    }

    private Object tx(Object target) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }
    private static void inject(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
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
        // These BaseDO mappings omit tenant_id present in both production table definitions.
        if (row == MesProEdhrWorkTaskDO.class || row == MesProEdhrReleaseTransactionDO.class) {
            columns.add("`tenant_id` BIGINT NOT NULL DEFAULT 0");
        }
        jdbc.execute("CREATE TABLE " + table + "(" + String.join(",", columns) + ")");
        tables.add(table);
    }
}
