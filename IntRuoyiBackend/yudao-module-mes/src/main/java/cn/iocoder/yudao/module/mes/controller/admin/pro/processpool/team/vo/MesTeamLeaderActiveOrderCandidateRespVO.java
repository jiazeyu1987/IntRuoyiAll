package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单候选 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderCandidateRespVO {

    @Schema(description = "生产工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    private Long workOrderId;

    @Schema(description = "生产工单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WO-9001")
    private String workOrderCode;
}
