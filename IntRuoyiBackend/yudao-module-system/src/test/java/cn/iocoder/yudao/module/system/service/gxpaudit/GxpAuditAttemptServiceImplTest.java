package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GxpAuditAttemptServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private GxpAuditService gxpAuditService;

    @InjectMocks
    private GxpAuditAttemptServiceImpl attemptService;

    @Test
    void record_shouldAppendFailedAttemptWithTargetOperationAndError() {
        when(gxpAuditService.append(any())).thenReturn(new GxpAuditAppendResult(31L, 7L, "hash", false));

        GxpAuditAppendResult result = attemptService.record(GxpAuditAttemptCommand.builder()
                .attemptedOperationId("mes.production.submit")
                .serverAttemptId("attempt-31")
                .resultStatus("FAILED")
                .errorCode("VALIDATION_FAILED")
                .reason("业务校验失败")
                .requestId("request-31")
                .sourceType("SERVICE_METHOD")
                .sourceLocator("unit-test#record")
                .build());

        assertEquals(31L, result.eventId());
        ArgumentCaptor<GxpAuditCommand> captor = ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(gxpAuditService).append(captor.capture());
        GxpAuditCommand command = captor.getValue();
        assertEquals("gxp.attempt.record", command.getOperationId());
        assertEquals("FAILED", command.getResultStatus());
        assertEquals("VALIDATION_FAILED", command.getErrorCode());
        assertEquals("mes.production.submit", command.getAttemptedOperationId());
        assertEquals("GXP2:ATTEMPT:attempt-31", command.getIdempotencyKey());
    }

    @Test
    void record_shouldRejectSuccessAndMissingAttemptIdentity() {
        assertThrows(RuntimeException.class, () -> attemptService.record(GxpAuditAttemptCommand.builder()
                .attemptedOperationId("mes.production.submit")
                .serverAttemptId("attempt-32")
                .resultStatus("SUCCESS")
                .errorCode("INVALID")
                .requestId("request-32")
                .build()));
    }
}
