package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import org.springframework.stereotype.Service;

/** Historical receipt endpoints reject delivery after eDHR inbox notifications were retired. */
@Service
public class MesReleaseTaskNotificationDispatchService {
    public void dispatchAssigned(Long tenantId, Long workTaskId) {
        throw new IllegalStateException("eDHR站内信通知已停用，禁止发送历史通知");
    }

    public void retryDelivery(Long tenantId, Long actorId, Long deliveryId, Integer expectedVersion, String reason) {
        throw new IllegalStateException("eDHR站内信通知已停用，禁止补发历史通知");
    }
}
