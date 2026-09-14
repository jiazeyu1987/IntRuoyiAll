package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 排产员工作台手动重排数据包导入 Response VO")
@Data
public class MesProSchedulerWorkbenchManualReplanDataImportRespVO {

    @Schema(description = "手动重排主数据导入行数", example = "32")
    private Integer masterDataCount;

    @Schema(description = "手动重排排产工单数据导入行数", example = "18")
    private Integer scheduleOrderDataCount;

    @Schema(description = "手动重排运行态数据导入行数", example = "47")
    private Integer runtimeDataCount;
}
