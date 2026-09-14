package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 班组长活跃订单模拟完成 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderSimulationRespVO {

    @Schema(description = "活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    private Long activeOrderId;

    @Schema(description = "模拟创建的生产提交数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer productionSubmitCount;

    @Schema(description = "模拟创建的生产复核数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer productionReviewCount;

    @Schema(description = "模拟创建的 PQC 提交数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pqcSubmitCount;

    @Schema(description = "模拟创建的 PQC 复核数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pqcReviewCount;

    @Schema(description = "生产进度百分比", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal productionProgressPercent;

    @Schema(description = "检验进度百分比", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal inspectionProgressPercent;
}
