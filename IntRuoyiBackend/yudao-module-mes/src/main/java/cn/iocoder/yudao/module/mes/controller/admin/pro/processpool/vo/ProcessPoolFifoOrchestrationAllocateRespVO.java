package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 工序池 FIFO 编排 Response VO")
@Data
@Builder
public class ProcessPoolFifoOrchestrationAllocateRespVO {

    @Schema(description = "总分配数量")
    private BigDecimal totalAllocatedQuantity;

    @Schema(description = "分配明细")
    private List<Line> lines;

    @Schema(description = "分配明细行")
    @Data
    @Builder
    public static class Line {
        private Long sourceQuantityFragmentId;
        private Long sourceEventId;
        private Long targetWorkOrderId;
        private String targetWorkOrderCode;
        private BigDecimal allocatedQuantity;
        private String allocationStatus;
    }
}
