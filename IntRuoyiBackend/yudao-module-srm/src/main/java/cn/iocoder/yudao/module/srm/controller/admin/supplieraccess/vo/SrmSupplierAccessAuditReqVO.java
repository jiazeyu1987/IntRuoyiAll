package cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 供应商准入审核 Request VO")
@Data
public class SrmSupplierAccessAuditReqVO {

    @Schema(description = "准入档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "准入档案编号不能为空")
    private Long id;

    @Schema(description = "审核备注", example = "资质核验通过")
    private String auditRemark;
}
