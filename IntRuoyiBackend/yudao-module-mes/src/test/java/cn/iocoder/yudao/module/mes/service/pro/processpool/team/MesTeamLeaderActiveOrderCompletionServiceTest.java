package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderCompletionServiceTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;
    @Mock
    private MesTeamLeaderActiveOrderCompletionBackfillPort backfillPort;
    @Mock
    private MesTeamLeaderActiveOrderCompletionProgressPort progressPort;

    private MesTeamLeaderActiveOrderCompletionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderCompletionServiceImpl(activeOrderMapper, receiptMapper,
                progressPort, backfillPort);
    }

    @Test
    void sourceAdapterFailureMustNotChangeOrderOrCreateReceipt() {
        MesProcessPoolActiveOrderDO order = order();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(null);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(null);
        when(progressPort.read(anyLong(), any())).thenReturn(progress());
        when(backfillPort.prepare(anyLong(), any(), any())).thenThrow(new IllegalStateException("source missing"));

        assertThrows(RuntimeException.class, () -> service.complete(20L, command()));

        verify(backfillPort, never()).write(any(), anyLong());
        verify(activeOrderMapper, never()).markCompleted(anyLong(), any(), anyLong());
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void txAWriteFailureMustNotMarkOrderOrCreateReceipt() {
        MesProcessPoolActiveOrderDO order = order();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(null);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(null);
        when(progressPort.read(anyLong(), any())).thenReturn(progress());
        when(backfillPort.prepare(anyLong(), any(), any())).thenReturn(draft());
        org.mockito.Mockito.doThrow(new IllegalStateException("process inspection write failed"))
                .when(backfillPort).write(any(), anyLong());

        assertThrows(RuntimeException.class, () -> service.complete(20L, command()));

        verify(activeOrderMapper, never()).markCompleted(anyLong(), any(), anyLong());
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void successfulCompletionWritesReceiptAndOnlyReturnsFlow6Handoff() {
        MesProcessPoolActiveOrderDO order = order();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(null);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(null);
        when(progressPort.read(anyLong(), any())).thenReturn(progress());
        when(backfillPort.prepare(anyLong(), any(), any())).thenReturn(draft());
        when(activeOrderMapper.markCompleted(10L, 2, 20L)).thenReturn(1);
        when(receiptMapper.insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class))).thenAnswer(invocation -> {
            MesProcessPoolActiveOrderCompletionReceiptDO receipt = invocation.getArgument(0);
            receipt.setId(99L);
            return 1;
        });

        MesTeamLeaderActiveOrderCompletionResult result = service.complete(20L, command());

        assertEquals(99L, result.getCompletionReceiptId());
        assertEquals("PENDING_FLOW6", result.getProvisionHandoff());
        verify(backfillPort).write(any(), anyLong());
        verify(activeOrderMapper).markCompleted(10L, 2, 20L);
        org.mockito.ArgumentCaptor<MesProcessPoolActiveOrderCompletionReceiptDO> receiptCaptor =
                org.mockito.ArgumentCaptor.forClass(MesProcessPoolActiveOrderCompletionReceiptDO.class);
        verify(receiptMapper).insert(receiptCaptor.capture());
        assertEquals(1L, receiptCaptor.getValue().getTenantId());
        assertEquals(101L, receiptCaptor.getValue().getBatchRecordId());
        assertEquals(102L, receiptCaptor.getValue().getProcessInspectionId());
    }

    @Test
    void writerWithoutFormalResultIdsMustNotMarkOrderOrCreateReceipt() {
        MesProcessPoolActiveOrderDO order = order();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(null);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(null);
        when(progressPort.read(anyLong(), any())).thenReturn(progress());
        when(backfillPort.prepare(anyLong(), any(), any())).thenReturn(draft()
                .setBatchRecordId(null).setProcessInspectionId(null));

        assertThrows(RuntimeException.class, () -> service.complete(20L, command()));

        verify(backfillPort).write(any(), anyLong());
        verify(activeOrderMapper, never()).markCompleted(anyLong(), any(), anyLong());
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void blockedOrIncompleteLossDraftMustNotWriteAnything() {
        MesProcessPoolActiveOrderDO order = order();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(null);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(null);
        when(progressPort.read(anyLong(), any())).thenReturn(progress());
        when(backfillPort.prepare(anyLong(), any(), any()))
                .thenReturn(draft().setLossConditionFactsJson("[{\"processId\":1,\"status\":\"BLOCKED\","
                        + "\"hasActualLoss\":false,\"lossQuantity\":0,\"sourceHash\":\"loss-source-1\"}]"));

        assertThrows(RuntimeException.class, () -> service.complete(20L, command()));

        verify(backfillPort, never()).write(any(), anyLong());
        verify(activeOrderMapper, never()).markCompleted(anyLong(), any(), anyLong());
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void positiveLossRequiresFormalRecordAndPersistsSuccessStatus() {
        MesProcessPoolActiveOrderDO order = order();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(null);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(null);
        when(progressPort.read(anyLong(), any())).thenReturn(progress());
        when(backfillPort.prepare(anyLong(), any(), any())).thenReturn(draft()
                .setLossReportStatus("SUCCESS")
                .setHasActualLoss(true)
                .setLossQuantity(BigDecimal.ONE)
                .setLossRecordId(44L)
                .setZeroLossConfirmationSnapshot(null)
                .setLossConditionFactsJson("[{\"processId\":1,\"status\":\"REQUIRED\","
                        + "\"hasActualLoss\":true,\"lossQuantity\":1,\"lossRecordId\":44,"
                        + "\"sourceHash\":\"loss-source-1\"}]"));
        when(activeOrderMapper.markCompleted(10L, 2, 20L)).thenReturn(1);
        when(receiptMapper.insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class))).thenAnswer(invocation -> {
            MesProcessPoolActiveOrderCompletionReceiptDO receipt = invocation.getArgument(0);
            receipt.setId(100L);
            return 1;
        });

        MesTeamLeaderActiveOrderCompletionResult result = service.complete(20L, command());

        assertEquals("SUCCESS", result.getLossReportStatus());
        assertEquals(BigDecimal.ONE, result.getLossQuantity());
        verify(backfillPort).write(any(), anyLong());
        verify(receiptMapper).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void sameKeyReplayReturnsImmutableReceiptAfterVersionIncrement() {
        MesProcessPoolActiveOrderDO order = order().setVersion(3);
        MesProcessPoolActiveOrderCompletionReceiptDO existing = MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .id(99L).activeOrderId(10L).requestIdempotencyKey("key-1")
                .requestPayloadHash("bdb037e6af3b4afeb31311f003a442327dd3b8b6ab02f520aacd0f1129032d7a")
                .sourceSnapshotHash("source-hash").completedVersion(3)
                .provisionHandoff("PENDING_FLOW6").build();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(existing);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(existing);
        when(backfillPort.readSourceSnapshotHash(anyLong(), any(), any())).thenReturn("source-hash");

        MesTeamLeaderActiveOrderCompletionResult result = service.complete(20L,
                new MesTeamLeaderActiveOrderCompletionCommand().setActiveOrderId(10L)
                        .setExpectedVersion(2).setIdempotencyKey("key-1"));

        assertEquals(99L, result.getCompletionReceiptId());
        verify(backfillPort, never()).prepare(anyLong(), any(), any());
        verify(activeOrderMapper, never()).markCompleted(anyLong(), any(), anyLong());
    }

    @Test
    void sameKeyReplayWithChangedFormalSourceMustFailInsteadOfReturningStaleReceipt() {
        MesProcessPoolActiveOrderDO order = order().setVersion(3);
        MesProcessPoolActiveOrderCompletionReceiptDO existing = MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .id(99L).activeOrderId(10L).requestIdempotencyKey("key-1")
                .requestPayloadHash("bdb037e6af3b4afeb31311f003a442327dd3b8b6ab02f520aacd0f1129032d7a")
                .sourceSnapshotHash("source-hash-before-change").completedVersion(3)
                .provisionHandoff("PENDING_FLOW6").build();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(existing);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(existing);
        when(backfillPort.readSourceSnapshotHash(anyLong(), any(), any()))
                .thenReturn("source-hash-after-change");

        assertThrows(RuntimeException.class, () -> service.complete(20L,
                new MesTeamLeaderActiveOrderCompletionCommand().setActiveOrderId(10L)
                        .setExpectedVersion(2).setIdempotencyKey("key-1")));

        verify(backfillPort).readSourceSnapshotHash(anyLong(), any(), any());
        verify(backfillPort, never()).prepare(anyLong(), any(), any());
        verify(backfillPort, never()).write(any(), anyLong());
        verify(activeOrderMapper, never()).markCompleted(anyLong(), any(), anyLong());
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void nonOwnerCannotReplayExistingReceipt() {
        MesProcessPoolActiveOrderDO order = order();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);

        assertThrows(RuntimeException.class, () -> service.complete(999L,
                new MesTeamLeaderActiveOrderCompletionCommand().setActiveOrderId(10L)
                        .setExpectedVersion(2).setIdempotencyKey("key-1")));

        verify(receiptMapper, never()).selectByActiveOrderIdForUpdate(anyLong());
        verify(receiptMapper, never()).selectByIdempotencyKeyForUpdate(any());
        verify(backfillPort, never()).prepare(anyLong(), any(), any());
    }

    @Test
    void incompleteProgressMustFailBeforeAnyBackfillWrite() {
        MesProcessPoolActiveOrderDO order = order();
        when(activeOrderMapper.selectByIdForUpdate(10L)).thenReturn(order);
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(null);
        when(receiptMapper.selectByIdempotencyKeyForUpdate("key-1")).thenReturn(null);
        when(progressPort.read(anyLong(), any())).thenReturn(
                progress().setProductionProgressPercent(BigDecimal.valueOf(99)));

        assertThrows(RuntimeException.class, () -> service.complete(20L, command()));

        verify(backfillPort, never()).prepare(anyLong(), any(), any());
        verify(backfillPort, never()).write(any(), anyLong());
        verify(activeOrderMapper, never()).markCompleted(anyLong(), any(), anyLong());
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    private MesTeamLeaderActiveOrderCompletionCommand command() {
        return new MesTeamLeaderActiveOrderCompletionCommand()
                .setActiveOrderId(10L).setExpectedVersion(2).setIdempotencyKey("key-1");
    }

    private MesProcessPoolActiveOrderDO order() {
        MesProcessPoolActiveOrderDO order = MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(20L)
                .workOrderId(30L).activeStatus("ACTIVE").businessStatus("PRODUCING").version(2).build();
        order.setTenantId(1L);
        return order;
    }

    private MesTeamLeaderActiveOrderCompletionBackfillDraft draft() {
        return new MesTeamLeaderActiveOrderCompletionBackfillDraft()
                .setWorkOrderId(30L).setBatchCode("BATCH-30").setRouteId(40L).setRouteVersionId(41L)
                .setSourceSnapshotHash("source-hash").setFormalSourceSnapshotJson("{\"formal\":true}")
                .setSignatureSnapshotJson("{\"signature\":true}").setBatchRecordSourceIdsJson("[1]")
                .setProcessInspectionSourceIdsJson("[2]").setBatchRecordStatus("SUCCESS")
                .setProcessInspectionStatus("SUCCESS").setLossReportStatus("NOT_REQUIRED")
                .setBatchRecordId(101L).setProcessInspectionId(102L)
                .setHasActualLoss(false).setLossQuantity(BigDecimal.ZERO)
                .setZeroLossConfirmationSnapshot("{\"confirmed\":true}")
                .setLossConditionFactsJson("[{\"processId\":1,\"status\":\"NO_LOSS\","
                        + "\"hasActualLoss\":false,\"lossQuantity\":0,"
                        + "\"zeroLossConfirmationSnapshot\":\"{\\\"confirmed\\\":true}\","
                        + "\"sourceHash\":\"loss-source-1\"}]");
    }

    private MesTeamLeaderActiveOrderCompletionProgress progress() {
        return new MesTeamLeaderActiveOrderCompletionProgress()
                .setProductionProgressPercent(BigDecimal.valueOf(100))
                .setInspectionProgressPercent(BigDecimal.valueOf(100));
    }
}
