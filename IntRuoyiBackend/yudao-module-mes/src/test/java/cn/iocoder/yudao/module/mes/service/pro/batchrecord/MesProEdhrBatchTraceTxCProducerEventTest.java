package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

class MesProEdhrBatchTraceTxCProducerEventTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void provisionedEventCreatesStableTxCWitnessCommand() {
        MesProEdhrBatchTraceTxCProducer producer =
                mock(MesProEdhrBatchTraceTxCProducer.class, CALLS_REAL_METHODS);
        doReturn(new MesProEdhrBatchTraceTxCResult()).when(producer)
                .produce(any(MesProEdhrBatchTraceTxCCommand.class));

        producer.onBatchExecutionProvisioned(new MesBatchExecutionProvisionedEvent(
                1L, 88L, "flow6-idem", "source-hash", "bundle-hash",
                "receipt-hash", "source-v1", 1001L));

        ArgumentCaptor<MesProEdhrBatchTraceTxCCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrBatchTraceTxCCommand.class);
        verify(producer).produce(captor.capture());
        MesProEdhrBatchTraceTxCCommand command = captor.getValue();
        assertEquals(88L, command.getBatchExecutionId());
        assertEquals("source-hash", command.getExpectedSourceSnapshotHash());
        assertEquals("bundle-hash", command.getExpectedSourceBundleHash());
        assertEquals("receipt-hash", command.getExpectedCompletionBackfillReceiptHash());
        assertEquals("source-v1", command.getExpectedSourceVersion());
        assertEquals(1001L, command.getCapturedBy());
        assertEquals("FLOW7-TXC-88-" + command.getIdempotencyKey().substring(command.getIdempotencyKey().length() - 32),
                command.getEventId());
    }
}
