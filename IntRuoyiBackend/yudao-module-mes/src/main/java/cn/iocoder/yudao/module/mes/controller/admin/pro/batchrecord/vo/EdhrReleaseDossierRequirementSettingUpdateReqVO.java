package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES eDHR 放行资料限制配置更新 Request VO")
@Data
@Accessors(chain = true)
public class EdhrReleaseDossierRequirementSettingUpdateReqVO {

    @Schema(description = "来料检报告是否放行必传", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "incomingInspectionReportRequired 不能为空")
    private Boolean incomingInspectionReportRequired;

    @Schema(description = "灭菌报告是否放行必传", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "sterilizationReportRequired 不能为空")
    private Boolean sterilizationReportRequired;

    @Schema(description = "成品检报告是否放行必传", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "finishedProductInspectionReportRequired 不能为空")
    private Boolean finishedProductInspectionReportRequired;

    @Schema(description = "成品检记录是否放行必传", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "finishedProductInspectionRecordRequired 不能为空")
    private Boolean finishedProductInspectionRecordRequired;
}
