package cn.iocoder.yudao.module.dcc.service.download;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_ENCRYPTION_CONFIG_MISSING;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccAesGcmDownloadEncryptionGatewayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] aesKey = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private final String base64Key = Base64.getEncoder().encodeToString(aesKey);

    @Test
    void encrypt_createsAesGcmEnvelopePersistsCipherArtifactAndDoesNotContainPlaintext() throws Exception {
        FileService fileService = mock(FileService.class);
        byte[] plainBytes = "Top secret DCC source bytes".getBytes(StandardCharsets.UTF_8);
        when(fileService.getFileContent(7L, "dcc/published/source.pdf")).thenReturn(plainBytes);
        when(fileService.createFileAndReturnId(any(byte[].class), eq("source.pdf.dcc"),
                eq("dcc/encrypted-download"), eq("application/octet-stream")))
                .thenReturn(88001L, 88002L);
        DccAesGcmDownloadEncryptionGateway gateway = new DccAesGcmDownloadEncryptionGateway(
                fileService, properties(), objectMapper, new SecureRandom());
        DccDownloadEncryptionRequest request = request();

        DccDownloadEncryptionResult first = gateway.encrypt(request);
        DccDownloadEncryptionResult second = gateway.encrypt(request);

        assertEquals("READY", first.status());
        assertEquals("DCCDL-AE-20260528-0001", first.artifactId());
        assertEquals("infra-file:88001", first.cipherFileRef());
        assertEquals("dcc-download-aes-gcm-v1", first.encryptionPolicyVersion());
        assertEquals("source.pdf.dcc", first.cipherFileName());
        assertEquals("application/octet-stream", first.contentType());
        assertEquals(sha256Hex(plainBytes), first.plainSha256());
        assertEquals(sha256Hex(first.cipherBytes()), first.cipherSha256());
        assertNotEquals(Arrays.toString(first.cipherBytes()), Arrays.toString(second.cipherBytes()));
        assertFalse(new String(first.cipherBytes(), StandardCharsets.ISO_8859_1)
                .contains("Top secret DCC source bytes"));
        assertArrayEquals(plainBytes, decrypt(first.cipherBytes(), request, first.plainSha256()));

        ArgumentCaptor<byte[]> artifactCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(fileService, times(2)).createFileAndReturnId(artifactCaptor.capture(), eq("source.pdf.dcc"),
                eq("dcc/encrypted-download"), eq("application/octet-stream"));
        assertArrayEquals(first.cipherBytes(), artifactCaptor.getAllValues().get(0));
    }

    @Test
    void propertiesRejectMissingOrInvalidRequiredEncryptionConfig() {
        DccDownloadEncryptionProperties missingKey = properties();
        missingKey.setBase64Key(null);
        assertConfigException(missingKey, "base64-key");

        DccDownloadEncryptionProperties invalidBase64 = properties();
        invalidBase64.setBase64Key("not base64");
        assertConfigException(invalidBase64, "valid Base64");

        DccDownloadEncryptionProperties invalidLength = properties();
        invalidLength.setBase64Key(Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8)));
        assertConfigException(invalidLength, "16, 24, or 32 bytes");

        DccDownloadEncryptionProperties missingPolicy = properties();
        missingPolicy.setPolicyVersion(" ");
        assertConfigException(missingPolicy, "policy-version");

        DccDownloadEncryptionProperties missingKeyId = properties();
        missingKeyId.setKeyId(null);
        assertConfigException(missingKeyId, "key-id");

        DccDownloadEncryptionProperties missingArtifactDirectory = properties();
        missingArtifactDirectory.setArtifactDirectory("");
        assertConfigException(missingArtifactDirectory, "artifact-directory");
    }

    @Test
    void springContextWiresGatewayThroughProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(FileService.class, () -> mock(FileService.class));
            context.registerBean(DccDownloadEncryptionProperties.class, this::properties);
            context.registerBean(ObjectMapper.class, () -> objectMapper);
            context.register(DccAesGcmDownloadEncryptionGateway.class);

            context.refresh();

            assertTrue(context.getBean(DccDownloadEncryptionGateway.class)
                    instanceof DccAesGcmDownloadEncryptionGateway);
        }
    }

    private DccDownloadEncryptionProperties properties() {
        DccDownloadEncryptionProperties properties = new DccDownloadEncryptionProperties();
        properties.setPolicyVersion("dcc-download-aes-gcm-v1");
        properties.setKeyId("dcc-download-key-20260528");
        properties.setBase64Key(base64Key);
        properties.setArtifactDirectory("dcc/encrypted-download");
        return properties;
    }

    private DccDownloadEncryptionRequest request() {
        return new DccDownloadEncryptionRequest(
                31L,
                99L,
                908L,
                "1.0",
                "DR-20260528-0001",
                88008L,
                "AE-20260528-0001",
                "dcc-download-policy-v1",
                509L,
                7L,
                "dcc/published/source.pdf",
                "source.pdf",
                "application/pdf");
    }

    private void assertConfigException(DccDownloadEncryptionProperties properties, String expectedMessagePart) {
        ServiceException exception = assertThrows(ServiceException.class, properties::validateRuntimeConfig);
        assertEquals(DCC_DOWNLOAD_ENCRYPTION_CONFIG_MISSING.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains(expectedMessagePart));
    }

    private byte[] decrypt(byte[] envelope, DccDownloadEncryptionRequest request, String plainSha256) throws Exception {
        byte[] magic = "DCC-AES-GCM-V1\n".getBytes(StandardCharsets.US_ASCII);
        assertTrue(startsWith(envelope, magic));
        int headerEnd = findHeaderEnd(envelope, magic.length);
        Map<String, Object> header = objectMapper.readValue(Arrays.copyOfRange(envelope, magic.length, headerEnd),
                new TypeReference<>() {
                });
        byte[] iv = Base64.getDecoder().decode((String) header.get("ivBase64"));
        byte[] cipherText = Arrays.copyOfRange(envelope, headerEnd + 2, envelope.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(objectMapper.writeValueAsBytes(aad(request, plainSha256)));
        return cipher.doFinal(cipherText);
    }

    private Map<String, Object> aad(DccDownloadEncryptionRequest request, String plainSha256) {
        Map<String, Object> aad = new LinkedHashMap<>();
        aad.put("tenantId", request.tenantId());
        aad.put("userId", request.userId());
        aad.put("controlledFileId", request.controlledFileId());
        aad.put("fileVersionNo", request.fileVersionNo());
        aad.put("downloadRequestId", request.downloadRequestId());
        aad.put("accessEventId", request.accessEventId());
        aad.put("accessEventCode", request.accessEventCode());
        aad.put("downloadPolicyVersion", request.policyVersion());
        aad.put("encryptionPolicyVersion", "dcc-download-aes-gcm-v1");
        aad.put("sourceFileId", request.sourceFileId());
        aad.put("sourcePath", request.sourcePath());
        aad.put("plainSha256", plainSha256);
        return aad;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private int findHeaderEnd(byte[] bytes, int start) {
        for (int i = start; i < bytes.length - 1; i++) {
            if (bytes[i] == '\n' && bytes[i + 1] == '\n') {
                return i;
            }
        }
        throw new AssertionError("envelope header separator missing");
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
