package cn.iocoder.yudao.module.signature.api.dto;

public record SignatureSubjectCommand(
        Long actorId,
        String moduleCode,
        String actionCode,
        String subjectType,
        String subjectId,
        String expectedSubjectVersion,
        String reason) {
}
