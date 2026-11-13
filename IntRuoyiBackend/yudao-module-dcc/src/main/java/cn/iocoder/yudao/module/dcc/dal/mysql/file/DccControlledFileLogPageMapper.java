package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogPageReqVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.dateTime;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text;

/** Filters/counts the complete event union in SQL; only selected source IDs are hydrated by the service. */
@Mapper
public interface DccControlledFileLogPageMapper {
    @Data
    class LogKey {
        private String logType;
        private Long sourceRecordId;
        public String identity() { return logType + ":" + sourceRecordId; }
    }

    @SelectProvider(type = Sql.class, method = "page")
    IPage<LogKey> selectLogPage(IPage<LogKey> page, @Param("ew") QueryWrapper<LogKey> wrapper,
                              @Param("controlledFileId") Long controlledFileId, @Param("tenantId") Long tenantId);

    default PageResult<LogKey> selectLogPage(DccControlledFileLogPageReqVO request, Long controlledFileId) {
        QueryWrapper<LogKey> wrapper = new QueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(request.getLogType()), "q.log_type", StrUtil.trim(request.getLogType()))
                .eq(StrUtil.isNotBlank(request.getActionType()), "q.action_type", StrUtil.trim(request.getActionType()))
                .eq(StrUtil.isNotBlank(request.getResult()), "q.result", StrUtil.trim(request.getResult()))
                .eq(request.getMasterId() != null, "q.master_id", request.getMasterId())
                .eq(StrUtil.isNotBlank(request.getVersionNo()), "q.version_no", StrUtil.trim(request.getVersionNo()))
                .eq(request.getProjectCodeId() != null, "q.project_code_id", request.getProjectCodeId())
                .eq(request.getAssignmentId() != null, "q.assignment_id", request.getAssignmentId())
                .eq(request.getOperatorUserId() != null, "q.operator_user_id", request.getOperatorUserId())
                .eq(StrUtil.isNotBlank(request.getFieldName()), "q.field_name", StrUtil.trim(request.getFieldName()));
        if (controlledFileId != null) {
            wrapper.and(w -> w.eq("q.controlled_file_id", controlledFileId).or().apply(
                    "q.log_type='PROJECT_CODE_ASSIGNMENT' AND EXISTS (SELECT 1 FROM dcc_project_code_assignment_file cf"
                            + " WHERE cf.assignment_id=q.source_record_id AND cf.controlled_file_id={0} AND cf.deleted=0 AND cf.tenant_id={1})",
                    controlledFileId, TenantContextHolder.getRequiredTenantId()));
        }
        if (request.getOccurredAt() != null) {
            wrapper.ge(request.getOccurredAt()[0] != null, "q.occurred_at", request.getOccurredAt()[0])
                    .le(request.getOccurredAt()[1] != null, "q.occurred_at", request.getOccurredAt()[1]);
        }
        if (StrUtil.isNotBlank(request.getKeyword())) {
            String keyword = request.getKeyword().trim().toLowerCase(java.util.Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            wrapper.apply("LOWER(q.keyword_text) LIKE {0} ESCAPE '!'", "%" + keyword + "%");
        }
        ListFilterQueryUtils.apply(request, wrapper, Map.ofEntries(
                Map.entry("occurredAt", dateTime("q.occurred_at")),
                Map.entry("logType", text("q.log_type_label")),
                Map.entry("actionLabel", text("COALESCE(NULLIF(q.action_label,''),'-')")),
                Map.entry("fileNumber", text("COALESCE(NULLIF(q.file_number,''),'-')")),
                Map.entry("fileName", text("COALESCE(NULLIF(q.file_name,''),'-')")),
                Map.entry("operatorName", text("COALESCE(NULLIF(q.operator_name,''),CONCAT('用户#',q.operator_user_id),'-')")),
                Map.entry("relatedObject", text("COALESCE(NULLIF(q.related_object,''),'-')")),
                Map.entry("summary", text("COALESCE(NULLIF(q.summary,''),'-')")),
                Map.entry("resultLabel", text("COALESCE(NULLIF(q.result_label,''),'-')"))));
        wrapper.orderByDesc("q.occurred_at", "q.source_record_id", "q.log_type");
        var page = MyBatisUtils.<LogKey>buildPage(request);
        page.setOptimizeCountSql(false); // UNION and correlated display fields require the same count query.
        selectLogPage(page, wrapper, controlledFileId, TenantContextHolder.getRequiredTenantId());
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    /** Every expression here is server owned. Request values appear only in bound parameters. */
    class Sql {
        private static final String[] COLUMNS = {"log_type", "source_record_id", "occurred_at", "action_type", "result",
                "controlled_file_id", "master_id", "project_code_id", "assignment_id", "operator_user_id", "field_name",
                "action_label", "result_label", "file_number", "file_name", "version_no", "operator_name",
                "related_object", "summary", "old_value_text", "new_value_text", "reason", "detail_json", "keyword_extra"};

        public static String page() {
            List<String> sources = new ArrayList<>();
            sources.add(access());
            sources.add(lifecycle("FILE_SUBMISSION", "f.submitted_time", "提交", "'已提交'", "f.submitter_id",
                    "COALESCE(NULLIF(TRIM(f.change_type),''),'NEW') NOT IN ('REVISION','OBSOLETE')", "NULL"));
            sources.add(lifecycle("FILE_REVISION", "f.submitted_time", "升版", fileStatus("f.status"), "f.submitter_id",
                    "f.change_type='REVISION'", "NULL"));
            sources.add(lifecycle("FILE_OBSOLETE", "COALESCE(f.obsoleted_time,f.submitted_time)", "作废", fileStatus("f.status"),
                    "COALESCE(f.obsoleted_by,f.submitter_id)", "f.change_type='OBSOLETE'", "f.obsolete_reason"));
            sources.add(lifecycle("FILE_APPROVAL", "f.approved_time", "审批", "'通过'", "NULL", "1=1", "NULL"));
            sources.add(lifecycle("FILE_RELEASE", "f.published_time", "放行", "'已放行'", "NULL", "1=1", "NULL"));
            sources.add(checkout());
            sources.add(distribution());
            sources.add(assignment());
            sources.add(change());
            sources.add(training());
            String label = label("s.log_type", "CONTROLLED_FILE_AUDIT", "访问", "FILE_SUBMISSION", "提交", "FILE_APPROVAL", "审批",
                    "FILE_RELEASE", "放行", "FILE_DISTRIBUTION", "分发", "FILE_REVISION", "升版", "FILE_CHECKOUT", "检出/检入",
                    "FILE_OBSOLETE", "作废", "PROJECT_CODE_ASSIGNMENT", "修正任务", "PROJECT_CODE_CHANGE", "修正追溯", "TRAINING_EXECUTION", "培训");
            String keyword = joined(" ", "s.log_type", label, "s.action_label", "s.result_label", "s.file_number", "s.file_name",
                    "s.version_no", "s.operator_name", "s.related_object", "s.summary", "s.old_value_text", "s.new_value_text", "s.reason", "s.detail_json", "s.keyword_extra");
            // No XML tags are needed: LanguageDriver parses the provider text and its bound placeholders directly.
            return "SELECT q.log_type,q.source_record_id FROM (SELECT s.*," + label + " AS log_type_label,"
                    + keyword + " AS keyword_text FROM (" + String.join(" UNION ALL ", sources)
                    + ") s) q ${ew.customSqlSegment}";
        }

        private static Map<String, String> row(String type, String id, String time, String action, String result) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("log_type", quote(type)); row.put("source_record_id", id); row.put("occurred_at", time);
            row.put("action_type", quote(type)); row.put("action_label", action); row.put("result_label", result);
            return row;
        }
        private static String select(Map<String, String> row, String from) {
            return "SELECT " + java.util.Arrays.stream(COLUMNS).map(key -> row.getOrDefault(key, "NULL") + " AS " + key)
                    .collect(java.util.stream.Collectors.joining(",")) + " FROM " + from;
        }
        private static void file(Map<String, String> row) {
            row.put("master_id", "f.master_id"); row.put("file_number", "f.file_number");
            row.put("file_name", first("f.file_name", "f.title")); row.put("version_no", "f.version_no");
            row.put("project_code_id", "f.dcc_project_code_id");
        }
        private static void user(Map<String, String> row, String id) {
            row.put("operator_user_id", id); row.put("operator_name", userName(id));
        }
        private static String userName(String id) {
            return "COALESCE((SELECT NULLIF(TRIM(lu.nickname),'') FROM system_users lu WHERE lu.id=" + id
                    + " AND lu.deleted=0 AND lu.tenant_id=#{tenantId}),CAST(" + id + " AS CHAR))";
        }
        private static String fileJoin(String id) {
            return " LEFT JOIN dcc_controlled_file f ON f.id=" + id + " AND f.deleted=0 AND f.tenant_id=#{tenantId}";
        }
        private static String scoped(String alias) { return alias + ".deleted=0 AND " + alias + ".tenant_id=#{tenantId}"; }
        private static String lifecycle(String type, String time, String action, String result, String operator, String condition, String reason) {
            Map<String, String> row = row(type, "f.id", time, quote(action), result); file(row); user(row, operator);
            row.put("controlled_file_id", "f.id"); row.put("result", "f.status"); row.put("reason", reason);
            row.put("related_object", type.equals("FILE_APPROVAL") ? "CONCAT('流程 ',COALESCE(NULLIF(TRIM(f.process_instance_id),''),'-'))"
                    : "CONCAT('版本 ',COALESCE(NULLIF(TRIM(f.version_no),''),'-'))");
            row.put("summary", joined(" / ", row.get("file_number"), row.get("file_name"), "f.version_no", quote(action), result));
            row.put("detail_json", json("changeType", "f.change_type", "status", "f.status", "processInstanceId", "f.process_instance_id",
                    "dccProjectCodeId", "f.dcc_project_code_id", "fileTypeTaxonomyId", "f.file_type_taxonomy_id"));
            return select(row, "dcc_controlled_file f WHERE " + scoped("f") + " AND " + condition + " AND " + time + " IS NOT NULL");
        }
        private static String access() {
            String user = "COALESCE(a.user_id,e.user_id)", action = first("a.action_type", "e.access_type"), result = first("a.result", "e.result");
            String actionLabel = label(action,"UPLOAD","上传","PREVIEW","预览","OFFICE_READ","OnlyOffice 阅读","DOWNLOAD","下载","DIRECT_LINK","直链拦截","TOKEN_VALIDATE","令牌校验","TEMP_CLEANUP","临时清理");
            String resultLabel = label(result,"SUCCESS","成功","ALLOWED","允许","DENIED","拒绝","FAILED","失败");
            Map<String, String> row = row("CONTROLLED_FILE_AUDIT", "a.id", "CASE WHEN e.id IS NOT NULL THEN e.occurred_at ELSE a.create_time END", actionLabel, resultLabel);
            file(row); user(row,user); row.put("project_code_id","NULL"); row.put("action_type",action);row.put("result",result);
            row.put("controlled_file_id","COALESCE(a.controlled_file_id,e.controlled_file_id)");
            row.put("version_no",first("f.version_no","a.file_version_no","e.file_version_no")); row.put("related_object","'受控文件访问'");
            row.put("reason",first("a.reason","e.failure_reason")); row.put("summary",joined(" / ",row.get("file_number"),row.get("file_name"),actionLabel,resultLabel));
            row.put("detail_json",json("accessEventCode",first("a.access_event_code","e.access_event_code"),"requestId",first("a.request_id","e.request_id"),"sourceIp",first("a.source_ip","e.source_ip"),"userAgent",first("a.user_agent","e.user_agent"),"purpose",first("a.purpose","e.purpose")));
            return select(row,"dcc_controlled_file_access_log a LEFT JOIN dcc_controlled_file_access_event e ON e.id=a.access_event_id AND "+scoped("e")+fileJoin(row.get("controlled_file_id"))+" WHERE "+scoped("a"));
        }
        private static String checkout() {
            String action="CASE c.status WHEN 'CHECKED_IN' THEN '检入' WHEN 'CANCELLED' THEN '撤销检出' ELSE '检出' END";
            String result="CASE c.status WHEN 'CHECKED_IN' THEN '已生成新小版本' WHEN 'CANCELLED' THEN '已撤销' ELSE '进行中' END";
            Map<String,String> row=row("FILE_CHECKOUT","c.id","CASE c.status WHEN 'CHECKED_IN' THEN COALESCE(c.checked_in_time,c.create_time) WHEN 'CANCELLED' THEN COALESCE(c.cancelled_time,c.create_time) ELSE c.create_time END",action,result);
            file(row);user(row,"c.actor_id");row.put("master_id","c.master_id");row.put("result","COALESCE(NULLIF(TRIM(c.status),''),'UNKNOWN')");row.put("controlled_file_id","c.base_iteration_id");
            row.put("related_object","CONCAT('版本 ',COALESCE(NULLIF(TRIM(f.version_no),''),'-'))");row.put("reason",first("c.cancel_reason","c.reason"));
            row.put("summary",joined(" / ",row.get("file_number"),row.get("file_name"),"f.version_no",action,result));
            row.put("detail_json",json("masterId","c.master_id","baseIterationId","c.base_iteration_id","status",row.get("result"),"cancelReason","c.cancel_reason","baseSourceSha256","c.base_source_sha256","checkinIterationId","c.checkin_iteration_id","checkinSourceSha256","c.checkin_source_sha256"));
            return select(row,"dcc_controlled_file_checkout c"+fileJoin("c.base_iteration_id")+" WHERE "+scoped("c"));
        }
        private static String distribution() {
            String result=label("d.status","PENDING","待阅读","SENT","已发送","READ","已阅读","ACKNOWLEDGED","已确认","RECOVERED","已回收");
            Map<String,String> row=row("FILE_DISTRIBUTION","d.id","CASE WHEN d.status='RECOVERED' AND d.recovered_at IS NOT NULL THEN d.recovered_at WHEN d.status IN ('ACKNOWLEDGED','READ') AND d.acknowledged_at IS NOT NULL THEN d.acknowledged_at ELSE COALESCE(d.update_time,d.create_time) END","'分发'",result);
            file(row);user(row,"COALESCE(d.recovered_by,d.acknowledged_by)");row.put("controlled_file_id","d.controlled_file_id");row.put("result","d.status");row.put("related_object","CONCAT('部门',d.department_id)");
            row.put("summary",joined(" / ",row.get("file_number"),row.get("file_name"),row.get("related_object"),label("d.distribution_medium","PUBLIC_FOLDER","公共区","PAPER","纸质"),result));
            row.put("detail_json",json("departmentId","d.department_id","distributionMedium","d.distribution_medium","status","d.status","acknowledgedAt",isoTime("d.acknowledged_at"),"recoveredAt",isoTime("d.recovered_at")));
            return select(row,"dcc_controlled_file_distribution d"+fileJoin("d.controlled_file_id")+" WHERE "+scoped("d"));
        }
        private static String assignment() {
            String files="SELECT af.* FROM dcc_project_code_assignment_file af WHERE "+scoped("af")+" AND (#{controlledFileId} IS NULL OR af.controlled_file_id=#{controlledFileId})";
            String count="(SELECT COUNT(*) FROM ("+files+") ac WHERE ac.assignment_id=a.id)";
            String firstId="(SELECT MIN(af1.id) FROM ("+files+") af1 WHERE af1.assignment_id=a.id)";
            String project=joined(" / ","pc.project_name","pc.project_code");
            Map<String,String> row=row("PROJECT_CODE_ASSIGNMENT","a.id","COALESCE(a.assigned_time,a.create_time)","'修正指派'",label("a.status","ACTIVE","生效","REVOKED","已撤回","EXPIRED","已过期"));
            user(row,"COALESCE(a.assigned_by,a.assignee_user_id)");row.put("result","a.status");row.put("project_code_id","a.project_code_id");row.put("assignment_id","a.id");
            row.put("file_number","af.file_number_snapshot"); row.put("file_name","CASE WHEN "+count+"=1 THEN af.file_name_snapshot WHEN a.file_count>0 THEN CONCAT(a.file_count,' 个文件') WHEN "+count+">0 THEN CONCAT("+count+",' 个文件') ELSE NULL END");
            row.put("related_object",project);row.put("summary",joined(" / ","a.assignment_no","CONCAT('指派给 ',COALESCE(NULLIF(TRIM("+userName("a.assignee_user_id")+"),''),'-'))",project));
            row.put("reason",first("a.assignment_reason","a.revoke_reason"));row.put("keyword_extra",joined(" ","a.assignment_no",project));
            row.put("detail_json",json("assignmentNo","a.assignment_no","scopeMode","a.scope_mode","assigneeUserId","a.assignee_user_id","fileCount","a.file_count","changedFileCount","a.changed_file_count","changedFieldCount","a.changed_field_count"));
            return select(row,"dcc_project_code_assignment a LEFT JOIN dcc_project_code pc ON pc.id=a.project_code_id AND "+scoped("pc")+" LEFT JOIN dcc_project_code_assignment_file af ON af.id="+firstId+" WHERE "+scoped("a"));
        }
        private static String change() {
            Map<String,String> row=row("PROJECT_CODE_CHANGE","i.id","COALESCE(i.changed_time,c.changed_time)","'字段修改'","'成功'");file(row);user(row,"i.operator_user_id");
            row.put("controlled_file_id","i.controlled_file_id");row.put("project_code_id","i.project_code_id");row.put("assignment_id","i.assignment_id");row.put("field_name","i.field_name");row.put("result","'SUCCESS'");
            String project=joined(" / ","pc.project_name","pc.project_code");row.put("related_object",project);row.put("summary",joined(" / ","i.field_label",joined(" -> ","i.old_value_text","i.new_value_text")));
            row.put("old_value_text","i.old_value_text");row.put("new_value_text","i.new_value_text");row.put("reason","c.change_reason");row.put("keyword_extra",joined(" ","i.field_name","i.field_label",project));
            row.put("detail_json",json("changeId","i.change_id","assignmentNo","a.assignment_no","fieldName","i.field_name","fieldLabel","i.field_label","source","c.source"));
            return select(row,"dcc_controlled_file_metadata_change_item i LEFT JOIN dcc_controlled_file_metadata_change c ON c.id=i.change_id AND "+scoped("c")+fileJoin("i.controlled_file_id")+" LEFT JOIN dcc_project_code pc ON pc.id=i.project_code_id AND "+scoped("pc")+" LEFT JOIN dcc_project_code_assignment a ON a.id=i.assignment_id AND "+scoped("a")+" WHERE "+scoped("i"));
        }
        private static String training() {
            String action="CASE WHEN p.acknowledged_at IS NOT NULL THEN '培训确认' WHEN p.last_viewed_at IS NOT NULL OR p.first_viewed_at IS NOT NULL THEN '培训阅读' ELSE '培训待执行' END";
            String status="CASE WHEN p.acknowledged_at IS NOT NULL THEN 'ACKNOWLEDGED' WHEN COALESCE(p.required_view_seconds,0)>0 AND COALESCE(p.accumulated_view_seconds,0)>=p.required_view_seconds THEN 'READY_TO_ACKNOWLEDGE' ELSE 'PENDING_VIEW' END";
            String result=label(status,"PENDING_VIEW","待阅读","READY_TO_ACKNOWLEDGE","待确认","ACKNOWLEDGED","已确认");
            Map<String,String> row=row("TRAINING_EXECUTION","p.id","COALESCE(p.acknowledged_at,p.last_viewed_at,p.first_viewed_at,p.create_time)",action,result);file(row);user(row,"p.user_id");
            row.put("controlled_file_id","p.controlled_file_id");row.put("result",status);row.put("related_object","'培训执行'");
            row.put("action_type","CASE WHEN p.acknowledged_at IS NOT NULL THEN 'TRAINING_ACKNOWLEDGE' WHEN p.last_viewed_at IS NOT NULL OR p.first_viewed_at IS NOT NULL THEN 'TRAINING_VIEW' ELSE 'TRAINING_PENDING' END");
            row.put("summary",joined(" / ",row.get("file_number"),row.get("file_name"),action,result));
            row.put("detail_json",json("requiredViewSeconds","p.required_view_seconds","accumulatedViewSeconds","p.accumulated_view_seconds","firstViewedAt",isoTime("p.first_viewed_at"),"lastViewedAt",isoTime("p.last_viewed_at"),"acknowledgedAt",isoTime("p.acknowledged_at")));
            return select(row,"dcc_controlled_file_training_progress p"+fileJoin("p.controlled_file_id")+" WHERE p.deleted=0 AND EXISTS (SELECT 1 FROM dcc_controlled_file owner_file WHERE owner_file.id=p.controlled_file_id AND owner_file.tenant_id=#{tenantId})");
        }
        private static String fileStatus(String value) {
            return label(value,"DRAFT","草稿","PENDING_DOC_CONTROL_REVIEW","评审中","PENDING_MATRIX_REVIEW","评审中","PENDING_MATRIX_APPROVAL","审批中","PENDING_DOC_CONTROL_APPROVAL","审批中","PENDING_APPLICANT_REWORK","返工中","PENDING_APPLICANT_TRAINING_RECORD","待培训","FINALIZING","放行中","TRAINING_IN_PROGRESS","培训中","PENDING_MANUAL_DISTRIBUTION","待分发","ACTIVE","当前受控","REJECTED","已驳回","WITHDRAWN","已撤回","OBSOLETE","已作废","SUPERSEDED","已替换","FINALIZATION_FAILED","放行失败","APPROVED","已审批");
        }
        private static String first(String... values) { return "COALESCE("+java.util.Arrays.stream(values).map(v->"NULLIF(TRIM("+v+"),'')").collect(java.util.stream.Collectors.joining(","))+",NULL)"; }
        private static String joined(String separator,String... values) { return "CONCAT_WS("+quote(separator)+","+java.util.Arrays.stream(values).map(v->"CASE WHEN TRIM(COALESCE("+v+",''))<>'' THEN "+v+" ELSE NULL END").collect(java.util.stream.Collectors.joining(","))+")"; }
        private static String quote(String value) { return "'"+value.replace("'","''")+"'"; }
        private static String label(String value,String... pairs) { StringBuilder sql=new StringBuilder("CASE ").append(value);for(int i=0;i<pairs.length;i+=2)sql.append(" WHEN ").append(quote(pairs[i])).append(" THEN ").append(quote(pairs[i+1]));return sql+" ELSE COALESCE(NULLIF(TRIM("+value+"),''),'-') END"; }
        private static String isoTime(String value) { return "REPLACE(CAST("+value+" AS CHAR),' ','T')"; }
        private static String json(String... pairs) { List<String> parts=new ArrayList<>();parts.add("'{'");for(int i=0;i<pairs.length;i+=2){parts.add(quote((i>0?",":"")+"\""+pairs[i]+"\":"));parts.add("JSON_QUOTE(COALESCE(CAST("+pairs[i+1]+" AS CHAR),''))");}parts.add("'}'");return "CONCAT("+String.join(",",parts)+")"; }
    }
}
