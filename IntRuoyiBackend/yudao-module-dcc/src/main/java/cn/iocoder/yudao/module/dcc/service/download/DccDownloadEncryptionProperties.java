package cn.iocoder.yudao.module.dcc.service.download;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Base64;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_ENCRYPTION_CONFIG_MISSING;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.dcc.download.encryption")
public class DccDownloadEncryptionProperties {

    private String policyVersion;
    private String keyId;
    private String base64Key;
    private String artifactDirectory;

    @PostConstruct
    public void validateStartupConfig() {
        validateRuntimeConfig();
    }

    public void validateRuntimeConfig() {
        requireNotBlank(policyVersion, "policy-version");
        requireNotBlank(keyId, "key-id");
        requireNotBlank(artifactDirectory, "artifact-directory");
        requireAesKey();
    }

    public byte[] requireAesKey() {
        requireNotBlank(base64Key, "base64-key");
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(StrUtil.trim(base64Key));
        } catch (IllegalArgumentException ex) {
            throw exception(DCC_DOWNLOAD_ENCRYPTION_CONFIG_MISSING, "base64-key must be valid Base64");
        }
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw exception(DCC_DOWNLOAD_ENCRYPTION_CONFIG_MISSING,
                    "base64-key must decode to 16, 24, or 32 bytes");
        }
        return keyBytes.clone();
    }

    private void requireNotBlank(String value, String propertyName) {
        if (StrUtil.isBlank(value)) {
            throw exception(DCC_DOWNLOAD_ENCRYPTION_CONFIG_MISSING, propertyName + " is required");
        }
    }
}
