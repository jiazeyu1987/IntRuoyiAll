package cn.iocoder.yudao.module.erp.controller.admin.sync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 金蝶同步运行 Response VO")
@Data
public class ErpKingdeeSyncRunRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "同步类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String syncType;

    @Schema(description = "触发类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String triggerType;

    @Schema(description = "运行状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "窗口开始时间")
    private LocalDateTime windowStartTime;

    @Schema(description = "窗口结束时间")
    private LocalDateTime windowEndTime;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    private LocalDateTime endedAt;

    @Schema(description = "新增数量")
    private Integer createdCount;

    @Schema(description = "更新数量")
    private Integer updatedCount;

    @Schema(description = "跳过数量")
    private Integer skippedCount;

    @Schema(description = "失败数量")
    private Integer failedCount;

    @Schema(description = "失败原因")
    private String failureMessage;
}
