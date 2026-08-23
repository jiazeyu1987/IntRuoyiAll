package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest {

    @Mock
    private MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;

    private MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort port;

    @BeforeEach
    void setUp() {
        port = new MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl(receiptMapper);
    }

    @Test
    void missingReceiptMustFailWithoutInference() {
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> port.getByReceiptId(99L, 7L));
    }

    @Test
    void tenantMismatchMustBeInvisibleToFlow6() {
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> port.getByReceiptId(99L, 7L));
    }

    @Test
    void tamperedReceiptHashMustBeRejected() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt();
        receipt.setReceiptHash("tampered");
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);

        assertThrows(RuntimeException.class, () -> port.getByReceiptId(99L, 7L));
    }

    @Test
    void validReceiptExposesFrozenSourceAndLossFields() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt();
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);

        MesFlow6CompletionBackfillReceipt handoff = port.getByReceiptId(99L, 7L);

        assertEquals(7L, handoff.getTenantId());
        assertEquals("key-99", handoff.getRequestIdempotencyKey());
        assertEquals(LocalDateTime.of(2026, 8, 23, 10, 0), handoff.getCreatedAt());
        assertEquals("{\"formal\":true}", handoff.getFormalSourceSnapshotJson());
        assertEquals("{\"signature\":true}", handoff.getSignatureSnapshotJson());
        assertEquals(44L, handoff.getLossRecordId());
        assertEquals(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED, handoff.getStatus());
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO validReceipt() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .id(99L).activeOrderId(10L).workOrderId(30L).batchCode("BATCH-30")
                .routeId(40L).routeVersionId(41L).leaderUserId(20L)
                .requestIdempotencyKey("key-99").requestPayloadHash("payload-hash")
                .sourceSnapshotHash("source-hash")
                .formalSourceSnapshotJson("{\"formal\":true}")
                .signatureSnapshotJson("{\"signature\":true}")
                .expectedVersion(2).completedVersion(3)
                .receiptStatus("BACKFILL_SUCCEEDED")
                .completionStatus("SUCCESS").batchRecordStatus("SUCCESS")
                .processInspectionStatus("SUCCESS").lossReportStatus("SUCCESS")
                .hasActualLoss(true).lossQuantity(BigDecimal.ONE).lossRecordId(44L)
                .lossConditionFactsJson("[{\"status\":\"REQUIRED\"}]")
                .batchRecordSourceIdsJson("[1]").processInspectionSourceIdsJson("[2]")
                .lossSourceHash("loss-hash").provisionHandoff("PENDING_FLOW6")
                .completedAt(LocalDateTime.of(2026, 8, 23, 10, 0)).completedBy(20L)
                .build();
        receipt.setTenantId(7L);
        receipt.setCreateTime(LocalDateTime.of(2026, 8, 23, 10, 0));
        return receipt;
    }
}
