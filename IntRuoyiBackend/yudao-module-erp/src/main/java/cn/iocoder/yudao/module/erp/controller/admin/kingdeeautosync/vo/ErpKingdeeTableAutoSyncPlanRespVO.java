package cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP 表格自动同步计划 Response VO")
@Data
public class ErpKingdeeTableAutoSyncPlanRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "是否启用")
    private Boolean enabled = false;

    @Schema(description = "每日开始时间")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime dailyStartTime;

    @Schema(description = "业务计划 CRON 表达式")
    private String cronExpression;

    @Schema(description = "关联 Job 编号")
    private Long jobId;

    @Schema(description = "最近自动执行日期")
    private LocalDate lastAutoRunDate;

    @Schema(description = "最近执行时间")
    private LocalDateTime lastRunTime;

    @Schema(description = "最近执行状态")
    private String lastStatus;

    @Schema(description = "最近执行信息")
    private String lastMessage;

    @Schema(description = "计划明细")
    private List<ErpKingdeeTableAutoSyncPlanItemRespVO> items = new ArrayList<>();
}
