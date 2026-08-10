package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 班组长报工 FIFO 分配预览 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderReportAllocationPreviewRespVO {

    private BigDecimal poolQuantity;

    @Schema(description = "本次预分配总数量", example = "80")
    private BigDecimal totalAllocatedQuantity;

    private BigDecimal unallocatedQuantity;

    @Schema(description = "预分配明细")
    private List<Line> lines;

    @Schema(description = "管理后台 - MES 班组长报工 FIFO 分配预览行 Response VO")
    @Data
    @Accessors(chain = true)
    public static class Line {

        @Schema(description = "活跃订单记录编号", example = "8101")
        private Long activeOrderId;

        @Schema(description = "生产订单编号", example = "9001")
        private Long workOrderId;

        @Schema(description = "生产订单编码", example = "WO-9001")
        private String workOrderCode;

        private Long routeProcessId;

        private Long processId;

        @Schema(description = "本次分配数量", example = "50")
        private BigDecimal allocatedQuantity;

        @Schema(description = "分配前该订单当前工序剩余数量", example = "50")
        private BigDecimal remainingQuantityBeforeAllocation;

        private Boolean released;

        private Boolean editable;
    }
}
