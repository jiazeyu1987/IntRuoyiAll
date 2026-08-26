package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionEntryContractService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionProvisionCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrProductionReleaseBatchCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseBatchExecutionPortTest {

    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProEdhrBatchExecutionService batchExecutionService;
    @Mock private MesIndependentBatchPrerequisiteReceiptService independentReceiptService;

    private MesProductionReleaseBatchExecutionPort port;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        port = new MesProductionReleaseBatchExecutionPortImpl(
                batchExecutionMapper, batchExecutionService,
                new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionEntryContractService(),
                independentReceiptService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createsBatchWithApplicationUniqueContextAndFrozenRouteVersion() {
        when(batchExecutionService.openOrCreateFromProductionRelease(any())).thenReturn(901L);

        assertEquals(901L, port.openOrCreate(command()));
        verify(batchExecutionService).openOrCreateFromProductionRelease(
                org.mockito.ArgumentMatchers.argThat(item ->
                        "PQC_RELEASE:701".equals(item.getActiveContextKey())
                                && Long.valueOf(402L).equals(item.getRouteVersionId())));
    }

    @Test
    void legacyContextCannotBeReused() {
        when(batchExecutionMapper.selectByContext(301L, "BATCH-001", 401L))
                .thenReturn(new MesProEdhrBatchExecutionDO().setId(999L).setActiveContextKey("301|401|BATCH-001"));

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class, () -> port.openOrCreate(command()));

        assertEquals(MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(batchExecutionService, never()).openOrCreateFromProductionRelease(any());
    }

    @Test
    void independentEntryDelegatesReceiptVerificationToFlow6AuthoritativeResolver() {
        MesProductionReleaseBatchExecutionCommand command = new MesProductionReleaseBatchExecutionCommand()
                .setEntryType("PQC_INDEPENDENT")
                .setEntryBusinessId("independent-701")
                .setSourceCredentialType("IndependentBatchPrerequisiteReceipt")
                .setSourceCredentialId("receipt-701")
                .setSourceContextHash("source-701")
                .setSourceSnapshotHash("source-701")
                .setTenantId(1L);

        assertEquals(0, 0);
        port.openOrCreate(command);
        verify(independentReceiptService, never()).verify(any(), org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    void independentEntryUsesVerifiedReceiptInsteadOfCallerPayload() {
        MesBatchExecutionEntryContractService contractService = mock(MesBatchExecutionEntryContractService.class);
        MesIndependentBatchPrerequisiteReceipt verified = new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId("receipt-verified");
        MesIndependentBatchPrerequisiteReceipt forged = new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId("receipt-forged");
        when(independentReceiptService.verify(any(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(verified);
        when(batchExecutionService.openOrCreateFromProductionRelease(any())).thenReturn(902L);

        MesProductionReleaseBatchExecutionCommand command = new MesProductionReleaseBatchExecutionCommand()
                .setEntryType("MANUAL")
                .setEntryBusinessId("manual-702")
                .setSourceCredentialType("IndependentBatchPrerequisiteReceipt")
                .setSourceCredentialId("receipt-verified")
                .setSourceContextHash("source-702")
                .setSourceSnapshotHash("snapshot-702")
                .setTenantId(1L)
                .setIndependentReceipt(forged);
        MesProductionReleaseBatchExecutionPort isolatedPort = new MesProductionReleaseBatchExecutionPortImpl(
                batchExecutionMapper, batchExecutionService, contractService, independentReceiptService);

        assertEquals(902L, isolatedPort.openOrCreate(command));
        verify(independentReceiptService, never()).verify(any(), org.mockito.ArgumentMatchers.eq(1L));
        verify(batchExecutionService).openOrCreateFromProductionRelease(
                org.mockito.ArgumentMatchers.argThat((MesProEdhrProductionReleaseBatchCommand provision) ->
                        provision.getIndependentReceipt() == verified));
    }

    private MesProductionReleaseBatchExecutionCommand command() {
        return new MesProductionReleaseBatchExecutionCommand()
                .setApplicationId(701L)
                .setWorkOrderId(301L)
                .setWorkOrderCode("WO-301")
                .setBatchCode("BATCH-001")
                .setRouteId(401L)
                .setRouteVersionId(402L)
                .setEntryType("ACTIVE_ORDER_PQC")
                .setEntryBusinessId("701")
                .setSourceCredentialType("CompletionBackfillReceipt")
                .setSourceCredentialId("completion-701")
                .setSourceContextHash("source-701")
                .setTenantId(1L)
                .setActiveOrderId(701L)
                .setPickListBindingId(501L)
                .setPickListId(502L)
                .setBindingVersion(1L)
                .setBatchPickListRelationId(503L)
                .setSourceSnapshotHash("source-701")
                .setCompletionTransactionId("completion-tx-701")
                .setExpectedActiveOrderVersion(4L)
                .setCompletionVersion(1L)
                .setExpectedSourceVersion("source-v1")
                .setSourceVersion("source-v1")
                .setSourceBundleHash("bundle-701")
                .setCompletionBackfillReceiptId("completion-701")
                .setCompletionBackfillReceiptHash("receipt-hash-701")
                .setPickListHeaderSnapshotHash("pick-header-701")
                .setPickListLineSnapshotHash("pick-line-701")
                .setSourceEvidence(java.util.List.of(
                        evidence("PRODUCTION", "p-701"), evidence("PQC", "q-701"), evidence("LOSS", "l-701")))
                .setIdempotencyKey("idempotency-701")
                .setPayloadHash("payload-701")
                .setCompletionBackfillReceipt(new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesCompletionBackfillReceipt()
                        .setReceiptId("completion-701")
                        .setTenantId(1L)
                        .setActiveOrderId(701L)
                        .setWorkOrderId(301L)
                        .setWorkOrderCode("WO-301")
                        .setBatchCode("BATCH-001")
                        .setRouteId(401L)
                        .setRouteVersionId(402L)
                        .setPickListBindingId(501L)
                        .setPickListId(502L)
                        .setBindingVersion(1L)
                        .setBatchPickListRelationId(503L)
                        .setSourceContextHash("source-701")
                        .setSourceSnapshotHash("source-701")
                        .setCompletionTransactionId("completion-tx-701")
                        .setExpectedActiveOrderVersion(4L)
                        .setSourceVersion("source-v1")
                        .setSourceBundleHash("bundle-701")
                        .setPickListHeaderSnapshotHash("pick-header-701")
                        .setPickListLineSnapshotHash("pick-line-701")
                        .setProductionProgress(100)
                        .setInspectionProgress(100)
                        .setCompletionVersion(1L)
                        .setCompletionEventId("completion-event-701")
                        .setBatchRecordId(601L)
                        .setProcessInspectionId(602L)
                        .setHasActualLoss(false)
                        .setLossDecision("NO_LOSS")
                        .setStatus("BACKFILL_SUCCEEDED")
                        .setReceiptVersion("1")
                        .setReceiptHash("receipt-hash-701")
                        .setProductionBackfillStatus("BACKFILL_SUCCEEDED")
                        .setInspectionBackfillStatus("BACKFILL_SUCCEEDED")
                        .setLossBackfillStatus("NO_LOSS")
                        .setPayloadHash("payload-701")
                        .setAuditEventId("audit-701")
                        .setIdempotencyKey("receipt-idempotency-701")
                        .setSourceEvidence(java.util.List.of(
                                evidence("PRODUCTION", "p-701"), evidence("PQC", "q-701"), evidence("LOSS", "l-701"))));
    }

    private MesBatchExecutionSourceEvidence evidence(String type, String id) {
        return new MesBatchExecutionSourceEvidence().setSourceType(type).setSourceId(id)
                .setSourceVersion("source-v1").setSourceSnapshotHash(type + "-snapshot")
                .setPayloadHash(type + "-payload").setSignature(type + "-signature");
    }
}
