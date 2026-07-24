package cn.iocoder.yudao.module.showroom.content.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class ShowroomHallCanvasLayoutPolicy {

    private static final double EPSILON = 0.000001D;
    private static final int SCALE = 6;

    private ShowroomHallCanvasLayoutPolicy() {
    }

    public static List<ShowroomHallProductMapping> withDefaultLayoutIfMissing(
            List<ShowroomHallProductMapping> orderedMappings) {
        boolean anyLayout = orderedMappings.stream().anyMatch(ShowroomHallProductMapping::hasAnyLayout);
        boolean allLayout = orderedMappings.stream().allMatch(ShowroomHallProductMapping::hasCompleteLayout);
        if (allLayout) {
            validateCompleteCoverage(orderedMappings);
            return List.copyOf(orderedMappings);
        }
        if (anyLayout) {
            throw new IllegalStateException(
                    "SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas layout must be complete for every product mapping");
        }
        return defaultLayout(orderedMappings);
    }

    public static List<ShowroomHallProductMapping> requireCanvasLayout(
            List<ShowroomHallProductMapping> orderedMappings) {
        for (ShowroomHallProductMapping mapping : orderedMappings) {
            if (!mapping.hasCompleteLayout()) {
                throw new IllegalStateException(
                        "SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas layout is required");
            }
        }
        validateCompleteCoverage(orderedMappings);
        return List.copyOf(orderedMappings);
    }

    private static List<ShowroomHallProductMapping> defaultLayout(List<ShowroomHallProductMapping> orderedMappings) {
        int size = orderedMappings.size();
        if (size == 0) {
            return List.of();
        }
        int rows = Math.max(1, (int) Math.floor(Math.sqrt(size)));
        int columns = (int) Math.ceil((double) size / rows);
        List<ShowroomHallProductMapping> result = new ArrayList<>();
        int index = 0;
        for (int row = 0; row < rows && index < size; row++) {
            int remaining = size - index;
            int rowCount = Math.min(columns, remaining);
            BigDecimal y = ratio(row, rows);
            BigDecimal nextY = ratio(row + 1, rows);
            BigDecimal height = nextY.subtract(y);
            for (int column = 0; column < rowCount; column++) {
                BigDecimal x = ratio(column, rowCount);
                BigDecimal nextX = ratio(column + 1, rowCount);
                ShowroomHallProductMapping mapping = orderedMappings.get(index++);
                result.add(new ShowroomHallProductMapping(mapping.productId(), mapping.displayOrder(),
                        x, y, nextX.subtract(x), height));
            }
        }
        validateCompleteCoverage(result);
        return result;
    }

    private static void validateCompleteCoverage(List<ShowroomHallProductMapping> mappings) {
        double area = 0D;
        for (int index = 0; index < mappings.size(); index++) {
            ShowroomHallProductMapping mapping = mappings.get(index);
            double x = requireNormalized(mapping.layoutX(), "layout_x");
            double y = requireNormalized(mapping.layoutY(), "layout_y");
            double width = requirePositiveNormalized(mapping.layoutWidth(), "layout_width");
            double height = requirePositiveNormalized(mapping.layoutHeight(), "layout_height");
            if (x + width > 1D + EPSILON || y + height > 1D + EPSILON) {
                throw new IllegalStateException("SHOWROOM_CANVAS_LAYOUT_INVALID: hall canvas rectangle exceeds bounds");
            }
            area += width * height;
            for (int otherIndex = index + 1; otherIndex < mappings.size(); otherIndex++) {
                if (overlaps(mapping, mappings.get(otherIndex))) {
                    throw new IllegalStateException("SHOWROOM_CANVAS_LAYOUT_INVALID: hall canvas rectangles overlap");
                }
            }
        }
        if (Math.abs(area - 1D) > EPSILON) {
            throw new IllegalStateException("SHOWROOM_CANVAS_LAYOUT_INVALID: hall canvas rectangles must cover full area");
        }
    }

    private static boolean overlaps(ShowroomHallProductMapping left, ShowroomHallProductMapping right) {
        double leftX = left.layoutX().doubleValue();
        double leftY = left.layoutY().doubleValue();
        double rightX = right.layoutX().doubleValue();
        double rightY = right.layoutY().doubleValue();
        return Math.min(leftX + left.layoutWidth().doubleValue(), rightX + right.layoutWidth().doubleValue())
                - Math.max(leftX, rightX) > EPSILON
                && Math.min(leftY + left.layoutHeight().doubleValue(), rightY + right.layoutHeight().doubleValue())
                - Math.max(leftY, rightY) > EPSILON;
    }

    private static double requireNormalized(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas " + fieldName + " is required");
        }
        double number = value.doubleValue();
        if (!Double.isFinite(number) || number < -EPSILON || number > 1D + EPSILON) {
            throw new IllegalStateException("SHOWROOM_CANVAS_LAYOUT_INVALID: hall canvas " + fieldName + " is out of range");
        }
        return number;
    }

    private static double requirePositiveNormalized(BigDecimal value, String fieldName) {
        double number = requireNormalized(value, fieldName);
        if (number <= EPSILON) {
            throw new IllegalStateException("SHOWROOM_CANVAS_LAYOUT_INVALID: hall canvas " + fieldName + " must be positive");
        }
        return number;
    }

    private static BigDecimal ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            throw new IllegalStateException("SHOWROOM_CANVAS_LAYOUT_INVALID: hall canvas denominator is invalid");
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), SCALE, RoundingMode.HALF_UP);
    }

}
