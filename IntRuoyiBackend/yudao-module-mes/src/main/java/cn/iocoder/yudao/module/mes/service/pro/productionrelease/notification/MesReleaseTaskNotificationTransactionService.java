package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/** Three public operations run through a separate Spring Bean proxy, never self-invocation. */
@Service
public class MesReleaseTaskNotificationTransactionService {
    @Resource private MesReleaseTaskNotifyDeliveryMapper deliveryMapper;
    @Resource private MesReleaseTaskNotificationAudit audit;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public MesReleaseTaskNotifyDeliveryDO beginAttempt(Long tenantId, Long deliveryId, Integer expectedVersion,
                                                       Long actorId, String reason, boolean retry) {
        validate(tenantId, actorId, reason);
        audit.lock();
        MesReleaseTaskNotifyDeliveryDO before = processable(tenantId, deliveryId, expectedVersion);
        if (deliveryMapper.beginAttempt(tenantId, deliveryId, expectedVersion, LocalDateTime.now()) != 1) {
            throw new IllegalStateException("通知投递尝试版本发生冲突");
        }
        MesReleaseTaskNotifyDeliveryDO after = require(tenantId, deliveryId);
        audit.attempted(before, after, reason, retry);
        return after;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markSent(Long tenantId, Long deliveryId, Integer expectedVersion, Long messageId,
                         Long actorId, String reason, boolean retry) {
        validate(tenantId, actorId, reason);
        if (messageId == null || messageId <= 0) { throw new IllegalArgumentException("平台消息正式回执缺失"); }
        audit.lock();
        MesReleaseTaskNotifyDeliveryDO before = processable(tenantId, deliveryId, expectedVersion);
        if (deliveryMapper.markSent(tenantId, deliveryId, expectedVersion, messageId, LocalDateTime.now()) != 1) {
            throw new IllegalStateException("通知成功回执版本发生冲突");
        }
        audit.sent(before, require(tenantId, deliveryId), reason, retry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long tenantId, Long deliveryId, Integer expectedVersion, Long actorId,
                           String reason, String errorSummary, boolean retry) {
        validate(tenantId, actorId, reason);
        if (MesReleaseTaskNotificationContract.blank(errorSummary) || errorSummary.length() > 512) {
            throw new IllegalArgumentException("通知失败必须保留有效脱敏原因");
        }
        audit.lock();
        MesReleaseTaskNotifyDeliveryDO before = processable(tenantId, deliveryId, expectedVersion);
        if (deliveryMapper.markFailed(tenantId, deliveryId, expectedVersion, errorSummary, LocalDateTime.now()) != 1) {
            throw new IllegalStateException("通知失败回执版本发生冲突");
        }
        audit.failed(before, require(tenantId, deliveryId), reason, retry);
    }

    private void validate(Long tenantId, Long actorId, String reason) {
        MesReleaseTaskNotificationContract.tenant(tenantId);
        MesReleaseTaskNotificationContract.reason(actorId, reason);
    }

    private MesReleaseTaskNotifyDeliveryDO processable(Long tenantId, Long id, Integer version) {
        MesReleaseTaskNotifyDeliveryDO row = require(tenantId, id);
        if (version == null || version < 0 || !Objects.equals(row.getRowVersion(), version)
                || !("PENDING".equals(row.getStatus()) || "FAILED".equals(row.getStatus()))) {
            throw new IllegalStateException("通知已发送或投递版本发生变化，请刷新正式回执");
        }
        return row;
    }

    private MesReleaseTaskNotifyDeliveryDO require(Long tenantId, Long id) {
        MesReleaseTaskNotifyDeliveryDO row = id == null ? null : deliveryMapper.selectDelivery(tenantId, id);
        if (row == null || !Objects.equals(tenantId, row.getTenantId())) {
            throw new IllegalArgumentException("当前租户的通知投递不存在");
        }
        return row;
    }
}
