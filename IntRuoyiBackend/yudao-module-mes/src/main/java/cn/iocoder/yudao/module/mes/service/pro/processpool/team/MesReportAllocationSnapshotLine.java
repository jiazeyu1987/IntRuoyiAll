package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MesReportAllocationSnapshotLine {
    private Long allocationId;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal allocatedQuantity;
    private BigDecimal overageQuantity;
    private Boolean needsAdjustment;
    private String allocationMode;
    private Boolean released;
    private Boolean editable;
}
