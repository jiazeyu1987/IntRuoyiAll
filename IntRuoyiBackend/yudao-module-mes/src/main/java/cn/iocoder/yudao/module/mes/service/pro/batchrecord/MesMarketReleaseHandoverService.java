package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesMarketReleaseHandoverMapper.Row;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.*;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MesMarketReleaseHandoverService {
    private final MesMarketReleaseHandoverMapper handovers;
    private final MesMarketReleaseAccess access;
    private final MesProEdhrBatchExecutionVisibilityService visibility;
    private final MesProEdhrBatchExecutionMapper batches;
    private final MesProEdhrBatchExecutionOriginMapper origins;
    private final MesProEdhrReleaseTransactionMapper transactions;
    private final MesProEdhrWorkTaskMapper tasks;
    private final MesProEdhrWorkTaskService taskService;
    private final MesProductionReleaseRequiredCandidateResolver candidates;
    private final AdminUserApi users;
    private final ApprovalAuthorityApi authority;
    private final GxpAuditService ledger;
    private final MesProEdhrOperationAuditService operationAudit;
    private final MesReleaseTaskNotificationService notifications;

    public record UserOption(Long id,String name) {}
    public record Context(Long batchExecutionId,Long workTaskId,String version,boolean visible,boolean writable,
                          boolean canHandover,boolean canRecall,String currentUserName,
                          String blocker,List<UserOption> candidates,List<Row> history) {}

    public MesMarketReleaseAccess.Access accessState(Long batchId,Long activeOrderId) {
        require((batchId==null)!=(activeOrderId==null),"请提供唯一批次或活跃工单身份");
        if(batchId==null) {
            var rows=handovers.activeOrderHistory(activeOrderId);
            if(rows.isEmpty()) return MesMarketReleaseAccess.resolve(List.of(),actor());
            batchId=rows.get(0).getBatchExecutionId();
        }
        var batch=requireBatch(batchId,false);
        visibility.requireVisibleBatch(batch,actor());
        return access.get(batchId,actor());
    }

    public Context context(Long batchId) {
        var batch=requireBatch(batchId,false);
        Long actor=actor();
        visibility.requireVisibleBatch(batch,actor);
        var current=access.get(batchId,actor);
        require(current.visible(),"该批记录的交接授权已被回收");
        var history=handovers.history(batchId);
        var transaction=transactions.selectCurrentByBatchExecutionId(batchId);
        require(transaction==null || Objects.equals(transaction.getTenantId(),TenantContextHolder.getRequiredTenantId()),"放行事务租户归属不一致");
        var task=transaction==null ? null : releaseTask(batchId,transaction.getId());
        boolean pending=transaction!=null && "PENDING_APPROVAL".equals(transaction.getReleaseStatus())
                && task!=null && Set.of("TODO","DOING","OVERDUE").contains(task.getStatus())
                && !Set.of(15,40,50,60).contains(batch.getStatus());
        boolean original=current.original() || history.isEmpty() && task!=null
                && MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(task,actor);
        boolean handover=pending && original && !current.active();
        var options=handover ? eligibleUsers().stream().filter(user -> !Objects.equals(user.id(),actor)).toList() : List.<UserOption>of();
        return new Context(batchId,task==null ? null : task.getId(),version(history),true,current.writable(),handover,
                pending && current.original() && current.active(),current.currentUserName(),
                pending ? "" : "该单尚无可办理的正式上市放行待办，或已完成、冻结、作废",
                options,history);
    }

    @Transactional(rollbackFor=Exception.class)
    public Context handover(Long batchId,Long recipient,String reason,String expectedVersion,String commandKey) {
        require(recipient!=null && recipient>0 && !Objects.equals(recipient,actor()),"请选择其他有效接收人");
        return change(batchId,recipient,reason,expectedVersion,commandKey,false);
    }
    @Transactional(rollbackFor=Exception.class)
    public Context recall(Long batchId,String reason,String expectedVersion,String commandKey) {
        return change(batchId,null,reason,expectedVersion,commandKey,true);
    }

    private Context change(Long batchId,Long recipient,String reason,String expectedVersion,String commandKey,boolean recall) {
        require(reason!=null && !reason.isBlank() && reason.length()<=1000,"请填写有效的交接或回收原因");
        require(commandKey!=null && commandKey.matches("[A-Za-z0-9_-]{8,100}"),"操作请求编号无效");
        Long actor=actor(),tenant=TenantContextHolder.getRequiredTenantId();
        // ponytail: reuse the signature ledger lock; no separate handover lock manager.
        ledger.acquireLedgerLock();
        var revision=authority.lockTenantApprovalAuthority(tenant);
        var batch=requireBatch(batchId,true);
        var history=handovers.history(batchId);
        Row prior=history.isEmpty() ? null : history.get(0);
        Row replay=recall ? prior : handovers.command(commandKey);
        if(replay!=null && (recall ? commandKey.equals(replay.getRecallCommandKey()) : commandKey.equals(replay.getCommandKey()))) {
            require(Objects.equals(replay.getBatchExecutionId(),batchId) && Objects.equals(replay.getOriginalUserId(),actor)
                    && Objects.equals(recall ? replay.getRecallReason() : replay.getReason(),reason.trim())
                    && (recall || Objects.equals(replay.getRecipientUserId(),recipient)),"同一请求编号不能用于不同交接内容");
            return context(batchId);
        }
        require(Objects.equals(version(history),expectedVersion),"交接状态已变化，请刷新后重试");
        var ctx=context(batchId);
        require(recall ? ctx.canRecall() : ctx.canHandover(),recall ? "当前用户不能回收该单" : "当前用户不能交接该单");
        var transaction=transactions.selectCurrentByBatchExecutionId(batchId);
        require(transaction==null || Objects.equals(transaction.getTenantId(),TenantContextHolder.getRequiredTenantId()),"放行事务租户归属不一致");
        require(transaction!=null && "PENDING_APPROVAL".equals(transaction.getReleaseStatus()),"放行已完成或状态已变化");
        transaction=transactions.selectByIdForUpdate(transaction.getId());
        var task=releaseTask(batchId,transaction.getId());
        require(task!=null,"正式上市放行待办不存在");
        task=tasks.selectByIdAndTenantForUpdate(task.getId(),tenant);
        require(task!=null && Set.of("TODO","DOING","OVERDUE").contains(task.getStatus()),"放行任务已完成或不再可办理");
        Long original=prior==null ? actor : prior.getOriginalUserId();
        Long next=recall ? original : recipient;
        require(eligibleUsers().stream().anyMatch(user -> Objects.equals(user.id(),next)),"接收人不具备当前上市放行资格");
        var sourceOrigins=origins.selectListByBatchExecutionId(batchId);
        require(!sourceOrigins.isEmpty() && sourceOrigins.get(0).getActiveOrderId()!=null,"批次缺少正式活跃工单来源");
        Long activeId=sourceOrigins.get(0).getActiveOrderId();
        require(sourceOrigins.stream().allMatch(o -> Objects.equals(o.getActiveOrderId(),activeId)
                && Objects.equals(o.getTenantId(),tenant) && Objects.equals(o.getWorkOrderId(),batch.getWorkOrderId())),"活跃工单来源不一致");
        Row row;
        String before=JsonUtils.toJsonString(!recall ? Map.of() : prior);
        if(recall) {
            row=prior;
            row.setRecalledAt(LocalDateTime.now()).setRecallReason(reason.trim()).setRecallCommandKey(commandKey);
            require(handovers.updateById(row)==1,"回收记录保存失败");
        } else {
            var originalUser=users.getUser(original); var target=users.getUser(recipient);
            require(originalUser!=null && target!=null,"交接人员不存在");
            row=new Row().setBatchExecutionId(batchId).setActiveOrderId(activeId).setReleaseTransactionId(transaction.getId())
                    .setWorkTaskId(task.getId()).setOriginalUserId(original).setOriginalUserName(originalUser.getNickname())
                    .setRecipientUserId(recipient).setRecipientUserName(target.getNickname())
                    .setOriginalCandidateSnapshot(task.getCandidateUserSnapshot()).setCommandKey(commandKey)
                    .setReason(reason.trim()).setHandedAt(LocalDateTime.now());
            row.setTenantId(tenant);
            require(handovers.insert(row)==1,"交接记录保存失败");
        }
        require(tasks.updateById(new MesProEdhrWorkTaskDO().setId(task.getId()).setAssigneeUserId(next)
                .setCandidateUserSnapshot(next.toString()).setOwnershipLastTransferredAt(LocalDateTime.now())
                .setOwnershipLastTransferredBy(actor).setReason(reason.trim()))==1,"放行任务归属更新失败");
        task=tasks.selectById(task.getId());
        require(transaction.getVersion()!=null && transaction.getVersion()>=0,"放行事务版本缺失");
        require(transactions.update(new MesProEdhrReleaseTransactionDO().setVersion(transaction.getVersion()+1),
                new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>().eq(MesProEdhrReleaseTransactionDO::getId,transaction.getId())
                        .eq(MesProEdhrReleaseTransactionDO::getVersion,transaction.getVersion())
                        .eq(MesProEdhrReleaseTransactionDO::getReleaseStatus,"PENDING_APPROVAL"))==1,"放行事务已变化，请刷新后重试");
        authority.incrementLockedAuthorityRevision(tenant,revision.revision());
        taskService.syncMarketReleaseHandoverEntitlements(task,original);
        audit(row,before,reason.trim(),commandKey,recall,batch);
        notifications.scheduleHandover(task,actor,row.getRecipientUserId(),row.getId(),recall,reason.trim(),row.getOriginalUserName());
        return context(batchId);
    }

    private List<UserOption> eligibleUsers() {
        var resolved=candidates.resolveRequiredCandidates(TenantContextHolder.getRequiredTenantId(),MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
        return users.getUserList(resolved.candidateUserIds()).stream()
                .filter(user -> Objects.equals(user.getStatus(),0)).map(user -> new UserOption(user.getId(),user.getNickname())).toList();
    }
    private MesProEdhrWorkTaskDO releaseTask(Long batchId,Long transactionId) {
        var rows=tasks.selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBatchExecutionId,batchId).eq(MesProEdhrWorkTaskDO::getTaskType,"RELEASE_APPROVE")
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType,"RELEASE_TRANSACTION").eq(MesProEdhrWorkTaskDO::getBusinessScopeId,transactionId));
        require(rows.size()<=1,"同一上市放行事务存在多个待办");
        return rows.isEmpty() ? null : rows.get(0);
    }
    private MesProEdhrBatchExecutionDO requireBatch(Long id,boolean lock) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var batch=lock ? batches.selectByTenantIdAndIdForUpdate(tenant,id) : batches.selectById(id);
        require(batch!=null && Objects.equals(batch.getTenantId(),tenant),"当前租户的批记录不存在");
        return batch;
    }
    private static String version(List<Row> rows) {
        return rows.isEmpty() ? "NONE" : rows.get(0).getId()+":"+(rows.get(0).getRecalledAt()==null ? "ACTIVE" : "RECALLED");
    }
    private static Long actor() {
        Long actor=SecurityFrameworkUtils.getLoginUserId();require(actor!=null,"请先登录");return actor;
    }
    private static void require(boolean valid,String message) { if(!valid) throw new ServiceException(403,message); }

    private void audit(Row row,String before,String reason,String key,boolean recall,MesProEdhrBatchExecutionDO batch) {
        String action=recall ? "recall" : "handover";
        String label=recall ? "上市放行回收" : "上市放行交接";
        String summary=label;
        var metadata=new LinkedHashMap<String,Object>();metadata.put("activeOrderId",row.getActiveOrderId());
        metadata.put("handoverId",row.getId());metadata.put("originalUserId",row.getOriginalUserId());
        metadata.put("recipientUserId",row.getRecipientUserId());metadata.put("reason",reason);
        metadata.put("handoverFromName",row.getOriginalUserName());metadata.put("handoverToName",row.getRecipientUserName());metadata.put("handoverReason",reason);
        operationAudit.recordInCallerTransaction(new MesProEdhrOperationAuditCommand().setRequestId(key)
                .setObjectType("ACTIVE_ORDER").setObjectId(row.getActiveOrderId().toString())
                .setBatchExecutionId(row.getBatchExecutionId()).setWorkTaskId(row.getWorkTaskId()).setRouteId(batch.getRouteId())
                .setOperationType(recall ? "MARKET_RELEASE_RECALL" : "MARKET_RELEASE_HANDOVER").setActionName(summary)
                .setActorUserId(actor()).setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr-release:approve").setPermissionDecision("ALLOW").setResultStatus("SUCCESS")
                .setBeforeSummaryHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(before))
                .setAfterSummaryHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(row)))
                .setMetadataJson(JsonUtils.toJsonString(metadata)).setOccurredAt(LocalDateTime.now()));
        ledger.append(GxpAuditCommand.builder().operationId("mes.market-release."+action).subjectId(row.getId().toString())
                .subjectVersion(recall ? "2" : "1")
                .beforeState(GxpAuditStateEnvelope.builder().state(recall ? "ACTIVE" : "ABSENT").objectVersion(recall ? "1" : "0").canonicalJson(before).build())
                .afterState(GxpAuditStateEnvelope.builder().state(recall ? "RECALLED" : "ACTIVE").objectVersion(recall ? "2" : "1").canonicalJson(JsonUtils.toJsonString(row)).build())
                .reason(reason).reasonSource("USER").reasonCode("MARKET_RELEASE_"+action.toUpperCase(Locale.ROOT))
                .resultStatus("SUCCESS").idempotencyKey(key).sourceType("SERVICE_METHOD")
                .sourceLocator(MesMarketReleaseHandoverService.class.getName()+"#"+action)
                .links(List.of(new GxpAuditRelation("SOURCE","MES_ACTIVE_ORDER",row.getActiveOrderId().toString(),null,null),
                        new GxpAuditRelation("SOURCE","MES_EDHR_BATCH",row.getBatchExecutionId().toString(),null,null),
                        new GxpAuditRelation("SOURCE","MES_WORK_TASK",row.getWorkTaskId().toString(),null,null))).build());
    }
}
