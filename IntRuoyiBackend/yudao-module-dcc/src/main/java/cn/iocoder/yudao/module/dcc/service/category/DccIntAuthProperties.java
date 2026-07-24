package cn.iocoder.yudao.module.dcc.service.category;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_DIRECTORY_IMPORT_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_FILE_CATEGORY_SYNC_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_SYNC_CONFIG_MISSING;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.dcc.int-auth")
public class DccIntAuthProperties {

    private String baseUrl;
    private String internalServiceToken;
    private String dbPath;

    public void validateFileCategorySyncConfig() {
        requireNotBlank(baseUrl, "yudao.dcc.int-auth.base-url");
        requireNotBlank(internalServiceToken, "yudao.dcc.int-auth.internal-service-token");
    }

    public void validatePositionSyncConfig() {
        requireNotBlank(baseUrl, "yudao.dcc.int-auth.base-url", INTAUTH_POSITION_SYNC_CONFIG_MISSING);
        requireNotBlank(internalServiceToken, "yudao.dcc.int-auth.internal-service-token",
                INTAUTH_POSITION_SYNC_CONFIG_MISSING);
    }

    public void validateDirectoryImportConfig() {
        requireNotBlank(dbPath, "yudao.dcc.int-auth.db-path", INTAUTH_DIRECTORY_IMPORT_CONFIG_MISSING);
    }

    private static void requireNotBlank(String value, String propertyName) {
        requireNotBlank(value, propertyName, INTAUTH_FILE_CATEGORY_SYNC_CONFIG_MISSING);
    }

    private static void requireNotBlank(String value, String propertyName,
                                        cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        if (StrUtil.isBlank(value)) {
            throw exception(errorCode, propertyName);
        }
    }

}
