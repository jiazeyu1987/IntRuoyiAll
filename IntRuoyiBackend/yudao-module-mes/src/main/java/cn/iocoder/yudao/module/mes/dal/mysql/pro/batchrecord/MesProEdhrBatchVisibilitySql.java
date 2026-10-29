package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import java.util.Collection;
import java.util.stream.Collectors;

/** Database predicates shared by page, count and request-local rule resolution. No batch rows leave the database. */
public final class MesProEdhrBatchVisibilitySql {
    public static final String BATCH = "mes_pro_edhr_batch_execution";
    private static final String TASK = "mes_pro_edhr_batch_execution_task";
    private static final String RULE = "mes_pro_edhr_process_form_permission_rule";
    private static final String WORK = "mes_pro_edhr_work_task";
    private static final String HANDOVER = "mes_pro_edhr_market_release_handover";
    // Literal whitespace works in both MySQL ICU and H2 Java regex engines (POSIX classes do not).
    private static final String SPACE = "[\0 \t\n\r\f\u000B\u001c-\u001f\u00a0\u1680\u180e\u2000-\u200a\u200c\u2028\u2029\u202a\u202f\u205f\u2800\u3000\u3164\ufeff]";
    private MesProEdhrBatchVisibilitySql() {}

    private static String live(String alias) { return alias + ".deleted=0 AND " + alias + ".tenant_id=" + BATCH + ".tenant_id"; }
    private static String batch(String alias) { return live(alias) + " AND " + alias + ".batch_execution_id=" + BATCH + ".id"; }
    private static String equal(String a, String b) { return "(" + a + "=" + b + " OR (" + a + " IS NULL AND " + b + " IS NULL))"; }
    private static String binaryEqual(String a, String b) {
        return "CAST(" + a + " AS BINARY)=CAST(" + b + " AS BINARY)";
    }
    private static String exact(String column, String value) {
        // Enum columns are <=64 characters. Explicit binary comparison preserves Java equals under MySQL *_ci collations.
        return "(CAST(" + column + " AS BINARY(64))=CAST('" + value + "' AS BINARY(64)) AND CHAR_LENGTH("
                + column + ")=" + value.length() + ")";
    }
    private static String oneOf(String column, String... values) {
        return "(" + java.util.Arrays.stream(values).map(value -> exact(column,value)).collect(Collectors.joining(" OR ")) + ")";
    }
    private static String notBlank(String column) { return "NOT REGEXP_LIKE(COALESCE("+column+",''), '^"+SPACE+"*$')"; }
    private static String reportIdentity() {
        return binaryEqual("report.report_id", "t.batch_record_report_id");
    }
    private static String actor(String column, Long userId) {
        return userId == null ? column + " IS NULL" : "(" + column + " IS NOT NULL AND " + column + "=" + userId + ")";
    }

    private static String latestHandover() {
        return batch("h") + " AND h.id=(SELECT MAX(hl.id) FROM " + HANDOVER + " hl WHERE " + batch("hl") + ")";
    }
    public static String handoverGrant(Long userId) {
        return "EXISTS (SELECT 1 FROM " + HANDOVER + " h WHERE " + latestHandover()
                + " AND (" + actor("h.original_user_id", userId)
                + (userId == null ? "" : " OR (h.recalled_at IS NULL AND " + actor("h.recipient_user_id", userId) + ")") + "))";
    }
    public static String handoverReadable(Long userId) {
        return "NOT EXISTS (SELECT 1 FROM " + HANDOVER + " h WHERE " + latestHandover()
                + " AND NOT (" + actor("h.original_user_id", userId) + ") AND NOT ("
                + actor("CASE WHEN h.recalled_at IS NULL THEN h.recipient_user_id ELSE h.original_user_id END", userId)
                + ") AND EXISTS (SELECT 1 FROM " + HANDOVER + " hp WHERE " + batch("hp")
                + " AND " + actor("hp.recipient_user_id", userId) + "))";
    }
    public static String managerTasks() {
        return "SELECT w.* FROM " + WORK + " w WHERE " + batch("w")
                + " AND " + exact("w.task_type","RELEASE_APPROVE") + " AND " + exact("w.business_scope_type","RELEASE_TRANSACTION")
                + " AND w.ownership_locked=1 AND " + oneOf("w.status","TODO","DOING","DONE")
                + " AND w.business_scope_id=(SELECT MAX(rt.id) FROM mes_pro_edhr_release_transaction rt WHERE " + batch("rt") + ")";
    }
    private static String candidate(String column, Long userId) {
        if (userId == null) return "1=0";
        String digits = userId.toString();
        String number = userId < 0 ? "-0*" + digits.substring(1) : "[+]?0*" + digits;
        return "REGEXP_LIKE(" + column + ", '(^|,)" + SPACE + "*" + number + "" + SPACE + "*(,|$)')";
    }
    public static String managerGrant(Long userId) { return "EXISTS (" + managerTasks() + " AND " + candidate("w.candidate_user_snapshot", userId) + ")"; }

    private static String formalOrder() {
        return live("a") + " AND a.id>0 AND " + BATCH + ".id>0 AND " + BATCH + ".work_order_id>0"
                + " AND " + BATCH + ".route_id>0 AND " + BATCH + ".route_version_id>0"
                + " AND a.work_order_id=" + BATCH + ".work_order_id AND a.route_id=" + BATCH + ".route_id"
                + " AND a.route_version_id=" + BATCH + ".route_version_id"
                + " AND EXISTS (SELECT 1 FROM mes_pro_edhr_batch_execution_origin o WHERE " + batch("o") + " AND o.active_order_id=a.id)"
                + " AND NOT EXISTS (SELECT 1 FROM mes_pro_edhr_batch_execution_origin bad WHERE " + batch("bad")
                + " AND (bad.id<=0 OR bad.work_order_id IS NULL OR bad.work_order_id<>" + BATCH + ".work_order_id"
                + " OR bad.active_order_id IS NULL OR bad.active_order_id<>a.id))";
    }
    private static String pqcTask() { return pqcTask(true); }
    private static String pqcTask(boolean statusCheck) {
        return "SELECT w.* FROM mes_pro_process_pool_active_order_release_application app JOIN " + WORK
                + " w ON w.id=app.pqc_release_work_task_id WHERE " + batch("app") + " AND " + live("w")
                + " AND app.id>0 AND app.active_order_id=a.id AND app.work_order_id=" + BATCH + ".work_order_id"
                + " AND app.route_id=" + BATCH + ".route_id AND app.route_version_id=" + BATCH + ".route_version_id"
                + " AND app.pqc_release_work_task_id>0 AND (w.batch_execution_id IS NULL OR w.batch_execution_id=" + BATCH + ".id)"
                + " AND w.work_order_id=" + BATCH + ".work_order_id AND w.route_id=" + BATCH + ".route_id"
                + " AND " + exact("w.task_type","PQC_PRODUCTION_RELEASE") + " AND " + exact("w.business_scope_type","RELEASE_APPLICATION")
                + " AND w.business_scope_id=app.id AND w.ownership_locked=1"
                + (statusCheck ? " AND ((" + exact("app.application_status","PQC_RELEASE_PENDING") + " AND " + oneOf("w.status","TODO","DOING","OVERDUE") + ")"
                + " OR (w.batch_execution_id=" + BATCH + ".id AND " + exact("w.status","DONE") + " AND " + exact("w.reason","APPROVE")
                + " AND " + exact("app.pqc_decision","APPROVE") + " AND " + oneOf("app.application_status","REPORT_UPLOAD_PENDING","MANAGER_RELEASE_PENDING","RELEASED") + "))" : "");
    }
    public static String formalGrant(Long userId) {
        return "EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order a WHERE " + formalOrder()
                + " AND (" + actor("a.leader_user_id", userId) + " OR EXISTS (" + pqcTask()
                + " AND " + candidate("w.candidate_user_snapshot", userId) + ")))";
    }
    private static String routeTask(String t) {
        return batch(t) + " AND (" + t + ".required_flag IS NULL OR " + t + ".required_flag=1)"
                + " AND " + exact("CASE WHEN " + notBlank(t+".node_type") + " THEN " + t + ".node_type"
                + " WHEN " + notBlank(t+".batch_record_report_id") + " THEN 'ROUTE_FORM' ELSE " + t + ".process_code END", "ROUTE_FORM");
    }
    private static String priority(String t) {
        return "CASE WHEN " + t + ".status=40 OR (" + t + ".status=45 AND (" + t + ".required_policy IS NULL OR "
                + "NOT (" + exact(t+".required_policy","SKIPPABLE_CONTROLLED") + ") OR (" + t + ".skipped_by IS NOT NULL AND " + t
                + ".skipped_at IS NOT NULL))) THEN 2 WHEN " + t + ".status=0 THEN 1 ELSE 0 END";
    }
    public static String currentTaskId() {
        String p = priority("c");
        StringBuilder order = new StringBuilder(p);
        for (String column : new String[]{"route_process_sort", "route_process_id", "batch_record_sort", "id"}) {
            order.append(", CASE WHEN ").append(p).append("=2 THEN c.").append(column).append(" END DESC")
                    .append(", CASE WHEN ").append(p).append("<>2 THEN c.").append(column).append(" END ASC");
        }
        return "(SELECT COALESCE((SELECT m.id FROM " + TASK + " m WHERE " + routeTask("m")
                + " AND (c.form_slot_type IS NULL OR NOT (" + exact("c.form_slot_type","MAIN") + ")) AND " + equal("m.route_process_id", "c.route_process_id")
                + " AND " + exact("m.form_slot_type","MAIN") + " ORDER BY CASE WHEN m.batch_record_sort IS NULL THEN 1 ELSE 0 END,"
                + " m.batch_record_sort, m.route_process_sort, m.route_process_id, m.id LIMIT 1),c.id) FROM " + TASK
                + " c WHERE " + routeTask("c") + " ORDER BY " + order + " LIMIT 1)";
    }
    private static String enabledRule(String r) {
        return live(r) + " AND " + r + ".enabled=1 AND " + oneOf(r+".rule_type","FILL","EQUIPMENT_FILL","QUALITY_FILL");
    }
    /** r is the effective first rule of its type: exact version before null version; route before form. */
    private static String effectiveRule() {
        return enabledRule("r") + " AND " + binaryEqual("r.batch_record_report_id", "t.batch_record_report_id")
                + " AND (r.batch_record_version_id=t.batch_record_version_id OR (r.batch_record_version_id IS NULL"
                + " AND NOT EXISTS (SELECT 1 FROM " + RULE + " vr WHERE " + enabledRule("vr")
                + " AND " + binaryEqual("vr.batch_record_report_id", "t.batch_record_report_id") + " AND vr.batch_record_version_id=t.batch_record_version_id"
                + " AND vr.route_process_id IN (t.route_process_id,0))))"
                + " AND (r.route_process_id=t.route_process_id OR (r.route_process_id=0 AND NOT EXISTS (SELECT 1 FROM " + RULE
                + " rr WHERE " + enabledRule("rr") + " AND " + binaryEqual("rr.batch_record_report_id", "r.batch_record_report_id") + " AND "
                + equal("rr.batch_record_version_id", "r.batch_record_version_id") + " AND rr.route_process_id=t.route_process_id)))"
                + " AND NOT EXISTS (SELECT 1 FROM " + RULE + " newer WHERE " + enabledRule("newer")
                + " AND " + binaryEqual("newer.batch_record_report_id", "r.batch_record_report_id") + " AND " + equal("newer.batch_record_version_id", "r.batch_record_version_id")
                + " AND newer.route_process_id=r.route_process_id AND newer.rule_type=r.rule_type"
                + " AND ((newer.scope_key IS NULL AND r.scope_key IS NOT NULL) OR newer.scope_key<r.scope_key OR ("
                + equal("newer.scope_key", "r.scope_key") + " AND newer.id>r.id)))";
    }
    public static String taskForRule() {
        return "EXISTS (SELECT 1 FROM " + TASK + " t JOIN mes_pro_batch_record_report report"
                + " ON " + reportIdentity() + " AND report.deleted=0 AND report.tenant_id="+ BATCH + ".tenant_id"
                + " AND report.batch_record_definition_id=t.batch_record_definition_id"
                + " AND report.batch_record_version_id=t.batch_record_version_id WHERE " + batch("t")
                + " AND t.id=" + currentTaskId() + " AND t.route_process_id IS NOT NULL"
                + " AND " + notBlank("t.batch_record_report_id") + " AND " + effectiveRule() + ")";
    }
    public static String earlierGrant(Long userId) { return "(" + handoverGrant(userId) + " OR " + managerGrant(userId) + " OR " + formalGrant(userId) + ")"; }
    public static String visible(Long userId, boolean overview, Collection<Long> ruleIds) {
        String grants = overview ? "1=1" : earlierGrant(userId);
        if (!overview && !ruleIds.isEmpty()) grants += " OR EXISTS (SELECT 1 FROM " + RULE + " r WHERE r.id IN ("
                + ruleIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") AND " + taskForRule() + ")";
        return "(" + handoverReadable(userId) + " AND (" + grants + "))";
    }

    // Long.valueOf accepts signed, zero-padded decimal tokens but rejects overflow. Validate in SQL
    // before checking membership so a malformed frozen authorization cannot become an invisible row.
    private static String boundedNumber(String maximum) {
        StringBuilder result = new StringBuilder("[0-9]{1,18}|").append(maximum);
        for (int i = 0; i < maximum.length(); i++) {
            int digit = maximum.charAt(i) - '0';
            if (digit > 0) result.append('|').append(maximum, 0, i).append("[0-")
                    .append(digit - 1).append("]").append("[0-9]{").append(maximum.length() - i - 1).append('}');
        }
        return "(" + result + ")";
    }
    private static String invalidSnapshot(String column) {
        String number = "([+]?0*" + boundedNumber("9223372036854775807") + "|-0*" + boundedNumber("9223372036854775808") + ")";
        String token = "" + SPACE + "*(" + number + ")?" + SPACE + "*";
        return column + " IS NOT NULL AND NOT REGEXP_LIKE(" + column + ", '^" + token + "(," + token + ")*$')";
    }
    public static String invalidManagerSnapshot(Long userId) {
        if (userId == null) return "1=0";
        String earlier = managerTasks().replace("w.", "prior.").replace(" w WHERE", " prior WHERE");
        return handoverReadable(userId) + " AND NOT (" + handoverGrant(userId) + ") AND EXISTS ("
                + managerTasks() + " AND " + invalidSnapshot("w.candidate_user_snapshot")
                + " AND NOT EXISTS (" + earlier + " AND " + candidate("prior.candidate_user_snapshot", userId)
                + " AND (prior.create_time<w.create_time OR (prior.create_time IS NULL AND w.create_time IS NOT NULL)"
                + " OR (" + equal("prior.create_time", "w.create_time") + " AND prior.id<w.id))))";
    }
    public static String invalidPqcSnapshot(Long userId) {
        if (userId == null) return "1=0";
        return handoverReadable(userId) + " AND NOT (" + handoverGrant(userId) + " OR " + managerGrant(userId)
                + ") AND EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order a WHERE " + formalOrder()
                + " AND (a.leader_user_id IS NULL OR a.leader_user_id<>" + userId + ") AND EXISTS ("
                + pqcTask(false) + " AND " + invalidSnapshot("w.candidate_user_snapshot") + "))";
    }
    public static String duplicateApplication(Long userId) {
        return handoverReadable(userId) + " AND NOT (" + handoverGrant(userId) + " OR " + managerGrant(userId)
                + ") AND EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order a WHERE " + formalOrder()
                + " AND NOT (" + actor("a.leader_user_id", userId)
                + ") AND (SELECT COUNT(*) FROM mes_pro_process_pool_active_order_release_application app WHERE " + batch("app") + ")>1)";
    }
    public static String duplicateReport(Long userId) {
        return handoverReadable(userId) + " AND NOT " + earlierGrant(userId)
                + " AND EXISTS (SELECT 1 FROM " + TASK + " t WHERE " + batch("t") + " AND t.id=" + currentTaskId()
                + " AND t.route_process_id IS NOT NULL AND " + notBlank("t.batch_record_report_id")
                + " AND (SELECT COUNT(*) FROM mes_pro_batch_record_report report WHERE " + live("report") + " AND " + reportIdentity() + ")>1)";
    }
}
