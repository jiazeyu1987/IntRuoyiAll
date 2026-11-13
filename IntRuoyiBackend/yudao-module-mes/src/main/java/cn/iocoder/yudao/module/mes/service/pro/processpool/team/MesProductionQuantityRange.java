package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesRouteLossCoefficient;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 完成下限取订单数量，累计上限仅取冻结的流程损耗系数。 */
public record MesProductionQuantityRange(BigDecimal lower, BigDecimal upper) {
    public static MesProductionQuantityRange of(MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        if (snapshot == null) throw new IllegalStateException("工序数量范围缺少冻结快照");
        BigDecimal quantity = snapshot.getErpFixedQuantitySnapshot();
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalStateException("工序数量范围无效：生产数量须大于0");
        }
        BigDecimal coefficient = MesRouteLossCoefficient.frozen(snapshot.getProductionConfigSnapshotJson());
        return new MesProductionQuantityRange(quantity,
                quantity.multiply(coefficient).setScale(6, RoundingMode.HALF_UP));
    }
}
