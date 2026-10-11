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
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest {

    @Mock
    private MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;

    @Mock
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationVerificationService creationVerifier;
    private MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort port;
    private MesProcessPoolActiveOrderCompletionReceiptDO currentReceipt;

    @BeforeEach
    void setUp() {
        port = new MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl(receiptMapper, creationVerifier);
        org.mockito.Mockito.lenient().when(creationVerifier.requireCompletionCreation(7L, 99L))
                .thenAnswer(invocation -> new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationEvidence(
                        "COMPLETION_MAIN_V1", 99L, "99", "3", currentReceipt.getReceiptHash(), 881L, "main-event-hash"));
        org.mockito.Mockito.lenient().when(creationVerifier.requireCompletionTransactionId(7L, 99L))
                .thenReturn("actual-main-transaction");
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
        assertEquals("actual-main-transaction", handoff.getCompletionTransactionId());
        assertEquals("881", handoff.getCompletionEventId());
        assertEquals("881", handoff.getAuditEventId());
        assertEquals("key-99", handoff.getRequestIdempotencyKey());
        assertEquals(LocalDateTime.of(2026, 8, 23, 10, 0), handoff.getCreatedAt());
        assertEquals(receipt.getFormalSourceSnapshotJson(), handoff.getFormalSourceSnapshotJson());
        assertEquals("{\"signature\":true}", handoff.getSignatureSnapshotJson());
        assertEquals(44L, handoff.getLossRecordId());
        assertEquals(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED, handoff.getStatus());
        assertEquals("SUCCESS", handoff.getBatchRecordStatus());
        assertEquals("SUCCESS", handoff.getProcessInspectionStatus());
        assertEquals("SUCCESS", handoff.getLossReportStatus());
        assertEquals(receipt.getSourceSnapshotHash(), handoff.getSourceSnapshotHash());
        assertEquals(84L, handoff.getPickListSources().get(0).getPickListBindingId());
        assertEquals(85L, handoff.getPickListSources().get(0).getPickListId());
        assertEquals(3, handoff.getCompletionVersion());
        assertEquals(receipt.getReceiptHash(), handoff.getReceiptHash());
        assertEquals(101L, handoff.getBatchRecordId());
        assertEquals(102L, handoff.getProcessInspectionId());
    }

    @Test
    void activeOrderLookupUsesTheLockedOwnerQuery() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt();
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByActiveOrderIdAndTenantIdForUpdate(10L, 7L)).thenReturn(receipt);

        MesFlow6CompletionBackfillReceipt handoff = port.getByActiveOrderId(10L, 7L);

        assertEquals(99L, handoff.getReceiptId());
        org.mockito.Mockito.verify(receiptMapper).selectByActiveOrderIdAndTenantIdForUpdate(10L, 7L);
    }

    @Test
    void receiptHashMustSurviveDatabaseDatetimePrecision() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt()
                .setCompletedAt(LocalDateTime.of(2026, 8, 23, 10, 0, 0, 123_456_789));
        String receiptHash = MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt);
        receipt.setReceiptHash(receiptHash);
        receipt.setCompletedAt(receipt.getCompletedAt().truncatedTo(ChronoUnit.SECONDS));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);

        MesFlow6CompletionBackfillReceipt handoff = port.getByReceiptId(99L, 7L);

        assertEquals(receiptHash, handoff.getReceiptHash());
    }

    @Test
    void receiptHashMustSurviveDatabaseDecimalScale() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt()
                .setLossQuantity(new BigDecimal("1"));
        String receiptHash = MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt);
        receipt.setReceiptHash(receiptHash);
        receipt.setLossQuantity(new BigDecimal("1.000000"));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);

        MesFlow6CompletionBackfillReceipt handoff = port.getByReceiptId(99L, 7L);

        assertEquals(receiptHash, handoff.getReceiptHash());
    }

    @Test
    void receiptHashMustSurviveDatabaseJsonNormalization() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt()
                .setSignatureSnapshotJson("{\"signatures\":[{\"userId\":20,\"type\":\"PQC\"}]}")
                .setLossConditionFactsJson("[{\"status\":\"REQUIRED\",\"lossQuantity\":1}]");
        String receiptHash = MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt);
        receipt.setReceiptHash(receiptHash);
        receipt.setFormalSourceSnapshotJson(receipt.getFormalSourceSnapshotJson().replace(":", " : ").replace(",", ", "));
        receipt.setSignatureSnapshotJson("{\"signatures\" : [ { \"type\" : \"PQC\", \"userId\" : 20.000000 } ]}");
        receipt.setLossConditionFactsJson("[ { \"lossQuantity\" : 1.000000, \"status\" : \"REQUIRED\" } ]");
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);

        MesFlow6CompletionBackfillReceipt handoff = port.getByReceiptId(99L, 7L);

        assertEquals(receiptHash, handoff.getReceiptHash());
    }

    @Test
    void incompleteBackfillStatusMustBlockFlow6() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt()
                .setBatchRecordStatus("BLOCKED");
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);

        assertThrows(RuntimeException.class, () -> port.getByReceiptId(99L, 7L));
    }

    @Test
    void missingFormalBackfillResultIdMustBlockFlow6() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt()
                .setProcessInspectionId(null);
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);

        assertThrows(RuntimeException.class, () -> port.getByReceiptId(99L, 7L));
    }

    @Test
    void anyNonSuccessReceiptStateMustBlockFlow6() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = validReceipt()
                .setReceiptStatus("PENDING_FLOW6");
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);

        assertThrows(RuntimeException.class, () -> port.getByReceiptId(99L, 7L));
    }

    @Test
    void missingMainOrAnyPartBlocksHashValidReceipt() {
        var receipt = validReceipt();
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);
        when(creationVerifier.requireCompletionCreation(7L, 99L)).thenThrow(new IllegalStateException("missing part"));
        assertThrows(IllegalStateException.class, () -> port.getByReceiptId(99L, 7L));
    }

    @Test
    void blankActualTransactionCannotBeReplacedByIdempotencyKey() {
        var receipt = validReceipt();
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);
        when(creationVerifier.requireCompletionTransactionId(7L, 99L)).thenReturn(" ");
        assertThrows(IllegalStateException.class, () -> port.getByReceiptId(99L, 7L));
    }

    @Test
    void verifiedFactContainsTheOriginalStrictCreationIdentity() {
        var receipt = validReceipt();
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);
        var fact = port.requireVerifiedByReceiptId(99L, 7L);
        assertEquals(99L, fact.receipt().getReceiptId());
        assertEquals(881L, fact.creation().creationEventId());
        assertEquals("actual-main-transaction", fact.transactionId());
    }

    @Test
    void missingOrUnrelatedFrozenBindingsFailEvenWithHashValidOwnerReceipt() {
        for (String raw : new String[]{"{}", validReceipt().getFormalSourceSnapshotJson().replace("\"BOUND\"", "\"UNBOUND\""),
                validReceipt().getFormalSourceSnapshotJson().replace("\"pickListId\":85", "\"pickListId\":0"),
                validReceipt().getFormalSourceSnapshotJson().replace("\"activeOrderId\":10", "\"activeOrderId\":11")}) {
            var receipt = validReceipt().setFormalSourceSnapshotJson(raw);
            receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
            when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);
            assertThrows(IllegalStateException.class, () -> port.getByReceiptId(99L, 7L));
        }
    }

    @Test
    void bindingMustBeAnActualFrozenBackfillSource() {
        var receipt = validReceipt().setBatchRecordSourceIdsJson("[1]");
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(receiptMapper.selectByIdAndTenantId(99L, 7L)).thenReturn(receipt);
        assertThrows(IllegalStateException.class, () -> port.getByReceiptId(99L, 7L));
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO validReceipt() {
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .id(99L).activeOrderId(10L).workOrderId(30L).batchCode("BATCH-30")
                .routeId(40L).routeVersionId(41L).leaderUserId(20L)
                .requestIdempotencyKey("key-99").requestPayloadHash("payload-hash")
                .sourceSnapshotHash("source-hash")
                .formalSourceSnapshotJson("{\"activeOrderBinding\":{\"id\":10,\"tenantId\":7,\"workOrderId\":30,\"routeId\":40,\"routeVersionId\":41},"
                        + "\"workOrderBinding\":{\"id\":30,\"batchCode\":\"BATCH-30\"},\"pickListBindings\":[{\"id\":84,\"tenantId\":7,\"activeOrderId\":10,\"workOrderId\":30,\"pickListId\":85,\"bindingVersion\":1,\"bindingStatus\":\"BOUND\",\"sourceSnapshotHash\":\"" + "a".repeat(64) + "\"}]}")
                .signatureSnapshotJson("{\"signature\":true}")
                .expectedVersion(2).completedVersion(3)
                .receiptStatus("BACKFILL_SUCCEEDED")
                .completionStatus("SUCCESS").batchRecordStatus("SUCCESS")
                .processInspectionStatus("SUCCESS").lossReportStatus("SUCCESS")
                .batchRecordId(101L).processInspectionId(102L)
                .hasActualLoss(true).lossQuantity(BigDecimal.ONE).lossRecordId(44L)
                .lossConditionFactsJson("[{\"status\":\"REQUIRED\"}]")
                .batchRecordSourceIdsJson("[84,1]").processInspectionSourceIdsJson("[2]")
                .lossSourceHash("loss-hash").provisionHandoff("PENDING_FLOW6")
                .completedAt(LocalDateTime.of(2026, 8, 23, 10, 0)).completedBy(20L)
                .build();
        receipt.setTenantId(7L);
        receipt.setCreateTime(LocalDateTime.of(2026, 8, 23, 10, 0));
        currentReceipt = receipt;
        return receipt;
    }
}
