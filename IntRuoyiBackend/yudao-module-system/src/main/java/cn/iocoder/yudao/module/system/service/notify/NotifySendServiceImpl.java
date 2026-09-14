package cn.iocoder.yudao.module.system.service.notify;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 站内信发送 Service 实现类
 *
 * @author xrcoder
 */
@Service
@Validated
public class NotifySendServiceImpl implements NotifySendService {

    @Resource
    private NotifyTemplateService notifyTemplateService;

    @Resource
    private NotifyMessageService notifyMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSingleNotifyToAdmin(Long userId, String templateCode, Map<String, Object> templateParams) {
        return sendSingleNotify(userId, UserTypeEnum.ADMIN.getValue(), templateCode, templateParams);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSingleNotifyToAdminIdempotently(Long userId, String templateCode,
                                                     Map<String, Object> templateParams, String businessKey) {
        String normalizedBusinessKey = normalizeBusinessKey(businessKey);
        Map<String, Object> normalizedTemplateParams = normalizeTemplateParams(templateParams);

        NotifyMessageDO existing = notifyMessageService.getNotifyMessageByBusinessKey(normalizedBusinessKey);
        if (existing != null) {
            validateReplayBinding(existing, userId, UserTypeEnum.ADMIN.getValue(), templateCode,
                    normalizedTemplateParams, normalizedBusinessKey);
        }

        NotifyTemplateDO template = validateNotifyTemplate(templateCode);
        validateTemplateEnabled(template);
        validateTemplateParams(template, normalizedTemplateParams);
        if (existing != null) {
            return requireMessageId(existing.getId());
        }

        String content = notifyTemplateService.formatNotifyTemplateContent(
                template.getContent(), normalizedTemplateParams);
        try {
            Long messageId = notifyMessageService.createNotifyMessage(userId, UserTypeEnum.ADMIN.getValue(),
                    template, content, normalizedTemplateParams, normalizedBusinessKey);
            return requireMessageId(messageId);
        } catch (DuplicateKeyException exception) {
            NotifyMessageDO concurrent = notifyMessageService
                    .getNotifyMessageByBusinessKeyForUpdate(normalizedBusinessKey);
            if (concurrent == null) {
                throw exception;
            }
            return validateReplay(concurrent, userId, UserTypeEnum.ADMIN.getValue(), templateCode,
                    normalizedTemplateParams, normalizedBusinessKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSingleNotifyToMember(Long userId, String templateCode, Map<String, Object> templateParams) {
        return sendSingleNotify(userId, UserTypeEnum.MEMBER.getValue(), templateCode, templateParams);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendSingleNotify(Long userId, Integer userType, String templateCode, Map<String, Object> templateParams) {
        // 校验模版
        NotifyTemplateDO template = validateNotifyTemplate(templateCode);
        validateTemplateEnabled(template);
        // 校验参数
        validateTemplateParams(template, templateParams);

        // 发送站内信
        String content = notifyTemplateService.formatNotifyTemplateContent(template.getContent(), templateParams);
        return requireMessageId(notifyMessageService.createNotifyMessage(
                userId, userType, template, content, templateParams));
    }

    @VisibleForTesting
    public NotifyTemplateDO validateNotifyTemplate(String templateCode) {
        // 获得站内信模板。考虑到效率，从缓存中获取
        NotifyTemplateDO template = notifyTemplateService.getNotifyTemplateByCodeFromCache(templateCode);
        // 站内信模板不存在
        if (template == null) {
            throw exception(NOTIFY_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    @VisibleForTesting
    public void validateTemplateEnabled(NotifyTemplateDO template) {
        if (Objects.equals(template.getStatus(), CommonStatusEnum.DISABLE.getStatus())) {
            throw exception(NOTIFY_SEND_TEMPLATE_DISABLED);
        }
    }

    /**
     * 校验站内信模版参数是否确实
     *
     * @param template 邮箱模板
     * @param templateParams 参数列表
     */
    @VisibleForTesting
    public void validateTemplateParams(NotifyTemplateDO template, Map<String, Object> templateParams) {
        if (template.getParams() == null) {
            return;
        }
        template.getParams().forEach(key -> {
            Object value = templateParams == null ? null : templateParams.get(key);
            if (value == null) {
                throw exception(NOTIFY_SEND_TEMPLATE_PARAM_MISS, key);
            }
        });
    }

    private String normalizeBusinessKey(String businessKey) {
        if (businessKey == null) {
            throw exception(NOTIFY_SEND_BUSINESS_KEY_INVALID);
        }
        String normalized = businessKey.trim();
        if (normalized.isEmpty() || normalized.length() > 255) {
            throw exception(NOTIFY_SEND_BUSINESS_KEY_INVALID);
        }
        return normalized;
    }

    private Map<String, Object> normalizeTemplateParams(Map<String, Object> templateParams) {
        if (templateParams == null) {
            throw exception(NOTIFY_SEND_TEMPLATE_PARAMS_REQUIRED);
        }
        TreeMap<String, Object> normalized = new TreeMap<>();
        for (Map.Entry<String, Object> entry : templateParams.entrySet()) {
            if (entry.getKey() == null) {
                throw exception(NOTIFY_SEND_TEMPLATE_PARAMS_REQUIRED);
            }
            normalized.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(normalized);
    }

    private Long validateReplay(NotifyMessageDO existing, Long userId, Integer userType,
                                String templateCode, Map<String, Object> templateParams,
                                String businessKey) {
        validateReplayBinding(existing, userId, userType, templateCode, templateParams, businessKey);
        return requireMessageId(existing.getId());
    }

    private void validateReplayBinding(NotifyMessageDO existing, Long userId, Integer userType,
                                       String templateCode, Map<String, Object> templateParams,
                                       String businessKey) {
        if (!Objects.equals(existing.getBusinessKey(), businessKey)
                || !Objects.equals(existing.getUserId(), userId)
                || !Objects.equals(existing.getUserType(), userType)
                || !Objects.equals(existing.getTemplateCode(), templateCode)
                || !Objects.equals(existing.getTemplateParams(), templateParams)) {
            throw exception(NOTIFY_SEND_BUSINESS_KEY_CONFLICT, businessKey);
        }
    }

    private Long requireMessageId(Long messageId) {
        if (messageId == null) {
            throw exception(NOTIFY_SEND_MESSAGE_ID_EMPTY);
        }
        return messageId;
    }
}
