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

    public List<MesReleaseTaskNotifyDeliveryDO> listForTask(Long tenantId, Long actorId, Long workTaskId) {
        MesReleaseTaskNotificationContract.tenant(tenantId);
        MesProEdhrWorkTaskDO task = workTaskMapper.selectById(workTaskId);
        MesReleaseTaskNotificationContract.access(permissionApi, tenantId, actorId, task);
        return deliveryMapper.selectTaskDeliveries(tenantId, workTaskId);
    }
}
