package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长最新版本模拟订单复制 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderSimulationCopyReqVO {

    @Schema(description = "来源活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long sourceActiveOrderId;

    @Schema(description = "模拟运行编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String simulationRunId;
}
