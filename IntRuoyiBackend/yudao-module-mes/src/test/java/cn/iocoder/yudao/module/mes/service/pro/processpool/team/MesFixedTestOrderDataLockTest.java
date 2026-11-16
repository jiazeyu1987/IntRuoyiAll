package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesFixedTestOrderDataLockTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final MesFixedTestOrderDataService service = new MesFixedTestOrderDataService(jdbc, null, null);
    private final String lookup = "SELECT * FROM erp_kingdee_production_pick_list_item WHERE tenant_id=? AND deleted=0 AND source_line_key=?";
    private final String lock = "SELECT * FROM erp_kingdee_production_pick_list_item WHERE id=? AND tenant_id=? AND deleted=0 AND source_line_key=? FOR UPDATE";

    private Map<String, Object> find() {
        return ReflectionTestUtils.invokeMethod(service, "existing", "erp_kingdee_production_pick_list_item",
                186L, Map.of("source_line_key", "FIXED-TEST-LINE"));
    }

    @Test
    void missingFixtureDoesNotLockTheGapUsedByErpInserts() {
        when(jdbc.queryForList(lookup, 186L, "FIXED-TEST-LINE")).thenReturn(List.of());
        assertNull(find());
        verify(jdbc).queryForList(lookup, 186L, "FIXED-TEST-LINE");
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void existingFixtureIsLockedByPrimaryKeyWithTenantAndIdentityRechecked() {
        Map<String, Object> before = Map.of("id", 42L, "actual_quantity", 1);
        Map<String, Object> current = Map.of("id", 42L, "actual_quantity", 2);
        when(jdbc.queryForList(lookup, 186L, "FIXED-TEST-LINE")).thenReturn(List.of(before));
        when(jdbc.queryForList(lock, 42L, 186L, "FIXED-TEST-LINE")).thenReturn(List.of(current));
        assertEquals(current, find());
        verify(jdbc).queryForList(lock, 42L, 186L, "FIXED-TEST-LINE");
    }

    @Test
    void concurrentIdentityChangeFailsInsteadOfTreatingTheRowAsMissing() {
        when(jdbc.queryForList(lookup, 186L, "FIXED-TEST-LINE")).thenReturn(List.of(Map.of("id", 42L)));
        when(jdbc.queryForList(lock, 42L, 186L, "FIXED-TEST-LINE")).thenReturn(List.of());
        assertThrows(RuntimeException.class, this::find);
    }

    @Test
    void ambiguousIdentityFailsBeforeLockingOrWriting() {
        when(jdbc.queryForList(lookup, 186L, "FIXED-TEST-LINE"))
                .thenReturn(List.of(Map.of("id", 42L), Map.of("id", 43L)));
        assertThrows(RuntimeException.class, this::find);
        verify(jdbc).queryForList(lookup, 186L, "FIXED-TEST-LINE");
        verifyNoMoreInteractions(jdbc);
    }
}
