package cn.iocoder.yudao.module.bpm.formcenter.model;

public class FormEffectExecution {

    private final String executionId;
    private final String idempotencyKey;
    private final FormEffectStatus status;
    private final String resultRef;
    private final String failureReason;

    private FormEffectExecution(String executionId, String idempotencyKey, FormEffectStatus status,
            String resultRef, String failureReason) {
        this.executionId = executionId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.resultRef = resultRef;
        this.failureReason = failureReason;
    }

    public static FormEffectExecution applied(String executionId, String idempotencyKey, String resultRef) {
        return new FormEffectExecution(executionId, idempotencyKey, FormEffectStatus.APPLIED, resultRef, null);
    }

    public static FormEffectExecution failed(String executionId, String idempotencyKey, String failureReason) {
        return new FormEffectExecution(executionId, idempotencyKey, FormEffectStatus.FAILED_PENDING, null,
                failureReason);
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public FormEffectStatus getStatus() {
        return status;
    }

    public String getResultRef() {
        return resultRef;
    }

    public String getFailureReason() {
        return failureReason;
    }

}
