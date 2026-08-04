package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 班组长活跃订单新增 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderAddReqVO {

    @Schema(description = "生产订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    @NotNull
    private Long workOrderId;

    @Schema(description = "正式工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "922119")
    @NotNull
    private Long routeId;

    @Schema(description = "正式工艺路线版本编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "448")
    @NotNull
    private Long routeVersionId;

    @Schema(description = "任务正式调拨/发货/补料/退料来源 ID 列表", example = "[5001,5002]")
    private List<Long> transferIds;
}
