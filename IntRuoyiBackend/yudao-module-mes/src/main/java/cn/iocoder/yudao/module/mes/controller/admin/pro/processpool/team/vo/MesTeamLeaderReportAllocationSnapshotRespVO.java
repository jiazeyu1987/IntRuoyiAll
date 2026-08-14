package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 报工共享分配池快照 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderReportAllocationSnapshotRespVO {

    private Long eventId;
    private Integer version;
    private BigDecimal poolQuantity;
    private BigDecimal releasedAllocatedQuantity;
    private BigDecimal editableAllocatedQuantity;
    private BigDecimal totalAllocatedQuantity;
    private BigDecimal unallocatedQuantity;
    private List<Line> lines;

    @Data
    @Accessors(chain = true)
    public static class Line {
        private Long allocationId;
        private Long activeOrderId;
        private Long workOrderId;
        private String workOrderCode;
        private Long routeProcessId;
        private Long processId;
        private BigDecimal allocatedQuantity;
        private String allocationMode;
        private Boolean released;
        private Boolean editable;
    }
}
