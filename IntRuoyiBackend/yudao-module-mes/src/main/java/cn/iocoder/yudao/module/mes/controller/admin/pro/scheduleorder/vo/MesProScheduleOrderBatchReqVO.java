package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 排产工单批量操作 Request VO")
@Data
public class MesProScheduleOrderBatchReqVO {

    @Schema(description = "排产工单编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "排产工单不能为空")
    private List<Long> ids;

    @Schema(description = "操作原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作原因不能为空")
    private String reason;

}
