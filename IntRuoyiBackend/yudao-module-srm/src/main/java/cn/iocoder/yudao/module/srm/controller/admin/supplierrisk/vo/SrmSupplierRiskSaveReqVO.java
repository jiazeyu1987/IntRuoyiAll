package cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 供应商风险新增 Request VO")
@Data
public class SrmSupplierRiskSaveReqVO {

    @Schema(description = "ERP 供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "ERP 供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "关联准入档案编号", example = "1")
    private Long supplierAccessId;

    @Schema(description = "风险等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "HIGH")
    @NotBlank(message = "风险等级不能为空")
    private String riskLevel;

    @Schema(description = "来源类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACCESS_REQUEST")
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @Schema(description = "来源编号", example = "1")
    private Long sourceId;

    @Schema(description = "来源编码", example = "ACCESS-001")
    private String sourceCode;

    @Schema(description = "来源名称", example = "准入申请-供应商A")
    private String sourceName;

    @Schema(description = "风险描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "资质文件过期")
    @NotBlank(message = "风险描述不能为空")
    private String riskDescription;

    @Schema(description = "风险备注", example = "等待补充新版资质")
    private String riskRemark;
}
