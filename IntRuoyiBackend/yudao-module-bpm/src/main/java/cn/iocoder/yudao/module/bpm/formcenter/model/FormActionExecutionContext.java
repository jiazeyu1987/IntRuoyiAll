package cn.iocoder.yudao.module.bpm.formcenter.model;

/** Server-supplied invocation authority. Never reconstructed from submitted form data. */
public final class FormActionExecutionContext {
    public enum Kind { MANUAL, VERIFIED_BACKFILL }
    private final Kind kind;
    private final Long actorUserId;
    private final String expectedExecutorCode;
    private final String evidenceHash;

    private FormActionExecutionContext(Kind kind, Long actorUserId, String executorCode, String evidenceHash) {
        if (actorUserId == null || actorUserId <= 0) throw new IllegalArgumentException("Actual form submitter is required");
        this.kind = kind;
        this.actorUserId = actorUserId;
        this.expectedExecutorCode = executorCode;
        this.evidenceHash = evidenceHash;
    }

    public static FormActionExecutionContext manual(Long actorUserId) {
        return new FormActionExecutionContext(Kind.MANUAL, actorUserId, null, null);
    }

    public static FormActionExecutionContext verifiedBackfill(Long actorUserId, String executorCode, String evidenceHash) {
        if (executorCode == null || executorCode.isBlank() || evidenceHash == null || !evidenceHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Verified backfill executor and evidence hash are required");
        }
        return new FormActionExecutionContext(Kind.VERIFIED_BACKFILL, actorUserId, executorCode, evidenceHash);
    }

    public Kind getKind() { return kind; }
    public Long getActorUserId() { return actorUserId; }
    public String getExpectedExecutorCode() { return expectedExecutorCode; }
    public String getEvidenceHash() { return evidenceHash; }
}
