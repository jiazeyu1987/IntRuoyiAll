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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @Test
    void wrongBusinessKeyFailsBeforePendingEffectDuplicateOrTerminalReturn() {
        for (BusinessApprovalRequestStatus status : List.of(BusinessApprovalRequestStatus.PENDING_BPM,
                BusinessApprovalRequestStatus.APPROVED)) {
            for (boolean duplicateKey : List.of(false, true)) {
                for (String wrongKey : List.of("BUSINESS_APPROVAL:9999", "BUSINESS_APPROVAL:",
                        "BUSINESS_APPROVAL:01", "BUSINESS_APPROVAL:1:extra")) {
                    var store = pendingRequestStore("bpm-001");
                    var validEvent = event("bpm-001", BpmProcessInstanceStatusEnum.APPROVE.getStatus());
                    String key = !duplicateKey ? null
                            : validEvent.getId() + ":" + validEvent.getStatus() + ":" + validEvent.getActorUserId();
                    store.update(store.findByProcessInstanceId("bpm-001").orElseThrow().toBuilder()
                            .status(status).lastEventKey(key).build());
                    var executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
                    var ex = assertThrows(BusinessApprovalException.class, () -> listener(store, executor)
                            .onApplicationEvent(validEvent.setBusinessKey(wrongKey)));
                    assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EVENT_MISMATCH, ex.getErrorCode());
                    assertEquals(status, store.findByProcessInstanceId("bpm-001").orElseThrow().getStatus());
                    assertEquals(key, store.findByProcessInstanceId("bpm-001").orElseThrow().getLastEventKey());
                    assertEquals(0, executor.getApprovedExecutions());
                    assertEquals(0, executor.getRejections());
                    assertEquals(0, executor.getCancellations());
                }
            }
        }
    }

    @Test
    void wrongStoredProcessOrDefinitionFailsBeforeAnyEffectOrStoreUpdate() {
        var pending = pendingRequestStore("bpm-001").findByProcessInstanceId("bpm-001").orElseThrow();
        for (var mismatch : List.of(pending.toBuilder().processInstanceId("other-process").build(),
                pending.toBuilder().processDefinitionKey("other-definition").build(),
                pending.toBuilder().processDefinitionKey(null).build())) {
            var store = mock(BusinessApprovalRequestStore.class);
            when(store.findByProcessInstanceId("bpm-001")).thenReturn(java.util.Optional.of(mismatch));
            var executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
            var listener = new BusinessApprovalBpmEventListener(store,
                    new BusinessApprovalEffectExecutorRegistry(List.of(executor)));
            var ex = assertThrows(BusinessApprovalException.class, () -> listener.onApplicationEvent(
                    event("bpm-001", BpmProcessInstanceStatusEnum.APPROVE.getStatus())));
            assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EVENT_MISMATCH, ex.getErrorCode());
            assertEquals(0, executor.getApprovedExecutions());
            verify(store, never()).update(any());
        }
    }

    @Test
    void unrelatedApplicationEventsKeepTheirExistingEarlyReturn() {
        var store = mock(BusinessApprovalRequestStore.class);
        var registry = mock(BusinessApprovalEffectExecutorRegistry.class);
        var listener = new BusinessApprovalBpmEventListener(store, registry);
        listener.onApplicationEvent(event("bpm-other", BpmProcessInstanceStatusEnum.APPROVE.getStatus()).setBusinessKey(null));
        listener.onApplicationEvent(event("bpm-other", BpmProcessInstanceStatusEnum.APPROVE.getStatus()).setBusinessKey("OTHER:1"));
        verifyNoInteractions(store, registry);
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
                .setBusinessKey("BUSINESS_APPROVAL:1")
                .setStatus(status)
                .setActorUserId(501L);
    }

}
