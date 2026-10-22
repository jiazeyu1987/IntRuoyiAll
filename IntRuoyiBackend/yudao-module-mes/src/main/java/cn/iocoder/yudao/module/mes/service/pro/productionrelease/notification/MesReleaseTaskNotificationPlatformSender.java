package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Separately commits the existing platform idempotent API before local delivery ACK. */
@Service
public class MesReleaseTaskNotificationPlatformSender {
    @Resource private NotifyMessageSendApi notifyMessageSendApi;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long send(NotifySendSingleToUserIdempotentReqDTO request) {
        Long messageId = notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(request);
        if (messageId == null || messageId <= 0) { throw new IllegalStateException("平台未返回正式站内信回执"); }
        return messageId;
    }
}
