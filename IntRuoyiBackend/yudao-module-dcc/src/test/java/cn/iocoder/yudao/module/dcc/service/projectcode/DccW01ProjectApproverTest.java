package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccW01ProjectApproverTest {
    @Test void nonFormalApproverCannotApproveOrRetryAnyWrite() {
        var actors = mock(DccProjectProductActorSupport.class);
        var states = mock(DccProjectProductCreateStateService.class);
        var writer = mock(DccProjectProductCreateWriteService.class);
        var service = new DccProjectProductCreateServiceImpl();
        ReflectionTestUtils.setField(service, "projectActors", actors);
        ReflectionTestUtils.setField(service, "stateService", states);
        ReflectionTestUtils.setField(service, "writeService", writer);
        when(actors.isApprover(7L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.approve(7L, 101L, "申请批准", true));
        assertThrows(RuntimeException.class, () -> service.retryWrite(7L, 101L, "再次写入"));
        verify(actors, times(2)).isApprover(7L);
        verifyNoInteractions(states, writer);
    }
}
