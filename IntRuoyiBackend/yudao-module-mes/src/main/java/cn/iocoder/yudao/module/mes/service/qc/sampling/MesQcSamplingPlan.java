package cn.iocoder.yudao.module.mes.service.qc.sampling;

import java.math.BigDecimal;

/** Frozen evidence for the agreed nominal sample-size policy, not AQL acceptance/rejection. */
public final class MesQcSamplingPlan {
    private MesQcSamplingPlan() { }

    public record Evidence(String standard, long totalQuantity, String inspectionLevel,
                           String sampleCode, Integer nominalSampleSize,
                           int inspectionQuantity, boolean fullInspection) { }

    public static MesQcSamplingTable.InspectionLevel requireLevel(String level) {
        if (level == null || level.isBlank()) throw new IllegalArgumentException("巡检检验水平不能为空");
        try {
            return MesQcSamplingTable.InspectionLevel.valueOf(level);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("巡检检验水平无效：" + level, ex);
        }
    }

    public static Evidence calculate(BigDecimal totalQuantity, String inspectionLevel) {
        var level = requireLevel(inspectionLevel);
        if (totalQuantity == null || totalQuantity.signum() <= 0)
            throw new IllegalArgumentException("抽样批量必须是大于0的整数");
        final long quantity;
        try {
            quantity = totalQuantity.longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("抽样批量必须为有效的整数", ex);
        }
        // A one-piece lot is inspected in full; table 1 begins at 2 and has no code for 1.
        if (quantity == 1) return new Evidence(MesQcSamplingTable.STANDARD, quantity, level.name(),
                null, null, 1, true);
        var result = MesQcSamplingTable.lookup(quantity, level);
        int actual = (int) Math.min(quantity, result.nominalSampleSize());
        return new Evidence(result.standard(), quantity, level.name(), result.sampleCode(),
                result.nominalSampleSize(), actual, actual == quantity);
    }
}
