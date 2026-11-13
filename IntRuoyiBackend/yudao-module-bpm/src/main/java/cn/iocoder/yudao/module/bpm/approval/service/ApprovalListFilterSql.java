package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.textCollection;

/** SQL counterparts of approval-center's column getValue functions. Expressions are provider-owned SQL. */
public final class ApprovalListFilterSql {
    private ApprovalListFilterSql() { }

    public static Map<String, Field> fields(Map<String, String> facts, boolean edhrReadView) {
        String taskLabel = "CASE " + value(facts,"taskOutcome")
                + " WHEN 'CREATED' THEN '交接已创建' WHEN 'WAIT' THEN '待处理' WHEN 'RUNNING' THEN '处理中'"
                + " WHEN 'COMPLETED' THEN '任务已完成' WHEN 'CANCELLED' THEN '已取消' WHEN 'OVERDUE' THEN '已逾期'"
                + " ELSE CONCAT('任务状态未识别（'," + recorded(value(facts,"sourceStatus")) + ",'）') END";
        String decisionLabel = "CASE WHEN " + value(facts,"decisionState") + "='APPROVE' AND "
                + value(facts,"result") + "='APPROVE' THEN '审批通过' WHEN " + value(facts,"decisionState")
                + "='REJECT' AND " + value(facts,"result") + "='REJECT' THEN '审批驳回' WHEN "
                + value(facts,"decisionState") + "='UNSUPPORTED' THEN CONCAT('决定未识别（',"
                + recorded(value(facts,"decisionSourceStatus")) + ",'）') ELSE '决定未知' END";
        String applicant = edhrReadView ? "'姓名未记录'" : value(facts,"applicant");
        String reviewer = edhrReadView ? "CONCAT('姓名未记录；'," + identity(value(facts,"decisionActor")) + ")"
                : value(facts,"reviewer");
        String nodeSub = edhrReadView ? "'正式决定人见审核人列'" : "CASE WHEN " + reviewer
                + "<>'--' THEN CONCAT('审核人：'," + reviewer + ") ELSE " + value(facts,"nodeStatus") + " END";
        String summary = join(value(facts,"title"), value(facts,"identifier"), taskLabel,
                recorded(value(facts,"sourceStatus")),recorded(value(facts,"businessStatus")),decisionLabel,
                recorded(value(facts,"decisionSourceStatus")),recorded(value(facts,"remark")),
                value(facts,"sourceRemark"),value(facts,"terminationReason"));
        if (facts.containsKey("extraSummary")) summary = join(summary,value(facts,"extraSummary"));
        return Map.of(
                "source",text(join(value(facts,"moduleLabel"),value(facts,"sourceLabel"))),
                "capabilities",textCollection(value(facts,"capabilities"),
                        "SELECT 1 FROM (SELECT 'AUDIT' capability UNION ALL SELECT 'EVIDENCE_LEDGER' UNION ALL SELECT 'NOTIFICATION' UNION ALL SELECT 'REMINDER' UNION ALL SELECT 'SIGNATURE_AUTHORIZATION' UNION ALL SELECT 'TIMELINE') afc WHERE FIND_IN_SET(afc.capability,REPLACE("+value(facts,"capabilities")+",' ',','))>0","afc.capability"),
                "businessSummary",text(summary),"applicant",text(applicant),
                "node",text(join(value(facts,"nodeName"),nodeSub)),"reviewer",text(reviewer),
                "approvalResult",text(join(decisionLabel,recorded(value(facts,"decisionSourceStatus")),
                        "CASE " + value(facts,"result") + " WHEN 'APPROVE' THEN '通过' WHEN 'REJECT' THEN '驳回' ELSE '' END")),
                "approvalRemark",text(join(recorded(value(facts,"remark")),value(facts,"sourceRemark"),
                        value(facts,"terminationReason"),recorded(value(facts,"evidenceType")),recorded(value(facts,"evidenceId")),
                        "COALESCE(NULLIF("+value(facts,"remark")+",''),'--')")),
                "time",text(join(time(value(facts,"created"),"未记录"),time(value(facts,"completed"),"未记录"),
                        identity(value(facts,"completedBy")),time(value(facts,"decisionAt"),"未记录"),
                        time("COALESCE("+value(facts,"created")+","+value(facts,"initiated")+")","--"),
                        time(value(facts,"completed"),"--"))));
    }

    /** Mirrors ApprovalCenterServiceImpl enrichment, including unreadable/missing account names. */
    public static String userLabel(String id, String tenant) {
        String source = " FROM system_users afu WHERE afu.id=("+id+") AND afu.tenant_id=("+tenant+") AND afu.deleted=0";
        String username="NULLIF(TRIM(afu.username),'')",nickname="NULLIF(TRIM(afu.nickname),'')";
        String name="CASE WHEN LOCATE('??',"+nickname+")>0 THEN COALESCE("+username+","+nickname+")"
                +" WHEN "+nickname+" IS NOT NULL AND "+username+" IS NOT NULL AND "+nickname+"<>"+username
                +" THEN CONCAT("+nickname+",'(',"+username+",')') ELSE COALESCE("+nickname+","+username+") END";
        return "CASE WHEN ("+id+") IS NULL THEN '--' WHEN NOT EXISTS (SELECT 1"+source+")"
                +" THEN CONCAT('用户不存在(',"+id+",')') ELSE COALESCE((SELECT "+name+source+"),CONCAT('用户 #',"+id+")) END";
    }
    public static String literal(String text) { return "'"+text.replace("'","''")+"'"; }
    public static String recorded(String sql) { return "CASE WHEN ("+sql+") IS NULL OR TRIM(CAST(("+sql+") AS CHAR))='' THEN '未记录' ELSE CAST(("+sql+") AS CHAR) END"; }
    public static String identity(String sql) { return "CASE WHEN ("+sql+") IS NULL THEN '未记录' ELSE CONCAT('系统用户 ID：',"+sql+") END"; }
    public static String join(String... sql) {
        return "CONCAT("+java.util.Arrays.stream(sql).map(s->"COALESCE(CAST(("+s+") AS CHAR),'')")
                .collect(java.util.stream.Collectors.joining(", ' ',"))+")";
    }
    private static String value(Map<String,String> facts,String key) { return facts.getOrDefault(key,"NULL"); }
    private static String time(String sql,String empty) { return "COALESCE(DATE_FORMAT(("+sql+"),'%Y-%m-%d %H:%i:%s'),'"+empty+"')"; }
}
