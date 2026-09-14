package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MesReportAllocationSnapshot {
    private Long eventId;
    private Integer version;
    private BigDecimal poolQuantity;
    private BigDecimal releasedAllocatedQuantity;
    private BigDecimal editableAllocatedQuantity;
    private BigDecimal totalAllocatedQuantity;
    private BigDecimal unallocatedQuantity;
    private List<MesReportAllocationSnapshotLine> lines;
}
