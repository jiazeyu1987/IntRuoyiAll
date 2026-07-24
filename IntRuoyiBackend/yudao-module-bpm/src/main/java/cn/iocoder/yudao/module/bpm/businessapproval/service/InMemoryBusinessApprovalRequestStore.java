package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryBusinessApprovalRequestStore implements BusinessApprovalRequestStore {

    private final AtomicLong idSequence = new AtomicLong(1L);
    private final Map<Long, BusinessApprovalRequest> byId = new LinkedHashMap<>();

    @Override
    public synchronized BusinessApprovalRequest createDirectRequest(BusinessApprovalContext context,
                                                                    BusinessApprovalPolicy policy) {
        BusinessApprovalRequest request = baseRequest(context, policy, BusinessApprovalRequestStatus.DIRECT_EXECUTING);
        byId.put(request.getRequestId(), request);
        return request;
    }

    @Override
    public synchronized BusinessApprovalRequest createPendingRequest(BusinessApprovalContext context,
                                                                     BusinessApprovalPolicy policy) {
        if (hasPendingRequest(context)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PENDING_CONFLICT,
                    "Business approval request already pending for object " + context.getObjectId());
        }
        BusinessApprovalRequest request = baseRequest(context, policy, BusinessApprovalRequestStatus.PENDING_BPM);
        byId.put(request.getRequestId(), request);
        return request;
    }

    @Override
    public synchronized BusinessApprovalRequest attachProcessInstance(Long requestId, String processInstanceId) {
        BusinessApprovalRequest request = requireRequest(requestId).withProcessInstance(processInstanceId);
        byId.put(request.getRequestId(), request);
        return request;
    }

    @Override
    public synchronized BusinessApprovalRequest update(BusinessApprovalRequest request) {
        requireRequest(request.getRequestId());
        byId.put(request.getRequestId(), request);
        return request;
    }

    @Override
    public synchronized Optional<BusinessApprovalRequest> findByProcessInstanceId(String processInstanceId) {
        return byId.values().stream()
                .filter(request -> Objects.equals(processInstanceId, request.getProcessInstanceId()))
                .findFirst();
    }

    @Override
    public synchronized Optional<BusinessApprovalRequest> findPendingByBusinessAction(BusinessApprovalContext context) {
        return byId.values().stream()
                .filter(request -> request.getStatus() == BusinessApprovalRequestStatus.PENDING_BPM
                        && sameBusinessAction(request.getContext(), context))
                .findFirst();
    }

    @Override
    public synchronized boolean hasPendingRequest(BusinessApprovalContext context) {
        return byId.values().stream().anyMatch(request ->
                request.getStatus() == BusinessApprovalRequestStatus.PENDING_BPM
                        && sameBusinessAction(request.getContext(), context));
    }

    private BusinessApprovalRequest requireRequest(Long requestId) {
        BusinessApprovalRequest request = byId.get(requestId);
        if (request == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_REQUEST_NOT_FOUND,
                    "Business approval request not found: " + requestId);
        }
        return request;
    }

    private BusinessApprovalRequest baseRequest(BusinessApprovalContext context,
                                                BusinessApprovalPolicy policy,
                                                BusinessApprovalRequestStatus status) {
        return BusinessApprovalRequest.builder()
                .requestId(idSequence.getAndIncrement())
                .tenantId(context.getTenantId())
                .policyId(policy.getPolicyId())
                .policyMode(policy.getMode())
                .processDefinitionKey(policy.getProcessDefinitionKey())
                .effectExecutorCode(policy.getEffectExecutorCode())
                .status(status)
                .context(context)
                .build();
    }

    private boolean sameBusinessAction(BusinessApprovalContext left, BusinessApprovalContext right) {
        return Objects.equals(left.getTenantId(), right.getTenantId())
                && Objects.equals(left.getDataDomain(), right.getDataDomain())
                && Objects.equals(left.getSystemCode(), right.getSystemCode())
                && Objects.equals(left.getObjectType(), right.getObjectType())
                && Objects.equals(left.getObjectId(), right.getObjectId())
                && Objects.equals(left.getObjectVersion(), right.getObjectVersion())
                && Objects.equals(left.getActionCode(), right.getActionCode());
    }

}
