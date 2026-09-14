package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ProcessPoolTimelineReportAllocationReadDO {

    private Long eventId;
    private Long allocationId;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private BigDecimal allocatedQuantity;
    private BigDecimal overageQuantity;
    private Boolean needsAdjustment;
    private Boolean released;
}
