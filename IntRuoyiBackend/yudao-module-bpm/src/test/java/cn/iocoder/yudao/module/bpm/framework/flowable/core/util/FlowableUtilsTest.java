package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlowableUtilsTest {

    @Test
    void preservesBusinessErrorAndClearsAuthenticatedUser() {
        var failure = new ServiceException(1009003002, "流程定义不存在");
        var thrown = assertThrows(ServiceException.class, () ->
                FlowableUtils.executeAuthenticatedUserId(99L, () -> {
                    assertEquals("99", Authentication.getAuthenticatedUserId());
                    throw failure;
                }));
        assertSame(failure, thrown);
        assertNull(Authentication.getAuthenticatedUserId());
    }

    @Test
    void preservesRuntimeCauseAndClearsAuthenticatedUser() {
        var failure = new IllegalStateException("engine failed");
        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                FlowableUtils.executeAuthenticatedUserId(99L, () -> { throw failure; })));
        assertNull(Authentication.getAuthenticatedUserId());
    }

    @Test
    void retainsCheckedCauseAndClearsAuthenticatedUser() {
        var failure = new java.io.IOException("engine IO failed");
        var thrown = assertThrows(RuntimeException.class, () ->
                FlowableUtils.executeAuthenticatedUserId(99L, () -> { throw failure; }));
        assertSame(failure, thrown.getCause());
        assertNull(Authentication.getAuthenticatedUserId());
    }
}
