package cn.iocoder.yudao.module.bpm.service.message;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceApproveReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceRejectReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskTimeoutReqDTO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import cn.iocoder.yudao.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmMessageServiceImplTest extends BaseMockitoUnitTest {

    private static final String DCC_PROCESS_DEFINITION_KEY = "dcc-controlled-file-approval";
    private static final String DCC_REGISTRATION_CERTIFICATE_PROCESS_DEFINITION_KEY =
            "dcc-registration-certificate-access";
    private static final String EDHR_PROCESS_DEFINITION_KEY = "mes-edhr-approval-v1";
    private static final String ROUTE_VERSION_PROCESS_DEFINITION_KEY = "mes-route-version-approval-v1";

    @Mock
    private SmsSendApi smsSendApi;
    @Mock
    private NotifyMessageSendApi notifyMessageSendApi;

    @InjectMocks
    private BpmMessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        WebProperties webProperties = new WebProperties();
        WebProperties.Ui ui = new WebProperties.Ui();
        ui.setUrl("http://127.0.0.1:8081");
        webProperties.setAdminUi(ui);
        org.springframework.test.util.ReflectionTestUtils.setField(messageService, "webProperties", webProperties);
    }

    @Test
    void sendMessageWhenTaskAssigned_dccProcess_usesNotifyInbox() {
        BpmMessageSendWhenTaskCreatedReqDTO reqDTO = new BpmMessageSendWhenTaskCreatedReqDTO();
        reqDTO.setProcessInstanceId("proc-1");
        reqDTO.setProcessInstanceName("DCC受控文件流程");
        reqDTO.setProcessDefinitionKey(DCC_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(99L);
        reqDTO.setStartUserNickname("提交人");
        reqDTO.setTaskId("task-1");
        reqDTO.setTaskName("审批矩阵审核");
        reqDTO.setAssigneeUserId(100L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9001L);

        messageService.sendMessageWhenTaskAssigned(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(100L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_task_assigned", notifyCaptor.getValue().getTemplateCode());
        assertEquals("DCC受控文件流程", notifyCaptor.getValue().getTemplateParams().get("processInstanceName"));
        assertEquals("审批矩阵审核", notifyCaptor.getValue().getTemplateParams().get("taskName"));
        assertTrue(String.valueOf(notifyCaptor.getValue().getTemplateParams().get("detailUrl"))
                .contains("/bpm/process-instance/detail?id=proc-1"));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenTaskAssigned_registrationCertificateProcess_usesNotifyInbox() {
        BpmMessageSendWhenTaskCreatedReqDTO reqDTO = new BpmMessageSendWhenTaskCreatedReqDTO();
        reqDTO.setProcessInstanceId("proc-reg-cert-1");
        reqDTO.setProcessInstanceName("注册证变更审批");
        reqDTO.setProcessDefinitionKey(DCC_REGISTRATION_CERTIFICATE_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(99L);
        reqDTO.setStartUserNickname("提交人");
        reqDTO.setTaskId("task-reg-cert-1");
        reqDTO.setTaskName("注册证审批");
        reqDTO.setAssigneeUserId(100L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9301L);

        messageService.sendMessageWhenTaskAssigned(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(100L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_task_assigned", notifyCaptor.getValue().getTemplateCode());
        assertEquals("注册证变更审批", notifyCaptor.getValue().getTemplateParams().get("processInstanceName"));
        assertEquals("注册证审批", notifyCaptor.getValue().getTemplateParams().get("taskName"));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenTaskAssigned_nonDccProcess_keepsSmsPath() {
        BpmMessageSendWhenTaskCreatedReqDTO reqDTO = new BpmMessageSendWhenTaskCreatedReqDTO();
        reqDTO.setProcessInstanceId("proc-2");
        reqDTO.setProcessInstanceName("其他流程");
        reqDTO.setProcessDefinitionKey("leave-process");
        reqDTO.setStartUserId(88L);
        reqDTO.setStartUserNickname("申请人");
        reqDTO.setTaskId("task-2");
        reqDTO.setTaskName("部门负责人审批");
        reqDTO.setAssigneeUserId(101L);

        messageService.sendMessageWhenTaskAssigned(reqDTO);

        ArgumentCaptor<SmsSendSingleToUserReqDTO> smsCaptor =
                ArgumentCaptor.forClass(SmsSendSingleToUserReqDTO.class);
        verify(smsSendApi).sendSingleSmsToAdmin(smsCaptor.capture());
        assertEquals(101L, smsCaptor.getValue().getUserId());
        assertEquals("bpm_task_assigned", smsCaptor.getValue().getTemplateCode());
        verify(notifyMessageSendApi, never()).sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenProcessInstanceApprove_dccProcess_usesNotifyInbox() {
        BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO = new BpmMessageSendWhenProcessInstanceApproveReqDTO();
        reqDTO.setProcessInstanceId("proc-3");
        reqDTO.setProcessInstanceName("DCC审批通过");
        reqDTO.setProcessDefinitionKey(DCC_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(77L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9002L);

        messageService.sendMessageWhenProcessInstanceApprove(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(77L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_controlled_file_approved", notifyCaptor.getValue().getTemplateCode());
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenProcessInstanceApprove_registrationCertificateProcess_usesNotifyInbox() {
        BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO =
                new BpmMessageSendWhenProcessInstanceApproveReqDTO();
        reqDTO.setProcessInstanceId("proc-reg-cert-2");
        reqDTO.setProcessInstanceName("注册证变更审批");
        reqDTO.setProcessDefinitionKey(DCC_REGISTRATION_CERTIFICATE_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(77L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9302L);

        messageService.sendMessageWhenProcessInstanceApprove(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(77L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_controlled_file_approved", notifyCaptor.getValue().getTemplateCode());
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenProcessInstanceReject_registrationCertificateProcess_usesNotifyInbox() {
        BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO = new BpmMessageSendWhenProcessInstanceRejectReqDTO();
        reqDTO.setProcessInstanceId("proc-reg-cert-3");
        reqDTO.setProcessInstanceName("注册证变更审批");
        reqDTO.setProcessDefinitionKey(DCC_REGISTRATION_CERTIFICATE_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(77L);
        reqDTO.setReason("资料需补充");
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9303L);

        messageService.sendMessageWhenProcessInstanceReject(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(77L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_controlled_file_rejected", notifyCaptor.getValue().getTemplateCode());
        assertEquals("资料需补充", notifyCaptor.getValue().getTemplateParams().get("reason"));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenTaskTimeout_registrationCertificateProcess_usesNotifyInbox() {
        BpmMessageSendWhenTaskTimeoutReqDTO reqDTO = new BpmMessageSendWhenTaskTimeoutReqDTO();
        reqDTO.setProcessInstanceId("proc-reg-cert-4");
        reqDTO.setProcessInstanceName("注册证变更审批");
        reqDTO.setProcessDefinitionKey(DCC_REGISTRATION_CERTIFICATE_PROCESS_DEFINITION_KEY);
        reqDTO.setTaskId("task-reg-cert-4");
        reqDTO.setTaskName("注册证审批");
        reqDTO.setAssigneeUserId(100L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9304L);

        messageService.sendMessageWhenTaskTimeout(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(100L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_task_timeout", notifyCaptor.getValue().getTemplateCode());
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenTaskAssigned_edhrProcess_usesNotifyInbox() {
        BpmMessageSendWhenTaskCreatedReqDTO reqDTO = new BpmMessageSendWhenTaskCreatedReqDTO();
        reqDTO.setProcessInstanceId("proc-edhr-1");
        reqDTO.setProcessInstanceName("eDHR Approval V1");
        reqDTO.setProcessDefinitionKey(EDHR_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(611L);
        reqDTO.setStartUserNickname("human");
        reqDTO.setTaskId("task-edhr-1");
        reqDTO.setTaskName("eDHR审批");
        reqDTO.setAssigneeUserId(916L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9101L);

        messageService.sendMessageWhenTaskAssigned(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(916L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_EDHR_BPM_TASK_ASSIGNED", notifyCaptor.getValue().getTemplateCode());
        assertEquals("eDHR Approval V1", notifyCaptor.getValue().getTemplateParams().get("processInstanceName"));
        assertEquals("eDHR审批", notifyCaptor.getValue().getTemplateParams().get("taskName"));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenProcessInstanceApprove_edhrProcess_usesNotifyInbox() {
        BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO = new BpmMessageSendWhenProcessInstanceApproveReqDTO();
        reqDTO.setProcessInstanceId("proc-edhr-2");
        reqDTO.setProcessInstanceName("eDHR Approval V1");
        reqDTO.setProcessDefinitionKey(EDHR_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(611L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9102L);

        messageService.sendMessageWhenProcessInstanceApprove(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(611L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_EDHR_BPM_APPROVED", notifyCaptor.getValue().getTemplateCode());
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenProcessInstanceReject_edhrProcess_usesNotifyInbox() {
        BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO = new BpmMessageSendWhenProcessInstanceRejectReqDTO();
        reqDTO.setProcessInstanceId("proc-edhr-3");
        reqDTO.setProcessInstanceName("eDHR Approval V1");
        reqDTO.setProcessDefinitionKey(EDHR_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(611L);
        reqDTO.setReason("退回补充");
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9103L);

        messageService.sendMessageWhenProcessInstanceReject(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(611L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_EDHR_BPM_REJECTED", notifyCaptor.getValue().getTemplateCode());
        assertEquals("退回补充", notifyCaptor.getValue().getTemplateParams().get("reason"));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenTaskTimeout_edhrProcess_usesNotifyInbox() {
        BpmMessageSendWhenTaskTimeoutReqDTO reqDTO = new BpmMessageSendWhenTaskTimeoutReqDTO();
        reqDTO.setProcessInstanceId("proc-edhr-4");
        reqDTO.setProcessInstanceName("eDHR Approval V1");
        reqDTO.setProcessDefinitionKey(EDHR_PROCESS_DEFINITION_KEY);
        reqDTO.setTaskId("task-edhr-4");
        reqDTO.setTaskName("eDHR审批");
        reqDTO.setAssigneeUserId(916L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9104L);

        messageService.sendMessageWhenTaskTimeout(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(916L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_EDHR_BPM_TASK_TIMEOUT", notifyCaptor.getValue().getTemplateCode());
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenTaskAssigned_routeVersionProcess_usesNotifyInbox() {
        BpmMessageSendWhenTaskCreatedReqDTO reqDTO = new BpmMessageSendWhenTaskCreatedReqDTO();
        reqDTO.setProcessInstanceId("proc-route-1");
        reqDTO.setProcessInstanceName("工艺路线版本 V20");
        reqDTO.setProcessDefinitionKey(ROUTE_VERSION_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(611L);
        reqDTO.setStartUserNickname("提交人");
        reqDTO.setTaskId("task-route-1");
        reqDTO.setTaskName("工艺路线版本 115 V20");
        reqDTO.setAssigneeUserId(916L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9201L);

        messageService.sendMessageWhenTaskAssigned(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(916L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_ROUTE_VERSION_BPM_TASK_ASSIGNED", notifyCaptor.getValue().getTemplateCode());
        assertEquals("工艺路线版本 V20", notifyCaptor.getValue().getTemplateParams().get("processInstanceName"));
        assertEquals("工艺路线版本 115 V20", notifyCaptor.getValue().getTemplateParams().get("taskName"));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenProcessInstanceApprove_routeVersionProcess_usesNotifyInbox() {
        BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO = new BpmMessageSendWhenProcessInstanceApproveReqDTO();
        reqDTO.setProcessInstanceId("proc-route-2");
        reqDTO.setProcessInstanceName("工艺路线版本 V20");
        reqDTO.setProcessDefinitionKey(ROUTE_VERSION_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(611L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9202L);

        messageService.sendMessageWhenProcessInstanceApprove(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(611L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_ROUTE_VERSION_BPM_APPROVED", notifyCaptor.getValue().getTemplateCode());
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenProcessInstanceReject_routeVersionProcess_usesNotifyInbox() {
        BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO = new BpmMessageSendWhenProcessInstanceRejectReqDTO();
        reqDTO.setProcessInstanceId("proc-route-3");
        reqDTO.setProcessInstanceName("工艺路线版本 V20");
        reqDTO.setProcessDefinitionKey(ROUTE_VERSION_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(611L);
        reqDTO.setReason("工艺参数需补充");
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9203L);

        messageService.sendMessageWhenProcessInstanceReject(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(611L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_ROUTE_VERSION_BPM_REJECTED", notifyCaptor.getValue().getTemplateCode());
        assertEquals("工艺参数需补充", notifyCaptor.getValue().getTemplateParams().get("reason"));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenTaskTimeout_routeVersionProcess_usesNotifyInbox() {
        BpmMessageSendWhenTaskTimeoutReqDTO reqDTO = new BpmMessageSendWhenTaskTimeoutReqDTO();
        reqDTO.setProcessInstanceId("proc-route-4");
        reqDTO.setProcessInstanceName("工艺路线版本 V20");
        reqDTO.setProcessDefinitionKey(ROUTE_VERSION_PROCESS_DEFINITION_KEY);
        reqDTO.setTaskId("task-route-4");
        reqDTO.setTaskName("工艺路线版本 115 V20");
        reqDTO.setAssigneeUserId(916L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9204L);

        messageService.sendMessageWhenTaskTimeout(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(916L, notifyCaptor.getValue().getUserId());
        assertEquals("MES_ROUTE_VERSION_BPM_TASK_TIMEOUT", notifyCaptor.getValue().getTemplateCode());
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenProcessInstanceReject_dccProcess_usesNotifyInbox() {
        BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO = new BpmMessageSendWhenProcessInstanceRejectReqDTO();
        reqDTO.setProcessInstanceId("proc-4");
        reqDTO.setProcessInstanceName("DCC审批驳回");
        reqDTO.setProcessDefinitionKey(DCC_PROCESS_DEFINITION_KEY);
        reqDTO.setStartUserId(78L);
        reqDTO.setReason("资料不完整");
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9003L);

        messageService.sendMessageWhenProcessInstanceReject(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(78L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_controlled_file_rejected", notifyCaptor.getValue().getTemplateCode());
        assertEquals("资料不完整", notifyCaptor.getValue().getTemplateParams().get("reason"));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    @Test
    void sendMessageWhenTaskTimeout_dccProcess_usesNotifyInbox() {
        BpmMessageSendWhenTaskTimeoutReqDTO reqDTO = new BpmMessageSendWhenTaskTimeoutReqDTO();
        reqDTO.setProcessInstanceId("proc-5");
        reqDTO.setProcessInstanceName("DCC超时提醒");
        reqDTO.setProcessDefinitionKey(DCC_PROCESS_DEFINITION_KEY);
        reqDTO.setTaskId("task-5");
        reqDTO.setTaskName("文控批准");
        reqDTO.setAssigneeUserId(79L);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class))).thenReturn(9004L);

        messageService.sendMessageWhenTaskTimeout(reqDTO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(79L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_task_timeout", notifyCaptor.getValue().getTemplateCode());
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }
}
