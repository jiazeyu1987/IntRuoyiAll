package cn.iocoder.yudao.module.bpm.service.message;

import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.bpm.convert.message.BpmMessageConvert;
import cn.iocoder.yudao.module.bpm.enums.message.BpmMessageEnum;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceApproveReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceRejectReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskTimeoutReqDTO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * BPM 消息 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
@Slf4j
public class BpmMessageServiceImpl implements BpmMessageService {

    static final String DCC_CONTROLLED_FILE_PROCESS_DEFINITION_KEY = "dcc-controlled-file-approval";
    static final String DCC_REGISTRATION_CERTIFICATE_PROCESS_DEFINITION_KEY =
            "dcc-registration-certificate-access";
    static final String EDHR_BATCH_RECORD_PROCESS_DEFINITION_KEY = "mes-edhr-approval-v1";
    static final String ROUTE_VERSION_PROCESS_DEFINITION_KEY = "mes-route-version-approval-v1";
    static final String DCC_TASK_ASSIGNED_NOTIFY_TEMPLATE_CODE = "dcc_task_assigned";
    static final String DCC_PROCESS_INSTANCE_APPROVE_NOTIFY_TEMPLATE_CODE = "dcc_controlled_file_approved";
    static final String DCC_PROCESS_INSTANCE_REJECT_NOTIFY_TEMPLATE_CODE = "dcc_controlled_file_rejected";
    static final String DCC_TASK_TIMEOUT_NOTIFY_TEMPLATE_CODE = "dcc_task_timeout";
    static final String EDHR_TASK_ASSIGNED_NOTIFY_TEMPLATE_CODE = "MES_EDHR_BPM_TASK_ASSIGNED";
    static final String EDHR_PROCESS_INSTANCE_APPROVE_NOTIFY_TEMPLATE_CODE = "MES_EDHR_BPM_APPROVED";
    static final String EDHR_PROCESS_INSTANCE_REJECT_NOTIFY_TEMPLATE_CODE = "MES_EDHR_BPM_REJECTED";
    static final String EDHR_TASK_TIMEOUT_NOTIFY_TEMPLATE_CODE = "MES_EDHR_BPM_TASK_TIMEOUT";
    static final String ROUTE_VERSION_TASK_ASSIGNED_NOTIFY_TEMPLATE_CODE = "MES_ROUTE_VERSION_BPM_TASK_ASSIGNED";
    static final String ROUTE_VERSION_PROCESS_INSTANCE_APPROVE_NOTIFY_TEMPLATE_CODE = "MES_ROUTE_VERSION_BPM_APPROVED";
    static final String ROUTE_VERSION_PROCESS_INSTANCE_REJECT_NOTIFY_TEMPLATE_CODE = "MES_ROUTE_VERSION_BPM_REJECTED";
    static final String ROUTE_VERSION_TASK_TIMEOUT_NOTIFY_TEMPLATE_CODE = "MES_ROUTE_VERSION_BPM_TASK_TIMEOUT";

    @Resource
    private SmsSendApi smsSendApi;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    @Resource
    private WebProperties webProperties;

    @Override
    public void sendMessageWhenProcessInstanceApprove(BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        String templateCode = resolveProcessApproveNotifyTemplateCode(reqDTO.getProcessDefinitionKey());
        if (templateCode != null) {
            sendNotifyMessage(reqDTO.getStartUserId(), templateCode, templateParams);
            return;
        }
        smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(reqDTO.getStartUserId(),
                BpmMessageEnum.PROCESS_INSTANCE_APPROVE.getSmsTemplateCode(), templateParams));
    }

    @Override
    public void sendMessageWhenProcessInstanceReject(BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("reason", reqDTO.getReason());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        String templateCode = resolveProcessRejectNotifyTemplateCode(reqDTO.getProcessDefinitionKey());
        if (templateCode != null) {
            sendNotifyMessage(reqDTO.getStartUserId(), templateCode, templateParams);
            return;
        }
        smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(reqDTO.getStartUserId(),
                BpmMessageEnum.PROCESS_INSTANCE_REJECT.getSmsTemplateCode(), templateParams));
    }

    @Override
    public void sendMessageWhenTaskAssigned(BpmMessageSendWhenTaskCreatedReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("taskName", reqDTO.getTaskName());
        templateParams.put("startUserNickname", reqDTO.getStartUserNickname());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        String templateCode = resolveTaskAssignedNotifyTemplateCode(reqDTO.getProcessDefinitionKey());
        if (templateCode != null) {
            sendNotifyMessage(reqDTO.getAssigneeUserId(), templateCode, templateParams);
            return;
        }
        smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(reqDTO.getAssigneeUserId(),
                BpmMessageEnum.TASK_ASSIGNED.getSmsTemplateCode(), templateParams));
    }

    @Override
    public void sendMessageWhenTaskTimeout(BpmMessageSendWhenTaskTimeoutReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("taskName", reqDTO.getTaskName());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        String templateCode = resolveTaskTimeoutNotifyTemplateCode(reqDTO.getProcessDefinitionKey());
        if (templateCode != null) {
            sendNotifyMessage(reqDTO.getAssigneeUserId(), templateCode, templateParams);
            return;
        }
        smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(reqDTO.getAssigneeUserId(),
                BpmMessageEnum.TASK_TIMEOUT.getSmsTemplateCode(), templateParams));
    }

    private void sendNotifyMessage(Long userId, String templateCode, Map<String, Object> templateParams) {
        NotifySendSingleToUserReqDTO notifyReqDTO = new NotifySendSingleToUserReqDTO();
        notifyReqDTO.setUserId(userId);
        notifyReqDTO.setTemplateCode(templateCode);
        notifyReqDTO.setTemplateParams(templateParams);
        notifyMessageSendApi.sendSingleMessageToAdmin(notifyReqDTO);
    }

    private String resolveProcessApproveNotifyTemplateCode(String processDefinitionKey) {
        if (isDccNotifyProcess(processDefinitionKey)) {
            return DCC_PROCESS_INSTANCE_APPROVE_NOTIFY_TEMPLATE_CODE;
        }
        if (EDHR_BATCH_RECORD_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return EDHR_PROCESS_INSTANCE_APPROVE_NOTIFY_TEMPLATE_CODE;
        }
        if (ROUTE_VERSION_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return ROUTE_VERSION_PROCESS_INSTANCE_APPROVE_NOTIFY_TEMPLATE_CODE;
        }
        return null;
    }

    private String resolveProcessRejectNotifyTemplateCode(String processDefinitionKey) {
        if (isDccNotifyProcess(processDefinitionKey)) {
            return DCC_PROCESS_INSTANCE_REJECT_NOTIFY_TEMPLATE_CODE;
        }
        if (EDHR_BATCH_RECORD_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return EDHR_PROCESS_INSTANCE_REJECT_NOTIFY_TEMPLATE_CODE;
        }
        if (ROUTE_VERSION_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return ROUTE_VERSION_PROCESS_INSTANCE_REJECT_NOTIFY_TEMPLATE_CODE;
        }
        return null;
    }

    private String resolveTaskAssignedNotifyTemplateCode(String processDefinitionKey) {
        if (isDccNotifyProcess(processDefinitionKey)) {
            return DCC_TASK_ASSIGNED_NOTIFY_TEMPLATE_CODE;
        }
        if (EDHR_BATCH_RECORD_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return EDHR_TASK_ASSIGNED_NOTIFY_TEMPLATE_CODE;
        }
        if (ROUTE_VERSION_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return ROUTE_VERSION_TASK_ASSIGNED_NOTIFY_TEMPLATE_CODE;
        }
        return null;
    }

    private String resolveTaskTimeoutNotifyTemplateCode(String processDefinitionKey) {
        if (isDccNotifyProcess(processDefinitionKey)) {
            return DCC_TASK_TIMEOUT_NOTIFY_TEMPLATE_CODE;
        }
        if (EDHR_BATCH_RECORD_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return EDHR_TASK_TIMEOUT_NOTIFY_TEMPLATE_CODE;
        }
        if (ROUTE_VERSION_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)) {
            return ROUTE_VERSION_TASK_TIMEOUT_NOTIFY_TEMPLATE_CODE;
        }
        return null;
    }

    private boolean isDccNotifyProcess(String processDefinitionKey) {
        return DCC_CONTROLLED_FILE_PROCESS_DEFINITION_KEY.equals(processDefinitionKey)
                || DCC_REGISTRATION_CERTIFICATE_PROCESS_DEFINITION_KEY.equals(processDefinitionKey);
    }

    private String getProcessInstanceDetailUrl(String taskId) {
        return webProperties.getAdminUi().getUrl() + "/bpm/process-instance/detail?id=" + taskId;
    }

}
