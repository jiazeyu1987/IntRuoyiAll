package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MesReportAllocationSaveLine {
    private Long activeOrderId;
    private BigDecimal allocatedQuantity;
}
