package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单重建 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRebuildReqVO {

    @Schema(description = "活跃订单记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    @NotNull
    private Long activeOrderId;

    @Schema(description = "确认删除报工记录、生产进度和 PQC 检验结果", example = "true")
    private Boolean confirmDeleteHistoricalRuntimeData;
}
