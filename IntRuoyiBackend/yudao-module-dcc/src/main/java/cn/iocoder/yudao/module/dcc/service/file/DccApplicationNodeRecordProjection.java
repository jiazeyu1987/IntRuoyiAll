package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO;
import org.flowable.task.api.history.HistoricTaskInstance;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/** Pure projection of authenticated historical facts; never looks up current user/dept configuration. */
public final class DccApplicationNodeRecordProjection {
    private DccApplicationNodeRecordProjection() {}
    public static List<DccApplicationNodeRecord> project(DccControlledFileDO file,String round,
            List<DccControlledFileSignatureSummaryRespVO> signatures,List<HistoricTaskInstance> history,
            List<DccControlledFileTaskAssigneeSnapshotDO> assignments) {
        Objects.requireNonNull(signatures);Objects.requireNonNull(history);Objects.requireNonNull(assignments);
        var tasks=new LinkedHashMap<String,HistoricTaskInstance>();
        for(var task:history) {
            if(task==null || task.getId()==null || task.getTaskDefinitionKey()==null || task.getTaskDefinitionKey().isBlank()
                    || !round.equals(task.getProcessInstanceId()) || !file.getTenantId().toString().equals(task.getTenantId())
                    || tasks.putIfAbsent(task.getId(),task)!=null)
                throw new IllegalStateException("前序节点任务与本文件正式BPM轮次或租户不一致");
        }
        var rows=new ArrayList<DccApplicationNodeRecord>();var signedTasks=new HashSet<String>();var signatureIds=new HashSet<Long>();
        for(var signed:signatures) {
            var task=tasks.get(signed.getTaskId());
            if(task==null || signed.getId()==null || !signatureIds.add(signed.getId())
                    || !round.equals(signed.getProcessInstanceId()) || !Objects.equals(file.getVersionNo(),signed.getVersionNo())
                    || signed.getActionType()==null || signed.getSignedAt()==null)
                throw new IllegalStateException("前序节点签名与真实任务或文件版本不一致");
            signedTasks.add(task.getId());
            DccApplicationNodeRecord.AssignmentTarget target=null;
            var fields=new ArrayList<DccApplicationNodeRecord.ConfirmedField>();
            if("ASSIGN".equals(signed.getActionType())) {
                var matched=assignments.stream().filter(a->Objects.equals(a.getAssignmentSignatureId(),signed.getId())).toList();
                if(matched.size()!=1)throw new IllegalStateException("前序指派缺少唯一正式签名绑定快照");
                var a=matched.get(0);
                if(!Objects.equals(a.getTenantId(),file.getTenantId()) || !Objects.equals(a.getControlledFileId(),file.getId())
                        || !round.equals(a.getProcessInstanceId()) || !task.getId().equals(a.getBpmTaskId())
                        || !Objects.equals(task.getTaskDefinitionKey(),a.getStageCode()) || Boolean.TRUE.equals(a.getDeleted())
                        || !Objects.equals(a.getLeaderUserId(),signed.getActorId()) || !Objects.equals(a.getAssignedTime(),signed.getSignedAt())
                        || a.getAssigneeUserId()==null)
                    throw new IllegalStateException("前序指派快照与签名、实际任务或轮次不一致");
                target=new DccApplicationNodeRecord.AssignmentTarget(a.getAssigneeUserId(),a.getAssigneeName(),a.getDepartmentName());
                fields.add(new DccApplicationNodeRecord.ConfirmedField("signoffAssignee","指派会签人",a.getAssigneeName()));
                fields.add(new DccApplicationNodeRecord.ConfirmedField("obligationDepartment","会签义务部门",a.getDepartmentName()));
            }
            if(Objects.equals(file.getFileOwnerSignatureId(),signed.getId()) && round.equals(file.getFileOwnerProcessInstanceId())) {
                if(!Objects.equals(file.getFileOwnerApprovalTaskId(),task.getId())) throw new IllegalStateException("文件负责人确认与批准任务不一致");
                fields.add(new DccApplicationNodeRecord.ConfirmedField("fileOwner","确认文件负责人",file.getFileOwnerNicknameSnapshot()));
            }
            if("APPROVE".equals(signed.getActionType()) && List.of("DOC_CONTROL_STANDARDIZATION","DOC_CONTROL_APPROVAL").contains(task.getTaskDefinitionKey())
                    && round.equals(file.getEffectiveProcessInstanceId()) && Objects.equals(file.getEffectiveSignatureId(),signed.getId())
                    && Objects.equals(file.getEffectiveTime(),signed.getSignedAt()))
                fields.add(new DccApplicationNodeRecord.ConfirmedField("effectiveTime","标准化签名生效时间",signed.getSignedAt().toString()));
            rows.add(new DccApplicationNodeRecord("SIGNATURE:"+signed.getId(),file.getId(),round,task.getId(),task.getTaskDefinitionKey(),task.getName(),
                    signed.getActionType(),actionResult(signed.getActionType()),signed.getActorId(),signed.getActorUsernameSnapshot(),
                    signed.getActorNicknameSnapshot(),signed.getActorDeptNameSnapshot(),signed.getComment(),signed.getSignedAt(),
                    signed.getId(),signed.getEvidenceStatus(),target,fields));
        }
        for(var task:tasks.values())if(task.getEndTime()!=null && !signedTasks.contains(task.getId())) {
            var variables=task.getTaskLocalVariables();
            Object result=variables==null?null:variables.get(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS);
            rows.add(new DccApplicationNodeRecord("TASK:"+task.getId(),file.getId(),round,task.getId(),task.getTaskDefinitionKey(),task.getName(),
                    "TASK_COMPLETED",result==null?null:result.toString(),positiveActor(task.getAssignee()),null,null,null,
                    variables==null?null:cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils.getTaskReason(task),
                    LocalDateTime.ofInstant(task.getEndTime().toInstant(),ZoneId.systemDefault()),null,null,null,List.of()));
        }
        return rows.stream().sorted(Comparator.comparing(DccApplicationNodeRecord::occurredAt).thenComparing(DccApplicationNodeRecord::recordId)).toList();
    }
    private static Long positiveActor(String value) {
        if(value==null)return null;
        try {long id=Long.parseLong(value);if(id>0)return id;} catch(NumberFormatException invalid) {throw new IllegalStateException("前序任务办理账号身份无效",invalid);}
        throw new IllegalStateException("前序任务办理账号身份无效");
    }
    private static String actionResult(String action) {
        return switch(action) {
            case "ASSIGN"->"已指派";case "APPROVE"->"已通过";case "REJECT"->"已驳回";
            case "RETURN"->"已退回";case "TRANSFER"->"已转办";case "ADD_SIGN"->"已加签";
            default->null;
        };
    }
}
