package cn.iocoder.yudao.module.mes.service.pro.productionrelease;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializer;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Real parent/H2 transactions; the manager seam persists the initializer's ten readiness/time columns. */
@Import({MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.class,
        MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.class,
        MesPqcProductionReleaseServiceImpl.class, MesReleaseAffectedStateCollector.class})
@org.springframework.test.context.TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:release_parent_affected_state;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value,day")
class MesReleaseParentAffectedStateTest extends BaseDbUnitTest {
    @org.springframework.boot.test.mock.mockito.MockBean private cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService handoffService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrBatchLifecycleGuard lifecycleGuard;

    private static final LocalDateTime PRIOR_PRECHECK_AT = LocalDateTime.of(2026, 9, 28, 8, 10);
    private static final List<String> MANAGER_AUDIT_FIELDS = List.of("lastPrecheckAt",
            "dhrStatus", "inspectionStatus", "deviationStatus", "reworkStatus", "scrapStatus", "inventoryStatus",
            "requiredCheckCount", "failedCheckCount", "blockingCheckCount");
    @Resource private MesTeamLeaderActiveOrderReleaseApplicationService applicationService;
    @Resource private MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistence;
    @Resource private MesPqcProductionReleaseService pqcService;
    @MockitoSpyBean private MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    @Resource private MesProEdhrWorkTaskMapper tasks;
    @Resource private DataSource dataSource;
    @MockitoBean private MesTeamLeaderActiveOrderReleaseGenerationService generation;
    @MockitoBean private MesTeamLeaderActiveOrderCompletionService completion;
    @MockitoBean private MesTeamLeaderActiveOrderCompletionBatchExecutionService completionBatch;
    @MockitoBean private MesProcessPoolActiveOrderCompletionReceiptMapper receipts;
    @MockitoBean private MesProcessPoolActiveOrderMapper activeOrders;
    @MockitoBean private MesPqcReleaseDossierPort dossier;
    @MockitoBean private MesProductionReleaseBatchExecutionPort batchPort;
    @MockitoBean private MesProductionReleaseReportStageInitializer reportStage;
    @MockitoBean private MesProductionReleaseManagerStageInitializer managerStage;
    @MockitoBean private MesReleaseFlowAuditRecorder specializedAudit;
    @MockitoBean private MesProBatchRecordExecutionSignatureService signatures;
    @MockitoBean private MesProEdhrNonconformanceReviewService nonconformance;
    @MockitoBean private MesReleaseTaskNotificationService notificationService;
    @MockitoBean private GxpAuditService audit;
    private JdbcTemplate jdbc;

    @BeforeEach
    void fixtures() {
        // This existing BaseDbUnitTest fixture has tenant_id=0 defaults, without the tenant plugin.
        TenantContextHolder.setTenantId(0L);
        jdbc = new JdbcTemplate(dataSource);
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("sql/release-parent-ncr-fixture.sql"))
                .execute(dataSource);
        jdbc.update("INSERT INTO mes_pro_work_order (id, code, status, tenant_id) VALUES (9300, 'RELEASE-SCOPE-WO', 1, 0)");
        when(activeOrders.selectByIdForUpdate(9200L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(9200L).setWorkOrderId(9300L).setVersion(4)
                .setActiveStatus("ACTIVE").setBusinessStatus("COMPLETED").setUdiControlDocumentNo("UDI-SCOPE"));
    }

    @AfterEach
    void removeOwnedApplicationFixture() {
        jdbc.update("DELETE FROM mes_pro_process_pool_active_order_release_application WHERE active_order_id=9200 AND tenant_id=0");
    }

    @Test
    void applyAuditIncludesPersistedBatchBindingAndFrozenPqcCandidates() {
        when(completion.completeForRelease(20L, 9200L, "release-scope-apply", true))
                .thenReturn(new MesTeamLeaderActiveOrderCompletionResult().setCompletionReceiptId(9400L));
        when(completionBatch.openOrCreate(20L, 9200L, 9400L, "release-scope-apply")).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            insertBatch();
            jdbc.update("INSERT INTO mes_pro_edhr_batch_execution_task (id, batch_execution_id, route_process_sort, route_binding_id, form_binding_key, batch_record_version_id, tenant_id) VALUES (9501, 9500, 1, 9510, 'formal-binding', 9511, 0)");
            jdbc.update("INSERT INTO mes_pro_edhr_batch_execution_origin (id, batch_execution_id, entry_type, origin_key, active_order_id, work_order_id, completion_backfill_receipt_id, source_snapshot_hash, batch_provision_receipt_id, batch_provision_status, source_bundle_hash, idempotency_key, relation_status, captured_at, tenant_id) VALUES (9502, 9500, 'ACTIVE_ORDER_COMPLETION', 'scope-origin', 9200, 9300, 9400, 'formal-source', 9503, 'SUCCESS', 'bundle-hash', 'scope-origin-key', 'ACTIVE', CURRENT_TIMESTAMP, 0)");
            tasks.insert(task(9504L, "FILL", "BATCH_TASK", 9501L).setBatchTaskId(9501L));
            return 9500L;
        });
        when(generation.generate(eq(20L), any())).thenAnswer(invocation -> persistence.persistPending(
                application(), new MesProductionReleaseRoleCandidates(44L, "PQC", List.of(7101L, 7102L), "pqc-candidates")));

        var result = applicationService.apply(20L, new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(9200L).setConfirmNoReplenishmentInfo(true).setIdempotencyKey("release-scope-apply"));

        var persisted = applications.selectById(result.getApplicationId());
        var pqcTask = tasks.selectById(persisted.getPqcReleaseWorkTaskId());
        assertEquals(9500L, persisted.getBatchExecutionId());
        assertEquals("7101,7102", pqcTask.getCandidateUserSnapshot());
        var written = onlyAudit("mes.pqc-release.apply");
        var before = affected(written, true);
        var after = affected(written, false);
        assertTrue(before.getJSONArray("batches").isEmpty(), "the batch did not exist before apply");
        assertTrue(before.getJSONArray("workTasks").isEmpty());
        assertEquals(9500L, row(after, "batches", 9500L).getLong("id"));
        assertEquals("formal-binding", row(after, "batchTasks", 9501L).getString("formBindingKey"));
        assertEquals(9511L, row(after, "batchTasks", 9501L).getLong("batchRecordVersionId"));
        assertEquals(9400L, row(after, "batchOrigins", 9502L).getLong("completionBackfillReceiptId"));
        assertEquals("FILL", row(after, "workTasks", 9504L).getString("taskType"));
        var frozen = row(after, "workTasks", pqcTask.getId());
        assertEquals(pqcTask.getAssigneeUserId(), frozen.getLong("assigneeUserId"));
        assertEquals(pqcTask.getCandidateUserSnapshot(), frozen.getString("candidateUserSnapshot"));
        assertEquals(pqcTask.getResponsibilitySourceDigest(), frozen.getString("responsibilitySourceDigest"));
        assertEquals(persisted.getPqcReleaseWorkTaskId(), row(after, "releaseApplications", persisted.getId())
                .getLong("pqcReleaseWorkTaskId"));
        when(generation.replayExisting(eq(20L), any())).thenReturn(result);
        applicationService.apply(20L, new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(9200L).setConfirmNoReplenishmentInfo(true).setIdempotencyKey("release-scope-apply"));
        verify(audit).append(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pqcAuditIncludesActualNewOrPromotedManagerStageAndCompletedPqcTask(boolean promote) {
        seedPqcApproval(promote);
        var persistedBefore = promote ? readManagerTransaction(9800L) : null;
        pqcService.approve(7101L, approveCommand());
        var persistedApplication = applications.selectById(9600L);
        Long transactionId = persistedApplication.getReleaseTransactionId();
        Long managerTaskId = persistedApplication.getReleaseApprovalWorkTaskId();
        assertNotNull(transactionId);
        assertNotNull(managerTaskId);
        var persistedAfter = readManagerTransaction(transactionId);
        assertNotNull(persistedAfter.getLastPrecheckAt(), "initializer seam must actually persist its precheck time");
        assertNotEquals(PRIOR_PRECHECK_AT, persistedAfter.getLastPrecheckAt());
        assertEquals(6, persistedAfter.getRequiredCheckCount());
        assertEquals(0, persistedAfter.getFailedCheckCount());
        assertEquals(0, persistedAfter.getBlockingCheckCount());
        assertEquals("DONE", tasks.selectById(9700L).getStatus());
        assertEquals("PENDING_APPROVAL", persistedAfter.getReleaseStatus());
        var written = onlyAudit("mes.pqc.production-release.approve");
        var before = affected(written, true);
        var after = affected(written, false);
        if (promote) {
            assertEquals("PRECHECK_PASSED", row(before, "releaseTransactions", 9800L).getString("releaseStatus"));
            assertEquals(7, row(before, "releaseTransactions", 9800L).getIntValue("version"));
            assertEquals("{\"prior\":true}", row(before, "releaseTransactions", 9800L).getString("precheckSnapshotJson"));
        } else {
            assertTrue(before.getJSONArray("releaseTransactions").isEmpty());
        }
        assertEquals("PENDING_APPROVAL", row(after, "releaseTransactions", transactionId).getString("releaseStatus"));
        assertEquals(promote ? 8 : 1, row(after, "releaseTransactions", transactionId).getIntValue("version"));
        assertEquals("TODO", row(before, "workTasks", 9700L).getString("status"));
        assertEquals("DONE", row(after, "workTasks", 9700L).getString("status"));
        var managerTask = row(after, "workTasks", managerTaskId);
        assertEquals("7201,7202", managerTask.getString("candidateUserSnapshot"));
        assertEquals(7201L, managerTask.getLong("assigneeUserId"));
        assertEquals("manager-candidates", managerTask.getString("responsibilitySourceDigest"));
        assertEquals(managerTaskId, row(after, "releaseApplications", 9600L).getLong("releaseApprovalWorkTaskId"));
        assertEquals(3, row(after, "releaseApplications", 9600L).getIntValue("version"));
        assertAll("actual manager readiness columns before and after",
                () -> {
                    if (promote) assertManagerReadiness(row(before, "releaseTransactions", transactionId), persistedBefore);
                },
                () -> assertManagerReadiness(row(after, "releaseTransactions", transactionId), persistedAfter));
        pqcService.approve(7101L, approveCommand());
        verify(audit).append(any());
        verify(managerStage).initializeManagerReleaseStage(any());
        assertEquals(managerTaskId, applications.selectById(9600L).getReleaseApprovalWorkTaskId());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_work_task WHERE business_scope_type='RELEASE_TRANSACTION' AND business_scope_id=?", Integer.class, transactionId));
    }

    @Test
    void pqcAuditFailureRollsBackPersistedManagerPromotionTaskAndApplication() {
        seedPqcApproval(true);
        var persistedBefore = readManagerTransaction(9800L);
        doAnswer(invocation -> {
            var written = readManagerTransaction(9800L);
            assertEquals("PENDING_APPROVAL", written.getReleaseStatus());
            assertEquals("PASS", written.getDhrStatus());
            assertNotEquals(persistedBefore.getLastPrecheckAt(), written.getLastPrecheckAt());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_work_task WHERE business_scope_type='RELEASE_TRANSACTION' AND business_scope_id=9800", Integer.class));
            throw new IllegalStateException("release-scope audit failure");
        }).when(audit).append(any());
        var error = assertThrows(IllegalStateException.class, () -> pqcService.approve(7101L, approveCommand()));
        assertEquals("release-scope audit failure", error.getMessage());
        assertEquals("PQC_RELEASE_PENDING", applications.selectById(9600L).getApplicationStatus());
        assertEquals(1, applications.selectById(9600L).getVersion());
        assertEquals("TODO", tasks.selectById(9700L).getStatus());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_work_task WHERE business_scope_type='RELEASE_TRANSACTION' AND business_scope_id=9800", Integer.class));
        assertEquals("PRECHECK_PASSED", jdbc.queryForObject(
                "SELECT release_status FROM mes_pro_edhr_release_transaction WHERE id=9800", String.class));
        assertEquals(7, jdbc.queryForObject("SELECT version FROM mes_pro_edhr_release_transaction WHERE id=9800", Integer.class));
        assertManagerReadiness(JSON.parseObject(JsonUtils.toJsonString(readManagerTransaction(9800L))), persistedBefore);
    }

    private void seedPqcApproval(boolean promote) {
        insertBatch();
        var application = application().setId(9600L).setBatchExecutionId(9500L).setPqcReleaseWorkTaskId(9700L);
        applications.insert(application);
        tasks.insert(task(9700L, "PQC_PRODUCTION_RELEASE", "RELEASE_APPLICATION", 9600L)
                .setAssigneeUserId(7101L).setCandidateUserSnapshot("7101,7102"));
        // The production MySQL bit literal and JSON_SET are not H2 syntax. Keep actual locks/CAS/writes.
        doAnswer(invocation -> jdbc.queryForObject(
                "SELECT * FROM mes_pro_process_pool_active_order_release_application WHERE id=? AND deleted=FALSE FOR UPDATE",
                org.springframework.jdbc.core.BeanPropertyRowMapper.newInstance(MesProcessPoolActiveOrderReleaseApplicationDO.class),
                (Object) invocation.getArgument(0))).when(applications).selectByIdForUpdate(9600L);
        doAnswer(invocation -> jdbc.update(
                "UPDATE mes_pro_process_pool_active_order_release_application SET application_status='MANAGER_RELEASE_PENDING', batch_execution_id=?, pqc_decision='APPROVE', pqc_decided_by=?, pqc_decided_at=?, pqc_reject_reason=NULL, report_snapshot_hash=?, dossier_summary_json=?, version=version+1 WHERE id=? AND deleted=FALSE AND version=? AND application_status='PQC_RELEASE_PENDING'",
                invocation.getArgument(2), invocation.getArgument(3), invocation.getArgument(4), invocation.getArgument(5),
                invocation.getArgument(6), invocation.getArgument(0), invocation.getArgument(1)))
                .when(applications).approveFromPending(eq(9600L), eq(1), eq(9500L), eq(7101L), any(), any(), any());
        doAnswer(invocation -> {
            // Read the JDBC CAS result directly; a mapper session may still cache its earlier observation.
            var receipt = JSON.parseObject(jdbc.queryForObject(
                    "SELECT dossier_summary_json FROM mes_pro_process_pool_active_order_release_application WHERE id=9600 AND deleted=FALSE FOR UPDATE",
                    String.class));
            receipt.put("managerCandidateSnapshotHash", invocation.getArgument(5));
            return jdbc.update("UPDATE mes_pro_process_pool_active_order_release_application SET application_status='MANAGER_RELEASE_PENDING', report_snapshot_hash=?, release_transaction_id=?, release_approval_work_task_id=?, dossier_summary_json=?, version=version+1 WHERE id=? AND version=? AND deleted=FALSE AND application_status IN ('REPORT_UPLOAD_PENDING','MANAGER_RELEASE_PENDING')",
                    invocation.getArgument(2), invocation.getArgument(3), invocation.getArgument(4), JSON.toJSONString(receipt),
                    invocation.getArgument(0), invocation.getArgument(1));
        }).when(applications).handoffReportsToManager(eq(9600L), eq(2), any(), anyLong(), anyLong(), eq("manager-candidates"));
        if (promote) jdbc.update("INSERT INTO mes_pro_edhr_release_transaction (id, release_code, batch_execution_id, work_order_id, release_status, version, precheck_snapshot_json, last_precheck_at, dhr_status, inspection_status, deviation_status, rework_status, scrap_status, inventory_status, required_check_count, failed_check_count, blocking_check_count, tenant_id) VALUES (9800, 'PRIOR-SCOPE-PRECHECK', 9500, 9300, 'PRECHECK_PASSED', 7, '{\"prior\":true}', ?, 'PASS', 'PASS', 'NOT_APPLICABLE', 'NOT_APPLICABLE', 'NOT_APPLICABLE', 'PASS', 5, 0, 0, 0)", PRIOR_PRECHECK_AT);
        when(signatures.recordPqcReleaseSignature(eq(7101L), eq(9500L), eq(9600L), any(), any())).thenReturn(9900L);
        // Match the real initializer's persisted readiness/time effects without expanding this parent fixture.
        // This seam does not prove candidate/readiness derivation or the initializer's own validation.
        when(managerStage.initializeManagerReleaseStage(any())).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            LocalDateTime lastPrecheckAt = LocalDateTime.now();
            if (promote) {
                assertEquals(1, jdbc.update("UPDATE mes_pro_edhr_release_transaction SET release_status='PENDING_APPROVAL', version=8, precheck_snapshot_json='{\"manager\":true}', last_precheck_at=?, dhr_status='PASS', inspection_status='PASS', deviation_status='PASS', rework_status='PASS', scrap_status='PASS', inventory_status='PASS', required_check_count=6, failed_check_count=0, blocking_check_count=0 WHERE id=9800", lastPrecheckAt));
            } else {
                assertEquals(1, jdbc.update("INSERT INTO mes_pro_edhr_release_transaction (id, batch_execution_id, work_order_id, release_status, version, precheck_snapshot_json, last_precheck_at, dhr_status, inspection_status, deviation_status, rework_status, scrap_status, inventory_status, required_check_count, failed_check_count, blocking_check_count, tenant_id) VALUES (9800, 9500, 9300, 'PENDING_APPROVAL', 1, '{\"manager\":true}', ?, 'PASS', 'PASS', 'PASS', 'PASS', 'PASS', 'PASS', 6, 0, 0, 0)", lastPrecheckAt));
            }
            tasks.insert(task(9801L, "RELEASE_APPROVE", "RELEASE_TRANSACTION", 9800L)
                    .setAssigneeUserId(7201L).setCandidateUserSnapshot("7201,7202")
                    .setResponsibilitySourceDigest("manager-candidates"));
            return new MesProductionReleaseManagerStageInitializationResult().setReleaseTransactionId(9800L)
                    .setManagerReleaseWorkTaskId(9801L).setManagerCandidateSnapshotHash("manager-candidates");
        });
    }

    private MesProEdhrReleaseTransactionDO readManagerTransaction(Long id) {
        return jdbc.queryForObject("SELECT * FROM mes_pro_edhr_release_transaction WHERE id=? AND tenant_id=0 AND deleted=FALSE",
                BeanPropertyRowMapper.newInstance(MesProEdhrReleaseTransactionDO.class), id);
    }

    private static void assertManagerReadiness(JSONObject actual, MesProEdhrReleaseTransactionDO persisted) {
        var expected = JSON.parseObject(JsonUtils.toJsonString(persisted));
        assertAll("persisted manager fields", MANAGER_AUDIT_FIELDS.stream().map(field -> (Executable) () -> {
            assertNotNull(expected.get(field), "test oracle must read a real persisted " + field);
            assertTrue(actual.containsKey(field), "missing actual manager audit field " + field);
            assertEquals(expected.get(field), actual.get(field), "persisted manager " + field);
        }));
    }

    private void insertBatch() {
        jdbc.update("INSERT INTO mes_pro_edhr_batch_execution (id, batch_execution_code, work_order_id, batch_code, route_id, route_version_id, tenant_id) VALUES (9500, 'SCOPE-BATCH', 9300, 'SCOPE-BATCH-CODE', 9100, 9101, 0)");
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO application() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO().setActiveOrderId(9200L).setWorkOrderId(9300L)
                .setWorkOrderCode("RELEASE-SCOPE-WO").setBatchCode("SCOPE-BATCH-CODE").setRouteId(9100L)
                .setRouteVersionId(9101L).setApplicationStatus("PQC_RELEASE_PENDING").setVersion(1)
                .setSourceSnapshotHash("formal-source").setAppliedBy(20L).setAppliedAt(LocalDateTime.now())
                .setRequestIdempotencyKey("release-scope-apply").setBusinessIdempotencyKey("release-scope-business");
    }

    private MesProEdhrWorkTaskDO task(Long id, String type, String scope, Long scopeId) {
        return new MesProEdhrWorkTaskDO().setId(id).setTaskCode("SCOPE-TASK-" + id).setTaskType(type)
                .setBusinessScopeType(scope).setBusinessScopeId(scopeId).setBatchExecutionId(9500L)
                .setWorkOrderId(9300L).setAssigneeUserId(20L).setCandidateSourceType("ROLE")
                .setCandidateSourceId(44L).setCandidateUserSnapshot("20,21").setSourceUserId(20L)
                .setResponsibilitySourceType("ROLE").setResponsibilitySourceKey("FROZEN-ROLE")
                .setResponsibilitySourceVersion("scope-v1").setResponsibilitySourceDigest("scope-digest")
                .setOwnershipLocked(true).setStatus("TODO").setActionUrl("/mes/production-release/pqc");
    }

    private MesPqcProductionReleaseApproveCommand approveCommand() {
        return new MesPqcProductionReleaseApproveCommand().setApplicationId(9600L).setPqcReleaseWorkTaskId(9700L)
                .setExpectedVersion(1).setIdempotencyKey("scope-pqc-approve").setSignaturePassword("fixture-only")
                .setApprovalOpinion("正式放行").setUdiControlDocumentNo("UDI-SCOPE");
    }

    private GxpAuditCommand onlyAudit(String operation) {
        var captor = ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(audit).append(captor.capture());
        assertEquals(operation, captor.getValue().getOperationId());
        return captor.getValue();
    }

    private static JSONObject affected(GxpAuditCommand command, boolean before) {
        var envelope = before ? command.getBeforeState() : command.getAfterState();
        var state = JSON.parseObject(envelope.getCanonicalJson()).getJSONObject("affectedState");
        assertNotNull(state, (before ? "before" : "after") + " audit must contain persisted affectedState, not only receipt IDs");
        return state;
    }

    private static JSONObject row(JSONObject state, String collection, Long id) {
        var rows = state.getJSONArray(collection);
        assertNotNull(rows, "missing affected rows: " + collection);
        return rows.toJavaList(JSONObject.class).stream().filter(row -> id.equals(row.getLong("id")))
                .findFirst().orElseThrow(() -> new AssertionError("missing " + collection + " row " + id));
    }
}
