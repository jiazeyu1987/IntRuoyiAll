package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 生产异常回流关闭 Request VO")
@Data
public class MesProAutoScheduleIssueResolveReqVO {

    @Schema(description = "问题编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "问题编号不能为空")
    private Long id;

    @Schema(description = "关闭原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关闭原因不能为空")
    private String resolutionReason;

}
