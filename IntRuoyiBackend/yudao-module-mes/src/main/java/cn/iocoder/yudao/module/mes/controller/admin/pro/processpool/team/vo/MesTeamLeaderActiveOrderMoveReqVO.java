package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单移动 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderMoveReqVO {

    @Schema(description = "活跃订单记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    @NotNull
    @Positive
    private Long activeOrderId;

    @Schema(description = "移动方向", requiredMode = Schema.RequiredMode.REQUIRED, example = "UP")
    @NotNull
    @Pattern(regexp = "UP|DOWN", message = "移动方向必须为 UP 或 DOWN")
    private String direction;
}
