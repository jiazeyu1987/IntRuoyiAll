package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTraceSearchScope;

import java.util.ArrayList;
import java.util.List;
import java.time.ZoneId;

/**
 * Search the authoritative rows in the database; there is no asynchronously maintained projection.
 * These predicates select matches, not integrity failures. The selected page must still pass the
 * formal evidence reader. A malformed, nonmatching document therefore cannot fail another batch's search.
 */
public final class MesProEdhrTraceSearchSql {
    private static final String B = "mes_pro_edhr_batch_execution";
    private static final String P = "mes_pro_process_pool_";
    private MesProEdhrTraceSearchSql() { }

    public static void apply(LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query,
                             EdhrBatchExecutionPageReqVO request, MesProEdhrTraceSearchScope scope) {
        java.util.Objects.requireNonNull(scope, "反查身份数据范围缺失");
        if (!request.isTraceBusinessTimeValid()) throw new IllegalArgumentException("业务时间范围无效");
        if (StrUtil.isNotBlank(request.getTracePerson())) {
            List<Object> args = new ArrayList<>();
            args.add(request.getTracePerson().trim());
            List<String> ids = new ArrayList<>();
            for (Long id : scope.visibleUserIds().stream().sorted().toList()) {
                ids.add("{" + args.size() + "}"); args.add(id);
            }
            query.apply(person(false).replace("__TRACE_SYSTEM_USER_IDS__", ids.isEmpty() ? "NULL" : String.join(",", ids)), args.toArray());
        }
        if (StrUtil.isNotBlank(request.getTraceEquipment())) {
            query.apply(equipment(), request.getTraceEquipment().trim());
        }
        if (StrUtil.isNotBlank(request.getTraceMaterial()) || StrUtil.isNotBlank(request.getTraceMaterialLot())) {
            List<Object> args = new ArrayList<>();
            String material = null;
            String lot = null;
            if (StrUtil.isNotBlank(request.getTraceMaterial())) {
                material = "{" + args.size() + "}";
                args.add(request.getTraceMaterial().trim());
            }
            if (StrUtil.isNotBlank(request.getTraceMaterialLot())) {
                lot = "{" + args.size() + "}";
                args.add(request.getTraceMaterialLot().trim());
            }
            query.apply(material(material, lot), args.toArray());
        }
        if (request.getTraceBusinessTime() != null) {
            var from = request.getTraceBusinessTime()[0];
            var to = request.getTraceBusinessTime()[1];
            query.apply(person(true), from, to, from.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    to.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
    }

    private static String person(boolean time) {
        List<String> sources = new ArrayList<>();
        sources.add(live("JOIN " + P + "report_allocation a ON " + liveOwner("a")
                + " AND a.lifecycle_status='CURRENT' JOIN " + P + "event e ON " + owned("e") + " AND e.id=a.event_id",
                persons("e.actual_employee_id", "e.signature_user_id", "e.server_submit_time", time)));
        sources.add(live("JOIN " + P + "report_allocation a ON " + liveOwner("a")
                + " AND a.lifecycle_status='CURRENT' JOIN " + P + "submission_review v ON " + owned("v") + " AND v.id=a.review_id",
                users("v.leader_user_id", "v.review_signature_user_id", "v.reviewed_at", time)));
        sources.add(live("JOIN mes_pqc_process_inspection_aggregate_detail d ON " + liveOwner("d")
                + " JOIN " + P + "pqc_record p ON " + owned("p") + " AND p.id=d.source_pqc_record_id",
                persons("p.actual_employee_id", "p.signature_user_id", "p.server_submit_time", time)));
        sources.add(live("JOIN mes_pqc_process_inspection_aggregate_detail d ON " + liveOwner("d")
                + " JOIN " + P + "submission_review v ON " + owned("v") + " AND v.id=d.review_id",
                users("v.leader_user_id", "v.review_signature_user_id", "v.reviewed_at", time)));
        sources.add(archive(rows("$.productionFacts.events", "j", personColumns()),
                time ? "(j.employee_id IS NOT NULL OR j.user_id IS NOT NULL) AND " + frozenTime("j.business_time")
                        : persons("j.employee_id", "j.user_id", "j.business_time", false)));
        sources.add(archive(rows("$.productionFacts.reviews", "j", reviewColumns()),
                time ? "(j.leader_id IS NOT NULL OR j.signature_id IS NOT NULL) AND " + frozenTime("j.business_time")
                        : users("j.leader_id", "j.signature_id", "j.business_time", false)));
        // The frozen PQC detail pins exact persisted record and review identities.
        sources.add(archive(rows("$.pqcDetails", "j", "record_id BIGINT PATH '$.sourcePqcRecordId', review_id BIGINT PATH '$.reviewId'")
                + " JOIN " + P + "pqc_record p ON " + owned("p") + " AND p.id=j.record_id",
                persons("p.actual_employee_id", "p.signature_user_id", "p.server_submit_time", time)));
        sources.add(archive(rows("$.pqcDetails", "j", "review_id BIGINT PATH '$.reviewId'")
                + " JOIN " + P + "submission_review v ON " + owned("v") + " AND v.id=j.review_id",
                users("v.leader_user_id", "v.review_signature_user_id", "v.reviewed_at", time)));
        sources.add("EXISTS (SELECT 1 FROM mes_pro_edhr_release_transaction t WHERE " + owned("t")
                + " AND t.batch_execution_id=" + B + ".id AND ("
                + user("t.submitted_by", "t.submitted_at", time) + " OR " + user("t.approved_by", "t.approved_at", time) + "))");
        sources.add("EXISTS (SELECT 1 FROM " + P + "active_order_release_application t WHERE " + owned("t")
                + " AND t.current_batch_execution_id=" + B + ".id AND ("
                + user("t.applied_by", "t.applied_at", time) + " OR " + user("t.pqc_decided_by", "t.pqc_decided_at", time) + "))");
        return "(" + String.join(" OR ", sources) + ")";
    }

    private static String equipment() {
        String liveEvents = "JOIN " + P + "report_allocation a ON " + liveOwner("a")
                + " AND a.lifecycle_status='CURRENT' JOIN " + P + "event e ON " + owned("e") + " AND e.id=a.event_id";
        String frozenEvents = rows("$.productionFacts.events", "j", "payload LONGTEXT PATH '$.rawPayload'");
        String livePqc = "JOIN mes_pqc_process_inspection_aggregate_detail d ON " + liveOwner("d");
        String frozenPqc = rows("$.pqcDetails", "d", "selected_equipment_id BIGINT PATH '$.selectedEquipmentId',"
                + "selected_equipment_code LONGTEXT PATH '$.selectedEquipmentCode',"
                + "selected_equipment_name LONGTEXT PATH '$.selectedEquipmentName',"
                + "selected_equipment_number LONGTEXT PATH '$.selectedEquipmentNumber'");
        return "(" + live(liveEvents, devices("e.raw_payload")) + " OR "
                + archive(frozenEvents, devices("j.payload")) + " OR "
                + live(livePqc, pqcEquipment()) + " OR " + archive(frozenPqc, pqcEquipment()) + ")";
    }

    private static String devices(String payload) {
        String columns = "device_id BIGINT PATH '$.deviceId', device_code LONGTEXT PATH '$.deviceCode', device_name LONGTEXT PATH '$.deviceName'";
        String predicate = "x.device_id IS NOT NULL AND (" + contains("x.device_code", "{0}") + " OR "
                + contains("x.device_name", "{0}") + " OR x.device_id IN (SELECT i.id FROM " + P + "team_device i WHERE "
                + owned("i") + " AND (LOCATE({0},i.device_code)>0 OR LOCATE({0},i.device_name)>0)))";
        return "(EXISTS (SELECT 1 FROM JSON_TABLE(" + safeJson(payload)
                + ", '$.selectedDevices[*]' COLUMNS (" + columns + ")) x WHERE " + predicate + ")"
                + " OR EXISTS (SELECT 1 FROM JSON_TABLE(" + safeJson(payload)
                + ", '$.materialDetails[*].selectedDevices[*]' COLUMNS (" + columns + ")) x WHERE " + predicate + "))";
    }

    private static String pqcEquipment() {
        return "d.selected_equipment_id IS NOT NULL AND (" + contains("d.selected_equipment_code", "{0}")
                + " OR " + contains("d.selected_equipment_name", "{0}") + " OR "
                + contains("d.selected_equipment_number", "{0}") + ")";
    }

    private static String material(String material, String lot) {
        List<String> conditions = new ArrayList<>();
        if (material != null) conditions.add("(" + contains("m.material_name", material) + " OR " + contains("m.material_number", material) + ")");
        if (lot != null) conditions.add(contains("m.lot_number", lot));
        String predicate = String.join(" AND ", conditions);
        return "(" + live("JOIN " + P + "active_order_pick_list_binding k ON " + liveOwner("k")
                + " AND k.binding_status='BOUND' JOIN " + P + "active_order_pick_list_binding_item m ON " + owned("m") + " AND m.binding_id=k.id", predicate)
                + " OR " + archive(rows("$.pickListBindingItems", "m", "material_number LONGTEXT PATH '$.materialNumber',"
                + "material_name LONGTEXT PATH '$.materialName',lot_number LONGTEXT PATH '$.lotNumber'"), predicate) + ")";
    }

    private static String live(String joins, String condition) {
        return origin(joins, "o.completion_backfill_receipt_id IS NULL AND (" + condition + ")");
    }

    private static String archive(String joins, String condition) {
        return origin("JOIN " + P + "active_order_completion_receipt r ON " + owned("r")
                + " AND r.id=o.completion_backfill_receipt_id " + joins, "(" + condition + ")");
    }

    private static String origin(String joins, String condition) {
        return "EXISTS (SELECT 1 FROM mes_pro_edhr_batch_execution_origin o " + joins + " WHERE " + owned("o")
                + " AND o.batch_execution_id=" + B + ".id AND " + condition + ")";
    }

    private static String owned(String alias) {
        return alias + ".tenant_id=" + B + ".tenant_id AND " + alias + ".deleted=0";
    }

    private static String liveOwner(String alias) {
        return owned(alias) + " AND " + alias + ".active_order_id=o.active_order_id";
    }

    private static String rows(String path, String alias, String columns) {
        String document = "JSON_EXTRACT(" + safeJson("r.formal_source_snapshot_json") + ",'" + path + "')";
        // snapshotRows supports either rows or a map of row arrays; normalize only those two shapes.
        String array = "CASE JSON_TYPE(" + document + ") WHEN 'OBJECT' THEN JSON_EXTRACT(" + document
                + ",'$.*[*]') WHEN 'ARRAY' THEN " + document + " ELSE JSON_ARRAY() END";
        return "JOIN JSON_TABLE(" + array + ", '$[*]' COLUMNS (" + columns + ")) " + alias + " ON TRUE";
    }

    private static String safeJson(String expression) {
        return "CASE WHEN JSON_VALID(" + expression + ") THEN " + expression + " ELSE NULL END";
    }

    private static String contains(String expression, String parameter) {
        return "LOCATE(LOWER(" + parameter + "),LOWER(" + expression + ") COLLATE utf8mb4_bin)>0";
    }

    private static String persons(String employee, String systemUser, String time, boolean byTime) {
        if (byTime) return "(" + employee + " IS NOT NULL OR " + systemUser + " IS NOT NULL) AND " + time + " BETWEEN {0} AND {1}";
        return "(" + employee + " IN (SELECT i.id FROM " + P + "team_employee_profile i WHERE " + owned("i")
                + " AND (LOCATE({0},i.employee_code)>0 OR LOCATE({0},i.employee_name)>0 OR LOCATE({0},i.display_name)>0)) OR "
                + user(systemUser, time, false) + ")";
    }

    private static String users(String first, String second, String time, boolean byTime) {
        return "(" + user(first, time, byTime) + " OR " + user(second, time, byTime) + ")";
    }

    private static String user(String id, String time, boolean byTime) {
        if (byTime) return "(" + id + " IS NOT NULL AND " + time + " BETWEEN {0} AND {1})";
        return id + " IN (__TRACE_SYSTEM_USER_IDS__)";
    }

    private static String personColumns() {
        return "employee_id BIGINT PATH '$.actualEmployeeId',user_id BIGINT PATH '$.signatureUserId',business_time VARCHAR(64) PATH '$.serverSubmitTime'";
    }

    private static String reviewColumns() {
        return "leader_id BIGINT PATH '$.leaderUserId',signature_id BIGINT PATH '$.reviewSignatureUserId',business_time VARCHAR(64) PATH '$.reviewedAt'";
    }

    private static String frozenTime(String field) {
        // Compare epochs in their original unit, using bounds converted by the same JVM zone as
        // the evidence reader. No dependency on MySQL session timezone or timezone table setup.
        return "(CASE WHEN " + field + " REGEXP '^-?[0-9]+$' THEN CAST(" + field
                + " AS DECIMAL(20,0)) BETWEEN {2} AND {3} ELSE CAST(REPLACE(" + field
                + ",'T',' ') AS DATETIME(6)) BETWEEN {0} AND {1} END)";
    }
}
