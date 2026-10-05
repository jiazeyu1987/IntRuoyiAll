package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/** Deliberately has no encompassing transaction: attempt, platform and ACK commit independently. */
@Service
@Slf4j
public class MesReleaseTaskNotificationDispatchService {
    @Resource private MesReleaseTaskNotifyDeliveryMapper deliveryMapper;
    @Resource private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource private MesReleaseTaskNotificationTransactionService transactionService;
    @Resource private MesReleaseTaskNotificationPlatformSender platformSender;
    @Resource private PermissionApi permissionApi;

    public void dispatchAssigned(Long tenantId, Long workTaskId) {
        MesReleaseTaskNotificationContract.tenant(tenantId);
        for (MesReleaseTaskNotifyDeliveryDO row : deliveryMapper.selectTaskDeliveries(tenantId, workTaskId)) {
            if (!"PENDING".equals(row.getStatus())) { continue; }
            try {
                dispatch(row, row.getInitiatedBy(), MesReleaseTaskNotificationAudit.AUTOMATIC_REASON, false);
            } catch (RuntimeException failure) {
                // Durable state and explicit log retain the failure without reversing the committed business action.
                log.error("Release notification remains recoverable: tenant={}, task={}, delivery={}, failure={}",
                        tenantId, workTaskId, row.getId(), MesReleaseTaskNotificationContract.safeError(failure));
            }
        }
    }

    public void retryDelivery(Long tenantId, Long actorId, Long deliveryId, Integer expectedVersion, String reason) {
        MesReleaseTaskNotificationContract.tenant(tenantId);
        MesReleaseTaskNotificationContract.reason(actorId, reason);
        MesReleaseTaskNotifyDeliveryDO row = deliveryMapper.selectDelivery(tenantId, deliveryId);
        if (row == null) { throw new IllegalArgumentException("当前租户的正式通知投递不存在"); }
        MesProEdhrWorkTaskDO task = workTaskMapper.selectById(row.getWorkTaskId());
        MesReleaseTaskNotificationContract.access(permissionApi, tenantId, actorId, task);
        if (!Objects.equals(row.getUserId(), actorId)) {
            throw new ServiceException(1_076_040_002, "仅原冻结候选收件人可重试本人的通知");
        }
        if (!Objects.equals(expectedVersion, row.getRowVersion())
                || !("PENDING".equals(row.getStatus()) || "FAILED".equals(row.getStatus()))) {
            throw new ServiceException(1_076_040_003, "通知已成功或版本已变化，请刷新投递回执");
        }
        dispatch(row, actorId, reason.trim(), true);
    }

    private void dispatch(MesReleaseTaskNotifyDeliveryDO frozen, Long actor, String reason, boolean retry) {
        MesReleaseTaskNotifyDeliveryDO attempt = transactionService.recordAttempt(
                frozen.getTenantId(), frozen.getId(), frozen.getRowVersion(), actor, reason, retry);
        try {
            NotifySendSingleToUserIdempotentReqDTO request = new NotifySendSingleToUserIdempotentReqDTO();
            request.setUserId(attempt.getUserId()); request.setTemplateCode(attempt.getTemplateCode());
            request.setBusinessKey(attempt.getBusinessKey());
            Map<String, Object> parameters = JsonUtils.parseObject(attempt.getTemplateParamsJson(), Map.class);
            if (parameters == null || parameters.isEmpty()
                    || parameters.values().stream().anyMatch(value -> !(value instanceof String))) {
                throw new IllegalStateException("正式通知冻结参数必须保持字符串类型");
            }
            request.setTemplateParams(parameters);
            Long messageId = platformSender.send(request);
            transactionService.recordSent(attempt.getTenantId(), attempt.getId(), attempt.getRowVersion(),
                    messageId, actor, reason, retry);
        } catch (RuntimeException failure) {
            String summary = MesReleaseTaskNotificationContract.safeError(failure);
            try {
                transactionService.recordFailed(attempt.getTenantId(), attempt.getId(), attempt.getRowVersion(),
                        actor, reason, summary, retry);
            } catch (RuntimeException ackFailure) {
                // A failed CAS/audit cannot overwrite a newer SENT. Existing attempt remains queryable/retryable.
                log.error("Release notification failure ACK rejected: tenant={}, delivery={}, failure={}",
                        attempt.getTenantId(), attempt.getId(), MesReleaseTaskNotificationContract.safeError(ackFailure));
                failure.addSuppressed(ackFailure);
            }
            throw new ServiceException(1_076_040_004, "通知投递未完成，请查询持久化回执后重试（" + summary + "）");
        }
    }
}
