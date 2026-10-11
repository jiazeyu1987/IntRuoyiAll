package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/** ERP 正式财务快照。仅由同步服务设置，禁止从管理后台请求反序列化。 */
@Data
public class ErpKingdeeOrderAmounts {
    private BigDecimal productAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private List<Line> lines;

    @Data
    public static class Line {
        private BigDecimal netAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
    }

    /** ERP 单价基于计价单位，落库单价必须与订单数量的交易单位一致。 */
    public static BigDecimal transactionUnitPrice(BigDecimal price, BigDecimal quantity,
                                                  BigDecimal priceUnitQuantity, String unit, String priceUnit) {
        if (price == null || quantity == null || priceUnitQuantity == null
                || unit == null || unit.isBlank() || priceUnit == null || priceUnit.isBlank()) {
            throw new IllegalStateException("ERP order pricing unit data is incomplete");
        }
        if (unit.equals(priceUnit)) {
            return price;
        }
        if (quantity.signum() == 0) {
            throw new IllegalStateException("ERP order has different pricing unit and zero transaction quantity");
        }
        return price.multiply(priceUnitQuantity).divide(quantity, 6, java.math.RoundingMode.HALF_UP);
    }

    public void validate(int itemCount) {
        if (productAmount == null || taxAmount == null || totalAmount == null || discountAmount == null
                || lines == null || lines.size() != itemCount) {
            throw new IllegalStateException("ERP order financial snapshot is incomplete");
        }
        for (Line line : lines) {
            if (line.getNetAmount() == null || line.getTaxAmount() == null || line.getTotalAmount() == null
                    || line.getNetAmount().add(line.getTaxAmount()).compareTo(line.getTotalAmount()) != 0) {
                throw new IllegalStateException("ERP order entry financial amounts do not balance");
            }
        }
        if (productAmount.add(taxAmount).compareTo(totalAmount) != 0) {
            throw new IllegalStateException("ERP order header financial amounts do not balance");
        }
    }

    /** 本地优惠率为抵减百分比，以 ERP 优惠金额计算等效比例，不重复抵减 ERP 净额。 */
    public BigDecimal discountPercent() {
        BigDecimal gross = totalAmount.add(discountAmount);
        if (gross.signum() == 0) {
            if (discountAmount.signum() != 0) { throw new IllegalStateException("ERP order discount has zero basis"); }
            return BigDecimal.ZERO;
        }
        return discountAmount.multiply(BigDecimal.valueOf(100)).divide(gross, 6, java.math.RoundingMode.HALF_UP);
    }
}
