package cn.iocoder.yudao.module.erp.service.sync;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.erp.service.sync.ErpKingdeeOrderLineIdentity.*;

class ErpKingdeeOrderLineIdentityTest {
    private Line line(Long id, String source, String count) {
        return new Line(id, source, 10L, 20L, BigDecimal.ONE, new BigDecimal(count), null, null);
    }

    @Test void sourceReorderingAndQuantityChangesRetainIdentity() {
        assertEquals(Map.of("a", 1L, "b", 2L), reconcile(
                List.of(line(1L, "a", "5"), line(2L, "b", "5")),
                List.of(line(null, "b", "9"), line(null, "a", "7")), List.of()));
    }

    @Test void previousSnapshotAdoptsIdentityIgnoringDecimalScale() {
        assertEquals(Map.of("a", 1L), reconcile(List.of(line(1L, null, "5")),
                List.of(line(null, "a", "7")), List.of(line(null, "a", "5.000"))));
    }

    @Test void identicalLegacyRowsAreAmbiguousAndMustNotBeGuessed() {
        assertThrows(IllegalStateException.class, () -> reconcile(List.of(line(1L, null, "5")),
                List.of(line(null, "a", "7")), List.of(line(null, "a", "5"), line(null, "b", "5"))));
    }

    @Test void currentSourceValuesCannotReplaceMissingPreviousSnapshot() {
        assertThrows(IllegalStateException.class, () -> reconcile(List.of(line(1L, null, "5")),
                List.of(line(null, "a", "5")), List.of()));
    }

    @Test void missingOrDuplicateSourceIdentityFails() {
        for (String source : new String[]{null, "", " "}) {
            assertThrows(IllegalStateException.class, () -> reconcile(List.of(), List.of(line(null, source, "5")), List.of()));
        }
        assertThrows(IllegalStateException.class, () -> reconcile(List.of(),
                List.of(line(null, "a", "5"), line(null, "a", "7")), List.of()));
    }

    @Test void duplicateStoredIdentityFails() {
        assertThrows(IllegalStateException.class, () -> reconcile(List.of(line(1L, "a", "5"), line(2L, "a", "7")),
                List.of(line(null, "a", "5")), List.of()));
    }

    @Test void oneHistoricalIdentityCannotBeAssignedTwice() {
        assertThrows(IllegalStateException.class, () -> reconcile(List.of(line(1L, "a", "5"), line(2L, null, "5")),
                List.of(line(null, "a", "5")), List.of(line(null, "a", "5"))));
    }

    @Test void unknownStoredQuantityUsesVerifiedActualDocumentsAndRejectsContradiction() {
        assertEquals(BigDecimal.TEN, quantity(Map.of(1L, BigDecimal.TEN), 1L, null));
        assertEquals(BigDecimal.ZERO, quantity(Map.of(), 1L, null));
        assertThrows(IllegalStateException.class, () -> quantity(Map.of(), 1L, BigDecimal.ONE));
        assertThrows(IllegalStateException.class, () -> quantity(Map.of(1L, BigDecimal.ONE.negate()), 1L, null));
    }
}
