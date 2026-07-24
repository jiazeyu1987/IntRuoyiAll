package cn.iocoder.yudao.module.dcc.signature.retention;

import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoveryRehearsalCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoveryRehearsalResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoverySample;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoverySampleType;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionPrecheckCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionPrecheckResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionReceiptCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionReceiptResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionService;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionServiceImpl;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionStatus;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionVerificationResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionVerificationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernanceRetentionServiceTest {

    private final SignatureGovernanceRetentionService service = new SignatureGovernanceRetentionServiceImpl(
            List.of(new PassingRetentionVerificationService()));

    @Test
    void precheck_exposesEveryRetentionPreconditionBlockerWithoutDefaultReceipt() {
        SignatureGovernanceRetentionPrecheckResult result = service.precheck(new SignatureGovernanceRetentionPrecheckCommand(
                "https://minio.test.local",
                "",
                false,
                false,
                false,
                "",
                false,
                null,
                null,
                null));

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isReady());
        assertEquals(Set.of(
                SignatureGovernanceRetentionBlockerCode.BUCKET_MISSING,
                SignatureGovernanceRetentionBlockerCode.OBJECT_LOCK_MISSING,
                SignatureGovernanceRetentionBlockerCode.VERSIONING_MISSING,
                SignatureGovernanceRetentionBlockerCode.DEFAULT_RETENTION_MISSING,
                SignatureGovernanceRetentionBlockerCode.RETENTION_MODE_MISSING,
                SignatureGovernanceRetentionBlockerCode.PERMISSION_MISSING,
                SignatureGovernanceRetentionBlockerCode.OWNER_MISSING,
                SignatureGovernanceRetentionBlockerCode.SAMPLE_DCC_SIGNATURE_MISSING,
                SignatureGovernanceRetentionBlockerCode.SAMPLE_EDHR_ARCHIVE_MISSING), blockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
        assertTrue(result.getBlockers().stream().allMatch(blocker -> !blocker.getImpact().isBlank()));
    }

    @Test
    void precheck_blocksReadyWhenVerificationSourceIsMissingEvenIfClientClaimsReady() {
        SignatureGovernanceRetentionService serviceWithoutVerifier = new SignatureGovernanceRetentionServiceImpl(
                List.of());

        SignatureGovernanceRetentionPrecheckResult result = serviceWithoutVerifier.precheck(
                new SignatureGovernanceRetentionPrecheckCommand(
                        "https://minio.test.local",
                        "dcc-signature-worm",
                        true,
                        true,
                        true,
                        "COMPLIANCE",
                        true,
                        101L,
                        710088L,
                        810099L));

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isReady());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING),
                blockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
    }

    @Test
    void precheck_marksReadyOnlyWhenServerVerifierConfirmsEveryRequiredPrecondition() {
        SignatureGovernanceRetentionPrecheckResult result = service.precheck(new SignatureGovernanceRetentionPrecheckCommand(
                "https://minio.test.local",
                "dcc-signature-worm",
                true,
                true,
                true,
                "COMPLIANCE",
                true,
                101L,
                710088L,
                810099L));

        assertEquals(SignatureGovernanceRetentionStatus.READY, result.getStatus());
        assertTrue(result.isReady());
        assertTrue(result.getBlockers().isEmpty());
        assertTrue(result.getReceiptId().isEmpty());
    }

    @Test
    void precheck_rejectsMissingCommandInsteadOfReturningDefaultSuccess() {
        assertThrows(IllegalArgumentException.class, () -> service.precheck(null));
    }

    @Test
    void dccEvidenceReceipt_requiresWormMetadataHashAndAuditEvent() {
        SignatureGovernanceRetentionReceiptResult result = service.createDccEvidenceReceipt(
                new SignatureGovernanceRetentionReceiptCommand(
                        "DCC_SIGNATURE",
                        710088L,
                        "dcc/signature/710088.json",
                        "",
                        "COMPLIANCE",
                        Instant.parse("2036-05-28T00:00:00Z"),
                        "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                        "",
                        "",
                        "",
                        ""));

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isRecorded());
        assertEquals(Set.of(
                SignatureGovernanceRetentionBlockerCode.VERSION_ID_MISSING,
                SignatureGovernanceRetentionBlockerCode.DCC_EVIDENCE_HASH_MISSING,
                SignatureGovernanceRetentionBlockerCode.AUDIT_EVENT_MISSING), receiptBlockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
    }

    @Test
    void dccEvidenceReceipt_blocksEdhrSourceTypeInsteadOfRecordingWrongReceipt() {
        SignatureGovernanceRetentionReceiptResult result = service.createDccEvidenceReceipt(
                new SignatureGovernanceRetentionReceiptCommand(
                        "EDHR_ARCHIVE",
                        710088L,
                        "dcc/signature/710088.json",
                        "v-0001",
                        "COMPLIANCE",
                        Instant.parse("2036-05-28T00:00:00Z"),
                        "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                        "dcc-evidence-hash",
                        "",
                        "",
                        "audit-1001"));

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isRecorded());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.SOURCE_TYPE_MISMATCH),
                receiptBlockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
    }

    @Test
    void dccEvidenceReceipt_blocksWhenServerVerifierIsMissingEvenIfRequestClaimsWormReceipt() {
        SignatureGovernanceRetentionService serviceWithoutVerifier = new SignatureGovernanceRetentionServiceImpl(
                List.of());

        SignatureGovernanceRetentionReceiptResult result = serviceWithoutVerifier.createDccEvidenceReceipt(
                validDccReceiptCommand());

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isRecorded());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING),
                receiptBlockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
    }

    @Test
    void dccEvidenceReceipt_blocksWhenServerVerifierDoesNotConfirmRealWormReceipt() {
        SignatureGovernanceRetentionService serviceWithBlockedVerifier = new SignatureGovernanceRetentionServiceImpl(
                List.of(new BlockingReceiptVerificationService()));

        SignatureGovernanceRetentionReceiptResult result = serviceWithBlockedVerifier.createDccEvidenceReceipt(
                validDccReceiptCommand());

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isRecorded());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.OBJECT_LOCK_MISSING),
                receiptBlockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
    }


    @Test
    void edhrArchiveReceipt_requiresArchiveHashSignatureHashVersionAndAuditEvent() {
        SignatureGovernanceRetentionReceiptResult result = service.createEdhrArchiveReceipt(
                new SignatureGovernanceRetentionReceiptCommand(
                        "EDHR_ARCHIVE",
                        880077L,
                        "edhr/archive/880077.pdf",
                        "",
                        "COMPLIANCE",
                        Instant.parse("2036-05-28T00:00:00Z"),
                        "",
                        "",
                        "",
                        "",
                        ""));

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isRecorded());
        assertEquals(Set.of(
                SignatureGovernanceRetentionBlockerCode.VERSION_ID_MISSING,
                SignatureGovernanceRetentionBlockerCode.SHA256_MISSING,
                SignatureGovernanceRetentionBlockerCode.EDHR_ARCHIVE_HASH_MISSING,
                SignatureGovernanceRetentionBlockerCode.SIGNATURE_HASH_MISSING,
                SignatureGovernanceRetentionBlockerCode.AUDIT_EVENT_MISSING), receiptBlockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
    }

    @Test
    void edhrArchiveReceipt_blocksDccSourceTypeInsteadOfRecordingWrongReceipt() {
        SignatureGovernanceRetentionReceiptResult result = service.createEdhrArchiveReceipt(
                new SignatureGovernanceRetentionReceiptCommand(
                        "DCC_SIGNATURE",
                        880077L,
                        "edhr/archive/880077.pdf",
                        "v-0002",
                        "COMPLIANCE",
                        Instant.parse("2036-05-28T00:00:00Z"),
                        "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                        "",
                        "edhr-archive-sha256",
                        "edhr-signature-hash",
                        "audit-1002"));

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isRecorded());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.SOURCE_TYPE_MISMATCH),
                receiptBlockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
    }

    @Test
    void edhrArchiveReceipt_blocksWhenServerVerifierIsMissingEvenIfRequestClaimsArchiveReceipt() {
        SignatureGovernanceRetentionService serviceWithoutVerifier = new SignatureGovernanceRetentionServiceImpl(
                List.of());

        SignatureGovernanceRetentionReceiptResult result = serviceWithoutVerifier.createEdhrArchiveReceipt(
                validEdhrReceiptCommand());

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isRecorded());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING),
                receiptBlockerCodes(result));
        assertTrue(result.getReceiptId().isEmpty());
    }

    @Test
    void recoveryRehearsal_exposesRuntimeOwnerVersionHashReportAndAuditBlockers() {
        SignatureGovernanceRecoveryRehearsalResult result = service.runRecoveryRehearsal(
                new SignatureGovernanceRecoveryRehearsalCommand(
                        "backup-20260528-001",
                        "",
                        false,
                        false,
                        false,
                        List.of(new SignatureGovernanceRecoverySample(
                                SignatureGovernanceRecoverySampleType.DCC_SIGNATURE,
                                "dcc/signature/710088.json",
                                "",
                                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                                "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"))));

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isPassed());
        assertEquals(Set.of(
                SignatureGovernanceRetentionBlockerCode.RECOVERY_RUNTIME_MISSING,
                SignatureGovernanceRetentionBlockerCode.OWNER_REVIEW_MISSING,
                SignatureGovernanceRetentionBlockerCode.VERSION_ID_MISSING,
                SignatureGovernanceRetentionBlockerCode.HASH_MISMATCH,
                SignatureGovernanceRetentionBlockerCode.REPORT_WRITE_FAILED,
                SignatureGovernanceRetentionBlockerCode.AUDIT_WRITE_FAILED), rehearsalBlockerCodes(result));
    }

    @Test
    void recoveryRehearsal_passesOnlyWhenRuntimeHashesReportAuditAndOwnerReviewArePresent() {
        SignatureGovernanceRecoveryRehearsalResult result = service.runRecoveryRehearsal(
                validRecoveryRehearsalCommand());

        assertEquals(SignatureGovernanceRetentionStatus.PASSED, result.getStatus());
        assertTrue(result.isPassed());
        assertTrue(result.getBlockers().isEmpty());
    }

    @Test
    void recoveryRehearsal_blocksWhenServerVerifierIsMissingEvenIfRequestClaimsSuccessfulRecovery() {
        SignatureGovernanceRetentionService serviceWithoutVerifier = new SignatureGovernanceRetentionServiceImpl(
                List.of());

        SignatureGovernanceRecoveryRehearsalResult result = serviceWithoutVerifier.runRecoveryRehearsal(
                validRecoveryRehearsalCommand());

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isPassed());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.RETENTION_VERIFICATION_SOURCE_MISSING),
                rehearsalBlockerCodes(result));
    }

    @Test
    void recoveryRehearsal_blocksWhenServerVerifierDoesNotConfirmRealRecoveryEvidence() {
        SignatureGovernanceRetentionService serviceWithBlockedVerifier = new SignatureGovernanceRetentionServiceImpl(
                List.of(new BlockingRecoveryVerificationService()));

        SignatureGovernanceRecoveryRehearsalResult result = serviceWithBlockedVerifier.runRecoveryRehearsal(
                validRecoveryRehearsalCommand());

        assertEquals(SignatureGovernanceRetentionStatus.BLOCKED, result.getStatus());
        assertFalse(result.isPassed());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.AUDIT_WRITE_FAILED),
                rehearsalBlockerCodes(result));
    }


    private static SignatureGovernanceRetentionReceiptCommand validDccReceiptCommand() {
        return new SignatureGovernanceRetentionReceiptCommand(
                "DCC_SIGNATURE",
                710088L,
                "dcc/signature/710088.json",
                "v-0001",
                "COMPLIANCE",
                Instant.parse("2036-05-28T00:00:00Z"),
                "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                "dcc-evidence-hash",
                "",
                "",
                "audit-1001");
    }

    private static SignatureGovernanceRetentionReceiptCommand validEdhrReceiptCommand() {
        return new SignatureGovernanceRetentionReceiptCommand(
                "EDHR_ARCHIVE",
                880077L,
                "edhr/archive/880077.pdf",
                "v-0002",
                "COMPLIANCE",
                Instant.parse("2036-05-28T00:00:00Z"),
                "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                "",
                "edhr-archive-sha256",
                "edhr-signature-hash",
                "audit-1002");
    }

    private static SignatureGovernanceRecoveryRehearsalCommand validRecoveryRehearsalCommand() {
        return new SignatureGovernanceRecoveryRehearsalCommand(
                "backup-20260528-001",
                "isolated-restore-runtime-01",
                true,
                true,
                true,
                List.of(
                        new SignatureGovernanceRecoverySample(
                                SignatureGovernanceRecoverySampleType.DCC_SIGNATURE,
                                "dcc/signature/710088.json",
                                "v-0001",
                                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"),
                        new SignatureGovernanceRecoverySample(
                                SignatureGovernanceRecoverySampleType.EDHR_ARCHIVE,
                                "edhr/archive/880077.pdf",
                                "v-0002",
                                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                                "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                                "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")));
    }

    private static Set<SignatureGovernanceRetentionBlockerCode> blockerCodes(
            SignatureGovernanceRetentionPrecheckResult result) {
        return result.getBlockers().stream().map(blocker -> blocker.getCode()).collect(Collectors.toSet());
    }

    private static Set<SignatureGovernanceRetentionBlockerCode> receiptBlockerCodes(
            SignatureGovernanceRetentionReceiptResult result) {
        return result.getBlockers().stream().map(blocker -> blocker.getCode()).collect(Collectors.toSet());
    }

    private static Set<SignatureGovernanceRetentionBlockerCode> rehearsalBlockerCodes(
            SignatureGovernanceRecoveryRehearsalResult result) {
        return result.getBlockers().stream().map(blocker -> blocker.getCode()).collect(Collectors.toSet());
    }

    private static class PassingRetentionVerificationService implements SignatureGovernanceRetentionVerificationService {

        @Override
        public SignatureGovernanceRetentionVerificationResult verify(SignatureGovernanceRetentionPrecheckCommand command) {
            return SignatureGovernanceRetentionVerificationResult.fromVerifiedBucketState(command);
        }

        @Override
        public SignatureGovernanceRetentionVerificationResult verifyReceipt(
                SignatureGovernanceRetentionReceiptCommand command) {
            return SignatureGovernanceRetentionVerificationResult.passed();
        }

        @Override
        public SignatureGovernanceRetentionVerificationResult verifyRecoveryRehearsal(
                SignatureGovernanceRecoveryRehearsalCommand command) {
            return SignatureGovernanceRetentionVerificationResult.passed();
        }
    }

    private static final class BlockingReceiptVerificationService extends PassingRetentionVerificationService {

        @Override
        public SignatureGovernanceRetentionVerificationResult verifyReceipt(
                SignatureGovernanceRetentionReceiptCommand command) {
            return SignatureGovernanceRetentionVerificationResult.blocked(List.of(
                    blocker(SignatureGovernanceRetentionBlockerCode.OBJECT_LOCK_MISSING)));
        }
    }

    private static final class BlockingRecoveryVerificationService extends PassingRetentionVerificationService {

        @Override
        public SignatureGovernanceRetentionVerificationResult verifyRecoveryRehearsal(
                SignatureGovernanceRecoveryRehearsalCommand command) {
            return SignatureGovernanceRetentionVerificationResult.blocked(List.of(
                    blocker(SignatureGovernanceRetentionBlockerCode.AUDIT_WRITE_FAILED)));
        }
    }

    private static cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionBlocker blocker(
            SignatureGovernanceRetentionBlockerCode code) {
        return new cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionBlocker(
                code,
                "server verifier did not confirm " + code.name(),
                "Receipt and recovery success must remain blocked until server-side evidence is verified.");
    }

}
