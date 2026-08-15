package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 自动排产任务状态 Response VO")
@Data
public class MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO {

    @Schema(description = "任务是否已注册")
    private Boolean configured;

    @Schema(description = "任务编号")
    private Long jobId;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务是否启用")
    private Boolean enabled;

    @Schema(description = "Cron 表达式")
    private String cronExpression;

    @Schema(description = "下一次触发时间")
    private LocalDateTime nextTriggerTime;

    @Schema(description = "最近一次执行开始时间")
    private LocalDateTime latestBeginTime;

    @Schema(description = "最近一次执行结束时间")
    private LocalDateTime latestEndTime;

    @Schema(description = "最近一次执行状态：RUNNING/SUCCESS/PARTIAL_FAILURE/FAILURE")
    private String latestStatus;

    @Schema(description = "最近一次执行结果")
    private String latestResult;

    @Schema(description = "最近一次执行结果的业务可读摘要")
    private String latestResultSummary;
}
