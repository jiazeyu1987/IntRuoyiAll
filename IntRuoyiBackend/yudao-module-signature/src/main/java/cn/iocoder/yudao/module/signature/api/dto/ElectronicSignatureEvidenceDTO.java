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
        String fieldDiffJson,
        String actorDisplayName,
        String timeZone
) {

    public ElectronicSignatureEvidenceDTO(Long id, String moduleCode, String actionCode, String subjectType,
                                          String subjectId, String subjectVersion, Long actorId, String meaningCode,
                                          String meaningLabel, String reason, LocalDateTime signedAt,
                                          String timeEvidenceId, String authenticationMethod, String contentHash,
                                          String evidenceHash, String algorithm, String keyVersion,
                                          String policyVersion, String verificationStatus, String processInstanceId,
                                          String taskId, String nodeCode, Integer nodeOrder,
                                          String canonicalContentJson, String beforeContentJson,
                                          String afterContentJson, String fieldDiffJson, String actorDisplayName) {
        this(id, moduleCode, actionCode, subjectType, subjectId, subjectVersion, actorId, meaningCode,
                meaningLabel, reason, signedAt, timeEvidenceId, authenticationMethod, contentHash, evidenceHash,
                algorithm, keyVersion, policyVersion, verificationStatus, processInstanceId, taskId, nodeCode,
                nodeOrder, canonicalContentJson, beforeContentJson, afterContentJson, fieldDiffJson,
                actorDisplayName, null);
    }
}
