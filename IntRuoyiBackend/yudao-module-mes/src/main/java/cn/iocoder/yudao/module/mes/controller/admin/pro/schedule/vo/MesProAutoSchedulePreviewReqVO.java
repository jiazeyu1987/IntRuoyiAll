package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 自动排产预览/发布 Request VO")
@Data
public class MesProAutoSchedulePreviewReqVO {

    @Schema(description = "内部派生生产工单编号列表，客户端不要传", hidden = true)
    private List<Long> workOrderIds;

    @Schema(description = "排产工单编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> scheduleOrderIds;

    @Schema(description = "排产起始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排产起始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "运行时产能基准", requiredMode = Schema.RequiredMode.REQUIRED, example = "PLANNED")
    @NotNull(message = "运行时产能基准不能为空")
    private String runtimeCapacityBasis;

    @Schema(description = "是否默认保留手工/锁定任务", example = "true")
    private Boolean preserveManualLockedTasks;

    private String calendarContextToken;

    @Schema(description = "发布/重排业务原因")
    private String reason;

}
