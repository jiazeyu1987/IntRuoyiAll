package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 班组长活跃订单领料单候选 Response VO")
@Data
public class MesTeamLeaderPickListOptionRespVO {
    @Schema(description = "领料单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pickListId;
    private String sourceFid;
    private String sourceBillNo;
    private String documentStatus;
    private LocalDateTime sourceModifyTime;
    private String productionOrderNo;
    private Integer detailCount;
    private List<String> detailIds;
    private String candidateSnapshotHash;
    private Boolean selectable;
    private String blockerCode;
}
