package cn.iocoder.yudao.module.system.service.user;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void passwordAgeUses365DayGlobalWindow() {
        assertFalse(AdminUserPasswordPolicy.isExpired(NOW.minusDays(364), NOW));
        assertFalse(AdminUserPasswordPolicy.isExpired(NOW.minusDays(365), NOW));
        assertTrue(AdminUserPasswordPolicy.isExpired(NOW.minusDays(366), NOW));
    }

    @Test
    void missingPasswordUpdateTimeIsExpired() {
        assertTrue(AdminUserPasswordPolicy.isExpired(null, NOW));
    }
}
