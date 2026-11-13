package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterValueUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterValueUtils.text;

/** Java counterparts of approval-center column getValue, after the original authoritative proof reader. */
public final class ApprovalListFilterValues {
    private ApprovalListFilterValues() { }
    public static final Set<String> STATIC_KEYS=Set.of("source","capabilities","applicant");
    public static Map<String,ListFilterValueUtils.Field<ApprovalTaskSummary>> dynamicFields(ApprovalTaskQueryContext context) {
        boolean read=context.getViewType()!=ApprovalTaskViewType.TODO;
        return Map.of("businessSummary",text(row->businessSummary(row)),"node",text(row->join(node(row),read&&row.getModuleCode()==ApprovalModuleCode.EDHR?"正式决定人见审核人列":nodeSub(row))),
                "reviewer",text(row->read&&row.getModuleCode()==ApprovalModuleCode.EDHR?"姓名未记录；"+identity(row.getDecisionActorUserId()):reviewer(row)),
                "approvalResult",text(row->join(decision(row),recorded(row.getDecisionSourceStatus()),result(row))),
                "approvalRemark",text(row->join(recorded(row.getApprovalRemark()),row.getSourceRemark(),row.getTerminationReason(),
                        recorded(row.getEvidenceType()),recorded(row.getEvidenceId()),row.getApprovalRemark()==null||row.getApprovalRemark().isEmpty()?"--":row.getApprovalRemark())),
                "time",text(row->join(time(row.getTaskCreatedAt(),"未记录"),time(row.getTaskCompletedAt(),"未记录"),
                        identity(row.getTaskCompletedByUserId()),time(row.getDecisionAt(),"未记录"),
                        time(row.getTaskCreatedAt()!=null?row.getTaskCreatedAt():row.getInitiatedAt(),"--"),time(row.getTaskCompletedAt(),"--"))));
    }
    private static String businessSummary(ApprovalTaskSummary row) {
        List<Object> parts=new ArrayList<>(Arrays.asList(title(row),identifier(row),task(row),recorded(row.getSourceStatus()),
                recorded(row.getBusinessStatus()),decision(row),recorded(row.getDecisionSourceStatus()),recorded(row.getApprovalRemark()),
                row.getSourceRemark(),row.getTerminationReason()));
        if(row.getBusinessContextTags()!=null)for(String tag:row.getBusinessContextTags())if(!empty(tag))parts.add(tag);
        return join(parts.toArray());
    }
    private static String title(ApprovalTaskSummary row) {
        if(empty(row.getBusinessTitle()))return "--";
        return row.getBusinessTitle().trim().replace("DCC Controlled File Approval","文控受控文件审批")
                .replace("Expense Dept Leader Approval","费用部门负责人审批").replace("eDHR Approval V1","电子批记录审批第一版");
    }
    private static String identifier(ApprovalTaskSummary row) {
        if(!empty(row.getBusinessCode()))return "业务编号："+row.getBusinessCode().trim();
        String key=empty(row.getBusinessKey())?row.getSourceTaskId():row.getBusinessKey();
        if(empty(key))return "--";
        key=key.trim();if(key.startsWith("FORM_ACTION:"))return "表单动作实例";
        if(english(key))return empty(row.getBusinessKey())?"任务编号已配置":"业务键已配置";
        return key;
    }
    private static String node(ApprovalTaskSummary row) {
        String name=row.getCurrentNodeName();if(name!=null)name=name.trim();if(!empty(name)&&!english(name))return name;
        if(name!=null&&STATUS.containsKey(name))return STATUS.get(name);
        return mapped(row.getCurrentNodeCode(),"未配置中文节点");
    }
    private static String nodeSub(ApprovalTaskSummary row) {
        String name=reviewer(row);return !"--".equals(name)?"审核人："+name:mapped(row.getBusinessStatus(),"未配置中文状态");
    }
    private static String reviewer(ApprovalTaskSummary row) {
        if(!empty(row.getAssigneeRoleName()))return "审批角色："+row.getAssigneeRoleName();
        return !empty(row.getAssigneeUserName())?row.getAssigneeUserName():row.getAssigneeUserId()!=null?"用户 #"+row.getAssigneeUserId():"--";
    }
    private static String task(ApprovalTaskSummary row) {
        return switch(Objects.toString(row.getTaskOutcome(),"")) {
            case "CREATED"->"交接已创建";case "WAIT"->"待处理";case "RUNNING"->"处理中";
            case "COMPLETED"->"任务已完成";case "CANCELLED"->"已取消";case "OVERDUE"->"已逾期";
            default->"任务状态未识别（"+recorded(row.getSourceStatus())+"）";
        };
    }
    private static String decision(ApprovalTaskSummary row) {
        if("APPROVE".equals(row.getDecisionState())&&row.getApprovalResult()==ApprovalTaskReviewResult.APPROVE)return "审批通过";
        if("REJECT".equals(row.getDecisionState())&&row.getApprovalResult()==ApprovalTaskReviewResult.REJECT)return "审批驳回";
        return "UNSUPPORTED".equals(row.getDecisionState())?"决定未识别（"+recorded(row.getDecisionSourceStatus())+"）":"决定未知";
    }
    private static String result(ApprovalTaskSummary row) {return row.getApprovalResult()==null?"":row.getApprovalResult()==ApprovalTaskReviewResult.APPROVE?"通过":"驳回";}
    private static String mapped(String value,String unknown) {
        if(empty(value))return "--";String v=value.trim();return STATUS.getOrDefault(v,english(v)?unknown:v);
    }
    private static final Map<String,String> STATUS=Map.ofEntries(
        Map.entry("MY_INITIATED","我发起的"),Map.entry("TODO","待办"),Map.entry("DONE","已办"),Map.entry("CC","抄送我的"),
        Map.entry("APPROVE","通过"),Map.entry("APPROVED","已通过"),Map.entry("REJECT","驳回"),Map.entry("REJECTED","已驳回"),
        Map.entry("CANCELED","已取消"),Map.entry("CANCELLED","已取消"),Map.entry("RUNNING","进行中"),Map.entry("PENDING","待处理"),
        Map.entry("PENDING_REVIEW","待审核"),Map.entry("PENDING_APPROVAL","待批准"),Map.entry("WRITING","写入中"),Map.entry("COMPLETED","已完成"),
        Map.entry("WRITE_FAILED","写入失败"),Map.entry("PENDING_SUPERVISOR_REVIEW","待直属主管审核"),Map.entry("PENDING_MATRIX_REVIEW","待会签"),
        Map.entry("PENDING_MATRIX_APPROVAL","待批准"),Map.entry("PENDING_DOC_CONTROL_REVIEW","待文控审核"),Map.entry("PENDING_APPLICANT_REWORK","待申请人返工"),
        Map.entry("PENDING_APPLICANT_TRAINING_RECORD","待文控上传培训记录"),Map.entry("PENDING_DELAYED_EFFECTIVE_CONFIRM","待延迟生效确认"),
        Map.entry("CONTROLLED_PENDING_EFFECTIVE","受控（待生效）"),Map.entry("WORKING","工作小版本"),Map.entry("DRAFT","草稿"),Map.entry("NEW","新建"),
        Map.entry("SUBMITTED","已提交"),Map.entry("APPROVING","审批中"),Map.entry("ACTIVE","现行"),Map.entry("SUPERSEDED","已替代"),
        Map.entry("OBSOLETE","已作废"),Map.entry("WITHDRAWN","已撤回"),Map.entry("DELETED","已删除"),Map.entry("APPROVE_USER_SELECT","选择审批人"),
        Map.entry("START","开始"),Map.entry("START_EVENT","开始"),Map.entry("END","结束"),Map.entry("END_EVENT","结束"));
    public static void enrichUsers(List<ApprovalTaskSummary> rows,AdminUserApi users) {
        Set<Long> ids=new HashSet<>();rows.forEach(row->{if(row.getInitiatorUserId()!=null)ids.add(row.getInitiatorUserId());if(row.getAssigneeUserId()!=null)ids.add(row.getAssigneeUserId());});
        if(ids.isEmpty())return;var names=Objects.requireNonNull(users.getUserMap(ids),"APPROVAL_TASK_USER_MAP_REQUIRED");
        rows.forEach(row->{row.setInitiatorUserName(userName(names,row.getInitiatorUserId()));row.setAssigneeUserName(userName(names,row.getAssigneeUserId()));});
    }
    private static String userName(Map<Long,AdminUserRespDTO> users,Long id) {
        if(id==null)return null;var user=users.get(id);if(user==null)return "用户不存在("+id+")";
        String username=empty(user.getUsername())?null:user.getUsername().trim(),nickname=empty(user.getNickname())?null:user.getNickname().trim();
        if(nickname!=null&&nickname.contains("??"))return username!=null?username:nickname;
        return nickname!=null&&username!=null&&!nickname.equals(username)?nickname+"("+username+")":nickname!=null?nickname:username;
    }
    private static String identity(Long id){return id==null?"未记录":"系统用户 ID："+id;}
    private static String recorded(Object value){return value==null||value.toString().trim().isEmpty()?"未记录":value.toString();}
    private static String time(LocalDateTime value,String absent){return value==null?absent:value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));}
    private static boolean empty(String text){return text==null||text.trim().isEmpty();}
    private static boolean english(String text){return text!=null&&text.matches(".*[A-Za-z].*");}
    private static String join(Object... values){return Arrays.stream(values).map(v->v==null?"":v.toString()).collect(java.util.stream.Collectors.joining(" "));}
}
