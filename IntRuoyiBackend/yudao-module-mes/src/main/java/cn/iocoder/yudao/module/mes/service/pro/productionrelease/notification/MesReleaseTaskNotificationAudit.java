package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

/** Uses the existing unified ledger; no second audit table or invented signing identity. */
@Component
public class MesReleaseTaskNotificationAudit {
    static final String AUTOMATIC_REASON = "正式放行任务提交后自动投递站内信";
    @Resource private GxpAuditService gxpAuditService;

    public void lock() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("通知状态与统一审计必须处于同一事务");
        }
        gxpAuditService.acquireLedgerLock();
    }

    public void created(MesReleaseTaskNotifyDeliveryDO after) {
        append("created", MesReleaseTaskNotificationService.class.getName() + "#scheduleAssigned",
                null, after, "冻结正式放行任务候选通知意图", false);
    }

    public void attempted(MesReleaseTaskNotifyDeliveryDO before, MesReleaseTaskNotifyDeliveryDO after,
                          String reason, boolean retry) {
        append("attempted", MesReleaseTaskNotificationTransactionService.class.getName() + "#beginAttempt",
                before, after, reason, retry);
    }

    public void sent(MesReleaseTaskNotifyDeliveryDO before, MesReleaseTaskNotifyDeliveryDO after,
                     String reason, boolean retry) {
        append("sent", MesReleaseTaskNotificationTransactionService.class.getName() + "#markSent",
                before, after, reason, retry);
    }

    public void failed(MesReleaseTaskNotifyDeliveryDO before, MesReleaseTaskNotifyDeliveryDO after,
                       String reason, boolean retry) {
        append("failed", MesReleaseTaskNotificationTransactionService.class.getName() + "#markFailed",
                before, after, reason, retry);
    }

    private void append(String action, String source, MesReleaseTaskNotifyDeliveryDO before,
                        MesReleaseTaskNotifyDeliveryDO after, String reason, boolean userReason) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("通知状态审计缺少活动事务");
        }
        MesReleaseTaskNotificationContract.tenant(after.getTenantId());
        gxpAuditService.append(GxpAuditCommand.builder()
                .operationId("mes.release-task-notification." + action)
                .subjectId(after.getId().toString()).subjectVersion(after.getRowVersion().toString())
                .beforeState(envelope(before)).afterState(envelope(after))
                .reason(reason).reasonSource(userReason ? "USER" : "SYSTEM")
                .reasonCode(userReason ? "RELEASE_NOTIFICATION_RETRY" : "RELEASE_NOTIFICATION_DISPATCH")
                .resultStatus("SUCCESS")
                .idempotencyKey("MES_NOTIFY:" + after.getId() + ":" + action + ":" + after.getRowVersion())
                .sourceType("SERVICE_METHOD").sourceLocator(source)
                .links(List.of(new GxpAuditRelation("SUBJECT", "MES_RELEASE_NOTIFY_DELIVERY",
                                after.getId().toString(), after.getRowVersion().toString(), null),
                        new GxpAuditRelation("SOURCE", "MES_WORK_TASK", after.getWorkTaskId().toString(), null, null)))
                .build());
    }

    private GxpAuditStateEnvelope envelope(MesReleaseTaskNotifyDeliveryDO row) {
        return GxpAuditStateEnvelope.builder().state(row == null ? "ABSENT" : row.getStatus())
                .objectVersion(row == null ? "0" : row.getRowVersion().toString())
                .canonicalJson(JsonUtils.toJsonString(row == null ? Map.of() : row)).build();
    }
}
