package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterSql;
import java.util.LinkedHashMap;
import java.util.Map;
import static cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterSql.*;

/** SQL equivalents of the DCC summaries rendered by approval-center, before count/window. */
public final class DccApprovalListFilterSql {
    private DccApprovalListFilterSql() { }
    public static Map<String,Field> flowable(boolean done) {
        String fileSource=" FROM dcc_controlled_file df WHERE df.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) AND CAST(CAST(df.id AS CHAR) AS BINARY)=CAST(o.authority_id AS BINARY)";
        java.util.function.Function<String,String> file=column->"(SELECT df."+column+fileSource+")";
        String exists="EXISTS(SELECT 1"+fileSource+")";
        String node="CASE WHEN "+exists+" AND d.KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete') AND t.TASK_DEF_KEY_='MATRIX_REVIEW' THEN CASE WHEN "+taskVariable(done,"dccAssignmentSignatureId")+" IS NULL THEN '部门负责人签名指派' ELSE '实际会签人会签' END ELSE t.NAME_ END";
        String code="CASE WHEN "+exists+" THEN "+file.apply("file_number")+" ELSE p.BUSINESS_KEY_ END";
        String title="CASE WHEN "+exists+" THEN "+file.apply("title")+" ELSE '已删除文控文件' END";
        String status="CASE WHEN "+exists+" THEN "+file.apply("status")+" ELSE 'DELETED' END";
        String version="COALESCE(NULLIF(TRIM("+file.apply("version_no")+"),''),'-')";
        String category="CASE WHEN NOT "+exists+" THEN '已删除记录' ELSE COALESCE((SELECT NULLIF(TRIM(dc.name),'') FROM dcc_file_category dc WHERE dc.id="+file.apply("category_id")+" AND dc.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) AND dc.deleted=0),CASE WHEN "+file.apply("category_id")+" IS NULL THEN '-' ELSE CONCAT('缺失类别#',"+file.apply("category_id")+") END) END";
        String stamp="CASE WHEN NOT "+exists+" THEN '记录已删除' WHEN "+file.apply("stamped_file_id")+" IS NOT NULL THEN '已生成' ELSE '需要' END";
        String distribution="CASE WHEN NOT "+exists+" THEN '记录已删除' WHEN (SELECT dc.distribution_required FROM dcc_file_category dc WHERE dc.id="+file.apply("category_id")+" AND dc.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) AND dc.deleted=0)=1 THEN '需要' ELSE '不需要' END";
        String result=done ? "CASE CAST("+taskVariable(true,"TASK_STATUS")+" AS SIGNED) WHEN 2 THEN 'APPROVE' WHEN 7 THEN 'APPROVE' WHEN 3 THEN 'REJECT' WHEN 4 THEN 'REJECT' WHEN 5 THEN 'REJECT' END" : "NULL";
        String remark=done ? "CASE WHEN ("+result+")='REJECT' THEN NULLIF(TRIM("+taskVariable(true,"TASK_REASON")+"),'') END" : "NULL";
        Map<String,String> f=base("'文控受控文件任务'",title,code,status,node,
                userLabel("CAST(NULLIF(p.START_USER_ID_,'') AS UNSIGNED)","CAST(t.TENANT_ID_ AS UNSIGNED)"),"'--'",result,remark,done?"t.START_TIME_":"t.CREATE_TIME_",done?"t.END_TIME_":"NULL","NULL");
        f.put("extraSummary",joinVisible(visibleField("文件编号",code),visibleField("版本",contextValue(version)),visibleField("文件类型",contextValue(category)),visibleField("当前审批节点",nodeLabel(node,"t.TASK_DEF_KEY_")),
                visibleTag("文件编号",code),visibleTag("版本",version),visibleTag("分类",category),visibleTag("当前节点",node),visibleTag("盖章",stamp),visibleTag("分发",distribution)));
        f.put("nodeName",nodeLabel(node,"t.TASK_DEF_KEY_"));
        f.put("capabilities","'AUDIT EVIDENCE_LEDGER NOTIFICATION SIGNATURE_AUTHORIZATION TIMELINE'");
        return ApprovalListFilterSql.fields(f,false);
    }
    public static Map<String,Field> project(boolean review,boolean done,Long approver) {
        String table="dcc_project_product_create_request.";
        String role=table+"review_contract='ROLE_SINGLE_REVIEW_V1'";
        String node="CASE WHEN "+role+" THEN '项目审核员' ELSE '"+(review?"项目产品审核":"项目产品批准")+"' END";
        String rejected=table+"status='REJECTED'"+(review?" AND "+table+"approved_time IS NULL":"");
        String result=done?"CASE WHEN "+rejected+" THEN 'REJECT' ELSE 'APPROVE' END":"NULL";
        String remark=done?"CASE WHEN "+rejected+" THEN "+table+"reject_reason ELSE "+table+(review?"review_reason":"approval_reason")+" END":"NULL";
        String reviewer="CASE WHEN "+role+" THEN "+(done?table+"reviewer_user_id":"NULL")+" ELSE "+(review?table+"configured_reviewer_user_id":done?table+"approver_user_id":String.valueOf(approver))+" END";
        Map<String,String> f=base(review?"'项目产品审核'":"'项目产品批准'","CONCAT("+table+"project_name,' · ',"+table+"product_name)",table+"project_code",table+"status",node,userLabel(table+"applicant_user_id",table+"tenant_id"),userLabel(reviewer,table+"tenant_id"),result,remark,table+(review?"submitted_time":"reviewed_time"),done?table+(review?"reviewed_time":"approved_time"):"NULL",table+"submitted_time");
        f.put("extraSummary","CONCAT('项目代码 ',"+table+"project_code,' 当前审批节点 ',"+node+")");
        f.put("capabilities","'AUDIT NOTIFICATION SIGNATURE_AUTHORIZATION TIMELINE'");
        return ApprovalListFilterSql.fields(f,false);
    }
    public static Map<String,Field> training(String node,Long actor) {
        String t="dcc_controlled_file.";
        Map<String,String> f=base("'文控上传线下培训记录'",t+"title",t+"file_number",t+"status",node,userLabel(t+"requester_id",t+"tenant_id"),userLabel(actor.toString(),t+"tenant_id"),"NULL","NULL",t+"approved_time","NULL",t+"submitted_time");
        String version=contextValue(t+"version_no");
        f.put("extraSummary",joinVisible(visibleField("文件编号",t+"file_number"),visibleField("版本",version),visibleField("当前审批节点",node),visibleTag("版本",t+"version_no")));
        f.put("capabilities","NULL");
        return ApprovalListFilterSql.fields(f,false);
    }
    public static String joinVisible(String... values) {
        return "CONCAT_WS(' ',"+java.util.Arrays.stream(values).map(value->"NULLIF("+value+",'')").collect(java.util.stream.Collectors.joining(","))+")";
    }
    static Map<String,String> base(String source,String title,String code,String status,String node,String applicant,String reviewer,String result,String remark,String created,String completed,String initiated) {
        Map<String,String> f=new LinkedHashMap<>();
        f.put("moduleLabel","'文控审批'");f.put("sourceLabel",source);f.put("title",titleLabel(title));
        f.put("identifier","CASE WHEN NULLIF(TRIM("+code+"),'') IS NOT NULL THEN CONCAT('业务编号：',TRIM("+code+")) ELSE '--' END");
        f.put("businessStatus",status);f.put("nodeName",nodeLabel(node,node));f.put("nodeStatus",statusLabel(status));
        f.put("applicant",applicant);f.put("reviewer",reviewer);f.put("result",result);f.put("remark",remark);
        f.put("created",created);f.put("completed",completed);f.put("initiated",initiated);return f;
    }
    static String taskVariable(boolean done,String name) {
        return "(SELECT COALESCE(v.TEXT_,CAST(v.LONG_ AS CHAR)) FROM "+(done?"ACT_HI_VARINST":"ACT_RU_VARIABLE")+" v WHERE v.TASK_ID_=t.ID_ AND v.NAME_='"+name+"' LIMIT 1)";
    }
    public static String titleLabel(String value) {
        return "COALESCE(NULLIF(REPLACE(REPLACE(REPLACE(TRIM("+value+"),'DCC Controlled File Approval','文控受控文件审批'),'Expense Dept Leader Approval','费用部门负责人审批'),'eDHR Approval V1','电子批记录审批第一版'),''),'--')";
    }
    public static String statusLabel(String value) {
        String[] codes={"MY_INITIATED","TODO","DONE","CC","APPROVE","APPROVED","REJECT","REJECTED","CANCELED","CANCELLED","RUNNING","PENDING","PENDING_REVIEW","PENDING_APPROVAL","WRITING","COMPLETED","WRITE_FAILED","PENDING_SUPERVISOR_REVIEW","PENDING_MATRIX_REVIEW","PENDING_MATRIX_APPROVAL","PENDING_DOC_CONTROL_REVIEW","PENDING_APPLICANT_REWORK","PENDING_APPLICANT_TRAINING_RECORD","PENDING_DELAYED_EFFECTIVE_CONFIRM","CONTROLLED_PENDING_EFFECTIVE","WORKING","DRAFT","NEW","SUBMITTED","APPROVING","ACTIVE","SUPERSEDED","OBSOLETE","WITHDRAWN","DELETED"};
        String[] labels={"我发起的","待办","已办","抄送我的","通过","已通过","驳回","已驳回","已取消","已取消","进行中","待处理","待审核","待批准","写入中","已完成","写入失败","待直属主管审核","待会签","待批准","待文控审核","待申请人返工","待文控上传培训记录","待延迟生效确认","受控（待生效）","工作小版本","草稿","新建","已提交","审批中","现行","已替代","已作废","已撤回","已删除"};
        StringBuilder sql=new StringBuilder("CASE BINARY TRIM(").append(value).append(")");
        for(int i=0;i<codes.length;i++)sql.append(" WHEN '").append(codes[i]).append("' THEN '").append(labels[i]).append("'");
        return sql.append(" ELSE CASE WHEN NULLIF(TRIM(").append(value).append("),'') IS NULL THEN '--' WHEN ").append(value).append(" REGEXP '[A-Za-z]' THEN '未配置中文状态' ELSE TRIM(").append(value).append(") END END").toString();
    }
    public static String nodeLabel(String name,String code) {
        String n="dnf.node_name",c="dnf.node_code";
        String mappedName="CASE BINARY "+n+" WHEN 'APPROVE_USER_SELECT' THEN '选择审批人' WHEN 'START' THEN '开始' WHEN 'START_EVENT' THEN '开始' WHEN 'END' THEN '结束' WHEN 'END_EVENT' THEN '结束' ELSE "+statusLabel(n)+" END";
        String mappedCode="CASE BINARY "+c+" WHEN 'APPROVE_USER_SELECT' THEN '选择审批人' WHEN 'START' THEN '开始' WHEN 'START_EVENT' THEN '开始' WHEN 'END' THEN '结束' WHEN 'END_EVENT' THEN '结束' ELSE REPLACE("+statusLabel(c)+",'未配置中文状态','未配置中文节点') END";
        return "(SELECT CASE WHEN NULLIF(TRIM("+n+"),'') IS NOT NULL AND "+n+" NOT REGEXP '[A-Za-z]' THEN TRIM("+n+") WHEN "+mappedName+" NOT IN ('--','未配置中文状态') THEN "+mappedName+" ELSE "+mappedCode+" END FROM (SELECT "+name+" node_name,"+code+" node_code) dnf)";
    }
    public static String contextValue(String value) {
        String v="dcf.context_value",translated=titleLabel(v);
        return "(SELECT CASE WHEN NULLIF(TRIM("+v+"),'') IS NULL THEN '--' WHEN BINARY "+v+"='APPROVE_USER_SELECT' THEN '选择审批人' WHEN BINARY "+v+" IN ('START','START_EVENT') THEN '开始' WHEN BINARY "+v+" IN ('END','END_EVENT') THEN '结束' WHEN "+v+" REGEXP '^[vV][0-9]+([.][0-9]+)?$' THEN SUBSTRING("+v+",2) WHEN "+v+" REGEXP '^[A-Z]/[1-9][0-9]*(-[1-9][0-9]*)?$' THEN "+v+" WHEN "+v+" REGEXP '^[A-Z_]+$' AND "+statusLabel(v)+"<>'未配置中文状态' THEN "+statusLabel(v)+" WHEN "+translated+" REGEXP '[A-Za-z]' AND "+translated+" NOT REGEXP '[一-龥]' AND NOT ("+translated+" REGEXP '^[A-Za-z0-9][A-Za-z0-9._/-]*$' AND "+translated+" REGEXP '[0-9]') THEN '' ELSE "+translated+" END FROM (SELECT TRIM("+value+") context_value) dcf)";
    }
    static String visible(String value) { return "(NULLIF(TRIM("+value+"),'') IS NOT NULL AND LOWER(TRIM("+value+")) NOT IN ('--','null','undefined') AND "+value+" NOT LIKE '未配置%' AND "+value+" NOT LIKE '%已配置')"; }
    public static String visibleField(String label,String value) { return "(SELECT CASE WHEN "+visible("dff.field_value")+" THEN CONCAT('"+label+" ',dff.field_value) ELSE '' END FROM (SELECT "+value+" field_value) dff)"; }
    public static String visibleTag(String label,String value) { return "(SELECT CASE WHEN "+visible("dtf.tag_value")+" THEN CONCAT('"+label+"：',dtf.tag_value) ELSE '' END FROM (SELECT "+contextValue(value)+" tag_value) dtf)"; }
}
