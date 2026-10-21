package cn.iocoder.yudao.module.infra.enums.config;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.CONFIG_PASSWORD_MAX_AGE_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.CONFIG_PASSWORD_MAX_AGE_MISSING;

/** 系统账号密码有效期的参数键与读写校验。 */
public final class PasswordMaxAgeConfig {

    public static final String KEY = "system.user.password-max-age-days";

    private PasswordMaxAgeConfig() {
    }

    public static int parseRequired(String value) {
        if (value == null) {
            throw exception(CONFIG_PASSWORD_MAX_AGE_MISSING);
        }
        if (!value.matches("[1-9][0-9]{0,9}")) {
            throw exception(CONFIG_PASSWORD_MAX_AGE_INVALID);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw exception(CONFIG_PASSWORD_MAX_AGE_INVALID);
        }
    }
}
