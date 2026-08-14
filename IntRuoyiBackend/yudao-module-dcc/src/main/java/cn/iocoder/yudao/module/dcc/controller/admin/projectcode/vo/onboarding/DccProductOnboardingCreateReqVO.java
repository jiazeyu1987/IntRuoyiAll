package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 产品建档申请创建 Request VO")
@Data
public class DccProductOnboardingCreateReqVO {

    @Schema(description = "已有 MDM 产品主数据 ID")
    private Long productMasterId;

    @Schema(description = "产品编码；未选择 MDM 产品时必填")
    private String productCode;

    @Schema(description = "DCC 产品编号；未选择 MDM 产品时必填")
    private String dccProductCode;

    @Schema(description = "产品中文名；未选择 MDM 产品时必填")
    private String productNameCn;

    @Schema(description = "产品英文名")
    private String productNameEn;

    @Schema(description = "型号规格")
    private String modelSpecification;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "文控")
    private String docControlNo;

    @Schema(description = "目标项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    @Schema(description = "目标项目代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目代码不能为空")
    private String projectCode;

    @Schema(description = "DCC 类别")
    private String category;

    @Schema(description = "委托生产")
    private String commissionedProduction;

    @Schema(description = "项目组负责人")
    private String projectLeader;

    @Schema(description = "项目工程师")
    private String projectEngineer;

    @Schema(description = "存放位置")
    private String storageLocation;

    @Schema(description = "优先级")
    private String priority;
}
