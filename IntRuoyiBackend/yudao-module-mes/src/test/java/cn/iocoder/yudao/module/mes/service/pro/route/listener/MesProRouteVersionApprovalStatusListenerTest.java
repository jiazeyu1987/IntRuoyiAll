package cn.iocoder.yudao.module.mes.service.pro.route.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionApprovalService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionWorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionApprovalStatusListenerTest {

    @Mock
    private MesProRouteVersionApprovalService approvalService;

    private MesProRouteVersionApprovalStatusListener listener;

    @BeforeEach
    void setUp() {
        listener = new MesProRouteVersionApprovalStatusListener();
        ReflectionTestUtils.setField(listener, "approvalService", approvalService);
    }

    @Test
    void approveEvent_shouldPublishPendingRouteVersionCandidate() {
        BpmProcessInstanceStatusEvent event = routeVersionEvent(BpmProcessInstanceStatusEnum.APPROVE.getStatus());

        listener.onApplicationEvent(event);

        verify(approvalService).handleApprovalCallback("fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-2", "APPROVED", null, 501L);
    }

    @Test
    void rejectEvent_shouldRejectPendingRouteVersionCandidate() {
        BpmProcessInstanceStatusEvent event = routeVersionEvent(BpmProcessInstanceStatusEnum.REJECT.getStatus())
                .setReason("配置不完整");

        listener.onApplicationEvent(event);

        verify(approvalService).handleApprovalCallback("fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-3", "REJECTED", "配置不完整", 501L);
    }

    @Test
    void cancelEvent_shouldCancelPendingRouteVersionCandidate() {
        BpmProcessInstanceStatusEvent event = routeVersionEvent(BpmProcessInstanceStatusEnum.CANCEL.getStatus())
                .setReason("撤回审批");

        listener.onApplicationEvent(event);

        verify(approvalService).handleApprovalCallback("fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-4", "CANCELED", "撤回审批", 501L);
    }

    @Test
    void otherProcessDefinitionKey_shouldBeIgnored() {
        BpmProcessInstanceStatusEvent event = routeVersionEvent(BpmProcessInstanceStatusEnum.APPROVE.getStatus())
                .setProcessDefinitionKey("other-process");

        listener.onApplicationEvent(event);

        verify(approvalService, never()).handleApprovalCallback("fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-2", "APPROVED", null, 501L);
    }

    @Test
    void platformBusinessApprovalEvent_shouldBeIgnoredByPrivateRouteVersionListener() {
        BpmProcessInstanceStatusEvent event = routeVersionEvent(BpmProcessInstanceStatusEnum.APPROVE.getStatus())
                .setBusinessKey("BUSINESS_APPROVAL:3001");

        listener.onApplicationEvent(event);

        verify(approvalService, never()).handleApprovalCallback("fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-2", "APPROVED", null, 501L);
    }

    private static BpmProcessInstanceStatusEvent routeVersionEvent(Integer status) {
        return new BpmProcessInstanceStatusEvent(new Object())
                .setId("fbede791-8138-11f1-80b5-00155d3585b8")
                .setProcessDefinitionKey(MesProRouteVersionWorkflowServiceImpl.ROUTE_VERSION_APPROVAL_PROCESS_DEFINITION_KEY)
                .setBusinessKey("77001")
                .setStatus(status)
                .setActorUserId(501L);
    }
}
