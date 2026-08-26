package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES Stage1 活跃订单完成模拟 Request VO")
@Data
@Accessors(chain = true)
public class MesStage1ActiveOrderCompleteSimulationReqVO {

    @NotBlank
    @Schema(description = "本轮模拟运行号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String simulationRunId;

    @NotNull
    @Schema(description = "用于复制正式工单和工艺资料的活跃订单模板编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long templateActiveOrderId;
}
