package cn.iocoder.yudao.module.dcc.service.download;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DccDownloadEncryptionContractValidatorTest {

    private static final String PLAIN_SHA256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String CIPHER_SHA256 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private final DccDownloadEncryptionContractValidator validator = new DccDownloadEncryptionContractValidator();

    @Test
    void requireReadyEvidence_acceptsCompleteReadyContractResult() {
        assertDoesNotThrow(() -> validator.requireReadyEvidence(result("READY",
                "ART-20260528-0001",
                "cipher://dcc-download/ART-20260528-0001",
                PLAIN_SHA256,
                CIPHER_SHA256,
                "dcc-download-policy-v1",
                new byte[0])));
    }

    @Test
    void requireReadyEvidence_rejectsMissingOrIncompleteContractResult() {
        assertInvalid(null);
        assertInvalid(result("PENDING", "ART-1", "cipher://ART-1", PLAIN_SHA256, CIPHER_SHA256, "v1", new byte[0]));
        assertInvalid(result("READY", null, "cipher://ART-1", PLAIN_SHA256, CIPHER_SHA256, "v1", new byte[0]));
        assertInvalid(result("READY", "ART-1", null, PLAIN_SHA256, CIPHER_SHA256, "v1", new byte[0]));
        assertInvalid(result("READY", "ART-1", "cipher://ART-1", null, CIPHER_SHA256, "v1", new byte[0]));
        assertInvalid(result("READY", "ART-1", "cipher://ART-1", PLAIN_SHA256, null, "v1", new byte[0]));
        assertInvalid(result("READY", "ART-1", "cipher://ART-1", PLAIN_SHA256, CIPHER_SHA256, null, new byte[0]));
        assertInvalid(result("READY", "ART-1", "cipher://ART-1", "not-a-sha", CIPHER_SHA256, "v1", new byte[0]));
        assertInvalid(result("READY", "ART-1", "cipher://ART-1", PLAIN_SHA256, "not-a-sha", "v1", new byte[0]));
    }

    private void assertInvalid(DccDownloadEncryptionResult result) {
        assertThrows(ServiceException.class, () -> validator.requireReadyEvidence(result));
    }

    private DccDownloadEncryptionResult result(String status, String artifactId, String cipherFileRef,
                                               String plainSha256, String cipherSha256,
                                               String encryptionPolicyVersion, byte[] cipherBytes) {
        return new DccDownloadEncryptionResult(status, artifactId, cipherFileRef, plainSha256, cipherSha256,
                encryptionPolicyVersion, "encrypted.dcc", "application/octet-stream", cipherBytes);
    }
}
