package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;

/** SQL expressions for the leader submission list, applied before the count and LIMIT. */
public final class MesSubmissionListFilters {
    private MesSubmissionListFilters() {}
    private static final String GROUP_SCOPE = "g.tenant_id=pool_event.tenant_id AND g.deleted=0 AND g.event_type='PQC_INSPECTION' AND JSON_VALID(g.raw_payload) AND JSON_UNQUOTE(JSON_EXTRACT(g.raw_payload,'$.pqcSubmissionGroupId'))=JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload,'$.pqcSubmissionGroupId'))";
    private static final String GROUPED = "pool_event.event_type='PQC_INSPECTION' AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload,'$.pqcSubmissionGroupId')),'') IS NOT NULL AND (SELECT COUNT(*) FROM mes_pro_process_pool_event g WHERE " + GROUP_SCOPE + ")>1";
    private static final String FIRST_PAYLOAD = "(SELECT g.raw_payload FROM mes_pro_process_pool_event g WHERE " + GROUP_SCOPE + " ORDER BY g.id LIMIT 1)";
    // Mirrors PqcSubmissionRowAccumulator: first payload, distinct quantities joined in event-id order.
    private static final String PAYLOAD = "CASE WHEN " + GROUPED + " THEN JSON_SET(" + FIRST_PAYLOAD + ",'$.actualInspectionQuantity'," + groupedQuantity("actualInspectionQuantity") + ",'$.scrapQuantity'," + groupedQuantity("scrapQuantity") + ") ELSE pool_event.raw_payload END";
    private static final String ROOT = "list_filter_payload.root_payload";

    public static String payloadExpression() { return PAYLOAD; }

    private static String scalar(String json, String path) {
        String value = "JSON_EXTRACT(" + json + ",'" + path + "')";
        return "CASE WHEN JSON_TYPE(" + value + ")='NULL' THEN NULL ELSE NULLIF(TRIM(JSON_UNQUOTE(" + value + ")),'') END";
    }

    private static String joinedJson(String query, String separator) {
        return "JSON_UNQUOTE(CONCAT(CHAR(34),REPLACE(SUBSTRING(CAST((" + query + ") AS CHAR),3,CHAR_LENGTH(CAST((" + query + ") AS CHAR))-4),CONCAT(CHAR(34),', ',CHAR(34)),'" + separator + "'),CHAR(34)))";
    }

    private static String groupedQuantity(String key) {
        String value = scalar("g.raw_payload", "$." + key);
        String earlier = scalar("h.raw_payload", "$." + key);
        return joinedJson("SELECT JSON_ARRAYAGG(" + value + ") OVER(ORDER BY g.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_pro_process_pool_event g WHERE " + GROUP_SCOPE + " AND (" + value + ") IS NOT NULL AND NOT EXISTS(SELECT 1 FROM mes_pro_process_pool_event h WHERE h.tenant_id=g.tenant_id AND h.deleted=0 AND h.event_type='PQC_INSPECTION' AND h.id<g.id AND JSON_UNQUOTE(JSON_EXTRACT(h.raw_payload,'$.pqcSubmissionGroupId'))=JSON_UNQUOTE(JSON_EXTRACT(g.raw_payload,'$.pqcSubmissionGroupId')) AND BINARY (" + earlier + ")=BINARY (" + value + ")) LIMIT 1", "、");
    }

    private static String read(String... keys) {
        java.util.List<String> values = new java.util.ArrayList<>();
        for (String key : keys) {
            values.add(scalar(ROOT, "$." + key));
            values.add(scalar(ROOT, "$.fieldValues." + key));
            values.add(scalar(ROOT, "$.pqcDraft." + key));
        }
        return "COALESCE(" + String.join(",", values) + ")";
    }

    private static String quantityText(boolean loss) {
        String value = "CASE WHEN pool_event.event_type='PQC_INSPECTION' THEN "
                + (loss ? read("scrapQuantity", "lossQuantity", "SCRAP_QUANTITY") : read("inspectionQuantity", "actualInspectionQuantity"))
                + " ELSE " + (loss ? read("lossQuantity", "SCRAP_QUANTITY") : read("outputQuantity", "OUTPUT_QUANTITY")) + " END";
        return "COALESCE(CONCAT((" + value + "),' 件'),'--')";
    }

    private static String inspectionTask() {
        String task = " FROM mes_pqc_inspection_task t WHERE t.id=pool_event.pqc_task_id AND t.tenant_id=pool_event.tenant_id AND t.deleted=0 AND pool_event.feedback_source_type='MES_PQC_INSPECTION_TASK'";
        String type = "COALESCE((SELECT CASE t.inspection_type WHEN 'FIRST' THEN '首检' WHEN 'PATROL' THEN '巡检' WHEN 'FINAL' THEN '末检' ELSE TRIM(t.inspection_type) END" + task + "),'')";
        String round = "COALESCE((SELECT CAST(NULLIF(t.round_no,0) AS CHAR)" + task + "),'--')";
        String ids = "CASE WHEN pool_event.event_type<>'PQC_INSPECTION' THEN '' WHEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload,'$.pqcSubmissionGroupId')),'') IS NULL THEN CAST(pool_event.id AS CHAR) ELSE " + joinedJson("SELECT JSON_ARRAYAGG(CAST(g.id AS CHAR)) OVER(ORDER BY g.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_pro_process_pool_event g WHERE " + GROUP_SCOPE + " LIMIT 1", "、") + " END";
        return "CONCAT(" + type + ",' '," + round + ",' '," + ids + ")";
    }

    private static String clearance(String key) {
        String path = "REPLACE(JSON_UNQUOTE(JSON_SEARCH(" + ROOT + ",'one','" + key + "',NULL,'$.clearanceConfirmations[*].key')),'.key','.confirmed')";
        String value = "JSON_EXTRACT(" + ROOT + "," + path + ")";
        return "CASE WHEN JSON_TYPE(" + value + ")='BOOLEAN' THEN CASE JSON_UNQUOTE(" + value + ") WHEN 'true' THEN '已选' ELSE '未选' END ELSE '--' END";
    }

    private static String reportAllocations() {
        String total = "COALESCE((SELECT SUM(q.allocated_quantity) FROM mes_pro_process_pool_report_allocation q WHERE q.tenant_id=a.tenant_id AND q.active_order_id=a.active_order_id AND q.route_process_id=a.route_process_id AND q.process_id=a.process_id AND q.deleted=0 AND q.lifecycle_status='CURRENT'),0)";
        String overage = "GREATEST(" + total + "-s.planned_quantity_snapshot,0)";
        String released = "EXISTS(SELECT 1 FROM mes_pro_process_pool_active_order_release_application r JOIN mes_pro_edhr_release_transaction t ON t.id=r.release_transaction_id AND t.tenant_id=r.tenant_id AND t.deleted=0 AND t.release_status='RELEASED' WHERE r.tenant_id=a.tenant_id AND r.active_order_id=a.active_order_id AND r.deleted=0)";
        // The original generic array filter flattens every ReportAllocationRespVO value in property order.
        String line = "CONCAT(COALESCE(CAST(a.id AS CHAR),''),' ',COALESCE(CAST(a.active_order_id AS CHAR),''),' ',COALESCE(CAST(a.work_order_id AS CHAR),''),' ',COALESCE(TRIM(w.code),''),' ',COALESCE(CAST(CAST(a.allocated_quantity AS DOUBLE) AS CHAR),''),' ',COALESCE(CAST(CAST(" + overage + " AS DOUBLE) AS CHAR),''),' ',CASE WHEN " + total + ">s.planned_quantity_snapshot THEN 'true' ELSE 'false' END,' ',CASE WHEN " + released + " THEN 'true false' ELSE 'false true' END)";
        String query = "SELECT JSON_ARRAYAGG(" + line + ") OVER(ORDER BY a.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_pro_process_pool_report_allocation a LEFT JOIN mes_pro_work_order w ON w.id=a.work_order_id AND w.tenant_id=a.tenant_id AND w.deleted=0 LEFT JOIN mes_pro_process_pool_active_order_process_snapshot s ON s.active_order_id=a.active_order_id AND s.route_process_id=a.route_process_id AND s.process_id=a.process_id AND s.tenant_id=a.tenant_id AND s.deleted=0 WHERE a.event_id=pool_event.id AND a.tenant_id=pool_event.tenant_id AND a.deleted=0 AND a.lifecycle_status='CURRENT' LIMIT 1";
        return "COALESCE(" + joinedJson(query, " ") + ",'')";
    }

    public static final Map<String, ListFilterQueryUtils.Field> FIELDS = Map.ofEntries(
            Map.entry("submittedAt", ListFilterQueryUtils.dateTime("pool_event.server_submit_time")),
            Map.entry("approvedAt", ListFilterQueryUtils.dateTime("latest_submission_review.reviewed_at")),
            Map.entry("process", ListFilterQueryUtils.text("COALESCE(NULLIF((SELECT x.name FROM mes_pro_process x WHERE x.id=pool_event.process_id AND x.tenant_id=pool_event.tenant_id AND x.deleted=0),''),NULLIF((SELECT x.code FROM mes_pro_process x WHERE x.id=pool_event.process_id AND x.tenant_id=pool_event.tenant_id AND x.deleted=0),''),'--')")),
            Map.entry("workOrder", ListFilterQueryUtils.text("COALESCE(NULLIF(work_order.code,''),NULLIF(work_order.name,''),'--')")),
            Map.entry("product", ListFilterQueryUtils.text("COALESCE(NULLIF((SELECT p.code FROM mes_md_item p WHERE p.id=work_order.product_id AND p.tenant_id=pool_event.tenant_id AND p.deleted=0),''),NULLIF((SELECT p.name FROM mes_md_item p WHERE p.id=work_order.product_id AND p.tenant_id=pool_event.tenant_id AND p.deleted=0),''),'--')")),
            Map.entry("reviewStatus", ListFilterQueryUtils.text("CASE latest_submission_review.review_status WHEN 'APPROVED' THEN '正确' WHEN 'REJECTED' THEN '不正确' ELSE '待判定' END")),
            Map.entry("reviewRemark", ListFilterQueryUtils.text("COALESCE(NULLIF(latest_submission_review.review_remark,''),'--')")),
            Map.entry("reportAllocations", ListFilterQueryUtils.text(reportAllocations())),
            Map.entry("inspectionTask", ListFilterQueryUtils.text(inspectionTask())),
            Map.entry("approvedBy", ListFilterQueryUtils.text("COALESCE(NULLIF((SELECT u.nickname FROM system_users u WHERE u.id=latest_submission_review.leader_user_id AND u.tenant_id=pool_event.tenant_id AND u.deleted=0),''),CAST(latest_submission_review.leader_user_id AS CHAR),'--')")),
            Map.entry("employeeUser", ListFilterQueryUtils.text("CASE (CASE WHEN pool_event.event_type='PQC_INSPECTION' OR (pool_event.event_type='PRODUCTION_SUBMIT' AND pool_event.template_type='SIMULATED_PRODUCTION_SUBMIT' AND JSON_VALID(pool_event.raw_payload) AND JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload,'$.simulated'))='true') THEN 'SYSTEM_USER' WHEN pool_event.event_type='PRODUCTION_SUBMIT' AND JSON_VALID(pool_event.raw_payload) THEN JSON_UNQUOTE(JSON_EXTRACT(pool_event.raw_payload,'$.signatureIdentityDomain')) END) WHEN 'SYSTEM_USER' THEN (SELECT COALESCE(NULLIF(u.nickname,''),NULLIF(u.username,'')) FROM system_users u WHERE u.id=pool_event.actual_employee_id AND u.tenant_id=pool_event.tenant_id AND u.deleted=0) WHEN 'MES_EMPLOYEE_PROFILE' THEN (SELECT COALESCE(NULLIF(u.display_name,''),NULLIF(u.employee_name,'')) FROM mes_pro_process_pool_team_employee_profile u WHERE u.id=pool_event.actual_employee_id AND u.tenant_id=pool_event.tenant_id AND u.deleted=0) END")),
            Map.entry("completionQuantity", ListFilterQueryUtils.text(quantityText(false))),
            Map.entry("lossQuantity", ListFilterQueryUtils.text(quantityText(true))),
            Map.entry("reportUnallocatedQuantity", ListFilterQueryUtils.text("COALESCE(CAST(pool_event.report_unallocated_quantity AS CHAR)," + scalar("pool_event.raw_payload", "$.outputQuantity") + ",'--')")),
            Map.entry("clearanceWorkplace", ListFilterQueryUtils.text(clearance("workplace"))),
            Map.entry("clearanceMaterial", ListFilterQueryUtils.text(clearance("material"))),
            Map.entry("clearanceCleaning", ListFilterQueryUtils.text(clearance("cleaning")))
    );
}
