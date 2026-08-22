package cn.iocoder.yudao.module.mes.productionrelease.core;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesReleaseFinalizationValidatorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void activeOrderRequiresCompletionAndBackfillReceipts() {
        MesReleaseFinalizationCommand command = base(MesReleaseOrigin.ACTIVE_ORDER)
                .setActiveOrderId(10L)
                .setPickListBindingId("pick-binding-1")
                .setCompletionEventId("completion-1")
                .setCompletionBackfillReceiptId("backfill-1")
                .setDualProgressCompleted(false)
                .setThreeBackfillsSucceeded(true);

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> MesReleaseFinalizationValidator.validate(command, CLOCK));

        assertEquals(MesReleaseFlowBlockerType.PRODUCTION_PROGRESS_NOT_COMPLETED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
    }

    @Test
    void independentSourceDoesNotRequireActiveOrderFieldsButRequiresCanonicalReceipt() {
        MesReleaseFinalizationCommand command = base(MesReleaseOrigin.MANUAL)
                .setIndependentPrerequisiteReceipt(new IndependentBatchPrerequisiteReceipt()
                        .setReceiptId("independent-1")
                        .setBatchExecutionId(20L)
                        .setSourceRelation("formal-source")
                        .setSourceSnapshotHash("source-hash")
                        .setReceiptHash("receipt-hash")
                        .setIssuedBy(100L)
                        .setIssuedAt(LocalDateTime.of(2026, 8, 21, 0, 0))
                        .setExpiresAt(LocalDateTime.of(2026, 8, 23, 0, 0))
                        .setVersion(1));

        assertDoesNotThrow(() -> MesReleaseFinalizationValidator.validate(command, CLOCK));
    }

    @Test
    void materialGateMustContainExactlyFourOfficialTypes() {
        MesReleaseFinalizationCommand command = base(MesReleaseOrigin.MANUAL)
                .setIndependentPrerequisiteReceipt(validIndependentReceipt());
        command.getMaterialGateReceipt().setMaterialTypeKeys(Set.of("INCOMING_INSPECTION_REPORT"));

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> MesReleaseFinalizationValidator.validate(command, CLOCK));

        assertEquals(MesReleaseFlowBlockerType.REPORT_SNAPSHOT_CHANGED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
    }

    @Test
    void independentReceiptMustMatchFormalSourceSnapshot() {
        MesReleaseFinalizationCommand command = base(MesReleaseOrigin.MANUAL)
                .setIndependentPrerequisiteReceipt(validIndependentReceipt()
                        .setSourceSnapshotHash("different-hash"));

        assertThrows(MesReleaseFlowBlockerException.class,
                () -> MesReleaseFinalizationValidator.validate(command, CLOCK));
    }

    @Test
    void independentReceiptMustMatchFormalSourceRelation() {
        MesReleaseFinalizationCommand command = base(MesReleaseOrigin.MANUAL)
                .setIndependentPrerequisiteReceipt(validIndependentReceipt()
                        .setSourceRelation("different-source"));

        assertThrows(MesReleaseFlowBlockerException.class,
                () -> MesReleaseFinalizationValidator.validate(command, CLOCK));
    }

    @Test
    void materialGateSourceSnapshotMustMatchRequestWhenProvided() {
        MesReleaseFinalizationCommand command = base(MesReleaseOrigin.MANUAL)
                .setIndependentPrerequisiteReceipt(validIndependentReceipt())
                .setMaterialGateSourceSnapshotHash("different-gate-source");

        assertThrows(MesReleaseFlowBlockerException.class,
                () -> MesReleaseFinalizationValidator.validate(command, authoritativeEvidence(command), CLOCK));
    }

    @Test
    void independentReceiptIssuedInTheFutureIsInvalid() {
        MesReleaseFinalizationCommand command = base(MesReleaseOrigin.MANUAL)
                .setIndependentPrerequisiteReceipt(validIndependentReceipt()
                        .setIssuedAt(LocalDateTime.of(2026, 8, 23, 0, 0)));

        assertThrows(MesReleaseFlowBlockerException.class,
                () -> MesReleaseFinalizationValidator.validate(command, CLOCK));
    }

    @Test
    void entryTypeMustMatchOrigin() {
        MesReleaseFinalizationCommand command = base(MesReleaseOrigin.MANUAL)
                .setIndependentPrerequisiteReceipt(validIndependentReceipt())
                .setEntryType("SCHEDULED");

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> MesReleaseFinalizationValidator.validate(command, CLOCK));

        assertEquals(MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                failure.getFailure().getBlockers().get(0).getBlockerType());
    }

    private MesReleaseFinalizationCommand base(MesReleaseOrigin origin) {
        return new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(1L)
                .setBatchExecutionId(20L)
                .setOrigin(origin)
                .setEntryType(origin.name())
                .setSourceRelation("formal-source")
                .setSourceSnapshotHash("source-hash")
                .setIdempotencyKey("release-1")
                .setActorUserId(100L)
                .setWorkTaskId(200L)
                .setExpectedVersion(1)
                .setSignoffEvidenceHash("signoff-hash")
                .setIndependentPrerequisiteReceiptId("independent-1")
                .setMaterialGateReceiptId("gate-1")
                .setMaterialGateReceipt(new MesReleaseMaterialGateReceipt()
                        .setReceiptId("gate-1")
                        .setBatchExecutionId(20L)
                        .setGateStatus(MesReleaseMaterialGateReceipt.STATUS_MATERIALS_READY)
                        .setMaterialTypeKeys(MesReleaseMaterialGateReceipt.REQUIRED_MATERIAL_TYPES)
                        .setManifestHash("gate-hash")
                        .setSourceSnapshotHash("source-hash")
                        .setMaterialVersionSetHash("version-set-hash")
                        .setReceiptHash("gate-receipt-hash")
                        .setIssuedBy(100L)
                        .setAuditEventId("gate-audit")
                        .setVersion(1));
    }

    private MesReleaseFinalizationEvidence authoritativeEvidence(MesReleaseFinalizationCommand command) {
        return new MesReleaseFinalizationEvidence()
                .setIndependentPrerequisiteReceipt(validIndependentReceipt())
                .setMaterialGateReceipt(command.getMaterialGateReceipt());
    }

    private IndependentBatchPrerequisiteReceipt validIndependentReceipt() {
        return new IndependentBatchPrerequisiteReceipt()
                .setReceiptId("independent-1")
                .setBatchExecutionId(20L)
                .setSourceRelation("formal-source")
                .setSourceSnapshotHash("source-hash")
                .setReceiptHash("receipt-hash")
                .setIssuedBy(100L)
                .setIssuedAt(LocalDateTime.of(2026, 8, 21, 0, 0))
                .setExpiresAt(LocalDateTime.of(2026, 8, 23, 0, 0))
                .setVersion(1);
    }
}
