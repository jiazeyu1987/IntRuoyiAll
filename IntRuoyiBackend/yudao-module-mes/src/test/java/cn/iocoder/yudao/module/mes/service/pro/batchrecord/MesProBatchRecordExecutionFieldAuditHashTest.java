package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordExecutionFieldAuditHashTest {

    @Test
    void canonicalizeTypedJson_preservesDistinctTypedValues() {
        assertEquals("\"\"", MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.STRING, ""));
        assertEquals("null", MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NULL, null));
        assertEquals("0", MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("0.0")));
        assertEquals("false", MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN, false));
        assertEquals("{\"a\":1,\"b\":2}", MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.JSON, Map.of("b", 2, "a", 1)));

        assertNotEquals(
                MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, ""),
                MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.NULL, null));
    }

    @Test
    void itemHashIncludesFieldPathDisplayReasonActorSignatureAndPreviousHash() {
        MesProBatchRecordExecutionFieldAuditItemHashInput input =
                MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                        .fieldPath("sheet[0].rows[1].cells[2].temperature")
                        .fieldKey("temperature")
                        .rowIndex(1)
                        .columnIndex(2)
                        .valueType(MesProBatchRecordExecutionFieldAuditValueType.NUMBER)
                        .oldValueJson("36.6")
                        .oldValueDisplay("36.6")
                        .oldValueHash(MesProBatchRecordExecutionFieldAuditHasher.sha256("old"))
                        .newValueJson("37.5")
                        .newValueDisplay("37.5")
                        .newValueHash(MesProBatchRecordExecutionFieldAuditHasher.sha256("new"))
                        .reasonCategory("CORRECTION")
                        .reasonText("operator correction")
                        .actorId(99L)
                        .actorName("QA")
                        .signatureProjectionHash(MesProBatchRecordExecutionFieldAuditHasher.sha256("signature"))
                        .previousHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                        .changedAt(LocalDateTime.of(2026, 5, 26, 10, 30))
                        .build();

        String original = MesProBatchRecordExecutionFieldAuditHasher.hashItem(input);

        assertEquals(64, original.length());
        assertTrue(original.matches("[0-9a-f]{64}"));
        assertNotEquals(original, MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                input.toBuilder().fieldPath("sheet[0].rows[1].cells[3].temperature").build()));
        assertNotEquals(original, MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                input.toBuilder().newValueDisplay("37.50").build()));
        assertNotEquals(original, MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                input.toBuilder().reasonText("different reason").build()));
        assertNotEquals(original, MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                input.toBuilder().actorId(100L).build()));
        assertNotEquals(original, MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                input.toBuilder().signatureProjectionHash(MesProBatchRecordExecutionFieldAuditHasher.sha256("other")).build()));
        assertNotEquals(original, MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                input.toBuilder().previousHash(MesProBatchRecordExecutionFieldAuditHasher.sha256("previous")).build()));
    }

    @Test
    void itemHashAllowsEmptyDisplayButRejectsMissingDisplay() {
        MesProBatchRecordExecutionFieldAuditItemHashInput input =
                MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                        .fieldPath("sheet[0].rows[1].cells[2].temperature")
                        .fieldKey("temperature")
                        .rowIndex(1)
                        .columnIndex(2)
                        .valueType(MesProBatchRecordExecutionFieldAuditValueType.STRING)
                        .oldValueJson("\"\"")
                        .oldValueDisplay("")
                        .oldValueHash(MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("\"\""))
                        .newValueJson("\"\"")
                        .newValueDisplay("")
                        .newValueHash(MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("\"\""))
                        .reasonCategory("CORRECTION")
                        .reasonText("operator correction")
                        .actorId(99L)
                        .actorName("QA")
                        .signatureProjectionHash(MesProBatchRecordExecutionFieldAuditHasher.sha256("signature"))
                        .previousHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                        .changedAt(LocalDateTime.of(2026, 5, 26, 10, 30))
                        .build();

        String hash = MesProBatchRecordExecutionFieldAuditHasher.hashItem(input);

        assertEquals(64, hash.length());
        assertThrows(IllegalArgumentException.class, () -> MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                input.toBuilder().oldValueDisplay(null).build()));
        assertThrows(IllegalArgumentException.class, () -> MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                input.toBuilder().newValueDisplay(null).build()));
    }

    @Test
    void hashVerificationStatus_contractIsFrozen() {
        Set<String> statuses = MesProBatchRecordExecutionFieldAuditHashVerificationStatus.names();

        assertEquals(Set.of(
                "VALID",
                "CHAIN_BROKEN",
                "SIGNATURE_MISMATCH",
                "SOURCE_MISSING",
                "CONCURRENCY_CONFLICT"
        ), statuses);
    }
}
