package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class DccWorkflowDateBusinessErrorTest {
    private final DccWorkflowDatePolicy policy = new DccWorkflowDatePolicy() {
        @Override public LocalDateTime now() { return LocalDateTime.of(2026, 10, 8, 0, 0, 15); }
    };
    @Test void crossedMidnightAndMissingDateAreBusinessErrorsNotGeneric500() {
        for (LocalDate date : new LocalDate[]{null, LocalDate.of(2026, 10, 7)}) {
            ServiceException error = assertThrows(ServiceException.class, () -> policy.requireReviewDate(date));
            assertTrue(error.getMessage().contains("生效日期为空或已过"));
            assertTrue(error.getMessage().contains("调整申请"));
        }
    }
    @Test void todayAndFutureRemainValidWithoutSilentlyChangingInput() {
        assertDoesNotThrow(() -> policy.requireReviewDate(LocalDate.of(2026, 10, 8)));
        assertDoesNotThrow(() -> policy.requireReviewDate(LocalDate.of(2026, 10, 16)));
    }
    @Test void missingBusinessZoneIsStillAnExplicitConfigurationFailure() {
        assertThrows(IllegalStateException.class, () -> new DccWorkflowDatePolicy().requireReviewDate(LocalDate.of(2026, 10, 8)));
    }
}
