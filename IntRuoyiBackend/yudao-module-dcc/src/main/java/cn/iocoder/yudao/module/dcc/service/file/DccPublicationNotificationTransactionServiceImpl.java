package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_STATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_VERSION_CONFLICT;

@Service
public class DccPublicationNotificationTransactionServiceImpl implements DccPublicationNotificationTransactionService {
    @Resource private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Resource private DccPublicationNotificationAuditMapper auditMapper;
    @Resource private DccPublicationFollowupStatusService statusService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public DccPublicationNotificationDeliveryDO beginAttempt(Long tenantId, Long deliveryId,
                                                               Integer expectedVersion, Long actorId,
                                                               String reason, boolean retry) {
        DccPublicationNotificationDeliveryDO before = require(tenantId, deliveryId);
        if (!"PENDING".equals(before.getStatus()) && !"FAILED".equals(before.getStatus())) {
            throw exception(PUBLICATION_NOTIFICATION_STATE_INVALID);
        }
        if (deliveryMapper.beginAttempt(tenantId, deliveryId, expectedVersion, LocalDateTime.now(), actorId) != 1) {
            throw exception(PUBLICATION_NOTIFICATION_VERSION_CONFLICT);
        }
        DccPublicationNotificationDeliveryDO after = require(tenantId, deliveryId);
        auditMapper.insert(audit(after, retry ? "RETRY" : "ATTEMPT", actorId, reason,
                before.getStatus(), after.getStatus(), before.getAttemptCount(), after.getAttemptCount(), null,
                null, before.getRowVersion(), after.getRowVersion()));
        return after;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long tenantId, Long deliveryId, Integer expectedVersion, Long messageId,
                         Long actorId, String reason) {
        DccPublicationNotificationDeliveryDO before = require(tenantId, deliveryId);
        statusService.lockBatch(tenantId, before.getBatchId());
        before = require(tenantId, deliveryId);
        LocalDateTime now = LocalDateTime.now();
        if (deliveryMapper.markSent(tenantId, deliveryId, expectedVersion, messageId, now, actorId) != 1) {
            throw exception(PUBLICATION_NOTIFICATION_VERSION_CONFLICT);
        }
        DccPublicationNotificationDeliveryDO after = require(tenantId, deliveryId);
        auditMapper.insert(audit(after, "SENT", actorId, reason, before.getStatus(), after.getStatus(),
                before.getAttemptCount(), after.getAttemptCount(), messageId, null,
                before.getRowVersion(), after.getRowVersion()));
        statusService.refreshBatchStatus(tenantId, after.getBatchId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long tenantId, Long deliveryId, Integer expectedVersion, Long actorId,
                           String reason, String errorSummary) {
        DccPublicationNotificationDeliveryDO before = require(tenantId, deliveryId);
        statusService.lockBatch(tenantId, before.getBatchId());
        before = require(tenantId, deliveryId);
        LocalDateTime now = LocalDateTime.now();
        if (deliveryMapper.markFailed(tenantId, deliveryId, expectedVersion,
                StrUtil.subWithLength(errorSummary, 0, 512), now, actorId) != 1) {
            throw exception(PUBLICATION_NOTIFICATION_VERSION_CONFLICT);
        }
        DccPublicationNotificationDeliveryDO after = require(tenantId, deliveryId);
        auditMapper.insert(audit(after, "FAILED", actorId, reason, before.getStatus(), after.getStatus(),
                before.getAttemptCount(), after.getAttemptCount(), null, errorSummary,
                before.getRowVersion(), after.getRowVersion()));
        statusService.refreshBatchStatus(tenantId, after.getBatchId());
    }

    private DccPublicationNotificationDeliveryDO require(Long tenantId, Long deliveryId) {
        DccPublicationNotificationDeliveryDO row = deliveryMapper.selectById(deliveryId);
        if (row == null || !tenantId.equals(row.getTenantId())) throw exception(PUBLICATION_NOTIFICATION_NOT_EXISTS);
        return row;
    }

    private DccPublicationNotificationAuditDO audit(DccPublicationNotificationDeliveryDO row, String action,
                                                     Long actor, String reason, String before, String after,
                                                     Integer attemptBefore, Integer attemptAfter, Long messageId,
                                                     String error, Integer versionBefore, Integer versionAfter) {
        DccPublicationNotificationAuditDO audit = DccPublicationNotificationAuditDO.builder()
                .deliveryId(row.getId()).batchId(row.getBatchId()).actionType(action).actorId(actor).reason(reason)
                .statusBefore(before).statusAfter(after).attemptCount(attemptAfter).systemMessageId(messageId)
                .errorSummary(error).rowVersionBefore(versionBefore).rowVersionAfter(versionAfter)
                .occurredAt(LocalDateTime.now()).build();
        audit.setTenantId(row.getTenantId());
        return audit;
    }
}
