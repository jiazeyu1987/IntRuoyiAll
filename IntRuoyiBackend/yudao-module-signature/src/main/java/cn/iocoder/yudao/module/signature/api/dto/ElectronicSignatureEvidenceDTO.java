package cn.iocoder.yudao.module.signature.api.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

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
    @JsonProperty("formatVersion")
    public String formatVersion() { var content=readContent();var version=content==null?null:content.get("evidenceFormatVersion");return version==null?"V1":version.isTextual()?version.textValue():null; }
    @JsonProperty("imageState")
    public String imageState() { var content=readContent();var image=content==null?null:content.get("signatureImage");return image==null?"LEGACY_MISSING":image.path("state").isTextual()?image.path("state").textValue():null; }
    @JsonProperty("nodePosition")
    public JsonNode nodePosition() { var content=readContent();var node=content==null?null:content.get("nodePosition");return node==null?null:node.deepCopy(); }

    @JsonProperty("identityDomain")
    public String identityDomain() {
        var content = readContent();
        var identity = content == null ? null : content.get("signatureIdentity");
        var domain = identity == null ? null : identity.get("domain");
        return domain != null && domain.isTextual() ? domain.textValue() : null;
    }

    @JsonProperty("identitySnapshotStatus")
    public String identitySnapshotStatus() {
        var content = readContent();
        var identity = content == null ? null : content.get("signatureIdentity");
        var name = identity == null || !identity.isObject() ? null : identity.get("displayName");
        return name == null || !name.isTextual() || name.textValue().isBlank() ? "MISSING" : "SNAPSHOT";
    }

    @JsonProperty("evidenceFormatVersion")
    public String evidenceFormatVersion() {
        var content = readContent();
        // Existing V1 storage has no formal version column. Business JSON keys cannot
        // declare a protocol; this display value does not imply successful verification.
        return content == null || !content.isObject() ? "UNSUPPORTED" : "V1";
    }

    private JsonNode readContent() {
        if (canonicalContentJson == null || canonicalContentJson.isBlank()) return null;
        try { return new ObjectMapper().readTree(canonicalContentJson); }
        catch (JsonProcessingException ex) { return null; }
    }

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
