package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "???? - MES ?????????? Response VO")
@Data
public class MesProWorkOrderSyncStatusRespVO {

    @Schema(description = "????", requiredMode = Schema.RequiredMode.REQUIRED)
    private String syncType;

    @Schema(description = "???????????", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean autoSyncConfigured;

    @Schema(description = "??????????")
    private Boolean autoSyncEnabled;

    @Schema(description = "????????")
    private Long autoSyncJobId;

    @Schema(description = "????????")
    private String autoSyncJobName;

    @Schema(description = "???? Cron")
    private String autoSyncCronExpression;

    @Schema(description = "????????", requiredMode = Schema.RequiredMode.REQUIRED)
    private String latestStatus;

    @Schema(description = "??????")
    private LocalDateTime latestRunTime;

    @Schema(description = "??????")
    private LocalDateTime latestFinishedTime;

    @Schema(description = "??????")
    private String latestTriggerType;

    @Schema(description = "??????")
    private Integer latestCreatedCount;

    @Schema(description = "??????")
    private Integer latestUpdatedCount;

    @Schema(description = "??????")
    private Integer latestSkippedCount;

    @Schema(description = "??????")
    private Integer latestFailedCount;

    @Schema(description = "??????")
    private String latestFailureMessage;

    @Schema(description = "??????")
    private LocalDateTime lastSuccessTime;
}
