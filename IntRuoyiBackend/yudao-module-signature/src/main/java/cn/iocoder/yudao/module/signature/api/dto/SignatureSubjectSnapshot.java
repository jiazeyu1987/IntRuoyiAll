package cn.iocoder.yudao.module.signature.api.dto;

public record SignatureSubjectSnapshot(
        String subjectType,
        String subjectId,
        String subjectVersion,
        String canonicalContentJson,
        String beforeContentJson,
        String afterContentJson,
        String fieldDiffJson,
        String processInstanceId,
        String taskId,
        String nodeCode,
        Integer nodeOrder) {
}
