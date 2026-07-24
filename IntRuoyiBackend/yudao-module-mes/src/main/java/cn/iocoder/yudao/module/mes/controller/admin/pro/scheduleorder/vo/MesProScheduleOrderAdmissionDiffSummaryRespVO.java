package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 排产工单待同步差异统计 Response VO")
@Data
public class MesProScheduleOrderAdmissionDiffSummaryRespVO {

    @Schema(description = "可入池数量", example = "8")
    private Integer readyCount = 0;

    @Schema(description = "已入池数量", example = "3")
    private Integer alreadyAdmittedCount = 0;

    @Schema(description = "警告数量", example = "1")
    private Integer warnCount = 0;

    @Schema(description = "阻断数量", example = "2")
    private Integer blockedCount = 0;

}
