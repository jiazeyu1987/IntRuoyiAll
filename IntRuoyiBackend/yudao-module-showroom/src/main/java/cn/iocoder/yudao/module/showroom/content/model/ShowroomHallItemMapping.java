package cn.iocoder.yudao.module.showroom.content.model;

import java.math.BigDecimal;

public record ShowroomHallItemMapping(String itemType, Long itemId, Integer displayOrder,
                                      BigDecimal layoutX, BigDecimal layoutY,
                                      BigDecimal layoutWidth, BigDecimal layoutHeight) {

    public static final String TYPE_PRODUCT = "PRODUCT";
    public static final String TYPE_AWARD = "AWARD";

    public ShowroomHallItemMapping {
        itemType = normalizeType(itemType);
        layoutX = normalize(layoutX);
        layoutY = normalize(layoutY);
        layoutWidth = normalize(layoutWidth);
        layoutHeight = normalize(layoutHeight);
    }

    public ShowroomHallItemMapping(String itemType, Long itemId, Integer displayOrder) {
        this(itemType, itemId, displayOrder, null, null, null, null);
    }

    public ShowroomHallProductMapping asProductMapping() {
        if (!TYPE_PRODUCT.equals(itemType)) {
            throw new IllegalStateException("SHOWROOM_HALL_ITEM_TYPE_INVALID: hall item is not PRODUCT");
        }
        return new ShowroomHallProductMapping(itemId, displayOrder, layoutX, layoutY, layoutWidth, layoutHeight);
    }

    public boolean hasCompleteLayout() {
        return layoutX != null && layoutY != null && layoutWidth != null && layoutHeight != null;
    }

    public boolean hasAnyLayout() {
        return layoutX != null || layoutY != null || layoutWidth != null || layoutHeight != null;
    }

    private static String normalizeType(String itemType) {
        if (itemType == null || itemType.trim().isEmpty()) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: hall item type is required");
        }
        String normalized = itemType.trim().toUpperCase();
        if (!TYPE_PRODUCT.equals(normalized) && !TYPE_AWARD.equals(normalized)) {
            throw new IllegalStateException("SHOWROOM_HALL_ITEM_TYPE_INVALID: " + itemType);
        }
        return normalized;
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}
