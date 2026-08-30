package cn.iocoder.yudao.module.system.job.user;

import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLifecycleDeactivateJobTest {

    @Test
    void execute_withExplicitPositiveLimitProcessesDueUsers() {
        AdminUserService userService = mock(AdminUserService.class);
        when(userService.processDueLifecycleDeactivations(any(), any(Integer.class))).thenReturn(3);
        UserLifecycleDeactivateJob job = new UserLifecycleDeactivateJob(userService);

        String result = job.execute("{\"limit\":20}");

        assertEquals("用户离职/转岗到期停用处理数量：3，limit=20", result);
        verify(userService).processDueLifecycleDeactivations(any(), eq(20));
    }

    @Test
    void execute_withoutLimitFailsFast() {
        UserLifecycleDeactivateJob job = new UserLifecycleDeactivateJob(mock(AdminUserService.class));

        assertThrows(IllegalArgumentException.class, () -> job.execute(null));
        assertThrows(IllegalArgumentException.class, () -> job.execute(""));
        assertThrows(IllegalArgumentException.class, () -> job.execute("{}"));
        assertThrows(IllegalArgumentException.class, () -> job.execute("{\"limit\":0}"));
    }

}
