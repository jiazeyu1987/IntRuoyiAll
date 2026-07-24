package cn.iocoder.yudao.module.bpm.businessapproval.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class BusinessApprovalRequest {

    private final Long requestId;
    private final Long tenantId;
    private final Long policyId;
    private final BusinessApprovalPolicyMode policyMode;
    private final String processDefinitionKey;
    private final String effectExecutorCode;
    private final BusinessApprovalRequestStatus status;
    private final BusinessApprovalContext context;
    private final String processInstanceId;
    private final String lastEventKey;
    private final String resultState;
    private final String failureReason;

    public BusinessApprovalRequest withStatus(BusinessApprovalRequestStatus status,
                                              BusinessApprovalEffectResult result) {
        return toBuilder()
                .status(status)
                .resultState(result == null ? resultState : result.getResultState())
                .failureReason(result == null ? failureReason : result.getFailureReason())
                .build();
    }

    public BusinessApprovalRequest withProcessInstance(String processInstanceId) {
        return toBuilder().processInstanceId(processInstanceId).build();
    }

    public BusinessApprovalRequest withTerminalEvent(BusinessApprovalRequestStatus status,
                                                     String eventKey,
                                                     BusinessApprovalEffectResult result) {
        return toBuilder()
                .status(status)
                .lastEventKey(eventKey)
                .resultState(result == null ? resultState : result.getResultState())
                .failureReason(result == null ? failureReason : result.getFailureReason())
                .build();
    }

}
