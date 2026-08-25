package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED;

class MesBatchExecutionAuthoritativeContextResolverTest {

    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionPort =
            mock(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class);
    private final MesIndependentBatchPrerequisiteReceiptService independentService =
            mock(MesIndependentBatchPrerequisiteReceiptService.class);
    private final MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper =
            mock(MesProcessPoolActiveOrderPickListBindingMapper.class);
    private final MesBatchExecutionAuthoritativeContextResolver resolver =
            new MesBatchExecutionAuthoritativeContextResolver(completionPort, independentService, pickListBindingMapper);

    @Test
    void forgedNestedCompletionReceiptWithoutServerCredentialIsBlocked() {
        MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION")
                .setEntryBusinessId("completion-1")
                .setSourceCredentialId("77")
                .setCompletionBackfillReceipt(new MesCompletionBackfillReceipt()
                        .setReceiptId("77").setStatus("BACKFILL_SUCCEEDED"));

        assertThrows(RuntimeException.class, () -> resolver.resolve(request, 1L));
    }

    @Test
    void activeEntryUsesFlow4AuthoritativeReceiptAndBlocksTamperedClientContext() {
        MesFlow6CompletionBackfillReceipt receipt = new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(77L).setTenantId(1L).setActiveOrderId(10L).setWorkOrderId(20L)
                .setBatchCode("B-77").setRouteId(30L).setRouteVersionId(31L)
                .setRequestIdempotencyKey("idem-77").setSourceSnapshotHash("source-77")
                .setCompletionVersion(2).setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setCreatedAt(LocalDateTime.now()).setReceiptHash("receipt-hash").setHasActualLoss(false);
        when(completionPort.getByReceiptId(77L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectByActiveOrderId(10L)).thenReturn(binding(10L, 20L));
        MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION").setEntryBusinessId("completion-1")
                .setSourceCredentialId("77").setSourceCredentialType("CompletionBackfillReceipt")
                .setWorkOrderId(999L).setBatchCode("CLIENT-FORGED").setRouteId(999L)
                .setSourceSnapshotHash("source-77");

        assertThrows(RuntimeException.class, () -> resolver.resolve(request, 1L));
        verify(completionPort).getByReceiptId(77L, 1L);
    }

    @Test
    void activeEntryCarriesFlow1PickListSnapshotIntoCanonicalCommand() {
        MesFlow6CompletionBackfillReceipt receipt = new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(78L).setTenantId(1L).setActiveOrderId(11L).setWorkOrderId(21L)
                .setBatchCode("B-78").setRouteId(30L).setRouteVersionId(31L)
                .setRequestIdempotencyKey("idem-78").setSourceSnapshotHash("source-78")
                .setCompletionVersion(1).setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setReceiptHash("receipt-78").setHasActualLoss(false)
                .setLossQuantity(java.math.BigDecimal.ZERO).setLossReportStatus("NO_LOSS")
                .setZeroLossConfirmationSnapshot("zero-loss-78");
        MesProcessPoolActiveOrderPickListBindingDO binding = binding(11L, 21L)
                .setId(8802L).setPickListId(9902L).setSourceSnapshotHash("pick-source-78");
        when(completionPort.getByReceiptId(78L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectByActiveOrderId(11L)).thenReturn(binding);

        MesBatchExecutionAuthoritativeContext resolved = resolver.resolve(new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION").setEntryBusinessId("completion-78")
                .setSourceCredentialType("CompletionBackfillReceipt").setSourceCredentialId("78")
                .setSourceSnapshotHash("source-78"), 1L);

        assertEquals(8802L, resolved.getProvisionCommand().getPickListBindingId());
        assertEquals(9902L, resolved.getProvisionCommand().getPickListId());
        assertEquals(1L, resolved.getProvisionCommand().getBindingVersion());
        assertEquals("pick-source-78", resolved.getPickListBinding().getSourceSnapshotHash());
    }

    @Test
    void activeEntryWithoutFormalFlow1BindingIsBlocked() {
        MesFlow6CompletionBackfillReceipt receipt = new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(79L).setTenantId(1L).setActiveOrderId(12L).setWorkOrderId(22L)
                .setBatchCode("B-79").setRouteId(30L).setRouteVersionId(31L)
                .setRequestIdempotencyKey("idem-79").setSourceSnapshotHash("source-79")
                .setCompletionVersion(1).setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setReceiptHash("receipt-79").setHasActualLoss(false)
                .setLossQuantity(java.math.BigDecimal.ZERO).setLossReportStatus("NO_LOSS")
                .setZeroLossConfirmationSnapshot("zero-loss-79");
        when(completionPort.getByReceiptId(79L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectByActiveOrderId(12L)).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class, () -> resolver.resolve(
                new MesBatchExecutionProvisionCommand().setEntryType("ACTIVE_ORDER_COMPLETION")
                        .setEntryBusinessId("completion-79").setSourceCredentialId("79")
                        .setSourceCredentialType("CompletionBackfillReceipt")
                        .setSourceSnapshotHash("source-79"), 1L));
        assertEquals(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED.getCode(), error.getCode());
    }

    @Test
    void everyActiveEntryTypeUsesFlow4ReceiptPort() {
        MesFlow6CompletionBackfillReceipt receipt = new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(77L).setTenantId(1L).setActiveOrderId(10L).setWorkOrderId(20L)
                .setBatchCode("B-77").setRouteId(30L).setRouteVersionId(31L)
                .setRequestIdempotencyKey("idem-77").setSourceSnapshotHash("source-77")
                .setCompletionVersion(2).setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setReceiptHash("receipt-hash").setHasActualLoss(false)
                .setLossQuantity(java.math.BigDecimal.ZERO).setLossReportStatus("NO_LOSS")
                .setZeroLossConfirmationSnapshot("zero-loss-confirmed");
        when(completionPort.getByReceiptId(77L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectByActiveOrderId(10L)).thenReturn(binding(10L, 20L));
        for (String entryType : java.util.List.of("ACTIVE_ORDER_COMPLETION", "ACTIVE_ORDER_SCHEDULED",
                "ACTIVE_ORDER_PQC", "MANUAL_CONTROLLED_RETRY")) {
            MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                    .setEntryType(entryType).setEntryBusinessId(entryType)
                    .setSourceCredentialId("77").setSourceCredentialType("CompletionBackfillReceipt")
                    .setSourceSnapshotHash("source-77");
            resolver.resolve(request, 1L);
        }
        verify(completionPort, org.mockito.Mockito.times(4)).getByReceiptId(77L, 1L);
    }

    @Test
    void independentEntryRejectsNestedClientReceiptBeforeFlow9() {
        MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                .setEntryType("MANUAL").setEntryBusinessId("manual-1").setSourceCredentialId("ind-1")
                .setSourceCredentialType("IndependentBatchPrerequisiteReceipt")
                .setIndependentReceipt(new MesIndependentBatchPrerequisiteReceipt()
                        .setReceiptId("ind-1").setBatchCode("FORGED"));

        assertThrows(RuntimeException.class, () -> resolver.resolve(request, 1L));
        verifyNoInteractions(independentService);
    }

    @Test
    void independentEntryUsesFlow9VerifiedReceiptAsCanonicalSource() {
        MesIndependentBatchPrerequisiteReceipt verified = independentReceipt();
        when(independentService.verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(verified);
        MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                .setEntryType("MANUAL").setEntryBusinessId("manual-1")
                .setSourceCredentialType("IndependentBatchPrerequisiteReceipt")
                .setSourceCredentialId("ind-1").setSourceSnapshotHash("source-1");

        MesBatchExecutionAuthoritativeContext resolved = resolver.resolve(request, 1L);

        assertEquals("B-20", resolved.getProvisionCommand().getBatchCode());
        assertEquals(verified, resolved.getProvisionCommand().getIndependentReceipt());
        verify(independentService).verify(org.mockito.ArgumentMatchers.argThat(command ->
                        "ind-1".equals(command.getReceiptId()) && "MANUAL".equals(command.getEntryType())),
                org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    void independentEntryBlocksCrossTenantVerifiedReceipt() {
        MesIndependentBatchPrerequisiteReceipt receipt = independentReceipt().setTenantId(2L);
        when(independentService.verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(receipt);
        MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                .setEntryType("MANUAL").setEntryBusinessId("manual-1")
                .setSourceCredentialId("ind-1").setSourceSnapshotHash("source-1");

        assertThrows(RuntimeException.class, () -> resolver.resolve(request, 1L));
    }

    @Test
    void everyIndependentEntryTypeUsesFlow9VerifiedService() {
        for (String entryType : java.util.List.of("MANUAL", "SCHEDULED", "PQC_INDEPENDENT")) {
            MesIndependentBatchPrerequisiteReceipt verified = independentReceipt().setEntryType(entryType);
            when(independentService.verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1L)))
                    .thenReturn(verified);
            MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                    .setEntryType(entryType).setEntryBusinessId(entryType)
                    .setSourceCredentialId("ind-1").setSourceSnapshotHash("source-1");
            resolver.resolve(request, 1L);
        }
        verify(independentService, org.mockito.Mockito.times(3)).verify(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1L));
    }

    private MesIndependentBatchPrerequisiteReceipt independentReceipt() {
        return new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId("ind-1").setTenantId(1L).setEntryType("MANUAL")
                .setWorkOrderId(20L).setWorkOrderCode("WO-20").setRouteId(30L)
                .setRouteVersionId(31L).setRouteVersion("1").setBatchCode("B-20")
                .setSourceRelationId("route-1").setSourceRelationVersion("1")
                .setSourceRelationSnapshotHash("relation-1").setSourceObjectType("WORK_ORDER")
                .setSourceObjectId("20").setMaterialSourceType("PICK_LIST").setMaterialSourceId("pick-1")
                .setSourceContextHash("ctx-1").setSourceSnapshotHash("source-1")
                .setBusinessReason("independent").setIssuerSystem("flow9").setIssuerUserId(9L)
                .setIssuerUserRole("SYSTEM").setIssuedAt(LocalDateTime.now().minusMinutes(1))
                .setExpiresAt(LocalDateTime.now().plusMinutes(10)).setCredentialVersion(1L)
                .setStatus("ISSUED").setReceiptHash("receipt-1").setPayloadHash("payload-1")
                .setSignature("signature-1").setAuditEventId("audit-1").setIdempotencyKey("idem-1")
                .setSourceEvidence(java.util.List.of(new MesBatchExecutionSourceEvidence()
                        .setSourceType("WORK_ORDER").setSourceId("20").setSourceVersion("1")
                        .setSourceSnapshotHash("source-1").setPayloadHash("payload-1").setSignature("signature-1")));
    }

    private MesProcessPoolActiveOrderPickListBindingDO binding(Long activeOrderId, Long workOrderId) {
        MesProcessPoolActiveOrderPickListBindingDO binding = new MesProcessPoolActiveOrderPickListBindingDO()
                .setId(8801L).setActiveOrderId(activeOrderId).setWorkOrderId(workOrderId)
                .setPickListId(9901L).setSourceSnapshotHash("pick-list-source-1")
                .setBindingStatus("BOUND").setBindingVersion(1);
        binding.setTenantId(1L);
        return binding;
    }
}
