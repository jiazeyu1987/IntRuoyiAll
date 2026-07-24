package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Schema(description = "管理后台 - MES 排产工单排产前检查 Response VO")
@Data
public class MesProScheduleOrderPreflightRespVO {

    @Schema(description = "检查结果", example = "BLOCKED")
    private String result;

    @Schema(description = "检查时间")
    private LocalDateTime checkedAt;

    @Schema(description = "检查范围")
    private MesProScheduleOrderPreflightScopeRespVO scope = new MesProScheduleOrderPreflightScopeRespVO();

    @Schema(description = "统计")
    private MesProScheduleOrderPreflightSummaryRespVO summary = new MesProScheduleOrderPreflightSummaryRespVO();

    @Schema(description = "问题列表")
    private List<MesProScheduleOrderPreflightIssueRespVO> issues = Collections.emptyList();

}
