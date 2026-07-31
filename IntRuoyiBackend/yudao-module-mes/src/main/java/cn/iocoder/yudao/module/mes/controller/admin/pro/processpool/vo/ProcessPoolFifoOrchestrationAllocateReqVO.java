package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 工序池 FIFO 编排 Request VO")
@Data
public class ProcessPoolFifoOrchestrationAllocateReqVO {

    @Schema(description = "分配批次号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String allocationBatchNo;

    @Schema(description = "来源工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long sourceProcessId;

    @Schema(description = "目标路线工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long targetRouteProcessId;

    @Schema(description = "目标工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long targetProcessId;

    @Schema(description = "目标生产工单ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<Long> targetWorkOrderIds;
}
