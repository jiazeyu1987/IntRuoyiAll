package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 排产工单排产前检查 Request VO")
@Data
public class MesProScheduleOrderPreflightReqVO {

    @Schema(description = "检查范围", example = "SELECTED")
    private String scopeType = "SELECTED";

    @Schema(description = "排产工单编号")
    @NotEmpty(message = "排产工单编号不能为空")
    private List<Long> scheduleOrderIds;

    @Schema(description = "是否包含待同步差异")
    private Boolean includeAdmissionDiff;

    @Schema(description = "排产开始时间")
    private LocalDateTime startTime;

    @Schema(description = "产能口径", example = "PLANNED")
    private String capacityMode;

}
