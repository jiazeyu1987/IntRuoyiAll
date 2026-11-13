package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field;
import java.util.*;
import static cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterSql.*;

/** Native engine display facts. All variable names and expressions are server-owned constants. */
public final class NativeApprovalListFilterSql {
    private NativeApprovalListFilterSql() { }
    static Projection taskFields(ApprovalTaskQueryContext context, boolean done, List<ApprovalTaskSummaryEnricher> enrichers, boolean domainPermissions) {
        String tenant="t.TENANT_ID_", process="t.PROC_INST_ID_";
        var facts=processFacts(process,tenant,done?"p.NAME_":"t.NAME_",done?"'DONE'":"'TODO'");
        facts.put("nodeName","n_current_node.value");
        if (!done) facts.put("identifier",identifier(businessCode(process,tenant),process,"t.ID_"));
        facts.put("nodeStatus",done?"'已办'":"'待办'");
        String role="(SELECT ar.name FROM system_role ar WHERE ar.code='dcc_registration_certificate_approver' AND ar.deleted=0 AND ar.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) LIMIT 1)";
        String claimable=variable(process,tenant,"requestType")+" IN ('UPLOAD_CERTIFICATE','DOWNLOAD_FILE') AND "
                +first(process,tenant,"registrationCertificateAccessRequestId","requestId")+" IS NOT NULL AND "+variable(process,tenant,"certificateId")+" IS NOT NULL";
        facts.put("reviewer","CASE WHEN "+claimable+" THEN CONCAT('审批角色：',"+role+") ELSE "+userLabel("CAST(NULLIF(t.ASSIGNEE_,'') AS UNSIGNED)","CAST(t.TENANT_ID_ AS UNSIGNED)")+" END");
        facts.put("applicant",userLabel("CAST(COALESCE(NULLIF(p.START_USER_ID_,''),"+variable(process,tenant,"PROCESS_START_USER_ID")+") AS UNSIGNED)","CAST(t.TENANT_ID_ AS UNSIGNED)"));
        facts.put("created",done?"t.START_TIME_":"t.CREATE_TIME_");
        facts.put("capabilities","'AUDIT NOTIFICATION'");
        if(done) {
            String status=taskVariable("TASK_STATUS"),reason=taskVariable("TASK_REASON");
            facts.put("completed","t.END_TIME_");
            facts.put("result","CASE WHEN "+status+" IN ('2','7') THEN 'APPROVE' WHEN "+status+" IN ('3','4','5') AND o.id IS NOT NULL THEN 'REJECT' WHEN "+status+"='3' THEN 'REJECT' END");
            facts.put("remark","CASE WHEN "+status+" IN ('3','4','5') AND (o.id IS NOT NULL OR "+status+"='3') THEN NULLIF(TRIM("+reason+"),'') END");
            facts.put("sourceLabel","CASE WHEN o.id IS NULL THEN '流程历史记录' ELSE '流程已办任务' END");
            facts.put("sourceStatus","CASE WHEN o.id IS NULL THEN "+status+" END");
            facts.put("sourceRemark","CASE WHEN o.id IS NULL THEN "+reason+" END");
            String outcome="CASE "+status+" WHEN '-2' THEN 'SKIP' WHEN '-1' THEN 'NOT_START' WHEN '1' THEN 'RUNNING' WHEN '2' THEN 'APPROVE' WHEN '3' THEN 'REJECT' WHEN '4' THEN 'CANCEL' WHEN '5' THEN 'RETURN' WHEN '7' THEN 'APPROVING' WHEN '0' THEN 'WAIT' ELSE 'UNKNOWN' END";
            facts.put("taskOutcome","CASE WHEN o.id IS NULL THEN "+outcome+" END");
            String statusName="CASE "+status+" WHEN '-2' THEN '跳过' WHEN '-1' THEN '未开始' WHEN '1' THEN '审批中' WHEN '2' THEN '审批通过' WHEN '3' THEN '审批不通过' WHEN '4' THEN '已取消' WHEN '5' THEN '已退回' WHEN '7' THEN '审批通过中' WHEN '0' THEN '待审批' END";
            facts.put("extraSummary",join(facts.get("extraSummary"),"CASE WHEN o.id IS NULL THEN CONCAT('历史归属证据缺失',COALESCE(CONCAT(' 任务状态：',"+statusName+"),'')) END"));
        } else {
            facts.put("sourceLabel","'流程待办任务'");
            facts.put("extraSummary",join(facts.get("extraSummary"),navigationBlocker(context.getLoginUserId(),domainPermissions)));

        }
        var projections=new LinkedHashMap<String,String>();
        enrichers.forEach(e -> e.enrichFilterFacts(facts, new ApprovalTaskSummaryEnricher.FilterContext(
                done ? "BPM_DONE_TASK" : "BPM_TODO_TASK", process, tenant, "d.KEY_", done ? "p.BUSINESS_KEY_" : process,
                facts.get("nodeName"), done ? "o.id IS NOT NULL" : "TRUE", projections)));
        return projection(facts,projections);
    }
    static Projection initiatedFields(ApprovalTaskQueryContext context, List<ApprovalTaskSummaryEnricher> enrichers) {
        var facts=processFacts("p.PROC_INST_ID_","p.TENANT_ID_","p.NAME_","'MY_INITIATED'");
        facts.put("sourceLabel","'流程实例'"); facts.put("nodeName","'我发起的'");facts.put("nodeStatus","'我发起的'");
        facts.put("applicant",userLabel("CAST(COALESCE(NULLIF(p.START_USER_ID_,''),"+variable("p.PROC_INST_ID_","p.TENANT_ID_","PROCESS_START_USER_ID")+") AS UNSIGNED)","CAST(p.TENANT_ID_ AS UNSIGNED)"));
        facts.put("created","p.START_TIME_");facts.put("initiated","p.START_TIME_");facts.put("completed","p.END_TIME_");facts.put("capabilities","'AUDIT NOTIFICATION'");
        var projections=new LinkedHashMap<String,String>();
        enrichers.forEach(e -> e.enrichFilterFacts(facts, new ApprovalTaskSummaryEnricher.FilterContext(
                "BPM_PROCESS_INSTANCE", "p.PROC_INST_ID_", "p.TENANT_ID_",
                "(SELECT nd.KEY_ FROM ACT_RE_PROCDEF nd WHERE nd.ID_=p.PROC_DEF_ID_)", "p.BUSINESS_KEY_", "'我发起的'", "TRUE", projections)));
        return projection(facts,projections);
    }
    static Projection copiedFields(ApprovalTaskQueryContext context, List<ApprovalTaskSummaryEnricher> enrichers) {
        var facts=new HashMap<String,String>(); facts.put("moduleLabel","'流程审批'");facts.put("sourceLabel","'流程抄送实例'");
        facts.put("title",titleTranslations("c.process_instance_name"));
        facts.put("identifier",identifier("NULL","c.process_instance_id","CAST(c.id AS CHAR)"));
        facts.put("businessStatus","'CC'");facts.put("nodeStatus","'抄送我的'");facts.put("nodeName",mappedNode("c.activity_name","c.activity_id"));
        facts.put("applicant",userLabel("c.start_user_id","c.tenant_id"));facts.put("reviewer","'--'");facts.put("created","c.create_time");facts.put("capabilities","'AUDIT NOTIFICATION'");
        var projections=new LinkedHashMap<String,String>();
        enrichers.forEach(e -> e.enrichFilterFacts(facts, new ApprovalTaskSummaryEnricher.FilterContext(
                "BPM_PROCESS_COPY", "c.process_instance_id", "CAST(c.tenant_id AS CHAR)",
                "(SELECT nd.KEY_ FROM ACT_HI_PROCINST np JOIN ACT_RE_PROCDEF nd ON nd.ID_=np.PROC_DEF_ID_ WHERE np.PROC_INST_ID_=c.process_instance_id AND np.TENANT_ID_=CAST(c.tenant_id AS CHAR))",
                "c.process_instance_id", facts.get("nodeName"), "TRUE", projections)));
        return projection(facts,projections);
    }
    record Projection(Map<String,Field> fields, String joins) { }
    private static Projection projection(Map<String,String> facts, Map<String,String> projections) {
        String joins=projections.entrySet().stream().map(entry->" LEFT JOIN LATERAL (SELECT "+entry.getValue()+" value) "+entry.getKey()+" ON TRUE ").collect(java.util.stream.Collectors.joining());
        return new Projection(fields(facts,false),joins);
    }
    private static String taskNodeExpression() {
        String process="t.PROC_INST_ID_",tenant="t.TENANT_ID_";
        String type=variable(process,tenant,"businessType");
        String node="CASE "+type+" WHEN 'BATCH_RECORD_VERSION_APPROVAL' THEN '批记录升版审核'"
                +" WHEN 'MES_ROUTE_VERSION_PUBLISH' THEN '工艺路线发布审核'"
                +" WHEN 'MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART' THEN '活跃订单升级重启审核'"
                +" WHEN 'EDHR_BATCH_EXECUTION_VOID' THEN '电子批记录批次作废审核'"
                +" ELSE CASE WHEN "+first(process,tenant,"edhrExecutionCode","edhrExecutionId")+" IS NOT NULL THEN '电子批记录审核'"
                +" WHEN "+certificate(process,tenant)+" THEN '注册证访问审批' WHEN t.TASK_DEF_KEY_='approveNode' THEN '审核节点' ELSE t.NAME_ END END";
        return mappedNode(node,"t.TASK_DEF_KEY_");
    }
    private static String navigationBlocker(Long user, boolean domainPermissions) {
        if (user == null || user <= 0) throw new IllegalArgumentException("APPROVAL_QUERY_ACTOR_REQUIRED");
        String requests=" FROM bpm_business_approval_request nr WHERE nr.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) AND CAST(nr.process_instance_id AS BINARY)=CAST(t.PROC_INST_ID_ AS BINARY) AND nr.deleted=0";
        String claim="EXISTS(SELECT 1"+requests+" AND (nr.object_type='EDHR_BATCH_EXECUTION' OR nr.data_domain='MES' AND nr.action_code='SUBMIT_REVIEW'))";
        String eligible="(SELECT COUNT(*) FROM mes_pro_edhr_work_task nw WHERE nw.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) AND nw.execution_id=(SELECT CAST(nr.object_id AS UNSIGNED)"+requests+") AND nw.bpm_task_id=t.ID_ AND nw.deleted=0 AND nw.task_type IN ('REVIEW','APPROVE') AND nw.status IN ('TODO','OVERDUE') AND (nw.assignee_user_id="+user+" OR CONCAT(',',COALESCE(nw.candidate_user_snapshot,''),',') REGEXP ',[[:space:]]*[+]?[0]*"+user+"[[:space:]]*,'))";
        return "CASE WHEN (d.KEY_='mes-edhr-approval-v1' OR "+claim+") AND NOT EXISTS(SELECT 1 FROM bpm_form_action_instance nf WHERE nf.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) AND nf.bpm_process_instance_id=t.PROC_INST_ID_ AND nf.deleted=0) THEN CASE WHEN NOT "+claim+" THEN '缺少正式电子批记录审批关联，暂不能办理' WHEN "+eligible+"=0 THEN '当前用户没有可进入的领域审核工作任务' WHEN "+eligible+">1 THEN '存在多个可办理签字格，缺少按执行记录选择工作任务的入口'"+(domainPermissions?"":" ELSE '当前用户缺少领域审核权限或不是该流程任务的审批人'")+" END END";
    }
    private static HashMap<String,String> processFacts(String process,String tenant,String originalTitle,String businessStatus) {
        var f=new HashMap<String,String>();f.put("moduleLabel","'流程审批'");f.put("businessStatus",businessStatus);f.put("reviewer","'--'");
        String type=variable(process,tenant,"businessType");
        String title="CASE "+type
                +" WHEN 'BATCH_RECORD_VERSION_APPROVAL' THEN "+title("批记录升版",variable(process,tenant,"batchRecordName"),variable(process,tenant,"versionNo"))
                +" WHEN 'EDHR_BATCH_EXECUTION_VOID' THEN "+title("电子批记录批次作废",first(process,tenant,"batchExecutionCode","batchExecutionId"),labelPart("批次",variable(process,tenant,"batchCode")),labelPart("工单",variable(process,tenant,"workOrderCode")))
                +" WHEN 'MES_ROUTE_VERSION_PUBLISH' THEN "+title("工艺路线发布",first(process,tenant,"routeName","routeCode","routeId","objectId"),first(process,tenant,"routeVersionNo","objectVersion"))
                +" WHEN 'MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART' THEN "+title("活跃订单升级重启",first(process,tenant,"requestCode","requestId"),labelPart("工单",variable(process,tenant,"workOrderCode")))
                +" ELSE CASE WHEN "+first(process,tenant,"edhrExecutionCode","edhrExecutionId")+" IS NOT NULL THEN "+title("电子批记录审核",first(process,tenant,"edhrExecutionCode","edhrExecutionId"),labelPart("工单",variable(process,tenant,"workOrderCode")),labelPart("批次",variable(process,tenant,"batchCode")),labelPart("工序",variable(process,tenant,"processName")))
                +" WHEN "+certificate(process,tenant)+" THEN "+certificateTitle(process,tenant)
                +" ELSE "+templateTitle(process,tenant,originalTitle)+" END END";
        f.put("title",titleTranslations(title));
        String code=businessCode(process,tenant);
        f.put("identifier",identifier(code,"p.BUSINESS_KEY_",process));
        f.put("extraSummary",tags(process,tenant));
        return f;
    }
    private static String businessCode(String process,String tenant) {
        return "CASE WHEN "+variable(process,tenant,"requestType")+"='UPLOAD_CERTIFICATE' THEN NULL ELSE "+first(process,tenant,"businessCode","edhrExecutionCode","batchExecutionCode","requestKey","routeCode","batchRecordCode","batchRecordVersionId","batchCode","workOrderCode","objectId","businessKey")+" END";
    }
    private static String title(String prefix,String...parts) {
        StringBuilder columns=new StringBuilder(), result=new StringBuilder("CONCAT(").append(literal(prefix));
        for(int i=0;i<parts.length;i++) {
            if(i>0) columns.append(',');columns.append(parts[i]).append(" title_part_").append(i);
            String part="nbt.title_part_"+i;
            result.append(",CASE WHEN NULLIF(TRIM(").append(part).append("),'') IS NULL THEN '' ELSE CONCAT(' ',TRIM(").append(part).append(")) END");
        }
        return "(SELECT "+result.append(')')+" FROM (SELECT "+columns+") nbt)";
    }
    private static String labelPart(String label,String value) { return "CONCAT("+literal(label+" ")+","+value+")"; }
    private static String identifier(String code,String key,String id) {
        return "(SELECT CASE WHEN NULLIF(TRIM(nif.business_code),'') IS NOT NULL THEN CONCAT('业务编号：',TRIM(nif.business_code)) WHEN NULLIF(TRIM(nif.business_key),'') IS NOT NULL THEN CASE WHEN nif.business_key LIKE 'FORM_ACTION:%' THEN '表单动作实例' WHEN nif.business_key REGEXP '[A-Za-z]' THEN '业务键已配置' ELSE nif.business_key END WHEN nif.task_id REGEXP '[A-Za-z]' THEN '任务编号已配置' ELSE COALESCE(nif.task_id,'--') END FROM (SELECT "+id+" task_id,"+key+" business_key,"+code+" business_code) nif)";
    }
    private static String mappedText(String value, String unmapped) {
        String[] codes={"MY_INITIATED","TODO","DONE","CC","APPROVE","APPROVED","REJECT","REJECTED","CANCELED","CANCELLED","RUNNING","PENDING","PENDING_REVIEW","PENDING_APPROVAL","WRITING","COMPLETED","WRITE_FAILED","PENDING_SUPERVISOR_REVIEW","PENDING_MATRIX_REVIEW","PENDING_MATRIX_APPROVAL","PENDING_DOC_CONTROL_REVIEW","PENDING_APPLICANT_REWORK","PENDING_APPLICANT_TRAINING_RECORD","PENDING_DELAYED_EFFECTIVE_CONFIRM","CONTROLLED_PENDING_EFFECTIVE","WORKING","DRAFT","NEW","SUBMITTED","APPROVING","ACTIVE","SUPERSEDED","OBSOLETE","WITHDRAWN","DELETED","APPROVE_USER_SELECT","START","START_EVENT","END","END_EVENT"};
        String[] labels={"我发起的","待办","已办","抄送我的","通过","已通过","驳回","已驳回","已取消","已取消","进行中","待处理","待审核","待批准","写入中","已完成","写入失败","待直属主管审核","待会签","待批准","待文控审核","待申请人返工","待文控上传培训记录","待延迟生效确认","受控（待生效）","工作小版本","草稿","新建","已提交","审批中","现行","已替代","已作废","已撤回","已删除","选择审批人","开始","开始","结束","结束"};
        StringBuilder sql=new StringBuilder("CASE TRIM(").append(value).append(")");
        for (int i=0;i<codes.length;i++) sql.append(" WHEN '").append(codes[i]).append("' THEN '").append(labels[i]).append("'");
        return sql.append(" ELSE ").append(unmapped).append(" END").toString();
    }
    private static String mappedNode(String node,String code) {
        String n="nnf.node_name",c="nnf.node_code";
        String codeLabel=mappedText(c,"CASE WHEN NULLIF(TRIM("+c+"),'') IS NULL THEN '--' WHEN "+c+" REGEXP '[A-Za-z]' THEN '未配置中文节点' ELSE TRIM("+c+") END");
        return "(SELECT CASE WHEN NULLIF(TRIM("+n+"),'') IS NOT NULL AND "+n+" NOT REGEXP '[A-Za-z]' THEN TRIM("+n+") ELSE "+mappedText(n,codeLabel)+" END FROM (SELECT "+node+" node_name,"+code+" node_code) nnf)";
    }
    public static String contextValue(String value) {
        String v="ncf.context_value",translated=titleTranslations(v);
        return "(SELECT CASE WHEN "+v+" REGEXP '^[vV][0-9]+([.][0-9]+)?$' THEN SUBSTRING("+v+",2) WHEN "+v+" REGEXP '^[A-Z]/[1-9][0-9]*(-[1-9][0-9]*)?$' THEN "+v+" ELSE "+mappedText(v,"CASE WHEN "+translated+" REGEXP '[A-Za-z]' AND "+translated+" NOT REGEXP '[一-龥]' AND NOT ("+translated+" REGEXP '^[A-Za-z0-9][A-Za-z0-9._/-]*$' AND "+translated+" REGEXP '[0-9]') THEN '' ELSE "+translated+" END")+" END FROM (SELECT TRIM("+value+") context_value) ncf)";
    }
    /** Scans the original varchar(255) engine title once; each recursive row consumes a complete token. */
    private static String templateTitle(String process,String tenant,String title) { return "n_title.resolved_title"; }
    private static String templateTitleExpression(String process,String tenant,String title) {
        String raw="TRIM("+title+")",pattern="'[$][{][^}]+[}]'";
        String name="SUBSTRING("+raw+",nt.next_start+2,nt.next_end-nt.next_start-2)";
        String replacement="JSON_UNQUOTE(JSON_EXTRACT(nv.process_variables,CONCAT('$.',JSON_QUOTE("+name+"))))";
        String cte="WITH RECURSIVE native_template_tokens(seq,prev_end,next_start,next_end) AS (SELECT 0,0,REGEXP_INSTR("+raw+","+pattern+"),REGEXP_INSTR("+raw+","+pattern+",1,1,1)-1 UNION ALL SELECT nt.seq+1,nt.next_end,CASE WHEN nt.next_end>=CHAR_LENGTH("+raw+") THEN 0 ELSE REGEXP_INSTR("+raw+","+pattern+",nt.next_end+1) END,CASE WHEN nt.next_end>=CHAR_LENGTH("+raw+") THEN 0 ELSE REGEXP_INSTR("+raw+","+pattern+",nt.next_end+1,1,1)-1 END FROM native_template_tokens nt WHERE nt.next_start>0) ";
        String piece="CASE WHEN nt.next_start>0 THEN CONCAT(SUBSTRING("+raw+",nt.prev_end+1,nt.next_start-nt.prev_end-1),COALESCE(TRIM("+replacement+"),'')) ELSE SUBSTRING("+raw+",nt.prev_end+1) END";
        String array="("+cte+"SELECT JSON_ARRAYAGG("+piece+") OVER (ORDER BY nt.seq ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM native_template_tokens nt LIMIT 1)";
        String decoded="(SELECT JSON_UNQUOTE(CONCAT(CHAR(34),REPLACE(SUBSTRING(CAST(nja.parts AS CHAR),3,CHAR_LENGTH(CAST(nja.parts AS CHAR))-4),CONCAT(CHAR(34),', ',CHAR(34)),''),CHAR(34))) FROM (SELECT "+array+" parts) nja)";
        String resolved="TRIM(REGEXP_REPLACE("+decoded+",'[[:space:]]+',' '))";
        String generic=title("BPM 审批",variable(process,tenant,"businessType"),first(process,tenant,"businessKey","objectId"));
        return "(SELECT CASE WHEN NULLIF(ntt.source_title,'') IS NULL THEN 'BPM 审批' WHEN LOCATE('${',ntt.source_title)=0 THEN ntt.source_title WHEN NULLIF(ntt.resolved_title,'') IS NOT NULL AND LOCATE('${',ntt.resolved_title)=0 THEN ntt.resolved_title ELSE "+generic+" END FROM (SELECT "+raw+" source_title,"+resolved+" resolved_title) ntt)";
    }
    private static String titleTranslations(String sql) { return "REPLACE(REPLACE(REPLACE("+sql+",'DCC Controlled File Approval','文控受控文件审批'),'Expense Dept Leader Approval','费用部门负责人审批'),'eDHR Approval V1','电子批记录审批第一版')"; }
    private static String certificate(String p,String t) { return "("+first(p,t,"registrationCertificateAccessRequestId","certificateId")+" IS NOT NULL OR "+variable(p,t,"requestType")+" IN ('UPLOAD_CERTIFICATE','VIEW_OLD_CERTIFICATE','DOWNLOAD_FILE'))"; }
    private static String certificateTitle(String p,String t) {
        String request="nct.request_type",operation="nct.request_operation";
        String label="CASE "+request+" WHEN 'UPLOAD_CERTIFICATE' THEN CASE "+operation+" WHEN 'RENEWAL_CERTIFICATE' THEN '注册证延续审批' WHEN 'CHANGE_CERTIFICATE' THEN '注册证变更审批' WHEN 'UPLOAD_CERTIFICATE' THEN '注册证上传审批' ELSE '注册证审批' END WHEN 'VIEW_OLD_CERTIFICATE' THEN '旧注册证查看审批' WHEN 'DOWNLOAD_FILE' THEN '注册证下载审批' ELSE '注册证访问审批' END";
        return "(SELECT CONCAT("+label+",COALESCE(CONCAT(' ',nct.certificate_no),''),CASE WHEN "+request+"='UPLOAD_CERTIFICATE' THEN '' ELSE COALESCE(CONCAT(' ',nct.request_key),'') END) FROM (SELECT "+variable(p,t,"requestType")+" request_type,"+first(p,t,"requestOperation","operation")+" request_operation,"+variable(p,t,"certificateNo")+" certificate_no,"+first(p,t,"requestKey","registrationCertificateAccessRequestId","requestId")+" request_key) nct)";
    }
    private static String first(String p,String t,String...keys) { return "COALESCE("+Arrays.stream(keys).map(k->variable(p,t,k)).collect(java.util.stream.Collectors.joining(","))+",NULL)"; }
    private static final List<String> VARIABLE_KEYS=List.of("PROCESS_START_USER_ID","batchCode","batchExecutionCode","batchExecutionId","batchRecordCode","batchRecordName","batchRecordVersionId","businessCode","businessKey","businessType","certificateId","certificateNo","classification","edhrExecutionCode","edhrExecutionId","objectId","objectVersion","operation","ownerCompanyId","ownerCompanyName","processName","productName","reasonCategory","reasonText","registrationCertificateAccessRequestId","requestCode","requestId","requestKey","requestOperation","requestType","routeCode","routeId","routeName","routeVersionNo","sourceActiveOrderId","sourceVersionNo","targetVersionsSummary","versionNo","workOrderCode","workstationName");
    private static String variable(String process,String tenant,String key) {
        if (!VARIABLE_KEYS.contains(key)) throw new IllegalArgumentException("APPROVAL_FILTER_VARIABLE_UNMAPPED: "+key);
        return "nv."+key;
    }
    /** Exactly one root-variable projection per process, shared by count and window predicates. */
    static String variablesJoin(String process, boolean runtime) {
        if (!Set.of("t.PROC_INST_ID_","p.PROC_INST_ID_").contains(process)) throw new IllegalArgumentException("APPROVAL_FILTER_PROCESS_ALIAS_INVALID");
        String tenant=process.startsWith("t.")?"t.TENANT_ID_":"p.TENANT_ID_";
        String columns=VARIABLE_KEYS.stream().map(key->",MAX(CASE nvr.NAME_ WHEN "+literal(key)+" THEN nvr.variable_value END) "+key).collect(java.util.stream.Collectors.joining());
        String source=runtime?"ACT_RU_VARIABLE":"ACT_HI_VARINST";
        String order=runtime?"v.ID_ DESC":"v.LAST_UPDATED_TIME_ DESC,v.ID_ DESC";
        return " LEFT JOIN LATERAL (SELECT JSON_OBJECTAGG(nvr.NAME_,nvr.variable_value) process_variables"+columns+" FROM (SELECT v.NAME_,NULLIF(TRIM(COALESCE(v.TEXT_,CAST(v.LONG_ AS CHAR),CAST(v.DOUBLE_ AS CHAR))),'') variable_value,ROW_NUMBER() OVER (PARTITION BY v.NAME_ ORDER BY "+order+") variable_rank FROM "+source+" v WHERE v.EXECUTION_ID_="+process+" AND v.TASK_ID_ IS NULL AND EXISTS(SELECT 1 FROM ACT_HI_PROCINST nvp WHERE nvp.PROC_INST_ID_=v.PROC_INST_ID_ AND nvp.TENANT_ID_="+tenant+")) nvr WHERE nvr.variable_rank=1) nv ON TRUE "
                +" LEFT JOIN LATERAL (SELECT "+templateTitleExpression(process,tenant,process.startsWith("t.")?(runtime?"t.NAME_":"p.NAME_"):"p.NAME_")+" resolved_title) n_title ON TRUE "
                +" LEFT JOIN LATERAL (SELECT "+tagsExpression(process,tenant)+" display_tags) n_tags ON TRUE "
                +(process.startsWith("t.")?" LEFT JOIN LATERAL (SELECT "+taskNodeExpression()+" value) n_current_node ON TRUE ":"");
    }
    private static String taskVariable(String key) { return "(SELECT COALESCE(v.TEXT_,CAST(v.LONG_ AS CHAR),CAST(v.DOUBLE_ AS CHAR)) FROM ACT_HI_VARINST v WHERE v.TASK_ID_=t.ID_ AND v.PROC_INST_ID_=t.PROC_INST_ID_ AND EXISTS(SELECT 1 FROM ACT_HI_PROCINST nvtenant WHERE nvtenant.PROC_INST_ID_=v.PROC_INST_ID_ AND nvtenant.TENANT_ID_=t.TENANT_ID_) AND v.NAME_="+literal(key)+" ORDER BY v.LAST_UPDATED_TIME_ DESC,v.ID_ DESC LIMIT 1)"; }
    private static String tag(String label,String value) { return "JSON_OBJECT('label',"+literal(label)+",'value',"+value+")"; }
    private static String jsonTags(String... tags) { return "JSON_ARRAY("+String.join(",",tags)+")"; }
    private static String visibleTags(String array) {
        String display="ntv.display_value";
        String visible="NULLIF(TRIM("+display+"),'') IS NOT NULL AND LOWER(TRIM("+display+")) NOT IN ('--','null','undefined') AND "+display+" NOT LIKE '未配置%' AND "+display+" NOT LIKE '%已配置'";
        // The provider defines at most five tags in every business branch; enumerate those positions directly.
        String positions="(SELECT 0 seq UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4)";
        String value="JSON_UNQUOTE(JSON_EXTRACT(nd.tags,CONCAT('$[',ni.seq,'].value')))";
        String label="JSON_UNQUOTE(JSON_EXTRACT(nd.tags,CONCAT('$[',ni.seq,'].label')))";
        String aggregate="(SELECT JSON_ARRAYAGG(CONCAT(ntv.tag_label,'：',"+display+")) OVER (ORDER BY ntv.seq ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM (SELECT ni.seq,"+label+" tag_label,"+contextValue(value)+" display_value FROM (SELECT "+array+" tags) nd CROSS JOIN "+positions+" ni WHERE ni.seq<JSON_LENGTH(nd.tags)) ntv WHERE "+visible+" LIMIT 1)";
        return "COALESCE((SELECT JSON_UNQUOTE(CONCAT(CHAR(34),REPLACE(SUBSTRING(CAST(nta.tags AS CHAR),3,CHAR_LENGTH(CAST(nta.tags AS CHAR))-4),CONCAT(CHAR(34),', ',CHAR(34)),' '),CHAR(34))) FROM (SELECT "+aggregate+" tags) nta),'')";
    }
    private static String tags(String p,String t) { return "n_tags.display_tags"; }
    private static String tagsExpression(String p,String t) {
        String type=variable(p,t,"businessType");
        String raw="CASE "+type
            +" WHEN 'BATCH_RECORD_VERSION_APPROVAL' THEN "+jsonTags(tag("批记录",variable(p,t,"batchRecordName")),tag("版本",variable(p,t,"versionNo")),tag("源版本",variable(p,t,"sourceVersionNo")),tag("工艺路线",first(p,t,"routeName","routeCode","routeId")))
            +" WHEN 'MES_ROUTE_VERSION_PUBLISH' THEN "+jsonTags(tag("路线编号",variable(p,t,"routeCode")),tag("路线名称",variable(p,t,"routeName")),tag("版本",first(p,t,"routeVersionNo","objectVersion")))
            +" WHEN 'MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART' THEN "+jsonTags(tag("申请单",variable(p,t,"requestCode")),tag("来源活跃订单",variable(p,t,"sourceActiveOrderId")),tag("工单",variable(p,t,"workOrderCode")),tag("目标版本",variable(p,t,"targetVersionsSummary")))
            +" WHEN 'EDHR_BATCH_EXECUTION_VOID' THEN "+jsonTags(tag("工单",variable(p,t,"workOrderCode")),tag("批次",variable(p,t,"batchCode")),tag("原因",first(p,t,"reasonText","reasonCategory")))
            +" ELSE CASE WHEN "+first(p,t,"edhrExecutionCode","edhrExecutionId")+" IS NOT NULL THEN "+jsonTags(tag("工单",variable(p,t,"workOrderCode")),tag("批次",variable(p,t,"batchCode")),tag("工序",variable(p,t,"processName")),tag("工作站",variable(p,t,"workstationName")))
            +" WHEN "+variable(p,t,"requestType")+"='UPLOAD_CERTIFICATE' THEN "+jsonTags(tag("注册证编号",variable(p,t,"certificateNo")),tag("分类",variable(p,t,"classification")),tag("产品",variable(p,t,"productName")),tag("所属公司名称",variable(p,t,"ownerCompanyName")))
            +" WHEN "+certificate(p,t)+" THEN "+jsonTags(tag("申请类型","CASE "+variable(p,t,"requestType")+" WHEN 'VIEW_OLD_CERTIFICATE' THEN '旧注册证查看' WHEN 'DOWNLOAD_FILE' THEN '注册证下载' ELSE '注册证访问' END"),tag("注册证编号",variable(p,t,"certificateNo")),tag("申请编号",first(p,t,"registrationCertificateAccessRequestId","requestId")),tag("注册证",variable(p,t,"certificateId")),tag("所属公司",variable(p,t,"ownerCompanyId")))
            +" ELSE JSON_ARRAY() END END";
        return visibleTags(raw);
    }
}
