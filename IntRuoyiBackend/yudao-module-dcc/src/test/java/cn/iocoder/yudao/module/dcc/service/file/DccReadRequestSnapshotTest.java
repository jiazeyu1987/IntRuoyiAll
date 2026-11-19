package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DccReadRequestSnapshotTest {
    @AfterEach void clear() {
        TenantContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    private void request(String method) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest(method, "/")));
    }

    @Test void reusesDeniedAndNullFactsButSeparatesTenantFactAndRequest() {
        TenantContextHolder.setTenantId(1L); request("GET");
        AtomicInteger calls = new AtomicInteger();
        assertFalse(DccReadRequestSnapshot.read("actor.1", () -> { calls.incrementAndGet(); return false; }));
        assertFalse(DccReadRequestSnapshot.read("actor.1", () -> true));
        assertNull(DccReadRequestSnapshot.read("scope.1", () -> { calls.incrementAndGet(); return null; }));
        assertNull(DccReadRequestSnapshot.read("scope.1", () -> "not cached"));
        assertTrue(DccReadRequestSnapshot.read("actor.2", () -> true));
        TenantContextHolder.setTenantId(2L);
        assertTrue(DccReadRequestSnapshot.read("actor.1", () -> true));
        TenantContextHolder.setTenantId(1L); request("GET");
        assertTrue(DccReadRequestSnapshot.read("actor.1", () -> true));
        assertEquals(2, calls.get());
    }

    @Test void writesAndBackgroundCallsAreFreshAndFailuresAreNotCached() {
        RequestContextHolder.resetRequestAttributes();
        TenantContextHolder.setTenantId(1L);
        AtomicInteger calls = new AtomicInteger();
        assertEquals(1, DccReadRequestSnapshot.read("fact", calls::incrementAndGet));
        assertEquals(2, DccReadRequestSnapshot.read("fact", calls::incrementAndGet));
        request("POST");
        assertEquals(3, DccReadRequestSnapshot.read("fact", calls::incrementAndGet));
        assertEquals(4, DccReadRequestSnapshot.read("fact", calls::incrementAndGet));
        request("GET");
        assertThrows(IllegalStateException.class, () -> DccReadRequestSnapshot.read("fact", () -> { throw new IllegalStateException("unavailable"); }));
        assertEquals(5, DccReadRequestSnapshot.read("fact", calls::incrementAndGet));
    }
}
