package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES Stage2.5 活跃订单完工闭环模拟 Request VO")
@Data
@Accessors(chain = true)
public class MesStage2_5BackfillBatchExecutionSimulationReqVO {

    @Schema(description = "Stage2.5 模拟运行编号", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "STAGE2_5-20260824103000")
    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[A-Za-z0-9._:-]+")
    private String simulationRunId;

    @Schema(description = "活跃订单记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "150")
    @NotNull
    private Long activeOrderId;

    @Schema(description = "客户端持有的活跃订单版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull
    private Integer expectedVersion;
}
