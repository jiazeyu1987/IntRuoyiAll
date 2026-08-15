package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

public final class PqcResultValueValidator {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    private static final Set<String> BOOLEAN_VALUES = Set.of("合格", "不合格");
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)");

    private PqcResultValueValidator() {
    }

    public static ValidatedValue validate(String resultType, String rawValue,
                                          BigDecimal lowerLimit, BigDecimal upperLimit,
                                          Integer precision) {
        String value = rawValue == null ? null : rawValue.trim();
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("sample value is required");
        }
        return switch (resultType) {
            case "BOOLEAN" -> validateBoolean(value);
            case "NUMERIC" -> validateNumeric(value, lowerLimit, upperLimit, precision);
            case "TEXT" -> new ValidatedValue(value, SUCCESS);
            default -> throw new IllegalArgumentException("unsupported resultType: " + resultType);
        };
    }

    private static ValidatedValue validateBoolean(String value) {
        if (!BOOLEAN_VALUES.contains(value)) {
            throw new IllegalArgumentException("BOOLEAN only accepts 合格/不合格");
        }
        return new ValidatedValue(value, "不合格".equals(value) ? FAILURE : SUCCESS);
    }

    private static ValidatedValue validateNumeric(String value, BigDecimal lowerLimit,
                                                  BigDecimal upperLimit, Integer precision) {
        if (!PLAIN_DECIMAL.matcher(value).matches() || lowerLimit == null || upperLimit == null
                || precision == null || precision < 0 || lowerLimit.compareTo(upperLimit) > 0) {
            throw new IllegalArgumentException("invalid NUMERIC contract or value");
        }
        BigDecimal numeric = new BigDecimal(value);
        BigDecimal normalized = numeric.stripTrailingZeros();
        int normalizedScale = Math.max(normalized.scale(), 0);
        if (normalizedScale > precision) {
            throw new IllegalArgumentException("NUMERIC precision exceeded");
        }
        String judgement = numeric.compareTo(lowerLimit) < 0 || numeric.compareTo(upperLimit) > 0
                ? FAILURE : SUCCESS;
        return new ValidatedValue(normalized.toPlainString(), judgement);
    }

    public record ValidatedValue(String normalizedValue, String judgement) {
    }
}
