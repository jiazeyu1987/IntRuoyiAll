package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientFactory;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import cn.iocoder.yudao.module.infra.framework.file.core.enums.FileStorageEnum;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Locale;

/**
 * eDHR 最终归档使用运行时 EDHR_S3_* 配置，不绑定普通文件主配置或受保护展厅配置。
 */
@Component
public class MesEdhrArchiveProtectedStorage {

    public static final Long FILE_CONFIG_ID = -1_040_750_314L;

    private static final String ENV_ENDPOINT = "EDHR_S3_ENDPOINT";
    private static final String ENV_BUCKET = "EDHR_S3_BUCKET";
    private static final String ENV_REGION = "EDHR_S3_REGION";
    private static final String ENV_ACCESS_KEY = "EDHR_S3_ACCESS_KEY";
    private static final String ENV_SECRET_KEY = "EDHR_S3_SECRET_KEY";
    private static final String ENV_RETENTION_MODE = "EDHR_S3_RETENTION_MODE";
    private static final String ENV_RETAIN_UNTIL_DAYS = "EDHR_S3_RETAIN_UNTIL_DAYS";
    private static final String ENV_REQUIRE_LEGAL_HOLD = "EDHR_S3_REQUIRE_LEGAL_HOLD";
    private static final String ENV_DOMAIN = "EDHR_S3_DOMAIN";
    private static final String ENV_ENABLE_PATH_STYLE_ACCESS = "EDHR_S3_ENABLE_PATH_STYLE_ACCESS";

    @Resource
    private Environment environment;
    @Resource
    private FileClientFactory fileClientFactory;

    private volatile S3FileClientConfig registeredConfig;

    public Long getFileConfigId() {
        return FILE_CONFIG_ID;
    }

    public StorageRetentionPolicy requireUploadPolicy(String checksumSha256) {
        if (StrUtil.isBlank(checksumSha256)) {
            throw new IllegalArgumentException("eDHR 归档 SHA-256 不能为空");
        }
        S3FileClientConfig config = requireClientRegistered();
        return config.buildStorageRetentionPolicy()
                .setChecksumSha256(checksumSha256);
    }

    public S3FileClientConfig requireClientRegistered() {
        S3FileClientConfig config = registeredConfig;
        if (config != null) {
            return config;
        }
        synchronized (this) {
            if (registeredConfig == null) {
                registeredConfig = buildAndRegisterClient();
            }
            return registeredConfig;
        }
    }

    private S3FileClientConfig buildAndRegisterClient() {
        S3FileClientConfig config = new S3FileClientConfig();
        config.setEndpoint(requiredEnv(ENV_ENDPOINT));
        config.setDomain(optionalEnv(ENV_DOMAIN));
        config.setBucket(requiredEnv(ENV_BUCKET));
        config.setAccessKey(requiredEnv(ENV_ACCESS_KEY));
        config.setAccessSecret(requiredEnv(ENV_SECRET_KEY));
        config.setEnablePathStyleAccess(optionalBooleanEnv(ENV_ENABLE_PATH_STYLE_ACCESS, true));
        config.setEnablePublicAccess(Boolean.FALSE);
        config.setRegion(requiredEnv(ENV_REGION));
        config.setObjectLockRequired(Boolean.TRUE);
        config.setRetentionMode(requiredRetentionMode());
        config.setRetentionDays(requiredPositiveIntegerEnv(ENV_RETAIN_UNTIL_DAYS));
        config.setLegalHoldRequired(requiredBooleanEnv(ENV_REQUIRE_LEGAL_HOLD));

        fileClientFactory.createOrUpdateFileClient(FILE_CONFIG_ID, FileStorageEnum.S3.getStorage(), config);
        return config;
    }

    private String requiredEnv(String key) {
        String value = environment.getProperty(key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalStateException("缺少 " + key + "，eDHR 受保护归档存储不可用");
        }
        return value.trim();
    }

    private String optionalEnv(String key) {
        String value = environment.getProperty(key);
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private String requiredRetentionMode() {
        String mode = requiredEnv(ENV_RETENTION_MODE).toUpperCase(Locale.ROOT);
        if (!"GOVERNANCE".equals(mode) && !"COMPLIANCE".equals(mode)) {
            throw new IllegalStateException(ENV_RETENTION_MODE + " 必须是 GOVERNANCE 或 COMPLIANCE");
        }
        return mode;
    }

    private Integer requiredPositiveIntegerEnv(String key) {
        String value = requiredEnv(key);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(key + " 必须是正整数", ex);
        }
    }

    private Boolean requiredBooleanEnv(String key) {
        return parseBooleanEnv(key, requiredEnv(key));
    }

    private Boolean optionalBooleanEnv(String key, boolean defaultValue) {
        String value = optionalEnv(key);
        return value == null ? defaultValue : parseBooleanEnv(key, value);
    }

    private Boolean parseBooleanEnv(String key, String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized)
                || "y".equals(normalized) || "on".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized)
                || "n".equals(normalized) || "off".equals(normalized)) {
            return Boolean.FALSE;
        }
        throw new IllegalStateException(key + " 必须是 true 或 false");
    }

}
