package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;

@Service
public class RuntimeOpsNotifyMessageSender implements RuntimeOpsSiteMessageSender {

    private static final String NOTIFY_API_CLASS = "cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi";
    private static final String NOTIFY_DTO_CLASS = "cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO";

    private final ApplicationContext applicationContext;

    public RuntimeOpsNotifyMessageSender(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Long sendSingleMessageToAdmin(Long userId, String templateCode, Map<String, Object> templateParams) {
        try {
            Class<?> apiType = Class.forName(NOTIFY_API_CLASS);
            Class<?> dtoType = Class.forName(NOTIFY_DTO_CLASS);
            Object api = applicationContext.getBean(apiType);
            Object reqDTO = dtoType.getDeclaredConstructor().newInstance();
            dtoType.getMethod("setUserId", Long.class).invoke(reqDTO, userId);
            dtoType.getMethod("setTemplateCode", String.class).invoke(reqDTO, templateCode);
            dtoType.getMethod("setTemplateParams", Map.class).invoke(reqDTO, templateParams);
            Object result = apiType.getMethod("sendSingleMessageToAdmin", dtoType).invoke(api, reqDTO);
            if (!(result instanceof Long messageId)) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "站内信发送失败：NotifyMessageSendApi 返回空消息编号");
            }
            return messageId;
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getTargetException();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "站内信发送失败：" + cause.getMessage());
        } catch (ClassNotFoundException | BeansException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "站内信 API 缺失：" + ex.getMessage());
        } catch (ReflectiveOperationException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "站内信 API 调用失败：" + ex.getMessage());
        }
    }
}
