package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesBatchExecutionEntryContractTest {

    private final MesBatchExecutionEntryContractService contract =
            new MesBatchExecutionEntryContractService();

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void clientNestedCompletionReceiptIsRejected() {
        MesBatchExecutionProvisionCommand command = baseActiveCommand()
                .setCompletionBackfillReceipt(new MesCompletionBackfillReceipt()
                        .setReceiptId("1").setStatus("BACKFILL_SUCCEEDED"));

        ServiceException error = assertThrows(ServiceException.class, () -> contract.validate(command));
        assertEquals(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID.getCode(), error.getCode());
    }

    @Test
    void clientNestedIndependentReceiptIsRejected() {
        MesBatchExecutionProvisionCommand command = baseIndependentCommand()
                .setIndependentReceipt(new MesIndependentBatchPrerequisiteReceipt()
                        .setReceiptId("independent-1").setStatus("ISSUED"));

        ServiceException error = assertThrows(ServiceException.class, () -> contract.validate(command));
        assertEquals(PRO_EDHR_BATCH_ENTRY_RECEIPT_INVALID.getCode(), error.getCode());
    }

    @Test
    void activeAuthoritativeCompletionReceiptIsAccepted() {
        MesBatchExecutionAuthoritativeContext context = new MesBatchExecutionAuthoritativeContext()
                .setProvisionCommand(baseActiveCommand())
                .setCompletionReceipt(activeReceipt())
                .setPickListBinding(pickListBinding());

        assertSame(context.getProvisionCommand(), contract.validate(context));
    }

    @Test
    void activePendingReceiptIsBlocked() {
        MesBatchExecutionAuthoritativeContext context = new MesBatchExecutionAuthoritativeContext()
                .setProvisionCommand(baseActiveCommand())
                .setCompletionReceipt(activeReceipt().setStatus("BACKFILL_PENDING"));

        assertThrows(ServiceException.class, () -> contract.validate(context));
    }

    @Test
    void activeNoLossRequiresZeroLossFact() {
        MesBatchExecutionAuthoritativeContext context = new MesBatchExecutionAuthoritativeContext()
                .setProvisionCommand(baseActiveCommand())
                .setCompletionReceipt(activeReceipt().setZeroLossConfirmationSnapshot(null));

        assertThrows(ServiceException.class, () -> contract.validate(context));
    }

    @Test
    void independentAuthoritativeReceiptIsAccepted() {
        MesIndependentBatchPrerequisiteReceipt receipt = independentReceipt();
        MesBatchExecutionProvisionCommand command = baseIndependentCommand()
                .setSourceCredentialId(receipt.getReceiptId())
                .setSourceRelationId(receipt.getSourceRelationId())
                .setSourceContextHash(receipt.getSourceContextHash())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setExpectedSourceVersion(receipt.getSourceRelationVersion())
                .setPayloadHash(receipt.getPayloadHash())
                .setWorkOrderId(receipt.getWorkOrderId()).setWorkOrderCode(receipt.getWorkOrderCode())
                .setBatchCode(receipt.getBatchCode()).setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId());
        MesBatchExecutionAuthoritativeContext context = new MesBatchExecutionAuthoritativeContext()
                .setProvisionCommand(command).setIndependentReceipt(receipt);

        assertSame(command, contract.validate(context));
    }

    @Test
    void independentExpiredReceiptIsBlocked() {
        MesIndependentBatchPrerequisiteReceipt receipt = independentReceipt()
                .setExpiresAt(LocalDateTime.now().minusMinutes(1));
        MesBatchExecutionProvisionCommand command = baseIndependentCommand()
                .setSourceCredentialId(receipt.getReceiptId())
                .setSourceRelationId(receipt.getSourceRelationId())
                .setSourceContextHash(receipt.getSourceContextHash())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setExpectedSourceVersion(receipt.getSourceRelationVersion())
                .setPayloadHash(receipt.getPayloadHash())
                .setWorkOrderId(receipt.getWorkOrderId()).setWorkOrderCode(receipt.getWorkOrderCode())
                .setBatchCode(receipt.getBatchCode()).setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId());
        MesBatchExecutionAuthoritativeContext context = new MesBatchExecutionAuthoritativeContext()
                .setProvisionCommand(command).setIndependentReceipt(receipt);

        assertThrows(ServiceException.class, () -> contract.validate(context));
    }

    private MesBatchExecutionProvisionCommand baseActiveCommand() {
        return new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION").setEntryBusinessId("completion-1")
                .setSourceCredentialType("CompletionBackfillReceipt").setSourceCredentialId("1")
                .setSourceContextHash("source-1").setSourceSnapshotHash("source-1")
                .setTenantId(1L).setActiveOrderId(11L).setWorkOrderId(22L)
                .setBatchCode("B-1").setRouteId(30L).setRouteVersionId(31L)
                .setPickListBindingId(8801L).setPickListId(9901L).setBindingVersion(1L)
                .setIdempotencyKey("completion-idem");
    }

    private MesFlow6CompletionBackfillReceipt activeReceipt() {
        return new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(1L).setTenantId(1L).setActiveOrderId(11L).setWorkOrderId(22L)
                .setBatchCode("B-1").setRouteId(30L).setRouteVersionId(31L)
                .setRequestIdempotencyKey("completion-idem").setSourceSnapshotHash("source-1")
                .setCompletionVersion(2).setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setHasActualLoss(false).setLossQuantity(BigDecimal.ZERO).setLossReportStatus("NO_LOSS")
                .setZeroLossConfirmationSnapshot("zero-loss-confirmed").setReceiptHash("receipt-hash");
    }

    private MesProcessPoolActiveOrderPickListBindingDO pickListBinding() {
        MesProcessPoolActiveOrderPickListBindingDO binding = new MesProcessPoolActiveOrderPickListBindingDO()
                .setId(8801L).setActiveOrderId(11L).setWorkOrderId(22L).setPickListId(9901L)
                .setSourceSnapshotHash("pick-list-source-1").setBindingStatus("BOUND").setBindingVersion(1);
        binding.setTenantId(1L);
        return binding;
    }

    private MesBatchExecutionProvisionCommand baseIndependentCommand() {
        return new MesBatchExecutionProvisionCommand()
                .setEntryType("MANUAL").setEntryBusinessId("manual-1")
                .setSourceCredentialType("IndependentBatchPrerequisiteReceipt")
                .setSourceCredentialId("independent-1").setTenantId(1L)
                .setSourceContextHash("context-1").setSourceSnapshotHash("source-1")
                .setIdempotencyKey("independent-idem");
    }

    private MesIndependentBatchPrerequisiteReceipt independentReceipt() {
        return new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId("independent-1").setTenantId(1L).setEntryType("MANUAL")
                .setWorkOrderId(22L).setWorkOrderCode("WO-22").setRouteId(30L).setRouteVersionId(31L)
                .setRouteVersion("v1").setBatchCode("B-1").setSourceRelationId("relation-1")
                .setSourceRelationVersion("v1").setSourceRelationSnapshotHash("relation-snapshot")
                .setSourceObjectType("WORK_ORDER").setSourceObjectId("22")
                .setMaterialSourceType("PICK_LIST").setMaterialSourceId("pick-1")
                .setSourceContextHash("context-1").setSourceSnapshotHash("source-1")
                .setBusinessReason("manual controlled batch").setIssuerSystem("flow9")
                .setIssuerUserId(9L).setIssuerUserRole("BACKEND_CONTROLLED")
                .setIssuedAt(LocalDateTime.now().minusMinutes(1)).setExpiresAt(LocalDateTime.now().plusMinutes(10))
                .setCredentialVersion(1L).setStatus("ISSUED").setReceiptHash("receipt-hash")
                .setPayloadHash("payload-hash").setSignature("signature").setAuditEventId("audit-1")
                .setIdempotencyKey("independent-idem")
                .setSourceEvidence(List.of(new MesBatchExecutionSourceEvidence()
                        .setSourceType("WORK_ORDER").setSourceId("22").setSourceVersion("v1")
                        .setSourceSnapshotHash("source-1").setPayloadHash("payload-hash")
                        .setSignature("signature")));
    }
}
