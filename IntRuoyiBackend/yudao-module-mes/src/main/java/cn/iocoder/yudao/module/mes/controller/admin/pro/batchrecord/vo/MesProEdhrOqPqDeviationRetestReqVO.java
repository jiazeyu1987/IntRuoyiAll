package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR OQ/PQ 偏差复测 Request VO")
@Data
public class MesProEdhrOqPqDeviationRetestReqVO {

    @Schema(description = "偏差ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "偏差ID不能为空")
    private Long deviationId;

    @Schema(description = "复测结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "复测结果不能为空")
    private String retestResult;

    @Schema(description = "复测证据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "复测证据不能为空")
    private String retestEvidence;

    @Schema(description = "复测复核人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "复测复核人不能为空")
    private String retestReviewerName;
}
