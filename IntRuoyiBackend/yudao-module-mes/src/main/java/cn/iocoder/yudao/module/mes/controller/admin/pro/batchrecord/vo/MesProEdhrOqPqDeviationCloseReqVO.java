package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR OQ/PQ 偏差关闭 Request VO")
@Data
public class MesProEdhrOqPqDeviationCloseReqVO {

    @Schema(description = "偏差ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "偏差ID不能为空")
    private Long deviationId;

    @Schema(description = "关闭签核人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关闭签核人不能为空")
    private String closeSignoffName;
}
