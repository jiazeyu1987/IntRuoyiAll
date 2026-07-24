package cn.iocoder.yudao.module.dcc.signature.retention;

import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoveryRehearsalCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoverySample;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoverySampleType;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionBucketState;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionObjectStore;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionObjectStoreVerificationService;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionPrecheckCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionReceiptCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionS3Configuration;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionS3Properties;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionStoredObject;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionVerificationResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernanceRetentionObjectStoreVerificationServiceTest {

    private static final Instant RETAIN_UNTIL = Instant.parse("2036-05-28T00:00:00Z");
    private static final byte[] DCC_CONTENT = bytes("dcc retained signature evidence");
    private static final byte[] EDHR_CONTENT = bytes("edhr retained archive evidence");
    private static final String DCC_SHA256 = sha256(DCC_CONTENT);
    private static final String EDHR_SHA256 = sha256(EDHR_CONTENT);
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SignatureGovernanceRetentionS3Configuration.class);

    @Test
    void s3VerifierIsNotRegisteredUntilExplicitlyEnabled() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("signatureGovernanceRetentionVerificationService"));
            assertFalse(context.containsBean("signatureGovernanceRetentionObjectStore"));
        });
    }

    @Test
    void s3VerifierRegistersWhenExplicitlyEnabledWithRequiredConfiguration() {
        contextRunner.withPropertyValues(
                "signature.governance.retention.s3.enabled=true",
                "signature.governance.retention.s3.endpoint=https://minio.test.local",
                "signature.governance.retention.s3.bucket-name=dcc-signature-worm",
                "signature.governance.retention.s3.region=us-east-1",
                "signature.governance.retention.s3.access-key=test-access",
                "signature.governance.retention.s3.secret-key=test-secret")
                .run(context -> {
                    assertTrue(context.containsBean("signatureGovernanceRetentionVerificationService"));
                    assertTrue(context.containsBean("signatureGovernanceRetentionObjectStore"));
                    assertTrue(context.getBean(SignatureGovernanceRetentionVerificationService.class)
                            instanceof SignatureGovernanceRetentionObjectStoreVerificationService);
                });
    }

    @Test
    void precheck_blocksWhenClientClaimsReadyButServerBucketHasNoVersioning() {
        FakeObjectStore store = new FakeObjectStore();
        store.bucketState = new SignatureGovernanceRetentionBucketState(
                true, false, true, true, "COMPLIANCE", true);

        SignatureGovernanceRetentionVerificationResult result = verifier(store).verify(precheckCommand());

        assertFalse(result.verified());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.VERSIONING_MISSING), blockerCodes(result));
    }

    @Test
    void precheck_blocksWhenRequestBucketDoesNotMatchServerVerifierConfiguration() {
        FakeObjectStore store = new FakeObjectStore();

        SignatureGovernanceRetentionVerificationResult result = verifier(store).verify(
                new SignatureGovernanceRetentionPrecheckCommand(
                        "https://minio.test.local",
                        "client-claimed-bucket",
                        true,
                        true,
                        true,
                        "COMPLIANCE",
                        true,
                        101L,
                        710088L,
                        810099L));

        assertFalse(result.verified());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.BUCKET_MISSING), blockerCodes(result));
    }

    @Test
    void dccReceipt_blocksWhenServerEvidenceHashDiffersEvenIfRequestIsOtherwiseValid() {
        FakeObjectStore store = new FakeObjectStore();
        store.put(storedObject("dcc/signature/710088.json", "v-0001", DCC_CONTENT, metadata(
                "sourceType", "DCC_SIGNATURE",
                "sourceId", "710088",
                "auditEventId", "audit-1001",
                "evidenceHash", "server-side-different-evidence-hash")));

        SignatureGovernanceRetentionVerificationResult result = verifier(store).verifyReceipt(dccReceiptCommand());

        assertFalse(result.verified());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.HASH_MISMATCH), blockerCodes(result));
    }

    @Test
    void dccReceipt_passesOnlyWhenServerObjectRetentionContentAndMetadataMatch() {
        FakeObjectStore store = new FakeObjectStore();
        store.put(storedObject("dcc/signature/710088.json", "v-0001", DCC_CONTENT, metadata(
                "sourceType", "DCC_SIGNATURE",
                "sourceId", "710088",
                "auditEventId", "audit-1001",
                "evidenceHash", "dcc-evidence-hash")));

        SignatureGovernanceRetentionVerificationResult result = verifier(store).verifyReceipt(dccReceiptCommand());

        assertTrue(result.verified());
        assertTrue(result.blockers().isEmpty());
    }

    @Test
    void dccReceipt_acceptsMinioStyleSgMetadataAliasesFromObjectStore() {
        FakeObjectStore store = new FakeObjectStore();
        store.put(storedObject("dcc/signature/710088.json", "v-0001", DCC_CONTENT, metadata(
                "sg-source-type", "DCC_SIGNATURE",
                "sg-source-id", "710088",
                "sg-audit-event-id", "audit-1001",
                "sg-dcc-evidence-hash", "dcc-evidence-hash")));

        SignatureGovernanceRetentionVerificationResult result = verifier(store).verifyReceipt(dccReceiptCommand());

        assertTrue(result.verified());
        assertTrue(result.blockers().isEmpty());
    }

    @Test
    void edhrReceipt_blocksWhenServerSignatureHashMetadataIsMissing() {
        FakeObjectStore store = new FakeObjectStore();
        store.put(storedObject("edhr/archive/880077.pdf", "v-0002", EDHR_CONTENT, metadata(
                "sourceType", "EDHR_ARCHIVE",
                "sourceId", "880077",
                "auditEventId", "audit-1002",
                "archiveSha256", "edhr-archive-hash")));

        SignatureGovernanceRetentionVerificationResult result = verifier(store).verifyReceipt(edhrReceiptCommand());

        assertFalse(result.verified());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.RETENTION_METADATA_MISSING), blockerCodes(result));
    }

    @Test
    void recoveryRehearsal_blocksWhenServerDomainHashDiffersFromRecoverySample() {
        FakeObjectStore store = new FakeObjectStore();
        store.put(storedObject("dcc/signature/710088.json", "v-0001", DCC_CONTENT, recoveryMetadata(
                "DCC_SIGNATURE", "dcc-server-different-domain-hash")));

        SignatureGovernanceRetentionVerificationResult result = verifier(store)
                .verifyRecoveryRehearsal(recoveryCommand(List.of(dccRecoverySample())));

        assertFalse(result.verified());
        assertEquals(Set.of(SignatureGovernanceRetentionBlockerCode.HASH_MISMATCH), blockerCodes(result));
    }

    @Test
    void recoveryRehearsal_passesOnlyWhenServerSamplesMatchRuntimeBackupAuditOwnerAndHashes() {
        FakeObjectStore store = new FakeObjectStore();
        store.put(storedObject("dcc/signature/710088.json", "v-0001", DCC_CONTENT, recoveryMetadata(
                "DCC_SIGNATURE", "dcc-evidence-hash")));
        store.put(storedObject("edhr/archive/880077.pdf", "v-0002", EDHR_CONTENT, recoveryMetadata(
                "EDHR_ARCHIVE", "edhr-archive-hash")));

        SignatureGovernanceRetentionVerificationResult result = verifier(store)
                .verifyRecoveryRehearsal(recoveryCommand(List.of(dccRecoverySample(), edhrRecoverySample())));

        assertTrue(result.verified());
        assertTrue(result.blockers().isEmpty());
    }

    @Test
    void recoveryRehearsal_acceptsMinioStyleSgRecoveryMetadataAliases() {
        FakeObjectStore store = new FakeObjectStore();
        store.put(storedObject("dcc/signature/710088.json", "v-0001", DCC_CONTENT, metadata(
                "sg-recovery-backup-id", "backup-20260528-001",
                "sg-recovery-runtime", "isolated-restore-runtime-01",
                "sg-owner-reviewed", "true",
                "sg-report-written", "true",
                "sg-audit-written", "true",
                "sg-source-type", "DCC_SIGNATURE",
                "sg-domain-hash", "dcc-evidence-hash")));

        SignatureGovernanceRetentionVerificationResult result = verifier(store)
                .verifyRecoveryRehearsal(recoveryCommand(List.of(dccRecoverySample())));

        assertTrue(result.verified());
        assertTrue(result.blockers().isEmpty());
    }

    private static SignatureGovernanceRetentionObjectStoreVerificationService verifier(FakeObjectStore store) {
        return new SignatureGovernanceRetentionObjectStoreVerificationService(s3Properties(), store);
    }

    private static SignatureGovernanceRetentionS3Properties s3Properties() {
        SignatureGovernanceRetentionS3Properties properties = new SignatureGovernanceRetentionS3Properties();
        properties.setEndpoint("https://minio.test.local");
        properties.setBucketName("dcc-signature-worm");
        properties.setRegion("us-east-1");
        properties.setAccessKey("test-access");
        properties.setSecretKey("test-secret");
        properties.setPathStyleAccess(true);
        return properties;
    }

    private static SignatureGovernanceRetentionPrecheckCommand precheckCommand() {
        return new SignatureGovernanceRetentionPrecheckCommand(
                "https://minio.test.local",
                "dcc-signature-worm",
                true,
                true,
                true,
                "COMPLIANCE",
                true,
                101L,
                710088L,
                810099L);
    }

    private static SignatureGovernanceRetentionReceiptCommand dccReceiptCommand() {
        return new SignatureGovernanceRetentionReceiptCommand(
                "DCC_SIGNATURE",
                710088L,
                "dcc/signature/710088.json",
                "v-0001",
                "COMPLIANCE",
                RETAIN_UNTIL,
                DCC_SHA256,
                "dcc-evidence-hash",
                "",
                "",
                "audit-1001");
    }

    private static SignatureGovernanceRetentionReceiptCommand edhrReceiptCommand() {
        return new SignatureGovernanceRetentionReceiptCommand(
                "EDHR_ARCHIVE",
                880077L,
                "edhr/archive/880077.pdf",
                "v-0002",
                "COMPLIANCE",
                RETAIN_UNTIL,
                EDHR_SHA256,
                "",
                "edhr-archive-hash",
                "edhr-signature-hash",
                "audit-1002");
    }

    private static SignatureGovernanceRecoveryRehearsalCommand recoveryCommand(
            List<SignatureGovernanceRecoverySample> samples) {
        return new SignatureGovernanceRecoveryRehearsalCommand(
                "backup-20260528-001",
                "isolated-restore-runtime-01",
                true,
                true,
                true,
                samples);
    }

    private static SignatureGovernanceRecoverySample dccRecoverySample() {
        return new SignatureGovernanceRecoverySample(
                SignatureGovernanceRecoverySampleType.DCC_SIGNATURE,
                "dcc/signature/710088.json",
                "v-0001",
                DCC_SHA256,
                DCC_SHA256,
                "dcc-evidence-hash",
                "dcc-evidence-hash");
    }

    private static SignatureGovernanceRecoverySample edhrRecoverySample() {
        return new SignatureGovernanceRecoverySample(
                SignatureGovernanceRecoverySampleType.EDHR_ARCHIVE,
                "edhr/archive/880077.pdf",
                "v-0002",
                EDHR_SHA256,
                EDHR_SHA256,
                "edhr-archive-hash",
                "edhr-archive-hash");
    }

    private static SignatureGovernanceRetentionStoredObject storedObject(String key, String versionId, byte[] content,
            Map<String, String> metadata) {
        return new SignatureGovernanceRetentionStoredObject(key, versionId, "COMPLIANCE", RETAIN_UNTIL,
                content, metadata);
    }

    private static Map<String, String> recoveryMetadata(String sourceType, String domainHash) {
        Map<String, String> metadata = metadata(
                "backupId", "backup-20260528-001",
                "recoveryRuntime", "isolated-restore-runtime-01",
                "ownerReviewed", "true",
                "reportWritten", "true",
                "auditWritten", "true",
                "sourceType", sourceType);
        if ("EDHR_ARCHIVE".equals(sourceType)) {
            metadata.put("archiveSha256", domainHash);
        } else {
            metadata.put("evidenceHash", domainHash);
        }
        return metadata;
    }

    private static Map<String, String> metadata(String... values) {
        Map<String, String> metadata = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            metadata.put(values[i], values[i + 1]);
        }
        return metadata;
    }

    private static Set<SignatureGovernanceRetentionBlockerCode> blockerCodes(
            SignatureGovernanceRetentionVerificationResult result) {
        return result.blockers().stream().map(blocker -> blocker.getCode()).collect(Collectors.toSet());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class FakeObjectStore implements SignatureGovernanceRetentionObjectStore {

        private SignatureGovernanceRetentionBucketState bucketState = new SignatureGovernanceRetentionBucketState(
                true, true, true, true, "COMPLIANCE", true);
        private final Map<String, SignatureGovernanceRetentionStoredObject> objects = new HashMap<>();

        @Override
        public SignatureGovernanceRetentionBucketState readBucketState() {
            return bucketState;
        }

        @Override
        public SignatureGovernanceRetentionStoredObject readObject(String objectKey, String versionId) {
            return objects.get(objectKey + ":" + versionId);
        }

        void put(SignatureGovernanceRetentionStoredObject object) {
            objects.put(object.objectKey() + ":" + object.versionId(), object);
        }
    }
}
