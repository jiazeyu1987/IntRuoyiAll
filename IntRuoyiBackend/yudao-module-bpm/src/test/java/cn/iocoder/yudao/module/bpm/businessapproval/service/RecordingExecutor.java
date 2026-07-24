package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;

final class RecordingExecutor implements BusinessApprovalEffectExecutor {

    private final String executorCode;
    private final String bpmProcessDefinitionKey;
    private int directExecutions;
    private int pendingMarks;
    private int approvedExecutions;
    private int rejections;
    private int cancellations;
    private boolean failOnMarkPending;
    private BusinessApprovalRequest lastPendingRequest;

    RecordingExecutor(String executorCode) {
        this(executorCode, null);
    }

    RecordingExecutor(String executorCode, String bpmProcessDefinitionKey) {
        this.executorCode = executorCode;
        this.bpmProcessDefinitionKey = bpmProcessDefinitionKey;
    }

    @Override
    public String getExecutorCode() {
        return executorCode;
    }

    @Override
    public String getBpmProcessDefinitionKey() {
        return bpmProcessDefinitionKey;
    }

    @Override
    public void precheck(BusinessApprovalContext context) {
        // Test executor records side effects only.
    }

    @Override
    public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context,
                                                      BusinessApprovalRequest request) {
        directExecutions++;
        return BusinessApprovalEffectResult.completed("DIRECT_ACTIVE");
    }

    @Override
    public BusinessApprovalEffectResult markPending(BusinessApprovalContext context,
                                                    BusinessApprovalRequest request) {
        pendingMarks++;
        lastPendingRequest = request;
        if (failOnMarkPending) {
            throw new IllegalStateException("domain pending failed");
        }
        return BusinessApprovalEffectResult.pending("PENDING_APPROVAL");
    }

    @Override
    public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context,
                                                        BusinessApprovalRequest request,
                                                        Long actorUserId) {
        approvedExecutions++;
        return BusinessApprovalEffectResult.completed("ACTIVE");
    }

    @Override
    public BusinessApprovalEffectResult reject(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        rejections++;
        return BusinessApprovalEffectResult.rejected("REJECTED");
    }

    @Override
    public BusinessApprovalEffectResult cancel(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        cancellations++;
        return BusinessApprovalEffectResult.cancelled("DRAFT");
    }

    int getDirectExecutions() {
        return directExecutions;
    }

    int getPendingMarks() {
        return pendingMarks;
    }

    int getApprovedExecutions() {
        return approvedExecutions;
    }

    int getRejections() {
        return rejections;
    }

    int getCancellations() {
        return cancellations;
    }

    BusinessApprovalRequest getLastPendingRequest() {
        return lastPendingRequest;
    }

    RecordingExecutor failOnMarkPending() {
        failOnMarkPending = true;
        return this;
    }

}
