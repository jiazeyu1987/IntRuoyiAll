package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR OQ/PQ 偏差整改 Request VO")
@Data
public class MesProEdhrOqPqDeviationRemediateReqVO {

    @Schema(description = "偏差ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "偏差ID不能为空")
    private Long deviationId;

    @Schema(description = "原因分析", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原因分析不能为空")
    private String rootCause;

    @Schema(description = "整改措施", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "整改措施不能为空")
    private String remediationAction;

    @Schema(description = "整改责任人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "整改责任人不能为空")
    private String remediationOwnerName;
}
