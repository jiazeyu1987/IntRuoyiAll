package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchTraceabilityValidatorTest {

    private final MesProEdhrBatchTraceabilityValidator validator =
            new MesProEdhrBatchTraceabilityValidator();

    @Test
    void activeOrderCompletionDoesNotRequireReleaseApplication() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setReleaseApplicationId(null);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertTrue(result.valid(), result.blockerCode());
    }

    @Test
    void releaseApplicationCannotBeCapturedBeforeReleaseDecision() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setReleaseApplicationId(77L);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.ENTRY_SCENARIO_MISMATCH, result.blockerCode());
    }

    @Test
    void missingBatchProvisionReceiptBlocksCapture() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setBatchProvisionReceiptId(null);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.BATCH_PROVISION_REQUIRED, result.blockerCode());
    }

    @Test
    void independentEntryRequiresItsOwnCredential() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setEntryType(MesProEdhrBatchTraceEntryType.MANUAL)
                .setActiveOrderId(null)
                .setCompletionTransactionId(null)
                .setCompletionBackfillReceiptId(null)
                .setCompletionBackfillReceiptHash(null)
                .setPickListBindingId(null)
                .setSourceCredentialId(null);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.INDEPENDENT_CREDENTIAL_REQUIRED, result.blockerCode());
    }

    @Test
    void independentEntryCanBeValidatedWithoutActiveOrderOrReleaseApplication() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setEntryType(MesProEdhrBatchTraceEntryType.MANUAL)
                .setActiveOrderId(null)
                .setCompletionTransactionId(null)
                .setCompletionBackfillReceiptId(null)
                .setCompletionBackfillReceiptHash(null)
                .setPickListBindingId(null)
                .setSourceCredentialId(44L)
                .setSourceCredentialHash("credential-hash")
                .setReleaseApplicationId(null);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertTrue(result.valid(), result.blockerCode());
    }

    @Test
    void pqcIndependentEntryUsesTheFlow9CanonicalEntryType() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setEntryType(MesProEdhrBatchTraceEntryType.PQC_INDEPENDENT)
                .setActiveOrderId(null)
                .setCompletionTransactionId(null)
                .setCompletionBackfillReceiptId(null)
                .setCompletionBackfillReceiptHash(null)
                .setPickListBindingId(null)
                .setSourceCredentialId(45L)
                .setSourceCredentialHash("pqc-credential-hash")
                .setReleaseApplicationId(null);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertTrue(result.valid(), result.blockerCode());
    }

    @Test
    void sourceHashMismatchBlocksCapture() {
        MesProEdhrBatchTraceSource source = new MesProEdhrBatchTraceSource()
                .setLinkType(MesProEdhrBatchTraceLinkType.WORK_ORDER)
                .setSourceObjectType("WORK_ORDER")
                .setSourceObjectId(11L)
                .setSnapshotJson("{\"workOrderId\":11}")
                .setSnapshotHash("not-the-sha256");
        MesProEdhrBatchTraceCaptureCommand command = activeCommand().setSources(List.of(source));

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_CONFLICT, result.blockerCode());
    }

    @Test
    void callerSuppliedSourceIdentityKeyMustMatchCanonicalFormalIdentity() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand();
        MesProEdhrBatchTraceSource source = command.getSources().stream()
                .filter(item -> MesProEdhrBatchTraceLinkType.WORK_ORDER.equals(item.getLinkType()))
                .findFirst().orElseThrow()
                .setSourceIdentityKey("caller-selected-identity");
        command.setSourceBundleHash(validator.calculateSourceBundleHash(command.getSources()));

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_CONFLICT, result.blockerCode());
        assertEquals("caller-selected-identity", source.getSourceIdentityKey());
    }

    @Test
    void activeOrderRequiresCompleteFormalSourceCoverage() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setSources(List.of(source(MesProEdhrBatchTraceLinkType.WORK_ORDER, 8L)));
        command.setSourceBundleHash(validator.calculateSourceBundleHash(command.getSources()));

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_REQUIRED, result.blockerCode());
    }

    @Test
    void actualLossRequiresLossReportReceipt() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand().setHasActualLoss(true);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_REQUIRED, result.blockerCode());
    }

    @Test
    void noLossRequiresNoLossConfirmedRelationStatus() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand();
        MesProEdhrBatchTraceSource lossFact = command.getSources().stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED.equals(source.getLinkType()))
                .findFirst().orElseThrow()
                .setRelationStatus("HAS_LOSS");
        command.setSourceBundleHash(validator.calculateSourceBundleHash(command.getSources()));

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.ENTRY_SCENARIO_MISMATCH, result.blockerCode());
    }

    @Test
    void actualLossWithFormalLossSourcesIsValid() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand().setHasActualLoss(true);
        List<MesProEdhrBatchTraceSource> sources = new ArrayList<>(command.getSources());
        sources.removeIf(source -> MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED.equals(source.getLinkType()));
        sources.add(source(MesProEdhrBatchTraceLinkType.LOSS_FACT, 60L).setRelationStatus("HAS_LOSS"));
        sources.add(source(MesProEdhrBatchTraceLinkType.LOSS_REPORT_RECEIPT, 61L));
        command.setSources(sources).setSourceBundleHash(validator.calculateSourceBundleHash(sources));

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertTrue(result.valid(), result.blockerCode());
    }

    @Test
    void sourceSnapshotHashMustMatchPickListSource() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setSourceSnapshotHash("not-the-pick-list-snapshot");

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_CONFLICT, result.blockerCode());
    }

    @Test
    void sameFormalSourceWithDifferentSnapshotHashBlocksCapture() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand();
        List<MesProEdhrBatchTraceSource> sources = new ArrayList<>(command.getSources());
        String changedSnapshot = "{\"sourceId\":20,\"revision\":2}";
        sources.add(new MesProEdhrBatchTraceSource()
                .setLinkType(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE)
                .setSourceObjectType(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE)
                .setSourceObjectId(20L)
                .setSourceVersion(2)
                .setSnapshotJson(changedSnapshot)
                .setSnapshotHash(DigestUtil.sha256Hex(
                        MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(changedSnapshot))));
        command.setSources(sources).setSourceBundleHash(validator.calculateSourceBundleHash(sources));

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_CONFLICT, result.blockerCode());
    }

    @Test
    void formalSourceIdsMustMatchCaptureCommand() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand().setActiveOrderId(999L);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_CONFLICT, result.blockerCode());
    }

    @Test
    void everyMaterialSourceMustMatchFrozenSnapshot() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand();
        MesProEdhrBatchTraceSource line = command.getSources().stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE.equals(source.getLinkType()))
                .findFirst().orElseThrow();
        String changedSnapshot = "{\"sourceId\":21,\"revision\":2}";
        line.setSnapshotJson(changedSnapshot).setSnapshotHash(DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(changedSnapshot)));
        command.setSourceBundleHash(validator.calculateSourceBundleHash(command.getSources()));

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_CONFLICT, result.blockerCode());
    }

    @Test
    void completionReceiptIdentityAndHashMustMatchFormalSource() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand()
                .setCompletionBackfillReceiptId(999L);

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertEquals(MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_CONFLICT, result.blockerCode());
    }

    @Test
    void completionReceiptUsesItsImmutableRawBodyHash() {
        MesProEdhrBatchTraceCaptureCommand command = activeCommand();
        MesProEdhrBatchTraceSource receiptSource = command.getSources().stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT.equals(source.getLinkType()))
                .findFirst().orElseThrow();
        String immutableReceiptBody = "{\"z\":1,\"a\":2}";
        String receiptHash = DigestUtil.sha256Hex(immutableReceiptBody);
        receiptSource.setSnapshotJson(immutableReceiptBody).setSnapshotHash(receiptHash);
        command.setCompletionBackfillReceiptHash(receiptHash)
                .setSourceBundleHash(validator.calculateSourceBundleHash(command.getSources()));

        MesProEdhrBatchTraceValidationResult result = validator.validate(command);

        assertTrue(result.valid(), result.blockerCode() + ":" + result.blockerScope());
    }

    @Test
    void persistedCompletionReceiptLinkUsesTheSameImmutableRawBodyHash() throws Exception {
        String immutableReceiptBody = "{\"z\":1,\"a\":2}";
        MesProEdhrBatchExecutionTraceLinkDO link = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .linkType(MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT)
                .sourceObjectType("COMPLETION_RECEIPT")
                .sourceObjectId(10L)
                .sourceIdentityKey("COMPLETION_BACKFILL_RECEIPT:COMPLETION_RECEIPT:10::")
                .snapshotJson(immutableReceiptBody)
                .snapshotHash(DigestUtil.sha256Hex(immutableReceiptBody))
                .build();
        Method method = MesProEdhrBatchTraceabilityServiceImpl.class
                .getDeclaredMethod("isTraceLinkIntegrityValid", MesProEdhrBatchExecutionTraceLinkDO.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(null, link));
    }

    private MesProEdhrBatchTraceCaptureCommand activeCommand() {
        List<MesProEdhrBatchTraceSource> sources = List.of(
                source(MesProEdhrBatchTraceLinkType.ACTIVE_ORDER, 7L),
                source(MesProEdhrBatchTraceLinkType.WORK_ORDER, 8L),
                source(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, 20L),
                source(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE, 21L),
                source(MesProEdhrBatchTraceLinkType.PRODUCTION_SUBMIT, 30L),
                source(MesProEdhrBatchTraceLinkType.PRODUCTION_SIGNATURE, 31L),
                source(MesProEdhrBatchTraceLinkType.PRODUCTION_LEADER_REVIEW, 32L),
                source(MesProEdhrBatchTraceLinkType.PQC_TASK, 40L),
                source(MesProEdhrBatchTraceLinkType.PQC_SUBMISSION, 41L),
                source(MesProEdhrBatchTraceLinkType.PQC_SIGNATURE, 42L),
                source(MesProEdhrBatchTraceLinkType.PQC_LEADER_CONFIRMATION, 43L),
                source(MesProEdhrBatchTraceLinkType.PQC_AGGREGATE_DETAIL, 44L),
                source(MesProEdhrBatchTraceLinkType.BATCH_RECORD_RECEIPT, 50L),
                source(MesProEdhrBatchTraceLinkType.PROCESS_INSPECTION_RECEIPT, 51L),
                source(MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT, 10L),
                source(MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT, 13L),
                source(MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED, 60L)
                        .setRelationStatus("NO_LOSS"));
        MesProEdhrBatchTraceSource pickListSource = sources.stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE.equals(source.getLinkType()))
                .findFirst().orElseThrow();
        MesProEdhrBatchTraceSource pickListLineSource = sources.stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE.equals(source.getLinkType()))
                .findFirst().orElseThrow();
        pickListLineSource.setSnapshotJson(pickListSource.getSnapshotJson())
                .setSnapshotHash(pickListSource.getSnapshotHash());
        MesProEdhrBatchTraceSource provisionSource = sources.stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT.equals(source.getLinkType()))
                .findFirst().orElseThrow();
        provisionSource.setSnapshotJson(pickListSource.getSnapshotJson())
                .setSnapshotHash(pickListSource.getSnapshotHash());
        String pickListSnapshotHash = sources.stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE.equals(source.getLinkType()))
                .findFirst().orElseThrow().getSnapshotHash();
        String completionReceiptHash = sources.stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT.equals(source.getLinkType()))
                .findFirst().orElseThrow().getSnapshotHash();
        return new MesProEdhrBatchTraceCaptureCommand()
                .setBatchExecutionId(101L)
                .setEntryType(MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION)
                .setOriginKey("tenant:active:7:completion:9")
                .setActiveOrderId(7L)
                .setWorkOrderId(8L)
                .setCompletionTransactionId(9L)
                .setCompletionVersion(1)
                .setCompletionBackfillReceiptId(10L)
                .setCompletionBackfillReceiptHash(completionReceiptHash)
                .setPickListBindingId(12L)
                .setPickListId(20L)
                .setPickListBindingVersion(1)
                .setHasActualLoss(false)
                .setSourceSnapshotHash(pickListSnapshotHash)
                .setBatchProvisionReceiptId(13L)
                .setBatchProvisionStatus(MesProEdhrBatchTraceProvisionStatus.CREATED)
                .setSourceBundleHash(validator.calculateSourceBundleHash(sources))
                .setIdempotencyKey("idempotency-key")
                .setSources(sources);
    }

    private MesProEdhrBatchTraceSource source(String linkType, Long sourceId) {
        String snapshot = "{\"sourceId\":" + sourceId + "}";
        return new MesProEdhrBatchTraceSource()
                .setLinkType(linkType)
                .setSourceObjectType(linkType)
                .setSourceObjectId(sourceId)
                .setSnapshotJson(snapshot)
                .setSnapshotHash(DigestUtil.sha256Hex(
                        MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(snapshot)));
    }
}
