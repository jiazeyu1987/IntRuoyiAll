package cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 供应商门户申请审核 Request VO")
@Data
public class SrmSupplierPortalApplicationAuditReqVO {

    @Schema(description = "申请编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请编号不能为空")
    private Long id;

    @Schema(description = "审核意见")
    private String auditRemark;
}
