package cn.iocoder.yudao.module.dcc.service.file;

public interface DccPublicationNotificationService {
    void materializeForPublicationBatch(Long batchId);
    void retryDelivery(Long actorId, Long deliveryId, Integer expectedVersion, String reason);
}
