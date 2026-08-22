package cn.iocoder.yudao.module.mes.productionrelease.core;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class MesReleaseFinalizationValidator {

    private MesReleaseFinalizationValidator() {
    }

    /**
     * Production finalization path. Evidence must have been loaded from flow owners before this method is called.
     */
    public static void validate(
            MesReleaseFinalizationCommand command,
            MesReleaseFinalizationEvidence evidence,
            Clock clock) {
        validateCommon(command);
        MesReleaseMaterialGateReceipt gate = evidence == null ? null : evidence.getMaterialGateReceipt();
        require(command.getMaterialGateReceiptId() != null
                        && gate != null
                        && command.getMaterialGateReceiptId().equals(gate.getReceiptId())
                        && gate.isCompleteFor(command.getBatchExecutionId())
                        && (command.getMaterialGateManifestHash() == null
                        || command.getMaterialGateManifestHash().equals(gate.getManifestHash()))
                        && (command.getMaterialGateSourceSnapshotHash() == null
                        || command.getMaterialGateSourceSnapshotHash().equals(gate.getSourceSnapshotHash())),
                MesReleaseFlowBlockerType.REPORT_SNAPSHOT_CHANGED,
                "flow 8 MATERIALS_READY receipt and manifest must come from the authoritative owner");

        if (command.getOrigin() == MesReleaseOrigin.ACTIVE_ORDER) {
            CompletionBackfillReceipt receipt = evidence == null ? null : evidence.getCompletionBackfillReceipt();
            requireText(command.getCompletionBackfillReceiptId(),
                    MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    "flow 4 completionBackfillReceiptId is required");
            require(receipt != null
                            && command.getCompletionBackfillReceiptId().equals(receipt.getReceiptId())
                            && receipt.isSuccessfulFor(command.getActiveOrderId(), command.getSourceSnapshotHash()),
                    MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    CompletionBackfillReceipt.CANONICAL_NAME
                            + " must be authoritative, immutable and BACKFILL_SUCCEEDED");
            require(command.getPickListId() != null
                            && command.getWorkOrderId() != null
                            && Objects.equals(command.getPickListBindingId(), receipt.getPickListBindingId())
                            && Objects.equals(command.getCompletionEventId(), receipt.getCompletionEventId()),
                    MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    "active-order finalization must match flow 1 binding and flow 4 completion event");
            require(Boolean.TRUE.equals(command.getDualProgressCompleted()),
                    MesReleaseFlowBlockerType.PRODUCTION_PROGRESS_NOT_COMPLETED,
                    "active-order dual progress must be 100% before finalization");
            require(Boolean.TRUE.equals(command.getThreeBackfillsSucceeded()),
                    MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    "three same-node backfills must be successful before finalization");
            require(command.getActiveOrderExpectedVersion() != null,
                    MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                    "active-order owner version is required for controlled release closure");
        } else {
            IndependentBatchPrerequisiteReceipt receipt =
                    evidence == null ? null : evidence.getIndependentPrerequisiteReceipt();
            requireText(command.getIndependentPrerequisiteReceiptId(),
                    MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    "independent prerequisite receipt id is required");
            require(receipt != null
                            && command.getIndependentPrerequisiteReceiptId().equals(receipt.getReceiptId())
                            && receipt.isValidFor(command.getOrigin().name(), clock)
                            && command.getBatchExecutionId().equals(receipt.getBatchExecutionId())
                            && Objects.equals(command.getSourceRelation(), receipt.getSourceRelation())
                            && Objects.equals(command.getSourceSnapshotHash(), receipt.getSourceSnapshotHash()),
                    MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    IndependentBatchPrerequisiteReceipt.CANONICAL_NAME
                            + " must be backend-issued, valid, unrevoked, unexpired and bound to the batch");
            require(command.getActiveOrderId() == null && command.getPickListId() == null
                            && command.getCompletionBackfillReceiptId() == null,
                    MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                    "independent finalization must not fabricate active-order completion fields");
        }
    }

    /**
     * Retained for pure core contract tests. The service never uses this overload for HTTP finalization because
     * request-body receipts are not authoritative.
     */
    @Deprecated
    public static void validate(MesReleaseFinalizationCommand command, Clock clock) {
        validateCommon(command);
        require(command.getMaterialGateReceipt() != null
                        && command.getMaterialGateReceipt().isCompleteFor(command.getBatchExecutionId()),
                MesReleaseFlowBlockerType.REPORT_SNAPSHOT_CHANGED,
                "flow 8 MATERIALS_READY receipt is required");
        if (command.getOrigin() == MesReleaseOrigin.ACTIVE_ORDER) {
            require(command.getActiveOrderId() != null, MesReleaseFlowBlockerType.PRODUCTION_PROGRESS_NOT_COMPLETED,
                    "active-order origin requires activeOrderId");
            requireText(command.getPickListBindingId(), MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    "flow 1 pickListBindingId is required");
            requireText(command.getCompletionEventId(), MesReleaseFlowBlockerType.PRODUCTION_PROGRESS_NOT_COMPLETED,
                    "flow 4 completionEventId is required");
            requireText(command.getCompletionBackfillReceiptId(), MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    "flow 4 BACKFILL_SUCCEEDED completion receipt is required");
            require(Boolean.TRUE.equals(command.getDualProgressCompleted()),
                    MesReleaseFlowBlockerType.PRODUCTION_PROGRESS_NOT_COMPLETED,
                    "active-order dual progress must be 100% before finalization");
            require(Boolean.TRUE.equals(command.getThreeBackfillsSucceeded()),
                    MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    "three same-node backfills must be successful before finalization");
            require(command.getActiveOrderExpectedVersion() != null,
                    MesReleaseFlowBlockerType.STATE_VERSION_CONFLICT,
                    "active-order owner version is required for controlled release closure");
        } else {
            require(command.getIndependentPrerequisiteReceipt() != null
                            && command.getIndependentPrerequisiteReceipt().isValidAt(clock)
                            && command.getBatchExecutionId().equals(
                            command.getIndependentPrerequisiteReceipt().getBatchExecutionId())
                            && Objects.equals(command.getSourceRelation(),
                            command.getIndependentPrerequisiteReceipt().getSourceRelation())
                            && Objects.equals(command.getSourceSnapshotHash(),
                            command.getIndependentPrerequisiteReceipt().getSourceSnapshotHash()),
                    MesReleaseFlowBlockerType.BATCH_RECORD_SOURCE_REQUIRED,
                    IndependentBatchPrerequisiteReceipt.CANONICAL_NAME
                            + " must be valid, unrevoked, unexpired and bound to batchExecutionId");
        }
    }

    private static void validateCommon(MesReleaseFinalizationCommand command) {
        Objects.requireNonNull(command, "finalization command must not be null");
        require(command.getReleaseTransactionId() != null,
                MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                "releaseTransactionId is required");
        require(command.getBatchExecutionId() != null,
                MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                "an existing batchExecutionId from flow 6 is required; finalizeRelease never creates a batch");
        require(command.getOrigin() != null,
                MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                "origin is required so source-specific prerequisites cannot be inferred");
        require(command.getEntryType() != null && !command.getEntryType().isBlank(),
                MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE, "entryType is required");
        require(command.getOrigin().name().equals(command.getEntryType()),
                MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                "entryType must match origin exactly");
        require(command.getSourceRelation() != null && !command.getSourceRelation().isBlank(),
                MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED, "formal source relation is required");
        require(command.getSourceSnapshotHash() != null && !command.getSourceSnapshotHash().isBlank(),
                MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED,
                "source snapshot hash is required");
        require(command.getIdempotencyKey() != null && !command.getIdempotencyKey().isBlank(),
                MesReleaseFlowBlockerType.IDEMPOTENCY_KEY_INVALID, "idempotencyKey is required");
        require(command.getActorUserId() != null && command.getWorkTaskId() != null
                        && command.getExpectedVersion() != null,
                MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                "actorUserId, workTaskId and expectedVersion are required for signed finalization");
        require(command.getSignoffEvidenceHash() != null && !command.getSignoffEvidenceHash().isBlank(),
                MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                "verified signoff evidence hash is required");
    }

    private static void requireText(String value, MesReleaseFlowBlockerType type, String reason) {
        require(value != null && !value.isBlank(), type, reason);
    }

    private static void require(boolean condition, MesReleaseFlowBlockerType type, String reason) {
        if (!condition) {
            throw new MesReleaseFlowBlockerException("release finalization precondition failed",
                    new MesReleaseFlowFailureRespVO()
                            .setStage(MesReleaseFlowStage.SP_4)
                            .setBlockers(List.of(new MesReleaseFlowBlocker()
                                    .setBlockerType(type)
                                    .setObjectType("RELEASE_FINALIZATION")
                                    .setReason(reason)
                                    .setSuggestion("provide the authoritative flow receipt and retry"))));
        }
    }
}
