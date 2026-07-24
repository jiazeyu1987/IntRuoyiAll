package cn.iocoder.yudao.module.bpm.formcenter.service;

public class FormBusinessEffectResult {

    private final boolean success;
    private final String resultRef;
    private final String failureReason;

    private FormBusinessEffectResult(boolean success, String resultRef, String failureReason) {
        this.success = success;
        this.resultRef = resultRef;
        this.failureReason = failureReason;
    }

    public static FormBusinessEffectResult success(String resultRef) {
        return new FormBusinessEffectResult(true, resultRef, null);
    }

    public static FormBusinessEffectResult failure(String failureReason) {
        return new FormBusinessEffectResult(false, null, failureReason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResultRef() {
        return resultRef;
    }

    public String getFailureReason() {
        return failureReason;
    }

}
