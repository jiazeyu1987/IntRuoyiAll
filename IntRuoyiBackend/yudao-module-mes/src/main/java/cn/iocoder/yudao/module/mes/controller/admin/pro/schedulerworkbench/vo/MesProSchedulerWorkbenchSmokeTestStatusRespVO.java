package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Schema(description = "管理后台 - MES 排产员工作台冒烟测试状态 Response VO")
@Data
public class MesProSchedulerWorkbenchSmokeTestStatusRespVO {

    @Schema(description = "状态：IDLE/RUNNING/STOPPED/FAILED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "是否运行中", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean running;

    @Schema(description = "本轮冒烟是否开启报工审批", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean feedbackApprovalEnabled;

    @Schema(description = "本轮冒烟测试编号")
    private String runId;

    @Schema(description = "操作系统名称")
    private String osName;

    @Schema(description = "进程编号")
    private Long pid;

    @Schema(description = "启动时间")
    private Instant startedAt;

    @Schema(description = "停止时间")
    private Instant stoppedAt;

    @Schema(description = "结束时间")
    private Instant finishedAt;

    @Schema(description = "退出码")
    private Integer exitCode;

    @Schema(description = "前端工作目录")
    private String frontendDirectory;

    @Schema(description = "npm 脚本名称")
    private String scriptName;

    @Schema(description = "启动命令摘要")
    private String commandText;

    @Schema(description = "日志文件")
    private String logFile;

    @Schema(description = "状态说明")
    private String message;

}
