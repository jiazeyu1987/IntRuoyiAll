package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;

public interface DccPublicationNotificationTransactionService {
    DccPublicationNotificationDeliveryDO beginAttempt(Long tenantId, Long deliveryId, Integer expectedVersion,
                                                       Long actorId, String reason, boolean retry);
    void markSent(Long tenantId, Long deliveryId, Integer expectedVersion, Long messageId,
                  Long actorId, String reason);
    void markFailed(Long tenantId, Long deliveryId, Integer expectedVersion, Long actorId,
                    String reason, String errorSummary);
}
