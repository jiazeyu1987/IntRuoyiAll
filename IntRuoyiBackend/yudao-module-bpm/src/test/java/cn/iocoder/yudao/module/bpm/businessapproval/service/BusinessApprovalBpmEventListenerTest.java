package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessApprovalBpmEventListenerTest {

    @Test
    void approvedEventExecutesDomainExecutorOnceAndDeduplicatesSameEvent() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        InMemoryBusinessApprovalRequestStore store = pendingRequestStore("bpm-001");
        BusinessApprovalBpmEventListener listener = listener(store, executor);
        BpmProcessInstanceStatusEvent event = event("bpm-001", BpmProcessInstanceStatusEnum.APPROVE.getStatus());

        BusinessApprovalRequest first = listener.onProcessStatusEvent(event);
        BusinessApprovalRequest duplicate = listener.onProcessStatusEvent(event);

        assertEquals(BusinessApprovalRequestStatus.APPROVED, first.getStatus());
        assertEquals(BusinessApprovalRequestStatus.APPROVED, duplicate.getStatus());
        assertEquals(1, executor.getApprovedExecutions());
    }

    @Test
    void rejectedEventCallsDomainRejectWithoutApprovedExecution() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        InMemoryBusinessApprovalRequestStore store = pendingRequestStore("bpm-001");
        BusinessApprovalBpmEventListener listener = listener(store, executor);

        BusinessApprovalRequest request = listener.onProcessStatusEvent(
                event("bpm-001", BpmProcessInstanceStatusEnum.REJECT.getStatus()));

        assertEquals(BusinessApprovalRequestStatus.REJECTED, request.getStatus());
        assertEquals(1, executor.getRejections());
        assertEquals(0, executor.getApprovedExecutions());
    }

    @Test
    void eventWithoutTrackedRequestFailsFast() {
        BusinessApprovalBpmEventListener listener = listener(new InMemoryBusinessApprovalRequestStore(),
                new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH"));

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> listener.onProcessStatusEvent(event("bpm-missing", BpmProcessInstanceStatusEnum.APPROVE.getStatus())));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_REQUEST_NOT_FOUND, ex.getErrorCode());
    }

    private static BusinessApprovalBpmEventListener listener(InMemoryBusinessApprovalRequestStore store,
                                                            RecordingExecutor executor) {
        return new BusinessApprovalBpmEventListener(store,
                new BusinessApprovalEffectExecutorRegistry(List.of(executor)));
    }

    private static InMemoryBusinessApprovalRequestStore pendingRequestStore(String processInstanceId) {
        InMemoryBusinessApprovalRequestStore store = new InMemoryBusinessApprovalRequestStore();
        BusinessApprovalRequest request = store.createPendingRequest(
                BusinessApprovalPolicyResolveServiceTest.baseContext().build(),
                BusinessApprovalPolicyResolveServiceTest.basePolicy()
                        .mode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                        .processDefinitionKey("mes-route-version-approval-v1")
                        .build());
        store.attachProcessInstance(request.getRequestId(), processInstanceId);
        return store;
    }

    private static BpmProcessInstanceStatusEvent event(String processInstanceId, Integer status) {
        return new BpmProcessInstanceStatusEvent("test")
                .setId(processInstanceId)
                .setProcessDefinitionKey("mes-route-version-approval-v1")
                .setBusinessKey("1001")
                .setStatus(status)
                .setActorUserId(501L);
    }

}
