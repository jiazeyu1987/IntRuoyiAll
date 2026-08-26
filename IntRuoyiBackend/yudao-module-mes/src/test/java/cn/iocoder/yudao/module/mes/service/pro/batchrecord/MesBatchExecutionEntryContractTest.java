package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_BACKFILL_NOT_SUCCEEDED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_CREDENTIAL_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_ENTRY_TENANT_REQUIRED;
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
    void activeOrderRequiresSuccessfulCompletionReceipt() {
        MesBatchExecutionProvisionCommand command = activeCommand()
                .setCompletionBackfillReceipt(new MesCompletionBackfillReceipt()
                        .setReceiptId("completion-1")
                        .setStatus("BACKFILL_PENDING")
                        .setActiveOrderId(11L)
                        .setWorkOrderId(22L)
                        .setPickListBindingId(33L)
                        .setPickListId(44L)
                        .setSourceSnapshotHash("source-hash")
                        .setBindingVersion(2L));

        ServiceException error = assertThrows(ServiceException.class, () -> contract.validate(command));
        assertEquals(PRO_EDHR_BATCH_ENTRY_BACKFILL_NOT_SUCCEEDED.getCode(), error.getCode());
    }

    @Test
    void activeOrderAcceptsOnlyMatchingCompletionFacts() {
        MesBatchExecutionProvisionCommand command = activeCommand()
                .setCompletionBackfillReceipt(validCompletionReceipt());
        assertSame(command, contract.validate(command));

        command.setRouteId(8L);
        assertThrows(ServiceException.class, () -> contract.validate(command));
    }

    @Test
    void payloadHashConflictIsBlocked() {
        MesBatchExecutionProvisionCommand command = activeCommand()
                .setCompletionBackfillReceipt(validCompletionReceipt())
                .setPayloadHash("tampered-payload");
        assertThrows(ServiceException.class, () -> contract.validate(command));
    }

    @Test
    void completionVersionAndFormalEvidenceMustMatchReceipt() {
        MesBatchExecutionProvisionCommand command = activeCommand()
                .setCompletionBackfillReceipt(validCompletionReceipt())
                .setCompletionVersion(99L);
        assertThrows(ServiceException.class, () -> contract.validate(command));

        command.setCompletionVersion(3L).getSourceEvidence().get(0).setPayloadHash("tampered");
        assertThrows(ServiceException.class, () -> contract.validate(command));
    }

    @Test
    void bindingVersionConflictIsBlocked() {
        MesBatchExecutionProvisionCommand command = activeCommand()
                .setCompletionBackfillReceipt(validCompletionReceipt())
                .setBindingVersion(9L);
        assertThrows(ServiceException.class, () -> contract.validate(command));
    }

    @Test
    void activeEvidenceMustCoverProductionPqcAndLoss() {
        MesBatchExecutionProvisionCommand command = activeCommand()
                .setCompletionBackfillReceipt(validCompletionReceipt())
                .setSourceEvidence(List.of(evidence("PRODUCTION", "p-1"),
                        evidence("PRODUCTION", "p-2"), evidence("LOSS", "l-1")));
        assertThrows(ServiceException.class, () -> contract.validate(command));
    }

    @Test
    void activePickListSnapshotConflictIsBlocked() {
        MesBatchExecutionProvisionCommand command = activeCommand()
                .setCompletionBackfillReceipt(validCompletionReceipt())
                .setPickListLineSnapshotHash("tampered-line-snapshot");
        assertThrows(ServiceException.class, () -> contract.validate(command));
    }

    @Test
    void securityTenantCannotBeOverriddenByCommandOrReceipt() {
        MesBatchExecutionProvisionCommand command = activeCommand()
                .setCompletionBackfillReceipt(validCompletionReceipt().setTenantId(2L));
        assertThrows(ServiceException.class, () -> contract.validate(command));

        MesBatchExecutionProvisionCommand overriddenTenantCommand = activeCommand().setTenantId(2L)
                .setCompletionBackfillReceipt(validCompletionReceipt());
        assertThrows(ServiceException.class, () -> contract.validate(overriddenTenantCommand));
    }

    @Test
    void missingSecurityTenantIsBlockedWithStableContractError() {
        TenantContextHolder.clear();
        ServiceException error = assertThrows(ServiceException.class,
                () -> contract.validate(activeCommand().setCompletionBackfillReceipt(validCompletionReceipt())));
        assertEquals(PRO_EDHR_BATCH_ENTRY_TENANT_REQUIRED.getCode(), error.getCode());
    }

    @Test
    void independentEntryRequiresFormalReceiptAndRelation() {
        MesBatchExecutionProvisionCommand command = independentCommand();
        ServiceException missingReceipt = assertThrows(ServiceException.class, () -> contract.validate(command));
        assertEquals(PRO_EDHR_BATCH_ENTRY_CREDENTIAL_REQUIRED.getCode(), missingReceipt.getCode());

        command.setIndependentReceipt(validIndependentReceipt().setSourceRelationId(null));
        ServiceException missingRelation = assertThrows(ServiceException.class, () -> contract.validate(command));
        assertEquals(PRO_EDHR_BATCH_ENTRY_SOURCE_RELATION_REQUIRED.getCode(), missingRelation.getCode());
    }

    @Test
    void independentPqcRejectsActiveReceiptAndAcceptsCanonicalReceipt() {
        MesBatchExecutionProvisionCommand command = independentCommand()
                .setCompletionBackfillReceipt(new MesCompletionBackfillReceipt()
                        .setReceiptId("active-receipt")
                        .setStatus("BACKFILL_SUCCEEDED"));
        ServiceException mismatch = assertThrows(ServiceException.class, () -> contract.validate(command));
        assertEquals(PRO_EDHR_BATCH_ENTRY_SCENARIO_MISMATCH.getCode(), mismatch.getCode());

        command.setCompletionBackfillReceipt(null).setIndependentReceipt(validIndependentReceipt());
        assertSame(command, contract.validate(command));
    }

    @Test
    void scheduledEntryTypesRemainValidAfterSharedValidation() {
        MesBatchExecutionProvisionCommand active = activeCommand()
                .setEntryType("ACTIVE_ORDER_SCHEDULED")
                .setCompletionBackfillReceipt(validCompletionReceipt());
        assertSame(active, contract.validate(active));

        MesBatchExecutionProvisionCommand independent = independentCommand()
                .setEntryType("SCHEDULED")
                .setIndependentReceipt(validIndependentReceipt().setEntryType("SCHEDULED"));
        assertSame(independent, contract.validate(independent));
    }

    @Test
    void expiredIndependentReceiptIsBlocked() {
        MesBatchExecutionProvisionCommand command = independentCommand()
                .setIndependentReceipt(validIndependentReceipt().setExpiresAt(LocalDateTime.now().minusMinutes(1)));
        ServiceException error = assertThrows(ServiceException.class, () -> contract.validate(command));
        assertEquals(PRO_EDHR_BATCH_ENTRY_RECEIPT_EXPIRED.getCode(), error.getCode());
    }

    private MesBatchExecutionProvisionCommand activeCommand() {
        return new MesBatchExecutionProvisionCommand()
                .setEntryType("ACTIVE_ORDER_COMPLETION")
                .setEntryBusinessId("active-11")
                .setSourceCredentialType("CompletionBackfillReceipt")
                .setSourceCredentialId("completion-1")
                .setSourceContextHash("source-hash")
                .setSourceSnapshotHash("source-hash")
                .setTenantId(1L)
                .setActiveOrderId(11L)
                .setWorkOrderId(22L)
                .setWorkOrderCode("WO-22")
                .setBatchCode("B-1")
                .setRouteId(7L)
                .setPickListBindingId(33L)
                .setPickListId(44L)
                .setBindingVersion(2L)
                .setBatchPickListRelationId(66L)
                .setRouteVersionId(55L)
                .setCompletionTransactionId("completion-tx-1")
                .setExpectedActiveOrderVersion(4L)
                .setCompletionVersion(3L)
                .setSourceVersion("source-v1")
                .setSourceBundleHash("bundle-hash")
                .setExpectedSourceVersion("source-v1")
                .setPickListHeaderSnapshotHash("pick-header-hash")
                .setPickListLineSnapshotHash("pick-line-hash")
                .setCompletionBackfillReceiptId("completion-1")
                .setCompletionBackfillReceiptHash("receipt-hash")
                .setSourceEvidence(List.of(
                        evidence("PRODUCTION", "p-1"), evidence("PQC", "q-1"), evidence("LOSS", "l-1")))
                .setIdempotencyKey("active-11-attempt-1")
                .setPayloadHash("payload-hash");
    }

    private MesBatchExecutionProvisionCommand independentCommand() {
        return new MesBatchExecutionProvisionCommand()
                .setEntryType("PQC_INDEPENDENT")
                .setEntryBusinessId("pqc-independent-1")
                .setSourceCredentialType("IndependentBatchPrerequisiteReceipt")
                .setSourceCredentialId("independent-1")
                .setSourceContextHash("independent-source-hash")
                .setSourceSnapshotHash("independent-source-hash")
                .setSourceRelationId("relation-1")
                .setTenantId(1L)
                .setWorkOrderId(22L)
                .setWorkOrderCode("WO-22")
                .setBatchCode("B-1")
                .setRouteId(7L)
                .setRouteVersionId(55L)
                .setExpectedSourceVersion("relation-v1")
                .setIdempotencyKey("pqc-independent-1")
                .setPayloadHash("payload-hash");
    }

    private MesCompletionBackfillReceipt validCompletionReceipt() {
        return new MesCompletionBackfillReceipt()
                .setReceiptId("completion-1")
                .setTenantId(1L)
                .setActiveOrderId(11L)
                .setWorkOrderId(22L)
                .setWorkOrderCode("WO-22")
                .setBatchCode("B-1")
                .setRouteId(7L)
                .setRouteVersionId(55L)
                .setCompletionTransactionId("completion-tx-1")
                .setExpectedActiveOrderVersion(4L)
                .setCompletionVersion(3L)
                .setSourceBundleHash("bundle-hash")
                .setPickListBindingId(33L)
                .setPickListId(44L)
                .setBindingVersion(2L)
                .setBatchPickListRelationId(66L)
                .setSourceContextHash("source-hash")
                .setSourceSnapshotHash("source-hash")
                .setSourceVersion("source-v1")
                .setPickListHeaderSnapshotHash("pick-header-hash")
                .setPickListLineSnapshotHash("pick-line-hash")
                .setProductionProgress(100)
                .setInspectionProgress(100)
                .setCompletionVersion(3L)
                .setCompletionEventId("completion-event-1")
                .setBatchRecordId(77L)
                .setProcessInspectionId(88L)
                .setHasActualLoss(false)
                .setLossDecision("NO_LOSS")
                .setStatus("BACKFILL_SUCCEEDED")
                .setReceiptVersion("1")
                .setReceiptHash("receipt-hash")
                .setProductionBackfillStatus("BACKFILL_SUCCEEDED")
                .setInspectionBackfillStatus("BACKFILL_SUCCEEDED")
                .setLossBackfillStatus("NO_LOSS")
                .setPayloadHash("payload-hash")
                .setAuditEventId("audit-1")
                .setIdempotencyKey("receipt-idempotency-1")
                .setSourceEvidence(List.of(evidence("PRODUCTION", "p-1"), evidence("PQC", "q-1"), evidence("LOSS", "l-1")));
    }

    private MesIndependentBatchPrerequisiteReceipt validIndependentReceipt() {
        return new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId("independent-1")
                .setTenantId(1L)
                .setEntryType("PQC_INDEPENDENT")
                .setWorkOrderId(22L)
                .setWorkOrderCode("WO-22")
                .setRouteId(7L)
                .setRouteVersionId(55L)
                .setRouteVersion("v2")
                .setBatchCode("B-1")
                .setSourceRelationId("relation-1")
                .setSourceRelationVersion("relation-v1")
                .setSourceRelationSnapshotHash("relation-hash")
                .setSourceObjectType("PQC_APPLICATION")
                .setSourceObjectId("pqc-source-1")
                .setMaterialSourceType("MATERIAL_ISSUE")
                .setMaterialSourceId("material-source-1")
                .setSourceContextHash("independent-source-hash")
                .setSourceSnapshotHash("independent-source-hash")
                .setBusinessReason("独立PQC业务")
                .setIssuerSystem("mes")
                .setIssuerUserId(9L)
                .setIssuerUserRole("PQC")
                .setIssuedAt(LocalDateTime.now().minusMinutes(1))
                .setExpiresAt(LocalDateTime.now().plusMinutes(10))
                .setCredentialVersion(1L)
                .setStatus("VALID")
                .setReceiptHash("independent-receipt-hash")
                .setPayloadHash("payload-hash")
                .setSignature("signed")
                .setAuditEventId("audit-1")
                .setIdempotencyKey("independent-1")
                .setSourceEvidence(List.of(evidence("PQC", "pqc-source-1")));
    }

    private MesBatchExecutionSourceEvidence evidence(String type, String id) {
        return new MesBatchExecutionSourceEvidence().setSourceType(type).setSourceId(id)
                .setSourceVersion("v1").setSourceSnapshotHash(type + "-snapshot")
                .setPayloadHash(type + "-payload").setSignature(type + "-signature");
    }
}
