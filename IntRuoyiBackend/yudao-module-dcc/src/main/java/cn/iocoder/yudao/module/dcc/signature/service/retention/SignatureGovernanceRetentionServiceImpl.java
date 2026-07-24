package cn.iocoder.yudao.module.dcc.signature.service.retention;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class SignatureGovernanceRetentionServiceImpl implements SignatureGovernanceRetentionService {

    private static final String SOURCE_TYPE_DCC_SIGNATURE = "DCC_SIGNATURE";
    private static final String SOURCE_TYPE_EDHR_ARCHIVE = "EDHR_ARCHIVE";

    private final SignatureGovernanceRetentionVerificationService verificationService;

    public SignatureGovernanceRetentionServiceImpl(
            List<SignatureGovernanceRetentionVerificationService> verificationServices) {
        if (verificationServices == null || verificationServices.isEmpty()) {
            this.verificationService = null;
            return;
        }
        if (verificationServices.size() > 1) {
            throw new IllegalStateException("Exactly one signature retention verification service is required");
        }
        this.verificationService = verificationServices.get(0);
    }

    @Override
    public SignatureGovernanceRetentionPrecheckResult precheck(SignatureGovernanceRetentionPrecheckCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Retention precheck command is required");
        }
        EnumSet<SignatureGovernanceRetentionBlockerCode> codes = EnumSet.noneOf(
                SignatureGovernanceRetentionBlockerCode.class);
        if (isBlank(command.endpoint())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.ENDPOINT_MISSING);
        }
        if (isBlank(command.bucketName())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.BUCKET_MISSING);
        }
        if (command.ownerUserId() == null) {
            codes.add(SignatureGovernanceRetentionBlockerCode.OWNER_MISSING);
        }
        if (command.sampleDccSignatureId() == null) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SAMPLE_DCC_SIGNATURE_MISSING);
        }
        if (command.sampleEdhrArchiveId() == null) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SAMPLE_EDHR_ARCHIVE_MISSING);
        }
        List<SignatureGovernanceRetentionBlocker> blockers = new ArrayList<>(toBlockers(codes));
        if (verificationService == null) {
            blockers.add(toBlocker(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING));
        } else {
            SignatureGovernanceRetentionVerificationResult verification = verificationService.verify(command);
            blockers.addAll(verification.blockers());
        }
        if (!blockers.isEmpty()) {
            return SignatureGovernanceRetentionPrecheckResult.blocked(blockers);
        }
        return SignatureGovernanceRetentionPrecheckResult.ready();
    }

    @Override
    public SignatureGovernanceRetentionReceiptResult createDccEvidenceReceipt(
            SignatureGovernanceRetentionReceiptCommand command) {
        EnumSet<SignatureGovernanceRetentionBlockerCode> codes = validateCommonReceiptCommand(command);
        validateSourceType(command, SOURCE_TYPE_DCC_SIGNATURE, codes);
        if (isBlank(command.evidenceHash())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.DCC_EVIDENCE_HASH_MISSING);
        }
        if (!codes.isEmpty()) {
            return SignatureGovernanceRetentionReceiptResult.blocked(toBlockers(codes));
        }
        List<SignatureGovernanceRetentionBlocker> verificationBlockers = verifyReceipt(command);
        if (!verificationBlockers.isEmpty()) {
            return SignatureGovernanceRetentionReceiptResult.blocked(verificationBlockers);
        }
        return SignatureGovernanceRetentionReceiptResult.recorded(receiptId(command));
    }

    @Override
    public SignatureGovernanceRetentionReceiptResult createEdhrArchiveReceipt(
            SignatureGovernanceRetentionReceiptCommand command) {
        EnumSet<SignatureGovernanceRetentionBlockerCode> codes = validateCommonReceiptCommand(command);
        validateSourceType(command, SOURCE_TYPE_EDHR_ARCHIVE, codes);
        if (isBlank(command.archiveSha256())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.EDHR_ARCHIVE_HASH_MISSING);
        }
        if (isBlank(command.signatureHash())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SIGNATURE_HASH_MISSING);
        }
        if (!codes.isEmpty()) {
            return SignatureGovernanceRetentionReceiptResult.blocked(toBlockers(codes));
        }
        List<SignatureGovernanceRetentionBlocker> verificationBlockers = verifyReceipt(command);
        if (!verificationBlockers.isEmpty()) {
            return SignatureGovernanceRetentionReceiptResult.blocked(verificationBlockers);
        }
        return SignatureGovernanceRetentionReceiptResult.recorded(receiptId(command));
    }

    @Override
    public SignatureGovernanceRecoveryRehearsalResult runRecoveryRehearsal(
            SignatureGovernanceRecoveryRehearsalCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Recovery rehearsal command is required");
        }
        EnumSet<SignatureGovernanceRetentionBlockerCode> codes = EnumSet.noneOf(
                SignatureGovernanceRetentionBlockerCode.class);
        if (isBlank(command.backupId())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.BACKUP_ID_MISSING);
        }
        if (isBlank(command.recoveryRuntime())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.RECOVERY_RUNTIME_MISSING);
        }
        if (!command.ownerReviewed()) {
            codes.add(SignatureGovernanceRetentionBlockerCode.OWNER_REVIEW_MISSING);
        }
        if (!command.reportWritten()) {
            codes.add(SignatureGovernanceRetentionBlockerCode.REPORT_WRITE_FAILED);
        }
        if (!command.auditWritten()) {
            codes.add(SignatureGovernanceRetentionBlockerCode.AUDIT_WRITE_FAILED);
        }
        if (command.samples() == null || command.samples().isEmpty()) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SAMPLE_DCC_SIGNATURE_MISSING);
            codes.add(SignatureGovernanceRetentionBlockerCode.SAMPLE_EDHR_ARCHIVE_MISSING);
        } else {
            command.samples().forEach(sample -> validateSample(sample, codes));
        }
        if (!codes.isEmpty()) {
            return SignatureGovernanceRecoveryRehearsalResult.blocked(toBlockers(codes));
        }
        List<SignatureGovernanceRetentionBlocker> verificationBlockers = verifyRecoveryRehearsal(command);
        if (!verificationBlockers.isEmpty()) {
            return SignatureGovernanceRecoveryRehearsalResult.blocked(verificationBlockers);
        }
        return SignatureGovernanceRecoveryRehearsalResult.passed();
    }

    private List<SignatureGovernanceRetentionBlocker> verifyReceipt(
            SignatureGovernanceRetentionReceiptCommand command) {
        if (verificationService == null) {
            return List.of(toBlocker(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING));
        }
        SignatureGovernanceRetentionVerificationResult verification = verificationService.verifyReceipt(command);
        if (verification == null || !verification.verified()) {
            return verification == null
                    ? List.of(toBlocker(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING))
                    : verification.blockers();
        }
        return List.of();
    }

    private List<SignatureGovernanceRetentionBlocker> verifyRecoveryRehearsal(
            SignatureGovernanceRecoveryRehearsalCommand command) {
        if (verificationService == null) {
            return List.of(toBlocker(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING));
        }
        SignatureGovernanceRetentionVerificationResult verification = verificationService.verifyRecoveryRehearsal(command);
        if (verification == null || !verification.verified()) {
            return verification == null
                    ? List.of(toBlocker(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING))
                    : verification.blockers();
        }
        return List.of();
    }

    private static EnumSet<SignatureGovernanceRetentionBlockerCode> validateCommonReceiptCommand(
            SignatureGovernanceRetentionReceiptCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Retention receipt command is required");
        }
        EnumSet<SignatureGovernanceRetentionBlockerCode> codes = EnumSet.noneOf(
                SignatureGovernanceRetentionBlockerCode.class);
        if (command.sourceId() == null) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SAMPLE_DCC_SIGNATURE_MISSING);
        }
        if (isBlank(command.objectKey())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.OBJECT_KEY_MISSING);
        }
        if (isBlank(command.versionId())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.VERSION_ID_MISSING);
        }
        if (isBlank(command.retentionMode())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.RETENTION_MODE_MISSING);
        }
        if (command.retainUntil() == null) {
            codes.add(SignatureGovernanceRetentionBlockerCode.RETAIN_UNTIL_MISSING);
        }
        if (isBlank(command.sha256())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SHA256_MISSING);
        }
        if (isBlank(command.auditEventId())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.AUDIT_EVENT_MISSING);
        }
        return codes;
    }

    private static void validateSourceType(SignatureGovernanceRetentionReceiptCommand command, String expectedSourceType,
            EnumSet<SignatureGovernanceRetentionBlockerCode> codes) {
        if (!expectedSourceType.equals(command.sourceType())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SOURCE_TYPE_MISMATCH);
        }
    }

    private static void validateSample(SignatureGovernanceRecoverySample sample,
            EnumSet<SignatureGovernanceRetentionBlockerCode> codes) {
        if (sample == null) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SAMPLE_DCC_SIGNATURE_MISSING);
            return;
        }
        if (isBlank(sample.objectKey())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.OBJECT_KEY_MISSING);
        }
        if (isBlank(sample.versionId())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.VERSION_ID_MISSING);
        }
        if (isBlank(sample.expectedSha256()) || isBlank(sample.restoredSha256())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.SHA256_MISSING);
        } else if (!sample.expectedSha256().equals(sample.restoredSha256())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.HASH_MISMATCH);
        }
        if (isBlank(sample.expectedDomainHash()) || isBlank(sample.restoredDomainHash())) {
            addDomainHashMissing(sample, codes);
        } else if (!sample.expectedDomainHash().equals(sample.restoredDomainHash())) {
            codes.add(SignatureGovernanceRetentionBlockerCode.HASH_MISMATCH);
        }
    }

    private static void addDomainHashMissing(SignatureGovernanceRecoverySample sample,
            EnumSet<SignatureGovernanceRetentionBlockerCode> codes) {
        if (sample.sampleType() == SignatureGovernanceRecoverySampleType.EDHR_ARCHIVE) {
            codes.add(SignatureGovernanceRetentionBlockerCode.EDHR_ARCHIVE_HASH_MISSING);
            return;
        }
        codes.add(SignatureGovernanceRetentionBlockerCode.DCC_EVIDENCE_HASH_MISSING);
    }

    private static String receiptId(SignatureGovernanceRetentionReceiptCommand command) {
        return command.sourceType() + ":" + command.sourceId() + ":" + command.versionId();
    }

    private static List<SignatureGovernanceRetentionBlocker> toBlockers(
            EnumSet<SignatureGovernanceRetentionBlockerCode> codes) {
        return codes.stream().map(SignatureGovernanceRetentionServiceImpl::toBlocker).toList();
    }

    private static SignatureGovernanceRetentionBlocker toBlocker(SignatureGovernanceRetentionBlockerCode code) {
        return new SignatureGovernanceRetentionBlocker(code, message(code), impact(code));
    }

    private static String message(SignatureGovernanceRetentionBlockerCode code) {
        return switch (code) {
            case ENDPOINT_MISSING -> "Retention endpoint is missing";
            case BUCKET_MISSING -> "Retention bucket is missing";
            case OBJECT_LOCK_MISSING -> "Object Lock is not enabled for the retention bucket";
            case VERSIONING_MISSING -> "Bucket versioning is not enabled";
            case DEFAULT_RETENTION_MISSING -> "Default retention is not configured";
            case RETENTION_VERIFICATION_SOURCE_MISSING ->
                    "Server-side retention verification source is not configured";
            case RETENTION_MODE_MISSING -> "Retention mode is missing";
            case PERMISSION_MISSING -> "Retention permissions are not verified";
            case OWNER_MISSING -> "Retention owner is missing";
            case OWNER_REVIEW_MISSING -> "Owner recovery review is missing";
            case SAMPLE_DCC_SIGNATURE_MISSING -> "Sample DCC signature evidence is missing";
            case SAMPLE_EDHR_ARCHIVE_MISSING -> "Sample eDHR archive is missing";
            case RECOVERY_RUNTIME_MISSING -> "Recovery runtime is missing";
            case BACKUP_ID_MISSING -> "Recovery backupId is missing";
            case OBJECT_KEY_MISSING -> "Retention object key is missing";
            case VERSION_ID_MISSING -> "Object version id is missing";
            case RETENTION_OBJECT_MISSING -> "Retained object version is missing";
            case RETENTION_METADATA_MISSING -> "Retained object metadata is missing";
            case RETENTION_VERIFICATION_FAILED -> "Server-side retention verification failed";
            case RETAIN_UNTIL_MISSING -> "Retain-until timestamp is missing";
            case SHA256_MISSING -> "SHA-256 hash is missing";
            case DCC_EVIDENCE_HASH_MISSING -> "DCC evidenceHash is missing";
            case EDHR_ARCHIVE_HASH_MISSING -> "eDHR archive SHA-256 is missing";
            case SIGNATURE_HASH_MISSING -> "eDHR signatureHash is missing";
            case SOURCE_TYPE_MISMATCH -> "Retention receipt sourceType does not match the receipt entry point";
            case HASH_MISMATCH -> "Recovered hash does not match the retained inventory";
            case REPORT_WRITE_FAILED -> "Recovery rehearsal report was not written";
            case AUDIT_EVENT_MISSING -> "Retention audit event id is missing";
            case AUDIT_WRITE_FAILED -> "Recovery rehearsal audit was not written";
        };
    }

    private static String impact(SignatureGovernanceRetentionBlockerCode code) {
        return switch (code) {
            case ENDPOINT_MISSING, BUCKET_MISSING, OBJECT_LOCK_MISSING, VERSIONING_MISSING,
                    DEFAULT_RETENTION_MISSING, RETENTION_VERIFICATION_SOURCE_MISSING, RETENTION_MODE_MISSING,
                    PERMISSION_MISSING ->
                    "Long-term tamper-resistant retention cannot be enabled for signature evidence.";
            case OWNER_MISSING ->
                    "Retention governance cannot assign accountability or complete precheck approval.";
            case OWNER_REVIEW_MISSING ->
                    "Recovery rehearsal cannot be marked passed until the owner reviews the result.";
            case SAMPLE_DCC_SIGNATURE_MISSING, SAMPLE_EDHR_ARCHIVE_MISSING ->
                    "Precheck cannot prove real DCC and eDHR evidence coverage.";
            case RECOVERY_RUNTIME_MISSING, BACKUP_ID_MISSING ->
                    "Recovery rehearsal cannot start in an independent verification runtime.";
            case OBJECT_KEY_MISSING, VERSION_ID_MISSING, RETENTION_OBJECT_MISSING, RETENTION_METADATA_MISSING,
                    RETENTION_VERIFICATION_FAILED, RETAIN_UNTIL_MISSING ->
                    "WORM receipt is incomplete and cannot prove object immutability.";
            case SHA256_MISSING, DCC_EVIDENCE_HASH_MISSING, EDHR_ARCHIVE_HASH_MISSING, SIGNATURE_HASH_MISSING,
                    SOURCE_TYPE_MISMATCH, HASH_MISMATCH ->
                    "Recovered evidence integrity cannot be proven.";
            case REPORT_WRITE_FAILED ->
                    "Recovery rehearsal has no durable report and must remain blocked.";
            case AUDIT_EVENT_MISSING, AUDIT_WRITE_FAILED ->
                    "Governance action has no audit trail and must fail fast.";
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
