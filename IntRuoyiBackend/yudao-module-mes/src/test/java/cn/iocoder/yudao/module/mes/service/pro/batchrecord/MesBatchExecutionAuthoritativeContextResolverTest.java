package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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
        MesFlow6CompletionBackfillReceipt receipt = validActiveReceipt(77L, 10L, 20L, "B-77", "source-77");
        when(completionPort.getByReceiptId(77L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectListByActiveOrderId(10L)).thenReturn(List.of(binding(10L, 20L)));
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
        MesFlow6CompletionBackfillReceipt receipt = validActiveReceipt(78L, 11L, 21L, "B-78", "source-78");
        MesProcessPoolActiveOrderPickListBindingDO binding = binding(11L, 21L)
                .setId(8802L).setPickListId(9902L).setSourceSnapshotHash("pick-source-78");
        when(completionPort.getByReceiptId(78L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectListByActiveOrderId(11L)).thenReturn(List.of(binding));

        MesBatchExecutionAuthoritativeContext resolved = resolver.resolve(new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION").setEntryBusinessId("completion-78")
                .setSourceCredentialType("CompletionBackfillReceipt").setSourceCredentialId("78")
                .setSourceSnapshotHash("source-78"), 1L);

        assertEquals(8802L, resolved.getProvisionCommand().getPickListBindingId());
        assertEquals(9902L, resolved.getProvisionCommand().getPickListId());
        assertEquals(1L, resolved.getProvisionCommand().getBindingVersion());
        assertEquals("pick-source-78", resolved.getPickListBindings().get(0).getSourceSnapshotHash());
    }

    @Test
    void activeEntryWithoutFormalFlow1BindingIsBlocked() {
        MesFlow6CompletionBackfillReceipt receipt = validActiveReceipt(79L, 12L, 22L, "B-79", "source-79");
        when(completionPort.getByReceiptId(79L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectListByActiveOrderId(12L)).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class, () -> resolver.resolve(
                new MesBatchExecutionProvisionCommand().setEntryType("ACTIVE_ORDER_COMPLETION")
                        .setEntryBusinessId("completion-79").setSourceCredentialId("79")
                        .setSourceCredentialType("CompletionBackfillReceipt")
                        .setSourceSnapshotHash("source-79"), 1L));
        assertEquals(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED.getCode(), error.getCode());
    }

    @Test
    void everyActiveEntryTypeUsesFlow4ReceiptPort() {
        MesFlow6CompletionBackfillReceipt receipt = validActiveReceipt(77L, 10L, 20L, "B-77", "source-77");
        when(completionPort.getByReceiptId(77L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectListByActiveOrderId(10L)).thenReturn(List.of(binding(10L, 20L)));

        for (String entryType : java.util.List.of("ACTIVE_ORDER_COMPLETION", "ACTIVE_ORDER_SCHEDULED",
                "ACTIVE_ORDER_PQC", "MANUAL_CONTROLLED_RETRY")) {
            resolver.resolve(new MesBatchExecutionProvisionCommand()
                    .setEntryType(entryType).setEntryBusinessId(entryType)
                    .setSourceCredentialId("77").setSourceCredentialType("CompletionBackfillReceipt")
                    .setSourceSnapshotHash("source-77"), 1L);
        }
        verify(completionPort, times(4)).getByReceiptId(77L, 1L);
    }

    @Test
    void activeEntryTracesEveryBoundPickListSnapshot() {
        MesFlow6CompletionBackfillReceipt receipt = validActiveReceipt(80L, 13L, 23L, "B-80", "source-80");
        MesProcessPoolActiveOrderPickListBindingDO first = binding(13L, 23L).setId(8803L).setPickListId(9903L);
        MesProcessPoolActiveOrderPickListBindingDO second = binding(13L, 23L).setId(8804L).setPickListId(9904L);
        when(completionPort.getByReceiptId(80L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectListByActiveOrderId(13L)).thenReturn(List.of(first, second));

        MesBatchExecutionAuthoritativeContext resolved = resolver.resolve(new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION").setEntryBusinessId("completion-80")
                .setSourceCredentialType("CompletionBackfillReceipt").setSourceCredentialId("80")
                .setSourceSnapshotHash("source-80"), 1L);

        assertEquals(2, resolved.getProvisionCommand().getCompletionBackfillReceipt().getSourceEvidence().stream()
                .filter(item -> "MATERIAL_ISSUE".equals(item.getSourceType())).count());
    }

    @Test
    void activeEntrySourceBundleMustPassTxCValidationAfterFlow6AddsProvisionReceipt() {
        MesFlow6CompletionBackfillReceipt receipt = validActiveReceipt(81L, 14L, 24L, "B-81", "source-81");
        MesProcessPoolActiveOrderPickListBindingDO binding = binding(14L, 24L)
                .setId(8805L).setPickListId(9905L).setSourceSnapshotHash("pick-source-81");
        when(completionPort.getByReceiptId(81L, 1L)).thenReturn(receipt);
        when(pickListBindingMapper.selectListByActiveOrderId(14L)).thenReturn(List.of(binding));

        MesBatchExecutionProvisionCommand resolved = resolver.resolve(new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION").setEntryBusinessId("completion-81")
                .setSourceCredentialType("CompletionBackfillReceipt").setSourceCredentialId("81")
                .setSourceSnapshotHash("source-81"), 1L).getProvisionCommand();

        List<MesProEdhrBatchTraceSource> sources = new ArrayList<>(resolved.getSourceEvidence().stream()
                .map(this::toTraceSource).toList());
        Long provisioningReceiptId = 7001L;
        Map<String, Object> provisionSnapshot = new LinkedHashMap<>();
        provisionSnapshot.put("sourceType", "BATCH_PROVISION_RECEIPT");
        provisionSnapshot.put("sourceId", provisioningReceiptId);
        provisionSnapshot.put("witnessHash", resolved.getSourceSnapshotHash());
        provisionSnapshot.put("sourceSnapshotHash", resolved.getSourceSnapshotHash());
        String provisionSnapshotJson = JSON.toJSONString(provisionSnapshot);
        sources.add(new MesProEdhrBatchTraceSource()
                .setLinkType(MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT)
                .setSourceObjectType("BATCH_PROVISIONING_RECORD")
                .setSourceObjectId(provisioningReceiptId)
                .setSourceVersion(1)
                .setSourceIdentityKey("BATCH_PROVISION_RECEIPT:BATCH_PROVISIONING_RECORD:7001::")
                .setSnapshotJson(provisionSnapshotJson)
                .setSnapshotHash(resolved.getSourceSnapshotHash())
                .setRelationStatus("BOUND"));

        MesProEdhrBatchTraceCaptureCommand capture = new MesProEdhrBatchTraceCaptureCommand()
                .setBatchExecutionId(101L)
                .setEntryType("ACTIVE_ORDER_COMPLETION")
                .setOriginKey("ACTIVE_ORDER:14")
                .setActiveOrderId(resolved.getActiveOrderId())
                .setWorkOrderId(resolved.getWorkOrderId())
                .setCompletionTransactionId(resolved.getCompletionTransactionId())
                .setCompletionVersion(resolved.getCompletionVersion().intValue())
                .setCompletionBackfillReceiptId(Long.valueOf(resolved.getCompletionBackfillReceiptId()))
                .setCompletionBackfillReceiptHash(resolved.getCompletionBackfillReceiptHash())
                .setPickListSources(resolved.getPickListSources())
                .setPickListBindingVersion(resolved.getBindingVersion().intValue())
                .setSourceSnapshotHash(resolved.getSourceSnapshotHash())
                .setBatchProvisionReceiptId(provisioningReceiptId)
                .setBatchProvisionStatus(MesBatchProvisioningStatus.BATCH_PROVISIONING.name())
                .setSourceBundleHash(resolved.getSourceBundleHash())
                .setIdempotencyKey("p2-81")
                .setHasActualLoss(resolved.getCompletionBackfillReceipt().getHasActualLoss())
                .setSources(sources);

        MesProEdhrBatchTraceValidationResult validation = new MesProEdhrBatchTraceabilityValidator()
                .validate(capture);

        assertTrue(validation.valid(), validation.blockerCode() + ":" + validation.blockerScope());
    }

    private MesProEdhrBatchTraceSource toTraceSource(MesBatchExecutionSourceEvidence evidence) {
        return new MesProEdhrBatchTraceSource()
                .setLinkType(evidence.getSourceType())
                .setSourceObjectType(evidence.getSourceObjectType())
                .setSourceObjectId(Long.valueOf(evidence.getSourceObjectId()))
                .setSourceVersion(Integer.valueOf(evidence.getSourceVersion()))
                .setSourceIdentityKey(evidence.getSourceIdentityKey())
                .setSnapshotJson(evidence.getSnapshotJson())
                .setSnapshotHash(evidence.getSourceSnapshotHash())
                .setRelationStatus(evidence.getRelationStatus())
                .setRelationReason(evidence.getRelationReason());
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
        when(independentService.verify(any(), eq(1L))).thenReturn(verified);
        MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                .setEntryType("MANUAL").setEntryBusinessId("manual-1")
                .setSourceCredentialType("IndependentBatchPrerequisiteReceipt")
                .setSourceCredentialId("ind-1").setSourceSnapshotHash("source-1");

        MesBatchExecutionAuthoritativeContext resolved = resolver.resolve(request, 1L);

        assertEquals("B-20", resolved.getProvisionCommand().getBatchCode());
        assertEquals(verified, resolved.getProvisionCommand().getIndependentReceipt());
        assertEquals("WORK_ORDER:WORK_ORDER:20::",
                resolved.getProvisionCommand().getSourceEvidence().get(0).getSourceIdentityKey());
        verify(independentService).verify(argThat(command ->
                        "ind-1".equals(command.getReceiptId()) && "MANUAL".equals(command.getEntryType())), eq(1L));
    }

    @Test
    void independentEntryRejectsEvidenceWithoutExplicitRelationStatus() {
        MesIndependentBatchPrerequisiteReceipt verified = independentReceipt();
        verified.getSourceEvidence().get(0).setRelationStatus(null);
        when(independentService.verify(any(), eq(1L))).thenReturn(verified);
        MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                .setEntryType("MANUAL").setEntryBusinessId("manual-1")
                .setSourceCredentialType("IndependentBatchPrerequisiteReceipt")
                .setSourceCredentialId("ind-1").setSourceSnapshotHash("source-1");

        assertThrows(RuntimeException.class, () -> resolver.resolve(request, 1L));
    }

    @Test
    void independentEntryBlocksCrossTenantVerifiedReceipt() {
        MesIndependentBatchPrerequisiteReceipt receipt = independentReceipt().setTenantId(2L);
        when(independentService.verify(any(), eq(1L))).thenReturn(receipt);
        MesBatchExecutionProvisionCommand request = new MesBatchExecutionProvisionCommand()
                .setEntryType("MANUAL").setEntryBusinessId("manual-1")
                .setSourceCredentialId("ind-1").setSourceSnapshotHash("source-1");

        assertThrows(RuntimeException.class, () -> resolver.resolve(request, 1L));
    }

    @Test
    void everyIndependentEntryTypeUsesFlow9VerifiedService() {
        for (String entryType : java.util.List.of("MANUAL", "SCHEDULED", "PQC_INDEPENDENT")) {
            MesIndependentBatchPrerequisiteReceipt verified = independentReceipt().setEntryType(entryType);
            when(independentService.verify(any(), eq(1L))).thenReturn(verified);
            resolver.resolve(new MesBatchExecutionProvisionCommand()
                    .setEntryType(entryType).setEntryBusinessId(entryType)
                    .setSourceCredentialId("ind-1").setSourceSnapshotHash("source-1"), 1L);
        }
        verify(independentService, times(3)).verify(any(), eq(1L));
    }

    private MesFlow6CompletionBackfillReceipt validActiveReceipt(Long receiptId, Long activeOrderId,
                                                                  Long workOrderId, String batchCode,
                                                                  String sourceHash) {
        return new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(receiptId).setTenantId(1L).setActiveOrderId(activeOrderId).setWorkOrderId(workOrderId)
                .setBatchCode(batchCode).setRouteId(30L).setRouteVersionId(31L)
                .setRequestIdempotencyKey("idem-" + receiptId).setSourceSnapshotHash(sourceHash)
                .setCompletionVersion(1).setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setReceiptHash("receipt-" + receiptId).setHasActualLoss(false)
                .setBatchRecordId(100L).setProcessInspectionId(101L)
                .setLossQuantity(java.math.BigDecimal.ZERO)
                .setLossReportStatus("NOT_REQUIRED")
                .setZeroLossConfirmationSnapshot("zero-loss-" + receiptId);
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
                        .setSourceSnapshotHash("source-1").setPayloadHash("payload-1").setSignature("signature-1")
                        .setSourceObjectType("WORK_ORDER").setSourceObjectId("20")
                        .setSnapshotJson("{\"workOrderId\":20}")
                        .setRelationStatus("BOUND")));
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
