package cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 采购计划生成寻源项目 Request VO")
@Data
public class SrmProcurementPlanGenerateReqVO {

    @Schema(description = "采购计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购计划编号不能为空")
    private Long id;

    @Schema(description = "目标项目类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "NON_BIDDING")
    @NotBlank(message = "目标项目类型不能为空")
    private String projectType;
}
