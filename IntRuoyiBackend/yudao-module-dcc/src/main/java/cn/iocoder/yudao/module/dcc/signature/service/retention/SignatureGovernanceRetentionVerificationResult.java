package cn.iocoder.yudao.module.dcc.signature.service.retention;

import java.util.EnumSet;
import java.util.List;

public record SignatureGovernanceRetentionVerificationResult(
        List<SignatureGovernanceRetentionBlocker> blockers) {

    public SignatureGovernanceRetentionVerificationResult {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    public boolean verified() {
        return blockers.isEmpty();
    }

    public static SignatureGovernanceRetentionVerificationResult passed() {
        return new SignatureGovernanceRetentionVerificationResult(List.of());
    }

    public static SignatureGovernanceRetentionVerificationResult blocked(
            List<SignatureGovernanceRetentionBlocker> blockers) {
        return new SignatureGovernanceRetentionVerificationResult(blockers);
    }

    public static SignatureGovernanceRetentionVerificationResult fromVerifiedBucketState(
            SignatureGovernanceRetentionPrecheckCommand command) {
        EnumSet<SignatureGovernanceRetentionBlockerCode> codes = EnumSet.noneOf(
                SignatureGovernanceRetentionBlockerCode.class);
        if (!command.objectLockEnabled()) {
            codes.add(SignatureGovernanceRetentionBlockerCode.OBJECT_LOCK_MISSING);
        }
        if (!command.versioningEnabled()) {
            codes.add(SignatureGovernanceRetentionBlockerCode.VERSIONING_MISSING);
        }
        if (!command.defaultRetentionEnabled()) {
            codes.add(SignatureGovernanceRetentionBlockerCode.DEFAULT_RETENTION_MISSING);
        }
        if (isBlank(command.retentionMode())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.RETENTION_MODE_MISSING);
        }
        if (!command.permissionsVerified()) {
            codes.add(SignatureGovernanceRetentionBlockerCode.PERMISSION_MISSING);
        }
        return new SignatureGovernanceRetentionVerificationResult(codes.stream()
                .map(SignatureGovernanceRetentionVerificationResult::toBlocker)
                .toList());
    }

    private static SignatureGovernanceRetentionBlocker toBlocker(SignatureGovernanceRetentionBlockerCode code) {
        return new SignatureGovernanceRetentionBlocker(code, message(code), impact(code));
    }

    private static String message(SignatureGovernanceRetentionBlockerCode code) {
        return switch (code) {
            case OBJECT_LOCK_MISSING -> "Object Lock is not enabled for the retention bucket";
            case VERSIONING_MISSING -> "Bucket versioning is not enabled";
            case DEFAULT_RETENTION_MISSING -> "Default retention is not configured";
            case RETENTION_MODE_MISSING -> "Retention mode is missing";
            case PERMISSION_MISSING -> "Retention permissions are not verified";
            case RETENTION_VERIFICATION_FAILED -> "Server-side retention verification failed";
            default -> throw new IllegalArgumentException("Unsupported retention verification blocker: " + code);
        };
    }

    private static String impact(SignatureGovernanceRetentionBlockerCode code) {
        return switch (code) {
            case OBJECT_LOCK_MISSING, VERSIONING_MISSING, DEFAULT_RETENTION_MISSING, RETENTION_MODE_MISSING,
                    PERMISSION_MISSING, RETENTION_VERIFICATION_FAILED ->
                    "Long-term tamper-resistant retention cannot be enabled for signature evidence.";
            default -> throw new IllegalArgumentException("Unsupported retention verification blocker: " + code);
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
