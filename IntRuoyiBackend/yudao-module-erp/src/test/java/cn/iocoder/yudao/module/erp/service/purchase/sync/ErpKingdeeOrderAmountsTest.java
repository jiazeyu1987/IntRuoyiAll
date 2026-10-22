package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ErpKingdeeOrderAmountsTest {
    @Test void convertsPriceFromPricingUnitToTransactionUnit() {
        var result = ErpKingdeeOrderAmounts.transactionUnitPrice(new BigDecimal("100"),
                new BigDecimal("20"), new BigDecimal("2"), "个", "箱");
        assertEquals(0, new BigDecimal("10").compareTo(result));
    }

    @Test void missingConversionQuantityFailsInsteadOfAssumingSameUnit() {
        assertThrows(IllegalStateException.class, () -> ErpKingdeeOrderAmounts.transactionUnitPrice(
                BigDecimal.TEN, BigDecimal.ONE, null, "个", "箱"));
        assertThrows(IllegalStateException.class, () -> ErpKingdeeOrderAmounts.transactionUnitPrice(
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ONE, "个", "箱"));
    }

    @Test void rejectsUnbalancedEntryBeforePersistingFinancialSnapshot() {
        var snapshot = new ErpKingdeeOrderAmounts();
        snapshot.setProductAmount(new BigDecimal("8.85"));
        snapshot.setTaxAmount(new BigDecimal("1.15"));
        snapshot.setTotalAmount(BigDecimal.TEN);
        snapshot.setDiscountAmount(BigDecimal.ZERO);
        var line = new ErpKingdeeOrderAmounts.Line();
        line.setNetAmount(new BigDecimal("8.85"));
        line.setTaxAmount(new BigDecimal("1.15"));
        line.setTotalAmount(new BigDecimal("11"));
        snapshot.setLines(List.of(line));
        assertThrows(IllegalStateException.class, () -> snapshot.validate(1));
    }
}
