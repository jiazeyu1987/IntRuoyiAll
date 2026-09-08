package cn.iocoder.yudao.module.signature.api.dto;

public record ElectronicSignatureCommand(
        String moduleCode,
        String actionCode,
        String subjectType,
        String subjectId,
        String expectedSubjectVersion,
        String credential,
        String reason,
        String idempotencyKey,
        String businessOccurredAt,
        String businessTimeZone) {
}
