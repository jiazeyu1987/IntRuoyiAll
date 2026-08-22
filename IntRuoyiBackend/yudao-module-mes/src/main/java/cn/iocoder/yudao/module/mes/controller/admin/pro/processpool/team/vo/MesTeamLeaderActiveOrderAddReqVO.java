package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单新增 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderAddReqVO {

    @Schema(description = "生产订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    @NotNull
    private Long workOrderId;

    @Schema(description = "正式领料单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long pickListId;

    @Schema(description = "候选快照 hash", requiredMode = Schema.RequiredMode.REQUIRED)
    @jakarta.validation.constraints.NotBlank
    private String pickListCandidateSnapshotHash;

    @Schema(description = "幂等键", requiredMode = Schema.RequiredMode.REQUIRED)
    @jakarta.validation.constraints.NotBlank
    private String idempotencyKey;
}
