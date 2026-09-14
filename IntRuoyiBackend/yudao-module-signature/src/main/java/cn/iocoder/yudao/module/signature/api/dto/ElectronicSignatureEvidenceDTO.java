package cn.iocoder.yudao.module.signature.api.dto;

import java.time.LocalDateTime;

public record ElectronicSignatureEvidenceDTO(
        Long id,
        String moduleCode,
        String actionCode,
        String subjectType,
        String subjectId,
        String subjectVersion,
        Long actorId,
        String meaningCode,
        String meaningLabel,
        String reason,
        LocalDateTime signedAt,
        String timeEvidenceId,
        String authenticationMethod,
        String contentHash,
        String evidenceHash,
        String algorithm,
        String keyVersion,
        String policyVersion,
        String verificationStatus,
        String processInstanceId,
        String taskId,
        String nodeCode,
        Integer nodeOrder,
        String canonicalContentJson,
        String beforeContentJson,
        String afterContentJson,
        String fieldDiffJson
) {
}
