package cn.iocoder.yudao.module.dcc.service.download;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_ENCRYPTION_EVIDENCE_INVALID;

@Service
public class DccAesGcmDownloadEncryptionGateway implements DccDownloadEncryptionGateway {

    private static final String MAGIC = "DCC-AES-GCM-V1\n";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final FileService fileService;
    private final DccDownloadEncryptionProperties properties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;

    @Autowired
    public DccAesGcmDownloadEncryptionGateway(FileService fileService,
                                               DccDownloadEncryptionProperties properties,
                                               ObjectMapper objectMapper) {
        this(fileService, properties, objectMapper, new SecureRandom());
    }

    DccAesGcmDownloadEncryptionGateway(FileService fileService,
                                       DccDownloadEncryptionProperties properties,
                                       ObjectMapper objectMapper,
                                       SecureRandom secureRandom) {
        this.fileService = fileService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.secureRandom = secureRandom;
    }

    @Override
    public DccDownloadEncryptionResult encrypt(DccDownloadEncryptionRequest request) {
        requireRequest(request);
        properties.validateRuntimeConfig();
        byte[] sourceBytes = readSourceBytes(request);
        if (sourceBytes.length == 0) {
            throw exception(DCC_DOWNLOAD_ENCRYPTION_EVIDENCE_INVALID);
        }

        String plainSha256 = sha256Hex(sourceBytes);
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        String artifactId = artifactId(request.accessEventCode());
        String cipherFileName = cipherFileName(request.sourceFileName());
        byte[] cipherText = encrypt(sourceBytes, properties.requireAesKey(), iv,
                aad(request, plainSha256));
        byte[] cipherBytes = envelope(request, artifactId, plainSha256, iv, cipherText);
        String cipherSha256 = sha256Hex(cipherBytes);
        Long cipherFileId = fileService.createFileAndReturnId(cipherBytes, cipherFileName,
                StrUtil.trim(properties.getArtifactDirectory()), CONTENT_TYPE);
        if (cipherFileId == null) {
            throw new IllegalStateException("DCC encrypted artifact file id is missing");
        }
        return new DccDownloadEncryptionResult(
                "READY",
                artifactId,
                "infra-file:" + cipherFileId,
                plainSha256,
                cipherSha256,
                StrUtil.trim(properties.getPolicyVersion()),
                cipherFileName,
                CONTENT_TYPE,
                cipherBytes);
    }

    private byte[] readSourceBytes(DccDownloadEncryptionRequest request) {
        try {
            byte[] bytes = fileService.getFileContent(request.sourceConfigId(), request.sourcePath());
            if (bytes == null) {
                throw exception(DCC_DOWNLOAD_ENCRYPTION_EVIDENCE_INVALID);
            }
            return bytes;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("DCC source file read failed", ex);
        }
    }

    private byte[] encrypt(byte[] sourceBytes, byte[] key, byte[] iv, Map<String, Object> aad) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(objectMapper.writeValueAsBytes(aad));
            return cipher.doFinal(sourceBytes);
        } catch (GeneralSecurityException | JsonProcessingException ex) {
            throw new IllegalStateException("DCC download encryption failed", ex);
        }
    }

    private byte[] envelope(DccDownloadEncryptionRequest request, String artifactId, String plainSha256,
                            byte[] iv, byte[] cipherText) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("format", "DCC-AES-GCM-V1");
            header.put("algorithm", ALGORITHM);
            header.put("tagBits", GCM_TAG_BITS);
            header.put("artifactId", artifactId);
            header.put("keyId", StrUtil.trim(properties.getKeyId()));
            header.put("encryptionPolicyVersion", StrUtil.trim(properties.getPolicyVersion()));
            header.put("downloadPolicyVersion", request.policyVersion());
            header.put("tenantId", request.tenantId());
            header.put("userId", request.userId());
            header.put("controlledFileId", request.controlledFileId());
            header.put("fileVersionNo", request.fileVersionNo());
            header.put("downloadRequestId", request.downloadRequestId());
            header.put("accessEventId", request.accessEventId());
            header.put("accessEventCode", request.accessEventCode());
            header.put("sourceFileId", request.sourceFileId());
            header.put("sourceFileName", request.sourceFileName());
            header.put("sourceContentType", request.sourceContentType());
            header.put("plainSha256", plainSha256);
            header.put("ivBase64", Base64.getEncoder().encodeToString(iv));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(MAGIC.getBytes(StandardCharsets.US_ASCII));
            output.write(objectMapper.writeValueAsBytes(header));
            output.write('\n');
            output.write('\n');
            output.write(cipherText);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("DCC encrypted envelope build failed", ex);
        }
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
        aad.put("encryptionPolicyVersion", StrUtil.trim(properties.getPolicyVersion()));
        aad.put("sourceFileId", request.sourceFileId());
        aad.put("sourcePath", request.sourcePath());
        aad.put("plainSha256", plainSha256);
        return aad;
    }

    private void requireRequest(DccDownloadEncryptionRequest request) {
        if (request == null
                || request.tenantId() == null
                || request.userId() == null
                || request.controlledFileId() == null
                || StrUtil.isBlank(request.fileVersionNo())
                || StrUtil.isBlank(request.downloadRequestId())
                || request.accessEventId() == null
                || StrUtil.isBlank(request.accessEventCode())
                || StrUtil.isBlank(request.policyVersion())
                || request.sourceFileId() == null
                || request.sourceConfigId() == null
                || StrUtil.isBlank(request.sourcePath())
                || StrUtil.isBlank(request.sourceFileName())) {
            throw exception(DCC_DOWNLOAD_ENCRYPTION_EVIDENCE_INVALID);
        }
    }

    private String artifactId(String accessEventCode) {
        return "DCCDL-" + StrUtil.trim(accessEventCode)
                .replaceAll("[^A-Za-z0-9_-]", "_")
                .toUpperCase(Locale.ROOT);
    }

    private String cipherFileName(String sourceFileName) {
        String sanitized = StrUtil.trim(sourceFileName)
                .replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
        return sanitized.endsWith(".dcc") ? sanitized : sanitized + ".dcc";
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
