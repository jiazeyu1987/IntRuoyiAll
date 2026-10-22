package cn.iocoder.yudao.module.mes.service.pro.productionrelease;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Clob;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Current, tenant-scoped reads for existing MES audit envelopes, in their business transaction. */
@Component
public class MesReleaseAffectedStateCollector {
    private final JdbcTemplate jdbc;

    public MesReleaseAffectedStateCollector(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public Long completionBatchId(Long completionReceiptId) {
        var records = rows("mes_pro_edhr_batch_provisioning_record", "id,batch_execution_id",
                "idempotency_key = ?", "ACTIVE_ORDER_COMPLETION_BATCH:" + completionReceiptId);
        if (records.size() > 1) throw new IllegalStateException("Completion batch identity is not unique");
        return records.isEmpty() ? null : ((Number) records.get(0).get("batchExecutionId")).longValue();
    }

    public Map<String, Object> capture(Long batchId, Long applicationId, Long transactionId,
                                       Long workOrderId, boolean includeBindings) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("batches", rows("mes_pro_edhr_batch_execution",
                "id,batch_execution_code,work_order_id,batch_code,active_context_key,attempt_no,route_id,route_version_id,"
                        + "route_snapshot_json,status,provisioning_status,task_total,task_approved_count,blocked_count,"
                        + "aggregate_hash,close_signature_id,closed_by,closed_at", "id = ?", batchId));
        state.put("releaseApplications", rows("mes_pro_process_pool_active_order_release_application",
                "id,active_order_id,work_order_id,route_id,route_version_id,batch_execution_id,release_transaction_id,"
                        + "release_approval_work_task_id,pqc_release_work_task_id,pqc_decision,pqc_decided_by,pqc_decided_at,"
                        + "pqc_reject_reason,application_status,source_snapshot_hash,report_snapshot_hash,version,"
                        + "request_idempotency_key,business_idempotency_key,blocker_snapshot_json,dossier_summary_json,applied_by,applied_at",
                "id = ?", applicationId));
        String transactionColumns = "id,batch_execution_id,work_order_id,release_status,release_decision_id,version,"
                + "last_precheck_at,dhr_status,inspection_status,deviation_status,rework_status,scrap_status,inventory_status,"
                + "required_check_count,failed_check_count,blocking_check_count,"
                + "precheck_snapshot_json,finalization_payload_hash,submit_idempotency_key,submitted_by,submitted_at,"
                + "approval_idempotency_key,approved_by,approved_at,approval_signature_id,approval_signoff_evidence_hash,"
                + "approval_opinion,rejected_by,rejected_at,reject_reason,withdrawn_by,withdrawn_at,withdraw_reason";
        var transactions = transactionId == null
                ? rows("mes_pro_edhr_release_transaction", transactionColumns,
                        "batch_execution_id = ? AND release_status NOT IN ('REJECTED', 'WITHDRAWN')", batchId)
                : rows("mes_pro_edhr_release_transaction", transactionColumns, "id = ?", transactionId);
        // The manager initializer promotes the latest transaction, not earlier attempts for this batch.
        if (transactionId == null && !transactions.isEmpty()) {
            transactions = List.of(transactions.get(transactions.size() - 1));
        }
        state.put("releaseTransactions", transactions);
        Long taskTransactionId = transactionId;
        if (taskTransactionId == null && !transactions.isEmpty()) {
            taskTransactionId = ((Number) transactions.get(0).get("id")).longValue();
        }
        state.put("workTasks", rows("mes_pro_edhr_work_task",
                "id,task_code,task_type,batch_execution_id,batch_task_id,business_scope_type,business_scope_id,execution_id,"
                        + "work_order_id,assignee_user_id,candidate_source_type,candidate_source_id,candidate_user_snapshot,"
                        + "source_user_id,responsibility_source_type,responsibility_source_key,responsibility_source_version,"
                        + "responsibility_source_digest,responsibility_scope_json,ownership_locked,ownership_last_transferred_at,"
                        + "ownership_last_transferred_by,status,started_at,completed_at,due_time,reason,remark,action_url",
                "(batch_execution_id = ? OR (business_scope_type = 'RELEASE_APPLICATION' AND business_scope_id = ?)"
                        + " OR (business_scope_type = 'RELEASE_TRANSACTION' AND business_scope_id = ?))",
                batchId, applicationId, taskTransactionId));
        state.put("workOrders", rows("mes_pro_work_order",
                "id,code,status,finish_date,release_decision_id,released_by,released_at", "id = ?", workOrderId));
        state.put("productionTasks", rows("mes_pro_task",
                "id,work_order_id,status,finish_date,cancel_date", "work_order_id = ?", workOrderId));
        if (includeBindings) {
            var batchTasks = rows("mes_pro_edhr_batch_execution_task",
                    "id,batch_execution_id,route_process_id,process_id,route_binding_id,route_binding_snapshot_hash,"
                            + "batch_record_definition_id,batch_record_version_id,form_binding_key,form_slot_type,"
                            + "form_template_id,form_template_version_id,form_center_instance_id,execution_id,"
                            + "instance_scope,shared_form_key,fillable_scope_json,record_category,validation_profile,"
                            + "permission_scope_id,required_policy,required_condition_json,owner_role_key,archive_visibility,"
                            + "slot_config_snapshot_hash,status,required_flag,blocker_code,blocker_message,submitted_at,approved_at",
                    "batch_execution_id = ?", batchId);
            state.put("batchTasks", batchTasks);
            state.put("batchOrigins", rows("mes_pro_edhr_batch_execution_origin",
                    "id,batch_execution_id,entry_type,origin_key,active_order_id,work_order_id,completion_transaction_id,"
                            + "completion_version,completion_backfill_receipt_id,completion_backfill_receipt_hash,"
                            + "pick_list_binding_id,pick_list_id,pick_list_binding_version,source_snapshot_hash,"
                            + "batch_provision_receipt_id,batch_provision_status,source_bundle_hash,idempotency_key,relation_status",
                    "batch_execution_id = ?", batchId));
            state.put("dossierItems", rows("mes_pro_edhr_batch_dossier_item",
                    "id,batch_execution_id,item_type,item_key,item_name,required_flag,item_status,source_doc_type,"
                            + "source_doc_id,source_doc_code,source_doc_status,source_doc_result,source_doc_hash,"
                            + "completed_at,verified_at,blocker_code,blocker_message", "batch_execution_id = ?", batchId));
            state.put("provisioningRecords", rows("mes_pro_edhr_batch_provisioning_record",
                    "id,batch_execution_id,entry_type,entry_business_id,source_credential_id,source_credential_hash,"
                            + "source_snapshot_hash,source_bundle_hash,source_version,idempotency_key,status,error_code,"
                            + "attempt_count,mapping_event_id,mapping_idempotency_key", "batch_execution_id = ?", batchId));
            // Follow the locked task bindings, never infer form instances from another business chain.
            var forms = referencedRows("bpm_form_action_instance",
                    "id,instance_code,policy_id,applicant_user_id,status,data_domain,system_code,object_type,object_id,"
                            + "object_version,action_code,object_state,idempotency_key,business_context_json,form_data_json,"
                            + "bpm_process_instance_id", "id", batchTasks, "formCenterInstanceId");
            state.put("formInstances", forms);
            state.put("formSnapshots", referencedRows("bpm_form_action_snapshot",
                    "id,instance_id,snapshot_type,snapshot_version,form_data_json,business_context_json,attachment_ids_json",
                    "instance_id", forms, "id"));
            state.put("sharedExecutions", rows("mes_pro_batch_record_execution",
                    "id,execution_code,template_id,template_code,template_name,work_order_id,work_order_code,"
                            + "route_process_id,task_id,workstation_id,batch_record_report_id,batch_record_definition_id,"
                            + "batch_record_version_id,batch_execution_id,route_id,instance_scope,shared_form_key,form_slot_type,"
                            + "record_category,validation_profile,recordbook_enabled,permission_scope_id,route_binding_id,"
                            + "route_binding_snapshot_hash,archive_visibility,slot_config_snapshot_hash,batch_code,status,"
                            + "sheet_layout_json,meta_json,execution_snapshot_json,cell_values_json,cell_values_hash,"
                            + "field_audit_revision,field_audit_head_hash,field_audit_last_batch_id,remark,active_context_key,"
                            + "revision_root_execution_id,revision_no,revision_parent_hash,active_revision_flag,"
                            + "submitted_by,submitted_at,approved_by,approved_at",
                    "batch_execution_id = ? AND instance_scope = 'BATCH_SHARED'", batchId));
            state.put("operationAudits", rows("mes_pro_edhr_operation_audit_event",
                    "id,request_id,object_type,object_id,batch_execution_id,execution_id,work_task_id,route_id,"
                            + "route_process_id,report_id,record_category,operation_type,action_name,actor_user_id,actor_username,"
                            + "permission_code,permission_decision,matched_rule_ids,result_status,failure_code,failure_message,"
                            + "before_summary_hash,after_summary_hash,metadata_json,occurred_at,previous_audit_hash,audit_hash",
                    "batch_execution_id = ?", batchId));
            // AFTER_COMMIT Tx-C changes are not part of this parent transaction's snapshot.
        }
        return state;
    }

    public GxpAuditStateEnvelope withAffected(GxpAuditStateEnvelope envelope, Map<String, Object> affected) {
        Map<String, Object> state = new LinkedHashMap<>(JsonUtils.parseObject(envelope.getCanonicalJson(), Map.class));
        state.put("affectedState", affected);
        return GxpAuditStateEnvelope.builder().state(envelope.getState()).objectVersion(envelope.getObjectVersion())
                .canonicalJson(JsonUtils.toJsonString(state)).build();
    }

    public Map<String, Object> captureCompletion(Long activeOrderId, Long workOrderId) {
        var tasks = queryRows("mes_pqc_inspection_task", "*", "active_order_id = ?", false, activeOrderId);
        var eventIds = tasks.stream().map(task -> task.get("submitted_event_id")).distinct().toList();
        var state = capturePqcRows(tasks, eventIds);
        state.put("mes_pro_process_pool_order_process_completion", queryRows(
                "mes_pro_process_pool_order_process_completion", "*", "work_order_id = ?", false, workOrderId));
        return state;
    }

    public Map<String, Object> capturePqcSubmission(Long eventId, Long taskId) {
        return capturePqcRows(queryRows("mes_pqc_inspection_task", "*", "id = ?", false, taskId), List.of(eventId));
    }

    private Map<String, Object> capturePqcRows(List<Map<String, Object>> tasks, List<?> eventIds) {
        Map<String, Object> state = new LinkedHashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();
        List<Map<String, Object>> aggregates = new ArrayList<>();
        for (Object eventId : eventIds) {
            records.addAll(queryRows("mes_pro_process_pool_pqc_record", "*", "event_id = ?", false, eventId));
            aggregates.addAll(queryRows("mes_pqc_process_inspection_aggregate_detail", "*", "event_id = ?", false, eventId));
        }
        state.put("mes_pqc_inspection_task", tasks);
        state.put("mes_pro_process_pool_pqc_record", records);
        state.put("mes_pqc_process_inspection_aggregate_detail", aggregates);
        return state;
    }

    private List<Map<String, Object>> rows(String table, String columns, String predicate, Object... values) {
        return queryRows(table, columns, predicate, true, values);
    }

    private List<Map<String, Object>> referencedRows(String table, String columns, String foreignKey,
                                                    List<Map<String, Object>> sources, String referenceKey) {
        var ids = sources.stream().map(row -> row.get(referenceKey)).filter(java.util.Objects::nonNull)
                .distinct().toList();
        if (ids.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return rows(table, columns, foreignKey + " IN (" + placeholders + ")", ids.toArray());
    }

    private List<Map<String, Object>> queryRows(String table, String columns, String predicate,
                                           boolean camelCaseKeys, Object... values) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Release audit snapshots require the business transaction");
        }
        List<Object> arguments = new ArrayList<>();
        arguments.add(TenantContextHolder.getRequiredTenantId());
        java.util.Collections.addAll(arguments, values);
        // Current reads avoid both MyBatis session caching and REPEATABLE READ's earlier read view.
        return jdbc.query("SELECT " + columns + " FROM " + table
                        + " WHERE tenant_id = ? AND deleted = FALSE AND " + predicate + " ORDER BY id FOR UPDATE",
                (result, rowNumber) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
                        String name = result.getMetaData().getColumnLabel(column).toLowerCase(Locale.ROOT);
                        StringBuilder key = new StringBuilder();
                        boolean upper = false;
                        for (char character : name.toCharArray()) {
                            if (character == '_') upper = true;
                            else { key.append(upper ? Character.toUpperCase(character) : character); upper = false; }
                        }
                        Object value = result.getObject(column);
                        if (value instanceof Clob clob) value = clob.getSubString(1, Math.toIntExact(clob.length()));
                        if (value instanceof Timestamp timestamp) value = timestamp.toLocalDateTime();
                        row.put(camelCaseKeys ? key.toString() : name, value);
                    }
                    return row;
                }, arguments.toArray());
    }
}
