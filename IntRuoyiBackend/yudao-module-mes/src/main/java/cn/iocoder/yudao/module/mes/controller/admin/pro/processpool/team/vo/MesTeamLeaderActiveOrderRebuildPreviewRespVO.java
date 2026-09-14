package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单重建预检 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRebuildPreviewRespVO {

    @Schema(description = "活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    private Long activeOrderId;

    @Schema(description = "是否存在需二次确认删除的历史业务结果", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasHistoricalRuntimeData;

    @Schema(description = "报工记录数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer productionReportCount;

    @Schema(description = "生产进度记录数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer productionProgressCount;

    @Schema(description = "PQC 检验结果数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pqcInspectionResultCount;

    @Schema(description = "生产快照数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer processSnapshotCount;

    @Schema(description = "PQC 快照任务数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pqcTaskCount;

    @Schema(description = "放行申请历史数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer releaseApplicationCount;

    @Schema(description = "工序池事件数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer eventCount;
}
