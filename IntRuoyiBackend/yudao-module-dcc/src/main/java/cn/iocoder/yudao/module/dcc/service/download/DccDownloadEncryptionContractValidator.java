package cn.iocoder.yudao.module.dcc.service.download;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_ENCRYPTION_EVIDENCE_INVALID;

@Service
public class DccDownloadEncryptionContractValidator {

    private static final String READY_STATUS = "READY";
    private static final Pattern SHA256_HEX = Pattern.compile("^[a-fA-F0-9]{64}$");

    public DccDownloadEncryptionResult requireReadyEvidence(DccDownloadEncryptionResult result) {
        if (result == null
                || !READY_STATUS.equals(result.status())
                || StrUtil.isBlank(result.artifactId())
                || StrUtil.isBlank(result.cipherFileRef())
                || !isSha256(result.plainSha256())
                || !isSha256(result.cipherSha256())
                || StrUtil.isBlank(result.encryptionPolicyVersion())) {
            throw exception(DCC_DOWNLOAD_ENCRYPTION_EVIDENCE_INVALID);
        }
        return result;
    }

    public DccDownloadEncryptionResult requireReturnableArtifact(DccDownloadEncryptionResult result) {
        requireReadyEvidence(result);
        if (StrUtil.isBlank(result.cipherFileName())
                || StrUtil.isBlank(result.contentType())
                || result.cipherBytes() == null
                || result.cipherBytes().length == 0
                || !result.cipherSha256().equalsIgnoreCase(sha256Hex(result.cipherBytes()))) {
            throw exception(DCC_DOWNLOAD_ENCRYPTION_EVIDENCE_INVALID);
        }
        return result;
    }

    private boolean isSha256(String value) {
        return StrUtil.isNotBlank(value) && SHA256_HEX.matcher(value).matches();
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
