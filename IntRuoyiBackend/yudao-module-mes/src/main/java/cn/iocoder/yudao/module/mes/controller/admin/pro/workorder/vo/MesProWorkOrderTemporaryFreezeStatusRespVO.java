package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 生产工单临时冻结状态 Response VO")
@Data
public class MesProWorkOrderTemporaryFreezeStatusRespVO {

    @Schema(description = "是否开启临时冻结", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean enabled;

    @Schema(description = "工单总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
    private Integer totalWorkOrderCount;

    @Schema(description = "冻结工单数", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    private Integer frozenWorkOrderCount;

    @Schema(description = "未冻结工单数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer unfrozenWorkOrderCount;

    @Schema(description = "本次清理的未结束任务数", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer clearedTaskCount;

}
