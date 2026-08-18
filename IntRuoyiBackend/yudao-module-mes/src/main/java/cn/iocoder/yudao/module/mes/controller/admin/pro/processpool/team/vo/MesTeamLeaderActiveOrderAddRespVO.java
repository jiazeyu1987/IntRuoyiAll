package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单提交结果 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderAddRespVO {

    @Schema(description = "活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    private Long activeOrderId;

    @Schema(description = "服务端最终提交动作", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"ADD", "REUSE", "RECOVER"}, example = "RECOVER")
    private String action;
}
