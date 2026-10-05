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
    private static final Set<ApprovalTaskCapability> CAPABILITIES=Set.of(ApprovalTaskCapability.TIMELINE,ApprovalTaskCapability.AUDIT,ApprovalTaskCapability.EVIDENCE_LEDGER);
    public List<ApprovalTaskSummary> list(ApprovalTaskQueryContext context) {
        require(context.getLoginUserId()!=null,"审批交接查询缺少本人身份");
        var rows=context.isGlobalView()?tasks.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getTenantId,tenant())):tasks.own(tenant(),context.getLoginUserId());
        return rows.stream().filter(t->!Boolean.TRUE.equals(t.getNotificationOnly()))
                .filter(t->context.getViewType()==ApprovalTaskViewType.TODO?"TODO".equals(t.getStatus())&&handoff.current(t)
                        :"DONE".equals(t.getStatus())&&(context.isGlobalView()||Objects.equals(t.getCompletedBy(),context.getLoginUserId())))
                .filter(t->context.getKeyword()==null||context.getKeyword().isBlank()
                        ||(label(t.getTaskType())+" "+t.getActiveOrderId()+" "+t.getWorkOrderId()+" "+t.getReason()).contains(context.getKeyword().trim()))
                .map(this::summary).toList();
    }
    private ApprovalTaskSummary summary(MesActiveOrderHandoffTaskDO task) {
        validate(task);String path=URI.create(task.getActionUrl()).getPath();var query=actionQuery(task);
        return ApprovalTaskSummary.builder().id("EDHR:"+SOURCE_TASK_TYPE+":"+task.getId())
                .moduleCode(ApprovalModuleCode.EDHR).sourceTaskType(SOURCE_TASK_TYPE).sourceTaskId(task.getId().toString())
                .businessKey(task.getId().toString()).businessTitle(label(task.getTaskType())+" / 活跃订单 "+task.getActiveOrderId())
                .businessCode(task.getActiveOrderId().toString()).businessStatus(task.getStatus())
                .currentNodeCode(task.getTaskType()).currentNodeName(label(task.getTaskType())).initiatorUserId(task.getInitiatedBy())
                .assigneeUserId(task.getCompletedBy()).taskCreatedAt(task.getCreateTime()).taskCompletedAt(task.getCompletedAt())
                .detailRoute(path).detailQuery(query).decisionDetailRoute(path).decisionDetailQuery(query)
                .requiresSignature(true).availableActions("TODO".equals(task.getStatus())?Set.of("PROCESS_IN_MODULE"):Set.of())
                .capabilities(CAPABILITIES).build();
    }
    public List<ApprovalTaskTimelineEntry> timeline(ApprovalTaskTimelineQueryContext context) {
        Long id;try{id=Long.valueOf(context.getSourceTaskId());}catch(RuntimeException e){throw failure("交接时间线身份无效");}
        var task=tasks.selectById(id);require(task!=null&&Objects.equals(tenant(),task.getTenantId()),"交接时间线不存在");
        if(!context.isGlobalView())access(task,context.getLoginUserId());validate(task);
        var result=new ArrayList<ApprovalTaskTimelineEntry>();
        result.add(entry(task,"CREATED","交接已创建",task.getInitiatedBy(),task.getCreateTime(),"TODO"));
        if(task.getCompletedAt()!=null)result.add(entry(task,task.getStatus(),"CANCELED".equals(task.getStatus())?"周期已关闭":"正式业务已完成",
                task.getCompletedBy(),task.getCompletedAt(),task.getStatus()));
        return result;
    }
    private ApprovalTaskTimelineEntry entry(MesActiveOrderHandoffTaskDO t,String action,String text,Long actor,java.time.LocalDateTime at,String state) {
        return ApprovalTaskTimelineEntry.builder().id("EDHR:"+SOURCE_TASK_TYPE+":"+t.getId()+":"+action).moduleCode(ApprovalModuleCode.EDHR)
                .sourceTaskType(SOURCE_TASK_TYPE).sourceTaskId(t.getId().toString()).businessKey(t.getId().toString())
                .nodeCode(t.getTaskType()).nodeName(label(t.getTaskType())).action(action).actionLabel(text).actorUserId(actor).actedAt(at)
                .comment(t.getReason()).status(state).evidenceType("REAL_ACTIVE_ORDER_HANDOFF").domainReferenceId(t.getId().toString()).build();
    }
}
