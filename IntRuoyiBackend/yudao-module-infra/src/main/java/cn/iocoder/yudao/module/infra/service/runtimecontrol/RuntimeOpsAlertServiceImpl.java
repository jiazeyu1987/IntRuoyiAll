package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixRespVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;

@Service
public class RuntimeOpsAlertServiceImpl implements RuntimeOpsAlertService {

    private final RuntimeOpsAlertStore alertStore;
    private final RuntimeOpsResponsibilityService responsibilityService;
    private final RuntimeOpsSiteMessageSender siteMessageSender;

    public RuntimeOpsAlertServiceImpl(RuntimeOpsAlertStore alertStore,
                                      RuntimeOpsResponsibilityService responsibilityService,
                                      RuntimeOpsSiteMessageSender siteMessageSender) {
        this.alertStore = alertStore;
        this.responsibilityService = responsibilityService;
        this.siteMessageSender = siteMessageSender;
    }

    @Override
    public PageResult<RuntimeControlAlertRespVO> getAlertsPage(RuntimeControlAlertPageReqVO pageReqVO) {
        return alertStore.page(pageReqVO);
    }

    @Override
    public RuntimeControlAlertRespVO createAlert(RuntimeControlAlertCreateReqVO reqVO) {
        RuntimeControlAlertRespVO alert = new RuntimeControlAlertRespVO();
        alert.setEnvironment(StrUtil.trim(reqVO.getEnvironment()));
        alert.setAction(StrUtil.trim(reqVO.getAction()));
        alert.setSeverity(StrUtil.trim(reqVO.getSeverity()));
        alert.setTitle(StrUtil.trim(reqVO.getTitle()));
        alert.setContent(StrUtil.trim(reqVO.getContent()));
        alert.setNotifyTemplateCode(StrUtil.trim(reqVO.getNotifyTemplateCode()));
        alert.setTemplateParams(resolveTemplateParams(reqVO));
        alert.setCreatedAt(LocalDateTime.now());
        return deliverOrBlock(alert);
    }

    @Override
    public RuntimeControlAlertRespVO resendSiteMessage(Long id) {
        RuntimeControlAlertRespVO alert = requireAlert(id);
        alert.setNotifyMessageId(null);
        alert.setSiteMessageFailureReason(null);
        alert.setSentAt(null);
        return deliverOrBlock(alert);
    }

    @Override
    public RuntimeControlAlertRespVO acknowledge(Long id, String acknowledgedBy) {
        RuntimeControlAlertRespVO alert = requireAlert(id);
        responsibilityService.validateRequiredOwners(alert.getEnvironment(), alert.getAction());
        requireText(acknowledgedBy, "acknowledgedBy");
        alert.setAcknowledgedBy(StrUtil.trim(acknowledgedBy));
        alert.setAcknowledgedAt(LocalDateTime.now());
        return alertStore.save(alert);
    }

    private RuntimeControlAlertRespVO deliverOrBlock(RuntimeControlAlertRespVO alert) {
        if (StrUtil.isBlank(alert.getNotifyTemplateCode())) {
            alert.setSiteMessageStatus(RuntimeControlSiteMessageStatus.BLOCKED);
            alert.setSiteMessageFailureReason("站内信模板缺失：" + alert.getEnvironment() + "/" + alert.getAction());
            return alertStore.save(alert);
        }
        String missingOwnerReason = responsibilityService.findMissingRequiredOwnerReason(alert.getEnvironment(), alert.getAction());
        if (missingOwnerReason != null) {
            alert.setSiteMessageStatus(RuntimeControlSiteMessageStatus.BLOCKED);
            alert.setSiteMessageFailureReason(missingOwnerReason);
            return alertStore.save(alert);
        }
        RuntimeControlAlertRespVO persistedAlert = alertStore.save(alert);
        try {
            Long messageId = null;
            List<RuntimeControlOwnerMatrixRespVO> owners = responsibilityService.getRequiredOwners(
                    persistedAlert.getEnvironment(), persistedAlert.getAction());
            for (RuntimeControlOwnerMatrixRespVO owner : owners) {
                messageId = siteMessageSender.sendSingleMessageToAdmin(owner.getOwnerUserId(),
                        persistedAlert.getNotifyTemplateCode(), persistedAlert.getTemplateParams());
                if (messageId == null) {
                    throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "站内信发送失败：消息编号为空");
                }
            }
            persistedAlert.setNotifyMessageId(messageId);
            persistedAlert.setSiteMessageStatus(RuntimeControlSiteMessageStatus.SENT);
            persistedAlert.setSiteMessageFailureReason(null);
            persistedAlert.setSentAt(LocalDateTime.now());
            return alertStore.save(persistedAlert);
        } catch (RuntimeException ex) {
            persistedAlert.setSiteMessageStatus(RuntimeControlSiteMessageStatus.FAILED);
            persistedAlert.setSiteMessageFailureReason(resolveFailureReason(ex));
            alertStore.save(persistedAlert);
            if (ex instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "站内信发送失败：" + resolveFailureReason(ex));
        }
    }

    private RuntimeControlAlertRespVO requireAlert(Long id) {
        RuntimeControlAlertRespVO alert = alertStore.findById(id);
        if (alert == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "告警不存在：" + id);
        }
        return alert;
    }

    private void requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, fieldName);
        }
    }

    private Map<String, Object> resolveTemplateParams(RuntimeControlAlertCreateReqVO reqVO) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (reqVO.getTemplateParams() != null) {
            params.putAll(reqVO.getTemplateParams());
        }
        params.putIfAbsent("environment", reqVO.getEnvironment());
        params.putIfAbsent("action", reqVO.getAction());
        params.putIfAbsent("title", reqVO.getTitle());
        params.putIfAbsent("content", reqVO.getContent());
        params.putIfAbsent("severity", reqVO.getSeverity());
        return params;
    }

    private String resolveFailureReason(RuntimeException ex) {
        return StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
    }
}
