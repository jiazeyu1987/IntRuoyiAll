package cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 采购计划审核 Request VO")
@Data
public class SrmProcurementPlanAuditReqVO {

    @Schema(description = "采购计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购计划编号不能为空")
    private Long id;

    @Schema(description = "审核意见", example = "同意采购")
    private String auditRemark;
}
