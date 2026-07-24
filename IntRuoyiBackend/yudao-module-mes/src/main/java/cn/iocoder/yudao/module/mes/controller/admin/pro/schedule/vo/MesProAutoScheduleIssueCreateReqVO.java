package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 生产异常回流创建 Request VO")
@Data
public class MesProAutoScheduleIssueCreateReqVO {

    @Schema(description = "问题类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCTION_EXCEPTION")
    @NotBlank(message = "问题类型不能为空")
    private String issueType;

    @Schema(description = "严重度", requiredMode = Schema.RequiredMode.REQUIRED, example = "BLOCKING")
    @NotBlank(message = "严重度不能为空")
    private String severity;

    @Schema(description = "生产工单编号")
    private Long workOrderId;

    @Schema(description = "生产任务编号")
    private Long taskId;

    @Schema(description = "工序编号")
    private Long processId;

    @Schema(description = "工作站编号")
    private Long workstationId;

    @Schema(description = "发生时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发生时间不能为空")
    private LocalDateTime occurredAt;

    @Schema(description = "异常来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "FEEDBACK")
    @NotBlank(message = "异常来源不能为空")
    private String sourceType;

    @Schema(description = "来源单据编号")
    private Long sourceId;

    @Schema(description = "异常说明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "异常说明不能为空")
    private String message;

}
