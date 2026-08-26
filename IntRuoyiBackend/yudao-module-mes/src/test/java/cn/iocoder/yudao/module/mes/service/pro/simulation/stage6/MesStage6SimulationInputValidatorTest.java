package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesStage6SimulationInputValidatorTest {

    @Test
    void acceptsIdiInputGoodAndLossQuantities() {
        assertDoesNotThrow(() -> MesStage6SimulationInputValidator.validate(
                "ID", new BigDecimal("100"), new BigDecimal("98"), new BigDecimal("2")));
    }

    @Test
    void rejectsNonIdiProduct() {
        assertThrows(IllegalArgumentException.class, () -> MesStage6SimulationInputValidator.validate(
                "OTHER", new BigDecimal("100"), new BigDecimal("98"), new BigDecimal("2")));
    }

    @Test
    void rejectsQuantityMismatch() {
        assertThrows(IllegalArgumentException.class, () -> MesStage6SimulationInputValidator.validate(
                "ID", new BigDecimal("100"), new BigDecimal("99"), new BigDecimal("2")));
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> MesStage6SimulationInputValidator.validate(
                "ID", new BigDecimal("100"), new BigDecimal("98"), new BigDecimal("-2")));
    }
}
