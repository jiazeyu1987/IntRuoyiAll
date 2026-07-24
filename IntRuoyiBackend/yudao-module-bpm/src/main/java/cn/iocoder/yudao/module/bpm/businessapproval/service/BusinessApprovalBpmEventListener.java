package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class BusinessApprovalBpmEventListener implements ApplicationListener<BpmProcessInstanceStatusEvent> {

    private static final String BUSINESS_KEY_PREFIX = "BUSINESS_APPROVAL:";

    private final BusinessApprovalRequestStore requestStore;
    private final BusinessApprovalEffectExecutorRegistry executorRegistry;

    public BusinessApprovalBpmEventListener(BusinessApprovalRequestStore requestStore,
                                            BusinessApprovalEffectExecutorRegistry executorRegistry) {
        this.requestStore = requestStore;
        this.executorRegistry = executorRegistry;
    }

    @Override
    public void onApplicationEvent(BpmProcessInstanceStatusEvent event) {
        if (event.getBusinessKey() == null || !event.getBusinessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        onProcessStatusEvent(event);
    }

    public BusinessApprovalRequest onProcessStatusEvent(BpmProcessInstanceStatusEvent event) {
        BusinessApprovalRequest request = requestStore.findByProcessInstanceId(event.getId())
                .orElseThrow(() -> new BusinessApprovalException(
                        BusinessApprovalErrorCode.BUSINESS_APPROVAL_REQUEST_NOT_FOUND,
                        "Business approval request not found by process instance: " + event.getId()));
        validateEventMatchesRequest(event, request);
        String eventKey = buildEventKey(event);
        if (Objects.equals(request.getLastEventKey(), eventKey)) {
            return request;
        }
        if (isTerminal(request)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EVENT_STALE,
                    "Business approval event is stale for request " + request.getRequestId());
        }
        BusinessApprovalEffectExecutor executor = executorRegistry.requireExecutor(request.getEffectExecutorCode());
        if (Objects.equals(event.getStatus(), BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            BusinessApprovalEffectResult result = executor.executeApproved(request.getContext(), request, event.getActorUserId());
            return requestStore.update(request.withTerminalEvent(BusinessApprovalRequestStatus.APPROVED, eventKey, result));
        }
        if (Objects.equals(event.getStatus(), BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            BusinessApprovalEffectResult result = executor.reject(request.getContext(), request, event.getActorUserId(),
                    event.getReason());
            return requestStore.update(request.withTerminalEvent(BusinessApprovalRequestStatus.REJECTED, eventKey, result));
        }
        if (Objects.equals(event.getStatus(), BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            BusinessApprovalEffectResult result = executor.cancel(request.getContext(), request, event.getActorUserId(),
                    event.getReason());
            return requestStore.update(request.withTerminalEvent(BusinessApprovalRequestStatus.CANCELED, eventKey, result));
        }
        return request;
    }

    private void validateEventMatchesRequest(BpmProcessInstanceStatusEvent event, BusinessApprovalRequest request) {
        if (request.getProcessDefinitionKey() != null
                && !request.getProcessDefinitionKey().isBlank()
                && !Objects.equals(request.getProcessDefinitionKey(), event.getProcessDefinitionKey())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EVENT_MISMATCH,
                    "BPM process definition key mismatched for approval request " + request.getRequestId());
        }
    }

    private boolean isTerminal(BusinessApprovalRequest request) {
        return request.getStatus() == BusinessApprovalRequestStatus.APPROVED
                || request.getStatus() == BusinessApprovalRequestStatus.REJECTED
                || request.getStatus() == BusinessApprovalRequestStatus.CANCELED
                || request.getStatus() == BusinessApprovalRequestStatus.DIRECT_EXECUTED;
    }

    private String buildEventKey(BpmProcessInstanceStatusEvent event) {
        return event.getId() + ":" + event.getStatus() + ":" + event.getActorUserId();
    }

}
