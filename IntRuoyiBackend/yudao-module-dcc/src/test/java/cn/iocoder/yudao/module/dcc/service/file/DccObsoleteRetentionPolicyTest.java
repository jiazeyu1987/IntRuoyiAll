package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccObsoleteRetentionPolicyTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileNameClaimService identities;
    @InjectMocks DccObsoleteRetentionService retention;
    @Test void twentyYearsAreCountedFromActualObsoleteTimestamp() {
        var obsoleteAt=LocalDateTime.of(2026,10,1,8,15,30);
        retention.retain(1L,10L,obsoleteAt);
        verify(identities).retainObsoleteIdentity(1L,10L,obsoleteAt,LocalDateTime.of(2046,10,1,8,15,30));
        verify(identities,never()).release(anyLong(),anyLong());
    }
    @Test void leapDayUsesCalendarYearsAndPreservesTime() {
        assertEquals(LocalDateTime.of(2020,2,29,9,0),retention.retainedUntil(LocalDateTime.of(2000,2,29,9,0)));
        assertEquals(LocalDateTime.of(2100,2,28,9,0),retention.retainedUntil(LocalDateTime.of(2080,2,29,9,0)));
    }
    @Test void missingTimestampDoesNotReserveGuessedDeadline() {
        assertThrows(IllegalArgumentException.class,()->retention.retain(1L,10L,null));
        verifyNoInteractions(identities);
    }
}
