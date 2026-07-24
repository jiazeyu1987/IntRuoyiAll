package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import java.util.Map;

public interface RuntimeOpsSiteMessageSender {

    Long sendSingleMessageToAdmin(Long userId, String templateCode, Map<String, Object> templateParams);
}
