package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderReportAllocationLineReqBO {

    private Long activeOrderId;
    private BigDecimal allocatedQuantity;
}
