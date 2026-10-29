package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Objects;

/** Result-only notices use the original application identity, never approval candidates or new tasks. */
@Service
@Slf4j
public class MesReleaseOutcomeNotificationService {
    @Resource private MesReleaseTaskNotifyDeliveryMapper deliveryMapper;
    @Resource private MesReleaseTaskNotificationDispatchService dispatchService;
    @Resource private MesReleaseTaskNotificationAudit audit;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void schedulePqc(MesProcessPoolActiveOrderReleaseApplicationDO application,
                            MesProEdhrWorkTaskDO task, Long actor, boolean approved, String reason) {
        if (application == null || task == null
                || !Objects.equals(application.getPqcReleaseWorkTaskId(), task.getId())
                || !Objects.equals(application.getId(), task.getBusinessScopeId())
                || !"RELEASE_APPLICATION".equals(task.getBusinessScopeType())) {
            throw new IllegalArgumentException("PQC结果通知必须关联原正式申请及任务");
        }
        schedule(task, actor, application.getAppliedBy(), approved ? "PQC_APPROVED" : "PQC_REJECTED", reason);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void scheduleMarketApproved(MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrReleaseTransactionDO transaction, MesProEdhrWorkTaskDO task, Long actor, String reason) {
        if (application == null || transaction == null || task == null
                || !Objects.equals(application.getReleaseTransactionId(), transaction.getId())
                || !Objects.equals(application.getReleaseApprovalWorkTaskId(), task.getId())
                || !Objects.equals(task.getBusinessScopeId(), transaction.getId())
                || !"RELEASE_TRANSACTION".equals(task.getBusinessScopeType())
                || !"RELEASED".equals(transaction.getReleaseStatus())) {
            throw new IllegalArgumentException("上市放行结果必须关联已批准事务、原正式申请及任务");
        }
        schedule(task, actor, application.getAppliedBy(), "MARKET_APPROVED", reason);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void scheduleMarketNegative(MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrReleaseTransactionDO transaction, MesProEdhrWorkTaskDO task, Long actor,
            boolean rejected, String reason) {
        if (application == null || transaction == null || task == null
                || !Objects.equals(application.getReleaseTransactionId(), transaction.getId())
                || !Objects.equals(application.getReleaseApprovalWorkTaskId(), task.getId())
                || !Objects.equals(task.getBusinessScopeId(), transaction.getId())
                || !"RELEASE_TRANSACTION".equals(task.getBusinessScopeType())
                || !(rejected ? "REJECTED" : "WITHDRAWN").equals(transaction.getReleaseStatus()))
            throw new IllegalArgumentException("上市负向结果必须关联原正式申请、终态事务及任务");
        String event = rejected ? "MARKET_REJECTED" : "MARKET_WITHDRAWN";
        schedule(task, actor, application.getAppliedBy(), event, reason);
        if (!rejected) {
            var recipients = task.getAssigneeUserId() != null ? java.util.List.of(task.getAssigneeUserId())
                    : MesReleaseTaskNotificationContract.candidates(task.getCandidateUserSnapshot());
            for (Long recipient : recipients)
                if (!Objects.equals(recipient, application.getAppliedBy())) schedule(task, actor, recipient, event, reason);
        }
    }

    private void schedule(MesProEdhrWorkTaskDO task, Long actor, Long recipient, String event, String reason) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("放行结果通知必须加入正式决策事务");
        }
        Long tenant = TenantContextHolder.getRequiredTenantId();
        MesReleaseTaskNotificationContract.tenant(tenant);
        MesReleaseTaskNotificationContract.taskTemplate(task);
        if (actor == null || actor <= 0 || recipient == null || recipient <= 0) {
            throw new IllegalArgumentException("放行结果必须有真实决策人与原申请人");
        }
        String key = "MES_EDHR_RELEASE_OUTCOME:" + event + ":TASK:" + task.getId() + ":USER:" + recipient;
        audit.lock();
        if (deliveryMapper.selectTaskDeliveries(tenant, task.getId()).stream()
                .anyMatch(row -> key.equals(row.getBusinessKey()))) {
            return;
        }
        var params = new LinkedHashMap<String, String>();
        params.put("workOrderCode", task.getWorkOrderCode());
        params.put("batchCode", task.getBatchCode());
        params.put("workTaskId", task.getId().toString());
        params.put("reason", reason == null ? "" : reason);
        params.put("actionUrl", "/user/profile");
        var row = new MesReleaseTaskNotifyDeliveryDO().setTenantId(tenant).setWorkTaskId(task.getId())
                .setEventType(event).setEventRevision(0L).setUserId(recipient).setBusinessKey(key)
                .setTemplateCode("MES_EDHR_RELEASE_" + event).setTemplateParamsJson(JsonUtils.toJsonString(params))
                .setInitiatedBy(actor).setStatus("PENDING").setAttemptCount(0).setRowVersion(0);
        if (deliveryMapper.insert(row) != 1 || row.getId() == null) {
            throw new IllegalStateException("放行结果通知未完整持久化");
        }
        audit.outcomeCreated(row);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                Long previous = TenantContextHolder.getTenantId();
                try {
                    TenantContextHolder.setTenantId(tenant);
                    dispatchService.dispatchAssigned(tenant, task.getId());
                } catch (RuntimeException failure) {
                    log.error("Release outcome notification remains recoverable: tenant={}, task={}, failure={}",
                            tenant, task.getId(), MesReleaseTaskNotificationContract.safeError(failure));
                } finally {
                    if (previous == null) TenantContextHolder.clear();
                    else TenantContextHolder.setTenantId(previous);
                }
            }
        });
    }
}
