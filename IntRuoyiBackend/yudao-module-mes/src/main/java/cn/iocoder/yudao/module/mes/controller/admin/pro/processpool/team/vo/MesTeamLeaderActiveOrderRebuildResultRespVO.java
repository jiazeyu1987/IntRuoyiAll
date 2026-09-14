package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单重建结果 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRebuildResultRespVO {

    @Schema(description = "活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    private Long activeOrderId;

    @Schema(description = "是否已删除历史业务结果", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean historicalRuntimeDataDeleted;

    @Schema(description = "删除的报工记录数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer deletedProductionReportCount;

    @Schema(description = "删除的生产进度记录数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer deletedProductionProgressCount;

    @Schema(description = "删除的 PQC 检验结果数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer deletedPqcInspectionResultCount;

    @Schema(description = "删除的生产快照数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer deletedProcessSnapshotCount;

    @Schema(description = "删除的 PQC 任务数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer deletedPqcTaskCount;

    @Schema(description = "重建的生产快照数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rebuiltProcessSnapshotCount;

    @Schema(description = "重建的 PQC 任务数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rebuiltPqcTaskCount;
}
