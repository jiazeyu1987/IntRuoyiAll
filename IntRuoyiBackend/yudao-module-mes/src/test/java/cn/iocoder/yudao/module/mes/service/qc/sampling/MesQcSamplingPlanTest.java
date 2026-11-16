package cn.iocoder.yudao.module.mes.service.qc.sampling;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MesQcSamplingPlanTest {
    @Test void sameLotUsesIndependentLevels() {
        var general = MesQcSamplingPlan.calculate(new BigDecimal("3000"), "I");
        var special = MesQcSamplingPlan.calculate(new BigDecimal("3000"), "S3");
        assertEquals("H", general.sampleCode());
        assertEquals(50, general.inspectionQuantity());
        assertEquals("E", special.sampleCode());
        assertEquals(13, special.inspectionQuantity());
        assertFalse(general.fullInspection());
    }
    @Test void respectsLotRangeBoundaries() {
        assertEquals(32, MesQcSamplingPlan.calculate(new BigDecimal("1200"), "I").inspectionQuantity());
        assertEquals(50, MesQcSamplingPlan.calculate(new BigDecimal("1201"), "I").inspectionQuantity());
        assertEquals(50, MesQcSamplingPlan.calculate(new BigDecimal("3200"), "I").inspectionQuantity());
        assertEquals(80, MesQcSamplingPlan.calculate(new BigDecimal("3201"), "I").inspectionQuantity());
    }
    @Test void capsAtLotAndHandlesOnePieceWithoutInventingCode() {
        var two = MesQcSamplingPlan.calculate(new BigDecimal("2"), "III");
        assertEquals(3, two.nominalSampleSize());
        assertEquals(2, two.inspectionQuantity());
        assertTrue(two.fullInspection());
        var one = MesQcSamplingPlan.calculate(BigDecimal.ONE, "S3");
        assertEquals(1, one.inspectionQuantity());
        assertNull(one.sampleCode());
        assertNull(one.nominalSampleSize());
        assertTrue(one.fullInspection());
    }
    @Test void rejectsMissingLevelAndInvalidLotWithoutPercentFallback() {
        for (String level : new String[]{null, "", "S-", "0.4", "IV"}) {
            assertThrows(IllegalArgumentException.class, () -> MesQcSamplingPlan.calculate(BigDecimal.TEN, level));
        }
        for (BigDecimal quantity : new BigDecimal[]{null, BigDecimal.ZERO, new BigDecimal("-1"),
                new BigDecimal("3.2"), new BigDecimal("9223372036854775808")}) {
            assertThrows(IllegalArgumentException.class, () -> MesQcSamplingPlan.calculate(quantity, "I"));
        }
    }
}
