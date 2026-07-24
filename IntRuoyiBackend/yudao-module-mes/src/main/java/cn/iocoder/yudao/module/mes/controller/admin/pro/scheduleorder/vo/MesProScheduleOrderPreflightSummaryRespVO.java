package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 排产工单排产前检查统计 Response VO")
@Data
public class MesProScheduleOrderPreflightSummaryRespVO {

    @Schema(description = "通过数量", example = "8")
    private Integer passCount = 0;

    @Schema(description = "警告数量", example = "1")
    private Integer warnCount = 0;

    @Schema(description = "阻断数量", example = "2")
    private Integer blockedCount = 0;

}
