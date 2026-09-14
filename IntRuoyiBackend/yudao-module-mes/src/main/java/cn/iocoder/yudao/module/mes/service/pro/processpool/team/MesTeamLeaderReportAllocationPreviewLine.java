package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderReportAllocationPreviewLine {

    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal allocatedQuantity;
    private BigDecimal remainingQuantityBeforeAllocation;
}
