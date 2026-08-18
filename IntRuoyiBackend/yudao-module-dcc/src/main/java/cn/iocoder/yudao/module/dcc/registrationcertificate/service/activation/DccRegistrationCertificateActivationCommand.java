package cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation;

public record DccRegistrationCertificateActivationCommand(
        Long tenantId,
        Long actorId,
        String idempotencyKey,
        String requestTraceId,
        Long certificateId,
        Integer expectedRowVersion,
        Long currentVersionId,
        Long pendingVersionId) {
}