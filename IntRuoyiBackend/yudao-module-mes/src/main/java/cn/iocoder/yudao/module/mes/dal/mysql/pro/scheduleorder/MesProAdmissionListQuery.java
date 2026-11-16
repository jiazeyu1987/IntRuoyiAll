package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.QuickFilterUtils;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

/** Request-local route outcomes are computed by the authoritative validator, never persisted or approximated. */
public final class MesProAdmissionListQuery {
    private MesProAdmissionListQuery() {}

    public record Outcome(Long productId, String admissionStatus, String severity, String reasonCode,
                          String message, String ownerRole, boolean selectable) {}

    public static final List<Outcome> BASE_OUTCOMES = List.of(
            new Outcome(null, "BLOCKED", "BLOCKED", "BLOCKED_WORK_ORDER_FROZEN",
                    "生产工单已临时冻结，不能加入排产工单池", "生产计划", false),
            new Outcome(null, "BLOCKED", "BLOCKED", "BLOCKED_WORK_ORDER_STATUS",
                    "生产工单不是已确认状态，不能加入排产工单池", "生产计划", false),
            new Outcome(null, "ALREADY_ADMITTED", "PASS", "ALREADY_ADMITTED",
                    "生产工单已在排产工单池中", "排产员", false),
            new Outcome(null, "BLOCKED", "BLOCKED", "BLOCKED_ERP_SYNC_RECORD_MISSING",
                    "生产工单缺少 ERP 正式生产订单同步记录或正式 ID/编号，不能加入排产工单池", "生产计划", false),
            new Outcome(null, "BLOCKED", "BLOCKED", "BLOCKED_MISSING_ROUTE",
                    "产品未绑定启用工艺流程排产配置", "工艺维护", false),
            new Outcome(null, "BLOCKED", "BLOCKED", "BLOCKED_ROUTE_PRODUCT_AMBIGUOUS",
                    "产品绑定了多条工艺流程排产配置，请保留唯一启用路线后再入池", "工艺维护", false));

    public static Outcome baseOutcome(String reasonCode) {
        return BASE_OUTCOMES.stream().filter(value -> value.reasonCode().equals(reasonCode)).findFirst().orElseThrow();
    }

    public static QueryWrapper<Object> baseFilter(MesProScheduleOrderAdmissionDiffPageReqVO request) {
        QueryWrapper<Object> query = new QueryWrapper<>();
        query.setParamAlias("base");
        query.eq("q.status", request.getStatus() == null ? MesProWorkOrderStatusEnum.CONFIRMED.getStatus() : request.getStatus());
        query.like(StrUtil.isNotBlank(request.getWorkOrderCode()), "q.code", request.getWorkOrderCode());
        query.like(StrUtil.isNotBlank(request.getProductCode()), "q.product_code", request.getProductCode());
        query.like(StrUtil.isNotBlank(request.getProductName()), "q.product_name", request.getProductName());
        query.like(StrUtil.isNotBlank(request.getProductSpecification()), "q.product_specification", request.getProductSpecification());
        if (request.getQuantity() != null) {
            if (request.getQuantity().length > 0) query.ge(request.getQuantity()[0] != null, "q.quantity", request.getQuantity()[0]);
            if (request.getQuantity().length > 1) query.le(request.getQuantity()[1] != null, "q.quantity", request.getQuantity()[1]);
        }
        if (request.getRequestDate() != null) {
            if (request.getRequestDate().length > 0) query.ge(request.getRequestDate()[0] != null, "q.request_date", request.getRequestDate()[0]);
            if (request.getRequestDate().length > 1) query.le(request.getRequestDate()[1] != null, "q.request_date", request.getRequestDate()[1]);
        }
        var quick = request.getQuickFilter();
        if (quick != null && StrUtil.isNotBlank(quick.getFieldKey())) {
            String value = StrUtil.trim(quick.getValue());
            switch (StrUtil.trim(quick.getFieldKey())) {
                case "workOrderCode" -> query.like(StrUtil.isNotBlank(value), "q.code", value);
                case "productCode" -> query.like(StrUtil.isNotBlank(value), "q.product_code", value);
                case "productName" -> query.like(StrUtil.isNotBlank(value), "q.product_name", value);
                case "productSpecification" -> query.like(StrUtil.isNotBlank(value), "q.product_specification", value);
                case "admissionStatus" -> { /* Derived predicate belongs to filter(), after real-time validation. */ }
                case "requestDate" -> {
                    var field = QuickFilterUtils.QuickFilterField.localDateTimeRange(MesProWorkOrderDO::getRequestDate);
                    if (!field.supports(quick.getOperator()) || StrUtil.isBlank(value) || StrUtil.isBlank(quick.getValueEnd()))
                        throw new IllegalArgumentException("需求日期快速过滤必须提供有效日期范围");
                    query.between("q.request_date", field.convert(value), field.convertEnd(quick.getValueEnd()));
                }
                default -> throw new IllegalArgumentException("非法待同步差异快速过滤字段：" + quick.getFieldKey());
            }
        }
        return query;
    }

    public static QueryWrapper<Object> filter(MesProScheduleOrderAdmissionDiffPageReqVO request) {
        QueryWrapper<Object> query = new QueryWrapper<>();
        String admissionStatus = request.getAdmissionStatus();
        var quick = request.getQuickFilter();
        if (quick != null && "admissionStatus".equals(StrUtil.trim(quick.getFieldKey())) && StrUtil.isNotBlank(quick.getValue()))
            admissionStatus = StrUtil.trim(quick.getValue());
        exact(query, "q.admission_status", admissionStatus);
        exact(query, "q.reason_code", request.getReasonCode());
        exact(query, "q.owner_role", request.getOwnerRole());
        if (StrUtil.isNotBlank(request.getMessage()))
            query.apply("LOCATE(CAST({0} AS BINARY),CAST(q.message AS BINARY)) > 0", request.getMessage());
        String reasonText = reasonText("q.reason_code");
        return ListFilterQueryUtils.apply(request, query, Map.ofEntries(
                entry("workOrderCode", text("q.code")), entry("productCode", text("q.product_code")),
                entry("productName", text("q.product_name")), entry("productSpecification", text("q.product_specification")),
                entry("analysis", text("CASE WHEN q.selectable=1 THEN '可加入' ELSE COALESCE(NULLIF(q.message,'')," + reasonText + ") END")),
                entry("quantity", number("FLOOR(COALESCE(q.quantity,0)+0.5)")),
                entry("requestDate", dateTime("q.request_date")),
                entry("admissionStatus", text("CASE q.admission_status WHEN 'READY_TO_ADMIT' THEN '可入池' WHEN 'ALREADY_ADMITTED' THEN '已入池' WHEN 'BLOCKED' THEN '阻断' WHEN 'WARN' THEN '警告' ELSE '未知' END")),
                entry("message", text("CONCAT(" + reasonText + ",' ',COALESCE(NULLIF(q.message,'')," + reasonText + "))")),
                entry("ownerRole", text("q.owner_role"))));
    }

    private static void exact(QueryWrapper<Object> query, String column, String value) {
        if (StrUtil.isNotBlank(value)) query.apply("CAST(" + column + " AS BINARY)=CAST({0} AS BINARY)", value);
    }

    private static String reasonText(String column) {
        return "CASE " + column + " WHEN 'READY_TO_ADMIT' THEN '可入池' WHEN 'ALREADY_ADMITTED' THEN '已入池'"
                + " WHEN 'BLOCKED_WORK_ORDER_FROZEN' THEN '工单已冻结' WHEN 'BLOCKED_WORK_ORDER_STATUS' THEN '工单未确认'"
                + " WHEN 'BLOCKED_MISSING_ROUTE' THEN '缺路线' WHEN 'BLOCKED_ROUTE_DISABLED' THEN '路线未启用'"
                + " WHEN 'BLOCKED_ROUTE_VERSION_MISSING' THEN '缺路线版本' WHEN 'BLOCKED_ROUTE_PROCESS_MISSING' THEN '缺路线工序'"
                + " WHEN 'BLOCKED_ROUTE_PROCESS_SCHEDULE_USE_MISSING' THEN '缺智能排产用途' WHEN 'BLOCKED_ROUTE_PROCESS_DISABLED_FOR_SCHEDULE' THEN '智能排产已关闭'"
                + " WHEN 'BLOCKED_ROUTE_SCHEDULE_CONFIG_MISSING' THEN '缺排产策略' WHEN 'BLOCKED_SHIFT_HOURS_REQUIRED' THEN '缺班次小时'"
                + " WHEN 'BLOCKED_INVALID_FINITE_CAPACITY' THEN '缺产能' WHEN 'BLOCKED_INVALID_INFINITE_FORMULA' THEN '缺无限产能公式'"
                + " WHEN 'BLOCKED_CALENDAR_RULE_MISSING' THEN '缺日历' WHEN 'BLOCKED_ERP_SYNC_RECORD_MISSING' THEN '缺 ERP 正式订单'"
                + " WHEN 'WARN_ERP_SYNC_RECORD_MISSING' THEN '缺 ERP 同步证据' ELSE '未知原因' END";
    }

    public static String products() {
        return "<script>SELECT DISTINCT q.product_id FROM (" + baseProjection() + ") q WHERE q.base_reason_code IS NULL"
                + baseWhere() + "</script>";
    }

    public static String summary() {
        return query("COUNT(*) AS total,COALESCE(SUM(CASE WHEN q.severity='WARN' THEN 1 ELSE 0 END),0) AS warnCount,"
                + "COALESCE(SUM(CASE WHEN q.severity&lt;&gt;'WARN' AND q.admission_status='READY_TO_ADMIT' THEN 1 ELSE 0 END),0) AS readyCount,"
                + "COALESCE(SUM(CASE WHEN q.admission_status='ALREADY_ADMITTED' THEN 1 ELSE 0 END),0) AS alreadyAdmittedCount,"
                + "COALESCE(SUM(CASE WHEN q.admission_status&lt;&gt;'ALREADY_ADMITTED' AND q.severity='BLOCKED' THEN 1 ELSE 0 END),0) AS blockedCount", false);
    }

    public static String page() { return query("q.id", true); }

    private static String query(String select, boolean page) {
        String result = "<script>SELECT " + select + " FROM (" + outcomeProjection() + ") q WHERE 1=1"
                + baseWhere() + " <if test=\"ew != null and !ew.emptyOfNormal\"> AND (${ew.sqlSegment}) </if>";
        if (page) result += " ORDER BY q.temporary_frozen ASC,q.id DESC <if test=\"req.pageSize != -1\"> LIMIT #{req.pageSize} OFFSET #{offset} </if>";
        return result + "</script>";
    }

    private static String baseWhere() {
        return " <if test=\"base != null and !base.emptyOfNormal\"> AND (${base.sqlSegment}) </if>";
    }

    private static String baseProjection() {
        String feedbackStatuses = MesProFeedbackStatusEnum.APPROVING.getStatus() + "," + MesProFeedbackStatusEnum.UNCHECK.getStatus() + "," + MesProFeedbackStatusEnum.FINISHED.getStatus();
        return "SELECT x.*,CASE WHEN x.temporary_frozen=1 THEN 'BLOCKED_WORK_ORDER_FROZEN'"
                + " WHEN x.status IS NULL OR x.status&lt;&gt;" + MesProWorkOrderStatusEnum.CONFIRMED.getStatus() + " THEN 'BLOCKED_WORK_ORDER_STATUS'"
                + " WHEN x.blocking_schedule_order_id IS NOT NULL THEN 'ALREADY_ADMITTED'"
                + " WHEN x.formal_erp_identity=0 THEN 'BLOCKED_ERP_SYNC_RECORD_MISSING' WHEN x.route_binding_count=0 THEN 'BLOCKED_MISSING_ROUTE' WHEN x.route_binding_count>1 THEN 'BLOCKED_ROUTE_PRODUCT_AMBIGUOUS' ELSE NULL END AS base_reason_code"
                + " FROM (SELECT w.*,i.code AS product_code,i.name AS product_name,i.specification AS product_specification,COALESCE(rp.route_binding_count,0) AS route_binding_count,"
                + "(SELECT so.id FROM mes_pro_schedule_order so WHERE so.work_order_id=w.id AND so.tenant_id=w.tenant_id AND so.deleted=0"
                + " AND (COALESCE(so.removed_from_schedule,0)=0 OR so.reentry_blocked=1"
                + " OR EXISTS(SELECT 1 FROM mes_pro_feedback f WHERE f.schedule_order_id=so.id AND f.tenant_id=w.tenant_id AND f.deleted=0 AND f.status IN (" + feedbackStatuses + "))"
                + " OR EXISTS(SELECT 1 FROM mes_pro_process_pool_active_order a WHERE a.work_order_id=w.id AND a.tenant_id=w.tenant_id AND a.deleted=0 AND a.active_status IN ('ACTIVE','REMOVED') AND COALESCE(a.simulated,0)=0)) ORDER BY so.id DESC LIMIT 1) AS blocking_schedule_order_id,"
                + "EXISTS(SELECT 1 FROM mes_kingdee_production_order_sync_record s WHERE s.work_order_id=w.id AND s.tenant_id=w.tenant_id AND s.deleted=0 AND s.source_fid REGEXP '[^[:space:]]' AND s.source_bill_no REGEXP '[^[:space:]]') AS formal_erp_identity"
                + " FROM mes_pro_work_order w LEFT JOIN mes_md_item i ON i.id=w.product_id AND i.tenant_id=w.tenant_id AND i.deleted=0"
                + " LEFT JOIN (SELECT tenant_id,item_id,COUNT(*) AS route_binding_count FROM mes_pro_route_product WHERE tenant_id=#{tenantId} AND deleted=0 GROUP BY tenant_id,item_id) rp ON rp.item_id=w.product_id AND rp.tenant_id=w.tenant_id WHERE w.tenant_id=#{tenantId} AND w.deleted=0) x";
    }

    private static String outcomeProjection() {
        return "SELECT b.*,COALESCE(bo.admission_status,ro.admission_status) AS admission_status,"
                + "COALESCE(bo.severity,ro.severity) AS severity,COALESCE(bo.reason_code,ro.reason_code) AS reason_code,"
                + "COALESCE(bo.message,ro.message) AS message,COALESCE(bo.owner_role,ro.owner_role) AS owner_role,"
                + "COALESCE(bo.selectable,ro.selectable) AS selectable FROM (" + baseProjection() + ") b"
                + " LEFT JOIN " + outcomeTable("baseOutcomes", "bo") + " ON bo.reason_code=b.base_reason_code"
                + " LEFT JOIN " + outcomeTable("routeOutcomes", "ro") + " ON b.base_reason_code IS NULL AND ro.product_id&lt;=&gt;b.product_id";
    }

    private static String outcomeTable(String parameter, String alias) {
        return "(<choose><when test=\"" + parameter + " != null and !" + parameter + ".isEmpty()\">"
                + "<foreach collection=\"" + parameter + "\" item=\"outcome\" separator=\" UNION ALL \">"
                + "SELECT #{outcome.productId} AS product_id,#{outcome.admissionStatus} AS admission_status,"
                + "#{outcome.severity} AS severity,#{outcome.reasonCode} AS reason_code,#{outcome.message} AS message,"
                + "#{outcome.ownerRole} AS owner_role,#{outcome.selectable} AS selectable"
                + "</foreach></when><otherwise>SELECT NULL AS product_id,NULL AS admission_status,NULL AS severity,"
                + "NULL AS reason_code,NULL AS message,NULL AS owner_role,NULL AS selectable WHERE 1=0</otherwise></choose>) " + alias;
    }
}
