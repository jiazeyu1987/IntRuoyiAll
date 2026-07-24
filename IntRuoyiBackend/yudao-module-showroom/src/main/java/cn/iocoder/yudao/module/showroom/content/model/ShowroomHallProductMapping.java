package cn.iocoder.yudao.module.showroom.content.model;

import java.math.BigDecimal;

public record ShowroomHallProductMapping(Long productId, Integer displayOrder,
                                         BigDecimal layoutX, BigDecimal layoutY,
                                         BigDecimal layoutWidth, BigDecimal layoutHeight) {

    public ShowroomHallProductMapping {
        layoutX = normalize(layoutX);
        layoutY = normalize(layoutY);
        layoutWidth = normalize(layoutWidth);
        layoutHeight = normalize(layoutHeight);
    }

    public ShowroomHallProductMapping(Long productId, Integer displayOrder) {
        this(productId, displayOrder, null, null, null, null);
    }

    public boolean hasCompleteLayout() {
        return layoutX != null && layoutY != null && layoutWidth != null && layoutHeight != null;
    }

    public boolean hasAnyLayout() {
        return layoutX != null || layoutY != null || layoutWidth != null || layoutHeight != null;
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

}
