package cn.iocoder.yudao.module.bpm.businessapproval.model;

import lombok.Getter;

@Getter
public class BusinessApprovalEffectResult {

    private final String resultState;
    private final String failureReason;

    private BusinessApprovalEffectResult(String resultState, String failureReason) {
        this.resultState = resultState;
        this.failureReason = failureReason;
    }

    public static BusinessApprovalEffectResult completed(String resultState) {
        return new BusinessApprovalEffectResult(resultState, null);
    }

    public static BusinessApprovalEffectResult pending(String resultState) {
        return new BusinessApprovalEffectResult(resultState, null);
    }

    public static BusinessApprovalEffectResult rejected(String resultState) {
        return new BusinessApprovalEffectResult(resultState, null);
    }

    public static BusinessApprovalEffectResult cancelled(String resultState) {
        return new BusinessApprovalEffectResult(resultState, null);
    }

    public static BusinessApprovalEffectResult failed(String failureReason) {
        return new BusinessApprovalEffectResult(null, failureReason);
    }

}
