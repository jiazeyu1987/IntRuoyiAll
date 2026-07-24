package cn.iocoder.yudao.module.dcc.signature.service.retention;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class SignatureGovernanceRetentionObjectStoreVerificationService
        implements SignatureGovernanceRetentionVerificationService {

    private static final String SOURCE_TYPE_DCC_SIGNATURE = "DCC_SIGNATURE";
    private static final String SOURCE_TYPE_EDHR_ARCHIVE = "EDHR_ARCHIVE";

    private final SignatureGovernanceRetentionS3Properties properties;
    private final SignatureGovernanceRetentionObjectStore objectStore;

    public SignatureGovernanceRetentionObjectStoreVerificationService(SignatureGovernanceRetentionS3Properties properties,
            SignatureGovernanceRetentionObjectStore objectStore) {
        this.properties = properties;
        this.objectStore = objectStore;
    }

    @Override
    public SignatureGovernanceRetentionVerificationResult verify(SignatureGovernanceRetentionPrecheckCommand command) {
        try {
            SignatureGovernanceRetentionBucketState state = objectStore.readBucketState();
            List<SignatureGovernanceRetentionBlocker> blockers = new ArrayList<>();
            if (!sameEndpoint(properties.getEndpoint(), command.endpoint())) {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.ENDPOINT_MISSING,
                        "Retention endpoint does not match the server-side verifier configuration"));
            }
            if (!safeEquals(properties.getBucketName(), command.bucketName())) {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.BUCKET_MISSING,
                        "Retention bucket does not match the server-side verifier configuration"));
            }
            if (state == null || !state.bucketExists()) {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.BUCKET_MISSING,
                        "Configured retention bucket does not exist or cannot be read"));
            }
            if (state != null && !state.versioningEnabled()) {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.VERSIONING_MISSING,
                        "Configured retention bucket versioning is not enabled"));
            }
            if (state != null && !state.objectLockEnabled()) {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.OBJECT_LOCK_MISSING,
                        "Configured retention bucket Object Lock is not enabled"));
            }
            if (state != null && !state.defaultRetentionEnabled()) {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.DEFAULT_RETENTION_MISSING,
                        "Configured retention bucket default retention is not configured"));
            }
            if (state != null && isBlank(state.defaultRetentionMode())) {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.RETENTION_MODE_MISSING,
                        "Configured retention bucket default retention mode is not readable"));
            }
            if (state != null && !state.permissionsReadable()) {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.PERMISSION_MISSING,
                        "Configured retention bucket permissions are not readable"));
            }
            return SignatureGovernanceRetentionVerificationResult.blocked(blockers);
        } catch (RuntimeException ex) {
            return failed(ex);
        }
    }

    @Override
    public SignatureGovernanceRetentionVerificationResult verifyReceipt(
            SignatureGovernanceRetentionReceiptCommand command) {
        try {
            SignatureGovernanceRetentionStoredObject object = objectStore.readObject(command.objectKey(),
                    command.versionId());
            if (object == null) {
                return blocked(SignatureGovernanceRetentionBlockerCode.RETENTION_OBJECT_MISSING,
                        "Configured retention object version does not exist: " + command.objectKey());
            }
            List<SignatureGovernanceRetentionBlocker> blockers = new ArrayList<>();
            verifyObjectRetention(command.retentionMode(), command.retainUntil(), object, blockers);
            verifySha256(command.sha256(), object, blockers);
            verifyMetadataValue(object, "sourceType", command.sourceType(), blockers);
            verifyMetadataValue(object, "sourceId", String.valueOf(command.sourceId()), blockers);
            verifyMetadataValue(object, "auditEventId", command.auditEventId(), blockers);
            if (SOURCE_TYPE_DCC_SIGNATURE.equals(command.sourceType())) {
                verifyMetadataValue(object, "evidenceHash", command.evidenceHash(), blockers);
            } else if (SOURCE_TYPE_EDHR_ARCHIVE.equals(command.sourceType())) {
                verifyMetadataValue(object, "archiveSha256", command.archiveSha256(), blockers);
                verifyMetadataValue(object, "signatureHash", command.signatureHash(), blockers);
            } else {
                blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.SOURCE_TYPE_MISMATCH,
                        "Unsupported retention receipt sourceType: " + command.sourceType()));
            }
            return SignatureGovernanceRetentionVerificationResult.blocked(blockers);
        } catch (RuntimeException ex) {
            return failed(ex);
        }
    }

    @Override
    public SignatureGovernanceRetentionVerificationResult verifyRecoveryRehearsal(
            SignatureGovernanceRecoveryRehearsalCommand command) {
        try {
            List<SignatureGovernanceRetentionBlocker> blockers = new ArrayList<>();
            for (SignatureGovernanceRecoverySample sample : command.samples()) {
                SignatureGovernanceRetentionStoredObject object = objectStore.readObject(sample.objectKey(),
                        sample.versionId());
                if (object == null) {
                    blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.RETENTION_OBJECT_MISSING,
                            "Configured recovery sample object version does not exist: " + sample.objectKey()));
                    continue;
                }
                verifySha256(sample.restoredSha256(), object, blockers);
                verifyMetadataValue(object, "backupId", command.backupId(), blockers);
                verifyMetadataValue(object, "recoveryRuntime", command.recoveryRuntime(), blockers);
                verifyMetadataValue(object, "ownerReviewed", String.valueOf(command.ownerReviewed()), blockers);
                verifyMetadataValue(object, "reportWritten", String.valueOf(command.reportWritten()), blockers);
                verifyMetadataValue(object, "auditWritten", String.valueOf(command.auditWritten()), blockers);
                verifyMetadataValue(object, "sourceType", sample.sampleType().name(), blockers);
                verifyRecoveryDomainHash(sample, object, blockers);
            }
            return SignatureGovernanceRetentionVerificationResult.blocked(blockers);
        } catch (RuntimeException ex) {
            return failed(ex);
        }
    }

    private void verifyObjectRetention(String expectedMode, Instant expectedRetainUntil,
            SignatureGovernanceRetentionStoredObject object, List<SignatureGovernanceRetentionBlocker> blockers) {
        if (isBlank(object.retentionMode())) {
            blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.OBJECT_LOCK_MISSING,
                    "Retained object version has no Object Lock retention mode"));
        } else if (!equalsIgnoreCase(expectedMode, object.retentionMode())) {
            blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.RETENTION_MODE_MISSING,
                    "Retained object Object Lock mode does not match the receipt command"));
        }
        if (object.retainUntil() == null) {
            blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.RETAIN_UNTIL_MISSING,
                    "Retained object version has no retain-until timestamp"));
        } else if (!object.retainUntil().equals(expectedRetainUntil)) {
            blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.RETAIN_UNTIL_MISSING,
                    "Retained object retain-until timestamp does not match the receipt command"));
        }
    }

    private void verifySha256(String expectedSha256, SignatureGovernanceRetentionStoredObject object,
            List<SignatureGovernanceRetentionBlocker> blockers) {
        String actualSha256 = sha256(object.content());
        if (!equalsIgnoreCase(expectedSha256, actualSha256)) {
            blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.HASH_MISMATCH,
                    "Server-side retained object SHA-256 does not match the command"));
        }
    }

    private void verifyRecoveryDomainHash(SignatureGovernanceRecoverySample sample,
            SignatureGovernanceRetentionStoredObject object, List<SignatureGovernanceRetentionBlocker> blockers) {
        if (sample.sampleType() == SignatureGovernanceRecoverySampleType.EDHR_ARCHIVE) {
            verifyMetadataValue(object, "archiveSha256", sample.restoredDomainHash(), blockers);
            return;
        }
        verifyMetadataValue(object, "evidenceHash", sample.restoredDomainHash(), blockers);
    }

    private void verifyMetadataValue(SignatureGovernanceRetentionStoredObject object, String key, String expectedValue,
            List<SignatureGovernanceRetentionBlocker> blockers) {
        String actualValue = object.metadataValue(key);
        if (isBlank(actualValue)) {
            blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.RETENTION_METADATA_MISSING,
                    "Retained object metadata is missing required key: " + key));
            return;
        }
        if (!actualValue.equals(expectedValue)) {
            blockers.add(blocker(SignatureGovernanceRetentionBlockerCode.HASH_MISMATCH,
                    "Retained object metadata value does not match command key: " + key));
        }
    }

    private SignatureGovernanceRetentionVerificationResult failed(RuntimeException ex) {
        SignatureGovernanceRetentionBlockerCode code = ex instanceof SignatureGovernanceRetentionObjectStoreException
                ? ((SignatureGovernanceRetentionObjectStoreException) ex).getBlockerCode()
                : SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_FAILED;
        return blocked(code, ex.getMessage());
    }

    private SignatureGovernanceRetentionVerificationResult blocked(SignatureGovernanceRetentionBlockerCode code,
            String message) {
        return SignatureGovernanceRetentionVerificationResult.blocked(List.of(blocker(code, message)));
    }

    private SignatureGovernanceRetentionBlocker blocker(SignatureGovernanceRetentionBlockerCode code, String message) {
        return new SignatureGovernanceRetentionBlocker(code, message, impact(code));
    }

    private String impact(SignatureGovernanceRetentionBlockerCode code) {
        return switch (code) {
            case ENDPOINT_MISSING, BUCKET_MISSING, VERSIONING_MISSING, OBJECT_LOCK_MISSING,
                    DEFAULT_RETENTION_MISSING, RETENTION_MODE_MISSING, PERMISSION_MISSING,
                    RETENTION_VERIFICATION_FAILED ->
                    "Configured S3 Object Lock retention cannot be proven from server-side evidence.";
            case RETENTION_OBJECT_MISSING, RETENTION_METADATA_MISSING, RETAIN_UNTIL_MISSING, SOURCE_TYPE_MISMATCH,
                    HASH_MISMATCH ->
                    "Receipt or recovery rehearsal cannot pass until retained object content and metadata match.";
            default -> "Retention verifier returned a blocker that must be resolved before release.";
        };
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private static boolean equalsIgnoreCase(String expected, String actual) {
        return expected != null && actual != null && expected.equalsIgnoreCase(actual);
    }

    private static boolean safeEquals(String expected, String actual) {
        return expected != null && expected.equals(actual);
    }

    private static boolean sameEndpoint(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return trimTrailingSlash(expected).equals(trimTrailingSlash(actual));
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
