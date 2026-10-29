package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MesReleaseTaskNotificationService {
    @Resource private MesReleaseTaskNotifyDeliveryMapper deliveryMapper;
    @Resource private MesReleaseTaskNotificationDispatchService dispatchService;
    @Resource private MesReleaseTaskNotificationAudit audit;
    @Resource private PermissionApi permissionApi;
    @Resource private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesMarketReleaseAccess marketReleaseAccess;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void scheduleAssigned(MesProEdhrWorkTaskDO task, Long initiatedBy) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("通知意图必须加入正式放行任务事务");
        }
        Long tenant = TenantContextHolder.getRequiredTenantId();
        MesReleaseTaskNotificationContract.tenant(tenant);
        String template = MesReleaseTaskNotificationContract.taskTemplate(task);
        if (initiatedBy == null || initiatedBy <= 0 || !"TODO".equals(task.getStatus())) {
            throw new IllegalArgumentException("通知意图必须来自真实操作者新建的正式待办");
        }
        List<Long> recipients = MesReleaseTaskNotificationContract.candidates(task.getCandidateUserSnapshot());
        Map<String, String> params = new LinkedHashMap<>();
        params.put("workOrderCode", task.getWorkOrderCode()); params.put("batchCode", task.getBatchCode());
        params.put("processName", task.getProcessName()); params.put("actionUrl", task.getActionUrl());
        params.put("workTaskId", task.getId().toString()); params.put("reason", "正式放行任务候选交接");
        String frozenJson = JsonUtils.toJsonString(params);
        audit.lock();
        for (Long recipient : recipients) {
            MesReleaseTaskNotifyDeliveryDO row = new MesReleaseTaskNotifyDeliveryDO()
                    .setTenantId(tenant).setWorkTaskId(task.getId()).setEventType("ASSIGNED")
                    .setUserId(recipient).setBusinessKey("MES_EDHR_RELEASE_TASK_ASSIGNED:" + task.getId() + ":USER:" + recipient)
                    .setTemplateCode(template).setTemplateParamsJson(frozenJson).setInitiatedBy(initiatedBy)
                    .setStatus("PENDING").setAttemptCount(0).setRowVersion(0);
            if (deliveryMapper.insert(row) != 1 || row.getId() == null) {
                throw new IllegalStateException("正式通知意图未完整持久化");
            }
            audit.created(row);
        }
        Long taskId = task.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                Long previous = TenantContextHolder.getTenantId();
                try {
                    TenantContextHolder.setTenantId(tenant);
                    dispatchService.dispatchAssigned(tenant, taskId);
                } catch (RuntimeException failure) {
                    log.error("Committed release task notification dispatch failed: tenant={}, task={}, failure={}",
                            tenant, taskId, MesReleaseTaskNotificationContract.safeError(failure));
                } finally {
                    if (previous == null) { TenantContextHolder.clear(); }
                    else { TenantContextHolder.setTenantId(previous); }
                }
            }
        });
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void scheduleHandover(MesProEdhrWorkTaskDO task, Long actorId, Long recipient,
                                 Long handoverId, boolean recalled, String reason, String originalName) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        MesReleaseTaskNotificationContract.taskTemplate(task);
        String event = recalled ? "RECALLED" : "HANDED_OVER";
        Map<String,String> params = new LinkedHashMap<>();
        params.put("workOrderCode",task.getWorkOrderCode()); params.put("batchCode",task.getBatchCode());
        params.put("workTaskId",task.getId().toString()); params.put("reason",reason);
        params.put("originalName",originalName);
        params.put("actionUrl",recalled ? "/user/profile" : task.getActionUrl());
        var row = new MesReleaseTaskNotifyDeliveryDO().setTenantId(tenant).setWorkTaskId(task.getId())
                .setEventType(event).setEventRevision(handoverId).setUserId(recipient)
                .setBusinessKey("MES_MARKET_RELEASE:"+event+":"+handoverId)
                .setTemplateCode("MES_MARKET_RELEASE_"+event).setTemplateParamsJson(JsonUtils.toJsonString(params))
                .setInitiatedBy(actorId).setStatus("PENDING").setAttemptCount(0).setRowVersion(0);
        audit.lock();
        if(deliveryMapper.insert(row)!=1) throw new IllegalStateException("交接通知保存失败");
        audit.handoverCreated(row);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                Long previous=TenantContextHolder.getTenantId();
                try { TenantContextHolder.setTenantId(tenant); dispatchService.dispatchAssigned(tenant,task.getId()); }
                catch(RuntimeException failure) { log.error("Handover notification delivery failed: tenant={}, task={}, failure={}",tenant,task.getId(),MesReleaseTaskNotificationContract.safeError(failure)); }
                finally { if(previous==null) TenantContextHolder.clear(); else TenantContextHolder.setTenantId(previous); }
            }
        });
    }

    public boolean canRetry(Long actor,MesReleaseTaskNotifyDeliveryDO row) {
        if(!List.of("PENDING","FAILED").contains(row.getStatus())) return false;
        if (MesReleaseTaskNotificationContract.outcome(row.getEventType())) {
            return java.util.Objects.equals(row.getUserId(),actor)
                    && permissionApi.hasAnyPermissions(actor,MesReleaseTaskNotificationContract.QUERY_PERMISSION);
        }
        var task=workTaskMapper.selectById(row.getWorkTaskId());
        if(task==null || task.getBatchExecutionId()==null) return false;
        var scope=marketReleaseAccess.get(task.getBatchExecutionId(),actor);
        if(!scope.visible()) return false;
        if(row.getEventRevision()!=null && row.getEventRevision()>0) {
            return scope.original() && java.util.Objects.equals(scope.handoverId(),row.getEventRevision())
                    && (scope.active() ? "HANDED_OVER" : "RECALLED").equals(row.getEventType());
        }
        return java.util.Objects.equals(row.getUserId(),actor)
                && MesReleaseTaskNotificationContract.candidates(task.getCandidateUserSnapshot()).contains(actor);
    }

    public List<MesReleaseTaskNotifyDeliveryDO> listForTask(Long tenantId, Long actorId, Long workTaskId) {
        MesReleaseTaskNotificationContract.tenant(tenantId);
        var deliveries = deliveryMapper.selectTaskDeliveries(tenantId, workTaskId);
        var ownOutcomes = deliveries.stream().filter(row -> MesReleaseTaskNotificationContract.outcome(row.getEventType())
                && java.util.Objects.equals(row.getUserId(), actorId)).toList();
        if (!ownOutcomes.isEmpty() && permissionApi.hasAnyPermissions(actorId, MesReleaseTaskNotificationContract.QUERY_PERMISSION)) {
            return ownOutcomes;
        }
        MesProEdhrWorkTaskDO task = workTaskMapper.selectById(workTaskId);
        if(task!=null && task.getBatchExecutionId()!=null) marketReleaseAccess.requireReadable(task.getBatchExecutionId());
        boolean original=task!=null && task.getBatchExecutionId()!=null && marketReleaseAccess.get(task.getBatchExecutionId(),actorId).original();
        if(!original || !permissionApi.hasAnyPermissions(actorId,MesReleaseTaskNotificationContract.QUERY_PERMISSION))
            MesReleaseTaskNotificationContract.access(permissionApi, tenantId, actorId, task);
        return deliveries.stream().filter(row -> !MesReleaseTaskNotificationContract.outcome(row.getEventType())).toList();
    }
}
