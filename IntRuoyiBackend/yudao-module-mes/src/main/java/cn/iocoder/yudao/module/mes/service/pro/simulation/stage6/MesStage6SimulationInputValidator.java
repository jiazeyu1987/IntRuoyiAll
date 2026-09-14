package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import java.math.BigDecimal;
import java.util.Objects;

/** Validates the fixed business input for the ID Stage6 fixture. */
public final class MesStage6SimulationInputValidator {

    public static final String PRODUCT_CODE = "ID";

    private MesStage6SimulationInputValidator() {
    }

    public static void validate(String productCode,
                                BigDecimal inputQuantity,
                                BigDecimal goodQuantity,
                                BigDecimal lossQuantity) {
        if (!Objects.equals(PRODUCT_CODE, productCode)) {
            throw new IllegalArgumentException("Stage6 simulation only supports product code ID");
        }
        requireNonNegative(inputQuantity, "inputQuantity");
        requireNonNegative(goodQuantity, "goodQuantity");
        requireNonNegative(lossQuantity, "lossQuantity");
        if (inputQuantity.signum() <= 0) {
            throw new IllegalArgumentException("inputQuantity must be greater than zero");
        }
        if (goodQuantity.add(lossQuantity).compareTo(inputQuantity) != 0) {
            throw new IllegalArgumentException("goodQuantity plus lossQuantity must equal inputQuantity");
        }
    }

    private static void requireNonNegative(BigDecimal quantity, String fieldName) {
        if (quantity == null || quantity.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }
}
