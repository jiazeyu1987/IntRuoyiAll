package cn.iocoder.yudao.module.system.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.*;

class AdminUserPasswordPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 24, 12, 0);

    @Test
    void isStrongRequiresLengthUpperLowerDigitAndSpecialCharacter() {
        assertFalse(AdminUserPasswordPolicy.isStrong("Aa1!aaa"));
        assertFalse(AdminUserPasswordPolicy.isStrong("aa1!aaaa"));
        assertFalse(AdminUserPasswordPolicy.isStrong("AA1!AAAA"));
        assertFalse(AdminUserPasswordPolicy.isStrong("Aa!!aaaa"));
        assertFalse(AdminUserPasswordPolicy.isStrong("Aa11aaaa"));
        assertTrue(AdminUserPasswordPolicy.isStrong("Yudao@2026"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 30, 90, 180, 365})
    void passwordAgeUsesConfiguredWindow(int days) {
        String configuredDays = Integer.toString(days);
        LocalDateTime changedAt = NOW.minusDays(days);
        assertFalse(AdminUserPasswordPolicy.isExpired(changedAt, NOW.minusNanos(1), configuredDays));
        assertTrue(AdminUserPasswordPolicy.isExpired(changedAt, NOW, configuredDays));
        assertTrue(AdminUserPasswordPolicy.isExpired(changedAt, NOW.plusNanos(1), configuredDays));
    }

    @Test
    void missingPasswordUpdateTimeIsExpired() {
        assertTrue(AdminUserPasswordPolicy.isExpired(null, NOW, "90"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "1.5", " 90", "90 ", "090", "abc", "2147483648", "999999999999"})
    void invalidConfigurationCannotAuthenticate(String value) {
        assertServiceException(() -> AdminUserPasswordPolicy.isExpired(NOW, NOW, value),
                CONFIG_PASSWORD_MAX_AGE_INVALID);
    }

    @Test
    void missingConfigurationFailsInsteadOfUsing90Days() {
        assertServiceException(() -> AdminUserPasswordPolicy.isExpired(NOW, NOW, null),
                CONFIG_PASSWORD_MAX_AGE_MISSING);
    }
}
