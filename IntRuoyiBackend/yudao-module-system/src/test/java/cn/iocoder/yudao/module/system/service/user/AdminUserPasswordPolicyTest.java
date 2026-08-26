package cn.iocoder.yudao.module.system.service.user;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUserPasswordPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 24, 12, 0);

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
