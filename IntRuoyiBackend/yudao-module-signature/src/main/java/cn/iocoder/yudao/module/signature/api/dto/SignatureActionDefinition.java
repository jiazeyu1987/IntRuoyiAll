package cn.iocoder.yudao.module.signature.api.dto;

public record SignatureActionDefinition(
        String moduleCode,
        String actionCode,
        String subjectType,
        String meaningCode,
        String meaningLabel,
        String policyVersion) {
}
