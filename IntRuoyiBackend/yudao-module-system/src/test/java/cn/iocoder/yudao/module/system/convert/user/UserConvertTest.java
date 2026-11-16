package cn.iocoder.yudao.module.system.convert.user;

import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UserConvertTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 3, 10, 0);

    @Test
    void projectsEffectiveLockWithoutChangingStoredState() {
        assertLock(1, now.minusMinutes(29), 1);
        assertLock(1, now.minusMinutes(30), 0);
        assertLock(1, now.minusMinutes(31), 0);
        assertLock(1, null, 0);
        assertLock(0, now, 0);
        assertLock(null, now, 0);
    }

    @Test
    void listAndDetailProjectExpiredLockConsistently() {
        AdminUserDO user = new AdminUserDO().setId(1L).setDeptId(7L)
                .setLoginLocked(1).setLoginLockedTime(LocalDateTime.now().minusMinutes(31));
        assertEquals(0, UserConvert.INSTANCE.convertList(List.of(user), Map.of()).get(0).getLoginLocked());
        assertEquals(0, UserConvert.INSTANCE.convert(user, (cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO) null)
                .getLoginLocked());
        assertEquals(0, UserConvert.INSTANCE.convertForUpdate(user, List.of()).getLoginLocked());
        assertEquals(1, user.getLoginLocked());
    }

    @Test
    void editDetailPreservesActiveLockAndStoredState() {
        LocalDateTime lockedAt = LocalDateTime.now();
        AdminUserDO user = new AdminUserDO().setId(1L).setLoginLocked(1).setLoginLockedTime(lockedAt);
        assertEquals(1, UserConvert.INSTANCE.convertForUpdate(user, List.of()).getLoginLocked());
        assertEquals(1, user.getLoginLocked());
        assertEquals(lockedAt, user.getLoginLockedTime());
    }

    private void assertLock(Integer stored, LocalDateTime lockedAt, int expected) {
        AdminUserDO user = new AdminUserDO().setId(1L).setLoginLocked(stored).setLoginLockedTime(lockedAt);
        var response = UserConvert.INSTANCE.convert(user, null, now);
        assertEquals(expected, response.getLoginLocked());
        assertEquals(stored, user.getLoginLocked());
        assertEquals(lockedAt, user.getLoginLockedTime());
        assertEquals(lockedAt, response.getLoginLockedTime());
    }
}
