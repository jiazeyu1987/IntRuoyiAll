package cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 供应商风险处理 Request VO")
@Data
public class SrmSupplierRiskResolveReqVO {

    @Schema(description = "风险记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "风险记录编号不能为空")
    private Long id;

    @Schema(description = "处理说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "补齐准入材料并复核通过")
    @NotBlank(message = "处理说明不能为空")
    private String resolutionRemark;
}
