package cn.iocoder.yudao.module.mes.service.pro.productionrelease;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SUBMITTED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BATCH-01 only: real application transaction, persistence and collector over private H2 rows.
 * The provisioning seam persists the inspected same-transaction child effects; it does not
 * pretend to execute the full batch service, its validation, or the AFTER_COMMIT Tx-C listener.
 */
@Import({MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.class,
        MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.class, MesReleaseAffectedStateCollector.class})
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:release_provisioning_affected_state;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value,day")
class MesReleaseProvisioningAffectedStateTest extends BaseDbUnitTest {
    private static final String KEY = "release-provisioning-scope";
    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 9, 29, 9, 10);
    private static final LocalDateTime APPROVED = LocalDateTime.of(2026, 9, 29, 9, 20);
    @Resource private MesTeamLeaderActiveOrderReleaseApplicationService applicationService;
    @Resource private MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistence;
    @Resource private DataSource dataSource;
    @MockitoBean private MesTeamLeaderActiveOrderReleaseGenerationService generation;
    @MockitoBean private MesTeamLeaderActiveOrderCompletionService completion;
    @MockitoBean private MesTeamLeaderActiveOrderCompletionBatchExecutionService completionBatch;
    @MockitoBean private MesProcessPoolActiveOrderCompletionReceiptMapper receipts;
    @MockitoBean private MesReleaseFlowAuditRecorder specializedAudit;
    @MockitoBean private GxpAuditService audit;
    private JdbcTemplate jdbc;

    @BeforeEach
    void fixtures() {
        TenantContextHolder.setTenantId(0L);
        jdbc = new JdbcTemplate(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("sql/release-provisioning-form-fixture.sql"))
                .execute(dataSource);
        when(completion.completeForRelease(20L, 9200L, KEY, true))
                .thenReturn(new MesTeamLeaderActiveOrderCompletionResult().setCompletionReceiptId(9400L));
        when(generation.generate(eq(20L), any())).thenAnswer(invocation -> persistence.persistPending(
                new MesProcessPoolActiveOrderReleaseApplicationDO().setActiveOrderId(9200L)
                        .setWorkOrderId(9300L).setWorkOrderCode("PROVISION-WO").setBatchCode("PROVISION-BATCH")
                        .setRouteId(9100L).setRouteVersionId(9101L).setApplicationStatus("PQC_RELEASE_PENDING")
                        .setVersion(1).setSourceSnapshotHash("formal-source").setAppliedBy(20L)
                        .setAppliedAt(SUBMITTED).setRequestIdempotencyKey(KEY).setBusinessIdempotencyKey(KEY),
                new MesProductionReleaseRoleCandidates(44L, "PQC", List.of(7101L), "candidate-hash")));
    }

    @AfterEach
    void clearOwnedExtraTables() {
        // BaseDbUnitTest cleans the MES tables; these extra fixtures live in this class's private DB.
        jdbc.update("DELETE FROM bpm_form_action_snapshot");
        jdbc.update("DELETE FROM bpm_form_action_instance");
        jdbc.update("DELETE FROM mes_pro_process_pool_active_order_release_application WHERE active_order_id=9200");
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @CsvSource({"dossierItems,9601", "provisioningRecords,9602", "formInstances,9603",
            "formSnapshots,9604", "sharedExecutions,9605", "operationAudits,9606"})
    void applyCapturesActualProvisioningRowsAndExcludesOtherScopes(String collection, long id) {
        insertEffects(9501, 10600, 0, false); // unrelated batch
        insertEffects(9500, 11600, 8, false); // another tenant, same batch identity
        insertEffects(9500, 12600, 0, true);  // logically deleted rows
        stubNewBatch();

        var result = apply();
        var command = onlyAudit();
        var before = affected(command, true);
        var after = affected(command, false);
        assertNotNull(before.getJSONArray(collection), "missing before collection: " + collection);
        assertTrue(before.getJSONArray(collection).isEmpty(), "new batch effects must be absent before creation");
        assertNotNull(after.getJSONArray(collection), "missing persisted collection: " + collection);
        assertEquals(List.of(id), after.getJSONArray(collection).toJavaList(JSONObject.class).stream()
                .map(row -> row.getLong("id")).toList(), "do not leak unrelated/foreign/deleted rows or duplicate shared instances");
        var actual = row(after, collection, id);
        switch (collection) {
            case "dossierItems" -> {
                assertEquals("FINAL_INSPECTION", actual.getString("itemType"));
                assertEquals("PENDING", actual.getString("itemStatus"));
                assertTrue(actual.getBooleanValue("requiredFlag"));
            }
            case "provisioningRecords" -> {
                assertEquals("ACTIVE_ORDER_COMPLETION_BATCH:9400", actual.getString("idempotencyKey"));
                assertEquals("BATCH_PROVISIONING", actual.getString("status"));
                assertEquals("formal-source", actual.getString("sourceSnapshotHash"));
                assertEquals("bundle-hash", actual.getString("sourceBundleHash"));
                assertEquals("receipt-hash", actual.getString("sourceCredentialHash"));
                assertEquals(1, actual.getIntValue("attemptCount"));
            }
            case "formInstances" -> {
                assertEquals("DRAFT", actual.getString("status"));
                assertEquals(20L, actual.getLong("applicantUserId"));
                assertEquals("{\"prefill\":\"formal\"}", actual.getString("formDataJson"));
                assertEquals("{\"batchExecutionId\":9500}", actual.getString("businessContextJson"));
            }
            case "formSnapshots" -> {
                assertEquals(9603L, actual.getLong("instanceId"));
                assertEquals(1, actual.getIntValue("snapshotVersion"));
                assertEquals("DRAFT", actual.getString("snapshotType"));
                assertEquals("{\"prefill\":\"formal\"}", actual.getString("formDataJson"));
            }
            case "sharedExecutions" -> {
                assertEquals("BATCH_SHARED", actual.getString("instanceScope"));
                assertEquals("shared-record", actual.getString("sharedFormKey"));
                assertEquals("{\"frozen\":true}", actual.getString("executionSnapshotJson"));
                assertEquals("[]", actual.getString("cellValuesJson"));
                assertEquals(1, actual.getIntValue("revisionNo"));
            }
            case "operationAudits" -> {
                assertEquals("OPEN", actual.getString("operationType"));
                assertEquals("SUCCESS", actual.getString("resultStatus"));
                assertEquals("{\"provisioningReceiptId\":9602}", actual.getString("metadataJson"));
                assertEquals("fixture-audit-hash", actual.getString("auditHash"));
            }
            default -> fail("unknown assertion case");
        }
        when(generation.replayExisting(eq(20L), any())).thenReturn(result);
        apply();
        verify(audit).append(any());
        verify(completionBatch).openOrCreate(20L, 9200L, 9400L, KEY);
    }

    @Test
    void reusedBatchSyncCapturesActualTaskTimesBeforeAndAfter() {
        insertBatch();
        insertEffects(9500, 9600, 0, false);
        when(completionBatch.openOrCreate(20L, 9200L, 9400L, KEY)).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            // Actual syncBatchStatus columns: status and execution submitted/approved timestamps.
            jdbc.update("UPDATE mes_pro_edhr_batch_execution_task SET status=?, submitted_at=? WHERE id=9608", TASK_STATUS_SUBMITTED, SUBMITTED);
            jdbc.update("UPDATE mes_pro_edhr_batch_execution_task SET status=?, approved_at=? WHERE id=9609", TASK_STATUS_APPROVED, APPROVED);
            jdbc.update("UPDATE mes_pro_edhr_batch_execution SET task_approved_count=1, status=? WHERE id=9500", BATCH_STATUS_IN_PROGRESS);
            return 9500L;
        });
        apply();
        var command = onlyAudit();
        var before = row(affected(command, true), "batchTasks", 9608);
        var after = row(affected(command, false), "batchTasks", 9608);
        assertNull(before.get("submittedAt"));
        assertNull(before.get("approvedAt"));
        assertEquals(0, before.getIntValue("status"));
        assertEquals(TASK_STATUS_SUBMITTED, after.getIntValue("status"));
        assertEquals(JSON.parse(JsonUtils.toJsonString(SUBMITTED)), after.get("submittedAt"));
        var approvedBefore = row(affected(command, true), "batchTasks", 9609);
        var approvedAfter = row(affected(command, false), "batchTasks", 9609);
        assertNull(approvedBefore.get("approvedAt"));
        assertEquals(TASK_STATUS_APPROVED, approvedAfter.getIntValue("status"));
        assertEquals(JSON.parse(JsonUtils.toJsonString(APPROVED)), approvedAfter.get("approvedAt"));
    }

    @Test
    void auditFailureRollsBackEveryProvisioningEffectAndApplication() {
        stubNewBatch();
        doAnswer(invocation -> {
            assertEquals(1, count("mes_pro_edhr_batch_provisioning_record"));
            assertEquals(1, count("bpm_form_action_snapshot"));
            assertEquals(1, count("mes_pro_edhr_work_task"));
            throw new IllegalStateException("provisioning audit failed");
        }).when(audit).append(any());
        assertEquals("provisioning audit failed", assertThrows(IllegalStateException.class, this::apply).getMessage());
        for (String table : List.of("mes_pro_edhr_batch_execution", "mes_pro_edhr_batch_execution_task",
                "mes_pro_edhr_batch_dossier_item", "mes_pro_edhr_batch_provisioning_record", "bpm_form_action_instance",
                "bpm_form_action_snapshot", "mes_pro_batch_record_execution", "mes_pro_edhr_operation_audit_event",
                "mes_pro_process_pool_active_order_release_application", "mes_pro_edhr_work_task")) {
            assertEquals(0, count(table), "audit failure must roll back " + table);
        }
    }

    private void stubNewBatch() {
        when(completionBatch.openOrCreate(20L, 9200L, 9400L, KEY)).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            insertBatch();
            insertEffects(9500, 9600, 0, false);
            return 9500L;
        });
    }

    private void insertBatch() {
        jdbc.update("INSERT INTO mes_pro_edhr_batch_execution (id,batch_execution_code,work_order_id,work_order_code,batch_code,route_id,route_version_id,status,tenant_id) VALUES (9500,'PROVISION-BATCH',9300,'PROVISION-WO','PROVISION-BATCH',9100,9101,0,0)");
    }

    private void insertEffects(long batchId, long base, long tenant, boolean deleted) {
        jdbc.update("INSERT INTO mes_pro_edhr_batch_dossier_item (id,batch_execution_id,item_type,item_key,item_name,required_flag,item_status,tenant_id,deleted) VALUES (?,?,'FINAL_INSPECTION','FINAL_INSPECTION','Final inspection',TRUE,'PENDING',?,?)", base + 1, batchId, tenant, deleted);
        jdbc.update("INSERT INTO mes_pro_edhr_batch_provisioning_record (id,batch_execution_id,entry_type,entry_business_id,source_credential_id,source_credential_hash,source_snapshot_hash,source_bundle_hash,source_version,idempotency_key,status,attempt_count,tenant_id,deleted) VALUES (?,?,'ACTIVE_ORDER_COMPLETION','9200','9400','receipt-hash','formal-source','bundle-hash','1',?,'BATCH_PROVISIONING',1,?,?)",
                base + 2, batchId, base == 9600 ? "ACTIVE_ORDER_COMPLETION_BATCH:9400" : "noise-" + base, tenant, deleted);
        String context = "{\"batchExecutionId\":" + batchId + "}";
        jdbc.update("INSERT INTO bpm_form_action_instance (id,instance_code,tenant_id,policy_id,applicant_user_id,status,data_domain,system_code,object_type,object_id,object_version,action_code,object_state,idempotency_key,business_context_json,form_data_json,deleted) VALUES (?,?,?,51,20,'DRAFT','MES','MES','BATCH_TASK',?,'9101','ROUTE_FORM','OPEN',?,?,?,?)",
                base + 3, "FORM-" + base, tenant, String.valueOf(base + 7), "form-" + base, context, "{\"prefill\":\"formal\"}", deleted);
        jdbc.update("INSERT INTO bpm_form_action_snapshot (id,instance_id,tenant_id,snapshot_type,snapshot_version,form_data_json,business_context_json,deleted) VALUES (?,?,?,'DRAFT',1,?,?,?)",
                base + 4, base + 3, tenant, "{\"prefill\":\"formal\"}", context, deleted);
        jdbc.update("INSERT INTO mes_pro_batch_record_execution (id,execution_code,work_order_id,work_order_code,batch_execution_id,instance_scope,shared_form_key,batch_code,status,sheet_layout_json,execution_snapshot_json,cell_values_json,revision_no,tenant_id,deleted) VALUES (?,?,9300,'PROVISION-WO',?,'BATCH_SHARED','shared-record','PROVISION-BATCH',0,'{}','{\"frozen\":true}','[]',1,?,?)",
                base + 5, "EXEC-" + base, batchId, tenant, deleted);
        jdbc.update("INSERT INTO mes_pro_edhr_operation_audit_event (id,request_id,object_type,object_id,batch_execution_id,operation_type,result_status,metadata_json,occurred_at,audit_hash,tenant_id,deleted) VALUES (?,?,'BATCH_EXECUTION',?,?,'OPEN','SUCCESS',?,?,'fixture-audit-hash',?,?)",
                base + 6, "REQUEST-" + base, String.valueOf(batchId), batchId,
                "{\"provisioningReceiptId\":" + (base + 2) + "}", SUBMITTED, tenant, deleted);
        jdbc.update("INSERT INTO mes_pro_edhr_batch_execution_task (id,batch_execution_id,route_process_sort,form_binding_key,form_center_instance_id,tenant_id,deleted) VALUES (?,?,1,'dynamic',?,?,?)",
                base + 7, batchId, base + 3, tenant, deleted);
        // Two current batch task bindings to the same shared execution must produce one frozen row.
        for (long taskId : List.of(base + 8, base + 9)) {
            jdbc.update("INSERT INTO mes_pro_edhr_batch_execution_task (id,batch_execution_id,route_process_sort,execution_id,instance_scope,shared_form_key,tenant_id,deleted) VALUES (?,?,2,?,'BATCH_SHARED','shared-record',?,?)",
                    taskId, batchId, base + 5, tenant, deleted);
        }
    }

    private MesTeamLeaderActiveOrderReleaseApplicationResult apply() {
        return applicationService.apply(20L, new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(9200L).setConfirmNoReplenishmentInfo(true).setIdempotencyKey(KEY));
    }

    private GxpAuditCommand onlyAudit() {
        var captor = ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(audit).append(captor.capture());
        assertEquals("mes.pqc-release.apply", captor.getValue().getOperationId());
        return captor.getValue();
    }

    private static JSONObject affected(GxpAuditCommand command, boolean before) {
        return JSON.parseObject((before ? command.getBeforeState() : command.getAfterState()).getCanonicalJson())
                .getJSONObject("affectedState");
    }

    private static JSONObject row(JSONObject state, String collection, long id) {
        assertNotNull(state.getJSONArray(collection), "missing persisted collection: " + collection);
        return state.getJSONArray(collection).toJavaList(JSONObject.class).stream()
                .filter(value -> value.getLongValue("id") == id).findFirst().orElseThrow();
    }

    private int count(String fixtureTable) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + fixtureTable, Integer.class);
    }
}
