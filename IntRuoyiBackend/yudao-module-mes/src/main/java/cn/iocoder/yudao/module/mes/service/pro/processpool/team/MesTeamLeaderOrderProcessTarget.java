package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import java.math.BigDecimal;

public record MesTeamLeaderOrderProcessTarget(Long routeProcessId,
                                               Long processId,
                                               BigDecimal erpFixedQuantity,
                                               BigDecimal productionQuantityFactor,
                                               BigDecimal plannedQuantity) {
}
