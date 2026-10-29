package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.handoff.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

/** Additional EDHR projection, not a second module provider. Existing workbench tasks remain single entries. */
@Component
public class MesActiveOrderHandoffApprovalProjection {
    public static final String SOURCE_TASK_TYPE="ACTIVE_ORDER_HANDOFF";
    @Resource private MesActiveOrderHandoffTaskMapper tasks;
    @Resource private MesActiveOrderHandoffService handoff;
    @Resource private MesEdhrApprovalOutcomeReader outcomeReader;
    @Resource private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;
    private static final Set<ApprovalTaskCapability> CAPABILITIES=Set.of(ApprovalTaskCapability.TIMELINE,ApprovalTaskCapability.AUDIT,ApprovalTaskCapability.EVIDENCE_LEDGER);
    public cn.iocoder.yudao.framework.common.pojo.PageResult<ApprovalTaskSummary> prefix(
            ApprovalTaskQueryContext context, long limit) {
        require(context.getLoginUserId() != null, "审批交接查询缺少本人身份");
        require(context.getViewType() == ApprovalTaskViewType.TODO || context.getViewType() == ApprovalTaskViewType.DONE,
                "审批交接查询视图无效");
        var page = tasks.selectApprovalCenterPrefix(tenant(), context.getLoginUserId(), context.isGlobalView(),
                context.getViewType() == ApprovalTaskViewType.DONE, context.getKeyword(), limit);
        return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(page.getList().stream().map(task -> summary(task, context.getLoginUserId())).toList(),
                page.getTotal());
    }
    public List<ApprovalTaskSummary> list(ApprovalTaskQueryContext context) {
        require(context.getLoginUserId()!=null,"审批交接查询缺少本人身份");
        var rows=context.isGlobalView()?tasks.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getTenantId,tenant())):tasks.own(tenant(),context.getLoginUserId());
        return rows.stream().filter(t->!Boolean.TRUE.equals(t.getNotificationOnly()))
                .filter(t->context.getViewType()==ApprovalTaskViewType.TODO?"TODO".equals(t.getStatus())&&handoff.current(t)
                                && !"QA_DECISION_HANDOFF".equals(t.getTaskType())
                        :"DONE".equals(t.getStatus())&&(context.isGlobalView()||Objects.equals(t.getCompletedBy(),context.getLoginUserId())))
                .filter(t->context.getKeyword()==null||context.getKeyword().isBlank()
                        ||(label(t.getTaskType())+" "+t.getActiveOrderId()+" "+t.getWorkOrderId()+" "+t.getReason()).contains(context.getKeyword().trim()))
                .map(task -> summary(task, context.getLoginUserId())).toList();
    }
    private ApprovalTaskSummary summary(MesActiveOrderHandoffTaskDO task, Long loginUserId) {
        validate(task);String path=URI.create(task.getActionUrl()).getPath();var query=new LinkedHashMap<>(actionQuery(task));
        if ("DONE".equals(task.getStatus())) query.put("handoffReadOnly", "1");
        var missing = requiredNavigationPermissions(task, path).stream()
                .filter(permission -> !permissions.hasAnyPermissions(loginUserId, permission)).toList();
        if (!missing.isEmpty()) query.put("requiredPermissionsMissing", String.join(",", missing));
        boolean ownResultAllowed = !"/user/profile".equals(path) || permissions.hasAnyPermissions(loginUserId,
                "mes:pro-feedback:query", "mes:pro-process-pool-team-leader:query",
                "mes:pro-edhr-nonconformance-review:query", "mes:pro-edhr-work-task:query");
        if (!ownResultAllowed) query.put("navigationContractMissing", "查看交接结果缺少本人交接查询权限");
        var outcome = outcomeReader.readAuthorizedHandoff(task);
        return MesEdhrApprovalOutcomeProjection.apply(ApprovalTaskSummary.builder().id("EDHR:"+SOURCE_TASK_TYPE+":"+task.getId())
                .moduleCode(ApprovalModuleCode.EDHR).sourceTaskType(SOURCE_TASK_TYPE).sourceTaskId(task.getId().toString())
                .businessKey(task.getId().toString()).businessTitle(label(task.getTaskType())+" / 活跃订单 "+task.getActiveOrderId())
                .businessCode(task.getActiveOrderId().toString())
                .currentNodeCode(task.getTaskType()).currentNodeName(label(task.getTaskType())).initiatorUserId(task.getInitiatedBy())
                .assigneeUserId(outcome.taskCompletedByUserId()).taskCreatedAt(task.getCreateTime()).taskCompletedAt(task.getCompletedAt())
                .detailRoute(path).detailQuery(query).decisionDetailRoute(path).decisionDetailQuery(query)
                .requiresSignature(!"QA_DECISION_HANDOFF".equals(task.getTaskType()))
                .availableActions("TODO".equals(task.getStatus()) && !"QA_DECISION_HANDOFF".equals(task.getTaskType())
                        && missing.isEmpty() && ownResultAllowed?Set.of("PROCESS_IN_MODULE"):Set.of())
                .capabilities(CAPABILITIES).build(), outcome);
    }
    private static List<String> requiredNavigationPermissions(MesActiveOrderHandoffTaskDO task, String path) {
        return switch (path) {
            case "/mes/pro/feedback/edhr-batch-production-fill", "/mes/pro/feedback/edhr-batch-pqc-fill" ->
                    task.getTaskType().endsWith("_RETURN")
                            ? List.of("mes:pro-feedback:query")
                            // Initial selection resolves the actual employee through the formal switch endpoint.
                            : List.of("mes:pro-feedback:query", "mes:pro-feedback:create");
            case "/mes/pro/process-pool/production-leader", "/mes/pro/process-pool/pqc-leader" ->
                    List.of("mes:pro-process-pool-team-leader:query");
            case "/mes/pro/feedback/edhr-nonconformance-review" -> List.of("mes:pro-edhr-nonconformance-review:query",
                    "mes:pro-edhr-nonconformance-review:dispose");
            case "/user/profile" -> List.of(); // Own notification reads require the authenticated user, not a menu permission.
            default -> throw failure("交接目标页权限合同未定义");
        };
    }
    public List<ApprovalTaskTimelineEntry> timeline(ApprovalTaskTimelineQueryContext context) {
        Long id;try{id=Long.valueOf(context.getSourceTaskId());}catch(RuntimeException e){throw failure("交接时间线身份无效");}
        require(id > 0 && Objects.equals(id.toString(), context.getSourceTaskId())
                && Objects.equals(context.getSourceTaskId(), context.getBusinessKey()) && context.getProcessInstanceId() == null,
                "交接时间线正式身份矛盾");
        var task=tasks.selectById(id);require(task!=null&&Objects.equals(tenant(),task.getTenantId()),"交接时间线不存在");
        if(!context.isGlobalView())access(task,context.getLoginUserId());validate(task);
        var outcome = outcomeReader.readAuthorizedHandoff(task);
        var result=new ArrayList<ApprovalTaskTimelineEntry>();
        result.add(entry(task,"CREATED","交接已创建",task.getInitiatedBy(),task.getCreateTime(),"TODO")
                .setSourceRemark(task.getReason()).setTaskOutcome("CREATED").setSourceStatus("TODO").setDecisionState("UNKNOWN"));
        if(task.getCompletedAt()!=null)result.add(MesEdhrApprovalOutcomeProjection.apply(
                entry(task,MesEdhrApprovalOutcomeProjection.action(outcome),MesEdhrApprovalOutcomeProjection.actionLabel(outcome),
                        null,null,task.getStatus()).setTaskCompletedAt(task.getCompletedAt()), outcome));
        return result;
    }
    private ApprovalTaskTimelineEntry entry(MesActiveOrderHandoffTaskDO t,String action,String text,Long actor,java.time.LocalDateTime at,String state) {
        return ApprovalTaskTimelineEntry.builder().id("EDHR:"+SOURCE_TASK_TYPE+":"+t.getId()+":"+action).moduleCode(ApprovalModuleCode.EDHR)
                .sourceTaskType(SOURCE_TASK_TYPE).sourceTaskId(t.getId().toString()).businessKey(t.getId().toString())
                .nodeCode(t.getTaskType()).nodeName(label(t.getTaskType())).action(action).actionLabel(text).actorUserId(actor).actedAt(at)
                .taskCreatedAt(t.getCreateTime())
                .status(state).evidenceType("REAL_ACTIVE_ORDER_HANDOFF").domainReferenceId(t.getId().toString()).build();
    }
}
