package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 排产员工作台排产工艺路线配置包导入 Response VO")
@Data
public class MesProSchedulerWorkbenchRouteConfigImportRespVO {

    @Schema(description = "导入路线数", example = "12")
    private Integer routeCount;

    @Schema(description = "导入流程配置工序数", example = "120")
    private Integer flowConfigProcessCount;

    @Schema(description = "导入排产配置数", example = "120")
    private Integer scheduleConfigCount;

    @Schema(description = "导入资源配置数", example = "240")
    private Integer resourceCount;

}
