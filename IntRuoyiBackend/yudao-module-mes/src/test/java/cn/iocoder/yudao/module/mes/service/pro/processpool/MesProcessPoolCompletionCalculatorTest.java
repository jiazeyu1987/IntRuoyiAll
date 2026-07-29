package cn.iocoder.yudao.module.mes.service.pro.processpool;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolCompletionCalculatorTest {

    private final MesProcessPoolCompletionCalculator calculator = new MesProcessPoolCompletionCalculator();

    @Test
    void shouldCompleteProcessByCumulativeQuantityAcrossEmployeesAndEvents() {
        MesProcessPoolCompletionResult result = calculator.calculate(MesProcessPoolCompletionCommand.of(
                10L,
                5000L,
                new BigDecimal("100"),
                List.of(
                        MesProcessPoolSubmissionQuantity.of(1000L, 11L, new BigDecimal("30")),
                        MesProcessPoolSubmissionQuantity.of(1001L, 12L, new BigDecimal("40")),
                        MesProcessPoolSubmissionQuantity.of(1002L, 11L, new BigDecimal("30"))
                )));

        assertTrue(result.isCompleted());
        assertAmount("100", result.getTotalSubmittedQuantity());
        assertEquals(3, result.getSubmissionCount());
        assertEquals(List.of(1000L, 1001L, 1002L), result.getSubmittedEventIds());
        assertEquals(List.of(11L, 12L), result.getEmployeeUserIds());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
