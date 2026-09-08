package cn.iocoder.yudao.module.signature.gxp.api;

public record GxpAuditTrailAppendCommand(
        String operationId,
        GxpAuditSubject subject,
        GxpAuditAction action,
        String reason,
        GxpAuditActor actor,
        GxpAuditStateEnvelope beforeState,
        GxpAuditStateEnvelope afterState,
        String policyVersion,
        String idempotencyKey,
        String requestId,
        String signatureRecordId,
        String signatureContentHash
) {

    public GxpAuditTrailAppendCommand withReason(String newReason) {
        return new GxpAuditTrailAppendCommand(operationId, subject, action, newReason, actor, beforeState, afterState,
                policyVersion, idempotencyKey, requestId, signatureRecordId, signatureContentHash);
    }

    public GxpAuditTrailAppendCommand withAfterState(GxpAuditStateEnvelope newAfterState) {
        return new GxpAuditTrailAppendCommand(operationId, subject, action, reason, actor, beforeState, newAfterState,
                policyVersion, idempotencyKey, requestId, signatureRecordId, signatureContentHash);
    }

}
