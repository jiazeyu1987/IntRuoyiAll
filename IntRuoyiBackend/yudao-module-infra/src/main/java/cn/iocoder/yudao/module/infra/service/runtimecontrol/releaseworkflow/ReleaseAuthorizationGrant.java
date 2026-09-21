package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ReleaseAuthorizationGrant(
        String grantId,
        String workflowId,
        String releaseTag,
        String packageDigest,
        String manifestDigest,
        String targetEnvironment,
        String presetId,
        String presetVersion,
        String approvedScope,
        String approver,
        Instant issuedAt,
        Instant validUntil,
        String nonce,
        Instant revokedAt,
        Instant consumedAt,
        String previewId,
        String targetFingerprint,
        long expectedStateVersion) {

    @JsonCreator
    public ReleaseAuthorizationGrant(
            @JsonProperty("grantId") String grantId,
            @JsonProperty("workflowId") String workflowId,
            @JsonProperty("releaseTag") String releaseTag,
            @JsonProperty("packageDigest") String packageDigest,
            @JsonProperty("manifestDigest") String manifestDigest,
            @JsonProperty("targetEnvironment") String targetEnvironment,
            @JsonProperty("presetId") String presetId,
            @JsonProperty("presetVersion") String presetVersion,
            @JsonProperty("approvedScope") String approvedScope,
            @JsonProperty("approver") String approver,
            @JsonProperty("issuedAt") Instant issuedAt,
            @JsonProperty("validUntil") Instant validUntil,
            @JsonProperty("nonce") String nonce,
            @JsonProperty("revokedAt") Instant revokedAt,
            @JsonProperty("consumedAt") Instant consumedAt,
            @JsonProperty("previewId") String previewId,
            @JsonProperty("targetFingerprint") String targetFingerprint,
            @JsonProperty("expectedStateVersion") long expectedStateVersion) {
        this.grantId = requireText(grantId, "grantId");
        this.workflowId = requireText(workflowId, "workflowId");
        this.releaseTag = requireText(releaseTag, "releaseTag");
        this.packageDigest = requireDigest(packageDigest, "packageDigest");
        this.manifestDigest = requireDigest(manifestDigest, "manifestDigest");
        this.targetEnvironment = requireText(targetEnvironment, "targetEnvironment");
        this.presetId = requireText(presetId, "presetId");
        this.presetVersion = requireText(presetVersion, "presetVersion");
        this.approvedScope = requireText(approvedScope, "approvedScope");
        this.approver = requireText(approver, "approver");
        this.issuedAt = issuedAt;
        this.validUntil = validUntil;
        this.nonce = requireText(nonce, "nonce");
        this.revokedAt = revokedAt;
        this.consumedAt = consumedAt;
        this.previewId = requireText(previewId, "previewId");
        this.targetFingerprint = requireDigest(targetFingerprint, "targetFingerprint");
        this.expectedStateVersion = expectedStateVersion;
        if (issuedAt == null || validUntil == null || !validUntil.isAfter(issuedAt)) {
            throw new IllegalArgumentException("RELEASE_AUTHORIZATION_TIME_INVALID");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("RELEASE_AUTHORIZATION_" + name.toUpperCase() + "_INVALID");
        }
        return value;
    }

    private static String requireDigest(String value, String name) {
        if (value == null || !value.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("RELEASE_AUTHORIZATION_" + name.toUpperCase() + "_INVALID");
        }
        return value.toLowerCase();
    }
}
