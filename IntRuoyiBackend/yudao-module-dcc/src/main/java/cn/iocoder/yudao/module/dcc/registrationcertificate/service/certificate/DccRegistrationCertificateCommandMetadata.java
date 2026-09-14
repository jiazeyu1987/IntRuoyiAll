package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

public record DccRegistrationCertificateCommandMetadata(
        Long tenantId,
        Long actorId,
        String idempotencyKey,
        String requestTraceId,
        String commandKind,
        String payloadHash) {
}
