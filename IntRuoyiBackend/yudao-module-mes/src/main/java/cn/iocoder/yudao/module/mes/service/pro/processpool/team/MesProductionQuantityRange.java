package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Completion starts at ERP quantity; the frozen production factor defines the ceiling. */
public record MesProductionQuantityRange(BigDecimal lower, BigDecimal upper) {
    public static MesProductionQuantityRange of(MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        if (snapshot == null) throw new IllegalStateException("工序数量范围缺少冻结快照");
        return of(snapshot.getErpFixedQuantitySnapshot(), snapshot.getProductionQuantityFactorSnapshot(),
                snapshot.getPlannedQuantitySnapshot());
    }

    public static MesProductionQuantityRange of(MesTeamLeaderOrderProcessTarget target) {
        if (target == null) throw new IllegalStateException("工序数量范围缺少目标");
        return of(target.erpFixedQuantity(), target.productionQuantityFactor(), target.plannedQuantity());
    }

    private static MesProductionQuantityRange of(BigDecimal quantity, BigDecimal factor, BigDecimal planned) {
        if (quantity == null || quantity.signum() <= 0 || factor == null || factor.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalStateException("工序数量范围无效：生产数量须大于0，生产系数须大于等于1");
        }
        BigDecimal upper = quantity.multiply(factor).setScale(6, RoundingMode.HALF_UP);
        if (planned == null || planned.compareTo(upper) != 0) {
            throw new IllegalStateException("工序数量上限与冻结的生产数量、生产系数不一致");
        }
        return new MesProductionQuantityRange(quantity, upper);
    }
}
