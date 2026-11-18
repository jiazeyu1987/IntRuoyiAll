package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ErpKingdeeSourceAmountValidationTest {
    private ErpKingdeeOrderAmounts snapshot() {
        var source = new ErpKingdeeOrderAmounts();
        source.setProductAmount(BigDecimal.ZERO);
        source.setTaxAmount(BigDecimal.ZERO);
        source.setTotalAmount(new BigDecimal("448.25"));
        source.setDiscountAmount(BigDecimal.ZERO);
        var line = new ErpKingdeeOrderAmounts.Line();
        line.setNetAmount(BigDecimal.ZERO);
        line.setTaxAmount(BigDecimal.ZERO);
        line.setTotalAmount(new BigDecimal("448.25"));
        source.setLines(List.of(line));
        return source;
    }

    @Test void completeSourcePreservesDifferencesWhileStrictValidationRejectsThem() {
        var source = snapshot();
        assertDoesNotThrow(() -> source.validateSourceSnapshot(1));
        assertEquals(BigDecimal.ZERO, source.getProductAmount());
        assertEquals(BigDecimal.ZERO, source.getLines().get(0).getTaxAmount());
        assertEquals(new BigDecimal("448.25"), source.getTotalAmount());
        assertEquals(new BigDecimal("448.25"), source.getLines().get(0).getTotalAmount());
        assertThrows(IllegalStateException.class, () -> source.validate(1));
    }

    @Test void headerOnlyDifferenceRemainsAcceptedOnlyForSourceSnapshots() {
        var source = snapshot();
        source.getLines().get(0).setTotalAmount(BigDecimal.ZERO);
        assertDoesNotThrow(() -> source.validateSourceSnapshot(1));
        assertTrue(assertThrows(IllegalStateException.class, () -> source.validate(1)).getMessage().contains("header"));
    }

    @Test void missingHeaderAmountStillFails() {
        var source = snapshot();
        source.setDiscountAmount(null);
        assertThrows(IllegalStateException.class, () -> source.validateSourceSnapshot(1));
    }

    @Test void missingLineAmountStillFails() {
        var source = snapshot();
        source.getLines().get(0).setTotalAmount(null);
        assertThrows(IllegalStateException.class, () -> source.validateSourceSnapshot(1));
    }

    @Test void mismatchedLineCountStillFails() {
        assertThrows(IllegalStateException.class, () -> snapshot().validateSourceSnapshot(2));
    }

    @Test void nullLineStillFailsExplicitly() {
        var source = snapshot();
        source.setLines(Arrays.asList((ErpKingdeeOrderAmounts.Line) null));
        assertThrows(IllegalStateException.class, () -> source.validateSourceSnapshot(1));
    }
}
