package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP NAS 表格同步计划 Response VO")
@Data
public class ErpNasTableSyncPlanRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "是否启用")
    private Boolean enabled = false;

    @Schema(description = "每日开始时间")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime dailyStartTime;

    @Schema(description = "业务计划 CRON 表达式")
    private String cronExpression;

    @Schema(description = "NAS 相对目录")
    private String nasDirectory;

    @Schema(description = "文件名规则")
    private String fileNamePattern;

    @Schema(description = "关联 Job 编号")
    private Long jobId;

    @Schema(description = "最近运行编号")
    private Long lastRunId;

    @Schema(description = "最近运行状态")
    private String lastStatus;

    @Schema(description = "计划明细")
    private List<ErpNasTableSyncPlanItemRespVO> items = new ArrayList<>();
}
