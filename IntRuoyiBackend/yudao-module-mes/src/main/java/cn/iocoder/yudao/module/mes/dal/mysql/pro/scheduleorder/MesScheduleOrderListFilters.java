package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/** Display projections are filtered in SQL before the list count and page limit. */
final class MesScheduleOrderListFilters {
    private MesScheduleOrderListFilters() {}
    private static final String ORDER = "mes_pro_schedule_order";
    private static final String PROCESS_SCOPE = "p.schedule_order_id=" + ORDER + ".id AND p.tenant_id=" + ORDER + ".tenant_id AND p.deleted=0 AND p.enabled=1";
    private static final String REPORTED = "COALESCE((SELECT SUM(ROUND(COALESCE(f.feedback_quantity,0),6)) FROM mes_pro_feedback f WHERE f.schedule_order_id=" + ORDER + ".id AND f.schedule_order_process_id=p.id AND f.tenant_id=" + ORDER + ".tenant_id AND f.deleted=0 AND f.status IN (2,3,4)),0)";
    private static final String PLANNED = "ROUND(COALESCE(p.planned_quantity,0),6)";
    private static final String CURRENT_PROGRESS = "CASE WHEN " + PLANNED + "<=0 THEN 0 ELSE ROUND(LEAST(" + REPORTED + "," + PLANNED + ")*100/" + PLANNED + ",6) END";
    private static final String ENABLED_TOTAL = "(SELECT SUM(" + PLANNED + ") FROM mes_pro_schedule_order_process p WHERE " + PROCESS_SCOPE + ")";
    private static final String ENABLED_COMPLETED = "(SELECT SUM(LEAST(" + REPORTED + "," + PLANNED + ")) FROM mes_pro_schedule_order_process p WHERE " + PROCESS_SCOPE + ")";
    private static final String TOTAL = "CASE WHEN manual_finished=1 THEN COALESCE(total_quantity,quantity) ELSE COALESCE(" + ENABLED_TOTAL + ",quantity) END";
    private static final String PROGRESS = "CASE WHEN manual_finished=1 THEN COALESCE(progress_percent,0) WHEN COALESCE(" + ENABLED_TOTAL + ",0)<=0 THEN 0 ELSE ROUND(" + ENABLED_COMPLETED + "*100/" + ENABLED_TOTAL + ",6) END";
    private static final String SOURCE_STATUS = "(SELECT w.status FROM mes_pro_work_order w WHERE w.id=" + ORDER + ".work_order_id AND w.tenant_id=" + ORDER + ".tenant_id AND w.deleted=0)";
    private static final String ISSUE_MESSAGE = "(SELECT i.message FROM mes_pro_schedule_issue i WHERE i.work_order_id=" + ORDER + ".work_order_id AND i.tenant_id=" + ORDER + ".tenant_id AND i.deleted=0 AND i.severity='BLOCKING' AND COALESCE(i.resolved,0)=0 ORDER BY i.id DESC LIMIT 1)";

    static Map<String, ListFilterQueryUtils.Field> augment(Map<String, ListFilterQueryUtils.Field> base) {
        Map<String, ListFilterQueryUtils.Field> fields = new LinkedHashMap<>(base);
        fields.put("displayColumn1", ListFilterQueryUtils.text("CASE WHEN removed_from_schedule=1 THEN '排产工单已删除' WHEN frozen=1 THEN '已冻结' WHEN " + SOURCE_STATUS + "=2 THEN '生产工单已完成' WHEN " + SOURCE_STATUS + "=3 THEN '生产工单已取消' WHEN status=3 THEN '已完成' WHEN status=4 THEN '已取消' ELSE '不满足重排条件' END"));
        fields.put("erpWorkOrderCode", ListFilterQueryUtils.text("CONCAT(COALESCE(erp_work_order_code,'undefined'),CASE WHEN manual_finished=1 OR status=3 THEN '(已完成)' ELSE '' END,' ',CASE WHEN " + SOURCE_STATUS + "=2 THEN '生产工单已完成' WHEN " + SOURCE_STATUS + "=3 THEN '生产工单已取消' ELSE COALESCE(NULLIF(TRIM(" + ISSUE_MESSAGE + "),''),'存在阻断问题') END)"));
        fields.put("progressPercent", ListFilterQueryUtils.text("CONCAT(CAST(FLOOR(COALESCE(" + TOTAL + ",0)+0.5) AS CHAR),' ',CAST(CAST(ROUND(GREATEST(0,LEAST(100,COALESCE(" + PROGRESS + ",0))),2) AS DECIMAL(12,2)) AS CHAR))"));
        fields.put("currentProcessName", ListFilterQueryUtils.text("COALESCE((SELECT CONCAT(COALESCE(NULLIF(d.name,''),NULLIF(d.code,''),CAST(p.process_id AS CHAR),''),' ',COALESCE(NULLIF(d.name,''),NULLIF(d.code,''),CAST(p.process_id AS CHAR),''),' ',CAST(CAST(ROUND(GREATEST(0,LEAST(100," + CURRENT_PROGRESS + ")),2) AS DECIMAL(12,2)) AS CHAR)) FROM mes_pro_schedule_order_process p LEFT JOIN mes_pro_process d ON d.id=p.process_id AND d.tenant_id=p.tenant_id AND d.deleted=0 WHERE " + PROCESS_SCOPE + " AND (" + CURRENT_PROGRESS + ")<100 ORDER BY p.sort IS NULL,p.sort,p.id LIMIT 1),'  0.00')"));
        fields.put("displayColumn16", ListFilterQueryUtils.text("CONCAT(COALESCE(DATE_FORMAT(removed_from_schedule_time,'%Y-%m-%d %H:%i:%s'),'--'),' ',COALESCE(NULLIF(removed_from_schedule_reason,''),'-'))"));
        return fields;
    }
}
